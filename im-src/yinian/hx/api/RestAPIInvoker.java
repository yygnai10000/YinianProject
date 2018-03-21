package yinian.hx.api;

import java.io.File;

import yinian.hx.wrapper.BodyWrapper;
import yinian.hx.wrapper.HeaderWrapper;
import yinian.hx.wrapper.QueryWrapper;
import yinian.hx.wrapper.ResponseWrapper;

public interface RestAPIInvoker {
	ResponseWrapper sendRequest(String method, String url, HeaderWrapper header, BodyWrapper body, QueryWrapper query);
	ResponseWrapper uploadFile(String url, HeaderWrapper header, File file);
    ResponseWrapper downloadFile(String url, HeaderWrapper header, QueryWrapper query);
}
