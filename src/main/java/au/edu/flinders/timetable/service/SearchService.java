package au.edu.flinders.timetable.service;

import au.edu.flinders.timetable.model.ClassEntry;
import au.edu.flinders.timetable.model.Topic;
import au.edu.flinders.timetable.repository.ClassRepository;
import au.edu.flinders.timetable.repository.TopicRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/** Provides keyword and structured search operations over class data. */
public class SearchService {

    private final ClassRepository classRepository;
    private final TopicRepository topicRepository;

    /** Constructs the service with the repositories it searches. */
    public SearchService(ClassRepository classRepository, TopicRepository topicRepository) {
        this.classRepository = classRepository;
        this.topicRepository = topicRepository;
    }

    /**
     * Backward-compatible keyword search.
     * Returns all ClassEntries where any field or linked-topic field contains the query
     * (case-insensitive). An empty query returns all classes.
     */
    public List<ClassEntry> search(String query) {
        if (query == null || query.isBlank()) {
            return classRepository.findAll();
        }
        return classRepository.findAll().stream()
            .filter(c -> matchesQuery(c, query.toLowerCase()))
            .collect(Collectors.toList());
    }

    /**
     * Structured multi-criteria search.
     * Returns all ClassEntries where every non-null, non-blank criterion matches.
     * String criteria use case-insensitive partial matching.
     * Integer criteria use exact matching; -1 means "any".
     * LocalTime criteria use exact matching; null means "any".
     */
    public List<ClassEntry> search(SearchCriteria sc) {
        if (sc == null || sc.isEmpty()) {
            return classRepository.findAll();
        }
        return classRepository.findAll().stream()
            .filter(c -> matchesCriteria(c, sc))
            .collect(Collectors.toList());
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /** Returns true when the class or its topic contains the keyword in any field. */
    private boolean matchesQuery(ClassEntry c, String q) {
        Optional<Topic> topicOpt = topicRepository.findByCourseCode(c.getCourseCode());
        if (contains(c.getClassId(),        q)) return true;
        if (contains(c.getCourseCode(),     q)) return true;
        if (contains(c.getType(),           q)) return true;
        if (contains(c.getBuilding(),       q)) return true;
        if (contains(c.getRoom(),           q)) return true;
        if (c.getDay() != null && contains(c.getDay().toString(), q)) return true;
        if (contains(c.getAttendanceMode(), q)) return true;
        if (contains(c.getDateFrom(),       q)) return true;
        if (contains(c.getDateTo(),         q)) return true;
        if (topicOpt.isPresent()) {
            Topic t = topicOpt.get();
            if (contains(t.getCampus(),    q)) return true;
            if (contains(t.getTopicName(), q)) return true;
            if (contains(t.getDelivery(),  q)) return true;
        }
        return false;
    }

    /** Returns true when the class matches all active criteria in the SearchCriteria. */
    private boolean matchesCriteria(ClassEntry c, SearchCriteria sc) {
        Optional<Topic> topicOpt = topicRepository.findByCourseCode(c.getCourseCode());
        Topic topic = topicOpt.orElse(null);

        if (!isBlank(sc.courseCode)     && !contains(c.getCourseCode(), sc.courseCode))                              return false;
        if (!isBlank(sc.topicName)      && (topic == null || !contains(topic.getTopicName(), sc.topicName)))          return false;
        if (!isBlank(sc.attendanceMode) && !contains(c.getAttendanceMode(), sc.attendanceMode))                       return false;
        if (!isBlank(sc.campus)         && (topic == null || !contains(topic.getCampus(), sc.campus)))                return false;
        if (sc.semester != -1           && (topic == null || topic.getSemester() != sc.semester))                     return false;
        if (sc.availabilityNumber != -1 && c.getAvailabilityNumber() != sc.availabilityNumber)                        return false;
        if (!isBlank(sc.classType)      && !contains(c.getType(), sc.classType))                                      return false;
        if (sc.classInstance != -1      && c.getClassInstance() != sc.classInstance)                                  return false;
        if (!isBlank(sc.dateFrom)       && !contains(c.getDateFrom(), sc.dateFrom))                                   return false;
        if (!isBlank(sc.dateTo)         && !contains(c.getDateTo(), sc.dateTo))                                       return false;
        if (!isBlank(sc.day)            && (c.getDay() == null || !contains(c.getDay().toString(), sc.day)))          return false;
        if (sc.startTime != null        && !sc.startTime.equals(c.getStartTime()))                                    return false;
        if (sc.endTime   != null        && !sc.endTime.equals(c.getEndTime()))                                        return false;
        if (!isBlank(sc.building)       && !contains(c.getBuilding(), sc.building))                                   return false;
        if (!isBlank(sc.room)           && !contains(c.getRoom(), sc.room))                                           return false;
        return true;
    }

    private boolean contains(String field, String query) {
        return field != null && field.toLowerCase().contains(query.toLowerCase());
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
