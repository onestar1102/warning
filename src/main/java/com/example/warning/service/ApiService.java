// src/main/java/com/example/shelter/service/ApiService.java
package com.example.warning.service;

import com.example.warning.dto.ApiResponse;
import com.example.warning.model.TsunamiShelter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;

import java.util.ArrayList;
import java.util.List;

@Service
public class ApiService {

    private static final Logger logger = LoggerFactory.getLogger(ApiService.class);

    @Value("${api.data.go.kr.service-key}")
    private String serviceKey;

    @Value("${api.data.go.kr.base-url}")
    private String baseUrl;

    @Value("${api.data.go.kr.endpoint}")
    private String endpoint;

    private final WebClient webClient;

    public ApiService() {
        this.webClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB
                .build();
    }

    /**
     * 공공데이터포털에서 대피소 데이터 가져오기
     */
    public List<TsunamiShelter> fetchShelterData(int pageNo, int numOfRows) {
        try {
            String fullUrl = baseUrl + endpoint;

            logger.info("API 호출 시작: {}", fullUrl);

            ApiResponse response = webClient.get()
                    .uri(fullUrl, uriBuilder -> uriBuilder
                            .queryParam("serviceKey", serviceKey)
                            .queryParam("pageNo", pageNo)
                            .queryParam("numOfRows", numOfRows)
                            .queryParam("type", "json")
                            .build())
                    .retrieve()
                    .bodyToMono(ApiResponse.class)
                    .block();

            if (response != null && response.getResponse() != null) {
                logger.info("API 호출 성공. 응답 코드: {}",
                        response.getResponse().getHeader().getResultCode());

                return convertToShelterList(response);
            } else {
                logger.warn("API 응답이 null입니다.");
                return new ArrayList<>();
            }

        } catch (WebClientException e) {
            logger.error("API 호출 중 오류 발생: {}", e.getMessage());
            return new ArrayList<>();
        } catch (Exception e) {
            logger.error("예상치 못한 오류 발생: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * API 응답을 TsunamiShelter 엔티티 리스트로 변환
     */
    private List<TsunamiShelter> convertToShelterList(ApiResponse response) {
        List<TsunamiShelter> shelters = new ArrayList<>();

        if (response.getResponse().getBody() != null &&
                response.getResponse().getBody().getItems() != null &&
                response.getResponse().getBody().getItems().getItem() != null) {

            for (ApiResponse.ShelterItem item : response.getResponse().getBody().getItems().getItem()) {
                TsunamiShelter shelter = new TsunamiShelter();

                shelter.setShelterName(item.getShelterName());
                shelter.setAddress(item.getAddress());
                shelter.setManagementAgency(item.getManagementAgency());
                shelter.setContactNumber(item.getContactNumber());
                shelter.setDesignationDate(item.getDesignationDate());
                shelter.setFacilityArea(item.getFacilityArea());

                // 문자열을 숫자로 변환
                try {
                    if (item.getLatitude() != null && !item.getLatitude().isEmpty()) {
                        shelter.setLatitude(Double.parseDouble(item.getLatitude()));
                    }
                    if (item.getLongitude() != null && !item.getLongitude().isEmpty()) {
                        shelter.setLongitude(Double.parseDouble(item.getLongitude()));
                    }
                    if (item.getAccommodationCapacity() != null && !item.getAccommodationCapacity().isEmpty()) {
                        shelter.setAccommodationCapacity(Integer.parseInt(item.getAccommodationCapacity()));
                    }
                } catch (NumberFormatException e) {
                    logger.warn("숫자 변환 오류: {}", e.getMessage());
                }

                // 위도, 경도가 유효한 경우만 추가
                if (shelter.getLatitude() != null && shelter.getLongitude() != null &&
                        shelter.getLatitude() != 0.0 && shelter.getLongitude() != 0.0) {
                    shelters.add(shelter);
                }
            }
        }

        logger.info("변환된 대피소 개수: {}", shelters.size());
        return shelters;
    }

    /**
     * 전체 데이터 가져오기 (페이징 처리)
     */
    public List<TsunamiShelter> fetchAllShelterData() {
        List<TsunamiShelter> allShelters = new ArrayList<>();
        int pageNo = 1;
        int numOfRows = 1000; // 한 번에 가져올 데이터 수

        while (true) {
            List<TsunamiShelter> shelters = fetchShelterData(pageNo, numOfRows);

            if (shelters.isEmpty()) {
                break; // 더 이상 데이터가 없으면 종료
            }

            allShelters.addAll(shelters);

            if (shelters.size() < numOfRows) {
                break; // 마지막 페이지인 경우 종료
            }

            pageNo++;

            // API 호출 제한을 위한 딜레이
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        logger.info("전체 데이터 로드 완료. 총 개수: {}", allShelters.size());
        return allShelters;
    }
}