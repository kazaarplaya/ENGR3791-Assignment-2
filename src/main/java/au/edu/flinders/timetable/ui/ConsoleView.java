package au.edu.flinders.timetable.ui;

import au.edu.flinders.timetable.model.ClassEntry;
import au.edu.flinders.timetable.model.Timetable;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

/** Handles all formatted console output including the ASCII banner and timetable grid. */
public class ConsoleView {

    // Grid constants
    private static final LocalTime GRID_START   = LocalTime.of(8, 0);
    private static final LocalTime GRID_END     = LocalTime.of(20, 0);
    private static final int       SLOT_MINUTES = 30;
    private static final int       CELL_WIDTH   = 16;

    private static final DayOfWeek[] DAYS = {
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
    };

    /** Prints the ASCII art application banner to System.out. */
    public void printBanner() {
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║   STUDENT TIMETABLE OPTIMISER  v1.0      ║");
        System.out.println("║   Flinders University — ENGR3791         ║");
        System.out.println("╚══════════════════════════════════════════╝");
        System.out.println();
    }

    /** Prints a numbered menu from the supplied options array. */
    public void printMenu(String[] options) {
        System.out.println("─".repeat(44));
        for (int i = 0; i < options.length; i++) {
            System.out.printf("  %d. %s%n", i + 1, options[i]);
        }
        System.out.println("─".repeat(44));
    }

    /**
     * Draws a weekly timetable grid (Mon–Fri, 08:00–20:00 in 30-minute slots).
     * Each occupied cell shows "COURSECODE TYPE". Clashing slots are marked [CLASH].
     */
    public void printTimetable(Timetable t, List<ClassEntry> resolved) {
        System.out.println();
        System.out.println("  Timetable : " + t.getTimetableName()
            + "  [overlap=" + t.isOverlap()
            + ", prefs=" + t.isHasPreference() + "]");
        System.out.println();

        int slots = (int) java.time.Duration.between(GRID_START, GRID_END).toMinutes()
                    / SLOT_MINUTES;

        // Build header
        String timeLabel = padRight("Time", 8);
        StringBuilder header = new StringBuilder("┌" + "─".repeat(8) + "┬");
        StringBuilder titleRow = new StringBuilder("│" + timeLabel + "│");
        for (int d = 0; d < DAYS.length; d++) {
            titleRow.append(padCenter(DAYS[d].toString().substring(0, 3), CELL_WIDTH)).append("│");
            header.append("─".repeat(CELL_WIDTH));
            header.append(d < DAYS.length - 1 ? "┬" : "┐");
        }
        System.out.println(header);
        System.out.println(titleRow);

        // Separator
        String midSep = buildSeparator("├", "┼", "┤");
        System.out.println(midSep);

        // Rows
        for (int s = 0; s < slots; s++) {
            LocalTime slotTime = GRID_START.plusMinutes((long) s * SLOT_MINUTES);
            StringBuilder row = new StringBuilder("│");
            row.append(padRight(slotTime.toString(), 8)).append("│");

            for (DayOfWeek day : DAYS) {
                List<ClassEntry> inSlot = getEntriesInSlot(resolved, day, slotTime);
                String cell;
                if (inSlot.isEmpty()) {
                    cell = padRight("", CELL_WIDTH);
                } else if (inSlot.size() > 1) {
                    cell = padRight("[CLASH]", CELL_WIDTH);
                } else {
                    ClassEntry c = inSlot.get(0);
                    String label = c.getCourseCode() + " " + c.getType();
                    cell = padRight(label, CELL_WIDTH);
                }
                row.append(cell).append("│");
            }
            System.out.println(row);
        }

        // Bottom border
        System.out.println(buildSeparator("└", "┴", "┘"));
        System.out.println();
    }

    /** Prints a numbered list of ClassEntry objects. */
    public void printClassList(List<ClassEntry> classes) {
        if (classes.isEmpty()) {
            System.out.println("  (no results)");
            return;
        }
        for (int i = 0; i < classes.size(); i++) {
            System.out.printf("  %3d. %s%n", i + 1, classes.get(i));
        }
    }

    /** Prints a success message prefixed with [OK]. */
    public void printSuccess(String msg) {
        System.out.println("[OK] " + msg);
    }

    /** Prints an error message prefixed with [ERROR]. */
    public void printError(String msg) {
        System.out.println("[ERROR] " + msg);
    }

    /** Prints a warning message prefixed with [WARN]. */
    public void printWarning(String msg) {
        System.out.println("[WARN] " + msg);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /** Returns all ClassEntry objects that occupy the given day and 30-minute slot. */
    private List<ClassEntry> getEntriesInSlot(List<ClassEntry> entries,
                                               DayOfWeek day, LocalTime slotTime) {
        LocalTime slotEnd = slotTime.plusMinutes(SLOT_MINUTES);
        return entries.stream()
            .filter(c -> c.getDay() == day
                && c.getStartTime().isBefore(slotEnd)
                && c.getEndTime().isAfter(slotTime))
            .toList();
    }

    /** Builds a horizontal separator line with the given left, middle, and right connectors. */
    private String buildSeparator(String left, String mid, String right) {
        StringBuilder sb = new StringBuilder(left).append("─".repeat(8));
        for (int d = 0; d < DAYS.length; d++) {
            sb.append(d == 0 ? mid : mid).append("─".repeat(CELL_WIDTH));
        }
        sb.append(right);
        return sb.toString();
    }

    /** Right-pads a string to the given width, truncating if necessary. */
    private String padRight(String s, int width) {
        if (s.length() >= width) return s.substring(0, width);
        return s + " ".repeat(width - s.length());
    }

    /** Centre-pads a string to the given width. */
    private String padCenter(String s, int width) {
        if (s.length() >= width) return s.substring(0, width);
        int totalPad = width - s.length();
        int left  = totalPad / 2;
        int right = totalPad - left;
        return " ".repeat(left) + s + " ".repeat(right);
    }
}
