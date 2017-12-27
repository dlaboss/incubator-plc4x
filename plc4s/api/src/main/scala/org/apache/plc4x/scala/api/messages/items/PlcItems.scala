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
package org.apache.plc4x.scala.api.messages.items

import org.apache.plc4x.java.api.model.Address
import org.apache.plc4x.java.api.types.ResponseCode
import scala.collection.immutable.List

case class ReadRequestItem(datatype: Class[_], address: Address, size: Int)
object ReadRequestItem{
    def apply(datatype: Class[_], address: Address, size: Int): ReadRequestItem =
        new ReadRequestItem(datatype, address, size)
    def apply(datatype: Class[_], address: Address): ReadRequestItem =
        new ReadRequestItem(datatype, address, size = 1)
}

case class ReadResponseItem(readRequestItem: ReadRequestItem, responseCode: ResponseCode, values: List[AnyRef])
object ReadResponseItem{
    def apply(readRequestItem: ReadRequestItem, responseCode: ResponseCode, values: List[AnyRef]): ReadResponseItem =
        new ReadResponseItem(readRequestItem, responseCode, values)
}