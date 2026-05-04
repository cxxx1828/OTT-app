package com.example.ott.rich;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.media.tv.TvInputInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * JobService that refreshes channel EPG data in the background.
 * Scheduled by OttBootReceiver and OttSetupActivity.
 *
 * Job ID is defined in {@link #JOB_ID}.
 */
public class OttEpgJobService extends JobService {

    private static final String TAG   = "OttEpgJobService";
    public  static final int    JOB_ID = 1001;

    private final ExecutorService mExecutor  = Executors.newSingleThreadExecutor();
    private final Handler         mHandler   = new Handler(Looper.getMainLooper());

    @Override
    public boolean onStartJob(JobParameters params) {
        Log.i(TAG, "EPG sync started");

        // Read the stored input ID (saved during setup)
        String inputId = getSharedPreferences("ott_prefs", Context.MODE_PRIVATE)
                .getString("input_id", null);
        if (inputId == null) {
            Log.w(TAG, "No input_id stored – skipping EPG sync");
            return false; // job finished
        }

        mExecutor.execute(() -> {
            try {
                List<ChannelModel> channels = ChannelFetcher.fetchChannels();
                TvContractHelper.syncChannels(this, inputId, channels);

                for (ChannelModel ch : channels) {
                    List<ProgramModel> programs = ChannelFetcher.fetchPrograms(ch.id);
                    Uri channelUri = TvContractHelper.findChannelUri(
                            this, inputId, ch.number);
                    TvContractHelper.syncPrograms(this, channelUri, programs);
                }
                Log.i(TAG, "EPG sync complete – " + channels.size() + " channels");
            } catch (Exception e) {
                Log.e(TAG, "EPG sync error", e);
            } finally {
                mHandler.post(() -> jobFinished(params, false));
            }
        });

        return true; // work is ongoing (async)
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        mExecutor.shutdownNow();
        return true; // reschedule
    }
}
