cmake_minimum_required(VERSION 3.10)
project(loop)

add_library(
        loop
        SHARED
        cpp/native-lib.cpp
)

find_library(
        log-lib
        log
)

target_link_libraries(
        loop
        ${log-lib}
)
