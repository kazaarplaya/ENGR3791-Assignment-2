package au.edu.flinders.timetable.controller;

import au.edu.flinders.timetable.model.ClassEntry;
import au.edu.flinders.timetable.model.Timetable;
import au.edu.flinders.timetable.model.User;
import au.edu.flinders.timetable.repository.ClassRepository;
import au.edu.flinders.timetable.service.CSVExportService;
import au.edu.flinders.timetable.service.TimetableGeneratorService;
import au.edu.flinders.timetable.service.TimetableService;
import au.edu.flinders.timetable.ui.ConsoleView;
import au.edu.flinders.timetable.ui.InputHelper;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Collectors;

/** Routes timetable management actions to TimetableGeneratorService and TimetableService. */
public class TimetableController {

    private final TimetableGeneratorService generatorService;
    private final TimetableService          timetableService;
    private final CSVExportService          exportService;
    private final ClassRepository           classRepository;
    private final ConsoleView               view;
    private final InputHelper               input;
    private final Scanner                   sc;

    /** Constructs the controller with all required dependencies. */
    public TimetableController(TimetableGeneratorService generatorService,
                                TimetableService timetableService,
                                CSVExportService exportService,
                                ClassRepository classRepository,
                                ConsoleView view,
                                InputHelper input,
                                Scanner sc) {
        this.generatorService = generatorService;
        this.timetableService = timetableService;
        this.exportService    = exportService;
        this.classRepository  = classRepository;
        this.view             = view;
        this.input            = input;
        this.sc               = sc;
    }

    /**
     * Interactive timetable generation flow: collects enrolled topics, campus selections,
     * overlap and preference toggles, then calls TimetableGeneratorService.
     */
    public void generate(User user) {
        System.out.println("\n── Generate Timetable ──────────────────────");

        // Collect enrolled topics
        System.out.println("Enter course codes to enrol (blank line to finish):");
        while (true) {
            String code = input.readLine(sc, "  Course code: ").trim();
            if (code.isEmpty()) break;
            user.enrol(code);
            System.out.println("  Enrolled in " + code);
        }

        if (user.getEnrolledTopics().isEmpty()) {
            view.printWarning("No topics enrolled. Returning to menu.");
            return;
        }

        // Collect per-topic campus selections
        Map<String, String> campusSelections = new HashMap<>();
        System.out.println("\nFor each topic, enter your preferred campus (blank = any campus):");
        for (String code : user.getEnrolledTopics()) {
            String campus = input.readLine(sc, "  Campus for " + code + ": ").trim();
            if (!campus.isBlank()) {
                campusSelections.put(code, campus);
            }
        }

        // Flags
        boolean overlap = input.readBoolean(sc, "\nAllow lecture overlap between campuses?");
        boolean prefs   = input.readBoolean(sc, "Apply your saved preferences?");

        // Optional name
        String name = input.readLine(sc, "Timetable name (blank = auto-generate): ").trim();

        try {
            Timetable t = generatorService.generate(user, campusSelections, overlap, prefs, name);
            view.printSuccess("Timetable '" + t.getTimetableName() + "' generated with "
                + t.getClassIds().size() + " class(es).");

            // Display the grid
            List<ClassEntry> resolved = t.getClassIds().stream()
                .map(classRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
            view.printTimetable(t, resolved);

        } catch (IllegalStateException | IllegalArgumentException e) {
            view.printError(e.getMessage());
        }
    }

    /** Lists the names and summaries of all saved timetables. */
    public void viewAll() {
        List<Timetable> all = timetableService.getAll();
        System.out.println("\n── Saved Timetables (" + all.size() + ") ──────────────────");
        if (all.isEmpty()) {
            System.out.println("  (none)");
            return;
        }
        for (int i = 0; i < all.size(); i++) {
            System.out.printf("  %d. %s%n", i + 1, all.get(i));
        }
    }

    /** Prompts for a timetable name and file path then exports to CSV. */
    public void export() {
        System.out.println("\n── Export Timetable ─────────────────────────");
        String name = input.readNonBlank(sc, "Timetable name to export: ");
        Optional<Timetable> opt = timetableService.getByName(name);
        if (opt.isEmpty()) {
            view.printError("No timetable named '" + name + "' found.");
            return;
        }
        String path = input.readNonBlank(sc, "Export file path (e.g. timetable.csv): ");
        try {
            exportService.exportTimetable(opt.get(), path);
            view.printSuccess("Timetable exported to " + path);
        } catch (IOException e) {
            view.printError("Export failed: " + e.getMessage());
        }
    }

    /** Prompts for a timetable name and deletes it from the repository. */
    public void delete() {
        System.out.println("\n── Delete Timetable ─────────────────────────");
        String name = input.readNonBlank(sc, "Timetable name to delete: ");
        try {
            timetableService.delete(name);
            view.printSuccess("Timetable '" + name + "' deleted.");
        } catch (IllegalArgumentException e) {
            view.printError(e.getMessage());
        }
    }
}
