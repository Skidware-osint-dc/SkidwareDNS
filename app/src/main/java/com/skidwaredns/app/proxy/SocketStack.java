package com.skidwaredns.app.proxy;

import android.net.VpnService;
import android.os.Build;

import com.skidwaredns.app.config.SkidConfig;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * Shared TCP / UDP / HTTPS / Port handlers for every outbound and listen socket.
 * Java 8, no lambdas. Best-effort on Android — kernel options that the SDK
 * cannot set are still stored in .skid and applied where the platform allows.
 */
public final class SocketStack {

    private static final SecureRandom RNG = new SecureRandom();

    private SocketStack() {}

    public static int bufferBytes(SkidConfig cfg) {
        int kb = (cfg != null) ? cfg.socketBufferSizeKb : 512;
        if (kb < 64) kb = 64;
        if (kb > 4096) kb = 4096;
        return kb * 1024;
    }

    public static int connectTimeoutMs(SkidConfig cfg) {
        if (cfg == null) return 8000;
        int ms = cfg.tcpConnectTimeoutMs;
        if (ms < 500) ms = 500;
        if (ms > 60000) ms = 60000;
        return ms;
    }

    public static int idleTimeoutMs(SkidConfig cfg) {
        if (cfg == null) return 15000;
        if (cfg.tcpUserTimeoutMs > 0) {
            return Math.max(500, Math.min(120000, cfg.tcpUserTimeoutMs));
        }
        if (cfg.idleTimeoutSec > 0) {
            return Math.min(15000, cfg.idleTimeoutSec * 1000);
        }
        return cfg.shorterTcpKeepalive ? 8000 : 15000;
    }

    public static int udpTimeoutMs(SkidConfig cfg) {
        if (cfg == null) return 3000;
        int ms = cfg.udpRecvTimeoutMs;
        if (ms <= 0) {
            if (cfg.idleTimeoutSec > 0) ms = Math.min(15000, cfg.idleTimeoutSec * 1000);
            else ms = 3000;
        }
        return Math.max(200, Math.min(30000, ms));
    }

    public static int httpsTimeoutMs(SkidConfig cfg) {
        if (cfg == null) return 10000;
        return Math.max(500, Math.min(60000, cfg.httpsTimeoutMs));
    }

    public static void applyTcp(Socket s, SkidConfig cfg) {
        if (s == null) return;
        try {
            s.setSoTimeout(idleTimeoutMs(cfg));
            if (cfg != null) {
                s.setTcpNoDelay(cfg.tcpNoDelay && !cfg.tcpCork);
                s.setKeepAlive(cfg.shorterTcpKeepalive || cfg.tcpKeepaliveIdleSec > 0);
                int buf = bufferBytes(cfg);
                s.setReceiveBufferSize(buf);
                s.setSendBufferSize(buf);
                s.setTrafficClass(cfg.getTrafficClassByte());
                if (cfg.tcpRstQuickClose) {
                    s.setSoLinger(true, 0);
                } else if (cfg.tcpUserTimeoutMs > 0) {
                    s.setSoLinger(true, Math.max(1, cfg.tcpUserTimeoutMs / 1000));
                }
                try {
                    s.setReuseAddress(cfg.tcpReuseAddress || cfg.soReusePort);
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
    }

    public static void applyUdp(DatagramSocket udp, SkidConfig cfg) {
        if (udp == null) return;
        try {
            udp.setSoTimeout(udpTimeoutMs(cfg));
            if (cfg != null) {
                int buf = bufferBytes(cfg);
                udp.setReceiveBufferSize(buf);
                udp.setSendBufferSize(buf);
                udp.setTrafficClass(cfg.getTrafficClassByte());
                try {
                    udp.setReuseAddress(cfg.udpReuseAddress || cfg.soReusePort);
                } catch (Exception ignored) {}
                try {
                    udp.setBroadcast(cfg.udpBroadcast);
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
    }

    public static void applyServer(ServerSocket ss, SkidConfig cfg) {
        if (ss == null) return;
        try {
            boolean reuse = cfg == null || cfg.tcpReuseAddress || cfg.soReusePort;
            ss.setReuseAddress(reuse);
            int buf = bufferBytes(cfg);
            try {
                ss.setReceiveBufferSize(buf);
            } catch (Exception ignored) {}
            ss.setSoTimeout(0);
        } catch (Exception ignored) {}
    }

    public static void bindLocal(ServerSocket ss, int port, SkidConfig cfg) throws Exception {
        applyServer(ss, cfg);
        boolean loopback = cfg == null || cfg.bindLoopbackOnly;
        InetAddress addr = loopback
                ? InetAddress.getByName("127.0.0.1")
                : InetAddress.getByName("0.0.0.0");
        if (cfg != null && cfg.privilegedPortProtect && port > 0 && port < 1024) {
            if (!cfg.allowWellKnownPorts || !isWellKnown(port)) {
                throw new SecurityException("Privileged-Port-Protect blocked bind of :" + port);
            }
        }
        ss.bind(new InetSocketAddress(addr, port), 64);
    }

    public static DatagramSocket openUdp(SkidConfig cfg) throws Exception {
        DatagramSocket udp;
        if (cfg != null && cfg.udpRandomizeSourcePort) {
            int p = ephemeralPort(cfg);
            try {
                udp = new DatagramSocket(p);
            } catch (Exception e) {
                udp = new DatagramSocket();
            }
        } else {
            udp = new DatagramSocket();
        }
        applyUdp(udp, cfg);
        return udp;
    }

    public static int ephemeralPort(SkidConfig cfg) {
        int min = 20000;
        int max = 60000;
        if (cfg != null) {
            min = Math.max(1024, Math.min(65534, cfg.ephemeralPortMin));
            max = Math.max(min + 1, Math.min(65535, cfg.ephemeralPortMax));
        }
        return min + RNG.nextInt(max - min);
    }

    public static int hopPort(SkidConfig cfg, int fallback) {
        if (cfg == null || !cfg.portHopEnabled) return fallback;
        int min = 10000;
        int max = 60000;
        String range = cfg.portHopRange;
        if (range != null && range.indexOf('-') > 0) {
            try {
                int dash = range.indexOf('-');
                min = Integer.parseInt(range.substring(0, dash).trim());
                max = Integer.parseInt(range.substring(dash + 1).trim());
            } catch (Exception ignored) {}
        }
        if (min < 1) min = 1;
        if (max > 65535) max = 65535;
        if (max <= min) return fallback;
        return min + RNG.nextInt(max - min);
    }

    public static int remapPort(SkidConfig cfg, int port, boolean udp) {
        if (cfg == null) return port;
        if (cfg.remapDns53toDoH && port == 53) return 443;
        String map = cfg.portForwardMap;
        if (map == null || map.trim().isEmpty()) return port;
        String[] parts = map.split("[,;\\n]+");
        for (int i = 0; i < parts.length; i++) {
            String t = parts[i].trim();
            int sep = t.indexOf('>');
            if (sep < 0) sep = t.indexOf('=');
            if (sep < 0) sep = t.indexOf(':');
            if (sep <= 0) continue;
            try {
                int from = Integer.parseInt(t.substring(0, sep).trim());
                int to = Integer.parseInt(t.substring(sep + 1).trim());
                if (from == port && to > 0 && to <= 65535) return to;
            } catch (Exception ignored) {}
        }
        return port;
    }

    public static boolean isPortAllowed(SkidConfig cfg, int port, boolean udp) {
        if (port < 1 || port > 65535) return false;
        if (cfg == null) return true;
        if (matchesList(cfg.portBlockList, port)) return false;
        if (cfg.portAllowList != null && !cfg.portAllowList.trim().isEmpty()) {
            if (matchesList(cfg.portAllowList, port)) return true;
            if (listContainsPort(cfg.vpnPorts, port)) return true;
            if (listContainsPort(cfg.proxyPorts, port)) return true;
            if (listContainsPort(cfg.dnsPorts, port)) return true;
            return false;
        }
        return true;
    }

    public static void knock(VpnService vpn, SkidConfig cfg) {
        if (cfg == null) return;
        String seq = cfg.portKnockSequence;
        if (seq == null || seq.trim().isEmpty()) return;
        String host = cfg.tunnelHost;
        if (host == null || host.trim().isEmpty()) return;
        host = host.trim();
        String[] parts = seq.split("[,; ]+");
        try {
            InetAddress addr = InetAddress.getByName(host);
            for (int i = 0; i < parts.length; i++) {
                String t = parts[i].trim();
                if (t.isEmpty()) continue;
                int p;
                try {
                    p = Integer.parseInt(t);
                } catch (Exception e) {
                    continue;
                }
                if (p < 1 || p > 65535) continue;
                DatagramSocket ds = null;
                try {
                    ds = new DatagramSocket();
                    if (vpn != null && (cfg.loopbackExperimental == false)) {
                        try { vpn.protect(ds); } catch (Exception ignored) {}
                    }
                    byte[] payload = new byte[]{0x53, 0x4B}; // "SK"
                    ds.send(new DatagramPacket(payload, payload.length, addr, p));
                } catch (Exception ignored) {
                } finally {
                    if (ds != null) try { ds.close(); } catch (Exception ignored) {}
                }
            }
        } catch (Exception ignored) {}
    }

    public static SSLSocket openTls(VpnService vpn, SkidConfig cfg, String host, int port, String sni)
            throws Exception {
        if (!isPortAllowed(cfg, port, false)) {
            throw new SecurityException("Port policy blocked TLS :" + port);
        }
        int destPort = remapPort(cfg, port, false);
        destPort = hopPort(cfg, destPort);
        knock(vpn, cfg);

        Socket raw = new Socket();
        if (vpn != null && (cfg == null || !cfg.loopbackExperimental)) {
            vpn.protect(raw);
        }
        applyTcp(raw, cfg);
        raw.connect(new InetSocketAddress(host, destPort), httpsTimeoutMs(cfg));

        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(null, null, RNG);
        SSLSocketFactory fac = ctx.getSocketFactory();
        String peer = peerName(cfg, host, sni);
        SSLSocket tls = (SSLSocket) fac.createSocket(raw, peer, destPort, true);
        applyHttps(tls, cfg, peer);
        tls.startHandshake();
        verifyPin(tls, cfg);
        if (cfg == null || cfg.httpsEndpointId) {
            HostnameVerifier hv = HttpsURLConnection.getDefaultHostnameVerifier();
            if (!hv.verify(peer, tls.getSession())) {
                try { tls.close(); } catch (Exception ignored) {}
                throw new SecurityException("HTTPS endpoint-id failed for " + peer);
            }
        }
        return tls;
    }

    public static void applyHttps(SSLSocket tls, SkidConfig cfg, String peer) {
        if (tls == null) return;
        try {
            tls.setUseClientMode(true);
            applyTcp(tls, cfg);
            tls.setSoTimeout(httpsTimeoutMs(cfg));
            String[] protos = enabledProtocols(cfg);
            try {
                tls.setEnabledProtocols(protos);
            } catch (Exception ignored) {}

            SSLParameters params = tls.getSSLParameters();
            if (params == null) params = new SSLParameters();
            if (cfg == null || cfg.httpsSniEnabled) {
                String sni = peer != null ? peer : "";
                if (!sni.isEmpty() && Build.VERSION.SDK_INT >= 24) {
                    List<javax.net.ssl.SNIServerName> names = new ArrayList<javax.net.ssl.SNIServerName>();
                    names.add(new SNIHostName(sni));
                    params.setServerNames(names);
                }
            }
            if (cfg == null || cfg.httpsEndpointId) {
                try {
                    params.setEndpointIdentificationAlgorithm("HTTPS");
                } catch (Exception ignored) {}
            }
            String alpn = (cfg != null && cfg.httpsAlpn != null) ? cfg.httpsAlpn : "h2,http/1.1";
            if (cfg != null && !cfg.httpsHttp2 && cfg.httpsFallbackHttp11) {
                alpn = "http/1.1";
            }
            if (Build.VERSION.SDK_INT >= 29 && alpn != null && !alpn.trim().isEmpty()) {
                String[] apps = splitAlpn(alpn);
                if (apps.length > 0) {
                    try {
                        params.setApplicationProtocols(apps);
                    } catch (Exception ignored) {}
                }
            }
            tls.setSSLParameters(params);
            if (cfg != null && !cfg.httpsSessionTickets) {
                try {
                    tls.getSession().invalidate();
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
    }

    public static String describe(SkidConfig cfg) {
        if (cfg == null) return "default socket stack";
        StringBuilder sb = new StringBuilder();
        sb.append("TCP ").append(cfg.tcpCongestion);
        sb.append(cfg.tcpNoDelay ? " NODELAY" : "");
        sb.append(cfg.tcpFastOpen ? " TFO" : "");
        sb.append(" buf ").append(cfg.socketBufferSizeKb).append("KB");
        sb.append(" · TLS ").append(cfg.httpsMinTls);
        sb.append(" ALPN ").append(cfg.httpsAlpn);
        sb.append(" · ports ");
        if (cfg.portAllowList != null && !cfg.portAllowList.trim().isEmpty()) {
            sb.append("allow ").append(cfg.portAllowList);
        } else {
            sb.append("open");
        }
        if (cfg.portBlockList != null && !cfg.portBlockList.trim().isEmpty()) {
            sb.append(" block ").append(cfg.portBlockList);
        }
        return sb.toString();
    }

    public static String[] enabledProtocols(SkidConfig cfg) {
        String min = (cfg != null && cfg.httpsMinTls != null) ? cfg.httpsMinTls : "TLS 1.2";
        String u = min.toUpperCase(Locale.US);
        if (u.contains("1.3") && !u.contains("1.2")) {
            return new String[]{"TLSv1.3"};
        }
        if (u.contains("1.3")) {
            return new String[]{"TLSv1.3", "TLSv1.2"};
        }
        return new String[]{"TLSv1.2"};
    }

    private static String peerName(SkidConfig cfg, String host, String sni) {
        if (sni != null && !sni.trim().isEmpty()) return sni.trim();
        if (cfg != null && cfg.httpsSniEnabled && cfg.sni != null && !cfg.sni.trim().isEmpty()) {
            return cfg.sni.trim();
        }
        return host;
    }

    private static String[] splitAlpn(String alpn) {
        String[] raw = alpn.split("[, ]+");
        List<String> out = new ArrayList<String>();
        for (int i = 0; i < raw.length; i++) {
            String t = raw[i].trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out.toArray(new String[out.size()]);
    }

    private static boolean isWellKnown(int port) {
        return port == 53 || port == 80 || port == 443 || port == 853
                || port == 123 || port == 22 || port == 993 || port == 995;
    }

    private static boolean listContainsPort(List<String> list, int port) {
        if (list == null) return false;
        for (int i = 0; i < list.size(); i++) {
            if (matchesToken(list.get(i), port)) return true;
        }
        return false;
    }

    private static boolean matchesList(String csv, int port) {
        if (csv == null || csv.trim().isEmpty()) return false;
        String[] parts = csv.split("[,;\\n]+");
        for (int i = 0; i < parts.length; i++) {
            if (matchesToken(parts[i], port)) return true;
        }
        return false;
    }

    private static void verifyPin(SSLSocket tls, SkidConfig cfg) throws Exception {
        if (cfg == null || cfg.httpsPinSha256 == null) return;
        String want = cfg.httpsPinSha256.trim().replace(":", "").replace(" ", "");
        if (want.isEmpty()) return;
        java.security.cert.Certificate[] chain = tls.getSession().getPeerCertificates();
        if (chain == null || chain.length == 0) {
            throw new SecurityException("HTTPS pin: empty peer chain");
        }
        java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(chain[0].getEncoded());
        StringBuilder hex = new StringBuilder();
        for (int i = 0; i < digest.length; i++) {
            hex.append(String.format(Locale.US, "%02x", digest[i] & 0xFF));
        }
        String b64 = android.util.Base64.encodeToString(digest, android.util.Base64.NO_WRAP);
        if (want.equalsIgnoreCase(hex.toString())
                || want.equals(b64)
                || want.equals("sha256/" + b64)
                || want.equalsIgnoreCase("sha256:" + hex.toString())) {
            return;
        }
        throw new SecurityException("HTTPS pin mismatch");
    }

    public static boolean matchesToken(String token, int port) {
        if (token == null) return false;
        String t = token.trim();
        if (t.isEmpty()) return false;
        int dash = t.indexOf('-');
        try {
            if (dash > 0) {
                int a = Integer.parseInt(t.substring(0, dash).trim());
                int b = Integer.parseInt(t.substring(dash + 1).trim());
                if (a > b) { int tmp = a; a = b; b = tmp; }
                return port >= a && port <= b;
            }
            return Integer.parseInt(t) == port;
        } catch (Exception e) {
            return false;
        }
    }
}
