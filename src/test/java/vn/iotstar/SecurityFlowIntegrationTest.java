package vn.iotstar;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import vn.iotstar.entity.User;
import vn.iotstar.repository.UserRepository;
import vn.iotstar.service.JwtService;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityFlowIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void clearDatabase() { userRepository.deleteAll(); }

    @Test
    void completeSignupLoginAndProtectedApiFlow() throws Exception {
        String signup = """
                {"email":"phuc@qycandle.vn","password":"Password123!","fullName":"Nguyen Hoang Phuc"}
                """;
        mockMvc.perform(post("/auth/signup").contentType(MediaType.APPLICATION_JSON).content(signup))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("phuc@qycandle.vn"))
                .andExpect(jsonPath("$.password").doesNotExist());

        User stored = userRepository.findByEmail("phuc@qycandle.vn").orElseThrow();
        assertThat(stored.getPassword()).isNotEqualTo("Password123!");
        assertThat(passwordEncoder.matches("Password123!", stored.getPassword())).isTrue();

        String loginBody = """
                {"email":"phuc@qycandle.vn","password":"Password123!"}
                """;
        String loginJson = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiresIn").value(3_600_000))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(loginJson).get("token").asText();

        mockMvc.perform(get("/users/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("phuc@qycandle.vn"))
                .andExpect(jsonPath("$.password").doesNotExist());
        mockMvc.perform(get("/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].email").value("phuc@qycandle.vn"));

        char last = token.charAt(token.length() - 1);
        String modified = token.substring(0, token.length() - 1) + (last == 'a' ? 'b' : 'a');
        mockMvc.perform(get("/users/me").header("Authorization", "Bearer " + modified))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongPasswordAndExpiredTokenReturn401() throws Exception {
        User user = new User();
        user.setFullName("Nguyen Hoang Phuc");
        user.setEmail("phuc@qycandle.vn");
        user.setPassword(passwordEncoder.encode("Password123!"));
        user.setRole("USER");
        user.setEnabled(true);
        user = userRepository.save(user);

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"phuc@qycandle.vn\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized());

        String testSecret = Base64.getEncoder().encodeToString(
                "This-is-a-test-secret-key-that-is-at-least-32-bytes-long".getBytes());
        String expired = new JwtService(testSecret, -1_000).generateToken(user);
        mockMvc.perform(get("/users/me").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized());
    }
}
