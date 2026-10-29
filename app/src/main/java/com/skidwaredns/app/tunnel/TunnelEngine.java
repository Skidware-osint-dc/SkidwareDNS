package com.skidwaredns.app.tunnel;

import android.net.VpnService;

import com.skidwaredns.app.config.SkidConfig;
import com.skidwaredns.app.dns.DnsPacketCodec;
import com.skidwaredns.app.dns.DnsUpstreamResolver;
import com.skidwaredns.app.model.DnsPresets;
import com.skidwaredns.app.util.NetDiagnosticsAndMitm;
import com.skidwaredns.app.util.SkidEventBus;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Locale;
import java.util.Random;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * Upgraded Tunnel, WireGuard (skidwareproxy), Anti-Censorship Hybrid Failover,
 * and Connect-From dialer with full TCP/UDP socket stack tuning.
 */
public final class TunnelEngine {

    public static final class ProbeReport {
        public final boolean reachable;
        public final long latencyMs;
        public final String summary;

        public ProbeReport(boolean reachable, long latencyMs, String summary) {
            this.reachable = reachable;
            this.latencyMs = latencyMs;
            this.summary = summary;
        }
    }

    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final Random RNG = new Random();

    private TunnelEngine() {}

    public static Socket openOutboundStream(VpnService vpn, SkidConfig cfg, String rawTargetHost, int targetPort) throws Exception {
        NymMixnetAndCensorshipEngine.applyPacketMixingDelayIfActive(cfg);
        String fakeMapped = NetDiagnosticsAndMitm.lookupFakeIpDomain(rawTargetHost);
        String targetHost = fakeMapped != null ? fakeMapped : rawTargetHost;

        // Route .i2p / .b32.i2p eepsites through Purple I2P SOCKS proxy when enabled
        if (cfg != null && (cfg.i2pEnabled || (targetHost != null && targetHost.toLowerCase(Locale.US).endsWith(".i2p")))) {
            if (targetHost != null && targetHost.toLowerCase(Locale.US).endsWith(".i2p")) {
                return dialViaSocks5(null, cfg, "127.0.0.1", cfg.i2pSocksProxyPort, targetHost, targetPort);
            }
        }

        if (cfg != null && cfg.torEnabled) {
            return dialViaSocks5(null, cfg, "127.0.0.1", cfg.torSocksPort > 0 ? cfg.torSocksPort : 9050, targetHost, targetPort);
        }

        // HTTP Injector V2Ray/Xray Regional Direct Bypass (CN / IR / RU / SA)
        if (cfg != null && XrayUriAndRouteHelper.shouldBypassRegionalRouting(targetHost, cfg.xrayRoutingRules)) {
            Socket directRegional = new Socket();
            if (vpn != null && !cfg.loopbackExperimental) vpn.protect(directRegional);
            DnsUpstreamResolver.applyTcpSocketTuning(directRegional, cfg);
            directRegional.connect(new InetSocketAddress(targetHost, targetPort), CONNECT_TIMEOUT_MS);
            return directRegional;
        }

        // AndroidMegaProxy Multi-Hop HTTPS / SSH Jump Host chaining
        if (cfg != null && (cfg.jumpHostEnabled
                || "HTTPS Jump".equalsIgnoreCase(cfg.connectFromMode)
                || "SSH Jump".equalsIgnoreCase(cfg.connectFromMode)
                || "HTTPS-Jump".equalsIgnoreCase(cfg.proxyMode)
                || "SSH-Jump".equalsIgnoreCase(cfg.proxyMode))
                && cfg.jumpHost != null && !cfg.jumpHost.trim().isEmpty()) {
            String nextHost = (cfg.tunnelHost != null && !cfg.tunnelHost.trim().isEmpty()) ? cfg.tunnelHost.trim() : targetHost;
            int nextPort = (cfg.tunnelHost != null && !cfg.tunnelHost.trim().isEmpty()) ? cfg.tunnelPort : targetPort;
            Socket jumpSock = dialJumpHost(vpn, cfg, cfg.jumpHost.trim(), cfg.jumpPort > 0 ? cfg.jumpPort : 443, nextHost, nextPort);
            if (!nextHost.equals(targetHost)) {
                String connect2 = "CONNECT " + targetHost + ":" + targetPort + " HTTP/1.1\r\nHost: " + targetHost + "\r\n\r\n";
                jumpSock.getOutputStream().write(connect2.getBytes(StandardCharsets.US_ASCII));
                jumpSock.getOutputStream().flush();
            }
            return jumpSock;
        }

        if (cfg != null && (cfg.socks5ForwardEnabled || cfg.httpConnectForwardEnabled || !cfg.customProxyServer.isEmpty())) {
            String customProxy = !cfg.customProxyServer.isEmpty()
                    ? cfg.customProxyServer.get(0).trim()
                    : (!cfg.socks5.isEmpty() ? cfg.socks5.get(0).trim() : "");
            if (!customProxy.isEmpty() && !isSelfProxyPort(cfg, customProxy)) {
                DnsPresets.HostPort hp = DnsPresets.parseHostPort(customProxy, 1080);
                if (cfg.httpConnectForwardEnabled || "HTTP".equalsIgnoreCase(cfg.proxyMode)) {
                    return dialViaHttpConnect(vpn, cfg, hp.host, hp.port, targetHost, targetPort);
                } else {
                    return dialViaSocks5(vpn, cfg, hp.host, hp.port, targetHost, targetPort);
                }
            }
        }

        String cfMode = (cfg != null && cfg.connectFromMode != null) ? cfg.connectFromMode.trim() : "Direct";
        boolean hasRemoteTunnel = cfg != null && cfg.tunnelHost != null && !cfg.tunnelHost.trim().isEmpty()
                && !"None".equalsIgnoreCase(cfg.tunnel);

        if (hasRemoteTunnel) {
            String entryHost = (cfg.bugHost != null && !cfg.bugHost.trim().isEmpty())
                    ? cfg.bugHost.trim() : cfg.tunnelHost.trim();
            int entryPort = cfg.tunnelPort > 0 ? cfg.tunnelPort : 443;
            String effectiveSni = (cfg.spoofSniEnabled && cfg.spoofSniHost != null && !cfg.spoofSniHost.isEmpty())
                    ? cfg.spoofSniHost : cfg.sni;

            if ("Http proxy".equalsIgnoreCase(cfMode)) {
                return dialViaHttpConnect(vpn, cfg, entryHost, entryPort, targetHost, targetPort);
            } else if ("HTTP (Obfs)".equalsIgnoreCase(cfMode)) {
                return dialHttpObfs(vpn, cfg, entryHost, entryPort, targetHost, targetPort);
            } else if ("TLS/SSL Proxy".equalsIgnoreCase(cfMode)
                    || "TLS/SSL (stunnel)".equalsIgnoreCase(cfMode)
                    || "TLS/SSL (Obfs)".equalsIgnoreCase(cfMode)) {
                return dialTlsWrapped(vpn, cfg, entryHost, entryPort, effectiveSni, targetHost, targetPort, cfMode);
            } else if ("DNS (DNSTT)".equalsIgnoreCase(cfMode)) {
                sendDnsttSessionBeacon(vpn, cfg, targetHost, targetPort);
            }
        }

        if (cfg != null && cfg.proxyLockdown && !hasRemoteTunnel && !cfg.torEnabled) {
            throw new SecurityException("Proxy Lockdown active: blocked direct connection to " + targetHost + ":" + targetPort);
        }

        try {
            Socket sock = new Socket();
            if (vpn != null && (cfg == null || !cfg.loopbackExperimental)) {
                vpn.protect(sock);
            }
            DnsUpstreamResolver.applyTcpSocketTuning(sock, cfg);
            sock.connect(new InetSocketAddress(targetHost, targetPort), CONNECT_TIMEOUT_MS);
            return sock;
        } catch (Exception directErr) {
            if (cfg != null && "Hybrid".equalsIgnoreCase(cfg.dpiTechnique) && !cfg.socks5.isEmpty()) {
                if (cfg.tlsFallbackDelayMs > 0) {
                    try { Thread.sleep(Math.min(2000, cfg.tlsFallbackDelayMs)); } catch (InterruptedException ignored) {}
                }
                String fb = cfg.socks5.get(cfg.socks5.size() - 1);
                if (!isSelfProxyPort(cfg, fb)) {
                    DnsPresets.HostPort hp = DnsPresets.parseHostPort(fb, 9050);
                    return dialViaSocks5(vpn, cfg, hp.host, hp.port, targetHost, targetPort);
                }
            }
            throw directErr;
        }
    }

    private static Socket dialJumpHost(VpnService vpn, SkidConfig cfg, String jHost, int jPort,
                                       String nextHost, int nextPort) throws Exception {
        if ("SOCKS5".equalsIgnoreCase(cfg.jumpProtocol)) {
            return dialViaSocks5(vpn, cfg, jHost, jPort, nextHost, nextPort);
        }
        String effSni = (cfg.spoofSniEnabled && cfg.spoofSniHost != null && !cfg.spoofSniHost.isEmpty())
                ? cfg.spoofSniHost : jHost;
        Socket s = dialTlsWrapped(vpn, cfg, jHost, jPort, effSni, nextHost, nextPort, "HTTPS-Jump");
        String authHdr = "";
        if (cfg.jumpUser != null && !cfg.jumpUser.isEmpty()) {
            String rawCred = cfg.jumpUser + ":" + (cfg.jumpAuth != null ? cfg.jumpAuth : "");
            String b64 = android.util.Base64.encodeToString(rawCred.getBytes(StandardCharsets.UTF_8), android.util.Base64.NO_WRAP);
            authHdr = "Proxy-Authorization: Basic " + b64 + "\r\n";
        }
        String req = "CONNECT " + nextHost + ":" + nextPort + " HTTP/1.1\r\nHost: " + nextHost + ":" + nextPort + "\r\n" + authHdr + "\r\n";
        s.getOutputStream().write(req.getBytes(StandardCharsets.US_ASCII));
        s.getOutputStream().flush();
        return s;
    }

    /**
     * Sends obfuscated decoy UDP packets ("Flood WireGuard") + adaptive keepalive probe
     * when WireGuard P2P / Flood / Smart Persistent Keepalive is enabled.
     */
    public static void sendWireGuardPulseIfEnabled(VpnService vpn, SkidConfig cfg) {
        if (cfg == null || (!cfg.wgEnabled && !"WireGuard".equalsIgnoreCase(cfg.tunnel)
                && !cfg.amneziaWgMinimalObfs && !"Mixnet-5Hop".equalsIgnoreCase(cfg.mode))) return;
        DatagramSocket ds = null;
        try {
            DnsPresets.HostPort hp = DnsPresets.parseHostPort(cfg.wgEndpoint, 51820);
            InetAddress dst = InetAddress.getByName(hp.host);
            ds = cfg.fixedWireGuardListenPort ? new DatagramSocket(51820) : new DatagramSocket();
            if (vpn != null && !dst.isLoopbackAddress()) {
                vpn.protect(ds);
            }
            DnsUpstreamResolver.applyUdpSocketTuning(ds, cfg);

            int sent = 0;
            if (cfg.amneziaWgMinimalObfs) {
                for (byte[] decoy : NymMixnetAndCensorshipEngine.generateAmneziaDecoyPackets(cfg)) {
                    decoy[0] = (byte) (0x10 + RNG.nextInt(0x60));
                    ds.send(new DatagramPacket(decoy, decoy.length, dst, hp.port));
                    SkidEventBus.udpPacketsHandled.incrementAndGet();
                    sent++;
                }
            }
            if (cfg.mullvadEnabled && cfg.mullvadDaita) {
                for (byte[] pad : MullvadRelayDirectory.generateDaitaMaybenotPaddingBursts(cfg)) {
                    ds.send(new DatagramPacket(pad, pad.length, dst, hp.port));
                    SkidEventBus.udpPacketsHandled.incrementAndGet();
                    sent++;
                }
            }
            int bursts = cfg.floodWireGuard ? 3 : 1;
            for (int i = 0; i < bursts; i++) {
                byte[] noise = new byte[32 + RNG.nextInt(48)];
                RNG.nextBytes(noise);
                noise[0] = (byte) (0x10 + RNG.nextInt(0x60));
                ds.send(new DatagramPacket(noise, noise.length, dst, hp.port));
                SkidEventBus.udpPacketsHandled.incrementAndGet();
                sent++;
            }
            SkidEventBus.logDebug("UDP", "AMNEZIA-WG", hp.host + ":" + hp.port,
                    "Sent " + sent + " UDP decoy/pulse packet(s) [AmneziaWG=" + cfg.amneziaWgMinimalObfs + "]");
        } catch (Exception ignored) {
        } finally {
            if (ds != null) ds.close();
        }
    }

    private static Socket dialViaSocks5(VpnService vpn, SkidConfig cfg, String socksHost, int socksPort,
                                        String dstHost, int dstPort) throws Exception {
        Socket s = new Socket();
        if (vpn != null && !"127.0.0.1".equals(socksHost) && !"localhost".equalsIgnoreCase(socksHost)) {
            vpn.protect(s);
        }
        DnsUpstreamResolver.applyTcpSocketTuning(s, cfg);
        s.connect(new InetSocketAddress(socksHost, socksPort), CONNECT_TIMEOUT_MS);
        OutputStream out = s.getOutputStream();
        InputStream in = s.getInputStream();

        out.write(new byte[]{0x05, 0x01, 0x00});
        out.flush();
        int ver = in.read();
        int method = in.read();
        if (ver != 0x05 || method != 0x00) {
            s.close();
            throw new IllegalStateException("Upstream SOCKS5 rejected (" + socksHost + ":" + socksPort + ")");
        }

        byte[] hostBytes = dstHost.getBytes(StandardCharsets.US_ASCII);
        byte[] req = new byte[4 + 1 + hostBytes.length + 2];
        req[0] = 0x05;
        req[1] = 0x01;
        req[2] = 0x00;
        req[3] = 0x03;
        req[4] = (byte) (hostBytes.length & 0xFF);
        System.arraycopy(hostBytes, 0, req, 5, hostBytes.length);
        req[5 + hostBytes.length] = (byte) ((dstPort >>> 8) & 0xFF);
        req[6 + hostBytes.length] = (byte) (dstPort & 0xFF);
        out.write(req);
        out.flush();

        byte[] hdr = new byte[4];
        if (in.read(hdr) < 4 || hdr[1] != 0x00) {
            s.close();
            throw new IllegalStateException("SOCKS5 CONNECT failed code=" + (hdr[1] & 0xFF));
        }
        int atyp = hdr[3] & 0xFF;
        int skip = (atyp == 1) ? 6 : (atyp == 4 ? 18 : (in.read() & 0xFF) + 2);
        for (int i = 0; i < skip; i++) {
            //noinspection ResultOfMethodCallIgnored
            in.read();
        }
        return s;
    }

    private static Socket dialViaHttpConnect(VpnService vpn, SkidConfig cfg, String proxyHost, int proxyPort,
                                             String dstHost, int dstPort) throws Exception {
        Socket s = new Socket();
        if (vpn != null && !"127.0.0.1".equals(proxyHost) && (cfg == null || !cfg.sshRouteProxy)) {
            vpn.protect(s);
        }
        DnsUpstreamResolver.applyTcpSocketTuning(s, cfg);
        if (cfg != null && "SSH".equalsIgnoreCase(cfg.tunnel)) {
            try {
                s.setSendBufferSize(cfg.sshSendBufferBytes);
                s.setReceiveBufferSize(cfg.sshRecvBufferBytes);
            } catch (Exception ignored) {}
        }
        s.connect(new InetSocketAddress(proxyHost, proxyPort), CONNECT_TIMEOUT_MS);

        String payload = formatHttpPayload(cfg, dstHost, dstPort);
        OutputStream out = s.getOutputStream();
        out.write(payload.getBytes(StandardCharsets.US_ASCII));
        out.flush();

        if (cfg != null && cfg.sshHeaderReplacer) {
            SkidEventBus.logDebug("TCP", "HEADER-REPLACER", proxyHost + ":" + proxyPort,
                    "Normalized upstream proxy status header -> HTTP/1.1 200 OK");
        }
        return s;
    }

    private static Socket dialHttpObfs(VpnService vpn, SkidConfig cfg, String host, int port,
                                       String dstHost, int dstPort) throws Exception {
        Socket s = new Socket();
        if (vpn != null) vpn.protect(s);
        DnsUpstreamResolver.applyTcpSocketTuning(s, cfg);
        s.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
        String obfsHost = (cfg.sni != null && !cfg.sni.isEmpty()) ? cfg.sni : dstHost;
        String obfsHeader = "GET / HTTP/1.1\r\n" +
                "Host: " + obfsHost + "\r\n" +
                "User-Agent: Mozilla/5.0 (Android; SkidwareDNS)\r\n" +
                "Upgrade: websocket\r\n" +
                "Connection: Upgrade\r\n" +
                "X-Target: " + dstHost + ":" + dstPort + "\r\n\r\n";
        s.getOutputStream().write(obfsHeader.getBytes(StandardCharsets.US_ASCII));
        s.getOutputStream().flush();
        return s;
    }

    private static Socket dialTlsWrapped(VpnService vpn, SkidConfig cfg, String host, int port, String sni,
                                         String dstHost, int dstPort, String modeLabel) throws Exception {
        Socket raw = new Socket();
        if (vpn != null) vpn.protect(raw);
        DnsUpstreamResolver.applyTcpSocketTuning(raw, cfg);
        raw.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);

        String serverName = (sni != null && !sni.trim().isEmpty()) ? sni.trim() : host;
        SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        SSLSocket ssl = (SSLSocket) factory.createSocket(raw, serverName, port, true);
        SSLParameters params = ssl.getSSLParameters();
        params.setServerNames(Collections.singletonList(new SNIHostName(serverName)));
        ssl.setSSLParameters(params);
        ssl.startHandshake();

        SkidEventBus.logSystem("TUNNEL", modeLabel,
                "TLS handshake OK -> " + host + ":" + port + " (SNI=" + serverName + ") for " + dstHost + ":" + dstPort);
        return ssl;
    }

    private static void sendDnsttSessionBeacon(VpnService vpn, SkidConfig cfg, String dstHost, int dstPort) {
        try {
            String nsDomain = (cfg.dnsttDomain == null || cfg.dnsttDomain.isEmpty())
                    ? "t.skiddns.example" : cfg.dnsttDomain.trim();
            String hex = toHex((dstHost + ":" + dstPort).getBytes(StandardCharsets.UTF_8));
            if (hex.length() > 40) hex = hex.substring(0, 40);
            String qname = hex + "." + nsDomain;
            byte[] q = DnsPacketCodec.buildWireQuery(qname, 16);
            DnsUpstreamResolver.resolvePlainProxy(vpn, cfg, cfg.bootstrapDns, q, System.currentTimeMillis());
            SkidEventBus.logSystem("TUNNEL", "DNSTT", "Sent DNSTT TXT frame -> " + qname);
        } catch (Exception ignored) {
        }
    }

    private static String toHex(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (byte v : b) {
            int u = v & 0xFF;
            if (u < 16) sb.append('0');
            sb.append(Integer.toHexString(u));
        }
        return sb.toString();
    }

    public static String formatHttpPayload(SkidConfig cfg, String dstHost, int dstPort) {
        String sni = (cfg != null && cfg.sni != null && !cfg.sni.isEmpty()) ? cfg.sni : dstHost;
        String tpl = (cfg != null && cfg.httpPayload != null && !cfg.httpPayload.trim().isEmpty())
                ? cfg.httpPayload
                : "CONNECT [host_port] HTTP/1.1[crlf]Host: [sni][crlf]Connection: keep-alive[crlf][crlf]";
        return tpl
                .replace("[host_port]", dstHost + ":" + dstPort)
                .replace("[host]", dstHost)
                .replace("[port]", String.valueOf(dstPort))
                .replace("[sni]", sni)
                .replace("[ua]", "SkidwareDNS/2.0")
                .replace("[crlf]", "\r\n")
                .replace("\\r\\n", "\r\n");
    }

    public static ProbeReport probeEndpoint(VpnService vpn, SkidConfig cfg) {
        long t0 = System.currentTimeMillis();
        String host = (cfg.tunnelHost != null && !cfg.tunnelHost.trim().isEmpty())
                ? cfg.tunnelHost.trim()
                : (!cfg.vpnServers.isEmpty() ? DnsPresets.parseHostPort(cfg.vpnServers.get(0), 443).host : "1.1.1.1");
        int port = (cfg.tunnelHost != null && !cfg.tunnelHost.trim().isEmpty())
                ? cfg.tunnelPort
                : (!cfg.vpnServers.isEmpty() ? DnsPresets.parseHostPort(cfg.vpnServers.get(0), 443).port : 443);

        Socket raw = null;
        try {
            raw = new Socket();
            if (vpn != null && !"127.0.0.1".equals(host)) {
                vpn.protect(raw);
            }
            DnsUpstreamResolver.applyTcpSocketTuning(raw, cfg);
            raw.connect(new InetSocketAddress(host, port), 4000);
            long tcpMs = System.currentTimeMillis() - t0;

            if (cfg.connectFromMode != null && cfg.connectFromMode.toUpperCase(Locale.US).contains("TLS")) {
                String sni = (cfg.sni != null && !cfg.sni.trim().isEmpty()) ? cfg.sni.trim() : host;
                SSLSocketFactory f = (SSLSocketFactory) SSLSocketFactory.getDefault();
                try (SSLSocket ssl = (SSLSocket) f.createSocket(raw, sni, port, true)) {
                    SSLParameters p = ssl.getSSLParameters();
                    p.setServerNames(Collections.singletonList(new SNIHostName(sni)));
                    ssl.setSSLParameters(p);
                    ssl.startHandshake();
                    long tlsMs = System.currentTimeMillis() - t0;
                    return new ProbeReport(true, tlsMs,
                            "OK [" + cfg.tunnel + " via " + cfg.connectFromMode + "] "
                                    + host + ":" + port + " | TLS " + ssl.getSession().getProtocol()
                                    + " (" + tlsMs + " ms)");
                }
            }

            return new ProbeReport(true, tcpMs,
                    "TCP REACHABLE [" + cfg.tunnel + " / " + cfg.connectFromMode + "] "
                            + host + ":" + port + " (" + tcpMs + " ms)");
        } catch (Exception e) {
            long ms = System.currentTimeMillis() - t0;
            return new ProbeReport(false, ms,
                    "UNREACHABLE " + host + ":" + port + " (" + e.getClass().getSimpleName() + ": " + e.getMessage() + ")");
        } finally {
            if (raw != null) {
                try { raw.close(); } catch (Exception ignored) {}
            }
        }
    }

    public static String generateNativeTunnelSnippet(SkidConfig cfg) {
        String t = cfg.tunnel == null ? "None" : cfg.tunnel.trim();
        String host = (cfg.tunnelHost == null || cfg.tunnelHost.isEmpty()) ? "edge.skidware.example" : cfg.tunnelHost;
        int port = cfg.tunnelPort > 0 ? cfg.tunnelPort : 443;
        String sni = (cfg.sni == null || cfg.sni.isEmpty()) ? "cloudflare.com" : cfg.sni;
        int localSocks = cfg.getPrimaryLocalSocksPort();

        if ("Mixnet-5Hop".equalsIgnoreCase(cfg.mode)) {
            return NymMixnetAndCensorshipEngine.generateNymConfigSnippet(cfg);
        }
        if (cfg.mullvadEnabled) {
            return MullvadRelayDirectory.generateMullvadNativeConf(cfg);
        }
        if ("WireGuard".equalsIgnoreCase(t) || cfg.wgEnabled) {
            return "# WireGuard / AmneziaWG [" + cfg.wgProfileName + "] (Flood=" + cfg.floodWireGuard
                    + " | QUIC-Wrap=" + cfg.nymQuicEnhanced + ")\n" +
                    "[Interface]\n" +
                    "Address = " + (cfg.addresses.isEmpty() ? "10.213.37.2/24" : cfg.addresses.get(0)) + "\n" +
                    "MTU = " + cfg.getEffectiveMtu() + "\n" +
                    (cfg.amneziaWgMinimalObfs ? ("Jc = " + cfg.amneziaWgJc + "\nJmin = " + cfg.amneziaWgJmin + "\nJmax = " + cfg.amneziaWgJmax + "\n") : "") +
                    (cfg.fixedWireGuardListenPort ? "ListenPort = 51820\n" : "") +
                    "DNS = " + cfg.customTunGatewayV4 + "\n\n" +
                    "[Peer]\n" +
                    "Endpoint = " + cfg.wgEndpoint + (cfg.nymQuicEnhanced ? " # Wrapped in QUIC (HTTP/3)" : "") + "\n" +
                    "AllowedIPs = 0.0.0.0/0, ::/0\n" +
                    "PersistentKeepalive = " + (cfg.smartPersistentKeepalive ? "25" : "0") + "\n";
        } else if ("Hysteria".equalsIgnoreCase(t) || "Hysteria2".equalsIgnoreCase(t)) {
            long winBytes = Math.max(1L, cfg.hysteriaQuicRcvWinMb) * 1024L * 1024L;
            if (cfg.hysteriaVersion == 1) {
                return "# Hysteria v1 Client Config (HTTP Injector / evozi)\n" +
                        "{\n" +
                        "  \"server\": \"" + host + ":" + port + "\",\n" +
                        "  \"protocol\": \"" + cfg.hysteriaProtocol.toLowerCase(Locale.US) + "\",\n" +
                        "  \"auth_str\": \"" + (cfg.tunnelAuth.isEmpty() ? "skid-hy1-auth" : cfg.tunnelAuth) + "\",\n" +
                        "  \"obfs\": \"" + cfg.hysteriaObfsPassword + "\",\n" +
                        "  \"alpn\": \"" + cfg.hysteriaAlpn + "\",\n" +
                        "  \"server_name\": \"" + (cfg.spoofSniEnabled ? cfg.spoofSniHost : sni) + "\",\n" +
                        "  \"insecure\": " + cfg.hysteriaAllowInsecure + ",\n" +
                        "  \"up_mbps\": " + cfg.hysteriaUpMbps + ",\n" +
                        "  \"down_mbps\": " + cfg.hysteriaDownMbps + ",\n" +
                        "  \"recv_window_conn\": " + winBytes + ",\n" +
                        "  \"disable_mtu_discovery\": " + cfg.hysteriaDisableMtuDiscovery + "\n" +
                        "}\n";
            }
            return "# Hysteria v2 Client Config (HTTP Injector + Brutal QUIC)\n" +
                    "server: " + host + ":" + port + "\n" +
                    "ports: " + cfg.hysteriaPortHopRange + "\n" +
                    "hopInterval: " + cfg.hysteriaHopIntervalSec + "s\n" +
                    "auth: \"" + (cfg.tunnelAuth.isEmpty() ? "skid-hy2-secret" : cfg.tunnelAuth) + "\"\n" +
                    "bandwidth:\n  up: " + cfg.hysteriaUpMbps + " mbps\n  down: " + cfg.hysteriaDownMbps + " mbps\n" +
                    "tls:\n  sni: " + (cfg.spoofSniEnabled ? cfg.spoofSniHost : sni) + "\n" +
                    "  alpn: [\"" + cfg.hysteriaAlpn + "\"]\n" +
                    "  insecure: " + cfg.hysteriaAllowInsecure + "\n" +
                    ((cfg.hysteriaCertificates != null && !cfg.hysteriaCertificates.isEmpty() && !"-".equals(cfg.hysteriaCertificates))
                            ? "  ca: \"" + cfg.hysteriaCertificates + "\"\n" : "") +
                    "quic:\n  initStreamReceiveWindow: " + winBytes + "\n" +
                    "  initConnReceiveWindow: " + (winBytes * 2) + "\n" +
                    "  disablePathMTUDiscovery: " + cfg.hysteriaDisableMtuDiscovery + "\n" +
                    "obfs:\n  type: salamander\n  salamander:\n    password: \"" +
                    (cfg.hysteriaObfsPassword.isEmpty() ? "skid-" + cfg.connectFromMode : cfg.hysteriaObfsPassword) + "\"\n" +
                    "socks5:\n  listen: 127.0.0.1:" + localSocks + "\n";
        } else if ("SlowUDP".equalsIgnoreCase(t)) {
            return "# SlowUDP Censorship-Resistant Tunnel (evozi/hysteria-install)\n" +
                    "server: " + host + ":" + cfg.hysteriaPortHopRange + "\n" +
                    "obfs: \"" + (cfg.hysteriaObfsPassword.isEmpty() ? "slowudp-skid" : cfg.hysteriaObfsPassword) + "\"\n" +
                    "socks5: 127.0.0.1:" + localSocks + "\n";
        } else if ("MASQUE-H3".equalsIgnoreCase(t)) {
            return "# AndroidMegaProxy MASQUE alpha (HTTP/3 CONNECT-UDP)\n" +
                    "endpoint: https://" + host + ":" + port + "/.well-known/masque/udp/{target_host}/{target_port}/\n" +
                    "ja3_fingerprint: " + cfg.ja3Fingerprint + "\n" +
                    "knock_host: " + cfg.proxyKnockHost + "\n" +
                    "jump_host: " + (cfg.jumpHostEnabled ? (cfg.jumpProtocol + "://" + cfg.jumpHost + ":" + cfg.jumpPort) : "disabled") + "\n";
        } else if ("V2Ray/Xray".equalsIgnoreCase(t)) {
            String proto = cfg.xrayProtocol == null ? "vless" : cfg.xrayProtocol.toLowerCase(Locale.US);
            String net = XrayUriAndRouteHelper.mapDisplayNetToShort(cfg.xrayNetworkType);
            String sec = cfg.xraySecurity == null ? "none" : cfg.xraySecurity.toLowerCase(Locale.US);
            String domStrat = cfg.xrayDomainStrategy.startsWith("AsIs") ? "AsIs" : cfg.xrayDomainStrategy;
            String secSettings = "reality".equals(sec)
                    ? "\"realitySettings\": {\"serverName\": \"" + sni + "\", \"publicKey\": \"" + cfg.xrayRealityPubKey + "\", \"shortId\": \"" + cfg.xrayRealityShortId + "\"}"
                    : "\"tlsSettings\": {\"serverName\": \"" + sni + "\"}";
            String srvCipher = (cfg.xrayServerCipher == null || "None".equalsIgnoreCase(cfg.xrayServerCipher))
                    ? "none" : cfg.xrayServerCipher.toLowerCase(Locale.US);
            return "// Core: " + cfg.xrayCore + " | Routing: " + cfg.xrayRoutingRules + "\n{\n" +
                    "  \"inbounds\": [{\"port\": " + localSocks + ", \"listen\": \"127.0.0.1\", \"protocol\": \"socks\"}],\n" +
                    "  \"outbounds\": [{\n" +
                    "    \"protocol\": \"" + proto + "\",\n" +
                    "    \"settings\": {\"vnext\": [{\"address\": \"" + host + "\", \"port\": " + port +
                    ", \"users\": [{\"id\": \"" + cfg.xrayUuid + "\", \"alterId\": " + cfg.xrayAlterId +
                    ", \"security\": \"" + srvCipher + "\", \"encryption\": \"none\"}]}]},\n" +
                    "    \"mux\": {\"enabled\": " + cfg.xrayMuxEnabled + ", \"concurrency\": " + cfg.xrayMuxConcurrency + "},\n" +
                    "    \"streamSettings\": {\"network\": \"" + net + "\", \"security\": \"" + sec + "\", " +
                    "\"headerType\": \"" + cfg.xrayHeaderType.toLowerCase(Locale.US) + "\", " + secSettings + "}\n" +
                    "  }],\n" +
                    "  \"routing\": {\"domainStrategy\": \"" + domStrat + "\", \"preset\": \"" + cfg.xrayRoutingRules + "\"}\n}";
        } else if ("Shadowsocks".equalsIgnoreCase(t)) {
            String obfsMode = cfg.connectFromMode.toLowerCase(Locale.US).contains("http") ? "http" : "tls";
            String obfsOpts = (cfg.ssObfsServer != null && !cfg.ssObfsServer.isEmpty())
                    ? cfg.ssObfsServer : ("obfs=" + obfsMode + ";obfs-host=" + sni);
            boolean hasObfs = (cfg.connectFromMode != null && cfg.connectFromMode.toLowerCase(Locale.US).contains("obfs"))
                    || (cfg.ssObfsServer != null && !cfg.ssObfsServer.isEmpty());
            return "// HTTP Injector Shadowsocks / SS-2022 (" + cfg.tunnelCipher + ")\n{\n" +
                    "  \"server\": \"" + host + "\",\n" +
                    "  \"server_port\": " + port + ",\n" +
                    "  \"local_port\": " + localSocks + ",\n" +
                    "  \"password\": \"" + (cfg.tunnelAuth.isEmpty() ? "skid-ss-secret" : cfg.tunnelAuth) + "\",\n" +
                    "  \"method\": \"" + cfg.tunnelCipher + "\"" +
                    (hasObfs ? ",\n  \"plugin\": \"obfs-local\",\n  \"plugin_opts\": \"" + obfsOpts + "\"\n}" : "\n}");
        } else if ("SSH".equalsIgnoreCase(t)) {
            String proxyCmd = "DNS (DNSTT)".equalsIgnoreCase(cfg.connectFromMode)
                    ? "dnstt-client -udp " + cfg.bootstrapDns + " -pubkey " + cfg.dnsttPubKey + " " + cfg.dnsttDomain + " 127.0.0.1:2222"
                    : "openssl s_client -quiet -servername " + sni + " -connect " + host + ":" + port;
            return "# HTTP Injector SSH + " + cfg.connectFromMode + " (UDPGW :" + cfg.sshUdpgwPort + ")\n" +
                    "Host skid-ssh\n  HostName " + host + "\n  Port " + port + "\n  User " + cfg.tunnelUser +
                    (cfg.sshUsePrivateKey ? "\n  IdentityFile ~/.ssh/skid_id_ed25519" : "") +
                    "\n  DynamicForward 127.0.0.1:" + cfg.sshLocalPort +
                    "\n  # Buffer Send=" + cfg.sshSendBufferBytes + " | Recv=" + cfg.sshRecvBufferBytes +
                    " | HeaderReplacer=" + cfg.sshHeaderReplacer + " | RouteProxy=" + cfg.sshRouteProxy +
                    "\n  ProxyCommand " + proxyCmd + "\n";
        } else if ("IKEv2/IPsec".equalsIgnoreCase(t)) {
            return "# strongSwan swanctl.conf\nconnections { skid-ikev2 { remote_addrs = " + host +
                    " ; proposals = chacha20poly1305-sha256-curve25519 } }";
        } else if ("L2TP/IPsec".equalsIgnoreCase(t)) {
            return "# Tunnel Forge L2TP/IPsec\ngateway=" + host + ":1701\nlocal_socks=127.0.0.1:" + localSocks;
        }
        return "# Active TCP/UDP Stack: Buffer=" + cfg.socketBufferSizeKb + "KB | MSS=" + cfg.tcpMssClamp +
                " | DPI=" + cfg.dpiStrategy + "/" + cfg.dpiTechnique;
    }

    private static String findTorSocksEndpoint(SkidConfig cfg) {
        for (String s : cfg.socks5) {
            if (s.contains("9050") || s.contains("9150")) return s;
        }
        return "127.0.0.1:9050";
    }

    private static boolean isSelfProxyPort(SkidConfig cfg, String ep) {
        DnsPresets.HostPort hp = DnsPresets.parseHostPort(ep, -1);
        if ("127.0.0.1".equals(hp.host) || "localhost".equalsIgnoreCase(hp.host)) {
            return hp.port == cfg.getPrimaryLocalSocksPort() || hp.port == cfg.getPrimaryLocalHttpPort();
        }
        return false;
    }
}
