package com.intruphon.app.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/**
 * The rows picked in a list, by id (multi-select, like the Gmail and Photos lists). Survives a rotation.
 * Pure state with no screen in it, so its rules can be tested.
 */
@Stable
class Selection internal constructor(initial: Set<Long> = emptySet()) {
    var selected: Set<Long> by mutableStateOf(initial)
        private set

    val isActive: Boolean get() = selected.isNotEmpty()
    val count: Int get() = selected.size

    fun isSelected(id: Long): Boolean = id in selected

    fun toggle(id: Long) {
        selected = if (id in selected) selected - id else selected + id
    }

    /** Picks all of [ids], on top of what is already picked. */
    fun selectAll(ids: Collection<Long>) {
        selected = selected + ids
    }

    /** All of [ids] already picked? Then the "select all" button should clear instead. */
    fun allSelected(ids: Collection<Long>): Boolean = ids.isNotEmpty() && selected.containsAll(ids)

    fun clear() {
        selected = emptySet()
    }

    /** Drops what left the list (a row removed elsewhere). */
    fun retain(ids: Set<Long>) {
        if (selected.isNotEmpty() && !ids.containsAll(selected)) selected = selected.intersect(ids)
    }

    companion object {
        val Saver = listSaver<Selection, Long>(save = { it.selected.toList() }, restore = { Selection(it.toSet()) })
    }
}

@Composable
fun rememberSelection(): Selection = rememberSaveable(saver = Selection.Saver) { Selection() }
