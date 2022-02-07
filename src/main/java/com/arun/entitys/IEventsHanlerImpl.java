package com.arun.entitys;

import com.arun.exchange.core2.core.IEventsHandler;

public class IEventsHanlerImpl implements IEventsHandler {

	@Override
	public void tradeEvent(TradeEvent tradeEvent) {
		System.out.println("Trade event: " + tradeEvent);
	}

	@Override
	public void reduceEvent(ReduceEvent reduceEvent) {
		System.out.println("Reduce event: " + reduceEvent);
	}

	@Override
	public void rejectEvent(RejectEvent rejectEvent) {
		System.out.println("Reject event: " + rejectEvent);
	}

	@Override
	public void commandResult(ApiCommandResult commandResult) {
		System.out.println("Command result: " + commandResult);
	}

	@Override
	public void orderBook(OrderBook orderBook) {
		System.out.println("OrderBook event: " + orderBook);
	}
}
