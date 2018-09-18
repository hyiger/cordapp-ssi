package com.derivativepath.analytics.states

import com.derivativepath.analytics.schema.SettlementSchemaV1
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState


open class SettlementState(val instruction: SettlementInstruction,
                           val bank: Party,
                           override val linearId: UniqueIdentifier = UniqueIdentifier()) :
        LinearState, QueryableState {

    /** The public keys of the involved parties. */
    override val participants: List<AbstractParty> get() = listOf(bank)

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
                    instruction.reference
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(SettlementSchemaV1)
}