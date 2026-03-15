'use strict';

angular.module('axonBank')
    .controller('BankAccountsCtrl', function ($scope, $uibModal, BankAccountService, TenantService, AiService) {
        $scope.currentTenant = TenantService.getTenantId();
        $scope.onTenantChange = function () {
            TenantService.setTenantId($scope.currentTenant);
            window.location.reload();
        };

        function updateBankAccounts(bankAccounts) {
            $scope.bankAccounts = bankAccounts;
        }

        $scope.executeAiCommand = function () {
            if (!$scope.aiCommand) return;
            var text = $scope.aiCommand;
            $scope.aiCommand = ''; // clear input
            AiService.executeCommand(text).then(function (response) {
                if (response.data && response.data.parsedCommand) {
                    var parsed = response.data.parsedCommand;
                    console.log("Executed NLP:", parsed);
                } else {
                    console.error("NLP failed", response);
                }
            }, function (err) {
                console.error("NLP API error:", err);
            });
        };

        $scope.showInsights = function (id) {
            $uibModal.open({
                controller: 'AiInsightsModalCtrl',
                templateUrl: '/app/modals/aiInsightsModal.html',
                windowClass: 'ai-modal-window',
                resolve: {
                    bankAccountId: function () {
                        return id;
                    }
                }
            });
        };

        $scope.create = function () {
            $uibModal.open({
                controller: 'CreateBankAccountModalCtrl',
                templateUrl: '/app/modals/createBankAccountModal.html',
                ariaLabelledBy: 'modal-title',
                ariaDescribedBy: 'modal-body'
            });
        };

        $scope.deposit = function (id) {
            $uibModal.open({
                controller: 'DepositMoneyModalCtrl',
                templateUrl: '/app/modals/depositMoneyModal.html',
                ariaLabelledBy: 'modal-title',
                ariaDescribedBy: 'modal-body',
                resolve: {
                    bankAccountId: function () {
                        return id;
                    }
                }
            });
        };

        $scope.withdraw = function (id) {
            $uibModal.open({
                controller: 'WithdrawMoneyModalCtrl',
                templateUrl: '/app/modals/withdrawMoneyModal.html',
                ariaLabelledBy: 'modal-title',
                ariaDescribedBy: 'modal-body',
                resolve: {
                    bankAccountId: function () {
                        return id;
                    }
                }
            });
        };

        $scope.transfer = function (id) {
            $uibModal.open({
                controller: 'TransferMoneyModalCtrl',
                templateUrl: '/app/modals/transferMoneyModal.html',
                ariaLabelledBy: 'modal-title',
                ariaDescribedBy: 'modal-body',
                resolve: {
                    bankAccountId: function () {
                        return id;
                    },
                    bankAccounts: function () {
                        return $scope.bankAccounts;
                    }
                }
            });
        };

        $scope.bankTransfers = function (id) {
            $uibModal.open({
                controller: 'BankTransfersModalCtrl',
                templateUrl: '/app/modals/bankTransfersModal.html',
                resolve: {
                    bankAccountId: function () {
                        return id;
                    },
                    bankTransfers: function () {
                        // Switch from STOMP to the new Categorization REST API
                        return AiService.getCategorizedTransactions(id).then(function (res) {
                            return res.data;
                        });
                    }
                }
            });
        };
        BankAccountService.connect()
            .then(function () {
                BankAccountService.loadBankAccounts()
                    .then(updateBankAccounts);

                BankAccountService.subscribeToBankAccountUpdates()
                    .then(function () {
                        // do nothing
                    }, function () {
                        // do nothing
                    }, updateBankAccounts)
            });
    })
    .controller('CreateBankAccountModalCtrl', function ($uibModalInstance, $scope, BankAccountService) {
        $scope.bankAccount = {};

        $scope.cancel = function () {
            $uibModalInstance.dismiss();
        };
        $scope.submit = function () {
            BankAccountService.createBankAccount($scope.bankAccount);
            $uibModalInstance.close();
        };
    })
    .controller('DepositMoneyModalCtrl', function ($uibModalInstance, $scope, BankAccountService, bankAccountId) {
        $scope.deposit = {
            bankAccountId: bankAccountId
        };

        $scope.cancel = function () {
            $uibModalInstance.dismiss();
        };
        $scope.submit = function () {
            BankAccountService.deposit($scope.deposit);
            $uibModalInstance.close();
        };
    })
    .controller('WithdrawMoneyModalCtrl', function ($uibModalInstance, $scope, BankAccountService, bankAccountId) {
        $scope.withdrawal = {
            bankAccountId: bankAccountId
        };

        $scope.cancel = function () {
            $uibModalInstance.dismiss();
        };
        $scope.submit = function () {
            BankAccountService.withdraw($scope.withdrawal);
            $uibModalInstance.close();
        };
    })
    .controller('TransferMoneyModalCtrl',
        function ($uibModalInstance, $scope, BankAccountService, bankAccountId, bankAccounts) {
            $scope.bankAccounts = bankAccounts;
            $scope.bankTransfer = {
                sourceBankAccountId: bankAccountId
            };

            $scope.cancel = function () {
                $uibModalInstance.dismiss();
            };
            $scope.submit = function () {
                BankAccountService.transfer($scope.bankTransfer);
                $uibModalInstance.close();
            };
        })
    .controller('BankTransfersModalCtrl',
        function ($uibModalInstance, $scope, BankAccountService, bankAccountId, bankTransfers) {
            $scope.bankAccountId = bankAccountId;
            $scope.bankTransfers = bankTransfers;

            $scope.close = function () {
                $uibModalInstance.close();
            };
        })
    .controller('AiInsightsModalCtrl', function ($uibModalInstance, $scope, AiService, bankAccountId) {
        $scope.bankAccountId = bankAccountId;
        $scope.loading = true;

        // Fetch insights and forecasts simultaneously
        Promise.all([
            AiService.getInsights(bankAccountId),
            AiService.getForecast(bankAccountId, 30)
        ]).then(function (results) {
            $scope.$apply(function () {
                $scope.insights = results[0].data;
                $scope.forecast = results[1].data;
                $scope.loading = false;
            });
        }).catch(function (err) {
            console.error("Failed to load insights", err);
            $scope.loading = false;
        });

        $scope.close = function () {
            $uibModalInstance.dismiss();
        };
    })
    .controller('AiHudCtrl', function ($scope, $interval, AiService) {
        $scope.alerts = [];
        var alertCount = 0;

        function addAlert(type, title, message) {
            var id = alertCount++;
            var alertObj = { id: id, type: type, title: title, message: message };
            $scope.alerts.push(alertObj);

            // Auto dismiss after 7 seconds
            setTimeout(function () {
                $scope.$apply(function () {
                    $scope.alerts = $scope.alerts.filter(function (a) { return a.id !== id; });
                });
            }, 7000);
        }

        // Poll API for AI alerts
        $interval(function () {
            AiService.getFraudAlerts().then(function (res) {
                var newAlerts = res.data || [];
                newAlerts.forEach(function (a) {
                    addAlert('fraud', '🚨 Fraud Alert', "Suspicious transaction: $" + a.amount + " (" + a.reason + ")");
                });
            });

            AiService.getAnomalyAlerts().then(function (res) {
                var newAlerts = res.data || [];
                newAlerts.forEach(function (a) {
                    addAlert('anomaly', '⚠️ Anomaly Detected', "Unusual pattern: $" + a.amount + " (" + a.reason + ")");
                });
            });
        }, 3000);
    });