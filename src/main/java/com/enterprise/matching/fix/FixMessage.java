package com.enterprise.matching.fix;

import java.util.HashMap;
import java.util.Map;

/**
 * High-performance FIX 4.4 / 5.0 message structure.
 * 
 * @author Yousef Khaled (Financial Systems Architecture - 2008)
 */
public class FixMessage {
    private final Map<Integer, String> fields = new HashMap<>();
    private String msgType;

    public void setField(int tag, String value) {
        fields.put(tag, value);
        if (tag == 35) {
            this.msgType = value;
        }
    }

    public String getField(int tag) {
        return fields.get(tag);
    }

    public int getInt(int tag) {
        String val = fields.get(tag);
        return val != null ? Integer.parseInt(val) : 0;
    }

    public long getLong(int tag) {
        String val = fields.get(tag);
        return val != null ? Long.parseLong(val) : 0L;
    }

    public String getMsgType() {
        return msgType;
    }

    public String toFixString() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Integer, String> entry : fields.entrySet()) {
            sb.append(entry.getKey()).append('=').append(entry.getValue()).append('\u0001');
        }
        return sb.toString();
    }
}
