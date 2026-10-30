package com.skidwaredns.app.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Parser, validator, and canonical serializer for SkidwareDNS v3.0 [.skid] configuration files.
 */
public final class SkidConfigParser {

    public static final class ParseResult {
        public final SkidConfig config;
        public final boolean valid;
        public final int sectionDividers;
        public final int parsedBrackets;
        public final int parsedDirectives;
        public final List<String> warnings;

        public ParseResult(SkidConfig config, boolean valid, int sectionDividers,
                           int parsedBrackets, int parsedDirectives, List<String> warnings) {
            this.config = config;
            this.valid = valid;
            this.sectionDividers = sectionDividers;
            this.parsedBrackets = parsedBrackets;
            this.parsedDirectives = parsedDirectives;
            this.warnings = warnings;
        }

        public String buildSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append(valid ? "[VALID .SKID v3.0]" : "[INVALID .SKID]");
            sb.append(" Profile=\"").append(config.name).append("\"");
            sb.append(" | Sections=").append(sectionDividers);
            sb.append(" | Arrays=").append(parsedBrackets);
            sb.append(" | Directives=").append(parsedDirectives);
            if (!warnings.isEmpty()) {
                sb.append("\nNotes: ");
                for (int i = 0; i < warnings.size(); i++) {
                    if (i > 0) sb.append("; ");
                    sb.append(warnings.get(i));
                }
            }
            return sb.toString();
        }
    }

    private SkidConfigParser() {}

    public static ParseResult parse(String rawText) {
        SkidConfig cfg = new SkidConfig();
        cfg.dnsServers.clear();
        cfg.vpnServers.clear();
        cfg.addresses.clear();
        cfg.socks5.clear();
        cfg.vpnPorts.clear();
        cfg.proxyPorts.clear();
        cfg.dnsPorts.clear();
        cfg.customDnsServer.clear();
        cfg.customVpnServer.clear();
        cfg.customProxyServer.clear();
        cfg.firewallRules.clear();
        cfg.blockedApps.clear();
        cfg.bypassApps.clear();
        cfg.torifyApps.clear();
        cfg.allowedDnsRecords.clear();
        cfg.activeBlocklistPacks.clear();
        cfg.lboxTemplates.clear();
        cfg.dnscryptRelays.clear();
        cfg.cloakingRules.clear();
        cfg.forwardingRules.clear();
        cfg.whitelistDomains.clear();
        cfg.ipBlacklist.clear();
        cfg.ignoredQtypes.clear();
        cfg.subscriptionUrls.clear();

        List<String> warnings = new ArrayList<>();
        if (rawText == null || rawText.trim().isEmpty()) {
            cfg.applySensibleDefaults();
            warnings.add("Empty .skid input - loaded default values");
            return new ParseResult(cfg, false, 0, 0, 0, warnings);
        }

        String[] lines = rawText.replace("\r\n", "\n").replace('\r', '\n').split("\n");
        int dividers = 0;
        int brackets = 0;
        int directives = 0;

        String multiLineKey = null;
        StringBuilder multiLineBuf = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) {
                continue;
            }
            if (line.startsWith("===")) {
                dividers++;
                continue;
            }

            if (multiLineKey != null) {
                int closeIdx = line.indexOf(']');
                if (closeIdx >= 0) {
                    multiLineBuf.append(',').append(line.substring(0, closeIdx));
                    applyBracketBlock(cfg, multiLineKey, multiLineBuf.toString(), warnings, i + 1);
                    brackets++;
                    multiLineKey = null;
                    multiLineBuf.setLength(0);
                } else {
                    multiLineBuf.append(',').append(line);
                }
                continue;
            }

            int eqIdx = line.indexOf('=');
            int openBracket = line.indexOf('[');

            if (openBracket > 0 && (eqIdx < 0 || openBracket < eqIdx)) {
                String key = line.substring(0, openBracket).trim();
                int closeBracket = line.lastIndexOf(']');
                if (closeBracket > openBracket) {
                    String inside = line.substring(openBracket + 1, closeBracket);
                    applyBracketBlock(cfg, key, inside, warnings, i + 1);
                    brackets++;
                } else {
                    multiLineKey = key;
                    multiLineBuf.setLength(0);
                    multiLineBuf.append(line.substring(openBracket + 1));
                }
                continue;
            }

            if (eqIdx > 0) {
                String key = line.substring(0, eqIdx).trim();
                String val = line.substring(eqIdx + 1).trim();
                applyDirective(cfg, key, val, warnings, i + 1);
                directives++;
                continue;
            }

            warnings.add("Line " + (i + 1) + " ignored: " + line);
        }

        if (cfg.addresses.isEmpty()) {
            cfg.addresses.add("10.213.37.2/24");
            cfg.addresses.add("fd00:534b:4944::2/64");
        }
        if (cfg.proxyPorts.isEmpty()) {
            cfg.proxyPorts.add("10808");
            cfg.proxyPorts.add("10809");
        }
        if (cfg.dnsPorts.isEmpty()) {
            cfg.dnsPorts.add("53");
            cfg.dnsPorts.add("853");
            cfg.dnsPorts.add("443");
        }
        if (cfg.vpnPorts.isEmpty()) {
            cfg.vpnPorts.add("51820");
            cfg.vpnPorts.add("443");
        }
        if (cfg.allowedDnsRecords.isEmpty()) {
            cfg.allowedDnsRecords.add("ALL");
        }
        if (cfg.activeBlocklistPacks.isEmpty()) {
            cfg.activeBlocklistPacks.add("Adware & Trackers");
            cfg.activeBlocklistPacks.add("Malware & Phishing");
        }

        boolean isValid = (brackets + directives) > 0;
        if (dividers == 0) {
            warnings.add("No =========== section dividers found (parsed tolerantly)");
        }
        return new ParseResult(cfg, isValid, dividers, brackets, directives, warnings);
    }

    private static void applyBracketBlock(SkidConfig cfg, String rawKey, String inside, List<String> warnings, int lineNo) {
        String k = normalizeKey(rawKey);
        List<String> items = splitItems(inside);
        switch (k) {
            case "dnsservers": cfg.dnsServers.addAll(items); break;
            case "vpnservers": cfg.vpnServers.addAll(items); break;
            case "addresses": cfg.addresses.addAll(items); break;
            case "socks5": cfg.socks5.addAll(items); break;
            case "vpnports": cfg.vpnPorts.addAll(items); break;
            case "proxyports": cfg.proxyPorts.addAll(items); break;
            case "dnsports": cfg.dnsPorts.addAll(items); break;
            case "customdnsserver":
            case "customdnsservers": cfg.customDnsServer.addAll(items); break;
            case "customvpnserver":
            case "customvpnservers": cfg.customVpnServer.addAll(items); break;
            case "customproxyserver":
            case "customproxyservers": cfg.customProxyServer.addAll(items); break;
            case "firewallrules":
            case "firewall": cfg.firewallRules.addAll(items); break;
            case "blockedapps": cfg.blockedApps.addAll(items); break;
            case "bypassapps": cfg.bypassApps.addAll(items); break;
            case "torifyapps": cfg.torifyApps.addAll(items); break;
            case "alloweddnsrecords": cfg.allowedDnsRecords.addAll(items); break;
            case "blocklistpacks":
            case "ondeviceblocklists": cfg.activeBlocklistPacks.addAll(items); break;
            case "lboxtemplates": cfg.lboxTemplates.addAll(items); break;
            case "dnscryptrelays": cfg.dnscryptRelays.addAll(items); break;
            case "cloakingrules": cfg.cloakingRules.addAll(items); break;
            case "forwardingrules": cfg.forwardingRules.addAll(items); break;
            case "whitelistdomains": cfg.whitelistDomains.addAll(items); break;
            case "ipblacklist": cfg.ipBlacklist.addAll(items); break;
            case "ignoredqtypes": cfg.ignoredQtypes.addAll(items); break;
            case "subscriptionurls": cfg.subscriptionUrls.addAll(items); break;
            case "openlistenports": cfg.openListenPorts.addAll(items); break;
            default:
                warnings.add("Line " + lineNo + ": unknown array '" + rawKey + "'");
                break;
        }
    }

    private static void applyDirective(SkidConfig cfg, String rawKey, String val, List<String> warnings, int lineNo) {
        String k = normalizeKey(rawKey);
        if (val.isEmpty()) return;
        switch (k) {
            case "torenabled": cfg.torEnabled = parseBool(val, false); break;
            case "dnscryptmoduleenabled": cfg.dnscryptModuleEnabled = parseBool(val, true); break;
            case "i2penabled": cfg.i2pEnabled = parseBool(val, false); break;
            case "mode": cfg.mode = val; break;
            case "name": cfg.name = val; break;
            case "proxymode": cfg.proxyMode = val; break;
            case "dnsmode": cfg.dnsMode = val; break;
            case "globaldnscategory": cfg.globalDnsCategory = val; break;
            case "quad9": cfg.quad9 = val; break;
            case "dnscrypt": cfg.dnsCrypt = val; break;
            case "dnsovertls": cfg.dnsOverTls = val; break;
            case "dnsproxy": cfg.dnsProxy = val; break;
            case "tunnel":
            case "tunneltype": cfg.tunnel = val; break;
            case "connectfrommode":
            case "connectfrom": cfg.connectFromMode = val; break;

            // Tunnel / Hysteria / Jump Host / Fake-IP
            case "rethinkdoh":
            case "rethinkdns": cfg.rethinkDoh = val; break;
            case "tunnelhost": cfg.tunnelHost = val; break;
            case "tunnelport": cfg.tunnelPort = parseInt(val, 443); break;
            case "tunneluser": cfg.tunnelUser = val; break;
            case "tunnelauth": cfg.tunnelAuth = val; break;
            case "tunnelcipher": cfg.tunnelCipher = val; break;
            case "v2raytransport": cfg.v2rayTransport = val; break;
            case "sni": cfg.sni = val; break;
            case "bughost": cfg.bugHost = val; break;
            case "httppayload": cfg.httpPayload = val; break;
            case "dnsttdomain": cfg.dnsttDomain = val; break;
            case "dnsttpubkey": cfg.dnsttPubKey = val; break;
            case "mixnetmode": cfg.mixnetMode = val; break;
            case "mtu": cfg.mtu = Math.max(1280, Math.min(1500, parseInt(val, 1400))); break;

            case "hysteriaversion": cfg.hysteriaVersion = parseInt(val, 2) == 1 ? 1 : 2; break;
            case "hysteriaprotocol": cfg.hysteriaProtocol = val; break;
            case "hysteriaupmbps": cfg.hysteriaUpMbps = Math.max(1, Math.min(1000, parseInt(val, 50))); break;
            case "hysteriadownmbps": cfg.hysteriaDownMbps = Math.max(1, Math.min(2000, parseInt(val, 200))); break;
            case "hysteriaobfspassword": cfg.hysteriaObfsPassword = val; break;
            case "hysteriaporthoprange": cfg.hysteriaPortHopRange = val; break;
            case "hysteriahopintervalsec": cfg.hysteriaHopIntervalSec = Math.max(5, Math.min(600, parseInt(val, 30))); break;
            case "hysteriaalpn": cfg.hysteriaAlpn = val; break;
            case "hysteriaallowinsecure": cfg.hysteriaAllowInsecure = parseBool(val, false); break;
            case "hysteriacustomcert": cfg.hysteriaCustomCert = val; break;
            case "hysteriaquicrcvwinmb": cfg.hysteriaQuicRcvWinMb = Math.max(1, Math.min(64, parseInt(val, 8))); break;
            case "hysteriadisablemtudiscovery": cfg.hysteriaDisableMtuDiscovery = parseBool(val, false); break;

            case "custompayloadenabled": cfg.customPayloadEnabled = parseBool(val, false); break;
            case "sshuseprivatekey": cfg.sshUsePrivateKey = parseBool(val, false); break;
            case "sshprivatekey": cfg.sshPrivateKey = val; break;
            case "sshlocalport": cfg.sshLocalPort = parseInt(val, 1080); break;
            case "sshudpgwport": cfg.sshUdpgwPort = parseInt(val, 7300); break;
            case "sshsendbufferbytes": cfg.sshSendBufferBytes = Math.max(1024, Math.min(1048576, parseInt(val, 16384))); break;
            case "sshrecvbufferbytes": cfg.sshRecvBufferBytes = Math.max(1024, Math.min(1048576, parseInt(val, 32768))); break;
            case "sshheaderreplacer": cfg.sshHeaderReplacer = parseBool(val, false); break;
            case "sshrouteproxy": cfg.sshRouteProxy = parseBool(val, false); break;

            case "xraycore": cfg.xrayCore = val; break;
            case "xrayprotocol": cfg.xrayProtocol = val; break;
            case "xrayuuid": cfg.xrayUuid = val; break;
            case "xrayalterid": cfg.xrayAlterId = Math.max(0, parseInt(val, 0)); break;
            case "xrayservercipher": cfg.xrayServerCipher = val; break;
            case "xraysecurity": cfg.xraySecurity = val; break;
            case "xraynetworktype": cfg.xrayNetworkType = val; break;
            case "xrayheadertype": cfg.xrayHeaderType = val; break;
            case "xrayrealitypubkey": cfg.xrayRealityPubKey = val; break;
            case "xrayrealityshortid": cfg.xrayRealityShortId = val; break;
            case "xraymuxenabled": cfg.xrayMuxEnabled = parseBool(val, false); break;
            case "xraymuxconcurrency": cfg.xrayMuxConcurrency = Math.max(1, Math.min(64, parseInt(val, 8))); break;
            case "xraydomainstrategy": cfg.xrayDomainStrategy = val; break;
            case "xrayroutingrules": cfg.xrayRoutingRules = val; break;
            case "ssobfsserver": cfg.ssObfsServer = val; break;
            case "hysteriacertificates": cfg.hysteriaCertificates = val; break;

            case "appsfilterenabled": cfg.appsFilterEnabled = parseBool(val, false); break;
            case "vpndnspreset": cfg.vpnDnsPreset = val; break;
            case "dnsforwarding": cfg.dnsForwarding = parseBool(val, true); break;
            case "usedefaultroute": cfg.useDefaultRoute = parseBool(val, true); break;
            case "customroutes": cfg.customRoutes = val; break;
            case "excludedroutes": cfg.excludedRoutes = val; break;
            case "httppingenabled": cfg.httpPingEnabled = parseBool(val, false); break;
            case "httppingurl": cfg.httpPingUrl = val; break;
            case "injectordohprovider": cfg.injectorDohProvider = val; break;
            case "darkmodetheme": cfg.darkModeTheme = val; break;
            case "uidarkmodesystem": cfg.darkModeSystem = parseBool(val, true); break;
            case "uilanguage": cfg.uiLanguage = val; break;
            case "shownotificationicon": cfg.showNotificationIcon = parseBool(val, true); break;
            case "vibrateonconnect": cfg.vibrateOnConnect = parseBool(val, true); break;
            case "nymprofile": cfg.nymProfile = val; break;
            case "nymusercountry": cfg.nymUserCountry = val; break;
            case "mixnetcontinuoustraffic": cfg.mixnetContinuousTraffic = parseBool(val, true); break;
            case "mixnetcontinuousrate": cfg.mixnetContinuousRate = val; break;
            case "mixnetcovertraffic":
            case "mixnetcovertrafficenabled": cfg.mixnetCoverTrafficEnabled = parseBool(val, true); break;
            case "mixnetcoverrate":
            case "mixnetcovertrafficrate": cfg.mixnetCoverTrafficRate = val; break;
            case "mixnetpacketmixingms":
            case "mixnetpacketmixingdelayms": cfg.mixnetPacketMixingDelayMs = Math.max(0, Math.min(500, parseInt(val, 15))); break;
            case "nymquicenhanced": cfg.nymQuicEnhanced = parseBool(val, false); break;
            case "amneziawgminimalobfs": cfg.amneziaWgMinimalObfs = parseBool(val, true); break;
            case "amneziawgjc": cfg.amneziaWgJc = Math.max(1, Math.min(32, parseInt(val, 4))); break;
            case "amneziawgjmin": cfg.amneziaWgJmin = Math.max(10, Math.min(512, parseInt(val, 40))); break;
            case "amneziawgjmax": cfg.amneziaWgJmax = Math.max(20, Math.min(1024, parseInt(val, 70))); break;
            case "nymstealthapiconnect": cfg.nymStealthApiConnect = parseBool(val, false); break;
            case "nymstealthcoverdomains": cfg.nymStealthCoverDomains = val; break;
            case "mullvadenabled": cfg.mullvadEnabled = parseBool(val, false); break;
            case "mullvadmultihop": cfg.mullvadMultihop = parseBool(val, false); break;
            case "mullvadentryrelay": cfg.mullvadEntryRelay = val; break;
            case "mullvadexitrelay": cfg.mullvadExitRelay = val; break;
            case "mullvaddaita": cfg.mullvadDaita = parseBool(val, true); break;
            case "mullvadusesocks5": cfg.mullvadUseSocks5 = parseBool(val, false); break;
            case "mullvadownedonly": cfg.mullvadOwnedOnly = parseBool(val, true); break;
            case "mullvadbridgerelay": cfg.mullvadBridgeRelay = val; break;
            case "mullvadsshsha256":
            case "mullvadsshsha256pin": cfg.mullvadSshSha256Pin = val; break;

            case "jumphostenabled": cfg.jumpHostEnabled = parseBool(val, false); break;
            case "jumpprotocol": cfg.jumpProtocol = val; break;
            case "jumphost": cfg.jumpHost = val; break;
            case "jumpport": cfg.jumpPort = parseInt(val, 443); break;
            case "jumpuser": cfg.jumpUser = val; break;
            case "jumpauth": cfg.jumpAuth = val; break;
            case "proxyknockhost": cfg.proxyKnockHost = val; break;
            case "ja3fingerprint": cfg.ja3Fingerprint = val; break;
            case "appownershipcheck": cfg.appOwnershipCheck = val; break;
            case "fakeipenabled": cfg.fakeIpEnabled = parseBool(val, false); break;
            case "routingsplitpreset": cfg.routingSplitPreset = val; break;

            // WireGuard & Proxy Advanced
            case "wgprofilename": cfg.wgProfileName = val; break;
            case "wgendpoint": cfg.wgEndpoint = val; break;
            case "wgpublickey": cfg.wgPublicKey = val; break;
            case "wgenabled": cfg.wgEnabled = parseBool(val, false); break;
            case "socks5forwardenabled": cfg.socks5ForwardEnabled = parseBool(val, false); break;
            case "httpconnectforwardenabled": cfg.httpConnectForwardEnabled = parseBool(val, false); break;
            case "loopbackproxyforwarderapps": cfg.loopbackProxyForwarderApps = parseBool(val, false); break;
            case "fixedwireguardlistenport": cfg.fixedWireGuardListenPort = parseBool(val, false); break;
            case "wireguardp2p": cfg.wireGuardP2p = parseBool(val, true); break;
            case "proxylockdown": cfg.proxyLockdown = parseBool(val, false); break;
            case "floodwireguard": cfg.floodWireGuard = parseBool(val, true); break;
            case "smartpersistentkeepalive": cfg.smartPersistentKeepalive = parseBool(val, true); break;

            // Stack & TCP / IP & UDP
            case "strictroute": cfg.strictRoute = parseBool(val, false); break;
            case "tunstack":
            case "stack": cfg.tunStack = val; break;
            case "shortertcpkeepalive": cfg.shorterTcpKeepalive = parseBool(val, true); break;
            case "tcpnodelay": cfg.tcpNoDelay = parseBool(val, true); break;
            case "tcpfastopen": cfg.tcpFastOpen = parseBool(val, true); break;
            case "tcprstquickclose": cfg.tcpRstQuickClose = parseBool(val, false); break;
            case "tcpmssclamp": cfg.tcpMssClamp = Math.max(536, Math.min(1460, parseInt(val, 1360))); break;
            case "tcpsynretries": cfg.tcpSynRetries = Math.max(1, Math.min(8, parseInt(val, 3))); break;
            case "dscptrafficclass": cfg.dscpTrafficClass = val; break;
            case "endpointindependentmapping": cfg.endpointIndependentMapping = parseBool(val, true); break;
            case "idletimeoutsec": cfg.idleTimeoutSec = Math.max(0, Math.min(3600, parseInt(val, 0))); break;
            case "bandwidthbooster": cfg.bandwidthBooster = parseBool(val, false); break;
            case "socketbuffersizekb":
            case "socketbufferkb": cfg.socketBufferSizeKb = Math.max(64, Math.min(4096, parseInt(val, 512))); break;
            case "ipversion": cfg.ipVersion = val; break;
            case "performprotocoltranslations": cfg.performProtocolTranslations = parseBool(val, false); break;
            case "customtungatewayv4": cfg.customTunGatewayV4 = val; break;
            case "customtungatewayv6": cfg.customTunGatewayV6 = val; break;
            case "udpquicpolicy": cfg.udpQuicPolicy = val; break;
            case "udpnattimeoutsec": cfg.udpNatTimeoutSec = Math.max(10, Math.min(600, parseInt(val, 60))); break;
            case "udprandomizesourceport": cfg.udpRandomizeSourcePort = parseBool(val, true); break;
            case "udpstrictchecksum": cfg.udpStrictChecksum = parseBool(val, true); break;
            case "udpednspayloadbytes":
            case "udpednspayload": cfg.udpEdnsPayloadBytes = Math.max(512, Math.min(4096, parseInt(val, 1232))); break;

            case "tcpkeepaliveidlesec": cfg.tcpKeepaliveIdleSec = Math.max(0, Math.min(7200, parseInt(val, 45))); break;
            case "tcpkeepaliveintervalsec": cfg.tcpKeepaliveIntervalSec = Math.max(1, Math.min(600, parseInt(val, 15))); break;
            case "tcpreuseaddress": cfg.tcpReuseAddress = parseBool(val, true); break;
            case "tcpcork": cfg.tcpCork = parseBool(val, false); break;
            case "tcpusertimeoutms": cfg.tcpUserTimeoutMs = Math.max(0, Math.min(120000, parseInt(val, 0))); break;
            case "tcpcongestion": cfg.tcpCongestion = val; break;
            case "tcpwindowscaling": cfg.tcpWindowScaling = parseBool(val, true); break;
            case "tcpconnecttimeoutms": cfg.tcpConnectTimeoutMs = Math.max(500, Math.min(60000, parseInt(val, 8000))); break;
            case "udprecvtimeoutms": cfg.udpRecvTimeoutMs = Math.max(200, Math.min(30000, parseInt(val, 3000))); break;
            case "udpbroadcast": cfg.udpBroadcast = parseBool(val, false); break;
            case "udpconnectedmode": cfg.udpConnectedMode = parseBool(val, true); break;
            case "udpmaxdatagrambytes": cfg.udpMaxDatagramBytes = Math.max(512, Math.min(65507, parseInt(val, 1400))); break;
            case "udpreuseaddress": cfg.udpReuseAddress = parseBool(val, true); break;
            case "udpgsohint": cfg.udpGsoHint = parseBool(val, false); break;
            case "httpsmintls": cfg.httpsMinTls = val; break;
            case "httpsalpn": cfg.httpsAlpn = val; break;
            case "httpssessiontickets": cfg.httpsSessionTickets = parseBool(val, true); break;
            case "httpshttp2": cfg.httpsHttp2 = parseBool(val, true); break;
            case "httpstimeoutms": cfg.httpsTimeoutMs = Math.max(500, Math.min(60000, parseInt(val, 10000))); break;
            case "httpsendpointid": cfg.httpsEndpointId = parseBool(val, true); break;
            case "httpssnienabled": cfg.httpsSniEnabled = parseBool(val, true); break;
            case "httpspinsha256": cfg.httpsPinSha256 = val; break;
            case "httpsfallbackhttp11": cfg.httpsFallbackHttp11 = parseBool(val, true); break;
            case "portallowlist": cfg.portAllowList = val; break;
            case "portblocklist": cfg.portBlockList = val; break;
            case "portforwardmap": cfg.portForwardMap = val; break;
            case "portknocksequence": cfg.portKnockSequence = val; break;
            case "ephemeralportmin": cfg.ephemeralPortMin = Math.max(1024, Math.min(65534, parseInt(val, 20000))); break;
            case "ephemeralportmax": cfg.ephemeralPortMax = Math.max(1025, Math.min(65535, parseInt(val, 60000))); break;
            case "soreuseport": cfg.soReusePort = parseBool(val, true); break;
            case "bindloopbackonly": cfg.bindLoopbackOnly = parseBool(val, true); break;
            case "privilegedportprotect": cfg.privilegedPortProtect = parseBool(val, true); break;
            case "porthopenabled": cfg.portHopEnabled = parseBool(val, false); break;
            case "porthoprange": cfg.portHopRange = val; break;
            case "remapdns53todoh": cfg.remapDns53toDoH = parseBool(val, false); break;
            case "httpslistenport": cfg.httpsListenPort = Math.max(0, Math.min(65535, parseInt(val, 10443))); break;
            case "allowwellknownports": cfg.allowWellKnownPorts = parseBool(val, true); break;
            case "openportbindmode": cfg.openPortBindMode = val; break;
            case "openportbanner": cfg.openPortBanner = parseBool(val, true); break;
            case "openporthold": cfg.openPortHold = parseBool(val, false); break;
            case "toolsftpsport": cfg.toolsFtpsPort = Math.max(1024, Math.min(65535, parseInt(val, 2121))); break;
            case "toolsftpsuser": cfg.toolsFtpsUser = val; break;
            case "toolsftpspass": cfg.toolsFtpsPass = val; break;
            case "toolsscanports": cfg.toolsScanPorts = val; break;
            case "toolscheckhost": cfg.toolsCheckHost = val; break;
            case "toolscheckfrom": cfg.toolsCheckFrom = Math.max(1, Math.min(65535, parseInt(val, 1))); break;
            case "toolscheckto": cfg.toolsCheckTo = Math.max(1, Math.min(65535, parseInt(val, 1024))); break;
            case "toolscheckproto": cfg.toolsCheckProto = val; break;
            case "toolsknockhost": cfg.toolsKnockHost = val; break;
            case "toolsknocksequence": cfg.toolsKnockSequence = val; break;
            case "toolsknockproto": cfg.toolsKnockProto = val; break;
            case "toolsknockdelayms": cfg.toolsKnockDelayMs = Math.max(0, Math.min(5000, parseInt(val, 200))); break;
            case "toolssshhost": cfg.toolsSshHost = val; break;
            case "toolssshport": cfg.toolsSshPort = Math.max(1, Math.min(65535, parseInt(val, 22))); break;
            case "toolssshuser": cfg.toolsSshUser = val; break;
            case "toolssshpass": cfg.toolsSshPass = val; break;
            case "toolssshcommand": cfg.toolsSshCommand = val; break;
            case "toolsmirrorsrc": cfg.toolsMirrorSrc = val; break;
            case "toolsmirrordst": cfg.toolsMirrorDst = val; break;

            // Anti-Censorship DPI + TLS Fragmentation + SNI Shaping
            case "dpistrategy": cfg.dpiStrategy = val; break;
            case "dpitechnique": cfg.dpiTechnique = val; break;
            case "tlsrecordsplitbytes": cfg.tlsRecordSplitBytes = Math.max(1, Math.min(64, parseInt(val, 2))); break;
            case "tlsfragment": cfg.tlsFragment = parseBool(val, false); break;
            case "tlsrecordfragment": cfg.tlsRecordFragment = parseBool(val, true); break;
            case "tlsfallbackdelayms": cfg.tlsFallbackDelayMs = Math.max(50, Math.min(5000, parseInt(val, 500))); break;
            case "mixedcasesni": cfg.mixedCaseSni = parseBool(val, false); break;
            case "spoofsni":
            case "spoofsnienabled": cfg.spoofSniEnabled = parseBool(val, false); break;
            case "spoofsnihost": cfg.spoofSniHost = val; break;

            // DNSCrypt Module Settings
            case "dnscryptlistenport": cfg.dnscryptListenPort = parseInt(val, 5354); break;
            case "dnscryptenableproto": cfg.dnscryptEnableProto = parseBool(val, true); break;
            case "dnscryptenabledoh": cfg.dnscryptEnableDoh = parseBool(val, true); break;
            case "dnscryptenableodoh": cfg.dnscryptEnableOdoh = parseBool(val, true); break;
            case "dnscryptipv4servers": cfg.dnscryptIpv4Servers = parseBool(val, true); break;
            case "dnscryptipv6servers": cfg.dnscryptIpv6Servers = parseBool(val, true); break;
            case "dnscryptrequirednssec": cfg.dnscryptRequireDnssec = parseBool(val, false); break;
            case "dnscryptrequirenolog": cfg.dnscryptRequireNoLog = parseBool(val, true); break;
            case "dnscryptrequirenofilter": cfg.dnscryptRequireNoFilter = parseBool(val, false); break;
            case "dnscryptforcetcp": cfg.dnscryptForceTcp = parseBool(val, false); break;
            case "dnscryptoutboundproxy": cfg.dnscryptOutboundProxy = parseBool(val, false); break;
            case "dnscryptoutboundproxyport": cfg.dnscryptOutboundProxyPort = parseInt(val, 9050); break;
            case "dnscryptignoresystemdns": cfg.dnscryptIgnoreSystemDns = parseBool(val, false); break;
            case "dnscrypthttp3": cfg.dnscryptHttp3 = parseBool(val, true); break;
            case "dnscryptdns64prefix": cfg.dnscryptDns64Prefix = val; break;
            case "dnscryptblockunqualified": cfg.dnscryptBlockUnqualified = parseBool(val, true); break;
            case "dnscryptblockundelegated": cfg.dnscryptBlockUndelegated = parseBool(val, true); break;
            case "dnscryptblockipv6": cfg.dnscryptBlockIpv6 = parseBool(val, false); break;
            case "dnscryptrefreshdelayhours": cfg.dnscryptRefreshDelayHours = parseInt(val, 72); break;
            case "dnscryptquerylogging": cfg.dnscryptQueryLogging = parseBool(val, true); break;
            case "dnscryptsuspiciouslogging": cfg.dnscryptSuspiciousLogging = parseBool(val, true); break;

            // Tor Module Settings
            case "torbridgetype": cfg.torBridgeType = val; break;
            case "torexcludeexitnodes": cfg.torExcludeExitNodes = val; break;
            case "torexitnodes": cfg.torExitNodes = val; break;
            case "torexcludenodes": cfg.torExcludeNodes = val; break;
            case "torentrynodes": cfg.torEntryNodes = val; break;
            case "torstrictnodes": cfg.torStrictNodes = parseBool(val, false); break;
            case "torvirtualaddrnetwork": cfg.torVirtualAddrNetwork = val; break;
            case "torenforcedistinctsubnets": cfg.torEnforceDistinctSubnets = parseBool(val, true); break;
            case "tortrackhostexits": cfg.torTrackHostExits = parseBool(val, false); break;
            case "torclientuseipv4": cfg.torClientUseIpv4 = parseBool(val, true); break;
            case "torclientuseipv6": cfg.torClientUseIpv6 = parseBool(val, true); break;
            case "torisolateuid": cfg.torIsolateUid = parseBool(val, true); break;
            case "torisolatedestaddr": cfg.torIsolateDestAddr = parseBool(val, false); break;
            case "torisolatedestport": cfg.torIsolateDestPort = parseBool(val, false); break;
            case "torsocksport": cfg.torSocksPort = parseInt(val, 9050); break;
            case "torhttptunnelenabled": cfg.torHttpTunnelEnabled = parseBool(val, true); break;
            case "torhttptunnelport": cfg.torHttpTunnelPort = parseInt(val, 8118); break;
            case "tortransproxyenabled": cfg.torTransProxyEnabled = parseBool(val, true); break;
            case "tortransport": cfg.torTransPort = parseInt(val, 9040); break;
            case "torresolvedns": cfg.torResolveDns = parseBool(val, true); break;
            case "tordnsport": cfg.torDnsPort = parseInt(val, 5400); break;
            case "torsnowflakerendezvous": cfg.torSnowflakeRendezvous = val; break;
            case "torsnowflakestun": cfg.torSnowflakeStun = val; break;
            case "torsocksoutboundproxy": cfg.torSocksOutboundProxy = parseBool(val, false); break;
            case "torhardwareaccel": cfg.torHardwareAccel = parseBool(val, true); break;
            case "toravoiddiskwrites": cfg.torAvoidDiskWrites = parseBool(val, true); break;
            case "torconnectionpadding": cfg.torConnectionPadding = parseBool(val, true); break;
            case "torreducedconnectionpadding": cfg.torReducedConnectionPadding = parseBool(val, true); break;
            case "torfascistfirewall": cfg.torFascistFirewall = parseBool(val, false); break;
            case "tornewcircuitperiodsec": cfg.torNewCircuitPeriodSec = parseInt(val, 30); break;
            case "tormaxcircuitdirtinesssec": cfg.torMaxCircuitDirtinessSec = parseInt(val, 600); break;
            case "tordormantclienttimeoutmin": cfg.torDormantClientTimeoutMin = parseInt(val, 10); break;

            // Purple I2P Module Settings
            case "i2pipv6": cfg.i2pIpv6 = parseBool(val, true); break;
            case "i2pnotransit": cfg.i2pNoTransit = parseBool(val, false); break;
            case "i2pfloodfill": cfg.i2pFloodfill = parseBool(val, false); break;
            case "i2pbandwidthlimitkbps": cfg.i2pBandwidthLimitKbps = parseInt(val, 2048); break;
            case "i2ptransitsharepct": cfg.i2pTransitSharePct = Math.max(0, Math.min(100, parseInt(val, 50))); break;
            case "i2pssu2": cfg.i2pSsu2 = parseBool(val, true); break;
            case "i2pntcp2": cfg.i2pNtcp2 = parseBool(val, true); break;
            case "i2phttpproxyenabled": cfg.i2pHttpProxyEnabled = parseBool(val, true); break;
            case "i2phttpproxyport": cfg.i2pHttpProxyPort = parseInt(val, 4444); break;
            case "i2phttpoutproxyenabled": cfg.i2pHttpOutproxyEnabled = parseBool(val, false); break;
            case "i2phttpoutproxyaddr": cfg.i2pHttpOutproxyAddr = val; break;
            case "i2psocksproxyenabled": cfg.i2pSocksProxyEnabled = parseBool(val, true); break;
            case "i2psocksproxyport": cfg.i2pSocksProxyPort = parseInt(val, 4447); break;
            case "i2psocksoutproxyenabled": cfg.i2pSocksOutproxyEnabled = parseBool(val, false); break;
            case "i2psocksoutproxyaddr": cfg.i2pSocksOutproxyAddr = val; break;
            case "i2psamenabled": cfg.i2pSamEnabled = parseBool(val, false); break;
            case "i2psamport": cfg.i2pSamPort = parseInt(val, 7656); break;
            case "i2pelgamalprecomputed": cfg.i2pElgamalPrecomputed = parseBool(val, true); break;
            case "i2pupnp": cfg.i2pUpnp = parseBool(val, true); break;
            case "i2preseedverifysu3": cfg.i2pReseedVerifySu3 = parseBool(val, true); break;
            case "i2pmaxtransittunnels": cfg.i2pMaxTransitTunnels = parseInt(val, 2500); break;
            case "i2paddressbookurl": cfg.i2pAddressBookUrl = val; break;

            // DNS Booster & Filtering
            case "dnsbooster": cfg.dnsBooster = parseBool(val, true); break;
            case "neverproxydns": cfg.neverProxyDns = parseBool(val, false); break;
            case "preventdnsleaks": cfg.preventDnsLeaks = parseBool(val, true); break;
            case "blockunknowndnssource": cfg.blockUnknownDnsSource = parseBool(val, false); break;
            case "systemdnsundelegated": cfg.systemDnsUndelegated = parseBool(val, true); break;
            case "systemdnscaptiveportal": cfg.systemDnsCaptivePortal = parseBool(val, true); break;
            case "dnsforbypass": cfg.dnsForBypass = val; break;
            case "ondeviceblocklistsenabled": cfg.onDeviceBlocklistsEnabled = parseBool(val, true); break;
            case "useinappdownloader": cfg.useInAppDownloader = parseBool(val, true); break;
            case "promptblocklistupdates": cfg.promptBlocklistUpdates = parseBool(val, false); break;
            case "advanceddnsfiltering": cfg.advancedDnsFiltering = parseBool(val, true); break;
            case "splitdns": cfg.splitDns = parseBool(val, true); break;

            // Network, MITM Security, Autostart & Automation
            case "loopbackexperimental": cfg.loopbackExperimental = parseBool(val, false); break;
            case "bootstrapdns": cfg.bootstrapDns = val; break;
            case "connectionchangepolicy": cfg.connectionChangePolicy = val; break;
            case "stallonnetworkloss": cfg.stallOnNetworkLoss = parseBool(val, false); break;
            case "enablenetworkvisibility": cfg.enableNetworkVisibility = parseBool(val, true); break;
            case "donotrouteprivateips": cfg.doNotRoutePrivateIps = parseBool(val, false); break;
            case "useallavailablenetworks": cfg.useAllAvailableNetworks = parseBool(val, true); break;
            case "alwaysmetered": cfg.alwaysMetered = parseBool(val, false); break;
            case "metermobilenetworks": cfg.meterMobileNetworks = parseBool(val, false); break;

            case "killswitch": cfg.killSwitch = parseBool(val, true); break;
            case "blockscreenlocked": cfg.blockScreenLocked = parseBool(val, false); break;
            case "blockmetered": cfg.blockMetered = parseBool(val, false); break;
            case "blockudpnondns": cfg.blockUdpNonDns = parseBool(val, false); break;
            case "blockbypasseddns": cfg.blockBypassedDns = parseBool(val, true); break;
            case "blocklan": cfg.blockLan = parseBool(val, false); break;
            case "blocknewlyinstalledapps": cfg.blockNewlyInstalledApps = parseBool(val, false); break;

            case "arpspoofingdetection": cfg.arpSpoofingDetection = parseBool(val, false); break;
            case "preventdevicesleep": cfg.preventDeviceSleep = parseBool(val, false); break;
            case "enablescriptcontrol": cfg.enableScriptControl = parseBool(val, false); break;
            case "autostartdnscrypt": cfg.autostartDnscrypt = parseBool(val, false); break;
            case "autostarttor": cfg.autostartTor = parseBool(val, false); break;
            case "autostarti2p": cfg.autostartI2p = parseBool(val, false); break;
            case "autostartvpn": cfg.autostartVpn = parseBool(val, false); break;
            case "autostartdelaysec": cfg.autostartDelaySec = Math.max(0, Math.min(120, parseInt(val, 3))); break;

            // General & Logs / PCAP
            case "rememberuninstalledapps": cfg.rememberUninstalledApps = parseBool(val, false); break;
            case "firewallbubble": cfg.firewallBubble = parseBool(val, false); break;
            case "showproviderinfo": cfg.showProviderInfo = parseBool(val, true); break;
            case "packetcapture":
            case "packetcapturemode": cfg.packetCaptureMode = val; break;
            case "enableondevicelogging": cfg.enableOnDeviceLogging = parseBool(val, true); break;
            case "loglevel": cfg.logLevel = val; break;
            case "loglifespan": cfg.logLifespan = val; break;

            default:
                warnings.add("Line " + lineNo + ": custom key '" + rawKey + "'");
                break;
        }
    }

    public static String serialize(SkidConfig c) {
        StringBuilder sb = new StringBuilder();
        sb.append("# SKIDWAREDNS CUSTOM CONFIG [.skid] v3.0 (MegaForge Edition)\n");
        sb.append("# Profile: ").append(c.name).append("\n\n");

        sb.append("===========\n");
        appendBracket(sb, "DNS Servers", c.dnsServers);
        appendBracket(sb, "VPN Servers", c.vpnServers);
        appendBracket(sb, "Addresses", c.addresses);
        appendBracket(sb, "SOCKS5", c.socks5);
        appendBracket(sb, "VPN PORTS", c.vpnPorts);
        appendBracket(sb, "PROXY PORTS", c.proxyPorts);
        appendBracket(sb, "DNS PORTS", c.dnsPorts);
        appendBracket(sb, "OPEN LISTEN PORTS", c.openListenPorts);
        appendBracket(sb, "Custom DNS Server", c.customDnsServer);
        appendBracket(sb, "Custom VPN Server", c.customVpnServer);
        appendBracket(sb, "Custom Proxy Server", c.customProxyServer);
        appendBracket(sb, "Firewall Rules", c.firewallRules);
        appendBracket(sb, "Blocked Apps", c.blockedApps);
        appendBracket(sb, "Bypass Apps", c.bypassApps);
        appendBracket(sb, "Torify Apps", c.torifyApps);
        appendBracket(sb, "Allowed DNS Records", c.allowedDnsRecords);
        appendBracket(sb, "Blocklist Packs", c.activeBlocklistPacks);
        appendBracket(sb, "LBox Templates", c.lboxTemplates);
        appendBracket(sb, "DNSCrypt Relays", c.dnscryptRelays);
        appendBracket(sb, "Cloaking Rules", c.cloakingRules);
        appendBracket(sb, "Forwarding Rules", c.forwardingRules);
        appendBracket(sb, "Whitelist Domains", c.whitelistDomains);
        appendBracket(sb, "IP Blacklist", c.ipBlacklist);
        appendBracket(sb, "Ignored QTypes", c.ignoredQtypes);
        appendBracket(sb, "Subscription URLs", c.subscriptionUrls);

        sb.append("===========\n");
        sb.append("Tor enabled=").append(c.torEnabled).append('\n');
        sb.append("DNSCrypt-Module-Enabled=").append(c.dnscryptModuleEnabled).append('\n');
        sb.append("I2P-Enabled=").append(c.i2pEnabled).append('\n');
        sb.append("Mode=").append(nullSafe(c.mode)).append('\n');
        sb.append("Name=").append(nullSafe(c.name)).append('\n');
        sb.append("Proxy mode=").append(nullSafe(c.proxyMode)).append('\n');
        sb.append("DNS Mode=").append(nullSafe(c.dnsMode)).append('\n');
        sb.append("Global-DNS-Category=").append(nullSafe(c.globalDnsCategory)).append('\n');
        sb.append("Quad9=").append(nullSafe(c.quad9)).append('\n');
        sb.append("DNSCrypt=").append(nullSafe(c.dnsCrypt)).append('\n');
        sb.append("DNS-over-TLS=").append(nullSafe(c.dnsOverTls)).append('\n');
        sb.append("DNS-Proxy=").append(nullSafe(c.dnsProxy)).append('\n');
        sb.append("Tunnel=").append(nullSafe(c.tunnel)).append('\n');
        sb.append("Connect-from-mode=").append(nullSafe(c.connectFromMode)).append('\n');

        sb.append("===========\n");
        sb.append("# Tunnel Forge, Jump Hosts, Fake-IP, Stack, TLS Fragmentation, DNSCrypt, Tor & I2P\n");
        sb.append("Rethink-DoH=").append(nullSafe(c.rethinkDoh)).append('\n');
        sb.append("Tunnel-Host=").append(nullSafe(c.tunnelHost)).append('\n');
        sb.append("Tunnel-Port=").append(c.tunnelPort).append('\n');
        sb.append("Tunnel-User=").append(nullSafe(c.tunnelUser)).append('\n');
        sb.append("Tunnel-Auth=").append(nullSafe(c.tunnelAuth)).append('\n');
        sb.append("Tunnel-Cipher=").append(nullSafe(c.tunnelCipher)).append('\n');
        sb.append("V2Ray-Transport=").append(nullSafe(c.v2rayTransport)).append('\n');
        sb.append("SNI=").append(nullSafe(c.sni)).append('\n');
        sb.append("Bug-Host=").append(nullSafe(c.bugHost)).append('\n');
        sb.append("HTTP-Payload=").append(nullSafe(c.httpPayload)).append('\n');
        sb.append("DNSTT-Domain=").append(nullSafe(c.dnsttDomain)).append('\n');
        sb.append("DNSTT-PubKey=").append(nullSafe(c.dnsttPubKey)).append('\n');
        sb.append("Hysteria-Version=").append(c.hysteriaVersion).append('\n');
        sb.append("Hysteria-Protocol=").append(nullSafe(c.hysteriaProtocol)).append('\n');
        sb.append("Hysteria-Up-Mbps=").append(c.hysteriaUpMbps).append('\n');
        sb.append("Hysteria-Down-Mbps=").append(c.hysteriaDownMbps).append('\n');
        sb.append("Hysteria-Obfs-Password=").append(nullSafe(c.hysteriaObfsPassword)).append('\n');
        sb.append("Hysteria-Port-Hop-Range=").append(nullSafe(c.hysteriaPortHopRange)).append('\n');
        sb.append("Hysteria-Hop-Interval-Sec=").append(c.hysteriaHopIntervalSec).append('\n');
        sb.append("Hysteria-ALPN=").append(nullSafe(c.hysteriaAlpn)).append('\n');
        sb.append("Hysteria-Allow-Insecure=").append(c.hysteriaAllowInsecure).append('\n');
        sb.append("Hysteria-QUIC-RcvWin-MB=").append(c.hysteriaQuicRcvWinMb).append('\n');
        sb.append("Hysteria-Disable-MTU-Discovery=").append(c.hysteriaDisableMtuDiscovery).append('\n');
        sb.append("Custom-Payload-Enabled=").append(c.customPayloadEnabled).append('\n');
        sb.append("SSH-Use-Private-Key=").append(c.sshUsePrivateKey).append('\n');
        sb.append("SSH-Local-Port=").append(c.sshLocalPort).append('\n');
        sb.append("SSH-UDPGW-Port=").append(c.sshUdpgwPort).append('\n');
        sb.append("SSH-Send-Buffer-Bytes=").append(c.sshSendBufferBytes).append('\n');
        sb.append("SSH-Recv-Buffer-Bytes=").append(c.sshRecvBufferBytes).append('\n');
        sb.append("SSH-Header-Replacer=").append(c.sshHeaderReplacer).append('\n');
        sb.append("SSH-Route-Proxy=").append(c.sshRouteProxy).append('\n');
        sb.append("Xray-Core=").append(nullSafe(c.xrayCore)).append('\n');
        sb.append("Xray-Protocol=").append(nullSafe(c.xrayProtocol)).append('\n');
        sb.append("Xray-UUID=").append(nullSafe(c.xrayUuid)).append('\n');
        sb.append("Xray-Alter-ID=").append(c.xrayAlterId).append('\n');
        sb.append("Xray-Server-Cipher=").append(nullSafe(c.xrayServerCipher)).append('\n');
        sb.append("Xray-Security=").append(nullSafe(c.xraySecurity)).append('\n');
        sb.append("Xray-Network-Type=").append(nullSafe(c.xrayNetworkType)).append('\n');
        sb.append("Xray-Header-Type=").append(nullSafe(c.xrayHeaderType)).append('\n');
        sb.append("Xray-Reality-PubKey=").append(nullSafe(c.xrayRealityPubKey)).append('\n');
        sb.append("Xray-Reality-ShortId=").append(nullSafe(c.xrayRealityShortId)).append('\n');
        sb.append("Xray-Mux-Enabled=").append(c.xrayMuxEnabled).append('\n');
        sb.append("Xray-Domain-Strategy=").append(nullSafe(c.xrayDomainStrategy)).append('\n');
        sb.append("Xray-Routing-Rules=").append(nullSafe(c.xrayRoutingRules)).append('\n');
        sb.append("SS-Obfs-Server=").append(nullSafe(c.ssObfsServer)).append('\n');
        sb.append("Hysteria-Certificates=").append(nullSafe(c.hysteriaCertificates)).append('\n');
        sb.append("Apps-Filter-Enabled=").append(c.appsFilterEnabled).append('\n');
        sb.append("VPN-DNS-Preset=").append(nullSafe(c.vpnDnsPreset)).append('\n');
        sb.append("DNS-Forwarding=").append(c.dnsForwarding).append('\n');
        sb.append("Use-Default-Route=").append(c.useDefaultRoute).append('\n');
        sb.append("Custom-Routes=").append(nullSafe(c.customRoutes)).append('\n');
        sb.append("Excluded-Routes=").append(nullSafe(c.excludedRoutes)).append('\n');
        sb.append("HTTP-Ping-Enabled=").append(c.httpPingEnabled).append('\n');
        sb.append("HTTP-Ping-URL=").append(nullSafe(c.httpPingUrl)).append('\n');
        sb.append("Injector-DoH-Provider=").append(nullSafe(c.injectorDohProvider)).append('\n');
        sb.append("UI-Dark-Mode-System=").append(c.darkModeSystem).append('\n');
        sb.append("UI-Language=").append(nullSafe(c.uiLanguage)).append('\n');
        sb.append("Show-Notification-Icon=").append(c.showNotificationIcon).append('\n');
        sb.append("Vibrate-On-Connect=").append(c.vibrateOnConnect).append('\n');
        sb.append("Nym-Profile=").append(nullSafe(c.nymProfile)).append('\n');
        sb.append("Nym-User-Country=").append(nullSafe(c.nymUserCountry)).append('\n');
        sb.append("Mixnet-Continuous-Traffic=").append(c.mixnetContinuousTraffic).append('\n');
        sb.append("Mixnet-Continuous-Rate=").append(nullSafe(c.mixnetContinuousRate)).append('\n');
        sb.append("Mixnet-Cover-Traffic=").append(c.mixnetCoverTrafficEnabled).append('\n');
        sb.append("Mixnet-Cover-Rate=").append(nullSafe(c.mixnetCoverTrafficRate)).append('\n');
        sb.append("Mixnet-Packet-Mixing-Ms=").append(c.mixnetPacketMixingDelayMs).append('\n');
        sb.append("Nym-QUIC-Enhanced=").append(c.nymQuicEnhanced).append('\n');
        sb.append("AmneziaWG-Minimal-Obfs=").append(c.amneziaWgMinimalObfs).append('\n');
        sb.append("AmneziaWG-Jc=").append(c.amneziaWgJc).append('\n');
        sb.append("AmneziaWG-Jmin=").append(c.amneziaWgJmin).append('\n');
        sb.append("AmneziaWG-Jmax=").append(c.amneziaWgJmax).append('\n');
        sb.append("Nym-Stealth-API-Connect=").append(c.nymStealthApiConnect).append('\n');
        sb.append("Nym-Stealth-Cover-Domains=").append(nullSafe(c.nymStealthCoverDomains)).append('\n');
        sb.append("Mullvad-Enabled=").append(c.mullvadEnabled).append('\n');
        sb.append("Mullvad-Multihop=").append(c.mullvadMultihop).append('\n');
        sb.append("Mullvad-Entry-Relay=").append(nullSafe(c.mullvadEntryRelay)).append('\n');
        sb.append("Mullvad-Exit-Relay=").append(nullSafe(c.mullvadExitRelay)).append('\n');
        sb.append("Mullvad-DAITA=").append(c.mullvadDaita).append('\n');
        sb.append("Mullvad-Use-Socks5=").append(c.mullvadUseSocks5).append('\n');
        sb.append("Mullvad-Owned-Only=").append(c.mullvadOwnedOnly).append('\n');
        sb.append("Mullvad-Bridge-Relay=").append(nullSafe(c.mullvadBridgeRelay)).append('\n');
        sb.append("Mullvad-SSH-SHA256=").append(nullSafe(c.mullvadSshSha256Pin)).append('\n');
        sb.append("Jump-Host-Enabled=").append(c.jumpHostEnabled).append('\n');
        sb.append("Jump-Protocol=").append(nullSafe(c.jumpProtocol)).append('\n');
        sb.append("Jump-Host=").append(nullSafe(c.jumpHost)).append('\n');
        sb.append("Jump-Port=").append(c.jumpPort).append('\n');
        sb.append("Jump-User=").append(nullSafe(c.jumpUser)).append('\n');
        sb.append("Jump-Auth=").append(nullSafe(c.jumpAuth)).append('\n');
        sb.append("Proxy-Knock-Host=").append(nullSafe(c.proxyKnockHost)).append('\n');
        sb.append("JA3-Fingerprint=").append(nullSafe(c.ja3Fingerprint)).append('\n');
        sb.append("App-Ownership-Check=").append(nullSafe(c.appOwnershipCheck)).append('\n');
        sb.append("Fake-IP-Enabled=").append(c.fakeIpEnabled).append('\n');
        sb.append("Routing-Split-Preset=").append(nullSafe(c.routingSplitPreset)).append('\n');
        sb.append("WG-Profile-Name=").append(nullSafe(c.wgProfileName)).append('\n');
        sb.append("WG-Endpoint=").append(nullSafe(c.wgEndpoint)).append('\n');
        sb.append("WG-Enabled=").append(c.wgEnabled).append('\n');
        sb.append("Flood-WireGuard=").append(c.floodWireGuard).append('\n');
        sb.append("Smart-Persistent-Keepalive=").append(c.smartPersistentKeepalive).append('\n');
        sb.append("WireGuard-P2P=").append(c.wireGuardP2p).append('\n');
        sb.append("Fixed-WireGuard-Listen-Port=").append(c.fixedWireGuardListenPort).append('\n');
        sb.append("Loopback-Proxy-Forwarder-Apps=").append(c.loopbackProxyForwarderApps).append('\n');
        sb.append("Proxy-Lockdown=").append(c.proxyLockdown).append('\n');
        sb.append("Strict-Route=").append(c.strictRoute).append('\n');
        sb.append("TUN-Stack=").append(nullSafe(c.tunStack)).append('\n');
        sb.append("Shorter-TCP-Keepalive=").append(c.shorterTcpKeepalive).append('\n');
        sb.append("TCP-NoDelay=").append(c.tcpNoDelay).append('\n');
        sb.append("TCP-FastOpen=").append(c.tcpFastOpen).append('\n');
        sb.append("TCP-RST-QuickClose=").append(c.tcpRstQuickClose).append('\n');
        sb.append("TCP-MSS-Clamp=").append(c.tcpMssClamp).append('\n');
        sb.append("TCP-SYN-Retries=").append(c.tcpSynRetries).append('\n');
        sb.append("DSCP-Traffic-Class=").append(nullSafe(c.dscpTrafficClass)).append('\n');
        sb.append("Endpoint-Independent-Mapping=").append(c.endpointIndependentMapping).append('\n');
        sb.append("Idle-Timeout-Sec=").append(c.idleTimeoutSec).append('\n');
        sb.append("Bandwidth-Booster=").append(c.bandwidthBooster).append('\n');
        sb.append("Socket-Buffer-Size-KB=").append(c.socketBufferSizeKb).append('\n');
        sb.append("IP-Version=").append(nullSafe(c.ipVersion)).append('\n');
        sb.append("Perform-Protocol-Translations=").append(c.performProtocolTranslations).append('\n');
        sb.append("Custom-TUN-Gateway-V4=").append(nullSafe(c.customTunGatewayV4)).append('\n');
        sb.append("Custom-TUN-Gateway-V6=").append(nullSafe(c.customTunGatewayV6)).append('\n');
        sb.append("UDP-QUIC-Policy=").append(nullSafe(c.udpQuicPolicy)).append('\n');
        sb.append("UDP-NAT-Timeout-Sec=").append(c.udpNatTimeoutSec).append('\n');
        sb.append("UDP-Randomize-Source-Port=").append(c.udpRandomizeSourcePort).append('\n');
        sb.append("UDP-Strict-Checksum=").append(c.udpStrictChecksum).append('\n');
        sb.append("UDP-EDNS-Payload-Bytes=").append(c.udpEdnsPayloadBytes).append('\n');
        sb.append("TCP-Keepalive-Idle-Sec=").append(c.tcpKeepaliveIdleSec).append('\n');
        sb.append("TCP-Keepalive-Interval-Sec=").append(c.tcpKeepaliveIntervalSec).append('\n');
        sb.append("TCP-ReuseAddress=").append(c.tcpReuseAddress).append('\n');
        sb.append("TCP-Cork=").append(c.tcpCork).append('\n');
        sb.append("TCP-UserTimeout-Ms=").append(c.tcpUserTimeoutMs).append('\n');
        sb.append("TCP-Congestion=").append(nullSafe(c.tcpCongestion)).append('\n');
        sb.append("TCP-WindowScaling=").append(c.tcpWindowScaling).append('\n');
        sb.append("TCP-ConnectTimeout-Ms=").append(c.tcpConnectTimeoutMs).append('\n');
        sb.append("UDP-RecvTimeout-Ms=").append(c.udpRecvTimeoutMs).append('\n');
        sb.append("UDP-Broadcast=").append(c.udpBroadcast).append('\n');
        sb.append("UDP-ConnectedMode=").append(c.udpConnectedMode).append('\n');
        sb.append("UDP-MaxDatagram-Bytes=").append(c.udpMaxDatagramBytes).append('\n');
        sb.append("UDP-ReuseAddress=").append(c.udpReuseAddress).append('\n');
        sb.append("UDP-GSO-Hint=").append(c.udpGsoHint).append('\n');
        sb.append("HTTPS-MinTLS=").append(nullSafe(c.httpsMinTls)).append('\n');
        sb.append("HTTPS-ALPN=").append(nullSafe(c.httpsAlpn)).append('\n');
        sb.append("HTTPS-SessionTickets=").append(c.httpsSessionTickets).append('\n');
        sb.append("HTTPS-HTTP2=").append(c.httpsHttp2).append('\n');
        sb.append("HTTPS-Timeout-Ms=").append(c.httpsTimeoutMs).append('\n');
        sb.append("HTTPS-EndpointId=").append(c.httpsEndpointId).append('\n');
        sb.append("HTTPS-SNI-Enabled=").append(c.httpsSniEnabled).append('\n');
        sb.append("HTTPS-Pin-SHA256=").append(nullSafe(c.httpsPinSha256)).append('\n');
        sb.append("HTTPS-Fallback-HTTP11=").append(c.httpsFallbackHttp11).append('\n');
        sb.append("Port-Allow-List=").append(nullSafe(c.portAllowList)).append('\n');
        sb.append("Port-Block-List=").append(nullSafe(c.portBlockList)).append('\n');
        sb.append("Port-Forward-Map=").append(nullSafe(c.portForwardMap)).append('\n');
        sb.append("Port-Knock-Sequence=").append(nullSafe(c.portKnockSequence)).append('\n');
        sb.append("Ephemeral-Port-Min=").append(c.ephemeralPortMin).append('\n');
        sb.append("Ephemeral-Port-Max=").append(c.ephemeralPortMax).append('\n');
        sb.append("SO-ReusePort=").append(c.soReusePort).append('\n');
        sb.append("Bind-Loopback-Only=").append(c.bindLoopbackOnly).append('\n');
        sb.append("Privileged-Port-Protect=").append(c.privilegedPortProtect).append('\n');
        sb.append("Port-Hop-Enabled=").append(c.portHopEnabled).append('\n');
        sb.append("Port-Hop-Range=").append(nullSafe(c.portHopRange)).append('\n');
        sb.append("Remap-DNS53-to-DoH=").append(c.remapDns53toDoH).append('\n');
        sb.append("HTTPS-Listen-Port=").append(c.httpsListenPort).append('\n');
        sb.append("Allow-WellKnown-Ports=").append(c.allowWellKnownPorts).append('\n');
        sb.append("Open-Port-Bind-Mode=").append(nullSafe(c.openPortBindMode)).append('\n');
        sb.append("Open-Port-Banner=").append(c.openPortBanner).append('\n');
        sb.append("Open-Port-Hold=").append(c.openPortHold).append('\n');
        sb.append("Tools-Ftps-Port=").append(c.toolsFtpsPort).append('\n');
        sb.append("Tools-Ftps-User=").append(nullSafe(c.toolsFtpsUser)).append('\n');
        sb.append("Tools-Ftps-Pass=").append(nullSafe(c.toolsFtpsPass)).append('\n');
        sb.append("Tools-Scan-Ports=").append(nullSafe(c.toolsScanPorts)).append('\n');
        sb.append("Tools-Check-Host=").append(nullSafe(c.toolsCheckHost)).append('\n');
        sb.append("Tools-Check-From=").append(c.toolsCheckFrom).append('\n');
        sb.append("Tools-Check-To=").append(c.toolsCheckTo).append('\n');
        sb.append("Tools-Check-Proto=").append(nullSafe(c.toolsCheckProto)).append('\n');
        sb.append("Tools-Knock-Host=").append(nullSafe(c.toolsKnockHost)).append('\n');
        sb.append("Tools-Knock-Sequence=").append(nullSafe(c.toolsKnockSequence)).append('\n');
        sb.append("Tools-Knock-Proto=").append(nullSafe(c.toolsKnockProto)).append('\n');
        sb.append("Tools-Knock-Delay-Ms=").append(c.toolsKnockDelayMs).append('\n');
        sb.append("Tools-Ssh-Host=").append(nullSafe(c.toolsSshHost)).append('\n');
        sb.append("Tools-Ssh-Port=").append(c.toolsSshPort).append('\n');
        sb.append("Tools-Ssh-User=").append(nullSafe(c.toolsSshUser)).append('\n');
        sb.append("Tools-Ssh-Pass=").append(nullSafe(c.toolsSshPass)).append('\n');
        sb.append("Tools-Ssh-Command=").append(nullSafe(c.toolsSshCommand)).append('\n');
        sb.append("Tools-Mirror-Src=").append(nullSafe(c.toolsMirrorSrc)).append('\n');
        sb.append("Tools-Mirror-Dst=").append(nullSafe(c.toolsMirrorDst)).append('\n');
        sb.append("DPI-Strategy=").append(nullSafe(c.dpiStrategy)).append('\n');
        sb.append("DPI-Technique=").append(nullSafe(c.dpiTechnique)).append('\n');
        sb.append("TLS-Record-Split-Bytes=").append(c.tlsRecordSplitBytes).append('\n');
        sb.append("TLS-Fragment=").append(c.tlsFragment).append('\n');
        sb.append("TLS-Record-Fragment=").append(c.tlsRecordFragment).append('\n');
        sb.append("TLS-Fallback-Delay-Ms=").append(c.tlsFallbackDelayMs).append('\n');
        sb.append("Mixed-Case-SNI=").append(c.mixedCaseSni).append('\n');
        sb.append("Spoof-SNI-Enabled=").append(c.spoofSniEnabled).append('\n');
        sb.append("Spoof-SNI-Host=").append(nullSafe(c.spoofSniHost)).append('\n');
        sb.append("DNSCrypt-Listen-Port=").append(c.dnscryptListenPort).append('\n');
        sb.append("DNSCrypt-Require-DNSSEC=").append(c.dnscryptRequireDnssec).append('\n');
        sb.append("DNSCrypt-Require-NoLog=").append(c.dnscryptRequireNoLog).append('\n');
        sb.append("DNSCrypt-Require-NoFilter=").append(c.dnscryptRequireNoFilter).append('\n');
        sb.append("DNSCrypt-Force-TCP=").append(c.dnscryptForceTcp).append('\n');
        sb.append("DNSCrypt-Outbound-Proxy=").append(c.dnscryptOutboundProxy).append('\n');
        sb.append("DNSCrypt-Outbound-Proxy-Port=").append(c.dnscryptOutboundProxyPort).append('\n');
        sb.append("DNSCrypt-Ignore-System-DNS=").append(c.dnscryptIgnoreSystemDns).append('\n');
        sb.append("DNSCrypt-HTTP3=").append(c.dnscryptHttp3).append('\n');
        sb.append("DNSCrypt-Block-Unqualified=").append(c.dnscryptBlockUnqualified).append('\n');
        sb.append("DNSCrypt-Block-Undelegated=").append(c.dnscryptBlockUndelegated).append('\n');
        sb.append("DNSCrypt-Block-IPv6=").append(c.dnscryptBlockIpv6).append('\n');
        sb.append("DNSCrypt-Query-Logging=").append(c.dnscryptQueryLogging).append('\n');
        sb.append("DNSCrypt-Suspicious-Logging=").append(c.dnscryptSuspiciousLogging).append('\n');
        sb.append("Tor-Bridge-Type=").append(nullSafe(c.torBridgeType)).append('\n');
        sb.append("Tor-Exit-Nodes=").append(nullSafe(c.torExitNodes)).append('\n');
        sb.append("Tor-Exclude-Exit-Nodes=").append(nullSafe(c.torExcludeExitNodes)).append('\n');
        sb.append("Tor-Entry-Nodes=").append(nullSafe(c.torEntryNodes)).append('\n');
        sb.append("Tor-Strict-Nodes=").append(c.torStrictNodes).append('\n');
        sb.append("Tor-Isolate-UID=").append(c.torIsolateUid).append('\n');
        sb.append("Tor-Isolate-Dest-Addr=").append(c.torIsolateDestAddr).append('\n');
        sb.append("Tor-Isolate-Dest-Port=").append(c.torIsolateDestPort).append('\n');
        sb.append("Tor-SOCKS-Port=").append(c.torSocksPort).append('\n');
        sb.append("Tor-HTTP-Tunnel-Port=").append(c.torHttpTunnelPort).append('\n');
        sb.append("Tor-Trans-Port=").append(c.torTransPort).append('\n');
        sb.append("Tor-DNS-Port=").append(c.torDnsPort).append('\n');
        sb.append("Tor-Snowflake-Rendezvous=").append(nullSafe(c.torSnowflakeRendezvous)).append('\n');
        sb.append("Tor-Fascist-Firewall=").append(c.torFascistFirewall).append('\n');
        sb.append("I2P-Bandwidth-Limit-KBps=").append(c.i2pBandwidthLimitKbps).append('\n');
        sb.append("I2P-Transit-Share-Pct=").append(c.i2pTransitSharePct).append('\n');
        sb.append("I2P-SSU2=").append(c.i2pSsu2).append('\n');
        sb.append("I2P-NTCP2=").append(c.i2pNtcp2).append('\n');
        sb.append("I2P-HTTP-Proxy-Port=").append(c.i2pHttpProxyPort).append('\n');
        sb.append("I2P-SOCKS-Proxy-Port=").append(c.i2pSocksProxyPort).append('\n');
        sb.append("I2P-SAM-Enabled=").append(c.i2pSamEnabled).append('\n');
        sb.append("I2P-SAM-Port=").append(c.i2pSamPort).append('\n');
        sb.append("I2P-AddressBook-URL=").append(nullSafe(c.i2pAddressBookUrl)).append('\n');
        sb.append("DNS-Booster=").append(c.dnsBooster).append('\n');
        sb.append("Never-Proxy-DNS=").append(c.neverProxyDns).append('\n');
        sb.append("Prevent-DNS-Leaks=").append(c.preventDnsLeaks).append('\n');
        sb.append("Block-Unknown-DNS-Source=").append(c.blockUnknownDnsSource).append('\n');
        sb.append("System-DNS-Undelegated=").append(c.systemDnsUndelegated).append('\n');
        sb.append("System-DNS-Captive-Portal=").append(c.systemDnsCaptivePortal).append('\n');
        sb.append("DNS-For-Bypass=").append(nullSafe(c.dnsForBypass)).append('\n');
        sb.append("On-Device-Blocklists-Enabled=").append(c.onDeviceBlocklistsEnabled).append('\n');
        sb.append("Advanced-DNS-Filtering=").append(c.advancedDnsFiltering).append('\n');
        sb.append("Split-DNS=").append(c.splitDns).append('\n');
        sb.append("Loopback-Experimental=").append(c.loopbackExperimental).append('\n');
        sb.append("Bootstrap-DNS=").append(nullSafe(c.bootstrapDns)).append('\n');
        sb.append("Connection-Change-Policy=").append(nullSafe(c.connectionChangePolicy)).append('\n');
        sb.append("Stall-On-Network-Loss=").append(c.stallOnNetworkLoss).append('\n');
        sb.append("Enable-Network-Visibility=").append(c.enableNetworkVisibility).append('\n');
        sb.append("Do-Not-Route-Private-IPs=").append(c.doNotRoutePrivateIps).append('\n');
        sb.append("Use-All-Available-Networks=").append(c.useAllAvailableNetworks).append('\n');
        sb.append("Always-Metered=").append(c.alwaysMetered).append('\n');
        sb.append("Meter-Mobile-Networks=").append(c.meterMobileNetworks).append('\n');
        sb.append("MTU=").append(c.mtu).append('\n');
        sb.append("Kill-Switch=").append(c.killSwitch).append('\n');
        sb.append("Block-Screen-Locked=").append(c.blockScreenLocked).append('\n');
        sb.append("Block-Metered=").append(c.blockMetered).append('\n');
        sb.append("Block-UDP-NonDNS=").append(c.blockUdpNonDns).append('\n');
        sb.append("Block-Bypassed-DNS=").append(c.blockBypassedDns).append('\n');
        sb.append("Block-LAN=").append(c.blockLan).append('\n');
        sb.append("ARP-Spoofing-Detection=").append(c.arpSpoofingDetection).append('\n');
        sb.append("Prevent-Device-Sleep=").append(c.preventDeviceSleep).append('\n');
        sb.append("Enable-Script-Control=").append(c.enableScriptControl).append('\n');
        sb.append("Autostart-DNSCrypt=").append(c.autostartDnscrypt).append('\n');
        sb.append("Autostart-Tor=").append(c.autostartTor).append('\n');
        sb.append("Autostart-I2P=").append(c.autostartI2p).append('\n');
        sb.append("Autostart-VPN=").append(c.autostartVpn).append('\n');
        sb.append("Autostart-Delay-Sec=").append(c.autostartDelaySec).append('\n');
        sb.append("Firewall-Bubble=").append(c.firewallBubble).append('\n');
        sb.append("Show-Provider-Info=").append(c.showProviderInfo).append('\n');
        sb.append("Packet-Capture-Mode=").append(nullSafe(c.packetCaptureMode)).append('\n');
        sb.append("Enable-On-Device-Logging=").append(c.enableOnDeviceLogging).append('\n');
        sb.append("Log-Level=").append(nullSafe(c.logLevel)).append('\n');
        sb.append("Log-Lifespan=").append(nullSafe(c.logLifespan)).append('\n');
        sb.append("===========\n");
        return sb.toString();
    }

    private static void appendBracket(StringBuilder sb, String key, List<String> items) {
        sb.append(key).append(" [");
        if (items != null && !items.isEmpty()) {
            for (int i = 0; i < items.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(items.get(i));
            }
        }
        sb.append("]\n");
    }

    private static List<String> splitItems(String inside) {
        List<String> out = new ArrayList<>();
        if (inside == null || inside.trim().isEmpty()) return out;
        String[] parts = inside.split("[,;\\n]");
        for (String p : parts) {
            String trimmed = p.trim();
            if ((trimmed.startsWith("\"") && trimmed.endsWith("\""))
                    || (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
                trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
            }
            if (!trimmed.isEmpty()) {
                out.add(trimmed);
            }
        }
        return out;
    }

    private static String normalizeKey(String key) {
        if (key == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < key.length(); i++) {
            char ch = key.charAt(i);
            if (Character.isLetterOrDigit(ch)) {
                sb.append(Character.toLowerCase(ch));
            }
        }
        return sb.toString();
    }

    private static boolean parseBool(String v, boolean def) {
        String s = v.trim().toLowerCase(Locale.US);
        if ("true".equals(s) || "1".equals(s) || "yes".equals(s) || "on".equals(s) || "enabled".equals(s)) return true;
        if ("false".equals(s) || "0".equals(s) || "no".equals(s) || "off".equals(s) || "disabled".equals(s)) return false;
        return def;
    }

    private static int parseInt(String v, int def) {
        try {
            return Integer.parseInt(v.trim());
        } catch (Exception e) {
            return def;
        }
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }
}
