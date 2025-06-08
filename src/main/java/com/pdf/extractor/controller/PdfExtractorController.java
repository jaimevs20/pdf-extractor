package com.pdf.extractor.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.pdf.extractor.service.PdfExtractorService;

@RestController
@RequestMapping("/api/pdf-extractor")
public class PdfExtractorController {

	@Autowired
	PdfExtractorService pdfExtractorService;
	
	@PostMapping("extract-text")
	public ResponseEntity<String> getText(@RequestParam(name = "file") MultipartFile multipartFile){
		
		pdfExtractorService.extractText(multipartFile);
		
		return ResponseEntity.ok("Extracted");
	}
	
}
