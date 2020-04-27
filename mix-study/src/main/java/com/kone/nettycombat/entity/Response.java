package com.kone.nettycombat.entity;

import lombok.Data;

@Data
public class Response  {
	
	private String requestId;
	private int code;
	private String error_msg;
	private Object data;

}
