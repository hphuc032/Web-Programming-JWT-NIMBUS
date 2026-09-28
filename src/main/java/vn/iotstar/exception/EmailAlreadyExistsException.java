package vn.iotstar.exception;

public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException() { super("Email đã được sử dụng"); }
}
