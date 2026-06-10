package au.edu.flinders.timetable.controller;

import au.edu.flinders.timetable.model.ClassEntry;
import au.edu.flinders.timetable.model.Timetable;
import au.edu.flinders.timetable.model.User;
import au.edu.flinders.timetable.repository.ClassRepository;
import au.edu.flinders.timetable.repository.PreferenceRepository;
import au.edu.flinders.timetable.repository.TimetableRepository;
import au.edu.flinders.timetable.repository.TopicRepository;
import au.edu.flinders.timetable.service.*;
import au.edu.flinders.timetable.ui.ConsoleView;
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
class TimetableControllerTest {

    private final PrintStream originalOut = System.out;
    private final ByteArrayOutputStream capturedOut = new ByteArrayOutputStream();

    private ClassRepository classRepository;
    private TopicRepository topicRepository;
    private PreferenceRepository preferenceRepository;
    private TimetableRepository timetableRepository;
    private TimetableService timetableService;
    private TimetableGeneratorService generatorService;
    private CSVExportService exportService;
    private ClassService classService;
    private ConsoleView view;

    @BeforeEach
    void setUp() {
        System.setOut(new PrintStream(capturedOut));
        classRepository      = new ClassRepository();
        topicRepository      = new TopicRepository();
        preferenceRepository = new PreferenceRepository();
        timetableRepository  = new TimetableRepository();
        timetableService     = new TimetableService(timetableRepository, classRepository);
        generatorService     = new TimetableGeneratorService(classRepository, topicRepository,
                preferenceRepository, timetableService);
        exportService        = new CSVExportService(classRepository, topicRepository);
        classService         = new ClassService(classRepository, topicRepository);
        view                 = new ConsoleView();
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        capturedOut.reset();
    }

    // Helper functions

    private TimetableController buildController(String input) {
        Scanner sc = new Scanner(new ByteArrayInputStream(input.getBytes()));
        InputHelper inputHelper = new InputHelper();
        return new TimetableController(generatorService, timetableService, exportService,
                classRepository, topicRepository, classService, view, inputHelper, sc);
    }

    private Timetable saveTimetable(String name) {
        Timetable t = new Timetable(name, "Semester 1", false, false);
        timetableRepository.save(t);
        return t;
    }

    private ClassEntry buildClass(String classId, String courseCode) {
        return new ClassEntry(classId, "Lecture", null,
                LocalTime.of(9, 0), LocalTime.of(11, 0), DayOfWeek.MONDAY,
                "Registry Building", "R101", courseCode, "In person", 1, 1,
                "01 Mar", "01 Jun");
    }


    @Test
    @Order(1)
    @DisplayName("TC X.01 – viewAll prints all saved timetable names")
    @Tag("TC X.01")
    @Tag("Luke")
    @Tag("Core")
    void viewAllPrintsAllSavedTimetableNames() {
        saveTimetable("Timetable A");
        saveTimetable("Timetable B");

        TimetableController controller = buildController("");
        controller.viewAll();

        String output = capturedOut.toString();
        assertAll(
                () -> assertTrue(output.contains("Timetable A")),
                () -> assertTrue(output.contains("Timetable B"))
        );
    }


    @Test
    @Order(2)
    @DisplayName("TC X.02 – viewAll prints none message when no timetables saved")
    @Tag("TC X.02")
    @Tag("Luke")
    @Tag("Core")
    void viewAllPrintsNoneMessageWhenNoTimetablesSaved() {
        TimetableController controller = buildController("");
        controller.viewAll();

        assertTrue(capturedOut.toString().contains("none"));
    }


    @Test
    @Order(3)
    @DisplayName("TC X.03 – view displays timetable when selected")
    @Tag("TC X.03")
    @Tag("Luke")
    @Tag("Core")
    void viewDisplaysTimetableWhenSelected() {
        saveTimetable("My Timetable");

        // Input: select timetable 1
        TimetableController controller = buildController("1\n");
        controller.view();

        assertTrue(capturedOut.toString().contains("My Timetable"));
    }


    @Test
    @Order(4)
    @DisplayName("TC X.04 – view prints warning when no timetables exist")
    @Tag("TC X.04")
    @Tag("Luke")
    @Tag("Core")
    void viewPrintsWarningWhenNoTimetablesExist() {
        TimetableController controller = buildController("");
        controller.view();

        assertTrue(capturedOut.toString().toLowerCase().contains("no saved timetables"));
    }


    @Test
    @Order(5)
    @DisplayName("TC X.05 – delete removes timetable after confirmation")
    @Tag("TC X.05")
    @Tag("Luke")
    @Tag("Core")
    void deletesRemovesTimetableAfterConfirmation() {
        saveTimetable("To Delete");

        // Input: select 1, confirm yes
        TimetableController controller = buildController("1\ny\n");
        controller.delete();

        assertFalse(timetableRepository.exists("To Delete"),
                "Timetable should be removed after confirmed delete");
    }


    @Test
    @Order(6)
    @DisplayName("TC X.06 – delete cancels when user declines confirmation")
    @Tag("TC X.06")
    @Tag("Luke")
    @Tag("Core")
    void deleteCancelsWhenUserDeclinesConfirmation() {
        saveTimetable("Keep Me");

        // Input: select 1, confirm no
        TimetableController controller = buildController("1\nn\n");
        controller.delete();

        assertTrue(timetableRepository.exists("Keep Me"),
                "Timetable should remain when delete is cancelled");
    }


    @Test
    @Order(7)
    @DisplayName("TC X.07 – delete prints warning when no timetables exist")
    @Tag("TC X.07")
    @Tag("Luke")
    @Tag("Core")
    void deletePrintsWarningWhenNoTimetablesExist() {
        TimetableController controller = buildController("");
        controller.delete();

        assertTrue(capturedOut.toString().toLowerCase().contains("no saved timetables"));
    }


    @Test
    @Order(8)
    @DisplayName("TC X.08 – export writes timetable to file")
    @Tag("TC X.08")
    @Tag("Luke")
    @Tag("Core")
    void exportWritesTimetableToFile() {
        saveTimetable("Export Me");

        // Input: select 1, blank export name (use timetable name), blank path (use default)
        TimetableController controller = buildController("1\n\n\n");
        assertDoesNotThrow(() -> controller.export());

        assertTrue(capturedOut.toString().toLowerCase().contains("exported"));
    }


    @Test
    @Order(9)
    @DisplayName("TC X.09 – export prints warning when no timetables exist")
    @Tag("TC X.09")
    @Tag("Luke")
    @Tag("Core")
    void exportPrintsWarningWhenNoTimetablesExist() {
        TimetableController controller = buildController("");
        controller.export();

        assertTrue(capturedOut.toString().toLowerCase().contains("no saved timetables"));
    }


    @Test
    @Order(10)
    @DisplayName("TC X.10 – export uses custom name when provided")
    @Tag("TC X.10")
    @Tag("Luke")
    @Tag("Core")
    void exportUsesCustomNameWhenProvided() {
        saveTimetable("My Timetable");

        // Input: select 1, custom export name, blank path
        TimetableController controller = buildController("1\nCustomExport\n\n");
        controller.export();

        assertTrue(capturedOut.toString().contains("CustomExport.csv"));
    }


    @Test
    @Order(11)
    @DisplayName("TC X.11 – export uses custom path when provided")
    @Tag("TC X.11")
    @Tag("Luke")
    @Tag("Core")
    void exportUsesCustomPathWhenProvided() {
        saveTimetable("My Timetable");

        // Input: select 1, blank name, custom path
        TimetableController controller = buildController("1\n\ncustom/path.csv\n");
        controller.export();

        assertTrue(capturedOut.toString().contains("custom/path.csv"));
    }


    @Test
    @Order(12)
    @DisplayName("TC X.12 – delete cancels when user enters n")
    @Tag("TC X.12")
    @Tag("Luke")
    @Tag("Core")
    void deleteCancelsWhenUserEntersN() {
        saveTimetable("Keep Me");

        // Input: select 1, n
        TimetableController controller = buildController("1\nn\n");
        controller.delete();

        assertTrue(timetableRepository.exists("Keep Me"));
        assertTrue(capturedOut.toString().contains("cancelled"));
    }


    @Test
    @Order(13)
    @DisplayName("TC X.13 – delete removes timetable when user enters y")
    @Tag("TC X.13")
    @Tag("Luke")
    @Tag("Core")
    void deleteRemovesTimetableWhenUserEntersY() {
        saveTimetable("To Delete");

        // Input: select 1, y
        TimetableController controller = buildController("1\ny\n");
        controller.delete();

        assertFalse(timetableRepository.exists("To Delete"));
        assertTrue(capturedOut.toString().toLowerCase().contains("deleted"));
    }

// ── TC X.14 ───────────────────────────────────────────────────────────────

    @Test
    @Order(14)
    @DisplayName("TC X.14 – generate prints warning when no classes selected")
    @Tag("TC X.14")
    @Tag("Luke")
    @Tag("Core")
    void generatePrintsWarningWhenNoClassesSelected() {
        // Input: done (no enrolments), then all prompts answered
        // semester: Enter (both), overlap: n, prefs: n, name: blank, confirm: n
        TimetableController controller = buildController("done\n\nn\nn\n\nn\n");
        User user = new User("student-001", "Test Student");

        controller.generate(user);

        assertTrue(capturedOut.toString().toLowerCase().contains("no classes were selected"));
    }


    @Test
    @Order(15)
    @DisplayName("TC X.15 – generate cancels when user declines confirmation")
    @Tag("TC X.15")
    @Tag("Luke")
    @Tag("Core")
    void generateCancelsWhenUserDeclinesConfirmation() {
        ClassEntry lecture = buildClass("COMP1000-LEC-MON-0900", "COMP1000");
        classRepository.save(lecture);
        topicRepository.save(new au.edu.flinders.timetable.model.Topic(
                "COMP1000", "Programming", "Bedford Park", 1, "In Person", 1));

        // Input: enrol COMP1000, done, Enter campus (any), Enter semester (both),
        // overlap: n, prefs: n, name: blank, confirm: n
        TimetableController controller = buildController(
                "COMP1000\ndone\n\n\nn\nn\n\nn\n");
        User user = new User("student-001", "Test Student");

        controller.generate(user);

        assertTrue(capturedOut.toString().contains("cancelled"));
    }


    @Test
    @Order(16)
    @DisplayName("TC X.16 – generate creates timetable when user confirms")
    @Tag("TC X.16")
    @Tag("Luke")
    @Tag("Core")
    void generateCreatesTimetableWhenUserConfirms() {
        ClassEntry lecture = buildClass("COMP1000-LEC-MON-0900", "COMP1000");
        classRepository.save(lecture);
        topicRepository.save(new au.edu.flinders.timetable.model.Topic(
                "COMP1000", "Programming", "Bedford Park", 1, "In Person", 1));

        // Input: enrol COMP1000, done, Enter campus, Enter semester,
        // overlap: n, prefs: n, name: TC16, confirm: y
        TimetableController controller = buildController(
                "COMP1000\ndone\n\n\nn\nn\nTC16\ny\n");
        User user = new User("student-001", "Test Student");

        controller.generate(user);

        assertTrue(timetableRepository.exists("TC16"),
                "Timetable should be saved after confirmed generation");
    }


    @Test
    @Order(17)
    @DisplayName("TC X.17 – generate infers City campus from Festival Tower building")
    @Tag("TC X.17")
    @Tag("Luke")
    @Tag("Core")
    void generateInfersCityCampusFromFestivalTowerBuilding() {
        ClassEntry cityClass = new ClassEntry("COMP1000-LEC-MON-0900", "Lecture", null,
                LocalTime.of(9, 0), LocalTime.of(11, 0), DayOfWeek.MONDAY,
                "Festival Tower", "R101", "COMP1000", "In person", 1, 1, "01 Mar", "01 Jun");
        classRepository.save(cityClass);
        topicRepository.save(new au.edu.flinders.timetable.model.Topic(
                "COMP1000", "Programming", "City", 1, "In Person", 1));

        // Input: enrol COMP1000, done, Enter campus, Enter semester,
        // overlap: n, prefs: n, name: TC17, confirm: y
        TimetableController controller = buildController(
                "COMP1000\ndone\n\n\nn\nn\nTC17\ny\n");
        User user = new User("student-001", "Test Student");

        controller.generate(user);

        String output = capturedOut.toString();
        assertTrue(output.contains("City") || timetableRepository.exists("TC17"));
    }


    @Test
    @Order(18)
    @DisplayName("TC X.18 – generate infers Tonsley campus from Tonsley building")
    @Tag("TC X.18")
    @Tag("Luke")
    @Tag("Core")
    void generateInfersTonslyCampusFromTonsleyBuilding() {
        ClassEntry tonsleyClass = new ClassEntry("COMP1000-LEC-MON-0900", "Lecture", null,
                LocalTime.of(9, 0), LocalTime.of(11, 0), DayOfWeek.MONDAY,
                "Tonsley T1", "R101", "COMP1000", "In person", 1, 1, "01 Mar", "01 Jun");
        classRepository.save(tonsleyClass);
        topicRepository.save(new au.edu.flinders.timetable.model.Topic(
                "COMP1000", "Programming", "Tonsley", 1, "In Person", 1));

        // Input: enrol COMP1000, done, Enter campus, Enter semester,
        // overlap: n, prefs: n, name: TC18, confirm: y
        TimetableController controller = buildController(
                "COMP1000\ndone\n\n\nn\nn\nTC18\ny\n");
        User user = new User("student-001", "Test Student");

        controller.generate(user);

        assertTrue(capturedOut.toString().contains("Tonsley") || timetableRepository.exists("TC18"));
    }


    @Test
    @Order(19)
    @DisplayName("TC X.19 – generate infers Bedford Park from unrecognised building")
    @Tag("TC X.19")
    @Tag("Luke")
    @Tag("Core")
    void generateInfersBedfordParkFromUnrecognisedBuilding() {
        ClassEntry bedfordClass = buildClass("COMP1000-LEC-MON-0900", "COMP1000");
        classRepository.save(bedfordClass);
        topicRepository.save(new au.edu.flinders.timetable.model.Topic(
                "COMP1000", "Programming", "Bedford Park", 1, "In Person", 1));

        // Input: enrol COMP1000, done, Enter campus, Enter semester,
        // overlap: n, prefs: n, name: TC19, confirm: y
        TimetableController controller = buildController(
                "COMP1000\ndone\n\n\nn\nn\nTC19\ny\n");
        User user = new User("student-001", "Test Student");

        controller.generate(user);

        assertTrue(timetableRepository.exists("TC19"));
    }


    @Test
    @Order(20)
    @DisplayName("TC X.20 – editTimetable prints warning when timetable has no classes")
    @Tag("TC X.20")
    @Tag("Luke")
    @Tag("Core")
    void editTimetablePrintsWarningWhenTimetableHasNoClasses() {
        saveTimetable("Empty Timetable");

        // Input: select 1
        TimetableController controller = buildController("1\n");
        controller.editTimetable();

        assertTrue(capturedOut.toString().toLowerCase().contains("no classes"));
    }


    @Test
    @Order(21)
    @DisplayName("TC X.21 – editTimetable prints warning when no alternatives available")
    @Tag("TC X.21")
    @Tag("Luke")
    @Tag("Core")
    void editTimetablePrintsWarningWhenNoAlternativesAvailable() {
        ClassEntry lecture = buildClass("COMP1000-LEC-MON-0900", "COMP1000");
        classRepository.save(lecture);

        Timetable t = new Timetable("One Class", "Semester 1", false, false);
        t.addClass("COMP1000-LEC-MON-0900");
        timetableRepository.save(t);

        // Input: select timetable 1, select class 1
        TimetableController controller = buildController("1\n1\n");
        controller.editTimetable();

        assertTrue(capturedOut.toString().toLowerCase().contains("no alternative"));
    }


    @Test
    @Order(22)
    @DisplayName("TC X.22 – editTimetable swaps class when confirmed")
    @Tag("TC X.22")
    @Tag("Luke")
    @Tag("Core")
    void editTimetableSwapsClassWhenConfirmed() {
        ClassEntry inst1 = new ClassEntry("COMP1000-LEC-MON-0900", "Lecture", null,
                LocalTime.of(9, 0), LocalTime.of(11, 0), DayOfWeek.MONDAY,
                "Registry Building", "R101", "COMP1000", "In person", 1, 1, "01 Mar", "01 Jun");
        ClassEntry inst2 = new ClassEntry("COMP1000-LEC-TUE-0900", "Lecture", null,
                LocalTime.of(9, 0), LocalTime.of(11, 0), DayOfWeek.TUESDAY,
                "Registry Building", "R101", "COMP1000", "In person", 1, 2, "01 Mar", "01 Jun");
        classRepository.save(inst1);
        classRepository.save(inst2);

        Timetable t = new Timetable("Swap Test", "Semester 1", false, false);
        t.addClass("COMP1000-LEC-MON-0900");
        timetableRepository.save(t);

        // Input: select timetable 1, select class 1, select replacement 1, confirm y
        TimetableController controller = buildController("1\n1\n1\ny\n");
        controller.editTimetable();

        assertTrue(capturedOut.toString().toLowerCase().contains("swapped"));
    }


    @Test
    @Order(23)
    @DisplayName("TC X.23 – editTimetable cancels swap when user declines")
    @Tag("TC X.23")
    @Tag("Luke")
    @Tag("Core")
    void editTimetableCancelsSwapWhenUserDeclines() {
        ClassEntry inst1 = new ClassEntry("COMP1000-LEC-MON-0900", "Lecture", null,
                LocalTime.of(9, 0), LocalTime.of(11, 0), DayOfWeek.MONDAY,
                "Registry Building", "R101", "COMP1000", "In person", 1, 1, "01 Mar", "01 Jun");
        ClassEntry inst2 = new ClassEntry("COMP1000-LEC-TUE-0900", "Lecture", null,
                LocalTime.of(9, 0), LocalTime.of(11, 0), DayOfWeek.TUESDAY,
                "Registry Building", "R101", "COMP1000", "In person", 1, 2, "01 Mar", "01 Jun");
        classRepository.save(inst1);
        classRepository.save(inst2);

        Timetable t = new Timetable("No Swap", "Semester 1", false, false);
        t.addClass("COMP1000-LEC-MON-0900");
        timetableRepository.save(t);

        // Input: select timetable 1, select class 1, select replacement 1, confirm n
        TimetableController controller = buildController("1\n1\n1\nn\n");
        controller.editTimetable();

        assertTrue(capturedOut.toString().contains("cancelled"));
    }

    @Test
    @Order(24)
    @DisplayName("TC X.24 – generate prints message when empty input entered during enrolment")
    @Tag("TC X.24")
    @Tag("Luke")
    @Tag("Core")
    void generatePrintsMessageWhenEmptyInputEnteredDuringEnrolment() {
        TimetableController controller = buildController("\ndone\n\nn\nn\n\nn\n");
        User user = new User("student-001", "Test Student");

        controller.generate(user);

        assertTrue(capturedOut.toString().contains("Please enter a course code"));
    }

    @Test
    @Order(25)
    @DisplayName("TC X.25 – generate prints message when out of range number entered during enrolment")
    @Tag("TC X.25")
    @Tag("Luke")
    @Tag("Core")
    void generatePrintsMessageWhenOutOfRangeNumberEnteredDuringEnrolment() {
        topicRepository.save(new au.edu.flinders.timetable.model.Topic(
                "COMP1000", "Programming", "Bedford Park", 1, "In Person", 1));

        TimetableController controller = buildController("99\ndone\n\nn\nn\n\nn\n");
        User user = new User("student-001", "Test Student");

        controller.generate(user);

        assertTrue(capturedOut.toString().contains("out of range"));
    }

    @Test
    @Order(26)
    @DisplayName("TC X.26 – generate prints message when invalid course code entered")
    @Tag("TC X.26")
    @Tag("Luke")
    @Tag("Core")
    void generatePrintsMessageWhenInvalidCourseCodeEntered() {
        TimetableController controller = buildController("INVALID999\ndone\n\nn\nn\n\nn\n");
        User user = new User("student-001", "Test Student");

        controller.generate(user);

        assertTrue(capturedOut.toString().contains("invalid or unavailable"));
    }

    @Test
    @Order(27)
    @DisplayName("TC X.27 – generate prints message when already enrolled in course")
    @Tag("TC X.27")
    @Tag("Luke")
    @Tag("Core")
    void generatePrintsMessageWhenAlreadyEnrolledInCourse() {
        ClassEntry lecture = buildClass("COMP1000-LEC-MON-0900", "COMP1000");
        classRepository.save(lecture);

        User user = new User("student-001", "Test Student");
        user.enrol("COMP1000");

        TimetableController controller = buildController("COMP1000\ndone\n\nn\nn\n\nn\n");

        controller.generate(user);

        assertTrue(capturedOut.toString().contains("Already enrolled"));
    }

    @Test
    @Order(28)
    @DisplayName("TC X.28 – generate accepts numeric course selection during enrolment")
    @Tag("TC X.28")
    @Tag("Luke")
    @Tag("Core")
    void generateAcceptsNumericCourseSelectionDuringEnrolment() {
        ClassEntry lecture = buildClass("COMP1000-LEC-MON-0900", "COMP1000");
        classRepository.save(lecture);
        topicRepository.save(new au.edu.flinders.timetable.model.Topic(
                "COMP1000", "Programming", "Bedford Park", 1, "In Person", 1));

        TimetableController controller = buildController("1\ndone\n\n\nn\nn\nTC28\ny\n");
        User user = new User("student-001", "Test Student");

        controller.generate(user);

        assertTrue(timetableRepository.exists("TC28"));
    }

    @Test
    @Order(29)
    @DisplayName("TC X.29 – generate accepts campus by number selection")
    @Tag("TC X.29")
    @Tag("Luke")
    @Tag("Core")
    void generateAcceptsCampusByNumberSelection() {
        ClassEntry lecture = buildClass("COMP1000-LEC-MON-0900", "COMP1000");
        classRepository.save(lecture);
        topicRepository.save(new au.edu.flinders.timetable.model.Topic(
                "COMP1000", "Programming", "Bedford Park", 1, "In Person", 1));

        // Input: enrol, done, select campus 1, semester Enter, overlap n, prefs n, name, confirm y
        TimetableController controller = buildController("COMP1000\ndone\n1\n\nn\nn\nTC29\ny\n");
        User user = new User("student-001", "Test Student");

        controller.generate(user);

        assertTrue(capturedOut.toString().contains("Selected:"));
    }

    @Test
    @Order(30)
    @DisplayName("TC X.30 – generate accepts campus by name selection")
    @Tag("TC X.30")
    @Tag("Luke")
    @Tag("Core")
    void generateAcceptsCampusByNameSelection() {
        ClassEntry lecture = buildClass("COMP1000-LEC-MON-0900", "COMP1000");
        classRepository.save(lecture);
        topicRepository.save(new au.edu.flinders.timetable.model.Topic(
                "COMP1000", "Programming", "Bedford Park", 1, "In Person", 1));

        // Input: enrol, done, type campus name, semester Enter, overlap n, prefs n, name, confirm y
        TimetableController controller = buildController("COMP1000\ndone\nBedford Park\n\nn\nn\nTC30\ny\n");
        User user = new User("student-001", "Test Student");

        controller.generate(user);

        assertTrue(capturedOut.toString().contains("Selected:"));
    }

    @Test
    @Order(31)
    @DisplayName("TC X.31 – generate prints message when invalid campus name entered")
    @Tag("TC X.31")
    @Tag("Luke")
    @Tag("Core")
    void generatePrintsMessageWhenInvalidCampusNameEntered() {
        ClassEntry lecture = buildClass("COMP1000-LEC-MON-0900", "COMP1000");
        classRepository.save(lecture);
        topicRepository.save(new au.edu.flinders.timetable.model.Topic(
                "COMP1000", "Programming", "Bedford Park", 1, "In Person", 1));

        // First attempt invalid campus, then Enter to skip
        TimetableController controller = buildController("COMP1000\ndone\nMars\n\n\nn\nn\nTC31\ny\n");
        User user = new User("student-001", "Test Student");

        controller.generate(user);

        assertTrue(capturedOut.toString().contains("not available"));
    }

    @Test
    @Order(32)
    @DisplayName("TC X.32 – generate prints message when out of range campus number entered")
    @Tag("TC X.32")
    @Tag("Luke")
    @Tag("Core")
    void generatePrintsMessageWhenOutOfRangeCampusNumberEntered() {
        ClassEntry lecture = buildClass("COMP1000-LEC-MON-0900", "COMP1000");
        classRepository.save(lecture);
        topicRepository.save(new au.edu.flinders.timetable.model.Topic(
                "COMP1000", "Programming", "Bedford Park", 1, "In Person", 1));

        // Enter out-of-range campus number, then Enter to skip
        TimetableController controller = buildController("COMP1000\ndone\n99\n\n\nn\nn\nTC32\ny\n");
        User user = new User("student-001", "Test Student");

        controller.generate(user);

        assertTrue(capturedOut.toString().contains("out of range"));
    }

    @Test
    @Order(33)
    @DisplayName("TC X.33 – generate selects semester one when user enters 1")
    @Tag("TC X.33")
    @Tag("Luke")
    @Tag("Core")
    void generateSelectsSemesterOneWhenUserEnters1() {
        ClassEntry lecture = buildClass("COMP1000-LEC-MON-0900", "COMP1000");
        classRepository.save(lecture);
        topicRepository.save(new au.edu.flinders.timetable.model.Topic(
                "COMP1000", "Programming", "Bedford Park", 1, "In Person", 1));

        TimetableController controller = buildController("COMP1000\ndone\n\n1\nn\nn\nTC33\ny\n");
        User user = new User("student-001", "Test Student");

        controller.generate(user);

        assertTrue(timetableRepository.exists("TC33"));
    }

    @Test
    @Order(34)
    @DisplayName("TC X.34 – generate selects semester two when user enters 2")
    @Tag("TC X.34")
    @Tag("Luke")
    @Tag("Core")
    void generateSelectsSemesterTwoWhenUserEnters2() {
        ClassEntry lecture = buildClass("COMP1000-LEC-MON-0900", "COMP1000");
        classRepository.save(lecture);
        topicRepository.save(new au.edu.flinders.timetable.model.Topic(
                "COMP1000", "Programming", "Bedford Park", 2, "In Person", 1));

        TimetableController controller = buildController("COMP1000\ndone\n\n2\nn\nn\nTC34\ny\n");
        User user = new User("student-001", "Test Student");

        controller.generate(user);

        assertTrue(timetableRepository.exists("TC34"));
    }

    @Test
    @Order(35)
    @DisplayName("TC X.35 – generate prints message when invalid semester input entered")
    @Tag("TC X.35")
    @Tag("Luke")
    @Tag("Core")
    void generatePrintsMessageWhenInvalidSemesterInputEntered() {
        ClassEntry lecture = buildClass("COMP1000-LEC-MON-0900", "COMP1000");
        classRepository.save(lecture);
        topicRepository.save(new au.edu.flinders.timetable.model.Topic(
                "COMP1000", "Programming", "Bedford Park", 1, "In Person", 1));

        // First invalid semester input, then Enter for both
        TimetableController controller = buildController("COMP1000\ndone\n\n3\n\nn\nn\nTC35\ny\n");
        User user = new User("student-001", "Test Student");

        controller.generate(user);

        assertTrue(capturedOut.toString().contains("Please enter 1, 2"));
    }

    @Test
    @Order(36)
    @DisplayName("TC X.36 – generate auto-selects when only one class instance exists")
    @Tag("TC X.36")
    @Tag("Luke")
    @Tag("Core")
    void generateAutoSelectsWhenOnlyOneClassInstanceExists() {
        ClassEntry lecture = buildClass("COMP1000-LEC-MON-0900", "COMP1000");
        classRepository.save(lecture);
        topicRepository.save(new au.edu.flinders.timetable.model.Topic(
                "COMP1000", "Programming", "Bedford Park", 1, "In Person", 1));

        TimetableController controller = buildController("COMP1000\ndone\n\n\nn\nn\nTC36\ny\n");
        User user = new User("student-001", "Test Student");

        controller.generate(user);

        assertTrue(capturedOut.toString().contains("Auto-selected"));
    }

    @Test
    @Order(37)
    @DisplayName("TC X.37 – generate prompts user to select when multiple class instances exist")
    @Tag("TC X.37")
    @Tag("Luke")
    @Tag("Core")
    void generatePromptsUserToSelectWhenMultipleClassInstancesExist() {
        ClassEntry inst1 = new ClassEntry("COMP1000-LEC-MON-0900", "Lecture", null,
                LocalTime.of(9, 0), LocalTime.of(11, 0), DayOfWeek.MONDAY,
                "Registry Building", "R101", "COMP1000", "In person", 1, 1, "01 Mar", "01 Jun");
        ClassEntry inst2 = new ClassEntry("COMP1000-LEC-TUE-0900", "Lecture", null,
                LocalTime.of(9, 0), LocalTime.of(11, 0), DayOfWeek.TUESDAY,
                "Registry Building", "R101", "COMP1000", "In person", 1, 2, "01 Mar", "01 Jun");
        classRepository.save(inst1);
        classRepository.save(inst2);
        topicRepository.save(new au.edu.flinders.timetable.model.Topic(
                "COMP1000", "Programming", "Bedford Park", 1, "In Person", 1));

        // Select instance 1
        TimetableController controller = buildController("COMP1000\ndone\n\n\n1\nn\nn\nTC37\ny\n");
        User user = new User("student-001", "Test Student");

        controller.generate(user);

        assertTrue(timetableRepository.exists("TC37"));
    }

    @Test
    @Order(38)
    @DisplayName("TC X.38 – generate prints warning when campus filter removes all classes")
    @Tag("TC X.38")
    @Tag("Luke")
    @Tag("Core")
    void generatePrintsWarningWhenCampusFilterRemovesAllClasses() {
        ClassEntry lecture = buildClass("COMP1000-LEC-MON-0900", "COMP1000");
        classRepository.save(lecture);
        topicRepository.save(new au.edu.flinders.timetable.model.Topic(
                "COMP1000", "Programming", "Bedford Park", 1, "In Person", 1));

        // Select City campus — no city classes exist, filtered list empty, falls back
        // byType becomes empty only if grouped itself is empty — need a topic with no classes after filter
        // Use a course code that has classes but all filtered out with no fallback
        ClassEntry cityOnly = new ClassEntry("COMP2000-LEC-MON-0900", "Lecture", null,
                LocalTime.of(9, 0), LocalTime.of(11, 0), DayOfWeek.MONDAY,
                "Festival Tower", "R101", "COMP2000", "In person", 1, 1, "01 Mar", "01 Jun");
        classRepository.save(cityOnly);
        topicRepository.save(new au.edu.flinders.timetable.model.Topic(
                "COMP2000", "Topic", "City", 1, "In Person", 1));

        // Enrol COMP2000, select Bedford Park campus (filters out city class), semester Enter
        TimetableController controller = buildController("COMP2000\ndone\nBedford Park\n\nn\nn\n\nn\n");
        User user = new User("student-001", "Test Student");

        controller.generate(user);

        assertTrue(capturedOut.toString().toLowerCase().contains("no classes found")
                || capturedOut.toString().contains("no classes were selected"));
    }

    @Test
    @Order(39)
    @DisplayName("TC X.39 – generate prints error when timetable name already exists")
    @Tag("TC X.39")
    @Tag("Luke")
    @Tag("Core")
    void generatePrintsErrorWhenTimetableNameAlreadyExists() {
        ClassEntry lecture = buildClass("COMP1000-LEC-MON-0900", "COMP1000");
        classRepository.save(lecture);
        topicRepository.save(new au.edu.flinders.timetable.model.Topic(
                "COMP1000", "Programming", "Bedford Park", 1, "In Person", 1));
        saveTimetable("Duplicate");

        TimetableController controller = buildController("COMP1000\ndone\n\n\nn\nn\nDuplicate\ny\n");
        User user = new User("student-001", "Test Student");

        controller.generate(user);

        assertTrue(capturedOut.toString().toLowerCase().contains("error")
                || capturedOut.toString().contains("already exists"));
    }

    @Test
    @Order(40)
    @DisplayName("TC X.40 – editTimetable prints warning when no timetables exist")
    @Tag("TC X.40")
    @Tag("Luke")
    @Tag("Core")
    void editTimetablePrintsWarningWhenNoTimetablesExist() {
        TimetableController controller = buildController("");
        controller.editTimetable();

        assertTrue(capturedOut.toString().toLowerCase().contains("no saved timetables"));
    }

    @Test
    @Order(41)
    @DisplayName("TC X.41 – editTimetable shows alternatives list with building and room")
    @Tag("TC X.41")
    @Tag("Luke")
    @Tag("Core")
    void editTimetableShowsAlternativesListWithBuildingAndRoom() {
        ClassEntry inst1 = new ClassEntry("COMP1000-LEC-MON-0900", "Lecture", null,
                LocalTime.of(9, 0), LocalTime.of(11, 0), DayOfWeek.MONDAY,
                "Registry Building", "R101", "COMP1000", "In person", 1, 1, "01 Mar", "01 Jun");
        ClassEntry inst2 = new ClassEntry("COMP1000-LEC-TUE-0900", "Lecture", null,
                LocalTime.of(9, 0), LocalTime.of(11, 0), DayOfWeek.TUESDAY,
                "Registry Building", "R202", "COMP1000", "In person", 1, 2, "01 Mar", "01 Jun");
        classRepository.save(inst1);
        classRepository.save(inst2);

        Timetable t = new Timetable("Edit Test", "Semester 1", false, false);
        t.addClass("COMP1000-LEC-MON-0900");
        timetableRepository.save(t);

        // Select timetable 1, class 1, replacement 1, confirm y
        TimetableController controller = buildController("1\n1\n1\ny\n");
        controller.editTimetable();

        assertTrue(capturedOut.toString().contains("R202") || capturedOut.toString().contains("Replacement"));
    }

    @Test
    @Order(42)
    @DisplayName("TC X.42 – generate handles comma-separated course codes")
    @Tag("TC X.42")
    @Tag("Luke")
    @Tag("Core")
    void generateHandlesCommaSeparatedCourseCodes() {
        ClassEntry comp = buildClass("COMP1000-LEC-MON-0900", "COMP1000");
        ClassEntry math = buildClass("MATH1000-LEC-TUE-0900", "MATH1000");
        classRepository.save(comp);
        classRepository.save(math);
        topicRepository.save(new au.edu.flinders.timetable.model.Topic(
                "COMP1000", "Programming", "Bedford Park", 1, "In Person", 1));
        topicRepository.save(new au.edu.flinders.timetable.model.Topic(
                "MATH1000", "Maths", "Bedford Park", 1, "In Person", 1));

        TimetableController controller = buildController("COMP1000,MATH1000\ndone\n\n\n\nn\nn\nTC42\ny\n");
        User user = new User("student-001", "Test Student");

        controller.generate(user);

        assertTrue(timetableRepository.exists("TC42"));
    }


    @Test
    @Order(43)
    @DisplayName("TC X.43 – generate filters city campus classes when city campus selected by number")
    @Tag("TC X.43")
    @Tag("Luke")
    @Tag("Core")
    void generateFiltersCityCampusClassesWhenCitySelected() {
        ClassEntry cityClass = new ClassEntry("COMP1000-LEC-MON-0900", "Lecture", null,
                LocalTime.of(9, 0), LocalTime.of(11, 0), DayOfWeek.MONDAY,
                "Festival Tower", "R101", "COMP1000", "In person", 1, 1, "01 Mar", "01 Jun");
        classRepository.save(cityClass);
        topicRepository.save(new au.edu.flinders.timetable.model.Topic(
                "COMP1000", "Programming", "City", 1, "In Person", 1));

        // Enrol, done, select campus 1 (City), semester Enter, overlap n, prefs n, name, confirm y
        TimetableController controller = buildController("COMP1000\ndone\n1\n\nn\nn\nTC43\ny\n");
        User user = new User("student-001", "Test Student");

        controller.generate(user);

        assertTrue(timetableRepository.exists("TC43"));
    }


    @Test
    @Order(44)
    @DisplayName("TC X.44 – generate ignores empty token from trailing comma in input")
    @Tag("TC X.44")
    @Tag("Luke")
    @Tag("Core")
    void generateIgnoresEmptyTokenFromTrailingComma() {
        ClassEntry lecture = buildClass("COMP1000-LEC-MON-0900", "COMP1000");
        classRepository.save(lecture);
        topicRepository.save(new au.edu.flinders.timetable.model.Topic(
                "COMP1000", "Programming", "Bedford Park", 1, "In Person", 1));

        // Trailing comma produces empty token
        TimetableController controller = buildController("COMP1000,\ndone\n\n\nn\nn\nTC44\ny\n");
        User user = new User("student-001", "Test Student");

        controller.generate(user);

        assertTrue(timetableRepository.exists("TC44"));
    }


    @Test
    @Order(45)
    @DisplayName("TC X.45 – generate prints out of range message when number exceeds topic list size")
    @Tag("TC X.45")
    @Tag("Luke")
    @Tag("Core")
    void generatePrintsOutOfRangeWhenNumberExceedsTopicListSize() {
        topicRepository.save(new au.edu.flinders.timetable.model.Topic(
                "COMP1000", "Programming", "Bedford Park", 1, "In Person", 1));

        TimetableController controller = buildController("99\ndone\n\nn\nn\n\nn\n");
        User user = new User("student-001", "Test Student");

        controller.generate(user);

        assertTrue(capturedOut.toString().contains("out of range"));
    }


    @Test
    @Order(46)
    @DisplayName("TC X.46 – generate handles class with null building defaulting to Bedford Park")
    @Tag("TC X.46")
    @Tag("Luke")
    @Tag("Core")
    void generateHandlesClassWithNullBuildingDefaultingToBedfordPark() {
        ClassEntry nullBuilding = new ClassEntry("COMP1000-LEC-MON-0900", "Lecture", null,
                LocalTime.of(9, 0), LocalTime.of(11, 0), DayOfWeek.MONDAY,
                null, "R101", "COMP1000", "In person", 1, 1, "01 Mar", "01 Jun");
        classRepository.save(nullBuilding);
        topicRepository.save(new au.edu.flinders.timetable.model.Topic(
                "COMP1000", "Programming", "Bedford Park", 1, "In Person", 1));

        TimetableController controller = buildController("COMP1000\ndone\n\n\nn\nn\nTC46\ny\n");
        User user = new User("student-001", "Test Student");

        controller.generate(user);

        assertTrue(timetableRepository.exists("TC46"));
    }
}