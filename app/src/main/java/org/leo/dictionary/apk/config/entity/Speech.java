package org.leo.dictionary.apk.config.entity;

import org.leo.dictionary.config.entity.ConfigParent;

public class Speech extends ConfigParent {

    private final float speed = 1.0f;
    private final float pitch = 1.0f;

    public float getSpeed() {
        return getOrDefault("speed", speed);
    }

    public float getPitch() {
        return getOrDefault("pitch", pitch);
    }
}
