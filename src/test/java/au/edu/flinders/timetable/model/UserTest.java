package au.edu.flinders.timetable.model;

import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserTest {

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("student-001", "Alice");
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    @Order(1)
    @DisplayName("TC X.01 – getUserId returns the user ID supplied at construction")
    @Tag("TC X.01")
    @Tag("Luke")
    @Tag("Core")
    void getUserIdReturnsConstructedId() {
        assertEquals("student-001", user.getUserId());
    }

    @Test
    @Order(2)
    @DisplayName("TC X.02 – getName returns the display name supplied at construction")
    @Tag("TC X.02")
    @Tag("Luke")
    @Tag("Core")
    void getNameReturnsConstructedName() {
        assertEquals("Alice", user.getName());
    }

    @Test
    @Order(3)
    @DisplayName("TC X.03 – getEnrolledTopics returns empty list before any enrolment")
    @Tag("TC X.03")
    @Tag("Luke")
    @Tag("Core")
    void getEnrolledTopicsEmptyBeforeEnrolment() {
        assertTrue(user.getEnrolledTopics().isEmpty());
    }

    @Test
    @Order(4)
    @DisplayName("TC X.04 – enrol adds course code to enrolled topics")
    @Tag("TC X.04")
    @Tag("Luke")
    @Tag("Core")
    void enrolAddsCourseCodeToEnrolledTopics() {
        user.enrol("COMP1001");

        assertTrue(user.isEnrolled("COMP1001"));
    }

    @Test
    @Order(5)
    @DisplayName("TC X.05 – enrol is a no-op when course code already enrolled")
    @Tag("TC X.05")
    @Tag("Luke")
    @Tag("Core")
    void enrolDuplicateCourseCodeIsNoOp() {
        user.enrol("COMP1001");
        user.enrol("COMP1001");

        assertEquals(1, user.getEnrolledTopics().size());
    }

    @Test
    @Order(6)
    @DisplayName("TC X.06 – withdraw removes course code from enrolled topics")
    @Tag("TC X.06")
    @Tag("Luke")
    @Tag("Core")
    void withdrawRemovesCourseCode() {
        user.enrol("COMP1001");
        user.withdraw("COMP1001");

        assertFalse(user.isEnrolled("COMP1001"));
    }

    @Test
    @Order(7)
    @DisplayName("TC X.07 – withdraw is a no-op when course code not enrolled")
    @Tag("TC X.07")
    @Tag("Luke")
    @Tag("Core")
    void withdrawNotEnrolledIsNoOp() {
        assertDoesNotThrow(() -> user.withdraw("COMP9999"));
        assertTrue(user.getEnrolledTopics().isEmpty());
    }

    @Test
    @Order(8)
    @DisplayName("TC X.08 – isEnrolled returns false when course code not enrolled")
    @Tag("TC X.08")
    @Tag("Luke")
    @Tag("Core")
    void isEnrolledReturnsFalseWhenNotEnrolled() {
        assertFalse(user.isEnrolled("COMP1001"));
    }

    @Test
    @Order(9)
    @DisplayName("TC X.09 – getEnrolledTopics returns unmodifiable list")
    @Tag("TC X.09")
    @Tag("Luke")
    @Tag("Core")
    void getEnrolledTopicsReturnsUnmodifiableList() {
        user.enrol("COMP1001");
        List<String> topics = user.getEnrolledTopics();

        assertThrows(UnsupportedOperationException.class, () -> topics.add("MATH1001"));
    }

    @Test
    @Order(10)
    @DisplayName("TC X.10 – getEnrolledTopics reflects multiple enrolments in order")
    @Tag("TC X.10")
    @Tag("Luke")
    @Tag("Core")
    void getEnrolledTopicsReflectsMultipleEnrolmentsInOrder() {
        user.enrol("COMP1001");
        user.enrol("MATH1001");
        user.enrol("PHYS1001");

        assertEquals(List.of("COMP1001", "MATH1001", "PHYS1001"), user.getEnrolledTopics());
    }

    @Test
    @Order(11)
    @DisplayName("TC X.11 – toString returns non-null string containing userId and name")
    @Tag("TC X.11")
    @Tag("Luke")
    @Tag("Core")
    void toStringContainsUserIdAndName() {
        String result = user.toString();

        assertAll(
                () -> assertNotNull(result),
                () -> assertTrue(result.contains("student-001")),
                () -> assertTrue(result.contains("Alice"))
        );
    }
}