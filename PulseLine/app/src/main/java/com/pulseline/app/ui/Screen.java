package com.pulseline.app.ui;

import android.view.View;

/** A page shown inside MainActivity. */
public interface Screen {
    View view();

    /** Called every second with fresh readings: update numbers and charts in place. */
    void onTick();

    /** Called when machines, alerts or settings changed: rebuild what is shown. */
    void onData();
}
