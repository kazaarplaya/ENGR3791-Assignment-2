package au.edu.flinders.timetable.model;

import org.junit.jupiter.api.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ClassEntryTest {

    private static final String ID   = "COMP1001-LEC-1-MON-0900-01Mar";
    private static final String CODE = "COMP1001";

    private ClassEntry make() {
        return new ClassEntry(ID, "Lecture", LocalDate.of(2025, 3, 1),
                LocalTime.of(9, 0), LocalTime.of(11, 0),
                DayOfWeek.MONDAY, "Registry", "R101",
                CODE, "In person", 2, 1, "01 Mar", "30 Mar");
    }


    @Test
    @Order(1)
    @DisplayName("TC X.01 – getClassId returns constructed ID")
    @Tag("Luke")
    @Tag("Core")
    void getClassIdReturnsConstructedId() {
        assertEquals(ID, make().getClassId());
    }

    @Test
    @Order(2)
    @DisplayName("TC X.02 – getType returns constructed type")
    @Tag("Luke")
    @Tag("Core")
    void getTypeReturnsConstructedType() {
        assertEquals("Lecture", make().getType());
    }

    @Test
    @Order(3)
    @DisplayName("TC X.03 – getDate returns constructed date")
    @Tag("Luke")
    @Tag("Core")
    void getDateReturnsConstructedDate() {
        assertEquals(LocalDate.of(2025, 3, 1), make().getDate());
    }

    @Test
    @Order(4)
    @DisplayName("TC X.04 – getStartTime returns constructed start time")
    @Tag("Luke")
    @Tag("Core")
    void getStartTimeReturnsConstructedStartTime() {
        assertEquals(LocalTime.of(9, 0), make().getStartTime());
    }

    @Test
    @Order(5)
    @DisplayName("TC X.05 – getEndTime returns constructed end time")
    @Tag("Luke")
    @Tag("Core")
    void getEndTimeReturnsConstructedEndTime() {
        assertEquals(LocalTime.of(11, 0), make().getEndTime());
    }

    @Test
    @Order(6)
    @DisplayName("TC X.06 – getDay returns constructed day")
    @Tag("Luke")
    @Tag("Core")
    void getDayReturnsConstructedDay() {
        assertEquals(DayOfWeek.MONDAY, make().getDay());
    }

    @Test
    @Order(7)
    @DisplayName("TC X.07 – getBuilding returns constructed building")
    @Tag("Luke")
    @Tag("Core")
    void getBuildingReturnsConstructedBuilding() {
        assertEquals("Registry", make().getBuilding());
    }

    @Test
    @Order(8)
    @DisplayName("TC X.08 – getRoom returns constructed room")
    @Tag("Luke")
    @Tag("Core")
    void getRoomReturnsConstructedRoom() {
        assertEquals("R101", make().getRoom());
    }

    @Test
    @Order(9)
    @DisplayName("TC X.09 – getCourseCode returns constructed course code")
    @Tag("Luke")
    @Tag("Core")
    void getCourseCodeReturnsConstructedCourseCode() {
        assertEquals(CODE, make().getCourseCode());
    }

    @Test
    @Order(10)
    @DisplayName("TC X.10 – getAttendanceMode returns constructed attendance mode")
    @Tag("Luke")
    @Tag("Core")
    void getAttendanceModeReturnsConstructedMode() {
        assertEquals("In person", make().getAttendanceMode());
    }

    @Test
    @Order(11)
    @DisplayName("TC X.11 – getAvailabilityNumber returns constructed availability number")
    @Tag("Luke")
    @Tag("Core")
    void getAvailabilityNumberReturnsConstructedValue() {
        assertEquals(2, make().getAvailabilityNumber());
    }

    @Test
    @Order(12)
    @DisplayName("TC X.12 – getClassInstance returns constructed class instance")
    @Tag("Luke")
    @Tag("Core")
    void getClassInstanceReturnsConstructedValue() {
        assertEquals(1, make().getClassInstance());
    }

    @Test
    @Order(13)
    @DisplayName("TC X.13 – getDateFrom returns constructed dateFrom")
    @Tag("Luke")
    @Tag("Core")
    void getDateFromReturnsConstructedValue() {
        assertEquals("01 Mar", make().getDateFrom());
    }

    @Test
    @Order(14)
    @DisplayName("TC X.14 – getDateTo returns constructed dateTo")
    @Tag("Luke")
    @Tag("Core")
    void getDateToReturnsConstructedValue() {
        assertEquals("30 Mar", make().getDateTo());
    }


    @Test
    @Order(15)
    @DisplayName("TC X.15 – null attendanceMode defaults to empty string")
    @Tag("Luke")
    @Tag("Core")
    void nullAttendanceModeDefaultsToEmptyString() {
        ClassEntry e = new ClassEntry(ID, "Lecture", null,
                LocalTime.of(9, 0), LocalTime.of(11, 0),
                DayOfWeek.MONDAY, "T1", "G42", CODE,
                null, 1, 1, "01 Mar", "01 Mar");
        assertEquals("", e.getAttendanceMode());
    }

    @Test
    @Order(16)
    @DisplayName("TC X.16 – null dateFrom defaults to empty string")
    @Tag("Luke")
    @Tag("Core")
    void nullDateFromDefaultsToEmptyString() {
        ClassEntry e = new ClassEntry(ID, "Lecture", null,
                LocalTime.of(9, 0), LocalTime.of(11, 0),
                DayOfWeek.MONDAY, "T1", "G42", CODE,
                "In person", 1, 1, null, null);
        assertEquals("", e.getDateFrom());
        assertEquals("", e.getDateTo());
    }


    @Test
    @Order(17)
    @DisplayName("TC X.17 – 9-arg constructor sets extended fields to defaults")
    @Tag("Luke")
    @Tag("Core")
    void nineArgConstructorSetsExtendedFieldsToDefaults() {
        ClassEntry e = new ClassEntry(ID, "Lecture", null,
                LocalTime.of(9, 0), LocalTime.of(11, 0),
                DayOfWeek.MONDAY, "T1", "G42", CODE);
        assertAll(
                () -> assertEquals("", e.getAttendanceMode()),
                () -> assertEquals(0, e.getAvailabilityNumber()),
                () -> assertEquals(0, e.getClassInstance()),
                () -> assertEquals("", e.getDateFrom()),
                () -> assertEquals("", e.getDateTo())
        );
    }


    @Test
    @Order(18)
    @DisplayName("TC X.18 – durationMinutes returns correct duration")
    @Tag("Luke")
    @Tag("Core")
    void durationMinutesReturnsCorrectDuration() {
        assertEquals(120, make().durationMinutes());
    }

    @Test
    @Order(19)
    @DisplayName("TC X.19 – durationMinutes returns 30 for half-hour class")
    @Tag("Luke")
    @Tag("Core")
    void durationMinutesReturns30ForHalfHourClass() {
        ClassEntry e = new ClassEntry(ID, "Tutorial", null,
                LocalTime.of(10, 0), LocalTime.of(10, 30),
                DayOfWeek.TUESDAY, "T1", "G42", CODE);
        assertEquals(30, e.durationMinutes());
    }


    @Test
    @Order(20)
    @DisplayName("TC X.20 – isLecture returns true for Lecture type")
    @Tag("Luke")
    @Tag("Core")
    void isLectureReturnsTrueForLectureType() {
        assertTrue(make().isLecture());
    }

    @Test
    @Order(21)
    @DisplayName("TC X.21 – isLecture returns true case-insensitively")
    @Tag("Luke")
    @Tag("Core")
    void isLectureReturnsTrueCaseInsensitively() {
        ClassEntry e = new ClassEntry(ID, "LECTURE", null,
                LocalTime.of(9, 0), LocalTime.of(11, 0),
                DayOfWeek.MONDAY, "T1", "G42", CODE);
        assertTrue(e.isLecture());
    }

    @Test
    @Order(22)
    @DisplayName("TC X.22 – isLecture returns false for non-Lecture type")
    @Tag("Luke")
    @Tag("Core")
    void isLectureReturnsFalseForTutorial() {
        ClassEntry e = new ClassEntry(ID, "Tutorial", null,
                LocalTime.of(9, 0), LocalTime.of(11, 0),
                DayOfWeek.MONDAY, "T1", "G42", CODE);
        assertFalse(e.isLecture());
    }


    @Test
    @Order(23)
    @DisplayName("TC X.23 – toString contains classId and courseCode")
    @Tag("Luke")
    @Tag("Core")
    void toStringContainsClassIdAndCourseCode() {
        String s = make().toString();
        assertAll(
                () -> assertNotNull(s),
                () -> assertTrue(s.contains(ID)),
                () -> assertTrue(s.contains(CODE))
        );
    }

    @Test
    @Order(24)
    @DisplayName("TC X.24 – toString shows recurring when date is null")
    @Tag("Luke")
    @Tag("Core")
    void toStringShowsRecurringWhenDateNull() {
        ClassEntry e = new ClassEntry(ID, "Lecture", null,
                LocalTime.of(9, 0), LocalTime.of(11, 0),
                DayOfWeek.MONDAY, "T1", "G42", CODE);
        assertTrue(e.toString().contains("recurring"));
    }


    @Test
    @Order(25)
    @DisplayName("TC X.25 – fromCsvRow parses valid 9-column row")
    @Tag("Luke")
    @Tag("Core")
    void fromCsvRowParsesValid9ColumnRow() {
        String[] cols = {ID, "Lecture", "", "09:00", "11:00", "MONDAY", "Registry", "R101", CODE};
        ClassEntry e = ClassEntry.fromCsvRow(cols);
        assertAll(
                () -> assertEquals(ID, e.getClassId()),
                () -> assertEquals("Lecture", e.getType()),
                () -> assertNull(e.getDate()),
                () -> assertEquals(LocalTime.of(9, 0), e.getStartTime()),
                () -> assertEquals(DayOfWeek.MONDAY, e.getDay()),
                () -> assertEquals(CODE, e.getCourseCode())
        );
    }

    @Test
    @Order(26)
    @DisplayName("TC X.26 – fromCsvRow parses valid 14-column row with extended fields")
    @Tag("Luke")
    @Tag("Core")
    void fromCsvRowParsesValid14ColumnRow() {
        String[] cols = {ID, "Lecture", "2025-03-01", "09:00", "11:00", "MONDAY",
                "Registry", "R101", CODE, "In person", "2", "1", "01 Mar", "30 Mar"};
        ClassEntry e = ClassEntry.fromCsvRow(cols);
        assertAll(
                () -> assertEquals(LocalDate.of(2025, 3, 1), e.getDate()),
                () -> assertEquals("In person", e.getAttendanceMode()),
                () -> assertEquals(2, e.getAvailabilityNumber()),
                () -> assertEquals(1, e.getClassInstance()),
                () -> assertEquals("01 Mar", e.getDateFrom()),
                () -> assertEquals("30 Mar", e.getDateTo())
        );
    }

    @Test
    @Order(27)
    @DisplayName("TC X.27 – fromCsvRow throws for fewer than 9 columns")
    @Tag("Luke")
    @Tag("Core")
    void fromCsvRowThrowsForTooFewColumns() {
        assertThrows(IllegalArgumentException.class,
                () -> ClassEntry.fromCsvRow(new String[]{"a", "b", "c"}));
    }

    @Test
    @Order(28)
    @DisplayName("TC X.28 – fromCsvRow throws for invalid date format")
    @Tag("Luke")
    @Tag("Core")
    void fromCsvRowThrowsForInvalidDate() {
        String[] cols = {ID, "Lecture", "not-a-date", "09:00", "11:00", "MONDAY", "T1", "G42", CODE};
        assertThrows(IllegalArgumentException.class, () -> ClassEntry.fromCsvRow(cols));
    }

    @Test
    @Order(29)
    @DisplayName("TC X.29 – fromCsvRow throws for invalid startTime format")
    @Tag("Luke")
    @Tag("Core")
    void fromCsvRowThrowsForInvalidStartTime() {
        String[] cols = {ID, "Lecture", "", "9am", "11:00", "MONDAY", "T1", "G42", CODE};
        assertThrows(IllegalArgumentException.class, () -> ClassEntry.fromCsvRow(cols));
    }

    @Test
    @Order(30)
    @DisplayName("TC X.30 – fromCsvRow throws for invalid endTime format")
    @Tag("Luke")
    @Tag("Core")
    void fromCsvRowThrowsForInvalidEndTime() {
        String[] cols = {ID, "Lecture", "", "09:00", "11pm", "MONDAY", "T1", "G42", CODE};
        assertThrows(IllegalArgumentException.class, () -> ClassEntry.fromCsvRow(cols));
    }

    @Test
    @Order(31)
    @DisplayName("TC X.31 – fromCsvRow throws for invalid day value")
    @Tag("Luke")
    @Tag("Core")
    void fromCsvRowThrowsForInvalidDay() {
        String[] cols = {ID, "Lecture", "", "09:00", "11:00", "SOMEDAY", "T1", "G42", CODE};
        assertThrows(IllegalArgumentException.class, () -> ClassEntry.fromCsvRow(cols));
    }

    @Test
    @Order(32)
    @DisplayName("TC X.32 – fromCsvRow throws for blank courseCode")
    @Tag("Luke")
    @Tag("Core")
    void fromCsvRowThrowsForBlankCourseCode() {
        String[] cols = {ID, "Lecture", "", "09:00", "11:00", "MONDAY", "T1", "G42", "  "};
        assertThrows(IllegalArgumentException.class, () -> ClassEntry.fromCsvRow(cols));
    }

    @Test
    @Order(33)
    @DisplayName("TC X.33 – fromCsvRow ignores non-numeric availabilityNumber and classInstance")
    @Tag("Luke")
    @Tag("Core")
    void fromCsvRowIgnoresNonNumericExtendedIntegers() {
        String[] cols = {ID, "Lecture", "", "09:00", "11:00", "MONDAY",
                "T1", "G42", CODE, "In person", "NaN", "NaN", "", ""};
        ClassEntry e = ClassEntry.fromCsvRow(cols);
        assertAll(
                () -> assertEquals(0, e.getAvailabilityNumber()),
                () -> assertEquals(0, e.getClassInstance())
        );
    }

    @Test
    @Order(34)
    @DisplayName("TC X.34 – fromCsvRow treats empty availabilityNumber column as zero")
    @Tag("Luke")
    @Tag("Core")
    void fromCsvRowEmptyAvailabilityNumberDefaultsToZero() {
        // cols.length > 10, but cols[10] is blank → isEmpty() true → skip parseInt
        String[] cols = {ID, "Lecture", "", "09:00", "11:00", "MONDAY",
                "T1", "G42", CODE, "In person", "", "", "", ""};
        ClassEntry e = ClassEntry.fromCsvRow(cols);
        assertEquals(0, e.getAvailabilityNumber());
    }

    @Test
    @Order(35)
    @DisplayName("TC X.35 – fromCsvRow treats empty classInstance column as zero")
    @Tag("Luke")
    @Tag("Core")
    void fromCsvRowEmptyClassInstanceDefaultsToZero() {
        // cols.length > 11, but cols[11] is blank → isEmpty() true → skip parseInt
        String[] cols = {ID, "Lecture", "", "09:00", "11:00", "MONDAY",
                "T1", "G42", CODE, "In person", "2", "", "", ""};
        ClassEntry e = ClassEntry.fromCsvRow(cols);
        assertEquals(0, e.getClassInstance());
    }
}