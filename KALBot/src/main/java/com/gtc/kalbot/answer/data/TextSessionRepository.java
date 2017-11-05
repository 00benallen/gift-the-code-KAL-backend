package com.gtc.kalbot.answer.data;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Component;

import com.gtc.kalbot.answer.model.TextSession;

@Component
public interface TextSessionRepository extends MongoRepository<TextSession, String>{

	public TextSession findByUserPhoneNumber(String userPhoneNumber);
	
}
