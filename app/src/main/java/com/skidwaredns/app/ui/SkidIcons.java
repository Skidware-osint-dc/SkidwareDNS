package com.skidwaredns.app.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.widget.ImageView;

/**
 * Programmatic 24×24 vector icons for SkidwareDNS v5.3.
 * Drawn in the caller's color so ImageView color-filters and compound-drawables work.
 * No XML, no lambdas, no third-party icon fonts.
 */
public final class SkidIcons {

    public static final int HOME = 1;
    public static final int STATS = 2;
    public static final int SKID = 3;
    public static final int GEAR = 4;
    public static final int LOGS = 5;
    public static final int PLAY = 6;
    public static final int PAUSE = 7;
    public static final int STOP = 8;
    public static final int CHEVRON_DOWN = 9;
    public static final int CHEVRON_LEFT = 10;
    public static final int CHEVRON_RIGHT = 11;
    public static final int PLUG = 12;
    public static final int MIXNET = 13;
    public static final int INJECT = 14;
    public static final int LOCK = 15;
    public static final int TOR = 16;
    public static final int I2P = 17;
    public static final int DNS = 18;
    public static final int SHIELD = 19;
    public static final int PROXY = 20;
    public static final int KEY = 21;
    public static final int TUNNEL = 22;
    public static final int ROCKET = 23;
    public static final int FLASH = 24;
    public static final int WIFI = 25;
    public static final int COPY = 26;
    public static final int SHARE = 27;
    public static final int IMPORT = 28;
    public static final int EXPORT = 29;
    public static final int CHECK = 30;
    public static final int TRASH = 31;
    public static final int PLUS = 32;
    public static final int REFRESH = 33;
    public static final int RADAR = 34;
    public static final int TARGET = 35;
    public static final int LOGIN = 36;
    public static final int BRIDGE = 37;
    public static final int SEARCH = 38;
    public static final int GLOBE = 39;
    public static final int CLOCK = 40;
    public static final int DOWNLOAD = 41;
    public static final int UMBRELLA = 42;
    public static final int HOUSE = 43;
    public static final int DICE = 44;
    public static final int WAVE = 45;
    public static final int FILTER = 46;
    public static final int SERVER = 47;
    public static final int APPS = 48;
    public static final int SLIDERS = 49;
    public static final int LINK = 50;
    public static final int SWAP = 51;
    public static final int DOT = 52;
    public static final int STAR = 53;
    public static final int BOLT = 54;
    public static final int CUBE = 55;
    public static final int WARNING = 56;
    public static final int EYE = 57;
    public static final int NETWORK = 58;
    public static final int HOURGLASS = 59;
    public static final int SCISSORS = 60;
    public static final int USER = 61;
    public static final int BELL = 62;
    public static final int MOON = 63;
    public static final int BATTERY = 64;
    public static final int CLOUD = 65;
    public static final int HASH = 66;
    public static final int PENCIL = 67;
    public static final int LAYERS = 68;
    public static final int ROUTE = 69;
    public static final int SHIELD_LOCK = 70;

    private SkidIcons() {}

    public static Bitmap bitmap(int kind, int color, int sizePx) {
        int sz = Math.max(12, sizePx);
        Bitmap bm = Bitmap.createBitmap(sz, sz, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bm);
        float s = sz / 24f;
        c.scale(s, s);
        Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        stroke.setColor(color);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setStrokeJoin(Paint.Join.ROUND);
        stroke.setStrokeWidth(1.7f);
        Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        fill.setColor(color);
        fill.setStyle(Paint.Style.FILL);
        draw(c, stroke, fill, kind);
        return bm;
    }

    public static Drawable drawable(Context ctx, int kind, int color, float dpSize) {
        int px = dpToPx(ctx, dpSize);
        BitmapDrawable d = new BitmapDrawable(ctx.getResources(), bitmap(kind, color, px));
        d.setBounds(0, 0, px, px);
        return d;
    }

    public static ImageView view(Context ctx, int kind, int color, float dpSize) {
        ImageView iv = new ImageView(ctx);
        int px = dpToPx(ctx, dpSize);
        iv.setImageBitmap(bitmap(kind, color, px));
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        iv.setAdjustViewBounds(true);
        return iv;
    }

    public static int dpToPx(Context ctx, float dp) {
        return Math.max(1, Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, ctx.getResources().getDisplayMetrics())));
    }

    public static int iconForGlyph(String glyph) {
        if (glyph == null || glyph.isEmpty()) return DOT;
        if (glyph.indexOf('\uD83D') >= 0 || glyph.indexOf('\uD83C') >= 0 || glyph.indexOf('\uD83E') >= 0) {
            // Emoji: plug, lock, turtle, syringe, onion, purple circle, globe, shield, rocket, key, house...
            if (glyph.contains("\uD83D\uDD0C")) return PLUG;          // 🔌
            if (glyph.contains("\uD83D\uDD10") || glyph.contains("\uD83D\uDD11") || glyph.contains("\uD83D\uDD12")) return LOCK;
            if (glyph.contains("\uD83D\uDC22")) return MIXNET;        // 🐢
            if (glyph.contains("\uD83D\uDC89")) return INJECT;        // 💉
            if (glyph.contains("\uD83E\uDDC5")) return TOR;           // 🧅
            if (glyph.contains("\uD83D\uDFE3")) return I2P;           // 🟣
            if (glyph.contains("\uD83C\uDF10")) return GLOBE;         // 🌐
            if (glyph.contains("\uD83D\uDEE1")) return SHIELD;        // 🛡
            if (glyph.contains("\uD83D\uDE80")) return ROCKET;        // 🚀
            if (glyph.contains("\uD83D\uDD0D")) return SEARCH;        // 🔍
            if (glyph.contains("\uD83D\uDEE1")) return SHIELD;
            if (glyph.contains("\uD83D\uDD27") || glyph.contains("\u2699")) return GEAR;
            if (glyph.contains("\uD83D\uDCC8") || glyph.contains("\uD83D\uDCC9")) return STATS;
            if (glyph.contains("\uD83D\uDCCA")) return STATS;
            if (glyph.contains("\uD83D\uDCE1")) return WIFI;          // 📡
            if (glyph.contains("\uD83D\uDD14")) return BELL;
            if (glyph.contains("\uD83C\uDF19")) return MOON;
            if (glyph.contains("\uD83D\uDD0B")) return BATTERY;
            if (glyph.contains("\uD83D\uDC64") || glyph.contains("\uD83D\uDC81")) return USER;
            if (glyph.contains("\uD83D\uDD11")) return KEY;
            if (glyph.contains("\uD83D\uDEE0")) return GEAR;
            if (glyph.contains("\uD83D\uDEE2")) return WARNING;
            if (glyph.contains("\uD83D\uDDA5")) return APPS;
            if (glyph.contains("\uD83D\uDCC1") || glyph.contains("\uD83D\uDCC2")) return LOGS;
            if (glyph.contains("\uD83D\uDD04")) return REFRESH;
            if (glyph.contains("\uD83D\uDD17")) return LINK;
            if (glyph.contains("\uD83D\uDEEB") || glyph.contains("\u2708")) return ROCKET;
            if (glyph.contains("\uD83D\uDCA1")) return FLASH;
            if (glyph.contains("\uD83D\uDCA7")) return WAVE;
            if (glyph.contains("\uD83C\uDFC6")) return STAR;
            if (glyph.contains("\uD83D\uDD12")) return LOCK;
            if (glyph.contains("\uD83D\uDD13")) return LOCK;
            if (glyph.contains("\uD83D\uDCE6")) return CUBE;
            if (glyph.contains("\uD83D\uDCE4")) return EXPORT;
            if (glyph.contains("\uD83D\uDCE5")) return IMPORT;
            if (glyph.contains("\uD83D\uDDD1")) return TRASH;
            if (glyph.contains("\uD83D\uDCCB")) return LOGS;
            if (glyph.contains("\uD83D\uDCDD")) return PENCIL;
            if (glyph.contains("\uD83D\uDEE1")) return SHIELD;
            if (glyph.contains("\uD83D\uDCE1")) return WIFI;
            if (glyph.contains("\uD83D\uDCF6")) return WIFI;
            if (glyph.contains("\uD83C\uDFE0")) return HOUSE;
            if (glyph.contains("\uD83D\uDD25")) return FLASH;
            if (glyph.contains("\u26A1")) return FLASH;
            return DOT;
        }
        // ASCII / dingbat glyphs used across the UI
        if ("⌂".equals(glyph) || "🏠".equals(glyph)) return HOME;
        if ("📈".equals(glyph) || "📊".equals(glyph)) return STATS;
        if ("✦".equals(glyph) || "✧".equals(glyph) || "★".equals(glyph)) return SKID;
        if ("⚙".equals(glyph) || "⚙️".equals(glyph)) return GEAR;
        if ("☰".equals(glyph) || "≡".equals(glyph)) return LOGS;
        if ("‹".equals(glyph) || "◀".equals(glyph) || "<".equals(glyph)) return CHEVRON_LEFT;
        if ("›".equals(glyph) || "▶".equals(glyph) || ">".equals(glyph)) return CHEVRON_RIGHT;
        if ("⌄".equals(glyph) || "⌃".equals(glyph) || "v".equals(glyph)) return CHEVRON_DOWN;
        if ("◈".equals(glyph) || "◆".equals(glyph) || "●".equals(glyph)) return DOT;
        if ("🔒".equals(glyph) || "🔐".equals(glyph)) return LOCK;
        if ("🔑".equals(glyph) || "🗝".equals(glyph)) return KEY;
        if ("⇄".equals(glyph) || "↔".equals(glyph) || "|↔|".equals(glyph) || "↪".equals(glyph)) return SWAP;
        if ("☁".equals(glyph) || "☁️".equals(glyph)) return CLOUD;
        if ("#".equals(glyph)) return HASH;
        if ("👤".equals(glyph)) return USER;
        if ("✎".equals(glyph) || "✏️".equals(glyph)) return PENCIL;
        if ("⚡".equals(glyph)) return FLASH;
        if ("🌐".equals(glyph)) return GLOBE;
        if ("⚠".equals(glyph) || "⚠️".equals(glyph)) return WARNING;
        if ("⏱".equals(glyph) || "🕒".equals(glyph) || "⏳".equals(glyph)) return glyph.indexOf('⏳') >= 0 ? HOURGLASS : CLOCK;
        if ("↑".equals(glyph) || "↓".equals(glyph) || "⬇".equals(glyph)) return DOWNLOAD;
        if ("➕".equals(glyph) || "+".equals(glyph) || "⊞".equals(glyph) || "＋".equals(glyph)) return PLUS;
        if ("➖".equals(glyph)) return FILTER;
        if ("☑".equals(glyph) || "✓".equals(glyph) || "✔".equals(glyph)) return CHECK;
        if ("🔗".equals(glyph) || "☍".equals(glyph)) return LINK;
        if ("🎲".equals(glyph)) return DICE;
        if ("🌊".equals(glyph)) return WAVE;
        if ("☂".equals(glyph) || "☂️".equals(glyph)) return UMBRELLA;
        if ("🚀".equals(glyph)) return ROCKET;
        if ("🌙".equals(glyph)) return MOON;
        if ("🔔".equals(glyph)) return BELL;
        if ("📳".equals(glyph)) return BELL;
        if ("🔋".equals(glyph)) return BATTERY;
        if ("📡".equals(glyph) || "📶".equals(glyph) || "((•))".equals(glyph)) return WIFI;
        if ("🎭".equals(glyph)) return EYE;
        if ("🕵".equals(glyph) || "🕵️".equals(glyph)) return EYE;
        if ("⧉".equals(glyph) || "❖".equals(glyph) || "▤".equals(glyph)) return LAYERS;
        if ("✈".equals(glyph) || "✈️".equals(glyph)) return ROCKET;
        if ("📜".equals(glyph)) return LOGS;
        if ("⊘".equals(glyph)) return STOP;
        if ("ⓘ".equals(glyph) || "ℹ".equals(glyph)) return DOT;
        if ("◎".equals(glyph) || "◉".equals(glyph)) return TARGET;
        if ("⑂".equals(glyph)) return SWAP;
        if ("✂".equals(glyph)) return SCISSORS;
        if ("🆕".equals(glyph)) return STAR;
        if ("🏠".equals(glyph)) return HOUSE;
        if ("🌐".equals(glyph)) return GLOBE;
        if ("🛡".equals(glyph)) return SHIELD;
        if ("⚡".equals(glyph)) return FLASH;
        if ("⏳".equals(glyph)) return HOURGLASS;
        if ("((•))".equals(glyph)) return WIFI;
        if ("|↔|".equals(glyph)) return SWAP;
        if ("25A6".equals(glyph) || "▤".equals(glyph) || "▦".equals(glyph)) return LAYERS;
        if ("\u25A6".equals(glyph)) return LAYERS;
        if ("\u2B83".equals(glyph)) return SWAP;
        if ("\u26A1".equals(glyph)) return FLASH;
        if ("\u2699".equals(glyph)) return GEAR;
        if ("\u21C4".equals(glyph)) return SWAP;
        if ("\u260D".equals(glyph)) return LINK;
        if ("\u25AB".equals(glyph)) return CUBE;
        if ("\u229E".equals(glyph)) return APPS;
        if ("\u2726".equals(glyph)) return STAR;
        if ("\u223F".equals(glyph)) return WAVE;
        if ("\u26A1".equals(glyph)) return FLASH;
        return DOT;
    }

    public static int iconForButtonLabel(String text) {
        if (text == null) return CHECK;
        String t = text.toLowerCase();
        if (t.contains("import")) return IMPORT;
        if (t.contains("export")) return EXPORT;
        if (t.contains("share")) return SHARE;
        if (t.contains("copy")) return COPY;
        if (t.contains("validate") || t.contains("parse") || t.contains("apply") || t.contains("save")) return CHECK;
        if (t.contains("clear") || t.contains("reset") && t.contains("counter")) return TRASH;
        if (t.contains("flush") || t.contains("sync") || t.contains("reset")) return REFRESH;
        if (t.contains("probe") || t.contains("resolve") || t.contains("test") || t.contains("ping")) return RADAR;
        if (t.contains("pcap")) return WAVE;
        if (t.contains("wireguard") || t.contains(".conf")) return KEY;
        if (t.contains("exit")) return TARGET;
        if (t.contains("entry")) return LOGIN;
        if (t.contains("ssh")) return BRIDGE;
        if (t.contains("v2ray") || t.contains("xray") || t.contains("hysteria") || t.contains("json")) return TUNNEL;
        if (t.contains("help")) return SEARCH;
        if (t.contains("add") || t.contains("+ ")) return PLUS;
        if (t.contains("preview") || t.contains("config")) return LOGS;
        if (t.contains("psk") || t.contains("key")) return KEY;
        if (t.contains("doh")) return DNS;
        if (t.contains("start")) return PLAY;
        if (t.contains("stop")) return STOP;
        if (t.contains("pause")) return PAUSE;
        return CHECK;
    }

    // =====================================================================
    private static void draw(Canvas c, Paint s, Paint f, int kind) {
        switch (kind) {
            case HOME:
                Path house = new Path();
                house.moveTo(4, 11); house.lineTo(12, 4); house.lineTo(20, 11);
                c.drawPath(house, s);
                c.drawRect(7, 11, 17, 20, s);
                c.drawRect(10.5f, 14, 13.5f, 20, s);
                break;
            case STATS:
                c.drawLine(5, 19, 5, 11, s);
                c.drawLine(10, 19, 10, 7, s);
                c.drawLine(15, 19, 15, 13, s);
                c.drawLine(20, 19, 20, 5, s);
                break;
            case SKID:
                drawHexU(c, s, f);
                break;
            case GEAR:
                c.drawCircle(12, 12, 3.2f, s);
                for (int i = 0; i < 8; i++) {
                    double a = Math.toRadians(i * 45);
                    float x1 = 12 + (float) Math.cos(a) * 5.2f;
                    float y1 = 12 + (float) Math.sin(a) * 5.2f;
                    float x2 = 12 + (float) Math.cos(a) * 8.4f;
                    float y2 = 12 + (float) Math.sin(a) * 8.4f;
                    c.drawLine(x1, y1, x2, y2, s);
                }
                break;
            case LOGS:
                c.drawLine(5, 7, 19, 7, s);
                c.drawLine(5, 12, 19, 12, s);
                c.drawLine(5, 17, 15, 17, s);
                break;
            case PLAY: {
                Path p = new Path();
                p.moveTo(8, 6); p.lineTo(18, 12); p.lineTo(8, 18); p.close();
                c.drawPath(p, f);
                break;
            }
            case PAUSE:
                c.drawRect(7, 6, 10.5f, 18, f);
                c.drawRect(13.5f, 6, 17, 18, f);
                break;
            case STOP:
                c.drawRoundRect(new RectF(7, 7, 17, 17), 2, 2, f);
                break;
            case CHEVRON_DOWN:
                c.drawLine(6, 9, 12, 15, s);
                c.drawLine(12, 15, 18, 9, s);
                break;
            case CHEVRON_LEFT:
                c.drawLine(14, 6, 8, 12, s);
                c.drawLine(8, 12, 14, 18, s);
                break;
            case CHEVRON_RIGHT:
                c.drawLine(10, 6, 16, 12, s);
                c.drawLine(16, 12, 10, 18, s);
                break;
            case PLUG:
                c.drawRoundRect(new RectF(8, 10, 16, 18), 2, 2, s);
                c.drawLine(10.5f, 7, 10.5f, 10, s);
                c.drawLine(13.5f, 7, 13.5f, 10, s);
                c.drawLine(12, 18, 12, 20.5f, s);
                break;
            case MIXNET:
                c.drawCircle(12, 8, 2.4f, s);
                c.drawCircle(7, 16, 2.4f, s);
                c.drawCircle(17, 16, 2.4f, s);
                c.drawLine(12, 10.4f, 8.4f, 14.2f, s);
                c.drawLine(12, 10.4f, 15.6f, 14.2f, s);
                c.drawLine(9.4f, 16, 14.6f, 16, s);
                break;
            case INJECT:
                c.drawLine(7, 17, 15, 9, s);
                c.drawLine(14, 8, 17, 11, s);
                c.drawLine(16.5f, 7.5f, 18.5f, 5.5f, s);
                c.drawLine(8, 16, 6, 18, s);
                c.drawLine(11, 11, 13.5f, 13.5f, s);
                break;
            case LOCK:
                c.drawRoundRect(new RectF(7, 11, 17, 20), 2, 2, s);
                c.drawArc(new RectF(8.5f, 5.5f, 15.5f, 13), -180, 180, false, s);
                break;
            case TOR:
                c.drawCircle(12, 13, 7, s);
                c.drawCircle(12, 13, 4.2f, s);
                c.drawCircle(12, 13, 1.6f, f);
                c.drawLine(12, 6, 12, 4.5f, s);
                break;
            case I2P:
                c.drawCircle(12, 12, 7.5f, s);
                c.drawCircle(12, 12, 3.2f, f);
                break;
            case DNS:
            case GLOBE:
                c.drawCircle(12, 12, 8, s);
                c.drawOval(new RectF(8, 4, 16, 20), s);
                c.drawLine(4, 12, 20, 12, s);
                c.drawLine(5.5f, 8, 18.5f, 8, s);
                c.drawLine(5.5f, 16, 18.5f, 16, s);
                break;
            case SHIELD:
            case SHIELD_LOCK: {
                Path sh = new Path();
                sh.moveTo(12, 3.5f);
                sh.lineTo(19, 6.5f);
                sh.lineTo(19, 13);
                sh.quadTo(19, 18.5f, 12, 21);
                sh.quadTo(5, 18.5f, 5, 13);
                sh.lineTo(5, 6.5f);
                sh.close();
                c.drawPath(sh, s);
                if (kind == SHIELD_LOCK) {
                    c.drawCircle(12, 12, 2.2f, s);
                }
                break;
            }
            case PROXY:
                c.drawCircle(6, 12, 2.2f, s);
                c.drawCircle(18, 7, 2.2f, s);
                c.drawCircle(18, 17, 2.2f, s);
                c.drawLine(8.2f, 12, 15.8f, 8, s);
                c.drawLine(8.2f, 12, 15.8f, 16, s);
                break;
            case KEY:
                c.drawCircle(8, 12, 3.2f, s);
                c.drawLine(11.2f, 12, 19, 12, s);
                c.drawLine(16, 12, 16, 15.5f, s);
                c.drawLine(18.2f, 12, 18.2f, 14.5f, s);
                break;
            case TUNNEL:
                c.drawArc(new RectF(4, 6, 20, 22), 180, 180, false, s);
                c.drawLine(4, 14, 4, 18, s);
                c.drawLine(20, 14, 20, 18, s);
                c.drawLine(8, 14, 8, 18, s);
                c.drawLine(16, 14, 16, 18, s);
                break;
            case ROCKET: {
                Path rk = new Path();
                rk.moveTo(12, 3.5f); rk.lineTo(16, 12); rk.lineTo(12, 10.5f); rk.lineTo(8, 12); rk.close();
                c.drawPath(rk, s);
                c.drawLine(9, 13, 7, 19, s);
                c.drawLine(15, 13, 17, 19, s);
                c.drawLine(12, 11, 12, 18, s);
                break;
            }
            case FLASH:
            case BOLT: {
                Path b = new Path();
                b.moveTo(13, 3); b.lineTo(8, 13); b.lineTo(12, 13); b.lineTo(11, 21); b.lineTo(16.5f, 10); b.lineTo(12.5f, 10); b.close();
                c.drawPath(b, f);
                break;
            }
            case WIFI:
                c.drawArc(new RectF(5, 8, 19, 22), 200, 140, false, s);
                c.drawArc(new RectF(8, 11, 16, 19), 200, 140, false, s);
                c.drawCircle(12, 18, 1.3f, f);
                break;
            case COPY:
                c.drawRoundRect(new RectF(8, 7, 19, 19), 1.6f, 1.6f, s);
                c.drawRoundRect(new RectF(5, 4.5f, 16, 16.5f), 1.6f, 1.6f, s);
                break;
            case SHARE:
                c.drawCircle(7, 12, 2, s);
                c.drawCircle(17, 6.5f, 2, s);
                c.drawCircle(17, 17.5f, 2, s);
                c.drawLine(8.8f, 11.2f, 15.2f, 7.4f, s);
                c.drawLine(8.8f, 12.8f, 15.2f, 16.6f, s);
                break;
            case IMPORT:
                c.drawLine(12, 4, 12, 14, s);
                c.drawLine(8, 11, 12, 15, s);
                c.drawLine(16, 11, 12, 15, s);
                c.drawLine(5, 18, 19, 18, s);
                break;
            case EXPORT:
                c.drawLine(12, 15, 12, 5, s);
                c.drawLine(8, 8, 12, 4, s);
                c.drawLine(16, 8, 12, 4, s);
                c.drawLine(5, 18, 19, 18, s);
                break;
            case CHECK:
                c.drawLine(5.5f, 12.5f, 10, 17, s);
                c.drawLine(10, 17, 19, 7, s);
                break;
            case TRASH:
                c.drawLine(6, 8, 18, 8, s);
                c.drawRect(7.5f, 8, 16.5f, 19, s);
                c.drawLine(10, 5.5f, 14, 5.5f, s);
                c.drawLine(10.5f, 11, 10.5f, 16, s);
                c.drawLine(13.5f, 11, 13.5f, 16, s);
                break;
            case PLUS:
                c.drawLine(12, 6, 12, 18, s);
                c.drawLine(6, 12, 18, 12, s);
                break;
            case REFRESH:
                c.drawArc(new RectF(5, 5, 19, 19), 40, 260, false, s);
                Path ar = new Path();
                ar.moveTo(17.5f, 4.5f); ar.lineTo(19.5f, 8.5f); ar.lineTo(15.2f, 8.2f);
                c.drawPath(ar, f);
                break;
            case RADAR:
                c.drawCircle(12, 12, 8, s);
                c.drawCircle(12, 12, 4.5f, s);
                c.drawLine(12, 12, 17.5f, 7.5f, s);
                c.drawCircle(12, 12, 1.4f, f);
                break;
            case TARGET:
                c.drawCircle(12, 12, 8, s);
                c.drawCircle(12, 12, 4.5f, s);
                c.drawCircle(12, 12, 1.5f, f);
                break;
            case LOGIN:
                c.drawRoundRect(new RectF(11, 5, 20, 19), 1.8f, 1.8f, s);
                c.drawLine(4, 12, 14, 12, s);
                c.drawLine(11, 9, 14.5f, 12, s);
                c.drawLine(11, 15, 14.5f, 12, s);
                break;
            case BRIDGE:
                c.drawLine(4, 16, 20, 16, s);
                c.drawArc(new RectF(6, 8, 18, 20), 180, 180, false, s);
                c.drawLine(8, 16, 8, 12.5f, s);
                c.drawLine(12, 16, 12, 8, s);
                c.drawLine(16, 16, 16, 12.5f, s);
                break;
            case SEARCH:
                c.drawCircle(10.5f, 10.5f, 5.5f, s);
                c.drawLine(14.6f, 14.6f, 19.5f, 19.5f, s);
                break;
            case CLOCK:
                c.drawCircle(12, 12, 8, s);
                c.drawLine(12, 12, 12, 7.5f, s);
                c.drawLine(12, 12, 16, 14, s);
                break;
            case DOWNLOAD:
                c.drawLine(12, 4, 12, 15, s);
                c.drawLine(8, 12, 12, 16, s);
                c.drawLine(16, 12, 12, 16, s);
                c.drawLine(5, 19, 19, 19, s);
                break;
            case UMBRELLA:
                c.drawArc(new RectF(4, 6, 20, 18), 180, 180, true, s);
                c.drawLine(12, 12, 12, 19, s);
                c.drawArc(new RectF(12, 17.5f, 16, 21.5f), 0, 180, false, s);
                break;
            case HOUSE:
                Path h2 = new Path();
                h2.moveTo(4, 12); h2.lineTo(12, 5); h2.lineTo(20, 12);
                c.drawPath(h2, s);
                c.drawRect(7.5f, 12, 16.5f, 20, s);
                break;
            case DICE:
                c.drawRoundRect(new RectF(5, 5, 19, 19), 3, 3, s);
                c.drawCircle(9, 9, 1.2f, f);
                c.drawCircle(15, 9, 1.2f, f);
                c.drawCircle(12, 12, 1.2f, f);
                c.drawCircle(9, 15, 1.2f, f);
                c.drawCircle(15, 15, 1.2f, f);
                break;
            case WAVE:
                Path wv = new Path();
                wv.moveTo(3, 14);
                wv.cubicTo(6, 8, 9, 20, 12, 14);
                wv.cubicTo(15, 8, 18, 20, 21, 14);
                c.drawPath(wv, s);
                break;
            case FILTER: {
                Path ft = new Path();
                ft.moveTo(5, 6); ft.lineTo(19, 6); ft.lineTo(14, 12); ft.lineTo(14, 18); ft.lineTo(10, 16); ft.lineTo(10, 12); ft.close();
                c.drawPath(ft, s);
                break;
            }
            case SERVER:
                c.drawRoundRect(new RectF(5, 4.5f, 19, 10), 1.6f, 1.6f, s);
                c.drawRoundRect(new RectF(5, 11, 19, 16.5f), 1.6f, 1.6f, s);
                c.drawRoundRect(new RectF(5, 17.5f, 19, 20.5f), 1.6f, 1.6f, s);
                c.drawCircle(8, 7.2f, 0.9f, f);
                c.drawCircle(8, 13.7f, 0.9f, f);
                break;
            case APPS:
                c.drawRoundRect(new RectF(5, 5, 10.5f, 10.5f), 1.4f, 1.4f, s);
                c.drawRoundRect(new RectF(13.5f, 5, 19, 10.5f), 1.4f, 1.4f, s);
                c.drawRoundRect(new RectF(5, 13.5f, 10.5f, 19), 1.4f, 1.4f, s);
                c.drawRoundRect(new RectF(13.5f, 13.5f, 19, 19), 1.4f, 1.4f, s);
                break;
            case SLIDERS:
                c.drawLine(7, 6, 7, 18, s);
                c.drawLine(12, 6, 12, 18, s);
                c.drawLine(17, 6, 17, 18, s);
                c.drawCircle(7, 10, 2, f);
                c.drawCircle(12, 15, 2, f);
                c.drawCircle(17, 9, 2, f);
                break;
            case LINK:
                c.drawArc(new RectF(4, 8, 13, 16), 70, 220, false, s);
                c.drawArc(new RectF(11, 8, 20, 16), -110, 220, false, s);
                break;
            case SWAP:
                c.drawLine(5, 9, 19, 9, s);
                c.drawLine(15, 6, 19, 9, s);
                c.drawLine(15, 12, 19, 9, s);
                c.drawLine(19, 16, 5, 16, s);
                c.drawLine(9, 13, 5, 16, s);
                c.drawLine(9, 19, 5, 16, s);
                break;
            case DOT:
                c.drawCircle(12, 12, 2.4f, f);
                break;
            case STAR: {
                Path st = new Path();
                for (int i = 0; i < 5; i++) {
                    double a = Math.toRadians(-90 + i * 72);
                    float x = 12 + (float) Math.cos(a) * 8;
                    float y = 12 + (float) Math.sin(a) * 8;
                    if (i == 0) st.moveTo(x, y); else st.lineTo(x, y);
                    double b = Math.toRadians(-90 + i * 72 + 36);
                    st.lineTo(12 + (float) Math.cos(b) * 3.4f, 12 + (float) Math.sin(b) * 3.4f);
                }
                st.close();
                c.drawPath(st, s);
                break;
            }
            case CUBE:
                Path cu = new Path();
                cu.moveTo(12, 4); cu.lineTo(20, 8.5f); cu.lineTo(12, 13); cu.lineTo(4, 8.5f); cu.close();
                c.drawPath(cu, s);
                c.drawLine(12, 13, 12, 20.5f, s);
                c.drawLine(20, 8.5f, 20, 16, s);
                c.drawLine(4, 8.5f, 4, 16, s);
                c.drawLine(4, 16, 12, 20.5f, s);
                c.drawLine(20, 16, 12, 20.5f, s);
                break;
            case WARNING: {
                Path tr = new Path();
                tr.moveTo(12, 4); tr.lineTo(21, 19.5f); tr.lineTo(3, 19.5f); tr.close();
                c.drawPath(tr, s);
                c.drawLine(12, 9, 12, 14.5f, s);
                c.drawCircle(12, 17, 0.9f, f);
                break;
            }
            case EYE:
                c.drawOval(new RectF(4, 8, 20, 16), s);
                c.drawCircle(12, 12, 2.6f, s);
                break;
            case NETWORK:
                c.drawCircle(12, 6, 2, s);
                c.drawCircle(6, 18, 2, s);
                c.drawCircle(18, 18, 2, s);
                c.drawLine(12, 8, 7.2f, 16.4f, s);
                c.drawLine(12, 8, 16.8f, 16.4f, s);
                c.drawLine(8, 18, 16, 18, s);
                break;
            case HOURGLASS:
                Path hg = new Path();
                hg.moveTo(7, 5); hg.lineTo(17, 5); hg.lineTo(12, 12); hg.lineTo(17, 19); hg.lineTo(7, 19); hg.lineTo(12, 12); hg.close();
                c.drawPath(hg, s);
                break;
            case SCISSORS:
                c.drawCircle(7, 8, 2.3f, s);
                c.drawCircle(7, 16, 2.3f, s);
                c.drawLine(9, 9, 19, 17, s);
                c.drawLine(9, 15, 19, 7, s);
                break;
            case USER:
                c.drawCircle(12, 8.5f, 3.4f, s);
                c.drawArc(new RectF(5.5f, 13, 18.5f, 22), 0, 180, false, s);
                break;
            case BELL:
                c.drawArc(new RectF(7, 5, 17, 16), 180, 180, false, s);
                c.drawLine(7, 11, 7, 16, s);
                c.drawLine(17, 11, 17, 16, s);
                c.drawLine(6, 16, 18, 16, s);
                c.drawArc(new RectF(10, 16, 14, 19.5f), 0, 180, false, s);
                break;
            case MOON:
                Path moon = new Path();
                moon.addCircle(12, 12, 7.5f, Path.Direction.CW);
                Path cut = new Path();
                cut.addCircle(15.5f, 10, 6.2f, Path.Direction.CW);
                moon.op(cut, Path.Op.DIFFERENCE);
                c.drawPath(moon, f);
                break;
            case BATTERY:
                c.drawRoundRect(new RectF(4, 8, 19, 16), 1.6f, 1.6f, s);
                c.drawRect(19, 10.5f, 21, 13.5f, f);
                c.drawRect(6, 10, 14, 14, f);
                break;
            case CLOUD:
                c.drawArc(new RectF(5, 10, 13, 18), 0, 360, false, s);
                c.drawArc(new RectF(10, 8, 19.5f, 18), 0, 360, false, s);
                c.drawLine(6, 16.5f, 18, 16.5f, s);
                break;
            case HASH:
                c.drawLine(9, 5, 7, 19, s);
                c.drawLine(17, 5, 15, 19, s);
                c.drawLine(4.5f, 9.5f, 20, 9.5f, s);
                c.drawLine(4, 14.5f, 19.5f, 14.5f, s);
                break;
            case PENCIL:
                c.drawLine(6, 18, 15, 9, s);
                c.drawLine(15, 9, 18, 12, s);
                c.drawLine(18, 12, 9, 21, s);
                c.drawLine(6, 18, 9, 21, s);
                c.drawLine(14, 8, 16.5f, 5.5f, s);
                break;
            case LAYERS:
                Path ly = new Path();
                ly.moveTo(12, 4); ly.lineTo(20, 8); ly.lineTo(12, 12); ly.lineTo(4, 8); ly.close();
                c.drawPath(ly, s);
                c.drawLine(4, 12, 12, 16, s);
                c.drawLine(20, 12, 12, 16, s);
                c.drawLine(4, 16, 12, 20, s);
                c.drawLine(20, 16, 12, 20, s);
                break;
            case ROUTE:
                c.drawCircle(7, 7, 2.2f, s);
                c.drawCircle(17, 17, 2.2f, s);
                c.drawLine(8.6f, 8.6f, 15.4f, 15.4f, s);
                break;
            default:
                c.drawCircle(12, 12, 2.4f, f);
                break;
        }
    }

    private static void drawHexU(Canvas c, Paint s, Paint f) {
        Path hex = new Path();
        for (int i = 0; i < 6; i++) {
            double a = Math.toRadians(-90 + i * 60);
            float x = 12 + (float) Math.cos(a) * 9.2f;
            float y = 12 + (float) Math.sin(a) * 9.2f;
            if (i == 0) hex.moveTo(x, y); else hex.lineTo(x, y);
        }
        hex.close();
        Paint fat = new Paint(s);
        fat.setStrokeWidth(1.9f);
        c.drawPath(hex, fat);
        Path u = new Path();
        u.moveTo(8.1f, 8.0f);
        u.lineTo(10.55f, 8.0f);
        u.lineTo(10.55f, 14.15f);
        u.lineTo(13.45f, 14.15f);
        u.lineTo(13.45f, 8.0f);
        u.lineTo(15.9f, 8.0f);
        u.lineTo(15.9f, 15.5f);
        u.quadTo(15.9f, 17.3f, 12.0f, 17.3f);
        u.quadTo(8.1f, 17.3f, 8.1f, 15.5f);
        u.close();
        c.drawPath(u, f);
    }
}
