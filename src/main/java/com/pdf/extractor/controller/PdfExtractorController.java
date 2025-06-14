package com.pdf.extractor.controller;

import java.lang.System.Logger;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.catalina.connector.Response;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.google.gson.JsonElement;
import com.pdf.extractor.kafka.listener.PDFKafkaListener;
import com.pdf.extractor.kafka.producer.PDFKafkaProducer;
import com.pdf.extractor.service.PdfExtractorService;

@RestController
@RequestMapping("/api/pdf-extractor")
public class PdfExtractorController {

	@Autowired
	PdfExtractorService pdfExtractorService;
	@Autowired
	PDFKafkaProducer pdfKafkaProducer;
	@Autowired
	PDFKafkaListener pdfKafkaListener;
	
	Logger logger = System.getLogger(PdfExtractorController.class.getName());
	
	@PostMapping("extract-text")
	public ResponseEntity<Object> getText(@RequestParam(name = "file", required = false) List<MultipartFile> multipartFileList){
		
		JSONArray jsonSuccess = new JSONArray();
		JSONArray jsonError = new JSONArray();
		
		if(multipartFileList == null || multipartFileList.isEmpty()) {
			JSONObject errors = new JSONObject();
			 errors.put("status", HttpStatus.BAD_REQUEST.value());
			 errors.put("message", "No file provided");
			 
			return ResponseEntity.badRequest().body(errors);
		}
		
		for(MultipartFile multipartFile : multipartFileList) {
			String extractedText = pdfExtractorService.extractText(multipartFile);

			 if(extractedText.isEmpty()) {
				 JSONObject errors = new JSONObject();
				 errors.put("file",  multipartFile.getOriginalFilename());
				 errors.put("status", HttpStatus.BAD_REQUEST.value());
				 errors.put("message", "No text extracted in file");
				 
				 jsonError.add(errors);
				 continue;
			}
			
			byte[] bytes = extractedText.getBytes();
			String b64File = Base64.getEncoder().encodeToString(bytes);
			JSONObject jsonObject = new JSONObject();
			
			jsonObject.put("fileName", multipartFile.getOriginalFilename());
			jsonObject.put("encodedFirstPageText", b64File);
			
			pdfKafkaProducer.sendMessage("pdf-extractor-topic", jsonObject);
			JSONObject success = new JSONObject();
			
			success.put("file", jsonObject.get("fileName").toString());
			success.put("status", HttpStatus.OK.value());
			success.put("message", "processed successfully");

			jsonSuccess.add(success);
			
			logger.log(Logger.Level.INFO, "PDF(s) extracted and sent to Kafka topic successfully");
			
		}
		
		JSONObject response = new JSONObject();
		response.put("success", jsonSuccess);
		response.put("errors", jsonError);

		return ResponseEntity.ok(response);
	}
}
