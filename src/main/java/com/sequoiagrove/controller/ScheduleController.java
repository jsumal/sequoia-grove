package com.sequoiagrove.controller;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.sequoiagrove.controller.MainController;
import com.sequoiagrove.model.Day;
import com.sequoiagrove.model.ScheduleTemplate;
import com.sequoiagrove.model.Scheduled;

@Controller
public class ScheduleController {

  // extract scope from request
  @ModelAttribute("scope")
    public List<String> getPermissions(HttpServletRequest request) {
      String csvPermissions = (String) request.getAttribute("scope");
      return Arrays.asList(csvPermissions.split(","));
    }

/* ----- HTTP Mapped Functions -----*/
  // Get current schedule template (current shifts) dd-mm-yyyy
  @RequestMapping(value = "/schedule/template/{mon}")
    public String getScheduleTemplate(Model model, @ModelAttribute("scope") List<String> permissions, @PathVariable("mon") final String mon) {

      JdbcTemplate jdbcTemplate = MainController.getJdbcTemplate();

      List<ScheduleTemplate> schTempList = jdbcTemplate.query(
        "select * from sequ_get_schedule(?)",
        new Object[]{mon},
        new RowMapper<ScheduleTemplate>() {
          public ScheduleTemplate mapRow(ResultSet rs, int rowNum) throws SQLException {

            ScheduleTemplate schTmp = new ScheduleTemplate(
                rs.getInt("index"),
                rs.getInt("sid"),
                rs.getInt("pid"),
                rs.getString("location"),
                rs.getString("tname"),
                rs.getString("pos"),
                rs.getString("wd_st"),// weekday start
                rs.getString("wd_ed"),// weekday end
                rs.getString("we_st"),// weekend start
                rs.getString("we_ed"),// weekend end
                new Day("mon", rs.getString("mon"), rs.getInt("mon_eid")),
                new Day("tue", rs.getString("tue"), rs.getInt("tue_eid")),
                new Day("wed", rs.getString("wed"), rs.getInt("wed_eid")),
                new Day("thu", rs.getString("thu"), rs.getInt("thu_eid")),
                new Day("fri", rs.getString("fri"), rs.getInt("fri_eid")),
                new Day("sat", rs.getString("sat"), rs.getInt("sat_eid")),
                new Day("sun", rs.getString("sun"), rs.getInt("sun_eid")) );

            return schTmp;
          }
        });

      Integer count = jdbcTemplate.queryForObject(
          "SELECT count(*) FROM sequ_published_schedule WHERE start_date = to_date(?,'dd-mm-yyyy')",Integer.class, mon);

      model.addAttribute("isPublished", (count!=null && count > 0));
      model.addAttribute("template", schTempList);
      return "jsonTemplate";
  }

  // Get current schedule template (current shifts) dd-mm-yyyy
  @RequestMapping(value = "/schedule/shiftIndices")
    public String saveShifts(Model model,  @RequestBody String data, @ModelAttribute("scope") List<String> permissions) {
        // the token did not have the required permissions, return 403 status
        if (!permissions.contains("manage-schedule")) {
            model.addAttribute("status", HttpServletResponse.SC_FORBIDDEN);
            return "jsonTemplate";
        }
        JdbcTemplate jdbcTemplate = MainController.getJdbcTemplate();

        // Parse the list of params to array of Strings
        // reuse Scheduled class where sid = shift id and eid = index
        Gson gson = new Gson();
        Scheduled [] shiftChanges = gson.fromJson(data, Scheduled[].class);

          for (Scheduled item : shiftChanges) {
            try {
              jdbcTemplate.update("update sequ_shift set index = ? where id = ?",
                  item.getEid(), item.getSid());
            }
            catch(DataIntegrityViolationException e) {
              System.out.println(e);
              // do nothing
            }
          }
        return "jsonTemplate";
  }



  // Update current schedule template (current shifts) dd/mm/yyyy
    @RequestMapping(value = "/schedule/update")
    public String updateSchedule(@RequestBody String data, @ModelAttribute("scope") List<String> permissions, Model model) throws Exception {

        // the token did not have the required permissions, return 403 status
        if (!(permissions.contains("manage-schedule") || permissions.contains("admin"))) {
            model.addAttribute("status", HttpServletResponse.SC_FORBIDDEN);
            return "jsonTemplate";
        }

        JdbcTemplate jdbcTemplate = MainController.getJdbcTemplate();

        // Parse the list of params to array of Strings
        Gson gson = new Gson();
        Scheduled [] scheduleChanges = gson.fromJson(data, Scheduled[].class);

          // update database schedule(eid, sid, mon)
          for (Scheduled change : scheduleChanges) {
            try {
                jdbcTemplate.update("select sequ_schedule(?, ?, ?)",
                    change.getEid(),
                    change.getSid(),
                    change.getDate());
            }
            catch(DataIntegrityViolationException e) {
              // do nothing
            }
          }
        return "jsonTemplate";
    }

  // Delete scheduled day dd/mm/yyyy
    @RequestMapping(value = "/schedule/delete")
    public String deleteSchedule(@RequestBody String data, @ModelAttribute("scope") List<String> permissions,  Model model) throws SQLException {

        // the token did not have the required permissions, return 403 status
        if (!(permissions.contains("manage-schedule") || permissions.contains("admin"))) {
            model.addAttribute("status", HttpServletResponse.SC_FORBIDDEN);
            return "jsonTemplate";
        }

        JdbcTemplate jdbcTemplate = MainController.getJdbcTemplate();

        // Parse the list of params to array of Strings
        Gson gson = new Gson();
        Scheduled [] scheduleChanges = gson.fromJson(data, Scheduled[].class);

          // update database
          for (Scheduled change : scheduleChanges) {
            try {
              jdbcTemplate.update("select sequ_delete_schedule(?, ?)",
                  change.getSid(),
                  change.getDate());
            }
            catch(Exception e) {
              // do nothing
            }
          }
        return "jsonTemplate";
    }

    @RequestMapping(value = "/schedule/publish")
    public String publishSchedule(@RequestBody String data, @ModelAttribute("scope") List<String> permissions,  Model model) throws SQLException {

        // the token did not have the required permissions, return 403 status
        if (!(permissions.contains("manage-schedule") || permissions.contains("admin"))) {
            model.addAttribute("status", HttpServletResponse.SC_FORBIDDEN);
            return "jsonTemplate";
        }

        JdbcTemplate jdbcTemplate = MainController.getJdbcTemplate();

        // parse params
        JsonElement jelement = new JsonParser().parse(data);
        JsonObject  jobject = jelement.getAsJsonObject();
        final int eid = jobject.get("eid").getAsInt();
        final String date = jobject.get("date").getAsString();

        // update database publish(eid, datestring)
        //try {
        jdbcTemplate.execute("select sequ_publish(?, ?)" ,
          new PreparedStatementCallback<Boolean>(){
              @Override
              public Boolean doInPreparedStatement(PreparedStatement ps)
              throws SQLException, DataAccessException {
                ps.setInt(1, eid);
                ps.setString(2, date);
                return ps.execute();
              }
          });

        return "jsonTemplate";
    }

  // Check with database if is published or not
    @RequestMapping(value = "/schedule/ispublished/{date}")
    public String checkifPublished( @PathVariable("date") String mon, @ModelAttribute("scope") List<String> permissions,  Model model) throws SQLException {
        JdbcTemplate jdbcTemplate = MainController.getJdbcTemplate();

        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM sequ_published_schedule WHERE start_date = to_date(?,'dd-mm-yyyy')",Integer.class, mon);

        boolean isPublished =  (count != null && count > 0);

        model.addAttribute("result", isPublished);
        return "jsonTemplate";
    }
}

