package com.paulmandal.atak.forwarder.plugin;

import android.content.Context;

import com.atakmap.android.maps.MapView;

public interface Destroyable {
    void onDestroy(Context context, MapView mapView);
}
