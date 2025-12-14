package cs209a.finalproject_demo.controller;

import cs209a.finalproject_demo.dto.ApiResponse;
import cs209a.finalproject_demo.dto.MetadataResponse;
import cs209a.finalproject_demo.service.MetadataService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/metadata")
public class MetadataController {

    private final MetadataService metadataService;

    public MetadataController(MetadataService metadataService) {
        this.metadataService = metadataService;
    }

    @GetMapping("/status")
    public ApiResponse<MetadataResponse> status() {
        return ApiResponse.of(metadataService.snapshot());
    }
}



















