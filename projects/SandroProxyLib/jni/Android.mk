LOCAL_PATH := $(call my-dir)
ROOT_PATH := $(LOCAL_PATH)

socket_dest_dir := $(addprefix $(LOCAL_PATH)/,$(addsuffix /Android.mk, \
 	socketdest \
 	))
include $(socket_dest_dir)

include $(CLEAR_VARS)
# Iptables
LOCAL_PATH := $(ROOT_PATH)
 iptables_subdirs := $(addprefix $(LOCAL_PATH)/iptables/,$(addsuffix /Android.mk, \
 	iptables \
 	extensions \
 	libiptc \
 	))
 include $(iptables_subdirs)
