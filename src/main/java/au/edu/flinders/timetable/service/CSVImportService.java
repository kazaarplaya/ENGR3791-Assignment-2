package au.edu.flinders.timetable.service;

import au.edu.flinders.timetable.model.ClassEntry;
import au.edu.flinders.timetable.model.Topic;
import au.edu.flinders.timetable.repository.ClassRepository;
import au.edu.flinders.timetable.repository.TopicRepository;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Handles importing Topics and ClassEntries from CSV files into their repositories. */
public class CSVImportService {

    /** Result returned by importFromTimetableFile() reporting new and promoted record counts. */
    public static class ImportResult {
        private final int newCount;
        private final int updatedCount;

        public ImportResult(int newCount, int updatedCount) {
            this.newCount = newCount;
            this.updatedCount = updatedCount;
        }

        public int newCount() {
            return newCount;
        }

        public int updatedCount() {
            return updatedCount;
        }
    }

    private static final DateTimeFormatter MONTH_DAY_FMT =
        DateTimeFormatter.ofPattern("dd MMM");

    private final TopicRepository  topicRepository;
    private final ClassRepository  classRepository;

    /** Constructs the service with the repositories it will populate. */
    public CSVImportService(TopicRepository topicRepository, ClassRepository classRepository) {
        this.topicRepository = topicRepository;
        this.classRepository = classRepository;
    }

    // ── Legacy import methods (unchanged — covered by unit tests) ─────────────

    /**
     * Reads a Topics CSV file (header + data rows) and saves each valid Topic.
     * Column order: CourseCode, TopicName, Campus, Semester, Delivery, Num_of_Classes.
     * Bad rows are logged to System.err and skipped; parsing continues.
     * Returns the list of successfully imported Topics.
     */
    public List<Topic> importTopics(String filePath) {
        List<Topic> imported = new ArrayList<>();
        int lineNumber = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (lineNumber == 1) continue;
                if (line.isBlank()) continue;

                String[] cols = splitCsvLine(line);
                if (cols[0].trim().isEmpty()) continue;

                try {
                    String courseCode   = cols[0].trim();
                    String topicName    = cols.length > 1 ? cols[1].trim() : "";
                    String campus       = cols.length > 2 ? cols[2].trim() : "";
                    int    semester     = cols.length > 3 ? Integer.parseInt(cols[3].trim()) : 1;
                    String delivery     = cols.length > 4 ? cols[4].trim() : "";
                    int    numOfClasses = cols.length > 5 ? Integer.parseInt(cols[5].trim()) : 0;

                    Topic topic = new Topic(courseCode, topicName, campus, semester, delivery, numOfClasses);
                    topicRepository.save(topic);
                    imported.add(topic);
                } catch (Exception e) {
                    System.err.println("[WARN] Skipping Topics row " + lineNumber + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("[ERROR] Could not read topics file '" + filePath + "': " + e.getMessage());
        }

        return imported;
    }

    /**
     * Reads a Classes CSV file (header + data rows) and saves each valid ClassEntry.
     * Column order: Class_ID, Type, Date, StartTime, EndTime, Day, Building, Room, CourseCode.
     * Bad rows are logged to System.err and skipped; parsing continues.
     * Returns the list of successfully imported ClassEntries.
     */
    public List<ClassEntry> importClasses(String filePath) {
        List<ClassEntry> imported = new ArrayList<>();
        int lineNumber = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (lineNumber == 1) continue;
                if (line.isBlank()) continue;

                String[] cols = splitCsvLine(line);
                if (cols[0].trim().isEmpty()) continue;

                try {
                    ClassEntry entry = ClassEntry.fromCsvRow(cols);
                    classRepository.save(entry);
                    imported.add(entry);
                } catch (IllegalArgumentException e) {
                    System.err.println("[WARN] Skipping Classes row " + lineNumber + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("[ERROR] Could not read classes file '" + filePath + "': " + e.getMessage());
        }

        return imported;
    }

    // ── University timetable file import ──────────────────────────────────────

    /**
     * Imports Topics and ClassEntries from a single university timetable export file.
     * File format (8 columns): Topic, Availability, Class, Class instance,
     *                          Date, Day, Time, Location.
     *
     * <p>Every data row is stored as its own {@link ClassEntry} — there is no
     * deduplication. A unique class ID is generated for each row by combining the
     * course code, class type, instance number, day, start time, and the start of
     * the date range (dateFrom), so that rows which share the same group and
     * day/time but cover different date ranges produce distinct IDs.
     *
     * <p>Topics are inferred from the data and saved once per unique course code.
     * Bad rows are logged to {@code System.err} and skipped.
     *
     * @return ImportResult with newCount (rows inserted) and updatedCount (always 0)
     */
    public ImportResult importFromTimetableFile(String filePath) {
        int newCount   = 0;
        int lineNumber = 0;

        Set<String> seenCourses = new HashSet<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (lineNumber == 1) continue; // skip header
                if (line.isBlank()) continue;

                String[] cols = splitCsvLine(line);
                if (cols.length < 8 || cols[0].trim().isEmpty()) continue;

                try {
                    // Column 0: course code + topic name
                    String fullTopic   = cols[0].trim();
                    String[] nameParts = fullTopic.split("\\s+", 2);
                    String courseCode  = nameParts[0];
                    String topicName   = nameParts.length > 1 ? nameParts[1] : "";

                    // Column 1: availability → attendanceMode, campus, semester, availabilityNumber
                    String availability       = cols[1].trim();
                    String attendanceMode     = parseAttendanceMode(availability);
                    String campus             = parseCampus(availability);
                    int    semester           = parseSemester(availability);
                    int    availabilityNumber = parseAvailabilityNumber(availability);

                    // Column 2: class type
                    String classType = cols[2].trim();

                    // Column 3: class instance
                    int classInstance = Integer.parseInt(cols[3].trim());

                    // Column 4: date range → dateFrom / dateTo
                    String[] dateParts = cols[4].trim().split(" - ", 2);
                    String dateFrom = dateParts[0].trim();
                    String dateTo   = dateParts.length > 1 ? dateParts[1].trim() : dateFrom;

                    // Column 5: day (strip any parenthetical qualifier such as "(once-only)")
                    String rawDay   = cols[5].trim();
                    int    parenIdx = rawDay.indexOf('(');
                    String dayStr   = (parenIdx >= 0 ? rawDay.substring(0, parenIdx) : rawDay).trim();
                    DayOfWeek day   = DayOfWeek.valueOf(dayStr.toUpperCase());

                    // Column 6: time range "HH:mm - HH:mm"
                    String[] timeParts = cols[6].trim().split(" - ", 2);
                    LocalTime startTime = LocalTime.parse(timeParts[0].trim());
                    LocalTime endTime   = LocalTime.parse(timeParts[1].trim());

                    // Column 7: location → building + room
                    String location = cols[7].trim();
                    int commaIdx    = location.indexOf(',');
                    String building, room;
                    if (commaIdx >= 0) {
                        building = location.substring(0, commaIdx).trim();
                        room     = location.substring(commaIdx + 1).trim();
                    } else {
                        building = location;
                        room     = "";
                    }

                    // Save Topic once per courseCode
                    if (!seenCourses.contains(courseCode)) {
                        seenCourses.add(courseCode);
                        topicRepository.save(new Topic(
                            courseCode, topicName, campus, semester,
                            "In Person", 0, attendanceMode));
                    }

                    // Build a unique class ID per row.
                    // Including dateFrom (stripped to alphanumerics) ensures that rows
                    // for the same group/day/time in different date ranges get distinct IDs.
                    String dateTag = dateFrom.replaceAll("[^A-Za-z0-9]", "");
                    String classId = courseCode + "-"
                        + classType.replaceAll("[^A-Za-z0-9]", "") + "-"
                        + classInstance + "-"
                        + day.toString().substring(0, 3).toUpperCase() + "-"
                        + startTime.toString().replace(":", "") + "-"
                        + dateTag;

                    ClassEntry entry = new ClassEntry(
                        classId, classType, null, startTime, endTime,
                        day, building, room, courseCode,
                        attendanceMode, availabilityNumber, classInstance,
                        dateFrom, dateTo);

                    classRepository.save(entry);
                    newCount++;

                } catch (Exception e) {
                    System.err.println("[WARN] Skipping row " + lineNumber + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("[ERROR] Could not read timetable file '" + filePath + "': " + e.getMessage());
        }

        return new ImportResult(newCount, 0);
    }

    // ── Path-based entry point ────────────────────────────────────────────────

    /**
     * Imports from a single CSV file or every CSV file inside a folder.
     * Delegates each file to importFromTimetableFile().
     */
    public ImportResult importFromPath(String path) {
        File f = new File(path);
        if (f.isDirectory()) {
            File[] csvFiles = f.listFiles((dir, name) -> name.toLowerCase().endsWith(".csv"));
            if (csvFiles == null || csvFiles.length == 0) {
                System.err.println("[WARN] No CSV files found in: " + path);
                return new ImportResult(0, 0);
            }
            int totalNew = 0;
            for (File csv : csvFiles) {
                ImportResult r = importFromTimetableFile(csv.getAbsolutePath());
                totalNew += r.newCount();
            }
            return new ImportResult(totalNew, 0);
        }
        return importFromTimetableFile(path);
    }

    // ── Private parsing helpers ───────────────────────────────────────────────

    /** Extracts the attendance mode (first segment before " - ") from an Availability string. */
    private static String parseAttendanceMode(String availability) {
        int idx = availability.indexOf(" - ");
        return idx >= 0 ? availability.substring(0, idx).trim() : availability.trim();
    }

    /**
     * Extracts and normalises the campus name from "In person - {Campus} - S{N} - {avail}".
     * Applies the mapping required by TimetableGeneratorService clash detection.
     */
    private static String parseCampus(String availability) {
        String[] parts = availability.split(" - ", -1);
        String raw = parts.length > 1 ? parts[1].trim() : availability.trim();
        switch (raw) {
            case "Flinders City Campus":
                return "City";
            case "Bedford Park":
                return "Bedford Park";
            case "Tonsley":
                return "Tonsley";
            default:
                return raw;
        }
    }

    /** Extracts the semester digit from patterns like "- S1 -" in the availability string. */
    private static int parseSemester(String availability) {
        Matcher m = Pattern.compile("- S(\\d) -").matcher(availability);
        return m.find() ? Integer.parseInt(m.group(1)) : 1;
    }

    /** Extracts the trailing availability number (the last integer after the last " - "). */
    private static int parseAvailabilityNumber(String availability) {
        String[] parts = availability.split(" - ", -1);
        if (parts.length > 0) {
            try { return Integer.parseInt(parts[parts.length - 1].trim()); }
            catch (NumberFormatException ignored) {}
        }
        return 1;
    }

    // ── Date comparison helpers ───────────────────────────────────────────────

    /**
     * Parses a "dd MMM" date string into a MonthDay for chronological comparison.
     * Returns MonthDay.of(1, 1) on any parse failure.
     */
    private static MonthDay parseMonthDay(String s) {
        try {
            return MonthDay.parse(s == null ? "" : s.trim(), MONTH_DAY_FMT);
        } catch (Exception e) {
            return MonthDay.of(1, 1);
        }
    }

    /**
     * Returns whichever of the two "dd MMM" date strings falls earlier in the year.
     * Falls back to {@code a} when either string cannot be parsed.
     */
    private static String earlierDate(String a, String b) {
        MonthDay ma = parseMonthDay(a);
        MonthDay mb = parseMonthDay(b);
        return (ma.compareTo(mb) <= 0) ? a : b;
    }

    /**
     * Returns whichever of the two "dd MMM" date strings falls later in the year.
     * Falls back to {@code a} when either string cannot be parsed.
     */
    private static String laterDate(String a, String b) {
        MonthDay ma = parseMonthDay(a);
        MonthDay mb = parseMonthDay(b);
        return (ma.compareTo(mb) >= 0) ? a : b;
    }

    // ── CSV tokeniser ─────────────────────────────────────────────────────────

    /**
     * Splits one CSV line into tokens, respecting double-quoted fields.
     * A field surrounded by double quotes may contain commas.
     * Two consecutive double-quotes inside a quoted field represent a literal quote.
     */
    private String[] splitCsvLine(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(ch);
                }
            } else {
                if (ch == '"')      { inQuotes = true; }
                else if (ch == ',') { tokens.add(current.toString()); current.setLength(0); }
                else                { current.append(ch); }
            }
        }
        tokens.add(current.toString());
        return tokens.toArray(new String[0]);
    }
}
