package au.edu.flinders.timetable.service;

import au.edu.flinders.timetable.model.ClassEntry;
import au.edu.flinders.timetable.repository.ClassRepository;
import au.edu.flinders.timetable.repository.TopicRepository;
import au.edu.flinders.timetable.model.Topic;
import org.junit.jupiter.api.*;


import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)

class ClassServiceTest {

    //Fields declared at class level, shared across all tests
        private ClassRepository classRepo;
        private TopicRepository topicRepo;
        private ClassService classService;

        //runs before each tests, giving clean empty repository
        @BeforeEach
        void setUp() {
            classRepo = new ClassRepository();
            topicRepo = new TopicRepository();
            classService = new ClassService(classRepo, topicRepo);
        }


        //Convenience
        private ClassEntry makeEntry(String classId, String courseCode, String type,
                                     int instance, DayOfWeek day, LocalTime start,
                                     LocalTime end, String dateFrom, String dateTo) {
            return new ClassEntry(classId, type, null, start, end, day,
                    "T1", "G42", courseCode, "In Person", 1, instance,
                    dateFrom, dateTo);
        }


    //TC 2.01
    @Test
    @Order(1)
    @DisplayName("TC 2.01 - Save Class stores by Class ID")
    @Tag("Thomas")
    @Tag("Critical")
    void save_storesClassByClassId() {
        ClassEntry entry = new ClassEntry(
                "COMP1001-LEC-1-MON-0900-01Mar", "Lecture", null,
                LocalTime.of(9, 0), LocalTime.of(11, 0),
                DayOfWeek.MONDAY, "Registry", "R101", "COMP1001",
                "In person", 1, 1, "01 Mar", "01 Mar");

        classRepo.save(entry);

        assertTrue(classRepo.exists("COMP1001-LEC-1-MON-0900-01Mar"));
    }

    //TC 2.02
    @Test
    @Order(2)
    @DisplayName("TC 2.02 - Find class by ID returns the stored class")
    @Tag("Thomas")
    @Tag("Critical")
    void findClassbyID() {
        ClassEntry entry = makeEntry(
                "COMP1001-LEC-1-MON-0900-01Mar", "COMP1001", "Lecture",
                1, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0),
                "01 Mar", "01 Mar");
        classRepo.save(entry);

        Optional<ClassEntry> result = classService.getClassById("COMP1001-LEC-1-MON-0900-01Mar");

        assertTrue(result.isPresent());
        assertEquals("COMP1001-LEC-1-MON-0900-01Mar", result.get().getClassId());
        assertEquals("COMP1001", result.get().getCourseCode());
    }


    //TC 2.03
    @Test
    @Order(3)
    @DisplayName("TC 2.03 - Find class by ID returns empty when class is empty")
    @Tag("Thomas")
    @Tag("Core")
    void emptyClasreturnsEmpty() {
            Optional<ClassEntry> result = classService.getClassById("");
            assertTrue(result.isEmpty());
    }

    //Tc 2.04
    @Test
    @Order(4)
    @DisplayName("TC 2.04 - Find classes by course code returns matching classes only")
    @Tag("Thomas")
    @Tag("Critical")
    void getClassesForTopic_returnsMatchingCourseOnly() {
        ClassEntry comp = makeEntry(
                "COMP1001-LEC-1-MON-0900-01Mar", "COMP1001", "Lecture",
                1, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0),
                "01 Mar", "01 Mar");
        ClassEntry math = makeEntry(
                "MATH1001-LEC-1-TUE-1000-01Mar", "MATH1001", "Lecture",
                1, DayOfWeek.TUESDAY, LocalTime.of(10, 0), LocalTime.of(12, 0),
                "01 Mar", "01 Mar");
        classRepo.save(comp);
        classRepo.save(math);

        List<ClassEntry> result = classService.getClassesForTopic("COMP1001");

        assertEquals(1, result.size());
        assertEquals("COMP1001", result.get(0).getCourseCode());
    }


    //TC 2.05
    @Test
    @Order(5)
    @DisplayName("TC 2.05 - Update class replaces exisitng class details")
    @Tag("Thomas")
    @Tag("Core")
    void updateClassReplacesOriginal() {
        ClassEntry original = makeEntry("COMP1001-LEC-1-MON-0900-01Mar",
                "COMP1001", "Lecture", 1, DayOfWeek.MONDAY,
                LocalTime.of(9,0), LocalTime.of(11,0),
                "01 Mar", "01 Mar");
        classRepo.save(original);

        //changing the time
        ClassEntry updated = new ClassEntry(
                original.getClassId(),
                original.getType(),
                original.getDate(),
                LocalTime.of(14, 0),
                LocalTime.of(16, 0),
                original.getDay(),
                original.getBuilding(),
                original.getRoom(),
                original.getCourseCode(),
                original.getAttendanceMode(),
                original.getAvailabilityNumber(),
                original.getClassInstance(),
                original.getDateFrom(),
                original.getDateTo());

        classService.updateClass(updated);

        ClassEntry stored = classService.getClassById("COMP1001-LEC-1-MON-0900-01Mar").get();
        assertEquals(LocalTime.of(14, 0), stored.getStartTime());
        assertEquals(LocalTime.of(16, 0), stored.getEndTime());
    }

    //TC 2.06
    @Test
    @Order(6)
    @DisplayName("TC 2.06 - Update missing class throws IllegalArgumentException")
    @Tag("Thomas")
    @Tag("Core")
    void updateClass_nonExistentClass_throwsIllegalArgumentException() {
        ClassEntry unknown = makeEntry(
                "Nothing-001", "COMP1000", "Lecture",
                1, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0),
                "01 Mar", "01 Mar");

        assertThrows(IllegalArgumentException.class,
                () -> classService.updateClass(unknown));
    }

    //TC 2.07
    @Test
    @Order(7)
    @DisplayName("TC 2.07 - Delete class removes it from the repository")
    @Tag("Thomas")
    @Tag("Critical")
    void deleteClass_removesExistingClass() {
        ClassEntry entry = makeEntry(
                "COMP1001-LEC-1-MON-0900-01Mar", "COMP1001", "Lecture",
                1, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0),
                "01 Mar", "01 Mar");
        classRepo.save(entry);

        classService.deleteClass("COMP1001-LEC-1-MON-0900-01Mar");

        assertTrue(classService.getClassById("COMP1001-LEC-1-MON-0900-01Mar").isEmpty());
    }

    //TC 2,08
    @Test
    @Order(8)
    @DisplayName("TC 2.08 - Delete missing class throws IllegalArgumentException")
    @Tag("Thomas")
    @Tag("Core")
    void deleteClass_nonExistentClass_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> classService.deleteClass("DOES-NOT-EXIST"));
    }

    @Test
    @Order(9)
    @DisplayName("TC 2.09 - Grouped Classes combine related date ranges")
    @Tag("Thomas")
    @Tag("Critical")
    void getGroupedClasses_sameGroup_mergesDateRange() {
        ClassEntry early = makeEntry(
                "COMP1000-Lab-1-WED-1400-05Mar", "COMP1000", "Laboratory",
                1, DayOfWeek.WEDNESDAY, LocalTime.of(14, 0), LocalTime.of(16, 0),
                "05 Mar", "05 Mar");
        ClassEntry late = makeEntry(
                "COMP1000-Lab-1-WED-1400-19Mar", "COMP1000", "Laboratory",
                1, DayOfWeek.WEDNESDAY, LocalTime.of(14, 0), LocalTime.of(16, 0),
                "19 Mar", "19 Mar");
        classRepo.save(early);
        classRepo.save(late);

        List<ClassEntry> grouped = classService.getGroupedClasses();

        assertEquals(1, grouped.size());
        assertEquals("05 Mar", grouped.get(0).getDateFrom());
        assertEquals("19 Mar", grouped.get(0).getDateTo());
    }

    @Test
    @Order(10)
    @DisplayName("TC 2.10 - Grouped classes keep seperate class instances apart")
    @Tag("Thomas")
    @Tag("Core")
    void getGroupedClasses_differentInstances_remainSeparate() {
        ClassEntry inst1 = makeEntry(
                "COMP1000-Lab-1-WED-1400-05Mar", "COMP1000", "Laboratory",
                1, DayOfWeek.WEDNESDAY, LocalTime.of(14, 0), LocalTime.of(16, 0),
                "05 Mar", "05 Mar");
        ClassEntry inst2 = makeEntry(
                "COMP1000-Lab-2-FRI-1000-05Mar", "COMP1000", "Laboratory",
                2, DayOfWeek.FRIDAY, LocalTime.of(10, 0), LocalTime.of(12, 0),
                "05 Mar", "05 Mar");
        classRepo.save(inst1);
        classRepo.save(inst2);

        List<ClassEntry> grouped = classService.getGroupedClasses();

        assertEquals(2, grouped.size());
    }


    @Test
    @Order(11)
    @DisplayName("TC 2.11 - getAllClasses returns all stored classes")
    @Tag("Luke")
    @Tag("Core")
    void getAllClassesReturnsAllStoredClasses() {
        ClassEntry a = makeEntry("COMP1001-LEC-1-MON-0900-01Mar", "COMP1001", "Lecture",
                1, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0), "01 Mar", "01 Mar");
        ClassEntry b = makeEntry("MATH1001-LEC-1-TUE-1000-01Mar", "MATH1001", "Lecture",
                1, DayOfWeek.TUESDAY, LocalTime.of(10, 0), LocalTime.of(12, 0), "01 Mar", "01 Mar");
        classRepo.save(a);
        classRepo.save(b);

        List<ClassEntry> result = classService.getAllClasses();

        assertEquals(2, result.size());
    }

    @Test
    @Order(12)
    @DisplayName("TC 2.12 - getClassesByCampus returns classes whose topic campus matches")
    @Tag("Luke")
    @Tag("Core")
    void getClassesByCampusReturnsCampusMatchingClasses() {
        ClassEntry entry = makeEntry("COMP1001-LEC-1-MON-0900-01Mar", "COMP1001", "Lecture",
                1, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0), "01 Mar", "01 Mar");
        classRepo.save(entry);
        topicRepo.save(new Topic("COMP1001", "Computing 1", "Bedford Park", 1, "In Person", 2));

        List<ClassEntry> result = classService.getClassesByCampus("Bedford Park");

        assertEquals(1, result.size());
        assertEquals("COMP1001", result.get(0).getCourseCode());
    }

    @Test
    @Order(13)
    @DisplayName("TC 2.13 - getClassesByCampus returns empty when topic campus does not match")
    @Tag("Luke")
    @Tag("Core")
    void getClassesByCampusReturnsEmptyWhenCampusDoesNotMatch() {
        ClassEntry entry = makeEntry("COMP1001-LEC-1-MON-0900-01Mar", "COMP1001", "Lecture",
                1, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0), "01 Mar", "01 Mar");
        classRepo.save(entry);
        topicRepo.save(new Topic("COMP1001", "Computing 1", "Bedford Park", 1, "In Person", 2));

        List<ClassEntry> result = classService.getClassesByCampus("Tonsley");

        assertTrue(result.isEmpty());
    }

    @Test
    @Order(14)
    @DisplayName("TC 2.14 - getClassesByCampus returns empty when class has no matching topic")
    @Tag("Luke")
    @Tag("Core")
    void getClassesByCampusReturnsEmptyWhenNoTopicExistsForClass() {
        ClassEntry entry = makeEntry("COMP9999-LEC-1-MON-0900-01Mar", "COMP9999", "Lecture",
                1, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0), "01 Mar", "01 Mar");
        classRepo.save(entry);

        List<ClassEntry> result = classService.getClassesByCampus("Bedford Park");

        assertTrue(result.isEmpty());
    }

    @Test
    @Order(15)
    @DisplayName("TC 2.15 - getGroupedClassesForTopic returns only entries matching course code")
    @Tag("Luke")
    @Tag("Core")
    void getGroupedClassesForTopicReturnsMatchingCourseCodeOnly() {
        ClassEntry comp = makeEntry("COMP1001-LEC-1-MON-0900-01Mar", "COMP1001", "Lecture",
                1, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0), "01 Mar", "01 Mar");
        ClassEntry math = makeEntry("MATH1001-LEC-1-TUE-1000-01Mar", "MATH1001", "Lecture",
                1, DayOfWeek.TUESDAY, LocalTime.of(10, 0), LocalTime.of(12, 0), "01 Mar", "01 Mar");
        classRepo.save(comp);
        classRepo.save(math);

        List<ClassEntry> result = classService.getGroupedClassesForTopic("COMP1001");

        assertEquals(1, result.size());
        assertEquals("COMP1001", result.get(0).getCourseCode());
    }

    @Test
    @Order(16)
    @DisplayName("TC 2.16 - getGroupedClasses handles invalid date string without throwing")
    @Tag("Luke")
    @Tag("Core")
    void getGroupedClassesInvalidDateStringDoesNotThrow() {
        ClassEntry invalid = makeEntry("COMP1001-LEC-1-MON-0900-BAD", "COMP1001", "Lecture",
                1, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0), "INVALID", "INVALID");
        ClassEntry valid = makeEntry("COMP1001-LEC-1-MON-0900-01Mar", "COMP1001", "Lecture",
                1, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0), "01 Mar", "01 Mar");
        classRepo.save(invalid);
        classRepo.save(valid);

        assertDoesNotThrow(() -> classService.getGroupedClasses());
    }
}
