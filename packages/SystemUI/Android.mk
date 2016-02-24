LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src) \
    src/com/android/systemui/EventLogTags.logtags

LOCAL_STATIC_JAVA_LIBRARIES := Keyguard
LOCAL_STATIC_JAVA_LIBRARIES += com.mediatek.systemui.ext
LOCAL_JAVA_LIBRARIES := telephony-common
LOCAL_JAVA_LIBRARIES += mediatek-framework

LOCAL_PACKAGE_NAME := SystemUI
LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

# Vanzo:zhongjunyu on: Mon, 20 Apr 2015 20:45:12 +0800
# implement#107212,replace signal icon
ifeq ($(strip $(VANZO_FEATURE_REPLACE_SIGNAL_ICON)),yes)
    $(shell rm $(LOCAL_PATH)/res/drawable/stat_sys_signal_null.xml)
endif
# End of Vanzo:zhongjunyu
LOCAL_RESOURCE_DIR := \
    frameworks/base/packages/Keyguard/res \
    $(LOCAL_PATH)/res
LOCAL_AAPT_FLAGS := --auto-add-overlay --extra-packages com.android.keyguard
LOCAL_AAPT_FLAGS += --extra-packages com.mediatek.keyguard.ext

include $(BUILD_PACKAGE)

include $(call all-makefiles-under,$(LOCAL_PATH))
