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
package org.apache.plc4x.java.s7.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import org.apache.plc4x.java.isotp.netty.model.IsoTPMessage;
import org.apache.plc4x.java.isotp.netty.model.tpdus.DataTpdu;
import org.apache.plc4x.java.netty.events.S7ConnectionEvent;
import org.apache.plc4x.java.netty.events.S7ConnectionState;
import org.apache.plc4x.java.s7.netty.model.messages.S7Message;
import org.apache.plc4x.java.s7.netty.model.messages.S7RequestMessage;
import org.apache.plc4x.java.s7.netty.model.messages.S7ResponseMessage;
import org.apache.plc4x.java.s7.netty.model.messages.SetupCommunicationRequestMessage;
import org.apache.plc4x.java.s7.netty.model.params.VarParameter;
import org.apache.plc4x.java.s7.netty.model.params.S7Parameter;
import org.apache.plc4x.java.s7.netty.model.params.SetupCommunicationParameter;
import org.apache.plc4x.java.s7.netty.model.params.items.VarParameterItem;
import org.apache.plc4x.java.s7.netty.model.params.items.S7AnyVarParameterItem;
import org.apache.plc4x.java.s7.netty.model.payloads.S7Payload;
import org.apache.plc4x.java.s7.netty.model.payloads.VarPayload;
import org.apache.plc4x.java.s7.netty.model.payloads.items.VarPayloadItem;
import org.apache.plc4x.java.s7.netty.model.types.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class S7Protocol extends MessageToMessageCodec<IsoTPMessage, S7Message> {

    public static final byte S7_PROTOCOL_MAGIC_NUMBER = 0x32;

    private static final Logger logger = LoggerFactory.getLogger(S7Protocol.class);

    private short maxAmqCaller;
    private short maxAmqCallee;
    private short pduSize;

    public S7Protocol(short requestedMaxAmqCaller, short requestedMaxAmqCallee, short requestedPduSize) {
        this.maxAmqCaller = requestedMaxAmqCaller;
        this.maxAmqCallee = requestedMaxAmqCallee;
        this.pduSize = requestedPduSize;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof S7ConnectionEvent &&
            ((S7ConnectionEvent) evt).getState() == S7ConnectionState.ISO_TP_CONNECTION_RESPONSE_RECEIVED) {
            // Setup Communication
            SetupCommunicationRequestMessage setupCommunicationRequest =
                new SetupCommunicationRequestMessage((short) 7, maxAmqCaller, maxAmqCallee, pduSize);

            ctx.channel().writeAndFlush(setupCommunicationRequest);
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, S7Message in, List<Object> out) {
        logger.debug("S7 Message sent");

        ByteBuf buf = Unpooled.buffer();

        encodeHeader(in, buf);
        encodeParameters(in, buf);
        encodePayloads(in, buf);

        out.add(new DataTpdu(true, (byte) 1, Collections.emptyList(), buf));
    }

    private void encodePayloads(S7Message in, ByteBuf buf) {
        for (S7Payload payload : in.getPayloads()) {
            ParameterType parameterType = payload.getType();
            if (parameterType == ParameterType.READ_VAR || parameterType == ParameterType.WRITE_VAR) {
                VarPayload varPayload = (VarPayload) payload;
                for (VarPayloadItem payloadItem : varPayload.getPayloadItems()) {
                    buf.writeByte(payloadItem.getReturnCode().getCode());
                    buf.writeByte(payloadItem.getDataTransportSize().getCode());
                    buf.writeShort(payloadItem.getData().length);
                    buf.writeBytes(payloadItem.getData());
                }
            }
        }
    }

    private void encodeParameters(S7Message in, ByteBuf buf) {
        for (S7Parameter s7Parameter : in.getParameters()) {
            buf.writeByte(s7Parameter.getType().getCode());
            switch (s7Parameter.getType()) {
                case READ_VAR:
                case WRITE_VAR:
                    encodeReadWriteVar(buf, (VarParameter) s7Parameter);
                    break;
                case SETUP_COMMUNICATION:
                    encodeSetupCommunication(buf, (SetupCommunicationParameter) s7Parameter);
                    break;
                default:
                    logger.error("writing this parameter type not implemented");
            }
        }
    }

    private void encodeHeader(S7Message in, ByteBuf buf) {
        buf.writeByte(S7_PROTOCOL_MAGIC_NUMBER);
        buf.writeByte(in.getMessageType().getCode());
        // Reserved (is always constant 0x0000)
        buf.writeShort((short) 0x0000);
        // PDU Reference (Request Id, generated by the initiating node)
        buf.writeShort(in.getTpduReference());
        // S7 message parameters length
        buf.writeShort(getParametersLength(in.getParameters()));
        // Data field length
        buf.writeShort(getPayloadsLength(in.getPayloads()));
        if (in instanceof S7ResponseMessage) {
            S7ResponseMessage s7ResponseMessage = (S7ResponseMessage) in;
            buf.writeByte(s7ResponseMessage.getErrorClass());
            buf.writeByte(s7ResponseMessage.getErrorCode());
        }
    }

    private void encodeSetupCommunication(ByteBuf buf, SetupCommunicationParameter s7Parameter) {
        // Reserved (is always constant 0x00)
        buf.writeByte((byte) 0x00);
        buf.writeShort(s7Parameter.getMaxAmqCaller());
        buf.writeShort(s7Parameter.getMaxAmqCallee());
        buf.writeShort(s7Parameter.getPduLength());
    }

    private void encodeReadWriteVar(ByteBuf buf, VarParameter s7Parameter) {
        List<VarParameterItem> items = s7Parameter.getItems();
        // ReadRequestItem count (Read one variable at a time)
        buf.writeByte((byte) items.size());
        for (VarParameterItem item : items) {
            VariableAddressingMode addressMode = item.getAddressingMode();
            if (addressMode == VariableAddressingMode.S7ANY) {
                encodeS7AnyParameterItem(buf, (S7AnyVarParameterItem) item);
            } else {
                logger.error("writing this item type not implemented");
            }
        }
    }

    private void encodeS7AnyParameterItem(ByteBuf buf, S7AnyVarParameterItem s7AnyRequestItem) {
        buf.writeByte(s7AnyRequestItem.getSpecificationType().getCode());
        // Length of this item (excluding spec type and length)
        buf.writeByte((byte) 0x0a);
        buf.writeByte(s7AnyRequestItem.getAddressingMode().getCode());
        buf.writeByte(s7AnyRequestItem.getTransportSize().getCode());
        buf.writeShort(s7AnyRequestItem.getNumElements());
        buf.writeShort(s7AnyRequestItem.getDataBlockNumber());
        buf.writeByte(s7AnyRequestItem.getMemoryArea().getCode());
        // A S7 address is 3 bytes long. Unfortunately the byte-offset is NOT located in
        // byte 1 and byte 2 and the bit offset in byte 3. Siemens used the last 3 bits of
        // byte 3 for the bit-offset and the remaining 5 bits of byte 3 to contain the lowest
        // 5 bits of the byte-offset. The highest 5 bits of byte 1 are probably left unused
        // for future extensions.
        buf.writeShort((short) (s7AnyRequestItem.getByteOffset() >> 5));
        buf.writeByte((byte) ((
                (s7AnyRequestItem.getByteOffset() & 0x1F) << 3)
                | (s7AnyRequestItem.getBitOffset() & 0x07)));
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, IsoTPMessage in, List<Object> out) {
        if (logger.isTraceEnabled()) {
            logger.trace("Got Data: {}", ByteBufUtil.hexDump(in.getUserData()));
        }
        ByteBuf userData = in.getUserData();
        if (userData.readableBytes() == 0) {
            return;
        }
        logger.debug("S7 Message received");

        if (userData.readByte() != S7_PROTOCOL_MAGIC_NUMBER) {
            logger.warn("Expecting S7 protocol magic number.");
            if (logger.isDebugEnabled()) {
                logger.debug("Got Data: {}", ByteBufUtil.hexDump(userData));
            }
            return;
        }

        MessageType messageType = MessageType.valueOf(userData.readByte());
        boolean isResponse = messageType == MessageType.ACK_DATA;
        userData.readShort();  // Reserved (is always constant 0x0000)
        short tpduReference = userData.readShort();
        short headerParametersLength = userData.readShort();
        short userDataLength = userData.readShort();
        byte errorClass = 0;
        byte errorCode = 0;
        if (isResponse) {
            errorClass = userData.readByte();
            errorCode = userData.readByte();
        }

        List<S7Parameter> s7Parameters = new LinkedList<>();
        SetupCommunicationParameter setupCommunicationParameter = null;
        VarParameter readWriteVarParameter = null;
        int i = 0;

        while (i < headerParametersLength) {
            S7Parameter parameter = parseParameter(userData, isResponse, headerParametersLength - i);
            s7Parameters.add(parameter);
            if (parameter instanceof SetupCommunicationParameter) {
                setupCommunicationParameter = (SetupCommunicationParameter) parameter;
            }
            if (readWriteVarParameter == null)  {
                readWriteVarParameter = decodeReadWriteParameter(parameter);
            }
            i += getParameterLength(parameter);
        }

        List<S7Payload> s7Payloads = decodePayloads(userData, isResponse, userDataLength, readWriteVarParameter);

        if (isResponse) {
            setupCommunications(ctx, setupCommunicationParameter);
            out.add(new S7ResponseMessage(messageType, tpduReference, s7Parameters, s7Payloads, errorClass, errorCode));
        } else {
            out.add(new S7RequestMessage(messageType, tpduReference, s7Parameters, s7Payloads));
        }
    }

    private void setupCommunications(ChannelHandlerContext ctx, SetupCommunicationParameter setupCommunicationParameter) {
        // If we got a SetupCommunicationParameter as part of the response
        // we are currently in the process of establishing a connection with
        // the PLC, so save some of the information in the session and tell
        // the next layer to negotiate the connection parameters.
        if (setupCommunicationParameter != null) {
            maxAmqCaller = setupCommunicationParameter.getMaxAmqCaller();
            maxAmqCallee = setupCommunicationParameter.getMaxAmqCallee();
            pduSize = setupCommunicationParameter.getPduLength();

            // Send an event that setup is complete.
            ctx.channel().pipeline().fireUserEventTriggered(
                new S7ConnectionEvent(S7ConnectionState.SETUP_COMPLETE));
        }
    }

    private VarParameter decodeReadWriteParameter(S7Parameter parameter) {
        VarParameter readWriteVarParameter = null;

        if (parameter instanceof VarParameter) {
            ParameterType paramType = parameter.getType();
            if (paramType == ParameterType.READ_VAR || paramType == ParameterType.WRITE_VAR) {
                readWriteVarParameter = (VarParameter) parameter;
            }
        }
        return readWriteVarParameter;
    }

    private List<S7Payload> decodePayloads(ByteBuf userData, boolean isResponse, short userDataLength, VarParameter readWriteVarParameter) {
        int i = 0;
        List<S7Payload> s7Payloads = new LinkedList<>();
        if (readWriteVarParameter != null) {
            List<VarPayloadItem> payloadItems = new LinkedList<>();

            while (i < userDataLength) {
                DataTransportErrorCode dataTransportErrorCode = DataTransportErrorCode.valueOf(userData.readByte());
                // This is a response to a WRITE_VAR request (It only contains the return code for every sent item.
                if ((readWriteVarParameter.getType() == ParameterType.WRITE_VAR) && isResponse) {
                    // Initialize a rudimentary payload (This is updated in the Plc4XS7Protocol class
                    VarPayloadItem payload = new VarPayloadItem(dataTransportErrorCode, null, null);
                    payloadItems.add(payload);
                    i += 1;
                }
                // This is a response to a READ_VAR request.
                else if ((readWriteVarParameter.getType() == ParameterType.READ_VAR) && isResponse) {
                    DataTransportSize dataTransportSize = DataTransportSize.valueOf(userData.readByte());
                    short length = (dataTransportSize.isSizeInBits()) ?
                        (short) Math.ceil(userData.readShort() / 8.0) : userData.readShort();
                    byte[] data = new byte[length];
                    userData.readBytes(data);
                    // Initialize a rudimentary payload (This is updated in the Plc4XS7Protocol class
                    VarPayloadItem payload = new VarPayloadItem(dataTransportErrorCode, dataTransportSize, data);
                    payloadItems.add(payload);
                    i += getPayloadLength(payload);
                }
            }

            VarPayload varPayload = new VarPayload(readWriteVarParameter.getType(), payloadItems);
            s7Payloads.add(varPayload);
        }
        return s7Payloads;
    }

    private S7Parameter parseParameter(ByteBuf in, boolean isResponse, int restLength) {
        ParameterType parameterType = ParameterType.valueOf(in.readByte());
        if (parameterType == null) {
            logger.error("Could not find parameter type");
            return null;
        }
        switch (parameterType) {
            case CPU_SERVICES:
                // Just read in the rest of the header as content of this parameter.
                // Will have to do a lot more investigation on how this parameter is
                // constructed.
                byte[] cpuServices = new byte[restLength - 1];
                in.readBytes(cpuServices);
                return null;
            case READ_VAR:
            case WRITE_VAR:
                List<VarParameterItem> varParamameter;
                byte numItems = in.readByte();
                if (!isResponse) {
                    varParamameter = parseReadWriteVarParameter(in, numItems);
                } else {
                    varParamameter = Collections.emptyList();
                }
                return new VarParameter(parameterType, varParamameter);
            case SETUP_COMMUNICATION:
                // Reserved (is always constant 0x00)
                in.readByte();
                short callingMaxAmq = in.readShort();
                short calledMaxAmq = in.readShort();
                short pduLength = in.readShort();
                return new SetupCommunicationParameter(callingMaxAmq, calledMaxAmq, pduLength);
            default:
                if (logger.isErrorEnabled()) {
                    logger.error("Unimplemented parameter type: {}", parameterType.name());
                }
        }
        return null;
    }

    private List<VarParameterItem> parseReadWriteVarParameter(ByteBuf in, byte numItems) {
        List<VarParameterItem> items = new LinkedList<>();
        for (int i = 0; i < numItems; i++) {
            SpecificationType specificationType = SpecificationType.valueOf(in.readByte());
            // Length of the rest of this item.
            byte itemLength = in.readByte();
            if (itemLength != 0x0a) {
                logger.warn("Expecting a length of 10 here.");
                return items;
            }
            VariableAddressingMode variableAddressingMode = VariableAddressingMode.valueOf(in.readByte());
            if (variableAddressingMode == VariableAddressingMode.S7ANY) {
                TransportSize transportSize = TransportSize.valueOf(in.readByte());
                short length = in.readShort();
                short dbNumber = in.readShort();
                MemoryArea memoryArea = MemoryArea.valueOf(in.readByte());
                short byteAddress = (short) (in.readShort() << 5);
                byte tmp = in.readByte();
                // Only the least 3 bits are the bit address, the
                byte bitAddress = (byte) (tmp & 0x07);
                // Bits 4-8 belong to the byte address
                byteAddress = (short) (byteAddress | (tmp >> 3));
                S7AnyVarParameterItem item = new S7AnyVarParameterItem(
                        specificationType, memoryArea, transportSize,
                        length, dbNumber, byteAddress, bitAddress);
                items.add(item);
            } else {
                logger.error("Error parsing item type");
                return items;
            }
        }

        return items;
    }

    private short getParametersLength(List<S7Parameter> parameters) {
        short l = 0;
        if (parameters != null) {
            for (S7Parameter parameter : parameters) {
                l += getParameterLength(parameter);
            }
        }
        return l;
    }

    private short getPayloadsLength(List<S7Payload> payloads) {
        short l = 0;
        if (payloads == null) {
            return 0;
        }

        for (S7Payload payload : payloads) {
            if(payload instanceof VarPayload) {
                VarPayload varPayload = (VarPayload) payload;
                for (VarPayloadItem payloadItem : varPayload.getPayloadItems()) {
                    l += getPayloadLength(payloadItem);
                }
            }
        }
        return l;
    }

    private short getParameterLength(S7Parameter parameter) {
        if (parameter == null) {
            return 0;
        }

        switch (parameter.getType()) {
            case READ_VAR:
            case WRITE_VAR:
                return getReadWriteVarParameterLength((VarParameter) parameter);
            case SETUP_COMMUNICATION:
                return 8;
            default:
                logger.error("Not implemented");
                return 0;
        }
    }

    private short getReadWriteVarParameterLength(VarParameter parameter) {
        short length = 2;
        for (VarParameterItem varParameterItem : parameter.getItems()) {
            VariableAddressingMode addressMode = varParameterItem.getAddressingMode();

            if (addressMode == VariableAddressingMode.S7ANY) {
                length += 12;
            } else {
                logger.error("Not implemented");
            }
        }
        return length;
    }

    private short getPayloadLength(VarPayloadItem payloadItem) {
        if (payloadItem == null) {
            return 0;
        }
        return (short) (4 + payloadItem.getData().length);
    }

}
