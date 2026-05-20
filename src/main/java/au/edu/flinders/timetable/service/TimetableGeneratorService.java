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
 * Generates an optimised Timetable for a user based on semester, campus selections,
 * preferences, and clash detection rules.
 */
public class TimetableGeneratorService {

    private static final String CITY         = "city";
    private static final String BEDFORD_PARK = "bedford park";
    private static final String TONSLEY      = "tonsley";
    private static final String FESTIVAL_TOWER = "festival tower";
    private static final String TONSLEY_PREFIX = "tonsley";

    private final ClassRepository      classRepository;
    private final TopicRepository      topicRepository;
    private final PreferenceRepository preferenceRepository;
    private final TimetableService     timetableService;

    /** Constructs the generator with all required dependencies. */
    public TimetableGeneratorService(ClassRepository classRepository,
                                     TopicRepository topicRepository,
                                     PreferenceRepository preferenceRepository,
                                     TimetableService timetableService) {
        this.classRepository     = classRepository;
        this.topicRepository     = topicRepository;
        this.preferenceRepository = preferenceRepository;
        this.timetableService    = timetableService;
    }

    // ── Public generate overloads ─────────────────────────────────────────────

    /**
     * Backward-compatible 5-argument overload (semester = 0 = both semesters).
     * Preserved so existing unit tests continue to compile without modification.
     */
    public Timetable generate(User user,
                              Map<String, String> campusSelections,
                              boolean allowLectureOverlap,
                              boolean applyPreferences,
                              String optionalName) {
        return generate(user, campusSelections, allowLectureOverlap,
                        applyPreferences, optionalName, 0);
    }

    /**
     * Generates and saves an optimised Timetable for the given user.
     *
     * @param user                the student whose enrolled topics are used
     * @param campusSelections    map of courseCode to the campus label chosen for that topic
     * @param allowLectureOverlap whether back-to-back lectures between campuses are permitted
     * @param applyPreferences    whether to filter using the user's saved preferences
     * @param optionalName        desired timetable name; blank/null = auto-generate
     * @param semester            1 = Sem 1 only, 2 = Sem 2 only, 0 = both
     * @return the saved Timetable
     * @throws IllegalStateException    when no valid classes remain for a topic after filtering
     * @throws IllegalArgumentException when the timetable name already exists
     */
    public Timetable generate(User user,
                              Map<String, String> campusSelections,
                              boolean allowLectureOverlap,
                              boolean applyPreferences,
                              String optionalName,
                              int semester) {

        String name = (optionalName != null && !optionalName.isBlank())
            ? optionalName : timetableService.generateUniqueName();

        Timetable timetable = new Timetable(name,
            semester == 0 ? "" : "Semester " + semester,
            allowLectureOverlap, applyPreferences);

        Optional<Preference> prefOpt = applyPreferences
            ? preferenceRepository.findByUserId(user.getUserId())
            : Optional.empty();

        List<ClassEntry> accepted = new ArrayList<>();

        for (String courseCode : user.getEnrolledTopics()) {
            List<ClassEntry> candidates = classRepository.findByCourseCode(courseCode);

            // Semester filter
            if (semester != 0) {
                final int sem = semester;
                candidates = candidates.stream()
                    .filter(c -> topicRepository.findByCourseCode(c.getCourseCode())
                        .map(t -> t.getSemester() == sem).orElse(true))
                    .collect(Collectors.toList());
            }

            // Campus constraint per topic (City/non-City exclusion rule)
            String selectedCampus = campusSelections.getOrDefault(courseCode, "");
            if (!selectedCampus.isBlank()) {
                candidates = applyCampusConstraint(candidates, courseCode, selectedCampus);
            }

            // Preference filters
            if (applyPreferences && prefOpt.isPresent()) {
                candidates = applyPreferenceFilters(candidates, prefOpt.get());
            }

            if (candidates.isEmpty()) {
                String campusMsg = selectedCampus.isBlank() ? "any campus" : selectedCampus;
                throw new IllegalStateException(
                    "No classes available for topic " + courseCode + " at " + campusMsg + ".");
            }

            for (ClassEntry candidate : candidates) {
                if (!hasClash(candidate, accepted, allowLectureOverlap)) {
                    accepted.add(candidate);
                    timetable.addClass(candidate.getClassId());
                }
            }
        }

        timetableService.save(timetable);
        return timetable;
    }

    // ── Campus constraint ─────────────────────────────────────────────────────

    /**
     * Enforces the City / non-City exclusion rule for a single topic.
     * City selected   → keep only classes in Festival Tower (City campus).
     * Non-City selected → exclude classes in Festival Tower; Bedford Park/Tonsley may coexist.
     */
    private List<ClassEntry> applyCampusConstraint(List<ClassEntry> candidates,
                                                    String courseCode,
                                                    String selectedCampus) {
        boolean citySelected = selectedCampus.equalsIgnoreCase("City")
                            || selectedCampus.equalsIgnoreCase("Flinders City Campus");

        List<ClassEntry> filtered = candidates.stream()
            .filter(c -> {
                String classCampus = inferCampusFromBuilding(c.getBuilding());
                return citySelected
                    ? classCampus.equalsIgnoreCase("City")
                    : !classCampus.equalsIgnoreCase("City");
            })
            .collect(Collectors.toList());

        // Fallback: match by Topic.campus when building inference yields nothing.
        if (filtered.isEmpty()) {
            filtered = candidates.stream()
                .filter(c -> topicRepository.findByCourseCode(c.getCourseCode())
                    .map(t -> t.getCampus().equalsIgnoreCase(selectedCampus)).orElse(false))
                .collect(Collectors.toList());
        }

        if (filtered.isEmpty()) {
            throw new IllegalStateException(
                "No classes available for topic " + courseCode
                    + " at campus " + selectedCampus + ".");
        }
        return filtered;
    }

    /** Infers the campus name from a building string. */
    private String inferCampusFromBuilding(String building) {
        if (building == null) return "Bedford Park";
        String b = building.toLowerCase();
        if (b.contains(FESTIVAL_TOWER)) return "City";
        if (b.contains(TONSLEY_PREFIX)) return "Tonsley";
        return "Bedford Park";
    }

    // ── Preference filters ────────────────────────────────────────────────────

    /**
     * Applies preference tokens in priority order.
     * A token is skipped if applying it would empty the candidate list.
     */
    private List<ClassEntry> applyPreferenceFilters(List<ClassEntry> candidates,
                                                     Preference pref) {
        List<ClassEntry> result = new ArrayList<>(candidates);
        for (String token : pref.getPriorityOrder()) {
            List<ClassEntry> filtered = applyToken(token, result);
            if (!filtered.isEmpty()) result = filtered;
        }
        return result;
    }

    /** Applies a single preference token as a filter. */
    private List<ClassEntry> applyToken(String token, List<ClassEntry> candidates) {
        return switch (token) {
            case Preference.CAMPUS_BEDFORD_PARK ->
                candidates.stream()
                    .filter(c -> inferCampusFromBuilding(c.getBuilding()).equalsIgnoreCase("Bedford Park"))
                    .collect(Collectors.toList());
            case Preference.CAMPUS_TONSLEY ->
                candidates.stream()
                    .filter(c -> inferCampusFromBuilding(c.getBuilding()).equalsIgnoreCase("Tonsley"))
                    .collect(Collectors.toList());
            case Preference.CAMPUS_CITY ->
                candidates.stream()
                    .filter(c -> inferCampusFromBuilding(c.getBuilding()).equalsIgnoreCase("City"))
                    .collect(Collectors.toList());
            case Preference.TIME_MORNING ->
                candidates.stream()
                    .filter(c -> c.getStartTime().isBefore(LocalTime.of(12, 0)))
                    .collect(Collectors.toList());
            case Preference.TIME_AFTERNOON ->
                candidates.stream()
                    .filter(c -> {
                        LocalTime s = c.getStartTime();
                        return !s.isBefore(LocalTime.of(12, 0)) && s.isBefore(LocalTime.of(17, 0));
                    })
                    .collect(Collectors.toList());
            case Preference.TIME_EVENING ->
                candidates.stream()
                    .filter(c -> !c.getStartTime().isBefore(LocalTime.of(17, 0)))
                    .collect(Collectors.toList());
            case Preference.DAY_MONDAY ->
                candidates.stream().filter(c -> c.getDay() == DayOfWeek.MONDAY).collect(Collectors.toList());
            case Preference.DAY_TUESDAY ->
                candidates.stream().filter(c -> c.getDay() == DayOfWeek.TUESDAY).collect(Collectors.toList());
            case Preference.DAY_WEDNESDAY ->
                candidates.stream().filter(c -> c.getDay() == DayOfWeek.WEDNESDAY).collect(Collectors.toList());
            case Preference.DAY_THURSDAY ->
                candidates.stream().filter(c -> c.getDay() == DayOfWeek.THURSDAY).collect(Collectors.toList());
            case Preference.DAY_FRIDAY ->
                candidates.stream().filter(c -> c.getDay() == DayOfWeek.FRIDAY).collect(Collectors.toList());
            // CAMPUS_SAME, SPREAD, COMPACT: ordering hints only — no exclusion.
            default -> new ArrayList<>(candidates);
        };
    }

    // ── Clash detection ───────────────────────────────────────────────────────

    /**
     * Public clash check used by TimetableController when editing timetables.
     * Returns true when candidate clashes with any class in the accepted list.
     */
    public boolean wouldClash(ClassEntry candidate,
                               List<ClassEntry> accepted,
                               boolean allowLectureOverlap) {
        return hasClash(candidate, accepted, allowLectureOverlap);
    }

    private boolean hasClash(ClassEntry candidate,
                              List<ClassEntry> accepted,
                              boolean allowLectureOverlap) {
        for (ClassEntry existing : accepted) {
            if (existing.getDay() != candidate.getDay()) continue;
            String campusA = resolveCampus(existing);
            String campusB = resolveCampus(candidate);
            if (clashes(existing, candidate, campusA, campusB, allowLectureOverlap)) return true;
        }
        return false;
    }

    /**
     * Rule evaluation order:
     *   a. Same campus          → only direct time overlap is a clash.
     *   g. City + any other     → unconditional 30-minute gap required.
     *   b–f. Bedford + Tonsley  → gap varies by class type and overlap flag.
     *   default                 → 30-minute gap required.
     */
    private boolean clashes(ClassEntry a, ClassEntry b,
                             String campusA, String campusB,
                             boolean allowLectureOverlap) {
        if (campusA.equalsIgnoreCase(campusB)) return timesOverlap(a, b);

        boolean aCityOther = campusA.equalsIgnoreCase(CITY) && !campusB.equalsIgnoreCase(CITY);
        boolean bCityOther = campusB.equalsIgnoreCase(CITY) && !campusA.equalsIgnoreCase(CITY);
        if (aCityOther || bCityOther) return !hasGap(a, b, 30);

        if (isBedfordTonsleyPair(campusA, campusB)) {
            boolean bothNonLect  = !a.isLecture() && !b.isLecture();
            boolean bothLectures = a.isLecture() && b.isLecture();
            if (bothNonLect)  return !hasGap(a, b, 30);
            if (bothLectures) return allowLectureOverlap ? timesOverlap(a, b) : !hasGap(a, b, 30);
            return allowLectureOverlap ? timesOverlap(a, b) : !hasGap(a, b, 30);
        }

        return !hasGap(a, b, 30);
    }

    private boolean timesOverlap(ClassEntry a, ClassEntry b) {
        return a.getStartTime().isBefore(b.getEndTime())
            && b.getStartTime().isBefore(a.getEndTime());
    }

    private boolean hasGap(ClassEntry a, ClassEntry b, int minGapMinutes) {
        long gapAtoB = java.time.Duration.between(a.getEndTime(), b.getStartTime()).toMinutes();
        long gapBtoA = java.time.Duration.between(b.getEndTime(), a.getStartTime()).toMinutes();
        return gapAtoB >= minGapMinutes || gapBtoA >= minGapMinutes;
    }

    private boolean isBedfordTonsleyPair(String campusA, String campusB) {
        return (campusA.equalsIgnoreCase(BEDFORD_PARK) && campusB.equalsIgnoreCase(TONSLEY))
            || (campusA.equalsIgnoreCase(TONSLEY) && campusB.equalsIgnoreCase(BEDFORD_PARK));
    }

    /** Resolves campus by building inference first, then Topic fallback. */
    private String resolveCampus(ClassEntry c) {
        String fromBuilding = inferCampusFromBuilding(c.getBuilding());
        if (!fromBuilding.equalsIgnoreCase("Bedford Park")
                || c.getBuilding() == null || c.getBuilding().isBlank()) {
            return fromBuilding.toLowerCase();
        }
        return topicRepository.findByCourseCode(c.getCourseCode())
                .map(t -> t.getCampus().toLowerCase())
                .orElse("bedford park");
    }
}
