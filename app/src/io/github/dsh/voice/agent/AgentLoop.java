package io.github.dsh.voice.agent;

import org.json.JSONArray;
import org.json.JSONObject;

public class AgentLoop {
    public interface Listener {
        void onStep(String note);
        void onAnswer(String text);
        void onError(String message);
    }

    private static final int MAX_STEPS = 6;

    private final LlmClient llm;
    private final ToolRegistry tools;

    public AgentLoop(Settings cfg, ToolRegistry tools) {
        this.llm = new LlmClient(cfg);
        this.tools = tools;
    }

    public void run(String userText, Listener l) {
        try {
            JSONArray messages = new JSONArray();
            messages.put(new JSONObject().put("role", "system").put("content", systemPrompt()));
            messages.put(new JSONObject().put("role", "user").put("content", userText));

            JSONArray defs = tools.definitions();

            for (int step = 0; step < MAX_STEPS; step++) {
                JSONObject resp = llm.chat(messages, defs);
                JSONObject msg = resp.getJSONArray("choices").getJSONObject(0).getJSONObject("message");

                JSONArray calls = msg.optJSONArray("tool_calls");
                String content = msg.optString("content", "");

                if (calls == null || calls.length() == 0) {
                    l.onAnswer(content == null || content.isEmpty() ? "（没有回答）" : content);
                    return;
                }

                messages.put(msg);
                for (int i = 0; i < calls.length(); i++) {
                    JSONObject call = calls.getJSONObject(i);
                    String id = call.optString("id", "call" + i);
                    JSONObject fn = call.getJSONObject("function");
                    String name = fn.optString("name");
                    String args = fn.optString("arguments", "{}");
                    l.onStep("调用 " + name);
                    String result = tools.execute(name, args);
                    messages.put(new JSONObject()
                            .put("role", "tool")
                            .put("tool_call_id", id)
                            .put("content", result));
                }
            }
            l.onError("超过最大步数（" + MAX_STEPS + "），任务未完成");
        } catch (Throwable e) {
            l.onError(String.valueOf(e.getMessage()));
        }
    }

    private String systemPrompt() {
        return "你是小米手机上的语音助理，代替小爱同学。"
             + "你能调用工具直接操作手机。回答要短，适合朗读，不要用markdown。"
             + "需要操作手机时先调工具，拿到结果再回答。"
             + "可用工具：" + tools.nameList() + "。";
    }
}
