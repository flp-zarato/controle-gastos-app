package com.flpzarato.controlegastos.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** One selectable item inside a [FilterSection] — e.g. a category or a payment method. */
data class FilterOption(val id: String, val label: String)

/**
 * One accordion category inside [FiltersBottomSheet] (e.g. "Forma de pagamento"). [selectedIds]
 * empty means "Todos" — no filter applied in this dimension, matching the ViewModel's existing
 * "empty set = no filter" convention. [onToggle] flips one option in/out of the pending
 * selection; [onAllSelected] clears it back to "Todos".
 */
data class FilterSection(
    val title: String,
    val options: List<FilterOption>,
    val selectedIds: Set<String>,
    val onToggle: (String) -> Unit,
    val onAllSelected: () -> Unit,
)

/**
 * Bottom sheet listing every filter dimension as a collapsible section with checkboxes. The
 * selection shown here is always a draft (`pending*` in the ViewModel) — callers must copy the
 * applied filters into it before opening, and only [onApply] (never a toggle) should affect what
 * the rest of the screen actually filters by. [onCancel] (including swipe-to-dismiss) discards
 * the draft; the next open re-seeds it from the still-unchanged applied filters.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiltersBottomSheet(
    sections: List<FilterSection>,
    onCancel: () -> Unit,
    onApply: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onCancel) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            sections.forEach { section -> FilterAccordionSection(section) }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onCancel) { Text("Cancelar") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = onApply) { Text("Aplicar") }
            }
        }
    }
}

@Composable
private fun FilterAccordionSection(section: FilterSection) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(section.title, style = MaterialTheme.typography.titleSmall)
            Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (expanded) "Recolher" else "Expandir",
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column {
                FilterOptionRow(
                    label = "Todos",
                    checked = section.selectedIds.isEmpty(),
                    onClick = section.onAllSelected,
                )
                section.options.forEach { option ->
                    FilterOptionRow(
                        label = option.label,
                        checked = option.id in section.selectedIds,
                        onClick = { section.onToggle(option.id) },
                    )
                }
            }
        }
    }
}

/**
 * The whole row is the tap target; the [Checkbox] itself gets `onCheckedChange = null` (display
 * only) so tapping it doesn't also fire the row's own `clickable` — that double-call toggled the
 * selection on and immediately back off.
 */
@Composable
private fun FilterOptionRow(label: String, checked: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}
