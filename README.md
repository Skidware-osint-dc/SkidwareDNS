# SkidwareDNS

**v6.0.0-skid** · `versionCode 600` · Android 7+ (API 24) · target SDK 34

A local Android VPN, encrypted DNS router, firewall, and tunnel forge in **one APK**. Traffic stays on the device unless you turn on a tunnel. Profiles are plain-text **`.skid`** files you can export, share, and re-import.

Package: `com.skidwaredns.app`  
Signed APK: [`SkidwareDNS.apk`](./SkidwareDNS.apk) (v2 + v3, ~546K)

---

## What it is

SkidwareDNS captures DNS and (optionally) IP traffic through Android `VpnService`, then applies whatever the active `.skid` profile says: blocklists, encrypted DNS, SOCKS/HTTP proxies, WireGuard / Mullvad, SSH, Xray, Hysteria, Tor / Orbot, I2P, mixnet, DPI shaping, and a LAN tools hub.

It is **not**:

- a hosted VPN provider or a cloud account
- a wrapper around someone else’s APK
- a root firewall
- a full Tor / i2pd / Xray binary bundle — those stacks are configured here and talked to as local SOCKS/HTTP where they exist

Nothing is uploaded. Blocklists, logs, and `.skid` profiles live on the phone.

---

## Install

1. Download [`SkidwareDNS.apk`](./SkidwareDNS.apk) from this repo (or build it — see [Building](#building)).
2. Allow install from the source you used.
3. Open the app. The **first-run intro** (six screens) explains DNS, modes, tunnels, and tools. Skip anytime; replay it from **Configure → Settings → Replay first-run intro**.
4. Pick a mode on Home (chevron next to **START**), then tap **START**. Android will ask for the VPN consent dialog.

Min SDK 24 (Android 7). Target 34.

---

## Five tabs

| Tab | What you get |
| --- | --- |
| **Home** | Protection state, live **allowed vs blocked** spark chart (tap a series to inspect), 6-line recent log, live ↑/↓ bytes (B / KB / MB / GB) and packet count, START / STOP / PAUSE, **15 operating modes** |
| **Stats** | Larger live chart, six tiles (DNS OK, blocked, TCP, UDP, bytes, RTT), mode + stack badges, flush DNS cache / reset counters, full telemetry dump |
| **SKID** | `.skid` Config Studio — 23 built-in presets, raw editor, import / export / share |
| **Configure** | Searchable hub: Mullvad, NymVPN, HTTP Injector, privacy stack, **Tools**, **Open Port 1–64000**, **Stack Lab**, HTTPS/TLS, DNS, firewall, apps, network, settings |
| **Logs** | Packet / debug log and PCAP export |

Bottom nav: Home · Stats · SKID · Configure · Logs.

---

## Operating modes (15)

The Home chevron sets `Mode=` and a small set of flags so **START** does the obvious thing.

| Mode | VPN tun | Default route | Intent |
| --- | --- | --- | --- |
| DNS+Firewall | yes | no | Resolve + blocklists, no full-tunnel |
| DNS-Only | yes | no | DNS capture only |
| Firewall-Only | yes | yes | Blocklists + tun, no proxy |
| VPN+DNS+Firewall | yes | yes | Full-tunnel |
| Proxy-Only | **no** | no | Local SOCKS/HTTP only |
| WireGuard-VPN | yes | yes | WireGuard profile as the tunnel |
| Mullvad-WG | yes | yes | Mullvad relay + WireGuard |
| SSH-Tunnel | **no** | no | SSH SOCKS jump |
| Tor+DNSCrypt+VPN | yes | yes | Tor + DNSCrypt inside a tun |
| Tor+DNSCrypt+I2P | yes | yes | Tor + DNSCrypt + I2P |
| I2P-Only | **no** | no | Purple I2P proxies |
| Mixnet-5Hop | yes | yes | Nym 5-hop mixnet |
| Stealth-DPI | yes | yes | Full-tunnel + always DPI desync |
| Lockdown | yes | yes | Strict default route + proxy lockdown |
| LAN-Tools | **no** | no | Local FTP / Open Port / scanners |

---

## Features

### DNS & firewall

- DoH / DoT / DNSCrypt / ODoH-style resolver selection, DNS booster cache, split DNS, leak prevention
- On-device blocklist packs, per-domain / per-port / per-app rules, bypass / torify UIDs
- Universal rules: block on lock, metered, LAN, newly installed apps, non-DNS UDP, unknown DNS
- Firewall bubble banner when a rule fires
- LBox DNS templates, cloaking / forwarding / whitelist / IP blacklist

### Tunnels & privacy

- **WireGuard** profile (endpoint, keys, AllowedIPs, DNS, listen port, keepalive, MTU, PSK)
- **Mullvad** — 23 pinned relays (Tallinn 100G DAITA, Zurich, Sofia, London, Copenhagen, Helsinki, Nicosia, Tel Aviv, Zurich SSH/V2Ray bridge), live API sync, true multihop (`EntryIPv4:ExitMultihopPort`), DAITA padding, SOCKS5 `:1080`
- **NymVPN** — Auto / Anonymous / Fastest / Random profiles, mixnet sliders (continuous traffic, cover, packet mixing), QUIC + AmneziaWG obfuscation, stealth API cover domains
- **HTTP Injector / Tunnel Forge** — SSH + UDPGW, V2Ray/Xray (VMess/VLESS/Trojan/SS/SOCKS/WG) with 8 transports + Reality, Hysteria 1/2, Shadowsocks 2022, custom HTTP payload, share URI import (`vless://`, `vmess://`, `trojan://`, `ss://`, `hy2://`)
- **Tor / Orbot** — SOCKS / HTTP / control ports, bridges, Snowflake, isolation, NEWNYM on start
- **Purple I2P** — HTTP `:4444` / SOCKS `:4447` / SAM, tunnel length & quantity, hidden mode
- Jump hosts, MASQUE, JA3 presets, Fake-IP `198.18.0.0/15`, DPI desync / TLS record split / mixed-case SNI

### Stack Lab (v6.0)

Configure → **Stack Lab** (hub tile). Extra knobs that persist as `.skid` keys and apply where the platform allows:

| Group | Settings |
| --- | --- |
| **TCP** | SO_LINGER, keepalive probes, SACK, timestamps, TIME_WAIT reuse, FIN timeout, pacing, SYN backlog |
| **UDP** | DF, GRO, hairpin NAT, multicast TTL, NAT table size, connect timeout |
| **Socket** | linger, SO_PRIORITY, SO_MARK, dual-stack, SO_BINDTODEVICE |
| **SOCKS5 / HTTP CONNECT** | user/pass, UDP ASSOCIATE, remote DNS, connect timeout |
| **WireGuard** | private key, AllowedIPs, DNS, listen port, keepalive, MTU, PSK |
| **Tor & Orbot** | SOCKS/HTTP/control ports, VPN mode, start-on-connect, NEWNYM, circuit timeout |
| **I2P** | inbound/outbound length & quantity, hidden mode |
| **VPN & network** | always-on hint, block IPv6, Happy Eyeballs + delay, captive-portal bypass, prefer Wi-Fi |
| **TLS / HTTPS** | max TLS, 1.3-only, OCSP stapling, strict pin, GREASE, ECH, HTTP/3 alt-svc, trust system CAs |

Linger and TLS 1.3-only are applied on real sockets via `SocketStack`.

### Tools hub (v5.7) — six LAN utilities, one APK

Reimplemented in Java. Not a copy of those projects, and not GPLv3 code from them.

| Screen | Inspired by | What it does here |
| --- | --- | --- |
| **FTPS / file share** | [drweb86/dotnet-ftps-server](https://github.com/drweb86/dotnet-ftps-server) | Password FTP on `:2121` (PASV, UTF-8). TLS/AUTH not bundled. |
| **Network scanner** | [usamaiqb/network-scanner](https://github.com/usamaiqb/network-scanner) | This `/24` TCP sweep + mDNS + SSDP. No root. |
| **Shellwave SSH** | [LordOfPolls/Shellwave](https://github.com/LordOfPolls/Shellwave) | Banner, password exec, SFTP ls (JSch 0.1.55). Not a full terminal. |
| **Knock on Ports** | [impalex/knockonports](https://github.com/impalex/knockonports) | TCP/UDP knockd sequences with delay. ICMP skipped (needs root). |
| **Port checker** | [CodeDead/Advanced-PortChecker-android](https://github.com/CodeDead/Advanced-PortChecker-android) | TCP/UDP range on one host (max 4096). Catalog names attached. |
| **Folder mirror** | [He11PC/android-mirror](https://github.com/He11PC/android-mirror) | One-way copy: local, `ftp://`, `sftp:`, `http://`. No SMB/NFS. |

Settings persist as `Tools-*` keys. Only scan / knock / check hosts you own.

### Open Port 1–64000 (v5.6)

Binds TCP/UDP **1–64000** on this device’s LAN IP. Catalog names 6000+ services from [IANA](https://www.iana.org/assignments/service-names-port-numbers/service-names-port-numbers.xhtml), [IpCisco](https://ipcisco.com/lesson/network-ports/), and [Wikipedia’s TCP/UDP port list](https://en.wikipedia.org/wiki/List_of_TCP_and_UDP_port_numbers). Other ports get an RFC 6335 range note. Not a proxy: accepted clients get a banner, then the socket closes.

Jump chips include **WG 51820**. Bind Device-IP / `0.0.0.0` / loopback. Opened ports persist as `OPEN LISTEN PORTS []`.

### UI

- AMOLED matte + sky `#6AD2FF`
- Lucide-style vector icons (`SkidIcons`, 78 glyphs, 2.25px stroke)
- Accent-tinted wells, card wash, sky selected tab
- Hub tiles stagger-fade; Stats / Home cards fade in; first-run overlay

---

## `.skid` config format

Human-readable. Two shapes:

```
# comments and === section banners ===
DNS SERVERS [https://sky.rethinkdns.com/rec, tls://1dot1dot1dot1.cloudflare-dns.com]
FIREWALL RULES [BLOCK_DOMAIN:doubleclick.net, BLOCK_PORT:445]

Mode=DNS+Firewall
Name=RDNS-Stealth-Core
TCP-NoDelay=true
HTTPS-MinTLS=TLS 1.2
WG-Keepalive-Sec=25
```

- Bracket arrays: `KEY [a, b, c]`
- Directives: `Key-Name=value` (booleans `true`/`false`, ints, strings)
- Parser normalizes keys (strips non-alphanumerics, lowercases) so `TCP-NoDelay` and `tcpnodelay` match
- Unknown keys are ignored; empty values keep defaults
- Round-trip is lossless for known keys (`SkidConfigParser.serialize` ↔ `parse`)

Import from Files / Share (`.skid` intent filter) or the SKID tab. Export / share from the same place. Active profile is stored in `SharedPreferences` as `active_skid_raw`.

---

## Built-in profiles (`configs/`)

| File | Profile |
| --- | --- |
| `rdns_fortress.skid` | RethinkDNS Security + DNS Booster + DPI Auto |
| `skidwareproxy_dpi_desync.skid` | WireGuard + Flood WG + Hybrid DPI Desync |
| `invizible_tor_dnscrypt.skid` | Tor + DNSCrypt Onion Shield |
| `hysteria_tls_obfs.skid` | Hysteria + ControlD DoT + TLS/SSL (Obfs) |
| `xray_http_payload.skid` | Xray WebSocket + HTTP payload forge |
| `dnstt_ssh_stealth.skid` | SSH over DNS (DNSTT SlowDNS) |
| `nym_strongswan_mixnet.skid` | Nym 5-hop + strongSwan IKEv2 |
| `lbox_tls_record_frag.skid` | LBox templates + gVisor + TLS record fragment |
| `invizible_full_trimodule.skid` | DNSCrypt + Tor + Purple I2P + ARP MITM |
| `megaproxy_masque_jumphost.skid` | HTTPS jump + MASQUE-H3 + JA3 + UID check |
| `lumine_hysteria2_fakeip.skid` | Fake-IP `198.18.0.0/15` + Hysteria2 |
| `injector_ssh_http_payload.skid` | HTTP Proxy → SSH + UDPGW + header replacer |
| `injector_xray_vless_reality_xhttp.skid` | Xray VLESS + XHTTP + Reality |
| `injector_hysteria2_quicwin_hop.skid` | Hysteria2 + 16 MB QUIC window + port hop |
| `injector_ss2022_blake3_doh_quad9.skid` | Shadowsocks 2022-blake3 + Quad9 DoH |
| `nymvpn_anonymous_mixnet_690ms.skid` | Nym Anonymous 5-hop (~690 ms) |
| `nymvpn_anticensorship_quic_stealth.skid` | Nym QUIC + AmneziaWG + stealth API |
| `mullvad_tallinn_100g_daita.skid` | Tallinn `ee-tll-wg-101` 100G DAITA |
| `mullvad_zurich_to_tallinn_multihop.skid` | Zurich → Tallinn WG multihop |
| `mullvad_zurich_bridge_ssh_v2ray.skid` | Zurich SSH/V2Ray bridge |
| `mullvad_sofia_daita.skid` | Sofia `bg-sof-wg-001` DAITA |
| `mullvad_london_100g.skid` | London `gb-lon-wg-101` 100G |
| `mullvad_copenhagen_to_sofia_multihop.skid` | Copenhagen → Sofia multihop |

---

## Architecture

```
app/src/main/java/com/skidwaredns/app/
  ui/          MainActivity, ConfigureScreens, StackLab, Tools, Open Port,
               Mullvad / Nym / Injector / InviZible screens, LiveChartView,
               OnboardingScreens, SkidIcons, UiFactory
  vpn/         SkidVpnService, Quick Settings tile, boot / shell-script receiver
  dns/         packet codec, upstream resolver, DNSCrypt list manager
  firewall/    SkidFirewallEngine
  proxy/       SocketStack, local proxy, OpenPortEngine, PortCatalog
  tunnel/      WireGuard / Mullvad / Nym / Xray / Hysteria helpers
  tools/       FTP, LAN scan, SSH (JSch), knock, port-check, mirror
  config/      SkidConfig model, parser/serializer, store + presets
  model/       OperatingModes, DnsPresets
  util/        SkidEventBus, PCAP, TLS SNI inspector, diagnostics
```

- **Language:** Java 8 source, no Kotlin, no lambdas in UI (anonymous classes)
- **VPN:** `android.net.VpnService` TUN; some modes skip the tun entirely
- **Sockets:** `SocketStack.applyTcp` / `applyUdp` / `openTls` on every listener and outbound
- **SSH:** `app/libs/jsch-0.1.55.jar` (jcraft). Do **not** swap in JSch 0.2.x — d8 NPEs on `Channel$1`
- **Verify:** `tools/verify/VerifySkidV51.java` — host-JVM checks (197 as of v6.0)

---

## Building

Needs Android SDK **build-tools 34.0.0**, **android-34** platform, and a JDK **12+** to sign (keystore uses HmacPBESHA256). Compile can be JDK 11; `apksigner` must be 12+.

```bash
export ANDROID_SDK_ROOT=/path/to/sdk          # build-tools/34.0.0 + platforms/android-34
export JAVA_HOME=/path/to/jdk-21              # Temurin 21 works
./build_apk.sh
# → /home/user/SkidwareDNS.apk  and  ./SkidwareDNS.apk
#    versionCode 600 / versionName 6.0.0-skid
#    signed v2 + v3 with skidwaredns-release.jks
```

Host verification (after compiling app classes against `android.jar`):

```bash
java -cp <verify>:<classes>:<android.jar> VerifySkidV51 ./configs
# PASS: VerifySkidV51 (197 checks)
```

The release keystore `skidwaredns-release.jks` is in the tree on purpose so updates keep the same signer. Password is in `build_apk.sh`. Rotate it if you fork publicly and do not want that key.

---

## Permissions

| Permission | Why |
| --- | --- |
| `INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE` | DNS, tunnels, LAN tools |
| `BIND_VPN_SERVICE` | Local TUN |
| `FOREGROUND_SERVICE` / `SPECIAL_USE`, `POST_NOTIFICATIONS` | Persistent VPN notification |
| `QUERY_ALL_PACKAGES` | Per-app firewall / split tunnel list |
| `RECEIVE_BOOT_COMPLETED`, `WAKE_LOCK`, `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Optional autostart |
| `VIBRATE` | Connect haptic |

---

## Disclaimer

SkidwareDNS is a **local** privacy / lab tool. Use it only on networks and hosts you own or have permission to test. Open Port, LAN scan, knock, and port-check will talk to whatever address you type. Tunnel settings (WireGuard keys, SSH passwords, `.skid` exports) are secrets — treat exported profiles like credentials.

This project is **not** affiliated with Mullvad, Nym, the Tor Project, I2P, RethinkDNS, InviZible, Evozi, or the GitHub apps the Tools hub is inspired by.

---

## Changelog

### v6.0 — Stack Lab (`600` / `6.0.0-skid`)

Configure → **Stack Lab**: TCP / UDP / socket / SOCKS5 / WireGuard / Tor / Orbot / I2P / VPN / TLS extras. Stats tab: live chart + six tiles + badges. First-run intro (6 pages). Fade / stagger / pulse motion.

### v5.9 — Live home (`590`)

Home allowed-vs-blocked chart, mini logs, live bytes B–GB, 15 operating modes, EventBus history ring, icon stroke 2.25px.

### v5.8 — Icons & chrome (`580`)

Lucide-style icons, tinted wells, card wash, sky selected tab.

### v5.7 — Tools hub (`570`)

FTP, LAN scan, SSH, knock, port-check, folder mirror in one APK.

### v5.6 — Open Port 1–64000 (`560`)

Bind catalog 1–64000 on device LAN IP (IANA + IpCisco + Wikipedia).

### v5.4 — Socket Stack (`540`)

Shared `SocketStack` handlers, HTTPS/TLS screen, Ports policy, congestion / keepalive / cork.

### v5.3 — Icon, switches, more Mullvad (`530`)

Hexagon-U launcher, vector icons everywhere, sky switches, 14 extra Mullvad relays (23 total).

### v5.2 — UI cleanup (`520`)

Home ACTIVE ROUTE + 2×3 tools; Configure becomes searchable collapsible groups.

### v5.1 — Mullvad suite (`510`)

Pinned relay directory, 100G Tallinn DAITA, Zurich multihop, SSH/V2Ray bridge.

### v5.0 — NymVPN (`500`)

Profiles, mixnet tuning estimator, QUIC / AmneziaWG / stealth API.

### v4.x — HTTP Injector

SSH + UDPGW, Xray, Hysteria, Shadowsocks 2022, payload forge, share URIs.

### v3.x — Privacy stack

LBox templates, DNSCrypt + Tor + Purple I2P, MegaProxy, Lumine Fake-IP, Hysteria 1/2.

---

## License

No SPDX license file is attached. Treat the tree as source-available unless the repository owner adds one. Third-party inspiration (Tools hub) was reimplemented; do not copy GPLv3 sources into this APK. JSch 0.1.55 is the jcraft BSD-style JAR under `app/libs/`.
