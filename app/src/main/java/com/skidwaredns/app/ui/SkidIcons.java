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
 * Lucide-style 24-grid line icons. 3x supersampled, 1.9px round stroke,
 * 2.5-unit padding. No Path.op, no XML. Java 8.
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
    public static final int PORT = 71;
    public static final int HTTPS = 72;
    public static final int SOCKET = 73;
    public static final int FTP = 74;
    public static final int TERMINAL = 75;
    public static final int FOLDER = 76;
    public static final int WRENCH = 77;
    public static final int KNOCK = 78;

    private SkidIcons() {}

    public static Bitmap bitmap(int kind, int color, int sizePx) {
        int sz = Math.max(24, sizePx);
        int hi = sz * 3;
        Bitmap raw = Bitmap.createBitmap(hi, hi, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(raw);
        float s = hi / 24f;
        c.scale(s, s);
        Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        stroke.setColor(color);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setStrokeJoin(Paint.Join.ROUND);
        stroke.setStrokeWidth(2.25f);
        Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        fill.setColor(color);
        fill.setStyle(Paint.Style.FILL);
        fill.setStrokeCap(Paint.Cap.ROUND);
        fill.setStrokeJoin(Paint.Join.ROUND);
        draw(c, stroke, fill, kind);
        Bitmap out = Bitmap.createScaledBitmap(raw, sz, sz, true);
        if (out != raw) raw.recycle();
        return out;
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
        iv.setScaleType(ImageView.ScaleType.CENTER);
        iv.setAdjustViewBounds(false);
        return iv;
    }

    public static int dpToPx(Context ctx, float dp) {
        return Math.max(1, Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, ctx.getResources().getDisplayMetrics())));
    }

    public static int iconForGlyph(String glyph) {
        if (glyph == null || glyph.isEmpty()) return DOT;
        String u = glyph.trim().toUpperCase();
        if ("FTP".equals(u) || "FTPS".equals(u)) return FTP;
        if ("RADAR".equals(u) || "SCAN".equals(u) || "SCANNER".equals(u)) return RADAR;
        if ("SSH".equals(u) || "SHELL".equals(u) || "SHELLWAVE".equals(u) || "TERM".equals(u)) return TERMINAL;
        if ("TARGET".equals(u) || "CHECK".equals(u) || "PCHECK".equals(u)) return TARGET;
        if ("COPY".equals(u) || "MIRROR".equals(u)) return COPY;
        if ("TOOLS".equals(u) || "TOOL".equals(u) || "WRENCH".equals(u)) return WRENCH;
        if ("KNOCK".equals(u)) return KNOCK;
        if ("FOLDER".equals(u) || "DIR".equals(u)) return FOLDER;
        if ("TLS".equals(u) || "HTTPS".equals(u)) return HTTPS;
        if ("PORT".equals(u) || "PORTS".equals(u)) return PORT;
        if (glyph.indexOf('\uD83D') >= 0 || glyph.indexOf('\uD83C') >= 0 || glyph.indexOf('\uD83E') >= 0) {
            if (glyph.contains("\uD83D\uDD0C")) return PLUG;
            if (glyph.contains("\uD83D\uDD10")) return LOCK;
            if (glyph.contains("\uD83D\uDD12")) return LOCK;
            if (glyph.contains("\uD83D\uDD11")) return KEY;
            if (glyph.contains("\uD83D\uDC22")) return MIXNET;
            if (glyph.contains("\uD83D\uDC89")) return INJECT;
            if (glyph.contains("\uD83E\uDDC5")) return TOR;
            if (glyph.contains("\uD83D\uDFE3")) return I2P;
            if (glyph.contains("\uD83C\uDF10")) return GLOBE;
            if (glyph.contains("\uD83D\uDEE1")) return SHIELD;
            if (glyph.contains("\uD83D\uDE80")) return ROCKET;
            if (glyph.contains("\uD83D\uDD0D")) return SEARCH;
            if (glyph.contains("\uD83D\uDEE0")) return WRENCH;
            if (glyph.contains("\uD83D\uDDA5")) return APPS;
            if (glyph.contains("\uD83D\uDD17")) return LINK;
            if (glyph.contains("\uD83D\uDCE1") || glyph.contains("\uD83D\uDCF6")) return WIFI;
            if (glyph.contains("\uD83D\uDD14")) return BELL;
            if (glyph.contains("\uD83C\uDF19")) return MOON;
            if (glyph.contains("\uD83D\uDD0B")) return BATTERY;
            if (glyph.contains("\uD83D\uDC64")) return USER;
            if (glyph.contains("\uD83D\uDCC1") || glyph.contains("\uD83D\uDCC2")) return FOLDER;
            if (glyph.contains("\uD83D\uDD04")) return REFRESH;
            if (glyph.contains("\uD83D\uDCE4")) return EXPORT;
            if (glyph.contains("\uD83D\uDCE5")) return IMPORT;
            if (glyph.contains("\uD83D\uDCDD")) return PENCIL;
            if (glyph.contains("\uD83C\uDFE0")) return HOUSE;
            if (glyph.contains("\u26A1")) return FLASH;
            if (glyph.contains("\uD83D\uDCE6")) return CUBE;
            return DOT;
        }
        if ("⌂".equals(glyph)) return HOME;
        if ("⚙".equals(glyph) || "\u2699".equals(glyph)) return GEAR;
        if ("☰".equals(glyph) || "≡".equals(glyph)) return LOGS;
        if ("‹".equals(glyph) || "<".equals(glyph)) return CHEVRON_LEFT;
        if ("›".equals(glyph) || ">".equals(glyph)) return CHEVRON_RIGHT;
        if ("⌄".equals(glyph) || "⌃".equals(glyph)) return CHEVRON_DOWN;
        if ("◈".equals(glyph) || "◆".equals(glyph) || "●".equals(glyph)) return DOT;
        if ("#".equals(glyph)) return PORT;
        if ("⚡".equals(glyph) || "\u26A1".equals(glyph)) return FLASH;
        if ("⚠".equals(glyph)) return WARNING;
        if ("+".equals(glyph) || "＋".equals(glyph) || "⊞".equals(glyph)) return PLUS;
        if ("✓".equals(glyph) || "✔".equals(glyph) || "☑".equals(glyph)) return CHECK;
        if ("\u25A6".equals(glyph)) return LAYERS;
        if ("\u2B83".equals(glyph) || "\u21C4".equals(glyph) || "⇄".equals(glyph)) return SWAP;
        if ("\u260D".equals(glyph) || "☍".equals(glyph)) return LINK;
        if ("\u25AB".equals(glyph)) return CUBE;
        if ("\u229E".equals(glyph)) return APPS;
        if ("\u2726".equals(glyph) || "✦".equals(glyph) || "★".equals(glyph)) return SKID;
        if ("\u223F".equals(glyph)) return WAVE;
        if ("\u26F6".equals(glyph) || "⎔".equals(glyph)) return SOCKET;
        return DOT;
    }

    public static int iconForButtonLabel(String text) {
        if (text == null) return CHECK;
        String t = text.toLowerCase();
        if (t.contains("ftp")) return FTP;
        if (t.contains("scan") || t.contains("lan")) return RADAR;
        if (t.contains("knock")) return KNOCK;
        if (t.contains("mirror")) return COPY;
        if (t.contains("sftp") || t.contains("folder")) return FOLDER;
        if (t.contains("banner")) return SEARCH;
        if (t.contains("import")) return IMPORT;
        if (t.contains("export")) return EXPORT;
        if (t.contains("share")) return SHARE;
        if (t.contains("copy")) return COPY;
        if (t.contains("validate") || t.contains("parse") || t.contains("apply") || t.contains("save")) return CHECK;
        if (t.contains("clear") || (t.contains("reset") && t.contains("counter"))) return TRASH;
        if (t.contains("flush") || t.contains("sync") || t.contains("reset")) return REFRESH;
        if (t.contains("probe") || t.contains("resolve") || t.contains("test") || t.contains("ping")) return RADAR;
        if (t.contains("pcap")) return WAVE;
        if (t.contains("wireguard") || t.contains(".conf")) return KEY;
        if (t.contains("exit")) return TARGET;
        if (t.contains("entry")) return LOGIN;
        if (t.contains("ssh")) return TERMINAL;
        if (t.contains("v2ray") || t.contains("xray") || t.contains("hysteria") || t.contains("json")) return TUNNEL;
        if (t.contains("help")) return SEARCH;
        if (t.contains("add") || t.contains("+ ")) return PLUS;
        if (t.contains("preview") || t.contains("config")) return LOGS;
        if (t.contains("psk") || t.contains("key")) return KEY;
        if (t.contains("doh") || t.contains("https")) return HTTPS;
        if (t.contains("port") || t.contains("check")) return PORT;
        if (t.contains("start") || t.contains("run")) return PLAY;
        if (t.contains("stop")) return STOP;
        if (t.contains("pause")) return PAUSE;
        return CHECK;
    }

    private static void draw(Canvas c, Paint s, Paint f, int kind) {
        switch (kind) {
            case HOME:
            case HOUSE:
                drawHome(c, s);
                break;
            case STATS:
                rr(c, f, 5.0f, 14.2f, 8.6f, 19.5f, 1.2f);
                rr(c, f, 10.2f, 9.2f, 13.8f, 19.5f, 1.2f);
                rr(c, f, 15.4f, 5.2f, 19.0f, 19.5f, 1.2f);
                break;
            case SKID:
                drawSkid(c, s);
                break;
            case GEAR:
                drawGear(c, s, f);
                break;
            case LOGS:
                rr(c, s, 6.2f, 4.5f, 17.8f, 19.5f, 1.8f);
                c.drawLine(9.0f, 9.0f, 15.0f, 9.0f, s);
                c.drawLine(9.0f, 12.5f, 15.0f, 12.5f, s);
                c.drawLine(9.0f, 16.0f, 13.2f, 16.0f, s);
                break;
            case PLAY: {
                Path p = new Path();
                p.moveTo(8.2f, 5.6f);
                p.lineTo(8.2f, 18.4f);
                p.lineTo(18.8f, 12.0f);
                p.close();
                c.drawPath(p, f);
                break;
            }
            case PAUSE:
                rr(c, f, 6.6f, 5.4f, 10.4f, 18.6f, 1.1f);
                rr(c, f, 13.6f, 5.4f, 17.4f, 18.6f, 1.1f);
                break;
            case STOP:
                rr(c, f, 6.4f, 6.4f, 17.6f, 17.6f, 2.4f);
                break;
            case CHEVRON_DOWN:
                c.drawLine(6.4f, 9.2f, 12.0f, 15.4f, s);
                c.drawLine(12.0f, 15.4f, 17.6f, 9.2f, s);
                break;
            case CHEVRON_LEFT:
                c.drawLine(14.8f, 5.6f, 8.4f, 12.0f, s);
                c.drawLine(8.4f, 12.0f, 14.8f, 18.4f, s);
                break;
            case CHEVRON_RIGHT:
                c.drawLine(9.2f, 5.6f, 15.6f, 12.0f, s);
                c.drawLine(15.6f, 12.0f, 9.2f, 18.4f, s);
                break;
            case PLUG:
                c.drawLine(8.6f, 4.2f, 8.6f, 8.6f, s);
                c.drawLine(15.4f, 4.2f, 15.4f, 8.6f, s);
                rr(c, s, 6.4f, 8.6f, 17.6f, 16.6f, 2.0f);
                c.drawLine(12.0f, 16.6f, 12.0f, 20.2f, s);
                break;
            case MIXNET:
                c.drawCircle(7.2f, 8.2f, 2.3f, s);
                c.drawCircle(16.8f, 8.2f, 2.3f, s);
                c.drawCircle(12.0f, 16.6f, 2.3f, s);
                c.drawLine(9.0f, 9.6f, 10.6f, 14.8f, s);
                c.drawLine(15.0f, 9.6f, 13.4f, 14.8f, s);
                c.drawLine(9.5f, 8.2f, 14.5f, 8.2f, s);
                break;
            case INJECT:
                c.drawLine(6.0f, 18.0f, 13.6f, 10.4f, s);
                rr(c, s, 12.4f, 6.4f, 17.2f, 11.2f, 1.2f);
                c.drawLine(15.6f, 6.0f, 18.4f, 8.8f, s);
                c.drawLine(5.2f, 16.6f, 7.4f, 18.8f, s);
                break;
            case LOCK:
                drawLock(c, s, false);
                break;
            case TOR:
                c.drawOval(new RectF(6.4f, 4.2f, 17.6f, 20.0f), s);
                c.drawOval(new RectF(8.2f, 7.2f, 15.8f, 17.0f), s);
                c.drawOval(new RectF(10.0f, 10.0f, 14.0f, 14.4f), s);
                break;
            case I2P:
                c.drawCircle(12.0f, 12.0f, 7.8f, s);
                c.drawLine(12.0f, 7.6f, 12.0f, 14.2f, s);
                c.drawCircle(12.0f, 16.6f, 0.95f, f);
                break;
            case DNS:
            case GLOBE:
                c.drawCircle(12.0f, 12.0f, 7.8f, s);
                c.drawLine(4.2f, 12.0f, 19.8f, 12.0f, s);
                c.drawOval(new RectF(8.2f, 4.2f, 15.8f, 19.8f), s);
                break;
            case SHIELD:
                drawShield(c, s);
                break;
            case PROXY:
                c.drawCircle(6.4f, 12.0f, 2.3f, s);
                c.drawCircle(17.6f, 7.2f, 2.3f, s);
                c.drawCircle(17.6f, 16.8f, 2.3f, s);
                c.drawLine(8.6f, 11.0f, 15.4f, 8.2f, s);
                c.drawLine(8.6f, 13.0f, 15.4f, 15.8f, s);
                break;
            case KEY:
                c.drawCircle(8.0f, 12.0f, 3.3f, s);
                c.drawLine(11.2f, 12.0f, 19.4f, 12.0f, s);
                c.drawLine(16.6f, 12.0f, 16.6f, 15.4f, s);
                c.drawLine(18.6f, 12.0f, 18.6f, 14.4f, s);
                break;
            case TUNNEL: {
                Path t = new Path();
                t.moveTo(4.4f, 17.6f);
                t.quadTo(12.0f, 3.6f, 19.6f, 17.6f);
                c.drawPath(t, s);
                c.drawLine(8.2f, 17.6f, 15.8f, 17.6f, s);
                break;
            }
            case ROCKET: {
                Path r = new Path();
                r.moveTo(12.0f, 3.6f);
                r.lineTo(16.4f, 14.6f);
                r.lineTo(12.0f, 12.4f);
                r.lineTo(7.6f, 14.6f);
                r.close();
                c.drawPath(r, s);
                c.drawLine(9.4f, 15.2f, 8.2f, 19.6f, s);
                c.drawLine(14.6f, 15.2f, 15.8f, 19.6f, s);
                c.drawCircle(12.0f, 8.4f, 1.15f, f);
                break;
            }
            case FLASH:
            case BOLT: {
                Path b = new Path();
                b.moveTo(13.6f, 3.6f);
                b.lineTo(8.0f, 13.0f);
                b.lineTo(12.2f, 13.0f);
                b.lineTo(10.0f, 20.4f);
                b.lineTo(16.4f, 10.4f);
                b.lineTo(12.2f, 10.4f);
                b.close();
                c.drawPath(b, f);
                break;
            }
            case WIFI:
                c.drawCircle(12.0f, 18.6f, 1.2f, f);
                c.drawArc(new RectF(8.2f, 12.8f, 15.8f, 20.4f), 210, 120, false, s);
                c.drawArc(new RectF(5.4f, 8.8f, 18.6f, 22.0f), 210, 120, false, s);
                c.drawArc(new RectF(3.2f, 5.2f, 20.8f, 22.8f), 210, 120, false, s);
                break;
            case COPY:
                rr(c, s, 8.0f, 7.4f, 19.2f, 19.6f, 1.8f);
                rr(c, s, 4.8f, 4.4f, 16.0f, 16.4f, 1.8f);
                break;
            case SHARE:
                c.drawCircle(17.6f, 6.4f, 2.0f, s);
                c.drawCircle(6.4f, 12.0f, 2.0f, s);
                c.drawCircle(17.6f, 17.6f, 2.0f, s);
                c.drawLine(8.2f, 11.0f, 15.6f, 7.4f, s);
                c.drawLine(8.2f, 13.0f, 15.6f, 16.6f, s);
                break;
            case IMPORT:
                c.drawLine(12.0f, 4.4f, 12.0f, 14.6f, s);
                c.drawLine(8.2f, 11.0f, 12.0f, 15.0f, s);
                c.drawLine(15.8f, 11.0f, 12.0f, 15.0f, s);
                c.drawLine(5.4f, 18.8f, 18.6f, 18.8f, s);
                break;
            case EXPORT:
            case DOWNLOAD:
                c.drawLine(12.0f, 4.4f, 12.0f, 14.6f, s);
                c.drawLine(8.2f, 11.0f, 12.0f, 15.0f, s);
                c.drawLine(15.8f, 11.0f, 12.0f, 15.0f, s);
                c.drawLine(5.4f, 18.8f, 18.6f, 18.8f, s);
                break;
            case CHECK:
                c.drawLine(5.2f, 12.4f, 10.2f, 17.6f, s);
                c.drawLine(10.2f, 17.6f, 19.0f, 6.4f, s);
                break;
            case TRASH:
                rr(c, s, 7.2f, 8.6f, 16.8f, 19.6f, 1.4f);
                c.drawLine(5.2f, 8.6f, 18.8f, 8.6f, s);
                c.drawLine(9.6f, 5.2f, 14.4f, 5.2f, s);
                c.drawLine(10.4f, 11.4f, 10.4f, 16.6f, s);
                c.drawLine(13.6f, 11.4f, 13.6f, 16.6f, s);
                break;
            case PLUS:
                c.drawLine(12.0f, 5.4f, 12.0f, 18.6f, s);
                c.drawLine(5.4f, 12.0f, 18.6f, 12.0f, s);
                break;
            case REFRESH:
                c.drawArc(new RectF(5.0f, 5.0f, 19.0f, 19.0f), 40, 260, false, s);
                c.drawLine(18.8f, 5.6f, 18.8f, 10.6f, s);
                c.drawLine(18.8f, 5.6f, 14.2f, 5.6f, s);
                break;
            case RADAR:
                c.drawCircle(12.0f, 12.0f, 7.8f, s);
                c.drawCircle(12.0f, 12.0f, 4.2f, s);
                c.drawLine(12.0f, 12.0f, 18.0f, 7.0f, s);
                c.drawCircle(12.0f, 12.0f, 1.15f, f);
                break;
            case TARGET:
                c.drawCircle(12.0f, 12.0f, 7.8f, s);
                c.drawCircle(12.0f, 12.0f, 4.4f, s);
                c.drawCircle(12.0f, 12.0f, 1.35f, f);
                break;
            case LOGIN:
                rr(c, s, 11.0f, 5.0f, 19.4f, 19.0f, 1.8f);
                c.drawLine(4.4f, 12.0f, 13.6f, 12.0f, s);
                c.drawLine(10.4f, 8.8f, 14.0f, 12.0f, s);
                c.drawLine(10.4f, 15.2f, 14.0f, 12.0f, s);
                break;
            case BRIDGE:
                c.drawLine(4.4f, 16.8f, 19.6f, 16.8f, s);
                c.drawArc(new RectF(6.4f, 7.2f, 17.6f, 16.8f), 180, 180, false, s);
                c.drawLine(8.4f, 16.8f, 8.4f, 12.4f, s);
                c.drawLine(12.0f, 16.8f, 12.0f, 10.0f, s);
                c.drawLine(15.6f, 16.8f, 15.6f, 12.4f, s);
                break;
            case SEARCH:
                c.drawCircle(10.4f, 10.4f, 5.4f, s);
                c.drawLine(14.4f, 14.4f, 19.2f, 19.2f, s);
                break;
            case CLOCK:
                c.drawCircle(12.0f, 12.0f, 7.8f, s);
                c.drawLine(12.0f, 12.0f, 12.0f, 7.2f, s);
                c.drawLine(12.0f, 12.0f, 16.2f, 14.2f, s);
                break;
            case UMBRELLA:
                c.drawArc(new RectF(4.6f, 5.2f, 19.4f, 18.4f), 180, 180, true, s);
                c.drawLine(12.0f, 12.0f, 12.0f, 19.2f, s);
                c.drawArc(new RectF(12.0f, 17.4f, 16.2f, 20.6f), 0, 180, false, s);
                break;
            case DICE:
                rr(c, s, 5.4f, 5.4f, 18.6f, 18.6f, 2.4f);
                c.drawCircle(9.0f, 9.0f, 1.05f, f);
                c.drawCircle(15.0f, 9.0f, 1.05f, f);
                c.drawCircle(12.0f, 12.0f, 1.05f, f);
                c.drawCircle(9.0f, 15.0f, 1.05f, f);
                c.drawCircle(15.0f, 15.0f, 1.05f, f);
                break;
            case WAVE: {
                Path w = new Path();
                w.moveTo(4.0f, 13.4f);
                w.cubicTo(7.4f, 8.4f, 10.4f, 18.4f, 14.0f, 13.4f);
                w.cubicTo(16.6f, 9.8f, 18.6f, 16.6f, 20.6f, 12.4f);
                c.drawPath(w, s);
                break;
            }
            case FILTER: {
                Path fp = new Path();
                fp.moveTo(5.0f, 5.8f);
                fp.lineTo(19.0f, 5.8f);
                fp.lineTo(14.2f, 12.4f);
                fp.lineTo(14.2f, 18.6f);
                fp.lineTo(9.8f, 16.6f);
                fp.lineTo(9.8f, 12.4f);
                fp.close();
                c.drawPath(fp, s);
                break;
            }
            case SERVER:
                rr(c, s, 5.0f, 4.8f, 19.0f, 10.4f, 1.6f);
                rr(c, s, 5.0f, 13.6f, 19.0f, 19.2f, 1.6f);
                c.drawCircle(8.0f, 7.6f, 0.9f, f);
                c.drawCircle(8.0f, 16.4f, 0.9f, f);
                c.drawLine(10.6f, 7.6f, 16.6f, 7.6f, s);
                c.drawLine(10.6f, 16.4f, 16.6f, 16.4f, s);
                break;
            case APPS:
                rr(c, s, 4.8f, 4.8f, 10.6f, 10.6f, 1.4f);
                rr(c, s, 13.4f, 4.8f, 19.2f, 10.6f, 1.4f);
                rr(c, s, 4.8f, 13.4f, 10.6f, 19.2f, 1.4f);
                rr(c, s, 13.4f, 13.4f, 19.2f, 19.2f, 1.4f);
                break;
            case SLIDERS:
                c.drawLine(5.4f, 7.4f, 18.6f, 7.4f, s);
                c.drawLine(5.4f, 12.0f, 18.6f, 12.0f, s);
                c.drawLine(5.4f, 16.6f, 18.6f, 16.6f, s);
                c.drawCircle(15.2f, 7.4f, 1.85f, f);
                c.drawCircle(9.2f, 12.0f, 1.85f, f);
                c.drawCircle(13.6f, 16.6f, 1.85f, f);
                break;
            case LINK:
                c.drawArc(new RectF(4.0f, 8.2f, 12.8f, 15.8f), 50, 260, false, s);
                c.drawArc(new RectF(11.2f, 8.2f, 20.0f, 15.8f), 230, 260, false, s);
                break;
            case SWAP:
                c.drawLine(5.0f, 8.8f, 19.0f, 8.8f, s);
                c.drawLine(15.4f, 5.8f, 19.0f, 8.8f, s);
                c.drawLine(15.4f, 11.8f, 19.0f, 8.8f, s);
                c.drawLine(19.0f, 15.2f, 5.0f, 15.2f, s);
                c.drawLine(8.6f, 12.2f, 5.0f, 15.2f, s);
                c.drawLine(8.6f, 18.2f, 5.0f, 15.2f, s);
                break;
            case DOT:
                c.drawCircle(12.0f, 12.0f, 2.5f, f);
                break;
            case STAR: {
                Path st = new Path();
                for (int i = 0; i < 5; i++) {
                    double a = Math.toRadians(-90 + i * 72);
                    double b = Math.toRadians(-90 + i * 72 + 36);
                    float ox = (float) (12 + Math.cos(a) * 7.8);
                    float oy = (float) (12 + Math.sin(a) * 7.8);
                    float ix = (float) (12 + Math.cos(b) * 3.3);
                    float iy = (float) (12 + Math.sin(b) * 3.3);
                    if (i == 0) st.moveTo(ox, oy);
                    else st.lineTo(ox, oy);
                    st.lineTo(ix, iy);
                }
                st.close();
                c.drawPath(st, f);
                break;
            }
            case CUBE: {
                Path cu = new Path();
                cu.moveTo(12.0f, 4.4f);
                cu.lineTo(19.2f, 8.4f);
                cu.lineTo(19.2f, 15.6f);
                cu.lineTo(12.0f, 19.6f);
                cu.lineTo(4.8f, 15.6f);
                cu.lineTo(4.8f, 8.4f);
                cu.close();
                c.drawPath(cu, s);
                c.drawLine(12.0f, 4.4f, 12.0f, 12.2f, s);
                c.drawLine(12.0f, 12.2f, 19.2f, 15.6f, s);
                c.drawLine(12.0f, 12.2f, 4.8f, 15.6f, s);
                break;
            }
            case WARNING: {
                Path tri = new Path();
                tri.moveTo(12.0f, 4.2f);
                tri.lineTo(20.4f, 19.2f);
                tri.lineTo(3.6f, 19.2f);
                tri.close();
                c.drawPath(tri, s);
                c.drawLine(12.0f, 9.6f, 12.0f, 14.6f, s);
                c.drawCircle(12.0f, 16.8f, 0.75f, f);
                break;
            }
            case EYE:
                c.drawOval(new RectF(3.6f, 8.0f, 20.4f, 16.0f), s);
                c.drawCircle(12.0f, 12.0f, 2.6f, s);
                c.drawCircle(12.0f, 12.0f, 1.05f, f);
                break;
            case NETWORK:
                c.drawCircle(12.0f, 6.2f, 2.15f, s);
                c.drawCircle(6.2f, 17.4f, 2.15f, s);
                c.drawCircle(17.8f, 17.4f, 2.15f, s);
                c.drawLine(12.0f, 8.4f, 7.6f, 15.6f, s);
                c.drawLine(12.0f, 8.4f, 16.4f, 15.6f, s);
                c.drawLine(8.3f, 17.4f, 15.7f, 17.4f, s);
                break;
            case HOURGLASS: {
                Path hg = new Path();
                hg.moveTo(7.0f, 5.0f);
                hg.lineTo(17.0f, 5.0f);
                hg.lineTo(12.0f, 12.0f);
                hg.close();
                c.drawPath(hg, s);
                Path hg2 = new Path();
                hg2.moveTo(7.0f, 19.0f);
                hg2.lineTo(17.0f, 19.0f);
                hg2.lineTo(12.0f, 12.0f);
                hg2.close();
                c.drawPath(hg2, s);
                break;
            }
            case SCISSORS:
                c.drawCircle(7.2f, 7.2f, 2.3f, s);
                c.drawCircle(7.2f, 16.8f, 2.3f, s);
                c.drawLine(9.2f, 8.6f, 19.0f, 16.5f, s);
                c.drawLine(9.2f, 15.4f, 19.0f, 7.5f, s);
                break;
            case USER:
                c.drawCircle(12.0f, 8.0f, 3.4f, s);
                c.drawArc(new RectF(5.2f, 13.0f, 18.8f, 22.4f), 200, 140, false, s);
                break;
            case BELL: {
                Path be = new Path();
                be.moveTo(7.2f, 10.4f);
                be.quadTo(7.2f, 5.2f, 12.0f, 5.2f);
                be.quadTo(16.8f, 5.2f, 16.8f, 10.4f);
                be.lineTo(16.8f, 15.4f);
                be.lineTo(19.0f, 17.8f);
                be.lineTo(5.0f, 17.8f);
                be.lineTo(7.2f, 15.4f);
                be.close();
                c.drawPath(be, s);
                c.drawArc(new RectF(10.0f, 17.2f, 14.0f, 20.4f), 0, 180, false, s);
                break;
            }
            case MOON:
                c.drawArc(new RectF(5.4f, 4.6f, 18.6f, 19.4f), 40, 280, false, s);
                c.drawArc(new RectF(9.6f, 6.4f, 20.8f, 17.8f), 310, -220, false, s);
                break;
            case BATTERY:
                rr(c, s, 4.2f, 8.0f, 18.2f, 16.0f, 1.6f);
                rr(c, f, 18.2f, 10.4f, 20.2f, 13.6f, 0.6f);
                rr(c, f, 6.2f, 10.0f, 14.6f, 14.0f, 0.7f);
                break;
            case CLOUD: {
                Path cl = new Path();
                cl.moveTo(7.2f, 16.4f);
                cl.lineTo(17.2f, 16.4f);
                cl.arcTo(new RectF(14.4f, 10.6f, 19.6f, 16.4f), 90, -160);
                cl.arcTo(new RectF(9.6f, 7.2f, 16.8f, 14.8f), 20, -200);
                cl.arcTo(new RectF(4.4f, 10.8f, 10.4f, 16.4f), 250, -140);
                cl.close();
                c.drawPath(cl, s);
                break;
            }
            case HASH:
                c.drawLine(9.2f, 5.4f, 7.4f, 18.6f, s);
                c.drawLine(16.6f, 5.4f, 14.8f, 18.6f, s);
                c.drawLine(5.0f, 9.4f, 19.4f, 9.4f, s);
                c.drawLine(4.6f, 14.6f, 19.0f, 14.6f, s);
                break;
            case PENCIL:
                c.drawLine(7.2f, 16.8f, 16.6f, 7.4f, s);
                c.drawLine(16.6f, 7.4f, 18.4f, 9.2f, s);
                c.drawLine(18.4f, 9.2f, 9.0f, 18.6f, s);
                c.drawLine(7.2f, 16.8f, 5.4f, 18.6f, s);
                break;
            case LAYERS: {
                Path l1 = new Path();
                l1.moveTo(12.0f, 4.4f);
                l1.lineTo(20.0f, 9.0f);
                l1.lineTo(12.0f, 13.6f);
                l1.lineTo(4.0f, 9.0f);
                l1.close();
                c.drawPath(l1, s);
                c.drawLine(4.0f, 12.6f, 12.0f, 17.2f, s);
                c.drawLine(12.0f, 17.2f, 20.0f, 12.6f, s);
                c.drawLine(4.0f, 15.8f, 12.0f, 20.4f, s);
                c.drawLine(12.0f, 20.4f, 20.0f, 15.8f, s);
                break;
            }
            case ROUTE:
                c.drawCircle(7.0f, 7.0f, 2.15f, s);
                c.drawCircle(17.0f, 17.0f, 2.15f, s);
                Path rt = new Path();
                rt.moveTo(8.8f, 8.6f);
                rt.lineTo(12.0f, 8.6f);
                rt.lineTo(12.0f, 15.4f);
                rt.lineTo(15.2f, 15.4f);
                c.drawPath(rt, s);
                break;
            case SHIELD_LOCK:
                drawShield(c, s);
                c.drawCircle(12.0f, 11.2f, 1.5f, s);
                c.drawLine(12.0f, 12.6f, 12.0f, 15.2f, s);
                break;
            case PORT:
                rr(c, s, 4.4f, 7.4f, 19.6f, 16.6f, 2.2f);
                c.drawCircle(9.0f, 12.0f, 1.35f, f);
                c.drawCircle(15.0f, 12.0f, 1.35f, f);
                c.drawLine(12.0f, 16.6f, 12.0f, 19.4f, s);
                break;
            case HTTPS:
                drawLock(c, s, true);
                break;
            case SOCKET:
                rr(c, s, 5.4f, 4.4f, 18.6f, 19.6f, 2.6f);
                c.drawCircle(9.2f, 10.8f, 1.45f, s);
                c.drawCircle(14.8f, 10.8f, 1.45f, s);
                c.drawArc(new RectF(9.2f, 13.6f, 14.8f, 17.6f), 20, 140, false, s);
                break;
            case FTP:
                rr(c, s, 4.6f, 8.4f, 19.4f, 17.6f, 1.8f);
                c.drawLine(4.6f, 12.0f, 19.4f, 12.0f, s);
                c.drawLine(12.0f, 8.4f, 12.0f, 17.6f, s);
                c.drawLine(12.0f, 5.0f, 12.0f, 8.4f, s);
                c.drawLine(9.4f, 6.8f, 12.0f, 4.6f, s);
                c.drawLine(14.6f, 6.8f, 12.0f, 4.6f, s);
                break;
            case TERMINAL:
                rr(c, s, 4.4f, 5.2f, 19.6f, 18.8f, 2.0f);
                c.drawLine(7.2f, 9.2f, 10.8f, 12.0f, s);
                c.drawLine(10.8f, 12.0f, 7.2f, 14.8f, s);
                c.drawLine(12.4f, 15.4f, 17.0f, 15.4f, s);
                break;
            case FOLDER: {
                Path fo = new Path();
                fo.moveTo(4.4f, 8.2f);
                fo.lineTo(4.4f, 18.6f);
                fo.lineTo(19.6f, 18.6f);
                fo.lineTo(19.6f, 9.6f);
                fo.lineTo(12.4f, 9.6f);
                fo.lineTo(10.6f, 7.0f);
                fo.lineTo(4.4f, 7.0f);
                fo.close();
                c.drawPath(fo, s);
                break;
            }
            case WRENCH:
                c.drawCircle(8.2f, 8.2f, 3.0f, s);
                c.drawLine(10.4f, 10.4f, 18.6f, 18.6f, s);
                c.drawLine(16.4f, 16.4f, 19.2f, 19.2f, s);
                c.drawLine(16.4f, 19.2f, 19.2f, 16.4f, s);
                break;
            case KNOCK:
                rr(c, s, 5.0f, 4.6f, 19.0f, 19.4f, 2.2f);
                c.drawCircle(12.0f, 12.6f, 2.4f, s);
                c.drawLine(12.0f, 7.2f, 12.0f, 9.4f, s);
                break;
            default:
                c.drawCircle(12.0f, 12.0f, 2.5f, f);
                break;
        }
    }

    private static void rr(Canvas c, Paint p, float l, float t, float r, float b, float rad) {
        c.drawRoundRect(new RectF(l, t, r, b), rad, rad, p);
    }

    private static void drawHome(Canvas c, Paint s) {
        Path roof = new Path();
        roof.moveTo(4.2f, 12.0f);
        roof.lineTo(12.0f, 4.4f);
        roof.lineTo(19.8f, 12.0f);
        c.drawPath(roof, s);
        Path body = new Path();
        body.moveTo(7.0f, 11.4f);
        body.lineTo(7.0f, 19.6f);
        body.lineTo(17.0f, 19.6f);
        body.lineTo(17.0f, 11.4f);
        c.drawPath(body, s);
        rr(c, s, 10.4f, 14.2f, 13.6f, 19.6f, 0.6f);
    }

    private static void drawSkid(Canvas c, Paint s) {
        Path hex = new Path();
        for (int i = 0; i < 6; i++) {
            double a = Math.toRadians(-90 + i * 60);
            float x = (float) (12 + Math.cos(a) * 9.0);
            float y = (float) (12 + Math.sin(a) * 9.0);
            if (i == 0) hex.moveTo(x, y);
            else hex.lineTo(x, y);
        }
        hex.close();
        c.drawPath(hex, s);
        Path u = new Path();
        u.moveTo(8.6f, 8.2f);
        u.lineTo(8.6f, 13.6f);
        u.quadTo(8.6f, 16.8f, 12.0f, 16.8f);
        u.quadTo(15.4f, 16.8f, 15.4f, 13.6f);
        u.lineTo(15.4f, 8.2f);
        c.drawPath(u, s);
    }

    private static void drawGear(Canvas c, Paint s, Paint f) {
        c.drawCircle(12.0f, 12.0f, 3.6f, s);
        c.drawCircle(12.0f, 12.0f, 1.5f, f);
        for (int i = 0; i < 8; i++) {
            double a = Math.toRadians(i * 45);
            float ca = (float) Math.cos(a);
            float sa = (float) Math.sin(a);
            c.drawLine(12f + ca * 5.0f, 12f + sa * 5.0f,
                    12f + ca * 8.2f, 12f + sa * 8.2f, s);
        }
    }

    private static void drawLock(Canvas c, Paint s, boolean bar) {
        rr(c, s, 6.4f, 11.0f, 17.6f, 20.0f, 2.0f);
        c.drawArc(new RectF(8.2f, 4.8f, 15.8f, 13.2f), 180, 180, false, s);
        if (bar) c.drawLine(10.4f, 15.4f, 13.6f, 15.4f, s);
    }

    private static void drawShield(Canvas c, Paint s) {
        Path sh = new Path();
        sh.moveTo(12.0f, 3.8f);
        sh.lineTo(19.2f, 6.6f);
        sh.lineTo(19.2f, 13.2f);
        sh.quadTo(19.2f, 18.6f, 12.0f, 20.6f);
        sh.quadTo(4.8f, 18.6f, 4.8f, 13.2f);
        sh.lineTo(4.8f, 6.6f);
        sh.close();
        c.drawPath(sh, s);
    }
}
