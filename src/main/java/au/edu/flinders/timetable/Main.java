package au.edu.flinders.timetable;

import au.edu.flinders.timetable.controller.ClassController;
import au.edu.flinders.timetable.controller.ImportExportController;
import au.edu.flinders.timetable.controller.MenuController;
import au.edu.flinders.timetable.controller.TimetableController;
import au.edu.flinders.timetable.repository.ClassRepository;
import au.edu.flinders.timetable.repository.PreferenceRepository;
import au.edu.flinders.timetable.repository.TimetableRepository;
import au.edu.flinders.timetable.repository.TopicRepository;
import au.edu.flinders.timetable.service.CSVExportService;
import au.edu.flinders.timetable.service.CSVImportService;
import au.edu.flinders.timetable.service.ClassService;
import au.edu.flinders.timetable.service.PreferenceService;
import au.edu.flinders.timetable.service.SearchService;
import au.edu.flinders.timetable.service.TimetableGeneratorService;
import au.edu.flinders.timetable.service.TimetableService;
import au.edu.flinders.timetable.ui.ConsoleView;
import au.edu.flinders.timetable.ui.InputHelper;

import java.util.Scanner;

/** Entry point — wires all dependencies and starts the application. */
public class Main {

    /** Bootstraps the application by constructing the dependency graph and starting the menu. */
    public static void main(String[] args) {

        // ── Repositories ──────────────────────────────────────────
        ClassRepository      classRepo      = new ClassRepository();
        TopicRepository      topicRepo      = new TopicRepository();
        TimetableRepository  timetableRepo  = new TimetableRepository();
        PreferenceRepository preferenceRepo = new PreferenceRepository();

        // ── Services ──────────────────────────────────────────────
        CSVImportService           importService     = new CSVImportService(topicRepo, classRepo);
        CSVExportService           exportService     = new CSVExportService(classRepo);
        ClassService               classService      = new ClassService(classRepo, topicRepo);
        SearchService              searchService     = new SearchService(classRepo, topicRepo);
        PreferenceService          preferenceService = new PreferenceService(preferenceRepo);
        TimetableService           timetableService  = new TimetableService(timetableRepo);
        TimetableGeneratorService  generatorService  = new TimetableGeneratorService(
            classRepo, topicRepo, preferenceRepo, timetableService);

        // ── UI helpers ────────────────────────────────────────────
        ConsoleView view  = new ConsoleView();
        InputHelper input = new InputHelper();
        Scanner     sc    = new Scanner(System.in);

        // ── Controllers ───────────────────────────────────────────
        ImportExportController importExportController = new ImportExportController(
            importService, view, input, sc);
        ClassController classController = new ClassController(
            classService, searchService, view);
        TimetableController timetableController = new TimetableController(
            generatorService, timetableService, exportService, classRepo, view, input, sc);

        MenuController menu = new MenuController(
            sc, view, input, importExportController,
            classController, timetableController, preferenceService);

        // ── Start ─────────────────────────────────────────────────
        menu.start();
        sc.close();
    }
}
