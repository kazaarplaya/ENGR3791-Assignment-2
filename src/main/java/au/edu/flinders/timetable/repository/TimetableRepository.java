package au.edu.flinders.timetable.repository;

import au.edu.flinders.timetable.model.Timetable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** In-memory store for Timetable objects, keyed by timetable name. */
public class TimetableRepository {

    private final Map<String, Timetable> store = new HashMap<>();

    /** Saves or overwrites the given Timetable. */
    public void save(Timetable t) {
        store.put(t.getTimetableName(), t);
    }

    /** Returns the Timetable with the given name, or empty if not found. */
    public Optional<Timetable> findByName(String name) {
        return Optional.ofNullable(store.get(name));
    }

    /** Returns all stored Timetables as a new list. */
    public List<Timetable> findAll() {
        return new ArrayList<>(store.values());
    }

    /** Removes the Timetable with the given name; no-op if not found. */
    public void delete(String name) {
        store.remove(name);
    }

    /** Returns true when a Timetable with the given name exists in the store. */
    public boolean exists(String name) {
        return store.containsKey(name);
    }
}
