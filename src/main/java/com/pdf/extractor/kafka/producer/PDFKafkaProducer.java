package com.pdf.extractor.kafka.producer;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import org.json.simple.JSONObject;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class PDFKafkaProducer {
	private final KafkaTemplate<String, String> kafkaTemplate;
	Logger logger = System.getLogger(PDFKafkaProducer.class.getName());
	
	public PDFKafkaProducer(KafkaTemplate<String, String> kafkaTemplate) {
		this.kafkaTemplate = kafkaTemplate;
	}
	
	public void sendMessage(String topic, JSONObject jsonB64Message) {
		String b64Message = jsonB64Message.toJSONString();
		kafkaTemplate.send(topic, b64Message);
		logger.log(Level.INFO, "Page sent to topic " + topic + ": " + b64Message);
	}
}
