package au.edu.flinders.timetable.controller;

import au.edu.flinders.timetable.model.Preference;
import au.edu.flinders.timetable.model.User;
import au.edu.flinders.timetable.service.PreferenceService;
import au.edu.flinders.timetable.ui.ConsoleView;
import au.edu.flinders.timetable.ui.InputHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/** Top-level controller that drives the main application loop. */
public class MenuController {

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
            int choice = input.readInt(sc, "Select option: ", 1, MENU_OPTIONS.length);
            System.out.println();
            switch (choice) {
                case 1:
                    importExportController.importData();
                    break;
                case 2:
                    handleClasses();
                    break;
                case 3:
                    handlePreferences();
                    break;
                case 4:
                    timetableController.generate(currentUser);
                    break;
                case 5:
                    handleViewExport();
                    break;
                case 6:
                    view.printSuccess("Goodbye!");
                    running = false;
                    break;
            }
            System.out.println();
        }
    }

    // ── Sub-menus ─────────────────────────────────────────────────────────────

    /** Classes sub-menu: search, view all, edit, delete. */
    private void handleClasses() {
        System.out.println("── Classes ──────────────────────────────────");
        System.out.println("  1. Search / browse classes");
        System.out.println("  2. View all classes (detailed)");
        System.out.println("  3. Edit a class");
        System.out.println("  4. Delete a class");
        int choice = input.readInt(sc, "Select: ", 1, 4);
        switch (choice) {
            case 1:
                classController.search();
                break;
            case 2:
                classController.viewAll();
                break;
            case 3:
                classController.editClass();
                break;
            case 4:
                classController.deleteClass();
                break;
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
        int choice = input.readInt(sc, "Select: ", 1, 5);
        switch (choice) {
            case 1:
                timetableController.viewAll();
                break;
            case 2:
                timetableController.view();
                break;
            case 3:
                timetableController.editTimetable();
                break;
            case 4:
                timetableController.export();
                break;
            case 5:
                timetableController.delete();
                break;
        }
    }

    /**
     * Preferences sub-menu.
     * Allows the user to view, set (token-based), or clear their preferences.
     * The "set" flow presents all 14 tokens; the user selects them one by one
     * in priority order (highest first), pressing Enter with no input to finish.
     */
    private void handlePreferences() {
        System.out.println("── Manage Preferences ───────────────────────");
        System.out.println("  1. View current preferences");
        System.out.println("  2. Set preferences");
        System.out.println("  3. Clear preferences");
        int choice = input.readInt(sc, "Select: ", 1, 3);

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
     * An empty line finishes selection. Validates and saves the resulting preference.
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
