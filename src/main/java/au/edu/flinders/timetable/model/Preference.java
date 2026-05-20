package au.edu.flinders.timetable.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents a student's scheduling preferences used during timetable generation.
 * Preferences are stored as an ordered list of token strings.
 * The 7-argument legacy constructor converts old-style fields to tokens automatically,
 * preserving backward compatibility with existing code and unit tests.
 */
public class Preference {

    // ── Token constants ───────────────────────────────────────────────────────
    public static final String CAMPUS_BEDFORD_PARK = "CAMPUS_BEDFORD_PARK";
    public static final String CAMPUS_TONSLEY      = "CAMPUS_TONSLEY";
    public static final String CAMPUS_CITY         = "CAMPUS_CITY";
    public static final String CAMPUS_SAME         = "CAMPUS_SAME";
    public static final String TIME_MORNING        = "TIME_MORNING";
    public static final String TIME_AFTERNOON      = "TIME_AFTERNOON";
    public static final String TIME_EVENING        = "TIME_EVENING";
    public static final String DAY_MONDAY          = "DAY_MONDAY";
    public static final String DAY_TUESDAY         = "DAY_TUESDAY";
    public static final String DAY_WEDNESDAY       = "DAY_WEDNESDAY";
    public static final String DAY_THURSDAY        = "DAY_THURSDAY";
    public static final String DAY_FRIDAY          = "DAY_FRIDAY";
    public static final String SPREAD              = "SPREAD";
    public static final String COMPACT             = "COMPACT";

    /** All valid token strings. */
    public static final Set<String> VALID_TOKENS = Set.of(
        CAMPUS_BEDFORD_PARK, CAMPUS_TONSLEY, CAMPUS_CITY, CAMPUS_SAME,
        TIME_MORNING, TIME_AFTERNOON, TIME_EVENING,
        DAY_MONDAY, DAY_TUESDAY, DAY_WEDNESDAY, DAY_THURSDAY, DAY_FRIDAY,
        SPREAD, COMPACT
    );

    private final String userId;

    // Legacy fields kept for backward-compatible getters (may be null for token-style).
    private final String campus;
    private final String timeOfDay;
    private final String day;
    private final int    campusPriority;
    private final int    timeOfDayPriority;
    private final int    dayPriority;

    // Tokens ordered from highest to lowest priority.
    private final List<String> priorityOrder;

    // ── Constructors ──────────────────────────────────────────────────────────

    /**
     * Legacy 7-argument constructor (backward compatible with existing tests).
     * campus / timeOfDay / day values of "Any" or blank are treated as inactive.
     * Internally converts active criteria to tokens ordered by the supplied priorities.
     */
    public Preference(String userId, String campus, String timeOfDay, String day,
                      int campusPriority, int timeOfDayPriority, int dayPriority) {
        this.userId            = userId;
        this.campus            = (campus    == null) ? "Any" : campus;
        this.timeOfDay         = (timeOfDay == null) ? "Any" : timeOfDay;
        this.day               = (day       == null) ? "Any" : day;
        this.campusPriority    = campusPriority;
        this.timeOfDayPriority = timeOfDayPriority;
        this.dayPriority       = dayPriority;

        // Convert active criteria to tokens, ordered by priority number (ascending).
        List<Map.Entry<Integer, String>> entries = new ArrayList<>();
        String ct = oldCampusToToken(this.campus);
        if (ct != null) entries.add(Map.entry(campusPriority, ct));
        String tt = oldTimeOfDayToToken(this.timeOfDay);
        if (tt != null) entries.add(Map.entry(timeOfDayPriority, tt));
        String dt = oldDayToToken(this.day);
        if (dt != null) entries.add(Map.entry(dayPriority, dt));
        entries.sort(Map.Entry.comparingByKey());

        List<String> order = new ArrayList<>();
        entries.forEach(e -> order.add(e.getValue()));
        this.priorityOrder = List.copyOf(order);
    }

    /**
     * Token-style constructor for the new UI-driven preference selection.
     * priorityOrder is the list of active tokens from highest to lowest priority.
     */
    public Preference(String userId, List<String> priorityOrder) {
        this.userId            = userId;
        this.campus            = null;
        this.timeOfDay         = null;
        this.day               = null;
        this.campusPriority    = 0;
        this.timeOfDayPriority = 0;
        this.dayPriority       = 0;
        this.priorityOrder     = List.copyOf(priorityOrder);
    }

    // ── Token conversion helpers ──────────────────────────────────────────────

    private static String oldCampusToToken(String campus) {
        if (campus == null) return null;
        switch (campus.toLowerCase().trim()) {
            case "bedford park":
                return CAMPUS_BEDFORD_PARK;
            case "tonsley":
                return CAMPUS_TONSLEY;
            case "city":
                return CAMPUS_CITY;
            default:
                return null;
        }
    }

    private static String oldTimeOfDayToToken(String timeOfDay) {
        if (timeOfDay == null) return null;
        switch (timeOfDay.toLowerCase().trim()) {
            case "morning":
                return TIME_MORNING;
            case "afternoon":
                return TIME_AFTERNOON;
            case "evening":
                return TIME_EVENING;
            default:
                return null;
        }
    }

    private static String oldDayToToken(String day) {
        if (day == null) return null;
        switch (day.toLowerCase().trim()) {
            case "monday":
                return DAY_MONDAY;
            case "tuesday":
                return DAY_TUESDAY;
            case "wednesday":
                return DAY_WEDNESDAY;
            case "thursday":
                return DAY_THURSDAY;
            case "friday":
                return DAY_FRIDAY;
            default:
                return null;
        }
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    /** Returns the user ID this preference belongs to. */
    public String getUserId()               { return userId; }

    /**
     * Returns the preferred campus (legacy field).
     * Returns null for token-style preferences created with the 2-arg constructor.
     */
    public String getCampus()               { return campus; }

    /**
     * Returns the preferred time of day (legacy field).
     * Returns null for token-style preferences created with the 2-arg constructor.
     * Use this null-check to distinguish legacy vs token-style in services.
     */
    public String getTimeOfDay()            { return timeOfDay; }

    /**
     * Returns the preferred day of the week (legacy field).
     * Returns null for token-style preferences created with the 2-arg constructor.
     */
    public String getDay()                  { return day; }

    /** Returns the campus criterion priority (legacy field). */
    public int getCampusPriority()          { return campusPriority; }

    /** Returns the time-of-day criterion priority (legacy field). */
    public int getTimeOfDayPriority()       { return timeOfDayPriority; }

    /** Returns the day criterion priority (legacy field). */
    public int getDayPriority()             { return dayPriority; }

    /** Returns the ordered list of active preference tokens (highest to lowest priority). */
    public List<String> getPriorityOrder()  { return priorityOrder; }

    /** Returns true when at least one token is active. */
    public boolean hasAnyCriteria()         { return !priorityOrder.isEmpty(); }

    /** Returns a readable summary of this preference. */
    @Override
    public String toString() {
        return "Preference[user=" + userId + ", tokens=" + priorityOrder + "]";
    }
}
