package vn.iotstar.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.iotstar.entity.User;
import vn.iotstar.model.LoginResponse;
import vn.iotstar.model.LoginUserModel;
import vn.iotstar.model.RegisterUserModel;
import vn.iotstar.model.UserResponse;
import vn.iotstar.service.AuthenticationService;
import vn.iotstar.service.JwtService;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {
    private final JwtService jwtService;
    private final AuthenticationService authenticationService;

    public AuthenticationController(JwtService jwtService, AuthenticationService authenticationService) {
        this.jwtService = jwtService;
        this.authenticationService = authenticationService;
    }

    @PostMapping("/signup")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterUserModel input) {
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(authenticationService.signup(input)));
    }

    @PostMapping("/login")
    public LoginResponse authenticate(@Valid @RequestBody LoginUserModel input) {
        User user = authenticationService.authenticate(input);
        return new LoginResponse(jwtService.generateToken(user), jwtService.getExpirationTime());
    }
}
