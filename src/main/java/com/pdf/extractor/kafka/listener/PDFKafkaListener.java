package com.pdf.extractor.kafka.listener;

import org.springframework.stereotype.Service;
import java.io.UnsupportedEncodingException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
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

			redisTemplate.opsForValue().set(jsonObject.get("fileName").toString(), messageFromJson);;
			logger.log(Level.INFO, "Text saved in Redis");
			
		} catch (Exception e) {
			logger.log(Level.ERROR, "Error processing pdf file message: " + e.getMessage());
		}
		
	}
	
	public String getMessageText(String fileName) {
		try {
			String message = redisTemplate.opsForValue().get(fileName);

			// Decode the b64 encoded message
			byte[] pdfBytes = Base64.getDecoder().decode(message);
			
			logger.log(Logger.Level.INFO, "PDF read successfully ".concat(message));
			
			return new String(pdfBytes, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			logger.log(Level.ERROR, e.getMessage());
			e.printStackTrace();
			return null;
		}
	}
	
	
}
