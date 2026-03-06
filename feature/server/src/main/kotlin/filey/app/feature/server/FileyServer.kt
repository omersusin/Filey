package filey.app.feature.server

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.InputStreamReader
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder

class FileyServer(private val context: Context) {

    private var serverSocket: ServerSocket? = null
    private var serverScope: CoroutineScope? = null
    private val storageRoot: String = "/storage/emulated/0"

    fun getIpAddress(): String {
        return try {
            NetworkInterface.getNetworkInterfaces()
                .asSequence()
                .flatMap { it.inetAddresses.asSequence() }
                .firstOrNull { !it.isLoopbackAddress && it.hostAddress.indexOf(':') < 0 }
                ?.hostAddress ?: "127.0.0.1"
        } catch (e: Exception) { "127.0.0.1" }
    }

    suspend fun start(port: Int = 8080) = withContext(Dispatchers.IO) {
        val scope = CoroutineScope(Dispatchers.IO + Job())
        serverScope = scope
        try {
            serverSocket = ServerSocket(port)
            while (scope.isActive) {
                val client = serverSocket?.accept() ?: break
                scope.launch { handleClient(client) }
            }
        } catch (e: Exception) {
            if (scope.isActive) e.printStackTrace()
        }
    }

    fun stop() {
        serverScope?.cancel()
        serverScope = null
        serverSocket?.close()
        serverSocket = null
    }

    private suspend fun handleClient(socket: Socket) = withContext(Dispatchers.IO) {
        try {
            socket.use {
                val input = BufferedReader(InputStreamReader(socket.getInputStream()))
                val line = input.readLine() ?: return@withContext
                val parts = line.split(" ")
                if (parts.size < 2) return@withContext
                val rawPath = URLDecoder.decode(parts[1], "UTF-8")
                val output = DataOutputStream(socket.getOutputStream())
                val safePath = resolveSafePath(rawPath)
                if (safePath == null) { sendError(output, "403 Forbidden"); return@withContext }
                val file = File(safePath)
                when {
                    !file.exists()   -> sendError(output, "404 Not Found")
                    file.isDirectory -> sendDirectoryListing(output, file)
                    else             -> sendFile(output, file)
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun resolveSafePath(urlPath: String): String? {
        val joined = if (urlPath == "/") storageRoot else "$storageRoot/${urlPath.trimStart('/')}"
        val canonical = try { File(joined).canonicalPath } catch (e: Exception) { return null }
        return if (canonical.startsWith(storageRoot)) canonical else null
    }

    private fun sendDirectoryListing(out: DataOutputStream, dir: File) {
        val parentLink = if (dir.absolutePath == storageRoot) "" else {
            val p = dir.parent?.removePrefix(storageRoot) ?: "/"
            "<a href='$p'>.. [Üst Dizin]</a>"
        }
        val html = buildString {
            append("<html><head><meta charset='UTF-8'><title>Filey</title></head><body>")
            append("<h1>${dir.name}</h1>$parentLink")
            dir.listFiles()?.sortedBy { !it.isDirectory }?.forEach { f ->
                val icon = if (f.isDirectory) "📁" else "📄"
                val rel = f.absolutePath.removePrefix(storageRoot)
                append("<a href='$rel'>$icon ${f.name}</a><br>")
            }
            append("</body></html>")
        }
        val bytes = html.toByteArray(Charsets.UTF_8)
        out.writeBytes("HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=UTF-8\r\nContent-Length: ${bytes.size}\r\n\r\n")
        out.write(bytes); out.flush()
    }

    private fun sendFile(out: DataOutputStream, file: File) {
        out.writeBytes("HTTP/1.1 200 OK\r\nContent-Type: application/octet-stream\r\nContent-Length: ${file.length()}\r\nContent-Disposition: attachment; filename=\"${file.name}\"\r\n\r\n")
        file.inputStream().use { it.copyTo(out) }; out.flush()
    }

    private fun sendError(out: DataOutputStream, error: String) {
        out.writeBytes("HTTP/1.1 $error\r\nContent-Length: 0\r\n\r\n"); out.flush()
    }
}
