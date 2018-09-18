package com.derivativepath.analytics.api

import com.derivativepath.analytics.flow.BilateralSettlementCreateFlow
import com.derivativepath.analytics.flow.SettlementCreateFlow
import com.derivativepath.analytics.schema.SettlementSchemaV1
import com.derivativepath.analytics.states.BilateralSettlementState
import com.derivativepath.analytics.states.SettlementInstruction
import com.derivativepath.analytics.states.SettlementMethod
import com.derivativepath.analytics.states.SettlementState
import net.corda.core.contracts.ContractState
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.FlowProgressHandle
import net.corda.core.messaging.startTrackedFlow
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.node.services.IdentityService
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.builder
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.getOrThrow
import net.corda.core.utilities.loggerFor
import org.slf4j.Logger
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.Status.BAD_REQUEST
import javax.ws.rs.core.Response.Status.CREATED


val SERVICE_NAMES = listOf("Notary", "Network Map Service")

// This API is accessible from /api/settlement. All paths specified below are relative to it.
@Path("settlement")
class SettlementApi(val services: CordaRPCOps) {
    private val myLegalName: CordaX500Name = services.nodeInfo().legalIdentities.first().name

    companion object {
        val logger: Logger = loggerFor<SettlementApi>()
    }

    /**
     * Returns the node's name.
     */
    @GET
    @Path("me")
    @Produces(MediaType.APPLICATION_JSON)
    fun whoami() = mapOf("me" to myLegalName)

    /**
     * Returns all parties registered with the [NetworkMapService]. These names can be used to look up identities
     * using the [IdentityService].
     */
    @GET
    @Path("peers")
    @Produces(MediaType.APPLICATION_JSON)
    fun getPeers(): Map<String, List<CordaX500Name>> =
            mapOf("peers" to services.networkMapSnapshot()
                    .map { it.legalIdentities.first().name }
                    //filter out myself, notary and eventual network map started by driver
                    .filter { it.organisation !in (SERVICE_NAMES + myLegalName.organisation) })

    @GET
    @Path("methods")
    @Produces(MediaType.APPLICATION_JSON)
    fun getMethods(): Map<String, List<String>> = mapOf("methods" to SettlementMethod.values().map { it.name })

    /**
     * Displays all Settlement states that exist in the node's vault.
     */
    @GET
    @Path("instructions")
    @Produces(MediaType.APPLICATION_JSON)
    fun getInstructions() = services.vaultQueryBy<BilateralSettlementState>().states

    inline fun <T> returnInstruction(handle: FlowProgressHandle<T>): Response {
        return try {
            val signedTx = handle.returnValue.getOrThrow() as SignedTransaction
            Response.status(CREATED).entity("Transaction id ${signedTx.id} committed to ledger.\n").build()
        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            Response.status(BAD_REQUEST).entity(ex.message!!).build()
        }
    }

    /**
     * Initiates a flow to agree a settlement instruction between two parties.
     *
     * Once the flow finishes it will have written the settlement instruction to ledger. Both the bank and the counterparty will be able to
     * see it when calling /api/settlement/instructions on their respective nodes.
     *
     * This end-point takes a Party name parameter as part of the path. If the serving node can't find the other party
     * in its network map cache, it will return an HTTP bad request.
     *
     * The flow is invoked asynchronously. It returns a future when the flow's call() method returns.
     */
    @PUT
    @Path("create/bilateral")
    fun returnInstruction(instruction: SettlementInstruction, @QueryParam("partyName") partyName: CordaX500Name?): Response {

        if (partyName == null)
            return Response.status(BAD_REQUEST).entity("Query parameter 'partyName' missing or has wrong format.\n").build()

        val otherParty = services.wellKnownPartyFromX500Name(partyName)
                ?: return Response.status(BAD_REQUEST).entity("Party named $partyName cannot be found.\n").build()

        return returnInstruction(services.startTrackedFlow(BilateralSettlementCreateFlow::Initiator, instruction, otherParty))
    }

    @PUT
    @Path("create")
    fun returnInstruction(instruction: SettlementInstruction): Response = returnInstruction(services.startTrackedFlow(SettlementCreateFlow::Initiator, instruction))

    inline fun <reified T : ContractState> getInstructions(): Response {
        val generalCriteria = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.ALL)

        val results = builder {
            var partyType = SettlementSchemaV1.PersistentSettlement::bankName.equal(services.nodeInfo().legalIdentities.first().name.toString())
            val customCriteria = QueryCriteria.VaultCustomQueryCriteria(partyType)
            val criteria = generalCriteria.and(customCriteria)
            val results = services.vaultQueryBy<T>(criteria).states
            return Response.ok(results).build()
        }
    }

    /**
     * Displays all Settlement instruction states that are created by Party.
     */
    @GET
    @Path("instructions/bilateral//mine")
    @Produces(MediaType.APPLICATION_JSON)
    fun myInstructions(): Response = getInstructions<BilateralSettlementState>()

    /**
     * Displays all Settlement instruction states that are created by Party.
     */
    @GET
    @Path("instructions/mine")
    @Produces(MediaType.APPLICATION_JSON)
    fun myInstructionsUnilateral(): Response = getInstructions<SettlementState>()

}