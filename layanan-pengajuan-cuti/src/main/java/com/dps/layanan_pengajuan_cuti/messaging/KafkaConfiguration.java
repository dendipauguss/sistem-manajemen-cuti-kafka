package com.dps.layanan_pengajuan_cuti.messaging;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfiguration {
	public static final String REQUESTS_TOPIC = "leave-requests";
	public static final String DECISIONS_TOPIC = "leave-decisions";

	@Value("${spring.kafka.bootstrap-servers:localhost:9092}")
	private String bootstrapServers;

	@Bean
	ProducerFactory<String, Object> producerFactory() {
		Map<String, Object> properties = new HashMap<>();
		properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
		return new DefaultKafkaProducerFactory<>(properties);
	}

	@Bean
	KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
		return new KafkaTemplate<>(producerFactory);
	}

	@Bean
	ConsumerFactory<String, LeaveDecisionEvent> leaveDecisionConsumerFactory() {
		Map<String, Object> properties = new HashMap<>();
		properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		properties.put(ConsumerConfig.GROUP_ID_CONFIG, "layanan-pengajuan-cuti");
		properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
		properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonJsonDeserializer.class);
		properties.put(JacksonJsonDeserializer.VALUE_DEFAULT_TYPE, LeaveDecisionEvent.class.getName());
		properties.put(JacksonJsonDeserializer.TRUSTED_PACKAGES, "com.dps.layanan_pengajuan_cuti.messaging");
		properties.put(JacksonJsonDeserializer.USE_TYPE_INFO_HEADERS, false);
		return new DefaultKafkaConsumerFactory<>(properties);
	}

	@Bean
	ConcurrentKafkaListenerContainerFactory<String, LeaveDecisionEvent> leaveDecisionKafkaListenerContainerFactory(
			ConsumerFactory<String, LeaveDecisionEvent> leaveDecisionConsumerFactory) {
		ConcurrentKafkaListenerContainerFactory<String, LeaveDecisionEvent> factory =
				new ConcurrentKafkaListenerContainerFactory<>();
		factory.setConsumerFactory(leaveDecisionConsumerFactory);
		return factory;
	}

	@Bean
	NewTopic leaveRequestsTopic() {
		return TopicBuilder.name(REQUESTS_TOPIC).partitions(1).replicas(1).build();
	}

	@Bean
	NewTopic leaveDecisionsTopic() {
		return TopicBuilder.name(DECISIONS_TOPIC).partitions(1).replicas(1).build();
	}
}
