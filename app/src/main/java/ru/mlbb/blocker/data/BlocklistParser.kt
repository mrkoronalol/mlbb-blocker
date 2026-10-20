package ru.mlbb.blocker.data

object BlocklistParser {

    fun parseLines(text: String): List<String> {
        return text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") && !it.startsWith("//") }
            .mapNotNull { line ->
                val withoutComment = line.substringBefore('#').substringBefore("//").trim()
                Cidr.normalize(withoutComment)
            }
            .distinct()
            .toList()
    }
}
