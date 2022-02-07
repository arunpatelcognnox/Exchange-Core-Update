package com.arun.controllers;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.arun.exchange.core2.core.ExchangeApi;
import com.arun.exchange.core2.core.common.UserProfile;
import com.arun.exchange.core2.core.common.api.ApiAddUser;
import com.arun.repsoitorys.UserRepository;

import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/user")
@AllArgsConstructor
@CrossOrigin
public class UserController {

	private ExchangeApi exchangeApi;
	private UserRepository userRepository;
	
	@GetMapping
	public List<UserProfile> getAll() {
		userRepository.findAll().forEach(System.out::println);
		return userRepository.findAll();
	}
	
	@GetMapping("/getUserIds")
	public List<Long> getUserIds() {
		return userRepository.findAll().stream().map(x ->x.getUid()).collect(Collectors.toList());
	}
	
	@PostMapping("/{id}")
	public Object save(@PathVariable long id) {
		return exchangeApi.submitCommandAsync(ApiAddUser.builder().uid(id).build());
	}
	
}
