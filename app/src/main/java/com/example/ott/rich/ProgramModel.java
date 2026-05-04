package com.example.ott.rich;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * POJO for a single EPG program from /programs/{channel_id} or /epg.
 *
 * JSON shape:
 * {
 *   "title": "Morning News", "description": "...",
 *   "start_utc": "20240101060000 +0000",
 *   "stop_utc":  "20240101070000 +0000",
 *   "genre": "NEWS", "icon": "", "channel_id": "ch3"
 * }
 */
public class ProgramModel {

    public final String title;
    public final String description;
    public final String startUtc;   // XMLTV format: "yyyyMMddHHmmss +0000"
    public final String stopUtc;
    public final String genre;
    public final String icon;
    public final String channelId;

    private ProgramModel(Builder b) {
        title       = b.title;
        description = b.description;
        startUtc    = b.startUtc;
        stopUtc     = b.stopUtc;
        genre       = b.genre;
        icon        = b.icon;
        channelId   = b.channelId;
    }

    public static ProgramModel fromJson(JSONObject obj) throws JSONException {
        return new Builder()
                .title(obj.optString("title", ""))
                .description(obj.optString("description", ""))
                .startUtc(obj.optString("start_utc", ""))
                .stopUtc(obj.optString("stop_utc", ""))
                .genre(obj.optString("genre", "UNDEFINED"))
                .icon(obj.optString("icon", ""))
                .channelId(obj.optString("channel_id", ""))
                .build();
    }

    public static class Builder {
        private String title = "", description = "", startUtc = "", stopUtc = "";
        private String genre = "UNDEFINED", icon = "", channelId = "";

        public Builder title(String v)       { title = v;       return this; }
        public Builder description(String v) { description = v; return this; }
        public Builder startUtc(String v)    { startUtc = v;    return this; }
        public Builder stopUtc(String v)     { stopUtc = v;     return this; }
        public Builder genre(String v)       { genre = v;       return this; }
        public Builder icon(String v)        { icon = v;        return this; }
        public Builder channelId(String v)   { channelId = v;   return this; }
        public ProgramModel build()          { return new ProgramModel(this); }
    }
}
