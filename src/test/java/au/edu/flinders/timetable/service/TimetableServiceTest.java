package au.edu.flinders.timetable.service;

import au.edu.flinders.timetable.model.Timetable;
import au.edu.flinders.timetable.repository.TimetableRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** Unit tests for TimetableService. */
class TimetableServiceTest {

    private TimetableRepository repo;
    private TimetableService    service;

    @BeforeEach
    void setUp() {
        repo    = new TimetableRepository();
        service = new TimetableService(repo);
    }

    @Test
    void generateUniqueName_firstCall_returnsTimetable_1() {
        String name = service.generateUniqueName();
        assertEquals("Timetable_1", name);
    }

    @Test
    void generateUniqueName_subsequentCalls_incrementsCounter() {
        service.generateUniqueName(); // Timetable_1
        service.generateUniqueName(); // Timetable_2
        String third = service.generateUniqueName();
        assertEquals("Timetable_3", third);
    }

    @Test
    void save_duplicateName_throwsIllegalArgumentException() {
        Timetable t = new Timetable("MyTimetable", "Semester 1", false, false);
        service.save(t);

        Timetable duplicate = new Timetable("MyTimetable", "Semester 1", false, false);
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> service.save(duplicate)
        );
        assertTrue(ex.getMessage().contains("already exists"));
    }

    @Test
    void delete_existingTimetable_removesIt() {
        Timetable t = new Timetable("DeleteMe", "Semester 1", false, false);
        service.save(t);
        service.delete("DeleteMe");
        Optional<Timetable> found = service.getByName("DeleteMe");
        assertTrue(found.isEmpty());
    }

    @Test
    void delete_nonExistentTimetable_throwsIllegalArgumentException() {
        assertThrows(
            IllegalArgumentException.class,
            () -> service.delete("Ghost")
        );
    }
}
