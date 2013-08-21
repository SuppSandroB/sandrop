LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := socketdest

LOCAL_SRC_FILES := socketdest.c

include $(BUILD_SHARED_LIBRARY)

