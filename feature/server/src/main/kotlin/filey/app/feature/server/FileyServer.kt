package filey.app.feature.server

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.util.*

class FileyServer(private val context: Context) {

    private var serverSocket: ServerSocket? = null
    private var isRunning = false

    fun getIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            for (intf in interfaces) {
                val addrs = intf.inetAddresses
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress) {
                        val sAddr = addr.hostAddress
                        val isIPv4 = sAddr.indexOf(':') < 0
                        if (isIPv4) return sAddr
                    }
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return "127.0.0.1"
    }

    suspend fun start(port: Int = 8080) = withContext(Dispatchers.IO) {
        try {
            serverSocket = ServerSocket(port)
            isRunning = true
            while (isRunning) {
                val client = serverSocket?.accept() ?: break
                handleClient(client)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stop() {
        isRunning = false
        serverSocket?.close()
        serverSocket = null
    }

    private fun handleClient(socket: Socket) {
        Thread {
            try {
                val input = BufferedReader(InputStreamReader(socket.getInputStream()))
                val line = input.readLine() ?: return@Thread
                val parts = line.split(" ")
                if (parts.size < 2) return@Thread
                
                val rawPath = URLDecoder.decode(parts[1], "UTF-8")
                val path = if (rawPath == "/") "/storage/emulated/0" else rawPath
                
                val file = File(path)
                val output = DataOutputStream(socket.getOutputStream())

                if (file.exists()) {
                    if (file.isDirectory) {
                        sendDirectoryListing(output, file)
                    } else {
                        sendFile(output, file)
                    }
                } else {
                    sendError(output, "404 Not Found")
                }
                socket.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun sendDirectoryListing(out: DataOutputStream, dir: File) {
        val html = buildString {
            append("<html><head><meta charset='UTF-8'><title>Filey Server</title>")
            append("<style>body{font-family:sans-serif;padding:20px} a{display:block;padding:8px;text-decoration:none;border-bottom:1px solid #eee} a:hover{background:#f5f5f5}</style>")
            append("</head><body>")
            append("<h1>${dir.name}</h1>")
            append("<a href='${dir.parent ?: "/"}'>.. [Üst Dizin]</a>")
            dir.listFiles()?.sortedBy { !it.isDirectory }?.forEach { file ->
                val icon = if (file.isDirectory) "📁" else "📄"
                append("<a href='${file.absolutePath}'>$icon ${file.name}</a>")
            }
            append("</body></html>")
        }
        val header = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html\r\n" +
                "Content-Length: ${html.length}\r\n" +
                "\r\n"
        out.writeBytes(header)
        out.writeBytes(html)
    }

    private fun sendFile(out: DataOutputStream, file: File) {
        val header = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: application/octet-stream\r\n" +
                "Content-Length: ${file.length()}\r\n" +
                "Content-Disposition: attachment; filename=\"${file.name}\"\r\n" +
                "\r\n"
        out.writeBytes(header)
        file.inputStream().use { it.copyTo(out) }
    }

    private fun sendError(out: DataOutputStream, error: String) {
        out.writeBytes("HTTP/1.1 $error\r\n\r\n")
    }
}
