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
package org.apache.plc4x.scala.core

import org.apache.plc4x.java.connection.{PlcConnection => JPlcConnection}
import org.apache.plc4x.scala.api.connection.PlcConnection

private[core] class JPlcConnectionWrapper(val jPlcConnection: JPlcConnection) extends PlcConnection  {
    /**
      * Establishes the connection to the remote PLC.
      *
      * @return Either nothing in case the connection could be established or an PlcError.
      */
    override def connect() = ???

    /**
      * Parses a PLC/protocol dependent address string into an Address object.
      *
      * @param addressString String representation of an address for the current type of PLC/protocol.
      * @return Either the Address object identifying an address for the current type of PLC/protocol or a PlcError.
      */
    override def parseAddress(addressString: String) = ???
}
