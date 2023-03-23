package de.uniluebeck.itcr.termserver_loadbalancer.ui

import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.routing.*
import kotlinx.html.*

fun Route.swaggerRouting() {
    get("/swagger-ui") {
        call.respondHtml {
            lang = "en"
            head {
                meta { charset = "utf-8" }
                meta("viewport") { content = "width=device-width, initial-scale=1, shrink-to-fit=no" }
                meta {
                    content = "Swagger UI"
                    name = "description"
                }
                title { +"Swagger UI" }
                link {
                    rel = "stylesheet"
                    href = "/static/css/swagger-ui.css"
                }
            }
            body {
                div { id = "swagger-ui" }
                script { src = "/static/js/swagger-ui-bundle.js" }
                script { src = "/static/js/swagger-ui-standalone-preset.js" }
                script {
                    unsafe {
                        +"""window.onload = function() { 
                        |   SwaggerUIBundle({ 
                        |       url: '/openapi/documentation.yaml', 
                        |       dom_id: '#swagger-ui',
                        |       plugins: [SwaggerUIBundle.plugins.DownloadUrl]
                        |   })
                        |}""".trimMargin()
                    }
                }
            }
        }
    }
}