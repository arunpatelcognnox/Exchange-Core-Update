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


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Builder
@EqualsAndHashCode(callSuper = false)
//@RequiredArgsConstructor
@AllArgsConstructor
@NoArgsConstructor
@Data
public final class ApiCancelOrder extends ApiCommand {

    public long orderId;

    public long uid;
    public int symbol;

    @Override
    public String toString() {
        return "[CANCEL " + orderId + " u" + uid + " s" + symbol + "]";
    }
}
