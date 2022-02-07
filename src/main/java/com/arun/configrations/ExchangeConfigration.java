package com.arun.configrations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.arun.entitys.IEventsHanlerImpl;
import com.arun.exchange.core2.core.ExchangeApi;
import com.arun.exchange.core2.core.ExchangeCore;
import com.arun.exchange.core2.core.SimpleEventsProcessor;
import com.arun.exchange.core2.core.common.config.ExchangeConfiguration;
import com.arun.repsoitorys.CoreSymbolSpecificationRepository;
import com.arun.repsoitorys.IncrementKeyRepository;
import com.arun.repsoitorys.MyTradeRepository;
import com.arun.repsoitorys.OrderRepository;
import com.arun.repsoitorys.UserRepository;
import com.arun.service.RepostoryService;

import lombok.AllArgsConstructor;

@Configuration
@AllArgsConstructor
public class ExchangeConfigration {

	private UserRepository userRepository;
	private CoreSymbolSpecificationRepository coreSymbolSpecificationRepository;
	private OrderRepository orderRepository;
	private MyTradeRepository myTradeRepsoitory;
	private IncrementKeyRepository incrementKeyRepository;
	
	@Bean
	public ExchangeApi exchangeApi() { 
		
		RepostoryService.addRespository(userRepository, coreSymbolSpecificationRepository, orderRepository, myTradeRepsoitory, incrementKeyRepository);
		
		ExchangeCore exchangeCore = ExchangeCore.builder()
		        .resultsConsumer(new SimpleEventsProcessor(new IEventsHanlerImpl()))
		   //     .serializationProcessorFactory(() -> DummySerializationProcessor.INSTANCE)
		        .exchangeConfiguration(ExchangeConfiguration.defaultBuilder().build())
		        .build();

		exchangeCore.startup();
		return exchangeCore.getApi();
	}
}
