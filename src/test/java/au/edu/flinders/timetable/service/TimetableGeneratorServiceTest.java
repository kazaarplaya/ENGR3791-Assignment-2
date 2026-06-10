package au.edu.flinders.timetable.service;

import au.edu.flinders.timetable.model.ClassEntry;
import au.edu.flinders.timetable.model.Preference;
import au.edu.flinders.timetable.model.Timetable;
import au.edu.flinders.timetable.model.Topic;
import au.edu.flinders.timetable.model.User;
import au.edu.flinders.timetable.repository.ClassRepository;
import au.edu.flinders.timetable.repository.PreferenceRepository;
import au.edu.flinders.timetable.repository.TimetableRepository;
import au.edu.flinders.timetable.repository.TopicRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TimetableGeneratorServiceTest {

    private ClassRepository classRepository;
    private TopicRepository topicRepository;
    private PreferenceRepository preferenceRepository;
    private TimetableRepository timetableRepository;
    private TimetableService timetableService;
    private TimetableGeneratorService generatorService;

    @BeforeAll
    static void suiteSetUp() {}

    @BeforeEach
    void setUp() {
        classRepository      = new ClassRepository();
        topicRepository      = new TopicRepository();
        preferenceRepository = new PreferenceRepository();
        timetableRepository  = new TimetableRepository();
        timetableService     = new TimetableService(timetableRepository);
        generatorService     = new TimetableGeneratorService(
                classRepository, topicRepository, preferenceRepository, timetableService);
    }

    @AfterEach
    void tearDown() {}

    @AfterAll
    static void suiteTearDown() {}

    // Helper function to create a user
    private User buildUser(String... courseCodes) {
        User user = new User("student-001", "Test Student");
        for (String code : courseCodes) user.enrol(code);
        return user;
    }

    // Creates a ClassEntry with the given values
    private ClassEntry buildClass(String classId, String courseCode, String type,
                                  DayOfWeek day, LocalTime start, LocalTime end,
                                  String building) {
        return new ClassEntry(classId, type, null, start, end, day,
                building, "R101", courseCode, "In person", 1, 1,
                "01 Mar", "01 Jun");
    }

    // Creates a Topic with the given values
    private Topic buildTopic(String courseCode, String campus, int semester) {
        return new Topic(courseCode, courseCode + " Topic", campus, semester, "In Person", 1);
    }


    @Test
    @Order(1)
    @DisplayName("TC 6.01 – Generate returns empty timetable for user with no enrolled topics")
    @Tag("Luke")
    @Tag("Critical")
    void generateReturnsEmptyTimetableForUserWithNoEnrolledTopics() {
        User user = new User("student-001", "Test Student");

        Timetable result = generatorService.generate(user, Map.of(), false, false, "TC601");

        assertTrue(result.isEmpty(), "Timetable should contain no classes");
    }


    @Test
    @Order(2)
    @DisplayName("TC 6.02 – Generate single topic timetable adds non-clashing class")
    @Tag("Luke")
    @Tag("Critical")
    void generateSingleTopicTimetableAddsNonClashingClass() {
        ClassEntry lecture = buildClass("COMP1000-LEC-MON-0900", "COMP1000", "Lecture",
                DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0), "Registry");
        classRepository.save(lecture);

        User user = buildUser("COMP1000");

        Timetable result = generatorService.generate(user, Map.of(), false, false, "TC602");

        assertAll(
                () -> assertFalse(result.isEmpty(), "Timetable should contain at least one class"),
                () -> assertTrue(result.getClassIds().contains("COMP1000-LEC-MON-0900"))
        );
    }


    @Test
    @Order(3)
    @DisplayName("TC 6.03 – Generate throws exception when no classes exist for enrolled topic")
    @Tag("Luke")
    @Tag("Critical")
    void generateThrowsExceptionWhenNoClassesExistForEnrolledTopic() {
        User user = buildUser("COMP9999");

        assertThrows(IllegalStateException.class,
                () -> generatorService.generate(user, Map.of(), false, false, "TC603"));
    }


    @Test
    @Order(4)
    @DisplayName("TC 6.04 – Generate rejects city campus mixed with Bedford Park/Tonsley for same topic")
    @Tag("Luke")
    @Tag("Critical")
    void generateRejectsCityCampusMixedWithBedfordParkForSameTopic() {
        // Only a non-city class exists; selecting City campus should yield no candidates.
        ClassEntry bedfordClass = buildClass("COMP1000-LEC-MON-0900", "COMP1000", "Lecture",
                DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0), "Registry Building");
        classRepository.save(bedfordClass);

        User user = buildUser("COMP1000");

        assertThrows(IllegalStateException.class,
                () -> generatorService.generate(user, Map.of("COMP1000", "City"),
                        false, false, "TC604"));
    }


    @Test
    @Order(5)
    @DisplayName("TC 6.05 – Generate enforces 30-minute gap between different campus classes")
    @Tag("Luke")
    @Tag("Critical")
    void generateEnforces30MinuteGapBetweenDifferentCampusClasses() {
        // City class ends at 10:00; Bedford Park class starts at 10:15 — only 15-min gap.
        ClassEntry cityClass = buildClass("COMP1000-LEC-MON-0900", "COMP1000", "Lecture",
                DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0), "Festival Tower");
        ClassEntry bedfordClass = buildClass("COMP2000-LEC-MON-1015", "COMP2000", "Lecture",
                DayOfWeek.MONDAY, LocalTime.of(10, 15), LocalTime.of(11, 15), "Registry Building");
        classRepository.save(cityClass);
        classRepository.save(bedfordClass);

        User user = buildUser("COMP1000", "COMP2000");

        Timetable result = generatorService.generate(user, Map.of(), false, false, "TC605");

        // The second class should be excluded due to insufficient commute gap.
        assertFalse(result.getClassIds().contains("COMP2000-LEC-MON-1015"),
                "Class with insufficient commute gap should be excluded");
    }


    @Test
    @Order(6)
    @DisplayName("TC 6.06 – Generate allows back-to-back classes at same campus")
    @Tag("Luke")
    @Tag("Critical")
    void generateAllowsBackToBackClassesAtSameCampus() {
        // Both at Bedford Park; first ends at 10:00, second starts at 10:00 — no gap needed.
        ClassEntry first = buildClass("COMP1000-LEC-MON-0900", "COMP1000", "Lecture",
                DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0), "Registry Building");
        ClassEntry second = buildClass("COMP2000-LEC-MON-1000", "COMP2000", "Lecture",
                DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(11, 0), "Registry Building");
        classRepository.save(first);
        classRepository.save(second);

        User user = buildUser("COMP1000", "COMP2000");

        Timetable result = generatorService.generate(user, Map.of(), false, false, "TC606");

        assertAll(
                () -> assertTrue(result.getClassIds().contains("COMP1000-LEC-MON-0900")),
                () -> assertTrue(result.getClassIds().contains("COMP2000-LEC-MON-1000"),
                        "Back-to-back same-campus class should be included")
        );
    }


    @Test
    @Order(7)
    @DisplayName("TC 6.07 – Generate with lecture overlap disabled excludes clashing lecture")
    @Tag("Luke")
    @Tag("Critical")
    void generateWithLectureOverlapDisabledExcludesClashingLecture() {
        // Two lectures on the same day with overlapping times at different campuses.
        ClassEntry tonsleyLec = buildClass("COMP1000-LEC-MON-0900", "COMP1000", "Lecture",
                DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0), "Tonsley T1");
        ClassEntry bedfordLec = buildClass("COMP2000-LEC-MON-0930", "COMP2000", "Lecture",
                DayOfWeek.MONDAY, LocalTime.of(9, 30), LocalTime.of(11, 30), "Registry Building");
        classRepository.save(tonsleyLec);
        classRepository.save(bedfordLec);

        User user = buildUser("COMP1000", "COMP2000");

        Timetable result = generatorService.generate(user, Map.of(), false, false, "TC607");

        assertFalse(result.getClassIds().contains("COMP2000-LEC-MON-0930"),
                "Clashing lecture should be excluded when overlap is disabled");
    }


    @Test
    @Order(8)
    @DisplayName("TC 6.08 – Generate filters candidates by semester one")
    @Tag("Luke")
    @Tag("Core")
    void generateFiltersCandidatesBySemesterOne() {
        ClassEntry sem1Class = buildClass("COMP1000-LEC-MON-0900", "COMP1000", "Lecture",
                DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0), "Registry Building");
        classRepository.save(sem1Class);
        topicRepository.save(buildTopic("COMP1000", "Bedford Park", 1));

        User user = buildUser("COMP1000");

        Timetable result = generatorService.generate(user, Map.of(), false, false, "TC608", 1);

        assertTrue(result.getClassIds().contains("COMP1000-LEC-MON-0900"),
                "Semester 1 class should be included when filtering by semester 1");
    }


    @Test
    @Order(9)
    @DisplayName("TC 6.09 – Generate filters candidates by semester two")
    @Tag("Luke")
    @Tag("Core")
    void generateFiltersCandidatesBySemesterTwo() {
        ClassEntry sem1Class = buildClass("COMP1000-LEC-MON-0900", "COMP1000", "Lecture",
                DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0), "Registry Building");
        classRepository.save(sem1Class);
        topicRepository.save(buildTopic("COMP1000", "Bedford Park", 1));

        User user = buildUser("COMP1000");

        // Filtering by semester 2 should exclude the semester 1 class, leaving no candidates.
        assertThrows(IllegalStateException.class,
                () -> generatorService.generate(user, Map.of(), false, false, "TC609", 2));
    }


    @Test
    @Order(10)
    @DisplayName("TC 6.10 – Generate applies morning preference")
    @Tag("Luke")
    @Tag("Core")
    void generateAppliesMorningPreference() {
        ClassEntry morningClass = buildClass("COMP1000-LEC-MON-0900", "COMP1000", "Lecture",
                DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0), "Registry Building");
        ClassEntry afternoonClass = buildClass("COMP1000-LEC-TUE-1400", "COMP1000", "Lecture",
                DayOfWeek.TUESDAY, LocalTime.of(14, 0), LocalTime.of(16, 0), "Registry Building");
        classRepository.save(morningClass);
        classRepository.save(afternoonClass);

        Preference pref = new Preference("student-001", List.of(Preference.TIME_MORNING));
        preferenceRepository.save(pref);

        User user = buildUser("COMP1000");

        Timetable result = generatorService.generate(user, Map.of(), false, true, "TC610");

        assumeTrue(!result.isEmpty(), "Timetable must have at least one class");
        assertTrue(result.getClassIds().contains("COMP1000-LEC-MON-0900"),
                "Morning preference should favour the 09:00 class");
    }


    @Test
    @Order(11)
    @DisplayName("TC 6.11 – Generate applies compact preference")
    @Tag("Luke")
    @Tag("Core")
    void generateAppliesCompactPreference() {
        // Two classes on the same day — compact should not exclude either (COMPACT is an
        // ordering hint only; the test verifies both are still included with no exception).
        ClassEntry first = buildClass("COMP1000-LEC-MON-0900", "COMP1000", "Lecture",
                DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0), "Registry Building");
        ClassEntry second = buildClass("COMP2000-LEC-MON-1100", "COMP2000", "Lecture",
                DayOfWeek.MONDAY, LocalTime.of(11, 0), LocalTime.of(12, 0), "Registry Building");
        classRepository.save(first);
        classRepository.save(second);

        Preference pref = new Preference("student-001", List.of(Preference.COMPACT));
        preferenceRepository.save(pref);

        User user = buildUser("COMP1000", "COMP2000");

        Timetable result = generatorService.generate(user, Map.of(), false, true, "TC611");

        assertAll(
                () -> assertTrue(result.getClassIds().contains("COMP1000-LEC-MON-0900")),
                () -> assertTrue(result.getClassIds().contains("COMP2000-LEC-MON-1100"))
        );
    }


    @Test
    @Order(12)
    @DisplayName("TC 6.12 – Generate applies campus preference")
    @Tag("Luke")
    @Tag("Core")
    void generateAppliesCampusPreference() {
        ClassEntry bedfordClass = buildClass("COMP1000-LEC-MON-0900", "COMP1000", "Lecture",
                DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0), "Registry Building");
        ClassEntry tonsleyClass = buildClass("COMP1000-LEC-TUE-0900", "COMP1000", "Lecture",
                DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(11, 0), "Tonsley T1");
        classRepository.save(bedfordClass);
        classRepository.save(tonsleyClass);

        Preference pref = new Preference("student-001", List.of(Preference.CAMPUS_BEDFORD_PARK));
        preferenceRepository.save(pref);

        User user = buildUser("COMP1000");

        Timetable result = generatorService.generate(user, Map.of(), false, true, "TC612");

        assumeTrue(!result.isEmpty());
        assertTrue(result.getClassIds().contains("COMP1000-LEC-MON-0900"),
                "Bedford Park preference should favour the Bedford Park class");
    }


    @Test
    @Order(13)
    @DisplayName("TC 6.13 – Generate produces clash-free timetable across multiple topics")
    @Tag("Luke")
    @Tag("Core")
    void generateProducesClashFreeTimetableAcrossMultipleTopics() {
        ClassEntry comp = buildClass("COMP1000-LEC-MON-0900", "COMP1000", "Lecture",
                DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0), "Registry Building");
        ClassEntry math = buildClass("MATH1000-LEC-MON-1100", "MATH1000", "Lecture",
                DayOfWeek.MONDAY, LocalTime.of(11, 0), LocalTime.of(13, 0), "Registry Building");
        classRepository.save(comp);
        classRepository.save(math);

        User user = buildUser("COMP1000", "MATH1000");

        Timetable result = generatorService.generate(user, Map.of(), false, false, "TC613");

        assertAll(
                () -> assertTrue(result.getClassIds().contains("COMP1000-LEC-MON-0900")),
                () -> assertTrue(result.getClassIds().contains("MATH1000-LEC-MON-1100"),
                        "Non-clashing classes across multiple topics should both be included")
        );
    }


    @Test
    @Order(14)
    @DisplayName("TC 6.14 – Generate uses auto-generated name when optionalName is null")
    @Tag("Luke")
    @Tag("Core")
    void generateUsesAutoGeneratedNameWhenOptionalNameIsNull() {
        User user = new User("student-001", "Test Student");

        Timetable result = generatorService.generate(user, Map.of(), false, false, null);

        assertNotNull(result.getTimetableName(), "Auto-generated name should not be null");
        assertFalse(result.getTimetableName().isBlank(), "Auto-generated name should not be blank");
    }


    @Test
    @Order(15)
    @DisplayName("TC 6.15 – Generate with applyPreferences true but no saved preference still generates")
    @Tag("Luke")
    @Tag("Core")
    void generateApplyPreferencesTrueButNoSavedPreferenceStillGenerates() {
        ClassEntry lecture = buildClass("COMP1000-LEC-MON-0900", "COMP1000", "Lecture",
                DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0), "Registry Building");
        classRepository.save(lecture);

        User user = buildUser("COMP1000");

        Timetable result = generatorService.generate(user, Map.of(), false, true, "TC615");

        assertTrue(result.getClassIds().contains("COMP1000-LEC-MON-0900"));
    }


    @Test
    @Order(16)
    @DisplayName("TC 6.16 – Generate throws with campus name in message when campus selected and no candidates")
    @Tag("Luke")
    @Tag("Core")
    void generateThrowsWithCampusNameInMessageWhenNoCandidates() {
        ClassEntry bedfordClass = buildClass("COMP1000-LEC-MON-0900", "COMP1000", "Lecture",
                DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0), "Registry Building");
        classRepository.save(bedfordClass);

        User user = buildUser("COMP1000");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> generatorService.generate(user, Map.of("COMP1000", "City"),
                        false, false, "TC616"));

        assertTrue(ex.getMessage().contains("City"));
    }


    @Test
    @Order(17)
    @DisplayName("TC 6.17 – Generate from selections saves explicitly chosen classes")
    @Tag("Luke")
    @Tag("Core")
    void generateFromSelectionsExplicitClassesAreSaved() {
        ClassEntry lecture = buildClass("COMP1000-LEC-MON-0900", "COMP1000", "Lecture",
                DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0), "Registry Building");
        classRepository.save(lecture);

        User user = buildUser("COMP1000");

        Timetable result = generatorService.generateFromSelections(
                user, List.of(lecture), false, false, "TC617", 0);

        assertTrue(result.getClassIds().contains("COMP1000-LEC-MON-0900"));
    }

    @Test
    @Order(18)
    @DisplayName("TC 6.18 – Generate from selections includes clashing class with warning")
    @Tag("Luke")
    @Tag("Core")
    void generateFromSelectionsIncludesClashingClassWithWarning() {
        ClassEntry first = buildClass("COMP1000-LEC-MON-0900", "COMP1000", "Lecture",
                DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0), "Registry Building");
        ClassEntry second = buildClass("COMP2000-LEC-MON-1000", "COMP2000", "Lecture",
                DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(12, 0), "Registry Building");

        User user = buildUser("COMP1000", "COMP2000");

        Timetable result = generatorService.generateFromSelections(
                user, List.of(first, second), false, false, "TC618", 0);

        assertAll(
                () -> assertTrue(result.getClassIds().contains("COMP1000-LEC-MON-0900")),
                () -> assertTrue(result.getClassIds().contains("COMP2000-LEC-MON-1000"),
                        "Clashing class should still be included in generateFromSelections")
        );
    }

    @Test
    @Order(19)
    @DisplayName("TC 6.19 – Generate from selections uses auto-generated name when null")
    @Tag("Luke")
    @Tag("Core")
    void generateFromSelectionsUsesAutoGeneratedNameWhenNull() {
        User user = new User("student-001", "Test Student");

        Timetable result = generatorService.generateFromSelections(
                user, List.of(), false, false, null, 0);

        assertNotNull(result.getTimetableName());
    }


    @Test
    @Order(20)
    @DisplayName("TC 6.20 – Generate accepts Flinders City Campus as city campus selector")
    @Tag("Luke")
    @Tag("Core")
    void generateAcceptsFlindersCtyCampusAsSelector() {
        ClassEntry cityClass = buildClass("COMP1000-LEC-MON-0900", "COMP1000", "Lecture",
                DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0), "Festival Tower");
        classRepository.save(cityClass);

        User user = buildUser("COMP1000");

        Timetable result = generatorService.generate(user,
                Map.of("COMP1000", "Flinders City Campus"), false, false, "TC620");

        assertTrue(result.getClassIds().contains("COMP1000-LEC-MON-0900"));
    }


    @Test
    @Order(21)
    @DisplayName("TC 6.21 – Generate falls back to topic repository campus when building inference yields nothing")
    @Tag("Luke")
    @Tag("Core")
    void generateFallsBackToTopicRepositoryCampusWhenBuildingInferenceYieldsNothing() {
        ClassEntry unknownBuilding = buildClass("COMP1000-LEC-MON-0900", "COMP1000", "Lecture",
                DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0), "Unknown Building");
        classRepository.save(unknownBuilding);
        topicRepository.save(buildTopic("COMP1000", "Tonsley", 1));

        User user = buildUser("COMP1000");

        Timetable result = generatorService.generate(user,
                Map.of("COMP1000", "Tonsley"), false, false, "TC621");

        assertTrue(result.getClassIds().contains("COMP1000-LEC-MON-0900"));
    }

    @Test
    @Order(22)
    @DisplayName("TC 6.22 – Generate throws when campus fallback also yields no candidates")
    @Tag("Luke")
    @Tag("Core")
    void generateThrowsWhenCampusFallbackAlsoYieldsNoCandidates() {
        ClassEntry unknownBuilding = buildClass("COMP1000-LEC-MON-0900", "COMP1000", "Lecture",
                DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0), "Unknown Building");
        classRepository.save(unknownBuilding);
        topicRepository.save(buildTopic("COMP1000", "Bedford Park", 1));

        User user = buildUser("COMP1000");

        assertThrows(IllegalStateException.class,
                () -> generatorService.generate(user,
                        Map.of("COMP1000", "Tonsley"), false, false, "TC622"));
    }


    @Test
    @Order(23)
    @DisplayName("TC 6.23 – Generate applies Tonsley campus preference")
    @Tag("Luke")
    @Tag("Core")
    void generateAppliesTonsleyCampusPreference() {
        ClassEntry bedfordClass = buildClass("COMP1000-LEC-MON-0900", "COMP1000", "Lecture",
                DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0), "Registry Building");
        ClassEntry tonsleyClass = buildClass("COMP1000-LEC-TUE-0900", "COMP1000", "Lecture",
                DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(11, 0), "Tonsley T1");
        classRepository.save(bedfordClass);
        classRepository.save(tonsleyClass);

        Preference pref = new Preference("student-001", List.of(Preference.CAMPUS_TONSLEY));
        preferenceRepository.save(pref);

        User user = buildUser("COMP1000");

        Timetable result = generatorService.generate(user, Map.of(), false, true, "TC623");

        assertTrue(result.getClassIds().contains("COMP1000-LEC-TUE-0900"),
                "Tonsley preference should favour the Tonsley class");
    }

    @Test
    @Order(24)
    @DisplayName("TC 6.24 – Generate applies City campus preference")
    @Tag("Luke")
    @Tag("Core")
    void generateAppliesCityCampusPreference() {
        ClassEntry bedfordClass = buildClass("COMP1000-LEC-MON-0900", "COMP1000", "Lecture",
                DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0), "Registry Building");
        ClassEntry cityClass = buildClass("COMP1000-LEC-TUE-0900", "COMP1000", "Lecture",
                DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(11, 0), "Festival Tower");
        classRepository.save(bedfordClass);
        classRepository.save(cityClass);

        Preference pref = new Preference("student-001", List.of(Preference.CAMPUS_CITY));
        preferenceRepository.save(pref);

        User user = buildUser("COMP1000");

        Timetable result = generatorService.generate(user, Map.of(), false, true, "TC624");

        assertTrue(result.getClassIds().contains("COMP1000-LEC-TUE-0900"),
                "City preference should favour the Festival Tower class");
    }


    @ParameterizedTest(name = "preference token ''{0}'' should favour ''{1}'' class")
    @Order(25)
    @DisplayName("TC 6.25 – Generate applies day preference for all weekdays")
    @Tag("Luke")
    @Tag("Core")
    @CsvSource({
            "DAY_TUESDAY,  COMP1000-LEC-TUE-0900, TUESDAY",
            "DAY_WEDNESDAY, COMP1000-LEC-WED-0900, WEDNESDAY",
            "DAY_THURSDAY, COMP1000-LEC-THU-0900, THURSDAY",
            "DAY_FRIDAY,   COMP1000-LEC-FRI-0900, FRIDAY"
    })
    void generateAppliesDayPreferenceForAllWeekdays(String token, String expectedClassId, String day) {
        ClassEntry mondayClass = buildClass("COMP1000-LEC-MON-0900", "COMP1000", "Lecture",
                DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0), "Registry Building");
        ClassEntry targetClass = buildClass(expectedClassId, "COMP1000", "Lecture",
                DayOfWeek.valueOf(day), LocalTime.of(9, 0), LocalTime.of(11, 0), "Registry Building");
        classRepository.save(mondayClass);
        classRepository.save(targetClass);

        Preference pref = new Preference("student-001", List.of(token));
        preferenceRepository.save(pref);

        User user = buildUser("COMP1000");

        Timetable result = generatorService.generate(user, Map.of(), false, true,
                "TC625-" + day);

        assertTrue(result.getClassIds().contains(expectedClassId),
                token + " preference should favour the " + day + " class");
    }


    @Test
    @Order(26)
    @DisplayName("TC 6.26 – Would clash returns false for non-overlapping classes")
    @Tag("Luke")
    @Tag("Core")
    void wouldClashReturnsFalseForNonOverlappingClasses() {
        ClassEntry first = buildClass("COMP1000-LEC-MON-0900", "COMP1000", "Lecture",
                DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0), "Registry Building");
        ClassEntry second = buildClass("COMP2000-LEC-MON-1100", "COMP2000", "Lecture",
                DayOfWeek.MONDAY, LocalTime.of(11, 0), LocalTime.of(12, 0), "Registry Building");

        assertFalse(generatorService.wouldClash(second, List.of(first), false));
    }

    @Test
    @Order(27)
    @DisplayName("TC 6.27 – Would clash returns true for overlapping classes")
    @Tag("Luke")
    @Tag("Core")
    void wouldClashReturnsTrueForOverlappingClasses() {
        ClassEntry first = buildClass("COMP1000-LEC-MON-0900", "COMP1000", "Lecture",
                DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0), "Registry Building");
        ClassEntry second = buildClass("COMP2000-LEC-MON-1000", "COMP2000", "Lecture",
                DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(12, 0), "Registry Building");

        assertTrue(generatorService.wouldClash(second, List.of(first), false));
    }

    @Test
    @Order(28)
    @DisplayName("TC 6.28 – Would clash returns false when classes are on different days")
    @Tag("Luke")
    @Tag("Core")
    void wouldClashReturnsFalseWhenClassesAreOnDifferentDays() {
        ClassEntry monday = buildClass("COMP1000-LEC-MON-0900", "COMP1000", "Lecture",
                DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0), "Registry Building");
        ClassEntry tuesday = buildClass("COMP2000-LEC-TUE-0900", "COMP2000", "Lecture",
                DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(11, 0), "Registry Building");

        assertFalse(generatorService.wouldClash(tuesday, List.of(monday), false));
    }


    @Test
    @Order(29)
    @DisplayName("TC 6.29 – Generate enforces 30-minute gap between Bedford Park and Tonsley non-lectures")
    @Tag("Luke")
    @Tag("Core")
    void generateEnforces30MinuteGapBetweenBedfordAndTonsleyNonLectures() {
        ClassEntry bedfordTut = buildClass("COMP1000-TUT-MON-0900", "COMP1000", "Tutorial",
                DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0), "Registry Building");
        ClassEntry tonsleyTut = buildClass("COMP2000-TUT-MON-1015", "COMP2000", "Tutorial",
                DayOfWeek.MONDAY, LocalTime.of(10, 15), LocalTime.of(11, 15), "Tonsley T1");
        classRepository.save(bedfordTut);
        classRepository.save(tonsleyTut);

        User user = buildUser("COMP1000", "COMP2000");

        Timetable result = generatorService.generate(user, Map.of(), false, false, "TC629");

        assertFalse(result.getClassIds().contains("COMP2000-TUT-MON-1015"),
                "Tonsley tutorial should be excluded — insufficient gap from Bedford Park");
    }

    @Test
    @Order(30)
    @DisplayName("TC 6.30 – Generate allows Bedford Park and Tonsley lectures when overlap enabled")
    @Tag("Luke")
    @Tag("Core")
    void generateAllowsBedfordAndTonsleyLecturesWhenOverlapEnabled() {
        ClassEntry bedfordLec = buildClass("COMP1000-LEC-MON-0900", "COMP1000", "Lecture",
                DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0), "Registry Building");
        ClassEntry tonsleyLec = buildClass("COMP2000-LEC-MON-0930", "COMP2000", "Lecture",
                DayOfWeek.MONDAY, LocalTime.of(9, 30), LocalTime.of(11, 30), "Tonsley T1");
        classRepository.save(bedfordLec);
        classRepository.save(tonsleyLec);

        User user = buildUser("COMP1000", "COMP2000");

        Timetable result = generatorService.generate(user, Map.of(), true, false, "TC630");

        assertAll(
                () -> assertTrue(result.getClassIds().contains("COMP1000-LEC-MON-0900")),
                () -> assertTrue(result.getClassIds().contains("COMP2000-LEC-MON-0930"),
                        "Overlapping lectures should be allowed when overlap is enabled")
        );
    }

    @Test
    @Order(31)
    @DisplayName("TC 6.31 – Generate excludes Tonsley lecture clashing with Bedford Park non-lecture when overlap disabled")
    @Tag("Luke")
    @Tag("Core")
    void generateExcludesTonsleyLectureClashingWithBedfordNonLectureWhenOverlapDisabled() {
        ClassEntry bedfordTut = buildClass("COMP1000-TUT-MON-0900", "COMP1000", "Tutorial",
                DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0), "Registry Building");
        ClassEntry tonsleyLec = buildClass("COMP2000-LEC-MON-0930", "COMP2000", "Lecture",
                DayOfWeek.MONDAY, LocalTime.of(9, 30), LocalTime.of(11, 30), "Tonsley T1");
        classRepository.save(bedfordTut);
        classRepository.save(tonsleyLec);

        User user = buildUser("COMP1000", "COMP2000");

        Timetable result = generatorService.generate(user, Map.of(), false, false, "TC631");

        assertFalse(result.getClassIds().contains("COMP2000-LEC-MON-0930"));
    }


    @Test
    @Order(32)
    @DisplayName("TC 6.32 – Generate allows Tonsley class followed by Bedford Park class with sufficient gap")
    @Tag("Luke")
    @Tag("Core")
    void generateAllowsTonsleyFollowedByBedfordWithSufficientGap() {
        ClassEntry tonsleyClass = buildClass("COMP1000-LEC-MON-0900", "COMP1000", "Lecture",
                DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0), "Tonsley T1");
        ClassEntry bedfordClass = buildClass("COMP2000-LEC-MON-1030", "COMP2000", "Lecture",
                DayOfWeek.MONDAY, LocalTime.of(10, 30), LocalTime.of(11, 30), "Registry Building");
        classRepository.save(tonsleyClass);
        classRepository.save(bedfordClass);

        User user = buildUser("COMP1000", "COMP2000");

        Timetable result = generatorService.generate(user, Map.of(), false, false, "TC632");

        assertAll(
                () -> assertTrue(result.getClassIds().contains("COMP1000-LEC-MON-0900")),
                () -> assertTrue(result.getClassIds().contains("COMP2000-LEC-MON-1030"),
                        "Bedford class should be included when 30-minute gap from Tonsley is met")
        );
    }


    @Test
    @Order(33)
    @DisplayName("TC 6.33 – Generate resolves campus as Bedford Park when building is null")
    @Tag("Luke")
    @Tag("Core")
    void generateResolvesCampusAsBedfordParkWhenBuildingIsNull() {
        ClassEntry nullBuilding = new ClassEntry("COMP1000-LEC-MON-0900", "Lecture", null,
                LocalTime.of(9, 0), LocalTime.of(11, 0), DayOfWeek.MONDAY,
                null, "R101", "COMP1000", "In person", 1, 1, "01 Mar", "01 Jun");
        classRepository.save(nullBuilding);

        User user = buildUser("COMP1000");

        Timetable result = generatorService.generate(user, Map.of(), false, false, "TC633");

        assertTrue(result.getClassIds().contains("COMP1000-LEC-MON-0900"),
                "Class with null building should still be included, defaulting to Bedford Park");
    }


    @Test
    @Order(34)
    @DisplayName("TC 6.34 – Generate 5-argument overload defaults to both semesters")
    @Tag("Luke")
    @Tag("Core")
    void generateFiveArgumentOverloadDefaultsToBothSemesters() {
        ClassEntry lecture = buildClass("COMP1000-LEC-MON-0900", "COMP1000", "Lecture",
                DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0), "Registry Building");
        classRepository.save(lecture);

        User user = buildUser("COMP1000");

        Timetable result = generatorService.generate(user, Map.of(), false, false, "TC634");

        assertTrue(result.getClassIds().contains("COMP1000-LEC-MON-0900"));
    }


    @Test
    @Order(35)
    @DisplayName("TC 6.35 – Generate applies afternoon preference filters afternoon classes")
    @Tag("Luke")
    @Tag("Core")
    void generateAppliesAfternoonPreferenceFiltersAfternoonClasses() {
        ClassEntry morningClass = buildClass("COMP1000-LEC-MON-0900", "COMP1000", "Lecture",
                DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0), "Registry Building");
        ClassEntry afternoonClass = buildClass("COMP1000-LEC-TUE-1400", "COMP1000", "Lecture",
                DayOfWeek.TUESDAY, LocalTime.of(14, 0), LocalTime.of(16, 0), "Registry Building");
        classRepository.save(morningClass);
        classRepository.save(afternoonClass);

        Preference pref = new Preference("student-001", List.of(Preference.TIME_AFTERNOON));
        preferenceRepository.save(pref);

        User user = buildUser("COMP1000");

        Timetable result = generatorService.generate(user, Map.of(), false, true, "TC635");

        assertTrue(result.getClassIds().contains("COMP1000-LEC-TUE-1400"),
                "Afternoon preference should favour the 14:00 class");
    }

    @Test
    @Order(36)
    @DisplayName("TC 6.36 – Generate applies evening preference filters evening classes")
    @Tag("Luke")
    @Tag("Core")
    void generateAppliesEveningPreferenceFiltersEveningClasses() {
        ClassEntry morningClass = buildClass("COMP1000-LEC-MON-0900", "COMP1000", "Lecture",
                DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0), "Registry Building");
        ClassEntry eveningClass = buildClass("COMP1000-LEC-TUE-1800", "COMP1000", "Lecture",
                DayOfWeek.TUESDAY, LocalTime.of(18, 0), LocalTime.of(20, 0), "Registry Building");
        classRepository.save(morningClass);
        classRepository.save(eveningClass);

        Preference pref = new Preference("student-001", List.of(Preference.TIME_EVENING));
        preferenceRepository.save(pref);

        User user = buildUser("COMP1000");

        Timetable result = generatorService.generate(user, Map.of(), false, true, "TC636");

        assertTrue(result.getClassIds().contains("COMP1000-LEC-TUE-1800"),
                "Evening preference should favour the 18:00 class");
    }

    @Test
    @Order(37)
    @DisplayName("TC 6.37 – Generate applies Monday preference filters Monday classes")
    @Tag("Luke")
    @Tag("Core")
    void generateAppliesMondayPreferenceFiltersMondayClasses() {
        ClassEntry mondayClass = buildClass("COMP1000-LEC-MON-0900", "COMP1000", "Lecture",
                DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0), "Registry Building");
        ClassEntry tuesdayClass = buildClass("COMP1000-LEC-TUE-0900", "COMP1000", "Lecture",
                DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(11, 0), "Registry Building");
        classRepository.save(mondayClass);
        classRepository.save(tuesdayClass);

        Preference pref = new Preference("student-001", List.of(Preference.DAY_MONDAY));
        preferenceRepository.save(pref);

        User user = buildUser("COMP1000");

        Timetable result = generatorService.generate(user, Map.of(), false, true, "TC637");

        assertTrue(result.getClassIds().contains("COMP1000-LEC-MON-0900"),
                "Monday preference should favour the Monday class");
    }

    @Test
    @Order(38)
    @DisplayName("TC 6.38 – Would clash throws for unrecognised campus combination")
    @Tag("Luke")
    @Tag("Core")
    void wouldClashThrowsForUnrecognisedCampusCombination() {
        ClassEntry first = new ClassEntry("COMP1000-LEC-MON-0900", "Lecture", null,
                LocalTime.of(9, 0), LocalTime.of(10, 0), DayOfWeek.MONDAY,
                "Online Campus A", "R101", "COMP1000", "In person", 1, 1, "01 Mar", "01 Jun");
        ClassEntry second = new ClassEntry("COMP2000-LEC-MON-1015", "Lecture", null,
                LocalTime.of(10, 15), LocalTime.of(11, 15), DayOfWeek.MONDAY,
                "Online Campus B", "R102", "COMP2000", "In person", 1, 1, "01 Mar", "01 Jun");

        assertThrows(IllegalStateException.class,
                () -> generatorService.wouldClash(second, List.of(first), false));
    }
}