cmake_minimum_required(VERSION 3.22.1)

# 프로젝트 이름 설정
project("mobile")

# 네이티브 라이브러리 추가
add_library(
        mobile # 라이브러리 이름 (System.loadLibrary("mobile")과 일치)
        SHARED
        native-lib.cpp # 소스 파일
)

# 로그 출력을 위한 라이브러리 연결 (디버깅용)
find_library(
        log-lib
        log
)

target_link_libraries(
        mobile
        ${log-lib}
)