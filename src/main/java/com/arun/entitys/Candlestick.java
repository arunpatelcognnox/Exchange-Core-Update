package com.arun.entitys;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Candlestick<T extends Number> {

	private String date;
	private T day;
	private T open;
	private T high;
	private T low;
	private T close;
	private T volume;
	
}
