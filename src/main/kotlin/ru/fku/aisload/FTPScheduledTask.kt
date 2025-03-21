package ru.fku.aisload

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class FTPScheduledTask(val ftpService: FtpService) {
    private val logger = LoggerFactory.getLogger(FtpService::class.java)

    @Scheduled(cron = "0 0 21-23,0-6 * * *")
    fun runFtpTaskProm() {
        if (!AisLoadApplication.loadProm) {
            logger.info("Проверка ПРОМ")
            AisLoadApplication.loadProm = true
            val config: Config = AisLoadApplication.config
            ftpService.checkAndDownload(config.ftpDirectoryProm, config.localDirectoryProm, "PROM")
            AisLoadApplication.loadProm = false
        }
    }

    @Scheduled(cron = "0 0 21-23,0-6 * * *")
    fun runFtpTaskOe() {
        if (!AisLoadApplication.loadOe) {
            logger.info("Проверка ОЕ")
            AisLoadApplication.loadOe = true
            val config: Config = AisLoadApplication.config
            ftpService.checkAndDownload(config.ftpDirectoryOe, config.localDirectoryOe, "OE")
            AisLoadApplication.loadOe = false
        }
    }
}