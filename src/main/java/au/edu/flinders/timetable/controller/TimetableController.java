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
import java.util.ArrayList;
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

    // ── Last-generation settings (persisted for "regenerate" convenience) ─────
    private int                 lastSemester         = 0;
    private boolean             lastOverlap          = false;
    private boolean             lastUsePrefs         = false;
    private List<String>        lastEnrolments       = new ArrayList<>();
    private Map<String, String> lastCampusSelections = new HashMap<>();

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

    // ── Public actions ────────────────────────────────────────────────────────

    /**
     * Interactive timetable generation flow.
     * Prompts for enrolled topics, per-topic campus selections, semester filter,
     * overlap and preference toggles, then generates and saves a timetable.
     */
    public void generate(User user) {
        System.out.println("\n── Generate Timetable ──────────────────────");

        // Collect enrolled topics
        System.out.println("Enter course codes to enrol (blank line to finish):");
        List<String> newEnrolments = new ArrayList<>();
        while (true) {
            String code = input.readLine(sc, "  Course code: ").trim();
            if (code.isEmpty()) break;
            user.enrol(code);
            newEnrolments.add(code);
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

        // Semester filter
        System.out.println("\nSemester filter: 1 = Semester 1, 2 = Semester 2, 0 = both");
        int semester = input.readInt(sc, "  Semester (0/1/2): ", 0, 2);

        // Flags
        boolean overlap = input.readBoolean(sc, "\nAllow lecture overlap between campuses?");
        boolean prefs   = input.readBoolean(sc, "Apply your saved preferences?");

        // Optional name
        String name = input.readLine(sc, "Timetable name (blank = auto-generate): ").trim();

        // Persist last settings
        lastSemester         = semester;
        lastOverlap          = overlap;
        lastUsePrefs         = prefs;
        lastEnrolments       = new ArrayList<>(newEnrolments);
        lastCampusSelections = new HashMap<>(campusSelections);

        try {
            Timetable t = generatorService.generate(
                user, campusSelections, overlap, prefs, name, semester);
            view.printSuccess("Timetable '" + t.getTimetableName() + "' generated with "
                + t.getClassIds().size() + " class(es).");
            view.printTimetable(t, resolveClasses(t));
        } catch (IllegalStateException | IllegalArgumentException e) {
            view.printError(e.getMessage());
        }
    }

    /**
     * Prompts for a timetable name and displays its weekly grid along with
     * a detailed list of all included classes.
     */
    public void view() {
        System.out.println("\n── View Timetable ───────────────────────────");
        String name = input.readNonBlank(sc, "Timetable name: ");
        Optional<Timetable> opt = timetableService.getByName(name);
        if (opt.isEmpty()) {
            view.printError("No timetable named '" + name + "' found.");
            return;
        }
        Timetable t = opt.get();
        view.printTimetable(t, resolveClasses(t));
    }

    /**
     * Prompts for a timetable name and allows swapping one class instance for another.
     * Both classes must share the same courseCode and classType.
     */
    public void editTimetable() {
        System.out.println("\n── Edit Timetable ───────────────────────────");
        String timetableName = input.readNonBlank(sc, "Timetable name: ");
        Optional<Timetable> opt = timetableService.getByName(timetableName);
        if (opt.isEmpty()) {
            view.printError("No timetable named '" + timetableName + "' found.");
            return;
        }

        Timetable t = opt.get();
        System.out.println("  Current classes: " + t.getClassIds());
        view.printTimetable(t, resolveClasses(t));

        String oldClassId = input.readNonBlank(sc, "Class ID to remove: ");
        String newClassId = input.readNonBlank(sc, "Class ID to add    : ");

        try {
            timetableService.swapClassInstance(timetableName, oldClassId, newClassId);
            view.printSuccess("Swapped '" + oldClassId + "' → '" + newClassId + "'.");
            // Show updated grid
            timetableService.getByName(timetableName).ifPresent(
                updated -> view.printTimetable(updated, resolveClasses(updated)));
        } catch (IllegalArgumentException e) {
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

    // ── Private helpers ───────────────────────────────────────────────────────

    /** Resolves the class IDs in a timetable to their full ClassEntry objects. */
    private List<ClassEntry> resolveClasses(Timetable t) {
        return t.getClassIds().stream()
            .map(classRepository::findById)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    }
}
