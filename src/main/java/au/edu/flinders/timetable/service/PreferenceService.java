package au.edu.flinders.timetable.service;

import au.edu.flinders.timetable.model.Preference;
import au.edu.flinders.timetable.repository.PreferenceRepository;

import java.time.DayOfWeek;
import java.util.Optional;
import java.util.Set;

/** Manages saving, retrieving, and clearing user preferences with validation. */
public class PreferenceService {

    private static final Set<String> VALID_TIMES =
        Set.of("morning", "afternoon", "evening", "any");

    private final PreferenceRepository preferenceRepository;

    /** Constructs the service with the repository it manages. */
    public PreferenceService(PreferenceRepository preferenceRepository) {
        this.preferenceRepository = preferenceRepository;
    }

    /**
     * Validates and saves the given Preference.
     * Throws IllegalArgumentException if timeOfDay or day contains an invalid value.
     */
    public void savePreference(Preference p) {
        validateTimeOfDay(p.getTimeOfDay());
        validateDay(p.getDay());
        preferenceRepository.save(p);
    }

    /** Returns the Preference for the given user ID, or empty if none exists. */
    public Optional<Preference> getPreference(String userId) {
        return preferenceRepository.findByUserId(userId);
    }

    /** Removes the Preference for the given user ID. */
    public void clearPreference(String userId) {
        preferenceRepository.delete(userId);
    }

    /** Throws IllegalArgumentException when timeOfDay is not a recognised value. */
    private void validateTimeOfDay(String timeOfDay) {
        if (timeOfDay == null || !VALID_TIMES.contains(timeOfDay.toLowerCase())) {
            throw new IllegalArgumentException(
                "Invalid timeOfDay '" + timeOfDay
                    + "'. Must be one of: Morning, Afternoon, Evening, Any.");
        }
    }

    /**
     * Throws IllegalArgumentException when day is not a recognised English day name or "Any".
     * Accepts full English day names (Monday–Friday, Saturday, Sunday) or "Any".
     */
    private void validateDay(String day) {
        if (day == null) {
            throw new IllegalArgumentException("day cannot be null.");
        }
        if (day.equalsIgnoreCase("Any")) return;
        try {
            DayOfWeek.valueOf(day.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "Invalid day '" + day
                    + "'. Must be a full day name (e.g. Monday) or 'Any'.");
        }
    }
}
