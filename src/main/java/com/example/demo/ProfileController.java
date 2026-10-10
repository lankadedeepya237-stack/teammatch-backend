package com.example.demo;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Optional;

@RestController
@RequestMapping("/api/profile")
@CrossOrigin(origins = "http://localhost:3000")
public class ProfileController {
    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;

    public ProfileController(ProfileRepository profileRepository, UserRepository userRepository) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getProfile(@PathVariable int userId) {
        return profileRepository.findByUserId(userId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("Profile not created yet"));
    }

    @PostMapping
    public ResponseEntity<?> saveProfile(@RequestBody Profile profile) {
        if (!userRepository.existsById(profile.getUserId())) {
            return ResponseEntity.badRequest().body("User does not exist");
        }
        Optional<Profile> existing = profileRepository.findByUserId(profile.getUserId());
        if (existing.isPresent()) {
            Profile p = existing.get();
            p.setSkills(profile.getSkills());
            p.setInterests(profile.getInterests());
            p.setBio(profile.getBio());
            p.setAvailability(profile.getAvailability());
            return ResponseEntity.ok(profileRepository.save(p));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(profileRepository.save(profile));
    }
}
