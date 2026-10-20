package ru.mlbb.blocker.data

data class BlockPreset(
    val id: String,
    val title: String,
    val description: String,
    val rules: List<Pair<String, String>>,
)

object Presets {

    val MOSCOW = BlockPreset(
        id = "moscow",
        title = "Москва",
        description = "Матч-сервер 154.85.91.0/24 и два московских адреса из вашего PCAP.",
        rules = RegionProfiles.MOSCOW.rules,
    )

    val NON_FAR_EAST = BlockPreset(
        id = "non_far_east",
        title = "Всё, кроме Хабаровска",
        description = "Москва + Екатеринбург, Новосибирск, Ростов, Абакан, Франкфурт, Тбилиси, Вьентьян. Хабаровск 92.223.80.0/24 не блокируется.",
        rules = RegionProfiles.FAR_EAST.rules,
    )
}
