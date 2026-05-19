package au.edu.flinders.timetable.repository;

import au.edu.flinders.timetable.model.Topic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** In-memory store for Topic objects, keyed by courseCode. */
public class TopicRepository {

    private final Map<String, Topic> store = new HashMap<>();

    /** Saves or overwrites the given Topic. */
    public void save(Topic t) {
        store.put(t.getCourseCode(), t);
    }

    /** Returns the Topic with the given course code, or empty if not found. */
    public Optional<Topic> findByCourseCode(String courseCode) {
        return Optional.ofNullable(store.get(courseCode));
    }

    /** Returns all stored Topics as a new list. */
    public List<Topic> findAll() {
        return new ArrayList<>(store.values());
    }

    /** Removes all stored Topics. */
    public void clear() {
        store.clear();
    }
}
