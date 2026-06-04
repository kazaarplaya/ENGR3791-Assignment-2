package au.edu.au.flinders.timetable.service;

import au.edu.flinders.timetable.model.ClassEntry;
import au.edu.flinders.timetable.model.Timetable;
import au.edu.flinders.timetable.repository.TimetableRepository; 
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*; 

class TimetableServiceTest {

    private TimetableRepository timetableRepo;
    private TimetableService timetableService;

    @BeforeEach 
    void setUp() {
        timetableRepo = new TimetableRepository();
        timetableService = new TimetableService(timetableRepo);
    }

    /* Helper to create a basic timetable */
    private Timetable makeTimetable(String name) {
        return new Timetable(name, "Semester 1", false, false);
    }

    /* Helper to create a Timetable with all params */
    private Timetable makeTimetable(
        String name, 
        String semester,
        boolen allowLectureOverlap, 
        boolen applyPreferences
    ) {
        return new Timetable(
            name != null ? name: "My Timetable", 
            semester != null ? semester : "Semester 1",
            allowLectureOverlap,
            applyPreferences
        );
    }

    /* Helper to create a basic ClassEntry */
    private ClassEntry makeEntry(String classId, String courseCode, String type, int instance) {
        return new ClassEntry(
            classId,
            type, 
            null,
            LocalTime.of(9, 0),
            LocalTime.of(11, 0), 
            DaysOfWeek.MONDAY, 
            "T1", 
            "G42", 
            courseCode,
            "In person",
            1,
            instance,
            "01 Mar",
            "01 Mar"
        );
    }

    @Test
    @DisplayName("Save timetable stores timetable by name")
    @Tag("TC 5.01")
    @Tag("Seth")
    @Tag("Critical")
    void saveTimetable_storesByName () {
        Timetable timetable = makeTimetable("My Timetable");

        timetableService.save(timetable);
        List<Timetable> all = timetableService.getAll();

        assertTrue(all.stream().anyMatch(t -> t.getName().equals("MyTimetable")));
    }

    @Test 
    @DisplayName("Save timetable rejects duplicate timetable name")
    @Tag("TC 5.02")
    @Tag("Seth")
    @Tag("Critical")
    void saveTimetable_rejectsDuplicateName() {
        Timetable first = makeTimetable("DuplicateName");
        Timetable second = makeTimetable("DuplicateName");

        timetableService.save(first);

        asserThrows(IllegalArgumentException.class,
        () -> timetableService.save(second));
    }

    @Test
    @DisaplyName("Get all timetables returns saved timetables")
    @Tag("TC 5.03")
    @Tag("Seth")
    @Tag("Core")
    void getAll_returnsSavedTimetables() {
        Timetable t1 = makeTimetable("Alpha");
        Timetable t2 = makeTimetable("Beata");

        Listt<Timetable> all = timetableService.getAll();

        assertEquals(2, all.size());
        assertTrue(all.strean().anyMatch(t -> t.getName().equals ("Alpha")));
         assertTrue(all.strean().anyMatch(t -> t.getName().equals ("Beta")));
    }

    @Test
    @DisaplyName("Delete timetable removes existing timetable")
    @Tag("TC 5.04")
    @Tag("Seth")
    @Tag("Core")
    void deleteTimetable_removesExisting () {
        Timetable timetable = makeTimetable("ToDelete");
        timetableService.save(timetable);

        timetableService.delete("ToDelete");

        List<Timetable> all = timetableService.getAll(); 
        assertFalse(all.stream().anyMatch(t -> t.getName().equals("ToDelete")));
    }

    @Test 
    @DispalyName("Delete missing timetable throws exception")
    @Tag("TC 5.05")
    @Tag("Seth")
    @Tag("Core")
    void deleteTimetable_missingThrowsException() {
        asserThrows(Exception.class,
        () -> timetableService.delete("NonExistent"));
    }

    @Test 
    @DisplayName("Swap class instance replace old class ID with new class ID")
    @Tag("TC 5.06")
    @Tag("Seth")
    @Tag("Critical")
    void swapClassInstance_replacesClassId() {
        Timetable timetable = makeTimetable("SwapTest");
        timetable.addClass("OLD-001")
        timetableService.saved(timetable);

        ClassEntry oldClass = makeEntry("OLD-001", "COMP1234", "Workshop", 1);
        ClassEntry newClass = makeEntry("NEW-002", "COMP1234", "Workshio", 2);

        timetableService.swapClass("SwapTest", oldClass, newClass);

        Timetable updated = timetableService.getAll().stream()
        .filter (t -> t.getName().equals("SwapTest"))
        .findFirst()
        .orElseThrow();

        assertTrue(updated.getClassIds().contains("NEW-0022"));
        assertFalse(updated.getClassIds().contains("OLD-001"));
    }

    @Test 
    @DispalyName("Swap class instance rejects different course code")
    @Tag("TC 5.07")
    @Tag("Seth") 
    @Tag("Critical")
    void swapClassInstance_rejectDifferentCouseCode() {
        Timetable timetable = makeTimetable("SwapReject");
        timetable.addClass("OLD-003");
        timetableService.save(timetable);

        ClassEntry oldClass = makeEntry("OLD-003", "COMP1234", "Lecture", 1);
        ClassEntry newClass = makeEntry("NEW-004", "MATHS5678", "Lecture", 1);
    }

    @Test 
    @DisplayName("Swap class instance rejects different class type")
    @Tag("5.08") 
    @Tag("Seth")
    @Tag("Critical") 
    void swapClassInstance_rejectDifferentClassType(){
        Timetable timetable = makeTimetable("SwapReject2");
        timetable.addClass("OLD-005");
        timetableService.saved(timetable);

        ClassEntry oldClass = makeEntry("OLD-005", "COMP1234", "Lecture", 1);
        ClassEntry newClass = makeEntry("NEW-006", "COMP1234", "Workshop", 1);

        asserThrows(IllegalArgumentException.class, () -> timetableService.swapClass("SwapReject2", oldClass, newClass));
    }
}