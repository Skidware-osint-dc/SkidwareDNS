package com.skidwaredns.app.vpn;

import android.app.PendingIntent;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import com.skidwaredns.app.config.SkidConfigStore;
import com.skidwaredns.app.ui.MainActivity;
import com.skidwaredns.app.util.SkidEventBus;

/**
 * Android Quick Settings shade tile (RethinkDNS / strongSwan / Nym style) for 1-tap Start/Resume/Stop.
 */
public final class SkidTileService extends TileService {

    @Override
    public void onStartListening() {
        super.onStartListening();
        syncTileState();
    }

    @Override
    public void onClick() {
        super.onClick();
        if (SkidEventBus.isPaused()) {
            Intent resume = new Intent(this, SkidVpnService.class).setAction(SkidVpnService.ACTION_RESUME);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(resume);
            } else {
                startService(resume);
            }
            syncTileState();
            return;
        }
        if (SkidEventBus.isRunning()) {
            Intent stop = new Intent(this, SkidVpnService.class).setAction(SkidVpnService.ACTION_STOP);
            startService(stop);
            syncTileState();
            return;
        }

        Intent prep = VpnService.prepare(this);
        if (prep == null) {
            Intent start = new Intent(this, SkidVpnService.class).setAction(SkidVpnService.ACTION_START);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(start);
            } else {
                startService(start);
            }
            syncTileState();
        } else {
            Intent open = new Intent(this, MainActivity.class);
            open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (Build.VERSION.SDK_INT >= 34) {
                PendingIntent pi = PendingIntent.getActivity(
                        this, 99, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                startActivityAndCollapse(pi);
            } else {
                startActivityAndCollapse(open);
            }
        }
    }

    private void syncTileState() {
        Tile tile = getQsTile();
        if (tile == null) return;
        boolean paused = SkidEventBus.isPaused();
        boolean active = SkidEventBus.isRunning() && !paused;
        tile.setState(active ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.setLabel("SkidwareDNS");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.setSubtitle(active ? SkidConfigStore.getActive(this).name : (paused ? "Paused" : "Not Protected"));
        }
        tile.updateTile();
    }
}
