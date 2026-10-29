package com.skidwaredns.app.vpn;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.ProxyInfo;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;

import com.skidwaredns.app.config.SkidConfig;
import com.skidwaredns.app.config.SkidConfigStore;
import com.skidwaredns.app.dns.DnsPacketCodec;
import com.skidwaredns.app.dns.DnsUpstreamResolver;
import com.skidwaredns.app.firewall.SkidFirewallEngine;
import com.skidwaredns.app.proxy.LocalProxyEngine;
import com.skidwaredns.app.tunnel.TunnelEngine;
import com.skidwaredns.app.tunnel.XrayUriAndRouteHelper;
import com.skidwaredns.app.ui.MainActivity;
import com.skidwaredns.app.util.NetDiagnosticsAndMitm;
import com.skidwaredns.app.util.PcapRecorder;
import com.skidwaredns.app.util.SkidEventBus;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Upgraded Core Android VpnService supporting:
 *  - Pause / Resume VPN snooze state
 *  - Custom Interface IPs, IPv4 / IPv6 / Dual-Stack & NAT64/DNS64 translation
 *  - Network Visibility (allowBypass), Underlying Networks, Always Metered, Loopback proxy exclusions
 *  - Real Wireshark-compatible .pcap Packet Capture (PcapRecorder)
 *  - DNS Booster cache, Record-Type filtering, Provider ASN tags, and WireGuard keepalive/flood pulses
 */
public final class SkidVpnService extends VpnService {

    public static final String ACTION_START = "com.skidwaredns.app.action.START";
    public static final String ACTION_STOP = "com.skidwaredns.app.action.STOP";
    public static final String ACTION_PAUSE = "com.skidwaredns.app.action.PAUSE";
    public static final String ACTION_RESUME = "com.skidwaredns.app.action.RESUME";
    public static final String ACTION_RELOAD = "com.skidwaredns.app.action.RELOAD";
    public static final String ACTION_ROTATE = "com.skidwaredns.app.action.ROTATE";

    private static final String CHANNEL_ID = "skidwaredns_engine_channel";
    private static final int NOTIF_ID = 1337;

    private static final List<String> KNOWN_PROXY_FORWARDER_APPS = Arrays.asList(
            "org.torproject.android",
            "pan.alexander.tordnscrypt.gp",
            "pan.alexander.tordnscrypt",
            "com.v2ray.ang",
            "io.nekohasekai.sagernet",
            "moe.nb4a"
    );

    private final AtomicBoolean engineActive = new AtomicBoolean(false);
    private final AtomicBoolean enginePaused = new AtomicBoolean(false);
    private final SkidFirewallEngine firewallEngine = new SkidFirewallEngine();

    private ParcelFileDescriptor tunInterface;
    private Thread tunReaderThread;
    private ExecutorService dnsWorkerPool;
    private ScheduledExecutorService keepaliveScheduler;
    private LocalProxyEngine localProxy;
    private PowerManager.WakeLock wakeLock;
    private volatile SkidConfig activeConfig;

    @Override
    public void onCreate() {
        super.onCreate();
        ensureNotificationChannel();
        localProxy = new LocalProxyEngine(this, this, firewallEngine);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : ACTION_START;

        if (ACTION_STOP.equals(action)) {
            stopEngine("NOT PROTECTED");
            stopSelf();
            return START_NOT_STICKY;
        }

        if (ACTION_PAUSE.equals(action) && engineActive.get()) {
            pauseTunOnly();
            return START_STICKY;
        }

        if (ACTION_RESUME.equals(action)) {
            enginePaused.set(false);
            stopEngine("Resuming...");
            startEngine();
            return START_STICKY;
        }

        if (ACTION_ROTATE.equals(action)) {
            DnsUpstreamResolver.flushCache();
            SkidEventBus.logSystem("SYSTEM", "Circuit/Booster Reset",
                    "Flushed DNS Booster cache & rotated session for [" + SkidEventBus.getActiveProfileName() + "]");
            updateForegroundNotification();
            return START_STICKY;
        }

        if (ACTION_RELOAD.equals(action) && engineActive.get()) {
            stopEngine("Reloading .skid profile...");
        }

        enginePaused.set(false);
        startEngine();
        return START_STICKY;
    }

    private synchronized void pauseTunOnly() {
        enginePaused.set(true);
        if (tunReaderThread != null) {
            tunReaderThread.interrupt();
            tunReaderThread = null;
        }
        if (tunInterface != null) {
            try { tunInterface.close(); } catch (Exception ignored) {}
            tunInterface = null;
        }
        if (localProxy != null) {
            localProxy.stop();
        }
        SkidEventBus.setState(SkidEventBus.EngineState.PAUSED, "PAUSED • Tap START or Resume to re-engage");
        SkidEventBus.logSystem("SYSTEM", "Engine Paused", "TUN0 temporarily snoozed");
        updateForegroundNotification();
    }

    private synchronized void startEngine() {
        if (engineActive.get() && !enginePaused.get()) {
            updateForegroundNotification();
            return;
        }

        activeConfig = SkidConfigStore.getActive(this);
        syncLoggingFlags(activeConfig);
        firewallEngine.reloadFromConfig(activeConfig);
        PcapRecorder.startOrUpdate(this, activeConfig.packetCaptureMode);

        SkidEventBus.setActiveProfileName(activeConfig.name);
        SkidEventBus.setActiveDnsBadge(describeDnsSelection(activeConfig));
        SkidEventBus.setActiveTunnelBadge(activeConfig.tunnel + " • " + activeConfig.connectFromMode);
        SkidEventBus.setState(SkidEventBus.EngineState.ENGAGING, "Starting " + activeConfig.name + "...");

        promoteToForeground("Starting " + activeConfig.name + "...");

        try {
            applyUnderlyingNetworkPolicy(activeConfig);
            localProxy.start(activeConfig);

            if ("Proxy-Only".equalsIgnoreCase(activeConfig.mode)) {
                engineActive.set(true);
                String msg = "PROTECTED (Proxy-Only :" + activeConfig.getPrimaryLocalSocksPort() + ")";
                SkidEventBus.setActiveTunAddress("127.0.0.1:" + activeConfig.getPrimaryLocalSocksPort());
                SkidEventBus.setState(SkidEventBus.EngineState.PROXY_ONLY_ACTIVE, msg);
                SkidEventBus.logSystem("SYSTEM", "Proxy-Only Mode", msg);
                startKeepaliveSchedulerIfNeeded(activeConfig);
                updateForegroundNotification();
                return;
            }

            Builder builder = new Builder();
            builder.setSession("SkidwareDNS [" + activeConfig.name + "]");
            builder.setMtu(activeConfig.getEffectiveMtu());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                builder.setMetered(activeConfig.alwaysMetered);
            }
            if (activeConfig.enableNetworkVisibility) {
                builder.allowBypass();
            }

            String primaryTunAddr = applyAddressesToBuilder(builder, activeConfig);
            SkidEventBus.setActiveTunAddress(primaryTunAddr);

            String gwV4 = (activeConfig.customTunGatewayV4 == null || activeConfig.customTunGatewayV4.trim().isEmpty())
                    ? "10.213.37.1" : activeConfig.customTunGatewayV4.trim();
            String gwV6 = (activeConfig.customTunGatewayV6 == null || activeConfig.customTunGatewayV6.trim().isEmpty())
                    ? "fd00:534b:4944::1" : activeConfig.customTunGatewayV6.trim();

            boolean wantV4 = !"IPv6".equalsIgnoreCase(activeConfig.ipVersion);
            boolean wantV6 = !"IPv4".equalsIgnoreCase(activeConfig.ipVersion);

            if (wantV4) {
                if (activeConfig.dnsForwarding) {
                    builder.addDnsServer(gwV4);
                }
                builder.addRoute(gwV4, 32);
                if (activeConfig.fakeIpEnabled) {
                    try { builder.addRoute("198.18.0.0", 15); } catch (Exception ignored) {}
                }
                if ("VPN+DNS+Firewall".equalsIgnoreCase(activeConfig.mode)
                        || "Tor+DNSCrypt+VPN".equalsIgnoreCase(activeConfig.mode)
                        || "Tor+DNSCrypt+I2P".equalsIgnoreCase(activeConfig.mode)) {
                    if (activeConfig.strictRoute) {
                        try {
                            builder.addRoute("0.0.0.0", 1);
                            builder.addRoute("128.0.0.0", 1);
                        } catch (Exception ignored) {}
                    } else {
                        List<XrayUriAndRouteHelper.CidrBlock> effRoutes =
                                XrayUriAndRouteHelper.computeEffectiveIpv4Routes(
                                        activeConfig.useDefaultRoute, activeConfig.customRoutes, activeConfig.excludedRoutes);
                        for (XrayUriAndRouteHelper.CidrBlock cb : effRoutes) {
                            try { builder.addRoute(cb.ipString(), cb.prefix); } catch (Exception ignored) {}
                        }
                    }
                }
            }
            if (wantV6) {
                try {
                    builder.addDnsServer(gwV6);
                    builder.addRoute(gwV6, 128);
                    if (activeConfig.strictRoute && ("VPN+DNS+Firewall".equalsIgnoreCase(activeConfig.mode)
                            || "Tor+DNSCrypt+VPN".equalsIgnoreCase(activeConfig.mode))) {
                        builder.addRoute("::", 1);
                        builder.addRoute("8000::", 1);
                    }
                } catch (Exception ignored) {
                }
            }

            // Prevent DNS leaks: capture port 53 hardcoded resolvers
            if ((activeConfig.preventDnsLeaks || activeConfig.blockBypassedDns) && wantV4 && !usesGooglePlainUpstream(activeConfig)) {
                try {
                    builder.addRoute("8.8.8.8", 32);
                    builder.addRoute("8.8.4.4", 32);
                } catch (Exception ignored) {
                }
            }

            // Sinkhole custom BLOCK_IP CIDRs (respecting Do-Not-Route-Private-IPs)
            List<SkidFirewallEngine.CidrRule> blockedCidrs = firewallEngine.getBlockedCidrsSnapshot();
            for (SkidFirewallEngine.CidrRule cidr : blockedCidrs) {
                if (activeConfig.doNotRoutePrivateIps && cidr.networkIp.startsWith("192.168.")) {
                    continue;
                }
                try {
                    builder.addRoute(cidr.networkIp, cidr.prefixLength);
                } catch (Exception ignored) {
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                    && (activeConfig.proxyLockdown
                    || "VPN+DNS+Firewall".equalsIgnoreCase(activeConfig.mode)
                    || "Tor+DNSCrypt+VPN".equalsIgnoreCase(activeConfig.mode))
                    && !"None".equalsIgnoreCase(activeConfig.proxyMode)) {
                try {
                    builder.setHttpProxy(ProxyInfo.buildDirectProxy("127.0.0.1", activeConfig.getPrimaryLocalHttpPort()));
                } catch (Exception ignored) {
                }
            }

            // Loopback & Proxy Forwarder App Exclusions
            if (!activeConfig.loopbackExperimental) {
                try {
                    builder.addDisallowedApplication(getPackageName());
                } catch (Exception ignored) {
                }
            }
            if (!activeConfig.loopbackProxyForwarderApps) {
                for (String fwdPkg : KNOWN_PROXY_FORWARDER_APPS) {
                    try {
                        builder.addDisallowedApplication(fwdPkg);
                    } catch (Exception ignored) {
                    }
                }
            }
            for (String bypassPkg : activeConfig.bypassApps) {
                try {
                    builder.addDisallowedApplication(bypassPkg.trim());
                } catch (Exception ignored) {
                }
            }

            tunInterface = builder.establish();
            if (tunInterface == null) {
                stopEngine("VPN permission required");
                SkidEventBus.setState(SkidEventBus.EngineState.ERROR, "VPN permission required");
                stopSelf();
                return;
            }

            engineActive.set(true);
            dnsWorkerPool = Executors.newFixedThreadPool(8);

            tunReaderThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    runTunPacketLoop();
                }
            }, "SkidwareDNS-TUN");
            tunReaderThread.setPriority(Thread.MAX_PRIORITY - 1);
            tunReaderThread.start();

            applyWakeLockPolicy(activeConfig);
            startKeepaliveSchedulerIfNeeded(activeConfig);
            triggerStatusVibration(activeConfig);

            String activeMsg = "PROTECTED • " + activeConfig.globalDnsCategory
                    + " • " + XrayUriAndRouteHelper.formatSelectedTunnelHeadline(activeConfig)
                    + " • SOCKS5 :" + activeConfig.getPrimaryLocalSocksPort();
            SkidEventBus.setState(SkidEventBus.EngineState.TUN_ACTIVE, activeMsg);
            SkidEventBus.logSystem("SYSTEM", "TUN0 Established",
                    "Profile=" + activeConfig.name + " | Addr=" + primaryTunAddr
                            + " | MTU=" + activeConfig.getEffectiveMtu()
                            + " |Buf=" + activeConfig.socketBufferSizeKb + "KB");
            updateForegroundNotification();

        } catch (Exception e) {
            SkidEventBus.logSystem("SYSTEM", "Engine Error", e.getMessage());
            stopEngine("Error: " + e.getMessage());
            SkidEventBus.setState(SkidEventBus.EngineState.ERROR, "Failed: " + e.getMessage());
            stopSelf();
        }
    }

    private void applyUnderlyingNetworkPolicy(SkidConfig cfg) {
        try {
            if (cfg.useAllAvailableNetworks) {
                setUnderlyingNetworks(null);
            } else {
                ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                if (cm != null) {
                    Network active = cm.getActiveNetwork();
                    setUnderlyingNetworks(active != null ? new Network[]{active} : null);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void applyWakeLockPolicy(SkidConfig cfg) {
        releaseWakeLock();
        if (cfg != null && cfg.preventDeviceSleep) {
            try {
                PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                if (pm != null) {
                    wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SkidwareDNS::TunnelKeepAlive");
                    wakeLock.setReferenceCounted(false);
                    wakeLock.acquire(12 * 60 * 60 * 1000L);
                    SkidEventBus.logSystem("SYSTEM", "WakeLock Active", "Prevent device sleep (PARTIAL_WAKE_LOCK engaged)");
                }
            } catch (Exception ignored) {
            }
        }
    }

    private void releaseWakeLock() {
        if (wakeLock != null) {
            try {
                if (wakeLock.isHeld()) wakeLock.release();
            } catch (Exception ignored) {
            }
            wakeLock = null;
        }
    }

    private void triggerStatusVibration(SkidConfig cfg) {
        if (cfg == null || !cfg.vibrateOnConnect) return;
        try {
            Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vib != null && vib.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vib.vibrate(VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vib.vibrate(45);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void startKeepaliveSchedulerIfNeeded(final SkidConfig cfg) {
        if (keepaliveScheduler != null) {
            keepaliveScheduler.shutdownNow();
            keepaliveScheduler = null;
        }
        boolean mixCover = ("Mixnet-5Hop".equalsIgnoreCase(cfg.mode) || "Anonymous".equalsIgnoreCase(cfg.nymProfile))
                && (cfg.mixnetCoverTrafficEnabled || cfg.mixnetContinuousTraffic);
        if (cfg.smartPersistentKeepalive || cfg.floodWireGuard || cfg.arpSpoofingDetection
                || cfg.httpPingEnabled || mixCover || cfg.amneziaWgMinimalObfs) {
            long intervalSec = mixCover
                    ? Math.max(3L, com.skidwaredns.app.tunnel.NymMixnetAndCensorshipEngine.computeCoverTrafficIntervalMs(cfg) / 1000L)
                    : 25L;
            keepaliveScheduler = Executors.newSingleThreadScheduledExecutor();
            keepaliveScheduler.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    if (engineActive.get() && !enginePaused.get()) {
                        if (cfg.smartPersistentKeepalive || cfg.floodWireGuard || cfg.amneziaWgMinimalObfs
                                || "Mixnet-5Hop".equalsIgnoreCase(cfg.mode)) {
                            TunnelEngine.sendWireGuardPulseIfEnabled(SkidVpnService.this, cfg);
                        }
                        if (cfg.arpSpoofingDetection) {
                            NetDiagnosticsAndMitm.runArpSpoofingAudit();
                        }
                        if (cfg.httpPingEnabled) {
                            XrayUriAndRouteHelper.PingResult pr = XrayUriAndRouteHelper.runHttpPingGenerate204(cfg);
                            SkidEventBus.logDebug("TCP", "HTTP-PING", cfg.httpPingUrl, pr.summary);
                        }
                    }
                }
            }, 5, intervalSec, TimeUnit.SECONDS);
        }
    }

    private void runTunPacketLoop() {
        final ParcelFileDescriptor pfd = this.tunInterface;
        if (pfd == null) return;

        final FileInputStream tunIn = new FileInputStream(pfd.getFileDescriptor());
        final FileOutputStream tunOut = new FileOutputStream(pfd.getFileDescriptor());
        final Object writeLock = new Object();
        byte[] packetBuf = new byte[32767];

        try {
            while (engineActive.get() && !enginePaused.get() && !Thread.currentThread().isInterrupted()) {
                int len = tunIn.read(packetBuf);
                if (len <= 0) continue;

                SkidEventBus.bytesOut.addAndGet(len);
                PcapRecorder.recordPacket(packetBuf, 0, len);

                final DnsPacketCodec.ParsedIpUdpPacket pkt = DnsPacketCodec.parseTunPacket(packetBuf, len);
                if (pkt == null) continue;

                if (pkt.protocol != 17 || (pkt.dstPort != 53 && pkt.dstPort != 5353 && pkt.dstPort != 853)) {
                    SkidFirewallEngine.Verdict v = firewallEngine.evaluateDestination(
                            this, pkt.dstIpString, pkt.dstPort, pkt.protocol == 17);
                    if (!v.allowed) {
                        SkidEventBus.recordDnsEvent(pkt.protocol == 17 ? "UDP" : "TCP",
                                pkt.dstIpString + ":" + pkt.dstPort, v.matchedRule, 0, true);
                    }
                    continue;
                }

                if (pkt.payload.length < 12) continue;

                ExecutorService pool = this.dnsWorkerPool;
                if (pool != null && !pool.isShutdown()) {
                    pool.execute(new Runnable() {
                        @Override
                        public void run() {
                            handleDnsPacket(pkt, tunOut, writeLock);
                        }
                    });
                }
            }
        } catch (Exception e) {
            if (engineActive.get() && !enginePaused.get()) {
                SkidEventBus.logSystem("SYSTEM", "TUN Loop", "TUN stream closed: " + e.getMessage());
            }
        }
    }

    private void handleDnsPacket(DnsPacketCodec.ParsedIpUdpPacket pkt, FileOutputStream tunOut, Object writeLock) {
        try {
            DnsPacketCodec.DnsQuestion q = DnsPacketCodec.parseDnsQuestion(pkt.payload);
            if (q == null || q.qname.isEmpty()) return;

            SkidFirewallEngine.Verdict verdict = firewallEngine.evaluateDnsQuery(this, q.qname, q.qtypeName);
            byte[] dnsReply;
            if (!verdict.allowed) {
                dnsReply = DnsPacketCodec.buildBlockedNxDomainReply(pkt.payload, q);
                SkidEventBus.recordDnsEvent(q.qtypeName, q.qname, "BLOCKED [" + verdict.matchedRule + "]", 0, true);
            } else {
                DnsUpstreamResolver.UpstreamResult res = DnsUpstreamResolver.resolve(this, activeConfig, pkt.payload);
                if (res.wireResponse == null || res.wireResponse.length < 12) {
                    SkidEventBus.pushLog(new SkidEventBus.LogEntry(
                            "DNS", "WARN", q.qtypeName, q.qname,
                            "FAIL (" + (res.error != null ? res.error : "timeout") + ")", res.latencyMs, false));
                    return;
                }
                dnsReply = res.wireResponse;
                dnsReply[0] = pkt.payload[0];
                dnsReply[1] = pkt.payload[1];

                String ansIp = DnsPacketCodec.extractAnswerSummary(dnsReply);
                String asn = (activeConfig != null && activeConfig.showProviderInfo)
                        ? DnsUpstreamResolver.describeProviderForAnswer(ansIp) : "";
                String summary = ansIp + asn + " via " + res.protocolUsed;
                boolean isNx = DnsPacketCodec.isNxDomain(dnsReply) || ansIp.startsWith("NXDOMAIN");
                boolean upstreamBlocked = isNx || ansIp.startsWith("REFUSED") || ansIp.startsWith("0.0.0.0");

                if (isNx && activeConfig != null && activeConfig.dnscryptSuspiciousLogging) {
                    SkidEventBus.pushLog(new SkidEventBus.LogEntry(
                            "DNS", "WARN", "SUSPICIOUS", q.qname,
                            "NXDOMAIN [Nonexistent zone via " + res.protocolUsed + "]", res.latencyMs, true));
                }

                boolean ignoredType = activeConfig != null && activeConfig.ignoredQtypes.contains(q.qtypeName);
                if (!ignoredType || upstreamBlocked) {
                    SkidEventBus.recordDnsEvent(q.qtypeName, q.qname, summary, res.latencyMs, upstreamBlocked);
                }
            }

            byte[] replyIpPacket = DnsPacketCodec.buildTunUdpReply(pkt, dnsReply);
            if (replyIpPacket != null) {
                synchronized (writeLock) {
                    tunOut.write(replyIpPacket);
                    tunOut.flush();
                }
                PcapRecorder.recordPacket(replyIpPacket, 0, replyIpPacket.length);
                SkidEventBus.bytesIn.addAndGet(replyIpPacket.length);
            }
        } catch (Exception ignored) {
        }
    }

    public static void syncLoggingFlags(SkidConfig cfg) {
        if (cfg == null) return;
        SkidEventBus.onDeviceLoggingEnabled = cfg.enableOnDeviceLogging;
        SkidEventBus.minLogLevel = cfg.logLevel;
        SkidEventBus.firewallBubbleEnabled = cfg.firewallBubble;
    }

    private synchronized void stopEngine(String reason) {
        engineActive.set(false);
        enginePaused.set(false);
        releaseWakeLock();
        PcapRecorder.stop();
        if (keepaliveScheduler != null) {
            keepaliveScheduler.shutdownNow();
            keepaliveScheduler = null;
        }
        if (localProxy != null) {
            localProxy.stop();
        }
        if (tunReaderThread != null) {
            tunReaderThread.interrupt();
            tunReaderThread = null;
        }
        if (dnsWorkerPool != null) {
            dnsWorkerPool.shutdownNow();
            dnsWorkerPool = null;
        }
        if (tunInterface != null) {
            try { tunInterface.close(); } catch (Exception ignored) {}
            tunInterface = null;
        }
        try {
            stopForeground(true);
        } catch (Exception ignored) {
        }
        SkidEventBus.setState(SkidEventBus.EngineState.DISENGAGED, reason);
        SkidEventBus.logSystem("SYSTEM", "Engine Stopped", reason);
    }

    @Override
    public void onRevoke() {
        stopEngine("VPN revoked by Android OS");
        super.onRevoke();
    }

    @Override
    public void onDestroy() {
        stopEngine("Service stopped");
        super.onDestroy();
    }

    private static String applyAddressesToBuilder(Builder builder, SkidConfig cfg) {
        String primary = "10.213.37.2/24";
        boolean wantV4 = !"IPv6".equalsIgnoreCase(cfg.ipVersion);
        boolean wantV6 = !"IPv4".equalsIgnoreCase(cfg.ipVersion);
        boolean addedAny = false;
        for (String raw : cfg.addresses) {
            try {
                String s = raw.trim();
                boolean isV6 = s.contains(":");
                if (isV6 && !wantV6) continue;
                if (!isV6 && !wantV4) continue;
                int slash = s.indexOf('/');
                String ip = slash > 0 ? s.substring(0, slash).trim() : s;
                int prefix = slash > 0 ? Integer.parseInt(s.substring(slash + 1).trim()) : (isV6 ? 64 : 24);
                builder.addAddress(ip, prefix);
                if (!addedAny) {
                    primary = ip + "/" + prefix;
                    addedAny = true;
                }
            } catch (Exception ignored) {
            }
        }
        if (!addedAny) {
            if (wantV4) {
                builder.addAddress("10.213.37.2", 24);
                primary = "10.213.37.2/24";
            } else {
                builder.addAddress("fd00:534b:4944::2", 64);
                primary = "fd00:534b:4944::2/64";
            }
        }
        return primary;
    }

    private static boolean usesGooglePlainUpstream(SkidConfig cfg) {
        return ("DNS-Proxy".equalsIgnoreCase(cfg.dnsMode) && cfg.dnsProxy != null
                && (cfg.dnsProxy.contains("8.8.8.8") || cfg.dnsProxy.equalsIgnoreCase("Google")))
                || "System DNS".equalsIgnoreCase(cfg.globalDnsCategory);
    }

    public static String describeDnsSelection(SkidConfig cfg) {
        if (cfg == null) return "Rethink DNS";
        if ("System DNS".equalsIgnoreCase(cfg.globalDnsCategory)) return "System DNS";
        if ("Smart DNS".equalsIgnoreCase(cfg.globalDnsCategory)) return "Smart DNS (DoT + DoH)";
        if (!cfg.customDnsServer.isEmpty() && !cfg.customDnsServer.get(0).isEmpty()) {
            return "Custom • " + cfg.customDnsServer.get(0);
        }
        if ("DNS-over-TLS".equalsIgnoreCase(cfg.dnsMode)) {
            return "DoT • " + cfg.dnsOverTls;
        } else if ("DNSCrypt".equalsIgnoreCase(cfg.dnsMode)) {
            return "DNSCrypt • " + cfg.dnsCrypt;
        } else if ("DNS-Proxy".equalsIgnoreCase(cfg.dnsMode)) {
            return "Proxy • " + (cfg.torEnabled ? "Orbot :5400" : cfg.dnsProxy);
        }
        return "Rethink • " + cfg.rethinkDoh.replace("https://sky.rethinkdns.com/", "sky/");
    }

    private void ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                NotificationChannel ch = new NotificationChannel(
                        CHANNEL_ID,
                        "SkidwareDNS Active Tunnel & Firewall",
                        NotificationManager.IMPORTANCE_LOW);
                ch.setDescription("Displays live SkidwareDNS VPN, DNS, Proxy, and Firewall status");
                nm.createNotificationChannel(ch);
            }
        }
    }

    private void promoteToForeground(String text) {
        Notification notif = buildNotification(text);
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIF_ID, notif);
        }
    }

    private void updateForegroundNotification() {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null && engineActive.get()) {
            String line = (enginePaused.get() ? "[PAUSED] " : "")
                    + describeDnsSelection(activeConfig)
                    + " | OK:" + SkidEventBus.queriesResolved.get()
                    + " | Blk:" + SkidEventBus.threatsBlocked.get();
            nm.notify(NOTIF_ID, buildNotification(line));
        }
    }

    private Notification buildNotification(String contentText) {
        int piFlags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;

        Intent openIntent = new Intent(this, MainActivity.class);
        PendingIntent openPi = PendingIntent.getActivity(this, 1, openIntent, piFlags);

        Intent stopIntent = new Intent(this, SkidVpnService.class).setAction(ACTION_STOP);
        PendingIntent stopPi = PendingIntent.getService(this, 2, stopIntent, piFlags);

        Intent pauseOrResume = new Intent(this, SkidVpnService.class)
                .setAction(enginePaused.get() ? ACTION_RESUME : ACTION_PAUSE);
        PendingIntent pausePi = PendingIntent.getService(this, 3, pauseOrResume, piFlags);

        Notification.Builder nb;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nb = new Notification.Builder(this, CHANNEL_ID);
        } else {
            nb = new Notification.Builder(this);
        }

        String title = "SkidwareDNS • " + (activeConfig != null ? activeConfig.name : "Active");
        nb.setContentTitle(title)
                .setContentText(contentText)
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setOngoing(true)
                .setContentIntent(openPi)
                .addAction(new Notification.Action.Builder(
                        android.R.drawable.ic_media_pause, enginePaused.get() ? "Resume" : "Pause", pausePi).build())
                .addAction(new Notification.Action.Builder(
                        android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPi).build());

        return nb.build();
    }
}
