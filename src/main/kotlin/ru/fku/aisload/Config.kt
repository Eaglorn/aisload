package ru.fku.aisload

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.stream.JsonReader
import java.io.FileReader
import java.nio.file.Files
import java.nio.file.Paths

class Config {
    var ftpServer: String = ""
    var ftpUser: String = ""
    var ftpPassword: String = ""
    var directories: ArrayList<DirectoryPath> = ArrayList()
    var maxFolderToKeep: Int = 0

    companion object {
        fun load(path: String): Config? {
            if (Files.exists(Paths.get(path))) {
                val gson: Gson = GsonBuilder().create()
                try {
                    val configServer: Config =
                        gson.fromJson(JsonReader(FileReader(path)), Config::class.java)
                    logger.info("Config loaded.")
                    return configServer
                } catch (e: Exception) {
                    logger.error(e.message)
                    return null
                }
            } else {
                return null
            }
        }
    }
}