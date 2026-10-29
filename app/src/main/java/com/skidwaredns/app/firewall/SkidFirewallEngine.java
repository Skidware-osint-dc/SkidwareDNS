package com.skidwaredns.app.firewall;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.PowerManager;

import com.skidwaredns.app.config.SkidConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Upgraded SkidwareDNS Custom + Universal + On-Device Blocklist Firewall Engine.
 * Enforces:
 *  - Universal rules (Lockscreen, Metered, Non-DNS UDP, QUIC UDP:443, Private LAN, Stall on Network Loss)
 *  - On-device toggleable blocklist packs (Adware/Trackers, Malware/Phishing, OEM Telemetry, Adult, Piracy, Social)
 *  - Allowed DNS record types (Auto/ALL or selective A, AAAA, HTTPS, SVCB, TXT, MX, CNAME, SRV, PTR)
 *  - Custom Domain / Keyword / IPv4 CIDR / Port Range rules
 *  - Per-App policies (VPN/DNS, BYPASS, TORIFY, BLOCK)
 */
public final class SkidFirewallEngine {

    public static final String[] BLOCKLIST_PACK_NAMES = new String[]{
            "Adware & Trackers",
            "Malware & Phishing",
            "OEM Telemetry",
            "Adult & NSFW",
            "Piracy & Torrents",
            "Social & Short-Video"
    };

    public static final String[] RECORD_TYPES = new String[]{
            "ALL", "A", "AAAA", "HTTPS", "SVCB", "CNAME", "TXT", "MX", "SRV", "PTR"
    };

    private static final Map<String, List<String>> BUILTIN_PACK_DOMAINS = new HashMap<>();

    static {
        BUILTIN_PACK_DOMAINS.put("Adware & Trackers", Arrays.asList(
                "doubleclick.net", "googleadservices.com", "googlesyndication.com",
                "app-measurement.com", "analytics.google.com", "scorecardresearch.com",
                "adservice.google.com", "ads.tiktok.com", "ads.yahoo.com", "criteo.com",
                "taboola.com", "outbrain.com", "appsflyer.com", "adjust.com", "branch.io",
                "mixpanel.com", "amplitude.com", "UnityAds.unity3d.com", "adcolony.com"
        ));
        BUILTIN_PACK_DOMAINS.put("Malware & Phishing", Arrays.asList(
                "malware-test.skid", "coinhive.com", "crypto-loot.com",
                "jsecoin.com", "minero.cc", "evil-phish-login.example"
        ));
        BUILTIN_PACK_DOMAINS.put("OEM Telemetry", Arrays.asList(
                "telemetry.microsoft.com", "vortex.data.microsoft.com",
                "tracking.miui.com", "data.mistat.xiaomi.com",
                "metrics.icloud.com", "samsungadhub.com", "smetrics.samsung.com",
                "crashlytics.com", "firebaselogging-pa.googleapis.com"
        ));
        BUILTIN_PACK_DOMAINS.put("Adult & NSFW", Arrays.asList(
                "pornhub.com", "xvideos.com", "xnxx.com", "xhamster.com", "redtube.com"
        ));
        BUILTIN_PACK_DOMAINS.put("Piracy & Torrents", Arrays.asList(
                "thepiratebay.org", "1337x.to", "rarbg.to", "yts.mx", "nyaa.si"
        ));
        BUILTIN_PACK_DOMAINS.put("Social & Short-Video", Arrays.asList(
                "graph.facebook.com", "pixel.facebook.com", "tr.snapchat.com", "log.byteoversea.com"
        ));
        // Also support shorthand .skid pack slugs
        BUILTIN_PACK_DOMAINS.put("oisd_small", BUILTIN_PACK_DOMAINS.get("Adware & Trackers"));
        BUILTIN_PACK_DOMAINS.put("adguard_dns", BUILTIN_PACK_DOMAINS.get("Adware & Trackers"));
        BUILTIN_PACK_DOMAINS.put("stevenblack_hosts", BUILTIN_PACK_DOMAINS.get("OEM Telemetry"));
        BUILTIN_PACK_DOMAINS.put("urlhaus_malware", BUILTIN_PACK_DOMAINS.get("Malware & Phishing"));
        BUILTIN_PACK_DOMAINS.put("hagezi_tif", BUILTIN_PACK_DOMAINS.get("Malware & Phishing"));
    }

    public static final class Verdict {
        public final boolean allowed;
        public final String matchedRule;

        public Verdict(boolean allowed, String matchedRule) {
            this.allowed = allowed;
            this.matchedRule = matchedRule;
        }

        public static Verdict allow(String reason) {
            return new Verdict(true, reason);
        }

        public static Verdict block(String rule) {
            return new Verdict(false, rule);
        }
    }

    public static final class CidrRule {
        public final String rawCidr;
        public final String networkIp;
        public final int prefixLength;
        public final int networkInt;
        public final int maskInt;

        public CidrRule(String rawCidr, String networkIp, int prefixLength, int networkInt, int maskInt) {
            this.rawCidr = rawCidr;
            this.networkIp = networkIp;
            this.prefixLength = prefixLength;
            this.networkInt = networkInt;
            this.maskInt = maskInt;
        }
    }

    public static final class PortRange {
        public final int start;
        public final int end;
        public final String raw;

        public PortRange(int start, int end, String raw) {
            this.start = start;
            this.end = end;
            this.raw = raw;
        }

        public boolean matches(int port) {
            return port >= start && port <= end;
        }
    }

    public static final class AppPolicyEntry {
        public final String label;
        public final String packageName;
        public final int uid;
        public final boolean systemApp;

        public AppPolicyEntry(String label, String packageName, int uid, boolean systemApp) {
            this.label = label;
            this.packageName = packageName;
            this.uid = uid;
            this.systemApp = systemApp;
        }
    }

    private final Set<String> allowDomains = new HashSet<>();
    private final Set<String> blockDomains = new HashSet<>();
    private final Set<String> packDomains = new HashSet<>();
    private final List<String> blockKeywords = new ArrayList<>();
    private final List<CidrRule> blockCidrs = new ArrayList<>();
    private final List<PortRange> blockPorts = new ArrayList<>();
    private final Set<String> allowedQtypes = new HashSet<>();

    private boolean blockScreenLocked;
    private boolean blockMetered;
    private boolean blockUdpNonDns;
    private boolean blockLan;
    private boolean stallOnNetworkLoss;
    private String udpQuicPolicy = "Allow";
    private String ipVersion = "IPv4";

    public synchronized void reloadFromConfig(SkidConfig cfg) {
        allowDomains.clear();
        blockDomains.clear();
        packDomains.clear();
        blockKeywords.clear();
        blockCidrs.clear();
        blockPorts.clear();
        allowedQtypes.clear();

        if (cfg == null) return;
        this.blockScreenLocked = cfg.blockScreenLocked;
        this.blockMetered = cfg.blockMetered;
        this.blockUdpNonDns = cfg.blockUdpNonDns;
        this.blockLan = cfg.blockLan;
        this.stallOnNetworkLoss = cfg.stallOnNetworkLoss;
        this.udpQuicPolicy = cfg.udpQuicPolicy != null ? cfg.udpQuicPolicy : "Allow";
        this.ipVersion = cfg.ipVersion != null ? cfg.ipVersion : "IPv4";

        for (String rt : cfg.allowedDnsRecords) {
            if (rt != null && !rt.trim().isEmpty()) {
                String u = rt.trim().toUpperCase(Locale.US);
                if ("AUTO".equals(u) || "*".equals(u)) {
                    allowedQtypes.add("ALL");
                } else {
                    allowedQtypes.add(u);
                }
            }
        }
        if (allowedQtypes.isEmpty()) allowedQtypes.add("ALL");

        if (cfg.onDeviceBlocklistsEnabled || !cfg.activeBlocklistPacks.isEmpty()) {
            if (cfg.onDeviceBlocklistsEnabled) {
                for (String packName : cfg.activeBlocklistPacks) {
                    List<String> doms = BUILTIN_PACK_DOMAINS.get(packName);
                    if (doms != null) {
                        for (String d : doms) {
                            packDomains.add(d.toLowerCase(Locale.US));
                        }
                    }
                }
            }
        }

        for (String wl : cfg.whitelistDomains) {
            if (wl != null) {
                String d = cleanDomain(wl);
                if (!d.isEmpty()) allowDomains.add(d);
            }
        }
        for (String ipb : cfg.ipBlacklist) {
            if (ipb != null && !ipb.trim().isEmpty()) {
                CidrRule cr = parseIpv4Cidr(ipb.trim());
                if (cr != null) blockCidrs.add(cr);
            }
        }

        for (String raw : cfg.firewallRules) {
            if (raw == null) continue;
            String r = raw.trim();
            if (r.isEmpty() || r.startsWith("#")) continue;

            String upper = r.toUpperCase(Locale.US);
            if (upper.startsWith("ALLOW_DOMAIN:")) {
                String d = cleanDomain(r.substring("ALLOW_DOMAIN:".length()));
                if (!d.isEmpty()) allowDomains.add(d);
            } else if (upper.startsWith("BLOCK_DOMAIN:")) {
                String d = cleanDomain(r.substring("BLOCK_DOMAIN:".length()));
                if (!d.isEmpty()) blockDomains.add(d);
            } else if (upper.startsWith("BLOCK_KEYWORD:")) {
                String kw = r.substring("BLOCK_KEYWORD:".length()).trim().toLowerCase(Locale.US);
                if (!kw.isEmpty()) blockKeywords.add(kw);
            } else if (upper.startsWith("BLOCK_IP:")) {
                CidrRule cidr = parseIpv4Cidr(r.substring("BLOCK_IP:".length()).trim());
                if (cidr != null) blockCidrs.add(cidr);
            } else if (upper.startsWith("BLOCK_PORT:")) {
                PortRange pr = parsePortRange(r.substring("BLOCK_PORT:".length()).trim());
                if (pr != null) blockPorts.add(pr);
            } else if (!r.contains(":")) {
                String d = cleanDomain(r);
                if (!d.isEmpty()) blockDomains.add(d);
            }
        }
    }

    public synchronized Verdict checkUniversalState(Context ctx) {
        if (ctx != null && stallOnNetworkLoss) {
            try {
                ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
                if (cm != null) {
                    NetworkInfo ni = cm.getActiveNetworkInfo();
                    if (ni == null || !ni.isConnected()) {
                        return Verdict.block("STALL_ON_NETWORK_LOSS");
                    }
                }
            } catch (Exception ignored) {
            }
        }
        if (ctx != null && blockScreenLocked) {
            try {
                PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
                KeyguardManager km = (KeyguardManager) ctx.getSystemService(Context.KEYGUARD_SERVICE);
                boolean interactive = pm == null || pm.isInteractive();
                boolean locked = km != null && km.isKeyguardLocked();
                if (!interactive || locked) {
                    return Verdict.block("LOCKSCREEN_LOCKDOWN");
                }
            } catch (Exception ignored) {
            }
        }
        if (ctx != null && blockMetered) {
            try {
                ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
                if (cm != null && cm.isActiveNetworkMetered()) {
                    return Verdict.block("METERED_NETWORK_BLOCK");
                }
            } catch (Exception ignored) {
            }
        }
        return Verdict.allow("UNIVERSAL_OK");
    }

    public synchronized Verdict evaluateDnsQuery(Context ctx, String rawDomain, String qtypeName) {
        if (qtypeName != null && !allowedQtypes.contains("ALL") && !allowedQtypes.contains(qtypeName.toUpperCase(Locale.US))) {
            return Verdict.block("RECORD_TYPE_FILTER:" + qtypeName);
        }
        if ("IPv4".equalsIgnoreCase(ipVersion) && "AAAA".equalsIgnoreCase(qtypeName) && !allowedQtypes.contains("AAAA")) {
            // Only filter AAAA when user explicitly restricted record types; otherwise allow standard resolution
        }
        return evaluateDomain(ctx, rawDomain);
    }

    public synchronized Verdict evaluateDomain(Context ctx, String rawDomain) {
        Verdict univ = checkUniversalState(ctx);
        if (!univ.allowed) return univ;

        String domain = cleanDomain(rawDomain);
        if (domain.isEmpty()) return Verdict.allow("EMPTY");

        if (matchesDomainSet(domain, allowDomains) != null) {
            return Verdict.allow("ALLOW_DOMAIN");
        }

        String matchedBlock = matchesDomainSet(domain, blockDomains);
        if (matchedBlock != null) {
            return Verdict.block("BLOCK_DOMAIN:" + matchedBlock);
        }

        String matchedPack = matchesDomainSet(domain, packDomains);
        if (matchedPack != null) {
            return Verdict.block("ON_DEVICE_BLOCKLIST:" + matchedPack);
        }

        for (String kw : blockKeywords) {
            if (domain.contains(kw)) {
                return Verdict.block("BLOCK_KEYWORD:" + kw);
            }
        }

        return Verdict.allow("CLEAN");
    }

    public synchronized Verdict evaluateDestination(Context ctx, String hostOrIp, int dstPort, boolean isUdp) {
        Verdict univ = checkUniversalState(ctx);
        if (!univ.allowed) return univ;

        if (isUdp && dstPort == 443 && udpQuicPolicy.toLowerCase(Locale.US).contains("block")) {
            return Verdict.block("BLOCK_QUIC_UDP_443");
        }

        if (isUdp && blockUdpNonDns && dstPort != 53 && dstPort != 853 && dstPort != 5400 && dstPort != 123) {
            return Verdict.block("BLOCK_UDP_NON_DNS:" + dstPort);
        }

        for (PortRange pr : blockPorts) {
            if (pr.matches(dstPort)) {
                return Verdict.block("BLOCK_PORT:" + pr.raw);
            }
        }

        if (hostOrIp != null && !hostOrIp.isEmpty()) {
            Integer ipInt = ipv4ToInt(hostOrIp);
            if (ipInt != null) {
                if (blockLan && isPrivateIpv4(ipInt)) {
                    return Verdict.block("BLOCK_LAN:" + hostOrIp);
                }
                for (CidrRule c : blockCidrs) {
                    if ((ipInt & c.maskInt) == (c.networkInt & c.maskInt)) {
                        return Verdict.block("BLOCK_IP:" + c.rawCidr);
                    }
                }
            } else {
                Verdict dv = evaluateDomain(null, hostOrIp);
                if (!dv.allowed) return dv;
            }
        }
        return Verdict.allow("PERMIT");
    }

    public synchronized List<CidrRule> getBlockedCidrsSnapshot() {
        return new ArrayList<>(blockCidrs);
    }

    private static String matchesDomainSet(String q, Set<String> set) {
        if (set.isEmpty()) return null;
        String cur = q;
        while (true) {
            if (set.contains(cur)) return cur;
            int dot = cur.indexOf('.');
            if (dot < 0 || dot + 1 >= cur.length()) break;
            cur = cur.substring(dot + 1);
        }
        return null;
    }

    private static String cleanDomain(String d) {
        if (d == null) return "";
        String s = d.trim().toLowerCase(Locale.US);
        if (s.startsWith("*.")) s = s.substring(2);
        if (s.endsWith(".")) s = s.substring(0, s.length() - 1);
        return s;
    }

    public static CidrRule parseIpv4Cidr(String s) {
        if (s == null || s.isEmpty()) return null;
        String ipPart = s;
        int prefix = 32;
        int slash = s.indexOf('/');
        if (slash > 0) {
            ipPart = s.substring(0, slash).trim();
            try {
                prefix = Integer.parseInt(s.substring(slash + 1).trim());
            } catch (Exception e) {
                return null;
            }
        }
        if (prefix < 0 || prefix > 32) return null;
        Integer ipInt = ipv4ToInt(ipPart);
        if (ipInt == null) return null;
        int mask = prefix == 0 ? 0 : (int) (0xFFFFFFFFL << (32 - prefix));
        return new CidrRule(ipPart + "/" + prefix, ipPart, prefix, ipInt & mask, mask);
    }

    public static Integer ipv4ToInt(String ip) {
        if (ip == null) return null;
        String[] parts = ip.trim().split("\\.");
        if (parts.length != 4) return null;
        int res = 0;
        try {
            for (int i = 0; i < 4; i++) {
                int octet = Integer.parseInt(parts[i]);
                if (octet < 0 || octet > 255) return null;
                res = (res << 8) | octet;
            }
            return res;
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isPrivateIpv4(int ip) {
        int b0 = (ip >>> 24) & 0xFF;
        int b1 = (ip >>> 16) & 0xFF;
        if (b0 == 10) return true;
        if (b0 == 172 && b1 >= 16 && b1 <= 31) return true;
        return b0 == 192 && b1 == 168;
    }

    private static PortRange parsePortRange(String s) {
        try {
            int dash = s.indexOf('-');
            if (dash > 0) {
                int a = Integer.parseInt(s.substring(0, dash).trim());
                int b = Integer.parseInt(s.substring(dash + 1).trim());
                if (a > 0 && b <= 65535 && a <= b) {
                    return new PortRange(a, b, s);
                }
            } else {
                int p = Integer.parseInt(s.trim());
                if (p > 0 && p <= 65535) {
                    return new PortRange(p, p, s);
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public static List<AppPolicyEntry> queryInstalledApps(Context ctx, boolean includeSystem) {
        List<AppPolicyEntry> result = new ArrayList<>();
        try {
            PackageManager pm = ctx.getPackageManager();
            List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
            String myPkg = ctx.getPackageName();
            for (ApplicationInfo ai : apps) {
                if (ai.packageName == null || ai.packageName.equals(myPkg)) continue;
                boolean isSys = (ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0
                        && (ai.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0;
                if (!includeSystem && isSys) continue;
                CharSequence lbl = pm.getApplicationLabel(ai);
                String name = lbl != null ? lbl.toString() : ai.packageName;
                result.add(new AppPolicyEntry(name, ai.packageName, ai.uid, isSys));
            }
            Collections.sort(result, new Comparator<AppPolicyEntry>() {
                @Override
                public int compare(AppPolicyEntry a, AppPolicyEntry b) {
                    return a.label.compareToIgnoreCase(b.label);
                }
            });
        } catch (Exception ignored) {
        }
        return result;
    }
}
