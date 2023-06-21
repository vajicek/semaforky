package com.vajsoft.semaforky.utils;

/// Copyright (C) 2017, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;

import com.vajsoft.semaforky.R;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Sound effect manager for preloading and easy replaying sound effects.
 */
public class SoundManager {
    private static final Logger LOGGER = Logger.getLogger(SoundManager.class.getName());
    private static final int MAX_STREAMS = 1;
    private final SoundPool soundPool = new SoundPool(MAX_STREAMS, AudioManager.STREAM_MUSIC, 0);
    private final HashMap<String, SoundEffect> sounds = new HashMap<>();

    public SoundManager(Context context) {
        init(context);
    }

    /**
     * Find sample by sound id.
     */
    public static SoundEffect findBySampleId(Collection<SoundEffect> collection, int sampleId) {
        return SearchArray.findFirst(collection, sampleId,
                new SearchArray.Comparator<SoundEffect, Integer>() {
                    public boolean isEqual(SoundEffect item, Integer value) {
                        return item.sampleId == value;
                    }
                }
        );
    }

    /**
     * play sample by name with repetition. Skip if not loaded.
     */
    public void play(String soundName, int loop) {
        LOGGER.info("soundPool.play loop=" + loop);
        if (sounds.containsKey(soundName)) {
            SoundEffect soundEffect = sounds.get(soundName);
            if (Objects.requireNonNull(soundEffect).loaded) {
                soundPool.play(soundEffect.sampleId, 1, 1, 99, loop - 1, 1);
            }
        }
    }

    /**
     * Initialized sample list and also initialized loading.
     */
    private void init(Context context) {
        sounds.put("buzzer", new SoundEffect(R.raw.buzzer, -1));

        for (Map.Entry<String, SoundEffect> entry : sounds.entrySet()) {
            entry.getValue().sampleId = this.soundPool.load(context, entry.getValue().resourceId, 1);
        }

        this.soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, final int sampleId, int status) {
                findBySampleId(sounds.values(), sampleId).loaded = true;
            }
        });
    }

    /**
     * Sound effect structure. Bind resource id with sample id or sound pool player. Also track
     * whether the sample is already loaded.
     */
    static class SoundEffect {
        public int resourceId;
        public int sampleId;
        public boolean loaded;

        public SoundEffect(int resourceId_, int sampleId_) {
            resourceId = resourceId_;
            sampleId = sampleId_;
            loaded = false;
        }
    }
}
