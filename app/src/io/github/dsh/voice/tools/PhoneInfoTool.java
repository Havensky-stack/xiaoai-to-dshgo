package io.github.dsh.voice.tools;

import android.os.Build;
import android.os.SystemClock;

import org.json.JSONObject;

import io.github.dsh.voice.agent.Tool;

public class PhoneInfoTool implements Tool {
    public String name() { return "phone_info"; }
    public String description() { return "读取本机型号、Android 版本、运行时长。无副作用，可随时调用。"; }
    public int risk() { return SAFE; }

    public JSONObject parameters() {
        try {
            return new JSONObject().put("type", "object").put("properties", new JSONObject());
        } catch (Exception e) { return new JSONObject(); }
    }

    public JSONObject execute(JSONObject a) throws Exception {
        long mins = SystemClock.elapsedRealtime() / 60000L;
        return new JSONObject()
                .put("model", Build.MODEL)
                .put("brand", Build.BRAND)
                .put("android", Build.VERSION.RELEASE)
                .put("sdk", Build.VERSION.SDK_INT)
                .put("uptime_minutes", mins);
    }
}
