package yinian.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

/**
����ֵ											˵��
success:msgid								�ύ�ɹ�������״̬���4.1
error:msgid									�ύʧ��
error:Missing username						�û���Ϊ��
error:Missing password						����Ϊ��
error:Missing apikey						APIKEYΪ��
error:Missing recipient						�ֻ�����Ϊ��
error:Missing message content				��������Ϊ��
error:Account is blocked					�ʺű�����
error:Unrecognized encoding					����δ��ʶ��
error:APIKEY or password error				APIKEY ���������
error:Unauthorized IP address				δ��Ȩ IP ��ַ
error:Account balance is insufficient		����
error:Black keywords is:������				���δ�
 */

public class SendMessage {

		
	public String send(String phonenumber , String MessageContent) throws IOException {
		//��������
		String content = MessageContent; 
		// ����StringBuffer�������������ַ���
		StringBuffer sb = new StringBuffer("http://m.5c.com.cn/api/send/?");
		// APIKEY
		sb.append("apikey=aedc6b0970d3b87d4725d50b07184e7f");
		//�û���
		sb.append("&username=weiwowl");
		// ��StringBuffer׷������
		sb.append("&password=meilian1234");
		// ��StringBuffer׷���ֻ�����
		sb.append("&mobile="+phonenumber+"");
		// ��StringBuffer׷����Ϣ����תURL��׼��
		sb.append("&content="+URLEncoder.encode(content,"GBK"));
		// ����url����
		URL url = new URL(sb.toString());
		// ��url����
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		// ����url����ʽ ��get�� ���� ��post��
		connection.setRequestMethod("POST");
		// ����
		BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
		// ���ط��ͽ��
		String inputline = in.readLine();
		// ������
//		System.out.println(inputline);
		return inputline;
	}

}