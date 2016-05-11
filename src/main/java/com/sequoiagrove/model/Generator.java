//Generator
//  Info:
//
//-------------------
//Directory
//-------------------
//  Variables_Hold
//  Building
//  Database_Gathering
//  Triming
//  Soon_to_be_Organized
//  Testing
//-------------------
//
package com.sequoiagrove.model;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import com.sequoiagrove.model.Employee;
import com.sequoiagrove.model.Request;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import com.sequoiagrove.controller.MainController;
import java.util.*;

public class Generator{
  //-------------------------
  //  Variables_Hold
  //-------------------------
  HashMap
    <String, HashMap<Integer, HashMap<Integer, Integer> > > generator;
    //[Day [ Shift [Employee, number of weeks scheduled] ] ]

  String startDate;
  String endDate;
  List<DayShiftEmployee> dayShiftEmployeeList;
  Request requests[];

  //-----------------------------------
  //  Building
  //-----------------------------------
  public Generator(){
    generator = new HashMap
      <String, HashMap<Integer, HashMap<Integer, Integer> > >();
  }

  public void addDay(String day){
    if(generator.containsKey(day)) return;
    generator.put(day, new HashMap<Integer, HashMap<Integer, Integer>>());
  }

  public void addShift(String day, Integer shift){
    if(!generator.containsKey(day)) return;
    if(generator.get(day).containsKey(shift)) return;
    generator.get(day).put(shift, new HashMap<Integer, Integer>());
  }

  public void addEmployee(String  day,
                          Integer shift,
                          Integer employee,
                          Integer amount){

    if(!generator.containsKey(day)) return;
    if(!generator.get(day).containsKey(shift)) return;
    if(generator.get(day).get(shift).containsKey(employee)) return;
    generator.get(day).get(shift).put(employee, amount);
  }

  public void add(String  day,
                          Integer shift,
                          Integer employee,
                          Integer amount){
    //This Is Diffrent from addEmployee, as it calls all other 3
    //function to build while making sure keys exists for eachother
    addDay(day);
    addShift(day, shift);
    addEmployee(day, shift, employee,  amount);

  }

  //----------------------------------
  //  Database_Gathering
  //---------------------------------
  public void getPastInformation(String startDate, String endDate){
    JdbcTemplate jdbcTemplate = MainController.getJdbcTemplate();
    dayShiftEmployeeList = jdbcTemplate.query(
        " select day, shift_id, employee_id, count(*) AS worked" +
        " from employee_shift_view " +
        " where on_date >= to_date(?, 'mm-dd-yyyy') AND " +
        " on_date <= to_date(?, 'mm-dd-yyyy') " +
        " group by day, shift_id, employee_id " +
        " order by day, shift_id, employee_id ",


          new RowMapper<DayShiftEmployee>() {
            public DayShiftEmployee mapRow(ResultSet rs, int rowNum) throws SQLException {
              DayShiftEmployee es = new DayShiftEmployee(
                rs.getInt("day"),
                rs.getInt("shift_id"),
                rs.getInt("employee_id"),
                rs.getInt("worked")
              );
              return es;
            }
      }, startDate, endDate);
      for (int i = 0; i < dayShiftEmployeeList.size(); i++) {
        add(convertDay(dayShiftEmployeeList.get(i).getDay()),
                      dayShiftEmployeeList.get(i).getShift(),
                      dayShiftEmployeeList.get(i).getEmployee(),
                      dayShiftEmployeeList.get(i).getWorked() );
      }
      printFormation();
  }

  public void getEmployeeInformation(String startDate, String endDate){
        JdbcTemplate jdbcTemplate = MainController.getJdbcTemplate();
        String queryStr = "select * from employee_info_view";
        List<Employee> empList = jdbcTemplate.query( queryStr,
            new RowMapper<Employee>() {
                public Employee mapRow(ResultSet rs, int rowNum) throws SQLException {
                    Employee employee = new Employee (
                      rs.getInt("id"),
                      rs.getInt("max_hrs_week"),
                      rs.getInt("min_hrs_week"),
                      rs.getBoolean("is_manager"),
                      rs.getInt("clock_number"),
                      rs.getString("first_name"),
                      rs.getString("last_name"),
                      rs.getString("phone_number"),
                      rs.getString("email"),
                      rs.getDate("birth_date"),
                      parseHistory(rs.getString("history")),
                      parsePositions(rs.getString("positions")),
                      parseAvailability(rs.getString("avail")),
                      false);

                      // if the history end is an empty string, employee is current
                      employee.setIsCurrent(
                        employee.getHistory()
                          .get(employee.getHistory().size() - 1)
                          .getEnd() == ""
                      );
                    return employee;
                }
            });
  }

  public WeeklyAvail parseAvailability(String avail) {
      WeeklyAvail entireAvail = new WeeklyAvail();
      // split string into array with one string per day
      String[] weekdays = avail.split("\\s+");
      // for each day, add it to the weekly availability
      for (String d : weekdays) {
        String[] day = d.split(",");
        for(int i=1; i<day.length; i++) {
          String[] times = day[i].split(":");
          entireAvail.add(day[0], times[0], times[1]);
        }
      }
      return entireAvail;
    }

    // change History string to list of java objects
    public List<Duration> parseHistory(String hist) {
      List<Duration> historyList = new ArrayList<Duration>();
      String[] histories = hist.split(",");
      for (String h : histories) {
        String[] times = h.split(":");
        if(times.length == 2) {
          historyList.add(new Duration(times[0], times[1]));
        }
        else {
          historyList.add(new Duration(times[0]));
        }
      }
      return historyList;
    }

    // change Position string to list of java objects
    public List<String> parsePositions(String pos) {
      if (pos == null) {
        return new ArrayList<String>();
      }
      return new ArrayList<String>(Arrays.asList(pos.split(",")));
    }


  public String convertDay(Integer value){
    if(value == 1) return "mon";
    if(value == 2) return "tue";
    if(value == 3) return "wen";
    if(value == 4) return "thu";
    if(value == 5) return "fri";
    if(value == 6) return "sat";
    if(value == 7) return "sun";
    return "---";
  }

 //----------------------------------
 //  Triming
 //---------------------------------
 public void trimByListRestriction(){
 }

 public void trimByRestriction(String person1, String person2){
 }

 public void trimByUnavaliablity(String person1, String date){
 }
 public void trimByRequest(){
  //TODO: Somehow get A Request List and compare to the employees and 
  //      in the week.
 }
 public void removeEmployee(Integer day, Integer shift, Integer employee){
   generator.get(day).get(shift).remove(employee);
 }

 //----------------------------------
 //Soon_to_be_Organized
 //----------------------------------
 // use this as a temporary way place to add function
 //
 public boolean checkEmployeeIf(
                             String  day,
                             Integer shift, 
                             Integer employee1,
                             Integer employee2){
   if(generator.get(day).get(shift).containsKey(employee1) &&
      generator.get(day).get(shift).containsKey(employee2)){

        return true;
      }
   return false;
 }

  //----------------------------------
  // Testing
  //----------------------------------
  //  Used Primarily to Test The Class
  public void printAllDays(){
    for (String key : generator.keySet()) {
      System.out.println(key + " ");
    }
  }

  public void printAllShiftsInDay(String day){
    for (Integer key : generator.get(day).keySet()) {
      System.out.println(key + " ");
    }
  }

 public void printAllEmployeesInShift(String day, Integer shift){
    for (Integer key : generator.get(day).get(shift).keySet()){
      System.out.println(key + " " +
        generator.get(day).get(shift).get(key));
    }
 }

 public void printFormation(){
   for (String dayKey : generator.keySet()) {
     System.out.println(dayKey);

     for (Integer shiftKey : generator.get(dayKey).keySet()) {
       System.out.println("   " + shiftKey);

       for (Integer empKey : generator.get(dayKey).get(shiftKey).keySet()){
         System.out.println("      " + empKey + " " +
             generator.get(dayKey).get(shiftKey).get(empKey));
       }
     }
   }
 }

}
