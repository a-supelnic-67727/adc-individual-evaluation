package pt.unl.fct.di.adc.firstwebapp.resources;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.codec.digest.DigestUtils;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import com.google.gson.Gson;
import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Transaction;
import com.google.cloud.datastore.DatastoreOptions;

import pt.unl.fct.di.adc.firstwebapp.util.ErrorResponse;
import pt.unl.fct.di.adc.firstwebapp.util.RegisterData;

@Path("/createaccount")
public class RegisterResource {

	private static final Logger LOG = Logger.getLogger(RegisterResource.class.getName());
	private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

	private final Gson g = new Gson();

	public RegisterResource() {
	} // Default constructor, nothing to do

	public static class InputData {
		public RegisterData input;
	}

	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response registerUser(InputData inputData) {
		RegisterData data = inputData.input;
		LOG.fine("Attempt to register user: " + data.username);

		if (!data.validRegistration())
			return buildErrorResponse(ErrorResponse.ErrorCodes.INVALID_INPUT);

		try {
			Transaction txn = datastore.newTransaction();
			Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
			Entity user = txn.get(userKey);

			if (user != null) {
				txn.rollback();
				return Response.status(Status.CONFLICT).entity("User already exists.").build();
			} else {
				user = Entity.newBuilder(userKey)
						.set("user_name", data.username)
						.set("user_pwd", DigestUtils.sha512Hex(data.password))
						.set("user_phone", data.phone)
						.set("user_address", data.address)
						.set("user_role", data.role)
						.set("user_creation_time", Timestamp.now())
						.build();
				txn.put(user);
				txn.commit();
				LOG.info("User registered " + data.username);
				return success(data.username, data.role);
			}
		} catch (Exception e) {
			LOG.severe("Error registering user: " + e.getMessage());
			return buildErrorResponse(ErrorResponse.ErrorCodes.USER_ALREADY_EXISTS);
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