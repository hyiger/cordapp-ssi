package com.derivativepath.analytics.client

import com.derivativepath.analytics.states.BilateralSettlementState
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.contracts.StateAndRef
import net.corda.core.utilities.NetworkHostAndPort
import net.corda.core.utilities.loggerFor
import org.slf4j.Logger

/**
 *  Demonstration of using the CordaRPCClient to connect to a Corda Node and
 *  steam some State data from the node.
 **/

fun main(args: Array<String>) {
    SettlementClientRPC().main(args)
}

private class SettlementClientRPC {
    companion object {
        val logger: Logger = loggerFor<SettlementClientRPC>()
        private fun logState(stateBilateral: StateAndRef<BilateralSettlementState>) = logger.info("{}", stateBilateral.state.data)
    }

    fun main(args: Array<String>) {
        require(args.size == 1) { "Usage: SettlementClientRPC <node address>" }
        val nodeAddress = NetworkHostAndPort.parse(args[0])
        val client = CordaRPCClient(nodeAddress)

        // Can be amended in the com.example.MainKt file.
        val proxy = client.start("user1", "test").proxy

        // Grab all existing and future Settlements states in the vault.
        val (snapshot, updates) = proxy.vaultTrack(BilateralSettlementState::class.java)

        // Log the 'placed' Settlement instruction states and listen for new ones.
        snapshot.states.forEach { logState(it) }
        updates.toBlocking().subscribe { update ->
            update.produced.forEach { logState(it) }
        }
    }
}
