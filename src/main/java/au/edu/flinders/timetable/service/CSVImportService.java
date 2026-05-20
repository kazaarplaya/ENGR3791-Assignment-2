package au.edu.flinders.timetable.service;

import au.edu.flinders.timetable.model.ClassEntry;
import au.edu.flinders.timetable.model.Topic;
import au.edu.flinders.timetable.repository.ClassRepository;
import au.edu.flinders.timetable.repository.TopicRepository;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Handles importing Topics and ClassEntries from CSV files into their repositories. */
public class CSVImportService {

    /** Result returned by importFromTimetableFile() reporting new and updated record counts. */
    public record ImportResult(int newCount, int updatedCount) {}

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
     * Topics are inferred from the data and saved once per unique course code.
     * ClassEntries are deduplicated by (courseCode, type, instance, day, startTime).
     * When a duplicate is detected the existing record's time and location are updated.
     * Bad rows are logged to System.err and skipped.
     *
     * @return ImportResult with newCount (inserted) and updatedCount (overwritten)
     */
    public ImportResult importFromTimetableFile(String filePath) {
        int newCount     = 0;
        int updatedCount = 0;
        int lineNumber   = 0;

        Set<String> seenCourses = new HashSet<>();
        Set<String> seenSlots   = new HashSet<>();

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
                    String availability      = cols[1].trim();
                    String attendanceMode    = parseAttendanceMode(availability);
                    String campus            = parseCampus(availability);
                    int    semester          = parseSemester(availability);
                    int    availabilityNumber = parseAvailabilityNumber(availability);

                    // Column 2: class type
                    String classType = cols[2].trim();

                    // Column 3: class instance
                    int classInstance = Integer.parseInt(cols[3].trim());

                    // Column 4: date range → dateFrom / dateTo
                    String[] dateParts = cols[4].trim().split(" - ", 2);
                    String dateFrom = dateParts[0].trim();
                    String dateTo   = dateParts.length > 1 ? dateParts[1].trim() : dateFrom;

                    // Column 5: day (strip qualifier in parentheses)
                    String rawDay  = cols[5].trim();
                    int    parenIdx = rawDay.indexOf('(');
                    String dayStr  = (parenIdx >= 0 ? rawDay.substring(0, parenIdx) : rawDay).trim();
                    DayOfWeek day  = DayOfWeek.valueOf(dayStr.toUpperCase());

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

                    // Generate deterministic class ID
                    String classId = courseCode + "-"
                        + classType.replaceAll("[^A-Za-z0-9]", "") + "-"
                        + classInstance + "-"
                        + day.toString().substring(0, 3).toUpperCase() + "-"
                        + startTime.toString().replace(":", "");

                    // Deduplication key
                    String slotKey = courseCode + "|" + classType + "|"
                                   + classInstance + "|" + day + "|" + startTime;

                    ClassEntry entry = new ClassEntry(
                        classId, classType, null, startTime, endTime,
                        day, building, room, courseCode,
                        attendanceMode, availabilityNumber, classInstance, dateFrom, dateTo);

                    if (seenSlots.contains(slotKey)) {
                        // Update existing record (time / location may have changed)
                        classRepository.update(entry);
                        updatedCount++;
                    } else {
                        seenSlots.add(slotKey);
                        classRepository.save(entry);
                        newCount++;
                    }

                } catch (Exception e) {
                    System.err.println("[WARN] Skipping row " + lineNumber + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("[ERROR] Could not read timetable file '" + filePath + "': " + e.getMessage());
        }

        return new ImportResult(newCount, updatedCount);
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
        return switch (raw) {
            case "Flinders City Campus" -> "City";
            case "Bedford Park"         -> "Bedford Park";
            case "Tonsley"              -> "Tonsley";
            default                     -> raw;
        };
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
