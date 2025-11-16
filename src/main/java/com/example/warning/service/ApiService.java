// src/main/java/com/example/shelter/service/ApiService.java
package com.example.warning.service;

import com.example.warning.dto.ApiResponse;
import com.example.warning.model.TsunamiShelter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 공공데이터포털 "지진해일 긴급대피장소" API와 연동하는 서비스.
 *
 * - 기존에는 더미 데이터를 List<TsunamiShelter>로 직접 만들어서 돌려줬지만,
 *   이제 실제 공공데이터 API를 호출해서 실데이터를 가져오도록 변경했다.
 *
 * - 이 서비스에서 하는 일:
 *   1) WebClient로 공공데이터포털 REST API 호출
 *   2) JSON 응답을 ApiResponse DTO로 매핑
 *   3) ApiResponse.ShelterItem → TsunamiShelter 엔티티로 변환
 *   4) 변환된 엔티티 리스트를 반환 (ShelterService가 DB 저장에 사용)
 */
@Service
public class ApiService {

    private static final Logger logger = LoggerFactory.getLogger(ApiService.class);

    // 공공데이터포털 API 인증키 (application.properties에서 주입)
    private final String serviceKey;

    // 요청할 엔드포인트 path (예: /getTsunamiShelter3List)
    private final String endpoint;

    // 실제 HTTP 요청을 담당하는 WebClient
    private final WebClient webClient;

    /**
     * 생성자에서 WebClient와 설정 값들을 주입받아 초기화한다.
     *
     * @param baseUrl   공공데이터포털 API 기본 URL
     * @param serviceKey API 인증키
     * @param endpoint  엔드포인트 path
     */
    public ApiService(
            @Value("${api.data.go.kr.base-url}") String baseUrl,
            @Value("${api.data.go.kr.service-key}") String serviceKey,
            @Value("${api.data.go.kr.endpoint}") String endpoint
    ) {
        // baseUrl을 기반으로 WebClient 생성
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
        this.serviceKey = serviceKey;
        this.endpoint = endpoint;
    }

    /**
     * 공공데이터포털 API에서 특정 페이지(pageNo)와 numOfRows에 해당하는
     * 지진해일 대피소 데이터를 조회한다.
     *
     * @param pageNo    페이지 번호
     * @param numOfRows 한 페이지당 결과 개수
     * @return TsunamiShelter 엔티티 리스트 (실제 대피소 데이터)
     */
    public List<TsunamiShelter> fetchShelterData(int pageNo, int numOfRows) {
        logger.info("지진해일 대피소 API 호출 시작: pageNo={}, numOfRows={}", pageNo, numOfRows);

        try {
            // WebClient를 이용한 GET 요청
            ApiResponse apiResponse = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(endpoint)
                            // 공공데이터포털 인증키
                            .queryParam("ServiceKey", serviceKey)
                            // JSON 응답을 받기 위한 type 지정 (기본이 xml인 경우가 많음)
                            .queryParam("type", "json")
                            .queryParam("pageNo", pageNo)
                            .queryParam("numOfRows", numOfRows)
                            .build()
                    )
                    .retrieve()
                    // 응답 바디를 ApiResponse DTO로 매핑
                    .bodyToMono(ApiResponse.class)
                    // block(): 비동기 Mono를 동기 방식으로 기다려 결과 받기
                    .block();

            if (apiResponse == null || apiResponse.getResponse() == null) {
                logger.warn("API 응답이 비어있습니다.");
                return Collections.emptyList();
            }

            ApiResponse.Response response = apiResponse.getResponse();
            ApiResponse.Header header = response.getHeader();

            // header에는 resultCode / resultMsg 등이 들어있음 (정상 호출 여부 확인용)
            if (header != null) {
                logger.info("API Header - resultCode={}, resultMsg={}",
                        header.getResultCode(), header.getResultMsg());
            }

            ApiResponse.Body body = response.getBody();
            if (body == null || body.getItems() == null) {
                logger.warn("API 응답 body 또는 items가 비어있습니다.");
                return Collections.emptyList();
            }

            // 실제 대피소 리스트
            List<ApiResponse.ShelterItem> items =
                    Optional.ofNullable(body.getItems().getItem())
                            .orElse(Collections.emptyList());

            // ShelterItem → TsunamiShelter 변환
            List<TsunamiShelter> shelters = items.stream()
                    .map(this::convertToEntity)
                    .collect(Collectors.toList());

            logger.info("API에서 변환된 대피소 개수: {}", shelters.size());
            return shelters;

        } catch (WebClientResponseException e) {
            // HTTP 상태 코드 4xx, 5xx 에러 처리
            logger.error("지진해일 대피소 API 호출 실패 - status={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString(), e);
            return Collections.emptyList();
        } catch (Exception e) {
            // 그 외 네트워크 오류, 파싱 오류 등
            logger.error("지진해일 대피소 API 호출 중 예기치 못한 오류 발생", e);
            return Collections.emptyList();
        }
    }

    /**
     * "전체" 대피소 데이터를 한 번에 받아오는 메서드.
     *
     * 간단하게 1페이지에 1000개 요청해서 가져오도록 구현했다.
     * (필요하면 body.getTotalCount()를 이용해 여러 페이지를 반복 호출하는 방식으로 확장 가능)
     */
    public List<TsunamiShelter> fetchAllShelterData() {
        logger.info("전체 지진해일 대피소 데이터 조회 시작");
        return fetchShelterData(1, 1000);
    }

    /**
     * 공공데이터포털 API 응답 아이템(ShelterItem)을
     * DB 엔티티(TsunamiShelter)로 변환하는 메서드.
     *
     * - 숫자 타입(위도/경도, 수용인원)은 문자열로 내려오기 때문에
     *   안전하게 parseDouble / parseInteger로 변환한다.
     * - 변환 실패 시 해당 필드는 null 처리하고 전체 엔티티 생성은 계속 진행한다.
     */
    private TsunamiShelter convertToEntity(ApiResponse.ShelterItem item) {
        TsunamiShelter shelter = new TsunamiShelter();

        // 대피소명
        shelter.setShelterName(item.getShelterName());
        // 주소(상세 주소)
        shelter.setAddress(item.getAddress());

        // 위도/경도 (문자열 → Double)
        shelter.setLatitude(parseDouble(item.getLatitude()));
        shelter.setLongitude(parseDouble(item.getLongitude()));

        // 시설 면적 (문자열 그대로 저장)
        shelter.setFacilityArea(item.getFacilityArea());

        // 수용 가능 인원 (문자열 → Integer)
        shelter.setAccommodationCapacity(parseInteger(item.getAccommodationCapacity()));

        // 관리 기관명
        shelter.setManagementAgency(item.getManagementAgency());

        // 연락처
        shelter.setContactNumber(item.getContactNumber());

        // 지정일자 (YYYYMMDD)
        shelter.setDesignationDate(item.getDesignationDate());

        return shelter;
    }

    /**
     * 문자열을 Integer로 안전하게 파싱하는 유틸 메서드.
     * - null, 빈 문자열, 공백 문자열이면 null 반환
     * - 숫자로 변환할 수 없으면 경고 로그만 남기고 null 반환
     */
    private Integer parseInteger(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            logger.warn("정수 파싱 실패: value='{}'", value);
            return null;
        }
    }

    /**
     * 문자열을 Double로 안전하게 파싱하는 유틸 메서드.
     * - null, 빈 문자열, 공백 문자열이면 null 반환
     * - 숫자로 변환할 수 없으면 경고 로그만 남기고 null 반환
     */
    private Double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            logger.warn("실수 파싱 실패: value='{}'", value);
            return null;
        }
    }
}
