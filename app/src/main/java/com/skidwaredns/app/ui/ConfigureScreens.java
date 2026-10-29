package com.skidwaredns.app.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.skidwaredns.app.config.SkidConfig;
import com.skidwaredns.app.dns.DnsUpstreamResolver;
import com.skidwaredns.app.proxy.OpenPortEngine;
import com.skidwaredns.app.firewall.SkidFirewallEngine;
import com.skidwaredns.app.model.DnsPresets;
import com.skidwaredns.app.tunnel.TunnelEngine;
import com.skidwaredns.app.util.PcapRecorder;
import com.skidwaredns.app.vpn.SkidVpnService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;

/**
 * Builds the Configure Hub and all 9 drill-down sub-screens modeled on RethinkDNS,
 * InviZible, strongSwan, Nym, and Tunnel Forge, plus expanded TCP & UDP stack controls.
 */
public final class ConfigureScreens {

    public static final int SUB_HUB = 0;
    public static final int SUB_APPS = 1;
    public static final int SUB_DNS = 2;
    public static final int SUB_FIREWALL = 3;
    public static final int SUB_PROXY = 4;
    public static final int SUB_TCPIP_UDP = 5;
    public static final int SUB_ANTICENSORSHIP = 6;
    public static final int SUB_NETWORK = 7;
    public static final int SUB_GENERAL = 8;
    public static final int SUB_LOGS_SETTINGS = 9;
    public static final int SUB_HTTPS = 25;
    public static final int SUB_PORTS = 26;
    public static final int SUB_OPEN_PORT = OpenPortScreens.SUB_OPEN_PORT;

    public interface HostCallbacks {
        SkidConfig getConfig();
        ExecutorService getBgExecutor();
        void persistAndReload(String note);
        void openConfigureSubScreen(int subId);
        void jumpToMainTab(int tabIdx);
        void triggerImportSkid();
        void triggerExportSkid();
        void triggerExportPcap();
    }

    private ConfigureScreens() {}

    public static View buildSubScreen(final Context ctx, int subId, final HostCallbacks host,
                                      final boolean[] includeSysAppsRef, final String[] appQueryRef) {
        switch (subId) {
            case SUB_APPS: return buildAppsScreen(ctx, host, includeSysAppsRef, appQueryRef);
            case SUB_DNS: return buildDnsScreen(ctx, host);
            case SUB_FIREWALL: return buildFirewallScreen(ctx, host);
            case SUB_PROXY: return buildProxyAndTunnelScreen(ctx, host);
            case SUB_TCPIP_UDP: return buildTcpIpUdpScreen(ctx, host);
            case SUB_HTTPS: return buildHttpsScreen(ctx, host);
            case SUB_PORTS: return buildPortsScreen(ctx, host);
            case OpenPortScreens.SUB_OPEN_PORT: return OpenPortScreens.build(ctx, host);
            case SUB_ANTICENSORSHIP: return buildAntiCensorshipScreen(ctx, host);
            case SUB_NETWORK: return buildNetworkScreen(ctx, host);
            case SUB_GENERAL: return buildGeneralSettingsScreen(ctx, host);
            case SUB_LOGS_SETTINGS: return buildLogsSettingsScreen(ctx, host);
            case InviZibleModuleScreens.SUB_LBOX_TEMPLATES:
                return InviZibleModuleScreens.buildLBoxScreen(ctx, host);
            case InviZibleModuleScreens.SUB_DNSCRYPT_SETTINGS:
                return InviZibleModuleScreens.buildDnscryptScreen(ctx, host);
            case InviZibleModuleScreens.SUB_PURPLE_I2P:
                return InviZibleModuleScreens.buildPurpleI2pScreen(ctx, host);
            case InviZibleModuleScreens.SUB_TOR_EXPANDED:
                return InviZibleModuleScreens.buildTorExpandedScreen(ctx, host);
            case InviZibleModuleScreens.SUB_FAST_AND_SECURITY:
                return InviZibleModuleScreens.buildFastAndSecurityScreen(ctx, host);
            case HttpInjectorScreens.SUB_TUNNEL_TYPE_MATRIX:
                return HttpInjectorScreens.buildTunnelTypeMatrixScreen(ctx, host);
            case HttpInjectorScreens.SUB_SSH_SETTINGS:
                return HttpInjectorScreens.buildSshSettingsScreen(ctx, host);
            case HttpInjectorScreens.SUB_V2RAY_XRAY:
                return HttpInjectorScreens.buildV2rayXrayScreen(ctx, host);
            case HttpInjectorScreens.SUB_HYSTERIA_SETTINGS:
                return HttpInjectorScreens.buildHysteriaScreen(ctx, host);
            case HttpInjectorScreens.SUB_INJECTOR_TUNNEL_HUB:
                return HttpInjectorScreens.buildInjectorHubScreen(ctx, host);
            case HttpInjectorScreens.SUB_SHADOWSOCKS_SETTINGS:
                return HttpInjectorScreens.buildShadowsocksScreen(ctx, host);
            case NymVpnScreens.SUB_NYM_PROFILES:
                return NymVpnScreens.buildProfilesScreen(ctx, host);
            case NymVpnScreens.SUB_NYM_MIXNET_TUNING:
                return NymVpnScreens.buildMixnetTuningScreen(ctx, host);
            case NymVpnScreens.SUB_NYM_ANTI_CENSORSHIP:
                return NymVpnScreens.buildAntiCensorshipScreen(ctx, host);
            case MullvadScreens.SUB_MULLVAD_RELAYS:
                return MullvadScreens.buildMullvadRelaysScreen(ctx, host);
            case SUB_HUB:
            default: return buildHubScreen(ctx, host);
        }
    }

    // =========================================================================
    // 0. CONFIGURE HUB (v5.2 task-first redesign: searchable, collapsible,
    //    grouped by task instead of a single 17-item wall of cards)
    // =========================================================================
    private static View buildHubScreen(final Context ctx, final HostCallbacks host) {
        final ScrollView sv = wrapScroll(ctx);
        final LinearLayout col = innerCol(ctx);
        final SkidConfig cfg = host.getConfig();

        // ---- Header -----------------------------------------------------
        LinearLayout headRow = new LinearLayout(ctx);
        headRow.setOrientation(LinearLayout.HORIZONTAL);
        headRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout headLeft = new LinearLayout(ctx);
        headLeft.setOrientation(LinearLayout.VERTICAL);
        headLeft.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        headLeft.addView(UiFactory.label(ctx, "Configure", 22f, UiFactory.COLOR_TEXT, true, false));
        headLeft.addView(UiFactory.label(ctx, "Every tool, one place \u2022 search or drill in", 12f, UiFactory.COLOR_MUTED, false, false));
        TextView verChip = UiFactory.counterChip(ctx, "v5.5", UiFactory.COLOR_SKY);
        LinearLayout headRight = new LinearLayout(ctx);
        headRight.setOrientation(LinearLayout.HORIZONTAL);
        headRight.setGravity(Gravity.CENTER_VERTICAL);
        headRight.setPadding(UiFactory.dp(ctx, 6), UiFactory.dp(ctx, 4), 0, UiFactory.dp(ctx, 4));
        headRight.addView(verChip);
        TextView mainChip = UiFactory.counterChip(ctx, "MAIN", UiFactory.COLOR_CYAN);
        LinearLayout.LayoutParams mcLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mcLp.leftMargin = UiFactory.dp(ctx, 6);
        mainChip.setLayoutParams(mcLp);
        headRight.addView(mainChip);
        headRight.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { host.jumpToMainTab(0); }
        });
        headRow.addView(headLeft);
        headRow.addView(headRight);
        LinearLayout.LayoutParams hrLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hrLp.topMargin = UiFactory.dp(ctx, 4);
        hrLp.bottomMargin = UiFactory.dp(ctx, 12);
        headRow.setLayoutParams(hrLp);
        col.addView(headRow);

        // ---- Task tiles: the 4 things people actually do -----------------
        LinearLayout tileRow1 = UiFactory.taskTileRow(ctx,
                UiFactory.taskTile(ctx, "\uD83D\uDD0C", "MULLVAD", shortMullvad(cfg), "Manage relays",
                        Color.parseColor("#2ECC40"), navTo(host, MullvadScreens.SUB_MULLVAD_RELAYS)),
                UiFactory.taskTile(ctx, "\uD83D\uDC22", "NYMVPN", shortNym(cfg), "Tune mixnet",
                        Color.parseColor("#3BE38C"), navTo(host, NymVpnScreens.SUB_NYM_PROFILES)));
        col.addView(tileRow1);

        LinearLayout tileRow2 = UiFactory.taskTileRow(ctx,
                UiFactory.taskTile(ctx, "\uD83D\uDC89", "HTTP INJECTOR",
                        com.skidwaredns.app.tunnel.XrayUriAndRouteHelper.formatSelectedTunnelBadge(cfg),
                        "Open tunnel forge",
                        Color.parseColor("#1D9BF0"), navTo(host, HttpInjectorScreens.SUB_INJECTOR_TUNNEL_HUB)),
                UiFactory.taskTile(ctx, "\uD83D\uDD10", "PRIVACY STACK",
                        privacyStackBadge(cfg), "DNSCrypt / Tor / I2P",
                        UiFactory.COLOR_PURPLE, navTo(host, InviZibleModuleScreens.SUB_DNSCRYPT_SETTINGS)));
        col.addView(tileRow2);

        // ---- Search ------------------------------------------------------
        final LinearLayout sectionsHolder = new LinearLayout(ctx);
        sectionsHolder.setOrientation(LinearLayout.VERTICAL);

        final EditText search = UiFactory.searchField(ctx, "\uD83D\uDD0D  Search all settings (dns, ssh, buffer, tor, mtu...)",
                new UiFactory.ChipSelectListener() {
                    @Override public void onSelected(String value) {
                        renderSectionDirectory(ctx, host, cfg, sectionsHolder, value);
                    }
                });
        col.addView(search);
        col.addView(sectionsHolder);

        renderSectionDirectory(ctx, host, cfg, sectionsHolder, "");

        col.addView(UiFactory.versionFooter(ctx, "v 5 . 3",
                "23 presets \u2022 23 Mullvad relays \u2022 " + cfg.name + ".skid"));

        sv.addView(col);
        return sv;
    }

    private static String shortMullvad(SkidConfig cfg) {
        if (!cfg.mullvadEnabled) return "Off";
        if (cfg.mullvadMultihop) {
            return shortHost(cfg.mullvadEntryRelay) + " \u2192 " + shortHost(cfg.mullvadExitRelay);
        }
        return shortHost(cfg.mullvadExitRelay) + (cfg.mullvadDaita ? " \u2022 DAITA" : "");
    }

    private static String shortHost(String host) {
        if (host == null || host.isEmpty()) return "\u2014";
        return host.replace(".relays.mullvad.net", "");
    }

    private static String shortNym(SkidConfig cfg) {
        com.skidwaredns.app.tunnel.NymMixnetAndCensorshipEngine.MixnetPerformanceEstimate e =
                com.skidwaredns.app.tunnel.NymMixnetAndCensorshipEngine.estimatePerformance(cfg);
        return cfg.nymProfile + " \u2022 " + e.latencyText;
    }

    private static String privacyStackBadge(SkidConfig cfg) {
        StringBuilder sb = new StringBuilder();
        if (cfg.dnscryptModuleEnabled) sb.append("DNSCrypt ");
        if (cfg.torEnabled) sb.append("Tor ");
        if (cfg.i2pEnabled) sb.append("I2P ");
        return sb.length() == 0 ? "All modules off" : sb.toString().trim();
    }

    /**
     * Renders the grouped, collapsible setting directory (filtered by `query`).
     * Rebuilt in place on every search keystroke.
     */
    private static void renderSectionDirectory(final Context ctx, final HostCallbacks host,
                                               final SkidConfig cfg, LinearLayout holder, String query) {
        holder.removeAllViews();
        boolean searching = query != null && !query.trim().isEmpty();

        // ================= 1. CONNECT & ROUTE =====================
        List<UiFactory.SettingEntry> connect = new ArrayList<>();
        connect.add(new UiFactory.SettingEntry("\uD83D\uDD0C", "Mullvad Relays, DAITA & Multihop",
                shortMullvad(cfg) + " \u2022 100G Tallinn + Zurich WG/Bridge", Color.parseColor("#2ECC40"), 0,
                navTo(host, MullvadScreens.SUB_MULLVAD_RELAYS)));
        connect.add(new UiFactory.SettingEntry("\uD83D\uDC22", "NymVPN Profiles & Mixnet Tuning",
                "Profile " + cfg.nymProfile + " \u2022 " + shortNym(cfg), Color.parseColor("#3BE38C"), 0,
                navTo(host, NymVpnScreens.SUB_NYM_PROFILES)));
        connect.add(new UiFactory.SettingEntry("\u2699", "NymVPN Anti-Censorship",
                "QUIC=" + cfg.nymQuicEnhanced + " \u2022 AmneziaWG=" + cfg.amneziaWgMinimalObfs,
                Color.parseColor("#3BE38C"), 0, navTo(host, NymVpnScreens.SUB_NYM_ANTI_CENSORSHIP)));
        connect.add(new UiFactory.SettingEntry("\u21C4", "Proxy & Tunnels",
                "WireGuard (" + cfg.wgProfileName + "), Orbot, SOCKS5, HTTP, SSH, Ports", UiFactory.COLOR_SKY, 0,
                navTo(host, SUB_PROXY)));
        connect.add(new UiFactory.SettingEntry("\u26A1", "Anti-Censorship & DPI Desync",
                cfg.dpiStrategy + " \u2022 " + cfg.dpiTechnique, UiFactory.COLOR_AMBER, 0,
                navTo(host, SUB_ANTICENSORSHIP)));
        connect.add(new UiFactory.SettingEntry("\u260D", "Network & VPN Interface",
                "Mode " + cfg.mode + " \u2022 Loopback & Bootstrap DNS", UiFactory.COLOR_SKY, 0,
                navTo(host, SUB_NETWORK)));
        addSection(ctx, host, holder, "\uD83D\uDD17", "Connect & Route",
                "Tunnel, relays, mixnet and censorship workarounds", UiFactory.COLOR_SKY, connect, query, true);

        // ================= 2. TUNNEL FORGE (HTTP Injector) =========
        List<UiFactory.SettingEntry> forge = new ArrayList<>();
        forge.add(new UiFactory.SettingEntry("\u25A6", "Tunnel Type & Connect-From Matrix",
                com.skidwaredns.app.tunnel.XrayUriAndRouteHelper.formatSelectedTunnelHeadline(cfg),
                Color.parseColor("#1D9BF0"), 0, navTo(host, HttpInjectorScreens.SUB_TUNNEL_TYPE_MATRIX)));
        forge.add(new UiFactory.SettingEntry("\uD83D\uDD11", "SSH Settings & UDPGW",
                cfg.tunnelHost + ":" + cfg.tunnelPort + " \u2022 local " + cfg.sshLocalPort,
                Color.parseColor("#1D9BF0"), 0, navTo(host, HttpInjectorScreens.SUB_SSH_SETTINGS)));
        forge.add(new UiFactory.SettingEntry("\u2B83", "V2Ray / Xray Core",
                cfg.xrayProtocol + " \u2022 " + cfg.xrayNetworkType, Color.parseColor("#1D9BF0"), 0,
                navTo(host, HttpInjectorScreens.SUB_V2RAY_XRAY)));
        forge.add(new UiFactory.SettingEntry("\u26A1", "Hysteria v1 / v2",
                cfg.tunnelHost + ":" + cfg.tunnelPort, Color.parseColor("#1D9BF0"), 0,
                navTo(host, HttpInjectorScreens.SUB_HYSTERIA_SETTINGS)));
        forge.add(new UiFactory.SettingEntry("\uD83D\uDD10", "Shadowsocks / SS-2022",
                cfg.tunnelCipher, Color.parseColor("#1D9BF0"), 0,
                navTo(host, HttpInjectorScreens.SUB_SHADOWSOCKS_SETTINGS)));
        forge.add(new UiFactory.SettingEntry("\u2699", "Injector Tunnel Hub",
                "Apps filter, routes, DoH provider, UI options", Color.parseColor("#1D9BF0"), 0,
                navTo(host, HttpInjectorScreens.SUB_INJECTOR_TUNNEL_HUB)));
        addSection(ctx, host, holder, "\uD83D\uDC89", "Tunnel Forge (HTTP Injector)",
                "SSH, Xray, Hysteria, Shadowsocks engines", Color.parseColor("#1D9BF0"), forge, query, false);

        // ================= 3. PRIVACY STACK ========================
        List<UiFactory.SettingEntry> privacy = new ArrayList<>();
        privacy.add(new UiFactory.SettingEntry("\uD83D\uDD10", "DNSCrypt & Anon Relays",
                cfg.dnscryptRelays.size() + " relays \u2022 port " + cfg.dnscryptListenPort + " \u2022 " + privacyStackBadge(cfg),
                UiFactory.COLOR_PURPLE, 0, navTo(host, InviZibleModuleScreens.SUB_DNSCRYPT_SETTINGS)));
        privacy.add(new UiFactory.SettingEntry("\uD83E\uDDC5", "Tor Onion Router",
                "SOCKS :" + cfg.torSocksPort + " \u2022 " + cfg.torBridgeType + " \u2022 Snowflake "
                        + cfg.torSnowflakeRendezvous, UiFactory.COLOR_PURPLE, 0,
                navTo(host, InviZibleModuleScreens.SUB_TOR_EXPANDED)));
        privacy.add(new UiFactory.SettingEntry("\uD83D\uDFE3", "Purple I2P (i2pd)",
                "HTTP :" + cfg.i2pHttpProxyPort + " \u2022 SOCKS :" + cfg.i2pSocksProxyPort,
                UiFactory.COLOR_PURPLE, 0, navTo(host, InviZibleModuleScreens.SUB_PURPLE_I2P)));
        privacy.add(new UiFactory.SettingEntry("\u25AB", "LBox Templates & TLS Fragment",
                cfg.lboxTemplates.size() + " templates \u2022 stack " + cfg.tunStack,
                UiFactory.COLOR_PURPLE, 0, navTo(host, InviZibleModuleScreens.SUB_LBOX_TEMPLATES)));
        privacy.add(new UiFactory.SettingEntry("\uD83D\uDE80", "Fast Settings, MITM, MegaProxy & Lumine",
                "Autostart, SNI spoof, ARP MITM, jump hosts, Fake-IP", UiFactory.COLOR_PURPLE, 0,
                navTo(host, InviZibleModuleScreens.SUB_FAST_AND_SECURITY)));
        addSection(ctx, host, holder, "\uD83D\uDEE1", "Privacy Stack",
                "DNSCrypt, Tor, I2P, LBox and proxy shapers", UiFactory.COLOR_PURPLE, privacy, query, false);

        // ================= 4. DNS & FIREWALL =======================
        List<UiFactory.SettingEntry> dnsfw = new ArrayList<>();
        dnsfw.add(new UiFactory.SettingEntry("\uD83C\uDF10", "DNS",
                SkidVpnService.describeDnsSelection(cfg), UiFactory.COLOR_EMERALD, 0, navTo(host, SUB_DNS)));
        dnsfw.add(new UiFactory.SettingEntry("\uD83D\uDEE1", "Firewall & Blocklists",
                cfg.countActiveUniversalRules() + " universal rules", UiFactory.COLOR_EMERALD, 0,
                navTo(host, SUB_FIREWALL)));
        dnsfw.add(new UiFactory.SettingEntry("\u229E", "Per-App Rules",
                "Bypass, Torify and UID policy per app", UiFactory.COLOR_EMERALD, 0, navTo(host, SUB_APPS)));
        addSection(ctx, host, holder, "\uD83C\uDF10", "DNS & Firewall",
                "Resolution, blocklists and per-app policy", UiFactory.COLOR_EMERALD, dnsfw, query, false);

        // ================= 5. PERFORMANCE ==========================
        List<UiFactory.SettingEntry> perf = new ArrayList<>();
        perf.add(new UiFactory.SettingEntry("\u2699", "TCP / IP & UDP",
                "Buffer " + cfg.socketBufferSizeKb + " KB \u2022 " + cfg.tcpCongestion + " \u2022 keepalive " + cfg.tcpKeepaliveIdleSec + "s",
                UiFactory.COLOR_AMBER, 0, navTo(host, SUB_TCPIP_UDP)));
        perf.add(new UiFactory.SettingEntry("\uD83D\uDD10", "HTTPS / TLS",
                cfg.httpsMinTls + " \u2022 ALPN " + cfg.httpsAlpn + " \u2022 " + cfg.httpsTimeoutMs + " ms",
                UiFactory.COLOR_AMBER, 0, navTo(host, SUB_HTTPS)));
        perf.add(new UiFactory.SettingEntry("#", "Ports",
                "block " + cfg.portBlockList + " \u2022 SOCKS :" + cfg.getPrimaryLocalSocksPort(),
                UiFactory.COLOR_AMBER, 0, navTo(host, SUB_PORTS)));
        perf.add(new UiFactory.SettingEntry("PORT", "Open Port",
                OpenPortEngine.get().list().size() + " listening \u2022 catalog 1–10000 \u2022 "
                        + OpenPortEngine.primaryDeviceIpv4(),
                UiFactory.COLOR_AMBER, 0, navTo(host, OpenPortScreens.SUB_OPEN_PORT)));
        addSection(ctx, host, holder, "\u26A1", "Performance & Sockets",
                "Buffers, TLS, NAT, MSS and port policy", UiFactory.COLOR_AMBER, perf, query, false);

        // ================= 6. APP & DIAGNOSTICS ====================
        List<UiFactory.SettingEntry> app = new ArrayList<>();
        app.add(new UiFactory.SettingEntry("\u2726", "Settings, Backup & Restore",
                ".skid import/export \u2022 bubble \u2022 ASN info", UiFactory.COLOR_MUTED, 0,
                navTo(host, SUB_GENERAL)));
        app.add(new UiFactory.SettingEntry("\u223F", "Logs & Packet Capture",
                cfg.packetCaptureMode + " \u2022 level " + cfg.logLevel, UiFactory.COLOR_MUTED, 0,
                navTo(host, SUB_LOGS_SETTINGS)));
        addSection(ctx, host, holder, "\u2699", "App & Diagnostics",
                "Profiles, logging and packet capture", UiFactory.COLOR_MUTED, app, query, false);

        if (searching && holder.getChildCount() == 0) {
            holder.addView(UiFactory.footnote(ctx, "No settings match \u201C" + query.trim() + "\u201D. Try dns, ssh, tor, mtu, buffer or mullvad."));
        }
    }

    /** Adds one collapsible group (or a flat filtered list when searching). */
    private static void addSection(Context ctx, final HostCallbacks host, LinearLayout holder,
                                   String glyph, String title, String summary, int accent,
                                   List<UiFactory.SettingEntry> entries, String query,
                                   boolean expandedDefault) {
        boolean searching = query != null && !query.trim().isEmpty();
        List<UiFactory.SettingEntry> shown = UiFactory.filterEntries(entries, query);
        if (shown.isEmpty()) return;

        LinearLayout body = new LinearLayout(ctx);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(UiFactory.dp(ctx, 12), UiFactory.dp(ctx, 4), UiFactory.dp(ctx, 12), UiFactory.dp(ctx, 4));
        body.setBackground(UiFactory.roundedRect(ctx, UiFactory.COLOR_CARD, UiFactory.COLOR_BORDER, 0.8f, 14f));

        for (int i = 0; i < shown.size(); i++) {
            final UiFactory.SettingEntry e = shown.get(i);
            body.addView(UiFactory.iconNavRow(ctx, e.glyph, e.title, e.subtitle, UiFactory.COLOR_MUTED, e.onClick));
            if (i < shown.size() - 1) body.addView(UiFactory.divider(ctx));
        }

        holder.addView(UiFactory.collapsibleSection(ctx, glyph, title,
                summary + "  \u2022  " + shown.size() + (shown.size() == 1 ? " setting" : " settings"),
                accent, searching || expandedDefault, body));
    }

    private static View buildAppsScreen(final Context ctx, final HostCallbacks host,
                                        final boolean[] includeSysRef, final String[] queryRef) {
        ScrollView sv = wrapScroll(ctx);
        LinearLayout col = innerCol(ctx);
        final SkidConfig cfg = host.getConfig();

        col.addView(UiFactory.backHeader(ctx, "Apps",
                "Tap any app badge to cycle VPN/DNS → BYPASS → TORIFY → BLOCK", backToHub(host)));

        LinearLayout topCard = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        topCard.addView(UiFactory.iconSwitchRow(ctx, "⚙", "Show System Packages",
                "Include Android OS & carrier apps (Bypass: " + cfg.bypassApps.size()
                        + " | Blocked: " + cfg.blockedApps.size() + " | Torify: " + cfg.torifyApps.size() + ")",
                includeSysRef[0], new UiFactory.ToggleListener() {
                    @Override
                    public void onToggled(boolean checked) {
                        includeSysRef[0] = checked;
                        host.openConfigureSubScreen(SUB_APPS);
                    }
                }));

        final EditText searchEt = UiFactory.monoInput(ctx, queryRef[0], "Search app name or package...", false);
        topCard.addView(searchEt);
        col.addView(topCard);

        final LinearLayout listHolder = new LinearLayout(ctx);
        listHolder.setOrientation(LinearLayout.VERTICAL);
        listHolder.addView(UiFactory.label(ctx, "Loading installed packages...", 12.5f, UiFactory.COLOR_MUTED, false, false));
        col.addView(listHolder);

        final Runnable loader = new Runnable() {
            @Override
            public void run() {
                final boolean sys = includeSysRef[0];
                final String q = queryRef[0].toLowerCase(Locale.US);
                host.getBgExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        final List<SkidFirewallEngine.AppPolicyEntry> all = SkidFirewallEngine.queryInstalledApps(ctx, sys);
                        final List<SkidFirewallEngine.AppPolicyEntry> out = new ArrayList<>();
                        for (SkidFirewallEngine.AppPolicyEntry a : all) {
                            if (q.isEmpty() || a.label.toLowerCase(Locale.US).contains(q)
                                    || a.packageName.toLowerCase(Locale.US).contains(q)) {
                                out.add(a);
                                if (out.size() >= 50) break;
                            }
                        }
                        listHolder.post(new Runnable() {
                            @Override
                            public void run() {
                                listHolder.removeAllViews();
                                for (final SkidFirewallEngine.AppPolicyEntry app : out) {
                                    listHolder.addView(buildAppRow(ctx, cfg, app, host));
                                }
                            }
                        });
                    }
                });
            }
        };

        searchEt.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                queryRef[0] = s.toString().trim();
                loader.run();
            }
        });
        loader.run();

        sv.addView(col);
        return sv;
    }

    private static View buildAppRow(Context ctx, final SkidConfig cfg,
                                    final SkidFirewallEngine.AppPolicyEntry app, final HostCallbacks host) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        int p = UiFactory.dp(ctx, 12);
        row.setPadding(p, p, p, p);
        row.setBackground(UiFactory.roundedRect(ctx, UiFactory.COLOR_CARD, UiFactory.COLOR_BORDER, 0.8f, 14f));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = UiFactory.dp(ctx, 8);
        row.setLayoutParams(lp);

        LinearLayout info = new LinearLayout(ctx);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        info.addView(UiFactory.label(ctx, app.label + " (UID " + app.uid + ")", 13.5f, UiFactory.COLOR_TEXT, true, false));
        info.addView(UiFactory.label(ctx, app.packageName, 11f, UiFactory.COLOR_MUTED, false, true));

        final TextView badge = UiFactory.label(ctx, appState(cfg, app.packageName), 11.5f, UiFactory.COLOR_SKY, true, true);
        styleAppBadge(ctx, badge, appState(cfg, app.packageName));
        badge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String cur = appState(cfg, app.packageName);
                cfg.blockedApps.remove(app.packageName);
                cfg.bypassApps.remove(app.packageName);
                cfg.torifyApps.remove(app.packageName);
                String next;
                if ("VPN/DNS".equals(cur)) { cfg.bypassApps.add(app.packageName); next = "BYPASS"; }
                else if ("BYPASS".equals(cur)) { cfg.torifyApps.add(app.packageName); next = "TORIFY"; }
                else if ("TORIFY".equals(cur)) { cfg.blockedApps.add(app.packageName); next = "BLOCK"; }
                else { next = "VPN/DNS"; }
                badge.setText(next);
                styleAppBadge(ctx, badge, next);
                host.persistAndReload("App " + app.packageName + " -> " + next);
            }
        });

        row.addView(info);
        row.addView(badge);
        return row;
    }

    private static String appState(SkidConfig cfg, String pkg) {
        if (cfg.blockedApps.contains(pkg)) return "BLOCK";
        if (cfg.bypassApps.contains(pkg)) return "BYPASS";
        if (cfg.torifyApps.contains(pkg)) return "TORIFY";
        return "VPN/DNS";
    }

    private static void styleAppBadge(Context ctx, TextView badge, String st) {
        int col = UiFactory.COLOR_EMERALD;
        if ("BLOCK".equals(st)) col = UiFactory.COLOR_CRIMSON;
        else if ("BYPASS".equals(st)) col = UiFactory.COLOR_AMBER;
        else if ("TORIFY".equals(st)) col = UiFactory.COLOR_PURPLE;
        badge.setTextColor(col);
        badge.setPadding(UiFactory.dp(ctx, 11), UiFactory.dp(ctx, 6), UiFactory.dp(ctx, 11), UiFactory.dp(ctx, 6));
        badge.setBackground(UiFactory.roundedRect(ctx, UiFactory.COLOR_SURFACE, col, 1.2f, 10f));
    }

    // =========================================================================
    // 2. DNS (Global + Presets + Rules + Filtering + Advanced)
    // =========================================================================
    private static View buildDnsScreen(final Context ctx, final HostCallbacks host) {
        ScrollView sv = wrapScroll(ctx);
        LinearLayout col = innerCol(ctx);
        final SkidConfig cfg = host.getConfig();

        col.addView(UiFactory.backHeader(ctx, "DNS",
                "Global mode, Rethink/DNSCrypt/DoT/Proxy endpoints, Booster & Filtering", backToHub(host)));

        // Global DNS Radio Card (Screenshot_20261028_153320)
        col.addView(UiFactory.categoryTitle(ctx, "Global"));
        col.addView(UiFactory.buildRadioCard(ctx, new UiFactory.RadioItem[]{
                new UiFactory.RadioItem("System DNS", "System DNS", "Use network/bootstrap DNS resolvers"),
                new UiFactory.RadioItem("Other DNS", "Other DNS", "DNS-over-TLS, DNSCrypt (sdns://), and DNS Proxy"),
                new UiFactory.RadioItem("Rethink DNS", "Rethink DNS", "Sky DoH servers with 100k+ threat/ad/privacy blocklists"),
                new UiFactory.RadioItem("Smart DNS", "Smart DNS", "Parallel low-latency DoT + DoH racing (" + DnsUpstreamResolver.getCacheSize() + " cached)")
        }, cfg.globalDnsCategory, new UiFactory.ChipSelectListener() {
            @Override
            public void onSelected(String value) {
                cfg.globalDnsCategory = value;
                if ("Rethink DNS".equalsIgnoreCase(value)) {
                    cfg.dnsMode = "Rethink-DoH";
                }
                host.persistAndReload("Global DNS -> " + value);
            }
        }));

        // Rules Card (Screenshot_20261028_153307)
        col.addView(UiFactory.categoryTitle(ctx, "Rules"));
        LinearLayout rulesCard = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        rulesCard.addView(UiFactory.iconNavRow(ctx, "☰", "On-device blocklists",
                cfg.onDeviceBlocklistsEnabled
                        ? ("Active (" + cfg.activeBlocklistPacks.size() + " packs enabled) • Tap to configure")
                        : "Disabled • Tap to configure blocklist packs",
                cfg.onDeviceBlocklistsEnabled ? UiFactory.COLOR_EMERALD : UiFactory.COLOR_CRIMSON,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showBlocklistPacksDialog(ctx, cfg, host);
                    }
                }));
        rulesCard.addView(UiFactory.divider(ctx));
        rulesCard.addView(UiFactory.iconSwitchRow(ctx, "⬇", "Use in-app downloader",
                "If disabled, Android's built-in Download Manager is used.",
                cfg.useInAppDownloader, new UiFactory.ToggleListener() {
                    @Override
                    public void onToggled(boolean checked) {
                        cfg.useInAppDownloader = checked;
                        host.persistAndReload("Use-In-App-Downloader=" + checked);
                    }
                }));
        rulesCard.addView(UiFactory.divider(ctx));
        rulesCard.addView(UiFactory.iconSwitchRow(ctx, "🕒", "Prompt on blocklist updates",
                "Check for blocklist updates once every three days.",
                cfg.promptBlocklistUpdates, new UiFactory.ToggleListener() {
                    @Override
                    public void onToggled(boolean checked) {
                        cfg.promptBlocklistUpdates = checked;
                        host.persistAndReload("Prompt-Blocklist-Updates=" + checked);
                    }
                }));
        col.addView(rulesCard);

        // Filtering Card (Screenshot_20261028_153307)
        col.addView(UiFactory.categoryTitle(ctx, "Filtering"));
        LinearLayout filtCard = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        filtCard.addView(UiFactory.iconSwitchRow(ctx, "◎", "Advanced DNS filtering (experimental)",
                "Assign unique tracking metadata per DNS request.",
                cfg.advancedDnsFiltering, new UiFactory.ToggleListener() {
                    @Override
                    public void onToggled(boolean checked) {
                        cfg.advancedDnsFiltering = checked;
                        host.persistAndReload("Advanced-DNS-Filtering=" + checked);
                    }
                }));
        filtCard.addView(UiFactory.divider(ctx));
        filtCard.addView(UiFactory.iconSwitchRow(ctx, "⑂", "Split DNS (experimental)",
                "Forward DNS queries from proxied/Tor apps to the proxy's DNS servers.",
                cfg.splitDns, new UiFactory.ToggleListener() {
                    @Override
                    public void onToggled(boolean checked) {
                        cfg.splitDns = checked;
                        host.persistAndReload("Split-DNS=" + checked);
                    }
                }));
        filtCard.addView(UiFactory.divider(ctx));
        String recSummary = cfg.allowedDnsRecords.contains("ALL")
                ? "Auto (All types)" : joinCsv(cfg.allowedDnsRecords);
        filtCard.addView(UiFactory.iconNavRow(ctx, "🌐", "Allowed DNS record types",
                "Select which DNS resource record types to allow. " + recSummary,
                UiFactory.COLOR_MUTED, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showAllowedRecordTypesDialog(ctx, cfg, host);
                    }
                }));
        col.addView(filtCard);

        // DNS Advanced Card (Screenshot_20261028_153250)
        col.addView(UiFactory.categoryTitle(ctx, "Advanced"));
        LinearLayout advCard = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        advCard.addView(UiFactory.iconSwitchRow(ctx, "🚀", "DNS booster",
                "Coalesce requests, cache responses (TTL 120s), resilient error handling.",
                cfg.dnsBooster, new UiFactory.ToggleListener() {
                    @Override
                    public void onToggled(boolean checked) {
                        cfg.dnsBooster = checked;
                        if (!checked) DnsUpstreamResolver.flushCache();
                        host.persistAndReload("DNS-Booster=" + checked);
                    }
                }));
        advCard.addView(UiFactory.divider(ctx));
        advCard.addView(UiFactory.iconSwitchRow(ctx, "⇄", "Never proxy DNS",
                "Do not proxy DNS over Always-on WireGuard, SOCKS5, HTTP proxies.",
                cfg.neverProxyDns, new UiFactory.ToggleListener() {
                    @Override
                    public void onToggled(boolean checked) {
                        cfg.neverProxyDns = checked;
                        host.persistAndReload("Never-Proxy-DNS=" + checked);
                    }
                }));
        advCard.addView(UiFactory.divider(ctx));
        advCard.addView(UiFactory.iconSwitchRow(ctx, "☂", "Prevent DNS leaks",
                "When enabled, SkidwareDNS captures all packets on port 53 and forwards to user-set DNS.",
                cfg.preventDnsLeaks, new UiFactory.ToggleListener() {
                    @Override
                    public void onToggled(boolean checked) {
                        cfg.preventDnsLeaks = checked;
                        cfg.blockBypassedDns = checked;
                        host.persistAndReload("Prevent-DNS-Leaks=" + checked);
                    }
                }));
        advCard.addView(UiFactory.divider(ctx));
        advCard.addView(UiFactory.iconSwitchRow(ctx, "⚙", "Block DNS from Unknown source",
                "Block all DNS requests originating from unknown applications.",
                cfg.blockUnknownDnsSource, new UiFactory.ToggleListener() {
                    @Override
                    public void onToggled(boolean checked) {
                        cfg.blockUnknownDnsSource = checked;
                        host.persistAndReload("Block-Unknown-DNS-Source=" + checked);
                    }
                }));
        advCard.addView(UiFactory.divider(ctx));
        advCard.addView(UiFactory.iconSwitchRow(ctx, "⊞", "Use System DNS for undelegated domains",
                "Use System/Bootstrap DNS for undelegated domains like .lan, .internal, .local.",
                cfg.systemDnsUndelegated, new UiFactory.ToggleListener() {
                    @Override
                    public void onToggled(boolean checked) {
                        cfg.systemDnsUndelegated = checked;
                        host.persistAndReload("System-DNS-Undelegated=" + checked);
                    }
                }));
        advCard.addView(UiFactory.divider(ctx));
        advCard.addView(UiFactory.iconSwitchRow(ctx, "🌐", "Use System DNS on captive portals",
                "Use System DNS to resolve domains until captive portal sign-in is complete.",
                cfg.systemDnsCaptivePortal, new UiFactory.ToggleListener() {
                    @Override
                    public void onToggled(boolean checked) {
                        cfg.systemDnsCaptivePortal = checked;
                        host.persistAndReload("System-DNS-Captive-Portal=" + checked);
                    }
                }));
        advCard.addView(UiFactory.divider(ctx));
        advCard.addView(UiFactory.iconNavRow(ctx, "↪", "DNS for bypass",
                cfg.dnsForBypass, UiFactory.COLOR_MUTED, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showSingleChoiceDialog(ctx, "DNS for bypass",
                                new String[]{"Use Bootstrap DNS", "System DNS", "Quad9 (9.9.9.9)"},
                                cfg.dnsForBypass, new UiFactory.ChipSelectListener() {
                                    @Override
                                    public void onSelected(String val) {
                                        cfg.dnsForBypass = val;
                                        host.persistAndReload("DNS-For-Bypass=" + val);
                                        host.openConfigureSubScreen(SUB_DNS);
                                    }
                                });
                    }
                }));
        col.addView(advCard);

        // Built-in Server Catalog (RethinkDNS, DNS Proxy, DNSCrypt, DoT + Custom)
        col.addView(UiFactory.categoryTitle(ctx, "Other DNS Protocol Selector"));
        LinearLayout protoCard = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        protoCard.addView(UiFactory.buildChipSelector(ctx, DnsPresets.DNS_MODES, cfg.dnsMode, UiFactory.COLOR_SKY,
                new UiFactory.ChipSelectListener() {
                    @Override
                    public void onSelected(String value) {
                        cfg.dnsMode = value;
                        cfg.globalDnsCategory = "Rethink-DoH".equalsIgnoreCase(value) ? "Rethink DNS" : "Other DNS";
                        host.persistAndReload("DNS Mode -> " + value);
                    }
                }));
        String existingCustom = cfg.customDnsServer.isEmpty() ? "" : cfg.customDnsServer.get(0);
        final EditText customDnsEt = UiFactory.monoInput(ctx, existingCustom, "Custom DNS override (https://, tls://, sdns://, or ip:port)", false);
        Button saveCustomBtn = UiFactory.actionButton(ctx, "Save Custom DNS Override", UiFactory.COLOR_SURFACE, UiFactory.COLOR_SKY, UiFactory.COLOR_BORDER);
        saveCustomBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cfg.customDnsServer.clear();
                String val = customDnsEt.getText().toString().trim();
                if (!val.isEmpty()) cfg.customDnsServer.add(val);
                host.persistAndReload("Updated Custom DNS Server []");
                Toast.makeText(ctx, val.isEmpty() ? "Cleared Custom DNS" : "Set Custom DNS: " + val, Toast.LENGTH_SHORT).show();
            }
        });
        protoCard.addView(customDnsEt);
        protoCard.addView(saveCustomBtn);
        col.addView(protoCard);

        // RethinkDNS Sky Servers
        col.addView(UiFactory.categoryTitle(ctx, "Rethink DNS Servers (Sky DoH)"));
        LinearLayout rdnsBox = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        for (final DnsPresets.DnsEndpoint ep : DnsPresets.RETHINK_SERVERS) {
            boolean active = "Rethink-DoH".equalsIgnoreCase(cfg.dnsMode) && ep.value.equalsIgnoreCase(cfg.rethinkDoh);
            rdnsBox.addView(buildEndpointSelectCard(ctx, ep, active, null, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    cfg.globalDnsCategory = "Rethink DNS";
                    cfg.dnsMode = "Rethink-DoH";
                    cfg.rethinkDoh = ep.value;
                    cfg.customDnsServer.clear();
                    prependServer(cfg, ep.value);
                    host.persistAndReload("Selected " + ep.name);
                    host.openConfigureSubScreen(SUB_DNS);
                }
            }));
        }
        col.addView(rdnsBox);

        // DNS Proxy Servers
        col.addView(UiFactory.categoryTitle(ctx, "DNS Proxy Servers (Orbot / IPv4 / IPv6)"));
        LinearLayout proxyBox = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        for (final DnsPresets.DnsEndpoint ep : DnsPresets.DNS_PROXY_SERVERS) {
            boolean active = "DNS-Proxy".equalsIgnoreCase(cfg.dnsMode)
                    && (ep.name.equalsIgnoreCase(cfg.dnsProxy) || ep.value.equalsIgnoreCase(cfg.dnsProxy));
            proxyBox.addView(buildEndpointSelectCard(ctx, ep, active, null, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    cfg.globalDnsCategory = "Other DNS";
                    cfg.dnsMode = "DNS-Proxy";
                    cfg.dnsProxy = ep.name;
                    if ("Orbot".equalsIgnoreCase(ep.name)) cfg.torEnabled = true;
                    cfg.customDnsServer.clear();
                    prependServer(cfg, ep.value);
                    host.persistAndReload("Selected DNS Proxy " + ep.name);
                    host.openConfigureSubScreen(SUB_DNS);
                }
            }));
        }
        col.addView(proxyBox);

        // DNSCrypt Servers
        col.addView(UiFactory.categoryTitle(ctx, "DNSCrypt Stamps (sdns:// Decoded)"));
        LinearLayout cryptBox = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        for (final DnsPresets.DnsEndpoint ep : DnsPresets.DNSCRYPT_SERVERS) {
            boolean active = "DNSCrypt".equalsIgnoreCase(cfg.dnsMode)
                    && (ep.name.equalsIgnoreCase(cfg.dnsCrypt) || ep.value.equalsIgnoreCase(cfg.dnsCrypt));
            DnsPresets.DecodedStamp st = DnsPresets.decodeSdnsStamp(ep.value);
            cryptBox.addView(buildEndpointSelectCard(ctx, ep, active, st.summaryBadges(), new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    cfg.globalDnsCategory = "Other DNS";
                    cfg.dnsMode = "DNSCrypt";
                    cfg.dnsCrypt = ep.name;
                    cfg.customDnsServer.clear();
                    prependServer(cfg, ep.value);
                    host.persistAndReload("Selected DNSCrypt " + ep.name);
                    host.openConfigureSubScreen(SUB_DNS);
                }
            }));
        }
        col.addView(cryptBox);

        // DoT Servers
        col.addView(UiFactory.categoryTitle(ctx, "DNS-over-TLS (DoT :853)"));
        LinearLayout dotBox = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        for (final DnsPresets.DnsEndpoint ep : DnsPresets.DOT_SERVERS) {
            boolean active = "DNS-over-TLS".equalsIgnoreCase(cfg.dnsMode)
                    && (ep.name.equalsIgnoreCase(cfg.dnsOverTls) || ep.value.equalsIgnoreCase(cfg.dnsOverTls));
            dotBox.addView(buildEndpointSelectCard(ctx, ep, active, null, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    cfg.globalDnsCategory = "Other DNS";
                    cfg.dnsMode = "DNS-over-TLS";
                    cfg.dnsOverTls = ep.name;
                    cfg.customDnsServer.clear();
                    prependServer(cfg, ep.value);
                    host.persistAndReload("Selected DoT " + ep.name);
                    host.openConfigureSubScreen(SUB_DNS);
                }
            }));
        }
        col.addView(dotBox);

        sv.addView(col);
        return sv;
    }

    // =========================================================================
    // 3. FIREWALL (Universal + Custom Domain/IP/Port Rules)
    // =========================================================================
    private static View buildFirewallScreen(final Context ctx, final HostCallbacks host) {
        ScrollView sv = wrapScroll(ctx);
        LinearLayout col = innerCol(ctx);
        final SkidConfig cfg = host.getConfig();

        col.addView(UiFactory.backHeader(ctx, "Firewall",
                cfg.countActiveUniversalRules() + " Universal rules active • " + cfg.firewallRules.size() + " custom rules",
                backToHub(host)));

        col.addView(UiFactory.categoryTitle(ctx, "Universal Firewall Rules"));
        LinearLayout uCard = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        uCard.addView(UiFactory.iconSwitchRow(ctx, "🔒", "Block all apps when device is locked",
                "Drops outbound traffic while screen/keyguard is locked.",
                cfg.blockScreenLocked, new UiFactory.ToggleListener() {
                    @Override
                    public void onToggled(boolean checked) {
                        cfg.blockScreenLocked = checked;
                        host.persistAndReload("Block-Screen-Locked=" + checked);
                    }
                }));
        uCard.addView(UiFactory.divider(ctx));
        uCard.addView(UiFactory.iconSwitchRow(ctx, "📶", "Block connections on metered networks",
                "Blocks traffic when connected to metered cellular data.",
                cfg.blockMetered, new UiFactory.ToggleListener() {
                    @Override
                    public void onToggled(boolean checked) {
                        cfg.blockMetered = checked;
                        host.persistAndReload("Block-Metered=" + checked);
                    }
                }));
        uCard.addView(UiFactory.divider(ctx));
        uCard.addView(UiFactory.iconSwitchRow(ctx, "⚡", "Block UDP (except DNS & NTP)",
                "Blocks non-DNS UDP packets & WebRTC/QUIC tracking leaks.",
                cfg.blockUdpNonDns, new UiFactory.ToggleListener() {
                    @Override
                    public void onToggled(boolean checked) {
                        cfg.blockUdpNonDns = checked;
                        host.persistAndReload("Block-UDP-NonDNS=" + checked);
                    }
                }));
        uCard.addView(UiFactory.divider(ctx));
        uCard.addView(UiFactory.iconSwitchRow(ctx, "🏠", "Block Private LAN access",
                "Blocks connections to RFC1918 local network subnets (10/8, 172.16/12, 192.168/16).",
                cfg.blockLan, new UiFactory.ToggleListener() {
                    @Override
                    public void onToggled(boolean checked) {
                        cfg.blockLan = checked;
                        host.persistAndReload("Block-LAN=" + checked);
                    }
                }));
        uCard.addView(UiFactory.divider(ctx));
        uCard.addView(UiFactory.iconSwitchRow(ctx, "🆕", "Block newly installed apps",
                "Automatically apply strict firewall policy to newly installed packages.",
                cfg.blockNewlyInstalledApps, new UiFactory.ToggleListener() {
                    @Override
                    public void onToggled(boolean checked) {
                        cfg.blockNewlyInstalledApps = checked;
                        host.persistAndReload("Block-Newly-Installed-Apps=" + checked);
                    }
                }));
        col.addView(uCard);

        // Custom Rule Builder
        col.addView(UiFactory.categoryTitle(ctx, "Custom Domain / IP CIDR / Port Rules"));
        LinearLayout rCard = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        final String[] prefixes = new String[]{"BLOCK_DOMAIN:", "ALLOW_DOMAIN:", "BLOCK_IP:", "BLOCK_PORT:", "BLOCK_KEYWORD:"};
        final String[] activePrefix = new String[]{prefixes[0]};
        rCard.addView(UiFactory.buildChipSelector(ctx, prefixes, prefixes[0], UiFactory.COLOR_CRIMSON,
                new UiFactory.ChipSelectListener() {
                    @Override
                    public void onSelected(String value) {
                        activePrefix[0] = value;
                    }
                }));
        final EditText ruleEt = UiFactory.monoInput(ctx, "", "e.g. ads.example.com, 198.51.100.0/24, or 445", false);
        rCard.addView(ruleEt);
        Button addBtn = UiFactory.actionButton(ctx, "+ Add Custom Firewall Rule", UiFactory.COLOR_CARD_SELECTED, UiFactory.COLOR_SKY, UiFactory.COLOR_SKY);
        addBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String t = ruleEt.getText().toString().trim();
                if (t.isEmpty()) return;
                String rule = t.contains(":") ? t : (activePrefix[0] + t);
                if (!cfg.firewallRules.contains(rule)) {
                    cfg.firewallRules.add(0, rule);
                    host.persistAndReload("Added firewall rule " + rule);
                    host.openConfigureSubScreen(SUB_FIREWALL);
                }
            }
        });
        rCard.addView(addBtn);

        for (final String r : cfg.firewallRules) {
            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(UiFactory.dp(ctx, 10), UiFactory.dp(ctx, 8), UiFactory.dp(ctx, 10), UiFactory.dp(ctx, 8));
            row.setBackground(UiFactory.roundedRect(ctx, UiFactory.COLOR_SURFACE, UiFactory.COLOR_BORDER, 0.8f, 10f));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = UiFactory.dp(ctx, 6);
            row.setLayoutParams(lp);

            TextView tv = UiFactory.label(ctx, r, 11.5f,
                    r.startsWith("ALLOW") ? UiFactory.COLOR_EMERALD : UiFactory.COLOR_TEXT, false, true);
            tv.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            TextView del = UiFactory.label(ctx, "✕", 14f, UiFactory.COLOR_CRIMSON, true, false);
            del.setPadding(UiFactory.dp(ctx, 10), 0, UiFactory.dp(ctx, 4), 0);
            del.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    cfg.firewallRules.remove(r);
                    host.persistAndReload("Removed rule " + r);
                    host.openConfigureSubScreen(SUB_FIREWALL);
                }
            });
            row.addView(tv);
            row.addView(del);
            rCard.addView(row);
        }
        col.addView(rCard);

        sv.addView(col);
        return sv;
    }

    // =========================================================================
    // 4. PROXY, WIREGUARD (skidwareproxy), ORBOT, TUNNEL FORGE & PORTS
    // =========================================================================
    private static View buildProxyAndTunnelScreen(final Context ctx, final HostCallbacks host) {
        ScrollView sv = wrapScroll(ctx);
        LinearLayout col = innerCol(ctx);
        final SkidConfig cfg = host.getConfig();

        col.addView(UiFactory.backHeader(ctx, "Proxy & Tunnels",
                "WireGuard, Orbot, SOCKS5, HTTP CONNECT, SSH/Xray/Hysteria/SS & Custom Ports", backToHub(host)));

        // WireGuard Section (Matches Screenshot_20261028_162955)
        col.addView(UiFactory.categoryTitle(ctx, "WireGuard"));
        LinearLayout wgCard = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        String wgStatus = cfg.wgProfileName + ": " + (cfg.wgEnabled ? ("Active (" + cfg.wgEndpoint + ")") : "Ready (tap to configure)");
        wgCard.addView(UiFactory.iconNavRow(ctx, "🐉", "Setup WireGuard",
                wgStatus, cfg.wgEnabled ? UiFactory.COLOR_EMERALD : UiFactory.COLOR_MUTED,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showWireGuardSetupDialog(ctx, cfg, host);
                    }
                }));
        col.addView(wgCard);

        // Orbot Section (Matches Screenshot_20261028_162955)
        col.addView(UiFactory.categoryTitle(ctx, "Orbot"));
        LinearLayout orbotCard = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        final boolean orbotInstalled = isPackageInstalled(ctx, "org.torproject.android");
        orbotCard.addView(UiFactory.iconNavRow(ctx, "🧅", "Setup Orbot",
                orbotInstalled
                        ? ("Orbot detected • Tor enabled=" + cfg.torEnabled + " (SOCKS5 :9050 / DNS :5400)")
                        : ("Tor chain=" + cfg.torEnabled + " • Tap to enable 127.0.0.1:9050 or install Orbot"),
                cfg.torEnabled ? UiFactory.COLOR_EMERALD : UiFactory.COLOR_MUTED,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showOrbotDialog(ctx, cfg, orbotInstalled, host);
                    }
                }));
        col.addView(orbotCard);

        // Other Proxy Section (Matches Screenshot_20261028_162955)
        col.addView(UiFactory.categoryTitle(ctx, "Other"));
        LinearLayout otherCard = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        otherCard.addView(UiFactory.iconSwitchRow(ctx, "⑂", "Setup SOCKS5 Proxy",
                "Forward connections to SOCKS5 endpoint (Local listener :" + cfg.getPrimaryLocalSocksPort() + ").",
                cfg.socks5ForwardEnabled, new UiFactory.ToggleListener() {
                    @Override
                    public void onToggled(boolean checked) {
                        cfg.socks5ForwardEnabled = checked;
                        if (checked) cfg.proxyMode = "SOCKS5";
                        host.persistAndReload("SOCKS5-Forward-Enabled=" + checked);
                    }
                }));
        otherCard.addView(UiFactory.divider(ctx));
        otherCard.addView(UiFactory.iconSwitchRow(ctx, "⇄", "Setup HTTP(S) CONNECT Proxy",
                "Forward connections to HTTP(S) CONNECT endpoint (Local listener :" + cfg.getPrimaryLocalHttpPort() + ").",
                cfg.httpConnectForwardEnabled, new UiFactory.ToggleListener() {
                    @Override
                    public void onToggled(boolean checked) {
                        cfg.httpConnectForwardEnabled = checked;
                        if (checked) cfg.proxyMode = "HTTP";
                        host.persistAndReload("HTTP-Connect-Forward-Enabled=" + checked);
                    }
                }));
        col.addView(otherCard);

        // Proxy Advanced Switches (Matches Screenshot_20261028_174107)
        col.addView(UiFactory.categoryTitle(ctx, "Proxy Advanced"));
        LinearLayout pAdv = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        pAdv.addView(UiFactory.iconSwitchRow(ctx, "↪", "Loopback proxy forwarder apps",
                "When disabled, excludes proxy (SOCKS5, HTTP, DNS) forwarder apps from SkidwareDNS's VPN tunnel.",
                cfg.loopbackProxyForwarderApps, new UiFactory.ToggleListener() {
                    @Override
                    public void onToggled(boolean checked) {
                        cfg.loopbackProxyForwarderApps = checked;
                        host.persistAndReload("Loopback-Proxy-Forwarder-Apps=" + checked);
                    }
                }));
        pAdv.addView(UiFactory.divider(ctx));
        pAdv.addView(UiFactory.iconSwitchRow(ctx, "🎲", "Do not randomize WireGuard listen port",
                "Keep WireGuard listen ports fixed (no randomization) in Advanced mode.",
                cfg.fixedWireGuardListenPort, new UiFactory.ToggleListener() {
                    @Override
                    public void onToggled(boolean checked) {
                        cfg.fixedWireGuardListenPort = checked;
                        host.persistAndReload("Fixed-WireGuard-Listen-Port=" + checked);
                    }
                }));
        pAdv.addView(UiFactory.divider(ctx));
        pAdv.addView(UiFactory.iconSwitchRow(ctx, "⇄", "WireGuard P2P (experimental)",
                "When enabled, allows incoming connections initiated by WireGuard peers.",
                cfg.wireGuardP2p, new UiFactory.ToggleListener() {
                    @Override
                    public void onToggled(boolean checked) {
                        cfg.wireGuardP2p = checked;
                        host.persistAndReload("WireGuard-P2P=" + checked);
                    }
                }));
        pAdv.addView(UiFactory.divider(ctx));
        pAdv.addView(UiFactory.iconSwitchRow(ctx, "🔒", "Proxy Lockdown (experimental)",
                "When enabled, all traffic is forced through Proxy or blocked.",
                cfg.proxyLockdown, new UiFactory.ToggleListener() {
                    @Override
                    public void onToggled(boolean checked) {
                        cfg.proxyLockdown = checked;
                        host.persistAndReload("Proxy-Lockdown=" + checked);
                    }
                }));
        pAdv.addView(UiFactory.divider(ctx));
        pAdv.addView(UiFactory.iconSwitchRow(ctx, "🌊", "Flood WireGuard",
                "Sends additional obfuscated WireGuard UDP packets to make tunnel traffic harder to identify and block.",
                cfg.floodWireGuard, new UiFactory.ToggleListener() {
                    @Override
                    public void onToggled(boolean checked) {
                        cfg.floodWireGuard = checked;
                        host.persistAndReload("Flood-WireGuard=" + checked);
                    }
                }));
        pAdv.addView(UiFactory.divider(ctx));
        pAdv.addView(UiFactory.iconSwitchRow(ctx, "⏳", "Smart Persistent Keepalive",
                "Proactively maintains tunnel connectivity by sending keep-alive probes only when needed.",
                cfg.smartPersistentKeepalive, new UiFactory.ToggleListener() {
                    @Override
                    public void onToggled(boolean checked) {
                        cfg.smartPersistentKeepalive = checked;
                        host.persistAndReload("Smart-Persistent-Keepalive=" + checked);
                    }
                }));
        col.addView(pAdv);

        // Skidware Tunnel Forge & Connect-From & Custom Ports
        col.addView(UiFactory.categoryTitle(ctx, "Tunnel Type & Connect-From (Skidware Forge)"));
        LinearLayout forge = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        forge.addView(UiFactory.sectionHeader(ctx, "Tunnel Type", "SSH / V2Ray-Xray / Hysteria / Shadowsocks"));
        forge.addView(UiFactory.buildChipSelector(ctx, DnsPresets.TUNNEL_TYPES, cfg.tunnel, UiFactory.COLOR_SKY,
                new UiFactory.ChipSelectListener() {
                    @Override
                    public void onSelected(String value) {
                        cfg.tunnel = value;
                        host.persistAndReload("Tunnel=" + value);
                    }
                }));
        forge.addView(UiFactory.sectionHeader(ctx, "Connect-From Mode", "HTTP / TLS / stunnel / DNSTT / Obfs"));
        forge.addView(UiFactory.buildChipSelector(ctx, DnsPresets.CONNECT_FROM_MODES, cfg.connectFromMode, UiFactory.COLOR_EMERALD,
                new UiFactory.ChipSelectListener() {
                    @Override
                    public void onSelected(String value) {
                        cfg.connectFromMode = value;
                        host.persistAndReload("Connect-from-mode=" + value);
                    }
                }));

        forge.addView(UiFactory.label(ctx, "Tunnel Host, Port & SNI:", 12f, UiFactory.COLOR_MUTED, false, false));
        final EditText hostEt = UiFactory.monoInput(ctx, cfg.tunnelHost, "edge.skidware.example", false);
        final EditText portEt = UiFactory.monoInput(ctx, String.valueOf(cfg.tunnelPort), "443", false);
        final EditText sniEt = UiFactory.monoInput(ctx, cfg.sni, "cloudflare.com", false);
        final EditText payloadEt = UiFactory.monoInput(ctx, cfg.httpPayload, "GET / HTTP/1.1[crlf]Host: [sni][crlf][crlf]", true);
        forge.addView(hostEt);
        forge.addView(portEt);
        forge.addView(sniEt);
        forge.addView(UiFactory.label(ctx, "HTTP Proxy / Obfs Payload:", 12f, UiFactory.COLOR_MUTED, false, false));
        forge.addView(payloadEt);

        forge.addView(UiFactory.label(ctx, "VPN PORTS [] / PROXY PORTS [] / DNS PORTS []:", 12f, UiFactory.COLOR_MUTED, false, false));
        final EditText vpnPortsEt = UiFactory.monoInput(ctx, joinCsv(cfg.vpnPorts), "51820, 4500, 500, 1701, 443", false);
        final EditText proxyPortsEt = UiFactory.monoInput(ctx, joinCsv(cfg.proxyPorts), "10808, 10809, 9050, 4444", false);
        final EditText dnsPortsEt = UiFactory.monoInput(ctx, joinCsv(cfg.dnsPorts), "53, 853, 443, 5400", false);
        forge.addView(vpnPortsEt);
        forge.addView(proxyPortsEt);
        forge.addView(dnsPortsEt);

        final TextView probeTv = UiFactory.label(ctx, TunnelEngine.generateNativeTunnelSnippet(cfg), 11f, UiFactory.COLOR_SKY, false, true);

        LinearLayout btnRow = new LinearLayout(ctx);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        Button saveBtn = UiFactory.actionButton(ctx, "Save Tunnel & Ports", UiFactory.COLOR_CARD_SELECTED, UiFactory.COLOR_SKY, UiFactory.COLOR_SKY);
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cfg.tunnelHost = hostEt.getText().toString().trim();
                try { cfg.tunnelPort = Integer.parseInt(portEt.getText().toString().trim()); } catch (Exception ignored) {}
                cfg.sni = sniEt.getText().toString().trim();
                cfg.httpPayload = payloadEt.getText().toString().trim();
                parseCsvInto(cfg.vpnPorts, vpnPortsEt.getText().toString());
                parseCsvInto(cfg.proxyPorts, proxyPortsEt.getText().toString());
                parseCsvInto(cfg.dnsPorts, dnsPortsEt.getText().toString());
                host.persistAndReload("Saved Tunnel, Payload & Custom Ports");
                probeTv.setText(TunnelEngine.generateNativeTunnelSnippet(cfg));
                Toast.makeText(ctx, "Saved Tunnel & Ports", Toast.LENGTH_SHORT).show();
            }
        });
        View sp = new View(ctx);
        sp.setLayoutParams(new LinearLayout.LayoutParams(UiFactory.dp(ctx, 8), 1));
        Button probeBtn = UiFactory.actionButton(ctx, "Probe Endpoint", UiFactory.COLOR_SURFACE, UiFactory.COLOR_EMERALD, UiFactory.COLOR_BORDER);
        probeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cfg.tunnelHost = hostEt.getText().toString().trim();
                try { cfg.tunnelPort = Integer.parseInt(portEt.getText().toString().trim()); } catch (Exception ignored) {}
                cfg.sni = sniEt.getText().toString().trim();
                probeTv.setText("Probing TCP/TLS endpoint...");
                host.getBgExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        final TunnelEngine.ProbeReport rep = TunnelEngine.probeEndpoint(null, cfg);
                        probeTv.post(new Runnable() {
                            @Override
                            public void run() {
                                probeTv.setTextColor(rep.reachable ? UiFactory.COLOR_EMERALD : UiFactory.COLOR_CRIMSON);
                                probeTv.setText(rep.summary + "\n\n" + TunnelEngine.generateNativeTunnelSnippet(cfg));
                            }
                        });
                    }
                });
            }
        });
        btnRow.addView(saveBtn);
        btnRow.addView(sp);
        btnRow.addView(probeBtn);
        forge.addView(btnRow);
        forge.addView(probeTv);
        col.addView(forge);

        sv.addView(col);
        return sv;
    }

    // =========================================================================
    // 5. TCP / IP & ADVANCED UDP TUNING (Screenshots 174059 & 174050 + Expanded TCP/UDP!)
    // =========================================================================
    private static View buildTcpIpUdpScreen(final Context ctx, final HostCallbacks host) {
        ScrollView sv = wrapScroll(ctx);
        LinearLayout col = innerCol(ctx);
        final SkidConfig cfg = host.getConfig();

        col.addView(UiFactory.backHeader(ctx, "TCP / IP & UDP",
                "Socket buffers, Keepalive, EIM NAT, MSS clamping, QUIC & IPv4/IPv6 translation", backToHub(host)));

        // Card 1: Core Rethink TCP / IP Settings (Matches 174059 & 174050)
        col.addView(UiFactory.categoryTitle(ctx, "TCP / IP"));
        LinearLayout tcpCard = UiFactory.card(ctx, UiFactory.COLOR_BORDER);

        tcpCard.addView(UiFactory.iconSwitchRow(ctx, "⏳", "Shorter TCP keepalive",
                "Quickly close TCP sockets with no recent activity.",
                cfg.shorterTcpKeepalive, new UiFactory.ToggleListener() {
                    @Override
                    public void onToggled(boolean checked) {
                        cfg.shorterTcpKeepalive = checked;
                        host.persistAndReload("Shorter-TCP-Keepalive=" + checked);
                    }
                }));
        tcpCard.addView(UiFactory.divider(ctx));

        tcpCard.addView(UiFactory.iconSwitchRow(ctx, "((•))", "Endpoint-Independent mapping",
                "When enabled, UDP sockets maintain a fixed address and port for all destinations.",
                cfg.endpointIndependentMapping, new UiFactory.ToggleListener() {
                    @Override
                    public void onToggled(boolean checked) {
                        cfg.endpointIndependentMapping = checked;
                        host.persistAndReload("Endpoint-Independent-Mapping=" + checked);
                    }
                }));
        tcpCard.addView(UiFactory.divider(ctx));

        final int[] idleSteps = new int[]{0, 15, 30, 60, 120, 300, 600};
        int initIdleIdx = findClosestIndex(idleSteps, cfg.idleTimeoutSec);
        tcpCard.addView(UiFactory.iconSliderRow(ctx, "◌", "Idle timeout",
                "Close idle TCP and UDP sockets after this duration.",
                idleSteps.length, initIdleIdx,
                new UiFactory.SliderValueFormatter() {
                    @Override
                    public String format(int idx) {
                        int v = idleSteps[idx];
                        return v == 0 ? "Disabled" : (v + "s");
                    }
                },
                new UiFactory.SliderChangeListener() {
                    @Override
                    public void onStepSelected(int idx) {
                        cfg.idleTimeoutSec = idleSteps[idx];
                        host.persistAndReload("Idle-Timeout-Sec=" + cfg.idleTimeoutSec);
                    }
                }));
        tcpCard.addView(UiFactory.divider(ctx));

        tcpCard.addView(UiFactory.iconSwitchRow(ctx, "|↔|", "Bandwidth booster",
                "Uses jumbo packets (MTU 1500 & 64 KB buffers) within SkidwareDNS's VPN tunnel.",
                cfg.bandwidthBooster, new UiFactory.ToggleListener() {
                    @Override
                    public void onToggled(boolean checked) {
                        cfg.bandwidthBooster = checked;
                        host.persistAndReload("Bandwidth-Booster=" + checked);
                    }
                }));
        tcpCard.addView(UiFactory.divider(ctx));

        final int[] bufSteps = new int[]{64, 128, 256, 512, 1024, 2048, 4096};
        int initBufIdx = findClosestIndex(bufSteps, cfg.socketBufferSizeKb);
        tcpCard.addView(UiFactory.iconSliderRow(ctx, "⊞", "Socket Buffer Size",
                "Adjust the network buffer size (SO_RCVBUF / SO_SNDBUF) used by SkidwareDNS sockets.",
                bufSteps.length, initBufIdx,
                new UiFactory.SliderValueFormatter() {
                    @Override
                    public String format(int idx) {
                        int kb = bufSteps[idx];
                        return kb >= 1024 ? ((kb / 1024) + " MB") : (kb + " KB");
                    }
                },
                new UiFactory.SliderChangeListener() {
                    @Override
                    public void onStepSelected(int idx) {
                        cfg.socketBufferSizeKb = bufSteps[idx];
                        host.persistAndReload("Socket-Buffer-Size-KB=" + cfg.socketBufferSizeKb);
                    }
                }));
        tcpCard.addView(UiFactory.divider(ctx));

        tcpCard.addView(UiFactory.iconNavRow(ctx, "IP", "Choose IP version",
                cfg.ipVersion, UiFactory.COLOR_MUTED, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showSingleChoiceDialog(ctx, "Choose IP version",
                                new String[]{"IPv4", "IPv6", "Dual-Stack", "Prefer IPv6"},
                                cfg.ipVersion, new UiFactory.ChipSelectListener() {
                                    @Override
                                    public void onSelected(String val) {
                                        cfg.ipVersion = val;
                                        host.persistAndReload("IP-Version=" + val);
                                        host.openConfigureSubScreen(SUB_TCPIP_UDP);
                                    }
                                });
                    }
                }));
        tcpCard.addView(UiFactory.divider(ctx));

        tcpCard.addView(UiFactory.iconSwitchRow(ctx, "⇄", "Perform protocol translations",
                "Translate outbound traffic to the selected IP version on-device (NAT64 / 464XLAT).",
                cfg.performProtocolTranslations, new UiFactory.ToggleListener() {
                    @Override
                    public void onToggled(boolean checked) {
                        cfg.performProtocolTranslations = checked;
                        host.persistAndReload("Perform-Protocol-Translations=" + checked);
                    }
                }));
        tcpCard.addView(UiFactory.divider(ctx));

        tcpCard.addView(UiFactory.iconNavRow(ctx, "⚙", "Custom Interface IPs for VPN",
                "Override default VPN Interface gateway (" + cfg.customTunGatewayV4 + "), router & CIDR addresses.",
                UiFactory.COLOR_MUTED, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showCustomInterfaceIpsDialog(ctx, cfg, host);
                    }
                }));
        col.addView(tcpCard);

        // Card 2: Extra Deep TCP Stack Tuning
        col.addView(UiFactory.categoryTitle(ctx, "Advanced TCP Controls"));
        LinearLayout advTcp = UiFactory.card(ctx, UiFactory.COLOR_BORDER);

        advTcp.addView(UiFactory.iconSwitchRow(ctx, "⚡", "TCP_NODELAY (Disable Nagle's Algorithm)",
                "Flushes small TCP packets immediately without coalescing delay for gaming, SSH & DNS.",
                cfg.tcpNoDelay, new UiFactory.ToggleListener() {
                    @Override
                    public void onToggled(boolean checked) {
                        cfg.tcpNoDelay = checked;
                        host.persistAndReload("TCP-NoDelay=" + checked);
                    }
                }));
        advTcp.addView(UiFactory.divider(ctx));

        advTcp.addView(UiFactory.iconSwitchRow(ctx, "🚀", "TCP Fast Open (TFO)",
                "Enables aggressive SYN payload fast-path optimization where supported by kernel.",
                cfg.tcpFastOpen, new UiFactory.ToggleListener() {
                    @Override
                    public void onToggled(boolean checked) {
                        cfg.tcpFastOpen = checked;
                        host.persistAndReload("TCP-FastOpen=" + checked);
                    }
                }));
        advTcp.addView(UiFactory.divider(ctx));

        advTcp.addView(UiFactory.iconSwitchRow(ctx, "✂", "TCP RST Quick-Close (SO_LINGER = 0)",
                "Immediately recycles closed proxy sockets via RST instead of holding TIME_WAIT state.",
                cfg.tcpRstQuickClose, new UiFactory.ToggleListener() {
                    @Override
                    public void onToggled(boolean checked) {
                        cfg.tcpRstQuickClose = checked;
                        host.persistAndReload("TCP-RST-QuickClose=" + checked);
                    }
                }));
        advTcp.addView(UiFactory.divider(ctx));

        final int[] mssSteps = new int[]{1200, 1280, 1320, 1360, 1400, 1440, 1460};
        advTcp.addView(UiFactory.iconSliderRow(ctx, "📏", "TCP MSS Clamping",
                "Clamps TCP Maximum Segment Size to prevent PMTU blackholes over nested tunnels.",
                mssSteps.length, findClosestIndex(mssSteps, cfg.tcpMssClamp),
                new UiFactory.SliderValueFormatter() {
                    @Override public String format(int idx) { return mssSteps[idx] + " B"; }
                },
                new UiFactory.SliderChangeListener() {
                    @Override
                    public void onStepSelected(int idx) {
                        cfg.tcpMssClamp = mssSteps[idx];
                        host.persistAndReload("TCP-MSS-Clamp=" + cfg.tcpMssClamp);
                    }
                }));
        advTcp.addView(UiFactory.divider(ctx));

        advTcp.addView(UiFactory.iconNavRow(ctx, "🏷", "DSCP / IP_TOS Traffic Class Marking",
                cfg.dscpTrafficClass, UiFactory.COLOR_SKY, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showSingleChoiceDialog(ctx, "DSCP / IP_TOS QoS Class",
                                new String[]{"Default (0x00)", "LowDelay (0x10)", "Throughput (0x08)", "Expedited Forwarding (0xB8)"},
                                cfg.dscpTrafficClass, new UiFactory.ChipSelectListener() {
                                    @Override
                                    public void onSelected(String val) {
                                        cfg.dscpTrafficClass = val;
                                        host.persistAndReload("DSCP-Traffic-Class=" + val);
                                        host.openConfigureSubScreen(SUB_TCPIP_UDP);
                                    }
                                });
                    }
                }));
        col.addView(advTcp);

        // Card 3: Extra Deep UDP Stack Controls
        col.addView(UiFactory.categoryTitle(ctx, "Advanced UDP & QUIC Controls"));
        LinearLayout advUdp = UiFactory.card(ctx, UiFactory.COLOR_BORDER);

        advUdp.addView(UiFactory.iconNavRow(ctx, "🌊", "UDP / QUIC (Port 443) Policy",
                cfg.udpQuicPolicy + " — Block QUIC to force HTTP/3 browsers onto inspectable TCP/TLS",
                UiFactory.COLOR_SKY, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showSingleChoiceDialog(ctx, "UDP / QUIC (Port 443) Policy",
                                new String[]{"Allow", "Block QUIC (UDP:443)", "Force Proxy"},
                                cfg.udpQuicPolicy, new UiFactory.ChipSelectListener() {
                                    @Override
                                    public void onSelected(String val) {
                                        cfg.udpQuicPolicy = val;
                                        host.persistAndReload("UDP-QUIC-Policy=" + val);
                                        host.openConfigureSubScreen(SUB_TCPIP_UDP);
                                    }
                                });
                    }
                }));
        advUdp.addView(UiFactory.divider(ctx));

        final int[] udpTimeouts = new int[]{15, 30, 60, 120, 180, 300};
        advUdp.addView(UiFactory.iconSliderRow(ctx, "⏱", "UDP NAT Session Timeout",
                "Controls lifetime of UDP NAT / STUN / VoIP mapping entries.",
                udpTimeouts.length, findClosestIndex(udpTimeouts, cfg.udpNatTimeoutSec),
                new UiFactory.SliderValueFormatter() {
                    @Override public String format(int idx) { return udpTimeouts[idx] + "s"; }
                },
                new UiFactory.SliderChangeListener() {
                    @Override
                    public void onStepSelected(int idx) {
                        cfg.udpNatTimeoutSec = udpTimeouts[idx];
                        host.persistAndReload("UDP-NAT-Timeout-Sec=" + cfg.udpNatTimeoutSec);
                    }
                }));
        advUdp.addView(UiFactory.divider(ctx));

        advUdp.addView(UiFactory.iconSwitchRow(ctx, "🎲", "UDP Ephemeral Source Port Randomization",
                "Randomizes outbound UDP source port (20000–60000) per query to prevent ISP flow tracking.",
                cfg.udpRandomizeSourcePort, new UiFactory.ToggleListener() {
                    @Override
                    public void onToggled(boolean checked) {
                        cfg.udpRandomizeSourcePort = checked;
                        host.persistAndReload("UDP-Randomize-Source-Port=" + checked);
                    }
                }));
        advUdp.addView(UiFactory.divider(ctx));

        advUdp.addView(UiFactory.iconSwitchRow(ctx, "✓", "Strict RFC 768 / RFC 8200 UDP Checksums",
                "Computes full IPv4/IPv6 pseudo-header UDP checksums and drops malformed zero-checksum IPv6 UDP.",
                cfg.udpStrictChecksum, new UiFactory.ToggleListener() {
                    @Override
                    public void onToggled(boolean checked) {
                        cfg.udpStrictChecksum = checked;
                        host.persistAndReload("UDP-Strict-Checksum=" + checked);
                    }
                }));
        advUdp.addView(UiFactory.divider(ctx));

        advUdp.addView(UiFactory.iconNavRow(ctx, "📦", "UDP DNS EDNS0 Payload Size",
                cfg.udpEdnsPayloadBytes + " bytes (1232B avoids IPv6 fragmentation)", UiFactory.COLOR_MUTED,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showSingleChoiceDialog(ctx, "UDP DNS EDNS0 Payload Size",
                                new String[]{"512", "1232", "1400", "4096"},
                                String.valueOf(cfg.udpEdnsPayloadBytes), new UiFactory.ChipSelectListener() {
                                    @Override
                                    public void onSelected(String val) {
                                        cfg.udpEdnsPayloadBytes = Integer.parseInt(val);
                                        host.persistAndReload("UDP-EDNS-Payload-Bytes=" + val);
                                        host.openConfigureSubScreen(SUB_TCPIP_UDP);
                                    }
                                });
                    }
                }));
        col.addView(advUdp);

        col.addView(UiFactory.categoryTitle(ctx, "TCP handlers (v5.4)"));
        LinearLayout tcp2 = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        tcp2.addView(UiFactory.iconNavRow(ctx, "\u26A1", "Congestion control",
                cfg.tcpCongestion + " (Cubic / BBR / Westwood / Reno)", UiFactory.COLOR_MUTED,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showSingleChoiceDialog(ctx, "TCP congestion",
                                new String[]{"Cubic", "BBR", "Westwood", "Reno"},
                                cfg.tcpCongestion, new UiFactory.ChipSelectListener() {
                                    @Override
                                    public void onSelected(String val) {
                                        cfg.tcpCongestion = val;
                                        host.persistAndReload("TCP-Congestion=" + val);
                                        host.openConfigureSubScreen(SUB_TCPIP_UDP);
                                    }
                                });
                    }
                }));
        tcp2.addView(UiFactory.divider(ctx));
        tcp2.addView(UiFactory.iconNavRow(ctx, "\u23F1", "Connect timeout",
                cfg.tcpConnectTimeoutMs + " ms", UiFactory.COLOR_MUTED,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showSingleChoiceDialog(ctx, "TCP connect timeout",
                                new String[]{"2000", "5000", "8000", "15000", "30000"},
                                String.valueOf(cfg.tcpConnectTimeoutMs), new UiFactory.ChipSelectListener() {
                                    @Override
                                    public void onSelected(String val) {
                                        cfg.tcpConnectTimeoutMs = Integer.parseInt(val);
                                        host.persistAndReload("TCP-ConnectTimeout-Ms=" + val);
                                        host.openConfigureSubScreen(SUB_TCPIP_UDP);
                                    }
                                });
                    }
                }));
        tcp2.addView(UiFactory.divider(ctx));
        tcp2.addView(UiFactory.iconNavRow(ctx, "\u23F3", "Keepalive idle / interval",
                cfg.tcpKeepaliveIdleSec + "s idle \u00b7 " + cfg.tcpKeepaliveIntervalSec + "s probe",
                UiFactory.COLOR_MUTED,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showSingleChoiceDialog(ctx, "TCP keepalive idle (seconds)",
                                new String[]{"15", "30", "45", "60", "120"},
                                String.valueOf(cfg.tcpKeepaliveIdleSec), new UiFactory.ChipSelectListener() {
                                    @Override
                                    public void onSelected(String val) {
                                        cfg.tcpKeepaliveIdleSec = Integer.parseInt(val);
                                        host.persistAndReload("TCP-Keepalive-Idle-Sec=" + val);
                                        host.openConfigureSubScreen(SUB_TCPIP_UDP);
                                    }
                                });
                    }
                }));
        tcp2.addView(UiFactory.divider(ctx));
        tcp2.addView(UiFactory.iconSwitchRow(ctx, "\u21BB", "SO_REUSEADDR",
                "Allow local address reuse on TCP sockets.",
                cfg.tcpReuseAddress, new UiFactory.ToggleListener() {
                    @Override
                    public void onToggled(boolean checked) {
                        cfg.tcpReuseAddress = checked;
                        host.persistAndReload("TCP-ReuseAddress=" + checked);
                    }
                }));
        tcp2.addView(UiFactory.divider(ctx));
        tcp2.addView(UiFactory.iconSwitchRow(ctx, "\u25A6", "TCP_CORK (Nagle hold)",
                "Hold small writes to fill a segment. Disables TCP_NODELAY while on.",
                cfg.tcpCork, new UiFactory.ToggleListener() {
                    @Override
                    public void onToggled(boolean checked) {
                        cfg.tcpCork = checked;
                        host.persistAndReload("TCP-Cork=" + checked);
                    }
                }));
        tcp2.addView(UiFactory.divider(ctx));
        tcp2.addView(UiFactory.iconSwitchRow(ctx, "\u2194", "Window scaling",
                "Hint RFC 1323 window scaling on high-BDP paths.",
                cfg.tcpWindowScaling, new UiFactory.ToggleListener() {
                    @Override
                    public void onToggled(boolean checked) {
                        cfg.tcpWindowScaling = checked;
                        host.persistAndReload("TCP-WindowScaling=" + checked);
                    }
                }));
        tcp2.addView(UiFactory.divider(ctx));
        tcp2.addView(UiFactory.iconNavRow(ctx, "\u23F1", "TCP user timeout",
                cfg.tcpUserTimeoutMs == 0 ? "Off (use idle timeout)" : cfg.tcpUserTimeoutMs + " ms",
                UiFactory.COLOR_MUTED,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showSingleChoiceDialog(ctx, "TCP_USER_TIMEOUT (ms)",
                                new String[]{"0", "5000", "15000", "30000", "60000"},
                                String.valueOf(cfg.tcpUserTimeoutMs), new UiFactory.ChipSelectListener() {
                                    @Override
                                    public void onSelected(String val) {
                                        cfg.tcpUserTimeoutMs = Integer.parseInt(val);
                                        host.persistAndReload("TCP-UserTimeout-Ms=" + val);
                                        host.openConfigureSubScreen(SUB_TCPIP_UDP);
                                    }
                                });
                    }
                }));
        col.addView(tcp2);

        col.addView(UiFactory.categoryTitle(ctx, "UDP handlers (v5.4)"));
        LinearLayout udp2 = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        udp2.addView(UiFactory.iconNavRow(ctx, "\u23F1", "UDP receive timeout",
                cfg.udpRecvTimeoutMs + " ms", UiFactory.COLOR_MUTED,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showSingleChoiceDialog(ctx, "UDP recv timeout (ms)",
                                new String[]{"500", "1000", "3000", "5000", "15000"},
                                String.valueOf(cfg.udpRecvTimeoutMs), new UiFactory.ChipSelectListener() {
                                    @Override
                                    public void onSelected(String val) {
                                        cfg.udpRecvTimeoutMs = Integer.parseInt(val);
                                        host.persistAndReload("UDP-RecvTimeout-Ms=" + val);
                                        host.openConfigureSubScreen(SUB_TCPIP_UDP);
                                    }
                                });
                    }
                }));
        udp2.addView(UiFactory.divider(ctx));
        udp2.addView(UiFactory.iconNavRow(ctx, "\uD83D\uDCE6", "Max datagram",
                cfg.udpMaxDatagramBytes + " bytes", UiFactory.COLOR_MUTED,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showSingleChoiceDialog(ctx, "UDP max datagram",
                                new String[]{"512", "1232", "1400", "1500", "4096"},
                                String.valueOf(cfg.udpMaxDatagramBytes), new UiFactory.ChipSelectListener() {
                                    @Override
                                    public void onSelected(String val) {
                                        cfg.udpMaxDatagramBytes = Integer.parseInt(val);
                                        host.persistAndReload("UDP-MaxDatagram-Bytes=" + val);
                                        host.openConfigureSubScreen(SUB_TCPIP_UDP);
                                    }
                                });
                    }
                }));
        udp2.addView(UiFactory.divider(ctx));
        udp2.addView(UiFactory.iconSwitchRow(ctx, "\u21BB", "SO_REUSEADDR (UDP)",
                "Reuse local UDP bind addresses.",
                cfg.udpReuseAddress, new UiFactory.ToggleListener() {
                    @Override
                    public void onToggled(boolean checked) {
                        cfg.udpReuseAddress = checked;
                        host.persistAndReload("UDP-ReuseAddress=" + checked);
                    }
                }));
        udp2.addView(UiFactory.divider(ctx));
        udp2.addView(UiFactory.iconSwitchRow(ctx, "((•))", "Connected datagrams",
                "connect() UDP sockets to a single peer for faster send/recv.",
                cfg.udpConnectedMode, new UiFactory.ToggleListener() {
                    @Override
                    public void onToggled(boolean checked) {
                        cfg.udpConnectedMode = checked;
                        host.persistAndReload("UDP-ConnectedMode=" + checked);
                    }
                }));
        udp2.addView(UiFactory.divider(ctx));
        udp2.addView(UiFactory.iconSwitchRow(ctx, "\uD83D\uDCE2", "UDP broadcast",
                "Allow SO_BROADCAST on datagram sockets.",
                cfg.udpBroadcast, new UiFactory.ToggleListener() {
                    @Override
                    public void onToggled(boolean checked) {
                        cfg.udpBroadcast = checked;
                        host.persistAndReload("UDP-Broadcast=" + checked);
                    }
                }));
        udp2.addView(UiFactory.divider(ctx));
        udp2.addView(UiFactory.iconSwitchRow(ctx, "\u26A1", "UDP GSO hint",
                "Hint generic segmentation offload on large datagrams (best-effort).",
                cfg.udpGsoHint, new UiFactory.ToggleListener() {
                    @Override
                    public void onToggled(boolean checked) {
                        cfg.udpGsoHint = checked;
                        host.persistAndReload("UDP-GSO-Hint=" + checked);
                    }
                }));
        col.addView(udp2);

        sv.addView(col);
        return sv;
    }

    // =========================================================================
    // 6. ANTI-CENSORSHIP DPI ENGINE (Matches Screenshots 173949 & 173941)
    // =========================================================================
    private static View buildAntiCensorshipScreen(final Context ctx, final HostCallbacks host) {
        ScrollView sv = wrapScroll(ctx);
        LinearLayout col = innerCol(ctx);
        final SkidConfig cfg = host.getConfig();

        col.addView(UiFactory.backHeader(ctx, "Anti-Censorship",
                "Packet fragmentation & TLS ClientHello desynchronization", backToHub(host)));

        col.addView(UiFactory.categoryTitle(ctx, "STRATEGY"));
        col.addView(UiFactory.label(ctx, "Set how retries work for censored connections.", 12.5f, UiFactory.COLOR_MUTED, false, false));
        col.addView(UiFactory.buildRadioCard(ctx, new UiFactory.RadioItem[]{
                new UiFactory.RadioItem("None", "None", "On failure, never retry anti-censorship techniques."),
                new UiFactory.RadioItem("Auto", "Auto", "Apply anti-censorship techniques when necessary."),
                new UiFactory.RadioItem("Always", "Always", "Always apply anti-censorship techniques.")
        }, cfg.dpiStrategy, new UiFactory.ChipSelectListener() {
            @Override
            public void onSelected(String value) {
                cfg.dpiStrategy = value;
                host.persistAndReload("DPI-Strategy=" + value);
            }
        }));

        col.addView(UiFactory.categoryTitle(ctx, "TECHNIQUE"));
        col.addView(UiFactory.label(ctx,
                "Anti-censorship tools can grant internet freedom, but may not work everywhere.",
                12.5f, UiFactory.COLOR_MUTED, false, false));
        col.addView(UiFactory.buildRadioCard(ctx, new UiFactory.RadioItem[]{
                new UiFactory.RadioItem("None", "None", "Do not alter packets."),
                new UiFactory.RadioItem("Auto", "Auto", "Alter packets as necessary."),
                new UiFactory.RadioItem("TCP", "TCP", "Alter TCP packets (segment split)."),
                new UiFactory.RadioItem("TCP/TLS", "TCP/TLS", "Alter TCP or TLS ClientHello packets."),
                new UiFactory.RadioItem("Desync", "Desync", "Multi-split desync & decoy packets as necessary."),
                new UiFactory.RadioItem("Hybrid", "Hybrid", "Also uses SOCKS5/Tunnel proxy fallback as necessary.")
        }, cfg.dpiTechnique, new UiFactory.ChipSelectListener() {
            @Override
            public void onSelected(String value) {
                cfg.dpiTechnique = value;
                host.persistAndReload("DPI-Technique=" + value);
            }
        }));

        LinearLayout splitCard = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        final int[] splitSteps = new int[]{1, 2, 3, 5, 8, 16, 32};
        splitCard.addView(UiFactory.iconSliderRow(ctx, "✂", "TLS ClientHello Split Offset",
                "Byte offset for initial TCP/TLS segment split when DPI bypass is engaged.",
                splitSteps.length, findClosestIndex(splitSteps, cfg.tlsRecordSplitBytes),
                new UiFactory.SliderValueFormatter() {
                    @Override public String format(int idx) { return splitSteps[idx] + " B"; }
                },
                new UiFactory.SliderChangeListener() {
                    @Override
                    public void onStepSelected(int idx) {
                        cfg.tlsRecordSplitBytes = splitSteps[idx];
                        host.persistAndReload("TLS-Record-Split-Bytes=" + cfg.tlsRecordSplitBytes);
                    }
                }));
        col.addView(splitCard);

        sv.addView(col);
        return sv;
    }

    // =========================================================================
    // 7. NETWORK (Matches Screenshots 174115 & 174124)
    // =========================================================================
    private static View buildNetworkScreen(final Context ctx, final HostCallbacks host) {
        ScrollView sv = wrapScroll(ctx);
        LinearLayout col = innerCol(ctx);
        final SkidConfig cfg = host.getConfig();

        col.addView(UiFactory.backHeader(ctx, "Network",
                "VPN tunnel mode, Loopback, Bootstrap DNS & connectivity rules", backToHub(host)));

        col.addView(UiFactory.categoryTitle(ctx, "Network"));
        LinearLayout nCard = UiFactory.card(ctx, UiFactory.COLOR_BORDER);

        nCard.addView(UiFactory.iconSwitchRow(ctx, "☍", "Stall on network loss",
                "When enabled, SkidwareDNS stalls all data when no connectivity is available, like in Airplane Mode.",
                cfg.stallOnNetworkLoss, new UiFactory.ToggleListener() {
                    @Override
                    public void onToggled(boolean checked) {
                        cfg.stallOnNetworkLoss = checked;
                        host.persistAndReload("Stall-On-Network-Loss=" + checked);
                    }
                }));
        nCard.addView(UiFactory.divider(ctx));

        nCard.addView(UiFactory.iconSwitchRow(ctx, "⊞", "Enable network visibility",
                "Allow requesting apps to access all available networks (e.g. Zoom and Meet on-demand bypass).",
                cfg.enableNetworkVisibility, new UiFactory.ToggleListener() {
                    @Override
                    public void onToggled(boolean checked) {
                        cfg.enableNetworkVisibility = checked;
                        host.persistAndReload("Enable-Network-Visibility=" + checked);
                    }
                }));
        nCard.addView(UiFactory.divider(ctx));

        nCard.addView(UiFactory.iconSwitchRow(ctx, "⊠", "Do not route Private IPs",
                "Exclude LAN, loopback, multicast, link-local routes from SkidwareDNS's VPN tunnel.",
                cfg.doNotRoutePrivateIps, new UiFactory.ToggleListener() {
                    @Override
                    public void onToggled(boolean checked) {
                        cfg.doNotRoutePrivateIps = checked;
                        host.persistAndReload("Do-Not-Route-Private-IPs=" + checked);
                    }
                }));
        nCard.addView(UiFactory.divider(ctx));

        nCard.addView(UiFactory.iconSwitchRow(ctx, "⇄", "Use all available networks",
                "When enabled, SkidwareDNS uses all available networks to connect to Internet; otherwise uses active network.",
                cfg.useAllAvailableNetworks, new UiFactory.ToggleListener() {
                    @Override
                    public void onToggled(boolean checked) {
                        cfg.useAllAvailableNetworks = checked;
                        host.persistAndReload("Use-All-Available-Networks=" + checked);
                    }
                }));
        nCard.addView(UiFactory.divider(ctx));

        nCard.addView(UiFactory.iconSwitchRow(ctx, "☁", "Always metered",
                "Mark SkidwareDNS's VPN tunnel as metered to encourage apps to use less data.",
                cfg.alwaysMetered, new UiFactory.ToggleListener() {
                    @Override
                    public void onToggled(boolean checked) {
                        cfg.alwaysMetered = checked;
                        host.persistAndReload("Always-Metered=" + checked);
                    }
                }));
        nCard.addView(UiFactory.divider(ctx));

        nCard.addView(UiFactory.iconSwitchRow(ctx, "⏲", "Meter mobile networks",
                "Only treat mobile networks as metered, rest as unmetered.",
                cfg.meterMobileNetworks, new UiFactory.ToggleListener() {
                    @Override
                    public void onToggled(boolean checked) {
                        cfg.meterMobileNetworks = checked;
                        host.persistAndReload("Meter-Mobile-Networks=" + checked);
                    }
                }));
        col.addView(nCard);

        // Experimental / Bootstrap Card (Matches 174124)
        col.addView(UiFactory.categoryTitle(ctx, "Bootstrap & Routing"));
        LinearLayout bCard = UiFactory.card(ctx, UiFactory.COLOR_BORDER);

        bCard.addView(UiFactory.iconSwitchRow(ctx, "◎", "Loopback (experimental)",
                "Route SkidwareDNS's own traffic, like blocklist downloads, connectivity checks, DNS connections, back into the tunnel.",
                cfg.loopbackExperimental, new UiFactory.ToggleListener() {
                    @Override
                    public void onToggled(boolean checked) {
                        cfg.loopbackExperimental = checked;
                        host.persistAndReload("Loopback-Experimental=" + checked);
                    }
                }));
        bCard.addView(UiFactory.divider(ctx));

        bCard.addView(UiFactory.iconNavRow(ctx, "↩", "Choose Bootstrap DNS",
                "Used only during startup to find the IP address of the encrypted DNS server (" + cfg.bootstrapDns + ").",
                UiFactory.COLOR_SKY, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showSingleChoiceDialog(ctx, "Choose Bootstrap DNS",
                                new String[]{"1.1.1.1:53", "9.9.9.9:53", "8.8.8.8:53", "76.76.2.0:53"},
                                cfg.bootstrapDns, new UiFactory.ChipSelectListener() {
                                    @Override
                                    public void onSelected(String val) {
                                        cfg.bootstrapDns = val;
                                        host.persistAndReload("Bootstrap-DNS=" + val);
                                        host.openConfigureSubScreen(SUB_NETWORK);
                                    }
                                });
                    }
                }));
        bCard.addView(UiFactory.divider(ctx));

        bCard.addView(UiFactory.iconNavRow(ctx, "⚙", "Connection change policy",
                cfg.connectionChangePolicy, UiFactory.COLOR_MUTED, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showSingleChoiceDialog(ctx, "Connection change policy",
                                new String[]{"Auto", "Reconnect", "Flush Sockets", "Strict Hold"},
                                cfg.connectionChangePolicy, new UiFactory.ChipSelectListener() {
                                    @Override
                                    public void onSelected(String val) {
                                        cfg.connectionChangePolicy = val;
                                        host.persistAndReload("Connection-Change-Policy=" + val);
                                        host.openConfigureSubScreen(SUB_NETWORK);
                                    }
                                });
                    }
                }));
        col.addView(bCard);

        sv.addView(col);
        return sv;
    }

    // =========================================================================
    // 8. GENERAL SETTINGS (Matches Screenshot_20261028_174017)
    // =========================================================================
    private static View buildGeneralSettingsScreen(final Context ctx, final HostCallbacks host) {
        ScrollView sv = wrapScroll(ctx);
        LinearLayout col = innerCol(ctx);
        final SkidConfig cfg = host.getConfig();

        col.addView(UiFactory.backHeader(ctx, "Settings",
                "Backup & Restore, Firewall Bubble & Provider Info", backToHub(host)));

        col.addView(UiFactory.categoryTitle(ctx, "General"));
        LinearLayout gCard = UiFactory.card(ctx, UiFactory.COLOR_BORDER);

        gCard.addView(UiFactory.iconNavRow(ctx, "↺", "Backup & Restore",
                "Manually back up or restore app data and [.skid] settings.", UiFactory.COLOR_MUTED,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new AlertDialog.Builder(ctx)
                                .setTitle("Backup & Restore (.skid)")
                                .setItems(new String[]{"Export Backup (.skid)", "Restore from File (.skid)", "Open .skid Studio"},
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface d, int which) {
                                                if (which == 0) host.triggerExportSkid();
                                                else if (which == 1) host.triggerImportSkid();
                                                else host.jumpToMainTab(2);
                                            }
                                        })
                                .show();
                    }
                }));
        gCard.addView(UiFactory.divider(ctx));

        gCard.addView(UiFactory.iconSwitchRow(ctx, "🛎", "Remember uninstalled apps",
                "When enabled, keep app rules for 7 days.",
                cfg.rememberUninstalledApps, new UiFactory.ToggleListener() {
                    @Override
                    public void onToggled(boolean checked) {
                        cfg.rememberUninstalledApps = checked;
                        host.persistAndReload("Remember-Uninstalled-Apps=" + checked);
                    }
                }));
        gCard.addView(UiFactory.divider(ctx));

        gCard.addView(UiFactory.iconSwitchRow(ctx, "◌", "Firewall Bubble",
                "Shows an alert bubble when blocked domains or apps are intercepted.",
                cfg.firewallBubble, new UiFactory.ToggleListener() {
                    @Override
                    public void onToggled(boolean checked) {
                        cfg.firewallBubble = checked;
                        SkidVpnService.syncLoggingFlags(cfg);
                        host.persistAndReload("Firewall-Bubble=" + checked);
                    }
                }));
        gCard.addView(UiFactory.divider(ctx));

        gCard.addView(UiFactory.iconSwitchRow(ctx, "📍", "Show provider information",
                "Enriches DNS & connection logs with ASN / CDN service provider details.",
                cfg.showProviderInfo, new UiFactory.ToggleListener() {
                    @Override
                    public void onToggled(boolean checked) {
                        cfg.showProviderInfo = checked;
                        host.persistAndReload("Show-Provider-Info=" + checked);
                    }
                }));
        col.addView(gCard);

        sv.addView(col);
        return sv;
    }

    // =========================================================================
    // 9. LOGS & PCAP PACKET CAPTURE (Matches Screenshot_20261028_174007)
    // =========================================================================
    private static View buildLogsSettingsScreen(final Context ctx, final HostCallbacks host) {
        ScrollView sv = wrapScroll(ctx);
        LinearLayout col = innerCol(ctx);
        final SkidConfig cfg = host.getConfig();

        col.addView(UiFactory.backHeader(ctx, "Logs & Packet Capture",
                "Wireshark .pcap capture, on-device logging & log retention", backToHub(host)));

        col.addView(UiFactory.categoryTitle(ctx, "Logs"));
        LinearLayout lCard = UiFactory.card(ctx, UiFactory.COLOR_BORDER);

        String pcapSub = cfg.packetCaptureMode + ("Off".equalsIgnoreCase(cfg.packetCaptureMode) ? " (default)"
                : (" • Captured " + PcapRecorder.packetsCaptured.get() + " pkts (" + (PcapRecorder.bytesCaptured.get() / 1024) + " KB)"));
        lCard.addView(UiFactory.iconNavRow(ctx, "⛶", "Packet capture",
                pcapSub, "Off".equalsIgnoreCase(cfg.packetCaptureMode) ? UiFactory.COLOR_MUTED : UiFactory.COLOR_EMERALD,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showSingleChoiceDialog(ctx, "Packet capture (.pcap)",
                                new String[]{"Off", "Headers-Only", "Full-PCAP"},
                                cfg.packetCaptureMode, new UiFactory.ChipSelectListener() {
                                    @Override
                                    public void onSelected(String val) {
                                        cfg.packetCaptureMode = val;
                                        PcapRecorder.startOrUpdate(ctx, val);
                                        host.persistAndReload("Packet-Capture-Mode=" + val);
                                        host.openConfigureSubScreen(SUB_LOGS_SETTINGS);
                                    }
                                });
                    }
                }));
        lCard.addView(UiFactory.divider(ctx));

        lCard.addView(UiFactory.iconSwitchRow(ctx, "∿", "Enable on-device logging",
                "Store DNS, TCP, UDP, and Firewall logs locally on-device.",
                cfg.enableOnDeviceLogging, new UiFactory.ToggleListener() {
                    @Override
                    public void onToggled(boolean checked) {
                        cfg.enableOnDeviceLogging = checked;
                        SkidVpnService.syncLoggingFlags(cfg);
                        host.persistAndReload("Enable-On-Device-Logging=" + checked);
                    }
                }));
        lCard.addView(UiFactory.divider(ctx));

        lCard.addView(UiFactory.iconNavRow(ctx, "📶", "Log level",
                cfg.logLevel, UiFactory.COLOR_MUTED, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showSingleChoiceDialog(ctx, "Log level",
                                new String[]{"Verbose", "Debug", "Info", "Warn", "Error"},
                                cfg.logLevel, new UiFactory.ChipSelectListener() {
                                    @Override
                                    public void onSelected(String val) {
                                        cfg.logLevel = val;
                                        SkidVpnService.syncLoggingFlags(cfg);
                                        host.persistAndReload("Log-Level=" + val);
                                        host.openConfigureSubScreen(SUB_LOGS_SETTINGS);
                                    }
                                });
                    }
                }));
        lCard.addView(UiFactory.divider(ctx));

        lCard.addView(UiFactory.iconNavRow(ctx, "△", "App Logs",
                "For debugging purposes — tap to open live packet & system logs", UiFactory.COLOR_MUTED,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        host.jumpToMainTab(4);
                    }
                }));
        lCard.addView(UiFactory.divider(ctx));

        lCard.addView(UiFactory.iconNavRow(ctx, "📅", "Log Lifespan",
                cfg.logLifespan, UiFactory.COLOR_MUTED, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showSingleChoiceDialog(ctx, "Log Lifespan",
                                new String[]{"1 day", "3 days", "7 days", "30 days"},
                                cfg.logLifespan, new UiFactory.ChipSelectListener() {
                                    @Override
                                    public void onSelected(String val) {
                                        cfg.logLifespan = val;
                                        host.persistAndReload("Log-Lifespan=" + val);
                                        host.openConfigureSubScreen(SUB_LOGS_SETTINGS);
                                    }
                                });
                    }
                }));
        col.addView(lCard);

        // Export .pcap Card
        File pcapFile = PcapRecorder.getCaptureFile(ctx);
        LinearLayout expCard = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        expCard.addView(UiFactory.sectionHeader(ctx, "Export Wireshark .pcap Capture",
                pcapFile != null ? (pcapFile.length() + " bytes ready") : "Enable Packet capture above to record TUN packets"));
        Button expBtn = UiFactory.actionButton(ctx, "💾 Export skidwaredns_capture.pcap",
                UiFactory.COLOR_CARD_SELECTED, UiFactory.COLOR_SKY, UiFactory.COLOR_SKY);
        expBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                host.triggerExportPcap();
            }
        });
        expCard.addView(expBtn);
        col.addView(expCard);

        sv.addView(col);
        return sv;
    }

    // =========================================================================
    // DIALOGS & HELPERS
    // =========================================================================
    private static void showBlocklistPacksDialog(final Context ctx, final SkidConfig cfg, final HostCallbacks host) {
        final String[] packs = SkidFirewallEngine.BLOCKLIST_PACK_NAMES;
        final boolean[] checked = new boolean[packs.length];
        for (int i = 0; i < packs.length; i++) {
            checked[i] = cfg.activeBlocklistPacks.contains(packs[i]);
        }
        new AlertDialog.Builder(ctx)
                .setTitle("On-device blocklists")
                .setMultiChoiceItems(packs, checked, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        checked[which] = isChecked;
                    }
                })
                .setPositiveButton("Apply", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        cfg.activeBlocklistPacks.clear();
                        for (int i = 0; i < packs.length; i++) {
                            if (checked[i]) cfg.activeBlocklistPacks.add(packs[i]);
                        }
                        cfg.onDeviceBlocklistsEnabled = !cfg.activeBlocklistPacks.isEmpty();
                        host.persistAndReload("Updated On-Device Blocklist Packs");
                        host.openConfigureSubScreen(SUB_DNS);
                    }
                })
                .setNeutralButton("Disable All", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        cfg.onDeviceBlocklistsEnabled = false;
                        host.persistAndReload("Disabled On-Device Blocklists");
                        host.openConfigureSubScreen(SUB_DNS);
                    }
                })
                .show();
    }

    private static void showAllowedRecordTypesDialog(final Context ctx, final SkidConfig cfg, final HostCallbacks host) {
        final String[] types = new String[]{"A", "AAAA", "HTTPS", "SVCB", "CNAME", "TXT", "MX", "SRV", "PTR"};
        final boolean[] checked = new boolean[types.length];
        boolean all = cfg.allowedDnsRecords.contains("ALL");
        for (int i = 0; i < types.length; i++) {
            checked[i] = all || cfg.allowedDnsRecords.contains(types[i]);
        }
        new AlertDialog.Builder(ctx)
                .setTitle("Allowed DNS record types")
                .setMultiChoiceItems(types, checked, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        checked[which] = isChecked;
                    }
                })
                .setPositiveButton("Save Selection", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        cfg.allowedDnsRecords.clear();
                        int count = 0;
                        for (int i = 0; i < types.length; i++) {
                            if (checked[i]) {
                                cfg.allowedDnsRecords.add(types[i]);
                                count++;
                            }
                        }
                        if (count == types.length || count == 0) {
                            cfg.allowedDnsRecords.clear();
                            cfg.allowedDnsRecords.add("ALL");
                        }
                        host.persistAndReload("Updated Allowed DNS Record Types");
                        host.openConfigureSubScreen(SUB_DNS);
                    }
                })
                .setNeutralButton("Auto (All)", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        cfg.allowedDnsRecords.clear();
                        cfg.allowedDnsRecords.add("ALL");
                        host.persistAndReload("Allowed DNS Records -> ALL");
                        host.openConfigureSubScreen(SUB_DNS);
                    }
                })
                .show();
    }

    private static void showCustomInterfaceIpsDialog(final Context ctx, final SkidConfig cfg, final HostCallbacks host) {
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        int p = UiFactory.dp(ctx, 16);
        box.setPadding(p, p, p, p);
        box.setBackgroundColor(UiFactory.COLOR_CARD);

        box.addView(UiFactory.label(ctx, "TUN Interface CIDRs (Addresses []):", 12f, UiFactory.COLOR_MUTED, false, false));
        final EditText addrEt = UiFactory.monoInput(ctx, joinCsv(cfg.addresses), "10.213.37.2/24, fd00:534b:4944::2/64", false);
        box.addView(addrEt);

        box.addView(UiFactory.label(ctx, "Virtual IPv4 Gateway / DNS Sinkhole:", 12f, UiFactory.COLOR_MUTED, false, false));
        final EditText gw4Et = UiFactory.monoInput(ctx, cfg.customTunGatewayV4, "10.213.37.1", false);
        box.addView(gw4Et);

        box.addView(UiFactory.label(ctx, "Virtual IPv6 Gateway / DNS Sinkhole:", 12f, UiFactory.COLOR_MUTED, false, false));
        final EditText gw6Et = UiFactory.monoInput(ctx, cfg.customTunGatewayV6, "fd00:534b:4944::1", false);
        box.addView(gw6Et);

        new AlertDialog.Builder(ctx)
                .setTitle("Custom Interface IPs for VPN")
                .setView(box)
                .setPositiveButton("Apply", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        parseCsvInto(cfg.addresses, addrEt.getText().toString());
                        cfg.customTunGatewayV4 = gw4Et.getText().toString().trim();
                        cfg.customTunGatewayV6 = gw6Et.getText().toString().trim();
                        host.persistAndReload("Updated Custom VPN Interface IPs");
                        host.openConfigureSubScreen(SUB_TCPIP_UDP);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private static void showWireGuardSetupDialog(final Context ctx, final SkidConfig cfg, final HostCallbacks host) {
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        int p = UiFactory.dp(ctx, 16);
        box.setPadding(p, p, p, p);
        box.setBackgroundColor(UiFactory.COLOR_CARD);

        box.addView(UiFactory.label(ctx, "WireGuard Profile Name:", 12f, UiFactory.COLOR_MUTED, false, false));
        final EditText nameEt = UiFactory.monoInput(ctx, cfg.wgProfileName, "skidwareproxy", false);
        box.addView(nameEt);

        box.addView(UiFactory.label(ctx, "Peer Endpoint (host:port):", 12f, UiFactory.COLOR_MUTED, false, false));
        final EditText epEt = UiFactory.monoInput(ctx, cfg.wgEndpoint, "engage.cloudflareclient.com:2408", false);
        box.addView(epEt);

        box.addView(UiFactory.label(ctx, "Peer Public Key (Base64):", 12f, UiFactory.COLOR_MUTED, false, false));
        final EditText pkEt = UiFactory.monoInput(ctx, cfg.wgPublicKey, "bmXOC+F1FxEMF9dyiK2H5/1SUtzH0JuVo51h2wPfgyo=", false);
        box.addView(pkEt);

        new AlertDialog.Builder(ctx)
                .setTitle("Setup WireGuard")
                .setView(box)
                .setPositiveButton("Enable Profile", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        cfg.wgProfileName = nameEt.getText().toString().trim();
                        cfg.wgEndpoint = epEt.getText().toString().trim();
                        cfg.wgPublicKey = pkEt.getText().toString().trim();
                        cfg.wgEnabled = true;
                        cfg.tunnel = "WireGuard";
                        host.persistAndReload("Enabled WireGuard [" + cfg.wgProfileName + "]");
                        host.openConfigureSubScreen(SUB_PROXY);
                    }
                })
                .setNeutralButton("Disable", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        cfg.wgEnabled = false;
                        host.persistAndReload("Disabled WireGuard");
                        host.openConfigureSubScreen(SUB_PROXY);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private static void showOrbotDialog(final Context ctx, final SkidConfig cfg, final boolean installed, final HostCallbacks host) {
        String[] actions = installed
                ? new String[]{cfg.torEnabled ? "Disable Orbot Routing" : "Enable Orbot Tor Chain (9050/5400)", "Launch Orbot App"}
                : new String[]{cfg.torEnabled ? "Disable Tor Chain" : "Enable Local Tor Chain (127.0.0.1:9050 / :5400)", "Install Orbot (F-Droid / Play)"};
        new AlertDialog.Builder(ctx)
                .setTitle("Setup Orbot")
                .setItems(actions, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            cfg.torEnabled = !cfg.torEnabled;
                            if (cfg.torEnabled) {
                                cfg.dnsProxy = "Orbot";
                            }
                            host.persistAndReload("Tor enabled=" + cfg.torEnabled);
                            host.openConfigureSubScreen(SUB_PROXY);
                        } else if (installed) {
                            Intent launch = ctx.getPackageManager().getLaunchIntentForPackage("org.torproject.android");
                            if (launch != null) ctx.startActivity(launch);
                        } else {
                            Intent web = new Intent(Intent.ACTION_VIEW, Uri.parse("https://orbot.app/"));
                            ctx.startActivity(web);
                        }
                    }
                })
                .show();
    }

    private static void showSingleChoiceDialog(Context ctx, String title, final String[] items,
                                               String current, final UiFactory.ChipSelectListener onPick) {
        int checked = 0;
        for (int i = 0; i < items.length; i++) {
            if (items[i].equalsIgnoreCase(current)) {
                checked = i;
                break;
            }
        }
        new AlertDialog.Builder(ctx)
                .setTitle(title)
                .setSingleChoiceItems(items, checked, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if (onPick != null) onPick.onSelected(items[which]);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private static View buildEndpointSelectCard(Context ctx, DnsPresets.DnsEndpoint ep,
                                                boolean selected, String decodedInfo, View.OnClickListener onClick) {
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = UiFactory.dp(ctx, 11);
        box.setPadding(pad, pad, pad, pad);
        box.setBackground(UiFactory.roundedRect(ctx,
                selected ? UiFactory.COLOR_CARD_SELECTED : UiFactory.COLOR_SURFACE,
                selected ? UiFactory.COLOR_SKY : UiFactory.COLOR_BORDER,
                selected ? 1.5f : 0.8f, 12f));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = UiFactory.dp(ctx, 8);
        box.setLayoutParams(lp);
        box.setOnClickListener(onClick);

        box.addView(UiFactory.label(ctx, (selected ? "◉ " : "◯ ") + ep.name,
                14f, selected ? UiFactory.COLOR_SKY : UiFactory.COLOR_TEXT, true, false));
        box.addView(UiFactory.label(ctx, ep.subtitle, 12f, UiFactory.COLOR_MUTED, false, false));
        box.addView(UiFactory.label(ctx, ep.value, 10.5f, UiFactory.COLOR_EMERALD, false, true));
        if (decodedInfo != null && !decodedInfo.isEmpty()) {
            box.addView(UiFactory.label(ctx, decodedInfo, 10f, UiFactory.COLOR_AMBER, false, true));
        }
        return box;
    }

    private static void prependServer(SkidConfig cfg, String val) {
        cfg.dnsServers.remove(val);
        cfg.dnsServers.add(0, val);
        while (cfg.dnsServers.size() > 4) cfg.dnsServers.remove(cfg.dnsServers.size() - 1);
    }

    private static boolean isPackageInstalled(Context ctx, String pkg) {
        try {
            ctx.getPackageManager().getPackageInfo(pkg, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private static int findClosestIndex(int[] arr, int val) {
        int best = 0;
        int bestDiff = Math.abs(arr[0] - val);
        for (int i = 1; i < arr.length; i++) {
            int d = Math.abs(arr[i] - val);
            if (d < bestDiff) {
                bestDiff = d;
                best = i;
            }
        }
        return best;
    }

    private static View.OnClickListener navTo(final HostCallbacks host, final int subId) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                host.openConfigureSubScreen(subId);
            }
        };
    }

    private static View.OnClickListener backToHub(final HostCallbacks host) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                host.openConfigureSubScreen(SUB_HUB);
            }
        };
    }

    private static View buildHttpsScreen(final Context ctx, final HostCallbacks host) {
        ScrollView sv = wrapScroll(ctx);
        LinearLayout col = innerCol(ctx);
        final SkidConfig cfg = host.getConfig();

        col.addView(UiFactory.backHeader(ctx, "HTTPS / TLS",
                "SNI, ALPN, TLS 1.2/1.3, session tickets, endpoint-id", backToHub(host)));

        col.addView(UiFactory.categoryTitle(ctx, "Protocol"));
        LinearLayout proto = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        proto.addView(UiFactory.iconNavRow(ctx, "HTTPS", "Minimum TLS",
                cfg.httpsMinTls, UiFactory.COLOR_MUTED,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showSingleChoiceDialog(ctx, "Minimum TLS",
                                new String[]{"TLS 1.2", "TLS 1.2 + 1.3", "TLS 1.3"},
                                cfg.httpsMinTls, new UiFactory.ChipSelectListener() {
                                    @Override
                                    public void onSelected(String val) {
                                        cfg.httpsMinTls = val;
                                        host.persistAndReload("HTTPS-MinTLS=" + val);
                                        host.openConfigureSubScreen(SUB_HTTPS);
                                    }
                                });
                    }
                }));
        proto.addView(UiFactory.divider(ctx));
        proto.addView(UiFactory.iconNavRow(ctx, "\uD83D\uDD17", "ALPN",
                cfg.httpsAlpn, UiFactory.COLOR_MUTED,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showSingleChoiceDialog(ctx, "HTTPS ALPN",
                                new String[]{"h2,http/1.1", "http/1.1", "h2", "h3,h2,http/1.1"},
                                cfg.httpsAlpn, new UiFactory.ChipSelectListener() {
                                    @Override
                                    public void onSelected(String val) {
                                        cfg.httpsAlpn = val;
                                        host.persistAndReload("HTTPS-ALPN=" + val);
                                        host.openConfigureSubScreen(SUB_HTTPS);
                                    }
                                });
                    }
                }));
        proto.addView(UiFactory.divider(ctx));
        proto.addView(UiFactory.iconNavRow(ctx, "\u23F1", "Handshake timeout",
                cfg.httpsTimeoutMs + " ms", UiFactory.COLOR_MUTED,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showSingleChoiceDialog(ctx, "HTTPS timeout (ms)",
                                new String[]{"3000", "5000", "10000", "15000", "30000"},
                                String.valueOf(cfg.httpsTimeoutMs), new UiFactory.ChipSelectListener() {
                                    @Override
                                    public void onSelected(String val) {
                                        cfg.httpsTimeoutMs = Integer.parseInt(val);
                                        host.persistAndReload("HTTPS-Timeout-Ms=" + val);
                                        host.openConfigureSubScreen(SUB_HTTPS);
                                    }
                                });
                    }
                }));
        col.addView(proto);

        col.addView(UiFactory.categoryTitle(ctx, "Identity & sessions"));
        LinearLayout id = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        id.addView(UiFactory.iconSwitchRow(ctx, "HTTPS", "Send SNI",
                "Attach the configured SNI / hostname on the TLS ClientHello.",
                cfg.httpsSniEnabled, new UiFactory.ToggleListener() {
                    @Override
                    public void onToggled(boolean checked) {
                        cfg.httpsSniEnabled = checked;
                        host.persistAndReload("HTTPS-SNI-Enabled=" + checked);
                    }
                }));
        id.addView(UiFactory.divider(ctx));
        id.addView(UiFactory.iconSwitchRow(ctx, "\u2713", "Endpoint identification",
                "Verify the certificate hostname against the SNI peer.",
                cfg.httpsEndpointId, new UiFactory.ToggleListener() {
                    @Override
                    public void onToggled(boolean checked) {
                        cfg.httpsEndpointId = checked;
                        host.persistAndReload("HTTPS-EndpointId=" + checked);
                    }
                }));
        id.addView(UiFactory.divider(ctx));
        id.addView(UiFactory.iconSwitchRow(ctx, "\uD83C\uDF10", "HTTP/2",
                "Offer h2 in ALPN. Off forces HTTP/1.1 when fallback is on.",
                cfg.httpsHttp2, new UiFactory.ToggleListener() {
                    @Override
                    public void onToggled(boolean checked) {
                        cfg.httpsHttp2 = checked;
                        host.persistAndReload("HTTPS-HTTP2=" + checked);
                    }
                }));
        id.addView(UiFactory.divider(ctx));
        id.addView(UiFactory.iconSwitchRow(ctx, "\u21BB", "Fallback HTTP/1.1",
                "If HTTP/2 is off, advertise only http/1.1.",
                cfg.httpsFallbackHttp11, new UiFactory.ToggleListener() {
                    @Override
                    public void onToggled(boolean checked) {
                        cfg.httpsFallbackHttp11 = checked;
                        host.persistAndReload("HTTPS-Fallback-HTTP11=" + checked);
                    }
                }));
        id.addView(UiFactory.divider(ctx));
        id.addView(UiFactory.iconSwitchRow(ctx, "\uD83C\uDFAB", "Session tickets",
                "Allow TLS session resumption tickets.",
                cfg.httpsSessionTickets, new UiFactory.ToggleListener() {
                    @Override
                    public void onToggled(boolean checked) {
                        cfg.httpsSessionTickets = checked;
                        host.persistAndReload("HTTPS-SessionTickets=" + checked);
                    }
                }));
        col.addView(id);

        col.addView(UiFactory.categoryTitle(ctx, "Pin & listen"));
        LinearLayout pin = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        pin.addView(UiFactory.label(ctx, "Optional SHA-256 pin (hex or base64, empty = off):", 12f, UiFactory.COLOR_MUTED, false, false));
        final EditText pinEt = UiFactory.monoInput(ctx, cfg.httpsPinSha256, "deadbeef... or Base64=", false);
        pin.addView(pinEt);
        pin.addView(UiFactory.iconNavRow(ctx, "#", "HTTPS listen port",
                String.valueOf(cfg.httpsListenPort), UiFactory.COLOR_MUTED,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showSingleChoiceDialog(ctx, "HTTPS listen port",
                                new String[]{"0", "10443", "8443", "4443", "443"},
                                String.valueOf(cfg.httpsListenPort), new UiFactory.ChipSelectListener() {
                                    @Override
                                    public void onSelected(String val) {
                                        cfg.httpsListenPort = Integer.parseInt(val);
                                        host.persistAndReload("HTTPS-Listen-Port=" + val);
                                        host.openConfigureSubScreen(SUB_HTTPS);
                                    }
                                });
                    }
                }));
        Button savePin = UiFactory.actionButton(ctx, "Save pin", UiFactory.COLOR_CARD_SELECTED, UiFactory.COLOR_SKY, UiFactory.COLOR_SKY);
        savePin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cfg.httpsPinSha256 = pinEt.getText().toString().trim();
                host.persistAndReload("HTTPS-Pin-SHA256=" + cfg.httpsPinSha256);
                Toast.makeText(ctx, "Saved HTTPS pin", Toast.LENGTH_SHORT).show();
            }
        });
        pin.addView(savePin);
        col.addView(pin);

        col.addView(UiFactory.label(ctx, com.skidwaredns.app.proxy.SocketStack.describe(cfg),
                11.5f, UiFactory.COLOR_SKY, false, true));

        sv.addView(col);
        return sv;
    }

    private static View buildPortsScreen(final Context ctx, final HostCallbacks host) {
        ScrollView sv = wrapScroll(ctx);
        LinearLayout col = innerCol(ctx);
        final SkidConfig cfg = host.getConfig();

        col.addView(UiFactory.backHeader(ctx, "Ports",
                "Allow / block lists, forwarding, knock, hop, bind policy", backToHub(host)));

        col.addView(UiFactory.categoryTitle(ctx, "Open a port"));
        LinearLayout openCard = UiFactory.card(ctx, UiFactory.COLOR_SKY);
        openCard.addView(UiFactory.iconNavRow(ctx, "PORT", "Open Port on this device",
                OpenPortEngine.describeBind(cfg) + " \u2022 catalog of ports 1–10000",
                UiFactory.COLOR_SKY, navTo(host, OpenPortScreens.SUB_OPEN_PORT)));
        col.addView(openCard);

        col.addView(UiFactory.categoryTitle(ctx, "Listen ports"));
        LinearLayout listen = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        listen.addView(UiFactory.label(ctx, "VPN PORTS []  /  PROXY PORTS []  /  DNS PORTS []", 12f, UiFactory.COLOR_MUTED, false, false));
        final EditText vpnPortsEt = UiFactory.monoInput(ctx, joinCsv(cfg.vpnPorts), "51820, 4500, 500, 1701, 443", false);
        final EditText proxyPortsEt = UiFactory.monoInput(ctx, joinCsv(cfg.proxyPorts), "10808, 10809, 9050, 4444", false);
        final EditText dnsPortsEt = UiFactory.monoInput(ctx, joinCsv(cfg.dnsPorts), "53, 853, 443, 5400", false);
        listen.addView(UiFactory.label(ctx, "VPN:", 11f, UiFactory.COLOR_MUTED, false, false));
        listen.addView(vpnPortsEt);
        listen.addView(UiFactory.label(ctx, "Proxy (SOCKS, HTTP, Tor, I2P):", 11f, UiFactory.COLOR_MUTED, false, false));
        listen.addView(proxyPortsEt);
        listen.addView(UiFactory.label(ctx, "DNS:", 11f, UiFactory.COLOR_MUTED, false, false));
        listen.addView(dnsPortsEt);
        col.addView(listen);

        col.addView(UiFactory.categoryTitle(ctx, "Policy"));
        LinearLayout policy = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        policy.addView(UiFactory.label(ctx, "Allow list (empty = all except block). Ranges ok: 1000-2000", 12f, UiFactory.COLOR_MUTED, false, false));
        final EditText allowEt = UiFactory.monoInput(ctx, cfg.portAllowList, "80,443,853,51820", false);
        policy.addView(allowEt);
        policy.addView(UiFactory.label(ctx, "Block list:", 12f, UiFactory.COLOR_MUTED, false, false));
        final EditText blockEt = UiFactory.monoInput(ctx, cfg.portBlockList, "135,137,139,445,1900,5353", false);
        policy.addView(blockEt);
        policy.addView(UiFactory.label(ctx, "Forward map (src>dst):", 12f, UiFactory.COLOR_MUTED, false, false));
        final EditText fwdEt = UiFactory.monoInput(ctx, cfg.portForwardMap, "8080>80, 8443>443", false);
        policy.addView(fwdEt);
        policy.addView(UiFactory.label(ctx, "Port-knock sequence:", 12f, UiFactory.COLOR_MUTED, false, false));
        final EditText knockEt = UiFactory.monoInput(ctx, cfg.portKnockSequence, "7000,8000,9000", false);
        policy.addView(knockEt);
        col.addView(policy);

        col.addView(UiFactory.categoryTitle(ctx, "Ephemeral & hop"));
        LinearLayout eph = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        eph.addView(UiFactory.iconNavRow(ctx, "#", "Ephemeral range",
                cfg.ephemeralPortMin + "\u2013" + cfg.ephemeralPortMax, UiFactory.COLOR_MUTED,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showSingleChoiceDialog(ctx, "Ephemeral min",
                                new String[]{"1024", "10000", "20000", "32768", "49152"},
                                String.valueOf(cfg.ephemeralPortMin), new UiFactory.ChipSelectListener() {
                                    @Override
                                    public void onSelected(String val) {
                                        cfg.ephemeralPortMin = Integer.parseInt(val);
                                        if (cfg.ephemeralPortMax <= cfg.ephemeralPortMin) {
                                            cfg.ephemeralPortMax = Math.min(65535, cfg.ephemeralPortMin + 10000);
                                        }
                                        host.persistAndReload("Ephemeral-Port-Min=" + val);
                                        host.openConfigureSubScreen(SUB_PORTS);
                                    }
                                });
                    }
                }));
        eph.addView(UiFactory.divider(ctx));
        eph.addView(UiFactory.iconSwitchRow(ctx, "\u21BB", "SO_REUSEPORT",
                "Share listen ports across handler sockets.",
                cfg.soReusePort, new UiFactory.ToggleListener() {
                    @Override
                    public void onToggled(boolean checked) {
                        cfg.soReusePort = checked;
                        host.persistAndReload("SO-ReusePort=" + checked);
                    }
                }));
        eph.addView(UiFactory.divider(ctx));
        eph.addView(UiFactory.iconSwitchRow(ctx, "\uD83C\uDFE0", "Bind loopback only",
                "Local SOCKS/HTTP listeners bind 127.0.0.1 instead of 0.0.0.0.",
                cfg.bindLoopbackOnly, new UiFactory.ToggleListener() {
                    @Override
                    public void onToggled(boolean checked) {
                        cfg.bindLoopbackOnly = checked;
                        host.persistAndReload("Bind-Loopback-Only=" + checked);
                    }
                }));
        eph.addView(UiFactory.divider(ctx));
        eph.addView(UiFactory.iconSwitchRow(ctx, "\uD83D\uDD12", "Privileged-port protect",
                "Refuse binds below 1024 unless well-known ports are allowed.",
                cfg.privilegedPortProtect, new UiFactory.ToggleListener() {
                    @Override
                    public void onToggled(boolean checked) {
                        cfg.privilegedPortProtect = checked;
                        host.persistAndReload("Privileged-Port-Protect=" + checked);
                    }
                }));
        eph.addView(UiFactory.divider(ctx));
        eph.addView(UiFactory.iconSwitchRow(ctx, "\u2713", "Allow well-known ports",
                "Permit 22/53/80/123/443/853 when privileged-port protect is on.",
                cfg.allowWellKnownPorts, new UiFactory.ToggleListener() {
                    @Override
                    public void onToggled(boolean checked) {
                        cfg.allowWellKnownPorts = checked;
                        host.persistAndReload("Allow-WellKnown-Ports=" + checked);
                    }
                }));
        eph.addView(UiFactory.divider(ctx));
        eph.addView(UiFactory.iconSwitchRow(ctx, "\uD83C\uDFAF", "Port hop",
                "Randomize outbound dest port inside hop range (UDP/QUIC style).",
                cfg.portHopEnabled, new UiFactory.ToggleListener() {
                    @Override
                    public void onToggled(boolean checked) {
                        cfg.portHopEnabled = checked;
                        host.persistAndReload("Port-Hop-Enabled=" + checked);
                    }
                }));
        eph.addView(UiFactory.divider(ctx));
        eph.addView(UiFactory.label(ctx, "Hop range:", 12f, UiFactory.COLOR_MUTED, false, false));
        final EditText hopEt = UiFactory.monoInput(ctx, cfg.portHopRange, "10000-60000", false);
        eph.addView(hopEt);
        eph.addView(UiFactory.divider(ctx));
        eph.addView(UiFactory.iconSwitchRow(ctx, "\uD83C\uDF10", "Remap DNS :53 \u2192 DoH :443",
                "Rewrite destination port 53 to 443 before the HTTPS handler.",
                cfg.remapDns53toDoH, new UiFactory.ToggleListener() {
                    @Override
                    public void onToggled(boolean checked) {
                        cfg.remapDns53toDoH = checked;
                        host.persistAndReload("Remap-DNS53-to-DoH=" + checked);
                    }
                }));
        col.addView(eph);

        Button save = UiFactory.actionButton(ctx, "Save ports", UiFactory.COLOR_CARD_SELECTED, UiFactory.COLOR_SKY, UiFactory.COLOR_SKY);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                parseCsvInto(cfg.vpnPorts, vpnPortsEt.getText().toString());
                parseCsvInto(cfg.proxyPorts, proxyPortsEt.getText().toString());
                parseCsvInto(cfg.dnsPorts, dnsPortsEt.getText().toString());
                cfg.portAllowList = allowEt.getText().toString().trim();
                cfg.portBlockList = blockEt.getText().toString().trim();
                cfg.portForwardMap = fwdEt.getText().toString().trim();
                cfg.portKnockSequence = knockEt.getText().toString().trim();
                cfg.portHopRange = hopEt.getText().toString().trim();
                host.persistAndReload("Saved Port policy");
                Toast.makeText(ctx, "Saved ports & policy", Toast.LENGTH_SHORT).show();
            }
        });
        col.addView(save);

        col.addView(UiFactory.label(ctx, "SOCKS :" + cfg.getPrimaryLocalSocksPort()
                        + "  HTTP :" + cfg.getPrimaryLocalHttpPort()
                        + "  DNS :" + cfg.getPrimaryDnsPort()
                        + "  HTTPS listen :" + cfg.httpsListenPort,
                11.5f, UiFactory.COLOR_SKY, false, true));

        sv.addView(col);
        return sv;
    }

    private static ScrollView wrapScroll(Context ctx) {
        ScrollView sv = new ScrollView(ctx);
        sv.setFillViewport(true);
        return sv;
    }

    private static LinearLayout innerCol(Context ctx) {
        LinearLayout col = new LinearLayout(ctx);
        col.setOrientation(LinearLayout.VERTICAL);
        int pad = UiFactory.dp(ctx, 14);
        col.setPadding(pad, pad, pad, UiFactory.dp(ctx, 24));
        return col;
    }

    private static String joinCsv(List<String> list) {
        if (list == null || list.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(list.get(i));
        }
        return sb.toString();
    }

    private static void parseCsvInto(List<String> out, String csv) {
        out.clear();
        if (csv == null) return;
        for (String s : csv.split("[,;\\n]")) {
            String t = s.trim();
            if (!t.isEmpty()) out.add(t);
        }
    }
}
