package com.groovernapp.mosqueathan.data

interface ScheduleItem {
    var id: String
    var startTime: String // "HH:mm"
    var endTime: String // "HH:mm"
    var activeDays: String // Comma-separated days, e.g. "Monday,Wednesday"
    var specificDates: String // Comma-separated dates, e.g. "2026-06-18,2026-06-19"
    var createdAt: Long // Timestamp when created for sorting
}
