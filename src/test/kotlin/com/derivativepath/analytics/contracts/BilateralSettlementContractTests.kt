package com.derivativepath.analytics.contracts

import com.derivativepath.analytics.contracts.BilateralSettlementContract.Companion.SETTLEMENT_CONTRACT_ID
import com.derivativepath.analytics.states.BilateralSettlementState
import com.derivativepath.analytics.states.SettlementInstruction
import com.derivativepath.analytics.states.SettlementMethod
import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test

class BilateralSettlementContractTests {
    private val ledgerServices = MockServices()
    private val megaCorp = TestIdentity(CordaX500Name("MegaCorp", "London", "GB"))
    private val miniCorp = TestIdentity(CordaX500Name("MiniCorp", "New York", "US"))

    @Test
    fun `transaction must include Create command`() {
        val instruction = SettlementInstruction(SettlementMethod.SWIFT, "", "", "")
        ledgerServices.ledger {
            transaction {
                output(SETTLEMENT_CONTRACT_ID, BilateralSettlementState(instruction, miniCorp.party, megaCorp.party))
                fails()
                command(listOf(megaCorp.publicKey, miniCorp.publicKey), BilateralSettlementContract.Commands.Create())
                verifies()
            }
        }
    }

    @Test
    fun `transaction must have no inputs`() {
        val instruction = SettlementInstruction(SettlementMethod.SWIFT, "", "", "")
        ledgerServices.ledger {
            transaction {
                input(SETTLEMENT_CONTRACT_ID, BilateralSettlementState(instruction, miniCorp.party, megaCorp.party))
                output(SETTLEMENT_CONTRACT_ID, BilateralSettlementState(instruction, miniCorp.party, megaCorp.party))
                command(listOf(megaCorp.publicKey, miniCorp.publicKey), BilateralSettlementContract.Commands.Create())
                `fails with`("No inputs should be consumed when issuing a Settlement.")
            }
        }
    }

    @Test
    fun `transaction must have one output`() {
        val instruction = SettlementInstruction(SettlementMethod.SWIFT, "", "", "")
        ledgerServices.ledger {
            transaction {
                output(SETTLEMENT_CONTRACT_ID, BilateralSettlementState(instruction, miniCorp.party, megaCorp.party))
                output(SETTLEMENT_CONTRACT_ID, BilateralSettlementState(instruction, miniCorp.party, megaCorp.party))
                command(listOf(megaCorp.publicKey, miniCorp.publicKey), BilateralSettlementContract.Commands.Create())
                `fails with`("There should be one output state of type BilateralSettlementState.")
            }
        }
    }

    @Test
    fun `lender must sign transaction`() {
        val instruction = SettlementInstruction(SettlementMethod.SWIFT, "", "", "")
        ledgerServices.ledger {
            transaction {
                output(SETTLEMENT_CONTRACT_ID, BilateralSettlementState(instruction, miniCorp.party, megaCorp.party))
                command(miniCorp.publicKey, BilateralSettlementContract.Commands.Create())
                `fails with`("There must be two signers.")
            }
        }
    }

    @Test
    fun `borrower must sign transaction`() {
        val instruction = SettlementInstruction(SettlementMethod.SWIFT, "", "", "")
        ledgerServices.ledger {
            transaction {
                output(SETTLEMENT_CONTRACT_ID, BilateralSettlementState(instruction, miniCorp.party, megaCorp.party))
                command(megaCorp.publicKey, BilateralSettlementContract.Commands.Create())
                `fails with`("There must be two signers.")
            }
        }
    }

    @Test
    fun `lender is not borrower`() {
        val instruction = SettlementInstruction(SettlementMethod.SWIFT, "", "", "")
        ledgerServices.ledger {
            transaction {
                output(SETTLEMENT_CONTRACT_ID, BilateralSettlementState(instruction, megaCorp.party, megaCorp.party))
                command(listOf(megaCorp.publicKey, miniCorp.publicKey), BilateralSettlementContract.Commands.Create())
                `fails with`("The bank and the counterparty cannot be the same entity.")
            }
        }
    }

    @Test
    fun `wire must include account and routing numbers`() {
        val instruction = SettlementInstruction(SettlementMethod.ACH, "", "", "")
        ledgerServices.ledger {
            transaction {
                output(SETTLEMENT_CONTRACT_ID, BilateralSettlementState(instruction, miniCorp.party, megaCorp.party))
                command(listOf(megaCorp.publicKey, miniCorp.publicKey), BilateralSettlementContract.Commands.Create())
                `fails with`("Settlement method of type WIRE must include account and routing number")
            }
        }
    }

    @Test
    fun `account number must be greater than 0`() {
        val instruction = SettlementInstruction(SettlementMethod.ACH, "", "", "", account = -1, routingNumber = 1)
        ledgerServices.ledger {
            transaction {
                output(SETTLEMENT_CONTRACT_ID, BilateralSettlementState(instruction, miniCorp.party, megaCorp.party))
                command(listOf(megaCorp.publicKey, miniCorp.publicKey), BilateralSettlementContract.Commands.Create())
                `fails with`("The Settlement account number must be greater than 0")
            }
        }
    }

    @Test
    fun `routing number must be greater than 0`() {
        val instruction = SettlementInstruction(SettlementMethod.ACH, "", "", "", account = 1, routingNumber = -1)
        ledgerServices.ledger {
            transaction {
                output(SETTLEMENT_CONTRACT_ID, BilateralSettlementState(instruction, miniCorp.party, megaCorp.party))
                command(listOf(megaCorp.publicKey, miniCorp.publicKey), BilateralSettlementContract.Commands.Create())
                `fails with`("The Settlement routing number must be greater than 0")
            }
        }
    }

}