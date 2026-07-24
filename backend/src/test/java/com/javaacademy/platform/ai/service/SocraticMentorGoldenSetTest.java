package com.javaacademy.platform.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.javaacademy.platform.ai.client.LlmClient;
import com.javaacademy.platform.ai.client.LlmResponse;
import com.javaacademy.platform.ai.dto.HintRequest;
import com.javaacademy.platform.ai.util.HintLeakDetector;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Golden-set test: 10 representative broken Java submissions each receive a Socratic hint
 * that contains no compilable solution code.
 *
 * <p>The LLM is stubbed to return a clean hint on every call. The test asserts that
 * {@link HintLeakDetector#containsSolutionCode(String)} returns {@code false} for every hint,
 * confirming the detector correctly classifies Socratic questions as safe to return to students.
 */
class SocraticMentorGoldenSetTest {

    static final String SYSTEM_PROMPT = "You are a Socratic mentor. Never provide solution code.";

    static final String CLEAN_HINT_1 =
            "What does the compiler mean when it says 'incompatible types'? Which type does your method declare it returns, and which type are you actually returning?";

    static final String CLEAN_HINT_2 =
            "Look at line 5 — what is the value of `i` when the loop body executes for the very first time? Is that the index you intended to start from?";

    static final String CLEAN_HINT_3 =
            "The test expects `null` when the list is empty, but what does your code return in that case? Trace through the method step by step with an empty input.";

    static final String CLEAN_HINT_4 =
            "You declared the variable inside the `if` block. What scope does that give it, and where in the code do you try to use it later?";

    static final String CLEAN_HINT_5 =
            "What is the difference between `==` and `.equals()` when comparing `String` objects in Java? Which one checks object identity and which checks value equality?";

    static final String CLEAN_HINT_6 =
            "Your `NullPointerException` points to line 12. Before you call a method on an object, what should you always verify about that object's state?";

    static final String CLEAN_HINT_7 =
            "The test passes `[3, 1, 4, 1, 5]` and expects `[1, 1, 3, 4, 5]`. Walk me through what your algorithm does to that array on the very first pass — does the largest or smallest element move first?";

    static final String CLEAN_HINT_8 =
            "You have a `Stack<Integer>` and you push elements, then pop them. In what order does a stack return elements — first-in-first-out or last-in-first-out? Does that match what the test expects?";

    static final String CLEAN_HINT_9 =
            "The `@Test` output says `expected: <25> but was: <0>`. Your `area()` method always returns a fixed number — where in the code should it use the field values instead?";

    static final String CLEAN_HINT_10 =
            "Think about integer division in Java: what does `7 / 2` evaluate to, and how does that affect the result you compute? How would you preserve the decimal portion?";

    LlmClient llmClient;
    SocraticMentorService service;

    @BeforeEach
    void setUp() {
        llmClient = mock(LlmClient.class);
        service = new SocraticMentorService(llmClient, SYSTEM_PROMPT);
    }

    static List<Object[]> brokenSubmissions() {
        return List.of(
                new Object[] {
                    new HintRequest(
                            "Return the sum of a list",
                            "Write a method `int sum(List<Integer> numbers)` that returns the sum of all elements.",
                            "public int sum(List<Integer> numbers) {\n    return \"0\";\n}",
                            "error: incompatible types: String cannot be converted to int"),
                    CLEAN_HINT_1
                },
                new Object[] {
                    new HintRequest(
                            "Print numbers 1 to 10",
                            "Write a loop that prints the integers from 1 to 10, each on its own line.",
                            "for (int i = 0; i <= 10; i++) { System.out.println(i); }",
                            "AssertionError: expected <1> but was <0>"),
                    CLEAN_HINT_2
                },
                new Object[] {
                    new HintRequest(
                            "Find max in list",
                            "Return the maximum element in a non-empty list, or null if the list is empty.",
                            "public Integer findMax(List<Integer> list) {\n    return Collections.max(list);\n}",
                            "java.util.NoSuchElementException: Collection is empty"),
                    CLEAN_HINT_3
                },
                new Object[] {
                    new HintRequest(
                            "Classify temperature",
                            "Write code that sets `String label` to 'hot' if temp > 30, else 'cool', then prints it.",
                            "if (temp > 30) { String label = \"hot\"; } else { String label = \"cool\"; } System.out.println(label);",
                            "error: cannot find symbol: variable label"),
                    CLEAN_HINT_4
                },
                new Object[] {
                    new HintRequest(
                            "Check palindrome",
                            "Return true if the given string is the same forwards and backwards.",
                            "public boolean isPalindrome(String s) {\n    return s == new StringBuilder(s).reverse().toString();\n}",
                            "AssertionError: expected: <true> but was: <false>"),
                    CLEAN_HINT_5
                },
                new Object[] {
                    new HintRequest(
                            "Get first character",
                            "Return the first character of the given string. The string may be null.",
                            "public char firstChar(String s) {\n    return s.charAt(0);\n}",
                            "java.lang.NullPointerException at Solution.java:12"),
                    CLEAN_HINT_6
                },
                new Object[] {
                    new HintRequest(
                            "Sort an array ascending",
                            "Sort the integer array in ascending order without using built-in sort.",
                            "public void sort(int[] arr) {\n    for (int i = 0; i < arr.length; i++)\n      for (int j = 0; j < arr.length; j++)\n        if (arr[i] < arr[j]) { int tmp = arr[i]; arr[i] = arr[j]; arr[j] = tmp; }\n}",
                            "AssertionError: arrays differ at element [0]: expected:<1> but was:<3>"),
                    CLEAN_HINT_7
                },
                new Object[] {
                    new HintRequest(
                            "Reverse a list using a stack",
                            "Use a Stack to reverse the given list and return a new list.",
                            "Stack<Integer> stack = new Stack<>();\nfor (int x : list) stack.push(x);\nList<Integer> result = new ArrayList<>();\nwhile (!stack.isEmpty()) result.add(stack.pop());",
                            "AssertionError: expected:<[1, 2, 3]> but was:<[3, 2, 1]>"),
                    CLEAN_HINT_8
                },
                new Object[] {
                    new HintRequest(
                            "Rectangle area",
                            "Create a Rectangle class with width and height fields and an `area()` method.",
                            "public class Rectangle {\n    int width;\n    int height;\n    public int area() { return 0; }\n}",
                            "AssertionError: expected: <25> but was: <0>"),
                    CLEAN_HINT_9
                },
                new Object[] {
                    new HintRequest(
                            "Average of two integers",
                            "Return the average of two integers as a double.",
                            "public double average(int a, int b) {\n    return (a + b) / 2;\n}",
                            "AssertionError: expected: <3.5> but was: <3.0>"),
                    CLEAN_HINT_10
                });
    }

    @ParameterizedTest(name = "[{index}] task: {0}")
    @MethodSource("brokenSubmissions")
    void generateHint_brokenSubmission_hintContainsNoSolutionCode(HintRequest request, String stubbedHint) {
        when(llmClient.complete(any())).thenReturn(new LlmResponse(stubbedHint, 50, 120));

        String hint = service.generateHint(request);

        assertThat(HintLeakDetector.containsSolutionCode(hint))
                .as("Hint for task '%s' must not contain solution code, but was:\n%s", request.taskTitle(), hint)
                .isFalse();
    }

    @ParameterizedTest(name = "[{index}] task: {0}")
    @MethodSource("brokenSubmissions")
    void generateHint_brokenSubmission_hintIsNotBlank(HintRequest request, String stubbedHint) {
        when(llmClient.complete(any())).thenReturn(new LlmResponse(stubbedHint, 50, 120));

        String hint = service.generateHint(request);

        assertThat(hint)
                .as("Hint for task '%s' must not be blank", request.taskTitle())
                .isNotBlank();
    }

    @ParameterizedTest(name = "[{index}] task: {0}")
    @MethodSource("brokenSubmissions")
    void generateHint_brokenSubmission_hintContainsQuestion(HintRequest request, String stubbedHint) {
        when(llmClient.complete(any())).thenReturn(new LlmResponse(stubbedHint, 50, 120));

        String hint = service.generateHint(request);

        assertThat(hint)
                .as("Hint for task '%s' should contain at least one question mark (Socratic hint)", request.taskTitle())
                .contains("?");
    }
}
