package yinian.hx.api.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yinian.hx.api.AuthTokenAPI;
import yinian.hx.api.EasemobRestAPI;
import yinian.hx.comm.body.AuthTokenBody;
import yinian.hx.comm.constant.HTTPMethod;
import yinian.hx.helper.HeaderHelper;
import yinian.hx.wrapper.BodyWrapper;
import yinian.hx.wrapper.HeaderWrapper;

public class EasemobAuthToken extends EasemobRestAPI implements AuthTokenAPI{
	
	public static final String ROOT_URI = "/token";
	
	private static final Logger log = LoggerFactory.getLogger(EasemobAuthToken.class);
	
	@Override
	public String getResourceRootURI() {
		return ROOT_URI;
	}

	public Object getAuthToken(String clientId, String clientSecret) {
		String url = getContext().getSeriveURL() + getResourceRootURI();
		BodyWrapper body = new AuthTokenBody(clientId, clientSecret);
		HeaderWrapper header = HeaderHelper.getDefaultHeader();
		return getInvoker().sendRequest(HTTPMethod.METHOD_POST, url, header, body, null);
	}
}
