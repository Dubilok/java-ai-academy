package com.javaacademy.platform.ai.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.experimental.UtilityClass;

/**
 * Detects whether a Socratic hint response contains solution code.
 *
 * <p>A hint is flagged if any code fence contains more than {@link #MAX_SUBSTANTIVE_LINES}
 * non-blank, non-comment lines — the heuristic that distinguishes a brief illustrative
 * snippet (allowed) from a method body that solves the task (banned).
 */
@UtilityClass
public class HintLeakDetector {

    static final int MAX_SUBSTANTIVE_LINES = 3;

    private static final Pattern CODE_FENCE_PATTERN = Pattern.compile("```(?:java)?\\s*\\n(.*?)```", Pattern.DOTALL);

    public static boolean containsSolutionCode(String hint) {
        if (hint == null || hint.isBlank()) {
            return false;
        }
        List<String> fences = extractCodeFences(hint);
        for (String fenceContent : fences) {
            if (countSubstantiveLines(fenceContent) > MAX_SUBSTANTIVE_LINES) {
                return true;
            }
        }
        return false;
    }

    private static List<String> extractCodeFences(String text) {
        List<String> fences = new ArrayList<>();
        Matcher matcher = CODE_FENCE_PATTERN.matcher(text);
        while (matcher.find()) {
            fences.add(matcher.group(1));
        }
        return fences;
    }

    private static int countSubstantiveLines(String code) {
        int count = 0;
        for (String line : code.split("\n", -1)) {
            String trimmed = line.strip();
            if (!trimmed.isEmpty()
                    && !trimmed.startsWith("//")
                    && !trimmed.startsWith("*")
                    && !trimmed.startsWith("/*")) {
                count++;
            }
        }
        return count;
    }
}
