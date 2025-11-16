// src/main/resources/static/js/app.js

/**
 * 지진해일 대피소 프론트 로직을 담당하는 클래스.
 *
 * 주요 역할:
 *  1) 카카오맵 초기화 및 사용자 위치 표시
 *  2) 백엔드 API(/admin/initialize, /api/nearest-shelters, /api/search) 호출
 *  3) 대피소 목록 렌더링 + 지도 마커 표시
 *  4) 목록 클릭 시: 해당 마커를 지도 중앙으로 이동시키고 인포윈도우 열기
 *  5) 마커 인포윈도우 안에서 "상세보기" / "카카오맵" 버튼 제공
 */
class ShelterApp {
    constructor() {
        // 카카오맵 객체
        this.map = null;
        // 사용자 위치 {lat, lng}
        this.userLocation = null;
        // 지도에 표시된 대피소 마커들
        this.markers = [];
        // 사용자 위치 마커
        this.userMarker = null;
        // 현재 화면에 표시 중인 대피소 목록 (목록, 상세 모달, 포커스에 사용)
        this.currentShelters = [];

        // 지도 초기화 + 이벤트 바인딩 + geolocation 체크
        this.initializeMap();
        this.bindEvents();
        this.checkGeolocationSupport();
    }

    // ============================
    // 지도 초기화
    // ============================
    initializeMap() {
        console.log('지도 초기화 시작');

        const container = document.getElementById('map');
        if (!container) {
            console.error('지도 컨테이너를 찾을 수 없습니다.');
            this.showAlert('지도 컨테이너를 찾을 수 없습니다.', 'error');
            return;
        }

        try {
            // 기본 중심: 서울 시청 근처
            const options = {
                center: new kakao.maps.LatLng(37.5665, 126.9780),
                level: 3
            };

            // 실제 카카오맵 객체 생성
            this.map = new kakao.maps.Map(container, options);
            console.log('카카오맵 생성 완료');

            // 지도 타입 컨트롤 (일반지도/스카이뷰 전환용)
            const mapTypeControl = new kakao.maps.MapTypeControl();
            this.map.addControl(mapTypeControl, kakao.maps.ControlPosition.TOPRIGHT);

            // 줌 컨트롤 (+/-)
            const zoomControl = new kakao.maps.ZoomControl();
            this.map.addControl(zoomControl, kakao.maps.ControlPosition.RIGHT);

            console.log('✅ 카카오맵 초기화 완료');
        } catch (error) {
            console.error('지도 초기화 중 오류:', error);
            this.showAlert('지도 초기화에 실패했습니다: ' + error.message, 'error');
        }
    }

    // ============================
    // DOM 이벤트 바인딩
    // ============================
    bindEvents() {
        // "내 위치 찾기" 버튼
        const locationBtn = document.getElementById('getCurrentLocationBtn');
        if (locationBtn) {
            locationBtn.addEventListener('click', () => this.getCurrentLocation());
        }

        // "데이터 초기화" 버튼 (공공데이터→DB 저장)
        const initBtn = document.getElementById('initDataBtn');
        if (initBtn) {
            initBtn.addEventListener('click', () => this.initializeData());
        }

        // "검색" 버튼
        const searchBtn = document.getElementById('searchBtn');
        if (searchBtn) {
            searchBtn.addEventListener('click', () => this.searchShelters());
        }

        // 검색창 Enter 키 이벤트
        const searchInput = document.getElementById('searchInput');
        if (searchInput) {
            searchInput.addEventListener('keypress', (e) => {
                if (e.key === 'Enter') {
                    this.searchShelters();
                }
            });
        }

        // 반경 변경 시 (현재 위치가 있을 때만 근처 대피소 재조회)
        const radiusSelect = document.getElementById('radiusSelect');
        if (radiusSelect) {
            radiusSelect.addEventListener('change', () => {
                if (this.userLocation) {
                    this.findNearestShelters();
                }
            });
        }

        // 개수 변경 시 (현재 위치가 있을 때만 근처 대피소 재조회)
        const limitSelect = document.getElementById('limitSelect');
        if (limitSelect) {
            limitSelect.addEventListener('change', () => {
                if (this.userLocation) {
                    this.findNearestShelters();
                }
            });
        }

        // 지도 타입 토글 버튼
        const toggleMapBtn = document.getElementById('toggleMapType');
        if (toggleMapBtn) {
            toggleMapBtn.addEventListener('click', () => this.toggleMapType());
        }

        // 상세 모달 닫기 버튼
        const closeBtn = document.querySelector('.close');
        if (closeBtn) {
            closeBtn.addEventListener('click', () => this.closeModal());
        }

        // 모달 영역 밖 클릭 시 모달 닫기
        const modal = document.getElementById('shelterModal');
        if (modal) {
            modal.addEventListener('click', (e) => {
                if (e.target.id === 'shelterModal') {
                    this.closeModal();
                }
            });
        }
    }

    // ============================
    // 브라우저 위치 서비스 지원 여부 체크
    // ============================
    checkGeolocationSupport() {
        if (!navigator.geolocation) {
            this.showAlert('이 브라우저는 위치 서비스를 지원하지 않습니다.', 'error');
        }
    }

    // ============================
    // 현재 위치 가져오기
    // ============================
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

                // 사용자 위치를 지도에 표시
                this.showUserLocationOnMap(lat, lng);
                // 현재 위치 기준으로 근처 대피소 조회
                this.findNearestShelters();
            },
            (error) => {
                let msg = '위치를 가져올 수 없습니다.';
                switch (error.code) {
                    case error.PERMISSION_DENIED:
                        msg = '위치 권한이 거부되었습니다. 브라우저 설정에서 위치 권한을 허용해주세요.';
                        break;
                    case error.POSITION_UNAVAILABLE:
                        msg = '위치 정보를 사용할 수 없습니다.';
                        break;
                    case error.TIMEOUT:
                        msg = '위치 요청 시간이 초과되었습니다.';
                        break;
                    default:
                        msg = '위치를 가져오는 중 알 수 없는 오류가 발생했습니다.';
                }
                if (statusEl) statusEl.textContent = msg;
                this.showAlert(msg, 'error');
            },
            options
        );
    }

    // ============================
    // 지도에 사용자 위치 표시
    // ============================
    showUserLocationOnMap(lat, lng) {
        if (!this.map || typeof kakao === 'undefined') {
            console.log('지도가 초기화되지 않음');
            return;
        }

        // 기존 사용자 마커 제거
        if (this.userMarker) {
            this.userMarker.setMap(null);
        }

        const position = new kakao.maps.LatLng(lat, lng);

        // 사용자 위치 마커 이미지
        const markerImage = new kakao.maps.MarkerImage(
            'https://t1.daumcdn.net/localimg/localimages/07/mapapidoc/marker_red.png',
            new kakao.maps.Size(30, 35)
        );

        this.userMarker = new kakao.maps.Marker({
            position,
            map: this.map,
            image: markerImage
        });

        // 지도 중심 사용자 위치로 이동
        this.map.setCenter(position);
        this.map.setLevel(3);

        // "현재 위치" 인포윈도우
        const infowindow = new kakao.maps.InfoWindow({
            content: '<div style="padding:5px;font-size:12px;">📍 현재 위치</div>'
        });
        infowindow.open(this.map, this.userMarker);
    }

    // ============================
    // 공공데이터 → DB 초기화 (관리자용)
    // ============================
    async initializeData() {
        this.showLoading(true);

        try {
            // 백엔드 /admin/initialize 호출 → ApiService가 실제 API 호출 후 DB 저장
            const response = await fetch('/admin/initialize', {
                method: 'POST'
            });

            const result = await response.text();
            this.showAlert(result, response.ok ? 'success' : 'error');

            // 초기화 성공 시, 헤더의 "대피소 개수" 갱신을 위해 새로고침
            if (response.ok) {
                setTimeout(() => location.reload(), 2000);
            }
        } catch (error) {
            console.error('Error:', error);
            this.showAlert('데이터 초기화 중 오류가 발생했습니다.', 'error');
        } finally {
            this.showLoading(false);
        }
    }

    // ============================
    // 현재 위치 기준 가까운 대피소 조회
    // ============================
    async findNearestShelters() {
        if (!this.userLocation) {
            this.showAlert('먼저 현재 위치를 설정해주세요.', 'warning');
            return;
        }

        this.showLoading(true);

        try {
            const limitSelect = document.getElementById('limitSelect');
            const limit = limitSelect ? limitSelect.value : '10';

            // /api/nearest-shelters 로 현재 위치 + limit 전송
            const response = await fetch('/api/nearest-shelters', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: `latitude=${this.userLocation.lat}&longitude=${this.userLocation.lng}&limit=${limit}`
            });

            if (!response.ok) {
                throw new Error('대피소 데이터를 가져오는데 실패했습니다.');
            }

            const shelters = await response.json();

            // 목록 + 지도 동시 갱신
            this.displayShelters(shelters);
            this.showSheltersOnMap(shelters);
        } catch (error) {
            console.error('Error:', error);
            this.showAlert(error.message, 'error');
        } finally {
            this.showLoading(false);
        }
    }

    // ============================
    // 검색 (주소 / 이름)
    // ============================
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
                this.showSheltersOnMap([]);
                return;
            }

            // 사용자 위치가 있으면 거리 계산 후 정렬
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

    // ============================
    // 대피소 목록 렌더링
    // ============================
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

        // ✅ 변경 포인트:
        //   - 예전: 목록 클릭 시 바로 상세 모달로 이동
        //   - 현재: 목록 클릭 시 해당 마커를 지도 중앙으로 이동 + 인포윈도우 열기
        listContainer.innerHTML = shelters.map((shelter, index) => `
            <div class="shelter-item" data-index="${index}" onclick="app.focusOnShelter(${index})">
                <div class="shelter-name">${this.escapeHtml(shelter.shelterName || '이름 없음')}</div>
                ${shelter.distanceFromUser
            ? `<div class="shelter-distance">📏 ${shelter.distanceFromUser.toFixed(2)}km</div>`
            : ''}
                <div class="shelter-address">📍 ${this.escapeHtml(shelter.address || '주소 정보 없음')}</div>
                <div class="shelter-info">
                    <div>👥 수용인원: ${shelter.accommodationCapacity || '정보없음'}명</div>
                    <div>📞 연락처: ${this.escapeHtml(shelter.contactNumber || '정보없음')}</div>
                </div>
            </div>
        `).join('');
    }

    // ============================
    // 지도에 대피소 마커 표시
    // ============================
    showSheltersOnMap(shelters) {
        if (!this.map || typeof kakao === 'undefined') {
            console.log('지도가 초기화되지 않음');
            return;
        }

        // 기존 마커/인포윈도우 제거
        this.markers.forEach(marker => {
            if (marker.infowindow) marker.infowindow.close();
            marker.setMap(null);
        });
        this.markers = [];

        if (!shelters || shelters.length === 0) return;

        const positions = [];

        shelters.forEach((shelter, index) => {
            if (!shelter.latitude || !shelter.longitude) return;

            const position = new kakao.maps.LatLng(shelter.latitude, shelter.longitude);
            positions.push(position);

            // 마커 생성
            const marker = new kakao.maps.Marker({
                position,
                map: this.map
            });

            // shelter에서 마커로, 마커에서 shelter 인덱스로 서로 연결해둔다.
            marker.shelterIndex = index;
            shelter.marker = marker;

            // 인포윈도우 내용 구성 (상세보기 + 카카오맵 버튼)
            const safeName = this.escapeHtml(shelter.shelterName || '대피소');
            const distanceText = shelter.distanceFromUser
                ? `거리: ${shelter.distanceFromUser.toFixed(2)}km<br>`
                : '';
            const capacityText = `수용인원: ${shelter.accommodationCapacity || '정보없음'}명`;

            const infowindow = new kakao.maps.InfoWindow({
                content: `
                    <div style="padding:8px;font-size:12px;min-width:180px;">
                        <strong>${safeName}</strong><br>
                        ${distanceText}
                        ${capacityText}
                        <div style="margin-top:8px; display:flex; gap:4px;">
                            <button type="button"
                                    class="btn-infowindow"
                                    onclick="app.showShelterDetail(${index})">
                                상세보기
                            </button>
                            <button type="button"
                                    class="btn-infowindow"
                                    onclick="app.openKakaoMap(${shelter.latitude}, ${shelter.longitude})">
                                카카오맵
                            </button>
                        </div>
                    </div>
                `
            });

            // 마커 클릭 시: 자기 인포윈도우만 열도록 처리
            kakao.maps.event.addListener(marker, 'click', () => {
                this.markers.forEach(m => {
                    if (m.infowindow) m.infowindow.close();
                });
                infowindow.open(this.map, marker);
            });

            marker.infowindow = infowindow;
            this.markers.push(marker);
        });

        // 모든 마커(+사용자 위치)가 화면에 들어오도록 bounds 조정
        if (positions.length > 0) {
            const bounds = new kakao.maps.LatLngBounds();
            positions.forEach(p => bounds.extend(p));

            if (this.userLocation) {
                bounds.extend(new kakao.maps.LatLng(this.userLocation.lat, this.userLocation.lng));
            }

            this.map.setBounds(bounds);
        }
    }

    // ============================
    // 목록 클릭 → 마커로 포커스
    // ============================
    focusOnShelter(index) {
        const shelter = this.currentShelters[index];
        if (!shelter) {
            this.showAlert('선택한 대피소 정보를 찾을 수 없습니다.', 'error');
            return;
        }

        if (!this.map || typeof kakao === 'undefined') {
            this.showAlert('지도가 아직 준비되지 않았습니다.', 'error');
            return;
        }

        let marker = shelter.marker;

        // 혹시 marker 연결이 없다면 좌표로 찾아보기 (예외 케이스용)
        if (!marker && shelter.latitude && shelter.longitude) {
            const targetLat = shelter.latitude;
            const targetLng = shelter.longitude;
            marker = this.markers.find(m => {
                const pos = m.getPosition();
                return Math.abs(pos.getLat() - targetLat) < 1e-6 &&
                    Math.abs(pos.getLng() - targetLng) < 1e-6;
            });
        }

        if (!marker) {
            this.showAlert('해당 대피소의 마커를 찾을 수 없습니다.', 'error');
            return;
        }

        const pos = marker.getPosition();
        this.map.setCenter(pos);
        this.map.setLevel(3);

        // 다른 인포윈도우 닫고, 해당 마커 인포윈도우 열기
        this.markers.forEach(m => {
            if (m.infowindow) m.infowindow.close();
        });
        if (marker.infowindow) {
            marker.infowindow.open(this.map, marker);
        }
    }

    // ============================
    // 상세 모달 표시 (상세페이지 역할)
    // ============================
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
                ${shelter.distanceFromUser
            ? `<p><strong>거리:</strong> ${shelter.distanceFromUser.toFixed(2)}km</p>`
            : ''}
                
                <h4 style="color: #2c3e50; margin: 20px 0 15px 0;">🏢 시설 정보</h4>
                <p><strong>수용 가능 인원:</strong> ${shelter.accommodationCapacity || '정보 없음'}명</p>
                <p><strong>시설 면적:</strong> ${this.escapeHtml(shelter.facilityArea || '정보 없음')}</p>
                
                <h4 style="color: #2c3e50; margin: 20px 0 15px 0;">📞 연락처 정보</h4>
                <p><strong>관리기관:</strong> ${this.escapeHtml(shelter.managementAgency || '정보 없음')}</p>
                <p><strong>연락처:</strong> ${this.escapeHtml(shelter.contactNumber || '정보 없음')}</p>
                <p><strong>지정일자:</strong> ${this.escapeHtml(shelter.designationDate || '정보 없음')}</p>
                
                ${(shelter.latitude && shelter.longitude) ? `
                <div style="margin-top: 20px;">
                    <button onclick="app.showDirections(${shelter.latitude}, ${shelter.longitude})" 
                            class="btn btn-primary" style="width: 100%;">
                        🗺️ 길찾기 (카카오맵)
                    </button>
                </div>
                ` : ''}
            </div>
        `;

        modal.style.display = 'flex';
    }

    // ============================
    // 카카오맵 길찾기 / 이동
    // ============================
    showDirections(lat, lng) {
        if (this.userLocation) {
            // 현재 위치 → 대피소 길찾기
            const url = `https://map.kakao.com/link/to/대피소,${lat},${lng}/from/현재위치,${this.userLocation.lat},${this.userLocation.lng}`;
            window.open(url, '_blank');
        } else {
            // 현재 위치 모를 때: 대피소 위치만 띄우기
            const url = `https://map.kakao.com/link/map/대피소,${lat},${lng}`;
            window.open(url, '_blank');
        }
    }

    // 인포윈도우 "카카오맵" 버튼에서 사용 (내부적으로 showDirections 재사용)
    openKakaoMap(lat, lng) {
        this.showDirections(lat, lng);
    }

    // 모달 닫기
    closeModal() {
        const modal = document.getElementById('shelterModal');
        if (modal) {
            modal.style.display = 'none';
        }
    }

    // 지도 타입 변경 (일반 <-> 스카이뷰)
    toggleMapType() {
        if (!this.map || typeof kakao === 'undefined') return;

        try {
            const mapTypes = [kakao.maps.MapTypeId.ROADMAP, kakao.maps.MapTypeId.SKYVIEW];
            const currentType = this.map.getMapTypeId();
            const newType = currentType === mapTypes[0] ? mapTypes[1] : mapTypes[0];
            this.map.setMapTypeId(newType);
        } catch (error) {
            console.error('지도 타입 변경 중 오류:', error);
        }
    }

    // 거리 계산 (Haversine)
    calculateDistance(lat1, lng1, lat2, lng2) {
        const R = 6371; // km
        const dLat = this.toRad(lat2 - lat1);
        const dLng = this.toRad(lng2 - lng1);
        const a =
            Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(this.toRad(lat1)) *
            Math.cos(this.toRad(lat2)) *
            Math.sin(dLng / 2) *
            Math.sin(dLng / 2);
        const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    toRad(deg) {
        return deg * (Math.PI / 180);
    }

    // XSS 방지를 위한 HTML 이스케이프
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

    // 상단 알림 (토스트 느낌)
    showAlert(message, type = 'info') {
        const existing = document.querySelector('.alert');
        if (existing && existing.parentNode) {
            existing.remove();
        }

        const alert = document.createElement('div');
        alert.className = `alert alert-${type}`;
        alert.textContent = message;

        const container = document.querySelector('.container');
        if (container) {
            container.insertBefore(alert, container.firstChild);

            setTimeout(() => {
                if (alert && alert.parentNode) {
                    alert.remove();
                }
            }, 3000);
        }
    }
}

// ⚠ 여기에서는 new ShelterApp() 을 생성하지 않는다.
//   → index.html 에서 kakao.maps.load(...) 콜백 안에서
//      window.app = new ShelterApp(); 로 생성하도록 되어 있음.
