package com.example.ott.rich;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.concurrent.TimeUnit;

/**
 * BroadcastReceiver that reschedules the EPG sync job after device reboot.
 * JobScheduler does not persist across reboots by default on older Android versions.
 */
public class OttBootReceiver extends BroadcastReceiver {

    private static final String TAG = "OttBootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;

        Log.i(TAG, "Boot completed – rescheduling EPG sync");

        JobScheduler scheduler =
                (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (scheduler == null) return;

        // Only schedule if setup has been run (input_id is stored)
        String inputId = context.getSharedPreferences("ott_prefs", Context.MODE_PRIVATE)
                .getString("input_id", null);
        if (inputId == null) {
            Log.d(TAG, "No input_id – skipping reschedule");
            return;
        }

        JobInfo job = new JobInfo.Builder(
                OttEpgJobService.JOB_ID,
                new ComponentName(context, OttEpgJobService.class))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(TimeUnit.HOURS.toMillis(12))  // refresh every 12 h
                .setPersisted(true)
                .build();

        scheduler.schedule(job);
        Log.i(TAG, "EPG job scheduled (periodic 12h)");
    }
}
