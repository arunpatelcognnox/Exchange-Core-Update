package com.arun.repsoitorys;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.arun.exchange.core2.core.common.CoreSymbolSpecification;

@Repository
public interface CoreSymbolSpecificationRepository extends MongoRepository<CoreSymbolSpecification, Integer> {

	public CoreSymbolSpecification findByBaseCurrencyAndQuoteCurrency(int baseCurrency, int quoteCurrency);

}
