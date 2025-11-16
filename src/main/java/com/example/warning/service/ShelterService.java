// src/main/java/com/example/shelter/service/ShelterService.java
package com.example.warning.service;

import com.example.warning.model.TsunamiShelter;
import com.example.warning.repository.TsunamiShelterRepository;
import com.example.warning.util.DistanceCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 지진해일 대피소 비즈니스 로직을 담당하는 서비스 클래스.
 *
 * 역할:
 *  1) 공공데이터포털 API 연동을 통해 대피소 데이터를 DB에 초기화 (ApiService 사용)
 *  2) 현재 위치 기준 가까운 대피소 조회
 *  3) 주소/이름으로 대피소 검색
 *
 * ※ 지금 실제로 사용하는 메서드만 활성화하고,
 *    컨트롤러에서 주석 처리해둔 기능과 연결되는 메서드는 여기서도 주석 처리해서
 *    "나중에 쓸 수 있도록 코드만 보존"하는 방식으로 구성했다.
 */
@Service
@Transactional(readOnly = true)
public class ShelterService {

    private static final Logger logger = LoggerFactory.getLogger(ShelterService.class);

    private final TsunamiShelterRepository shelterRepository;
    private final ApiService apiService;

    // 생성자 주입 (권장 방식)
    public ShelterService(TsunamiShelterRepository shelterRepository, ApiService apiService) {
        this.shelterRepository = shelterRepository;
        this.apiService = apiService;
    }

    // =========================================================
    // 0. 공통 유틸 성격 메서드
    // =========================================================

    /**
     * DB에 저장된 대피소 전체 개수 조회
     * - 메인 페이지 상단에 "총 N개" 표시용.
     */
    public long getShelterCount() {
        return shelterRepository.count();
    }

    // =========================================================
    // 1. 공공데이터 → DB 초기화 (관리자용)
    // =========================================================

    /**
     * 공공데이터포털 API를 호출하여 DB를 최신 데이터로 초기화한다.
     *
     * 동작 순서:
     *  1) 기존 DB 데이터 모두 삭제
     *  2) ApiService.fetchAllShelterData() 호출 → 실제 공공데이터 API에서 모든 대피소 조회
     *  3) 불러온 TsunamiShelter 리스트를 DB에 저장
     *
     * @return 초기화 결과 메시지 (화면에 그대로 보여주기 위함)
     */
    @Transactional
    public String initializeShelterData() {
        logger.info("==== 지진해일 대피소 데이터 초기화 시작 ====");

        // 1) 기존 데이터 전체 삭제
        shelterRepository.deleteAll();
        logger.info("기존 대피소 데이터 전체 삭제 완료");

        // 2) 공공데이터 API에서 전체 대피소 데이터 조회
        List<TsunamiShelter> shelters = apiService.fetchAllShelterData();
        logger.info("API로부터 가져온 대피소 개수: {}", shelters.size());

        if (shelters.isEmpty()) {
            logger.warn("API에서 가져온 대피소 데이터가 없습니다.");
            return "초기화 실패: API에서 가져온 대피소 데이터가 없습니다.";
        }

        // 3) DB에 저장
        shelterRepository.saveAll(shelters);
        logger.info("DB에 대피소 데이터 저장 완료");

        return "초기화 완료: 총 " + shelters.size() + "개의 대피소 데이터를 불러왔습니다.";
    }

    // =========================================================
    // 2. 현재 위치 기준 가장 가까운 대피소 목록
    // =========================================================

    /**
     * 사용자 현재 위치 기준으로 가장 가까운 대피소들을 조회한다.
     *
     * 동작 방식:
     *  1) DB에서 모든 대피소를 조회
     *  2) 각 대피소에 대해 DistanceCalculator.calculateDistance 로 거리 계산
     *  3) 엔티티의 transient 필드(distanceFromUser)에 거리 저장
     *  4) 거리 기준 오름차순으로 정렬 후 limit 개수만 반환
     */
    public List<TsunamiShelter> findNearestShelters(double userLat, double userLng, int limit) {
        logger.info("가장 가까운 대피소 조회: lat={}, lng={}, limit={}", userLat, userLng, limit);

        List<TsunamiShelter> allShelters = shelterRepository.findAll();

        // 위도/경도가 있는 데이터만 대상으로 거리 계산
        allShelters.forEach(shelter -> {
            if (shelter.getLatitude() != null && shelter.getLongitude() != null) {
                double distance = DistanceCalculator.calculateDistance(
                        userLat, userLng,
                        shelter.getLatitude(), shelter.getLongitude()
                );
                shelter.setDistanceFromUser(distance);
            } else {
                // 좌표가 없는 경우 거리를 null 로 두고 정렬에서 제외
                shelter.setDistanceFromUser(null);
            }
        });

        // 거리값이 있는 것만 필터링해서 가까운 순으로 정렬 → limit 개수까지 자르기
        return allShelters.stream()
                .filter(s -> s.getDistanceFromUser() != null)
                .sorted(Comparator.comparing(TsunamiShelter::getDistanceFromUser))
                .limit(limit)
                .collect(Collectors.toList());
    }

    // =========================================================
    // 3. 검색 (주소 / 이름)
    // =========================================================

    /**
     * 주소 또는 이름 기준으로 대피소 검색.
     *
     * @param type    "address" 또는 "name"
     * @param keyword 검색어
     * @return 검색 결과 리스트
     */
    public List<TsunamiShelter> search(String type, String keyword) {
        logger.info("대피소 검색 요청: type={}, keyword={}", type, keyword);

        if ("address".equalsIgnoreCase(type)) {
            // 주소에 keyword 가 포함되는 대피소 검색
            return shelterRepository.findByAddressContaining(keyword);
        } else if ("name".equalsIgnoreCase(type)) {
            // 대피소명에 keyword 가 포함되는 대피소 검색
            return shelterRepository.findByShelterNameContaining(keyword);
        } else {
            logger.warn("알 수 없는 검색 타입: {}", type);
            return List.of();
        }
    }

    // =========================================================
    // 4. 지금은 안 쓰지만 나중에 다시 쓸 수 있는 메서드들 (주석으로 보존)
    // =========================================================

    /*
     * [현재 미사용] 반경 내 대피소 검색
     *
     * - 컨트롤러의 /api/shelters-in-radius 엔드포인트와 함께 사용.
     * - DistanceCalculator.calculateSearchBounds 로 사각형 범위를 구한 후,
     *   Repository.findSheltersInRange(...) 로 1차 필터링,
     *   이후 실제 거리 계산으로 반경 안에 들어오는 대피소만 다시 필터링하는 구조.
     */
    /*
    public List<TsunamiShelter> findSheltersWithinRadius(double userLat, double userLng, double radiusKm) {
        logger.info("반경 내 대피소 조회: lat={}, lng={}, radius={}km", userLat, userLng, radiusKm);

        // 1차: 위도/경도 범위로 후보 군 추리기 (DB 레벨에서 필터링)
        double[] bounds = DistanceCalculator.calculateSearchBounds(userLat, userLng, radiusKm);
        List<TsunamiShelter> candidates = shelterRepository.findSheltersInRange(
                bounds[0], bounds[1], // minLat, maxLat
                bounds[2], bounds[3]  // minLng, maxLng
        );

        // 2차: 실제 거리 계산해서 radiusKm 이내만 필터링
        return candidates.stream()
                .peek(shelter -> {
                    double distance = DistanceCalculator.calculateDistance(
                            userLat, userLng,
                            shelter.getLatitude(), shelter.getLongitude()
                    );
                    shelter.setDistanceFromUser(distance);
                })
                .filter(shelter -> shelter.getDistanceFromUser() <= radiusKm)
                .sorted(Comparator.comparing(TsunamiShelter::getDistanceFromUser))
                .collect(Collectors.toList());
    }
    */

    /*
     * [현재 미사용] 전체 대피소 목록 조회
     *
     * - 서버 렌더링 페이지(/shelters)에서 사용하려고 만들었던 메서드.
     * - 컨트롤러의 /shelters 엔드포인트가 주석 처리되어 있어서 같이 비활성화.
     */
    /*
    public List<TsunamiShelter> getAllShelters() {
        return shelterRepository.findAll();
    }
    */

    /*
     * [현재 미사용] ID로 단건 대피소 조회
     *
     * - 컨트롤러의 /api/shelter/{id} 엔드포인트와 함께 사용.
     * - 추후 "URL로 바로 상세 페이지 접근" 기능을 만들 때 다시 사용 가능.
     */
    /*
    public Optional<TsunamiShelter> getShelterById(Long id) {
        return shelterRepository.findById(id);
    }
    */
}
