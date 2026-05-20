package au.edu.flinders.timetable.model;

import java.util.Objects;

/** Represents a university topic (subject) offering at a specific campus and semester. */
public class Topic {

    private final String courseCode;
    private final String topicName;
    private final String campus;
    private final int    semester;
    private final String delivery;
    private final int    numOfClasses;

    /** Constructs a Topic; throws IllegalArgumentException if courseCode is blank. */
    public Topic(String courseCode, String topicName, String campus,
                 int semester, String delivery, int numOfClasses) {
        if (courseCode == null || courseCode.isBlank()) {
            throw new IllegalArgumentException("courseCode cannot be blank");
        }
        this.courseCode   = courseCode;
        this.topicName    = (topicName == null) ? "" : topicName;
        this.campus       = campus;
        this.semester     = semester;
        this.delivery     = delivery;
        this.numOfClasses = numOfClasses;
    }

    /** Returns the course code (primary key). */
    public String getCourseCode()   { return courseCode; }

    /** Returns the human-readable topic name, e.g. "Computer Programming 1". */
    public String getTopicName()    { return topicName; }

    /** Returns the campus at which this topic is offered. */
    public String getCampus()       { return campus; }

    /** Returns the semester number (1 or 2). */
    public int getSemester()        { return semester; }

    /** Returns the delivery mode, e.g. "In Person" or "Online". */
    public String getDelivery()     { return delivery; }

    /** Returns the total number of class sessions for this topic. */
    public int getNumOfClasses()    { return numOfClasses; }

    /** Two topics are equal when their course codes match. */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Topic)) return false;
        Topic other = (Topic) o;
        return Objects.equals(courseCode, other.courseCode);
    }

    /** Hash based on course code only. */
    @Override
    public int hashCode() {
        return Objects.hash(courseCode);
    }

    /** Returns a readable summary: "COMP1000 – Computer Programming 1 – City (Semester 1)". */
    @Override
    public String toString() {
        return courseCode + " – " + topicName + " – " + campus + " (Semester " + semester + ")";
    }
}
