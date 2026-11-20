package io.github.dsh.voice;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.service.voice.VoiceInteractionSession;
import android.service.voice.VoiceInteractionSessionService;

public class AssistantSessionService extends VoiceInteractionSessionService {
    @Override
    public VoiceInteractionSession onNewSession(Bundle args) {
        return new Session(this);
    }

    static class Session extends VoiceInteractionSession {
        Session(Context c) { super(c); }

        @Override
        public void onShow(Bundle args, int showFlags) {
            super.onShow(args, showFlags);
            try {
                Intent i = new Intent(getContext(), AssistantActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                getContext().startActivity(i);
                android.util.Log.i("DSHVOICE", "session -> activity");
            } catch (Throwable t) {
                android.util.Log.w("DSHVOICE", "session start failed: " + t);
            }
            hide();
        }
    }
}
