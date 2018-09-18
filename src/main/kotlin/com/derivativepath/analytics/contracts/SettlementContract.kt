package com.derivativepath.analytics.contracts

import com.derivativepath.analytics.states.SettlementMethod
import com.derivativepath.analytics.states.SettlementState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

class SettlementContract : Contract {

    companion object {
        @JvmStatic
        val SETTLEMENT_CONTRACT_ID = SettlementContract::class.java.canonicalName!!
    }

    /**
     * The verify() function of all the states' contracts must not throw an exception for a transaction to be
     * considered valid.
     */
    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<SettlementContract.Commands>()

        when (command.value) {

            is Commands.Create ->
                requireThat {
                    // Constraints on the shape of the transaction.
                    "No inputs should be consumed when issuing a Settlement." using (tx.inputs.isEmpty())
                    "There should be one output state of type SettlementState." using (tx.outputs.size == 1)

                    // Settlement-specific constraints.
                    val out = tx.outputsOfType<SettlementState>().single()
                    if (out.instruction.settlementMethod == SettlementMethod.ACH) {
                        "Settlement method of type WIRE must include account and routing number" using (out.instruction.account != null && out.instruction.routingNumber != null)
                        "The Settlement account number must be greater than 0" using (out.instruction.account != null && out.instruction.account > 0)
                        "The Settlement routing number must be greater than 0" using (out.instruction.routingNumber != null && out.instruction.routingNumber > 0)
                    }

                    // Constraints on the signers.
                    "The party must be a signer." using (command.signers.toSet().size == 1 && command.signers.contains(out.bank.owningKey))
                }

            is Commands.Delete -> requireThat {
                "There should be one input of type SettlementState" using (tx.inputs.size == 1)
                "There should be no output states." using (tx.outputs.isEmpty())

                val out = tx.outputsOfType<SettlementState>().single()
                "The party must be a signer." using (command.signers.toSet().size == 1 && command.signers.contains(out.bank.owningKey))
            }

            is Commands.Update -> requireThat {
                "There should be one input of type SettlementState" using (tx.inputs.size == 1)
                "There should be one output state of type SettlementState." using (tx.outputs.size == 1)

                val out = tx.outputsOfType<SettlementState>().single()
                "The party must be a signer." using (command.signers.toSet().size == 1 && command.signers.contains(out.bank.owningKey))
            }
        }
    }

    interface Commands : CommandData {
        class Create : Commands
        class Update : Commands
        class Delete : Commands
    }

}