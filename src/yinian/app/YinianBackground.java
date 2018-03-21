package yinian.app;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.jfinal.aop.Before;
import com.jfinal.core.Controller;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;
import com.jfinal.plugin.activerecord.tx.Tx;
import com.jfinal.plugin.ehcache.CacheKit;
import com.jfinal.plugin.ehcache.CacheName;
import com.jfinal.plugin.ehcache.EvictInterceptor;
import com.jfinal.plugin.ehcache.IDataLoader;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.UploadManager;

import yinian.common.CommonParam;
import yinian.model.Event;
import yinian.model.Goods;
import yinian.model.Group;
import yinian.model.QiniuModel;
import yinian.model.TodayMemory;
import yinian.model.User;
import yinian.model.UserPhoto;
import yinian.service.YinianService;
import yinian.utils.JsonData;
import yinian.utils.QiniuOperate;

public class YinianBackground extends Controller {

	private String jsonString; // ���ص�json�ַ���
	private JsonData jsonData = new JsonData(); // json������
	private YinianService service = new YinianService();// ҵ������
	private YinianDataProcess dataProcess = new YinianDataProcess();// ���ݴ�����
	private QiniuOperate operate = new QiniuOperate(); // ��ţ������

	public static int albumNum = 680;
	public static int picNum = 11440;
	public static int userNum = 1658;

	public static String startDate = "20170327";
	public static String endDate = "20170406";

	/**
	 * ��ʱ�ӿ�
	 */
	public void Temp() {
		String groupid = this.getPara("groupid");
		Record judge = Db.findFirst("select `status` from ynTemp where id=16");
		int num = Integer.parseInt(judge.get("status").toString());
		String result = "<html><body>";
		if (num > 20) {
			result = "�鿴�������ù�</body></html>";
		} else {
			int left = 20 - num - 1;
			result += "<h1>���ܲ鿴" + left + "��</h1>";
			Db.update("update ynTemp set `status`=`status`+1 where id=16  ");
			List<Record> list = Db.find("select poriginal from groups,`events`,pictures where groupid=" + groupid
					+ " and groupid=egroupid and eid=peid limit 1000 ");
			for (Record record : list) {
				String url = operate.getDownloadToken(record.get("poriginal").toString() + "?imageView2/2/w/300");
				result += "<img src=\"" + url + "\" />";
			}
			result += "</body></html>";
		}
		renderHtml(result);
	}

	/**
	 * ��ѯ��ҵ��������
	 */
	public void SeachIndustryData() {
		String startTime = this.getPara("startTime");
		String endTime = this.getPara("endTime");
		List<Record> user = Db.find("select distinct industryUserID from industry where industryTime>='" + startTime
				+ "' and industryTime<='" + endTime + "' ");
		List<Record> num = Db.find(
				"select * from industry where industryTime>='" + startTime + "' and industryTime<='" + endTime + "' ");
		List<Record> rank = Db.find("select industryContent,count(*) as num from industry where industryTime>='"
				+ startTime + "' and industryTime<='" + endTime + "' GROUP BY industryContent order by count(*) desc");
		String result = "<html><body><p>" + startTime + "��" + endTime + "</p><p>ʹ������" + user.size() + "&nbsp&nbspʹ�ô���"
				+ num.size() + "</p><p>��ǩ����</p>";
		for (Record record : rank) {
			result += "<p>" + record.getStr("industryContent") + "&nbsp&nbsp&nbsp" + record.get("num").toString()
					+ "</p>";
		}
		result += "</body></html>";
		renderHtml(result);
	}

	/**
	 * Ĭ�Ϻ��Խ��ռ��ϴ�����
	 * 
	 * @throws ParseException
	 */
	public void DefaultAndSelfSpaceUploadNum() throws ParseException {
		String before = "2017-04-25";
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Date date = sdf.parse(before);

		int[] def = new int[11];
		int[] self = new int[11];
		int[] inUse = new int[11];
		for (int i = 0; i <= 10; i++) {
			Calendar calendar = new GregorianCalendar();
			calendar.setTime(date);
			calendar.add(calendar.DATE, i);
			String temp = sdf.format(calendar.getTime());
			List<Record> list = Db.find(
					"SELECT groupid, count(*) as num FROM users,groups,`events`,pictures WHERE userid = gcreator AND groupid = egroupid AND eid = peid AND usource = 'С����' AND DATE(gtime) = '"
							+ temp
							+ "' and DATE(gtime)=DATE(euploadtime) and gtype in (6,7,8) and gstatus=0  GROUP BY groupid ");
			def[i] = list.size();
		}
		for (int i = 0; i <= 10; i++) {
			Calendar calendar = new GregorianCalendar();
			calendar.setTime(date);
			calendar.add(calendar.DATE, i);
			String temp = sdf.format(calendar.getTime());
			List<Record> list = Db.find(
					"SELECT groupid, count(*) as num FROM users,groups,`events`,pictures WHERE userid = gcreator AND groupid = egroupid AND eid = peid AND usource = 'С����' AND DATE(gtime) = '"
							+ temp
							+ "' and DATE(gtime)=DATE(euploadtime) and gtype not in (6,7,8) and gstatus=0  GROUP BY groupid ");
			self[i] = list.size();
		}
		for (int i = 0; i <= 10; i++) {
			Calendar calendar = new GregorianCalendar();
			calendar.setTime(date);
			calendar.add(calendar.DATE, i);
			String temp = sdf.format(calendar.getTime());
			List<Record> list = Db.find(
					"SELECT DATE(gtime),count(*) FROM groups,users,`events`,pictures WHERE usource = 'С����' AND userid = gcreator AND groupid = egroupid AND eid = peid AND gnum = 1 AND DATE(gtime) = '"
							+ temp + "' GROUP BY groupid  ");
			inUse[i] = list.size();
		}
		String result = "<html><body><table><tr><td>����</td><td>Ĭ��</td><td>�Խ�</td><td>��Ч�����</td></tr>";
		for (int i = 0; i <= 10; i++) {
			Calendar calendar = new GregorianCalendar();
			calendar.setTime(date);
			calendar.add(calendar.DATE, i);
			String temp = sdf.format(calendar.getTime());
			result += "<tr><td>" + temp + "</td><td>" + def[i] + "</td><td>" + self[i] + "</td><td>" + inUse[i]
					+ "</td></tr>";
		}
		result += "</table></body></html>";
		renderHtml(result);
	}

	/**
	 * ÿ���¿ռ���Ƭ������
	 * 
	 * @throws ParseException
	 * 
	 */
	public void DailyNewSpacePhotoNum() throws ParseException {
		String before = "2017-04-25";
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Date date = sdf.parse(before);

		int[] num1 = new int[11];
		int[] num2_9 = new int[11];
		int[] num10_50 = new int[11];
		int[] num51 = new int[11];
		for (int i = 0; i <= 10; i++) {
			Calendar calendar = new GregorianCalendar();
			calendar.setTime(date);
			calendar.add(calendar.DATE, i);
			String temp = sdf.format(calendar.getTime());
			List<Record> list = Db.find(
					"SELECT groupid, count(*) as num FROM users,groups,`events`,pictures WHERE userid = gcreator AND groupid = egroupid AND eid = peid AND usource = 'С����' AND DATE(gtime) = '"
							+ temp
							+ "' and DATE(gtime)=DATE(euploadtime) and gstatus=0  GROUP BY groupid HAVING count(*)=1");
			num1[i] = list.size();
		}
		for (int i = 0; i <= 10; i++) {
			Calendar calendar = new GregorianCalendar();
			calendar.setTime(date);
			calendar.add(calendar.DATE, i);
			String temp = sdf.format(calendar.getTime());
			List<Record> list = Db.find(
					"SELECT groupid, count(*) as num FROM users,groups,`events`,pictures WHERE userid = gcreator AND groupid = egroupid AND eid = peid AND usource = 'С����' AND DATE(gtime) = '"
							+ temp
							+ "' and DATE(gtime)=DATE(euploadtime) and gstatus=0  GROUP BY groupid HAVING count(*)>=2 and count(*)<=9 ");
			num2_9[i] = list.size();
		}
		for (int i = 0; i <= 10; i++) {
			Calendar calendar = new GregorianCalendar();
			calendar.setTime(date);
			calendar.add(calendar.DATE, i);
			String temp = sdf.format(calendar.getTime());
			List<Record> list = Db.find(
					"SELECT groupid, count(*) as num FROM users,groups,`events`,pictures WHERE userid = gcreator AND groupid = egroupid AND eid = peid AND usource = 'С����' AND DATE(gtime) = '"
							+ temp
							+ "' and DATE(gtime)=DATE(euploadtime) and gstatus=0  GROUP BY groupid HAVING count(*)>=10 and count(*)<=50");
			num10_50[i] = list.size();
		}
		for (int i = 0; i <= 10; i++) {
			Calendar calendar = new GregorianCalendar();
			calendar.setTime(date);
			calendar.add(calendar.DATE, i);
			String temp = sdf.format(calendar.getTime());
			List<Record> list = Db.find(
					"SELECT groupid, count(*) as num FROM users,groups,`events`,pictures WHERE userid = gcreator AND groupid = egroupid AND eid = peid AND usource = 'С����' AND DATE(gtime) = '"
							+ temp
							+ "' and DATE(gtime)=DATE(euploadtime) and gstatus=0  GROUP BY groupid HAVING count(*)>=51");
			num51[i] = list.size();
		}
		String result = "<html><body><table><tr><td>����</td><td>1��</td><td>2-9��</td><td>10-50��</td><td>51������</td></tr>";
		for (int i = 0; i <= 10; i++) {
			Calendar calendar = new GregorianCalendar();
			calendar.setTime(date);
			calendar.add(calendar.DATE, i);
			String temp = sdf.format(calendar.getTime());
			result += "<tr><td>" + temp + "</td><td>" + num1[i] + "</td><td>" + num2_9[i] + "</td><td>" + num10_50[i]
					+ "</td><td>" + num51[i] + "</td></tr>";
		}
		result += "</table></body></html>";
		renderHtml(result);

	}

	/**
	 * ��ѯ�ռ��ƴͼ��Ϣ
	 */
	public void SearchSpaceOrPuzzle() {
		String type = this.getPara("type");
		String content = this.getPara("content");
		List<Record> list = new ArrayList<Record>();
		switch (type) {
		case "space":
			list = Db.find(
					"select groupid as id,gname as name,count(*) as num from groups,`events`,pictures where groupid=egroupid and eid=peid and gname like '%"
							+ content + "%' group by groupid ");
			break;
		case "puzzle":
			list = Db.find("select puzzleID as id,puzzleContent as name from puzzle where puzzleContent like '%"
					+ content + "%' ");
			break;
		}
		String result = "<html><body>";
		for (Record record : list) {
			String id = record.get("id").toString();
			String name = record.get("name").toString();
			String num = record.get("num").toString();
			result += ("<p>" + id + "&nbsp&nbsp&nbsp" + name + "&nbsp&nbsp&nbsp" + num + "</p>");
		}
		result += "</body></html>";
		renderHtml(result);

	}

	/**
	 * ��ѯ�ռ������û���
	 */
	public void SearchSpaceNewUserNum() {
		String groupid = this.getPara("groupid");

		int total = 0;
		List<Record> list = Db.find(
				"select DATE(utime) as date,count(*) as num from groupmembers,users where userid=gmuserid and gmgroupid="
						+ groupid + " and TIMEDIFF(gmtime,utime)<60 GROUP BY DATE(utime) desc");
		String result = "<html><body><table border=\"1\">";
		for (Record record : list) {
			String date = record.get("date").toString();
			int num = Integer.parseInt(record.get("num").toString());
			total += num;
			result += ("<tr><td>" + date + "</td><td>" + num + "</td></tr>");
		}
		result += "</table>�����û�����" + total + "��</n></body></html>";
		renderHtml(result);
	}

	/**
	 * ��ʾ�����û���ռ����Ƭ
	 */
	public void ShowPhotoOfSingleUserOrSpace() {
		String id = this.getPara("id");
		String type = this.getPara("type");
		List<Record> list = new ArrayList<Record>();
		if (type.equals("user")) {
			list = Db.find("select poriginal from `events`,pictures where eid=peid and euserid=" + id + "");
		} else {
			list = Db.find("select poriginal from groups,`events`,pictures where groupid=" + id
					+ " and groupid=egroupid and eid=peid ");
		}

		String result = "<html><body>";
		for (Record record : list) {
			String url = operate.getDownloadToken(record.get("poriginal").toString() + "?imageView2/2/w/300");
			result += "<img src=\"" + url + "\" />";
		}
		result += "</body></html>";
		renderHtml(result);
	}

	/**
	 * ����ͼƬ
	 * 
	 * @throws MalformedURLException
	 * 
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public void DownloadPicture() throws MalformedURLException {
		List<Record> list = Db.find(
				"select userid,unickname,COUNT(*) from users,`events`,pictures where userid=euserid and eid=peid and uOrigin=0 and userid not in (67504,43,8746,364,97,1444,2,5298,52,4488,396) GROUP BY euserid order by count(*) desc limit 1000");
		int index = 895;
		int i = 8810;

		for (int j = 895; j < 1000; j++) {
			String userid = list.get(j).get("userid").toString();

			System.out.println("������������������ʼ��ȡ��" + index + "���û����ݣ��û�idΪ��" + userid + "����������������");

			Set<Integer> key = new HashSet<Integer>();
			List<Record> photo = Db.find(
					"select euserid,pid,poriginal from `events`,pictures where euserid=" + userid + " and eid=peid");
			int size = photo.size();
			for (int k = 0; k < 10; k++) {
				int num;
				while (true) {
					num = (int) (Math.random() * (size - 1));
					if (!key.contains(num)) {
						break;
					}
				}

				key.add(num);
				String address = operate.getDownloadToken(photo.get(num).get("poriginal").toString());
				// ����ͼƬ
				URL url = new URL(address);
				try {
					DataInputStream dataInputStream = new DataInputStream(url.openStream());
					String imageName = "F:\\ͼƬ\\" + String.format("%08d", i) + ".jpg";
					FileOutputStream fileOutputStream = new FileOutputStream(new File(imageName));
					byte[] buffer = new byte[1024];
					int length;

					while ((length = dataInputStream.read(buffer)) > 0) {
						fileOutputStream.write(buffer, 0, length);
					}

					dataInputStream.close();
					fileOutputStream.close();
					System.out.println("������������������" + i + "��ͼƬ���سɹ�����������������");
					i++;
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					System.out.println("������������������" + i + "��ͼƬ����ʧ�ܣ��ļ������ڡ���������������");
					continue;
				}

			}

			System.out.println("������������������" + index + "���û����ݻ�ȡ��ϣ��û�idΪ��" + userid + "����������������");
			index++;
		}
	}

	/**
	 * ���ص�����Ƭ
	 * 
	 * @throws MalformedURLException
	 */
	public void DownLoadSinglePicture() throws MalformedURLException {

		int userid = Integer.parseInt(this.getPara("userid"));
		int index = Integer.parseInt(this.getPara("index"));
		int i = Integer.parseInt(this.getPara("i"));

		System.out.println("������������������ʼ��ȡ�û�idΪ��" + userid + "����Ƭ����������������");

		Set<Integer> key = new HashSet<Integer>();
		List<Record> photo = Db
				.find("select euserid,pid,poriginal from `events`,pictures where euserid=" + userid + " and eid=peid");
		int size = photo.size();
		for (int k = 0; k < index; k++) {
			int num;
			while (true) {
				num = (int) (Math.random() * (size - 1));
				if (!key.contains(num)) {
					break;
				}
			}

			key.add(num);
			String address = operate.getDownloadToken(photo.get(num).get("poriginal").toString());
			// ����ͼƬ
			URL url = new URL(address);
			try {
				DataInputStream dataInputStream = new DataInputStream(url.openStream());
				String imageName = "F:\\ͼƬ\\" + String.format("%08d", i) + ".jpg";
				FileOutputStream fileOutputStream = new FileOutputStream(new File(imageName));
				byte[] buffer = new byte[1024];
				int length;

				while ((length = dataInputStream.read(buffer)) > 0) {
					fileOutputStream.write(buffer, 0, length);
				}

				dataInputStream.close();
				fileOutputStream.close();
				System.out.println("������������������" + i + "��ͼƬ���سɹ�����������������");
				i++;
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				System.out.println("������������������" + i + "��ͼƬ����ʧ�ܣ��ļ������ڡ���������������");
				k--;
				continue;
			}

		}

		System.out.println("����������������������ȡ�û�idΪ��" + userid + "����Ƭ����������������");

	}

	/**
	 * ���¹����ռ������
	 */
	public void UpdateOpenSpaceLikes() {
		int i = 0;
		List<Record> list = Db.find(
				"select groupid,gname from groups where gtype=5 and gstatus=0 and groupid<20000 or groupid =104295");
		for (Record record : list) {
			String groupid = record.get("groupid").toString();
			List<Record> eventList = Db
					.find("select eid from `events` where egroupid=" + groupid + " and estatus in (0,3)");
			for (Record temp : eventList) {
				String eid = temp.get("eid").toString();
				Event event = new Event().findById(eid);
				int random = (int) (Math.random() * (200 - 80 + 1) + 80);
				event.set("elike", random);
				if (event.update()) {
					System.out.println(i + "   :" + random);
					i++;
				} else {
					System.out.println("ʧ��");
				}
			}
		}
		System.out.println("end");
	}

	/**
	 * ���������
	 * 
	 * @throws ParseException
	 */
	public void insertTodayMemory() throws ParseException {

		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		Date start = sdf.parse(startDate);
		Date end = sdf.parse(endDate);

		while (start.before(end)) {
			String address = CommonParam.qiniuOpenAddress + "jinriyi" + sdf.format(start) + ".jpg";
			SimpleDateFormat temp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			String time = temp.format(start);
			TodayMemory today = new TodayMemory();
			today.set("TMpic", address).set("TMtime", time);
			if (today.save()) {
				System.out.println(start.toString() + "����");
				Calendar calendar = new GregorianCalendar();
				calendar.setTime(start);
				calendar.add(calendar.DATE, 1);// ��������������һ��.����������,������ǰ�ƶ�
				start = calendar.getTime(); // ���ʱ���������������һ��Ľ��
			}

		}

		System.out.println("������������");
	}

	/**
	 * �������
	 */
	public void CacheTest() {
		long startTime = System.nanoTime(); // ��ȡ��ʼʱ��

		List<Record> userList = Db.findByCache("sampleCache1", "user", "select * from users");
		System.out.println(userList.size());
		List<Record> bannerList = Db.findByCache("sampleCache1", "banner", "select * from banner");
		System.out.println(bannerList.size());
		Record record = userList.get(1);
		System.out.println(record.getStr("unickname"));

		long endTime = System.nanoTime(); // ��ȡ����ʱ��
		System.out.println("��������ʱ�䣺 " + (endTime - startTime) + "ns");
	}

	/**
	 * CacheKit����
	 */
	public void cacheKit() {
		List<Record> userList = CacheKit.get("sampleCache1", "user", new IDataLoader() {

			@Override
			public Object load() {
				// TODO Auto-generated method stub
				return Db.find("select * from banner");
			}
		});
		System.out.println(userList.size());
		List<Record> bannerList = CacheKit.get("sampleCache1", "banner", new IDataLoader() {

			@Override
			public Object load() {
				// TODO Auto-generated method stub
				return Db.find("select * from banner");
			}
		});
		System.out.println(bannerList.size());

		// List<Record> test = Db.find("select * from banner");
		// CacheKit.put("sampleCache1", "user", test);
	}

	/**
	 * �����Ʒ
	 */
	public void AddGoods() {
		String type = this.getPara("type");
		String num = this.getPara("num");
		String start = "2016-07-" + num + " 19:00:00";
		int a = 1;
		String end = "2016-07-" + a + " 00:00:00";
		boolean flag = false;
		Goods good = new Goods();
		if (type.equals("1")) {
			good.set("goodsName", "��������֤����").set("goodsPrice", 4.9).set("goodsNum", 0).set("goodsLimit", 100)
					.set("goodsPicture", "http://7xlmtr.com1.z0.glb.clouddn.com/zhengjianzhao.jpg")
					.set("goodsBeginTime", start).set("goodsEndTime", end);
		}
		if (type.equals("2")) {
			good.set("goodsName", "�������������").set("goodsPrice", 9.9).set("goodsNum", 0).set("goodsLimit", 100)
					.set("goodsPicture", "http://7xlmtr.com1.z0.glb.clouddn.com/yishuzhao.jpg")
					.set("goodsBeginTime", start).set("goodsEndTime", end);
		}
		flag = good.save();
		jsonString = dataProcess.insertFlagResult(flag);
		renderText(jsonString);
	}

	/**
	 * ���������û�����ʹ�ÿռ�
	 */
	public void updateAllUserStorage() {
		// ���������û������ÿռ�
		List<Record> list = Db.find("select userid from users");
		for (Record record : list) {
			String userid = record.get("userid").toString();
			Record a = Db.findFirst(
					"select count(*) as number from events,pictures where eid=peid and euserid=" + userid + "");
			int n = Integer.parseInt(a.get("number").toString());
			Double num = 0.0;
			if (1 <= n && n <= 10) {
				num = n * 1.5 * 1024;
			}
			if (11 <= n && n <= 100) {
				num = n * 1.0 * 1024;
			}
			if (n > 100) {
				num = n * 0.5 * 1024;
			}
			User user = new User().findById(userid);
			user.set("uusespace", num);
			if (user.update()) {
				System.out.println(userid + "���û��洢�ռ���³ɹ���");
			}
		}
		System.out.println("�����û��洢�ռ���³ɹ�");
	}

	/**
	 * ��ţ�ϴ����ԣ��ϴ��������ռ�
	 * 
	 * @throws QiniuException
	 */
	public void QiniuUploadTestWithOpenSpace() throws QiniuException {

		QiniuOperate operate = new QiniuOperate();
		String token = operate.getUploadToken();
		UploadManager upload = new UploadManager();
		Response res = upload.put("C:\\Users\\lenovo\\Pictures\\test1.jpg", "test12.jpg", token);
		System.out.println("�ϴ��ɹ�");
		QiniuModel qiniu = res.jsonToObject(QiniuModel.class);
		String result = "��С�� " + qiniu.fsize + " ��ϣ�룺 " + qiniu.hash + " ��ַ��  " + qiniu.key + " ���ȣ�  " + qiniu.height
				+ " ��ȣ�  " + qiniu.width;
		System.out.println(result);
	}

	/**
	 * Do Something In User
	 */
	@Before(Tx.class)
	public void DoSomethingInUser() {
		// Set User Number
		String[] userArray = new String[userNum];

		// Random Object
		Random random = new Random();

		// Birthday
		SimpleDateFormat birthDf = new SimpleDateFormat("yyyy-MM-dd");
		String birth = birthDf.format(new Date());

		// Default Background
		String defaultBackground = CommonParam.qiniuOpenAddress + CommonParam.userDefaultBackground;

		// Default Picture
		String defaultPic = CommonParam.qiniuOpenAddress + CommonParam.userDefaultHeadPic;

		for (int i = 0; i < userArray.length; i++) {
			// Sex
			int sex = random.nextInt(1);

			// Name
			String base = "ABCDEFGHIJKLMNOPQRSTUVXYZabcdefghijklmnopqrstuvxyz0123456789";
			StringBuffer sb = new StringBuffer();
			for (int j = 0; j < 8; j++) {
				int number = random.nextInt(base.length());
				sb.append(base.charAt(number));
			}
			String nickname = sb.toString();

			// WeChat OpenID
			StringBuffer sb2 = new StringBuffer();
			for (int k = 0; k < 26; k++) {
				int number = random.nextInt(base.length());
				sb2.append(base.charAt(number));
			}
			String wechatID = ("o7C7k" + sb2.toString());

			// Register
			User user = new User().set("uwechatid", wechatID).set("usex", sex).set("unickname", nickname)
					.set("ubirth", birth).set("upic", defaultPic).set("ubackground", defaultBackground)
					.set("uOrigin", 1);
			if (user.save()) {
				String userid = user.get("userid").toString();
				userArray[i] = userid;
				System.out.println("��" + i + "���û�ע��ɹ�");
			}
		}
		System.out.println(userArray.length);
	}

	/**
	 * Do Something In Album
	 */
	public void DoSomethingInAlbum() {

		// Get Date
		SimpleDateFormat birthDf = new SimpleDateFormat("yyyy-MM-dd");
		String date = birthDf.format(new Date());

		// Get Today New Register User
		List<Record> userList = Db.find("select userid from users where ubirth like '" + date + "' and uOrigin=1 ");

		// Random Object
		Random random = new Random();

		// Create Albums
		for (int i = 0; i < albumNum; i++) {
			// Get Userid
			int index = random.nextInt(userList.size() - 1);
			String userid = userList.get(index).get("userid").toString();

			// Get Album Type
			int gtype = random.nextInt(4);
			boolean flag = service.creatSomeAlbum(userid, String.valueOf(gtype));
			if (flag) {
				System.out.println("��" + (i + 1) + "����ᴴ���ɹ�");
			}
		}
		System.out.println("��ᴴ������");
	}

	/**
	 * Do Something In Picture
	 */
	@Before(Tx.class)
	public void DoSomethingInPicture() {

		// Get Date
		SimpleDateFormat birthDf = new SimpleDateFormat("yyyy-MM-dd");
		String date = birthDf.format(new Date());

		// Get Today New Register User
		List<Record> AlbumList = Db
				.find("select groupid,gcreator from groups where gtime like '" + date + "%' and gOrigin=1 ");

		// Random Object
		Random random = new Random();

		// Define Increse Picture Num
		int totalPic = 0;

		while (totalPic < picNum) {

			// Get Publisher Info
			int index = random.nextInt(AlbumList.size() - 1);
			String userid = AlbumList.get(index).get("gcreator").toString();
			String groupid = AlbumList.get(index).get("groupid").toString();

			// Random Picture Num And Must Bigger Than 0
			int uploadPicNum = (random.nextInt(19) + 1);

			// Combine All Picture Address To String
			String picAddress = "";
			for (int i = 0; i < uploadPicNum; i++) {
				picAddress += ((CommonParam.qiniuOpenAddress + CommonParam.defaultFirstPicOfGroup) + ",");
			}
			picAddress = picAddress.substring(0, picAddress.length() - 1);

			// Get Storage
			int storage = 300 * uploadPicNum;
			String place = String.valueOf(storage);

			boolean flag = service.uploadSomeEvent(userid, groupid, picAddress, null, place);
			if (flag) {
				totalPic += uploadPicNum;
				System.out.println("��ǰ���ϴ���" + totalPic + "��ͼƬ");
			}

		}
		System.out.println("ͼƬ�ϴ�����,�ܼ��ϴ�" + totalPic + "��ͼƬ");
	}

	/**
	 * ��ȡÿ��С�����û���
	 */
	public void GetDailySmallAppUser() {
		String account = this.getPara("account");
		String password = this.getPara("password");

		if (account.equals("yinianAdmin") && password.equals("shijiashishabi")) {
			List<Record> list = Db.find(
					"select count(*) as num,date(utime) as date from users where usource='С����' and date(utime)>'2017-08-01' GROUP BY date(utime) desc");
			String result = "<html>";
			result += "<style> table{margin:  53px auto 0;width: 1000px; border: none;}td{border: none;text-align: center;margin: 0;padding: 0;height: 40px;}tr:nth-child(2n){background: rgba(237, 237, 237, 0.8);}tr:hover{background: rgba(200, 200, 200, 0.9);}.theader{background: #888;color: white;position: fixed;top: 0;width:1000px;height: 50px;}.theader td{width: 250px;}td{width: 250px;}</style>";
			result += "<table border=\"1\"><tr class=\"theader\"><td>����</td><td>�û���</td></tr>";

			for (int i = 0; i < list.size(); i++) {
				String date = list.get(i).get("date").toString();
				String num = list.get(i).get("num").toString();
				result += "<tr><td>" + date + "</td><td>" + num + "</td></tr>";

			}
			result += "</table></html>";
			renderHtml(result);
		} else {
			jsonString = jsonData.getJson(1, "��������");
			renderText(jsonString);
		}
	}

	/**
	 * ��ȡУ԰�������
	 */
	public void GetSchoolAlbumData() {
		String date = this.getPara("date");
		// ÿ�������û�
		List<Record> dailyUser = Db.find(
				"SELECT gname,count(*) as num from groups,groupmembers where groupid=gmgroupid and gtype in(10,11) and DATE(gmtime)='"
						+ date
						+ "' and gmuserid not in (3,101838,101839,101841,101840,101842,101843,101844,101845,24816,24827,24832,24835,24837,24838,24839,24840,24841,24842,24844,29848,29850,29851,29852,29853,29854,29855,29856,29857,29858,101727,101728,101729,101730,101731,101732,101734,101735,101736,101737,101738,101739,101740,101741,101742,101743,101744,101745,101746,101747,101846,101847,101848,101849,101850,101851,101852,101853,101854,101855,104166,104167) GROUP BY groupid");
		// ÿ��UV
		List<Record> dailyUV = Db.find(
				"SELECT gname,count(*) as num from groups,albumUVandPV where groups.groupid=albumUVandPV.groupid and gtype in (10,11) and date='"
						+ date + "' GROUP BY groups.groupid");
		// ÿ��PV
		List<Record> dailyPV = Db.find(
				"SELECT gname,sum(num) as num from groups,albumUVandPV where groups.groupid=albumUVandPV.groupid and gtype in (10,11) and date='"
						+ date + "' GROUP BY groups.groupid");
		// ÿ���û���̬��
		List<Record> dailyUserEvent = Db.find(
				"select gname,COUNT(*) as num from groups,`events` where groupid=egroupid and gtype in (10,11) and DATE(euploadtime)='"
						+ date
						+ "' and euserid!=gcreator and euserid not in (3,101838,101839,101841,101840,101842,101843,101844,101845,24816,24827,24832,24835,24837,24838,24839,24840,24841,24842,24844,29848,29850,29851,29852,29853,29854,29855,29856,29857,29858,101727,101728,101729,101730,101731,101732,101734,101735,101736,101737,101738,101739,101740,101741,101742,101743,101744,101745,101746,101747,101846,101847,101848,101849,101850,101851,101852,101853,101854,101855,104166,104167) GROUP BY groupid ");
		// ÿ���̼Ҷ�̬��
		List<Record> dailyShopEvent = Db.find(
				"select gname,COUNT(*) as num from groups,`events` where groupid=egroupid and gtype in (10,11) and DATE(euploadtime)='"
						+ date + "' and euserid=gcreator GROUP BY groupid");
		// ÿ��������
		List<Record> dailyComment = Db.find(
				"select gname,COUNT(*) as num from groups,`events`,comments where groupid=egroupid and eid=ceid and gtype in (10,11) and DATE(ctime)='"
						+ date
						+ "' and cuserid not in (3,101838,101839,101841,101840,101842,101843,101844,101845,24816,24827,24832,24835,24837,24838,24839,24840,24841,24842,24844,29848,29850,29851,29852,29853,29854,29855,29856,29857,29858,101727,101728,101729,101730,101731,101732,101734,101735,101736,101737,101738,101739,101740,101741,101742,101743,101744,101745,101746,101747,101846,101847,101848,101849,101850,101851,101852,101853,101854,101855,104166,104167) group by groupid");
		// ÿ�յ�����
		List<Record> dailyLike = Db.find(
				"select gname,COUNT(*) as num from groups,`events`,`like` where groupid=egroupid and eid=likeEventID and gtype in (10,11) and DATE(likeTime)='"
						+ date
						+ "' and likeUserID not in (3,101838,101839,101841,101840,101842,101843,101844,101845,24816,24827,24832,24835,24837,24838,24839,24840,24841,24842,24844,29848,29850,29851,29852,29853,29854,29855,29856,29857,29858,101727,101728,101729,101730,101731,101732,101734,101735,101736,101737,101738,101739,101740,101741,101742,101743,101744,101745,101746,101747,101846,101847,101848,101849,101850,101851,101852,101853,101854,101855,104166,104167) group by groupid");

		String result = "<html><body>" + date + "����</br></br>ÿ�������û�</br>";
		for (Record record : dailyUser) {
			result += (record.get("gname").toString() + ":" + record.get("num").toString() + "</br>");
		}
		result += "</br>ÿ��UV</br>";
		for (Record record : dailyUV) {
			result += (record.get("gname").toString() + ":" + record.get("num").toString() + "</br>");
		}
		result += "</br>ÿ��PV</br>";
		for (Record record : dailyPV) {
			result += (record.get("gname").toString() + ":" + record.get("num").toString() + "</br>");
		}
		result += "</br>ÿ���û���̬��</br>";
		for (Record record : dailyUserEvent) {
			result += (record.get("gname").toString() + ":" + record.get("num").toString() + "</br>");
		}
		result += "</br>ÿ���̼Ҷ�̬��</br>";
		for (Record record : dailyShopEvent) {
			result += (record.get("gname").toString() + ":" + record.get("num").toString() + "</br>");
		}
		result += "</br>ÿ��������</br>";
		for (Record record : dailyComment) {
			result += (record.get("gname").toString() + ":" + record.get("num").toString() + "</br>");
		}
		result += "</br>ÿ�յ�����</br>";
		for (Record record : dailyLike) {
			result += (record.get("gname").toString() + ":" + record.get("num").toString() + "</br>");
		}
		result += "</body></html>";
		renderHtml(result);

	}

	/**
	 * �����������
	 */
	public void CreateCitySpace() {
		String name = this.getPara("name");
		String url = this.getPara("url");
		String inviteCode = Group.CreateSpaceInviteCode();
		YinianService service = new YinianService();
		jsonString = service.createAlbum(name, "3", CommonParam.qiniuOpenAddress + url, "14", inviteCode, "С����");
		// ���ؽ��
		renderText(jsonString);
	}

}
