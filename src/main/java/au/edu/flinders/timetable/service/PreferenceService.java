package au.edu.flinders.timetable.service;

import au.edu.flinders.timetable.model.Preference;
import au.edu.flinders.timetable.repository.PreferenceRepository;

import java.time.DayOfWeek;
import java.util.Optional;
import java.util.Set;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

/** Manages saving, retrieving, and clearing user preferences with validation. */
public class PreferenceService {

    private static final Set<String> VALID_TIMES = Collections.unmodifiableSet(
        new HashSet<>(Arrays.asList("morning", "afternoon", "evening", "any"))
    );

    private final PreferenceRepository preferenceRepository;

    /** Constructs the service with the repository it manages. */
    public PreferenceService(PreferenceRepository preferenceRepository) {
        this.preferenceRepository = preferenceRepository;
    }

    /**
     * Validates and saves the given Preference.
     * For legacy preferences (7-arg constructor): validates timeOfDay and day.
     * For token-based preferences: validates that all tokens are in the valid set.
     * Throws IllegalArgumentException on invalid input.
     */
    public void savePreference(Preference p) {
        // Legacy-style: the old 7-arg constructor always sets non-null campus/timeOfDay/day.
        // Token-style: uses the 2-arg constructor which leaves those fields null.
        if (p.getTimeOfDay() != null) {
            // Legacy path — validate the old-style fields.
            validateTimeOfDay(p.getTimeOfDay());
            validateDay(p.getDay());
        } else {
            // Token path — validate each token.
            for (String token : p.getPriorityOrder()) {
                if (!Preference.VALID_TOKENS.contains(token)) {
                    throw new IllegalArgumentException(
                        "Invalid preference token: '" + token + "'.");
                }
            }
        }
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
        if (!VALID_TIMES.contains(timeOfDay.toLowerCase())) {
            throw new IllegalArgumentException(
                "Invalid timeOfDay '" + timeOfDay
                    + "'. Must be one of: Morning, Afternoon, Evening, Any.");
        }
    }

    /**
     * Throws IllegalArgumentException when day is not a recognised English day name or "Any".
     * Accepts full English day names (Monday–Sunday) or "Any".
     */
    private void validateDay(String day) {
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
