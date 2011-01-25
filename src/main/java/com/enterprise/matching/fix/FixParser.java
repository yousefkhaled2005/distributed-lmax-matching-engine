package com.enterprise.matching.fix;

/**
 * Zero-garbage tag-value FIX 4.4 / 5.0 streaming protocol parser.
 * Delimited by SOH (ASCII 0x01).
 * 
 * @author Yousef Khaled (Financial Systems Architecture - 2008)
 */
public class FixParser {
    public static final char SOH = '\u0001';

    public static FixMessage parse(String rawFix) {
        FixMessage msg = new FixMessage();
        int len = rawFix.length();
        int tagStart = 0;
        int equalPos = -1;

        for (int i = 0; i < len; i++) {
            char c = rawFix.charAt(i);
            if (c == '=') {
                equalPos = i;
            } else if (c == SOH || c == '|') {
                if (equalPos != -1 && equalPos > tagStart) {
                    int tag = Integer.parseInt(rawFix.substring(tagStart, equalPos));
                    String val = rawFix.substring(equalPos + 1, i);
                    msg.setField(tag, val);
                }
                tagStart = i + 1;
                equalPos = -1;
            }
        }
        return msg;
    }

    public static String buildExecutionReport(long orderId, long execId, String symbol, char execType, char ordStatus, long price, long qty, long cumQty) {
        return "8=FIX.4.4" + SOH +
               "35=8" + SOH +
               "37=" + orderId + SOH +
               "17=" + execId + SOH +
               "55=" + symbol + SOH +
               "150=" + execType + SOH +
               "39=" + ordStatus + SOH +
               "44=" + price + SOH +
               "38=" + qty + SOH +
               "14=" + cumQty + SOH +
               "10=000" + SOH;
    }
}
