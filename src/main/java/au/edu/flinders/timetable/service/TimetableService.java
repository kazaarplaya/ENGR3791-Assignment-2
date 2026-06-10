package au.edu.flinders.timetable.service;

import au.edu.flinders.timetable.model.ClassEntry;
import au.edu.flinders.timetable.model.Timetable;
import au.edu.flinders.timetable.repository.ClassRepository;
import au.edu.flinders.timetable.repository.TimetableRepository;

import java.util.List;
import java.util.Optional;

/** Manages persistence, naming, and editing of Timetable objects. */
public class TimetableService {

    private final TimetableRepository timetableRepository;
    private final ClassRepository     classRepository; // may be null (backward compat)
    private int nameCounter = 0;

    /**
     * Single-argument constructor preserved for backward compatibility with unit tests.
     * swapClassInstance() class-validation is skipped when classRepository is null.
     */
    public TimetableService(TimetableRepository timetableRepository) {
        this(timetableRepository, null);
    }

    /** Full constructor used in production wiring via Main. */
    public TimetableService(TimetableRepository timetableRepository,
                             ClassRepository classRepository) {
        this.timetableRepository = timetableRepository;
        this.classRepository     = classRepository;
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
     * Replaces oldClassId with newClassId in the named timetable.
     * Both classes must share the same courseCode and classType.
     * Throws IllegalArgumentException if the timetable is not found, either class ID
     * is missing from the repository, or courseCode/classType do not match.
     */
    public void swapClassInstance(String timetableName, String oldClassId, String newClassId) {
        Timetable t = timetableRepository.findByName(timetableName)
            .orElseThrow(() -> new IllegalArgumentException(
                "Timetable '" + timetableName + "' not found."));

        if (!t.getClassIds().contains(oldClassId)) {
            throw new IllegalArgumentException(
                "Class '" + oldClassId + "' is not in timetable '" + timetableName + "'.");
        }

        if (classRepository != null) {
            ClassEntry oldEntry = classRepository.findById(oldClassId)
                .orElseThrow(() -> new IllegalArgumentException(
                    "Class '" + oldClassId + "' not found in repository."));
            ClassEntry newEntry = classRepository.findById(newClassId)
                .orElseThrow(() -> new IllegalArgumentException(
                    "Class '" + newClassId + "' not found in repository."));

            if (!oldEntry.getCourseCode().equalsIgnoreCase(newEntry.getCourseCode())) {
                throw new IllegalArgumentException(
                    "Cannot swap: course codes differ ("
                    + oldEntry.getCourseCode() + " vs " + newEntry.getCourseCode() + ").");
            }
            if (!oldEntry.getType().equalsIgnoreCase(newEntry.getType())) {
                throw new IllegalArgumentException(
                    "Cannot swap: class types differ ("
                    + oldEntry.getType() + " vs " + newEntry.getType() + ").");
            }
        }

        t.removeClass(oldClassId);
        t.addClass(newClassId);
        timetableRepository.save(t);
    }

    /**
     * Generates a unique sequential timetable name for this session.
     * Returns names in the format Timetable_1, Timetable_2, etc.
     */
    public String generateUniqueName() {
        nameCounter++;
        return "Timetable_" + nameCounter;
    }

    public void swapClass(String swapTest, ClassEntry oldClass, ClassEntry newClass) {
    }
}
