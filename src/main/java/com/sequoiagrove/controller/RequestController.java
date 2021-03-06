package com.sequoiagrove.controller;

import com.google.gson.JsonParser;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sequoiagrove.model.RequestStatus;
import com.sequoiagrove.model.RequestRowMapper;
import com.sequoiagrove.controller.MainController;
/**
RequestController:
Puts Starting Date, End Date, and Employee ID from the front end to the datebase
It will also retrieve information from the backend :
*/


@Controller
public class RequestController{

  // extract scope from request
  @ModelAttribute("scope")
    public List<String> getPermissions(HttpServletRequest request) {
      String csvPermissions = (String) request.getAttribute("scope");
      return Arrays.asList(csvPermissions.split(","));
    }

  // employee submits a request for vacation
  @RequestMapping(value = "/request/submit")
    public String sumbitRequest(@RequestBody String data, @ModelAttribute("scope") List<String> permissions, Model model) throws SQLException {

      // the token did not have the required permissions, return 403 status
        if (!(permissions.contains("submit-requests-off") || permissions.contains("admin"))) {
          model.addAttribute("status", HttpServletResponse.SC_FORBIDDEN);
          return "jsonTemplate";
      }

      JdbcTemplate jdbcTemplate = MainController.getJdbcTemplate();
      JsonElement jelement = new JsonParser().parse(data);
      JsonObject jobject = jelement.getAsJsonObject();

      int id = jdbcTemplate.queryForObject("select nextval('sequ_requests_sequence')",
            Integer.class);
      Object[] params = new Object[] {
        id,
        jobject.get("eid").getAsInt(),
        null, false,
        jobject.get("start").getAsString(),
        jobject.get("end").getAsString(),
      };

      jdbcTemplate.update(
          "insert into sequ_requests_vacation"+
          "(id, requested_by, responded_by, is_approved, start_date_time," +
          " end_date_time)" +
          "values(?, ?, ?, ?, "+
          "to_date(?, 'mm-dd-yyyy'), to_date(?, 'mm-dd-yyyy'))",
          params);

      return "jsonTemplate";
    }

  @RequestMapping(value = "/request/get")
    public String getRequest( @ModelAttribute("scope") List<String> permissions, Model model){

      // the token did not have the required permissions, return 403 status
        if (!(permissions.contains("manage-requests") || permissions.contains("admin"))) {
          model.addAttribute("status", HttpServletResponse.SC_FORBIDDEN);
          return "jsonTemplate";
      }

      JdbcTemplate jdbcTemplate = MainController.getJdbcTemplate();
      String queryStr = "select * from sequ_request_view order by start_date_time asc";
      List<RequestStatus> requestList =
            jdbcTemplate.query( queryStr, new RequestRowMapper());

      model.addAttribute("requestStatus", requestList);
      return "jsonTemplate";
    }

  @RequestMapping(value = "/request/get/checked")
    public String getCheckedRequest(Model model, @ModelAttribute("scope") List<String> permissions){

      // the token did not have the required permissions, return 403 status
        if (!(permissions.contains("manage-requests") || permissions.contains("admin"))) {
          model.addAttribute("status", HttpServletResponse.SC_FORBIDDEN);
          return "jsonTemplate";
      }

      JdbcTemplate jdbcTemplate = MainController.getJdbcTemplate();
      String queryStr = "select * from sequ_request_view " +
          "where responded_by IS NOT NULL ";
      List<RequestStatus> requestList =
            jdbcTemplate.query( queryStr, new RequestRowMapper());

      model.addAttribute("requestStatus", requestList);
      return "jsonTemplate";
    }

  @RequestMapping(value = "/request/get/pending")
    public String getPendingRequest(Model model, @ModelAttribute("scope") List<String> permissions){

      // the token did not have the required permissions, return 403 status
      if (!(permissions.contains("manage-requests") || permissions.contains("admin"))) {
          model.addAttribute("status", HttpServletResponse.SC_FORBIDDEN);
          return "jsonTemplate";
      }

      JdbcTemplate jdbcTemplate = MainController.getJdbcTemplate();
      String queryStr = "select * from sequ_request_view "+
          "where responded_by IS NULL and end_date_time >= current_date order by start_date_time asc";
      List<RequestStatus> requestList =
            jdbcTemplate.query( queryStr, new RequestRowMapper());

      model.addAttribute("requestStatus", requestList);
      return "jsonTemplate";
    }

    @RequestMapping(value = "/request/get/current/employee/{eid}")
      public String getCurrentEmployeeRequestl(Model model, @ModelAttribute("scope") List<String> permissions,
          @PathVariable("eid") int eid) throws SQLException {

          // the token did not have the required permissions, return 403 status
          if (!(permissions.contains("submit-requests-off") || permissions.contains("admin"))) {
              model.addAttribute("status", HttpServletResponse.SC_FORBIDDEN);
              return "jsonTemplate";
          }

          JdbcTemplate jdbcTemplate = MainController.getJdbcTemplate();
          String queryStr = "select * from sequ_request_view " +
                  "where requested_by = ? and end_date_time >= current_date " +
                  "order by start_date_time asc";
          Object[] params = new Object[] { eid };

          List<RequestStatus> requestList =
                jdbcTemplate.query( queryStr, params, new RequestRowMapper());

          model.addAttribute("request", requestList);
          return "jsonTemplate";
      }

    // Manager responds to pending request. They approve or deny it.
    @RequestMapping(value = "/request/respond")
      public String updateRequest(@RequestBody String data, @ModelAttribute("scope") List<String> permissions, Model model) throws SQLException{

        // the token did not have the required permissions, return 403 status
        if (!(permissions.contains("manage-requests") || permissions.contains("admin"))) {
            model.addAttribute("status", HttpServletResponse.SC_FORBIDDEN);
            return "jsonTemplate";
        }

        JdbcTemplate jdbcTemplate = MainController.getJdbcTemplate();
        JsonElement jelement = new JsonParser().parse(data);
        JsonObject  jobject = jelement.getAsJsonObject();
        Object[] params = new Object[] {
          jobject.get("isApproved").getAsBoolean(),
          jobject.get("approverId").getAsInt(),
          jobject.get("requestId").getAsInt(),
        };
        jdbcTemplate.update("update sequ_requests_vacation " +
            " set is_approved = ?, responded_by = ? where id = ?", params);

        return "jsonTemplate";
      }

    @RequestMapping(value = "/request/update/dates")
      public String changeRequestDates(@RequestBody String data, @ModelAttribute("scope") List<String> permissions, Model model) throws SQLException {

        // the token did not have the required permissions, return 403 status
        if (!(permissions.contains("manage-requests") || permissions.contains("admin"))) {
            model.addAttribute("status", HttpServletResponse.SC_FORBIDDEN);
            return "jsonTemplate";
        }

        JdbcTemplate jdbcTemplate = MainController.getJdbcTemplate();
        JsonElement jelement = new JsonParser().parse(data);
        JsonObject  jobject = jelement.getAsJsonObject();
        Object[] params = new Object[] {
          jobject.get("eid").getAsInt(),
          jobject.get("startDate").getAsString(),
          jobject.get("endDate").getAsString(),
        };

        /*
         jdbcTemplate.update("update requests_vacation " +
         " set" +
         " start_date_time = " + "to_timestamp(" + start + ", 'yyyy-mm-dd')" +
         " where id = " + eid
         );
         */
        return "jsonTemplate";
      }

    @RequestMapping(value = "/request/{start}/{end}")
      public String getRequestInterval(@PathVariable("start") String start, @PathVariable("end") String end, @ModelAttribute("scope") List<String> permissions, Model model) throws SQLException {
        // the token did not have the required permissions, return 403 status
        if (!(permissions.contains("manage-requests") || permissions.contains("admin"))) {
            model.addAttribute("status", HttpServletResponse.SC_FORBIDDEN);
            return "jsonTemplate";
        }
        Object[] params = new Object[] { start, start, end, end };
        JdbcTemplate jdbcTemplate = MainController.getJdbcTemplate();
        List<RequestStatus> requestList = new ArrayList<RequestStatus>();
        String queryStr = "select * from sequ_request_view " +
            "where (start_date_time <= to_date(?, 'dd-mm-yyyy') and end_date_time >= to_date(?, 'dd-mm-yyyy') ) " +
            "or (end_date_time >= to_date(?, 'dd-mm-yyyy') and start_date_time <= to_date(?, 'dd-mm-yyyy')) " +
            "and is_approved = true";

        try {
          requestList =
            jdbcTemplate.query( queryStr, params, new RequestRowMapper());
        } catch (EmptyResultDataAccessException e) {
          // there were no requests
        };
        model.addAttribute("requests", requestList);
        return "jsonTemplate";
    }
}

