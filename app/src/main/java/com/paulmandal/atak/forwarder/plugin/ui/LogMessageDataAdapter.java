package com.paulmandal.atak.forwarder.plugin.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.paulmandal.atak.forwarder.R;
import com.paulmandal.atak.forwarder.helpers.Logger;
import com.paulmandal.atak.forwarder.plugin.ui.viewmodels.LoggingViewModel;

import java.util.List;

public class LogMessageDataAdapter extends ArrayAdapter<LoggingViewModel.LogMessage> {
    private final Context mPluginContext;
    private final List<LoggingViewModel.LogMessage> mLogMessages;

    public LogMessageDataAdapter(Context pluginContext, List<LoggingViewModel.LogMessage> logMessages) {
        super(pluginContext, R.layout.log_message_listview_item, logMessages);
        mPluginContext = pluginContext;
        mLogMessages = logMessages;
    }

    public static class ViewHolder {
        public TextView logMessage;
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    @NonNull
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        View view;
        if (convertView == null) {
            LayoutInflater inflator = (LayoutInflater) mPluginContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflator.inflate(R.layout.log_message_listview_item, parent, false);
            final ViewHolder viewHolder = new ViewHolder();
            viewHolder.logMessage = view.findViewById(R.id.textview_log_message);
            view.setTag(viewHolder);
        } else {
            view = convertView;
        }
        LoggingViewModel.LogMessage logMessage = mLogMessages.get(position);

        ViewHolder holder = (ViewHolder) view.getTag();
        holder.logMessage.setText(String.format("%s/%s: %s", logMessage.tag, logLevelToStr(logMessage.level), logMessage.message));
        return view;
    }

    private String logLevelToStr(int level) {
        switch (level) {
            case Logger.LOG_LEVEL_VERBOSE:
                return "V";
            case Logger.LOG_LEVEL_DEBUG:
                return "D";
            case Logger.LOG_LEVEL_INFO:
                return "I";
            case Logger.LOG_LEVEL_WARN:
                return "W";
            case Logger.LOG_LEVEL_ERROR:
                return "E";
            default:
                return "?";
        }
    }
}
