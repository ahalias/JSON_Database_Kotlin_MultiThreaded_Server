package jsondatabase.client

import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetAddress
import java.net.Socket
import kotlin.system.exitProcess
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import java.io.File

fun main(args: Array<String>) {

    val dir = File("src/client/data")

    val address = "127.0.0.1"
    val port = 23456

    val socket = Socket(InetAddress.getByName(address), port)
    val input = DataInputStream(socket.getInputStream())
    val output = DataOutputStream(socket.getOutputStream())
    println("Client started!")

    var outMessage = ""

    val command = args[0]
    val type = args[1]
    if (command == "-t") {
        when(type) {
            "exit" -> {
                outMessage = Json.encodeToString(mapOf("type" to "exit"))
            }
            "set" -> {
                outMessage = Json.encodeToString(mapOf("type" to type, "key" to args[3], "value" to args[5]))
            }
            else -> {
                outMessage = Json.encodeToString(mapOf("type" to type, "key" to args[3]))
            }
        }
    } else if (command == "-in") {
        val content = File("${dir}/${args[1]}").readText()
        outMessage = Json.parseToJsonElement(content).jsonObject.toString()


    }



    output.writeUTF(outMessage)
    println("Sent: ${outMessage}")

    val inMessage = input.readUTF()
    println("Received: ${inMessage}")

    socket.close()
    exitProcess(1)
}
