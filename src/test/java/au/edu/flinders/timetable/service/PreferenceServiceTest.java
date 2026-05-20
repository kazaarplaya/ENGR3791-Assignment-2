package au.edu.flinders.timetable.service;

import au.edu.flinders.timetable.model.Preference;
import au.edu.flinders.timetable.repository.PreferenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** Unit tests for PreferenceService. */
class PreferenceServiceTest {

    private PreferenceRepository repo;
    private PreferenceService    service;

    @BeforeEach
    void setUp() {
        repo    = new PreferenceRepository();
        service = new PreferenceService(repo);
    }

    @Test
    void savePreference_invalidTimeOfDay_throwsIllegalArgumentException() {
        Preference pref = new Preference("u1", "Any", "Midday", "Any", 1, 2, 3);
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> service.savePreference(pref)
        );
        assertTrue(ex.getMessage().toLowerCase().contains("timeofdaY".toLowerCase())
            || ex.getMessage().toLowerCase().contains("invalid"));
    }

    @Test
    void savePreference_invalidDay_throwsIllegalArgumentException() {
        Preference pref = new Preference("u1", "Any", "Morning", "Funday", 1, 2, 3);
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> service.savePreference(pref)
        );
        assertTrue(ex.getMessage().toLowerCase().contains("day")
            || ex.getMessage().toLowerCase().contains("invalid"));
    }

    @Test
    void savePreference_valid_canBeRetrievedByUserId() {
        Preference pref = new Preference("u1", "City", "Morning", "Monday", 1, 2, 3);
        service.savePreference(pref);

        Optional<Preference> found = service.getPreference("u1");
        assertTrue(found.isPresent());
        assertEquals("City", found.get().getCampus());
        assertEquals("Morning", found.get().getTimeOfDay());
        assertEquals("Monday", found.get().getDay());
    }

    @Test
    void clearPreference_removesPreference() {
        Preference pref = new Preference("u1", "Any", "Any", "Any", 1, 2, 3);
        service.savePreference(pref);
        service.clearPreference("u1");

        assertTrue(service.getPreference("u1").isEmpty());
    }
}
