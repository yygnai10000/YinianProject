package yinian.hx.api.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import yinian.hx.api.ChatMessageAPI;
import yinian.hx.api.EasemobRestAPI;
import yinian.hx.helper.HeaderHelper;
import yinian.hx.wrapper.HeaderWrapper;

public class EasemobChatMessage extends EasemobRestAPI implements
		ChatMessageAPI {

	private static final String ROOT_URI = "/chatmessages";

	public Object exportChatMessages(Long limit, String cursor, String timestamp) {
		String url = getContext().getSeriveURL() + getResourceRootURI()
				+ "?ql=select+*+where+timestamp>" + timestamp + "&limit=1000";
		HeaderWrapper header = HeaderHelper.getDefaultHeaderWithToken();

		String token = header.getHeaders().get(1).getValue();
		String responseBody = getResponseBody(url, token);
		while (responseBody.equals("IOException")) {
//			System.out.println("io异常，可能为请求太频繁，代码睡眠10秒后重复执行");
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
//				e.printStackTrace();
			}
			responseBody = getResponseBody(url, token);
		}
		return responseBody;
	}

	@Override
	public String getResourceRootURI() {
		return ROOT_URI;
	}

	/**
	 * 获取聊天记录并返回
	 * @param url
	 * @param token
	 * @return
	 */
	private String getResponseBody(String url, String token) {
		String responseBody = "";
		try {
			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
			con.setRequestMethod("GET");
			con.setRequestProperty("Content-Type", "application/json");
			con.setRequestProperty("Authorization", token);
			con.setRequestProperty("Accept-Charset", "utf-8");
			responseBody = readResponseBody(con.getInputStream());
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
//			e.printStackTrace();
			responseBody = "IOException";
			return responseBody;
		}

		return responseBody;
	}

	// 读取输入流中的数据
	private String readResponseBody(InputStream inputStream) throws IOException {

		BufferedReader in = new BufferedReader(new InputStreamReader(
				inputStream, "utf-8"));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();

		return response.toString();
	}
}
