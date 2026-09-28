package vn.iotstar.exception;

import org.springframework.security.core.AuthenticationException;

public class InvalidJwtException extends AuthenticationException {
    public InvalidJwtException(String message) { super(message); }
    public InvalidJwtException(String message, Throwable cause) { super(message, cause); }
}
