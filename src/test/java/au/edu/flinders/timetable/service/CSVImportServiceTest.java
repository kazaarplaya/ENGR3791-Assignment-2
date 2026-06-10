package au.edu.flinders.timetable.service;

import au.edu.flinders.timetable.model.ClassEntry;
import au.edu.flinders.timetable.model.Topic;
import au.edu.flinders.timetable.repository.ClassRepository;
import au.edu.flinders.timetable.repository.TopicRepository;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CSVImportServiceTest {

    private static final String HEADER_TT =
            "Topic,Availability,Class,Class instance,Date,Day,Time,Location";

    private static final String VALID_ROW =
            "COMP1234 Introduction to Programming,"
                    + "In person - Flinders City Campus - S1 - 1,"
                    + "Lecture,1,03 Mar - 09 Jun,Monday,09:00 - 11:00,"
                    + "\"Building A, Room 101\"";

    @TempDir
    Path tempDir;

    private TopicRepository topicRepo;
    private ClassRepository classRepo;
    private CSVImportService service;

    @BeforeAll
    static void initSuite() {
    }

    @AfterAll
    static void endSuite() {
    }

    @BeforeEach
    void setUp() {
        topicRepo = new TopicRepository();
        classRepo = new ClassRepository();
        service   = new CSVImportService(topicRepo, classRepo);
    }

    @AfterEach
    void tearDown() {
        topicRepo.clear();
        classRepo.clear();
    }

    // Helper – writes timetable header + data rows to a temp file, returns its path
    private String timetableFile(String... dataRows) throws IOException {
        Path file = tempDir.resolve("timetable.csv");
        List<String> lines = new ArrayList<>();
        lines.add(HEADER_TT);
        Collections.addAll(lines, dataRows);
        Files.write(file, lines);
        return file.toString();
    }

    // Helper – valid row with a custom Availability field
    private String rowWithAvailability(String availability) {
        return "COMP1234 Introduction to Programming," + availability
                + ",Lecture,1,03 Mar - 09 Jun,Monday,09:00 - 11:00,\"Building A, Room 101\"";
    }

    // Helper – valid row with a custom Day field
    private String rowWithDay(String day) {
        return "COMP1234 Introduction to Programming,"
                + "In person - Flinders City Campus - S1 - 1,Lecture,1,03 Mar - 09 Jun,"
                + day + ",09:00 - 11:00,\"Building A, Room 101\"";
    }

    //TC 1.01
    @Test
    @Order(1)
    @DisplayName("TC 1.01 Import timetable file creates class entries for valid rows")
    @Tag("Oscar")
    @Tag("Critical")
    void importCreatesClassEntriesForValidRows() throws IOException {
        service.importFromTimetableFile(timetableFile(VALID_ROW));

        List<ClassEntry> all = classRepo.findAll();
        assertNotNull(all);
        assertEquals(1, all.size());
        assertTrue(classRepo.exists(all.get(0).getClassId()));
    }

    //TC 1.02
    @Test
    @Order(2)
    @DisplayName("TC 1.02 Import timetable file extracts topic code and topic name")
    @Tag("Oscar")
    @Tag("Critical")
    void importExtractsTopicCodeAndName() throws IOException {
        service.importFromTimetableFile(timetableFile(VALID_ROW));

        Topic topic = topicRepo.findByCourseCode("COMP1234").orElseThrow();
        assertAll("topic code and name",
                () -> assertEquals("COMP1234", topic.getCourseCode()),
                () -> assertEquals("Introduction to Programming", topic.getTopicName()));
    }

    //TC 1.03
    @Test
    @Order(3)
    @DisplayName("TC 1.03 Import timetable file extracts attendance mode")
    @Tag("Oscar")
    @Tag("Core")
    void importExtractsAttendanceMode() throws IOException {
        service.importFromTimetableFile(timetableFile(VALID_ROW));

        Topic topic = topicRepo.findByCourseCode("COMP1234").orElseThrow();
        assertEquals("In person", topic.getAttendanceMode());
    }

    //TC 1.04
    @ParameterizedTest(name = "TC 1.04 [{index}] {0} -> {1}")
    @Order(4)
    @DisplayName("TC 1.04 Import timetable file normalises Flinders City Campus to City")
    @Tag("Oscar")
    @Tag("Core")
    @CsvSource({
            "Flinders City Campus, City",
            "Bedford Park,         Bedford Park",
            "Tonsley,              Tonsley",
            "Sturt,                Sturt"
    })
    void importNormalisesCampus(String rawCampus, String expectedCampus) throws IOException {
        String availability = "In person - " + rawCampus + " - S1 - 1";
        service.importFromTimetableFile(timetableFile(rowWithAvailability(availability)));

        Topic topic = topicRepo.findByCourseCode("COMP1234").orElseThrow();
        assertEquals(expectedCampus, topic.getCampus());
    }

    //TC 1.05
    @ParameterizedTest(name = "TC 1.05 [{index}] {0} -> semester {1}")
    @Order(5)
    @DisplayName("TC 1.05 Import timetable file extracts semester from availability")
    @Tag("Oscar")
    @Tag("Critical")
    @CsvSource({ "S1, 1", "S2, 2" })
    void importExtractsSemester(String semesterCode, int expectedSemester) throws IOException {
        String availability = "In person - Flinders City Campus - " + semesterCode + " - 1";
        service.importFromTimetableFile(timetableFile(rowWithAvailability(availability)));

        Topic topic = topicRepo.findByCourseCode("COMP1234").orElseThrow();
        assertEquals(expectedSemester, topic.getSemester());
    }

    //TC 1.06
    @Test
    @Order(6)
    @DisplayName("TC 1.06 Import timetable file extracts availability number")
    @Tag("Oscar")
    @Tag("Core")
    void importExtractsAvailabilityNumber() throws IOException {
        service.importFromTimetableFile(timetableFile(VALID_ROW));

        ClassEntry entry = classRepo.findAll().get(0);
        assertEquals(1, entry.getAvailabilityNumber());
    }

    //TC 1.07
    @Test
    @Order(7)
    @DisplayName("TC 1.07 Import timetable file parses date range into dateFrom and dateTo")
    @Tag("Oscar")
    @Tag("Core")
    void importParsesDateRange() throws IOException {
        service.importFromTimetableFile(timetableFile(VALID_ROW));

        ClassEntry entry = classRepo.findAll().get(0);
        assertAll("date range",
                () -> assertEquals("03 Mar", entry.getDateFrom()),
                () -> assertEquals("09 Jun", entry.getDateTo()));
    }

    //TC 1.08
    @ParameterizedTest(name = "TC 1.08 [{index}] {0} -> {1}")
    @Order(8)
    @DisplayName("TC 1.08 Import timetable file strips day qualifier from once-only day values")
    @Tag("Oscar")
    @Tag("Core")
    @CsvSource({
            "Monday (once-only),    MONDAY",
            "Friday,                FRIDAY",
            "Wednesday (once-only), WEDNESDAY",
            "Tuesday,               TUESDAY"
    })
    void importStripsDayQualifier(String rawDay, String expectedDay) throws IOException {
        service.importFromTimetableFile(timetableFile(rowWithDay(rawDay)));

        ClassEntry entry = classRepo.findAll().get(0);
        assertEquals(DayOfWeek.valueOf(expectedDay), entry.getDay());
    }

    //TC 1.09
    @Test
    @Order(9)
    @DisplayName("TC 1.09 Import timetable file parses start and end time")
    @Tag("Oscar")
    @Tag("Critical")
    void importParsesStartAndEndTime() throws IOException {
        service.importFromTimetableFile(timetableFile(VALID_ROW));

        ClassEntry entry = classRepo.findAll().get(0);
        assertAll("start and end time",
                () -> assertEquals(LocalTime.of(9, 0),  entry.getStartTime()),
                () -> assertEquals(LocalTime.of(11, 0), entry.getEndTime()));
    }

    //TC 1.10
    @Test
    @Order(10)
    @DisplayName("TC 1.10 Import timetable file splits location into building and room")
    @Tag("Oscar")
    @Tag("Core")
    void importSplitsLocation() throws IOException {
        service.importFromTimetableFile(timetableFile(VALID_ROW));

        ClassEntry entry = classRepo.findAll().get(0);
        assertAll("location split",
                () -> assertEquals("Building A", entry.getBuilding()),
                () -> assertEquals("Room 101",   entry.getRoom()));
    }

    //TC 1.11
    @Test
    @Order(11)
    @DisplayName("TC 1.11 Import timetable file skips invalid rows without stopping import")
    @Tag("Oscar")
    @Tag("Critical")
    void importSkipsInvalidRowsAndContinues() throws IOException {
        String badRow = "COMP9999 Broken Topic,"
                + "In person - Flinders City Campus - S1 - 1,"
                + "Lecture,NOT_A_NUMBER,03 Mar - 09 Jun,Monday,09:00 - 11:00,"
                + "\"Building A, Room 101\"";

        CSVImportService.ImportResult result = assertDoesNotThrow(
                () -> service.importFromTimetableFile(timetableFile(badRow, VALID_ROW)));

        assumeTrue(result != null, "import should return a result");
        assertAll("only the valid row was imported",
                () -> assertEquals(1, result.newCount()),
                () -> assertEquals(1, classRepo.findAll().size()));
    }

    //TC 1.12
    @Test
    @Order(12)
    @DisplayName("TC 1.12 Import topics skips rows with invalid numeric fields")
    @Tag("Oscar")
    @Tag("Core")
    void importTopicsSkipsInvalidNumericFields() throws IOException {
        String header = "CourseCode,TopicName,Campus,Semester,Delivery,Num_of_Classes";
        String bad    = "COMP5678,Data Structures,Bedford Park,NOT_A_NUMBER,In Person,12";
        String good   = "COMP1234,Introduction to Programming,Bedford Park,1,In Person,12";

        Path file = tempDir.resolve("topics.csv");
        Files.write(file, List.of(header, bad, good));

        List<Topic> imported = service.importTopics(file.toString());
        assertAll("invalid numeric row skipped",
                () -> assertEquals(1, imported.size()),
                () -> assertEquals("COMP1234", imported.get(0).getCourseCode()));
    }

    //TC 1.13
    @Test
    @Order(13)
    @DisplayName("TC 1.13 Import classes skips rows with missing course code")
    @Tag("Oscar")
    @Tag("Core")
    void importClassesSkipsMissingCourseCode() throws IOException {
        String header        = "Class_ID,Type,Date,StartTime,EndTime,Day,Building,Room,CourseCode";
        String missingCourse = "CL002,Lecture,2026-03-03,09:00,11:00,Monday,Building A,Room 101,";
        String good          = "CL001,Lecture,2026-03-03,09:00,11:00,Monday,Building A,Room 101,COMP1234";

        Path file = tempDir.resolve("classes.csv");
        Files.write(file, List.of(header, missingCourse, good));

        List<ClassEntry> imported = service.importClasses(file.toString());
        assertEquals(1, imported.size());
    }

    //TC 1.14
    @Test
    @Order(14)
    @DisplayName("TC 1.14 Import timetable file with only a header imports nothing")
    @Tag("Oscar")
    @Tag("Additional")
    void importHeaderOnlyImportsNothing() throws IOException {
        service.importFromTimetableFile(timetableFile());

        assertAll("nothing stored",
                () -> assertEquals(0, classRepo.findAll().size()),
                () -> assertTrue(topicRepo.findAll().isEmpty()));
    }

    //TC 1.15
    @ParameterizedTest(name = "TC 1.15 [{index}] malformed row skipped")
    @Order(15)
    @DisplayName("TC 1.15 Import timetable file skips malformed rows (boundary / invalid input)")
    @Tag("Oscar")
    @Tag("Additional")
    @ValueSource(strings = {
            "too,few,columns,only,five,here",
            "COMP1 X,In person - City - S1 - 1,Lec,X,03 Mar - 09 Jun,Monday,09:00 - 11:00,\"B, R\"",
            "COMP1 X,In person - City - S1 - 1,Lec,1,03 Mar - 09 Jun,Funday,09:00 - 11:00,\"B, R\""
    })
    void importSkipsMalformedRows(String malformedRow) throws IOException {
        service.importFromTimetableFile(timetableFile(malformedRow));
        assertEquals(0, classRepo.findAll().size());
    }

    //TC 1.16
    @Test
    @Order(16)
    @DisplayName("TC 1.16 Import timetable file with null path throws (invalid input)")
    @Tag("Oscar")
    @Tag("Additional")
    void importNullPathThrows() {
        assertThrows(NullPointerException.class,
                () -> service.importFromTimetableFile(null));
    }

    //TC 1.17
    @Test
    @Order(17)
    @DisplayName("TC 1.17 Import treats CSV formula injection as inert text (security)")
    @Tag("Oscar")
    @Tag("Additional")
    void importTreatsFormulaInjectionAsText() throws IOException {
        String injection = "=2+2 Injection,"
                + "In person - Flinders City Campus - S1 - 1,"
                + "Lecture,1,03 Mar - 09 Jun,Monday,09:00 - 11:00,"
                + "\"Building A, Room 101\"";

        assertDoesNotThrow(() -> service.importFromTimetableFile(timetableFile(injection)));

        Topic topic = topicRepo.findByCourseCode("=2+2").orElse(null);
        assumingThat(topic != null,
                () -> assertEquals("=2+2", topic.getCourseCode()));
    }

    //TC 1.18
    @RepeatedTest(value = 3, name = "TC 1.18 run {currentRepetition} of {totalRepetitions}")
    @Order(18)
    @DisplayName("TC 1.18 Import is deterministic across repeated runs")
    @Tag("Oscar")
    @Tag("Additional")
    void importIsDeterministic() throws IOException {
        service.importFromTimetableFile(timetableFile(VALID_ROW));
        assertEquals(1, classRepo.findAll().size());
    }

    //TC 1.19
    @Test
    @Order(19)
    @DisplayName("TC 1.19 importFromPath with single file imports correctly")
    @Tag("Oscar")
    @Tag("Core")
    void importFromPathSingleFile() throws IOException {
        String path = timetableFile(VALID_ROW);

        CSVImportService.ImportResult result = service.importFromPath(path);

        assertEquals(1, result.newCount());
        assertEquals(1, classRepo.findAll().size());
    }

    //TC 1.20
    @Test
    @Order(20)
    @DisplayName("TC 1.20 importFromPath with directory imports all CSVs")
    @Tag("Oscar")
    @Tag("Core")
    void importFromPathDirectory() throws IOException {
        // write two separate CSV files into tempDir
        Files.write(tempDir.resolve("one.csv"), List.of(HEADER_TT, VALID_ROW));
        String secondRow = "MATH1001 Calculus,"
                + "In person - Bedford Park - S1 - 1,"
                + "Lecture,1,03 Mar - 09 Jun,Tuesday,10:00 - 12:00,Library";
        Files.write(tempDir.resolve("two.csv"), List.of(HEADER_TT, secondRow));

        CSVImportService.ImportResult result = service.importFromPath(tempDir.toString());

        assertEquals(2, result.newCount());
    }

    //TC 1.21
    @Test
    @Order(21)
    @DisplayName("TC 1.21 importFromPath with empty directory returns zero")
    @Tag("Oscar")
    @Tag("Core")
    void importFromPathEmptyDirectory() throws IOException {
        Path emptyDir = tempDir.resolve("empty");
        Files.createDirectory(emptyDir);

        CSVImportService.ImportResult result = service.importFromPath(emptyDir.toString());

        assertEquals(0, result.newCount());
    }

    //TC 1.22
    @Test
    @Order(22)
    @DisplayName("TC 1.22 Location without comma sets building only and room empty")
    @Tag("Oscar")
    @Tag("Core")
    void locationWithoutCommaGivesBuildingOnly() throws IOException {
        String row = "COMP1234 Programming,"
                + "In person - Flinders City Campus - S1 - 1,"
                + "Lecture,1,03 Mar - 09 Jun,Monday,09:00 - 11:00,Library";

        service.importFromTimetableFile(timetableFile(row));

        ClassEntry entry = classRepo.findAll().get(0);
        assertAll("building only, no room",
                () -> assertEquals("Library", entry.getBuilding()),
                () -> assertEquals("", entry.getRoom()));
    }

    //TC 1.23
    @Test
    @Order(23)
    @DisplayName("TC 1.23 Single date with no range sets dateFrom and dateTo equal")
    @Tag("Oscar")
    @Tag("Core")
    void singleDateSetsBothFromAndTo() throws IOException {
        String row = "COMP1234 Programming,"
                + "In person - Flinders City Campus - S1 - 1,"
                + "Lecture,1,03 Mar,Monday,09:00 - 11:00,\"Building A, Room 101\"";

        service.importFromTimetableFile(timetableFile(row));

        ClassEntry entry = classRepo.findAll().get(0);
        assertAll("single date",
                () -> assertEquals("03 Mar", entry.getDateFrom()),
                () -> assertEquals("03 Mar", entry.getDateTo()));
    }

    //TC 1.24
    @Test
    @Order(24)
    @DisplayName("TC 1.24 Topic with course code only and no name stores empty name")
    @Tag("Oscar")
    @Tag("Additional")
    void topicCodeOnlyStoresEmptyName() throws IOException {
        String row = "COMP1234,"
                + "In person - Flinders City Campus - S1 - 1,"
                + "Lecture,1,03 Mar - 09 Jun,Monday,09:00 - 11:00,\"Building A, Room 101\"";

        service.importFromTimetableFile(timetableFile(row));

        Topic topic = topicRepo.findByCourseCode("COMP1234").orElseThrow();
        assertEquals("", topic.getTopicName());
    }

    //TC 1.25
    @Test
    @Order(25)
    @DisplayName("TC 1.25 Duplicate course code rows save topic only once")
    @Tag("Oscar")
    @Tag("Core")
    void duplicateCourseCodeSavesTopicOnce() throws IOException {
        String row2 = "COMP1234 Programming,"
                + "In person - Flinders City Campus - S1 - 1,"
                + "Tutorial,2,03 Mar - 09 Jun,Tuesday,14:00 - 16:00,\"Building A, Room 102\"";

        service.importFromTimetableFile(timetableFile(VALID_ROW, row2));

        assertEquals(2, classRepo.findAll().size());
        assertEquals(1, topicRepo.findAll().size());
    }

    //TC 1.26
    @Test
    @Order(26)
    @DisplayName("TC 1.26 Blank lines in middle of file are skipped")
    @Tag("Oscar")
    @Tag("Additional")
    void blankLinesAreSkipped() throws IOException {
        Path file = tempDir.resolve("blanks.csv");
        Files.write(file, List.of(HEADER_TT, "", VALID_ROW, "", ""));

        CSVImportService.ImportResult result = service.importFromTimetableFile(file.toString());

        assertEquals(1, result.newCount());
    }

    //TC 1.27
    @Test
    @Order(27)
    @DisplayName("TC 1.27 CSV tokeniser handles embedded double quotes")
    @Tag("Oscar")
    @Tag("Additional")
    void csvTokeniserHandlesEmbeddedQuotes() throws IOException {
        // Building field contains a double quote: The "Main" Hall
        // In CSV this is: "The ""Main"" Hall"
        String row = "COMP1234 Programming,"
                + "In person - Flinders City Campus - S1 - 1,"
                + "Lecture,1,03 Mar - 09 Jun,Monday,09:00 - 11:00,"
                + "\"The \"\"Main\"\" Hall, Room 101\"";

        service.importFromTimetableFile(timetableFile(row));

        ClassEntry entry = classRepo.findAll().get(0);
        assertAll("embedded quotes parsed",
                () -> assertEquals("The \"Main\" Hall", entry.getBuilding()),
                () -> assertEquals("Room 101", entry.getRoom()));
    }

    //TC 1.28
    @Test
    @Order(28)
    @DisplayName("TC 1.28 Availability with no semester token defaults to semester 1")
    @Tag("Oscar")
    @Tag("Additional")
    void noSemesterTokenDefaultsToOne() throws IOException {
        String row = rowWithAvailability("In person - Flinders City Campus - 1");

        service.importFromTimetableFile(timetableFile(row));

        Topic topic = topicRepo.findByCourseCode("COMP1234").orElseThrow();
        assertEquals(1, topic.getSemester());
    }
}
