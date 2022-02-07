package com.arun;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.arun.exchange.core2.core.ExchangeApi;
import com.arun.exchange.core2.core.common.CoreSymbolSpecification;
import com.arun.exchange.core2.core.common.SymbolType;
import com.arun.exchange.core2.core.common.api.binary.BatchAddSymbolsCommand;

@SpringBootTest
class ExchangeTestApplicationTests {

	private ExchangeApi exchangeApi;
	
	@Test
	void contextLoads() {
		CoreSymbolSpecification	coreSymbolSpecification = CoreSymbolSpecification.builder().symbolId(14).symbol("BTC/USD").type(SymbolType.CURRENCY_EXCHANGE_PAIR).baseCurrency(11).quoteCurrency(15).baseScaleK(1545.45).quoteScaleK(14656858.65).takerFee(146.545).makerFee(15643.42).build();
		exchangeApi.submitBinaryDataAsync(new BatchAddSymbolsCommand(coreSymbolSpecification));
	}

}
