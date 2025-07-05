package com.pdf.extractor.kafka.listener;

import org.springframework.stereotype.Service;
import java.io.UnsupportedEncodingException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.time.Duration;
import java.util.Base64;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;

@Service
public class PDFKafkaListener {
	
	Logger logger = System.getLogger(PDFKafkaListener.class.getName());
	
	@Autowired
	private StringRedisTemplate redisTemplate;
	
	@KafkaListener(topics = "pdf-extractor-topic", groupId = "pdf-extractor-group")
	public void consumePdfMessage(String message) {
		
		try {
			JSONParser jsonParser = new JSONParser();
			JSONObject jsonObject = (JSONObject) jsonParser.parse(message);
			
			String messageFromJson = jsonObject.get("encodedText").toString();

			redisTemplate.opsForValue().set(jsonObject.get("fileName").toString(), messageFromJson, Duration.ofMinutes(20));
			logger.log(Level.INFO, "Text saved in Redis");
			
		} catch (Exception e) {
			logger.log(Level.ERROR, "Error processing pdf file message: " + e.getMessage());
		}
		
	}
}
