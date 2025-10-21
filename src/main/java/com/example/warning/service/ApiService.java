// src/main/java/com/example/shelter/service/ApiService.java
package com.example.warning.service;

import com.example.warning.model.TsunamiShelter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ApiService {

    private static final Logger logger = LoggerFactory.getLogger(ApiService.class);

    /**
     * 더미 데이터로 대피소 정보 생성
     */
    public List<TsunamiShelter> fetchShelterData(int pageNo, int numOfRows) {
        logger.info("더미 데이터 로드 시작: pageNo={}, numOfRows={}", pageNo, numOfRows);
        return createDummyShelterData();
    }

    /**
     * 전체 더미 데이터 반환
     */
    public List<TsunamiShelter> fetchAllShelterData() {
        logger.info("전체 더미 데이터 로드");
        return createDummyShelterData();
    }

    /**
     * 더미 대피소 데이터 생성
     */
    private List<TsunamiShelter> createDummyShelterData() {
        List<TsunamiShelter> shelters = new ArrayList<>();

        // 성남시 지역 대피소 데이터 (실제 좌표 기반)
        shelters.add(createShelter(
                "성남시청",
                "경기도 성남시 분당구 공단로 67",
                37.3951,
                127.1074,
                "5000",
                150,
                "성남시",
                "032-1234-5678",
                "20230101"
        ));

        shelters.add(createShelter(
                "분당구청",
                "경기도 성남시 분당구 분당로 50",
                37.4009,
                127.1048,
                "3500",
                100,
                "분당구",
                "031-123-4567",
                "20230115"
        ));

        shelters.add(createShelter(
                "성남종합운동장",
                "경기도 성남시 중원구 봉영로 424",
                37.4419,
                127.1311,
                "8000",
                200,
                "중원구",
                "031-111-2222",
                "20230220"
        ));

        shelters.add(createShelter(
                "성남 문화센터",
                "경기도 성남시 분당구 정자일로 143",
                37.3899,
                127.1156,
                "4000",
                120,
                "분당구",
                "031-777-8888",
                "20230310"
        ));

        shelters.add(createShelter(
                "중원고등학교",
                "경기도 성남시 중원구 금광로 51",
                37.4357,
                127.1487,
                "6000",
                180,
                "중원구",
                "031-555-6666",
                "20230405"
        ));

        shelters.add(createShelter(
                "수정초등학교",
                "경기도 성남시 수정구 대왕판교로 645",
                37.4195,
                127.0865,
                "3000",
                90,
                "수정구",
                "031-444-5555",
                "20230510"
        ));

        shelters.add(createShelter(
                "야탑도서관",
                "경기도 성남시 분당구 야탑로 343",
                37.3750,
                127.1246,
                "2500",
                80,
                "분당구",
                "031-999-0000",
                "20230615"
        ));

        shelters.add(createShelter(
                "판교역 대피소",
                "경기도 성남시 분당구 판교역로 72",
                37.3945,
                127.1084,
                "4500",
                130,
                "분당구",
                "031-333-4444",
                "20230720"
        ));

        shelters.add(createShelter(
                "모란역 대피소",
                "경기도 성남시 중원구 성남대로 1237",
                37.4503,
                127.1078,
                "5500",
                160,
                "중원구",
                "031-666-7777",
                "20230825"
        ));

        shelters.add(createShelter(
                "성남종로청",
                "경기도 성남시 중원구 신필로 25",
                37.4228,
                127.1212,
                "3500",
                100,
                "중원구",
                "031-888-9999",
                "20230930"
        ));

        logger.info("더미 데이터 생성 완료: {} 개", shelters.size());
        return shelters;
    }

    /**
     * 대피소 객체 생성 헬퍼 메서드
     */
    private TsunamiShelter createShelter(String name, String address, double latitude, double longitude,
                                         String facilityArea, int capacity, String agency,
                                         String contactNumber, String designationDate) {
        TsunamiShelter shelter = new TsunamiShelter();
        shelter.setShelterName(name);
        shelter.setAddress(address);
        shelter.setLatitude(latitude);
        shelter.setLongitude(longitude);
        shelter.setFacilityArea(facilityArea);
        shelter.setAccommodationCapacity(capacity);
        shelter.setManagementAgency(agency);
        shelter.setContactNumber(contactNumber);
        shelter.setDesignationDate(designationDate);
        return shelter;
    }
}