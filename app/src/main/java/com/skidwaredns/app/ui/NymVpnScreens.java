package com.skidwaredns.app.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.skidwaredns.app.config.SkidConfig;
import com.skidwaredns.app.model.DnsPresets;
import com.skidwaredns.app.tunnel.NymMixnetAndCensorshipEngine;

/**
 * SkidwareDNS v5.0 NymVPN UI Suite:
 *  - SUB_NYM_PROFILES        (21): Profiles — Auto, Anonymous (Mixnet), Fastest, Random (Screenshot 190916)
 *  - SUB_NYM_MIXNET_TUNING   (22): Mixnet tuning — Live Expected Performance (Up to 1.0 Mbps / At least 690 ms),
 *                                  Send continuous traffic (2 / 1 / 0.7 Mbps), Background cover traffic rate
 *                                  (Low, Balanced 5x, Medium 10x, High 20x), Packet mixing (0..200 ms) & Save custom settings (190903, 190908)
 *  - SUB_NYM_ANTI_CENSORSHIP (23): Anti-censorship — Enhanced connection (QUIC), Minimal obfuscation (AmneziaWG),
 *                                  and Stealth API connect (Screenshot 185659)
 */
public final class NymVpnScreens {

    public static final int SUB_NYM_PROFILES = 21;
    public static final int SUB_NYM_MIXNET_TUNING = 22;
    public static final int SUB_NYM_ANTI_CENSORSHIP = 23;

    private static final int COLOR_NYM_CARD = Color.parseColor("#1D1F22");
    private static final int COLOR_NYM_SELECTED_BG = Color.parseColor("#2A2E30");
    private static final int COLOR_NYM_MINT = Color.parseColor("#3BE38C");

    private NymVpnScreens() {}

    // =========================================================================
    // 21. NYMVPN PROFILES SCREEN (Screenshot 190916)
    // =========================================================================
    public static View buildProfilesScreen(final Context ctx, final ConfigureScreens.HostCallbacks host) {
        ScrollView sv = wrapScroll(ctx);
        LinearLayout col = innerCol(ctx);
        final SkidConfig cfg = host.getConfig();

        col.addView(UiFactory.backHeader(ctx, "Profiles",
                "Profiles are connection templates you can use to connect quickly with the best type of server configuration for you.",
                new View.OnClickListener() {
                    @Override public void onClick(View v) { host.openConfigureSubScreen(ConfigureScreens.SUB_HUB); }
                }));

        // Quick Tab Strip across the 3 NymVPN screens
        col.addView(buildNymTopNav(ctx, host, SUB_NYM_PROFILES));

        LinearLayout listCard = new LinearLayout(ctx);
        listCard.setOrientation(LinearLayout.VERTICAL);
        int pad = UiFactory.dp(ctx, 8);
        listCard.setPadding(pad, pad, pad, pad);
        listCard.setBackground(UiFactory.roundedRect(ctx, COLOR_NYM_CARD, UiFactory.COLOR_BORDER, 1f, 16f));
        LinearLayout.LayoutParams lcLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lcLp.bottomMargin = UiFactory.dp(ctx, 14);
        listCard.setLayoutParams(lcLp);

        final String[][] profiles = new String[][]{
                {"Auto", "🧭", "Auto", "Best performing servers, never your own country"},
                {"Anonymous", "🐢", "Anonymous", "Mixnet mode with closest servers to you"},
                {"Fastest", "🐇", "Fastest", "Closest, best performing servers"},
                {"Random", "🎲", "Random", "Different servers for every connection"}
        };

        for (final String[] p : profiles) {
            final boolean active = p[0].equalsIgnoreCase(cfg.nymProfile);
            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            int rPad = UiFactory.dp(ctx, 16);
            row.setPadding(rPad, rPad, rPad, rPad);
            if (active) {
                row.setBackground(UiFactory.roundedRect(ctx, COLOR_NYM_SELECTED_BG, COLOR_NYM_MINT, 1.2f, 14f));
            }
            LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rLp.bottomMargin = UiFactory.dp(ctx, 4);
            row.setLayoutParams(rLp);

            TextView iconTv = UiFactory.label(ctx, p[1], 20f, active ? COLOR_NYM_MINT : UiFactory.COLOR_TEXT, true, false);
            LinearLayout.LayoutParams iLp = new LinearLayout.LayoutParams(UiFactory.dp(ctx, 38), ViewGroup.LayoutParams.WRAP_CONTENT);
            iconTv.setLayoutParams(iLp);

            LinearLayout txtCol = new LinearLayout(ctx);
            txtCol.setOrientation(LinearLayout.VERTICAL);
            txtCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            txtCol.addView(UiFactory.label(ctx, p[2], 16f, active ? COLOR_NYM_MINT : UiFactory.COLOR_TEXT, true, false));
            txtCol.addView(UiFactory.label(ctx, p[3], 12.5f, UiFactory.COLOR_MUTED, false, false));

            row.addView(iconTv);
            row.addView(txtCol);

            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    cfg.nymProfile = p[0];
                    if ("Anonymous".equalsIgnoreCase(p[0])) {
                        cfg.mode = "Mixnet-5Hop";
                    } else if ("Mixnet-5Hop".equalsIgnoreCase(cfg.mode)) {
                        cfg.mode = "VPN+DNS+Firewall";
                    }
                    NymMixnetAndCensorshipEngine.GatewaySelection sel = NymMixnetAndCensorshipEngine.selectGatewaysForProfile(cfg);
                    cfg.wgEndpoint = sel.entry.host;
                    host.persistAndReload("NymVPN Profile=" + sel.summary);
                    Toast.makeText(ctx, sel.summary, Toast.LENGTH_SHORT).show();
                    host.openConfigureSubScreen(SUB_NYM_PROFILES);
                }
            });
            listCard.addView(row);
        }
        col.addView(listCard);

        // Active Gateway Resolution Card
        NymMixnetAndCensorshipEngine.GatewaySelection sel = NymMixnetAndCensorshipEngine.selectGatewaysForProfile(cfg);
        LinearLayout gwCard = UiFactory.card(ctx, COLOR_NYM_MINT);
        gwCard.addView(UiFactory.label(ctx, "Active Profile Route", 12f, COLOR_NYM_MINT, true, false));
        gwCard.addView(UiFactory.label(ctx, sel.summary, 13.5f, UiFactory.COLOR_TEXT, true, true));
        gwCard.addView(UiFactory.iconNavRow(ctx, "🏳", "Your Country (excluded in Auto)", cfg.nymUserCountry,
                UiFactory.COLOR_MUTED, new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        final EditText et = UiFactory.monoInput(ctx, cfg.nymUserCountry, "US", false);
                        new AlertDialog.Builder(ctx)
                                .setTitle("Two-Letter Country Code (ISO-3166)")
                                .setView(et)
                                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                                    @Override public void onClick(DialogInterface d, int w) {
                                        String c = et.getText().toString().trim().toUpperCase();
                                        if (!c.isEmpty()) cfg.nymUserCountry = c;
                                        host.persistAndReload("Nym User Country=" + cfg.nymUserCountry);
                                        host.openConfigureSubScreen(SUB_NYM_PROFILES);
                                    }
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                    }
                }));
        col.addView(gwCard);

        TextView learnTv = UiFactory.label(ctx, "Learn more about profiles here ↗", 13.5f, UiFactory.COLOR_TEXT, true, false);
        learnTv.setPadding(0, UiFactory.dp(ctx, 8), 0, UiFactory.dp(ctx, 12));
        learnTv.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                new AlertDialog.Builder(ctx)
                        .setTitle("About NymVPN Profiles")
                        .setMessage("• Auto: Selects the lowest-latency entry & exit gateways outside your own country (" + cfg.nymUserCountry + ").\n\n" +
                                "• Anonymous: Routes Sphinx-wrapped packets through 5 independent mixnet hops with Poisson mixing delays and loop cover traffic.\n\n" +
                                "• Fastest: Direct 2-hop WireGuard / AmneziaWG tunnel using the closest gateways.\n\n" +
                                "• Random: Cryptographically rotates distinct entry and exit gateways on every session.")
                        .setPositiveButton("Close", null)
                        .show();
            }
        });
        col.addView(learnTv);

        sv.addView(col);
        return sv;
    }

    // =========================================================================
    // 22. NYMVPN MIXNET TUNING SCREEN (Screenshots 190903 & 190908)
    // =========================================================================
    public static View buildMixnetTuningScreen(final Context ctx, final ConfigureScreens.HostCallbacks host) {
        ScrollView sv = wrapScroll(ctx);
        LinearLayout col = innerCol(ctx);
        final SkidConfig cfg = host.getConfig();

        col.addView(UiFactory.backHeader(ctx, "Mixnet tuning",
                "Tune Sphinx continuous cover traffic, loop cover multipliers & Poisson packet-mixing delays",
                new View.OnClickListener() {
                    @Override public void onClick(View v) { host.openConfigureSubScreen(ConfigureScreens.SUB_HUB); }
                }));

        col.addView(buildNymTopNav(ctx, host, SUB_NYM_MIXNET_TUNING));

        // Card 1: Expected performance of current settings (live updating!)
        LinearLayout perfCard = makeNymCard(ctx);
        perfCard.addView(UiFactory.label(ctx, "Expected performance of current settings:", 14.5f, UiFactory.COLOR_TEXT, false, false));

        NymMixnetAndCensorshipEngine.MixnetPerformanceEstimate initEst = NymMixnetAndCensorshipEngine.estimatePerformance(cfg);

        LinearLayout rateRow = new LinearLayout(ctx);
        rateRow.setOrientation(LinearLayout.HORIZONTAL);
        rateRow.setPadding(0, UiFactory.dp(ctx, 12), 0, UiFactory.dp(ctx, 10));
        TextView rateKey = UiFactory.label(ctx, "Packet rate", 15f, UiFactory.COLOR_TEXT, false, false);
        rateKey.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        final TextView rateValTv = UiFactory.label(ctx, initEst.packetRateText, 15.5f, COLOR_NYM_MINT, true, false);
        rateRow.addView(rateKey);
        rateRow.addView(rateValTv);
        perfCard.addView(rateRow);
        perfCard.addView(UiFactory.divider(ctx));

        LinearLayout latRow = new LinearLayout(ctx);
        latRow.setOrientation(LinearLayout.HORIZONTAL);
        latRow.setPadding(0, UiFactory.dp(ctx, 10), 0, UiFactory.dp(ctx, 6));
        TextView latKey = UiFactory.label(ctx, "Latency", 15f, UiFactory.COLOR_TEXT, false, false);
        latKey.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        final TextView latValTv = UiFactory.label(ctx, initEst.latencyText, 15.5f, COLOR_NYM_MINT, true, false);
        latRow.addView(latKey);
        latRow.addView(latValTv);
        perfCard.addView(latRow);
        perfCard.addView(UiFactory.label(ctx, "Expected round-trip latency (RTT) on a standard network",
                12f, UiFactory.COLOR_MUTED, false, false));
        col.addView(perfCard);

        final Runnable refreshPerf = new Runnable() {
            @Override
            public void run() {
                NymMixnetAndCensorshipEngine.MixnetPerformanceEstimate e = NymMixnetAndCensorshipEngine.estimatePerformance(cfg);
                rateValTv.setText(e.packetRateText);
                latValTv.setText(e.latencyText);
            }
        };

        // Card 2: Send continuous traffic (2 Mbps | 1 Mbps | 0.7 Mbps — Screenshot 190903)
        LinearLayout contCard = makeNymCard(ctx);
        contCard.addView(UiFactory.iconSwitchRow(ctx, "⇄", "Send continuous traffic",
                "Cover traffic is always sent to keep the pattern uniform. Adjust the sending rate of your packets.",
                cfg.mixnetContinuousTraffic, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean c) {
                        cfg.mixnetContinuousTraffic = c;
                        refreshPerf.run();
                    }
                }));
        contCard.addView(buildRangeCaptionRow(ctx, "Faster", "Slower"));

        int contIdx = 1;
        for (int i = 0; i < DnsPresets.MIXNET_CONTINUOUS_RATES.length; i++) {
            if (DnsPresets.MIXNET_CONTINUOUS_RATES[i].equalsIgnoreCase(cfg.mixnetContinuousRate)) {
                contIdx = i;
                break;
            }
        }
        final TextView[] contStopTvs = new TextView[3];
        SeekBar contSeek = new SeekBar(ctx);
        contSeek.setMax(2);
        contSeek.setProgress(contIdx);
        contCard.addView(contSeek);

        LinearLayout contLabels = new LinearLayout(ctx);
        contLabels.setOrientation(LinearLayout.HORIZONTAL);
        String[] cNames = new String[]{"2 Mbps", "1 Mbps", "0.7 Mbps"};
        int[] cGrav = new int[]{Gravity.START, Gravity.CENTER_HORIZONTAL, Gravity.END};
        for (int i = 0; i < 3; i++) {
            contStopTvs[i] = UiFactory.label(ctx, cNames[i], 13.5f,
                    i == contIdx ? COLOR_NYM_MINT : UiFactory.COLOR_TEXT, i == contIdx, false);
            contStopTvs[i].setGravity(cGrav[i]);
            contStopTvs[i].setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            contLabels.addView(contStopTvs[i]);
        }
        contCard.addView(contLabels);
        contSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int p, boolean fromUser) {
                cfg.mixnetContinuousRate = DnsPresets.MIXNET_CONTINUOUS_RATES[p];
                for (int i = 0; i < 3; i++) {
                    contStopTvs[i].setTextColor(i == p ? COLOR_NYM_MINT : UiFactory.COLOR_TEXT);
                }
                refreshPerf.run();
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        col.addView(contCard);

        // Card 3: Background cover traffic rate (Low | Balanced 5x | Medium 10x | High 20x — 190903 & 190908)
        LinearLayout covCard = makeNymCard(ctx);
        covCard.addView(UiFactory.iconSwitchRow(ctx, "🛡", "Background cover traffic rate",
                "Choose how much cover you need.",
                cfg.mixnetCoverTrafficEnabled, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean c) {
                        cfg.mixnetCoverTrafficEnabled = c;
                        refreshPerf.run();
                    }
                }));
        covCard.addView(buildRangeCaptionRow(ctx, "Faster", "+ Anonymity"));

        int covIdx = 0;
        for (int i = 0; i < DnsPresets.MIXNET_COVER_RATES.length; i++) {
            if (DnsPresets.MIXNET_COVER_RATES[i].equalsIgnoreCase(cfg.mixnetCoverTrafficRate)) {
                covIdx = i;
                break;
            }
        }
        final TextView[] covStopTvs = new TextView[4];
        SeekBar covSeek = new SeekBar(ctx);
        covSeek.setMax(3);
        covSeek.setProgress(covIdx);
        covCard.addView(covSeek);

        LinearLayout covLabels = new LinearLayout(ctx);
        covLabels.setOrientation(LinearLayout.HORIZONTAL);
        String[] covNames = new String[]{"Low", "Balanced\n5x", "Medium\n10x", "High\n20x"};
        int[] covGrav = new int[]{Gravity.START, Gravity.CENTER_HORIZONTAL, Gravity.CENTER_HORIZONTAL, Gravity.END};
        for (int i = 0; i < 4; i++) {
            covStopTvs[i] = UiFactory.label(ctx, covNames[i], 13f,
                    i == covIdx ? COLOR_NYM_MINT : UiFactory.COLOR_TEXT, i == covIdx, false);
            covStopTvs[i].setGravity(covGrav[i]);
            covStopTvs[i].setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            covLabels.addView(covStopTvs[i]);
        }
        covCard.addView(covLabels);
        covSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int p, boolean fromUser) {
                cfg.mixnetCoverTrafficRate = DnsPresets.MIXNET_COVER_RATES[p];
                for (int i = 0; i < 4; i++) {
                    covStopTvs[i].setTextColor(i == p ? COLOR_NYM_MINT : UiFactory.COLOR_TEXT);
                }
                refreshPerf.run();
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        col.addView(covCard);

        // Card 4: Packet mixing (Low 0 ms | Default 15 ms | High 200 ms — Screenshot 190908)
        LinearLayout mixCard = makeNymCard(ctx);
        mixCard.addView(UiFactory.label(ctx, "Packet mixing", 16f, UiFactory.COLOR_TEXT, true, false));
        mixCard.addView(UiFactory.label(ctx, "Adjust timing delays to change how well your packets are mixed.",
                12.5f, UiFactory.COLOR_MUTED, false, false));
        mixCard.addView(buildRangeCaptionRow(ctx, "Faster", "+ Anonymity"));

        SeekBar mixSeek = new SeekBar(ctx);
        mixSeek.setMax(200);
        mixSeek.setProgress(Math.max(0, Math.min(200, cfg.mixnetPacketMixingDelayMs)));
        mixCard.addView(mixSeek);

        LinearLayout mixLabels = new LinearLayout(ctx);
        mixLabels.setOrientation(LinearLayout.HORIZONTAL);
        final TextView lowMixTv = UiFactory.label(ctx, "Low\n0 ms", 13f,
                cfg.mixnetPacketMixingDelayMs == 0 ? COLOR_NYM_MINT : UiFactory.COLOR_TEXT, false, false);
        lowMixTv.setGravity(Gravity.START);
        lowMixTv.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        String midLbl = (cfg.mixnetPacketMixingDelayMs == 15 ? "Default\n15 ms" : "Active\n" + cfg.mixnetPacketMixingDelayMs + " ms");
        final TextView midMixTv = UiFactory.label(ctx, midLbl, 13f, COLOR_NYM_MINT, true, false);
        midMixTv.setGravity(Gravity.CENTER_HORIZONTAL);
        midMixTv.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        final TextView highMixTv = UiFactory.label(ctx, "High\n200 ms", 13f,
                cfg.mixnetPacketMixingDelayMs >= 200 ? COLOR_NYM_MINT : UiFactory.COLOR_TEXT, false, false);
        highMixTv.setGravity(Gravity.END);
        highMixTv.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        mixLabels.addView(lowMixTv);
        mixLabels.addView(midMixTv);
        mixLabels.addView(highMixTv);
        mixCard.addView(mixLabels);

        mixSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int p, boolean fromUser) {
                cfg.mixnetPacketMixingDelayMs = p;
                lowMixTv.setTextColor(p == 0 ? COLOR_NYM_MINT : UiFactory.COLOR_TEXT);
                highMixTv.setTextColor(p >= 200 ? COLOR_NYM_MINT : UiFactory.COLOR_TEXT);
                midMixTv.setText(p == 15 ? "Default\n15 ms" : ("Active\n" + p + " ms"));
                refreshPerf.run();
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        col.addView(mixCard);

        TextView learnMix = UiFactory.label(ctx, "Learn more about mixnet tuning ↗", 13.5f, UiFactory.COLOR_TEXT, true, false);
        learnMix.setPadding(0, UiFactory.dp(ctx, 4), 0, UiFactory.dp(ctx, 12));
        learnMix.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                new AlertDialog.Builder(ctx)
                        .setTitle("Sphinx Mixnet Tuning")
                        .setMessage("• Send continuous traffic: Emits Sphinx packets at a constant Poisson rate (2 / 1 / 0.7 Mbps) so global passive adversaries cannot correlate burst timing.\n\n" +
                                "• Background cover traffic: Injects dummy loop packets (Low / 5x / 10x / 20x) indistinguishable from real payload.\n\n" +
                                "• Packet mixing: Each mix node holds packets for an exponential random delay (Default 15 ms -> ~690 ms 5-hop RTT) to defeat timing correlation.")
                        .setPositiveButton("Close", null)
                        .show();
            }
        });
        col.addView(learnMix);

        // Full-width "Save custom settings" button matching bottom of Screenshot 190908
        Button saveCustomBtn = UiFactory.actionButton(ctx, "Save custom settings",
                COLOR_NYM_SELECTED_BG, COLOR_NYM_MINT, COLOR_NYM_MINT);
        saveCustomBtn.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, UiFactory.dp(ctx, 48)));
        saveCustomBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NymMixnetAndCensorshipEngine.MixnetPerformanceEstimate est = NymMixnetAndCensorshipEngine.estimatePerformance(cfg);
                host.persistAndReload("Saved Mixnet Tuning (" + est.packetRateText + ", " + est.latencyText + ")");
                Toast.makeText(ctx, "Saved Mixnet settings: " + est.packetRateText + " • " + est.latencyText, Toast.LENGTH_SHORT).show();
            }
        });
        col.addView(saveCustomBtn);

        sv.addView(col);
        return sv;
    }

    // =========================================================================
    // 23. NYMVPN ANTI-CENSORSHIP SCREEN (Screenshot 185659)
    // =========================================================================
    public static View buildAntiCensorshipScreen(final Context ctx, final ConfigureScreens.HostCallbacks host) {
        ScrollView sv = wrapScroll(ctx);
        LinearLayout col = innerCol(ctx);
        final SkidConfig cfg = host.getConfig();

        col.addView(UiFactory.backHeader(ctx, "Anti-censorship",
                "Advanced features to improve connectivity in restrictive networks or when experiencing NymVPN blocking.",
                new View.OnClickListener() {
                    @Override public void onClick(View v) { host.openConfigureSubScreen(ConfigureScreens.SUB_HUB); }
                }));

        col.addView(buildNymTopNav(ctx, host, SUB_NYM_ANTI_CENSORSHIP));

        // Card 1: Enhanced connection (QUIC) + Minimal obfuscation (AmneziaWG) (grouped with divider as in 185659)
        LinearLayout mainCard = makeNymCard(ctx);
        mainCard.addView(UiFactory.iconSwitchRow(ctx, "⚡", "Enhanced connection (QUIC)",
                "Improves connections on restricted networks by wrapping WireGuard in QUIC (HTTP/3).\n\n" +
                        "Note: Limits entry server options to QUIC-compatible servers only.",
                cfg.nymQuicEnhanced, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean c) {
                        cfg.nymQuicEnhanced = c;
                        NymMixnetAndCensorshipEngine.GatewaySelection sel = NymMixnetAndCensorshipEngine.selectGatewaysForProfile(cfg);
                        cfg.wgEndpoint = sel.entry.host;
                        host.persistAndReload("Enhanced connection (QUIC)=" + c + " -> " + sel.entry.host);
                    }
                }));
        mainCard.addView(buildLearnMoreLink(ctx, "Enhanced connection (QUIC)",
                "Wraps your WireGuard entry tunnel inside standard HTTP/3 QUIC UDP/443 datagrams (RFC 9298 MASQUE CONNECT-UDP) so firewalls see ordinary web traffic instead of WireGuard Type 1–4 headers."));
        mainCard.addView(UiFactory.divider(ctx));

        mainCard.addView(UiFactory.iconSwitchRow(ctx, "🎭", "Minimal obfuscation (AmneziaWG)",
                "Disguises WireGuard traffic with decoy packets to prevent VPN blocking.\n\n" +
                        "Note: Only implemented client-side.",
                cfg.amneziaWgMinimalObfs, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean c) {
                        cfg.amneziaWgMinimalObfs = c;
                        host.persistAndReload("Minimal obfuscation (AmneziaWG)=" + c);
                    }
                }));
        mainCard.addView(UiFactory.iconNavRow(ctx, "⚙", "AmneziaWG Decoy Params (Jc / Jmin / Jmax)",
                "Jc=" + cfg.amneziaWgJc + " packets • Jmin=" + cfg.amneziaWgJmin + "B • Jmax=" + cfg.amneziaWgJmax + "B",
                COLOR_NYM_MINT, new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        final EditText et = UiFactory.monoInput(ctx,
                                cfg.amneziaWgJc + "," + cfg.amneziaWgJmin + "," + cfg.amneziaWgJmax, "4,40,70", false);
                        new AlertDialog.Builder(ctx)
                                .setTitle("AmneziaWG Jc,Jmin,Jmax")
                                .setView(et)
                                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                                    @Override public void onClick(DialogInterface d, int w) {
                                        String[] p = et.getText().toString().split("[,:\\s]+");
                                        if (p.length >= 3) {
                                            try {
                                                cfg.amneziaWgJc = Integer.parseInt(p[0].trim());
                                                cfg.amneziaWgJmin = Integer.parseInt(p[1].trim());
                                                cfg.amneziaWgJmax = Integer.parseInt(p[2].trim());
                                            } catch (Exception ignored) {}
                                        }
                                        host.persistAndReload("AmneziaWG Decoys updated");
                                        host.openConfigureSubScreen(SUB_NYM_ANTI_CENSORSHIP);
                                    }
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                    }
                }));
        mainCard.addView(buildLearnMoreLink(ctx, "Minimal obfuscation (AmneziaWG)",
                "Transmits Jc (" + cfg.amneziaWgJc + ") high-entropy client-side decoy UDP packets between " +
                        cfg.amneziaWgJmin + " and " + cfg.amneziaWgJmax + " bytes prior to WireGuard handshakes so stateful DPI classifiers lose packet-length & inter-arrival fingerprint lock without requiring server changes."));
        col.addView(mainCard);

        // Card 2: Stealth API connect (Screenshot 185659)
        LinearLayout apiCard = makeNymCard(ctx);
        apiCard.addView(UiFactory.iconSwitchRow(ctx, "🕵", "Stealth API connect",
                "Protects against VPN blocking by using cover domains. Toggle on to use always, or off to use when normal connections fail.",
                cfg.nymStealthApiConnect, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean c) {
                        cfg.nymStealthApiConnect = c;
                        host.persistAndReload("Stealth API connect=" + (c ? "Always" : "Auto-Fallback"));
                    }
                }));
        apiCard.addView(UiFactory.iconNavRow(ctx, "🌐", "Cover Domains", cfg.nymStealthCoverDomains,
                UiFactory.COLOR_SKY, new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        final EditText et = UiFactory.monoInput(ctx, cfg.nymStealthCoverDomains,
                                "cloudflare.com,cdn.jsdelivr.net", false);
                        new AlertDialog.Builder(ctx)
                                .setTitle("Stealth API Cover Domains (comma-separated)")
                                .setView(et)
                                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                                    @Override public void onClick(DialogInterface d, int w) {
                                        cfg.nymStealthCoverDomains = et.getText().toString().trim();
                                        host.persistAndReload("Stealth Cover Domains updated");
                                        host.openConfigureSubScreen(SUB_NYM_ANTI_CENSORSHIP);
                                    }
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                    }
                }));

        final TextView probeTv = UiFactory.label(ctx, "Tap to test Stealth API cover-domain reachability.",
                11.5f, UiFactory.COLOR_MUTED, false, true);
        Button probeBtn = UiFactory.actionButton(ctx, "Test Stealth API Cover Connect",
                UiFactory.COLOR_SURFACE, COLOR_NYM_MINT, COLOR_NYM_MINT);
        probeBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                probeTv.setTextColor(UiFactory.COLOR_AMBER);
                probeTv.setText("Testing cover domains...");
                host.getBgExecutor().execute(new Runnable() {
                    @Override public void run() {
                        final String res = NymMixnetAndCensorshipEngine.probeStealthApiCoverConnect(cfg);
                        probeTv.post(new Runnable() {
                            @Override public void run() {
                                boolean ok = res.contains("OK");
                                probeTv.setTextColor(ok ? COLOR_NYM_MINT : UiFactory.COLOR_CRIMSON);
                                probeTv.setText(res);
                            }
                        });
                    }
                });
            }
        });
        apiCard.addView(probeBtn);
        apiCard.addView(probeTv);
        apiCard.addView(buildLearnMoreLink(ctx, "Stealth API connect",
                "Uses CDN cover domains (" + cfg.nymStealthCoverDomains + ") during directory & gateway credential bootstrap so SNI censors cannot block Nym directory API discovery."));
        col.addView(apiCard);

        // Preview Native NymVPN + AmneziaWG + QUIC Config Button
        Button previewBtn = UiFactory.actionButton(ctx, "Preview nym-vpnd + AmneziaWG Config",
                UiFactory.COLOR_SURFACE, UiFactory.COLOR_TEXT, UiFactory.COLOR_BORDER);
        previewBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                final EditText et = UiFactory.monoInput(ctx, NymMixnetAndCensorshipEngine.generateNymConfigSnippet(cfg), "", true);
                et.setMinLines(14);
                new AlertDialog.Builder(ctx)
                        .setTitle("Generated NymVPN v5.0 Config")
                        .setView(et)
                        .setPositiveButton("Close", null)
                        .show();
            }
        });
        col.addView(previewBtn);

        sv.addView(col);
        return sv;
    }

    // === Shared UI Helpers ===
    private static View buildNymTopNav(Context ctx, final ConfigureScreens.HostCallbacks host, int activeSub) {
        LinearLayout bar = new LinearLayout(ctx);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = UiFactory.dp(ctx, 12);
        bar.setLayoutParams(lp);

        String[] labels = new String[]{"PROFILES", "MIXNET TUNING", "ANTI-CENSORSHIP"};
        final int[] subs = new int[]{SUB_NYM_PROFILES, SUB_NYM_MIXNET_TUNING, SUB_NYM_ANTI_CENSORSHIP};
        for (int i = 0; i < 3; i++) {
            final int dest = subs[i];
            boolean sel = dest == activeSub;
            Button b = UiFactory.actionButton(ctx, labels[i],
                    sel ? COLOR_NYM_SELECTED_BG : UiFactory.COLOR_SURFACE,
                    sel ? COLOR_NYM_MINT : UiFactory.COLOR_TEXT,
                    sel ? COLOR_NYM_MINT : UiFactory.COLOR_BORDER);
            LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            if (i < 2) bLp.rightMargin = UiFactory.dp(ctx, 6);
            b.setLayoutParams(bLp);
            b.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { host.openConfigureSubScreen(dest); }
            });
            bar.addView(b);
        }
        return bar;
    }

    private static LinearLayout makeNymCard(Context ctx) {
        LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.VERTICAL);
        int p = UiFactory.dp(ctx, 16);
        card.setPadding(p, p, p, p);
        card.setBackground(UiFactory.roundedRect(ctx, COLOR_NYM_CARD, UiFactory.COLOR_BORDER, 1f, 16f));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = UiFactory.dp(ctx, 14);
        card.setLayoutParams(lp);
        return card;
    }

    private static View buildRangeCaptionRow(Context ctx, String left, String right) {
        LinearLayout r = new LinearLayout(ctx);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setPadding(0, UiFactory.dp(ctx, 10), 0, UiFactory.dp(ctx, 4));
        TextView lTv = UiFactory.label(ctx, left, 12f, UiFactory.COLOR_MUTED, false, false);
        lTv.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView rTv = UiFactory.label(ctx, right, 12f, UiFactory.COLOR_MUTED, false, false);
        r.addView(lTv);
        r.addView(rTv);
        return r;
    }

    private static View buildLearnMoreLink(final Context ctx, final String title, final String body) {
        TextView tv = UiFactory.label(ctx, "Learn more ↗", 13f, UiFactory.COLOR_TEXT, true, false);
        tv.setPadding(0, UiFactory.dp(ctx, 6), 0, UiFactory.dp(ctx, 6));
        tv.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                new AlertDialog.Builder(ctx)
                        .setTitle(title)
                        .setMessage(body)
                        .setPositiveButton("Close", null)
                        .show();
            }
        });
        return tv;
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
}
