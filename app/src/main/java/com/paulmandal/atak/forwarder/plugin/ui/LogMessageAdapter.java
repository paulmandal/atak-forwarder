package com.paulmandal.atak.forwarder.plugin.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.paulmandal.atak.forwarder.R;
import com.paulmandal.atak.forwarder.plugin.ui.viewmodels.LoggingViewModel;

import java.util.List;

public class LogMessageAdapter extends RecyclerView.Adapter<LogMessageAdapter.ViewHolder> {
    private final Context mPluginContext;
    private final List<LoggingViewModel.LogMessage> mLogMessages;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView logMessageTextView;

        public ViewHolder(View view) {
            super(view);

            logMessageTextView = view.findViewById(R.id.textview_log_message);
        }

        public TextView getLogMessageTextView() {
            return logMessageTextView;
        }
    }

    public LogMessageAdapter(Context pluginContext, List<LoggingViewModel.LogMessage> logMessages) {
        mPluginContext = pluginContext;
        mLogMessages = logMessages;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.log_message_recyclerview_item, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {
        LoggingViewModel.LogMessage logMessage = mLogMessages.get(position);
        viewHolder.getLogMessageTextView().setText(String.format("%s: %s", logMessage.tag, logMessage.message));
    }

    @Override
    public int getItemCount() {
        return mLogMessages.size();
    }
}
