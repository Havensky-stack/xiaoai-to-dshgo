package io.github.dsh.xiaoai;

import java.lang.reflect.Method;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class Main implements IXposedHookLoadPackage {
    private static final String MUTE = "/sdcard/Download/DSH-Go/xiaoai-mute.txt";
    private static final String STATUS = "/sdcard/Download/DSH-Go/xiaoai-bridge-status.json";

    private static boolean muted() {
        try {
            java.io.File f = new java.io.File(MUTE);
            if (!f.exists()) return false;
            java.io.BufferedReader r = new java.io.BufferedReader(new java.io.FileReader(f));
            String line = r.readLine(); r.close();
            return line != null && line.trim().endsWith("1");
        } catch (Throwable t) { return false; }
    }

    private static void writeStatus(String lastText) {
        try {
            String role = "";
            try {
                android.content.Context c = de.robv.android.xposed.AndroidAppHelper.currentApplication();
                if (c != null) role = android.provider.Settings.Secure.getString(c.getContentResolver(), "assistant");
            } catch (Throwable ignored) { }
            org.json.JSONObject o = new org.json.JSONObject();
            o.put("role", role == null ? "" : role);
            o.put("moduleLoaded", true);
            o.put("lastText", lastText == null ? "" : lastText);
            o.put("lastAt", (lastText == null || lastText.isEmpty()) ? 0L : System.currentTimeMillis());
            java.io.FileWriter w = new java.io.FileWriter(STATUS, false);
            w.write(o.toString()); w.close();
        } catch (Throwable t) {
            XposedBridge.log("DSHASAN: status write failed " + t);
        }
    }
    private static final String TARGET = "io.github.ccch1mneyyy.dsh_app";
    private static final String ACTIVITY = "io.github.ccch1mneyyy.dsh_app.share.ShareActivity";
    private static final int MAXLEN = 300;

    private static volatile boolean listening = false;
    private static volatile String candidate = null;
    private static volatile long lastSent = 0L;
    private static volatile String lastText = "";

    @Override
    public void handleLoadPackage(LoadPackageParam lp) throws Throwable {
        if (!"com.miui.voiceassist".equals(lp.packageName)) return;
        XposedBridge.log("DSHASAN: v5 installed proc=" + lp.processName);
        try {
            Class<?> tv = Class.forName("android.widget.TextView", false, lp.classLoader);
            Method m = tv.getDeclaredMethod("setText", CharSequence.class);
            XposedBridge.hookMethod(m, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam p) {
                    Object a = (p.args != null && p.args.length > 0) ? p.args[0] : null;
                    if (!(a instanceof CharSequence)) return;
                    String s = a.toString().trim();
                    if (s.isEmpty() || s.length() > MAXLEN) return;

                    if (s.equals("我在听")) {
                        listening = true; candidate = null;
                        XposedBridge.log("DSHASAN: listen-start");
                        return;
                    }
                    if (s.startsWith("思考中")) {
                        if (listening && candidate != null) forward(p.thisObject, candidate);
                        listening = false; candidate = null;
                        XposedBridge.log("DSHASAN: listen-end");
                        return;
                    }
                    if (!listening) return;
                    if (isNoise(s)) return;
                    candidate = s;
                }
            });
            try {
                Class<?> tts = Class.forName("android.speech.tts.TextToSpeech", false, lp.classLoader);
                XposedBridge.hookAllMethods(tts, "speak", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam p) {
                        if (!muted()) return;
                        XposedBridge.log("DSHASAN: tts suppressed");
                        p.setResult(Integer.valueOf(0));
                    }
                });
                XposedBridge.log("DSHASAN: tts hooked");
            } catch (Throwable t) {
                XposedBridge.log("DSHASAN: tts hook failed " + t);
            }
            XposedBridge.log("DSHASAN: v5 hooked");
            writeStatus("");
        } catch (Throwable t) {
            XposedBridge.log("DSHASAN: hook failed: " + t);
        }
    }

    private static boolean isNoise(String s) {
        switch (s) {
            case "快速": case "问小爱": case "小爱陪伴": case "随心修图":
            case "翻译": case "闹钟": case "取消": case "发送": case "我在听":
                return true;
            default:
                return false;
        }
    }

    private static void forward(Object viewObj, String text) {
        long now = System.currentTimeMillis();
        if (text.equals(lastText) && now - lastSent < 5000) {
            XposedBridge.log("DSHASAN: skip dup " + text);
            return;
        }
        try {
            Context ctx = (viewObj instanceof View) ? ((View) viewObj).getContext() : null;
            if (ctx == null) { XposedBridge.log("DSHASAN: no ctx for " + text); return; }
            lastText = text; lastSent = now;
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_TEXT, text);
            i.setClassName(TARGET, ACTIVITY);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            ctx.startActivity(i);
            XposedBridge.log("DSHASAN: forwarded -> " + text);
            writeStatus(text);
        } catch (Throwable t) {
            XposedBridge.log("DSHASAN: forward failed: " + t);
        }
    }
}
