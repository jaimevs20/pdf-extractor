package com.pdf.extractor.controller;

import java.lang.System.Logger;
import java.util.Base64;
import java.util.List;

import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
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
	
	@PostMapping("extract-text")
	public ResponseEntity<String> getText(@RequestParam(name = "file") List<MultipartFile> multipartFileList){
		
		if(multipartFileList.isEmpty()) {
			return ResponseEntity.badRequest().body("No files provided");
		}
		
		for(MultipartFile multipartFile : multipartFileList) {
			String extractedText = pdfExtractorService.extractText(multipartFile);

			 if(extractedText.isEmpty()) {
				return ResponseEntity.badRequest().body("No text extracted in file: "+ multipartFile.getOriginalFilename());
			}
			
			byte[] bytes = extractedText.getBytes();
			String b64File = Base64.getEncoder().encodeToString(bytes);
			JSONObject jsonObject = new JSONObject();
			
			jsonObject.put("fileName", multipartFile.getOriginalFilename());
			jsonObject.put("encodedFirstPageText", b64File);
			
			pdfKafkaProducer.sendMessage("pdf-extractor-topic", jsonObject);
			
			logger.log(Logger.Level.INFO, "PDF(s) extracted and sent to Kafka topic successfully");
			
		}
		return ResponseEntity.ok("PDF(s) extracted and sent to Kafka topic successfully");
	}
}
