package pt.unl.fct.di.adc.firstwebapp.resources;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.apache.commons.codec.digest.DigestUtils;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pt.unl.fct.di.adc.firstwebapp.util.AuthToken;
import pt.unl.fct.di.adc.firstwebapp.util.ErrorResponse;
import pt.unl.fct.di.adc.firstwebapp.util.LoginData;

import com.google.cloud.datastore.*;
import com.google.gson.Gson;

@Path("/login")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class LoginResource {

	private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());
	private static final String LOG_MESSAGE_LOGIN_ATTEMP = "Login attempt by user: ";
	private static final String LOG_MESSAGE_LOGIN_SUCCESSFUL = "Login successful by user: ";
	private static final String LOG_MESSAGE_WRONG_PASSWORD = "Wrong password for: ";
	private static final String LOG_MESSAGE_UNKNOW_USER = "Failed login attempt for username: ";

	private static final String USER_PWD = "user_pwd";
	private static final String USER_ROLE = "user_role";

	private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
	private static final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");
	private static final KeyFactory tokenKeyFactory = datastore.newKeyFactory().setKind("Token");

	private final Gson g = new Gson();

	public LoginResource() {
	}

	private static class InputData {
		public LoginData input;
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public Response doLogin(InputData inputData) {
		LoginData data = inputData.input;
		LOG.info(LOG_MESSAGE_LOGIN_ATTEMP + data.username);

		Key userKey = userKeyFactory.newKey(data.username);
		Entity user = datastore.get(userKey);

		if (user == null) {
			LOG.warning(LOG_MESSAGE_UNKNOW_USER + data.username);
			return buildErrorResponse(ErrorResponse.ErrorCodes.USER_NOT_FOUND);
		}

		String storedPassword = user.getString(USER_PWD);
		if (!storedPassword.equals(DigestUtils.sha512Hex(data.password))) {
			LOG.warning(LOG_MESSAGE_WRONG_PASSWORD + data.username);
			return buildErrorResponse(ErrorResponse.ErrorCodes.INVALID_CREDENTIALS);
		}

		String role = user.getString(USER_ROLE);
		AuthToken token = new AuthToken(data.username);
		token.role = role;

		// Store token in Datastore
		Key tokenKey = tokenKeyFactory.newKey(token.tokenID);
		Entity tokenEntity = Entity.newBuilder(tokenKey)
				.set("username", token.username)
				.set("role", token.role)
				.set("issuedAt", token.issuedAt)
				.set("expiresAt", token.expiresAt)
				.build();
		datastore.put(tokenEntity);

		LOG.info(LOG_MESSAGE_LOGIN_SUCCESSFUL + data.username);

		return success(token);
	}

	private Response success(AuthToken tkn) {
		Map<String, Object> data = new HashMap<>();
		data.put("tokenId", tkn.tokenID);
		data.put("username", tkn.username);
		data.put("role", tkn.role);
		data.put("issuedAt", tkn.issuedAt);
		data.put("expiresAt", tkn.expiresAt);

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