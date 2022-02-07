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


import com.arun.entitys.MyOrder;
import com.arun.entitys.Side;
import com.arun.exchange.core2.core.common.OrderAction;
import com.arun.exchange.core2.core.common.OrderType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Builder
@EqualsAndHashCode(callSuper = false)
//@RequiredArgsConstructor
@Data
@AllArgsConstructor
@NoArgsConstructor
public final class ApiPlaceOrder extends ApiCommand {

    public double price;
    public double size;
    public long orderId;
    public OrderAction action;
    public OrderType orderType;
    public long uid;
    public int symbol;
    public int userCookie;
    public double reservePrice;

    public ApiPlaceOrder(MyOrder myOrder) {
    	price = myOrder.getPrice();
    	size = myOrder.getOrigQty();
    	orderId = myOrder.getOrderId();
    	action = myOrder.getSide().equals(Side.BUY) ? OrderAction.BID : OrderAction.ASK ;
    	orderType = myOrder.getTimeInForce();
    	uid = myOrder.getClientId();
    	symbol = myOrder.getSymbolId();		
    }
    
    
    @Override
    public String toString() {
        return "[ADD o" + orderId + " s" + symbol + " u" + uid + " " + (action == OrderAction.ASK ? 'A' : 'B')
                + ":" + (orderType == OrderType.IOC ? "IOC" : "GTC")
                + ":" + price + ":" + size + "]";
        //(reservePrice != 0 ? ("(R" + reservePrice + ")") : "") +
    }
}
