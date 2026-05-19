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
     * Prompts the user for a file path and data type (Topics or Classes),
     * delegates to CSVImportService, and reports import results.
     */
    public void importData() {
        System.out.println("\n── Import Data ──────────────────────────────");
        System.out.println("  1. Import Topics");
        System.out.println("  2. Import Classes");
        int choice = input.readInt(sc, "Select type (1-2): ", 1, 2);
        String path = input.readNonBlank(sc, "File path: ");

        if (choice == 1) {
            List<Topic> imported = importService.importTopics(path);
            view.printSuccess("Imported " + imported.size() + " topic(s) from " + path + ".");
        } else {
            List<ClassEntry> imported = importService.importClasses(path);
            view.printSuccess("Imported " + imported.size() + " class(es) from " + path + ".");
        }
    }
}
