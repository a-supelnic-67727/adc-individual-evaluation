package pt.unl.fct.di.adc.firstwebapp.resources;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.cloud.datastore.*;

import pt.unl.fct.di.adc.firstwebapp.util.ErrorResponse;

@Path("/logout")
public class LogoutResource {

  private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

  private final Gson g = new Gson();

  public LogoutResource() {
  }

  // Request format
  private static class RequestData {
    public InputData input;
    public TokenData token;
  }

  private static class InputData {
    public String username;
  }

  private static class TokenData {
    public String tokenID;
  }

  @POST
  @Path("/")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response logout(RequestData request) {

    // Validate input
    if (request == null || request.input == null || request.input.username == null) {
      return buildErrorResponse(ErrorResponse.ErrorCodes.INVALID_INPUT);
    }

    try {
      // Validate token (continua a ser necessário para permissões)
      if (request.token == null || request.token.tokenID == null) {
        return buildErrorResponse(ErrorResponse.ErrorCodes.INVALID_TOKEN);
      }

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

      String requesterRole = tokenEntity.getString("role");
      String requesterUsername = tokenEntity.getString("username");

      // Authorization
      if (!requesterRole.equals("ADMIN") &&
          !requesterUsername.equals(request.input.username)) {
        return buildErrorResponse(ErrorResponse.ErrorCodes.UNAUTHORIZED);
      }

      Query<Entity> query = Query.newEntityQueryBuilder()
          .setKind("Token")
          .setFilter(StructuredQuery.PropertyFilter.eq("username", request.input.username))
          .build();

      QueryResults<Entity> results = datastore.run(query);

      List<Key> keysToDelete = new ArrayList<>();

      while (results.hasNext()) {
        Entity token = results.next();
        keysToDelete.add(token.getKey());
      }

      if (keysToDelete.isEmpty()) {
        return buildErrorResponse(ErrorResponse.ErrorCodes.INVALID_TOKEN);
      }

      datastore.delete(keysToDelete.toArray(new Key[0]));

      return success();

    } catch (Exception e) {
      return buildErrorResponse(ErrorResponse.ErrorCodes.FORBIDDEN);
    }
  }

  private Response success() {
    Map<String, Object> data = new HashMap<>();
    data.put("message", "Logout successful");

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