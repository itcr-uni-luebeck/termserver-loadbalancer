package de.uniluebeck.itcr.termserver_loadbalancer

import de.uniluebeck.itcr.termserver_loadbalancer.Storage.Companion.fileFolder
import de.uniluebeck.itcr.termserver_loadbalancer.Storage.Companion.json
import de.uniluebeck.itcr.termserver_loadbalancer.models.Endpoints
import de.uniluebeck.itcr.termserver_loadbalancer.models.LoadBalancerConfiguration
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class Storage {
    companion object {
        val fileFolder = File("storage/").also {
            it.mkdirs()
        }
        val json by lazy { Json { prettyPrint = true } }
        val endpoints by lazy { Endpoints() }
        val loadBalancerConf by lazy { LoadBalancerConfiguration() }
    }
}

abstract class JsonBackedStorage<T> {
    val fileLocation get() = fileFolder.resolve("${this::class.simpleName}.json")
    inline fun <reified T> writeToStorage(data: T) {
        val jsonString = json.encodeToString(data)
        fileLocation.writeText(jsonString)
    }
    inline fun <reified T> readFromStorage(): T {
        val jsonString = fileLocation.readText()
        return json.decodeFromString(jsonString)
    }
}