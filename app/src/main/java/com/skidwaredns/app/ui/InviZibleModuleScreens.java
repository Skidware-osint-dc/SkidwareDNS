package com.skidwaredns.app.ui;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.skidwaredns.app.config.SkidConfig;
import com.skidwaredns.app.config.SkidConfigStore;
import com.skidwaredns.app.dns.DnscryptListManager;
import com.skidwaredns.app.dns.DnsUpstreamResolver;
import com.skidwaredns.app.model.DnsPresets;
import com.skidwaredns.app.util.NetDiagnosticsAndMitm;
import com.skidwaredns.app.util.TlsSniInspector;
import com.skidwaredns.app.vpn.SkidAutomationReceiver;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * SkidwareDNS v3.0 UI Screens for:
 *  - LBox DNS Templates & Core / TLS Record Fragmentation + Mixed-Case SNI (SUB_LBOX_TEMPLATES = 10)
 *  - Full InviZible DNSCrypt & Live v3 Public-Resolvers / Relays Browser (SUB_DNSCRYPT_SETTINGS = 11)
 *  - Full Purple I2P (i2pd) Settings & .b32.i2p AddressBook Sync (SUB_PURPLE_I2P = 12)
 *  - Expanded InviZible Tor Settings, Snowflake, Stream Isolation & tor.conf (SUB_TOR_EXPANDED = 13)
 *  - Fast Settings, ARP MITM Security, Script Control, MegaProxy & Lumine Fake-IP (SUB_FAST_AND_SECURITY = 14)
 */
public final class InviZibleModuleScreens {

    public static final int SUB_LBOX_TEMPLATES = 10;
    public static final int SUB_DNSCRYPT_SETTINGS = 11;
    public static final int SUB_PURPLE_I2P = 12;
    public static final int SUB_TOR_EXPANDED = 13;
    public static final int SUB_FAST_AND_SECURITY = 14;

    private static final String[] SNOWFLAKE_METHODS = new String[]{
            "AMP Cache", "Domain Fronting (Fastly)", "SQS (AWS)"
    };

    private static final String[] TOR_BRIDGE_MODES = new String[]{
            "None", "obfs4", "snowflake", "webtunnel", "meek-azure"
    };

    private InviZibleModuleScreens() {}

    // =========================================================================
    // 10. LBOX DNS TEMPLATES & CORE TLS FRAGMENTATION (183002, 183049)
    // =========================================================================
    public static View buildLBoxScreen(final Context ctx, final ConfigureScreens.HostCallbacks host) {
        ScrollView sv = wrapScroll(ctx);
        LinearLayout col = innerCol(ctx);
        final SkidConfig cfg = host.getConfig();

        col.addView(UiFactory.backHeader(ctx, "LBox Templates & TLS Fragmentation",
                "Toggle encrypted DNS templates, Strict Route, TUN Stack, TLS Record Split & Mixed-Case SNI",
                backToHub(host)));

        col.addView(UiFactory.categoryTitle(ctx, "DNS TEMPLATES (LBOX)"));
        LinearLayout tplCard = UiFactory.card(ctx, UiFactory.COLOR_SKY);
        tplCard.addView(UiFactory.label(ctx,
                "Enable one or more LBox DNS templates. Active templates are chained ahead of fallback DNS.",
                12f, UiFactory.COLOR_MUTED, false, false));

        for (int i = 0; i < DnsPresets.LBOX_TEMPLATES.size(); i++) {
            final DnsPresets.DnsEndpoint tpl = DnsPresets.LBOX_TEMPLATES.get(i);
            if (i > 0) tplCard.addView(UiFactory.divider(ctx));

            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(UiFactory.dp(ctx, 2), UiFactory.dp(ctx, 6), UiFactory.dp(ctx, 2), UiFactory.dp(ctx, 6));

            LinearLayout left = new LinearLayout(ctx);
            left.setOrientation(LinearLayout.VERTICAL);
            left.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            LinearLayout titleLine = new LinearLayout(ctx);
            titleLine.setOrientation(LinearLayout.HORIZONTAL);
            titleLine.setGravity(Gravity.CENTER_VERTICAL);
            titleLine.addView(UiFactory.label(ctx, tpl.id, 14.5f, UiFactory.COLOR_TEXT, true, true));

            TextView badge = UiFactory.label(ctx, "Template", 10.5f, UiFactory.COLOR_SKY, true, false);
            badge.setPadding(UiFactory.dp(ctx, 8), UiFactory.dp(ctx, 2), UiFactory.dp(ctx, 8), UiFactory.dp(ctx, 2));
            badge.setBackground(UiFactory.roundedRect(ctx, UiFactory.COLOR_CARD_SELECTED, UiFactory.COLOR_SKY, 0.8f, 8f));
            LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            bLp.leftMargin = UiFactory.dp(ctx, 8);
            badge.setLayoutParams(bLp);
            titleLine.addView(badge);

            left.addView(titleLine);
            left.addView(UiFactory.label(ctx, tpl.name + " • " + tpl.subtitle + " • " + tpl.value, 12f, UiFactory.COLOR_MUTED, false, false));

            final Switch sw = new Switch(ctx);
            sw.setChecked(cfg.lboxTemplates.contains(tpl.id));
            UiFactory.tintSwitch(sw);
            sw.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (sw.isChecked()) {
                        if (!cfg.lboxTemplates.contains(tpl.id)) cfg.lboxTemplates.add(tpl.id);
                    } else {
                        cfg.lboxTemplates.remove(tpl.id);
                    }
                    host.persistAndReload("DNS Template " + tpl.id + "=" + sw.isChecked());
                }
            });

            row.addView(left);
            row.addView(sw);
            tplCard.addView(row);
        }
        col.addView(tplCard);

        // Card 2: Core & TLS Fragmentation (Screenshot 183049_LBox.jpg)
        col.addView(UiFactory.categoryTitle(ctx, "CORE ROUTE, STACK & TLS FRAGMENTATION"));
        LinearLayout fragCard = UiFactory.card(ctx, UiFactory.COLOR_BORDER);

        fragCard.addView(UiFactory.iconSwitchRow(ctx, "⛨", "Strict route",
                "Route 0.0.0.0/1 + 128.0.0.0/1 & ::/1 + 8000::/1 to prevent potential leak problems",
                cfg.strictRoute, new UiFactory.ToggleListener() {
                    @Override
                    public void onToggled(boolean checked) {
                        cfg.strictRoute = checked;
                        host.persistAndReload("Strict route=" + checked);
                    }
                }));
        fragCard.addView(UiFactory.divider(ctx));

        fragCard.addView(UiFactory.sectionHeader(ctx, "TUN Stack", cfg.tunStack));
        fragCard.addView(UiFactory.buildChipSelector(ctx, DnsPresets.TUN_STACKS, cfg.tunStack,
                UiFactory.COLOR_SKY, new UiFactory.ChipSelectListener() {
                    @Override
                    public void onSelected(String value) {
                        cfg.tunStack = value;
                        host.persistAndReload("Stack=" + value);
                    }
                }));
        fragCard.addView(UiFactory.divider(ctx));

        fragCard.addView(UiFactory.iconSwitchRow(ctx, "✂", "TLS Fragment",
                "Fragment TCP packets carrying TLS ClientHello to bypass SNI blocking",
                cfg.tlsFragment, new UiFactory.ToggleListener() {
                    @Override
                    public void onToggled(boolean checked) {
                        cfg.tlsFragment = checked;
                        host.persistAndReload("TLS Fragment=" + checked);
                    }
                }));
        fragCard.addView(UiFactory.divider(ctx));

        fragCard.addView(UiFactory.iconSwitchRow(ctx, "⧉", "TLS Record Fragment",
                "Fragment TLS handshake into multiple 0x16 TLS records right inside SNI hostname",
                cfg.tlsRecordFragment, new UiFactory.ToggleListener() {
                    @Override
                    public void onToggled(boolean checked) {
                        cfg.tlsRecordFragment = checked;
                        host.persistAndReload("TLS Record Fragment=" + checked);
                    }
                }));
        fragCard.addView(UiFactory.divider(ctx));

        final int[] fbValues = new int[]{100, 250, 500, 750, 1000, 1500};
        int initFb = 2;
        for (int i = 0; i < fbValues.length; i++) {
            if (cfg.tlsFallbackDelayMs <= fbValues[i]) { initFb = i; break; }
        }
        fragCard.addView(UiFactory.iconSliderRow(ctx, "⏱", "Fallback delay",
                "Time to wait before falling back when TLS fragmentation is active",
                fbValues.length, initFb,
                new UiFactory.SliderValueFormatter() {
                    @Override public String format(int idx) { return fbValues[idx] + "ms"; }
                },
                new UiFactory.SliderChangeListener() {
                    @Override
                    public void onStepSelected(int idx) {
                        cfg.tlsFallbackDelayMs = fbValues[idx];
                        host.persistAndReload("Fallback delay=" + cfg.tlsFallbackDelayMs + "ms");
                    }
                }));
        fragCard.addView(UiFactory.divider(ctx));

        fragCard.addView(UiFactory.iconSwitchRow(ctx, "Aa", "Mixed-case SNI",
                "Randomize SNI domain casing (e.g. WwW.gOoGle.CoM) to fool DPI — skipped on REALITY",
                cfg.mixedCaseSni, new UiFactory.ToggleListener() {
                    @Override
                    public void onToggled(boolean checked) {
                        cfg.mixedCaseSni = checked;
                        host.persistAndReload("Mixed-case SNI=" + checked);
                    }
                }));

        final TextView previewTv = UiFactory.label(ctx,
                "Tap below to verify binary TLS ClientHello SNI mixed-case + 2-record 0x16 framing.",
                11.5f, UiFactory.COLOR_EMERALD, false, true);
        Button testTlsBtn = UiFactory.actionButton(ctx, "Test TLS ClientHello Shaper",
                UiFactory.COLOR_SURFACE, UiFactory.COLOR_SKY, UiFactory.COLOR_BORDER);
        testTlsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String domain = (cfg.spoofSniEnabled && !cfg.spoofSniHost.isEmpty()) ? cfg.spoofSniHost : "www.google.com";
                byte[] hello = TlsSniInspector.buildSyntheticClientHello(domain);
                TlsSniInspector.applyMixedCaseSniInPlace(hello, hello.length, false);
                TlsSniInspector.SniLocation span = TlsSniInspector.locateClientHelloSni(hello, hello.length);
                String mixed = span != null ? span.hostname : domain;
                byte[] recSplit = TlsSniInspector.fragmentTlsRecord(hello, hello.length, 3);
                previewTv.setText("Original SNI: " + domain + " (" + hello.length + "B)\n"
                        + "Mixed-Case  : " + mixed + "\n"
                        + "2-Record TLS: " + recSplit.length + "B (+5B second 0x16 header)");
            }
        });
        fragCard.addView(testTlsBtn);
        fragCard.addView(previewTv);
        col.addView(fragCard);

        sv.addView(col);
        return sv;
    }

    // =========================================================================
    // 11. FULL INVIZIBLE DNSCRYPT & ANONYMIZED RELAYS (183357–183459)
    // =========================================================================
    public static View buildDnscryptScreen(final Context ctx, final ConfigureScreens.HostCallbacks host) {
        ScrollView sv = wrapScroll(ctx);
        LinearLayout col = innerCol(ctx);
        final SkidConfig cfg = host.getConfig();

        col.addView(UiFactory.backHeader(ctx, "DNSCrypt & Anon Relays",
                "v3 public-resolvers.md + relays.md mirrors, filters, cloaking/forwarding & dnscrypt-proxy.toml",
                backToHub(host)));

        col.addView(UiFactory.categoryTitle(ctx, "LIVE V3 RESOLVERS & ANONYMIZED RELAYS"));
        LinearLayout liveCard = UiFactory.card(ctx, UiFactory.COLOR_SKY);
        final TextView syncStatus = UiFactory.label(ctx,
                "Active Relays (" + cfg.dnscryptRelays.size() + "): " + cfg.dnscryptRelays
                        + "\nMirrors: raw.githubusercontent.com • download.dnscrypt.info • ipv6.download.dnscrypt.info",
                11.5f, UiFactory.COLOR_MUTED, false, true);
        liveCard.addView(syncStatus);

        LinearLayout btnRow = new LinearLayout(ctx);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        Button btnRes = UiFactory.actionButton(ctx, "Fetch Resolvers", UiFactory.COLOR_SURFACE, UiFactory.COLOR_SKY, UiFactory.COLOR_SKY);
        Button btnRel = UiFactory.actionButton(ctx, "Fetch Anon Relays", UiFactory.COLOR_SURFACE, UiFactory.COLOR_EMERALD, UiFactory.COLOR_EMERALD);
        LinearLayout.LayoutParams b1Lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        b1Lp.rightMargin = UiFactory.dp(ctx, 8);
        btnRes.setLayoutParams(b1Lp);
        btnRel.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        btnRow.addView(btnRes);
        btnRow.addView(btnRel);
        liveCard.addView(btnRow);

        btnRes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                syncStatus.setTextColor(UiFactory.COLOR_AMBER);
                syncStatus.setText("Downloading v3/public-resolvers.md across mirrors...");
                host.getBgExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            final List<DnscryptListManager.RemoteStampEntry> entries = DnscryptListManager.fetchPublicResolvers(cfg);
                            syncStatus.post(new Runnable() {
                                @Override
                                public void run() {
                                    syncStatus.setTextColor(UiFactory.COLOR_EMERALD);
                                    syncStatus.setText("Parsed " + entries.size() + " matching v3 resolvers.");
                                    if (!entries.isEmpty()) {
                                        showDnscryptEntryPicker(ctx, "Select DNSCrypt / DoH Resolver", entries, false, cfg, host);
                                    }
                                }
                            });
                        } catch (final Exception e) {
                            syncStatus.post(new Runnable() {
                                @Override public void run() {
                                    syncStatus.setTextColor(UiFactory.COLOR_CRIMSON);
                                    syncStatus.setText("Resolver fetch error: " + e.getMessage());
                                }
                            });
                        }
                    }
                });
            }
        });

        btnRel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                syncStatus.setTextColor(UiFactory.COLOR_AMBER);
                syncStatus.setText("Downloading v3/relays.md (0x81 Anonymized DNSCrypt Relays)...");
                host.getBgExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            final List<DnscryptListManager.RemoteStampEntry> entries = DnscryptListManager.fetchRelays();
                            syncStatus.post(new Runnable() {
                                @Override
                                public void run() {
                                    syncStatus.setTextColor(UiFactory.COLOR_EMERALD);
                                    syncStatus.setText("Parsed " + entries.size() + " Anonymized Relays.");
                                    if (!entries.isEmpty()) {
                                        showDnscryptEntryPicker(ctx, "Toggle Anonymized DNS Relays", entries, true, cfg, host);
                                    }
                                }
                            });
                        } catch (final Exception e) {
                            syncStatus.post(new Runnable() {
                                @Override public void run() {
                                    syncStatus.setTextColor(UiFactory.COLOR_CRIMSON);
                                    syncStatus.setText("Relay fetch error: " + e.getMessage());
                                }
                            });
                        }
                    }
                });
            }
        });
        col.addView(liveCard);

        // Require Servers & Protocol Card (183357–183417)
        col.addView(UiFactory.categoryTitle(ctx, "SERVER REQUIREMENTS & TRANSPORT"));
        LinearLayout reqCard = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        reqCard.addView(UiFactory.iconSwitchRow(ctx, "⚡", "Defend with DNSCrypt",
                "Listen on 127.0.0.1:" + cfg.dnscryptListenPort + " & verify sdns:// stamps",
                cfg.dnscryptModuleEnabled, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean c) { cfg.dnscryptModuleEnabled = c; host.persistAndReload("DNSCrypt=" + c); }
                }));
        reqCard.addView(UiFactory.divider(ctx));
        reqCard.addView(UiFactory.iconNavRow(ctx, "#", "Listen port", String.valueOf(cfg.dnscryptListenPort),
                UiFactory.COLOR_SKY, new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        promptSingleInput(ctx, "DNSCrypt Listen Port", String.valueOf(cfg.dnscryptListenPort), new InputDone() {
                            @Override public void onText(String val) {
                                try { cfg.dnscryptListenPort = Integer.parseInt(val.trim()); } catch (Exception ignored) {}
                                host.persistAndReload("DNSCrypt port=" + cfg.dnscryptListenPort);
                                host.openConfigureSubScreen(SUB_DNSCRYPT_SETTINGS);
                            }
                        });
                    }
                }));
        reqCard.addView(UiFactory.iconSwitchRow(ctx, "◈", "DNSCrypt servers", "Enable 0x01 DNSCrypt protocol servers",
                cfg.dnscryptEnableProto, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean c) { cfg.dnscryptEnableProto = c; host.persistAndReload("DNSCrypt proto=" + c); }
                }));
        reqCard.addView(UiFactory.iconSwitchRow(ctx, "◈", "DoH servers", "Enable 0x02 DNS-over-HTTPS servers",
                cfg.dnscryptEnableDoh, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean c) { cfg.dnscryptEnableDoh = c; host.persistAndReload("DoH proto=" + c); }
                }));
        reqCard.addView(UiFactory.iconSwitchRow(ctx, "◈", "ODoH servers", "Enable 0x05 Oblivious DoH servers",
                cfg.dnscryptEnableOdoh, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean c) { cfg.dnscryptEnableOdoh = c; host.persistAndReload("ODoH proto=" + c); }
                }));
        reqCard.addView(UiFactory.iconSwitchRow(ctx, "4", "IPv4 servers", "Include IPv4 resolvers",
                cfg.dnscryptIpv4Servers, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean c) { cfg.dnscryptIpv4Servers = c; host.persistAndReload("IPv4 resolvers=" + c); }
                }));
        reqCard.addView(UiFactory.iconSwitchRow(ctx, "6", "IPv6 servers", "Include IPv6 resolvers",
                cfg.dnscryptIpv6Servers, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean c) { cfg.dnscryptIpv6Servers = c; host.persistAndReload("IPv6 resolvers=" + c); }
                }));
        reqCard.addView(UiFactory.iconSwitchRow(ctx, "✓", "Require DNSSEC", "Server must support DNSSEC validation",
                cfg.dnscryptRequireDnssec, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean c) { cfg.dnscryptRequireDnssec = c; host.persistAndReload("Require DNSSEC=" + c); }
                }));
        reqCard.addView(UiFactory.iconSwitchRow(ctx, "✓", "Require no log", "Server must not log user queries",
                cfg.dnscryptRequireNoLog, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean c) { cfg.dnscryptRequireNoLog = c; host.persistAndReload("Require nolog=" + c); }
                }));
        reqCard.addView(UiFactory.iconSwitchRow(ctx, "✓", "Require no filter", "Server must not enforce censorship filters",
                cfg.dnscryptRequireNoFilter, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean c) { cfg.dnscryptRequireNoFilter = c; host.persistAndReload("Require nofilter=" + c); }
                }));
        reqCard.addView(UiFactory.iconSwitchRow(ctx, "⇄", "Force TCP", "Always use TCP instead of UDP for DNSCrypt",
                cfg.dnscryptForceTcp, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean c) { cfg.dnscryptForceTcp = c; host.persistAndReload("Force TCP=" + c); }
                }));
        reqCard.addView(UiFactory.iconSwitchRow(ctx, "☍", "Enable Outbound proxy",
                "Route DNSCrypt via SOCKS5 127.0.0.1:" + cfg.dnscryptOutboundProxyPort,
                cfg.dnscryptOutboundProxy, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean c) { cfg.dnscryptOutboundProxy = c; host.persistAndReload("DNSCrypt proxy=" + c); }
                }));
        reqCard.addView(UiFactory.iconSwitchRow(ctx, "3", "HTTP3 / QUIC", "Enable HTTP/3 for DoH & ODoH servers",
                cfg.dnscryptHttp3, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean c) { cfg.dnscryptHttp3 = c; host.persistAndReload("DNSCrypt HTTP3=" + c); }
                }));
        reqCard.addView(UiFactory.iconSwitchRow(ctx, "⊘", "Ignore system DNS", "Always use bootstrap resolvers (" + cfg.bootstrapDns + ")",
                cfg.dnscryptIgnoreSystemDns, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean c) { cfg.dnscryptIgnoreSystemDns = c; host.persistAndReload("Ignore system DNS=" + c); }
                }));
        reqCard.addView(UiFactory.iconSwitchRow(ctx, "64", "DNS64 synthesis", "Prefix: " + cfg.dnscryptDns64Prefix,
                cfg.performProtocolTranslations, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean c) { cfg.performProtocolTranslations = c; host.persistAndReload("DNS64=" + c); }
                }));
        col.addView(reqCard);

        // Filters, Blacklist/Whitelist, Cloaking/Forwarding & Logging (183424–183459)
        col.addView(UiFactory.categoryTitle(ctx, "FILTERS, CLOAKING, FORWARDING & LOGGING"));
        LinearLayout fltCard = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        fltCard.addView(UiFactory.iconSwitchRow(ctx, "⊘", "Block unqualified DNS names",
                "Immediately block single-label queries without dots",
                cfg.dnscryptBlockUnqualified, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean c) { cfg.dnscryptBlockUnqualified = c; host.persistAndReload("Block unqualified=" + c); }
                }));
        fltCard.addView(UiFactory.iconSwitchRow(ctx, "⊘", "Block undelegated DNS names",
                "Locally block .local, .lan, .internal, .home.arpa & RFC1918 reverse zones",
                cfg.dnscryptBlockUndelegated, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean c) { cfg.dnscryptBlockUndelegated = c; host.persistAndReload("Block undelegated=" + c); }
                }));
        fltCard.addView(UiFactory.iconSwitchRow(ctx, "⊘", "Block IPv6 responses",
                "Synthesize empty NOERROR on AAAA queries to prevent IPv6 leaks",
                cfg.dnscryptBlockIpv6, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean c) { cfg.dnscryptBlockIpv6 = c; host.persistAndReload("Block IPv6=" + c); }
                }));
        fltCard.addView(UiFactory.divider(ctx));

        fltCard.addView(UiFactory.iconNavRow(ctx, "✓", "Domain Whitelist (Allowlist)",
                cfg.whitelistDomains.size() + " pattern(s) • Tap to edit or erase", UiFactory.COLOR_EMERALD,
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        showListRuleDialog(ctx, "DNSCrypt Whitelist Domains", "example.com or *.trusted.org",
                                cfg.whitelistDomains, host, SUB_DNSCRYPT_SETTINGS);
                    }
                }));
        fltCard.addView(UiFactory.iconNavRow(ctx, "⊘", "IP Blacklist",
                cfg.ipBlacklist.size() + " IP/CIDR entry(s) • Tap to edit or erase", UiFactory.COLOR_CRIMSON,
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        showListRuleDialog(ctx, "DNSCrypt IP Blacklist", "203.0.113.99 or 198.51.100.0/24",
                                cfg.ipBlacklist, host, SUB_DNSCRYPT_SETTINGS);
                    }
                }));
        fltCard.addView(UiFactory.iconNavRow(ctx, "↪", "Forwarding rules",
                cfg.forwardingRules.size() + " rule(s) • e.g. onion 127.0.0.1:5400", UiFactory.COLOR_SKY,
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        showListRuleDialog(ctx, "DNSCrypt Forwarding Rules", "domain ip:port (e.g. corp.lan 10.0.0.53:53)",
                                cfg.forwardingRules, host, SUB_DNSCRYPT_SETTINGS);
                    }
                }));
        fltCard.addView(UiFactory.iconNavRow(ctx, "🎭", "Cloaking rules (HOSTS / CNAME)",
                cfg.cloakingRules.size() + " rule(s) • e.g. example.org=93.184.216.34", UiFactory.COLOR_SKY,
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        showListRuleDialog(ctx, "DNSCrypt Cloaking Rules", "host=ip_or_cname",
                                cfg.cloakingRules, host, SUB_DNSCRYPT_SETTINGS);
                    }
                }));
        fltCard.addView(UiFactory.iconNavRow(ctx, "∿", "Ignored qtypes",
                cfg.ignoredQtypes.toString(), UiFactory.COLOR_MUTED,
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        showListRuleDialog(ctx, "Ignored Query Types", "DNSKEY, NS, SOA, HTTPS",
                                cfg.ignoredQtypes, host, SUB_DNSCRYPT_SETTINGS);
                    }
                }));
        fltCard.addView(UiFactory.iconSwitchRow(ctx, "⚠", "Suspicious logging (NXDOMAIN)",
                "Flag nonexistent domain responses in Logs tab",
                cfg.dnscryptSuspiciousLogging, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean c) { cfg.dnscryptSuspiciousLogging = c; host.persistAndReload("Suspicious log=" + c); }
                }));
        fltCard.addView(UiFactory.divider(ctx));

        fltCard.addView(UiFactory.iconNavRow(ctx, "✎", "Edit dnscrypt-proxy.toml directly",
                "Inspect or customize generated native TOML configuration", UiFactory.COLOR_SKY,
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        showRawModuleFileEditor(ctx, DnscryptListManager.FILE_DNSCRYPT_TOML, cfg);
                    }
                }));
        fltCard.addView(UiFactory.iconNavRow(ctx, "⟳", "Clear DNSCrypt & Booster cache",
                "Flush cached DNS answers & reset mirror lists", UiFactory.COLOR_AMBER,
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        DnsUpstreamResolver.flushCache();
                        DnscryptListManager.clearModuleCache(ctx, "dnscrypt");
                        Toast.makeText(ctx, "DNSCrypt & DNS Booster cache cleared", Toast.LENGTH_SHORT).show();
                    }
                }));
        col.addView(fltCard);

        sv.addView(col);
        return sv;
    }

    // =========================================================================
    // 12. PURPLE I2P (i2pd) SETTINGS (183540–183612 + .b32.i2p/hosts.txt)
    // =========================================================================
    public static View buildPurpleI2pScreen(final Context ctx, final ConfigureScreens.HostCallbacks host) {
        ScrollView sv = wrapScroll(ctx);
        LinearLayout col = innerCol(ctx);
        final SkidConfig cfg = host.getConfig();

        col.addView(UiFactory.backHeader(ctx, "Purple I2P (i2pd) Settings",
                "Invisible Internet Project router, HTTP/SOCKS/SAM proxies, SSU2/NTCP2 & .i2p AddressBook",
                backToHub(host)));

        col.addView(UiFactory.categoryTitle(ctx, "ROUTER, BANDWIDTH & TRANSPORTS"));
        LinearLayout mainCard = UiFactory.card(ctx, UiFactory.COLOR_PURPLE);
        mainCard.addView(UiFactory.iconSwitchRow(ctx, "🟣", "Access I2P network with Purple I2P",
                "HTTP Proxy 127.0.0.1:" + cfg.i2pHttpProxyPort + " • SOCKS 127.0.0.1:" + cfg.i2pSocksProxyPort,
                cfg.i2pEnabled, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean c) { cfg.i2pEnabled = c; host.persistAndReload("Purple I2P=" + c); }
                }));
        mainCard.addView(UiFactory.divider(ctx));
        mainCard.addView(UiFactory.iconSwitchRow(ctx, "6", "IPv6 communication", "Enable IPv6 peer transports",
                cfg.i2pIpv6, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean c) { cfg.i2pIpv6 = c; host.persistAndReload("I2P IPv6=" + c); }
                }));
        mainCard.addView(UiFactory.iconSwitchRow(ctx, "⊘", "No transit", "Disable transit tunnels for other routers",
                cfg.i2pNoTransit, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean c) { cfg.i2pNoTransit = c; host.persistAndReload("I2P notransit=" + c); }
                }));
        mainCard.addView(UiFactory.iconSwitchRow(ctx, "⊞", "Floodfill mode", "Participate in NetDB distributed hash table",
                cfg.i2pFloodfill, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean c) { cfg.i2pFloodfill = c; host.persistAndReload("I2P floodfill=" + c); }
                }));
        mainCard.addView(UiFactory.iconSwitchRow(ctx, "⚡", "SSU2 UDP transport", "Modern encrypted UDP I2P transport",
                cfg.i2pSsu2, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean c) { cfg.i2pSsu2 = c; host.persistAndReload("I2P SSU2=" + c); }
                }));
        mainCard.addView(UiFactory.iconSwitchRow(ctx, "⇄", "NTCP2 TCP transport", "Obfuscated TCP I2P transport",
                cfg.i2pNtcp2, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean c) { cfg.i2pNtcp2 = c; host.persistAndReload("I2P NTCP2=" + c); }
                }));
        mainCard.addView(UiFactory.iconSliderRow(ctx, "%", "Transit Share (%)",
                "Bandwidth (" + cfg.i2pBandwidthLimitKbps + " KBps) allocated to transit",
                11, Math.min(10, cfg.i2pTransitSharePct / 10),
                new UiFactory.SliderValueFormatter() {
                    @Override public String format(int step) { return (step * 10) + "%"; }
                },
                new UiFactory.SliderChangeListener() {
                    @Override public void onStepSelected(int step) {
                        cfg.i2pTransitSharePct = step * 10;
                        host.persistAndReload("I2P share=" + cfg.i2pTransitSharePct + "%");
                    }
                }));
        col.addView(mainCard);

        col.addView(UiFactory.categoryTitle(ctx, "PROXIES, SAM, LIMITS & .B32.I2P ADDRESSBOOK"));
        LinearLayout pCard = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        pCard.addView(UiFactory.iconSwitchRow(ctx, "🌐", "HTTP proxy (:" + cfg.i2pHttpProxyPort + ")",
                "Outproxy: " + cfg.i2pHttpOutproxyAddr, cfg.i2pHttpProxyEnabled, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean c) { cfg.i2pHttpProxyEnabled = c; host.persistAndReload("I2P HTTP=" + c); }
                }));
        pCard.addView(UiFactory.iconSwitchRow(ctx, "⇄", "SOCKS proxy (:" + cfg.i2pSocksProxyPort + ")",
                "Route .i2p & .b32.i2p eepsites transparently", cfg.i2pSocksProxyEnabled, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean c) { cfg.i2pSocksProxyEnabled = c; host.persistAndReload("I2P SOCKS=" + c); }
                }));
        pCard.addView(UiFactory.iconSwitchRow(ctx, "⌘", "SAM bridge interface (:" + cfg.i2pSamPort + ")",
                "Enable Simple Anonymous Messaging API for I2P apps", cfg.i2pSamEnabled, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean c) { cfg.i2pSamEnabled = c; host.persistAndReload("I2P SAM=" + c); }
                }));
        pCard.addView(UiFactory.iconSwitchRow(ctx, "✓", "UPnP + Verify su3 + Elgamal",
                "Max transit tunnels: " + cfg.i2pMaxTransitTunnels, cfg.i2pReseedVerifySu3, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean c) {
                        cfg.i2pReseedVerifySu3 = c;
                        cfg.i2pUpnp = c;
                        host.persistAndReload("I2P su3/UPnP=" + c);
                    }
                }));
        pCard.addView(UiFactory.divider(ctx));

        final TextView abStatus = UiFactory.label(ctx,
                "AddressBook URL: " + cfg.i2pAddressBookUrl
                        + "\nLoaded .i2p hosts: " + DnscryptListManager.getI2pHostCount(),
                11.5f, UiFactory.COLOR_MUTED, false, true);
        pCard.addView(abStatus);
        Button syncI2pBtn = UiFactory.actionButton(ctx, "Sync .b32.i2p/hosts.txt AddressBook",
                UiFactory.COLOR_SURFACE, UiFactory.COLOR_PURPLE, UiFactory.COLOR_PURPLE);
        syncI2pBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                abStatus.setTextColor(UiFactory.COLOR_AMBER);
                abStatus.setText("Syncing I2P hosts.txt via 127.0.0.1:" + cfg.i2pHttpProxyPort + "...");
                host.getBgExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            final int n = DnscryptListManager.syncI2pAddressBook(cfg);
                            abStatus.post(new Runnable() {
                                @Override
                                public void run() {
                                    abStatus.setTextColor(UiFactory.COLOR_EMERALD);
                                    abStatus.setText("Synced " + n + " .i2p hosts (Total active: " + DnscryptListManager.getI2pHostCount() + ")");
                                }
                            });
                        } catch (final Exception e) {
                            abStatus.post(new Runnable() {
                                @Override
                                public void run() {
                                    abStatus.setTextColor(UiFactory.COLOR_AMBER);
                                    abStatus.setText("Seeded local .i2p AddressBook (" + DnscryptListManager.getI2pHostCount()
                                            + " hosts active; start i2pd :" + cfg.i2pHttpProxyPort + " for live .b32.i2p sync)");
                                }
                            });
                        }
                    }
                });
            }
        });
        pCard.addView(syncI2pBtn);
        pCard.addView(UiFactory.divider(ctx));

        pCard.addView(UiFactory.iconNavRow(ctx, "✎", "Edit i2pd.conf directly",
                "View or customize raw i2pd router config", UiFactory.COLOR_SKY,
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        showRawModuleFileEditor(ctx, DnscryptListManager.FILE_I2PD_CONF, cfg);
                    }
                }));
        pCard.addView(UiFactory.iconNavRow(ctx, "✎", "Edit tunnels.conf directly",
                "Configure client/server I2P eepsite tunnels", UiFactory.COLOR_SKY,
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        showRawModuleFileEditor(ctx, DnscryptListManager.FILE_TUNNELS_CONF, cfg);
                    }
                }));
        pCard.addView(UiFactory.iconNavRow(ctx, "⟳", "Clear Purple I2P cache",
                "Reset NetDB & re-seed core .i2p AddressBook", UiFactory.COLOR_AMBER,
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        DnscryptListManager.clearModuleCache(ctx, "i2p");
                        Toast.makeText(ctx, "Purple I2P cache cleared", Toast.LENGTH_SHORT).show();
                    }
                }));
        col.addView(pCard);

        sv.addView(col);
        return sv;
    }

    // =========================================================================
    // 13. EXPANDED INVIZIBLE TOR SETTINGS (183633–183714)
    // =========================================================================
    public static View buildTorExpandedScreen(final Context ctx, final ConfigureScreens.HostCallbacks host) {
        ScrollView sv = wrapScroll(ctx);
        LinearLayout col = innerCol(ctx);
        final SkidConfig cfg = host.getConfig();

        col.addView(UiFactory.backHeader(ctx, "Tor Onion Router Settings",
                "Exit/Entry nodes, Stream Isolation, Snowflake Rendezvous, Padding & tor.conf",
                backToHub(host)));

        col.addView(UiFactory.categoryTitle(ctx, "ONION CIRCUIT, EXIT COUNTRIES & ISOLATION"));
        LinearLayout torCard = UiFactory.card(ctx, UiFactory.COLOR_EMERALD);
        torCard.addView(UiFactory.iconSwitchRow(ctx, "🧅", "Hide IP with TOR",
                "SOCKS :" + cfg.torSocksPort + " • DNSPort :" + cfg.torDnsPort + " • VirtualNet " + cfg.torVirtualAddrNetwork,
                cfg.torEnabled, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean c) { cfg.torEnabled = c; host.persistAndReload("Tor=" + c); }
                }));
        torCard.addView(UiFactory.divider(ctx));

        torCard.addView(UiFactory.iconNavRow(ctx, "🌍", "Exit Nodes (Country Codes)",
                cfg.torExitNodes.isEmpty() ? "Automatic (All countries)" : cfg.torExitNodes,
                UiFactory.COLOR_SKY, new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        promptSingleInput(ctx, "Tor ExitNodes (e.g. {ch},{is},{se})", cfg.torExitNodes, new InputDone() {
                            @Override public void onText(String val) {
                                cfg.torExitNodes = val.trim();
                                host.persistAndReload("Tor ExitNodes=" + cfg.torExitNodes);
                                host.openConfigureSubScreen(SUB_TOR_EXPANDED);
                            }
                        });
                    }
                }));
        torCard.addView(UiFactory.iconNavRow(ctx, "⊘", "Exclude Exit Nodes",
                cfg.torExcludeExitNodes.isEmpty() ? "None" : cfg.torExcludeExitNodes,
                UiFactory.COLOR_MUTED, new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        promptSingleInput(ctx, "Tor ExcludeExitNodes (e.g. {ru},{cn},{ir})", cfg.torExcludeExitNodes, new InputDone() {
                            @Override public void onText(String val) {
                                cfg.torExcludeExitNodes = val.trim();
                                host.persistAndReload("Tor ExcludeExitNodes updated");
                                host.openConfigureSubScreen(SUB_TOR_EXPANDED);
                            }
                        });
                    }
                }));
        torCard.addView(UiFactory.iconSwitchRow(ctx, "🔒", "Strict Nodes",
                "Never use excluded nodes even when circuit building fails", cfg.torStrictNodes,
                new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean c) { cfg.torStrictNodes = c; host.persistAndReload("Tor StrictNodes=" + c); }
                }));
        torCard.addView(UiFactory.iconSwitchRow(ctx, "⊞", "Isolate By App UID",
                "Separate Tor circuits per Android application UID", cfg.torIsolateUid,
                new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean c) { cfg.torIsolateUid = c; host.persistAndReload("Tor IsolateUID=" + c); }
                }));
        torCard.addView(UiFactory.iconSwitchRow(ctx, "⇄", "Isolate Dest Addr & Dest Port",
                "Prevent cross-domain correlation on shared circuits", cfg.torIsolateDestAddr,
                new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean c) {
                        cfg.torIsolateDestAddr = c;
                        cfg.torIsolateDestPort = c;
                        host.persistAndReload("Tor IsolateDest=" + c);
                    }
                }));
        col.addView(torCard);

        col.addView(UiFactory.categoryTitle(ctx, "SNOWFLAKE, BRIDGES, PADDING & TOR.CONF"));
        LinearLayout sfCard = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        sfCard.addView(UiFactory.sectionHeader(ctx, "Snowflake Rendezvous Method", cfg.torSnowflakeRendezvous));
        sfCard.addView(UiFactory.buildChipSelector(ctx, SNOWFLAKE_METHODS, cfg.torSnowflakeRendezvous,
                UiFactory.COLOR_SKY, new UiFactory.ChipSelectListener() {
                    @Override public void onSelected(String val) {
                        cfg.torSnowflakeRendezvous = val;
                        host.persistAndReload("Snowflake=" + val);
                    }
                }));
        sfCard.addView(UiFactory.divider(ctx));
        sfCard.addView(UiFactory.sectionHeader(ctx, "Pluggable Transport / Bridges", cfg.torBridgeType));
        sfCard.addView(UiFactory.buildChipSelector(ctx, TOR_BRIDGE_MODES, cfg.torBridgeType,
                UiFactory.COLOR_EMERALD, new UiFactory.ChipSelectListener() {
                    @Override public void onSelected(String val) {
                        cfg.torBridgeType = val;
                        host.persistAndReload("Tor Bridge=" + val);
                    }
                }));
        sfCard.addView(UiFactory.divider(ctx));

        sfCard.addView(UiFactory.iconSwitchRow(ctx, "🛡", "Connection Padding",
                "Defend against ISP traffic timing analysis", cfg.torConnectionPadding,
                new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean c) { cfg.torConnectionPadding = c; host.persistAndReload("Tor Padding=" + c); }
                }));
        sfCard.addView(UiFactory.iconSwitchRow(ctx, "📉", "Reduced Connection Padding",
                "Lower mobile data overhead while keeping circuit cover", cfg.torReducedConnectionPadding,
                new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean c) { cfg.torReducedConnectionPadding = c; host.persistAndReload("Tor ReducedPadding=" + c); }
                }));
        sfCard.addView(UiFactory.iconSwitchRow(ctx, "🔥", "Fascist Firewall (80/443 only)",
                "Only connect to Tor Guards reachable on TCP 80 & 443", cfg.torFascistFirewall,
                new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean c) { cfg.torFascistFirewall = c; host.persistAndReload("FascistFirewall=" + c); }
                }));
        sfCard.addView(UiFactory.iconSwitchRow(ctx, "⚡", "Hardware Accel + Avoid Disk Writes",
                "NewCircuitPeriod=" + cfg.torNewCircuitPeriodSec + "s • MaxDirtiness=" + cfg.torMaxCircuitDirtinessSec + "s",
                cfg.torAvoidDiskWrites, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean c) {
                        cfg.torAvoidDiskWrites = c;
                        cfg.torHardwareAccel = c;
                        host.persistAndReload("Tor Disk/HW=" + c);
                    }
                }));
        sfCard.addView(UiFactory.divider(ctx));

        sfCard.addView(UiFactory.iconNavRow(ctx, "✎", "Edit tor.conf directly",
                "Inspect or customize generated native torrc", UiFactory.COLOR_SKY,
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        showRawModuleFileEditor(ctx, DnscryptListManager.FILE_TOR_CONF, cfg);
                    }
                }));
        sfCard.addView(UiFactory.iconNavRow(ctx, "⟳", "Clear Tor state & descriptor cache",
                "Reset cached consensus & microdescriptors", UiFactory.COLOR_AMBER,
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        DnscryptListManager.clearModuleCache(ctx, "tor");
                        Toast.makeText(ctx, "Tor cache cleared", Toast.LENGTH_SHORT).show();
                    }
                }));
        col.addView(sfCard);

        sv.addView(col);
        return sv;
    }

    // =========================================================================
    // 14. FAST SETTINGS, SYSTEM SECURITY, MEGAPROXY & LUMINE (183728–183809 + REPOS)
    // =========================================================================
    public static View buildFastAndSecurityScreen(final Context ctx, final ConfigureScreens.HostCallbacks host) {
        ScrollView sv = wrapScroll(ctx);
        LinearLayout col = innerCol(ctx);
        final SkidConfig cfg = host.getConfig();

        col.addView(UiFactory.backHeader(ctx, "Fast Settings, MITM, MegaProxy & Lumine",
                "Autostart, Spoof SNI, ARP MITM, Script Control, Multi-Hop Jump, Fake-IP & Hysteria2",
                backToHub(host)));

        // 1. InviZible Fast Settings + Spoof SNI (183754)
        col.addView(UiFactory.categoryTitle(ctx, "INVIZIBLE FAST SETTINGS & SPOOF SNI"));
        LinearLayout fastCard = UiFactory.card(ctx, UiFactory.COLOR_SKY);
        fastCard.addView(UiFactory.iconSwitchRow(ctx, "🚀", "Autostart DNSCrypt on Boot",
                "Delay: " + cfg.autostartDelaySec + "s after BOOT_COMPLETED", cfg.autostartDnscrypt,
                new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean c) { cfg.autostartDnscrypt = c; host.persistAndReload("Autostart DNSCrypt=" + c); }
                }));
        fastCard.addView(UiFactory.iconSwitchRow(ctx, "🚀", "Autostart Tor on Boot",
                "Start Onion routing automatically at boot", cfg.autostartTor,
                new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean c) { cfg.autostartTor = c; host.persistAndReload("Autostart Tor=" + c); }
                }));
        fastCard.addView(UiFactory.iconSwitchRow(ctx, "🚀", "Autostart Purple I2P on Boot",
                "Start i2pd router automatically at boot", cfg.autostartI2p,
                new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean c) { cfg.autostartI2p = c; host.persistAndReload("Autostart I2P=" + c); }
                }));
        fastCard.addView(UiFactory.divider(ctx));

        fastCard.addView(UiFactory.iconSwitchRow(ctx, "🎭", "Spoof SNI",
                "Active SNI Decoy: " + cfg.spoofSniHost, cfg.spoofSniEnabled,
                new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean c) { cfg.spoofSniEnabled = c; host.persistAndReload("Spoof SNI=" + c); }
                }));
        fastCard.addView(UiFactory.buildChipSelector(ctx, DnsPresets.SPOOF_SNI_HOSTS, cfg.spoofSniHost,
                UiFactory.COLOR_SKY, new UiFactory.ChipSelectListener() {
                    @Override public void onSelected(String val) {
                        cfg.spoofSniHost = val;
                        cfg.spoofSniEnabled = true;
                        host.persistAndReload("Spoof SNI=" + val);
                    }
                }));
        col.addView(fastCard);

        // 2. System Security: ARP Spoofing MITM + WakeLock + Script Control (183809)
        col.addView(UiFactory.categoryTitle(ctx, "SYSTEM SECURITY & SHELL SCRIPT CONTROL"));
        LinearLayout secCard = UiFactory.card(ctx, UiFactory.COLOR_CRIMSON);
        secCard.addView(UiFactory.iconSwitchRow(ctx, "🛡", "ARP spoofing detection",
                "Monitor /proc/net/arp for duplicate gateway MAC MITM attacks", cfg.arpSpoofingDetection,
                new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean c) { cfg.arpSpoofingDetection = c; host.persistAndReload("ARP MITM=" + c); }
                }));
        final TextView arpResultTv = UiFactory.label(ctx, "Tap below to audit /proc/net/arp table right now.",
                11.5f, UiFactory.COLOR_MUTED, false, true);
        Button arpBtn = UiFactory.actionButton(ctx, "Audit /proc/net/arp Now",
                UiFactory.COLOR_SURFACE, UiFactory.COLOR_SKY, UiFactory.COLOR_BORDER);
        arpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String r = NetDiagnosticsAndMitm.runArpSpoofingAudit();
                arpResultTv.setTextColor(r.startsWith("MITM") ? UiFactory.COLOR_CRIMSON : UiFactory.COLOR_EMERALD);
                arpResultTv.setText(r);
            }
        });
        secCard.addView(arpBtn);
        secCard.addView(arpResultTv);
        secCard.addView(UiFactory.divider(ctx));

        secCard.addView(UiFactory.iconSwitchRow(ctx, "🔋", "Prevent device sleep (WakeLock)",
                "Hold PARTIAL_WAKE_LOCK while tunnel is active to prevent Doze drops", cfg.preventDeviceSleep,
                new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean c) { cfg.preventDeviceSleep = c; host.persistAndReload("WakeLock=" + c); }
                }));
        secCard.addView(UiFactory.iconSwitchRow(ctx, "⌘", "Enable script control (am broadcast)",
                "Accept SHELL_SCRIPT_CONTROL intents for Tasker / Termux / adb", cfg.enableScriptControl,
                new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean c) { cfg.enableScriptControl = c; host.persistAndReload("ScriptControl=" + c); }
                }));
        Button copyCliBtn = UiFactory.actionButton(ctx, "Copy CLI Broadcast Commands",
                UiFactory.COLOR_SURFACE, UiFactory.COLOR_EMERALD, UiFactory.COLOR_BORDER);
        copyCliBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager cm = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null) {
                    cm.setPrimaryClip(ClipData.newPlainText("SkidwareDNS CLI", SkidAutomationReceiver.buildExampleShellScript()));
                    Toast.makeText(ctx, "Copied SHELL_SCRIPT_CONTROL commands!", Toast.LENGTH_SHORT).show();
                }
            }
        });
        secCard.addView(copyCliBtn);
        col.addView(secCard);

        // 3. AndroidMegaProxy Multi-Hop Jump, Ownership, JA3 & Live Exit-IP/Trace Benchmark
        col.addView(UiFactory.categoryTitle(ctx, "ANDROIDMEGAPROXY MULTI-HOP & DIAGNOSTICS"));
        LinearLayout megaCard = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        megaCard.addView(UiFactory.iconSwitchRow(ctx, "⛓", "Multi-Hop HTTPS / SSH Jump Host",
                cfg.jumpProtocol + "://" + (cfg.jumpHost.isEmpty() ? "(set jump host)" : cfg.jumpHost) + ":" + cfg.jumpPort,
                cfg.jumpHostEnabled, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean c) { cfg.jumpHostEnabled = c; host.persistAndReload("JumpHost=" + c); }
                }));
        megaCard.addView(UiFactory.iconNavRow(ctx, "✎", "Configure Jump Host & Knock Host",
                "Jump=" + (cfg.jumpHost.isEmpty() ? "none" : cfg.jumpHost) + " • Knock=" + cfg.proxyKnockHost,
                UiFactory.COLOR_SKY, new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        promptSingleInput(ctx, "Jump Host (host:port)", cfg.jumpHost + ":" + cfg.jumpPort, new InputDone() {
                            @Override public void onText(String val) {
                                DnsPresets.HostPort hp = DnsPresets.parseHostPort(val, 443);
                                cfg.jumpHost = hp.host;
                                cfg.jumpPort = hp.port;
                                cfg.jumpHostEnabled = !hp.host.isEmpty();
                                host.persistAndReload("JumpHost=" + hp.host + ":" + hp.port);
                                host.openConfigureSubScreen(SUB_FAST_AND_SECURITY);
                            }
                        });
                    }
                }));
        megaCard.addView(UiFactory.sectionHeader(ctx, "App Ownership Validation (Android 10+ UID)", cfg.appOwnershipCheck));
        megaCard.addView(UiFactory.buildChipSelector(ctx, DnsPresets.APP_OWNERSHIP_LEVELS, cfg.appOwnershipCheck,
                UiFactory.COLOR_SKY, new UiFactory.ChipSelectListener() {
                    @Override public void onSelected(String val) {
                        cfg.appOwnershipCheck = val;
                        host.persistAndReload("OwnershipCheck=" + val);
                    }
                }));
        megaCard.addView(UiFactory.sectionHeader(ctx, "TLS / QUIC JA3 Fingerprint Preset", cfg.ja3Fingerprint));
        megaCard.addView(UiFactory.buildChipSelector(ctx, DnsPresets.JA3_FINGERPRINTS, cfg.ja3Fingerprint,
                UiFactory.COLOR_EMERALD, new UiFactory.ChipSelectListener() {
                    @Override public void onSelected(String val) {
                        cfg.ja3Fingerprint = val;
                        host.persistAndReload("JA3=" + val);
                    }
                }));

        final TextView diagTv = UiFactory.label(ctx, "Run live Cloudflare cdn-cgi/trace + 3-sample TTFB + Knock-Host test.",
                11.5f, UiFactory.COLOR_MUTED, false, true);
        Button diagBtn = UiFactory.actionButton(ctx, "Run MegaProxy Exit-IP & TTFB Diagnostics",
                UiFactory.COLOR_SURFACE, UiFactory.COLOR_SKY, UiFactory.COLOR_SKY);
        diagBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                diagTv.setTextColor(UiFactory.COLOR_AMBER);
                diagTv.setText("Probing Cloudflare cdn-cgi/trace & knockHost (" + cfg.proxyKnockHost + ")...");
                host.getBgExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        final String rep = NetDiagnosticsAndMitm.runMegaProxyDiagnostics(cfg);
                        diagTv.post(new Runnable() {
                            @Override
                            public void run() {
                                diagTv.setTextColor(UiFactory.COLOR_EMERALD);
                                diagTv.setText(rep);
                            }
                        });
                    }
                });
            }
        });
        megaCard.addView(diagBtn);
        megaCard.addView(diagTv);
        col.addView(megaCard);

        // 4. Lumine Fake-IP, Subscription Importer & Evozi Hysteria2
        col.addView(UiFactory.categoryTitle(ctx, "LUMINE FAKE-IP, SUBSCRIPTIONS & HYSTERIA2"));
        LinearLayout lumCard = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        lumCard.addView(UiFactory.iconSwitchRow(ctx, "⚡", "Lumine Fake-IP DNS Pool (198.18.0.0/15)",
                "Zero-RTT synthetic A replies + reverse domain mapping (" + NetDiagnosticsAndMitm.getFakeIpPoolSize() + " active)",
                cfg.fakeIpEnabled, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean c) { cfg.fakeIpEnabled = c; host.persistAndReload("Fake-IP=" + c); }
                }));
        lumCard.addView(UiFactory.iconSwitchRow(ctx, "🌐", "GFWList Intelligent Rule Split",
                "Split domestic vs censored domains automatically (" + cfg.routingSplitPreset + ")",
                "GFWList-Split".equalsIgnoreCase(cfg.routingSplitPreset),
                new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean c) {
                        cfg.routingSplitPreset = c ? "GFWList-Split" : "Global";
                        host.persistAndReload("RoutingSplit=" + cfg.routingSplitPreset);
                    }
                }));
        lumCard.addView(UiFactory.iconNavRow(ctx, "🔋", "OEM Background Keep-Alive Helper",
                "Open Android Battery Optimization exemption settings", UiFactory.COLOR_AMBER,
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        cfg.preventDeviceSleep = true;
                        host.persistAndReload("OEM KeepAlive enabled");
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                ctx.startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
                            }
                        } catch (Exception ignored) {
                            Toast.makeText(ctx, "OEM Keep-Alive policy recorded in .skid", Toast.LENGTH_SHORT).show();
                        }
                    }
                }));
        lumCard.addView(UiFactory.divider(ctx));

        String initialSubUrl = cfg.subscriptionUrls.isEmpty() ? DnsPresets.DNSCRYPT_RESOLVER_SOURCES[0] : cfg.subscriptionUrls.get(0);
        final EditText subEt = UiFactory.monoInput(ctx, initialSubUrl,
                "https://example.com/sub.skid or Clash YAML URL", false);
        final TextView subStatus = UiFactory.label(ctx, "Supports remote .skid profiles, Clash YAML rules, or server lists.",
                11.5f, UiFactory.COLOR_MUTED, false, true);
        Button subBtn = UiFactory.actionButton(ctx, "Import / Refresh Subscription URL",
                UiFactory.COLOR_SURFACE, UiFactory.COLOR_EMERALD, UiFactory.COLOR_EMERALD);
        subBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String url = subEt.getText().toString().trim();
                if (url.isEmpty()) return;
                subStatus.setTextColor(UiFactory.COLOR_AMBER);
                subStatus.setText("Fetching subscription from " + url + "...");
                host.getBgExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            final String msg = NetDiagnosticsAndMitm.refreshSubscriptionUrl(url, cfg);
                            if (!cfg.subscriptionUrls.contains(url)) cfg.subscriptionUrls.add(0, url);
                            SkidConfigStore.saveActive(ctx, cfg);
                            subStatus.post(new Runnable() {
                                @Override
                                public void run() {
                                    subStatus.setTextColor(UiFactory.COLOR_EMERALD);
                                    subStatus.setText(msg);
                                    host.persistAndReload("Subscription synced");
                                }
                            });
                        } catch (final Exception e) {
                            subStatus.post(new Runnable() {
                                @Override public void run() {
                                    subStatus.setTextColor(UiFactory.COLOR_CRIMSON);
                                    subStatus.setText("Subscription error: " + e.getMessage());
                                }
                            });
                        }
                    }
                });
            }
        });
        lumCard.addView(subEt);
        lumCard.addView(subBtn);
        lumCard.addView(subStatus);
        lumCard.addView(UiFactory.divider(ctx));

        lumCard.addView(UiFactory.iconNavRow(ctx, "🚀", "Evozi Hysteria2 / SlowUDP Parameters",
                "Up " + cfg.hysteriaUpMbps + " Mbps • Down " + cfg.hysteriaDownMbps + " Mbps • Ports " + cfg.hysteriaPortHopRange,
                UiFactory.COLOR_SKY, new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        promptSingleInput(ctx, "Hysteria2 Salamander Obfs Password", cfg.hysteriaObfsPassword, new InputDone() {
                            @Override public void onText(String val) {
                                cfg.hysteriaObfsPassword = val.trim();
                                cfg.tunnel = "Hysteria2";
                                host.persistAndReload("Hysteria2 Salamander updated");
                                host.openConfigureSubScreen(SUB_FAST_AND_SECURITY);
                            }
                        });
                    }
                }));
        col.addView(lumCard);

        sv.addView(col);
        return sv;
    }

    // =========================================================================
    // DIALOG HELPERS
    // =========================================================================
    private interface InputDone {
        void onText(String val);
    }

    private static void promptSingleInput(Context ctx, String title, String current, final InputDone cb) {
        final EditText et = UiFactory.monoInput(ctx, current, "", false);
        new AlertDialog.Builder(ctx)
                .setTitle(title)
                .setView(et)
                .setPositiveButton("Apply", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        cb.onText(et.getText().toString());
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private static void showDnscryptEntryPicker(final Context ctx, String title,
                                                final List<DnscryptListManager.RemoteStampEntry> entries,
                                                final boolean isRelayList,
                                                final SkidConfig cfg,
                                                final ConfigureScreens.HostCallbacks host) {
        int max = Math.min(entries.size(), 40);
        final String[] labels = new String[max];
        final boolean[] checked = new boolean[max];
        for (int i = 0; i < max; i++) {
            DnscryptListManager.RemoteStampEntry e = entries.get(i);
            labels[i] = e.name + " [" + e.decoded.summaryBadges() + "]";
            checked[i] = isRelayList ? cfg.dnscryptRelays.contains(e.stamp) : cfg.dnsServers.contains(e.stamp);
        }

        new AlertDialog.Builder(ctx)
                .setTitle(title + " (" + entries.size() + " available)")
                .setMultiChoiceItems(labels, checked, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        checked[which] = isChecked;
                    }
                })
                .setPositiveButton("Save to .skid", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        for (int i = 0; i < checked.length; i++) {
                            DnscryptListManager.RemoteStampEntry e = entries.get(i);
                            if (isRelayList) {
                                if (checked[i] && !cfg.dnscryptRelays.contains(e.stamp)) cfg.dnscryptRelays.add(e.stamp);
                                if (!checked[i]) cfg.dnscryptRelays.remove(e.stamp);
                            } else {
                                if (checked[i] && !cfg.dnsServers.contains(e.stamp)) cfg.dnsServers.add(0, e.stamp);
                                if (!checked[i]) cfg.dnsServers.remove(e.stamp);
                            }
                        }
                        host.persistAndReload(isRelayList ? "Updated DNSCrypt Relays" : "Updated DNSCrypt Resolvers");
                        host.openConfigureSubScreen(SUB_DNSCRYPT_SETTINGS);
                    }
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private static void showListRuleDialog(final Context ctx, String title, String hint,
                                           final List<String> targetList,
                                           final ConfigureScreens.HostCallbacks host,
                                           final int reopenSub) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < targetList.size(); i++) {
            if (i > 0) sb.append('\n');
            sb.append(targetList.get(i));
        }
        final EditText et = UiFactory.monoInput(ctx, sb.toString(), hint, true);
        et.setMinLines(6);

        new AlertDialog.Builder(ctx)
                .setTitle(title + " (one per line)")
                .setView(et)
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        targetList.clear();
                        for (String line : et.getText().toString().split("\\r?\\n")) {
                            String t = line.trim();
                            if (!t.isEmpty()) targetList.add(t);
                        }
                        host.persistAndReload("Saved " + targetList.size() + " rule(s)");
                        host.openConfigureSubScreen(reopenSub);
                    }
                })
                .setNeutralButton("Erase All", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        targetList.clear();
                        host.persistAndReload("Erased rules");
                        host.openConfigureSubScreen(reopenSub);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private static void showRawModuleFileEditor(final Context ctx, final String fileName, final SkidConfig cfg) {
        String content = DnscryptListManager.loadOrGenerateModuleFile(ctx, fileName, cfg);
        final EditText et = UiFactory.monoInput(ctx, content, fileName, true);
        et.setMinLines(12);

        new AlertDialog.Builder(ctx)
                .setTitle("Edit " + fileName)
                .setView(et)
                .setPositiveButton("Save File", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        DnscryptListManager.saveModuleFile(ctx, fileName, et.getText().toString());
                        Toast.makeText(ctx, "Saved " + fileName, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNeutralButton("Regenerate from .skid", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        String fresh;
                        if (DnscryptListManager.FILE_DNSCRYPT_TOML.equals(fileName)) {
                            fresh = DnscryptListManager.generateDnscryptToml(cfg);
                        } else if (DnscryptListManager.FILE_TOR_CONF.equals(fileName)) {
                            fresh = DnscryptListManager.generateTorConf(cfg);
                        } else if (DnscryptListManager.FILE_I2PD_CONF.equals(fileName)) {
                            fresh = DnscryptListManager.generateI2pdConf(cfg);
                        } else {
                            fresh = DnscryptListManager.generateTunnelsConf(cfg);
                        }
                        DnscryptListManager.saveModuleFile(ctx, fileName, fresh);
                        Toast.makeText(ctx, "Regenerated " + fileName + " from .skid", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private static ScrollView wrapScroll(Context ctx) {
        ScrollView sv = new ScrollView(ctx);
        sv.setFillViewport(true);
        sv.setBackgroundColor(UiFactory.COLOR_BG);
        return sv;
    }

    private static LinearLayout innerCol(Context ctx) {
        LinearLayout col = new LinearLayout(ctx);
        col.setOrientation(LinearLayout.VERTICAL);
        int p = UiFactory.dp(ctx, 16);
        col.setPadding(p, p, p, UiFactory.dp(ctx, 28));
        return col;
    }

    private static View.OnClickListener backToHub(final ConfigureScreens.HostCallbacks host) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                host.openConfigureSubScreen(ConfigureScreens.SUB_HUB);
            }
        };
    }
}
