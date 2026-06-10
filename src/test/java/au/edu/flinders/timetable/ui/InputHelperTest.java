package au.edu.flinders.timetable.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class InputHelperTest {

    private final InputHelper input = new InputHelper();

    private Scanner scanner(String text) {
        return new Scanner(text);
    }

    @Test
    @DisplayName("TC 9.01 readLine returns normal input")
    @Tag("Hans")
    @Tag("Core")
    void readLineReturnsNormalInput() {
        String result = input.readLine(scanner("hello world\n"), "Prompt: ");

        assertEquals("hello world", result);
    }

    @Test
    @DisplayName("TC 9.02 readLine with q throws EarlyExitException")
    @Tag("Hans")
    @Tag("Critical")
    void readLineWithQThrowsEarlyExitException() {
        assertThrows(EarlyExitException.class,
                () -> input.readLine(scanner("q\n"), "Prompt: "));
    }

    @Test
    @DisplayName("TC 9.03 readInt accepts minimum and maximum boundaries")
    @Tag("Hans")
    @Tag("Core")
    void readIntAcceptsMinimumAndMaximumBoundaries() {
        int minResult = input.readInt(scanner("1\n"), "Number: ", 1, 5);
        int maxResult = input.readInt(scanner("5\n"), "Number: ", 1, 5);

        assertAll(
                () -> assertEquals(1, minResult),
                () -> assertEquals(5, maxResult)
        );
    }

    @Test
    @DisplayName("TC 9.04 readInt re-prompts after non-numeric input")
    @Tag("Hans")
    @Tag("Core")
    void readIntRepromptsAfterNonNumericInput() {
        int result = input.readInt(scanner("abc\n3\n"), "Number: ", 1, 5);

        assertEquals(3, result);
    }

    @Test
    @DisplayName("TC 9.05 readInt re-prompts after out-of-range input")
    @Tag("Hans")
    @Tag("Core")
    void readIntRepromptsAfterOutOfRangeInput() {
        int result = input.readInt(scanner("9\n4\n"), "Number: ", 1, 5);

        assertEquals(4, result);
    }

    @Test
    @DisplayName("TC 9.06 readBoolean accepts y and n case-insensitively")
    @Tag("Hans")
    @Tag("Core")
    void readBooleanAcceptsYAndNCaseInsensitively() {
        boolean yes = input.readBoolean(scanner("Y\n"), "Confirm");
        boolean no = input.readBoolean(scanner("N\n"), "Confirm");

        assertAll(
                () -> assertTrue(yes),
                () -> assertFalse(no)
        );
    }

    @Test
    @DisplayName("TC 9.07 readBoolean re-prompts after invalid answer")
    @Tag("Hans")
    @Tag("Core")
    void readBooleanRepromptsAfterInvalidAnswer() {
        boolean result = input.readBoolean(scanner("maybe\ny\n"), "Confirm");

        assertTrue(result);
    }

    @Test
    @DisplayName("TC 9.08 readNonBlank trims input and rejects blank lines")
    @Tag("Hans")
    @Tag("Core")
    void readNonBlankTrimsInputAndRejectsBlankLines() {
        String result = input.readNonBlank(scanner("   \n  COMP1001  \n"), "Code: ");

        assertEquals("COMP1001", result);
    }

    @Test
    @DisplayName("TC 9.09 readNonBlank with q throws EarlyExitException")
    @Tag("Hans")
    @Tag("Critical")
    void readNonBlankWithQThrowsEarlyExitException() {
        assertThrows(EarlyExitException.class,
                () -> input.readNonBlank(scanner("q\n"), "Code: "));
    }
}
