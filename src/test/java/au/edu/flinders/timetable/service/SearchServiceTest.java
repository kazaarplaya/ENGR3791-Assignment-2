package au.edu.flinders.timetable.service;

import au.edu.flinders.timetable.model.ClassEntry;
import au.edu.flinders.timetable.model.Topic;
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
import java.util.Optional;

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

    /* Helper function to make topics. Common topics*/
    private Topic makeTopic(
            String courseCode,
            String topicName
    ) {
        return new Topic(
                courseCode,
                topicName,
                "Bedford Park",
                1,
                "Internal",
                1,
                "In person"
        );
    }

    /* Helper function to make topics */
    private Topic makeTopic(
            String courseCode,
            String topicName,
            String campus,
            Integer semester,
            String delivery,
            Integer numOfClasses,
            String attendanceMode
    ) {
        return new Topic(
                courseCode != null ? courseCode : "COMP1001",
                topicName != null ? topicName : "Programming Fundamentals",
                campus != null ? campus : "Bedford Park",
                semester != null ? semester : 1,
                delivery != null ? delivery : "Internal",
                numOfClasses != null ? numOfClasses : 1,
                attendanceMode != null ? attendanceMode : "In person"
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
    @DisplayName("Keyword search matches class type")
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
    @DisplayName("Keyword search returns no classes with no valid matches")
    @Tag("TC 3.03")
    @Tag("Hans")
    @Tag("Critical")
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

        ClassEntry secondClass = makeEntry(
                "MATH1001-LEC-1-MON-0900-01Mar",
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
        classRepo.save(secondClass);

        List<ClassEntry> results = searchService.search("Lecture");
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Criteria search with no criteria returns all classes")
    @Tag("TC 3.04")
    @Tag("Hans")
    @Tag("Critical")
    void criteriaSearchWithNoCriteriaReturnsAllClasses() {
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

        ClassEntry secondClass = makeEntry(
                "MATH1001-LEC-1-MON-0900-01Mar",
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

        SearchCriteria newCriteria = new SearchCriteria();

        classRepo.save(newClass);
        classRepo.save(secondClass);

        List<ClassEntry> results = searchService.search(newCriteria);
        assertEquals(2, results.size());
    }

    @Test
    @DisplayName("Criteria search filters by course code")
    @Tag("TC 3.04")
    @Tag("Hans")
    @Tag("Critical")
    void criteriaSearchFiltersByCourseCode() {
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

        ClassEntry secondClass = makeEntry(
                "MATH1001-LEC-1-MON-0900-01Mar",
                "Tutorial",
                null,
                null,
                null,
                null,
                null,
                null,
                "MATH1001",
                null,
                null,
                null,
                null,
                null
        );

        SearchCriteria newCriteria = new SearchCriteria();
        newCriteria.courseCode = "COMP";

        classRepo.save(newClass);
        classRepo.save(secondClass);

        List<ClassEntry> results = searchService.search(newCriteria);
        assertEquals(1, results.size());
    }

    @Test
    @DisplayName("Criteria search filters by topic name")
    @Tag("TC 3.05")
    @Tag("Hans")
    @Tag("Core")
    void criteriaSearchFiltersByTopicName() {
        ClassEntry matchingClass = makeEntry("C1", "COMP1001");
        ClassEntry nonMatchingClass = makeEntry("C2", "MATH1001");

        Topic matchingTopic = makeTopic("COMP1001", "Programming Fundamentals");
        Topic nonMatchingTopic = makeTopic("MATH1001", "Calculus Fundamentals");

        classRepo.save(matchingClass);
        classRepo.save(nonMatchingClass);

        topicRepo.save(matchingTopic);
        topicRepo.save(nonMatchingTopic);

        SearchCriteria criteria = new SearchCriteria();
        criteria.topicName = "Programming";

        List<ClassEntry> results = searchService.search(criteria);

        assertEquals(1, results.size());
        assertEquals("COMP1001", results.get(0).getCourseCode());
    }

    @Test
    @DisplayName("Criteria search filters by attendance mode")
    @Tag("TC 3.06")
    @Tag("Hans")
    @Tag("Core")
    void criteriaSearchFiltersByAttendanceMode() {
        ClassEntry matchingClass = makeEntry(
                "C1",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "COMP1001",
                "Online",
                null,
                null,
                null,
                null
        );

        ClassEntry nonMatchingClass = makeEntry(
                "C2",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "MATH1001",
                "In person",
                null,
                null,
                null,
                null
        );

        classRepo.save(matchingClass);
        classRepo.save(nonMatchingClass);

        SearchCriteria criteria = new SearchCriteria();
        criteria.attendanceMode = "Online";

        List<ClassEntry> results = searchService.search(criteria);

        assertEquals(1, results.size());
        assertEquals("Online", results.get(0).getAttendanceMode());
    }

    @Test
    @DisplayName("Criteria search filters by campus")
    @Tag("TC 3.07")
    @Tag("Hans")
    @Tag("Core")
    void criteriaSearchFiltersByCampus() {
        ClassEntry matchingClass = makeEntry("C1", "COMP1001");
        ClassEntry nonMatchingClass = makeEntry("C2", "MATH1001");

        Topic matchingTopic = makeTopic(
                "COMP1001",
                "Programming Fundamentals",
                "Bedford Park",
                null,
                null,
                null,
                null
        );

        Topic nonMatchingTopic = makeTopic(
                "MATH1001",
                "Calculus Fundamentals",
                "Tonsley",
                null,
                null,
                null,
                null
        );

        classRepo.save(matchingClass);
        classRepo.save(nonMatchingClass);

        topicRepo.save(matchingTopic);
        topicRepo.save(nonMatchingTopic);

        SearchCriteria criteria = new SearchCriteria();
        criteria.campus = "Bedford";

        List<ClassEntry> results = searchService.search(criteria);

        assertEquals(1, results.size());
        assertEquals("COMP1001", results.get(0).getCourseCode());
    }

    @Test
    @DisplayName("Criteria search filters by semester")
    @Tag("TC 3.08")
    @Tag("Hans")
    @Tag("Core")
    void criteriaSearchFiltersBySemester() {
        ClassEntry matchingClass = makeEntry("C1", "COMP1001");
        ClassEntry nonMatchingClass = makeEntry("C2", "MATH1001");

        Topic matchingTopic = makeTopic(
                "COMP1001",
                "Programming Fundamentals",
                null,
                1,
                null,
                null,
                null
        );

        Topic nonMatchingTopic = makeTopic(
                "MATH1001",
                "Calculus Fundamentals",
                null,
                2,
                null,
                null,
                null
        );

        classRepo.save(matchingClass);
        classRepo.save(nonMatchingClass);

        topicRepo.save(matchingTopic);
        topicRepo.save(nonMatchingTopic);

        SearchCriteria criteria = new SearchCriteria();
        criteria.semester = 1;

        List<ClassEntry> results = searchService.search(criteria);

        assertEquals(1, results.size());
        assertEquals("COMP1001", results.get(0).getCourseCode());
    }

    @Test
    @DisplayName("Criteria search filters by day")
    @Tag("TC 3.09")
    @Tag("Hans")
    @Tag("Core")
    void criteriaSearchFiltersByDay() {
        ClassEntry matchingClass = makeEntry(
                "C1",
                null,
                null,
                null,
                null,
                DayOfWeek.MONDAY,
                null,
                null,
                "COMP1001",
                null,
                null,
                null,
                null,
                null
        );

        ClassEntry nonMatchingClass = makeEntry(
                "C2",
                null,
                null,
                null,
                null,
                DayOfWeek.TUESDAY,
                null,
                null,
                "MATH1001",
                null,
                null,
                null,
                null,
                null
        );

        classRepo.save(matchingClass);
        classRepo.save(nonMatchingClass);

        SearchCriteria criteria = new SearchCriteria();
        criteria.day = "Monday";

        List<ClassEntry> results = searchService.search(criteria);

        assertEquals(1, results.size());
        assertEquals(DayOfWeek.MONDAY, results.get(0).getDay());
    }

    @Test
    @DisplayName("Criteria search applies multiple criteria")
    @Tag("TC 3.10")
    @Tag("Hans")
    @Tag("Critical")
    void criteriaSearchAppliesMultipleCriteria() {
        ClassEntry matchesBoth = makeEntry("C1", "COMP1001");
        ClassEntry matchesCourseCodeOnly = makeEntry("C2", "COMP2001");
        ClassEntry matchesTopicNameOnly = makeEntry("C3", "MATH1001");
        ClassEntry matchesNeither = makeEntry("C4", "MATH2001");

        topicRepo.save(makeTopic("COMP1001", "Programming Fundamentals"));
        topicRepo.save(makeTopic("COMP2001", "Calculus Fundamentals"));
        topicRepo.save(makeTopic("MATH1001", "Programming for Mathematics"));
        topicRepo.save(makeTopic("MATH2001", "Calculus Fundamentals"));

        classRepo.save(matchesBoth);
        classRepo.save(matchesCourseCodeOnly);
        classRepo.save(matchesTopicNameOnly);
        classRepo.save(matchesNeither);

        SearchCriteria criteria = new SearchCriteria();
        criteria.courseCode = "COMP";
        criteria.topicName = "Programming";

        List<ClassEntry> results = searchService.search(criteria);

        assertEquals(1, results.size());
        assertEquals("COMP1001", results.get(0).getCourseCode());
    }
}
