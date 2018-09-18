package com.derivativepath.analytics.flow

import com.derivativepath.analytics.states.BilateralSettlementState
import com.derivativepath.analytics.states.SettlementInstruction
import com.derivativepath.analytics.states.SettlementMethod
import net.corda.core.contracts.TransactionVerificationException
import net.corda.core.node.services.queryBy
import net.corda.core.utilities.getOrThrow
import net.corda.testing.core.singleIdentity
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.StartedMockNode
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CreateBilateralSettlementFlowTests {
    lateinit var network: MockNetwork
    lateinit var a: StartedMockNode
    lateinit var b: StartedMockNode

    @Before
    fun setup() {
        network = MockNetwork(listOf("com.derivativepath.analytics.contracts"))
        a = network.createPartyNode()
        b = network.createPartyNode()
        // For real nodes this happens automatically, but we have to manually register the flow for tests.
        listOf(a, b).forEach { it.registerInitiatedFlow(BilateralSettlementCreateFlow.Acceptor::class.java) }
        network.runNetwork()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    @Test
    fun `flow rejects invalid Settlement Instructions`() {
        val instruction = SettlementInstruction(SettlementMethod.ACH, "", "", "")
        val flow = BilateralSettlementCreateFlow.Initiator(instruction, b.info.singleIdentity())
        val future = a.startFlow(flow)
        network.runNetwork()

        // The BilateralSettlementContract specifies that settlement instructions cannot have negative values.
        assertFailsWith<TransactionVerificationException> { future.getOrThrow() }
    }

    @Test
    fun `SignedTransaction returned by the flow is signed by the initiator`() {
        val instruction = SettlementInstruction(SettlementMethod.WIRE, "", "", "")
        val flow = BilateralSettlementCreateFlow.Initiator(instruction, b.info.singleIdentity())
        val future = a.startFlow(flow)
        network.runNetwork()

        val signedTx = future.getOrThrow()
        signedTx.verifySignaturesExcept(b.info.singleIdentity().owningKey)
    }

    @Test
    fun `SignedTransaction returned by the flow is signed by the acceptor`() {
        val instruction = SettlementInstruction(SettlementMethod.WIRE, "", "", "")
        val flow = BilateralSettlementCreateFlow.Initiator(instruction, b.info.singleIdentity())
        val future = a.startFlow(flow)
        network.runNetwork()

        val signedTx = future.getOrThrow()
        signedTx.verifySignaturesExcept(a.info.singleIdentity().owningKey)
    }

    @Test
    fun `flow records a transaction in both parties' transaction storages`() {
        val instruction = SettlementInstruction(SettlementMethod.WIRE, "", "", "")
        val flow = BilateralSettlementCreateFlow.Initiator(instruction, b.info.singleIdentity())
        val future = a.startFlow(flow)
        network.runNetwork()
        val signedTx = future.getOrThrow()

        // We check the recorded transaction in both transaction storages.
        for (node in listOf(a, b)) {
            assertEquals(signedTx, node.services.validatedTransactions.getTransaction(signedTx.id))
        }
    }

    @Test
    fun `recorded transaction has no inputs and a single output, the input settlement instruction`() {
        val instruction = SettlementInstruction(SettlementMethod.WIRE, "", "", "")
        val flow = BilateralSettlementCreateFlow.Initiator(instruction, b.info.singleIdentity())
        val future = a.startFlow(flow)
        network.runNetwork()
        val signedTx = future.getOrThrow()

        // We check the recorded transaction in both vaults.
        for (node in listOf(a, b)) {
            val recordedTx = node.services.validatedTransactions.getTransaction(signedTx.id)
            val txOutputs = recordedTx!!.tx.outputs
            assert(txOutputs.size == 1)

            val recordedState = txOutputs[0].data as BilateralSettlementState
            assertEquals(recordedState.instruction, instruction)
            assertEquals(recordedState.bank, a.info.singleIdentity())
            assertEquals(recordedState.counterparty, b.info.singleIdentity())
        }
    }

    @Test
    fun `flow records the correct settlement state in both parties' vaults`() {
        val instruction = SettlementInstruction(SettlementMethod.WIRE, "", "", "")
        val flow = BilateralSettlementCreateFlow.Initiator(instruction, b.info.singleIdentity())
        val future = a.startFlow(flow)
        network.runNetwork()
        future.getOrThrow()

        // We check the recorded settlement instruction is in both vaults.
        for (node in listOf(a, b)) {
            node.transaction {
                val bilateralStates = node.services.vaultService.queryBy<BilateralSettlementState>().states
                assertEquals(1, bilateralStates.size)
                val recordedState = bilateralStates.single().state.data
                assertEquals(recordedState.instruction, instruction)
                assertEquals(recordedState.bank, a.info.singleIdentity())
                assertEquals(recordedState.counterparty, b.info.singleIdentity())
            }
        }
    }
}