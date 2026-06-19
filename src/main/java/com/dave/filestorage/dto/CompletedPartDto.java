package com.dave.filestorage.dto;

public class CompletedPartDto {

    private int partNumber;
    private String eTag;

    public CompletedPartDto() {
    }

    public CompletedPartDto(int partNumber, String eTag) {
        this.partNumber = partNumber;
        this.eTag = eTag;
    }

    public int getPartNumber() {
        return partNumber;
    }

    public void setPartNumber(int partNumber) {
        this.partNumber = partNumber;
    }

    public String getETag() {
        return eTag;
    }

    public void setETag(String eTag) {
        this.eTag = eTag;
    }
}
