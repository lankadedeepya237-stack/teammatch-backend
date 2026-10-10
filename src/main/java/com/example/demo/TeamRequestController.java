package com.example.demo;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/requests")
@CrossOrigin(origins = "http://localhost:3000")
public class TeamRequestController {
    private final TeamRequestRepository requestRepository;
    private final UserRepository userRepository;

    public TeamRequestController(TeamRequestRepository requestRepository, UserRepository userRepository) {
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<?> sendRequest(@RequestBody RequestData data) {
        if (data.getSenderId() == data.getReceiverId()) {
            return ResponseEntity.badRequest().body("You cannot send a request to yourself");
        }
        if (!userRepository.existsById(data.getSenderId()) || !userRepository.existsById(data.getReceiverId())) {
            return ResponseEntity.badRequest().body("Invalid student");
        }
        if (requestRepository.existsBySenderIdAndReceiverIdAndStatus(data.getSenderId(), data.getReceiverId(), "PENDING")) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Request already sent");
        }
        TeamRequest request = new TeamRequest();
        request.setSenderId(data.getSenderId());
        request.setReceiverId(data.getReceiverId());
        request.setMessage(data.getMessage());
        request.setStatus("PENDING");
        return ResponseEntity.status(HttpStatus.CREATED).body(requestRepository.save(request));
    }

    @GetMapping("/received/{userId}")
    public ResponseEntity<?> received(@PathVariable int userId) {
        return ResponseEntity.ok(enrich(requestRepository.findByReceiverIdOrderByIdDesc(userId)));
    }

    @GetMapping("/sent/{userId}")
    public ResponseEntity<?> sent(@PathVariable int userId) {
        return ResponseEntity.ok(enrich(requestRepository.findBySenderIdOrderByIdDesc(userId)));
    }

    @PutMapping("/{id}/{action}")
    public ResponseEntity<?> updateRequest(@PathVariable int id, @PathVariable String action) {
        Optional<TeamRequest> found = requestRepository.findById(id);
        if (found.isEmpty()) return ResponseEntity.notFound().build();
        if (!action.equalsIgnoreCase("accept") && !action.equalsIgnoreCase("reject")) {
            return ResponseEntity.badRequest().body("Action must be accept or reject");
        }
        TeamRequest r = found.get();
        r.setStatus(action.equalsIgnoreCase("accept") ? "ACCEPTED" : "REJECTED");
        return ResponseEntity.ok(requestRepository.save(r));
    }

    @GetMapping("/team/{userId}")
    public ResponseEntity<?> team(@PathVariable int userId) {
        List<TeamRequest> accepted = requestRepository.findByStatus("ACCEPTED");
        List<Integer> ids = new ArrayList<>();
        ids.add(userId);
        for (TeamRequest r : accepted) {
            if (r.getSenderId() == userId) ids.add(r.getReceiverId());
            if (r.getReceiverId() == userId) ids.add(r.getSenderId());
        }
        List<Map<String, Object>> members = new ArrayList<>();
        for (Integer id : ids.stream().distinct().toList()) {
            userRepository.findById(id).ifPresent(u -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", u.getId());
                m.put("name", u.getName());
                m.put("email", u.getEmail());
                m.put("studentId", u.getStudentId());
                m.put("department", u.getDepartment());
                members.add(m);
            });
        }
        return ResponseEntity.ok(members);
    }

    private List<Map<String, Object>> enrich(List<TeamRequest> requests) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (TeamRequest r : requests) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", r.getId());
            m.put("senderId", r.getSenderId());
            m.put("receiverId", r.getReceiverId());
            m.put("message", r.getMessage());
            m.put("status", r.getStatus());
            m.put("createdAt", r.getCreatedAt());
            userRepository.findById(r.getSenderId()).ifPresent(u -> m.put("senderName", u.getName()));
            userRepository.findById(r.getReceiverId()).ifPresent(u -> m.put("receiverName", u.getName()));
            result.add(m);
        }
        return result;
    }
}
