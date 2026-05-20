package au.edu.flinders.timetable.controller;

import au.edu.flinders.timetable.model.ClassEntry;
import au.edu.flinders.timetable.model.Topic;
import au.edu.flinders.timetable.repository.TopicRepository;
import au.edu.flinders.timetable.service.ClassService;
import au.edu.flinders.timetable.service.SearchCriteria;
import au.edu.flinders.timetable.service.SearchService;
import au.edu.flinders.timetable.ui.ConsoleView;
import au.edu.flinders.timetable.ui.InputHelper;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;

/** Routes class-related user actions to ClassService and SearchService. */
public class ClassController {

    private final ClassService    classService;
    private final SearchService   searchService;
    private final TopicRepository topicRepository; // nullable
    private final ConsoleView     view;
    private final InputHelper     input;
    private final Scanner         sc;

    /**
     * Backward-compatible 3-argument constructor.
     * topicRepository is null; InputHelper/Scanner use defaults.
     * Preserves any code that wires the old constructor signature.
     */
    public ClassController(ClassService classService, SearchService searchService,
                           ConsoleView view) {
        this(classService, searchService, null, view, new InputHelper(),
             new Scanner(System.in));
    }

    /** Full constructor used in production wiring via Main. */
    public ClassController(ClassService classService, SearchService searchService,
                           TopicRepository topicRepository,
                           ConsoleView view, InputHelper input, Scanner sc) {
        this.classService    = classService;
        this.searchService   = searchService;
        this.topicRepository = topicRepository;
        this.view            = view;
        this.input           = input;
        this.sc              = sc;
    }

    // ── Public actions ────────────────────────────────────────────────────────

    /**
     * Displays all grouped classes in a compact browse-style list.
     * Groups are formed by (courseCode, attendanceMode, availabilityNumber, type, instance).
     */
    public void showAll() {
        List<ClassEntry> grouped = classService.getGroupedClasses();
        if (grouped.isEmpty()) {
            view.printWarning("No classes have been imported yet.");
            return;
        }
        view.printBrowseList(grouped, buildTopicMap(grouped));
    }

    /**
     * Displays all grouped classes in a detailed view-style list.
     * Shows all fields including date range, attendance mode, and campus.
     */
    public void viewAll() {
        List<ClassEntry> grouped = classService.getGroupedClasses();
        if (grouped.isEmpty()) {
            view.printWarning("No classes have been imported yet.");
            return;
        }
        view.printViewList(grouped, buildTopicMap(grouped));
    }

    /**
     * Backward-compatible keyword search (called by legacy menu flows).
     * Results are printed via printClassList.
     */
    public void search(String query) {
        List<ClassEntry> results = searchService.search(query);
        System.out.println("\n  Search results for \"" + query + "\" ("
            + results.size() + " found):");
        view.printClassList(results);
    }

    /**
     * Interactive structured search using SearchCriteria.
     * User is prompted for each criterion; blank = skip that field.
     */
    public void search() {
        System.out.println("\n── Search Classes ───────────────────────────");
        System.out.println("  Enter criteria (press Enter to skip any field):");

        SearchCriteria criteria = new SearchCriteria();
        criteria.courseCode     = promptOptional("  Course code        : ");
        criteria.topicName      = promptOptional("  Topic name         : ");
        criteria.classType      = promptOptional("  Class type         : ");
        criteria.attendanceMode = promptOptional("  Attendance mode    : ");
        criteria.campus         = promptOptional("  Campus             : ");
        criteria.day            = promptOptional("  Day (e.g. MONDAY)  : ");

        String startStr = promptOptional("  Start time (HH:mm) : ");
        if (startStr != null) {
            try { criteria.startTime = LocalTime.parse(startStr); }
            catch (DateTimeParseException e) {
                view.printWarning("Invalid start time '" + startStr + "'; field skipped.");
            }
        }

        String semStr = promptOptional("  Semester (1 or 2)  : ");
        if (semStr != null) {
            try { criteria.semester = Integer.parseInt(semStr); }
            catch (NumberFormatException e) {
                view.printWarning("Invalid semester; field skipped.");
            }
        }

        List<ClassEntry> results = searchService.search(criteria);
        System.out.println();
        if (results.isEmpty()) {
            view.printWarning("No classes matched those criteria.");
        } else {
            view.printBrowseList(results, buildTopicMap(results));
        }
    }

    /**
     * Prompts for a class ID and allows the user to update building, room,
     * start time, end time, and day. Asks for confirmation before saving.
     */
    public void editClass() {
        System.out.println("\n── Edit Class ───────────────────────────────");
        String classId = input.readNonBlank(sc, "  Class ID to edit: ");
        Optional<ClassEntry> opt = classService.getClassById(classId);
        if (opt.isEmpty()) {
            view.printError("Class '" + classId + "' not found.");
            return;
        }
        ClassEntry e = opt.get();
        System.out.println("  Current: " + e);
        System.out.println("  Press Enter to keep the current value.\n");

        String building = promptWithDefault(
            "  Building [" + e.getBuilding() + "]: ", e.getBuilding());
        String room     = promptWithDefault(
            "  Room     [" + e.getRoom() + "]: ", e.getRoom());

        LocalTime start = e.getStartTime();
        String startStr = promptOptional("  Start    [" + e.getStartTime() + "]: ");
        if (startStr != null) {
            try { start = LocalTime.parse(startStr); }
            catch (DateTimeParseException ex) {
                view.printWarning("Invalid time; keeping " + e.getStartTime() + ".");
            }
        }

        LocalTime end = e.getEndTime();
        String endStr = promptOptional("  End      [" + e.getEndTime() + "]: ");
        if (endStr != null) {
            try { end = LocalTime.parse(endStr); }
            catch (DateTimeParseException ex) {
                view.printWarning("Invalid time; keeping " + e.getEndTime() + ".");
            }
        }

        DayOfWeek day = e.getDay();
        String dayStr = promptOptional("  Day      [" + e.getDay() + "]: ");
        if (dayStr != null) {
            try { day = DayOfWeek.valueOf(dayStr.toUpperCase()); }
            catch (IllegalArgumentException ex) {
                view.printWarning("Invalid day; keeping " + e.getDay() + ".");
            }
        }

        ClassEntry updated = new ClassEntry(
            e.getClassId(), e.getType(), e.getDate(),
            start, end, day, building, room, e.getCourseCode(),
            e.getAttendanceMode(), e.getAvailabilityNumber(),
            e.getClassInstance(), e.getDateFrom(), e.getDateTo());

        System.out.println("\n  Preview: " + updated);
        if (input.readBoolean(sc, "  Save changes?")) {
            try {
                classService.updateClass(updated);
                view.printSuccess("Class '" + classId + "' updated.");
            } catch (IllegalArgumentException ex) {
                view.printError(ex.getMessage());
            }
        } else {
            System.out.println("  Edit cancelled.");
        }
    }

    /**
     * Prompts for a class ID and, after confirmation, deletes it from the repository.
     */
    public void deleteClass() {
        System.out.println("\n── Delete Class ─────────────────────────────");
        String classId = input.readNonBlank(sc, "  Class ID to delete: ");
        Optional<ClassEntry> opt = classService.getClassById(classId);
        if (opt.isEmpty()) {
            view.printError("Class '" + classId + "' not found.");
            return;
        }
        System.out.println("  Found: " + opt.get());
        if (input.readBoolean(sc, "  Confirm delete?")) {
            try {
                classService.deleteClass(classId);
                view.printSuccess("Class '" + classId + "' deleted.");
            } catch (IllegalArgumentException e) {
                view.printError(e.getMessage());
            }
        } else {
            System.out.println("  Delete cancelled.");
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /** Reads a line; returns null if blank (meaning "skip this field"). */
    private String promptOptional(String prompt) {
        String val = input.readLine(sc, prompt).trim();
        return val.isEmpty() ? null : val;
    }

    /** Reads a line; returns defaultVal if blank. */
    private String promptWithDefault(String prompt, String defaultVal) {
        String val = input.readLine(sc, prompt).trim();
        return val.isEmpty() ? defaultVal : val;
    }

    /** Builds a courseCode → Topic map for the given list of class entries. */
    private Map<String, Topic> buildTopicMap(List<ClassEntry> classes) {
        Map<String, Topic> map = new HashMap<>();
        if (topicRepository == null) return map;
        for (ClassEntry c : classes) {
            if (!map.containsKey(c.getCourseCode())) {
                topicRepository.findByCourseCode(c.getCourseCode())
                    .ifPresent(t -> map.put(c.getCourseCode(), t));
            }
        }
        return map;
    }
}
