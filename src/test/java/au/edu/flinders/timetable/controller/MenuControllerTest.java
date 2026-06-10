package au.edu.flinders.timetable.controller;

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
    @DisplayName("Menu displays ASCII banner")
    @Tag("TC 6.01")
    @Tag("Seth")
    @Tag("Core")
    void displayMenu_showsBanner() {
        assertNotNull(MenuController.BANNER);
        assertFalse(MenuController.BANNER.trim().isEmpty());  // fixed: isEmpty() always false warning
    }

    @Test
    @DisplayName("Menu displays header")
    @Tag("TC 6.02")
    @Tag("Seth")
    @Tag("Core")
    void displayMenu_showsHeader() {
        String output = menuController.displayMenu();

        assertTrue(output.contains(MenuController.MENU_HEADER));
    }

    @Test
    @DisplayName("Menu displays all six options")
    @Tag("TC 6.03")
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
    @DisplayName("Menu displays input prompt")
    @Tag("TC 6.04")
    @Tag("Seth")
    @Tag("Core")
    void displayMenu_showsPrompt() {
        String output = menuController.displayMenu();

        assertTrue(output.contains(MenuController.PROMPT));
    }

    @Test
    @DisplayName("Input 1 delegates to ImportExportController")
    @Tag("TC 6.05")
    @Tag("Seth")
    @Tag("Critical")
    void handleInput_1_delegatesToImportController() {
        menuController.handleInput("1");

        assertTrue(importExportController.importCalled);
    }

    @Test
    @DisplayName("Input 2 delegates to ClassController")
    @Tag("TC 6.06")
    @Tag("Seth")
    @Tag("Critical")
    void handleInput_2_delegatesToClassController() {
        menuController.handleInput("2");

        assertTrue(classController.showAllCalled);
    }

    @Test
    @DisplayName("Input 3 delegates to PreferenceService")
    @Tag("TC 6.07")
    @Tag("Seth")
    @Tag("Critical")
    void handleInput_3_delegatesToPreferenceService() {
        menuController.handleInput("3");

        assertTrue(preferenceService.manageCalled);
    }

    @Test
    @DisplayName("Input 4 delegates to TimetableController generate")
    @Tag("TC 6.08")
    @Tag("Seth")
    @Tag("Critical")
    void handleInput_4_delegatesToTimetableGenerate() {
        menuController.handleInput("4");

        assertTrue(timetableController.generateCalled);
    }

    @Test
    @DisplayName("Input 5 delegates to TimetableController viewAll")
    @Tag("TC 6.09")
    @Tag("Seth")
    @Tag("Critical")
    void handleInput_5_delegatesToTimetableViewAll() {
        menuController.handleInput("5");

        assertTrue(timetableController.viewAllCalled);
    }

    @Test
    @DisplayName("Input 6 returns exit message")
    @Tag("TC 6.10")
    @Tag("Seth")
    @Tag("Critical")
    void handleInput_6_returnsExitMessage() {
        String result = menuController.handleInput("6");

        assertEquals(MenuController.EXIT_MESSAGE, result);
    }

    @Test
    @DisplayName("Invalid input shows error message")
    @Tag("TC 6.11")
    @Tag("Seth")
    @Tag("Core")
    void handleInput_invalidOption_showsErrorMessage() {
        String result = menuController.handleInput("9");

        assertEquals(MenuController.INVALID_INPUT, result);
    }

    @Test
    @DisplayName("Blank input shows error message")
    @Tag("TC 6.12")
    @Tag("Seth")
    @Tag("Core")
    void handleInput_blankInput_showsErrorMessage() {
        String result = menuController.handleInput("   ");

        assertEquals(MenuController.INVALID_INPUT, result);
    }

    @Test
    @DisplayName("Null input shows error message")
    @Tag("TC 6.13")
    @Tag("Seth")
    @Tag("Core")
    void handleInput_nullInput_showsErrorMessage() {
        String result = menuController.handleInput(null);

        assertEquals(MenuController.INVALID_INPUT, result);
    }

    @Test
    @DisplayName("Input with surrounding whitespace is trimmed and handled correctly")
    @Tag("TC 6.14")
    @Tag("Seth")
    @Tag("Core")
    void handleInput_withWhitespace_isTrimmed() {
        menuController.handleInput("  2  ");

        assertTrue(classController.showAllCalled);
    }

    @Test
    @DisplayName("Exit input 6 is correctly identified")
    @Tag("TC 6.15")
    @Tag("Seth")
    @Tag("Core")
    void isExitInput_returnsTrueForSix() {
        assertTrue(menuController.isExitInput("6"));
    }

    @Test
    @DisplayName("Non-exit input is not identified as exit")
    @Tag("TC 6.16")
    @Tag("Seth")
    @Tag("Core")
    void isExitInput_returnsFalseForOtherInput() {
        assertFalse(menuController.isExitInput("1"));
        assertFalse(menuController.isExitInput(""));
        assertFalse(menuController.isExitInput(null));
    }
}