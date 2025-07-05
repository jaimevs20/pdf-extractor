package com.pdf.extractor.kafka.listener;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.pdf.extractor.kafka.producer.PDFKafkaProducer;
import com.pdf.extractor.service.PdfExtractorService;

import java.io.UnsupportedEncodingException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mock.web.MockMultipartFile;


@Service
public class PDFKafkaListener {
	
	Logger logger = System.getLogger(PDFKafkaListener.class.getName());
	
	@Autowired
	private StringRedisTemplate redisTemplate;
	@Autowired
	PdfExtractorService pdfExtractorService;
	@Autowired
	PDFKafkaProducer pdfKafkaProducer;
	
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
	
	@KafkaListener(topics = "pdf-save-raw-topic", groupId = "pdf-extractor-group")
	public void consumeRawMessage(String message) {
		
		try {
			JSONParser jsonParser = new JSONParser();
			JSONObject jsonObject = (JSONObject) jsonParser.parse(message);
			StringBuilder full = new StringBuilder();
			
			String messageFromJson = jsonObject.get("rawText").toString();
			String fileNameFromJson = jsonObject.get("fileName").toString();

			byte [] rawMessage = Base64.getDecoder().decode(messageFromJson.getBytes(StandardCharsets.UTF_8));
			
			MultipartFile multipartFile = new MockMultipartFile(fileNameFromJson, fileNameFromJson, "application/pdf", rawMessage);
			
			for(String extractedForPage : pdfExtractorService.extractText(multipartFile).values()) {
				
				 full.append(extractedForPage).append("\n");

				logger.log(Logger.Level.INFO, "Page "+ extractedForPage.indexOf(extractedForPage) +" extracted from file " + fileNameFromJson);
			}
				
			String fullDoc = full.toString();
			
			byte[] bytes = fullDoc.getBytes(StandardCharsets.UTF_8);
			String b64File = Base64.getEncoder().encodeToString(bytes);
			
			jsonObject.put("fileName", fileNameFromJson);
			jsonObject.put("encodedText", b64File);
			
			pdfKafkaProducer.sendMessage("pdf-extractor-topic", jsonObject);
			
		} catch (Exception e) {
			logger.log(Level.ERROR, "Error processing pdf file message: " + e.getMessage());
		}
	}
}
