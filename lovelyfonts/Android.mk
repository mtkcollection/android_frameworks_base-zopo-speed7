LOCAL_PATH := $(my-dir)
ifeq ($(strip $(VANZO_FEATURE_LOVELYFONTS_SUPPORT)),yes)
$(shell chmod 777 $(LOCAL_PATH)/fontd)

$(shell mkdir -p $(PRODUCT_OUT)/system/app/Lovelyfonts)
$(shell mkdir -p $(PRODUCT_OUT)/system/bin)
$(shell mkdir -p $(PRODUCT_OUT)/system/etc)
$(shell mkdir -p $(PRODUCT_OUT)/system/fonts)
$(shell mkdir -p $(PRODUCT_OUT)/system/lib)
$(shell mkdir -p $(PRODUCT_OUT)/system/lib64)
$(shell mkdir -p $(PRODUCT_OUT)/system/fonts/free)

$(shell cp -a $(LOCAL_PATH)/fontd $(PRODUCT_OUT)/system/bin/fontd)
defaultfont:=$(strip $(VANZO_DEFAULT_LOVELYFONT))
ifeq ($(defaultfont),xingyunzhongxi)
    $(shell cp -a $(LOCAL_PATH)/xingyunzhongxi-v3.02-FB24819F2A7BF624111C18C0F5B9A874.ttf  $(PRODUCT_OUT)/system/fonts/free/)
endif
ifeq ($(defaultfont),xingyunshaonv)
    $(shell cp -a $(LOCAL_PATH)/xingyunshaonv-v0.01-3F7A648ADBD1E067AF498EF06B9BE790.ttf $(PRODUCT_OUT)/system/fonts/free/)
endif
ifeq ($(defaultfont),xingyunshaonian)
    $(shell cp -a $(LOCAL_PATH)/xingyunshaonian-v1.00-98086EEC234588DE6732CECD8065EB79.ttf $(PRODUCT_OUT)/system/fonts/free/)
endif
ifeq ($(defaultfont),xingyunfangsong)
    $(shell cp -a $(LOCAL_PATH)/xingyunfangsong-v1.00-DF083BAEA35641982BB6ECC882741C4A.ttf $(PRODUCT_OUT)/system/fonts/free/)
endif
$(shell cp -a $(LOCAL_PATH)/libFonts.so $(PRODUCT_OUT)/system/lib/libFonts.so)
$(shell cp -a $(LOCAL_PATH)/libFonts64.so $(PRODUCT_OUT)/system/lib64/libFonts.so)

ifeq ($(strip $(VANZO_FEATURE_LOVELYFONTS_ICON_SHOW)),yes)
$(shell cp -a $(LOCAL_PATH)/lovelyfonts_vanzo_2.0_icon $(PRODUCT_OUT)/system/app/Lovelyfonts/lovelyfonts_vanzo_2.0_icon.apk)
else
$(shell cp -a $(LOCAL_PATH)/lovelyfonts_vanzo_2.0_noicon $(PRODUCT_OUT)/system/app/Lovelyfonts/lovelyfonts_vanzo_2.0_noicon.apk)
endif

include $(call all-subdir-makefiles)
endif

