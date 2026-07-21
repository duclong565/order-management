package com.example.order_management.config;

import com.example.order_management.entity.User;
import com.example.order_management.entity.UserRole;
import com.example.order_management.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;

@Configuration
@AllArgsConstructor
class DataSeeder {

    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner seedUser(UserRepository userRepository) {
        return args -> {
            if (userRepository.count() == 0) {
                User u = new User();
                u.setUsername("admin");
                u.setPassword(passwordEncoder.encode("admin"));
                u.setEmail("admin@example.com");
                u.setCreatedAt(Instant.now());
                u.setRole(UserRole.ADMIN);

                User savedUser = userRepository.save(u);
                System.out.println("Seeded user: " + savedUser.getUsername() + " with role: " + savedUser.getRole());
                System.out.println("Created at = " + savedUser.getCreatedAt());
            }
        };
    }
}