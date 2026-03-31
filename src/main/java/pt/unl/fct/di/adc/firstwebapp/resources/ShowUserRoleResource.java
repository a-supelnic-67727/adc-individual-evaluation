package pt.unl.fct.di.adc.firstwebapp.resources;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

import com.google.cloud.datastore.*;

import pt.unl.fct.di.adc.firstwebapp.util.ErrorResponse;

@Path("/showuserrole")
public class ShowUserRoleResource {

  private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

  private final Gson g = new Gson();

  public ShowUserRoleResource() {
  }

  public static class RequestData {
    public InputData input;
    public TokenData token;
  }

  public static class InputData {
    public String username;
  }

  public static class TokenData {
    public String tokenID;
  }

  @POST
  @Path("/")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response showUserRole(RequestData request) {
    // Validate request
    if (request == null || request.input == null || request.input.username == null) {
      return buildErrorResponse(ErrorResponse.ErrorCodes.INVALID_INPUT);
    }

    try {
      // Validate token
      Key tokenKey = datastore.newKeyFactory().setKind("Token").newKey(request.token.tokenID);
      Entity tokenEntity = datastore.get(tokenKey);

      if (tokenEntity == null) {
        return buildErrorResponse(ErrorResponse.ErrorCodes.INVALID_TOKEN);
      }

      int now = (int) (System.currentTimeMillis() / 1000);
      int expiresAt = (int) tokenEntity.getLong("expiresAt");
      if (now > expiresAt) {
        return buildErrorResponse(ErrorResponse.ErrorCodes.TOKEN_EXPIRED);
      }

      // Check role (ONLY ADMIN or BOFFICER allowed)
      if (!(tokenEntity.getString("role").equals("ADMIN")
          || tokenEntity.getString("role").equals("BOFFICER"))) {
        return buildErrorResponse(ErrorResponse.ErrorCodes.UNAUTHORIZED);
      }

      // Get target user
      Key userKey = datastore.newKeyFactory().setKind("User").newKey(request.input.username);
      Entity user = datastore.get(userKey);

      if (user == null) {
        return buildErrorResponse(ErrorResponse.ErrorCodes.USER_NOT_FOUND);
      }

      String role = user.getString("user_role");

      return success(request.input.username, role);

    } catch (Exception e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity("Error retrieving user role.").build();
    }
  }

  private Response success(String username, String role) {
    Map<String, Object> data = new HashMap<>();
    data.put("username", username);
    data.put("role", role);

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