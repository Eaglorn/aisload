package ru.fku.aisload

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.stream.JsonReader
import java.io.FileReader
import java.nio.file.Files
import java.nio.file.Paths

class Config {
    var FTP_SERVER: String = ""
    var FTP_USER: String = ""
    var FTP_PASSWORD: String = ""
    var FTP_DIRECTORY_PROM: String = ""
    var LOCAL_DIRECTORY_PROM: String = ""
    var FTP_DIRECTORY_OE: String = ""
    var LOCAL_DIRECTORY_OE : String = ""
    var MAX_FOLDERS_TO_KEEP: Int = 0

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