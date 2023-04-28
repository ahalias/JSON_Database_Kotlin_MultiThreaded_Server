package jsondatabase.server

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import kotlin.system.exitProcess
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File



class Db(private val server: ServerSocket) : Thread() {
    private val socket = server.accept()
    private val input = DataInputStream(socket.getInputStream())
    private val output = DataOutputStream(socket.getOutputStream())
    private val dir = File("src/server/data")
    val dbJSON = File("${dir}/db.json")



    override fun run() {
        try {
            input.use { input ->
                output.use { output ->
                    val jsonMessage = input.readUTF()
                    val inMessage = Json.decodeFromString<Map<String, String>>(jsonMessage)
                    menu(inMessage)
                    socket.close()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun serializeAndSend(response: String, param: String) {
        var message = ""
        if (response == "ERROR") {
            val processResponse = mapOf("response" to response, "reason" to param)
            message = Json.encodeToString(processResponse)
        } else if (response == "OK" && param.isNotBlank()) {
            val processResponse = mapOf("response" to response, "value" to param)
            message = Json.encodeToString(processResponse)
        } else {
            message = Json.encodeToString(mapOf("response" to "OK"))
        }
        output.writeUTF(message)
    }

    fun menu (inMessage: Map<String, String>) {
        when(inMessage["type"]!!.uppercase()) {
            "EXIT" -> {
                output.writeUTF("OK")
                output.close()
                input.close()
                socket.close()
                server.close()
                exitProcess(1)
            }
            "SET" -> {
                try {
                    val entry = Json.encodeToString<Map<String, String>>(mapOf(inMessage["key"]!! to inMessage["value"]!!))
                    dbJSON.writeText(entry)
                    serializeAndSend("OK", "")
                } catch(e: Exception) {
                    serializeAndSend("ERROR", "No such key")
                }
            }
            "GET" -> {
                val jsonStr = dbJSON.readText()
                val json = Json.parseToJsonElement(jsonStr).jsonObject
                var getParam = ""
                if (json.containsKey(inMessage["key"])) {
                    getParam = json[inMessage["key"]]!!.jsonPrimitive.content
                    serializeAndSend("OK", getParam)
                } else {
                    serializeAndSend("ERROR", "No such key")
                }
            }

            "DELETE" -> {
                val jsonStr = dbJSON.readText()
                val json = Json.parseToJsonElement(jsonStr).jsonObject

                if (json.containsKey(inMessage["key"])) {
                    val mutableJson = json.toMutableMap()
                    mutableJson.remove(inMessage["key"])
                    val newJsonStr = Json.encodeToString(JsonObject(mutableJson))
                    dbJSON.writeText(newJsonStr)
                    serializeAndSend("OK", "")
                } else {
                    serializeAndSend("ERROR", "No such key")
                }


            }
        }
    }
}


fun main(args: Array<String>) {

    val address = "127.0.0.1"
    val port = 23456

    try {
        ServerSocket(port, 50, InetAddress.getByName(address)).use { server ->
            println("Server started!")
            while (true) {
                val session = Db(server)
                session.start()
            }
        }
    } catch (e: IOException) {
        e.printStackTrace()
    }
}