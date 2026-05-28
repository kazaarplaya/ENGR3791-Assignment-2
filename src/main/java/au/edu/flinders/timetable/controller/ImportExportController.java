package au.edu.flinders.timetable.controller;

import au.edu.flinders.timetable.service.CSVImportService;
import au.edu.flinders.timetable.ui.ConsoleView;
import au.edu.flinders.timetable.ui.InputHelper;

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
     * Prompts for a path to a single CSV file or a folder of CSV files and imports all data.
     */
    public void importData() {
        System.out.println("\n── Import Data ──────────────────────────────");
        System.out.println("  Provide a path to a single .csv file or a folder of .csv files.");
        String path = input.readNonBlank(sc, "  Path (or q to return): ");
        CSVImportService.ImportResult result = importService.importFromPath(path);
        view.printSuccess("Import complete — "
            + result.newCount()     + " new, "
            + result.updatedCount() + " updated  (" + path + ")");
    }
}
