package au.edu.flinders.timetable.service;

import au.edu.flinders.timetable.model.ClassEntry;
import au.edu.flinders.timetable.model.Topic;
import au.edu.flinders.timetable.repository.ClassRepository;
import au.edu.flinders.timetable.repository.TopicRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/** Provides partial, case-insensitive search over class and topic data. */
public class SearchService {

    private final ClassRepository classRepository;
    private final TopicRepository topicRepository;

    /** Constructs the service with the repositories it searches. */
    public SearchService(ClassRepository classRepository, TopicRepository topicRepository) {
        this.classRepository = classRepository;
        this.topicRepository = topicRepository;
    }

    /**
     * Searches all ClassEntry objects for partial, case-insensitive matches against the query.
     * Fields searched: classId, type, building, room, courseCode,
     * and the campus/delivery of the associated Topic.
     * An empty or blank query returns all classes sorted by courseCode then startTime.
     * Results are sorted by courseCode then startTime.
     */
    public List<ClassEntry> search(String query) {
        String q = (query == null) ? "" : query.toLowerCase().trim();

        return classRepository.findAll().stream()
                .filter(c -> matchesQuery(c, q))
                .sorted(Comparator.comparing(ClassEntry::getCourseCode)
                                  .thenComparing(ClassEntry::getStartTime))
                .collect(Collectors.toList());
    }

    /** Returns true when the given ClassEntry matches the search query. */
    private boolean matchesQuery(ClassEntry c, String q) {
        if (q.isEmpty()) return true;

        if (contains(c.getClassId(),    q)) return true;
        if (contains(c.getType(),       q)) return true;
        if (contains(c.getBuilding(),   q)) return true;
        if (contains(c.getRoom(),       q)) return true;
        if (contains(c.getCourseCode(), q)) return true;
        if (c.getDay() != null && contains(c.getDay().toString(), q)) return true;

        Optional<Topic> topic = topicRepository.findByCourseCode(c.getCourseCode());
        if (topic.isPresent()) {
            Topic t = topic.get();
            if (contains(t.getCampus(),   q)) return true;
            if (contains(t.getDelivery(), q)) return true;
        }

        return false;
    }

    /** Null-safe case-insensitive contains check. */
    private boolean contains(String field, String q) {
        return field != null && field.toLowerCase().contains(q);
    }
}
