import com.skidwaredns.app.config.SkidConfig;
import com.skidwaredns.app.config.SkidConfigParser;
import com.skidwaredns.app.config.SkidConfigStore;
import com.skidwaredns.app.proxy.PortCatalog;
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
        check(relays.size() == 23, "23 built-in Mullvad relays (" + relays.size() + ")");
        String[][] expected = {
            {"ee-tll-wg-101", "213.232.112.3", "3503", "Abxw9kZlH8Stt7mWJ2rKeCQjVVdJpLvV6czby23FzmA="},
            {"ee-tll-wg-102", "213.232.112.166", "3514", "TQi/1PpM98XRNEAYkyRR0lspDLg/SAarUkMclPDk52w="},
            {"ee-tll-wg-103", "213.232.112.209", "3515", "jbqIQX8pY4hwoSNw6hZWoUFmyrj/zk1jhlZznPnSkwg="},
            {"ch-zrh-wg-001", "193.32.127.71", "3241", "chvEpRH+05o+ESv8QLzNyY3Phirsym0mUvF03Kt7oCo="},
            {"ch-zrh-wg-002", "193.32.127.70", "3125", "cq9ktmbTZwxYCrCSxV8SItOjRhJhLoYYkPThlifkkVI="},
            {"ch-zrh-wg-003", "193.32.127.72", "3377", "WHsy9kByfDzRtM1V/uDR2BFNUXqouU61vHdHnrgBMzI="},
            {"ch-zrh-wg-201", "179.43.189.66", "3105", "66NPINP4+1AlojLP0J6O9GxdloiegNnGMV4Yit9Kzg0="},
            {"ch-zrh-wg-202", "46.19.136.226", "3106", "gSLSfY2zNFRczxHndeda258z+ayMvd7DqTlKYlKWJUo="},
            {"bg-sof-wg-001", "146.70.188.130", "3225", "J8KysHmHZWqtrVKKOppneDXSks/PDsB1XTlRHpwiABA="},
            {"bg-sof-wg-002", "135.136.38.2", "3226", "dg+Fw7GnKvDPBxFpnj1KPoNIu1GakuVoDJjKRni+pRU="},
            {"bg-sof-wg-003", "146.70.188.2", "3100", "ug2e9Pdp5imXjuefrKqmYYzfMnC83whlLBj7eFi81UI="},
            {"cy-nic-wg-001", "195.47.194.131", "3337", "Ae9YcQjcQT+W8MU0EhKXx6KPWo6ticS1NI91e+Zy5GA="},
            {"cy-nic-wg-002", "195.47.194.161", "3338", "LOd1SY9YCHGiJUVT+XdYRdORu6ZMw4CqOKQBW2ElLg8="},
            {"dk-cph-wg-102", "149.88.109.73", "3599", "oFSh/6OxGDc8LY+kCj9SNebPphDGM9UIeln0cseILxs="},
            {"fi-hel-wg-201", "185.65.133.5", "3155", "u+Ir9bnz8PtwXoJGQXvCxz6a+1NChEbBIox8KdWarxk="},
            {"fi-hel-wg-202", "185.65.133.85", "3194", "hAbVKL5Q0285gJ28nMSfztw7/FXJb+cxiKD2NMgPpTM="},
            {"gb-lon-wg-101", "45.66.219.3", "3440", "c63L5Zl92C7K0j3rsHDeEu6ZzEaG9PkA4sZeFRPhgHo="},
            {"gb-lon-wg-102", "45.66.219.166", "3441", "KT35IIlJExy9SSR1PqjnjE9wZ7HP2y52J5wjjRBUSjo="},
            {"gb-lon-wg-103", "45.66.219.209", "3442", "FmapWWiwyLpMsKf9jG3pgTSM3GD609nQowqEWFtZoAc="},
            {"il-tlv-wg-101", "169.150.227.197", "3513", "XOedjVJaT2IrEDJbzvtZeL4hP5uPRHzFxvD1cwVwUFo="},
            {"il-tlv-wg-102", "169.150.227.210", "3533", "UNeML4rXjvOerAstTNf4gG5B+OfjVzjSQrWE6mrswD0="},
            {"il-tlv-wg-103", "169.150.227.222", "3534", "11FJ/NY3jaAw1PSYG9w7bxsMxAzlI+1p8/juh1LJPT0="},
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
        check(MullvadRelayDirectory.findByHostname("bg-sof-wg-001").daita, "bg-sof-wg-001 DAITA = Yes");
        check(MullvadRelayDirectory.findByHostname("gb-lon-wg-101").speedGbps == 100, "London speed 100 Gbps");
        check(MullvadRelayDirectory.findByHostname("dk-cph-wg-102").ownedByMullvad, "Copenhagen Owned by Mullvad");
        check("bg-sof-wg-socks5-003.relays.mullvad.net:1080".equals(
                MullvadRelayDirectory.findByHostname("bg-sof-wg-003").socks5Proxy), "Sofia-003 SOCKS5 :1080");
        check(SkidConfigStore.BUILTIN_PRESETS.size() == 23, "23 built-in presets (" + SkidConfigStore.BUILTIN_PRESETS.size() + ")");
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

        System.out.println("== 4. v5.4 socket / HTTPS / port .skid round-trip ==");
        SkidConfig t = new SkidConfig();
        t.tcpCongestion = "BBR";
        t.tcpConnectTimeoutMs = 15000;
        t.tcpCork = true;
        t.udpRecvTimeoutMs = 5000;
        t.udpBroadcast = true;
        t.httpsMinTls = "TLS 1.3";
        t.httpsAlpn = "h2";
        t.httpsHttp2 = false;
        t.portBlockList = "445,135";
        t.portAllowList = "443,853";
        t.portForwardMap = "8080>80";
        t.portKnockSequence = "7000,8000";
        t.ephemeralPortMin = 30000;
        t.portHopEnabled = true;
        t.remapDns53toDoH = true;
        String ser = SkidConfigParser.serialize(t);
        SkidConfig t2 = SkidConfigParser.parse(ser).config;
        check("BBR".equals(t2.tcpCongestion), "TCP-Congestion=BBR");
        check(t2.tcpConnectTimeoutMs == 15000, "TCP-ConnectTimeout-Ms=15000");
        check(t2.tcpCork, "TCP-Cork=true");
        check(t2.udpRecvTimeoutMs == 5000, "UDP-RecvTimeout-Ms=5000");
        check(t2.udpBroadcast, "UDP-Broadcast=true");
        check("TLS 1.3".equals(t2.httpsMinTls), "HTTPS-MinTLS=TLS 1.3");
        check("h2".equals(t2.httpsAlpn), "HTTPS-ALPN=h2");
        check(!t2.httpsHttp2, "HTTPS-HTTP2=false");
        check("445,135".equals(t2.portBlockList), "Port-Block-List");
        check("443,853".equals(t2.portAllowList), "Port-Allow-List");
        check("8080>80".equals(t2.portForwardMap), "Port-Forward-Map");
        check("7000,8000".equals(t2.portKnockSequence), "Port-Knock-Sequence");
        check(t2.ephemeralPortMin == 30000, "Ephemeral-Port-Min");
        check(t2.portHopEnabled, "Port-Hop-Enabled");
        check(t2.remapDns53toDoH, "Remap-DNS53-to-DoH");
        check(t2.bindLoopbackOnly, "Bind-Loopback-Only default true");
        check(t2.httpsEndpointId, "HTTPS-EndpointId default true");
        SkidConfigParser.ParseResult oldRt = SkidConfigParser.parse(
                "Name=legacy\nTCP-NoDelay=false\nUDP-QUIC-Policy=Block\n");
        check(oldRt.valid && !oldRt.config.tcpNoDelay && oldRt.config.udpQuicPolicy.contains("Block"),
                "v5.3 TCP/UDP keys still parse");
        String oldSer = SkidConfigParser.serialize(oldRt.config);
        check(oldSer.contains("TCP-NoDelay=false") && oldSer.contains("HTTPS-MinTLS="),
                "legacy serialize keeps old keys and adds v5.4 keys");

        System.out.println("== 5. Open Port catalog 1-64000 (IANA + IpCisco + Wikipedia) ==");
        PortCatalog.Info http = PortCatalog.lookup(80);
        check(http.assigned && http.name.toLowerCase().contains("http"), "port 80 is HTTP");
        check(PortCatalog.lookup(22).name.toLowerCase().contains("ssh"), "port 22 is SSH");
        check(PortCatalog.lookup(443).name.toLowerCase().contains("https"), "port 443 is HTTPS");
        check(PortCatalog.lookup(53).name.toLowerCase().contains("domain")
                || PortCatalog.lookup(53).name.toLowerCase().contains("dns"), "port 53 is DNS");
        check(PortCatalog.page(1, 50).size() == 50, "page size 50");
        check(PortCatalog.page(63951, 50).size() == 50, "last page 63951-64000");
        check(PortCatalog.search("ssh", 10).size() > 0, "search ssh");
        check(PortCatalog.search("8080", 5).get(0).port == 8080, "search 8080");
        check(!PortCatalog.lookup(4242).about.isEmpty(), "port 4242 still has a description");
        check(PortCatalog.lookup(0).name.contains("Out of range"), "port 0 out of range");
        check(PortCatalog.lookup(64001).name.contains("Out of range"), "port 64001 out of range");
        check(PortCatalog.lookup(64000).port == 64000, "port 64000 in range");
        check(PortCatalog.namedCount() > 5000, "named catalog > 5000 (got " + PortCatalog.namedCount() + ")");
        check(PortCatalog.lookup(51820).name.toLowerCase().contains("wireguard"), "51820 WireGuard");
        check(PortCatalog.lookup(25565).name.toLowerCase().contains("minecraft"), "25565 Minecraft (IpCisco)");
        check(PortCatalog.lookup(27017).name.toLowerCase().contains("mongo"), "27017 MongoDB (IpCisco)");
        check(PortCatalog.lookup(11371).name.toLowerCase().contains("pgp"), "11371 PGP keyserver (IpCisco)");
        check(PortCatalog.MAX == 64000, "MAX=64000");
        SkidConfig op = new SkidConfig();
        op.openListenPorts.add("8080/tcp");
        op.openPortBindMode = "0.0.0.0";
        op.openPortHold = true;
        String opSer = SkidConfigParser.serialize(op);
        SkidConfig op2 = SkidConfigParser.parse(opSer).config;
        check(op2.openListenPorts.contains("8080/tcp"), "OPEN LISTEN PORTS round-trip");
        check("0.0.0.0".equals(op2.openPortBindMode), "Open-Port-Bind-Mode round-trip");
        check(op2.openPortHold, "Open-Port-Hold round-trip");
        check(op2.openPortBanner, "Open-Port-Banner default true");

        System.out.println();
        System.out.println(failures == 0 ? "PASS: VerifySkidV51 (" + checks + " checks)"
                : "FAIL: " + failures + "/" + checks + " checks");
        if (failures != 0) System.exit(1);
    }

    private static boolean eq(String a, String b) { return a == null ? b == null : a.equals(b); }
}
