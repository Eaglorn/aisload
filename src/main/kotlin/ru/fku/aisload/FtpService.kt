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

    fun checkAndDownload(pathFtp: String, pathLocal: String, appName: String) {
        val config: Config? = AisLoadApplication.config
        val ftpClient = FTPClient()
        try {
            ftpClient.connect(config?.ftpServer)
            ftpClient.login(config?.ftpUser, config?.ftpPassword)
            ftpClient.enterLocalPassiveMode()
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE)
            val folders = ftpClient.listFiles(pathFtp)
                .filter { it.isDirectory && it.name.matches(Regex("\\d+_\\d+_\\d+_\\d+")) }
                .mapNotNull { parseFolderName(it.name) }
                .sortedByDescending { it.name }
                .take(config?.maxFolderToKeep ?: 999)
            for (folder in folders) {
                downloadFolder(ftpClient, "$pathFtp/${folder.name}", pathLocal, appName)
            }
            val localFiles = File(pathLocal).listFiles()
            if (localFiles != null) {
                val filesToDelete = localFiles
                    .filter { it.isFile && (it.name.endsWith(".rar") || it.name.endsWith(".zip")) }
                    .sortedByDescending { it.name }
                    .drop(config?.maxFolderToKeep ?: 999)
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

    private fun downloadFile(
        ftpClient: FTPClient,
        ftpFile: FTPFile,
        localFile: File,
        remoteFilePath: String,
        appName: String
    ) {
        logger.info("Load start: $appName - ${ftpFile.name}")
        val outputStream = FileOutputStream(localFile)
        ftpClient.retrieveFile(remoteFilePath, outputStream)
        outputStream.close()
        localFile.setLastModified(ftpFile.timestamp.timeInMillis)
        logger.info("Load end: $appName - ${ftpFile.name}")
    }

    private fun downloadFolder(ftpClient: FTPClient, remotePath: String, pathLocal: String, appName: String) {
        try {
            val files = ftpClient.listFiles(remotePath)
            for (file in files) {
                val remoteFilePath = "$remotePath/${file.name}"
                if (file.isDirectory && file.name.startsWith("EKP")) {
                    val ftpFiles = ftpClient.listFiles(remoteFilePath)
                    for (ftpFile in ftpFiles) {
                        val remoteFilePath = "$remoteFilePath/${ftpFile.name}"
                        val localFile = File(pathLocal, ftpFile.name)
                        if (ftpFile.isFile && (ftpFile.name.endsWith(".rar") || ftpFile.name.endsWith(".zip"))) {
                            try {
                                if (!localFile.exists()) {
                                    logger.info("Find version: $appName - ${ftpFile.name}")
                                    downloadFile(ftpClient, ftpFile, localFile, remoteFilePath, appName)
                                } else {
                                    if (localFile.isFile) {
                                        var reload = false
                                        val localFileSize: Long = localFile.length()
                                        val localFileTime: Long = localFile.lastModified()
                                        val ftpFileSize: Long = ftpFile.size
                                        val ftpFileTime: Long = ftpFile.timestamp.timeInMillis
                                        if (localFileSize != ftpFileSize) {
                                            logger.info("Check error(size): $appName - (ftp $ftpFileSize | local $localFileSize)")
                                            reload = true
                                        }
                                        if (localFileTime != ftpFileTime) {
                                            reload = true
                                            logger.info("Check error(last modified): $appName - (ftp $ftpFileTime | local $localFileTime)")
                                        }
                                        if (reload) downloadFile(ftpClient, ftpFile, localFile, remoteFilePath, appName)
                                    }
                                }
                            } catch (e: Exception) {
                                logger.error(e.message)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            logger.error(e.message)
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