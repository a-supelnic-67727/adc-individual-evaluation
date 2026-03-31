package pt.unl.fct.di.adc.firstwebapp.util;

import java.util.HashMap;
import java.util.Map;

public class ErrorResponse {

  // Nested Error Codes
  public static class ErrorCodes {
    public static final String INVALID_CREDENTIALS = "9900";
    public static final String USER_ALREADY_EXISTS = "9901";
    public static final String USER_NOT_FOUND = "9902";
    public static final String INVALID_TOKEN = "9903";
    public static final String TOKEN_EXPIRED = "9904";
    public static final String UNAUTHORIZED = "9905";
    public static final String INVALID_INPUT = "9906";
    public static final String FORBIDDEN = "9907";
  }

  private static final Map<String, String> defaultMessages = new HashMap<>();

  static {
    defaultMessages.put(ErrorCodes.INVALID_CREDENTIALS,
        "The username-password pair is not valid");
    defaultMessages.put(ErrorCodes.USER_ALREADY_EXISTS,
        "Error in creating an account because the username already exists");
    defaultMessages.put(ErrorCodes.USER_NOT_FOUND,
        "The username referred in the operation doesn't exist in registered accounts");
    defaultMessages.put(ErrorCodes.INVALID_TOKEN,
        "The operation is called with an invalid token (wrong format for example)");
    defaultMessages.put(ErrorCodes.TOKEN_EXPIRED,
        "The operation is called with a token that is expired");
    defaultMessages.put(ErrorCodes.UNAUTHORIZED,
        "The operation is not allowed for the user role");
    defaultMessages.put(ErrorCodes.INVALID_INPUT,
        "The call is using input data not following the correct specification");
    defaultMessages.put(ErrorCodes.FORBIDDEN,
        "The operation generated a forbidden error by other reason");
  }

  private String status;
  private Object data;

  // Constructor using just the error code
  public ErrorResponse(String errorCode) {
    this.status = errorCode;
    this.data = defaultMessages.getOrDefault(errorCode, "Unknown error");
  }

  public String getStatus() {
    return status;
  }

  public Object getData() {
    return data;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public void setData(Object data) {
    this.data = data;
  }
}