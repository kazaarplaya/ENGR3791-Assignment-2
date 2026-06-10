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
    @DisplayName("showAll – empty repository prints warning")
    @Tag("ClassController")
    @Tag("Thomas")
    @Tag("Critical")
    void showAll_emptyRepo_printsWarning() {
        controller("").showAll();
        assertTrue(out().contains("No classes have been imported yet."));
    }

    @Test
    @Order(2)
    @DisplayName("showAll – classes present outputs course code")
    @Tag("ClassController")
    @Tag("Thomas")
    @Tag("Critical")
    void showAll_withClasses_outputsCourseCode() {
        classRepo.save(entry("COMP1001-LEC-1-MON-0900-01Mar", "COMP1001"));
        controller("").showAll();
        assertTrue(out().contains("COMP1001"));
    }

    @Test
    @Order(3)
    @DisplayName("showAll – matching topic name appears in browse list")
    @Tag("ClassController")
    @Tag("Thomas")
    @Tag("Core")
    void showAll_withMatchingTopic_showsTopicName() {
        classRepo.save(entry("COMP1001-LEC-1-MON-0900-01Mar", "COMP1001"));
        topicRepo.save(topic("COMP1001", "Programming"));
        controller("").showAll();
        assertTrue(out().contains("Programming"));
    }

    @Test
    @Order(4)
    @DisplayName("showAll – null topicRepository produces empty topic map without crashing")
    @Tag("ClassController")
    @Tag("Thomas")
    @Tag("Core")
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
    @DisplayName("viewAllDetailed – empty repository prints warning")
    @Tag("ClassController")
    @Tag("Thomas")
    @Tag("Critical")
    void viewAllDetailed_emptyRepo_printsWarning() {
        controller("").viewAllDetailed();
        assertTrue(out().contains("No classes have been imported yet."));
    }

    @Test
    @Order(6)
    @DisplayName("viewAllDetailed – classes present outputs course code")
    @Tag("ClassController")
    @Tag("Thomas")
    @Tag("Critical")
    void viewAllDetailed_withClasses_outputsCourseCode() {
        classRepo.save(entry("COMP1001-LEC-1-MON-0900-01Mar", "COMP1001"));
        controller("").viewAllDetailed();
        assertTrue(out().contains("COMP1001"));
    }

    @Test
    @Order(7)
    @DisplayName("viewAllDetailed – topic details shown when topic is present")
    @Tag("ClassController")
    @Tag("Thomas")
    @Tag("Core")
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
    @DisplayName("viewAll – null topicRepository prints warning")
    @Tag("ClassController")
    @Tag("Thomas")
    @Tag("Core")
    void viewAll_nullTopicRepo_printsWarning() {
        controllerNullTopicRepo("").viewAll();
        assertTrue(out().contains("No course data available."));
    }

    @Test
    @Order(9)
    @DisplayName("viewAll – empty topic repository prints warning")
    @Tag("ClassController")
    @Tag("Thomas")
    @Tag("Critical")
    void viewAll_emptyTopicRepo_printsWarning() {
        controller("").viewAll();
        assertTrue(out().contains("No courses have been imported yet."));
    }

    @Test
    @Order(10)
    @DisplayName("viewAll – topics present prints numbered list with count")
    @Tag("ClassController")
    @Tag("Thomas")
    @Tag("Critical")
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
    @DisplayName("viewAll – courses sorted alphabetically by course code")
    @Tag("ClassController")
    @Tag("Thomas")
    @Tag("Additional")
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
    @DisplayName("search(String) – matching keyword shows results")
    @Tag("ClassController")
    @Tag("Thomas")
    @Tag("Critical")
    void searchKeyword_matchFound_showsResults() {
        classRepo.save(entry("COMP1001-LEC-1-MON-0900-01Mar", "COMP1001"));
        controller("").search("COMP1001");
        assertTrue(out().contains("COMP1001"));
    }

    @Test
    @Order(13)
    @DisplayName("search(String) – no match reports 0 found")
    @Tag("ClassController")
    @Tag("Thomas")
    @Tag("Critical")
    void searchKeyword_noMatch_reportsZeroFound() {
        controller("").search("ZZZZ9999");
        assertTrue(out().contains("0 found"));
    }

    @Test
    @Order(14)
    @DisplayName("search(String) – empty query returns all classes")
    @Tag("ClassController")
    @Tag("Thomas")
    @Tag("Core")
    void searchKeyword_emptyQuery_returnsAllClasses() {
        classRepo.save(entry("COMP1001-LEC-1-MON-0900-01Mar", "COMP1001"));
        classRepo.save(entry("MATH1001-LEC-1-TUE-1000-01Mar", "MATH1001"));
        controller("").search("");
        assertTrue(out().contains("2 found"));
    }

    @Test
    @Order(15)
    @DisplayName("search(String) – result count matches number of stored matching classes")
    @Tag("ClassController")
    @Tag("Thomas")
    @Tag("Additional")
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
    @DisplayName("search() interactive – all blank, empty repo prints no-match warning")
    @Tag("ClassController")
    @Tag("Thomas")
    @Tag("Core")
    void searchInteractive_allBlank_emptyRepo_noMatchWarning() {
        controller("\n\n\n\n\n\n\n\n").search();
        assertTrue(out().contains("No classes matched those criteria."));
    }

    @Test
    @Order(17)
    @DisplayName("search() interactive – matching courseCode criterion shows results")
    @Tag("ClassController")
    @Tag("Thomas")
    @Tag("Critical")
    void searchInteractive_courseCodeCriterion_showsResults() {
        classRepo.save(entry("COMP1001-LEC-1-MON-0900-01Mar", "COMP1001"));
        // courseCode=COMP1001, rest blank
        controller("COMP1001\n\n\n\n\n\n\n\n").search();
        assertTrue(out().contains("COMP1001"));
    }

    @Test
    @Order(18)
    @DisplayName("search() interactive – no matching results prints warning")
    @Tag("ClassController")
    @Tag("Thomas")
    @Tag("Core")
    void searchInteractive_noMatch_printsWarning() {
        classRepo.save(entry("COMP1001-LEC-1-MON-0900-01Mar", "COMP1001"));
        // courseCode=MATH9999 — no match
        controller("MATH9999\n\n\n\n\n\n\n\n").search();
        assertTrue(out().contains("No classes matched those criteria."));
    }

    @Test
    @Order(19)
    @DisplayName("search() interactive – invalid start time warns and skips field")
    @Tag("ClassController")
    @Tag("Thomas")
    @Tag("Core")
    void searchInteractive_invalidStartTime_warnsAndSkipsField() {
        controller("\n\n\n\n\n\nNOT_A_TIME\n\n").search();
        assertTrue(out().contains("Invalid start time"));
    }

    @Test
    @Order(20)
    @DisplayName("search() interactive – valid start time filters correctly")
    @Tag("ClassController")
    @Tag("Thomas")
    @Tag("Core")
    void searchInteractive_validStartTime_filtersResults() {
        classRepo.save(entry("COMP1001-LEC-1-MON-0900-01Mar", "COMP1001"));
        // all blank except startTime=09:00
        controller("\n\n\n\n\n\n09:00\n\n").search();
        assertTrue(out().contains("COMP1001"));
    }

    @Test
    @Order(21)
    @DisplayName("search() interactive – invalid semester warns and skips field")
    @Tag("ClassController")
    @Tag("Thomas")
    @Tag("Core")
    void searchInteractive_invalidSemester_warnsAndSkipsField() {
        controller("\n\n\n\n\n\n\nABC\n").search();
        assertTrue(out().contains("Invalid semester"));
    }

    @Test
    @Order(22)
    @DisplayName("search() interactive – valid semester filters correctly")
    @Tag("ClassController")
    @Tag("Thomas")
    @Tag("Additional")
    void searchInteractive_validSemester_filtersResults() {
        classRepo.save(entry("COMP1001-LEC-1-MON-0900-01Mar", "COMP1001"));
        topicRepo.save(topic("COMP1001", "Computer Programming 1"));
        // all blank except semester=1
        controller("\n\n\n\n\n\n\n1\n").search();
        assertTrue(out().contains("COMP1001"));
    }


    @Test
    @Order(23)
    @DisplayName("deleteClass – unknown ID prints error")
    @Tag("ClassController")
    @Tag("Thomas")
    @Tag("Critical")
    void deleteClass_unknownId_printsError() {
        controller("DOES-NOT-EXIST\n").deleteClass();
        assertTrue(out().contains("not found"));
    }

    @Test
    @Order(24)
    @DisplayName("deleteClass – confirmed removes class from repository")
    @Tag("ClassController")
    @Tag("Thomas")
    @Tag("Critical")
    void deleteClass_confirmed_removesClass() {
        classRepo.save(entry("COMP1001-LEC-1-MON-0900-01Mar", "COMP1001"));
        controller("COMP1001-LEC-1-MON-0900-01Mar\ny\n").deleteClass();
        assertTrue(classService.getClassById("COMP1001-LEC-1-MON-0900-01Mar").isEmpty());
    }

    @Test
    @Order(25)
    @DisplayName("deleteClass – confirmed prints success message")
    @Tag("ClassController")
    @Tag("Thomas")
    @Tag("Core")
    void deleteClass_confirmed_printsSuccess() {
        classRepo.save(entry("COMP1001-LEC-1-MON-0900-01Mar", "COMP1001"));
        controller("COMP1001-LEC-1-MON-0900-01Mar\ny\n").deleteClass();
        assertTrue(out().contains("deleted"));
    }

    @Test
    @Order(26)
    @DisplayName("deleteClass – cancelled keeps class in repository")
    @Tag("ClassController")
    @Tag("Thomas")
    @Tag("Critical")
    void deleteClass_cancelled_keepsClass() {
        classRepo.save(entry("COMP1001-LEC-1-MON-0900-01Mar", "COMP1001"));
        controller("COMP1001-LEC-1-MON-0900-01Mar\nn\n").deleteClass();
        assertTrue(classService.getClassById("COMP1001-LEC-1-MON-0900-01Mar").isPresent());
    }

    @Test
    @Order(27)
    @DisplayName("deleteClass – cancelled prints cancelled message")
    @Tag("ClassController")
    @Tag("Thomas")
    @Tag("Additional")
    void deleteClass_cancelled_printsCancelledMessage() {
        classRepo.save(entry("COMP1001-LEC-1-MON-0900-01Mar", "COMP1001"));
        controller("COMP1001-LEC-1-MON-0900-01Mar\nn\n").deleteClass();
        assertTrue(out().contains("cancelled"));
    }


    @Test
    @Order(28)
    @DisplayName("editClass – unknown ID prints error")
    @Tag("ClassController")
    @Tag("Thomas")
    @Tag("Critical")
    void editClass_unknownId_printsError() {
        controller("DOES-NOT-EXIST\n").editClass();
        assertTrue(out().contains("not found"));
    }

    @Test
    @Order(29)
    @DisplayName("editClass – all blank inputs keeps all original field values")
    @Tag("ClassController")
    @Tag("Thomas")
    @Tag("Core")
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
    @DisplayName("editClass – new building and room saved correctly")
    @Tag("ClassController")
    @Tag("Thomas")
    @Tag("Critical")
    void editClass_newBuildingAndRoom_savedCorrectly() {
        classRepo.save(entry("COMP1001-LEC-1-MON-0900-01Mar", "COMP1001"));
        controller("COMP1001-LEC-1-MON-0900-01Mar\nBuilding2\nR999\n\n\n\ny\n").editClass();
        ClassEntry stored = classService.getClassById("COMP1001-LEC-1-MON-0900-01Mar").get();
        assertEquals("Building2", stored.getBuilding());
        assertEquals("R999",      stored.getRoom());
    }

    @Test
    @Order(31)
    @DisplayName("editClass – valid new start and end times saved correctly")
    @Tag("ClassController")
    @Tag("Thomas")
    @Tag("Critical")
    void editClass_validNewTimes_savedCorrectly() {
        classRepo.save(entry("COMP1001-LEC-1-MON-0900-01Mar", "COMP1001"));
        controller("COMP1001-LEC-1-MON-0900-01Mar\n\n\n14:00\n16:00\n\ny\n").editClass();
        ClassEntry stored = classService.getClassById("COMP1001-LEC-1-MON-0900-01Mar").get();
        assertEquals(LocalTime.of(14, 0), stored.getStartTime());
        assertEquals(LocalTime.of(16, 0), stored.getEndTime());
    }

    @Test
    @Order(32)
    @DisplayName("editClass – invalid start time warns and keeps original")
    @Tag("ClassController")
    @Tag("Thomas")
    @Tag("Core")
    void editClass_invalidStartTime_warnsAndKeepsOriginal() {
        classRepo.save(entry("COMP1001-LEC-1-MON-0900-01Mar", "COMP1001"));
        controller("COMP1001-LEC-1-MON-0900-01Mar\n\n\nBAD_TIME\n\n\ny\n").editClass();
        ClassEntry stored = classService.getClassById("COMP1001-LEC-1-MON-0900-01Mar").get();
        assertEquals(LocalTime.of(9, 0), stored.getStartTime());
        assertTrue(out().contains("Invalid time"));
    }

    @Test
    @Order(33)
    @DisplayName("editClass – invalid end time warns and keeps original")
    @Tag("ClassController")
    @Tag("Thomas")
    @Tag("Core")
    void editClass_invalidEndTime_warnsAndKeepsOriginal() {
        classRepo.save(entry("COMP1001-LEC-1-MON-0900-01Mar", "COMP1001"));
        controller("COMP1001-LEC-1-MON-0900-01Mar\n\n\n\nBAD_TIME\n\ny\n").editClass();
        ClassEntry stored = classService.getClassById("COMP1001-LEC-1-MON-0900-01Mar").get();
        assertEquals(LocalTime.of(11, 0), stored.getEndTime());
        assertTrue(out().contains("Invalid time"));
    }

    @Test
    @Order(34)
    @DisplayName("editClass – valid new day saved correctly")
    @Tag("ClassController")
    @Tag("Thomas")
    @Tag("Core")
    void editClass_validNewDay_savedCorrectly() {
        classRepo.save(entry("COMP1001-LEC-1-MON-0900-01Mar", "COMP1001"));
        controller("COMP1001-LEC-1-MON-0900-01Mar\n\n\n\n\nFRIDAY\ny\n").editClass();
        ClassEntry stored = classService.getClassById("COMP1001-LEC-1-MON-0900-01Mar").get();
        assertEquals(DayOfWeek.FRIDAY, stored.getDay());
    }

    @Test
    @Order(35)
    @DisplayName("editClass – invalid day warns and keeps original")
    @Tag("ClassController")
    @Tag("Thomas")
    @Tag("Core")
    void editClass_invalidDay_warnsAndKeepsOriginal() {
        classRepo.save(entry("COMP1001-LEC-1-MON-0900-01Mar", "COMP1001"));
        controller("COMP1001-LEC-1-MON-0900-01Mar\n\n\n\n\nBADDAY\ny\n").editClass();
        ClassEntry stored = classService.getClassById("COMP1001-LEC-1-MON-0900-01Mar").get();
        assertEquals(DayOfWeek.MONDAY, stored.getDay());
        assertTrue(out().contains("Invalid day"));
    }

    @Test
    @Order(36)
    @DisplayName("editClass – save confirmed prints success message")
    @Tag("ClassController")
    @Tag("Thomas")
    @Tag("Core")
    void editClass_saveConfirmed_printsSuccess() {
        classRepo.save(entry("COMP1001-LEC-1-MON-0900-01Mar", "COMP1001"));
        controller("COMP1001-LEC-1-MON-0900-01Mar\n\n\n\n\n\ny\n").editClass();
        assertTrue(out().contains("updated"));
    }

    @Test
    @Order(37)
    @DisplayName("editClass – cancelled does not persist any changes")
    @Tag("ClassController")
    @Tag("Thomas")
    @Tag("Critical")
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
    @DisplayName("editClass – cancelled prints cancelled message")
    @Tag("ClassController")
    @Tag("Thomas")
    @Tag("Additional")
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
    @DisplayName("deleteClass – exit sentinel 'q' at ID prompt throws EarlyExitException")
    @Tag("ClassController")
    @Tag("Thomas")
    @Tag("Additional")
    void deleteClass_exitSentinel_throwsEarlyExitException() {
        assertThrows(EarlyExitException.class,
                () -> controller("q\n").deleteClass());
    }

    @Test
    @Order(40)
    @DisplayName("editClass – exit sentinel 'q' at ID prompt throws EarlyExitException")
    @Tag("ClassController")
    @Tag("Thomas")
    @Tag("Additional")
    void editClass_exitSentinel_throwsEarlyExitException() {
        assertThrows(EarlyExitException.class,
                () -> controller("q\n").editClass());
    }
}
