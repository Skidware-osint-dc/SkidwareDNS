package com.skidwaredns.app.ui;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import com.skidwaredns.app.config.SkidConfig;
import com.skidwaredns.app.proxy.OpenPortEngine;
import com.skidwaredns.app.proxy.PortCatalog;

import java.util.List;

/**
 * Open Port menu: bind 1–10000 on this device's IP, plus a catalog of what
 * each port is for. Java 8, no lambdas.
 */
public final class OpenPortScreens {

    public static final int SUB_OPEN_PORT = 27;

    private static final int PAGE_SIZE = 50;
    private static int pageStart = 1;
    private static int selectedPort = 8080;
    private static String searchQ = "";

    private OpenPortScreens() {}

    public static View build(final Context ctx, final ConfigureScreens.HostCallbacks host) {
        ScrollView sv = new ScrollView(ctx);
        sv.setFillViewport(true);
        LinearLayout col = new LinearLayout(ctx);
        col.setOrientation(LinearLayout.VERTICAL);
        int pad = UiFactory.dp(ctx, 14);
        col.setPadding(pad, pad, pad, UiFactory.dp(ctx, 24));
        final SkidConfig cfg = host.getConfig();
        OpenPortEngine.get().applyConfig(cfg);

        col.addView(UiFactory.backHeader(ctx, "Open Port",
                "Bind a port on this device and read what 1–10000 are for",
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        host.openConfigureSubScreen(ConfigureScreens.SUB_PORTS);
                    }
                }));

        // ---- Device IP ------------------------------------------------
        col.addView(UiFactory.categoryTitle(ctx, "This device"));
        LinearLayout ipCard = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        ipCard.addView(UiFactory.label(ctx, "Will bind: " + OpenPortEngine.describeBind(cfg),
                14f, UiFactory.COLOR_SKY, true, false));
        List<String> ips = OpenPortEngine.deviceIpv4s();
        if (ips.isEmpty()) {
            ipCard.addView(UiFactory.label(ctx,
                    "No LAN IPv4 yet (Wi-Fi off?). Binding 0.0.0.0 still works; the catalog is available offline.",
                    12.5f, UiFactory.COLOR_MUTED, false, false));
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < ips.size(); i++) {
                if (i > 0) sb.append('\n');
                sb.append(ips.get(i));
            }
            ipCard.addView(UiFactory.label(ctx, sb.toString(), 12.5f, UiFactory.COLOR_TEXT, false, true));
        }
        ipCard.addView(UiFactory.divider(ctx));
        ipCard.addView(UiFactory.iconNavRow(ctx, "#", "Bind address",
                cfg.openPortBindMode, UiFactory.COLOR_MUTED,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showBindMode(ctx, cfg, host);
                    }
                }));
        ipCard.addView(UiFactory.divider(ctx));
        ipCard.addView(UiFactory.iconSwitchRow(ctx, "HTTPS", "Reply banner",
                "TCP clients get a one-line description of the port; UDP gets a short SKID reply.",
                cfg.openPortBanner, new UiFactory.ToggleListener() {
                    @Override
                    public void onToggled(boolean checked) {
                        cfg.openPortBanner = checked;
                        OpenPortEngine.get().applyConfig(cfg);
                        host.persistAndReload("Open-Port-Banner=" + checked);
                    }
                }));
        ipCard.addView(UiFactory.divider(ctx));
        ipCard.addView(UiFactory.iconSwitchRow(ctx, "\u23F1", "Hold TCP briefly",
                "Keep the accepted socket ~30s instead of closing after the banner. Still not a proxy.",
                cfg.openPortHold, new UiFactory.ToggleListener() {
                    @Override
                    public void onToggled(boolean checked) {
                        cfg.openPortHold = checked;
                        OpenPortEngine.get().applyConfig(cfg);
                        host.persistAndReload("Open-Port-Hold=" + checked);
                    }
                }));
        col.addView(ipCard);

        // ---- Currently open -------------------------------------------
        col.addView(UiFactory.categoryTitle(ctx, "Open now"));
        LinearLayout live = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        List<OpenPortEngine.Session> sess = OpenPortEngine.get().list();
        if (sess.isEmpty()) {
            live.addView(UiFactory.label(ctx, "Nothing listening. Pick a port below and tap Open.",
                    12.5f, UiFactory.COLOR_MUTED, false, false));
        } else {
            for (int i = 0; i < sess.size(); i++) {
                final OpenPortEngine.Session s = sess.get(i);
                if (i > 0) live.addView(UiFactory.divider(ctx));
                live.addView(UiFactory.iconNavRow(ctx, "#", s.summary(),
                        "tap to select · long-press not needed", UiFactory.COLOR_EMERALD,
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                selectedPort = s.port;
                                host.openConfigureSubScreen(SUB_OPEN_PORT);
                            }
                        }));
            }
            Button closeAll = UiFactory.actionButton(ctx, "Close all",
                    UiFactory.COLOR_SURFACE, UiFactory.COLOR_TEXT, UiFactory.COLOR_BORDER);
            closeAll.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    OpenPortEngine.get().closeAll(cfg);
                    host.persistAndReload("Closed all open ports");
                    host.openConfigureSubScreen(SUB_OPEN_PORT);
                }
            });
            live.addView(closeAll);
        }
        col.addView(live);

        // ---- Selected port --------------------------------------------
        final PortCatalog.Info info = PortCatalog.lookup(selectedPort);
        col.addView(UiFactory.categoryTitle(ctx, "Selected  ·  " + info.rangeLabel()));
        LinearLayout det = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        det.addView(UiFactory.label(ctx, info.titleLine(), 16f, UiFactory.COLOR_TEXT, true, false));
        det.addView(UiFactory.label(ctx, info.about, 13f, UiFactory.COLOR_MUTED, false, false));

        LinearLayout goRow = new LinearLayout(ctx);
        goRow.setOrientation(LinearLayout.HORIZONTAL);
        final EditText portEt = UiFactory.monoInput(ctx, String.valueOf(selectedPort), "1–10000", false);
        LinearLayout.LayoutParams etLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        portEt.setLayoutParams(etLp);
        Button jump = UiFactory.actionButton(ctx, "Go",
                UiFactory.COLOR_CARD_SELECTED, UiFactory.COLOR_SKY, UiFactory.COLOR_SKY);
        jump.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    int p = Integer.parseInt(portEt.getText().toString().trim());
                    if (p < 1 || p > 10000) throw new NumberFormatException();
                    selectedPort = p;
                    pageStart = ((p - 1) / PAGE_SIZE) * PAGE_SIZE + 1;
                    searchQ = "";
                    host.openConfigureSubScreen(SUB_OPEN_PORT);
                } catch (Exception e) {
                    Toast.makeText(ctx, "Enter a port 1–10000", Toast.LENGTH_SHORT).show();
                }
            }
        });
        goRow.addView(portEt);
        View sp = new View(ctx);
        sp.setLayoutParams(new LinearLayout.LayoutParams(UiFactory.dp(ctx, 8), 1));
        goRow.addView(sp);
        goRow.addView(jump);
        det.addView(goRow);

        LinearLayout btnRow = new LinearLayout(ctx);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        Button tcpBtn = UiFactory.actionButton(ctx, "Open TCP",
                UiFactory.COLOR_CARD_SELECTED, UiFactory.COLOR_SKY, UiFactory.COLOR_SKY);
        tcpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doOpen(ctx, host, cfg, selectedPort, true, false);
            }
        });
        Button udpBtn = UiFactory.actionButton(ctx, "Open UDP",
                UiFactory.COLOR_SURFACE, UiFactory.COLOR_EMERALD, UiFactory.COLOR_BORDER);
        udpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doOpen(ctx, host, cfg, selectedPort, false, true);
            }
        });
        btnRow.addView(wrapFlex(tcpBtn));
        View sp2 = new View(ctx);
        sp2.setLayoutParams(new LinearLayout.LayoutParams(UiFactory.dp(ctx, 8), 1));
        btnRow.addView(sp2);
        btnRow.addView(wrapFlex(udpBtn));
        det.addView(btnRow);

        LinearLayout btnRow2 = new LinearLayout(ctx);
        btnRow2.setOrientation(LinearLayout.HORIZONTAL);
        Button bothBtn = UiFactory.actionButton(ctx, "Open TCP+UDP",
                UiFactory.COLOR_SURFACE, UiFactory.COLOR_SKY, UiFactory.COLOR_BORDER);
        bothBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doOpen(ctx, host, cfg, selectedPort, true, true);
            }
        });
        Button closeBtn = UiFactory.actionButton(ctx, "Close",
                UiFactory.COLOR_SURFACE, UiFactory.COLOR_TEXT, UiFactory.COLOR_BORDER);
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = OpenPortEngine.get().close(selectedPort, cfg);
                host.persistAndReload(msg);
                Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
                host.openConfigureSubScreen(SUB_OPEN_PORT);
            }
        });
        btnRow2.addView(wrapFlex(bothBtn));
        View sp3 = new View(ctx);
        sp3.setLayoutParams(new LinearLayout.LayoutParams(UiFactory.dp(ctx, 8), 1));
        btnRow2.addView(sp3);
        btnRow2.addView(wrapFlex(closeBtn));
        det.addView(btnRow2);
        col.addView(det);

        // ---- Catalog search + page ------------------------------------
        col.addView(UiFactory.categoryTitle(ctx, "Catalog  1–10000  ·  " + PortCatalog.namedCount() + " named IANA services"));
        LinearLayout cat = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        final EditText searchEt = UiFactory.monoInput(ctx, searchQ, "Search name, number, or what it does", false);
        searchEt.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override
            public void afterTextChanged(Editable s) {
                searchQ = s == null ? "" : s.toString();
            }
        });
        cat.addView(searchEt);
        Button searchBtn = UiFactory.actionButton(ctx, "Search catalog",
                UiFactory.COLOR_CARD_SELECTED, UiFactory.COLOR_SKY, UiFactory.COLOR_SKY);
        searchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchQ = searchEt.getText().toString();
                host.openConfigureSubScreen(SUB_OPEN_PORT);
            }
        });
        cat.addView(searchBtn);

        LinearLayout rangeRow = new LinearLayout(ctx);
        rangeRow.setOrientation(LinearLayout.HORIZONTAL);
        addRangeChip(ctx, host, rangeRow, "1–1023", 1);
        addRangeChip(ctx, host, rangeRow, "1024+", 1024);
        addRangeChip(ctx, host, rangeRow, "8000s", 8000);
        addRangeChip(ctx, host, rangeRow, "9000s", 9000);
        cat.addView(rangeRow);

        boolean searching = searchQ != null && searchQ.trim().length() > 0;
        List<PortCatalog.Info> rows;
        if (searching) {
            rows = PortCatalog.search(searchQ, 80);
            cat.addView(UiFactory.label(ctx, rows.size() + " match(es) for \"" + searchQ.trim() + "\"",
                    12f, UiFactory.COLOR_MUTED, false, false));
        } else {
            if (pageStart < 1) pageStart = 1;
            if (pageStart > 10000) pageStart = 9951;
            rows = PortCatalog.page(pageStart, PAGE_SIZE);
            int end = Math.min(10000, pageStart + PAGE_SIZE - 1);
            cat.addView(UiFactory.label(ctx, "Showing " + pageStart + "–" + end + " of 10000",
                    12f, UiFactory.COLOR_MUTED, false, false));
        }

        for (int i = 0; i < rows.size(); i++) {
            final PortCatalog.Info row = rows.get(i);
            if (i > 0) cat.addView(UiFactory.divider(ctx));
            String sub = row.assigned ? row.about : row.about;
            if (sub.length() > 110) sub = sub.substring(0, 107) + "…";
            cat.addView(UiFactory.iconNavRow(ctx, row.assigned ? "#" : "◈",
                    row.port + "  " + row.name,
                    sub, row.port == selectedPort ? UiFactory.COLOR_SKY : UiFactory.COLOR_MUTED,
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            selectedPort = row.port;
                            host.openConfigureSubScreen(SUB_OPEN_PORT);
                        }
                    }));
        }

        if (!searching) {
            LinearLayout nav = new LinearLayout(ctx);
            nav.setOrientation(LinearLayout.HORIZONTAL);
            Button prev = UiFactory.actionButton(ctx, "Prev 50",
                    UiFactory.COLOR_SURFACE, UiFactory.COLOR_TEXT, UiFactory.COLOR_BORDER);
            prev.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    pageStart = Math.max(1, pageStart - PAGE_SIZE);
                    host.openConfigureSubScreen(SUB_OPEN_PORT);
                }
            });
            Button next = UiFactory.actionButton(ctx, "Next 50",
                    UiFactory.COLOR_SURFACE, UiFactory.COLOR_TEXT, UiFactory.COLOR_BORDER);
            next.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    pageStart = Math.min(9951, pageStart + PAGE_SIZE);
                    host.openConfigureSubScreen(SUB_OPEN_PORT);
                }
            });
            nav.addView(wrapFlex(prev));
            View spn = new View(ctx);
            spn.setLayoutParams(new LinearLayout.LayoutParams(UiFactory.dp(ctx, 8), 1));
            nav.addView(spn);
            nav.addView(wrapFlex(next));
            cat.addView(nav);
        }
        col.addView(cat);

        sv.addView(col);
        return sv;
    }

    private static void doOpen(Context ctx, ConfigureScreens.HostCallbacks host, SkidConfig cfg,
                               int port, boolean tcp, boolean udp) {
        String msg = OpenPortEngine.get().open(cfg, port, tcp, udp);
        host.persistAndReload(msg);
        Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show();
        host.openConfigureSubScreen(SUB_OPEN_PORT);
    }

    private static void showBindMode(final Context ctx, final SkidConfig cfg,
                                     final ConfigureScreens.HostCallbacks host) {
        final String[] opts = new String[]{"Device-IP", "0.0.0.0", "Loopback"};
        android.app.AlertDialog.Builder b = new android.app.AlertDialog.Builder(ctx);
        b.setTitle("Bind address");
        int sel = 0;
        for (int i = 0; i < opts.length; i++) {
            if (opts[i].equalsIgnoreCase(cfg.openPortBindMode)) sel = i;
        }
        b.setSingleChoiceItems(opts, sel, new android.content.DialogInterface.OnClickListener() {
            @Override
            public void onClick(android.content.DialogInterface dialog, int which) {
                cfg.openPortBindMode = opts[which];
                host.persistAndReload("Open-Port-Bind-Mode=" + opts[which]);
                dialog.dismiss();
                host.openConfigureSubScreen(SUB_OPEN_PORT);
            }
        });
        b.setNegativeButton("Cancel", null);
        b.show();
    }

    private static void addRangeChip(final Context ctx, final ConfigureScreens.HostCallbacks host,
                                     LinearLayout row, String label, final int start) {
        Button b = UiFactory.actionButton(ctx, label,
                UiFactory.COLOR_SURFACE, UiFactory.COLOR_SKY, UiFactory.COLOR_BORDER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.rightMargin = UiFactory.dp(ctx, 4);
        b.setLayoutParams(lp);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pageStart = start;
                searchQ = "";
                selectedPort = start;
                host.openConfigureSubScreen(SUB_OPEN_PORT);
            }
        });
        row.addView(b);
    }

    private static View wrapFlex(View v) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        v.setLayoutParams(lp);
        return v;
    }
}
