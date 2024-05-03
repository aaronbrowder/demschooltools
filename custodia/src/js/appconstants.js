var exports = {
  studentEvents: {
    LOADED: "STUDENTEVENTS.LOADED",
    STUDENT_LOADED: "STUDENTEVENTS.STUDENTLOADED",
    MARKED_ABSENT: "STUDENTEVENTS.MARKED_ABSENT",
    STUDENT_SWIPED: "STUDENTEVENTS.STUDENT_SWIPED",
    EXCUSED: "STUDENTEVENTS.EXCUSED",
  },
  classEvents: {
    CLASSES_LOADED: "CLASSES.LOADED",
    CLASS_CREATED: "CLASS.CREATED",
    CLASS_DELETED: "CLASS.DELETED",
    CLASS_STUDENT_ADDED: "CLASS.STUDENT.ADDED",
    CLASS_STUDENT_REMOVED: "CLASS.STUDENT.REMOVED",
  },
  systemEvents: {
    TODAY_LOADED: "SYSTEMEVENTS.TODAYLOADED",
    FLASH: "SYSTEMEVENTS.FLASH",
  },
  reportEvents: {
    YEARS_LOADED: "REPORTEVENTS.YEARSLOADED",
    REPORT_LOADED: "REPORTEVENT.REPORTLOADED",
    PERIOD_CREATED: "REPORTEVENT.PERIOD_CREATED",
    PERIOD_DELETED: "REPORTEVENT.PERIOD_DELETED",
  },
};

module.exports = exports;
