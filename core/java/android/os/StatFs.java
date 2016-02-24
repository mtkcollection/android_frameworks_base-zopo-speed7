/*
 * Copyright (C) 2007 The Android Open Source Project
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

package android.os;

import android.system.ErrnoException;
import android.system.Os;
import android.system.StructStatVfs;

/**
 * Retrieve overall information about the space on a filesystem. This is a
 * wrapper for Unix statvfs().
 */
public class StatFs {
    private StructStatVfs mStat;
/* Vanzo:yucheng on: Wed, 04 Dec 2013 22:08:05 +0800
 * Modify for ROM size customization
 */
    private String mBlockPath;
// End of Vanzo: yucheng

    /**
     * Construct a new StatFs for looking at the stats of the filesystem at
     * {@code path}. Upon construction, the stat of the file system will be
     * performed, and the values retrieved available from the methods on this
     * class.
     *
     * @param path path in the desired file system to stat.
     */
    public StatFs(String path) {
/* Vanzo:yucheng on: Wed, 04 Dec 2013 22:18:35 +0800
 * Modify for ROM size customization
 */
        mBlockPath = path;
// End of Vanzo: yucheng
        mStat = doStat(path);
    }

    private static StructStatVfs doStat(String path) {
        try {
            return Os.statvfs(path);
        } catch (ErrnoException e) {
            throw new IllegalArgumentException("Invalid path: " + path, e);
        }
    }

    /**
     * Perform a restat of the file system referenced by this object. This is
     * the same as re-constructing the object with the same file system path,
     * and the new stat values are available upon return.
     */
    public void restat(String path) {
/* Vanzo:yucheng on: Wed, 04 Dec 2013 22:18:35 +0800
 * Modify for ROM size customization
 */
        mBlockPath = path;
// End of Vanzo: yucheng
        mStat = doStat(path);
    }

    /**
     * @deprecated Use {@link #getBlockSizeLong()} instead.
     */
    @Deprecated
    public int getBlockSize() {
        return (int) mStat.f_bsize;
    }

    /**
     * The size, in bytes, of a block on the file system. This corresponds to
     * the Unix {@code statvfs.f_bsize} field.
     */
    public long getBlockSizeLong() {
        return mStat.f_bsize;
    }

    /**
     * @deprecated Use {@link #getBlockCountLong()} instead.
     */
    @Deprecated
    public int getBlockCount() {
/* Vanzo:yucheng on: Wed, 04 Dec 2013 22:11:01 +0800
 * Modify for ROM size customization
        return (int) mStat.f_blocks;
 */
        //Customized disk size, unit: M byte
        int customizedSize = android.os.SystemProperties.getInt("ro.init.data_size", -1);
        int customizeBlocks = 0;
        if (customizedSize > 0 && android.os.Environment.getDataDirectory().getAbsolutePath().equals(mBlockPath)) {
            customizeBlocks = (int) (customizedSize * 1024L * 1024L / mStat.f_bsize);
        }

        return (int) ((customizeBlocks > mStat.f_blocks) ? customizeBlocks : mStat.f_blocks);
// End of Vanzo: yucheng
    }

    /**
     * The total number of blocks on the file system. This corresponds to the
     * Unix {@code statvfs.f_blocks} field.
     */
    public long getBlockCountLong() {
        return mStat.f_blocks;
    }

    /**
     * @deprecated Use {@link #getFreeBlocksLong()} instead.
     */
    @Deprecated
    public int getFreeBlocks() {
        return (int) mStat.f_bfree;
    }

    /**
     * The total number of blocks that are free on the file system, including
     * reserved blocks (that are not available to normal applications). This
     * corresponds to the Unix {@code statvfs.f_bfree} field. Most applications
     * will want to use {@link #getAvailableBlocks()} instead.
     */
    public long getFreeBlocksLong() {
        return mStat.f_bfree;
    }

    /**
     * The number of bytes that are free on the file system, including reserved
     * blocks (that are not available to normal applications). Most applications
     * will want to use {@link #getAvailableBytes()} instead.
     */
    public long getFreeBytes() {
        return mStat.f_bfree * mStat.f_bsize;
    }

    /**
     * @deprecated Use {@link #getAvailableBlocksLong()} instead.
     */
    @Deprecated
    public int getAvailableBlocks() {
        return (int) mStat.f_bavail;
    }

    /**
     * The number of blocks that are free on the file system and available to
     * applications. This corresponds to the Unix {@code statvfs.f_bavail} field.
     */
    public long getAvailableBlocksLong() {
        return mStat.f_bavail;
    }

    /**
     * The number of bytes that are free on the file system and available to
     * applications.
     */
    public long getAvailableBytes() {
        return mStat.f_bavail * mStat.f_bsize;
    }

    /**
     * The total number of bytes supported by the file system.
     */
    public long getTotalBytes() {
        return mStat.f_blocks * mStat.f_bsize;
    }
}
