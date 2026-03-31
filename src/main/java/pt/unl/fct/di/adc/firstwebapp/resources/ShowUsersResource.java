package pt.unl.fct.di.adc.firstwebapp.resources;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.google.cloud.datastore.*;

import com.google.gson.Gson;

import pt.unl.fct.di.adc.firstwebapp.util.ErrorResponse;
import pt.unl.fct.di.adc.firstwebapp.util.UserData;

@Path("/showusers")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class ShowUsersResource {
  private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

  private final Gson g = new Gson();

  public ShowUsersResource() {
  }

  public static class RequestData {
    public InputData input;
    public TokenData token;
  }

  public static class InputData {
  }

  public static class TokenData {
    public String tokenID;
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  public Response showUsers(RequestData requestData) {

    try {
      // Validate token
      Key tokenKey = datastore.newKeyFactory().setKind("Token").newKey(requestData.token.tokenID);
      Entity tokenEntity = datastore.get(tokenKey);

      if (tokenEntity == null) {
        return buildErrorResponse(ErrorResponse.ErrorCodes.INVALID_TOKEN);
      }

      int now = (int) (System.currentTimeMillis() / 1000);
      int expiresAt = (int) tokenEntity.getLong("expiresAt");
      if (now > expiresAt) {
        return buildErrorResponse(ErrorResponse.ErrorCodes.TOKEN_EXPIRED);
      }

      if (tokenEntity.getString("role").equals("USER")) {
        return buildErrorResponse(ErrorResponse.ErrorCodes.UNAUTHORIZED);
      }

      // Query all users
      Query<Entity> query = Query.newEntityQueryBuilder()
          .setKind("User")
          .build();

      QueryResults<Entity> results = datastore.run(query);

      List<UserData> users = new ArrayList<>();

      while (results.hasNext()) {
        Entity user = results.next();

        String username = user.getKey().getName();
        String role = user.getString("user_role");

        users.add(new UserData(username, role));
      }

      // Return result
      return success(users);

    } catch (Exception e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity("Error retrieving users.").build();
    }
  }

  private Response success(List<UserData> users) {
    List<Object> userList = new ArrayList<>();
    for (UserData u : users) {
      Map<String, Object> userMap = new HashMap<>();
      userMap.put("username", u.username);
      userMap.put("role", u.role);
      userList.add(userMap);
    }

    Map<String, Object> data = new HashMap<>();
    data.put("users", userList);

    Map<String, Object> response = new HashMap<>();
    response.put("status", "success");
    response.put("data", data);

    return Response.ok(g.toJson(response)).build();
  }

  private Response buildErrorResponse(String errorCode) {
    ErrorResponse response = new ErrorResponse(errorCode);
    return Response.ok(g.toJson(response)).build();
  }
}