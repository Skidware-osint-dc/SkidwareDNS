package com.skidwaredns.app.dns;

import android.content.Context;

import com.skidwaredns.app.config.SkidConfig;
import com.skidwaredns.app.model.DnsPresets;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Live DNSCrypt v3 public-resolvers.md & relays.md downloader/parser,
 * Purple I2P .b32.i2p/hosts.txt addressbook synchronizer, and direct config file manager
 * for dnscrypt-proxy.toml, tor.conf, i2pd.conf, and tunnels.conf.
 */
public final class DnscryptListManager {

    public static final String FILE_DNSCRYPT_TOML = "dnscrypt-proxy.toml";
    public static final String FILE_TOR_CONF = "tor.conf";
    public static final String FILE_I2PD_CONF = "i2pd.conf";
    public static final String FILE_TUNNELS_CONF = "tunnels.conf";

    public static int getI2pHostCount() {
        return I2P_HOSTS_MAP.size();
    }

    public static void clearModuleCache(Context ctx, String module) {
        if ("dnscrypt".equalsIgnoreCase(module)) {
            cachedResolvers = Collections.emptyList();
            cachedRelays = Collections.emptyList();
        } else if ("i2p".equalsIgnoreCase(module)) {
            I2P_HOSTS_MAP.clear();
            I2P_HOSTS_MAP.put("stats.i2p", "127.0.0.1");
            I2P_HOSTS_MAP.put("reg.i2p", "127.0.0.1");
            I2P_HOSTS_MAP.put("identiguy.i2p", "127.0.0.1");
            I2P_HOSTS_MAP.put("notbob.i2p", "127.0.0.1");
        }
        if (ctx != null && module != null) {
            File f = new File(ctx.getFilesDir(), module + ".cache");
            if (f.exists()) {
                //noinspection ResultOfMethodCallIgnored
                f.delete();
            }
        }
    }

    public static final class RemoteStampEntry {
        public final String name;
        public final String description;
        public final String stamp;
        public final DnsPresets.DecodedStamp decoded;

        public RemoteStampEntry(String name, String description, String stamp, DnsPresets.DecodedStamp decoded) {
            this.name = name;
            this.description = description;
            this.stamp = stamp;
            this.decoded = decoded;
        }
    }

    private static volatile List<RemoteStampEntry> cachedResolvers = Collections.emptyList();
    private static volatile List<RemoteStampEntry> cachedRelays = Collections.emptyList();
    private static final Map<String, String> I2P_HOSTS_MAP = new ConcurrentHashMap<>();

    static {
        // Seed popular I2P eepsites so .i2p / .b32.i2p cloaking works immediately
        I2P_HOSTS_MAP.put("stats.i2p", "127.0.0.1");
        I2P_HOSTS_MAP.put("reg.i2p", "127.0.0.1");
        I2P_HOSTS_MAP.put("identiguy.i2p", "127.0.0.1");
        I2P_HOSTS_MAP.put("notbob.i2p", "127.0.0.1");
        I2P_HOSTS_MAP.put("shx5vqsw7usdaunyzr2qmes2fq37oumybpudrd4jjj4e4vk4uusa.b32.i2p", "127.0.0.1");
    }

    private DnscryptListManager() {}

    public static List<RemoteStampEntry> getCachedResolvers() {
        return cachedResolvers;
    }

    public static List<RemoteStampEntry> getCachedRelays() {
        return cachedRelays;
    }

    public static String lookupI2pHost(String domain) {
        if (domain == null) return null;
        String d = domain.toLowerCase();
        if (I2P_HOSTS_MAP.containsKey(d)) return I2P_HOSTS_MAP.get(d);
        if (d.endsWith(".b32.i2p") || d.endsWith(".i2p")) {
            return "127.0.0.1";
        }
        return null;
    }

    public static List<RemoteStampEntry> fetchPublicResolvers(SkidConfig cfg) throws Exception {
        Exception lastErr = null;
        for (String url : DnsPresets.DNSCRYPT_RESOLVER_SOURCES) {
            try {
                String md = downloadText(url, null, 7000);
                List<RemoteStampEntry> parsed = parseMarkdownStamps(md, cfg, false);
                if (!parsed.isEmpty()) {
                    cachedResolvers = parsed;
                    return parsed;
                }
            } catch (Exception e) {
                lastErr = e;
            }
        }
        if (lastErr != null) throw lastErr;
        return Collections.emptyList();
    }

    public static List<RemoteStampEntry> fetchRelays() throws Exception {
        Exception lastErr = null;
        for (String url : DnsPresets.DNSCRYPT_RELAY_SOURCES) {
            try {
                String md = downloadText(url, null, 7000);
                List<RemoteStampEntry> parsed = parseMarkdownStamps(md, null, true);
                if (!parsed.isEmpty()) {
                    cachedRelays = parsed;
                    return parsed;
                }
            } catch (Exception e) {
                lastErr = e;
            }
        }
        if (lastErr != null) throw lastErr;
        return Collections.emptyList();
    }

    /**
     * Parses DNSCrypt v3 Markdown resolver/relay catalogs (## name ... sdns://...).
     */
    public static List<RemoteStampEntry> parseMarkdownStamps(String markdown, SkidConfig filterCfg, boolean relaysOnly) {
        List<RemoteStampEntry> out = new ArrayList<>();
        if (markdown == null || markdown.isEmpty()) return out;

        String[] lines = markdown.replace("\r\n", "\n").split("\n");
        String currentName = null;
        StringBuilder descBuf = new StringBuilder();

        for (String raw : lines) {
            String line = raw.trim();
            if (line.startsWith("## ")) {
                currentName = line.substring(3).trim();
                descBuf.setLength(0);
            } else if (line.startsWith("sdns://") && currentName != null) {
                DnsPresets.DecodedStamp dec = DnsPresets.decodeSdnsStamp(line);
                if (dec.valid && matchesFilter(dec, filterCfg, relaysOnly)) {
                    String desc = descBuf.length() > 120 ? descBuf.substring(0, 120) + "..." : descBuf.toString();
                    out.add(new RemoteStampEntry(currentName, desc.trim(), line, dec));
                }
            } else if (currentName != null && !line.isEmpty()) {
                if (descBuf.length() > 0) descBuf.append(' ');
                descBuf.append(line);
            }
        }
        return out;
    }

    private static boolean matchesFilter(DnsPresets.DecodedStamp d, SkidConfig cfg, boolean relaysOnly) {
        if (relaysOnly) {
            return d.protoId == 0x81 || d.protoId == 0x85;
        }
        if (cfg == null) return true;
        if (d.protoId == 0x01 && !cfg.dnscryptEnableProto) return false;
        if (d.protoId == 0x02 && !cfg.dnscryptEnableDoh) return false;
        if (d.protoId == 0x05 && !cfg.dnscryptEnableOdoh) return false;
        boolean isV6 = d.serverAddr.contains("[") || (d.host != null && d.host.contains(":"));
        if (isV6 && !cfg.dnscryptIpv6Servers) return false;
        if (!isV6 && !cfg.dnscryptIpv4Servers) return false;
        if (cfg.dnscryptRequireDnssec && !d.dnssec) return false;
        if (cfg.dnscryptRequireNoLog && !d.noLog) return false;
        if (cfg.dnscryptRequireNoFilter && !d.noFilter) return false;
        return true;
    }

    /**
     * Fetches I2P hosts.txt (automatically routing via local I2P HTTP proxy 127.0.0.1:4444 for .i2p URLs).
     */
    public static int syncI2pAddressBook(SkidConfig cfg) throws Exception {
        String url = (cfg != null && cfg.i2pAddressBookUrl != null && !cfg.i2pAddressBookUrl.isEmpty())
                ? cfg.i2pAddressBookUrl.trim() : DnsPresets.DEFAULT_I2P_HOSTS_URL;
        Proxy proxy = null;
        if (url.contains(".i2p")) {
            int port = cfg != null ? cfg.i2pHttpProxyPort : 4444;
            proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", port));
        }
        String body = downloadText(url, proxy, 8000);
        int count = 0;
        for (String line : body.split("\n")) {
            String t = line.trim();
            if (t.isEmpty() || t.startsWith("#")) continue;
            int eq = t.indexOf('=');
            if (eq > 0) {
                String host = t.substring(0, eq).trim().toLowerCase();
                I2P_HOSTS_MAP.put(host, "127.0.0.1");
                count++;
            } else {
                String[] parts = t.split("\\s+");
                if (parts.length >= 2) {
                    I2P_HOSTS_MAP.put(parts[1].trim().toLowerCase(), parts[0].trim());
                    count++;
                }
            }
        }
        return count;
    }

    public static String downloadText(String urlStr, Proxy proxy, int timeoutMs) throws Exception {
        URL u = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) (proxy != null ? u.openConnection(proxy) : u.openConnection());
        conn.setConnectTimeout(timeoutMs);
        conn.setReadTimeout(timeoutMs);
        conn.setRequestProperty("User-Agent", "SkidwareDNS/3.0");
        try (InputStream is = conn.getInputStream()) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) != -1) {
                baos.write(buf, 0, n);
                if (baos.size() > 2_000_000) break;
            }
            return new String(baos.toByteArray(), StandardCharsets.UTF_8);
        } finally {
            conn.disconnect();
        }
    }

    // === Direct Raw Config File Generators & Editors (InviZible Pro compatibility) ===

    public static String loadOrGenerateModuleFile(Context ctx, String fileName, SkidConfig cfg) {
        if (ctx != null) {
            File f = new File(ctx.getFilesDir(), fileName);
            if (f.exists() && f.length() > 0) {
                try (FileInputStream fis = new FileInputStream(f)) {
                    byte[] b = new byte[(int) f.length()];
                    int r = fis.read(b);
                    if (r > 0) return new String(b, 0, r, StandardCharsets.UTF_8);
                } catch (Exception ignored) {
                }
            }
        }
        if ("dnscrypt-proxy.toml".equals(fileName)) return generateDnscryptToml(cfg);
        if ("tor.conf".equals(fileName)) return generateTorConf(cfg);
        if ("i2pd.conf".equals(fileName)) return generateI2pdConf(cfg);
        return generateTunnelsConf(cfg);
    }

    public static void saveModuleFile(Context ctx, String fileName, String content) {
        if (ctx == null) return;
        File f = new File(ctx.getFilesDir(), fileName);
        try (FileOutputStream fos = new FileOutputStream(f)) {
            fos.write(content.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {
        }
    }

    public static String generateDnscryptToml(SkidConfig c) {
        return "# SkidwareDNS dnscrypt-proxy.toml\n" +
                "listen_addresses = ['127.0.0.1:" + c.dnscryptListenPort + "']\n" +
                "dnscrypt_servers = " + c.dnscryptEnableProto + "\n" +
                "doh_servers = " + c.dnscryptEnableDoh + "\n" +
                "odoh_servers = " + c.dnscryptEnableOdoh + "\n" +
                "ipv4_servers = " + c.dnscryptIpv4Servers + "\n" +
                "ipv6_servers = " + c.dnscryptIpv6Servers + "\n" +
                "require_dnssec = " + c.dnscryptRequireDnssec + "\n" +
                "require_nolog = " + c.dnscryptRequireNoLog + "\n" +
                "require_nofilter = " + c.dnscryptRequireNoFilter + "\n" +
                "force_tcp = " + c.dnscryptForceTcp + "\n" +
                "http3 = " + c.dnscryptHttp3 + "\n" +
                "ignore_system_dns = " + c.dnscryptIgnoreSystemDns + "\n" +
                "block_unqualified = " + c.dnscryptBlockUnqualified + "\n" +
                "block_undelegated = " + c.dnscryptBlockUndelegated + "\n" +
                "block_ipv6 = " + c.dnscryptBlockIpv6 + "\n" +
                "bootstrap_resolvers = ['" + c.bootstrapDns + "']\n" +
                (c.dnscryptOutboundProxy ? "proxy = 'socks5://127.0.0.1:" + c.dnscryptOutboundProxyPort + "'\n" : "") +
                "\n[sources.'public-resolvers']\n" +
                "urls = ['" + DnsPresets.DNSCRYPT_RESOLVER_SOURCES[0] + "', '" + DnsPresets.DNSCRYPT_RESOLVER_SOURCES[1] + "', '" + DnsPresets.DNSCRYPT_RESOLVER_SOURCES[2] + "']\n" +
                "minisign_key = 'RWQf6LRCGA9i53mlYecO4IzT51TGPpvWucNSCh1CBM0QTaLn73Y7GFO3'\n" +
                "\n[sources.'relays']\n" +
                "urls = ['" + DnsPresets.DNSCRYPT_RELAY_SOURCES[0] + "', '" + DnsPresets.DNSCRYPT_RELAY_SOURCES[1] + "', '" + DnsPresets.DNSCRYPT_RELAY_SOURCES[2] + "']\n" +
                "minisign_key = 'RWQf6LRCGA9i53mlYecO4IzT51TGPpvWucNSCh1CBM0QTaLn73Y7GFO3'\n";
    }

    public static String generateTorConf(SkidConfig c) {
        String iso = (c.torIsolateUid ? " IsolateClientAddr" : "")
                + (c.torIsolateDestAddr ? " IsolateDestAddr" : "")
                + (c.torIsolateDestPort ? " IsolateDestPort" : "");
        return "# SkidwareDNS tor.conf (torrc)\n" +
                "VirtualAddrNetworkIPv4 " + c.torVirtualAddrNetwork + "\n" +
                "AutomapHostsOnResolve 1\n" +
                "SOCKSPort 127.0.0.1:" + c.torSocksPort + iso + "\n" +
                (c.torHttpTunnelEnabled ? "HTTPTunnelPort 127.0.0.1:" + c.torHttpTunnelPort + "\n" : "") +
                (c.torTransProxyEnabled ? "TransPort 127.0.0.1:" + c.torTransPort + "\n" : "") +
                (c.torResolveDns ? "DNSPort 127.0.0.1:" + c.torDnsPort + "\n" : "") +
                "ClientUseIPv4 " + (c.torClientUseIpv4 ? 1 : 0) + "\n" +
                "ClientUseIPv6 " + (c.torClientUseIpv6 ? 1 : 0) + "\n" +
                "EnforceDistinctSubnets " + (c.torEnforceDistinctSubnets ? 1 : 0) + "\n" +
                "HardwareAccel " + (c.torHardwareAccel ? 1 : 0) + "\n" +
                "AvoidDiskWrites " + (c.torAvoidDiskWrites ? 1 : 0) + "\n" +
                "ConnectionPadding " + (c.torConnectionPadding ? 1 : 0) + "\n" +
                "ReducedConnectionPadding " + (c.torReducedConnectionPadding ? 1 : 0) + "\n" +
                "FascistFirewall " + (c.torFascistFirewall ? 1 : 0) + "\n" +
                "NewCircuitPeriod " + c.torNewCircuitPeriodSec + "\n" +
                "MaxCircuitDirtiness " + c.torMaxCircuitDirtinessSec + "\n" +
                "DormantClientTimeout " + c.torDormantClientTimeoutMin + " minutes\n" +
                (!c.torExitNodes.isEmpty() ? "ExitNodes " + c.torExitNodes + "\n" : "") +
                (!c.torExcludeExitNodes.isEmpty() ? "ExcludeExitNodes " + c.torExcludeExitNodes + "\n" : "") +
                (!c.torEntryNodes.isEmpty() ? "EntryNodes " + c.torEntryNodes + "\n" : "") +
                "StrictNodes " + (c.torStrictNodes ? 1 : 0) + "\n";
    }

    public static String generateI2pdConf(SkidConfig c) {
        return "# SkidwareDNS Purple I2P i2pd.conf\n" +
                "ipv6 = " + c.i2pIpv6 + "\n" +
                "notransit = " + c.i2pNoTransit + "\n" +
                "floodfill = " + c.i2pFloodfill + "\n" +
                "bandwidth = " + c.i2pBandwidthLimitKbps + "\n" +
                "share = " + c.i2pTransitSharePct + "\n" +
                "ssu2 = " + c.i2pSsu2 + "\n" +
                "ntcp2 = " + c.i2pNtcp2 + "\n" +
                "\n[httpproxy]\n" +
                "enabled = " + c.i2pHttpProxyEnabled + "\n" +
                "port = " + c.i2pHttpProxyPort + "\n" +
                (c.i2pHttpOutproxyEnabled ? "outproxy = " + c.i2pHttpOutproxyAddr + "\n" : "") +
                "\n[socksproxy]\n" +
                "enabled = " + c.i2pSocksProxyEnabled + "\n" +
                "port = " + c.i2pSocksProxyPort + "\n" +
                "\n[sam]\n" +
                "enabled = " + c.i2pSamEnabled + "\n" +
                "port = " + c.i2pSamPort + "\n" +
                "\n[addressbook]\n" +
                "defaulturl = " + c.i2pAddressBookUrl + "\n" +
                "subscriptions = " + DnsPresets.DEFAULT_I2P_HOSTS_URL + "\n" +
                "\n[limits]\n" +
                "transittunnels = " + c.i2pMaxTransitTunnels + "\n";
    }

    public static String generateTunnelsConf(SkidConfig c) {
        return "# SkidwareDNS Purple I2P tunnels.conf\n" +
                "[skid-irc]\n" +
                "type = client\n" +
                "address = 127.0.0.1\n" +
                "port = 6668\n" +
                "destination = irc.postman.i2p\n" +
                "destinationport = 6667\n";
    }
}
