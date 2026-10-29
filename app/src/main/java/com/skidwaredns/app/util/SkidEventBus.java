package com.skidwaredns.app.util;

import android.os.Handler;
import android.os.Looper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe telemetry, TCP/UDP/DNS counters, Firewall Bubble notifier, and state bus.
 */
public final class SkidEventBus {

    public enum EngineState {
        DISENGAGED,
        ENGAGING,
        TUN_ACTIVE,
        PROXY_ONLY_ACTIVE,
        PAUSED,
        ERROR
    }

    public static final class LogEntry {
        public final long timestampMs;
        public final String timeFormatted;
        public final String category;   // DNS, BLOCKED, TCP, UDP, PROXY, TUNNEL, SYSTEM
        public final String level;      // VERBOSE, DEBUG, INFO, WARN, ERROR
        public final String qtype;      // A, AAAA, HTTPS, TCP, UDP, SOCKS5, DPI
        public final String target;     // Domain or IP:Port
        public final String result;     // Resolved IP + ASN, NXDOMAIN, Chain status
        public final long latencyMs;
        public final boolean blocked;

        public LogEntry(String category, String qtype, String target, String result, long latencyMs, boolean blocked) {
            this(category, blocked ? "WARN" : "INFO", qtype, target, result, latencyMs, blocked);
        }

        public LogEntry(String category, String level, String qtype, String target, String result, long latencyMs, boolean blocked) {
            this.timestampMs = System.currentTimeMillis();
            this.timeFormatted = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(new Date(this.timestampMs));
            this.category = category != null ? category : "SYSTEM";
            this.level = level != null ? level : "INFO";
            this.qtype = qtype != null ? qtype : "-";
            this.target = target != null ? target : "";
            this.result = result != null ? result : "";
            this.latencyMs = latencyMs;
            this.blocked = blocked;
        }
    }

    public interface Listener {
        void onStateChanged(EngineState state, String statusLine);
        void onTelemetryUpdated();
        void onLogAdded(LogEntry entry);
        void onFirewallBubbleAlert(String target, String rule);
    }

    private static final int MAX_LOGS = 450;
    private static final CopyOnWriteArrayList<Listener> LISTENERS = new CopyOnWriteArrayList<>();
    private static final List<LogEntry> LOG_RING = Collections.synchronizedList(new ArrayList<LogEntry>());
    private static final Handler MAIN_HANDLER = createMainHandlerSafe();

    private static Handler createMainHandlerSafe() {
        try {
            Looper main = Looper.getMainLooper();
            return main != null ? new Handler(main) : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void postUi(Runnable r) {
        if (MAIN_HANDLER != null) {
            MAIN_HANDLER.post(r);
        } else {
            r.run();
        }
    }

    private static volatile EngineState currentState = EngineState.DISENGAGED;
    private static volatile String statusLine = "NOT PROTECTED";
    private static volatile String activeProfileName = "RDNS-Stealth-Core";
    private static volatile String activeDnsBadge = "Rethink DNS (RDNS Default)";
    private static volatile String activeTunnelBadge = "Hysteria / TLS/SSL (Obfs)";
    private static volatile String activeTunAddress = "10.213.37.2/24";
    private static volatile long connectedSinceMs = 0L;

    public static volatile boolean onDeviceLoggingEnabled = true;
    public static volatile String minLogLevel = "Info";
    public static volatile boolean firewallBubbleEnabled = false;

    public static final AtomicLong queriesResolved = new AtomicLong(0);
    public static final AtomicLong dnsCacheHits = new AtomicLong(0);
    public static final AtomicLong threatsBlocked = new AtomicLong(0);
    public static final AtomicLong proxySessions = new AtomicLong(0);
    public static final AtomicLong tcpStreamsHandled = new AtomicLong(0);
    public static final AtomicLong udpPacketsHandled = new AtomicLong(0);
    public static final AtomicLong dpiDesyncApplied = new AtomicLong(0);
    public static final AtomicLong bytesIn = new AtomicLong(0);
    public static final AtomicLong bytesOut = new AtomicLong(0);
    public static final AtomicLong lastLatencyMs = new AtomicLong(0);

    private SkidEventBus() {}

    public static void addListener(Listener listener) {
        if (listener != null && !LISTENERS.contains(listener)) {
            LISTENERS.add(listener);
        }
    }

    public static void removeListener(Listener listener) {
        LISTENERS.remove(listener);
    }

    public static EngineState getState() {
        return currentState;
    }

    public static boolean isRunning() {
        return currentState == EngineState.TUN_ACTIVE
                || currentState == EngineState.PROXY_ONLY_ACTIVE
                || currentState == EngineState.PAUSED;
    }

    public static boolean isPaused() {
        return currentState == EngineState.PAUSED;
    }

    public static String getStatusLine() {
        return statusLine;
    }

    public static String getActiveProfileName() {
        return activeProfileName;
    }

    public static void setActiveProfileName(String name) {
        activeProfileName = (name == null || name.trim().isEmpty()) ? "Untitled.skid" : name.trim();
        notifyTelemetry();
    }

    public static String getActiveDnsBadge() {
        return activeDnsBadge;
    }

    public static void setActiveDnsBadge(String badge) {
        activeDnsBadge = badge != null ? badge : "";
        notifyTelemetry();
    }

    public static String getActiveTunnelBadge() {
        return activeTunnelBadge;
    }

    public static void setActiveTunnelBadge(String badge) {
        activeTunnelBadge = badge != null ? badge : "";
        notifyTelemetry();
    }

    public static String getActiveTunAddress() {
        return activeTunAddress;
    }

    public static void setActiveTunAddress(String addr) {
        activeTunAddress = addr != null ? addr : "10.213.37.2/24";
        notifyTelemetry();
    }

    public static long getConnectedSinceMs() {
        return connectedSinceMs;
    }

    public static void setState(final EngineState newState, final String message) {
        currentState = newState;
        statusLine = message != null ? message : newState.name();
        if (newState == EngineState.TUN_ACTIVE || newState == EngineState.PROXY_ONLY_ACTIVE) {
            if (connectedSinceMs == 0L) {
                connectedSinceMs = System.currentTimeMillis();
            }
        } else if (newState == EngineState.DISENGAGED || newState == EngineState.ERROR) {
            connectedSinceMs = 0L;
        }
        postUi(new Runnable() {
            @Override
            public void run() {
                for (Listener l : LISTENERS) {
                    l.onStateChanged(newState, statusLine);
                }
            }
        });
    }

    public static void recordDnsEvent(final String qtype, final String domain, final String answer, long latency, boolean blocked) {
        udpPacketsHandled.incrementAndGet();
        if (blocked) {
            threatsBlocked.incrementAndGet();
            if (firewallBubbleEnabled) {
                postUi(new Runnable() {
                    @Override
                    public void run() {
                        for (Listener l : LISTENERS) {
                            l.onFirewallBubbleAlert(domain, answer);
                        }
                    }
                });
            }
        } else {
            queriesResolved.incrementAndGet();
            if (latency >= 0) {
                lastLatencyMs.set(latency);
            }
        }
        pushLog(new LogEntry(blocked ? "BLOCKED" : "DNS", blocked ? "WARN" : "INFO",
                qtype, domain, answer, latency, blocked));
    }

    public static void logSystem(String category, String target, String detail) {
        pushLog(new LogEntry(category, "INFO", "SYS", target, detail, -1, false));
    }

    public static void logDebug(String category, String proto, String target, String detail) {
        pushLog(new LogEntry(category, "DEBUG", proto, target, detail, -1, false));
    }

    public static void pushLog(final LogEntry entry) {
        if (!onDeviceLoggingEnabled) {
            notifyTelemetry();
            return;
        }
        if (!passesLevel(entry.level, minLogLevel)) {
            return;
        }
        synchronized (LOG_RING) {
            LOG_RING.add(0, entry);
            while (LOG_RING.size() > MAX_LOGS) {
                LOG_RING.remove(LOG_RING.size() - 1);
            }
        }
        postUi(new Runnable() {
            @Override
            public void run() {
                for (Listener l : LISTENERS) {
                    l.onLogAdded(entry);
                    l.onTelemetryUpdated();
                }
            }
        });
    }

    private static boolean passesLevel(String entryLvl, String minLvl) {
        return levelRank(entryLvl) >= levelRank(minLvl);
    }

    private static int levelRank(String s) {
        if (s == null) return 2;
        String u = s.trim().toUpperCase(Locale.US);
        if (u.startsWith("VERB")) return 0;
        if (u.startsWith("DEB")) return 1;
        if (u.startsWith("INF")) return 2;
        if (u.startsWith("WAR")) return 3;
        if (u.startsWith("ERR")) return 4;
        return 2;
    }

    public static List<LogEntry> snapshotLogs() {
        synchronized (LOG_RING) {
            return new ArrayList<>(LOG_RING);
        }
    }

    public static void clearLogs() {
        synchronized (LOG_RING) {
            LOG_RING.clear();
        }
        notifyTelemetry();
    }

    public static void resetCounters() {
        queriesResolved.set(0);
        dnsCacheHits.set(0);
        threatsBlocked.set(0);
        proxySessions.set(0);
        tcpStreamsHandled.set(0);
        udpPacketsHandled.set(0);
        dpiDesyncApplied.set(0);
        bytesIn.set(0);
        bytesOut.set(0);
        lastLatencyMs.set(0);
        notifyTelemetry();
    }

    public static void notifyTelemetry() {
        postUi(new Runnable() {
            @Override
            public void run() {
                for (Listener l : LISTENERS) {
                    l.onTelemetryUpdated();
                }
            }
        });
    }
}
