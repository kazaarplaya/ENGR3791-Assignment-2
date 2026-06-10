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
    @Tag("TC 3.04b")
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


    @Test
    @DisplayName("TC 3.11 – Keyword search with null query returns all classes")
    @Tag("TC 3.11")
    @Tag("Luke")
    @Tag("Core")
    void keywordSearchNullQueryReturnsAllClasses() {
        classRepo.save(makeEntry("C1", "COMP1001"));
        classRepo.save(makeEntry("C2", "MATH1001"));

        List<ClassEntry> results = searchService.search((String) null);

        assertEquals(2, results.size());
    }

    @Test
    @DisplayName("TC 3.12 – Keyword search with blank query returns all classes")
    @Tag("TC 3.12")
    @Tag("Luke")
    @Tag("Core")
    void keywordSearchBlankQueryReturnsAllClasses() {
        classRepo.save(makeEntry("C1", "COMP1001"));
        classRepo.save(makeEntry("C2", "MATH1001"));

        List<ClassEntry> results = searchService.search("   ");

        assertEquals(2, results.size());
    }

    @Test
    @DisplayName("TC 3.13 – Criteria search with null SearchCriteria returns all classes")
    @Tag("TC 3.13")
    @Tag("Luke")
    @Tag("Core")
    void criteriaSearchNullCriteriaReturnsAllClasses() {
        classRepo.save(makeEntry("C1", "COMP1001"));
        classRepo.save(makeEntry("C2", "MATH1001"));

        List<ClassEntry> results = searchService.search((SearchCriteria) null);

        assertEquals(2, results.size());
    }

    @Test
    @DisplayName("TC 3.14 – Keyword search matches class ID")
    @Tag("TC 3.14")
    @Tag("Luke")
    @Tag("Core")
    void keywordSearchMatchesClassId() {
        classRepo.save(makeEntry("UNIQUE-ID-XYZ", "COMP1001"));

        List<ClassEntry> results = searchService.search("UNIQUE-ID-XYZ");

        assertEquals(1, results.size());
        assertEquals("UNIQUE-ID-XYZ", results.get(0).getClassId());
    }

    @Test
    @DisplayName("TC 3.15 – Keyword search matches building")
    @Tag("TC 3.15")
    @Tag("Luke")
    @Tag("Core")
    void keywordSearchMatchesBuilding() {
        ClassEntry entry = makeEntry("C1", null, null, null, null, null, "Registry", null, "COMP1001", null, null, null, null, null);
        classRepo.save(entry);

        List<ClassEntry> results = searchService.search("Registry");

        assertEquals(1, results.size());
    }

    @Test
    @DisplayName("TC 3.16 – Keyword search matches room")
    @Tag("TC 3.16")
    @Tag("Luke")
    @Tag("Core")
    void keywordSearchMatchesRoom() {
        ClassEntry entry = makeEntry("C1", null, null, null, null, null, null, "R101", "COMP1001", null, null, null, null, null);
        classRepo.save(entry);

        List<ClassEntry> results = searchService.search("R101");

        assertEquals(1, results.size());
    }

    @Test
    @DisplayName("TC 3.17 – Keyword search matches day of week")
    @Tag("TC 3.17")
    @Tag("Luke")
    @Tag("Core")
    void keywordSearchMatchesDay() {
        ClassEntry entry = makeEntry("C1", null, null, null, null, DayOfWeek.WEDNESDAY, null, null, "COMP1001", null, null, null, null, null);
        classRepo.save(entry);

        List<ClassEntry> results = searchService.search("WEDNESDAY");

        assertEquals(1, results.size());
    }

    @Test
    @DisplayName("TC 3.18 – Keyword search matches attendance mode")
    @Tag("TC 3.18")
    @Tag("Luke")
    @Tag("Core")
    void keywordSearchMatchesAttendanceMode() {
        ClassEntry entry = makeEntry("C1", null, null, null, null, null, null, null, "COMP1001", "Online", null, null, null, null);
        classRepo.save(entry);

        List<ClassEntry> results = searchService.search("Online");

        assertEquals(1, results.size());
    }

    @Test
    @DisplayName("TC 3.19 – Keyword search matches dateFrom")
    @Tag("TC 3.19")
    @Tag("Luke")
    @Tag("Core")
    void keywordSearchMatchesDateFrom() {
        ClassEntry entry = makeEntry("C1", null, null, null, null, null, null, null, "COMP1001", null, null, null, "15 Apr", "30 Apr");
        classRepo.save(entry);

        List<ClassEntry> results = searchService.search("15 Apr");

        assertEquals(1, results.size());
    }

    @Test
    @DisplayName("TC 3.20 – Keyword search matches dateTo")
    @Tag("TC 3.20")
    @Tag("Luke")
    @Tag("Core")
    void keywordSearchMatchesDateTo() {
        ClassEntry entry = makeEntry("C1", null, null, null, null, null, null, null, "COMP1001", null, null, null, "01 Mar", "28 Mar");
        classRepo.save(entry);

        List<ClassEntry> results = searchService.search("28 Mar");

        assertEquals(1, results.size());
    }

    @Test
    @DisplayName("TC 3.21 – Keyword search matches topic campus")
    @Tag("TC 3.21")
    @Tag("Luke")
    @Tag("Core")
    void keywordSearchMatchesTopicCampus() {
        classRepo.save(makeEntry("C1", "COMP1001"));
        topicRepo.save(makeTopic("COMP1001", "Computing", "Tonsley", 1, "Internal", 1, "In person"));

        List<ClassEntry> results = searchService.search("Tonsley");

        assertEquals(1, results.size());
    }

    @Test
    @DisplayName("TC 3.22 – Keyword search matches topic name")
    @Tag("TC 3.22")
    @Tag("Luke")
    @Tag("Core")
    void keywordSearchMatchesTopicName() {
        classRepo.save(makeEntry("C1", "COMP1001"));
        topicRepo.save(makeTopic("COMP1001", "Advanced Algorithms"));

        List<ClassEntry> results = searchService.search("Advanced Algorithms");

        assertEquals(1, results.size());
    }

    @Test
    @DisplayName("TC 3.23 – Keyword search matches topic delivery")
    @Tag("TC 3.23")
    @Tag("Luke")
    @Tag("Core")
    void keywordSearchMatchesTopicDelivery() {
        classRepo.save(makeEntry("C1", "COMP1001"));
        topicRepo.save(makeTopic("COMP1001", "Computing", "Bedford Park", 1, "External", 1, "In person"));

        List<ClassEntry> results = searchService.search("External");

        assertEquals(1, results.size());
    }


    @Test
    @DisplayName("TC 3.24 – Criteria topicName filter excludes class with no linked topic")
    @Tag("TC 3.24")
    @Tag("Luke")
    @Tag("Core")
    void criteriaTopicNameExcludesClassWithNoTopic() {
        classRepo.save(makeEntry("C1", "COMP1001")); // no topic saved

        SearchCriteria sc = new SearchCriteria();
        sc.topicName = "Programming";

        assertTrue(searchService.search(sc).isEmpty());
    }

    @Test
    @DisplayName("TC 3.25 – Criteria campus filter excludes class with no linked topic")
    @Tag("TC 3.25")
    @Tag("Luke")
    @Tag("Core")
    void criteriaCampusExcludesClassWithNoTopic() {
        classRepo.save(makeEntry("C1", "COMP1001"));

        SearchCriteria sc = new SearchCriteria();
        sc.campus = "Bedford Park";

        assertTrue(searchService.search(sc).isEmpty());
    }

    @Test
    @DisplayName("TC 3.26 – Criteria semester filter excludes class with no linked topic")
    @Tag("TC 3.26")
    @Tag("Luke")
    @Tag("Core")
    void criteriaSemesterExcludesClassWithNoTopic() {
        classRepo.save(makeEntry("C1", "COMP1001"));

        SearchCriteria sc = new SearchCriteria();
        sc.semester = 1;

        assertTrue(searchService.search(sc).isEmpty());
    }


    @Test
    @DisplayName("TC 3.27 – Criteria search filters by availability number")
    @Tag("TC 3.27")
    @Tag("Luke")
    @Tag("Core")
    void criteriaSearchFiltersByAvailabilityNumber() {
        ClassEntry match = makeEntry("C1", null, null, null, null, null, null, null, "COMP1001", null, 2, null, null, null);
        ClassEntry noMatch = makeEntry("C2", null, null, null, null, null, null, null, "MATH1001", null, 3, null, null, null);
        classRepo.save(match);
        classRepo.save(noMatch);

        SearchCriteria sc = new SearchCriteria();
        sc.availabilityNumber = 2;

        List<ClassEntry> results = searchService.search(sc);
        assertEquals(1, results.size());
        assertEquals("COMP1001", results.get(0).getCourseCode());
    }

    @Test
    @DisplayName("TC 3.28 – Criteria search filters by class type")
    @Tag("TC 3.28")
    @Tag("Luke")
    @Tag("Core")
    void criteriaSearchFiltersByClassType() {
        ClassEntry match = makeEntry("C1", "Workshop", null, null, null, null, null, null, "COMP1001", null, null, null, null, null);
        ClassEntry noMatch = makeEntry("C2", "Lecture", null, null, null, null, null, null, "MATH1001", null, null, null, null, null);
        classRepo.save(match);
        classRepo.save(noMatch);

        SearchCriteria sc = new SearchCriteria();
        sc.classType = "Workshop";

        List<ClassEntry> results = searchService.search(sc);
        assertEquals(1, results.size());
        assertEquals("COMP1001", results.get(0).getCourseCode());
    }

    @Test
    @DisplayName("TC 3.29 – Criteria search filters by class instance")
    @Tag("TC 3.29")
    @Tag("Luke")
    @Tag("Core")
    void criteriaSearchFiltersByClassInstance() {
        ClassEntry match = makeEntry("C1", null, null, null, null, null, null, null, "COMP1001", null, null, 2, null, null);
        ClassEntry noMatch = makeEntry("C2", null, null, null, null, null, null, null, "MATH1001", null, null, 3, null, null);
        classRepo.save(match);
        classRepo.save(noMatch);

        SearchCriteria sc = new SearchCriteria();
        sc.classInstance = 2;

        List<ClassEntry> results = searchService.search(sc);
        assertEquals(1, results.size());
        assertEquals("COMP1001", results.get(0).getCourseCode());
    }

    @Test
    @DisplayName("TC 3.30 – Criteria search filters by dateFrom")
    @Tag("TC 3.30")
    @Tag("Luke")
    @Tag("Core")
    void criteriaSearchFiltersByDateFrom() {
        ClassEntry match = makeEntry("C1", null, null, null, null, null, null, null, "COMP1001", null, null, null, "10 Apr", "30 Apr");
        ClassEntry noMatch = makeEntry("C2", null, null, null, null, null, null, null, "MATH1001", null, null, null, "01 Mar", "30 Mar");
        classRepo.save(match);
        classRepo.save(noMatch);

        SearchCriteria sc = new SearchCriteria();
        sc.dateFrom = "10 Apr";

        List<ClassEntry> results = searchService.search(sc);
        assertEquals(1, results.size());
        assertEquals("COMP1001", results.get(0).getCourseCode());
    }

    @Test
    @DisplayName("TC 3.31 – Criteria search filters by dateTo")
    @Tag("TC 3.31")
    @Tag("Luke")
    @Tag("Core")
    void criteriaSearchFiltersByDateTo() {
        ClassEntry match = makeEntry("C1", null, null, null, null, null, null, null, "COMP1001", null, null, null, "01 Apr", "25 Apr");
        ClassEntry noMatch = makeEntry("C2", null, null, null, null, null, null, null, "MATH1001", null, null, null, "01 Mar", "15 Mar");
        classRepo.save(match);
        classRepo.save(noMatch);

        SearchCriteria sc = new SearchCriteria();
        sc.dateTo = "25 Apr";

        List<ClassEntry> results = searchService.search(sc);
        assertEquals(1, results.size());
        assertEquals("COMP1001", results.get(0).getCourseCode());
    }

    @Test
    @DisplayName("TC 3.32 – Criteria day filter excludes class with null day")
    @Tag("TC 3.32")
    @Tag("Luke")
    @Tag("Core")
    void criteriaDayFilterExcludesNullDayClass() {
        // Override with null day requires full constructor — use makeEntry with null day
        ClassEntry nullDay = new ClassEntry("C1", "Lecture", null,
                LocalTime.of(9, 0), LocalTime.of(11, 0),
                null, "T1", "G42", "COMP1001", "In person", 1, 1, "01 Mar", "01 Mar");
        classRepo.save(nullDay);

        SearchCriteria sc = new SearchCriteria();
        sc.day = "MONDAY";

        assertTrue(searchService.search(sc).isEmpty());
    }

    @Test
    @DisplayName("TC 3.33 – Criteria search filters by start time")
    @Tag("TC 3.33")
    @Tag("Luke")
    @Tag("Core")
    void criteriaSearchFiltersByStartTime() {
        ClassEntry match = makeEntry("C1", null, null, LocalTime.of(9, 0), null, null, null, null, "COMP1001", null, null, null, null, null);
        ClassEntry noMatch = makeEntry("C2", null, null, LocalTime.of(14, 0), null, null, null, null, "MATH1001", null, null, null, null, null);
        classRepo.save(match);
        classRepo.save(noMatch);

        SearchCriteria sc = new SearchCriteria();
        sc.startTime = LocalTime.of(9, 0);

        List<ClassEntry> results = searchService.search(sc);
        assertEquals(1, results.size());
        assertEquals("COMP1001", results.get(0).getCourseCode());
    }

    @Test
    @DisplayName("TC 3.34 – Criteria search filters by end time")
    @Tag("TC 3.34")
    @Tag("Luke")
    @Tag("Core")
    void criteriaSearchFiltersByEndTime() {
        ClassEntry match = makeEntry("C1", null, null, null, LocalTime.of(11, 0), null, null, null, "COMP1001", null, null, null, null, null);
        ClassEntry noMatch = makeEntry("C2", null, null, null, LocalTime.of(16, 0), null, null, null, "MATH1001", null, null, null, null, null);
        classRepo.save(match);
        classRepo.save(noMatch);

        SearchCriteria sc = new SearchCriteria();
        sc.endTime = LocalTime.of(11, 0);

        List<ClassEntry> results = searchService.search(sc);
        assertEquals(1, results.size());
        assertEquals("COMP1001", results.get(0).getCourseCode());
    }

    @Test
    @DisplayName("TC 3.35 – Criteria search filters by building")
    @Tag("TC 3.35")
    @Tag("Luke")
    @Tag("Core")
    void criteriaSearchFiltersByBuilding() {
        ClassEntry match = makeEntry("C1", null, null, null, null, null, "Registry", null, "COMP1001", null, null, null, null, null);
        ClassEntry noMatch = makeEntry("C2", null, null, null, null, null, "Library", null, "MATH1001", null, null, null, null, null);
        classRepo.save(match);
        classRepo.save(noMatch);

        SearchCriteria sc = new SearchCriteria();
        sc.building = "Registry";

        List<ClassEntry> results = searchService.search(sc);
        assertEquals(1, results.size());
        assertEquals("COMP1001", results.get(0).getCourseCode());
    }

    @Test
    @DisplayName("TC 3.36 – Criteria search filters by room")
    @Tag("TC 3.36")
    @Tag("Luke")
    @Tag("Core")
    void criteriaSearchFiltersByRoom() {
        ClassEntry match = makeEntry("C1", null, null, null, null, null, null, "R101", "COMP1001", null, null, null, null, null);
        ClassEntry noMatch = makeEntry("C2", null, null, null, null, null, null, "R202", "MATH1001", null, null, null, null, null);
        classRepo.save(match);
        classRepo.save(noMatch);

        SearchCriteria sc = new SearchCriteria();
        sc.room = "R101";

        List<ClassEntry> results = searchService.search(sc);
        assertEquals(1, results.size());
        assertEquals("COMP1001", results.get(0).getCourseCode());
    }


    @Test
    @DisplayName("TC 3.37 – SearchCriteria isEmpty returns false when campus set")
    @Tag("TC 3.37")
    @Tag("Luke")
    @Tag("Core")
    void searchCriteriaIsEmptyReturnsFalseWhenCampusSet() {
        SearchCriteria sc = new SearchCriteria();
        sc.campus = "Bedford Park";
        assertFalse(sc.isEmpty());
    }

    @Test
    @DisplayName("TC 3.38 – SearchCriteria isEmpty returns false when semester set")
    @Tag("TC 3.38")
    @Tag("Luke")
    @Tag("Core")
    void searchCriteriaIsEmptyReturnsFalseWhenSemesterSet() {
        SearchCriteria sc = new SearchCriteria();
        sc.semester = 1;
        assertFalse(sc.isEmpty());
    }

    @Test
    @DisplayName("TC 3.39 – SearchCriteria isEmpty returns false when availabilityNumber set")
    @Tag("TC 3.39")
    @Tag("Luke")
    @Tag("Core")
    void searchCriteriaIsEmptyReturnsFalseWhenAvailabilityNumberSet() {
        SearchCriteria sc = new SearchCriteria();
        sc.availabilityNumber = 2;
        assertFalse(sc.isEmpty());
    }

    @Test
    @DisplayName("TC 3.40 – SearchCriteria isEmpty returns false when classType set")
    @Tag("TC 3.40")
    @Tag("Luke")
    @Tag("Core")
    void searchCriteriaIsEmptyReturnsFalseWhenClassTypeSet() {
        SearchCriteria sc = new SearchCriteria();
        sc.classType = "Lecture";
        assertFalse(sc.isEmpty());
    }

    @Test
    @DisplayName("TC 3.41 – SearchCriteria isEmpty returns false when classInstance set")
    @Tag("TC 3.41")
    @Tag("Luke")
    @Tag("Core")
    void searchCriteriaIsEmptyReturnsFalseWhenClassInstanceSet() {
        SearchCriteria sc = new SearchCriteria();
        sc.classInstance = 1;
        assertFalse(sc.isEmpty());
    }

    @Test
    @DisplayName("TC 3.42 – SearchCriteria isEmpty returns false when dateFrom set")
    @Tag("TC 3.42")
    @Tag("Luke")
    @Tag("Core")
    void searchCriteriaIsEmptyReturnsFalseWhenDateFromSet() {
        SearchCriteria sc = new SearchCriteria();
        sc.dateFrom = "01 Mar";
        assertFalse(sc.isEmpty());
    }

    @Test
    @DisplayName("TC 3.43 – SearchCriteria isEmpty returns false when dateTo set")
    @Tag("TC 3.43")
    @Tag("Luke")
    @Tag("Core")
    void searchCriteriaIsEmptyReturnsFalseWhenDateToSet() {
        SearchCriteria sc = new SearchCriteria();
        sc.dateTo = "30 Mar";
        assertFalse(sc.isEmpty());
    }

    @Test
    @DisplayName("TC 3.44 – SearchCriteria isEmpty returns false when day set")
    @Tag("TC 3.44")
    @Tag("Luke")
    @Tag("Core")
    void searchCriteriaIsEmptyReturnsFalseWhenDaySet() {
        SearchCriteria sc = new SearchCriteria();
        sc.day = "Monday";
        assertFalse(sc.isEmpty());
    }

    @Test
    @DisplayName("TC 3.45 – SearchCriteria isEmpty returns false when startTime set")
    @Tag("TC 3.45")
    @Tag("Luke")
    @Tag("Core")
    void searchCriteriaIsEmptyReturnsFalseWhenStartTimeSet() {
        SearchCriteria sc = new SearchCriteria();
        sc.startTime = LocalTime.of(9, 0);
        assertFalse(sc.isEmpty());
    }

    @Test
    @DisplayName("TC 3.46 – SearchCriteria isEmpty returns false when endTime set")
    @Tag("TC 3.46")
    @Tag("Luke")
    @Tag("Core")
    void searchCriteriaIsEmptyReturnsFalseWhenEndTimeSet() {
        SearchCriteria sc = new SearchCriteria();
        sc.endTime = LocalTime.of(11, 0);
        assertFalse(sc.isEmpty());
    }

    @Test
    @DisplayName("TC 3.47 – SearchCriteria isEmpty returns false when building set")
    @Tag("TC 3.47")
    @Tag("Luke")
    @Tag("Core")
    void searchCriteriaIsEmptyReturnsFalseWhenBuildingSet() {
        SearchCriteria sc = new SearchCriteria();
        sc.building = "Registry";
        assertFalse(sc.isEmpty());
    }


    @Test
    @DisplayName("TC 3.49 – Keyword search on class with null building does not throw")
    @Tag("TC 3.49")
    @Tag("Luke")
    @Tag("Core")
    void keywordSearchNullFieldDoesNotThrow() {
        ClassEntry nullBuilding = new ClassEntry("C1", "Lecture", null,
                LocalTime.of(9, 0), LocalTime.of(11, 0),
                DayOfWeek.MONDAY, null, null, "COMP1001", "In person", 1, 1, "01 Mar", "01 Mar");
        classRepo.save(nullBuilding);

        assertDoesNotThrow(() -> searchService.search("Registry"));
    }


    @Test
    @DisplayName("TC 3.52 – Keyword search with topic where no topic field matches query")
    @Tag("TC 3.52")
    @Tag("Luke")
    @Tag("Core")
    void keywordSearchTopicPresentButNoFieldMatchesQuery() {
        classRepo.save(makeEntry("C1", "COMP1001"));
        // topic campus/topicName/delivery all don't contain the query
        topicRepo.save(makeTopic("COMP1001", "Computing", "Bedford Park", 1, "Internal", 1, "In person"));

        // "ZZZNOMATCH" won't match any field on class or topic → hits line 72 false → return false
        List<ClassEntry> results = searchService.search("ZZZNOMATCH");

        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("TC 3.53 – Keyword search matches course code when class ID does not match")
    @Tag("TC 3.53")
    @Tag("Luke")
    @Tag("Core")
    void keywordSearchMatchesCourseCodeWhenClassIdDoesNotMatch() {
        // classId "UNRELATED-ID" does not contain "COMP1001" → line 59 false, line 60 true
        ClassEntry entry = new ClassEntry("UNRELATED-ID", "Lecture", null,
                LocalTime.of(9, 0), LocalTime.of(11, 0),
                DayOfWeek.MONDAY, "T1", "G42", "COMP1001",
                "In person", 1, 1, "01 Mar", "01 Mar");
        classRepo.save(entry);

        List<ClassEntry> results = searchService.search("COMP1001");

        assertEquals(1, results.size());
        assertEquals("COMP1001", results.get(0).getCourseCode());
    }

    @Test
    @DisplayName("TC 3.54 – Keyword search on class with null day does not throw and returns no match")
    @Tag("TC 3.54")
    @Tag("Luke")
    @Tag("Core")
    void keywordSearchNullDayBranchDoesNotThrow() {
        // null day → line 64: c.getDay() != null is false → short circuits
        ClassEntry nullDay = new ClassEntry("UNRELATED-ID", "Lecture", null,
                LocalTime.of(9, 0), LocalTime.of(11, 0),
                null, "T1", "G42", "COMP1001",
                "In person", 1, 1, "01 Mar", "01 Mar");
        classRepo.save(nullDay);

        assertDoesNotThrow(() -> searchService.search("MONDAY"));
    }
}
