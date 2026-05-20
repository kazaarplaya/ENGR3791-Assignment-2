package au.edu.flinders.timetable.service;

import au.edu.flinders.timetable.model.ClassEntry;
import au.edu.flinders.timetable.model.Topic;
import au.edu.flinders.timetable.repository.ClassRepository;
import au.edu.flinders.timetable.repository.TopicRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Unit tests for CSVImportService. */
class CSVImportServiceTest {

    @TempDir
    Path tempDir;

    private TopicRepository    topicRepo;
    private ClassRepository    classRepo;
    private CSVImportService   service;

    @BeforeEach
    void setUp() {
        topicRepo = new TopicRepository();
        classRepo = new ClassRepository();
        service   = new CSVImportService(topicRepo, classRepo);
    }

    // ── Topic import tests ────────────────────────────────────────────────────

    @Test
      void importTopics_validFile_returnsCorrectCount() throws IOException {
        Path file = tempDir.resolve("topics.csv");
        Files.writeString(file,
            "CourseCode,TopicName,Campus,Semester,Delivery,Num_of_Classes\n"
          + "COMP1000,Computer Programming 1,City,1,In Person,24\n"
          + "MATH1000,Mathematics 1,Bedford Park,2,Online,12\n");

        List<Topic> result = service.importTopics(file.toString());

        assertEquals(2, result.size());
        assertEquals("COMP1000", result.get(0).getCourseCode());
        assertEquals("MATH1000", result.get(1).getCourseCode());
    }

    @Test
    void importTopics_badRow_skipsRowAndContinues() throws IOException {
        Path file = tempDir.resolve("topics_bad.csv");
        Files.writeString(file,
            "CourseCode,TopicName,Campus,Semester,Delivery,Num_of_Classes\n"
          + "COMP1000,Computer Programming 1,City,1,In Person,24\n"
          + "BADROW,Bad Topic,City,NOT_A_NUMBER,In Person,5\n"   // bad semester
          + "MATH1000,Mathematics 1,Bedford Park,2,Online,12\n");

        List<Topic> result = service.importTopics(file.toString());

        // Only the two valid rows should be imported
        assertEquals(2, result.size());
    }

    // ── Class import tests ────────────────────────────────────────────────────

    @Test
    void importClasses_validFile_parsesStartTimeCorrectly() throws IOException {
        Path file = tempDir.resolve("classes.csv");
        Files.writeString(file,
            "Class_ID,Type,Date,StartTime,EndTime,Day,Building,Room,CourseCode\n"
          + "C001,Lecture,,09:00,11:00,MONDAY,Registry,R101,COMP1000\n");

        List<ClassEntry> result = service.importClasses(file.toString());

        assertEquals(1, result.size());
        assertEquals(9, result.get(0).getStartTime().getHour());
        assertEquals(0, result.get(0).getStartTime().getMinute());
    }

    @Test
    void importClasses_missingCourseCode_throwsIllegalArgumentException() throws IOException {
        Path file = tempDir.resolve("classes_bad.csv");
        Files.writeString(file,
            "Class_ID,Type,Date,StartTime,EndTime,Day,Building,Room,CourseCode\n"
          + "C001,Lecture,,09:00,11:00,MONDAY,Registry,R101,\n");  // blank courseCode

        // The bad row should be skipped; the import should not throw
        List<ClassEntry> result = service.importClasses(file.toString());
        assertEquals(0, result.size());
    }
}
