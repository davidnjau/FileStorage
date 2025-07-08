package com.dave.filestorage.dto;

public class ResponseBodyDto {

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