package au.edu.flinders.timetable.controller;

import au.edu.flinders.timetable.model.ClassEntry;
import au.edu.flinders.timetable.model.Topic;
import au.edu.flinders.timetable.service.CSVImportService;
import au.edu.flinders.timetable.ui.ConsoleView;
import au.edu.flinders.timetable.ui.InputHelper;

import java.util.List;
import java.util.Scanner;

/** Routes data import actions to CSVImportService and reports results to the user. */
public class ImportExportController {

    private final CSVImportService importService;
    private final ConsoleView      view;
    private final InputHelper      input;
    private final Scanner          sc;

    /** Constructs the controller with its required dependencies. */
    public ImportExportController(CSVImportService importService,
                                   ConsoleView view,
                                   InputHelper input,
                                   Scanner sc) {
        this.importService = importService;
        this.view          = view;
        this.input         = input;
        this.sc            = sc;
    }

    /**
     * Prompts the user for a file path and data type, delegates to CSVImportService,
     * and reports import results. Supports three modes:
     * 1 = Topics CSV (synthetic format), 2 = Classes CSV (synthetic format),
     * 3 = University timetable export file (data/ folder format).
     */
    public void importData() {
        System.out.println("\n── Import Data ──────────────────────────────");
        System.out.println("  1. Import Topics (course list CSV)");
        System.out.println("  2. Import Classes (class list CSV)");
        System.out.println("  3. Import from university timetable file (data/ folder)");
        int choice = input.readInt(sc, "Select type (1-3): ", 1, 3);
        String path = input.readNonBlank(sc, "File path: ");

        switch (choice) {
            case 1 -> {
                List<Topic> imported = importService.importTopics(path);
                view.printSuccess("Imported " + imported.size() + " topic(s) from: " + path);
            }
            case 2 -> {
                List<ClassEntry> imported = importService.importClasses(path);
                view.printSuccess("Imported " + imported.size() + " class(es) from: " + path);
            }
            case 3 -> {
                CSVImportService.ImportResult result =
                    importService.importFromTimetableFile(path);
                view.printSuccess(
                    "Import complete — "
                    + result.newCount()     + " new, "
                    + result.updatedCount() + " updated  (" + path + ")");
            }
        }
    }
}
