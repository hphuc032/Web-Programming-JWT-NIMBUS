package vn.iotstar.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.iotstar.entity.User;
import vn.iotstar.model.UserResponse;
import vn.iotstar.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;
    public UserController(UserService userService) { this.userService = userService; }

    @GetMapping("/me")
    public UserResponse authenticatedUser(@AuthenticationPrincipal User currentUser) {
        return UserResponse.from(currentUser);
    }

    @GetMapping
    public List<UserResponse> allUsers() {
        return userService.allUsers().stream().map(UserResponse::from).toList();
    }
}
