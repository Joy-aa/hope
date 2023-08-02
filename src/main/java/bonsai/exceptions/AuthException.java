package bonsai.exceptions;

import javax.ws.rs.NotAuthorizedException;

/**
 * Created by mohan on 8/5/17.
 */
public class AuthException extends NotAuthorizedException {

    public AuthException() {
        super("error");
    }

    public AuthException(String msg) {
        super(msg);
    }
}
