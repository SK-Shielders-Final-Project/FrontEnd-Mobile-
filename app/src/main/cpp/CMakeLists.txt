cmake_minimum_required(VERSION 3.22.1)

# 프로젝트 이름 설정
project("mobile")

# 네이티브 라이브러리 추가
add_library(
        mobile              # 라이브러리 이름
        SHARED
        native-lib.cpp      # 원본 프로젝트의 기존 소스
        integrity_check.cpp # 무결성 검사 소스
        anti_frida.cpp
)

# -----------------------------------------------------------
# [보안/개발 패치] Android 15+ 호환성을 위한 16KB 페이지 정렬 강제 설정
# 설명: 링커(Linker)에게 바이너리를 16KB(16384 bytes) 경계로 정렬하라고 지시합니다.
# -----------------------------------------------------------
target_link_options(mobile PRIVATE "-Wl,-z,max-page-size=16384")

# 로그 출력을 위한 라이브러리 연결
find_library(
        log-lib
        log
)

# 모든 라이브러리 연결
target_link_libraries(
        mobile
        ${log-lib}
)