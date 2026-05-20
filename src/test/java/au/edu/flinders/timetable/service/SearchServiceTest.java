package au.edu.flinders.timetable.service;

import au.edu.flinders.timetable.model.ClassEntry;
import au.edu.flinders.timetable.model.Topic;
import au.edu.flinders.timetable.repository.ClassRepository;
import au.edu.flinders.timetable.repository.TopicRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Unit tests for SearchService. */
class SearchServiceTest {

    private ClassRepository classRepo;
    private TopicRepository topicRepo;
    private SearchService   service;

    @BeforeEach
    void setUp() {
        classRepo = new ClassRepository();
        topicRepo = new TopicRepository();
        service   = new SearchService(classRepo, topicRepo);

        // Seed a topic and two classes
        topicRepo.save(new Topic("COMP1000", "Computer Programming 1", "City", 1, "In Person", 24));
        topicRepo.save(new Topic("MATH1000", "Mathematics 1", "Bedford Park", 1, "Online", 12));

        classRepo.save(new ClassEntry("C001", "Lecture", null,
            LocalTime.of(9, 0), LocalTime.of(11, 0),
            DayOfWeek.MONDAY, "Registry", "R101", "COMP1000"));

        classRepo.save(new ClassEntry("C002", "Tutorial", null,
            LocalTime.of(13, 0), LocalTime.of(14, 0),
            DayOfWeek.TUESDAY, "Tonsley", "T201", "MATH1000"));
    }

    @Test
    void search_partialCourseCode_returnsMatchingClasses() {
        List<ClassEntry> results = service.search("COMP");
        assertEquals(1, results.size());
        assertEquals("C001", results.get(0).getClassId());
    }

    @Test
    void search_caseInsensitive_returnsMatchingClasses() {
        List<ClassEntry> results = service.search("lecture");
        assertEquals(1, results.size());
        assertEquals("C001", results.get(0).getClassId());
    }

    @Test
    void search_noMatch_returnsEmptyList() {
        List<ClassEntry> results = service.search("ZZZNOMATCH");
        assertTrue(results.isEmpty());
    }

    @Test
    void search_emptyQuery_returnsAllClasses() {
        List<ClassEntry> results = service.search("");
        assertEquals(2, results.size());
    }
}
