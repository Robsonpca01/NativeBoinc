
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := nativeboinc_utils
LOCAL_SRC_FILES := chmod.c putils.c execperms.c

include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)

LOCAL_MODULE    := execwrapper
LOCAL_SRC_FILES := libexecwrapper.so
LOCAL_PREBUILTS := libexecwrapper.so
include $(PREBUILT_SHARED_LIBRARY)