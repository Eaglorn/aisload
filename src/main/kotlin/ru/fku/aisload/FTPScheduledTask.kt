package ru.fku.aisload

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Suppress("unused")
@Component
class FTPScheduledTask (val ftpService: FtpService) {
    private val logger = LoggerFactory.getLogger(FtpService::class.java)

    @Suppress("unused")
    @Scheduled(cron = "* * * * * *")
    fun runFtpTaskProm() {
        val config: Config = AisLoadApplication.config
        ftpService.checkAndDownloadNewFolders(config.FTP_DIRECTORY_PROM, config.LOCAL_DIRECTORY_PROM)
    }

    @Suppress("unused")
    @Scheduled(cron = "* * * * * *")
    fun runFtpTaskOe() {
        val config: Config = AisLoadApplication.config
        ftpService.checkAndDownloadNewFolders(config.FTP_DIRECTORY_OE, config.LOCAL_DIRECTORY_OE)
    }
}