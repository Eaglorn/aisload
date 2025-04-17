package ru.fku.aisload

import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPFile
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.charset.Charset
import java.security.MessageDigest

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
                .sortedWith(compareByDescending<FolderInfo> { it.year }
                    .thenByDescending { it.month }
                    .thenByDescending { it.day }
                    .thenByDescending { it.buildNumber }
                )
                .take(config?.maxFolderToKeep ?: 1)

            for (folder in folders) {
                downloadFolder(ftpClient, "$pathFtp/${folder.name}", pathLocal, appName)
            }
            val localFiles = File(pathLocal).listFiles()
            if (localFiles != null) {
                val filesToDelete = localFiles
                    .filter { it.isFile && (it.name.endsWith(".rar") || it.name.endsWith(".zip")) }
                    .mapNotNull { parseFileName(it.name) }
                    .sortedWith(compareByDescending<FileInfo> { it.year }
                        .thenByDescending { it.month }
                        .thenByDescending { it.day }
                        .thenByDescending { it.buildNumber }
                    )
                    .mapNotNull { fileInfo -> localFiles.find { it.name == fileInfo.name } }
                    .drop(config?.maxFolderToKeep ?: 99)

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

    fun calculateFileHash(file: File, algorithm: String = "SHA-1"): String {
        val digest = MessageDigest.getInstance(algorithm)
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun downloadFile(
        ftpClient: FTPClient,
        ftpFile: FTPFile,
        localFile: File,
        remoteFilePath: String,
        appName: String
    ) {
        val maxRetries: Int = AisLoadApplication.config?.maxRetries ?: 3
        var retryDelayMin: Long = AisLoadApplication.config?.retryDelayMin ?: 5
        retryDelayMin = retryDelayMin.times(60000)
        var attempt = 0
        while (attempt < maxRetries) {
            try {
                logger.info("Load start: $appName - ${ftpFile.name} (Attempt ${attempt + 1})")
                val outputStream = FileOutputStream(localFile)
                ftpClient.retrieveFile(remoteFilePath, outputStream)
                outputStream.close()
                localFile.setLastModified(ftpFile.timestamp.timeInMillis)
                logger.info("Load end: $appName - ${ftpFile.name}")
                return
            } catch (e: Exception) {
                attempt++
                logger.error("Error downloading file: ${ftpFile.name}. Attempt $attempt of $maxRetries. Error: ${e.message}")
                if (attempt >= maxRetries) {
                    logger.error("Max retries reached. Failed to download file: ${ftpFile.name}")
                    return
                }
                Thread.sleep(retryDelayMin)
            }
        }
    }

    private fun downloadFileToString(ftpClient: FTPClient, remoteFilePath: String, charset: Charset = Charsets.UTF_8): String {
        try {
            val byteArrayOutputStream = ByteArrayOutputStream()
            ftpClient.retrieveFile(remoteFilePath, byteArrayOutputStream)
            return byteArrayOutputStream.toString(charset.name())
        } catch (e: Exception) {
            logger.error(e.message)
            return ""
        }
    }

    private fun downloadFolder(ftpClient: FTPClient, remotePath: String, pathLocal: String, appName: String) {
        try {
            val files = ftpClient.listFiles(remotePath)
            for (file in files) {
                val remoteFilePathParent = "$remotePath/${file.name}"
                if (file.isDirectory && file.name.startsWith("EKP")) {
                    val ftpFiles = ftpClient.listFiles(remoteFilePathParent)
                    for (ftpFile in ftpFiles) {
                        val remoteFilePathChild = "$remoteFilePathParent/${ftpFile.name}"
                        val localFile = File(pathLocal, ftpFile.name)
                        if (ftpFile.isFile && (ftpFile.name.endsWith(".rar") || ftpFile.name.endsWith(".zip"))) {
                            try {
                                if (!localFile.exists()) {
                                    logger.info("Find version: $appName - ${ftpFile.name}")
                                    downloadFile(ftpClient, ftpFile, localFile, remoteFilePathChild, appName)
                                } else {
                                    if (localFile.isFile) {
                                        val ftpFileSha = ftpFiles.filter { it -> it.name.contains("sha") && it.name.contains("Client")}
                                        val shaFtp = downloadFileToString(ftpClient, "$remoteFilePathParent/${ftpFileSha.first().name}").split(" ").first()
                                        val shaLocal = calculateFileHash(localFile)
                                        if(shaFtp != shaLocal) {
                                            println("Check error(hash): $appName - (ftp $shaFtp | local $shaLocal)")
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
                                            if (reload) downloadFile(ftpClient, ftpFile, localFile, remoteFilePathChild, appName)
                                        }
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

    private fun parseFileName(name: String): FileInfo? {
        val partsFullName = name.split("_")
        if (partsFullName.size != 3) return null

        val parts = partsFullName[2].split(".")
        return try {
            FileInfo(
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
