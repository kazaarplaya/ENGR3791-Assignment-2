package au.edu.flinders.timetable.repository;

import au.edu.flinders.timetable.model.Preference;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** In-memory store for Preference objects, keyed by userId. */
public class PreferenceRepository {

    private final Map<String, Preference> store = new HashMap<>();

    /** Saves or overwrites the Preference for the user identified by its userId. */
    public void save(Preference p) {
        store.put(p.getUserId(), p);
    }

    /** Returns the Preference for the given user ID, or empty if none exists. */
    public Optional<Preference> findByUserId(String userId) {
        return Optional.ofNullable(store.get(userId));
    }

    /** Removes the Preference for the given user ID; no-op if not found. */
    public void delete(String userId) {
        store.remove(userId);
    }
}
