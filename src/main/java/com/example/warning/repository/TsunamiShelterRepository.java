// src/main/java/com/example/shelter/repository/TsunamiShelterRepository.java
package com.example.warning.repository;

import com.example.warning.model.TsunamiShelter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TsunamiShelterRepository extends JpaRepository<TsunamiShelter, Long> {

    // 위도, 경도 범위로 대피소 검색 (성능 최적화를 위한 사전 필터링)
    @Query("SELECT s FROM TsunamiShelter s WHERE " +
            "s.latitude BETWEEN :minLat AND :maxLat AND " +
            "s.longitude BETWEEN :minLng AND :maxLng")
    List<TsunamiShelter> findSheltersInRange(
            @Param("minLat") Double minLat,
            @Param("maxLat") Double maxLat,
            @Param("minLng") Double minLng,
            @Param("maxLng") Double maxLng
    );

    // 지역명으로 검색
    List<TsunamiShelter> findByAddressContaining(String address);

    // 대피소명으로 검색
    List<TsunamiShelter> findByShelterNameContaining(String shelterName);
}