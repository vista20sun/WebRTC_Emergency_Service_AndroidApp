package io.agora.iitEmergencyCall.model;

public class EngineConfig {
    public int mVideoProfile;

    public int mUid;

    public String mChannel;

    public void reset() {
        mChannel = null;
    }

    EngineConfig() {
    }
}
