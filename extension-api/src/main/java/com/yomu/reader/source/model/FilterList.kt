package com.yomu.reader.source.model

/**
 * Search filters a source exposes. Minimal port of Tachiyomi's Filter hierarchy —
 * enough for text search plus a couple of common filter widgets.
 */
sealed class Filter<T>(val name: String, var state: T) {
    open class Header(name: String) : Filter<Any?>(name, null)
    open class Separator : Filter<Any?>("", null)
    abstract class Select<V>(name: String, val values: Array<V>, state: Int = 0) : Filter<Int>(name, state)
    abstract class CheckBox(name: String, state: Boolean = false) : Filter<Boolean>(name, state)
    abstract class TriState(name: String, state: Int = STATE_IGNORE) : Filter<Int>(name, state) {
        fun isIgnored() = state == STATE_IGNORE
        fun isIncluded() = state == STATE_INCLUDE
        fun isExcluded() = state == STATE_EXCLUDE
        companion object {
            const val STATE_IGNORE = 0
            const val STATE_INCLUDE = 1
            const val STATE_EXCLUDE = 2
        }
    }
    abstract class Group<V>(name: String, val filters: List<V>) : Filter<List<V>>(name, filters)
    abstract class Sort(name: String, val values: Array<String>, state: Selection? = null) :
        Filter<Sort.Selection?>(name, state) {
        data class Selection(val index: Int, val ascending: Boolean)
    }
}

typealias FilterList = List<Filter<*>>
