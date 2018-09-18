package com.derivativepath.analytics.states

import net.corda.core.serialization.CordaSerializable

@CordaSerializable
enum class SettlementMethod {
    ACH, SWIFT, WIRE
}

@CordaSerializable
data class SettlementInstruction(
        val settlementMethod: SettlementMethod,
        val beneficiaryName: String,
        val code: String,
        val institution: String,
        val additionalCode: String? = null,
        val account: Long? = null,
        val routingNumber: Long? = null,
        val attention: String? = null,
        val reference: String? = null)
