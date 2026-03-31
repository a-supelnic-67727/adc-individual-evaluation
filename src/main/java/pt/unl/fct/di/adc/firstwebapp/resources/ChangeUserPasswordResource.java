package pt.unl.fct.di.adc.firstwebapp.resources;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.google.gson.Gson;
import com.google.cloud.datastore.*;

import pt.unl.fct.di.adc.firstwebapp.util.ErrorResponse;

@Path("/changeuserpwd")
public class ChangeUserPasswordResource {

  private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

  private final Gson g = new Gson();

  public ChangeUserPasswordResource() {
  }

  // Request format
  public static class RequestData {
    public InputData input;
    public TokenData token;
  }

  public static class InputData {
    public String username;
    public String oldPassword;
    public String newPassword;
  }

  public static class TokenData {
    public String tokenID;
  }

  @POST
  @Path("/")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response changeUserPassword(RequestData request) {
    // Validate input
    if (request == null || request.input == null || request.input.username == null
        || request.input.oldPassword == null || request.input.newPassword == null) {
      return Response.ok(buildErrorResponse(ErrorResponse.ErrorCodes.INVALID_INPUT)).build();
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

      // Check role
      if (!tokenEntity.getString("username").equals(request.input.username)) {
        return buildErrorResponse(ErrorResponse.ErrorCodes.UNAUTHORIZED);
      }

      // Get user
      Key userKey = datastore.newKeyFactory().setKind("User").newKey(request.input.username);
      Entity user = datastore.get(userKey);

      if (user == null) {
        return buildErrorResponse(ErrorResponse.ErrorCodes.USER_NOT_FOUND);
      }

      // Check old password
      String storedPwd = user.getString("user_pwd");
      String oldPwdHash = DigestUtils.sha512Hex(request.input.oldPassword);

      if (!storedPwd.equals(oldPwdHash)) {
        return buildErrorResponse(ErrorResponse.ErrorCodes.INVALID_CREDENTIALS);
      }

      // Update password using transaction
      Transaction txn = datastore.newTransaction();
      try {
        Entity updatedUser = Entity.newBuilder(user)
            .set("user_pwd", DigestUtils.sha512Hex(request.input.newPassword))
            .build();

        txn.put(updatedUser);
        txn.commit();

        return success();

      } catch (Exception e) {
        txn.rollback();
        throw e;
      }

    } catch (Exception e) {
      return buildErrorResponse(ErrorResponse.ErrorCodes.FORBIDDEN);
    }
  }

  private Response success() {
    Map<String, Object> data = new HashMap<>();
    data.put("message", "Password changed successfully");

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