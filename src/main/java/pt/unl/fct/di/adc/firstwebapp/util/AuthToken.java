package pt.unl.fct.di.adc.firstwebapp.util;

import java.util.UUID;

public class AuthToken {

	public static final long EXPIRATION_TIME = 1000 * 60 * 15; // 15 minutes

	public String tokenID;
	public String username;
	public String role;

	public int issuedAt;
	public int expiresAt;

	public AuthToken() {
	}

	public AuthToken(String username) {
		this.username = username;
		this.tokenID = UUID.randomUUID().toString();
		this.issuedAt = (int) (System.currentTimeMillis() / 1000);
		this.expiresAt = this.issuedAt + (int) (EXPIRATION_TIME / 1000);
	}

}
