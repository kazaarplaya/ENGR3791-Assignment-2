package au.edu.flinders.timetable.repository;

import au.edu.flinders.timetable.model.ClassEntry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** In-memory store for ClassEntry objects, keyed by classId. */
public class ClassRepository {

    private final Map<String, ClassEntry> store = new HashMap<>();

    /** Saves or overwrites the given ClassEntry. */
    public void save(ClassEntry c) {
        store.put(c.getClassId(), c);
    }

    /** Returns the ClassEntry with the given ID, or empty if not found. */
    public Optional<ClassEntry> findById(String classId) {
        return Optional.ofNullable(store.get(classId));
    }

    /** Returns all stored ClassEntry objects as a new list. */
    public List<ClassEntry> findAll() {
        return new ArrayList<>(store.values());
    }

    /** Returns all ClassEntry objects whose courseCode matches the given value. */
    public List<ClassEntry> findByCourseCode(String courseCode) {
        return store.values().stream()
                .filter(c -> c.getCourseCode().equalsIgnoreCase(courseCode))
                .collect(Collectors.toList());
    }

    /** Removes all stored ClassEntry objects. */
    public void clear() {
        store.clear();
    }
}
