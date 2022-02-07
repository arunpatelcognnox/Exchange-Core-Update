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
package com.arun.exchange.core2.core.utils;

import com.arun.exchange.core2.core.common.CoreSymbolSpecification;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class CoreArithmeticUtils {

    public static double calculateAmountAsk(double size, CoreSymbolSpecification spec) {
        return size * spec.baseScaleK;
    }

    public static double calculateAmountBid(double size, double price, CoreSymbolSpecification spec) {
        return size * (price * spec.quoteScaleK);
    }

    public static double calculateAmountBidTakerFee(double size, double price, CoreSymbolSpecification spec) {
        return size * (price * spec.quoteScaleK + spec.takerFee);
    }

    public static double calculateAmountBidReleaseCorrMaker(double size, double priceDiff, CoreSymbolSpecification spec) {
        return size * (priceDiff * spec.quoteScaleK + (spec.takerFee - spec.makerFee));
    }

    public static double calculateAmountBidTakerFeeForBudget(double size, double budgetInSteps, CoreSymbolSpecification spec) {

        return budgetInSteps * spec.quoteScaleK + size * spec.takerFee;
    }

}
