package au.edu.flinders.timetable.controller;

import au.edu.flinders.timetable.service.PreferenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MenuControllerTest {

    private MenuController         menuController;
    private ImportExportController importExportController;
    private ClassController        classController;
    private PreferenceService      preferenceService;
    private TimetableController    timetableController;

    @BeforeEach
    void setUp() {
        importExportController = mock(ImportExportController.class);
        classController        = mock(ClassController.class);
        preferenceService      = mock(PreferenceService.class);
        timetableController    = mock(TimetableController.class);

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
        assertFalse(MenuController.BANNER.isBlank());
    }

    @Test
    @DisplayName("Menu displays header")
    @Tag("TC 6.02")
    @Tag("Seth")
    @Tag("Core")
    void displayMenu_showsHeader() {
        String output = menuController.displayMenu();  // fixed typo: disaplysMenu -> displayMenu

        assertTrue(output.contains(MenuController.MENU_HEADER));
    }

    @Test
    @DisplayName("Menu displays all six options")
    @Tag("TC 6.03")
    @Tag("Seth")
    @Tag("Core")
    void displayMenu_showsAllSixOptions() {
        String output = menuController.displayMenu();

        assertTrue(output.contains(MenuController.OPTION_IMPORT));       // fixed typo: OPTION_IMPORR
        assertTrue(output.contains(MenuController.OPTION_SEARCH));
        assertTrue(output.contains(MenuController.OPTION_PREFERENCES));  // fixed typo: OPTION_PREFERENCE
        assertTrue(output.contains(MenuController.OPTION_GENERATE));
        assertTrue(output.contains(MenuController.OPTION_VIEW_EXPORT));  // was missing
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
    @Tag("TC 6.05")  // fixed typo: Tc -> TC
    @Tag("Seth")
    @Tag("Critical")
    void handleInput_1_delegatesToImportController() {
        menuController.handleInput("1");

        verify(importExportController, times(1)).importData();
    }

    @Test
    @DisplayName("Input 2 delegates to ClassController")
    @Tag("TC 6.06")
    @Tag("Seth")
    @Tag("Critical")
    void handleInput_2_delegatesToClassController() {
        menuController.handleInput("2");

        verify(classController, times(1)).showAll();
    }

    @Test
    @DisplayName("Input 3 delegates to PreferenceService")
    @Tag("TC 6.07")
    @Tag("Seth")
    @Tag("Critical")
    void handleInput_3_delegatesToPreferenceService() {
        menuController.handleInput("3");

        verify(preferenceService, times(1)).managePreferences();
    }

    @Test
    @DisplayName("Input 4 delegates to TimetableController generate")
    @Tag("TC 6.08")
    @Tag("Seth")
    @Tag("Critical")
    void handleInput_4_delegatesToTimetableGenerate() {
        menuController.handleInput("4");

        verify(timetableController, times(1)).generate(null);
    }

    @Test
    @DisplayName("Input 5 delegates to TimetableController viewAll")
    @Tag("TC 6.09")
    @Tag("Seth")
    @Tag("Critical")
    void handleInput_5_delegatesToTimetableViewAll() {
        menuController.handleInput("5");

        verify(timetableController, times(1)).viewAll();
    }

    @Test
    @DisplayName("Input 6 returns exit message")
    @Tag("TC 6.10")
    @Tag("Seth")
    @Tag("Critical")
    void handleInput_6_returnsExitMessage() {
        String result = menuController.handleInput("6");

        assertEquals(MenuController.EXIT_MESSAGE, result);  // fixed typo: EXOT_MESSAGE -> EXIT_MESSAGE
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
        String result = menuController.handleInput("   ");  // fixed: single space may not trigger isBlank()

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

        verify(classController, times(1)).showAll();
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
        assertFalse(menuController.isExitInput("1"));  // removed duplicate line
        assertFalse(menuController.isExitInput(""));
        assertFalse(menuController.isExitInput(null));
    }
}