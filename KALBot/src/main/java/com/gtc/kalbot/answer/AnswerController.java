package com.gtc.kalbot.answer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.gtc.kalbot.answer.data.ConversationRepository;
import com.gtc.kalbot.answer.data.TextSessionRepository;
import com.gtc.kalbot.answer.model.Conversation;
import com.gtc.kalbot.answer.model.Option;
import com.gtc.kalbot.answer.model.TextSession;
import com.twilio.sdk.TwilioRestClient;
import com.twilio.sdk.TwilioRestException;
import com.twilio.sdk.TwilioRestResponse;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@CrossOrigin
@RestController
@Slf4j
@AllArgsConstructor
public class AnswerController {
	
	protected ConversationRepository convoRepo;
	
	protected TextSessionRepository sessionRepo;
	@GetMapping("/answer")
	public Conversation getAnswer(
			@RequestParam(required=true, name="response") final String id,
			@RequestParam(required=false, name="text") final boolean text,
			@RequestParam(required=false, name="mobileNumber", defaultValue="INSERT") final String mobileNumber) throws TwilioRestException {
		
		/*
		 * Lookup conversation from database, using UI message as ID
		 */
		Conversation convo = convoRepo.findOne(id);
		
		/*
		 * Validate conversation, return default if corrupted or not found 
		 */
		List<Option> options = null;
		if(convo == null) {
			options = new LinkedList<Option>();
			options.add(new Option("Seek help from IT.", "0"));
			return new Conversation("I don't understand.", options);
		} else {
			
			options = convo.getOptions();
			
		}
		
		/*
		 * If text conversation is enabled from query params
		 */
		if(text) {
			
			/*
			 * Build Twilio client with auth data for SMS interaction
			 */
			TwilioRestClient client = new TwilioRestClient(
					"insert account id", 
					"insert auth id");
			
			/*
			 * Build request for Twilio
			 */
			LinkedHashMap<String, String> params = new LinkedHashMap<String, String>();
			params.put("To", "+" + mobileNumber);
			params.put("From", "insert registered number");
			params.put("Body", buildSMSString(convo));
			
			/*
			 * Execute request to send text
			 */
			TwilioRestResponse resp = client.safeRequest("/2010-04-01/Accounts/Insert account id/Messages.json", "POST", params);
			
			/*
			 * If successful
			 */
			if(resp.getHttpStatus() == 201) {
				
				/*
				 * Lookup if SMS session previously exists, if it doesn't
				 */
				TextSession preSession = sessionRepo.findByUserPhoneNumber("+" + mobileNumber);
				if(preSession == null) {
				
					/*
					 * Create new SMS session, save it to database
					 */
					//should be 5 mins
					LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(5);
					
					log.info(expiryTime.toString());
					TextSession session = new TextSession(
							UUID.randomUUID().toString(),
							convo,
							"+" + mobileNumber,
							expiryTime);
					
					
					sessionRepo.save(session);
					
				} else {
					
					/*
					 * If it exists, update the session with new conversation node
					 */
					preSession.setLastConversation(convo);
				}
			}
		}
		
		/*
		 * Return conversation to UI
		 */
		return convo;

	}
	
	@GetMapping("/reply")
	public ResponseEntity<Void> replyToText(
			@RequestParam("From") String from,
			@RequestParam("To") String to,
			@RequestParam("Body") String body) throws TwilioRestException {
		
		/*
		 * Lookup SMS session using user phone number in "From" field
		 */
		TextSession session = sessionRepo.findByUserPhoneNumber(from);
		
		Conversation convo = null;
		if(session == null) {
			
			/*
			 * Create new SMS session, save it to database
			 */
			//should be 5 mins
			LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(5);
			
			log.info("New SMS session with expiry: " + expiryTime.toString());
			session = new TextSession(
					UUID.randomUUID().toString(),
					convo,
					from,
					expiryTime);
			
			convo = convoRepo.findOne("What's on your mind?");
			
		} else {
			
			/*
			 * Build client for Twilio from auth data
			 */
			TwilioRestClient client = new TwilioRestClient(
					"insert account id", 
					"insert auth id");
			
			if(body.equalsIgnoreCase("x") || body.equalsIgnoreCase("exit")) {
				sessionRepo.delete(session);
				
				/*
				 * Build request to Twilio
				 */
				LinkedHashMap<String, String> params = new LinkedHashMap<String, String>();
				params.put("To", from);
				params.put("From", to);
				params.put("Body", "You have selected to exit conversation, thank you for using KAL");
				
				/*
				 * Execute request to send SMS reply
				 */
				log.info("Executing response to user, with params: " + params.toString());
				TwilioRestResponse resp  = client.safeRequest("/2010-04-01/Accounts/insert account id/Messages.json", "POST", params);
				log.info("Response from Twilio: " + resp.getHttpStatus());
				
				/*
				 * Return success to Twilio
				 */
				return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
				
			}
		
			/*
			 * Get current conversation node
			 */
			Conversation lastConvo = session.getLastConversation();
			Option selectedOption = lastConvo.getOptions().get(Integer.parseInt(body));
			
			/*
			 * Lookup next node
			 */
			convo = convoRepo.findOne(selectedOption.getLink());
			
		
		}
		
		/*
		 * Build client for Twilio from auth data
		 */
		TwilioRestClient client = new TwilioRestClient(
				"insert account id", 
				"insert auth id");
		
		/*
		 * Build request to Twilio
		 */
		LinkedHashMap<String, String> params = new LinkedHashMap<String, String>();
		params.put("To", from);
		params.put("From", to);
		params.put("Body", buildSMSString(convo));
		
		/*
		 * Execute request to send SMS reply
		 */
		log.info("Executing response to user, with params: " + params.toString());
		TwilioRestResponse resp  = client.safeRequest("/2010-04-01/Accounts/insert account id/Messages.json", "POST", params);
		log.info("Response from Twilio: " + resp.getHttpStatus());
		
		/*
		 * Update SMS session
		 */
		session.setLastConversation(convo);
		sessionRepo.save(session);
		
		/*
		 * Return success to Twilio
		 */
		return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
	}
	
	private String buildSMSString(Conversation convo) {
		String smsString = convo.getMessage();
		
		for(int i = 0 ; i < convo.getOptions().size(); i++) {
			if(!convo.getOptions().get(i).getText().equals("")) {
				if(i == 0) {
					 smsString += " Please text one of the following options: \n";
				}
				smsString += " ( " + i + " ) " + convo.getOptions().get(i).getText() + "\n";
			}
		}
		
		return smsString;
	}

	@PostMapping("/answer")
	public ResponseEntity<Void> addAnswer(@NotNull @Valid @RequestBody final Conversation convo) {
		
		log.info("Saving conversation: " + convo.toString());
		convoRepo.save(convo);
		
		return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
		
	}
	
	@DeleteMapping("/answer")
	public ResponseEntity<Void> deleteAnswer(@RequestParam(required=true, name="response") final String response) {
		
		log.info("Deleting conversation mapped to input: " + response);
		convoRepo.delete(response);
		
		return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
	}
	
	@DeleteMapping("answers/clear")
	public ResponseEntity<Void> deleteAllAnswers() {
		
		log.info("Deleting all conversations");
		
		convoRepo.deleteAll();
		
		return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
	}
	
	@DeleteMapping("sessions/clear")
	public ResponseEntity<Void> deleteAllSessions() {
		
		log.info("Deleting all sessions");
		
		sessionRepo.deleteAll();
		
		return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
	}
	
	//should be 300000
	@Scheduled(fixedRate = 300000)
	public void cleanupSessions() {
		
		List<TextSession> sessions = sessionRepo.findAll();
		
		log.info("Cleaning sessions.");
		
		if(sessions != null && !sessions.isEmpty()) {
			sessions.stream().forEach(e -> {
				if(e.getExpiryTime() == null || e.getExpiryTime().isAfter(LocalDateTime.now())) {
					log.info("Expired session found with ID " + e.getSessionId() + ", deleting from database");
					sessionRepo.delete(e);
				} else {
					log.info("Session ID " + e.getSessionId() + " not expired yet, leaving. Will expire at: " + e.getExpiryTime());
				}
			});
		}
		
	}
	
	@PostMapping("/conversations/set")
	public ResponseEntity<Void> toPOJO(@RequestParam("file") MultipartFile cvsFile) throws IllegalStateException, IOException {
		
		String content = new String(cvsFile.getBytes(), "UTF-8");
		
		Pattern pattern = Pattern.compile(",");
		BufferedReader in = new BufferedReader(new StringReader(content));
		
		List<Conversation> data = in.lines().skip(1).map(line -> {
			String[] tokens = pattern.split(line);
			
			List<Option> options = new ArrayList<Option>();
			for(int i = 1; i < tokens.length; i +=2) {
				options.add(new Option(tokens[i], tokens[i+1]));
			}
			
			
			return new Conversation(tokens[0], options);
		}).collect(Collectors.toList());
		
		in.close();
		
		convoRepo.deleteAll();
		sessionRepo.deleteAll();
		
		data.stream().forEach(e->{
			convoRepo.save(e);
		});
		
		return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
	}
}
