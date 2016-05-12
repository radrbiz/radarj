angular.module('clientApp.search',[])
    .controller('search', ['$scope', '$interval', '$location', '$http', '$route',
    function ($scope, $interval, $location, $http, $route) {

        $scope.open = {
            "account_info":3,
            "account_lines":3,
            "account_offers":3,
            "account_tx":3,
            "ledger":3,
            "tx":3
        };
        var index = $route.current.params.index;

        $scope.expandAll = function(type){
            $scope.open[type]=100;
        }

        $scope.search = function(){
            $scope.open = {
                "account_info":3,
                "account_lines":3,
                "account_offers":3,
                "account_tx":3,
                "ledger":3,
                "tx":3
            };
            delete $scope.is_address;
            delete $scope.is_tx;
            delete $scope.is_ledger;
            if($scope.address && $scope.address.indexOf('r') == 0){
                $scope.is_address = true;
                $scope.accInfoOrLedgerOrTx();
                $scope.accountLine();
                $scope.accountOffer();
                $scope.accountTx();
            }else if(isNaN($scope.address)){
                $scope.is_tx = true;
                $scope.accInfoOrLedgerOrTx();
            }else{
                $scope.is_ledger = true;
                $scope.accInfoOrLedgerOrTx();
            }

        }

        $scope.accInfoOrLedgerOrTx = function(){
            $scope.accinc_loading = true;
            var url = urlBase+"/search/"+$scope.address;
            console.log("-----"+$scope.full)
            if($scope.full){
                url += "?full=1"
            }
            $http.get(url).success(
                function(resp){
                    delete $scope.accinc_loading;
                    $scope.account_info = resp;
                })
        }
        $scope.accountLine = function(){
            $scope.acclin_loading = true;
            $http.get(urlBase+"/search/"+$scope.address+"?type=lines").success(
                function(resp){
                    delete $scope.acclin_loading;
                    $scope.account_lines = resp;
                })
        }
        $scope.accountOffer = function(){
            $scope.accoffer_loading = true;
            $http.get(urlBase+"/search/"+$scope.address+"?type=offers").success(
                function(resp){
                    delete $scope.accoffer_loading;
                    $scope.account_offers = resp;
                })
        }
        $scope.accountTx = function(){
            $scope.acctx_loading = true;
            $http.get(urlBase+"/search/"+$scope.address+"?type=tx").success(
                function(resp){
                    delete $scope.acctx_loading;
                    $scope.account_tx = resp;
                })
        }
        console.log("index:"+index)
        if(index){
            $scope.address = index;
            $scope.search();
        }
    }]).controller('server', ['$scope', '$interval', '$location', '$http', '$route',
    function ($scope, $interval, $location, $http, $route) {

        $scope.loading = true;
        $scope.info = function(){
            delete $scope.error;
            $http.get(urlBase+"/server/info").success(
                function(resp){
                    delete $scope.loading;
                    if(!resp.result || resp.result.status!='success'){
                        $scope.error = true;
                        return;
                    }
                    $scope.server_info = [];
                    $scope.server_info.push({"name":"Server Address", value:"http://api.radarlab.org:5005"})
                    $scope.server_info.push({"name":"Build Version", value:resp.result.info.build_version})
                    $scope.server_info.push({"name":"Complete Ledgers", value:resp.result.info.complete_ledgers})
                    $scope.server_info.push({"name":"IO Latency(ms)", value:resp.result.info.io_latency_ms})
                    $scope.server_info.push({"name":"Peers", value:resp.result.info.peers})
                    $scope.server_info.push({"name":"Server State", value:resp.result.info.server_state})
                    $scope.server_info.push({"name":"Uptime", value:resp.result.info.uptime})
                    $scope.server_info.push({"name":"Validated Ledger", value:resp.result.info.validated_ledger.seq})
                    $scope.server_info.push({"name":"Validation Quorum", value:resp.result.info.validation_quorum})
                    $scope.server_info.push({"name":"Pubkey Node", value:resp.result.info.pubkey_node})
                    $http.get(urlBase+"/server/peers").success(
                        function(resp){
                            if(resp.result && resp.result.peers){
                                $scope.peers = resp.result.peers;
                            }
                        })
                })
        }
        $scope.info();
        $scope.intervalInfo = $interval($scope.info, 5000);
        $scope.stopLoad = function () {
            $interval.cancel($scope.intervalInfo);
        };
        $scope.$on('$locationChangeSuccess', function () {
            $scope.stopLoad();
        });

    }]).controller('ledger', ['$scope', '$interval', '$location', '$http', '$route',
    function ($scope, $interval, $location, $http, $route) {
        $scope.loading = true;
        $scope.ledgerList = function(){
            $http.get(urlBase+"/ledger/recent").success(
                function(resp){
                    delete $scope.loading;
                    $scope.ledgers = resp;
                })
        };
        $scope.ledgerList();
        $scope.intervalLedger = $interval($scope.ledgerList, 5000);
        $scope.stopLoad = function () {
            $interval.cancel($scope.intervalLedger);
        };
        $scope.$on('$locationChangeSuccess', function () {
            $scope.stopLoad();
        });
    }]).controller('ledgerDetail', ['$scope', '$interval', '$location', '$http', '$route',
    function ($scope, $interval, $location, $http, $route) {
        $scope.i = $route.current.params.index;
        $scope.txType={
            "Payment":"label-primary",
            "OfferCreate":"label-info",
            "OfferCancel":"label-info",
            "Dividend":"label-danger",
            "Issue":"label-danger",
            "ActiveAccount":"label-success",
            "AddReferee":"label-warning",
            "AccountSet":"label-warning",
            "SetRegularKey":"label-warning",
            "TrustSet":"label-warning"
        }
        $scope.loading = true;
        $scope.ledger = function(){
            delete $scope.error;
            $http.get(urlBase+"/search/"+$scope.i+"?full=1").success(
                function(resp){
                    delete $scope.loading;
                    console.log(resp)
                    if(!resp.result || resp.result.status!='success'){
                        $scope.error = true;
                    }else
                        $scope.ledger_info = resp;
                })
        };
        $scope.ledger();
    }]).directive('currencyPair', function () {
    function link(scope, element, attrs) {
        var balance;

        function update() {

            if (attrs.rpPrecision) {
                element.text(fmoney(balance / 1000000, attrs.rpPrecision));
            } else {
                element.text(fmoney(balance / 1000000, 6));
            }
        }

        scope.$watch(attrs.rpPrettyAmountNative, function (value) {
            balance = value;
            update();
        });
    }

    return {
        restrict: 'E',
        replace: true,
        scope: {
            amount: '='
        },
        templateUrl: './currency_pair.html',
    };
}).directive('errorPage', function () {
    return {templateUrl: './error.html'}
});