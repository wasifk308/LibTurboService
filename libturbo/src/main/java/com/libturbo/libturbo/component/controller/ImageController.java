package com.libturbo.libturbo.component.controller;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;


import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.springframework.web.bind.annotation.RequestParam;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageOutputStream;

@RestController
@RequestMapping("/api")
public class ImageController {

    @PostMapping("/download")
    public ResponseEntity<InputStreamResource> compressAndDownloadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("quality") float quality) throws Exception {

        // Save uploaded file to temp
        File inputFile = File.createTempFile("input-", "-" + file.getOriginalFilename());
        file.transferTo(inputFile);

        // Compressed output file
        String compressedFileName = "compressed-" + file.getOriginalFilename();
        File outputFile = new File(compressedFileName);

        // Compress
        boolean success = compressImage(inputFile, outputFile, quality);
        if (!success) {
            return ResponseEntity.status(500).build();
        }

        // Prepare response
        InputStreamResource resource = new InputStreamResource(new FileInputStream(outputFile));
        String mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE; // Fallback

        // Try to guess MIME type
        if (compressedFileName.endsWith(".jpg") || compressedFileName.endsWith(".jpeg")) {
            mimeType = MediaType.IMAGE_JPEG_VALUE;
        } else if (compressedFileName.endsWith(".png")) {
            mimeType = MediaType.IMAGE_PNG_VALUE;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + compressedFileName);

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(outputFile.length())
                .contentType(MediaType.parseMediaType(mimeType))
                .body(resource);

    }

    public boolean compressImage(File inputFile, File outputFile, float quality)
            throws IOException, InterruptedException {
        if (!inputFile.exists()) {
            throw new IllegalArgumentException("Input file does not exist.");
        }

        int qualityPercent = Math.round(quality * 100);

        String inputPath = inputFile.getAbsolutePath();
        String outputPath = outputFile.getAbsolutePath();

        List<String> command = new ArrayList<>();
        command.add("C:\\Program Files\\ImageMagick-7.1.1-Q16-HDRI\\magick.exe");
        command.add(inputPath);

        // Format-specific options
        if (outputPath.toLowerCase().endsWith(".jpg") || outputPath.toLowerCase().endsWith(".jpeg")) {
            // Flatten transparency and compress for JPEG
            command.add("-background");
            command.add("white");
            command.add("-flatten");
            command.add("-quality");
            command.add(String.valueOf(qualityPercent));
            command.add("-strip");
        } else if (outputPath.toLowerCase().endsWith(".png")) {
            command.add("-define");
            command.add("png:compression-level=9");
            command.add("-strip");
        }

        command.add(outputPath);

        ProcessBuilder pb = new ProcessBuilder(command);
        Process process = pb.start();
        int exitCode = process.waitFor();

        return exitCode == 0 && outputFile.exists();
    }

    @GetMapping("/CompressPng")
    public String CompressPng() throws IOException {
        BufferedImage image = ImageIO.read(new File("C:\\Users\\MSI\\OneDrive\\Pictures\\input1.png"));

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("png");
        ImageWriter writer = writers.next();

        File output = new File("compressed.png");
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(output)) {
            writer.setOutput(ios);

            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(0.0f); // 0 = max compression, 1 = no compression
            }

            writer.write(null, new javax.imageio.IIOImage(image, null, null), param);
        }
        writer.dispose();

        return "success";
    }

    @GetMapping("/CompressJPG")
    public String CompressImage() throws IOException {
        BufferedImage image = ImageIO.read(new File("C:\\Users\\MSI\\OneDrive\\Pictures\\input1.png"));

        if (image == null) {
            System.out.println("Failed to load image. Check the format and integrity of the file.");
            return "not";
        }

        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        JPEGImageWriteParam param = new JPEGImageWriteParam(null);
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(0.1f); // Set compression quality (0.0 to 1.0)

        File outputFile = new File("output-image.jpg");
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(outputFile)) {
            writer.setOutput(ios);
            writer.write(null, new javax.imageio.IIOImage(image, null, null), param);
        }

        System.out.println("JPEG image written with quality settings.");

        return "success";
    }


    public static BufferedImage resizeImage(BufferedImage originalImage, double scaleFactor) {
        int width = (int) (originalImage.getWidth() * scaleFactor);
        int height = (int) (originalImage.getHeight() * scaleFactor);
        BufferedImage resizedImage = new BufferedImage(width, height, originalImage.getType());
        resizedImage.getGraphics().drawImage(originalImage, 0, 0, width, height, null);
        return resizedImage;
    }

    // // // Method to convert BufferedImage to raw BGR byte array
    private byte[] bufferedImageToRawByteArray(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int pitch = width * 3;
        byte[] pixels = new byte[pitch * height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int index = y * pitch + x * 3;
                pixels[index] = (byte) (rgb & 0xFF); // Blue
                pixels[index + 1] = (byte) ((rgb >> 8) & 0xFF); // Green
                pixels[index + 2] = (byte) ((rgb >> 16) & 0xFF); // Red
            }
        }

        return pixels;
    }


}
