package au.edu.flinders.timetable.model;

/** Represents a student's scheduling preferences used during timetable generation. */
public class Preference {

    private final String userId;
    private final String campus;
    private final String timeOfDay;
    private final String day;
    private final int    campusPriority;
    private final int    timeOfDayPriority;
    private final int    dayPriority;

    /**
     * Constructs a Preference with per-criterion priorities.
     * Use "Any" or an empty string for campus/timeOfDay/day to indicate no preference.
     * Priority 1 = most important; higher numbers = lower importance.
     * When two criteria share a priority level they are applied together.
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
    }

    /**
     * Returns true when at least one preference criterion is active
     * (i.e. not "Any" and not blank).
     */
    public boolean hasAnyCriteria() {
        return isActive(campus) || isActive(timeOfDay) || isActive(day);
    }

    private boolean isActive(String value) {
        return value != null && !value.isBlank() && !value.equalsIgnoreCase("Any");
    }

    /** Returns the user ID this preference belongs to. */
    public String getUserId()          { return userId; }

    /** Returns the preferred campus, or "Any" for no campus preference. */
    public String getCampus()          { return campus; }

    /** Returns the preferred time of day: "Morning", "Afternoon", "Evening", or "Any". */
    public String getTimeOfDay()       { return timeOfDay; }

    /** Returns the preferred day of the week, or "Any" for no day preference. */
    public String getDay()             { return day; }

    /** Returns the priority of the campus criterion (1 = most important). */
    public int getCampusPriority()     { return campusPriority; }

    /** Returns the priority of the time-of-day criterion (1 = most important). */
    public int getTimeOfDayPriority()  { return timeOfDayPriority; }

    /** Returns the priority of the day criterion (1 = most important). */
    public int getDayPriority()        { return dayPriority; }

    /** Returns a readable summary of this preference. */
    @Override
    public String toString() {
        return String.format(
            "Preference[user=%s, campus=%s(p%d), time=%s(p%d), day=%s(p%d)]",
            userId, campus, campusPriority, timeOfDay, timeOfDayPriority, day, dayPriority);
    }
}
