package com.gtc.kalbot.answer.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TextSession {
	
	@Id
	protected String sessionId;
	
	protected Conversation lastConversation;
	
	protected String userPhoneNumber;
	
	protected LocalDateTime expiryTime;
	

}
