package com.skidwaredns.app.config;

import com.skidwaredns.app.model.DnsPresets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * SkidwareDNS v3.0 ("MegaForge Edition") [.skid] configuration model combining:
 *  - RethinkDNS + LBox DNS Templates & Core Stack / TLS Record Fragmentation / Mixed-Case SNI
 *  - InviZible Pro Tri-Module (DNSCrypt + Remote v3 Resolvers/Relays, Tor + Snowflake/Isolation, Purple I2P + .b32.i2p)
 *  - AndroidMegaProxy Multi-Hop HTTPS/SSH Jump Hosts, MASQUE HTTP/3, Knock-Host, App Ownership Checks & JA3
 *  - SniShaper Lumine Fake-IP (198.18.0.0/15), Intelligent Rule Splitting & Subscriptions
 *  - Hysteria 1/2 + SlowUDP, WireGuard skidwareproxy, MITM ARP Spoofing Detection & Shell Script Broadcast Control
 */
public final class SkidConfig {

    // === Section 1: Bracketed Array Blocks [] ===
    public final List<String> dnsServers = new ArrayList<>();
    public final List<String> vpnServers = new ArrayList<>();
    public final List<String> addresses = new ArrayList<>();
    public final List<String> socks5 = new ArrayList<>();
    public final List<String> vpnPorts = new ArrayList<>();
    public final List<String> proxyPorts = new ArrayList<>();
    public final List<String> dnsPorts = new ArrayList<>();
    public final List<String> customDnsServer = new ArrayList<>();
    public final List<String> customVpnServer = new ArrayList<>();
    public final List<String> customProxyServer = new ArrayList<>();

    public final List<String> firewallRules = new ArrayList<>();
    public final List<String> blockedApps = new ArrayList<>();
    public final List<String> bypassApps = new ArrayList<>();
    public final List<String> torifyApps = new ArrayList<>();
    public final List<String> allowedDnsRecords = new ArrayList<>();
    public final List<String> activeBlocklistPacks = new ArrayList<>();

    // v3.0 Extended Bracket Arrays
    public final List<String> lboxTemplates = new ArrayList<>();
    public final List<String> dnscryptRelays = new ArrayList<>();
    public final List<String> cloakingRules = new ArrayList<>();
    public final List<String> forwardingRules = new ArrayList<>();
    public final List<String> whitelistDomains = new ArrayList<>();
    public final List<String> ipBlacklist = new ArrayList<>();
    public final List<String> ignoredQtypes = new ArrayList<>();
    public final List<String> subscriptionUrls = new ArrayList<>();

    // === Section 2: Core Key=Value Directives ===
    public boolean torEnabled = false;
    public boolean dnscryptModuleEnabled = true;
    public boolean i2pEnabled = false;
    public String mode = "DNS+Firewall";
    public String name = "RDNS-Stealth-Core";
    public String proxyMode = "SOCKS5";
    public String dnsMode = "Rethink-DoH";
    public String globalDnsCategory = "Rethink DNS";
    public String quad9 = "Security";
    public String dnsCrypt = "Quad9 Security";
    public String dnsOverTls = "Cloudflare";
    public String dnsProxy = "Quad9";
    public String tunnel = "Hysteria2";
    public String connectFromMode = "TLS/SSL (Obfs)";

    // === Section 3A: Tunnel, Hysteria 1/2, SlowUDP, Jump Hosts, WireGuard & Connect-From ===
    public String rethinkDoh = "https://sky.rethinkdns.com/rec";
    public String tunnelHost = "";
    public int tunnelPort = 443;
    public String tunnelUser = "skid";
    public String tunnelAuth = "";
    public String tunnelCipher = "chacha20-ietf-poly1305";
    public String v2rayTransport = "ws";
    public String sni = "cloudflare.com";
    public String bugHost = "";
    public String httpPayload = "GET / HTTP/1.1[crlf]Host: [sni][crlf]Upgrade: websocket[crlf]Connection: Upgrade[crlf]User-Agent: SkidwareDNS/3.0[crlf][crlf]";
    public String dnsttDomain = "ns.skidtunnel.example";
    public String dnsttPubKey = "7e3f9a10c4b82e1d5f60718293a4b5c6d7e8f90123456789abcdef0123456789";
    public String mixnetMode = "Fast-2Hop-WG";
    public int mtu = 1400;

    // Hysteria 1/2 & SlowUDP (evozi/hysteria-install + HTTP Injector)
    public int hysteriaVersion = 2; // 1 or 2
    public String hysteriaProtocol = "UDP"; // UDP, wechat-video, faketcp
    public int hysteriaUpMbps = 50;
    public int hysteriaDownMbps = 200;
    public String hysteriaObfsPassword = "";
    public String hysteriaPortHopRange = "20000-50000";
    public int hysteriaHopIntervalSec = 30;
    public String hysteriaAlpn = "h3";
    public boolean hysteriaAllowInsecure = false;
    public String hysteriaCustomCert = "";
    public int hysteriaQuicRcvWinMb = 8; // 1..64 MB (default 8 MB per HTTP Injector)
    public boolean hysteriaDisableMtuDiscovery = false; // RFC 8899

    // HTTP Injector SSH Settings & Custom Payload
    public boolean customPayloadEnabled = false;
    public boolean sshUsePrivateKey = false;
    public String sshPrivateKey = "";
    public int sshLocalPort = 1080;
    public int sshUdpgwPort = 7300;
    public int sshSendBufferBytes = 16384;
    public int sshRecvBufferBytes = 32768;
    public boolean sshHeaderReplacer = false; // Replace header status to 200 OK
    public boolean sshRouteProxy = false;     // Route remote proxy via local IP

    // HTTP Injector V2Ray / Xray Full Configurator
    public String xrayCore = "Xray";
    public String xrayProtocol = "VMess"; // VMess, VLESS, Trojan, Shadowsocks, Socks, Wireguard
    public String xrayUuid = "b831381d-6324-4d53-ad4f-8cda48b30811";
    public int xrayAlterId = 0;
    public String xrayServerCipher = "None"; // None, Auto, AES-128-GCM, ChaCha20-Poly1305, Zero (Server -> Security in 184448)
    public String xraySecurity = "None"; // None, TLS, Reality (Transport Layer Encryption -> Security in 184618)
    public String xrayNetworkType = "TCP"; // TCP, mKCP, WebSocket, HTTP Upgrade, HTTP/2, XHTTP, QUIC, gRPC
    public String xrayHeaderType = "None"; // None, HTTP
    public String xrayRealityPubKey = "";
    public String xrayRealityShortId = "";
    public boolean xrayMuxEnabled = false;
    public int xrayMuxConcurrency = 8;
    public String xrayDomainStrategy = "AsIs (Default)"; // AsIs (Default), IPIfNonMatch, IPOnDemand
    public String xrayRoutingRules = "Proxy All"; // Proxy All, Direct China (CN), Direct Iran (IR), Direct Russia (RU), Direct Saudi Arabia (SA), Block Ads (Basic)

    // HTTP Injector Shadowsocks & Hysteria Certificates (184728, 184826, 184834)
    public String ssObfsServer = "";
    public String hysteriaCertificates = "-";

    // HTTP Injector Global Connection, Routes, Ping, DoH, UI & Notification Settings (184356, 184409, 184849)
    public boolean appsFilterEnabled = false;
    public String vpnDnsPreset = "Cloudflare DNS";
    public boolean dnsForwarding = true;
    public boolean useDefaultRoute = true;
    public String customRoutes = "0.0.0.0/0";
    public String excludedRoutes = "10.0.0.0/8;172.16.0.0/12;192.168.0.0/16";
    public boolean httpPingEnabled = false;
    public String httpPingUrl = "clients3.google.com/generate_204";
    public String injectorDohProvider = "Quad9";
    public String darkModeTheme = "System";
    public boolean darkModeSystem = true;
    public String uiLanguage = "Default";
    public boolean showNotificationIcon = true;
    public boolean vibrateOnConnect = true;

    // NymVPN v5.0 Profiles, Mixnet Tuning & Anti-Censorship (190916, 190903, 190908, 185659)
    public String nymProfile = "Anonymous"; // Auto, Anonymous, Fastest, Random
    public String nymUserCountry = "US";
    public boolean mixnetContinuousTraffic = true;
    public String mixnetContinuousRate = "1 Mbps"; // 2 Mbps, 1 Mbps, 0.7 Mbps
    public boolean mixnetCoverTrafficEnabled = true;
    public String mixnetCoverTrafficRate = "Low"; // Low, Balanced (5x), Medium (10x), High (20x)
    public int mixnetPacketMixingDelayMs = 15; // 0..200 ms (Default 15 ms)
    public boolean nymQuicEnhanced = false;
    public boolean amneziaWgMinimalObfs = true;
    public int amneziaWgJc = 4;
    public int amneziaWgJmin = 40;
    public int amneziaWgJmax = 70;
    public boolean nymStealthApiConnect = false;
    public String nymStealthCoverDomains = "cloudflare.com,cdn.jsdelivr.net,ajax.aspnetcdn.com";

    // Mullvad v5.1 Relays, Multihop, DAITA, SOCKS5 & SSH/V2Ray Bridge (215141–215713)
    public boolean mullvadEnabled = false;
    public boolean mullvadMultihop = false;
    public String mullvadEntryRelay = "ch-zrh-wg-001";
    public String mullvadExitRelay = "ee-tll-wg-101";
    public boolean mullvadDaita = true;
    public boolean mullvadUseSocks5 = false;
    public boolean mullvadOwnedOnly = true;
    public String mullvadBridgeRelay = "ch-zrh-br-001";
    public String mullvadSshSha256Pin = "SHA256:IE/308Y8M8dOs+e2IP7sl1CtY6TSwt6uD5ygHTU/32w";

    // AndroidMegaProxy Jump Host & Auth / Ownership / JA3
    public boolean jumpHostEnabled = false;
    public String jumpProtocol = "HTTPS"; // HTTPS, SSH, SOCKS5
    public String jumpHost = "";
    public int jumpPort = 443;
    public String jumpUser = "";
    public String jumpAuth = "";
    public String proxyKnockHost = "example.com";
    public String ja3Fingerprint = "Chrome-128";
    public String appOwnershipCheck = "Disabled"; // Disabled, Standard, Advanced

    // Lumine Fake-IP & Intelligent Rule Splitting
    public boolean fakeIpEnabled = false;
    public String routingSplitPreset = "Global"; // Global, Bypass-LAN, GFWList-Split, Proxy-Lockdown

    // WireGuard & Proxy Advanced
    public String wgProfileName = "skidwareproxy";
    public String wgEndpoint = "engage.cloudflareclient.com:2408";
    public String wgPublicKey = "";
    public boolean wgEnabled = false;
    public boolean socks5ForwardEnabled = false;
    public boolean httpConnectForwardEnabled = false;
    public boolean loopbackProxyForwarderApps = false;
    public boolean fixedWireGuardListenPort = false;
    public boolean wireGuardP2p = true;
    public boolean proxyLockdown = false;
    public boolean floodWireGuard = true;
    public boolean smartPersistentKeepalive = true;

    // === Section 3B: LBox Stack & TCP / IP & UDP Advanced Stack Settings ===
    public boolean strictRoute = false;
    public String tunStack = "system"; // system, gvisor, mixed
    public boolean shorterTcpKeepalive = true;
    public boolean tcpNoDelay = true;
    public boolean tcpFastOpen = true;
    public boolean tcpRstQuickClose = false;
    public int tcpMssClamp = 1360;
    public int tcpSynRetries = 3;
    public String dscpTrafficClass = "LowDelay (0x10)";
    public boolean endpointIndependentMapping = true;
    public int idleTimeoutSec = 0;
    public boolean bandwidthBooster = false;
    public int socketBufferSizeKb = 512;
    public String ipVersion = "IPv4";
    public boolean performProtocolTranslations = false;
    public String customTunGatewayV4 = "10.213.37.1";
    public String customTunGatewayV6 = "fd00:534b:4944::1";

    public String udpQuicPolicy = "Allow";
    public int udpNatTimeoutSec = 60;
    public boolean udpRandomizeSourcePort = true;
    public boolean udpStrictChecksum = true;
    public int udpEdnsPayloadBytes = 1232;

    // === Section 3B2: v5.4 Socket / HTTPS / Port handlers ===
    public int tcpKeepaliveIdleSec = 45;
    public int tcpKeepaliveIntervalSec = 15;
    public boolean tcpReuseAddress = true;
    public boolean tcpCork = false;
    public int tcpUserTimeoutMs = 0;
    public String tcpCongestion = "Cubic";
    public boolean tcpWindowScaling = true;
    public int tcpConnectTimeoutMs = 8000;

    public int udpRecvTimeoutMs = 3000;
    public boolean udpBroadcast = false;
    public boolean udpConnectedMode = true;
    public int udpMaxDatagramBytes = 1400;
    public boolean udpReuseAddress = true;
    public boolean udpGsoHint = false;

    public String httpsMinTls = "TLS 1.2";
    public String httpsAlpn = "h2,http/1.1";
    public boolean httpsSessionTickets = true;
    public boolean httpsHttp2 = true;
    public int httpsTimeoutMs = 10000;
    public boolean httpsEndpointId = true;
    public boolean httpsSniEnabled = true;
    public String httpsPinSha256 = "";
    public boolean httpsFallbackHttp11 = true;

    public String portAllowList = "";
    public String portBlockList = "135,137,139,445,1900,5353";
    public String portForwardMap = "";
    public String portKnockSequence = "";
    public int ephemeralPortMin = 20000;
    public int ephemeralPortMax = 60000;
    public boolean soReusePort = true;
    public boolean bindLoopbackOnly = true;
    public boolean privilegedPortProtect = true;
    public boolean portHopEnabled = false;
    public String portHopRange = "10000-60000";
    public boolean remapDns53toDoH = false;
    public int httpsListenPort = 10443;
    public boolean allowWellKnownPorts = true;

    // v5.5 Open Port menu (1–10000 on device IP)
    public final List<String> openListenPorts = new ArrayList<>();
    public String openPortBindMode = "Device-IP";
    public boolean openPortBanner = true;
    public boolean openPortHold = false;

    // v5.7 Tools hub (FTPS, LAN scan, SSH, knock, port-check, mirror)
    public int toolsFtpsPort = 2121;
    public String toolsFtpsUser = "skid";
    public String toolsFtpsPass = "skid";
    public String toolsScanPorts = "22,80,443,445,3389,8080,8443";
    public String toolsCheckHost = "127.0.0.1";
    public int toolsCheckFrom = 1;
    public int toolsCheckTo = 1024;
    public String toolsCheckProto = "TCP";
    public String toolsKnockHost = "";
    public String toolsKnockSequence = "7000,8000,9000";
    public String toolsKnockProto = "TCP";
    public int toolsKnockDelayMs = 200;
    public String toolsSshHost = "";
    public int toolsSshPort = 22;
    public String toolsSshUser = "root";
    public String toolsSshPass = "";
    public String toolsSshCommand = "uname -a";
    public String toolsMirrorSrc = "";
    public String toolsMirrorDst = "";

    // === Section 3B3: v6.0 Stack Lab extras ===
    public int tcpLingerSec = 0;
    public int tcpKeepaliveCount = 4;
    public boolean tcpSack = true;
    public boolean tcpTimestamps = true;
    public boolean tcpTwReuse = true;
    public int tcpFinTimeoutSec = 60;
    public boolean tcpPacing = false;
    public int tcpMaxSynBacklog = 128;

    public boolean udpDontFrag = true;
    public boolean udpGroHint = false;
    public boolean udpHairpin = false;
    public int udpMulticastTtl = 1;
    public int udpNatMaxEntries = 4096;
    public int udpConnectTimeoutMs = 1500;

    public int soLingerSec = 0;
    public int soPriority = 0;
    public int soMark = 0;
    public boolean socketDualStack = true;
    public String soBindToDevice = "";

    public String socks5AuthUser = "";
    public String socks5AuthPass = "";
    public boolean socks5UdpAssociate = true;
    public boolean socks5RemoteDns = true;
    public int socks5ConnectTimeoutMs = 8000;
    public String httpConnectUser = "";
    public String httpConnectPass = "";

    public String wgPrivateKey = "";
    public String wgAllowedIps = "0.0.0.0/0, ::/0";
    public String wgDns = "1.1.1.1";
    public int wgListenPort = 51820;
    public int wgKeepaliveSec = 25;
    public int wgMtu = 1420;
    public String wgPresharedKey = "";

    public String orbotSocksHost = "127.0.0.1";
    public int orbotSocksPort = 9050;
    public int orbotHttpPort = 8118;
    public int orbotControlPort = 9051;
    public boolean orbotVpnMode = false;
    public boolean orbotStartOnConnect = true;
    public boolean torNewnymOnStart = false;
    public int torCircuitTimeoutSec = 60;

    public int i2pInboundLength = 3;
    public int i2pOutboundLength = 3;
    public int i2pInboundQty = 2;
    public int i2pOutboundQty = 2;
    public boolean i2pHiddenMode = false;

    public boolean vpnAlwaysOnHint = false;
    public boolean vpnBlockIpv6 = false;
    public boolean networkHappyEyeballs = true;
    public int networkHappyDelayMs = 250;
    public boolean networkCaptiveBypass = true;
    public boolean networkPreferWifi = false;

    public String httpsMaxTls = "TLS 1.3";
    public boolean httpsTls13Only = false;
    public boolean httpsOcspStapling = true;
    public boolean httpsCertPinStrict = false;
    public boolean httpsGrease = true;
    public boolean httpsEchEnabled = false;
    public boolean httpsH3Alt = false;
    public boolean httpsTrustSystem = true;

    // === Section 3C: Anti-Censorship DPI + LBox TLS Fragmentation & SNI Shaping ===
    public String dpiStrategy = "Always";   // None, Auto, Always
    public String dpiTechnique = "Auto";    // None, Auto, TCP, TCP/TLS, Desync, Hybrid
    public int tlsRecordSplitBytes = 2;
    public boolean tlsFragment = false;
    public boolean tlsRecordFragment = true;
    public int tlsFallbackDelayMs = 500;
    public boolean mixedCaseSni = false;
    public boolean spoofSniEnabled = false;
    public String spoofSniHost = "play.googleapis.com";

    // === Section 3D: InviZible DNSCrypt Full Settings ===
    public int dnscryptListenPort = 5354;
    public boolean dnscryptEnableProto = true;
    public boolean dnscryptEnableDoh = true;
    public boolean dnscryptEnableOdoh = true;
    public boolean dnscryptIpv4Servers = true;
    public boolean dnscryptIpv6Servers = true;
    public boolean dnscryptRequireDnssec = false;
    public boolean dnscryptRequireNoLog = true;
    public boolean dnscryptRequireNoFilter = false;
    public boolean dnscryptForceTcp = false;
    public boolean dnscryptOutboundProxy = false;
    public int dnscryptOutboundProxyPort = 9050;
    public boolean dnscryptIgnoreSystemDns = false;
    public boolean dnscryptHttp3 = true;
    public String dnscryptDns64Prefix = "64:ff9b::/96";
    public boolean dnscryptBlockUnqualified = true;
    public boolean dnscryptBlockUndelegated = true;
    public boolean dnscryptBlockIpv6 = false;
    public int dnscryptRefreshDelayHours = 72;
    public boolean dnscryptQueryLogging = true;
    public boolean dnscryptSuspiciousLogging = true;

    // === Section 3E: InviZible Tor Full Settings ===
    public String torBridgeType = "obfs4";
    public String torExcludeExitNodes = "";
    public String torExitNodes = "";
    public String torExcludeNodes = "";
    public String torEntryNodes = "";
    public boolean torStrictNodes = false;
    public String torVirtualAddrNetwork = "10.192.0.0/10";
    public boolean torEnforceDistinctSubnets = true;
    public boolean torTrackHostExits = false;
    public boolean torClientUseIpv4 = true;
    public boolean torClientUseIpv6 = true;
    public boolean torIsolateUid = true;
    public boolean torIsolateDestAddr = false;
    public boolean torIsolateDestPort = false;
    public int torSocksPort = 9050;
    public boolean torHttpTunnelEnabled = true;
    public int torHttpTunnelPort = 8118;
    public boolean torTransProxyEnabled = true;
    public int torTransPort = 9040;
    public boolean torResolveDns = true;
    public int torDnsPort = 5400;
    public String torSnowflakeRendezvous = "AMP Cache";
    public String torSnowflakeStun = "stun:stun.l.google.com:19302,stun:stun.voip.blackberry.com:3478";
    public boolean torSocksOutboundProxy = false;
    public boolean torHardwareAccel = true;
    public boolean torAvoidDiskWrites = true;
    public boolean torConnectionPadding = true;
    public boolean torReducedConnectionPadding = true;
    public boolean torFascistFirewall = false;
    public int torNewCircuitPeriodSec = 30;
    public int torMaxCircuitDirtinessSec = 600;
    public int torDormantClientTimeoutMin = 10;

    // === Section 3F: InviZible Purple I2P Full Settings ===
    public boolean i2pIpv6 = true;
    public boolean i2pNoTransit = false;
    public boolean i2pFloodfill = false;
    public int i2pBandwidthLimitKbps = 2048;
    public int i2pTransitSharePct = 50;
    public boolean i2pSsu2 = true;
    public boolean i2pNtcp2 = true;
    public boolean i2pHttpProxyEnabled = true;
    public int i2pHttpProxyPort = 4444;
    public boolean i2pHttpOutproxyEnabled = false;
    public String i2pHttpOutproxyAddr = "http://false.i2p";
    public boolean i2pSocksProxyEnabled = true;
    public int i2pSocksProxyPort = 4447;
    public boolean i2pSocksOutproxyEnabled = false;
    public String i2pSocksOutproxyAddr = "127.0.0.1:9050";
    public boolean i2pSamEnabled = false;
    public int i2pSamPort = 7656;
    public boolean i2pElgamalPrecomputed = true;
    public boolean i2pUpnp = true;
    public boolean i2pReseedVerifySu3 = true;
    public int i2pMaxTransitTunnels = 2500;
    public String i2pAddressBookUrl = DnsPresets.DEFAULT_I2P_HOSTS_URL;

    // === Section 3G: DNS Booster, Filtering & On-Device Blocklists ===
    public boolean dnsBooster = true;
    public boolean neverProxyDns = false;
    public boolean preventDnsLeaks = true;
    public boolean blockUnknownDnsSource = false;
    public boolean systemDnsUndelegated = true;
    public boolean systemDnsCaptivePortal = true;
    public String dnsForBypass = "Use Bootstrap DNS";
    public boolean onDeviceBlocklistsEnabled = true;
    public boolean useInAppDownloader = true;
    public boolean promptBlocklistUpdates = false;
    public boolean advancedDnsFiltering = true;
    public boolean splitDns = true;

    // === Section 3H: Network, MITM Security, Autostart & Shell Script Automation ===
    public boolean loopbackExperimental = false;
    public String bootstrapDns = "1.1.1.1:53";
    public String connectionChangePolicy = "Auto";
    public boolean stallOnNetworkLoss = false;
    public boolean enableNetworkVisibility = true;
    public boolean doNotRoutePrivateIps = false;
    public boolean useAllAvailableNetworks = true;
    public boolean alwaysMetered = false;
    public boolean meterMobileNetworks = false;

    public boolean killSwitch = true;
    public boolean blockScreenLocked = false;
    public boolean blockMetered = false;
    public boolean blockUdpNonDns = false;
    public boolean blockBypassedDns = true;
    public boolean blockLan = false;
    public boolean blockNewlyInstalledApps = false;

    public boolean arpSpoofingDetection = false;
    public boolean preventDeviceSleep = false;
    public boolean enableScriptControl = false;
    public boolean autostartDnscrypt = false;
    public boolean autostartTor = false;
    public boolean autostartI2p = false;
    public boolean autostartVpn = false;
    public int autostartDelaySec = 3;

    // === Section 3I: General & Logs / Packet Capture ===
    public boolean rememberUninstalledApps = false;
    public boolean firewallBubble = false;
    public boolean showProviderInfo = true;
    public String packetCaptureMode = "Off";
    public boolean enableOnDeviceLogging = true;
    public String logLevel = "Info";
    public String logLifespan = "7 days";

    public SkidConfig() {
        applySensibleDefaults();
    }

    public void applySensibleDefaults() {
        dnsServers.clear();
        dnsServers.add("https://sky.rethinkdns.com/rec");
        dnsServers.add("tls://1dot1dot1dot1.cloudflare-dns.com");
        dnsServers.add("9.9.9.9:53");

        vpnServers.clear();
        vpnServers.add("ch-zurich-01.nym-gw.skid:51820");
        vpnServers.add("nl-amsterdam-02.mysterium-dvpn.skid:443");

        addresses.clear();
        addresses.add("10.213.37.2/24");
        addresses.add("fd00:534b:4944::2/64");

        socks5.clear();
        socks5.add("127.0.0.1:10808");
        socks5.add("127.0.0.1:9050");

        vpnPorts.clear();
        vpnPorts.addAll(Arrays.asList("51820", "4500", "500", "1701", "443"));

        proxyPorts.clear();
        proxyPorts.addAll(Arrays.asList("10808", "10809", "9050", "4444"));

        dnsPorts.clear();
        dnsPorts.addAll(Arrays.asList("53", "853", "443", "5400", "5354"));

        firewallRules.clear();
        firewallRules.add("BLOCK_DOMAIN:doubleclick.net");
        firewallRules.add("BLOCK_DOMAIN:googleadservices.com");
        firewallRules.add("BLOCK_DOMAIN:graph.facebook.com");
        firewallRules.add("BLOCK_DOMAIN:app-measurement.com");
        firewallRules.add("BLOCK_DOMAIN:ads.tiktok.com");
        firewallRules.add("BLOCK_PORT:445");
        firewallRules.add("BLOCK_PORT:135");

        allowedDnsRecords.clear();
        allowedDnsRecords.add("ALL");

        activeBlocklistPacks.clear();
        activeBlocklistPacks.add("Adware & Trackers");
        activeBlocklistPacks.add("Malware & Phishing");

        lboxTemplates.clear();
        lboxTemplates.add("cloudflare_dot");
        lboxTemplates.add("quad9_doh");

        dnscryptRelays.clear();
        dnscryptRelays.add("sdns://gRE3NS4xMTkuMTQ0LjA6ODQ0Mw");

        cloakingRules.clear();
        cloakingRules.add("skid.local=127.0.0.1");
        cloakingRules.add("router.i2p=127.0.0.1");

        forwardingRules.clear();
        whitelistDomains.clear();
        ipBlacklist.clear();
        ignoredQtypes.clear();

        subscriptionUrls.clear();
        subscriptionUrls.add(DnsPresets.DNSCRYPT_RESOLVER_SOURCES[0]);
        subscriptionUrls.add(DnsPresets.DNSCRYPT_RELAY_SOURCES[0]);
        subscriptionUrls.add(DnsPresets.DEFAULT_I2P_HOSTS_URL);
    }

    public int countActiveUniversalRules() {
        int c = 0;
        if (blockScreenLocked) c++;
        if (blockMetered) c++;
        if (blockUdpNonDns) c++;
        if (blockBypassedDns || preventDnsLeaks) c++;
        if (blockLan) c++;
        if (blockNewlyInstalledApps) c++;
        if (blockUnknownDnsSource) c++;
        if (dnscryptBlockUnqualified) c++;
        if (dnscryptBlockIpv6) c++;
        if (arpSpoofingDetection) c++;
        if (udpQuicPolicy != null && udpQuicPolicy.toLowerCase().contains("block")) c++;
        return c;
    }

    public int getTrafficClassByte() {
        if (dscpTrafficClass == null) return 0x10;
        if (dscpTrafficClass.contains("0xB8")) return 0xB8;
        if (dscpTrafficClass.contains("0x88")) return 0x88;
        if (dscpTrafficClass.contains("0x20")) return 0x20;
        if (dscpTrafficClass.contains("0x08")) return 0x08;
        if (dscpTrafficClass.contains("0x00")) return 0x00;
        return 0x10;
    }

    public int getEffectiveMtu() {
        if (bandwidthBooster) return 1500;
        return Math.max(1280, Math.min(1500, mtu));
    }

    public int getPrimaryLocalSocksPort() {
        return parsePortAt(proxyPorts, 0, 10808);
    }

    public int getPrimaryLocalHttpPort() {
        return parsePortAt(proxyPorts, 1, 10809);
    }

    public int getPrimaryDnsPort() {
        return parsePortAt(dnsPorts, 0, 53);
    }

    public int getDoTPort() {
        for (String p : dnsPorts) {
            int val = safePort(p, -1);
            if (val == 853 || val == 8853 || val == 5443 || val == 8443) return val;
        }
        return 853;
    }

    private static int parsePortAt(List<String> list, int idx, int fallback) {
        if (list == null || idx >= list.size()) return fallback;
        return safePort(list.get(idx), fallback);
    }

    private static int safePort(String s, int fallback) {
        if (s == null) return fallback;
        try {
            int p = Integer.parseInt(s.trim());
            return (p > 0 && p <= 65535) ? p : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    public SkidConfig copy() {
        return SkidConfigParser.parse(SkidConfigParser.serialize(this)).config;
    }
}
