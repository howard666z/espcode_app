add_library(loop SHARED loop.c)
find_library(log-lib log)
target_link_libraries(loop ${log-lib})
