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
    var ftpDirectoryProm: String = ""
    var localDirectoryProm: String = ""
    var ftpDirectoryOe: String = ""
    var localDirectoryOe: String = ""
    var maxFolderToKeep: Int = 0

    companion object {
        fun load(path: String): Config {
            return if (Files.exists(Paths.get(path))) {
                val gson: Gson = GsonBuilder().create()
                val configServer: Config =
                    gson.fromJson(JsonReader(FileReader(path)), Config::class.java)
                configServer
            } else {
                Config()
            }
        }
    }
}