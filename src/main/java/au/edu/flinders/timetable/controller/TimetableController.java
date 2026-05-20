package au.edu.flinders.timetable.controller;

import au.edu.flinders.timetable.model.ClassEntry;
import au.edu.flinders.timetable.model.Timetable;
import au.edu.flinders.timetable.model.User;
import au.edu.flinders.timetable.repository.ClassRepository;
import au.edu.flinders.timetable.service.CSVExportService;
import au.edu.flinders.timetable.service.ClassService;
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
import java.util.TreeMap;
import java.util.stream.Collectors;

/** Routes timetable management actions to TimetableGeneratorService and TimetableService. */
public class TimetableController {

    private final TimetableGeneratorService generatorService;
    private final TimetableService          timetableService;
    private final CSVExportService          exportService;
    private final ClassRepository           classRepository;
    private final ClassService              classService;
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
                                ClassService classService,
                                ConsoleView view,
                                InputHelper input,
                                Scanner sc) {
        this.generatorService = generatorService;
        this.timetableService = timetableService;
        this.exportService    = exportService;
        this.classRepository  = classRepository;
        this.classService     = classService;
        this.view             = view;
        this.input            = input;
        this.sc               = sc;
    }

    // ── Public actions ────────────────────────────────────────────────────────

    /**
     * Interactive timetable generation flow.
     * <ol>
     *   <li>Collects enrolled topics and per-topic campus preferences.</li>
     *   <li>Prompts the student to select exactly ONE instance of each class type
     *       (Lecture, Workshop, Laboratory, etc.) for every enrolled topic.</li>
     *   <li>Runs clash detection and builds the timetable from the explicit selections.</li>
     * </ol>
     */
    public void generate(User user) {
        System.out.println("\n── Generate Timetable ──────────────────────");

        // ── Step 1: Collect enrolled topics ───────────────────────────────────
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

        // ── Step 2: Collect per-topic campus preferences ───────────────────────
        Map<String, String> campusSelections = new HashMap<>();
        System.out.println("\nFor each topic, enter your preferred campus (blank = any campus):");
        for (String code : user.getEnrolledTopics()) {
            String campus = input.readLine(sc, "  Campus for " + code + ": ").trim();
            if (!campus.isBlank()) {
                campusSelections.put(code, campus);
            }
        }

        // ── Step 3: Semester filter ────────────────────────────────────────────
        System.out.println("\nSemester filter: 1 = Semester 1, 2 = Semester 2, 0 = both");
        int semester = input.readInt(sc, "  Semester (0/1/2): ", 0, 2);

        // ── Step 4: Interactive class-instance selection ───────────────────────
        System.out.println("\n── Select Classes ──────────────────────────────────");
        System.out.println("For each enrolled topic, choose one class instance");
        System.out.println("per type (Lecture, Workshop, Laboratory, etc.).\n");

        List<ClassEntry> explicitSelections = new ArrayList<>();

        for (String courseCode : user.getEnrolledTopics()) {

            // Get one representative ClassEntry per (courseCode, type, instance) group
            List<ClassEntry> grouped = classService.getGroupedClassesForTopic(courseCode);

            // Apply campus filter if the student stated a campus preference
            String selectedCampus = campusSelections.getOrDefault(courseCode, "");
            if (!selectedCampus.isBlank()) {
                boolean citySelected = selectedCampus.equalsIgnoreCase("City")
                        || selectedCampus.equalsIgnoreCase("Flinders City Campus");
                List<ClassEntry> filtered = grouped.stream()
                    .filter(c -> {
                        String classCampus = inferCampusFromBuilding(c.getBuilding());
                        return citySelected
                            ? classCampus.equalsIgnoreCase("City")
                            : !classCampus.equalsIgnoreCase("City");
                    })
                    .collect(Collectors.toList());
                if (!filtered.isEmpty()) grouped = filtered;
                // If filtered is empty, fall back to the unfiltered list (no matching classes)
            }

            // Group by class type, sorted alphabetically for consistent presentation
            Map<String, List<ClassEntry>> byType = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            for (ClassEntry c : grouped) {
                byType.computeIfAbsent(c.getType(), k -> new ArrayList<>()).add(c);
            }

            if (byType.isEmpty()) {
                view.printWarning("No classes found for " + courseCode
                    + " with the selected campus. Skipping topic.");
                continue;
            }

            System.out.println("  ── " + courseCode + " ─────────────────────────────────────");

            for (Map.Entry<String, List<ClassEntry>> typeEntry : byType.entrySet()) {
                String type          = typeEntry.getKey();
                List<ClassEntry> options = typeEntry.getValue();

                System.out.println("  ── " + courseCode + " › " + type + " ──────────────");

                if (options.size() == 1) {
                    // Only one option — auto-select and inform the student
                    ClassEntry auto = options.get(0);
                    System.out.printf("  Auto-selected: Group #%d | %s %s–%s | %s %s%n",
                        auto.getClassInstance(),
                        auto.getDay()       != null ? auto.getDay()       : "---",
                        auto.getStartTime() != null ? auto.getStartTime() : "",
                        auto.getEndTime()   != null ? auto.getEndTime()   : "",
                        auto.getBuilding()  != null ? auto.getBuilding()  : "",
                        auto.getRoom()      != null ? auto.getRoom()      : "");
                    explicitSelections.add(auto);
                } else {
                    // Multiple options — list them and prompt
                    for (int i = 0; i < options.size(); i++) {
                        ClassEntry opt = options.get(i);
                        System.out.printf("  %d. Group #%d | %s %s–%s | %s %s%n",
                            i + 1,
                            opt.getClassInstance(),
                            opt.getDay()       != null ? opt.getDay()       : "---",
                            opt.getStartTime() != null ? opt.getStartTime() : "",
                            opt.getEndTime()   != null ? opt.getEndTime()   : "",
                            opt.getBuilding()  != null ? opt.getBuilding()  : "",
                            opt.getRoom()      != null ? opt.getRoom()      : "");
                    }
                    int choice = input.readInt(sc, "  Select " + type + ": ", 1, options.size());
                    explicitSelections.add(options.get(choice - 1));
                }
            }
        }

        // ── Step 5: Overlap and preferences ───────────────────────────────────
        boolean overlap = input.readBoolean(sc, "\nAllow lecture overlap between campuses?");
        boolean prefs   = input.readBoolean(sc, "Apply your saved preferences?");
        String  name    = input.readLine(sc, "Timetable name (blank = auto-generate): ").trim();

        // Persist last settings
        lastSemester         = semester;
        lastOverlap          = overlap;
        lastUsePrefs         = prefs;
        lastEnrolments       = new ArrayList<>(newEnrolments);
        lastCampusSelections = new HashMap<>(campusSelections);

        if (explicitSelections.isEmpty()) {
            view.printWarning("No classes were selected. Timetable not generated.");
            return;
        }

        try {
            Timetable t = generatorService.generateFromSelections(
                user, explicitSelections, overlap, prefs, name, semester);
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

    /**
     * Infers the campus label from a building name string.
     * Festival Tower → City; "tonsley" prefix → Tonsley; everything else → Bedford Park.
     */
    private static String inferCampusFromBuilding(String building) {
        if (building == null) return "Bedford Park";
        String b = building.toLowerCase();
        if (b.contains("festival tower")) return "City";
        if (b.contains("tonsley"))        return "Tonsley";
        return "Bedford Park";
    }
}
