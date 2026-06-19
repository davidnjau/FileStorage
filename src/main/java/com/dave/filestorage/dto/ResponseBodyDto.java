package com.dave.filestorage.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Generic message response.
 */
@Schema(description = "Generic message response")
public class ResponseBodyDto {

    @Schema(description = "Human-readable status message")
    private String details;

    public ResponseBodyDto() {
    }

    public ResponseBodyDto(String details) {
        this.details = details;
    }

    public String getDetails() {
        return details;
    }

    /**
     * Sets the details of the response.
     *
     * @param details the details to set for the response
     */
    public void setDetails(String details) {
        this.details = details;
    }
}