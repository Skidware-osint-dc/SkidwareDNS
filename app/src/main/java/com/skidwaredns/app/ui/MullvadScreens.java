package com.skidwaredns.app.ui;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
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
import com.skidwaredns.app.tunnel.MullvadRelayDirectory;

import java.util.List;

/**
 * SkidwareDNS v5.1 Mullvad Relay Directory, DAITA, Multihop & Bridge UI Screen (SUB_MULLVAD_RELAYS = 24):
 *  - Renders expandable Mullvad Relay Cards matching Screenshots 215141–215713:
 *    ee-tll-wg-101..103 (Tallinn, Estonia 100 Gbps DAITA), ch-zrh-wg-001..003 (Zurich 20 Gbps Owned),
 *    ch-zrh-wg-201..202 (Zurich 10 Gbps PrivateLayer Rented), and ch-zrh-br-001 (Zurich SSH/V2Ray Bridge)
 *  - True WireGuard Multihop builder (Entry IPv4 + Exit Multihop Port + Exit PublicKey)
 *  - DAITA (Defense Against AI-guided Traffic Analysis) toggle & Mullvad SOCKS5 :1080 toggle
 *  - Live Mullvad API Sync (https://api.mullvad.net/www/relays/all/) + Live SSH/Relay Probe
 */
public final class MullvadScreens {

    public static final int SUB_MULLVAD_RELAYS = 24;

    private static final int COLOR_MULLVAD_GREEN = Color.parseColor("#2ECC40");
    private static final int COLOR_MULLVAD_HEADER = Color.parseColor("#18222F");
    private static final int COLOR_MULLVAD_BODY = Color.parseColor("#11151E");

    private static int activeFilterIdx = 0; // 0=ALL, 1=DAITA, 2=OWNED, 3=100G, 4=BRIDGES

    private MullvadScreens() {}

    public static View buildMullvadRelaysScreen(final Context ctx, final ConfigureScreens.HostCallbacks host) {
        ScrollView sv = new ScrollView(ctx);
        sv.setFillViewport(true);
        sv.setBackgroundColor(UiFactory.COLOR_BG);

        LinearLayout col = new LinearLayout(ctx);
        col.setOrientation(LinearLayout.VERTICAL);
        int pad = UiFactory.dp(ctx, 16);
        col.setPadding(pad, pad, pad, UiFactory.dp(ctx, 28));
        final SkidConfig cfg = host.getConfig();

        col.addView(UiFactory.backHeader(ctx, "Mullvad Relays, DAITA & Multihop",
                MullvadRelayDirectory.formatActiveSummary(cfg),
                new View.OnClickListener() {
                    @Override public void onClick(View v) { host.openConfigureSubScreen(ConfigureScreens.SUB_HUB); }
                }));

        // Top Active Mullvad Control & Multihop Card
        LinearLayout ctrlCard = UiFactory.card(ctx, COLOR_MULLVAD_GREEN);
        ctrlCard.addView(UiFactory.label(ctx, "Active Mullvad Route", 12f, COLOR_MULLVAD_GREEN, true, false));
        ctrlCard.addView(UiFactory.label(ctx, MullvadRelayDirectory.formatActiveSummary(cfg),
                14.5f, UiFactory.COLOR_TEXT, true, true));
        ctrlCard.addView(UiFactory.divider(ctx));

        ctrlCard.addView(UiFactory.iconSwitchRow(ctx, "☍", "WireGuard Multihop (Entry → Exit)",
                "Connects to Entry IPv4 on Exit's Multihop Port (" +
                        MullvadRelayDirectory.findByHostname(cfg.mullvadEntryRelay).hostname + " → " +
                        MullvadRelayDirectory.findByHostname(cfg.mullvadExitRelay).hostname + ")",
                cfg.mullvadMultihop, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean c) {
                        cfg.mullvadMultihop = c;
                        String sum = MullvadRelayDirectory.applyToConfig(cfg);
                        host.persistAndReload("Mullvad Multihop=" + c + " -> " + sum);
                        host.openConfigureSubScreen(SUB_MULLVAD_RELAYS);
                    }
                }));

        ctrlCard.addView(UiFactory.iconSwitchRow(ctx, "🛡", "DAITA (Defense Against AI Traffic Analysis)",
                "Maybenot uniform 1280B packet padding & anti-fingerprinting bursts (supported on Tallinn 100G relays)",
                cfg.mullvadDaita, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean c) {
                        cfg.mullvadDaita = c;
                        host.persistAndReload("Mullvad DAITA=" + c);
                    }
                }));

        ctrlCard.addView(UiFactory.iconSwitchRow(ctx, "⇄", "Chain Mullvad Exit SOCKS5 Proxy (:1080)",
                "Routes TCP streams via *-wg-socks5-*.relays.mullvad.net:1080 to minimize captcha prompts & isolate DNS",
                cfg.mullvadUseSocks5, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean c) {
                        cfg.mullvadUseSocks5 = c;
                        MullvadRelayDirectory.applyToConfig(cfg);
                        host.persistAndReload("Mullvad SOCKS5=" + c);
                    }
                }));

        LinearLayout topBtns = new LinearLayout(ctx);
        topBtns.setOrientation(LinearLayout.HORIZONTAL);
        topBtns.setPadding(0, UiFactory.dp(ctx, 8), 0, 0);
        Button btnConf = UiFactory.actionButton(ctx, "WireGuard .conf",
                UiFactory.COLOR_SURFACE, COLOR_MULLVAD_GREEN, COLOR_MULLVAD_GREEN);
        Button btnSync = UiFactory.actionButton(ctx, "↻ Sync Live API",
                UiFactory.COLOR_SURFACE, UiFactory.COLOR_SKY, UiFactory.COLOR_SKY);
        LinearLayout.LayoutParams halfLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        halfLp.rightMargin = UiFactory.dp(ctx, 6);
        btnConf.setLayoutParams(halfLp);
        btnSync.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        topBtns.addView(btnConf);
        topBtns.addView(btnSync);
        ctrlCard.addView(topBtns);

        final TextView syncStatusTv = UiFactory.label(ctx,
                "Loaded " + MullvadRelayDirectory.getAllRelays().size() + " verified relays (Tallinn 100G DAITA + Zurich WG/Bridge).",
                11.5f, UiFactory.COLOR_MUTED, false, true);
        ctrlCard.addView(syncStatusTv);
        col.addView(ctrlCard);

        btnConf.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                final String conf = MullvadRelayDirectory.generateMullvadNativeConf(cfg);
                final EditText et = UiFactory.monoInput(ctx, conf, "", true);
                et.setMinLines(14);
                new AlertDialog.Builder(ctx)
                        .setTitle("Mullvad WireGuard / Multihop .conf")
                        .setView(et)
                        .setPositiveButton("Copy .conf", new android.content.DialogInterface.OnClickListener() {
                            @Override public void onClick(android.content.DialogInterface d, int w) {
                                copyText(ctx, "Mullvad .conf", conf);
                            }
                        })
                        .setNegativeButton("Close", null)
                        .show();
            }
        });

        btnSync.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                syncStatusTv.setTextColor(UiFactory.COLOR_AMBER);
                syncStatusTv.setText("Syncing https://api.mullvad.net/www/relays/all/ ...");
                host.getBgExecutor().execute(new Runnable() {
                    @Override public void run() {
                        try {
                            final int total = MullvadRelayDirectory.syncFromLiveMullvadApi();
                            syncStatusTv.post(new Runnable() {
                                @Override public void run() {
                                    Toast.makeText(ctx, "Synced " + total + " live Mullvad relays!", Toast.LENGTH_SHORT).show();
                                    host.openConfigureSubScreen(SUB_MULLVAD_RELAYS);
                                }
                            });
                        } catch (final Exception e) {
                            syncStatusTv.post(new Runnable() {
                                @Override public void run() {
                                    syncStatusTv.setTextColor(UiFactory.COLOR_CRIMSON);
                                    syncStatusTv.setText("Sync error: " + e.getMessage());
                                }
                            });
                        }
                    }
                });
            }
        });

        // Filter Bar
        LinearLayout filterRow = new LinearLayout(ctx);
        filterRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams frLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        frLp.bottomMargin = UiFactory.dp(ctx, 12);
        filterRow.setLayoutParams(frLp);

        String[] fNames = new String[]{"ALL", "DAITA", "OWNED", "100G", "BRIDGE"};
        for (int i = 0; i < fNames.length; i++) {
            final int fIdx = i;
            boolean sel = activeFilterIdx == i;
            TextView chip = UiFactory.label(ctx, fNames[i], 11f,
                    sel ? UiFactory.COLOR_BG : UiFactory.COLOR_TEXT, true, false);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(UiFactory.dp(ctx, 8), UiFactory.dp(ctx, 6), UiFactory.dp(ctx, 8), UiFactory.dp(ctx, 6));
            chip.setBackground(UiFactory.roundedRect(ctx,
                    sel ? COLOR_MULLVAD_GREEN : UiFactory.COLOR_SURFACE,
                    sel ? COLOR_MULLVAD_GREEN : UiFactory.COLOR_BORDER, 1f, 8f));
            LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            if (i < fNames.length - 1) cLp.rightMargin = UiFactory.dp(ctx, 5);
            chip.setLayoutParams(cLp);
            chip.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    activeFilterIdx = fIdx;
                    host.openConfigureSubScreen(SUB_MULLVAD_RELAYS);
                }
            });
            filterRow.addView(chip);
        }
        col.addView(filterRow);

        // Render Mullvad Relay Cards (exact layout & fields from Screenshots 215141–215713)
        List<MullvadRelayDirectory.MullvadRelay> all = MullvadRelayDirectory.getAllRelays();
        int shown = 0;
        for (final MullvadRelayDirectory.MullvadRelay r : all) {
            if (activeFilterIdx == 1 && !r.daita) continue;
            if (activeFilterIdx == 2 && !r.ownedByMullvad) continue;
            if (activeFilterIdx == 3 && r.speedGbps < 100) continue;
            if (activeFilterIdx == 4 && !r.isBridge()) continue;

            col.addView(buildSingleRelayCard(ctx, host, cfg, r));
            shown++;
            if (shown >= 35) break; // Keep scroll smooth when 100+ live relays are synced
        }

        sv.addView(col);
        return sv;
    }

    private static View buildSingleRelayCard(final Context ctx, final ConfigureScreens.HostCallbacks host,
                                             final SkidConfig cfg, final MullvadRelayDirectory.MullvadRelay r) {
        boolean isSelectedExit = r.hostname.equalsIgnoreCase(cfg.mullvadExitRelay);
        boolean isSelectedEntry = cfg.mullvadMultihop && r.hostname.equalsIgnoreCase(cfg.mullvadEntryRelay);
        int borderCol = isSelectedExit ? COLOR_MULLVAD_GREEN
                : (isSelectedEntry ? UiFactory.COLOR_SKY : UiFactory.COLOR_BORDER);

        LinearLayout outer = new LinearLayout(ctx);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setBackground(UiFactory.roundedRect(ctx, COLOR_MULLVAD_BODY, borderCol,
                (isSelectedExit || isSelectedEntry) ? 1.5f : 1f, 14f));
        LinearLayout.LayoutParams oLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        oLp.bottomMargin = UiFactory.dp(ctx, 14);
        outer.setLayoutParams(oLp);

        // Header Band (Green plug circle + hostname + City, Country Flag + provider / Gbps + Owned/Rented)
        LinearLayout hdr = new LinearLayout(ctx);
        hdr.setOrientation(LinearLayout.HORIZONTAL);
        hdr.setGravity(Gravity.CENTER_VERTICAL);
        int hp = UiFactory.dp(ctx, 14);
        hdr.setPadding(hp, hp, hp, hp);
        hdr.setBackground(UiFactory.roundedRect(ctx, COLOR_MULLVAD_HEADER, Color.TRANSPARENT, 0f, 13f));

        TextView plugDot = UiFactory.label(ctx, "🔌", 14f, Color.WHITE, true, false);
        plugDot.setGravity(Gravity.CENTER);
        int dotSz = UiFactory.dp(ctx, 32);
        LinearLayout.LayoutParams dLp = new LinearLayout.LayoutParams(dotSz, dotSz);
        dLp.rightMargin = UiFactory.dp(ctx, 12);
        plugDot.setLayoutParams(dLp);
        plugDot.setBackground(UiFactory.roundedRect(ctx, COLOR_MULLVAD_GREEN, Color.TRANSPARENT, 0f, 16f));

        LinearLayout metaCol = new LinearLayout(ctx);
        metaCol.setOrientation(LinearLayout.VERTICAL);
        metaCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        String roleTag = isSelectedExit ? "  [ACTIVE EXIT]" : (isSelectedEntry ? "  [MULTIHOP ENTRY]" : "");
        metaCol.addView(UiFactory.label(ctx, r.hostname + roleTag, 16f,
                isSelectedExit ? COLOR_MULLVAD_GREEN : UiFactory.COLOR_TEXT, true, false));
        metaCol.addView(UiFactory.label(ctx, r.city + ", " + r.country + " " + r.flagEmoji,
                14f, UiFactory.COLOR_TEXT, false, false));
        metaCol.addView(UiFactory.label(ctx, r.providerSpeedLabel(), 13f, UiFactory.COLOR_MUTED, false, false));
        metaCol.addView(UiFactory.label(ctx, r.ownershipLabel(), 13f,
                r.ownedByMullvad ? COLOR_MULLVAD_GREEN : UiFactory.COLOR_AMBER, false, false));

        hdr.addView(plugDot);
        hdr.addView(metaCol);
        outer.addView(hdr);

        // Specification Body (matches exact uppercase captions + monospace values in 215141–215713)
        LinearLayout body = new LinearLayout(ctx);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(hp, UiFactory.dp(ctx, 12), hp, hp);

        addSpecField(ctx, body, "DOMAIN NAME", r.fqdn);
        addSpecField(ctx, body, "IPV4", r.ipv4);
        if (!r.isBridge() && !"-".equals(r.ipv6)) {
            addSpecField(ctx, body, "IPV6", r.ipv6);
        }
        if (r.daita) {
            addSpecField(ctx, body, "DAITA", "Yes");
        }
        if (!r.isBridge()) {
            addSpecField(ctx, body, "PUBLIC KEY", r.publicKey);
            if (!r.socks5Proxy.isEmpty()) {
                addSpecField(ctx, body, "SOCKS5 PROXY ADDRESS", r.socks5Proxy);
            }
            addSpecField(ctx, body, "MULTIHOP PORT", String.valueOf(r.multihopPort));
        } else {
            addSpecField(ctx, body, "SSH FINGERPRINT (SHA256)", r.sshSha256);
            addSpecField(ctx, body, "SSH FINGERPRINT (MD5)", r.sshMd5);
            if (!r.ipv4V2ray.isEmpty()) {
                addSpecField(ctx, body, "V2RAY BRIDGE IPV4", r.ipv4V2ray);
            }
        }

        final TextView probeOutTv = UiFactory.label(ctx, "", 11.5f, COLOR_MULLVAD_GREEN, false, true);
        probeOutTv.setVisibility(View.GONE);

        // Action Row
        LinearLayout actRow = new LinearLayout(ctx);
        actRow.setOrientation(LinearLayout.HORIZONTAL);
        actRow.setPadding(0, UiFactory.dp(ctx, 8), 0, 0);
        LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        bLp.rightMargin = UiFactory.dp(ctx, 6);

        if (!r.isBridge()) {
            Button btnExit = UiFactory.actionButton(ctx, "Use as Exit",
                    UiFactory.COLOR_SURFACE, COLOR_MULLVAD_GREEN, COLOR_MULLVAD_GREEN);
            btnExit.setLayoutParams(bLp);
            btnExit.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    cfg.mullvadExitRelay = r.hostname;
                    if (r.daita) cfg.mullvadDaita = true;
                    String sum = MullvadRelayDirectory.applyToConfig(cfg);
                    host.persistAndReload("Selected Mullvad Exit: " + sum);
                    Toast.makeText(ctx, sum, Toast.LENGTH_SHORT).show();
                    host.openConfigureSubScreen(SUB_MULLVAD_RELAYS);
                }
            });

            Button btnEntry = UiFactory.actionButton(ctx, "Set Entry",
                    UiFactory.COLOR_SURFACE, UiFactory.COLOR_SKY, UiFactory.COLOR_SKY);
            btnEntry.setLayoutParams(bLp);
            btnEntry.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    cfg.mullvadEntryRelay = r.hostname;
                    cfg.mullvadMultihop = true;
                    String sum = MullvadRelayDirectory.applyToConfig(cfg);
                    host.persistAndReload("Selected Mullvad Multihop Entry: " + sum);
                    Toast.makeText(ctx, sum, Toast.LENGTH_SHORT).show();
                    host.openConfigureSubScreen(SUB_MULLVAD_RELAYS);
                }
            });
            actRow.addView(btnExit);
            actRow.addView(btnEntry);
        } else {
            Button btnSshBr = UiFactory.actionButton(ctx, "SSH Bridge",
                    UiFactory.COLOR_SURFACE, UiFactory.COLOR_SKY, UiFactory.COLOR_SKY);
            btnSshBr.setLayoutParams(bLp);
            btnSshBr.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    cfg.mullvadBridgeRelay = r.hostname;
                    cfg.mullvadEntryRelay = r.hostname;
                    cfg.mullvadMultihop = true;
                    cfg.mullvadSshSha256Pin = r.sshSha256;
                    String sum = MullvadRelayDirectory.applyToConfig(cfg);
                    host.persistAndReload("Enabled Mullvad SSH Bridge: " + sum);
                    Toast.makeText(ctx, sum, Toast.LENGTH_SHORT).show();
                    host.openConfigureSubScreen(SUB_MULLVAD_RELAYS);
                }
            });

            Button btnV2Br = UiFactory.actionButton(ctx, "V2Ray Bridge",
                    UiFactory.COLOR_SURFACE, COLOR_MULLVAD_GREEN, COLOR_MULLVAD_GREEN);
            btnV2Br.setLayoutParams(bLp);
            btnV2Br.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    if (!r.ipv4V2ray.isEmpty()) {
                        cfg.tunnel = "V2Ray/Xray";
                        cfg.xrayProtocol = "VMess";
                        cfg.tunnelHost = r.ipv4V2ray;
                        cfg.tunnelPort = 443;
                        host.persistAndReload("Mullvad V2Ray Bridge -> " + r.ipv4V2ray + ":443");
                        Toast.makeText(ctx, "Configured V2Ray Bridge: " + r.ipv4V2ray + ":443", Toast.LENGTH_SHORT).show();
                    }
                }
            });
            actRow.addView(btnSshBr);
            actRow.addView(btnV2Br);
        }

        Button btnProbe = UiFactory.actionButton(ctx, "Probe",
                UiFactory.COLOR_SURFACE, UiFactory.COLOR_TEXT, UiFactory.COLOR_BORDER);
        btnProbe.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.75f));
        btnProbe.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                probeOutTv.setVisibility(View.VISIBLE);
                probeOutTv.setTextColor(UiFactory.COLOR_AMBER);
                probeOutTv.setText("Probing " + r.hostname + " (" + r.ipv4 + ")...");
                host.getBgExecutor().execute(new Runnable() {
                    @Override public void run() {
                        final String res = MullvadRelayDirectory.probeRelayLive(r);
                        probeOutTv.post(new Runnable() {
                            @Override public void run() {
                                boolean ok = res.startsWith("BRIDGE LIVE") || res.startsWith("RELAY ");
                                probeOutTv.setTextColor(ok ? COLOR_MULLVAD_GREEN : UiFactory.COLOR_CRIMSON);
                                probeOutTv.setText(res);
                            }
                        });
                    }
                });
            }
        });
        actRow.addView(btnProbe);

        body.addView(actRow);
        body.addView(probeOutTv);
        outer.addView(body);
        return outer;
    }

    private static void addSpecField(final Context ctx, LinearLayout parent, final String key, final String val) {
        TextView kTv = UiFactory.label(ctx, key, 10.5f, UiFactory.COLOR_MUTED, true, false);
        LinearLayout.LayoutParams kLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        kLp.topMargin = UiFactory.dp(ctx, 6);
        kTv.setLayoutParams(kLp);

        int valCol = ("DAITA".equals(key) && "Yes".equalsIgnoreCase(val)) ? COLOR_MULLVAD_GREEN : UiFactory.COLOR_TEXT;
        TextView vTv = UiFactory.label(ctx, val, 13f, valCol, "DAITA".equals(key), true);
        vTv.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                copyText(ctx, key, val);
            }
        });

        parent.addView(kTv);
        parent.addView(vTv);
    }

    private static void copyText(Context ctx, String label, String val) {
        ClipboardManager cm = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText(label, val));
            Toast.makeText(ctx, "Copied " + label + ": " + val, Toast.LENGTH_SHORT).show();
        }
    }
}
