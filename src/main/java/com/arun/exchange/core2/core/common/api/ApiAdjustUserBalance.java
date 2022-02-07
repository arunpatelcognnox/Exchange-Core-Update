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
package com.arun.exchange.core2.core.common.api;


import com.arun.exchange.core2.core.common.BalanceAdjustmentType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Builder
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@ToString
public final class ApiAdjustUserBalance extends ApiCommand {

    public final long uid;

    public final int currency;
    public final double amount;
    public final long transactionId;

    public final BalanceAdjustmentType adjustmentType = BalanceAdjustmentType.ADJUSTMENT; // TODO support suspend

}
