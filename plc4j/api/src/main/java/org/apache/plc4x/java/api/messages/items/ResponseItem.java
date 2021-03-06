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
package org.apache.plc4x.java.api.messages.items;

import org.apache.plc4x.java.api.types.ResponseCode;

import java.util.Objects;

public abstract class ResponseItem<REQUEST_ITEM extends RequestItem> {

    private final REQUEST_ITEM requestItem;

    private final ResponseCode responseCode;

    public ResponseItem(REQUEST_ITEM requestItem, ResponseCode responseCode) {
        Objects.requireNonNull(requestItem,"Request item must not be null");
        Objects.requireNonNull(responseCode,"Response code must not be null");
        this.requestItem = requestItem;
        this.responseCode = responseCode;
    }

    public REQUEST_ITEM getRequestItem() {
        return requestItem;
    }

    public ResponseCode getResponseCode() {
        return responseCode;
    }
}
