package au.edu.flinders.timetable.controller;

import au.edu.flinders.timetable.model.ClassEntry;
import au.edu.flinders.timetable.repository.ClassRepository;
import au.edu.flinders.timetable.repository.TopicRepository;
import au.edu.flinders.timetable.service.CSVImportService;
import au.edu.flinders.timetable.ui.ConsoleView;
import au.edu.flinders.timetable.ui.EarlyExitException;
import au.edu.flinders.timetable.ui.InputHelper;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ImportExportControllerTest {

    @TempDir
    Path tempDir;

    private TopicRepository topicRepo;
    private ClassRepository classRepo;
    private CSVImportService importService;

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
        importService = new CSVImportService(topicRepo, classRepo);
    }

    @AfterEach
    void tearDown() {
        topicRepo.clear();
        classRepo.clear();
    }

    
    private ImportExportController buildController(String userInput) {
        Scanner sc = new Scanner(userInput);
        return new ImportExportController(importService, new ConsoleView(), new InputHelper(), sc);
    }

    private String writeTimetableCsv() throws IOException {
        String header = "Topic,Availability,Class,Class instance,Date,Day,Time,Location";
        String row = "COMP1234 Introduction to Programming,"
                + "In person - Flinders City Campus - S1 - 1,"
                + "Lecture,1,03 Mar - 09 Jun,Monday,09:00 - 11:00,"
                + "\"Building A, Room 101\"";
        Path file = tempDir.resolve("timetable.csv");
        Files.write(file, List.of(header, row));
        return file.toString();
    }

    //TC 01
    @Test
    @Order(1)
    @DisplayName("TC 7.01 importData imports classes from a valid CSV path")
    @Tag("Oscar")
    @Tag("Critical")
    void importDataImportsFromValidPath() throws IOException {
        String csvPath = writeTimetableCsv();
        ImportExportController controller = buildController(csvPath + "\n");

        controller.importData();

        List<ClassEntry> stored = classRepo.findAll();
        assertFalse(stored.isEmpty());
        assertEquals("COMP1234", stored.get(0).getCourseCode());
    }

    //TC 02
    @Test
    @Order(2)
    @DisplayName("TC 7.02 importData with q input throws EarlyExitException")
    @Tag("Oscar")
    @Tag("Core")
    void importDataWithQuitInputThrowsEarlyExit() {
        ImportExportController controller = buildController("q\n");

        assertThrows(EarlyExitException.class, () -> controller.importData());
    }

    //TC 03
    @Test
    @Order(3)
    @DisplayName("TC 7.03 importData with nonexistent path does not crash")
    @Tag("Oscar")
    @Tag("Core")
    void importDataWithBadPathDoesNotCrash() {
        ImportExportController controller = buildController("/no/such/file.csv\n");

        assertDoesNotThrow(() -> controller.importData());
        assertTrue(classRepo.findAll().isEmpty());
    }

    //TC 04
    @Test
    @Order(4)
    @DisplayName("TC 7.04 importData with folder path imports all CSVs in folder")
    @Tag("Oscar")
    @Tag("Core")
    void importDataWithFolderImportsAllCsvs() throws IOException {
        writeTimetableCsv(); // writes timetable.csv into tempDir
        ImportExportController controller = buildController(tempDir.toString() + "\n");

        controller.importData();

        assertFalse(classRepo.findAll().isEmpty());
    }
}
