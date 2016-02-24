/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
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

package com.android.server.pm;

import android.content.pm.ApplicationInfo;
import android.util.ArraySet;

class GrantedPermissions {
    int pkgFlags;
    /// M: [FlagExt] Flags for MTK internal use
    int pkgFlagsEx;

    ArraySet<String> grantedPermissions = new ArraySet<String>();

    int[] gids;

    GrantedPermissions(int pkgFlags) {
        setFlags(pkgFlags);
    }

    /// M: [FlagExt] Additional constructor for MTK flags
    GrantedPermissions(int pkgFlags, int pkgFlagsEx) {
        setFlags(pkgFlags);
        setFlagsEx(pkgFlagsEx);
    }

    @SuppressWarnings("unchecked")
    GrantedPermissions(GrantedPermissions base) {
        pkgFlags = base.pkgFlags;
        /// M: [FlagExt] copy mtkFlags
        pkgFlagsEx = base.pkgFlagsEx;
        grantedPermissions = new ArraySet<>(base.grantedPermissions);

        if (base.gids != null) {
            gids = base.gids.clone();
        }
    }

    void setFlags(int pkgFlags) {
        /// M: Directly set pkgFlags with the parameter
        this.pkgFlags = pkgFlags;
    }

    /// M: [FlagExt] mtkFlags set up function
    void setFlagsEx(int pkgFlagsEx) {
        this.pkgFlagsEx = pkgFlagsEx;
    }
}
