package com.aicodementor.controller;

import com.aicodementor.entity.User;
import com.aicodementor.repository.UserRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class UserController {
    
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    
    @Autowired
    private UserRepository userRepository;
    
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userRepository.findAll();
        return ResponseEntity.ok(users);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        Optional<User> userOpt = userRepository.findById(id);
        
        // If user doesn't exist, create a default student user
        if (userOpt.isEmpty()) {
            logger.warn("User not found with ID: {}, creating default student user", id);
            
            // Generate unique username and email
            String baseUsername = "student_" + id;
            String baseEmail = "student" + id + "@demo.com";
            
            // Check if username or email already exists, if so, find or create with different suffix
            String username = baseUsername;
            String email = baseEmail;
            int suffix = 0;
            
            while (userRepository.existsByUsername(username) || userRepository.existsByEmail(email)) {
                suffix++;
                username = baseUsername + "_" + suffix;
                email = "student" + id + "_" + suffix + "@demo.com";
            }
            
            User defaultUser = new User();
            defaultUser.setUsername(username);
            defaultUser.setEmail(email);
            defaultUser.setPassword("demo123");
            defaultUser.setFullName("Student " + id + (suffix > 0 ? " (" + suffix + ")" : ""));
            defaultUser.setRole(User.UserRole.STUDENT);
            defaultUser.setCreatedAt(LocalDateTime.now());
            defaultUser.setUpdatedAt(LocalDateTime.now());
            
            try {
                User savedUser = userRepository.save(defaultUser);
                logger.info("Created default student user with ID: {}, username: {}", savedUser.getId(), username);
                return ResponseEntity.ok(savedUser);
            } catch (DataIntegrityViolationException e) {
                logger.error("Data integrity violation when creating default student user: {}", e.getMessage(), e);
                // Try to find existing user by username as fallback
                Optional<User> existingUser = userRepository.findByUsername(baseUsername);
                if (existingUser.isPresent()) {
                    logger.info("Using existing user with ID: {}, username: {}", existingUser.get().getId(), existingUser.get().getUsername());
                    return ResponseEntity.ok(existingUser.get());
                } else {
                    throw new RuntimeException("Failed to create user: " + e.getMessage(), e);
                }
            }
        }
        
        return ResponseEntity.ok(userOpt.get());
    }
    
    @PostMapping
    public ResponseEntity<User> createUser(@Valid @RequestBody User user) {
        try {
            // Check if username or email already exists
            if (userRepository.existsByUsername(user.getUsername())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
            if (userRepository.existsByEmail(user.getEmail())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
            
            User savedUser = userRepository.save(user);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
        } catch (DataIntegrityViolationException e) {
            logger.error("Data integrity violation when creating user: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (jakarta.validation.ConstraintViolationException e) {
            logger.error("Validation constraint violation when creating user: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @Valid @RequestBody User userDetails) {
        Optional<User> optionalUser = userRepository.findById(id);
        
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            user.setUsername(userDetails.getUsername());
            user.setEmail(userDetails.getEmail());
            user.setFullName(userDetails.getFullName());
            user.setRole(userDetails.getRole());
            
            User updatedUser = userRepository.save(user);
            return ResponseEntity.ok(updatedUser);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/username/{username}")
    public ResponseEntity<User> getUserByUsername(@PathVariable String username) {
        Optional<User> user = userRepository.findByUsername(username);
        return user.map(ResponseEntity::ok)
                  .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/email/{email}")
    public ResponseEntity<User> getUserByEmail(@PathVariable String email) {
        Optional<User> user = userRepository.findByEmail(email);
        return user.map(ResponseEntity::ok)
                  .orElse(ResponseEntity.notFound().build());
    }
}









