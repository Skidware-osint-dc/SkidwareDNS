package com.skidwaredns.app.tools;

import com.skidwaredns.app.proxy.OpenPortEngine;
import com.skidwaredns.app.util.SkidEventBus;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * LAN discovery: /24 TCP probes + mDNS + SSDP (network-scanner style, no root).
 */
public final class LanScannerEngine {

    private static final LanScannerEngine INSTANCE = new LanScannerEngine();

    public static final class Device {
        public final String ip;
        public String hostname = "";
        public final List<Integer> openPorts = new ArrayList<Integer>();
        public String extra = "";

        public Device(String ip) { this.ip = ip; }

        public String title() {
            if (hostname != null && hostname.length() > 0) return hostname + "  (" + ip + ")";
            return ip;
        }

        public String portsLine() {
            if (openPorts.isEmpty()) return extra.length() == 0 ? "up (no probed ports open)" : extra;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < openPorts.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(openPorts.get(i));
            }
            if (extra.length() > 0) sb.append("  ·  ").append(extra);
            return sb.toString();
        }
    }

    private final Map<String, Device> last = new LinkedHashMap<String, Device>();
    private volatile boolean running;
    private volatile String summary = "Idle.";

    public static LanScannerEngine get() { return INSTANCE; }

    private LanScannerEngine() {}

    public boolean isRunning() { return running; }

    public String summary() { return summary; }

    public synchronized List<Device> lastDevices() {
        List<Device> out = new ArrayList<Device>(last.values());
        Collections.sort(out, new Comparator<Device>() {
            @Override public int compare(Device a, Device b) {
                return compareIp(a.ip, b.ip);
            }
        });
        return out;
    }

    public List<Device> scan(String portList, int timeoutMs) {
        running = true;
        Map<String, Device> found = Collections.synchronizedMap(new LinkedHashMap<String, Device>());
        try {
            String self = OpenPortEngine.primaryDeviceIpv4();
            List<String> hosts = ToolNet.lanHosts24();
            if (hosts.isEmpty()) {
                summary = "No LAN IPv4. Turn on Wi-Fi.";
                return store(found);
            }
            Device me = new Device(self);
            me.hostname = "this device";
            found.put(self, me);

            List<Integer> ports = ToolNet.parsePorts(portList);
            if (ports.isEmpty()) {
                ports.add(Integer.valueOf(22));
                ports.add(Integer.valueOf(80));
                ports.add(Integer.valueOf(443));
                ports.add(Integer.valueOf(445));
                ports.add(Integer.valueOf(8080));
            }
            int timeout = Math.max(80, Math.min(timeoutMs, 1500));
            summary = "Sweeping " + hosts.size() + " hosts × " + ports.size() + " ports...";
            ExecutorService pool = Executors.newFixedThreadPool(48);
            final CountDownLatch latch = new CountDownLatch(hosts.size());
            final List<Integer> portsF = ports;
            for (int i = 0; i < hosts.size(); i++) {
                final String ip = hosts.get(i);
                pool.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Device d = probeHost(ip, portsF, timeout);
                            if (d != null) found.put(ip, d);
                        } finally {
                            latch.countDown();
                        }
                    }
                });
            }
            mdns(found);
            ssdp(found);
            try { latch.await(25, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
            pool.shutdownNow();
            summary = found.size() + " device(s) on " + ToolNet.guessLanCidr();
            SkidEventBus.logSystem("SCAN", self, summary);
            return store(found);
        } finally {
            running = false;
        }
    }

    private Device probeHost(String ip, List<Integer> ports, int timeout) {
        Device d = null;
        for (int i = 0; i < ports.size(); i++) {
            int port = ports.get(i).intValue();
            Socket s = null;
            try {
                s = new Socket();
                s.connect(new InetSocketAddress(ip, port), timeout);
                if (d == null) d = new Device(ip);
                d.openPorts.add(Integer.valueOf(port));
            } catch (Exception ignored) {
            } finally {
                if (s != null) try { s.close(); } catch (Exception ignored) {}
            }
        }
        if (d != null) {
            d.hostname = ToolNet.resolveName(ip);
        }
        return d;
    }

    private void mdns(Map<String, Device> found) {
        MulticastSocket ms = null;
        try {
            ms = new MulticastSocket();
            ms.setSoTimeout(600);
            byte[] q = mdnsQuery();
            InetAddress group = InetAddress.getByName("224.0.0.251");
            ms.send(new DatagramPacket(q, q.length, group, 5353));
            long end = System.currentTimeMillis() + 700;
            byte[] buf = new byte[512];
            while (System.currentTimeMillis() < end) {
                DatagramPacket in = new DatagramPacket(buf, buf.length);
                try { ms.receive(in); } catch (Exception e) { break; }
                String ip = in.getAddress().getHostAddress();
                Device d = found.get(ip);
                if (d == null) {
                    d = new Device(ip);
                    found.put(ip, d);
                }
                if (d.extra.indexOf("mDNS") < 0) {
                    d.extra = d.extra.length() == 0 ? "mDNS" : d.extra + ", mDNS";
                }
                if (d.hostname.length() == 0) d.hostname = ToolNet.resolveName(ip);
            }
        } catch (Exception ignored) {
        } finally {
            if (ms != null) try { ms.close(); } catch (Exception ignored) {}
        }
    }

    private void ssdp(Map<String, Device> found) {
        DatagramSocket ds = null;
        try {
            ds = new DatagramSocket();
            ds.setSoTimeout(500);
            String req = "M-SEARCH * HTTP/1.1\r\nHOST: 239.255.255.250:1900\r\n"
                    + "MAN: \"ssdp:discover\"\r\nMX: 1\r\nST: ssdp:all\r\n\r\n";
            byte[] payload = req.getBytes(Charset.forName("US-ASCII"));
            InetAddress group = InetAddress.getByName("239.255.255.250");
            ds.send(new DatagramPacket(payload, payload.length, group, 1900));
            long end = System.currentTimeMillis() + 600;
            byte[] buf = new byte[1024];
            while (System.currentTimeMillis() < end) {
                DatagramPacket in = new DatagramPacket(buf, buf.length);
                try { ds.receive(in); } catch (Exception e) { break; }
                String ip = in.getAddress().getHostAddress();
                Device d = found.get(ip);
                if (d == null) {
                    d = new Device(ip);
                    found.put(ip, d);
                }
                if (d.extra.indexOf("SSDP") < 0) {
                    d.extra = d.extra.length() == 0 ? "SSDP/UPnP" : d.extra + ", SSDP/UPnP";
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (ds != null) try { ds.close(); } catch (Exception ignored) {}
        }
    }

    /** PTR _services._dns-sd._udp.local */
    private static byte[] mdnsQuery() {
        return new byte[]{
                0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x09, '_', 's', 'e', 'r', 'v', 'i', 'c', 'e', 's',
                0x07, '_', 'd', 'n', 's', '-', 's', 'd',
                0x04, '_', 'u', 'd', 'p',
                0x05, 'l', 'o', 'c', 'a', 'l',
                0x00, 0x00, 0x0c, 0x00, 0x01
        };
    }

    private synchronized List<Device> store(Map<String, Device> found) {
        last.clear();
        last.putAll(found);
        return lastDevices();
    }

    private static int compareIp(String a, String b) {
        try {
            String[] aa = a.split("\\.");
            String[] bb = b.split("\\.");
            for (int i = 0; i < 4; i++) {
                int d = Integer.parseInt(aa[i]) - Integer.parseInt(bb[i]);
                if (d != 0) return d;
            }
        } catch (Exception ignored) {}
        return a.compareTo(b);
    }
}
