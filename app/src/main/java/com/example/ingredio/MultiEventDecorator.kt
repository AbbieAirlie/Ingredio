package com.example.ingredio

import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade

class MultiEventDecorator(private val day: CalendarDay, private val colors: List<Int>) : DayViewDecorator {
    override fun shouldDecorate(day: CalendarDay): Boolean {
        return day == this.day
    }

    override fun decorate(view: DayViewFacade) {
        view.addSpan(MultiDotSpan(5f, colors))
    }
}
