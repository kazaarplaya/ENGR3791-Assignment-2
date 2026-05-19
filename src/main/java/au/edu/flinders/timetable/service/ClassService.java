package au.edu.flinders.timetable.service;

import au.edu.flinders.timetable.model.ClassEntry;
import au.edu.flinders.timetable.model.Topic;
import au.edu.flinders.timetable.repository.ClassRepository;
import au.edu.flinders.timetable.repository.TopicRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/** Provides read-only query operations over the class data store. */
public class ClassService {

    private final ClassRepository classRepository;
    private final TopicRepository topicRepository;

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
     * Returns all ClassEntry objects whose associated Topic campus matches the given campus label.
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
}
