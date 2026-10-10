package com.example.demo;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/projects")
@CrossOrigin(origins = "http://localhost:3000")
public class ProjectController {
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public ProjectController(ProjectRepository projectRepository, UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody ProjectData data) {
        if (!userRepository.existsById(data.getOwnerId())) {
            return ResponseEntity.badRequest().body("Invalid owner");
        }
        if (data.getName() == null || data.getName().isBlank()) {
            return ResponseEntity.badRequest().body("Project name is required");
        }
        Project p = new Project();
        p.setOwnerId(data.getOwnerId());
        p.setName(data.getName());
        p.setDescription(data.getDescription());
        p.setTechnology(data.getTechnology());
        p.setDeadline(data.getDeadline());
        p.setStatus(data.getStatus() == null || data.getStatus().isBlank() ? "NOT STARTED" : data.getStatus());
        return ResponseEntity.status(HttpStatus.CREATED).body(projectRepository.save(p));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserProjects(@PathVariable int userId) {
        return ResponseEntity.ok(projectRepository.findByOwnerIdOrderByIdDesc(userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable int id, @RequestBody ProjectData data) {
        Optional<Project> found = projectRepository.findById(id);
        if (found.isEmpty()) return ResponseEntity.notFound().build();
        Project p = found.get();
        if (data.getName() != null) p.setName(data.getName());
        if (data.getDescription() != null) p.setDescription(data.getDescription());
        if (data.getTechnology() != null) p.setTechnology(data.getTechnology());
        if (data.getDeadline() != null) p.setDeadline(data.getDeadline());
        if (data.getStatus() != null) p.setStatus(data.getStatus());
        return ResponseEntity.ok(projectRepository.save(p));
    }
}
