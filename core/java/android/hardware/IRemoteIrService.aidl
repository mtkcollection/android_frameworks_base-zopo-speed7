/*
 * Copyright (C) 2011 The Android Open Source Project
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

import android.os.ParcelFileDescriptor;

/** {@hide} */
interface IRemoteIrService
{
	boolean hasIrEmitter();

	int  transmit_raw( String packageName, in byte[] data, int size );
	int  transmit_raw2( String packageName, in byte[] data, int size );
	int  transmit_unit( String packageName, in byte[] data, int size, int unitsize );
	int  cancelTransmit( String packageName );
	int	 receiveData(String packageName, out byte[] data, int size);
	int	 receiveData2(String packageName, out byte[] data, int size);

	int  recvInit( String packageName );
	int  recvIsReady( String packageName );
	int  receive ( String packageName, out byte[] data, int size );
	int  setMode ( String packageName, int mode, int value );
	int  getMode ( String packageName );
	int  setValue( String packageName, int value );
	int  getValue( String packageName );
	int  setResetValue ( String packageName, int value );
	int  change_fw_mode ( String packageName, int mode );
	
	int[] getCarrierFrequencies();
}
