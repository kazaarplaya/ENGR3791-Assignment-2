package au.edu.flinders.timetable.model;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TopicTest {

    private Topic topic;

    @BeforeEach
    void setUp() {
        topic = new Topic("COMP1001", "Computing Fundamentals", "Bedford Park",
                1, "Internal", 3, "In person");
    }

    @AfterEach
    void tearDown() {}


    @Test
    @Order(1)
    @DisplayName("TC 15.01 - getCourseCode returns constructed course code")
    @Tag("Luke")
    @Tag("Core")
    void getCourseCodeReturnsConstructedValue() {
        assertEquals("COMP1001", topic.getCourseCode());
    }

    @Test
    @Order(2)
    @DisplayName("TC 15.02 - getTopicName returns constructed topic name")
    @Tag("Luke")
    @Tag("Core")
    void getTopicNameReturnsConstructedValue() {
        assertEquals("Computing Fundamentals", topic.getTopicName());
    }

    @Test
    @Order(3)
    @DisplayName("TC 15.03 - getCampus returns constructed campus")
    @Tag("Luke")
    @Tag("Core")
    void getCampusReturnsConstructedValue() {
        assertEquals("Bedford Park", topic.getCampus());
    }

    @Test
    @Order(4)
    @DisplayName("TC 15.04 - getSemester returns constructed semester")
    @Tag("Luke")
    @Tag("Core")
    void getSemesterReturnsConstructedValue() {
        assertEquals(1, topic.getSemester());
    }

    @Test
    @Order(5)
    @DisplayName("TC 15.05 - getDelivery returns constructed delivery mode")
    @Tag("Luke")
    @Tag("Core")
    void getDeliveryReturnsConstructedValue() {
        assertEquals("Internal", topic.getDelivery());
    }

    @Test
    @Order(6)
    @DisplayName("TC 15.06 - getNumOfClasses returns constructed number of classes")
    @Tag("Luke")
    @Tag("Core")
    void getNumOfClassesReturnsConstructedValue() {
        assertEquals(3, topic.getNumOfClasses());
    }

    @Test
    @Order(7)
    @DisplayName("TC 15.07 - getAttendanceMode returns constructed attendance mode")
    @Tag("Luke")
    @Tag("Core")
    void getAttendanceModeReturnsConstructedValue() {
        assertEquals("In person", topic.getAttendanceMode());
    }

    @Test
    @Order(8)
    @DisplayName("TC 15.08 - null topicName defaults to empty string")
    @Tag("Luke")
    @Tag("Core")
    void nullTopicNameDefaultsToEmptyString() {
        Topic t = new Topic("COMP1001", null, "Bedford Park", 1, "Internal", 1, "In person");
        assertEquals("", t.getTopicName());
    }

    @Test
    @Order(9)
    @DisplayName("TC 15.09 - null attendanceMode defaults to empty string")
    @Tag("Luke")
    @Tag("Core")
    void nullAttendanceModeDefaultsToEmptyString() {
        Topic t = new Topic("COMP1001", "Computing", "Bedford Park", 1, "Internal", 1, null);
        assertEquals("", t.getAttendanceMode());
    }

    @Test
    @Order(10)
    @DisplayName("TC 15.10 - null courseCode throws IllegalArgumentException")
    @Tag("Luke")
    @Tag("Core")
    void nullCourseCodeThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> new Topic(null, "Computing", "Bedford Park", 1, "Internal", 1, "In person"));
    }

    @Test
    @Order(11)
    @DisplayName("TC 15.11 - blank courseCode throws IllegalArgumentException")
    @Tag("Luke")
    @Tag("Core")
    void blankCourseCodeThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> new Topic("   ", "Computing", "Bedford Park", 1, "Internal", 1, "In person"));
    }

    @Test
    @Order(12)
    @DisplayName("TC 15.12 - 6-arg constructor sets attendanceMode to empty string")
    @Tag("Luke")
    @Tag("Core")
    void sixArgConstructorSetsAttendanceModeToEmptyString() {
        Topic t = new Topic("COMP1001", "Computing", "Bedford Park", 1, "Internal", 2);
        assertEquals("", t.getAttendanceMode());
    }

    @Test
    @Order(13)
    @DisplayName("TC 15.13 - equals returns true for same course code")
    @Tag("Luke")
    @Tag("Core")
    void equalsReturnsTrueForSameCourseCode() {
        Topic other = new Topic("COMP1001", "Different Name", "Tonsley", 2, "External", 1, "Online");
        assertEquals(topic, other);
    }

    @Test
    @Order(14)
    @DisplayName("TC 15.14 - equals returns false for different course code")
    @Tag("Luke")
    @Tag("Core")
    void equalsReturnsFalseForDifferentCourseCode() {
        Topic other = new Topic("MATH1001", "Calculus", "Bedford Park", 1, "Internal", 2, "In person");
        assertNotEquals(topic, other);
    }

    @Test
    @Order(15)
    @DisplayName("TC 15.15 - equals returns true for same instance")
    @Tag("Luke")
    @Tag("Core")
    void equalsReturnsTrueForSameInstance() {
        assertEquals(topic, topic);
    }

    @Test
    @Order(16)
    @DisplayName("TC 15.16 - equals returns false for non-Topic object")
    @Tag("Luke")
    @Tag("Core")
    void equalsReturnsFalseForNonTopicObject() {
        assertNotEquals(topic, "COMP1001");
    }

    @Test
    @Order(17)
    @DisplayName("TC 15.17 - equal topics have same hash code")
    @Tag("Luke")
    @Tag("Core")
    void equalTopicsHaveSameHashCode() {
        Topic other = new Topic("COMP1001", "Other Name", "Tonsley", 2, "External", 1);
        assertEquals(topic.hashCode(), other.hashCode());
    }


    @Test
    @Order(18)
    @DisplayName("TC 15.18 - toString contains courseCode, topicName, campus and semester")
    @Tag("Luke")
    @Tag("Core")
    void toStringContainsAllKeyFields() {
        String s = topic.toString();
        assertAll(
                () -> assertTrue(s.contains("COMP1001")),
                () -> assertTrue(s.contains("Computing Fundamentals")),
                () -> assertTrue(s.contains("Bedford Park")),
                () -> assertTrue(s.contains("Semester 1"))
        );
    }
}
