package au.edu.flinders.timetable.controller;

import au.edu.flinders.timetable.model.ClassEntry;
import au.edu.flinders.timetable.model.Topic;
import au.edu.flinders.timetable.repository.ClassRepository;
import au.edu.flinders.timetable.repository.TopicRepository;
import au.edu.flinders.timetable.service.ClassService;
import au.edu.flinders.timetable.service.SearchService;
import au.edu.flinders.timetable.ui.ConsoleView;
import au.edu.flinders.timetable.ui.EarlyExitException;
import au.edu.flinders.timetable.ui.InputHelper;
import org.junit.jupiter.api.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;


@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ClassControllerTest {

    // Shared state
    private ClassRepository  classRepo;
    private TopicRepository  topicRepo;
    private ClassService     classService;
    private SearchService    searchService;
    private ConsoleView      view;

    private ByteArrayOutputStream outContent;
    private PrintStream           originalOut;

    // Lifecycle

    @BeforeEach
    void setUp() {
        classRepo     = new ClassRepository();
        topicRepo     = new TopicRepository();
        classService  = new ClassService(classRepo, topicRepo);
        searchService = new SearchService(classRepo, topicRepo);
        view          = new ConsoleView();

        originalOut = System.out;
        outContent  = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    // Builder helpers

    private ClassController controller(String simulatedInput) {
        Scanner sc = new Scanner(new ByteArrayInputStream(simulatedInput.getBytes()));
        return new ClassController(classService, searchService, topicRepo,
                view, new InputHelper(), sc);
    }

    private ClassController controllerNullTopicRepo(String simulatedInput) {
        Scanner sc = new Scanner(new ByteArrayInputStream(simulatedInput.getBytes()));
        return new ClassController(classService, searchService, null,
                view, new InputHelper(), sc);
    }

    /** Creates a minimal ClassEntry with sensible defaults. */
    private ClassEntry entry(String classId, String courseCode) {
        return new ClassEntry(classId, "Lecture", null,
                LocalTime.of(9, 0), LocalTime.of(11, 0),
                DayOfWeek.MONDAY, "T1", "G42", courseCode,
                "In Person", 1, 1, "01 Mar", "01 Mar");
    }

    private Topic topic(String courseCode, String name) {
        return new Topic(courseCode, name, "Bedford Park", 1, "In Person", 10);
    }

    private String out() {
        return outContent.toString();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // showAll()
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(1)
    @DisplayName("TC 11.01 - showAll – empty repository prints warning")
    @Tag("Thomas")
    @Tag("Critical")
    @Tag("ClassController")
    void showAll_emptyRepo_printsWarning() {
        controller("").showAll();
        assertTrue(out().contains("No classes have been imported yet."));
    }

    @Test
    @Order(2)
    @DisplayName("TC 11.02 - showAll – classes present outputs course code")
    @Tag("Thomas")
    @Tag("Critical")
    @Tag("ClassController")
    void showAll_withClasses_outputsCourseCode() {
        classRepo.save(entry("COMP1001-LEC-1-MON-0900-01Mar", "COMP1001"));
        controller("").showAll();
        assertTrue(out().contains("COMP1001"));
    }

    @Test
    @Order(3)
    @DisplayName("TC 11.03 - showAll – matching topic name appears in browse list")
    @Tag("Thomas")
    @Tag("Core")
    @Tag("ClassController")
    void showAll_withMatchingTopic_showsTopicName() {
        classRepo.save(entry("COMP1001-LEC-1-MON-0900-01Mar", "COMP1001"));
        topicRepo.save(topic("COMP1001", "Programming"));
        controller("").showAll();
        assertTrue(out().contains("Programming"));
    }

    @Test
    @Order(4)
    @DisplayName("TC 11.04 - showAll – null topicRepository produces empty topic map without crashing")
    @Tag("Thomas")
    @Tag("Core")
    @Tag("ClassController")
    void showAll_nullTopicRepo_doesNotCrash() {
        classRepo.save(entry("COMP1001-LEC-1-MON-0900-01Mar", "COMP1001"));
        assertDoesNotThrow(() -> controllerNullTopicRepo("").showAll());
        assertTrue(out().contains("COMP1001"));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // viewAllDetailed()
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(5)
    @DisplayName("TC 11.05 - viewAllDetailed – empty repository prints warning")
    @Tag("Thomas")
    @Tag("Critical")
    @Tag("ClassController")
    void viewAllDetailed_emptyRepo_printsWarning() {
        controller("").viewAllDetailed();
        assertTrue(out().contains("No classes have been imported yet."));
    }

    @Test
    @Order(6)
    @DisplayName("TC 11.06 - viewAllDetailed – classes present outputs course code")
    @Tag("Thomas")
    @Tag("Critical")
    @Tag("ClassController")
    void viewAllDetailed_withClasses_outputsCourseCode() {
        classRepo.save(entry("COMP1001-LEC-1-MON-0900-01Mar", "COMP1001"));
        controller("").viewAllDetailed();
        assertTrue(out().contains("COMP1001"));
    }

    @Test
    @Order(7)
    @DisplayName("TC 11.07 - viewAllDetailed – topic details shown when topic is present")
    @Tag("Thomas")
    @Tag("Core")
    @Tag("ClassController")
    void viewAllDetailed_withTopic_showsTopicDetails() {
        classRepo.save(entry("COMP1001-LEC-1-MON-0900-01Mar", "COMP1001"));
        topicRepo.save(topic("COMP1001", "Computer Programming 1"));
        controller("").viewAllDetailed();
        assertTrue(out().contains("Bedford Park"));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // viewAll()
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(8)
    @DisplayName("TC 11.08 - viewAll – null topicRepository prints warning")
    @Tag("Thomas")
    @Tag("Core")
    @Tag("ClassController")
    void viewAll_nullTopicRepo_printsWarning() {
        controllerNullTopicRepo("").viewAll();
        assertTrue(out().contains("No course data available."));
    }

    @Test
    @Order(9)
    @DisplayName("TC 11.09 - viewAll – empty topic repository prints warning")
    @Tag("Thomas")
    @Tag("Critical")
    @Tag("ClassController")
    void viewAll_emptyTopicRepo_printsWarning() {
        controller("").viewAll();
        assertTrue(out().contains("No courses have been imported yet."));
    }

    @Test
    @Order(10)
    @DisplayName("TC 11.10 - viewAll – topics present prints numbered list with count")
    @Tag("Thomas")
    @Tag("Critical")
    @Tag("ClassController")
    void viewAll_withTopics_printsNumberedList() {
        topicRepo.save(topic("COMP1001", "Computer Programming 1"));
        topicRepo.save(topic("MATH1001", "Mathematics 1"));
        controller("").viewAll();
        String o = out();
        assertTrue(o.contains("COMP1001"));
        assertTrue(o.contains("MATH1001"));
        assertTrue(o.contains("2 course(s) imported"));
    }

    @Test
    @Order(11)
    @DisplayName("TC 11.11 - viewAll – courses sorted alphabetically by course code")
    @Tag("Thomas")
    @Tag("Additional")
    @Tag("ClassController")
    void viewAll_multipleTopics_sortedAlphabetically() {
        topicRepo.save(topic("MATH1001", "Mathematics 1"));
        topicRepo.save(topic("COMP1001", "Computer Programming 1"));
        controller("").viewAll();
        String o = out();
        assertTrue(o.indexOf("COMP1001") < o.indexOf("MATH1001"));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // search(String) — keyword search
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(12)
    @DisplayName("TC 11.12 - search(String) – matching keyword shows results")
    @Tag("Thomas")
    @Tag("Critical")
    @Tag("ClassController")
    void searchKeyword_matchFound_showsResults() {
        classRepo.save(entry("COMP1001-LEC-1-MON-0900-01Mar", "COMP1001"));
        controller("").search("COMP1001");
        assertTrue(out().contains("COMP1001"));
    }

    @Test
    @Order(13)
    @DisplayName("TC 11.13 - search(String) – no match reports 0 found")
    @Tag("Thomas")
    @Tag("Critical")
    @Tag("ClassController")
    void searchKeyword_noMatch_reportsZeroFound() {
        controller("").search("ZZZZ9999");
        assertTrue(out().contains("0 found"));
    }

    @Test
    @Order(14)
    @DisplayName("TC 11.14 - search(String) – empty query returns all classes")
    @Tag("Thomas")
    @Tag("Core")
    @Tag("ClassController")
    void searchKeyword_emptyQuery_returnsAllClasses() {
        classRepo.save(entry("COMP1001-LEC-1-MON-0900-01Mar", "COMP1001"));
        classRepo.save(entry("MATH1001-LEC-1-TUE-1000-01Mar", "MATH1001"));
        controller("").search("");
        assertTrue(out().contains("2 found"));
    }

    @Test
    @Order(15)
    @DisplayName("TC 11.15 - search(String) – result count matches number of stored matching classes")
    @Tag("Thomas")
    @Tag("Additional")
    @Tag("ClassController")
    void searchKeyword_multipleMatches_correctCount() {
        classRepo.save(entry("COMP1001-LEC-1-MON-0900-01Mar", "COMP1001"));
        classRepo.save(entry("COMP1001-LAB-1-WED-1400-01Mar", "COMP1001"));
        controller("").search("COMP1001");
        assertTrue(out().contains("2 found"));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // search() — interactive structured search
    //
    // The method issues 8 promptOptional() calls in order:
    //   1. courseCode  2. topicName  3. classType  4. attendanceMode
    //   5. campus      6. day        7. startTime  8. semester
    // Blank input (empty line) skips that criterion.
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(16)
    @DisplayName("TC 11.16 - search() interactive – all blank, empty repo prints no-match warning")
    @Tag("Thomas")
    @Tag("Core")
    @Tag("ClassController")
    void searchInteractive_allBlank_emptyRepo_noMatchWarning() {
        controller("\n\n\n\n\n\n\n\n").search();
        assertTrue(out().contains("No classes matched those criteria."));
    }

    @Test
    @Order(17)
    @DisplayName("TC 11.17 - search() interactive – matching courseCode criterion shows results")
    @Tag("Thomas")
    @Tag("Critical")
    @Tag("ClassController")
    void searchInteractive_courseCodeCriterion_showsResults() {
        classRepo.save(entry("COMP1001-LEC-1-MON-0900-01Mar", "COMP1001"));
        // courseCode=COMP1001, rest blank
        controller("COMP1001\n\n\n\n\n\n\n\n").search();
        assertTrue(out().contains("COMP1001"));
    }

    @Test
    @Order(18)
    @DisplayName("TC 11.18 - search() interactive – no matching results prints warning")
    @Tag("Thomas")
    @Tag("Core")
    @Tag("ClassController")
    void searchInteractive_noMatch_printsWarning() {
        classRepo.save(entry("COMP1001-LEC-1-MON-0900-01Mar", "COMP1001"));
        // courseCode=MATH9999 — no match
        controller("MATH9999\n\n\n\n\n\n\n\n").search();
        assertTrue(out().contains("No classes matched those criteria."));
    }

    @Test
    @Order(19)
    @DisplayName("TC 11.19 - search() interactive – invalid start time warns and skips field")
    @Tag("Thomas")
    @Tag("Core")
    @Tag("ClassController")
    void searchInteractive_invalidStartTime_warnsAndSkipsField() {
        controller("\n\n\n\n\n\nNOT_A_TIME\n\n").search();
        assertTrue(out().contains("Invalid start time"));
    }

    @Test
    @Order(20)
    @DisplayName("TC 11.20 - search() interactive – valid start time filters correctly")
    @Tag("Thomas")
    @Tag("Core")
    @Tag("ClassController")
    void searchInteractive_validStartTime_filtersResults() {
        classRepo.save(entry("COMP1001-LEC-1-MON-0900-01Mar", "COMP1001"));
        // all blank except startTime=09:00
        controller("\n\n\n\n\n\n09:00\n\n").search();
        assertTrue(out().contains("COMP1001"));
    }

    @Test
    @Order(21)
    @DisplayName("TC 11.21 - search() interactive – invalid semester warns and skips field")
    @Tag("Thomas")
    @Tag("Core")
    @Tag("ClassController")
    void searchInteractive_invalidSemester_warnsAndSkipsField() {
        controller("\n\n\n\n\n\n\nABC\n").search();
        assertTrue(out().contains("Invalid semester"));
    }

    @Test
    @Order(22)
    @DisplayName("TC 11.22 - search() interactive – valid semester filters correctly")
    @Tag("Thomas")
    @Tag("Additional")
    @Tag("ClassController")
    void searchInteractive_validSemester_filtersResults() {
        classRepo.save(entry("COMP1001-LEC-1-MON-0900-01Mar", "COMP1001"));
        topicRepo.save(topic("COMP1001", "Computer Programming 1"));
        // all blank except semester=1
        controller("\n\n\n\n\n\n\n1\n").search();
        assertTrue(out().contains("COMP1001"));
    }


    @Test
    @Order(23)
    @DisplayName("TC 11.23 - deleteClass – unknown ID prints error")
    @Tag("Thomas")
    @Tag("Critical")
    @Tag("ClassController")
    void deleteClass_unknownId_printsError() {
        controller("DOES-NOT-EXIST\n").deleteClass();
        assertTrue(out().contains("not found"));
    }

    @Test
    @Order(24)
    @DisplayName("TC 11.24 - deleteClass – confirmed removes class from repository")
    @Tag("Thomas")
    @Tag("Critical")
    @Tag("ClassController")
    void deleteClass_confirmed_removesClass() {
        classRepo.save(entry("COMP1001-LEC-1-MON-0900-01Mar", "COMP1001"));
        controller("COMP1001-LEC-1-MON-0900-01Mar\ny\n").deleteClass();
        assertTrue(classService.getClassById("COMP1001-LEC-1-MON-0900-01Mar").isEmpty());
    }

    @Test
    @Order(25)
    @DisplayName("TC 11.25 - deleteClass – confirmed prints success message")
    @Tag("Thomas")
    @Tag("Core")
    @Tag("ClassController")
    void deleteClass_confirmed_printsSuccess() {
        classRepo.save(entry("COMP1001-LEC-1-MON-0900-01Mar", "COMP1001"));
        controller("COMP1001-LEC-1-MON-0900-01Mar\ny\n").deleteClass();
        assertTrue(out().contains("deleted"));
    }

    @Test
    @Order(26)
    @DisplayName("TC 11.26 - deleteClass – cancelled keeps class in repository")
    @Tag("Thomas")
    @Tag("Critical")
    @Tag("ClassController")
    void deleteClass_cancelled_keepsClass() {
        classRepo.save(entry("COMP1001-LEC-1-MON-0900-01Mar", "COMP1001"));
        controller("COMP1001-LEC-1-MON-0900-01Mar\nn\n").deleteClass();
        assertTrue(classService.getClassById("COMP1001-LEC-1-MON-0900-01Mar").isPresent());
    }

    @Test
    @Order(27)
    @DisplayName("TC 11.27 - deleteClass – cancelled prints cancelled message")
    @Tag("Thomas")
    @Tag("Additional")
    @Tag("ClassController")
    void deleteClass_cancelled_printsCancelledMessage() {
        classRepo.save(entry("COMP1001-LEC-1-MON-0900-01Mar", "COMP1001"));
        controller("COMP1001-LEC-1-MON-0900-01Mar\nn\n").deleteClass();
        assertTrue(out().contains("cancelled"));
    }


    @Test
    @Order(28)
    @DisplayName("TC 11.28 - editClass – unknown ID prints error")
    @Tag("Thomas")
    @Tag("Critical")
    @Tag("ClassController")
    void editClass_unknownId_printsError() {
        controller("DOES-NOT-EXIST\n").editClass();
        assertTrue(out().contains("not found"));
    }

    @Test
    @Order(29)
    @DisplayName("TC 11.29 - editClass – all blank inputs keeps all original field values")
    @Tag("Thomas")
    @Tag("Core")
    @Tag("ClassController")
    void editClass_allBlankInputs_keepsOriginalValues() {
        classRepo.save(entry("COMP1001-LEC-1-MON-0900-01Mar", "COMP1001"));
        controller("COMP1001-LEC-1-MON-0900-01Mar\n\n\n\n\n\ny\n").editClass();
        ClassEntry stored = classService.getClassById("COMP1001-LEC-1-MON-0900-01Mar").get();
        assertEquals(LocalTime.of(9, 0),    stored.getStartTime());
        assertEquals(LocalTime.of(11, 0),   stored.getEndTime());
        assertEquals(DayOfWeek.MONDAY,      stored.getDay());
        assertEquals("T1",                  stored.getBuilding());
        assertEquals("G42",                 stored.getRoom());
    }

    @Test
    @Order(30)
    @DisplayName("TC 11.30 - editClass – new building and room saved correctly")
    @Tag("Thomas")
    @Tag("Critical")
    @Tag("ClassController")
    void editClass_newBuildingAndRoom_savedCorrectly() {
        classRepo.save(entry("COMP1001-LEC-1-MON-0900-01Mar", "COMP1001"));
        controller("COMP1001-LEC-1-MON-0900-01Mar\nBuilding2\nR999\n\n\n\ny\n").editClass();
        ClassEntry stored = classService.getClassById("COMP1001-LEC-1-MON-0900-01Mar").get();
        assertEquals("Building2", stored.getBuilding());
        assertEquals("R999",      stored.getRoom());
    }

    @Test
    @Order(31)
    @DisplayName("TC 11.31 - editClass – valid new start and end times saved correctly")
    @Tag("Thomas")
    @Tag("Critical")
    @Tag("ClassController")
    void editClass_validNewTimes_savedCorrectly() {
        classRepo.save(entry("COMP1001-LEC-1-MON-0900-01Mar", "COMP1001"));
        controller("COMP1001-LEC-1-MON-0900-01Mar\n\n\n14:00\n16:00\n\ny\n").editClass();
        ClassEntry stored = classService.getClassById("COMP1001-LEC-1-MON-0900-01Mar").get();
        assertEquals(LocalTime.of(14, 0), stored.getStartTime());
        assertEquals(LocalTime.of(16, 0), stored.getEndTime());
    }

    @Test
    @Order(32)
    @DisplayName("TC 11.32 - editClass – invalid start time warns and keeps original")
    @Tag("Thomas")
    @Tag("Core")
    @Tag("ClassController")
    void editClass_invalidStartTime_warnsAndKeepsOriginal() {
        classRepo.save(entry("COMP1001-LEC-1-MON-0900-01Mar", "COMP1001"));
        controller("COMP1001-LEC-1-MON-0900-01Mar\n\n\nBAD_TIME\n\n\ny\n").editClass();
        ClassEntry stored = classService.getClassById("COMP1001-LEC-1-MON-0900-01Mar").get();
        assertEquals(LocalTime.of(9, 0), stored.getStartTime());
        assertTrue(out().contains("Invalid time"));
    }

    @Test
    @Order(33)
    @DisplayName("TC 11.33 - editClass – invalid end time warns and keeps original")
    @Tag("Thomas")
    @Tag("Core")
    @Tag("ClassController")
    void editClass_invalidEndTime_warnsAndKeepsOriginal() {
        classRepo.save(entry("COMP1001-LEC-1-MON-0900-01Mar", "COMP1001"));
        controller("COMP1001-LEC-1-MON-0900-01Mar\n\n\n\nBAD_TIME\n\ny\n").editClass();
        ClassEntry stored = classService.getClassById("COMP1001-LEC-1-MON-0900-01Mar").get();
        assertEquals(LocalTime.of(11, 0), stored.getEndTime());
        assertTrue(out().contains("Invalid time"));
    }

    @Test
    @Order(34)
    @DisplayName("TC 11.34 - editClass – valid new day saved correctly")
    @Tag("Thomas")
    @Tag("Core")
    @Tag("ClassController")
    void editClass_validNewDay_savedCorrectly() {
        classRepo.save(entry("COMP1001-LEC-1-MON-0900-01Mar", "COMP1001"));
        controller("COMP1001-LEC-1-MON-0900-01Mar\n\n\n\n\nFRIDAY\ny\n").editClass();
        ClassEntry stored = classService.getClassById("COMP1001-LEC-1-MON-0900-01Mar").get();
        assertEquals(DayOfWeek.FRIDAY, stored.getDay());
    }

    @Test
    @Order(35)
    @DisplayName("TC 11.35 - editClass – invalid day warns and keeps original")
    @Tag("Thomas")
    @Tag("Core")
    @Tag("ClassController")
    void editClass_invalidDay_warnsAndKeepsOriginal() {
        classRepo.save(entry("COMP1001-LEC-1-MON-0900-01Mar", "COMP1001"));
        controller("COMP1001-LEC-1-MON-0900-01Mar\n\n\n\n\nBADDAY\ny\n").editClass();
        ClassEntry stored = classService.getClassById("COMP1001-LEC-1-MON-0900-01Mar").get();
        assertEquals(DayOfWeek.MONDAY, stored.getDay());
        assertTrue(out().contains("Invalid day"));
    }

    @Test
    @Order(36)
    @DisplayName("TC 11.36 - editClass – save confirmed prints success message")
    @Tag("Thomas")
    @Tag("Core")
    @Tag("ClassController")
    void editClass_saveConfirmed_printsSuccess() {
        classRepo.save(entry("COMP1001-LEC-1-MON-0900-01Mar", "COMP1001"));
        controller("COMP1001-LEC-1-MON-0900-01Mar\n\n\n\n\n\ny\n").editClass();
        assertTrue(out().contains("updated"));
    }

    @Test
    @Order(37)
    @DisplayName("TC 11.37 - editClass – cancelled does not persist any changes")
    @Tag("Thomas")
    @Tag("Critical")
    @Tag("ClassController")
    void editClass_cancelled_doesNotPersistChanges() {
        classRepo.save(entry("COMP1001-LEC-1-MON-0900-01Mar", "COMP1001"));
        // Provide new values for every field but cancel
        controller("COMP1001-LEC-1-MON-0900-01Mar\nNewBuilding\nR999\n14:00\n16:00\nFRIDAY\nn\n").editClass();
        ClassEntry stored = classService.getClassById("COMP1001-LEC-1-MON-0900-01Mar").get();
        assertEquals("T1",                stored.getBuilding());
        assertEquals("G42",               stored.getRoom());
        assertEquals(LocalTime.of(9, 0),  stored.getStartTime());
        assertEquals(LocalTime.of(11, 0), stored.getEndTime());
        assertEquals(DayOfWeek.MONDAY,    stored.getDay());
    }

    @Test
    @Order(38)
    @DisplayName("TC 11.38 - editClass – cancelled prints cancelled message")
    @Tag("Thomas")
    @Tag("Additional")
    @Tag("ClassController")
    void editClass_cancelled_printsCancelledMessage() {
        classRepo.save(entry("COMP1001-LEC-1-MON-0900-01Mar", "COMP1001"));
        controller("COMP1001-LEC-1-MON-0900-01Mar\n\n\n\n\n\nn\n").editClass();
        assertTrue(out().contains("cancelled"));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // EarlyExitException — typing "q" exits any prompt immediately
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(39)
    @DisplayName("TC 11.39 - deleteClass – exit sentinel 'q' at ID prompt throws EarlyExitException")
    @Tag("Thomas")
    @Tag("Additional")
    @Tag("ClassController")
    void deleteClass_exitSentinel_throwsEarlyExitException() {
        assertThrows(EarlyExitException.class,
                () -> controller("q\n").deleteClass());
    }

    @Test
    @Order(40)
    @DisplayName("TC 11.40 - editClass – exit sentinel 'q' at ID prompt throws EarlyExitException")
    @Tag("Thomas")
    @Tag("Additional")
    @Tag("ClassController")
    void editClass_exitSentinel_throwsEarlyExitException() {
        assertThrows(EarlyExitException.class,
                () -> controller("q\n").editClass());
    }
}
