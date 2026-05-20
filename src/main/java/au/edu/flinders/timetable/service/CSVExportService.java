package au.edu.flinders.timetable.service;

import au.edu.flinders.timetable.model.ClassEntry;
import au.edu.flinders.timetable.model.Timetable;
import au.edu.flinders.timetable.model.Topic;
import au.edu.flinders.timetable.repository.ClassRepository;
import au.edu.flinders.timetable.repository.TopicRepository;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Optional;

/** Handles exporting a Timetable to a CSV file with all required spec fields. */
public class CSVExportService {

    private final ClassRepository classRepository;
    private final TopicRepository topicRepository;

    /** Constructs the service with the repositories used to resolve class and topic details. */
    public CSVExportService(ClassRepository classRepository, TopicRepository topicRepository) {
        this.classRepository = classRepository;
        this.topicRepository = topicRepository;
    }

    /**
     * Exports the given Timetable to a CSV file at the specified path.
     * Header: classId, courseCode, topicName, attendanceMode, campus, semester,
     *         availabilityNumber, classType, classInstance, dateFrom, dateTo,
     *         day, startTime, endTime, building, room.
     * One data row is written per class ID in the timetable.
     * Throws IOException if the file cannot be written.
     */
    public void exportTimetable(Timetable t, String filePath) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write("classId,courseCode,topicName,attendanceMode,campus,semester,"
                       + "availabilityNumber,classType,classInstance,dateFrom,dateTo,"
                       + "day,startTime,endTime,building,room");
            writer.newLine();

            for (String classId : t.getClassIds()) {
                Optional<ClassEntry> opt = classRepository.findById(classId);
                if (opt.isEmpty()) {
                    System.err.println("[WARN] Class ID '" + classId
                        + "' not found; skipping export row.");
                    continue;
                }
                ClassEntry c = opt.get();
                Optional<Topic> topicOpt = topicRepository.findByCourseCode(c.getCourseCode());
                String topicName   = topicOpt.map(Topic::getTopicName).orElse("");
                String campus      = topicOpt.map(Topic::getCampus).orElse("");
                String semesterStr = topicOpt.map(tp -> String.valueOf(tp.getSemester())).orElse("");
                String attendMode  = !c.getAttendanceMode().isBlank()
                                      ? c.getAttendanceMode()
                                      : topicOpt.map(Topic::getAttendanceMode).orElse("");

                String row = String.join(",",
                    escape(c.getClassId()),
                    escape(c.getCourseCode()),
                    escape(topicName),
                    escape(attendMode),
                    escape(campus),
                    escape(semesterStr),
                    escape(String.valueOf(c.getAvailabilityNumber())),
                    escape(c.getType()),
                    escape(String.valueOf(c.getClassInstance())),
                    escape(c.getDateFrom()),
                    escape(c.getDateTo()),
                    escape(c.getDay() != null ? c.getDay().toString() : ""),
                    escape(c.getStartTime() != null ? c.getStartTime().toString() : ""),
                    escape(c.getEndTime()   != null ? c.getEndTime().toString()   : ""),
                    escape(c.getBuilding()),
                    escape(c.getRoom())
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
