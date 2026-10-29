package ru.mlbb.blocker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.mlbb.blocker.data.rules.RulesParseResult
import ru.mlbb.blocker.data.rules.RulesParser
import java.io.File

class RulesParserTest {

    private fun config(
        schemaVersion: Int = 1,
        matchPorts: String = "[10003]",
        matchPortRanges: String = "[\"5000:5058\", \"10063:10063\"]",
        safePorts: String = "[30190]",
        rules: String = "[[\"146.185.199.0/24\", \"Новосибирск\"]]",
        profiles: String = profile("moscow", rules),
    ): String = """
        {
          "schemaVersion": $schemaVersion,
          "updatedAt": "2026-10-29",
          "matchPorts": $matchPorts,
          "matchPortRanges": $matchPortRanges,
          "safePorts": $safePorts,
          "startup": [{ "ip": "154.85.91.151", "port": 10003, "label": "main" }],
          "profiles": [$profiles],
          "knownServers": {
            "exact": [{ "ip": "66.253.15.83", "city": "Москва", "kind": "MOSCOW" }],
            "subnets": [{ "prefix": "154.85.91", "city": "Москва", "kind": "MOSCOW" }]
          }
        }
    """.trimIndent()

    private fun profile(id: String, rules: String = "[[\"146.185.199.0/24\", \"Новосибирск\"]]"): String = """
        {
          "id": "$id",
          "title": "Москва",
          "badge": "МСК",
          "description": "test",
          "rules": $rules,
          "allowedCidrs": ["154.85.91.0/24"],
          "blockOtherMatchServers": true
        }
    """.trimIndent()

    @Test
    fun parsesValidConfig() {
        val result = RulesParser.parse(config())
        assertTrue(result is RulesParseResult.Success)
        val parsed = (result as RulesParseResult.Success).config
        assertEquals(1, parsed.schemaVersion)
        assertEquals("2026-10-29", parsed.updatedAt)
        assertEquals(10003, parsed.mainPort)
        assertTrue(10003 in parsed.matchPortSet)
        assertTrue(5058 in parsed.matchPortSet)
        assertTrue(10063 in parsed.matchPortSet)
        assertEquals(setOf(30190), parsed.safePortSet)
        assertEquals("moscow", parsed.profiles.single().id)
        assertEquals("154.85.91.151/32", parsed.startup.single().cidr)
        assertEquals("Москва", parsed.knownExact["66.253.15.83"]?.city)
        assertEquals("Москва", parsed.knownSubnets["154.85.91"]?.city)
    }

    @Test
    fun rejectsUnsupportedSchema() {
        assertTrue(RulesParser.parse(config(schemaVersion = 99)) is RulesParseResult.Failure)
    }

    @Test
    fun rejectsInvalidCidr() {
        val result = RulesParser.parse(config(rules = "[[\"146.185.199.999/24\", \"bad\"]]"))
        assertTrue(result is RulesParseResult.Failure)
    }

    @Test
    fun rejectsPortThatIsBothMatchAndSafe() {
        assertTrue(RulesParser.parse(config(safePorts = "[10003]")) is RulesParseResult.Failure)
    }

    @Test
    fun rejectsInvalidPortRange() {
        assertTrue(
            RulesParser.parse(config(matchPortRanges = "[\"5058:5000\"]")) is RulesParseResult.Failure,
        )
    }

    @Test
    fun rejectsEmptyMatchPorts() {
        assertTrue(RulesParser.parse(config(matchPorts = "[]")) is RulesParseResult.Failure)
    }

    @Test
    fun rejectsDuplicateProfileIds() {
        val duplicated = config(profiles = "${profile("moscow")}, ${profile("moscow")}")
        assertTrue(RulesParser.parse(duplicated) is RulesParseResult.Failure)
    }

    @Test
    fun rejectsUnknownServerKind() {
        val broken = config().replace("\"kind\": \"MOSCOW\"", "\"kind\": \"PLANET\"")
        assertTrue(RulesParser.parse(broken) is RulesParseResult.Failure)
    }

    @Test
    fun bundledAssetIsValid() {
        val file = File("src/main/assets/rules/rules.json")
        if (!file.isFile) return
        val result = RulesParser.parse(file.readText())
        if (result is RulesParseResult.Failure) {
            throw AssertionError("assets/rules.json не проходит проверку: ${result.errors}")
        }
        val parsed = (result as RulesParseResult.Success).config
        assertEquals(3, parsed.profiles.size)
        assertTrue(10003 in parsed.matchPortSet)
        assertTrue(30190 in parsed.safePortSet)
        assertTrue(parsed.startup.any { it.ip == "154.85.91.151" && it.port == 10003 })
    }
}
