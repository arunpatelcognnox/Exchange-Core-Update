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


import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.bytes.BytesOut;

import org.eclipse.collections.impl.map.mutable.primitive.IntDoubleHashMap;
import org.eclipse.collections.impl.map.mutable.primitive.IntLongHashMap;

import com.arun.exchange.core2.core.utils.SerializationUtils;

import java.util.stream.Stream;

@AllArgsConstructor
@EqualsAndHashCode
@Getter
@ToString
public final class TotalCurrencyBalanceReportResult implements ReportResult {

    // currency -> balance
    final private IntDoubleHashMap accountBalances;
    final private IntDoubleHashMap fees;
    final private IntDoubleHashMap adjustments;
    final private IntDoubleHashMap suspends;
    final private IntDoubleHashMap ordersBalances;

    // symbol -> volume
    // We have to keep shorts and longs separately because for multi-core processing different risk engine instances will give non-matching results.
    // They should match when aggregated though.
    final private IntDoubleHashMap openInterestLong;
    final private IntDoubleHashMap openInterestShort;

    public static TotalCurrencyBalanceReportResult createEmpty() {
        return new TotalCurrencyBalanceReportResult(
                null, null, null, null, null, null, null);
    }

    public static TotalCurrencyBalanceReportResult ofOrderBalances(final IntDoubleHashMap currencyBalance) {
        return new TotalCurrencyBalanceReportResult(
                null, null, null, null, currencyBalance, null, null);
    }

    private TotalCurrencyBalanceReportResult(final BytesIn bytesIn) {
        this.accountBalances = SerializationUtils.readNullable(bytesIn, SerializationUtils::readIntDoubleHashMap);
        this.fees = SerializationUtils.readNullable(bytesIn, SerializationUtils::readIntDoubleHashMap);
        this.adjustments = SerializationUtils.readNullable(bytesIn, SerializationUtils::readIntDoubleHashMap);
        this.suspends = SerializationUtils.readNullable(bytesIn, SerializationUtils::readIntDoubleHashMap);
        this.ordersBalances = SerializationUtils.readNullable(bytesIn, SerializationUtils::readIntDoubleHashMap);
        this.openInterestLong = SerializationUtils.readNullable(bytesIn, SerializationUtils::readIntDoubleHashMap);
        this.openInterestShort = SerializationUtils.readNullable(bytesIn, SerializationUtils::readIntDoubleHashMap);
    }

    @Override
    public void writeMarshallable(final BytesOut bytes) {
        SerializationUtils.marshallNullable(accountBalances, bytes, SerializationUtils::marshallIntDoubleHashMap);
        SerializationUtils.marshallNullable(fees, bytes, SerializationUtils::marshallIntDoubleHashMap);
        SerializationUtils.marshallNullable(adjustments, bytes, SerializationUtils::marshallIntDoubleHashMap);
        SerializationUtils.marshallNullable(suspends, bytes, SerializationUtils::marshallIntDoubleHashMap);
        SerializationUtils.marshallNullable(ordersBalances, bytes, SerializationUtils::marshallIntDoubleHashMap);
        SerializationUtils.marshallNullable(openInterestLong, bytes, SerializationUtils::marshallIntDoubleHashMap);
        SerializationUtils.marshallNullable(openInterestShort, bytes, SerializationUtils::marshallIntDoubleHashMap);
    }

    public IntDoubleHashMap getGlobalBalancesSum() {
        return SerializationUtils.mergeSum(accountBalances, ordersBalances, fees, adjustments, suspends);
    }

    public IntDoubleHashMap getClientsBalancesSum() {
        return SerializationUtils.mergeSum(accountBalances, ordersBalances, suspends);
    }

    public boolean isGlobalBalancesAllZero() {
        return getGlobalBalancesSum().allSatisfy(amount -> amount == 0L);
    }

    public static TotalCurrencyBalanceReportResult merge(final Stream<BytesIn> pieces) {
        return pieces
                .map(TotalCurrencyBalanceReportResult::new)
                .reduce(
                        TotalCurrencyBalanceReportResult.createEmpty(),
                        (a, b) -> new TotalCurrencyBalanceReportResult(
                                SerializationUtils.mergeSum(a.accountBalances, b.accountBalances),
                                SerializationUtils.mergeSum(a.fees, b.fees),
                                SerializationUtils.mergeSum(a.adjustments, b.adjustments),
                                SerializationUtils.mergeSum(a.suspends, b.suspends),
                                SerializationUtils.mergeSum(a.ordersBalances, b.ordersBalances),
                                SerializationUtils.mergeSum(a.openInterestLong, b.openInterestLong),
                                SerializationUtils.mergeSum(a.openInterestShort, b.openInterestShort)));
    }

}
