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

    @Test
    @DisplayName("TC 10.10 – printBanner outputs ASCII art box with STUDENT text")
    @Tag("TC 10.10")
    @Tag("Luke")
    @Tag("Core")
    void printBannerOutputsAsciiArtBox() {
        view.printBanner();

        String result = printedOutput();
        assertAll(
                () -> assertTrue(result.contains("T I M E T A B L E")),
                () -> assertTrue(result.contains("Flinders University")),
                () -> assertTrue(result.contains("╔")),
                () -> assertTrue(result.contains("╚"))
        );
    }

    @Test
    @DisplayName("TC 10.11 – printClassList truncates entry longer than 60 chars")
    @Tag("TC 10.11")
    @Tag("Luke")
    @Tag("Core")
    void printClassListTruncatesLongEntry() {
        ClassEntry entry = new ClassEntry(
                "VERYLONGCOURSECODE-LEC-1-MON-0900-01Mar",
                "VeryLongTypeName",
                null,
                LocalTime.of(9, 0), LocalTime.of(11, 0),
                DayOfWeek.MONDAY,
                "VeryLongBuildingNameHere", "VeryLongRoomIdentifier",
                "VERYLONGCOURSECODE", "In person", 1, 1, "01 Mar", "01 Mar");

        view.printClassList(List.of(entry));

        assertTrue(printedOutput().contains("..."));
    }

    @Test
    @DisplayName("TC 10.12 – printViewList shows warning for empty class list")
    @Tag("TC 10.12")
    @Tag("Luke")
    @Tag("Core")
    void printViewListShowsWarningForEmptyList() {
        view.printViewList(List.of(), Map.of());

        assertTrue(printedOutput().contains("No classes to display"));
    }

    @Test
    @DisplayName("TC 10.13 – printViewList renders topic name, campus and semester when topic present")
    @Tag("TC 10.13")
    @Tag("Luke")
    @Tag("Core")
    void printViewListRenderTopicDetailsWhenTopicPresent() {
        ClassEntry entry = makeEntry("C1", "COMP1001", "Lecture",
                DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0));
        Topic topic = new Topic("COMP1001", "Computing Fundamentals", "Bedford Park", 1, "Internal", 2);

        view.printViewList(List.of(entry), Map.of("COMP1001", topic));

        String result = printedOutput();
        assertAll(
                () -> assertTrue(result.contains("Computing Fundamentals")),
                () -> assertTrue(result.contains("Bedford Park")),
                () -> assertTrue(result.contains("Sem 1"))
        );
    }

    @Test
    @DisplayName("TC 10.14 – printViewList renders blank room without comma separator")
    @Tag("TC 10.14")
    @Tag("Luke")
    @Tag("Core")
    void printViewListBlankRoomOmitsCommaSeparator() {
        ClassEntry entry = new ClassEntry(
                "C1", "Lecture", null,
                LocalTime.of(9, 0), LocalTime.of(11, 0),
                DayOfWeek.MONDAY, "Registry", "",
                "COMP1001", "In person", 1, 1, "01 Mar", "01 Mar");

        view.printViewList(List.of(entry), Map.of());

        String result = printedOutput();
        assertTrue(result.contains("Registry"));
        assertFalse(result.contains("Registry,"));
    }

    @Test
    @DisplayName("TC 10.15 – printViewList renders separator between multiple entries")
    @Tag("TC 10.15")
    @Tag("Luke")
    @Tag("Core")
    void printViewListRendersSeparatorBetweenMultipleEntries() {
        ClassEntry e1 = makeEntry("C1", "COMP1001", "Lecture",
                DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0));
        ClassEntry e2 = makeEntry("C2", "MATH1001", "Tutorial",
                DayOfWeek.TUESDAY, LocalTime.of(10, 0), LocalTime.of(12, 0));

        view.printViewList(List.of(e1, e2), Map.of());

        String result = printedOutput();
        assertTrue(result.contains("├"));
    }

    @Test
    @DisplayName("TC 10.16 – printTimetable shows yes for overlap and preference flags")
    @Tag("TC 10.16")
    @Tag("Luke")
    @Tag("Core")
    void printTimetableShowsYesForOverlapAndPreferenceFlags() {
        Timetable t = new Timetable("FlagTest", "Semester 1", true, true);

        view.printTimetable(t, List.of());

        String result = printedOutput();
        assertAll(
                () -> assertTrue(result.contains("Overlap: yes")),
                () -> assertTrue(result.contains("Prefs: yes"))
        );
    }

    @Test
    @DisplayName("TC 10.17 – printBrowseList uses empty string when topic not in map")
    @Tag("TC 10.17")
    @Tag("Luke")
    @Tag("Core")
    void printBrowseListUsesEmptyTopicWhenNotInMap() {
        ClassEntry entry = makeEntry("C1", "COMP1001", "Lecture",
                DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0));

        view.printBrowseList(List.of(entry), Map.of());

        assertTrue(printedOutput().contains("COMP1001"));
    }

    @Test
    @DisplayName("TC 10.18 – printBrowseList truncates topic name longer than 17 chars")
    @Tag("TC 10.18")
    @Tag("Luke")
    @Tag("Core")
    void printBrowseListTruncatesLongTopicName() {
        ClassEntry entry = makeEntry("C1", "COMP1001", "Lecture",
                DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0));
        Topic topic = new Topic("COMP1001", "A Very Long Topic Name Indeed", "Bedford Park", 1, "Internal", 1);

        view.printBrowseList(List.of(entry), Map.of("COMP1001", topic));

        assertTrue(printedOutput().contains("..."));
    }

    @Test
    @DisplayName("TC 10.19 – printBrowseList truncates type+group longer than 10 chars")
    @Tag("TC 10.19")
    @Tag("Luke")
    @Tag("Core")
    void printBrowseListTruncatesLongTypeGroup() {
        ClassEntry entry = new ClassEntry(
                "C1", "LaboratorySession", null,
                LocalTime.of(9, 0), LocalTime.of(11, 0),
                DayOfWeek.MONDAY, "T1", "G42",
                "COMP1001", "In person", 1, 99, "01 Mar", "01 Mar");

        view.printBrowseList(List.of(entry), Map.of());

        assertTrue(printedOutput().contains("."));
    }

    @Test
    @DisplayName("TC 10.20 – printBrowseList shows --- for null day")
    @Tag("TC 10.20")
    @Tag("Luke")
    @Tag("Core")
    void printBrowseListShowsDashesForNullDay() {
        ClassEntry entry = new ClassEntry(
                "C1", "Lecture", null,
                LocalTime.of(9, 0), LocalTime.of(11, 0),
                null, "T1", "G42",
                "COMP1001", "In person", 1, 1, "01 Mar", "01 Mar");

        view.printBrowseList(List.of(entry), Map.of());

        assertTrue(printedOutput().contains("---"));
    }

    @Test
    @DisplayName("TC 10.21 – printBrowseList shows empty time when startTime is null")
    @Tag("TC 10.21")
    @Tag("Luke")
    @Tag("Core")
    void printBrowseListShowsEmptyTimeWhenStartTimeNull() {
        ClassEntry entry = new ClassEntry(
                "C1", "Lecture", null,
                null, null,
                DayOfWeek.MONDAY, "T1", "G42",
                "COMP1001", "In person", 1, 1, "01 Mar", "01 Mar");

        view.printBrowseList(List.of(entry), Map.of());

        // No NPE and output still contains course code
        assertTrue(printedOutput().contains("COMP1001"));
    }

    @Test
    @DisplayName("TC 10.22 – printBrowseList shows empty location when building is null")
    @Tag("TC 10.22")
    @Tag("Luke")
    @Tag("Core")
    void printBrowseListShowsEmptyLocationWhenBuildingNull() {
        ClassEntry entry = new ClassEntry(
                "C1", "Lecture", null,
                LocalTime.of(9, 0), LocalTime.of(11, 0),
                DayOfWeek.MONDAY, null, null,
                "COMP1001", "In person", 1, 1, "01 Mar", "01 Mar");

        assertDoesNotThrow(() -> view.printBrowseList(List.of(entry), Map.of()));
    }

    @Test
    @DisplayName("TC 10.23 – printBrowseList truncates location longer than 9 chars")
    @Tag("TC 10.23")
    @Tag("Luke")
    @Tag("Core")
    void printBrowseListTruncatesLongLocation() {
        ClassEntry entry = new ClassEntry(
                "C1", "Lecture", null,
                LocalTime.of(9, 0), LocalTime.of(11, 0),
                DayOfWeek.MONDAY, "VerylongBuildingName", "R101",
                "COMP1001", "In person", 1, 1, "01 Mar", "01 Mar");

        view.printBrowseList(List.of(entry), Map.of());

        assertTrue(printedOutput().contains(".."));
    }

    @Test
    @DisplayName("TC 10.24 – printTimetable shows --- for class with null day in legend")
    @Tag("TC 10.24")
    @Tag("Luke")
    @Tag("Core")
    void printTimetableShowsDashesForNullDayInLegend() {
        ClassEntry entry = new ClassEntry(
                "C1", "Lecture", null,
                LocalTime.of(9, 0), LocalTime.of(11, 0),
                null, "T1", "G42",
                "COMP1001", "In person", 1, 1, "01 Mar", "01 Mar");
        Timetable t = makeTimetable("T", "C1");

        view.printTimetable(t, List.of(entry));

        assertTrue(printedOutput().contains("---"));
    }

    @Test
    @DisplayName("TC 10.25 – printTimetable shows empty fields for null startTime endTime building room in legend")
    @Tag("TC 10.25")
    @Tag("Luke")
    @Tag("Core")
    void printTimetableHandlesNullTimesBuildingRoomInLegend() {
        ClassEntry entry = new ClassEntry(
                "C1", "Lecture", null,
                null, null,
                DayOfWeek.MONDAY, null, null,
                "COMP1001", "In person", 1, 1, "01 Mar", "01 Mar");
        Timetable t = makeTimetable("T", "C1");

        assertDoesNotThrow(() -> view.printTimetable(t, List.of(entry)));
    }

    @Test
    @DisplayName("TC 10.26 – printTimetable excludes class with null times from grid slots")
    @Tag("TC 10.26")
    @Tag("Luke")
    @Tag("Core")
    void printTimetableExcludesNullTimeClassFromGridSlots() {
        ClassEntry nullTimes = new ClassEntry(
                "C1", "Lecture", null,
                null, null,
                DayOfWeek.MONDAY, "T1", "G42",
                "COMP1001", "In person", 1, 1, "01 Mar", "01 Mar");
        Timetable t = makeTimetable("T", "C1");

        view.printTimetable(t, List.of(nullTimes));

        // Grid renders — no clash, no class label in slots
        assertFalse(printedOutput().contains("!! CLASH !!"));
    }
}
