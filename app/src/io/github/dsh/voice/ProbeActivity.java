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
        "{\"id\":\"1\",\"method\":\"help\"}",
        "{\"id\":\"1\",\"method\":\"list\"}",
        "{\"id\":\"1\",\"method\":\"shell\",\"params\":{\"command\":\"id\"}}",
        "{\"id\":\"1\",\"method\":\"exec\"}",
        "{\"id\":\"1\",\"method\":\"execute\",\"params\":{\"command\":\"id\"}}",
        "{\"id\":\"1\",\"method\":\"tools/list\"}",
        "{\"id\":\"1\",\"method\":\"run\",\"params\":{\"cmd\":\"id\"}}"
    };

    private void probe(IBinder binder) {
        for (String payload : PAYLOADS) {
            try {
                String r = call(binder, 1, payload);
                log("payload " + payload + " -> " + trim(r));
            } catch (Throwable t) {
                log("payload " + payload + " !! " + t.getMessage());
            }
        }
        for (int code = 1; code <= 8; code++) {
            try {
                String r = call(binder, code, "id");
                log("code " + code + " -> " + trim(r));
            } catch (Throwable t) {
                log("code " + code + " !! " + t.getClass().getSimpleName() + ": " + t.getMessage());
            }
        }
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
