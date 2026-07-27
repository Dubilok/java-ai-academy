package com.javaacademy.platform.sandbox.util;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JavaClassNameExtractor {

    // Matches "public class Foo", "class Foo", "public final class Foo" etc.
    private static final Pattern ANY_CLASS = Pattern.compile("(?:public\\s+)?(?:final\\s+)?class\\s+(\\w+)");
    private static final Pattern PUBLIC_CLASS = Pattern.compile("public\\s+(?:final\\s+)?class\\s+(\\w+)");

    /**
     * Returns the public class name if one exists, otherwise falls back to the first class name
     * found. Test classes often omit the public modifier.
     */
    public static Optional<String> extractPublicClassName(String javaSource) {
        Matcher publicMatcher = PUBLIC_CLASS.matcher(javaSource);
        if (publicMatcher.find()) {
            return Optional.of(publicMatcher.group(1));
        }
        Matcher anyMatcher = ANY_CLASS.matcher(javaSource);
        return anyMatcher.find() ? Optional.of(anyMatcher.group(1)) : Optional.empty();
    }
}
