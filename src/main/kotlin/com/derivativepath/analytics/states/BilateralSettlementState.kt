package com.derivativepath.analytics.states

import com.derivativepath.analytics.schema.SettlementSchemaV1
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState


/**
 * The state object recording settlements agreements between two parties.
 *
 * A state must implement [ContractState] or one of its descendants.
 *
 * @param instruction the settlement instructions.
 * @param bank the party issuing the settlement instructions.
 * @param counterparty the party receiving and approving the settlement instructions.
 */
class BilateralSettlementState(instruction: SettlementInstruction,
                               bank: Party,
                               val counterparty: Party) :
        SettlementState(instruction, bank) {

    /** The public keys of the involved parties. */
    override val participants: List<AbstractParty> get() = listOf(bank, counterparty)

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is SettlementSchemaV1 -> SettlementSchemaV1.PersistentSettlement(
                    linearId.id,
                    bank.name.toString(),
                    instruction.beneficiaryName,
                    instruction.settlementMethod.name,
                    instruction.code,
                    instruction.institution,
                    instruction.additionalCode,
                    instruction.account,
                    instruction.routingNumber,
                    instruction.attention,
                    instruction.reference,
                    counterparty.name.toString()
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(SettlementSchemaV1)
}

