package yinian.controller;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Random;

import org.bouncycastle.util.encoders.Base64;

import yinian.app.YinianDataProcess;
import yinian.common.CommonParam;
import yinian.interceptor.CrossDomain;
import yinian.model.BackupEvent;
import yinian.model.BackupPhoto;
import yinian.model.Encourage;
import yinian.model.Group;
import yinian.model.GroupCanPublish;
import yinian.model.GroupMember;
import yinian.model.LoverTimeMachine;
import yinian.model.Order;
import yinian.model.SmallAppLog;
import yinian.model.TodayMemory;
import yinian.model.User;
import yinian.service.H5Service;
import yinian.service.IMService;
import yinian.service.SimplificationH5Service;
import yinian.service.UserService;
import yinian.service.YinianService;
import yinian.utils.AES;
import yinian.utils.DES;
import yinian.utils.JsonData;
import yinian.utils.QiniuOperate;

import com.alibaba.fastjson.JSONObject;
import com.jfinal.aop.Before;
import com.jfinal.core.Controller;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;
import com.jfinal.plugin.activerecord.tx.Tx;

@Before(CrossDomain.class)
public class H5Controller extends Controller {

	private String jsonString; // ���ص�json�ַ���
	private JsonData jsonData = new JsonData(); // json������
	private YinianService service = new YinianService(); // ҵ������
	private YinianDataProcess dataProcess = new YinianDataProcess();// ���ݴ�����
	private H5Service h5Service = new H5Service();
	private IMService im = new IMService();
	// enhance������ҵ���Ŀ�����AOP��ǿ
	YinianService TxService = enhance(YinianService.class);

	/**
	 * ��ȡϵͳʱ��
	 */
	public void GetSystemTime() {
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");// �������ڸ�ʽ
		String time = df.format(new Date());
		Record record = new Record().set("systemTime", time);
		List<Record> list = new ArrayList<Record>();
		list.add(record);
		jsonString = jsonData.getJson(0, "success", list);
		renderText(jsonString);
	}

	/**
	 * ��ȡ��ɱ��Ϣ
	 */
	public void ShowGoodsInfo() {
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");// �������ڸ�ʽ
		String time = df.format(new Date());
		// �жϵ�ǰ�Ƿ������ڽ��е���ɱ
		List<Record> nowList = Db.find(
				"select goodsID,goodsName,goodsPrice,goodsPicture,goodsBeginTime,goodsEndTime from goods where goodsBeginTime<='"
						+ time + "' and goodsEndTime>='" + time + "' ");
		if (nowList.size() == 0) {
			// û����ɱ�����ؼ������е���ɱ
			List<Record> nextList = Db.find(
					"select goodsID,goodsName,goodsPrice,goodsPicture,goodsBeginTime,goodsEndTime from goods where goodsBeginTime>'"
							+ time + "' limit 2 ");
			if (nextList.size() == 0) {
				// û�л��
				jsonString = jsonData.getJson(1000, "����ɱ��Ʒ");
			} else {
				// ������һ����ɱ��Ϣ
				jsonString = jsonData.getJson(0, "success", nextList);
			}
		} else {
			// ���ڽ�����ɱ�����ظ���ɱ��Ϣ
			jsonString = jsonData.getJson(0, "success", nowList);
		}
		renderText(jsonString);
	}

	/**
	 * ��ɱ��Ʒ
	 */
	@Before(Tx.class)
	public void GetGoods() {
		String code = this.getPara("code");
		String goodsID = this.getPara("goodsID");
		try {
			// ����
			String result = DES.decryptDES(code, "YZadZjYx");
			String[] array = result.split(",");
			// ��ȡ�û�id
			String userID = "";
			for (int i = 0; i < array.length; i++) {
				if (((array[i].split("="))[0]).equals("userid")) {
					userID = (array[i].split("="))[1];
				}
			}
			if (userID.equals("") || goodsID.equals("")) {
				jsonString = jsonData.getJson(2, "�����������");
			} else {
				// ��ȡҪ��ɱ����Ʒ�����Ϣ
				Record goodsRecord = Db
						.findFirst("select goodsNum,goodsLimit,goodsBeginTime,goodsEndTime from goods where goodsID="
								+ goodsID + " ");
				// �ж���Ʒ��ɱ��Ƿ�δ��ʼ���Ѿ�����
				String beginTime = goodsRecord.get("goodsBeginTime").toString();
				String endTime = goodsRecord.get("goodsEndTime").toString();
				SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm");
				Date now = new Date();
				Date begin = df.parse(beginTime);
				Date end = df.parse(endTime);
				if (now.getTime() < begin.getTime() && now.getTime() > end.getTime()) {
					// ��ɱ����
					jsonString = jsonData.getJson(1001, "����Ʒ��ɱ����");
				} else {
					// ��ɱδ�������ж���Ʒ�Ƿ��Ѿ�����
					int num = Integer.parseInt(goodsRecord.get("goodsNum").toString());
					int limit = Integer.parseInt(goodsRecord.get("goodsLimit").toString());
					if (num >= limit) {
						// ������
						jsonString = jsonData.getJson(1002, "����Ʒ�ѱ�����");
					} else {
						// ��Ʒδ����
						// �ж��û��Ƿ��Ѿ���������Ʒ
						List<Record> judge1 = Db.find("select * from orders where orderUserID=" + userID
								+ " and orderGoodsID=" + goodsID + " ");
						if (judge1.size() != 0) {
							// �û�����������Ʒ
							jsonString = jsonData.getJson(1003, "�û�������������Ʒ");
						} else {
							// �ж��û��Ƿ�����һ���Ѿ���������Ʒ
							String time = df.format(new Date());
							time = time.substring(0, 10);
							List<Record> judge2 = Db.find("select * from orders where orderUserID=" + userID
									+ " and orderTime LIKE '" + time + "%'  ");
							if (judge2.size() != 0) {
								// ��һ���Ѿ�������Ʒ
								jsonString = jsonData.getJson(1004, "�û���������������Ʒ");
							} else {
								// ��ɱ!
								int count = 0;
								count = Db.update(
										"update goods set goodsNum = goodsNum+1 where goodsID=" + goodsID + " ");
								// ����12λ����ʶ����
								String base = "0123456789";
								Random random = new Random();
								StringBuffer sb = new StringBuffer();
								for (int i = 0; i < 12; i++) {
									int number = random.nextInt(base.length());
									sb.append(base.charAt(number));
								}
								String verifyCode = sb.toString();
								// ���ɶ���
								Order order = new Order().set("orderUserID", userID).set("orderGoodsID", goodsID)
										.set("orderVerifyCode", verifyCode);
								if (order.save() && count == 1) {
									// ������Ӧ����
									String orderID = order.get("orderID").toString();
									orderID = DES.encryptDES(orderID, "YZadZjYx");
									Record record = new Record().set("orderID", orderID);
									List<Record> list = new ArrayList<Record>();
									list.add(record);
									jsonString = jsonData.getJson(0, "success", list);
								} else {
									jsonString = jsonData.getJson(-50, "���ݲ���ʧ��");
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			jsonString = jsonData.getJson(2001, "����ʧ��");
		}
		renderText(jsonString);
	}

	/**
	 * ��ȡ��֤��Ϣ
	 */
	public void GetVerifyInfo() {
		String code = this.getPara("orderID");
		try {
			String orderID = DES.decryptDES(code, "YZadZjYx");
			List<Record> list = Db.find(
					"select orderTime,orderVerifyCode,unickname,upic,goodsName,goodsPicture,goodsPrice from users,goods,orders where orderUserID=userid and orderGoodsID=goodsID and orderID="
							+ orderID + "  ");
			jsonString = jsonData.getJson(0, "success", list);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			jsonString = jsonData.getJson(2001, "����ʧ��");
		}
		renderText(jsonString);
	}

	/**
	 * ��ʾһ���ڵĽ�����
	 */
	public void ShowTodayMemoryInOneWeek() {
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");// �������ڸ�ʽ
		// ��ȡ���������
		String tomorrow = df.format(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24));
		List<Record> list = Db
				.find("select * from todaymemory where TMtime < '" + tomorrow + "' order by TMtime desc limit 7");
		jsonString = jsonData.getJson(0, "success", list);
		renderText(jsonString);
	}

	/**
	 * ��ʾ����Ľ�����
	 */
	public void ShowTodayMemoryInThreeDay() {
		Date date = new Date();// ȡʱ��
		Calendar calendar = new GregorianCalendar();
		calendar.setTime(date);
		calendar.add(Calendar.DATE, 1);// ��������������һ��.����������,������ǰ�ƶ�
		date = calendar.getTime(); // ���ʱ���������������һ��Ľ��
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		String tomorrow = formatter.format(date);
		List<Record> list = Db.find("select TMpic,TMtext from todaymemory where date(TMtime) <= '" + tomorrow
				+ "' order by TMtime desc limit 3");
		jsonString = jsonData.getJson(0, "success", list);
		renderText(jsonString);
	}

	/**
	 * �ϴ�������
	 */
	public void UploadTodayMemory() {
		String picUrl = this.getPara("picUrl");
		String audioUrl = this.getPara("audioUrl");
		String text = this.getPara("text");
		String date = this.getPara("date") + " 20:00:00";

		TodayMemory tm = new TodayMemory().set("TMpic", picUrl).set("TMaudio", audioUrl).set("TMtext", text)
				.set("TMtime", date);
		boolean flag = tm.save();
		jsonString = dataProcess.insertFlagResult(flag);
		renderText(jsonString);
	}

	/**
	 * ɾ��������
	 */
	public void DeleteTodayMemory() {
		String TMid = this.getPara("TMid");
		TodayMemory tm = new TodayMemory().findById(TMid);
		boolean flag = tm.delete();
		jsonString = dataProcess.deleteFlagResult(flag);
		renderText(jsonString);
	}

	/**
	 * ��ʾ��ҳ�������Ϣ
	 */
	public void ShowWebAlbumInformation() {
		String userid = this.getPara("userid");
		String groupid = this.getPara("groupid");
		// �˴���groupid���ƶ��˴���web�˵ļ������ݣ��������н��ܻ�ȡ���ĵ�groupid
		groupid = dataProcess.decryptData(groupid, "groupid");
		System.out.println(groupid);
		List<Record> list = service.getSingleOfficialAlbumInfo(userid, groupid);
		if (list.size() == 0) {
			jsonString = jsonData.getJson(1012, "����ѱ�ɾ��");
		} else {
			jsonString = jsonData.getJson(0, "success", list);
		}
		renderText(jsonString);
	}

	/**
	 * ��ʾ��ҳ����ᶯ̬����
	 */
	public void ShowWebAlbumContents() {
		String userid = this.getPara("userid");
		String sign = this.getPara("sign");
		String gtype = this.getPara("gtype");
		String eventid = this.getPara("eventid");
		String groupid = this.getPara("groupid");
		// �˴���groupid���ƶ��˴���web�˵ļ������ݣ��������н��ܻ�ȡ���ĵ�groupid
		groupid = dataProcess.decryptData(groupid, "groupid");
		if (gtype.equals("�ٷ����")) {
			jsonString = service.getOfficialAlbumEvents(userid, groupid, eventid, sign);
		} else {
			jsonString = service.getPrivateAlbumEvents(userid, groupid, eventid, sign);
		}
		renderText(jsonString);
	}

	/**
	 * ���������ᣬ��ҳ�˽ӿ�
	 */
	@Before(Tx.class)
	public void EnterAlbum() {
		String userid = this.getPara("userid");
		String groupid = this.getPara("groupid");
		// �˴���groupid���ƶ��˴���web�˵ļ������ݣ��������н��ܻ�ȡ���ĵ�groupid
		groupid = dataProcess.decryptData(groupid, "groupid");
		System.out.println(groupid);
		// �ж��û��Ƿ�������
		List<Record> list = Db
				.find("select * from groupmembers where gmgroupid=" + groupid + " and gmuserid=" + userid + " ");
		// ��ȡ������֮ǰ�������г�ԱID������֪ͨ
		List<Record> groupmemberList = service.getGroupMemberID(groupid);
		// ��ȡ�������
		List<Record> groupTypeList = Db.find("select gtype from groups where groupid=" + groupid + " ");
		String gtype = groupTypeList.get(0).get("gtype").toString();
		if (list.size() == 0) {
			// ͨ���ж�������������Ҫ�����ĸ�֪ͨ����
			boolean insertFlag = false;
			boolean NotificationFlag = false;
			if (gtype.equals("5")) {
				TxService.enterOfficialAlbum(groupid, userid);// ���鲢��������Ϣ�����û���ӵ��������б���
				// �������ݵ�likes����
				insertFlag = TxService.newUserJoinInsertLikes(userid, groupid);
				NotificationFlag = TxService.enterOfficialAlbumNotification(groupid, userid);
			} else {
				TxService.enterGroup(groupid, userid);
				NotificationFlag = TxService.insertEnterNotification(groupid, userid, groupmemberList);
				insertFlag = true;
			}
			if (NotificationFlag && insertFlag) {
				jsonString = jsonData.getJson(0, "success");
			} else {
				jsonString = jsonData.getJson(-51, "��������ʧ��");
			}
		} else {
			jsonString = jsonData.getJson(1010, "�û���������");
		}
		renderText(jsonString);
	}

	/**
	 * ��ȡ��������Ϣ
	 */
	public void GetInviteInfo() {
		String userid = this.getPara("userid");
		String code = this.getPara("code");
		// ��code���н���
		String inviteUserid = dataProcess.decryptData(code, "userid");
		String groupid = dataProcess.decryptData(code, "groupid");
		System.out.println(inviteUserid);
		System.out.println(groupid);
		// ��ȡ�������
		Record userRecord = Db.findFirst("select unickname,upic from users where userid=" + inviteUserid + " ");
		Record groupRecord = Db.findFirst("select gtype,gname from groups where groupid=" + groupid + " ");
		// �ж��û��Ƿ���������
		List<Record> judge = Db.find("select * from groupmembers where gmuserid=" + userid + " and gmgroupid=" + groupid
				+ " and gmstatus=0");
		if (judge.size() == 0) {
			userRecord.set("isInAlbum", 0);
		} else {
			userRecord.set("isInAlbum", 1);
		}
		// ��������
		userRecord.set("gtype", groupRecord.get("gtype").toString()).set("gname", groupRecord.get("gname").toString());
		List<Record> list = new ArrayList<Record>();
		list.add(userRecord);
		// ��������ת������Ӧ������
		list = dataProcess.changeGroupTypeIntoWord(list);
		jsonString = jsonData.getJson(0, "success", list);
		renderText(jsonString);
	}

	/**
	 * ��ȡ�û�����������������
	 */
	public void GetUserAllCoupleAlbums() {
		String userid = this.getPara("userid");
		userid = dataProcess.decryptData(userid, "userid");
		List<Record> list = Db.find(
				"select groupid,gname,gpic from groups,groupmembers where groupid=gmgroupid and gtype=3 and gmuserid="
						+ userid + " and gstatus=0 and gmstatus=0 ");
		jsonString = jsonData.getJson(0, "success", list);
		renderText(jsonString);
	}

	/**
	 * ��������ʱ���
	 */
	public void CreateLoverTimeMachine() {
		String userid = this.getPara("userid");
		String groupid = this.getPara("groupid");
		String name = this.getPara("name");
		String memoryTime = this.getPara("memoryTime");
		userid = dataProcess.decryptData(userid, "userid");
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String time = sdf.format(new Date());
		List<Record> list = Db
				.find("select ltmID,ltmLoverName,ltmMemoryTime,ltmGroupID from lovertimemachine where ltmUserid="
						+ userid + " and ltmStatus=0 ");

		if (list.size() == 0) {
			LoverTimeMachine ltm = new LoverTimeMachine().set("ltmUserID", userid).set("ltmGroupID", groupid)
					.set("ltmLoverName", name).set("ltmMemoryTime", memoryTime);
			// �ж�������Ƿ����е���ʱ��û����������
			boolean groupFlag = true;
			Record record = Db.findFirst("select grecordtime from groups where groupid=" + groupid + " ");
			if (record.get("grecordtime") == null) {
				Group group = new Group().findById(groupid).set("grecordtime", time);
				groupFlag = group.update();
			}
			boolean ltmFlag = false;
			ltmFlag = ltm.save();
			if (ltmFlag && groupFlag) {
				jsonString = jsonData.getJson(0, "success");
			} else {
				jsonString = jsonData.getJson(-51, "��������ʧ��");
			}
		} else {
			jsonString = jsonData.getJson(2005, "�û��Ѵ�������ʱ���");
		}
		renderText(jsonString);
	}

	/**
	 * ��ʾ����ʱ���
	 */
	public void ShowLoverTimeMachine() {
		String userid = this.getPara("userid");
		userid = dataProcess.decryptData(userid, "userid");
		System.out.println(userid);
		List<Record> list = Db.find(
				"select ltmID,ltmLoverName,ltmMemoryTime,ltmGroupID,unickname from lovertimemachine,users where userid=ltmUserid and ltmUserid="
						+ userid + " and ltmStatus=0 ");
		if (list.size() == 0) {
			jsonString = jsonData.getJson(2004, "�û�δ��������ʱ���");
		} else {
			String groupid = list.get(0).get("ltmGroupID").toString();
			Record record = Db.findFirst("select grecordtime,gtime from groups where groupid=" + groupid + " ");
			List<Record> picList = Db.find(
					"select count(*) as picNum from groups,events,pictures where groupid=egroupid and eid=peid and groupid="
							+ groupid + " group by groupid ");
			if (picList.size() == 0) {
				list.get(0).set("pictureNum", 0);
			} else {
				list.get(0).set("pictureNum", picList.get(0).get("picNum"));
			}
			list.get(0).set("grecordtime", record.get("grecordtime").toString()).set("gtime",
					record.get("gtime").toString());
			jsonString = jsonData.getJson(0, "success", list);
		}
		renderText(jsonString);
	}

	/**
	 * ��ʾ������̬,���ܰ�
	 */
	public void ShowSingleEvent() {
		// ��ȡ����
		String code = this.getPara("code");
		String eventid = dataProcess.decryptData(code, "eventid");
		jsonString = service.getSingleEvent(eventid);
		// ���ؽ��
		renderText(jsonString);
	}

	/**
	 * ɾ�����ű�����Ƭ��web�汾
	 */
	public void DeleteSingleBackupPhoto() {
		String backupPhotoID = this.getPara("backupPhotoID");
		int count = Db.update("update backupphoto set backupPStatus=1 where backupPhotoID in(" + backupPhotoID + ") ");
		// �ж��Ƿ�Ϊ���һ��
		BackupPhoto bup = new BackupPhoto().findById(backupPhotoID);
		String bupEid = bup.get("backupPEid").toString();
		List<Record> judge = Db.find("select * from backupphoto where backupPEid=" + bupEid + " and backupPStatus=0 ");
		if (judge.size() == 0) {
			BackupEvent bue = new BackupEvent().findById(bupEid);
			bue.set("backupStatus", 1);
			bue.update();
		}

		jsonString = dataProcess.updateFlagResult(1 == count);
		renderText(jsonString);
	}

	/**
	 * չʾ��Ƭǽ
	 */
	public void ShowPhotoWall() {
		String type = this.getPara("type");
		String code = this.getPara("code");

		String groupid = "";
		try {
			code = DES.decryptDES(code, CommonParam.DESSecretKey);
			String[] array = code.split(",");
			// ��ȡgroupid
			for (int i = 0; i < array.length; i++) {
				if (((array[i].split("="))[0]).equals("groupid")) {
					groupid = (array[i].split("="))[1];
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		int id = Integer.parseInt(this.getPara("id"));
		List<Record> list = h5Service.getPhotoWall(type, groupid, id);
		jsonString = jsonData.getJson(0, "success", list);
		// ���ؽ��
		renderText(jsonString);
	}

	/**
	 * PC��ע���¼
	 */
	public void PCLoginAndRegister() {
		// ���ղ���
		String code = this.getPara("code");
		// �û���Դ�ͻ��˼���Ӧ�汾
		String source = this.getPara("source");
		String version = this.getPara("version");
		// ��¼�û���Դ�˿ڲ���
		String port = this.getPara("port");
		port = (port == null ? "��Ȼ����" : port);
		String fromUserID = this.getPara("fromUserID");
		String fromSpaceID = this.getPara("fromSpaceID");
		String fromEventID = this.getPara("fromEventID");

		// ���ݵ�¼��code��΢�ŷ������������󣬻�ȡ�û���access_token��openid
		String result = dataProcess.sentNetworkRequest(CommonParam.getPCAccessToken + code);
		if (result.equals("")) {
			jsonString = jsonData.getJson(1047, "����΢�ŷ�����ʧ��");
		} else {
			JSONObject jo = JSONObject.parseObject(result);
			String access_token = jo.getString("access_token");
			String openid = jo.getString("openid");

			// ��ȡ�û�΢����Ϣ��unionid
			result = dataProcess.sentNetworkRequest(
					CommonParam.getWechatUserInfoUrl + "?access_token=" + access_token + "&openid=" + openid);
			if (result.equals("")) {
				jsonString = jsonData.getJson(1047, "����΢�ŷ�����ʧ��");
			} else {
				jo = JSONObject.parseObject(result);
				String nickname = jo.getString("nickname");
				String unionid = jo.getString("unionid");
				String headimgurl = jo.getString("headimgurl");
				String sex = String.valueOf(jo.getIntValue("sex"));
				String province = jo.getString("province");
				String city = jo.getString("city");

				// ���е�¼��ע���ж�
				List<Record> list = service.judgeUserQQorWechatID(unionid, "wechat");
				if (list.size() == 0) {
					// ��΢���û�δע�ᣬע��󷵻�userid
					String userid = service.wechatUserRegister(unionid, nickname, headimgurl, sex, "PC", province, city,
							version, port, fromUserID, fromSpaceID, fromEventID, null);
					if (userid.equals("")) {
						jsonString = jsonData.getJson(1016, "΢���û�ע��ʧ��");
					} else {
						// ��������ϵͳ�������
						boolean createFlag1 = TxService.creatDefaultAlbum(userid, "6", "С����");
						boolean createFlag2 = TxService.creatDefaultAlbum(userid, "7", "С����");
						boolean createFlag3 = TxService.creatDefaultAlbum(userid, "8", "С����");
						if (createFlag1 && createFlag2 && createFlag3) {
							// ע�ỷ���û�
							im.AddSingleIMUser(userid, nickname);

							Record record = new Record();
							record.set("userid", userid).set("unickname", nickname);
							list.add(record);
							jsonString = jsonData.getJson(0, "success", list);
						} else {
							jsonString = jsonData.getJson(-50, "��������ʧ��");
						}
					}
				} else {
					// ��΢���û���ע�ᣬ����û���ʷ���ʼ�¼����ӳɹ����¼���ҷ���userid
					String userid = list.get(0).get("userid").toString();
					UserService userService = new UserService();
					if (userService.AddHistoryAccessInfo(userid, source, version, port, null)) {
						// �ж��Ƿ���APP�˵�¼�����������ƽ���
						List<Record> encourage = Encourage.getAppEncourageInfo(userid);
						int appLoginWeb = Integer.parseInt(encourage.get(0).get("appLoginWeb").toString());
						if (appLoginWeb == 0)
							Encourage.SetOneFieldCanBeGet(userid, "appLoginWeb");
						// ���ؽ��
						list.get(0).set("unickname", nickname);
						jsonString = jsonData.getJson(0, "success", list);
					} else {
						jsonString = jsonData.getJson(-51, "�������ݿ�����ʧ��");
					}
				}

			}

		}
		renderText(jsonString);
	}
	/**
	 * С�����¼ע�� by lk ����Ĭ�϶�̬
	 */
	public void SmallAppLoginAndRegisterByLk() {
		String code = this.getPara("code");
		String encodeData = this.getPara("encodeData");
		String iv = this.getPara("iv");
		// �û���Դ�ͻ��˼���Ӧ�汾
		String source = this.getPara("source");
		source = (source == null ? "С����" : source);
		String version = this.getPara("version");
		// ��¼�û���Դ�˿ڲ���
		String port = this.getPara("port");
		port = ((port == null || port.equals("")) ? "��Ȼ����" : port);
		String fromUserID = this.getPara("fromUserID");
		String fromSpaceID = this.getPara("fromSpaceID");
		String fromEventID = this.getPara("fromEventID");

		// ���ݵ�¼��code��΢�ŷ������������󣬻�ȡ�û���session_key��openid
		String result = "";

		switch (source) {
		case "С����":
			result = dataProcess.sentNetworkRequest(CommonParam.getWechatSessionKeyUrl + code);
			break;
		case "��ͼС����":
			result = dataProcess.sentNetworkRequest(CommonParam.playImageGetWechatSessionKeyUrl + code);
			break;
		case "����С����":
			result = dataProcess.sentNetworkRequest(CommonParam.testGetWechatSessionKeyUrl + code);
			break;
		case "�����С����":
			result = dataProcess.sentNetworkRequest(CommonParam.jjGetWechatSessionKeyUrl + code);
			break;
		default:
			result = dataProcess.sentNetworkRequest(CommonParam.getWechatSessionKeyUrl + code);
			break;
		}

		JSONObject jo = JSONObject.parseObject(result);
		String session_key = jo.getString("session_key");
		String openid = jo.getString("openid");

		// ���ݽ���
		AES aes = new AES();
		byte[] resultByte;
		String userInfo = "";
		if(null!=encodeData&&null!=session_key&&null!=iv){
			try {
				resultByte = aes.decrypt(Base64.decode(encodeData), Base64.decode(session_key), Base64.decode(iv));
				if (null != resultByte && resultByte.length > 0) {
					userInfo = new String(resultByte, "UTF-8");
				}
			} catch (InvalidAlgorithmParameterException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (userInfo.equals("")) {
			jsonString = jsonData.getJson(1014, "����ʧ��");
		} else {
			// ��������
			jo = JSONObject.parseObject(userInfo);
			String wechatNickname = jo.getString("nickName");
			String wechatPic = jo.getString("avatarUrl");
			String wechatSex = String.valueOf(jo.getIntValue("gender"));
			String province = jo.getString("province");
			String city = jo.getString("city");
			String unionID = jo.getString("unionId");

			// ���е�¼��ע���ж�
			List<Record> list = service.judgeUserQQorWechatID(unionID, "wechat");
			if (list.size() == 0) {
				// ��΢���û�δע�ᣬע��󷵻�userid
				String userid = service.wechatUserRegister(unionID, wechatNickname, wechatPic, wechatSex, source,
						province, city, version, port, fromUserID, fromSpaceID, fromEventID, openid);
				if (userid.equals("")) {
					jsonString = jsonData.getJson(1016, "΢���û�ע��ʧ��");
				} else {
					/**
					 * 1000w��Ƭ�
					 */
					if(CommonParam.pOpenJoinGroup){
						GroupMember gm = new GroupMember();
						boolean isInFlag = gm.judgeUserIsInTheAlbum(Integer.parseInt(userid), CommonParam.pGroupId6); // trueʱ�û����ڿռ���
						if(isInFlag){
							SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
							String time = sdf.format(new Date());
							gm = new GroupMember().set("gmgroupid", CommonParam.pGroupId6).set("gmuserid", userid).set("gmPort", port)
									.set("gmFromUserID", fromUserID).set("isTop", 1).set("topTime", time);
							gm.save();
						}
						service.SetGroupIsTop(userid, CommonParam.pGroupId6+"", "yes");
					}
					/**
					 * 1000w��Ƭ� end
					 */
					// ��������ϵͳ�������
					//boolean createFlag1 = TxService.creatDefaultAlbumAndPulishMsg(userid, "6", "С����");
					//boolean createFlag2 = TxService.creatDefaultAlbumAndPulishMsg(userid, "8", "С����");
					boolean createFlag1 = true;
					boolean createFlag2 = true;
//					if(source.equals("�����С����")){
//						createFlag1 = TxService.creatDefaultAlbum(userid, "9", "�����С����");
//						createFlag1 = TxService.creatDefaultAlbum(userid, "10", "�����С����");
//					}else{
//						createFlag1 = TxService.creatDefaultAlbumAndPulishMsg(userid, "6", "С����");
//						createFlag2 = TxService.creatDefaultAlbumAndPulishMsg(userid, "8", "С����");
//					}
					// boolean createFlag3 = TxService.creatDefaultAlbum(userid, "8", "С����");
					if (createFlag1 && createFlag2) {
						// ע�ỷ���û�
						im.AddSingleIMUser(userid, wechatNickname);

						Record record = new Record();
						record.set("userid", userid).set("isNewUser", "yes").set("unickname", wechatNickname)
								.set("uLockPass", null).set("openIdFlag", "true");
						list.add(record);
						jsonString = jsonData.getJson(0, "success", list);
					} else {
						jsonString = jsonData.getJson(-50, "��������ʧ��");
					}
				}
			} else {
				// ��΢���û���ע�ᣬ����û���ʷ���ʼ�¼����ӳɹ����¼���ҷ���userid
				String userid = list.get(0).get("userid").toString();
				//by lk �޸��û�ͷ��
				if(list.get(0).get("upic").toString().indexOf("https://wx.qlogo.cn/mmopen/")!=-1){
					User u=new User();
					u.set("userid", userid);
					u.set("unickname", wechatNickname);
					u.set("upic", wechatPic);
					u.update();
				}
				// lk end 
				UserService userService = new UserService();
				if (userService.AddHistoryAccessInfo(userid, source, version, port, openid)) {
					list.get(0).set("isNewUser", "no").set("unickname", wechatNickname)
							.set("uLockPass", list.get(0).get("uLockPass")).set("openIdFlag", "true");
					/**
					 * 1000w��Ƭ�
					 */
					if(CommonParam.pOpenJoinGroup){
						GroupMember gm = new GroupMember();
						boolean isInFlag = gm.judgeUserIsInTheAlbum(Integer.parseInt(userid), CommonParam.pGroupId6); // trueʱ�û����ڿռ���
						if(isInFlag){
							SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
							String time = sdf.format(new Date());
							gm = new GroupMember().set("gmgroupid", CommonParam.pGroupId6).set("gmuserid", userid).set("gmPort", port)
									.set("gmFromUserID", fromUserID).set("isTop", 1).set("topTime", time);
							gm.save();
						}
						service.SetGroupIsTop(userid, CommonParam.pGroupId6+"", "yes");
					}
					/**
					 * 1000w��Ƭ� end
					 */
					jsonString = jsonData.getJson(0, "success", list);
				} else {
					jsonString = jsonData.getJson(-51, "�������ݿ�����ʧ��");
				}
			}
		}
		// ���ؽ��
		renderText(jsonString);
	}

	/**
	 * С�����¼ע��
	 */
	public void SmallAppLoginAndRegister() {
		String code = this.getPara("code");
		String encodeData = this.getPara("encodeData");
		String iv = this.getPara("iv");
		// �û���Դ�ͻ��˼���Ӧ�汾
		String source = this.getPara("source");
		source = (source == null ? "С����" : source);
		String version = this.getPara("version");
		// ��¼�û���Դ�˿ڲ���
		String port = this.getPara("port");
		port = ((port == null || port.equals("")) ? "��Ȼ����" : port);
		String fromUserID = this.getPara("fromUserID");
		String fromSpaceID = this.getPara("fromSpaceID");
		String fromEventID = this.getPara("fromEventID");

		// ���ݵ�¼��code��΢�ŷ������������󣬻�ȡ�û���session_key��openid
		String result = "";

		switch (source) {
		case "С����":
			result = dataProcess.sentNetworkRequest(CommonParam.getWechatSessionKeyUrl + code);
			break;
		case "��ͼС����":
			result = dataProcess.sentNetworkRequest(CommonParam.playImageGetWechatSessionKeyUrl + code);
			break;
		case "����С����":
			result = dataProcess.sentNetworkRequest(CommonParam.testGetWechatSessionKeyUrl + code);
			break;
		case "�����С����":
			result = dataProcess.sentNetworkRequest(CommonParam.jjGetWechatSessionKeyUrl + code);
			break;
		default:
			result = dataProcess.sentNetworkRequest(CommonParam.getWechatSessionKeyUrl + code);
			break;
		}

		JSONObject jo = JSONObject.parseObject(result);
		String session_key = jo.getString("session_key");
		String openid = jo.getString("openid");

		// ���ݽ���
		AES aes = new AES();
		byte[] resultByte;
		String userInfo = "";
		try {
			resultByte = aes.decrypt(Base64.decode(encodeData), Base64.decode(session_key), Base64.decode(iv));
			if (null != resultByte && resultByte.length > 0) {
				userInfo = new String(resultByte, "UTF-8");
			}
		} catch (InvalidAlgorithmParameterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (userInfo.equals("")) {
			jsonString = jsonData.getJson(1014, "����ʧ��");
		} else {
			// ��������
			jo = JSONObject.parseObject(userInfo);
			String wechatNickname = jo.getString("nickName");
			String wechatPic = jo.getString("avatarUrl");
			String wechatSex = String.valueOf(jo.getIntValue("gender"));
			String province = jo.getString("province");
			String city = jo.getString("city");
			String unionID = jo.getString("unionId");

			// ���е�¼��ע���ж�
			List<Record> list = service.judgeUserQQorWechatID(unionID, "wechat");
			if (list.size() == 0) {
				// ��΢���û�δע�ᣬע��󷵻�userid
				String userid = service.wechatUserRegister(unionID, wechatNickname, wechatPic, wechatSex, source,
						province, city, version, port, fromUserID, fromSpaceID, fromEventID, openid);
				if (userid.equals("")) {
					jsonString = jsonData.getJson(1016, "΢���û�ע��ʧ��");
				} else {
					// ��������ϵͳ�������
					boolean createFlag1 = false;
					boolean createFlag2 = false;
					if(source.equals("�����С����")){
						createFlag1 = TxService.creatDefaultAlbum(userid, "9", "�����С����");
						createFlag1 = TxService.creatDefaultAlbum(userid, "11", "�����С����");
					}else{
						createFlag1 = TxService.creatDefaultAlbum(userid, "6", "С����");
						createFlag2 = TxService.creatDefaultAlbum(userid, "8", "С����");
					}
//					boolean createFlag1 = TxService.creatDefaultAlbum(userid, "6", "С����");
//					boolean createFlag2 = TxService.creatDefaultAlbum(userid, "7", "С����");
					// boolean createFlag3 = TxService.creatDefaultAlbum(userid, "8", "С����");
					if (createFlag1 && createFlag2) {
						// ע�ỷ���û�
						im.AddSingleIMUser(userid, wechatNickname);

						Record record = new Record();
						record.set("userid", userid).set("isNewUser", "yes").set("unickname", wechatNickname)
								.set("uLockPass", null).set("openIdFlag", "true");
						list.add(record);
						jsonString = jsonData.getJson(0, "success", list);
					} else {
						jsonString = jsonData.getJson(-50, "��������ʧ��");
					}
				}
			} else {
				// ��΢���û���ע�ᣬ����û���ʷ���ʼ�¼����ӳɹ����¼���ҷ���userid
				String userid = list.get(0).get("userid").toString();
				User u=new User();
				u.set("userid", userid);
				u.set("upic", wechatPic);
				u.update();
				UserService userService = new UserService();
				if (userService.AddHistoryAccessInfo(userid, source, version, port, openid)) {
					list.get(0).set("isNewUser", "no").set("unickname", wechatNickname)
							.set("uLockPass", list.get(0).get("uLockPass")).set("openIdFlag", "true");
					jsonString = jsonData.getJson(0, "success", list);
				} else {
					jsonString = jsonData.getJson(-51, "�������ݿ�����ʧ��");
				}
			}
		}
		// ���ؽ��
		renderText(jsonString);
	}

	/**
	 * ��¼С���������Ϣ
	 */
	public void RecordSmallAppFaultMsg() {
		String msg = this.getPara("msg");
		String device = this.getPara("device");
		SmallAppLog sal = new SmallAppLog().set("msg", msg).set("device", device);
		jsonString = dataProcess.insertFlagResult(sal.save());
		renderText(jsonString);
	}

	/**
	 * ���û�ȡ����������
	 */
	public void SetOrCancelLockPass() {
		String userid = this.getPara("userid");
		String password = this.getPara("password");
		String type = this.getPara("type");

		User user = new User().findById(userid);

		switch (type) {
		case "set":
			user.set("uLockPass", password);
			break;
		case "cancel":
			user.set("uLockPass", null);
			break;
		}

		jsonString = dataProcess.updateFlagResult(user.update());
		renderText(jsonString);

	}
	/*
	 * by lk ���ʾ
	 */
	public void GetActivityMsg(){
		String userid=this.getPara("userid")==null?"":this.getPara("userid");
		H5Service service=new H5Service();
		if(userid.equals("")){		
			jsonString = jsonData.getJson(2, "�����������");
		}else{
			jsonString = jsonData.getJson(0, "success", service.GetActivityMsg(userid));
		}
		renderText(jsonString);
	}
	/**
	 * by lk ��¼���ʾ��ʾ״̬
	 */
	public void SetUserJoinActivity(){
		String userid=this.getPara("userid")==null?"":this.getPara("userid");
		String activityId=this.getPara("activityId")==null?"":this.getPara("activityId");
		H5Service service=new H5Service();
		if(userid.equals("")||activityId.equals("")||activityId.equals("0")){		
			jsonString = jsonData.getJson(2, "�����������");
		}else{
			service.SetActivityMsg(userid,activityId);
			jsonString = jsonData.getJson(0, "success");
		}
		renderText(jsonString);
	}
	/**
	 * ��ȡ��������ͼƬ
	 */
	public void GetGroupPic(){
//		System.out.println("123");
		QiniuOperate operate = new QiniuOperate();
		String groupid=this.getPara("groupid")==null?"":this.getPara("groupid");
		String userid=this.getPara("userid")==null?"":this.getPara("userid");
		List<Record> returnList=new ArrayList<>();
		List<Record> picList=new ArrayList<>();
		Record r=new Record();
		Record pr=new Record();
		if(!groupid.equals("")&&!userid.equals("")){	
			Group group=new Group().findById(groupid);
			r.set("groupname",group.get("gname"));
			User user=new User().findById(userid);
			r.set("unickname", user.get("unickname"));
			r.set("upic", user.get("upic"));
			pr.set("poriginal", "http://7xlmtr.com1.z0.glb.clouddn.com/20180313_3.png");
			pr.set("thumbnail", "http://7xlmtr.com1.z0.glb.clouddn.com/20180313_3.png");
			pr.set("midThumbnail", "http://7xlmtr.com1.z0.glb.clouddn.com/20180313_3.png");
			picList.add(pr);		
			r.set("picList", picList);			
			returnList.add(r);
			jsonString = jsonData.getSuccessJson(returnList);		
		}else{
			jsonString = jsonData.getJson(2, "�����������");
		}
		renderText(jsonString);
	}
	/**
	 * ��ȡ������һ����̬��1��ͼƬ
	 */
	public void GetGroupLastPic(){
//		System.out.println("123");
		QiniuOperate operate = new QiniuOperate();
		String groupid=this.getPara("groupid")==null?"":this.getPara("groupid");
		String userid=this.getPara("userid")==null?"":this.getPara("userid");
		List<Record> returnList=new ArrayList<>();
		List<Record> picList=new ArrayList<>();
		Record r=new Record();
		if(!groupid.equals("")&&!userid.equals("")){	
			Group group=new Group().findById(groupid);
			r.set("groupname",group.get("gname"));
			User user=new User().findById(userid);
			r.set("unickname", user.get("unickname"));
			r.set("upic", user.get("upic"));
			List<Record> eventList=Db.find("select eid from events where eMain=0 and estatus=0 and egroupid="+groupid+" order by eid desc limit 0,1");
			//System.out.println("eid="+eventList.get(0).getLong("eid").intValue());
			if(!eventList.isEmpty()){
				List<Record> pList=Db.find("select poriginal from pictures where pstatus = 0 and peid ="+eventList.get(0).getLong("eid").intValue()+" order by puploadtime desc limit 0,1");
				if(!pList.isEmpty()){
					for(Record pic:pList ){
						//System.out.println("poriginal="+pic.getStr("poriginal"));
						Record p=new Record();
						p.set("poriginal",operate.getDownloadToken(pic.getStr("poriginal")));
						p.set("thumbnail", operate
								.getDownloadToken(pic.getStr("poriginal") + "?imageView2/2/w/250"));
						// �е�����ͼ��Ȩ
						p.set("midThumbnail", operate
								.getDownloadToken(pic.getStr("poriginal") + "?imageView2/2/w/1000"));
						picList.add(p);	
					}								
				}
				r.set("picList", picList);
				returnList.add(r);
				jsonString = jsonData.getSuccessJson(returnList);
			}else{
				Record p=new Record();
				p.set("poriginal",group.get("gpic"));
				p.set("thumbnail", group.get("gpic"));
				// �е�����ͼ��Ȩ
				p.set("midThumbnail", group.get("gpic"));
				picList.add(p);
				r.set("picList", picList);			
				returnList.add(r);
				jsonString = jsonData.getSuccessJson(returnList);
			}
		}else{
			jsonString = jsonData.getJson(2, "�����������");
		}
		renderText(jsonString);
	}
//	/**
//	 * ���� ������������
//	 */
//	public void getActivityDialog(){
//		String groupid=this.getPara("groupid");
//		if(null!=groupid&&!groupid.equals("")){
//			//String
//			List<Record> list=new GroupCanPublish().getDialogByGroupid(groupid, "2");
//			if(null!=list&&!list.isEmpty()){
//				Record r=list.get(0);
//				r.remove("id");
//				r.remove("pGroupType");
//				r.remove("pStatus");
//				r.remove("createTime");
//			}
//			jsonString = jsonData.getSuccessJson(list);
//		}else{
//			jsonString = jsonData.getJson(2, "�����������");
//		}
//		renderText(jsonString);
//	}
	/*
	 * �ж��û��Ƿ������������
	 */
	public void getUserCreateGroupCnt(){
		jsonString = jsonData.getJson(2, "�����������");
		String userid=this.getPara("userid");
		if(null!=userid&&!userid.equals("")){
			jsonString = jsonData.getSuccessJson(h5Service.getUserCreateGroupCnt(userid));
		}
		renderText(jsonString);
	}
	/**
	 * ����ͼƬIDɾ��ͼƬ�����û�ɾ����һ����̬�����е�ͼƬ��ɾ����̬
	 */
	public void deletePic(){
		jsonString = jsonData.getJson(2, "�����������");
		String userid = this.getPara("userid")==null?"":this.getPara("userid");
		String pid = this.getPara("pid")==null?"":this.getPara("pid");
		String source = this.getPara("source");
		if(!pid.equals("")){			
			List<Record> list=h5Service.deletePic(userid,pid);
				jsonString = jsonData.getSuccessJson(list);			
		}
		System.out.println("jsonString:"+jsonString);
		renderText(jsonString);
	}
}
