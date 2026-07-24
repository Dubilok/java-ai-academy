package com.javaacademy.platform.ai.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HintLeakDetectorTest {

    // ── clean hints — should NOT be flagged ───────────────────────────────────

    @Test
    void containsSolutionCode_noCodeFence_returnsFalse() {
        String hint = "What does the error message tell you about the expected type? "
                + "Which line in your code computes the value that the test checks?";

        assertThat(HintLeakDetector.containsSolutionCode(hint)).isFalse();
    }

    @Test
    void containsSolutionCode_smallSnippetTwoLines_returnsFalse() {
        String hint = "Consider the Math utility class:\n\n"
                + "```java\n"
                + "Math.sqrt(x)\n"
                + "Math.pow(x, 2)\n"
                + "```\n\n"
                + "Which of these relates to the Euclidean formula?";

        assertThat(HintLeakDetector.containsSolutionCode(hint)).isFalse();
    }

    @Test
    void containsSolutionCode_fenceWithOnlyComments_returnsFalse() {
        String hint = "Notice the structure:\n\n"
                + "```java\n"
                + "// Step 1: compute the delta for each axis\n"
                + "// Step 2: sum the squares\n"
                + "// Step 3: take the square root\n"
                + "```\n\n"
                + "Which step is your current code skipping?";

        assertThat(HintLeakDetector.containsSolutionCode(hint)).isFalse();
    }

    @Test
    void containsSolutionCode_nullHint_returnsFalse() {
        assertThat(HintLeakDetector.containsSolutionCode(null)).isFalse();
    }

    @Test
    void containsSolutionCode_blankHint_returnsFalse() {
        assertThat(HintLeakDetector.containsSolutionCode("   ")).isFalse();
    }

    // ── leaky hints — SHOULD be flagged ──────────────────────────────────────

    @Test
    void containsSolutionCode_methodBodyFourLines_returnsTrue() {
        String hint = "Here is a hint:\n\n"
                + "```java\n"
                + "public double distanceTo(Point other) {\n"
                + "    double dx = this.x - other.x;\n"
                + "    double dy = this.y - other.y;\n"
                + "    return Math.sqrt(dx * dx + dy * dy);\n"
                + "}\n"
                + "```\n\n"
                + "Does this help?";

        assertThat(HintLeakDetector.containsSolutionCode(hint)).isTrue();
    }

    @Test
    void containsSolutionCode_multipleStatementsFourLines_returnsTrue() {
        String hint = "Try this approach:\n\n"
                + "```java\n"
                + "double dx = x1 - x2;\n"
                + "double dy = y1 - y2;\n"
                + "double sumSquares = dx * dx + dy * dy;\n"
                + "return Math.sqrt(sumSquares);\n"
                + "```";

        assertThat(HintLeakDetector.containsSolutionCode(hint)).isTrue();
    }

    @Test
    void containsSolutionCode_plainFenceWithFourSubstantiveLines_returnsTrue() {
        String hint = "Solution:\n\n"
                + "```\n"
                + "double dx = x1 - x2;\n"
                + "double dy = y1 - y2;\n"
                + "double d = Math.sqrt(dx*dx + dy*dy);\n"
                + "return d;\n"
                + "```";

        assertThat(HintLeakDetector.containsSolutionCode(hint)).isTrue();
    }

    @Test
    void containsSolutionCode_exactlyThreeSubstantiveLines_returnsFalse() {
        String hint = "Consider:\n\n"
                + "```java\n"
                + "double dx = x1 - x2;\n"
                + "double dy = y1 - y2;\n"
                + "double distance = Math.sqrt(dx*dx);\n"
                + "```\n\n"
                + "How does this relate to the full formula?";

        assertThat(HintLeakDetector.containsSolutionCode(hint)).isFalse();
    }
}
