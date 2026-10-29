package com.skidwaredns.app.ui;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.skidwaredns.app.config.SkidConfig;
import com.skidwaredns.app.dns.DnsPacketCodec;
import com.skidwaredns.app.dns.DnsUpstreamResolver;
import com.skidwaredns.app.model.DnsPresets;
import com.skidwaredns.app.tunnel.TunnelEngine;
import com.skidwaredns.app.tunnel.XrayUriAndRouteHelper;

import java.util.List;

/**
 * SkidwareDNS v4.1 ("HTTP Injector + MegaForge Edition") UI Screens:
 *  - SUB_TUNNEL_TYPE_MATRIX  (15): 2-Column Tunnel Type + Connect From Matrix + Selected Tunnel Banner + Custom Payload (184206–184228)
 *  - SUB_SSH_SETTINGS        (16): SSH Host/Port, /root-skidware Auth, Private Key lock/modal, Local Port, UDPGW, Buffer Size & Header Replacer / Route Proxy modal with (i) tooltips (184256–184315)
 *  - SUB_V2RAY_XRAY          (17): Xray/V2Ray Core, Protocol (6), Server Cipher + Transport Security (TLS/Reality), 8 Network Types, Header Obfs, Mux, Domain Strategy, Regional Routing & URI Import/Export (184448–184701)
 *  - SUB_HYSTERIA_SETTINGS   (18): Hysteria v1/v2, UDP/faketcp, Auth/Obfs, SNI/ALPN/Allow Insecure/Certificates, Port Hopping, Up/Down Mbps, QUIC Receive Window Slider (8MB) & RFC 8899 (184721–184728)
 *  - SUB_INJECTOR_TUNNEL_HUB (19): Global Tunnel Hub, Apps Filter, VPN DNS, DNS Forwarding, Use Default Route, Custom/Excluded Routes, HTTP Ping + URL, 14-Provider DoH, UI Dark Mode/Language & Vibrate (184356–184413, 184849)
 *  - SUB_SHADOWSOCKS_SETTINGS(20): Dedicated Shadowsocks & SS-2022 Configurator: Server (Obfs), Port, Password (+ PSK Gen), 11 Encrypt Methods (incl. 2022-blake3-*), ss:// URI Import/Export & JSON Preview (184826–184834)
 */
public final class HttpInjectorScreens {

    public static final int SUB_TUNNEL_TYPE_MATRIX = 15;
    public static final int SUB_SSH_SETTINGS = 16;
    public static final int SUB_V2RAY_XRAY = 17;
    public static final int SUB_HYSTERIA_SETTINGS = 18;
    public static final int SUB_INJECTOR_TUNNEL_HUB = 19;
    public static final int SUB_SHADOWSOCKS_SETTINGS = 20;

    private static final int COLOR_TEAL_BANNER = Color.parseColor("#164353");
    private static final int COLOR_BADGE_BLUE = Color.parseColor("#1D9BF0");
    private static final int COLOR_DIMMED = Color.parseColor("#526075");

    private HttpInjectorScreens() {}

    // =========================================================================
    // 15. 2-COLUMN TUNNEL TYPE + CONNECT FROM MATRIX (184206–184228)
    // =========================================================================
    public static View buildTunnelTypeMatrixScreen(final Context ctx, final ConfigureScreens.HostCallbacks host) {
        ScrollView sv = wrapScroll(ctx);
        LinearLayout col = innerCol(ctx);
        final SkidConfig cfg = host.getConfig();

        col.addView(UiFactory.backHeader(ctx, "Tunnel Type",
                "Select Tunnel Type, Connect From transport & Custom Payload",
                new View.OnClickListener() {
                    @Override public void onClick(View v) { host.openConfigureSubScreen(SUB_INJECTOR_TUNNEL_HUB); }
                }));

        LinearLayout selBanner = new LinearLayout(ctx);
        selBanner.setOrientation(LinearLayout.VERTICAL);
        int bPad = UiFactory.dp(ctx, 16);
        selBanner.setPadding(bPad, bPad, bPad, bPad);
        selBanner.setBackground(UiFactory.roundedRect(ctx, COLOR_TEAL_BANNER, UiFactory.COLOR_SKY, 1f, 14f));
        LinearLayout.LayoutParams sbLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sbLp.bottomMargin = UiFactory.dp(ctx, 14);
        selBanner.setLayoutParams(sbLp);

        LinearLayout topHdr = new LinearLayout(ctx);
        topHdr.setOrientation(LinearLayout.HORIZONTAL);
        topHdr.setGravity(Gravity.CENTER_VERTICAL);
        TextView selCap = UiFactory.label(ctx, "Selected Tunnel Type", 13.5f, UiFactory.COLOR_TEXT, true, false);
        selCap.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView resetBtn = UiFactory.label(ctx, "↺ Reset", 12f, UiFactory.COLOR_SKY, true, false);
        resetBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cfg.tunnel = "SSH";
                cfg.connectFromMode = "Http proxy";
                cfg.customPayloadEnabled = false;
                host.persistAndReload("Reset Tunnel Type to HTTP Proxy -> SSH");
                host.openConfigureSubScreen(SUB_TUNNEL_TYPE_MATRIX);
            }
        });
        topHdr.addView(selCap);
        topHdr.addView(resetBtn);
        selBanner.addView(topHdr);

        TextView comboTv = UiFactory.label(ctx, XrayUriAndRouteHelper.formatSelectedTunnelHeadline(cfg),
                17f, UiFactory.COLOR_TEXT, false, false);
        LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cLp.topMargin = UiFactory.dp(ctx, 4);
        cLp.bottomMargin = UiFactory.dp(ctx, 8);
        comboTv.setLayoutParams(cLp);
        selBanner.addView(comboTv);

        TextView protoPill = UiFactory.label(ctx, XrayUriAndRouteHelper.formatSelectedTunnelBadge(cfg),
                11f, Color.WHITE, true, true);
        protoPill.setPadding(UiFactory.dp(ctx, 9), UiFactory.dp(ctx, 3), UiFactory.dp(ctx, 9), UiFactory.dp(ctx, 3));
        protoPill.setBackground(UiFactory.roundedRect(ctx, COLOR_BADGE_BLUE, Color.TRANSPARENT, 0f, 6f));
        protoPill.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        selBanner.addView(protoPill);
        col.addView(selBanner);

        LinearLayout tunCard = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        tunCard.addView(UiFactory.label(ctx, "Tunnel Type", 15f, UiFactory.COLOR_TEXT, true, false));

        final String[][] tunnelRows = new String[][]{
                {"SSH", "Secure Shell (SSH)", "V2Ray/Xray", "V2Ray/Xray"},
                {"Hysteria2", "Hysteria", "Shadowsocks", "Shadowsocks"},
                {"WireGuard", "WireGuard", "MASQUE-H3", "MASQUE (HTTP/3)"}
        };
        for (final String[] r : tunnelRows) {
            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, UiFactory.dp(ctx, 8), 0, UiFactory.dp(ctx, 4));
            row.addView(buildRadioCell(ctx, r[1], isTunnelMatch(cfg.tunnel, r[0]), true, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    cfg.tunnel = r[0];
                    if (!XrayUriAndRouteHelper.isConnectFromAllowed(cfg.tunnel, cfg.connectFromMode)) {
                        cfg.connectFromMode = "Direct";
                    }
                    if (!XrayUriAndRouteHelper.isCustomPayloadAllowed(cfg.tunnel, cfg.connectFromMode)) {
                        cfg.customPayloadEnabled = false;
                    }
                    host.openConfigureSubScreen(SUB_TUNNEL_TYPE_MATRIX);
                }
            }));
            row.addView(buildRadioCell(ctx, r[3], isTunnelMatch(cfg.tunnel, r[2]), true, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    cfg.tunnel = r[2];
                    if (!XrayUriAndRouteHelper.isConnectFromAllowed(cfg.tunnel, cfg.connectFromMode)) {
                        cfg.connectFromMode = "Direct";
                    }
                    if (!XrayUriAndRouteHelper.isCustomPayloadAllowed(cfg.tunnel, cfg.connectFromMode)) {
                        cfg.customPayloadEnabled = false;
                    }
                    host.openConfigureSubScreen(SUB_TUNNEL_TYPE_MATRIX);
                }
            }));
            tunCard.addView(row);
        }
        col.addView(tunCard);

        LinearLayout cfCard = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        cfCard.addView(UiFactory.label(ctx, "Connect From", 15f, UiFactory.COLOR_TEXT, true, false));

        final String[][] cfRows = new String[][]{
                {"Direct", "None (Direct)", "DNS (DNSTT)", "DNS (DNSTT)"},
                {"Http proxy", "HTTP Proxy", "HTTP (Obfs)", "HTTP (Obfs)"},
                {"TLS/SSL Proxy", "TLS/SSL Proxy", "TLS/SSL (Obfs)", "TLS/SSL (Obfs)"},
                {"TLS/SSL (stunnel)", "TLS/SSL (stunnel)", "HTTPS Jump", "HTTPS Jump"}
        };
        for (final String[] r : cfRows) {
            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, UiFactory.dp(ctx, 8), 0, UiFactory.dp(ctx, 4));

            final boolean enLeft = XrayUriAndRouteHelper.isConnectFromAllowed(cfg.tunnel, r[0]);
            row.addView(buildRadioCell(ctx, r[1], r[0].equalsIgnoreCase(cfg.connectFromMode), enLeft, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!enLeft) return;
                    cfg.connectFromMode = r[0];
                    if (!XrayUriAndRouteHelper.isCustomPayloadAllowed(cfg.tunnel, cfg.connectFromMode)) {
                        cfg.customPayloadEnabled = false;
                    }
                    host.openConfigureSubScreen(SUB_TUNNEL_TYPE_MATRIX);
                }
            }));

            final boolean enRight = XrayUriAndRouteHelper.isConnectFromAllowed(cfg.tunnel, r[2]);
            row.addView(buildRadioCell(ctx, r[3], r[2].equalsIgnoreCase(cfg.connectFromMode), enRight, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!enRight) return;
                    cfg.connectFromMode = r[2];
                    if (!XrayUriAndRouteHelper.isCustomPayloadAllowed(cfg.tunnel, cfg.connectFromMode)) {
                        cfg.customPayloadEnabled = false;
                    }
                    host.openConfigureSubScreen(SUB_TUNNEL_TYPE_MATRIX);
                }
            }));
            cfCard.addView(row);
        }
        col.addView(cfCard);

        LinearLayout optCard = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        optCard.addView(UiFactory.label(ctx, "Options", 15f, UiFactory.COLOR_TEXT, true, false));
        final boolean payloadAllowed = XrayUriAndRouteHelper.isCustomPayloadAllowed(cfg.tunnel, cfg.connectFromMode);

        final CheckBox payloadCb = new CheckBox(ctx);
        payloadCb.setText("  Custom Payload");
        payloadCb.setTextColor(payloadAllowed ? UiFactory.COLOR_TEXT : COLOR_DIMMED);
        payloadCb.setEnabled(payloadAllowed);
        payloadCb.setChecked(payloadAllowed && cfg.customPayloadEnabled);
        payloadCb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cfg.customPayloadEnabled = payloadCb.isChecked();
                host.openConfigureSubScreen(SUB_TUNNEL_TYPE_MATRIX);
            }
        });
        optCard.addView(payloadCb);

        final EditText payloadEt = UiFactory.monoInput(ctx, cfg.httpPayload,
                "CONNECT [host_port] HTTP/1.1[crlf]Host: [sni][crlf][crlf]", true);
        if (payloadAllowed && cfg.customPayloadEnabled) {
            optCard.addView(UiFactory.label(ctx, "Payload Template ([host_port], [sni], [crlf], [ua]):",
                    11.5f, UiFactory.COLOR_SKY, true, false));
            optCard.addView(payloadEt);
        }
        col.addView(optCard);

        Button saveBtn = UiFactory.actionButton(ctx, "S A V E",
                UiFactory.COLOR_SKY, UiFactory.COLOR_BG, UiFactory.COLOR_SKY);
        saveBtn.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, UiFactory.dp(ctx, 48)));
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (payloadAllowed && cfg.customPayloadEnabled) {
                    cfg.httpPayload = payloadEt.getText().toString().trim();
                }
                host.persistAndReload("Saved Tunnel Type: " + XrayUriAndRouteHelper.formatSelectedTunnelHeadline(cfg));
                Toast.makeText(ctx, "Saved: " + XrayUriAndRouteHelper.formatSelectedTunnelHeadline(cfg), Toast.LENGTH_SHORT).show();
                host.openConfigureSubScreen(SUB_INJECTOR_TUNNEL_HUB);
            }
        });
        col.addView(saveBtn);

        sv.addView(col);
        return sv;
    }

    private static boolean isTunnelMatch(String current, String target) {
        if (current == null) return false;
        if ("Hysteria2".equalsIgnoreCase(target) && "Hysteria".equalsIgnoreCase(current)) return true;
        return current.equalsIgnoreCase(target);
    }

    private static View buildRadioCell(Context ctx, String label, boolean selected, boolean enabled, View.OnClickListener onClick) {
        LinearLayout cell = new LinearLayout(ctx);
        cell.setOrientation(LinearLayout.HORIZONTAL);
        cell.setGravity(Gravity.CENTER_VERTICAL);
        cell.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        cell.setPadding(UiFactory.dp(ctx, 4), UiFactory.dp(ctx, 6), UiFactory.dp(ctx, 4), UiFactory.dp(ctx, 6));
        if (enabled) cell.setOnClickListener(onClick);

        int dotColor = !enabled ? COLOR_DIMMED : (selected ? UiFactory.COLOR_SKY : UiFactory.COLOR_MUTED);
        int txtColor = !enabled ? COLOR_DIMMED : UiFactory.COLOR_TEXT;

        TextView dot = UiFactory.label(ctx, selected ? "◉" : "◯", 18f, dotColor, true, false);
        LinearLayout.LayoutParams dLp = new LinearLayout.LayoutParams(UiFactory.dp(ctx, 26), ViewGroup.LayoutParams.WRAP_CONTENT);
        dot.setLayoutParams(dLp);

        TextView tv = UiFactory.label(ctx, label, 13.5f, txtColor, selected, false);
        cell.addView(dot);
        cell.addView(tv);
        return cell;
    }

    // =========================================================================
    // 16. SSH SETTINGS + ADVANCED MODAL (184256, 184303, 184315)
    // =========================================================================
    public static View buildSshSettingsScreen(final Context ctx, final ConfigureScreens.HostCallbacks host) {
        ScrollView sv = wrapScroll(ctx);
        LinearLayout col = innerCol(ctx);
        final SkidConfig cfg = host.getConfig();

        col.addView(UiFactory.backHeader(ctx, "SSH Settings",
                "SSH Tunnel, Account (/root-skidware), Private Key, UDPGW & Header Replacer",
                new View.OnClickListener() {
                    @Override public void onClick(View v) { host.openConfigureSubScreen(SUB_INJECTOR_TUNNEL_HUB); }
                }));

        col.addView(UiFactory.categoryTitle(ctx, "SSH TUNNEL SETTINGS"));
        LinearLayout hostCard = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        hostCard.addView(UiFactory.iconNavRow(ctx, "☁", "SSH Host",
                cfg.tunnelHost.isEmpty() ? "The host name of SSH service" : cfg.tunnelHost,
                UiFactory.COLOR_SKY, new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        promptInput(ctx, "SSH Host", cfg.tunnelHost, new InputCb() {
                            @Override public void onVal(String s) {
                                cfg.tunnelHost = s.trim();
                                cfg.tunnel = "SSH";
                                host.persistAndReload("SSH Host=" + cfg.tunnelHost);
                                host.openConfigureSubScreen(SUB_SSH_SETTINGS);
                            }
                        });
                    }
                }));
        hostCard.addView(UiFactory.divider(ctx));
        hostCard.addView(UiFactory.iconNavRow(ctx, "#", "SSH Port",
                "Port " + cfg.tunnelPort + " — The port number of your SSH service",
                UiFactory.COLOR_MUTED, new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        promptInput(ctx, "SSH Port", String.valueOf(cfg.tunnelPort), new InputCb() {
                            @Override public void onVal(String s) {
                                try { cfg.tunnelPort = Integer.parseInt(s.trim()); } catch (Exception ignored) {}
                                host.persistAndReload("SSH Port=" + cfg.tunnelPort);
                                host.openConfigureSubScreen(SUB_SSH_SETTINGS);
                            }
                        });
                    }
                }));
        col.addView(hostCard);

        col.addView(UiFactory.categoryTitle(ctx, "ACCOUNT INFORMATION"));
        LinearLayout accCard = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        accCard.addView(UiFactory.iconNavRow(ctx, "👤", "Username",
                cfg.tunnelUser.isEmpty() ? "/root-skidware" : cfg.tunnelUser,
                UiFactory.COLOR_TEXT, new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        promptInput(ctx, "SSH Username", cfg.tunnelUser, new InputCb() {
                            @Override public void onVal(String s) {
                                cfg.tunnelUser = s.trim();
                                host.persistAndReload("SSH User=" + cfg.tunnelUser);
                                host.openConfigureSubScreen(SUB_SSH_SETTINGS);
                            }
                        });
                    }
                }));
        accCard.addView(UiFactory.divider(ctx));
        accCard.addView(UiFactory.iconNavRow(ctx, "🔑", "Password / Passphrase",
                cfg.tunnelAuth.isEmpty() ? "(not set)" : "*********",
                UiFactory.COLOR_MUTED, new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        promptInput(ctx, "SSH Password / Passphrase", cfg.tunnelAuth, new InputCb() {
                            @Override public void onVal(String s) {
                                cfg.tunnelAuth = s;
                                host.persistAndReload("SSH Password updated");
                                host.openConfigureSubScreen(SUB_SSH_SETTINGS);
                            }
                        });
                    }
                }));
        accCard.addView(UiFactory.divider(ctx));
        accCard.addView(UiFactory.iconSwitchRow(ctx, "🗝", "Private Key (Optional)",
                "Use private key to authenticate instead of password",
                cfg.sshUsePrivateKey, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean c) {
                        cfg.sshUsePrivateKey = c;
                        host.persistAndReload("SSH Private Key=" + c);
                        host.openConfigureSubScreen(SUB_SSH_SETTINGS);
                    }
                }));

        View setKeyRow = UiFactory.iconNavRow(ctx, "✎", "Set Private Key",
                !cfg.sshUsePrivateKey ? "Enable Private Key switch above to configure"
                        : (cfg.sshPrivateKey.isEmpty() ? "Paste OpenSSH / PEM / Ed25519 key" : "Private key configured (" + cfg.sshPrivateKey.length() + " chars)"),
                cfg.sshUsePrivateKey ? UiFactory.COLOR_SKY : COLOR_DIMMED,
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        if (!cfg.sshUsePrivateKey) {
                            Toast.makeText(ctx, "Turn on Private Key (Optional) first", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        final EditText et = UiFactory.monoInput(ctx, cfg.sshPrivateKey, "-----BEGIN OPENSSH PRIVATE KEY-----", true);
                        et.setMinLines(6);
                        new AlertDialog.Builder(ctx)
                                .setTitle("Set SSH Private Key")
                                .setView(et)
                                .setPositiveButton("Save Key", new DialogInterface.OnClickListener() {
                                    @Override public void onClick(DialogInterface d, int w) {
                                        cfg.sshPrivateKey = et.getText().toString().trim();
                                        host.persistAndReload("SSH Private Key saved");
                                        host.openConfigureSubScreen(SUB_SSH_SETTINGS);
                                    }
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                    }
                });
        setKeyRow.setAlpha(cfg.sshUsePrivateKey ? 1.0f : 0.48f);
        accCard.addView(setKeyRow);
        col.addView(accCard);

        // Connection Section (184256)
        col.addView(UiFactory.categoryTitle(ctx, "CONNECTION"));
        LinearLayout connCard = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        connCard.addView(UiFactory.iconNavRow(ctx, "⇄", "Local Port", String.valueOf(cfg.sshLocalPort),
                UiFactory.COLOR_TEXT, new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        promptInput(ctx, "SSH Local Port", String.valueOf(cfg.sshLocalPort), new InputCb() {
                            @Override public void onVal(String s) {
                                try { cfg.sshLocalPort = Integer.parseInt(s.trim()); } catch (Exception ignored) {}
                                host.persistAndReload("SSH Local Port=" + cfg.sshLocalPort);
                                host.openConfigureSubScreen(SUB_SSH_SETTINGS);
                            }
                        });
                    }
                }));
        connCard.addView(UiFactory.divider(ctx));
        connCard.addView(UiFactory.iconNavRow(ctx, "⚡", "UDPGW Port",
                cfg.sshUdpgwPort > 0 ? String.valueOf(cfg.sshUdpgwPort) : "(tap to set badvpn-udpgw port, e.g. 7300)",
                UiFactory.COLOR_SKY, new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        promptInput(ctx, "UDPGW Port (e.g. 7300)", String.valueOf(cfg.sshUdpgwPort), new InputCb() {
                            @Override public void onVal(String s) {
                                try { cfg.sshUdpgwPort = Integer.parseInt(s.trim()); } catch (Exception ignored) {}
                                host.persistAndReload("UDPGW Port=" + cfg.sshUdpgwPort);
                                host.openConfigureSubScreen(SUB_SSH_SETTINGS);
                            }
                        });
                    }
                }));
        col.addView(connCard);

        // Advanced Settings Section (184256, 184315)
        col.addView(UiFactory.categoryTitle(ctx, "ADVANCED SETTINGS"));
        LinearLayout advCard = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        advCard.addView(UiFactory.iconNavRow(ctx, "▤", "Buffer Size",
                "Send: " + cfg.sshSendBufferBytes + " | Receive: " + cfg.sshRecvBufferBytes,
                UiFactory.COLOR_MUTED, new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        promptInput(ctx, "Send,Receive Buffer Bytes", cfg.sshSendBufferBytes + "," + cfg.sshRecvBufferBytes, new InputCb() {
                            @Override public void onVal(String s) {
                                String[] p = s.split("[,:\\s]+");
                                if (p.length >= 2) {
                                    try {
                                        cfg.sshSendBufferBytes = Integer.parseInt(p[0].trim());
                                        cfg.sshRecvBufferBytes = Integer.parseInt(p[1].trim());
                                    } catch (Exception ignored) {}
                                }
                                host.persistAndReload("SSH Buffers updated");
                                host.openConfigureSubScreen(SUB_SSH_SETTINGS);
                            }
                        });
                    }
                }));
        advCard.addView(UiFactory.divider(ctx));
        advCard.addView(UiFactory.iconNavRow(ctx, "⚙", "Advanced Settings",
                "Settings or beta features for advanced user (Header Replacer=" + cfg.sshHeaderReplacer + ", Route Proxy=" + cfg.sshRouteProxy + ")",
                UiFactory.COLOR_SKY, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        LinearLayout box = new LinearLayout(ctx);
                        box.setOrientation(LinearLayout.VERTICAL);
                        int pad = UiFactory.dp(ctx, 16);
                        box.setPadding(pad, pad, pad, pad);
                        box.setBackgroundColor(UiFactory.COLOR_CARD);

                        box.addView(UiFactory.iconSwitchRow(ctx, "ⓘ", "Header Replacer",
                                "Replace header status to 200 OK (rewrites 301/302/403/101 proxy status lines to HTTP/1.1 200 OK)",
                                cfg.sshHeaderReplacer, new UiFactory.ToggleListener() {
                                    @Override public void onToggled(boolean c) { cfg.sshHeaderReplacer = c; }
                                }));
                        box.addView(UiFactory.divider(ctx));
                        box.addView(UiFactory.iconSwitchRow(ctx, "ⓘ", "Route Proxy",
                                "Route remote proxy via local IP (prevents routing loops on tethered / LAN upstream proxies)",
                                cfg.sshRouteProxy, new UiFactory.ToggleListener() {
                                    @Override public void onToggled(boolean c) { cfg.sshRouteProxy = c; }
                                }));

                        new AlertDialog.Builder(ctx)
                                .setTitle("Advanced Settings")
                                .setView(box)
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override public void onClick(DialogInterface d, int w) {
                                        host.persistAndReload("SSH Advanced updated");
                                        host.openConfigureSubScreen(SUB_SSH_SETTINGS);
                                    }
                                })
                                .show();
                    }
                }));
        col.addView(advCard);

        sv.addView(col);
        return sv;
    }

    // =========================================================================
    // 17. FULL V2RAY / XRAY CONFIGURATOR + URI IMPORT/EXPORT (184448–184701)
    // =========================================================================
    public static View buildV2rayXrayScreen(final Context ctx, final ConfigureScreens.HostCallbacks host) {
        ScrollView sv = wrapScroll(ctx);
        LinearLayout col = innerCol(ctx);
        final SkidConfig cfg = host.getConfig();

        col.addView(UiFactory.backHeader(ctx, "V2Ray/Xray",
                "VMess / VLESS / Trojan / SS / Socks / WG, XHTTP / gRPC / WS, Reality, Mux & Routing",
                new View.OnClickListener() {
                    @Override public void onClick(View v) { host.openConfigureSubScreen(SUB_INJECTOR_TUNNEL_HUB); }
                }));

        LinearLayout uriBar = new LinearLayout(ctx);
        uriBar.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams ubLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ubLp.bottomMargin = UiFactory.dp(ctx, 12);
        uriBar.setLayoutParams(ubLp);

        Button btnImportUri = UiFactory.actionButton(ctx, "Import URI", UiFactory.COLOR_SURFACE, UiFactory.COLOR_SKY, UiFactory.COLOR_SKY);
        Button btnJson = UiFactory.actionButton(ctx, "[+] Xray JSON", UiFactory.COLOR_SURFACE, UiFactory.COLOR_TEXT, UiFactory.COLOR_BORDER);
        Button btnCopyUri = UiFactory.actionButton(ctx, "🔗 Copy URI", UiFactory.COLOR_SURFACE, UiFactory.COLOR_EMERALD, UiFactory.COLOR_EMERALD);
        LinearLayout.LayoutParams wLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        wLp.rightMargin = UiFactory.dp(ctx, 6);
        btnImportUri.setLayoutParams(wLp);
        btnJson.setLayoutParams(wLp);
        btnCopyUri.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        uriBar.addView(btnImportUri);
        uriBar.addView(btnJson);
        uriBar.addView(btnCopyUri);
        col.addView(uriBar);

        btnImportUri.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                promptInput(ctx, "Paste vless://, vmess://, trojan://, ss://, or hy2:// URI", "", new InputCb() {
                    @Override public void onVal(String s) {
                        try {
                            String msg = XrayUriAndRouteHelper.importShareUri(s, cfg);
                            host.persistAndReload(msg);
                            Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show();
                            host.openConfigureSubScreen(SUB_V2RAY_XRAY);
                        } catch (Exception e) {
                            Toast.makeText(ctx, e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });

        btnJson.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cfg.tunnel = "V2Ray/Xray";
                final EditText et = UiFactory.monoInput(ctx, TunnelEngine.generateNativeTunnelSnippet(cfg), "", true);
                et.setMinLines(12);
                new AlertDialog.Builder(ctx)
                        .setTitle("Generated Xray JSON Config")
                        .setView(et)
                        .setPositiveButton("Close", null)
                        .show();
            }
        });

        btnCopyUri.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cfg.tunnel = "V2Ray/Xray";
                String uri = XrayUriAndRouteHelper.exportShareUri(cfg);
                copyClip(ctx, "Xray URI", uri);
            }
        });

        LinearLayout coreCard = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        coreCard.addView(UiFactory.iconNavRow(ctx, "❖", "Core", cfg.xrayCore, UiFactory.COLOR_SKY,
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        showRadioDialog(ctx, "Core", DnsPresets.XRAY_CORES, cfg.xrayCore, new InputCb() {
                            @Override public void onVal(String val) {
                                cfg.xrayCore = val;
                                host.persistAndReload("Xray Core=" + val);
                                host.openConfigureSubScreen(SUB_V2RAY_XRAY);
                            }
                        });
                    }
                }));
        coreCard.addView(UiFactory.divider(ctx));
        coreCard.addView(UiFactory.iconNavRow(ctx, "⇄", "Protocol", cfg.xrayProtocol, UiFactory.COLOR_EMERALD,
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        showRadioDialog(ctx, "Protocol", DnsPresets.XRAY_PROTOCOLS, cfg.xrayProtocol, new InputCb() {
                            @Override public void onVal(String val) {
                                cfg.xrayProtocol = val;
                                cfg.tunnel = "V2Ray/Xray";
                                host.persistAndReload("Xray Protocol=" + val);
                                host.openConfigureSubScreen(SUB_V2RAY_XRAY);
                            }
                        });
                    }
                }));
        col.addView(coreCard);

        // Server Card (Host, Server Port, User ID, Alter ID, Security — exact match to 184448)
        col.addView(UiFactory.categoryTitle(ctx, "SERVER"));
        LinearLayout srvCard = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        srvCard.addView(UiFactory.iconNavRow(ctx, "☁", "Host",
                cfg.tunnelHost.isEmpty() ? "(tap to set)" : cfg.tunnelHost, UiFactory.COLOR_TEXT,
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        promptInput(ctx, "Xray Server Host", cfg.tunnelHost, new InputCb() {
                            @Override public void onVal(String s) {
                                cfg.tunnelHost = s.trim();
                                host.persistAndReload("Xray Host=" + cfg.tunnelHost);
                                host.openConfigureSubScreen(SUB_V2RAY_XRAY);
                            }
                        });
                    }
                }));
        srvCard.addView(UiFactory.divider(ctx));
        srvCard.addView(UiFactory.iconNavRow(ctx, "#", "Server Port", String.valueOf(cfg.tunnelPort), UiFactory.COLOR_MUTED,
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        promptInput(ctx, "Server Port", String.valueOf(cfg.tunnelPort), new InputCb() {
                            @Override public void onVal(String s) {
                                try { cfg.tunnelPort = Integer.parseInt(s.trim()); } catch (Exception ignored) {}
                                host.persistAndReload("Xray Port=" + cfg.tunnelPort);
                                host.openConfigureSubScreen(SUB_V2RAY_XRAY);
                            }
                        });
                    }
                }));
        srvCard.addView(UiFactory.divider(ctx));
        srvCard.addView(UiFactory.iconNavRow(ctx, "🆔", "User ID", cfg.xrayUuid, UiFactory.COLOR_MUTED,
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        promptInput(ctx, "User ID / UUID", cfg.xrayUuid, new InputCb() {
                            @Override public void onVal(String s) {
                                cfg.xrayUuid = s.trim();
                                host.persistAndReload("Xray UUID updated");
                                host.openConfigureSubScreen(SUB_V2RAY_XRAY);
                            }
                        });
                    }
                }));
        srvCard.addView(UiFactory.divider(ctx));
        srvCard.addView(UiFactory.iconNavRow(ctx, "±", "Alter ID", String.valueOf(cfg.xrayAlterId), UiFactory.COLOR_MUTED,
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        promptInput(ctx, "Alter ID (0 for AEAD)", String.valueOf(cfg.xrayAlterId), new InputCb() {
                            @Override public void onVal(String s) {
                                try { cfg.xrayAlterId = Integer.parseInt(s.trim()); } catch (Exception ignored) {}
                                host.persistAndReload("Alter ID=" + cfg.xrayAlterId);
                                host.openConfigureSubScreen(SUB_V2RAY_XRAY);
                            }
                        });
                    }
                }));
        srvCard.addView(UiFactory.divider(ctx));
        srvCard.addView(UiFactory.iconNavRow(ctx, "🛡", "Security (Payload Cipher)", cfg.xrayServerCipher, UiFactory.COLOR_MUTED,
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        showRadioDialog(ctx, "Server Security", DnsPresets.XRAY_SERVER_CIPHERS, cfg.xrayServerCipher, new InputCb() {
                            @Override public void onVal(String val) {
                                cfg.xrayServerCipher = val;
                                host.persistAndReload("Server Security=" + val);
                                host.openConfigureSubScreen(SUB_V2RAY_XRAY);
                            }
                        });
                    }
                }));
        col.addView(srvCard);

        // Transport (184448, 184535, 184613)
        col.addView(UiFactory.categoryTitle(ctx, "TRANSPORT"));
        LinearLayout trCard = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        trCard.addView(UiFactory.iconNavRow(ctx, "⇄", "Network Type", cfg.xrayNetworkType, UiFactory.COLOR_SKY,
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        showRadioDialog(ctx, "Network Type", DnsPresets.XRAY_NETWORK_TYPES, cfg.xrayNetworkType, new InputCb() {
                            @Override public void onVal(String val) {
                                cfg.xrayNetworkType = val;
                                cfg.v2rayTransport = XrayUriAndRouteHelper.mapDisplayNetToShort(val);
                                host.persistAndReload("Network Type=" + val);
                                host.openConfigureSubScreen(SUB_V2RAY_XRAY);
                            }
                        });
                    }
                }));
        trCard.addView(UiFactory.divider(ctx));
        trCard.addView(UiFactory.iconNavRow(ctx, "▤", "Header Type (obfuscation)", cfg.xrayHeaderType, UiFactory.COLOR_MUTED,
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        showRadioDialog(ctx, "Header Type (obfuscation)", DnsPresets.XRAY_HEADER_TYPES, cfg.xrayHeaderType, new InputCb() {
                            @Override public void onVal(String val) {
                                cfg.xrayHeaderType = val;
                                host.persistAndReload("Header Type=" + val);
                                host.openConfigureSubScreen(SUB_V2RAY_XRAY);
                            }
                        });
                    }
                }));
        col.addView(trCard);

        // Transport Layer Encryption (184606, 184618)
        col.addView(UiFactory.categoryTitle(ctx, "TRANSPORT LAYER ENCRYPTION"));
        LinearLayout tleCard = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        tleCard.addView(UiFactory.iconNavRow(ctx, "🔒", "Security", cfg.xraySecurity, UiFactory.COLOR_EMERALD,
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        showRadioDialog(ctx, "Security", DnsPresets.XRAY_SECURITIES, cfg.xraySecurity, new InputCb() {
                            @Override public void onVal(String val) {
                                cfg.xraySecurity = val;
                                host.persistAndReload("Transport Security=" + val);
                                host.openConfigureSubScreen(SUB_V2RAY_XRAY);
                            }
                        });
                    }
                }));
        if ("TLS".equalsIgnoreCase(cfg.xraySecurity) || "Reality".equalsIgnoreCase(cfg.xraySecurity)) {
            tleCard.addView(UiFactory.divider(ctx));
            tleCard.addView(UiFactory.iconNavRow(ctx, "🌐", "SNI (Server Name)", cfg.sni.isEmpty() ? "(tap to set)" : cfg.sni,
                    UiFactory.COLOR_SKY, new View.OnClickListener() {
                        @Override public void onClick(View v) {
                            promptInput(ctx, "Server Name Indication (SNI)", cfg.sni, new InputCb() {
                                @Override public void onVal(String s) {
                                    cfg.sni = s.trim();
                                    host.persistAndReload("SNI=" + cfg.sni);
                                    host.openConfigureSubScreen(SUB_V2RAY_XRAY);
                                }
                            });
                        }
                    }));
        }
        if ("Reality".equalsIgnoreCase(cfg.xraySecurity)) {
            tleCard.addView(UiFactory.divider(ctx));
            tleCard.addView(UiFactory.iconNavRow(ctx, "🔑", "Reality PublicKey & ShortId",
                    "pbk=" + (cfg.xrayRealityPubKey.isEmpty() ? "(unset)" : cfg.xrayRealityPubKey) + " • sid=" + cfg.xrayRealityShortId,
                    UiFactory.COLOR_SKY, new View.OnClickListener() {
                        @Override public void onClick(View v) {
                            promptInput(ctx, "Reality PublicKey,ShortId", cfg.xrayRealityPubKey + "," + cfg.xrayRealityShortId, new InputCb() {
                                @Override public void onVal(String s) {
                                    String[] p = s.split(",", -1);
                                    cfg.xrayRealityPubKey = p[0].trim();
                                    if (p.length > 1) cfg.xrayRealityShortId = p[1].trim();
                                    host.persistAndReload("Reality keys updated");
                                    host.openConfigureSubScreen(SUB_V2RAY_XRAY);
                                }
                            });
                        }
                    }));
        }
        col.addView(tleCard);

        // Multiplexing & Routing (exact Mux note from 184606 + 184650 + 184701)
        col.addView(UiFactory.categoryTitle(ctx, "MULTIPLEXING"));
        LinearLayout muxCard = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        muxCard.addView(UiFactory.iconSwitchRow(ctx, "⧉", "Mux",
                "Use one physical TCP connections for multiple virtual TCP connections.\n" +
                        "Mux is designed to reduce TCP handshake latency. It is NOT for high throughput. " +
                        "When used for downloading large files or speed measurement, Mux is usually SLOWER than a normal TCP connection.",
                cfg.xrayMuxEnabled, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean c) {
                        cfg.xrayMuxEnabled = c;
                        host.persistAndReload("Xray Mux=" + c);
                    }
                }));
        col.addView(muxCard);

        col.addView(UiFactory.categoryTitle(ctx, "ROUTING"));
        LinearLayout rtCard = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        rtCard.addView(UiFactory.iconNavRow(ctx, "🌐", "Domain Strategy", cfg.xrayDomainStrategy, UiFactory.COLOR_SKY,
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        showRadioDialog(ctx, "Domain Strategy", DnsPresets.XRAY_DOMAIN_STRATEGIES, cfg.xrayDomainStrategy, new InputCb() {
                            @Override public void onVal(String val) {
                                cfg.xrayDomainStrategy = val;
                                host.persistAndReload("Domain Strategy=" + val);
                                host.openConfigureSubScreen(SUB_V2RAY_XRAY);
                            }
                        });
                    }
                }));
        rtCard.addView(UiFactory.divider(ctx));
        rtCard.addView(UiFactory.iconNavRow(ctx, "↪", "Routing Rules", cfg.xrayRoutingRules, UiFactory.COLOR_EMERALD,
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        showRadioDialog(ctx, "Routing Rules", DnsPresets.XRAY_ROUTING_RULES, cfg.xrayRoutingRules, new InputCb() {
                            @Override public void onVal(String val) {
                                cfg.xrayRoutingRules = val;
                                host.persistAndReload("Routing Rules=" + val);
                                host.openConfigureSubScreen(SUB_V2RAY_XRAY);
                            }
                        });
                    }
                }));
        col.addView(rtCard);

        sv.addView(col);
        return sv;
    }

    // =========================================================================
    // 18. FULL HYSTERIA v1 / v2 CONFIGURATOR (184721, 184728)
    // =========================================================================
    public static View buildHysteriaScreen(final Context ctx, final ConfigureScreens.HostCallbacks host) {
        ScrollView sv = wrapScroll(ctx);
        LinearLayout col = innerCol(ctx);
        final SkidConfig cfg = host.getConfig();

        col.addView(UiFactory.backHeader(ctx, "Hysteria",
                "Version 1/2, Auth/Obfs, SNI/ALPN/Allow Insecure/Certificates, Port Hopping, QUIC Window & RFC 8899",
                new View.OnClickListener() {
                    @Override public void onClick(View v) { host.openConfigureSubScreen(SUB_INJECTOR_TUNNEL_HUB); }
                }));

        // Top Bar matching [+] and [?] icons in 184721 / 184728
        LinearLayout topBar = new LinearLayout(ctx);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams tbLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tbLp.bottomMargin = UiFactory.dp(ctx, 12);
        topBar.setLayoutParams(tbLp);

        Button btnCfg = UiFactory.actionButton(ctx, "[+] Hysteria Config", UiFactory.COLOR_SURFACE, UiFactory.COLOR_SKY, UiFactory.COLOR_SKY);
        Button btnHyUri = UiFactory.actionButton(ctx, "🔗 Copy hy2://", UiFactory.COLOR_SURFACE, UiFactory.COLOR_EMERALD, UiFactory.COLOR_EMERALD);
        Button btnHelp = UiFactory.actionButton(ctx, "[?] Help", UiFactory.COLOR_SURFACE, UiFactory.COLOR_TEXT, UiFactory.COLOR_BORDER);
        LinearLayout.LayoutParams wLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        wLp.rightMargin = UiFactory.dp(ctx, 6);
        btnCfg.setLayoutParams(wLp);
        btnHyUri.setLayoutParams(wLp);
        btnHelp.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.7f));
        topBar.addView(btnCfg);
        topBar.addView(btnHyUri);
        topBar.addView(btnHelp);
        col.addView(topBar);

        btnCfg.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                cfg.tunnel = cfg.hysteriaVersion == 1 ? "Hysteria" : "Hysteria2";
                final EditText et = UiFactory.monoInput(ctx, TunnelEngine.generateNativeTunnelSnippet(cfg), "", true);
                et.setMinLines(12);
                new AlertDialog.Builder(ctx)
                        .setTitle("Hysteria v" + cfg.hysteriaVersion + " Native Config")
                        .setView(et)
                        .setPositiveButton("Close", null)
                        .show();
            }
        });
        btnHyUri.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                cfg.tunnel = "Hysteria2";
                copyClip(ctx, "Hysteria2 URI", XrayUriAndRouteHelper.exportShareUri(cfg));
            }
        });
        btnHelp.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                new AlertDialog.Builder(ctx)
                        .setTitle("Hysteria QUIC Guide")
                        .setMessage("• Version 2 uses HTTP/3 + Brutal congestion control + Salamander UDP obfuscation.\n" +
                                "• Version 1 supports UDP, faketcp, and wechat-video.\n" +
                                "• QUIC Receive Window Size defaults to 8 MB; increase to 16–32 MB on high-BDP fiber links.\n" +
                                "• Enable Disable Path MTU Discovery (RFC 8899) if middleboxes drop oversized UDP probes.")
                        .setPositiveButton("Got it", null)
                        .show();
            }
        });

        LinearLayout verCard = UiFactory.card(ctx, UiFactory.COLOR_SKY);
        verCard.addView(UiFactory.iconNavRow(ctx, "⚡", "Version", String.valueOf(cfg.hysteriaVersion),
                UiFactory.COLOR_SKY, new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        showRadioDialog(ctx, "Hysteria Version", DnsPresets.HYSTERIA_VERSIONS, String.valueOf(cfg.hysteriaVersion), new InputCb() {
                            @Override public void onVal(String val) {
                                cfg.hysteriaVersion = "1".equals(val) ? 1 : 2;
                                cfg.tunnel = cfg.hysteriaVersion == 1 ? "Hysteria" : "Hysteria2";
                                host.persistAndReload("Hysteria Version=" + cfg.hysteriaVersion);
                                host.openConfigureSubScreen(SUB_HYSTERIA_SETTINGS);
                            }
                        });
                    }
                }));
        col.addView(verCard);

        col.addView(UiFactory.categoryTitle(ctx, "SERVER & AUTHENTICATION"));
        LinearLayout srvCard = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        srvCard.addView(UiFactory.iconNavRow(ctx, "☁", "Host",
                cfg.tunnelHost.isEmpty() ? "(tap to set)" : cfg.tunnelHost, UiFactory.COLOR_TEXT,
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        promptInput(ctx, "Hysteria Host", cfg.tunnelHost, new InputCb() {
                            @Override public void onVal(String s) {
                                cfg.tunnelHost = s.trim();
                                host.persistAndReload("Hysteria Host=" + cfg.tunnelHost);
                                host.openConfigureSubScreen(SUB_HYSTERIA_SETTINGS);
                            }
                        });
                    }
                }));
        srvCard.addView(UiFactory.divider(ctx));
        srvCard.addView(UiFactory.iconNavRow(ctx, "#", "Server Port",
                cfg.tunnelPort + " (Hop Range: " + cfg.hysteriaPortHopRange + ")", UiFactory.COLOR_MUTED,
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        promptInput(ctx, "Port or Hop Range (e.g. 20000-50000)", cfg.hysteriaPortHopRange, new InputCb() {
                            @Override public void onVal(String s) {
                                cfg.hysteriaPortHopRange = s.trim();
                                host.persistAndReload("Hysteria Ports=" + cfg.hysteriaPortHopRange);
                                host.openConfigureSubScreen(SUB_HYSTERIA_SETTINGS);
                            }
                        });
                    }
                }));
        srvCard.addView(UiFactory.divider(ctx));
        srvCard.addView(UiFactory.iconNavRow(ctx, "⇄", "Protocol", cfg.hysteriaProtocol, UiFactory.COLOR_SKY,
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        showRadioDialog(ctx, "Hysteria Protocol", DnsPresets.HYSTERIA_PROTOCOLS, cfg.hysteriaProtocol, new InputCb() {
                            @Override public void onVal(String val) {
                                cfg.hysteriaProtocol = val;
                                host.persistAndReload("Hysteria Protocol=" + val);
                                host.openConfigureSubScreen(SUB_HYSTERIA_SETTINGS);
                            }
                        });
                    }
                }));
        srvCard.addView(UiFactory.divider(ctx));
        srvCard.addView(UiFactory.iconNavRow(ctx, "🔑", "Authentication Password",
                cfg.tunnelAuth.isEmpty() ? "(not set)" : "*********", UiFactory.COLOR_TEXT,
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        promptInput(ctx, "Authentication Password", cfg.tunnelAuth, new InputCb() {
                            @Override public void onVal(String s) {
                                cfg.tunnelAuth = s.trim();
                                host.persistAndReload("Hysteria Auth updated");
                                host.openConfigureSubScreen(SUB_HYSTERIA_SETTINGS);
                            }
                        });
                    }
                }));
        srvCard.addView(UiFactory.divider(ctx));
        srvCard.addView(UiFactory.iconNavRow(ctx, "🎭", "Obfuscation Password",
                cfg.hysteriaObfsPassword.isEmpty() ? "(none)" : cfg.hysteriaObfsPassword, UiFactory.COLOR_MUTED,
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        promptInput(ctx, "Obfuscation Password", cfg.hysteriaObfsPassword, new InputCb() {
                            @Override public void onVal(String s) {
                                cfg.hysteriaObfsPassword = s.trim();
                                host.persistAndReload("Hysteria Obfs updated");
                                host.openConfigureSubScreen(SUB_HYSTERIA_SETTINGS);
                            }
                        });
                    }
                }));
        col.addView(srvCard);

        // Transport Layer Encryption including Certificates row (184721, 184728)
        col.addView(UiFactory.categoryTitle(ctx, "TRANSPORT LAYER ENCRYPTION"));
        LinearLayout tlsCard = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        tlsCard.addView(UiFactory.iconNavRow(ctx, "🌐", "Server Name Indication (SNI)",
                cfg.sni.isEmpty() ? "-" : cfg.sni, UiFactory.COLOR_SKY,
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        promptInput(ctx, "Server Name Indication (SNI)", cfg.sni, new InputCb() {
                            @Override public void onVal(String s) {
                                cfg.sni = s.trim();
                                host.persistAndReload("SNI=" + cfg.sni);
                                host.openConfigureSubScreen(SUB_HYSTERIA_SETTINGS);
                            }
                        });
                    }
                }));
        tlsCard.addView(UiFactory.divider(ctx));
        tlsCard.addView(UiFactory.iconNavRow(ctx, "⧉", "Application-Layer Protocol Negotiation (ALPN)",
                cfg.hysteriaAlpn, UiFactory.COLOR_MUTED,
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        promptInput(ctx, "ALPN (e.g. h3)", cfg.hysteriaAlpn, new InputCb() {
                            @Override public void onVal(String s) {
                                cfg.hysteriaAlpn = s.trim();
                                host.persistAndReload("ALPN=" + cfg.hysteriaAlpn);
                                host.openConfigureSubScreen(SUB_HYSTERIA_SETTINGS);
                            }
                        });
                    }
                }));
        tlsCard.addView(UiFactory.divider(ctx));
        tlsCard.addView(UiFactory.iconSwitchRow(ctx, "⚠", "Allow Insecure",
                "Allows insecure connection at TLS client, e.g., TLS server uses unverifiable certificates",
                cfg.hysteriaAllowInsecure, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean c) {
                        cfg.hysteriaAllowInsecure = c;
                        host.persistAndReload("Allow Insecure=" + c);
                        host.openConfigureSubScreen(SUB_HYSTERIA_SETTINGS);
                    }
                }));
        tlsCard.addView(UiFactory.divider(ctx));
        View certRow = UiFactory.iconNavRow(ctx, "📜", "Certificates",
                cfg.hysteriaCertificates == null || cfg.hysteriaCertificates.isEmpty() ? "-" : cfg.hysteriaCertificates,
                cfg.hysteriaAllowInsecure ? COLOR_DIMMED : UiFactory.COLOR_SKY,
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        promptInput(ctx, "Custom CA Certificate Path / Pin (- for system)", cfg.hysteriaCertificates, new InputCb() {
                            @Override public void onVal(String s) {
                                cfg.hysteriaCertificates = s.trim().isEmpty() ? "-" : s.trim();
                                host.persistAndReload("Hysteria Certificates=" + cfg.hysteriaCertificates);
                                host.openConfigureSubScreen(SUB_HYSTERIA_SETTINGS);
                            }
                        });
                    }
                });
        certRow.setAlpha(cfg.hysteriaAllowInsecure ? 0.5f : 1.0f);
        tlsCard.addView(certRow);
        col.addView(tlsCard);

        col.addView(UiFactory.categoryTitle(ctx, "CONNECTION"));
        LinearLayout connCard = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        connCard.addView(UiFactory.iconNavRow(ctx, "⏱", "Port Hopping Interval (second)",
                cfg.hysteriaHopIntervalSec + " s", UiFactory.COLOR_TEXT,
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        promptInput(ctx, "Port Hopping Interval (seconds)", String.valueOf(cfg.hysteriaHopIntervalSec), new InputCb() {
                            @Override public void onVal(String s) {
                                try { cfg.hysteriaHopIntervalSec = Integer.parseInt(s.trim()); } catch (Exception ignored) {}
                                host.persistAndReload("Hop Interval=" + cfg.hysteriaHopIntervalSec + "s");
                                host.openConfigureSubScreen(SUB_HYSTERIA_SETTINGS);
                            }
                        });
                    }
                }));
        connCard.addView(UiFactory.divider(ctx));
        connCard.addView(UiFactory.iconNavRow(ctx, "↑", "Max Upload Speed (Mbps)",
                cfg.hysteriaUpMbps + " Mbps", UiFactory.COLOR_EMERALD,
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        promptInput(ctx, "Max Upload Speed (Mbps)", String.valueOf(cfg.hysteriaUpMbps), new InputCb() {
                            @Override public void onVal(String s) {
                                try { cfg.hysteriaUpMbps = Integer.parseInt(s.trim()); } catch (Exception ignored) {}
                                host.persistAndReload("Up Mbps=" + cfg.hysteriaUpMbps);
                                host.openConfigureSubScreen(SUB_HYSTERIA_SETTINGS);
                            }
                        });
                    }
                }));
        connCard.addView(UiFactory.divider(ctx));
        connCard.addView(UiFactory.iconNavRow(ctx, "↓", "Max Download Speed (Mbps)",
                cfg.hysteriaDownMbps + " Mbps", UiFactory.COLOR_SKY,
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        promptInput(ctx, "Max Download Speed (Mbps)", String.valueOf(cfg.hysteriaDownMbps), new InputCb() {
                            @Override public void onVal(String s) {
                                try { cfg.hysteriaDownMbps = Integer.parseInt(s.trim()); } catch (Exception ignored) {}
                                host.persistAndReload("Down Mbps=" + cfg.hysteriaDownMbps);
                                host.openConfigureSubScreen(SUB_HYSTERIA_SETTINGS);
                            }
                        });
                    }
                }));
        connCard.addView(UiFactory.divider(ctx));

        final int[] winSteps = new int[]{2, 4, 8, 16, 24, 32, 48, 64};
        int initWin = 2;
        for (int i = 0; i < winSteps.length; i++) {
            if (cfg.hysteriaQuicRcvWinMb <= winSteps[i]) { initWin = i; break; }
        }
        connCard.addView(UiFactory.iconSliderRow(ctx, "▤", "QUIC Receive Window Size",
                "Initial QUIC receive window size (in Megabyte, MB). Don't change these values unless you understand what you're doing.",
                winSteps.length, initWin,
                new UiFactory.SliderValueFormatter() {
                    @Override public String format(int idx) { return String.valueOf(winSteps[idx]); }
                },
                new UiFactory.SliderChangeListener() {
                    @Override public void onStepSelected(int idx) {
                        cfg.hysteriaQuicRcvWinMb = winSteps[idx];
                        host.persistAndReload("QUIC Receive Window=" + cfg.hysteriaQuicRcvWinMb + " MB");
                    }
                }));
        connCard.addView(UiFactory.divider(ctx));

        connCard.addView(UiFactory.iconSwitchRow(ctx, "⊘", "Disable Path MTU Discovery (RFC 8899)",
                "Prevent QUIC packet drops on blackhole PMTU links",
                cfg.hysteriaDisableMtuDiscovery, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean c) {
                        cfg.hysteriaDisableMtuDiscovery = c;
                        host.persistAndReload("Disable PMTU=" + c);
                    }
                }));
        col.addView(connCard);

        sv.addView(col);
        return sv;
    }

    // =========================================================================
    // 20. DEDICATED SHADOWSOCKS & SS-2022 SCREEN (184826, 184834)
    // =========================================================================
    public static View buildShadowsocksScreen(final Context ctx, final ConfigureScreens.HostCallbacks host) {
        ScrollView sv = wrapScroll(ctx);
        LinearLayout col = innerCol(ctx);
        final SkidConfig cfg = host.getConfig();

        col.addView(UiFactory.backHeader(ctx, "Shadowsocks",
                "AEAD & Shadowsocks-2022 BLAKE3 Ciphers, SIP003 Server (Obfs) & ss:// URI Share",
                new View.OnClickListener() {
                    @Override public void onClick(View v) { host.openConfigureSubScreen(SUB_INJECTOR_TUNNEL_HUB); }
                }));

        // Top Bar matching [+] and [link] icons in 184826
        LinearLayout ssBar = new LinearLayout(ctx);
        ssBar.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams sbLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sbLp.bottomMargin = UiFactory.dp(ctx, 12);
        ssBar.setLayoutParams(sbLp);

        Button btnImportSs = UiFactory.actionButton(ctx, "Import ss://", UiFactory.COLOR_SURFACE, UiFactory.COLOR_SKY, UiFactory.COLOR_SKY);
        Button btnJsonSs = UiFactory.actionButton(ctx, "[+] SS JSON", UiFactory.COLOR_SURFACE, UiFactory.COLOR_TEXT, UiFactory.COLOR_BORDER);
        Button btnShareSs = UiFactory.actionButton(ctx, "🔗 Copy ss://", UiFactory.COLOR_SURFACE, UiFactory.COLOR_EMERALD, UiFactory.COLOR_EMERALD);
        LinearLayout.LayoutParams wLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        wLp.rightMargin = UiFactory.dp(ctx, 6);
        btnImportSs.setLayoutParams(wLp);
        btnJsonSs.setLayoutParams(wLp);
        btnShareSs.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        ssBar.addView(btnImportSs);
        ssBar.addView(btnJsonSs);
        ssBar.addView(btnShareSs);
        col.addView(ssBar);

        btnImportSs.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                promptInput(ctx, "Paste ss:// URI (SIP002 / SIP003 / SS-2022)", "", new InputCb() {
                    @Override public void onVal(String s) {
                        try {
                            String msg = XrayUriAndRouteHelper.importShareUri(s, cfg);
                            host.persistAndReload(msg);
                            Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show();
                            host.openConfigureSubScreen(SUB_SHADOWSOCKS_SETTINGS);
                        } catch (Exception e) {
                            Toast.makeText(ctx, e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });
        btnJsonSs.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                cfg.tunnel = "Shadowsocks";
                final EditText et = UiFactory.monoInput(ctx, TunnelEngine.generateNativeTunnelSnippet(cfg), "", true);
                et.setMinLines(10);
                new AlertDialog.Builder(ctx)
                        .setTitle("Shadowsocks / SS-2022 Config")
                        .setView(et)
                        .setPositiveButton("Close", null)
                        .show();
            }
        });
        btnShareSs.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                cfg.tunnel = "Shadowsocks";
                copyClip(ctx, "Shadowsocks URI", XrayUriAndRouteHelper.exportShareUri(cfg));
            }
        });

        // Settings Card (Server (Obfs), Port, Password, Encrypt Method — exact match to 184826 & 184834)
        col.addView(UiFactory.categoryTitle(ctx, "SETTINGS"));
        LinearLayout ssCard = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        String srvSummary = (cfg.tunnelHost.isEmpty() ? "(tap to set server)" : cfg.tunnelHost)
                + (cfg.ssObfsServer.isEmpty() ? "" : " • " + cfg.ssObfsServer);
        ssCard.addView(UiFactory.iconNavRow(ctx, "☁", "Server (Obfs)", srvSummary, UiFactory.COLOR_SKY,
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        String cur = cfg.tunnelHost + (cfg.ssObfsServer.isEmpty() ? "" : "|" + cfg.ssObfsServer);
                        promptInput(ctx, "Server Host (optional: |obfs=tls;obfs-host=bing.com)", cur, new InputCb() {
                            @Override public void onVal(String s) {
                                int bar = s.indexOf('|');
                                if (bar >= 0) {
                                    cfg.tunnelHost = s.substring(0, bar).trim();
                                    cfg.ssObfsServer = s.substring(bar + 1).trim();
                                } else {
                                    cfg.tunnelHost = s.trim();
                                }
                                cfg.tunnel = "Shadowsocks";
                                host.persistAndReload("Shadowsocks Server=" + cfg.tunnelHost);
                                host.openConfigureSubScreen(SUB_SHADOWSOCKS_SETTINGS);
                            }
                        });
                    }
                }));
        ssCard.addView(UiFactory.divider(ctx));
        ssCard.addView(UiFactory.iconNavRow(ctx, "#", "Port", String.valueOf(cfg.tunnelPort), UiFactory.COLOR_TEXT,
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        promptInput(ctx, "Shadowsocks Port", String.valueOf(cfg.tunnelPort), new InputCb() {
                            @Override public void onVal(String s) {
                                try { cfg.tunnelPort = Integer.parseInt(s.trim()); } catch (Exception ignored) {}
                                cfg.tunnel = "Shadowsocks";
                                host.persistAndReload("Shadowsocks Port=" + cfg.tunnelPort);
                                host.openConfigureSubScreen(SUB_SHADOWSOCKS_SETTINGS);
                            }
                        });
                    }
                }));
        ssCard.addView(UiFactory.divider(ctx));
        ssCard.addView(UiFactory.iconNavRow(ctx, "🔑", "Password",
                cfg.tunnelAuth.isEmpty() ? "(tap to set password or Base64 PSK)" : "*********",
                UiFactory.COLOR_TEXT, new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        promptInput(ctx, "Shadowsocks Password / Base64 PSK", cfg.tunnelAuth, new InputCb() {
                            @Override public void onVal(String s) {
                                cfg.tunnelAuth = s.trim();
                                cfg.tunnel = "Shadowsocks";
                                host.persistAndReload("Shadowsocks Password updated");
                                host.openConfigureSubScreen(SUB_SHADOWSOCKS_SETTINGS);
                            }
                        });
                    }
                }));
        ssCard.addView(UiFactory.divider(ctx));
        ssCard.addView(UiFactory.iconNavRow(ctx, "🔒", "Encrypt Method", cfg.tunnelCipher, UiFactory.COLOR_EMERALD,
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        showRadioDialog(ctx, "Encrypt Method", DnsPresets.SHADOWSOCKS_ENCRYPT_METHODS, cfg.tunnelCipher, new InputCb() {
                            @Override public void onVal(String val) {
                                cfg.tunnelCipher = val;
                                cfg.tunnel = "Shadowsocks";
                                host.persistAndReload("Encrypt Method=" + val);
                                host.openConfigureSubScreen(SUB_SHADOWSOCKS_SETTINGS);
                            }
                        });
                    }
                }));

        if (cfg.tunnelCipher != null && cfg.tunnelCipher.startsWith("2022-blake3-")) {
            ssCard.addView(UiFactory.divider(ctx));
            Button genPsk = UiFactory.actionButton(ctx,
                    "Generate Base64 PSK for " + cfg.tunnelCipher,
                    UiFactory.COLOR_SURFACE, UiFactory.COLOR_SKY, UiFactory.COLOR_SKY);
            genPsk.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    cfg.tunnelAuth = XrayUriAndRouteHelper.generateShadowsocks2022Psk(cfg.tunnelCipher);
                    host.persistAndReload("Generated SS-2022 Base64 PSK");
                    copyClip(ctx, "SS-2022 PSK", cfg.tunnelAuth);
                    host.openConfigureSubScreen(SUB_SHADOWSOCKS_SETTINGS);
                }
            });
            ssCard.addView(genPsk);
        }
        col.addView(ssCard);

        sv.addView(col);
        return sv;
    }

    // =========================================================================
    // 19. HTTP INJECTOR GLOBAL SETTINGS HUB (184356, 184409, 184413, 184849)
    // =========================================================================
    public static View buildInjectorHubScreen(final Context ctx, final ConfigureScreens.HostCallbacks host) {
        ScrollView sv = wrapScroll(ctx);
        LinearLayout col = innerCol(ctx);
        final SkidConfig cfg = host.getConfig();

        col.addView(UiFactory.backHeader(ctx, "Settings (HTTP Injector)",
                XrayUriAndRouteHelper.formatSelectedTunnelHeadline(cfg) + " [" + XrayUriAndRouteHelper.formatSelectedTunnelBadge(cfg) + "]",
                new View.OnClickListener() {
                    @Override public void onClick(View v) { host.openConfigureSubScreen(ConfigureScreens.SUB_HUB); }
                }));

        // Tunnel Section (184413)
        col.addView(UiFactory.categoryTitle(ctx, "TUNNEL"));
        LinearLayout tunHub = UiFactory.card(ctx, UiFactory.COLOR_SKY);
        tunHub.addView(UiFactory.iconNavRow(ctx, "⊞", "Tunnel Type (" + XrayUriAndRouteHelper.formatSelectedTunnelHeadline(cfg) + ")",
                "2-Column Tunnel Type + Connect From + Custom Payload matrix", UiFactory.COLOR_SKY,
                new View.OnClickListener() {
                    @Override public void onClick(View v) { host.openConfigureSubScreen(SUB_TUNNEL_TYPE_MATRIX); }
                }));
        tunHub.addView(UiFactory.divider(ctx));
        tunHub.addView(UiFactory.iconNavRow(ctx, "☁", "Secure Shell (SSH)",
                "User " + cfg.tunnelUser + " • Local :" + cfg.sshLocalPort + " • UDPGW :" + cfg.sshUdpgwPort,
                UiFactory.COLOR_MUTED, new View.OnClickListener() {
                    @Override public void onClick(View v) { host.openConfigureSubScreen(SUB_SSH_SETTINGS); }
                }));
        tunHub.addView(UiFactory.iconNavRow(ctx, "❖", "V2Ray/Xray",
                cfg.xrayCore + " • " + cfg.xrayProtocol + " • " + cfg.xrayNetworkType + " • " + cfg.xraySecurity,
                UiFactory.COLOR_MUTED, new View.OnClickListener() {
                    @Override public void onClick(View v) { host.openConfigureSubScreen(SUB_V2RAY_XRAY); }
                }));
        tunHub.addView(UiFactory.iconNavRow(ctx, "⚡", "Hysteria",
                "v" + cfg.hysteriaVersion + " • " + cfg.hysteriaProtocol + " • QUIC Window " + cfg.hysteriaQuicRcvWinMb + " MB",
                UiFactory.COLOR_MUTED, new View.OnClickListener() {
                    @Override public void onClick(View v) { host.openConfigureSubScreen(SUB_HYSTERIA_SETTINGS); }
                }));
        tunHub.addView(UiFactory.iconNavRow(ctx, "🌐", "DNSTT (DNS)",
                cfg.dnsttDomain + " • PubKey " + (cfg.dnsttPubKey.isEmpty() ? "(unset)" : "set"),
                UiFactory.COLOR_MUTED, new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        promptInput(ctx, "DNSTT Domain,PubKey", cfg.dnsttDomain + "," + cfg.dnsttPubKey, new InputCb() {
                            @Override public void onVal(String s) {
                                String[] p = s.split(",", -1);
                                cfg.dnsttDomain = p[0].trim();
                                if (p.length > 1) cfg.dnsttPubKey = p[1].trim();
                                cfg.tunnel = "SSH";
                                cfg.connectFromMode = "DNS (DNSTT)";
                                host.persistAndReload("DNSTT=" + cfg.dnsttDomain);
                                host.openConfigureSubScreen(SUB_INJECTOR_TUNNEL_HUB);
                            }
                        });
                    }
                }));
        tunHub.addView(UiFactory.iconNavRow(ctx, "✈", "Shadowsocks",
                cfg.tunnelCipher + (cfg.ssObfsServer.isEmpty() ? "" : " • " + cfg.ssObfsServer),
                UiFactory.COLOR_EMERALD, new View.OnClickListener() {
                    @Override public void onClick(View v) { host.openConfigureSubScreen(SUB_SHADOWSOCKS_SETTINGS); }
                }));
        tunHub.addView(UiFactory.divider(ctx));
        tunHub.addView(UiFactory.iconSwitchRow(ctx, "☑", "Apps Filter",
                "Enable per-app VPN/Bypass filtering (" + cfg.bypassApps.size() + " bypassed)",
                cfg.appsFilterEnabled, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean c) {
                        cfg.appsFilterEnabled = c;
                        host.persistAndReload("Apps Filter=" + c);
                    }
                }));
        col.addView(tunHub);

        // General & Connection Routes + HTTP Ping + HTTP Ping URL (184409)
        col.addView(UiFactory.categoryTitle(ctx, "GENERAL & CONNECTION"));
        LinearLayout routeCard = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        routeCard.addView(UiFactory.iconNavRow(ctx, "🌐", "VPN DNS", cfg.vpnDnsPreset, UiFactory.COLOR_SKY,
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        showRadioDialog(ctx, "VPN DNS", DnsPresets.VPN_DNS_PRESETS, cfg.vpnDnsPreset, new InputCb() {
                            @Override public void onVal(String val) {
                                cfg.vpnDnsPreset = val;
                                host.persistAndReload("VPN DNS=" + val);
                                host.openConfigureSubScreen(SUB_INJECTOR_TUNNEL_HUB);
                            }
                        });
                    }
                }));
        routeCard.addView(UiFactory.iconSwitchRow(ctx, "↪", "DNS Forwarding",
                "Enable to redirect all DNS queries to remote server, disable to resolved by local system",
                cfg.dnsForwarding, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean c) {
                        cfg.dnsForwarding = c;
                        host.persistAndReload("DNS Forwarding=" + c);
                    }
                }));
        routeCard.addView(UiFactory.divider(ctx));

        routeCard.addView(UiFactory.iconSwitchRow(ctx, "☍", "Use default route",
                "Route 0.0.0.0/0 minus Excluded Routes", cfg.useDefaultRoute,
                new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean c) {
                        cfg.useDefaultRoute = c;
                        host.persistAndReload("Use default route=" + c);
                        host.openConfigureSubScreen(SUB_INJECTOR_TUNNEL_HUB);
                    }
                }));
        View customRtRow = UiFactory.iconNavRow(ctx, "➕", "Custom Routes", cfg.customRoutes,
                cfg.useDefaultRoute ? COLOR_DIMMED : UiFactory.COLOR_SKY,
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        promptInput(ctx, "Custom Routes (CIDRs)", cfg.customRoutes, new InputCb() {
                            @Override public void onVal(String s) {
                                cfg.customRoutes = s.trim();
                                host.persistAndReload("Custom Routes=" + cfg.customRoutes);
                                host.openConfigureSubScreen(SUB_INJECTOR_TUNNEL_HUB);
                            }
                        });
                    }
                });
        customRtRow.setAlpha(cfg.useDefaultRoute ? 0.58f : 1.0f);
        routeCard.addView(customRtRow);

        List<XrayUriAndRouteHelper.CidrBlock> computed = XrayUriAndRouteHelper.computeEffectiveIpv4Routes(
                cfg.useDefaultRoute, cfg.customRoutes, cfg.excludedRoutes);
        routeCard.addView(UiFactory.iconNavRow(ctx, "➖", "Excluded Routes (" + computed.size() + " effective subnets)",
                cfg.excludedRoutes, UiFactory.COLOR_SKY,
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        promptInput(ctx, "Excluded Routes (semicolon-separated CIDRs)", cfg.excludedRoutes, new InputCb() {
                            @Override public void onVal(String s) {
                                cfg.excludedRoutes = s.trim();
                                host.persistAndReload("Excluded Routes updated");
                                host.openConfigureSubScreen(SUB_INJECTOR_TUNNEL_HUB);
                            }
                        });
                    }
                }));
        routeCard.addView(UiFactory.divider(ctx));

        routeCard.addView(UiFactory.iconSwitchRow(ctx, "📡", "HTTP Ping",
                "Periodic HTTP 204 keep-alive connectivity check", cfg.httpPingEnabled,
                new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean c) {
                        cfg.httpPingEnabled = c;
                        host.persistAndReload("HTTP Ping=" + c);
                        host.openConfigureSubScreen(SUB_INJECTOR_TUNNEL_HUB);
                    }
                }));
        View pingUrlRow = UiFactory.iconNavRow(ctx, "🔗", "HTTP Ping URL", cfg.httpPingUrl,
                cfg.httpPingEnabled ? UiFactory.COLOR_SKY : COLOR_DIMMED,
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        promptInput(ctx, "HTTP Ping URL", cfg.httpPingUrl, new InputCb() {
                            @Override public void onVal(String s) {
                                cfg.httpPingUrl = s.trim().isEmpty() ? "clients3.google.com/generate_204" : s.trim();
                                host.persistAndReload("HTTP Ping URL=" + cfg.httpPingUrl);
                                host.openConfigureSubScreen(SUB_INJECTOR_TUNNEL_HUB);
                            }
                        });
                    }
                });
        pingUrlRow.setAlpha(cfg.httpPingEnabled ? 1.0f : 0.58f);
        routeCard.addView(pingUrlRow);

        final TextView pingTv = UiFactory.label(ctx, "Tap below to run live HTTP 204 connectivity probe.",
                11.5f, UiFactory.COLOR_MUTED, false, true);
        Button pingBtn = UiFactory.actionButton(ctx, "Test HTTP Ping (" + cfg.httpPingUrl + ")",
                UiFactory.COLOR_SURFACE, UiFactory.COLOR_EMERALD, UiFactory.COLOR_EMERALD);
        pingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pingTv.setTextColor(UiFactory.COLOR_AMBER);
                pingTv.setText("Pinging " + cfg.httpPingUrl + "...");
                host.getBgExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        final XrayUriAndRouteHelper.PingResult pr = XrayUriAndRouteHelper.runHttpPingGenerate204(cfg);
                        pingTv.post(new Runnable() {
                            @Override public void run() {
                                pingTv.setTextColor(pr.ok ? UiFactory.COLOR_EMERALD : UiFactory.COLOR_CRIMSON);
                                pingTv.setText(pr.summary);
                            }
                        });
                    }
                });
            }
        });
        routeCard.addView(pingBtn);
        routeCard.addView(pingTv);
        col.addView(routeCard);

        // Advanced Settings including exact 14-Provider DNS over HTTPS (DoH) modal (184356, 184849)
        col.addView(UiFactory.categoryTitle(ctx, "ADVANCED SETTINGS"));
        LinearLayout advCard = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        advCard.addView(UiFactory.iconSwitchRow(ctx, "🔋", "CPU Wakelock",
                "Keep CPU awake to reduce disconnect when screen off (Drain Battery)",
                cfg.preventDeviceSleep, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean c) {
                        cfg.preventDeviceSleep = c;
                        host.persistAndReload("CPU Wakelock=" + c);
                    }
                }));
        advCard.addView(UiFactory.iconNavRow(ctx, "⚙", "Battery Optimization",
                "Request Android Doze exemption", UiFactory.COLOR_AMBER,
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                ctx.startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
                            }
                        } catch (Exception ignored) {}
                    }
                }));
        advCard.addView(UiFactory.divider(ctx));

        String dohSub = "Disabled".equalsIgnoreCase(cfg.injectorDohProvider)
                ? "Disabled"
                : (cfg.injectorDohProvider + " (" + DnsPresets.getInjectorDohUrl(cfg.injectorDohProvider) + ")");
        advCard.addView(UiFactory.iconNavRow(ctx, "🔐", "DNS over HTTPS (DoH)", dohSub, UiFactory.COLOR_SKY,
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        showRadioDialog(ctx, "DNS over HTTPS (DoH)", DnsPresets.INJECTOR_DOH_PROVIDERS, cfg.injectorDohProvider, new InputCb() {
                            @Override public void onVal(String val) {
                                cfg.injectorDohProvider = val;
                                if (!"Disabled".equalsIgnoreCase(val)) {
                                    cfg.dnsMode = "Injector-DoH";
                                } else if ("Injector-DoH".equalsIgnoreCase(cfg.dnsMode)) {
                                    cfg.dnsMode = "Rethink-DoH";
                                }
                                host.persistAndReload("DNS over HTTPS (DoH)=" + val);
                                host.openConfigureSubScreen(SUB_INJECTOR_TUNNEL_HUB);
                            }
                        });
                    }
                }));

        final TextView dohTestTv = UiFactory.label(ctx, "Tap to test active DoH resolver (RFC 8484 wire query).",
                11.5f, UiFactory.COLOR_MUTED, false, true);
        if (!"Disabled".equalsIgnoreCase(cfg.injectorDohProvider)) {
            Button dohBtn = UiFactory.actionButton(ctx, "Probe DoH (" + cfg.injectorDohProvider + ")",
                    UiFactory.COLOR_SURFACE, UiFactory.COLOR_SKY, UiFactory.COLOR_SKY);
            dohBtn.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    dohTestTv.setTextColor(UiFactory.COLOR_AMBER);
                    dohTestTv.setText("Querying " + cfg.injectorDohProvider + " DoH...");
                    host.getBgExecutor().execute(new Runnable() {
                        @Override public void run() {
                            byte[] q = DnsPacketCodec.buildWireQuery("example.com", 1);
                            String prevMode = cfg.dnsMode;
                            cfg.dnsMode = "Injector-DoH";
                            final DnsUpstreamResolver.UpstreamResult r = DnsUpstreamResolver.resolve(null, cfg, q);
                            cfg.dnsMode = prevMode;
                            final String ip = r.wireResponse != null ? DnsPacketCodec.extractAnswerSummary(r.wireResponse) : "FAIL";
                            dohTestTv.post(new Runnable() {
                                @Override public void run() {
                                    boolean ok = r.wireResponse != null;
                                    dohTestTv.setTextColor(ok ? UiFactory.COLOR_EMERALD : UiFactory.COLOR_CRIMSON);
                                    dohTestTv.setText(ok
                                            ? ("DoH OK [" + cfg.injectorDohProvider + "] example.com -> " + ip + " (" + r.latencyMs + " ms)")
                                            : ("DoH Error: " + r.error));
                                }
                            });
                        }
                    });
                }
            });
            advCard.addView(dohBtn);
            advCard.addView(dohTestTv);
        }
        col.addView(advCard);

        // User Interface Section (Dark Mode / System + Language / Default — exact match to 184356)
        col.addView(UiFactory.categoryTitle(ctx, "USER INTERFACE"));
        LinearLayout uiCard = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        uiCard.addView(UiFactory.iconSwitchRow(ctx, "🌙", "Dark Mode",
                cfg.darkModeSystem ? "System" : "AMOLED Always Dark",
                !cfg.darkModeSystem, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean c) {
                        cfg.darkModeSystem = !c;
                        cfg.darkModeTheme = c ? "AMOLED" : "System";
                        host.persistAndReload("Dark Mode=" + cfg.darkModeTheme);
                        host.openConfigureSubScreen(SUB_INJECTOR_TUNNEL_HUB);
                    }
                }));
        uiCard.addView(UiFactory.divider(ctx));
        uiCard.addView(UiFactory.iconNavRow(ctx, "🌐", "Language", cfg.uiLanguage, UiFactory.COLOR_MUTED,
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        showRadioDialog(ctx, "Language", DnsPresets.UI_LANGUAGES, cfg.uiLanguage, new InputCb() {
                            @Override public void onVal(String val) {
                                cfg.uiLanguage = val;
                                host.persistAndReload("Language=" + val);
                                host.openConfigureSubScreen(SUB_INJECTOR_TUNNEL_HUB);
                            }
                        });
                    }
                }));
        col.addView(uiCard);

        // Notifications Section (Show notification icon + Vibrate — exact match to 184356)
        col.addView(UiFactory.categoryTitle(ctx, "NOTIFICATIONS"));
        LinearLayout notifCard = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        notifCard.addView(UiFactory.iconSwitchRow(ctx, "🔔", "Show notification icon",
                "Display persistent tunnel status in Android status bar",
                cfg.showNotificationIcon, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean c) {
                        cfg.showNotificationIcon = c;
                        host.persistAndReload("Show notification=" + c);
                    }
                }));
        notifCard.addView(UiFactory.divider(ctx));
        notifCard.addView(UiFactory.iconSwitchRow(ctx, "📳", "Vibrate",
                "Vibrate when connection status changed",
                cfg.vibrateOnConnect, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean c) {
                        cfg.vibrateOnConnect = c;
                        host.persistAndReload("Vibrate=" + c);
                    }
                }));
        col.addView(notifCard);

        sv.addView(col);
        return sv;
    }

    // === Small Dialog & Clipboard Helpers ===
    private interface InputCb {
        void onVal(String val);
    }

    private static void copyClip(Context ctx, String label, String text) {
        ClipboardManager cm = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText(label, text));
            Toast.makeText(ctx, "Copied: " + text, Toast.LENGTH_SHORT).show();
        }
    }

    private static void promptInput(Context ctx, String title, String current, final InputCb cb) {
        final EditText et = UiFactory.monoInput(ctx, current, "", false);
        new AlertDialog.Builder(ctx)
                .setTitle(title)
                .setView(et)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        cb.onVal(et.getText().toString());
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private static void showRadioDialog(Context ctx, String title, final String[] items, String current, final InputCb cb) {
        int sel = 0;
        for (int i = 0; i < items.length; i++) {
            if (items[i].equalsIgnoreCase(current)) { sel = i; break; }
        }
        new AlertDialog.Builder(ctx)
                .setTitle(title)
                .setSingleChoiceItems(items, sel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        cb.onVal(items[which]);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
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
