package yinian.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

/**
返回值											说明
success:msgid								提交成功，发送状态请见4.1
error:msgid									提交失败
error:Missing username						用户名为空
error:Missing password						密码为空
error:Missing apikey						APIKEY为空
error:Missing recipient						手机号码为空
error:Missing message content				短信内容为空
error:Account is blocked					帐号被禁用
error:Unrecognized encoding					编码未能识别
error:APIKEY or password error				APIKEY 或密码错误
error:Unauthorized IP address				未授权 IP 地址
error:Account balance is insufficient		余额不足
error:Black keywords is:党中央				屏蔽词
 */

public class SendMessage {

		
	public String send(String phonenumber , String MessageContent) throws IOException {
		//发送内容
		String content = MessageContent; 
		// 创建StringBuffer对象用来操作字符串
		StringBuffer sb = new StringBuffer("http://m.5c.com.cn/api/send/?");
		// APIKEY
		sb.append("apikey=aedc6b0970d3b87d4725d50b07184e7f");
		//用户名
		sb.append("&username=weiwowl");
		// 向StringBuffer追加密码
		sb.append("&password=meilian1234");
		// 向StringBuffer追加手机号码
		sb.append("&mobile="+phonenumber+"");
		// 向StringBuffer追加消息内容转URL标准码
		sb.append("&content="+URLEncoder.encode(content,"GBK"));
		// 创建url对象
		URL url = new URL(sb.toString());
		// 打开url连接
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		// 设置url请求方式 ‘get’ 或者 ‘post’
		connection.setRequestMethod("POST");
		// 发送
		BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
		// 返回发送结果
		String inputline = in.readLine();
		// 输出结果
//		System.out.println(inputline);
		return inputline;
	}

}