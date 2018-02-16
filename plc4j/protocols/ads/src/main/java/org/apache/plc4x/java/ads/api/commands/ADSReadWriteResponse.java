/*
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
 */
package org.apache.plc4x.java.ads.api.commands;

import org.apache.plc4x.java.ads.api.commands.types.Data;
import org.apache.plc4x.java.ads.api.commands.types.Length;
import org.apache.plc4x.java.ads.api.commands.types.Result;
import org.apache.plc4x.java.ads.api.generic.ADSData;
import org.apache.plc4x.java.ads.api.generic.AMSHeader;
import org.apache.plc4x.java.ads.api.generic.AMSTCPHeader;
import org.apache.plc4x.java.ads.api.generic.types.AMSNetId;
import org.apache.plc4x.java.ads.api.generic.types.AMSPort;
import org.apache.plc4x.java.ads.api.generic.types.Command;
import org.apache.plc4x.java.ads.api.generic.types.Invoke;
import org.apache.plc4x.java.ads.api.util.LengthSupplier;

import java.util.Objects;

/**
 * With ADS Read Write data will be written to an ADS device. Additionally, data can be read from the ADS device.
 */
@ADSCommandType(Command.ADS_Read_Write)
public class ADSReadWriteResponse extends ADSAbstractResponse {

    /**
     * 4 bytes	ADS error number
     */
    private final Result result;

    /**
     * 4 bytes	Length of data which are supplied back.
     */
    private final Length length;

    /**
     * n bytes	Data which are supplied back.
     */
    private final Data data;

    ////
    // Used when fields should be calculated. TODO: check if we better work with a subclass.
    private final LengthSupplier lengthSupplier;
    private final boolean calculated;
    //
    ///

    protected ADSReadWriteResponse(AMSTCPHeader amstcpHeader, AMSHeader amsHeader, Result result, Length length, Data data) {
        super(amstcpHeader, amsHeader);
        this.result = Objects.requireNonNull(result);
        this.length = Objects.requireNonNull(length);
        this.data = Objects.requireNonNull(data);
        this.lengthSupplier = null;
        this.calculated = false;
    }

    protected ADSReadWriteResponse(AMSNetId targetAmsNetId, AMSPort targetAmsPort, AMSNetId sourceAmsNetId, AMSPort sourceAmsPort, Invoke invokeId, Result result, Data data) {
        super(targetAmsNetId, targetAmsPort, sourceAmsNetId, sourceAmsPort, invokeId);
        this.result = Objects.requireNonNull(result);
        this.length = null;
        this.data = Objects.requireNonNull(data);
        this.lengthSupplier = () -> data.getCalculatedLength();
        this.calculated = true;
    }

    public static ADSReadWriteResponse of(AMSTCPHeader amstcpHeader, AMSHeader amsHeader, Result result, Length length, Data data) {
        return new ADSReadWriteResponse(amstcpHeader, amsHeader, result, length, data);
    }

    public static ADSReadWriteResponse of(AMSNetId targetAmsNetId, AMSPort targetAmsPort, AMSNetId sourceAmsNetId, AMSPort sourceAmsPort, Invoke invokeId, Result result, Data data) {
        return new ADSReadWriteResponse(targetAmsNetId, targetAmsPort, sourceAmsNetId, sourceAmsPort, invokeId, result, data);
    }

    @Override
    public ADSData getAdsData() {
        return buildADSData(result, (calculated ? Length.of(lengthSupplier.getCalculatedLength()) : length), data);
    }

    @Override
    public String toString() {
        return "ADSReadWriteResponse{" +
            "result=" + result +
            ", length=" + (calculated ? Length.of(lengthSupplier.getCalculatedLength()) : length) +
            ", data=" + data +
            "} " + super.toString();
    }
}