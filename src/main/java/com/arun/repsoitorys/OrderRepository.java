package com.arun.repsoitorys;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.arun.exchange.core2.core.common.Order;

@Repository
public interface OrderRepository extends MongoRepository<Order, Long>  {

	List<Order> findByUid(long userId);

}
