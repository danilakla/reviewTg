package com.example.telegramreminderbot.repository;

import com.example.telegramreminderbot.model.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;
import java.util.List;

public interface ReminderRepository extends JpaRepository<Reminder, Long> {
    List<Reminder> findByUserId(Long userId);
    
    @Query("SELECT r FROM Reminder r WHERE r.active = true AND r.reminderTime <= ?1")
    List<Reminder> findActiveRemindersForTime(LocalDateTime time);
    
    void deleteByUserIdAndId(Long userId, Long id);
} 