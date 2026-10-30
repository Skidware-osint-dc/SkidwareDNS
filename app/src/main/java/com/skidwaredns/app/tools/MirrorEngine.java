package com.skidwaredns.app.tools;

import com.skidwaredns.app.util.SkidEventBus;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * One-way folder mirror (android-mirror style): local copy, FTP STOR, HTTP PUT, SFTP put.
 * SMB/NFS omitted (would need extra libraries).
 */
public final class MirrorEngine {

    private static final MirrorEngine INSTANCE = new MirrorEngine();
    private static final Charset UTF8 = Charset.forName("UTF-8");

    private volatile String lastOut = "Idle.";
    private volatile boolean running;

    public static MirrorEngine get() { return INSTANCE; }

    private MirrorEngine() {}

    public String lastOut() { return lastOut; }

    public boolean isRunning() { return running; }

    public String run(String srcPath, String dstSpec, String ftpUser, String ftpPass,
                      String sshHost, int sshPort, String sshUser, String sshPass) {
        running = true;
        try {
            File src = new File(srcPath == null ? "" : srcPath);
            if (!src.exists()) {
                lastOut = "Source does not exist: " + srcPath;
                return lastOut;
            }
            String dst = dstSpec == null ? "" : dstSpec.trim();
            if (dst.isEmpty()) {
                lastOut = "Set a destination (folder, ftp://host/path, http://..., or sftp:remote/path).";
                return lastOut;
            }
            List<File> files = new ArrayList<File>();
            collect(src, files);
            int copied = 0;
            int skipped = 0;
            StringBuilder log = new StringBuilder();
            String kind = kindOf(dst);
            for (int i = 0; i < files.size(); i++) {
                File f = files.get(i);
                String rel = relTo(src, f);
                boolean ok;
                if ("file".equals(kind)) ok = copyLocal(f, new File(dst, rel));
                else if ("ftp".equals(kind)) ok = copyFtp(f, dst, rel, ftpUser, ftpPass);
                else if ("http".equals(kind)) ok = copyHttp(f, dst, rel);
                else if ("sftp".equals(kind)) {
                    String remote = dst.startsWith("sftp:") ? dst.substring(5) : dst;
                    if (!remote.startsWith("/")) remote = "/" + remote;
                    if (remote.endsWith("/")) remote = remote + rel;
                    else remote = remote + "/" + rel;
                    String r = SshSessionEngine.get().sftpPut(sshHost, sshPort, sshUser, sshPass, f, remote);
                    ok = r != null && r.startsWith("put ");
                    if (!ok) log.append(r).append('\n');
                } else {
                    lastOut = "Unknown destination. Use a folder, ftp://, http:// or sftp:";
                    return lastOut;
                }
                if (ok) copied++;
                else skipped++;
            }
            lastOut = kind + "  copied " + copied + "  skipped " + skipped + "  from " + files.size()
                    + " file(s)\n" + log.toString();
            SkidEventBus.logSystem("MIRROR", src.getAbsolutePath(), lastOut);
            return lastOut;
        } finally {
            running = false;
        }
    }

    private static String kindOf(String dst) {
        String d = dst.toLowerCase();
        if (d.startsWith("ftp://")) return "ftp";
        if (d.startsWith("http://") || d.startsWith("https://")) return "http";
        if (d.startsWith("sftp:") || d.startsWith("sftp://")) return "sftp";
        return "file";
    }

    private static void collect(File src, List<File> out) {
        if (src.isFile()) { out.add(src); return; }
        File[] kids = src.listFiles();
        if (kids == null) return;
        for (int i = 0; i < kids.length; i++) {
            if (kids[i].isDirectory()) collect(kids[i], out);
            else out.add(kids[i]);
        }
    }

    private static String relTo(File root, File f) {
        try {
            String a = root.isFile() ? root.getParentFile().getCanonicalPath() : root.getCanonicalPath();
            String b = f.getCanonicalPath();
            if (b.startsWith(a)) {
                String r = b.substring(a.length()).replace('\\', '/');
                if (r.startsWith("/")) r = r.substring(1);
                return r.length() == 0 ? f.getName() : r;
            }
        } catch (Exception ignored) {}
        return f.getName();
    }

    private static boolean copyLocal(File src, File dst) {
        try {
            File parent = dst.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            if (dst.exists() && dst.length() == src.length() && dst.lastModified() >= src.lastModified()) {
                return true;
            }
            InputStream in = new BufferedInputStream(new FileInputStream(src));
            OutputStream out = new BufferedOutputStream(new FileOutputStream(dst));
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            out.close();
            in.close();
            dst.setLastModified(src.lastModified());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean copyHttp(File src, String base, String rel) {
        HttpURLConnection c = null;
        try {
            String url = base.endsWith("/") ? base + rel : base + "/" + rel;
            c = (HttpURLConnection) new URL(url).openConnection();
            c.setRequestMethod("PUT");
            c.setDoOutput(true);
            c.setConnectTimeout(8000);
            c.setReadTimeout(30000);
            c.setRequestProperty("Content-Type", "application/octet-stream");
            OutputStream os = c.getOutputStream();
            InputStream in = new FileInputStream(src);
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) os.write(buf, 0, n);
            in.close();
            os.close();
            int code = c.getResponseCode();
            return code >= 200 && code < 300;
        } catch (Exception e) {
            return false;
        } finally {
            if (c != null) c.disconnect();
        }
    }

    /** Tiny FTP client: USER/PASS/TYPE/PASV/STOR. dst = ftp://host[:port]/path */
    private static boolean copyFtp(File src, String dst, String rel, String user, String pass) {
        Socket ctrl = null;
        Socket data = null;
        try {
            URL u;
            try {
                u = new URL(dst);
            } catch (Exception e) {
                return false;
            }
            String host = u.getHost();
            int port = u.getPort() > 0 ? u.getPort() : 21;
            String basePath = u.getPath();
            if (basePath == null || basePath.isEmpty()) basePath = "/";
            String remote = basePath.endsWith("/") ? basePath + rel : basePath + "/" + rel;
            ctrl = new Socket();
            ctrl.connect(new InetSocketAddress(host, port), 8000);
            ctrl.setSoTimeout(15000);
            FtpTalk t = new FtpTalk(ctrl);
            t.expect(220);
            t.send("USER " + (user == null || user.isEmpty() ? "anonymous" : user));
            t.expect(331, 230);
            if (!t.last.startsWith("230")) {
                t.send("PASS " + (pass == null ? "" : pass));
                t.expect(230);
            }
            t.send("TYPE I");
            t.expect(200, 220);
            t.send("PASV");
            String pasv = t.expect(227);
            int[] hp = parsePasv(pasv);
            if (hp == null) return false;
            data = new Socket();
            data.connect(new InetSocketAddress(host, hp[0] * 256 + hp[1]), 8000);
            t.send("STOR " + remote);
            t.expect(150, 125);
            OutputStream os = data.getOutputStream();
            InputStream in = new FileInputStream(src);
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) os.write(buf, 0, n);
            in.close();
            os.close();
            data.close();
            data = null;
            t.expect(226);
            t.send("QUIT");
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            if (data != null) try { data.close(); } catch (Exception ignored) {}
            if (ctrl != null) try { ctrl.close(); } catch (Exception ignored) {}
        }
    }

    private static int[] parsePasv(String line) {
        int a = line.indexOf('(');
        int b = line.indexOf(')');
        if (a < 0 || b < a) return null;
        String[] p = line.substring(a + 1, b).split(",");
        if (p.length < 6) return null;
        try {
            return new int[]{Integer.parseInt(p[4].trim()), Integer.parseInt(p[5].trim())};
        } catch (Exception e) {
            return null;
        }
    }

    private static final class FtpTalk {
        final Socket s;
        final InputStream in;
        final OutputStream out;
        String last = "";

        FtpTalk(Socket s) throws Exception {
            this.s = s;
            this.in = s.getInputStream();
            this.out = s.getOutputStream();
        }

        void send(String line) throws Exception {
            out.write((line + "\r\n").getBytes(UTF8));
            out.flush();
        }

        String expect(int... codes) throws Exception {
            String line = readLine();
            last = line;
            if (line.length() >= 4 && line.charAt(3) == '-') {
                String code = line.substring(0, 3);
                while (true) {
                    String n = readLine();
                    last = n;
                    if (n.startsWith(code) && n.length() >= 4 && n.charAt(3) == ' ') break;
                }
            }
            int got = 0;
            try { got = Integer.parseInt(last.substring(0, 3)); } catch (Exception ignored) {}
            boolean ok = false;
            for (int i = 0; i < codes.length; i++) if (got == codes[i]) ok = true;
            if (!ok) throw new Exception("FTP " + last);
            return last;
        }

        String readLine() throws Exception {
            StringBuilder sb = new StringBuilder();
            while (true) {
                int c = in.read();
                if (c < 0) break;
                if (c == '\n') break;
                if (c != '\r') sb.append((char) c);
            }
            return sb.toString();
        }
    }
}
