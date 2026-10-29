package com.skidwaredns.app.util;

import com.skidwaredns.app.config.SkidConfig;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Binary TLS 1.2/1.3 ClientHello inspector, Mixed-Case SNI shaper (RFC 6066),
 * TLS Record Fragmenter (multi-record 0x16 framing), and TCP Segment Fragmenter.
 */
public final class TlsSniInspector {

    public static final class SniLocation {
        public final String hostname;
        public final int offsetInRecord;
        public final int length;

        public SniLocation(String hostname, int offsetInRecord, int length) {
            this.hostname = hostname;
            this.offsetInRecord = offsetInRecord;
            this.length = length;
        }
    }

    private TlsSniInspector() {}

    public static boolean isTlsClientHello(byte[] buf, int len) {
        return buf != null && len > 9
                && (buf[0] & 0xFF) == 0x16
                && (buf[1] & 0xFF) == 0x03
                && (buf[5] & 0xFF) == 0x01;
    }

    /**
     * Locates the ASCII server_name (extension 0x0000) inside a TLS ClientHello record.
     */
    public static SniLocation locateClientHelloSni(byte[] buf, int len) {
        if (!isTlsClientHello(buf, len)) return null;
        try {
            int pos = 5; // Handshake type (0x01)
            int hsLen = ((buf[pos + 1] & 0xFF) << 16) | ((buf[pos + 2] & 0xFF) << 8) | (buf[pos + 3] & 0xFF);
            int hsEnd = Math.min(len, pos + 4 + hsLen);
            pos += 4; // ClientVersion (2) + Random (32)
            pos += 34;
            if (pos >= hsEnd) return null;

            // Session ID
            int sidLen = buf[pos] & 0xFF;
            pos += 1 + sidLen;
            if (pos + 2 > hsEnd) return null;

            // Cipher Suites
            int csLen = ((buf[pos] & 0xFF) << 8) | (buf[pos + 1] & 0xFF);
            pos += 2 + csLen;
            if (pos + 1 > hsEnd) return null;

            // Compression Methods
            int compLen = buf[pos] & 0xFF;
            pos += 1 + compLen;
            if (pos + 2 > hsEnd) return null;

            // Extensions total length
            int extTotalLen = ((buf[pos] & 0xFF) << 8) | (buf[pos + 1] & 0xFF);
            pos += 2;
            int extEnd = Math.min(hsEnd, pos + extTotalLen);

            while (pos + 4 <= extEnd) {
                int extType = ((buf[pos] & 0xFF) << 8) | (buf[pos + 1] & 0xFF);
                int extLen = ((buf[pos + 2] & 0xFF) << 8) | (buf[pos + 3] & 0xFF);
                pos += 4;
                if (pos + extLen > extEnd) break;
                if (extType == 0x0000 && extLen >= 5) {
                    int listLen = ((buf[pos] & 0xFF) << 8) | (buf[pos + 1] & 0xFF);
                    int p = pos + 2;
                    int listEnd = Math.min(pos + extLen, p + listLen);
                    while (p + 3 <= listEnd) {
                        int nameType = buf[p] & 0xFF;
                        int nameLen = ((buf[p + 1] & 0xFF) << 8) | (buf[p + 2] & 0xFF);
                        p += 3;
                        if (p + nameLen > listEnd) break;
                        if (nameType == 0x00 && nameLen > 0) {
                            String host = new String(buf, p, nameLen, StandardCharsets.US_ASCII);
                            return new SniLocation(host, p, nameLen);
                        }
                        p += nameLen;
                    }
                }
                pos += extLen;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * Randomizes/alternates letter case of the SNI hostname in-place (e.g., WwW.gOoGle.CoM).
     * Per RFC 6066, DNS hostnames in server_name are case-insensitive; skipped on REALITY nodes.
     */
    public static boolean applyMixedCaseSniInPlace(byte[] buf, int len, boolean isRealityNode) {
        if (isRealityNode) return false;
        SniLocation loc = locateClientHelloSni(buf, len);
        if (loc == null || loc.length <= 0) return false;
        boolean changed = false;
        for (int i = 0; i < loc.length; i++) {
            int idx = loc.offsetInRecord + i;
            char c = (char) (buf[idx] & 0xFF);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                char out = (i % 2 == 0) ? Character.toUpperCase(c) : Character.toLowerCase(c);
                if (out != c) {
                    buf[idx] = (byte) out;
                    changed = true;
                }
            }
        }
        return changed;
    }

    /**
     * Splits a single TLS ClientHello record [0x16, 0x03, verMinor, lenHi, lenLo, payload...]
     * into two valid TLS records so no single TLS record carries the full handshake signature.
     */
    public static byte[] fragmentTlsRecord(byte[] buf, int len, int splitPayloadOffset) {
        if (!isTlsClientHello(buf, len)) {
            byte[] copy = new byte[len];
            System.arraycopy(buf, 0, copy, 0, len);
            return copy;
        }
        int recLen = ((buf[3] & 0xFF) << 8) | (buf[4] & 0xFF);
        if (5 + recLen > len || recLen <= 2) {
            byte[] copy = new byte[len];
            System.arraycopy(buf, 0, copy, 0, len);
            return copy;
        }

        // Prefer splitting right across the SNI midpoint if SNI is present, else at splitPayloadOffset
        int splitAt = Math.max(1, Math.min(recLen - 1, splitPayloadOffset));
        SniLocation sni = locateClientHelloSni(buf, len);
        if (sni != null && sni.offsetInRecord > 5 && (sni.offsetInRecord - 5 + sni.length / 2) < recLen) {
            splitAt = Math.max(1, (sni.offsetInRecord - 5) + Math.max(1, sni.length / 2));
        }

        int part1Len = splitAt;
        int part2Len = recLen - splitAt;
        int trailingLen = len - (5 + recLen);

        byte[] out = new byte[5 + part1Len + 5 + part2Len + trailingLen];
        int p = 0;
        // Record 1
        out[p++] = 0x16;
        out[p++] = buf[1];
        out[p++] = buf[2];
        out[p++] = (byte) ((part1Len >>> 8) & 0xFF);
        out[p++] = (byte) (part1Len & 0xFF);
        System.arraycopy(buf, 5, out, p, part1Len);
        p += part1Len;

        // Record 2
        out[p++] = 0x16;
        out[p++] = buf[1];
        out[p++] = buf[2];
        out[p++] = (byte) ((part2Len >>> 8) & 0xFF);
        out[p++] = (byte) (part2Len & 0xFF);
        System.arraycopy(buf, 5 + part1Len, out, p, part2Len);
        p += part2Len;

        if (trailingLen > 0) {
            System.arraycopy(buf, 5 + recLen, out, p, trailingLen);
        }
        return out;
    }

    /**
     * Unified outbound ClientHello shaper applying Mixed-Case SNI, TLS Record Fragmentation,
     * and TCP Segment Fragmentation according to SkidConfig.
     */
    public static void writeShapedPayload(OutputStream out, byte[] buf, int len, SkidConfig cfg) throws IOException {
        if (cfg == null || len <= 0) {
            out.write(buf, 0, len);
            out.flush();
            return;
        }

        byte[] working = new byte[len];
        System.arraycopy(buf, 0, working, 0, len);
        int workLen = len;
        boolean shaped = false;

        boolean isReality = (cfg.v2rayTransport != null && cfg.v2rayTransport.toLowerCase().contains("reality"))
                || ("V2Ray/Xray".equalsIgnoreCase(cfg.tunnel) && "Reality".equalsIgnoreCase(cfg.xraySecurity));
        if (cfg.mixedCaseSni && isTlsClientHello(working, workLen)) {
            if (applyMixedCaseSniInPlace(working, workLen, isReality)) {
                shaped = true;
            }
        }

        boolean dpiActive = !"None".equalsIgnoreCase(cfg.dpiStrategy);
        if ((cfg.tlsRecordFragment || (dpiActive && "TCP/TLS".equalsIgnoreCase(cfg.dpiTechnique)))
                && isTlsClientHello(working, workLen)) {
            working = fragmentTlsRecord(working, workLen, Math.max(1, cfg.tlsRecordSplitBytes));
            workLen = working.length;
            shaped = true;
        }

        if (cfg.tlsFragment || (dpiActive && ("TCP".equalsIgnoreCase(cfg.dpiTechnique)
                || "Desync".equalsIgnoreCase(cfg.dpiTechnique)
                || "Hybrid".equalsIgnoreCase(cfg.dpiTechnique)))) {
            int seg = Math.max(2, Math.min(workLen - 1, Math.max(2, cfg.tlsRecordSplitBytes + 3)));
            if (workLen > seg) {
                out.write(working, 0, seg);
                out.flush();
                int nextSeg = Math.min(seg * 2, workLen - seg);
                if (nextSeg > 0 && "Desync".equalsIgnoreCase(cfg.dpiTechnique) && workLen > seg + nextSeg) {
                    out.write(working, seg, nextSeg);
                    out.flush();
                    out.write(working, seg + nextSeg, workLen - seg - nextSeg);
                } else {
                    out.write(working, seg, workLen - seg);
                }
                out.flush();
                SkidEventBus.dpiDesyncApplied.incrementAndGet();
                return;
            }
        }

        out.write(working, 0, workLen);
        out.flush();
        if (shaped) {
            SkidEventBus.dpiDesyncApplied.incrementAndGet();
        }
    }

    /**
     * Builds a standards-compliant binary TLS 1.3 ClientHello record (0x16 0x03 0x01)
     * carrying server_name extension 0x0000 for unit testing & live UI previewing.
     */
    public static byte[] buildSyntheticClientHello(String hostname) {
        byte[] hostBytes = (hostname == null ? "www.google.com" : hostname).getBytes(StandardCharsets.US_ASCII);
        int sniNameEntryLen = 3 + hostBytes.length;          // [0x00][nameLenHi][nameLenLo][host]
        int sniExtPayloadLen = 2 + sniNameEntryLen;          // [listLenHi][listLenLo] + entry
        int extTotalLen = 4 + sniExtPayloadLen;              // [0x00,0x00][extLenHi,extLenLo] + payload
        int hsBodyLen = 2 + 32 + 1 + 2 + 2 + 2 + 2 + extTotalLen; // ver(2)+rnd(32)+sid(1)+csLen(2)+cs(2)+comp(2)+extLen(2)+ext
        int recPayloadLen = 4 + hsBodyLen;                   // [0x01][hsLen24(3)] + hsBody
        byte[] rec = new byte[5 + recPayloadLen];
        int p = 0;
        rec[p++] = 0x16;
        rec[p++] = 0x03;
        rec[p++] = 0x01;
        rec[p++] = (byte) ((recPayloadLen >>> 8) & 0xFF);
        rec[p++] = (byte) (recPayloadLen & 0xFF);
        // Handshake header
        rec[p++] = 0x01; // ClientHello
        rec[p++] = (byte) ((hsBodyLen >>> 16) & 0xFF);
        rec[p++] = (byte) ((hsBodyLen >>> 8) & 0xFF);
        rec[p++] = (byte) (hsBodyLen & 0xFF);
        // ClientVersion 0x0303 + 32B Random
        rec[p++] = 0x03;
        rec[p++] = 0x03;
        p += 32;
        // SessionID length = 0
        rec[p++] = 0x00;
        // CipherSuites length = 2 (TLS_AES_128_GCM_SHA256 = 0x1301)
        rec[p++] = 0x00;
        rec[p++] = 0x02;
        rec[p++] = 0x13;
        rec[p++] = 0x01;
        // Compression methods length = 1, null = 0x00
        rec[p++] = 0x01;
        rec[p++] = 0x00;
        // Extensions total length
        rec[p++] = (byte) ((extTotalLen >>> 8) & 0xFF);
        rec[p++] = (byte) (extTotalLen & 0xFF);
        // Extension 0x0000 (server_name)
        rec[p++] = 0x00;
        rec[p++] = 0x00;
        rec[p++] = (byte) ((sniExtPayloadLen >>> 8) & 0xFF);
        rec[p++] = (byte) (sniExtPayloadLen & 0xFF);
        rec[p++] = (byte) ((sniNameEntryLen >>> 8) & 0xFF);
        rec[p++] = (byte) (sniNameEntryLen & 0xFF);
        rec[p++] = 0x00; // host_name
        rec[p++] = (byte) ((hostBytes.length >>> 8) & 0xFF);
        rec[p++] = (byte) (hostBytes.length & 0xFF);
        System.arraycopy(hostBytes, 0, rec, p, hostBytes.length);
        return rec;
    }
}
