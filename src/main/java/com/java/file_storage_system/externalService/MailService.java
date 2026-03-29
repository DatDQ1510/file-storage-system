package com.java.file_storage_system.externalService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MailService {

    @Value("${app.mail.test-scheduler-enabled:false}")
    private boolean testSchedulerEnabled;

    @Value("${app.mail.test-recipient:}")
    private String testRecipient;

    @Autowired
    private JavaMailSender mailSender; // Spring tự động lấy cấu hình từ properties nạp vào đây

    @Autowired
    private RedisTemplate<String, String> template;

    @Scheduled(fixedRate = 5000)
    public void guiEmailDonGian() {
        if (!testSchedulerEnabled || testRecipient == null || testRecipient.isBlank()) {
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(testRecipient);
        message.setSubject("Tiêu đề test");
        message.setText("Nội dung email");
        log.info(message.toString());
        mailSender.send(message); // Gọi lệnh gửi

        template.opsForValue().set("loda", "hello world");

        // In ra màn hình Giá trị của key "loda" trong Redis
        System.out.println("Value of key loda: " + template.opsForValue().get("loda"));

    }
}