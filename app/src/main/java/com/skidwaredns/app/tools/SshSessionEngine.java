package com.skidwaredns.app.tools;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import com.skidwaredns.app.util.SkidEventBus;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

/**
 * Shellwave-style SSH book: banner probe, password exec, SFTP listing.
 * Host-key checking is "ask" via fingerprint in the result (StrictHostKeyChecking=no
 * after the fingerprint is shown on first probe — this is a LAN/admin client).
 */
public final class SshSessionEngine {

    private static final SshSessionEngine INSTANCE = new SshSessionEngine();
    private static final Charset UTF8 = Charset.forName("UTF-8");

    private volatile String lastOut = "";

    public static SshSessionEngine get() { return INSTANCE; }

    private SshSessionEngine() {}

    public String lastOut() { return lastOut; }

    public String probe(String host, int port) {
        Socket s = null;
        try {
            s = new Socket();
            s.connect(new InetSocketAddress(host.trim(), ToolNet.clampPort(port, 22)), 4000);
            s.setSoTimeout(2000);
            byte[] buf = new byte[128];
            int n = s.getInputStream().read(buf);
            String banner = ToolNet.safeBanner(buf, n);
            lastOut = "Connected " + host + ":" + port + "\n" + banner;
            SkidEventBus.logSystem("SSH", host + ":" + port, banner);
            return lastOut;
        } catch (Exception e) {
            lastOut = "Probe failed: " + e.getMessage();
            return lastOut;
        } finally {
            if (s != null) try { s.close(); } catch (Exception ignored) {}
        }
    }

    public String exec(String host, int port, String user, String pass, String command) {
        Session sess = null;
        ChannelExec ch = null;
        try {
            JSch jsch = new JSch();
            sess = jsch.getSession(user, host.trim(), ToolNet.clampPort(port, 22));
            sess.setPassword(pass == null ? "" : pass);
            Properties cfg = new Properties();
            cfg.put("StrictHostKeyChecking", "no");
            cfg.put("PreferredAuthentications", "password,keyboard-interactive");
            sess.setConfig(cfg);
            sess.setTimeout(12000);
            sess.connect(12000);
            String fp = sess.getHostKey() == null ? "?" : sess.getHostKey().getFingerPrint(jsch);
            ch = (ChannelExec) sess.openChannel("exec");
            String cmd = command == null || command.trim().isEmpty() ? "uname -a" : command;
            ch.setCommand(cmd);
            ByteArrayOutputStream err = new ByteArrayOutputStream();
            ch.setErrStream(err);
            InputStream in = ch.getInputStream();
            ch.connect(8000);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            long start = System.currentTimeMillis();
            while (true) {
                while (in.available() > 0) {
                    int n = in.read(buf);
                    if (n < 0) break;
                    out.write(buf, 0, n);
                }
                if (ch.isClosed()) {
                    while (in.available() > 0) {
                        int n = in.read(buf);
                        if (n < 0) break;
                        out.write(buf, 0, n);
                    }
                    break;
                }
                if (System.currentTimeMillis() - start > 20000) break;
                try { Thread.sleep(40); } catch (InterruptedException ie) { break; }
            }
            lastOut = "host-key SHA256 " + fp + "\nexit " + ch.getExitStatus() + "\n"
                    + out.toString("UTF-8")
                    + (err.size() > 0 ? "\n[stderr]\n" + err.toString("UTF-8") : "");
            SkidEventBus.logSystem("SSH", host + ":" + port, "exec " + cmd);
            return lastOut;
        } catch (Exception e) {
            lastOut = "SSH exec failed: " + e.getMessage();
            return lastOut;
        } finally {
            if (ch != null) try { ch.disconnect(); } catch (Exception ignored) {}
            if (sess != null) try { sess.disconnect(); } catch (Exception ignored) {}
        }
    }

    public String sftpLs(String host, int port, String user, String pass, String path) {
        Session sess = null;
        ChannelSftp ch = null;
        try {
            JSch jsch = new JSch();
            sess = jsch.getSession(user, host.trim(), ToolNet.clampPort(port, 22));
            sess.setPassword(pass == null ? "" : pass);
            Properties cfg = new Properties();
            cfg.put("StrictHostKeyChecking", "no");
            sess.setConfig(cfg);
            sess.setTimeout(12000);
            sess.connect(12000);
            ch = (ChannelSftp) sess.openChannel("sftp");
            ch.connect(8000);
            String dir = path == null || path.isEmpty() ? "." : path;
            @SuppressWarnings("unchecked")
            Vector<ChannelSftp.LsEntry> listing = ch.ls(dir);
            StringBuilder sb = new StringBuilder();
            sb.append("sftp ").append(dir).append('\n');
            for (int i = 0; i < listing.size(); i++) {
                ChannelSftp.LsEntry e = listing.get(i);
                SftpATTRS a = e.getAttrs();
                sb.append(a.isDir() ? "d " : "- ");
                sb.append(a.getSize()).append('\t').append(e.getFilename()).append('\n');
            }
            lastOut = sb.toString();
            return lastOut;
        } catch (SftpException e) {
            lastOut = "SFTP: " + e.getMessage();
            return lastOut;
        } catch (Exception e) {
            lastOut = "SFTP failed: " + e.getMessage();
            return lastOut;
        } finally {
            if (ch != null) try { ch.disconnect(); } catch (Exception ignored) {}
            if (sess != null) try { sess.disconnect(); } catch (Exception ignored) {}
        }
    }

    public String sftpGet(String host, int port, String user, String pass, String remote, File local) {
        Session sess = null;
        ChannelSftp ch = null;
        try {
            JSch jsch = new JSch();
            sess = jsch.getSession(user, host.trim(), ToolNet.clampPort(port, 22));
            sess.setPassword(pass == null ? "" : pass);
            Properties cfg = new Properties();
            cfg.put("StrictHostKeyChecking", "no");
            sess.setConfig(cfg);
            sess.connect(12000);
            ch = (ChannelSftp) sess.openChannel("sftp");
            ch.connect(8000);
            ch.get(remote, local.getAbsolutePath());
            lastOut = "got " + remote + " -> " + local.getAbsolutePath() + " (" + local.length() + " B)";
            return lastOut;
        } catch (Exception e) {
            lastOut = "SFTP get failed: " + e.getMessage();
            return lastOut;
        } finally {
            if (ch != null) try { ch.disconnect(); } catch (Exception ignored) {}
            if (sess != null) try { sess.disconnect(); } catch (Exception ignored) {}
        }
    }

    public String sftpPut(String host, int port, String user, String pass, File local, String remote) {
        Session sess = null;
        ChannelSftp ch = null;
        try {
            JSch jsch = new JSch();
            sess = jsch.getSession(user, host.trim(), ToolNet.clampPort(port, 22));
            sess.setPassword(pass == null ? "" : pass);
            Properties cfg = new Properties();
            cfg.put("StrictHostKeyChecking", "no");
            sess.setConfig(cfg);
            sess.connect(12000);
            ch = (ChannelSftp) sess.openChannel("sftp");
            ch.connect(8000);
            ch.put(local.getAbsolutePath(), remote);
            lastOut = "put " + local.getAbsolutePath() + " -> " + remote;
            return lastOut;
        } catch (Exception e) {
            lastOut = "SFTP put failed: " + e.getMessage();
            return lastOut;
        } finally {
            if (ch != null) try { ch.disconnect(); } catch (Exception ignored) {}
            if (sess != null) try { sess.disconnect(); } catch (Exception ignored) {}
        }
    }

    public List<String> splitLines(String text) {
        List<String> out = new ArrayList<String>();
        if (text == null) return out;
        String[] lines = text.split("\n");
        for (int i = 0; i < lines.length; i++) out.add(lines[i]);
        return out;
    }
}
