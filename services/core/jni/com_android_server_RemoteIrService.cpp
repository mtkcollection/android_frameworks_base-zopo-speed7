/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#define LOG_TAG "RemoteIrService"

#include "jni.h"
#include "JNIHelp.h"
#include "android_runtime/AndroidRuntime.h"

#include <stdlib.h>
#include <utils/misc.h>
#include <utils/Log.h>
#include <hardware/hardware.h>
#include <hardware/remoteir.h>
#include <ScopedPrimitiveArray.h>

namespace android {

static jlong halOpen(JNIEnv *env, jobject obj) {
    hw_module_t const* module;
    remoteir_device_t *dev;
    int err;
    unsigned long dev_pt;
	ALOGE("RemoteIrService: halOpen called");//xsd

    err = hw_get_module(REMOTEIR_HARDWARE_MODULE_ID, &module);
    if (err != 0) {
        ALOGE("Can't open remote IR HW Module, error: %d", err);
        return -1;
    }

    err = module->methods->open(module, REMOTEIR_TRANSMITTER,
            (hw_device_t **) &dev);
    if (err < 0) {
        ALOGE("Can't open remote IR transmitter, error: %d", err);
        return -2;
    }
    dev_pt = (unsigned long)dev;
	ALOGE("RemoteIrService: halOpen called ended:%d",dev_pt);//xsd
		
    return (jlong)dev_pt;
}


static jint halTransmit(JNIEnv *env, jobject obj, jlong halObject, jbyteArray data, jint size) 
{
    int ret;
	ALOGE("RemoteIrService: halTransmit called");//xsd
    remoteir_device_t *dev = reinterpret_cast<remoteir_device_t*>(halObject);
    ScopedByteArrayRO cData(env, data);
    if (cData.get() == NULL) {
        return -10; //-EINVAL;
       ALOGE("HAL:Remote IR Can't use halTransmit");//xsd 
    }
    //jsize patternLength = cData.size();

    ret = dev->transmit(dev, (unsigned char *)cData.get(), size);
	ALOGE("RemoteIrService: halTransmit called ended");//xsd
    return (jint)(ret);
}

static jint halTransmit2(JNIEnv *env, jobject obj, jlong halObject, jbyteArray data, jint size) 
{
    int ret;
	ALOGE("RemoteIrService: halTransmit2 called");//xsd

    remoteir_device_t *dev = reinterpret_cast<remoteir_device_t*>(halObject);
    ScopedByteArrayRO cData(env, data);
    if (cData.get() == NULL) {
		ALOGE("HAL:Remote IR Can't transmit2"); //xsd
		return -10; //-EINVAL;
       
    }
    //jsize patternLength = cData.size();

    ret = dev->transmit_raw(dev, (unsigned char *)cData.get(), size);
	ALOGE("RemoteIrService: halTransmit2 called ended");//xsd

    return (jint)(ret);
}


static jint halCancelTransmit(JNIEnv *env, jobject obj, jlong halObject) 
{
    int ret;
	ALOGE("RemoteIrService: halCancelTransmit called");//xsd

    remoteir_device_t *dev = reinterpret_cast<remoteir_device_t*>(halObject);

    ret = dev->cancel_transmit(dev);
	ALOGE("RemoteIrService: halCancelTransmit called ended");//xsd

    return (jint)(ret);
}


static jint halReceiveData(JNIEnv *env, jobject obj, jlong halObject, jbyteArray data, jint size) 
{
    int ret;
	unsigned char *buf;
	ALOGE("RemoteIrService: halReceiveData called");//xsd

	if( size <= 0 ) return 0;
	
    remoteir_device_t *dev = reinterpret_cast<remoteir_device_t*>(halObject);

	
	buf = (unsigned char *)malloc(size);
	memset( buf, 0, size );

    ret = dev->receive_data(dev, buf, size);

	env->SetByteArrayRegion(data, 0, size, (const jbyte*)buf);

	free( buf );
	ALOGE("RemoteIrService: halReceiveData called ended");//xsd

    return (jint)(ret);
}


static jint halReceiveData2(JNIEnv *env, jobject obj, jlong halObject, jbyteArray data, jint size) 
{
    int ret;
	unsigned char *buf;
	ALOGE("RemoteIrService: halReceiveData2 called");//xsd

	if( size <= 0 ) return 0;
	
    remoteir_device_t *dev = reinterpret_cast<remoteir_device_t*>(halObject);

	
	buf = (unsigned char *)malloc(size);
	memset( buf, 0, size );

    ret = dev->receive_data_raw(dev, buf, size);

	env->SetByteArrayRegion(data, 0, size, (const jbyte*)buf);

	free( buf );
	ALOGE("RemoteIrService: halReceiveData2 called ended");//xsd

    return (jint)(ret);
}






static jint halReceiveInit(JNIEnv *env, jobject obj, jlong halObject) 
{
	int ret;
	ALOGE("RemoteIrService: halReceiveInit called");//xsd
    remoteir_device_t *dev = reinterpret_cast<remoteir_device_t*>(halObject);

    ret = dev->recv_init(dev);
	ALOGE("RemoteIrService: halReceiveInit called ended");//xsd

    return (jint)(ret);
}


static jint halCheckReceiveReady(JNIEnv *env, jobject obj, jlong halObject) 
{
	ALOGE("RemoteIrService: halCheckReceiveReady called");//xsd
    remoteir_device_t *dev = reinterpret_cast<remoteir_device_t*>(halObject);
	ALOGE("RemoteIrService: halCheckReceiveReady called ended");//xsd

    return (jint)dev->recv_is_ready(dev);
}


static jint halReceive(JNIEnv *env, jobject obj, jlong halObject, jbyteArray data, jint size) 
{
    int ret;
	unsigned char *buf;
	ALOGE("RemoteIrService: halReceive called");//xsd

	if( size <= 0 ) return 0;
	
    remoteir_device_t *dev = reinterpret_cast<remoteir_device_t*>(halObject);

	
	buf = (unsigned char *)malloc(size);
	memset( buf, 0, size );

    ret = dev->recv_sync(dev, buf, size);

	env->SetByteArrayRegion(data, 0, size, (const jbyte*)buf);

	free( buf );
	ALOGE("RemoteIrService: halReceive called ended");//xsd

    return (jint)(ret);
}



static jint halSetMode(JNIEnv *env, jobject obj, jlong halObject, jint mode, jint value) 
{
    int ret;
	ALOGE("RemoteIrService: halSetMode called");//xsd	
    remoteir_device_t *dev = reinterpret_cast<remoteir_device_t*>(halObject);
    ret = dev->setmode(dev, mode, value);
	ALOGE("RemoteIrService: halSetMode called ended");//xsd
    return (jint)(ret);
}

static jint halGetMode(JNIEnv *env, jobject obj, jlong halObject) 
{
	ALOGE("RemoteIrService: halGetMode called");//xsd	
    remoteir_device_t *dev = reinterpret_cast<remoteir_device_t*>(halObject);
	ALOGE("RemoteIrService: halGetMode called ended");//xsd
    return dev->getmode(dev);
}


static jint halSetValue(JNIEnv *env, jobject obj, jlong halObject, jint value) 
{
    int ret;
	ALOGE("RemoteIrService: halSetValue called");//xsd	
    remoteir_device_t *dev = reinterpret_cast<remoteir_device_t*>(halObject);
    ret = dev->setvalue(dev, value);
	ALOGE("RemoteIrService: halSetValue called ended");//xsd
    return (jint)(ret);
}

static jint halGetValue(JNIEnv *env, jobject obj, jlong halObject) 
{
	ALOGE("RemoteIrService: halGetValue called");//xsd
    remoteir_device_t *dev = reinterpret_cast<remoteir_device_t*>(halObject);
	ALOGE("RemoteIrService: halGetValue called ended");//xsd
	return dev->getvalue(dev);
}


static jint halSetResetMode(JNIEnv *env, jobject obj, jlong halObject, jint mode) 
{
	ALOGE("RemoteIrService: halGetValue called");//xsd
    remoteir_device_t *dev = reinterpret_cast<remoteir_device_t*>(halObject);
	ALOGE("RemoteIrService: halSetResetMode called ended");//xsd
	return dev->set_reset_mode(dev, mode);
}

/* mode: zero for bootloader, non-zero for normal  */
static jint halChangeFWMode(JNIEnv *env, jobject obj, jlong halObject, jint mode) 
{
	ALOGE("RemoteIrService: halChangeFWMode called");//xsd
    remoteir_device_t *dev = reinterpret_cast<remoteir_device_t*>(halObject);
    ALOGE("RemoteIrService: halChangeFWMode called ended");//xsd	
    return dev->change_device_mode(dev, mode);
}



static jintArray halGetCarrierFrequencies(JNIEnv *env, jobject obj, jlong halObject) 
{
    remoteir_device_t *dev = (remoteir_device_t *) halObject;
    remoteir_freq_range_t *ranges;
    int len;
	ALOGE("RemoteIrService: halGetCarrierFrequencies called");//xsd

	jintArray freqsOut;
	jint *outElements;
	
    len = dev->get_num_carrier_freqs(dev);
    if (len <= 0)
        return NULL;

    ranges = new remoteir_freq_range_t[len];

    len = dev->get_carrier_freqs(dev, len, ranges);
    if (len <= 0) {
        delete[] ranges;
        return NULL;
    }

    int i;
    
    freqsOut = env->NewIntArray(len*2);
    if (freqsOut == NULL) {
        delete[] ranges;
        return NULL;
    }

    outElements = env->GetIntArrayElements(freqsOut,NULL);
    
    for (i = 0; i < len; i++) {
        outElements[i*2]   = ranges[i].min;
        outElements[i*2+1] = ranges[i].max;
    }
	env->ReleaseIntArrayElements(freqsOut,outElements,0);

    delete[] ranges;
	ALOGE("RemoteIrService: halGetCarrierFrequencies called ended");//xsd	
    return freqsOut;
}

static JNINativeMethod method_table[] = {
    { "halOpen", "()J", (void *)halOpen },
    { "halTransmit", "(J[BI)I", (void *)halTransmit },
    { "halTransmit2", "(J[BI)I", (void *)halTransmit2 },
    { "halCancelTransmit", 	"(J)I", (void *)halCancelTransmit },
    { "halReceiveData", 	"(J[BI)I", (void *)halReceiveData },
    { "halReceiveData2", 	"(J[BI)I", (void *)halReceiveData2 },
    { "halReceiveInit", "(J)I", (void *)halReceiveInit },
    { "halCheckReceiveReady", "(J)I", (void *)halCheckReceiveReady },
    { "halReceive",  	"(J[BI)I", (void *)halReceive },
    { "halSetMode",  "(JII)I", (void *)halSetMode },
    { "halGetMode",  "(J)I", (void *)halGetMode },
    { "halSetValue", "(JI)I", (void *)halSetValue },
    { "halGetValue", "(J)I", (void *)halGetValue },
    { "halSetResetMode", "(JI)I", (void *)halSetResetMode },
    { "halChangeFWMode", "(JI)I", (void *)halChangeFWMode },
    { "halGetCarrierFrequencies", "(J)[I", (void *)halGetCarrierFrequencies},
};

int register_android_server_RemoteIrService(JNIEnv *env) 
{
    jclass clazz = env->FindClass("com/android/server/RemoteIrService");
    if (clazz == NULL) {
        ALOGE("Can't find com/android/server/RemoteIrService");
        //return -1;
    }
	 ALOGE("register android server RemoteIrService successed!");
    return jniRegisterNativeMethods(env, "com/android/server/RemoteIrService",
            method_table, NELEM(method_table));
}

}; // namespace android
