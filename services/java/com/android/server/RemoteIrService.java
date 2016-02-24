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

package com.android.server;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.hardware.input.InputManager;
import android.hardware.IRemoteIrService;
import android.os.Handler;
import android.os.PowerManager;
import android.os.Process;
import android.os.RemoteException;
import android.os.IBinder;
import android.os.Binder;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.WorkSource;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Slog;
import android.view.InputDevice;

import java.lang.RuntimeException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.ListIterator;

public class RemoteIrService extends IRemoteIrService.Stub {
	
    private static final String TAG = "RemoteIrService";

    private static final int MAX_XMIT_TIME = 2000000; /* in microseconds */
	
    private static native long halOpen();
    private static native int halTransmit(long halObject, byte[] data, int size);
    private static native int halTransmit2(long halObject, byte[] data, int size);
    private static native int halCancelTransmit(long halObject);
    private static native int halReceiveData(long halObject, byte[] data, int size);
    private static native int halReceiveData2(long halObject, byte[] data, int size);
    private static native int halReceiveInit(long halObject);
    private static native int halCheckReceiveReady(long halObject);
    private static native int halReceive(long halObject, byte[] data, int size);
    private static native int halSetMode(long halObject, int mode, int value);
    private static native int halGetMode(long halObject);
    private static native int halSetValue(long halObject, int value);
    private static native int halGetValue(long halObject);
    private static native int halSetResetMode(long halObject, int mode);
    private static native int halChangeFWMode(long halObject, int mode);
    private static native int[] halGetCarrierFrequencies(long halObject);

    private final Context mContext;
    private final PowerManager.WakeLock mWakeLock;
    private final long mNativeHal;
    private final Object mHalLock = new Object();


    RemoteIrService(Context context) {
    	
        mContext = context;
        Slog.e(TAG, "Class RemoteIrService : RemoteIrService called");//xsd
        PowerManager pm = (PowerManager)context.getSystemService(
                Context.POWER_SERVICE);
                
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        mWakeLock.setReferenceCounted(true);

        mNativeHal = halOpen();

        if (mNativeHal <= 0) {
            throw new RuntimeException("No RemoteIR HAL loaded! ("+mNativeHal+")");
        }
        
        if (mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CONSUMER_IR)) {
            if (mNativeHal == 0) {
                throw new RuntimeException("FEATURE_CONSUMER_IR present, but no IR HAL loaded!");
            }
        } else if (mNativeHal != 0) {
            //throw new RuntimeException("IR HAL present, but FEATURE_CONSUMER_IR is not set!");
        }
    	Slog.e(TAG, "Class RemoteIrService : RemoteIrService called ended");//xsd    
    }

    @Override
    public boolean hasIrEmitter() {
        return (mNativeHal > 0);
    }

    private void throwIfNoIrEmitter() {
        if (mNativeHal <= 0) {
            throw new UnsupportedOperationException("IR emitter not available");
			}
    }


    @Override
    public int transmit_raw(String packageName, byte[] data, int size) {
    	
    	int err = -1;
    	Slog.e(TAG, "Class RemoteIrService : transmit_raw called");//xsd
        if (mContext.checkCallingOrSelfPermission(android.Manifest.permission.TRANSMIT_IR)
                != PackageManager.PERMISSION_GRANTED) {
            throw new SecurityException("Requires TRANSMIT_IR permission");
        }

        throwIfNoIrEmitter();

        // Right now there is no mechanism to ensure fair queing of IR requests
        synchronized (mHalLock) {
            err = halTransmit(mNativeHal, data, size);
            if (err < 0) {
                Slog.e(TAG, "Error transmitting: " + err);
				        }
        }
        Slog.e(TAG, "Class RemoteIrService : transmit_raw called ended");//xsd  
        return err;
    }

    @Override
    public int transmit_raw2(String packageName, byte[] data, int size) {
    	
    	int err = -1;
    	Slog.e(TAG, "Class RemoteIrService : transmit_raw2 called");//xsd
        if (mContext.checkCallingOrSelfPermission(android.Manifest.permission.TRANSMIT_IR)
                != PackageManager.PERMISSION_GRANTED) {
            throw new SecurityException("Requires TRANSMIT_IR permission");
        }

        throwIfNoIrEmitter();

        // Right now there is no mechanism to ensure fair queing of IR requests
        synchronized (mHalLock) {
            err = halTransmit2(mNativeHal, data, size);
            if (err < 0) {
                Slog.e(TAG, "Error transmitting: " + err);
            }
        }
         Slog.e(TAG, "Class RemoteIrService : transmit_raw2 called ended");//xsd
        return err;
    }


	@Override
    public int transmit_unit(String packageName, byte[] data, int size, int unitsize ) {
    	
    	int err = -1;
    	Slog.e(TAG, "Class RemoteIrService : transmit_unit called");//xsd
        if (mContext.checkCallingOrSelfPermission(android.Manifest.permission.TRANSMIT_IR)
                != PackageManager.PERMISSION_GRANTED) {
            throw new SecurityException("Requires TRANSMIT_IR permission");
        }

        //Slog.e(TAG, "transmit_unit: " + size + "("+unitsize+") ...");

        throwIfNoIrEmitter();

		if( unitsize < 4 ) {
			return -2;
		}
		
		byte [] unitBuf = new byte [unitsize + 4];
		int num_to_write, srcPos, chksum;
		int i, pktNo;
		
        // Right now there is no mechanism to ensure fair queing of IR requests
        synchronized (mHalLock) {
        	
        	srcPos = 0;
        	pktNo  = 0;
        	
        	num_to_write = 0;
        	
			while( size > 0 ) 
			{

	        	num_to_write = (size > unitsize) ? unitsize : size;
	        	
				// build unit packet
				unitBuf[0] = (byte)pktNo;
				unitBuf[1] = (byte)(num_to_write + 2);			// packet length except packet no and length field.
				
				// (src, srcPos, dest, destPos, length)
				System.arraycopy( data, srcPos, unitBuf, 2, num_to_write );
				
				chksum = pktNo;
				for(i=1; i < (num_to_write+2); i++) {
					chksum += (int)(unitBuf[i] & 0xff);
				}
	        	
				unitBuf[num_to_write + 2] = (byte)((chksum >> 8) & 0xff);
				unitBuf[num_to_write + 3] = (byte)(chksum & 0xff);
	        	
            	err = halTransmit(mNativeHal, unitBuf, (num_to_write+4) );
	            if (err < 0) {
	                Slog.e(TAG, "Error transmitting_unit: " + err + "("+pktNo+")");
	            }
            	
				size   -= num_to_write;
				srcPos += num_to_write;
				pktNo ++;
				
				//if( size > 0 ) {
					//sleep(1);
				//}
            	
            }	// while
            
            
			// check data length of last packet
			if( num_to_write == unitsize ) {
				
				//sleep(1);
		
				unitBuf[0] = (byte)pktNo;		// packet No.
				unitBuf[1] = 2;					// Length
				unitBuf[2] = 0;					// checksum
				unitBuf[3] = (byte)(pktNo + 2);
				
				//rc += i2c_master_send(gDeviceData->client,	&_i2c_buf[written],	num_to_write);
				err = halTransmit(mNativeHal, unitBuf, 4 );
			}

        }
		Slog.e(TAG, "Class RemoteIrService : transmit_unit called ended");//xsd
        return err;
    }


    @Override
    public int cancelTransmit(String packageName) {

    	int ret = -1;
		Slog.e(TAG, "Class RemoteIrService : cancelTransmit called");//xsd			
        throwIfNoIrEmitter();

        synchronized (mHalLock) {
            ret = halCancelTransmit(mNativeHal);
            if (ret < 0) {
                Slog.e(TAG, "Error cancelTransmit: " + ret);
            }
        }
        Slog.e(TAG, "Class RemoteIrService : cancelTransmit called ended");//xsd
        return ret;
    }

    @Override
    public int receiveData(String packageName, byte[] data, int size) {

    	int ret = -1;
		Slog.e(TAG, "Class RemoteIrService : receiveData called");//xsd		

        throwIfNoIrEmitter();

        synchronized (mHalLock) {
            ret = halReceiveData(mNativeHal, data, size);
            if (ret < 0) {
                Slog.e(TAG, "Error receiveData: " + ret);
            }
        }
        Slog.e(TAG, "Class RemoteIrService : receiveData called ended");//xsd
        return ret;
    }
    

    @Override
    public int receiveData2(String packageName, byte[] data, int size) {

    	int ret = -1;
		Slog.e(TAG, "Class RemoteIrService : receiveData2 called");//xsd	
        throwIfNoIrEmitter();

        synchronized (mHalLock) {
            ret = halReceiveData2(mNativeHal, data, size);
            if (ret < 0) {
                Slog.e(TAG, "Error receiveData: " + ret);
            }
        }
        Slog.e(TAG, "Class RemoteIrService : receiveData2 called ended");//xsd
        return ret;
    }
    
    
    @Override
    public int recvInit(String packageName) {

    	int ret = -1;
		Slog.e(TAG, "Class RemoteIrService : recvInit called");//xsd	
        if (mContext.checkCallingOrSelfPermission(android.Manifest.permission.TRANSMIT_IR)
                != PackageManager.PERMISSION_GRANTED) {
            throw new SecurityException("Requires TRANSMIT_IR permission");
        }

        throwIfNoIrEmitter();

        synchronized (mHalLock) {
            ret = halReceiveInit(mNativeHal);
        }
        Slog.e(TAG, "Class RemoteIrService : recvInit called ended");//xsd
        return ret;
	}


    @Override
    public int recvIsReady(String packageName) {

    	int ret = -1;
		Slog.e(TAG, "Class RemoteIrService : recvIsReady called");//xsd		
        if (mContext.checkCallingOrSelfPermission(android.Manifest.permission.TRANSMIT_IR)
                != PackageManager.PERMISSION_GRANTED) {
            throw new SecurityException("Requires TRANSMIT_IR permission");
        }

        throwIfNoIrEmitter();

        synchronized (mHalLock) {
            ret = halCheckReceiveReady(mNativeHal);
        }
        Slog.e(TAG, "Class RemoteIrService : recvIsReady called ended");//xsd
        return ret;
	}

    
    @Override
    public int receive(String packageName, byte[] data, int size) {
    	
    	int err = -1;
		Slog.e(TAG, "Class RemoteIrService : receive called");//xsd	
        if (mContext.checkCallingOrSelfPermission(android.Manifest.permission.TRANSMIT_IR)
                != PackageManager.PERMISSION_GRANTED) {
            throw new SecurityException("Requires TRANSMIT_IR permission");
        }

        throwIfNoIrEmitter();

		//Slog.i(TAG, "RemoteIrService.receive: " + data.length + " size:"+size);

        // Right now there is no mechanism to ensure fair queing of IR requests
        synchronized (mHalLock) {
        	
        	//halReceiveInit(mNativeHal);
        	
            err = halReceive(mNativeHal, data, size);

            if (err < 0) {
                Slog.e(TAG, "Error receiving: " + err);
				            }
        }
        Slog.e(TAG, "Class RemoteIrService : receive called ended");//xsd
        return err;
    }
    
    
    @Override
    public int setMode(String packageName, int mode, int value) {
    	
    	int err = -1;
		Slog.e(TAG, "Class RemoteIrService : setMode called");//xsd

        if (mContext.checkCallingOrSelfPermission(android.Manifest.permission.TRANSMIT_IR)
                != PackageManager.PERMISSION_GRANTED) {
            throw new SecurityException("Requires TRANSMIT_IR permission");
        }

        throwIfNoIrEmitter();

        // Right now there is no mechanism to ensure fair queing of IR requests
        synchronized (mHalLock) {
            err = halSetMode(mNativeHal, mode, value);
        }
        Slog.e(TAG, "Class RemoteIrService :  setMode called ended");//xsd
        return err;
    }    

    @Override
    public int getMode(String packageName) {
    	
    	int err = -1;
		Slog.e(TAG, "Class RemoteIrService : getMode called");//xsd

        if (mContext.checkCallingOrSelfPermission(android.Manifest.permission.TRANSMIT_IR)
                != PackageManager.PERMISSION_GRANTED) {
            throw new SecurityException("Requires TRANSMIT_IR permission");
        }

        throwIfNoIrEmitter();

        // Right now there is no mechanism to ensure fair queing of IR requests
        synchronized (mHalLock) {
            err = halGetMode(mNativeHal);
        }
        Slog.e(TAG, "Class RemoteIrService :  getMode called ended");//xsd
        return err;
    }    
    
    
    @Override
    public int setValue(String packageName, int value) {
    	
    	int err = -1;
		Slog.e(TAG, "Class RemoteIrService : setValue called");//xsd

        if (mContext.checkCallingOrSelfPermission(android.Manifest.permission.TRANSMIT_IR)
                != PackageManager.PERMISSION_GRANTED) {
            throw new SecurityException("Requires TRANSMIT_IR permission");
        }

        throwIfNoIrEmitter();

        synchronized (mHalLock) {
            err = halSetValue(mNativeHal, value);
        }
        Slog.e(TAG, "Class RemoteIrService :  setValue called ended");//xsd
        return err;
    }    

    @Override
    public int getValue(String packageName) {
    	
    	int err = -1;
		Slog.e(TAG, "Class RemoteIrService : getValue called");//xsd

        if (mContext.checkCallingOrSelfPermission(android.Manifest.permission.TRANSMIT_IR)
                != PackageManager.PERMISSION_GRANTED) {
            throw new SecurityException("Requires TRANSMIT_IR permission");
        }

        throwIfNoIrEmitter();

        synchronized (mHalLock) {
            err = halGetValue(mNativeHal);
        }
        Slog.e(TAG, "Class RemoteIrService :  getValue called ended");//xsd
        return err;
    }    
    
    @Override
    public int setResetValue( String packageName, int value ) {
    	
    	int err = -1;
		Slog.e(TAG, "Class RemoteIrService : setResetValue called");//xsd

        if (mContext.checkCallingOrSelfPermission(android.Manifest.permission.TRANSMIT_IR)
                != PackageManager.PERMISSION_GRANTED) {
            throw new SecurityException("Requires TRANSMIT_IR permission");
        }

        throwIfNoIrEmitter();

        synchronized (mHalLock) {
            err = halSetResetMode(mNativeHal, value);
        }
       Slog.e(TAG, "Class RemoteIrService :  setResetValue called ended");//xsd 
        return err;
    }    

        
    @Override
    public int change_fw_mode( String packageName, int mode ) {
    	
    	int err = -1;
		Slog.e(TAG, "Class RemoteIrService : change_fw_mode called");//xsd

        if (mContext.checkCallingOrSelfPermission(android.Manifest.permission.TRANSMIT_IR)
                != PackageManager.PERMISSION_GRANTED) {
            throw new SecurityException("Requires TRANSMIT_IR permission");
        }

        throwIfNoIrEmitter();

        synchronized (mHalLock) {
            err = halChangeFWMode(mNativeHal, mode);	// mode: zero for bootloader, non-zero for normal 
			
        }
        Slog.e(TAG, "Class RemoteIrService :  change_fw_mode called ended");//xsd 
        return err;
    }

    @Override
    public int[] getCarrierFrequencies() {
 
 		Slog.e(TAG, "Class RemoteIrService : getCarrierFrequencies called");//xsd
        if (mContext.checkCallingOrSelfPermission(android.Manifest.permission.TRANSMIT_IR)
                != PackageManager.PERMISSION_GRANTED) {
            throw new SecurityException("Requires TRANSMIT_IR permission");
        }
        
        throwIfNoIrEmitter();

        synchronized(mHalLock) {
			Slog.e(TAG, "Class RemoteIrService : change_fw_mode called ended");//xsd 
            return halGetCarrierFrequencies(mNativeHal);
			
        }
    }
}
