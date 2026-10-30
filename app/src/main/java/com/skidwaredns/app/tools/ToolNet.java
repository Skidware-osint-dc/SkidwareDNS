package com.skidwaredns.app.tools;

import com.skidwaredns.app.proxy.OpenPortEngine;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Shared parsers for the Tools hub (knock sequences, port lists, LAN /24).
 * Pure Java 8, no lambdas.
 */
public final class ToolNet {

    private ToolNet() {}

    public static final class KnockStep {
        public final int port;
        public final String proto; // TCP, UDP
        public final byte[] payload;

        public KnockStep(int port, String proto, byte[] payload) {
            this.port = port;
            this.proto = proto;
            this.payload = payload;
        }

        public String describe() {
            return proto + "/" + port;
        }
    }

    /** "7000,8000/udp,9000/tcp" or "7000 8000 9000". */
    public static List<KnockStep> parseKnock(String seq, String defaultProto, byte[] payload) {
        List<KnockStep> out = new ArrayList<KnockStep>();
        if (seq == null) return out;
        String def = defaultProto == null ? "TCP" : defaultProto.trim().toUpperCase(Locale.US);
        if (!"UDP".equals(def)) def = "TCP";
        byte[] pl = payload == null ? new byte[]{'S', 'K'} : payload;
        String[] parts = seq.split("[,;\\s]+");
        for (int i = 0; i < parts.length; i++) {
            String t = parts[i].trim();
            if (t.isEmpty()) continue;
            String proto = def;
            String num = t;
            int slash = t.indexOf('/');
            if (slash > 0) {
                num = t.substring(0, slash).trim();
                String p = t.substring(slash + 1).trim().toUpperCase(Locale.US);
                if (p.startsWith("U")) proto = "UDP";
                else proto = "TCP";
            }
            try {
                int port = Integer.parseInt(num);
                if (port >= 1 && port <= 65535) out.add(new KnockStep(port, proto, pl));
            } catch (Exception ignored) {}
        }
        return out;
    }

    public static List<Integer> parsePorts(String list) {
        List<Integer> out = new ArrayList<Integer>();
        if (list == null) return out;
        String[] parts = list.split("[,;\\s]+");
        for (int i = 0; i < parts.length; i++) {
            String t = parts[i].trim();
            if (t.isEmpty()) continue;
            int dash = t.indexOf('-');
            if (dash > 0) {
                try {
                    int a = Integer.parseInt(t.substring(0, dash).trim());
                    int b = Integer.parseInt(t.substring(dash + 1).trim());
                    if (a > b) { int tmp = a; a = b; b = tmp; }
                    if (b - a > 4096) b = a + 4096;
                    for (int p = a; p <= b; p++) {
                        if (p >= 1 && p <= 65535) out.add(Integer.valueOf(p));
                    }
                } catch (Exception ignored) {}
            } else {
                try {
                    int p = Integer.parseInt(t);
                    if (p >= 1 && p <= 65535) out.add(Integer.valueOf(p));
                } catch (Exception ignored) {}
            }
        }
        return out;
    }

    public static String lanPrefix24(String ipv4) {
        if (ipv4 == null) return null;
        int last = ipv4.lastIndexOf('.');
        if (last <= 0) return null;
        return ipv4.substring(0, last);
    }

    /** Hosts 1-254 of this device's /24, excluding itself. */
    public static List<String> lanHosts24() {
        String self = OpenPortEngine.primaryDeviceIpv4();
        String prefix = lanPrefix24(self);
        List<String> out = new ArrayList<String>();
        if (prefix == null) return out;
        String selfIp = self;
        for (int i = 1; i <= 254; i++) {
            String ip = prefix + "." + i;
            if (!ip.equals(selfIp)) out.add(ip);
        }
        return out;
    }

    public static String guessLanCidr() {
        String self = OpenPortEngine.primaryDeviceIpv4();
        String prefix = lanPrefix24(self);
        if (prefix == null) return "(no LAN IPv4)";
        return prefix + ".0/24  (this device " + self + ")";
    }

    public static String safeBanner(byte[] buf, int n) {
        if (buf == null || n <= 0) return "";
        StringBuilder sb = new StringBuilder();
        int cap = Math.min(n, 120);
        for (int i = 0; i < cap; i++) {
            int c = buf[i] & 0xFF;
            if (c == 10 || c == 13) break;
            if (c >= 32 && c < 127) sb.append((char) c);
            else sb.append('.');
        }
        return sb.toString().trim();
    }

    public static String resolveName(String ip) {
        try {
            InetAddress a = InetAddress.getByName(ip);
            String n = a.getCanonicalHostName();
            if (n != null && !n.equals(ip)) return n;
        } catch (Exception ignored) {}
        return "";
    }

    public static int clampPort(int p, int fallback) {
        if (p < 1 || p > 65535) return fallback;
        return p;
    }
}
