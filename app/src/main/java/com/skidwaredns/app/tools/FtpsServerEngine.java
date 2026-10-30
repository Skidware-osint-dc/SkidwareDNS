package com.skidwaredns.app.tools;

import com.skidwaredns.app.proxy.OpenPortEngine;
import com.skidwaredns.app.util.SkidEventBus;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * LAN FTP server (dotnet-ftps-server style): USER/PASS, PASV, LIST/RETR/STOR,
 * UTF-8. Implicit TLS is off in this build (no bundled cert); password still
 * required. Default listen 2121 so Android can bind without root.
 */
public final class FtpsServerEngine {

    private static final FtpsServerEngine INSTANCE = new FtpsServerEngine();
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final AtomicInteger SESS = new AtomicInteger(1);

    private volatile boolean running;
    private volatile int listenPort;
    private volatile String bindIp = "0.0.0.0";
    private volatile String status = "Stopped.";
    private ServerSocket control;
    private Thread acceptTh;
    private File root;
    private String user = "skid";
    private String pass = "skid";

    public static FtpsServerEngine get() { return INSTANCE; }

    private FtpsServerEngine() {}

    public boolean isRunning() { return running; }

    public String status() { return status; }

    public int listenPort() { return listenPort; }

    public synchronized String start(int port, String user, String pass, File rootDir) {
        if (running) return "Already running on " + bindIp + ":" + listenPort;
        int p = ToolNet.clampPort(port, 2121);
        if (p < 1024) return "Pick 1024+ (Android cannot bind well-known ports).";
        File root = rootDir;
        if (root == null) return "No share folder.";
        if (!root.exists() && !root.mkdirs()) return "Cannot create " + root.getAbsolutePath();
        this.root = root;
        this.user = user == null || user.isEmpty() ? "skid" : user;
        this.pass = pass == null ? "" : pass;
        try {
            ServerSocket ss = new ServerSocket();
            ss.setReuseAddress(true);
            String ip = OpenPortEngine.primaryDeviceIpv4();
            if (ip == null || ip.isEmpty() || "0.0.0.0".equals(ip)) ip = "0.0.0.0";
            ss.bind(new InetSocketAddress(ip, p), 8);
            this.control = ss;
            this.listenPort = ss.getLocalPort();
            this.bindIp = ip;
            this.running = true;
            this.status = "FTP " + bindIp + ":" + listenPort + "  root " + root.getAbsolutePath();
            final FtpsServerEngine self = this;
            acceptTh = new Thread(new Runnable() {
                @Override public void run() { self.acceptLoop(); }
            }, "skid-ftp-accept");
            acceptTh.setDaemon(true);
            acceptTh.start();
            SkidEventBus.logSystem("FTP", bindIp + ":" + listenPort, "STARTED");
            return status;
        } catch (Exception e) {
            running = false;
            status = "Start failed: " + e.getMessage();
            return status;
        }
    }

    public synchronized String stop() {
        running = false;
        ServerSocket ss = control;
        control = null;
        if (ss != null) try { ss.close(); } catch (Exception ignored) {}
        status = "Stopped.";
        SkidEventBus.logSystem("FTP", bindIp + ":" + listenPort, "STOPPED");
        return status;
    }

    private void acceptLoop() {
        while (running && control != null) {
            try {
                final Socket c = control.accept();
                Thread t = new Thread(new Runnable() {
                    @Override public void run() { session(c); }
                }, "skid-ftp-" + SESS.getAndIncrement());
                t.setDaemon(true);
                t.start();
            } catch (Exception e) {
                if (!running) break;
            }
        }
    }

    private void session(Socket c) {
        Session s = new Session(c);
        try {
            s.out("220 SkidwareDNS FTP ready (UTF-8, PASV).");
            String line;
            while (running && (line = s.readLine()) != null) {
                if (!handle(s, line)) break;
            }
        } catch (Exception ignored) {
        } finally {
            s.close();
        }
    }

    private boolean handle(Session s, String line) throws IOException {
        String cmd = line;
        String arg = "";
        int sp = line.indexOf(' ');
        if (sp > 0) {
            cmd = line.substring(0, sp);
            arg = line.substring(sp + 1).trim();
        }
        cmd = cmd.toUpperCase(Locale.US);
        if ("QUIT".equals(cmd)) { s.out("221 Bye."); return false; }
        if ("NOOP".equals(cmd) || "NOP".equals(cmd)) { s.out("200 OK."); return true; }
        if ("SYST".equals(cmd)) { s.out("215 UNIX Type: L8"); return true; }
        if ("FEAT".equals(cmd)) {
            s.out("211-Features:");
            s.out(" UTF8");
            s.out(" SIZE");
            s.out(" PASV");
            s.out("211 End");
            return true;
        }
        if ("OPTS".equals(cmd)) { s.out("200 OK."); return true; }
        if ("USER".equals(cmd)) {
            s.pendingUser = arg;
            s.out("331 Password required.");
            return true;
        }
        if ("PASS".equals(cmd)) {
            if (user.equals(s.pendingUser) && pass.equals(arg)) {
                s.authed = true;
                s.out("230 Logged in.");
            } else {
                s.out("530 Login incorrect.");
            }
            return true;
        }
        if (!s.authed) { s.out("530 Log in first."); return true; }
        if ("PWD".equals(cmd) || "XPWD".equals(cmd)) {
            s.out("257 \"" + s.cwdRel + "\" is current directory.");
            return true;
        }
        if ("CWD".equals(cmd) || "XCWD".equals(cmd)) {
            File next = resolve(s, arg, true);
            if (next != null && next.isDirectory()) {
                s.cwdRel = rel(next);
                s.out("250 Directory changed.");
            } else s.out("550 Failed.");
            return true;
        }
        if ("CDUP".equals(cmd)) { return handle(s, "CWD .."); }
        if ("TYPE".equals(cmd)) { s.out("200 Type set."); return true; }
        if ("PASV".equals(cmd)) { return pasv(s); }
        if ("LIST".equals(cmd) || "NLST".equals(cmd)) { return list(s, arg, "NLST".equals(cmd)); }
        if ("RETR".equals(cmd)) { return retr(s, arg); }
        if ("STOR".equals(cmd)) { return stor(s, arg); }
        if ("SIZE".equals(cmd)) {
            File f = resolve(s, arg, false);
            if (f != null && f.isFile()) s.out("213 " + f.length());
            else s.out("550 No file.");
            return true;
        }
        if ("DELE".equals(cmd)) {
            File f = resolve(s, arg, false);
            if (f != null && f.isFile() && f.delete()) s.out("250 Deleted.");
            else s.out("550 Failed.");
            return true;
        }
        if ("MKD".equals(cmd) || "XMKD".equals(cmd)) {
            File f = resolve(s, arg, false);
            if (f != null && (f.mkdirs() || f.isDirectory())) s.out("257 \"" + rel(f) + "\" created.");
            else s.out("550 Failed.");
            return true;
        }
        if ("RMD".equals(cmd) || "XRMD".equals(cmd)) {
            File f = resolve(s, arg, false);
            if (f != null && f.isDirectory() && f.delete()) s.out("250 Removed.");
            else s.out("550 Failed.");
            return true;
        }
        if ("AUTH".equals(cmd)) {
            s.out("502 TLS not configured (plain FTP on LAN).");
            return true;
        }
        s.out("502 Command not implemented.");
        return true;
    }

    private boolean pasv(Session s) throws IOException {
        if (s.pasv != null) try { s.pasv.close(); } catch (Exception ignored) {}
        ServerSocket ps = new ServerSocket(0, 1, InetAddress.getByName(bindIp));
        s.pasv = ps;
        int p = ps.getLocalPort();
        String ip = bindIp;
        if ("0.0.0.0".equals(ip) || "127.0.0.1".equals(ip)) ip = OpenPortEngine.primaryDeviceIpv4();
        String[] oct = ip.split("\\.");
        if (oct.length != 4) {
            s.out("425 PASV failed.");
            return true;
        }
        s.out("227 Entering Passive Mode (" + oct[0] + "," + oct[1] + "," + oct[2] + "," + oct[3]
                + "," + (p / 256) + "," + (p % 256) + ").");
        return true;
    }

    private Socket data(Session s) throws IOException {
        if (s.pasv == null) return null;
        s.pasv.setSoTimeout(15000);
        Socket d = s.pasv.accept();
        try { s.pasv.close(); } catch (Exception ignored) {}
        s.pasv = null;
        return d;
    }

    private boolean list(Session s, String arg, boolean namesOnly) throws IOException {
        File dir = resolve(s, arg, true);
        if (dir == null || !dir.isDirectory()) { s.out("550 No directory."); return true; }
        s.out("150 Opening data.");
        Socket d = data(s);
        if (d == null) { s.out("425 No PASV."); return true; }
        try {
            OutputStream os = d.getOutputStream();
            File[] files = dir.listFiles();
            if (files == null) files = new File[0];
            SimpleDateFormat df = new SimpleDateFormat("MMM dd HH:mm", Locale.US);
            for (int i = 0; i < files.length; i++) {
                File f = files[i];
                String row;
                if (namesOnly) row = f.getName() + "\r\n";
                else {
                    String mode = f.isDirectory() ? "drwxr-xr-x" : "-rw-r--r--";
                    row = mode + " 1 skid skid " + f.length() + " " + df.format(new Date(f.lastModified()))
                            + " " + f.getName() + "\r\n";
                }
                os.write(row.getBytes(UTF8));
            }
            os.flush();
        } finally {
            try { d.close(); } catch (Exception ignored) {}
        }
        s.out("226 Transfer complete.");
        return true;
    }

    private boolean retr(Session s, String arg) throws IOException {
        File f = resolve(s, arg, false);
        if (f == null || !f.isFile()) { s.out("550 No file."); return true; }
        s.out("150 Opening data.");
        Socket d = data(s);
        if (d == null) { s.out("425 No PASV."); return true; }
        InputStream in = null;
        try {
            in = new BufferedInputStream(new FileInputStream(f));
            OutputStream os = d.getOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) os.write(buf, 0, n);
            os.flush();
        } finally {
            if (in != null) try { in.close(); } catch (Exception ignored) {}
            try { d.close(); } catch (Exception ignored) {}
        }
        s.out("226 Transfer complete.");
        return true;
    }

    private boolean stor(Session s, String arg) throws IOException {
        File f = resolve(s, arg, false);
        if (f == null) { s.out("550 Bad path."); return true; }
        s.out("150 Opening data.");
        Socket d = data(s);
        if (d == null) { s.out("425 No PASV."); return true; }
        OutputStream out = null;
        try {
            out = new BufferedOutputStream(new FileOutputStream(f));
            InputStream in = d.getInputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            out.flush();
        } finally {
            if (out != null) try { out.close(); } catch (Exception ignored) {}
            try { d.close(); } catch (Exception ignored) {}
        }
        s.out("226 Transfer complete.");
        return true;
    }

    private File resolve(Session s, String arg, boolean dirOk) {
        String rel = s.cwdRel;
        if (arg != null && arg.length() > 0) {
            if (arg.startsWith("/")) rel = arg;
            else if ("..".equals(arg)) {
                int sl = rel.lastIndexOf('/');
                rel = sl > 0 ? rel.substring(0, sl) : "/";
            } else if (!".".equals(arg)) {
                if ("/".equals(rel)) rel = "/" + arg;
                else rel = rel + "/" + arg;
            }
        }
        rel = rel.replace('\\', '/');
        while (rel.contains("//")) rel = rel.replace("//", "/");
        if (!rel.startsWith("/")) rel = "/" + rel;
        File f = new File(root, rel.substring(1));
        try {
            String canon = f.getCanonicalPath();
            String rootCanon = root.getCanonicalPath();
            if (!canon.equals(rootCanon) && !canon.startsWith(rootCanon + File.separator)) return null;
            return new File(canon);
        } catch (Exception e) {
            return null;
        }
    }

    private String rel(File f) {
        try {
            String canon = f.getCanonicalPath();
            String rootCanon = root.getCanonicalPath();
            if (canon.equals(rootCanon)) return "/";
            String r = canon.substring(rootCanon.length()).replace('\\', '/');
            if (!r.startsWith("/")) r = "/" + r;
            return r;
        } catch (Exception e) {
            return "/";
        }
    }

    private static final class Session {
        final Socket c;
        BufferedReader in;
        OutputStream raw;
        boolean authed;
        String pendingUser = "";
        String cwdRel = "/";
        ServerSocket pasv;

        Session(Socket c) {
            this.c = c;
            try {
                c.setSoTimeout(120000);
                in = new BufferedReader(new InputStreamReader(c.getInputStream(), UTF8));
                raw = c.getOutputStream();
            } catch (Exception ignored) {}
        }

        String readLine() throws IOException {
            return in == null ? null : in.readLine();
        }

        void out(String line) throws IOException {
            if (raw == null) return;
            raw.write((line + "\r\n").getBytes(UTF8));
            raw.flush();
        }

        void close() {
            if (pasv != null) try { pasv.close(); } catch (Exception ignored) {}
            try { c.close(); } catch (Exception ignored) {}
        }
    }
}
