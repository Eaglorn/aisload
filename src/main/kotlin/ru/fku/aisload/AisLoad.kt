package ru.fku.aisload

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationContext
import org.springframework.scheduling.annotation.EnableScheduling
import ru.fku.aisload.AisLoadApplication.Companion.applicationContext
import ru.fku.aisload.AisLoadApplication.Companion.config

@SpringBootApplication(scanBasePackages = ["ru.fku.aisload"])
@EnableScheduling
open class AisLoadApplication {
    private val logger = LoggerFactory.getLogger(FtpService::class.java)

    companion object {
        lateinit var config: Config
        lateinit var applicationContext: ApplicationContext
        var loadProm = false;
        var loadOe = false;
    }
}

val logger: Logger = LoggerFactory.getLogger("Main")

fun main(args: Array<String>) {
    if (args.isNotEmpty()) {
        config = Config.load(args[0])
    }
    applicationContext = runApplication<AisLoadApplication>()
}