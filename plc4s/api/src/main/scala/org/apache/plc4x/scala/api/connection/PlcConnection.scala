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
package org.apache.plc4x.scala.api.connection

import org.apache.plc4x.java.messages.Address
import org.apache.plc4x.scala.api.PlcError

/**
  * Interface defining the most basic methods a PLC4X connection should support.
  * This generally handles the connection establishment itself and the parsing of
  * address strings to the platform dependent Address instances.
  *
  * The individual operations are then defined by other interfaces within this package.
  */
trait PlcConnection {

    /**
      * Establishes the connection to the remote PLC.
      *
      * @return Either nothing in case the connection could be established or an PlcError.
      */
    def connect(): Either[PlcError, Unit]

    /**
      * Parses a PLC/protocol dependent address string into an Address object.
      *
      * @param addressString String representation of an address for the current type of PLC/protocol.
      * @return Either the Address object identifying an address for the current type of PLC/protocol or a PlcError.
      */
    def parseAddress(addressString: String): Either[PlcError, Address]

}
