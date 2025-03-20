package ru.fku.aisload

import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPFile
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileOutputStream

@Service
class FtpService {
    private val logger = LoggerFactory.getLogger(FtpService::class.java)

    fun checkAndDownload(pathFtp: String, pathLocal: String) {
        val config: Config = AisLoadApplication.config
        val ftpClient = FTPClient()
        try {
            ftpClient.connect(config.ftpServer)
            ftpClient.login(config.ftpUser, config.ftpPassword)
            ftpClient.enterLocalPassiveMode()
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE)
            val folders = ftpClient.listFiles(pathFtp)
                .filter { it.isDirectory && it.name.matches(Regex("\\d+_\\d+_\\d+_\\d+")) }
                .mapNotNull { parseFolderName(it.name) }
                .sortedByDescending { it }
                .take(config.maxFolderToKeep)
            for (folder in folders) {
                downloadFolder(ftpClient, "$pathFtp/${folder.name}", pathLocal)
            }
            val localFiles = File(pathLocal).listFiles()
            if (localFiles != null) {
                val filesToDelete = localFiles
                    .filter { it.isFile && (it.name.endsWith(".rar") || it.name.endsWith(".zip")) }
                    .sortedByDescending { it.lastModified() }
                    .drop(config.maxFolderToKeep)
                for (file in filesToDelete) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            logger.error(e.message)
        } finally {
            try {
                ftpClient.logout()
                ftpClient.disconnect()
            } catch (e: Exception) {
                logger.error(e.message)
            }
        }
    }

    private fun downloadFolder(ftpClient: FTPClient, remotePath: String, pathLocal: String) {
        val files = ftpClient.listFiles(remotePath)
        for (file in files) {
            val remoteFilePath = "$remotePath/${file.name}"
            if (file.isDirectory && file.name.startsWith("EKP")) {
                val ekpFiles = ftpClient.listFiles(remoteFilePath)
                for (ekpFile in ekpFiles) {
                    val ekpRemoteFilePath = "$remoteFilePath/${ekpFile.name}"
                    val ekpLocalFilePath = File(pathLocal, ekpFile.name)
                    if (!ekpFile.isDirectory && (ekpFile.name.endsWith(".rar") || ekpFile.name.endsWith(".zip"))) {
                        try {
                            if (!ekpLocalFilePath.exists()) {
                                val outputStream = FileOutputStream(ekpLocalFilePath)
                                ftpClient.retrieveFile(ekpRemoteFilePath, outputStream)
                                outputStream.close()
                            }
                        } catch (e: Exception) {
                            logger.error(e.message)
                        }
                    }
                }
            }
        }
    }

    private fun parseFolderName(name: String): FolderInfo? {
        val parts = name.split("_")
        if (parts.size != 4) return null
        return try {
            FolderInfo(
                name = name,
                year = parts[0].toInt(),
                month = parts[1].toInt(),
                day = parts[2].toInt(),
                buildNumber = parts[3].toInt()
            )
        } catch (e: NumberFormatException) {
            logger.error(e.message)
            null
        }
    }
}