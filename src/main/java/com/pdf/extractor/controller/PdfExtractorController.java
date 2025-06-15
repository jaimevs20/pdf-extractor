package com.pdf.extractor.controller;

import java.lang.System.Logger;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
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
	
	@SuppressWarnings("unchecked")
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
			String file = multipartFile.getOriginalFilename();
			JSONObject jsonObject = new JSONObject();
			StringBuilder full = new StringBuilder();
			
			for(String extractedForPage : pdfExtractorService.extractText(multipartFile).values()) {
				
				 if(extractedForPage.isEmpty()) {
					 JSONObject errors = new JSONObject();
					 errors.put("file",  multipartFile.getOriginalFilename());
					 errors.put("status", HttpStatus.BAD_REQUEST.value());
					 errors.put("message", "No text extracted in file");
					 
					 jsonError.add(errors);
					 continue;
				}
				
				full.append(extractedForPage).append("\n");

				logger.log(Logger.Level.INFO, "Page "+ extractedForPage.indexOf(extractedForPage) +" extracted and sent to Kafka topic successfully");
			}
			String fullDoc = full.toString();
			
			byte[] bytes = fullDoc.getBytes(StandardCharsets.UTF_8);
			String b64File = Base64.getEncoder().encodeToString(bytes);
			
			jsonObject.put("fileName", multipartFile.getOriginalFilename());
			jsonObject.put("encodedText", b64File);
			
			pdfKafkaProducer.sendMessage("pdf-extractor-topic", jsonObject);
			
			JSONObject success = new JSONObject();
			
			success.put("file", file);
			success.put("status", HttpStatus.OK.value());
			success.put("message", "processed successfully");

			jsonSuccess.add(success);
		}
		
		if(jsonSuccess.isEmpty() && jsonError.isEmpty()) {
			return ResponseEntity.internalServerError().build();
		}

		logger.log(Logger.Level.INFO, "PDF(s) extracted and sent to Kafka topic successfully");
		
		JSONObject response = new JSONObject();
		response.put("success", jsonSuccess);
		response.put("errors", jsonError);

		return ResponseEntity.ok(response);
	}
	
	@GetMapping("get-text")
	public ResponseEntity<String> getMessageText(@RequestHeader String fileName){
		if(fileName == null || fileName.isBlank()) {
			return ResponseEntity.badRequest().build();	
		}
		
		String response = pdfKafkaListener.getMessageText(fileName);
		
		if(response == null) {
			return ResponseEntity.internalServerError().build();
		}
		return ResponseEntity.ok(pdfKafkaListener.getMessageText(fileName));
	}
	
}
