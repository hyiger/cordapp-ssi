package com.derivativepath.analytics.plugin

import com.derivativepath.analytics.api.SettlementApi
import net.corda.webserver.services.WebServerPluginRegistry
import java.util.function.Function

class SettlementPlugin : WebServerPluginRegistry {
    /**
     * A list of classes that expose web APIs.
     */
    override val webApis = listOf(Function(::SettlementApi))

    /**
     * A list of directories in the resources directory that will be served by Jetty under /web.
     */
    override val staticServeDirs = mapOf(
            // This will serve the exampleWeb directory in resources to /web/example
            "example" to javaClass.classLoader.getResource("exampleWeb").toExternalForm()
    )
}
