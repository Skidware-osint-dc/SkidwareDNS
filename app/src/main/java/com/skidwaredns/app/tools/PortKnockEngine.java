package com.skidwaredns.app.tools;

import com.skidwaredns.app.util.SkidEventBus;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Port-knock client (Knock on Ports style): TCP SYN / UDP datagram sequence
 * with inter-packet delay. ICMP skipped (needs raw sockets / root).
 */
public final class PortKnockEngine {

    private static final PortKnockEngine INSTANCE = new PortKnockEngine();

    public static final class Result {
        public final String step;
        public final boolean ok;
        public final String detail;

        public Result(String step, boolean ok, String detail) {
            this.step = step;
            this.ok = ok;
            this.detail = detail;
        }
    }

    private final List<Result> last = new ArrayList<Result>();
    private volatile boolean running;

    public static PortKnockEngine get() { return INSTANCE; }

    private PortKnockEngine() {}

    public synchronized List<Result> lastResults() {
        return new ArrayList<Result>(last);
    }

    public boolean isRunning() { return running; }

    public List<Result> knock(String host, String sequence, String proto, int delayMs, String payloadText) {
        List<Result> out = new ArrayList<Result>();
        running = true;
        try {
            if (host == null || host.trim().isEmpty()) {
                out.add(new Result("host", false, "Set a host first."));
                return store(out);
            }
            byte[] payload;
            if (payloadText == null || payloadText.isEmpty()) payload = new byte[]{'S', 'K'};
            else payload = payloadText.getBytes(Charset.forName("UTF-8"));
            List<ToolNet.KnockStep> steps = ToolNet.parseKnock(sequence, proto, payload);
            if (steps.isEmpty()) {
                out.add(new Result("seq", false, "No valid ports in sequence."));
                return store(out);
            }
            int wait = delayMs < 0 ? 0 : Math.min(delayMs, 5000);
            InetAddress addr;
            try {
                addr = InetAddress.getByName(host.trim());
            } catch (Exception e) {
                out.add(new Result("dns", false, "Cannot resolve " + host + ": " + e.getMessage()));
                return store(out);
            }
            for (int i = 0; i < steps.size(); i++) {
                ToolNet.KnockStep st = steps.get(i);
                Result r;
                if ("UDP".equals(st.proto)) r = sendUdp(addr, st);
                else r = sendTcp(addr, st);
                out.add(r);
                SkidEventBus.logSystem("KNOCK", host + ":" + st.port, r.detail);
                if (wait > 0 && i < steps.size() - 1) {
                    try { Thread.sleep(wait); } catch (InterruptedException ie) { break; }
                }
            }
            return store(out);
        } finally {
            running = false;
        }
    }

    private Result sendTcp(InetAddress addr, ToolNet.KnockStep st) {
        Socket s = null;
        try {
            s = new Socket();
            s.connect(new InetSocketAddress(addr, st.port), 400);
            return new Result(st.describe(), true, "TCP SYN/ACK " + addr.getHostAddress() + ":" + st.port);
        } catch (Exception e) {
            // A refused/reset knock still delivered the SYN, which is what knockd wants.
            String m = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            return new Result(st.describe(), true, "TCP knock sent (" + m + ")");
        } finally {
            if (s != null) try { s.close(); } catch (Exception ignored) {}
        }
    }

    private Result sendUdp(InetAddress addr, ToolNet.KnockStep st) {
        DatagramSocket ds = null;
        try {
            ds = new DatagramSocket();
            ds.send(new DatagramPacket(st.payload, st.payload.length, addr, st.port));
            return new Result(st.describe(), true, "UDP " + st.payload.length + "B -> :" + st.port);
        } catch (Exception e) {
            return new Result(st.describe(), false, "UDP fail: " + e.getMessage());
        } finally {
            if (ds != null) try { ds.close(); } catch (Exception ignored) {}
        }
    }

    private synchronized List<Result> store(List<Result> out) {
        last.clear();
        last.addAll(out);
        return out;
    }
}
