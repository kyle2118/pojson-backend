package com.pojson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayInputStream;
import java.util.Map;

@RestController
class PoJsonRestController {
    public static final ObjectMapper JACKSON = new ObjectMapper();
    private final Logger logger = LoggerFactory.getLogger(PoJsonRestController.class);
    private final PoJsonService service;

    public PoJsonRestController(PoJsonService service) {
        this.service = service;
    }


    @PostMapping(value = "/getFiles")
    public ResponseEntity<?> getFile(@RequestBody PoJsonRequest request) {

        if (request == null || request.getJson() == null) {
            logger.error("Invalid payload!");
            return new ResponseEntity<>("No payload is given!", HttpStatus.BAD_REQUEST);
        }
        String packageName = request.getPackageName();
        Map<String, Object> jsonMap = request.getJson();
        try {
            String json = JACKSON.writeValueAsString(jsonMap);
            byte[] fileContentBytes = service.generateFile(json, packageName);
            // Convert Java code string to InputStream
            ByteArrayInputStream inputStream = new ByteArrayInputStream(fileContentBytes);
            return new ResponseEntity<>(new InputStreamResource(inputStream), createResponseHeaders(), HttpStatus.OK);
        } catch (JsonProcessingException e) {
            logger.error("Unable to process json: ", e);
            return ResponseEntity.badRequest().body("Invalid json: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Exception while processing json: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected exception occurred: " + e.getMessage());
        }
    }

    public static HttpHeaders createResponseHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "GeneratedClasses.zip");
        return headers;
    }
}
