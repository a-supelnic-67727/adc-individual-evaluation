package pt.unl.fct.di.adc.firstwebapp.resources;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.google.gson.Gson;
import com.google.cloud.datastore.*;

import pt.unl.fct.di.adc.firstwebapp.util.ErrorResponse;

@Path("/showauthsessions")
public class ShowAuthenticatedSessionsResource {

  private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

  private final Gson g = new Gson();

  public ShowAuthenticatedSessionsResource() {
  }

  private static class RequestData {
    public InputData input;
    public TokenData token;
  }

  private static class InputData {
  }

  private static class TokenData {
    public String tokenID;
  }

  @POST
  @Path("/")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response showSessions(RequestData requestData) {
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

      // Check role (ONLY ADMIN allowed)
      if (!tokenEntity.getString("role").equals("ADMIN")) {
        return buildErrorResponse(ErrorResponse.ErrorCodes.UNAUTHORIZED);
      }

      // Query all tokens (sessions)
      Query<Entity> query = Query.newEntityQueryBuilder()
          .setKind("Token")
          .build();

      QueryResults<Entity> results = datastore.run(query);

      return success(resultsToList(results));

    } catch (Exception e) {
      return buildErrorResponse(ErrorResponse.ErrorCodes.FORBIDDEN);
    }
  }

  private List<Entity> resultsToList(QueryResults<Entity> results) {
    List<Entity> list = new ArrayList<>();
    results.forEachRemaining(list::add);
    return list;
  }

  private Response success(List<Entity> sessionEntities) {
    List<Map<String, Object>> sessions = new ArrayList<>();

    for (Entity session : sessionEntities) {
      Map<String, Object> sessionMap = new HashMap<>();
      sessionMap.put("tokenID", session.getKey().getName());
      sessionMap.put("username", session.getString("username"));
      sessionMap.put("role", session.getString("role"));
      sessionMap.put("expiresAt", (int) session.getLong("expiresAt"));
      sessions.add(sessionMap);
    }

    Map<String, Object> data = new HashMap<>();
    data.put("sessions", sessions);

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