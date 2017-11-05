package com.gtc.kalbot.answer.model;

import java.util.List;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.annotation.Id;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Conversation {

	
	@NotNull
	@NotEmpty
	@Id
	public String message;
	
	@NotNull
	@NotEmpty
	public List<Option> options;
	
	public Conversation() {}
	
	@Override
    public String toString() {
        return String.format(
                "Conversation[message='%s', options=%s']",
                message,
                options.toString());
    }
	
	

}
