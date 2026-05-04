package com.example.ott.rich;

import android.content.Context;
import android.database.Cursor;
import android.media.tv.TvContract;
import android.media.tv.TvInputManager;
import android.media.tv.TvInputService;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.Nullable;

/**
 * TvInputService that tunes HLS channels stored in TvContract.
 *
 * When the user selects a channel in Live Channels, onTune() is called with
 * the channel Uri. We read the HLS URL from
 * {@link TvContract.Channels#COLUMN_INTERNAL_PROVIDER_DATA} and hand it to
 * ExoPlayer (wire-up left as a TODO so this compiles without the ExoPlayer
 * dependency being mandatory in this file).
 */
public class OttTvInputService extends TvInputService {

    private static final String TAG = "OttTvInputService";

    @Override
    public Session onCreateSession(String inputId) {
        Log.d(TAG, "onCreateSession: " + inputId);
        OttSession session = new OttSession(this, inputId);
        session.setOverlayViewEnabled(false);
        return session;
    }

    // =========================================================================
    // Session
    // =========================================================================
    class OttSession extends TvInputService.Session {

        private final String mInputId;
        private final Handler mHandler = new Handler(Looper.getMainLooper());

        // TODO: declare ExoPlayer / MediaPlayer field here
        // private ExoPlayer mPlayer;

        OttSession(Context context, String inputId) {
            super(context);
            mInputId = inputId;
        }

        @Override
        public void onRelease() {
            Log.d(TAG, "onRelease");
            // TODO: mPlayer.release();
        }

        @Override
        public boolean onSetSurface(@Nullable Surface surface) {
            Log.d(TAG, "onSetSurface: " + surface);
            // TODO: mPlayer.setVideoSurface(surface);
            return true;
        }

        @Override
        public void onSetStreamVolume(float volume) {
            // TODO: mPlayer.setVolume(volume);
        }

        @Override
        public boolean onTune(Uri channelUri) {
            Log.d(TAG, "onTune: " + channelUri);
            notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_TUNING);

            // Read HLS URL from TvContract
            String hlsUrl = readHlsUrl(channelUri);
            if (hlsUrl == null || hlsUrl.isEmpty()) {
                Log.e(TAG, "No HLS URL for channel: " + channelUri);
                notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_UNKNOWN);
                return false;
            }

            Log.i(TAG, "Tuning to HLS: " + hlsUrl);

            // TODO: initialise ExoPlayer with hlsUrl, e.g.:
            //   MediaItem item = MediaItem.fromUri(hlsUrl);
            //   mPlayer.setMediaItem(item);
            //   mPlayer.prepare();
            //   mPlayer.play();
            //
            // Then in the playback ready callback:
            //   notifyVideoAvailable();

            // Stub – mark video available immediately for testing
            mHandler.postDelayed(this::notifyVideoAvailable, 500);
            return true;
        }

        @Override
        public void onSetCaptionEnabled(boolean enabled) {
            // Subtitle / caption handling – extend as needed
        }

        // ---------------------------------------------------------------
        // Helper: read HLS URL from TvContract internal provider data
        // ---------------------------------------------------------------
        @Nullable
        private String readHlsUrl(Uri channelUri) {
            try (Cursor c = getContentResolver().query(
                    channelUri,
                    new String[]{TvContract.Channels.COLUMN_INTERNAL_PROVIDER_DATA},
                    null, null, null)) {
                if (c != null && c.moveToFirst()) {
                    byte[] data = c.getBlob(0);
                    if (data != null) return new String(data);
                }
            } catch (Exception e) {
                Log.e(TAG, "readHlsUrl error", e);
            }
            return null;
        }
    }
}
