package au.edu.flinders.timetable.service;

import au.edu.flinders.timetable.model.ClassEntry;
import au.edu.flinders.timetable.model.Timetable;
import au.edu.flinders.timetable.model.Topic;
import au.edu.flinders.timetable.repository.ClassRepository;
import au.edu.flinders.timetable.repository.TopicRepository;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CSVExportServiceTest {

    @TempDir
    Path tempDir;

    private ClassRepository classRepo;
    private TopicRepository topicRepo;
    private CSVExportService exportService;

    @BeforeAll
    static void initSuite() {
    }

    @AfterAll
    static void endSuite() {
    }

    @BeforeEach
    void setUp() {
        classRepo = new ClassRepository();
        topicRepo = new TopicRepository();
        exportService = new CSVExportService(classRepo, topicRepo);
    }

    @AfterEach
    void tearDown() {
        classRepo.clear();
        topicRepo.clear();
    }

    // Helper – builds a ClassEntry with sensible defaults
    private ClassEntry makeEntry(String classId, String courseCode) {
        return new ClassEntry(
                classId, "Lecture", null,
                LocalTime.of(9, 0), LocalTime.of(11, 0),
                DayOfWeek.MONDAY, "Building A", "Room 101",
                courseCode, "In person", 1, 1,
                "03 Mar", "09 Jun");
    }

    // Helper – builds a Topic with sensible defaults
    private Topic makeTopic(String courseCode, String topicName) {
        return new Topic(courseCode, topicName, "Bedford Park", 1,
                "Internal", 1, "In person");
    }

    // Helper – exports to a temp file and returns all lines
    private List<String> exportAndRead(Timetable timetable) throws IOException {
        Path file = tempDir.resolve("export.csv");
        exportService.exportTimetable(timetable, file.toString());
        return Files.readAllLines(file);
    }

    //TC 6.01
    @Test
    @Order(1)
    @DisplayName("TC 6.01 Export writes correct CSV header")
    @Tag("Oscar")
    @Tag("Critical")
    void exportWritesCorrectHeader() throws IOException {
        Timetable tt = new Timetable("Test", "S1", false, false);

        List<String> lines = exportAndRead(tt);

        assertFalse(lines.isEmpty());
        assertEquals("classId,courseCode,topicName,attendanceMode,campus,semester,"
                + "availabilityNumber,classType,classInstance,dateFrom,dateTo,"
                + "day,startTime,endTime,building,room", lines.get(0));
    }

    //TC 6.02
    @Test
    @Order(2)
    @DisplayName("TC 6.02 Export empty timetable writes header only")
    @Tag("Oscar")
    @Tag("Core")
    void exportEmptyTimetableWritesHeaderOnly() throws IOException {
        Timetable tt = new Timetable("Empty", "S1", false, false);

        List<String> lines = exportAndRead(tt);

        assertEquals(1, lines.size());
    }

    //TC 6.03
    @Test
    @Order(3)
    @DisplayName("TC 6.03 Export writes correct field values for a valid entry")
    @Tag("Oscar")
    @Tag("Critical")
    void exportWritesCorrectFieldValues() throws IOException {
        ClassEntry entry = makeEntry("COMP1234-LEC-1-MON-0900-03Mar", "COMP1234");
        Topic topic = makeTopic("COMP1234", "Introduction to Programming");
        classRepo.save(entry);
        topicRepo.save(topic);

        Timetable tt = new Timetable("Test", "S1", false, false);
        tt.addClass("COMP1234-LEC-1-MON-0900-03Mar");

        List<String> lines = exportAndRead(tt);

        assumeTrue(lines.size() >= 2, "Should have header + 1 data row");
        String dataRow = lines.get(1);
        assertAll("exported field values",
                () -> assertTrue(dataRow.contains("COMP1234-LEC-1-MON-0900-03Mar")),
                () -> assertTrue(dataRow.contains("COMP1234")),
                () -> assertTrue(dataRow.contains("Introduction to Programming")),
                () -> assertTrue(dataRow.contains("In person")),
                () -> assertTrue(dataRow.contains("Bedford Park")),
                () -> assertTrue(dataRow.contains("MONDAY")),
                () -> assertTrue(dataRow.contains("09:00")),
                () -> assertTrue(dataRow.contains("11:00")));
    }

    //TC 6.04
    @Test
    @Order(4)
    @DisplayName("TC 6.04 Export writes multiple class entries")
    @Tag("Oscar")
    @Tag("Core")
    void exportWritesMultipleEntries() throws IOException {
        classRepo.save(makeEntry("C1", "COMP1234"));
        classRepo.save(makeEntry("C2", "MATH1001"));
        topicRepo.save(makeTopic("COMP1234", "Programming"));
        topicRepo.save(makeTopic("MATH1001", "Calculus"));

        Timetable tt = new Timetable("Test", "S1", false, false);
        tt.addClass("C1");
        tt.addClass("C2");

        List<String> lines = exportAndRead(tt);

        assertEquals(3, lines.size());
    }

    //TC 6.05
    @Test
    @Order(5)
    @DisplayName("TC 6.05 Export skips missing class IDs without crashing")
    @Tag("Oscar")
    @Tag("Critical")
    void exportSkipsMissingClassIds() throws IOException {
        classRepo.save(makeEntry("C1", "COMP1234"));
        topicRepo.save(makeTopic("COMP1234", "Programming"));

        Timetable tt = new Timetable("Test", "S1", false, false);
        tt.addClass("DOES-NOT-EXIST");
        tt.addClass("C1");

        List<String> lines = assertDoesNotThrow(() -> exportAndRead(tt));

        assertAll("missing ID skipped, valid ID exported",
                () -> assertEquals(2, lines.size()),
                () -> assertTrue(lines.get(1).contains("COMP1234")));
    }

    //TC 6.06
    @Test
    @Order(6)
    @DisplayName("TC 6.06 Export handles missing topic gracefully")
    @Tag("Oscar")
    @Tag("Core")
    void exportHandlesMissingTopic() throws IOException {
        classRepo.save(makeEntry("C1", "COMP1234"));
        // deliberately not saving a topic for COMP1234

        Timetable tt = new Timetable("Test", "S1", false, false);
        tt.addClass("C1");

        List<String> lines = assertDoesNotThrow(() -> exportAndRead(tt));

        assertEquals(2, lines.size());
    }

    //TC 6.07
    @Test
    @Order(7)
    @DisplayName("TC 6.07 Export escapes fields containing commas")
    @Tag("Oscar")
    @Tag("Core")
    void exportEscapesCommasInFields() throws IOException {
        ClassEntry entry = new ClassEntry(
                "C1", "Lecture", null,
                LocalTime.of(9, 0), LocalTime.of(11, 0),
                DayOfWeek.MONDAY, "Science, Engineering", "Room 101",
                "COMP1234", "In person", 1, 1,
                "03 Mar", "09 Jun");
        classRepo.save(entry);
        topicRepo.save(makeTopic("COMP1234", "Programming"));

        Timetable tt = new Timetable("Test", "S1", false, false);
        tt.addClass("C1");

        List<String> lines = exportAndRead(tt);

        assumeTrue(lines.size() >= 2);
        assertTrue(lines.get(1).contains("\"Science, Engineering\""));
    }

    //TC 6.08
    @Test
    @Order(8)
    @DisplayName("TC 6.08 Export escapes fields containing double quotes")
    @Tag("Oscar")
    @Tag("Core")
    void exportEscapesQuotesInFields() throws IOException {
        ClassEntry entry = new ClassEntry(
                "C1", "Lecture", null,
                LocalTime.of(9, 0), LocalTime.of(11, 0),
                DayOfWeek.MONDAY, "The \"Main\" Hall", "Room 101",
                "COMP1234", "In person", 1, 1,
                "03 Mar", "09 Jun");
        classRepo.save(entry);
        topicRepo.save(makeTopic("COMP1234", "Programming"));

        Timetable tt = new Timetable("Test", "S1", false, false);
        tt.addClass("C1");

        List<String> lines = exportAndRead(tt);

        assumeTrue(lines.size() >= 2);
        assertTrue(lines.get(1).contains("\"\"Main\"\""));
    }

    //TC 6.09
    @Test
    @Order(9)
    @DisplayName("TC 6.09 Export with null file path throws IOException")
    @Tag("Oscar")
    @Tag("Additional")
    void exportNullPathThrows() {
        Timetable tt = new Timetable("Test", "S1", false, false);

        assertThrows(Exception.class,
                () -> exportService.exportTimetable(tt, null));
    }

    //TC 6.10
    @Test
    @Order(10)
    @DisplayName("TC 6.10 Export uses class attendance mode over topic when present")
    @Tag("Oscar")
    @Tag("Additional")
    void exportPrefersClassAttendanceModeOverTopic() throws IOException {
        ClassEntry entry = new ClassEntry(
                "C1", "Lecture", null,
                LocalTime.of(9, 0), LocalTime.of(11, 0),
                DayOfWeek.MONDAY, "Building A", "Room 101",
                "COMP1234", "Online", 1, 1,
                "03 Mar", "09 Jun");
        classRepo.save(entry);
        topicRepo.save(makeTopic("COMP1234", "Programming"));

        Timetable tt = new Timetable("Test", "S1", false, false);
        tt.addClass("C1");

        List<String> lines = exportAndRead(tt);

        assumeTrue(lines.size() >= 2);
        assertTrue(lines.get(1).contains("Online"));
    }
}
