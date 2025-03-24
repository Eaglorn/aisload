package ru.fku.aisload

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class FTPScheduledTask(val ftpService: FtpService) {
    private val logger = LoggerFactory.getLogger(FtpService::class.java)

    @Scheduled(cron = "0 0 21-23,0-6 * * *")
    fun runFtpTask() {
        try {
            if (!AisLoadApplication.isLoad) {
                AisLoadApplication.isLoad = true
                val config: Config? = AisLoadApplication.config
                config?.directories?.forEach {
                    ftpService.checkAndDownload(it.ftp + "/", it.local + "/", it.name)
                }
                AisLoadApplication.isLoad = false
            }
        } catch (e: Exception) {
            logger.error(e.message)
        }
    }

}