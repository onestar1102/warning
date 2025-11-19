package com.example.warning.service;

import com.example.warning.dto.DsspResponse;
import com.example.warning.model.TsunamiShelter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * safetydata DSSP-IF-10944 API 파싱 + TsunamiShelter 변환 서비스
 */
@Service
public class ApiService {

    private static final Logger logger = LoggerFactory.getLogger(ApiService.class);

    private final String serviceKey;
    private final String endpoint;
    private final WebClient webClient;

    public ApiService(
            @Value("${api.data.go.kr.base-url}") String baseUrl,
            @Value("${api.data.go.kr.service-key}") String serviceKey,
            @Value("${api.data.go.kr.endpoint}") String endpoint
    ) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
        this.serviceKey = serviceKey;
        this.endpoint = endpoint;
    }

    /**
     * DSSP-IF-10944 전체 페이지를 돌면서
     * 모든 대피소 데이터를 TsunamiShelter 리스트로 변환해서 반환.
     */
    public List<TsunamiShelter> fetchAllShelterData() {

        List<TsunamiShelter> result = new ArrayList<>();

        int pageNo = 1;
        int numOfRows = 10; // 1페이지당 개수
        int totalCount = -1;

        while (true) {
            try {
                int finalPageNo = pageNo;
                DsspResponse response = webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path(endpoint)
                                .queryParam("serviceKey", serviceKey)
                                .queryParam("pageNo", finalPageNo)
                                .queryParam("numOfRows", numOfRows)
                                .build()
                        )
                        .retrieve()
                        .bodyToMono(DsspResponse.class)
                        .block();

                if (response == null || response.getBody() == null) {
                    logger.warn("DSSP 응답이 null 또는 body가 비어있음 (pageNo={})", pageNo);
                    break;
                }

                if (totalCount == -1) {
                    totalCount = response.getTotalCount();
                    logger.info("총 개수: {}", totalCount);
                }

                // 여기에서 this::convertToEntity 를 씀
                List<TsunamiShelter> pageList = response.getBody().stream()
                        .map(this::convertToEntity)
                        .collect(Collectors.toList());

                result.addAll(pageList);

                logger.info("{}페이지 처리: {}개 추가됨 (현재 총 {}개)",
                        pageNo, pageList.size(), result.size());

                if (result.size() >= totalCount) {
                    break;
                }

                pageNo++;

            } catch (Exception e) {
                logger.error("페이지 {} 조회 중 오류 발생", pageNo, e);
                break;
            }
        }

        logger.info("최종 모은 데이터 개수 = {}", result.size());
        return result;
    }

    /**
     * DSSP 응답(DsspItem) → DB 엔티티(TsunamiShelter)로 변환
     */
    private TsunamiShelter convertToEntity(DsspResponse.DsspItem item) {

        TsunamiShelter shelter = new TsunamiShelter();

        // DsspItem 필드 → TsunamiShelter 필드 매핑
        shelter.setShelterName(item.getShelterName());              // SHNT_PLACE_NM
        shelter.setAddress(item.getAddress());                      // SHNT_PLACE_DTL_POSITION
        shelter.setLatitude(item.getLatitude());                    // LA
        shelter.setLongitude(item.getLongitude());                  // LO
        shelter.setAccommodationCapacity(item.getCapacity());       // PSBL_NMPR

        // API에서 안 주는 값은 일단 임시 값으로 채워둠
        shelter.setManagementAgency("N/A");
        shelter.setContactNumber("N/A");
        shelter.setFacilityArea("N/A");
        shelter.setDesignationDate("N/A");

        return shelter;
    }
}
