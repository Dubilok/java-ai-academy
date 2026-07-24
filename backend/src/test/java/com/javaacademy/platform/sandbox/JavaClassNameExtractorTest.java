package com.javaacademy.platform.sandbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.javaacademy.platform.sandbox.util.JavaClassNameExtractor;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class JavaClassNameExtractorTest {

    @Test
    void extractPublicClassName_singlePublicClass_returnsName() {
        String source = "public class StringConcatTest { @Test void test() {} }";
        Optional<String> result = JavaClassNameExtractor.extractPublicClassName(source);
        assertThat(result).hasValue("StringConcatTest");
    }

    @Test
    void extractPublicClassName_withPackageAndImports_returnsFirstPublicClass() {
        String source =
                """
                package com.example;
                import org.junit.jupiter.api.Test;
                public class TaskTest {
                    @Test void passes() {}
                }
                """;
        assertThat(JavaClassNameExtractor.extractPublicClassName(source)).hasValue("TaskTest");
    }

    @Test
    void extractPublicClassName_noPublicClass_returnsEmpty() {
        String source = "class Solution { int solve() { return 0; } }";
        assertThat(JavaClassNameExtractor.extractPublicClassName(source)).isEmpty();
    }

    @Test
    void extractPublicClassName_emptyString_returnsEmpty() {
        assertThat(JavaClassNameExtractor.extractPublicClassName("")).isEmpty();
    }
}
