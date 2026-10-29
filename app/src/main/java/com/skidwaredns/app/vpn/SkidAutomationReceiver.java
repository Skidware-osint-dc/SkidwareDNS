package com.skidwaredns.app.vpn;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import com.skidwaredns.app.config.SkidConfig;
import com.skidwaredns.app.config.SkidConfigStore;
import com.skidwaredns.app.util.SkidEventBus;

/**
 * Handles:
 *  1. InviZible-compatible Shell Script Control broadcasts:
 *     am broadcast -a com.skidwaredns.app.SHELL_SCRIPT_CONTROL --ei dnscrypt 1 --ei tor 1 --ei i2p 1 --ei vpn 1
 *     (also accepts pan.alexander.tordnscrypt.SHELL_SCRIPT_CONTROL when Enable-Script-Control=true)
 *  2. Fast Settings Autostart on Device Boot (Autostart DNSCrypt / Tor / I2P / VPN with configurable delay)
 */
public final class SkidAutomationReceiver extends BroadcastReceiver {

    public static final String ACTION_SKID_SCRIPT = "com.skidwaredns.app.SHELL_SCRIPT_CONTROL";
    public static final String ACTION_INVIZIBLE_SCRIPT = "pan.alexander.tordnscrypt.SHELL_SCRIPT_CONTROL";

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (context == null || intent == null || intent.getAction() == null) return;
        String action = intent.getAction();
        final SkidConfig cfg = SkidConfigStore.getActive(context);

        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            if (!cfg.autostartDnscrypt && !cfg.autostartTor && !cfg.autostartI2p && !cfg.autostartVpn) {
                return;
            }
            if (cfg.autostartDnscrypt) cfg.dnscryptModuleEnabled = true;
            if (cfg.autostartTor) cfg.torEnabled = true;
            if (cfg.autostartI2p) cfg.i2pEnabled = true;
            SkidConfigStore.saveActive(context, cfg);

            long delayMs = Math.max(0, cfg.autostartDelaySec) * 1000L;
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (VpnService.prepare(context) == null) {
                        startEngine(context, SkidVpnService.ACTION_START);
                        SkidEventBus.logSystem("SYSTEM", "Autostart", "Boot autostart engaged after " + cfg.autostartDelaySec + "s");
                    }
                }
            }, delayMs);
            return;
        }

        if (ACTION_SKID_SCRIPT.equals(action) || ACTION_INVIZIBLE_SCRIPT.equals(action)) {
            if (!cfg.enableScriptControl) {
                SkidEventBus.logSystem("SYSTEM", "ScriptControl", "Rejected broadcast (Enable script control is OFF)");
                return;
            }
            int dc = intent.getIntExtra("dnscrypt", -1);
            int tor = intent.getIntExtra("tor", -1);
            int i2p = intent.getIntExtra("i2p", -1);
            int vpn = intent.getIntExtra("vpn", -1);

            if (dc == 0 || dc == 1) cfg.dnscryptModuleEnabled = (dc == 1);
            if (tor == 0 || tor == 1) cfg.torEnabled = (tor == 1);
            if (i2p == 0 || i2p == 1) cfg.i2pEnabled = (i2p == 1);
            SkidConfigStore.saveActive(context, cfg);

            boolean anyStart = (dc == 1 || tor == 1 || i2p == 1 || vpn == 1);
            boolean allStop = (vpn == 0) || (dc == 0 && tor == 0 && i2p == 0);
            if (allStop && SkidEventBus.isRunning()) {
                context.startService(new Intent(context, SkidVpnService.class).setAction(SkidVpnService.ACTION_STOP));
            } else if (anyStart && VpnService.prepare(context) == null) {
                startEngine(context, SkidEventBus.isRunning() ? SkidVpnService.ACTION_RELOAD : SkidVpnService.ACTION_START);
            }
            SkidEventBus.logSystem("SYSTEM", "ScriptControl",
                    "Applied broadcast: dnscrypt=" + dc + " tor=" + tor + " i2p=" + i2p + " vpn=" + vpn);
        }
    }

    private static void startEngine(Context ctx, String action) {
        Intent svc = new Intent(ctx, SkidVpnService.class).setAction(action);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ctx.startForegroundService(svc);
        } else {
            ctx.startService(svc);
        }
    }

    public static String buildExampleShellScript() {
        return "# Start DNSCrypt + Tor + Purple I2P + VPN via ADB / Termux / Tasker:\n"
                + "am broadcast -a com.skidwaredns.app.SHELL_SCRIPT_CONTROL --ei dnscrypt 1 --ei tor 1 --ei i2p 1 --ei vpn 1\n"
                + "# InviZible Pro legacy broadcast alias also supported:\n"
                + "am broadcast -a pan.alexander.tordnscrypt.SHELL_SCRIPT_CONTROL --ei dnscrypt 1 --ei tor 1 --ei i2p 0";
    }
}
