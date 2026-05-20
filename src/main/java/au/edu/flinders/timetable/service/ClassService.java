package au.edu.flinders.timetable.service;

import au.edu.flinders.timetable.model.ClassEntry;
import au.edu.flinders.timetable.model.Topic;
import au.edu.flinders.timetable.repository.ClassRepository;
import au.edu.flinders.timetable.repository.TopicRepository;

import java.time.MonthDay;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** Provides query and mutation operations over the class data store. */
public class ClassService {

    private final ClassRepository classRepository;
    private final TopicRepository topicRepository;

    private static final DateTimeFormatter MONTH_DAY_FMT =
        DateTimeFormatter.ofPattern("dd MMM");

    /** Constructs the service with the repositories it queries. */
    public ClassService(ClassRepository classRepository, TopicRepository topicRepository) {
        this.classRepository = classRepository;
        this.topicRepository = topicRepository;
    }

    /** Returns all stored ClassEntry objects. */
    public List<ClassEntry> getAllClasses() {
        return classRepository.findAll();
    }

    /** Returns the ClassEntry with the given ID, or empty if not found. */
    public Optional<ClassEntry> getClassById(String id) {
        return classRepository.findById(id);
    }

    /** Returns all ClassEntry objects belonging to the given course code. */
    public List<ClassEntry> getClassesForTopic(String courseCode) {
        return classRepository.findByCourseCode(courseCode);
    }

    /**
     * Returns all ClassEntry objects whose associated Topic campus matches the given label.
     * Matching is case-insensitive. Classes with no matching topic are excluded.
     */
    public List<ClassEntry> getClassesByCampus(String campus) {
        return classRepository.findAll().stream()
                .filter(c -> {
                    Optional<Topic> topic = topicRepository.findByCourseCode(c.getCourseCode());
                    return topic.map(t -> t.getCampus().equalsIgnoreCase(campus)).orElse(false);
                })
                .collect(Collectors.toList());
    }

    /**
     * Returns one representative ClassEntry per logical class group.
     * Two entries belong to the same group when they share:
     * courseCode, attendanceMode, availabilityNumber, type, and classInstance.
     * The representative entry has dateFrom = earliest and dateTo = latest across the group.
     * All other fields come from the first entry encountered in the group.
     */
    public List<ClassEntry> getGroupedClasses() {
        Map<String, List<ClassEntry>> groups = new LinkedHashMap<>();

        for (ClassEntry c : classRepository.findAll()) {
            String key = c.getCourseCode() + "|"
                       + c.getAttendanceMode() + "|"
                       + c.getAvailabilityNumber() + "|"
                       + c.getType() + "|"
                       + c.getClassInstance();
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(c);
        }

        List<ClassEntry> result = new ArrayList<>();
        for (List<ClassEntry> group : groups.values()) {
            ClassEntry first = group.get(0);
            String dateFrom = group.stream()
                .map(ClassEntry::getDateFrom)
                .filter(s -> !s.isBlank())
                .min(Comparator.comparing(this::toMonthDay))
                .orElse(first.getDateFrom());
            String dateTo = group.stream()
                .map(ClassEntry::getDateTo)
                .filter(s -> !s.isBlank())
                .max(Comparator.comparing(this::toMonthDay))
                .orElse(first.getDateTo());

            result.add(new ClassEntry(
                first.getClassId(), first.getType(), first.getDate(),
                first.getStartTime(), first.getEndTime(),
                first.getDay(), first.getBuilding(), first.getRoom(),
                first.getCourseCode(), first.getAttendanceMode(),
                first.getAvailabilityNumber(), first.getClassInstance(),
                dateFrom, dateTo));
        }
        return result;
    }

    /**
     * Returns one representative ClassEntry per logical class group
     * whose courseCode matches the given code (case-insensitive).
     * Delegates to getGroupedClasses() and filters by courseCode.
     */
    public List<ClassEntry> getGroupedClassesForTopic(String courseCode) {
        return getGroupedClasses().stream()
            .filter(c -> c.getCourseCode().equalsIgnoreCase(courseCode))
            .collect(Collectors.toList());
    }

    /**
     * Deletes the class with the given ID.
     * Throws IllegalArgumentException if the class is not found.
     */
    public void deleteClass(String classId) {
        if (!classRepository.exists(classId)) {
            throw new IllegalArgumentException("Class '" + classId + "' not found.");
        }
        classRepository.delete(classId);
    }

    /**
     * Replaces a stored class with the provided updated entry.
     * Throws IllegalArgumentException if the classId does not already exist.
     */
    public void updateClass(ClassEntry updated) {
        if (!classRepository.exists(updated.getClassId())) {
            throw new IllegalArgumentException(
                "Class '" + updated.getClassId() + "' not found; cannot update.");
        }
        classRepository.update(updated);
    }

    /** Parses a "dd MMM" date string into a MonthDay for comparison. Returns Jan 1 on failure. */
    private MonthDay toMonthDay(String s) {
        try {
            return MonthDay.parse(s.trim(), MONTH_DAY_FMT);
        } catch (Exception e) {
            return MonthDay.of(1, 1);
        }
    }
}
