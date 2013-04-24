LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := unix_socket
LOCAL_SRC_FILES := unix_socket.cpp
include $(BUILD_EXECUTABLE)
