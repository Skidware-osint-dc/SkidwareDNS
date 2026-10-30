package com.skidwaredns.app.ui;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.widget.ImageView;
import android.net.Uri;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.skidwaredns.app.config.SkidConfig;
import com.skidwaredns.app.config.SkidConfigParser;
import com.skidwaredns.app.config.SkidConfigStore;
import com.skidwaredns.app.dns.DnsPacketCodec;
import com.skidwaredns.app.dns.DnsUpstreamResolver;
import com.skidwaredns.app.firewall.SkidFirewallEngine;
import com.skidwaredns.app.model.DnsPresets;
import com.skidwaredns.app.util.PcapRecorder;
import com.skidwaredns.app.util.SkidEventBus;
import com.skidwaredns.app.vpn.SkidVpnService;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Upgraded SkidwareDNS Main Activity featuring:
 *  - Bento Home Screen (matching RethinkDNS dark matte + sky-blue #5CC8FF design with Pause ||, START pill, Mode v)
 *  - Live TCP / UDP / DNS / DPI / PCAP Stats tab
 *  - [.skid] Custom Config Studio tab
 *  - Full Configure Hub with 9 sub-screens (Apps, DNS, Firewall, Proxy, TCP/IP & UDP, Anti-Censorship, Network, Settings, Logs/PCAP)
 *  - Live Packet & Debug Logs tab + Floating Firewall Bubble banner
 */
public final class MainActivity extends Activity implements SkidEventBus.Listener, ConfigureScreens.HostCallbacks {

    private static final int REQ_VPN_PREPARE = 4001;
    private static final int REQ_IMPORT_SKID = 4002;
    private static final int REQ_EXPORT_SKID = 4003;
    private static final int REQ_POST_NOTIF = 4004;
    private static final int REQ_EXPORT_PCAP = 4005;

    private final ExecutorService bgExecutor = Executors.newCachedThreadPool();

    private SkidConfig config;
    private int currentTab = 0;
    private int currentConfigureSub = ConfigureScreens.SUB_HUB;
    private int homeFwCardPage = 0;
    private String logFilter = "ALL";
    private final boolean[] includeSysAppsRef = new boolean[]{false};
    private final String[] appSearchQueryRef = new String[]{""};

    private FrameLayout rootShell;
    private FrameLayout contentContainer;
    private TextView firewallBubbleBanner;
    private final List<LinearLayout> navPills = new ArrayList<>();
    private final List<ImageView> navIcons = new ArrayList<>();
    private final List<TextView> navLabels = new ArrayList<>();

    // Home Bento Live Refs
    private TextView homeProtectionHeader;
    private TextView homeDnsPillText;
    private TextView homeStatePillText;
    private TextView homeFwCardBigNumber;
    private TextView homeFwCardCaption;
    private TextView homeFwDots;
    private TextView homeProxyCardTitle;
    private TextView homeProxyCardSub;
    private TextView homeFirewallRowText;
    private TextView homeTcpDpiRowText;
    private ImageView homePauseBtn;
    private TextView homeStartPillBtn;
    private LiveChartView homeChart;
    private TextView homeAllowChip;
    private TextView homeBlockChip;
    private TextView homeBytesLine;
    private LinearLayout homeMiniLogs;

    // Stats Live Refs
    private TextView statsGridSummaryTv;
    private LiveChartView statsChart;
    private TextView statsDnsTv;
    private TextView statsBlkTv;
    private TextView statsTcpTv;
    private TextView statsUdpTv;
    private TextView statsBytesTv;
    private TextView statsRttTv;
    private TextView statsModeTv;
    private TextView statsBadgesTv;

    // .skid Studio Refs
    private EditText skidRawEditor;
    private TextView skidDiagnosticBanner;

    // Logs Live Ref
    private LinearLayout liveLogsListContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        config = SkidConfigStore.getActive(this);
        SkidVpnService.syncLoggingFlags(config);
        SkidEventBus.setActiveProfileName(config.name);
        SkidEventBus.setActiveDnsBadge(SkidVpnService.describeDnsSelection(config));
        SkidEventBus.setActiveTunnelBadge(config.tunnel + " • " + config.connectFromMode);

        requestNotificationPermissionIfNeeded();
        buildRootShell();
        handleIncomingSkidIntent(getIntent());
        switchTab(0);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingSkidIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        SkidEventBus.addListener(this);
        refreshLiveViews();
    }

    @Override
    protected void onPause() {
        SkidEventBus.removeListener(this);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        bgExecutor.shutdownNow();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (currentTab == 3 && currentConfigureSub != ConfigureScreens.SUB_HUB) {
            openConfigureSubScreen(ConfigureScreens.SUB_HUB);
            return;
        }
        if (currentTab != 0) {
            switchTab(0);
            return;
        }
        super.onBackPressed();
    }

    private void buildRootShell() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(UiFactory.COLOR_BG);

        // Optional Firewall Bubble floating banner at top when triggered
        firewallBubbleBanner = UiFactory.label(this, "", 12f, UiFactory.COLOR_TEXT, true, true);
        firewallBubbleBanner.setVisibility(View.GONE);
        firewallBubbleBanner.setPadding(UiFactory.dp(this, 14), UiFactory.dp(this, 8), UiFactory.dp(this, 14), UiFactory.dp(this, 8));
        firewallBubbleBanner.setBackgroundColor(Color.parseColor("#55EC4E6E"));
        firewallBubbleBanner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                firewallBubbleBanner.setVisibility(View.GONE);
                switchTab(4);
            }
        });
        root.addView(firewallBubbleBanner);

        // Main Screen Frame
        contentContainer = new FrameLayout(this);
        contentContainer.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        root.addView(contentContainer);

        // Bottom Divider + 5-Item Navigation Bar (Home, Stats, SKID, Configure, Logs)
        View navDiv = new View(this);
        navDiv.setBackgroundColor(UiFactory.COLOR_BORDER);
        navDiv.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, UiFactory.dp(this, 1)));
        root.addView(navDiv);

        LinearLayout navBar = new LinearLayout(this);
        navBar.setOrientation(LinearLayout.HORIZONTAL);
        navBar.setGravity(Gravity.CENTER_VERTICAL);
        navBar.setBackgroundColor(UiFactory.COLOR_SURFACE);
        navBar.setPadding(UiFactory.dp(this, 6), UiFactory.dp(this, 10), UiFactory.dp(this, 6), UiFactory.dp(this, 12));

        int[] navIconKinds = new int[]{
                SkidIcons.HOME, SkidIcons.STATS, SkidIcons.SKID, SkidIcons.SLIDERS, SkidIcons.LOGS
        };
        String[] titles = new String[]{"Home", "Stats", "SKID", "Configure", "Logs"};

        navPills.clear();
        navIcons.clear();
        navLabels.clear();

        for (int i = 0; i < titles.length; i++) {
            final int tabIdx = i;
            LinearLayout slot = new LinearLayout(this);
            slot.setOrientation(LinearLayout.VERTICAL);
            slot.setGravity(Gravity.CENTER);
            slot.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            LinearLayout pill = new LinearLayout(this);
            pill.setGravity(Gravity.CENTER);
            pill.setPadding(UiFactory.dp(this, 16), UiFactory.dp(this, 4), UiFactory.dp(this, 16), UiFactory.dp(this, 4));

            ImageView ic = UiFactory.iconView(this, navIconKinds[i], UiFactory.COLOR_TEXT, 20f);
            pill.addView(ic);

            TextView lb = UiFactory.label(this, titles[i], 11.5f, UiFactory.COLOR_MUTED, true, false);
            LinearLayout.LayoutParams lbLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lbLp.topMargin = UiFactory.dp(this, 3);
            lb.setLayoutParams(lbLp);

            slot.addView(pill);
            slot.addView(lb);
            slot.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (tabIdx == 3) {
                        currentConfigureSub = ConfigureScreens.SUB_HUB;
                    }
                    switchTab(tabIdx);
                }
            });

            navPills.add(pill);
            navIcons.add(ic);
            navLabels.add(lb);
            navBar.addView(slot);
        }

        root.addView(navBar);

        rootShell = new FrameLayout(this);
        rootShell.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        rootShell.addView(root, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(rootShell);
        if (!SkidConfigStore.isOnboardingDone(this)) {
            replayOnboarding();
        }
    }

    public void replayOnboarding() {
        if (rootShell == null) return;
        View old = rootShell.findViewWithTag("skid-onboarding");
        if (old != null) rootShell.removeView(old);
        View overlay = OnboardingScreens.build(this, new OnboardingScreens.Done() {
            @Override public void onFinished() {
                SkidConfigStore.setOnboardingDone(MainActivity.this, true);
            }
        });
        rootShell.addView(overlay);
    }

    private void switchTab(int idx) {
        currentTab = idx;
        for (int i = 0; i < navPills.size(); i++) {
            boolean sel = (i == idx);
            navPills.get(i).setBackground(sel
                    ? UiFactory.roundedRect(this,
                        UiFactory.mixColor(UiFactory.COLOR_ICON_BG, UiFactory.COLOR_SKY, 0.28f),
                        UiFactory.mixColor(UiFactory.COLOR_SKY, UiFactory.COLOR_BORDER, 0.45f),
                        0.9f, 16f)
                    : null);
            navIcons.get(i).setColorFilter(sel ? UiFactory.COLOR_SKY : UiFactory.COLOR_MUTED, PorterDuff.Mode.SRC_IN);
            navLabels.get(i).setTextColor(sel ? UiFactory.COLOR_SKY : UiFactory.COLOR_MUTED);
        }

        contentContainer.removeAllViews();
        liveLogsListContainer = null;
        statsGridSummaryTv = null;
        statsChart = null;
        statsDnsTv = null;
        statsBlkTv = null;
        statsTcpTv = null;
        statsUdpTv = null;
        statsBytesTv = null;
        statsRttTv = null;
        statsModeTv = null;
        statsBadgesTv = null;

        View page;
        switch (idx) {
            case 0: page = buildHomeBentoScreen(); break;
            case 1: page = buildStatsScreen(); break;
            case 2: page = buildSkidStudioScreen(); break;
            case 3:
                page = ConfigureScreens.buildSubScreen(
                        this, currentConfigureSub, this, includeSysAppsRef, appSearchQueryRef);
                break;
            case 4: page = buildLogsScreen(); break;
            default: page = buildHomeBentoScreen(); break;
        }
        contentContainer.addView(page);
        refreshLiveViews();
    }

    // =========================================================================
    // TAB 0: BENTO HOME SCREEN (Matches Screenshot_20261028_174221_Rethink.jpg)
    // =========================================================================
    private View buildHomeBentoScreen() {
        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setBackgroundColor(UiFactory.COLOR_BG);

        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        sv.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        int pad = UiFactory.dp(this, 16);
        col.setPadding(pad, UiFactory.dp(this, 22), pad, pad);

        // Top Protection Status Header ("NOT PROTECTED" / "PROTECTED")
        homeProtectionHeader = UiFactory.label(this, "NOT PROTECTED", 14.5f, UiFactory.COLOR_CRIMSON, true, false);
        homeProtectionHeader.setLetterSpacing(0.06f);
        LinearLayout.LayoutParams phLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        phLp.leftMargin = UiFactory.dp(this, 6);
        phLp.bottomMargin = UiFactory.dp(this, 18);
        homeProtectionHeader.setLayoutParams(phLp);
        col.addView(homeProtectionHeader);

        // Bento Row 1: [DNS • ...] pill + [Inactive / Active] pill
        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams r1Lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        r1Lp.bottomMargin = UiFactory.dp(this, 14);
        row1.setLayoutParams(r1Lp);

        LinearLayout dnsPill = new LinearLayout(this);
        dnsPill.setGravity(Gravity.CENTER_VERTICAL);
        dnsPill.setPadding(UiFactory.dp(this, 20), UiFactory.dp(this, 16), UiFactory.dp(this, 16), UiFactory.dp(this, 16));
        dnsPill.setBackground(UiFactory.pill(this, UiFactory.COLOR_CARD, UiFactory.COLOR_BORDER, 0.8f));
        LinearLayout.LayoutParams dpLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.9f);
        dpLp.rightMargin = UiFactory.dp(this, 12);
        dnsPill.setLayoutParams(dpLp);
        homeDnsPillText = UiFactory.label(this, "DNS • off", 15.5f, UiFactory.COLOR_TEXT, false, false);
        dnsPill.addView(homeDnsPillText);
        dnsPill.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openConfigureSubScreen(ConfigureScreens.SUB_DNS);
            }
        });

        LinearLayout statePill = new LinearLayout(this);
        statePill.setGravity(Gravity.CENTER);
        statePill.setPadding(UiFactory.dp(this, 14), UiFactory.dp(this, 16), UiFactory.dp(this, 14), UiFactory.dp(this, 16));
        statePill.setBackground(UiFactory.pill(this, UiFactory.COLOR_CARD, UiFactory.COLOR_BORDER, 0.8f));
        statePill.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        homeStatePillText = UiFactory.label(this, "Inactive", 13f, UiFactory.COLOR_TEXT, false, false);
        statePill.addView(homeStatePillText);
        statePill.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(1);
            }
        });

        row1.addView(dnsPill);
        row1.addView(statePill);
        col.addView(row1);

        col.addView(buildHomeLiveCard());

        // =====================================================================
        // v5.2 CLEANUP: one "Active route" summary card + one flat tool launcher
        // replaces the five stacked product cards (InviZible / Injector / Nym /
        // Mullvad) that used to dominate the home screen.
        // =====================================================================
        LinearLayout routeCard = UiFactory.card(this, Color.parseColor("#2ECC40"));
        routeCard.addView(UiFactory.label(this, "ACTIVE ROUTE", 10.5f, Color.parseColor("#2ECC40"), true, false));
        routeCard.addView(UiFactory.label(this,
                com.skidwaredns.app.tunnel.MullvadRelayDirectory.formatActiveSummary(config),
                15f, UiFactory.COLOR_TEXT, true, false));
        LinearLayout routePills = new LinearLayout(this);
        routePills.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rpLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rpLp.topMargin = UiFactory.dp(this, 6);
        routePills.setLayoutParams(rpLp);
        String[] routeChipLabels = new String[]{
                "MULLVAD",
                "NYM " + config.nymProfile.toUpperCase(Locale.US),
                com.skidwaredns.app.tunnel.XrayUriAndRouteHelper.formatSelectedTunnelBadge(config),
                privacyChipLabel()
        };
        final int[] routeChipTargets = new int[]{
                MullvadScreens.SUB_MULLVAD_RELAYS,
                NymVpnScreens.SUB_NYM_PROFILES,
                HttpInjectorScreens.SUB_TUNNEL_TYPE_MATRIX,
                InviZibleModuleScreens.SUB_DNSCRYPT_SETTINGS
        };
        for (int rc = 0; rc < routeChipLabels.length; rc++) {
            final int rcDest = routeChipTargets[rc];
            TextView rcTv = UiFactory.label(this, routeChipLabels[rc], 10f, UiFactory.COLOR_TEXT, true, false);
            rcTv.setPadding(UiFactory.dp(this, 8), UiFactory.dp(this, 4), UiFactory.dp(this, 8), UiFactory.dp(this, 4));
            rcTv.setBackground(UiFactory.roundedRect(this, UiFactory.COLOR_SURFACE, UiFactory.COLOR_BORDER, 0.8f, 8f));
            LinearLayout.LayoutParams rcLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rcLp.rightMargin = UiFactory.dp(this, 5);
            rcTv.setLayoutParams(rcLp);
            rcTv.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { openConfigureSubScreen(rcDest); }
            });
            routePills.addView(rcTv);
        }
        routeCard.addView(routePills);
        col.addView(routeCard);

        // Flat 2x3 tool launcher: vector icon + label per engine, no duplicate toggles.
        col.addView(UiFactory.groupCaption(this, "TOOLS"));
        final int[] toolIconKinds = new int[]{
                SkidIcons.PLUG, SkidIcons.MIXNET, SkidIcons.INJECT,
                SkidIcons.LOCK, SkidIcons.TOR, SkidIcons.I2P
        };
        final int[] toolIconColors = new int[]{
                Color.parseColor("#2ECC40"), Color.parseColor("#3BE38C"), Color.parseColor("#1D9BF0"),
                UiFactory.COLOR_PURPLE, UiFactory.COLOR_AMBER, Color.parseColor("#C084FC")
        };
        String[] toolNames = new String[]{"Relays", "Mixnet", "Injector", "DNSCrypt", "Tor", "I2P"};
        final int[] toolTargets = new int[]{
                MullvadScreens.SUB_MULLVAD_RELAYS,
                NymVpnScreens.SUB_NYM_MIXNET_TUNING,
                HttpInjectorScreens.SUB_INJECTOR_TUNNEL_HUB,
                InviZibleModuleScreens.SUB_DNSCRYPT_SETTINGS,
                InviZibleModuleScreens.SUB_TOR_EXPANDED,
                InviZibleModuleScreens.SUB_PURPLE_I2P
        };
        LinearLayout currentToolRow = null;
        for (int ti = 0; ti < toolTargets.length; ti++) {
            final int tDest = toolTargets[ti];
            LinearLayout tile = new LinearLayout(this);
            tile.setOrientation(LinearLayout.VERTICAL);
            tile.setGravity(Gravity.CENTER);
            tile.setPadding(UiFactory.dp(this, 8), UiFactory.dp(this, 14), UiFactory.dp(this, 8), UiFactory.dp(this, 14));
            tile.setBackground(UiFactory.cardFill(this, UiFactory.COLOR_BORDER, 16f));
            ImageView g = UiFactory.iconWell(this, toolIconKinds[ti], toolIconColors[ti], 44f, 22f);
            TextView n = UiFactory.label(this, toolNames[ti], 11.5f, UiFactory.COLOR_MUTED, true, false);
            n.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams nLp2 = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            nLp2.topMargin = UiFactory.dp(this, 3);
            n.setLayoutParams(nLp2);
            tile.addView(g);
            tile.addView(n);
            tile.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { openConfigureSubScreen(tDest); }
            });

            if (ti % 2 == 0) {
                currentToolRow = new LinearLayout(this);
                currentToolRow.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rwLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                rwLp.bottomMargin = UiFactory.dp(this, 10);
                currentToolRow.setLayoutParams(rwLp);
                col.addView(currentToolRow);
                LinearLayout.LayoutParams lLp2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                lLp2.rightMargin = UiFactory.dp(this, 10);
                tile.setLayoutParams(lLp2);
            } else {
                tile.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            }
            currentToolRow.addView(tile);
        }

        // Bento Row 2: [Universal firewall rules] card + [Proxy Inactive/Active] card
        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams r2Lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, UiFactory.dp(this, 134));
        r2Lp.bottomMargin = UiFactory.dp(this, 14);
        row2.setLayoutParams(r2Lp);

        LinearLayout fwBento = new LinearLayout(this);
        fwBento.setOrientation(LinearLayout.VERTICAL);
        fwBento.setPadding(UiFactory.dp(this, 18), UiFactory.dp(this, 18), UiFactory.dp(this, 16), UiFactory.dp(this, 12));
        fwBento.setBackground(UiFactory.roundedRect(this, UiFactory.COLOR_CARD, UiFactory.COLOR_BORDER, 0.8f, 20f));
        LinearLayout.LayoutParams fbLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        fbLp.rightMargin = UiFactory.dp(this, 12);
        fwBento.setLayoutParams(fbLp);

        LinearLayout fwLine = new LinearLayout(this);
        fwLine.setOrientation(LinearLayout.VERTICAL);
        fwLine.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        fwLine.setGravity(Gravity.CENTER_VERTICAL);

        homeFwCardBigNumber = UiFactory.label(this, "0", 22f, UiFactory.COLOR_TEXT, true, false);
        homeFwCardCaption = UiFactory.label(this, "Universal firewall rules", 13.5f, UiFactory.COLOR_MUTED, false, false);
        fwLine.addView(homeFwCardBigNumber);
        fwLine.addView(homeFwCardCaption);

        homeFwDots = UiFactory.label(this, "●  •  •", 11f, UiFactory.COLOR_MUTED, true, false);
        homeFwDots.setGravity(Gravity.CENTER);
        fwBento.addView(fwLine);
        fwBento.addView(homeFwDots);
        fwBento.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                homeFwCardPage = (homeFwCardPage + 1) % 3;
                refreshLiveViews();
            }
        });

        LinearLayout proxyBento = new LinearLayout(this);
        proxyBento.setOrientation(LinearLayout.VERTICAL);
        proxyBento.setPadding(UiFactory.dp(this, 18), UiFactory.dp(this, 18), UiFactory.dp(this, 16), UiFactory.dp(this, 14));
        proxyBento.setBackground(UiFactory.roundedRect(this, UiFactory.COLOR_CARD, UiFactory.COLOR_BORDER, 0.8f, 20f));
        proxyBento.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        homeProxyCardTitle = UiFactory.label(this, "Proxy Inactive", 15.5f, UiFactory.COLOR_TEXT, false, false);
        homeProxyCardSub = UiFactory.label(this, "", 12f, UiFactory.COLOR_MUTED, false, false);
        LinearLayout.LayoutParams psLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        psLp.topMargin = UiFactory.dp(this, 6);
        homeProxyCardSub.setLayoutParams(psLp);
        proxyBento.addView(homeProxyCardTitle);
        proxyBento.addView(homeProxyCardSub);
        proxyBento.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openConfigureSubScreen(ConfigureScreens.SUB_PROXY);
            }
        });

        row2.addView(fwBento);
        row2.addView(proxyBento);
        col.addView(row2);

        // Bento Row 3: [Firewall • off / active   ›]
        LinearLayout fwRow = new LinearLayout(this);
        fwRow.setOrientation(LinearLayout.HORIZONTAL);
        fwRow.setGravity(Gravity.CENTER_VERTICAL);
        fwRow.setPadding(UiFactory.dp(this, 20), UiFactory.dp(this, 17), UiFactory.dp(this, 18), UiFactory.dp(this, 17));
        fwRow.setBackground(UiFactory.roundedRect(this, UiFactory.COLOR_CARD, UiFactory.COLOR_BORDER, 0.8f, 20f));
        LinearLayout.LayoutParams frLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        frLp.bottomMargin = UiFactory.dp(this, 12);
        fwRow.setLayoutParams(frLp);
        homeFirewallRowText = UiFactory.label(this, "Firewall • off", 15f, UiFactory.COLOR_MUTED, false, false);
        homeFirewallRowText.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        fwRow.addView(homeFirewallRowText);
        fwRow.addView(UiFactory.label(this, "›", 20f, UiFactory.COLOR_TEXT, false, false));
        fwRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openConfigureSubScreen(ConfigureScreens.SUB_FIREWALL);
            }
        });
        col.addView(fwRow);

        // Bento Row 4: [TCP / UDP & Anti-Censorship • ...   ›]
        LinearLayout tcpRow = new LinearLayout(this);
        tcpRow.setOrientation(LinearLayout.HORIZONTAL);
        tcpRow.setGravity(Gravity.CENTER_VERTICAL);
        tcpRow.setPadding(UiFactory.dp(this, 20), UiFactory.dp(this, 16), UiFactory.dp(this, 18), UiFactory.dp(this, 16));
        tcpRow.setBackground(UiFactory.roundedRect(this, UiFactory.COLOR_CARD, UiFactory.COLOR_BORDER, 0.8f, 20f));
        LinearLayout.LayoutParams trLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        trLp.bottomMargin = UiFactory.dp(this, 12);
        tcpRow.setLayoutParams(trLp);
        homeTcpDpiRowText = UiFactory.label(this, "", 13.5f, UiFactory.COLOR_MUTED, false, false);
        homeTcpDpiRowText.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        tcpRow.addView(homeTcpDpiRowText);
        tcpRow.addView(UiFactory.label(this, "›", 20f, UiFactory.COLOR_TEXT, false, false));
        tcpRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openConfigureSubScreen(ConfigureScreens.SUB_TCPIP_UDP);
            }
        });
        col.addView(tcpRow);

        // Bento Row 5: Quick Live DNS & Firewall Probe
        LinearLayout probeBox = UiFactory.card(this, UiFactory.COLOR_BORDER);
        probeBox.addView(UiFactory.sectionHeader(this, "Quick DNS & Firewall Probe", config.name + ".skid"));
        final EditText domEt = UiFactory.monoInput(this, "cloudflare.com", "Enter domain (e.g. doubleclick.net)", false);
        final TextView resTv = UiFactory.label(this, "Test live resolution & on-device blocklists", 11.5f, UiFactory.COLOR_MUTED, false, true);
        Button testBtn = UiFactory.actionButton(this, "Resolve Domain", UiFactory.COLOR_SURFACE, UiFactory.COLOR_SKY, UiFactory.COLOR_BORDER);
        testBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String dom = domEt.getText().toString().trim();
                if (dom.isEmpty()) return;
                resTv.setTextColor(UiFactory.COLOR_AMBER);
                resTv.setText("Resolving " + dom + "...");
                bgExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        SkidFirewallEngine fw = new SkidFirewallEngine();
                        fw.reloadFromConfig(config);
                        final SkidFirewallEngine.Verdict verdict = fw.evaluateDnsQuery(MainActivity.this, dom, "A");
                        if (!verdict.allowed) {
                            SkidEventBus.recordDnsEvent("A", dom, "BLOCKED [" + verdict.matchedRule + "]", 0, true);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    resTv.setTextColor(UiFactory.COLOR_CRIMSON);
                                    resTv.setText("BLOCKED → " + verdict.matchedRule);
                                }
                            });
                            return;
                        }
                        byte[] q = DnsPacketCodec.buildWireQuery(dom, 1);
                        final DnsUpstreamResolver.UpstreamResult r = DnsUpstreamResolver.resolve(null, config, q);
                        final String ip = r.wireResponse != null
                                ? DnsPacketCodec.extractAnswerSummary(r.wireResponse) : ("FAIL: " + r.error);
                        final String asn = (r.wireResponse != null && config.showProviderInfo)
                                ? DnsUpstreamResolver.describeProviderForAnswer(ip) : "";
                        SkidEventBus.recordDnsEvent("A", dom, ip + asn + " via " + r.protocolUsed, r.latencyMs, false);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                resTv.setTextColor(r.wireResponse != null ? UiFactory.COLOR_EMERALD : UiFactory.COLOR_CRIMSON);
                                resTv.setText(dom + " → " + ip + asn + "\n" + r.protocolUsed + " (" + r.latencyMs + " ms)");
                            }
                        });
                    }
                });
            }
        });
        probeBox.addView(domEt);
        probeBox.addView(testBtn);
        LinearLayout.LayoutParams rtLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rtLp.topMargin = UiFactory.dp(this, 8);
        resTv.setLayoutParams(rtLp);
        probeBox.addView(resTv);
        col.addView(probeBox);

        sv.addView(col);
        outer.addView(sv);

        // Docked Bottom Control Bar: [ || ]   [    START    ]   [ v ]
        LinearLayout dock = new LinearLayout(this);
        dock.setOrientation(LinearLayout.HORIZONTAL);
        dock.setGravity(Gravity.CENTER);
        dock.setPadding(UiFactory.dp(this, 22), UiFactory.dp(this, 10), UiFactory.dp(this, 22), UiFactory.dp(this, 16));

        int circleSize = UiFactory.dp(this, 48);
        homePauseBtn = UiFactory.iconView(this, SkidIcons.PAUSE, UiFactory.COLOR_TEXT, 20f);
        LinearLayout.LayoutParams pauseLp = new LinearLayout.LayoutParams(circleSize, circleSize);
        homePauseBtn.setLayoutParams(pauseLp);
        int pausePad = UiFactory.dp(this, 14);
        homePauseBtn.setPadding(pausePad, pausePad, pausePad, pausePad);
        homePauseBtn.setScaleType(ImageView.ScaleType.FIT_CENTER);
        homePauseBtn.setBackground(UiFactory.circleRing(this, UiFactory.COLOR_CARD, UiFactory.COLOR_BORDER, 1f));
        homePauseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!SkidEventBus.isRunning()) {
                    Toast.makeText(MainActivity.this, "Start SkidwareDNS first to pause/resume", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (SkidEventBus.isPaused()) {
                    startSkidService(SkidVpnService.ACTION_RESUME);
                } else {
                    startSkidService(SkidVpnService.ACTION_PAUSE);
                }
            }
        });

        homeStartPillBtn = new TextView(this);
        homeStartPillBtn.setText("S T A R T");
        homeStartPillBtn.setGravity(Gravity.CENTER);
        homeStartPillBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16.5f);
        homeStartPillBtn.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
        homeStartPillBtn.setCompoundDrawablesWithIntrinsicBounds(SkidIcons.drawable(this, SkidIcons.PLAY, Color.parseColor("#061018"), 18f), null, null, null);
        homeStartPillBtn.setCompoundDrawablePadding(UiFactory.dp(this, 8));
        homeStartPillBtn.setPadding(UiFactory.dp(this, 18), 0, UiFactory.dp(this, 18), 0);
        LinearLayout.LayoutParams startLp = new LinearLayout.LayoutParams(0, UiFactory.dp(this, 56), 1f);
        startLp.leftMargin = UiFactory.dp(this, 16);
        startLp.rightMargin = UiFactory.dp(this, 16);
        homeStartPillBtn.setLayoutParams(startLp);
        homeStartPillBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleStartStop();
            }
        });

        ImageView modeDropBtn = UiFactory.iconView(this, SkidIcons.CHEVRON_DOWN, UiFactory.COLOR_TEXT, 18f);
        LinearLayout.LayoutParams modeLp = new LinearLayout.LayoutParams(circleSize, circleSize);
        modeDropBtn.setLayoutParams(modeLp);
        int modePad = UiFactory.dp(this, 14);
        modeDropBtn.setPadding(modePad, modePad, modePad, modePad);
        modeDropBtn.setScaleType(ImageView.ScaleType.FIT_CENTER);
        modeDropBtn.setBackground(UiFactory.circleRing(this, UiFactory.COLOR_CARD, UiFactory.COLOR_BORDER, 1f));
        modeDropBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showQuickModeSelectorDialog();
            }
        });

        dock.addView(homePauseBtn);
        dock.addView(homeStartPillBtn);
        dock.addView(modeDropBtn);
        outer.addView(dock);

        return outer;
    }

    private View buildHomeLiveCard() {
        LinearLayout card = UiFactory.card(this, UiFactory.COLOR_BORDER);
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = UiFactory.label(this, "LIVE TRAFFIC", 11.5f, UiFactory.COLOR_SKY, true, false);
        title.setLetterSpacing(0.08f);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        homeBytesLine = UiFactory.label(this, "0 B  ·  0 pkts", 11.5f, UiFactory.COLOR_MUTED, true, true);
        head.addView(title);
        head.addView(homeBytesLine);
        card.addView(head);

        homeChart = new LiveChartView(this);
        LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, UiFactory.dp(this, 112));
        cLp.topMargin = UiFactory.dp(this, 8);
        homeChart.setLayoutParams(cLp);
        homeChart.setSeriesTap(new LiveChartView.SeriesTap() {
            @Override public void onSeries(boolean blocked) {
                showTrafficList(blocked);
            }
        });
        card.addView(homeChart);

        LinearLayout legend = new LinearLayout(this);
        legend.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams lLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lLp.topMargin = UiFactory.dp(this, 10);
        legend.setLayoutParams(lLp);
        homeAllowChip = legendChip("ALLOWED  0", UiFactory.COLOR_EMERALD, false);
        homeBlockChip = legendChip("BLOCKED  0", UiFactory.COLOR_CRIMSON, true);
        legend.addView(homeAllowChip);
        View gap = new View(this);
        gap.setLayoutParams(new LinearLayout.LayoutParams(UiFactory.dp(this, 8), 1));
        legend.addView(gap);
        legend.addView(homeBlockChip);
        card.addView(legend);

        card.addView(UiFactory.label(this, "Tap Allowed / Blocked (or the chart) to inspect",
                11f, UiFactory.COLOR_MUTED, false, false));

        LinearLayout logHead = new LinearLayout(this);
        logHead.setOrientation(LinearLayout.HORIZONTAL);
        logHead.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams lhLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lhLp.topMargin = UiFactory.dp(this, 14);
        logHead.setLayoutParams(lhLp);
        TextView lt = UiFactory.label(this, "RECENT", 11.5f, UiFactory.COLOR_SKY, true, false);
        lt.setLetterSpacing(0.08f);
        lt.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView more = UiFactory.label(this, "Logs  >", 12f, UiFactory.COLOR_MUTED, true, false);
        more.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { switchTab(4); }
        });
        logHead.addView(lt);
        logHead.addView(more);
        card.addView(logHead);

        homeMiniLogs = new LinearLayout(this);
        homeMiniLogs.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams mlLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mlLp.topMargin = UiFactory.dp(this, 6);
        homeMiniLogs.setLayoutParams(mlLp);
        card.addView(homeMiniLogs);
        refreshHomeLive();
        UiFactory.fadeIn(card, 0);
        return card;
    }

    private TextView legendChip(String text, final int accent, final boolean blocked) {
        TextView tv = UiFactory.label(this, text, 12f, accent, true, false);
        tv.setPadding(UiFactory.dp(this, 12), UiFactory.dp(this, 8), UiFactory.dp(this, 12), UiFactory.dp(this, 8));
        tv.setBackground(UiFactory.roundedRect(this,
                UiFactory.mixColor(UiFactory.COLOR_ICON_BG, accent, 0.22f),
                accent, 0.9f, 14f));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tv.setLayoutParams(lp);
        tv.setGravity(Gravity.CENTER);
        tv.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showTrafficList(blocked); }
        });
        return tv;
    }

    private void showTrafficList(boolean blocked) {
        java.util.List<SkidEventBus.LogEntry> rows = SkidEventBus.snapshotByBlocked(blocked, 16);
        String title = blocked ? "Blocked" : "Allowed";
        if (rows.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle(title)
                    .setMessage("Nothing " + title.toLowerCase() + " yet. Start the engine or run a DNS probe.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }
        CharSequence[] items = new CharSequence[rows.size()];
        for (int i = 0; i < rows.size(); i++) {
            SkidEventBus.LogEntry e = rows.get(i);
            items[i] = e.timeFormatted + "  " + e.target + "\n" + e.result;
        }
        new AlertDialog.Builder(this)
                .setTitle(title + "  ·  " + (blocked ? SkidEventBus.threatsBlocked.get() : SkidEventBus.queriesResolved.get()))
                .setItems(items, null)
                .setPositiveButton("Full logs", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) { switchTab(4); }
                })
                .show();
    }

    private void refreshHomeLive() {
        if (homeChart != null) homeChart.invalidate();
        long allow = SkidEventBus.queriesResolved.get();
        long block = SkidEventBus.threatsBlocked.get();
        if (homeAllowChip != null) homeAllowChip.setText("ALLOWED  " + allow);
        if (homeBlockChip != null) homeBlockChip.setText("BLOCKED  " + block);
        if (homeBytesLine != null) {
            long up = SkidEventBus.bytesOut.get();
            long down = SkidEventBus.bytesIn.get();
            long pk = SkidEventBus.packetsSeen.get()
                    + SkidEventBus.tcpStreamsHandled.get()
                    + SkidEventBus.udpPacketsHandled.get();
            homeBytesLine.setText("\u2191 " + SkidEventBus.formatBytes(up)
                    + "   \u2193 " + SkidEventBus.formatBytes(down)
                    + "   ·  " + pk + " pkts");
        }
        if (homeMiniLogs == null) return;
        homeMiniLogs.removeAllViews();
        java.util.List<SkidEventBus.LogEntry> rec = SkidEventBus.snapshotRecent(6);
        if (rec.isEmpty()) {
            homeMiniLogs.addView(UiFactory.label(this, "Waiting for DNS / firewall events…",
                    12f, UiFactory.COLOR_MUTED, false, false));
            return;
        }
        for (int i = 0; i < rec.size(); i++) {
            final SkidEventBus.LogEntry e = rec.get(i);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, UiFactory.dp(this, 4), 0, UiFactory.dp(this, 4));
            int accent = e.blocked ? UiFactory.COLOR_CRIMSON : UiFactory.COLOR_EMERALD;
            TextView tag = UiFactory.label(this, e.blocked ? "BLK" : "OK", 10f, accent, true, true);
            tag.setPadding(UiFactory.dp(this, 6), UiFactory.dp(this, 2), UiFactory.dp(this, 6), UiFactory.dp(this, 2));
            tag.setBackground(UiFactory.roundedRect(this,
                    UiFactory.mixColor(UiFactory.COLOR_ICON_BG, accent, 0.25f), accent, 0.8f, 8f));
            TextView body = UiFactory.label(this, e.target, 12.5f, UiFactory.COLOR_TEXT, false, true);
            body.setPadding(UiFactory.dp(this, 8), 0, 0, 0);
            body.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            row.addView(tag);
            row.addView(body);
            row.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { showTrafficList(e.blocked); }
            });
            homeMiniLogs.addView(row);
        }
    }

    /** Home chip label summarising the active privacy modules, e.g. "DNS+TOR+I2P". */
    private String privacyChipLabel() {
        StringBuilder sb = new StringBuilder();
        if (config.dnscryptModuleEnabled) sb.append("DNS+");
        if (config.torEnabled) sb.append("TOR+");
        if (config.i2pEnabled) sb.append("I2P+");
        if (sb.length() == 0) return "PRIVACY OFF";
        return sb.substring(0, sb.length() - 1);
    }

    private void showQuickModeSelectorDialog() {
        final String[] opts = com.skidwaredns.app.model.OperatingModes.NAMES;
        final String[] blurbs = com.skidwaredns.app.model.OperatingModes.BLURBS;
        CharSequence[] rows = new CharSequence[opts.length];
        int checked = 0;
        for (int i = 0; i < opts.length; i++) {
            rows[i] = opts[i] + "\n" + blurbs[i];
            if (opts[i].equalsIgnoreCase(config.mode)) { checked = i; }
        }
        new AlertDialog.Builder(this)
                .setTitle("Operating Mode (" + config.name + ".skid)")
                .setSingleChoiceItems(rows, checked, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        com.skidwaredns.app.model.OperatingModes.apply(config, opts[which]);
                        persistAndReload("Mode -> " + config.mode);
                    }
                })
                .setNeutralButton("Switch .skid Preset", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switchTab(2);
                    }
                })
                .show();
    }

    // =========================================================================
    // TAB 1: LIVE STATS (TCP / UDP / DNS / Cache / DPI / PCAP)
    // =========================================================================
    private View buildStatsScreen() {
        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        int p = UiFactory.dp(this, 15);
        col.setPadding(p, p, p, p);

        col.addView(UiFactory.label(this, "Live Stats", 22f, UiFactory.COLOR_TEXT, true, false));
        col.addView(UiFactory.label(this, "Allowed vs blocked, sockets, bytes and RTT — tap the chart",
                12.5f, UiFactory.COLOR_MUTED, false, false));

        LinearLayout chartCard = UiFactory.card(this, UiFactory.COLOR_BORDER);
        LinearLayout.LayoutParams chLp = (LinearLayout.LayoutParams) chartCard.getLayoutParams();
        if (chLp != null) chLp.topMargin = UiFactory.dp(this, 14);
        TextView liveCap = UiFactory.label(this, "TRAFFIC", 11.5f, UiFactory.COLOR_SKY, true, false);
        liveCap.setLetterSpacing(0.08f);
        chartCard.addView(liveCap);
        statsChart = new LiveChartView(this, 148);
        LinearLayout.LayoutParams chartLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, UiFactory.dp(this, 148));
        chartLp.topMargin = UiFactory.dp(this, 8);
        statsChart.setLayoutParams(chartLp);
        statsChart.setSeriesTap(new LiveChartView.SeriesTap() {
            @Override public void onSeries(boolean blocked) { showTrafficList(blocked); }
        });
        chartCard.addView(statsChart);
        col.addView(chartCard);

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rLp.topMargin = UiFactory.dp(this, 10);
        row1.setLayoutParams(rLp);
        LinearLayout dnsTile = UiFactory.statTile(this, "DNS OK", UiFactory.COLOR_SKY);
        LinearLayout blkTile = UiFactory.statTile(this, "BLOCKED", UiFactory.COLOR_CRIMSON);
        LinearLayout.LayoutParams dLp = (LinearLayout.LayoutParams) dnsTile.getLayoutParams();
        dLp.rightMargin = UiFactory.dp(this, 8);
        statsDnsTv = UiFactory.statTileValue(dnsTile);
        statsBlkTv = UiFactory.statTileValue(blkTile);
        row1.addView(dnsTile);
        row1.addView(blkTile);
        col.addView(row1);

        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams r2Lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        r2Lp.topMargin = UiFactory.dp(this, 8);
        row2.setLayoutParams(r2Lp);
        LinearLayout tcpTile = UiFactory.statTile(this, "TCP", UiFactory.COLOR_EMERALD);
        LinearLayout udpTile = UiFactory.statTile(this, "UDP", UiFactory.COLOR_AMBER);
        LinearLayout.LayoutParams tLp = (LinearLayout.LayoutParams) tcpTile.getLayoutParams();
        tLp.rightMargin = UiFactory.dp(this, 8);
        statsTcpTv = UiFactory.statTileValue(tcpTile);
        statsUdpTv = UiFactory.statTileValue(udpTile);
        row2.addView(tcpTile);
        row2.addView(udpTile);
        col.addView(row2);

        LinearLayout row3 = new LinearLayout(this);
        row3.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams r3Lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        r3Lp.topMargin = UiFactory.dp(this, 8);
        row3.setLayoutParams(r3Lp);
        LinearLayout bytTile = UiFactory.statTile(this, "BYTES", UiFactory.COLOR_PURPLE);
        LinearLayout rttTile = UiFactory.statTile(this, "RTT", UiFactory.COLOR_SKY);
        LinearLayout.LayoutParams bTileLp = (LinearLayout.LayoutParams) bytTile.getLayoutParams();
        bTileLp.rightMargin = UiFactory.dp(this, 8);
        statsBytesTv = UiFactory.statTileValue(bytTile);
        statsRttTv = UiFactory.statTileValue(rttTile);
        row3.addView(bytTile);
        row3.addView(rttTile);
        col.addView(row3);

        LinearLayout modeCard = UiFactory.card(this, UiFactory.COLOR_BORDER);
        LinearLayout.LayoutParams mLp = (LinearLayout.LayoutParams) modeCard.getLayoutParams();
        if (mLp != null) mLp.topMargin = UiFactory.dp(this, 12);
        statsModeTv = UiFactory.label(this, "", 14f, UiFactory.COLOR_TEXT, true, false);
        statsBadgesTv = UiFactory.label(this, "", 12.5f, UiFactory.COLOR_MUTED, false, false);
        LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        badgeLp.topMargin = UiFactory.dp(this, 4);
        statsBadgesTv.setLayoutParams(badgeLp);
        modeCard.addView(statsModeTv);
        modeCard.addView(statsBadgesTv);
        col.addView(modeCard);

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bLp.topMargin = UiFactory.dp(this, 12);
        btnRow.setLayoutParams(bLp);

        Button flushBtn = UiFactory.actionButton(this, "Flush DNS cache", UiFactory.COLOR_SURFACE, UiFactory.COLOR_SKY, UiFactory.COLOR_BORDER);
        flushBtn.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        flushBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DnsUpstreamResolver.flushCache();
                refreshLiveViews();
                Toast.makeText(MainActivity.this, "Flushed DNS Booster cache", Toast.LENGTH_SHORT).show();
            }
        });
        View sp = new View(this);
        sp.setLayoutParams(new LinearLayout.LayoutParams(UiFactory.dp(this, 8), 1));
        Button resetBtn = UiFactory.actionButton(this, "Reset", UiFactory.COLOR_SURFACE, UiFactory.COLOR_CRIMSON, UiFactory.COLOR_BORDER);
        resetBtn.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        resetBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SkidEventBus.resetCounters();
                refreshLiveViews();
                UiFactory.pulseOnce(row1);
            }
        });
        btnRow.addView(flushBtn);
        btnRow.addView(sp);
        btnRow.addView(resetBtn);
        col.addView(btnRow);

        LinearLayout dumpCard = UiFactory.card(this, UiFactory.COLOR_BORDER);
        LinearLayout.LayoutParams dumpLp = (LinearLayout.LayoutParams) dumpCard.getLayoutParams();
        if (dumpLp != null) dumpLp.topMargin = UiFactory.dp(this, 12);
        dumpCard.addView(UiFactory.label(this, "DETAILS", 11.5f, UiFactory.COLOR_SKY, true, false));
        statsGridSummaryTv = UiFactory.label(this, "", 12f, UiFactory.COLOR_MUTED, false, true);
        dumpCard.addView(statsGridSummaryTv);
        col.addView(dumpCard);

        UiFactory.fadeIn(chartCard, 0);
        UiFactory.fadeIn(row1, 40);
        UiFactory.fadeIn(row2, 80);
        UiFactory.fadeIn(row3, 120);
        UiFactory.fadeIn(modeCard, 160);

        sv.addView(col);
        return sv;
    }

    // =========================================================================
    // TAB 2: [.SKID] CUSTOM CONFIG STUDIO
    // =========================================================================
    private View buildSkidStudioScreen() {
        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        int p = UiFactory.dp(this, 15);
        col.setPadding(p, p, p, p);

        col.addView(UiFactory.label(this, ".skid Config Studio", 20f, UiFactory.COLOR_TEXT, true, false));
        col.addView(UiFactory.label(this, "Custom bracket + key=value configuration format", 12.5f, UiFactory.COLOR_MUTED, false, false));

        col.addView(UiFactory.categoryTitle(this, "Built-in .skid Presets"));
        LinearLayout presetCard = UiFactory.card(this, UiFactory.COLOR_BORDER);
        for (final SkidConfigStore.PresetTemplate tpl : SkidConfigStore.BUILTIN_PRESETS) {
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            int ip = UiFactory.dp(this, 10);
            item.setPadding(ip, ip, ip, ip);
            item.setBackground(UiFactory.roundedRect(this, UiFactory.COLOR_SURFACE, UiFactory.COLOR_BORDER, 0.8f, 10f));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = UiFactory.dp(this, 7);
            item.setLayoutParams(lp);
            item.addView(UiFactory.label(this, "✦ " + tpl.title + " (" + tpl.filename + ")", 13f, UiFactory.COLOR_SKY, true, false));
            item.addView(UiFactory.label(this, tpl.description, 11.5f, UiFactory.COLOR_MUTED, false, false));
            item.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SkidConfigParser.ParseResult res = SkidConfigStore.saveActiveFromRaw(MainActivity.this, tpl.skidContent);
                    config = res.config;
                    if (skidRawEditor != null) skidRawEditor.setText(SkidConfigParser.serialize(config));
                    if (skidDiagnosticBanner != null) skidDiagnosticBanner.setText(res.buildSummary());
                    persistAndReload("Loaded preset " + tpl.filename);
                    Toast.makeText(MainActivity.this, "Loaded " + tpl.filename, Toast.LENGTH_SHORT).show();
                }
            });
            presetCard.addView(item);
        }
        col.addView(presetCard);

        // Import / Export / Share row
        LinearLayout ioRow = new LinearLayout(this);
        ioRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams ioLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ioLp.bottomMargin = UiFactory.dp(this, 12);
        ioRow.setLayoutParams(ioLp);

        Button impBtn = UiFactory.actionButton(this, "Import .skid", UiFactory.COLOR_CARD, UiFactory.COLOR_SKY, UiFactory.COLOR_BORDER);
        impBtn.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        impBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { triggerImportSkid(); }
        });
        View s1 = new View(this); s1.setLayoutParams(new LinearLayout.LayoutParams(UiFactory.dp(this, 8), 1));

        Button expBtn = UiFactory.actionButton(this, "Export .skid", UiFactory.COLOR_CARD, UiFactory.COLOR_EMERALD, UiFactory.COLOR_BORDER);
        expBtn.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        expBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { triggerExportSkid(); }
        });
        View s2 = new View(this); s2.setLayoutParams(new LinearLayout.LayoutParams(UiFactory.dp(this, 8), 1));

        Button shareBtn = UiFactory.actionButton(this, "Share", UiFactory.COLOR_CARD, UiFactory.COLOR_AMBER, UiFactory.COLOR_BORDER);
        shareBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String txt = skidRawEditor != null ? skidRawEditor.getText().toString() : SkidConfigParser.serialize(config);
                Intent send = new Intent(Intent.ACTION_SEND);
                send.setType("text/plain");
                send.putExtra(Intent.EXTRA_SUBJECT, config.name + ".skid");
                send.putExtra(Intent.EXTRA_TEXT, txt);
                startActivity(Intent.createChooser(send, "Share .skid"));
            }
        });
        ioRow.addView(impBtn);
        ioRow.addView(s1);
        ioRow.addView(expBtn);
        ioRow.addView(s2);
        ioRow.addView(shareBtn);
        col.addView(ioRow);

        // Raw .skid Editor Card
        LinearLayout edCard = UiFactory.card(this, UiFactory.COLOR_BORDER);
        skidDiagnosticBanner = UiFactory.label(this, "Active: " + config.name + ".skid", 11.5f, UiFactory.COLOR_EMERALD, false, true);
        edCard.addView(skidDiagnosticBanner);

        skidRawEditor = UiFactory.monoInput(this, SkidConfigParser.serialize(config), "# .skid config...", true);
        skidRawEditor.setMinLines(18);
        edCard.addView(skidRawEditor);

        LinearLayout edBtns = new LinearLayout(this);
        edBtns.setOrientation(LinearLayout.HORIZONTAL);
        Button lintBtn = UiFactory.actionButton(this, "Validate", UiFactory.COLOR_SURFACE, UiFactory.COLOR_SKY, UiFactory.COLOR_BORDER);
        lintBtn.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        lintBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SkidConfigParser.ParseResult res = SkidConfigParser.parse(skidRawEditor.getText().toString());
                skidDiagnosticBanner.setTextColor(res.valid ? UiFactory.COLOR_EMERALD : UiFactory.COLOR_CRIMSON);
                skidDiagnosticBanner.setText(res.buildSummary());
            }
        });
        View s3 = new View(this); s3.setLayoutParams(new LinearLayout.LayoutParams(UiFactory.dp(this, 8), 1));
        Button applyBtn = UiFactory.actionButton(this, "Parse & Apply .skid", UiFactory.COLOR_CARD_SELECTED, UiFactory.COLOR_SKY, UiFactory.COLOR_SKY);
        applyBtn.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.4f));
        applyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SkidConfigParser.ParseResult res = SkidConfigStore.saveActiveFromRaw(
                        MainActivity.this, skidRawEditor.getText().toString());
                config = res.config;
                skidDiagnosticBanner.setTextColor(res.valid ? UiFactory.COLOR_EMERALD : UiFactory.COLOR_AMBER);
                skidDiagnosticBanner.setText("Applied → " + res.buildSummary());
                persistAndReload("Applied edited .skid [" + config.name + "]");
                Toast.makeText(MainActivity.this, "Applied " + config.name + ".skid", Toast.LENGTH_SHORT).show();
            }
        });
        edBtns.addView(lintBtn);
        edBtns.addView(s3);
        edBtns.addView(applyBtn);
        edCard.addView(edBtns);
        col.addView(edCard);

        sv.addView(col);
        return sv;
    }

    // =========================================================================
    // TAB 4: LIVE PACKET, TCP, UDP, DNS & FIREWALL LOGS
    // =========================================================================
    private View buildLogsScreen() {
        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        int p = UiFactory.dp(this, 15);
        col.setPadding(p, p, p, p);

        LinearLayout headerCard = UiFactory.card(this, UiFactory.COLOR_BORDER);
        headerCard.addView(UiFactory.sectionHeader(this, "Live Packet & App Logs",
                "Level=" + config.logLevel + " • PCAP=" + config.packetCaptureMode));
        headerCard.addView(UiFactory.buildChipSelector(this,
                new String[]{"ALL", "DNS", "BLOCKED", "TCP", "UDP", "PROXY", "TUNNEL", "SYSTEM"},
                logFilter, UiFactory.COLOR_SKY, new UiFactory.ChipSelectListener() {
                    @Override
                    public void onSelected(String value) {
                        logFilter = value;
                        populateLogs();
                    }
                }));

        LinearLayout actRow = new LinearLayout(this);
        actRow.setOrientation(LinearLayout.HORIZONTAL);
        Button copyBtn = UiFactory.actionButton(this, "Copy", UiFactory.COLOR_SURFACE, UiFactory.COLOR_SKY, UiFactory.COLOR_BORDER);
        copyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StringBuilder sb = new StringBuilder();
                for (SkidEventBus.LogEntry e : SkidEventBus.snapshotLogs()) {
                    sb.append(e.timeFormatted).append(" [").append(e.category).append('/').append(e.level).append("] ")
                            .append(e.qtype).append(' ').append(e.target).append(" -> ").append(e.result).append('\n');
                }
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null) {
                    cm.setPrimaryClip(ClipData.newPlainText("SkidwareDNS Logs", sb.toString()));
                    Toast.makeText(MainActivity.this, "Copied logs", Toast.LENGTH_SHORT).show();
                }
            }
        });
        View s1 = new View(this); s1.setLayoutParams(new LinearLayout.LayoutParams(UiFactory.dp(this, 8), 1));
        Button pcapBtn = UiFactory.actionButton(this, "PCAP Settings", UiFactory.COLOR_SURFACE, UiFactory.COLOR_EMERALD, UiFactory.COLOR_BORDER);
        pcapBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { openConfigureSubScreen(ConfigureScreens.SUB_LOGS_SETTINGS); }
        });
        View s2 = new View(this); s2.setLayoutParams(new LinearLayout.LayoutParams(UiFactory.dp(this, 8), 1));
        Button clrBtn = UiFactory.actionButton(this, "Clear", UiFactory.COLOR_SURFACE, UiFactory.COLOR_CRIMSON, UiFactory.COLOR_BORDER);
        clrBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SkidEventBus.clearLogs();
                populateLogs();
            }
        });
        actRow.addView(copyBtn);
        actRow.addView(s1);
        actRow.addView(pcapBtn);
        actRow.addView(s2);
        actRow.addView(clrBtn);
        headerCard.addView(actRow);
        col.addView(headerCard);

        liveLogsListContainer = new LinearLayout(this);
        liveLogsListContainer.setOrientation(LinearLayout.VERTICAL);
        col.addView(liveLogsListContainer);
        populateLogs();

        sv.addView(col);
        return sv;
    }

    private void populateLogs() {
        if (liveLogsListContainer == null) return;
        liveLogsListContainer.removeAllViews();
        List<SkidEventBus.LogEntry> all = SkidEventBus.snapshotLogs();
        int count = 0;
        for (SkidEventBus.LogEntry e : all) {
            if (!"ALL".equals(logFilter) && !logFilter.equalsIgnoreCase(e.category)) continue;
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            int pad = UiFactory.dp(this, 11);
            card.setPadding(pad, pad, pad, pad);
            int stroke = e.blocked ? UiFactory.COLOR_CRIMSON
                    : ("DNS".equals(e.category) ? UiFactory.COLOR_EMERALD
                    : ("TCP".equals(e.category) || "UDP".equals(e.category) ? UiFactory.COLOR_SKY : UiFactory.COLOR_BORDER));
            card.setBackground(UiFactory.roundedRect(this, UiFactory.COLOR_CARD, stroke, 0.9f, 12f));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = UiFactory.dp(this, 7);
            card.setLayoutParams(lp);

            String rtt = e.latencyMs >= 0 ? (" • " + e.latencyMs + " ms") : "";
            card.addView(UiFactory.label(this,
                    e.timeFormatted + "  [" + e.category + "]  " + e.qtype + rtt,
                    10.5f, e.blocked ? UiFactory.COLOR_CRIMSON : UiFactory.COLOR_SKY, true, true));
            card.addView(UiFactory.label(this, e.target, 13f, UiFactory.COLOR_TEXT, true, true));
            card.addView(UiFactory.label(this, e.result, 11.5f, UiFactory.COLOR_MUTED, false, true));

            liveLogsListContainer.addView(card);
            if (++count >= 120) break;
        }
        if (count == 0) {
            liveLogsListContainer.addView(UiFactory.label(this,
                    "No entries for [" + logFilter + "]. Tap START on Home or run a Quick DNS Probe.",
                    12.5f, UiFactory.COLOR_MUTED, false, false));
        }
    }

    // =========================================================================
    // HOST CALLBACKS & VPN ACTIONS
    // =========================================================================
    @Override
    public SkidConfig getConfig() {
        return config;
    }

    @Override
    public ExecutorService getBgExecutor() {
        return bgExecutor;
    }

    @Override
    public void persistAndReload(String note) {
        SkidConfigStore.saveActive(this, config);
        SkidVpnService.syncLoggingFlags(config);
        SkidEventBus.setActiveProfileName(config.name);
        SkidEventBus.setActiveDnsBadge(SkidVpnService.describeDnsSelection(config));
        SkidEventBus.setActiveTunnelBadge(config.tunnel + " • " + config.connectFromMode);
        if (note != null) {
            SkidEventBus.logSystem("SYSTEM", "Config", note);
        }
        if (SkidEventBus.isRunning() && !SkidEventBus.isPaused()) {
            startSkidService(SkidVpnService.ACTION_RELOAD);
        }
        refreshLiveViews();
    }

    @Override
    public void openConfigureSubScreen(int subId) {
        currentConfigureSub = subId;
        switchTab(3);
    }

    @Override
    public void jumpToMainTab(int tabIdx) {
        switchTab(tabIdx);
    }

    @Override
    public void triggerImportSkid() {
        Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        pick.addCategory(Intent.CATEGORY_OPENABLE);
        pick.setType("*/*");
        startActivityForResult(pick, REQ_IMPORT_SKID);
    }

    @Override
    public void triggerExportSkid() {
        Intent create = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        create.addCategory(Intent.CATEGORY_OPENABLE);
        create.setType("application/octet-stream");
        create.putExtra(Intent.EXTRA_TITLE, SkidConfigStore.sanitizeFilename(config.name) + ".skid");
        startActivityForResult(create, REQ_EXPORT_SKID);
    }

    @Override
    public void triggerExportPcap() {
        if (PcapRecorder.getCaptureFile(this) == null) {
            Toast.makeText(this, "No .pcap recorded yet — set Packet capture to Headers-Only or Full-PCAP and start VPN", Toast.LENGTH_LONG).show();
            return;
        }
        Intent create = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        create.addCategory(Intent.CATEGORY_OPENABLE);
        create.setType("application/vnd.tcpdump.pcap");
        create.putExtra(Intent.EXTRA_TITLE, "skidwaredns_capture.pcap");
        startActivityForResult(create, REQ_EXPORT_PCAP);
    }

    private void toggleStartStop() {
        if (SkidEventBus.isPaused()) {
            startSkidService(SkidVpnService.ACTION_RESUME);
            return;
        }
        if (SkidEventBus.isRunning()) {
            startService(new Intent(this, SkidVpnService.class).setAction(SkidVpnService.ACTION_STOP));
            return;
        }
        if (!com.skidwaredns.app.model.OperatingModes.usesVpnTun(config.mode)) {
            startSkidService(SkidVpnService.ACTION_START);
            return;
        }
        Intent prep = VpnService.prepare(this);
        if (prep != null) {
            startActivityForResult(prep, REQ_VPN_PREPARE);
        } else {
            startSkidService(SkidVpnService.ACTION_START);
        }
    }

    private void startSkidService(String action) {
        Intent svc = new Intent(this, SkidVpnService.class).setAction(action);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(svc);
        } else {
            startService(svc);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_VPN_PREPARE && resultCode == RESULT_OK) {
            startSkidService(SkidVpnService.ACTION_START);
        } else if (requestCode == REQ_IMPORT_SKID && resultCode == RESULT_OK && data != null && data.getData() != null) {
            importSkidUri(data.getData());
        } else if (requestCode == REQ_EXPORT_SKID && resultCode == RESULT_OK && data != null && data.getData() != null) {
            exportSkidUri(data.getData());
        } else if (requestCode == REQ_EXPORT_PCAP && resultCode == RESULT_OK && data != null && data.getData() != null) {
            try (OutputStream os = getContentResolver().openOutputStream(data.getData())) {
                if (os != null && PcapRecorder.exportToStream(this, os)) {
                    Toast.makeText(this, "Exported Wireshark .pcap successfully!", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, "PCAP export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void handleIncomingSkidIntent(Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (Intent.ACTION_VIEW.equals(action) && intent.getData() != null) {
            importSkidUri(intent.getData());
            switchTab(2);
        } else if (Intent.ACTION_SEND.equals(action)) {
            String txt = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (txt != null && (txt.contains("===") || txt.contains("DNS Mode="))) {
                config = SkidConfigStore.saveActiveFromRaw(this, txt).config;
                persistAndReload("Imported shared .skid [" + config.name + "]");
                switchTab(2);
            }
        }
    }

    private void importSkidUri(Uri uri) {
        try (InputStream is = getContentResolver().openInputStream(uri)) {
            if (is == null) return;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) != -1) baos.write(buf, 0, n);
            String raw = new String(baos.toByteArray(), StandardCharsets.UTF_8);
            SkidConfigParser.ParseResult res = SkidConfigStore.saveActiveFromRaw(this, raw);
            config = res.config;
            persistAndReload("Imported .skid -> " + config.name);
            if (currentTab == 2) switchTab(2);
            Toast.makeText(this, "Imported " + config.name + ".skid", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Import error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void exportSkidUri(Uri uri) {
        try (OutputStream os = getContentResolver().openOutputStream(uri)) {
            if (os == null) return;
            String raw = (skidRawEditor != null && currentTab == 2)
                    ? skidRawEditor.getText().toString() : SkidConfigParser.serialize(config);
            os.write(raw.getBytes(StandardCharsets.UTF_8));
            os.flush();
            Toast.makeText(this, "Saved " + config.name + ".skid", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Export error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_POST_NOTIF);
            }
        }
    }

    // =========================================================================
    // EVENT BUS LISTENERS & UI REFRESH
    // =========================================================================
    @Override
    public void onStateChanged(SkidEventBus.EngineState state, String statusLine) {
        refreshLiveViews();
    }

    @Override
    public void onTelemetryUpdated() {
        refreshLiveViews();
    }

    @Override
    public void onLogAdded(SkidEventBus.LogEntry entry) {
        if (currentTab == 4 && liveLogsListContainer != null) {
            populateLogs();
        }
        if (currentTab == 0) refreshHomeLive();
    }

    @Override
    public void onFirewallBubbleAlert(String target, String rule) {
        if (firewallBubbleBanner != null && config.firewallBubble) {
            firewallBubbleBanner.setText("🛡 Firewall Bubble: Blocked " + target + " (" + rule + ")");
            firewallBubbleBanner.setVisibility(View.VISIBLE);
        }
    }

    private void refreshLiveViews() {
        SkidEventBus.EngineState st = SkidEventBus.getState();
        boolean active = (st == SkidEventBus.EngineState.TUN_ACTIVE || st == SkidEventBus.EngineState.PROXY_ONLY_ACTIVE);
        boolean paused = (st == SkidEventBus.EngineState.PAUSED);

        if (homeProtectionHeader != null) {
            if (active) {
                homeProtectionHeader.setText("PROTECTED • " + config.name.toUpperCase());
                homeProtectionHeader.setTextColor(UiFactory.COLOR_EMERALD);
            } else if (paused) {
                homeProtectionHeader.setText("PAUSED • SNOOZED");
                homeProtectionHeader.setTextColor(UiFactory.COLOR_AMBER);
            } else {
                homeProtectionHeader.setText("NOT PROTECTED");
                homeProtectionHeader.setTextColor(UiFactory.COLOR_CRIMSON);
            }
        }

        if (homeDnsPillText != null) {
            homeDnsPillText.setText(active
                    ? ("DNS • " + config.globalDnsCategory)
                    : ("DNS • " + (paused ? "paused" : "off") + " (" + config.globalDnsCategory + ")"));
        }
        if (homeStatePillText != null) {
            homeStatePillText.setText(active ? "Active" : (paused ? "Paused" : "Inactive"));
            homeStatePillText.setTextColor(active ? UiFactory.COLOR_EMERALD : UiFactory.COLOR_TEXT);
        }

        if (homeFwCardBigNumber != null && homeFwCardCaption != null && homeFwDots != null) {
            if (homeFwCardPage == 0) {
                homeFwCardBigNumber.setText(String.valueOf(config.countActiveUniversalRules()));
                homeFwCardCaption.setText("Universal firewall rules");
                homeFwDots.setText("●  •  •");
            } else if (homeFwCardPage == 1) {
                homeFwCardBigNumber.setText(String.valueOf(config.firewallRules.size()));
                homeFwCardCaption.setText("Custom .skid rules");
                homeFwDots.setText("•  ●  •");
            } else {
                homeFwCardBigNumber.setText(String.valueOf(SkidEventBus.threatsBlocked.get()));
                homeFwCardCaption.setText("Threats blocked");
                homeFwDots.setText("•  •  ●");
            }
        }

        if (homeProxyCardTitle != null && homeProxyCardSub != null) {
            boolean hasProxy = active && !"None".equalsIgnoreCase(config.proxyMode);
            homeProxyCardTitle.setText(hasProxy ? "Proxy Active" : "Proxy Inactive");
            String sub = (config.wgEnabled ? config.wgProfileName : config.tunnel)
                    + "\nSOCKS5 :" + config.getPrimaryLocalSocksPort()
                    + (config.torEnabled ? " • Orbot" : "");
            homeProxyCardSub.setText(sub);
        }

        if (homeFirewallRowText != null) {
            String fwState = active
                    ? ("Firewall • active (" + (config.onDeviceBlocklistsEnabled ? config.activeBlocklistPacks.size() + " packs" : "custom") + ")")
                    : ("Firewall • off (" + config.firewallRules.size() + " rules ready)");
            homeFirewallRowText.setText(fwState);
            homeFirewallRowText.setTextColor(active ? UiFactory.COLOR_TEXT : UiFactory.COLOR_MUTED);
        }

        if (homeTcpDpiRowText != null) {
            homeTcpDpiRowText.setText("TCP/UDP • Buf " + config.socketBufferSizeKb + " KB • MSS "
                    + config.tcpMssClamp + " • DPI " + config.dpiStrategy + "/" + config.dpiTechnique);
        }
        refreshHomeLive();

        if (homePauseBtn != null) {
            int pauseKind = paused ? SkidIcons.PLAY : SkidIcons.PAUSE;
            homePauseBtn.setImageBitmap(SkidIcons.bitmap(pauseKind, UiFactory.COLOR_TEXT,
                    SkidIcons.dpToPx(this, 20f)));
        }

        if (homeStartPillBtn != null) {
            if (active) {
                homeStartPillBtn.setText("S T O P");
                homeStartPillBtn.setTextColor(UiFactory.COLOR_TEXT);
                homeStartPillBtn.setBackground(UiFactory.pill(this, Color.parseColor("#2A1822"), UiFactory.COLOR_CRIMSON, 1.8f));
                homeStartPillBtn.setCompoundDrawablesWithIntrinsicBounds(SkidIcons.drawable(this, SkidIcons.STOP, UiFactory.COLOR_TEXT, 18f), null, null, null);
            } else if (paused) {
                homeStartPillBtn.setText("R E S U M E");
                homeStartPillBtn.setTextColor(Color.parseColor("#081018"));
                homeStartPillBtn.setBackground(UiFactory.pill(this, UiFactory.COLOR_AMBER, UiFactory.COLOR_TEXT, 1.5f));
                homeStartPillBtn.setCompoundDrawablesWithIntrinsicBounds(SkidIcons.drawable(this, SkidIcons.PLAY, Color.parseColor("#081018"), 18f), null, null, null);
            } else {
                homeStartPillBtn.setText("S T A R T");
                homeStartPillBtn.setTextColor(Color.parseColor("#061018"));
                homeStartPillBtn.setBackground(UiFactory.pill(this, UiFactory.COLOR_SKY, Color.parseColor("#8A4E65"), 1.8f));
                homeStartPillBtn.setCompoundDrawablesWithIntrinsicBounds(SkidIcons.drawable(this, SkidIcons.PLAY, Color.parseColor("#061018"), 18f), null, null, null);
            }
        }

        if (statsChart != null) statsChart.invalidate();
        if (statsDnsTv != null) statsDnsTv.setText(String.valueOf(SkidEventBus.queriesResolved.get()));
        if (statsBlkTv != null) statsBlkTv.setText(String.valueOf(SkidEventBus.threatsBlocked.get()));
        if (statsTcpTv != null) statsTcpTv.setText(String.valueOf(SkidEventBus.tcpStreamsHandled.get()));
        if (statsUdpTv != null) statsUdpTv.setText(String.valueOf(SkidEventBus.udpPacketsHandled.get()));
        if (statsBytesTv != null) {
            statsBytesTv.setText(SkidEventBus.formatBytes(SkidEventBus.bytesIn.get() + SkidEventBus.bytesOut.get()));
        }
        if (statsRttTv != null) statsRttTv.setText(SkidEventBus.lastLatencyMs.get() + " ms");
        if (statsModeTv != null) {
            statsModeTv.setText(config.mode + "  ·  " + config.name);
        }
        if (statsBadgesTv != null) {
            statsBadgesTv.setText("SOCKS :" + config.getPrimaryLocalSocksPort()
                    + (config.socks5UdpAssociate ? " UDP" : "")
                    + "  ·  WG :" + config.wgListenPort + " ka " + config.wgKeepaliveSec + "s"
                    + "  ·  Tor :" + config.orbotSocksPort
                    + "  ·  TLS " + (config.httpsTls13Only ? "1.3-only" : config.httpsMinTls)
                    + "  ·  linger " + config.tcpLingerSec + "s");
        }

        if (statsGridSummaryTv != null) {
            long kbIn = SkidEventBus.bytesIn.get() / 1024L;
            long kbOut = SkidEventBus.bytesOut.get() / 1024L;
            String s = "Engine State        : " + st.name() + "\n" +
                    "Active Profile      : " + config.name + ".skid (" + config.mode + ")\n" +
                    "TUN Interface       : " + SkidEventBus.getActiveTunAddress() + " (MTU " + config.getEffectiveMtu() + ")\n\n" +
                    "--- TRAFFIC & PACKET COUNTERS ---\n" +
                    "DNS Queries OK      : " + SkidEventBus.queriesResolved.get() + "\n" +
                    "DNS Booster Hits    : " + SkidEventBus.dnsCacheHits.get() + " (" + DnsUpstreamResolver.getCacheSize() + " entries)\n" +
                    "Threats Blocked     : " + SkidEventBus.threatsBlocked.get() + "\n" +
                    "TCP Streams Proxied : " + SkidEventBus.tcpStreamsHandled.get() + "\n" +
                    "UDP Packets Handled : " + SkidEventBus.udpPacketsHandled.get() + "\n" +
                    "DPI Desync Splits   : " + SkidEventBus.dpiDesyncApplied.get() + "\n" +
                    "PCAP Captured       : " + PcapRecorder.packetsCaptured.get() + " pkts (" + (PcapRecorder.bytesCaptured.get() / 1024L) + " KB)\n" +
                    "Data Rx / Tx        : " + kbIn + " KB in / " + kbOut + " KB out\n" +
                    "Last Upstream RTT   : " + SkidEventBus.lastLatencyMs.get() + " ms\n\n" +
                    "--- ACTIVE TCP / UDP STACK ---\n" +
                    "IP Version          : " + config.ipVersion + " (NAT64=" + config.performProtocolTranslations + ")\n" +
                    "Socket Buffer Size  : " + config.socketBufferSizeKb + " KB (Jumbo=" + config.bandwidthBooster + ")\n" +
                    "TCP Keepalive / TFO : Shorter=" + config.shorterTcpKeepalive + " | NoDelay=" + config.tcpNoDelay + " | TFO=" + config.tcpFastOpen + "\n" +
                    "TCP MSS / Idle      : " + config.tcpMssClamp + " B | Idle=" + (config.idleTimeoutSec == 0 ? "Disabled" : config.idleTimeoutSec + "s") + "\n" +
                    "UDP EIM / QUIC      : EIM=" + config.endpointIndependentMapping + " | QUIC=" + config.udpQuicPolicy + " | NAT=" + config.udpNatTimeoutSec + "s\n" +
                    "Anti-Censorship DPI : Strategy=" + config.dpiStrategy + " | Technique=" + config.dpiTechnique + " (Split=" + config.tlsRecordSplitBytes + "B)\\n" +
                    "Stack Lab           : Linger=" + config.tcpLingerSec + "s | SOCKS UDP=" + config.socks5UdpAssociate
                    + " | WG :" + config.wgListenPort + " ka=" + config.wgKeepaliveSec
                    + " | TLS13-only=" + config.httpsTls13Only + " | Orbot :" + config.orbotSocksPort;
            statsGridSummaryTv.setText(s);
        }
    }
}
