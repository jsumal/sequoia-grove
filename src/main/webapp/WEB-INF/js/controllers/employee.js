'use strict';

/**
 * @ngdoc function
 * @name sequoiaGroveApp.controller:EmployeeCtrl
 * @description
 * # EmployeeCtrl
 * Controller for managing employees.
 */
angular.module('sequoiaGroveApp')
  .controller('EmployeeCtrl', function ($http, $log, $scope, $rootScope, $location) {

/************** Login Redirect, Containers and UI settings **************/

    $rootScope.lastPath = '/employee';

    // user is not logged in
    if ($rootScope.loggedIn == false) {
      $location.path('/login');
    }

    $scope.activeTab = 'info';
    $scope.current;
    $scope.selectedEmployee = {id:0};
    $scope.newAvail = {day:'', start:'', end:''};
    $scope.newPos = {};
    $scope.saving = false;
    $scope.typeFilter = 'current';

/************** Pure Functions **************/

    // switch filter of employee list type for all, current or past
    $scope.changeType = function(type) {
      $scope.typeFilter = type;
    }

    // filter employee list by all, current, or past employees
    $scope.filterByType = function(isCurrent) {
      if ($scope.typeFilter === 'all') {
        return true;
      }
      else if ($scope.typeFilter === 'current') {
        return isCurrent;
      }
      else if ($scope.typeFilter === 'past') {
        return !isCurrent;
      }
    }

    $scope.formatEmploymentHistory = function(dateString) {
      if (dateString=='') {
        return 'Present';
      }
      return moment(dateString,'MM-DD-YYYY').format('MMMM Do, YYYY');
    }

    // click existing times to populate input with those times
    $scope.setNewAvailTimes = function(sH, sM, eH, eM) {
      var start = moment({hour:sH, minute:sM}).format('h:mm a');
      var end = moment({hour:eH, minute:eM}).format('h:mm a');
      $scope.newAvail.start = start;
      $scope.newAvail.end = end;
    }

    $scope.selectEmployee = function(id) {
      var length = $scope.employees.length;
      for(var i = 0; i < length; i++) {
        var curid = $scope.employees[i].id;
        if (curid==id) {
          $scope.selectedEmployee = $scope.employees[i];
          break;
        }
      }
    }

    // reset selected employee
    $scope.clearEmployee = function() {
      $scope.selectedEmployee = {'id':0};
    }

/************** HTTP Request Functions **************/

    // add a new availability time for an employee
    $scope.addAvailability = function() {
      // guard against double clicking
      if ($scope.saving) {
        return;
      }
      $scope.saving = true;

      var avail = {
        'eid':$scope.selectedEmployee.id,
        'day': $scope.newAvail.day,
        'start': $scope.newAvail.start.val,
        'end': $scope.newAvail.end.val
      }

      // make sure all fields are filled in
      if (avail.day!='' && avail.start!='' && avail.end!='') {

        $log.debug(avail);
        $http({
          url: '/sequoiagrove/avail/add',
          method: "POST",
          data: avail
        }).success(function(data, status) {
          $log.debug(data, status);
          // update front end
          $scope.selectedEmployee.avail[$scope.newAvail.day].push(
            {'start':avail.start, 'end':avail.end});
          $scope.saving = false;
        }).error(function(data, status) {
          $log.debug(data, status);
        });
      }
    }

    $scope.getPositionTitle = function(pid) {
      var title = "";
      _.map($scope.positions, function(p) {
      if (p.id === parseInt(pid)) {
          title = p.title;
        }
      });
      return title;
    }

    // add a new position for an employee
    $scope.addPosition = function() {
      // guard against double clicking
      if ($scope.saving) {
        return;
      }
      $scope.saving = true;
      var pid = $scope.newPos.id;
      var obj = { 'pid':pid, 'eid':$scope.selectedEmployee.id };
      // reset input
      $scope.newPos = {};

      if ($scope.employeeHasPosition(obj.eid, pid) === false) {
        // send new position to back end
        $http({
          url: '/sequoiagrove/position/add/',
          method: "POST",
          data: obj
        }).success(function(data, status, headers, config) {
            $scope.saving = false;
            // update front end
            $scope.selectedEmployee.positions.push(pid);
        }).error(function(data, status) {
          $log.debug(status, 'failed to add position(', pid, ') for employee(', obj.eid, ')');
        });
      }
    }

    // remove an availability for an employee
    $scope.removeAvailability = function(day, index) {
      // guard against double clicking
      if ($scope.saving) {
        return;
      }
      $scope.saving = true;
        // update front end, and get start_time
        var start = $scope.selectedEmployee.avail[day][index].start;
        $scope.selectedEmployee.avail[day].splice(index, 1);

        // remove availability from database
        $http({
            url: '/sequoiagrove/avail/remove/'+
                $scope.selectedEmployee.id + '/' + day + '/' + start,
            method: "POST"
        }).success(function(data, status) {
          $scope.saving = false;
        });
    }

    $scope.removePosition = function(pid) {
      // guard against double clicking
      if ($scope.saving) {
        return;
      }
      $scope.saving = true;
      var eid = $scope.selectedEmployee.id;

      // remove the position from the employee (front end)
      $scope.employees = _.map($scope.employees, function(e) {
        if (parseInt(e.id) === parseInt(eid)) {
          e.positions = _.reject(e.positions, function(id) {
            return parseInt(id) === parseInt(pid);
          });
        }
        return e;
      });

      // remove position from the employee (back end)
      var obj = { 'pid':pid, 'eid':eid };
      $http({
        url: '/sequoiagrove/position/remove/',
        method: "POST",
        data: obj
      }).success(function(data, status) {
        $scope.saving = false;
      }).error(function(data, status) {
        $log.debug('error removing position',pid,'from',eid);
      });
    }

    $scope.updateEmployee = function() {
      $scope.selectedEmployee.isManager = "1";
      // guard against double clicking
      if ($scope.saving) {
        return;
      }
      $scope.saving = true;
      var action = "update";
      if ($scope.selectedEmployee.id === 0) {
        action = "add"
        // TODO make it so a new employee can be added - there are
        // lots of checks that need to be made before selectedEmployee can
        // be sent to the back end, for now, disallow new employee additions
        return;
      }

      $http.post("/sequoiagrove/employee/"+action, $scope.selectedEmployee).
        success(function(data, status){
          $scope.saving = false;
        });
    }

    // Deactivate (un-employ) an employee
    $scope.deactivateEmployee = function() {
      $http({
        url: '/sequoiagrove/employee/deactivate/',
        method: "POST",
        data: {'id': $scope.selectedEmployee.id}
      }).success(function(data, status) {

        // update UI with change
        $scope.employees = _.map($scope.employees, function(e) {
          if(e.id === $scope.selectedEmployee.id) {
            e.isCurrent = false;
            e.history = _.map(e.history, function(h) {
              if(h.end === '') {
                h.end = moment().format('MM-DD-YYYY');
              }
              return h;
            });
          }
          return e;
        });

      }).error(function(data, status) {
        $log.debug("error deactivating employee: ", $scope.selectedEmployee.id, status);
      });
    }

    // Activate (re-employ) an employee
    $scope.activateEmployee = function() {
      $http({
        url: '/sequoiagrove/employee/activate/',
        method: "POST",
        data: {'id': $scope.selectedEmployee.id}
      }).success(function(data, status) {

        // update UI with change
        $scope.employees = _.map($scope.employees, function(e) {
          if(e.id === $scope.selectedEmployee.id) {
            e.isCurrent = true;
            e.history = _.union(e.history,
              [{'start': moment().format('MM-DD-YYYY'), 'end':''}])
          }
          return e;
        });

      }).error(function(data, status) {
        $log.debug("error activating employee: ", $scope.selectedEmployee.id, status);
      });
    }

});
