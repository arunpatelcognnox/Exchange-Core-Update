package com.arun.repsoitorys;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.arun.entitys.MyTrade;

@Repository
public interface MyTradeRepository extends MongoRepository<MyTrade, Long> {

}
