package plc.homework;

import com.sun.org.apache.xpath.internal.Arg;
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
                Arguments.of("One Letter", "b@ufl.edu",true),
                Arguments.of("No domain name", "abc@.com", true),
                Arguments.of("One letter each", "c@c.edu", true),
                Arguments.of("Two letter end", "aol@google.de", true),
                Arguments.of("Underscores", "_name_123@gmail.com",true),
                Arguments.of("Dashes", "-me--@gmail.com", true),
                Arguments.of("Numbers first", "123abc@00.gov", true),
                Arguments.of("Missing Domain Dot", "missingdot@gmailcom", false),
                Arguments.of("Symbols", "symbols#$%@gmail.com", false),
                Arguments.of("No email name", "@gmail.com", false),
                Arguments.of("No @ symbol", "mynamegmail.com", false),
                Arguments.of("Symbol in domain", "symbols@g%mail.com", false),
                Arguments.of("Underscore in both", "my_name@g_mail.com", false),
                Arguments.of("Uppercase after dot", "me@yahoo.Com", false),
                Arguments.of("Too large after dot", "mail@yahoo.comm", false),
                Arguments.of("Too small after dot", "mail@yahoo.c", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testEvenStringsRegex(String test, String input, boolean success) {
        test(input, Regex.EVEN_STRINGS, success);
    }

    public static Stream<Arguments> testEvenStringsRegex() {
        return Stream.of(
                //what has ten letters and starts with gas?
                Arguments.of("10 Characters", "automobile", true),
                Arguments.of("14 Characters", "i<3pancakes10!", true),
                Arguments.of("16 Characters", "24_3059ajgnmeotl", true),
                Arguments.of("18 Characters", "PL@!0mce###10amcit", true),
                Arguments.of("20 Characters", "PL@!0mce###10amcit20", true),
                Arguments.of("Wierd chars", "!@#$%^&*()`~';[]|,./", true),
                Arguments.of("6 Characters", "6chars", false),
                Arguments.of("13 Characters", "i<3pancakes9!", false),
                Arguments.of("22 Characters", "22characterslongggg22", false),
                Arguments.of("19 Characters", "19characterslongg19", false),
                Arguments.of("9 characters", "9chars!!!", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testIntegerListRegex(String test, String input, boolean success) {
        test(input, Regex.INTEGER_LIST, success);
    }

    public static Stream<Arguments> testIntegerListRegex() {
        return Stream.of(
                Arguments.of("Single Element", "[1]", true),
                Arguments.of("Multiple Elements", "[1,2,3]", true),
                Arguments.of("Empty", "[]", true),
                Arguments.of("Multiple digits", "[11, 12, 13]", true),
                Arguments.of("Variable digits", "[1, 22, 333]", true),
                Arguments.of("Variable spaces", "[22,30, 10, 20,2]", true),
                Arguments.of("Missing Brackets", "1,2,3", false),
                Arguments.of("Missing Commas", "[1 2 3]", false),
                Arguments.of("Comma at end", "[1,2,3,]", false),
                Arguments.of("Two commas", "1,,2,3", false),
                Arguments.of("Non digit", "[1,2,3a]", false),
                Arguments.of("Non digit2", "[1c,2,3]", false),
                Arguments.of("Non digit3", "[b]", false),
                Arguments.of("Non digit", "[0]", false),
                Arguments.of("Leading zeroes", "[01,02, 03]", false),
                Arguments.of("Bracket in mid", "1,[02, 03]", false),
                Arguments.of("Bracket in mid", "[1,02, 0]3", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testNumberRegex(String test, String input, boolean success) {
        test(input, Regex.NUMBER, success); //TODO
    }

    public static Stream<Arguments> testNumberRegex() {
        return Stream.of(
                Arguments.of("Single Digit", "1", true),
                Arguments.of("Single Digit with sign", "+1", true),
                Arguments.of("Many digits", "204", true),
                Arguments.of("single digit and decimal", "1.0", true),
                Arguments.of("many digits and decimal digits", "234.2040", true),
                Arguments.of("many digits with positive", "+2044", true),
                Arguments.of("trailing zeroes with sign", "-20.000", true),
                Arguments.of("many digits with negative", "-20", true),
                Arguments.of("sign with leading zeroes", "-00001", true),
                Arguments.of("sign with leading zero and decimal", "-0.1", true),
                Arguments.of("Nothing", "", false),
                Arguments.of("Leading decimal", ".5", false),
                Arguments.of("Ending decimal", "5.", false),
                Arguments.of("Multiple decimals", "1.02.200", false),
                Arguments.of("Multiple decimals2", "102..200", false),
                Arguments.of("Two signs", "+-1", false)
        ); //TODO
    }

    @ParameterizedTest
    @MethodSource
    public void testStringRegex(String test, String input, boolean success) {
        test(input, Regex.STRING, success); //TODO
    }

    public static Stream<Arguments> testStringRegex() {
        return Stream.of(
                Arguments.of("single letter", "\"o\"", true),
                Arguments.of("two letters", "\"bo\"", true),
                Arguments.of("many letters", "\"okg93939\"", true),
                Arguments.of("escape character", "\"o\\t00s\"", true),
                Arguments.of("dbl backslash", "\"paf\\\\mrm\"", true),
                Arguments.of("empty string", "\"\"", true),
                Arguments.of("only 2 backslashes", "\"\\\\\"", true),
                Arguments.of("special characters", "\"mgm00#%2\"", true),
                Arguments.of("escapes and whitespace", "\"mgm\\b   oooe\\n\\\\nnm\\'mm\\\"%2\"", true),
                Arguments.of("just whitespace", "\"         \"", true),
                Arguments.of("1 backslash", "\"\\\"", false),
                Arguments.of("single quote", "\"\'\"", false),
                Arguments.of("letters before quote", "mmdg\"gooe1\"", false),
                Arguments.of("letters after quote", "\"fdg,p\"gg", false),
                Arguments.of("single quotes", "\'hello\'", false),
                Arguments.of("invalid escape", "\"omgm\\qomom\"", false),
                Arguments.of("number escape", "\"02003mmmfm\\00\"", false),
                Arguments.of("missing quote", "\"mgm0", false)
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
