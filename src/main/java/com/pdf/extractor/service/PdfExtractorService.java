package com.pdf.extractor.service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import net.sourceforge.tess4j.Tesseract;

@Service
public class PdfExtractorService {
	Logger logger = System.getLogger(PdfExtractorService.class.getName());
	
	@Autowired
	private StringRedisTemplate redisTemplate;
	
	public Map<Integer, String> extractText(MultipartFile multipartFile) {
		try {
			File file = File.createTempFile("temp", ".pdf");
			
			multipartFile.transferTo(file);
			
			PDDocument document = PDDocument.load(file);
			PDFRenderer renderer = new PDFRenderer(document);
			
			Tesseract tess4j = new Tesseract();
			
			URL tessdataUrl = getClass().getClassLoader().getResource("tessdata");
			File tessdataDir = new File(tessdataUrl.toURI());
			tess4j.setDatapath(tessdataDir.getAbsolutePath());
			tess4j.setLanguage("por");
			
			logger.log(Logger.Level.INFO, "Processing "+ multipartFile.getOriginalFilename());
			
			Map<Integer, String> fullFile = new HashMap<>();
			
			for (int page = 0; page < document.getNumberOfPages(); page++) {
                BufferedImage image = renderer.renderImageWithDPI(page, 300);
                String text = tess4j.doOCR(image);
                fullFile.put(page, text);
            }
			
			document.close();
            file.delete();
			return fullFile;
		} catch(Exception e) {
			logger.log(Logger.Level.ERROR, "An error has occurred ".concat(e.getMessage()));
			return new HashMap<>();
		}
	}
	
	public String getMessageText(String fileName) {
		try {
			String message = redisTemplate.opsForValue().get(fileName);

			if(message == null) {
				return new String("");
			}
			
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
