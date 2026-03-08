package dev.spyglass.android.calculators.notes

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.spyglass.android.R
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.spyglass.android.core.ui.*
import dev.spyglass.android.data.db.entities.NoteEntity
import dev.spyglass.android.data.repository.GameDataRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class NotesViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = GameDataRepository.get(app)
    private val _query = MutableStateFlow("")
    private val _labelFilter = MutableStateFlow("all")
    val query: StateFlow<String> = _query.asStateFlow()
    val labelFilter: StateFlow<String> = _labelFilter.asStateFlow()

    val labels: StateFlow<List<String>> = repo.allNoteLabels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notes: StateFlow<List<NoteEntity>> = combine(_query.debounce(200), _labelFilter) { q, label ->
        if (label != "all") repo.notesByLabel(label)
        else repo.searchNotes(q)
    }.flatMapLatest { it }
     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _expandedIds = MutableStateFlow<Set<Long>>(emptySet())
    val expandedIds: StateFlow<Set<Long>> = _expandedIds.asStateFlow()

    fun setQuery(q: String) { _query.value = q }
    fun setLabelFilter(l: String) { _labelFilter.value = l }
    fun toggleExpanded(id: Long) {
        val current = _expandedIds.value
        _expandedIds.value = if (id in current) current - id else current + id
    }

    fun createNote(title: String, label: String, content: String) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            repo.createNote(NoteEntity(title = title, label = label, content = content, createdAt = now, updatedAt = now))
        }
    }

    fun updateNote(id: Long, title: String, label: String, content: String) {
        viewModelScope.launch { repo.updateNote(id, title, label, content) }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch {
            repo.deleteNote(id)
            _expandedIds.value = _expandedIds.value - id
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NotesScreen(vm: NotesViewModel = viewModel()) {
    val hapticClick = rememberHapticClick()
    val hapticConfirm = rememberHapticConfirm()
    val query by vm.query.collectAsStateWithLifecycle()
    val labelFilter by vm.labelFilter.collectAsStateWithLifecycle()
    val notes by vm.notes.collectAsStateWithLifecycle()
    val labels by vm.labels.collectAsStateWithLifecycle()
    val expandedIds by vm.expandedIds.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<NoteEntity?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search bar + add button
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = query, onValueChange = vm::setQuery,
                placeholder = { Text("Search notes\u2026", color = MaterialTheme.colorScheme.secondary) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.secondary) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline, cursorColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            FilledTonalButton(
                onClick = { hapticClick(); showCreateDialog = true },
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), contentColor = MaterialTheme.colorScheme.primary),
            ) {
                Icon(Icons.Default.Add, contentDescription = "New note", modifier = Modifier.size(18.dp))
            }
        }

        // Label filter chips
        if (labels.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                FilterChip(
                    selected = labelFilter == "all",
                    onClick = { hapticClick(); vm.setLabelFilter("all") },
                    label = { Text(stringResource(R.string.all), style = MaterialTheme.typography.labelSmall) },
                )
                labels.forEach { label ->
                    FilterChip(
                        selected = labelFilter == label,
                        onClick = { hapticClick(); vm.setLabelFilter(label) },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                TabIntroHeader(
                    icon = PixelIcons.Bookmark,
                    title = "Notes",
                    description = "Write, label, and organize your Minecraft notes. Add labels to group them by topic.",
                    stat = "${notes.size} notes",
                )
            }

            if (notes.isEmpty()) {
                item {
                    EmptyState(
                        icon = PixelIcons.Bookmark,
                        title = if (query.isNotBlank() || labelFilter != "all") "No notes found" else "No notes yet",
                        subtitle = if (query.isNotBlank() || labelFilter != "all") "Try a different search or label" else "Tap + to create your first note",
                    )
                }
            }

            items(notes, key = { it.id }) { note ->
                val isExpanded = note.id in expandedIds
                Column {
                    BrowseListItem(
                        headline = note.title,
                        supporting = note.content.take(80).replace('\n', ' '),
                        supportingMaxLines = 1,
                        leadingIcon = PixelIcons.Bookmark,
                        modifier = Modifier.clickable { hapticClick(); vm.toggleExpanded(note.id) },
                        trailing = {
                            if (note.label.isNotBlank()) {
                                CategoryBadge(label = note.label, color = MaterialTheme.colorScheme.primary)
                            }
                        },
                    )
                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = expandVertically(),
                        exit = shrinkVertically(),
                    ) {
                        NoteDetailCard(
                            note = note,
                            onEdit = { editingNote = note },
                            onDelete = { vm.deleteNote(note.id) },
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }

    if (showCreateDialog) {
        NoteDialog(
            title = "New Note",
            existingLabels = labels,
            onDismiss = { showCreateDialog = false },
            onSave = { title, label, content ->
                vm.createNote(title, label, content)
                showCreateDialog = false
            },
        )
    }

    if (editingNote != null) {
        val note = editingNote!!
        NoteDialog(
            title = "Edit Note",
            initialTitle = note.title,
            initialLabel = note.label,
            initialContent = note.content,
            existingLabels = labels,
            onDismiss = { editingNote = null },
            onSave = { title, label, content ->
                vm.updateNote(note.id, title, label, content)
                editingNote = null
            },
        )
    }
}

@Composable
private fun NoteDetailCard(
    note: NoteEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val hapticClick = rememberHapticClick()
    val hapticConfirm = rememberHapticConfirm()
    var confirmDelete by remember { mutableStateOf(false) }

    ResultCard(modifier = Modifier.padding(top = 4.dp)) {
        if (note.content.isNotBlank()) {
            Text(note.content, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            SpyglassDivider()
        }
        if (note.label.isNotBlank()) {
            StatRow("Label", note.label)
        }
        StatRow("Created", formatTimestamp(note.createdAt))
        StatRow("Updated", formatTimestamp(note.updatedAt))
        SpyglassDivider()
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { hapticClick(); onEdit() }) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.edit))
            }
            if (!confirmDelete) {
                TextButton(onClick = { hapticConfirm(); confirmDelete = true }, colors = ButtonDefaults.textButtonColors(contentColor = Red400)) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.delete))
                }
            } else {
                TextButton(onClick = { hapticConfirm(); onDelete() }, colors = ButtonDefaults.textButtonColors(contentColor = Red400)) {
                    Text(stringResource(R.string.confirm_delete))
                }
                TextButton(onClick = { hapticClick(); confirmDelete = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NoteDialog(
    title: String,
    initialTitle: String = "",
    initialLabel: String = "",
    initialContent: String = "",
    existingLabels: List<String>,
    onDismiss: () -> Unit,
    onSave: (title: String, label: String, content: String) -> Unit,
) {
    val hapticClick = rememberHapticClick()
    var noteTitle by remember { mutableStateOf(initialTitle) }
    var noteLabel by remember { mutableStateOf(initialLabel) }
    var noteContent by remember { mutableStateOf(initialContent) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = noteTitle,
                    onValueChange = { noteTitle = it },
                    label = { Text("Title") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, cursorColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = noteLabel,
                    onValueChange = { noteLabel = it },
                    label = { Text("Label (optional)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, cursorColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (existingLabels.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        existingLabels.forEach { label ->
                            FilterChip(
                                selected = noteLabel == label,
                                onClick = { hapticClick(); noteLabel = if (noteLabel == label) "" else label },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = noteContent,
                    onValueChange = { noteContent = it },
                    label = { Text("Content") },
                    minLines = 4,
                    maxLines = 8,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, cursorColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { hapticClick(); onSave(noteTitle.trim(), noteLabel.trim(), noteContent.trim()) },
                enabled = noteTitle.isNotBlank(),
            ) { Text(stringResource(R.string.save), color = MaterialTheme.colorScheme.primary) }
        },
        dismissButton = {
            TextButton(onClick = { hapticClick(); onDismiss() }) { Text(stringResource(R.string.cancel)) }
        },
    )
}

private fun formatTimestamp(millis: Long): String {
    val sdf = java.text.SimpleDateFormat("MMM d, yyyy h:mm a", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(millis))
}
