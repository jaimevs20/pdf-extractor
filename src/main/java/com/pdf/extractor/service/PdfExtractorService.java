package com.pdf.extractor.service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.System.Logger;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import net.sourceforge.tess4j.Tesseract;

@Service
public class PdfExtractorService {
	Logger logger = System.getLogger(PdfExtractorService.class.getName());
	
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
	
}
