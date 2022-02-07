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


import java.util.Objects;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.bytes.BytesOut;
import net.openhft.chronicle.bytes.WriteBytesMarshallable;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Document
public class CoreSymbolSpecification implements WriteBytesMarshallable, StateHash {

	@Id
    public int symbolId;
	public String symbol;

    @NonNull
    public SymbolType type;

    // currency pair specification
    public int baseCurrency;  // base currency
    public int quoteCurrency; // quote/counter currency (OR futures contract currency)
    public double baseScaleK;   // base currency amount multiplier (lot size in base currency units)
    public double quoteScaleK;  // quote currency amount multiplier (step size in quote currency units)

    // fees per lot in quote? currency units
    public double takerFee; // TODO check invariant: taker fee is not less than maker fee
    public double makerFee;

    // margin settings (for type=FUTURES_CONTRACT only)
    public double marginBuy;   // buy margin (quote currency)
    public double marginSell;  // sell margin (quote currency)

    public CoreSymbolSpecification(BytesIn bytes) {
        this.symbolId = bytes.readInt();
        this.symbol = bytes.readUtf8();
        this.type = SymbolType.of(bytes.readByte());
        this.baseCurrency = bytes.readInt();
        this.quoteCurrency = bytes.readInt();
        this.baseScaleK = bytes.readDouble();
        this.quoteScaleK = bytes.readDouble();
        this.takerFee = bytes.readDouble();
        this.makerFee = bytes.readDouble();
        this.marginBuy = bytes.readDouble();
        this.marginSell = bytes.readDouble();

        System.out.println("readDouble => "+ this.baseScaleK+" = "+this.quoteScaleK+" = "+this.takerFee);
    }

/* NOT SUPPORTED YET:

//    order book limits -- for FUTURES only
//    public final long highLimit;
//    public final long lowLimit;

//    swaps -- not by
//    public final long longSwap;
//    public final long shortSwap;

// activity (inactive, active, expired)

  */

    @Override
    public void writeMarshallable(BytesOut bytes) {
        bytes.writeInt(symbolId);
        bytes.writeUtf8(symbol);
        bytes.writeByte(type.getCode());
        bytes.writeInt(baseCurrency);
        bytes.writeInt(quoteCurrency);
        bytes.writeDouble(baseScaleK);
        bytes.writeDouble(quoteScaleK);
        bytes.writeDouble(takerFee);
        bytes.writeDouble(makerFee);
        bytes.writeDouble(marginBuy);
        bytes.writeDouble(marginSell);
        System.out.println("writeMarshallable =>"+this);
        System.out.println("write double => "+ this.baseScaleK+" = "+this.quoteScaleK+" = "+this.takerFee);
    }

    @Override
    public int stateHash() {
        return Objects.hash(
                symbolId,
                symbol,
                type.getCode(),
                baseCurrency,
                quoteCurrency,
                baseScaleK,
                quoteScaleK,
                takerFee,
                makerFee,
                marginBuy,
                marginSell);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CoreSymbolSpecification that = (CoreSymbolSpecification) o;
        return symbolId == that.symbolId &&
        		symbol.equals(that.symbol) &&  
        		baseCurrency == that.baseCurrency &&
                quoteCurrency == that.quoteCurrency &&
                baseScaleK == that.baseScaleK &&
                quoteScaleK == that.quoteScaleK &&
                takerFee == that.takerFee &&
                makerFee == that.makerFee &&
                marginBuy == that.marginBuy &&
                marginSell == that.marginSell &&
                type == that.type;
    }
}
