package io.github.dsh.voice.tools;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;

import io.github.dsh.voice.agent.Tool;

public class ShellTool implements Tool {
    private static final int MAX_OUT = 4000;

    public String name() { return "shell"; }
    public String description() { return "以 root(su) 身份执行一条 Android sh 命令并返回输出。只读查询优先用别的工具；破坏性命令不要用。"; }
    public int risk() { return DANGER; }

    public JSONObject parameters() {
        try {
            JSONObject cmd = new JSONObject().put("type", "string").put("description", "要执行的 sh 命令");
            return new JSONObject().put("type", "object")
                    .put("properties", new JSONObject().put("cmd", cmd))
                    .put("required", new org.json.JSONArray().put("cmd"));
        } catch (Exception e) { return new JSONObject(); }
    }

    public JSONObject execute(JSONObject a) throws Exception {
        String cmd = a.optString("cmd", "");
        if (cmd.isEmpty()) return new JSONObject().put("ok", false).put("error", "cmd 为空");
        if (cmd.length() > 1500) return new JSONObject().put("ok", false).put("error", "命令过长");

        ProcessBuilder pb = new ProcessBuilder("su", "-c", cmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), "UTF-8"))) {
            String line;
            while ((line = r.readLine()) != null) {
                sb.append(line).append('\n');
                if (sb.length() > MAX_OUT * 2) break;
            }
        }
        boolean done = p.waitFor(20, TimeUnit.SECONDS);
        if (!done) { p.destroy(); return new JSONObject().put("ok", false).put("error", "超时 20s"); }
        String out = sb.toString();
        if (out.length() > MAX_OUT) out = out.substring(0, MAX_OUT) + "\n...(截断)";
        return new JSONObject().put("exit", p.exitValue()).put("output", out);
    }
}
