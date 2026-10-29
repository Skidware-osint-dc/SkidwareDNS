package com.skidwaredns.app.model;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Built-in catalog of RethinkDNS Sky servers, LBox DNS Templates, DNS Proxy resolvers,
 * DNSCrypt sdns:// stamps & Anonymized Relays, DNS-over-TLS endpoints, Tunnel types
 * (SSH, V2Ray/Xray, Hysteria 1/2, SlowUDP, MASQUE-H3, Shadowsocks, WireGuard, IKEv2, L2TP),
 * Connect-From / Jump-Host modes, Spoof-SNI hosts, and binary sdns:// decoder.
 */
public final class DnsPresets {

    public static final class DnsEndpoint {
        public final String id;
        public final String category; // RETHINK_DOH, DNS_PROXY, DNSCRYPT, DOT, LBOX_TEMPLATE, DNSCRYPT_RELAY
        public final String name;
        public final String subtitle;
        public final String value;

        public DnsEndpoint(String id, String category, String name, String subtitle, String value) {
            this.id = id;
            this.category = category;
            this.name = name;
            this.subtitle = subtitle;
            this.value = value;
        }
    }

    public static final class DecodedStamp {
        public final boolean valid;
        public final int protoId;
        public final String protoName;
        public final boolean dnssec;
        public final boolean noLog;
        public final boolean noFilter;
        public final String serverAddr;
        public final String host;
        public final int port;
        public final String publicKeyHex;
        public final String providerName;
        public final String path;
        public final String error;

        public DecodedStamp(boolean valid, int protoId, String protoName,
                            boolean dnssec, boolean noLog, boolean noFilter,
                            String serverAddr, String host, int port,
                            String publicKeyHex, String providerName, String path, String error) {
            this.valid = valid;
            this.protoId = protoId;
            this.protoName = protoName;
            this.dnssec = dnssec;
            this.noLog = noLog;
            this.noFilter = noFilter;
            this.serverAddr = serverAddr;
            this.host = host;
            this.port = port;
            this.publicKeyHex = publicKeyHex;
            this.providerName = providerName;
            this.path = path;
            this.error = error;
        }

        public String summaryBadges() {
            if (!valid) return "Invalid Stamp: " + error;
            StringBuilder sb = new StringBuilder();
            sb.append(protoName).append(" | ").append(serverAddr);
            if (!providerName.isEmpty()) sb.append(" | ").append(providerName);
            List<String> flags = new ArrayList<>();
            if (dnssec) flags.add("DNSSEC");
            if (noLog) flags.add("NO-LOG");
            if (noFilter) flags.add("NO-FILTER");
            if (!flags.isEmpty()) {
                sb.append(" [");
                for (int i = 0; i < flags.size(); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(flags.get(i));
                }
                sb.append("]");
            }
            return sb.toString();
        }
    }

    public static final class HostPort {
        public final String host;
        public final int port;

        public HostPort(String host, int port) {
            this.host = host;
            this.port = port;
        }
    }

    // Remote v3 DNSCrypt & Anonymized Relay & I2P Hosts Lists
    public static final String[] DNSCRYPT_RESOLVER_SOURCES = new String[]{
            "https://raw.githubusercontent.com/DNSCrypt/dnscrypt-resolvers/master/v3/public-resolvers.md",
            "https://download.dnscrypt.info/resolvers-list/v3/public-resolvers.md",
            "https://ipv6.download.dnscrypt.info/resolvers-list/v3/public-resolvers.md"
    };

    public static final String[] DNSCRYPT_RELAY_SOURCES = new String[]{
            "https://raw.githubusercontent.com/DNSCrypt/dnscrypt-resolvers/master/v3/relays.md",
            "https://download.dnscrypt.info/resolvers-list/v3/relays.md",
            "https://ipv6.download.dnscrypt.info/resolvers-list/v3/relays.md"
    };

    public static final String DEFAULT_I2P_HOSTS_URL =
            "http://shx5vqsw7usdaunyzr2qmes2fq37oumybpudrd4jjj4e4vk4uusa.b32.i2p/hosts.txt";

    public static final String[] SPOOF_SNI_HOSTS = new String[]{
            "play.googleapis.com",
            "drive.google.com",
            "cdn.ampproject.org",
            "api.github.com",
            "ajax.aspnetcdn.com",
            "verizon.com",
            "eset.com"
    };

    public static final String[] TUN_STACKS = new String[]{
            "system",
            "gvisor",
            "mixed"
    };

    public static final String[] JA3_FINGERPRINTS = new String[]{
            "Chrome-128",
            "Firefox-130",
            "Safari-18",
            "Android-OkHttp",
            "Random-JA3",
            "System-Default"
    };

    public static final String[] APP_OWNERSHIP_LEVELS = new String[]{
            "Disabled",
            "Standard",
            "Advanced"
    };

    // HTTP Injector v4.0 V2Ray/Xray, Hysteria & Connection Constants
    public static final String[] XRAY_CORES = new String[]{
            "Xray",
            "V2Ray (v2fly)"
    };

    public static final String[] XRAY_PROTOCOLS = new String[]{
            "VMess",
            "VLESS",
            "Trojan",
            "Shadowsocks",
            "Socks",
            "Wireguard"
    };

    public static final String[] XRAY_NETWORK_TYPES = new String[]{
            "TCP",
            "mKCP",
            "WebSocket",
            "HTTP Upgrade",
            "HTTP/2",
            "XHTTP",
            "QUIC",
            "gRPC"
    };

    public static final String[] XRAY_HEADER_TYPES = new String[]{
            "None",
            "HTTP"
    };

    public static final String[] XRAY_SECURITIES = new String[]{
            "None",
            "TLS",
            "Reality"
    };

    public static final String[] XRAY_DOMAIN_STRATEGIES = new String[]{
            "AsIs (Default)",
            "IPIfNonMatch",
            "IPOnDemand"
    };

    public static final String[] XRAY_ROUTING_RULES = new String[]{
            "Proxy All",
            "Direct China (CN)",
            "Direct Iran (IR)",
            "Direct Russia (RU)",
            "Direct Saudi Arabia (SA)",
            "Block Ads (Basic)"
    };

    public static final String[] HYSTERIA_VERSIONS = new String[]{
            "2",
            "1"
    };

    public static final String[] HYSTERIA_PROTOCOLS = new String[]{
            "UDP",
            "wechat-video",
            "faketcp"
    };

    public static final String[] VPN_DNS_PRESETS = new String[]{
            "Cloudflare DNS",
            "Google DNS",
            "Quad9",
            "AdGuard",
            "Custom DNS"
    };

    // Exact 11 Shadowsocks Encrypt Methods (Screenshot 184834, including Shadowsocks-2022 BLAKE3)
    public static final String[] SHADOWSOCKS_ENCRYPT_METHODS = new String[]{
            "none",
            "plain",
            "aes-256-gcm",
            "aes-128-gcm",
            "chacha20-poly1305",
            "chacha20-ietf-poly1305",
            "xchacha20-poly1305",
            "xchacha20-ietf-poly1305",
            "2022-blake3-aes-128-gcm",
            "2022-blake3-aes-256-gcm",
            "2022-blake3-chacha20-poly1305"
    };

    // VMess / Xray Server-Level Cipher Security (Screenshot 184448)
    public static final String[] XRAY_SERVER_CIPHERS = new String[]{
            "None",
            "Auto",
            "AES-128-GCM",
            "ChaCha20-Poly1305",
            "Zero"
    };

    // Exact 14 DNS over HTTPS (DoH) Providers (Screenshot 184849)
    public static final String[] INJECTOR_DOH_PROVIDERS = new String[]{
            "Disabled",
            "Cloudflare",
            "Google",
            "AdGuard",
            "Quad9",
            "AliDNS",
            "DNSPod",
            "360",
            "Quad 101",
            "Mullvad",
            "ControlD",
            "Najalla",
            "SheCan",
            "LibreDNS"
    };

    public static final String[] UI_LANGUAGES = new String[]{
            "Default",
            "English",
            "Español",
            "Русский",
            "فارسی",
            "العربية",
            "简体中文",
            "Bahasa Indonesia",
            "Português"
    };

    // NymVPN v5.0 Profiles, Mixnet Tuning & Anti-Censorship Constants (190916, 190903, 190908, 185659)
    public static final String[] NYM_PROFILES = new String[]{
            "Auto",
            "Anonymous",
            "Fastest",
            "Random"
    };

    public static final String[] MIXNET_CONTINUOUS_RATES = new String[]{
            "2 Mbps",
            "1 Mbps",
            "0.7 Mbps"
    };

    public static final String[] MIXNET_COVER_RATES = new String[]{
            "Low",
            "Balanced (5x)",
            "Medium (10x)",
            "High (20x)"
    };

    public static final int[] MIXNET_DELAY_STEPS_MS = new int[]{
            0, 15, 50, 100, 200
    };

    public static String getInjectorDohUrl(String provider) {
        if (provider == null || provider.trim().isEmpty() || "Disabled".equalsIgnoreCase(provider.trim())) {
            return "";
        }
        String p = provider.trim();
        if ("Cloudflare".equalsIgnoreCase(p)) return "https://cloudflare-dns.com/dns-query";
        if ("Google".equalsIgnoreCase(p)) return "https://dns.google/dns-query";
        if ("AdGuard".equalsIgnoreCase(p)) return "https://dns.adguard-dns.com/dns-query";
        if ("Quad9".equalsIgnoreCase(p)) return "https://dns.quad9.net/dns-query";
        if ("AliDNS".equalsIgnoreCase(p)) return "https://dns.alidns.com/dns-query";
        if ("DNSPod".equalsIgnoreCase(p)) return "https://doh.pub/dns-query";
        if ("360".equalsIgnoreCase(p)) return "https://doh.360.cn/dns-query";
        if ("Quad 101".equalsIgnoreCase(p)) return "https://dns.twnic.tw/dns-query";
        if ("Mullvad".equalsIgnoreCase(p)) return "https://dns.mullvad.net/dns-query";
        if ("ControlD".equalsIgnoreCase(p)) return "https://freedns.controld.com/p0";
        if ("Najalla".equalsIgnoreCase(p)) return "https://dns.njal.la/dns-query";
        if ("SheCan".equalsIgnoreCase(p)) return "https://free.shecan.ir/dns-query";
        if ("LibreDNS".equalsIgnoreCase(p)) return "https://doh.libredns.gr/dns-query";
        if (p.startsWith("https://")) return p;
        return "https://dns.quad9.net/dns-query";
    }

    // 1. RethinkDNS Sky Servers (DoH RFC 8484)
    public static final List<DnsEndpoint> RETHINK_SERVERS;
    // 2. DNS Proxy Servers (UDP/TCP Wire)
    public static final List<DnsEndpoint> DNS_PROXY_SERVERS;
    // 3. DNSCrypt sdns:// Stamps
    public static final List<DnsEndpoint> DNSCRYPT_SERVERS;
    // 4. DNS-over-TLS (DoT port 853)
    public static final List<DnsEndpoint> DOT_SERVERS;
    // 5. LBox Toggleable DNS Templates
    public static final List<DnsEndpoint> LBOX_TEMPLATES;
    // 6. Built-in Anonymized DNSCrypt Relays (from v3/relays.md)
    public static final List<DnsEndpoint> DNSCRYPT_RELAYS;

    public static final String[] OPERATING_MODES = new String[]{
            "DNS+Firewall",
            "VPN+DNS+Firewall",
            "Proxy-Only",
            "Tor+DNSCrypt+VPN",
            "Tor+DNSCrypt+I2P",
            "Mixnet-5Hop"
    };

    public static final String[] DNS_MODES = new String[]{
            "Rethink-DoH",
            "Injector-DoH",
            "DNSCrypt",
            "DNS-over-TLS",
            "DNS-Proxy",
            "LBox-Template",
            "Fake-IP"
    };

    public static final String[] PROXY_MODES = new String[]{
            "SOCKS5",
            "HTTP",
            "Orbot-Tor",
            "I2P-Tunnel",
            "HTTPS-Jump",
            "SSH-Jump",
            "MASQUE-H3",
            "None"
    };

    public static final String[] TUNNEL_TYPES = new String[]{
            "SSH",
            "V2Ray/Xray",
            "Hysteria",
            "Hysteria2",
            "SlowUDP",
            "MASQUE-H3",
            "Shadowsocks",
            "WireGuard",
            "IKEv2/IPsec",
            "L2TP/IPsec",
            "None"
    };

    public static final String[] CONNECT_FROM_MODES = new String[]{
            "Direct",
            "Http proxy",
            "TLS/SSL Proxy",
            "TLS/SSL (stunnel)",
            "DNS (DNSTT)",
            "HTTP (Obfs)",
            "TLS/SSL (Obfs)",
            "HTTPS Jump",
            "SSH Jump"
    };

    static {
        List<DnsEndpoint> rdns = new ArrayList<>();
        rdns.add(new DnsEndpoint(
                "rdns_privacy", "RETHINK_DOH", "RDNS Privacy",
                "Blocks 100k+ adware, spyware, and trackers",
                "https://sky.rethinkdns.com/1:QAcCAIAcAhCkAg=="));
        rdns.add(new DnsEndpoint(
                "rdns_default", "RETHINK_DOH", "RDNS Default",
                "Blocks 100k+ phishing, malvertising, malware, spyware, ransomware, cryptojacking & threats",
                "https://sky.rethinkdns.com/rec"));
        rdns.add(new DnsEndpoint(
                "rdns_adult", "RETHINK_DOH", "RDNS Adult",
                "Blocks over 30k adult websites",
                "https://sky.rethinkdns.com/pec"));
        rdns.add(new DnsEndpoint(
                "rdns_piracy", "RETHINK_DOH", "RDNS Piracy",
                "Blocks torrent, dubious video streaming and file sharing websites",
                "https://sky.rethinkdns.com/1:EID-BwCB"));
        rdns.add(new DnsEndpoint(
                "rdns_security", "RETHINK_DOH", "RDNS Security",
                "Blocks over 150k malware, ransomware, phishing and other threats",
                "https://sky.rethinkdns.com/sec"));
        RETHINK_SERVERS = Collections.unmodifiableList(rdns);

        List<DnsEndpoint> proxy = new ArrayList<>();
        proxy.add(new DnsEndpoint(
                "proxy_orbot", "DNS_PROXY", "Orbot",
                "Local Tor DNSPort onion resolver (InviZible / Orbot)",
                "127.0.0.1:5400"));
        proxy.add(new DnsEndpoint(
                "proxy_google", "DNS_PROXY", "Google",
                "Google Public DNS IPv4",
                "8.8.8.8:53"));
        proxy.add(new DnsEndpoint(
                "proxy_cloudflare", "DNS_PROXY", "Cloudflare",
                "Cloudflare 1.1.1.1 fast anycast resolver",
                "1.1.1.1:53"));
        proxy.add(new DnsEndpoint(
                "proxy_quad9", "DNS_PROXY", "Quad9",
                "Quad9 threat-intelligence filtered IPv4 DNS",
                "9.9.9.9:53"));
        proxy.add(new DnsEndpoint(
                "proxy_google_v6", "DNS_PROXY", "Google IPv6",
                "Google Public DNS over IPv6",
                "2001:4860:4860::8888:53"));
        proxy.add(new DnsEndpoint(
                "proxy_cloudflare_v6", "DNS_PROXY", "Cloudflare IPv6",
                "Cloudflare anycast DNS over IPv6",
                "2606:4700:4700::1111:53"));
        proxy.add(new DnsEndpoint(
                "proxy_quad9_v6", "DNS_PROXY", "Quad9 IPv6",
                "Quad9 threat-blocking DNS over IPv6",
                "2620:fe::fe:53"));
        DNS_PROXY_SERVERS = Collections.unmodifiableList(proxy);

        List<DnsEndpoint> crypt = new ArrayList<>();
        crypt.add(new DnsEndpoint(
                "dnscrypt_cleanbrowsing", "DNSCRYPT", "Cleanbrowsing Family",
                "Family-safe DNSCrypt stamp (185.228.168.168:8443)",
                "sdns://AQMAAAAAAAAAFDE4NS4yMjguMTY4LjE2ODo4NDQzILysMvrVQ2kXHwgy1gdQJ8MgjO7w6OmflBjcd2Bl1I8pEWNsZWFuYnJvd3Npbmcub3Jn"));
        crypt.add(new DnsEndpoint(
                "dnscrypt_adguard", "DNSCRYPT", "Adguard",
                "AdGuard default ad & tracker blocking DNSCrypt (94.140.14.14:5443)",
                "sdns://AQMAAAAAAAAAETk0LjE0MC4xNC4xNDo1NDQzINErR_JS3PLCu_iZEIbq95zkSV2LFsigxDIuUso_OQhzIjIuZG5zY3J5cHQuZGVmYXVsdC5uczEuYWRndWFyZC5jb20"));
        crypt.add(new DnsEndpoint(
                "dnscrypt_adguard_family", "DNSCRYPT", "Adguard Family",
                "AdGuard family protection + adblock DNSCrypt (94.140.14.15:5443)",
                "sdns://AQMAAAAAAAAAETk0LjE0MC4xNC4xNTo1NDQzILgxXdexS27jIKRw3C7Wsao5jMnlhvhdRUXWuMm1AFq6ITIuZG5zY3J5cHQuZmFtaWx5Lm5zMS5hZGd1YXJkLmNvbQ"));
        crypt.add(new DnsEndpoint(
                "dnscrypt_quad9_sec", "DNSCRYPT", "Quad9 Security",
                "Quad9 malware/phishing blocking DNSCrypt + DNSSEC (9.9.9.9:8443)",
                "sdns://AQMAAAAAAAAADDkuOS45Ljk6ODQ0MyBnyEe4yHWM0SAkVUO-dWdG3zTfHYTAC4xHA2jfgh2GPhkyLmRuc2NyeXB0LWNlcnQucXVhZDkubmV0"));
        crypt.add(new DnsEndpoint(
                "dnscrypt_quad9", "DNSCRYPT", "Quad9",
                "Quad9 unfiltered DNSSEC + No-Log DNSCrypt (9.9.9.10:8443)",
                "sdns://AQcAAAAAAAAADTkuOS45LjEwOjg0NDMgZ8hHuMh1jNEgJFVDvnVnRt803x2EwAuMRwNo34Idhj4ZMi5kbnNjcnlwdC1jZXJ0LnF1YWQ5Lm5ldA"));
        crypt.add(new DnsEndpoint(
                "dnscrypt_aa", "DNSCRYPT", "a-and-a (No-Log DoH)",
                "Andrews & Arnold DNSSEC + No-Log (217.169.20.22)",
                "sdns://AgcAAAAAAAAADTIxNy4xNjkuMjAuMjIADWRucy5hYS5uZXQudWsKL2Rucy1xdWVyeQ"));
        DNSCRYPT_SERVERS = Collections.unmodifiableList(crypt);

        List<DnsEndpoint> dot = new ArrayList<>();
        dot.add(new DnsEndpoint(
                "dot_cloudflare", "DOT", "Cloudflare",
                "Cloudflare's DNS over TLS. No blocking",
                "tls://1dot1dot1dot1.cloudflare-dns.com"));
        dot.add(new DnsEndpoint(
                "dot_cloudflare_family", "DOT", "Cloudflare Family",
                "Cloudflare's DNS over TLS. Blocks malware and adult",
                "tls://family.cloudflare-dns.com"));
        dot.add(new DnsEndpoint(
                "dot_adguard", "DOT", "Adguard",
                "AdGuard DNS over TLS. Blocks ads, tracking and phishing",
                "tls://dns.adguard-dns.com"));
        dot.add(new DnsEndpoint(
                "dot_controld", "DOT", "ControlD",
                "ControlD Freedns - blocks spyware and tracking domains",
                "tls://p2.freedns.controld.com"));
        DOT_SERVERS = Collections.unmodifiableList(dot);

        // LBox Toggleable DNS Templates (matching Screenshot_20261028_183002_LBox.jpg)
        List<DnsEndpoint> lbox = new ArrayList<>();
        lbox.add(new DnsEndpoint(
                "google_dot", "LBOX_TEMPLATE", "Google DoT",
                "google_dot · tls · 8.8.8.8",
                "tls://dns.google"));
        lbox.add(new DnsEndpoint(
                "google_doh", "LBOX_TEMPLATE", "Google DoH",
                "google_doh · https · 8.8.8.8",
                "https://dns.google/dns-query"));
        lbox.add(new DnsEndpoint(
                "cloudflare_udp", "LBOX_TEMPLATE", "Cloudflare UDP",
                "cloudflare_udp · udp · 1.1.1.1",
                "1.1.1.1:53"));
        lbox.add(new DnsEndpoint(
                "cloudflare_dot", "LBOX_TEMPLATE", "Cloudflare DoT",
                "cloudflare_dot · tls · 1.1.1.1",
                "tls://1dot1dot1dot1.cloudflare-dns.com"));
        lbox.add(new DnsEndpoint(
                "safe_dns_dot", "LBOX_TEMPLATE", "Safe DNS (DoT)",
                "safe_dns_dot · tls · 9.9.9.9",
                "tls://dns.quad9.net"));
        lbox.add(new DnsEndpoint(
                "opendns_udp", "LBOX_TEMPLATE", "OpenDNS UDP (direct)",
                "opendns_udp · udp · 208.67.222.222",
                "208.67.222.222:53"));
        lbox.add(new DnsEndpoint(
                "opendns_doh", "LBOX_TEMPLATE", "OpenDNS DoH",
                "opendns_doh · https · 208.67.222.222",
                "https://doh.opendns.com/dns-query"));
        lbox.add(new DnsEndpoint(
                "quad9_doh", "LBOX_TEMPLATE", "Quad9 DoH (via VPN, anti-malware)",
                "quad9_doh · https · 9.9.9.9",
                "https://dns.quad9.net/dns-query"));
        lbox.add(new DnsEndpoint(
                "yandex_dot", "LBOX_TEMPLATE", "Yandex DoT (direct)",
                "yandex_dot · tls · 77.88.8.8",
                "tls://common.dot.dns.yandex.net"));
        LBOX_TEMPLATES = Collections.unmodifiableList(lbox);

        // Built-in Anonymized DNSCrypt Relays (from DNSCrypt/dnscrypt-resolvers v3/relays.md)
        List<DnsEndpoint> relays = new ArrayList<>();
        relays.add(new DnsEndpoint(
                "anon-cryptids-relay", "DNSCRYPT_RELAY", "anon-cryptids-relay",
                "Public DNSCrypt anonymized relay (75.119.144.0:8443)",
                "sdns://gRE3NS4xMTkuMTQ0LjA6ODQ0Mw"));
        relays.add(new DnsEndpoint(
                "anon-cs-ch", "DNSCRYPT_RELAY", "anon-cs-ch (Switzerland)",
                "CryptoStorm Switzerland anonymized relay (190.211.255.227:443)",
                "sdns://gRMxOTAuMjExLjI1NS4yMjc6NDQz"));
        relays.add(new DnsEndpoint(
                "anon-cs-de", "DNSCRYPT_RELAY", "anon-cs-de (Frankfurt)",
                "CryptoStorm Germany anonymized relay (146.70.82.3:443)",
                "sdns://gQ8xNDYuNzAuODIuMzo0NDM"));
        relays.add(new DnsEndpoint(
                "anon-cs-nl", "DNSCRYPT_RELAY", "anon-cs-nl (Netherlands)",
                "CryptoStorm Netherlands anonymized relay (185.107.80.84:443)",
                "sdns://gRExODUuMTA3LjgwLjg0OjQ0Mw"));
        relays.add(new DnsEndpoint(
                "anon-cs-la", "DNSCRYPT_RELAY", "anon-cs-la (Los Angeles)",
                "CryptoStorm US-West anonymized relay (195.206.104.203:443)",
                "sdns://gRMxOTUuMjA2LjEwNC4yMDM6NDQz"));
        relays.add(new DnsEndpoint(
                "anon-cs-dc", "DNSCRYPT_RELAY", "anon-cs-dc (Washington DC)",
                "CryptoStorm US-East anonymized relay (198.7.58.227:443)",
                "sdns://gRAxOTguNy41OC4yMjc6NDQz"));
        DNSCRYPT_RELAYS = Collections.unmodifiableList(relays);
    }

    private DnsPresets() {}

    public static DnsEndpoint findByNameOrValue(List<DnsEndpoint> list, String query) {
        if (query == null || query.trim().isEmpty()) return null;
        String q = query.trim();
        for (DnsEndpoint ep : list) {
            if (ep.name.equalsIgnoreCase(q) || ep.value.equalsIgnoreCase(q) || ep.id.equalsIgnoreCase(q)) {
                return ep;
            }
        }
        return null;
    }

    public static HostPort parseHostPort(String raw, int defaultPort) {
        if (raw == null || raw.trim().isEmpty()) {
            return new HostPort("1.1.1.1", defaultPort);
        }
        String s = raw.trim();
        if (s.startsWith("tls://")) s = s.substring(6);
        if (s.startsWith("tcp://") || s.startsWith("udp://")) s = s.substring(6);

        if (s.startsWith("[")) {
            int close = s.indexOf(']');
            if (close > 0) {
                String host = s.substring(1, close);
                int port = defaultPort;
                if (close + 2 <= s.length() && s.charAt(close + 1) == ':') {
                    port = safePort(s.substring(close + 2), defaultPort);
                }
                return new HostPort(host, port);
            }
        }

        int firstColon = s.indexOf(':');
        int lastColon = s.lastIndexOf(':');
        if (firstColon < 0) {
            return new HostPort(s, defaultPort);
        }
        if (firstColon == lastColon) {
            String host = s.substring(0, firstColon);
            int port = safePort(s.substring(firstColon + 1), defaultPort);
            return new HostPort(host, port);
        }

        if (s.endsWith(":53") && !s.endsWith("::53")) {
            String candidateHost = s.substring(0, lastColon);
            return new HostPort(candidateHost, 53);
        }
        if (s.endsWith(":853") || s.endsWith(":5353") || s.endsWith(":5400")
                || s.endsWith(":8443") || s.endsWith(":5443") || s.endsWith(":443")) {
            String candidateHost = s.substring(0, lastColon);
            int p = safePort(s.substring(lastColon + 1), defaultPort);
            return new HostPort(candidateHost, p);
        }
        return new HostPort(s, defaultPort);
    }

    private static int safePort(String s, int fallback) {
        try {
            int p = Integer.parseInt(s.trim());
            return (p > 0 && p <= 65535) ? p : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    /**
     * Binary decoder for DNS Stamps (0x01 DNSCrypt, 0x02 DoH, 0x03 DoT, 0x05 ODoH, 0x81 DNSCrypt-Relay, 0x85 ODoH-Relay).
     */
    public static DecodedStamp decodeSdnsStamp(String stamp) {
        if (stamp == null || !stamp.trim().startsWith("sdns://")) {
            return new DecodedStamp(false, 0, "UNKNOWN", false, false, false,
                    "", "", 0, "", "", "", "Missing sdns:// scheme");
        }
        try {
            String b64 = stamp.trim().substring(7).trim();
            byte[] data = decodeBase64Url(b64);
            if (data == null || data.length < 3) {
                return new DecodedStamp(false, 0, "UNKNOWN", false, false, false,
                        "", "", 0, "", "", "", "Stamp payload too short");
            }
            int pos = 0;
            int proto = data[pos++] & 0xFF;

            // Anonymized DNSCrypt relay (0x81) has NO 8-byte props header: [0x81][LP(addr)]
            if (proto == 0x81) {
                int addrLen = data[pos++] & 0xFF;
                if (pos + addrLen > data.length) {
                    return new DecodedStamp(false, proto, "DNSCrypt-Relay", false, true, true,
                            "", "", 0, "", "", "", "Invalid relay addr length");
                }
                String addr = new String(data, pos, addrLen, StandardCharsets.UTF_8);
                HostPort hp = parseHostPort(addr, 443);
                return new DecodedStamp(true, proto, "DNSCrypt-Relay", false, true, true,
                        addr, hp.host, hp.port, "", "Anonymized-Relay", "", "");
            }

            if (data.length < 10) {
                return new DecodedStamp(false, proto, "UNKNOWN", false, false, false,
                        "", "", 0, "", "", "", "Stamp payload too short");
            }
            long props = 0;
            for (int i = 0; i < 8; i++) {
                props |= ((long) (data[pos++] & 0xFF)) << (8 * i);
            }
            boolean dnssec = (props & 0x01L) != 0;
            boolean noLog = (props & 0x02L) != 0;
            boolean noFilter = (props & 0x04L) != 0;

            String protoName;
            switch (proto) {
                case 0x00: protoName = "Plain-DNS"; break;
                case 0x01: protoName = "DNSCrypt"; break;
                case 0x02: protoName = "DoH"; break;
                case 0x03: protoName = "DoT"; break;
                case 0x05: protoName = "ODoH-Target"; break;
                case 0x85: protoName = "ODoH-Relay"; break;
                default: protoName = "Proto-0x" + Integer.toHexString(proto); break;
            }

            if (pos >= data.length) {
                return new DecodedStamp(false, proto, protoName, dnssec, noLog, noFilter,
                        "", "", 0, "", "", "", "Truncated address");
            }
            int addrLen = data[pos++] & 0xFF;
            if (pos + addrLen > data.length) {
                return new DecodedStamp(false, proto, protoName, dnssec, noLog, noFilter,
                        "", "", 0, "", "", "", "Invalid address length");
            }
            String addr = new String(data, pos, addrLen, StandardCharsets.UTF_8);
            pos += addrLen;

            int defaultPort = (proto == 0x01) ? 443 : (proto == 0x03 ? 853 : (proto == 0x02 || proto == 0x05 || proto == 0x85 ? 443 : 53));
            HostPort hp = parseHostPort(addr, defaultPort);

            String pkHex = "";
            String provider = "";
            String path = "";

            if (proto == 0x01) {
                if (pos < data.length) {
                    int pkLen = data[pos++] & 0xFF;
                    if (pos + pkLen <= data.length) {
                        pkHex = bytesToHex(data, pos, pkLen);
                        pos += pkLen;
                    }
                }
                if (pos < data.length) {
                    int provLen = data[pos++] & 0xFF;
                    if (pos + provLen <= data.length) {
                        provider = new String(data, pos, provLen, StandardCharsets.UTF_8);
                    }
                }
            } else if (proto == 0x02 || proto == 0x03 || proto == 0x05 || proto == 0x85) {
                if (proto != 0x05) {
                    while (pos < data.length) {
                        int vlen = data[pos++] & 0xFF;
                        int len = vlen & 0x7F;
                        pos = Math.min(data.length, pos + len);
                        if ((vlen & 0x80) == 0) break;
                    }
                }
                if (pos < data.length) {
                    int hostLen = data[pos++] & 0xFF;
                    if (pos + hostLen <= data.length) {
                        provider = new String(data, pos, hostLen, StandardCharsets.UTF_8);
                        pos += hostLen;
                    }
                }
                if ((proto == 0x02 || proto == 0x05 || proto == 0x85) && pos < data.length) {
                    int pathLen = data[pos++] & 0xFF;
                    if (pos + pathLen <= data.length) {
                        path = new String(data, pos, pathLen, StandardCharsets.UTF_8);
                    }
                }
            }

            return new DecodedStamp(true, proto, protoName, dnssec, noLog, noFilter,
                    addr, hp.host, hp.port, pkHex, provider, path, "");
        } catch (Exception e) {
            return new DecodedStamp(false, 0, "ERROR", false, false, false,
                    "", "", 0, "", "", "", e.getMessage());
        }
    }

    private static String bytesToHex(byte[] bytes, int offset, int len) {
        StringBuilder sb = new StringBuilder(len * 2);
        for (int i = 0; i < len; i++) {
            int v = bytes[offset + i] & 0xFF;
            if (v < 16) sb.append('0');
            sb.append(Integer.toHexString(v));
        }
        return sb.toString();
    }

    public static byte[] decodeBase64Url(String input) {
        if (input == null) return new byte[0];
        ByteArrayOutputStream out = new ByteArrayOutputStream(input.length() * 3 / 4);
        int buffer = 0;
        int bits = 0;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '=' || Character.isWhitespace(c)) continue;
            int val;
            if (c >= 'A' && c <= 'Z') val = c - 'A';
            else if (c >= 'a' && c <= 'z') val = c - 'a' + 26;
            else if (c >= '0' && c <= '9') val = c - '0' + 52;
            else if (c == '-' || c == '+') val = 62;
            else if (c == '_' || c == '/') val = 63;
            else continue;
            buffer = (buffer << 6) | val;
            bits += 6;
            if (bits >= 8) {
                bits -= 8;
                out.write((buffer >>> bits) & 0xFF);
            }
        }
        return out.toByteArray();
    }
}
