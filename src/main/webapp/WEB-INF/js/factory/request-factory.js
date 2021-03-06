'use strict';

// Factory to inject authorization token with each request sent
angular.module('sequoiaGroveApp').factory('requestFactory', function ( $log, localStorageService, $q, $http, $rootScope, $timeout, $mdDialog) {
  var service = {};
  var observerCallbacks = [];
  var requestsNum = 0;

  var pending = [];
  var user = [];
  var all = [];

  // call this to notify observers
  var notifyObservers = function(){
    angular.forEach(observerCallbacks, function(callback){
      callback();
    });
  };

  function initPending() {
    var deferred = $q.defer();
    $http({
      url: '/sequoiagrove/request/get/pending',
      method: 'GET'
    }).then(function(success) {
      pending = success.data.requestStatus;
      requestsNum = pending.length;
      deferred.resolve(success);
    });
    return deferred.promise;
  }

  function initUser(id) {
    var deferred = $q.defer();
    $http({
      url: '/sequoiagrove/request/get/current/employee/'+ id,
      method: 'POST'
    }).then(function(success) {
      user = success.data.request;
      deferred.resolve(success);
    });
    return deferred.promise;
  }

  function initAll() {
    var deferred = $q.defer();
    return $http({
      url: '/sequoiagrove/request/get/checked',
      method: 'GET'
    }).then(function (success) {
      all = success.data.requestStatus
      deferred.resolve(success);
    });
    return deferred.promise;
  }

  function submit(request, ev){
    var deferred = $q.defer();
    var obj = {'eid':request.eid,
      'start': moment(request.start).format('MM-DD-YYYY'),
      'end': moment(request.end).format('MM-DD-YYYY')
    }

    checkDatesCollide(obj.start, obj.end, ev).then(
        function(success) {
          // dates conflicted
          if(success) {
            //deferred.resolve(success);
          }
          //else {
            confirmSubmit(obj, ev).then(function(success) {
              if(success === true) {
                $http({ url: '/sequoiagrove/request/submit/',
                  method: 'POST',
                  data: obj
                }).then(function (success){
                  notifyObservers();
                  deferred.resolve(success);
                },function(failure) {
                  //notifyObservers();
                  deferred.reject(failure);
                });
              }
              else {
                deferred.resolve(success);
              }
            });
          //}
        });
    return deferred.promise;
  }

  // makes sure user hasn't submitted overlapping requests
  function checkDatesCollide(start, end, ev){
    var deferred = $q.defer();
    /*
    var i = 0;
    //var submitStart = moment(start);
    //var submitEnd = moment(end);
    var collision = false;

    $mdDialog.show( $mdDialog.alert()
      .parent(angular.element(document.querySelector('#popupContainer')))
      .clickOutsideToClose(true)
      .title('Request Denied')
      .textContent('Dates Are Already in use')
      .ariaLabel('Alert Dialog Demo')
      .targetEvent(ev)
      .ok('Got it!'))
      .then(function(success) {
      */
        deferred.resolve(true);
        /*
    });
    */

    /*
    for(i = 0; i < $scope.userRequests.length; i++){
      var checkStart = moment($scope.userRequests[i].startDate).subtract(1, 'day');
      var checkEnd   = moment($scope.userRequests[i].endDate).add(1, 'day');

      if(moment(submitStart).isBetween(checkStart, checkEnd)){
        return true;
      }
      if(moment(submitEnd).isBetween(checkStart, checkEnd)){
        return true;
      }
      if(moment(checkStart).isBetween(submitStart, submitEnd)){
        return true;
      }
      if(moment(checkEnd).isBetween(submitStart, submitEnd)){
        return true;
      }
    }
    */
    return deferred.promise;
    //if (collision) {
      //datesCollidePopup();
    //}
  }

  function confirmSubmit(request, ev) {
    var deferred = $q.defer();
    var message = 'from '+  moment(request.start, 'MM-DD-YYYY').format('MMMM Do') +
      ' to ' +  moment(request.end, 'MM-DD-YYYY').format('MMMM Do');

    if(request.days === 1) {
      message = 'Date: ' + moment(request.start, 'MM-DD-YYYY').format('MMMM Do');
    }

    // Appending dialog to document.body to cover sidenav in docs app
    var confirmPopup = $mdDialog.confirm()
      .title('Submit Request?')
      .textContent(message)
      .ariaLabel('Request Submit')
      .targetEvent(ev)
      .ok('OK')
      .cancel('Cancel');

    $mdDialog.show(confirmPopup).then(function() {
      deferred.resolve(true);
    }, function() {
      deferred.resolve(false);
    });

    return deferred.promise;
  };

  function updateRequest(request, isApproved, approverId, totalDays) {
    var deferred = $q.defer();
    var requestId = request.requestID;
    var title = (isApproved === true)? 'Approve Request for ': 'Deny Request for ';
    title += request.employeeFirstName + '? (' + totalDays;
    title += ((totalDays > 1)? ' days' : ' day') + ')';
    var message =  moment(request.startDate).format('MMMM Do');
    if (totalDays > 1) {
      var message =
        'from '+  moment(request.startDate).format('MMMM Do') +
        ' to ' +  moment(request.endDate).format('MMMM Do,');
    }

    var confirmDialog = $mdDialog.confirm()
      .title(title)
      .textContent(message)
      .ariaLabel('Request Respond')
      .ok((isApproved === true)? 'Approve':'Deny')
      .cancel('Cancel');

    $mdDialog.show(confirmDialog).then(function(success) {
      // request response confirmed
      if (success) {
        $http({ url: '/sequoiagrove/request/respond',
          method: 'POST',
          data: {'requestId':requestId, 'approverId':approverId, 'isApproved':isApproved}
        }).then(function(success) {
          initPending().then(function(success) {
            return initUser(approverId);
          }).then(function(success) {
            return initAll();
          }).then(function(success) {
            notifyObservers();
            deferred.resolve(success);
          }).then(function(success) {
            deferred.resolve(success);
          });
        });
      }
      // request response cancelled
      else {
        deferred.resolve(success);
      }
    });
    return deferred.promise;
  }


  var setManagePrivelage = function() {
    service.init = function(id) {
      var deferred = $q.defer();
      initPending().then(function(success) {
        return initUser(id);
      }).then(function(success) {
        return initAll();
      }).then(function(success) {
        notifyObservers();
        deferred.resolve(success);
      });
    };
    service.getNumberPending = function() {
      return requestsNum;
    };
    service.getPending = function() {
      return pending;
    };
    service.getUser = function() {
      return user;
    };
    service.submit = function(request, ev) {
      //TODO make submit specific to manager/non-manager roles
      //for now, all is stuck in manager.
      submit(request, ev).then(function(success) {
        initPending().then(function(success) {
          return initUser(request.eid);
        }).then(function(success) {
          return initAll();
        }).then(function(success) {
          notifyObservers();
        });
      });
    };
    service.getAll = function() {
      return all;
    };
    service.removeManagePrivelage = function() {
      service = removeManagePrivelage();
    }
    service.updateRequest = function(request, isApproved, approverId, totalDays) {
      updateRequest(request, isApproved, approverId, totalDays);
    }
  }

  var removeManagePrivelage = function() {
  // Exposed factory functionality
    return {
      'init': function() {
      },
      'getNumberPending': function() {
        return 0;
      },
      'getPending': function() {
        return [];
      },
      'submit': function(request, ev) {
        return submit(request, ev);
      },
      'setManagePrivelage': function() {
        setManagePrivelage();
      },
      'removeManagePrivelage': function() {
        // do nothing
      }
    }
  }

  var service = removeManagePrivelage();

  // register observers
  service.registerObserverCallback = function(callback){
    observerCallbacks.push(callback);
  };

  return service
});

