/*
 * Copyright 2019 Maksim Zheravin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.arun.exchange.core2.core.processors;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import org.eclipse.collections.impl.map.mutable.primitive.DoubleObjectHashMap;

import com.arun.exchange.core2.core.ExchangeApi;
import com.arun.exchange.core2.core.common.MatcherTradeEvent;
import com.arun.exchange.core2.core.common.StateHash;
import com.arun.exchange.core2.core.common.api.binary.BinaryDataCommand;
import com.arun.exchange.core2.core.common.api.reports.ReportQueriesHandler;
import com.arun.exchange.core2.core.common.api.reports.ReportQuery;
import com.arun.exchange.core2.core.common.cmd.CommandResultCode;
import com.arun.exchange.core2.core.common.cmd.OrderCommand;
import com.arun.exchange.core2.core.common.cmd.OrderCommandType;
import com.arun.exchange.core2.core.common.config.ReportsQueriesConfiguration;
import com.arun.exchange.core2.core.orderbook.OrderBookEventsHelper;
import com.arun.exchange.core2.core.utils.SerializationUtils;
import com.arun.exchange.core2.core.utils.UnsafeUtils;

import lombok.extern.slf4j.Slf4j;
import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.bytes.BytesOut;
import net.openhft.chronicle.bytes.NativeBytes;
import net.openhft.chronicle.bytes.WriteBytesMarshallable;

/**
 * Stateful Binary Commands Processor
 * <p>
 * Has incoming data buffer
 * Can receive events in arbitrary order and duplicates - at-least-once-delivery compatible.
 */
@Slf4j
public final class BinaryCommandsProcessor implements WriteBytesMarshallable, StateHash {

    // TODO connect object pool

    // transactionId -> TransferRecord (long array + bitset)
    private final DoubleObjectHashMap<TransferRecord> incomingData;

    // TODO improve type (Object is not ok)
    private final Consumer<BinaryDataCommand> completeMessagesHandler;

    private final ReportQueriesHandler reportQueriesHandler;

    private final OrderBookEventsHelper eventsHelper;

    private final ReportsQueriesConfiguration queriesConfiguration;

    private final int section;

    public BinaryCommandsProcessor(final Consumer<BinaryDataCommand> completeMessagesHandler,
                                   final ReportQueriesHandler reportQueriesHandler,
                                   final SharedPool sharedPool,
                                   final ReportsQueriesConfiguration queriesConfiguration,
                                   final int section) {
        this.completeMessagesHandler = completeMessagesHandler;
        this.reportQueriesHandler = reportQueriesHandler;
        this.incomingData = new DoubleObjectHashMap<>();
        this.eventsHelper = new OrderBookEventsHelper(sharedPool::getChain);
        this.queriesConfiguration = queriesConfiguration;
        this.section = section;
    }

    public BinaryCommandsProcessor(final Consumer<BinaryDataCommand> completeMessagesHandler,
                                   final ReportQueriesHandler reportQueriesHandler,
                                   final SharedPool sharedPool,
                                   final ReportsQueriesConfiguration queriesConfiguration,
                                   final BytesIn bytesIn,
                                   int section) {
        this.completeMessagesHandler = completeMessagesHandler;
        this.reportQueriesHandler = reportQueriesHandler;
        this.incomingData = SerializationUtils.readDoubleHashMap(bytesIn, b -> new TransferRecord(bytesIn));
        this.eventsHelper = new OrderBookEventsHelper(sharedPool::getChain);
        this.section = section;
        this.queriesConfiguration = queriesConfiguration;
    }

    public CommandResultCode acceptBinaryFrame(OrderCommand cmd) {

        final int transferId = cmd.userCookie;

        final TransferRecord record = incomingData.getIfAbsentPut(
                transferId,
                () -> {
                    final int bytesLength = (int) (cmd.orderId >> 32) & 0x7FFF_FFFF;
                    final int longArraySize = SerializationUtils.requiredLongArraySize(bytesLength, ExchangeApi.LONGS_PER_MESSAGE);
//            log.debug("EXPECTED: bytesLength={} longArraySize={}", bytesLength, longArraySize);
                    return new TransferRecord(longArraySize);
                });

        record.addWord(cmd.orderId);
        record.addWord(cmd.price);
        record.addWord(cmd.reserveBidPrice);
        record.addWord(cmd.size);
        record.addWord(cmd.uid);

        if (cmd.symbol == -1) {
            // all frames received

            incomingData.removeKey(transferId);

            final BytesIn bytesIn = SerializationUtils.longsLz4ToWire(record.dataArray, record.wordsTransfered).bytes();

            if (cmd.command == OrderCommandType.BINARY_DATA_QUERY) {

                deserializeQuery(bytesIn)
                        .flatMap(reportQueriesHandler::handleReport)
                        .ifPresent(res -> {
                            final NativeBytes<Void> bytes = Bytes.allocateElasticDirect(128);
                            res.writeMarshallable(bytes);
                            final MatcherTradeEvent binaryEventsChain = eventsHelper.createBinaryEventsChain(cmd.timestamp, section, bytes);
                            UnsafeUtils.appendEventsVolatile(cmd, binaryEventsChain);
                        });

            } else if (cmd.command == OrderCommandType.BINARY_DATA_COMMAND) {

//                log.debug("Unpack {} words", record.wordsTransfered);
                final BinaryDataCommand binaryDataCommand = deserializeBinaryCommand(bytesIn);
//                log.debug("Succeed");
                completeMessagesHandler.accept(binaryDataCommand);

            } else {
                throw new IllegalStateException();
            }


            return CommandResultCode.SUCCESS;
        } else {
            return CommandResultCode.ACCEPTED;
        }
    }

    private BinaryDataCommand deserializeBinaryCommand(BytesIn bytesIn) {
System.out.println("bytesIn = > "+bytesIn);
        final int classCode = bytesIn.readInt();

        final Constructor<? extends BinaryDataCommand> constructor = queriesConfiguration.getBinaryCommandConstructors().get(classCode);
        if (constructor == null) {
            throw new IllegalStateException("Unknown Binary Data Command class code: " + classCode);
        }

        try {
            return constructor.newInstance(bytesIn);

        } catch (final IllegalAccessException | InstantiationException | InvocationTargetException ex) {
            throw new IllegalStateException("Failed to deserialize Binary Data Command instance of class " + constructor.getDeclaringClass().getSimpleName(), ex);
        }
    }

    private Optional<ReportQuery<?>> deserializeQuery(BytesIn bytesIn) {

        final int classCode = bytesIn.readInt();

        final Constructor<? extends ReportQuery<?>> constructor = queriesConfiguration.getReportConstructors().get(classCode);
        if (constructor == null) {
            log.error("Unknown Report Query class code: {}", classCode);
            return Optional.empty();
        }

        try {
            return Optional.of(constructor.newInstance(bytesIn));

        } catch (final IllegalAccessException | InstantiationException | InvocationTargetException ex) {
            log.error("Failed to deserialize report instance of class {} error: {}", constructor.getDeclaringClass().getSimpleName(), ex.getMessage());
            return Optional.empty();
        }
    }

    public static NativeBytes<Void> serializeObject(WriteBytesMarshallable data, int objectType) {
        final NativeBytes<Void> bytes = Bytes.allocateElasticDirect(128);
        bytes.writeInt(objectType);
        data.writeMarshallable(bytes);
        return bytes;
    }

    public void reset() {
        incomingData.clear();
    }

    @Override
    public void writeMarshallable(BytesOut bytes) {

        // write symbolSpecs
        SerializationUtils.marshallDoubleHashMap(incomingData, bytes);
    }

    @Override
    public int stateHash() {
    	System.out.println("call startehash");
    	return 0;
        //return HashingUtils.stateHash(incomingData);
    }


    private static class TransferRecord implements WriteBytesMarshallable, StateHash {

        private double[] dataArray;
        private int wordsTransfered;

        public TransferRecord(int expectedLength) {
            System.out.println("size =>"+expectedLength);
            if(expectedLength==0)
            	expectedLength = 15;
        	this.wordsTransfered = 0;
            this.dataArray = new double[expectedLength];
        }

        public TransferRecord(BytesIn bytes) {
            wordsTransfered = bytes.readInt();
            this.dataArray = SerializationUtils.readDoubleArray(bytes);
        }

        public void addWord(double word) {

            if (wordsTransfered == dataArray.length) {
                // should never happen
                log.warn("Resizing incoming transfer buffer to {} longs", dataArray.length * 2);
                double[] newArray = new double[dataArray.length * 8];
                System.arraycopy(dataArray, 0, newArray, 0, dataArray.length);
                dataArray = newArray;
            }

            dataArray[wordsTransfered++] = word;

        }

        @Override
        public void writeMarshallable(BytesOut bytes) {
            bytes.writeInt(wordsTransfered);
            SerializationUtils.marshallDoubleArray(dataArray, bytes);
        }

        @Override
        public int stateHash() {
            return Objects.hash(Arrays.hashCode(dataArray), wordsTransfered);
        }
    }

}
