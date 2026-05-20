package au.edu.flinders.timetable.ui;

import au.edu.flinders.timetable.model.ClassEntry;
import au.edu.flinders.timetable.model.Timetable;
import au.edu.flinders.timetable.model.Topic;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/** Handles all formatted console output including the ASCII banner and timetable grid. */
public class ConsoleView {

    // ── ANSI colour codes ─────────────────────────────────────────────────────
    private static final String RESET       = "[0m";
    private static final String BOLD        = "[1m";
    private static final String DIM         = "[2m";
    private static final String CYAN        = "[36m";
    private static final String BRIGHT_CYAN = "[96m";
    private static final String GREEN       = "[32m";
    private static final String BRIGHT_GREEN  = "[92m";
    private static final String RED         = "[31m";
    private static final String BRIGHT_RED    = "[91m";
    private static final String YELLOW      = "[33m";
    private static final String BRIGHT_YELLOW = "[93m";
    private static final String BLUE        = "[34m";
    private static final String BRIGHT_BLUE   = "[94m";
    private static final String MAGENTA     = "[35m";
    private static final String BRIGHT_WHITE  = "[97m";

    // ── Grid constants ────────────────────────────────────────────────────────
    private static final LocalTime GRID_START   = LocalTime.of(8, 0);
    private static final LocalTime GRID_END     = LocalTime.of(20, 0);
    private static final int       SLOT_MINUTES = 30;
    private static final int       CELL_WIDTH   = 16;

    private static final DayOfWeek[] DAYS = {
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
    };

    // ── Block-letter glyphs (7 rows each, 5 cols wide + 1 space) ─────────────
    private static final String[][] GLYPHS = {
        // S
        { " ████",
          "█    ",
          "█    ",
          " ███ ",
          "    █",
          "    █",
          " ████" },
        // T
        { "█████",
          "  █  ",
          "  █  ",
          "  █  ",
          "  █  ",
          "  █  ",
          "  █  " },
        // U
        { "█   █",
          "█   █",
          "█   █",
          "█   █",
          "█   █",
          "█   █",
          " ███ " },
        // D
        { "████ ",
          "█   █",
          "█   █",
          "█   █",
          "█   █",
          "█   █",
          "████ " },
        // E
        { "█████",
          "█    ",
          "█    ",
          "████ ",
          "█    ",
          "█    ",
          "█████" },
        // N
        { "█   █",
          "██  █",
          "█ █ █",
          "█  ██",
          "█   █",
          "█   █",
          "█   █" },
        // T (second)
        { "█████",
          "  █  ",
          "  █  ",
          "  █  ",
          "  █  ",
          "  █  ",
          "  █  " }
    };

    // ── Banner ────────────────────────────────────────────────────────────────

    /** Prints the ASCII art application banner to System.out. */
    public void printBanner() {
        int boxWidth = 60;
        String top    = "╔" + "═".repeat(boxWidth) + "╗";
        String bottom = "╚" + "═".repeat(boxWidth) + "╝";
        String blank  = "║" + " ".repeat(boxWidth) + "║";

        System.out.println(BRIGHT_CYAN + BOLD + top    + RESET);
        System.out.println(BRIGHT_CYAN + BOLD + blank  + RESET);

        // Block-letter "STUDENT" — 7 rows
        for (int row = 0; row < 7; row++) {
            StringBuilder line = new StringBuilder();
            for (String[] glyph : GLYPHS) {
                line.append(glyph[row]).append(" ");
            }
            String content = line.toString();
            int pad  = (boxWidth - content.length()) / 2;
            int rpad = boxWidth - content.length() - pad;
            String padded = " ".repeat(Math.max(0, pad)) + content + " ".repeat(Math.max(0, rpad));
            System.out.println(BRIGHT_CYAN + BOLD + "║" + RESET
                + BRIGHT_WHITE + BOLD + padded + RESET
                + BRIGHT_CYAN  + BOLD + "║" + RESET);
        }

        System.out.println(BRIGHT_CYAN + BOLD + blank + RESET);

        // Subtitle
        String subtitle = "T I M E T A B L E   O P T I M I S E R";
        System.out.println(BRIGHT_CYAN + BOLD + "║"
            + centreInWidth(BRIGHT_YELLOW + BOLD + subtitle + RESET, boxWidth,
                            BRIGHT_YELLOW + BOLD, RESET)
            + BRIGHT_CYAN + BOLD + "║" + RESET);

        System.out.println(BRIGHT_CYAN + BOLD + blank + RESET);

        String footer = "Flinders University  ◆  ENGR3791  ◆  v1.0";
        System.out.println(BRIGHT_CYAN + BOLD + "║"
            + centreInWidth(DIM + BRIGHT_WHITE + footer + RESET, boxWidth,
                            DIM + BRIGHT_WHITE, RESET)
            + BRIGHT_CYAN + BOLD + "║" + RESET);

        System.out.println(BRIGHT_CYAN + BOLD + blank  + RESET);
        System.out.println(BRIGHT_CYAN + BOLD + bottom + RESET);
        System.out.println();
    }

    // ── Menus ─────────────────────────────────────────────────────────────────

    /** Prints a numbered menu from the supplied options array. */
    public void printMenu(String[] options) {
        int boxWidth = 52;
        String title = "MAIN MENU";

        System.out.println(CYAN + "┌" + "─".repeat(boxWidth) + "┐" + RESET);
        System.out.println(CYAN + "│" + RESET
            + centreInWidth(BOLD + BRIGHT_CYAN + title + RESET, boxWidth, BOLD + BRIGHT_CYAN, RESET)
            + CYAN + "│" + RESET);
        System.out.println(CYAN + "├" + "─".repeat(boxWidth) + "┤" + RESET);
        System.out.println(CYAN + "│" + " ".repeat(boxWidth) + "│" + RESET);

        for (int i = 0; i < options.length; i++) {
            String num   = BRIGHT_YELLOW + BOLD + "[" + (i + 1) + "]" + RESET;
            String arrow = CYAN + "►" + RESET;
            String label = BRIGHT_WHITE + options[i] + RESET;
            String inner = "  " + num + "  " + arrow + "  " + label;
            String visible = "  [" + (i + 1) + "]  ►  " + options[i];
            int rpad = boxWidth - visible.length();
            System.out.println(CYAN + "│" + RESET + inner
                + " ".repeat(Math.max(0, rpad)) + CYAN + "│" + RESET);
            System.out.println(CYAN + "│" + " ".repeat(boxWidth) + "│" + RESET);
        }

        System.out.println(CYAN + "└" + "─".repeat(boxWidth) + "┘" + RESET);
        System.out.println(DIM + "  └─ Enter choice: _" + RESET);
        System.out.println();
    }

    // ── Notification boxes ────────────────────────────────────────────────────

    /** Prints a success message in a green bordered box. */
    public void printSuccess(String msg) {
        printNotification(BRIGHT_GREEN, "✓  OK", msg);
    }

    /** Prints an error message in a red bordered box. */
    public void printError(String msg) {
        printNotification(BRIGHT_RED, "✗  ERROR", msg);
    }

    /** Prints a warning message in a yellow bordered box. */
    public void printWarning(String msg) {
        printNotification(BRIGHT_YELLOW, "⚠  WARN", msg);
    }

    // ── Class list renderers ──────────────────────────────────────────────────

    /** Prints a numbered list of ClassEntry objects (legacy format, compact). */
    public void printClassList(List<ClassEntry> classes) {
        if (classes.isEmpty()) {
            System.out.println(DIM + CYAN + "┌" + "─".repeat(30) + "┐" + RESET);
            System.out.println(DIM + CYAN + "│" + RESET
                + centreInWidth("  (no results found)  ", 30, "", "")
                + DIM + CYAN + "│" + RESET);
            System.out.println(DIM + CYAN + "└" + "─".repeat(30) + "┘" + RESET);
            return;
        }

        int boxWidth = 70;
        String title = " CLASS RESULTS (" + classes.size() + ") ";
        System.out.println(CYAN + "┌─" + BOLD + BRIGHT_CYAN + title + RESET
            + CYAN + "─".repeat(Math.max(0, boxWidth - 2 - title.length())) + "┐" + RESET);

        for (int i = 0; i < classes.size(); i++) {
            ClassEntry c = classes.get(i);
            String row   = String.format("%3d", i + 1);
            String entry = c.getCourseCode() + " | " + c.getType()
                         + " grp" + c.getClassInstance()
                         + " | " + c.getDay()
                         + " " + c.getStartTime() + "–" + c.getEndTime()
                         + " | " + c.getBuilding() + " " + c.getRoom();
            if (entry.length() > 60) entry = entry.substring(0, 57) + "...";
            String inner = BRIGHT_YELLOW + row + RESET + "  " + BRIGHT_WHITE + entry + RESET;
            String visible = String.format("%3d  %s", i + 1, entry);
            int rpad = Math.max(0, boxWidth - Math.min(visible.length(), boxWidth));
            System.out.println(CYAN + "│" + RESET + " " + inner
                + " ".repeat(rpad - 1) + CYAN + "│" + RESET);
        }

        System.out.println(CYAN + "└" + "─".repeat(boxWidth) + "┘" + RESET);
        System.out.println();
    }

    /**
     * Prints a compact browse list of class entries with their topic names.
     * One row per entry: number, courseCode, topicName, type, instance, day, time, building/room.
     *
     * @param classes  the class entries to display
     * @param topicMap map from courseCode to Topic (may be missing entries)
     */
    public void printBrowseList(List<ClassEntry> classes, Map<String, Topic> topicMap) {
        if (classes.isEmpty()) {
            printWarning("No classes to display.");
            return;
        }

        // Column widths: # (3) + Code (9) + Topic (17) + Type (10) + Day (3) + Time (11) + Loc (9)
        // Total visible content: 1 + 3+1 + 9+1 + 17+1 + 10+1 + 3+2 + 11+2 + 9 = 71  → fits in 78
        int boxWidth = 78;
        String title = " CLASSES (" + classes.size() + ") ";
        System.out.println(CYAN + "┌─" + BOLD + BRIGHT_CYAN + title + RESET
            + CYAN + "─".repeat(Math.max(0, boxWidth - 2 - title.length())) + "┐" + RESET);

        // Column header  (1 leading space + columns + right padding)
        String hdr = String.format(" %-3s %-9s %-17s %-10s %-3s  %-11s  %-9s",
            "#", "Code", "Topic", "Type/Grp", "Day", "Time", "Location");
        hdr = padRight(hdr, boxWidth);
        System.out.println(CYAN + "│" + RESET + DIM + hdr + RESET + CYAN + "│" + RESET);
        System.out.println(CYAN + "├" + "─".repeat(boxWidth) + "┤" + RESET);

        for (int i = 0; i < classes.size(); i++) {
            ClassEntry c = classes.get(i);
            Topic t      = topicMap.get(c.getCourseCode());
            String topic = (t != null) ? t.getTopicName() : "";
            if (topic.length() > 17) topic = topic.substring(0, 14) + "...";
            String typeGrp  = c.getType() + " #" + c.getClassInstance();
            if (typeGrp.length() > 10) typeGrp = typeGrp.substring(0, 9) + ".";
            String day  = (c.getDay() != null) ? c.getDay().toString().substring(0, 3) : "---";
            String time = (c.getStartTime() != null)
                ? c.getStartTime() + "–" + c.getEndTime() : "";
            String location = c.getBuilding() != null ? c.getBuilding() : "";
            if (c.getRoom() != null && !c.getRoom().isBlank()) location += " " + c.getRoom();
            if (location.length() > 9) location = location.substring(0, 7) + "..";

            String row = String.format(" %-3d %-9s %-17s %-10s %-3s  %-11s  %-9s",
                i + 1, c.getCourseCode(), topic, typeGrp, day, time, location);
            row = padRight(row, boxWidth);
            System.out.println(CYAN + "│" + RESET
                + BRIGHT_WHITE + row + RESET
                + CYAN + "│" + RESET);
        }

        System.out.println(CYAN + "└" + "─".repeat(boxWidth) + "┘" + RESET);
        System.out.println();
    }

    /**
     * Prints a detailed view list of class entries, showing all fields in a
     * multi-line block per entry.
     *
     * @param classes  the class entries to display
     * @param topicMap map from courseCode to Topic (may be missing entries)
     */
    public void printViewList(List<ClassEntry> classes, Map<String, Topic> topicMap) {
        if (classes.isEmpty()) {
            printWarning("No classes to display.");
            return;
        }

        int boxWidth = 70;
        String title = " CLASS DETAIL VIEW (" + classes.size() + " entries) ";
        System.out.println(CYAN + "┌─" + BOLD + BRIGHT_CYAN + title + RESET
            + CYAN + "─".repeat(Math.max(0, boxWidth - 2 - title.length())) + "┐" + RESET);

        for (int i = 0; i < classes.size(); i++) {
            ClassEntry c = classes.get(i);
            Topic t      = topicMap.get(c.getCourseCode());
            String topicName    = (t != null) ? t.getTopicName()    : "(unknown)";
            String campus       = (t != null) ? t.getCampus()       : "";
            String semStr       = (t != null) ? "Sem " + t.getSemester() : "";

            printDetailRow(boxWidth, BRIGHT_YELLOW + BOLD + "#" + (i + 1)
                + "  " + c.getCourseCode() + "  " + topicName + RESET, true);
            printDetailRow(boxWidth, "  ID          : " + c.getClassId(), false);
            printDetailRow(boxWidth, "  Type        : " + c.getType()
                + "  Group #" + c.getClassInstance(), false);
            printDetailRow(boxWidth, "  Day & Time  : " + c.getDay()
                + "  " + c.getStartTime() + "–" + c.getEndTime(), false);
            printDetailRow(boxWidth, "  Location    : " + c.getBuilding()
                + (c.getRoom().isBlank() ? "" : ", " + c.getRoom()), false);
            printDetailRow(boxWidth, "  Dates       : " + c.getDateFrom()
                + " → " + c.getDateTo(), false);
            printDetailRow(boxWidth, "  Mode        : " + c.getAttendanceMode()
                + "  Campus: " + campus + "  " + semStr, false);
            printDetailRow(boxWidth, "  Availability: #" + c.getAvailabilityNumber(), false);

            if (i < classes.size() - 1) {
                System.out.println(CYAN + "├" + "─".repeat(boxWidth) + "┤" + RESET);
            }
        }

        System.out.println(CYAN + "└" + "─".repeat(boxWidth) + "┘" + RESET);
        System.out.println();
    }

    // ── Timetable grid ────────────────────────────────────────────────────────

    /**
     * Draws a weekly timetable grid (Mon–Fri, 08:00–20:00 in 30-minute slots),
     * followed by a detailed class legend below the grid.
     * Clashing slots are marked !! CLASH !! and highlighted in red.
     *
     * @param t        the timetable whose metadata populates the header
     * @param resolved the resolved ClassEntry objects to plot on the grid
     */
    public void printTimetable(Timetable t, List<ClassEntry> resolved) {
        System.out.println();

        // ── Header box ────────────────────────────────────────────────────────
        int hboxWidth = 58;
        System.out.println(BRIGHT_CYAN + BOLD + "╔" + "═".repeat(hboxWidth) + "╗" + RESET);
        printHeaderLine(hboxWidth, "◆  " + t.getTimetableName(), BRIGHT_WHITE + BOLD);
        printHeaderLine(hboxWidth,
            "Classes: " + t.getClassIds().size()
            + "   Overlap: " + (t.isOverlap() ? "yes" : "no")
            + "   Prefs: "   + (t.isHasPreference() ? "yes" : "no"),
            DIM + BRIGHT_WHITE);
        System.out.println(BRIGHT_CYAN + BOLD + "╚" + "═".repeat(hboxWidth) + "╝" + RESET);
        System.out.println();

        // ── Grid ──────────────────────────────────────────────────────────────
        int slots = (int) java.time.Duration.between(GRID_START, GRID_END).toMinutes()
                    / SLOT_MINUTES;

        // Top border
        StringBuilder topBorder = new StringBuilder(CYAN + "┌" + "─".repeat(8));
        for (int d = 0; d < DAYS.length; d++) {
            topBorder.append("┬").append("─".repeat(CELL_WIDTH));
        }
        topBorder.append("┐" + RESET);
        System.out.println(topBorder);

        // Day name header row
        StringBuilder dayRow = new StringBuilder(CYAN + "│" + RESET);
        dayRow.append(padCenter("", 8));
        dayRow.append(CYAN + "│" + RESET);
        for (DayOfWeek day : DAYS) {
            String dayName = day.toString().substring(0, 3);
            dayRow.append(BOLD + BRIGHT_CYAN + padCenter(dayName, CELL_WIDTH) + RESET)
                  .append(CYAN + "│" + RESET);
        }
        System.out.println(dayRow);

        // Heavy separator after day names
        StringBuilder heavySep = new StringBuilder(CYAN + "╞" + "═".repeat(8));
        for (int d = 0; d < DAYS.length; d++) {
            heavySep.append("╪").append("═".repeat(CELL_WIDTH));
        }
        heavySep.append("╡" + RESET);
        System.out.println(heavySep);

        // Slot rows
        for (int s = 0; s < slots; s++) {
            LocalTime slotTime = GRID_START.plusMinutes((long) s * SLOT_MINUTES);

            StringBuilder row = new StringBuilder(CYAN + "│" + RESET);
            row.append(DIM + padRight(slotTime.toString(), 8) + RESET);
            row.append(CYAN + "│" + RESET);

            for (DayOfWeek day : DAYS) {
                List<ClassEntry> inSlot = getEntriesInSlot(resolved, day, slotTime);
                if (inSlot.isEmpty()) {
                    row.append(" ".repeat(CELL_WIDTH));
                } else if (inSlot.size() > 1) {
                    row.append(BRIGHT_RED + BOLD + padCenter("!! CLASH !!", CELL_WIDTH) + RESET);
                } else {
                    ClassEntry c = inSlot.get(0);
                    String label = c.getCourseCode() + " " + c.getType();
                    row.append(BRIGHT_BLUE + padRight(label, CELL_WIDTH) + RESET);
                }
                row.append(CYAN + "│" + RESET);
            }
            System.out.println(row);

            if (s < slots - 1) {
                System.out.println(buildSeparator("├", "┼", "┤"));
            }
        }

        // Bottom border
        System.out.println(buildSeparator("└", "┴", "┘"));
        System.out.println();

        // ── Legend line ───────────────────────────────────────────────────────
        System.out.println(DIM + "  Legend: "
            + BRIGHT_BLUE + "■" + RESET + DIM + " = class   "
            + BRIGHT_RED + BOLD + "!!" + RESET + DIM + " = clash   "
            + "times in 30-min slots" + RESET);
        System.out.println();

        // ── Class detail blocks below the grid ────────────────────────────────
        if (!resolved.isEmpty()) {
            int detailWidth = 58;
            System.out.println(CYAN + BOLD + "┌─ Included Classes " + "─".repeat(detailWidth - 18) + "┐" + RESET);
            for (ClassEntry c : resolved) {
                String line = String.format("  %-10s | %-10s #%-2d | %-9s %s–%s | %s %s",
                    c.getCourseCode(),
                    c.getType(),
                    c.getClassInstance(),
                    (c.getDay() != null ? c.getDay().toString().substring(0, 3) : "---"),
                    (c.getStartTime() != null ? c.getStartTime() : ""),
                    (c.getEndTime()   != null ? c.getEndTime()   : ""),
                    c.getBuilding(),
                    c.getRoom());
                int rpad = Math.max(0, detailWidth - line.length());
                System.out.println(CYAN + "│" + RESET
                    + BRIGHT_WHITE + line + RESET
                    + " ".repeat(rpad)
                    + CYAN + "│" + RESET);
            }
            System.out.println(CYAN + "└" + "─".repeat(detailWidth) + "┘" + RESET);
            System.out.println();
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /** Returns all ClassEntry objects that occupy the given day and 30-minute slot. */
    private List<ClassEntry> getEntriesInSlot(List<ClassEntry> entries,
                                               DayOfWeek day, LocalTime slotTime) {
        LocalTime slotEnd = slotTime.plusMinutes(SLOT_MINUTES);
        return entries.stream()
            .filter(c -> c.getDay() == day
                && c.getStartTime() != null && c.getEndTime() != null
                && c.getStartTime().isBefore(slotEnd)
                && c.getEndTime().isAfter(slotTime))
            .toList();
    }

    /** Builds a horizontal separator line with the given left, middle, and right connectors. */
    private String buildSeparator(String left, String mid, String right) {
        StringBuilder sb = new StringBuilder(CYAN + left + RESET)
            .append("─".repeat(8));
        for (int d = 0; d < DAYS.length; d++) {
            sb.append(CYAN + mid + RESET).append("─".repeat(CELL_WIDTH));
        }
        sb.append(CYAN + right + RESET);
        return sb.toString();
    }

    /** Right-pads a string to the given width, truncating if necessary. */
    private String padRight(String s, int width) {
        if (s == null) s = "";
        if (s.length() >= width) return s.substring(0, width);
        return s + " ".repeat(width - s.length());
    }

    /** Centre-pads a string to the given width. */
    private String padCenter(String s, int width) {
        if (s == null) s = "";
        if (s.length() >= width) return s.substring(0, width);
        int totalPad = width - s.length();
        int left  = totalPad / 2;
        int right = totalPad - left;
        return " ".repeat(left) + s + " ".repeat(right);
    }

    /** Renders a single-line notification box (success / error / warn). */
    private void printNotification(String colour, String tag, String msg) {
        int innerWidth = Math.max(msg.length() + 2, tag.length() + 4);
        String top    = "╔─ " + tag + " " + "─".repeat(Math.max(0, innerWidth - tag.length() - 2)) + "╗";
        String middle = "║ " + padRight(msg, innerWidth) + " ║";
        String bot    = "╚" + "─".repeat(innerWidth + 2) + "╝";
        System.out.println(colour + BOLD + top    + RESET);
        System.out.println(colour + middle + RESET);
        System.out.println(colour + BOLD + bot    + RESET);
        System.out.println();
    }

    /** Prints one row of the timetable header box, centred. */
    private void printHeaderLine(int boxWidth, String text, String colour) {
        int pad  = (boxWidth - text.length()) / 2;
        int rpad = boxWidth - text.length() - pad;
        System.out.println(BRIGHT_CYAN + BOLD + "║" + RESET
            + " ".repeat(Math.max(0, pad))
            + colour + text + RESET
            + " ".repeat(Math.max(0, rpad))
            + BRIGHT_CYAN + BOLD + "║" + RESET);
    }

    /** Prints one row inside the detail view box, with box borders. */
    private void printDetailRow(int boxWidth, String text, boolean header) {
        String colour = header ? BRIGHT_CYAN + BOLD : BRIGHT_WHITE;
        int rpad = Math.max(0, boxWidth - text.length());
        System.out.println(CYAN + "│" + RESET
            + colour + text + RESET
            + " ".repeat(rpad)
            + CYAN + "│" + RESET);
    }

    /**
     * Centres {@code text} (which may contain ANSI codes) inside a field of {@code width}
     * visible characters. {@code colourOn} and {@code colourOff} wrap the padding spaces.
     */
    private String centreInWidth(String text, int width, String colourOn, String colourOff) {
        String visible = text.replaceAll("\\[[;\\d]*m", "");
        int visLen = visible.length();
        int pad    = Math.max(0, (width - visLen) / 2);
        int rpad   = Math.max(0, width - visLen - pad);
        return colourOn + " ".repeat(pad) + colourOff + text + colourOn + " ".repeat(rpad) + colourOff;
    }
}
