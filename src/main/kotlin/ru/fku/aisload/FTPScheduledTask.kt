package ru.fku.aisload

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class FTPScheduledTask(val ftpService: FtpService) {
    private val logger = LoggerFactory.getLogger(FtpService::class.java)

    @Scheduled(cron = "0 0 21-23,0-7 * * *")
    fun runFtpTaskProm() {
        if (!AisLoadApplication.loadProm) {
            AisLoadApplication.loadProm = true
            val config: Config = AisLoadApplication.config
            ftpService.checkAndDownload(config.ftpDirectoryProm, config.localDirectoryProm)
            AisLoadApplication.loadProm = false
        }
    }

    @Scheduled(cron = "0 0 21-23,0-7 * * *")
    fun runFtpTaskOe() {
        if (!AisLoadApplication.loadOe) {
            AisLoadApplication.loadOe = true
            val config: Config = AisLoadApplication.config
            ftpService.checkAndDownload(config.ftpDirectoryOe, config.localDirectoryOe)
            AisLoadApplication.loadOe = false
        }
    }
}