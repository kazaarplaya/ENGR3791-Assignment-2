package au.edu.flinders.timetable;

import org.junit.jupiter.api.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

class MainTest {

    private final PrintStream originalOut = System.out;
    private final PrintStream originalIn  = System.out;
    private final ByteArrayOutputStream capturedOut = new ByteArrayOutputStream();

    @BeforeEach
    void setUp() {
        System.setOut(new PrintStream(capturedOut));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setIn(System.in);
        capturedOut.reset();
    }

    @Test
    @Order(1)
    @DisplayName("TC 17.01 - Main starts and exits cleanly when user enters exit command")
    @Tag("Luke")
    @Tag("Core")
    void mainStartsAndExitsCleanlyWhenUserEntersExitCommand() {
        // "0" or whichever input triggers exit in MenuController
        System.setIn(new ByteArrayInputStream("0\n".getBytes()));

        assertDoesNotThrow(() -> Main.main(new String[]{}));
    }
}
