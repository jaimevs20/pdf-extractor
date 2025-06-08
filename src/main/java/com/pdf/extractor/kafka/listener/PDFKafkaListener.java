package com.pdf.extractor.kafka.listener;

import org.springframework.stereotype.Service;

import java.io.File;
import java.lang.System.Logger;
import java.util.Base64;

import org.apache.commons.io.FileUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.kafka.annotation.KafkaListener;

@Service
public class PDFKafkaListener {
	
	Logger logger = System.getLogger(PDFKafkaListener.class.getName());
	
	@KafkaListener(topics = "pdf-extractor-topic", groupId = "pdf-extractor-group")
	public String consumePdfMessage(String message) {
		
		try {
			JSONParser jsonParser = new JSONParser();
			JSONObject jsonObject = (JSONObject) jsonParser.parse(message);
			
			String messageFromJson = jsonObject.get("encodedFirstPageText").toString();
			
			// Decode the b64 encoded message
			byte[] pdfBytes = Base64.getDecoder().decode(messageFromJson);
			
			/** It can be saved to a temporary file, sent to a file database
			* File pdfFile = new File("pdf","extracted_pdf.pdf");
			* FileUtils.writeByteArrayToFile(pdfFile, pdfBytes);
			*/
			
			logger.log(Logger.Level.INFO, "PDF read successfully ".concat(message));
			
			String text = new String(pdfBytes, "UTF-8");
			// Just return the byte array as a string
			return "Texto\n: " + text;
			
		} catch (Exception e) {
			System.err.println("Error processing pdf file message: " + e.getMessage());
			return "";
		}
		
	}
	
	
}
