package yinian.thread;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;

import yinian.model.Event;
import yinian.model.Picture;
import yinian.utils.QiniuOperate;

public class Yellow extends Thread {

	private List<Record> list;
	private int index;

	public Yellow(List<Record> list, int index) {
		this.list = list;
		this.index = index;
	}

	public void run() {
		System.out.println("开始线程" + Thread.currentThread().getName());
		QiniuOperate operate = new QiniuOperate();
		for (int i = index; i < index + 3200; i++) {
			String groupid = list.get(i).get("groupid").toString();
			List<Record> list = Db.find(
					"select eid,GROUP_CONCAT(pid) as pid,GROUP_CONCAT(poriginal) as url,count(*) as num from `events`,pictures where eid=peid and estatus=0 and pstatus=0 and egroupid="
							+ groupid + " GROUP BY eid");
			System.out.println("空间id:" + groupid + " 共有动态数:" + list.size());
			for (Record record : list) {
				// 逐个动态检查
				int count = 0;
				int picNum = Integer.parseInt(record.get("num").toString());
				String[] url = record.getStr("url").split(",");
				String[] pid = record.getStr("pid").split(",");
				// 鉴定动态内的所有图片
				for (int j = 0; j < url.length; j++) {
					// 直接删除GIF图
					String suffix = url[j].substring(url[j].length() - 3, url[j].length());
					if (suffix.equals("gif")) {
						String id = pid[j];
						Picture picture = new Picture().findById(id);
						picture.set("pstatus", 1);
						picture.update();
						System.out.println("发现gif图,pid为" + id + ",执行删除");
						count++;
					} else {
						String address = operate.getDownloadToken(url[j] + "?nrop");
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
								if (label != 2) {
									// 色情或性感图片，删除
									String id = pid[j];
									Picture picture = new Picture().findById(id);
									picture.set("pstatus", 1);
									picture.update();
									System.out.println("发现违规照片,pid为" + id + ",执行删除");
									count++;
								}
							}
						}
					}
				}
				if (count == picNum) {
					// 动态内已经没有照片，删除动态
					String eid = record.get("eid").toString();
					System.out.println("动态" + eid + "已经没有照片，删除");
					Event event = new Event().findById(eid);
					event.set("estatus", 1);
					event.update();
				}
			}
		}
		System.out.println("结束线程" + Thread.currentThread().getName());
	}

	/**
	 * 发送网络请求并返回结果
	 */
	public static String sentNetworkRequest(String url) {
		String result = "";

		try {
			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
			con.setRequestMethod("POST");
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
