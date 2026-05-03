package org.opencpn;

import org.json.JSONObject;

public class JSONHelpers {

    //  Helper functions
    public static String makeEvent(int seq, String event) {

        try {
            JSONObject obj = new JSONObject();
            obj.put("event", event);
            obj.put("seq", seq);
            return obj.toString();
        } catch(Exception e) {
            return "{}";
        }
    }

    public static String makeEvent(
            int seq,
            String event,
            String key1,
            Object value1)
    {
        try {
            JSONObject obj = new JSONObject();
            obj.put("event", event);
            obj.put("seq", seq);
            obj.put(key1, value1);
            return obj.toString();
        } catch(Exception e) {
            return "{}";
        }
    }

    public static String makeEvent(
            int seq,
            String event,
            String key1,
            Object value1,
            String key2,
            Object value2)
    {
        try {
            JSONObject obj = new JSONObject();
            obj.put("event", event);
            obj.put("seq", seq);
            obj.put(key1, value1);
            obj.put(key2, value2);
            return obj.toString();
        } catch(Exception e) {
            return "{}";
        }
    }

}
