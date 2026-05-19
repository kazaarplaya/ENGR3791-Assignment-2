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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Unit tests for TimetableGeneratorService. */
class TimetableGeneratorServiceTest {

    private ClassRepository           classRepo;
    private TopicRepository           topicRepo;
    private PreferenceRepository      prefRepo;
    private TimetableRepository       timetableRepo;
    private TimetableService          timetableService;
    private TimetableGeneratorService generator;

    @BeforeEach
    void setUp() {
        classRepo       = new ClassRepository();
        topicRepo       = new TopicRepository();
        prefRepo        = new PreferenceRepository();
        timetableRepo   = new TimetableRepository();
        timetableService = new TimetableService(timetableRepo);
        generator       = new TimetableGeneratorService(
            classRepo, topicRepo, prefRepo, timetableService);
    }

    // ── Helper methods ────────────────────────────────────────────────────────

    /** Creates and stores a Topic. */
    private Topic topic(String code, String campus) {
        Topic t = new Topic(code, campus, 1, "In Person", 10);
        topicRepo.save(t);
        return t;
    }

    /** Creates and stores a ClassEntry. */
    private ClassEntry classEntry(String id, String type, String courseCode,
                                   DayOfWeek day, LocalTime start, LocalTime end) {
        ClassEntry c = new ClassEntry(id, type, null, start, end, day, "Building", "R1", courseCode);
        classRepo.save(c);
        return c;
    }

    /** Creates a user enrolled in the given course codes. */
    private User user(String... codes) {
        User u = new User("u1", "Test");
        for (String code : codes) u.enrol(code);
        return u;
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    void generate_noEnrolledTopics_returnsEmptyTimetable() {
        User u = new User("u1", "Test"); // no enrolments
        Timetable t = generator.generate(u, Map.of(), false, false, "T1");
        assertTrue(t.isEmpty());
    }

    @Test
    void generate_singleTopic_singleCampus_noClash_succeeds() {
        topic("COMP1000", "City");
        classEntry("C001", "Lecture", "COMP1000",
            DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0));

        Timetable t = generator.generate(
            user("COMP1000"), Map.of("COMP1000", "City"), false, false, "T1");

        assertFalse(t.isEmpty());
        assertTrue(t.getClassIds().contains("C001"));
    }

    @Test
    void generate_cityAndBedfordPark_alwaysRequires30MinGap() {
        topic("COMP1000", "City");
        topic("MATH1000", "Bedford Park");

        // City class ends 10:00, Bedford Park class starts 10:20 — only 20-min gap → clash
        classEntry("C001", "Lecture", "COMP1000",
            DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0));
        classEntry("C002", "Lecture", "MATH1000",
            DayOfWeek.MONDAY, LocalTime.of(10, 20), LocalTime.of(11, 20));

        Timetable t = generator.generate(
            user("COMP1000", "MATH1000"), Map.of(), false, false, "T1");

        // C001 should be accepted; C002 should be rejected (gap < 30 min)
        assertTrue(t.getClassIds().contains("C001"));
        assertFalse(t.getClassIds().contains("C002"));
    }

    @Test
    void generate_bedfordAndTonsley_bothLectures_overlapEnabled_allowsBackToBack() {
        topic("COMP1000", "Bedford Park");
        topic("MATH1000", "Tonsley");

        // Back-to-back: no gap at all
        classEntry("C001", "Lecture", "COMP1000",
            DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0));
        classEntry("C002", "Lecture", "MATH1000",
            DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(11, 0));

        Timetable t = generator.generate(
            user("COMP1000", "MATH1000"), Map.of(), true, false, "T1");

        assertTrue(t.getClassIds().contains("C001"));
        assertTrue(t.getClassIds().contains("C002"));
    }

    @Test
    void generate_bedfordAndTonsley_bothLectures_overlapDisabled_requires30MinGap() {
        topic("COMP1000", "Bedford Park");
        topic("MATH1000", "Tonsley");

        // Back-to-back: 0 gap → should clash when overlap disabled
        classEntry("C001", "Lecture", "COMP1000",
            DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0));
        classEntry("C002", "Lecture", "MATH1000",
            DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(11, 0));

        Timetable t = generator.generate(
            user("COMP1000", "MATH1000"), Map.of(), false, false, "T1");

        assertTrue(t.getClassIds().contains("C001"));
        assertFalse(t.getClassIds().contains("C002"));
    }

    @Test
    void generate_bedfordAndTonsley_lectureAndNonLecture_overlapEnabled_allowsBackToBack() {
        topic("COMP1000", "Bedford Park");
        topic("MATH1000", "Tonsley");

        classEntry("C001", "Lecture",  "COMP1000",
            DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0));
        classEntry("C002", "Tutorial", "MATH1000",
            DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(11, 0));

        Timetable t = generator.generate(
            user("COMP1000", "MATH1000"), Map.of(), true, false, "T1");

        assertTrue(t.getClassIds().contains("C001"));
        assertTrue(t.getClassIds().contains("C002"));
    }

    @Test
    void generate_preferenceFilterDay_excludesNonMatchingClasses() {
        topic("COMP1000", "City");
        classEntry("C001", "Lecture", "COMP1000",
            DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0));
        classEntry("C002", "Lecture", "COMP1000",
            DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(11, 0));

        // Prefer Tuesday only
        prefRepo.save(new Preference("u1", "Any", "Any", "Tuesday"));
        User u = user("COMP1000");

        Timetable t = generator.generate(u, Map.of(), false, true, "T1");

        assertFalse(t.getClassIds().contains("C001")); // Monday — excluded
        assertTrue(t.getClassIds().contains("C002"));  // Tuesday — kept
    }

    @Test
    void generate_preferenceFilterTimeOfDay_morning_excludesAfternoonClasses() {
        topic("COMP1000", "City");
        classEntry("C001", "Lecture", "COMP1000",
            DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0));   // morning
        classEntry("C002", "Lecture", "COMP1000",
            DayOfWeek.TUESDAY, LocalTime.of(13, 0), LocalTime.of(15, 0)); // afternoon

        prefRepo.save(new Preference("u1", "Any", "Morning", "Any"));
        User u = user("COMP1000");

        Timetable t = generator.generate(u, Map.of(), false, true, "T1");

        assertTrue(t.getClassIds().contains("C001"));  // morning — kept
        assertFalse(t.getClassIds().contains("C002")); // afternoon — excluded
    }

    @Test
    void generate_noClassesAfterFilter_throwsIllegalStateException() {
        topic("COMP1000", "City");
        // No classes stored for COMP1000 at City
        User u = user("COMP1000");

        assertThrows(
            IllegalStateException.class,
            () -> generator.generate(u, Map.of("COMP1000", "City"), false, false, "T1")
        );
    }

    // ── Refinement Prompt C tests ─────────────────────────────────────────────

    @Test
    void generate_duplicateName_throwsIllegalArgumentException() {
        topic("COMP1000", "City");
        classEntry("C001", "Lecture", "COMP1000",
            DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0));

        User u1 = user("COMP1000");
        generator.generate(u1, Map.of(), false, false, "MyTimetable");

        // Fresh user — same name should fail
        User u2 = new User("u2", "Student2");
        u2.enrol("COMP1000");

        assertThrows(
            IllegalArgumentException.class,
            () -> generator.generate(u2, Map.of(), false, false, "MyTimetable")
        );
    }

    @Test
    void generate_applyPreferenceWithNoMatchingClasses_throwsIllegalStateException() {
        topic("COMP1000", "City");
        // Only an afternoon class exists
        classEntry("C001", "Lecture", "COMP1000",
            DayOfWeek.MONDAY, LocalTime.of(14, 0), LocalTime.of(16, 0));

        // Preference requires morning — no classes survive the filter
        prefRepo.save(new Preference("u1", "Any", "Morning", "Any"));
        User u = user("COMP1000");

        assertThrows(
            IllegalStateException.class,
            () -> generator.generate(u, Map.of(), false, true, "T1")
        );
    }

    @Test
    void generate_autoName_followsSequentialFormat() {
        topic("COMP1000", "City");
        classEntry("C001", "Lecture", "COMP1000",
            DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0));

        User u1 = user("COMP1000");
        Timetable first = generator.generate(u1, Map.of(), false, false, "");

        // Clear timetable repo and class repo for a fresh second call
        classRepo.save(new ClassEntry("C001", "Lecture", null,
            LocalTime.of(9, 0), LocalTime.of(11, 0),
            DayOfWeek.MONDAY, "Building", "R1", "COMP1000"));
        User u2 = new User("u2", "Student2");
        u2.enrol("COMP1000");
        Timetable second = generator.generate(u2, Map.of(), false, false, "");

        assertTrue(first.getTimetableName().matches("Timetable_\\d+"),
            "First name should follow Timetable_N format");
        assertTrue(second.getTimetableName().matches("Timetable_\\d+"),
            "Second name should follow Timetable_N format");
        assertNotEquals(first.getTimetableName(), second.getTimetableName(),
            "Auto-generated names must be unique");
    }
}
