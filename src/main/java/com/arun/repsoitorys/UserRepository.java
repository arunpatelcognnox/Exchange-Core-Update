package com.arun.repsoitorys;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.arun.exchange.core2.core.common.UserProfile;

@Repository
public interface UserRepository extends MongoRepository<UserProfile, Long> {

}
