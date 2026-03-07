package dev.spyglass.android.core.shell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.spyglass.android.core.module.HomeSection
import dev.spyglass.android.core.module.HomeSectionScope
import dev.spyglass.android.core.module.ModuleRegistry

/**
 * Thin shell home screen — collects [HomeSection]s from all enabled modules,
 * sorts by weight, and renders them in a LazyColumn.
 */
@Composable
fun ShellHomeScreen(scope: HomeSectionScope, scrollToTopTrigger: Int = 0) {
    val context = LocalContext.current

    // Re-read enabled modules whenever a module is toggled
    val revision by ModuleRegistry.revision.collectAsStateWithLifecycle()

    val sections by produceState(emptyList<HomeSection>(), revision) {
        value = ModuleRegistry.enabledModules(context)
            .flatMap { it.homeSections() }
            .sortedBy { it.weight }
    }

    val listState = rememberLazyListState()

    // Scroll to top when tab is re-tapped
    LaunchedEffect(scrollToTopTrigger) {
        if (scrollToTopTrigger > 0) listState.animateScrollToItem(0)
    }

    // Ensure list starts at top when sections first load
    LaunchedEffect(sections.isNotEmpty()) {
        if (sections.isNotEmpty()) listState.scrollToItem(0)
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        sections.forEach { section ->
            item(key = section.key) {
                section.content(scope)
            }
        }

        item(key = "bottom_spacer") {
            Spacer(Modifier.height(8.dp))
        }
    }
}
