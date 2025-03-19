package ru.fku.aisload

import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileOutputStream

@Service
class FtpService {
    @Suppress("unused")
    private val logger = LoggerFactory.getLogger(FtpService::class.java)

    fun checkAndDownloadNewFolders(pathFtp: String, pathLocalString: String) {
        val config: Config = AisLoadApplication.config
        val ftpClient = FTPClient()
        try {
            ftpClient.connect(config.FTP_SERVER)
            ftpClient.login(config.FTP_USER, config.FTP_PASSWORD)
            ftpClient.enterLocalPassiveMode()
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE)

            val folders = ftpClient.listFiles(pathFtp)
                .filter { it.isDirectory && it.name.matches(Regex("\\d+_\\d+_\\d+_\\d+")) }
                .sortedBy { it.name }

            for (folder in folders) {
                val folderName = folder.name
                val localFolder = File("$pathLocalString/$folderName")
                if (!localFolder.exists()) {
                    downloadFolderRecursively(ftpClient, "$pathFtp/$folderName", localFolder)
                }
            }

            val localFolders = File(pathLocalString).listFiles()
                ?.filter { it.isDirectory && it.name.matches(Regex("\\d+_\\d+_\\d+_\\d+")) }
                ?.sortedBy { it.name }
                ?.toList() ?: emptyList()

            if (localFolders.size > config.MAX_FOLDERS_TO_KEEP) {
                val foldersToDelete = localFolders.take(localFolders.size - config.MAX_FOLDERS_TO_KEEP)
                for (folder in foldersToDelete) {
                    folder.deleteRecursively()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                ftpClient.logout()
                ftpClient.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun downloadFolderRecursively(ftpClient: FTPClient, remotePath: String, localFolder: File) {
        localFolder.mkdirs()
        val files = ftpClient.listFiles(remotePath)
        for (file in files) {
            val remoteFilePath = "$remotePath/${file.name}"
            val localFilePath = File(localFolder, file.name)
            if (file.isDirectory) {
                downloadFolderRecursively(ftpClient, remoteFilePath, localFilePath)
            } else {
                val outputStream = FileOutputStream(localFilePath)
                ftpClient.retrieveFile(remoteFilePath, outputStream)
                outputStream.close()
            }
        }
    }
}