package yinian.controller;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.security.InvalidAlgorithmParameterException;
import java.security.MessageDigest;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bouncycastle.util.encoders.Base64;

import com.jfinal.core.Controller;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;
import com.qiniu.util.Auth;

import freemarker.ext.beans.NumberModel;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import redis.clients.jedis.Jedis;
import yinian.app.YinianDataProcess;
import yinian.common.CommonParam;
import yinian.model.Event;
import yinian.model.FormID;
import yinian.model.Group;
import yinian.model.GroupMember;
import yinian.model.Points;
import yinian.model.PointsReceive;
import yinian.model.HotPic;
import yinian.model.PartnerRecord;
import yinian.model.User;
import yinian.push.SmallAppPush;
import yinian.schedule.CanUserdMoney;
import yinian.service.SpaceService;
import yinian.service.TestService;
import yinian.thread.PointsThread;
import yinian.utils.AES;
import yinian.utils.HttpUtils;
import yinian.utils.JsonData;
import yinian.utils.Pkcs7Encoder;
import yinian.utils.QiniuOperate;
import yinian.utils.RedisUtils;
import yinian.utils.SmallAppQRCode;
import yinian.utils.UrlUtils;

public class TestController extends Controller {
	private String jsonString; // 返回的json字符串
	private JsonData jsonData = new JsonData(); // json操作类
	 private static String controllerUrl =
	 "http://121.41.26.122/1105.php";
//	 public void testWx(){
//		 Record judge = Db.findFirst("select * from ynTemp where id=43");//获取accessToken
//		 YinianDataProcess dataProcess = new YinianDataProcess();
//			// 获取accessToken
//			String url = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=" + CommonParam.appID
//					+ "&secret=" + CommonParam.secretID;
//			String result="";
//			DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//			if(null!=judge.get("remark")){
//				//if(System.currentTimeMillis()-judge.getTimestamp("time").getTime()<1000*7200){
//				if(System.currentTimeMillis()-judge.getTimestamp("time").getTime()<1000*200){
//					result=judge.get("remark");
//				}else{
//					result = dataProcess.sentNetworkRequest(url);
//					Db.update("update ynTemp set `remark`='"+result+"',time='"+df.format(new Date())+"' where id=43  ");
//				}
//			}else{
//				result = dataProcess.sentNetworkRequest(url);
//				Db.update("update ynTemp set `remark`='"+result+"',time='"+df.format(new Date())+"' where id=43  ");
//			}
//			//String result = dataProcess.sentNetworkRequest(url);
//			com.alibaba.fastjson.JSONObject jo = com.alibaba.fastjson.JSONObject.parseObject(result);
//			String accessToken = jo.getString("access_token");
//	 }
	 /*
	  * 获取微信群信息
	  */
	 public void testShareInfo(){
		 YinianDataProcess dataProcess = new YinianDataProcess();
		 String code=this.getPara("code");
		 System.out.println("code="+code);
		 String encryptedData=this.getPara("encryptedData");
		 System.out.println("encryptedData="+encryptedData);
		 String iv=this.getPara("iv");
		 System.out.println("iv="+iv);
		 AES aes = new AES();
		 if(null!=encryptedData&&null!=iv){
			 String result= dataProcess.sentNetworkRequest(CommonParam.getWechatSessionKeyUrl + code);
			 com.alibaba.fastjson.JSONObject jo = com.alibaba.fastjson.JSONObject.parseObject(result);
			 System.out.println("jo="+jo);
			 byte[] resultByte;
			 String session_key = jo.getString("session_key");
				try {
					byte[] ed=Base64.decode(encryptedData);
					byte[] ivb=Base64.decode(iv);
					resultByte = 
							//Pkcs7Encoder.decryptOfDiyIV(Base64.decode(encryptedData), Base64.decode(session_key), Base64.decode(iv));
							aes.decrypt_lk(Base64.decode(encryptedData), Base64.decode(session_key), Base64.decode(iv));
					 System.out.println("resultByte="+resultByte);
					String u="";
					if (null != resultByte && resultByte.length > 0) {
						u = new String(resultByte, "UTF-8");
						com.alibaba.fastjson.JSONObject jou = com.alibaba.fastjson.JSONObject.parseObject(u);
						List<Record> list = new ArrayList<Record>();
						Record r=new Record();
						r.set("openGId", jou.get("openGId"));
						list.add(r);
						jsonString = jsonData.getJson(0, "success", list);
						renderText(jsonString);
					}
					 System.out.println("u="+u);
					// System.out.println("ivb="+ivb);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
			}
	 }
//	 public void testShareInfo(){
//		 YinianDataProcess dataProcess = new YinianDataProcess();
//		 String code=this.getPara("code");
//		 System.out.println("code="+code);
//		 String encryptedData=this.getPara("encryptedData");
//		 System.out.println("encryptedData="+encryptedData);
//		 String iv=this.getPara("iv");
//		 System.out.println("iv="+iv);
//		 AES aes = new AES();
//		 if(null!=encryptedData&&null!=iv){
//			 String result= dataProcess.sentNetworkRequest(CommonParam.getWechatSessionKeyUrl + code);
//			 com.alibaba.fastjson.JSONObject jo = com.alibaba.fastjson.JSONObject.parseObject(result);
//			 System.out.println("jo="+jo);
//			 byte[] resultByte;
//			 String session_key = jo.getString("session_key");
//				try {
//					byte[] ed=Base64.decode(encryptedData);
//					byte[] ivb=Base64.decode(iv);
//					resultByte = aes.decrypt_lk(Base64.decode(encryptedData), Base64.decode(session_key), Base64.decode(iv));
//					 System.out.println("resultByte="+resultByte);
//					String u="";
//					if (null != resultByte && resultByte.length > 0) {
//						u = new String(resultByte, "UTF-8");
//						com.alibaba.fastjson.JSONObject jou = com.alibaba.fastjson.JSONObject.parseObject(u);
//						List<Record> list = new ArrayList<Record>();
//						Record r=new Record();
//						r.set("openGId", jou.get("openGId"));
//						list.add(r);
//						jsonString = jsonData.getJson(0, "success", list);
//						renderText(jsonString);
//					}
//					 System.out.println("u="+u);
//					// System.out.println("ivb="+ivb);
//				} catch (Exception e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				} 
//			}
//	 }
	 public String sentNetworkRequest(String url) {
	        String result = "";

	        try {
	            URL obj = new URL(url);
	            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
	            con.setRequestMethod("POST");
	            con.setRequestProperty("accept", "*/*");
	            con.setDoOutput(true);
	            con.setDoInput(true);
	            con.connect();

	            InputStream input = con.getInputStream();
	            BufferedReader reader = new BufferedReader(new InputStreamReader(input, "utf-8"));
	            StringBuilder builder = new StringBuilder();
	            String line = null;
	            while ((line = reader.readLine()) != null) {
	                builder.append(line);
	            }
	            result = builder.toString();
	        } catch (IOException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
	        }
	        return result;
	    }
//	 public void online(){
//		 String groupid="4534106";
//			List<Record> eventsList = Db.find("select * from events where egroupid=" + groupid + " and estatus=0");
//			for(Record r : eventsList){
//				sentNetworkRequest("https://api.zhuiyinanian.com/YinianProject/yinian/CreateSmallAppPlan2QRCode?type=eventdetail2&id="+r.get("eid").toString());
//				//break;		
//			}
//			System.out.println("ok");
//	 }
	//private static String controllerUrl = "http://localhost/~liukai/1105.php";
	 public void getEventPlan1QRCode(){
		 String eventid=this.getPara("eventid");
		 if(eventid!=null&&!eventid.equals("")){
			 SmallAppQRCode small = new SmallAppQRCode();
			 Event e=new Event().findById(eventid);
			 small.GetSmallAppQRCodeURLByLkTest("spaceEvent", eventid,eventid);//X
			 //e.getStr("etext").substring(0, e.getStr("etext").length()>5?5:e.getStr("etext").length())
		 }else{
			 System.out.println("参数错误");
		 }
	 }
//	public void getPhotoQRCode(){
//		QiniuOperate operate=new QiniuOperate();
//		TestService service=new TestService();
//		String groupid="4595528";
//		List<Record> eventsList = Db.find("select * from events where egroupid=" + groupid + " and estatus=0");
//		for(Record r : eventsList){
//			String eventid=r.getLong("eid").toString();
//			 if(eventid!=null&&!eventid.equals("")&&r.getLong("eid").intValue()>0){ // 1172005
//				 Event e=new Event().findById(eventid);
//				 String mpicUrl="";
//				 String picUrl="";
//				 if(null!=e.get("efirstpic")&&!e.get("efirstpic").equals("")){
//					 //673 宽 534 高
//					 mpicUrl=operate.getDownloadToken(e.get("efirstpic")+"?imageMogr2/auto-orient/blur/50x40/thumbnail/1179x");
//					 picUrl=operate.getDownloadToken(e.get("efirstpic")+"?imageMogr2/auto-orient/thumbnail/1179x");
//				 }			 
//				 String ename=e.getStr("etext").substring(0, e.getStr("etext").length()>10?10:e.getStr("etext").length());
//				 ename=ename.replaceAll("//", "-").replaceAll("/", "-");
//				// ename=ename;
//				 //service.GetLocalSmallAppQRCodeURLPlan2("eventdetail2", e.get("eid"),ename,picUrl,mpicUrl);
//				 service.GetSmallAppQRCodeURLByLkTest("spaceEvent", e.get("eid"),ename,picUrl,mpicUrl);
//				 }else{
//				 System.out.println("参数错误");
//		 }
//		}
//	}
//	 public void getPhotoQRCode3(){
//			QiniuOperate operate=new QiniuOperate();
//			TestService service=new TestService();
//			String eids="1729770,1741562,1741566,1741478,1742030,1742054,1742068,1741560,1741564,1741568,1741572,1741578,";
//			String[] idArray=eids.split(",");
//			for(int i=0;i<idArray.length;i++){
//				Event e=new Event().findById(idArray[i]);
//				String mpicUrl="";
//				String picUrl="";
//				if(null!=e.get("efirstpic")&&!e.get("efirstpic").equals("")){
//					 picUrl=operate.getDownloadToken(e.get("efirstpic")+"?imageMogr2/crop/x927");
//					 mpicUrl =operate.getDownloadToken(e.get("efirstpic")+"?imageMogr2/auto-orient/blur/50x40/thumbnail/1381x");
//				}
//				String ename=e.getStr("etext").substring(0, e.getStr("etext").length()>10?10:e.getStr("etext").length());
//				ename=ename.replaceAll("//", "-").replaceAll("/", "-");
//				service.GetSmallAppQRCodeURLByLkTest2("spaceEvent", e.get("eid"),ename,picUrl,mpicUrl);
//			}
//			System.out.println("====ok");
//		}
	public void getPhotoQRCode2(){
		QiniuOperate operate=new QiniuOperate();
		TestService service=new TestService();
		//String groupid="4595528";
		String groupid="5324302";
		List<Record> eventsList = Db.find("select * from events where egroupid=" + groupid + " and estatus=0");
		for(Record r : eventsList){
			String eventid=r.getLong("eid").toString();
			 if(eventid!=null&&!eventid.equals("")&&r.getLong("eid").intValue()>=3367024){ // 1172005
				 Event e=new Event().findById(eventid);
				 String mpicUrl="";
				 String picUrl="";
				 if(null!=e.get("efirstpic")&&!e.get("efirstpic").equals("")){
					 //闺蜜&基友合照大赛
//					 picUrl=operate.getDownloadToken(e.get("efirstpic")+"?imageView2/1/w/1179/h/827/");
//					 mpicUrl =operate.getDownloadToken(e.get("efirstpic")+"?imageMogr2/auto-orient/blur/50x40/thumbnail/1179x");
					 //4894932
//					 picUrl=operate.getDownloadToken(e.get("efirstpic")+"?imageView2/1/w/2873/h/2504/");
//					 mpicUrl =operate.getDownloadToken(e.get("efirstpic")+"?imageMogr2/auto-orient/blur/50x40/thumbnail/2873x");
					 //最友爱“闺蜜&基友”合照大赛
					// picUrl=operate.getDownloadToken(e.get("efirstpic")+"?imageView2/1/w/1341/h/930/");
//					 picUrl=operate.getDownloadToken(e.get("efirstpic")+"?imageMogr2/crop/x930");
				//	 mpicUrl =operate.getDownloadToken(e.get("efirstpic")+"?imageMogr2/auto-orient/blur/50x40/thumbnail/1341x");
					//纵梦第二季最美合照大赛
					// picUrl=operate.getDownloadToken(e.get("efirstpic")+"?imageView2/1/w/1381/h/927/");
					 //picUrl=operate.getDownloadToken(e.get("efirstpic")+"?imageMogr2/gravity/Center/crop/1381x927");
//					 picUrl=operate.getDownloadToken(e.get("efirstpic")+"?imageMogr2/crop/x927");
//					 mpicUrl ="";
							// operate.getDownloadToken(e.get("efirstpic")+"?imageMogr2/auto-orient/blur/50x40/thumbnail/1381x");
					 /*
					  * 【江西赛区】花样宿舍校园拍照大赛  4931684
【河北赛区】花样宿舍校园拍照大赛   4931692
【河南赛区】花样宿舍校园拍照大赛   4931686
【湖北赛区】花样宿舍校园拍照大赛   4931676
					  */
					// picUrl=operate.getDownloadToken(e.get("efirstpic")+"?imageView2/1/w/589/h/459/");
					// mpicUrl =operate.getDownloadToken(e.get("efirstpic")+"?imageMogr2/auto-orient/blur/50x40/thumbnail/589x");
				 /*最创意合照大赛*/
					// picUrl=operate.getDownloadToken(e.get("efirstpic")+"?imageView2/1/w/520/h/280/");					
					 /*最情深『老朋友』合照大赛*/
					// picUrl=operate.getDownloadToken(e.get("efirstpic")+"?imageView2/1/w/892/h/725/");
					 /*纵梦合照大赛*/
					 picUrl=operate.getDownloadToken(e.get("efirstpic")+"?imageView2/1/w/1316/h/1125/");	
				 }			 
				 String ename=e.getStr("etext").substring(0, e.getStr("etext").length()>10?10:e.getStr("etext").length());
				 ename=ename.replaceAll("//", "-").replaceAll("/", "-");
				// ename=ename;
				 //service.GetLocalSmallAppQRCodeURLPlan2("eventdetail2", e.get("eid"),ename,picUrl,mpicUrl);
				 service.GetSmallAppQRCodeURLByLkTest2("spaceEvent", e.get("eid"),ename,picUrl,mpicUrl);
				 //service.GetSmallAppQRCodeURLByLkTest2("spaceEvent", e.get("eid"),ename,picUrl,mpicUrl);
				 }else{
				 System.out.println("参数错误");
		 }
		}
		System.out.println("====ok");
	}
	public void getGroupQRCode() {
		SmallAppQRCode small = new SmallAppQRCode();
//		 small.GetLocalSmallAppQRCodeURLPlan2("eventdetail2", 1587983,"黄子佳");
//		 small.GetLocalSmallAppQRCodeURLPlan2("eventdetail2", 1588473,"许梦航1");
//		 small.GetLocalSmallAppQRCodeURLPlan2("eventdetail2", 1588751,"许梦航2");
//		 
//		 small.GetLocalSmallAppQRCodeURLPlan2("eventdetail2", 1588807,"许梦航3");
//	
		small.GetLocalSmallAppQRCodeURLPlan2("spaceQR", 5638422  ,5638422+"");
//		small.GetLocalSmallAppQRCodeURLPlan2("spaceQR", 5405208,5405208+"");
//		small.GetLocalSmallAppQRCodeURLPlan2("spaceQR", 5405210,5405210+"");
		
//		
		
//		small.GetLocalSmallAppQRCodeURLPlan2("spaceQR", 3510079);
//		small.GetLocalSmallAppQRCodeURLPlan2("spaceQR", 3615733);
		
//		 small.GetSmallAppQRCodeURLByLkTest("space", 5400018 +"","5400018");
//		 small.GetSmallAppQRCodeURLByLkTest("space", 4931686+"","河南赛区");
//		 small.GetSmallAppQRCodeURLByLkTest("space", 4931684+"","江西赛区");
//		 small.GetSmallAppQRCodeURLByLkTest("space", 4931676+"","湖北赛区");
		//small.GetSmallAppQRCodeURLByLkTest("space", 4534106+"",4534106+"");//X
		
		//small.GetLocalSmallAppQRCodeURLPlan2("eventdetail2", "1070099");
	//	small.GetLocalSmallAppQRCodeURLPlan2("eventdetail2", "1070113");//X
//		String groupid = "5324302";
//////////			
//		List<Record> eventsList = Db.find("select * from events where egroupid=" + groupid + " and estatus=0");
////////////////	
//		for(Record r : eventsList){
//			String eventid=r.getLong("eid").toString();
//			 if(eventid!=null&&!eventid.equals("")&&r.getLong("eid").intValue()>0){ // 1172005
//				 Event e=new Event().findById(eventid);
//				 String ename=e.getStr("etext").substring(0, e.getStr("etext").length()>10?10:e.getStr("etext").length());
//				 ename=ename.replaceAll("//", "-").replaceAll("/", "-");
//				// ename=ename;
//				// small.GetLocalSmallAppQRCodeURLPlan2("eventdetail2", e.get("eid"),ename);//e.getStr("etext").substring(0, e.getStr("etext").length()>10?10:e.getStr("etext").length())
//				 small.GetSmallAppQRCodeURLByLkTest("spaceEvent", eventid,e.getStr("etext").substring(0, e.getStr("etext").length()>10?10:e.getStr("etext").length()));//X
//			 }else{
//				 System.out.println("参数错误");
//		 }
//		}

		/* String url = small.GetSmallAppQRCodeURLByLkTest("space","2806362"
		 ,"2806362");
		 String url2 = small.GetSmallAppQRCodeURLByLkTest("space","2806277"
				 ,"2806277");*/
//		 url = small.GetSmallAppQRCodeURLByLkTest("space","2733581"
//				 ,"2733581");
//		 url = small.GetSmallAppQRCodeURLByLkTest("space","2733592"
//				 ,"2733592");
//		 url = small.GetSmallAppQRCodeURLByLkTest("space","2733605"
//				 ,"2733605");
//		url = small.GetSmallAppQRCodeURLByLkTest("space","2733610"
//				 ,"2733610");
//		 
		// url = small.GetSmallAppQRCodeURLByLkTest("spaceEvent","410133"
		// ,"410133");
		// url = small.GetSmallAppQRCodeURLByLkTest("spaceEvent","410134"
		// ,"410134");
		// url = small.GetSmallAppQRCodeURLByLkTest("spaceEvent","417949"
		// ,"417949");
		// url = small.GetSmallAppQRCodeURLByLkTest("spaceEvent","417971"
		// ,"417971");
		// url = small.GetSmallAppQRCodeURLByLkTest("spaceEvent","417976"
		// ,"417976");
		// url = small.GetSmallAppQRCodeURLByLkTest("spaceEvent","417979"
		// ,"417979");
		System.out.println("====ok");
	}
	public void getPlan2QRCode() {
		SmallAppQRCode small = new SmallAppQRCode();
		small.GetSmallAppQRCodeURLPlan2("eventdetail2", 549671+"");
	}
	public void GetControllerValue() throws IOException {
		jsonString = jsonData.getJson(2, "参数错误");
		BufferedReader reader = null; 
		try {
			String strUrl = "http://picture.zhuiyinanian.com/yinian/compValue.txt";
			URL url = new URL(strUrl); 
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			InputStreamReader input = new InputStreamReader(conn.getInputStream());
			reader = new BufferedReader(input);
			StringBuffer buffer = new StringBuffer();
			String line = reader.readLine();
			while (line!=null) {
				buffer.append(line);
				line = reader.readLine();
			}
			reader.close(); 
			String content = buffer.toString();
			//String content = UrlUtils.loadJson(controllerUrl);
			//String content = "{'openloading':false,'canlogin':true,'setlikecnt':true}";
			// List<Record> likelist = new Event().GetListByGroup(groupid, 1);//
			// 获取点赞第一名
			// List<Record> publishlist = new
			// Event().GetUsePublishPhotoCont(groupid, 0, false, 1);// 获取照片达人第一名

			// String returnValue=jsonData.getJson(2, "请求参数错误");
			Record r = new Record();
			if (content != null && content.length() > 0) {
				// returnValue=jsonData.getJson(0, "success",
				// JSONArray.fromObject(content));
				JSONObject j = JSONObject.fromObject(content);
				r.set("openloading", j.getBoolean("openloading"));
				r.set("canlogin", (new Random().nextInt(100)+1)>j.getInt("canlogin"));
				r.set("setlikecnt", j.getBoolean("setlikecnt"));
				// r.set("context", JSONObject.fromObject(content));
			}
			List<Record> newReturnList = new ArrayList<Record>();
			newReturnList.add(r);
			jsonString = jsonData.getSuccessJson(newReturnList);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			reader.close();
			renderText(jsonString);
		}

	}
	/**
	 * 分享相册二维码
	 */
	public void getPhotoQRCode3(){
		QiniuOperate operate=new QiniuOperate();
		TestService service=new TestService();
		String groupid = this.getPara("groupid");
		String userid = this.getPara("userid");
		//查询相册内最新发布的动态id 如果为null时eid默认为0
		List<Record> eventsList = Db.find("select IFNULL(MAX(eid),0) as eid from events where egroupid=" + groupid + " and estatus=0" +" and eMain=0");
		String eventid=eventsList.get(0).getLong("eid").toString();
		User u = new User().findById(userid);
		String url = "";
		Group g = new Group();
		if(groupid!=null&&!groupid.equals("")) {
			g=new Group().findById(groupid);
			String gQRCode = g.get("gQRCode");
			if(gQRCode == null||gQRCode .equals("")) {
				String mpicUrl = "";
				String picUrl = "";
				Event e = new Event();
				if(!eventid.equals("0")) {
					e=new Event().findById(eventid);
					//获取相册最新动态的第一张照片作为二维码的背景图
					mpicUrl =operate.getDownloadToken(e.get("efirstpic")+"?imageMogr2/auto-orient/blur/1x1/thumbnail/1179x");
					//获取用户头像
					picUrl =operate.getDownloadToken(u.get("upic")+"?imageView2/1/w/126/h/126/");
				}else {
					//如果相册为null则采用默认相册封面为背景图
					mpicUrl =operate.getDownloadToken(g.get("gpic")+"?imageMogr2/auto-orient/blur/1x1/thumbnail/1179x");
					picUrl =operate.getDownloadToken(u.get("upic")+"?imageView2/1/w/126/h/126/");
				}
					
				String gname = g.get("gname");
				int numberCharacter = 0;
			    int enCharacter = 0;
				if(gname.length()>6) {
					gname = gname.substring(0, 6);
					gname = gname + "..";
					//统计相册名中的字母数字
					for (int i = 0; i < gname.length(); i++) {
			            char tmp = gname.charAt(i);
			            if ((tmp >= 'A' && tmp <= 'Z') || (tmp >= 'a' && tmp <= 'z')) {
			                enCharacter ++;
			            } else if ((tmp >= '0') && (tmp <= '9')) {
			                numberCharacter ++;
			            } 
			        }
					int count = numberCharacter + enCharacter;
					int mm = count/2;
					for(int x=0;x<mm;x++) {
						gname =" "+gname;
					}
				}else {
					
					int len = (6-gname.length())/2;
					int le = (6-gname.length())%2;
					for (int i = 0; i < gname.length(); i++) {
			            char tmp = gname.charAt(i);
			            if ((tmp >= 'A' && tmp <= 'Z') || (tmp >= 'a' && tmp <= 'z')) {
			                enCharacter ++;
			            } else if ((tmp >= '0') && (tmp <= '9')) {
			                numberCharacter ++;
			            } 
			        }
					int count = numberCharacter + enCharacter;
					int mm = count/2;
					if(le==0) {
						for(int x=0;x<=len;x++) {
							gname = "  "+gname;
						}
					}
					if(le==1) {
						for(int x=0;x<=len;x++) {
							gname = "  "+gname;
						}
						gname = " " + gname;
					}
						for(int x=0;x<mm;x++) {
							gname =" "+gname;
						}
				}
				url = service.GetLocalSmallAppQRCodeURLPlan3("spaceQR", groupid,gname,picUrl,mpicUrl);
				if(url!=null&&url.indexOf("QRCodeError.png")!=-1) {
					
				}else{
					e.set("gQRCode", url);
					e.update();
				}
			} else {
				url = gQRCode;
			}
		}
		System.out.println(url);
		Record resultRecord = new Record();
		resultRecord.set("url", url);
		List<Record> resultList = new ArrayList<Record>();
		resultList.add(resultRecord);
		jsonString = jsonData.getSuccessJson(resultList);
		renderText(jsonString);
	}

	
	/**
	 * 分享动态二维码--ly
	 */
	public void getPhotoQRCode4(){
		QiniuOperate operate=new QiniuOperate();
		TestService service=new TestService();
		String groupid = this.getPara("groupid");
		String userid = this.getPara("userid");
		String eventid = this.getPara("eventid");
		if(eventid!=null&&!eventid.equals("")){
			String url = "";
			Event e=new Event().findById(eventid);
			if(null!=e.get("egroupid")&&e.get("egroupid").toString().equals("5460577")){
				SmallAppQRCode small = new SmallAppQRCode();
				url = small.GetSmallAppQRCodeURLPlan2_5460577("eventdetail2", eventid);//使用统一底图
				if(url!=null&&url.indexOf("QRCodeError.png")!=-1) {
					
				}else{
					e.set("eQRCode", url);
					e.update();
				}
			}
			User u = new User().findById(userid);
			Group g = new Group().findById(groupid);
			String gname = g.get("gname");
			String eQRCode = e.get("eQRCode");
			
			if(eQRCode == null||eQRCode .equals("")) {
				int numberCharacter = 0;
			    int enCharacter = 0;
			    if(gname.length()>6) {
					gname = gname.substring(0, 6);
					gname = gname + "..";
					for (int i = 0; i < gname.length(); i++) {
			            char tmp = gname.charAt(i);
			            if ((tmp >= 'A' && tmp <= 'Z') || (tmp >= 'a' && tmp <= 'z')) {
			                enCharacter ++;
			            } else if ((tmp >= '0') && (tmp <= '9')) {
			                numberCharacter ++;
			            } 
			        }
					int count = numberCharacter + enCharacter;
					int mm = count/2;
					for(int x=0;x<mm;x++) {
						gname =" "+gname;
					}
				}else {
					
					int len = (6-gname.length())/2;
					int le = (6-gname.length())%2;
					for (int i = 0; i < gname.length(); i++) {
			            char tmp = gname.charAt(i);
			            if ((tmp >= 'A' && tmp <= 'Z') || (tmp >= 'a' && tmp <= 'z')) {
			                enCharacter ++;
			            } else if ((tmp >= '0') && (tmp <= '9')) {
			                numberCharacter ++;
			            } 
			        }
					int count = numberCharacter + enCharacter;
					int mm = count/2;
					if(le==0) {
						for(int x=0;x<=len;x++) {
							gname = "  "+gname;
						}
					}
					if(le==1) {
						for(int x=0;x<=len;x++) {
							gname = "  "+gname;
						}
						gname = " " + gname;
					}
						for(int x=0;x<mm;x++) {
							gname =" "+gname;
						}
				}
				System.out.println(gname);
				String mpicUrl="";
				String picUrl="";
				if(null!=e.get("efirstpic")&&!e.get("efirstpic").equals("")&&
						null!=u.get("upic")&&!u.get("upic").equals("")){
					picUrl=operate.getDownloadToken(u.get("upic")+"?imageView2/1/w/126/h/126/");
					//mpicUrl=operate.getDownloadToken(e.get("efirstpic")+"?imageView2/1/w/500/h/500/");
					mpicUrl =operate.getDownloadToken(e.get("efirstpic")+"?imageMogr2/auto-orient/blur/1x1/thumbnail/1179x");
//						System.out.println(mpicUrl);
				}
				
				url = service.GetLocalSmallAppQRCodeURLPlan3("eventdetail2", eventid,gname,picUrl,mpicUrl);
				if(url!=null&&url.indexOf("QRCodeError.png")!=-1) {
					
				}else{
					e.set("eQRCode", url);
					e.update();
				}
			}else {
				url = eQRCode;
			}
			Record resultRecord = new Record();
			resultRecord.set("url", url);
			List<Record> resultList = new ArrayList<Record>();
			resultList.add(resultRecord);
			jsonString = jsonData.getSuccessJson(resultList);
			renderText(jsonString);
		 }
	}

	
	public void getUserCnt(){
		String groupid = this.getPara("groupid");

		int total = 0;
		List<Record> list = Db.find(
				"select DATE(utime) as date,count(*) as num from groupmembers,users where userid=gmuserid and gmgroupid="
						+ groupid + " and TIMEDIFF(gmtime,utime)<10 GROUP BY DATE(utime) desc");
		String result = "<html><body><table border=\"1\">";
		for (Record record : list) {
			String date = record.get("date").toString();
			int num = Integer.parseInt(record.get("num").toString());
			total += num;
			result += ("<tr><td>" + date + "</td><td>" + num + "</td></tr>");
		}
		result += "</table>总新用户数：" + total + "人</n></body></html>";
		renderText(result);
	}

	public void zipCallBack(){
		
	}
	public void downPic(){
		String accessKey = "2caeYdL9QSwjhpJc2v05LLgaOrk_Mc_HterAtD28";
		// 七牛云密钥
		String secretKey = "XQYY6AE3rhhp-ep9-xwOOUc2noRyAvXu8uLjkTMT";
		Auth auth = Auth.create(accessKey, secretKey);
		List<Record> list=Db.find("select * from pictures where peid "
				+ "in (select eid from events where egroupid=4362296 and estatus=0) and pstatus=0");
		SmallAppQRCode sm=new SmallAppQRCode();
		for(Record r:list){
			String url=r.get("poriginal").toString();
			
			down(auth.privateDownloadUrl(url),url.substring(31, url.length()));			
		}
	}
	public void down(String url,String fileName){
		PrintWriter out = null;
		InputStream in = null;
		try {
			URL realUrl = new URL(url);
			// 打开和URL之间的连接
			URLConnection conn = realUrl.openConnection();
			// 设置通用的请求属性
			conn.setRequestProperty("accept", "*/*");
			conn.setRequestProperty("connection", "Keep-Alive");
			conn.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
			// 发送POST请求必须设置如下两行
			conn.setDoOutput(true);
			conn.setDoInput(true);
			// 获取URLConnection对象对应的输出流
			out = new PrintWriter(conn.getOutputStream());
			// 发送请求参数
			//out.print(param);
			// flush输出流的缓冲
			out.flush();

			// 获取输入流并获取图片
			in = conn.getInputStream();
			byte[] data = new byte[1024];
			int len = 0;
			FileOutputStream fileOutputStream = null;
			//String path = CommonParam.tempPictureServerSavePath + fileName;
			
			String path = CommonParam.pictureSaveLocalPath + fileName;
			fileOutputStream = new FileOutputStream(path);
			while ((len = in.read(data)) != -1) {
				fileOutputStream.write(data, 0, len);
			}
			String msg=new String(data);
			System.out.println(msg);
			// 关闭流
			fileOutputStream.close();
			//JSONObject jo = JSONObject.parseObject(msg);
//			if(msg.indexOf("errcode")!=-1) {
//				//return "http://oibl5dyji.bkt.clouddn.com/QRCodeError.png";//获取失败时返回"error"图片路径
//			}else {
//				QiniuOperate qiniu = new QiniuOperate();
//				//qiniu.uploadFileToOpenSpace(CommonParam.pictureSaveLocalPath + fileName, fileName);
//				//qiniu.uploadFileToOpenSpace(CommonParam.tempPictureServerSavePath + fileName, fileName);
//				return CommonParam.qiniuOpenAddress + fileName;
//			}
		} catch (Exception e) {
			System.out.println("发送 POST 请求出现异常！" + e);
			e.printStackTrace();
		}
		// 使用finally块来关闭输出流、输入流
		finally {
			try {
				if (out != null) {
					out.close();
				}
				if (in != null) {
					in.close();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		//return null;
	//}
	}
	
	/**
	 * 生成共享纸巾圆形二维码
	 */
	public void getPhotoQRCodeSharePaper(){
		QiniuOperate operate=new QiniuOperate();
		TestService service=new TestService();
		String groupid ="5298806";
		String zj= this.getPara("zj");
		
		String url = service.getSharePaperQRCodeURL("spaceQR", groupid,zj);
		System.out.println(url);
		Record resultRecord = new Record();
		resultRecord.set("url", url);
		List<Record> resultList = new ArrayList<Record>();
		resultList.add(resultRecord);
		jsonString = jsonData.getSuccessJson(resultList);
		renderText(jsonString);
	}
	/**
	 * 生成友宝圆形二维码
	 */
	public void getPhotoQRCodeSharePaper2(){
		QiniuOperate operate=new QiniuOperate();
		TestService service=new TestService();
		String groupid ="5451186";
		String zj= this.getPara("zj");
		
		String url = service.getSharePaperQRCodeURL("spaceQR", groupid,zj);
		System.out.println(url);
		Record resultRecord = new Record();
		resultRecord.set("url", url);
		List<Record> resultList = new ArrayList<Record>();
		resultList.add(resultRecord);
		jsonString = jsonData.getSuccessJson(resultList);
		renderText(jsonString);
	}
	/*
	 * 生成口袋合作圆形二维码
	 */
	public void getPhotoQRCodeKD(){
		for(int i=1;i<101;i++){
		QiniuOperate operate=new QiniuOperate();
		TestService service=new TestService();
		String groupid ="5193566";
		String zj= this.getPara("zj");
		
		String url = service.getSharePaperQRCodeURL("spaceQR", groupid,i+"");
		System.out.println(url);
		Record resultRecord = new Record();
		resultRecord.set("url", url);
		List<Record> resultList = new ArrayList<Record>();
		resultList.add(resultRecord);
		jsonString = jsonData.getSuccessJson(resultList);
		}
		renderText(jsonString);
	}
	public static void main(String[] a){
//		String s="8ec73aa975b8";
//		Long ts=System.currentTimeMillis();
//		String url="http://www.zhenhuaonline.cn/api/in/thirdparty/op_app/v1/handle?app_id=a_qLCogTZJjJbMU6&timestamp="+ts+"&sign=";		
//		String param1 = getMD5("api=User.Tissue.Order.TicketOrdertimestamp="+ts+"tissue_ticket="+s+"bjSjRNJmXvBE16faKxo3caLcFjGSZ5bV");
//		url+=param1;
//		param1="api=User.Tissue.Order.TicketOrder&timestamp="+ts+"&tissue_ticket="+s;
//		HttpUtils u=new HttpUtils();
//		System.out.println(u.sendPost(url, param1));
	}
	/*
	 * 共享纸巾post
	 */
	public void postToZJ(){
		Long ts=System.currentTimeMillis();
		String url="http://www.zhenhuaonline.cn/api/in/thirdparty/op_app/v1/handle?app_id=a_qLCogTZJjJbMU6&timestamp="+ts+"&sign=";		
		String param1 = getMD5("api=User.Tissue.Order.TicketOrdertimestamp="+ts+"tissue_ticket=07cf45d58966"+"bjSjRNJmXvBE16faKxo3caLcFjGSZ5bV");
		url+=param1;
		param1="api=User.Tissue.Order.TicketOrder&timestamp="+ts+"&tissue_ticket=07cf45d58966";
		HttpUtils u=new HttpUtils();
		System.out.println(u.sendPost(url, param1));
		//{"status":-1,"status_str":"已发放奖励","api_data":[]}
	}
//	public void postToZJ2(){
//		Long ts=System.currentTimeMillis();
//		String url="http://www.zhenhuaonline.cn/api/in/thirdparty/op_app/v1/handle?app_id=a_qLCogTZJjJbMU8&timestamp="+ts+"&sign=";		
//		String param1 = getMD5("api=User.Tissue.Order.ExamineOrderorder_serial_id=ORV151515943722846868timestamp="+ts+""+"bjSjRNJmXvBE16faKxo3caLcFjGSZ5bV");
//		url+=param1;
//		param1="api=User.Tissue.Order.ExamineOrder&timestamp="+ts+"&order_serial_id=ORV151515943722846868";
//		HttpUtils u=new HttpUtils();
//		System.out.println(u.sendPost(url, param1));
//		//{"status":1,"status_str":"","api_data":{"order_serial_id":"ORV151515943722846868","vm_openid":"a43b2504fe7b52bbff3cc3bfd209cb6a","create_time":1515159437,"status":1}}
//	}
	 /** 
     * 生成md5 
     *  
     * @param message 
     * @return 
     */  
    public static String getMD5(String message) {  
    String md5str = "";  
    try {  
        // 1 创建一个提供信息摘要算法的对象，初始化为md5算法对象  
        MessageDigest md = MessageDigest.getInstance("MD5");  
  
        // 2 将消息变成byte数组  
        byte[] input = message.getBytes();  
  
        // 3 计算后获得字节数组,这就是那128位了  
        byte[] buff = md.digest(input);  
  
        // 4 把数组每一字节（一个字节占八位）换成16进制连成md5字符串  
        md5str = bytesToHex(buff);
        md5str = md5str.toLowerCase();
        System.out.println(md5str);
  
    } catch (Exception e) {  
        e.printStackTrace();  
    }  
    return md5str;  
    }  
    /** 
     * 二进制转十六进制 
     *  
     * @param bytes 
     * @return 
     */  
    public static String bytesToHex(byte[] bytes) {  
    StringBuffer md5str = new StringBuffer();  
    // 把数组每一字节换成16进制连成md5字符串  
    int digital;  
    for (int i = 0; i < bytes.length; i++) {  
        digital = bytes[i];  
  
        if (digital < 0) {  
        digital += 256;  
        }  
        if (digital < 16) {  
        md5str.append("0");  
        }  
        md5str.append(Integer.toHexString(digital));  
    }  
    return md5str.toString().toUpperCase();  
    }
    /*
     * 热点图片导入
     */
    public void importHotPic(){
    	String groupid=5091580+"";
    	List<Record> eventsList=Db.find("select * from events where egroupid="+groupid+" and estatus=0 ");
    	for(Record r:eventsList){
    		String text=r.get("etext");
    		String type=r.getLong("eMain").toString();
    		List<Record> picList=Db.find("select * from pictures where peid="+r.getLong("eid")+" and pstatus=0");
    		for(Record p:picList){
    			  int max=98;
  		        int min=53;
  		        Random random = new Random();  		       
  		        int s = random.nextInt(max)%(max-min+1) + min;
    			HotPic h=new HotPic();
    			h.set("hPic", p.get("poriginal"))
    			.set("hPcover", p.get("pcover"))
    			.set("hot", s)
    			.set("hText", text)
    			.set("hStatus", 0)
    			.set("hType", type);
    			h.save();
    		}
    	}
    	System.out.println("ok");
    }
    /*
     * 刘凯，紧急推送
     */
	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private static YinianDataProcess dataProcess = new YinianDataProcess();
	public void allUsersPublish(){
		publishMsg("oIrcI0c0AncUqFWGRm2ciK766f4I","6befde11cbb2989abb123aff3372dbcd");
		Db.update("delete from formid where userID=816596 and formID='6befde11cbb2989abb123aff3372dbcd'");
//		Db.update("delete from formid where time <DATE_ADD(Now(),INTERVAL -7 day)");
//		List<Record> formidList = Db.find("select formid.userID userID,formID,uopenid from formid,users where 1 "
//				+ "and formid.userID=users.userid and status=0 and "
//				+ "time between DATE_ADD(Now(),INTERVAL -7 day) and Now() "
//				+ "group by formid.userID order by time asc");
//		for (Record fr : formidList) {
//			publishMsg(fr.get("uopenid"),fr.get("formID"));
//			Db.update("delete from formid where userID="+fr.get("userID")+" and formID='"+fr.get("formID")+"'");
//		}
	}
    public void publishMsg(String openID,String formID){
    	//String openID=this.getPara("openid");
    	//String formID=this.getPara("formid");
    	// 获取access_token
    			String accessToken = SmallAppQRCode.GetSmallAppAccessToken();
    			// 模板ID
    			String templateID = CommonParam.callbackPushTemplateID;
    			// 获取页面路径
    			String path = "pages/viewscoll/viewscoll?port=推送&groupid=5314062";
    			// 参数
    			String time = sdf.format(new Date());
    			
    			Map<String, Map<String, String>> map = new HashMap<String, Map<String, String>>();
    			Map<String, String> tempMap = new HashMap<String, String>();
    			tempMap.put("value", "【抢个100W红包试试？】");
    			map.put("keyword1", tempMap);
    			tempMap = new HashMap<String, String>();
    			tempMap.put("value", time);
    			map.put("keyword2", tempMap);
    			
    			// 构造请求URL和参数
    			String url = "https://api.weixin.qq.com/cgi-bin/message/wxopen/template/send?access_token=" + accessToken + "";
    			com.alibaba.fastjson.JSONObject param = new com.alibaba.fastjson.JSONObject();
    			param.put("touser", openID);
    			param.put("template_id", templateID);
    			param.put("page", path);
    			param.put("form_id", formID);
    			param.put("data", map);
    			// 发送请求
    			dataProcess.sendPost(url, param.toJSONString(), "json");
    }

    
    /**
	 * 添加签到积分
	 */
	public void addPoints() {
		TestService service = new TestService();
		List<Record> list8 = Db.find("SELECT signUserID,signEndDate,signStartDate, DATEDIFF(signEndDate,signStartDate)+1 as num from sign where TO_DAYS(now())=to_days(signEndDate) AND signType=0");
		String typeName = "老用户连续签到奖励";
		int count = 0;
		int point = 0;
		int count1=0;
		int count2=0;
		int count3=0;
		int count4=0;
		int count5=0;
		int count6=0;
		int count7=0;
		for (Record record : list8) {
			String userid = record.get("signUserID").toString();
			Long num =record.get("num");
			count+=1;
			if(num==1) {
				point = 10;
				count1+=1;
				System.out.println("userid="+userid + "-------" + "num1=" + num +"-----"+ "point1="+point);
			} else if (num>=2 && num<=5) {
				point = (int) (num*20);
				count2+=1;
				System.out.println("userid="+userid + "-------" + "num2=" + num +"-----"+ "point2="+point);
			} else if(num>5 && num<=10) {
				point = (int) (num*50);
				count3+=1;
				System.out.println("userid="+userid + "-------" + "num3=" + num +"-----"+ "point3="+point);
			} else if (num>10 && num<=30) {
				point = (int) (num*100);
				count4+=1;
				System.out.println("userid="+userid + "-------" + "num4=" + num +"-----"+ "point4="+point);
			}else if (num>30 && num<=50) {
				point = (int) (num*300);
				count5+=1;
				System.out.println("userid="+userid + "-------" + "num5=" + num +"-----"+ "point5="+point);
			}else if (num>50 && num<=100) {
				point = (int) (num*500);
				count6+=1;
				System.out.println("userid="+userid + "-------" + "num6=" + num +"-----"+ "point6="+point);
			}else if (num>100) {
				point = (int) (num*1000);
				count7+=1;
				System.out.println("userid="+userid + "-------" + "num7=" + num +"-----"+ "point7="+point);
			}
			
			//service.addPoints(userid+"",point,typeName);
			
		}
		System.out.println("count1="+count1);
		System.out.println("count2="+count2);
		System.out.println("count3="+count3);
		System.out.println("count4="+count4);
		System.out.println("count5="+count5);
		System.out.println("count6="+count6);
		System.out.println("count7="+count7);
		
		System.out.println("count="+count);
		renderText("=======ok");
	}
	
	/**
	 * 添加额外的签到积分
	 */
	public void addOtherSignPoints() {
		TestService service = new TestService();
		List<Record> list = Db.find("SELECT signUserID,signCount from sign where TO_DAYS(now())-1=to_days(signEndDate) AND signType=0");
		int count1 = 0;
		int count2 = 0;
		int count3 = 0;
		int count4 = 0;
		int count5 = 0;
		int count6 = 0;
		int count7 = 0;
		int count8 = 0;
		for (Record record : list) {
			Long signUserID = record.get("signUserID");
			Integer signCount = record.get("signCount");
			String typeName = "老用户连续签到额外奖励积分";
			if(signCount>=7) {
				
				List<Record> list2 = Db.find("select * from encouragelogistics where elUserID=" + signUserID + " and elType='signLevelOne'");
				if(list2.size()!=0) {
					String name = list2.get(0).get("elTypeName");
					if(name!=null && !name.equals("")) {
						System.out.println("signUserID2="+signUserID+"----"+"name2="+name);
					} else {
						count1+=1;
						//service.addPoints(signUserID+"",1000,typeName);
						System.out.println("signUserID2="+signUserID+"----"+"count1="+count1);
					}
				}else {
					count2+=1;
					//service.addPoints(signUserID+"",1000,typeName);
					System.out.println("signUserID="+signUserID+"count1="+count2);
				}
			}
			if(signCount>=14) {
				List<Record> list3 = Db.find("select * from encouragelogistics where elUserID=" + signUserID + " and elType='signLevelTwo'");
				if(list3.size()!=0) {
					String name = list3.get(0).get("elTypeName");
					if(name!=null && !name.equals("")) {
						System.out.println("signUserID3="+signUserID+"----"+"name3="+name);
					}else {
						count3+=1;
						//service.addPoints(signUserID+"",5000,typeName);
						System.out.println("signUserID3="+signUserID+"-----"+"count2="+count3);
					}
					
				}else {
					//service.addPoints(signUserID+"",5000,typeName);
					count4+=1;
					System.out.println("signUserID3="+signUserID+"-----"+"count2="+count4);
				}
			}
			
			if(signCount>=30) {
				List<Record> list4 = Db.find("select * from encouragelogistics where elUserID=" + signUserID + " and elType='signLevelThree'");
				if(list4.size()!=0) {
					String name = list4.get(0).get("elTypeName");
					if(name!=null && !name.equals("")) {
						System.out.println("signUserID4="+signUserID+"----"+"name4="+name);
					}else {
						count5+=1;
						//service.addPoints(signUserID+"",10000,typeName);
						System.out.println("signUserID4="+signUserID+"----"+"count5="+count5);
						
					}
				}else {
					//service.addPoints(signUserID+"",10000,typeName);
					count6+=1;
					System.out.println("signUserID4="+signUserID+"----"+"count6="+count6);
				}
			}
			
			if(signCount>=100) {
				List<Record> list5 = Db.find("select * from encouragelogistics where elUserID=" + signUserID + " and elType='signLevelFour'");
				if(list5.size()!=0) {
					String name = list5.get(0).get("elTypeName");
					if(name!=null && !name.equals("")) {
						System.out.println("signUserID5="+signUserID+"----"+"name5="+name);
					}else {
						count7+=1;
						//service.addPoints(signUserID+"",20000,typeName);
						System.out.println("signUserID5="+signUserID+"----"+"count7"+count7);
					}
				}else {
					count8+=1;
					//service.addPoints(signUserID+"",20000,typeName);
					System.out.println("signUserID5="+signUserID+"----"+"count8"+count8);
				}
			}
			
		}
		System.out.println("count1="+count1);
		System.out.println("count2="+count2);
		System.out.println("count3="+count3);
		System.out.println("count4="+count4);
		System.out.println("count5="+count5);
		System.out.println("count6="+count6);
		System.out.println("count7="+count7);
		System.out.println("count8="+count8);
		renderText("=======ok");
	}
	
	/**
	 * 添加邀请人数达到50人以上的用户积分
	 */
	public void addInvitePoints() {
		TestService service = new TestService();
		//查询邀请的人数
		List<Record> encourageInfo = Db.find("select inviteNum,encourageUserID from encourage where inviteNum>=50");
		if(encourageInfo.size()!=0) {
			for (Record record : encourageInfo) {
				Long inviteNum = record.get("inviteNum");
				String userid = record.get("encourageUserID").toString();
				int point = 50000;
				String typeName = "老用户邀请好友奖励积分";
				//service.addPoints(userid+"", point,typeName);
				System.out.println("inviteNum="+inviteNum+"-----"+"userid="+userid);
			}
			
		}
		renderText("=======ok");
	}
	
	/**
	 * 上传照片数量积分
	 */
	public void addPhotoPoints() {
		long startTime = System.currentTimeMillis();    //获取开始时间
		System.out.println(startTime);
		//创建一个线程
		ExecutorService exec = Executors.newFixedThreadPool(5);
		for(int i=0; i<5; i++) {
			exec.execute(new PointsThread(i));
		}
		// 关闭线程池
		exec.shutdown();
		// 判断线程池执行完毕再继续执行
		try {
			// awaitTermination返回false即超时会继续循环，返回true即线程池中的线程执行完成主线程跳出循环往下执行，每隔1秒循环一次
			while (!exec.awaitTermination(1, TimeUnit.SECONDS))
				;
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		long endTime = System.currentTimeMillis();    //获取结束时间
		System.out.println("总共花费="+(endTime-startTime));
		renderText("=======ok");
	}
	
	/*
 	 * 智能绘工坊对接-开始
 	 * 获取风格列表
 	 */
 	public void getStyleList(){
 		HttpUtils utils=new HttpUtils();
 		String value=utils.sendGet("http://39.106.60.85/arc/style/styleList", "");
 		if(null!=value&&!value.equals("")){
 			JSONObject j=JSONObject.fromObject(value);
 			System.out.println("j="+j);
 			if(null!=j&&j.containsKey("data")){
 				JSONObject data=j.getJSONObject("data");
 				if(null!=data&&data.containsKey("styles")){
 					JSONArray array=data.getJSONArray("styles");
 					System.out.println("array="+array);
 				}
 			}
 			
 		}
 	}
 	/*
 	 * 智能绘工坊对接-开始
 	 * 获取风格图片
 	 */
 	public void getStylePhoto(){
 		HttpUtils utils=new HttpUtils();
 		String value=utils.sendPost("http://39.106.60.85/arc/style/styleList", "");
 	}
	
	
	/////////////////////广告位////////////////////////////////
 	public void GetAdsenseValue() throws IOException {
		jsonString = jsonData.getJson(2, "参数错误");
		BufferedReader reader = null; 
		try {
			String strUrl = "http://picture.zhuiyinanian.com/yinian/adsenseValue.txt";
			URL url = new URL(strUrl); 
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			InputStreamReader input = new InputStreamReader(conn.getInputStream());
			reader = new BufferedReader(input);
			//reader = new BufferedReader(new InputStreamReader(new FileInputStream("D:\\leiyu\\adsenseValue.txt")));
			StringBuffer buffer = new StringBuffer();
			String line = reader.readLine();
			while (line!=null) {
				buffer.append(line);
				line = reader.readLine();
			}
			reader.close(); 
			String content = buffer.toString();
			Record r = new Record();
			if (content != null && content.length() > 0) {
				JSONObject j = JSONObject.fromObject(content);
				r.set("adsenseOpen", j.getBoolean("adsenseOpen"));
				r.set("groupid", j.getLong("groupid"));
				r.set("address", j.get("address"));
				// r.set("context", JSONObject.fromObject(content));
			}
			List<Record> newReturnList = new ArrayList<Record>();
			newReturnList.add(r);
			jsonString = jsonData.getSuccessJson(newReturnList);
		} catch (Exception e) {
			e.printStackTrace();
			jsonString = jsonData.getJson(-50,"读取文件失败");
		} finally {
			reader.close();
			renderText(jsonString);
		}

	}
	
	/**
	 * 设置管理员
	 */
	public void setAdmin() {
		List<Record> list = Db.find("select groupid,gcreator from groups");
		for (Record record : list) {
			Long groupid = record.getLong("groupid");
			Long gcreator = record.getLong("gcreator");
			List<Record> list2 = Db.find("select gmid from groupmembers where gmuserid="+gcreator+" and gmgroupid="+groupid + " group by gmid");
			if(list2.size()!=0) {
				Long gmid = list2.get(0).getLong("gmid");
				GroupMember member = new GroupMember().findById(gmid);
				member.set("IsAdmin", 1);
				member.update();
			}
		}
		renderText("=====ok");
	}
	public void getAllMoney(){
		int money=CanUserdMoney.getInstance().getMoney();
		Record r=new Record();
		r.set("money", money);
		List<Record> list=new ArrayList<Record>();
		list.add(r);
		jsonString = jsonData.getSuccessJson(list);
		renderText(jsonString);
	}
	public void setAllMoney(){
		String userdMoney=this.getPara("userdMoney");
		if(null!=userdMoney&&userdMoney!=""){
			int money=CanUserdMoney.getInstance().getMoney()-Integer.parseInt(userdMoney);
			CanUserdMoney.getInstance().setMoney(money);
		}
		renderText(CanUserdMoney.getInstance().getMoney()+"");
//		int money=CanUserdMoney.getInstance().getMoney();
//		Record r=new Record();
//		r.set("money", money);
//		List<Record> list=new ArrayList<Record>();
//		list.add(r);
//		jsonString = jsonData.getSuccessJson(list);
//		renderText(jsonString);
	}
	
	//1000w推送

	 public void activitiPush() {//and formid.userID=816596
//		 publishMessage("oIrcI0c0AncUqFWGRm2ciK766f4I", "b14a58cc8aeb9db5cde60903f831cf9a");
//		 Db.update("delete from formid where userID=816596 and formID='b14a58cc8aeb9db5cde60903f831cf9a'");
	 Db.update("delete from formid where time <DATE_ADD(Now(),INTERVAL -7 day)");
	  System.out.println("select formid.userID userID,formID,uopenid from formid,users where 1 "
	    + "and formid.userID=users.userid and status=0 "
	    + " and time between DATE_ADD(Now(),INTERVAL -7 day) and Now() "
	    + "group by formid.userID order by time asc");
	  List<Record> formidList = Db.find("select formid.userID userID,formID,uopenid from formid,users where 1 "
	    + "and formid.userID=users.userid and status=0 "
	    + " and time between DATE_ADD(Now(),INTERVAL -7 day) and Now() "
	    + "group by formid.userID order by time asc");
	  int i=0;
	  for (Record fr : formidList) {
	   publishMessage(fr.get("uopenid"),fr.get("formID"));
	   Db.update("delete from formid where userID="+fr.get("userID")+" and formID='"+fr.get("formID")+"'");
	   i++;
	   System.out.println("已发："+i);
	  }
	  System.out.println("推送完毕");
	 } 
	 
	 public void publishMessage(String openID,String formID){
	     //String openID=this.getPara("openid");
	     //String formID=this.getPara("formid");
	     // 获取access_token
	       String accessToken = SmallAppQRCode.GetSmallAppAccessToken();
	       // 模板ID
	       String templateID = "tFi5jU173_giRHylhDTHvk9e1Y8aTj3-selufvtPj5Q";
	       // 获取页面路径
	       String path = "pages/viewscoll/viewscoll?port=推送&groupid=5577346";
	       // 参数
	       String time = sdf.format(new Date());
	       
	       Map<String, Map<String, String>> map = new HashMap<String, Map<String, String>>();
	       Map<String, String> tempMap = new HashMap<String, String>();
	       tempMap.put("value", "晒出游，秀出你最美的旅行照");
	       tempMap.put("color", "#ffa200");
	       map.put("keyword1", tempMap);
	       tempMap = new HashMap<String, String>();
	       tempMap.put("value", "3月13日-3月19日");
	       tempMap.put("color", "#ffa200");
	       map.put("keyword2", tempMap);
	       tempMap = new HashMap<String, String>();
	       tempMap.put("value", "参与就有奖，新一轮晒照片活动火爆开启");
	       tempMap.put("color", "#ffa200");
	       map.put("keyword3", tempMap);
	       
	       // 构造请求URL和参数
	       String url = "https://api.weixin.qq.com/cgi-bin/message/wxopen/template/send?access_token=" + accessToken + "";
	       com.alibaba.fastjson.JSONObject param = new com.alibaba.fastjson.JSONObject();
	       param.put("touser", openID);
			param.put("template_id", templateID);
			param.put("page", path);
			param.put("form_id", formID);
			param.put("data", map);
			// 发送请求
			System.out.println(dataProcess.sendPost(url, param.toJSONString(), "json"));
	 }
	 /*
	 public void publishMessage(String openID,String formID){
	     //String openID=this.getPara("openid");
	     //String formID=this.getPara("formid");
	     // 获取access_token
	       String accessToken = SmallAppQRCode.GetSmallAppAccessToken();
	       // 模板ID
	       String templateID = "tFi5jU173_giRHylhDTHvhko1ctl9P88Z-NQAfW4CHM";
	       // 获取页面路径
	       String path = "pages/viewscoll/viewscoll?port=推送&groupid=5268248";
	       // 参数
	       String time = sdf.format(new Date());
	       
	       Map<String, Map<String, String>> map = new HashMap<String, Map<String, String>>();
	       Map<String, String> tempMap = new HashMap<String, String>();
	       tempMap.put("value", "照片换钱活动");
	       tempMap.put("color", "#ffa200");
	       map.put("keyword1", tempMap);
	       tempMap = new HashMap<String, String>();
	       tempMap.put("value", "2月12日-2月22日");
	       tempMap.put("color", "#ffa200");
	       map.put("keyword2", tempMap);
	       tempMap = new HashMap<String, String>();
	       tempMap.put("value", "活动期间在忆年小程序上传照片即可获得红包,先到先得,最少1元,最高10000元.");
	       tempMap.put("color", "#ffa200");
	       map.put("keyword3", tempMap);
	       
	       // 构造请求URL和参数
	       String url = "https://api.weixin.qq.com/cgi-bin/message/wxopen/template/send?access_token=" + accessToken + "";
	       com.alibaba.fastjson.JSONObject param = new com.alibaba.fastjson.JSONObject();
	       param.put("touser", openID);
			param.put("template_id", templateID);
			param.put("page", path);
			param.put("form_id", formID);
			param.put("data", map);
			// 发送请求
			dataProcess.sendPost(url, param.toJSONString(), "json");
	 }
	 */
	 public void publishTest(){
		/* System.out.println("照片达人开始："+System.currentTimeMillis());
			if(this.getPara("groupid")==null||this.getPara("groupid").equals("")||this.getPara("uid")==null||this.getPara("uid").equals("")){
				jsonString=jsonData.getJson(2, "参数错误",new ArrayList<Record>());
				renderText(jsonString);
				return;
			}
			int groupid = Integer.parseInt(this.getPara("groupid"));
			int uid = Integer.parseInt(this.getPara("uid"));
			if(groupid==CommonParam.pGroupId||groupid==CommonParam.pGroupId2){
				List<Record> photo = new SpaceService().GetGroupAllUserPublishList_2(uid, groupid, 100) ;				
				String returnValue = jsonData.getJson(0, "success",photo
						);
				renderText(returnValue);
				System.out.println("照片达人结束："+System.currentTimeMillis());
				return;
			}		
			int searchLimit = Integer
					.parseInt(this.getPara("searchLimit") != null && !this.getPara("searchLimit").equals("")
							? this.getPara("searchLimit")
							: "100");
			String returnValue = jsonData.getJson(0, "success",
					new SpaceService().GetPublishList(uid, groupid, searchLimit));*/
		 String returnValue = jsonData.getJson(0, "success",
					new SpaceService().GetGroupAllUserPublishList_3(439193,5386332,100));
			
			
	 }
	 public void testMini(){
		 postToMiniapp("aaabbcc","t1");
	 }
	 public void postToMiniapp(String zj,String openid){
			Long ts=System.currentTimeMillis();
			String url="https://api.uboxsale.com/Miniapp/SendCouponYi/umid/"+zj+"/thirdid/"+openid;		
//			String param1 = getMD5("api=User.Tissue.Order.TicketOrdertimestamp="+ts+"tissue_ticket="+zj+"bjSjRNJmXvBE16faKxo3caLcFjGSZ5bV");
//			url+=param1;
//			param1="api=User.Tissue.Order.TicketOrder&timestamp="+ts+"&tissue_ticket="+zj;
			HttpUtils u=new HttpUtils();
			
			savePartnerRecord(0+"", zj+"   openid", u.sendPost(url, ""), "Miniapp");
			//{"status":-1,"status_str":"已发放奖励","api_data":[]}
		}
	 public void savePartnerRecord(String userid,String postValue,String getValue,String partner){
			PartnerRecord pr=new PartnerRecord();
			pr.set("userid", userid).set("postValue", postValue).set("getValue", getValue).set("partner", partner);
			pr.save();
		}
	 public void getPicTest(){
		 String type = this.getPara("type");
			
			if(type==null||type.equals("")){
				jsonString=jsonData.getJson(2, "参数错误",new ArrayList<Record>());
				renderText(jsonString);
				return;
			}
			Record r=new Record();
			List<Record> list=new ArrayList<Record>();
			if(type.equals("1")){
				r.set("pic", "http://7xlmtr.com1.z0.glb.clouddn.com/20180311-14-5.jpg");
			}
			if(type.equals("2")){
				r.set("pic", "http://7xlmtr.com1.z0.glb.clouddn.com/xiangcefengmian1.png");
			}
			if(type.equals("3")){
				r.set("pic", "http://7xlmtr.com1.z0.glb.clouddn.com/20180104-1-1.jpg");
			}
			list.add(r);
			jsonString = jsonData.getSuccessJson(list);
			renderText(jsonString);
			System.out.println("ShowMoments结束："+System.currentTimeMillis());
	 }
}
