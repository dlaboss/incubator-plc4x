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
import org.apache.plc4x.scala.api.PlcConnectionError
import org.apache.plc4x.scala.api.connection.PlcConnection

import scala.util.{Failure, Success, Try}

private[core] class JPlcConnectionWrapper(val jPlcConnection: JPlcConnection) extends PlcConnection {

    override def connect() = Try(jPlcConnection.connect()) match {
        case Success(_) => Right(Unit)
        case Failure(ex) => Left(PlcConnectionError(ex.getMessage))
    }

    override def parseAddress(addressString: String) = Try(jPlcConnection.parseAddress(addressString)) match {
        case Success(address) => Right(address)
        case Failure(ex) => Left(PlcConnectionError(ex.getMessage))
    }
}
