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

/**
 * Sound effect manager for preloading and easy replaying sound effects.
 */
public class SoundManager {

    private SoundPool soundPool = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
    private HashMap<String, SoundEffect> sounds = new HashMap<String, SoundEffect>();

    public SoundManager(Context context) {
        Init(context);
    }

    /**
     * Initialized sample list and also initialized loading.
     */
    private void Init(Context context) {
        sounds.put("buzzer", new SoundEffect(R.raw.buzzer, -1));

        for (Map.Entry<String, SoundEffect> entry : sounds.entrySet()) {
            entry.getValue().sampleId = soundPool.load(context, R.raw.buzzer, 1);
        }

        soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, final int sampleId, int status) {
                FindBySampleId(sounds.values(), sampleId).loaded = true;
            }
        });
    }

    /**
     * Find sample by sound id.
     */
    public static SoundEffect FindBySampleId(Collection<SoundEffect> collection, int sampleId) {
        SoundEffect sound_effect = SearchArray.findFirst(collection, sampleId,
                new SearchArray.Comparator<SoundEffect, Integer>() {
                    public boolean isEqual(SoundEffect item, Integer value) {
                        return item.sampleId == value.intValue();
                    }
                }
        );
        return sound_effect;
    }

    public boolean IsLoaded(String soundName) {
        return sounds.containsKey(soundName) && sounds.get(soundName).sampleId >= 0;
    }

    /**
     * Play sample by name with repetition. Skip if not loaded.
     */
    public void Play(String soundName, int loop) {
        if (sounds.containsKey(soundName)) {
            SoundEffect soundEffect = sounds.get(soundName);
            if (soundEffect.loaded) {
                soundPool.play(soundEffect.sampleId, 1, 1, 1, loop - 1, 2);
            }
        }
    }

    /**
     * Sound effect structure. Bind resource id with sample id or sound pool player. Also track
     * whether the sample is already loaded.
     */
    class SoundEffect {
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
