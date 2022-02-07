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
package com.arun.exchange.core2.core.common;


import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.bytes.BytesOut;
import net.openhft.chronicle.bytes.WriteBytesMarshallable;

import java.util.Objects;

import com.arun.exchange.core2.core.processors.RiskEngine;

@Slf4j
@NoArgsConstructor
public final class SymbolPositionRecord implements WriteBytesMarshallable, StateHash {

    public long uid;

    public int symbol;
    public int currency;

    // open positions state (for margin trades only)
    public PositionDirection direction = PositionDirection.EMPTY;
    public double openVolume = 0;
    public double openPriceSum = 0; //
    public double profit = 0;

    // pending orders total size
    // increment before sending order to matching engine
    // decrement after receiving trade confirmation from matching engine
    public long pendingSellSize = 0;
    public long pendingBuySize = 0;

    public void initialize(long uid, int symbol, int currency) {
        this.uid = uid;

        this.symbol = symbol;
        this.currency = currency;

        this.direction = PositionDirection.EMPTY;
        this.openVolume = 0;
        this.openPriceSum = 0;
        this.profit = 0;

        this.pendingSellSize = 0;
        this.pendingBuySize = 0;
    }

    public SymbolPositionRecord(long uid, BytesIn bytes) {
        this.uid = uid;

        this.symbol = bytes.readInt();
        this.currency = bytes.readInt();

        this.direction = PositionDirection.of(bytes.readByte());
        this.openVolume = bytes.readLong();
        this.openPriceSum = bytes.readLong();
        this.profit = bytes.readLong();

        this.pendingSellSize = bytes.readLong();
        this.pendingBuySize = bytes.readLong();
    }


    /**
     * Check if position is empty (no pending orders, no open trades) - can remove it from hashmap
     *
     * @return true if position is empty (no pending orders, no open trades)
     */
    public boolean isEmpty() {
        return direction == PositionDirection.EMPTY
                && pendingSellSize == 0
                && pendingBuySize == 0;
    }

    public void pendingHold(OrderAction orderAction, double size) {
        if (orderAction == OrderAction.ASK) {
            pendingSellSize += size;
        } else {
            pendingBuySize += size;
        }
    }

    public void pendingRelease(OrderAction orderAction, double size) {
        if (orderAction == OrderAction.ASK) {
            pendingSellSize -= size;
        } else {
            pendingBuySize -= size;
        }

//        if (pendingSellSize < 0 || pendingBuySize < 0) {
//            log.error("uid {} : pendingSellSize:{} pendingBuySize:{}", uid, pendingSellSize, pendingBuySize);
//        }
    }

    public double estimateProfit(final CoreSymbolSpecification spec, final RiskEngine.LastPriceCacheRecord lastPriceCacheRecord) {
        switch (direction) {
            case EMPTY:
                return profit;
            case LONG:
                return profit + ((lastPriceCacheRecord != null && lastPriceCacheRecord.bidPrice != 0)
                        ? (openVolume * lastPriceCacheRecord.bidPrice - openPriceSum)
                        : spec.marginBuy * openVolume); // unknown price - no liquidity - require extra margin
            case SHORT:
                return profit + ((lastPriceCacheRecord != null && lastPriceCacheRecord.askPrice != Long.MAX_VALUE)
                        ? (openPriceSum - openVolume * lastPriceCacheRecord.askPrice)
                        : spec.marginSell * openVolume); // unknown price - no liquidity - require extra margin
            default:
                throw new IllegalStateException();
        }
    }

    /**
     * Calculate required margin based on specification and current position/orders
     *
     * @param spec core symbol specification
     * @return required margin
     */
    public double calculateRequiredMarginForFutures(CoreSymbolSpecification spec) {
        final double specMarginBuy = spec.marginBuy;
        final double specMarginSell = spec.marginSell;

        final double signedPosition = openVolume * direction.getMultiplier();
        final double currentRiskBuySize = pendingBuySize + signedPosition;
        final double currentRiskSellSize = pendingSellSize - signedPosition;

        final double marginBuy = specMarginBuy * currentRiskBuySize;
        final double marginSell = specMarginSell * currentRiskSellSize;
        // marginBuy or marginSell can be negative, but not both of them
        return Math.max(marginBuy, marginSell);
    }

    /**
     * Calculate required margin based on specification and current position/orders
     * considering extra size added to current position (or outstanding orders)
     *
     * @param spec   symbols specification
     * @param action order action
     * @param size   order size
     * @return -1 if order will reduce current exposure (no additional margin required), otherwise full margin for symbol position if order placed/executed
     */
    public double calculateRequiredMarginForOrder(final CoreSymbolSpecification spec, final OrderAction action, final double size) {
        final double specMarginBuy = spec.marginBuy;
        final double specMarginSell = spec.marginSell;

        final double signedPosition = openVolume * direction.getMultiplier();
        final double currentRiskBuySize = pendingBuySize + signedPosition;
        final double currentRiskSellSize = pendingSellSize - signedPosition;

        double marginBuy = specMarginBuy * currentRiskBuySize;
        double marginSell = specMarginSell * currentRiskSellSize;
        // either marginBuy or marginSell can be negative (because of signedPosition), but not both of them
        final double currentMargin = Math.max(marginBuy, marginSell);

        if (action == OrderAction.BID) {
            marginBuy += spec.marginBuy * size;
        } else {
            marginSell += spec.marginSell * size;
        }

        // marginBuy or marginSell can be negative, but not both of them
        final double newMargin = Math.max(marginBuy, marginSell);

        return (newMargin <= currentMargin) ? -1 : newMargin;
    }


    /**
     * Update position for one user
     * 1. Un-hold pending size
     * 2. Reduce opposite position accordingly (if exists)
     * 3. Increase forward position accordingly (if size left in the trading event)
     *
     * @param action order action
     * @param size   order size
     * @param price  order price
     * @return opened size
     */
    public double updatePositionForMarginTrade(OrderAction action, double size, double price) {

        // 1. Un-hold pending size
        pendingRelease(action, size);

        // 2. Reduce opposite position accordingly (if exists)
        final double sizeToOpen = closeCurrentPositionFutures(action, size, price);

        // 3. Increase forward position accordingly (if size left in the trading event)
        if (sizeToOpen > 0) {
            openPositionMargin(action, sizeToOpen, price);
        }
        return sizeToOpen;
    }

    private double closeCurrentPositionFutures(final OrderAction action, final double tradeSize, final double tradePrice) {

        // log.debug("{} {} {} {} cur:{}-{} profit={}", uid, action, tradeSize, tradePrice, position, totalSize, profit);

        if (direction == PositionDirection.EMPTY || direction == PositionDirection.of(action)) {
            // nothing to close
            return tradeSize;
        }

        if (openVolume > tradeSize) {
            // current position is bigger than trade size - just reduce position accordingly, don't fix profit
            openVolume -= tradeSize;
            openPriceSum -= tradeSize * tradePrice;
            return 0;
        }

        // current position smaller than trade size, can close completely and calculate profit
        profit += (openVolume * tradePrice - openPriceSum) * direction.getMultiplier();
        openPriceSum = 0;
        direction = PositionDirection.EMPTY;
        final double sizeToOpen = tradeSize - openVolume;
        openVolume = 0;

        // validateInternalState();

        return sizeToOpen;
    }

    private void openPositionMargin(OrderAction action, double sizeToOpen, double tradePrice) {
        openVolume += sizeToOpen;
        openPriceSum += tradePrice * sizeToOpen;
        direction = PositionDirection.of(action);

        // validateInternalState();
    }

    @Override
    public void writeMarshallable(BytesOut bytes) {
        bytes.writeInt(symbol);
        bytes.writeInt(currency);
        bytes.writeByte((byte) direction.getMultiplier());
        bytes.writeDouble(openVolume);
        bytes.writeDouble(openPriceSum);
        bytes.writeDouble(profit);
        bytes.writeLong(pendingSellSize);
        bytes.writeLong(pendingBuySize);
    }

    public void reset() {

        // log.debug("records: {}, Pending B{} S{} total size: {}", records.size(), pendingBuySize, pendingSellSize, totalSize);

        pendingBuySize = 0;
        pendingSellSize = 0;

        openVolume = 0;
        openPriceSum = 0;
        direction = PositionDirection.EMPTY;
    }

    public void validateInternalState() {
        if (direction == PositionDirection.EMPTY && (openVolume != 0 || openPriceSum != 0)) {
            log.error("uid {} : position:{} totalSize:{} openPriceSum:{}", uid, direction, openVolume, openPriceSum);
            throw new IllegalStateException();
        }
        if (direction != PositionDirection.EMPTY && (openVolume <= 0 || openPriceSum <= 0)) {
            log.error("uid {} : position:{} totalSize:{} openPriceSum:{}", uid, direction, openVolume, openPriceSum);
            throw new IllegalStateException();
        }

        if (pendingSellSize < 0 || pendingBuySize < 0) {
            log.error("uid {} : pendingSellSize:{} pendingBuySize:{}", uid, pendingSellSize, pendingBuySize);
            throw new IllegalStateException();
        }
    }

    @Override
    public int stateHash() {
        return Objects.hash(symbol, currency, direction.getMultiplier(), openVolume, openPriceSum, profit, pendingSellSize, pendingBuySize);
    }

    @Override
    public String toString() {
        return "SPR{" +
                "u" + uid +
                " sym" + symbol +
                " cur" + currency +
                " pos" + direction +
                " Σv=" + openVolume +
                " Σp=" + openPriceSum +
                " pnl=" + profit +
                " pendingS=" + pendingSellSize +
                " pendingB=" + pendingBuySize +
                '}';
    }
}
