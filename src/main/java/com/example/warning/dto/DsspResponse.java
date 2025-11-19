package com.example.warning.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class DsspResponse {

    @JsonProperty("header")
    private Header header;

    @JsonProperty("numOfRows")
    private int numOfRows;

    @JsonProperty("pageNo")
    private int pageNo;

    @JsonProperty("totalCount")
    private int totalCount;

    @JsonProperty("body")
    private List<DsspItem> body;

    @Data
    public static class Header {
        @JsonProperty("resultMsg")
        private String resultMsg;

        @JsonProperty("resultCode")
        private String resultCode;

        @JsonProperty("errorMsg")
        private String errorMsg;
    }

    @Data
    public static class DsspItem {

        @JsonProperty("LA") // 위도
        private Double latitude;

        @JsonProperty("LO") // 경도
        private Double longitude;

        @JsonProperty("SHNT_PLACE_NM") // 대피소 명칭
        private String shelterName;

        @JsonProperty("SHNT_PLACE_DTL_POSITION") // 상세 주소
        private String address;

        @JsonProperty("PSBL_NMPR") // 수용 가능 인원
        private Integer capacity;

        @JsonProperty("USE_AT") // 사용 가능 여부 Y/N
        private String useAt;

        // 추가 필드 필요하면 여기 넣어도 됨
    }
}
