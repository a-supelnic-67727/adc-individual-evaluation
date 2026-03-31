package pt.unl.fct.di.adc.firstwebapp.resources;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.HashMap;
import java.util.Map;

import com.google.cloud.datastore.*;
import com.google.gson.Gson;

import pt.unl.fct.di.adc.firstwebapp.util.ErrorResponse;

@Path("/deleteaccount")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class DeleteAccountResource {

  private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

  private final Gson g = new Gson();

  public DeleteAccountResource() {
  }

  public static class DeleteRequest {
    public Input input;
    public TokenData token;
  }

  public static class Input {
    public String username;
  }

  public static class TokenData {
    public String tokenID;
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  public Response deleteAccount(DeleteRequest request) {

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

      // Check role (only ADMIN allowed)
      if (!tokenEntity.getString("role").equals("ADMIN")) {
        return buildErrorResponse(ErrorResponse.ErrorCodes.UNAUTHORIZED);
      }

      KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");
      String usernameToDelete = request.input.username;
      Key userKey = userKeyFactory.newKey(usernameToDelete);
      Entity user = datastore.get(userKey);

      if (user == null) {
        return buildErrorResponse(ErrorResponse.ErrorCodes.USER_NOT_FOUND);
      }

      // Delete user
      datastore.delete(userKey);

      return success("Account deleted successfully");

    } catch (Exception e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity("Error deleting account.").build();
    }
  }

  private Response success(String message) {
    Map<String, Object> data = new HashMap<>();
    data.put("message", message);

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