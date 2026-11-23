package io.github.dsh.xiaoai;

import java.lang.reflect.Method;

import android.content.Context;
import android.content.Intent;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class Main implements IXposedHookLoadPackage {
    private static final String TARGET = "io.github.ccch1mneyyy.dsh_app";
    private static final String ACTIVITY = "io.github.ccch1mneyyy.dsh_app.share.ShareActivity";
    private static final String MUTE = "/sdcard/Download/DSH-Go/xiaoai-mute.txt";
    private static final String STATUS = "/sdcard/Download/DSH-Go/xiaoai-bridge-status.json";

    private static volatile long lastSent = 0;
    private static volatile Context appCtx = null;

    @Override
    public void handleLoadPackage(LoadPackageParam lp) throws Throwable {
        if (!"com.miui.voiceassist".equals(lp.packageName)) return;
        XposedBridge.log("DSHASAN: v11 installed proc=" + lp.processName);

        try {
            Class<?> om = XposedHelpers.findClassIfExists(
                    "com.xiaomi.voiceassistant.instruction.base.OperationManager", lp.classLoader);
            if (om == null) {
                XposedBridge.log("DSHASAN: OperationManager not found");
            } else {
                XposedBridge.hookAllMethods(om, "onAsrResult", new XC_MethodHook() {
                    @Override protected void beforeHookedMethod(MethodHookParam p) {
                        try {
                            if (p.args == null || p.args.length == 0 || p.args[0] == null) return;
                            Object q = p.args[0];
                            Method getQuery = q.getClass().getMethod("getQuery");
                            Object v = getQuery.invoke(q);
                            String text = v == null ? "" : v.toString().trim();
                            if (text.isEmpty()) return;
                            XposedBridge.log("DSHASAN: asr -> " + text);
                            forward(p.thisObject, text);
                        } catch (Throwable t) {
                            XposedBridge.log("DSHASAN: asr parse failed " + t);
                        }
                    }
                });
                XposedBridge.log("DSHASAN: asr hooked");
            }
        } catch (Throwable t) {
            XposedBridge.log("DSHASAN: asr hook failed " + t);
        }

        try {
            Class<?> tts = Class.forName("android.speech.tts.TextToSpeech", false, lp.classLoader);
            XposedBridge.hookAllMethods(tts, "speak", new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam p) {
                    if (!muted()) return;
                    XposedBridge.log("DSHASAN: tts suppressed");
                    p.setResult(Integer.valueOf(0));
                }
            });
        } catch (Throwable t) {
            XposedBridge.log("DSHASAN: tts hook failed " + t);
        }
        writeStatus("");
    }

    private static boolean muted() {
        try {
            java.io.File f = new java.io.File(MUTE);
            if (!f.exists()) return false;
            java.io.BufferedReader r = new java.io.BufferedReader(new java.io.FileReader(f));
            String line = r.readLine(); r.close();
            return line != null && line.trim().endsWith("1");
        } catch (Throwable t) { return false; }
    }

    private static Context ctx(Object thisObj) {
        try {
            if (appCtx != null) return appCtx;
            Context c = de.robv.android.xposed.AndroidAppHelper.currentApplication();
            if (c != null) { appCtx = c; return c; }
        } catch (Throwable ignored) { }
        return null;
    }

    private static void forward(Object thisObj, String text) {
        long now = System.currentTimeMillis();
        if (now - lastSent < 1500) { XposedBridge.log("DSHASAN: skip dup"); return; }
        try {
            Context c = ctx(thisObj);
            if (c == null) { XposedBridge.log("DSHASAN: no ctx"); return; }
            lastSent = now;
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_TEXT, text);
            i.setClassName(TARGET, ACTIVITY);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            c.startActivity(i);
            XposedBridge.log("DSHASAN: forwarded -> " + text);
            writeStatus(text);
        } catch (Throwable t) {
            XposedBridge.log("DSHASAN: forward failed " + t);
        }
    }

    private static void writeStatus(String lastText) {
        try {
            String role = "";
            try {
                Context c = ctx(null);
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
}
