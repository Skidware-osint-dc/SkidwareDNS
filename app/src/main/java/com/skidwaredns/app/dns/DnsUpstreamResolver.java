package com.skidwaredns.app.dns;

import android.net.VpnService;

import com.skidwaredns.app.config.SkidConfig;
import com.skidwaredns.app.model.DnsPresets;
import com.skidwaredns.app.util.NetDiagnosticsAndMitm;
import com.skidwaredns.app.util.SkidEventBus;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * Upgraded Multi-Protocol DNS Resolver with:
 *  - DNS Booster (in-memory TTL response cache + resilient multi-upstream failover)
 *  - Global DNS Modes: Rethink DNS, Other DNS, Smart DNS, System DNS
 *  - Undelegated domain (.lan / .local / .internal) routing
 *  - Configurable Bootstrap DNS & TCP/UDP Socket Stack Tuning (SO_RCVBUF/SNDBUF, TCP_NODELAY, DSCP, UDP port randomization)
 *  - Provider / ASN enrichment (Show provider information)
 */
public final class DnsUpstreamResolver {

    public static final class UpstreamResult {
        public final byte[] wireResponse;
        public final String protocolUsed;
        public final long latencyMs;
        public final String error;

        public UpstreamResult(byte[] wireResponse, String protocolUsed, long latencyMs, String error) {
            this.wireResponse = wireResponse;
            this.protocolUsed = protocolUsed;
            this.latencyMs = latencyMs;
            this.error = error;
        }
    }

    private static final class CacheEntry {
        final byte[] wire;
        final String proto;
        final long expiresAtMs;

        CacheEntry(byte[] wire, String proto, long expiresAtMs) {
            this.wire = wire;
            this.proto = proto;
            this.expiresAtMs = expiresAtMs;
        }
    }

    private static final int DEFAULT_TIMEOUT_MS = 4500;
    private static final long CACHE_TTL_MS = 120_000L;
    private static final Random RNG = new Random();
    private static final Map<String, String> BOOTSTRAP_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, CacheEntry> BOOSTER_CACHE = new ConcurrentHashMap<>();

    static {
        BOOTSTRAP_CACHE.put("1dot1dot1dot1.cloudflare-dns.com", "1.1.1.1");
        BOOTSTRAP_CACHE.put("cloudflare-dns.com", "1.1.1.1");
        BOOTSTRAP_CACHE.put("family.cloudflare-dns.com", "1.1.1.3");
        BOOTSTRAP_CACHE.put("dns.adguard-dns.com", "94.140.14.14");
        BOOTSTRAP_CACHE.put("family.adguard-dns.com", "94.140.14.15");
        BOOTSTRAP_CACHE.put("dns.quad9.net", "9.9.9.9");
        BOOTSTRAP_CACHE.put("dns10.quad9.net", "9.9.9.10");
        BOOTSTRAP_CACHE.put("p2.freedns.controld.com", "76.76.2.2");
        BOOTSTRAP_CACHE.put("dns.google", "8.8.8.8");
        BOOTSTRAP_CACHE.put("doh.opendns.com", "208.67.222.222");
        BOOTSTRAP_CACHE.put("common.dot.dns.yandex.net", "77.88.8.8");
        BOOTSTRAP_CACHE.put("dns.aa.net.uk", "217.169.20.22");
        BOOTSTRAP_CACHE.put("dns.alidns.com", "223.5.5.5");
        BOOTSTRAP_CACHE.put("doh.pub", "119.29.29.29");
        BOOTSTRAP_CACHE.put("doh.360.cn", "101.226.4.6");
        BOOTSTRAP_CACHE.put("dns.twnic.tw", "101.101.101.101");
        BOOTSTRAP_CACHE.put("dns.mullvad.net", "194.242.2.2");
        BOOTSTRAP_CACHE.put("freedns.controld.com", "76.76.2.0");
        BOOTSTRAP_CACHE.put("dns.njal.la", "95.215.19.53");
        BOOTSTRAP_CACHE.put("free.shecan.ir", "178.22.122.100");
        BOOTSTRAP_CACHE.put("doh.libredns.gr", "116.202.176.26");
        BOOTSTRAP_CACHE.put("ee-tll-wg-101.relays.mullvad.net", "213.232.112.3");
        BOOTSTRAP_CACHE.put("ee-tll-wg-102.relays.mullvad.net", "213.232.112.166");
        BOOTSTRAP_CACHE.put("ee-tll-wg-103.relays.mullvad.net", "213.232.112.209");
        BOOTSTRAP_CACHE.put("ch-zrh-wg-001.relays.mullvad.net", "193.32.127.71");
        BOOTSTRAP_CACHE.put("ch-zrh-wg-002.relays.mullvad.net", "193.32.127.70");
        BOOTSTRAP_CACHE.put("ch-zrh-wg-003.relays.mullvad.net", "193.32.127.72");
        BOOTSTRAP_CACHE.put("ch-zrh-wg-201.relays.mullvad.net", "179.43.189.66");
        BOOTSTRAP_CACHE.put("ch-zrh-wg-202.relays.mullvad.net", "46.19.136.226");
        BOOTSTRAP_CACHE.put("ch-zrh-br-001.relays.mullvad.net", "193.32.127.117");
        BOOTSTRAP_CACHE.put("bg-sof-wg-001.relays.mullvad.net", "146.70.188.130");
        BOOTSTRAP_CACHE.put("bg-sof-wg-002.relays.mullvad.net", "135.136.38.2");
        BOOTSTRAP_CACHE.put("bg-sof-wg-003.relays.mullvad.net", "146.70.188.2");
        BOOTSTRAP_CACHE.put("cy-nic-wg-001.relays.mullvad.net", "195.47.194.131");
        BOOTSTRAP_CACHE.put("cy-nic-wg-002.relays.mullvad.net", "195.47.194.161");
        BOOTSTRAP_CACHE.put("dk-cph-wg-102.relays.mullvad.net", "149.88.109.73");
        BOOTSTRAP_CACHE.put("fi-hel-wg-201.relays.mullvad.net", "185.65.133.5");
        BOOTSTRAP_CACHE.put("fi-hel-wg-202.relays.mullvad.net", "185.65.133.85");
        BOOTSTRAP_CACHE.put("gb-lon-wg-101.relays.mullvad.net", "45.66.219.3");
        BOOTSTRAP_CACHE.put("gb-lon-wg-102.relays.mullvad.net", "45.66.219.166");
        BOOTSTRAP_CACHE.put("gb-lon-wg-103.relays.mullvad.net", "45.66.219.209");
        BOOTSTRAP_CACHE.put("il-tlv-wg-101.relays.mullvad.net", "169.150.227.197");
        BOOTSTRAP_CACHE.put("il-tlv-wg-102.relays.mullvad.net", "169.150.227.210");
        BOOTSTRAP_CACHE.put("il-tlv-wg-103.relays.mullvad.net", "169.150.227.222");
    }

    private DnsUpstreamResolver() {}

    public static void flushCache() {
        BOOSTER_CACHE.clear();
    }

    public static int getCacheSize() {
        return BOOSTER_CACHE.size();
    }

    public static UpstreamResult resolve(VpnService vpnService, SkidConfig cfg, byte[] dnsQuery) {
        long start = System.currentTimeMillis();
        DnsPacketCodec.DnsQuestion q = DnsPacketCodec.parseDnsQuestion(dnsQuery);
        String cacheKey = (q != null) ? (q.qname.toLowerCase(Locale.US) + "#" + q.qtype) : null;

        // 0A. DNSCrypt Filter: Block Unqualified (hostnames without a dot)
        if (cfg != null && cfg.dnscryptBlockUnqualified && q != null && !q.qname.contains(".")) {
            return new UpstreamResult(DnsPacketCodec.buildBlockedNxDomainReply(dnsQuery, q), "Filter:Unqualified", 0, null);
        }

        // 0B. DNSCrypt Filter: Block IPv6 (instant empty NOERROR NODATA on AAAA queries)
        if (cfg != null && cfg.dnscryptBlockIpv6 && q != null && q.qtype == 28) {
            return new UpstreamResult(DnsPacketCodec.buildEmptyNoErrorReply(dnsQuery, q), "Filter:Block-IPv6", 0, null);
        }

        // 0C. Cloaking Rules & Purple I2P (.i2p / .b32.i2p) Hosts
        if (cfg != null && q != null && q.qtype == 1) {
            byte[] cloakedIp = matchCloakingOrI2p(cfg, q.qname);
            if (cloakedIp != null) {
                byte[] rep = DnsPacketCodec.buildSynthesizedIpv4Reply(dnsQuery, q, cloakedIp, 60);
                return new UpstreamResult(rep, "Cloaking/I2P", 0, null);
            }
        }

        // 0D. Lumine / Clash Fake-IP Pool (198.18.0.0/15)
        if (cfg != null && (cfg.fakeIpEnabled || "Fake-IP".equalsIgnoreCase(cfg.dnsMode)) && q != null && q.qtype == 1) {
            byte[] fakeIp = NetDiagnosticsAndMitm.allocateFakeIpv4Bytes(q.qname);
            byte[] rep = DnsPacketCodec.buildSynthesizedIpv4Reply(dnsQuery, q, fakeIp, 30);
            return new UpstreamResult(rep, "Fake-IP(198.18.0.0/15)", 0, null);
        }

        // 0E. DNSCrypt Forwarding Rules (domain_suffix=dedicated_server)
        if (cfg != null && q != null && !cfg.forwardingRules.isEmpty()) {
            String fwdEp = matchForwardingRule(cfg, q.qname);
            if (fwdEp != null) {
                UpstreamResult fwdRes = resolveByEndpointString(vpnService, cfg, fwdEp, dnsQuery, start);
                if (fwdRes.wireResponse != null) {
                    return finalizeDnsResult(vpnService, cfg, dnsQuery, q, cacheKey,
                            new UpstreamResult(fwdRes.wireResponse, "ForwardRule->" + fwdRes.protocolUsed, fwdRes.latencyMs, null));
                }
            }
        }

        // 1. Check DNS Booster Cache
        if (cfg != null && cfg.dnsBooster && cacheKey != null) {
            CacheEntry ce = BOOSTER_CACHE.get(cacheKey);
            if (ce != null) {
                if (System.currentTimeMillis() < ce.expiresAtMs && ce.wire.length >= 12) {
                    byte[] copy = new byte[ce.wire.length];
                    System.arraycopy(ce.wire, 0, copy, 0, ce.wire.length);
                    copy[0] = dnsQuery[0];
                    copy[1] = dnsQuery[1];
                    SkidEventBus.dnsCacheHits.incrementAndGet();
                    return new UpstreamResult(copy, "BoosterCache(" + ce.proto + ")", 0, null);
                } else {
                    BOOSTER_CACHE.remove(cacheKey);
                }
            }
        }

        // 2. Undelegated local domain routing (.lan, .local, .internal, .home, .arpa)
        if (cfg != null && q != null && isUndelegatedDomain(q.qname)) {
            if (cfg.dnscryptBlockUndelegated && !cfg.systemDnsUndelegated) {
                return new UpstreamResult(DnsPacketCodec.buildBlockedNxDomainReply(dnsQuery, q), "Filter:Undelegated", 0, null);
            }
            if (cfg.systemDnsUndelegated) {
                String boot = (cfg.bootstrapDns != null && !cfg.bootstrapDns.isEmpty()) ? cfg.bootstrapDns : "1.1.1.1:53";
                return resolvePlainProxy(vpnService, cfg, boot, dnsQuery, start);
            }
        }

        // 3. Global DNS Category: System DNS or Smart DNS
        if (cfg != null && "System DNS".equalsIgnoreCase(cfg.globalDnsCategory) && !cfg.dnscryptIgnoreSystemDns) {
            String boot = (cfg.bootstrapDns != null && !cfg.bootstrapDns.isEmpty()) ? cfg.bootstrapDns : "8.8.8.8:53";
            return finalizeDnsResult(vpnService, cfg, dnsQuery, q, cacheKey, resolvePlainProxy(vpnService, cfg, boot, dnsQuery, start));
        }

        if (cfg != null && "Smart DNS".equalsIgnoreCase(cfg.globalDnsCategory)) {
            UpstreamResult dot = resolveDoT(vpnService, cfg, resolveDoTTarget(cfg), cfg.getDoTPort(), dnsQuery, start);
            if (dot.wireResponse != null) {
                return finalizeDnsResult(vpnService, cfg, dnsQuery, q, cacheKey,
                        new UpstreamResult(dot.wireResponse, "SmartDNS->" + dot.protocolUsed, dot.latencyMs, null));
            }
            UpstreamResult doh = resolveDoH(vpnService, cfg, resolveRethinkDohUrl(cfg), dnsQuery, start);
            if (doh.wireResponse != null) {
                return finalizeDnsResult(vpnService, cfg, dnsQuery, q, cacheKey,
                        new UpstreamResult(doh.wireResponse, "SmartDNS->" + doh.protocolUsed, doh.latencyMs, null));
            }
        }

        // 4. Split DNS for Tor/Proxy when enabled
        if (cfg != null && cfg.splitDns && cfg.torEnabled && !cfg.neverProxyDns) {
            UpstreamResult torRes = resolvePlainProxy(vpnService, cfg, "127.0.0.1:" + cfg.torDnsPort, dnsQuery, start);
            if (torRes.wireResponse != null) {
                return finalizeDnsResult(vpnService, cfg, dnsQuery, q, cacheKey, torRes);
            }
        }

        // 5. Custom DNS override if explicitly specified
        if (cfg != null && !cfg.customDnsServer.isEmpty()) {
            String custom = cfg.customDnsServer.get(0).trim();
            if (!custom.isEmpty()) {
                UpstreamResult r = resolveByEndpointString(vpnService, cfg, custom, dnsQuery, start);
                if (r.wireResponse != null) return finalizeDnsResult(vpnService, cfg, dnsQuery, q, cacheKey, r);
            }
        }

        String mode = (cfg != null && cfg.dnsMode != null) ? cfg.dnsMode.trim() : "Rethink-DoH";
        try {
            UpstreamResult primary;
            if ("Injector-DoH".equalsIgnoreCase(mode) && cfg != null) {
                String dohUrl = DnsPresets.getInjectorDohUrl(cfg.injectorDohProvider);
                if (!dohUrl.isEmpty()) {
                    primary = resolveDoH(vpnService, cfg, dohUrl, dnsQuery, start);
                } else {
                    primary = resolveDoH(vpnService, cfg, resolveRethinkDohUrl(cfg), dnsQuery, start);
                }
            } else if ("LBox-Template".equalsIgnoreCase(mode) && cfg != null && !cfg.lboxTemplates.isEmpty()) {
                primary = resolveLboxTemplates(vpnService, cfg, dnsQuery, start);
            } else if ("DNS-over-TLS".equalsIgnoreCase(mode)) {
                primary = resolveDoT(vpnService, cfg, resolveDoTTarget(cfg), cfg != null ? cfg.getDoTPort() : 853, dnsQuery, start);
            } else if ("DNSCrypt".equalsIgnoreCase(mode)) {
                primary = resolveDnsCryptStampEndpoint(vpnService, cfg, resolveDnsCryptStamp(cfg), dnsQuery, start);
            } else if ("DNS-Proxy".equalsIgnoreCase(mode)) {
                primary = resolvePlainProxy(vpnService, cfg, resolveDnsProxyTarget(cfg), dnsQuery, start);
            } else {
                primary = resolveDoH(vpnService, cfg, resolveRethinkDohUrl(cfg), dnsQuery, start);
            }
            if (primary.wireResponse != null) {
                return finalizeDnsResult(vpnService, cfg, dnsQuery, q, cacheKey, primary);
            }

            // DNS Booster resilient error handling fallback
            if (cfg == null || cfg.dnsBooster) {
                String fallbackEp = (cfg != null && cfg.bootstrapDns != null && !cfg.bootstrapDns.isEmpty())
                        ? cfg.bootstrapDns : "9.9.9.9:53";
                UpstreamResult fallback = resolvePlainProxy(vpnService, cfg, fallbackEp, dnsQuery, start);
                if (fallback.wireResponse != null) {
                    return finalizeDnsResult(vpnService, cfg, dnsQuery, q, cacheKey, new UpstreamResult(
                            fallback.wireResponse, fallback.protocolUsed + " [Failover]", fallback.latencyMs, null));
                }
            }
            return primary;
        } catch (Exception e) {
            return new UpstreamResult(null, mode, System.currentTimeMillis() - start, e.getMessage());
        }
    }

    private static UpstreamResult finalizeDnsResult(VpnService vpn, SkidConfig cfg, byte[] origQuery,
                                                    DnsPacketCodec.DnsQuestion q, String key, UpstreamResult res) {
        if (cfg != null && cfg.performProtocolTranslations && q != null && q.qtype == 28
                && res != null && res.wireResponse != null && !DnsPacketCodec.hasPositiveAnswer(res.wireResponse, 28)) {
            byte[] aQuery = DnsPacketCodec.buildWireQuery(q.qname, 1);
            String boot = (cfg.bootstrapDns != null && !cfg.bootstrapDns.isEmpty()) ? cfg.bootstrapDns : "1.1.1.1:53";
            UpstreamResult aRes = resolvePlainProxy(vpn, cfg, boot, aQuery, System.currentTimeMillis());
            byte[] v4 = aRes.wireResponse != null ? DnsPacketCodec.extractFirstIpv4Bytes(aRes.wireResponse) : null;
            if (v4 != null) {
                byte[] dns64 = DnsPacketCodec.buildDns64AaaaReply(origQuery, q, v4);
                if (dns64 != null) {
                    res = new UpstreamResult(dns64, res.protocolUsed + " [DNS64 64:ff9b::/96]", res.latencyMs, null);
                }
            }
        }
        return storeCacheIfOk(cfg, key, res);
    }

    private static UpstreamResult storeCacheIfOk(SkidConfig cfg, String key, UpstreamResult res) {
        if (cfg != null && cfg.dnsBooster && key != null && res != null && res.wireResponse != null && res.wireResponse.length >= 12) {
            if (BOOSTER_CACHE.size() > 600) {
                BOOSTER_CACHE.clear();
            }
            byte[] saved = new byte[res.wireResponse.length];
            System.arraycopy(res.wireResponse, 0, saved, 0, res.wireResponse.length);
            BOOSTER_CACHE.put(key, new CacheEntry(saved, res.protocolUsed, System.currentTimeMillis() + CACHE_TTL_MS));
        }
        return res;
    }

    public static UpstreamResult resolveByEndpointString(VpnService vpn, SkidConfig cfg, String ep, byte[] query, long start) {
        if (ep.startsWith("https://")) {
            return resolveDoH(vpn, cfg, ep, query, start);
        } else if (ep.startsWith("tls://")) {
            return resolveDoT(vpn, cfg, ep, cfg != null ? cfg.getDoTPort() : 853, query, start);
        } else if (ep.startsWith("sdns://")) {
            return resolveDnsCryptStampEndpoint(vpn, cfg, ep, query, start);
        } else {
            return resolvePlainProxy(vpn, cfg, ep, query, start);
        }
    }

    public static UpstreamResult resolveDoH(VpnService vpn, String dohUrl, byte[] dnsQuery, long startMs) {
        return resolveDoH(vpn, null, dohUrl, dnsQuery, startMs);
    }

    public static UpstreamResult resolveDoH(VpnService vpn, SkidConfig cfg, String dohUrl, byte[] dnsQuery, long startMs) {
        if (vpn == null) {
            javax.net.ssl.HttpsURLConnection conn = null;
            try {
                java.net.URL u = new java.net.URL(dohUrl.trim());
                conn = (javax.net.ssl.HttpsURLConnection) u.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(getSocketTimeoutMs(cfg));
                conn.setReadTimeout(getSocketTimeoutMs(cfg));
                conn.setRequestProperty("Content-Type", "application/dns-message");
                conn.setRequestProperty("Accept", "application/dns-message");
                conn.setRequestProperty("User-Agent", "SkidwareDNS/4.1");
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(dnsQuery);
                    os.flush();
                }
                int code = conn.getResponseCode();
                if (code == 200) {
                    byte[] body = readAllBytesBounded(conn.getInputStream(), 16384);
                    long ms = System.currentTimeMillis() - startMs;
                    return new UpstreamResult(body, "DoH:" + u.getHost(), ms, null);
                } else if (code == 505) {
                    return resolveDoT(null, cfg, "tls://" + u.getHost(), 853, dnsQuery, startMs);
                }
            } catch (Exception ignored) {
            } finally {
                if (conn != null) conn.disconnect();
            }
        }
        Socket raw = null;
        SSLSocket ssl = null;
        String parsedHost = null;
        try {
            URI uri = new URI(dohUrl.trim());
            String host = uri.getHost();
            parsedHost = host;
            int port = uri.getPort() > 0 ? uri.getPort() : 443;
            String path = (uri.getRawPath() == null || uri.getRawPath().isEmpty()) ? "/dns-query" : uri.getRawPath();
            if (uri.getRawQuery() != null && !uri.getRawQuery().isEmpty()) {
                path = path + "?" + uri.getRawQuery();
            }

            String targetIp = resolveBootstrapIp(vpn, cfg, host);
            raw = new Socket();
            if (vpn != null && !cfgLoopback(cfg)) {
                vpn.protect(raw);
            }
            applyTcpSocketTuning(raw, cfg);
            raw.connect(new InetSocketAddress(targetIp, port), getSocketTimeoutMs(cfg));

            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            ssl = (SSLSocket) factory.createSocket(raw, host, port, true);
            SSLParameters params = ssl.getSSLParameters();
            params.setServerNames(Collections.singletonList(new SNIHostName(host)));
            params.setEndpointIdentificationAlgorithm("HTTPS");
            ssl.setSSLParameters(params);
            ssl.startHandshake();

            OutputStream out = ssl.getOutputStream();
            String reqHeaders = "POST " + path + " HTTP/1.1\r\n" +
                    "Host: " + host + "\r\n" +
                    "Content-Type: application/dns-message\r\n" +
                    "Accept: application/dns-message\r\n" +
                    "Content-Length: " + dnsQuery.length + "\r\n" +
                    "User-Agent: SkidwareDNS/2.0\r\n" +
                    "Connection: close\r\n\r\n";
            out.write(reqHeaders.getBytes(StandardCharsets.US_ASCII));
            out.write(dnsQuery);
            out.flush();

            InputStream in = ssl.getInputStream();
            String statusLine = readAsciiLine(in);
            if (statusLine != null && statusLine.contains("505") && parsedHost != null) {
                closeQuietly(ssl);
                closeQuietly(raw);
                ssl = null;
                raw = null;
                return resolveDoT(vpn, cfg, "tls://" + parsedHost, 853, dnsQuery, startMs);
            }
            if (statusLine == null || !statusLine.contains("200")) {
                throw new IllegalStateException("DoH HTTP status: " + statusLine);
            }
            int contentLength = -1;
            boolean chunked = false;
            while (true) {
                String hdr = readAsciiLine(in);
                if (hdr == null || hdr.isEmpty()) break;
                String lower = hdr.toLowerCase(Locale.US);
                if (lower.startsWith("content-length:")) {
                    contentLength = Integer.parseInt(hdr.substring(15).trim());
                } else if (lower.startsWith("transfer-encoding:") && lower.contains("chunked")) {
                    chunked = true;
                }
            }

            byte[] body;
            if (chunked) {
                body = readChunkedBody(in);
            } else if (contentLength > 0 && contentLength < 65536) {
                body = new byte[contentLength];
                new DataInputStream(in).readFully(body);
            } else {
                body = readAllBytesBounded(in, 8192);
            }

            long ms = System.currentTimeMillis() - startMs;
            return new UpstreamResult(body, "DoH:" + host, ms, null);
        } catch (Exception e) {
            return new UpstreamResult(null, "DoH", System.currentTimeMillis() - startMs, e.getMessage());
        } finally {
            closeQuietly(ssl);
            closeQuietly(raw);
        }
    }

    public static UpstreamResult resolveDoT(VpnService vpn, String tlsEndpoint, int defaultPort, byte[] dnsQuery, long startMs) {
        return resolveDoT(vpn, null, tlsEndpoint, defaultPort, dnsQuery, startMs);
    }

    public static UpstreamResult resolveDoT(VpnService vpn, SkidConfig cfg, String tlsEndpoint, int defaultPort, byte[] dnsQuery, long startMs) {
        Socket raw = null;
        SSLSocket ssl = null;
        try {
            DnsPresets.HostPort hp = DnsPresets.parseHostPort(tlsEndpoint, defaultPort);
            String sniHost = hp.host;
            String targetIp = resolveBootstrapIp(vpn, cfg, sniHost);

            raw = new Socket();
            if (vpn != null && !cfgLoopback(cfg)) {
                vpn.protect(raw);
            }
            applyTcpSocketTuning(raw, cfg);
            raw.connect(new InetSocketAddress(targetIp, hp.port), getSocketTimeoutMs(cfg));

            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            ssl = (SSLSocket) factory.createSocket(raw, sniHost, hp.port, true);
            if (!isLiteralIp(sniHost)) {
                SSLParameters params = ssl.getSSLParameters();
                params.setServerNames(Collections.singletonList(new SNIHostName(sniHost)));
                params.setEndpointIdentificationAlgorithm("HTTPS");
                ssl.setSSLParameters(params);
            }
            ssl.startHandshake();

            DataOutputStream dos = new DataOutputStream(ssl.getOutputStream());
            dos.writeShort(dnsQuery.length);
            dos.write(dnsQuery);
            dos.flush();

            DataInputStream dis = new DataInputStream(ssl.getInputStream());
            int respLen = dis.readUnsignedShort();
            if (respLen <= 0 || respLen > 65535) {
                throw new IllegalStateException("Invalid DoT frame length: " + respLen);
            }
            byte[] resp = new byte[respLen];
            dis.readFully(resp);

            long ms = System.currentTimeMillis() - startMs;
            return new UpstreamResult(resp, "DoT:" + sniHost, ms, null);
        } catch (Exception e) {
            return new UpstreamResult(null, "DoT", System.currentTimeMillis() - startMs, e.getMessage());
        } finally {
            closeQuietly(ssl);
            closeQuietly(raw);
        }
    }

    private static byte[] matchCloakingOrI2p(SkidConfig cfg, String qname) {
        if (qname == null) return null;
        String lower = qname.toLowerCase(Locale.US);
        if (lower.endsWith(".")) lower = lower.substring(0, lower.length() - 1);

        if (cfg.i2pEnabled || lower.endsWith(".i2p")) {
            String i2pIp = DnscryptListManager.lookupI2pHost(lower);
            if (i2pIp != null) {
                byte[] parsed = parseSimpleV4(i2pIp);
                if (parsed != null) return parsed;
            }
        }
        for (String rule : cfg.cloakingRules) {
            if (rule == null) continue;
            String r = rule.trim();
            int sep = r.indexOf('=');
            if (sep < 0) sep = r.indexOf(' ');
            if (sep > 0) {
                String pat = r.substring(0, sep).trim().toLowerCase(Locale.US);
                String targetIp = r.substring(sep + 1).trim();
                if (lower.equals(pat) || lower.endsWith("." + pat)) {
                    byte[] v4 = parseSimpleV4(targetIp);
                    if (v4 != null) return v4;
                }
            }
        }
        return null;
    }

    private static String matchForwardingRule(SkidConfig cfg, String qname) {
        if (qname == null) return null;
        String lower = qname.toLowerCase(Locale.US);
        for (String rule : cfg.forwardingRules) {
            if (rule == null) continue;
            String r = rule.trim();
            int sep = r.indexOf('=');
            if (sep < 0) sep = r.indexOf(' ');
            if (sep > 0) {
                String suffix = r.substring(0, sep).trim().toLowerCase(Locale.US);
                String ep = r.substring(sep + 1).trim();
                if (lower.equals(suffix) || lower.endsWith("." + suffix)) {
                    return ep;
                }
            }
        }
        return null;
    }

    private static UpstreamResult resolveLboxTemplates(VpnService vpn, SkidConfig cfg, byte[] query, long start) {
        UpstreamResult last = new UpstreamResult(null, "LBox-Template", 0, "No LBox template enabled");
        for (String tplId : cfg.lboxTemplates) {
            DnsPresets.DnsEndpoint ep = DnsPresets.findByNameOrValue(DnsPresets.LBOX_TEMPLATES, tplId);
            String target = ep != null ? ep.value : tplId;
            UpstreamResult r = resolveByEndpointString(vpn, cfg, target, query, start);
            if (r.wireResponse != null) {
                String label = ep != null ? ("LBox[" + ep.id + "]->" + r.protocolUsed) : r.protocolUsed;
                return new UpstreamResult(r.wireResponse, label, r.latencyMs, null);
            }
            last = r;
        }
        return last;
    }

    private static byte[] parseSimpleV4(String ip) {
        try {
            String[] p = ip.trim().split("\\.");
            if (p.length != 4) return null;
            return new byte[]{
                    (byte) Integer.parseInt(p[0]),
                    (byte) Integer.parseInt(p[1]),
                    (byte) Integer.parseInt(p[2]),
                    (byte) Integer.parseInt(p[3])
            };
        } catch (Exception e) {
            return null;
        }
    }

    public static UpstreamResult resolveDnsCryptStampEndpoint(VpnService vpn, String stampRaw, byte[] dnsQuery, long startMs) {
        return resolveDnsCryptStampEndpoint(vpn, null, stampRaw, dnsQuery, startMs);
    }

    public static UpstreamResult resolveDnsCryptStampEndpoint(VpnService vpn, SkidConfig cfg, String stampRaw, byte[] dnsQuery, long startMs) {
        DnsPresets.DecodedStamp decoded = DnsPresets.decodeSdnsStamp(stampRaw);
        if (!decoded.valid || decoded.host.isEmpty()) {
            return new UpstreamResult(null, "DNSCrypt", System.currentTimeMillis() - startMs, decoded.error);
        }
        String relayTag = (cfg != null && !cfg.dnscryptRelays.isEmpty()) ? "+AnonRelay" : "";

        // DoH sdns:// stamp (proto 0x02 from public-resolvers.md)
        if (decoded.protoId == 0x02 && !decoded.providerName.isEmpty()) {
            BOOTSTRAP_CACHE.put(decoded.providerName, decoded.host);
            String dohPath = decoded.path.isEmpty() ? "/dns-query" : decoded.path;
            UpstreamResult doh = resolveDoH(vpn, cfg, "https://" + decoded.providerName + dohPath, dnsQuery, startMs);
            if (doh.wireResponse != null) {
                return new UpstreamResult(doh.wireResponse, "DoH-Stamp[" + decoded.providerName + "]", doh.latencyMs, null);
            }
        }

        String sni = mapKnownStampIpToSni(decoded.host, decoded.providerName);
        if (sni != null) {
            BOOTSTRAP_CACHE.put(sni, decoded.host);
            UpstreamResult dot = resolveDoT(vpn, cfg, "tls://" + sni, 853, dnsQuery, startMs);
            if (dot.wireResponse != null) {
                return new UpstreamResult(dot.wireResponse, "DNSCrypt[" + decoded.host + "]" + relayTag, dot.latencyMs, null);
            }
        }

        if (cfg != null && cfg.dnscryptForceTcp) {
            try {
                InetAddress addr = InetAddress.getByName(decoded.host);
                UpstreamResult tcp = resolveTcpDns(vpn, cfg, addr, 53, dnsQuery, startMs);
                if (tcp.wireResponse != null) {
                    return new UpstreamResult(tcp.wireResponse, "DNSCrypt-ForceTCP[" + decoded.host + "]" + relayTag, tcp.latencyMs, null);
                }
            } catch (Exception ignored) {
            }
        }

        UpstreamResult udp = resolvePlainProxy(vpn, cfg, decoded.host + ":53", dnsQuery, startMs);
        if (udp.wireResponse != null) {
            return new UpstreamResult(udp.wireResponse, "DNSCrypt-Stamp[" + decoded.host + "]" + relayTag, udp.latencyMs, null);
        }
        return udp;
    }

    public static UpstreamResult resolvePlainProxy(VpnService vpn, String hostPortRaw, byte[] dnsQuery, long startMs) {
        return resolvePlainProxy(vpn, null, hostPortRaw, dnsQuery, startMs);
    }

    public static UpstreamResult resolvePlainProxy(VpnService vpn, SkidConfig cfg, String hostPortRaw, byte[] dnsQuery, long startMs) {
        DatagramSocket udp = null;
        try {
            DnsPresets.HostPort hp = DnsPresets.parseHostPort(hostPortRaw, 53);
            InetAddress addr = InetAddress.getByName(hp.host);

            if (cfg != null && cfg.udpRandomizeSourcePort) {
                int ephemPort = com.skidwaredns.app.proxy.SocketStack.ephemeralPort(cfg);
                try {
                    udp = new DatagramSocket(ephemPort);
                } catch (Exception e) {
                    udp = new DatagramSocket();
                }
            } else {
                udp = new DatagramSocket();
            }

            if (vpn != null && !addr.isLoopbackAddress() && !cfgLoopback(cfg)) {
                vpn.protect(udp);
            }
            applyUdpSocketTuning(udp, cfg);

            DatagramPacket outPkt = new DatagramPacket(dnsQuery, dnsQuery.length, addr, hp.port);
            udp.send(outPkt);

            int maxUdp = (cfg != null) ? Math.max(cfg.udpMaxDatagramBytes, cfg.udpEdnsPayloadBytes) : 4096;
            byte[] buf = new byte[Math.max(4096, maxUdp)];
            DatagramPacket inPkt = new DatagramPacket(buf, buf.length);
            udp.receive(inPkt);

            byte[] resp = new byte[inPkt.getLength()];
            System.arraycopy(buf, 0, resp, 0, resp.length);

            if (resp.length >= 4 && (resp[2] & 0x02) != 0) {
                UpstreamResult tcpRes = resolveTcpDns(vpn, cfg, addr, hp.port, dnsQuery, startMs);
                if (tcpRes.wireResponse != null) return tcpRes;
            }

            long ms = System.currentTimeMillis() - startMs;
            return new UpstreamResult(resp, "UDP-DNS:" + hp.host + ":" + hp.port, ms, null);
        } catch (Exception e) {
            return new UpstreamResult(null, "UDP-DNS:" + hostPortRaw, System.currentTimeMillis() - startMs, e.getMessage());
        } finally {
            if (udp != null) udp.close();
        }
    }

    private static UpstreamResult resolveTcpDns(VpnService vpn, SkidConfig cfg, InetAddress addr, int port, byte[] query, long startMs) {
        Socket sock = null;
        try {
            sock = new Socket();
            if (vpn != null && !addr.isLoopbackAddress() && !cfgLoopback(cfg)) {
                vpn.protect(sock);
            }
            applyTcpSocketTuning(sock, cfg);
            sock.connect(new InetSocketAddress(addr, port), getSocketTimeoutMs(cfg));
            DataOutputStream dos = new DataOutputStream(sock.getOutputStream());
            dos.writeShort(query.length);
            dos.write(query);
            dos.flush();

            DataInputStream dis = new DataInputStream(sock.getInputStream());
            int len = dis.readUnsignedShort();
            byte[] resp = new byte[len];
            dis.readFully(resp);
            return new UpstreamResult(resp, "TCP-DNS:" + addr.getHostAddress(), System.currentTimeMillis() - startMs, null);
        } catch (Exception e) {
            return new UpstreamResult(null, "TCP-DNS", System.currentTimeMillis() - startMs, e.getMessage());
        } finally {
            closeQuietly(sock);
        }
    }

    public static void applyTcpSocketTuning(Socket s, SkidConfig cfg) {
        com.skidwaredns.app.proxy.SocketStack.applyTcp(s, cfg);
    }

    public static void applyUdpSocketTuning(DatagramSocket udp, SkidConfig cfg) {
        com.skidwaredns.app.proxy.SocketStack.applyUdp(udp, cfg);
    }

    /**
     * Enriches resolved IPv4/IPv6 strings with ASN / Service Provider labels when Show-Provider-Info=true.
     */
    public static String describeProviderForAnswer(String answerIp) {
        if (answerIp == null || answerIp.isEmpty()) return "";
        if (answerIp.startsWith("104.16.") || answerIp.startsWith("104.17.") || answerIp.startsWith("104.18.")
                || answerIp.startsWith("172.64.") || answerIp.startsWith("172.67.") || answerIp.startsWith("1.1.1.")) {
            return " [Cloudflare AS13335]";
        }
        if (answerIp.startsWith("142.250.") || answerIp.startsWith("172.217.") || answerIp.startsWith("8.8.")) {
            return " [Google AS15169]";
        }
        if (answerIp.startsWith("151.101.") || answerIp.startsWith("199.232.")) {
            return " [Fastly CDN AS54113]";
        }
        if (answerIp.startsWith("13.") || answerIp.startsWith("52.") || answerIp.startsWith("54.")) {
            return " [Amazon AWS AS16509]";
        }
        if (answerIp.startsWith("9.9.9.")) {
            return " [Quad9 AS19281]";
        }
        if (answerIp.startsWith("94.140.14.")) {
            return " [AdGuard AS212757]";
        }
        if (answerIp.startsWith("185.228.168.")) {
            return " [CleanBrowsing AS47583]";
        }
        if (answerIp.startsWith("192.168.") || answerIp.startsWith("10.")) {
            return " [Private LAN]";
        }
        return "";
    }

    private static int getSocketTimeoutMs(SkidConfig cfg) {
        return com.skidwaredns.app.proxy.SocketStack.connectTimeoutMs(cfg);
    }

    private static boolean cfgLoopback(SkidConfig cfg) {
        return cfg != null && cfg.loopbackExperimental;
    }

    private static boolean isUndelegatedDomain(String qname) {
        if (qname == null) return false;
        String d = qname.toLowerCase(Locale.US);
        return !d.contains(".") || d.endsWith(".lan") || d.endsWith(".local")
                || d.endsWith(".internal") || d.endsWith(".home") || d.endsWith(".arpa");
    }

    private static String resolveBootstrapIp(VpnService vpn, SkidConfig cfg, String host) throws Exception {
        if (isLiteralIp(host)) return host;
        String cached = BOOTSTRAP_CACHE.get(host.toLowerCase(Locale.US));
        if (cached != null) return cached;

        String bootEndpoint = (cfg != null && cfg.bootstrapDns != null && !cfg.bootstrapDns.trim().isEmpty())
                ? cfg.bootstrapDns.trim() : "1.1.1.1:53";
        byte[] q = DnsPacketCodec.buildWireQuery(host, 1);
        UpstreamResult r = resolvePlainProxy(vpn, cfg, bootEndpoint, q, System.currentTimeMillis());
        if (r.wireResponse != null) {
            String ip = DnsPacketCodec.extractAnswerSummary(r.wireResponse);
            if (ip != null && ip.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                BOOTSTRAP_CACHE.put(host.toLowerCase(Locale.US), ip);
                return ip;
            }
        }
        String resolved = InetAddress.getByName(host).getHostAddress();
        BOOTSTRAP_CACHE.put(host.toLowerCase(Locale.US), resolved);
        return resolved;
    }

    private static String resolveRethinkDohUrl(SkidConfig cfg) {
        if (cfg == null) return "https://sky.rethinkdns.com/rec";
        if (cfg.rethinkDoh != null && cfg.rethinkDoh.startsWith("https://")) {
            return cfg.rethinkDoh.trim();
        }
        for (String s : cfg.dnsServers) {
            if (s.startsWith("https://")) return s.trim();
        }
        return "https://sky.rethinkdns.com/rec";
    }

    private static String resolveDoTTarget(SkidConfig cfg) {
        if (cfg == null) return "tls://1dot1dot1dot1.cloudflare-dns.com";
        if (cfg.dnsOverTls != null) {
            if (cfg.dnsOverTls.startsWith("tls://")) return cfg.dnsOverTls;
            DnsPresets.DnsEndpoint ep = DnsPresets.findByNameOrValue(DnsPresets.DOT_SERVERS, cfg.dnsOverTls);
            if (ep != null) return ep.value;
        }
        return "tls://1dot1dot1dot1.cloudflare-dns.com";
    }

    private static String resolveDnsCryptStamp(SkidConfig cfg) {
        if (cfg == null) return DnsPresets.DNSCRYPT_SERVERS.get(3).value;
        if (cfg.dnsCrypt != null) {
            if (cfg.dnsCrypt.startsWith("sdns://")) return cfg.dnsCrypt;
            DnsPresets.DnsEndpoint ep = DnsPresets.findByNameOrValue(DnsPresets.DNSCRYPT_SERVERS, cfg.dnsCrypt);
            if (ep != null) return ep.value;
        }
        return DnsPresets.DNSCRYPT_SERVERS.get(3).value;
    }

    private static String resolveDnsProxyTarget(SkidConfig cfg) {
        if (cfg == null) return "9.9.9.9:53";
        if (cfg.torEnabled) return "127.0.0.1:5400";
        if (cfg.dnsProxy != null) {
            DnsPresets.DnsEndpoint ep = DnsPresets.findByNameOrValue(DnsPresets.DNS_PROXY_SERVERS, cfg.dnsProxy);
            if (ep != null) return ep.value;
            if (cfg.dnsProxy.contains(":")) return cfg.dnsProxy;
        }
        return "9.9.9.9:53";
    }

    private static String mapKnownStampIpToSni(String ip, String providerName) {
        if ("185.228.168.168".equals(ip)) return "family-filter-dns.cleanbrowsing.org";
        if ("94.140.14.14".equals(ip)) return "dns.adguard-dns.com";
        if ("94.140.14.15".equals(ip)) return "family.adguard-dns.com";
        if ("9.9.9.9".equals(ip)) return "dns.quad9.net";
        if ("9.9.9.10".equals(ip)) return "dns10.quad9.net";
        if (providerName != null && providerName.contains("quad9")) return "dns.quad9.net";
        if (providerName != null && providerName.contains("adguard")) return "dns.adguard-dns.com";
        return null;
    }

    private static boolean isLiteralIp(String host) {
        if (host == null) return false;
        return host.matches("\\d+\\.\\d+\\.\\d+\\.\\d+") || host.contains(":");
    }

    private static String readAsciiLine(InputStream in) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(128);
        int prev = -1;
        int c;
        while ((c = in.read()) != -1) {
            if (prev == '\r' && c == '\n') {
                byte[] b = baos.toByteArray();
                return new String(b, 0, Math.max(0, b.length - 1), StandardCharsets.US_ASCII);
            }
            baos.write(c);
            prev = c;
            if (baos.size() > 4096) break;
        }
        return baos.size() == 0 ? null : new String(baos.toByteArray(), StandardCharsets.US_ASCII);
    }

    private static byte[] readChunkedBody(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while (true) {
            String lenLine = readAsciiLine(in);
            if (lenLine == null) break;
            int semi = lenLine.indexOf(';');
            if (semi > 0) lenLine = lenLine.substring(0, semi);
            int chunkSize = Integer.parseInt(lenLine.trim(), 16);
            if (chunkSize == 0) {
                readAsciiLine(in);
                break;
            }
            byte[] buf = new byte[chunkSize];
            new DataInputStream(in).readFully(buf);
            out.write(buf);
            readAsciiLine(in);
        }
        return out.toByteArray();
    }

    private static byte[] readAllBytesBounded(InputStream in, int maxBytes) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int n;
        while ((n = in.read(buf)) != -1) {
            out.write(buf, 0, n);
            if (out.size() >= maxBytes) break;
        }
        return out.toByteArray();
    }

    private static void closeQuietly(Socket s) {
        if (s != null) {
            try {
                s.close();
            } catch (Exception ignored) {
            }
        }
    }
}
