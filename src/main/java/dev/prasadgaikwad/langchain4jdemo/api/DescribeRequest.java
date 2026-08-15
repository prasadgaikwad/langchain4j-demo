package dev.prasadgaikwad.langchain4jdemo.api;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request for the image-description endpoint: either an image {@code url} or
 * base64-encoded {@code imageData} with a {@code mimeType} must be provided,
 * plus the question to ask about the image.
 */
@Schema(description = "Image to describe and the question to ask about it")
public record DescribeRequest(
        @Schema(description = "Public URL of the image", example = "https://example.com/cat.png")
        String imageUrl,
        @Schema(description = "Base64-encoded image data (alternative to imageUrl)", example = "iVBORw0KGgo...")
        String imageData,
        @Schema(description = "Mime type of the image data", example = "image/png")
        String mimeType,
        @Schema(description = "Question to answer about the image",
                example = "What is visible in this image?", defaultValue = "Describe this image.")
        String question) {
}
