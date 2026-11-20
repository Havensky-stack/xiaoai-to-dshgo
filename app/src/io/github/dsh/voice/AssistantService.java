package io.github.dsh.voice;

import android.service.voice.VoiceInteractionService;

public class AssistantService extends VoiceInteractionService {
    @Override
    public void onReady() {
        super.onReady();
        android.util.Log.i("DSHVOICE", "assistant ready");
    }
}
