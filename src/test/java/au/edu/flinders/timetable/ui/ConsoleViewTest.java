package au.edu.flinders.timetable.ui;

import au.edu.flinders.timetable.model.ClassEntry;
import au.edu.flinders.timetable.model.Timetable;
import au.edu.flinders.timetable.model.Topic;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConsoleViewTest {

    private final ConsoleView view = new ConsoleView();
    private final PrintStream originalOut = System.out;
    private ByteArrayOutputStream output;

    @BeforeEach
    void setUp() {
        output = new ByteArrayOutputStream();
        System.setOut(new PrintStream(output));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    private String printedOutput() {
        return output.toString();
    }

    private ClassEntry makeEntry(String classId, String courseCode, String type,
                                 DayOfWeek day, LocalTime startTime, LocalTime endTime) {
        return new ClassEntry(
                classId,
                type,
                null,
                startTime,
                endTime,
                day,
                "T1",
                "G42",
                courseCode,
                "In person",
                1,
                1,
                "01 Mar",
                "01 Mar"
        );
    }

    private Timetable makeTimetable(String name, String... classIds) {
        Timetable timetable = new Timetable(name, "Semester 1", false, false);
        for (String classId : classIds) {
            timetable.addClass(classId);
        }
        return timetable;
    }

    @Test
    @DisplayName("TC 10.01 printMenu renders all supplied options")
    @Tag("Hans")
    @Tag("Core")
    void printMenuRendersAllSuppliedOptions() {
        view.printMenu(new String[] {"Import Data", "Classes", "Exit"});

        String result = printedOutput();
        assertAll(
                () -> assertTrue(result.contains("MAIN MENU")),
                () -> assertTrue(result.contains("Import Data")),
                () -> assertTrue(result.contains("Classes")),
                () -> assertTrue(result.contains("Exit"))
        );
    }

    @Test
    @DisplayName("TC 10.02 printSuccess, printError and printWarning include message text")
    @Tag("Hans")
    @Tag("Core")
    void printNotificationsIncludeMessageText() {
        view.printSuccess("Saved");
        view.printError("Failed");
        view.printWarning("Careful");

        String result = printedOutput();
        assertAll(
                () -> assertTrue(result.contains("Saved")),
                () -> assertTrue(result.contains("Failed")),
                () -> assertTrue(result.contains("Careful"))
        );
    }

    @Test
    @DisplayName("TC 10.03 printClassList renders no-results state for empty list")
    @Tag("Hans")
    @Tag("Core")
    void printClassListRendersNoResultsStateForEmptyList() {
        view.printClassList(List.of());

        assertTrue(printedOutput().contains("no results found"));
    }

    @Test
    @DisplayName("TC 10.04 printClassList renders compact class details")
    @Tag("Hans")
    @Tag("Core")
    void printClassListRendersCompactClassDetails() {
        ClassEntry entry = makeEntry(
                "C1",
                "COMP1001",
                "Lecture",
                DayOfWeek.MONDAY,
                LocalTime.of(9, 0),
                LocalTime.of(11, 0)
        );

        view.printClassList(List.of(entry));

        String result = printedOutput();
        assertAll(
                () -> assertTrue(result.contains("CLASS RESULTS")),
                () -> assertTrue(result.contains("COMP1001")),
                () -> assertTrue(result.contains("Lecture")),
                () -> assertTrue(result.contains("T1 G42"))
        );
    }

    @Test
    @DisplayName("TC 10.05 printBrowseList renders topic names from topic map")
    @Tag("Hans")
    @Tag("Core")
    void printBrowseListRendersTopicNamesFromTopicMap() {
        ClassEntry entry = makeEntry(
                "C1",
                "COMP1001",
                "Tutorial",
                DayOfWeek.TUESDAY,
                LocalTime.of(12, 0),
                LocalTime.of(13, 0)
        );
        Topic topic = new Topic("COMP1001", "Programming", "Bedford Park", 1, "Internal", 1);

        view.printBrowseList(List.of(entry), Map.of("COMP1001", topic));

        String result = printedOutput();
        assertAll(
                () -> assertTrue(result.contains("CLASSES")),
                () -> assertTrue(result.contains("COMP1001")),
                () -> assertTrue(result.contains("Programming")),
                () -> assertTrue(result.contains("Tutorial"))
        );
    }

    @Test
    @DisplayName("TC 10.06 printBrowseList handles empty class list with warning")
    @Tag("Hans")
    @Tag("Core")
    void printBrowseListHandlesEmptyClassListWithWarning() {
        view.printBrowseList(List.of(), Map.of());

        assertTrue(printedOutput().contains("No classes to display"));
    }

    @Test
    @DisplayName("TC 10.07 printViewList renders unknown topic when topic map has no entry")
    @Tag("Hans")
    @Tag("Core")
    void printViewListRendersUnknownTopicWhenTopicMapHasNoEntry() {
        ClassEntry entry = makeEntry(
                "C1",
                "COMP1001",
                "Lecture",
                DayOfWeek.WEDNESDAY,
                LocalTime.of(14, 0),
                LocalTime.of(16, 0)
        );

        view.printViewList(List.of(entry), Map.of());

        String result = printedOutput();
        assertAll(
                () -> assertTrue(result.contains("CLASS DETAIL VIEW")),
                () -> assertTrue(result.contains("COMP1001")),
                () -> assertTrue(result.contains("(unknown)"))
        );
    }

    @Test
    @DisplayName("TC 10.08 printTimetable renders empty timetable grid without class legend")
    @Tag("Hans")
    @Tag("Core")
    void printTimetableRendersEmptyTimetableGridWithoutClassLegend() {
        Timetable timetable = makeTimetable("Empty Timetable");

        view.printTimetable(timetable, List.of());

        String result = printedOutput();
        assertAll(
                () -> assertTrue(result.contains("Empty Timetable")),
                () -> assertTrue(result.contains("Classes: 0")),
                () -> assertTrue(result.contains("Legend")),
                () -> assertFalse(result.contains("Included Classes"))
        );
    }

    @Test
    @DisplayName("TC 10.09 printTimetable marks overlapping classes with clash indicator")
    @Tag("Hans")
    @Tag("Critical")
    void printTimetableMarksOverlappingClassesWithClashIndicator() {
        ClassEntry first = makeEntry(
                "C1",
                "COMP1001",
                "Lecture",
                DayOfWeek.MONDAY,
                LocalTime.of(9, 0),
                LocalTime.of(11, 0)
        );
        ClassEntry second = makeEntry(
                "C2",
                "MATH1001",
                "Tutorial",
                DayOfWeek.MONDAY,
                LocalTime.of(10, 0),
                LocalTime.of(12, 0)
        );
        Timetable timetable = makeTimetable("Clash Timetable", "C1", "C2");

        view.printTimetable(timetable, List.of(first, second));

        String result = printedOutput();
        assertAll(
                () -> assertTrue(result.contains("Clash Timetable")),
                () -> assertTrue(result.contains("!! CLASH !!")),
                () -> assertTrue(result.contains("Included Classes")),
                () -> assertTrue(result.contains("COMP1001")),
                () -> assertTrue(result.contains("MATH1001"))
        );
    }
}
