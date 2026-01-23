cmake_minimum_required(VERSION 3.22.1)

project("mobile")

add_library(
    mobile
    SHARED
    native-lib.cpp
    integrity_check.cpp
    ksu_detector.cpp
    anti_debug.cpp
)

find_library(
    log-lib
    log
)

target_link_libraries(
    mobile
    ${log-lib}
)