// src/main/java/com/example/warning/controller/ShelterController.java
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
import java.util.Optional;

/**
 * 지진해일 긴급대피장소(해일 대피소) 관련 컨트롤러.
 *
 * 역할을 크게 3가지로 나눈다.
 *  1) 화면(View) 반환: 메인 페이지 등
 *  2) 관리자용 초기화 API: 공공데이터 → DB 저장
 *  3) 프론트 JS에서 호출하는 AJAX 데이터 API
 *
 * 지금은 JS(app.js)에서 다음 엔드포인트들을 사용한다.
 *  - POST /admin/initialize
 *  - POST /api/nearest-shelters
 *  - GET  /api/search
 *
 * 그 외 엔드포인트(/shelters, /api/shelter/{id}, /api/shelters-in-radius)는
 *  나중에 쓸 수 있도록 코드 안에 남겨두되, 현재는 주석 처리해서 비활성화한다.
 */
@Controller
public class ShelterController {

    private static final Logger logger = LoggerFactory.getLogger(ShelterController.class);

    @Autowired
    private ShelterService shelterService;

    // =========================================
    // 1. 화면(View) 관련 엔드포인트
    // =========================================

    /**
     * 메인 페이지
     *
     * - DB에 저장된 대피소 개수를 조회해서 모델에 넣는다.
     * - templates/index.html 에서 ${shelterCount}로 사용 가능.
     */
    @GetMapping("/")
    public String index(Model model) {
        long shelterCount = shelterService.getShelterCount();
        model.addAttribute("shelterCount", shelterCount);
        return "index";
    }

    /**
     * 단순 테스트용 텍스트 응답
     * - 브라우저에서 /test 로 접근해서 서버와 DB가 정상 동작하는지 확인할 수 있다.
     */
    @GetMapping("/test")
    @ResponseBody
    public String test() {
        return "대피소 개수: " + shelterService.getShelterCount();
    }

    /*
     * [현재 미사용] 전체 대피소 목록 페이지
     *
     * - 서버에서 렌더링하는 목록 페이지(/shelters)가 필요할 때 사용할 수 있다.
     * - 지금 UI 흐름은 index 페이지 + JS 로만 동작하므로, 엔드포인트를 비활성화하고 코드만 남겨둔다.
     */
    /*
    @GetMapping("/shelters")
    public String allShelters(Model model) {
        List<TsunamiShelter> shelters = shelterService.getAllShelters();
        model.addAttribute("shelters", shelters);
        return "shelters";
    }
    */

    // =========================================
    // 2. 관리자용 초기화 API
    // =========================================

    /**
     * 데이터 초기화 (관리자용)
     *
     * - 프론트 JS의 initializeData()에서 /admin/initialize 로 POST 요청을 보낸다.
     * - 동작 순서:
     *    1) ShelterService.initializeShelterData() 호출
     *       - 기존 DB 대피소 삭제
     *       - 공공데이터포털 API 호출 → 최신 데이터 가져와 DB 저장
     *    2) 서비스에서 반환한 메시지를 그대로 클라이언트에게 반환
     */
    @PostMapping("/admin/initialize")
    @ResponseBody
    public String initializeData() {
        try {
            String message = shelterService.initializeShelterData();
            return message;
        } catch (Exception e) {
            logger.error("데이터 초기화 중 오류 발생", e);
            return "데이터 초기화 중 오류가 발생했습니다: " + e.getMessage();
        }
    }

    // =========================================
    // 3. 프론트에서 사용하는 AJAX API
    // =========================================

    /**
     * 가장 가까운 대피소 찾기 (AJAX)
     *
     * - 프론트 JS:
     *    fetch('/api/nearest-shelters', { method: 'POST', body: latitude, longitude, limit ... })
     *
     * - 동작:
     *    1) 요청으로 받은 위도/경도 기준으로
     *       ShelterService.findNearestShelters(...) 호출
     *    2) 서비스에서 거리 계산 및 정렬까지 끝낸 List<TsunamiShelter> 를 그대로 JSON으로 반환
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
     * 대피소 검색 (AJAX)
     *
     * - 프론트 JS:
     *    GET /api/search?type=address&keyword=...
     *    GET /api/search?type=name&keyword=...
     *
     * - 동작:
     *    1) type, keyword를 그대로 ShelterService.search(type, keyword)에 넘김
     *    2) 서비스 내부에서 type 값(address/name)에 따라
     *       Repository 검색 메서드(주소 포함 검색, 이름 포함 검색) 호출
     */
    @GetMapping("/api/search")
    @ResponseBody
    public List<TsunamiShelter> searchShelters(
            @RequestParam("type") String type,
            @RequestParam("keyword") String keyword) {

        logger.info("대피소 검색: type={}, keyword={}", type, keyword);

        return shelterService.search(type, keyword);
    }

    // =========================================
    // 4. 지금은 안 쓰지만, 나중에 쓸 수 있는 API들 (주석으로 보존)
    // =========================================

    /*
     * [현재 미사용] 반경 내 대피소 찾기 (AJAX)
     *
     * - radius(반경 km) 파라미터를 받아 해당 거리 이내의 대피소만 필터링해서 반환하는 용도.
     * - 현재 프론트에서는 사용하지 않지만,
     *   "내 주변 3km 이내 대피소만 보기" 같은 기능이 필요해지면 살려서 사용하면 된다.
     */
    /*
    @PostMapping("/api/shelters-in-radius")
    @ResponseBody
    public List<TsunamiShelter> findSheltersInRadius(
            @RequestParam("latitude") double latitude,
            @RequestParam("longitude") double longitude,
            @RequestParam("radius") double radius) {

        logger.info("반경 내 대피소 요청: lat={}, lng={}, radius={}km", latitude, longitude, radius);

        return shelterService.findSheltersWithinRadius(latitude, longitude, radius);
    }
    */

    /*
     * [현재 미사용] 대피소 단건 상세 정보 (AJAX)
     *
     * - ID로 단건 조회해서 JSON으로 내려주는 API.
     * - 지금은 JS에서 currentShelters[index]로 이미 모든 정보를 가지고 있어서
     *   별도 API 호출 없이 상세 모달을 만들 수 있기 때문에 사용하지 않는다.
     * - 향후 "URL로 바로 상세 페이지 접근" 같은 기능이 필요하면 다시 활성화해서 사용.
     */
    /*
    @GetMapping("/api/shelter/{id}")
    @ResponseBody
    public TsunamiShelter getShelterDetail(@PathVariable Long id) {
        Optional<TsunamiShelter> shelterOpt = shelterService.getShelterById(id);
        return shelterOpt.orElse(null);
    }
    */
}
