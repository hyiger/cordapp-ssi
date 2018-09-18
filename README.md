![Corda](https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png)

# Settlement Cordapp Prototype

This is a prototype of a basic settlement workflow on a distributed ledger

## Deploying and Starting Nodes

### Deployment
```kotlin
./gradlew deployNodes
```

### Starting Web Services and Consoles
```kotlin
build/nodes/runnodes
```

## Running commands in the CrASH console


### A Settlement Instruction
A settlement instruction has the following structure:

```
"instruction" : {
     "account" : 12345,
     "routingNumber" : 54321,
     "currency" : "USD",
     "amount" : 100000.0
   },
```

### Validity Checks
The instruction will be checked for validity. For example: the following is an invalid instruction

```kotlin
flow start SettlementFlow$Initiator instruction: {account: 12345, routingNumber: 54321, currency: "USD", amount: -100000.0}, otherParty: "O=PartyB,L=New York,C=US"
☠   Contract verification failed: Failed requirement: The Settlement amount must be greater than 0, contract: com.derivativepath.analytics.contract.BilateralSettlementContract
flow start SettlementFlow$Initiator instruction: {account: 12345, routingNumber: 0, currency: "USD", amount: 100000.0}, otherParty: "O=PartyB,L=New York,C=US"
☠   Contract verification failed: Failed requirement: The Settlement routing number must be greater than 0, contract: com.derivativepath.analytics.contract.BilateralSettlementContract
``` 

### Sending a valid instruction
Send a settlement instruction from PartyA to PartyB

```
flow start SettlementFlow$Initiator instruction: {account: 12345, routingNumber: 54321, currency: "USD", amount: 100000.0}, otherParty: "O=PartyB,L=New York,C=US"


✅   Generating transaction based on a new settlement instruction.
✅   Verifying contract constraints.
✅   Signing transaction with our private key.
✅   Gathering the counterparty's signature.
    ✅   Collecting signatures from counterparties.
    ✅   Verifying collected signatures.
✅   Obtaining notary signature and recording transaction.
    ✅   Requesting signature by notary service
        ✅   Requesting signature by Notary service
        ✅   Validating response from Notary service
    ✅   Broadcasting transaction to participants
✅   Done


```

Query the vault for all Settlement Instructions

```kotlin
run vaultQuery contractStateType: com.derivativepath.analytics.state.BilateralSettlementStatementState
```

```

{
  "states" : [ {
    "stateBilateral" : {
      "data" : {
        "instruction" : {
          "account" : 12345,
          "routingNumber" : 54321,
          "currency" : "USD",
          "amount" : 100000.0
        },
        "bank" : "O=PartyA, L=London, C=GB",
        "counterparty" : "O=PartyB, L=New York, C=US",
        "linearId" : {
          "externalId" : null,
          "id" : "8bb931dc-3b84-447c-8fe0-49f74d239ef4"
        },
        "participants" : [ "O=PartyA, L=London, C=GB", "O=PartyB, L=New York, C=US" ]
      },
      "contract" : "com.derivativepath.analytics.contract.BilateralSettlementContract",
      "notary" : "O=Notary, L=London, C=GB",
      "encumbrance" : null,
      "constraint" : {
        "attachmentId" : "7306D7C34C101F7C1B38F71EC8129123806AAD4195B6EFBA19846E9108814998"
      }
    },
    "ref" : {
      "txhash" : "7AC8AFE7FF8766DE5A6CA2883CFD16A3992732CA03E276BAD50D3965AECCCE1A",
      "index" : 0
    }
  } ],
  "statesMetadata" : [ {
    "ref" : {
      "txhash" : "7AC8AFE7FF8766DE5A6CA2883CFD16A3992732CA03E276BAD50D3965AECCCE1A",
      "index" : 0
    },
    "contractStateClassName" : "com.derivativepath.analytics.state.BilateralSettlementStatementState",
    "recordedTime" : "2018-08-16T16:39:29.432Z",
    "consumedTime" : null,
    "status" : "UNCONSUMED",
    "notary" : "O=Notary, L=London, C=GB",
    "lockId" : null,
    "lockUpdateTime" : null
  } ],
  "totalStatesAvailable" : -1,
  "stateTypes" : "UNCONSUMED",
  "otherResults" : [ ]
}

```