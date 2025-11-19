// src/main/java/com/example/shelter/model/TsunamiShelter.java
package com.example.warning.model;

import jakarta.persistence.*;

@Entity
@Table(name = "tsunami_shelter")
public class TsunamiShelter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shelter_name")
    private String shelterName; // 대피소명


    @Column(name = "address")
    private String address; // 소재지주소

    @Column(name = "latitude")
    private Double latitude; // 위도

    @Column(name = "longitude")
    private Double longitude; // 경도

    @Column(name = "facility_area")
    private String facilityArea; // 시설면적

    @Column(name = "accommodation_capacity")
    private Integer accommodationCapacity; // 수용가능인원

    @Column(name = "management_agency")
    private String managementAgency; // 관리기관명

    @Column(name = "contact_number")
    private String contactNumber; // 연락처

    @Column(name = "designation_date")
    private String designationDate; // 지정일자

    // 거리 계산용 (데이터베이스에 저장되지 않음)
    @Transient
    private Double distanceFromUser;

    // 기본 생성자
    public TsunamiShelter() {}

    // Getter, Setter 메서드들
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getShelterName() { return shelterName; }
    public void setShelterName(String shelterName) { this.shelterName = shelterName; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getFacilityArea() { return facilityArea; }
    public void setFacilityArea(String facilityArea) { this.facilityArea = facilityArea; }

    public Integer getAccommodationCapacity() { return accommodationCapacity; }
    public void setAccommodationCapacity(Integer accommodationCapacity) {
        this.accommodationCapacity = accommodationCapacity;
    }

    public String getManagementAgency() { return managementAgency; }
    public void setManagementAgency(String managementAgency) { this.managementAgency = managementAgency; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

    public String getDesignationDate() { return designationDate; }
    public void setDesignationDate(String designationDate) { this.designationDate = designationDate; }

    public Double getDistanceFromUser() { return distanceFromUser; }
    public void setDistanceFromUser(Double distanceFromUser) { this.distanceFromUser = distanceFromUser; }
}