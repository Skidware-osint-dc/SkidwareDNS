package com.skidwaredns.app.proxy;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.VpnService;
import android.os.Build;
import android.system.OsConstants;

import com.skidwaredns.app.config.SkidConfig;
import com.skidwaredns.app.dns.DnsUpstreamResolver;
import com.skidwaredns.app.firewall.SkidFirewallEngine;
import com.skidwaredns.app.model.DnsPresets;
import com.skidwaredns.app.tunnel.TunnelEngine;
import com.skidwaredns.app.util.NetDiagnosticsAndMitm;
import com.skidwaredns.app.util.SkidEventBus;
import com.skidwaredns.app.util.TlsSniInspector;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Upgraded Local SOCKS5 + HTTP CONNECT Proxy Engine with:
 *  - Anti-Censorship DPI Strategy & Technique (TLS ClientHello / TCP micro-record splitting)
 *  - TCP Socket Stack Tuning (Socket Buffer Size, Shorter Keepalive, Idle Timeout, TCP_NODELAY, Bandwidth Booster)
 *  - Custom Firewall inspection on every proxied target
 */
public final class LocalProxyEngine {

    private final Context appContext;
    private final VpnService vpnService;
    private final SkidFirewallEngine firewall;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private volatile SkidConfig config;
    private ServerSocket socksServer;
    private ServerSocket httpServer;
    private ExecutorService pool;

    public LocalProxyEngine(Context appContext, VpnService vpnService, SkidFirewallEngine firewall) {
        this.appContext = appContext.getApplicationContext();
        this.vpnService = vpnService;
        this.firewall = firewall;
    }

    public synchronized void start(SkidConfig cfg) {
        stop();
        this.config = cfg;
        if (cfg != null && "None".equalsIgnoreCase(cfg.proxyMode)
                && !"Proxy-Only".equalsIgnoreCase(cfg.mode)
                && !cfg.proxyLockdown) {
            return;
        }
        running.set(true);
        pool = Executors.newCachedThreadPool();

        final int socksPort = cfg != null ? cfg.getPrimaryLocalSocksPort() : 10808;
        final int httpPort = cfg != null ? cfg.getPrimaryLocalHttpPort() : 10809;

        pool.execute(new Runnable() {
            @Override
            public void run() {
                runSocksAcceptLoop(socksPort);
            }
        });

        if (httpPort != socksPort) {
            pool.execute(new Runnable() {
                @Override
                public void run() {
                    runHttpAcceptLoop(httpPort);
                }
            });
        }
    }

    public synchronized void stop() {
        running.set(false);
        closeServerQuietly(socksServer);
        closeServerQuietly(httpServer);
        socksServer = null;
        httpServer = null;
        if (pool != null) {
            pool.shutdownNow();
            pool = null;
        }
    }

    public boolean isRunning() {
        return running.get();
    }

    private void runSocksAcceptLoop(int port) {
        try {
            socksServer = new ServerSocket();
            socksServer.setReuseAddress(true);
            socksServer.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), port), 64);
            SkidEventBus.logSystem("PROXY", "127.0.0.1:" + port, "SOCKS5 listener online");

            while (running.get()) {
                final Socket client = socksServer.accept();
                if (pool != null && running.get()) {
                    pool.execute(new Runnable() {
                        @Override
                        public void run() {
                            handleSocks5Client(client);
                        }
                    });
                } else {
                    client.close();
                }
            }
        } catch (Exception e) {
            if (running.get()) {
                SkidEventBus.logSystem("PROXY", "SOCKS5:" + port, "Listener stopped: " + e.getMessage());
            }
        }
    }

    private void runHttpAcceptLoop(int port) {
        try {
            httpServer = new ServerSocket();
            httpServer.setReuseAddress(true);
            httpServer.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), port), 64);
            SkidEventBus.logSystem("PROXY", "127.0.0.1:" + port, "HTTP CONNECT proxy online");

            while (running.get()) {
                final Socket client = httpServer.accept();
                if (pool != null && running.get()) {
                    pool.execute(new Runnable() {
                        @Override
                        public void run() {
                            handleHttpProxyClient(client);
                        }
                    });
                } else {
                    client.close();
                }
            }
        } catch (Exception e) {
            if (running.get()) {
                SkidEventBus.logSystem("PROXY", "HTTP:" + port, "Listener stopped: " + e.getMessage());
            }
        }
    }

    private void handleSocks5Client(Socket client) {
        Socket upstream = null;
        long start = System.currentTimeMillis();
        try {
            DnsUpstreamResolver.applyTcpSocketTuning(client, config);
            InputStream in = client.getInputStream();
            OutputStream out = client.getOutputStream();

            int ver = in.read();
            if (ver != 0x05) return;
            int nMethods = in.read();
            if (nMethods <= 0) return;
            byte[] methods = new byte[nMethods];
            if (in.read(methods) < nMethods) return;

            out.write(new byte[]{0x05, 0x00});
            out.flush();

            byte[] req = new byte[4];
            if (in.read(req) < 4) return;
            int cmd = req[1] & 0xFF;
            int atyp = req[3] & 0xFF;
            if (cmd != 0x01) {
                out.write(new byte[]{0x05, 0x07, 0x00, 0x01, 0, 0, 0, 0, 0, 0});
                out.flush();
                return;
            }

            String targetHost;
            if (atyp == 0x01) {
                byte[] ip = new byte[4];
                if (in.read(ip) < 4) return;
                targetHost = InetAddress.getByAddress(ip).getHostAddress();
            } else if (atyp == 0x03) {
                int dlen = in.read() & 0xFF;
                byte[] db = new byte[dlen];
                if (in.read(db) < dlen) return;
                targetHost = new String(db, StandardCharsets.US_ASCII);
            } else if (atyp == 0x04) {
                byte[] ip6 = new byte[16];
                if (in.read(ip6) < 16) return;
                targetHost = InetAddress.getByAddress(ip6).getHostAddress();
            } else {
                return;
            }
            int hi = in.read() & 0xFF;
            int lo = in.read() & 0xFF;
            int targetPort = (hi << 8) | lo;

            String fakeResolved = NetDiagnosticsAndMitm.lookupFakeIpDomain(targetHost);
            if (fakeResolved != null) {
                targetHost = fakeResolved;
            }

            if (!validateAppOwnership(client, targetHost, targetPort)) {
                out.write(new byte[]{0x05, 0x02, 0x00, 0x01, 0, 0, 0, 0, 0, 0});
                out.flush();
                return;
            }

            SkidFirewallEngine.Verdict verdict = firewall.evaluateDestination(appContext, targetHost, targetPort, false);
            if (!verdict.allowed) {
                out.write(new byte[]{0x05, 0x02, 0x00, 0x01, 0, 0, 0, 0, 0, 0});
                out.flush();
                SkidEventBus.recordDnsEvent("SOCKS5", targetHost + ":" + targetPort, verdict.matchedRule, 0, true);
                return;
            }

            upstream = TunnelEngine.openOutboundStream(vpnService, config, targetHost, targetPort);
            long ms = System.currentTimeMillis() - start;

            out.write(new byte[]{0x05, 0x00, 0x00, 0x01, 0, 0, 0, 0, 0, 0});
            out.flush();

            SkidEventBus.proxySessions.incrementAndGet();
            SkidEventBus.tcpStreamsHandled.incrementAndGet();
            SkidEventBus.pushLog(new SkidEventBus.LogEntry(
                    "TCP", "INFO", "SOCKS5", targetHost + ":" + targetPort,
                    "OPEN (" + (config != null ? config.connectFromMode : "Direct") + ")", ms, false));

            relayBidirectional(client, upstream, targetHost + ":" + targetPort);
        } catch (Exception ignored) {
        } finally {
            closeSocketQuietly(upstream);
            closeSocketQuietly(client);
        }
    }

    private void handleHttpProxyClient(Socket client) {
        Socket upstream = null;
        long start = System.currentTimeMillis();
        try {
            DnsUpstreamResolver.applyTcpSocketTuning(client, config);
            InputStream in = client.getInputStream();
            OutputStream out = client.getOutputStream();

            String firstLine = readLine(in);
            if (firstLine == null || firstLine.isEmpty()) return;
            String[] parts = firstLine.split(" ");
            if (parts.length < 2) return;
            String method = parts[0].trim();
            String targetSpec = parts[1].trim();

            StringBuilder headerBuf = new StringBuilder();
            String hostHeader = "";
            while (true) {
                String line = readLine(in);
                if (line == null || line.isEmpty()) break;
                headerBuf.append(line).append("\r\n");
                if (line.regionMatches(true, 0, "Host:", 0, 5)) {
                    hostHeader = line.substring(5).trim();
                }
            }

            String dstHost;
            int dstPort;
            if ("CONNECT".equalsIgnoreCase(method)) {
                DnsPresets.HostPort hp = DnsPresets.parseHostPort(targetSpec, 443);
                dstHost = hp.host;
                dstPort = hp.port;
            } else {
                URI uri = URI.create(targetSpec);
                dstHost = uri.getHost() != null ? uri.getHost() : DnsPresets.parseHostPort(hostHeader, 80).host;
                dstPort = uri.getPort() > 0 ? uri.getPort() : 80;
            }

            SkidFirewallEngine.Verdict verdict = firewall.evaluateDestination(appContext, dstHost, dstPort, false);
            if (!verdict.allowed) {
                String deny = "HTTP/1.1 403 Forbidden\r\nX-Skidware-Firewall: " + verdict.matchedRule + "\r\nConnection: close\r\n\r\n";
                out.write(deny.getBytes(StandardCharsets.US_ASCII));
                out.flush();
                SkidEventBus.recordDnsEvent("HTTP", dstHost + ":" + dstPort, verdict.matchedRule, 0, true);
                return;
            }

            upstream = TunnelEngine.openOutboundStream(vpnService, config, dstHost, dstPort);
            long ms = System.currentTimeMillis() - start;

            if ("CONNECT".equalsIgnoreCase(method)) {
                out.write("HTTP/1.1 200 Connection Established\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
                out.flush();
            } else {
                String path = targetSpec;
                try {
                    URI u = URI.create(targetSpec);
                    path = (u.getRawPath() == null || u.getRawPath().isEmpty()) ? "/" : u.getRawPath();
                    if (u.getRawQuery() != null) path += "?" + u.getRawQuery();
                } catch (Exception ignored) {}
                String fwd = method + " " + path + " HTTP/1.1\r\n" + headerBuf + "\r\n";
                upstream.getOutputStream().write(fwd.getBytes(StandardCharsets.US_ASCII));
                upstream.getOutputStream().flush();
            }

            SkidEventBus.proxySessions.incrementAndGet();
            SkidEventBus.tcpStreamsHandled.incrementAndGet();
            SkidEventBus.pushLog(new SkidEventBus.LogEntry(
                    "TCP", "INFO", "HTTP", dstHost + ":" + dstPort, method + " ESTABLISHED", ms, false));

            relayBidirectional(client, upstream, dstHost + ":" + dstPort);
        } catch (Exception ignored) {
        } finally {
            closeSocketQuietly(upstream);
            closeSocketQuietly(client);
        }
    }

    private void relayBidirectional(final Socket client, final Socket upstream, final String label) throws Exception {
        final SkidConfig cfg = this.config;
        Thread inboundThread = new Thread(new Runnable() {
            @Override
            public void run() {
                pump(upstream, client, true, false, cfg, label);
            }
        });
        inboundThread.setDaemon(true);
        inboundThread.start();
        pump(client, upstream, false, shouldApplyDpiSplit(cfg), cfg, label);
        inboundThread.join(2000);
    }

    private static boolean shouldApplyDpiSplit(SkidConfig cfg) {
        if (cfg == null) return false;
        if (cfg.tlsFragment || cfg.tlsRecordFragment || cfg.mixedCaseSni) {
            return true;
        }
        return !("None".equalsIgnoreCase(cfg.dpiStrategy) || "None".equalsIgnoreCase(cfg.dpiTechnique));
    }

    private boolean validateAppOwnership(Socket client, String targetHost, int targetPort) {
        SkidConfig cfg = this.config;
        if (cfg == null || cfg.appOwnershipCheck == null || "Disabled".equalsIgnoreCase(cfg.appOwnershipCheck)) {
            return true;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || appContext == null) {
            return true;
        }
        try {
            ConnectivityManager cm = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return true;
            InetSocketAddress localClient = (InetSocketAddress) client.getRemoteSocketAddress();
            InetSocketAddress localProxy = (InetSocketAddress) client.getLocalSocketAddress();
            int uid = cm.getConnectionOwnerUid(OsConstants.IPPROTO_TCP, localClient, localProxy);
            if (uid <= 0) {
                if ("Advanced".equalsIgnoreCase(cfg.appOwnershipCheck)) {
                    SkidEventBus.recordDnsEvent("UID-CHECK", targetHost + ":" + targetPort,
                            "BLOCKED [Unverified UID owner]", 0, true);
                    return false;
                }
                return true;
            }
            String[] pkgs = appContext.getPackageManager().getPackagesForUid(uid);
            if (pkgs != null) {
                for (String pkg : pkgs) {
                    if (cfg.blockedApps.contains(pkg)) {
                        SkidEventBus.recordDnsEvent("UID-CHECK", targetHost + ":" + targetPort,
                                "BLOCKED [Owner " + pkg + "]", 0, true);
                        return false;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return true;
    }

    private static void pump(Socket src, Socket dst, boolean inbound, boolean applyFirstPacketDpi,
                             SkidConfig cfg, String label) {
        try {
            InputStream in = src.getInputStream();
            OutputStream out = dst.getOutputStream();
            int chunkSize = (cfg != null && cfg.bandwidthBooster) ? 65536 : 16384;
            byte[] buf = new byte[chunkSize];
            boolean firstChunk = true;
            int n;
            while ((n = in.read(buf)) != -1) {
                if (!inbound && firstChunk && applyFirstPacketDpi && n > 5) {
                    firstChunk = false;
                    writeDpiFragmented(dst, out, buf, n, cfg, label);
                } else if (inbound && firstChunk && cfg != null && cfg.sshHeaderReplacer && n > 12
                        && buf[0] == 'H' && buf[1] == 'T' && buf[2] == 'T' && buf[3] == 'P') {
                    firstChunk = false;
                    byte[] norm = com.skidwaredns.app.tunnel.XrayUriAndRouteHelper
                            .normalizeProxyResponseStatus200Ok(buf, n);
                    if (norm == buf) {
                        out.write(buf, 0, n);
                    } else {
                        out.write(norm, 0, norm.length);
                    }
                    out.flush();
                } else {
                    firstChunk = false;
                    out.write(buf, 0, n);
                    out.flush();
                }
                if (inbound) {
                    SkidEventBus.bytesIn.addAndGet(n);
                } else {
                    SkidEventBus.bytesOut.addAndGet(n);
                }
            }
        } catch (Exception ignored) {
        } finally {
            try { dst.shutdownOutput(); } catch (Exception ignored) {}
        }
    }

    /**
     * Real Anti-Censorship TCP / TLS ClientHello micro-segment splitter.
     * Splits initial TLS Handshake (0x16 0x03) or HTTP request across multiple flushed TCP segments
     * to defeat middlebox SNI / Host inspection.
     */
    private static void writeDpiFragmented(Socket dstSocket, OutputStream out, byte[] buf, int len,
                                           SkidConfig cfg, String label) throws Exception {
        dstSocket.setTcpNoDelay(true);
        TlsSniInspector.writeShapedPayload(out, buf, len, cfg);
    }

    private static String readLine(InputStream in) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(128);
        int prev = -1;
        int c;
        while ((c = in.read()) != -1) {
            if (prev == '\r' && c == '\n') {
                byte[] b = baos.toByteArray();
                return new String(b, 0, Math.max(0, b.length - 1), StandardCharsets.US_ASCII);
            }
            baos.write(c);
            prev = c;
            if (baos.size() > 8192) break;
        }
        return baos.size() == 0 ? null : new String(baos.toByteArray(), StandardCharsets.US_ASCII);
    }

    private static void closeServerQuietly(ServerSocket ss) {
        if (ss != null) {
            try { ss.close(); } catch (Exception ignored) {}
        }
    }

    private static void closeSocketQuietly(Socket s) {
        if (s != null) {
            try { s.close(); } catch (Exception ignored) {}
        }
    }
}
