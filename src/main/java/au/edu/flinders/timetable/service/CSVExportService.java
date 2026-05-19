package au.edu.flinders.timetable.service;

import au.edu.flinders.timetable.model.ClassEntry;
import au.edu.flinders.timetable.model.Timetable;
import au.edu.flinders.timetable.repository.ClassRepository;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Optional;

/** Handles exporting a Timetable to a CSV file. */
public class CSVExportService {

    private final ClassRepository classRepository;

    /** Constructs the service with the ClassRepository used to resolve class details. */
    public CSVExportService(ClassRepository classRepository) {
        this.classRepository = classRepository;
    }

    /**
     * Exports the given Timetable to a CSV file at the specified path.
     * Header: TimetableName,Semester,ClassId,Type,Day,StartTime,EndTime,Building,Room,CourseCode.
     * One data row is written per class ID in the timetable.
     * Throws IOException if the file cannot be written.
     */
    public void exportTimetable(Timetable t, String filePath) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write("TimetableName,Semester,ClassId,Type,Day,"
                       + "StartTime,EndTime,Building,Room,CourseCode");
            writer.newLine();

            for (String classId : t.getClassIds()) {
                Optional<ClassEntry> opt = classRepository.findById(classId);
                if (opt.isEmpty()) {
                    System.err.println("[WARN] Class ID '" + classId
                        + "' not found in repository; skipping export row.");
                    continue;
                }
                ClassEntry c = opt.get();
                String row = String.join(",",
                    escape(t.getTimetableName()),
                    escape(t.getSemester()),
                    escape(c.getClassId()),
                    escape(c.getType()),
                    escape(c.getDay().toString()),
                    escape(c.getStartTime().toString()),
                    escape(c.getEndTime().toString()),
                    escape(c.getBuilding()),
                    escape(c.getRoom()),
                    escape(c.getCourseCode())
                );
                writer.write(row);
                writer.newLine();
            }
        }
    }

    /** Wraps a value in double quotes if it contains a comma or double quote. */
    private String escape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
