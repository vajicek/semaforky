package com.vajsoft.semaforky.activities;

/// Copyright (C) 2019, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class GuiEventReceiver {

    interface GuiEventSubscriber {
        void updateRoundClocks(final Date roundStart);

        void updateSetClocks(final int remainingSeconds);

        void updateGui();
    }

    Set<GuiEventSubscriber> subscribers = new HashSet<>();

    public void subscribe(GuiEventSubscriber subscriber) {
        subscribers.add(subscriber);
    }

    public void unsubscribe(GuiEventSubscriber subscriber) {
        subscribers.remove(subscriber);
    }

    public void updateRoundClocks(final Date roundStart) {
        for (GuiEventSubscriber subscriber : subscribers) {
            subscriber.updateRoundClocks(roundStart);
        }
    }

    public void updateSetClocks(final int remainingSeconds) {
        for (GuiEventSubscriber subscriber : subscribers) {
            subscriber.updateSetClocks(remainingSeconds);
        }
    }

    public void updateGui() {
        for (GuiEventSubscriber subscriber : subscribers) {
            subscriber.updateGui();
        }
    }
}