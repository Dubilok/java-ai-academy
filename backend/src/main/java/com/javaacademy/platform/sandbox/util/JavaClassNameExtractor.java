package com.javaacademy.platform.sandbox.util;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JavaClassNameExtractor {

    private static final Pattern PUBLIC_CLASS = Pattern.compile("public\\s+class\\s+(\\w+)");

    /** Returns the first public class name found in the Java source, or empty if none found. */
    public static Optional<String> extractPublicClassName(String javaSource) {
        Matcher matcher = PUBLIC_CLASS.matcher(javaSource);
        return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
    }
}
