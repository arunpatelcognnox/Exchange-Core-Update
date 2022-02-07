package com.arun.controllers;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.arun.entitys.MyOrder;
import com.arun.exchange.core2.core.ExchangeApi;
import com.arun.exchange.core2.core.common.Order;
import com.arun.exchange.core2.core.common.api.ApiCancelOrder;
import com.arun.exchange.core2.core.common.api.ApiMoveOrder;
import com.arun.exchange.core2.core.common.api.ApiPlaceOrder;
import com.arun.exchange.core2.core.common.cmd.CommandResultCode;
import com.arun.repsoitorys.MyOrderRepository;
import com.arun.repsoitorys.OrderRepository;
import com.arun.service.RepostoryService;

import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/placeOrder")
@CrossOrigin
@AllArgsConstructor
public class PlaceOrderController {

	private ExchangeApi exchangeApi;
	private MyOrderRepository myOrderRepository; 
	
	@GetMapping
	public List<MyOrder> getAll() {
		return myOrderRepository.findAll();
	}
	
	@GetMapping("/getByUserId/{userId}")
	public List<MyOrder> getByUserId(@PathVariable long userId) {
		return myOrderRepository.findByClientId(userId);
	}
	
	@GetMapping("/requestOrderBook/symbolXbtLtc/{symbolXbtLtc}")
	public Object requestOrderBook(@PathVariable int symbolXbtLtc) {
		 return exchangeApi.requestOrderBookAsync(symbolXbtLtc, 10);
	}
	
	
	@PostMapping
	public Object saveMyOrder(@RequestBody MyOrder myOrder) throws InterruptedException, ExecutionException {
		CompletableFuture<CommandResultCode> result = exchangeApi.submitCommandAsync(new ApiPlaceOrder(myOrder));
		if(result.get().equals(CommandResultCode.SUCCESS)) {
			System.out.println("kafka Strem");
			myOrderRepository.save(myOrder);
		}
		return result;
	}
	
	@PutMapping
	public Object updateOrder(@RequestBody ApiMoveOrder moveOrder) {
		return exchangeApi.submitCommandAsync(moveOrder);
	}
	
	@DeleteMapping
	public Object cancelOrder(@RequestBody ApiCancelOrder cancelOrde) {
		return exchangeApi.submitCommandAsync(cancelOrde);
	}
	
}
