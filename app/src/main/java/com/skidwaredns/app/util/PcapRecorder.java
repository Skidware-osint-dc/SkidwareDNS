package com.skidwaredns.app.util;

import android.content.Context;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Real Wireshark-compatible Libpcap (.pcap) packet recorder for SkidwareDNS TUN traffic.
 * Uses LINKTYPE_RAW (101) for raw IPv4/IPv6 TUN frames.
 * Modes:
 *  - "Off": disabled
 *  - "Headers-Only": 96-byte snaplen (captures IP + TCP/UDP + DNS header without full payload)
 *  - "Full-PCAP": 65535-byte snaplen (captures complete raw packets)
 */
public final class PcapRecorder {

    private static final int PCAP_MAGIC = 0xa1b2c3d4;
    private static final int LINKTYPE_RAW = 101;
    private static final long MAX_PCAP_BYTES = 8L * 1024L * 1024L; // 8 MB safety cap

    private static OutputStream pcapOut;
    private static File activeFile;
    private static int snapLen = 65535;
    private static volatile boolean recording = false;
    public static final AtomicLong packetsCaptured = new AtomicLong(0);
    public static final AtomicLong bytesCaptured = new AtomicLong(0);

    private PcapRecorder() {}

    public static synchronized void startOrUpdate(Context ctx, String mode) {
        if (mode == null || "Off".equalsIgnoreCase(mode) || "Disabled".equalsIgnoreCase(mode)) {
            stop();
            return;
        }
        int desiredSnap = mode.toLowerCase().contains("header") ? 96 : 65535;
        if (recording && snapLen == desiredSnap && pcapOut != null) {
            return;
        }
        stop();
        try {
            File dir = new File(ctx.getFilesDir(), "captures");
            if (!dir.exists()) {
                //noinspection ResultOfMethodCallIgnored
                dir.mkdirs();
            }
            activeFile = new File(dir, "skidwaredns_capture.pcap");
            snapLen = desiredSnap;
            packetsCaptured.set(0);
            bytesCaptured.set(24);

            pcapOut = new BufferedOutputStream(new FileOutputStream(activeFile, false), 16384);
            writeGlobalHeader(pcapOut, snapLen);
            pcapOut.flush();
            recording = true;
            SkidEventBus.logSystem("SYSTEM", "Packet Capture",
                    "Started .pcap capture (" + mode + ", snaplen=" + snapLen + ")");
        } catch (Exception e) {
            recording = false;
            SkidEventBus.logSystem("SYSTEM", "Packet Capture Error", e.getMessage());
        }
    }

    public static synchronized void recordPacket(byte[] packet, int offset, int length) {
        if (!recording || pcapOut == null || packet == null || length <= 0) return;
        if (bytesCaptured.get() >= MAX_PCAP_BYTES) return;
        try {
            long nowMs = System.currentTimeMillis();
            int tsSec = (int) (nowMs / 1000L);
            int tsUsec = (int) ((nowMs % 1000L) * 1000L);
            int inclLen = Math.min(length, snapLen);

            writeIntLE(pcapOut, tsSec);
            writeIntLE(pcapOut, tsUsec);
            writeIntLE(pcapOut, inclLen);
            writeIntLE(pcapOut, length);
            pcapOut.write(packet, offset, inclLen);

            packetsCaptured.incrementAndGet();
            bytesCaptured.addAndGet(16 + inclLen);
        } catch (Exception ignored) {
        }
    }

    public static synchronized void flushQuietly() {
        if (pcapOut != null) {
            try {
                pcapOut.flush();
            } catch (Exception ignored) {
            }
        }
    }

    public static synchronized void stop() {
        recording = false;
        if (pcapOut != null) {
            try {
                pcapOut.flush();
                pcapOut.close();
            } catch (Exception ignored) {
            }
            pcapOut = null;
        }
    }

    public static synchronized File getCaptureFile(Context ctx) {
        flushQuietly();
        if (activeFile != null && activeFile.exists()) return activeFile;
        File f = new File(new File(ctx.getFilesDir(), "captures"), "skidwaredns_capture.pcap");
        return f.exists() ? f : null;
    }

    public static synchronized boolean exportToStream(Context ctx, OutputStream dest) {
        flushQuietly();
        File src = getCaptureFile(ctx);
        if (src == null || !src.exists()) return false;
        try (FileInputStream fis = new FileInputStream(src)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = fis.read(buf)) != -1) {
                dest.write(buf, 0, n);
            }
            dest.flush();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static void writeGlobalHeader(OutputStream out, int snap) throws Exception {
        writeIntLE(out, PCAP_MAGIC); // magic_number
        writeShortLE(out, 2);        // version_major
        writeShortLE(out, 4);        // version_minor
        writeIntLE(out, 0);          // thiszone
        writeIntLE(out, 0);          // sigfigs
        writeIntLE(out, snap);       // snaplen
        writeIntLE(out, LINKTYPE_RAW); // network = raw IPv4/IPv6
    }

    private static void writeIntLE(OutputStream out, int v) throws Exception {
        out.write(v & 0xFF);
        out.write((v >>> 8) & 0xFF);
        out.write((v >>> 16) & 0xFF);
        out.write((v >>> 24) & 0xFF);
    }

    private static void writeShortLE(OutputStream out, int v) throws Exception {
        out.write(v & 0xFF);
        out.write((v >>> 8) & 0xFF);
    }
}
