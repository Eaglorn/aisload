package ru.fku.aisload

data class FolderInfo(
    val name: String,
    val year: Int,
    val month: Int,
    val day: Int,
    val buildNumber: Int
) : Comparable<FolderInfo> {
    override fun compareTo(other: FolderInfo): Int {
        return compareValuesBy(
            this, other,
            { it.year },
            { it.month },
            { it.day },
            { it.buildNumber }
        )
    }
}