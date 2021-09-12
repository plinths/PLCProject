package plc.homework;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Contains JUnit tests for {@link Regex}. A framework of the test structure 
 * is provided, you will fill in the remaining pieces.
 *
 * To run tests, either click the run icon on the left margin, which can be used
 * to run all tests or only a specific test. You should make sure your tests are
 * run through IntelliJ (File > Settings > Build, Execution, Deployment > Build
 * Tools > Gradle > Run tests using <em>IntelliJ IDEA</em>). This ensures the
 * name and inputs for the tests are displayed correctly in the run window.
 */
public class RegexTests {

    /**
     * This is a parameterized test for the {@link Regex#EMAIL} regex. The
     * {@link ParameterizedTest} annotation defines this method as a
     * parameterized test, and {@link MethodSource} tells JUnit to look for the
     * static method {@link #testEmailRegex()}.
     *
     * For personal preference, I include a test name as the first parameter
     * which describes what that test should be testing - this is visible in
     * IntelliJ when running the tests (see above note if not working).
     */
    @ParameterizedTest
    @MethodSource
    public void testEmailRegex(String test, String input, boolean success) {
        test(input, Regex.EMAIL, success);
    }

    /**
     * This is the factory method providing test cases for the parameterized
     * test above - note that it is static, takes no arguments, and has the same
     * name as the test. The {@link Arguments} object contains the arguments for
     * each test to be passed to the function above.
     */
    public static Stream<Arguments> testEmailRegex() {
        return Stream.of(
                Arguments.of("Alphanumeric", "thelegend27@gmail.com", true),
                Arguments.of("UF Domain", "otherdomain@ufl.edu", true),
                Arguments.of("numeric", "12345@gmail.com",true),
                Arguments.of("alpha","abcdefgh@gmail.com",true),
                Arguments.of("multiple periods","other@ufl.instructure.com", true),

                Arguments.of("Missing Domain Dot", "missingdot@gmailcom", false),
                Arguments.of("Symbols", "symbols#$%@gmail.com", false),
                Arguments.of("2 letter domain", "the@gmail.co",false),
                Arguments.of("one letter" , "a@gmail.com", false),
                Arguments.of("no @" , "otherdomainufl.edu", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testOddStringsRegex(String test, String input, boolean success) {
        test(input, Regex.ODD_STRINGS, success);
    }

    public static Stream<Arguments> testOddStringsRegex() {
        return Stream.of(
                // what have eleven letters and starts with gas?
                Arguments.of("11 Characters", "automobiles", true),
                Arguments.of("13 Characters", "i<3pancakes13", true),
                Arguments.of("19 Characters", "0123456789123456789", true),
                Arguments.of("15 Characters", "fifteen fifteen", true),
                Arguments.of("any character 11", "      .....", true),

                Arguments.of("9 character", "9ninechar", false),
                Arguments.of("20 Characters", "01234567890123456789", false),
                Arguments.of("10 Characters", "0123456789", false),
                Arguments.of("5 Characters", "5five", false),
                Arguments.of("14 Characters", "i<3pancakes14!", false)

        );
    }

    @ParameterizedTest
    @MethodSource
    public void testCharacterListRegex(String test, String input, boolean success) {
        test(input, Regex.CHARACTER_LIST, success);
    }

    public static Stream<Arguments> testCharacterListRegex() {
        return Stream.of(
                Arguments.of("Single Element", "['a']", true),
                Arguments.of("Multiple Elements", "['a','b','c']", true),
                Arguments.of("Empty", "[]",true ),
                Arguments.of("two surrounding spaces", "[ 'a' ,'b','c']", true),
                Arguments.of("valid", "['a', 'b','c', 'd' ]", true),

                Arguments.of("no single quotes around character","[a]", false),
                Arguments.of("two characters with single quotes", "['a ', ' b', 'c']", false),
                Arguments.of("Missing Brackets", "'a','b','c'", false),
                Arguments.of("Consecutive Spaces", "[  'a','b','c']", false),
                Arguments.of("Missing Commas", "['a' 'b' 'c']", false)

        );
    }

    @ParameterizedTest
    @MethodSource
    public void testDecimalRegex(String test, String input, boolean success) {
        test(input, Regex.DECIMAL, success);
    }

    public static Stream<Arguments> testDecimalRegex() {
        return Stream.of(
                Arguments.of("1st example", "10100.001",true),
                Arguments.of("2nd example", "-1.0", true),
                Arguments.of("Trailing zeros", "1.0000000", true),
                Arguments.of("1 leading 0","0.999" , true),
                Arguments.of("valid","0.00090", true),

                Arguments.of("No decimal", "1", false),
                Arguments.of("Nothing before decimal", ".5", false),
                Arguments.of("Only the decimal", ".", false),
                Arguments.of("leading 0","0111.001",false),
                Arguments.of("Multiple negatives", "----1.0", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testStringRegex(String test, String input, boolean success) {
        test(input, Regex.STRING, success);
    }

    public static Stream<Arguments> testStringRegex() {
        return Stream.of(
                Arguments.of("empty quotes","\"\"", true),
                Arguments.of("valid 2", "\"Hello, World!\"", true),
                Arguments.of("valid 3", "\"1\\t2\"", true),
                Arguments.of("bnrt without backslashes","\"bnrt\"", true),
                Arguments.of("valid escape backslash","\"1\\\\2\"",true),
                Arguments.of("backslash escape","\"\\\\\"",true),
                Arguments.of("apostrophe exit","\"\\\'\"",true),


                Arguments.of("unterminated", "\"unterminated", false),
                Arguments.of("invalid escape(one backslash)", "\"invalid\\escape\"", false),
                Arguments.of("3 backslashes ", "\\\\\\", false),
                Arguments.of("apostrophe without escape","\"ae1\'a\"",false),
                Arguments.of("two quotes at the front", "\"\"dkdkl", false)

        );
    }

    /**
     * Asserts that the input matches the given pattern. This method doesn't do
     * much now, but you will see this concept in future assignments.
     */
    private static void test(String input, Pattern pattern, boolean success) {
        Assertions.assertEquals(success, pattern.matcher(input).matches());
    }

}
