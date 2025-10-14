package kopo.poly.service.feign;

import kopo.poly.dto.GeminiRequest;
import kopo.poly.dto.GeminiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "geminiApi", url = "https://generativelanguage.googleapis.com/v1beta/models")
public interface GeminiAPIService {

    @PostMapping("/{modelName}:generateContent")
    GeminiResponse generateContent(
            @PathVariable("modelName") String modelName,
            @RequestParam("key") String apiKey,
            @RequestBody GeminiRequest request
    );
}