#!/usr/bin/env python3
"""Generate PortCatalog.java for ports 1-64000 from IANA + IpCisco + Wikipedia extras."""
import csv
from pathlib import Path

MAX = 64000
MIN = 1
IANA = Path("/tmp/iana-ports.csv")
OUT = Path("/home/user/SkidwareDNS/app/src/main/java/com/skidwaredns/app/proxy/PortCatalog.java")

def ascii(s):
    if s is None:
        return ""
    s = s.replace("\u2013", "-").replace("\u2014", "-").replace("\u2019", "'")
    s = s.replace("\u2018", "'").replace("\u00a0", " ")
    return "".join(ch if ord(ch) < 128 else " " for ch in s).strip()

def java_str(s):
    t = ascii(s).replace("\\", "\\\\").replace('"', '\\"').replace("\n", "\\n").replace("\r", "").replace("\t", "\\t")
    return '"' + t + '"'

# port -> (proto T/U/B, name, about)
P = {}

def add(ports, proto, name, about, overwrite=True):
    if isinstance(ports, int):
        ports = [ports]
    for p in ports:
        if not (MIN <= p <= MAX):
            continue
        if (not overwrite) and p in P:
            continue
        P[p] = (proto, ascii(name)[:48] or "service", ascii(about)[:280] or name)

# ---- IANA official assignments ------------------------------------------
if IANA.exists():
    by = {}
    with IANA.open(newline="", encoding="utf-8", errors="replace") as f:
        r = csv.reader(f)
        next(r, None)
        for row in r:
            if len(row) < 4:
                continue
            sname, pstr, proto, desc = row[0].strip(), row[1].strip(), row[2].strip().lower(), row[3].strip()
            if not pstr.isdigit():
                continue
            p = int(pstr)
            if p < MIN or p > MAX:
                continue
            rec = by.setdefault(p, {"tcp": False, "udp": False, "name": "", "desc": ""})
            if proto == "tcp":
                rec["tcp"] = True
            elif proto == "udp":
                rec["udp"] = True
            if sname and not rec["name"]:
                rec["name"] = sname
            if desc and len(desc) > len(rec["desc"]):
                rec["desc"] = desc
    for p, rec in by.items():
        if rec["tcp"] and rec["udp"]:
            proto = "B"
        elif rec["udp"]:
            proto = "U"
        else:
            proto = "T"
        name = rec["name"] or rec["desc"] or "IANA"
        name = name.replace("_", "-")[:48]
        desc = rec["desc"] or rec["name"] or "IANA-assigned service."
        if desc.lower() in ("reserved", "unassigned", ""):
            continue
        add(p, proto, name, desc + " (IANA).", overwrite=False)

# ---- IpCisco well-known teaching list (https://ipcisco.com/lesson/network-ports/)
IPC = {
    1: ("B", "tcpmux", "TCP port multiplexer (IpCisco / IANA well-known)."),
    5: ("B", "rje", "Remote job entry."),
    7: ("B", "echo", "Echo service. Whatever you send is bounced back."),
    9: ("B", "discard", "Zero service for test purposes. Accepts data and throws it away."),
    11: ("B", "systat", "System information / active users."),
    13: ("B", "daytime", "Time and date as human-readable text."),
    17: ("B", "qotd", "Sends quote of the day. Keep closed (amplification)."),
    18: ("B", "msp", "Message Send Protocol. Sends text messages."),
    19: ("B", "chargen", "Sends an infinite character string. Famous UDP amplifier - keep closed."),
    20: ("T", "ftp-data", "FTP data transfer."),
    21: ("B", "ftp", "FTP connection / control."),
    22: ("B", "ssh", "Secure Shell Service. Encrypted remote login, SCP/SFTP, tunnels."),
    23: ("T", "telnet", "Telnet service. Unencrypted remote terminal."),
    25: ("T", "smtp", "Simple Mail Transfer Protocol. Mail-server to mail-server."),
    37: ("B", "time", "Machine-readable time protocol."),
    39: ("B", "rlp", "Resource location protocol."),
    42: ("B", "nameserver", "Name service / WINS."),
    43: ("T", "nicname", "WHOIS directory service."),
    49: ("B", "tacacs", "Terminal access controller access control system."),
    50: ("B", "re-mail-ck", "Remote mail checking."),
    53: ("B", "domain", "DNS name resolver. UDP for small queries, TCP for large / AXFR."),
    67: ("U", "bootps", "Bootstrap protocol services / DHCP server."),
    68: ("U", "bootpc", "Bootstrap client / DHCP client."),
    69: ("U", "tftp", "Trivial file transfer protocol. No login."),
    70: ("T", "gopher", "Document search (Gopher)."),
    79: ("T", "finger", "Delivers user contact information. Privacy hazard."),
    80: ("T", "http", "Hypertext transfer protocol. Unencrypted web."),
    81: ("T", "torpark", "TorPark onion routing (unofficial, IpCisco)."),
    82: ("U", "torpark-ctl", "TorPark control (unofficial, IpCisco)."),
    88: ("B", "kerberos", "Network authentication system (Active Directory)."),
    101: ("T", "hostname", "NIC host name."),
    102: ("T", "iso-tsap", "ISO TSAP protocol."),
    105: ("B", "csnet-ns", "Mailbox mail server / CCSO nameserver."),
    107: ("T", "rtelnet", "Remote telnet."),
    109: ("T", "pop2", "Post office protocol v2 for e-mail."),
    110: ("T", "pop3", "Post office protocol v3 for e-mail."),
    111: ("B", "sunrpc", "RPC protocol for NFS (portmapper)."),
    113: ("U", "auth", "Authentication / ident service."),
    115: ("T", "sftp", "Simple file transfer protocol (not SSH-SFTP)."),
    117: ("T", "uucp-path", "File transfer between Unix systems."),
    119: ("T", "nntp", "Transfer of messages in news groups."),
    123: ("U", "ntp", "Time synchronization service."),
    137: ("B", "netbios-ns", "NETBIOS name service."),
    138: ("B", "netbios-dgm", "NETBIOS datagram service."),
    139: ("B", "netbios-ssn", "NETBIOS session service."),
    143: ("B", "imap", "Internet message access protocol for e-mail."),
    161: ("U", "snmp", "Simple network management protocol."),
    162: ("B", "snmptrap", "SNMP trap messages."),
    177: ("B", "xdmcp", "X display manager."),
    179: ("T", "bgp", "Border gateway protocol."),
    194: ("B", "irc", "Internet relay chat."),
    199: ("B", "smux", "SNMP UNIX multiplexer."),
    201: ("B", "at-rtmp", "AppleTalk routing."),
    209: ("B", "qmtp", "Quick mail transfer protocol."),
    210: ("B", "z39.50", "Bibliographic information system."),
    213: ("B", "ipx", "Internetwork packet exchange."),
    220: ("B", "imap3", "IMAP v3 for e-mail."),
    369: ("B", "rpc2portmap", "Coda file system port mapper."),
    370: ("B", "codaauth2", "Coda file system authentication service."),
    389: ("B", "ldap", "Lightweight directory access protocol."),
    427: ("B", "svrloc", "Service location protocol."),
    443: ("T", "https", "HTTPS (HTTP over SSL/TLS). Encrypted web."),
    444: ("B", "snpp", "Simple network paging protocol."),
    445: ("T", "microsoft-ds", "SMB over TCP/IP. Windows file sharing."),
    464: ("B", "kpasswd", "Kerberos password change."),
    500: ("U", "isakmp", "IKE / IPsec security protocol."),
    512: ("T", "exec", "Remote process execution (rexec)."),
    513: ("T", "login", "Login to remote computer (rlogin)."),
    514: ("T", "shell", "Remote shell (rsh). UDP 514 is syslog."),
    515: ("T", "printer", "Line printer daemon print services."),
    517: ("U", "talk", "Talk remote calling."),
    518: ("U", "ntalk", "Network talk."),
    520: ("U", "router", "Routing information protocol (RIP)."),
    521: ("U", "ripng", "Routing information protocol for IPv6."),
    525: ("U", "timed", "Time server."),
    530: ("B", "courier", "Courier remote procedure call."),
    531: ("B", "conference", "Chat over AIM and IRC."),
    532: ("T", "netnews", "Netnews newsgroup service."),
    533: ("U", "netwall", "Emergency broadcasts."),
    540: ("T", "uucp", "Unix-to-Unix copy protocol."),
    543: ("T", "klogin", "Kerberos v5 remote login."),
    544: ("T", "kshell", "Kerberos v5 remote shell."),
    546: ("B", "dhcpv6-client", "DHCP v6 client."),
    547: ("B", "dhcpv6-server", "DHCP v6 server."),
    548: ("T", "afpovertcp", "Apple filing protocol over TCP."),
    554: ("B", "rtsp", "Real time streaming protocol."),
    556: ("T", "remotefs", "Remote file system."),
    563: ("B", "nntps", "NNTP over SSL/TLS."),
    587: ("T", "submission", "Message submission agent (outgoing mail)."),
    631: ("B", "ipp", "Internet printing protocol / CUPS."),
    636: ("B", "ldaps", "LDAP over SSL/TLS."),
    674: ("T", "acap", "Application configuration access protocol."),
    694: ("B", "ha-cluster", "Heartbeat service."),
    749: ("B", "kerberos-adm", "Kerberos v5 administration."),
    750: ("U", "kerberos-iv", "Kerberos v4 services."),
    873: ("T", "rsync", "rsync file transfer services."),
    992: ("B", "telnets", "Telnet over SSL/TLS."),
    993: ("T", "imaps", "IMAP over SSL/TLS."),
    995: ("T", "pop3s", "POP3 over SSL/TLS."),
    # IpCisco registered table
    1080: ("T", "socks", "SOCKS proxy."),
    1433: ("T", "ms-sql-s", "Microsoft SQL server."),
    1434: ("B", "ms-sql-m", "Microsoft SQL monitor."),
    1494: ("T", "ica", "Citrix ICA client."),
    1512: ("B", "wins", "Windows internet name service."),
    1524: ("B", "ingreslock", "Ingres DBMS."),
    1701: ("U", "l2tp", "Layer 2 tunneling protocol / L2F."),
    1719: ("U", "h323gatestat", "H.323 gatekeeper status."),
    1720: ("T", "h323hostcall", "H.323 host call."),
    1812: ("B", "radius", "RADIUS authentication."),
    1813: ("B", "radius-acct", "RADIUS accounting."),
    1985: ("U", "hsrp", "Cisco HSRP."),
    2008: ("T", "teamspeak-acct", "TeamSpeak 3 accounting (unofficial, IpCisco)."),
    2010: ("U", "teamspeak-list", "TeamSpeak 3 web list (unofficial, IpCisco)."),
    2049: ("B", "nfs", "Network file system."),
    2102: ("B", "zephyr-srv", "Zephyr server."),
    2103: ("B", "zephyr-clt", "Zephyr client."),
    2104: ("B", "zephyr-hm", "Zephyr host manager."),
    2401: ("T", "cvspserver", "Concurrent versions system."),
    2809: ("B", "corbaloc", "CORBA locator."),
    3306: ("B", "mysql", "MySQL / MariaDB database service."),
    4321: ("T", "rwhois", "Remote whois service."),
    5999: ("T", "cvsup", "CVSup."),
    6000: ("T", "X11", "X Windows system services."),
    11371: ("T", "pgpkeyserver", "Public keyserver for PGP (IpCisco registered)."),
    13720: ("B", "bprd", "Symantec/Veritas NetBackup."),
    13721: ("B", "bpdbm", "Symantec/Veritas database manager."),
    13724: ("B", "vnetd", "Symantec/Veritas network utility."),
    13782: ("B", "bpcd", "Symantec/Veritas NetBackup."),
    13783: ("B", "vopied", "Symantec/Veritas VOPIE."),
    22273: ("B", "wnn6", "Kana/Kanji conversion."),
    23399: ("B", "skype", "Skype (unofficial, IpCisco)."),
    25565: ("T", "minecraft", "Minecraft Java Edition server (unofficial, IpCisco / Wikipedia)."),
    26000: ("B", "quake", "Quake and other multiplayer games (IpCisco)."),
    27017: ("T", "mongodb", "MongoDB (unofficial, IpCisco / Wikipedia)."),
    33434: ("B", "traceroute", "Network tracking / traceroute (IpCisco)."),
}
for p, (proto, name, about) in IPC.items():
    add(p, proto, name, about, overwrite=True)

# ---- Wikipedia unofficial / widely used (List of TCP and UDP port numbers)
WIKI = {
    1194: ("B", "OpenVPN", "OpenVPN. UDP (typical) or TCP tunnel. Wikipedia well-known VPN port."),
    1337: ("T", "WASTE", "Unofficial 'leet' port. WASTE P2P historically."),
    1723: ("B", "PPTP", "Point-to-Point Tunneling Protocol VPN. Broken crypto - do not use."),
    1883: ("T", "MQTT", "MQTT IoT pub/sub. Use 8883 for TLS."),
    1900: ("U", "SSDP", "UPnP / SSDP discovery. TVs, printers, routers."),
    1935: ("T", "RTMP", "Real-Time Messaging Protocol. Live streaming ingest."),
    3074: ("B", "Xbox Live", "Xbox Live multiplayer (unofficial, Wikipedia)."),
    3128: ("T", "Squid", "Default Squid HTTP proxy."),
    3389: ("B", "RDP", "Remote Desktop Protocol. Windows GUI remote control."),
    3478: ("B", "STUN", "STUN/TURN. WebRTC/VoIP NAT traversal (Wikipedia)."),
    3724: ("T", "WoW", "World of Warcraft and some Blizzard games."),
    4500: ("U", "IPsec-NAT-T", "IPsec NAT traversal. IKEv2 VPNs after UDP 500."),
    5004: ("U", "RTP", "Real-time audio/video payload."),
    5005: ("U", "RTCP", "RTP control companion."),
    5060: ("B", "SIP", "Session Initiation Protocol. VoIP call setup."),
    5061: ("T", "SIPS", "SIP over TLS."),
    5222: ("T", "XMPP", "XMPP / Jabber client-to-server."),
    5223: ("T", "XMPP-SSL", "XMPP over SSL; also older Apple Push."),
    5269: ("T", "XMPP-s2s", "XMPP server-to-server federation."),
    5351: ("U", "NAT-PMP", "NAT Port Mapping Protocol / PCP (Wikipedia)."),
    5353: ("U", "mDNS", "Multicast DNS (Bonjour/Avahi)."),
    5355: ("B", "LLMNR", "Link-Local Multicast Name Resolution (Windows)."),
    5357: ("T", "WSDAPI", "Web Services for Devices (Windows, Wikipedia unofficial)."),
    5432: ("T", "PostgreSQL", "PostgreSQL database."),
    5500: ("T", "VNC-listen", "VNC incoming listening viewer / Hotline (Wikipedia unofficial)."),
    5554: ("T", "fastboot", "Fastboot default wireless port (Wikipedia unofficial)."),
    5555: ("T", "ADB", "Android Debug Bridge over TCP / Oracle Inbound Refinery (Wikipedia unofficial)."),
    5631: ("T", "pcAnywhere", "Symantec pcAnywhere data."),
    5672: ("T", "AMQP", "Advanced Message Queuing Protocol (RabbitMQ)."),
    5900: ("T", "VNC", "VNC Remote Framebuffer. Display :0."),
    5938: ("B", "TeamViewer", "TeamViewer remote-support."),
    6379: ("T", "Redis", "Redis in-memory database/cache."),
    6443: ("T", "Kubernetes", "Kubernetes API server HTTPS."),
    6667: ("T", "IRC", "Default Internet Relay Chat."),
    6697: ("T", "IRC-TLS", "IRC over TLS."),
    6881: ("B", "BitTorrent", "BitTorrent peer. 6881-6889 classic range."),
    8000: ("T", "HTTP-alt", "Common alternate HTTP."),
    8080: ("T", "HTTP-alt", "The default second HTTP. Tomcat, proxies, cameras."),
    8443: ("T", "HTTPS-alt", "Alternate HTTPS. Tomcat SSL, appliances."),
    8883: ("T", "MQTT-TLS", "MQTT over TLS."),
    9000: ("T", "app-9000", "SonarQube / PHP-FPM / many app servers."),
    9090: ("T", "Prometheus", "Prometheus metrics / dashboards."),
    9100: ("T", "JetDirect", "HP JetDirect raw TCP printing."),
    9200: ("T", "Elasticsearch", "Elasticsearch HTTP API."),
    9418: ("T", "git", "Git daemon (git://)."),
    9987: ("U", "TeamSpeak3", "TeamSpeak 3 voice."),
    10000: ("T", "Webmin", "Webmin admin UI and NDMP backups."),
    11211: ("B", "memcached", "memcached. Memory cache. Never public."),
    15672: ("T", "RabbitMQ-UI", "RabbitMQ management UI (unofficial)."),
    19132: ("U", "MC-Bedrock", "Minecraft Bedrock Edition UDP (Wikipedia unofficial)."),
    19133: ("U", "MC-Bedrock-v6", "Minecraft Bedrock IPv6 (Wikipedia unofficial)."),
    24800: ("T", "Synergy", "Synergy keyboard/mouse share (Wikipedia unofficial)."),
    25575: ("T", "MC-RCON", "Minecraft RCON (Wikipedia unofficial)."),
    27015: ("B", "Steam/SRCDS", "Steam / Source dedicated server (Wikipedia unofficial)."),
    28015: ("U", "Rust", "Rust game server (Wikipedia unofficial)."),
    32400: ("T", "Plex", "Plex Media Server (Wikipedia unofficial)."),
    51820: ("U", "WireGuard", "WireGuard VPN. Default listen port on many phones/routers (IANA + Wikipedia)."),
    51821: ("U", "WireGuard-alt", "Common second WireGuard interface."),
}
for p, (proto, name, about) in WIKI.items():
    add(p, proto, name, about, overwrite=True)

# SkidwareDNS local defaults
add(10808, "T", "skid-socks", "SkidwareDNS local SOCKS5 listener (default).")
add(10809, "T", "skid-http", "SkidwareDNS local HTTP CONNECT listener (default).")
add(5354, "T", "skid-dnscrypt", "SkidwareDNS DNSCrypt local listen (default 5354).")
add(5400, "T", "skid-tor-dns", "SkidwareDNS Tor DNS default.")
add(9050, "T", "Tor-SOCKS", "Tor SOCKS proxy. SkidwareDNS default 9050.")
add(9051, "T", "Tor-control", "Tor control port. Never public.")
add(9040, "T", "Tor-trans", "Tor transparent proxy (SkidwareDNS default 9040).")
add(4444, "T", "I2P-HTTP", "I2P HTTP proxy (SkidwareDNS default 4444).")
add(4447, "T", "I2P-SOCKS", "I2P SOCKS proxy (SkidwareDNS default 4447).")
add(7656, "T", "I2P-SAM", "I2P SAM bridge (SkidwareDNS default 7656).")
add(10443, "T", "skid-https", "SkidwareDNS HTTPS listen default.")

keys = sorted(P.keys())
# Packed records so <clinit> stays under Java's 64KB method limit.
rows = []
for k in keys:
    proto, name, about = P[k]
    rec = "%d\t%s\t%s\t%s" % (k, ascii(proto), ascii(name).replace("\t", " "), ascii(about).replace("\t", " ").replace("\n", " "))
    rows.append(rec)
packs = []
cur, cur_sz = [], 0
for rec in rows:
    extra = len(rec) + 1
    if cur and cur_sz + extra > 48000:
        packs.append("\n".join(cur))
        cur, cur_sz = [], 0
    cur.append(rec)
    cur_sz += extra
if cur:
    packs.append("\n".join(cur))

lines = []
lines.append("package com.skidwaredns.app.proxy;\n")
lines.append("import java.util.ArrayList;\n")
lines.append("import java.util.List;\n")
lines.append("import java.util.Locale;\n\n")
lines.append("/**\n")
lines.append(" * What every TCP/UDP port 1-64000 is for.\n")
lines.append(" * Named entries come from IANA service-names-port-numbers, IpCisco\n")
lines.append(" * well-known/registered tables, and Wikipedia's list of TCP/UDP ports.\n")
lines.append(" * Everything else gets an honest unassigned note plus the RFC 6335 range.\n")
lines.append(" * Java 8, no lambdas. Packed strings keep each method under 64KB.\n")
lines.append(" */\n")
lines.append("public final class PortCatalog {\n")
lines.append("    public static final int MIN = 1;\n")
lines.append("    public static final int MAX = 64000;\n\n")
lines.append("    public static final class Info {\n")
lines.append("        public final int port;\n")
lines.append("        public final String proto;\n")
lines.append("        public final String name;\n")
lines.append("        public final String about;\n")
lines.append("        public final boolean assigned;\n")
lines.append("        public Info(int port, String proto, String name, String about, boolean assigned) {\n")
lines.append("            this.port = port; this.proto = proto; this.name = name; this.about = about; this.assigned = assigned;\n")
lines.append("        }\n")
lines.append("        public String titleLine() {\n")
lines.append("            return port + \"  \" + name + \"  (\" + proto + \")\";\n")
lines.append("        }\n")
lines.append("        public String rangeLabel() {\n")
lines.append("            if (port < 1024) return \"Well-known / system (IANA 0-1023)\";\n")
lines.append("            if (port < 49152) return \"Registered / user (IANA 1024-49151)\";\n")
lines.append("            return \"Dynamic / private (IANA 49152-65535)\";\n")
lines.append("        }\n")
lines.append("    }\n\n")
lines.append("    private static final int[] PORTS;\n")
lines.append("    private static final String[] PROTOS;\n")
lines.append("    private static final String[] NAMES;\n")
lines.append("    private static final String[] ABOUTS;\n")
lines.append("    static {\n")
lines.append("        int n = %d;\n" % len(keys))
lines.append("        PORTS = new int[n];\n")
lines.append("        PROTOS = new String[n];\n")
lines.append("        NAMES = new String[n];\n")
lines.append("        ABOUTS = new String[n];\n")
lines.append("        int i = 0;\n")
for i in range(len(packs)):
    lines.append("        i = load(pack%d(), i);\n" % i)
lines.append("        if (i != n) throw new IllegalStateException(\"catalog size \" + i);\n")
lines.append("    }\n\n")
for i, pack in enumerate(packs):
    lines.append("    private static String pack%d() { return %s; }\n" % (i, java_str(pack)))
lines.append("""
    private static int load(String pack, int i) {
        int start = 0;
        int n = pack.length();
        while (start < n) {
            int nl = pack.indexOf('\\n', start);
            if (nl < 0) nl = n;
            String line = pack.substring(start, nl);
            start = nl + 1;
            if (line.length() == 0) continue;
            int t1 = line.indexOf('\\t');
            int t2 = line.indexOf('\\t', t1 + 1);
            int t3 = line.indexOf('\\t', t2 + 1);
            PORTS[i] = Integer.parseInt(line.substring(0, t1));
            PROTOS[i] = line.substring(t1 + 1, t2);
            NAMES[i] = line.substring(t2 + 1, t3);
            ABOUTS[i] = line.substring(t3 + 1);
            i++;
        }
        return i;
    }

""")
lines.append("""    private PortCatalog() {}

    public static int namedCount() { return PORTS.length; }

    public static Info lookup(int port) {
        if (port < MIN || port > MAX) {
            return new Info(port, "?", "Out of range",
                    "SkidwareDNS Open Port covers 1-64000. " + port + " is outside that menu.", false);
        }
        int idx = indexOf(port);
        if (idx >= 0) {
            return new Info(port, expandProto(PROTOS[idx]), NAMES[idx],
                    ABOUTS[idx] + " " + privilegeNote(port), true);
        }
        return new Info(port, "TCP/UDP", "Unassigned",
                unassignedNote(port) + " " + privilegeNote(port), false);
    }

    public static List<Info> page(int start, int count) {
        List<Info> out = new ArrayList<Info>();
        int s = Math.max(MIN, start);
        int e = Math.min(MAX, s + count - 1);
        for (int p = s; p <= e; p++) out.add(lookup(p));
        return out;
    }

    public static List<Info> search(String query, int limit) {
        List<Info> out = new ArrayList<Info>();
        if (query == null) return out;
        String q = query.trim().toLowerCase(Locale.US);
        if (q.isEmpty()) return out;
        try {
            int asNum = Integer.parseInt(q);
            if (asNum >= MIN && asNum <= MAX) {
                out.add(lookup(asNum));
                return out;
            }
        } catch (Exception ignored) {}
        int cap = limit <= 0 ? 80 : limit;
        for (int i = 0; i < PORTS.length && out.size() < cap; i++) {
            String n = NAMES[i].toLowerCase(Locale.US);
            String a = ABOUTS[i].toLowerCase(Locale.US);
            if (n.indexOf(q) >= 0 || a.indexOf(q) >= 0 || String.valueOf(PORTS[i]).indexOf(q) >= 0) {
                out.add(lookup(PORTS[i]));
            }
        }
        return out;
    }

    public static String privilegeNote(int port) {
        if (port < 1024) {
            return "Well-known port (1-1023): Android apps cannot bind it without root.";
        }
        if (port < 49152) {
            return "Registered/user port (1024-49151): an app can bind it if nothing else owns it.";
        }
        return "Dynamic/private port (49152-64000): typically ephemeral; fine as a custom listen.";
    }

    public static String unassignedNote(int port) {
        String range = port < 1024 ? "well-known" : (port < 49152 ? "registered" : "dynamic");
        return "IANA has no standard service on " + port + " (" + range
                + " range, RFC 6335). Safe to use as a custom SkidwareDNS listen port.";
    }

    public static String expandProto(String p) {
        if ("T".equals(p)) return "TCP";
        if ("U".equals(p)) return "UDP";
        if ("B".equals(p)) return "TCP/UDP";
        return p == null ? "TCP/UDP" : p;
    }

    private static int indexOf(int port) {
        int lo = 0, hi = PORTS.length - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            int v = PORTS[mid];
            if (v == port) return mid;
            if (v < port) lo = mid + 1;
            else hi = mid - 1;
        }
        return -1;
    }
}
""")
OUT.write_text("".join(lines), encoding="utf-8")
print("wrote", OUT, "named", len(keys), "bytes", OUT.stat().st_size)
