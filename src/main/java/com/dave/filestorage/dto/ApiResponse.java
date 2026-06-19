package com.dave.filestorage.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Standard API response envelope")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    @Schema(description = "Outcome: success or error")
    private final String status;

    @Schema(description = "Response payload (present on success)")
    private final T data;

    @Schema(description = "Error details (present on error)")
    private final ApiError error;

    @Schema(description = "Epoch milliseconds of the response")
    private final long timestamp;

    private ApiResponse(String status, T data, ApiError error) {
        this.status = status;
        this.data = data;
        this.error = error;
        this.timestamp = System.currentTimeMillis();
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("success", data, null);
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>("error", null, new ApiError(code, message));
    }

    public String getStatus() { return status; }
    public T getData() { return data; }
    public ApiError getError() { return error; }
    public long getTimestamp() { return timestamp; }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ApiError {
        private final String code;
        private final String message;

        public ApiError(String code, String message) {
            this.code = code;
            this.message = message;
        }

        public String getCode() { return code; }
        public String getMessage() { return message; }
    }
}
