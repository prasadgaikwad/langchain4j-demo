package dev.prasadgaikwad.langchain4jdemo.api;

import dev.prasadgaikwad.langchain4jdemo.multimodal.VisionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Base64;

/**
 * REST endpoint for the multi-modal vision capability.
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Vision", description = "Ask a model to describe an image")
public class VisionApiController {

    private final VisionService visionService;

    public VisionApiController(VisionService visionService) {
        this.visionService = visionService;
    }

    @PostMapping("/describe")
    @Operation(summary = "Describe an image",
            description = "Sends an image (by URL or base64 data) together with a question to a "
                    + "multimodal model and returns the model's description.")
    @ApiResponse(responseCode = "200", description = "The model's answer",
            content = @Content(schema = @Schema(implementation = DescribeResponse.class)))
    @ApiResponse(responseCode = "400", description = "No image provided (need imageUrl or imageData)")
    public ResponseEntity<DescribeResponse> describe(@RequestBody DescribeRequest request) {
        String question = request.question() == null || request.question().isBlank()
                ? "Describe this image."
                : request.question();

        if (request.imageUrl() != null && !request.imageUrl().isBlank()) {
            return ResponseEntity.ok(new DescribeResponse(visionService.describeImage(request.imageUrl(), question)));
        }
        if (request.imageData() != null && request.mimeType() != null && !request.imageData().isBlank()) {
            byte[] bytes = Base64.getDecoder().decode(request.imageData());
            return ResponseEntity.ok(new DescribeResponse(visionService.describeImage(bytes, request.mimeType(), question)));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
}
