package com.example.demo;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TeamRequestRepository extends JpaRepository<TeamRequest, Integer> {
    List<TeamRequest> findByReceiverIdOrderByIdDesc(int receiverId);
    List<TeamRequest> findBySenderIdOrderByIdDesc(int senderId);
    List<TeamRequest> findByStatus(String status);
    boolean existsBySenderIdAndReceiverIdAndStatus(int senderId, int receiverId, String status);
}
