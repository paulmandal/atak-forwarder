package com.paulmandal.atak.forwarder.plugin;

import android.content.Context;

import androidx.annotation.CallSuper;

import com.atakmap.android.maps.MapView;

public interface Destroyable {
    @CallSuper
    void onDestroy(Context context, MapView mapView);
}
