package com.pdf.extractor.controller;

import java.io.IOException;
import java.lang.System.Logger;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Date;
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
import jakarta.websocket.server.PathParam;


@RestController
@RequestMapping("/pdf-extractor")
public class PdfExtractorController {

	@Autowired
	PdfExtractorService pdfExtractorService;
	@Autowired
	PDFKafkaProducer pdfKafkaProducer;
	@Autowired
	PDFKafkaListener pdfKafkaListener;
	
	Logger logger = System.getLogger(PdfExtractorController.class.getName());
	
	@GetMapping("test")
	public ResponseEntity<String> getTest(){
		return ResponseEntity.ok().build();
	}
	
	@SuppressWarnings("unchecked")
	@PostMapping("upload")
	public ResponseEntity<Object> uploadDoc(@RequestParam(name = "file", required = false) List<MultipartFile> multipartFileList){
		
		JSONArray jsonSuccess = new JSONArray();
		JSONArray jsonError = new JSONArray();
		
		if(multipartFileList == null || multipartFileList.isEmpty()) {
			JSONObject errors = new JSONObject();
			 errors.put("status", HttpStatus.BAD_REQUEST.value());
			 errors.put("message", "No file provided");
			 
			return ResponseEntity.badRequest().body(errors);
		}
		
		for(MultipartFile multipartFile : multipartFileList) {
			
			if(multipartFile.getSize() > 100000) {
				JSONObject errors = new JSONObject();
				errors.put("fileName", multipartFile.getOriginalFilename());
				errors.put("status", HttpStatus.BAD_REQUEST.value());
				errors.put("message", "file is too large");
				jsonError.add(errors);
				
				continue;
			}
			
			String file = multipartFile.getOriginalFilename();
			JSONObject jsonObject = new JSONObject();
			
			try {
				byte[] bytes = multipartFile.getBytes();
				String b64File = Base64.getEncoder().encodeToString(bytes);
				
				jsonObject.put("fileName", multipartFile.getOriginalFilename());
				jsonObject.put("rawText", b64File);
				
				pdfKafkaProducer.sendMessage("pdf-save-raw-topic", jsonObject);
				
				logger.log(Logger.Level.INFO, "File ".concat(file).concat(" sent to Kafka (pdf-save-raw-topic) successfully"));
				
				JSONObject success = new JSONObject();
				
				success.put("file", file);
				success.put("status", HttpStatus.OK.value());
				success.put("message", "processed successfully");
	
				LocalDateTime date =  LocalDateTime.now();
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("YYYY-MM-dd'T'HH:mm:ss.SSS'Z'");
				
				success.put("processed_at", formatter.format(date));
				jsonSuccess.add(success);
			} catch(IOException e) {
				 JSONObject errors = new JSONObject();
				 errors.put("file",  multipartFile.getOriginalFilename());
				 errors.put("status", HttpStatus.BAD_REQUEST.value());
				 errors.put("message", e.getMessage());
				 
				 jsonError.add(errors);
			}
		}
		
		if(jsonSuccess.isEmpty() && !jsonError.isEmpty()) {
			return ResponseEntity.badRequest().build();
		} else if(jsonSuccess.isEmpty() && jsonError.isEmpty()) {
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
		
		String response = pdfExtractorService.getMessageText(fileName);
		
		if(response == null || response.isBlank()) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(response);
	}
	
	@GetMapping("get-word-from-file")
	public ResponseEntity<Object> findWord(@RequestHeader String fileName, @PathParam("word") String word) {
		
		if(fileName == null || fileName.isBlank() || word == null || word.isBlank()) {
			return ResponseEntity.badRequest().build();
		}
		
		String response = pdfExtractorService.getMessageText(fileName);
		
		if(response.isBlank()) {
			return ResponseEntity.status(404).build();
		} else if(response.toLowerCase().contains(word.toLowerCase())
				|| response.replaceAll("[^a-zA-Z0-9]", "").contains(word)) {
			return ResponseEntity.ok().build();
		}
		
		return ResponseEntity.status(404).build();
	}
}
