var urlBase = ".";
angular.module('clientApp', [
    'clientApp.search',
    'jsonFormatter',
    'ngRoute',
    'ngCookies'
]).config(['$routeProvider', function ($routeProvider) {
        $routeProvider.when('/', {templateUrl: './search.html'});
        $routeProvider.when('/ledger', {templateUrl: './ledger_list.html'});
        $routeProvider.when('/search/:index', {templateUrl: './search.html'});
        $routeProvider.when('/server', {templateUrl: './server.html'});
        $routeProvider.when('/ledger/:index', {templateUrl: './ledger.html'});

        $routeProvider.otherwise({redirectTo: '/'});
    }])
    .config(function (JSONFormatterConfigProvider) {

    // Enable the hover preview feature
    JSONFormatterConfigProvider.hoverPreviewEnabled = false;
})
    .run(function ($rootScope, $location, $interval, $http, $cookies) {
        $rootScope.$on('$routeChangeStart', function () {
            $rootScope.path=$location.path();
        });
    });
