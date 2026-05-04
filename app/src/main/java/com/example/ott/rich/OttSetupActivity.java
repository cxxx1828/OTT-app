package com.example.ott.rich;

import android.app.Activity;
import android.media.tv.TvInputInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.ott.R;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SetupActivity for the OTT TV input.
 *
 * Flow (launched by Live Channels before the input is used):
 *  1. User sees welcome screen with "Scan Channels" button.
 *  2. Click → background thread fetches JSON from Python server,
 *     writes channels + EPG programs into TvContract.
 *  3. Success → RESULT_OK, activity finishes.
 *  4. Error   → message shown, user can retry.
 *
 * Uses ExecutorService + Handler (no deprecated AsyncTask).
 */
public class OttSetupActivity extends Activity {

    private static final String TAG = "OttSetupActivity";

    // Single-thread executor for background work
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    // Handler that posts UI updates back to the main thread
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());

    private TextView    mStatusText;
    private ProgressBar mProgressBar;
    private Button      mScanButton;
    private Button      mCancelButton;

    private String mInputId;

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        mStatusText   = findViewById(R.id.tv_setup_status);
        mProgressBar  = findViewById(R.id.setup_progress);
        mScanButton   = findViewById(R.id.btn_scan);
        mCancelButton = findViewById(R.id.btn_cancel);

        // Live Channels passes the TV input ID via this extra
        mInputId = getIntent().getStringExtra(TvInputInfo.EXTRA_INPUT_ID);
        if (mInputId == null) {
            // Fallback for standalone testing
            mInputId = "com.example.ott/.rich.OttTvInputService";
            Log.w(TAG, "No EXTRA_INPUT_ID – using fallback");
        }

        mScanButton.setOnClickListener(v -> startScan());
        mCancelButton.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        setUiIdle(getString(R.string.setup_prompt));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mExecutor.shutdownNow();
    }

    // -----------------------------------------------------------------------
    // UI helpers (always called on main thread)
    // -----------------------------------------------------------------------

    private void setUiScanning(String msg) {
        mStatusText.setText(msg);
        mProgressBar.setVisibility(View.VISIBLE);
        mScanButton.setEnabled(false);
    }

    private void setUiIdle(String msg) {
        mStatusText.setText(msg);
        mProgressBar.setVisibility(View.GONE);
        mScanButton.setEnabled(true);
    }

    private void setUiError(String msg) {
        mStatusText.setText(msg);
        mProgressBar.setVisibility(View.GONE);
        mScanButton.setEnabled(true); // allow retry
    }

    // -----------------------------------------------------------------------
    // Channel scan (background)
    // -----------------------------------------------------------------------

    private void startScan() {
        setUiScanning(getString(R.string.setup_connecting));

        mExecutor.execute(() -> {
            try {
                // Step 1 – fetch channels from Python server
                publishProgress(getString(R.string.setup_fetching_channels));
                List<ChannelModel> channels = ChannelFetcher.fetchChannels();

                if (channels.isEmpty()) {
                    publishError(getString(R.string.setup_error_no_channels)
                            + "\n" + ChannelFetcher.SERVER_BASE);
                    return;
                }

                // Step 2 – write channels to TvContract
                publishProgress(getString(R.string.setup_writing_channels,
                        channels.size()));
                TvContractHelper.syncChannels(
                        OttSetupActivity.this, mInputId, channels);

                // Step 3 – fetch & write EPG programs per channel
                int total = channels.size();
                for (int i = 0; i < total; i++) {
                    ChannelModel ch = channels.get(i);
                    publishProgress(getString(R.string.setup_loading_epg,
                            ch.name, i + 1, total));

                    List<ProgramModel> programs =
                            ChannelFetcher.fetchPrograms(ch.id);

                    Uri channelUri = TvContractHelper.findChannelUri(
                            OttSetupActivity.this, mInputId, ch.number);
                    TvContractHelper.syncPrograms(
                            OttSetupActivity.this, channelUri, programs);
                }

                // Done
                String doneMsg = getString(R.string.setup_done, total);
                mMainHandler.post(() -> {
                    setUiIdle(doneMsg);
                    setResult(RESULT_OK);
                    finish();
                });

            } catch (Exception e) {
                Log.e(TAG, "Scan failed", e);
                publishError(getString(R.string.setup_error_generic,
                        e.getMessage()));
            }
        });
    }

    /** Post a progress message to the main thread (updates status TextView). */
    private void publishProgress(String msg) {
        mMainHandler.post(() -> setUiScanning(msg));
    }

    /** Post an error message to the main thread. */
    private void publishError(String msg) {
        mMainHandler.post(() -> setUiError("error " + msg));
    }
}
