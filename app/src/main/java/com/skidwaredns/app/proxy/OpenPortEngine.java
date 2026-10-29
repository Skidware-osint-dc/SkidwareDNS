package com.skidwaredns.app.proxy;

import com.skidwaredns.app.config.SkidConfig;
import com.skidwaredns.app.util.SkidEventBus;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Binds user-picked TCP/UDP ports on the device LAN IP (or 0.0.0.0 / loopback).
 * Not a proxy: accepted TCP clients get a short banner describing the port, then
 * the socket closes unless Hold is on. UDP packets are logged.
 */
public final class OpenPortEngine {

    public static final int MAX_OPEN = 24;

    public static final class Session {
        public final int port;
        public final boolean tcp;
        public final boolean udp;
        public final String bindIp;
        public final long openedAtMs;
        public final AtomicInteger accepts = new AtomicInteger(0);
        public final AtomicInteger datagrams = new AtomicInteger(0);
        final AtomicBoolean live = new AtomicBoolean(true);
        ServerSocket tcpSock;
        DatagramSocket udpSock;
        Thread tcpThread;
        Thread udpThread;

        Session(int port, boolean tcp, boolean udp, String bindIp) {
            this.port = port;
            this.tcp = tcp;
            this.udp = udp;
            this.bindIp = bindIp;
            this.openedAtMs = System.currentTimeMillis();
        }

        public String token() {
            if (tcp && udp) return port + "/tcp+udp";
            if (udp) return port + "/udp";
            return port + "/tcp";
        }

        public String summary() {
            PortCatalog.Info info = PortCatalog.lookup(port);
            return bindIp + ":" + port
                    + (tcp ? " TCP" : "")
                    + (udp ? " UDP" : "")
                    + "  " + info.name
                    + "  acc=" + accepts.get()
                    + "  udp=" + datagrams.get();
        }
    }

    private static final OpenPortEngine INSTANCE = new OpenPortEngine();

    private final Map<Integer, Session> sessions = new LinkedHashMap<Integer, Session>();
    private volatile SkidConfig cfg;
    private volatile boolean banner = true;
    private volatile boolean hold = false;

    public static OpenPortEngine get() { return INSTANCE; }

    private OpenPortEngine() {}

    public synchronized void applyConfig(SkidConfig c) {
        this.cfg = c;
        if (c != null) {
            this.banner = c.openPortBanner;
            this.hold = c.openPortHold;
        }
    }

    public synchronized List<Session> list() {
        return new ArrayList<Session>(sessions.values());
    }

    public synchronized boolean isOpen(int port) {
        return sessions.containsKey(Integer.valueOf(port));
    }

    public synchronized String open(SkidConfig c, int port, boolean tcp, boolean udp) {
        applyConfig(c);
        if (port < PortCatalog.MIN || port > PortCatalog.MAX) {
            return "Port must be 1–10000.";
        }
        if (!tcp && !udp) return "Pick TCP, UDP, or both.";
        if (sessions.containsKey(Integer.valueOf(port))) {
            return "Port " + port + " is already open. Close it first.";
        }
        if (sessions.size() >= MAX_OPEN) {
            return "At most " + MAX_OPEN + " ports can be open at once. Close one first.";
        }
        if (c != null && c.privilegedPortProtect && port < 1024 && !c.allowWellKnownPorts) {
            return "Privileged-Port-Protect blocked :" + port
                    + " (well-known 1–1023). Turn on Allow well-known ports, or pick 1024+.";
        }

        String bindIp = resolveBindIp(c);
        Session s = new Session(port, tcp, udp, bindIp);
        String err = null;
        if (tcp) err = startTcp(s, c);
        if (err == null && udp) err = startUdp(s, c);
        if (err != null) {
            teardown(s);
            return err;
        }
        sessions.put(Integer.valueOf(port), s);
        remember(c, s);
        PortCatalog.Info info = PortCatalog.lookup(port);
        SkidEventBus.logSystem("PORT", bindIp + ":" + port,
                "OPEN " + (tcp ? "TCP " : "") + (udp ? "UDP " : "") + info.name + " — " + info.about);
        return "Opened " + s.summary();
    }

    public synchronized String close(int port, SkidConfig c) {
        Session s = sessions.remove(Integer.valueOf(port));
        if (s == null) return "Port " + port + " was not open.";
        teardown(s);
        forget(c, port);
        SkidEventBus.logSystem("PORT", s.bindIp + ":" + port, "CLOSED");
        return "Closed " + s.bindIp + ":" + port;
    }

    public synchronized void closeAll(SkidConfig c) {
        List<Session> copy = new ArrayList<Session>(sessions.values());
        sessions.clear();
        for (int i = 0; i < copy.size(); i++) teardown(copy.get(i));
        if (c != null) c.openListenPorts.clear();
    }

    public synchronized void restore(SkidConfig c) {
        applyConfig(c);
        if (c == null || c.openListenPorts.isEmpty()) return;
        List<String> tokens = new ArrayList<String>(c.openListenPorts);
        for (int i = 0; i < tokens.size(); i++) {
            String t = tokens.get(i);
            if (t == null) continue;
            Parsed p = parseToken(t);
            if (p == null) continue;
            if (sessions.containsKey(Integer.valueOf(p.port))) continue;
            open(c, p.port, p.tcp, p.udp);
        }
    }

    public static List<String> deviceIpv4s() {
        List<String> out = new ArrayList<String>();
        try {
            Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
            if (en == null) return out;
            List<NetworkInterface> ifs = Collections.list(en);
            for (int i = 0; i < ifs.size(); i++) {
                NetworkInterface ni = ifs.get(i);
                try {
                    if (ni.isLoopback() || !ni.isUp()) continue;
                } catch (Exception e) {
                    continue;
                }
                String n = ni.getName() == null ? "" : ni.getName().toLowerCase(Locale.US);
                if (n.startsWith("tun") || n.startsWith("ppp") || n.startsWith("dummy")) continue;
                Enumeration<InetAddress> addrs = ni.getInetAddresses();
                List<InetAddress> list = Collections.list(addrs);
                for (int a = 0; a < list.size(); a++) {
                    InetAddress ia = list.get(a);
                    if (ia instanceof Inet4Address && !ia.isLoopbackAddress()) {
                        out.add(ia.getHostAddress() + "  (" + ni.getName() + ")");
                    }
                }
            }
        } catch (Exception ignored) {}
        return out;
    }

    public static String primaryDeviceIpv4() {
        List<String> all = deviceIpv4s();
        String wlan = null;
        String any = null;
        for (int i = 0; i < all.size(); i++) {
            String row = all.get(i);
            int sp = row.indexOf(' ');
            String ip = sp > 0 ? row.substring(0, sp) : row;
            if (row.contains("wlan") || row.contains("wifi") || row.contains("ap0")) {
                if (wlan == null) wlan = ip;
            }
            if (any == null) any = ip;
        }
        if (wlan != null) return wlan;
        if (any != null) return any;
        return "0.0.0.0";
    }

    public static String describeBind(SkidConfig c) {
        String mode = c != null && c.openPortBindMode != null ? c.openPortBindMode : "Device-IP";
        if ("Loopback".equalsIgnoreCase(mode)) return "127.0.0.1 (this app only)";
        if ("0.0.0.0".equalsIgnoreCase(mode) || "All interfaces".equalsIgnoreCase(mode)) {
            return "0.0.0.0 (every interface)";
        }
        return primaryDeviceIpv4() + " (this device)";
    }

    private String resolveBindIp(SkidConfig c) {
        String mode = c != null && c.openPortBindMode != null ? c.openPortBindMode : "Device-IP";
        if ("Loopback".equalsIgnoreCase(mode)) return "127.0.0.1";
        if ("0.0.0.0".equalsIgnoreCase(mode) || "All interfaces".equalsIgnoreCase(mode)) return "0.0.0.0";
        String ip = primaryDeviceIpv4();
        if (ip == null || ip.isEmpty() || "0.0.0.0".equals(ip)) return "0.0.0.0";
        return ip;
    }

    private String startTcp(final Session s, SkidConfig c) {
        try {
            ServerSocket ss = new ServerSocket();
            SocketStack.applyServer(ss, c);
            ss.setReuseAddress(true);
            InetAddress addr = InetAddress.getByName(s.bindIp);
            ss.bind(new InetSocketAddress(addr, s.port), 32);
            s.tcpSock = ss;
            final OpenPortEngine self = this;
            Thread th = new Thread(new Runnable() {
                @Override
                public void run() {
                    self.acceptLoop(s);
                }
            }, "skid-open-tcp-" + s.port);
            th.setDaemon(true);
            s.tcpThread = th;
            th.start();
            return null;
        } catch (Exception e) {
            return "TCP bind " + s.bindIp + ":" + s.port + " failed: " + e.getMessage()
                    + (s.port < 1024 ? " (ports 1–1023 need root on Android)" : "");
        }
    }

    private String startUdp(final Session s, SkidConfig c) {
        try {
            DatagramSocket ds = new DatagramSocket(null);
            ds.setReuseAddress(true);
            InetAddress addr = InetAddress.getByName(s.bindIp);
            ds.bind(new InetSocketAddress(addr, s.port));
            SocketStack.applyUdp(ds, c);
            ds.setSoTimeout(1000);
            s.udpSock = ds;
            final OpenPortEngine self = this;
            Thread th = new Thread(new Runnable() {
                @Override
                public void run() {
                    self.udpLoop(s);
                }
            }, "skid-open-udp-" + s.port);
            th.setDaemon(true);
            s.udpThread = th;
            th.start();
            return null;
        } catch (Exception e) {
            return "UDP bind " + s.bindIp + ":" + s.port + " failed: " + e.getMessage()
                    + (s.port < 1024 ? " (ports 1–1023 need root on Android)" : "");
        }
    }

    private void acceptLoop(Session s) {
        ServerSocket ss = s.tcpSock;
        while (s.live.get() && ss != null && !ss.isClosed()) {
            Socket client = null;
            try {
                client = ss.accept();
                s.accepts.incrementAndGet();
                SocketStack.applyTcp(client, cfg);
                String peer = String.valueOf(client.getRemoteSocketAddress());
                PortCatalog.Info info = PortCatalog.lookup(s.port);
                SkidEventBus.logSystem("PORT", s.bindIp + ":" + s.port, "TCP accept " + peer + " (" + info.name + ")");
                if (banner) {
                    String msg = "SkidwareDNS open-port " + s.bindIp + ":" + s.port
                            + " " + info.name + " (" + info.proto + ")\r\n"
                            + info.about + "\r\n";
                    client.getOutputStream().write(msg.getBytes(StandardCharsets.US_ASCII));
                    client.getOutputStream().flush();
                }
                if (hold) {
                    try {
                        client.setSoTimeout(30000);
                        byte[] buf = new byte[256];
                        client.getInputStream().read(buf);
                    } catch (Exception ignored) {}
                }
            } catch (Exception e) {
                if (!s.live.get()) break;
            } finally {
                if (client != null) {
                    try { client.close(); } catch (Exception ignored) {}
                }
            }
        }
    }

    private void udpLoop(Session s) {
        DatagramSocket ds = s.udpSock;
        byte[] buf = new byte[1500];
        while (s.live.get() && ds != null && !ds.isClosed()) {
            try {
                DatagramPacket pkt = new DatagramPacket(buf, buf.length);
                ds.receive(pkt);
                s.datagrams.incrementAndGet();
                PortCatalog.Info info = PortCatalog.lookup(s.port);
                SkidEventBus.logSystem("PORT", s.bindIp + ":" + s.port,
                        "UDP " + pkt.getLength() + "B from " + pkt.getAddress().getHostAddress()
                                + ":" + pkt.getPort() + " (" + info.name + ")");
                if (banner) {
                    byte[] reply = ("SKID " + s.port + " " + info.name).getBytes(StandardCharsets.US_ASCII);
                    ds.send(new DatagramPacket(reply, reply.length, pkt.getAddress(), pkt.getPort()));
                }
            } catch (java.net.SocketTimeoutException ignored) {
            } catch (Exception e) {
                if (!s.live.get()) break;
            }
        }
    }

    private void teardown(Session s) {
        s.live.set(false);
        if (s.tcpSock != null) {
            try { s.tcpSock.close(); } catch (Exception ignored) {}
        }
        if (s.udpSock != null) {
            try { s.udpSock.close(); } catch (Exception ignored) {}
        }
        if (s.tcpThread != null) s.tcpThread.interrupt();
        if (s.udpThread != null) s.udpThread.interrupt();
    }

    private void remember(SkidConfig c, Session s) {
        if (c == null) return;
        String tok = s.token();
        for (int i = 0; i < c.openListenPorts.size(); i++) {
            if (tok.equalsIgnoreCase(c.openListenPorts.get(i))) return;
        }
        c.openListenPorts.add(tok);
    }

    private void forget(SkidConfig c, int port) {
        if (c == null) return;
        List<String> keep = new ArrayList<String>();
        for (int i = 0; i < c.openListenPorts.size(); i++) {
            String t = c.openListenPorts.get(i);
            Parsed p = parseToken(t);
            if (p == null || p.port != port) keep.add(t);
        }
        c.openListenPorts.clear();
        c.openListenPorts.addAll(keep);
    }

    private static Parsed parseToken(String raw) {
        if (raw == null) return null;
        String t = raw.trim().toLowerCase(Locale.US);
        if (t.isEmpty()) return null;
        int slash = t.indexOf('/');
        String num = slash < 0 ? t : t.substring(0, slash);
        String rest = slash < 0 ? "tcp" : t.substring(slash + 1);
        int port;
        try {
            port = Integer.parseInt(num.trim());
        } catch (Exception e) {
            return null;
        }
        boolean tcp = rest.contains("tcp") || rest.contains("both") || rest.contains("+");
        boolean udp = rest.contains("udp") || rest.contains("both") || rest.contains("+");
        if (!tcp && !udp) tcp = true;
        Parsed p = new Parsed();
        p.port = port;
        p.tcp = tcp;
        p.udp = udp;
        return p;
    }

    private static final class Parsed {
        int port;
        boolean tcp;
        boolean udp;
    }
}
