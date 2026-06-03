package au.edu.flinders.timetable.service;

import au.edu.flinders.timetable.model.ClassEntry;
import au.edu.flinders.timetable.repository.ClassRepository;
import au.edu.flinders.timetable.repository.TopicRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SearchServiceTest {

    private ClassRepository classRepo;
    private TopicRepository topicRepo;
    private SearchService searchService;

    @BeforeEach
    void setUp() {
        classRepo = new ClassRepository();
        topicRepo = new TopicRepository();
        searchService = new SearchService(classRepo, topicRepo);
    }

    /* Helper function to make class entries. Better for common classes*/
    private ClassEntry makeEntry(String classId, String courseCode) {
        return new ClassEntry(
                classId,
                "Lecture",
                null,
                LocalTime.of(9, 0),
                LocalTime.of(11, 0),
                DayOfWeek.MONDAY,
                "T1",
                "G42",
                courseCode,
                "In person",
                1,
                1,
                "01 Mar",
                "01 Mar"
        );
    }

    /* Helper that takes all params as nullable. Use defaults when null is passed */
    private ClassEntry makeEntry(
            String classId,
            String type,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            DayOfWeek day,
            String building,
            String room,
            String courseCode,
            String attendanceMode,
            Integer availabilityNumber,
            Integer classInstance,
            String dateFrom,
            String dateTo
    ) {
        return new ClassEntry(
                classId != null ? classId : "COMP1001-LEC-1-MON-0900-01Mar",
                type != null ? type : "Lecture",
                date,
                startTime != null ? startTime : LocalTime.of(9, 0),
                endTime != null ? endTime : LocalTime.of(11, 0),
                day != null ? day : DayOfWeek.MONDAY,
                building != null ? building : "T1",
                room != null ? room : "G42",
                courseCode != null ? courseCode : "COMP1001",
                attendanceMode != null ? attendanceMode : "In person",
                availabilityNumber != null ? availabilityNumber : 1,
                classInstance != null ? classInstance : 1,
                dateFrom != null ? dateFrom : "01 Mar",
                dateTo != null ? dateTo : "01 Mar"
        );
    }

    @Test
    @DisplayName("Keyword search matches course code")
    @Tag("TC 3.01")
    @Tag("Hans")
    @Tag("Critical")
    void keywordSearchMatchesCourseCode() {
        ClassEntry matchingClass = makeEntry("COMP1001-LEC-1-MON-0900-01Mar", "COMP1001");
        ClassEntry nonMatchingClass = makeEntry("MATH1001-LEC-1-MON-0900-01Mar", "MATH1001");

        classRepo.save(matchingClass);
        classRepo.save(nonMatchingClass);

        List<ClassEntry> results = searchService.search("COMP1001");

        assertEquals(1, results.size());
        assertEquals("COMP1001", results.get(0).getCourseCode());
    }

    @Test
    @DisplayName("")
    @Tag("TC 3.02")
    @Tag("Hans")
    @Tag("Core")
    void keywordSearchMatchesClassType() {
        ClassEntry newClass = makeEntry(
                "COMP1001-LEC-1-MON-0900-01Mar",
                "Tutorial",
                null,
                null,
                null,
                null,
                null,
                null,
                "COMP1001",
                null,
                null,
                null,
                null,
                null
        );

        classRepo.save(newClass);

        List<ClassEntry> results = searchService.search("Tutorial");

        assertEquals(1, results.size());
        assertEquals("Tutorial", results.get(0).getType());
    }

    @Test
    @DisplayName("")
    @Tag("TC 3.03")
    @Tag("Hans")
    @Tag("Core")
    void keywordSearchMatchesClassesWithNoValidCriteria() {
        ClassEntry newClass = makeEntry(
                "COMP1001-LEC-1-MON-0900-01Mar",
                "Tutorial",
                null,
                null,
                null,
                null,
                null,
                null,
                "COMP1001",
                null,
                null,
                null,
                null,
                null
        );

        classRepo.save(newClass);

        List<ClassEntry> results = searchService.search("Tutorial");

        assertEquals(1, results.size());
        assertEquals("Tutorial", results.get(0).getType());
    }
}