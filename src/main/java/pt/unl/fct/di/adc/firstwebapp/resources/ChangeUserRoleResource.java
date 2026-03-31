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

@Path("/changeuserrole")
public class ChangeUserRoleResource {

  private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

  private final Gson g = new Gson();

  public ChangeUserRoleResource() {
  }

  // Request format
  public static class RequestData {
    public InputData input;
    public TokenData token;
  }

  public static class InputData {
    public String username;
    public String newRole;
  }

  public static class TokenData {
    public String tokenID;
  }

  @POST
  @Path("/")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response changeUserRole(RequestData request) {
    // Validate input
    if (request == null || request.input == null || request.input.username == null
        || request.input.newRole == null) {
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

      // Check role (ONLY ADMIN allowed)
      if (!tokenEntity.getString("role").equals("ADMIN")) {
        return buildErrorResponse(ErrorResponse.ErrorCodes.UNAUTHORIZED);
      }

      // Get user
      Key userKey = datastore.newKeyFactory().setKind("User").newKey(request.input.username);
      Entity user = datastore.get(userKey);

      if (user == null) {
        return buildErrorResponse(ErrorResponse.ErrorCodes.USER_NOT_FOUND);
      }

      // Update role using transaction
      Transaction txn = datastore.newTransaction();
      try {
        Entity updatedUser = Entity.newBuilder(user)
            .set("user_role", request.input.newRole)
            .build();

        txn.put(updatedUser);
        txn.commit();

        return success();

      } catch (Exception e) {
        txn.rollback();
        throw e;
      }

    } catch (Exception e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity("Error changing user role.").build();
    }
  }

  private Response success() {
    Map<String, Object> data = new HashMap<>();
    data.put("message", "Role updated successfully");

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