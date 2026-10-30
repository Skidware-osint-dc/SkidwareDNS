package com.skidwaredns.app.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * First-launch intro. Explains DNS, firewall, modes, tools, privacy stack.
 */
public final class OnboardingScreens {

    public static final String[] TITLES = new String[]{
            "Welcome to SkidwareDNS",
            "DNS + Firewall",
            "Pick a mode",
            "Tunnels & privacy",
            "Tools on this phone",
            "You are in control"
    };
    public static final String[] BODIES = new String[]{
            "A local VPN, encrypted DNS router, and firewall. Traffic stays on this device unless you turn on a tunnel. Nothing is uploaded.",
            "Start captures DNS on this phone. Blocklists and .skid rules decide allow vs block. Home shows a live chart of both — tap a series to inspect.",
            "The chevron next to START picks how the engine binds: DNS-only, full-tunnel VPN, Proxy-Only (no tun), WireGuard, Tor, I2P, mixnet, or lockdown.",
            "Configure has WireGuard, Orbot/Tor, I2P, SOCKS5, SSH, Xray and Hysteria. Stack Lab fine-tunes TCP, UDP, sockets, TLS and SOCKS.",
            "Open Port binds 1-64000 on this LAN IP. Tools hub: FTP share, LAN scan, SSH, port knock, port check, folder mirror.",
            "Everything lives in a .skid profile you can export. Skip anytime — reopen this intro from Settings."
    };
    private static final int[] ICONS = new int[]{
            SkidIcons.SKID, SkidIcons.SHIELD, SkidIcons.SLIDERS,
            SkidIcons.LOCK, SkidIcons.WRENCH, SkidIcons.CHECK
    };

    public interface Done {
        void onFinished();
    }

    private OnboardingScreens() {}

    public static View build(final Context ctx, final Done done) {
        final FrameLayout overlay = new FrameLayout(ctx);
        overlay.setClickable(true);
        overlay.setTag("skid-onboarding");
        overlay.setBackgroundColor(Color.parseColor("#E607080B"));
        overlay.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        final LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = UiFactory.dp(ctx, 22);
        card.setPadding(pad, pad, pad, pad);
        card.setBackground(UiFactory.cardFill(ctx, UiFactory.COLOR_BORDER, 22f));
        FrameLayout.LayoutParams cLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cLp.gravity = Gravity.CENTER;
        int m = UiFactory.dp(ctx, 18);
        cLp.setMargins(m, m, m, m);
        card.setLayoutParams(cLp);

        final int[] page = new int[]{0};
        final ImageView[] iconSlot = new ImageView[1];
        iconSlot[0] = UiFactory.iconWell(ctx, ICONS[0], UiFactory.COLOR_SKY, 72f, 36f);
        LinearLayout.LayoutParams iLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        iLp.bottomMargin = UiFactory.dp(ctx, 16);
        iLp.gravity = Gravity.CENTER_HORIZONTAL;
        iconSlot[0].setLayoutParams(iLp);

        final TextView title = UiFactory.label(ctx, TITLES[0], 22f, UiFactory.COLOR_TEXT, true, false);
        title.setGravity(Gravity.CENTER);
        final TextView body = UiFactory.label(ctx, BODIES[0], 14.5f, UiFactory.COLOR_MUTED, false, false);
        body.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bLp.topMargin = UiFactory.dp(ctx, 10);
        body.setLayoutParams(bLp);

        final LinearLayout dots = new LinearLayout(ctx);
        dots.setOrientation(LinearLayout.HORIZONTAL);
        dots.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams dLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dLp.topMargin = UiFactory.dp(ctx, 16);
        dLp.bottomMargin = UiFactory.dp(ctx, 18);
        dots.setLayoutParams(dLp);

        card.addView(iconSlot[0]);
        card.addView(title);
        card.addView(body);
        card.addView(dots);

        LinearLayout btns = new LinearLayout(ctx);
        btns.setOrientation(LinearLayout.HORIZONTAL);
        final Button skip = UiFactory.actionButton(ctx, "Skip", UiFactory.COLOR_CARD, UiFactory.COLOR_MUTED, UiFactory.COLOR_BORDER);
        final Button next = UiFactory.actionButton(ctx, "Next", UiFactory.COLOR_SKY, UiFactory.COLOR_BG, UiFactory.COLOR_SKY);
        skip.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.8f));
        next.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.2f));
        View g = new View(ctx);
        g.setLayoutParams(new LinearLayout.LayoutParams(UiFactory.dp(ctx, 8), 1));
        btns.addView(skip);
        btns.addView(g);
        btns.addView(next);
        card.addView(btns);

        final Runnable render = new Runnable() {
            @Override public void run() {
                int i = page[0];
                title.setText(TITLES[i]);
                body.setText(BODIES[i]);
                next.setText(i == TITLES.length - 1 ? "Start" : "Next");
                dots.removeAllViews();
                for (int d = 0; d < TITLES.length; d++) {
                    View dot = new View(ctx);
                    int sz = UiFactory.dp(ctx, d == i ? 8 : 6);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(sz, sz);
                    lp.leftMargin = UiFactory.dp(ctx, 3);
                    lp.rightMargin = UiFactory.dp(ctx, 3);
                    dot.setLayoutParams(lp);
                    dot.setBackground(UiFactory.circleRing(ctx,
                            d == i ? UiFactory.COLOR_SKY : UiFactory.COLOR_ICON_BG,
                            Color.TRANSPARENT, 0f));
                    dots.addView(dot);
                }
                ImageView nv = UiFactory.iconWell(ctx, ICONS[i], UiFactory.COLOR_SKY, 72f, 36f);
                nv.setLayoutParams(iconSlot[0].getLayoutParams());
                int idx = card.indexOfChild(iconSlot[0]);
                card.removeViewAt(idx);
                card.addView(nv, idx);
                iconSlot[0] = nv;
                UiFactory.fadeIn(card, 0);
            }
        };
        render.run();

        skip.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (overlay.getParent() instanceof ViewGroup) {
                    ((ViewGroup) overlay.getParent()).removeView(overlay);
                }
                if (done != null) done.onFinished();
            }
        });
        next.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (page[0] < TITLES.length - 1) {
                    page[0]++;
                    render.run();
                } else {
                    skip.performClick();
                }
            }
        });

        overlay.addView(card);
        UiFactory.fadeIn(overlay, 40);
        return overlay;
    }
}
