package dev.prasadgaikwad.langchain4jdemo.api;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response of the image-description endpoint.
 */
@Schema(description = "The model's description of the image")
public record DescribeResponse(
        @Schema(description = "The model's answer about the image", example = "A black cat on a windowsill.")
        String answer) {
}
