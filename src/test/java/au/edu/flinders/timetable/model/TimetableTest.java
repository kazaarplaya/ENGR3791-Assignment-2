package au.edu.flinders.timetable.model;

import au.edu.flinders.timetable.repository.TimetableRepository;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TimetableTest {

    private Timetable timetable;

    @BeforeEach
    void setUp() {
        timetable = new Timetable("My Timetable", "Semester 1", false, false);
    }

    @AfterEach
    void tearDown() {}


    @Test
    @Order(1)
    @DisplayName("TC 7.01 – Constructor creates timetable with correct name and flags")
    @Tag("Luke")
    @Tag("Core")
    void constructorCreatesTimetableWithCorrectNameAndFlags() {
        assertAll(
                () -> assertEquals("My Timetable", timetable.getTimetableName()),
                () -> assertEquals("Semester 1", timetable.getSemester()),
                () -> assertFalse(timetable.isOverlap()),
                () -> assertFalse(timetable.isHasPreference())
        );
    }


    @Test
    @Order(2)
    @DisplayName("TC 7.02 – addClass adds class ID to timetable")
    @Tag("Luke")
    @Tag("Core")
    void addClassAddsClassIdToTimetable() {
        timetable.addClass("COMP1000-LEC-MON-0900");

        assertTrue(timetable.getClassIds().contains("COMP1000-LEC-MON-0900"));
    }


    @Test
    @Order(3)
    @DisplayName("TC 7.03 – addClass does not add duplicate class ID")
    @Tag("Luke")
    @Tag("Core")
    void addClassDoesNotAddDuplicateClassId() {
        timetable.addClass("COMP1000-LEC-MON-0900");
        timetable.addClass("COMP1000-LEC-MON-0900");

        assertEquals(1, timetable.getClassIds().size(),
                "Duplicate class ID should not be added");
    }


    @Test
    @Order(4)
    @DisplayName("TC 7.04 – removeClass removes existing class ID")
    @Tag("Luke")
    @Tag("Core")
    void removeClassRemovesExistingClassId() {
        timetable.addClass("COMP1000-LEC-MON-0900");
        timetable.removeClass("COMP1000-LEC-MON-0900");

        assertFalse(timetable.getClassIds().contains("COMP1000-LEC-MON-0900"));
    }


    @Test
    @Order(5)
    @DisplayName("TC 7.05 – removeClass does nothing when class ID not present")
    @Tag("Luke")
    @Tag("Core")
    void removeClassDoesNothingWhenClassIdNotPresent() {
        assertDoesNotThrow(() -> timetable.removeClass("NONEXISTENT-ID"));
        assertTrue(timetable.isEmpty());
    }


    @Test
    @Order(6)
    @DisplayName("TC 7.06 – isEmpty returns true for new timetable")
    @Tag("Luke")
    @Tag("Core")
    void isEmptyReturnsTrueForNewTimetable() {
        assertTrue(timetable.isEmpty());
    }


    @Test
    @Order(7)
    @DisplayName("TC 7.07 – isEmpty returns false after class is added")
    @Tag("Luke")
    @Tag("Core")
    void isEmptyReturnsFalseAfterClassIsAdded() {
        timetable.addClass("COMP1000-LEC-MON-0900");

        assertFalse(timetable.isEmpty());
    }


    @Test
    @Order(8)
    @DisplayName("TC 7.08 – getClassIds returns unmodifiable list")
    @Tag("Luke")
    @Tag("Core")
    void getClassIdsReturnsUnmodifiableList() {
        timetable.addClass("COMP1000-LEC-MON-0900");
        List<String> ids = timetable.getClassIds();

        assertThrows(UnsupportedOperationException.class,
                () -> ids.add("COMP2000-LEC-TUE-0900"),
                "getClassIds should return an unmodifiable list");
    }


    @Test
    @Order(9)
    @DisplayName("TC 7.09 – setTimetableName updates timetable name")
    @Tag("Luke")
    @Tag("Core")
    void setTimetableNameUpdatesTimetableName() {
        timetable.setTimetableName("Updated Name");

        assertEquals("Updated Name", timetable.getTimetableName());
    }


    @Test
    @Order(10)
    @DisplayName("TC 7.10 – setSemester updates semester label")
    @Tag("Luke")
    @Tag("Core")
    void setSemesterUpdatesSemesterLabel() {
        timetable.setSemester("Semester 2");

        assertEquals("Semester 2", timetable.getSemester());
    }


    @Test
    @Order(11)
    @DisplayName("TC 7.11 – setOverlap updates overlap flag")
    @Tag("Luke")
    @Tag("Core")
    void setOverlapUpdatesOverlapFlag() {
        timetable.setOverlap(true);

        assertTrue(timetable.isOverlap());
    }


    @Test
    @Order(12)
    @DisplayName("TC 7.12 – setHasPreference updates preference flag")
    @Tag("Luke")
    @Tag("Core")
    void setHasPreferenceUpdatesPreferenceFlag() {
        timetable.setHasPreference(true);

        assertTrue(timetable.isHasPreference());
    }


    @Test
    @Order(13)
    @DisplayName("TC 7.13 – toString returns non-null readable summary")
    @Tag("Luke")
    @Tag("Core")
    void toStringReturnsNonNullReadableSummary() {
        assertAll(
                () -> assertNotNull(timetable.toString()),
                () -> assertTrue(timetable.toString().contains("My Timetable"))
        );
    }


    @Test
    @Order(14)
    @DisplayName("TC 7.14 – getName returns timetable name")
    @Tag("Luke")
    @Tag("Core")
    void getNameReturnsTimetableName() {
        assertEquals("My Timetable", timetable.getName());
    }


    @Test
    @Order(15)
    @DisplayName("TC 7.15 – findByName returns timetable when it exists")
    @Tag("Luke")
    @Tag("Core")
    void findByNameReturnsTimetableWhenItExists() {
        TimetableRepository repository = new TimetableRepository();
        repository.save(timetable);

        Optional<Timetable> result = repository.findByName("My Timetable");

        assertTrue(result.isPresent());
        assertEquals("My Timetable", result.get().getTimetableName());
    }
}