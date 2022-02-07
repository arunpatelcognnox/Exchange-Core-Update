package com.arun.controllers;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.arun.entitys.Candlestick;

import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/candlestick")
@AllArgsConstructor
@CrossOrigin
public class CandlestickDataController {

	private RestTemplate restTemplate;
	
	@GetMapping("/symbole/{symbole}/interval/{interval}")
	public Object getData(@PathVariable String symbole, @PathVariable String interval) {
		List<List<? extends Number>> data = restTemplate.exchange("https://api.binance.com/api/v1/klines?symbol="+symbole+"&interval="+interval, HttpMethod.GET,null, new ParameterizedTypeReference<List<List<? extends Number>>>() {} ).getBody();
		
		return getCandlesticksData(data);
	}
	
	private List<Candlestick<? extends Number>> getCandlesticksData(List<List<? extends Number>> input) {
		List<Candlestick<? extends Number>> result = new LinkedList<>();
		
        DateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		
		input.forEach(x -> result.add(Candlestick.builder().date(f.format(new Date((long) x.get(0)))).day(new Date((long)x.get(0)).getDate()).open(x.get(1)).high(x.get(2)).low(x.get(3)).close(x.get(4)).volume(x.get(5)).build()));
		
		return result;
	}
	
}
