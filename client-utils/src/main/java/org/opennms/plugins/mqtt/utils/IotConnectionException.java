/* ***************************************************************************
 * OpenNMS Modifications: Copyright 2018 OpenNMS Group Inc, Entimoss Ltd. Or their affiliates.
 * All Rights Reserved.
/****************************************************************************
/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.opennms.plugins.mqtt.utils;


public class IotConnectionException extends Exception {
    /**
     * The Constant serialVersionUID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Instantiates a new exception object.
     *
     * @param message
     * the error message
     */
    public IotConnectionException(String message) {
        super(message);
    }

    /**
     * Instantiates a new exception object.
     *
     * @param cause
     * the cause. A null value is permitted, and indicates that the
     * cause is nonexistent or unknown.
     */
    public IotConnectionException(Throwable cause) {
        super(cause);
    }

 
}
