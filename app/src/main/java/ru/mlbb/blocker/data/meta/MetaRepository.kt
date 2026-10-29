package ru.mlbb.blocker.data.meta

import android.content.Context
import android.content.SharedPreferences
import ru.mlbb.blocker.BuildConfig

/**
 * Кэш меты и настройки её источника. Последний успешный ответ сайта хранится
 * в Room, поэтому тир-листы открываются и без интернета.
 */
object MetaRepository {

    private const val PREFS = "mlbb_meta"
    private const val KEY_API_BASE = "api_base"
    private const val KEY_REGION = "region"
    private const val KEY_DRAFT_BANS = "draft_bans"

    @Volatile
    private lateinit var prefs: SharedPreferences

    @Volatile
    private var initialized = false

    fun initialize(context: Context) {
        if (initialized) return
        val app = context.applicationContext
        prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        MetaDatabase.get(app)
        initialized = true
    }

    fun apiBase(context: Context): String {
        initialize(context)
        val saved = prefs.getString(KEY_API_BASE, null)?.trim()
        if (!saved.isNullOrBlank()) return saved
        return BuildConfig.META_API_BASE
    }

    fun setApiBase(context: Context, value: String) {
        initialize(context)
        prefs.edit().putString(KEY_API_BASE, value.trim().trimEnd('/')).apply()
    }

    fun region(context: Context): String {
        initialize(context)
        return prefs.getString(KEY_REGION, REGION_WORLD) ?: REGION_WORLD
    }

    fun setRegion(context: Context, region: String) {
        initialize(context)
        val valid = REGIONS.firstOrNull { it.first == region }?.first ?: REGION_WORLD
        prefs.edit().putString(KEY_REGION, valid).apply()
    }

    fun draftBans(context: Context): Set<String> {
        initialize(context)
        return prefs.getStringSet(KEY_DRAFT_BANS, null)?.toSet() ?: emptySet()
    }

    fun addDraftBans(context: Context, ids: Collection<String>): Set<String> {
        initialize(context)
        val updated = draftBans(context) + ids
        prefs.edit().putStringSet(KEY_DRAFT_BANS, updated).apply()
        return updated
    }

    fun toggleDraftBan(context: Context, heroId: String): Set<String> {
        initialize(context)
        val current = draftBans(context).toMutableSet()
        if (!current.add(heroId)) current.remove(heroId)
        prefs.edit().putStringSet(KEY_DRAFT_BANS, current).apply()
        return current
    }

    suspend fun loadCached(context: Context): MetaSnapshot? {
        val dao = MetaDatabase.get(context).metaDao()
        val heroes = dao.heroes()
        if (heroes.isEmpty()) return null
        val state = dao.state()
        return MetaSnapshot(
            heroes = heroes.map { it.toModel() },
            region = state?.region ?: REGION_WORLD,
            regional = state?.regional ?: false,
            patch = state?.patch,
            updatedAt = state?.updatedAt,
            fetchedAt = state?.fetchedAt ?: 0L,
        )
    }

    suspend fun refresh(context: Context, region: String): MetaResult<MetaSnapshot> {
        val result = MetaApi(apiBase(context)).fetchMeta(region)
        if (result is MetaResult.Success) {
            val dao = MetaDatabase.get(context).metaDao()
            dao.clearHeroes()
            dao.upsertHeroes(result.value.heroes.map { it.toEntity() })
            dao.upsertState(
                MetaStateEntity(
                    region = result.value.region,
                    regional = result.value.regional,
                    patch = result.value.patch,
                    updatedAt = result.value.updatedAt,
                    fetchedAt = result.value.fetchedAt,
                ),
            )
        }
        return result
    }

    suspend fun loadCounters(context: Context, heroId: String): MetaResult<MetaCounters> {
        return MetaApi(apiBase(context)).fetchCounters(heroId)
    }

    const val REGION_WORLD = "world"
    const val REGION_MOSCOW = "moscow"
    const val REGION_FAR_EAST = "far_east"

    val REGIONS = listOf(
        REGION_WORLD to "Мир",
        REGION_MOSCOW to "Москва",
        REGION_FAR_EAST to "ДВ",
    )

    fun regionTitle(region: String): String =
        REGIONS.firstOrNull { it.first == region }?.second ?: "Мир"

    private fun MetaHero.toEntity(): MetaHeroEntity = MetaHeroEntity(
        id = id,
        nameEn = nameEn,
        nameRu = nameRu,
        role = role,
        lane = lane?.key ?: "",
        lanes = lanes.joinToString(",") { it.key },
        imageUrl = imageUrl,
        winRate = winRate,
        pickRate = pickRate,
        banRate = banRate,
        matches = matches,
        tier = tier.key,
        sortOrder = sortOrder,
    )

    private fun MetaHeroEntity.toModel(): MetaHero = MetaHero(
        id = id,
        nameEn = nameEn,
        nameRu = nameRu,
        role = role,
        lane = MetaLane.from(lane),
        lanes = lanes.split(',').mapNotNull { MetaLane.from(it) },
        imageUrl = imageUrl,
        winRate = winRate,
        pickRate = pickRate,
        banRate = banRate,
        matches = matches,
        tier = MetaTier.from(tier),
        sortOrder = sortOrder,
    )
}
