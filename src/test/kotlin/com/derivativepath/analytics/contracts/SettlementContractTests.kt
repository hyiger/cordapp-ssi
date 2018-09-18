package com.derivativepath.analytics.contracts

import com.derivativepath.analytics.contracts.SettlementContract.Companion.SETTLEMENT_CONTRACT_ID
import com.derivativepath.analytics.states.SettlementInstruction
import com.derivativepath.analytics.states.SettlementMethod
import com.derivativepath.analytics.states.SettlementState
import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test

class SettlementContractTests {
    private val ledgerServices = MockServices()
    private val corp = TestIdentity(CordaX500Name("Bank", "New York", "US"))
    private val otherCorp = TestIdentity(CordaX500Name("Other", "London", "GB"))

    @Test
    fun `transaction must include Create command`() {
        val instruction = SettlementInstruction(SettlementMethod.WIRE, "", "", "")
        //val instruction = SettlementInstruction(1,1,"USD", 1.0);
        ledgerServices.ledger {
            transaction {
                output(SETTLEMENT_CONTRACT_ID, SettlementState(instruction, corp.party))
                fails()
                command(corp.publicKey, SettlementContract.Commands.Create())
                verifies()
            }
        }
    }

    @Test
    fun `transaction must have no inputs`() {
        val instruction = SettlementInstruction(SettlementMethod.WIRE, "", "", "")
        ledgerServices.ledger {
            transaction {
                input(SETTLEMENT_CONTRACT_ID, SettlementState(instruction, corp.party))
                output(SETTLEMENT_CONTRACT_ID, SettlementState(instruction, corp.party))
                command(corp.publicKey, SettlementContract.Commands.Create())
                `fails with`("No inputs should be consumed when issuing a Settlement.")
            }
        }
    }

    @Test
    fun `transaction must have one output`() {
        val instruction = SettlementInstruction(SettlementMethod.WIRE, "", "", "")
        ledgerServices.ledger {
            transaction {
                output(SETTLEMENT_CONTRACT_ID, SettlementState(instruction, corp.party))
                output(SETTLEMENT_CONTRACT_ID, SettlementState(instruction, corp.party))
                command(corp.publicKey, SettlementContract.Commands.Create())
                `fails with`("There should be one output state of type SettlementState.")
            }
        }
    }

    @Test
    fun `bank must sign transaction`() {
        val instruction = SettlementInstruction(SettlementMethod.WIRE, "", "", "")
        ledgerServices.ledger {
            transaction {
                output(SETTLEMENT_CONTRACT_ID, SettlementState(instruction, corp.party))
                command(otherCorp.publicKey, SettlementContract.Commands.Create())
                `fails with`("The party must be a signer.")
            }
        }
    }

    @Test
    fun `account number must be greater than 0`() {
        val instruction = SettlementInstruction(SettlementMethod.ACH, "", "", "", account = -1, routingNumber = 1)
        ledgerServices.ledger {
            transaction {
                output(SETTLEMENT_CONTRACT_ID, SettlementState(instruction, corp.party))
                command(corp.publicKey, SettlementContract.Commands.Create())
                `fails with`("The Settlement account number must be greater than 0")
            }
        }
    }

    @Test
    fun `routing number must be greater than 0`() {
        val instruction = SettlementInstruction(SettlementMethod.ACH, "", "", "", account = 1, routingNumber = -1)
        ledgerServices.ledger {
            transaction {
                output(SETTLEMENT_CONTRACT_ID, SettlementState(instruction, corp.party))
                command(corp.publicKey, SettlementContract.Commands.Create())
                `fails with`("The Settlement routing number must be greater than 0")
            }
        }
    }

}