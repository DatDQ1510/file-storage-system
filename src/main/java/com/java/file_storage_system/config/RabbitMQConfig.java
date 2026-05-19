package com.java.file_storage_system.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class RabbitMQConfig {

	public static final String FILE_ASSEMBLY_EXCHANGE = "file-assembly-exchange";
	public static final String FILE_ASSEMBLY_QUEUE = "file-assembly-queue";
	public static final String FILE_ASSEMBLY_ROUTING_KEY = "merge-key";
	public static final String TENANT_ACTIVATION_MAIL_EXCHANGE = "tenant-activation-mail-exchange";
	public static final String TENANT_ACTIVATION_MAIL_QUEUE = "tenant-activation-mail-queue";
	public static final String TENANT_ACTIVATION_MAIL_ROUTING_KEY = "tenant-activation-mail-key";

	@Bean
	public DirectExchange fileAssemblyExchange() {
		return new DirectExchange(FILE_ASSEMBLY_EXCHANGE, true, false);
	}

	@Bean
	public Queue fileAssemblyQueue() {
		return QueueBuilder.durable(FILE_ASSEMBLY_QUEUE).build();
	}

	@Bean
	public Binding fileAssemblyBinding(Queue fileAssemblyQueue, DirectExchange fileAssemblyExchange) {
		return BindingBuilder.bind(fileAssemblyQueue).to(fileAssemblyExchange).with(FILE_ASSEMBLY_ROUTING_KEY);
	}

	@Bean
	public DirectExchange tenantActivationMailExchange() {
		return new DirectExchange(TENANT_ACTIVATION_MAIL_EXCHANGE, true, false);
	}

	@Bean
	public Queue tenantActivationMailQueue() {
		return QueueBuilder.durable(TENANT_ACTIVATION_MAIL_QUEUE).build();
	}

	@Bean
	public Binding tenantActivationMailBinding(Queue tenantActivationMailQueue, DirectExchange tenantActivationMailExchange) {
		return BindingBuilder.bind(tenantActivationMailQueue)
				.to(tenantActivationMailExchange)
				.with(TENANT_ACTIVATION_MAIL_ROUTING_KEY);
	}

	@Bean
	public JacksonMessageConverter jacksonMessageConverter(ObjectMapper objectMapper) {
		return new JacksonMessageConverter(objectMapper);
	}

	@Bean
	public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, JacksonMessageConverter jacksonMessageConverter) {
		RabbitTemplate template = new RabbitTemplate(connectionFactory);
		template.setMessageConverter(jacksonMessageConverter);
		return template;
	}

	@Bean
	public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
			ConnectionFactory connectionFactory, 
			JacksonMessageConverter jacksonMessageConverter) {
		SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
		factory.setConnectionFactory(connectionFactory);
		factory.setMessageConverter(jacksonMessageConverter);
		return factory;
	}
}
