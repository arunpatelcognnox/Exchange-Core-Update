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
package com.arun.exchange.core2.core.common.api.reports;


import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.collections.impl.map.mutable.primitive.IntDoubleHashMap;
import org.eclipse.collections.impl.map.mutable.primitive.IntLongHashMap;
import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;

import com.arun.exchange.core2.core.common.Order;
import com.arun.exchange.core2.core.common.PositionDirection;
import com.arun.exchange.core2.core.common.UserStatus;
import com.arun.exchange.core2.core.utils.SerializationUtils;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.bytes.BytesOut;
import net.openhft.chronicle.bytes.WriteBytesMarshallable;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode
@Getter
@Slf4j
public final class SingleUserReportResult implements ReportResult {

    public static SingleUserReportResult IDENTITY = new SingleUserReportResult(0L, null, null, null, null, QueryExecutionStatus.OK);

    private final long uid;

    // risk engine: user profile from
    //private final UserProfile userProfile;

    private final UserStatus userStatus;
    private final IntDoubleHashMap accounts;
    private final IntObjectHashMap<Position> positions;

    // matching engine: orders placed by user
    // symbol -> orders
    private final IntObjectHashMap<List<Order>> orders;

    // status
    private final QueryExecutionStatus queryExecutionStatus;


    public static SingleUserReportResult createFromMatchingEngine(long uid, IntObjectHashMap<List<Order>> orders) {
        return new SingleUserReportResult(uid, null, null, null, orders, QueryExecutionStatus.OK);
    }

    public static SingleUserReportResult createFromRiskEngineFound(long uid, UserStatus userStatus, IntDoubleHashMap accounts, IntObjectHashMap<Position> positions) {
        return new SingleUserReportResult(uid, userStatus, accounts, positions, null, QueryExecutionStatus.OK);
    }

    public static SingleUserReportResult createFromRiskEngineNotFound(long uid) {
        return new SingleUserReportResult(uid, null, null, null, null, QueryExecutionStatus.USER_NOT_FOUND);
    }

    public Map<Long, Order> fetchIndexedOrders() {
        return orders.stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toMap(Order::getOrderId, ord -> ord));
    }

    private SingleUserReportResult(final BytesIn bytesIn) {
        this.uid = bytesIn.readLong();
//        this.userProfile = bytesIn.readBoolean() ? new UserProfile(bytesIn) : null;
        this.userStatus = bytesIn.readBoolean() ? UserStatus.of(bytesIn.readByte()) : null;
        this.accounts = bytesIn.readBoolean() ? SerializationUtils.readIntDoubleHashMap(bytesIn) : null;
        this.positions = bytesIn.readBoolean() ? SerializationUtils.readIntHashMap(bytesIn, Position::new) : null;
        this.orders = bytesIn.readBoolean() ? SerializationUtils.readIntHashMap(bytesIn, b -> SerializationUtils.readList(b, Order::new)) : null;
        this.queryExecutionStatus = QueryExecutionStatus.of(bytesIn.readInt());
    }

    @Override
    public void writeMarshallable(final BytesOut bytes) {

        bytes.writeLong(uid);

//        bytes.writeBoolean(userProfile != null);
//        if (userProfile != null) {
//            userProfile.writeMarshallable(bytes);
//        }

        bytes.writeBoolean(userStatus != null);
        if (userStatus != null) {
            bytes.writeByte(userStatus.getCode());
        }

        bytes.writeBoolean(accounts != null);
        if (accounts != null) {
            SerializationUtils.marshallIntDoubleHashMap(accounts, bytes);
        }

        bytes.writeBoolean(positions != null);
        if (positions != null) {
            SerializationUtils.marshallIntHashMap(positions, bytes);
        }

        bytes.writeBoolean(orders != null);
        if (orders != null) {
            SerializationUtils.marshallIntHashMap(orders, bytes, symbolOrders -> SerializationUtils.marshallList(symbolOrders, bytes));
        }
        bytes.writeInt(queryExecutionStatus.code);

    }

    public enum QueryExecutionStatus {
        OK(0),
        USER_NOT_FOUND(1);

        private final int code;

        QueryExecutionStatus(int code) {
            this.code = code;
        }

        public static QueryExecutionStatus of(int code) {
            switch (code) {
                case 0:
                    return OK;
                case 1:
                    return USER_NOT_FOUND;
                default:
                    throw new IllegalArgumentException("unknown ExecutionStatus:" + code);
            }
        }
    }

    public static SingleUserReportResult merge(final Stream<BytesIn> pieces) {
        return pieces
                .map(SingleUserReportResult::new)
                .reduce(
                        IDENTITY,
                        (a, b) -> new SingleUserReportResult(
                                a.uid,
//                                SerializationUtils.preferNotNull(a.userProfile, b.userProfile),
                                SerializationUtils.preferNotNull(a.userStatus, b.userStatus),
                                SerializationUtils.preferNotNull(a.accounts, b.accounts),
                                SerializationUtils.preferNotNull(a.positions, b.positions),
                                SerializationUtils.mergeOverride(a.orders, b.orders),
                                a.queryExecutionStatus != QueryExecutionStatus.OK ? a.queryExecutionStatus : b.queryExecutionStatus));
    }

    @RequiredArgsConstructor
    @Getter
    public static class Position implements WriteBytesMarshallable {

        public final int quoteCurrency;
        // open positions state (for margin trades only)
        public final PositionDirection direction;
        public final double openVolume;
        public final double openPriceSum;
        public final double profit;

        // pending orders total size
        public final double pendingSellSize;
        public final double pendingBuySize;

        private Position(BytesIn bytes) {

            this.quoteCurrency = bytes.readInt();

            this.direction = PositionDirection.of(bytes.readByte());
            this.openVolume = bytes.readDouble();
            this.openPriceSum = bytes.readDouble();
            this.profit = bytes.readDouble();

            this.pendingSellSize = bytes.readDouble();
            this.pendingBuySize = bytes.readDouble();
        }

        @Override
        public void writeMarshallable(BytesOut bytes) {
            bytes.writeInt(quoteCurrency);
            bytes.writeByte((byte) direction.getMultiplier());
            bytes.writeDouble(openVolume);
            bytes.writeDouble(openPriceSum);
            bytes.writeDouble(profit);
            bytes.writeDouble(pendingSellSize);
            bytes.writeDouble(pendingBuySize);
        }
    }


    @Override
    public String toString() {
        return "SingleUserReportResult{" +
                "userProfile=" + userStatus +
                ", accounts=" + accounts +
                ", orders=" + orders +
                ", queryExecutionStatus=" + queryExecutionStatus +
                '}';
    }
}
