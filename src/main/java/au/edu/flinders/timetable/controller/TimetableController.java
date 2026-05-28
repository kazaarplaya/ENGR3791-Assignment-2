package au.edu.flinders.timetable.controller;

import au.edu.flinders.timetable.model.ClassEntry;
import au.edu.flinders.timetable.model.Timetable;
import au.edu.flinders.timetable.model.Topic;
import au.edu.flinders.timetable.model.User;
import au.edu.flinders.timetable.repository.ClassRepository;
import au.edu.flinders.timetable.repository.TopicRepository;
import au.edu.flinders.timetable.service.CSVExportService;
import au.edu.flinders.timetable.service.ClassService;
import au.edu.flinders.timetable.service.TimetableGeneratorService;
import au.edu.flinders.timetable.service.TimetableService;
import au.edu.flinders.timetable.ui.ConsoleView;
import au.edu.flinders.timetable.ui.InputHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
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
    private final TopicRepository           topicRepository;
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
                                TopicRepository topicRepository,
                                ClassService classService,
                                ConsoleView view,
                                InputHelper input,
                                Scanner sc) {
        this.generatorService = generatorService;
        this.timetableService = timetableService;
        this.exportService    = exportService;
        this.classRepository  = classRepository;
        this.topicRepository  = topicRepository;
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
        List<Topic> availableTopics = topicRepository.findAll().stream()
            .sorted(Comparator.comparing(Topic::getCourseCode))
            .collect(Collectors.toList());

        System.out.printf("%nAvailable courses (%d):%n%n", availableTopics.size());
        for (int i = 0; i < availableTopics.size(); i++) {
            Topic t = availableTopics.get(i);
            System.out.printf("  [%2d]  %-12s  %s%n", i + 1, t.getCourseCode(), t.getTopicName());
        }

        System.out.println("\nEnter a course code or number (comma-separated for multiple).");
        System.out.println("Type 'done' to finish or 'q' to return to the main menu.");
        List<String> newEnrolments = new ArrayList<>();
        while (true) {
            String raw = input.readLine(sc, "\nEnter course code(s) or number(s): ").trim();
            if (raw.equalsIgnoreCase("done")) break;
            if (raw.isEmpty()) {
                System.out.println("  Please enter a course code or number, type 'done' to finish, or 'q' to return.");
                continue;
            }

            for (String part : raw.split(",")) {
                String token = part.trim();
                if (token.isEmpty()) continue;

                // Resolve numeric index to course code
                String code = token;
                try {
                    int idx = Integer.parseInt(token);
                    if (idx < 1 || idx > availableTopics.size()) {
                        System.out.println("  Number " + idx + " is out of range (1–" + availableTopics.size() + ").");
                        continue;
                    }
                    code = availableTopics.get(idx - 1).getCourseCode();
                } catch (NumberFormatException ignored) {
                    // not a number — treat as course code string
                }

                if (classRepository.findByCourseCode(code).isEmpty()) {
                    System.out.println("  Course code '" + code + "' is invalid or unavailable.");
                    System.out.println("  Enter a valid course code or number, type 'done' to finish, or 'q' to return.");
                } else if (user.isEnrolled(code)) {
                    System.out.println("  Already enrolled in " + code + ".");
                } else {
                    user.enrol(code);
                    newEnrolments.add(code);
                    System.out.println("  Enrolled in " + code + ".");
                }
            }
        }

        // ── Step 2: Collect per-topic campus preferences ───────────────────────
        Map<String, String> campusSelections = new HashMap<>();
        System.out.println("\nFor each enrolled topic, select your preferred campus.");
        for (String code : user.getEnrolledTopics()) {
            List<String> campuses = classRepository.findByCourseCode(code).stream()
                .map(c -> inferCampusFromBuilding(c.getBuilding()))
                .distinct()
                .sorted()
                .collect(Collectors.toList());

            System.out.println("\n  Campuses available for " + code + ":");
            for (int i = 0; i < campuses.size(); i++) {
                System.out.printf("    [%d] %s%n", i + 1, campuses.get(i));
            }
            System.out.println("    [Enter] Any campus");

            while (true) {
                String raw = input.readLine(sc, "  Select campus: ").trim();
                if (raw.isEmpty()) break;

                String selected = null;
                try {
                    int idx = Integer.parseInt(raw);
                    if (idx >= 1 && idx <= campuses.size()) {
                        selected = campuses.get(idx - 1);
                    } else {
                        System.out.println("  Number out of range (1–" + campuses.size() + ").");
                        continue;
                    }
                } catch (NumberFormatException ignored) {
                    String finalRaw = raw;
                    Optional<String> match = campuses.stream()
                        .filter(c -> c.equalsIgnoreCase(finalRaw))
                        .findFirst();
                    if (match.isPresent()) {
                        selected = match.get();
                    } else {
                        System.out.println("  '" + raw + "' is not available for " + code + ". Choose from the list or press Enter for any.");
                        continue;
                    }
                }
                campusSelections.put(code, selected);
                System.out.println("  Selected: " + selected);
                break;
            }
        }

        // ── Step 3: Semester filter ────────────────────────────────────────────
        System.out.println("\nSemester:");
        System.out.println("  [1] Semester 1");
        System.out.println("  [2] Semester 2");
        System.out.println("  [Enter] Both semesters");

        int semester;
        while (true) {
            String raw = input.readLine(sc, "  Select (1, 2, or Enter for both): ").trim();
            if (raw.isEmpty())    { semester = 0; break; }
            if (raw.equals("1")) { semester = 1; break; }
            if (raw.equals("2")) { semester = 2; break; }
            System.out.println("  Please enter 1, 2, or press Enter for both.");
        }

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

        if (!input.readBoolean(sc, "\nGenerate timetable with these selections?")) {
            System.out.println("  Generation cancelled.");
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
     * Lists all saved timetables by name, prompts the user to select one,
     * then displays its weekly grid and included classes.
     */
    public void view() {
        System.out.println("\n── View Timetable ───────────────────────────");
        Timetable t = pickTimetable();
        if (t == null) return;
        view.printTimetable(t, resolveClasses(t));
    }

    /**
     * Lists saved timetables, lets the user pick one, then guides them through
     * selecting a class to remove and a replacement via numbered lists.
     */
    public void editTimetable() {
        System.out.println("\n── Edit Timetable ───────────────────────────");
        Timetable t = pickTimetable();
        if (t == null) return;
        String timetableName = t.getTimetableName();

        List<ClassEntry> current = resolveClasses(t);
        view.printTimetable(t, current);

        if (current.isEmpty()) {
            view.printWarning("This timetable has no classes.");
            return;
        }

        // ── Step 1: pick class to remove ──────────────────────────────────────
        System.out.println("  Select class to remove:");
        for (int i = 0; i < current.size(); i++) {
            ClassEntry c = current.get(i);
            System.out.printf("  [%2d]  %-10s  %-12s #%d  %s  %s–%s%n",
                i + 1,
                c.getCourseCode(),
                c.getType(),
                c.getClassInstance(),
                c.getDay() != null ? c.getDay().toString().substring(0, 3) : "---",
                c.getStartTime() != null ? c.getStartTime() : "",
                c.getEndTime()   != null ? c.getEndTime()   : "");
        }
        int removeIdx = input.readInt(sc, "  Select: ", 1, current.size());
        ClassEntry oldEntry = current.get(removeIdx - 1);

        // ── Step 2: pick replacement (same courseCode + type, different instance) ──
        List<ClassEntry> alternatives = classService
            .getGroupedClassesForTopic(oldEntry.getCourseCode()).stream()
            .filter(c -> c.getType().equalsIgnoreCase(oldEntry.getType()))
            .filter(c -> !c.getClassId().equals(oldEntry.getClassId()))
            .collect(Collectors.toList());

        if (alternatives.isEmpty()) {
            view.printWarning("No alternative instances available for "
                + oldEntry.getCourseCode() + " " + oldEntry.getType() + ".");
            return;
        }

        System.out.println("\n  Replacement options for "
            + oldEntry.getCourseCode() + " " + oldEntry.getType() + ":");
        for (int i = 0; i < alternatives.size(); i++) {
            ClassEntry c = alternatives.get(i);
            System.out.printf("  [%2d]  Group #%d  %s  %s–%s  %s %s%n",
                i + 1,
                c.getClassInstance(),
                c.getDay()       != null ? c.getDay().toString().substring(0, 3) : "---",
                c.getStartTime() != null ? c.getStartTime() : "",
                c.getEndTime()   != null ? c.getEndTime()   : "",
                c.getBuilding()  != null ? c.getBuilding()  : "",
                c.getRoom()      != null ? c.getRoom()      : "");
        }
        int addIdx = input.readInt(sc, "  Select: ", 1, alternatives.size());
        ClassEntry newEntry = alternatives.get(addIdx - 1);

        // ── Confirm ────────────────────────────────────────────────────────────
        System.out.printf("%n  Remove: %s %s Group #%d%n",
            oldEntry.getCourseCode(), oldEntry.getType(), oldEntry.getClassInstance());
        System.out.printf("  Add:    %s %s Group #%d%n",
            newEntry.getCourseCode(), newEntry.getType(), newEntry.getClassInstance());
        if (!input.readBoolean(sc, "  Confirm swap?")) {
            System.out.println("  Edit cancelled.");
            return;
        }

        try {
            timetableService.swapClassInstance(timetableName, oldEntry.getClassId(), newEntry.getClassId());
            view.printSuccess("Swapped " + oldEntry.getCourseCode() + " " + oldEntry.getType()
                + " Group #" + oldEntry.getClassInstance()
                + " → Group #" + newEntry.getClassInstance() + ".");
            timetableService.getByName(timetableName).ifPresent(
                updated -> view.printTimetable(updated, resolveClasses(updated)));
        } catch (IllegalArgumentException e) {
            view.printError(e.getMessage());
        }
    }

    /** Lists the names of all saved timetables. */
    public void viewAll() {
        List<Timetable> all = timetableService.getAll();
        System.out.println("\n── Saved Timetables (" + all.size() + ") ──────────────────");
        if (all.isEmpty()) {
            System.out.println("  (none)");
            return;
        }
        for (int i = 0; i < all.size(); i++) {
            System.out.printf("  %d. %s%n", i + 1, all.get(i).getTimetableName());
        }
    }

    /** Lists saved timetables, lets the user pick one, then exports it to CSV. */
    public void export() {
        System.out.println("\n── Export Timetable ─────────────────────────");
        Timetable t = pickTimetable();
        if (t == null) return;

        String nameRaw = input.readLine(sc, "  Export name [" + t.getTimetableName() + "]: ").trim();
        String exportName = nameRaw.isEmpty() ? t.getTimetableName() : nameRaw;

        String defaultPath = exportName + ".csv";
        String pathRaw = input.readLine(sc, "  Export file path [" + defaultPath + "]: ").trim();
        String path = pathRaw.isEmpty() ? defaultPath : pathRaw;

        try {
            exportService.exportTimetable(t, path);
            view.printSuccess("Timetable exported to " + path);
        } catch (IOException e) {
            view.printError("Export failed: " + e.getMessage());
        }
    }

    /** Lists saved timetables, lets the user pick one, then deletes it after confirmation. */
    public void delete() {
        System.out.println("\n── Delete Timetable ─────────────────────────");
        Timetable t = pickTimetable();
        if (t == null) return;
        if (!input.readBoolean(sc, "  Confirm delete '" + t.getTimetableName() + "'?")) {
            System.out.println("  Delete cancelled.");
            return;
        }
        try {
            timetableService.delete(t.getTimetableName());
            view.printSuccess("Timetable '" + t.getTimetableName() + "' deleted.");
        } catch (IllegalArgumentException e) {
            view.printError(e.getMessage());
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Lists all saved timetables by name with numbers and prompts the user to select one.
     * Returns the selected Timetable, or null if there are none or the user cancels.
     */
    private Timetable pickTimetable() {
        List<Timetable> all = timetableService.getAll();
        if (all.isEmpty()) {
            view.printWarning("No saved timetables found.");
            return null;
        }
        System.out.println();
        for (int i = 0; i < all.size(); i++) {
            System.out.printf("  [%2d]  %s%n", i + 1, all.get(i).getTimetableName());
        }
        System.out.println();
        int choice = input.readInt(sc, "  Select timetable: ", 1, all.size());
        return all.get(choice - 1);
    }

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
