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


import java.util.Objects;

import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;

import com.arun.exchange.core2.core.common.CoreSymbolSpecification;
import com.arun.exchange.core2.core.common.StateHash;
import com.arun.exchange.core2.core.utils.HashingUtils;
import com.arun.exchange.core2.core.utils.SerializationUtils;
import com.arun.service.RepostoryService;

import lombok.extern.slf4j.Slf4j;
import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.bytes.BytesOut;
import net.openhft.chronicle.bytes.WriteBytesMarshallable;

@Slf4j
public final class SymbolSpecificationProvider implements WriteBytesMarshallable, StateHash {

    // symbol->specs
    private final IntObjectHashMap<CoreSymbolSpecification> symbolSpecs;

    public SymbolSpecificationProvider() {
        this.symbolSpecs = new IntObjectHashMap<>();
        RepostoryService.getCoreSymbolSpecificationRepository().findAll().forEach(x -> symbolSpecs.put(x.getSymbolId(), x));
    }

    public SymbolSpecificationProvider(BytesIn bytes) {
        this.symbolSpecs = SerializationUtils.readIntHashMap(bytes, CoreSymbolSpecification::new);
        RepostoryService.getCoreSymbolSpecificationRepository().findAll().forEach(x -> symbolSpecs.put(x.getSymbolId(), x));
    }


    public boolean addSymbol(final CoreSymbolSpecification symbolSpecification) {
   System.out.println("SymbileSpecfincation "+symbolSpecification);
    	if (getSymbolSpecification(symbolSpecification.symbolId) != null) {
            return false; // CommandResultCode.SYMBOL_MGMT_SYMBOL_ALREADY_EXISTS;
        } else {
            registerSymbol(symbolSpecification.symbolId, symbolSpecification);
            return true;
        }
    }

    /**
     * Get symbol specification
     *
     * @param symbol - symbol code
     * @return symbol specification
     */
    public CoreSymbolSpecification getSymbolSpecification(int symbol) {
        return symbolSpecs.get(symbol);
    }

    /**
     * register new symbol specification
     *
     * @param symbol - symbol code
     * @param spec   - symbol specification
     */
    public void registerSymbol(int symbol, CoreSymbolSpecification spec) {
    	RepostoryService.getCoreSymbolSpecificationRepository().save(spec);
    	symbolSpecs.put(symbol, spec);
    }

    /**
     * Reset state
     */
    public void reset() {
        symbolSpecs.clear();
    }

    @Override
    public void writeMarshallable(BytesOut bytes) {
        // write symbolSpecs
        SerializationUtils.marshallIntHashMap(symbolSpecs, bytes);
    }

    @Override
    public int stateHash() {
        return Objects.hash(HashingUtils.stateHash(symbolSpecs));
    }

}
