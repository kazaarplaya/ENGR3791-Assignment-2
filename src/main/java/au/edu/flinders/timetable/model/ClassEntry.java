package au.edu.flinders.timetable.model;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

/** Represents a single scheduled class session belonging to a topic. */
public class ClassEntry {

    private final String    classId;
    private final String    type;
    private final LocalDate date;       // nullable for recurring classes
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final DayOfWeek day;
    private final String    building;
    private final String    room;
    private final String    courseCode;

    /** Constructs a ClassEntry with all fields. date may be null for recurring classes. */
    public ClassEntry(String classId, String type, LocalDate date,
                      LocalTime startTime, LocalTime endTime,
                      DayOfWeek day, String building, String room, String courseCode) {
        this.classId    = classId;
        this.type       = type;
        this.date       = date;
        this.startTime  = startTime;
        this.endTime    = endTime;
        this.day        = day;
        this.building   = building;
        this.room       = room;
        this.courseCode = courseCode;
    }

    /** Returns the duration of this class in whole minutes. */
    public int durationMinutes() {
        return (int) Duration.between(startTime, endTime).toMinutes();
    }

    /** Returns true when this class is a lecture (case-insensitive). */
    public boolean isLecture() {
        return "lecture".equalsIgnoreCase(type);
    }

    /**
     * Parses a 9-column CSV row into a ClassEntry.
     * Column order: classId, type, date (yyyy-MM-dd or empty), startTime (HH:mm),
     * endTime (HH:mm), day (e.g. MONDAY), building, room, courseCode.
     * Throws IllegalArgumentException with a descriptive message on bad input.
     */
    public static ClassEntry fromCsvRow(String[] cols) {
        if (cols.length < 9) {
            throw new IllegalArgumentException(
                "Expected at least 9 columns but got " + cols.length);
        }

        String classId = cols[0].trim();
        String type    = cols[1].trim();

        LocalDate date = null;
        String rawDate = cols[2].trim();
        if (!rawDate.isEmpty()) {
            try {
                date = LocalDate.parse(rawDate);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException(
                    "Invalid date '" + rawDate + "' in column 3. Expected yyyy-MM-dd.");
            }
        }

        LocalTime startTime;
        try {
            startTime = LocalTime.parse(cols[3].trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                "Invalid startTime '" + cols[3].trim() + "' in column 4. Expected HH:mm.");
        }

        LocalTime endTime;
        try {
            endTime = LocalTime.parse(cols[4].trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                "Invalid endTime '" + cols[4].trim() + "' in column 5. Expected HH:mm.");
        }

        DayOfWeek day;
        try {
            day = DayOfWeek.valueOf(cols[5].trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "Invalid day '" + cols[5].trim() + "' in column 6. Expected e.g. MONDAY.");
        }

        String building  = cols[6].trim();
        String room      = cols[7].trim();
        String courseCode = cols[8].trim();

        if (courseCode.isEmpty()) {
            throw new IllegalArgumentException(
                "courseCode (column 9) cannot be blank.");
        }

        return new ClassEntry(classId, type, date, startTime, endTime, day, building, room, courseCode);
    }

    /** Returns the class ID (primary key). */
    public String getClassId()    { return classId; }

    /** Returns the class type, e.g. "Lecture" or "Tutorial". */
    public String getType()       { return type; }

    /** Returns the calendar date of this class, or null for recurring classes. */
    public LocalDate getDate()    { return date; }

    /** Returns the scheduled start time. */
    public LocalTime getStartTime() { return startTime; }

    /** Returns the scheduled end time. */
    public LocalTime getEndTime()   { return endTime; }

    /** Returns the day of the week this class runs. */
    public DayOfWeek getDay()     { return day; }

    /** Returns the building identifier. */
    public String getBuilding()   { return building; }

    /** Returns the room identifier. */
    public String getRoom()       { return room; }

    /** Returns the course code (foreign key to Topic). */
    public String getCourseCode() { return courseCode; }

    /** Returns a readable single-line summary of this class entry. */
    @Override
    public String toString() {
        String dateStr = (date != null) ? date.toString() : "recurring";
        return String.format("[%s] %s | %s | %s %s | %s-%s | %s %s",
            classId, courseCode, type, day, dateStr,
            startTime, endTime, building, room);
    }
}
