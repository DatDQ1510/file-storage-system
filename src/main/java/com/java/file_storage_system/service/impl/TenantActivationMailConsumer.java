package com.java.file_storage_system.service.impl;

import com.java.file_storage_system.dto.mail.TenantAdminActivationMailMessage;
import com.java.file_storage_system.externalService.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.stereotype.Component;

import java.time.Duration;

import static com.java.file_storage_system.config.RabbitMQConfig.TENANT_ACTIVATION_MAIL_QUEUE;

@Component
@Slf4j
@RequiredArgsConstructor
public class TenantActivationMailConsumer {

    private final MailService mailService;

    @RabbitListener(queues = TENANT_ACTIVATION_MAIL_QUEUE)
    public void handleTenantActivationMail(TenantAdminActivationMailMessage message) {
        if (message == null) {
            log.warn("tenant.activation.mail.consumer.empty-message");
            return;
        }

        log.info(
                "tenant.activation.mail.consumer.received messageId={}, tenantAdminId={}, tenantId={}, email={}, createdAt={}",
                message.messageId(),
                message.tenantAdminId(),
                message.tenantId(),
                message.toEmail(),
                message.createdAt()
        );

        try {
            mailService.sendTenantAdminActivationMail(
                    message.toEmail(),
                    message.username(),
                    message.tenantName(),
                    message.token(),
                    Duration.ofHours(Math.max(message.ttlHours(), 1))
            );

            log.info(
                    "tenant.activation.mail.consumer.sent messageId={}, tenantAdminId={}, email={}",
                    message.messageId(),
                    message.tenantAdminId(),
                    message.toEmail()
            );
        } catch (MailAuthenticationException exception) {
            log.error(
                    "tenant.activation.mail.consumer.auth-failed messageId={}, tenantAdminId={}, email={}, error={}",
                    message.messageId(),
                    message.tenantAdminId(),
                    message.toEmail(),
                    exception.getMessage(),
                    exception
            );
        } catch (IllegalStateException exception) {
            log.error(
                    "tenant.activation.mail.consumer.config-invalid messageId={}, tenantAdminId={}, email={}, error={}",
                    message.messageId(),
                    message.tenantAdminId(),
                    message.toEmail(),
                    exception.getMessage()
            );
        } catch (Exception exception) {
            log.error(
                    "tenant.activation.mail.consumer.failed messageId={}, tenantAdminId={}, email={}, error={}",
                    message.messageId(),
                    message.tenantAdminId(),
                    message.toEmail(),
                    exception.getMessage(),
                    exception
            );
            throw exception;
        }
    }
}
