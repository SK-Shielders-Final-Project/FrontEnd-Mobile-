# CMake 최소 요구 버전 설정
cmake_minimum_required(VERSION 3.18.1)

# 프로젝트 이름 설정
project("MobileProject")

# --- 기존 네이티브 라이브러리 (mobile) ---
# 앱의 핵심 네이티브 기능을 담당합니다.
add_library(mobile SHARED
    native-lib.cpp
    anti_debug.cpp
    ksu_detector.cpp
    integrity_check.cpp)

# --- 제로클릭 시나리오를 위한 신규 취약 라이브러리 ---
add_library(image_processor_lib SHARED image_processor_lib.cpp)


# --- 공통 라이브러리 링크 ---
# Android 로깅 라이브러리(log-lib)를 찾아 링크합니다.
find_library(log-lib log)

# 각 라이브러리에 로깅 라이브러리를 링크합니다.
target_link_libraries(mobile ${log-lib})
target_link_libraries(image_processor_lib ${log-lib})
