package pt.unl.fct.di.adc.firstwebapp.resources;

import java.util.HashMap;
import java.util.Map;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.google.gson.Gson;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Transaction;

import pt.unl.fct.di.adc.firstwebapp.util.ErrorResponse;

@Path("/modaccount")
public class ModifyAccountResource {

  private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

  private final Gson g = new Gson();

  public ModifyAccountResource() {
  }

  public static class ModifyAccountData {
    public Input input;
    public TokenData token;

    public boolean validModification() {
      return input.attributes != null && !input.attributes.isEmpty();
    }
  }

  public static class TokenData {
    public String tokenID;
  }

  public static class Input {
    public String username;
    public Map<String, String> attributes; // phone, address
  }

  @POST
  @Path("/")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response modifyAccount(ModifyAccountData data) {
    if (data.input.username == null || !data.validModification()) {
      return buildErrorResponse(ErrorResponse.ErrorCodes.INVALID_INPUT);
    }

    try {
      // Validate token
      Key tokenKey = datastore.newKeyFactory().setKind("Token").newKey(data.token.tokenID);
      Entity tokenEntity = datastore.get(tokenKey);

      if (tokenEntity == null) {
        return buildErrorResponse(ErrorResponse.ErrorCodes.INVALID_TOKEN);
      }

      int now = (int) (System.currentTimeMillis() / 1000);
      int expiresAt = (int) tokenEntity.getLong("expiresAt");
      if (now > expiresAt) {
        return buildErrorResponse(ErrorResponse.ErrorCodes.TOKEN_EXPIRED);
      }

      if (tokenEntity.getString("role").equals("USER")
          && !tokenEntity.getString("username").equals(data.input.username)) {
        return buildErrorResponse(ErrorResponse.ErrorCodes.UNAUTHORIZED);
      } else if ("BOFFICER".equals(tokenEntity.getString("role"))) {

        Transaction txn = datastore.newTransaction();
        Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.input.username);
        Entity targetUser = txn.get(userKey);

        if (targetUser == null) {
          txn.rollback();
          return buildErrorResponse(ErrorResponse.ErrorCodes.USER_NOT_FOUND);
        }

        String targetRole = targetUser.getString("user_role");

        // BOFFICER can only modify USER accounts
        if (!targetRole.equals("USER") || !tokenEntity.getString("username").equals(data.input.username)) {
          txn.rollback();
          return buildErrorResponse(ErrorResponse.ErrorCodes.UNAUTHORIZED);
        }

        txn.rollback(); // only checking, not modifying here
      }
      Transaction txn = datastore.newTransaction();
      Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.input.username);
      Entity user = txn.get(userKey);

      if (user == null) {
        txn.rollback();
        return buildErrorResponse(ErrorResponse.ErrorCodes.USER_NOT_FOUND);
      }

      Entity.Builder builder = Entity.newBuilder(user);
      if (data.input.attributes.containsKey("phone")) {
        builder.set("user_phone", data.input.attributes.get("phone"));
      }
      if (data.input.attributes.containsKey("address")) {
        builder.set("user_address", data.input.attributes.get("address"));
      }

      txn.put(builder.build());
      txn.commit();
      return success("Updated successfully");
    } catch (Exception e) {
      return buildErrorResponse(ErrorResponse.ErrorCodes.FORBIDDEN);
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