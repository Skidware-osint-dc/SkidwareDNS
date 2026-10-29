package com.skidwaredns.app.tunnel;

import com.skidwaredns.app.config.SkidConfig;
import com.skidwaredns.app.model.DnsPresets;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * SkidwareDNS v4.0 HTTP Injector Engine Helpers:
 *  1. Share URI Importer & Exporter (vless://, vmess://, trojan://, ss://, hy2://)
 *  2. Bitwise IPv4 CIDR Custom Routes & Excluded Routes (10.0.0.0/8;172.16.0.0/12;192.168.0.0/16) calculator
 *  3. HTTP Ping Connectivity Probe (clients3.google.com/generate_204)
 *  4. SSH Header Replacer ("Replace header status to 200 OK")
 *  5. Tunnel Type Matrix compatibility rules & Selected Tunnel Type banner formatter
 */
public final class XrayUriAndRouteHelper {

    public static final class CidrBlock {
        public final int netInt;
        public final int prefix;

        public CidrBlock(int netInt, int prefix) {
            this.prefix = Math.max(0, Math.min(32, prefix));
            int mask = this.prefix == 0 ? 0 : (-1 << (32 - this.prefix));
            this.netInt = netInt & mask;
        }

        public String ipString() {
            return ((netInt >>> 24) & 0xFF) + "."
                    + ((netInt >>> 16) & 0xFF) + "."
                    + ((netInt >>> 8) & 0xFF) + "."
                    + (netInt & 0xFF);
        }

        @Override
        public String toString() {
            return ipString() + "/" + prefix;
        }
    }

    public static final class PingResult {
        public final boolean ok;
        public final int statusCode;
        public final long latencyMs;
        public final String summary;

        public PingResult(boolean ok, int statusCode, long latencyMs, String summary) {
            this.ok = ok;
            this.statusCode = statusCode;
            this.latencyMs = latencyMs;
            this.summary = summary;
        }
    }

    private XrayUriAndRouteHelper() {}

    // =========================================================================
    // 1. HTTP INJECTOR TUNNEL MATRIX & SELECTED BANNER FORMATTER
    // =========================================================================
    public static boolean isConnectFromAllowed(String tunnelType, String connectFrom) {
        String t = tunnelType == null ? "SSH" : tunnelType.trim();
        String cf = connectFrom == null ? "Direct" : connectFrom.trim();
        boolean isDirect = "Direct".equalsIgnoreCase(cf) || "None (Direct)".equalsIgnoreCase(cf);
        if (isDirect) return true;

        if ("Hysteria".equalsIgnoreCase(t) || "Hysteria2".equalsIgnoreCase(t) || "V2Ray/Xray".equalsIgnoreCase(t)) {
            return false; // Per HTTP Injector matrix (184214 & 184228), Hysteria & V2Ray/Xray lock Connect From to None (Direct)
        }
        if ("Shadowsocks".equalsIgnoreCase(t)) {
            // Per 184221: Shadowsocks enables None (Direct), HTTP (Obfs), TLS/SSL (Obfs)
            return "HTTP (Obfs)".equalsIgnoreCase(cf) || "TLS/SSL (Obfs)".equalsIgnoreCase(cf);
        }
        // SSH (184206): enables None (Direct), DNS (DNSTT), HTTP Proxy, TLS/SSL Proxy, TLS/SSL (stunnel)
        if ("SSH".equalsIgnoreCase(t)) {
            return "DNS (DNSTT)".equalsIgnoreCase(cf)
                    || "Http proxy".equalsIgnoreCase(cf)
                    || "HTTP Proxy".equalsIgnoreCase(cf)
                    || "TLS/SSL Proxy".equalsIgnoreCase(cf)
                    || "TLS/SSL (stunnel)".equalsIgnoreCase(cf)
                    || "HTTPS Jump".equalsIgnoreCase(cf)
                    || "SSH Jump".equalsIgnoreCase(cf);
        }
        return true;
    }

    public static boolean isCustomPayloadAllowed(String tunnelType, String connectFrom) {
        String t = tunnelType == null ? "" : tunnelType.trim();
        if (!"SSH".equalsIgnoreCase(t)) return false;
        String cf = connectFrom == null ? "Direct" : connectFrom.trim();
        return !"DNS (DNSTT)".equalsIgnoreCase(cf) && !"TLS/SSL (stunnel)".equalsIgnoreCase(cf);
    }

    public static String formatSelectedTunnelHeadline(SkidConfig cfg) {
        String t = (cfg == null || cfg.tunnel == null) ? "SSH" : cfg.tunnel.trim();
        String cf = (cfg == null || cfg.connectFromMode == null) ? "Direct" : cfg.connectFromMode.trim();
        boolean isDirect = "Direct".equalsIgnoreCase(cf) || "None (Direct)".equalsIgnoreCase(cf);

        if ("Hysteria".equalsIgnoreCase(t) || "Hysteria2".equalsIgnoreCase(t)) {
            return "Hysteria (v" + (cfg != null ? cfg.hysteriaVersion : 2) + ")";
        }
        if ("V2Ray/Xray".equalsIgnoreCase(t)) {
            return "V2Ray/Xray (" + (cfg != null ? cfg.xrayProtocol : "VMess") + ")";
        }
        if ("Shadowsocks".equalsIgnoreCase(t)) {
            return isDirect ? "Shadowsocks (Direct)" : (cf + " → Shadowsocks");
        }
        if ("SSH".equalsIgnoreCase(t)) {
            String prettyCf = "Http proxy".equalsIgnoreCase(cf) ? "HTTP Proxy" : cf;
            return isDirect ? "SSH (Direct)" : (prettyCf + " → SSH");
        }
        return isDirect ? t : (cf + " → " + t);
    }

    public static String formatSelectedTunnelBadge(SkidConfig cfg) {
        String t = (cfg == null || cfg.tunnel == null) ? "SSH" : cfg.tunnel.trim();
        String cf = (cfg == null || cfg.connectFromMode == null) ? "Direct" : cfg.connectFromMode.trim();

        if ("Hysteria".equalsIgnoreCase(t) || "Hysteria2".equalsIgnoreCase(t) || "SlowUDP".equalsIgnoreCase(t)) {
            return "UDP";
        }
        if ("V2Ray/Xray".equalsIgnoreCase(t)) {
            return "TCP (HTTP2/WS) | UDP (QUIC/mKCP)";
        }
        if ("Shadowsocks".equalsIgnoreCase(t)) {
            return "TCP (SOCKS)";
        }
        if ("SSH".equalsIgnoreCase(t)) {
            if ("DNS (DNSTT)".equalsIgnoreCase(cf)) return "UDP (DNS)";
            if (cf.toUpperCase(Locale.US).contains("TLS")) return "TCP (TLS/SSL)";
            if (cf.toUpperCase(Locale.US).contains("HTTP")) return "TCP (HTTP)";
            return "TCP (SSH)";
        }
        if ("MASQUE-H3".equalsIgnoreCase(t)) return "UDP (HTTP/3)";
        if ("WireGuard".equalsIgnoreCase(t)) return "UDP (WG)";
        return "TCP / UDP";
    }

    // =========================================================================
    // 2. SHARE URI IMPORTER & EXPORTER (vless://, vmess://, trojan://, ss://, hy2://)
    // =========================================================================
    public static String importShareUri(String rawUri, SkidConfig cfg) {
        if (rawUri == null || rawUri.trim().isEmpty()) {
            throw new IllegalArgumentException("Empty share URI");
        }
        String u = rawUri.trim();
        if (u.startsWith("vmess://")) {
            return importVmessBase64(u.substring(8), cfg);
        }
        if (u.startsWith("vless://") || u.startsWith("trojan://")) {
            boolean isVless = u.startsWith("vless://");
            URI parsed = URI.create(u);
            cfg.tunnel = "V2Ray/Xray";
            cfg.xrayProtocol = isVless ? "VLESS" : "Trojan";
            cfg.xrayUuid = parsed.getUserInfo() != null ? decodePct(parsed.getUserInfo()) : cfg.xrayUuid;
            cfg.tunnelHost = parsed.getHost() != null ? parsed.getHost() : "";
            cfg.tunnelPort = parsed.getPort() > 0 ? parsed.getPort() : 443;
            Map<String, String> q = parseQueryMap(parsed.getRawQuery());
            if (q.containsKey("security")) {
                String sec = q.get("security");
                if ("reality".equalsIgnoreCase(sec)) cfg.xraySecurity = "Reality";
                else if ("tls".equalsIgnoreCase(sec)) cfg.xraySecurity = "TLS";
                else cfg.xraySecurity = "None";
            }
            if (q.containsKey("type")) {
                cfg.xrayNetworkType = mapShortNetToDisplay(q.get("type"));
                cfg.v2rayTransport = q.get("type");
            }
            if (q.containsKey("sni")) cfg.sni = q.get("sni");
            if (q.containsKey("pbk")) cfg.xrayRealityPubKey = q.get("pbk");
            if (q.containsKey("sid")) cfg.xrayRealityShortId = q.get("sid");
            if (q.containsKey("headerType")) cfg.xrayHeaderType = "http".equalsIgnoreCase(q.get("headerType")) ? "HTTP" : "None";
            return "Imported " + cfg.xrayProtocol + " [" + cfg.tunnelHost + ":" + cfg.tunnelPort + " • " + cfg.xraySecurity + "/" + cfg.xrayNetworkType + "]";
        }
        if (u.startsWith("hy2://") || u.startsWith("hysteria2://")) {
            String norm = u.startsWith("hy2://") ? "https://" + u.substring(6) : "https://" + u.substring(12);
            URI parsed = URI.create(norm);
            cfg.tunnel = "Hysteria2";
            cfg.hysteriaVersion = 2;
            cfg.tunnelAuth = parsed.getUserInfo() != null ? decodePct(parsed.getUserInfo()) : "";
            cfg.tunnelHost = parsed.getHost() != null ? parsed.getHost() : "";
            cfg.tunnelPort = parsed.getPort() > 0 ? parsed.getPort() : 443;
            Map<String, String> q = parseQueryMap(parsed.getRawQuery());
            if (q.containsKey("sni")) cfg.sni = q.get("sni");
            if (q.containsKey("obfs-password")) cfg.hysteriaObfsPassword = q.get("obfs-password");
            if (q.containsKey("insecure")) cfg.hysteriaAllowInsecure = "1".equals(q.get("insecure")) || "true".equalsIgnoreCase(q.get("insecure"));
            if (q.containsKey("alpn")) cfg.hysteriaAlpn = q.get("alpn");
            return "Imported Hysteria2 [" + cfg.tunnelHost + ":" + cfg.tunnelPort + "]";
        }
        if (u.startsWith("ss://")) {
            String body = u.substring(5);
            int hash = body.indexOf('#');
            if (hash >= 0) {
                String tag = decodePct(body.substring(hash + 1));
                if (!tag.isEmpty()) cfg.name = tag;
                body = body.substring(0, hash);
            }
            String rawQuery = "";
            int qIdx = body.indexOf('?');
            if (qIdx >= 0) {
                rawQuery = body.substring(qIdx + 1);
                body = body.substring(0, qIdx);
            }
            int at = body.indexOf('@');
            if (at < 0) {
                String decodedAll = decodeB64Tolerant(body);
                if (decodedAll.contains("@")) {
                    body = decodedAll;
                    at = body.indexOf('@');
                }
            }
            if (at > 0) {
                String rawUser = body.substring(0, at);
                String cred = rawUser.contains(":") ? decodePct(rawUser) : decodeB64Tolerant(rawUser);
                String hostPortPart = body.substring(at + 1);
                if (hostPortPart.endsWith("/")) hostPortPart = hostPortPart.substring(0, hostPortPart.length() - 1);
                DnsPresets.HostPort hp = DnsPresets.parseHostPort(hostPortPart, 8388);
                int colon = cred.indexOf(':');
                if (colon > 0) {
                    cfg.tunnelCipher = cred.substring(0, colon);
                    cfg.tunnelAuth = cred.substring(colon + 1);
                }
                Map<String, String> qm = parseQueryMap(rawQuery);
                if (qm.containsKey("plugin")) {
                    cfg.ssObfsServer = qm.get("plugin");
                    if (cfg.ssObfsServer.toLowerCase(Locale.US).contains("tls")) {
                        cfg.connectFromMode = "TLS/SSL (Obfs)";
                    } else if (cfg.ssObfsServer.toLowerCase(Locale.US).contains("http")) {
                        cfg.connectFromMode = "HTTP (Obfs)";
                    }
                }
                cfg.tunnel = "Shadowsocks";
                cfg.tunnelHost = hp.host;
                cfg.tunnelPort = hp.port;
                return "Imported Shadowsocks [" + cfg.tunnelHost + ":" + cfg.tunnelPort + " • " + cfg.tunnelCipher + "]";
            }
        }
        throw new IllegalArgumentException("Unsupported URI scheme (expected vless://, vmess://, trojan://, ss://, or hy2://)");
    }

    private static String importVmessBase64(String b64, SkidConfig cfg) {
        String json = decodeB64Tolerant(b64);
        cfg.tunnel = "V2Ray/Xray";
        cfg.xrayProtocol = "VMess";
        String add = extractJsonString(json, "add");
        String port = extractJsonString(json, "port");
        String id = extractJsonString(json, "id");
        String aid = extractJsonString(json, "aid");
        String net = extractJsonString(json, "net");
        String tls = extractJsonString(json, "tls");
        String sni = extractJsonString(json, "sni");
        if (!add.isEmpty()) cfg.tunnelHost = add;
        if (!port.isEmpty()) {
            try { cfg.tunnelPort = Integer.parseInt(port); } catch (Exception ignored) {}
        }
        if (!id.isEmpty()) cfg.xrayUuid = id;
        if (!aid.isEmpty()) {
            try { cfg.xrayAlterId = Integer.parseInt(aid); } catch (Exception ignored) {}
        }
        if (!net.isEmpty()) cfg.xrayNetworkType = mapShortNetToDisplay(net);
        cfg.xraySecurity = "tls".equalsIgnoreCase(tls) ? "TLS" : "None";
        if (!sni.isEmpty()) cfg.sni = sni;
        return "Imported VMess [" + cfg.tunnelHost + ":" + cfg.tunnelPort + " • " + cfg.xrayNetworkType + "]";
    }

    public static String exportShareUri(SkidConfig cfg) {
        String host = (cfg.tunnelHost == null || cfg.tunnelHost.isEmpty()) ? "edge.skidware.example" : cfg.tunnelHost;
        int port = cfg.tunnelPort > 0 ? cfg.tunnelPort : 443;
        if ("Shadowsocks".equalsIgnoreCase(cfg.tunnel)) {
            String method = (cfg.tunnelCipher == null || cfg.tunnelCipher.isEmpty())
                    ? "2022-blake3-aes-256-gcm" : cfg.tunnelCipher;
            String pass = (cfg.tunnelAuth == null || cfg.tunnelAuth.isEmpty())
                    ? "skid-ss-secret" : cfg.tunnelAuth;
            int ssPort = cfg.tunnelPort > 0 ? cfg.tunnelPort : 8388;
            String userInfo = method.startsWith("2022-blake3-")
                    ? (encodePct(method) + ":" + encodePct(pass))
                    : Base64.getUrlEncoder().withoutPadding().encodeToString(
                            (method + ":" + pass).getBytes(StandardCharsets.UTF_8));
            String pluginQ = (cfg.ssObfsServer != null && !cfg.ssObfsServer.trim().isEmpty())
                    ? ("/?plugin=" + encodePct(cfg.ssObfsServer.trim())) : "";
            return "ss://" + userInfo + "@" + host + ":" + ssPort + pluginQ + "#" + encodePct(cfg.name);
        }
        if ("Hysteria".equalsIgnoreCase(cfg.tunnel) || "Hysteria2".equalsIgnoreCase(cfg.tunnel)) {
            return "hy2://" + encodePct(cfg.tunnelAuth.isEmpty() ? "skid-auth" : cfg.tunnelAuth)
                    + "@" + host + ":" + port
                    + "/?sni=" + encodePct(cfg.sni)
                    + "&alpn=" + encodePct(cfg.hysteriaAlpn)
                    + "&insecure=" + (cfg.hysteriaAllowInsecure ? "1" : "0")
                    + (cfg.hysteriaObfsPassword.isEmpty() ? "" : "&obfs=salamander&obfs-password=" + encodePct(cfg.hysteriaObfsPassword))
                    + "#" + encodePct(cfg.name);
        }
        String sec = cfg.xraySecurity == null ? "none" : cfg.xraySecurity.toLowerCase(Locale.US);
        String net = mapDisplayNetToShort(cfg.xrayNetworkType);
        if ("Trojan".equalsIgnoreCase(cfg.xrayProtocol)) {
            return "trojan://" + encodePct(cfg.xrayUuid) + "@" + host + ":" + port
                    + "?security=" + sec + "&type=" + net + "&sni=" + encodePct(cfg.sni)
                    + "#" + encodePct(cfg.name);
        }
        return "vless://" + encodePct(cfg.xrayUuid) + "@" + host + ":" + port
                + "?encryption=none&security=" + sec + "&type=" + net + "&sni=" + encodePct(cfg.sni)
                + ("reality".equals(sec) ? "&pbk=" + encodePct(cfg.xrayRealityPubKey) + "&sid=" + encodePct(cfg.xrayRealityShortId) : "")
                + "#" + encodePct(cfg.name);
    }

    public static String mapDisplayNetToShort(String disp) {
        if (disp == null) return "tcp";
        String d = disp.trim().toLowerCase(Locale.US);
        if (d.contains("websocket") || "ws".equals(d)) return "ws";
        if (d.contains("mkcp") || "kcp".equals(d)) return "kcp";
        if (d.contains("upgrade")) return "httpupgrade";
        if (d.contains("http/2") || "h2".equals(d)) return "h2";
        if (d.contains("xhttp")) return "xhttp";
        if (d.contains("quic")) return "quic";
        if (d.contains("grpc")) return "grpc";
        return "tcp";
    }

    public static String mapShortNetToDisplay(String s) {
        if (s == null) return "TCP";
        String d = s.trim().toLowerCase(Locale.US);
        switch (d) {
            case "ws": return "WebSocket";
            case "kcp":
            case "mkcp": return "mKCP";
            case "httpupgrade": return "HTTP Upgrade";
            case "h2":
            case "http": return "HTTP/2";
            case "xhttp":
            case "splithttp": return "XHTTP";
            case "quic": return "QUIC";
            case "grpc": return "gRPC";
            default: return "TCP";
        }
    }

    // =========================================================================
    // 3. BITWISE CIDR CUSTOM ROUTES & EXCLUDED ROUTES SUBTRACTION
    // =========================================================================
    public static List<CidrBlock> parseCidrList(String spec) {
        List<CidrBlock> out = new ArrayList<>();
        if (spec == null || spec.trim().isEmpty()) return out;
        for (String part : spec.split("[;,\\s]+")) {
            String t = part.trim();
            if (t.isEmpty() || t.contains(":")) continue;
            try {
                int slash = t.indexOf('/');
                String ip = slash > 0 ? t.substring(0, slash) : t;
                int pfx = slash > 0 ? Integer.parseInt(t.substring(slash + 1)) : 32;
                String[] oct = ip.split("\\.");
                if (oct.length == 4) {
                    int n = ((Integer.parseInt(oct[0]) & 0xFF) << 24)
                            | ((Integer.parseInt(oct[1]) & 0xFF) << 16)
                            | ((Integer.parseInt(oct[2]) & 0xFF) << 8)
                            | (Integer.parseInt(oct[3]) & 0xFF);
                    out.add(new CidrBlock(n, pfx));
                }
            } catch (Exception ignored) {
            }
        }
        return out;
    }

    public static List<CidrBlock> computeEffectiveIpv4Routes(boolean useDefault, String customSpec, String excludedSpec) {
        List<CidrBlock> base = new ArrayList<>();
        if (useDefault) {
            List<CidrBlock> custom = parseCidrList(customSpec);
            if (custom.isEmpty()) {
                base.add(new CidrBlock(0, 0));
            } else {
                base.addAll(custom);
            }
        } else {
            base.addAll(parseCidrList(customSpec));
        }
        List<CidrBlock> excluded = parseCidrList(excludedSpec);
        for (CidrBlock ex : excluded) {
            List<CidrBlock> next = new ArrayList<>();
            for (CidrBlock cur : base) {
                subtractCidr(cur, ex, next);
            }
            base = next;
        }
        return base;
    }

    private static void subtractCidr(CidrBlock target, CidrBlock ex, List<CidrBlock> out) {
        if (!overlaps(target, ex)) {
            out.add(target);
            return;
        }
        if (ex.prefix <= target.prefix) {
            // Excluded block completely covers target block
            return;
        }
        // Split target into two (target.prefix + 1) halves and recurse
        int nextPfx = target.prefix + 1;
        CidrBlock left = new CidrBlock(target.netInt, nextPfx);
        int bit = 1 << (32 - nextPfx);
        CidrBlock right = new CidrBlock(target.netInt | bit, nextPfx);
        subtractCidr(left, ex, out);
        subtractCidr(right, ex, out);
    }

    private static boolean overlaps(CidrBlock a, CidrBlock b) {
        int minPfx = Math.min(a.prefix, b.prefix);
        if (minPfx == 0) return true;
        int mask = -1 << (32 - minPfx);
        return (a.netInt & mask) == (b.netInt & mask);
    }

    // =========================================================================
    // 4. HTTP PING CONNECTIVITY PROBE (clients3.google.com/generate_204)
    // =========================================================================
    public static PingResult runHttpPingGenerate204(SkidConfig cfg) {
        String raw = (cfg != null && cfg.httpPingUrl != null && !cfg.httpPingUrl.trim().isEmpty())
                ? cfg.httpPingUrl.trim() : "clients3.google.com/generate_204";
        String urlStr = (raw.startsWith("http://") || raw.startsWith("https://")) ? raw : ("http://" + raw);
        long t0 = System.currentTimeMillis();
        HttpURLConnection conn = null;
        try {
            URL u = new URL(urlStr);
            conn = (HttpURLConnection) u.openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(4000);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "SkidwareDNS/4.0 (HTTP-Ping)");
            int code = conn.getResponseCode();
            long ms = System.currentTimeMillis() - t0;
            boolean ok = (code == 204 || code == 200);
            return new PingResult(ok, code, ms,
                    "HTTP Ping " + raw + " -> HTTP " + code + " (" + ms + " ms)");
        } catch (Exception e) {
            long ms = System.currentTimeMillis() - t0;
            return new PingResult(false, -1, ms,
                    "HTTP Ping FAILED (" + e.getClass().getSimpleName() + ": " + e.getMessage() + ")");
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    // =========================================================================
    // 5. SSH HEADER REPLACER & REGIONAL ROUTING HELPER
    // =========================================================================
    public static byte[] normalizeProxyResponseStatus200Ok(byte[] resp, int len) {
        if (resp == null || len < 12) return resp;
        String text = new String(resp, 0, len, StandardCharsets.ISO_8859_1);
        int crlf = text.indexOf("\r\n");
        if (crlf > 0 && text.startsWith("HTTP/1.")) {
            String firstLine = text.substring(0, crlf);
            if (!firstLine.contains("200 ")) {
                String rewritten = "HTTP/1.1 200 OK" + text.substring(crlf);
                return rewritten.getBytes(StandardCharsets.ISO_8859_1);
            }
        }
        return resp;
    }

    public static String generateShadowsocks2022Psk(String method) {
        int keyLen = (method != null && method.contains("aes-128")) ? 16 : 32;
        byte[] raw = new byte[keyLen];
        new java.security.SecureRandom().nextBytes(raw);
        return Base64.getEncoder().encodeToString(raw);
    }

    public static boolean shouldBypassRegionalRouting(String host, String rule) {
        if (host == null || rule == null) return false;
        String h = host.toLowerCase(Locale.US);
        if (rule.contains("(CN)")) return h.endsWith(".cn") || h.endsWith(".baidu.com") || h.endsWith(".qq.com");
        if (rule.contains("(IR)")) return h.endsWith(".ir") || h.endsWith(".shaparak.ir");
        if (rule.contains("(RU)")) return h.endsWith(".ru") || h.endsWith(".yandex.ru") || h.endsWith(".vk.com");
        if (rule.contains("(SA)")) return h.endsWith(".sa") || h.endsWith(".gov.sa");
        return false;
    }

    // === Small String Parsers ===
    private static Map<String, String> parseQueryMap(String rawQuery) {
        Map<String, String> m = new HashMap<>();
        if (rawQuery == null || rawQuery.isEmpty()) return m;
        for (String pair : rawQuery.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0) {
                m.put(decodePct(pair.substring(0, eq)), decodePct(pair.substring(eq + 1)));
            }
        }
        return m;
    }

    private static String decodePct(String s) {
        try { return URLDecoder.decode(s, "UTF-8"); } catch (Exception e) { return s; }
    }

    private static String encodePct(String s) {
        try { return URLEncoder.encode(s == null ? "" : s, "UTF-8"); } catch (Exception e) { return ""; }
    }

    private static String decodeB64Tolerant(String b64) {
        try {
            String norm = b64.trim().replace('-', '+').replace('_', '/');
            while (norm.length() % 4 != 0) norm += "=";
            byte[] dec = Base64.getDecoder().decode(norm);
            return new String(dec, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    private static String extractJsonString(String json, String key) {
        String pat = "\"" + key + "\"";
        int idx = json.indexOf(pat);
        if (idx < 0) return "";
        int colon = json.indexOf(':', idx + pat.length());
        if (colon < 0) return "";
        int p = colon + 1;
        while (p < json.length() && Character.isWhitespace(json.charAt(p))) p++;
        if (p >= json.length()) return "";
        if (json.charAt(p) == '"') {
            int end = json.indexOf('"', p + 1);
            return end > p ? json.substring(p + 1, end) : "";
        }
        int end = p;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '.')) end++;
        return json.substring(p, end);
    }
}
