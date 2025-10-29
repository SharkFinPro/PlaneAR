cmake_minimum_required(VERSION 3.22.1)

project("GraphicsEngine")

add_library(${CMAKE_PROJECT_NAME} SHARED
    main.cpp)

find_package(game-activity REQUIRED CONFIG)

set(CMAKE_SHARED_LINKER_FLAGS
  "${CMAKE_SHARED_LINKER_FLAGS} -u Java_com_google_androidgamesdk_GameActivity_initializeNativeCode")

target_link_libraries(${CMAKE_PROJECT_NAME}
  game-activity::game-activity_static
    android
    log)
