package com.java.file_storage_system.externalService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MailService {

    @Autowired
    private JavaMailSender mailSender; // Spring tự động lấy cấu hình từ properties nạp vào đây

    @Autowired
    private RedisTemplate template;

    @Scheduled(fixedRate = 5000)
    public void guiEmailDonGian() {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo("bhhoang1203@gmail.com");
        message.setSubject("Tiêu đề test");
        message.setText("Nội dung email");
        log.info(message.toString());
        mailSender.send(message); // Gọi lệnh gửi

        template.opsForValue().set("loda","hello world");

        // In ra màn hình Giá trị của key "loda" trong Redis
        System.out.println("Value of key loda: "+template.opsForValue().get("loda"));

    }
}