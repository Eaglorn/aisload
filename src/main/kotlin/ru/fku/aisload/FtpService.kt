package ru.fku.aisload

import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
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

            for (folder in folders) {
                val folderName = folder.name
                val localFolder = File("$pathLocal/$folderName")
                if (!localFolder.exists()) {
                    downloadFolder(ftpClient, "$pathFtp/$folderName", localFolder)
                }
            }

            val localFolders = File(pathLocal).listFiles()!!
                .filter { it.isDirectory && it.name.matches(Regex("\\d+_\\d+_\\d+_\\d+")) }
                .mapNotNull { parseFolderName(it.name) }
                .sortedByDescending { it }
                .toList()

            if (localFolders.size > config.maxFolderToKeep) {
                val foldersToDelete = localFolders.take(localFolders.size - config.maxFolderToKeep)
                for (folder in foldersToDelete) {
                    val folderToDelete = File("$pathLocal/${folder.name}")
                    folderToDelete.deleteRecursively()
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

    private fun downloadFolder(ftpClient: FTPClient, remotePath: String, localFolder: File) {
        localFolder.mkdirs()
        val files = ftpClient.listFiles(remotePath)
        for (file in files) {
            val remoteFilePath = "$remotePath/${file.name}"
            val localFilePath = File(localFolder, file.name)
            if (file.isDirectory && file.name.contains("EKP")) {
                downloadFolder(ftpClient, remoteFilePath, localFilePath)
            } else {
                if(file.name.contains(".rar")) {
                    val outputStream = FileOutputStream(localFilePath)
                    ftpClient.retrieveFile(remoteFilePath, outputStream)
                    outputStream.close()
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
            null
        }
    }
}