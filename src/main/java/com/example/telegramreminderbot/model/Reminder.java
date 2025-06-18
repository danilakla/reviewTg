package com.example.telegramreminderbot.model;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "reminders")
public class Reminder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String eventName;
    private LocalDateTime reminderTime;
    
    @Enumerated(EnumType.STRING)
    private ReminderFrequency frequency;
    
    private boolean active = true;
    
    public enum ReminderFrequency {
        DAILY,
        WEEKLY,
        MONTHLY,
        ONCE
    }
} 