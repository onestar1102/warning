// src/main/java/com/example/shelter/util/DistanceCalculator.java
package com.example.warning.util;

public class DistanceCalculator {

    private static final double EARTH_RADIUS = 6371.0; // 지구 반지름 (km)

    /**
     * 두 지점 간의 거리를 계산 (Haversine 공식 사용)
     * @param lat1 첫 번째 지점의 위도
     * @param lng1 첫 번째 지점의 경도
     * @param lat2 두 번째 지점의 위도
     * @param lng2 두 번째 지점의 경도
     * @return 거리 (km)
     */
    public static double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c;
    }

    /**
     * 검색 범위 계산 (위도, 경도 범위)
     * @param centerLat 중심 위도
     * @param centerLng 중심 경도
     * @param radiusKm 검색 반경 (km)
     * @return [minLat, maxLat, minLng, maxLng]
     */
    public static double[] calculateSearchBounds(double centerLat, double centerLng, double radiusKm) {
        double latDiff = radiusKm / 111.0; // 위도 1도 ≈ 111km
        double lngDiff = radiusKm / (111.0 * Math.cos(Math.toRadians(centerLat))); // 경도는 위도에 따라 변함

        return new double[] {
                centerLat - latDiff,  // minLat
                centerLat + latDiff,  // maxLat
                centerLng - lngDiff,  // minLng
                centerLng + lngDiff   // maxLng
        };
    }
}