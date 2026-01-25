project(graphicsEngineTest)

add_library(${PROJECT_NAME} SHARED main.cpp)

target_link_libraries(${PROJECT_NAME} PRIVATE GraphicsEngine)

set(CMAKE_SHARED_LINKER_FLAGS
  "${CMAKE_SHARED_LINKER_FLAGS} -u Java_com_google_androidgamesdk_GameActivity_initializeNativeCode")