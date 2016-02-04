'use strict';

/**
 * @ngdoc function
 * @name sequoiaGroveApp.controller:ScheduleCtrl
 * @description
 * # ScheduleCtrl
 * Controller for editing the schedule.
 */
angular.module('sequoiaGroveApp')
  .controller('ScheduleCtrl', function (
        $filter,
        $window,
        $location,
        $http,
        $log,
        $rootScope,
        $scope,
        $timeout,
        $translate) {

/************** Login Redirect, Containers and UI settings **************/

  // user is not logged in
  if ($rootScope.loggedIn == false) {
    $location.path('/login');
  }

  $scope.activeTab = 'schedule';
  $scope.selectedId = 0;
  $scope.newDelivery = '';
  $scope.selectedPid = 0;
  $scope.empEditSearch = '';
  $scope.saving = false;
  $scope.selectedShift = {
    idx : -1,
    title : '',
    pos : '',
    wd_st : '',
    wd_ed : '',
    we_st : '',
    we_ed : ''
  };

/************** Pure Functions **************/

  // Call browser to print schedule on paper
  $scope.print = function() {
    $window.print();
  }

  $scope.selectEid = function(id) {
    $scope.selectedId = id;
  }

  $scope.selectShift = function(cur) {
    $scope.selectedShift.idx = cur;
    if ($scope.selectedShift.idx != -1) {
      $scope.selectedShift.title = $scope.template[cur].tname;
      $scope.selectedShift.pos = $scope.template[cur].position;
      $scope.selectedShift.wd_st = moment($scope.template[cur].wd_st_h + ':' + $scope.template[cur].wd_st_m, 'HH:mm').format('h:mm A');
      $scope.selectedShift.wd_ed = moment($scope.template[cur].wd_ed_h + ':' + $scope.template[cur].wd_ed_m, 'HH:mm').format('h:mm A');
      $scope.selectedShift.we_st = moment($scope.template[cur].we_st_h + ':' + $scope.template[cur].we_st_m, 'HH:mm').format('h:mm A');
      $scope.selectedShift.we_ed = moment($scope.template[cur].we_ed_h + ':' + $scope.template[cur].we_ed_m, 'HH:mm').format('h:mm A');
    }
    else {
      $scope.selectedShift.title = '';
      $scope.selectedShift.pos = '';
      $scope.selectedShift.wd_st = '';
      $scope.selectedShift.wd_ed = '';
      $scope.selectedShift.we_st = '';
      $scope.selectedShift.we_ed = '';
    }
  }

  $scope.shiftSelected = function() {
    return ($scope.selectedShift.idx != -1);
  }

  $scope.switchPos = function(pos) {
    $scope.empEditSearch = '';
    $scope.selectedPid = pos;
  }

  // Filter employees by selected position
  $scope.filterEmployees = function(eid) {
    if($scope.selectedPid == 0) {
      return true;
    }
    var hasPos = $scope.hasPosition[$scope.selectedPid];
    var i=0;
    var len = hasPos.length;

    for(; i<len; i++) {
      // this employee has this position
      if(hasPos[i] == eid){
        return true;
      }
    }
    return false;
  }

  // Filter schedule by selected position
  $scope.filterSchedule = function(pid) {
    if($scope.selectedPid == 0) {
      return true;
    }
    if(pid == $scope.selectedPid) {
      return true;
    }
    return false;
  }


  // find the matching employee by name
  $scope.getEmployeeByname = function(name) {
    var employee = {'id':0};
    _.map($scope.currentEmployees, function(e) {
      if(_.isMatch(e, {'firstName':name})) {
        employee = e;
      }
    });
    return employee;
  }

  // get if employee is available
  $scope.employeeIsAvailable = function(attrs, employee) {
    var avail = [];
    var isAvailable = false;

    // 1. get employee availability
    avail = _.map(employee.avail[attrs.day], function(a) {
      return {
        'start':moment(attrs.date +' '+ a.startHr +' '+ a.startMin, 'DD-MM-YYYY hh mm'),
        'end':moment(attrs.date +' '+ a.endHr +' '+ a.endMin, 'DD-MM-YYYY hh mm')
      }
    });
    if (avail.length <=0 ) {
      return false;
    }

    // 2. determine shift duration times
    var shiftStart = moment(attrs.date + ' ' + attrs.sthr+attrs.stmin, 'DD-MM-YYYY hhmm');
    var shiftEnd = moment(attrs.date + ' ' + attrs.endhr+attrs.endmin, 'DD-MM-YYYY hhmm');

    // 3. check employee availability against shift duration
    _.map(avail, function(a, index) {
      if (a.start.isBefore(shiftStart) && a.end.isAfter(shiftEnd)) {
        isAvailable = true;
      }
    });
    return isAvailable;
  }

  // validation for schedule edit input
  $scope.inputStatus = function(id, shiftId) {
    var style = 'form-control schedule-edit-input';

    // Highlight all occurences of the employee that was clicked
    if (id == $scope.selectedId) {
      style += ' schedule-edit-highlight';
    }
    // Dummy Error/Warning Application
    /* // apply an error
    if (weekday=='monday' && shiftId == '3') {
      style += ' schedule-edit-input-error';
    }
    // apply a warning
    else if(weekday=='thursday' && shiftId == '2') {
      style += ' schedule-edit-input-warn';
    }
    // no warnings or errors
    else {
      style += ' schedule-edit-input-highlight';
    } */
    return style;
  }

  $scope.shiftStatus = function(i) {
    var style = '';
    if(i===$scope.selectedShift.idx) {
      style += 'schedule-edit-task-selected';
    }
    return style;
  }

  $scope.removeDelivery = function(index) {
    // remove delivery from dummy list for now
    $scope.deliveries.splice(index, 1);
  }

  // add delivery to front end
  $scope.addDelivery = function() {
    if ($scope.newDelivery != '') {
        $scope.deliveries.push(
        { title: $scope.newDelivery,
          days: {
            monday:    false,
            tuesday:   false,
            wednesday: false,
            thursday:  false,
            friday:    false,
            saturday:  false,
            sunday:    false}
        })
        $scope.newDelivery = '';
    }
  }

  // a shift was typed in blank, add it to delete list, if it isn't
  // already in there
  $scope.addToDeleteList = function(obj) {
    var isInDeleteList = false;
    obj.sid = parseInt(obj.sid);
    _.map($scope.deleteShifts, function(shift, index, list) {
      if( _.isEqual(shift, obj)) {
        isInDeleteList = true;
      }
    });
    if (isInDeleteList === false) {
      $scope.deleteShifts.push({'sid':obj.sid, 'date':obj.date});
    }
  }

  // tracks changes by keeping update list current
  $scope.trackScheduleChange = function(eid, sid, date) {
    sid = parseInt(sid);
    var paramObj = {'eid':eid, 'sid':sid, 'date':date};
    var inOriginal = false;
    var inUpdate = false;
    var originalIndex = -1;
    var updateIndex = -1;

    // check if this shift is in the update list
    _.map($scope.updateShifts, function(shift, index, list) {
      if (_.isMatch(shift, { 'sid':sid, 'date':date})) {
        inUpdate = true;
        updateIndex = index;
      }
    });

    // check if this shift is in the original list
    _.map($scope.originalTemplate, function(shift, index, list) {
      if( _.isEqual(shift, paramObj)) {
        inOriginal = true;
        originalIndex = index;
      }
    });

    // check if this shift is in the delete list
    _.map($scope.deleteShifts, function(shift, index, list) {
      if(_.isMatch(shift, {'sid':sid, 'date':date})) {
        // remove this from delete shifts, because if this function
        // was called, it means this shift was assigned a name
        $scope.deleteShifts.splice(index, 1);
      }
    });

    // decide what to do with the info gathered above
    if (inOriginal && inUpdate) {
      // item needs to be removed from update
      $scope.updateShifts.splice(updateIndex, 1);
    }
    else if (inOriginal && !inUpdate) {
      // do nothing
    }
    else if (!inOriginal && inUpdate) {
      // update the update list
      $scope.updateShifts.splice(updateIndex, 1);
      $scope.updateShifts.push(paramObj);
    }
    else if (!inOriginal && !inUpdate) {
      // add item to update list
      $scope.updateShifts.push(paramObj);
    }

    $scope.selectEid(eid);
  }


  // adds all shifts to delete list, so they are deleted when save is clicked
  $scope.clearSchedule = function() {
    $scope.deleteShifts = [];
    _.map($scope.template, function(t, index, list) {
      $scope.deleteShifts.push({'sid':t.sid, 'date':$scope.date.mon.val});
      $scope.deleteShifts.push({'sid':t.sid, 'date':$scope.date.tue.val});
      $scope.deleteShifts.push({'sid':t.sid, 'date':$scope.date.wed.val});
      $scope.deleteShifts.push({'sid':t.sid, 'date':$scope.date.thu.val});
      $scope.deleteShifts.push({'sid':t.sid, 'date':$scope.date.fri.val});
      $scope.deleteShifts.push({'sid':t.sid, 'date':$scope.date.sat.val});
      $scope.deleteShifts.push({'sid':t.sid, 'date':$scope.date.sun.val});

      t.mon.name = ""; t.mon.eid = 0;
      t.tue.name = ""; t.tue.eid = 0;
      t.wed.name = ""; t.wed.eid = 0;
      t.thu.name = ""; t.thu.eid = 0;
      t.fri.name = ""; t.fri.eid = 0;
      t.sat.name = ""; t.sat.eid = 0;
      t.sun.name = ""; t.sun.eid = 0;
    });
  }

  $scope.importLastWeek = function() {
    var d = moment($scope.date.mon.val,'DD-MM-YYYY').subtract(7, 'days').format('DD-MM-YYYY');
    $scope.getScheduleTemplate(d);
  }

/************** HTTP Request Functions **************/

  // Save the shifts in the list of updateShifts
  $scope.saveSchedule = function() {
    // remove blank spaces from update list - they are in delete shifts, or
    // have not been assigned
    $scope.updateShifts = _.filter($scope.updateShifts, function(shift) {
      return (shift.eid !== 0);
    });
    $log.debug($scope.updateShifts);
    $scope.saving = true;

    $http({
      url: '/sequoiagrove/schedule/update/',
      method: "POST",
      data: { 'body': JSON.stringify($scope.updateShifts) }
    }).success(function (data, status, headers, config) {
      if (status == 200) {
        // clear update shifts list
        $scope.updateShifts = [];
        $scope.saving = false;
        $scope.deleteSchedule();
      }
      else {
        $log.error('Error saving schedule ', status, data);
      }
    }).error(function (data, status, headers, config) {
      $log.error(status + " Error saving schedule " + data);
      $scope.saving = false;
    });
  }

  // Delete all these shifts
  $scope.deleteSchedule = function() {
    $scope.saving = true;

    $http({
      url: '/sequoiagrove/schedule/delete/',
      method: "DELETE",
      data: { 'body': JSON.stringify($scope.deleteShifts) }
    }).success(function (data, status, headers, config) {
      if (status == 200) {
        // clear delete shifts list
        $scope.deleteShifts = [];
        $scope.getScheduleTemplate($scope.date.mon.val);
        $scope.saving = false;
      }
      else {
        $log.error('Error deleting schedule ', status, data);
      }
    }).error(function (data, status, headers, config) {
      $log.error(status + " Error deleting schedule " + data);
    });
  }

/************** Controller Initialization **************/

  $scope.init = function() {
  }

  $scope.init();

});
