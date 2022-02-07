package com.arun.repsoitorys;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.arun.entitys.IncrementKey;

@Repository
public interface IncrementKeyRepository extends MongoRepository<IncrementKey, String> {

}
