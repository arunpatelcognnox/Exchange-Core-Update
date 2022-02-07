package com.arun.service;

import java.util.Optional;

import com.arun.entitys.IncrementKey;
import com.arun.exchange.core2.core.common.MatcherTradeEvent;
import com.arun.repsoitorys.CoreSymbolSpecificationRepository;
import com.arun.repsoitorys.IncrementKeyRepository;
import com.arun.repsoitorys.MyTradeRepository;
import com.arun.repsoitorys.OrderRepository;
import com.arun.repsoitorys.UserRepository;

import lombok.Getter;


public class RepostoryService {

	@Getter private static UserRepository userRepository;
	@Getter private static CoreSymbolSpecificationRepository coreSymbolSpecificationRepository;
	@Getter private static OrderRepository orderRepository;
	@Getter private static MyTradeRepository myTradeRepository;
	private static IncrementKeyRepository incrementKeyRepository;
	
	public static void addRespository (UserRepository userRepo, CoreSymbolSpecificationRepository coreSymbolSpecificationRepo, OrderRepository orderRepo, MyTradeRepository myTradeRepo, IncrementKeyRepository incrementKeyRepo) {
		userRepository = userRepo;
		coreSymbolSpecificationRepository = coreSymbolSpecificationRepo;
		orderRepository = orderRepo;
		myTradeRepository = myTradeRepo;
		incrementKeyRepository = incrementKeyRepo;
	}
	
	public static long getId(String className) {
		Optional<IncrementKey> optional = incrementKeyRepository.findById(className);
		IncrementKey incrementKey;
		if(!optional.isPresent()) {
			incrementKey = new IncrementKey();
			incrementKey.setClassName(className);
		} else 
			incrementKey = optional.get();
		incrementKey.setCurrentId(incrementKey.getCurrentId()+1);
		return incrementKeyRepository.save(incrementKey).getCurrentId();
	}
	
    public static void saveTradeEventInRepository(MatcherTradeEvent event) {
    	
    	System.out.println(event);
    	
    	
//    	RepostoryService.getKafkaTemplateForTrade().send(ListnerConfigration.KAFKA_TOPIC_TRADE, event);
    }
	
}
