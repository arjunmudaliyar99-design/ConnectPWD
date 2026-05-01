package org.connectpwd.config;

import lombok.RequiredArgsConstructor;
import org.connectpwd.user.User;
import org.connectpwd.user.UserRepository;
import org.connectpwd.user.UserRole;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class MongoDbSeeder {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    @SuppressWarnings("null")
    CommandLineRunner seedAdmin() {
        return args -> {
            if (!userRepository.existsByEmail("admin@connectpwd.org")) {
                userRepository.save(User.builder()
                        .fullName("ConnectPWD Admin")
                        .email("admin@connectpwd.org")
                        .passwordHash(passwordEncoder.encode("Admin@ConnectPWD1"))
                        .role(UserRole.ADMIN)
                        .language("en")
                        .isActive(true)
                        .build());
            }
        };
    }
}
