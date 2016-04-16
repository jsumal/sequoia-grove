package com.sequoiagrove.controller;

import com.google.gson.*;
import info.modprobe.browserid.BrowserIDResponse.Status;
import info.modprobe.browserid.BrowserIDResponse;
import info.modprobe.browserid.Verifier;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.MissingClaimException;
import io.jsonwebtoken.IncorrectClaimException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.crypto.MacProvider;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.Key;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sequoiagrove.model.User;
import com.sequoiagrove.model.UserRowMapper;
import com.sequoiagrove.controller.MainController;

@Controller
public class Authentication {
  private final String USER_AGENT = "Mozilla/5.0";
  private static Key key = MacProvider.generateKey();

    // Verify mozilla persona token received
    @RequestMapping(value = "/auth/login/", method = RequestMethod.POST)
    protected String login(Model model, @RequestBody String postLoad) throws ServletException, IOException, SQLException {
        JdbcTemplate jdbcTemplate = MainController.getJdbcTemplate();
        User user = new User(0, "", "", "", "", false);

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
          String sql = "select * from employee where email = ?";
          try {
          user = (User)jdbcTemplate.queryForObject(
                    sql, new Object[] { email }, new UserRowMapper());
          } catch (EmptyResultDataAccessException e) {

            // this user does not exist in the database
            model.addAttribute("userNotRegistered", true);
            model.addAttribute("email", email);
            return "jsonTemplate";
          }

          // found the user in the database
          if(user != null) {
            Object[] params = new Object[] { user.getId() };

            // make sure this is a current employee
            int count = jdbcTemplate.queryForObject(
                "select count(*) from employment_history " +
                " where employee_id = ? and date_unemployed is null",
                params, Integer.class);

            // Success! This employee is currently employed
            if (count > 0) {
                System.out.println(user.getFullname() + " has sucessfully signed in");
                model.addAttribute("user", user);
                model.addAttribute("auth_token",
                    getToken(user.getEmail(),
                      user.getIsManager()? "manager":"employee"));
            }
            else {
                model.addAttribute("userNotCurrent", true);
                model.addAttribute("email", email);
            }

          }
        }
        // Authentication with Persona failed
        else {
          model.addAttribute("user", user);
          model.addAttribute("email", "");
          model.addAttribute("loginFailed", true);
          System.out.println("Sign in failed...");
        }
        return "jsonTemplate";
    }

    // Create initial token upon authorization
    protected static String getToken(String subject, String scope) {
      //byte[] key = getSignatureKey();
      // We need a signing key, so we'll create one just for this example. Usually
      // the key would be read from your application configuration instead.

      String jwt =
        Jwts.builder().setIssuer("localhost:8080/sequoiagrove/")
        //.setSubject(subject)
        // for now hard code subject, later, change to be related to
        // this user.
        .setSubject("token_subject")
        //.setExpiration(expirationDate)
        //.put("scope", scope)
        .signWith(SignatureAlgorithm.HS256,key)
        .compact();

      return jwt;
    }

    public static String verifyToken(String jwt, String URI) {
      String subject = "HACKER";
        try {
            // Parse jwt
            Jws<Claims> jwtClaims =
              Jwts.parser().setSigningKey(key).parseClaimsJws(jwt);
              subject = jwtClaims.getBody().getSubject();
            try {
                // Check jwt subject
                // for now, use hard coded subject, later, check datbase for it
                Jwts.parser().requireSubject("token_subject").setSigningKey(key).parseClaimsJws(jwt);
            } catch (MissingClaimException e) {
                // the parsed JWT did not have the subject field
                System.out.println("Missing Claim Exception for " + URI
                    + "\n\t-> No subject field present.");
                return("invalid");
            } catch (IncorrectClaimException e) {
                // the parsed JWT had a sub field, but its value was not equal to 'subject'
                System.out.println("Incorrect Claim Exception for " + URI
                    + "\n\t-> Subject field was not what was expected.");
                return("invalid");
            }
            //we can trust this JWT, get the subject
            //System.out.println(subject);
        } catch (SignatureException e) {
            //don't trust the JWT!
            System.out.println("Signature Exception: " + URI
                + "\n\t-> cannot trust this token.");
            return("invalid");
        } catch (MalformedJwtException e) {
            // Jwt was not formed correctly (needs 2 dots etc.)
            System.out.println("Malformed Jwt: " + URI
                + "\n\t-> needs two dots etc.");
            return("invalid");
        } catch (IllegalArgumentException e) {
            // Jwt String cannot be null or empty
            System.out.println("Illegal Argument (Jwt): " + URI
                + "\n\t-> possibly null or empty Jwt.");
            return("invalid");
        }
       return(subject);
    }

}
