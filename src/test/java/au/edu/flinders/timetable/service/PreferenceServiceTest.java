package au.edu.flinders.timetable.service;

import au.edu.flinders.timetable.model.Preference;
import au.edu.flinders.timetable.repository.PreferenceRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.Assumptions.assumingThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PreferenceServiceTest {

    private static final String USER_ID = "student-001";

    private PreferenceRepository preferenceRepository;
    private PreferenceService preferenceService;

    @BeforeAll
    static void suiteSetUp() {

    }

    @BeforeEach
    void setUp() {
        preferenceRepository = new PreferenceRepository();
        preferenceService = new PreferenceService(preferenceRepository);
    }

    @AfterEach
    void tearDown() {
        preferenceService.clearPreference(USER_ID);
    }

    @AfterAll
    static void suitTearDown() {

    }

    // Helper function builds legacy-style Preference using 7-argument constructor
    private Preference buildLegacyPref(String timeOfDay, String day) {
        return new Preference(USER_ID, "Bedford Park", timeOfDay, day, 1, 2, 3);
    }

    // Helper function builds token-style Preference using 2-argument constructor
    private Preference buildTokenPref(List<String> tokens) {
        return new Preference(USER_ID, tokens);
    }

    @ParameterizedTest(name = "day=''{1}'' should be accepted")
    @Order(1)
    @DisplayName("TC 4.01 - Save legacy preference boundary – accepts Monday and Friday")
    @Tag("Luke")
    @Tag("Core")
    @CsvSource({"morning, Monday", "afternoon, Friday"})
    void saveLegacyPreferenceBoundaryDaysIsAccepted(String time, String day) {
        Preference pref = buildLegacyPref(time, day);

        assertDoesNotThrow(() -> preferenceService.savePreference(pref));

        Optional<Preference> saved = preferenceService.getPreference(USER_ID);
        assertTrue(saved.isPresent(), "Preference for boundary day '" + day + "' should be saved");
    }

    @ParameterizedTest(name = "time=''{0}'' should be accepted")
    @Order(2)
    @DisplayName("TC 4.02 - Save legacy preference boundary – accepts morning and evening edge times")
    @Tag("Luke")
    @Tag("Core")
    @CsvSource({"morning, Monday", "evening, Friday"})
    void saveLegacyPreferenceBoundaryTimesIsAccepted(String time, String day) {
        Preference pref = buildLegacyPref(time, day);

        assertDoesNotThrow(() -> preferenceService.savePreference(pref));

        Optional<Preference> saved = preferenceService.getPreference(USER_ID);
        assertTrue(saved.isPresent(), "Preference for boundary time '" + time + "' should be saved");
    }

    @ParameterizedTest(name = "day=''{0}'' should be rejected")
    @Order(3)
    @DisplayName("TC 4.03 - Save legacy preference rejects invalid day")
    @Tag("Luke")
    @Tag("Core")
    @ValueSource(strings = {"Someday", "Mon", "1", "MONDAY!", ""})
    void saveLegacyPreferenceInvalidDayThrowsIllegalArgumentException(String invalidDay) {
        Preference pref = buildLegacyPref("morning", invalidDay);

        assertThrows(IllegalArgumentException.class,
                () -> preferenceService.savePreference(pref));
    }

    @ParameterizedTest(name = "time=''{0}'' should be rejected")
    @Order(4)
    @DisplayName("TC 4.04 - Save legacy preference rejects invalid time")
    @Tag("Luke")
    @Tag("Core")
    @ValueSource(strings = {"dawn", "midnight", "24:00", "", "null"})
    void saveLegacyPreferenceInvalidTimeThrowsIllegalArgumentException(String invalidTime) {
        Preference pref = buildLegacyPref(invalidTime, "Monday");

        assertThrows(IllegalArgumentException.class,
                () -> preferenceService.savePreference(pref));
    }

    @ParameterizedTest(name = "token=''{0}'' should be accepted")
    @Order(5)
    @DisplayName("TC 4.05 – Save token preference accepts valid tokens")
    @Tag("Luke")
    @Tag("Core")
    @ValueSource(strings = {
            "CAMPUS_BEDFORD_PARK", "CAMPUS_TONSLEY", "CAMPUS_CITY", "CAMPUS_SAME",
            "TIME_MORNING", "TIME_AFTERNOON", "TIME_EVENING",
            "DAY_MONDAY", "DAY_TUESDAY", "DAY_WEDNESDAY", "DAY_THURSDAY", "DAY_FRIDAY",
            "SPREAD", "COMPACT"
    })
    void saveTokenPreferenceEachValidTokenIsSaved(String token) {
        Preference pref = buildTokenPref(List.of(token));

        assertDoesNotThrow(() -> preferenceService.savePreference(pref));

        Optional<Preference> saved = preferenceService.getPreference(USER_ID);
        assumeTrue(saved.isPresent(), "Preference must be saved before asserting tokens");
        assertTrue(saved.get().getPriorityOrder().contains(token));
    }

    @Test
    @Order(6)
    @DisplayName("TC 4.06 - Save token preference rejects empty token list")
    @Tag("Luke")
    @Tag("Core")
    void saveTokenPreferenceEmptyListSavedWithNoTokens() {
        Preference pref = buildTokenPref(List.of());

        preferenceService.savePreference(pref);

        Optional<Preference> saved = preferenceService.getPreference(USER_ID);
        assertAll(
                () -> assertTrue(saved.isPresent()),
                () -> assertFalse(saved.get().hasAnyCriteria(),
                        "Preference with empty token list should have no active criteria")
        );
    }

    @Test
    @Order(7)
    @DisplayName("TC 4.07 - Save token preference rejects duplicate tokens")
    @Tag("Luke")
    @Tag("Core")
    void saveTokenPreferenceDuplicateTokensThrowsIllegalArgumentException() {
        List<String> tokens = List.of(Preference.TIME_MORNING, Preference.TIME_MORNING);
        Preference pref = buildTokenPref(tokens);

        assertThrows(IllegalArgumentException.class,
                () -> preferenceService.savePreference(pref));
    }

    @Test
    @Order(8)
    @DisplayName("TC 4.08 - Get preferences returns preferences in insertion order")
    @Tag("Luke")
    @Tag("Core")
    void getPreferenceTokenOrderMatchesInsertionOrder() {
        List<String> ordered = List.of(
                Preference.TIME_MORNING,
                Preference.CAMPUS_BEDFORD_PARK,
                Preference.COMPACT
        );
        preferenceService.savePreference(buildTokenPref(ordered));

        Optional<Preference> saved = preferenceService.getPreference(USER_ID);
        assertTrue(saved.isPresent());
        assertEquals(ordered, saved.get().getPriorityOrder(),
    "Token order must be preserved from insertion");
    }

    @Test
    @Order(9)
    @DisplayName("TC 4.09 - Reorder preferences updates priority order")
    @Tag("Luke")
    @Tag("Core")
    void reorderPreferencesNewTokenOrderIsReflectedAfterSave() {
        List<String> initial   = List.of(Preference.TIME_MORNING, Preference.COMPACT);
        List<String> reordered = List.of(Preference.COMPACT, Preference.TIME_MORNING);

        preferenceService.savePreference(buildTokenPref(initial));
        assumingThat(
                preferenceService.getPreference(USER_ID).isPresent(),
                () -> {
                    preferenceService.savePreference(buildTokenPref(reordered));

                    Optional<Preference> updated = preferenceService.getPreference(USER_ID);
                    assertAll(
                            () -> assertTrue(updated.isPresent()),
                            () -> assertEquals(reordered, updated.get().getPriorityOrder(),
                                    "Priority order should reflect the updated token sequence")
                    );
                }
        );
    }

    @Test
    @Order(10)
    @DisplayName("TC 4.10 - Clear preference removes saved preference")
    @Tag("Luke")
    @Tag("Core")
    void clearPreferenceAfterSavePreferenceIsAbsent() {
        Preference pref = buildLegacyPref("morning", "Monday");
        preferenceService.savePreference(pref);

        assumeTrue(preferenceService.getPreference(USER_ID).isPresent(),
                "Precondition: preference must be saved before clear test runs");

        preferenceService.clearPreference(USER_ID);

        assertTrue(preferenceService.getPreference(USER_ID).isEmpty(),
                "Preference should be absent after clear");
    }


    @Test
    @Order(11)
    @DisplayName("TC 4.11 - Save legacy preference accepts Any as day")
    @Tag("Luke")
    @Tag("Core")
    void saveLegacyPreferenceAnyDayIsAccepted() {
        Preference pref = buildLegacyPref("morning", "Any");

        assertDoesNotThrow(() -> preferenceService.savePreference(pref));

        assertTrue(preferenceService.getPreference(USER_ID).isPresent());
    }


    @Test
    @Order(12)
    @DisplayName("TC 4.12 - Save token preference rejects unknown token string")
    @Tag("Luke")
    @Tag("Core")
    void saveTokenPreferenceUnknownTokenThrowsIllegalArgumentException() {
        Preference pref = new Preference(USER_ID, List.of("NOT_A_VALID_TOKEN"));

        assertThrows(IllegalArgumentException.class,
                () -> preferenceService.savePreference(pref));
    }


    @Test
    @Order(13)
    @DisplayName("TC 4.13 - Legacy preference with Tonsley campus converts to CAMPUS_TONSLEY token")
    @Tag("Luke")
    @Tag("Core")
    void legacyPreferenceTonslyCampusConvertsToToken() {
        Preference pref = new Preference(USER_ID, "Tonsley", "morning", "Monday", 1, 2, 3);
        preferenceService.savePreference(pref);

        Preference saved = preferenceService.getPreference(USER_ID).get();
        assertTrue(saved.getPriorityOrder().contains(Preference.CAMPUS_TONSLEY));
    }

    @Test
    @Order(14)
    @DisplayName("TC 4.14 - Legacy preference with City campus converts to CAMPUS_CITY token")
    @Tag("Luke")
    @Tag("Core")
    void legacyPreferenceCityCampusConvertsToToken() {
        Preference pref = new Preference(USER_ID, "City", "morning", "Monday", 1, 2, 3);
        preferenceService.savePreference(pref);

        Preference saved = preferenceService.getPreference(USER_ID).get();
        assertTrue(saved.getPriorityOrder().contains(Preference.CAMPUS_CITY));
    }

    @Test
    @Order(15)
    @DisplayName("TC 4.15 - Legacy preference with afternoon time converts to TIME_AFTERNOON token")
    @Tag("Luke")
    @Tag("Core")
    void legacyPreferenceAfternoonTimeConvertsToToken() {
        Preference pref = new Preference(USER_ID, "Bedford Park", "afternoon", "Monday", 1, 2, 3);
        preferenceService.savePreference(pref);

        Preference saved = preferenceService.getPreference(USER_ID).get();
        assertTrue(saved.getPriorityOrder().contains(Preference.TIME_AFTERNOON));
    }

    @Test
    @Order(16)
    @DisplayName("TC 4.16 - Legacy preference with evening time converts to TIME_EVENING token")
    @Tag("Luke")
    @Tag("Core")
    void legacyPreferenceEveningTimeConvertsToToken() {
        Preference pref = new Preference(USER_ID, "Bedford Park", "evening", "Monday", 1, 2, 3);
        preferenceService.savePreference(pref);

        Preference saved = preferenceService.getPreference(USER_ID).get();
        assertTrue(saved.getPriorityOrder().contains(Preference.TIME_EVENING));
    }

    @Test
    @Order(18)
    @DisplayName("TC 4.18 - Legacy preference getters return correct values")
    @Tag("Luke")
    @Tag("Core")
    void legacyPreferenceGettersReturnCorrectValues() {
        Preference pref = new Preference(USER_ID, "Bedford Park", "morning", "Monday", 1, 2, 3);

        assertAll(
                () -> assertEquals(USER_ID, pref.getUserId()),
                () -> assertEquals("Bedford Park", pref.getCampus()),
                () -> assertEquals("morning", pref.getTimeOfDay()),
                () -> assertEquals("Monday", pref.getDay()),
                () -> assertEquals(1, pref.getCampusPriority()),
                () -> assertEquals(2, pref.getTimeOfDayPriority()),
                () -> assertEquals(3, pref.getDayPriority())
        );
    }

    @Test
    @Order(19)
    @DisplayName("TC 4.19 - toString returns non-null readable summary")
    @Tag("Luke")
    @Tag("Core")
    void toStringReturnsNonNullSummary() {
        Preference pref = new Preference(USER_ID, List.of(Preference.TIME_MORNING));

        assertNotNull(pref.toString());
        assertTrue(pref.toString().contains(USER_ID));
    }


    @Test
    @Order(20)
    @DisplayName("TC 4.20 - Legacy preference with null campus defaults to Any")
    @Tag("Luke")
    @Tag("Core")
    void legacyPreferenceNullCampusDefaultsToAny() {
        Preference pref = new Preference(USER_ID, null, "morning", "Monday", 1, 2, 3);
        preferenceService.savePreference(pref);

        Preference saved = preferenceService.getPreference(USER_ID).get();
        assertAll(
                () -> assertEquals("Any", saved.getCampus()),
                () -> assertFalse(saved.getPriorityOrder().contains(Preference.CAMPUS_BEDFORD_PARK),
                        "Any campus should not add a campus token")
        );
    }

    @Test
    @Order(21)
    @DisplayName("TC 4.21 - Legacy preference with null timeOfDay defaults to Any")
    @Tag("Luke")
    @Tag("Core")
    void legacyPreferenceNullTimeOfDayDefaultsToAny() {
        Preference pref = new Preference(USER_ID, "Bedford Park", null, "Monday", 1, 2, 3);
        preferenceService.savePreference(pref);

        Preference saved = preferenceService.getPreference(USER_ID).get();
        assertAll(
                () -> assertEquals("Any", saved.getTimeOfDay()),
                () -> assertFalse(saved.getPriorityOrder().contains(Preference.TIME_MORNING),
                        "Any timeOfDay should not add a time token")
        );
    }

    @Test
    @Order(22)
    @DisplayName("TC 4.22 - Legacy preference with null day defaults to Any")
    @Tag("Luke")
    @Tag("Core")
    void legacyPreferenceNullDayDefaultsToAny() {
        Preference pref = new Preference(USER_ID, "Bedford Park", "morning", null, 1, 2, 3);
        preferenceService.savePreference(pref);

        Preference saved = preferenceService.getPreference(USER_ID).get();
        assertAll(
                () -> assertEquals("Any", saved.getDay()),
                () -> assertFalse(saved.getPriorityOrder().contains(Preference.DAY_MONDAY),
                        "Any day should not add a day token")
        );
    }

}
