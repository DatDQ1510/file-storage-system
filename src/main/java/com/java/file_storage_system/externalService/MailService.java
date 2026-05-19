package com.java.file_storage_system.externalService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@Slf4j
public class MailService {

    @Value("${app.mail.test-scheduler-enabled:false}")
    private boolean testSchedulerEnabled;

    @Value("${app.mail.test-recipient:}")
    private String testRecipient;

    @Value("${app.mail.activation-enabled:false}")
    private boolean activationMailEnabled;

    @Value("${spring.mail.username:}")
    private String mailFromAddress;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Value("${app.frontend-base-url:http://localhost:5173}")
    private String frontendBaseUrl;

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

    public void sendTenantAdminActivationMail(
            String toEmail,
            String username,
            String tenantName,
            String token,
            Duration ttl
    ) {
        String activationLink = frontendBaseUrl.replaceAll("/+$", "")
                + "/setup-password?token=" + token;
        long ttlHours = Math.max(ttl.toHours(), 1);

        if (!activationMailEnabled) {
            log.info(
                    "Tenant admin activation mail is disabled. toEmail={}, tenantName={}, activationLink={}",
                    toEmail,
                    tenantName,
                    activationLink
            );
            return;
        }

        log.info(
                "tenant.activation.mail.smtp.config toEmail={}, usernameConfigured={}, passwordConfigured={}, frontendBaseUrl={}",
                toEmail,
                mailFromAddress != null && !mailFromAddress.isBlank(),
                mailPassword != null && !mailPassword.isBlank(),
                frontendBaseUrl
        );

        if (mailFromAddress == null || mailFromAddress.isBlank() || mailPassword == null || mailPassword.isBlank()) {
            throw new IllegalStateException("SMTP username/password is not configured. Check SPRING_MAIL_USERNAME and SPRING_MAIL_PASSWORD in the backend runtime environment.");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFromAddress);
        message.setTo(toEmail);
        message.setSubject("Activate your tenant admin account");
        message.setText("""
                Hello %s,

                Your tenant admin account for %s has been created.

                Please set your password using this link:
                %s

                This link expires in %d hour(s). If you did not expect this email, please ignore it.
                """.formatted(username, tenantName, activationLink, ttlHours));

        log.info("tenant.activation.mail.smtp.send-start toEmail={}, tenantName={}", toEmail, tenantName);
        mailSender.send(message);
        log.info("tenant.activation.mail.smtp.send-success toEmail={}, tenantName={}", toEmail, tenantName);
    }
}
