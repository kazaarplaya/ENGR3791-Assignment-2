package au.edu.flinders.timetable.controller;

import au.edu.flinders.timetable.model.Preference;
import au.edu.flinders.timetable.model.User;
import au.edu.flinders.timetable.service.PreferenceService;
import au.edu.flinders.timetable.ui.ConsoleView;
import au.edu.flinders.timetable.ui.EarlyExitException;
import au.edu.flinders.timetable.ui.InputHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/** Top-level controller that drives the main application loop. */
public class MenuController {

    public static final String BANNER = "=== Flinders University Timetable System ===";

    static final String MENU_HEADER        = "=== Main Menu ===";
    static final String OPTION_IMPORT      = "1. Import Data";
    static final String OPTION_SEARCH      = "2. Classes";
    static final String OPTION_PREFERENCES = "3. Manage Preferences";
    static final String OPTION_GENERATE    = "4. Generate Timetable";
    static final String OPTION_VIEW_EXPORT = "5. View / Export Timetable";
    static final String OPTION_EXIT        = "6. Exit";
    static final String PROMPT             = "Select option: ";
    static final String INVALID_INPUT      = "Invalid option. Please enter 1-6.";
    static final String EXIT_MESSAGE       = "Goodbye!";

    private static final String[] MENU_OPTIONS = {
            "Import Data",
            "Classes",
            "Manage Preferences",
            "Generate Timetable",
            "View / Export Timetable",
            "Exit"
    };

    // All 14 valid preference tokens presented to the user with friendly labels.
    private static final String[][] PREF_TOKENS = {
            { Preference.CAMPUS_BEDFORD_PARK, "Campus: Bedford Park" },
            { Preference.CAMPUS_TONSLEY,      "Campus: Tonsley"      },
            { Preference.CAMPUS_CITY,         "Campus: City"         },
            { Preference.CAMPUS_SAME,         "Campus: Same campus"  },
            { Preference.TIME_MORNING,        "Time: Morning (before 12:00)" },
            { Preference.TIME_AFTERNOON,      "Time: Afternoon (12:00–17:00)" },
            { Preference.TIME_EVENING,        "Time: Evening (17:00+)" },
            { Preference.DAY_MONDAY,          "Day: Monday"    },
            { Preference.DAY_TUESDAY,         "Day: Tuesday"   },
            { Preference.DAY_WEDNESDAY,       "Day: Wednesday" },
            { Preference.DAY_THURSDAY,        "Day: Thursday"  },
            { Preference.DAY_FRIDAY,          "Day: Friday"    },
            { Preference.SPREAD,              "Spread classes across the week" },
            { Preference.COMPACT,             "Compact classes into fewer days" }
    };

    private final Scanner                sc;
    private final ConsoleView            view;
    private final InputHelper            input;
    private final ImportExportController importExportController;
    private final ClassController        classController;
    private final TimetableController    timetableController;
    private final PreferenceService      preferenceService;

    // Populated during start() after the user enters their details.
    private User currentUser;

    /** Constructs MenuController with all sub-controllers and shared dependencies. */
    public MenuController(Scanner sc,
                          ConsoleView view,
                          InputHelper input,
                          ImportExportController importExportController,
                          ClassController classController,
                          TimetableController timetableController,
                          PreferenceService preferenceService) {
        this.sc                     = sc;
        this.view                   = view;
        this.input                  = input;
        this.importExportController = importExportController;
        this.classController        = classController;
        this.timetableController    = timetableController;
        this.preferenceService      = preferenceService;
        this.currentUser            = null;
    }

    /** Secondary constructor used by tests. */
    public MenuController(ImportExportController importExportController,
                          ClassController classController,
                          PreferenceService preferenceService,
                          TimetableController timetableController,
                          Scanner scanner) {
        this.sc                     = scanner;
        this.view                   = null;
        this.input                  = null;
        this.importExportController = importExportController;
        this.classController        = classController;
        this.timetableController    = timetableController;
        this.preferenceService      = preferenceService;
        this.currentUser            = null;
    }

    /** Displays the banner, collects the user's identity, then runs the main menu loop. */
    public void start() {
        view.printBanner();
        System.out.println("  Please enter your details to begin.");
        String userId = input.readNonBlank(sc, "  Student ID : ");
        String name   = input.readNonBlank(sc, "  Your name  : ");
        currentUser   = new User(userId, name);
        view.printSuccess("Welcome, " + name + "!");
        System.out.println();

        boolean running = true;
        while (running) {
            view.printMenu(MENU_OPTIONS);
            System.out.println("  (Enter 'q' at any prompt to return to the main menu.)\n");
            try {
                int choice = input.readInt(sc, "Select option: ", 1, MENU_OPTIONS.length);
                System.out.println();
                switch (choice) {
                    case 1: importExportController.importData(); break;
                    case 2: handleClasses();                     break;
                    case 3: handlePreferences();                 break;
                    case 4: timetableController.generate(currentUser); break;
                    case 5: handleViewExport();                  break;
                    case 6: view.printSuccess("Goodbye!"); running = false; break;
                }
            } catch (EarlyExitException e) {
                System.out.println("\nReturning to main menu.");
            }
            System.out.println();
        }
    }

    /** Returns the main menu as a formatted string. Used by tests. */
    public String displayMenu() {
        return MENU_HEADER        + "\n" +
                OPTION_IMPORT      + "\n" +
                OPTION_SEARCH      + "\n" +
                OPTION_PREFERENCES + "\n" +
                OPTION_GENERATE    + "\n" +
                OPTION_VIEW_EXPORT + "\n" +
                OPTION_EXIT        + "\n" +
                PROMPT;
    }

    /** Processes a single menu input and returns the result message. Used by tests. */
    public String handleInput(String input) {
        if (input == null || input.isBlank()) return INVALID_INPUT;
        switch (input.trim()) {
            case "1": importExportController.importData();        return "Import complete.";
            case "2": classController.showAll();                  return "Search complete.";
            case "3": preferenceService.managePreferences();      return "Preferences updated.";
            case "4": timetableController.generate(currentUser);  return "Timetable generated.";
            case "5": timetableController.viewAll();              return "View / export complete.";
            case "6": return EXIT_MESSAGE;
            default:  return INVALID_INPUT;
        }
    }

    /** Returns true when the given input should exit the application. */
    public boolean isExitInput(String input) {
        return input != null && input.trim().equals("6");
    }

    // ── Sub-menus ─────────────────────────────────────────────────────────────

    /** Classes sub-menu: search, view all, view all detailed, edit, delete. */
    private void handleClasses() {
        System.out.println("── Classes ──────────────────────────────────");
        System.out.println("  1. Search / browse classes");
        System.out.println("  2. View all classes");
        System.out.println("  3. View all classes (detailed)");
        System.out.println("  4. Edit a class");
        System.out.println("  5. Delete a class");
        int choice = input.readInt(sc, "Select (or q to return): ", 1, 5);
        switch (choice) {
            case 1: classController.search();      break;
            case 2: classController.viewAll();     break;
            case 3: classController.viewAllDetailed(); break;
            case 4: classController.editClass();   break;
            case 5: classController.deleteClass(); break;
        }
    }

    /** View / Export sub-menu: list, view grid, edit/swap, export, delete. */
    private void handleViewExport() {
        System.out.println("── View / Export Timetable ──────────────────");
        System.out.println("  1. List all timetables");
        System.out.println("  2. View timetable (grid)");
        System.out.println("  3. Edit timetable (swap class instance)");
        System.out.println("  4. Export timetable to CSV");
        System.out.println("  5. Delete a timetable");
        int choice = input.readInt(sc, "Select (or q to return): ", 1, 5);
        switch (choice) {
            case 1: timetableController.viewAll();        break;
            case 2: timetableController.view();           break;
            case 3: timetableController.editTimetable();  break;
            case 4: timetableController.export();         break;
            case 5: timetableController.delete();         break;
        }
    }

    /**
     * Preferences sub-menu.
     * Allows the user to view, set (token-based), or clear their preferences.
     */
    private void handlePreferences() {
        System.out.println("── Manage Preferences ───────────────────────");
        System.out.println("  1. View current preferences");
        System.out.println("  2. Set preferences");
        System.out.println("  3. Clear preferences");
        int choice = input.readInt(sc, "Select (or q to return): ", 1, 3);

        switch (choice) {
            case 1:
                preferenceService.getPreference(currentUser.getUserId())
                        .ifPresentOrElse(
                                p -> System.out.println("  " + p),
                                () -> System.out.println("  No preferences set.")
                        );
                break;
            case 2:
                setPreferencesTokenMode();
                break;
            case 3:
                preferenceService.clearPreference(currentUser.getUserId());
                view.printSuccess("Preferences cleared.");
                break;
        }
    }

    /**
     * Token-based preference builder.
     * Shows a numbered list of all 14 tokens; user enters numbers in priority order.
     */
    private void setPreferencesTokenMode() {
        System.out.println("\n  Available preference tokens (enter numbers in priority order):");
        System.out.println("  Press Enter with no input when done.\n");

        for (int i = 0; i < PREF_TOKENS.length; i++) {
            System.out.printf("  %2d. %s%n", i + 1, PREF_TOKENS[i][1]);
        }
        System.out.println();

        List<String> selected = new ArrayList<>();
        boolean[] used = new boolean[PREF_TOKENS.length];

        while (true) {
            String raw = input.readLine(sc,
                    "  Add token #" + (selected.size() + 1) + " (or Enter to finish): ").trim();
            if (raw.isEmpty()) break;

            int idx;
            try {
                idx = Integer.parseInt(raw) - 1;
            } catch (NumberFormatException e) {
                view.printWarning("Please enter a number between 1 and " + PREF_TOKENS.length + ".");
                continue;
            }

            if (idx < 0 || idx >= PREF_TOKENS.length) {
                view.printWarning("Number out of range. Enter 1–" + PREF_TOKENS.length + ".");
                continue;
            }
            if (used[idx]) {
                view.printWarning("Token already selected. Choose a different one.");
                continue;
            }

            used[idx] = true;
            selected.add(PREF_TOKENS[idx][0]);
            System.out.println("  Added: " + PREF_TOKENS[idx][1]);
        }

        if (selected.isEmpty()) {
            view.printWarning("No tokens selected; preferences unchanged.");
            return;
        }

        try {
            Preference pref = new Preference(currentUser.getUserId(), selected);
            preferenceService.savePreference(pref);
            view.printSuccess("Preferences saved. Priority order: " + selected);
        } catch (IllegalArgumentException e) {
            view.printError(e.getMessage());
        }
    }
}