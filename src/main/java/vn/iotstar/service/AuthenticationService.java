package vn.iotstar.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.entity.User;
import vn.iotstar.exception.EmailAlreadyExistsException;
import vn.iotstar.model.LoginUserModel;
import vn.iotstar.model.RegisterUserModel;
import vn.iotstar.repository.UserRepository;

@Service
public class AuthenticationService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public AuthenticationService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                                 AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public User signup(RegisterUserModel input) {
        String email = input.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) throw new EmailAlreadyExistsException();
        User user = new User();
        user.setFullName(input.fullName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(input.password()));
        user.setRole("USER");
        user.setEnabled(true);
        return userRepository.save(user);
    }

    public User authenticate(LoginUserModel input) {
        String email = input.email().trim().toLowerCase();
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, input.password()));
        return userRepository.findByEmail(email).orElseThrow();
    }
}
