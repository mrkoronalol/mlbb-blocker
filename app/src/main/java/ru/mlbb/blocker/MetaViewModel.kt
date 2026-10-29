package ru.mlbb.blocker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.mlbb.blocker.data.meta.MetaCounters
import ru.mlbb.blocker.data.meta.MetaHero
import ru.mlbb.blocker.data.meta.MetaRepository
import ru.mlbb.blocker.data.meta.MetaResult
import ru.mlbb.blocker.data.meta.MetaSnapshot
import ru.mlbb.blocker.data.meta.MetaTier

data class MetaUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val region: String = MetaRepository.REGION_WORLD,
    val snapshot: MetaSnapshot? = null,
    val error: String? = null,
    val bans: Set<String> = emptySet(),
    val expandedTiers: Set<MetaTier> = setOf(MetaTier.S, MetaTier.A),
    val selectedHero: MetaHero? = null,
    val counters: MetaCounters? = null,
    val countersLoading: Boolean = false,
    val countersError: String? = null,
    val sheetMessage: String? = null,
)

class MetaViewModel(application: Application) : AndroidViewModel(application) {

    private val _ui = MutableStateFlow(
        MetaUiState(
            region = MetaRepository.region(application),
            bans = MetaRepository.draftBans(application),
        ),
    )
    val ui: StateFlow<MetaUiState> = _ui.asStateFlow()

    init {
        MetaRepository.initialize(application)
        loadCache()
        refresh()
    }

    private fun loadCache() {
        viewModelScope.launch {
            val cached = withContext(Dispatchers.IO) {
                MetaRepository.loadCached(getApplication())
            }
            _ui.update {
                it.copy(
                    loading = false,
                    snapshot = cached ?: it.snapshot,
                    bans = MetaRepository.draftBans(getApplication()),
                )
            }
        }
    }

    fun refresh() {
        if (_ui.value.refreshing) return
        _ui.update { it.copy(refreshing = true, error = null) }
        viewModelScope.launch {
            val region = _ui.value.region
            val result = withContext(Dispatchers.IO) {
                MetaRepository.refresh(getApplication(), region)
            }
            _ui.update { state ->
                when (result) {
                    is MetaResult.Success -> state.copy(
                        refreshing = false,
                        loading = false,
                        snapshot = result.value,
                        error = null,
                    )

                    is MetaResult.Failure -> state.copy(
                        refreshing = false,
                        loading = false,
                        error = result.message,
                    )
                }
            }
        }
    }

    fun selectRegion(region: String) {
        if (_ui.value.region == region) return
        MetaRepository.setRegion(getApplication(), region)
        _ui.update { it.copy(region = region) }
        refresh()
    }

    fun toggleTier(tier: MetaTier) {
        _ui.update { state ->
            val expanded = state.expandedTiers.toMutableSet()
            if (!expanded.add(tier)) expanded.remove(tier)
            state.copy(expandedTiers = expanded)
        }
    }

    fun openHero(hero: MetaHero) {
        _ui.update {
            it.copy(
                selectedHero = hero,
                counters = null,
                countersError = null,
                sheetMessage = null,
            )
        }
        loadCounters(hero)
    }

    fun closeHero() {
        _ui.update { it.copy(selectedHero = null, counters = null, countersError = null, sheetMessage = null) }
    }

    private fun loadCounters(hero: MetaHero) {
        _ui.update { it.copy(countersLoading = true, countersError = null) }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                MetaRepository.loadCounters(getApplication(), hero.id)
            }
            _ui.update { state ->
                when (result) {
                    is MetaResult.Success -> state.copy(
                        countersLoading = false,
                        counters = result.value,
                    )

                    is MetaResult.Failure -> state.copy(
                        countersLoading = false,
                        countersError = result.message,
                    )
                }
            }
        }
    }

    fun banCounters() {
        val counters = _ui.value.counters ?: return
        if (counters.ids.isEmpty()) {
            _ui.update { it.copy(sheetMessage = "Для этого героя пока нет контрпиков") }
            return
        }
        val updated = MetaRepository.addDraftBans(getApplication(), counters.ids)
        _ui.update {
            it.copy(
                bans = updated,
                sheetMessage = "Контрпики добавлены в бан-лист: ${counters.names.size}",
            )
        }
    }

    fun toggleBan(heroId: String) {
        val updated = MetaRepository.toggleDraftBan(getApplication(), heroId)
        _ui.update { it.copy(bans = updated) }
    }
}
