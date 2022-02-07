package com.arun.entitys;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.arun.exchange.core2.core.common.OrderType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Document
public class MyOrder {

	@Id
	private long orderId;
	private long clientId;
	private String clientOrderId;
	private int symbolId;
	private String symbol;
	private double price;
	private double origQty;

	private double executedQty;
	private double cummulativeQuoteQty;
	
	private Status status;
	private OrderType timeInForce;
	private Type type; 
	private Side side;
	
	private long transactTime;
	  
}
