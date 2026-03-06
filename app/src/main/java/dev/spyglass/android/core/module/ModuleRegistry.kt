package dev.spyglass.android.core.module

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import dev.spyglass.android.settings.dataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Central registry for all [SpyglassModule] instances.
 * Enable/disable state is persisted via DataStore.
 */
object ModuleRegistry {

    private val _modules = mutableListOf<SpyglassModule>()

    /** All registered modules, regardless of enabled state. */
    val modules: List<SpyglassModule> get() = _modules.toList()

    /**
     * Incremented each time a module is toggled. Observe this to reactively
     * recompute enabled modules in Compose.
     */
    private val _revision = MutableStateFlow(0)
    val revision: StateFlow<Int> = _revision.asStateFlow()

    /** Register a module. Call from [SpyglassApp.onCreate]. */
    fun register(module: SpyglassModule) {
        _modules.removeAll { it.id == module.id }
        _modules.add(module)
        _modules.sortBy { it.priority }
    }

    /** Modules that are currently enabled, sorted by priority. */
    suspend fun enabledModules(context: Context): List<SpyglassModule> {
        return _modules.filter { isEnabled(context, it) }
    }

    /** Check if a specific module is enabled. Non-disableable modules always return true. */
    suspend fun isEnabled(context: Context, module: SpyglassModule): Boolean {
        if (!module.canDisable) return true
        val key = booleanPreferencesKey("module_enabled_${module.id}")
        return context.dataStore.data.map { it[key] ?: true }.first()
    }

    /** Check if a module is enabled by id. */
    suspend fun isEnabled(context: Context, moduleId: String): Boolean {
        val module = _modules.find { it.id == moduleId } ?: return false
        return isEnabled(context, module)
    }

    /** Toggle a module's enabled state. No-op for modules where canDisable is false. */
    suspend fun setEnabled(context: Context, moduleId: String, enabled: Boolean) {
        val module = _modules.find { it.id == moduleId } ?: return
        if (!module.canDisable) return
        val key = booleanPreferencesKey("module_enabled_${module.id}")
        context.dataStore.edit { it[key] = enabled }
        if (enabled) module.onEnabled() else module.onDisabled()
        _revision.value++
    }

    /** Initialize all registered modules. Call from Application.onCreate. */
    suspend fun initAll(context: Context) {
        _modules.forEach { module ->
            if (!module.canDisable || isEnabled(context, module)) {
                module.onInit(context)
            }
        }
    }
}
