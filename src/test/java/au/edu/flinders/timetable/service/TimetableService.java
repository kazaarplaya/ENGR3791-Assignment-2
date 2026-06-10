package au.edu.flinders.timetable.service;

import au.edu.flinders.timetable.model.ClassEntry;
import au.edu.flinders.timetable.model.Timetable;
import au.edu.flinders.timetable.repository.ClassRepository;
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
            boolean allowLectureOverlap,
            boolean applyPreferences
    ) {
        return new Timetable(
                name != null ? name : "My Timetable",
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
                DayOfWeek.MONDAY,
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
    void saveTimetable_storesByName() {
        Timetable timetable = makeTimetable("My Timetable");

        timetableService.save(timetable);
        List<Timetable> all = timetableService.getAll();


        assertTrue(all.stream().anyMatch(t -> t.getName().equals("My Timetable")));
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


        assertThrows(IllegalArgumentException.class,
                () -> timetableService.save(second));
    }

    @Test
    @DisplayName("Get all timetables returns saved timetables")
    @Tag("TC 5.03")
    @Tag("Seth")
    @Tag("Core")
    void getAll_returnsSavedTimetables() {
        Timetable t1 = makeTimetable("Alpha");
        Timetable t2 = makeTimetable("Beta");

        timetableService.save(t1);
        timetableService.save(t2);

        List<Timetable> all = timetableService.getAll();

        assertEquals(2, all.size());
        assertTrue(all.stream().anyMatch(t -> t.getName().equals("Alpha")));
        assertTrue(all.stream().anyMatch(t -> t.getName().equals("Beta")));
    }

    @Test
    @DisplayName("Delete timetable removes existing timetable")
    @Tag("TC 5.04")
    @Tag("Seth")
    @Tag("Core")
    void deleteTimetable_removesExisting() {
        Timetable timetable = makeTimetable("ToDelete");
        timetableService.save(timetable);

        timetableService.delete("ToDelete");

        List<Timetable> all = timetableService.getAll();
        assertFalse(all.stream().anyMatch(t -> t.getName().equals("ToDelete")));
    }

    @Test
    @DisplayName("Delete missing timetable throws exception")
    @Tag("TC 5.05")
    @Tag("Seth")
    @Tag("Core")
    void deleteTimetable_missingThrowsException() {
        assertThrows(Exception.class,
                () -> timetableService.delete("NonExistent"));
    }

    @Test
    @DisplayName("Swap class instance replace old class ID with new class ID")
    @Tag("TC 5.06")
    @Tag("Seth")
    @Tag("Critical")
    void swapClassInstance_replacesClassId() {
        Timetable timetable = makeTimetable("SwapTest");
        timetable.addClass("OLD-001");
        timetableService.save(timetable);

        ClassEntry oldClass = makeEntry("OLD-001", "COMP1234", "Workshop", 1);
        ClassEntry newClass = makeEntry("NEW-002", "COMP1234", "Workshop", 2);

        timetableService.swapClass("SwapTest", oldClass, newClass);

        Timetable updated = timetableService.getAll().stream()
                .filter(t -> t.getName().equals("SwapTest"))
                .findFirst()
                .orElseThrow();

        assertTrue(updated.getClassIds().contains("NEW-002"));
        assertFalse(updated.getClassIds().contains("OLD-001"));
    }

    @Test
    @DisplayName("Swap class instance rejects different course code")
    @Tag("TC 5.07")
    @Tag("Seth")
    @Tag("Critical")
    void swapClassInstance_rejectDifferentCourseCode() {
        Timetable timetable = makeTimetable("SwapReject");
        timetable.addClass("OLD-003");
        timetableService.save(timetable);

        ClassEntry oldClass = makeEntry("OLD-003", "COMP1234", "Lecture", 1);
        ClassEntry newClass = makeEntry("NEW-004", "MATHS5678", "Lecture", 1);


        assertThrows(IllegalArgumentException.class,
                () -> timetableService.swapClass("SwapReject", oldClass, newClass));
    }

    @Test
    @DisplayName("Swap class instance rejects different class type")
    @Tag("TC 5.08")
    @Tag("Seth")
    @Tag("Critical")
    void swapClassInstance_rejectDifferentClassType() {
        Timetable timetable = makeTimetable("SwapReject2");
        timetable.addClass("OLD-005");
        timetableService.save(timetable);

        ClassEntry oldClass = makeEntry("OLD-005", "COMP1234", "Lecture", 1);
        ClassEntry newClass = makeEntry("NEW-006", "COMP1234", "Workshop", 1);

        assertThrows(IllegalArgumentException.class,
                () -> timetableService.swapClass("SwapReject2", oldClass, newClass));
    }

    public void swapClass(String timetableName, ClassEntry oldClass, ClassEntry newClass) {
        if (!oldClass.getCourseCode().equals(newClass.getCourseCode())) {
            throw new IllegalArgumentException("Cannot swap classes from different courses.");
        }

        if(!oldClass.getType().equals((newClass.getType()))) {
            throw new IllegalArgumentException("Cannot swap classes from different courses.");
        }
        Timetable timetable = timetableRepo.findByName(timetableName)
                .orElseThrow(() -> new IllegalArgumentException("Timetable not found: " +timetableName));

        timetable.removeClass(oldClass.getClassId());
        timetable.addClass(newClass.getClassId());
    }


    @Test
    @DisplayName("TC 5.09 – getByName returns present when timetable exists")
    @Tag("Luke")
    @Tag("Core")
    void getByNameReturnsPresentWhenTimetableExists() {
        timetableService.save(makeTimetable("FindMe"));

        assertTrue(timetableService.getByName("FindMe").isPresent());
    }

    @Test
    @DisplayName("TC 5.10 – getByName returns empty when timetable does not exist")
    @Tag("Luke")
    @Tag("Core")
    void getByNameReturnsEmptyWhenTimetableAbsent() {
        assertTrue(timetableService.getByName("Ghost").isEmpty());
    }

    @Test
    @DisplayName("TC 5.11 – swapClassInstance throws when timetable not found")
    @Tag("Luke")
    @Tag("Core")
    void swapClassInstanceThrowsWhenTimetableNotFound() {
        assertThrows(IllegalArgumentException.class,
                () -> timetableService.swapClassInstance("NoSuchTimetable", "OLD-001", "NEW-002"));
    }

    @Test
    @DisplayName("TC 5.12 – swapClassInstance throws when old class not in timetable")
    @Tag("Luke")
    @Tag("Core")
    void swapClassInstanceThrowsWhenOldClassNotInTimetable() {
        Timetable t = makeTimetable("SwapT");
        timetableService.save(t);

        assertThrows(IllegalArgumentException.class,
                () -> timetableService.swapClassInstance("SwapT", "MISSING-001", "NEW-002"));
    }

    @Test
    @DisplayName("TC 5.13 – swapClassInstance succeeds without class repository validation")
    @Tag("Luke")
    @Tag("Core")
    void swapClassInstanceSucceedsWithNullClassRepository() {
        Timetable t = makeTimetable("SwapNoRepo");
        t.addClass("OLD-001");
        timetableService.save(t);

        timetableService.swapClassInstance("SwapNoRepo", "OLD-001", "NEW-002");

        Timetable updated = timetableService.getByName("SwapNoRepo").orElseThrow();
        assertAll(
                () -> assertTrue(updated.getClassIds().contains("NEW-002")),
                () -> assertFalse(updated.getClassIds().contains("OLD-001"))
        );
    }

    @Test
    @DisplayName("TC 5.14 – swapClassInstance with repo throws when old class not in repository")
    @Tag("Luke")
    @Tag("Core")
    void swapClassInstanceThrowsWhenOldClassNotInRepository() {
        ClassRepository classRepo = new ClassRepository();
        TimetableService svc = new TimetableService(timetableRepo, classRepo);

        Timetable t = makeTimetable("SwapRepo1");
        t.addClass("OLD-001");
        timetableRepo.save(t);

        assertThrows(IllegalArgumentException.class,
                () -> svc.swapClassInstance("SwapRepo1", "OLD-001", "NEW-002"));
    }

    @Test
    @DisplayName("TC 5.15 – swapClassInstance with repo throws when new class not in repository")
    @Tag("Luke")
    @Tag("Core")
    void swapClassInstanceThrowsWhenNewClassNotInRepository() {
        ClassRepository classRepo = new ClassRepository();
        TimetableService svc = new TimetableService(timetableRepo, classRepo);

        ClassEntry old = makeEntry("OLD-001", "COMP1001", "Lecture", 1);
        classRepo.save(old);

        Timetable t = makeTimetable("SwapRepo2");
        t.addClass("OLD-001");
        timetableRepo.save(t);

        assertThrows(IllegalArgumentException.class,
                () -> svc.swapClassInstance("SwapRepo2", "OLD-001", "NEW-002"));
    }

    @Test
    @DisplayName("TC 5.16 – swapClassInstance with repo throws when course codes differ")
    @Tag("Luke")
    @Tag("Core")
    void swapClassInstanceThrowsWhenCourseCodesDiffer() {
        ClassRepository classRepo = new ClassRepository();
        TimetableService svc = new TimetableService(timetableRepo, classRepo);

        ClassEntry old = makeEntry("OLD-001", "COMP1001", "Lecture", 1);
        ClassEntry newE = makeEntry("NEW-002", "MATH1001", "Lecture", 1);
        classRepo.save(old);
        classRepo.save(newE);

        Timetable t = makeTimetable("SwapRepo3");
        t.addClass("OLD-001");
        timetableRepo.save(t);

        assertThrows(IllegalArgumentException.class,
                () -> svc.swapClassInstance("SwapRepo3", "OLD-001", "NEW-002"));
    }

    @Test
    @DisplayName("TC 5.17 – swapClassInstance with repo throws when class types differ")
    @Tag("Luke")
    @Tag("Core")
    void swapClassInstanceThrowsWhenClassTypesDiffer() {
        ClassRepository classRepo = new ClassRepository();
        TimetableService svc = new TimetableService(timetableRepo, classRepo);

        ClassEntry old = makeEntry("OLD-001", "COMP1001", "Lecture", 1);
        ClassEntry newE = makeEntry("NEW-002", "COMP1001", "Workshop", 1);
        classRepo.save(old);
        classRepo.save(newE);

        Timetable t = makeTimetable("SwapRepo4");
        t.addClass("OLD-001");
        timetableRepo.save(t);

        assertThrows(IllegalArgumentException.class,
                () -> svc.swapClassInstance("SwapRepo4", "OLD-001", "NEW-002"));
    }

    @Test
    @DisplayName("TC 5.18 – swapClassInstance with repo succeeds when classes are compatible")
    @Tag("Luke")
    @Tag("Core")
    void swapClassInstanceSucceedsWithCompatibleClasses() {
        ClassRepository classRepo = new ClassRepository();
        TimetableService svc = new TimetableService(timetableRepo, classRepo);

        ClassEntry old = makeEntry("OLD-001", "COMP1001", "Lecture", 1);
        ClassEntry newE = makeEntry("NEW-002", "COMP1001", "Lecture", 2);
        classRepo.save(old);
        classRepo.save(newE);

        Timetable t = makeTimetable("SwapRepo5");
        t.addClass("OLD-001");
        timetableRepo.save(t);

        svc.swapClassInstance("SwapRepo5", "OLD-001", "NEW-002");

        Timetable updated = svc.getByName("SwapRepo5").orElseThrow();
        assertAll(
                () -> assertTrue(updated.getClassIds().contains("NEW-002")),
                () -> assertFalse(updated.getClassIds().contains("OLD-001"))
        );
    }


    @Test
    @DisplayName("TC 5.19 – generateUniqueName returns sequentially numbered names")
    @Tag("Luke")
    @Tag("Core")
    void generateUniqueNameReturnsSequentialNames() {
        String first = timetableService.generateUniqueName();
        String second = timetableService.generateUniqueName();

        assertEquals("Timetable_1", first);
        assertEquals("Timetable_2", second);
    }

    @Test
    @DisplayName("TC 5.20 – generateUniqueName returns unique name on each call")
    @Tag("Luke")
    @Tag("Core")
    void generateUniqueNameReturnsUniqueNameEachCall() {
        String a = timetableService.generateUniqueName();
        String b = timetableService.generateUniqueName();

        assertNotEquals(a, b);
    }
}