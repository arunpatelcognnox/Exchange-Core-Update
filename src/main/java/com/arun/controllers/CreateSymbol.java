package com.arun.controllers;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.arun.exchange.core2.core.ExchangeApi;
import com.arun.exchange.core2.core.common.CoreSymbolSpecification;
import com.arun.exchange.core2.core.common.api.binary.BatchAddSymbolsCommand;
import com.arun.repsoitorys.CoreSymbolSpecificationRepository;

import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/symbol")
@AllArgsConstructor
@CrossOrigin
public class CreateSymbol {

	private ExchangeApi exchangeApi;
	private CoreSymbolSpecificationRepository coreSymbolSpecificationRepository;
	
	@GetMapping
	public List<CoreSymbolSpecification> getAll() {
		return coreSymbolSpecificationRepository.findAll();
	}
	
	@GetMapping("/{symboleId}")
	public CoreSymbolSpecification getById(@PathVariable int symboleId) {
		return coreSymbolSpecificationRepository.findById(symboleId).orElse(null);
	}
	
	@GetMapping("/getByBaseCurrencyAndQuoteCurrency/baseCurrency/{baseCurrency}/quoteCurrency/{quoteCurrency}")
	public CoreSymbolSpecification getByBaseCurrencyAndQuoteCurrency(@PathVariable int baseCurrency, @PathVariable int quoteCurrency ) {
		return coreSymbolSpecificationRepository.findByBaseCurrencyAndQuoteCurrency(baseCurrency, quoteCurrency);
	}
	
	@PostMapping
	public Object createSymbol(@RequestBody CoreSymbolSpecification coreSymbolSpecification) {
		return exchangeApi.submitBinaryDataAsync(new BatchAddSymbolsCommand(coreSymbolSpecification));
	}
	
}
