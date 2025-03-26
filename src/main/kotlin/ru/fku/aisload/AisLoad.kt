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
    companion object {
        var config: Config? = null
        lateinit var applicationContext: ApplicationContext
        var isLoad = false
    }
}

val logger: Logger = LoggerFactory.getLogger("Main")

fun main(args: Array<String>) {
    if (args.isNotEmpty()) {
        config = Config.load(args[0])
        if (config != null) {
            applicationContext = runApplication<AisLoadApplication>()
        } else {
            logger.error("Config not laded.")
        }
    }
}
