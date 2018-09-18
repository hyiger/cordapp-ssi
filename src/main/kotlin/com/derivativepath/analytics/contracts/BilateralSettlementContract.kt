package com.derivativepath.analytics.contracts

import com.derivativepath.analytics.states.BilateralSettlementState
import com.derivativepath.analytics.states.SettlementMethod
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

/**
 * A implementation of a settlement instruction smart contract in Corda.
 *
 * This contract enforces rules regarding the creation of a valid [BilateralSettlementState], which in turn encapsulates an [SettlementInstruction].
 *
 * For a new [SettlementInstruction] to be issued onto the ledger, a transaction is required which takes:
 * - Zero input states.
 * - One output state: the new [SettlementInstruction].
 * - An Create() command with the public keys of both the bank and the counterparty.
 *
 * All contracts must sub-class the [Contract] interface.
 */
class BilateralSettlementContract : Contract {

	companion object {
		@JvmStatic
		val SETTLEMENT_CONTRACT_ID = BilateralSettlementContract::class.java.canonicalName!!
	}

	/**
	 * The verify() function of all the states' contracts must not throw an exception for a transaction to be
	 * considered valid.
	 */
	override fun verify(tx: LedgerTransaction) {

		val command = tx.commands.requireSingleCommand<Commands>()

		when (command.value) {
			is Commands.Create -> requireThat {
				// Constraints on the shape of the transaction.
				"No inputs should be consumed when issuing a Settlement." using (tx.inputs.isEmpty())
				"There should be one output state of type BilateralSettlementState." using (tx.outputs.size == 1)

				// Settlement-specific constraints.
				val out = tx.outputsOfType<BilateralSettlementState>().single()

				if (out.instruction.settlementMethod == SettlementMethod.ACH) {
					"Settlement method of type WIRE must include account and routing number" using (out.instruction.account != null && out.instruction.routingNumber != null)
					"The Settlement account number must be greater than 0" using (out.instruction.account != null && out.instruction.account > 0)
					"The Settlement routing number must be greater than 0" using (out.instruction.routingNumber != null && out.instruction.routingNumber > 0)
				}

				"The bank and the counterparty cannot be the same entity." using (out.bank != out.counterparty)

				// Constraints on the signers.
				"There must be two signers." using (command.signers.toSet().size == 2)
				"The bank and counterparty must be signers." using (command.signers.containsAll(listOf(
						out.bank.owningKey, out.counterparty.owningKey)))
			}
		}
	}

	interface Commands : CommandData {
		class Create : Commands
		class Update : Commands
		class Delete : Commands
	}

}

