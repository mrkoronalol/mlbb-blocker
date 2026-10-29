package ru.mlbb.blocker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.mlbb.blocker.data.meta.MetaApi
import ru.mlbb.blocker.data.meta.MetaLane
import ru.mlbb.blocker.data.meta.MetaResult
import ru.mlbb.blocker.data.meta.MetaTier

class MetaApiTest {

    private val payload = """
        {
          "source": "mlbb-meta",
          "region": "world",
          "regional": false,
          "patch": "1.9.44",
          "updatedAt": "2026-10-30T10:00:00.000Z",
          "count": 2,
          "heroes": [
            {
              "id": "aamon",
              "nameEn": "Aamon",
              "nameRu": "Аамон",
              "role": "assassin",
              "lane": "jungle",
              "lanes": ["jungle", "mid"],
              "imageUrl": "/heroes/aamon.png",
              "winRate": 51.2,
              "pickRate": 3.4,
              "banRate": 12.1,
              "matches": 1234,
              "tier": "S",
              "sortOrder": 1
            },
            {
              "id": "balmond",
              "nameEn": "Balmond",
              "nameRu": "",
              "role": "fighter",
              "lane": "exp",
              "lanes": [],
              "imageUrl": "https://cdn.example.com/balmond.png",
              "winRate": null,
              "pickRate": 1.0,
              "banRate": null,
              "matches": null,
              "tier": "A+",
              "sortOrder": 9
            }
          ]
        }
    """.trimIndent()

    @Test
    fun parsesHeroesAndResolvesImages() {
        val result = MetaApi("https://site.example").parseMeta(payload, "world")
        assertTrue(result is MetaResult.Success)
        val snapshot = (result as MetaResult.Success).value
        assertEquals(2, snapshot.heroes.size)
        assertEquals("1.9.44", snapshot.patch)

        val aamon = snapshot.heroes.first()
        assertEquals("Аамон", aamon.displayName)
        assertEquals(MetaTier.S, aamon.tier)
        assertEquals(listOf(MetaLane.JUNGLE, MetaLane.MID), aamon.lanes)
        assertEquals("https://site.example/heroes/aamon.png", aamon.imageUrl)
        assertEquals(51.2, aamon.winRate!!, 0.001)

        val balmond = snapshot.heroes.last()
        assertEquals("Balmond", balmond.displayName)
        assertEquals(MetaTier.A, balmond.tier)
        assertEquals("https://cdn.example.com/balmond.png", balmond.imageUrl)
        assertEquals(null, balmond.winRate)
    }

    @Test
    fun rejectsEmptyHeroList() {
        val result = MetaApi("https://site.example")
            .parseMeta("""{"heroes":[]}""", "world")
        assertTrue(result is MetaResult.Failure)
    }

    @Test
    fun rejectsMalformedJson() {
        val result = MetaApi("https://site.example").parseMeta("<html>oops", "world")
        assertTrue(result is MetaResult.Failure)
    }

    @Test
    fun normalizesTierValues() {
        assertEquals(MetaTier.S, MetaTier.from("S+"))
        assertEquals(MetaTier.S, MetaTier.from("s"))
        assertEquals(MetaTier.A, MetaTier.from("A-"))
        assertEquals(MetaTier.C, MetaTier.from(null))
        assertEquals(MetaTier.C, MetaTier.from("unknown"))
    }

    @Test
    fun requiresConfiguredBaseUrl() {
        val result = MetaApi("").fetchMeta("world")
        assertTrue(result is MetaResult.Failure)
    }
}
