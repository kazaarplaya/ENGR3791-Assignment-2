package au.edu.flinders.timetable.service;

import java.time.LocalTime;

/**
 * Holds structured search criteria for class queries.
 * String fields use case-insensitive partial matching; null or blank means "any".
 * Integer fields use exact matching; -1 means "any".
 * LocalTime fields use exact matching; null means "any".
 */
public class SearchCriteria {

    /** Partial match on course code (e.g. "COMP"). */
    public String    courseCode;

    /** Partial match on topic name. */
    public String    topicName;

    /** Partial match on attendance mode (e.g. "In person"). */
    public String    attendanceMode;

    /** Partial match on campus name (e.g. "Bedford Park"). */
    public String    campus;

    /** Exact semester number; -1 = any. */
    public int       semester        = -1;

    /** Exact availability number; -1 = any. */
    public int       availabilityNumber = -1;

    /** Partial match on class type (e.g. "Lecture"). */
    public String    classType;

    /** Exact class instance number; -1 = any. */
    public int       classInstance   = -1;

    /** Partial match on start of date range (e.g. "02 Mar"). */
    public String    dateFrom;

    /** Partial match on end of date range (e.g. "16 Mar"). */
    public String    dateTo;

    /** Partial match on day of week (e.g. "Monday"). */
    public String    day;

    /** Exact start time; null = any. */
    public LocalTime startTime;

    /** Exact end time; null = any. */
    public LocalTime endTime;

    /** Partial match on building name. */
    public String    building;

    /** Partial match on room identifier. */
    public String    room;

    /** Returns true when no criteria have been set (all are "any"). */
    public boolean isEmpty() {
        return isBlank(courseCode)
            && isBlank(topicName)
            && isBlank(attendanceMode)
            && isBlank(campus)
            && semester             == -1
            && availabilityNumber   == -1
            && isBlank(classType)
            && classInstance        == -1
            && isBlank(dateFrom)
            && isBlank(dateTo)
            && isBlank(day)
            && startTime            == null
            && endTime              == null
            && isBlank(building)
            && isBlank(room);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
