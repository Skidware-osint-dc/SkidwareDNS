package com.skidwaredns.app.tools;

import com.skidwaredns.app.proxy.PortCatalog;
import com.skidwaredns.app.util.SkidEventBus;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Advanced PortChecker-style range scan of one host. TCP connect;
 * UDP send-and-wait (open|filtered when silent).
 */
public final class PortCheckerEngine {

    public static final int MAX_PORTS = 4096;

    private static final PortCheckerEngine INSTANCE = new PortCheckerEngine();

    public static final class Hit {
        public final int port;
        public final String proto;
        public final String state; // open, closed, filtered
        public final String banner;
        public final String name;

        public Hit(int port, String proto, String state, String banner) {
            this.port = port;
            this.proto = proto;
            this.state = state;
            this.banner = banner == null ? "" : banner;
            PortCatalog.Info info = PortCatalog.lookup(port);
            this.name = info == null ? "" : info.name;
        }
    }

    private final List<Hit> last = new ArrayList<Hit>();
    private volatile boolean running;
    private volatile String summary = "Idle.";

    public static PortCheckerEngine get() { return INSTANCE; }

    private PortCheckerEngine() {}

    public boolean isRunning() { return running; }

    public String summary() { return summary; }

    public synchronized List<Hit> lastHits() {
        return new ArrayList<Hit>(last);
    }

    public List<Hit> scan(String host, int from, int to, boolean udp, int timeoutMs) {
        running = true;
        List<Hit> hits = Collections.synchronizedList(new ArrayList<Hit>());
        try {
            if (host == null || host.trim().isEmpty()) {
                summary = "Set a host.";
                return store(hits);
            }
            int a = Math.min(from, to);
            int b = Math.max(from, to);
            a = ToolNet.clampPort(a, 1);
            b = ToolNet.clampPort(b, 1024);
            if (b - a + 1 > MAX_PORTS) b = a + MAX_PORTS - 1;
            InetAddress addr;
            try {
                addr = InetAddress.getByName(host.trim());
            } catch (Exception e) {
                summary = "Cannot resolve " + host;
                return store(hits);
            }
            int n = b - a + 1;
            summary = "Scanning " + n + " ports on " + addr.getHostAddress() + "...";
            int timeout = Math.max(80, Math.min(timeoutMs, 3000));
            int threads = Math.min(64, Math.max(8, n / 8));
            ExecutorService pool = Executors.newFixedThreadPool(threads);
            final CountDownLatch latch = new CountDownLatch(n);
            final AtomicInteger open = new AtomicInteger(0);
            for (int p = a; p <= b; p++) {
                final int port = p;
                pool.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Hit h = udp ? probeUdp(addr, port, timeout) : probeTcp(addr, port, timeout);
                            if (h != null && !"closed".equals(h.state)) {
                                hits.add(h);
                                if ("open".equals(h.state)) open.incrementAndGet();
                            }
                        } finally {
                            latch.countDown();
                        }
                    }
                });
            }
            try { latch.await(Math.max(15, n / 4), TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
            pool.shutdownNow();
            List<Hit> copy = new ArrayList<Hit>(hits);
            Collections.sort(copy, new Comparator<Hit>() {
                @Override public int compare(Hit x, Hit y) { return x.port - y.port; }
            });
            summary = open.get() + " open  ·  " + copy.size() + " reported  ·  "
                    + addr.getHostAddress() + "  " + a + "-" + b
                    + (udp ? " UDP" : " TCP");
            SkidEventBus.logSystem("PCHECK", host, summary);
            return store(copy);
        } finally {
            running = false;
        }
    }

    private Hit probeTcp(InetAddress addr, int port, int timeout) {
        Socket s = null;
        try {
            s = new Socket();
            s.connect(new InetSocketAddress(addr, port), timeout);
            String banner = "";
            try {
                s.setSoTimeout(180);
                byte[] buf = new byte[96];
                int n = s.getInputStream().read(buf);
                banner = ToolNet.safeBanner(buf, n);
            } catch (Exception ignored) {}
            return new Hit(port, "TCP", "open", banner);
        } catch (Exception e) {
            return new Hit(port, "TCP", "closed", "");
        } finally {
            if (s != null) try { s.close(); } catch (Exception ignored) {}
        }
    }

    private Hit probeUdp(InetAddress addr, int port, int timeout) {
        DatagramSocket ds = null;
        try {
            ds = new DatagramSocket();
            ds.setSoTimeout(timeout);
            byte[] payload = new byte[]{0};
            ds.send(new DatagramPacket(payload, payload.length, addr, port));
            byte[] buf = new byte[256];
            DatagramPacket in = new DatagramPacket(buf, buf.length);
            ds.receive(in);
            return new Hit(port, "UDP", "open", ToolNet.safeBanner(buf, in.getLength()));
        } catch (Exception e) {
            String m = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
            if (m.contains("refused") || m.contains("unreachable")) {
                return new Hit(port, "UDP", "closed", "");
            }
            return new Hit(port, "UDP", "filtered", "");
        } finally {
            if (ds != null) try { ds.close(); } catch (Exception ignored) {}
        }
    }

    private synchronized List<Hit> store(List<Hit> hits) {
        last.clear();
        last.addAll(hits);
        return new ArrayList<Hit>(last);
    }
}
