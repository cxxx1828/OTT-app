package com.example.ott.rich;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.media.tv.TvContract;
import android.net.Uri;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Maps ChannelModel / ProgramModel objects into Android's TvContract database.
 *
 * Reference: https://developer.android.com/reference/android/media/tv/TvContract
 *
 * All methods are blocking – call from a background thread.
 */
public class TvContractHelper {

    private static final String TAG = "TvContractHelper";
    private static final String TS_FORMAT = "yyyyMMddHHmmss Z";

    // -----------------------------------------------------------------------
    // Genre mapping  (server string → TvContract canonical genre)
    // -----------------------------------------------------------------------
    private static String mapGenre(String g) {
        if (g == null) return "";
        switch (g.toUpperCase(Locale.ROOT)) {
            case "MOVIES":           return TvContract.Programs.Genres.MOVIES;
            case "SPORTS":           return TvContract.Programs.Genres.SPORTS;
            case "NEWS":             return TvContract.Programs.Genres.NEWS;
            case "FAMILY_KIDS":      return TvContract.Programs.Genres.FAMILY_KIDS;
            case "TECH_SCIENCE":     return TvContract.Programs.Genres.TECH_SCIENCE;
            case "EDUCATION":        return TvContract.Programs.Genres.EDUCATION;
            case "LIFESTYLE":        return TvContract.Programs.Genres.LIFE_STYLE;
            case "ANIMAL_WILDLIFE":  return TvContract.Programs.Genres.ANIMAL_WILDLIFE;
            case "COMEDY":           return TvContract.Programs.Genres.COMEDY;
            case "DRAMA":            return TvContract.Programs.Genres.DRAMA;
            case "ENTERTAINMENT":    return TvContract.Programs.Genres.ENTERTAINMENT;
            case "MUSIC":            return TvContract.Programs.Genres.MUSIC;
            case "PREMIER":          return TvContract.Programs.Genres.PREMIER;
            case "TRAVEL":           return TvContract.Programs.Genres.TRAVEL;
            default:                 return "";
        }
    }

    // -----------------------------------------------------------------------
    // Channels
    // -----------------------------------------------------------------------

    /**
     * Delete all existing channels for this input and insert fresh ones.
     *
     * @param context  application context
     * @param inputId  TvInputInfo.getId() – identifies our TV input
     * @param channels list fetched from server
     */
    public static void syncChannels(Context context, String inputId,
                                    List<ChannelModel> channels) {
        // Remove old channels first (clean re-scan strategy)
        Uri uri = TvContract.buildChannelsUriForInput(inputId);
        context.getContentResolver().delete(uri, null, null);

        for (int i = 0; i < channels.size(); i++) {
            insertChannel(context, inputId, channels.get(i), i + 1);
        }
        Log.i(TAG, "syncChannels: " + channels.size() + " channels written");
    }

    private static Uri insertChannel(Context context, String inputId,
                                     ChannelModel ch, int sortOrder) {
        ContentValues v = new ContentValues();

        // ---- Required TvContract.Channels columns ----
        v.put(TvContract.Channels.COLUMN_INPUT_ID,            inputId);
        v.put(TvContract.Channels.COLUMN_TYPE,
                TvContract.Channels.TYPE_OTHER); // TYPE_HLS is not a standard constant
        v.put(TvContract.Channels.COLUMN_SERVICE_TYPE,
                TvContract.Channels.SERVICE_TYPE_AUDIO_VIDEO);
        v.put(TvContract.Channels.COLUMN_DISPLAY_NUMBER,      ch.number);
        v.put(TvContract.Channels.COLUMN_DISPLAY_NAME,        ch.name);
        v.put(TvContract.Channels.COLUMN_DESCRIPTION,         ch.description);

        // Store the raw HLS URL in internal provider data so OttTvInputService
        // can read it when the user tunes to this channel.
        v.put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_DATA,
                ch.url.getBytes());

        // ---- Optional ----
        v.put(TvContract.Channels.COLUMN_BROWSABLE,            1);
        v.put(TvContract.Channels.COLUMN_SEARCHABLE,           1);
        v.put(TvContract.Channels.COLUMN_ORIGINAL_NETWORK_ID,  sortOrder);

        // Store genre tag in FLAG1 (readable by OttTvInputService for filtering)
        v.put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG1, ch.genre);

        Uri inserted = context.getContentResolver()
                .insert(TvContract.Channels.CONTENT_URI, v);
        Log.d(TAG, "  channel inserted: " + ch.name + " → " + inserted);
        return inserted;
    }

    // -----------------------------------------------------------------------
    // Programs (EPG)
    // -----------------------------------------------------------------------

    /**
     * Delete all programs for the given channel URI and insert new ones.
     *
     * @param channelUri  Uri returned by insertChannel / findChannelUri
     * @param programs    list from /programs/{id}
     */
    public static void syncPrograms(Context context, Uri channelUri,
                                    List<ProgramModel> programs) {
        if (channelUri == null) {
            Log.w(TAG, "syncPrograms: null channelUri, skipping");
            return;
        }
        long channelId = Long.parseLong(channelUri.getLastPathSegment());
        Uri programsUri = TvContract.buildProgramsUriForChannel(channelId);
        context.getContentResolver().delete(programsUri, null, null);

        for (ProgramModel p : programs) {
            insertProgram(context, channelId, p);
        }
        Log.i(TAG, "syncPrograms: " + programs.size()
                + " programs written for channel " + channelId);
    }

    private static void insertProgram(Context context, long channelId,
                                      ProgramModel p) {
        long startMs = parseTimestamp(p.startUtc);
        long stopMs  = parseTimestamp(p.stopUtc);
        if (startMs < 0 || stopMs < 0) {
            Log.w(TAG, "  bad timestamps for: " + p.title);
            return;
        }

        ContentValues v = new ContentValues();
        v.put(TvContract.Programs.COLUMN_CHANNEL_ID,            channelId);
        v.put(TvContract.Programs.COLUMN_TITLE,                 p.title);
        v.put(TvContract.Programs.COLUMN_SHORT_DESCRIPTION,     p.description);
        v.put(TvContract.Programs.COLUMN_START_TIME_UTC_MILLIS, startMs);
        v.put(TvContract.Programs.COLUMN_END_TIME_UTC_MILLIS,   stopMs);
        
        String encodedGenre = mapGenre(p.genre);
        if (!encodedGenre.isEmpty()) {
            v.put(TvContract.Programs.COLUMN_CANONICAL_GENRE,
                    TvContract.Programs.Genres.encode(encodedGenre));
        }

        if (p.icon != null && !p.icon.isEmpty()) {
            v.put(TvContract.Programs.COLUMN_POSTER_ART_URI, p.icon);
        }

        context.getContentResolver().insert(TvContract.Programs.CONTENT_URI, v);
    }

    // -----------------------------------------------------------------------
    // Lookup
    // -----------------------------------------------------------------------

    /**
     * Find the content Uri of a channel by its display number.
     *
     * @return channel Uri, or {@code null} if not found
     */
    public static Uri findChannelUri(Context context, String inputId,
                                     String displayNumber) {
        Uri channelsUri = TvContract.buildChannelsUriForInput(inputId);
        try (Cursor c = context.getContentResolver().query(
                channelsUri,
                new String[]{
                        TvContract.Channels._ID,
                        TvContract.Channels.COLUMN_DISPLAY_NUMBER
                },
                null, null, null)) {
            if (c == null) return null;
            while (c.moveToNext()) {
                if (displayNumber.equals(c.getString(1))) {
                    return TvContract.buildChannelUri(c.getLong(0));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "findChannelUri", e);
        }
        return null;
    }

    // -----------------------------------------------------------------------
    // Timestamp
    // -----------------------------------------------------------------------

    /** Parse "20240101060000 +0000" → epoch ms, -1 on error. */
    static long parseTimestamp(String raw) {
        if (raw == null || raw.isEmpty()) return -1;
        SimpleDateFormat sdf = new SimpleDateFormat(TS_FORMAT, Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            Date d = sdf.parse(raw);
            return d != null ? d.getTime() : -1;
        } catch (ParseException e) {
            Log.w(TAG, "parseTimestamp failed: " + raw);
            return -1;
        }
    }
}
