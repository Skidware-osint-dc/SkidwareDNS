package com.skidwaredns.app.ui;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.skidwaredns.app.config.SkidConfig;
import com.skidwaredns.app.proxy.OpenPortEngine;
import com.skidwaredns.app.tools.FtpsServerEngine;
import com.skidwaredns.app.tools.LanScannerEngine;
import com.skidwaredns.app.tools.MirrorEngine;
import com.skidwaredns.app.tools.PortCheckerEngine;
import com.skidwaredns.app.tools.PortKnockEngine;
import com.skidwaredns.app.tools.SshSessionEngine;
import com.skidwaredns.app.tools.ToolNet;

import java.io.File;
import java.util.List;

/**
 * Tools hub: FTPS share, LAN scanner, SSH (Shellwave), port knock,
 * port checker, folder mirror. Java 8, no lambdas.
 */
public final class ToolScreens {

    public static final int SUB_TOOLS_HUB = 40;
    public static final int SUB_FTPS = 41;
    public static final int SUB_SCANNER = 42;
    public static final int SUB_SHELLWAVE = 43;
    public static final int SUB_KNOCK = 44;
    public static final int SUB_PORTCHECK = 45;
    public static final int SUB_MIRROR = 46;

    private ToolScreens() {}

    public static View build(final Context ctx, int subId, final ConfigureScreens.HostCallbacks host) {
        switch (subId) {
            case SUB_FTPS: return buildFtps(ctx, host);
            case SUB_SCANNER: return buildScanner(ctx, host);
            case SUB_SHELLWAVE: return buildSsh(ctx, host);
            case SUB_KNOCK: return buildKnock(ctx, host);
            case SUB_PORTCHECK: return buildChecker(ctx, host);
            case SUB_MIRROR: return buildMirror(ctx, host);
            case SUB_TOOLS_HUB:
            default: return buildHub(ctx, host);
        }
    }

    private static View buildHub(final Context ctx, final ConfigureScreens.HostCallbacks host) {
        ScrollView sv = wrap(ctx);
        LinearLayout col = inner(ctx);
        col.addView(UiFactory.backHeader(ctx, "Tools",
                "FTPS, LAN scan, SSH, knock, port-check, mirror",
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        host.openConfigureSubScreen(ConfigureScreens.SUB_HUB);
                    }
                }));
        col.addView(UiFactory.label(ctx,
                "Reimplemented inside SkidwareDNS (not a copy of those APKs). Scan and knock only hosts you own or have permission to test.",
                12f, UiFactory.COLOR_MUTED, false, false));

        col.addView(UiFactory.configureHubCard(ctx, "FTP", "FTPS / file share",
                FtpsServerEngine.get().isRunning() ? FtpsServerEngine.get().status()
                        : "Share this phone over FTP on :" + host.getConfig().toolsFtpsPort,
                go(host, SUB_FTPS)));
        col.addView(UiFactory.configureHubCard(ctx, "RADAR", "Network scanner",
                LanScannerEngine.get().summary(),
                go(host, SUB_SCANNER)));
        col.addView(UiFactory.configureHubCard(ctx, "SSH", "Shellwave SSH",
                host.getConfig().toolsSshUser + "@" + empty(host.getConfig().toolsSshHost, "host")
                        + ":" + host.getConfig().toolsSshPort,
                go(host, SUB_SHELLWAVE)));
        col.addView(UiFactory.configureHubCard(ctx, "#", "Knock on Ports",
                empty(host.getConfig().toolsKnockSequence, "no sequence"),
                go(host, SUB_KNOCK)));
        col.addView(UiFactory.configureHubCard(ctx, "TARGET", "Port checker",
                host.getConfig().toolsCheckHost + "  "
                        + host.getConfig().toolsCheckFrom + "-" + host.getConfig().toolsCheckTo,
                go(host, SUB_PORTCHECK)));
        col.addView(UiFactory.configureHubCard(ctx, "COPY", "Folder mirror",
                empty(host.getConfig().toolsMirrorDst, "local / ftp / sftp / http"),
                go(host, SUB_MIRROR)));
        sv.addView(col);
        return sv;
    }

    private static View buildFtps(final Context ctx, final ConfigureScreens.HostCallbacks host) {
        ScrollView sv = wrap(ctx);
        LinearLayout col = inner(ctx);
        final SkidConfig cfg = host.getConfig();
        col.addView(back(ctx, host, "FTPS / file share", "LAN FTP with password (PASV, UTF-8)"));
        col.addView(UiFactory.label(ctx,
                "Inspired by drweb86/dotnet-ftps-server. This phone is the server. "
                        + "TLS/AUTH is not bundled (no device cert); use a LAN you trust.",
                12f, UiFactory.COLOR_MUTED, false, false));

        LinearLayout card = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        card.addView(UiFactory.label(ctx, FtpsServerEngine.get().status(), 13.5f,
                FtpsServerEngine.get().isRunning() ? UiFactory.COLOR_EMERALD : UiFactory.COLOR_SKY, true, false));
        card.addView(UiFactory.label(ctx, "Bind " + OpenPortEngine.primaryDeviceIpv4(),
                12f, UiFactory.COLOR_MUTED, false, true));
        col.addView(card);

        col.addView(UiFactory.categoryTitle(ctx, "Listen"));
        final EditText portEt = numbered(ctx, String.valueOf(cfg.toolsFtpsPort), "2121");
        col.addView(labeled(ctx, "Port (1024+)", portEt));
        final EditText userEt = UiFactory.monoInput(ctx, cfg.toolsFtpsUser, "user", false);
        col.addView(labeled(ctx, "User", userEt));
        final EditText passEt = UiFactory.monoInput(ctx, cfg.toolsFtpsPass, "password", false);
        passEt.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        col.addView(labeled(ctx, "Password", passEt));
        File root = ftpRoot(ctx);
        col.addView(UiFactory.label(ctx, "Share folder: " + root.getAbsolutePath(),
                12f, UiFactory.COLOR_MUTED, false, true));

        LinearLayout btns = row(ctx);
        Button start = UiFactory.actionButton(ctx, "Start FTP", UiFactory.COLOR_SKY, UiFactory.COLOR_BG, UiFactory.COLOR_SKY);
        start.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                saveFtps(cfg, portEt, userEt, passEt);
                String msg = FtpsServerEngine.get().start(cfg.toolsFtpsPort, cfg.toolsFtpsUser, cfg.toolsFtpsPass, ftpRoot(ctx));
                host.persistAndReload(msg);
                Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show();
            }
        });
        Button stop = UiFactory.actionButton(ctx, "Stop", UiFactory.COLOR_CARD, UiFactory.COLOR_TEXT, UiFactory.COLOR_BORDER);
        stop.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                saveFtps(cfg, portEt, userEt, passEt);
                String msg = FtpsServerEngine.get().stop();
                host.persistAndReload(msg);
            }
        });
        btns.addView(start, btnLp(ctx, 1f));
        btns.addView(stop, btnLp(ctx, 0.6f));
        col.addView(btns);
        sv.addView(col);
        return sv;
    }

    private static View buildScanner(final Context ctx, final ConfigureScreens.HostCallbacks host) {
        ScrollView sv = wrap(ctx);
        LinearLayout col = inner(ctx);
        final SkidConfig cfg = host.getConfig();
        col.addView(back(ctx, host, "Network scanner", ToolNet.guessLanCidr()));
        col.addView(UiFactory.label(ctx,
                "Inspired by usamaiqb/network-scanner. TCP sweep of this /24 plus mDNS and SSDP. No root, no ARP on Android 10+.",
                12f, UiFactory.COLOR_MUTED, false, false));
        final EditText portsEt = UiFactory.monoInput(ctx, cfg.toolsScanPorts, "22,80,443,445,8080", false);
        col.addView(labeled(ctx, "Ports to probe", portsEt));
        Button goBtn = UiFactory.actionButton(ctx,
                LanScannerEngine.get().isRunning() ? "Scanning..." : "Scan LAN",
                UiFactory.COLOR_SKY, UiFactory.COLOR_BG, UiFactory.COLOR_SKY);
        goBtn.setEnabled(!LanScannerEngine.get().isRunning());
        goBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                cfg.toolsScanPorts = portsEt.getText().toString().trim();
                host.persistAndReload("scan start");
                host.getBgExecutor().execute(new Runnable() {
                    @Override public void run() {
                        LanScannerEngine.get().scan(cfg.toolsScanPorts, 220);
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override public void run() {
                                host.openConfigureSubScreen(SUB_SCANNER);
                            }
                        });
                    }
                });
            }
        });
        col.addView(goBtn);
        col.addView(UiFactory.label(ctx, LanScannerEngine.get().summary(), 12.5f, UiFactory.COLOR_EMERALD, true, false));
        List<LanScannerEngine.Device> devs = LanScannerEngine.get().lastDevices();
        LinearLayout card = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        if (devs.isEmpty()) {
            card.addView(UiFactory.label(ctx, "No results yet.", 12.5f, UiFactory.COLOR_MUTED, false, false));
        } else {
            for (int i = 0; i < devs.size(); i++) {
                if (i > 0) card.addView(UiFactory.divider(ctx));
                LanScannerEngine.Device d = devs.get(i);
                card.addView(UiFactory.label(ctx, d.title(), 14f, UiFactory.COLOR_TEXT, true, false));
                card.addView(UiFactory.label(ctx, d.portsLine(), 12f, UiFactory.COLOR_MUTED, false, true));
            }
        }
        col.addView(card);
        sv.addView(col);
        return sv;
    }

    private static View buildSsh(final Context ctx, final ConfigureScreens.HostCallbacks host) {
        ScrollView sv = wrap(ctx);
        LinearLayout col = inner(ctx);
        final SkidConfig cfg = host.getConfig();
        col.addView(back(ctx, host, "Shellwave SSH", "Banner, exec, SFTP list — password auth"));
        col.addView(UiFactory.label(ctx,
                "Inspired by LordOfPolls/Shellwave. Password auth via JSch. Host key is shown after connect; this is an admin client, not a full terminal emulator.",
                12f, UiFactory.COLOR_MUTED, false, false));
        final EditText hostEt = UiFactory.monoInput(ctx, cfg.toolsSshHost, "host or IP", false);
        col.addView(labeled(ctx, "Host", hostEt));
        final EditText portEt = numbered(ctx, String.valueOf(cfg.toolsSshPort), "22");
        col.addView(labeled(ctx, "Port", portEt));
        final EditText userEt = UiFactory.monoInput(ctx, cfg.toolsSshUser, "user", false);
        col.addView(labeled(ctx, "User", userEt));
        final EditText passEt = UiFactory.monoInput(ctx, cfg.toolsSshPass, "password", false);
        passEt.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        col.addView(labeled(ctx, "Password", passEt));
        final EditText cmdEt = UiFactory.monoInput(ctx, cfg.toolsSshCommand, "uname -a", false);
        col.addView(labeled(ctx, "Command", cmdEt));

        LinearLayout btns = row(ctx);
        Button probe = UiFactory.actionButton(ctx, "Banner", UiFactory.COLOR_CARD, UiFactory.COLOR_TEXT, UiFactory.COLOR_BORDER);
        Button exec = UiFactory.actionButton(ctx, "Run", UiFactory.COLOR_SKY, UiFactory.COLOR_BG, UiFactory.COLOR_SKY);
        Button ls = UiFactory.actionButton(ctx, "SFTP ls", UiFactory.COLOR_CARD, UiFactory.COLOR_TEXT, UiFactory.COLOR_BORDER);
        probe.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                saveSsh(cfg, hostEt, portEt, userEt, passEt, cmdEt);
                runBg(host, new Runnable() {
                    @Override public void run() {
                        SshSessionEngine.get().probe(cfg.toolsSshHost, cfg.toolsSshPort);
                    }
                }, SUB_SHELLWAVE);
            }
        });
        exec.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                saveSsh(cfg, hostEt, portEt, userEt, passEt, cmdEt);
                runBg(host, new Runnable() {
                    @Override public void run() {
                        SshSessionEngine.get().exec(cfg.toolsSshHost, cfg.toolsSshPort,
                                cfg.toolsSshUser, cfg.toolsSshPass, cfg.toolsSshCommand);
                    }
                }, SUB_SHELLWAVE);
            }
        });
        ls.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                saveSsh(cfg, hostEt, portEt, userEt, passEt, cmdEt);
                runBg(host, new Runnable() {
                    @Override public void run() {
                        SshSessionEngine.get().sftpLs(cfg.toolsSshHost, cfg.toolsSshPort,
                                cfg.toolsSshUser, cfg.toolsSshPass, ".");
                    }
                }, SUB_SHELLWAVE);
            }
        });
        btns.addView(probe, btnLp(ctx, 1f));
        btns.addView(exec, btnLp(ctx, 1f));
        btns.addView(ls, btnLp(ctx, 1f));
        col.addView(btns);

        LinearLayout out = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        String text = SshSessionEngine.get().lastOut();
        if (text == null || text.isEmpty()) text = "Output lands here.";
        out.addView(UiFactory.label(ctx, text, 12f, UiFactory.COLOR_TEXT, false, true));
        col.addView(out);
        sv.addView(col);
        return sv;
    }

    private static View buildKnock(final Context ctx, final ConfigureScreens.HostCallbacks host) {
        ScrollView sv = wrap(ctx);
        LinearLayout col = inner(ctx);
        final SkidConfig cfg = host.getConfig();
        col.addView(back(ctx, host, "Knock on Ports", "TCP / UDP sequence with delay"));
        col.addView(UiFactory.label(ctx,
                "Inspired by impalex/knockonports. Compatible with knockd. ICMP knocks need root and are skipped. Format: 7000,8000/udp,9000/tcp",
                12f, UiFactory.COLOR_MUTED, false, false));
        final EditText hostEt = UiFactory.monoInput(ctx, cfg.toolsKnockHost, "host or IP", false);
        col.addView(labeled(ctx, "Host", hostEt));
        final EditText seqEt = UiFactory.monoInput(ctx, cfg.toolsKnockSequence, "7000,8000/udp,9000", false);
        col.addView(labeled(ctx, "Sequence", seqEt));
        final EditText protoEt = UiFactory.monoInput(ctx, cfg.toolsKnockProto, "TCP or UDP default", false);
        col.addView(labeled(ctx, "Default proto", protoEt));
        final EditText delayEt = numbered(ctx, String.valueOf(cfg.toolsKnockDelayMs), "200");
        col.addView(labeled(ctx, "Delay ms", delayEt));
        Button goBtn = UiFactory.actionButton(ctx, "Knock", UiFactory.COLOR_SKY, UiFactory.COLOR_BG, UiFactory.COLOR_SKY);
        goBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                cfg.toolsKnockHost = hostEt.getText().toString().trim();
                cfg.toolsKnockSequence = seqEt.getText().toString().trim();
                cfg.toolsKnockProto = protoEt.getText().toString().trim();
                try { cfg.toolsKnockDelayMs = Integer.parseInt(delayEt.getText().toString().trim()); }
                catch (Exception ignored) {}
                runBg(host, new Runnable() {
                    @Override public void run() {
                        PortKnockEngine.get().knock(cfg.toolsKnockHost, cfg.toolsKnockSequence,
                                cfg.toolsKnockProto, cfg.toolsKnockDelayMs, "SKID");
                    }
                }, SUB_KNOCK);
            }
        });
        col.addView(goBtn);
        LinearLayout card = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        List<PortKnockEngine.Result> rs = PortKnockEngine.get().lastResults();
        if (rs.isEmpty()) {
            card.addView(UiFactory.label(ctx, "No knock yet.", 12.5f, UiFactory.COLOR_MUTED, false, false));
        } else {
            for (int i = 0; i < rs.size(); i++) {
                if (i > 0) card.addView(UiFactory.divider(ctx));
                PortKnockEngine.Result r = rs.get(i);
                card.addView(UiFactory.label(ctx, r.step + "  " + (r.ok ? "ok" : "fail"),
                        13.5f, r.ok ? UiFactory.COLOR_EMERALD : UiFactory.COLOR_CRIMSON, true, false));
                card.addView(UiFactory.label(ctx, r.detail, 12f, UiFactory.COLOR_MUTED, false, true));
            }
        }
        col.addView(card);
        sv.addView(col);
        return sv;
    }

    private static View buildChecker(final Context ctx, final ConfigureScreens.HostCallbacks host) {
        ScrollView sv = wrap(ctx);
        LinearLayout col = inner(ctx);
        final SkidConfig cfg = host.getConfig();
        col.addView(back(ctx, host, "Port checker", "TCP/UDP range on one host, max 4096 ports"));
        col.addView(UiFactory.label(ctx,
                "Inspired by CodeDead/Advanced-PortChecker-android. Named ports come from the Open Port catalog.",
                12f, UiFactory.COLOR_MUTED, false, false));
        final EditText hostEt = UiFactory.monoInput(ctx, cfg.toolsCheckHost, "127.0.0.1", false);
        col.addView(labeled(ctx, "Host", hostEt));
        final EditText fromEt = numbered(ctx, String.valueOf(cfg.toolsCheckFrom), "1");
        col.addView(labeled(ctx, "From", fromEt));
        final EditText toEt = numbered(ctx, String.valueOf(cfg.toolsCheckTo), "1024");
        col.addView(labeled(ctx, "To", toEt));
        final EditText protoEt = UiFactory.monoInput(ctx, cfg.toolsCheckProto, "TCP or UDP", false);
        col.addView(labeled(ctx, "Proto", protoEt));
        Button goBtn = UiFactory.actionButton(ctx,
                PortCheckerEngine.get().isRunning() ? "Scanning..." : "Check ports",
                UiFactory.COLOR_SKY, UiFactory.COLOR_BG, UiFactory.COLOR_SKY);
        goBtn.setEnabled(!PortCheckerEngine.get().isRunning());
        goBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                cfg.toolsCheckHost = hostEt.getText().toString().trim();
                try { cfg.toolsCheckFrom = Integer.parseInt(fromEt.getText().toString().trim()); } catch (Exception ignored) {}
                try { cfg.toolsCheckTo = Integer.parseInt(toEt.getText().toString().trim()); } catch (Exception ignored) {}
                cfg.toolsCheckProto = protoEt.getText().toString().trim();
                final boolean udp = cfg.toolsCheckProto.toUpperCase().startsWith("U");
                runBg(host, new Runnable() {
                    @Override public void run() {
                        PortCheckerEngine.get().scan(cfg.toolsCheckHost, cfg.toolsCheckFrom, cfg.toolsCheckTo, udp, 280);
                    }
                }, SUB_PORTCHECK);
            }
        });
        col.addView(goBtn);
        col.addView(UiFactory.label(ctx, PortCheckerEngine.get().summary(), 12.5f, UiFactory.COLOR_EMERALD, true, false));
        LinearLayout card = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        List<PortCheckerEngine.Hit> hits = PortCheckerEngine.get().lastHits();
        if (hits.isEmpty()) {
            card.addView(UiFactory.label(ctx, "Closed ports are hidden. Open/filtered show here.",
                    12.5f, UiFactory.COLOR_MUTED, false, false));
        } else {
            int cap = Math.min(hits.size(), 200);
            for (int i = 0; i < cap; i++) {
                if (i > 0) card.addView(UiFactory.divider(ctx));
                PortCheckerEngine.Hit h = hits.get(i);
                String title = h.port + "/" + h.proto + "  " + h.state + "  " + h.name;
                card.addView(UiFactory.label(ctx, title, 13.5f,
                        "open".equals(h.state) ? UiFactory.COLOR_EMERALD : UiFactory.COLOR_AMBER, true, false));
                if (h.banner.length() > 0) {
                    card.addView(UiFactory.label(ctx, h.banner, 12f, UiFactory.COLOR_MUTED, false, true));
                }
            }
            if (hits.size() > cap) {
                card.addView(UiFactory.label(ctx, "+" + (hits.size() - cap) + " more",
                        12f, UiFactory.COLOR_MUTED, false, false));
            }
        }
        col.addView(card);
        sv.addView(col);
        return sv;
    }

    private static View buildMirror(final Context ctx, final ConfigureScreens.HostCallbacks host) {
        ScrollView sv = wrap(ctx);
        LinearLayout col = inner(ctx);
        final SkidConfig cfg = host.getConfig();
        col.addView(back(ctx, host, "Folder mirror", "One-way copy: local, ftp://, sftp:, http://"));
        col.addView(UiFactory.label(ctx,
                "Inspired by He11PC/android-mirror. SMB/NFS need extra libs and are not in this APK. SFTP uses the Shellwave password.",
                12f, UiFactory.COLOR_MUTED, false, false));
        if (cfg.toolsMirrorSrc == null || cfg.toolsMirrorSrc.isEmpty()) {
            cfg.toolsMirrorSrc = ftpRoot(ctx).getAbsolutePath();
        }
        final EditText srcEt = UiFactory.monoInput(ctx, cfg.toolsMirrorSrc, "/path/on/phone", false);
        col.addView(labeled(ctx, "Source folder", srcEt));
        final EditText dstEt = UiFactory.monoInput(ctx, cfg.toolsMirrorDst, "ftp://192.168.1.2/backup or /storage/...", true);
        col.addView(labeled(ctx, "Destination", dstEt));
        Button goBtn = UiFactory.actionButton(ctx, "Mirror now", UiFactory.COLOR_SKY, UiFactory.COLOR_BG, UiFactory.COLOR_SKY);
        goBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                cfg.toolsMirrorSrc = srcEt.getText().toString().trim();
                cfg.toolsMirrorDst = dstEt.getText().toString().trim();
                runBg(host, new Runnable() {
                    @Override public void run() {
                        MirrorEngine.get().run(cfg.toolsMirrorSrc, cfg.toolsMirrorDst,
                                cfg.toolsFtpsUser, cfg.toolsFtpsPass,
                                cfg.toolsSshHost, cfg.toolsSshPort, cfg.toolsSshUser, cfg.toolsSshPass);
                    }
                }, SUB_MIRROR);
            }
        });
        col.addView(goBtn);
        LinearLayout out = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        out.addView(UiFactory.label(ctx, MirrorEngine.get().lastOut(), 12f, UiFactory.COLOR_TEXT, false, true));
        col.addView(out);
        sv.addView(col);
        return sv;
    }

    // ---- helpers -------------------------------------------------------

    private static void runBg(final ConfigureScreens.HostCallbacks host, final Runnable work, final int backTo) {
        host.persistAndReload("tools");
        host.getBgExecutor().execute(new Runnable() {
            @Override public void run() {
                try { work.run(); } catch (Exception ignored) {}
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override public void run() {
                        host.openConfigureSubScreen(backTo);
                    }
                });
            }
        });
    }

    private static View.OnClickListener go(final ConfigureScreens.HostCallbacks host, final int id) {
        return new View.OnClickListener() {
            @Override public void onClick(View v) { host.openConfigureSubScreen(id); }
        };
    }

    private static View back(final Context ctx, final ConfigureScreens.HostCallbacks host,
                             String title, String sub) {
        return UiFactory.backHeader(ctx, title, sub, new View.OnClickListener() {
            @Override public void onClick(View v) {
                host.openConfigureSubScreen(SUB_TOOLS_HUB);
            }
        });
    }

    private static ScrollView wrap(Context ctx) {
        ScrollView sv = new ScrollView(ctx);
        sv.setFillViewport(true);
        return sv;
    }

    private static LinearLayout inner(Context ctx) {
        LinearLayout col = new LinearLayout(ctx);
        col.setOrientation(LinearLayout.VERTICAL);
        int pad = UiFactory.dp(ctx, 14);
        col.setPadding(pad, pad, pad, UiFactory.dp(ctx, 24));
        return col;
    }

    private static LinearLayout row(Context ctx) {
        LinearLayout r = new LinearLayout(ctx);
        r.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = UiFactory.dp(ctx, 8);
        lp.bottomMargin = UiFactory.dp(ctx, 8);
        r.setLayoutParams(lp);
        return r;
    }

    private static LinearLayout.LayoutParams btnLp(Context ctx, float w) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, w);
        lp.rightMargin = UiFactory.dp(ctx, 6);
        return lp;
    }

    private static View labeled(Context ctx, String title, View field) {
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = UiFactory.dp(ctx, 8);
        box.setLayoutParams(lp);
        box.addView(UiFactory.label(ctx, title, 11.5f, UiFactory.COLOR_MUTED, true, false));
        box.addView(field);
        return box;
    }

    private static EditText numbered(Context ctx, String v, String hint) {
        EditText et = UiFactory.monoInput(ctx, v, hint, false);
        et.setInputType(InputType.TYPE_CLASS_NUMBER);
        return et;
    }

    private static File ftpRoot(Context ctx) {
        File f = new File(ctx.getFilesDir(), "ftp-root");
        if (!f.exists()) f.mkdirs();
        return f;
    }

    private static String empty(String s, String fallback) {
        return s == null || s.trim().isEmpty() ? fallback : s.trim();
    }

    private static void saveFtps(SkidConfig cfg, EditText portEt, EditText userEt, EditText passEt) {
        try { cfg.toolsFtpsPort = Integer.parseInt(portEt.getText().toString().trim()); } catch (Exception ignored) {}
        cfg.toolsFtpsUser = userEt.getText().toString().trim();
        cfg.toolsFtpsPass = passEt.getText().toString();
    }

    private static void saveSsh(SkidConfig cfg, EditText hostEt, EditText portEt,
                                EditText userEt, EditText passEt, EditText cmdEt) {
        cfg.toolsSshHost = hostEt.getText().toString().trim();
        try { cfg.toolsSshPort = Integer.parseInt(portEt.getText().toString().trim()); } catch (Exception ignored) {}
        cfg.toolsSshUser = userEt.getText().toString().trim();
        cfg.toolsSshPass = passEt.getText().toString();
        cfg.toolsSshCommand = cmdEt.getText().toString();
    }
}
