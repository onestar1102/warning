// src/main/resources/static/js/app.js
class ShelterApp {
    constructor() {
        this.map = null;
        this.userLocation = null;
        this.markers = [];
        this.userMarker = null;
        this.currentShelters = [];

        this.init();
    }


    init() {
        // 카카오맵 API가 이미 로드된 상태에서 호출되므로 바로 초기화
        this.initializeMap();
        this.bindEvents();
        this.checkGeolocationSupport();
    }


    // 카카오맵 초기화
    // 카카오맵 초기화
    initializeMap() {
        console.log('지도 초기화 시작');

        const container = document.getElementById('map');
        if (!container) {
            console.error('지도 컨테이너를 찾을 수 없습니다.');
            this.showAlert('지도 컨테이너를 찾을 수 없습니다.', 'error');
            return;
        }

        try {
            console.log('카카오맵 객체 생성 중...');

            const options = {
                center: new kakao.maps.LatLng(37.5665, 126.9780), // 서울시청
                level: 3
            };

            this.map = new kakao.maps.Map(container, options);
            console.log('카카오맵 생성 완료');

            // 지도 타입 컨트롤러 추가
            const mapTypeControl = new kakao.maps.MapTypeControl();
            this.map.addControl(mapTypeControl, kakao.maps.ControlPosition.TOPRIGHT);

            // 줌 컨트롤러 추가
            const zoomControl = new kakao.maps.ZoomControl();
            this.map.addControl(zoomControl, kakao.maps.ControlPosition.RIGHT);

            console.log('✅ 카카오맵 초기화 완료');
        } catch (error) {
            console.error('지도 초기화 중 오류:', error);
            this.showAlert('지도 초기화에 실패했습니다: ' + error.message, 'error');
        }
    }

    // 이벤트 바인딩
    bindEvents() {
        // 내 위치 찾기 버튼
        const locationBtn = document.getElementById('getCurrentLocationBtn');
        if (locationBtn) {
            locationBtn.addEventListener('click', () => {
                this.getCurrentLocation();
            });
        }

        // 데이터 초기화 버튼
        const initBtn = document.getElementById('initDataBtn');
        if (initBtn) {
            initBtn.addEventListener('click', () => {
                this.initializeData();
            });
        }

        // 검색 버튼
        const searchBtn = document.getElementById('searchBtn');
        if (searchBtn) {
            searchBtn.addEventListener('click', () => {
                this.searchShelters();
            });
        }

        // 검색 입력란에서 엔터키
        const searchInput = document.getElementById('searchInput');
        if (searchInput) {
            searchInput.addEventListener('keypress', (e) => {
                if (e.key === 'Enter') {
                    this.searchShelters();
                }
            });
        }

        // 반경 변경
        const radiusSelect = document.getElementById('radiusSelect');
        if (radiusSelect) {
            radiusSelect.addEventListener('change', () => {
                if (this.userLocation) {
                    this.findNearestShelters();
                }
            });
        }

        // 표시 개수 변경
        const limitSelect = document.getElementById('limitSelect');
        if (limitSelect) {
            limitSelect.addEventListener('change', () => {
                if (this.userLocation) {
                    this.findNearestShelters();
                }
            });
        }

        // 지도 타입 변경 버튼
        const toggleMapBtn = document.getElementById('toggleMapType');
        if (toggleMapBtn) {
            toggleMapBtn.addEventListener('click', () => {
                this.toggleMapType();
            });
        }

        // 모달 닫기
        const closeBtn = document.querySelector('.close');
        if (closeBtn) {
            closeBtn.addEventListener('click', () => {
                this.closeModal();
            });
        }

        // 모달 외부 클릭시 닫기
        const modal = document.getElementById('shelterModal');
        if (modal) {
            modal.addEventListener('click', (e) => {
                if (e.target.id === 'shelterModal') {
                    this.closeModal();
                }
            });
        }
    }

    // 지오로케이션 지원 확인
    checkGeolocationSupport() {
        if (!navigator.geolocation) {
            this.showAlert('이 브라우저는 위치 서비스를 지원하지 않습니다.', 'error');
        }
    }

    // 현재 위치 가져오기
    getCurrentLocation() {
        const statusEl = document.getElementById('locationStatus');
        if (statusEl) {
            statusEl.textContent = '위치를 가져오는 중...';
        }

        if (!navigator.geolocation) {
            this.showAlert('이 브라우저는 위치 서비스를 지원하지 않습니다.', 'error');
            return;
        }

        const options = {
            enableHighAccuracy: true,
            timeout: 10000,
            maximumAge: 0
        };

        navigator.geolocation.getCurrentPosition(
            (position) => {
                const lat = position.coords.latitude;
                const lng = position.coords.longitude;

                this.userLocation = { lat, lng };
                if (statusEl) {
                    statusEl.textContent = `현재 위치: ${lat.toFixed(6)}, ${lng.toFixed(6)}`;
                }

                this.showUserLocationOnMap(lat, lng);
                this.findNearestShelters();
            },
            (error) => {
                let errorMsg = '위치를 가져올 수 없습니다.';
                switch (error.code) {
                    case error.PERMISSION_DENIED:
                        errorMsg = '위치 권한이 거부되었습니다. 브라우저 설정에서 위치 권한을 허용해주세요.';
                        break;
                    case error.POSITION_UNAVAILABLE:
                        errorMsg = '위치 정보를 사용할 수 없습니다.';
                        break;
                    case error.TIMEOUT:
                        errorMsg = '위치 요청 시간이 초과되었습니다.';
                        break;
                    default:
                        errorMsg = '위치를 가져오는 중 알 수 없는 오류가 발생했습니다.';
                        break;
                }
                if (statusEl) {
                    statusEl.textContent = errorMsg;
                }
                this.showAlert(errorMsg, 'error');
            },
            options
        );
    }

    // 지도에 사용자 위치 표시
    showUserLocationOnMap(lat, lng) {
        if (!this.map || typeof kakao === 'undefined') {
            console.log('지도가 초기화되지 않음');
            return;
        }

        // 기존 사용자 마커 제거
        if (this.userMarker) {
            this.userMarker.setMap(null);
        }

        try {
            const position = new kakao.maps.LatLng(lat, lng);

            // 사용자 위치 마커 생성
            const markerImage = new kakao.maps.MarkerImage(
                'https://t1.daumcdn.net/localimg/localimages/07/mapapidoc/marker_red.png',
                new kakao.maps.Size(30, 35)
            );

            this.userMarker = new kakao.maps.Marker({
                position: position,
                map: this.map,
                image: markerImage
            });

            // 지도 중심을 사용자 위치로 이동
            this.map.setCenter(position);
            this.map.setLevel(3);

            // 사용자 위치 정보창
            const infowindow = new kakao.maps.InfoWindow({
                content: '<div style="padding:5px;font-size:12px;">📍 현재 위치</div>'
            });
            infowindow.open(this.map, this.userMarker);
        } catch (error) {
            console.error('사용자 위치 표시 중 오류:', error);
        }
    }

    // 데이터 초기화
    async initializeData() {
        this.showLoading(true);

        try {
            const response = await fetch('/admin/initialize', {
                method: 'POST'
            });

            const result = await response.text();
            this.showAlert(result, response.ok ? 'success' : 'error');

            if (response.ok) {
                // 페이지 새로고침하여 업데이트된 데이터 개수 표시
                setTimeout(() => {
                    location.reload();
                }, 2000);
            }

        } catch (error) {
            console.error('Error:', error);
            this.showAlert('데이터 초기화 중 오류가 발생했습니다.', 'error');
        } finally {
            this.showLoading(false);
        }
    }

    // 가장 가까운 대피소 찾기
    async findNearestShelters() {
        if (!this.userLocation) {
            this.showAlert('먼저 현재 위치를 설정해주세요.', 'warning');
            return;
        }

        this.showLoading(true);

        try {
            const limitSelect = document.getElementById('limitSelect');
            const limit = limitSelect ? limitSelect.value : '10';

            const response = await fetch('/api/nearest-shelters', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: `latitude=${this.userLocation.lat}&longitude=${this.userLocation.lng}&limit=${limit}`
            });

            if (!response.ok) {
                throw new Error('대피소 데이터를 가져오는데 실패했습니다.');
            }

            const shelters = await response.json();
            this.displayShelters(shelters);
            this.showSheltersOnMap(shelters);

        } catch (error) {
            console.error('Error:', error);
            this.showAlert(error.message, 'error');
        } finally {
            this.showLoading(false);
        }
    }

    // 대피소 검색
    async searchShelters() {
        const searchType = document.getElementById('searchType');
        const searchInput = document.getElementById('searchInput');

        if (!searchType || !searchInput) {
            this.showAlert('검색 요소를 찾을 수 없습니다.', 'error');
            return;
        }

        const keyword = searchInput.value.trim();
        if (!keyword) {
            this.showAlert('검색어를 입력해주세요.', 'warning');
            return;
        }

        this.showLoading(true);

        try {
            const response = await fetch(`/api/search?type=${searchType.value}&keyword=${encodeURIComponent(keyword)}`);

            if (!response.ok) {
                throw new Error('검색에 실패했습니다.');
            }

            const shelters = await response.json();

            if (shelters.length === 0) {
                this.showAlert('검색 결과가 없습니다.', 'warning');
                this.displayShelters([]);
                return;
            }

            // 사용자 위치가 있으면 거리 계산
            if (this.userLocation) {
                shelters.forEach(shelter => {
                    shelter.distanceFromUser = this.calculateDistance(
                        this.userLocation.lat, this.userLocation.lng,
                        shelter.latitude, shelter.longitude
                    );
                });
                shelters.sort((a, b) => a.distanceFromUser - b.distanceFromUser);
            }

            this.displayShelters(shelters);
            this.showSheltersOnMap(shelters);

        } catch (error) {
            console.error('Error:', error);
            this.showAlert(error.message, 'error');
        } finally {
            this.showLoading(false);
        }
    }

    // 대피소 목록 표시
    displayShelters(shelters) {
        this.currentShelters = shelters;
        const listContainer = document.getElementById('shelterList');
        const countEl = document.getElementById('resultsCount');

        if (!listContainer) return;

        if (countEl) {
            countEl.textContent = `${shelters.length}개`;
        }

        if (shelters.length === 0) {
            listContainer.innerHTML = '<div class="no-results">검색 결과가 없습니다.</div>';
            return;
        }

        listContainer.innerHTML = shelters.map((shelter, index) => `
            <div class="shelter-item" data-index="${index}" onclick="app.showShelterDetail(${index})">
                <div class="shelter-name">${this.escapeHtml(shelter.shelterName || '이름 없음')}</div>
                ${shelter.distanceFromUser ?
            `<div class="shelter-distance">📏 ${shelter.distanceFromUser.toFixed(2)}km</div>` :
            ''
        }
                <div class="shelter-address">📍 ${this.escapeHtml(shelter.address || '주소 정보 없음')}</div>
                <div class="shelter-info">
                    <div>👥 수용인원: ${shelter.accommodationCapacity || '정보없음'}명</div>
                    <div>📞 연락처: ${this.escapeHtml(shelter.contactNumber || '정보없음')}</div>
                </div>
            </div>
        `).join('');
    }

    // 지도에 대피소 마커 표시
    showSheltersOnMap(shelters) {
        if (!this.map || typeof kakao === 'undefined') {
            console.log('지도가 초기화되지 않음');
            return;
        }

        // 기존 마커 제거
        this.markers.forEach(marker => {
            if (marker.infowindow) {
                marker.infowindow.close();
            }
            marker.setMap(null);
        });
        this.markers = [];

        if (shelters.length === 0) return;

        try {
            // 마커를 표시할 위치들을 담을 배열
            const positions = [];

            // 새 마커 추가
            shelters.forEach((shelter, index) => {
                if (shelter.latitude && shelter.longitude) {
                    const position = new kakao.maps.LatLng(shelter.latitude, shelter.longitude);
                    positions.push(position);

                    const marker = new kakao.maps.Marker({
                        position: position,
                        map: this.map
                    });

                    const infowindow = new kakao.maps.InfoWindow({
                        content: `
                            <div style="padding:8px;font-size:12px;min-width:150px;">
                                <strong>${this.escapeHtml(shelter.shelterName || '대피소')}</strong><br>
                                ${shelter.distanceFromUser ?
                            `거리: ${shelter.distanceFromUser.toFixed(2)}km<br>` :
                            ''
                        }
                                수용인원: ${shelter.accommodationCapacity || '정보없음'}명
                            </div>
                        `
                    });

                    // 마커 클릭시 정보창 표시
                    kakao.maps.event.addListener(marker, 'click', () => {
                        // 다른 정보창들 모두 닫기
                        this.markers.forEach(m => {
                            if (m.infowindow) m.infowindow.close();
                        });
                        infowindow.open(this.map, marker);
                    });

                    // 마커에 정보창 참조 저장
                    marker.infowindow = infowindow;
                    this.markers.push(marker);
                }
            });

            // 모든 마커가 보이도록 지도 범위 조정
            if (positions.length > 0) {
                const bounds = new kakao.maps.LatLngBounds();
                positions.forEach(position => bounds.extend(position));

                // 사용자 위치도 포함
                if (this.userLocation) {
                    bounds.extend(new kakao.maps.LatLng(this.userLocation.lat, this.userLocation.lng));
                }

                this.map.setBounds(bounds);
            }
        } catch (error) {
            console.error('마커 표시 중 오류:', error);
        }
    }

    // 대피소 상세 정보 표시
    showShelterDetail(index) {
        const shelter = this.currentShelters[index];
        if (!shelter) return;

        const modal = document.getElementById('shelterModal');
        const title = document.getElementById('modalTitle');
        const content = document.getElementById('modalContent');

        if (!modal || !title || !content) return;

        title.textContent = shelter.shelterName || '대피소 정보';

        content.innerHTML = `
            <div style="line-height: 1.6;">
                <h4 style="color: #2c3e50; margin-bottom: 15px;">📍 기본 정보</h4>
                <p><strong>대피소명:</strong> ${this.escapeHtml(shelter.shelterName || '정보 없음')}</p>
                <p><strong>주소:</strong> ${this.escapeHtml(shelter.address || '정보 없음')}</p>
                ${shelter.distanceFromUser ?
            `<p><strong>거리:</strong> ${shelter.distanceFromUser.toFixed(2)}km</p>` :
            ''
        }
                
                <h4 style="color: #2c3e50; margin: 20px 0 15px 0;">🏢 시설 정보</h4>
                <p><strong>수용 가능 인원:</strong> ${shelter.accommodationCapacity || '정보 없음'}명</p>
                <p><strong>시설 면적:</strong> ${this.escapeHtml(shelter.facilityArea || '정보 없음')}</p>
                
                <h4 style="color: #2c3e50; margin: 20px 0 15px 0;">📞 연락처 정보</h4>
                <p><strong>관리기관:</strong> ${this.escapeHtml(shelter.managementAgency || '정보 없음')}</p>
                <p><strong>연락처:</strong> ${this.escapeHtml(shelter.contactNumber || '정보 없음')}</p>
                <p><strong>지정일자:</strong> ${this.escapeHtml(shelter.designationDate || '정보 없음')}</p>
                
                ${shelter.latitude && shelter.longitude ? `
                <div style="margin-top: 20px;">
                    <button onclick="app.showDirections(${shelter.latitude}, ${shelter.longitude})" 
                            class="btn btn-primary" style="width: 100%;">
                        🗺️ 길찾기
                    </button>
                </div>
                ` : ''}
            </div>
        `;

        modal.style.display = 'flex';
    }

    // 길찾기 (외부 지도 앱 연동)
    showDirections(lat, lng) {
        if (this.userLocation) {
            // 카카오맵 길찾기 URL
            const url = `https://map.kakao.com/link/to/대피소,${lat},${lng}/from/현재위치,${this.userLocation.lat},${this.userLocation.lng}`;
            window.open(url, '_blank');
        } else {
            // 현재 위치 없으면 해당 위치만 표시
            const url = `https://map.kakao.com/link/map/대피소,${lat},${lng}`;
            window.open(url, '_blank');
        }
    }

    // 모달 닫기
    closeModal() {
        const modal = document.getElementById('shelterModal');
        if (modal) {
            modal.style.display = 'none';
        }
    }

    // 지도 타입 변경
    toggleMapType() {
        if (!this.map || typeof kakao === 'undefined') {
            console.log('지도가 초기화되지 않음');
            return;
        }

        try {
            const mapTypes = [kakao.maps.MapTypeId.ROADMAP, kakao.maps.MapTypeId.SKYVIEW];
            const currentType = this.map.getMapTypeId();
            const newType = currentType === mapTypes[0] ? mapTypes[1] : mapTypes[0];
            this.map.setMapTypeId(newType);
        } catch (error) {
            console.error('지도 타입 변경 중 오류:', error);
        }
    }

    // 거리 계산 (Haversine 공식)
    calculateDistance(lat1, lng1, lat2, lng2) {
        const R = 6371; // 지구 반지름 (km)
        const dLat = this.toRad(lat2 - lat1);
        const dLng = this.toRad(lng2 - lng1);
        const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(this.toRad(lat1)) * Math.cos(this.toRad(lat2)) *
            Math.sin(dLng / 2) * Math.sin(dLng / 2);
        const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    toRad(deg) {
        return deg * (Math.PI / 180);
    }

    // HTML 이스케이프 처리
    escapeHtml(text) {
        if (!text) return '';
        const map = {
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            '"': '&quot;',
            "'": '&#039;'
        };
        return text.toString().replace(/[&<>"']/g, (m) => map[m]);
    }

    // 로딩 스피너 표시/숨김
    showLoading(show) {
        const spinner = document.getElementById('loadingSpinner');
        if (spinner) {
            spinner.style.display = show ? 'flex' : 'none';
        }
    }

    // 알림 메시지 표시
    showAlert(message, type = 'info') {
        // 기존 알림 제거
        const existingAlert = document.querySelector('.alert');
        if (existingAlert && existingAlert.parentNode) {
            existingAlert.remove();
        }

        // 새 알림 생성
        const alert = document.createElement('div');
        alert.className = `alert alert-${type}`;
        alert.textContent = message;

        // 컨테이너 상단에 추가
        const container = document.querySelector('.container');
        if (container) {
            container.insertBefore(alert, container.firstChild);

            // 3초 후 자동 제거
            setTimeout(() => {
                if (alert && alert.parentNode) {
                    alert.remove();
                }
            }, 3000);
        }
    }
}

// 애플리케이션 시작
let app;

// DOM 로드 완료 후 앱 시작
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', function() {
        app = new ShelterApp();
    });
} else {
    app = new ShelterApp();
}