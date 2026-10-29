package com.skidwaredns.app.tunnel;

import com.skidwaredns.app.config.SkidConfig;

import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * SkidwareDNS v5.0 NymVPN Mixnet, Profiles & Anti-Censorship Engine:
 *  1. Profiles (190916): Auto (never own country), Anonymous (5-hop Mixnet), Fastest (2-hop WG), Random
 *  2. Mixnet Tuning (190903, 190908): Expected Packet Rate & RTT estimator, Continuous Traffic (2/1/0.7 Mbps),
 *     Background Cover Traffic (Low, Balanced 5x, Medium 10x, High 20x), and Poisson Packet Mixing (0..200 ms)
 *  3. Anti-Censorship (185659): Enhanced connection (QUIC HTTP/3 entry filter + wrapper),
 *     Minimal obfuscation (Client-side AmneziaWG Jc/Jmin/Jmax decoy packets), and Stealth API connect (cover domains)
 */
public final class NymMixnetAndCensorshipEngine {

    private static final SecureRandom RNG = new SecureRandom();

    public static final class NymGateway {
        public final String id;
        public final String host;
        public final String country;
        public final String city;
        public final int baseRttMs;
        public final boolean quicCapable;

        public NymGateway(String id, String host, String country, String city, int baseRttMs, boolean quicCapable) {
            this.id = id;
            this.host = host;
            this.country = country;
            this.city = city;
            this.baseRttMs = baseRttMs;
            this.quicCapable = quicCapable;
        }
    }

    public static final class GatewaySelection {
        public final String profile;
        public final NymGateway entry;
        public final NymGateway exit;
        public final int hopCount;
        public final String summary;

        public GatewaySelection(String profile, NymGateway entry, NymGateway exit, int hopCount, String summary) {
            this.profile = profile;
            this.entry = entry;
            this.exit = exit;
            this.hopCount = hopCount;
            this.summary = summary;
        }
    }

    public static final class MixnetPerformanceEstimate {
        public final double effectiveMbps;
        public final int minRttMs;
        public final String packetRateText;
        public final String latencyText;

        public MixnetPerformanceEstimate(double effectiveMbps, int minRttMs, String packetRateText, String latencyText) {
            this.effectiveMbps = effectiveMbps;
            this.minRttMs = minRttMs;
            this.packetRateText = packetRateText;
            this.latencyText = latencyText;
        }
    }

    private static final NymGateway[] GATEWAY_DIRECTORY = new NymGateway[]{
            new NymGateway("ch-zrh-01", "ch-zurich-01.nym-gw.skid:51820", "CH", "Zurich", 34, true),
            new NymGateway("nl-ams-02", "nl-amsterdam-02.nym-gw.skid:443", "NL", "Amsterdam", 38, true),
            new NymGateway("de-fra-01", "de-frankfurt-01.nym-gw.skid:51820", "DE", "Frankfurt", 41, false),
            new NymGateway("se-sto-01", "se-stockholm-01.nym-gw.skid:443", "SE", "Stockholm", 49, true),
            new NymGateway("is-rek-01", "is-reykjavik-01.nym-gw.skid:443", "IS", "Reykjavik", 62, true),
            new NymGateway("ca-yvr-01", "ca-vancouver-01.nym-gw.skid:443", "CA", "Vancouver", 19, true),
            new NymGateway("us-sea-01", "us-seattle-01.nym-gw.skid:51820", "US", "Seattle", 12, false),
            new NymGateway("jp-tyo-01", "jp-tokyo-01.nym-gw.skid:443", "JP", "Tokyo", 96, true)
    };

    private NymMixnetAndCensorshipEngine() {}

    // =========================================================================
    // 1. EXPECTED PERFORMANCE ESTIMATOR (Screenshots 190903 & 190908)
    // =========================================================================
    public static MixnetPerformanceEstimate estimatePerformance(SkidConfig cfg) {
        double baseMbps = 1.0;
        if (cfg != null && cfg.mixnetContinuousTraffic) {
            String r = cfg.mixnetContinuousRate == null ? "1 Mbps" : cfg.mixnetContinuousRate.trim();
            if (r.startsWith("2")) baseMbps = 2.0;
            else if (r.startsWith("0.7")) baseMbps = 0.7;
            else baseMbps = 1.0;
        } else {
            baseMbps = 2.5; // On-demand burst mode when continuous uniform traffic is toggled off
        }

        double coverPenaltyFactor = 1.0;
        int coverExtraRttMs = 0;
        if (cfg != null && cfg.mixnetCoverTrafficEnabled) {
            String cr = cfg.mixnetCoverTrafficRate == null ? "Low" : cfg.mixnetCoverTrafficRate;
            if (cr.contains("20x") || cr.startsWith("High")) {
                coverPenaltyFactor = 0.45;
                coverExtraRttMs = 110;
            } else if (cr.contains("10x") || cr.startsWith("Medium")) {
                coverPenaltyFactor = 0.65;
                coverExtraRttMs = 60;
            } else if (cr.contains("5x") || cr.startsWith("Balanced")) {
                coverPenaltyFactor = 0.85;
                coverExtraRttMs = 25;
            }
        }

        double effMbps = Math.max(0.2, Math.round((baseMbps * coverPenaltyFactor) * 10.0) / 10.0);

        int delayMs = cfg != null ? Math.max(0, cfg.mixnetPacketMixingDelayMs) : 15;
        int rateRttAdjust = 0;
        if (cfg != null && cfg.mixnetContinuousTraffic) {
            if (baseMbps >= 1.9) rateRttAdjust = -40;
            else if (baseMbps <= 0.8) rateRttAdjust = 70;
        }
        // 5-hop Sphinx mixnet base propagation (600 ms) + 6 mix-node Poisson hops (6 * 15 ms = 90 ms -> 690 ms!)
        int rttMs = Math.max(180, 600 + (6 * delayMs) + coverExtraRttMs + rateRttAdjust);

        String rateStr = String.format(Locale.US, "Up to %.1f Mbps", effMbps);
        String latStr = "At least " + rttMs + " ms";
        return new MixnetPerformanceEstimate(effMbps, rttMs, rateStr, latStr);
    }

    // =========================================================================
    // 2. NYMVPN PROFILES & QUIC-ENTRY SERVER FILTER (Screenshots 190916 & 185659)
    // =========================================================================
    public static GatewaySelection selectGatewaysForProfile(SkidConfig cfg) {
        String prof = (cfg == null || cfg.nymProfile == null) ? "Anonymous" : cfg.nymProfile.trim();
        String ownCountry = (cfg == null || cfg.nymUserCountry == null || cfg.nymUserCountry.isEmpty())
                ? "US" : cfg.nymUserCountry.trim().toUpperCase(Locale.US);
        boolean requireQuicEntry = cfg != null && cfg.nymQuicEnhanced;

        List<NymGateway> entryCandidates = new ArrayList<>();
        List<NymGateway> exitCandidates = new ArrayList<>();
        for (NymGateway gw : GATEWAY_DIRECTORY) {
            if ("Auto".equalsIgnoreCase(prof) && gw.country.equalsIgnoreCase(ownCountry)) {
                continue; // Auto: "Best performing servers, never your own country"
            }
            exitCandidates.add(gw);
            if (!requireQuicEntry || gw.quicCapable) {
                entryCandidates.add(gw); // Enhanced connection (QUIC): "Limits entry server options to QUIC-compatible servers only"
            }
        }
        if (entryCandidates.isEmpty()) entryCandidates.add(GATEWAY_DIRECTORY[0]);
        if (exitCandidates.isEmpty()) exitCandidates.add(GATEWAY_DIRECTORY[0]);

        NymGateway entry;
        NymGateway exit;
        int hops;

        if ("Random".equalsIgnoreCase(prof)) {
            entry = entryCandidates.get(RNG.nextInt(entryCandidates.size()));
            exit = exitCandidates.get(RNG.nextInt(exitCandidates.size()));
            if (exitCandidates.size() > 1 && exit.id.equals(entry.id)) {
                exit = exitCandidates.get((exitCandidates.indexOf(entry) + 1) % exitCandidates.size());
            }
            hops = 2;
        } else if ("Fastest".equalsIgnoreCase(prof)) {
            entry = pickLowestRtt(entryCandidates, null);
            exit = pickLowestRtt(exitCandidates, entry.id);
            hops = 2;
        } else if ("Auto".equalsIgnoreCase(prof)) {
            entry = pickLowestRtt(entryCandidates, null);
            exit = pickLowestRtt(exitCandidates, entry.id);
            hops = 2;
        } else {
            // "Anonymous": "Mixnet mode with closest servers to you" (5-hop Sphinx Mixnet)
            entry = pickLowestRtt(entryCandidates, null);
            exit = pickLowestRtt(exitCandidates, entry.id);
            hops = 5;
        }

        String quicTag = requireQuicEntry ? " [QUIC H3 Entry]" : "";
        String obfsTag = (cfg != null && cfg.amneziaWgMinimalObfs) ? " [AmneziaWG Decoys]" : "";
        String sum = prof + " (" + hops + "-hop): " + entry.city + " (" + entry.country + ") → "
                + exit.city + " (" + exit.country + ")" + quicTag + obfsTag;
        return new GatewaySelection(prof, entry, exit, hops, sum);
    }

    private static NymGateway pickLowestRtt(List<NymGateway> list, String excludeId) {
        NymGateway best = list.get(0);
        for (NymGateway g : list) {
            if (excludeId != null && excludeId.equals(g.id) && list.size() > 1) continue;
            if (g.baseRttMs < best.baseRttMs || (excludeId != null && excludeId.equals(best.id))) {
                best = g;
            }
        }
        return best;
    }

    // =========================================================================
    // 3. POISSON PACKET MIXING & COVER TRAFFIC TIMING
    // =========================================================================
    public static long samplePoissonMixDelayMs(int meanDelayMs) {
        if (meanDelayMs <= 0) return 0L;
        double u = Math.max(1e-6, RNG.nextDouble());
        long sampled = Math.round(-meanDelayMs * Math.log(u));
        return Math.min(meanDelayMs * 3L, Math.max(0L, sampled));
    }

    public static void applyPacketMixingDelayIfActive(SkidConfig cfg) {
        if (cfg == null) return;
        boolean mixActive = "Mixnet-5Hop".equalsIgnoreCase(cfg.mode) || "Anonymous".equalsIgnoreCase(cfg.nymProfile);
        if (!mixActive || cfg.mixnetPacketMixingDelayMs <= 0) return;
        long d = samplePoissonMixDelayMs(cfg.mixnetPacketMixingDelayMs);
        if (d > 0) {
            try { Thread.sleep(d); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
        }
    }

    public static long computeCoverTrafficIntervalMs(SkidConfig cfg) {
        if (cfg == null || !cfg.mixnetCoverTrafficEnabled) return 60_000L;
        String cr = cfg.mixnetCoverTrafficRate == null ? "Low" : cfg.mixnetCoverTrafficRate;
        if (cr.contains("20x") || cr.startsWith("High")) return 3_000L;
        if (cr.contains("10x") || cr.startsWith("Medium")) return 6_000L;
        if (cr.contains("5x") || cr.startsWith("Balanced")) return 12_000L;
        return 25_000L;
    }

    // =========================================================================
    // 4. AMNEZIAWG CLIENT-SIDE DECOY PACKETS & STEALTH API CONNECT (185659)
    // =========================================================================
    public static List<byte[]> generateAmneziaDecoyPackets(SkidConfig cfg) {
        List<byte[]> decoys = new ArrayList<>();
        if (cfg == null || !cfg.amneziaWgMinimalObfs) return decoys;
        int jc = Math.max(1, Math.min(16, cfg.amneziaWgJc));
        int jmin = Math.max(16, cfg.amneziaWgJmin);
        int jmax = Math.max(jmin, cfg.amneziaWgJmax);
        for (int i = 0; i < jc; i++) {
            int len = jmin + (jmax > jmin ? RNG.nextInt(jmax - jmin + 1) : 0);
            byte[] pkt = new byte[len];
            RNG.nextBytes(pkt);
            decoys.add(pkt);
        }
        return decoys;
    }

    public static String probeStealthApiCoverConnect(SkidConfig cfg) {
        String domainsCsv = (cfg != null && cfg.nymStealthCoverDomains != null && !cfg.nymStealthCoverDomains.trim().isEmpty())
                ? cfg.nymStealthCoverDomains : "cloudflare.com,cdn.jsdelivr.net";
        String modeNote = (cfg != null && cfg.nymStealthApiConnect)
                ? "Mode: ALWAYS-ON Cover Domain" : "Mode: Auto-Fallback on Blocking";

        for (String dom : domainsCsv.split("[,;\\s]+")) {
            String clean = dom.trim();
            if (clean.isEmpty()) continue;
            long t0 = System.currentTimeMillis();
            HttpURLConnection conn = null;
            try {
                URL u = new URL("https://" + clean + "/");
                conn = (HttpURLConnection) u.openConnection();
                conn.setRequestMethod("HEAD");
                conn.setInstanceFollowRedirects(false);
                conn.setConnectTimeout(3500);
                conn.setReadTimeout(3500);
                int code = conn.getResponseCode();
                long ms = System.currentTimeMillis() - t0;
                if (code > 0) {
                    return "Stealth API Cover OK via " + clean + " -> HTTP " + code + " (" + ms + " ms) [" + modeNote + "]";
                }
            } catch (Exception ignored) {
            } finally {
                if (conn != null) conn.disconnect();
            }
        }
        return "Stealth API Cover unreachable across [" + domainsCsv + "] (" + modeNote + ")";
    }

    public static String generateNymConfigSnippet(SkidConfig cfg) {
        GatewaySelection sel = selectGatewaysForProfile(cfg);
        MixnetPerformanceEstimate est = estimatePerformance(cfg);
        return "# NymVPN v5.0 (" + sel.profile + " • " + sel.hopCount + "-Hop • " + est.packetRateText + " / " + est.latencyText + ")\n" +
                "[nym_vpnd]\n" +
                "profile = \"" + sel.profile + "\"\n" +
                "entry_gateway = \"" + sel.entry.host + "\" # " + sel.entry.city + " (" + sel.entry.country + ", QUIC=" + sel.entry.quicCapable + ")\n" +
                "exit_gateway = \"" + sel.exit.host + "\" # " + sel.exit.city + " (" + sel.exit.country + ")\n" +
                "quic_transport = " + cfg.nymQuicEnhanced + "\n" +
                "stealth_api_connect = \"" + (cfg.nymStealthApiConnect ? "always" : "fallback") + "\"\n" +
                "cover_domains = [\"" + cfg.nymStealthCoverDomains.replace(",", "\", \"") + "\"]\n\n" +
                "[mixnet_tuning]\n" +
                "send_continuous_traffic = " + cfg.mixnetContinuousTraffic + "\n" +
                "continuous_rate = \"" + cfg.mixnetContinuousRate + "\"\n" +
                "background_cover_enabled = " + cfg.mixnetCoverTrafficEnabled + "\n" +
                "background_cover_rate = \"" + cfg.mixnetCoverTrafficRate + "\"\n" +
                "packet_mixing_delay_ms = " + cfg.mixnetPacketMixingDelayMs + "\n\n" +
                "[amneziawg_client_obfs]\n" +
                "enabled = " + cfg.amneziaWgMinimalObfs + "\n" +
                "Jc = " + cfg.amneziaWgJc + "\n" +
                "Jmin = " + cfg.amneziaWgJmin + "\n" +
                "Jmax = " + cfg.amneziaWgJmax + "\n";
    }
}
