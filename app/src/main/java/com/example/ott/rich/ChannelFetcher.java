package com.example.ott.rich;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Fetches channel/program JSON from the Python FastAPI server and
 * parses it into model objects.
 *
 * All methods are blocking – always call from a background thread
 * (e.g. via ExecutorService, see OttSetupActivity).
 */
public class ChannelFetcher {

    private static final String TAG = "ChannelFetcher";

    /**
     * Base URL of the Python server.
     *  - Emulator accessing host machine : http://10.0.2.2:8000
     *  - Real device on same Wi-Fi       : http://<host-LAN-IP>:8000
     */
    public static final String SERVER_BASE = "http://10.0.2.2:8000";

    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS    = 15_000;

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /** Fetch and parse /channels → list of ChannelModel. */
    public static List<ChannelModel> fetchChannels() {
        String json = httpGet(SERVER_BASE + "/channels");
        return parseChannels(json);
    }

    /** Fetch and parse /programs/{channelId} → list of ProgramModel. */
    public static List<ProgramModel> fetchPrograms(String channelId) {
        String json = httpGet(SERVER_BASE + "/programs/" + channelId);
        return parsePrograms(json);
    }

    // -----------------------------------------------------------------------
    // HTTP
    // -----------------------------------------------------------------------

    private static String httpGet(String urlString) {
        HttpURLConnection conn = null;
        StringBuilder sb = new StringBuilder();
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setRequestProperty("Accept", "application/json");

            int status = conn.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "HTTP " + status + " for " + urlString);
                return "";
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Network error: " + urlString, e);
        } finally {
            if (conn != null) conn.disconnect();
        }
        return sb.toString();
    }

    // -----------------------------------------------------------------------
    // Parsers
    // -----------------------------------------------------------------------

    static List<ChannelModel> parseChannels(String json) {
        List<ChannelModel> result = new ArrayList<>();
        if (json == null || json.isEmpty()) return result;
        try {
            JSONObject root = new JSONObject(json);
            JSONArray  arr  = root.getJSONArray("channels");
            for (int i = 0; i < arr.length(); i++) {
                result.add(ChannelModel.fromJson(arr.getJSONObject(i)));
            }
        } catch (Exception e) {
            Log.e(TAG, "parseChannels", e);
        }
        return result;
    }

    static List<ProgramModel> parsePrograms(String json) {
        List<ProgramModel> result = new ArrayList<>();
        if (json == null || json.isEmpty()) return result;
        try {
            JSONObject root = new JSONObject(json);
            JSONArray  arr  = root.getJSONArray("programs");
            for (int i = 0; i < arr.length(); i++) {
                result.add(ProgramModel.fromJson(arr.getJSONObject(i)));
            }
        } catch (Exception e) {
            Log.e(TAG, "parsePrograms", e);
        }
        return result;
    }
}
