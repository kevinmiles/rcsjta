/*******************************************************************************
 * Software Name : RCS IMS Stack
 * <p/>
 * Copyright (C) 2010-2016 Orange.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.gsma.rcs.api.connection.utils;

/**
 * @author Philippe LEMORDANT
 */
public class LogUtils {

    /**
     * Flag {@code isActive} to disable or enable log
     */
    public static final boolean isActive = true;

    /**
     * Utility routine to forge log tag of the RI client application
     *
     * @param classname the class name
     * @return the log tag
     */
    public static String getTag(final String classname) {
        return "[CNX][" + classname + "]";
    }
}
