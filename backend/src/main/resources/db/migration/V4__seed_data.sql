-- Seed data: Java 21 Fundamentals — 1 course, 1 module, 3 lectures, 5 tasks.
-- All content is original; not derived from any third-party source.
-- Uses a DO block with dollar-quoting to embed multi-line code strings cleanly.

DO $migration$
DECLARE
    v_course_id   UUID;
    v_module_id   UUID;
    v_lecture1_id UUID;
    v_lecture2_id UUID;
    v_lecture3_id UUID;
BEGIN

    -- ── Course ────────────────────────────────────────────────────────────────

    INSERT INTO courses (title, description, technology, is_published)
    VALUES (
        'Java 21 Fundamentals',
        'Learn the core Java 21 language — strings, control flow, and methods through hands-on coding tasks.',
        'Java',
        TRUE
    )
    RETURNING id INTO v_course_id;

    -- ── Module ────────────────────────────────────────────────────────────────

    INSERT INTO modules (course_id, title, order_index)
    VALUES (v_course_id, 'Core Language Concepts', 1)
    RETURNING id INTO v_module_id;

    -- ── Lectures ──────────────────────────────────────────────────────────────

    INSERT INTO lectures (module_id, title, content_markdown, order_index)
    VALUES (v_module_id, 'Working with Strings', $lec1$
# Working with Strings

Java's `String` class represents an immutable sequence of characters. Once created, a String cannot be changed — any operation that appears to modify it returns a new String.

## Key methods

| Method | Returns | What it does |
|---|---|---|
| `length()` | `int` | Number of characters |
| `charAt(i)` | `char` | Character at position `i` |
| `indexOf(s)` | `int` | First position of `s`, or -1 |
| `substring(start, end)` | `String` | Characters from `start` to `end - 1` |
| `toLowerCase()` | `String` | All characters in lower case |
| `toUpperCase()` | `String` | All characters in upper case |
| `equals(other)` | `boolean` | True if content matches (`==` compares identity, not content) |
| `toCharArray()` | `char[]` | Converts to a character array for iteration |

## Building strings efficiently

Since String is immutable, joining characters in a loop creates many temporary objects. Use `StringBuilder` instead:

```java
StringBuilder builder = new StringBuilder();
for (int i = 0; i < 5; i++) {
    builder.append(i);
}
String result = builder.toString(); // "01234"
```

`StringBuilder` also provides a `reverse()` method that reverses its content in place.
$lec1$, 1)
    RETURNING id INTO v_lecture1_id;

    INSERT INTO lectures (module_id, title, content_markdown, order_index)
    VALUES (v_module_id, 'Control Flow', $lec2$
# Control Flow

Control flow statements determine which code runs and how many times.

## Conditional — if / else if / else

```java
int score = 85;
if (score >= 90) {
    System.out.println("A");
} else if (score >= 80) {
    System.out.println("B");   // this branch runs
} else {
    System.out.println("C or below");
}
```

## For loop — repeat a fixed number of times

```java
for (int i = 0; i < 5; i++) {
    System.out.println(i); // prints 0 1 2 3 4
}
```

## For-each loop — iterate arrays and collections

```java
int[] numbers = {10, 20, 30};
for (int number : numbers) {
    System.out.println(number);
}
```

## While loop — repeat while a condition holds

```java
int n = 1;
while (n <= 3) {
    System.out.println(n);
    n++;  // 1 2 3
}
```

## The modulo operator `%`

`a % b` is the remainder after dividing `a` by `b`.

- `n % 2 == 0` means `n` is even
- `n % 3 == 0` means `n` is divisible by 3
- `n % 15 == 0` means `n` is divisible by both 3 and 5
$lec2$, 2)
    RETURNING id INTO v_lecture2_id;

    INSERT INTO lectures (module_id, title, content_markdown, order_index)
    VALUES (v_module_id, 'Writing Methods', $lec3$
# Writing Methods

A method groups logic under a name so you can call it repeatedly without copying code.

## Anatomy of a method

```java
public int add(int first, int second) {
    return first + second;
}
```

- **`public`** — any code can call this method
- **`int`** — the return type; use `void` if nothing is returned
- **`add`** — the method name (camelCase by convention)
- **`(int first, int second)`** — typed parameters; give them clear, descriptive names
- **`return`** — exits the method and sends a value back to the caller

## Calling a method

```java
Solution solution = new Solution();
int result = solution.add(3, 4);  // result == 7
```

## Useful methods from the Math class

Java includes many built-in helpers under `Math`:

```java
Math.max(a, b)    // returns the larger of a and b
Math.min(a, b)    // returns the smaller of a and b
Math.abs(n)       // returns n without its sign
Math.sqrt(n)      // returns the square root of n
```

## When to write a new method

- When a block of logic has a single clear purpose that can be named
- When the same logic is needed in two or more places
- When a method grows beyond roughly 20 lines
$lec3$, 3)
    RETURNING id INTO v_lecture3_id;

    -- ── Tasks ─────────────────────────────────────────────────────────────────
    -- Task 1 — Reverse a String (Lecture 1)

    INSERT INTO tasks (lecture_id, title, description, difficulty, template_code, test_code, solution_code, xp_reward)
    VALUES (
        v_lecture1_id,
        'Reverse a String',
        'Implement the `reverse` method that takes a String and returns a new String with the characters in reverse order.',
        'EASY',
        $tmpl1$
public class Solution {
    public String reverse(String input) {
        // Write your code here
        return null;
    }
}
$tmpl1$,
        $test1$
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SolutionTest {
    private final Solution solution = new Solution();

    @Test
    void reverse_basicString_returnsReversed() {
        assertEquals("olleh", solution.reverse("hello"));
    }

    @Test
    void reverse_emptyString_returnsEmpty() {
        assertEquals("", solution.reverse(""));
    }

    @Test
    void reverse_singleChar_returnsSameChar() {
        assertEquals("a", solution.reverse("a"));
    }

    @Test
    void reverse_palindrome_returnsSameString() {
        assertEquals("racecar", solution.reverse("racecar"));
    }
}
$test1$,
        $sol1$
public class Solution {
    public String reverse(String input) {
        return new StringBuilder(input).reverse().toString();
    }
}
$sol1$,
        100
    );

    -- Task 2 — Count Vowels (Lecture 1)

    INSERT INTO tasks (lecture_id, title, description, difficulty, template_code, test_code, solution_code, xp_reward)
    VALUES (
        v_lecture1_id,
        'Count Vowels',
        'Implement `countVowels` that returns the number of vowel characters (a, e, i, o, u — case-insensitive) in the input string.',
        'EASY',
        $tmpl2$
public class Solution {
    public int countVowels(String input) {
        // Write your code here — count a, e, i, o, u (case-insensitive)
        return 0;
    }
}
$tmpl2$,
        $test2$
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SolutionTest {
    private final Solution solution = new Solution();

    @Test
    void countVowels_mixedCase_countsCorrectly() {
        assertEquals(2, solution.countVowels("Hello"));
    }

    @Test
    void countVowels_emptyString_returnsZero() {
        assertEquals(0, solution.countVowels(""));
    }

    @Test
    void countVowels_allUpperVowels_returnsCount() {
        assertEquals(5, solution.countVowels("AEIOU"));
    }

    @Test
    void countVowels_noVowels_returnsZero() {
        assertEquals(0, solution.countVowels("xyz"));
    }
}
$test2$,
        $sol2$
public class Solution {
    public int countVowels(String input) {
        int count = 0;
        for (char character : input.toCharArray()) {
            if ("aeiouAEIOU".indexOf(character) >= 0) {
                count++;
            }
        }
        return count;
    }
}
$sol2$,
        100
    );

    -- Task 3 — FizzBuzz (Lecture 2)

    INSERT INTO tasks (lecture_id, title, description, difficulty, template_code, test_code, solution_code, xp_reward)
    VALUES (
        v_lecture2_id,
        'FizzBuzz',
        'Implement `fizzBuzz(int number)`: return "FizzBuzz" if divisible by both 3 and 5, "Fizz" if divisible by 3, "Buzz" if divisible by 5, or the number as a String otherwise.',
        'EASY',
        $tmpl3$
public class Solution {
    public String fizzBuzz(int number) {
        // Return "FizzBuzz" if divisible by both 3 and 5,
        // "Fizz" if divisible by 3, "Buzz" if divisible by 5,
        // otherwise return String.valueOf(number).
        return null;
    }
}
$tmpl3$,
        $test3$
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SolutionTest {
    private final Solution solution = new Solution();

    @Test
    void fizzBuzz_divisibleByThree_returnsFizz() {
        assertEquals("Fizz", solution.fizzBuzz(9));
    }

    @Test
    void fizzBuzz_divisibleByFive_returnsBuzz() {
        assertEquals("Buzz", solution.fizzBuzz(10));
    }

    @Test
    void fizzBuzz_divisibleByBoth_returnsFizzBuzz() {
        assertEquals("FizzBuzz", solution.fizzBuzz(15));
    }

    @Test
    void fizzBuzz_notDivisible_returnsNumberAsString() {
        assertEquals("7", solution.fizzBuzz(7));
    }
}
$test3$,
        $sol3$
public class Solution {
    public String fizzBuzz(int number) {
        if (number % 15 == 0) return "FizzBuzz";
        if (number % 3 == 0) return "Fizz";
        if (number % 5 == 0) return "Buzz";
        return String.valueOf(number);
    }
}
$sol3$,
        100
    );

    -- Task 4 — Sum of Even Numbers (Lecture 2)

    INSERT INTO tasks (lecture_id, title, description, difficulty, template_code, test_code, solution_code, xp_reward)
    VALUES (
        v_lecture2_id,
        'Sum of Even Numbers',
        'Implement `sumOfEvens(int[] numbers)` that returns the sum of all even numbers in the array.',
        'EASY',
        $tmpl4$
public class Solution {
    public long sumOfEvens(int[] numbers) {
        // Write your code here — return the sum of all even numbers
        return 0;
    }
}
$tmpl4$,
        $test4$
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SolutionTest {
    private final Solution solution = new Solution();

    @Test
    void sumOfEvens_mixedArray_returnsSumOfEvenNumbers() {
        assertEquals(6L, solution.sumOfEvens(new int[]{1, 2, 3, 4}));
    }

    @Test
    void sumOfEvens_emptyArray_returnsZero() {
        assertEquals(0L, solution.sumOfEvens(new int[]{}));
    }

    @Test
    void sumOfEvens_noEvens_returnsZero() {
        assertEquals(0L, solution.sumOfEvens(new int[]{1, 3, 5}));
    }

    @Test
    void sumOfEvens_allEvens_returnsSum() {
        assertEquals(12L, solution.sumOfEvens(new int[]{2, 4, 6}));
    }
}
$test4$,
        $sol4$
public class Solution {
    public long sumOfEvens(int[] numbers) {
        long sum = 0;
        for (int number : numbers) {
            if (number % 2 == 0) {
                sum += number;
            }
        }
        return sum;
    }
}
$sol4$,
        100
    );

    -- Task 5 — Maximum of Three (Lecture 3)

    INSERT INTO tasks (lecture_id, title, description, difficulty, template_code, test_code, solution_code, xp_reward)
    VALUES (
        v_lecture3_id,
        'Maximum of Three',
        'Implement `maxOfThree(int first, int second, int third)` that returns the largest of the three values.',
        'EASY',
        $tmpl5$
public class Solution {
    public int maxOfThree(int first, int second, int third) {
        // Write your code here — return the largest of the three values
        return 0;
    }
}
$tmpl5$,
        $test5$
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SolutionTest {
    private final Solution solution = new Solution();

    @Test
    void maxOfThree_firstIsLargest_returnsFirst() {
        assertEquals(10, solution.maxOfThree(10, 5, 3));
    }

    @Test
    void maxOfThree_secondIsLargest_returnsSecond() {
        assertEquals(10, solution.maxOfThree(3, 10, 5));
    }

    @Test
    void maxOfThree_thirdIsLargest_returnsThird() {
        assertEquals(10, solution.maxOfThree(3, 5, 10));
    }

    @Test
    void maxOfThree_allEqual_returnsSameValue() {
        assertEquals(7, solution.maxOfThree(7, 7, 7));
    }

    @Test
    void maxOfThree_negativeValues_returnsLargest() {
        assertEquals(-1, solution.maxOfThree(-1, -5, -10));
    }
}
$test5$,
        $sol5$
public class Solution {
    public int maxOfThree(int first, int second, int third) {
        return Math.max(first, Math.max(second, third));
    }
}
$sol5$,
        150
    );

END
$migration$;
