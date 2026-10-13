package com.example.demo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {
    "http://localhost:3000",
    "https://teammatch-frontend-92wv.vercel.app"
})
public class UserController {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final TeamRequestRepository requestRepository;
    private final ProjectRepository projectRepository;

    public UserController(UserRepository userRepository, ProfileRepository profileRepository,
                          TeamRequestRepository requestRepository, ProjectRepository projectRepository) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.requestRepository = requestRepository;
        this.projectRepository = projectRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Users user) {
        if (user.getName() == null || user.getName().isBlank() ||
            user.getEmail() == null || user.getEmail().isBlank() ||
            user.getPassword() == null || user.getPassword().isBlank() ||
            user.getStudentId() == null || user.getStudentId().isBlank()) {
            return ResponseEntity.badRequest().body("Please fill all required fields");
        }
        if (userRepository.existsByEmail(user.getEmail().trim())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Email already registered");
        }
        if (userRepository.existsByStudentId(user.getStudentId().trim())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Student ID already registered");
        }
        user.setEmail(user.getEmail().trim().toLowerCase());
        user.setStudentId(user.getStudentId().trim());
        userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body("Registration successful");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginData data) {
        Optional<Users> found = userRepository.findByEmail(data.getEmail().trim().toLowerCase());
        if (found.isPresent() && found.get().getPassword().equals(data.getPassword())) {
            Users u = found.get();
            Map<String, Object> result = new HashMap<>();
            result.put("id", u.getId());
            result.put("name", u.getName());
            result.put("email", u.getEmail());
            result.put("studentId", u.getStudentId());
            result.put("department", u.getDepartment());
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password");
    }

    @GetMapping("/user/{id}")
    public ResponseEntity<?> getUser(@PathVariable int id) {
        return userRepository.findById(id)
                .<ResponseEntity<?>>map(u -> ResponseEntity.ok(toUserMap(u)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/students")
    public ResponseEntity<?> searchStudents(
            @RequestParam(defaultValue = "") String skill,
            @RequestParam(defaultValue = "") String department,
            @RequestParam(defaultValue = "") String interest,
            @RequestParam(defaultValue = "") String name,
            @RequestParam(required = false) Integer excludeUserId) {

        List<Map<String, Object>> result = new ArrayList<>();
        for (Users user : userRepository.findAll()) {
            if (excludeUserId != null && user.getId() == excludeUserId) continue;
            Optional<Profile> p = profileRepository.findByUserId(user.getId());
            String skills = p.map(Profile::getSkills).orElse("");
            String interests = p.map(Profile::getInterests).orElse("");

            boolean skillMatch = skill.isBlank() || skills.toLowerCase().contains(skill.toLowerCase());
            boolean deptMatch = department.isBlank() || (user.getDepartment() != null && user.getDepartment().equalsIgnoreCase(department));
            boolean interestMatch = interest.isBlank() || interests.toLowerCase().contains(interest.toLowerCase());
            boolean nameMatch = name.isBlank() || user.getName().toLowerCase().contains(name.toLowerCase());

            if (skillMatch && deptMatch && interestMatch && nameMatch) {
                Map<String, Object> item = toUserMap(user);
                item.put("profile", p.orElse(null));
                item.put("compatibility", calculateCompatibility(skill, interest, skills, interests));
                result.add(item);
            }
        }
        return ResponseEntity.ok(result);
    }

    private int calculateCompatibility(String wantedSkill, String wantedInterest,
                                       String skills, String interests) {
        int total = 0, match = 0;
        if (!wantedSkill.isBlank()) {
            total++;
            if (skills.toLowerCase().contains(wantedSkill.toLowerCase())) match++;
        }
        if (!wantedInterest.isBlank()) {
            total++;
            if (interests.toLowerCase().contains(wantedInterest.toLowerCase())) match++;
        }
        return total == 0 ? 0 : (match * 100 / total);
    }

    private Map<String, Object> toUserMap(Users u) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", u.getId());
        map.put("name", u.getName());
        map.put("email", u.getEmail());
        map.put("studentId", u.getStudentId());
        map.put("department", u.getDepartment());
        return map;
    }
}
