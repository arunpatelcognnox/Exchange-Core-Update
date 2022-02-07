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

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.bytes.BytesOut;

import org.eclipse.collections.impl.map.mutable.primitive.IntDoubleHashMap;
import org.eclipse.collections.impl.map.mutable.primitive.IntLongHashMap;
import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;

import com.arun.exchange.core2.core.common.CoreSymbolSpecification;
import com.arun.exchange.core2.core.common.PositionDirection;
import com.arun.exchange.core2.core.common.SymbolType;
import com.arun.exchange.core2.core.processors.MatchingEngineRouter;
import com.arun.exchange.core2.core.processors.RiskEngine;
import com.arun.exchange.core2.core.processors.SymbolSpecificationProvider;
import com.arun.exchange.core2.core.utils.CoreArithmeticUtils;

import java.util.Optional;
import java.util.stream.Stream;

@NoArgsConstructor
@EqualsAndHashCode
@ToString
public final class TotalCurrencyBalanceReportQuery implements ReportQuery<TotalCurrencyBalanceReportResult> {

    public TotalCurrencyBalanceReportQuery(BytesIn bytesIn) {
        // do nothing
    }

    @Override
    public int getReportTypeCode() {
        return ReportType.TOTAL_CURRENCY_BALANCE.getCode();
    }

    @Override
    public TotalCurrencyBalanceReportResult createResult(final Stream<BytesIn> sections) {
        return TotalCurrencyBalanceReportResult.merge(sections);
    }

    @Override
    public Optional<TotalCurrencyBalanceReportResult> process(final MatchingEngineRouter matchingEngine) {

        final IntDoubleHashMap currencyBalance = new IntDoubleHashMap();

        matchingEngine.getOrderBooks().stream()
                .filter(ob -> ob.getSymbolSpec().type == SymbolType.CURRENCY_EXCHANGE_PAIR)
                .forEach(ob -> {
                    final CoreSymbolSpecification spec = ob.getSymbolSpec();

                    currencyBalance.addToValue(
                            spec.getBaseCurrency(),
                            ob.askOrdersStream(false).mapToDouble(ord -> CoreArithmeticUtils.calculateAmountAsk(ord.getSize() - ord.getFilled(), spec)).sum());

                    currencyBalance.addToValue(
                            spec.getQuoteCurrency(),
                            ob.bidOrdersStream(false).mapToDouble(ord -> CoreArithmeticUtils.calculateAmountBidTakerFee(ord.getSize() - ord.getFilled(), ord.getReserveBidPrice(), spec)).sum());
                });

        return Optional.of(TotalCurrencyBalanceReportResult.ofOrderBalances(currencyBalance));
    }

    @Override
    public Optional<TotalCurrencyBalanceReportResult> process(final RiskEngine riskEngine) {

        // prepare fast price cache for profit estimation with some price (exact value is not important, except ask==bid condition)
        final IntObjectHashMap<RiskEngine.LastPriceCacheRecord> dummyLastPriceCache = new IntObjectHashMap<>();
        riskEngine.getLastPriceCache().forEachKeyValue((s, r) -> dummyLastPriceCache.put(s, r.averagingRecord()));

        final IntDoubleHashMap currencyBalance = new IntDoubleHashMap();

        final IntDoubleHashMap symbolOpenInterestLong = new IntDoubleHashMap();
        final IntDoubleHashMap symbolOpenInterestShort = new IntDoubleHashMap();

        final SymbolSpecificationProvider symbolSpecificationProvider = riskEngine.getSymbolSpecificationProvider();

        riskEngine.getUserProfileService().getUserProfiles().forEach(userProfile -> {
            userProfile.accounts.forEachKeyValue(currencyBalance::addToValue);
            userProfile.positions.forEachKeyValue((symbolId, positionRecord) -> {
                final CoreSymbolSpecification spec = symbolSpecificationProvider.getSymbolSpecification(symbolId);
                final RiskEngine.LastPriceCacheRecord avgPrice = dummyLastPriceCache.getIfAbsentPut(symbolId, RiskEngine.LastPriceCacheRecord.dummy);
                currencyBalance.addToValue(positionRecord.currency, positionRecord.estimateProfit(spec, avgPrice));

                if (positionRecord.direction == PositionDirection.LONG) {
                    symbolOpenInterestLong.addToValue(symbolId, positionRecord.openVolume);
                } else if (positionRecord.direction == PositionDirection.SHORT) {
                    symbolOpenInterestShort.addToValue(symbolId, positionRecord.openVolume);
                }
            });
        });

        return Optional.of(
                new TotalCurrencyBalanceReportResult(
                        currencyBalance,
                        new IntDoubleHashMap(riskEngine.getFees()),
                        new IntDoubleHashMap(riskEngine.getAdjustments()),
                        new IntDoubleHashMap(riskEngine.getSuspends()),
                        null,
                        symbolOpenInterestLong,
                        symbolOpenInterestShort));
    }

    @Override
    public void writeMarshallable(BytesOut bytes) {
        // do nothing
    }
}
