package au.edu.flinders.timetable.service;

import au.edu.flinders.timetable.model.ClassEntry;
import au.edu.flinders.timetable.model.Preference;
import au.edu.flinders.timetable.model.Timetable;
import au.edu.flinders.timetable.model.Topic;
import au.edu.flinders.timetable.model.User;
import au.edu.flinders.timetable.repository.ClassRepository;
import au.edu.flinders.timetable.repository.PreferenceRepository;
import au.edu.flinders.timetable.repository.TopicRepository;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Generates an optimised Timetable for a user based on campus selections,
 * preferences, and clash detection rules.
 */
public class TimetableGeneratorService {

    // Campus label constants used in clash detection.
    private static final String CITY         = "city";
    private static final String BEDFORD_PARK = "bedford park";
    private static final String TONSLEY      = "tonsley";

    private final ClassRepository     classRepository;
    private final TopicRepository     topicRepository;
    private final PreferenceRepository preferenceRepository;
    private final TimetableService    timetableService;

    /** Constructs the generator with all required service and repository dependencies. */
    public TimetableGeneratorService(ClassRepository classRepository,
                                     TopicRepository topicRepository,
                                     PreferenceRepository preferenceRepository,
                                     TimetableService timetableService) {
        this.classRepository     = classRepository;
        this.topicRepository     = topicRepository;
        this.preferenceRepository = preferenceRepository;
        this.timetableService    = timetableService;
    }

    /**
     * Generates and saves an optimised Timetable for the given user.
     *
     * @param user                the student whose enrolled topics are used
     * @param campusSelections    map of courseCode -> chosen campus label (may be empty)
     * @param allowLectureOverlap whether back-to-back lectures at different campuses are allowed
     * @param applyPreferences    whether to filter classes using the user's saved preferences
     * @param optionalName        desired timetable name; auto-generated if blank
     * @return the saved Timetable
     * @throws IllegalStateException    when no valid classes remain for a topic after filtering
     * @throws IllegalArgumentException when the timetable name already exists
     */
    public Timetable generate(User user,
                              Map<String, String> campusSelections,
                              boolean allowLectureOverlap,
                              boolean applyPreferences,
                              String optionalName) {

        // Resolve timetable name
        String name = (optionalName != null && !optionalName.isBlank())
            ? optionalName
            : timetableService.generateUniqueName();

        Timetable timetable = new Timetable(name, "", allowLectureOverlap, applyPreferences);

        // Load preference if requested
        Optional<Preference> prefOpt = applyPreferences
            ? preferenceRepository.findByUserId(user.getUserId())
            : Optional.empty();

        // Accumulate all accepted ClassEntry objects across topics
        List<ClassEntry> accepted = new ArrayList<>();

        for (String courseCode : user.getEnrolledTopics()) {
            List<ClassEntry> candidates = classRepository.findByCourseCode(courseCode);

            // Step 2: campus constraint
            String selectedCampus = campusSelections.getOrDefault(courseCode, "");
            if (!selectedCampus.isBlank()) {
                candidates = applyCampusConstraint(candidates, courseCode, selectedCampus);
            }

            // Step 3: preference filters
            if (applyPreferences && prefOpt.isPresent()) {
                candidates = applyPreferenceFilters(candidates, prefOpt.get());
            }

            if (candidates.isEmpty()) {
                String campusMsg = selectedCampus.isBlank() ? "any campus" : selectedCampus;
                throw new IllegalStateException(
                    "No classes available for topic " + courseCode + " at " + campusMsg + ".");
            }

            // Step 4: clash detection — add each candidate if it doesn't clash
            for (ClassEntry candidate : candidates) {
                if (!hasClash(candidate, accepted, allowLectureOverlap)) {
                    accepted.add(candidate);
                    timetable.addClass(candidate.getClassId());
                }
            }
        }

        // Steps 6 & 7 — save
        timetableService.save(timetable);
        return timetable;
    }

    // ── Campus constraint ─────────────────────────────────────────────────────

    /**
     * Filters candidates to only those whose Topic.campus matches the selected campus.
     * City campus classes may not be mixed with Bedford Park or Tonsley classes for the same topic.
     */
    private List<ClassEntry> applyCampusConstraint(List<ClassEntry> candidates,
                                                    String courseCode,
                                                    String selectedCampus) {
        List<ClassEntry> filtered = candidates.stream()
            .filter(c -> {
                Optional<Topic> t = topicRepository.findByCourseCode(c.getCourseCode());
                return t.map(topic -> topic.getCampus().equalsIgnoreCase(selectedCampus))
                        .orElse(false);
            })
            .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            throw new IllegalStateException(
                "No classes available for topic " + courseCode
                    + " at campus " + selectedCampus + ".");
        }
        return filtered;
    }

    // ── Preference filters ────────────────────────────────────────────────────

    /** Applies campus, timeOfDay, and day preference filters to the candidate list. */
    private List<ClassEntry> applyPreferenceFilters(List<ClassEntry> candidates,
                                                     Preference pref) {
        List<ClassEntry> result = new ArrayList<>(candidates);

        // Campus filter
        if (!pref.getCampus().equalsIgnoreCase("Any") && !pref.getCampus().isBlank()) {
            result = result.stream()
                .filter(c -> c.getBuilding().equalsIgnoreCase(pref.getCampus()))
                .collect(Collectors.toList());
        }

        // Time-of-day filter
        if (!pref.getTimeOfDay().equalsIgnoreCase("Any")) {
            result = result.stream()
                .filter(c -> matchesTimeOfDay(c.getStartTime(), pref.getTimeOfDay()))
                .collect(Collectors.toList());
        }

        // Day filter
        if (!pref.getDay().equalsIgnoreCase("Any") && !pref.getDay().isBlank()) {
            result = result.stream()
                .filter(c -> c.getDay() != null
                    && c.getDay() == DayOfWeek.valueOf(pref.getDay().toUpperCase()))
                .collect(Collectors.toList());
        }

        return result;
    }

    /** Returns true when the given start time falls within the named time-of-day window. */
    private boolean matchesTimeOfDay(LocalTime start, String timeOfDay) {
        return switch (timeOfDay.toLowerCase()) {
            case "morning"   -> start.isBefore(LocalTime.of(12, 0));
            case "afternoon" -> !start.isBefore(LocalTime.of(12, 0))
                                && start.isBefore(LocalTime.of(18, 0));
            case "evening"   -> !start.isBefore(LocalTime.of(18, 0));
            default          -> true;
        };
    }

    // ── Clash detection ───────────────────────────────────────────────────────

    /**
     * Returns true when the candidate clashes with any already-accepted class
     * according to the campus and lecture-overlap rules.
     */
    private boolean hasClash(ClassEntry candidate,
                              List<ClassEntry> accepted,
                              boolean allowLectureOverlap) {
        for (ClassEntry existing : accepted) {
            if (existing.getDay() != candidate.getDay()) continue;

            String campusA = resolveCampus(existing);
            String campusB = resolveCampus(candidate);

            if (clashes(existing, candidate, campusA, campusB, allowLectureOverlap)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Applies the ordered clash detection rules from the design specification.
     * Returns true when the two classes clash and cannot both be scheduled.
     *
     * Rule evaluation order:
     *   a. Same campus          → only a direct time overlap is a clash.
     *   g. City + any campus    → always require a 30-minute gap; class type and
     *                             allowLectureOverlap have no effect on this rule.
     *   b–f. Bedford + Tonsley  → gap rules depend on class type and overlap flag.
     *   default                 → 30-minute gap required.
     */
    private boolean clashes(ClassEntry a, ClassEntry b,
                             String campusA, String campusB,
                             boolean allowLectureOverlap) {

        // Rule a: same campus — back-to-back always allowed; only check time overlap.
        if (campusA.equalsIgnoreCase(campusB)) {
            return timesOverlap(a, b);
        }

        // Rule g: City campus paired with ANY other campus (including Tonsley) —
        // unconditionally requires a 30-minute gap regardless of class type or
        // the allowLectureOverlap flag.  This check must come before all other
        // cross-campus rules so it cannot be bypassed.
        boolean aCityOther = campusA.equalsIgnoreCase(CITY) && !campusB.equalsIgnoreCase(CITY);
        boolean bCityOther = campusB.equalsIgnoreCase(CITY) && !campusA.equalsIgnoreCase(CITY);
        if (aCityOther || bCityOther) {
            return !hasGap(a, b, 30);
        }

        // Rules b–f: Bedford Park + Tonsley — gap rules vary by class type and flag.
        if (isBedfordTonsleyPair(campusA, campusB)) {
            boolean bothLectures    = a.isLecture() && b.isLecture();
            boolean bothNonLectures = !a.isLecture() && !b.isLecture();

            if (bothNonLectures) {
                // Rule b: both non-lectures — always require 30-minute gap.
                return !hasGap(a, b, 30);
            }
            if (bothLectures) {
                // Rules c/d: both lectures — gap waived only when overlap enabled.
                return allowLectureOverlap ? timesOverlap(a, b) : !hasGap(a, b, 30);
            }
            // Rules e/f: one lecture + one non-lecture — gap waived only when overlap enabled.
            return allowLectureOverlap ? timesOverlap(a, b) : !hasGap(a, b, 30);
        }

        // Default: any other cross-campus pairing requires a 30-minute gap.
        return !hasGap(a, b, 30);
    }

    /** Returns true when time intervals of a and b overlap (exclusive of touching boundaries). */
    private boolean timesOverlap(ClassEntry a, ClassEntry b) {
        return a.getStartTime().isBefore(b.getEndTime())
            && b.getStartTime().isBefore(a.getEndTime());
    }

    /**
     * Returns true when there is at least minGapMinutes between the end of one class
     * and the start of the other (in either order).
     */
    private boolean hasGap(ClassEntry a, ClassEntry b, int minGapMinutes) {
        long gapAtoB = java.time.Duration.between(a.getEndTime(), b.getStartTime()).toMinutes();
        long gapBtoA = java.time.Duration.between(b.getEndTime(), a.getStartTime()).toMinutes();
        return gapAtoB >= minGapMinutes || gapBtoA >= minGapMinutes;
    }

    /** Returns true when one campus is City and the other is something different. */
    private boolean isCityPairedWithOther(String campusA, String campusB) {
        return (campusA.equalsIgnoreCase(CITY) && !campusB.equalsIgnoreCase(CITY))
            || (campusB.equalsIgnoreCase(CITY) && !campusA.equalsIgnoreCase(CITY));
    }

    /** Returns true when one campus is Bedford Park and the other is Tonsley (or vice versa). */
    private boolean isBedfordTonsleyPair(String campusA, String campusB) {
        return (campusA.equalsIgnoreCase(BEDFORD_PARK) && campusB.equalsIgnoreCase(TONSLEY))
            || (campusA.equalsIgnoreCase(TONSLEY) && campusB.equalsIgnoreCase(BEDFORD_PARK));
    }

    /** Resolves the campus label for a ClassEntry via its associated Topic (lower-cased). */
    private String resolveCampus(ClassEntry c) {
        return topicRepository.findByCourseCode(c.getCourseCode())
                .map(t -> t.getCampus().toLowerCase())
                .orElse("");
    }
}
