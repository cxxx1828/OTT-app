package com.example.ott.rich;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * POJO representing one channel entry from the Python server /channels response.
 *
 * JSON shape:
 * {
 *   "id": "ch1", "number": "1", "name": "...", "url": "...",
 *   "type": "HLS", "logo": "", "genre": "TECH_SCIENCE",
 *   "group": "Test", "description": "..."
 * }
 */
public class ChannelModel {

    public final String id;
    public final String number;
    public final String name;
    public final String url;
    public final String type;
    public final String logo;
    public final String genre;
    public final String group;
    public final String description;

    private ChannelModel(Builder b) {
        id          = b.id;
        number      = b.number;
        name        = b.name;
        url         = b.url;
        type        = b.type;
        logo        = b.logo;
        genre       = b.genre;
        group       = b.group;
        description = b.description;
    }

    public static ChannelModel fromJson(JSONObject obj) throws JSONException {
        return new Builder()
                .id(obj.optString("id", ""))
                .number(obj.optString("number", "0"))
                .name(obj.optString("name", "Unknown"))
                .url(obj.optString("url", ""))
                .type(obj.optString("type", "HLS"))
                .logo(obj.optString("logo", ""))
                .genre(obj.optString("genre", "UNDEFINED"))
                .group(obj.optString("group", ""))
                .description(obj.optString("description", ""))
                .build();
    }

    public static class Builder {
        private String id = "", number = "0", name = "", url = "", type = "HLS";
        private String logo = "", genre = "UNDEFINED", group = "", description = "";

        public Builder id(String v)          { id = v;          return this; }
        public Builder number(String v)      { number = v;      return this; }
        public Builder name(String v)        { name = v;        return this; }
        public Builder url(String v)         { url = v;         return this; }
        public Builder type(String v)        { type = v;        return this; }
        public Builder logo(String v)        { logo = v;        return this; }
        public Builder genre(String v)       { genre = v;       return this; }
        public Builder group(String v)       { group = v;       return this; }
        public Builder description(String v) { description = v; return this; }
        public ChannelModel build()          { return new ChannelModel(this); }
    }
}
