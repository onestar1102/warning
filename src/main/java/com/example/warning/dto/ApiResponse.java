// src/main/java/com/example/shelter/dto/ApiResponse.java
package com.example.warning.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class ApiResponse {
    @JsonProperty("response")
    private Response response;

    public Response getResponse() { return response; }
    public void setResponse(Response response) { this.response = response; }

    public static class Response {
        @JsonProperty("header")
        private Header header;

        @JsonProperty("body")
        private Body body;

        public Header getHeader() { return header; }
        public void setHeader(Header header) { this.header = header; }

        public Body getBody() { return body; }
        public void setBody(Body body) { this.body = body; }
    }

    public static class Header {
        @JsonProperty("resultCode")
        private String resultCode;

        @JsonProperty("resultMsg")
        private String resultMsg;

        public String getResultCode() { return resultCode; }
        public void setResultCode(String resultCode) { this.resultCode = resultCode; }

        public String getResultMsg() { return resultMsg; }
        public void setResultMsg(String resultMsg) { this.resultMsg = resultMsg; }
    }

    public static class Body {
        @JsonProperty("items")
        private Items items;

        @JsonProperty("numOfRows")
        private Integer numOfRows;

        @JsonProperty("pageNo")
        private Integer pageNo;

        @JsonProperty("totalCount")
        private Integer totalCount;

        public Items getItems() { return items; }
        public void setItems(Items items) { this.items = items; }

        public Integer getNumOfRows() { return numOfRows; }
        public void setNumOfRows(Integer numOfRows) { this.numOfRows = numOfRows; }

        public Integer getPageNo() { return pageNo; }
        public void setPageNo(Integer pageNo) { this.pageNo = pageNo; }

        public Integer getTotalCount() { return totalCount; }
        public void setTotalCount(Integer totalCount) { this.totalCount = totalCount; }
    }

    public static class Items {
        @JsonProperty("item")
        private List<ShelterItem> item;

        public List<ShelterItem> getItem() { return item; }
        public void setItem(List<ShelterItem> item) { this.item = item; }
    }

    public static class ShelterItem {
        @JsonProperty("vt_acmd_psbl_nmpr")
        private String accommodationCapacity; // 수용가능인원

        @JsonProperty("dtl_adres")
        private String address; // 소재지주소

        @JsonProperty("dsgntn_de")
        private String designationDate; // 지정일자

        @JsonProperty("mngnt_instt_nm")
        private String managementAgency; // 관리기관명

        @JsonProperty("ctprvn_nm")
        private String provinceName; // 시도명

        @JsonProperty("signgu_nm")
        private String cityName; // 시군구명

        @JsonProperty("emd_nm")
        private String districtName; // 읍면동명

        @JsonProperty("shnt_nm")
        private String shelterName; // 대피소명

        @JsonProperty("xcnts")
        private String longitude; // 경도

        @JsonProperty("ydnts")
        private String latitude; // 위도

        @JsonProperty("fclty_ar")
        private String facilityArea; // 시설면적

        @JsonProperty("cntct_no")
        private String contactNumber; // 연락처

        // Getter, Setter 메서드들
        public String getAccommodationCapacity() { return accommodationCapacity; }
        public void setAccommodationCapacity(String accommodationCapacity) {
            this.accommodationCapacity = accommodationCapacity;
        }

        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }

        public String getDesignationDate() { return designationDate; }
        public void setDesignationDate(String designationDate) { this.designationDate = designationDate; }

        public String getManagementAgency() { return managementAgency; }
        public void setManagementAgency(String managementAgency) { this.managementAgency = managementAgency; }

        public String getProvinceName() { return provinceName; }
        public void setProvinceName(String provinceName) { this.provinceName = provinceName; }

        public String getCityName() { return cityName; }
        public void setCityName(String cityName) { this.cityName = cityName; }

        public String getDistrictName() { return districtName; }
        public void setDistrictName(String districtName) { this.districtName = districtName; }

        public String getShelterName() { return shelterName; }
        public void setShelterName(String shelterName) { this.shelterName = shelterName; }

        public String getLongitude() { return longitude; }
        public void setLongitude(String longitude) { this.longitude = longitude; }

        public String getLatitude() { return latitude; }
        public void setLatitude(String latitude) { this.latitude = latitude; }

        public String getFacilityArea() { return facilityArea; }
        public void setFacilityArea(String facilityArea) { this.facilityArea = facilityArea; }

        public String getContactNumber() { return contactNumber; }
        public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }
    }
}