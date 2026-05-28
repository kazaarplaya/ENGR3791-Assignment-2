package au.edu.flinders.timetable.ui;

/** Thrown when the user types the exit sentinel at any input prompt. */
public class EarlyExitException extends RuntimeException {
    public EarlyExitException() { super(); }
}
