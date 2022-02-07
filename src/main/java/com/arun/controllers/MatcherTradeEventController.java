package com.arun.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.arun.entitys.MyTrade;
import com.arun.repsoitorys.MyTradeRepository;

@RestController
@RequestMapping("/api/trade")
public class MatcherTradeEventController {

	@Autowired private MyTradeRepository myTradeRepository;
	
	@GetMapping
	public List<MyTrade> getAll() {
		return myTradeRepository.findAll();
	}
	
}
