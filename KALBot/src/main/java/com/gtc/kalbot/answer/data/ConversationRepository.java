package com.gtc.kalbot.answer.data;


import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Component;

import com.gtc.kalbot.answer.model.Conversation;

@Component
public interface ConversationRepository extends MongoRepository<Conversation, String> {

}