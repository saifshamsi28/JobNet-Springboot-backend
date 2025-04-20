package com.saif.JobNet;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NumberExtractor {

    // Extracts the first float or integer from a string
    public static String extractNumber(String input) {
        if (input == null || input.isEmpty()) return null;

        // Match float or integer (e.g., "4.5", "100")
        Pattern pattern = Pattern.compile("(\\d+(\\.\\d+)?)");
        Matcher matcher = pattern.matcher(input);

        if (matcher.find()) {
            return matcher.group(1);  // returns the number as string
        }
        return null;
    }
}
