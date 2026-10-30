package com.skidwaredns.app.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.skidwaredns.app.config.SkidConfig;

/**
 * v6.0 Stack Lab — extra TCP / UDP / socket / SOCKS5 / WireGuard / Tor / Orbot /
 * I2P / VPN / TLS / HTTPS knobs that sit on top of the existing screens.
 */
public final class StackLabScreens {

    public static final int SUB_STACK_LAB = 50;

    private StackLabScreens() {}

    public static View build(final Context ctx, final ConfigureScreens.HostCallbacks host) {
        ScrollView sv = new ScrollView(ctx);
        sv.setFillViewport(true);
        LinearLayout col = new LinearLayout(ctx);
        col.setOrientation(LinearLayout.VERTICAL);
        int pad = UiFactory.dp(ctx, 14);
        col.setPadding(pad, pad, pad, UiFactory.dp(ctx, 24));
        final SkidConfig cfg = host.getConfig();

        col.addView(UiFactory.backHeader(ctx, "Stack Lab",
                "TCP, UDP, sockets, SOCKS5, WireGuard, Tor, I2P, VPN, TLS",
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        host.openConfigureSubScreen(ConfigureScreens.SUB_HUB);
                    }
                }));

        col.addView(UiFactory.label(ctx,
                "Fine-tune the packet path. Kernel options Android cannot set still persist in .skid and apply where the platform allows.",
                12.5f, UiFactory.COLOR_MUTED, false, false));

        // ---- TCP ----
        col.addView(UiFactory.categoryTitle(ctx, "TCP"));
        LinearLayout tcp = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        addInt(ctx, tcp, "⏱", "SO_LINGER (sec)", "Graceful close wait. 0 leaves the default; RST Quick-Close still wins.",
                String.valueOf(cfg.tcpLingerSec), "0", host, new IntSink() {
                    @Override public void set(int v) { cfg.tcpLingerSec = clamp(v, 0, 120); }
                });
        tcp.addView(UiFactory.divider(ctx));
        addInt(ctx, tcp, "#", "Keepalive probes", "TCP_KEEPCNT — probes before the socket is dead.",
                String.valueOf(cfg.tcpKeepaliveCount), "4", host, new IntSink() {
                    @Override public void set(int v) { cfg.tcpKeepaliveCount = clamp(v, 1, 16); }
                });
        tcp.addView(UiFactory.divider(ctx));
        tcp.addView(UiFactory.iconSwitchRow(ctx, "▣", "Selective ACK (SACK)",
                "Ask the peer for SACK so lost segments retransmit without a full window.",
                cfg.tcpSack, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean checked) {
                        cfg.tcpSack = checked;
                        host.persistAndReload("TCP-SACK=" + checked);
                    }
                }));
        tcp.addView(UiFactory.divider(ctx));
        tcp.addView(UiFactory.iconSwitchRow(ctx, "⏲", "TCP timestamps",
                "PAWS + RTT measurement. Off can help some broken middleboxes.",
                cfg.tcpTimestamps, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean checked) {
                        cfg.tcpTimestamps = checked;
                        host.persistAndReload("TCP-Timestamps=" + checked);
                    }
                }));
        tcp.addView(UiFactory.divider(ctx));
        tcp.addView(UiFactory.iconSwitchRow(ctx, "↺", "TIME_WAIT reuse",
                "tcp_tw_reuse — recycle sockets faster on this device.",
                cfg.tcpTwReuse, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean checked) {
                        cfg.tcpTwReuse = checked;
                        host.persistAndReload("TCP-TW-Reuse=" + checked);
                    }
                }));
        tcp.addView(UiFactory.divider(ctx));
        addInt(ctx, tcp, "⌛", "FIN timeout (sec)", "tcp_fin_timeout for half-closed sockets.",
                String.valueOf(cfg.tcpFinTimeoutSec), "60", host, new IntSink() {
                    @Override public void set(int v) { cfg.tcpFinTimeoutSec = clamp(v, 5, 300); }
                });
        tcp.addView(UiFactory.divider(ctx));
        tcp.addView(UiFactory.iconSwitchRow(ctx, "➤", "TCP pacing",
                "Spread bursts so nested tunnels drop fewer packets.",
                cfg.tcpPacing, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean checked) {
                        cfg.tcpPacing = checked;
                        host.persistAndReload("TCP-Pacing=" + checked);
                    }
                }));
        tcp.addView(UiFactory.divider(ctx));
        addInt(ctx, tcp, "☰", "SYN backlog", "Listen queue depth for local proxy sockets.",
                String.valueOf(cfg.tcpMaxSynBacklog), "128", host, new IntSink() {
                    @Override public void set(int v) { cfg.tcpMaxSynBacklog = clamp(v, 8, 1024); }
                });
        col.addView(tcp);

        // ---- UDP ----
        col.addView(UiFactory.categoryTitle(ctx, "UDP"));
        LinearLayout udp = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        udp.addView(UiFactory.iconSwitchRow(ctx, "✂", "Don't fragment",
                "Prefer DF so QUIC / WireGuard discover the real path MTU.",
                cfg.udpDontFrag, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean checked) {
                        cfg.udpDontFrag = checked;
                        host.persistAndReload("UDP-DontFrag=" + checked);
                    }
                }));
        udp.addView(UiFactory.divider(ctx));
        udp.addView(UiFactory.iconSwitchRow(ctx, "⬇", "UDP GRO hint",
                "Ask the NIC to coalesce inbound datagrams (pairs with GSO).",
                cfg.udpGroHint, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean checked) {
                        cfg.udpGroHint = checked;
                        host.persistAndReload("UDP-GRO-Hint=" + checked);
                    }
                }));
        udp.addView(UiFactory.divider(ctx));
        udp.addView(UiFactory.iconSwitchRow(ctx, "↻", "Hairpin NAT",
                "Allow LAN peers to bounce through this device's NAT mapping.",
                cfg.udpHairpin, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean checked) {
                        cfg.udpHairpin = checked;
                        host.persistAndReload("UDP-Hairpin=" + checked);
                    }
                }));
        udp.addView(UiFactory.divider(ctx));
        addInt(ctx, udp, "📡", "Multicast TTL", "IP_MULTICAST_TTL for SSDP / mDNS helpers.",
                String.valueOf(cfg.udpMulticastTtl), "1", host, new IntSink() {
                    @Override public void set(int v) { cfg.udpMulticastTtl = clamp(v, 0, 32); }
                });
        udp.addView(UiFactory.divider(ctx));
        addInt(ctx, udp, "#", "NAT table size", "Max concurrent UDP 5-tuples in the proxy.",
                String.valueOf(cfg.udpNatMaxEntries), "4096", host, new IntSink() {
                    @Override public void set(int v) { cfg.udpNatMaxEntries = clamp(v, 64, 65536); }
                });
        udp.addView(UiFactory.divider(ctx));
        addInt(ctx, udp, "⏱", "UDP connect timeout (ms)", "Connected-mode datagram wait.",
                String.valueOf(cfg.udpConnectTimeoutMs), "1500", host, new IntSink() {
                    @Override public void set(int v) { cfg.udpConnectTimeoutMs = clamp(v, 200, 15000); }
                });
        col.addView(udp);

        // ---- Socket ----
        col.addView(UiFactory.categoryTitle(ctx, "Socket"));
        LinearLayout sock = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        addInt(ctx, sock, "⏱", "SO_LINGER (socket)", "Fallback linger if TCP linger is 0.",
                String.valueOf(cfg.soLingerSec), "0", host, new IntSink() {
                    @Override public void set(int v) { cfg.soLingerSec = clamp(v, 0, 120); }
                });
        sock.addView(UiFactory.divider(ctx));
        addInt(ctx, sock, "▲", "SO_PRIORITY", "Linux packet priority 0-6.",
                String.valueOf(cfg.soPriority), "0", host, new IntSink() {
                    @Override public void set(int v) { cfg.soPriority = clamp(v, 0, 6); }
                });
        sock.addView(UiFactory.divider(ctx));
        addInt(ctx, sock, "⚑", "SO_MARK", "fwmark for policy routing. 0 = none.",
                String.valueOf(cfg.soMark), "0", host, new IntSink() {
                    @Override public void set(int v) { cfg.soMark = clamp(v, 0, 65535); }
                });
        sock.addView(UiFactory.divider(ctx));
        sock.addView(UiFactory.iconSwitchRow(ctx, "6", "Dual-stack sockets",
                "IPV6_V6ONLY off so one bind covers v4-mapped too.",
                cfg.socketDualStack, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean checked) {
                        cfg.socketDualStack = checked;
                        host.persistAndReload("Socket-DualStack=" + checked);
                    }
                }));
        sock.addView(UiFactory.divider(ctx));
        addStr(ctx, sock, "📶", "SO_BINDTODEVICE", "Optional iface name (wlan0, rmnet0). Empty = any.",
                cfg.soBindToDevice, "wlan0", host, new StrSink() {
                    @Override public void set(String v) { cfg.soBindToDevice = v; }
                });
        col.addView(sock);

        // ---- SOCKS5 / HTTP CONNECT ----
        col.addView(UiFactory.categoryTitle(ctx, "SOCKS5 & HTTP CONNECT"));
        LinearLayout px = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        addStr(ctx, px, "👤", "SOCKS5 username", "RFC 1929 user/pass. Empty = no-auth.",
                cfg.socks5AuthUser, "skid", host, new StrSink() {
                    @Override public void set(String v) { cfg.socks5AuthUser = v; }
                });
        px.addView(UiFactory.divider(ctx));
        addStr(ctx, px, "🔑", "SOCKS5 password", "Paired with username.",
                cfg.socks5AuthPass, "", host, new StrSink() {
                    @Override public void set(String v) { cfg.socks5AuthPass = v; }
                });
        px.addView(UiFactory.divider(ctx));
        px.addView(UiFactory.iconSwitchRow(ctx, "UDP", "UDP ASSOCIATE",
                "Offer SOCKS5 UDP associate for QUIC / DNS / games.",
                cfg.socks5UdpAssociate, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean checked) {
                        cfg.socks5UdpAssociate = checked;
                        host.persistAndReload("SOCKS5-UDP-Associate=" + checked);
                    }
                }));
        px.addView(UiFactory.divider(ctx));
        px.addView(UiFactory.iconSwitchRow(ctx, "DNS", "Remote DNS via SOCKS",
                "Resolve A/AAAA through the proxy instead of the device stub.",
                cfg.socks5RemoteDns, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean checked) {
                        cfg.socks5RemoteDns = checked;
                        host.persistAndReload("SOCKS5-Remote-DNS=" + checked);
                    }
                }));
        px.addView(UiFactory.divider(ctx));
        addInt(ctx, px, "⏱", "SOCKS connect timeout (ms)", "Handshake wait to 127.0.0.1 or upstream.",
                String.valueOf(cfg.socks5ConnectTimeoutMs), "8000", host, new IntSink() {
                    @Override public void set(int v) { cfg.socks5ConnectTimeoutMs = clamp(v, 500, 60000); }
                });
        px.addView(UiFactory.divider(ctx));
        addStr(ctx, px, "👤", "HTTP CONNECT user", "Proxy-Authorization for HTTP tunnels.",
                cfg.httpConnectUser, "", host, new StrSink() {
                    @Override public void set(String v) { cfg.httpConnectUser = v; }
                });
        px.addView(UiFactory.divider(ctx));
        addStr(ctx, px, "🔑", "HTTP CONNECT password", "Paired with CONNECT user.",
                cfg.httpConnectPass, "", host, new StrSink() {
                    @Override public void set(String v) { cfg.httpConnectPass = v; }
                });
        col.addView(px);

        // ---- WireGuard ----
        col.addView(UiFactory.categoryTitle(ctx, "WireGuard"));
        LinearLayout wg = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        addStr(ctx, wg, "🔑", "Private key", "Local base64 private key. Never uploaded.",
                cfg.wgPrivateKey, "", host, new StrSink() {
                    @Override public void set(String v) { cfg.wgPrivateKey = v; }
                });
        wg.addView(UiFactory.divider(ctx));
        addStr(ctx, wg, "🛡", "AllowedIPs", "Comma CIDRs sent through the tunnel.",
                cfg.wgAllowedIps, "0.0.0.0/0, ::/0", host, new StrSink() {
                    @Override public void set(String v) { cfg.wgAllowedIps = v; }
                });
        wg.addView(UiFactory.divider(ctx));
        addStr(ctx, wg, "DNS", "WG DNS", "DNS pushed after handshake.",
                cfg.wgDns, "1.1.1.1", host, new StrSink() {
                    @Override public void set(String v) { cfg.wgDns = v; }
                });
        wg.addView(UiFactory.divider(ctx));
        addInt(ctx, wg, "#", "Listen port", "Local UDP listen. 0 = random.",
                String.valueOf(cfg.wgListenPort), "51820", host, new IntSink() {
                    @Override public void set(int v) { cfg.wgListenPort = clamp(v, 0, 64000); }
                });
        wg.addView(UiFactory.divider(ctx));
        addInt(ctx, wg, "♥", "Persistent keepalive (sec)", "0 disables. 25 is typical on mobile.",
                String.valueOf(cfg.wgKeepaliveSec), "25", host, new IntSink() {
                    @Override public void set(int v) { cfg.wgKeepaliveSec = clamp(v, 0, 600); }
                });
        wg.addView(UiFactory.divider(ctx));
        addInt(ctx, wg, "📏", "WG MTU", "Inner MTU, independent of the VPN tun MTU.",
                String.valueOf(cfg.wgMtu), "1420", host, new IntSink() {
                    @Override public void set(int v) { cfg.wgMtu = clamp(v, 1280, 1500); }
                });
        wg.addView(UiFactory.divider(ctx));
        addStr(ctx, wg, "🔒", "Preshared key", "Optional extra PSK on the peer.",
                cfg.wgPresharedKey, "", host, new StrSink() {
                    @Override public void set(String v) { cfg.wgPresharedKey = v; }
                });
        col.addView(wg);

        // ---- Tor / Orbot ----
        col.addView(UiFactory.categoryTitle(ctx, "Tor & Orbot"));
        LinearLayout tor = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        addStr(ctx, tor, "🧅", "Orbot SOCKS host", "Usually 127.0.0.1 when Orbot is on this phone.",
                cfg.orbotSocksHost, "127.0.0.1", host, new StrSink() {
                    @Override public void set(String v) { cfg.orbotSocksHost = v; }
                });
        tor.addView(UiFactory.divider(ctx));
        addInt(ctx, tor, "#", "Orbot SOCKS port", "Orbot default 9050.",
                String.valueOf(cfg.orbotSocksPort), "9050", host, new IntSink() {
                    @Override public void set(int v) { cfg.orbotSocksPort = clamp(v, 1, 65535); }
                });
        tor.addView(UiFactory.divider(ctx));
        addInt(ctx, tor, "#", "Orbot HTTP port", "HTTP tunnel 8118.",
                String.valueOf(cfg.orbotHttpPort), "8118", host, new IntSink() {
                    @Override public void set(int v) { cfg.orbotHttpPort = clamp(v, 1, 65535); }
                });
        tor.addView(UiFactory.divider(ctx));
        addInt(ctx, tor, "#", "Orbot control port", "NEWNYM / SIGNAL. 9051.",
                String.valueOf(cfg.orbotControlPort), "9051", host, new IntSink() {
                    @Override public void set(int v) { cfg.orbotControlPort = clamp(v, 1, 65535); }
                });
        tor.addView(UiFactory.divider(ctx));
        tor.addView(UiFactory.iconSwitchRow(ctx, "VPN", "Orbot VPN mode",
                "Prefer Orbot's own tun instead of SOCKS when both are installed.",
                cfg.orbotVpnMode, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean checked) {
                        cfg.orbotVpnMode = checked;
                        host.persistAndReload("Orbot-VPN-Mode=" + checked);
                    }
                }));
        tor.addView(UiFactory.divider(ctx));
        tor.addView(UiFactory.iconSwitchRow(ctx, "▶", "Start Orbot on connect",
                "Send Orbot the start intent when this profile enables Tor.",
                cfg.orbotStartOnConnect, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean checked) {
                        cfg.orbotStartOnConnect = checked;
                        host.persistAndReload("Orbot-Start-On-Connect=" + checked);
                    }
                }));
        tor.addView(UiFactory.divider(ctx));
        tor.addView(UiFactory.iconSwitchRow(ctx, "↺", "NEWNYM on start",
                "Rotate circuits when the engine comes up.",
                cfg.torNewnymOnStart, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean checked) {
                        cfg.torNewnymOnStart = checked;
                        host.persistAndReload("Tor-Newnym-On-Start=" + checked);
                    }
                }));
        tor.addView(UiFactory.divider(ctx));
        addInt(ctx, tor, "⏱", "Circuit timeout (sec)", "Give up building a circuit after this.",
                String.valueOf(cfg.torCircuitTimeoutSec), "60", host, new IntSink() {
                    @Override public void set(int v) { cfg.torCircuitTimeoutSec = clamp(v, 5, 600); }
                });
        col.addView(tor);

        // ---- I2P ----
        col.addView(UiFactory.categoryTitle(ctx, "I2P"));
        LinearLayout i2p = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        addInt(ctx, i2p, "←", "Inbound length", "Hops inbound (2-5 typical).",
                String.valueOf(cfg.i2pInboundLength), "3", host, new IntSink() {
                    @Override public void set(int v) { cfg.i2pInboundLength = clamp(v, 0, 7); }
                });
        i2p.addView(UiFactory.divider(ctx));
        addInt(ctx, i2p, "→", "Outbound length", "Hops outbound.",
                String.valueOf(cfg.i2pOutboundLength), "3", host, new IntSink() {
                    @Override public void set(int v) { cfg.i2pOutboundLength = clamp(v, 0, 7); }
                });
        i2p.addView(UiFactory.divider(ctx));
        addInt(ctx, i2p, "#", "Inbound quantity", "Parallel inbound tunnels.",
                String.valueOf(cfg.i2pInboundQty), "2", host, new IntSink() {
                    @Override public void set(int v) { cfg.i2pInboundQty = clamp(v, 1, 16); }
                });
        i2p.addView(UiFactory.divider(ctx));
        addInt(ctx, i2p, "#", "Outbound quantity", "Parallel outbound tunnels.",
                String.valueOf(cfg.i2pOutboundQty), "2", host, new IntSink() {
                    @Override public void set(int v) { cfg.i2pOutboundQty = clamp(v, 1, 16); }
                });
        i2p.addView(UiFactory.divider(ctx));
        i2p.addView(UiFactory.iconSwitchRow(ctx, "🕶", "Hidden mode",
                "No floodfill, reduced transit — quieter on the netdb.",
                cfg.i2pHiddenMode, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean checked) {
                        cfg.i2pHiddenMode = checked;
                        host.persistAndReload("I2P-Hidden-Mode=" + checked);
                    }
                }));
        col.addView(i2p);

        // ---- VPN / Network ----
        col.addView(UiFactory.categoryTitle(ctx, "VPN & Network"));
        LinearLayout net = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        net.addView(UiFactory.iconSwitchRow(ctx, "📌", "Always-on VPN hint",
                "Remind Android to keep this VPN as always-on (settings still owned by the system).",
                cfg.vpnAlwaysOnHint, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean checked) {
                        cfg.vpnAlwaysOnHint = checked;
                        host.persistAndReload("VPN-AlwaysOn-Hint=" + checked);
                    }
                }));
        net.addView(UiFactory.divider(ctx));
        net.addView(UiFactory.iconSwitchRow(ctx, "6", "Block IPv6 on tun",
                "Do not add an IPv6 address to the VPN interface.",
                cfg.vpnBlockIpv6, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean checked) {
                        cfg.vpnBlockIpv6 = checked;
                        host.persistAndReload("VPN-Block-IPv6=" + checked);
                    }
                }));
        net.addView(UiFactory.divider(ctx));
        net.addView(UiFactory.iconSwitchRow(ctx, "◎", "Happy Eyeballs",
                "Race v4 and v6 with a short delay (RFC 8305).",
                cfg.networkHappyEyeballs, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean checked) {
                        cfg.networkHappyEyeballs = checked;
                        host.persistAndReload("Network-Happy-Eyeballs=" + checked);
                    }
                }));
        net.addView(UiFactory.divider(ctx));
        addInt(ctx, net, "⏱", "Happy Eyeballs delay (ms)", "Wait before the second family.",
                String.valueOf(cfg.networkHappyDelayMs), "250", host, new IntSink() {
                    @Override public void set(int v) { cfg.networkHappyDelayMs = clamp(v, 0, 2000); }
                });
        net.addView(UiFactory.divider(ctx));
        net.addView(UiFactory.iconSwitchRow(ctx, "🌐", "Captive-portal bypass",
                "Let connectivitycheck / generate_204 skip the tunnel.",
                cfg.networkCaptiveBypass, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean checked) {
                        cfg.networkCaptiveBypass = checked;
                        host.persistAndReload("Network-Captive-Bypass=" + checked);
                    }
                }));
        net.addView(UiFactory.divider(ctx));
        net.addView(UiFactory.iconSwitchRow(ctx, "📶", "Prefer Wi-Fi",
                "Pause mobile when Wi-Fi is up (engine still owns the tun).",
                cfg.networkPreferWifi, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean checked) {
                        cfg.networkPreferWifi = checked;
                        host.persistAndReload("Network-Prefer-WiFi=" + checked);
                    }
                }));
        col.addView(net);

        // ---- TLS / HTTPS ----
        col.addView(UiFactory.categoryTitle(ctx, "TLS & HTTPS"));
        LinearLayout tls = UiFactory.card(ctx, UiFactory.COLOR_BORDER);
        tls.addView(UiFactory.label(ctx, "Max TLS", 13f, UiFactory.COLOR_TEXT, true, false));
        tls.addView(UiFactory.buildChipSelector(ctx,
                new String[]{"TLS 1.2", "TLS 1.3"},
                cfg.httpsMaxTls, UiFactory.COLOR_SKY, new UiFactory.ChipSelectListener() {
                    @Override public void onSelected(String value) {
                        cfg.httpsMaxTls = value;
                        host.persistAndReload("HTTPS-Max-TLS=" + value);
                    }
                }));
        tls.addView(UiFactory.divider(ctx));
        tls.addView(UiFactory.iconSwitchRow(ctx, "1.3", "TLS 1.3 only",
                "Disable 1.2 even if min TLS is lower. Breaks some CDNs.",
                cfg.httpsTls13Only, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean checked) {
                        cfg.httpsTls13Only = checked;
                        host.persistAndReload("HTTPS-TLS13-Only=" + checked);
                    }
                }));
        tls.addView(UiFactory.divider(ctx));
        tls.addView(UiFactory.iconSwitchRow(ctx, "✓", "OCSP stapling",
                "Prefer stapled OCSP when the peer offers it.",
                cfg.httpsOcspStapling, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean checked) {
                        cfg.httpsOcspStapling = checked;
                        host.persistAndReload("HTTPS-OCSP-Stapling=" + checked);
                    }
                }));
        tls.addView(UiFactory.divider(ctx));
        tls.addView(UiFactory.iconSwitchRow(ctx, "📌", "Strict cert pin",
                "Fail closed when a pin is set and the chain does not match.",
                cfg.httpsCertPinStrict, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean checked) {
                        cfg.httpsCertPinStrict = checked;
                        host.persistAndReload("HTTPS-Cert-Pin-Strict=" + checked);
                    }
                }));
        tls.addView(UiFactory.divider(ctx));
        tls.addView(UiFactory.iconSwitchRow(ctx, "✦", "TLS GREASE",
                "Chrome-style GREASE on ClientHello so middleboxes cannot ossify.",
                cfg.httpsGrease, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean checked) {
                        cfg.httpsGrease = checked;
                        host.persistAndReload("HTTPS-GREASE=" + checked);
                    }
                }));
        tls.addView(UiFactory.divider(ctx));
        tls.addView(UiFactory.iconSwitchRow(ctx, "🕶", "Encrypted Client Hello",
                "ECH when the resolver returns HTTPS records. Best-effort.",
                cfg.httpsEchEnabled, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean checked) {
                        cfg.httpsEchEnabled = checked;
                        host.persistAndReload("HTTPS-ECH=" + checked);
                    }
                }));
        tls.addView(UiFactory.divider(ctx));
        tls.addView(UiFactory.iconSwitchRow(ctx, "3", "HTTP/3 alt-svc",
                "Follow Alt-Svc h3 when ALPN already lists h3.",
                cfg.httpsH3Alt, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean checked) {
                        cfg.httpsH3Alt = checked;
                        host.persistAndReload("HTTPS-H3-Alt=" + checked);
                    }
                }));
        tls.addView(UiFactory.divider(ctx));
        tls.addView(UiFactory.iconSwitchRow(ctx, "🏛", "Trust system CAs",
                "Off = only the pin / custom cert. Dangerous if neither is set.",
                cfg.httpsTrustSystem, new UiFactory.ToggleListener() {
                    @Override public void onToggled(boolean checked) {
                        cfg.httpsTrustSystem = checked;
                        host.persistAndReload("HTTPS-Trust-System=" + checked);
                    }
                }));
        col.addView(tls);

        UiFactory.staggerFade(col, 20);
        sv.addView(col);
        return sv;
    }

    private interface StrSink { void set(String v); }
    private interface IntSink { void set(int v); }

    private static int clamp(int v, int lo, int hi) {
        if (v < lo) return lo;
        if (v > hi) return hi;
        return v;
    }

    private static void addStr(final Context ctx, LinearLayout card, String glyph, final String title,
                               String desc, final String current, String hint,
                               final ConfigureScreens.HostCallbacks host, final StrSink sink) {
        String shown = (current == null || current.isEmpty()) ? desc : current;
        card.addView(UiFactory.iconNavRow(ctx, glyph, title, shown, UiFactory.COLOR_MUTED,
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        final EditText et = UiFactory.monoInput(ctx, current, hint, false);
                        LinearLayout box = new LinearLayout(ctx);
                        box.setOrientation(LinearLayout.VERTICAL);
                        int p = UiFactory.dp(ctx, 16);
                        box.setPadding(p, p, p, p);
                        box.addView(et);
                        new AlertDialog.Builder(ctx)
                                .setTitle(title)
                                .setView(box)
                                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                                    @Override public void onClick(DialogInterface d, int w) {
                                        sink.set(et.getText().toString().trim());
                                        host.persistAndReload(title);
                                        host.openConfigureSubScreen(SUB_STACK_LAB);
                                    }
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                    }
                }));
    }

    private static void addInt(final Context ctx, LinearLayout card, String glyph, final String title,
                               String desc, final String current, String hint,
                               final ConfigureScreens.HostCallbacks host, final IntSink sink) {
        card.addView(UiFactory.iconNavRow(ctx, glyph, title, current + "  ·  " + desc,
                UiFactory.COLOR_MUTED, new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        final EditText et = UiFactory.monoInput(ctx, current, hint, false);
                        LinearLayout box = new LinearLayout(ctx);
                        box.setOrientation(LinearLayout.VERTICAL);
                        int p = UiFactory.dp(ctx, 16);
                        box.setPadding(p, p, p, p);
                        box.addView(et);
                        new AlertDialog.Builder(ctx)
                                .setTitle(title)
                                .setView(box)
                                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                                    @Override public void onClick(DialogInterface d, int w) {
                                        int n = 0;
                                        try { n = Integer.parseInt(et.getText().toString().trim()); }
                                        catch (Exception ignored) {}
                                        sink.set(n);
                                        host.persistAndReload(title);
                                        host.openConfigureSubScreen(SUB_STACK_LAB);
                                    }
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                    }
                }));
    }
}
