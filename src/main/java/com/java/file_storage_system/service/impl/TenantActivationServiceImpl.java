package com.java.file_storage_system.service.impl;

import com.java.file_storage_system.custom.CustomUserDetails;
import com.java.file_storage_system.custom.JwtTokenProvider;
import com.java.file_storage_system.dto.mail.TenantAdminActivationMailMessage;
import com.java.file_storage_system.dto.tenant.activation.TenantActivationCompleteRequest;
import com.java.file_storage_system.dto.tenant.activation.TenantActivationCompleteResponse;
import com.java.file_storage_system.dto.tenant.activation.TenantActivationTokenInfo;
import com.java.file_storage_system.entity.TenantAdminEntity;
import com.java.file_storage_system.entity.TenantEntity;
import com.java.file_storage_system.exception.BadRequestException;
import com.java.file_storage_system.exception.ResourceNotFoundException;
import com.java.file_storage_system.exception.UnauthorizedException;
import com.java.file_storage_system.repository.TenantAdminRepository;
import com.java.file_storage_system.service.TenantActivationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static com.java.file_storage_system.config.RabbitMQConfig.TENANT_ACTIVATION_MAIL_EXCHANGE;
import static com.java.file_storage_system.config.RabbitMQConfig.TENANT_ACTIVATION_MAIL_ROUTING_KEY;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantActivationServiceImpl implements TenantActivationService {

    private static final String ACTIVATION_TOKEN_PREFIX = "tenant-admin:activation:";

    private final TenantAdminRepository tenantAdminRepository;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<Object, Object> redisTemplate;
    private final JwtTokenProvider jwtTokenProvider;
    private final RabbitTemplate rabbitTemplate;

    @Value("${app.tenant-activation.ttl-hours:24}")
    private long activationTtlHours;

    @Override
    public void createActivationInvitation(TenantAdminEntity tenantAdmin) {
        String token = UUID.randomUUID().toString();
        Duration ttl = Duration.ofHours(activationTtlHours);
        TenantEntity tenant = tenantAdmin.getTenant();
        String messageId = UUID.randomUUID().toString();

        redisTemplate.opsForValue().set(getActivationKey(token), tenantAdmin.getId(), ttl);

        log.info(
                "tenant.activation.created tenantAdminId={}, tenantId={}, email={}, tokenPrefix={}, ttlHours={}",
                tenantAdmin.getId(),
                tenant == null ? null : tenant.getId(),
                tenantAdmin.getEmail(),
                token.substring(0, 8),
                ttl.toHours()
        );

        TenantAdminActivationMailMessage message = new TenantAdminActivationMailMessage(
                messageId,
                tenantAdmin.getId(),
                tenant == null ? null : tenant.getId(),
                tenant == null ? null : tenant.getNameTenant(),
                tenantAdmin.getEmail(),
                tenantAdmin.getUserName(),
                token,
                Math.max(ttl.toHours(), 1),
                Instant.now().toString()
        );

        rabbitTemplate.convertAndSend(
                TENANT_ACTIVATION_MAIL_EXCHANGE,
                TENANT_ACTIVATION_MAIL_ROUTING_KEY,
                message,
                amqpMessage -> {
                    amqpMessage.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    amqpMessage.getMessageProperties().setMessageId(messageId);
                    return amqpMessage;
                }
        );

        log.info(
                "tenant.activation.mail.queued messageId={}, tenantAdminId={}, exchange={}, routingKey={}, email={}",
                messageId,
                tenantAdmin.getId(),
                TENANT_ACTIVATION_MAIL_EXCHANGE,
                TENANT_ACTIVATION_MAIL_ROUTING_KEY,
                tenantAdmin.getEmail()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public TenantActivationTokenInfo validateActivationToken(String token) {
        if (token == null || token.isBlank()) {
            return TenantActivationTokenInfo.invalid("Activation token is required.");
        }

        String key = getActivationKey(token.trim());
        log.info("tenant.activation.validate.start tokenPrefix={}", token.trim().substring(0, Math.min(token.trim().length(), 8)));
        Object tenantAdminId = redisTemplate.opsForValue().get(key);
        if (tenantAdminId == null) {
            log.warn("tenant.activation.validate.invalid tokenPrefix={}", token.trim().substring(0, Math.min(token.trim().length(), 8)));
            return TenantActivationTokenInfo.invalid("Activation token is invalid or expired.");
        }

        TenantAdminEntity tenantAdmin = tenantAdminRepository.findById(tenantAdminId.toString())
                .orElse(null);
        if (tenantAdmin == null) {
            redisTemplate.delete(key);
            log.warn("tenant.activation.validate.missing-admin tenantAdminId={}", tenantAdminId);
            return TenantActivationTokenInfo.invalid("Tenant admin account no longer exists.");
        }

        TenantEntity tenant = tenantAdmin.getTenant();
        Long ttlSeconds = redisTemplate.getExpire(key);
        Instant expiresAt = ttlSeconds == null || ttlSeconds < 0
                ? null
                : Instant.now().plusSeconds(ttlSeconds);

        log.info(
                "tenant.activation.validate.success tenantAdminId={}, tenantId={}, email={}, ttlSeconds={}",
                tenantAdmin.getId(),
                tenant == null ? null : tenant.getId(),
                tenantAdmin.getEmail(),
                ttlSeconds
        );

        return new TenantActivationTokenInfo(
                true,
                "Activation token is valid.",
                tenant == null ? null : tenant.getNameTenant(),
                tenant == null ? null : tenant.getDomainTenant(),
                tenantAdmin.getEmail(),
                expiresAt
        );
    }

    @Override
    @Transactional
    public TenantActivationCompleteResponse completeActivation(TenantActivationCompleteRequest request) {
        String token = request.token() == null ? "" : request.token().trim();
        String key = getActivationKey(token);
        log.info("tenant.activation.complete.start tokenPrefix={}", token.substring(0, Math.min(token.length(), 8)));
        Object tenantAdminId = redisTemplate.opsForValue().get(key);
        if (tenantAdminId == null) {
            log.warn("tenant.activation.complete.invalid tokenPrefix={}", token.substring(0, Math.min(token.length(), 8)));
            throw new UnauthorizedException("Activation token is invalid or expired");
        }

        if (!request.password().equals(request.confirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }

        if (!isValidPassword(request.password())) {
            throw new BadRequestException("Password must be at least 8 characters and include at least 3 character types");
        }

        TenantAdminEntity tenantAdmin = tenantAdminRepository.findById(tenantAdminId.toString())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant admin not found"));

        tenantAdmin.setHashedPassword(passwordEncoder.encode(request.password()));
        tenantAdminRepository.save(tenantAdmin);
        redisTemplate.delete(key);

        TenantEntity tenant = tenantAdmin.getTenant();
        CustomUserDetails principal = new CustomUserDetails(
                tenantAdmin.getId(),
                tenantAdmin.getUserName(),
                tenantAdmin.getHashedPassword(),
                "TENANT_ADMIN",
                tenant == null ? null : tenant.getId(),
                tenantAdmin.getEmail()
        );

        log.info(
                "tenant.activation.complete.success tenantAdminId={}, tenantId={}, email={}",
                tenantAdmin.getId(),
                tenant == null ? null : tenant.getId(),
                tenantAdmin.getEmail()
        );

        return new TenantActivationCompleteResponse(
                jwtTokenProvider.generateAccessToken(principal),
                "Account activated successfully.",
                principal.getRole(),
                principal.getTenantId(),
                principal.getId(),
                principal.getUsername(),
                principal.getEmail()
        );
    }

    private String getActivationKey(String token) {
        return ACTIVATION_TOKEN_PREFIX + token;
    }

    private boolean isValidPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }

        int score = 0;
        score += password.chars().anyMatch(Character::isUpperCase) ? 1 : 0;
        score += password.chars().anyMatch(Character::isLowerCase) ? 1 : 0;
        score += password.chars().anyMatch(Character::isDigit) ? 1 : 0;
        score += password.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch)) ? 1 : 0;

        return score >= 3;
    }
}
