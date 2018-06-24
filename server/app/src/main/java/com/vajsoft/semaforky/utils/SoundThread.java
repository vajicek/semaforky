package com.vajsoft.semaforky.utils;

/// Copyright (C) 2018, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

import android.media.SoundPool;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Thread for playing sounds. Workaround for delayed playback observed on bluetooth connected
 * speakers.
 */
class SoundThread extends Thread {

    private SoundPool soundPool;
    private BlockingQueue<SoundItem> sounds = new LinkedBlockingQueue<SoundItem>();
    private boolean stop = false;
    /**
     * Construct Thread from sound pool.
     */
    public SoundThread(SoundPool soundPool) {
        this.soundPool = soundPool;
    }

    /**
     * Queue given sample to play in given loops.
     */
    public void queueToPlay(int sampleId, int loop) {
        this.sounds.add(new SoundItem(sampleId, 1, loop));
    }

    /**
     * Issue thread loop exit command.
     */
    public void dispose() {
        this.sounds.add(new SoundItem());
    }

    @Override
    public void run() {
        try {
            SoundItem item;
            while (!this.stop) {
                item = this.sounds.take();
                if (item.stop) {
                    this.stop = true;
                    break;
                }
                this.soundPool.play(item.soundID, item.volume, item.volume, 0, item.loop, 1);
            }
        } catch (InterruptedException e) {
        }
    }

    /**
     * Internal sound item command reference. It allows to issue thread exit command.
     */
    class SoundItem {
        public int soundID;
        public float volume;
        public int loop;
        public boolean stop = false;

        public SoundItem(int soundID, float volume, int loop) {
            this.soundID = soundID;
            this.volume = volume;
            this.loop = loop;
        }

        public SoundItem() {
            this.stop = true;
        }
    }
}
