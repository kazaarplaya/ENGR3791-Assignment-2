package au.edu.flinders.timetable.service;

import au.edu.flinders.timetable.model.ClassEntry;
import au.edu.flinders.timetable.model.Topic;
import au.edu.flinders.timetable.repository.ClassRepository;
import au.edu.flinders.timetable.repository.TopicRepository;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Handles importing Topics and ClassEntries from CSV files into their repositories. */
public class CSVImportService {

    private final TopicRepository  topicRepository;
    private final ClassRepository  classRepository;

    /** Constructs the service with the repositories it will populate. */
    public CSVImportService(TopicRepository topicRepository, ClassRepository classRepository) {
        this.topicRepository = topicRepository;
        this.classRepository = classRepository;
    }

    /**
     * Reads a Topics CSV file (header + data rows) and saves each valid Topic.
     * Column order: CourseCode, Campus, Semester, Delivery, Num_of_Classes.
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
                if (lineNumber == 1) continue; // skip header
                if (line.isBlank()) continue;

                String[] cols = line.split(",", -1);
                if (cols[0].trim().isEmpty()) continue; // skip blank first column

                try {
                    String courseCode   = cols[0].trim();
                    String campus       = cols.length > 1 ? cols[1].trim() : "";
                    int    semester     = cols.length > 2 ? Integer.parseInt(cols[2].trim()) : 1;
                    String delivery     = cols.length > 3 ? cols[3].trim() : "";
                    int    numOfClasses = cols.length > 4 ? Integer.parseInt(cols[4].trim()) : 0;

                    Topic topic = new Topic(courseCode, campus, semester, delivery, numOfClasses);
                    topicRepository.save(topic);
                    imported.add(topic);
                } catch (Exception e) {
                    System.err.println("[WARN] Skipping Topics row " + lineNumber
                        + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("[ERROR] Could not read topics file '" + filePath
                + "': " + e.getMessage());
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
                if (lineNumber == 1) continue; // skip header
                if (line.isBlank()) continue;

                String[] cols = line.split(",", -1);
                if (cols[0].trim().isEmpty()) continue; // skip blank first column

                try {
                    ClassEntry entry = ClassEntry.fromCsvRow(cols);
                    classRepository.save(entry);
                    imported.add(entry);
                } catch (IllegalArgumentException e) {
                    System.err.println("[WARN] Skipping Classes row " + lineNumber
                        + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("[ERROR] Could not read classes file '" + filePath
                + "': " + e.getMessage());
        }

        return imported;
    }
}
