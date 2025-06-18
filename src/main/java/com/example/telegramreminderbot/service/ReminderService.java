package com.example.telegramreminderbot.service;

import com.example.telegramreminderbot.model.Reminder;
import com.example.telegramreminderbot.repository.ReminderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReminderService {
    private final ReminderRepository reminderRepository;
    private final MessageSenderService messageSenderService;

    @Transactional
    public void createReminder(Long chatId, String eventName, LocalDateTime time, Reminder.ReminderFrequency frequency) {
        Reminder reminder = new Reminder();
        reminder.setUserId(chatId);
        reminder.setEventName(eventName);
        reminder.setReminderTime(time);
        reminder.setFrequency(frequency);
        reminderRepository.save(reminder);
    }

    @Transactional(readOnly = true)
    public List<Reminder> getUserReminders(Long chatId) {
        return reminderRepository.findByUserId(chatId);
    }

    @Transactional
    public void deleteReminder(Long chatId, Long reminderId) {
        Reminder reminder = reminderRepository.findById(reminderId)
                .orElseThrow(() -> new RuntimeException("Reminder not found"));
        
        if (!reminder.getUserId().equals(chatId)) {
            throw new RuntimeException("Unauthorized to delete this reminder");
        }
        
        reminderRepository.delete(reminder);
    }

    public Reminder updateReminder(Long userId, Long reminderId, String eventName, LocalDateTime reminderTime) {
        Reminder reminder = reminderRepository.findById(reminderId)
                .orElseThrow(() -> new RuntimeException("Reminder not found"));
        
        if (!reminder.getUserId().equals(userId)) {
            throw new RuntimeException("Not authorized to update this reminder");
        }

        reminder.setEventName(eventName);
        reminder.setReminderTime(reminderTime);
        return reminderRepository.save(reminder);
    }


    @Scheduled(fixedRate = 60000) // Check every minute
    public void checkAndSendReminders() {
        LocalDateTime now = LocalDateTime.now();
        System.out.println(now.toString());
        List<Reminder> dueReminders = reminderRepository.findActiveRemindersForTime(now);

        for (Reminder reminder : dueReminders) {
            sendReminder(reminder);
            updateReminderAfterNotification(reminder);
        }
    }

    @Transactional
    public void sendReminder(Reminder reminder) {
        String message = String.format("Напоминание: %s", reminder.getEventName());
        messageSenderService.sendMessageWithDeleteButton(reminder.getUserId(), message, reminder.getId());
    }

    private void updateReminderAfterNotification(Reminder reminder) {
        if (reminder.getFrequency() == Reminder.ReminderFrequency.ONCE) {
            reminder.setActive(false);
            reminderRepository.save(reminder);
        } else {
            LocalDateTime nextTime = calculateNextReminderTime(reminder);
            reminder.setReminderTime(nextTime);
            reminderRepository.save(reminder);
        }
    }

    private LocalDateTime calculateNextReminderTime(Reminder reminder) {
        LocalDateTime currentTime = reminder.getReminderTime();
        switch (reminder.getFrequency()) {
            case DAILY:
                return currentTime.plusDays(1);
            case WEEKLY:
                return currentTime.plusWeeks(1);
            case MONTHLY:
                return currentTime.plusMonths(1);
            default:
                return currentTime;
        }
    }
} 