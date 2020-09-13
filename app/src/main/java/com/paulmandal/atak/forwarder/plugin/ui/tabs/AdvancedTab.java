package com.paulmandal.atak.forwarder.plugin.ui.tabs;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.paulmandal.atak.forwarder.R;
import com.paulmandal.atak.forwarder.comm.CotMessageCache;
import com.paulmandal.atak.forwarder.comm.queue.CommandQueue;

import java.util.Locale;

public class AdvancedTab {
    private final Context mAtakContext;

    private final CommandQueue mCommandQueue;
    private final CotMessageCache mCotMessageCache;

    public AdvancedTab(Context atakContext, CommandQueue commandQueue, CotMessageCache cotMessageCache) {
        mAtakContext = atakContext;
        mCommandQueue = commandQueue;
        mCotMessageCache = cotMessageCache;
    }

    public void init(View templateView) {
        Button setDefaultCachePurgeTime = (Button) templateView.findViewById(R.id.button_set_default_purge_time_ms);
        Button setPliCachePurgeTime = (Button) templateView.findViewById(R.id.button_set_pli_purge_time_ms);
        Button clearMessageCache = (Button) templateView.findViewById(R.id.button_clear_message_cache);
        Button clearMessageQueue = (Button) templateView.findViewById(R.id.button_clear_message_queue);
        Button clearData = (Button) templateView.findViewById(R.id.button_clear_data);

        EditText cachePurgeTimeMins = (EditText) templateView.findViewById(R.id.edittext_default_purge_time_mins);
        EditText pliPurgeTimeS = (EditText) templateView.findViewById(R.id.edittext_pli_purge_time_s);

        cachePurgeTimeMins.setText(String.format(Locale.getDefault(), "%d", mCotMessageCache.getDefaultCachePurgeTimeMs() / 60000));
        pliPurgeTimeS.setText(String.format(Locale.getDefault(), "%d", mCotMessageCache.getPliCachePurgeTimeMs() / 1000));

        clearData.setOnClickListener((View v) -> {
            Toast.makeText(mAtakContext, "Clearing all plugin data", Toast.LENGTH_LONG).show();
            mCotMessageCache.clearData();
            mCommandQueue.clearData();
        });

        clearMessageCache.setOnClickListener((View v) -> {
            Toast.makeText(mAtakContext, "Clearing duplicate message cache", Toast.LENGTH_SHORT).show();
            mCotMessageCache.clearData();
        });

        clearMessageQueue.setOnClickListener((View v) -> {
            Toast.makeText(mAtakContext, "Clearing outgoing message queue", Toast.LENGTH_SHORT).show();
            mCommandQueue.clearData();
        });

        setDefaultCachePurgeTime.setOnClickListener((View v) -> {
            String cachePurgeTimeMinsStr = cachePurgeTimeMins.getText().toString();
            if (cachePurgeTimeMinsStr.equals("")) {
                return;
            }
            Toast.makeText(mAtakContext, "Set duplicate message cache TTL", Toast.LENGTH_SHORT).show();
            int cachePurgeTimeMs = Integer.parseInt(cachePurgeTimeMinsStr) * 60000;
            mCotMessageCache.setDefaultCachePurgeTimeMs(cachePurgeTimeMs);
        });

        setPliCachePurgeTime.setOnClickListener((View v) -> {
            String pliPurgeTimeSStr = pliPurgeTimeS.getText().toString();
            if (pliPurgeTimeSStr.equals("")) {
                return;
            }
            Toast.makeText(mAtakContext, "Set PLI message cache TTL", Toast.LENGTH_SHORT).show();
            int pliPurgeTimeSInt = Integer.parseInt(pliPurgeTimeSStr);
            mCotMessageCache.setPliCachePurgeTimeMs(pliPurgeTimeSInt * 1000);
        });
    }
}
