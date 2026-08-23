package com.fabri.ministerium;

import org.json.JSONObject;

public final class PersonalPrayer {
    public String id, title, text;
    JSONObject toJson() throws Exception {
        return new JSONObject().put("id", id).put("title", title).put("text", text);
    }
    static PersonalPrayer fromJson(JSONObject value) {
        PersonalPrayer prayer = new PersonalPrayer(); prayer.id = value.optString("id");
        prayer.title = value.optString("title"); prayer.text = value.optString("text"); return prayer;
    }
}
