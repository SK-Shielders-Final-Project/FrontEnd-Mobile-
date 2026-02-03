cmake_minimum_required(VERSION 3.22.1)

# 프로젝트 이름 설정
project("mobile")

# 네이티브 라이브러리 추가
add_library(
        mobile              # 라이브러리 이름
        SHARED
        native-lib.cpp      # 원본 프로젝트의 기존 소스
        integrity_check.cpp # 무결성 검사 소스
)

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