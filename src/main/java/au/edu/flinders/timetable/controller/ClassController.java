package au.edu.flinders.timetable.controller;

import au.edu.flinders.timetable.model.ClassEntry;
import au.edu.flinders.timetable.service.ClassService;
import au.edu.flinders.timetable.service.SearchService;
import au.edu.flinders.timetable.ui.ConsoleView;

import java.util.List;

/** Routes class-related user actions to ClassService and SearchService. */
public class  ClassController {

    private final ClassService  classService;
    private final SearchService searchService;
    private final ConsoleView   view;

    /** Constructs the controller with its required service and view dependencies. */
    public ClassController(ClassService classService, SearchService searchService,
                           ConsoleView view) {
        this.classService  = classService;
        this.searchService = searchService;
        this.view          = view;
    }

    /** Retrieves and displays all stored classes via ConsoleView. */
    public void showAll() {
        List<ClassEntry> all = classService.getAllClasses();
        if (all.isEmpty()) {
            view.printWarning("No classes have been imported yet.");
        } else {
            System.out.println("\n  All Classes (" + all.size() + " total):");
            view.printClassList(all);
        }
    }

    /** Executes a search with the given query and displays matching results via ConsoleView. */
    public void search(String query) {
        List<ClassEntry> results = searchService.search(query);
        System.out.println("\n  Search results for \"" + query + "\" ("
            + results.size() + " found):");
        view.printClassList(results);
    }
}
