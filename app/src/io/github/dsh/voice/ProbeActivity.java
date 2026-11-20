package io.github.dsh.voice;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ProbeActivity extends Activity {
    private static final String SVC = "com.aios.osbot.action.CLI_COMMAND";
    private static final String PKG = "com.miui.voiceassist";
    private static final String DESC = "com.aios.apptoolsdk.aidl.ICliCommandService";

    private TextView out;
    private final StringBuilder sb = new StringBuilder();

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setPadding(40, 80, 40, 40);
        ll.setBackgroundColor(0xFF101016);
        out = new TextView(this);
        out.setTextSize(13f);
        out.setTextColor(0xFFE0E0F0);
        out.setGravity(Gravity.START);
        ll.addView(out);
        setContentView(ll);

        log("binding…");
        Intent i = new Intent(SVC).setPackage(PKG);
        boolean ok = bindService(i, conn, Context.BIND_AUTO_CREATE);
        log("bindService -> " + ok);
    }

    private final ServiceConnection conn = new ServiceConnection() {
        public void onServiceConnected(ComponentName n, IBinder binder) {
            log("connected: " + n);
            probe(binder);
        }
        public void onServiceDisconnected(ComponentName n) { log("disconnected"); }
    };

    private static final String[] PAYLOADS = {
        "{\"id\":\"1\",\"method\":\"asr.start\"}",
        "{\"id\":\"1\",\"method\":\"asr.startRecognize\"}",
        "{\"id\":\"1\",\"method\":\"asr.stop\"}",
        "{\"id\":\"1\",\"method\":\"asr.cancel\"}",
        "{\"id\":\"1\",\"method\":\"tts.open\"}",
        "{\"id\":\"1\",\"method\":\"agent.submit\"}",
        "{\"id\":\"1\",\"method\":\"agent.consent\"}",
        "{\"id\":\"1\",\"method\":\"auth.status\"}",
        "{\"id\":\"1\",\"method\":\"auth.login\"}",
        "{\"id\":\"1\",\"method\":\"session.open\"}",
        "{\"id\":\"1\",\"method\":\"framework.ping\"}",
        "{\"id\":\"1\",\"method\":\"ping\"}",
        "{\"id\":\"1\",\"method\":\"device.info\"}",
        "{\"id\":\"1\",\"method\":\"app.list\"}",
        "{\"id\":\"1\",\"method\":\"tool.list\"}",
        "{\"id\":\"1\",\"method\":\"shell.exec\"}",
        "{\"id\":\"1\",\"method\":\"cmd.exec\"}",
        "{\"id\":\"1\",\"method\":\"system.info\"}",
        "{\"id\":\"1\",\"method\":\"cli.ping\"}",
        "{\"id\":\"1\",\"method\":\"version\"}"
    };

    private void probe(IBinder binder) {
        try {
            String open = call(binder, 1, "{\"id\":\"1\",\"method\":\"session.open\"}");
            log("session.open => " + open);
            String sid = null;
            try { sid = new org.json.JSONObject(open).optString("sessionId", null); } catch (Throwable ignored) {}
            if (sid == null || sid.isEmpty()) { log("no sessionId"); return; }
            String a = "{\"id\":\"2\",\"method\":\"agent.submit\",\"session_id\":\"" + sid + "\",\"text\":\"用一句话介绍你自己\"}";
            log("A root-fields => " + call(binder, 1, a));
            String b2 = "{\"id\":\"3\",\"method\":\"agent.submit\",\"params\":{\"sessionId\":\"" + sid + "\",\"text\":\"用一句话介绍你自己\"}}";
            log("B camelCase => " + call(binder, 1, b2));
            Cb cb = new Cb();
            log("S1 stream open => " + stream(binder, "{\"id\":\"s1\",\"method\":\"session.open\"}", cb));
            Thread.sleep(800);
            log("S2 stream submit => " + stream(binder, b2, cb));
            Thread.sleep(800);
            Cb cb2 = new Cb();
            String st = tx4(binder, "{\"id\":\"u0\",\"method\":\"session.open\"}", cb2);
            log("upload.open => " + st);
            Thread.sleep(1500);
            log("lastSession=" + lastSession);
            if (lastSession != null && !lastSession.isEmpty()) {
                tx5(binder, st, "{\"id\":\"u1\",\"method\":\"agent.submit\",\"params\":{\"sessionId\":\"" + lastSession + "\",\"text\":\"用一句话介绍你自己\"}}");
                log("upload.chunk sent");
                Thread.sleep(2500);
            }
            tx6(binder, st);
            log("upload.end done");
        } catch (Throwable t) {
            log("probe !! " + t.getClass().getSimpleName() + ": " + t.getMessage());
        }
    }

    volatile String lastSession = null;

    class Cb extends android.os.Binder {
        @Override
        protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) {
            try {
                data.enforceInterface("com.aios.apptoolsdk.aidl.ICliCommandCallback");
                String v = data.readString();
                try {
                    org.json.JSONObject o = new org.json.JSONObject(v);
                    String sid2 = o.optString("sessionId", null);
                    if (sid2 != null && !sid2.isEmpty()) lastSession = sid2;
                } catch (Throwable ignored) { }
                log("CB#" + code + " => " + (v == null ? "null" : (v.length() > 160 ? v.substring(0,160) : v)));
                if (reply != null) reply.writeNoException();
            } catch (Throwable t) { log("CB#" + code + " err " + t); }
            return true;
        }
    }

    private String stream(IBinder b, String json, Cb cb) throws Exception {
        Parcel d = Parcel.obtain(); Parcel r = Parcel.obtain();
        try {
            d.writeInterfaceToken(DESC);
            d.writeString(json);
            d.writeStrongBinder(cb);
            b.transact(2, d, r, 0);
            r.readException();
            return r.readString();
        } finally { d.recycle(); r.recycle(); }
    }

    private String tx4(IBinder b, String json, Cb cb) throws Exception {
        Parcel d = Parcel.obtain(); Parcel r = Parcel.obtain();
        try {
            d.writeInterfaceToken(DESC); d.writeString(json); d.writeStrongBinder(cb);
            b.transact(4, d, r, 0); r.readException();
            return r.readString();
        } finally { d.recycle(); r.recycle(); }
    }

    private void tx5(IBinder b, String streamId, String chunk) throws Exception {
        Parcel d = Parcel.obtain(); Parcel r = Parcel.obtain();
        try {
            d.writeInterfaceToken(DESC); d.writeString(streamId); d.writeString(chunk);
            b.transact(5, d, r, 0); r.readException();
        } finally { d.recycle(); r.recycle(); }
    }

    private void tx6(IBinder b, String streamId) throws Exception {
        Parcel d = Parcel.obtain(); Parcel r = Parcel.obtain();
        try {
            d.writeInterfaceToken(DESC); d.writeString(streamId);
            b.transact(6, d, r, 0); r.readException();
        } finally { d.recycle(); r.recycle(); }
    }

    private String call(IBinder b, int code, String cmd) throws Exception {
        Parcel d = Parcel.obtain();
        Parcel r = Parcel.obtain();
        try {
            d.writeInterfaceToken(DESC);
            d.writeString(cmd);
            b.transact(code, d, r, 0);
            r.readException();
            return r.readString();
        } finally {
            d.recycle();
            r.recycle();
        }
    }

    private String trim(String s) {
        if (s == null) return "null";
        return s.length() > 300 ? s.substring(0, 300) + "…" : s;
    }

    private void log(String s) {
        android.util.Log.i("DSHVOICE", "PROBE " + s);
        sb.append(s).append('\n');
        out.setText(sb.toString());
    }
}
