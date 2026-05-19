package au.edu.flinders.timetable.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Represents a student user of the timetable application. */
public class User {

    private final String     userId;
    private final String     name;
    private final List<String> enrolledTopics;

    /** Constructs a User with the given ID and display name. */
    public User(String userId, String name) {
        this.userId         = userId;
        this.name           = name;
        this.enrolledTopics = new ArrayList<>();
    }

    /** Enrols this user in the topic identified by courseCode (no-op if already enrolled). */
    public void enrol(String courseCode) {
        if (!enrolledTopics.contains(courseCode)) {
            enrolledTopics.add(courseCode);
        }
    }

    /** Withdraws this user from the topic identified by courseCode. */
    public void withdraw(String courseCode) {
        enrolledTopics.remove(courseCode);
    }

    /** Returns true when this user is enrolled in the given courseCode. */
    public boolean isEnrolled(String courseCode) {
        return enrolledTopics.contains(courseCode);
    }

    /** Returns the user's unique ID. */
    public String getUserId() { return userId; }

    /** Returns the user's display name. */
    public String getName() { return name; }

    /** Returns an unmodifiable view of the enrolled course codes. */
    public List<String> getEnrolledTopics() {
        return Collections.unmodifiableList(enrolledTopics);
    }

    /** Returns a readable summary of this user. */
    @Override
    public String toString() {
        return String.format("User[id=%s, name=%s, topics=%s]", userId, name, enrolledTopics);
    }
}
