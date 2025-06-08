package com.pdf.extractor.service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import net.sourceforge.tess4j.Tesseract;

@Service
public class PdfExtractorService {
	
	public String extractText(MultipartFile multipartFile) {
		try {
			URL urlFileTemp = PdfExtractorService.class.getClassLoader().getResource("file.tmp");
			
			File file = new File(urlFileTemp.toURI());
			
			multipartFile.transferTo(file);
			
			PDDocument document = PDDocument.load(file);
			PDFRenderer renderer = new PDFRenderer(document);
			
			Tesseract tess4j = new Tesseract();
			
			URL tessdataUrl = getClass().getClassLoader().getResource("tessdata");
			File tessdataDir = new File(tessdataUrl.toURI());
			tess4j.setDatapath(tessdataDir.getAbsolutePath());
			tess4j.setLanguage("por");
			
			System.out.println(multipartFile.getOriginalFilename());
			
			for (int page = 0; page < document.getNumberOfPages(); ++page) {
                BufferedImage image = renderer.renderImageWithDPI(page, 300);
                String text = tess4j.doOCR(image);
                //System.out.println("Page " + (page + 1) + " Text:\n" + text);
                return text;
            }
            document.close();
		} catch(Exception e) {
			System.err.println(e.getMessage());
			return "";
		}
		
		return "";
	}
	
}
