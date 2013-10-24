/*
 * Copyright (c) 2013
 */

angular.module('play2sec', ["ngResource"], ['$provide', function($provide) {

  $provide.factory('$authRpcService', ['$resource', function($resource) {
    return $resource("/auth/:action", {}, {
      login:  { method: 'POST', params: {"action": "login"} },
      signup: { method: 'PUT', params: {"action": "signup"} }
    });
  }]);

  $provide.factory('$rpcWrapper', ['$rootScope', '$timeout', function(rootScope, timeout) {
    var service = {};
    /**
     * Request shorter than this would not cause progress indicator to appear.
     * @@type {number}
     */
    var delay = 200;
    service.call = function(mainCall, mainCallParams, scope, successCallback, errorCallback) {
      timeout(function() { rootScope.$broadcast("onRpcStart", {}); }, delay);
      scope.rpcResult = {};
      return mainCall(mainCallParams, function success(response) {
        if (successCallback !== undefined) successCallback(response);
        scope.rpcResult.success = response;
        timeout(function() { rootScope.$broadcast("onRpcEnd", {}); });
      }, function error(response) {
        if (errorCallback !== undefined) errorCallback(response);
        scope.rpcResult[response.data.status] = response.data;
        if (response.status === 403) {
          rootScope.$broadcast("onAccessDenied", {message: scope.rpcResult[response.data.status].message});
        }
        if (response.status === 500) {
          rootScope.$broadcast("onServerError", {message: "Unknown error happened. Please try again in a moment."});
        }
        timeout(function() { rootScope.$broadcast("onRpcEnd", {}); });
      });
    };
    return service;
  }]);

}]);

function UserCtrl($scope, $rpcWrapper, $authRpcService) {
  $scope.signup = function() {
    $rpcWrapper.call($authRpcService.signup, $scope.user, $scope);
  };

  $scope.login = function() {
    $rpcWrapper.call($authRpcService.login, $scope.user, $scope);
  };
}

UserCtrl.$inject = ['$scope', '$rpcWrapper', '$authRpcService'];
