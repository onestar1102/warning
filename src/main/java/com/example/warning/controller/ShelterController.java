// src/main/java/com/example/shelter/controller/ShelterController.java
package com.example.warning.controller;

import com.example.warning.model.TsunamiShelter;
import com.example.warning.service.ShelterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class ShelterController {

    private static final Logger logger = LoggerFactory.getLogger(ShelterController.class);

    @Autowired
    private ShelterService shelterService;

    /**
     * 메인 페이지
     */
    @GetMapping("/")
    public String index(Model model) {
        long shelterCount = shelterService.getShelterCount();
        model.addAttribute("shelterCount", shelterCount);
        return "index";
    }

    /**
     * 데이터 초기화 (관리자용)
     */
    @PostMapping("/admin/initialize")
    @ResponseBody
    public String initializeData() {
        try {
            shelterService.initializeShelterData();
            return "데이터 초기화가 완료되었습니다.";
        } catch (Exception e) {
            logger.error("데이터 초기화 중 오류 발생", e);
            return "데이터 초기화 중 오류가 발생했습니다: " + e.getMessage();
        }
    }

    /**
     * 가장 가까운 대피소 찾기 (AJAX)
     */
    @PostMapping("/api/nearest-shelters")
    @ResponseBody
    public List<TsunamiShelter> findNearestShelters(
            @RequestParam("latitude") double latitude,
            @RequestParam("longitude") double longitude,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {

        logger.info("가장 가까운 대피소 요청: lat={}, lng={}, limit={}", latitude, longitude, limit);

        return shelterService.findNearestShelters(latitude, longitude, limit);
    }

    /**
     * 반경 내 대피소 찾기 (AJAX)
     */
    @PostMapping("/api/shelters-in-radius")
    @ResponseBody
    public List<TsunamiShelter> findSheltersInRadius(
            @RequestParam("latitude") double latitude,
            @RequestParam("longitude") double longitude,
            @RequestParam("radius") double radius) {

        logger.info("반경 내 대피소 요청: lat={}, lng={}, radius={}km", latitude, longitude, radius);

        return shelterService.findSheltersWithinRadius(latitude, longitude, radius);
    }

    /**
     * 대피소 검색 (AJAX)
     */
    @GetMapping("/api/search")
    @ResponseBody
    public List<TsunamiShelter> searchShelters(
            @RequestParam("type") String type,
            @RequestParam("keyword") String keyword) {

        logger.info("대피소 검색: type={}, keyword={}", type, keyword);

        if ("address".equals(type)) {
            return shelterService.searchByAddress(keyword);
        } else if ("name".equals(type)) {
            return shelterService.searchByShelterName(keyword);
        } else {
            return List.of(); // 빈 리스트 반환
        }
    }

    /**
     * 전체 대피소 목록
     */
    @GetMapping("/shelters")
    public String allShelters(Model model) {
        List<TsunamiShelter> shelters = shelterService.getAllShelters();
        model.addAttribute("shelters", shelters);
        return "shelters";
    }

    /**
     * 대피소 상세 정보 (AJAX)
     */
    @GetMapping("/api/shelter/{id}")
    @ResponseBody
    public TsunamiShelter getShelterDetail(@PathVariable Long id) {
        return shelterService.getAllShelters().stream()
                .filter(shelter -> shelter.getId().equals(id))
                .findFirst()
                .orElse(null);
    }
}