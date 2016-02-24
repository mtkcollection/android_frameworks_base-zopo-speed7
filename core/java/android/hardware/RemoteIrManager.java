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

package android.hardware;

import android.content.Context;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

/**
 *
 * Class that operates remote infrared on the device.
 *
 * <p>
 * To obtain an instance of the system infrared transmitter, call
 * {@link android.content.Context#getSystemService(java.lang.String)
 * Context.getSystemService()} with
 * {@link android.content.Context#REMOTE_IR_SERVICE} as the argument.
 * </p>
 * @hide
 */
public final class RemoteIrManager {
    private static final String TAG = "RemoteIr";

    private final String 			mPackageName;
    private final IRemoteIrService 	mService;

    /**
     * @hide to prevent subclassing from outside of the framework
     * @hide
     */
    public RemoteIrManager(Context context) {
        mPackageName = context.getPackageName();
        mService = IRemoteIrService.Stub.asInterface(
                	ServiceManager.getService(Context.REMOTE_IR_SERVICE));

        if (mService == null) {
            Log.w(TAG, "RemoteIrManager: no remote ir service.");
        }
    }

    /**
     * Check whether the device has an infrared emitter.
     * @hide
     * @return true if the device has an infrared emitter, else false.
     */
    public boolean hasIrEmitter() {
        if (mService == null) {
            Log.w(TAG, "no remote ir service.");
            return false;
        }

        try {
            return mService.hasIrEmitter();
        } catch (RemoteException e) {
        }
        return false;
    }


    /**
     * Tansmit infrared signal data.
     * <p>
     * This method is synchronous; when it returns the pattern has been transmitted.
     * </p>
     *
     * @param data The IR signal data array.
     * @param size The total number of byte of data array.
     * @hide
     */
    public int transmit(byte[] data, int size ) {
    	
    	int ret = -1;
    	Log.w(TAG, "RemoteIrManager: function transmit called");//xsd
        //Log.w(TAG, "RemoteIrManager: transmit called.");
        
        if (mService == null) {
            Log.w(TAG, "failed to transmit; no remote ir service.");
            return ret;
        }

        try {
            ret = mService.transmit_unit(mPackageName, data, size, 250 );
        } catch (RemoteException e) {
            Log.w(TAG, "failed to transmit.", e);
        }
        Log.w(TAG, "RemoteIrManager end function transmit 2");//xsd
        return ret;
    }


    /**
     * Transmit infrared signal data.
     * <p>
     * This method is synchronous; when it returns the pattern has been transmitted.
     * </p>
     *
     * @param data The IR signal data array.
     * @param size The number of byte of data array.
     * @hide
     */
    public int write_data(byte[] data, int size) {
    	int ret = -1;
    	
        Log.w(TAG, "RemoteIrManager: write_data called.");//xsd
        
        if (mService == null) {
            Log.w(TAG, "failed to write_data; no remote ir service.");
            return ret;
        }

        try {
            ret = mService.transmit_raw(mPackageName, data, size);
        } catch (RemoteException e) {
            Log.w(TAG, "failed to write_data.", e);
        }
        Log.w(TAG, "RemoteIrManager: write_data called end.");//xsd
        return ret;
    }
    

    /**
     * Transmit infrared signal data.
     * <p>
     * This method is synchronous; when it returns the pattern has been transmitted.
     * (No wakeup process)
     * </p>
     *
     * @param data The IR signal data array.
     * @param size The number of byte of data array.
     * @hide
     */
    public int write_data2(byte[] data, int size) {
    	int ret = -1;
    	
        Log.w(TAG, "RemoteIrManager: write_data2 called.");
        
        if (mService == null) {
            Log.w(TAG, "failed to write_data; no remote ir service.");
            return ret;
        }

        try {
            ret = mService.transmit_raw2(mPackageName, data, size);
        } catch (RemoteException e) {
            Log.w(TAG, "failed to write_data.", e);
        }
        Log.w(TAG, "RemoteIrManager: write_data2 called end.");//xsd
        return ret;
    }
    
        
    /**
     * {@hide}
     * @hide
     */
    public int write_unit(byte[] data, int size, int unitsize ) {
    	
    	int ret = -1;
    	
        Log.w(TAG, "RemoteIrManager: transmit_unit called.");
        
        if (mService == null) {
            Log.w(TAG, "failed to write_unit; no remote ir service.");
            return ret;
        }

        try {
            ret = mService.transmit_unit(mPackageName, data, size, unitsize );
        } catch (RemoteException e) {
            Log.w(TAG, "failed to write_unit.", e);
        }
        
        return ret;
    }


    /**
     * @hide
     */
    public int cancelTransmit() {

        int ret = -1;
        byte [] cmd = { (byte)0x69, (byte)0x69, (byte)0x96, (byte)0x96 };

        ret = transmit( cmd, 4 );

        if( ret < 0 ) {
            Log.w(TAG, "failed to cancelTransmit.");
        }

        return ret;
    }



    /**
     * receive data from ir sensor.
     * <p>
     * Read any remained or ready data from ir sensor.
     * </p>
     *
     * @param data The data array to receive from IR chip.
     * @param size The number of byte of data array.
     *
     * @return The size of received data in bytes.
     * @hide
     */
    public int receiveData(byte[] data, int size) {
    	int ret = -1;
    	
        if (mService == null) {
            Log.w(TAG, "failed to receiveData; no consumer ir service.");
            return ret;
        }

        try {
            ret = mService.receiveData(mPackageName, data, size);
        } catch (RemoteException e) {
            Log.w(TAG, "failed to receiveData.", e);
        }

        Log.w(TAG, "RemoteIrManager: receiveData returned:" +ret );
        
        return ret;
    }



    /**
     * (experimental) receive data from ir sensor.
     * <p>
     * Read any remained or ready data from ir sensor.
     * </p>
     *
     * @param data The data array to receive from IR chip.
     * @param size The number of byte of data array.
     *
     * @return The size of received data in bytes.
     * @hide
     */
    public int receiveData2(byte[] data, int size) {
    	int ret = -1;
    	
        if (mService == null) {
            Log.w(TAG, "failed to receiveData; no consumer ir service.");
            return ret;
        }

        try {
            ret = mService.receiveData2(mPackageName, data, size);
        } catch (RemoteException e) {
            Log.w(TAG, "failed to receiveData.", e);
        }

        Log.w(TAG, "RemoteIrManager: receiveData returned:" +ret );
        
        return ret;
    }


    /**
     * Initialize ir signal receive mode.
     *
     * <p>
     * 	Change current IR operation mode to receive mode.
     * 	You can use receive_is_ready(), receive() function after execute this function.
     * </p>
     * @hide
     */
	public int receive_init() {

		int ret = -1;
		byte [] cmd = { (byte)0x11, (byte)0x11, (byte)0xee, (byte)0xee };
		
		ret = transmit( cmd, 4 );
    	
    	if( ret < 0 ) {
            Log.w(TAG, "failed to initialize receive.");
        }
        else {
        	
			/* change mode to input mode */
			//ioctl(dev->file_fd, REMOTE_CTRL_CTL_SET_GPIO_MODE, 0, 0);        	
	        try {
				mService.setMode( mPackageName, 0, 0 );	// 0:INPUT_MODE, 0:VALUE
	        } catch (RemoteException e) {
	            Log.w(TAG, "failed to setMode.", e);
	        }
        }

        return ret;
	}


    /**
     * Check the received data exists in internal buffer.
     *
     * <p>
     * If the data ready, user can use receive() function to retrieve received data.
     * </p>
     *
     * @return 1 if data ready. 0 if data not yet ready.
     * @hide
     */
	public int receive_is_ready() {
		int ret = -1;
    	
        if (mService != null) {
	    
	        try {
	            ret = mService.recvIsReady(mPackageName);
	        } catch (RemoteException e) {
	            Log.w(TAG, "failed to check receive data is ready.", e);
	        }
		}
        
        return ret;		
	}
	

    /**
     * receive infrared signal data from ir sensor.
     * <p>
     * This method is synchronous; when it returns the buffer has been received.
     * </p>
     *
     * @param data The data array to receive IR signal data.
     * @param size The number of byte of data array.
     *
     * @return The size of received data in bytes.
     * @hide
     */
    public int receive(byte[] data, int size) {
    	int ret = -1;
    	
        if (mService == null) {
            Log.w(TAG, "failed to receive; no remote ir service.");
            return ret;
        }

        try {
            ret = mService.receive(mPackageName, data, size);
        } catch (RemoteException e) {
            Log.w(TAG, "failed to receive.", e);
        }

        Log.w(TAG, "RemoteIrManager: receive returned:" +ret );
        
        return ret;
    }


    /**
     * @hide
     */
    public int setGpioMode( int mode, int value ) {

        // mode: 0:INPUT 1:OUTPUT
        // value: 0:LOW  1:HIGH
        int ret = -1;

        if (mService == null) {
            Log.w(TAG, "failed to setGpioMode; no remote ir service.");
            return ret;
        }

        try {
            ret = mService.setMode(mPackageName, mode, value);	
        } catch (RemoteException e) {
            Log.w(TAG, "failed to setGpioMode.", e);
        }

        //Log.w(TAG, "RemoteIrManager: setGpioMode returned:" +ret );

        return ret;
    }

    /**
     * @hide
     */
    public int setGpioValue( int value ) {

        // value: 0:LOW  1:HIGH
        int ret = -1;

        if (mService == null) {
            Log.w(TAG, "failed to setGpioMode; no remote ir service.");
            return ret;
        }

        try {
            ret = mService.setValue(mPackageName, value);	
        } catch (RemoteException e) {
            Log.w(TAG, "failed to setGpioMode.", e);
        }

        //Log.w(TAG, "RemoteIrManager: setGpioMode returned:" +ret );

        return ret;
    }

    /**
     * @hide
     */
    public int getGpioValue() {

        // value: 0:LOW  1:HIGH
        int ret = -1;

        if (mService == null) {
            Log.w(TAG, "failed to setGpioMode; no remote ir service.");
            return ret;
        }

        try {
            ret = mService.getValue(mPackageName);	
        } catch (RemoteException e) {
            Log.w(TAG, "failed to getGpioValue.", e);
        }

        //Log.w(TAG, "RemoteIrManager: setGpioMode returned:" +ret );

        return ret;
    }


    /**
     * @hide
     */
    public int setResetValue( int value ) {

        // value: 0:LOW  1:HIGH
        int ret = -1;

        if (mService == null) {
            Log.w(TAG, "failed to setGpioMode; no remote ir service.");
            return ret;
        }

        try {
            ret = mService.setResetValue(mPackageName, value);	
        } catch (RemoteException e) {
            Log.w(TAG, "failed to setResetValue.", e);
        }

        //Log.w(TAG, "RemoteIrManager: setGpioMode returned:" +ret );

        return ret;
    }

    /**
     * @hide
     */
    public int changeFWMode( int mode ) {

        // mode: zero for bootloader, non-zero for normal 
        int ret = -1;

        if (mService == null) {
            Log.w(TAG, "failed to receive; no remote ir service.");
            return ret;
        }

        try {
            ret = mService.change_fw_mode(mPackageName, mode);	
        } catch (RemoteException e) {
            Log.w(TAG, "failed to changeFWMode.", e);
        }

        Log.w(TAG, "RemoteIrManager: changeModeUserIr returned:" +ret );

        return ret;
    }

    /**
     *
     * Represents a range of carrier frequencies (inclusive) on which the
     * infrared transmitter can transmit
     * @hide
     */
    public final class CarrierFrequencyRange {
        private final int mMinFrequency;
        private final int mMaxFrequency;

        /**
         * Create a segment of a carrier frequency range.
         *
         * @param min The minimum transmittable frequency in this range segment.
         * @param max The maximum transmittable frequency in this range segment.
         * @hide
         */
        public CarrierFrequencyRange(int min, int max) {
            mMinFrequency = min;
            mMaxFrequency = max;
        }

        /**
         * Get the minimum (inclusive) frequency in this range segment.
         * @hide
         */
        public int getMinFrequency() {
            return mMinFrequency;
        }

        /**
         * Get the maximum (inclusive) frequency in this range segment.
         * @hide
         */
        public int getMaxFrequency() {
            return mMaxFrequency;
        }
    };

    /**
     * Query the infrared transmitter's supported carrier frequencies
     *
     * @return an array of
     * {@link android.hardware.RemoteIrManager.CarrierFrequencyRange}
     * objects representing the ranges that the transmitter can support, or
     * null if there was an error communicating with the Consumer IR Service.
     * @hide
     */
    public CarrierFrequencyRange[] getCarrierFrequencies() {
        if (mService == null) {
            Log.w(TAG, "no remote ir service.");
            return null;
        }

        try {
            int[] freqs = mService.getCarrierFrequencies();
            if (freqs.length % 2 != 0) {
                Log.w(TAG, "remote ir service returned an uneven number of frequencies.");
                return null;
            }
            CarrierFrequencyRange[] range = new CarrierFrequencyRange[freqs.length / 2];

            for (int i = 0; i < freqs.length; i += 2) {
                range[i / 2] = new CarrierFrequencyRange(freqs[i], freqs[i+1]);
            }
            return range;
        } catch (RemoteException e) {
        }
        return null;
    }

}
