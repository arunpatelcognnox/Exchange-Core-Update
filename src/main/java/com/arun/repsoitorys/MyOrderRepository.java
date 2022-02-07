package com.arun.repsoitorys;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.arun.entitys.MyOrder;

@Repository
public interface MyOrderRepository extends MongoRepository<MyOrder, Long> {

	List<MyOrder> findByClientId(long userId);

}
