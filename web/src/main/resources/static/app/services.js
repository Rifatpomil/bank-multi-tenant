'use strict';

angular.module('bankMultiTenant')
    .service('TenantService', function () {
        var STORAGE_KEY = 'bank-multi-tenant-tenant-id';
        var currentTenant = (function () {
            try {
                return sessionStorage.getItem(STORAGE_KEY) || 'default';
            } catch (e) {
                return 'default';
            }
        })();
        return {
            getTenantId: function () {
                return currentTenant;
            },
            setTenantId: function (id) {
                currentTenant = id || 'default';
                try {
                    sessionStorage.setItem(STORAGE_KEY, currentTenant);
                } catch (e) {}
                return currentTenant;
            }
        };
    })
    .service('BankAccountService', function ($stomp, $q, TenantService) {

        var isConnected = false;

        function tenantHeaders() {
            return { 'tenant-id': TenantService.getTenantId() };
        }

        return {
            connect: function () {
                return $q(function (resolve, reject) {
                    if (!isConnected) {
                        $stomp.connect('/websocket', tenantHeaders())
                            .then(function (frame) {
                                isConnected = true;
                                resolve();
                            })
                            .catch(function (reason) {
                                reject(reason);
                            });
                    }
                    else {
                        resolve();
                    }
                });
            },
            loadBankAccounts: function () {
                return $q(function (resolve, reject) {
                    $stomp.subscribe('/app/bank-accounts', function (data) {
                        resolve(data);
                    }, tenantHeaders());
                });
            },
            loadBankTransfers: function (bankAccountId) {
                return $q(function (resolve, reject) {
                    $stomp.subscribe('/app/bank-accounts/' + bankAccountId + '/bank-transfers', function (data) {
                        resolve(data);
                    }, tenantHeaders());
                });
            },
            subscribeToBankAccountUpdates: function () {
                var deferred = $q.defer();
                var topic = '/topic/bank-accounts.updates.' + TenantService.getTenantId();
                $stomp.subscribe(topic, function (data) {
                    deferred.notify(data);
                }, tenantHeaders());
                return deferred.promise;
            },

            createBankAccount: function (overdraftLimit) {
                console.log("WebSocket sending create command with limit:", overdraftLimit);
                $stomp.send('/app/bank-accounts/create', { overdraftLimit: overdraftLimit }, tenantHeaders());
            },
            deposit: function (data) {
                $stomp.send('/app/bank-accounts/deposit', data, tenantHeaders());
            },
            withdraw: function (data) {
                $stomp.send('/app/bank-accounts/withdraw', data, tenantHeaders());
            },
            transfer: function (data) {
                $stomp.send('/app/bank-transfers/create', data, tenantHeaders());
            }
        };
    })
    .service('AiService', function ($http, TenantService) {
        var baseUrl = '/api/ai';
        function getHeaders() {
            return {
                headers: { 'X-Tenant-ID': TenantService.getTenantId() }
            };
        }
        return {
            executeCommand: function (commandText) {
                return $http.post(baseUrl + '/nlp/execute', { command: commandText }, getHeaders());
            },
            getInsights: function (accountId) {
                return $http.post(baseUrl + '/insights', { bankAccountId: accountId }, getHeaders());
            },
            getForecast: function (accountId, days) {
                return $http.get(baseUrl + '/forecast/' + accountId + '?days=' + (days || 30), getHeaders());
            },
            getFraudAlerts: function () {
                return $http.get(baseUrl + '/fraud/alerts', getHeaders());
            },
            getAnomalyAlerts: function () {
                return $http.get(baseUrl + '/anomaly/alerts', getHeaders());
            },
            getCategorizedTransactions: function (accountId) {
                return $http.get(baseUrl + '/categorization/' + accountId + '?page=0&size=50', getHeaders());
            }
        };
    });