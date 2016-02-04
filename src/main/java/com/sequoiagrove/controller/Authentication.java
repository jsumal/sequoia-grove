
package com.sequoiagrove.controller;

import com.google.gson.*;

import info.modprobe.browserid.BrowserIDResponse;
import info.modprobe.browserid.BrowserIDResponse.Status;
import info.modprobe.browserid.Verifier;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiagrove.model.Delivery;
import com.sequoiagrove.model.User;
import com.sequoiagrove.model.UserRowMapper;
import com.sequoiagrove.dao.DeliveryDAO;
import com.sequoiagrove.controller.MainController;

@Controller
public class Authentication {
  private final String USER_AGENT = "Mozilla/5.0";

    // Verify mozilla persona token received
    @RequestMapping(value = "/auth/login/", method = RequestMethod.POST)
    protected String login(Model model, @RequestBody String postLoad) throws ServletException, IOException, SQLException {
        JdbcTemplate jdbcTemplate = MainController.getJdbcTemplate();
        User user;

        JsonElement jelement = new JsonParser().parse(postLoad);
        JsonObject  jobject = jelement.getAsJsonObject();
        final String assertion = jobject.get("assertion").getAsString();
        final String audience = "http://localhost:8080";
        final Verifier verifier = new Verifier();
        final BrowserIDResponse personaResponse = verifier.verify(assertion,audience);
        final Status status = personaResponse.getStatus();

        if (status == Status.OK) {
          // Authentication with Persona was successful
          String email = personaResponse.getEmail();

          // find this user in database
          String sql = "select * from bajs_employee where email = ?"; 
          try {
          user = (User)jdbcTemplate.queryForObject(
                    sql, new Object[] { email }, new UserRowMapper());
          } catch (EmptyResultDataAccessException e) {

            // this user does not exist in the database
            model.addAttribute("UserNotRegistered", true);
            model.addAttribute("email", email);
            return "jsonTemplate";
          }

          // found the user in the database
          if(user != null) {
            System.out.println(user.getFullname() + " has sucessfully signed in");
            model.addAttribute("user", user);
          }

        } 
        // Authentication with Persona failed
        else {
          System.out.println("Sign in failed...");
        }
        return "jsonTemplate";
    }

    // Find Employee in database, and get their fullname
    /*
    private User getUser(String email) throws SQLException {
        JdbcTemplate jdbcTemplate = MainController.getJdbcTemplate();
        String sql = "select * from bajs_employee where email = ?"; 
        User user;
        try {
        user = (User)jdbcTemplate.queryForObject(
                  sql, new Object[] { email }, new UserRowMapper());
        } catch (EmptyResultDataAccessException e) {
        }

        return user;
    }
    */


}









