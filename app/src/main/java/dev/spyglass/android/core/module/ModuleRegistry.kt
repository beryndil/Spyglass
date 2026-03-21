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
 *
 * Modules are registered at app startup from [SpyglassApp.onCreate] and the
 * list is immutable after that point. Enable/disable state is persisted via
 * DataStore and toggled at runtime from the Settings screen.
 *
 * Thread safety: [register] is called only during single-threaded Application
 * init. The backing list is guarded by `synchronized` for defensive safety.
 */
object ModuleRegistry {

    private val _modules = mutableListOf<SpyglassModule>()
    private val lock = Any()

    /** Snapshot of all registered modules, regardless of enabled state. */
    val modules: List<SpyglassModule>
        get() = synchronized(lock) { _modules.toList() }

    /**
     * Incremented each time a module is toggled. Compose producers observe
     * this to reactively recompute the enabled-module list.
     */
    private val _revision = MutableStateFlow(0)
    val revision: StateFlow<Int> = _revision.asStateFlow()

    /**
     * Register a module, replacing any existing module with the same [SpyglassModule.id].
     * Safe to call from [SpyglassApp.onCreate] only — not designed for runtime hot-swap.
     */
    fun register(module: SpyglassModule) {
        synchronized(lock) {
            _modules.removeAll { it.id == module.id }
            _modules.add(module)
            _modules.sortBy { it.priority }
        }
    }

    /** Modules that are currently enabled, sorted by priority. */
    suspend fun enabledModules(context: Context): List<SpyglassModule> {
        return modules.filter { isEnabled(context, it) }
    }

    /** Check if a specific module is enabled. Non-disableable modules always return true. */
    suspend fun isEnabled(context: Context, module: SpyglassModule): Boolean {
        if (!module.canDisable) return true
        val key = booleanPreferencesKey("module_enabled_${module.id}")
        return context.dataStore.data.map { it[key] ?: true }.first()
    }

    /** Check if a module is enabled by id. Returns false if the module is not registered. */
    suspend fun isEnabled(context: Context, moduleId: String): Boolean {
        val module = modules.find { it.id == moduleId } ?: return false
        return isEnabled(context, module)
    }

    /** Toggle a module's enabled state. No-op for modules where [SpyglassModule.canDisable] is false. */
    suspend fun setEnabled(context: Context, moduleId: String, enabled: Boolean) {
        val module = modules.find { it.id == moduleId } ?: return
        if (!module.canDisable) return
        val key = booleanPreferencesKey("module_enabled_${module.id}")
        context.dataStore.edit { it[key] = enabled }
        if (enabled) module.onEnabled() else module.onDisabled()
        _revision.value++
    }

    /** Initialize all registered modules. Call from Application.onCreate. */
    suspend fun initAll(context: Context) {
        modules.forEach { module ->
            if (!module.canDisable || isEnabled(context, module)) {
                module.onInit(context)
            }
        }
    }
}
