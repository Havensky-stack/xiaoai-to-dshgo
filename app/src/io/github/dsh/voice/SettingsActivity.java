package io.github.dsh.voice;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import io.github.dsh.voice.agent.Settings;

public class SettingsActivity extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Settings cfg = new Settings(this);

        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setPadding(48, 96, 48, 48);
        ll.setBackgroundColor(0xFF121218);

        TextView title = new TextView(this);
        title.setText("模型配置");
        title.setTextSize(22f);
        title.setTextColor(0xFFEDEDF5);
        title.setPadding(0, 0, 0, 32);
        ll.addView(title);

        final EditText base = field(ll, "接口地址（OpenAI 兼容）", cfg.baseUrl());
        final EditText key = field(ll, "API Key", cfg.apiKey());
        final EditText model = field(ll, "模型名", cfg.model());

        Button save = new Button(this);
        save.setText("保存");
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = 40;
        ll.addView(save, lp);

        TextView hint = new TextView(this);
        hint.setText("\n支持 DeepSeek / 硅基流动 / 通义 / 本地 ollama 等\n填写后返回助理界面即可使用");
        hint.setTextSize(13f);
        hint.setTextColor(0xFF8A8A9A);
        hint.setGravity(Gravity.START);
        ll.addView(hint);

        save.setOnClickListener(v -> {
            cfg.save(base.getText().toString(), key.getText().toString(), model.getText().toString());
            finish();
        });

        setContentView(ll);
    }

    private EditText field(LinearLayout parent, String label, String value) {
        TextView l = new TextView(this);
        l.setText(label);
        l.setTextSize(13f);
        l.setTextColor(0xFF9A9AAA);
        l.setPadding(0, 24, 0, 8);
        parent.addView(l);

        EditText e = new EditText(this);
        e.setText(value);
        e.setTextSize(15f);
        e.setSingleLine(true);
        e.setTextColor(0xFFEDEDF5);
        parent.addView(e);
        return e;
    }
}
