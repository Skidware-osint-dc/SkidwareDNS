package com.skidwaredns.app.config;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manages persisted [.skid] v2.0 profiles on Android device storage + built-in preset templates.
 */
public final class SkidConfigStore {

    public static final class PresetTemplate {
        public final String filename;
        public final String title;
        public final String description;
        public final String skidContent;

        public PresetTemplate(String filename, String title, String description, String skidContent) {
            this.filename = filename;
            this.title = title;
            this.description = description;
            this.skidContent = skidContent;
        }
    }

    private static final String PREF_NAME = "skidwaredns_prefs";
    private static final String KEY_ACTIVE_SKID = "active_skid_raw";
    private static volatile SkidConfig activeCache;

    public static final List<PresetTemplate> BUILTIN_PRESETS;

    static {
        List<PresetTemplate> list = new ArrayList<>();

        list.add(new PresetTemplate(
                "rdns_fortress.skid",
                "RDNS Security Fortress v2.0",
                "RethinkDNS Security + DNS Booster + On-Device Blocklists + Anti-Censorship Auto + DNS Leak Shield",
                "# SKIDWAREDNS CUSTOM CONFIG [.skid] v2.0\n" +
                "# Preset: RethinkDNS Security Fortress + DNS Booster + DPI Auto\n\n" +
                "===========\n" +
                "DNS Servers [https://sky.rethinkdns.com/sec, https://sky.rethinkdns.com/1:QAcCAIAcAhCkAg==]\n" +
                "VPN Servers []\n" +
                "Addresses [10.213.37.2/24, fd00:534b:4944::2/64]\n" +
                "SOCKS5 [127.0.0.1:10808]\n" +
                "VPN PORTS [51820, 443]\n" +
                "PROXY PORTS [10808, 10809]\n" +
                "DNS PORTS [53, 853, 443]\n" +
                "Custom DNS Server [https://sky.rethinkdns.com/sec]\n" +
                "Custom VPN Server []\n" +
                "Custom Proxy Server []\n" +
                "Firewall Rules [BLOCK_DOMAIN:doubleclick.net, BLOCK_DOMAIN:googleadservices.com, BLOCK_DOMAIN:graph.facebook.com, BLOCK_DOMAIN:app-measurement.com, BLOCK_DOMAIN:ads.tiktok.com, BLOCK_PORT:445, BLOCK_PORT:135]\n" +
                "Allowed DNS Records [Auto]\n" +
                "On-Device Blocklists [Adware & Trackers, Malware & Phishing, OEM Telemetry]\n" +
                "===========\n" +
                "Tor enabled=false\n" +
                "Mode=DNS+Firewall\n" +
                "Name=RDNS-Security-Fortress\n" +
                "Proxy mode=SOCKS5\n" +
                "DNS Mode=Rethink-DoH\n" +
                "Quad9=Security\n" +
                "DNSCrypt=Quad9 Security\n" +
                "DNS-over-TLS=Adguard\n" +
                "DNS-Proxy=Quad9\n" +
                "Tunnel=None\n" +
                "Connect-from-mode=Direct\n" +
                "===========\n" +
                "Global-DNS-Category=Rethink DNS\n" +
                "Rethink-DoH=https://sky.rethinkdns.com/sec\n" +
                "DNS-Booster=true\n" +
                "Prevent-DNS-Leaks=true\n" +
                "Block-Unknown-DNS-Source=true\n" +
                "On-Device-Blocklists-Enabled=true\n" +
                "Shorter-TCP-Keepalive=true\n" +
                "TCP-NoDelay=true\n" +
                "Endpoint-Independent-Mapping=true\n" +
                "Socket-Buffer-KB=512\n" +
                "DPI-Strategy=Auto\n" +
                "DPI-Technique=TCP/TLS\n" +
                "TLS-Record-Split-Bytes=5\n" +
                "Show-Provider-Info=true\n" +
                "Firewall-Bubble=true\n" +
                "Kill-Switch=true\n" +
                "Block-Bypassed-DNS=true\n" +
                "===========\n"
        ));

        list.add(new PresetTemplate(
                "skidwareproxy_dpi_desync.skid",
                "WireGuard skidwareproxy + DPI Desync",
                "WireGuard skidwareproxy (Flood WG + Smart Keepalive + Proxy Lockdown) + Jumbo Bandwidth Booster + TCP/TLS SNI Split",
                "# SKIDWAREDNS CUSTOM CONFIG [.skid] v2.0\n" +
                "# Preset: WireGuard skidwareproxy + Anti-Censorship DPI Desync + Bandwidth Booster\n\n" +
                "===========\n" +
                "DNS Servers [https://sky.rethinkdns.com/1:QAcCAIAcAhCkAg==, tls://1dot1dot1dot1.cloudflare-dns.com]\n" +
                "VPN Servers [wg.skidwareproxy.example:51820]\n" +
                "Addresses [10.213.37.2/24, fd00:534b:4944::2/64]\n" +
                "SOCKS5 [127.0.0.1:10808]\n" +
                "VPN PORTS [51820, 443, 2408]\n" +
                "PROXY PORTS [10808, 10809]\n" +
                "DNS PORTS [443, 853, 53]\n" +
                "Custom DNS Server []\n" +
                "Custom VPN Server [wg.skidwareproxy.example:51820]\n" +
                "Custom Proxy Server []\n" +
                "Firewall Rules [BLOCK_DOMAIN:doubleclick.net, BLOCK_DOMAIN:app-measurement.com, BLOCK_PORT:445]\n" +
                "Allowed DNS Records [A, AAAA, CNAME, HTTPS, SVCB, TXT]\n" +
                "On-Device Blocklists [Adware & Trackers, Malware & Phishing]\n" +
                "===========\n" +
                "Tor enabled=false\n" +
                "Mode=VPN+DNS+Firewall\n" +
                "Name=skidwareproxy-DPI-Desync\n" +
                "Proxy mode=SOCKS5\n" +
                "DNS Mode=Rethink-DoH\n" +
                "Quad9=Security\n" +
                "DNSCrypt=Quad9 Security\n" +
                "DNS-over-TLS=Cloudflare\n" +
                "DNS-Proxy=Cloudflare\n" +
                "Tunnel=WireGuard\n" +
                "Connect-from-mode=TLS/SSL (Obfs)\n" +
                "===========\n" +
                "WG-Enabled=true\n" +
                "WG-Profile-Name=skidwareproxy\n" +
                "WG-Endpoint=wg.skidwareproxy.example:51820\n" +
                "Flood-WireGuard=true\n" +
                "Smart-Persistent-Keepalive=true\n" +
                "Proxy-Lockdown=true\n" +
                "WireGuard-P2P=true\n" +
                "Bandwidth-Booster=true\n" +
                "Socket-Buffer-KB=2048\n" +
                "Shorter-TCP-Keepalive=true\n" +
                "TCP-NoDelay=true\n" +
                "TCP-Fast-Open=true\n" +
                "Endpoint-Independent-Mapping=true\n" +
                "IP-Version=IPv4 + IPv6\n" +
                "Perform-Protocol-Translations=true\n" +
                "DPI-Strategy=Always\n" +
                "DPI-Technique=Hybrid\n" +
                "TLS-Record-Split-Bytes=3\n" +
                "Global-DNS-Category=Smart DNS\n" +
                "DNS-Booster=true\n" +
                "Packet-Capture=Headers-Only\n" +
                "===========\n"
        ));

        list.add(new PresetTemplate(
                "invizible_tor_dnscrypt.skid",
                "InviZible Tor + DNSCrypt + I2P",
                "Orbot Tor DNSPort (127.0.0.1:5400) + SOCKS5 (9050) + Quad9 Security DNSCrypt stamp + Split DNS + QUIC Block",
                "# SKIDWAREDNS CUSTOM CONFIG [.skid] v2.0\n" +
                "# Preset: InviZible Multi-Layer Anonymity\n\n" +
                "===========\n" +
                "DNS Servers [127.0.0.1:5400, sdns://AQMAAAAAAAAADDkuOS45Ljk6ODQ0MyBnyEe4yHWM0SAkVUO-dWdG3zTfHYTAC4xHA2jfgh2GPhkyLmRuc2NyeXB0LWNlcnQucXVhZDkubmV0]\n" +
                "VPN Servers []\n" +
                "Addresses [10.213.40.2/24, fd00:534b:4944::2/64]\n" +
                "SOCKS5 [127.0.0.1:9050, 127.0.0.1:10808]\n" +
                "VPN PORTS [9001, 443]\n" +
                "PROXY PORTS [10808, 10809, 9050, 4444]\n" +
                "DNS PORTS [5400, 8443, 53]\n" +
                "Custom DNS Server [127.0.0.1:5400]\n" +
                "Custom VPN Server []\n" +
                "Custom Proxy Server [127.0.0.1:9050]\n" +
                "Firewall Rules [BLOCK_DOMAIN:telemetry.microsoft.com, BLOCK_DOMAIN:analytics.google.com, BLOCK_PORT:6881]\n" +
                "Allowed DNS Records [A, AAAA, CNAME, TXT]\n" +
                "On-Device Blocklists [Adware & Trackers, OEM Telemetry]\n" +
                "===========\n" +
                "Tor enabled=true\n" +
                "Mode=Tor+DNSCrypt+VPN\n" +
                "Name=InviZible-Onion-Shield\n" +
                "Proxy mode=Orbot-Tor\n" +
                "DNS Mode=DNSCrypt\n" +
                "Quad9=Security\n" +
                "DNSCrypt=Quad9 Security\n" +
                "DNS-over-TLS=ControlD\n" +
                "DNS-Proxy=Orbot\n" +
                "Tunnel=Shadowsocks\n" +
                "Connect-from-mode=TLS/SSL (Obfs)\n" +
                "===========\n" +
                "Global-DNS-Category=Other DNS\n" +
                "On-Device-Blocklists-Enabled=true\n" +
                "Split-DNS=true\n" +
                "Prevent-DNS-Leaks=true\n" +
                "Tor-Bridge-Type=obfs4\n" +
                "I2P-Enabled=true\n" +
                "UDP-QUIC-Policy=Block-Force-TCP\n" +
                "UDP-Randomize-Source-Port=true\n" +
                "Block-UDP-NonDNS=true\n" +
                "Block-Bypassed-DNS=true\n" +
                "Stall-On-Network-Loss=true\n" +
                "===========\n"
        ));

        list.add(new PresetTemplate(
                "hysteria_tls_obfs.skid",
                "Hysteria + TLS/SSL (Obfs)",
                "QUIC/Hysteria high-throughput tunnel over TLS/SSL (Obfs) with ControlD DoT + 1024 KB Socket Buffers + EIM NAT",
                "# SKIDWAREDNS CUSTOM CONFIG [.skid] v2.0\n" +
                "# Preset: Hysteria + TLS/SSL Obfuscation\n\n" +
                "===========\n" +
                "DNS Servers [tls://p2.freedns.controld.com, https://sky.rethinkdns.com/1:QAcCAIAcAhCkAg==]\n" +
                "VPN Servers [edge-hy2.skidnet.example:443]\n" +
                "Addresses [10.213.88.2/24, fd00:534b:4944::88/64]\n" +
                "SOCKS5 [127.0.0.1:10808]\n" +
                "VPN PORTS [443, 8443, 2083]\n" +
                "PROXY PORTS [10808, 10809]\n" +
                "DNS PORTS [853, 443, 53]\n" +
                "Custom DNS Server [tls://p2.freedns.controld.com]\n" +
                "Custom VPN Server [edge-hy2.skidnet.example:443]\n" +
                "Custom Proxy Server [127.0.0.1:10808]\n" +
                "Firewall Rules [BLOCK_DOMAIN:adservice.google.com, BLOCK_DOMAIN:scorecardresearch.com]\n" +
                "Allowed DNS Records [Auto]\n" +
                "On-Device Blocklists [Adware & Trackers]\n" +
                "===========\n" +
                "Tor enabled=false\n" +
                "Mode=VPN+DNS+Firewall\n" +
                "Name=Hysteria-TLS-Obfs\n" +
                "Proxy mode=SOCKS5\n" +
                "DNS Mode=DNS-over-TLS\n" +
                "Quad9=Off\n" +
                "DNSCrypt=Adguard\n" +
                "DNS-over-TLS=ControlD\n" +
                "DNS-Proxy=Cloudflare\n" +
                "Tunnel=Hysteria\n" +
                "Connect-from-mode=TLS/SSL (Obfs)\n" +
                "===========\n" +
                "Global-DNS-Category=Other DNS\n" +
                "Tunnel-Host=edge-hy2.skidnet.example\n" +
                "Tunnel-Port=443\n" +
                "SNI=cdn.cloudflare.com\n" +
                "MTU=1380\n" +
                "Socket-Buffer-KB=1024\n" +
                "Endpoint-Independent-Mapping=true\n" +
                "UDP-QUIC-Policy=Allow\n" +
                "DPI-Strategy=Auto\n" +
                "DPI-Technique=TCP/TLS\n" +
                "===========\n"
        ));

        list.add(new PresetTemplate(
                "xray_http_payload.skid",
                "V2Ray/Xray + HTTP Payload Forge",
                "V2Ray/Xray WebSocket tunnel forged through custom HTTP Proxy payload + Cloudflare DoT + TCP Desync",
                "# SKIDWAREDNS CUSTOM CONFIG [.skid] v2.0\n" +
                "# Preset: V2Ray/Xray + HTTP Proxy Payload Injector\n\n" +
                "===========\n" +
                "DNS Servers [tls://1dot1dot1dot1.cloudflare-dns.com, 1.1.1.1:53]\n" +
                "VPN Servers [xray-ws.skidforge.example:8080]\n" +
                "Addresses [10.213.99.2/24]\n" +
                "SOCKS5 [127.0.0.1:10808]\n" +
                "VPN PORTS [80, 8080, 443, 2053]\n" +
                "PROXY PORTS [10808, 10809, 8888]\n" +
                "DNS PORTS [853, 53]\n" +
                "Custom DNS Server []\n" +
                "Custom VPN Server [xray-ws.skidforge.example:8080]\n" +
                "Custom Proxy Server [127.0.0.1:10809]\n" +
                "Firewall Rules [BLOCK_DOMAIN:ads.yahoo.com, BLOCK_PORT:25]\n" +
                "Allowed DNS Records [Auto]\n" +
                "On-Device Blocklists [oisd_small]\n" +
                "===========\n" +
                "Tor enabled=false\n" +
                "Mode=VPN+DNS+Firewall\n" +
                "Name=Xray-HTTP-Injector\n" +
                "Proxy mode=HTTP\n" +
                "DNS Mode=DNS-over-TLS\n" +
                "Quad9=Off\n" +
                "DNSCrypt=Adguard\n" +
                "DNS-over-TLS=Cloudflare\n" +
                "DNS-Proxy=Cloudflare\n" +
                "Tunnel=V2Ray/Xray\n" +
                "Connect-from-mode=Http proxy\n" +
                "===========\n" +
                "Tunnel-Host=xray-ws.skidforge.example\n" +
                "Tunnel-Port=8080\n" +
                "V2Ray-Transport=ws\n" +
                "SNI=speed.cloudflare.com\n" +
                "Bug-Host=104.16.132.229\n" +
                "HTTP-Payload=GET /ws HTTP/1.1[crlf]Host: [sni][crlf]Upgrade: websocket[crlf]Connection: Upgrade[crlf]User-Agent: SkidForge/2.0[crlf][crlf]\n" +
                "DPI-Strategy=Always\n" +
                "DPI-Technique=TCP\n" +
                "TCP-MSS-Clamp=1340\n" +
                "===========\n"
        ));

        list.add(new PresetTemplate(
                "dnstt_ssh_stealth.skid",
                "SSH over DNS (DNSTT SlowDNS)",
                "Censorship-resistant SSH tunnel encapsulated inside DNSTT recursive DNS queries + Cleanbrowsing/Quad9",
                "# SKIDWAREDNS CUSTOM CONFIG [.skid] v2.0\n" +
                "# Preset: SSH + DNS (DNSTT) Stealth Tunnel\n\n" +
                "===========\n" +
                "DNS Servers [9.9.9.9:53, 2620:fe::fe:53]\n" +
                "VPN Servers [ssh-dnstt.skid.example:22]\n" +
                "Addresses [10.213.53.2/24]\n" +
                "SOCKS5 [127.0.0.1:10808]\n" +
                "VPN PORTS [22, 2222, 443]\n" +
                "PROXY PORTS [10808, 10809]\n" +
                "DNS PORTS [53, 5300, 5353]\n" +
                "Custom DNS Server [9.9.9.9:53]\n" +
                "Custom VPN Server [ssh-dnstt.skid.example:22]\n" +
                "Custom Proxy Server []\n" +
                "Firewall Rules [BLOCK_PORT:445]\n" +
                "Allowed DNS Records [A, AAAA, TXT, CNAME]\n" +
                "On-Device Blocklists [oisd_small]\n" +
                "===========\n" +
                "Tor enabled=false\n" +
                "Mode=VPN+DNS+Firewall\n" +
                "Name=SSH-DNSTT-SlowDNS\n" +
                "Proxy mode=SOCKS5\n" +
                "DNS Mode=DNS-Proxy\n" +
                "Quad9=Standard\n" +
                "DNSCrypt=Cleanbrowsing Family\n" +
                "DNS-over-TLS=Cloudflare Family\n" +
                "DNS-Proxy=Quad9\n" +
                "Tunnel=SSH\n" +
                "Connect-from-mode=DNS (DNSTT)\n" +
                "===========\n" +
                "Tunnel-Host=ssh-dnstt.skid.example\n" +
                "Tunnel-Port=22\n" +
                "Tunnel-User=skidroot\n" +
                "DNSTT-Domain=t.skiddns.example\n" +
                "DNSTT-PubKey=9b4e1120a5c8f0e4718293a4b5c6d7e8f90123456789abcdef9876543210abcd\n" +
                "UDP-EDNS-Payload=1232\n" +
                "Shorter-TCP-Keepalive=true\n" +
                "===========\n"
        ));

        list.add(new PresetTemplate(
                "nym_strongswan_mixnet.skid",
                "Nym 5-Hop Mixnet + strongSwan IKEv2",
                "Anonymous 5-hop Mixnet metadata resistance + IKEv2/IPsec NAT-T ports (500/4500) + RDNS Privacy + Lockdown",
                "# SKIDWAREDNS CUSTOM CONFIG [.skid] v2.0\n" +
                "# Preset: Nym Mixnet + strongSwan IKEv2 Hybrid\n\n" +
                "===========\n" +
                "DNS Servers [https://sky.rethinkdns.com/1:QAcCAIAcAhCkAg==, tls://dns.adguard-dns.com]\n" +
                "VPN Servers [ch-mixnet-entry.nym.skid:1789, ch-ikev2.strongswan.skid:4500]\n" +
                "Addresses [10.213.77.2/24, fd00:534b:4944::77/64]\n" +
                "SOCKS5 [127.0.0.1:10808, 127.0.0.1:1080]\n" +
                "VPN PORTS [500, 4500, 1789, 51820]\n" +
                "PROXY PORTS [10808, 10809, 1080]\n" +
                "DNS PORTS [443, 853, 53]\n" +
                "Custom DNS Server [https://sky.rethinkdns.com/1:QAcCAIAcAhCkAg==]\n" +
                "Custom VPN Server [ch-ikev2.strongswan.skid:4500]\n" +
                "Custom Proxy Server []\n" +
                "Firewall Rules [BLOCK_DOMAIN:app-measurement.com, BLOCK_DOMAIN:crashlytics.com, BLOCK_DOMAIN:branch.io]\n" +
                "Allowed DNS Records [Auto]\n" +
                "On-Device Blocklists [oisd_small, stevenblack_hosts, urlhaus_malware, hagezi_tif]\n" +
                "===========\n" +
                "Tor enabled=false\n" +
                "Mode=Mixnet-5Hop\n" +
                "Name=Nym-StrongSwan-Mixnet\n" +
                "Proxy mode=SOCKS5\n" +
                "DNS Mode=Rethink-DoH\n" +
                "Quad9=Security\n" +
                "DNSCrypt=Quad9 Security\n" +
                "DNS-over-TLS=Adguard\n" +
                "DNS-Proxy=Quad9 IPv6\n" +
                "Tunnel=IKEv2/IPsec\n" +
                "Connect-from-mode=TLS/SSL (stunnel)\n" +
                "===========\n" +
                "Rethink-DoH=https://sky.rethinkdns.com/1:QAcCAIAcAhCkAg==\n" +
                "Mixnet-Mode=Anon-5Hop-Mixnet\n" +
                "Kill-Switch=true\n" +
                "Block-Screen-Locked=true\n" +
                "Block-Bypassed-DNS=true\n" +
                "Never-Proxy-DNS=true\n" +
                "Do-Not-Route-Private-IPs=true\n" +
                "===========\n"
        ));

        list.add(new PresetTemplate(
                "lbox_tls_record_frag.skid",
                "LBox DNS Templates + TLS Record Fragment",
                "LBox Quad9 DoH + Cloudflare DoT templates, Strict Route, gVisor Stack, TLS Record Fragmentation & Mixed-Case SNI",
                "# SKIDWAREDNS CUSTOM CONFIG [.skid] v3.0\n" +
                "# Preset: LBox DNS Templates + TLS Fragmentation Core\n\n" +
                "===========\n" +
                "DNS Servers [https://dns.quad9.net/dns-query, tls://1dot1dot1dot1.cloudflare-dns.com]\n" +
                "VPN Servers []\n" +
                "Addresses [10.213.37.2/24, fd00:534b:4944::2/64]\n" +
                "SOCKS5 [127.0.0.1:10808]\n" +
                "VPN PORTS [443, 8443]\n" +
                "PROXY PORTS [10808, 10809]\n" +
                "DNS PORTS [443, 853, 53]\n" +
                "Custom DNS Server []\n" +
                "Custom VPN Server []\n" +
                "Custom Proxy Server []\n" +
                "Firewall Rules [BLOCK_DOMAIN:doubleclick.net, BLOCK_DOMAIN:ads.tiktok.com]\n" +
                "Allowed DNS Records [Auto]\n" +
                "On-Device Blocklists [Adware & Trackers, Malware & Phishing]\n" +
                "LBox Templates [quad9_doh, cloudflare_dot, safe_dns_dot]\n" +
                "===========\n" +
                "Tor enabled=false\n" +
                "Mode=VPN+DNS+Firewall\n" +
                "Name=LBox-TLS-Record-Frag\n" +
                "Proxy mode=SOCKS5\n" +
                "DNS Mode=LBox-Template\n" +
                "Quad9=Security\n" +
                "DNSCrypt=Quad9 Security\n" +
                "DNS-over-TLS=Cloudflare\n" +
                "DNS-Proxy=Cloudflare\n" +
                "Tunnel=None\n" +
                "Connect-from-mode=Direct\n" +
                "===========\n" +
                "Strict-Route=true\n" +
                "TUN-Stack=gvisor\n" +
                "TLS-Fragment=true\n" +
                "TLS-Record-Fragment=true\n" +
                "TLS-Fallback-Delay-Ms=500\n" +
                "Mixed-Case-SNI=true\n" +
                "Spoof-SNI-Enabled=true\n" +
                "Spoof-SNI-Host=play.googleapis.com\n" +
                "DNS-Booster=true\n" +
                "===========\n"
        ));

        list.add(new PresetTemplate(
                "invizible_full_trimodule.skid",
                "InviZible Pro v3 Tri-Module (DNSCrypt + Tor + Purple I2P)",
                "Full DNSCrypt + AnonRelays + Tor Snowflake/IsolateUID + Purple I2P (.b32.i2p AddressBook) + ARP Spoofing Shield",
                "# SKIDWAREDNS CUSTOM CONFIG [.skid] v3.0\n" +
                "# Preset: InviZible Pro Complete Tri-Module Stack\n\n" +
                "===========\n" +
                "DNS Servers [sdns://AQMAAAAAAAAAETk0LjE0MC4xNC4xNDo1NDQzINErR_JS3PLCu_iZEIbq95zkSV2LFsigxDIuUso_OQhzIjIuZG5zY3J5cHQuZGVmYXVsdC5uczEuYWRndWFyZC5jb20]\n" +
                "VPN Servers []\n" +
                "Addresses [10.213.40.2/24, fd00:534b:4944::40/64]\n" +
                "SOCKS5 [127.0.0.1:9050, 127.0.0.1:4447, 127.0.0.1:10808]\n" +
                "VPN PORTS [9001, 443]\n" +
                "PROXY PORTS [10808, 10809, 9050, 4444, 4447]\n" +
                "DNS PORTS [5354, 5400, 8443]\n" +
                "Custom DNS Server []\n" +
                "Custom VPN Server []\n" +
                "Custom Proxy Server [127.0.0.1:9050]\n" +
                "Firewall Rules [BLOCK_DOMAIN:telemetry.microsoft.com, BLOCK_DOMAIN:graph.facebook.com]\n" +
                "Allowed DNS Records [A, AAAA, CNAME, TXT]\n" +
                "On-Device Blocklists [Adware & Trackers, Malware & Phishing]\n" +
                "DNSCrypt Relays [sdns://gQ8xNDYuNzAuODIuMzo0NDM, sdns://gRE3NS4xMTkuMTQ0LjA6ODQ0Mw]\n" +
                "Forwarding Rules [onion 127.0.0.1:5400]\n" +
                "Cloaking Rules [stats.i2p=127.0.0.1, reg.i2p=127.0.0.1]\n" +
                "Ignored Qtypes [DNSKEY, NS, SOA]\n" +
                "===========\n" +
                "Tor enabled=true\n" +
                "DNSCrypt-Module-Enabled=true\n" +
                "I2P-Enabled=true\n" +
                "Mode=Tor+DNSCrypt+I2P\n" +
                "Name=InviZible-TriModule-v3\n" +
                "Proxy mode=Orbot-Tor\n" +
                "DNS Mode=DNSCrypt\n" +
                "Quad9=Security\n" +
                "DNSCrypt=Adguard\n" +
                "DNS-over-TLS=ControlD\n" +
                "DNS-Proxy=Orbot\n" +
                "Tunnel=None\n" +
                "Connect-from-mode=Direct\n" +
                "===========\n" +
                "DNSCrypt-Listen-Port=5354\n" +
                "DNSCrypt-Require-DNSSEC=true\n" +
                "DNSCrypt-Require-NoLog=true\n" +
                "DNSCrypt-HTTP3=true\n" +
                "DNSCrypt-Block-Unqualified=true\n" +
                "DNSCrypt-Block-Undelegated=true\n" +
                "DNSCrypt-Query-Logging=true\n" +
                "DNSCrypt-Suspicious-Logging=true\n" +
                "I2P-HTTP-Proxy-Enabled=true\n" +
                "I2P-SOCKS-Proxy-Enabled=true\n" +
                "Tor-Isolate-UID=true\n" +
                "Tor-Isolate-Dest-Addr=true\n" +
                "Tor-Snowflake-Rendezvous=AMP Cache\n" +
                "ARP-Spoofing-Detection=true\n" +
                "Prevent-Device-Sleep=true\n" +
                "Enable-Script-Control=true\n" +
                "===========\n"
        ));

        list.add(new PresetTemplate(
                "megaproxy_masque_jumphost.skid",
                "MegaProxy Multi-Hop HTTPS Jump + MASQUE-H3",
                "AndroidMegaProxy HTTPS Jump Host chain, MASQUE HTTP/3 UDP, Chrome-128 JA3 Fingerprint, Knock-Host & UID Ownership",
                "# SKIDWAREDNS CUSTOM CONFIG [.skid] v3.0\n" +
                "# Preset: AndroidMegaProxy Multi-Hop + MASQUE-H3 + UID Ownership\n\n" +
                "===========\n" +
                "DNS Servers [https://cloudflare-dns.com/dns-query, tls://dns.quad9.net]\n" +
                "VPN Servers [exit-masque.megaproxy.example:443]\n" +
                "Addresses [10.213.60.2/24, fd00:534b:4944::60/64]\n" +
                "SOCKS5 [127.0.0.1:10808]\n" +
                "VPN PORTS [443, 8443]\n" +
                "PROXY PORTS [10808, 10809]\n" +
                "DNS PORTS [443, 853]\n" +
                "Custom DNS Server []\n" +
                "Custom VPN Server [exit-masque.megaproxy.example:443]\n" +
                "Custom Proxy Server []\n" +
                "Firewall Rules [BLOCK_DOMAIN:doubleclick.net]\n" +
                "Allowed DNS Records [Auto]\n" +
                "On-Device Blocklists [Adware & Trackers]\n" +
                "LBox Templates [cloudflare_dot, quad9_doh]\n" +
                "===========\n" +
                "Tor enabled=false\n" +
                "Mode=VPN+DNS+Firewall\n" +
                "Name=MegaProxy-MASQUE-Jump\n" +
                "Proxy mode=HTTPS-Jump\n" +
                "DNS Mode=LBox-Template\n" +
                "Quad9=Security\n" +
                "DNSCrypt=Quad9 Security\n" +
                "DNS-over-TLS=Cloudflare\n" +
                "DNS-Proxy=Cloudflare\n" +
                "Tunnel=MASQUE-H3\n" +
                "Connect-from-mode=HTTPS Jump\n" +
                "===========\n" +
                "Tunnel-Host=exit-masque.megaproxy.example\n" +
                "Tunnel-Port=443\n" +
                "Jump-Host-Enabled=true\n" +
                "Jump-Protocol=HTTPS\n" +
                "Jump-Host=jump1-eu.megaproxy.example\n" +
                "Jump-Port=443\n" +
                "Proxy-Knock-Host=cp.cloudflare.com\n" +
                "App-Ownership-Check=Standard\n" +
                "JA3-Fingerprint=Chrome-128\n" +
                "Strict-Route=true\n" +
                "===========\n"
        ));

        list.add(new PresetTemplate(
                "lumine_hysteria2_fakeip.skid",
                "Lumine Fake-IP + Evozi Hysteria2 Brutal",
                "Lumine 198.18.0.0/15 Fake-IP pool + GFWList Smart Split + Evozi Hysteria2 Salamander Obfs (300/80 Mbps)",
                "# SKIDWAREDNS CUSTOM CONFIG [.skid] v3.0\n" +
                "# Preset: Lumine Fake-IP + Evozi Hysteria2 Salamander\n\n" +
                "===========\n" +
                "DNS Servers [tls://p2.freedns.controld.com, https://dns.quad9.net/dns-query]\n" +
                "VPN Servers [hy2-brutal.skid.example:443]\n" +
                "Addresses [10.213.88.2/24, fd00:534b:4944::88/64]\n" +
                "SOCKS5 [127.0.0.1:10808]\n" +
                "VPN PORTS [443, 20000]\n" +
                "PROXY PORTS [10808, 10809]\n" +
                "DNS PORTS [853, 443]\n" +
                "Custom DNS Server []\n" +
                "Custom VPN Server [hy2-brutal.skid.example:443]\n" +
                "Custom Proxy Server []\n" +
                "Firewall Rules [BLOCK_DOMAIN:ads.tiktok.com, BLOCK_DOMAIN:app-measurement.com]\n" +
                "Allowed DNS Records [Auto]\n" +
                "On-Device Blocklists [Adware & Trackers, Malware & Phishing]\n" +
                "===========\n" +
                "Tor enabled=false\n" +
                "Mode=VPN+DNS+Firewall\n" +
                "Name=Lumine-Hysteria2-FakeIP\n" +
                "Proxy mode=SOCKS5\n" +
                "DNS Mode=Fake-IP\n" +
                "Quad9=Security\n" +
                "DNSCrypt=Adguard\n" +
                "DNS-over-TLS=ControlD\n" +
                "DNS-Proxy=Cloudflare\n" +
                "Tunnel=Hysteria2\n" +
                "Connect-from-mode=TLS/SSL (Obfs)\n" +
                "===========\n" +
                "Tunnel-Host=hy2-brutal.skid.example\n" +
                "Tunnel-Port=443\n" +
                "Hysteria-Obfs-Password=skid-salamander-v3\n" +
                "Hysteria-Up-Mbps=80\n" +
                "Hysteria-Down-Mbps=300\n" +
                "Hysteria-Port-Hop-Range=20000-50000\n" +
                "Fake-IP-Enabled=true\n" +
                "Routing-Split-Preset=GFWList-Split\n" +
                "TLS-Record-Fragment=true\n" +
                "Mixed-Case-SNI=true\n" +
                "===========\n"
        ));

        list.add(new PresetTemplate(
                "injector_ssh_http_payload.skid",
                "HTTP Injector SSH + HTTP Proxy + Header Replacer",
                "HTTP Proxy -> SSH (/root-skidware), Custom Payload, UDPGW :7300, Send 16KB / Recv 32KB, Header Replacer 200 OK & Excluded LAN Routes",
                "# SKIDWAREDNS CUSTOM CONFIG [.skid] v4.0\n" +
                "# Preset: HTTP Injector SSH + HTTP Proxy + Custom Payload + Header Replacer\n\n" +
                "===========\n" +
                "DNS Servers [https://cloudflare-dns.com/dns-query, 1.1.1.1:53]\n" +
                "VPN Servers [ssh-edge.skidware.example:443]\n" +
                "Addresses [10.213.22.2/24, fd00:534b:4944::22/64]\n" +
                "SOCKS5 [127.0.0.1:1080]\n" +
                "VPN PORTS [22, 443, 8080]\n" +
                "PROXY PORTS [10808, 10809, 1080]\n" +
                "DNS PORTS [443, 53]\n" +
                "Custom DNS Server []\n" +
                "Custom VPN Server [ssh-edge.skidware.example:443]\n" +
                "Custom Proxy Server [127.0.0.1:10809]\n" +
                "Firewall Rules [BLOCK_DOMAIN:doubleclick.net]\n" +
                "Allowed DNS Records [Auto]\n" +
                "On-Device Blocklists [Adware & Trackers]\n" +
                "LBox Templates [cloudflare_dot]\n" +
                "===========\n" +
                "Tor enabled=false\n" +
                "Mode=VPN+DNS+Firewall\n" +
                "Name=Injector-HTTP-Proxy-SSH\n" +
                "Proxy mode=HTTP\n" +
                "DNS Mode=LBox-Template\n" +
                "Quad9=Security\n" +
                "DNSCrypt=Quad9 Security\n" +
                "DNS-over-TLS=Cloudflare\n" +
                "DNS-Proxy=Cloudflare\n" +
                "Tunnel=SSH\n" +
                "Connect-from-mode=Http proxy\n" +
                "===========\n" +
                "Tunnel-Host=ssh-edge.skidware.example\n" +
                "Tunnel-Port=443\n" +
                "Tunnel-User=/root-skidware\n" +
                "Custom-Payload-Enabled=true\n" +
                "HTTP-Payload=CONNECT [host_port] HTTP/1.1[crlf]Host: [sni][crlf]X-Online-Host: [sni][crlf]Connection: Keep-Alive[crlf][crlf]\n" +
                "SSH-Local-Port=1080\n" +
                "SSH-UDPGW-Port=7300\n" +
                "SSH-Send-Buffer-Bytes=16384\n" +
                "SSH-Recv-Buffer-Bytes=32768\n" +
                "SSH-Header-Replacer=true\n" +
                "SSH-Route-Proxy=false\n" +
                "Use-Default-Route=true\n" +
                "Custom-Routes=0.0.0.0/0\n" +
                "Excluded-Routes=10.0.0.0/8;172.16.0.0/12;192.168.0.0/16\n" +
                "HTTP-Ping-Enabled=true\n" +
                "HTTP-Ping-URL=clients3.google.com/generate_204\n" +
                "===========\n"
        ));

        list.add(new PresetTemplate(
                "injector_xray_vless_reality_xhttp.skid",
                "HTTP Injector Xray VLESS + XHTTP + Reality",
                "Xray Core + VLESS over XHTTP + Reality TLS encryption + IPIfNonMatch Domain Strategy + Block Ads Routing",
                "# SKIDWAREDNS CUSTOM CONFIG [.skid] v4.0\n" +
                "# Preset: HTTP Injector Xray VLESS + XHTTP + Reality\n\n" +
                "===========\n" +
                "DNS Servers [https://dns.quad9.net/dns-query, tls://1dot1dot1dot1.cloudflare-dns.com]\n" +
                "VPN Servers [xray-reality.skidware.example:443]\n" +
                "Addresses [10.213.90.2/24, fd00:534b:4944::90/64]\n" +
                "SOCKS5 [127.0.0.1:10808]\n" +
                "VPN PORTS [443, 8443]\n" +
                "PROXY PORTS [10808, 10809]\n" +
                "DNS PORTS [443, 853]\n" +
                "Custom DNS Server []\n" +
                "Custom VPN Server [xray-reality.skidware.example:443]\n" +
                "Custom Proxy Server []\n" +
                "Firewall Rules [BLOCK_DOMAIN:ads.tiktok.com, BLOCK_DOMAIN:doubleclick.net]\n" +
                "Allowed DNS Records [Auto]\n" +
                "On-Device Blocklists [Adware & Trackers, Malware & Phishing]\n" +
                "LBox Templates [quad9_doh]\n" +
                "===========\n" +
                "Tor enabled=false\n" +
                "Mode=VPN+DNS+Firewall\n" +
                "Name=Injector-Xray-VLESS-Reality\n" +
                "Proxy mode=SOCKS5\n" +
                "DNS Mode=LBox-Template\n" +
                "Quad9=Security\n" +
                "DNSCrypt=Quad9 Security\n" +
                "DNS-over-TLS=Cloudflare\n" +
                "DNS-Proxy=Quad9\n" +
                "Tunnel=V2Ray/Xray\n" +
                "Connect-from-mode=Direct\n" +
                "===========\n" +
                "Tunnel-Host=xray-reality.skidware.example\n" +
                "Tunnel-Port=443\n" +
                "SNI=www.microsoft.com\n" +
                "Xray-Core=Xray\n" +
                "Xray-Protocol=VLESS\n" +
                "Xray-UUID=b831381d-6324-4d53-ad4f-8cda48b30811\n" +
                "Xray-Security=Reality\n" +
                "Xray-Network-Type=XHTTP\n" +
                "Xray-Header-Type=None\n" +
                "Xray-Reality-PubKey=jK8vR2m9xLpQ4wZn7cTyB1nF6hDsG3uAe5oVcXiYz08\n" +
                "Xray-Reality-ShortId=6ba85179e30d4fc2\n" +
                "Xray-Mux-Enabled=false\n" +
                "Xray-Domain-Strategy=IPIfNonMatch\n" +
                "Xray-Routing-Rules=Block Ads (Basic)\n" +
                "===========\n"
        ));

        list.add(new PresetTemplate(
                "injector_hysteria2_quicwin_hop.skid",
                "HTTP Injector Hysteria v2 + 16MB QUIC Window + Port Hop",
                "Hysteria v2 UDP + ALPN h3 + 30s Port Hopping + 16 MB QUIC Receive Window + RFC 8899 Path MTU Control",
                "# SKIDWAREDNS CUSTOM CONFIG [.skid] v4.0\n" +
                "# Preset: HTTP Injector Hysteria v2 + QUIC Receive Window + Port Hopping\n\n" +
                "===========\n" +
                "DNS Servers [https://dns.quad9.net/dns-query]\n" +
                "VPN Servers [hy2-injector.skidware.example:443]\n" +
                "Addresses [10.213.95.2/24, fd00:534b:4944::95/64]\n" +
                "SOCKS5 [127.0.0.1:10808]\n" +
                "VPN PORTS [443, 20000]\n" +
                "PROXY PORTS [10808, 10809]\n" +
                "DNS PORTS [443, 853]\n" +
                "Custom DNS Server []\n" +
                "Custom VPN Server [hy2-injector.skidware.example:443]\n" +
                "Custom Proxy Server []\n" +
                "Firewall Rules [BLOCK_DOMAIN:doubleclick.net]\n" +
                "Allowed DNS Records [Auto]\n" +
                "On-Device Blocklists [Adware & Trackers]\n" +
                "===========\n" +
                "Tor enabled=false\n" +
                "Mode=VPN+DNS+Firewall\n" +
                "Name=Injector-Hysteria2-QUIC16M\n" +
                "Proxy mode=SOCKS5\n" +
                "DNS Mode=Rethink-DoH\n" +
                "Quad9=Security\n" +
                "DNSCrypt=Quad9 Security\n" +
                "DNS-over-TLS=Cloudflare\n" +
                "DNS-Proxy=Quad9\n" +
                "Tunnel=Hysteria2\n" +
                "Connect-from-mode=Direct\n" +
                "===========\n" +
                "Tunnel-Host=hy2-injector.skidware.example\n" +
                "Tunnel-Port=443\n" +
                "Tunnel-Auth=skid-hy2-token-v4\n" +
                "SNI=cdn.cloudflare.com\n" +
                "Hysteria-Version=2\n" +
                "Hysteria-Protocol=UDP\n" +
                "Hysteria-Obfs-Password=salamander-obfs-v4\n" +
                "Hysteria-ALPN=h3\n" +
                "Hysteria-Allow-Insecure=false\n" +
                "Hysteria-Port-Hop-Range=20000-50000\n" +
                "Hysteria-Hop-Interval-Sec=30\n" +
                "Hysteria-Up-Mbps=100\n" +
                "Hysteria-Down-Mbps=400\n" +
                "Hysteria-QUIC-RcvWin-MB=16\n" +
                "Hysteria-Disable-MTU-Discovery=false\n" +
                "===========\n"
        ));

        list.add(new PresetTemplate(
                "injector_ss2022_blake3_doh_quad9.skid",
                "HTTP Injector SS-2022 BLAKE3 + TLS Obfs + Quad9 DoH",
                "Shadowsocks 2022-blake3-aes-256-gcm + Server (Obfs) TLS + Quad9 DoH (14-Provider Selector) + Excluded LAN Routes",
                "# SKIDWAREDNS CUSTOM CONFIG [.skid] v4.1\n" +
                "# Preset: HTTP Injector Shadowsocks-2022 BLAKE3 + TLS Obfs + Quad9 DoH\n\n" +
                "===========\n" +
                "DNS Servers [https://dns.quad9.net/dns-query]\n" +
                "VPN Servers [ss2022-edge.skidware.example:8388]\n" +
                "Addresses [10.213.99.2/24, fd00:534b:4944::99/64]\n" +
                "SOCKS5 [127.0.0.1:10808]\n" +
                "VPN PORTS [8388, 443]\n" +
                "PROXY PORTS [10808, 10809]\n" +
                "DNS PORTS [443, 853]\n" +
                "Custom DNS Server []\n" +
                "Custom VPN Server [ss2022-edge.skidware.example:8388]\n" +
                "Custom Proxy Server []\n" +
                "Firewall Rules [BLOCK_DOMAIN:doubleclick.net, BLOCK_DOMAIN:ads.tiktok.com]\n" +
                "Allowed DNS Records [Auto]\n" +
                "On-Device Blocklists [Adware & Trackers, Malware & Phishing]\n" +
                "===========\n" +
                "Tor enabled=false\n" +
                "Mode=VPN+DNS+Firewall\n" +
                "Name=Injector-SS2022-BLAKE3-Quad9\n" +
                "Proxy mode=SOCKS5\n" +
                "DNS Mode=Injector-DoH\n" +
                "Quad9=Security\n" +
                "DNSCrypt=Quad9 Security\n" +
                "DNS-over-TLS=Cloudflare\n" +
                "DNS-Proxy=Quad9\n" +
                "Tunnel=Shadowsocks\n" +
                "Connect-from-mode=TLS/SSL (Obfs)\n" +
                "===========\n" +
                "Tunnel-Host=ss2022-edge.skidware.example\n" +
                "Tunnel-Port=8388\n" +
                "Tunnel-Auth=q8wL2m9xLpQ4wZn7cTyB1nF6hDsG3uAe5oVcXiYz08M=\n" +
                "Tunnel-Cipher=2022-blake3-aes-256-gcm\n" +
                "SS-Obfs-Server=obfs=tls;obfs-host=www.microsoft.com\n" +
                "SNI=www.microsoft.com\n" +
                "Injector-DoH-Provider=Quad9\n" +
                "Use-Default-Route=true\n" +
                "Custom-Routes=0.0.0.0/0\n" +
                "Excluded-Routes=10.0.0.0/8;172.16.0.0/12;192.168.0.0/16\n" +
                "HTTP-Ping-Enabled=true\n" +
                "HTTP-Ping-URL=clients3.google.com/generate_204\n" +
                "===========\n"
        ));

        list.add(new PresetTemplate(
                "nymvpn_anonymous_mixnet_690ms.skid",
                "NymVPN Anonymous 5-Hop Mixnet (1 Mbps / 690ms) + AmneziaWG",
                "Anonymous Profile, 1 Mbps Continuous Cover, Low Loop Cover, 15ms Packet Mixing (At least 690 ms) & Client-Side AmneziaWG Decoys",
                "# SKIDWAREDNS CUSTOM CONFIG [.skid] v5.0\n" +
                "# Preset: NymVPN Anonymous 5-Hop Sphinx Mixnet + Mixnet Tuning + AmneziaWG\n\n" +
                "===========\n" +
                "DNS Servers [https://dns.quad9.net/dns-query, tls://1dot1dot1dot1.cloudflare-dns.com]\n" +
                "VPN Servers [ch-zurich-01.nym-gw.skid:51820, nl-amsterdam-02.nym-gw.skid:443]\n" +
                "Addresses [10.213.55.2/24, fd00:534b:4944::55/64]\n" +
                "SOCKS5 [127.0.0.1:10808]\n" +
                "VPN PORTS [51820, 443]\n" +
                "PROXY PORTS [10808, 10809]\n" +
                "DNS PORTS [443, 853]\n" +
                "Custom DNS Server []\n" +
                "Custom VPN Server [ch-zurich-01.nym-gw.skid:51820]\n" +
                "Custom Proxy Server []\n" +
                "Firewall Rules [BLOCK_DOMAIN:doubleclick.net, BLOCK_DOMAIN:graph.facebook.com]\n" +
                "Allowed DNS Records [Auto]\n" +
                "On-Device Blocklists [Adware & Trackers, Malware & Phishing]\n" +
                "===========\n" +
                "Tor enabled=false\n" +
                "Mode=Mixnet-5Hop\n" +
                "Name=NymVPN-Anonymous-5Hop-690ms\n" +
                "Proxy mode=SOCKS5\n" +
                "DNS Mode=Injector-DoH\n" +
                "Quad9=Security\n" +
                "DNSCrypt=Quad9 Security\n" +
                "DNS-over-TLS=Cloudflare\n" +
                "DNS-Proxy=Quad9\n" +
                "Tunnel=WireGuard\n" +
                "Connect-from-mode=Direct\n" +
                "===========\n" +
                "WG-Enabled=true\n" +
                "WG-Endpoint=ch-zurich-01.nym-gw.skid:51820\n" +
                "Injector-DoH-Provider=Quad9\n" +
                "Nym-Profile=Anonymous\n" +
                "Nym-User-Country=US\n" +
                "Mixnet-Continuous-Traffic=true\n" +
                "Mixnet-Continuous-Rate=1 Mbps\n" +
                "Mixnet-Cover-Traffic=true\n" +
                "Mixnet-Cover-Rate=Low\n" +
                "Mixnet-Packet-Mixing-Ms=15\n" +
                "Nym-QUIC-Enhanced=false\n" +
                "AmneziaWG-Minimal-Obfs=true\n" +
                "AmneziaWG-Jc=4\n" +
                "AmneziaWG-Jmin=40\n" +
                "AmneziaWG-Jmax=70\n" +
                "Nym-Stealth-API-Connect=false\n" +
                "Nym-Stealth-Cover-Domains=cloudflare.com,cdn.jsdelivr.net,ajax.aspnetcdn.com\n" +
                "===========\n"
        ));

        list.add(new PresetTemplate(
                "nymvpn_anticensorship_quic_stealth.skid",
                "NymVPN Auto + QUIC (HTTP/3) + AmneziaWG + Stealth API",
                "Auto Profile (Never Own Country) + Enhanced Connection (QUIC Entry) + AmneziaWG Decoys + Always-On Stealth API Cover Domains",
                "# SKIDWAREDNS CUSTOM CONFIG [.skid] v5.0\n" +
                "# Preset: NymVPN Anti-Censorship QUIC + AmneziaWG + Stealth API Connect\n\n" +
                "===========\n" +
                "DNS Servers [https://dns.quad9.net/dns-query]\n" +
                "VPN Servers [ca-vancouver-01.nym-gw.skid:443, ch-zurich-01.nym-gw.skid:51820]\n" +
                "Addresses [10.213.56.2/24, fd00:534b:4944::56/64]\n" +
                "SOCKS5 [127.0.0.1:10808]\n" +
                "VPN PORTS [443, 51820]\n" +
                "PROXY PORTS [10808, 10809]\n" +
                "DNS PORTS [443, 853]\n" +
                "Custom DNS Server []\n" +
                "Custom VPN Server [ca-vancouver-01.nym-gw.skid:443]\n" +
                "Custom Proxy Server []\n" +
                "Firewall Rules [BLOCK_DOMAIN:doubleclick.net]\n" +
                "Allowed DNS Records [Auto]\n" +
                "On-Device Blocklists [Adware & Trackers, Malware & Phishing]\n" +
                "===========\n" +
                "Tor enabled=false\n" +
                "Mode=VPN+DNS+Firewall\n" +
                "Name=NymVPN-QUIC-AmneziaWG-Stealth\n" +
                "Proxy mode=MASQUE-H3\n" +
                "DNS Mode=Injector-DoH\n" +
                "Quad9=Security\n" +
                "DNSCrypt=Quad9 Security\n" +
                "DNS-over-TLS=Cloudflare\n" +
                "DNS-Proxy=Quad9\n" +
                "Tunnel=WireGuard\n" +
                "Connect-from-mode=Direct\n" +
                "===========\n" +
                "WG-Enabled=true\n" +
                "WG-Endpoint=ca-vancouver-01.nym-gw.skid:443\n" +
                "Injector-DoH-Provider=Quad9\n" +
                "Nym-Profile=Auto\n" +
                "Nym-User-Country=US\n" +
                "Mixnet-Continuous-Traffic=true\n" +
                "Mixnet-Continuous-Rate=2 Mbps\n" +
                "Mixnet-Cover-Traffic=true\n" +
                "Mixnet-Cover-Rate=Balanced (5x)\n" +
                "Mixnet-Packet-Mixing-Ms=15\n" +
                "Nym-QUIC-Enhanced=true\n" +
                "AmneziaWG-Minimal-Obfs=true\n" +
                "AmneziaWG-Jc=6\n" +
                "AmneziaWG-Jmin=50\n" +
                "AmneziaWG-Jmax=90\n" +
                "Nym-Stealth-API-Connect=true\n" +
                "Nym-Stealth-Cover-Domains=cloudflare.com,cdn.jsdelivr.net,ajax.aspnetcdn.com\n" +
                "===========\n"
        ));

        list.add(new PresetTemplate(
                "mullvad_tallinn_100g_daita.skid",
                "Mullvad Estonia ee-tll-wg-101 (100 Gbps • Owned • DAITA)",
                "Tallinn ee-tll-wg-101 (213.232.112.3 • 100 Gbps • DAITA: Yes • SOCKS5 :1080 • Mullvad DoH)",
                "# SKIDWAREDNS CUSTOM CONFIG [.skid] v5.1\n" +
                "# Preset: Mullvad Tallinn Estonia ee-tll-wg-101 (xtom / 100 Gbps / Owned by Mullvad / DAITA)\n\n" +
                "===========\n" +
                "DNS Servers [https://dns.mullvad.net/dns-query, 100.64.0.1:53]\n" +
                "VPN Servers [213.232.112.3:51820, ee-tll-wg-101.relays.mullvad.net:51820]\n" +
                "Addresses [10.64.0.2/32, fc00:bbbb:bbbb:bb01::2/128]\n" +
                "SOCKS5 [ee-tll-wg-socks5-101.relays.mullvad.net:1080, 127.0.0.1:10808]\n" +
                "VPN PORTS [51820, 3503, 443]\n" +
                "PROXY PORTS [1080, 10808, 10809]\n" +
                "DNS PORTS [443, 53]\n" +
                "Custom DNS Server []\n" +
                "Custom VPN Server [213.232.112.3:51820]\n" +
                "Custom Proxy Server [ee-tll-wg-socks5-101.relays.mullvad.net:1080]\n" +
                "Firewall Rules [BLOCK_DOMAIN:doubleclick.net, BLOCK_DOMAIN:ads.tiktok.com]\n" +
                "Allowed DNS Records [Auto]\n" +
                "On-Device Blocklists [Adware & Trackers, Malware & Phishing]\n" +
                "===========\n" +
                "Tor enabled=false\n" +
                "Mode=VPN+DNS+Firewall\n" +
                "Name=Mullvad-Tallinn-100G-DAITA\n" +
                "Proxy mode=SOCKS5\n" +
                "DNS Mode=Injector-DoH\n" +
                "Quad9=Security\n" +
                "DNSCrypt=Quad9 Security\n" +
                "DNS-over-TLS=Cloudflare\n" +
                "DNS-Proxy=Quad9\n" +
                "Tunnel=WireGuard\n" +
                "Connect-from-mode=Direct\n" +
                "===========\n" +
                "WG-Profile-Name=mullvad-ee-tll-wg-101\n" +
                "WG-Enabled=true\n" +
                "WG-Endpoint=213.232.112.3:51820\n" +
                "WG-Public-Key=Abxw9kZlH8Stt7mWJ2rKeCQjVVdJpLvV6czby23FzmA=\n" +
                "Injector-DoH-Provider=Mullvad\n" +
                "Mullvad-Enabled=true\n" +
                "Mullvad-Multihop=false\n" +
                "Mullvad-Exit-Relay=ee-tll-wg-101\n" +
                "Mullvad-DAITA=true\n" +
                "Mullvad-Use-Socks5=true\n" +
                "Mullvad-Owned-Only=true\n" +
                "===========\n"
        ));

        list.add(new PresetTemplate(
                "mullvad_zurich_to_tallinn_multihop.skid",
                "Mullvad WireGuard Multihop: Zurich ch-zrh-wg-001 → Tallinn ee-tll-wg-102",
                "Entry ch-zrh-wg-001 (193.32.127.71) → Exit ee-tll-wg-102 (Multihop Port 3514 • 100 Gbps • DAITA: Yes)",
                "# SKIDWAREDNS CUSTOM CONFIG [.skid] v5.1\n" +
                "# Preset: Mullvad True WireGuard Multihop (Zurich Entry -> Tallinn 100G DAITA Exit :3514)\n\n" +
                "===========\n" +
                "DNS Servers [https://dns.mullvad.net/dns-query, 100.64.0.1:53]\n" +
                "VPN Servers [193.32.127.71:3514, ee-tll-wg-102.relays.mullvad.net:51820]\n" +
                "Addresses [10.64.0.2/32, fc00:bbbb:bbbb:bb01::2/128]\n" +
                "SOCKS5 [ee-tll-wg-socks5-102.relays.mullvad.net:1080, 127.0.0.1:10808]\n" +
                "VPN PORTS [3514, 51820]\n" +
                "PROXY PORTS [1080, 10808, 10809]\n" +
                "DNS PORTS [443, 53]\n" +
                "Custom DNS Server []\n" +
                "Custom VPN Server [193.32.127.71:3514]\n" +
                "Custom Proxy Server [ee-tll-wg-socks5-102.relays.mullvad.net:1080]\n" +
                "Firewall Rules [BLOCK_DOMAIN:doubleclick.net]\n" +
                "Allowed DNS Records [Auto]\n" +
                "On-Device Blocklists [Adware & Trackers, Malware & Phishing]\n" +
                "===========\n" +
                "Tor enabled=false\n" +
                "Mode=VPN+DNS+Firewall\n" +
                "Name=Mullvad-Multihop-ZRH-to-TLL\n" +
                "Proxy mode=SOCKS5\n" +
                "DNS Mode=Injector-DoH\n" +
                "Quad9=Security\n" +
                "DNSCrypt=Quad9 Security\n" +
                "DNS-over-TLS=Cloudflare\n" +
                "DNS-Proxy=Quad9\n" +
                "Tunnel=WireGuard\n" +
                "Connect-from-mode=Direct\n" +
                "===========\n" +
                "WG-Profile-Name=mullvad-ch-zrh-wg-001-to-ee-tll-wg-102\n" +
                "WG-Enabled=true\n" +
                "WG-Endpoint=193.32.127.71:3514\n" +
                "WG-Public-Key=TQi/1PpM98XRNEAYkyRR0lspDLg/SAarUkMclPDk52w=\n" +
                "Injector-DoH-Provider=Mullvad\n" +
                "Mullvad-Enabled=true\n" +
                "Mullvad-Multihop=true\n" +
                "Mullvad-Entry-Relay=ch-zrh-wg-001\n" +
                "Mullvad-Exit-Relay=ee-tll-wg-102\n" +
                "Mullvad-DAITA=true\n" +
                "Mullvad-Use-Socks5=true\n" +
                "Mullvad-Owned-Only=true\n" +
                "===========\n"
        ));

        list.add(new PresetTemplate(
                "mullvad_zurich_bridge_ssh_v2ray.skid",
                "Mullvad Bridge ch-zrh-br-001 (SSH SHA256 Pin / V2Ray) → ch-zrh-wg-003",
                "Zurich Bridge 193.32.127.117 (SSH SHA256:IE/308Y8...) / 193.32.127.118 (V2Ray) → Zurich WG ch-zrh-wg-003",
                "# SKIDWAREDNS CUSTOM CONFIG [.skid] v5.1\n" +
                "# Preset: Mullvad Bridge ch-zrh-br-001 + WireGuard Exit ch-zrh-wg-003\n\n" +
                "===========\n" +
                "DNS Servers [https://dns.mullvad.net/dns-query, 100.64.0.1:53]\n" +
                "VPN Servers [193.32.127.117:22, 193.32.127.72:3377]\n" +
                "Addresses [10.64.0.2/32, fc00:bbbb:bbbb:bb01::2/128]\n" +
                "SOCKS5 [ch-zrh-wg-socks5-003.relays.mullvad.net:1080, 127.0.0.1:10808]\n" +
                "VPN PORTS [22, 443, 3377, 51820]\n" +
                "PROXY PORTS [1080, 10808, 10809]\n" +
                "DNS PORTS [443, 53]\n" +
                "Custom DNS Server []\n" +
                "Custom VPN Server [193.32.127.72:51820]\n" +
                "Custom Proxy Server [193.32.127.117:22]\n" +
                "Firewall Rules [BLOCK_DOMAIN:doubleclick.net]\n" +
                "Allowed DNS Records [Auto]\n" +
                "On-Device Blocklists [Adware & Trackers, Malware & Phishing]\n" +
                "===========\n" +
                "Tor enabled=false\n" +
                "Mode=VPN+DNS+Firewall\n" +
                "Name=Mullvad-Bridge-ZRH-BR001\n" +
                "Proxy mode=SSH-Jump\n" +
                "DNS Mode=Injector-DoH\n" +
                "Quad9=Security\n" +
                "DNSCrypt=Quad9 Security\n" +
                "DNS-over-TLS=Cloudflare\n" +
                "DNS-Proxy=Quad9\n" +
                "Tunnel=WireGuard\n" +
                "Connect-from-mode=SSH Jump\n" +
                "===========\n" +
                "WG-Profile-Name=mullvad-ch-zrh-br-001-to-ch-zrh-wg-003\n" +
                "WG-Enabled=true\n" +
                "WG-Endpoint=193.32.127.72:51820\n" +
                "WG-Public-Key=WHsy9kByfDzRtM1V/uDR2BFNUXqouU61vHdHnrgBMzI=\n" +
                "Jump-Host-Enabled=true\n" +
                "Jump-Protocol=SSH\n" +
                "Jump-Host=193.32.127.117\n" +
                "Jump-Port=22\n" +
                "Injector-DoH-Provider=Mullvad\n" +
                "Mullvad-Enabled=true\n" +
                "Mullvad-Multihop=true\n" +
                "Mullvad-Entry-Relay=ch-zrh-br-001\n" +
                "Mullvad-Exit-Relay=ch-zrh-wg-003\n" +
                "Mullvad-Bridge-Relay=ch-zrh-br-001\n" +
                "Mullvad-SSH-SHA256=SHA256:IE/308Y8M8dOs+e2IP7sl1CtY6TSwt6uD5ygHTU/32w\n" +
                "Mullvad-DAITA=false\n" +
                "===========\n"
        ));

        BUILTIN_PRESETS = Collections.unmodifiableList(list);
    }

    private SkidConfigStore() {}

    public static synchronized SkidConfig getActive(Context ctx) {
        if (activeCache != null) {
            return activeCache;
        }
        SharedPreferences sp = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String raw = sp.getString(KEY_ACTIVE_SKID, null);
        if (raw == null || raw.trim().isEmpty()) {
            raw = BUILTIN_PRESETS.get(0).skidContent;
            sp.edit().putString(KEY_ACTIVE_SKID, raw).apply();
        }
        activeCache = SkidConfigParser.parse(raw).config;
        return activeCache;
    }

    public static synchronized String getActiveRaw(Context ctx) {
        SkidConfig cfg = getActive(ctx);
        return SkidConfigParser.serialize(cfg);
    }

    public static synchronized void saveActive(Context ctx, SkidConfig config) {
        activeCache = config;
        String serialized = SkidConfigParser.serialize(config);
        SharedPreferences sp = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_ACTIVE_SKID, serialized).apply();
        saveNamedProfileFile(ctx, sanitizeFilename(config.name) + ".skid", serialized);
    }

    public static synchronized SkidConfigParser.ParseResult saveActiveFromRaw(Context ctx, String rawText) {
        SkidConfigParser.ParseResult res = SkidConfigStore.parseRawInternal(rawText);
        activeCache = res.config;
        String canonical = SkidConfigParser.serialize(res.config);
        SharedPreferences sp = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_ACTIVE_SKID, canonical).apply();
        saveNamedProfileFile(ctx, sanitizeFilename(res.config.name) + ".skid", canonical);
        return res;
    }

    private static SkidConfigParser.ParseResult parseRawInternal(String rawText) {
        return SkidConfigParser.parse(rawText);
    }

    public static File getProfilesDir(Context ctx) {
        File dir = new File(ctx.getFilesDir(), "skid_profiles");
        if (!dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }
        return dir;
    }

    public static void saveNamedProfileFile(Context ctx, String fileName, String content) {
        try {
            if (!fileName.endsWith(".skid")) {
                fileName = fileName + ".skid";
            }
            File out = new File(getProfilesDir(ctx), fileName);
            try (FileOutputStream fos = new FileOutputStream(out)) {
                fos.write(content.getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception ignored) {
        }
    }

    public static List<File> listSavedProfileFiles(Context ctx) {
        File[] files = getProfilesDir(ctx).listFiles();
        List<File> out = new ArrayList<>();
        if (files != null) {
            for (File f : files) {
                if (f.isFile() && f.getName().endsWith(".skid")) {
                    out.add(f);
                }
            }
        }
        return out;
    }

    public static String readFileUtf8(File f) {
        try (FileInputStream fis = new FileInputStream(f)) {
            byte[] buf = new byte[(int) f.length()];
            int read = fis.read(buf);
            if (read <= 0) return "";
            return new String(buf, 0, read, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    public static String sanitizeFilename(String input) {
        if (input == null || input.trim().isEmpty()) return "custom_profile";
        String cleaned = input.trim().replaceAll("[^a-zA-Z0-9._-]", "_");
        return cleaned.isEmpty() ? "custom_profile" : cleaned;
    }
}
