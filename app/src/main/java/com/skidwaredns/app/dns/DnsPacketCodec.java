package com.skidwaredns.app.dns;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Random;

/**
 * Pure Java IPv4/IPv6 + UDP + RFC 1035 DNS wire-format packet parser, checksum engine,
 * NXDOMAIN sinkhole synthesizer, and answer inspector for SkidwareDNS VpnService.
 */
public final class DnsPacketCodec {

    public static final class ParsedIpUdpPacket {
        public final int ipVersion; // 4 or 6
        public final int protocol;  // 17 = UDP, 6 = TCP
        public final byte[] srcIp;
        public final byte[] dstIp;
        public final String dstIpString;
        public final int srcPort;
        public final int dstPort;
        public final byte[] payload;

        public ParsedIpUdpPacket(int ipVersion, int protocol, byte[] srcIp, byte[] dstIp,
                                 String dstIpString, int srcPort, int dstPort, byte[] payload) {
            this.ipVersion = ipVersion;
            this.protocol = protocol;
            this.srcIp = srcIp;
            this.dstIp = dstIp;
            this.dstIpString = dstIpString;
            this.srcPort = srcPort;
            this.dstPort = dstPort;
            this.payload = payload;
        }
    }

    public static final class DnsQuestion {
        public final int txId;
        public final String qname;
        public final int qtype;
        public final String qtypeName;
        public final int questionEndOffset;

        public DnsQuestion(int txId, String qname, int qtype, String qtypeName, int questionEndOffset) {
            this.txId = txId;
            this.qname = qname;
            this.qtype = qtype;
            this.qtypeName = qtypeName;
            this.questionEndOffset = questionEndOffset;
        }
    }

    private static final Random RNG = new Random();

    private DnsPacketCodec() {}

    public static ParsedIpUdpPacket parseTunPacket(byte[] buf, int length) {
        if (buf == null || length < 20) return null;
        int version = (buf[0] >>> 4) & 0x0F;

        if (version == 4) {
            int ihl = (buf[0] & 0x0F) * 4;
            if (ihl < 20 || length < ihl + 8) return null;
            int proto = buf[9] & 0xFF;
            byte[] src = new byte[4];
            byte[] dst = new byte[4];
            System.arraycopy(buf, 12, src, 0, 4);
            System.arraycopy(buf, 16, dst, 0, 4);
            String dstStr = (dst[0] & 0xFF) + "." + (dst[1] & 0xFF) + "." + (dst[2] & 0xFF) + "." + (dst[3] & 0xFF);
            int srcPort = ((buf[ihl] & 0xFF) << 8) | (buf[ihl + 1] & 0xFF);
            int dstPort = ((buf[ihl + 2] & 0xFF) << 8) | (buf[ihl + 3] & 0xFF);
            if (proto != 17) {
                return new ParsedIpUdpPacket(4, proto, src, dst, dstStr, srcPort, dstPort, new byte[0]);
            }
            int udpLen = ((buf[ihl + 4] & 0xFF) << 8) | (buf[ihl + 5] & 0xFF);
            int payloadOffset = ihl + 8;
            int payloadLen = Math.min(Math.max(0, udpLen - 8), length - payloadOffset);
            byte[] payload = new byte[payloadLen];
            System.arraycopy(buf, payloadOffset, payload, 0, payloadLen);
            return new ParsedIpUdpPacket(4, 17, src, dst, dstStr, srcPort, dstPort, payload);
        } else if (version == 6) {
            if (length < 48) return null;
            int nextHdr = buf[6] & 0xFF;
            byte[] src = new byte[16];
            byte[] dst = new byte[16];
            System.arraycopy(buf, 8, src, 0, 16);
            System.arraycopy(buf, 24, dst, 0, 16);
            String dstStr = formatIpSafe(dst);
            int srcPort = ((buf[40] & 0xFF) << 8) | (buf[41] & 0xFF);
            int dstPort = ((buf[42] & 0xFF) << 8) | (buf[43] & 0xFF);
            if (nextHdr != 17) {
                return new ParsedIpUdpPacket(6, nextHdr, src, dst, dstStr, srcPort, dstPort, new byte[0]);
            }
            int udpLen = ((buf[44] & 0xFF) << 8) | (buf[45] & 0xFF);
            int payloadLen = Math.min(Math.max(0, udpLen - 8), length - 48);
            byte[] payload = new byte[payloadLen];
            System.arraycopy(buf, 48, payload, 0, payloadLen);
            return new ParsedIpUdpPacket(6, 17, src, dst, dstStr, srcPort, dstPort, payload);
        }
        return null;
    }

    public static byte[] buildTunUdpReply(ParsedIpUdpPacket req, byte[] dnsPayload) {
        if (req == null || dnsPayload == null) return null;
        if (req.ipVersion == 4) {
            int udpLen = 8 + dnsPayload.length;
            int totalLen = 20 + udpLen;
            byte[] pkt = new byte[totalLen];
            pkt[0] = 0x45;
            pkt[1] = 0x00;
            pkt[2] = (byte) ((totalLen >>> 8) & 0xFF);
            pkt[3] = (byte) (totalLen & 0xFF);
            int id = RNG.nextInt(0xFFFF);
            pkt[4] = (byte) ((id >>> 8) & 0xFF);
            pkt[5] = (byte) (id & 0xFF);
            pkt[6] = 0x40; // Don't Fragment
            pkt[7] = 0x00;
            pkt[8] = 64;   // TTL
            pkt[9] = 17;   // UDP
            // Swap src/dst IP
            System.arraycopy(req.dstIp, 0, pkt, 12, 4);
            System.arraycopy(req.srcIp, 0, pkt, 16, 4);
            int ipCsum = computeChecksum(pkt, 0, 20, 0);
            pkt[10] = (byte) ((ipCsum >>> 8) & 0xFF);
            pkt[11] = (byte) (ipCsum & 0xFF);

            // UDP Header (swap ports)
            pkt[20] = (byte) ((req.dstPort >>> 8) & 0xFF);
            pkt[21] = (byte) (req.dstPort & 0xFF);
            pkt[22] = (byte) ((req.srcPort >>> 8) & 0xFF);
            pkt[23] = (byte) (req.srcPort & 0xFF);
            pkt[24] = (byte) ((udpLen >>> 8) & 0xFF);
            pkt[25] = (byte) (udpLen & 0xFF);
            System.arraycopy(dnsPayload, 0, pkt, 28, dnsPayload.length);

            int udpCsum = computeUdpChecksumIpv4(req.dstIp, req.srcIp, pkt, 20, udpLen);
            pkt[26] = (byte) ((udpCsum >>> 8) & 0xFF);
            pkt[27] = (byte) (udpCsum & 0xFF);
            return pkt;
        } else if (req.ipVersion == 6) {
            int udpLen = 8 + dnsPayload.length;
            int totalLen = 40 + udpLen;
            byte[] pkt = new byte[totalLen];
            pkt[0] = 0x60; // Version 6
            pkt[4] = (byte) ((udpLen >>> 8) & 0xFF);
            pkt[5] = (byte) (udpLen & 0xFF);
            pkt[6] = 17;   // Next Header = UDP
            pkt[7] = 64;   // Hop Limit
            System.arraycopy(req.dstIp, 0, pkt, 8, 16);
            System.arraycopy(req.srcIp, 0, pkt, 24, 16);

            pkt[40] = (byte) ((req.dstPort >>> 8) & 0xFF);
            pkt[41] = (byte) (req.dstPort & 0xFF);
            pkt[42] = (byte) ((req.srcPort >>> 8) & 0xFF);
            pkt[43] = (byte) (req.srcPort & 0xFF);
            pkt[44] = (byte) ((udpLen >>> 8) & 0xFF);
            pkt[45] = (byte) (udpLen & 0xFF);
            System.arraycopy(dnsPayload, 0, pkt, 48, dnsPayload.length);

            int udpCsum = computeUdpChecksumIpv6(req.dstIp, req.srcIp, pkt, 40, udpLen);
            pkt[46] = (byte) ((udpCsum >>> 8) & 0xFF);
            pkt[47] = (byte) (udpCsum & 0xFF);
            return pkt;
        }
        return null;
    }

    public static DnsQuestion parseDnsQuestion(byte[] dns) {
        if (dns == null || dns.length < 12) return null;
        int txId = ((dns[0] & 0xFF) << 8) | (dns[1] & 0xFF);
        int qdCount = ((dns[4] & 0xFF) << 8) | (dns[5] & 0xFF);
        if (qdCount < 1) return null;

        int pos = 12;
        StringBuilder name = new StringBuilder();
        while (pos < dns.length) {
            int len = dns[pos++] & 0xFF;
            if (len == 0) break;
            if ((len & 0xC0) == 0xC0) {
                if (pos >= dns.length) return null;
                pos++;
                break;
            }
            if (pos + len > dns.length) return null;
            if (name.length() > 0) name.append('.');
            name.append(new String(dns, pos, len, StandardCharsets.US_ASCII));
            pos += len;
        }
        if (pos + 4 > dns.length) return null;
        int qtype = ((dns[pos] & 0xFF) << 8) | (dns[pos + 1] & 0xFF);
        int endOffset = pos + 4;
        return new DnsQuestion(txId, name.toString(), qtype, qtypeToString(qtype), endOffset);
    }

    /**
     * Synthesizes an instant NXDOMAIN sinkhole DNS reply when SkidFirewallEngine blocks a domain.
     */
    public static byte[] buildBlockedNxDomainReply(byte[] query, DnsQuestion q) {
        int copyLen = (q != null && q.questionEndOffset <= query.length) ? q.questionEndOffset : query.length;
        byte[] out = new byte[copyLen];
        System.arraycopy(query, 0, out, 0, copyLen);
        int rd = query[2] & 0x01;
        out[2] = (byte) (0x80 | rd);
        out[3] = (byte) 0x83;
        out[4] = 0x00; out[5] = 0x01;
        out[6] = 0x00; out[7] = 0x00;
        out[8] = 0x00; out[9] = 0x00;
        out[10] = 0x00; out[11] = 0x00;
        return out;
    }

    /**
     * Synthesizes an instant empty NOERROR (NODATA, ANCOUNT=0) DNS reply (e.g. for Block IPv6 AAAA).
     */
    public static byte[] buildEmptyNoErrorReply(byte[] query, DnsQuestion q) {
        int copyLen = (q != null && q.questionEndOffset <= query.length) ? q.questionEndOffset : query.length;
        byte[] out = new byte[copyLen];
        System.arraycopy(query, 0, out, 0, copyLen);
        int rd = query[2] & 0x01;
        out[2] = (byte) (0x80 | rd);
        out[3] = (byte) 0x80; // RA=1, RCODE=0 (NOERROR)
        out[4] = 0x00; out[5] = 0x01;
        out[6] = 0x00; out[7] = 0x00;
        out[8] = 0x00; out[9] = 0x00;
        out[10] = 0x00; out[11] = 0x00;
        return out;
    }

    /**
     * Synthesizes an instant RFC 1035 IPv4 A answer (used for Cloaking rules, .i2p hosts & Fake-IP 198.18.0.0/15).
     */
    public static byte[] buildSynthesizedIpv4Reply(byte[] origQuery, DnsQuestion q, byte[] ipv4, int ttlSec) {
        if (origQuery == null || ipv4 == null || ipv4.length != 4) return null;
        int qEnd = (q != null && q.questionEndOffset <= origQuery.length) ? q.questionEndOffset : origQuery.length;
        byte[] out = new byte[qEnd + 16];
        System.arraycopy(origQuery, 0, out, 0, qEnd);
        int rd = origQuery[2] & 0x01;
        out[2] = (byte) (0x80 | rd);
        out[3] = (byte) 0x80;
        out[4] = 0x00; out[5] = 0x01;
        out[6] = 0x00; out[7] = 0x01;
        out[8] = 0x00; out[9] = 0x00;
        out[10] = 0x00; out[11] = 0x00;
        int p = qEnd;
        out[p++] = (byte) 0xC0;
        out[p++] = 0x0C;
        out[p++] = 0x00; out[p++] = 0x01; // TYPE = A
        out[p++] = 0x00; out[p++] = 0x01; // CLASS = IN
        out[p++] = (byte) ((ttlSec >>> 24) & 0xFF);
        out[p++] = (byte) ((ttlSec >>> 16) & 0xFF);
        out[p++] = (byte) ((ttlSec >>> 8) & 0xFF);
        out[p++] = (byte) (ttlSec & 0xFF);
        out[p++] = 0x00; out[p++] = 0x04;
        System.arraycopy(ipv4, 0, out, p, 4);
        return out;
    }

    public static boolean isNxDomain(byte[] resp) {
        return resp != null && resp.length >= 12 && (resp[3] & 0x0F) == 3;
    }

    /**
     * Checks whether a DNS wire response carries at least one RR of expectedRtype.
     */
    public static boolean hasPositiveAnswer(byte[] resp, int expectedRtype) {
        if (resp == null || resp.length < 12) return false;
        if ((resp[3] & 0x0F) != 0) return false;
        int qdCount = ((resp[4] & 0xFF) << 8) | (resp[5] & 0xFF);
        int anCount = ((resp[6] & 0xFF) << 8) | (resp[7] & 0xFF);
        if (anCount == 0) return false;
        int pos = 12;
        for (int q = 0; q < qdCount; q++) {
            pos = skipDnsName(resp, pos);
            if (pos < 0 || pos + 4 > resp.length) return false;
            pos += 4;
        }
        for (int a = 0; a < anCount; a++) {
            pos = skipDnsName(resp, pos);
            if (pos < 0 || pos + 10 > resp.length) break;
            int rtype = ((resp[pos] & 0xFF) << 8) | (resp[pos + 1] & 0xFF);
            int rdlen = ((resp[pos + 8] & 0xFF) << 8) | (resp[pos + 9] & 0xFF);
            pos += 10;
            if (rtype == expectedRtype && pos + rdlen <= resp.length) return true;
            pos += rdlen;
        }
        return false;
    }

    /**
     * Extracts the first 4-byte IPv4 A record from a DNS wire response (for DNS64 synthesis).
     */
    public static byte[] extractFirstIpv4Bytes(byte[] resp) {
        if (resp == null || resp.length < 12) return null;
        if ((resp[3] & 0x0F) != 0) return null;
        int qdCount = ((resp[4] & 0xFF) << 8) | (resp[5] & 0xFF);
        int anCount = ((resp[6] & 0xFF) << 8) | (resp[7] & 0xFF);
        int pos = 12;
        for (int q = 0; q < qdCount; q++) {
            pos = skipDnsName(resp, pos);
            if (pos < 0 || pos + 4 > resp.length) return null;
            pos += 4;
        }
        for (int a = 0; a < anCount; a++) {
            pos = skipDnsName(resp, pos);
            if (pos < 0 || pos + 10 > resp.length) break;
            int rtype = ((resp[pos] & 0xFF) << 8) | (resp[pos + 1] & 0xFF);
            int rdlen = ((resp[pos + 8] & 0xFF) << 8) | (resp[pos + 9] & 0xFF);
            pos += 10;
            if (rtype == 1 && rdlen == 4 && pos + 4 <= resp.length) {
                byte[] v4 = new byte[4];
                System.arraycopy(resp, pos, v4, 0, 4);
                return v4;
            }
            pos += rdlen;
        }
        return null;
    }

    /**
     * RFC 6147 DNS64 / NAT64 synthesis: embeds an IPv4 address inside well-known prefix 64:ff9b::/96
     * to answer AAAA queries for IPv4-only hosts when Perform protocol translations is enabled.
     */
    public static byte[] buildDns64AaaaReply(byte[] origQuery, DnsQuestion q, byte[] ipv4) {
        if (origQuery == null || ipv4 == null || ipv4.length != 4) return null;
        int qEnd = (q != null && q.questionEndOffset <= origQuery.length) ? q.questionEndOffset : origQuery.length;
        // Header+Question + 2B pointer (0xC00C) + 2B TYPE(28) + 2B CLASS(1) + 4B TTL(120) + 2B RDLEN(16) + 16B IPv6
        byte[] out = new byte[qEnd + 28];
        System.arraycopy(origQuery, 0, out, 0, qEnd);
        int rd = origQuery[2] & 0x01;
        out[2] = (byte) (0x80 | rd);
        out[3] = (byte) 0x80; // RA=1, NOERROR
        out[4] = 0x00; out[5] = 0x01; // QDCOUNT=1
        out[6] = 0x00; out[7] = 0x01; // ANCOUNT=1
        out[8] = 0x00; out[9] = 0x00;
        out[10] = 0x00; out[11] = 0x00;

        int p = qEnd;
        out[p++] = (byte) 0xC0;
        out[p++] = 0x0C; // pointer to QNAME at offset 12
        out[p++] = 0x00; out[p++] = 28; // TYPE = AAAA
        out[p++] = 0x00; out[p++] = 0x01; // CLASS = IN
        out[p++] = 0x00; out[p++] = 0x00; out[p++] = 0x00; out[p++] = 120; // TTL = 120s
        out[p++] = 0x00; out[p++] = 16; // RDLENGTH = 16
        // 64:ff9b::/96 prefix (0064:ff9b:0000:0000:0000:0000) + 4-byte IPv4
        out[p++] = 0x00; out[p++] = 0x64;
        out[p++] = (byte) 0xFF; out[p++] = (byte) 0x9B;
        p += 8; // 8 zero bytes
        System.arraycopy(ipv4, 0, out, p, 4);
        return out;
    }

    /**
     * Builds a fresh RFC 1035 DNS wire query for diagnostic probing / benchmarking.
     */
    public static byte[] buildWireQuery(String domain, int qtype) {
        String clean = domain == null ? "example.com" : domain.trim();
        if (clean.endsWith(".")) clean = clean.substring(0, clean.length() - 1);
        String[] labels = clean.split("\\.");
        int qnameLen = 1;
        for (String l : labels) {
            qnameLen += 1 + l.length();
        }
        byte[] buf = new byte[12 + qnameLen + 4];
        int txId = RNG.nextInt(0xFFFF);
        buf[0] = (byte) ((txId >>> 8) & 0xFF);
        buf[1] = (byte) (txId & 0xFF);
        buf[2] = 0x01; // Recursion Desired
        buf[3] = 0x00;
        buf[4] = 0x00;
        buf[5] = 0x01; // QDCOUNT = 1
        int pos = 12;
        for (String l : labels) {
            byte[] lb = l.getBytes(StandardCharsets.US_ASCII);
            buf[pos++] = (byte) (lb.length & 0xFF);
            System.arraycopy(lb, 0, buf, pos, lb.length);
            pos += lb.length;
        }
        buf[pos++] = 0x00;
        buf[pos++] = (byte) ((qtype >>> 8) & 0xFF);
        buf[pos++] = (byte) (qtype & 0xFF);
        buf[pos++] = 0x00;
        buf[pos] = 0x01; // QCLASS = IN
        return buf;
    }

    /**
     * Extracts a concise human-readable answer summary (e.g. "93.184.215.14" or "NXDOMAIN")
     * from an RFC 1035 DNS wire response for live RethinkDNS-style telemetry logs.
     */
    public static String extractAnswerSummary(byte[] resp) {
        if (resp == null || resp.length < 12) return "SHORT_RESP";
        int rcode = resp[3] & 0x0F;
        if (rcode == 3) return "NXDOMAIN (Sinkholed/Blocked)";
        if (rcode == 2) return "SERVFAIL";
        if (rcode == 5) return "REFUSED (Filtered upstream)";
        int qdCount = ((resp[4] & 0xFF) << 8) | (resp[5] & 0xFF);
        int anCount = ((resp[6] & 0xFF) << 8) | (resp[7] & 0xFF);
        if (anCount == 0) return "NOERROR (0 answers /NODATA)";

        int pos = 12;
        for (int q = 0; q < qdCount; q++) {
            pos = skipDnsName(resp, pos);
            if (pos < 0 || pos + 4 > resp.length) return "NOERROR (" + anCount + " RR)";
            pos += 4;
        }
        for (int a = 0; a < anCount; a++) {
            pos = skipDnsName(resp, pos);
            if (pos < 0 || pos + 10 > resp.length) break;
            int rtype = ((resp[pos] & 0xFF) << 8) | (resp[pos + 1] & 0xFF);
            int rdlen = ((resp[pos + 8] & 0xFF) << 8) | (resp[pos + 9] & 0xFF);
            pos += 10;
            if (pos + rdlen > resp.length) break;
            if (rtype == 1 && rdlen == 4) {
                return (resp[pos] & 0xFF) + "." + (resp[pos + 1] & 0xFF) + "."
                        + (resp[pos + 2] & 0xFF) + "." + (resp[pos + 3] & 0xFF);
            } else if (rtype == 28 && rdlen == 16) {
                byte[] v6 = new byte[16];
                System.arraycopy(resp, pos, v6, 0, 16);
                return formatIpSafe(v6);
            }
            pos += rdlen;
        }
        return "NOERROR (" + anCount + " RR)";
    }

    private static int skipDnsName(byte[] buf, int pos) {
        int jumps = 0;
        while (pos < buf.length && jumps < 32) {
            int len = buf[pos] & 0xFF;
            if (len == 0) return pos + 1;
            if ((len & 0xC0) == 0xC0) return pos + 2;
            pos += 1 + len;
            jumps++;
        }
        return -1;
    }

    public static String qtypeToString(int qtype) {
        switch (qtype) {
            case 1: return "A";
            case 2: return "NS";
            case 5: return "CNAME";
            case 6: return "SOA";
            case 12: return "PTR";
            case 15: return "MX";
            case 16: return "TXT";
            case 28: return "AAAA";
            case 33: return "SRV";
            case 64: return "SVCB";
            case 65: return "HTTPS";
            default: return "T" + qtype;
        }
    }

    private static String formatIpSafe(byte[] raw) {
        try {
            return InetAddress.getByAddress(raw).getHostAddress();
        } catch (Exception e) {
            return "ip";
        }
    }

    private static int computeUdpChecksumIpv4(byte[] srcIp, byte[] dstIp, byte[] pkt, int udpOffset, int udpLen) {
        long sum = 0;
        for (int i = 0; i < 4; i += 2) {
            sum += ((srcIp[i] & 0xFF) << 8) | (srcIp[i + 1] & 0xFF);
            sum += ((dstIp[i] & 0xFF) << 8) | (dstIp[i + 1] & 0xFF);
        }
        sum += 17; // UDP protocol
        sum += udpLen;
        int csum = computeChecksum(pkt, udpOffset, udpLen, sum);
        return csum == 0 ? 0xFFFF : csum;
    }

    private static int computeUdpChecksumIpv6(byte[] srcIp, byte[] dstIp, byte[] pkt, int udpOffset, int udpLen) {
        long sum = 0;
        for (int i = 0; i < 16; i += 2) {
            sum += ((srcIp[i] & 0xFF) << 8) | (srcIp[i + 1] & 0xFF);
            sum += ((dstIp[i] & 0xFF) << 8) | (dstIp[i + 1] & 0xFF);
        }
        sum += udpLen;
        sum += 17; // Next header UDP
        int csum = computeChecksum(pkt, udpOffset, udpLen, sum);
        return csum == 0 ? 0xFFFF : csum;
    }

    private static int computeChecksum(byte[] buf, int offset, int length, long initialSum) {
        long sum = initialSum;
        int i = 0;
        while (i + 1 < length) {
            int word = ((buf[offset + i] & 0xFF) << 8) | (buf[offset + i + 1] & 0xFF);
            sum += word;
            i += 2;
        }
        if (i < length) {
            sum += (buf[offset + i] & 0xFF) << 8;
        }
        while ((sum >>> 16) != 0) {
            sum = (sum & 0xFFFF) + (sum >>> 16);
        }
        return (int) (~sum & 0xFFFF);
    }
}
