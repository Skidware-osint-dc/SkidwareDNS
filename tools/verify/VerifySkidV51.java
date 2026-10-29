import com.skidwaredns.app.config.SkidConfig;
import com.skidwaredns.app.config.SkidConfigParser;
import com.skidwaredns.app.config.SkidConfigStore;
import com.skidwaredns.app.tunnel.MullvadRelayDirectory;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/** Host-JVM verification + preset exporter for SkidwareDNS v5.1 (5.1.0-skid). */
public final class VerifySkidV51 {

    private static int checks = 0;
    private static int failures = 0;

    private static void check(boolean cond, String what) {
        checks++;
        if (!cond) { failures++; System.out.println("  FAIL: " + what); }
        else System.out.println("  ok  : " + what);
    }

    public static void main(String[] args) throws Exception {
        File outDir = new File(args.length > 0 ? args[0] : "/home/user/SkidwareDNS/configs");
        if (!outDir.exists()) outDir.mkdirs();

        System.out.println("== 1. Built-in Mullvad relay directory ==");
        List<MullvadRelayDirectory.MullvadRelay> relays = MullvadRelayDirectory.BUILTIN_RELAYS;
        check(relays.size() == 9, "9 built-in Mullvad relays (" + relays.size() + ")");
        String[][] expected = {
            {"ee-tll-wg-101", "213.232.112.3", "3503", "Abxw9kZlH8Stt7mWJ2rKeCQjVVdJpLvV6czby23FzmA="},
            {"ee-tll-wg-102", "213.232.112.166", "3514", "TQi/1PpM98XRNEAYkyRR0lspDLg/SAarUkMclPDk52w="},
            {"ee-tll-wg-103", "213.232.112.209", "3515", "jbqIQX8pY4hwoSNw6hZWoUFmyrj/zk1jhlZznPnSkwg="},
            {"ch-zrh-wg-001", "193.32.127.71", "3241", "chvEpRH+05o+ESv8QLzNyY3Phirsym0mUvF03Kt7oCo="},
            {"ch-zrh-wg-002", "193.32.127.70", "3125", "cq9ktmbTZwxYCrCSxV8SItOjRhJhLoYYkPThlifkkVI="},
            {"ch-zrh-wg-003", "193.32.127.72", "3377", "WHsy9kByfDzRtM1V/uDR2BFNUXqouU61vHdHnrgBMzI="},
            {"ch-zrh-wg-201", "179.43.189.66", "3105", "66NPINP4+1AlojLP0J6O9GxdloiegNnGMV4Yit9Kzg0="},
            {"ch-zrh-wg-202", "46.19.136.226", "3106", "gSLSfY2zNFRczxHndeda258z+ayMvd7DqTlKYlKWJUo="},
        };
        for (String[] e : expected) {
            MullvadRelayDirectory.MullvadRelay r = MullvadRelayDirectory.findByHostname(e[0]);
            boolean ok = r != null && e[1].equals(r.ipv4) && Integer.parseInt(e[2]) == r.multihopPort
                    && e[3].equals(r.publicKey) && r.fqdn.equals(e[0] + ".relays.mullvad.net");
            check(ok, e[0] + " " + e[1] + " multihop " + e[2] + " pubkey " + (r == null ? "null" : r.publicKey));
        }
        MullvadRelayDirectory.MullvadRelay br = MullvadRelayDirectory.findByHostname("ch-zrh-br-001");
        check(br != null && br.isBridge() && "193.32.127.117".equals(br.ipv4)
                && "193.32.127.118".equals(br.ipv4V2ray)
                && "SHA256:IE/308Y8M8dOs+e2IP7sl1CtY6TSwt6uD5ygHTU/32w".equals(br.sshSha256)
                && "MD5:7d:ec:f4:5e:f1:d2:5a:33:86:80:6e:a4:2b:7e:18:58".equals(br.sshMd5),
                "ch-zrh-br-001 bridge 193.32.127.117 / V2Ray 193.32.127.118 / SSH pins");
        check(MullvadRelayDirectory.findByHostname("ee-tll-wg-101").daita, "ee-tll-wg-101 DAITA = Yes");
        check(MullvadRelayDirectory.findByHostname("ee-tll-wg-103").speedGbps == 100, "Tallinn speed 100 Gbps");
        check(MullvadRelayDirectory.findByHostname("ch-zrh-wg-201").ownedByMullvad == false, "ch-zrh-wg-201 Rented");
        check("ee-tll-wg-socks5-102.relays.mullvad.net:1080".equals(
                MullvadRelayDirectory.findByHostname("ee-tll-wg-102").socks5Proxy), "Tallinn SOCKS5 :1080");

        System.out.println("== 2. .skid parser round-trip (lossless) ==");
        check(SkidConfigStore.BUILTIN_PRESETS.size() == 20, "20 built-in presets (" + SkidConfigStore.BUILTIN_PRESETS.size() + ")");
        for (SkidConfigStore.PresetTemplate p : SkidConfigStore.BUILTIN_PRESETS) {
            SkidConfigParser.ParseResult pr = SkidConfigParser.parse(p.skidContent);
            check(pr.valid, "parse valid: " + p.filename + " (" + pr.buildSummary() + ")");
            String reser = SkidConfigParser.serialize(pr.config);
            SkidConfigParser.ParseResult pr2 = SkidConfigParser.parse(reser);
            check(pr2.valid, "re-parse valid: " + p.filename);
            SkidConfig a = pr.config, b = pr2.config;
            boolean lossless = a.mullvadEnabled == b.mullvadEnabled && a.mullvadMultihop == b.mullvadMultihop
                    && a.mullvadDaita == b.mullvadDaita && a.mullvadUseSocks5 == b.mullvadUseSocks5
                    && a.mullvadOwnedOnly == b.mullvadOwnedOnly
                    && eq(a.mullvadEntryRelay, b.mullvadEntryRelay) && eq(a.mullvadExitRelay, b.mullvadExitRelay)
                    && eq(a.mullvadBridgeRelay, b.mullvadBridgeRelay) && eq(a.mullvadSshSha256Pin, b.mullvadSshSha256Pin);
            check(lossless, "lossless Mullvad round-trip: " + p.filename);
            File f = new File(outDir, p.filename);
            PrintWriter w = new PrintWriter(f, "UTF-8");
            w.print(p.skidContent);
            w.close();
        }

        System.out.println("== 3. Multihop / DAITA / conf generation ==");
        SkidConfig cfg = new SkidConfig();
        cfg.mullvadEnabled = true;
        cfg.mullvadEntryRelay = "ch-zrh-wg-001";
        cfg.mullvadExitRelay = "ee-tll-wg-102";
        cfg.mullvadMultihop = true;
        cfg.mullvadDaita = true;
        cfg.mullvadUseSocks5 = true;
        String sum = MullvadRelayDirectory.applyToConfig(cfg);
        System.out.println("  summary: " + sum);
        check(sum.contains("193.32.127.71") && sum.contains("3514"), "multihop entry:exitPort applied -> " + sum);
        String conf = MullvadRelayDirectory.generateMullvadNativeConf(cfg);
        check(conf.contains("193.32.127.71:3514"), ".conf uses Entry IPv4 + Exit Multihop Port");
        check(conf.contains("TQi/1PpM98XRNEAYkyRR0lspDLg/SAarUkMclPDk52w="), ".conf carries Exit public key");
        check(MullvadRelayDirectory.generateDaitaMaybenotPaddingBursts(cfg).size() > 0, "DAITA Maybenot padding bursts generated");
        cfg.mullvadDaita = false;
        check(MullvadRelayDirectory.generateDaitaMaybenotPaddingBursts(cfg).isEmpty(), "no padding when DAITA disabled");
        cfg.mullvadBridgeRelay = "ch-zrh-br-001";
        cfg.mullvadSshSha256Pin = MullvadRelayDirectory.findByHostname("ch-zrh-br-001").sshSha256;
        cfg.mullvadDaita = true;
        check(cfg.mullvadSshSha256Pin.startsWith("SHA256:"), "SSH SHA256 bridge pin stored");

        System.out.println();
        System.out.println(failures == 0 ? "PASS: VerifySkidV51 (" + checks + " checks)"
                : "FAIL: " + failures + "/" + checks + " checks");
        if (failures != 0) System.exit(1);
    }

    private static boolean eq(String a, String b) { return a == null ? b == null : a.equals(b); }
}
