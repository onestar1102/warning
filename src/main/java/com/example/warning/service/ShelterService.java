// src/main/java/com/example/shelter/service/ShelterService.java
package com.example.warning.service;

import com.example.warning.model.TsunamiShelter;
import com.example.warning.repository.TsunamiShelterRepository;
import com.example.warning.util.DistanceCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ShelterService {

    private static final Logger logger = LoggerFactory.getLogger(ShelterService.class);

    @Autowired
    private TsunamiShelterRepository shelterRepository;

    @Autowired
    private ApiService apiService;

    /**
     * 데이터베이스에 대피소 데이터 초기화
     */
    public void initializeShelterData() {
        logger.info("대피소 데이터 초기화 시작");

        // 기존 데이터 삭제
        shelterRepository.deleteAll();

        // API에서 데이터 가져와서 저장
        List<TsunamiShelter> shelters = apiService.fetchAllShelterData();

        if (!shelters.isEmpty()) {
            shelterRepository.saveAll(shelters);
            logger.info("대피소 데이터 저장 완료: {} 건", shelters.size());
        } else {
            logger.warn("저장할 대피소 데이터가 없습니다.");
        }
    }

    /**
     * 사용자 위치에서 가장 가까운 대피소 찾기
     */
    public List<TsunamiShelter> findNearestShelters(double userLat, double userLng, int limit) {
        logger.info("가장 가까운 대피소 검색: 위도={}, 경도={}, 제한={}", userLat, userLng, limit);

        // 검색 범위 설정 (반경 50km)
        double[] bounds = DistanceCalculator.calculateSearchBounds(userLat, userLng, 50.0);

        // 범위 내 대피소 조회
        List<TsunamiShelter> shelters = shelterRepository.findSheltersInRange(
                bounds[0], bounds[1], bounds[2], bounds[3]
        );

        // 거리 계산 및 정렬
        return shelters.stream()
                .peek(shelter -> {
                    double distance = DistanceCalculator.calculateDistance(
                            userLat, userLng,
                            shelter.getLatitude(), shelter.getLongitude()
                    );
                    shelter.setDistanceFromUser(distance);
                })
                .sorted(Comparator.comparing(TsunamiShelter::getDistanceFromUser))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 전체 대피소 목록 조회
     */
    public List<TsunamiShelter> getAllShelters() {
        return shelterRepository.findAll();
    }

    /**
     * 주소로 대피소 검색
     */
    public List<TsunamiShelter> searchByAddress(String address) {
        return shelterRepository.findByAddressContaining(address);
    }

    /**
     * 대피소명으로 검색
     */
    public List<TsunamiShelter> searchByShelterName(String name) {
        return shelterRepository.findByShelterNameContaining(name);
    }

    /**
     * 데이터베이스에 저장된 대피소 개수 확인
     */
    public long getShelterCount() {
        return shelterRepository.count();
    }

    /**
     * 특정 반경 내의 대피소 찾기
     */
    public List<TsunamiShelter> findSheltersWithinRadius(double userLat, double userLng, double radiusKm) {
        double[] bounds = DistanceCalculator.calculateSearchBounds(userLat, userLng, radiusKm);

        List<TsunamiShelter> shelters = shelterRepository.findSheltersInRange(
                bounds[0], bounds[1], bounds[2], bounds[3]
        );

        return shelters.stream()
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



}