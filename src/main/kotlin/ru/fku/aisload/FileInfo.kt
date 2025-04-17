package ru.fku.aisload

data class FileInfo(
    val name: String,
    val year: Int,
    val month: Int,
    val day: Int,
    val buildNumber: Int
) : Comparable<FileInfo> {
    override fun compareTo(other: FileInfo): Int {
        return compareValuesBy(
            this, other,
            { it.year },
            { it.month },
            { it.day },
            { it.buildNumber }
        )
    }
}
