package com.arun.controllers;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.arun.exchange.core2.core.ExchangeApi;
import com.arun.exchange.core2.core.common.api.ApiAdjustUserBalance;
import com.arun.service.RepostoryService;

import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/deposits")
@CrossOrigin
@AllArgsConstructor
public class DepositsController {

	private ExchangeApi exchangeApi;
	
	@PostMapping("/userId/{userId}/currencyCode/{currencyCode}/amount/{amount}")
	public Object deposits(@PathVariable int userId, @PathVariable int currencyCode, @PathVariable double amount ) {
		return exchangeApi.submitCommandAsync(ApiAdjustUserBalance.builder()
		        .uid(userId)
		        .currency(currencyCode)
		        .amount(amount)
		        .transactionId(RepostoryService.getId("DEPOSITS"))
		        .build());
	}

	
}
