package yinian.controller;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;

import yinian.app.YinianDataProcess;
import yinian.common.CommonParam;
import yinian.interceptor.CrossDomain;
import yinian.model.Comment;
import yinian.model.Event;
import yinian.model.FormID;
import yinian.model.Group;
import yinian.model.GroupCanPublish;
import yinian.model.GroupMember;
import yinian.model.Like;
import yinian.model.Picture;
import yinian.model.TextLibrary;
import yinian.model.User;
import yinian.service.EventService;
import yinian.service.TestService;
import yinian.thread.EventPushPicNumThread;
import yinian.thread.PictureVerifyThread;
import yinian.thread.PictureVerifyThreadNew;
import yinian.thread.VerifyPicture;
import yinian.utils.JsonData;
import yinian.utils.PHash;
import yinian.utils.QiniuOperate;
import yinian.utils.SmallAppQRCode;

import com.jfinal.aop.Before;
import com.jfinal.core.Controller;
import com.jfinal.log.Logger;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;
import com.jfinal.plugin.ehcache.CacheKit;
import com.jfinal.upload.UploadFile;

public class EventController extends Controller {
	

	private String jsonString; // ���ص�json�ַ���
	private JsonData jsonData = new JsonData(); // json������
	private YinianDataProcess dataProcess = new YinianDataProcess();// ���ݴ�����
	private EventService service = new EventService();// ҵ������
	private QiniuOperate qiniu = new QiniuOperate();
	// enhance������Ŀ�����AOP��ǿ
	EventService TxService = enhance(EventService.class);
	private static final Logger log = Logger.getLogger(EventController.class);

	/**
	 * ��ȡ���ֿ�����
	 */
	public void GetTextLibraryType() {
		TextLibrary tl = new TextLibrary();
		List<Record> list = tl.GetAllTextType();
		jsonString = jsonData.getSuccessJson(list);
		renderText(jsonString);
	}

	/**
	 * ��ȡ���ֿ�����
	 */
	public void GetTextOfOneTextType() {
		String textType = this.getPara("textType");
		int textID = Integer.parseInt(this.getPara("textID"));
		TextLibrary tl = new TextLibrary();
		List<Record> list = tl.GetTextOfOneTextType(textType, textID);
		jsonString = jsonData.getSuccessJson(list);
		renderText(jsonString);
	}

	/**
	 * �ϴ���̬
	 */
	@Before(CrossDomain.class)
	public void UploadEvent() {
		// ˭�����ĸ��ռ���
		String userid = this.getPara("userid")==null?"":this.getPara("userid");
		String groupid = this.getPara("groupid")==null?"":this.getPara("groupid");
		// ͼƬ
		String picAddress = this.getPara("picAddress")==null?"":this.getPara("picAddress");
		// ����
		String content = this.getPara("content")==null?"":this.getPara("content");
		// ����
		String audio = this.getPara("audio")==null?"":this.getPara("audio");
		// �ص�
		String place = this.getPara("place")==null?"":this.getPara("place");
		String placePic = this.getPara("placePic")==null?"":this.getPara("placePic");// λ�����ɵ�ͼƬ��ַ
		String placeLongitude = this.getPara("placeLongitude")==null?"":this.getPara("placeLongitude");// ����
		String placeLatitude = this.getPara("placeLatitude")==null?"":this.getPara("placeLatitude");// γ��
		// ��˭
		String peopleName = this.getPara("peopleName")==null?"":this.getPara("peopleName");
		// ��̬���ĸ�Ҫ��Ϊ��
		String main = this.getPara("main")==null?"":this.getPara("main"); // 0--��Ƭ 1--���� 2--���� 3--�ص�
		// ����Ԫ��
		String storage = this.getPara("storage")==null?"":this.getPara("storage");// �洢�ռ�
		String source = this.getPara("source")==null?"":this.getPara("source");// �жϽӿ���Դ
		String isPush = this.getPara("isPush")==null?"":this.getPara("isPush"); // �����ж� app:yes/no С����:true/false
		String formID = this.getPara("formID")==null?"":this.getPara("formID"); // С�������ͱ�ID

		// ����formID
		//FormID.insertFormID(userid, formID);
		if(!userid.equals("")&&!formID.equals("")){
			FormID.insert(userid, formID);
		}
		// �жϴ洢�ռ��Ƿ��д�
		double storagePlace;
		if (storage == null || storage.equals("")) {
			storagePlace = 0.00;
		} else {
			storagePlace = Double.parseDouble(storage);
		}

		// �ӿ���ԴΪweb����Ҫ����
		if (source != null && source.equals("web")) {
			groupid = dataProcess.decryptData(groupid, "groupid");
		}

		// ��ַ�ֶ����ݴ���
		String firstPic = null;
		String[] picArray = new String[0];
		if (picAddress != null && !picAddress.equals("")) {
			picArray = dataProcess.getPicAddress(picAddress, "private");
			// ͼƬ����
		//	picArray = dataProcess.PictureVerify(picArray);
			// ��ȡ��̬��һ��ͼƬ��ַ,����û���ϴ�ͼƬ
			firstPic = (picArray.length == 0 ? null : picArray[0]);
		}

		// ͼƬ�������˵�������������
		int eid = 0;
		if (main.equals("0") && picArray.length == 0) {
			List<Record> errorList = new ArrayList<Record>();
			Record r = new Record();
			r.set("picList", new ArrayList<String>());
			errorList.add(r);
			jsonString = jsonData.getSuccessJson(errorList);
		} else {
			// ֧��ͬʱ�ϴ�������ռ�
			String[] IDs = groupid.split(",");
			// ����ռ��ϴ�
			for (int i = 0; i < IDs.length; i++) {
				// ͬ�����,0--��ͬ�� 1--ͬ�� ,��һ���ռ�Ϊԭ��������Ϊͬ��
				int isSynchronize = (i == 0 ? 0 : 1);
				eid = TxService.upload(userid, IDs[i], picArray, content, audio, place, placePic, placeLongitude,
						placeLatitude, peopleName, main, storagePlace, firstPic, isPush, source, isSynchronize, formID);
				if (eid != 0) {
					// ˵���ϴ��ɹ�
					List<Record> result = service.getSingleEvent(eid, source);// ��ȡ��̬����Ϣ
					jsonString = jsonData.getSuccessJson(result);
					//�������� by lk
//					if(CommonParam.canPublish){
//						Group group = new Group().findById(IDs[i]);
//						String gOrigin = String.valueOf(group.getLong("gOrigin"));
//						if (gOrigin.equals("0")) {
//							ExecutorService exec = Executors.newCachedThreadPool();
//							exec.execute(new EventPushPicNumThread(IDs[i], picArray.length,userid));
////							// �ر��̳߳�
//							exec.shutdown();
//						}
//					}
				} else {
					jsonString = jsonData.getJson(-50, "���ݲ��뵽���ݿ�ʧ��");
					break;
				}
			}
		}

		// ���ؽ��
		renderText(jsonString);
		//�������ͣ�����������
//		if (eid != 0) {
//			String[] IDs = groupid.split(",");
//			for (int i = 0; i < IDs.length; i++) {
//				// ��ʱ�ϴ���̬���Ϳ���
//				Boolean eventIsPush = false;
//				// �ж��Ƿ�����ͨ��� ����ͨ�����޲�����
//				Group group = new Group().findById(IDs[i]);
//				String gOrigin = String.valueOf(group.getLong("gOrigin"));
//				if (gOrigin.equals("1")) {
//					eventIsPush = false;
//				}
//				if (eventIsPush) {
//					// ����һ���̳߳�
//					ExecutorService exec = Executors.newCachedThreadPool();
//					System.out.println(picArray.length);
//					exec.execute(new EventPushPicNumThread(IDs[i], picArray.length));
//					// �ر��̳߳�
//					exec.shutdown();
//				}
//			}
//		}
		//�������ͣ�����������
		//�����߳�
		if(eid != 0) {
			// ����һ���̳߳�
			ExecutorService exec = Executors.newCachedThreadPool();
			// ִ�м����߳�
			//exec.execute(new PictureVerifyThread(userid,eid, main));
			exec.execute(new PictureVerifyThreadNew(userid,eid, main));
			// �ر��̳߳�
			exec.shutdown();
		}
	}
	
	/**
	 * ��ʾʱ����
	 */
	@Before(CrossDomain.class)
	public void ShowTimeAxis() {
		String userid = this.getPara("userid");
		String groupid = this.getPara("groupid");
		String type = this.getPara("type");
		int eid = this.getParaToInt("eid");
		String source = this.getPara("source");

		// // ���øÿռ�Ը��û����¶�̬
		// GroupMember gm = new GroupMember();
		// gm.UpdateSingleGroupMenberNoNewDynamic(groupid,
		// String.valueOf(userid));

		// ��ʼ��ʱ���Ỻ��
		List<Record> result;
		if (type.equals("initialize")) {
			result = CacheKit.get("ConcurrencyCache", groupid + "InitializeEvent");
			if (result == null) {
				result = service.getSpaceTimeAxisContent(userid, groupid, type, eid, source);
				CacheKit.put("ConcurrencyCache", groupid + "InitializeEvent", result);
			}
		} else {
			result = service.getSpaceTimeAxisContent(userid, groupid, type, eid, source);
		}

		jsonString = jsonData.getSuccessJson(result);
		renderText(jsonString);

	}
	/**
	 * ��ʾʱ���� by lk test
	 */
	@Before(CrossDomain.class)
	public void ShowTimeAxisNew() {
		System.out.println("����ʱ���� "+System.currentTimeMillis());
		String userid = this.getPara("userid");
		String groupid = this.getPara("groupid");
		String type = this.getPara("type");
		int eid = this.getParaToInt("eid");
		String source = this.getPara("source");//h5 С���������޸�
		// ��ʼ��ʱ���Ỻ��
		List<Record> result;
		if (type.equals("initialize")) {
			System.out.println("ʱ���� ��ȡ���濪ʼ��"+System.currentTimeMillis());
			result = CacheKit.get("ConcurrencyCache", groupid + "InitializeEvent_lk_new");
			System.out.println("ʱ���� ��ȡ���������"+System.currentTimeMillis());
			if (result == null) {
				System.out.println("ʱ���� ��ȡDB��ʼ��"+System.currentTimeMillis());
				//result = service.getSpaceTimeAxisContent(userid, groupid, type, eid, source);
				result = service.getSpaceTimeAxisContentByLk(userid, groupid, type, eid, source);
				System.out.println("ʱ���� ��ȡDB������"+System.currentTimeMillis());
				System.out.println("ʱ���� д�뻺�濪ʼ��"+System.currentTimeMillis());
				CacheKit.put("ConcurrencyCache", groupid + "InitializeEvent_lk_new", result);
				System.out.println("ʱ���� д�뻺�������"+System.currentTimeMillis());
			}
		} else {
			//result = service.getSpaceTimeAxisContent(userid, groupid, type, eid, source);
			result = service.getSpaceTimeAxisContentByLk(userid, groupid, type, eid, source);
		}

		jsonString = jsonData.getSuccessJson(result);
		renderText(jsonString);
		System.out.println("�˳�ʱ���� "+System.currentTimeMillis());
	}

	/**
	 * ��ȡ������̬
	 */
	@Before(CrossDomain.class)
	public void GetSingleEventContent() {
		String eid = this.getPara("eid");
		String userid = this.getPara("userid");
		if(eid==null||eid.equals("")||eid.equals("undefined")||eid.equals("NaN")){
			jsonString=jsonData.getJson(2, "��������",new ArrayList<Record>());
			renderText(jsonString);
			return;
		}
		String source = this.getPara("source");
		if (source != null && source.equals("web")) {
			eid = dataProcess.decryptData(eid, "eid");
		}
		List<Record> result=new ArrayList<Record>();
		result = CacheKit.get("ConcurrencyCache", eid + "GetSingleEventContent");
		if(result==null){
			result = service.getSingleEvent(Integer.parseInt(eid), source);
			CacheKit.put("ConcurrencyCache", eid + "GetSingleEventContent", result);
		}
		
		if (result == null) {
			// ��̬��ɾ��
			jsonString = jsonData.getJson(1027, "��̬�ѱ�ɾ��");
		} else {
//			//�û�������� by lk 20171031
//			if(null!=result.get(0)&&userid!=null&&!userid.equals("")||!userid.equals("undefined")||!userid.equals("NaN")){
//				try{
//					int groupid=result.get(0).getLong("egroupid").intValue();
//					GroupMember gm=new GroupMember();
//					if(gm.judgeUserIsInTheAlbum(Integer.parseInt(userid), groupid)){
//						gm.AddGroupMember(Integer.parseInt(userid), groupid);
//					}
//				}catch(Exception e){
//					e.printStackTrace();
//				}
//			}
//			//�û�������� by lk 20171031 end 
			jsonString = jsonData.getSuccessJson(result);
		}

		renderText(jsonString);
	}

	/**
	 * ��ȡ��ռ䵥����̬
	 */
	@Before(CrossDomain.class)
	public void GetActivitySpaceSingleEventContent() {
		String eid = this.getPara("eid");
		String source = this.getPara("source");

		if (source != null && source.equals("web")) {
			eid = dataProcess.decryptData(eid, "eid");
		}

		List<Record> result = service.getSingleEvent(Integer.parseInt(eid), source);
		if (result == null) {
			// ��̬��ɾ��
			jsonString = jsonData.getJson(1027, "��̬�ѱ�ɾ��");
		} else {
			// ��ȡ������Ϣ
			Event event = new Event().findById(eid);
			Group group = new Group().findById(event.get("egroupid").toString());
			result.get(0).set("gname", group.get("gname")).set("gintroducePic", group.get("gintroducePic"))
					.set("gbanner", group.get("gbanner"));

			jsonString = jsonData.getSuccessJson(result);
		}

		renderText(jsonString);
	}

	/**
	 * ��ʾ�ҵģ��ڶ���
	 */
	public void ShowMe2ndVersion() {
		String userid = this.getPara("userid");
		String minID = this.getPara("minID");

		if (minID == null) {
			// ˢ����û�������ˣ�ֱ�ӷ���
			jsonString = jsonData.getSuccessJson();
		} else {
			int eid = Integer.parseInt(minID);
			String source = this.getPara("source");
			List<Record> result = service.getMyEvents2ndVersion(userid, eid, source);
			jsonString = jsonData.getSuccessJson(result);
		}

		renderText(jsonString);
	}

	/**
	 * ��ʾ�ռ��Ա�Ķ�̬
	 */
	public void ShowSpaceMemberEvents() {
		// ��ȡ����
		int userid = this.getParaToInt("userid");
		//int groupid = this.getParaToInt("groupid");
		String minID = this.getPara("minID");
		String source = this.getPara("source");

		if (minID == null||null== this.getPara("groupid")||this.getPara("groupid").equals("")) {
			// ˢ����û�������ˣ�ֱ�ӷ���
			jsonString = jsonData.getSuccessJson();
		} else {
			int groupid = this.getParaToInt("groupid");
			int eid = Integer.parseInt(minID);
			List<Record> result = service.getSpaceMemberEvents(groupid, userid, eid, source);
			jsonString = jsonData.getSuccessJson(result);
		}
		renderText(jsonString);
	}
	
	/**
	 * ��ʾ�ռ��Ա�Ķ�̬
	 */
	public void ShowSpaceMemberEventsNew() {
		// ��ȡ����
		int userid = this.getParaToInt("userid");//����������ĵĸ���id
		String ownUserid = this.getPara("ownUserid");//�������˵ĸ�������ʱ���Լ���id
		//int groupid = this.getParaToInt("groupid");
		int pagenum = Integer.parseInt(this.getPara("pagenum"));
		String source = this.getPara("source");

		int groupid = this.getParaToInt("groupid");
		List<Record> list = service.getSpaceMemberEventsNew(groupid, userid,ownUserid,pagenum,source);
		jsonString = jsonData.getSuccessJson(list);
		renderText(jsonString);
	}
	
	/**
	 * ��ȡ�ռ��Ա��Ƭ���Ͷ�̬��
	 */
	public void GetSpaceMemberPhotoNum() {
		String userid = this.getPara("userid");
		String groupid = this.getPara("groupid");

		List<Record> list = Db.find("select count(*) as num from events,pictures where eid=peid and euserid=" + userid
				+ " and egroupid=" + groupid + " and estatus=0 and pstatus=0 ");
		jsonString = jsonData.getSuccessJson(list);
		renderText(jsonString);
	}
	
	/**
	 * ͨ�����id���û�id��ȡ�ռ��Ա��Ƭ���Ͷ�̬��
	 */
	public void GetSpaceMemberPhotoEventNum() {
		String userid = this.getPara("userid");
		String groupid = this.getPara("groupid");
		Record record = new Record();
		//��ѯ��������Ƭ��
//		List<Record> list = Db.find("select count(*) as num from events,pictures where eid=peid and euserid=" + userid
//				+ " and egroupid=" + groupid + " and estatus=0 and pstatus=0 ");
		List<Record> list = CacheKit.get("ConcurrencyCache", groupid + "_GetSpaceMemberPhotoEventNum_num");
		if(null==list){
			list = Db.find("select count(*) as num from pictures where puserid=" + userid
				+ "  and pstatus=0 and pGroupid=" + groupid);
			CacheKit.put("ConcurrencyCache", groupid + "_GetSpaceMemberPhotoEventNum_num", list);
		}
		record.set("num", 0);
		if(null!=list&&!list.isEmpty()){
			Long num = list.get(0).getLong("num");
			record.set("num", num);
		}
		//��ѯ�����Ķ�̬��
		List<Record> list2 = CacheKit.get("ConcurrencyCache", groupid + "_GetSpaceMemberPhotoEventNum_eventNum");
		if(null==list2){
			list2 = Db.find("select count(*) as eventNum from events where euserid=" + userid
				+ " and egroupid=" + groupid + " and estatus=0");
			CacheKit.put("ConcurrencyCache", groupid + "_GetSpaceMemberPhotoEventNum_eventNum", list2);
		}
		record.set("eventNum", 0);
		if(null!=list2&&!list2.isEmpty()){
			Long eventNum = list2.get(0).getLong("eventNum");
			record.set("eventNum", eventNum);
		}
		//��ѯ���Ĵ����ߺ��Ƿ��ǻ���
		List<Record> list3 = Db.find("select gcreator,gOrigin from groups where groupid=" + groupid);
		Long gcreator = list3.get(0).getLong("gcreator");
		Long gOrigin = list3.get(0).getLong("gOrigin");
		record.set("gcreator", gcreator);//���Ĵ�����
		record.set("gOrigin", gOrigin);//�Ƿ��ǻ���
		//����̬��ʱ���Ƿ���ʾ��ά��Ŀ���
		int eventQRCodeCanPublish=new GroupCanPublish().getGroupPublishByType(groupid+"", "1");
		record.set("eventQRCodeCanPublish", eventQRCodeCanPublish);
		List<Record> newReturnList = new ArrayList<Record>();
		newReturnList.add(record);
		jsonString = jsonData.getSuccessJson(newReturnList);
		renderText(jsonString);
	}

	/**
	 * ��ʾ��̬�����б�
	 */
	public void ShowEventLikeList() {
		String eid = this.getPara("eid");
		String type = this.getPara("type");
		String likeID = this.getPara("likeID");

		List<Record> list = new ArrayList<Record>();
		Like like = new Like();
		switch (type) {
		case "initialize":
			list = CacheKit.get("ConcurrencyCache", eid + "ShowEventLikeList");
			if(null==list||list.isEmpty()){
				list = like.InitializeEventLikeLike(eid);
				CacheKit.put("ConcurrencyCache", eid + "ShowEventLikeList", list);
			}
			break;
		case "loading":
			list = like.LoadingEventLikeLike(eid, likeID);
			break;
		}
		jsonString = jsonData.getSuccessJson(list);
		renderText(jsonString);
	}

	/*******************************
	 * <����Ƶ>��ؽӿ� Start
	 *******************************/

	/**
	 * �ϴ�����Ƶ
	 */
	@Before(CrossDomain.class)
	public void UploadShortVideo() {
		String userid = this.getPara("userid");
		String groupid = this.getPara("groupid");
		String address = this.getPara("address");
		String content = this.getPara("content");
		String storage = this.getPara("storage");
		String place = this.getPara("place");
		String cover = this.getPara("cover");
		String time = this.getPara("time");
		String source = this.getPara("source");

		// �жϴ洢�ռ��Ƿ��д�
		double storagePlace = ((storage == null || storage.equals("")) ? 0.00 : Double.parseDouble(storage));
		cover = (cover == null ? "" : cover);
		time = (time == null ? "0" : time);

		// ��Դ��ַ��ǰ׺
		address = CommonParam.qiniuPrivateAddress + address;
		// ��Ƶ����,��Ƶ����ͼƬ����trueΪɫ����Ƶ
		boolean videoJudge = dataProcess.VideoVerify(address);
		boolean coverJudge = false;
		if(!cover.equals(""))
		 coverJudge = dataProcess.SinglePictureVerify(cover);
		
		if (videoJudge || coverJudge) {
			jsonString = jsonData.getJson(1039, "��ԴΥ��");
		} else {
			// ֧��ͬʱ�ϴ�������ռ�
			String[] IDs = groupid.split(",");
			boolean flag = true;
			int eventID = 0;
			// ����ռ��ϴ�
			for (int i = 0; i < IDs.length; i++) {
				// ͬ�����,0--��ͬ�� 1--ͬ�� ,��һ���ռ�Ϊԭ��������Ϊͬ��
				int isSynchronize = (i == 0 ? 0 : 1);
				// �ϴ�����Ƶ
				int eid = TxService.uploadShortVedio(userid, IDs[i], address, content, storagePlace, place, cover, time,
						isSynchronize, source);
				eventID = eid;
				if (eid == 0) {
					flag = false;
					break;
				}
			}
			if (flag) {
				// ˵���ϴ��ɹ�
				List<Record> result = service.getSingleEvent(eventID, source);// ��ȡ��̬����Ϣ
				jsonString = jsonData.getSuccessJson(result);
			} else {
				jsonString = jsonData.getJson(-50, "���ݲ��뵽���ݿ�ʧ��");
			}
		}

		// ���ؽ��
		renderText(jsonString);
	}

	/**
	 * ��ʾ����Ƶǽ
	 */
	@Before(CrossDomain.class)
	public void ShowShortVideoWall() {
		String userid = this.getPara("userid");
		String groupid = this.getPara("groupid");
		String pid = this.getPara("pid");
		String source = this.getPara("source");

		// ��ԴΪweb����Ҫ��groupid����
		if (source != null && source.equals("web")) {
			groupid = dataProcess.decryptData(groupid, "groupid");
		}

		List<Record> videoList = service.getShortVideoWallContent(userid, groupid, pid);
		jsonString = jsonData.getSuccessJson(videoList);
		renderText(jsonString);

	}

	/******************************* <����Ƶ>��ؽӿ� End *******************************/

	/*******************************
	 * <������>��ؽӿ� Start
	 *******************************/

	/**
	 * �ϴ�������
	 */
	public void UploadScanEvent() {
		// ˭�����ĸ��ռ���
		String userid = this.getPara("userid");
		String groupid = this.getPara("groupid");
		// ͼƬ
		String picAddress = this.getPara("picAddress");
		// �ȶ�ͼƬ
		String verifyPicAddress = this.getPara("verifyPicAddress");
		// ����
		String content = this.getPara("content");
		// ����
		String audio = this.getPara("audio");
		// ���Ԫ��
		String totalNum = this.getPara("totalNum");
		String totalMoney = this.getPara("totalMoney");
		// ����Ԫ��
		String storage = this.getPara("storage");
		String isPush = this.getPara("isPush"); // �����ж�
		// �жϽӿ���Դ
		String source = this.getPara("source");

		// �жϴ洢�ռ��Ƿ��д�
		double storagePlace;
		if (storage == null || storage.equals("")) {
			storagePlace = 0.00;
		} else {
			storagePlace = Double.parseDouble(storage);
		}

		// �жϺ���Ƿ��а���
		double money;
		int num;
		if (totalNum == null || totalNum.equals("") || totalMoney == null || totalMoney.equals("")) {
			money = 0.00;
			num = 0;
		} else {
			money = Double.parseDouble(totalMoney);
			num = Integer.parseInt(totalNum);
		}

		// �ж�����Ƿ��㹻
		User user = new User();
		boolean judge = user.JudgeUserBalanceIsEnough(userid, new BigDecimal(String.valueOf(money)));

		if (judge) {
			// �ϴ���̬
			int eid = TxService.uploadScanEvent(userid, groupid, picAddress, verifyPicAddress, content, audio,
					storagePlace, isPush, num, money, source);
			if (eid != 0) {
				// ˵���ϴ��ɹ�
				List<Record> result = service.getSingleEvent(eid, source);// ��ȡ��̬����Ϣ
				jsonString = jsonData.getSuccessJson(result);
			} else {
				jsonString = jsonData.getJson(1042, "�˻�����");

			}
		} else {
			jsonString = jsonData.getJson(1042, "�˻�����");
		}

		// ���ؽ��
		renderText(jsonString);

	}

	/**
	 * ɨ���ȡ��̬
	 */
	public void ScanForEvent() {

		UploadFile uploadFile = this.getFile("uploadFile");
		String groupid = this.getPara("groupid");

		List<Record> resultList = new ArrayList<Record>();

		// ��ȡ�ռ��ڵ�����������
		Group group = new Group();
		List<Record> scanEventsList = group.getAllScanEventsInSpace(groupid);

		if (scanEventsList.size() == 0) {
			jsonString = jsonData.getJson(1039, "�ռ���û��������");
		} else {
			File comparePic = uploadFile.getFile();
			if (comparePic == null) {
				jsonString = jsonData.getJson(1, "�������ȱʧ");
			} else {
				// ����ȶԣ����ƶȴ���86%�򷵻�
				for (Record record : scanEventsList) {
					// ��ȡ�ȶ�ͼƬ��ַ����Ȩ
					String verifyPic = qiniu.getDownloadToken(record.getStr("eVerifyPic"));
					// ����ͼƬ��ת���ļ�
					try {
						URL picUrl = new URL(verifyPic);
						BufferedImage compareImg = ImageIO.read(comparePic);
						BufferedImage sourceImg = ImageIO.read(picUrl);

						// ִ�жԱ�
						Double compareResult = PHash.calculateSimilarity(PHash.getFeatureValue(compareImg),
								PHash.getFeatureValue(sourceImg));

						// ������ض�̬
						if (compareResult >= 0.86) {
							int eid = Integer.parseInt(record.get("eid").toString());
							List<Record> temp = service.getSingleEvent(eid, "app");
							if (temp != null)
								resultList.add(temp.get(0));
						}

					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						continue;
					}
				}
				jsonString = jsonData.getSuccessJson(resultList);
			}

		}
		renderText(jsonString);

	}

	/******************************* <������>��ؽӿ� End *******************************/

	/******************************* <ʱ��>��ؽӿ� Start *****************************/

	/**
	 * ��ʾʱ��
	 */
	public void ShowMoments() {
		System.out.println("ShowMoments��ʼ��"+System.currentTimeMillis());
		String userid = this.getPara("userid");
		String type = this.getPara("type");
		String eid = this.getPara("eid");
		if(userid==null||userid.equals("")||userid.equals("undefined")||userid.equals("NaN")||eid==null||eid.equals("")||eid.equals("undefined")||eid.equals("NaN")){
			jsonString=jsonData.getJson(2, "��������",new ArrayList<Record>());
			renderText(jsonString);
			return;
		}
		List<Record> result = service.GetMoments(userid, type, eid);
		jsonString = jsonData.getSuccessJson(result);
		renderText(jsonString);
		System.out.println("ShowMoments������"+System.currentTimeMillis());
	}

	/******************************* <ʱ��>��ؽӿ� End *******************************/
	/******************************* <ʱ��>��ؽӿ� Start *****************************/

	/**
	 * ��ʾʱ�� by lk �򻯰汾
	 */
	public void ShowMoments_sim() {
		System.out.println("ShowMoments��ʼ��"+System.currentTimeMillis());
		String userid = this.getPara("userid");
		String type = this.getPara("type");
		String eid = this.getPara("eid");
		if(userid==null||userid.equals("")||userid.equals("undefined")||userid.equals("NaN")||eid==null||eid.equals("")||eid.equals("undefined")||eid.equals("NaN")){
			jsonString=jsonData.getJson(2, "��������",new ArrayList<Record>());
			renderText(jsonString);
			return;
		}
		List<Record> result = service.GetMoments_sim(userid, type, eid);
		jsonString = jsonData.getSuccessJson(result);
		renderText(jsonString);
		System.out.println("ShowMoments������"+System.currentTimeMillis());
	}
	/**
	 * ��ʾʱ�� by lk �򻯰汾-����ʾ��������
	 */
	public void ShowMoments_sim_noGtype() {
		System.out.println("ShowMoments��ʼ��"+System.currentTimeMillis());
		String userid = this.getPara("userid");
		String type = this.getPara("type");
		String eid = this.getPara("eid");
		if(userid==null||userid.equals("")||userid.equals("undefined")||userid.equals("NaN")||eid==null||eid.equals("")||eid.equals("undefined")||eid.equals("NaN")){
			jsonString=jsonData.getJson(2, "��������",new ArrayList<Record>());
			renderText(jsonString);
			return;
		}
		//List<Record> result = service.GetMoments_sim(userid, type, eid);
		List<Record> result = service.GetMoments_sim_noGtype(userid, type, eid);
		jsonString = jsonData.getSuccessJson(result);
		renderText(jsonString);
		System.out.println("ShowMoments������"+System.currentTimeMillis());
	}

	/******************************* <ʱ��>��ؽӿ� End *******************************/
	
	/**
	 * ����̬��ά��--ly
	 */
	public void shareEventQRCode(){
		QiniuOperate operate=new QiniuOperate();
		SmallAppQRCode small = new SmallAppQRCode();
		String groupid = this.getPara("groupid");
		String userid = this.getPara("userid");
		String eventid = this.getPara("eventid");
		if(eventid!=null&&!eventid.equals("")){
			Event e=new Event().findById(eventid);
			User u = new User().findById(userid);
			Group g = new Group().findById(groupid);
			String gname = g.get("gname");
			String eQRCode = e.get("eQRCode");
			String url = "";
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
				
				url = small.GetShareSmallAppQRCodeURL("eventdetail2", eventid,gname,picUrl,mpicUrl);
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
	/**
	 * ���۷�ҳ����  EventController

	 */
	public void GetCommentByPaged() {
		String eid = this.getPara("eid");
		String type = this.getPara("type");
		String cid = this.getPara("cid");
		
		List<Record> result = new ArrayList<>();
		if(eid != null && !eid.equals("") && null!=cid && !cid.equals("")) {
			result = new Comment().GetEventComment(eid,cid,type);
		}
		
		//��װ��̬����������û�����
		result = dataProcess.encapsulationCommentList(result);
		jsonString = jsonData.getSuccessJson(result);
		renderText(jsonString);
	}
}
