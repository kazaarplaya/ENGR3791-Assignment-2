package au.edu.flinders.timetable.service;

import au.edu.flinders.timetable.model.Timetable;
import au.edu.flinders.timetable.repository.TimetableRepository;

import java.util.List;
import java.util.Optional;

/** Manages persistence and naming of Timetable objects. */
public class TimetableService {

    private final TimetableRepository timetableRepository;
    private int nameCounter = 0;

    /** Constructs the service with the repository it manages. */
    public TimetableService(TimetableRepository timetableRepository) {
        this.timetableRepository = timetableRepository;
    }

    /**
     * Saves the given Timetable.
     * Throws IllegalArgumentException if a timetable with that name already exists.
     */
    public void save(Timetable t) {
        if (timetableRepository.exists(t.getTimetableName())) {
            throw new IllegalArgumentException(
                "A timetable with that name already exists.");
        }
        timetableRepository.save(t);
    }

    /** Returns the Timetable with the given name, or empty if not found. */
    public Optional<Timetable> getByName(String name) {
        return timetableRepository.findByName(name);
    }

    /** Returns all stored Timetables. */
    public List<Timetable> getAll() {
        return timetableRepository.findAll();
    }

    /**
     * Deletes the Timetable with the given name.
     * Throws IllegalArgumentException if no such timetable exists.
     */
    public void delete(String name) {
        if (!timetableRepository.exists(name)) {
            throw new IllegalArgumentException(
                "No timetable named '" + name + "' exists.");
        }
        timetableRepository.delete(name);
    }

    /**
     * Generates a unique sequential timetable name for this session.
     * Returns names in the format Timetable_1, Timetable_2, etc.
     * The counter increments with each call and never resets within a session.
     */
    public String generateUniqueName() {
        nameCounter++;
        return "Timetable_" + nameCounter;
    }
}
