# SRF Connect

주변의 BLE 기기를 스캔하고 연결하여 Service와 Characteristic을 파악하고 데이터 상호작용(Read, Write, Notify)을 수행하는 간단한 Android 앱입니다.

---

## 화면 구성

### ScanScreen
- 주변 BLE 기기 스캔
- 발견된 기기 목록을 디바이스 이름 + MAC 주소로 표시
- 기기 탭 시 연결 시작

### ConnectingScreen
- 기기 연결 중 로딩 화면

### DeviceScreen
- 연결된 기기의 모든 Characteristic을 개별 카드로 표시
- 각 카드에서 지원하는 기능(READ / WRITE / NOTIFY)에 따라 UI 표시
  - **READ**: Value 표시 + Read 버튼
  - **WRITE**: 텍스트 입력 + Send 버튼
  - **NOTIFY**: Enable/Disable 토글 + 최근 수신 로그 (최대 5개 표시, 20개 보관)

---

## 기술 스택

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material3
- **Architecture**: MVVM (AndroidViewModel + StateFlow)
- **BLE**: Android Bluetooth LE API

---

## 권한

| 권한 | 용도 |
|------|------|
| `BLUETOOTH_SCAN` | BLE 기기 스캔 (Android 12+) |
| `BLUETOOTH_CONNECT` | BLE 기기 연결 (Android 12+) |
| `ACCESS_FINE_LOCATION` | BLE 스캔 결과 수신 |
| `BLUETOOTH` / `BLUETOOTH_ADMIN` | BLE 제어 (Android 11 이하) |

---

## 주요 동작

- 스캔 모드: `SCAN_MODE_LOW_LATENCY` (빠른 기기 발견)
- 연결 방식: `TRANSPORT_LE` 강제 지정
- 데이터 수신: Android 13 이상/미만 콜백 이중 처리
- 중복 Service UUID 대응: `getService()` 대신 전체 서비스 목록에서 Characteristic 탐색

---

<img width="250"  alt="image" src="https://github.com/user-attachments/assets/c5b63f5b-1802-413b-9ac3-cad90ed7e4f0" />
<img width="250" alt="image" src="https://github.com/user-attachments/assets/a0b34057-e079-422f-908b-53b3e1cce870" />

---
### 영상
https://github.com/user-attachments/assets/30f225dc-ad40-4b5e-a22e-7e5912b9fffb


