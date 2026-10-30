package com.skidwaredns.app.model;

import com.skidwaredns.app.config.SkidConfig;

/**
 * Named engine modes for the Home chevron. Each mode flips a small set of
 * .skid flags so Start does the obvious thing.
 */
public final class OperatingModes {

    public static final String[] NAMES = new String[]{
            "DNS+Firewall",
            "DNS-Only",
            "Firewall-Only",
            "VPN+DNS+Firewall",
            "Proxy-Only",
            "WireGuard-VPN",
            "Mullvad-WG",
            "SSH-Tunnel",
            "Tor+DNSCrypt+VPN",
            "Tor+DNSCrypt+I2P",
            "I2P-Only",
            "Mixnet-5Hop",
            "Stealth-DPI",
            "Lockdown",
            "LAN-Tools"
    };

    public static final String[] BLURBS = new String[]{
            "Resolve + blocklists, no full-tunnel",
            "DNS capture only, firewall idle",
            "Blocklists + tun, no proxy",
            "Full-tunnel VPN, DNS and firewall",
            "Local SOCKS/HTTP, no VpnService tun",
            "WireGuard profile as the tunnel",
            "Mullvad relay + WireGuard",
            "SSH SOCKS jump, no tun",
            "Tor + DNSCrypt inside a VPN tun",
            "Tor + DNSCrypt + I2P together",
            "Purple I2P proxies only",
            "Nym 5-hop mixnet",
            "Full-tunnel + Always DPI desync",
            "Strict default route + proxy lockdown",
            "Local FTP / Open Port / scanners"
    };

    private OperatingModes() {}

    public static boolean usesVpnTun(String mode) {
        if (mode == null) return true;
        String m = mode.trim();
        return !eq(m, "Proxy-Only") && !eq(m, "I2P-Only") && !eq(m, "SSH-Tunnel") && !eq(m, "LAN-Tools");
    }

    public static boolean usesDefaultRoute(String mode) {
        if (mode == null) return false;
        String m = mode.trim();
        return eq(m, "VPN+DNS+Firewall")
                || eq(m, "Tor+DNSCrypt+VPN")
                || eq(m, "Tor+DNSCrypt+I2P")
                || eq(m, "Mixnet-5Hop")
                || eq(m, "WireGuard-VPN")
                || eq(m, "Mullvad-WG")
                || eq(m, "Stealth-DPI")
                || eq(m, "Lockdown")
                || eq(m, "Firewall-Only");
    }

    public static void apply(SkidConfig c, String mode) {
        if (c == null || mode == null) return;
        c.mode = mode;
        if (eq(mode, "DNS-Only")) {
            c.onDeviceBlocklistsEnabled = false;
        } else if (eq(mode, "Firewall-Only")) {
            c.onDeviceBlocklistsEnabled = true;
            c.proxyMode = "None";
        } else if (eq(mode, "VPN+DNS+Firewall")) {
            c.onDeviceBlocklistsEnabled = true;
        } else if (eq(mode, "Proxy-Only")) {
            if ("None".equalsIgnoreCase(c.proxyMode)) c.proxyMode = "SOCKS5";
        } else if (eq(mode, "WireGuard-VPN") || eq(mode, "Mullvad-WG")) {
            c.wgEnabled = true;
            c.tunnel = "WireGuard";
        } else if (eq(mode, "SSH-Tunnel")) {
            c.proxyMode = "SOCKS5";
            c.tunnel = "SSH";
        } else if (eq(mode, "Tor+DNSCrypt+VPN")) {
            c.torEnabled = true;
            c.dnscryptModuleEnabled = true;
            c.dnsMode = "DNSCrypt";
        } else if (eq(mode, "Tor+DNSCrypt+I2P")) {
            c.torEnabled = true;
            c.dnscryptModuleEnabled = true;
            c.i2pEnabled = true;
            c.dnsMode = "DNSCrypt";
        } else if (eq(mode, "I2P-Only")) {
            c.i2pEnabled = true;
            c.proxyMode = "I2P-Tunnel";
        } else if (eq(mode, "Mixnet-5Hop")) {
            c.nymProfile = "Anonymous";
            c.mixnetMode = "Anonymous-5Hop";
        } else if (eq(mode, "Stealth-DPI")) {
            c.dpiStrategy = "Always";
            c.dpiTechnique = "Hybrid";
            c.onDeviceBlocklistsEnabled = true;
        } else if (eq(mode, "Lockdown")) {
            c.strictRoute = true;
            c.proxyLockdown = true;
            c.onDeviceBlocklistsEnabled = true;
            if ("None".equalsIgnoreCase(c.proxyMode)) c.proxyMode = "SOCKS5";
        } else if (eq(mode, "LAN-Tools")) {
            if ("None".equalsIgnoreCase(c.proxyMode)) c.proxyMode = "SOCKS5";
        }
    }

    public static String blurb(String mode) {
        for (int i = 0; i < NAMES.length; i++) {
            if (eq(NAMES[i], mode)) return BLURBS[i];
        }
        return "";
    }

    private static boolean eq(String a, String b) {
        return a != null && b != null && a.equalsIgnoreCase(b);
    }
}
