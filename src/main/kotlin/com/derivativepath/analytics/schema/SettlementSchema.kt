package com.derivativepath.analytics.schema

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

/**
 * The family of schemas for SettlementState.
 */
object SettlementSchema

/**
 * An SettlementState schema.
 */
object SettlementSchemaV1 : MappedSchema(
        schemaFamily = SettlementSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentSettlement::class.java)) {

    @Entity
    @Table(name = "settlement_states")
    class PersistentSettlement(
            @Column(name = "linear_id")
            var linearId: UUID,
            var bankName: String,
            @Column(name = "beneficiary")
            var beneficiaryName: String,
            @Column(name = "method")
            var settlementMethod: String,
            var code: String,
            var institution: String,
            var additionalCode: String? = null,
            var account: Long? = null,
            var routingNumber: Long? = null,
            var attention: String? = null,
            var reference: String? = null,
            @Column(name = "counterparty")
            var counterpartyName: String? = null
    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor() : this(UUID.randomUUID(), "", "", "", "", "")
    }
}