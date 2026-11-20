package io.github.dsh.voice;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

public class AssistantActivity extends Activity implements android.speech.RecognitionListener {
    private TextView status;
    private android.speech.SpeechRecognizer sr;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setGravity(Gravity.CENTER);
        ll.setBackgroundColor(0xF0121218);
        status = new TextView(this);
        status.setText("正在听…");
        status.setTextSize(24f);
        status.setGravity(Gravity.CENTER);
        status.setTextColor(0xFFE8E8F0);
        ll.addView(status, new LinearLayout.LayoutParams(-1, -2));
        TextView hint = new TextView(this);
        hint.setText("\n说完自动跳转到 DSH\n\n点屏幕任意处重试");
        hint.setTextSize(14f);
        hint.setGravity(Gravity.CENTER);
        hint.setTextColor(0xFF8A8A9A);
        ll.addView(hint, new LinearLayout.LayoutParams(-1, -2));
        ll.setOnClickListener(v -> startListening());
        setContentView(ll);
        startListening();
    }

    private void startListening() {
        try {
            if (sr != null) sr.destroy();
            sr = android.speech.SpeechRecognizer.createSpeechRecognizer(this);
            sr.setRecognitionListener(this);
            Intent i = new Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            i.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            i.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, "zh-CN");
            i.putExtra(android.speech.RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            status.setText("正在听…");
            sr.startListening(i);
            android.util.Log.i("DSHVOICE", "listening started");
        } catch (Throwable t) {
            status.setText("启动失败: " + t);
            android.util.Log.w("DSHVOICE", "start failed", t);
        }
    }

    @Override public void onReadyForSpeech(Bundle p) { status.setText("我在听，请说…"); }
    @Override public void onBeginningOfSpeech() {}
    @Override public void onRmsChanged(float v) {}
    @Override public void onBufferReceived(byte[] b) {}
    @Override public void onEndOfSpeech() { status.setText("识别中…"); }
    @Override public void onEvent(int t, Bundle b) {}

    @Override public void onPartialResults(Bundle b) {
        java.util.ArrayList<String> l = b == null ? null
                : b.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION);
        if (l != null && !l.isEmpty()) status.setText(l.get(0));
    }

    @Override public void onResults(Bundle b) {
        java.util.ArrayList<String> l = b == null ? null
                : b.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION);
        if (l != null && !l.isEmpty()) deliver(l.get(0));
        else { status.setText("没听清，点一下重试"); }
    }

    @Override public void onError(int e) {
        status.setText("识别失败 code=" + e + "\n点一下重试");
        android.util.Log.w("DSHVOICE", "error code=" + e);
    }

    private void deliver(String text) {
        android.util.Log.i("DSHVOICE", "result: " + text);
        try {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_TEXT, text);
            i.setClassName("io.github.ccch1mneyyy.dsh_app",
                    "io.github.ccch1mneyyy.dsh_app.share.ShareActivity");
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Throwable t) {
            android.util.Log.w("DSHVOICE", "deliver failed", t);
        }
        finish();
    }

    @Override protected void onDestroy() {
        if (sr != null) { try { sr.destroy(); } catch (Throwable ignored) {} }
        super.onDestroy();
    }
}
