package yinian.hx.api.impl;

import yinian.hx.api.EasemobRestAPI;
import yinian.hx.api.SendMessageAPI;
import yinian.hx.comm.constant.HTTPMethod;
import yinian.hx.helper.HeaderHelper;
import yinian.hx.wrapper.BodyWrapper;
import yinian.hx.wrapper.HeaderWrapper;

public class EasemobSendMessage extends EasemobRestAPI implements SendMessageAPI {
    private static final String ROOT_URI = "/messages";

    @Override
    public String getResourceRootURI() {
        return ROOT_URI;
    }

    public Object sendMessage(Object payload) {
        String  url = getContext().getSeriveURL() + getResourceRootURI();
        HeaderWrapper header = HeaderHelper.getDefaultHeaderWithToken();
        BodyWrapper body = (BodyWrapper) payload;

        return getInvoker().sendRequest(HTTPMethod.METHOD_POST, url, header, body, null);
    }
}
