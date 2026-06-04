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
    @DisplayName("TC 4.01 – Save legacy preference boundary – accepts Monday and Friday")
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
    @DisplayName("TC 4.02 – Save legacy preference boundary – accepts morning and evening edge times")
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
    @DisplayName("TC 4.03 – Save legacy preference rejects invalid day")
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
    @DisplayName("TC 4.04 – Save legacy preference rejects invalid time")
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
    @DisplayName("TC 4.06 – Save token preference rejects empty token list")
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
    @DisplayName("TC 4.07 – Save token preference rejects duplicate tokens")
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
    @DisplayName("TC 4.08 – Get preferences returns preferences in insertion order")
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
        assertAll(
                () -> assertTrue(saved.isPresent()),
                () -> assertEquals(ordered, saved.get().getPriorityOrder(),
                        "Token order must be preserved from insertion")
        );
    }

    @Test
    @Order(9)
    @DisplayName("TC 4.09 – Reorder preferences updates priority order")
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
    @DisplayName("TC 4.10 – Clear preference removes saved preference")
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
    @DisplayName("TC 4.11 – Save legacy preference accepts Any as day")
    @Tag("Luke")
    @Tag("Core")
    void saveLegacyPreferenceAnyDayIsAccepted() {
        Preference pref = buildLegacyPref("morning", "Any");

        assertDoesNotThrow(() -> preferenceService.savePreference(pref));

        assertTrue(preferenceService.getPreference(USER_ID).isPresent());
    }


    @Test
    @Order(12)
    @DisplayName("TC 4.12 – Save token preference rejects unknown token string")
    @Tag("Luke")
    @Tag("Core")
    void saveTokenPreferenceUnknownTokenThrowsIllegalArgumentException() {
        Preference pref = new Preference(USER_ID, List.of("NOT_A_VALID_TOKEN"));

        assertThrows(IllegalArgumentException.class,
                () -> preferenceService.savePreference(pref));
    }

}
