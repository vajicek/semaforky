package com.vajsoft.semaforky.utils;

/// Copyright (C) 2017, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;

import com.vajsoft.semaforky.R;

import java.util.HashMap;
import java.util.Map;

public class SoundManager {

    class SoundEffect {
        public int resourceId;
        public int soundId;
        public boolean loaded;
        public SoundEffect(int resourceId_, int soundId_) {
            resourceId = resourceId_;
            soundId = soundId_;
            loaded = false;
        }
    }

    public static SoundEffect findBySoundId(Iterable<SoundEffect> collection, int soundId) {
        for(SoundEffect s: collection){
            if(s.soundId == soundId) {
                return s;
            }
        }
        return null;
    }

    private SoundPool soundPool = new SoundPool(5, AudioManager.STREAM_MUSIC,0);
    private HashMap<String, SoundEffect> sounds = new HashMap<String, SoundEffect>();
    public boolean IsLoaded(String soundName)  {
        return sounds.containsKey(soundName) && sounds.get(soundName).soundId >= 0;
    }

    private void Init(Context context) {
        sounds.put("buzzer", new SoundEffect(R.raw.buzzer, -1));

        for (Map.Entry<String, SoundEffect> entry : sounds.entrySet()) {
            entry.getValue().soundId = soundPool.load(context, R.raw.buzzer, 1);
        }


        soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, final int sampleId, int status) {
                findBySoundId(sounds.values(), sampleId).loaded = true;
            }
        });
    }

    public void Play(String soundName, int loop) {
        if (sounds.containsKey(soundName)) {
            SoundEffect soundEffect = sounds.get(soundName);
            if (soundEffect.loaded) {
                soundPool.play(soundEffect.soundId, 1, 1, 1, loop, 2);
            }
        }
    }

    public SoundManager(Context context) {
        Init(context);
    }
}
