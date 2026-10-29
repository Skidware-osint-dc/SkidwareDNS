# SkidwareDNS v5.2 ("Clean Configure Edition") — `5.2.0-skid` (`versionCode 520`)

**SkidwareDNS v5.2** is an Android VPN, 5-hop Sphinx mixnet tuner, encrypted DNS router, multi-hop tunnel forge, anti-censorship packet shaper, and universal firewall (`com.skidwaredns.app`) powered by the human-readable **`.skid` Custom Config** format.

Version **5.2** is the **UI cleanup release**: the home screen drops from five stacked product cards to one **ACTIVE ROUTE** summary plus a flat **2x3 TOOLS** launcher, and Configure becomes a **searchable, collapsible, task-grouped directory** (27 destinations in six groups) instead of a 17-card wall.

Version **5.1** added the complete **Mullvad Relay** suite — a cryptographically verified relay directory (checked byte-for-byte against `https://api.mullvad.net/www/relays/all/`), **100 Gbps Tallinn `xtom` DAITA** relays, **Zurich `31173` / `PrivateLayer`** WireGuard relays, **AWS-style true WireGuard multihop** (`EntryIPv4:ExitMultihopPort`), **DAITA (Defense Against AI-guided Traffic Analysis)** Maybenot uniform-packet padding, Mullvad **SOCKS5 `:1080`** exit chaining, and the Zurich **`ch-zrh-br-001` SSH/V2Ray bridge** with pinned `SHA256`/`MD5` fingerprints — unified with **NymVPN**, **Evozi HTTP Injector**, **RethinkDNS**, **LBox**, **InviZible Pro** (`DNSCrypt + Tor + Purple I2P`), **AndroidMegaProxy**, and **SniShaper Lumine**.

---

## v5.0 (NymVPN Mixnet, Profiles & Anti-Censorship)

### 1. NymVPN Connection Profiles (`SUB_NYM_PROFILES`)
- **🧭 `Auto`**: Selects the best-performing entry & exit gateways while excluding your own country (`Nym-User-Country`).
- **🐢 `Anonymous`**: Switches into 5-hop Sphinx mixnet mode (`Mode=Mixnet-5Hop`) with closest gateways, uniform cover traffic, and exponential mixing delays.
- **🐇 `Fastest`**: Selects lowest-RTT 2-hop WireGuard / AmneziaWG gateways.
- **🎲 `Random`**: Cryptographically rotates distinct entry and exit gateways on every connection.

### 2. Live Mixnet Tuning & Performance Estimator (`SUB_NYM_MIXNET_TUNING`)
- **Real-Time `Expected performance of current settings` Card**: Dynamically computes `Packet rate` (`Up to 1.0 Mbps` at default) and `Latency` (`At least 690 ms` at `1 Mbps` + `Low` cover + `15 ms` mixing) as you adjust sliders or switches.
- **`Send continuous traffic`**: Toggle + 3-step `Faster` ↔ `Slower` slider (`2 Mbps`, `1 Mbps`, `0.7 Mbps`).
- **`Background cover traffic rate`**: Toggle + 4-step `Faster` ↔ `+ Anonymity` slider (`Low`, `Balanced 5x`, `Medium 10x`, `High 20x`).
- **`Packet mixing`**: Continuous `0 ms` (`Low`) → `15 ms` (`Default`) → `200 ms` (`High`) Poisson timing delay slider + **`Save custom settings`**.

### 3. NymVPN Anti-Censorship (`SUB_NYM_ANTI_CENSORSHIP`)
- **`Enhanced connection (QUIC)`**: Wraps WireGuard entry traffic in QUIC (`HTTP/3`) and strictly filters entry servers to QUIC-capable gateways.
- **`Minimal obfuscation (AmneziaWG)`**: Injects client-side high-entropy decoy UDP packets (`Jc=4`, `Jmin=40`, `Jmax=70`) prior to WireGuard handshakes/pulses to defeat stateful VPN blocking.
- **`Stealth API connect`**: Protects directory & gateway bootstrap using CDN cover domains (`cloudflare.com,cdn.jsdelivr.net,ajax.aspnetcdn.com`) in Always-On or Auto-Fallback mode, with a 1-tap live cover-domain probe.

---


## What's New in v5.2 (UI Cleanup & Information Architecture)

The tool count grew to 25 screens across five vendors; v5.2 reorganises them instead of adding more.

### 1. Home: one summary card + one tool launcher
- The five stacked product cards (InviZible tri-module, HTTP Injector, NymVPN, Mullvad) are replaced by a single **ACTIVE ROUTE** card (current Mullvad route summary + four context chips) and a flat **2x3 TOOLS** launcher (Relays, Mixnet, Injector, DNSCrypt, Tor, I2P).
- Module on/off switches now live only where they belong (each module's own screen), removing the duplicate toggles that previously existed in two places.

### 2. Configure: searchable, collapsible, task-grouped
- **Search field** filters all 27 setting destinations live as you type (`dns`, `ssh`, `buffer`, `tor`, `mtu`, `mullvad`, ...).
- Six collapsible groups replace the 17-item card wall: **Connect & Route**, **Tunnel Forge (HTTP Injector)**, **Privacy Stack**, **DNS & Firewall**, **Performance & Sockets**, **App & Diagnostics**. Searching auto-expands every matching group and hides empty ones.
- Four **task tiles** at the top answer the common questions directly: current Mullvad route, Nym profile/RTT, selected tunnel badge, and privacy-stack status.
- Consistent header (`Configure` + version/`MAIN` chips) and a version footer replacing the old marketing banner.

### 3. New shared UI components (`UiFactory`)
`SettingEntry` + `filterEntries`, `searchField`, `collapsibleSection`, `taskTile` / `taskTileRow`, `compactRow`, `groupCaption`, `footnote`, `counterChip`, `versionFooter` — one density-reduced row style, one section style, one tile style across the app.

## v5.1 (Mullvad Relays, 100 Gbps DAITA, Multihop & Bridges)

### 1. Mullvad Relay Directory (`SUB_MULLVAD_RELAYS = 24`)
- Expandable **Mullvad Relay Cards** reproducing the official relay pages field-for-field: `DOMAIN NAME`, `IPV4`, `IPV6`, `DAITA`, `PUBLIC KEY`, `SOCKS5 PROXY ADDRESS`, `MULTIHOP PORT` (and for bridges `SSH FINGERPRINT (SHA256)` / `(MD5)` + `V2RAY BRIDGE IPV4`). Every value is tap-to-copy.
- Pure-Java **live sync** against `https://api.mullvad.net/www/relays/all/` (`↻ Sync Live API`) parses real relay JSON, merges new relays, and refreshes keys/IPs/multihop ports without any third-party library.
- Filter chips: **ALL**, **DAITA**, **OWNED**, **100G**, **BRIDGE**; plus **Probe** per relay (live WireGuard-handshake / SSH-banner reachability).

### 2. Tallinn, Estonia — 100 Gbps `xtom` DAITA Relays (`Owned by Mullvad`)
`ee-tll-wg-101` / `.102` / `.103` — `213.232.112.3` / `.166` / `.209`, full IPv6 (`2a07:d883:704:1..3::f001`), Curve25519 public keys, SOCKS5 `ee-tll-wg-socks5-101..103.relays.mullvad.net:1080`, multihop ports `3503` / `3514` / `3515`, `DAITA: Yes`.

### 3. Zurich, Switzerland WireGuard Relays
- Owned (`31173 / 20 Gbps`): `ch-zrh-wg-001` `193.32.127.71` (`3241`), `ch-zrh-wg-002` `193.32.127.70` (`3125`), `ch-zrh-wg-003` `193.32.127.72` (`3377`).
- Rented (`PrivateLayer / 10 Gbps`): `ch-zrh-wg-201` `179.43.189.66` (`3105`), `ch-zrh-wg-202` `46.19.136.226` (`3106`).

### 4. Zurich Bridge Relay `ch-zrh-br-001` (SSH + V2Ray)
`ch-zrh-br-001.relays.mullvad.net` `193.32.127.117` (V2Ray `193.32.127.118`), `31173 / 10 Gbps`, `Owned by Mullvad`, SSH `SHA256:IE/308Y8M8dOs+e2IP7sl1CtY6TSwt6uD5ygHTU/32w` and `MD5:7d:ec:f4:5e:f1:d2:5a:33:86:80:6e:a4:2b:7e:18:58` pinned into `Jump-Host`/`Mullvad-SSH-SHA256`, with one-tap **V2Ray Bridge** configuration.

### 5. True WireGuard Multihop + DAITA & SOCKS5
- **Multihop**: dials `Entry IPv4` on the `Exit`'s real multihop port and keeps the exit's Curve25519 public key (e.g. `ch-zrh-wg-001 → ee-tll-wg-102` = `193.32.127.71:3514`).
- **DAITA**: Maybenot uniform 1280-byte padding bursts injected into the tunnel pulse loop in `TunnelEngine.sendWireGuardPulseIfEnabled`, plus anti-fingerprinting burst sizing.
- **SOCKS5 chaining**: optional `*‑wg‑socks5‑*.relays.mullvad.net:1080` exit proxying, and native Mullvad WireGuard `.conf` export/copy.
- Mullvad `dns.mullvad.net` DoH added to bootstrap pinning; all 9 relay hostnames resolve without DNS dependencies.

---

## What's Included from v4.0–v4.1 (Evozi HTTP Injector Integration)

### 1. 2-Column Tunnel Type × Connect-From Matrix & Selected Tunnel Banner
- **Live `Selected Tunnel Type` Banner**: Displays the active chain headline + protocol pill (`HTTP Proxy → SSH` `[TCP (HTTP)]`, `Hysteria (v2)` `[UDP]`, `Shadowsocks (Direct)` `[TCP (SOCKS)]`, `V2Ray/Xray (VLESS)` `[TCP (HTTP2/WS) | UDP (QUIC/mKCP)]`), top-right `↺ Reset`, and full-width `SAVE`.
- **Enforced Compatibility Matrix**:
  - **Secure Shell (SSH)**: Enables `None (Direct)`, `DNS (DNSTT)`, `HTTP Proxy`, `TLS/SSL Proxy`, `TLS/SSL (stunnel)`, and `Custom Payload` (`CONNECT [host_port] HTTP/1.1[crlf]...`).
  - **Hysteria & V2Ray/Xray**: Automatically locks `Connect From` to `None (Direct)` and disables `Custom Payload`.
  - **Shadowsocks**: Enables `None (Direct)`, `HTTP (Obfs)`, and `TLS/SSL (Obfs)`.

### 2. Full SSH + UDPGW + Buffer Sizing + Header Replacer
- **Account & Key Auth**: `SSH Host`, `SSH Port`, `Username` (`/root-skidware`), `Password / Passphrase`, and `Private Key (Optional)` + OpenSSH/PEM key modal.
- **Connection & Advanced Modal**: `Local Port` (`1080`), **`UDPGW Port`** (`7300` `badvpn-udpgw`), configurable **`Buffer Size`** (`Send: 16384 | Receive: 32768` applied via `Socket.setSendBufferSize` / `setReceiveBufferSize`), **`Header Replacer`** (normalizes non-200 HTTP proxy responses to `HTTP/1.1 200 OK`), and **`Route Proxy`** (local IP routing).

### 3. Complete V2Ray / Xray Configurator & Share URI Engine
- **Core & 6 Protocols**: `Xray` / `V2Ray` cores across `VMess`, `VLESS`, `Trojan`, `Shadowsocks`, `Socks`, and `Wireguard`.
- **8 Network Transports & Header Obfuscation**: `TCP`, `mKCP`, `WebSocket`, `HTTP Upgrade`, `HTTP/2`, `XHTTP`, `QUIC`, and `gRPC` + `Header Type` (`None` / `HTTP`).
- **Transport Layer Encryption, Mux & Regional Routing**: `Security` (`None`, `TLS`, `Reality` with `PublicKey` & `ShortId`), `Mux` toggle, `Domain Strategy` (`AsIs (Default)`, `IPIfNonMatch`, `IPOnDemand`), and `Routing Rules` (`Proxy All`, `Direct China (CN)`, `Direct Iran (IR)`, `Direct Russia (RU)`, `Direct Saudi Arabia (SA)`, `Block Ads (Basic)`).
- **1-Tap Share URI Import/Export**: Native `vless://`, `vmess://`, `trojan://`, `ss://`, and `hy2://` clipboard/URI parser & generator plus live Xray JSON config preview.

### 4. Hysteria v1 / v2 Configurator, QUIC Window Slider & CIDR Route Subtraction
- **Hysteria v1 & v2**: `Version` (`1` / `2`), `Protocol` (`UDP`, `FakeTCP`, `WeChat-Video`), `Authentication Password`, `Obfuscation Password` (Salamander), `SNI`, `ALPN` (`h3`), `Allow Insecure`, `Port Hopping Interval (s)`, `Max Up/Down Mbps`, **`QUIC Receive Window Size`** slider (`2`–`64` MB, default `8` MB), and **`Disable Path MTU Discovery (RFC 8899)`**.
- **Global Connection Routes & HTTP Ping**: `VPN DNS`, `DNS Forwarding`, `Use default route`, bitwise IPv4 CIDR subtraction (`Custom Routes: 0.0.0.0/0` minus `Excluded Routes: 10.0.0.0/8;172.16.0.0/12;192.168.0.0/16` -> 31 exact `VpnService.Builder.addRoute` subnets), periodic + 1-tap **`HTTP Ping`** (`clients3.google.com/generate_204`), `CPU Wakelock`, `Show notification icon`, and `Vibrate` on status change.

---

## What's Included from v3.0 & Earlier

### 1. LBox DNS Templates & Core TLS Record Fragmentation
- **Toggleable LBox DNS Templates (`LBox Templates [...]`)**: `google_dot` (`8.8.8.8`), `google_doh`, `cloudflare_udp` (`1.1.1.1`), `cloudflare_dot`, `safe_dns_dot` (`9.9.9.9`), `opendns_udp` (`208.67.222.222` direct), `opendns_doh`, `quad9_doh` (via VPN, anti-malware), and `yandex_dot` (`77.88.8.8` direct).
- **Core Routing & TUN Stack**: `Strict-Route` (`0.0.0.0/1` + `128.0.0.0/1`, `::/1` + `8000::/1`) and `TUN-Stack` (`system`, `gvisor`, `mixed`).
- **Binary TLS `ClientHello` Shaper (`TlsSniInspector.java`)**:
  - **Mixed-case SNI** (`Mixed-Case-SNI=true`, e.g. `WwW.GoOgLe.cOm`, automatically skipped on REALITY transports).
  - **TLS Record Fragmentation** (`TLS-Record-Fragment=true`, splits a single `0x16` TLS handshake into two valid `0x16` records right across the SNI hostname).
  - **TCP Segment Fragmentation** (`TLS-Fragment=true`) + configurable **Fallback Delay** (`TLS-Fallback-Delay-Ms=500`).

### 2. InviZible Pro Tri-Module (`DNSCrypt` + `Tor` + `Purple I2P`)
- **Home Tri-Module Quick Card (`MAIN | DNSCRYPT | TOR | PURPLE I2P | LBOX | MEGAPROXY`)**: 1-tap switches for *Defend with DNSCrypt*, *Hide IP with TOR*, and *Access I2P with Purple I2P*.
- **Full DNSCrypt & Live v3 Mirror Browser (`DnscryptListManager.java`)**:
  - Live downloader & binary `sdns://` (`0x01`, `0x02`, `0x05`, `0x81`, `0x85`) stamp decoder across all 3 official v3 mirrors (`raw.githubusercontent.com/DNSCrypt/dnscrypt-resolvers/master/v3/...`, `download.dnscrypt.info/resolvers-list/v3/...`, `ipv6.download.dnscrypt.info/resolvers-list/v3/...`).
  - `Require DNSSEC / No-Log / No-Filter`, `Force TCP`, `HTTP3 / QUIC`, `Outbound SOCKS Proxy`, `DNS64`, `Block Unqualified / Undelegated / IPv6`, **Whitelist / IP Blacklist**, **Forwarding Rules**, **Cloaking Rules**, `Ignored Qtypes`, `Suspicious (NXDOMAIN) Logging`, and direct **`dnscrypt-proxy.toml`** editor.
- **Expanded Tor Onion Settings**:
  - `ExitNodes` / `ExcludeExitNodes` / `EntryNodes`, `StrictNodes`, `VirtualAddrNetwork` (`10.192.0.0/10`), Stream Isolation (`Isolate UID`, `Dest Addr`, `Dest Port`), **Snowflake** (`AMP Cache`, `Domain Fronting`, `SQS`) + STUN servers, `Connection Padding` / `Reduced Padding`, `Fascist Firewall`, `Hardware Accel`, `Avoid Disk Writes`, and direct **`tor.conf`** editor.
- **Full Purple I2P (`i2pd`) Settings**:
  - `SSU2` & `NTCP2` transports, `Bandwidth` & `Transit Share (%)`, `HTTP Proxy (:4444)` + outproxy (`http://false.i2p`), `SOCKS Proxy (:4447)`, `SAM (:7656)`, `UPnP`, `Verify su3`, **`.b32.i2p/hosts.txt` AddressBook** (`http://shx5vqsw7usdaunyzr2qmes2fq37oumybpudrd4jjj4e4vk4uusa.b32.i2p/hosts.txt`), and direct **`i2pd.conf` / `tunnels.conf`** editors.
- **Fast Settings, MITM ARP Audit & CLI Automation (`SkidAutomationReceiver.java`)**:
  - Boot `Autostart` (`DNSCrypt`, `Tor`, `I2P`, `Autostart Delay`), **Spoof SNI** (`play.googleapis.com`, `drive.google.com`, `cdn.ampproject.org`, `api.github.com`, `ajax.aspnetcdn.com`, `verizon.com`, `eset.com`), **`/proc/net/arp` ARP Spoofing MITM Detector**, `Prevent Device Sleep` (`PARTIAL_WAKE_LOCK`), and ADB/Termux/Tasker broadcast control:
    ```bash
    am broadcast -a com.skidwaredns.app.SHELL_SCRIPT_CONTROL --ei dnscrypt 1 --ei tor 1 --ei i2p 1 --ei vpn 1
    # Also compatible with pan.alexander.tordnscrypt.SHELL_SCRIPT_CONTROL
    ```

### 3. AndroidMegaProxy, Lumine Fake-IP & Evozi Hysteria 1/2 + SlowUDP
- **AndroidMegaProxy**: Multi-hop **HTTPS & SSH Jump Hosts**, **MASQUE (`MASQUE-H3`)**, `Proxy-Knock-Host` auth check, Android 10+ `ConnectivityManager.getConnectionOwnerUid` **App Ownership Checks** (`Disabled` / `Standard` / `Advanced`), **JA3 Fingerprint** presets, and live **Cloudflare `cdn-cgi/trace` Exit-IP + 3-Sample Median TTFB** diagnostics.
- **Lumine (`lumine-for-android`)**: Zero-RTT **`198.18.0.0/15` Bidirectional Fake-IP Pool**, `GFWList-Split` routing preset, remote `.skid` / Clash YAML Subscription importer, and OEM battery keep-alive helper.
- **Evozi Hysteria (`evozi/hysteria-install`)**: Dedicated **`Hysteria`**, **`Hysteria2`** (Salamander obfuscation + up/down Mbps + port hopping), and **`SlowUDP`** tunnel generators.

---

## Built-In `.skid` Profiles (`configs/`)

1. `rdns_fortress.skid` — RethinkDNS Security + DNS Booster + DPI Auto
2. `skidwareproxy_dpi_desync.skid` — WireGuard `skidwareproxy` + Flood WG + Hybrid DPI Desync
3. `invizible_tor_dnscrypt.skid` — Classic Tor + DNSCrypt Onion Shield
4. `hysteria_tls_obfs.skid` — Hysteria + ControlD DoT + TLS/SSL (Obfs)
5. `xray_http_payload.skid` — V2Ray/Xray WebSocket + Custom HTTP Payload Forge
6. `dnstt_ssh_stealth.skid` — SSH over DNS (`DNSTT` SlowDNS)
7. `nym_strongswan_mixnet.skid` — Nym 5-Hop Mixnet + strongSwan IKEv2
8. `lbox_tls_record_frag.skid` — **v3.0** LBox DNS Templates + gVisor Stack + TLS Record Fragment + Mixed-Case SNI
9. `invizible_full_trimodule.skid` — **v3.0** InviZible Pro Complete Tri-Module (`DNSCrypt + AnonRelays + Tor + Purple I2P + ARP MITM`)
10. `megaproxy_masque_jumphost.skid` — **v3.0** AndroidMegaProxy Multi-Hop HTTPS Jump + MASQUE-H3 + Chrome-128 JA3 + UID Check
11. `lumine_hysteria2_fakeip.skid` — **v3.0** Lumine `198.18.0.0/15` Fake-IP + GFWList Split + Evozi Hysteria2 Brutal Salamander
12. `injector_ssh_http_payload.skid` — **v4.0** HTTP Injector `HTTP Proxy → SSH` (`/root-skidware`), Custom Payload, UDPGW `:7300`, `16KB/32KB` Buffers, `Header Replacer` (`200 OK`) & Excluded LAN Routes
13. `injector_xray_vless_reality_xhttp.skid` — **v4.0** HTTP Injector Xray `VLESS` + `XHTTP` + `Reality` + `IPIfNonMatch` + `Block Ads (Basic)`
14. `injector_hysteria2_quicwin_hop.skid` — **v4.0** HTTP Injector Hysteria v2 + `16 MB` QUIC Receive Window + `30s` Port Hopping + RFC 8899 PMTU Control
15. `injector_ss2022_blake3_doh_quad9.skid` — **v4.1** HTTP Injector Shadowsocks `2022-blake3-aes-256-gcm` + `Server (Obfs)` TLS + `Quad9` DoH (14-Provider Selector) + Excluded LAN Routes
16. `nymvpn_anonymous_mixnet_690ms.skid` — **v5.0** NymVPN `Anonymous` 5-Hop Mixnet (`1 Mbps` Continuous + `Low` Cover + `15ms` Packet Mixing = `At least 690 ms`) + Client-Side `AmneziaWG` Decoys
17. `nymvpn_anticensorship_quic_stealth.skid` — **v5.0** NymVPN `Auto` (Never Own Country) + `Enhanced connection (QUIC)` + `Minimal obfuscation (AmneziaWG)` + Always-On `Stealth API connect` Cover Domains
18. `mullvad_tallinn_100g_daita.skid` — **v5.1** Mullvad `ee-tll-wg-101` Tallinn `xtom / 100 Gbps` (`Owned by Mullvad`, `DAITA: Yes`) + SOCKS5 `:1080` + Mullvad DoH
19. `mullvad_zurich_to_tallinn_multihop.skid` — **v5.1** True WireGuard multihop `ch-zrh-wg-001` (`193.32.127.71`) → `ee-tll-wg-102` on multihop port `3514` (100 Gbps DAITA exit)
20. `mullvad_zurich_bridge_ssh_v2ray.skid` — **v5.1** `ch-zrh-br-001` SSH bridge (`193.32.127.117`, pinned `SHA256` fingerprint) / V2Ray (`193.32.127.118`) → `ch-zrh-wg-003`

---

## Rebuilding the Signed APK

```bash
./build_apk.sh
# Outputs v5.2 (520 / 5.2.0-skid) v2+v3 signed APK -> /home/user/SkidwareDNS.apk

# Host-JVM verification + .skid preset export (81 checks)
java -cp <classes> VerifySkidV51 /home/user/SkidwareDNS/configs
```
