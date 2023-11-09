package de.uniluebeck.itcr.termserver_loadbalancer.ui.components

import de.uniluebeck.itcr.termserver_loadbalancer.APP_NAME
import de.uniluebeck.itcr.termserver_loadbalancer.APP_VERSION
import de.uniluebeck.itcr.termserver_loadbalancer.logger
import kotlinx.html.*
import java.io.File
import java.io.FileNotFoundException

private fun listFilesInResourceDir(relPath: String, filterExtension: String): List<String> {
    val resource = (object {}).javaClass.getResource(relPath)
    val resourceDir = resource?.file?.let {path ->
        File(path)
    } ?: throw FileNotFoundException("The $relPath resource dir could not be found")
    return resourceDir.listFiles()?.filter {
        it.extension == filterExtension
    }?.map {
        "${relPath.removeSuffix("/")}/${it.name}"
    } ?: throw FileNotFoundException("Could not list $filterExtension files in $relPath")
}

val stylesheets by lazy {
    logger.info("Building CSS links")
    listFilesInResourceDir("/static/css", "css").sorted().onEach {
        logger.debug("Discovered CSS $it")
    }.also {
        logger.info("Found ${it.size} CSS files")
    }
}

val javascript by lazy {
    logger.info("Building Javascript links")
    listFilesInResourceDir("/static/js", "js").sorted().onEach {
        logger.debug("Discovered Javascript $it")
    }.also {
        logger.info("Found ${it.size} JS files")
    }
}

fun HEAD.htmlHeader() {
    title { +"$APP_NAME version $APP_VERSION" }

    javascript.forEach { link ->
        script {
            src = link
        }
    }

    stylesheets.forEach { link ->
        link {
            rel = LinkRel.stylesheet
            href = link
        }
    }
}