package au.edu.flinders.timetable.controller;

import au.edu.flinders.timetable.model.User;
import au.edu.flinders.timetable.model.Preference;
import au.edu.flinders.timetable.service.PreferenceService;
import au.edu.flinders.timetable.ui.ConsoleView;
import au.edu.flinders.timetable.ui.InputHelper;

import java.util.Scanner;

/** Top-level controller that drives the main application loop. */
public class MenuController {

    private static final String[] MENU_OPTIONS = {
        "Import Data",
        "Search Classes",
        "Manage Preferences",
        "Generate Timetable",
        "View / Export Timetable",
        "Exit"
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
                case 1 -> importExportController.importData();
                case 2 -> handleSearch();
                case 3 -> handlePreferences();
                case 4 -> timetableController.generate(currentUser);
                case 5 -> handleViewExport();
                case 6 -> {
                    view.printSuccess("Goodbye!");
                    running = false;
                }
            }
            System.out.println();
        }
    }

    /** Prompts for a search query and delegates to ClassController. */
    private void handleSearch() {
        System.out.println("── Search Classes ───────────────────────────");
        System.out.println("  1. Search by keyword");
        System.out.println("  2. Show all classes");
        int choice = input.readInt(sc, "Select: ", 1, 2);
        if (choice == 1) {
            String query = input.readLine(sc, "Search query: ").trim();
            classController.search(query);
        } else {
            classController.showAll();
        }
    }

    /** Prompts the user to view, set, or clear their preferences. */
    private void handlePreferences() {
        System.out.println("── Manage Preferences ───────────────────────");
        System.out.println("  1. View current preferences");
        System.out.println("  2. Set preferences");
        System.out.println("  3. Clear preferences");
        int choice = input.readInt(sc, "Select: ", 1, 3);

        switch (choice) {
            case 1 -> {
                preferenceService.getPreference(currentUser.getUserId())
                    .ifPresentOrElse(
                        p -> System.out.println("  " + p),
                        () -> System.out.println("  No preferences set.")
                    );
            }
            case 2 -> {
                String campus    = input.readLine(sc, "Preferred campus (blank = Any): ").trim();
                String timeOfDay = input.readLine(sc, "Time of day (Morning/Afternoon/Evening/Any): ").trim();
                String day       = input.readLine(sc, "Preferred day (Monday-Friday/Any): ").trim();

                if (campus.isBlank())    campus    = "Any";
                if (timeOfDay.isBlank()) timeOfDay = "Any";
                if (day.isBlank())       day       = "Any";

                System.out.println("  Assign a priority to each criterion (1 = most important).");
                int cp = input.readInt(sc, "  Campus priority:      ", 1, 3);
                int tp = input.readInt(sc, "  Time-of-day priority: ", 1, 3);
                int dp = input.readInt(sc, "  Day priority:         ", 1, 3);

                try {
                    Preference pref = new Preference(
                        currentUser.getUserId(), campus, timeOfDay, day, cp, tp, dp);
                    preferenceService.savePreference(pref);
                    view.printSuccess("Preferences saved.");
                } catch (IllegalArgumentException e) {
                    view.printError(e.getMessage());
                }
            }
            case 3 -> {
                preferenceService.clearPreference(currentUser.getUserId());
                view.printSuccess("Preferences cleared.");
            }
        }
    }

    /** Prompts the user to view all timetables, view a grid, export, or delete. */
    private void handleViewExport() {
        System.out.println("── View / Export Timetable ──────────────────");
        System.out.println("  1. List all timetables");
        System.out.println("  2. Export timetable to CSV");
        System.out.println("  3. Delete a timetable");
        int choice = input.readInt(sc, "Select: ", 1, 3);
        switch (choice) {
            case 1 -> timetableController.viewAll();
            case 2 -> timetableController.export();
            case 3 -> timetableController.delete();
        }
    }
}
