package vn.iotstar.model;

public record LoginResponse(String token, long expiresIn, String tokenType) {
    public LoginResponse(String token, long expiresIn) {
        this(token, expiresIn, "Bearer");
    }
}
