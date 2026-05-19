package com.java.file_storage_system.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java.file_storage_system.dto.chunk.FileAssemblyMessage;
import com.java.file_storage_system.dto.mail.TenantAdminActivationMailMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;

/**
 * Custom Jackson-based message converter for RabbitMQ messages.
 * Serializes/deserializes messages to/from JSON using Jackson ObjectMapper.
 * This avoids Java object serialization security issues in Spring AMQP.
 */
@Slf4j
public class JacksonMessageConverter implements MessageConverter {

    private final ObjectMapper objectMapper;
    private static final String JSON_TYPE = "application/json";
    private static final String TYPE_ID_HEADER = "__TypeId__";

    public JacksonMessageConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Message toMessage(Object object, MessageProperties messageProperties) 
            throws MessageConversionException {
        if (object == null) {
            return new Message(new byte[0], messageProperties);
        }

        try {
            messageProperties.setContentType(JSON_TYPE);
            messageProperties.setContentEncoding("UTF-8");
            // Store the class name for deserialization
            messageProperties.setHeader(TYPE_ID_HEADER, object.getClass().getName());
            byte[] jsonBytes = objectMapper.writeValueAsBytes(object);
            log.debug("Converted object of type {} to JSON message, size: {} bytes", 
                      object.getClass().getSimpleName(), jsonBytes.length);
            return new Message(jsonBytes, messageProperties);
        } catch (Exception e) {
            log.error("Failed to convert object to message: {}", object.getClass().getName(), e);
            throw new MessageConversionException("Failed to convert object to message", e);
        }
    }

    @Override
    public Object fromMessage(Message message) throws MessageConversionException {
        try {
            byte[] body = message.getBody();
            if (body.length == 0) {
                log.warn("Received empty message body");
                return null;
            }

            MessageProperties props = message.getMessageProperties();
            String typeHeader = (String) props.getHeader(TYPE_ID_HEADER);
            
            if (typeHeader != null && typeHeader.contains("FileAssemblyMessage")) {
                Object result = objectMapper.readValue(body, FileAssemblyMessage.class);
                log.debug("Deserialized message to FileAssemblyMessage, size: {} bytes", body.length);
                return result;
            }

            if (typeHeader != null && typeHeader.contains("TenantAdminActivationMailMessage")) {
                Object result = objectMapper.readValue(body, TenantAdminActivationMailMessage.class);
                log.debug("Deserialized message to TenantAdminActivationMailMessage, size: {} bytes", body.length);
                return result;
            }
            
            // Default: deserialize to generic object
            log.warn("No type information in message, deserializing to Object");
            return objectMapper.readValue(body, Object.class);
        } catch (Exception e) {
            log.error("Failed to deserialize message", e);
            throw new MessageConversionException("Failed to deserialize message", e);
        }
    }

    /**
     * Deserialize message to specific target class using Jackson
     */
    public <T> T fromMessage(Message message, Class<T> targetClass) 
            throws MessageConversionException {
        try {
            byte[] body = message.getBody();
            if (body.length == 0) {
                log.warn("Received empty message body");
                return null;
            }

            T result = objectMapper.readValue(body, targetClass);
            log.debug("Deserialized message to type {}, size: {} bytes", 
                      targetClass.getSimpleName(), body.length);
            return result;
        } catch (Exception e) {
            log.error("Failed to deserialize message to type {}", targetClass.getName(), e);
            throw new MessageConversionException("Failed to deserialize message", e);
        }
    }
}
