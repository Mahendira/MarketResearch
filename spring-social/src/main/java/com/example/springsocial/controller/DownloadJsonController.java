package com.example.springsocial.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class DownloadJsonController {

    @Value("${external.api.url:https://api.liquidspace.com/marketplace/api/search}")
    private String externalApiUrl;

    @Value("${external.api.key:b457c6cf7ff84edc96bc2cb81c2184b7}")
    private String apiKey;

    @Value("${file.download.directory:./downloads}")
    private String downloadDirectory;
    @PostMapping("/download-json")
    public ResponseEntity<?> downloadJson(@RequestBody Map<String, String> requestBody) {
        try {
            // Extract "address" from the request body
            String address = requestBody.getOrDefault("address", "default");

            // Create file name with address and current date
            String currentDate = new SimpleDateFormat("yyyyMMdd").format(new Date());
            String fileName = String.format("%s-%s-%s.csv", "address", address, currentDate);
            File file = new File(downloadDirectory, fileName);

            // Check if file already exists
            if (file.exists()) {
                InputStreamResource resource = new InputStreamResource(new FileInputStream(file));

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
                headers.setContentDispositionFormData("attachment", fileName);

                return ResponseEntity.ok()
                        .headers(headers)
                        .body(resource);
            }

            // Prepare the body and headers for the POST request
            RestTemplate restTemplate = new RestTemplate();

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("Cache-Control", "no-cache");
            headers.add("LS-Subscription-Key", apiKey);

            // Convert Map to JSON
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonRequestBody = objectMapper.writeValueAsString(requestBody);

            org.springframework.http.HttpEntity<String> requestEntity =
                    new org.springframework.http.HttpEntity<>(jsonRequestBody, headers);

            // Send the POST request
            ResponseEntity<String> response = restTemplate.postForEntity(externalApiUrl, requestEntity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                // Ensure the download directory exists
                Files.createDirectories(Paths.get(downloadDirectory));

                // Parse response and write to CSV
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                FileWriter csvWriter = new FileWriter(file);
                csvWriter.append("venue-name;venue-address;workspaceTypesFormatted;workspaceSearchResults-spaceTypeFormatted;price;priceDescription\n");

                for (JsonNode venue : jsonNode.path("venues")) {
                    String name = venue.path("name").asText();
                    String addressField = venue.path("address").asText();
                    String workspaceTypes = venue.path("workspaceTypesFormatted").asText();

                    JsonNode workspaceSearchResults = venue.path("workspaceSearchResults");
                    if (workspaceSearchResults.isArray()) {
                        for (JsonNode workspace : workspaceSearchResults) {
                            String spaceTypeFormatted = workspace.path("spaceTypeFormatted").asText();
                            String price = workspace.path("price").asText();
                            String priceDescription = workspace.path("priceDescription").asText();

                            csvWriter.append(String.format("%s;%s;%s;%s;%s;%s\n",
                                    name, addressField, workspaceTypes, spaceTypeFormatted, price, priceDescription));
                        }
                    }
                }

                csvWriter.flush();
                csvWriter.close();

                // Return the file as a downloadable response
                InputStreamResource resource = new InputStreamResource(new FileInputStream(file));

                HttpHeaders downloadHeaders = new HttpHeaders();
                downloadHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
                downloadHeaders.setContentDispositionFormData("attachment", fileName);

                return ResponseEntity.ok()
                        .headers(downloadHeaders)
                        .body(resource);
            } else {
                return ResponseEntity.status(response.getStatusCode())
                        .body("Failed to fetch data: " + response.getStatusCode());
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error occurred: " + e.getMessage());
        }
    }
}
//    @PostMapping("/download-json")
//    public ResponseEntity<?> downloadJson(@RequestBody Map<String, String> requestBody) {
//        try {
//            // Extract "address" from the request body
//            String address = requestBody.getOrDefault("address", "default");
//
//            // Create file name with address and current date
//            String currentDate = new SimpleDateFormat("yyyyMMdd").format(new Date());
//            String fileName = String.format("%s-%s-%s.json", "address", address, currentDate);
//            File file = new File(downloadDirectory, fileName);
//
//            // Check if file already exists
//            if (file.exists()) {
//                InputStreamResource resource = new InputStreamResource(new FileInputStream(file));
//
//                HttpHeaders headers = new HttpHeaders();
//                headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
//                headers.setContentDispositionFormData("attachment", fileName);
//
//                return ResponseEntity.ok()
//                        .headers(headers)
//                        .body(resource);
//            }
//
//            // Prepare the body and headers for the POST request
//            RestTemplate restTemplate = new RestTemplate();
//
//            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
//            headers.setContentType(MediaType.APPLICATION_JSON);
//            headers.add("Cache-Control", "no-cache");
//            headers.add("LS-Subscription-Key", apiKey);
//
//            // Convert Map to JSON
//            ObjectMapper objectMapper = new ObjectMapper();
//            String jsonRequestBody = objectMapper.writeValueAsString(requestBody);
//
//            org.springframework.http.HttpEntity<String> requestEntity =
//                    new org.springframework.http.HttpEntity<>(jsonRequestBody, headers);
//
//            // Send the POST request
//            ResponseEntity<String> response = restTemplate.postForEntity(externalApiUrl, requestEntity, String.class);
//
//            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
//                // Ensure the download directory exists
//                Files.createDirectories(Paths.get(downloadDirectory));
//
//                // Write the response body to the file
//                try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
//                    writer.write(response.getBody());
//                }
//
//                // Return the file as a downloadable response
//                InputStreamResource resource = new InputStreamResource(new FileInputStream(file));
//
//                HttpHeaders downloadHeaders = new HttpHeaders();
//                downloadHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
//                downloadHeaders.setContentDispositionFormData("attachment", fileName);
//
//                return ResponseEntity.ok()
//                        .headers(downloadHeaders)
//                        .body(resource);
//            } else {
//                return ResponseEntity.status(response.getStatusCode())
//                        .body("Failed to fetch data: " + response.getStatusCode());
//            }
//
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body("Error occurred: " + e.getMessage());
//        }
//    }
//
//}
