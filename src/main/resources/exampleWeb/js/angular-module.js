"use strict";

const app = angular.module('demoAppModule', ['ui.bootstrap']);

// Fix for unhandled rejections bug.
app.config(['$qProvider', function ($qProvider) {
    $qProvider.errorOnUnhandledRejections(false);
}]);

app.controller('DemoAppController', function ($http, $location, $uibModal) {
    const demoApp = this;

    // We identify the node.
    const apiBaseURL = "/api/settlement/";
    let peers = [];
    let methods = [];

    $http.get(apiBaseURL + "me").then((response) => demoApp.thisNode = response.data.me);
    $http.get(apiBaseURL + "peers").then((response) => peers = response.data.peers);
    $http.get(apiBaseURL + "methods").then((response) => methods = response.data.methods);

    demoApp.openModal = () => {
        const modalInstance = $uibModal.open({
            templateUrl: 'unilateral.html',
            controller: 'ModalInstanceCtrl',
            controllerAs: 'modalInstance',
            resolve: {
                demoApp: () => demoApp,
                apiBaseURL: () => apiBaseURL,
                peers: () => peers,
                methods: () => methods
            }
        });

        modalInstance.result.then(() => {
        }, () => {
        });
    };

    demoApp.getSettlements = () => $http.get(apiBaseURL + "instructions")
        .then((response) => demoApp.settlements = Object.keys(response.data)
            .map((key) => response.data[key].state.data)
            .reverse());

    demoApp.getMySettlements = () => $http.get(apiBaseURL + "instructions/mine")
        .then((response) => demoApp.settlements = Object.keys(response.data)
            .map((key) => response.data[key].state.data)
            .reverse());

    demoApp.getMySettlementsBilateral = () => $http.get(apiBaseURL + "instructions/bilateral/mine")
        .then((response) => demoApp.mySettlementsUnilateral = Object.keys(response.data)
            .map((key) => response.data[key].state.data)
            .reverse());

    //demoApp.getSettlements();
    demoApp.getMySettlements();

    //demoApp.getMySettlementsBilateral();
});

app.controller('ModalInstanceCtrl', function ($http, $location, $uibModalInstance, $uibModal, demoApp, apiBaseURL, peers, methods) {
    const modalInstance = this;

    modalInstance.methods = methods;
    modalInstance.peers = peers;
    modalInstance.form = {};
    modalInstance.formError = false;

    // Validate and create Settlement instruction.
    modalInstance.create = () => {
        if (invalidFormInput()) {
            modalInstance.formError = true;
        } else {
            modalInstance.formError = false;

            $uibModalInstance.close();

            const createPath = 'create';

            const createSettlementEndpoint = `${apiBaseURL}${createPath}`;

            const data = `{
                  "settlementMethod" : "${modalInstance.form.method}",
                  "beneficiaryName" : "${modalInstance.form.beneficiary}",
                  "code" : "${modalInstance.form.code}",
                  "institution" : "${modalInstance.form.institution}",
                  "additionalCode" : "${modalInstance.form.additionalCode}",
                  "account" : ${modalInstance.form.account},
                  "routingNumber" : ${modalInstance.form.routing},
                  "attention" : "${modalInstance.form.attention}",
                  "reference" : "${modalInstance.form.reference}"
             }`;

            // Create PO and handle success / fail responses.
            $http.put(createSettlementEndpoint, data).then(
                (result) => {
                    modalInstance.displayMessage(result);
                    demoApp.getSettlements();
                    demoApp.getMySettlements();
                },
                (result) => {
                    modalInstance.displayMessage(result);
                }
            );
        }
    };

    modalInstance.displayMessage = (message) => {
        const modalInstanceTwo = $uibModal.open({
            templateUrl: 'messageContent.html',
            controller: 'messageCtrl',
            controllerAs: 'modalInstanceTwo',
            resolve: {message: () => message}
        });

        // No behaviour on close / dismiss.
        modalInstanceTwo.result.then(() => {
        }, () => {
        });
    };

    // Close create Settlement modal dialogue.
    modalInstance.cancel = () => $uibModalInstance.dismiss();

    // Validate the Settlement form.
    function invalidFormInput() {
        return false;
    }
});

// Controller for success/fail modal dialogue.
app.controller('messageCtrl', function ($uibModalInstance, message) {
    const modalInstanceTwo = this;
    modalInstanceTwo.message = message.data;
});
