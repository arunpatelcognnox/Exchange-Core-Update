package com.arun.entitys;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.arun.exchange.core2.core.common.OrderType;
import com.arun.service.RepostoryService;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Document
public class MyTrade {
	
	@Id
	private long id;
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
	  
	MyTrade(MyOrder myOrder) {
		this.id = RepostoryService.getId("MYTRADE");
		this.orderId = myOrder.getOrderId();
		this.clientId = myOrder.getClientId();
		this.clientOrderId = myOrder.getClientOrderId();
		this.symbolId = myOrder.getSymbolId();
		this.symbol = myOrder.getSymbol();
		this.price = myOrder.getPrice();
		this.origQty = myOrder.getOrigQty();
		this.executedQty = myOrder.getExecutedQty();
		this.cummulativeQuoteQty = myOrder.getCummulativeQuoteQty();
		this.status = myOrder.getStatus();
		this.timeInForce = myOrder.getTimeInForce();
		this.type =  myOrder.getType();
		this.side = myOrder.getSide();
		this.transactTime = System.currentTimeMillis();		
	}
}
