package au.edu.flinders.timetable.controller;

import au.edu.flinders.timetable.model.Preference;
import au.edu.flinders.timetable.model.User;
import au.edu.flinders.timetable.service.PreferenceService;
import au.edu.flinders.timetable.repository.PreferenceRepository;
import au.edu.flinders.timetable.service.CSVImportService;
import au.edu.flinders.timetable.service.ClassService;
import au.edu.flinders.timetable.service.SearchService;
import au.edu.flinders.timetable.service.TimetableGeneratorService;
import au.edu.flinders.timetable.service.TimetableService;
import au.edu.flinders.timetable.repository.ClassRepository;
import au.edu.flinders.timetable.repository.TimetableRepository;
import au.edu.flinders.timetable.repository.TopicRepository;
import au.edu.flinders.timetable.ui.ConsoleView;
import au.edu.flinders.timetable.ui.InputHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class MenuControllerTest {

    /* Records whether importData() was called. */
    private static class StubImportExportController extends ImportExportController {
        boolean importCalled = false;
        StubImportExportController() {
            super(new CSVImportService(new TopicRepository(), new ClassRepository()),  // fixed order
                    new ConsoleView(),
                    new InputHelper(),
                    new Scanner(System.in));
        }
        @Override public void importData() { importCalled = true; }
    }

    /* Records whether showAll() was called. */
    private static class StubClassController extends ClassController {
        boolean showAllCalled = false;
        StubClassController() {
            super(new ClassService(new ClassRepository(), new TopicRepository()),
                    new SearchService(new ClassRepository(), new TopicRepository()),
                    new ConsoleView());
        }
        @Override public void showAll() { showAllCalled = true; }
    }

    /* Records whether generate() and viewAll() were called. */
    private static class StubTimetableController extends TimetableController {
        boolean generateCalled = false;
        boolean viewAllCalled  = false;
        StubTimetableController() {
            super(new TimetableGeneratorService(
                            new ClassRepository(), new TopicRepository(),
                            new PreferenceRepository(),
                            new TimetableService(new TimetableRepository())),
                    new TimetableService(new TimetableRepository()),
                    new au.edu.flinders.timetable.service.CSVExportService(  // fixed: null -> real instance
                            new ClassRepository(), new TopicRepository()),
                    new ClassRepository(),
                    new TopicRepository(),
                    new ClassService(new ClassRepository(), new TopicRepository()),
                    new ConsoleView(),
                    new InputHelper(),
                    new Scanner(System.in));
        }
        @Override public void generate(User user) { generateCalled = true; }
        @Override public void viewAll()           { viewAllCalled  = true; }
    }

    /* Records whether managePreferences() was called. */
    private static class StubPreferenceService extends PreferenceService {
        boolean manageCalled = false;
        StubPreferenceService() {
            super(new PreferenceRepository());
        }
        @Override public void managePreferences() { manageCalled = true; }
    }

    /* Overrides all ClassController sub-menu methods */
    private static class FullStubClassController extends ClassController {
        String lastCalled = "";
        FullStubClassController() {
            super(new ClassService(new ClassRepository(), new TopicRepository()),
                    new SearchService(new ClassRepository(), new TopicRepository()),
                    new ConsoleView());
        }
        @Override public void search()          { lastCalled = "search"; }
        @Override public void showAll()         { lastCalled = "showAll"; }
        @Override public void viewAll()         { lastCalled = "viewAll"; }
        @Override public void viewAllDetailed() { lastCalled = "viewAllDetailed"; }
        @Override public void editClass()       { lastCalled = "editClass"; }
        @Override public void deleteClass()     { lastCalled = "deleteClass"; }
    }

    /* Overrides all TimetableController sub-menu methods */
    private static class FullStubTimetableController extends TimetableController {
        String lastCalled = "";
        FullStubTimetableController() {
            super(new TimetableGeneratorService(
                            new ClassRepository(), new TopicRepository(),
                            new PreferenceRepository(),
                            new TimetableService(new TimetableRepository())),
                    new TimetableService(new TimetableRepository()),
                    new au.edu.flinders.timetable.service.CSVExportService(
                            new ClassRepository(), new TopicRepository()),
                    new ClassRepository(), new TopicRepository(),
                    new ClassService(new ClassRepository(), new TopicRepository()),
                    new ConsoleView(), new InputHelper(), new Scanner(System.in));
        }
        @Override public void generate(User u) { lastCalled = "generate"; }
        @Override public void viewAll()        { lastCalled = "viewAll"; }
        @Override public void view()           { lastCalled = "view"; }
        @Override public void editTimetable()  { lastCalled = "editTimetable"; }
        @Override public void export()         { lastCalled = "export"; }
        @Override public void delete()         { lastCalled = "delete"; }
    }

    // builds 7-arg controller with piped input
    private MenuController buildFull(String inputStr,
                                     ClassController cc,
                                     TimetableController tc,
                                     PreferenceService ps) {
        Scanner sc = new Scanner(new ByteArrayInputStream(inputStr.getBytes()));
        return new MenuController(sc, new ConsoleView(), new InputHelper(),
                importExportController, cc, tc, ps);
    }

    // capture / restore helpers (call per-test when output assertion needed)
    private final PrintStream originalOut = System.out;
    private ByteArrayOutputStream capturedOut;
    private void captureOut() {
        capturedOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capturedOut));
    }
    private String captured() { return capturedOut.toString(); }
    private void restoreOut() { System.setOut(originalOut); }

    private MenuController             menuController;
    private StubImportExportController importExportController;
    private StubClassController        classController;
    private StubTimetableController    timetableController;
    private StubPreferenceService      preferenceService;

    @BeforeEach
    void setUp() {
        importExportController = new StubImportExportController();
        classController        = new StubClassController();
        timetableController    = new StubTimetableController();
        preferenceService      = new StubPreferenceService();

        menuController = new MenuController(
                importExportController,
                classController,
                preferenceService,
                timetableController,
                new Scanner(System.in)
        );
    }

    @Test
    @DisplayName("TC 13.05 - Menu displays ASCII banner")
    @Tag("Seth")
    @Tag("Core")
    void displayMenu_showsBanner() {
        assertNotNull(MenuController.BANNER);
        assertFalse(MenuController.BANNER.trim().isEmpty());  // fixed: isEmpty() always false warning
    }

    @Test
    @DisplayName("TC 13.06 - Menu displays header")
    @Tag("Seth")
    @Tag("Core")
    void displayMenu_showsHeader() {
        String output = menuController.displayMenu();

        assertTrue(output.contains(MenuController.MENU_HEADER));
    }

    @Test
    @DisplayName("TC 13.07 - Menu displays all six options")
    @Tag("Seth")
    @Tag("Core")
    void displayMenu_showsAllSixOptions() {
        String output = menuController.displayMenu();

        assertTrue(output.contains(MenuController.OPTION_IMPORT));
        assertTrue(output.contains(MenuController.OPTION_SEARCH));
        assertTrue(output.contains(MenuController.OPTION_PREFERENCES));
        assertTrue(output.contains(MenuController.OPTION_GENERATE));
        assertTrue(output.contains(MenuController.OPTION_VIEW_EXPORT));
        assertTrue(output.contains(MenuController.OPTION_EXIT));
    }

    @Test
    @DisplayName("TC 13.08 - Menu displays input prompt")
    @Tag("Seth")
    @Tag("Core")
    void displayMenu_showsPrompt() {
        String output = menuController.displayMenu();

        assertTrue(output.contains(MenuController.PROMPT));
    }

    @Test
    @DisplayName("TC 13.09 - Input 1 delegates to ImportExportController")
    @Tag("Seth")
    @Tag("Critical")
    void handleInput_1_delegatesToImportController() {
        menuController.handleInput("1");

        assertTrue(importExportController.importCalled);
    }

    @Test
    @DisplayName("TC 13.10 - Input 2 delegates to ClassController")
    @Tag("Seth")
    @Tag("Critical")
    void handleInput_2_delegatesToClassController() {
        menuController.handleInput("2");

        assertTrue(classController.showAllCalled);
    }

    @Test
    @DisplayName("TC 13.11 - Input 3 delegates to PreferenceService")
    @Tag("Seth")
    @Tag("Critical")
    void handleInput_3_delegatesToPreferenceService() {
        menuController.handleInput("3");

        assertTrue(preferenceService.manageCalled);
    }

    @Test
    @DisplayName("TC 13.12 - Input 4 delegates to TimetableController generate")
    @Tag("Seth")
    @Tag("Critical")
    void handleInput_4_delegatesToTimetableGenerate() {
        menuController.handleInput("4");

        assertTrue(timetableController.generateCalled);
    }

    @Test
    @DisplayName("TC 13.13 - Input 5 delegates to TimetableController viewAll")
    @Tag("Seth")
    @Tag("Critical")
    void handleInput_5_delegatesToTimetableViewAll() {
        menuController.handleInput("5");

        assertTrue(timetableController.viewAllCalled);
    }

    @Test
    @DisplayName("TC 13.14 - Input 6 returns exit message")
    @Tag("Seth")
    @Tag("Critical")
    void handleInput_6_returnsExitMessage() {
        String result = menuController.handleInput("6");

        assertEquals(MenuController.EXIT_MESSAGE, result);
    }

    @Test
    @DisplayName("TC 13.15 - Invalid input shows error message")
    @Tag("Seth")
    @Tag("Core")
    void handleInput_invalidOption_showsErrorMessage() {
        String result = menuController.handleInput("9");

        assertEquals(MenuController.INVALID_INPUT, result);
    }

    @Test
    @DisplayName("TC 13.16 - Blank input shows error message")
    @Tag("Seth")
    @Tag("Core")
    void handleInput_blankInput_showsErrorMessage() {
        String result = menuController.handleInput("   ");

        assertEquals(MenuController.INVALID_INPUT, result);
    }

    @Test
    @DisplayName("TC 13.17 - Null input shows error message")
    @Tag("Seth")
    @Tag("Core")
    void handleInput_nullInput_showsErrorMessage() {
        String result = menuController.handleInput(null);

        assertEquals(MenuController.INVALID_INPUT, result);
    }

    @Test
    @DisplayName("TC 13.18 - Input with surrounding whitespace is trimmed and handled correctly")
    @Tag("Seth")
    @Tag("Core")
    void handleInput_withWhitespace_isTrimmed() {
        menuController.handleInput("  2  ");

        assertTrue(classController.showAllCalled);
    }

    @Test
    @DisplayName("TC 13.19 - Exit input 6 is correctly identified")
    @Tag("Seth")
    @Tag("Core")
    void isExitInput_returnsTrueForSix() {
        assertTrue(menuController.isExitInput("6"));
    }

    @Test
    @DisplayName("TC 13.20 - Non-exit input is not identified as exit")
    @Tag("Seth")
    @Tag("Core")
    void isExitInput_returnsFalseForOtherInput() {
        assertFalse(menuController.isExitInput("1"));
        assertFalse(menuController.isExitInput(""));
        assertFalse(menuController.isExitInput(null));
    }

    @Test
    @DisplayName("TC 13.21 - start() creates user and exits with Goodbye on choice 6")
    @Tag("Luke")
    @Tag("Core")
    void startCreatesUserAndExitsOnChoiceSix() {
        captureOut();
        try {
            // userId, name, then choice 6 to exit
            MenuController mc = buildFull("s001\nAlice\n6\n",
                    new FullStubClassController(),
                    new FullStubTimetableController(),
                    new StubPreferenceService());
            mc.start();
            String out = captured();
            assertTrue(out.contains("Alice"));
            assertTrue(out.contains("Goodbye"));
        } finally { restoreOut(); }
    }

    @Test
    @DisplayName("TC 13.22 - start() catches EarlyExitException and returns to main menu")
    @Tag("Luke")
    @Tag("Core")
    void startCatchesEarlyExitExceptionAndContinuesLoop() {
        captureOut();
        try {
            // q at menu choice → EarlyExitException → "Returning to main menu." → 6 exits
            MenuController mc = buildFull("s001\nAlice\nq\n6\n",
                    new FullStubClassController(),
                    new FullStubTimetableController(),
                    new StubPreferenceService());
            mc.start();
            assertTrue(captured().contains("Returning to main menu"));
        } finally { restoreOut(); }
    }

    @Test
    @DisplayName("TC 13.23 - handleClasses choice 1 delegates to classController.search()")
    @Tag("Luke")
    @Tag("Core")
    void handleClassesChoice1DelegatesToSearch() {
        captureOut();
        try {
            FullStubClassController cc = new FullStubClassController();
            // menu=2 (Classes), sub=1 (search), menu=6 (exit)
            buildFull("s001\nAlice\n2\n1\n6\n", cc,
                    new FullStubTimetableController(), new StubPreferenceService()).start();
            assertEquals("search", cc.lastCalled);
        } finally { restoreOut(); }
    }

    @Test
    @DisplayName("TC 13.24 - handleClasses choice 2 delegates to classController.viewAll()")
    @Tag("Luke")
    @Tag("Core")
    void handleClassesChoice2DelegatesToViewAll() {
        captureOut();
        try {
            FullStubClassController cc = new FullStubClassController();
            buildFull("s001\nAlice\n2\n2\n6\n", cc,
                    new FullStubTimetableController(), new StubPreferenceService()).start();
            assertEquals("viewAll", cc.lastCalled);
        } finally { restoreOut(); }
    }

    @Test
    @DisplayName("TC 13.25 - handleClasses choice 3 delegates to classController.viewAllDetailed()")
    @Tag("Luke")
    @Tag("Core")
    void handleClassesChoice3DelegatesToViewAllDetailed() {
        captureOut();
        try {
            FullStubClassController cc = new FullStubClassController();
            buildFull("s001\nAlice\n2\n3\n6\n", cc,
                    new FullStubTimetableController(), new StubPreferenceService()).start();
            assertEquals("viewAllDetailed", cc.lastCalled);
        } finally { restoreOut(); }
    }

    @Test
    @DisplayName("TC 13.26 - handleClasses choice 4 delegates to classController.editClass()")
    @Tag("Luke")
    @Tag("Core")
    void handleClassesChoice4DelegatesToEditClass() {
        captureOut();
        try {
            FullStubClassController cc = new FullStubClassController();
            buildFull("s001\nAlice\n2\n4\n6\n", cc,
                    new FullStubTimetableController(), new StubPreferenceService()).start();
            assertEquals("editClass", cc.lastCalled);
        } finally { restoreOut(); }
    }

    @Test
    @DisplayName("TC 13.27 - handleClasses choice 5 delegates to classController.deleteClass()")
    @Tag("Luke")
    @Tag("Core")
    void handleClassesChoice5DelegatesToDeleteClass() {
        captureOut();
        try {
            FullStubClassController cc = new FullStubClassController();
            buildFull("s001\nAlice\n2\n5\n6\n", cc,
                    new FullStubTimetableController(), new StubPreferenceService()).start();
            assertEquals("deleteClass", cc.lastCalled);
        } finally { restoreOut(); }
    }

    @Test
    @DisplayName("TC 13.28 - handleViewExport choice 1 delegates to timetableController.viewAll()")
    @Tag("Luke")
    @Tag("Core")
    void handleViewExportChoice1DelegatesToViewAll() {
        captureOut();
        try {
            FullStubTimetableController tc = new FullStubTimetableController();
            buildFull("s001\nAlice\n5\n1\n6\n",
                    new FullStubClassController(), tc, new StubPreferenceService()).start();
            assertEquals("viewAll", tc.lastCalled);
        } finally { restoreOut(); }
    }

    @Test
    @DisplayName("TC 13.29 - handleViewExport choice 2 delegates to timetableController.view()")
    @Tag("Luke")
    @Tag("Core")
    void handleViewExportChoice2DelegatesToView() {
        captureOut();
        try {
            FullStubTimetableController tc = new FullStubTimetableController();
            buildFull("s001\nAlice\n5\n2\n6\n",
                    new FullStubClassController(), tc, new StubPreferenceService()).start();
            assertEquals("view", tc.lastCalled);
        } finally { restoreOut(); }
    }

    @Test
    @DisplayName("TC 13.30 - handleViewExport choice 3 delegates to timetableController.editTimetable()")
    @Tag("Luke")
    @Tag("Core")
    void handleViewExportChoice3DelegatesToEditTimetable() {
        captureOut();
        try {
            FullStubTimetableController tc = new FullStubTimetableController();
            buildFull("s001\nAlice\n5\n3\n6\n",
                    new FullStubClassController(), tc, new StubPreferenceService()).start();
            assertEquals("editTimetable", tc.lastCalled);
        } finally { restoreOut(); }
    }

    @Test
    @DisplayName("TC 13.31 - handleViewExport choice 4 delegates to timetableController.export()")
    @Tag("Luke")
    @Tag("Core")
    void handleViewExportChoice4DelegatesToExport() {
        captureOut();
        try {
            FullStubTimetableController tc = new FullStubTimetableController();
            buildFull("s001\nAlice\n5\n4\n6\n",
                    new FullStubClassController(), tc, new StubPreferenceService()).start();
            assertEquals("export", tc.lastCalled);
        } finally { restoreOut(); }
    }

    @Test
    @DisplayName("TC 13.32 - handleViewExport choice 5 delegates to timetableController.delete()")
    @Tag("Luke")
    @Tag("Core")
    void handleViewExportChoice5DelegatesToDelete() {
        captureOut();
        try {
            FullStubTimetableController tc = new FullStubTimetableController();
            buildFull("s001\nAlice\n5\n5\n6\n",
                    new FullStubClassController(), tc, new StubPreferenceService()).start();
            assertEquals("delete", tc.lastCalled);
        } finally { restoreOut(); }
    }

    @Test
    @DisplayName("TC 13.33 - handlePreferences choice 1 prints no preferences when none set")
    @Tag("Luke")
    @Tag("Core")
    void handlePreferencesChoice1PrintsNoneWhenNoPreferencesSet() {
        captureOut();
        try {
            // menu=3 (Preferences), sub=1 (view), menu=6 (exit)
            buildFull("s001\nAlice\n3\n1\n6\n",
                    new FullStubClassController(),
                    new FullStubTimetableController(),
                    new StubPreferenceService()).start();
            assertTrue(captured().contains("No preferences set"));
        } finally { restoreOut(); }
    }

    @Test
    @DisplayName("TC 13.34 - handlePreferences choice 1 prints existing preference")
    @Tag("Luke")
    @Tag("Core")
    void handlePreferencesChoice1PrintsExistingPreference() {
        captureOut();
        try {
            StubPreferenceService ps = new StubPreferenceService();
            // Pre-save a preference so getPreference returns it
            ps.savePreference(new Preference("s001", java.util.List.of(Preference.TIME_MORNING)));

            buildFull("s001\nAlice\n3\n1\n6\n",
                    new FullStubClassController(),
                    new FullStubTimetableController(), ps).start();
            assertTrue(captured().contains("TIME_MORNING"));
        } finally { restoreOut(); }
    }

    @Test
    @DisplayName("TC 13.35 - handlePreferences choice 3 clears preference and prints success")
    @Tag("Luke")
    @Tag("Core")
    void handlePreferencesChoice3ClearsPreference() {
        captureOut();
        try {
            StubPreferenceService ps = new StubPreferenceService();
            ps.savePreference(new Preference("s001", java.util.List.of(Preference.TIME_MORNING)));

            buildFull("s001\nAlice\n3\n3\n6\n",
                    new FullStubClassController(),
                    new FullStubTimetableController(), ps).start();

            assertTrue(captured().contains("Preferences cleared"));
            assertTrue(ps.getPreference("s001").isEmpty());
        } finally { restoreOut(); }
    }

    @Test
    @DisplayName("TC 13.36 - setPreferencesTokenMode saves selected token and prints success")
    @Tag("Luke")
    @Tag("Core")
    void setPreferencesTokenModeSavesSelectedToken() {
        captureOut();
        try {
            StubPreferenceService ps = new StubPreferenceService();
            // menu=3, sub=2 (set), token=1 (CAMPUS_BEDFORD_PARK), Enter (done), menu=6
            buildFull("s001\nAlice\n3\n2\n1\n\n6\n",
                    new FullStubClassController(),
                    new FullStubTimetableController(), ps).start();

            assertTrue(captured().contains("Preferences saved"));
            assertTrue(ps.getPreference("s001").isPresent());
        } finally { restoreOut(); }
    }

    @Test
    @DisplayName("TC 13.37 - setPreferencesTokenMode prints warning when no tokens selected")
    @Tag("Luke")
    @Tag("Core")
    void setPreferencesTokenModeWarnsWhenNoTokensSelected() {
        captureOut();
        try {
            // Enter immediately (empty) → no tokens selected
            buildFull("s001\nAlice\n3\n2\n\n6\n",
                    new FullStubClassController(),
                    new FullStubTimetableController(),
                    new StubPreferenceService()).start();
            assertTrue(captured().contains("No tokens selected"));
        } finally { restoreOut(); }
    }

    @Test
    @DisplayName("TC 13.38 - setPreferencesTokenMode warns on non-numeric input")
    @Tag("Luke")
    @Tag("Core")
    void setPreferencesTokenModeWarnsOnNonNumericInput() {
        captureOut();
        try {
            // "abc" → warning, then Enter → no tokens → warning, menu=6
            buildFull("s001\nAlice\n3\n2\nabc\n\n6\n",
                    new FullStubClassController(),
                    new FullStubTimetableController(),
                    new StubPreferenceService()).start();
            assertTrue(captured().contains("Please enter a number"));
        } finally { restoreOut(); }
    }

    @Test
    @DisplayName("TC 13.39 - setPreferencesTokenMode warns on out-of-range number")
    @Tag("Luke")
    @Tag("Core")
    void setPreferencesTokenModeWarnsOnOutOfRangeNumber() {
        captureOut();
        try {
            // "99" → out of range warning, Enter → no tokens warning, menu=6
            buildFull("s001\nAlice\n3\n2\n99\n\n6\n",
                    new FullStubClassController(),
                    new FullStubTimetableController(),
                    new StubPreferenceService()).start();
            assertTrue(captured().contains("out of range"));
        } finally { restoreOut(); }
    }

    @Test
    @DisplayName("TC 13.40 - setPreferencesTokenMode warns on duplicate token selection")
    @Tag("Luke")
    @Tag("Core")
    void setPreferencesTokenModeWarnsOnDuplicateToken() {
        captureOut();
        try {
            // "1", "1" (duplicate) → warning, Enter → saves just the one token, menu=6
            buildFull("s001\nAlice\n3\n2\n1\n1\n\n6\n",
                    new FullStubClassController(),
                    new FullStubTimetableController(),
                    new StubPreferenceService()).start();
            assertTrue(captured().contains("already selected"));
        } finally { restoreOut(); }
    }

    @Test
    @DisplayName("TC 13.41 - start() choice 1 delegates to importExportController.importData()")
    @Tag("Luke")
    @Tag("Core")
    void startChoice1DelegatesToImportData() {
        captureOut();
        try {
            buildFull("s001\nAlice\n1\n6\n",
                    new FullStubClassController(),
                    new FullStubTimetableController(),
                    new StubPreferenceService()).start();
            assertTrue(importExportController.importCalled);
        } finally { restoreOut(); }
    }

    @Test
    @DisplayName("TC 13.42 - start() choice 4 delegates to timetableController.generate()")
    @Tag("Luke")
    @Tag("Core")
    void startChoice4DelegatesToGenerate() {
        captureOut();
        try {
            FullStubTimetableController tc = new FullStubTimetableController();
            buildFull("s001\nAlice\n4\n6\n",
                    new FullStubClassController(), tc,
                    new StubPreferenceService()).start();
            assertEquals("generate", tc.lastCalled);
        } finally { restoreOut(); }
    }

    @Test
    @DisplayName("TC 13.43 - start() choice 2 enters handleClasses and executes switch")
    @Tag("Luke")
    @Tag("Core")
    void startChoice2EntersHandleClassesSwitch() {
        captureOut();
        try {
            FullStubClassController cc = new FullStubClassController();
            buildFull("s001\nAlice\n2\n1\n6\n", cc,
                    new FullStubTimetableController(),
                    new StubPreferenceService()).start();
            assertEquals("search", cc.lastCalled);
        } finally { restoreOut(); }
    }

    @Test
    @DisplayName("TC 13.44 - start() choice 5 enters handleViewExport and executes switch")
    @Tag("Luke")
    @Tag("Core")
    void startChoice5EntersHandleViewExportSwitch() {
        captureOut();
        try {
            FullStubTimetableController tc = new FullStubTimetableController();
            buildFull("s001\nAlice\n5\n1\n6\n",
                    new FullStubClassController(), tc,
                    new StubPreferenceService()).start();
            assertEquals("viewAll", tc.lastCalled);
        } finally { restoreOut(); }
    }

    @Test
    @DisplayName("TC 13.45 - start() choice 3 enters handlePreferences and executes switch")
    @Tag("Luke")
    @Tag("Core")
    void startChoice3EntersHandlePreferencesSwitch() {
        captureOut();
        try {
            buildFull("s001\nAlice\n3\n1\n6\n",
                    new FullStubClassController(),
                    new FullStubTimetableController(),
                    new StubPreferenceService()).start();
            assertTrue(captured().contains("No preferences set"));
        } finally { restoreOut(); }
    }

    @Test
    @DisplayName("TC 13.46 - setPreferencesTokenMode warns when number is out of range")
    @Tag("Luke")
    @Tag("Core")
    void setPreferencesTokenModeWarnsOnOutOfRange() {
        captureOut();
        try {
            // 99 → out of range (line 261), then Enter → no tokens, menu=6
            buildFull("s001\nAlice\n3\n2\n99\n\n6\n",
                    new FullStubClassController(),
                    new FullStubTimetableController(),
                    new StubPreferenceService()).start();
            assertTrue(captured().contains("out of range"));
        } finally { restoreOut(); }
    }
}
