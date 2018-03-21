package yinian.thread;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import yinian.utils.QiniuOperate;
import yinian.utils.YinianUtils;

public class VerifyPicture extends Thread {

	private String[] picArray;
	private int from;
	private int end;

	public VerifyPicture(String[] picArray, int from, int end) {
		this.picArray = picArray;
		this.from = from;
		this.end = end;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		QiniuOperate operate = new QiniuOperate();
		end = (end > picArray.length - 1 ? picArray.length - 1 : end);
		for (int i = from; i <= end; i++) {
			// 筛选GIF图并过滤
			String suffix = picArray[i].substring(picArray[i].length() - 3, picArray[i].length());
			if (suffix.equals("gif")) {
				picArray[i] = "";
			} else {
				// 执行鉴黄
				String address = operate.getDownloadToken(picArray[i] + "?nrop");
				// 发送请求并获取返回结果
				String result = sentNetworkRequest(address);
				if (!result.equals("")) {
					JSONObject jo = JSONObject.parseObject(result);
					int code = jo.getIntValue("code");
					if (code == 0) {
						JSONArray ja = jo.getJSONArray("fileList");
						JSONObject temp = ja.getJSONObject(0);

						// JSONObject temp = JSONObject.parseObject(jo.get("result").toString());
						int label = temp.getIntValue("label");
						if (label !=2) {
						//if (label ==0) {
							// 色情图片,图片地址设为空
							picArray[i] = "";
						}
					}
				}
			}
		}
	}

	/**
	 * 发送网络请求并返回结果
	 */
	public static String sentNetworkRequest(String url) {
		String result = "";

		try {
			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
			con.setRequestMethod("GET");
			con.setRequestProperty("accept", "*/*");
			con.setDoOutput(true);
			con.setDoInput(true);
			con.setConnectTimeout(3000);
			con.connect();

			InputStream input = con.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(input, "utf-8"));
			StringBuilder builder = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				builder.append(line).append("\n");
			}
			result = builder.toString();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "";
		}
		return result;
	}

}
