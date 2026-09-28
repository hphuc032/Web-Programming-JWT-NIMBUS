package vn.iotstar.model;

import vn.iotstar.entity.User;

import java.time.Instant;

public record UserResponse(Integer id, String fullName, String email, String images, String role,
                           boolean enabled, Instant createdAt, Instant updatedAt) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getImages(),
                user.getRole(), user.isEnabled(), user.getCreatedAt(), user.getUpdatedAt());
    }
}
