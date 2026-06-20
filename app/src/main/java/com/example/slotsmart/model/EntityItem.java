package com.example.slotsmart.model;

import com.google.gson.JsonObject;

/**
 * Generic wrapper used by the RecyclerView adapter across all admin list screens.
 * Each screen sets title, subtitle, badge and stores the raw JsonObject for edit dialogs.
 */
public class EntityItem {
    public String id;
    public String title;
    public String subtitle;
    public String badge;
    public JsonObject raw;

    public EntityItem(String id, String title, String subtitle, String badge, JsonObject raw) {
        this.id = id;
        this.title = title;
        this.subtitle = subtitle;
        this.badge = badge;
        this.raw = raw;
    }
}
