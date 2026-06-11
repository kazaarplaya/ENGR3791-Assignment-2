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
    @DisplayName("TC 16.01 - getUserId returns the user ID supplied at construction")
    @Tag("Luke")
    @Tag("Core")
    void getUserIdReturnsConstructedId() {
        assertEquals("student-001", user.getUserId());
    }

    @Test
    @Order(2)
    @DisplayName("TC 16.02 - getName returns the display name supplied at construction")
    @Tag("Luke")
    @Tag("Core")
    void getNameReturnsConstructedName() {
        assertEquals("Alice", user.getName());
    }

    @Test
    @Order(3)
    @DisplayName("TC 16.03 - getEnrolledTopics returns empty list before any enrolment")
    @Tag("Luke")
    @Tag("Core")
    void getEnrolledTopicsEmptyBeforeEnrolment() {
        assertTrue(user.getEnrolledTopics().isEmpty());
    }

    @Test
    @Order(4)
    @DisplayName("TC 16.04 - enrol adds course code to enrolled topics")
    @Tag("Luke")
    @Tag("Core")
    void enrolAddsCourseCodeToEnrolledTopics() {
        user.enrol("COMP1001");

        assertTrue(user.isEnrolled("COMP1001"));
    }

    @Test
    @Order(5)
    @DisplayName("TC 16.05 - enrol is a no-op when course code already enrolled")
    @Tag("Luke")
    @Tag("Core")
    void enrolDuplicateCourseCodeIsNoOp() {
        user.enrol("COMP1001");
        user.enrol("COMP1001");

        assertEquals(1, user.getEnrolledTopics().size());
    }

    @Test
    @Order(6)
    @DisplayName("TC 16.06 - withdraw removes course code from enrolled topics")
    @Tag("Luke")
    @Tag("Core")
    void withdrawRemovesCourseCode() {
        user.enrol("COMP1001");
        user.withdraw("COMP1001");

        assertFalse(user.isEnrolled("COMP1001"));
    }

    @Test
    @Order(7)
    @DisplayName("TC 16.07 - withdraw is a no-op when course code not enrolled")
    @Tag("Luke")
    @Tag("Core")
    void withdrawNotEnrolledIsNoOp() {
        assertDoesNotThrow(() -> user.withdraw("COMP9999"));
        assertTrue(user.getEnrolledTopics().isEmpty());
    }

    @Test
    @Order(8)
    @DisplayName("TC 16.08 - isEnrolled returns false when course code not enrolled")
    @Tag("Luke")
    @Tag("Core")
    void isEnrolledReturnsFalseWhenNotEnrolled() {
        assertFalse(user.isEnrolled("COMP1001"));
    }

    @Test
    @Order(9)
    @DisplayName("TC 16.09 - getEnrolledTopics returns unmodifiable list")
    @Tag("Luke")
    @Tag("Core")
    void getEnrolledTopicsReturnsUnmodifiableList() {
        user.enrol("COMP1001");
        List<String> topics = user.getEnrolledTopics();

        assertThrows(UnsupportedOperationException.class, () -> topics.add("MATH1001"));
    }

    @Test
    @Order(10)
    @DisplayName("TC 16.10 - getEnrolledTopics reflects multiple enrolments in order")
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
    @DisplayName("TC 16.11 - toString returns non-null string containing userId and name")
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
