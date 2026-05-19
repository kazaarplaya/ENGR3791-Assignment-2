package au.edu.flinders.timetable.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Represents a student's generated weekly timetable. */
public class Timetable {

    private String       timetableName;
    private String       semester;
    private List<String> classIds;
    private boolean      isOverlap;
    private boolean      hasPreference;

    /** Constructs a new empty Timetable with the given name and configuration flags. */
    public Timetable(String timetableName, String semester,
                     boolean isOverlap, boolean hasPreference) {
        this.timetableName = timetableName;
        this.semester      = semester;
        this.classIds      = new ArrayList<>();
        this.isOverlap     = isOverlap;
        this.hasPreference = hasPreference;
    }

    /** Adds a class ID to this timetable if not already present. */
    public void addClass(String classId) {
        if (!classIds.contains(classId)) {
            classIds.add(classId);
        }
    }

    /** Removes a class ID from this timetable. */
    public void removeClass(String classId) {
        classIds.remove(classId);
    }

    /** Returns true when this timetable contains no classes. */
    public boolean isEmpty() {
        return classIds.isEmpty();
    }

    /** Returns the timetable name. */
    public String getTimetableName() { return timetableName; }

    /** Sets the timetable name. */
    public void setTimetableName(String timetableName) { this.timetableName = timetableName; }

    /** Returns the semester label. */
    public String getSemester() { return semester; }

    /** Sets the semester label. */
    public void setSemester(String semester) { this.semester = semester; }

    /** Returns an unmodifiable view of the class ID list. */
    public List<String> getClassIds() { return Collections.unmodifiableList(classIds); }

    /** Returns true if lecture overlap is enabled for this timetable. */
    public boolean isOverlap() { return isOverlap; }

    /** Sets whether lecture overlap is enabled. */
    public void setOverlap(boolean overlap) { isOverlap = overlap; }

    /** Returns true if preferences were applied when generating this timetable. */
    public boolean isHasPreference() { return hasPreference; }

    /** Sets whether preferences were applied. */
    public void setHasPreference(boolean hasPreference) { this.hasPreference = hasPreference; }

    /** Returns a readable summary of this timetable. */
    @Override
    public String toString() {
        return String.format("Timetable[name=%s, semester=%s, classes=%d, overlap=%b, prefs=%b]",
            timetableName, semester, classIds.size(), isOverlap, hasPreference);
    }
}
