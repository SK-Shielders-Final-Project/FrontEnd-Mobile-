# 🚲 [SK 쉴더스 루키즈] 공유 모빌리티 모의해킹 프로젝트

## 1. 프로젝트 개요
* **주제**: 전동 킥보드/전기차 '공유 모빌리티' 서비스 네이티브 안드로이드 앱 개발 및 보안 진단
* **기간**: 2026.01.20 ~ (총 10주 과정)
* **목표**: 서비스 개발, 동작 구현, 해킹 보고서 작성 및 최종 발표

---

## 2. 개발 및 테스트 환경 (Environment)
팀원 간 빌드 에러 방지 및 원활한 모의해킹 실습을 위해 아래 설정을 반드시 준수해 주세요.

### 🛠 Mobile Tech Stack
* **언어(Language)**: Java (Empty Views Activity 템플릿 사용)
* **JDK 버전**: JetBrains Runtime 21.0.8 (JDK 21)
* **Android SDK 설정**:
    * `compileSdk`: **36** (최신 라이브러리 빌드 호환용)
    * `minSdk`: **24** (Android 7.0 Nougat) — **보안 진단 및 패킷 분석 최적화 버전**
    * `targetSdk`: **34**
* **라이브러리**: Retrofit2 (Network), Gson (JSON Parsing)

### 💻 Backend Reference (Web Team)
* **Framework**: Spring Boot 3.5.9
* **Database**: Oracle Database 21c XE (User: `zdme` / PW: `zdme`)
* **ORM**: Spring Data JPA

---

## 3. 상세 팀원 역할 분담 (Role & Mission)
모든 팀원이 앱 개발 입문자임을 고려하여 아래와 같이 업무를 분담합니다.

| 담당 팀원 | 담당 앱 기능 (Function) | 보안 진단 및 구현 방법 |
| :--- | :--- | :--- |
| **대한** | 프로젝트 환경 세팅 및 총괄 | 루팅 탐지(Root Detection) 로직 구현, NDK($C++/.so$) 활용 API Key 은닉 |
| **민지** | 주행 및 위치 서비스 | File Storage 기록 저장 취약점 재현, GPS 위치 전송 및 주변 기기 검색 |
| **석주** | 결제 및 대여 시스템 | 결제/이용권 로직, QR 대여(Qshing) 취약점 시나리오, 딥링크 친구 초대 |
| **승진** | 사용자 관리 및 소통 | 로그인/회원가입(SharedPrefs 취약점), 챗봇, 공지사항 관리 |

---

## 4. 프로젝트 구조 (Project Structure)
역할별로 주로 작업하게 될 소스 코드 경로입니다.

```text


app/
├── manifests/
│   └── AndroidManifest.xml      # [공통] 앱 권한(인터넷, GPS) 및 액티비티 등록
├── java/com.example.mobile/
│   ├── network/                 # [ㅅㅈ ㅈ] Retrofit 인터페이스 및 API 통신 관련
│   ├── ui/                      # [민지, LuckJu_, ㅅㅈ ㅈ] 각 기능별 자바 로직 (Activity 등)
│   ├── security/                # [대한] 루팅 탐지 및 NDK 관련 자바 코드
│   └── util/                    # [공통] 공통 유틸리티 함수
├── cpp/                         # [대한] C++ 소스 파일 및 CMake 설정 (보안 로직)
└── res/
    ├── layout/                  # [민지, LuckJu_, ㅅㅈ ㅈ] 모든 화면 디자인 (XML)
    └── drawable/                # [공통] 앱 아이콘 및 이미지 리소스

5. 보안 진단 항목

* 루팅 탐지 및 우회 (Rooting Detection)

* 중요 데이터 평문 저장 및 전송 (Insecure Data Storage)

* 네트워크 패킷 가로채기 및 변조 (MITM)

* QR 코드 위변조(Qshing) 및 딥링크 취약점 점검
