import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.Charset
import java.util.*
import kotlin.collections.HashSet
import kotlin.concurrent.thread

object Server {
    private val serverSocket = ServerSocket(9999)
    val clientHandlers = HashSet<ClientHandler>()
    //private val mutex = Mutex()

    fun run() {
        while (true) {
            val clientSocket = serverSocket.accept()
            val clientHandler = ClientHandler(clientSocket)
            println(clientHandler.toString())
            clientHandlers.add(clientHandler)
            //mutex.unlock()
            thread { clientHandler.run() }
        }
    }

    fun sendToEveryoneElse(clientHandler: ClientHandler, text: String) {
        thread {
            //mutex.lock()
            val handlers = HashSet<ClientHandler>(clientHandlers)
            //mutex.unlock()
            for (handler in handlers)
                if (handler != clientHandler)
                    handler.write("$clientHandler: $text")
        }
    }
}

class ClientHandler(private val clientSocket: Socket) {
    private val reader: Scanner = Scanner(clientSocket.getInputStream())
    private val writer: OutputStream = clientSocket.getOutputStream()
    private var running: Boolean = false

    fun run() {
        running = true
        // Welcome message
        write(
            "Welcome to the server!\n" +
                    "To Exit, write: 'EXIT'.\n" +
                    "To send message to everyone else, write: 'SEND' your_text"
        )

        while (running) {
            try {
                val text = reader.nextLine()
                if (text == "EXIT") {
                    shutdown()
                    continue
                }
                if (text.startsWith("SEND")) {
                    if (text.length <= 5)
                        continue
                    val values = text.substring(5)
                    Server.sendToEveryoneElse(this, values)
                }

            } catch (ex: Exception) {
                shutdown()
            } finally {

            }

        }
    }

    fun write(message: String) {
        writer.write((message + '\n').toByteArray(Charset.defaultCharset()))
    }

    private fun shutdown() {
        running = false
        clientSocket.close()
        println("${clientSocket.inetAddress.hostAddress} closed the connection")
        Server.clientHandlers.remove(this)
    }
}

fun main(args: Array<String>) {
    Server.run()
}