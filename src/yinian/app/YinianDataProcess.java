package yinian.app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import yinian.common.CommonParam;
import yinian.model.Event;
import yinian.model.Grab;
import yinian.model.Group;
import yinian.model.RedEnvelop;
import yinian.model.User;
import yinian.thread.VerifyPicture;
import yinian.thread.VerifyPictureNew;
import yinian.utils.DES;
import yinian.utils.JsonData;
import yinian.utils.QiniuOperate;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jfinal.log.Logger;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;

public class YinianDataProcess {

	private static final Logger log = Logger.getLogger(YinianDataProcess.class);
	private static QiniuOperate operate = new QiniuOperate();

	/**
	 * �¼������۽����
	 * 
	 * @param event
	 * @param comment
	 * @return
	 */
	public List<Record> combieEventAndComment(List<Record> event, List<Record> comment) {
		for (Record EventRecord : event) {
			List<Record> AddList = new ArrayList<Record>();
			for (Record CommentRecord : comment) {
				if (EventRecord.get("eid").equals(CommentRecord.get("ceid"))) {
					AddList.add(CommentRecord);
				}
			}
			EventRecord.set("comments", AddList);
		}
		// ��ȡ��̬�鿴�û���Ϣ
		List<Record> view = Db
				.find("select userid,unickname,upic,veid from view,users where userid=vuserid and vstatus=0 ");
		for (Record EventRecord : event) {
			List<Record> AddList = new ArrayList<Record>();
			for (Record ViewRecord : view) {
				if (EventRecord.get("eid").equals(ViewRecord.get("veid"))) {
					AddList.add(ViewRecord);
				}
			}
			EventRecord.set("view", AddList);
		}
		return event;
	}

	/**
	 * �绰���봦�������
	 * 
	 * @param phonenumber
	 * @param phoneList
	 * @return
	 */
	public Record disposePhonenumber(String phonenumber, List<Record> phoneList) {
		String notifyPhone = ""; // ��Ҫ����֪ͨ�ĵ绰
		String messagePhone = ""; // ��Ҫ���Ͷ��ŵĵ绰
		// ���е绰�����ɸѡ�ʹ���
		String[] phoneArray = phonenumber.split(",");
		for (int i = 0; i < phoneArray.length; i++) {
			boolean flag = false;
			for (Record phone : phoneList) {
				if ((phoneArray[i]).equals(phone.get("uphone").toString())) {
					flag = true;
					break;
				}
			}
			if (flag) {
				notifyPhone += (phoneArray[i] + ",");
			} else {
				messagePhone += (phoneArray[i] + ",");
			}
		}
		// �ַ�������
		if (!notifyPhone.equals("")) {
			notifyPhone = notifyPhone.substring(0, notifyPhone.length() - 1);
		}
		if (!messagePhone.equals("")) {
			messagePhone = messagePhone.substring(0, messagePhone.length() - 1);
		}
		Record result = new Record().set("notifyPhone", notifyPhone).set("messagePhone", messagePhone);
		return result;
	}

	/**
	 * �����Ƿ���waits���в��Ҫ���Ͷ��ŵĺ���
	 * 
	 * @param messagePhone
	 * @param waitPhoneList
	 * @return
	 */
	public Record disposeMessagePhone(String messagePhone, String userid, List<Record> waitPhoneList) {
		String inWaitsPhone = ""; // ����waits���еĵ绰��Ϣ
		String notInWaitsPhone = ""; // ����waits���еĵ绰��Ϣ
		// ���е绰�����ɸѡ�ʹ���
		String[] phoneArray = messagePhone.split(",");
		for (int i = 0; i < phoneArray.length; i++) {
			boolean flag = false;
			for (Record phone : waitPhoneList) {
				String waitsPhone = phone.get("wphone").toString();
				String waitsUserid = phone.get("wsender").toString();
				if ((phoneArray[i]).equals(waitsPhone) && userid.equals(waitsUserid)) {
					flag = true;
					break;
				}
			}
			if (flag) {
				inWaitsPhone += (phoneArray[i] + ",");
			} else {
				notInWaitsPhone += (phoneArray[i] + ",");
			}
		}
		// �ַ�������
		if (!inWaitsPhone.equals("")) {
			inWaitsPhone = inWaitsPhone.substring(0, inWaitsPhone.length() - 1);
		}
		if (!notInWaitsPhone.equals("")) {
			notInWaitsPhone = notInWaitsPhone.substring(0, notInWaitsPhone.length() - 1);
		}
		Record result = new Record().set("inWaitsPhone", inWaitsPhone).set("notInWaitsPhone", notInWaitsPhone);
		return result;
	}

	/**
	 * ���ؽ����url�ֶ�ת�������ʽ�����࣬�ȱ�������ͼ�汾
	 * 
	 * @param pic
	 * @return
	 */
	public List<Record> ChangePicAsArray(List<Record> list) {
		QiniuOperate operate = new QiniuOperate();
		String pic = "";
		for (Record record : list) {
			pic = record.get("url");
			String[] picArray = null;
			String[] thumbnailArray = null;
			if (pic != null) {
				picArray = pic.split(",");
				thumbnailArray = new String[picArray.length];
				// ��ȡͼƬ����Ȩ��
				for (int i = 0; i < picArray.length; i++) {
					thumbnailArray[i] = operate.getDownloadToken(picArray[i] + "?imageView2/2/w/300");
					picArray[i] = operate.getDownloadToken(picArray[i]);

				}
			}
			// ��ȡ��Ƶ����Ȩ��
			String audio = record.get("eaudio");
			if (audio != null && !audio.equals("")) {
				audio = operate.getDownloadToken(audio);
			}
			record.set("url", picArray).set("eaudio", audio).set("thumbnail", thumbnailArray);
		}
		return list;
	}

	/**
	 * ���ؽ����url�ֶ�ת�������ʽ�����ֱ࣬�Ӳü��汾
	 * 
	 * @param pic
	 * @return
	 */
	public List<Record> ChangePicAsArrayDirectCutVersion(List<Record> list) {
		QiniuOperate operate = new QiniuOperate();
		String pic = "";
		for (Record record : list) {
			pic = record.get("url");
			String[] picArray = pic.split(",");
			String[] thumbnailArray = new String[picArray.length];
			// ��ȡͼƬ����Ȩ��
			if (picArray.length == 1) {
				// ֻ��һ��ͼʱ���ش�һ��
				thumbnailArray[0] = operate.getDownloadToken(picArray[0] + "?imageView2/1/w/600");
				picArray[0] = operate.getDownloadToken(picArray[0]);
			} else {
				// ��������ƴ�С
				for (int i = 0; i < picArray.length; i++) {
					thumbnailArray[i] = operate.getDownloadToken(picArray[i] + "?imageView2/1/w/200");
					picArray[i] = operate.getDownloadToken(picArray[i]);

				}
			}
			// ��ȡ��Ƶ����Ȩ��
			String audio = record.get("eaudio");
			if (audio != null && !audio.equals("")) {
				audio = operate.getDownloadToken(audio);
			}
			record.set("url", picArray).set("eaudio", audio).set("thumbnail", thumbnailArray);
		}
		return list;
	}
	/**
	 * ��Դ��Ȩ����ȡͼƬ����ͼ by lk ʱ��
	 * 
	 * @param list
	 * @return
	 */
	public List<Record> AuthorizeResourceAndGetThumbnailShowMoments(List<Record> list) {
		QiniuOperate operate = new QiniuOperate();

		for (Record record : list) {
			List<Record> picList = record.get("picList");
			String eMain=record.getLong("eMain").toString();
			
			// ��ȡͼƬ����Ȩ��
			if (picList.size() != 0) {
				if (picList.size() == 1) {
					String p=picList.get(0).get("poriginal").toString();
					//System.out.println("poriginal="+p);
					//��Ƶ����
					String sourcePcover=picList.get(0).get("pcover");
					picList.get(0).set("pcover", null!=picList.get(0).get("pcover")&&!picList.get(0).get("pcover").equals("")
							?picList.get(0).get("pcover"):getVideoCover(picList.get(0).get("poriginal").toString(),eMain));
					// ֻ��һ��ͼʱ���ش�һ��,����ͼ��Ȩ
					picList.get(0).set("thumbnail", operate
							.getDownloadToken(p + "?imageView2/2/w/250"));
					// �е�����ͼ��Ȩ
					picList.get(0).set("midThumbnail", operate
							.getDownloadToken(p + "?imageView2/2/w/1000"));
					// ԭͼ��Ȩ
					picList.get(0).set("poriginal",
							operate.getDownloadToken(p));
					int width=345;
					int height=345;
					if(eMain.equals("4")){
						picList.get(0).set("width",width);
						picList.get(0).set("height",height);
//						String result="";
//						if(null!=sourcePcover&&!sourcePcover.equals("")){
//							 result = sentHttpRequestToGetImageInfo(operate.getDownloadToken(sourcePcover + "?imageInfo"));
//							 try{
//								 net.sf.json.JSONObject js=net.sf.json.JSONObject.fromObject(result);
//								//{"size":590849,"format":"png","width":750,"height":1334,"colorModel":"nrgba"}						
//								 width=Integer.parseInt(js.get("width").toString());
//								 height=Integer.parseInt(js.get("height").toString());
//							 }catch(Exception e){
//								 e.printStackTrace();
//							 }finally{
//								 picList.get(0).set("width",width);
//								 picList.get(0).set("height",height);
//							 }
//						}else{
//							result = sentHttpRequestToGetImageInfo(operate.getDownloadToken(p + "?avinfo"));
//							 net.sf.json.JSONObject js=net.sf.json.JSONObject.fromObject(result);
//								//{"size":590849,"format":"png","width":750,"height":1334,"colorModel":"nrgba"}	
//							 if(null!=js.get("streams")&&!js.get("streams").equals("")){
//								 try{
//									 net.sf.json.JSONArray sjs=net.sf.json.JSONArray.fromObject(js.get("streams"));
//									 net.sf.json.JSONObject sjsValue=net.sf.json.JSONObject.fromObject(sjs.get(0));
//									 width=Integer.parseInt(js.get("width").toString());
//									 height=Integer.parseInt(js.get("height").toString());
//								 }catch(Exception e){
//									 e.printStackTrace();
//								 }finally{
//									 picList.get(0).set("width",width);
//									 picList.get(0).set("height",height);
//								 }
//							 }
//						}
//										
//						
					}
					if(eMain.equals("0")){
						String result = sentHttpRequestToGetImageInfo(operate.getDownloadToken(p + "?imageInfo"));	
						 try{
							 net.sf.json.JSONObject js=net.sf.json.JSONObject.fromObject(result);
							 //{"size":590849,"format":"png","width":750,"height":1334,"colorModel":"nrgba"}
							 width=Integer.parseInt(js.get("width").toString());
							 height=Integer.parseInt(js.get("height").toString());
							 if(null!=js.get("orientation")&&
									 (js.get("orientation").toString().equals("Left-bottom")
											 ||js.get("orientation").toString().equals("Right-bottom")
											 ||js.get("orientation").toString().equals("Right-top")||js.get("orientation").toString().equals("Left-top"))){
								 width=Integer.parseInt(js.get("height").toString());
								 height=Integer.parseInt(js.get("width").toString());
							 }
							
						 }catch(Exception e){
							 e.printStackTrace();
						 }finally{
							 picList.get(0).set("width",width);
							 picList.get(0).set("height",height);
						 }
					}
					
				} 
			}

			// ��ȡ��Ƶ����Ȩ��
			String audio = record.get("eaudio");
			if (audio != null && !audio.equals("")) {
				audio = operate.getDownloadToken(audio);
			}
			// ����λ��ͼƬ��Ȩ
			String placePic = record.get("ePlacePic");
			if (placePic != null && !placePic.equals("")) {
				placePic = operate.getDownloadToken(placePic);
			}
			// �����ñȶ�ͼƬ��Ȩ
			String verifyPic = record.get("eVerifyPic");
			if (verifyPic != null && !verifyPic.equals("")) {
				verifyPic = operate.getDownloadToken(verifyPic);
			}

			record.set("picList", picList).set("eaudio", audio).set("ePlacePic", placePic).set("eVerifyPic", verifyPic);
		}
		return list;
	}
	/**
	 * ��Դ��Ȩ����ȡͼƬ����ͼ
	 * 
	 * @param list
	 * @return
	 */
	public List<Record> AuthorizeResourceAndGetThumbnail(List<Record> list) {
		QiniuOperate operate = new QiniuOperate();

		for (Record record : list) {
			List<Record> picList = record.get("picList");
			String eMain=record.getLong("eMain").toString();
			
			// ��ȡͼƬ����Ȩ��
			if (picList.size() != 0) {
				if (picList.size() == 1) {
					//��Ƶ����
					picList.get(0).set("pcover", null!=picList.get(0).get("pcover")&&!picList.get(0).get("pcover").equals("")
							?picList.get(0).get("pcover"):getVideoCover(picList.get(0).get("poriginal").toString(),eMain));
					// ֻ��һ��ͼʱ���ش�һ��,����ͼ��Ȩ
					picList.get(0).set("thumbnail", operate
							.getDownloadToken(picList.get(0).get("poriginal").toString() + "?imageView2/2/w/600"));
					// �е�����ͼ��Ȩ
					picList.get(0).set("midThumbnail", operate
							.getDownloadToken(picList.get(0).get("poriginal").toString() + "?imageView2/2/w/1000"));
					// ԭͼ��Ȩ
					picList.get(0).set("poriginal",
							operate.getDownloadToken(picList.get(0).get("poriginal").toString()));
					
				} else if (picList.size() == 2||picList.size() == 4) {
					for (Record picRecord : picList) {
						//��Ƶ����
						picRecord.set("pcover", null!=picRecord.get("pcover")&&!picRecord.get("pcover").equals("")
								?picRecord.get("pcover"):getVideoCover(picRecord.get("poriginal").toString(),eMain));
						// ����ͼ��Ȩ
						picRecord.set("thumbnail", operate
								.getDownloadToken(picRecord.get("poriginal").toString() + "?imageView2/2/w/300"));
						// �е�����ͼ��Ȩ
						picRecord.set("midThumbnail", operate
								.getDownloadToken(picRecord.get("poriginal").toString() + "?imageView2/2/w/1000"));
						// ԭͼ��Ȩ
						picRecord.set("poriginal", operate.getDownloadToken(picRecord.get("poriginal").toString()));
						
					}
				} else{
					// ��������ƴ�С
					for (Record picRecord : picList) {
						//��Ƶ����
						picRecord.set("pcover", null!=picRecord.get("pcover")&&!picRecord.get("pcover").equals("")
								?picRecord.get("pcover"):getVideoCover(picRecord.get("poriginal").toString(),eMain));
						// ����ͼ��Ȩ
						picRecord.set("thumbnail", operate
								.getDownloadToken(picRecord.get("poriginal").toString() + "?imageView2/2/w/200"));
						// �е�����ͼ��Ȩ
						picRecord.set("midThumbnail", operate
								.getDownloadToken(picRecord.get("poriginal").toString() + "?imageView2/2/w/1000"));
						// ԭͼ��Ȩ
						picRecord.set("poriginal", operate.getDownloadToken(picRecord.get("poriginal").toString()));
						
					}
				}
			}

			// ��ȡ��Ƶ����Ȩ��
			String audio = record.get("eaudio");
			if (audio != null && !audio.equals("")) {
				audio = operate.getDownloadToken(audio);
			}
			// ����λ��ͼƬ��Ȩ
			String placePic = record.get("ePlacePic");
			if (placePic != null && !placePic.equals("")) {
				placePic = operate.getDownloadToken(placePic);
			}
			// �����ñȶ�ͼƬ��Ȩ
			String verifyPic = record.get("eVerifyPic");
			if (verifyPic != null && !verifyPic.equals("")) {
				verifyPic = operate.getDownloadToken(verifyPic);
			}

			record.set("picList", picList).set("eaudio", audio).set("ePlacePic", placePic).set("eVerifyPic", verifyPic);
		}
		return list;
	}
/**
 * BY LK 20171108 ��ȡ����Ƶ����
 */
	public String getVideoCover(String url,String type){
		String cover="";
		if(type.equals("4")){
			QiniuOperate operate = new QiniuOperate();
			cover=operate
			.getDownloadToken(url + "?vframe/jpg/offset/1/w/750");
		}
		return cover;
	}
	/**
	 * ������Դ��Ȩ
	 * 
	 * @param list
	 * @param field
	 * @return
	 */
	public List<Record> AuthorizeSingleResource(List<Record> list, String field) {

		QiniuOperate operate = new QiniuOperate();

		for (Record record : list) {
			record.set(field, operate.getDownloadToken(record.get(field).toString()));

		}
		return list;
	}

	/**
	 * ��ȡ��Ϣ���������������δ��������
	 * 
	 * @param CommentNum
	 * @param InviteNum
	 * @return
	 */
	public Record getUnreadMessageNum(Record CommentNum, Record InviteNum) {
		Record numRecord = new Record();
		numRecord.set("commentNum", CommentNum.get("number"));
		numRecord.set("inviteNum", InviteNum.get("number"));
		return numRecord;
	}

	/**
	 * ������List�ϲ���һ���Ĵ�����
	 * 
	 * @param list1
	 * @param list2
	 * @return
	 */
	public List<Record> combineTwoList(List<Record> list1, List<Record> list2) {
		for (Record record : list2) {
			list1.add(record);
		}
		return list1;
	}

	/**
	 * ����һ����ת��List<Record>
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	public List<Record> makeSingleParamToList(String key, Object value) {
		Record record = new Record();
		record.set(key, value);
		List<Record> list = new ArrayList<Record>();
		list.add(record);
		return list;
	}

	/**
	 * ��ȡ��ַ����
	 * 
	 * @param picAddress
	 * @return
	 */
	public String[] getPicAddress(String picAddress, String mode) {
		String[] picArray = picAddress.split(",");
		for (int i = 0; i < picArray.length; i++) {
			if ((picArray[i].substring(0, 7)).equals("http://")) {
				picArray[i] = picArray[i];
			} else {
				if (mode == null || mode.equals("")) {
					picArray[i] = CommonParam.qiniuOpenAddress + picArray[i];
				} else {
					picArray[i] = CommonParam.qiniuPrivateAddress + picArray[i];
				}

			}
		}
		return picArray;
	}

	/**
	 * ��װ�����б��е��û��ࣨ����װ�������뱻�����˵���Ϣ��
	 * 
	 * @param list
	 * @return
	 */
	public List<Record> encapsulationCommentList(List<Record> list) {
		for (Record record : list) {
			// �����û����󲢸�ֵ
			User commentUser = new User().set("userid", record.get("cuid"))
					.set("unickname", record.get("cunickname").toString()).set("upic", record.get("cpic").toString());
			User commentedUser = new User().set("userid", record.get("ruid"))
					.set("unickname", record.get("runickname").toString()).set("upic", record.get("rpic").toString());
			// �Ƴ���¼�ж���ļ�¼
			record.remove("cuid").remove("cunickname").remove("cpic").remove("ruid").remove("runickname")
					.remove("rpic");
			// ���û�������ӵ�record��
			record.set("commentUser", commentUser).set("commentedUser", commentedUser);
		}
		return list;
	}

	/**
	 * ��װ�¼��б��е��û��ࣨ����װ��������Ϣ��
	 * 
	 * @param list
	 * @return
	 */
	public List<Record> encapsulationEventList(List<Record> list) {
		for (Record record : list) {
			// �����û����󲢸�ֵ
			User user = new User().set("userid", record.get("userid"))
					.set("unickname", record.get("unickname").toString()).set("upic", record.get("upic").toString());
			// �Ƴ���¼�ж���ļ�¼
			record.remove("userid").remove("unickname").remove("upic");
			// ���û�������ӵ�record��
			record.set("publishUser", user);
		}
		return list;
	}

	/**
	 * ��װ��ʷ��¼�е��û��ࣨ����װ��Ϣ�����ߵĸ�����Ϣ��
	 * 
	 * @param list
	 * @return
	 */
	public List<Record> encapsulationChatMessagePublisher(List<Record> list) {
		for (Record record : list) {
			// �����û����󲢸�ֵ
			User user = new User().set("userid", record.get("chatFrom"))
					.set("unickname", record.get("unickname").toString()).set("upic", record.get("upic").toString());
			// �Ƴ���¼�ж���ļ�¼
			record.remove("unickname").remove("upic");
			// ���û�������ӵ�record��
			record.set("chatFrom", user);
		}
		return list;
	}

	/**
	 * ��װ�û���Ϣ
	 * 
	 * @param list
	 * @return
	 */
	public List<Record> encapsulationUserInfo(List<Record> list) {
		for (Record record : list) {
			// �����û����󲢸�ֵ
			Record temp = new Record().set("userid", record.get("userid"))
					.set("unickname", record.get("unickname").toString()).set("upic", record.get("upic").toString())
					.set("noteName", record.get("noteName"));
			// �Ƴ���¼�ж���ļ�¼
			record.remove("userid").remove("unickname").remove("upic").remove("noteName");
			// ���û�������ӵ�record��
			record.set("user", temp);
		}
		return list;
	}

	/**
	 * ������ת��������
	 * 
	 * @param list
	 * @return
	 */
	public List<Record> changeGroupTypeIntoWord(List<Record> list) {
		String type;
		for (Record record : list) {
			type = record.get("gtype").toString();
			switch (type) {
			case "0":
				record.set("gtype", "�������");
				break;
			case "1":
				record.set("gtype", "�������");
				break;
			case "2":
				record.set("gtype", "�������");
				break;
			case "3":
				record.set("gtype", "�������");
				break;
			case "4":
				record.set("gtype", "�������");
				break;
			case "5":
				record.set("gtype", "�ٷ����");
				break;
			default:
				break;
			}
		}
		return list;
	}
	
	public List<Record> changeGroupTypeIntoWordNew(List<Record> list) {
		String type;
		String groupid;
		for (Record record : list) {
			groupid = record.get("groupid").toString();
			Group group = new Group().findById(groupid);
			type = group.get("gtype").toString();
			String gname = group.get("gname").toString();
			switch (type) {
			case "0":
				record.set("gtype", "�������").set("gname", gname);
				break;
			case "1":
				record.set("gtype", "�������").set("gname", gname);
				break;
			case "2":
				record.set("gtype", "�������").set("gname", gname);
				break;
			case "3":
				record.set("gtype", "�������").set("gname", gname);
				break;
			case "4":
				record.set("gtype", "�������").set("gname", gname);
				break;
			case "5":
				record.set("gtype", "�ٷ����").set("gname", gname);
				break;
			default:
				break;
			}
		}
		return list;
	}

	/**
	 * ����״̬ת��������
	 * 
	 * @param list
	 * @return
	 */
	public List<Record> changeOrderStatusIntoWord(List<Record> list) {
		String type;
		for (Record record : list) {
			type = record.get("ebOrderStatus").toString();
			switch (type) {
			case "0":
				record.set("ebOrderStatus", "δ֧��");
				break;
			case "1":
				record.set("ebOrderStatus", "��֧��");
				break;
			case "2":
				record.set("ebOrderStatus", "������");
				break;
			case "3":
				record.set("ebOrderStatus", "�ѷ���");
				break;
			case "4":
				record.set("ebOrderStatus", "��ǩ��");
				break;
			case "5":
				record.set("ebOrderStatus", "��ȡ��");
				break;
			case "6":
				record.set("ebOrderStatus", "�ѹر�");
				break;
			case "7":
				record.set("ebOrderStatus", "��ɾ��");
			default:
				break;
			}
		}
		return list;
	}

	/**
	 * ������״̬ת����Ӧ������
	 * 
	 * @param groupType
	 * @return
	 */
	public int changeOrderStatusIntoNumber(String status) {
		int num;
		switch (status) {
		case "δ֧��":
			num = 0;
			break;
		case "��֧��":
			num = 1;
			break;
		case "������":
			num = 2;
			break;
		case "�ѷ���":
			num = 3;
			break;
		case "��ǩ��":
			num = 4;
			break;
		case "��ȡ��":
			num = 5;
			break;
		case "�ѹر�":
			num = 6;
			break;
		case "��ɾ��":
			num = 7;
			break;
		default:
			num = -1;
			break;
		}
		return num;
	}

	/**
	 * ��ȡ�������Ƶ�������
	 * 
	 * @param type
	 * @return
	 */
	public String getGroupType(String type) {
		String newType = "";
		switch (type) {
		case "0":
			newType += "�������";
			break;
		case "1":
			newType += "�������";
			break;
		case "2":
			newType += "�������";
			break;
		case "3":
			newType += "�������";
			break;
		case "4":
			newType += "�������";
			break;
		case "5":
			newType += "�ٷ����";
			break;
		default:
			break;
		}
		return newType;
	}

	/**
	 * ��������ת����Ӧ��ID
	 * 
	 * @param groupType
	 * @return
	 */
	public int changeGroupTypeWordIntoNumber(String groupType) {
		int gtype;
		switch (groupType) {
		case "�������":
			gtype = 0;
			break;
		case "�������":
			gtype = 1;
			break;
		case "�������":
			gtype = 2;
			break;
		case "�������":
			gtype = 3;
			break;
		case "�������":
			gtype = 4;
			break;
		case "�ٷ����":
			gtype = 5;
			break;
		default:
			gtype = -1;
			break;
		}
		return gtype;
	}

	/**
	 * ��ȡ������Ϣ���͵�����
	 * 
	 * @param type
	 * @return
	 */
	public String getChatType(String type) {
		String newType = "";
		switch (type) {
		case "0":
			newType += "�ı�";
			break;
		case "1":
			newType += "ͼƬ";
			break;
		case "2":
			newType += "����";
			break;
		case "3":
			newType += "λ��";
			break;
		case "4":
			newType += "��¼��Ƭ";
			break;
		case "5":
			newType += "ʱ������Ƭ";
			break;
		case "6":
			newType += "ʱ��ӡ��";
			break;
		}
		return newType;
	}

	/**
	 * ��������ת������Ӧ����
	 * 
	 * @param list
	 * @return
	 */
	public List<Record> changeChatTypeIntoWord(List<Record> list) {
		String type;
		for (Record record : list) {
			type = record.get("chatType").toString();
			switch (type) {
			case "0":
				record.set("chatType", "�ı�");
				break;
			case "1":
				record.set("chatType", "ͼƬ");
				break;
			case "2":
				record.set("chatType", "����");
				break;
			case "3":
				record.set("chatType", "λ��");
				break;
			case "4":
				record.set("chatType", "��¼��Ƭ");
				break;
			case "5":
				record.set("chatType", "ʱ������Ƭ");
				break;
			case "6":
				record.set("chatType", "ʱ��ӡ��");
				break;
			default:
				break;
			}
		}
		return list;
	}

	/**
	 * ��������Ϣ����ת����Ӧ������
	 * 
	 * @param chatType
	 * @return
	 */
	public int changeChatTypeIntoNumber(String chatType) {
		int num;
		switch (chatType) {
		case "�ı�":
			num = 0;
			break;
		case "ͼƬ":
			num = 1;
			break;
		case "����":
			num = 2;
			break;
		case "λ��":
			num = 3;
			break;
		case "��¼��Ƭ":
			num = 4;
			break;
		case "ʱ������Ƭ":
			num = 5;
			break;
		case "ʱ��ӡ��":
			num = 6;
			break;
		default:
			num = -1;
			break;
		}
		return num;
	}

	/**
	 * �Ż�ȯʹ���������ת������Ӧ����
	 * 
	 * @param list
	 * @return
	 */
	public List<Record> changeCouponStatusIntoWord(List<Record> list) {
		String type;
		for (Record record : list) {
			type = record.get("couponStatus").toString();
			switch (type) {
			case "0":
				record.set("couponStatus", "δʹ��");
				break;
			case "1":
				record.set("couponStatus", "��ʹ��");
				break;
			case "2":
				record.set("couponStatus", "�ѹ���");
				break;
			default:
				break;
			}
		}
		return list;
	}

	/**
	 * ��ȡ֪ͨ����
	 * 
	 * @param List
	 * @return
	 */
	public String getNotificationContent(Record record, String type) {

		String content = "";

		switch (type) {
		case "enter":
			content = "������Ȧ�ӡ�" + record.getStr("gname") + "��";
			break;
		case "delete":
			content = "�Ѿ���ɢ��Ȧ�ӡ�" + record.getStr("gname") + "��";
			break;
		case "system":
			content = "ϵͳ֪ͨ~";
			break;
		case "quit":
			content = "�˳���Ȧ�ӡ�" + record.getStr("gname") + "��";
			break;
		case "success":
			content = "���ѳɹ�����Ȧ�ӡ�" + record.getStr("gname") + "��";
			break;
		case "kick":
			content = "���ѱ��Ƴ�Ȧ�ӡ�" + record.getStr("gname") + "��";
			break;
		case "contribute":
			content = "�ڹٷ��ռ䡰" + record.getStr("gname") + "��Ͷ����~";
			break;
		case "refuseContribution":
			content = "���ڹٷ��ռ䡰" + record.getStr("gname") + "���е�Ͷ��û��ͨ�����";
			break;
		case "acceptContribution":
			content = "���ڹٷ��ռ䡰" + record.getStr("gname") + "���е�Ͷ��ͨ�����";
			break;
		default:
			break;
		}

		return content;
	}

	/**
	 * ���ݲ���flag���ز�����
	 * 
	 * @param flag
	 * @return
	 */
	public String insertFlagResult(boolean flag) {
		String jsonString = "";
		JsonData jsonData = new JsonData();
		if (flag) {
			jsonString = jsonData.getJson(0, "success");
		} else {
			jsonString = jsonData.getJson(-50, "��������ʧ��");
		}
		return jsonString;
	}

	/**
	 * ���ݸ���flag���ظ��½�� 1.1�汾
	 * 
	 * @param flag
	 * @return
	 */
	public String updateFlagResult(boolean flag) {
		String jsonString = "";
		JsonData jsonData = new JsonData();
		if (flag) {
			jsonString = jsonData.getJson(0, "success");
		} else {
			jsonString = jsonData.getJson(-51, "��������ʧ��");
		}
		return jsonString;
	}

	/**
	 * ����ɾ��flag���ظ��½��
	 * 
	 * @param flag
	 * @return
	 */
	public String deleteFlagResult(boolean flag) {
		String jsonString = "";
		JsonData jsonData = new JsonData();
		if (flag) {
			jsonString = jsonData.getJson(0, "success");
		} else {
			jsonString = jsonData.getJson(-52, "ɾ������ʧ��");
		}
		return jsonString;
	}

	/**
	 * ��list�е�����ƴ�ӳ�string������
	 * 
	 * @param list
	 * @param field
	 * @return
	 */
	public String changeListToString(List<Record> list, String field) {
		String result = "";
		if (list.isEmpty())
			return result;

		for (Record record : list) {
			result += record.get(field).toString();
			result += ",";
		}
		result = result.substring(0, result.length() - 1);
		return result;
	}

	/**
	 * �ж�����ʱ���ĸ��磬��һ��������ֵ�ȵڶ��������򷵻�true
	 * 
	 * @param time1
	 * @param time2
	 * @return
	 */
	public boolean compareTwoTime(String time1, String time2) {
		// �����Ƚ�������д��"yyyy-MM-dd"�Ϳ�����
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		// ���ַ�����ʽ��ʱ��ת��ΪDate���͵�ʱ��
		Date a = new Date();
		Date b = new Date();
		try {
			a = sdf.parse(time1);
			b = sdf.parse(time2);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Date���һ�����������a����b����true�����򷵻�false
		if (a.before(b))
			return true;
		else
			return false;
	}

	/**
	 * �����������ڵĲ�ֵ
	 */
	public int computeTheDifferenceOfTwoDate(String now, String plan) {
		Calendar ca = Calendar.getInstance();
		String year = String.valueOf(ca.get(Calendar.YEAR));
		now = (year + "-" + now);
		plan = (year + "-" + plan);
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		int different = -1;
		try {
			long to = df.parse(plan).getTime();
			long from = df.parse(now).getTime();
			different = (int) ((to - from) / (1000 * 60 * 60 * 24));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return different;
	}

	/**
	 * ����������е��û���Ϣ��װ�ɶ���
	 * 
	 * @param list
	 * @return
	 */
	public List<Record> changeUserInfoIntoObeject(List<Record> list) {
		for (Record record : list) {
			Long userid = record.get("maCreatorID");
			String nickname = record.get("unickname").toString();
			String upic = record.get("upic").toString();
			Record newRecord = new Record().set("maCreatorID", userid).set("unickname", nickname).set("upic", upic);
			record.remove("maCreatorID").remove("unickname").remove("upic");
			record.set("userInfo", newRecord);
		}
		return list;
	}

	/**
	 * ������̬�����ǩ
	 */
	public List<Record> combineEventWithTags(List<Record> eventList) {
		for (Record record : eventList) {
			String eid = record.get("eid").toString();
			List<Record> tagList = Db
					.find("select tagID,tagContent from tags where tagEventID=" + eid + " and tagStatus=0 ");
			record.set("tags", tagList);
		}
		return eventList;
	}

	/**
	 * ������̬�������
	 */
	public List<Record> combineEventWithLike(List<Record> eventList, String source) {
		for (Record record : eventList) {
			String eid = record.get("eid").toString();
			List<Record> likeList = Db.find(
					"select likeID,likeUserID,likeStatus,upic from `like`,users where userid=likeUserID and likeEventID="
							+ eid + " and likeStatus!=1 ");
			// app��ת���ɵ��ʣ�С�����ת��
			if (source != null && !source.equals("app")) {
				likeList = changeLikeStatusToWord(likeList);
			}
			record.set("like", likeList);
		}
		return eventList;
	}/**
	 * ����������
	 */
	public List<Record> combineEventWithLikeNum(List<Record> eventList, String source,int userid,String ownUserid) {
		for (Record record : eventList) {
			String eid = record.get("eid").toString();
			List<Record> likeList = Db.find(
					"select count(*) as likenum from `like` where likeEventID="+ eid);
			record.set("like", likeList.get(0).getLong("likenum"));
			if(ownUserid != null && !ownUserid.equals("")) {
				List<Record> likeStatus = Db.find(
						"select * from `like` where likeUserID="+ownUserid+ " and likeEventID="+ eid);
				if(likeStatus.size()!=0) {
					record.set("likeStatus", 1);
				}else {
					record.set("likeStatus", 0);
				}
			}
			
			
		}
		return eventList;
	}
	
	
	

	/**
	 * ������̬�Ϳ�Ƭ��ʽ
	 */
	public List<Record> combineEventWithCardStyle(List<Record> eventList) {
		for (Record record : eventList) {
			String cardstyle = record.get("ecardstyle").toString();
			Record cardstyleRecord = Db
					.findFirst("select csid,cspic,csurl from cardstyle where csid=" + cardstyle + " ");
			record.set("cardstyle", cardstyleRecord);
			record.remove("ecardstyle");
		}
		return eventList;
	}

	/**
	 * �����������ݻ����Ӧ����
	 */
	public String decryptData(String code, String type) {
		String data = "";
		// �����������ʧ�����ݡ����ո񻻳ɼӺ�
		code = code.replace(" ", "+");
		try {
			String result = DES.decryptDES(code, CommonParam.DESSecretKey);
			String[] array = result.split(",");
			for (int i = 0; i < array.length; i++) {
				if (((array[i].split("="))[0]).equals(type)) {
					data = (array[i].split("="))[1];
				}
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			log.error("����ʧ�ܣ�" + e.toString());
			e.printStackTrace();
		}
		return data;
	}

	/**
	 * ��ȡͼƬ����Ȩ��
	 */
	public List<Record> getPicAccess(List<Record> list, String type) {
		QiniuOperate operate = new QiniuOperate();
		for (Record record : list) {
			String url = "";
			String[] urlArray = record.get("url").toString().split(",");
			for (int i = 0; i < urlArray.length; i++) {
				urlArray[i] = operate.getDownloadToken(urlArray[i]);
				url += (urlArray[i] + ",");
			}
			if (!url.equals("")) {
				url = url.substring(0, url.length() - 1);
			}
			record.set("url", url);
		}

		return list;
	}

	/**
	 * ����������Դǰ׺����
	 */
	public String singleOpenResourcePrefix(String data) {
		if (data != null && !data.equals("")) {
			data = CommonParam.qiniuOpenAddress + data;
		}
		return data;
	}

	/**
	 * ����ͼƬ��ַ�ַ�������
	 */
	public String EBPicStringProcess(String picAddress) {
		String[] picArray = picAddress.split(",");
		String newUrl = "";
		for (int i = 0; i < picArray.length; i++) {
			if ((picArray[i].substring(0, 7)).equals("http://")) {
				newUrl += picArray[i] + ",";
			} else {
				newUrl += CommonParam.qiniuOpenAddress + picArray[i] + ",";
			}
		}
		newUrl = newUrl.substring(0, newUrl.length() - 1);
		return newUrl;
	}

	/**
	 * ��ȡ�б���ͼƬԭͼ������ͼ�ķ���Ȩ�ޣ��ȱ���
	 */
	public List<Record> GetOriginAndThumbnailAccess(List<Record> list, String field) {
		QiniuOperate qiniu = new QiniuOperate();

		for (Record record : list) {
			String url = record.get(field).toString();
			String thumbnail = url + "?imageView2/2/w/300";
			String midThumbnail = url + "?imageView2/2/w/500";
			url = qiniu.getDownloadToken(url);
			thumbnail = qiniu.getDownloadToken(thumbnail);
			midThumbnail = qiniu.getDownloadToken(midThumbnail);

			record.set(field, url).set("thumbnail", thumbnail).set("midThumbnail", midThumbnail);
		}
		return list;
	}

	/**
	 * ��ȡ�б���ͼƬԭͼ������ͼ�ķ���Ȩ�ޣ�ֱ�Ӳü�
	 */
	public List<Record> GetOriginAndThumbnailAccessWithDirectCut(List<Record> list, String field) {
		QiniuOperate qiniu = new QiniuOperate();
		for (Record record : list) {
			//by lk 20171108 ��Ƶ����
			String pcover="";
			if(null!=record.get("pcover")&&!record.get("pcover").equals("")){
				pcover=record.get("pcover");
			}else{				
				pcover=getVideoCover(record.get("url").toString(),record.getLong("eMain").toString());
			}
					
			record.set("pcover", pcover);
			//by lk 20171108 ��Ƶ���� end
			String url = record.get(field).toString();
			String thumbnail = url + "?imageView2/1/w/200";
			String midThumbnail = url + "?imageView2/1/w/500";
			url = qiniu.getDownloadToken(url);
			thumbnail = qiniu.getDownloadToken(thumbnail);
			midThumbnail = qiniu.getDownloadToken(midThumbnail);
			record.set(field, url).set("thumbnail", thumbnail).set("midThumbnail", midThumbnail);
		}
		return list;
	}
	/**
	 * ����ǰ��Ҫ��
	 */
	public List<Record> GetOriginAndThumbnailAccessWithDirectCutNew2(List<Record> list, String field) {
		QiniuOperate qiniu = new QiniuOperate();
		for (Record record : list) {
			//by lk 20171108 ��Ƶ����
			String pcover="";
			if(null!=record.get("pcover")&&!record.get("pcover").equals("")){
				pcover=record.get("pcover");
			}else{				
				pcover=getVideoCover(record.get("url").toString(),record.getLong("eMain").toString());
			}
			System.out.println(pcover);		
			record.set("pcover", pcover);
			//by lk 20171108 ��Ƶ���� end
			String url = record.get(field).toString();
			String thumbnail = url + "?imageView2/1/w/200";
			String midThumbnail = url + "?imageView2/1/w/500";
			url = qiniu.getDownloadToken(url);
			thumbnail = qiniu.getDownloadToken(thumbnail);
			midThumbnail = qiniu.getDownloadToken(midThumbnail);
			record.set("poriginal", url).set("thumbnail", thumbnail).set("midThumbnail", midThumbnail);
		}
		return list;
	}
	
	/**
	 * ��ȡ�б���ͼƬԭͼ������ͼ�ķ���Ȩ�ޣ�ֱ�Ӳü�
	 */
	public List<Record> GetOriginAndThumbnailAccessWithDirectCutNew(List<Record> list) {
		QiniuOperate qiniu = new QiniuOperate();
		for (Record record : list) {
			//by lk 20171108 ��Ƶ����
			String pcover="";
			if(null!=record.get("pcover")&&!record.get("pcover").equals("")){
				pcover=record.get("pcover");
			}else{				
				pcover=getVideoCover(record.get("url").toString(),record.get("pMain").toString());
			}
					
			record.set("pcover", pcover);
			//by lk 20171108 ��Ƶ���� end
			String url = record.get("url").toString();
			String thumbnail = url + "?imageView2/1/w/200";
			String midThumbnail = url + "?imageView2/1/w/500";
			url = qiniu.getDownloadToken(url);
			thumbnail = qiniu.getDownloadToken(thumbnail);
			midThumbnail = qiniu.getDownloadToken(midThumbnail);
			record.set("poriginal", url).set("thumbnail", thumbnail).set("midThumbnail", midThumbnail);
		}
		return list;
	}

	/**
	 * ��ȡ�б���ͼƬԭͼ������ͼ�ķ���Ȩ�ޣ�������ͼƬ��Ϣ���ȱ���
	 */
	public List<Record> GetOriginAndThumbnailAccessWithImageInfo(List<Record> list, String field) {
		QiniuOperate qiniu = new QiniuOperate();
		for (Record record : list) {
			String url = record.get("url").toString();
			// ��ȡͼƬ��Ϣ
			String imageInfo = sentHttpRequestToGetImageInfo(qiniu.getDownloadToken(url + "?imageInfo"));
			JSONObject jo = JSONObject.parseObject(imageInfo);
			int width = jo.getIntValue("width");
			int height = jo.getIntValue("height");
			// ��ȡͼƬ��Ȩ��ĵ�ַ������ͼ��ַ
			String thumbnail = url + "?imageView2/2/w/300";
			url = qiniu.getDownloadToken(url);
			thumbnail = qiniu.getDownloadToken(thumbnail);
			record.set("url", url).set("thumbnail", thumbnail).set("width", width).set("height", height);
		}
		return list;
	}

	/**
	 * ������������
	 * 
	 * @param url
	 * @param param
	 * @return
	 */
	public boolean sentNetworkRequest(String url, String value) {
		try {
			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
			con.setRequestMethod("POST");
			con.setRequestProperty("accept", "*/*");
			con.setDoOutput(true);
			con.setDoInput(true);
			PrintWriter out = new PrintWriter(con.getOutputStream());
			value = ("picaddress=" + value);
			out.print(value);
			out.flush();
			con.connect();

			InputStream input = con.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(input, "utf-8"));
			StringBuilder builder = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				builder.append(line).append("\n");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;

	}

	/**
	 * �����������󲢷��ؽ��
	 */
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
				builder.append(line).append("\n");
			}
			result = builder.toString();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}
	/**
	 * �����������󲢷��ؽ��
	 */
	public String getNetworkRequest(String url) {
		String result = "";

		try {
			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
			con.setRequestMethod("GET");
			con.setRequestProperty("accept", "*/*");
			con.setDoOutput(true);
			con.setDoInput(true);
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
		}
		return result;
	}

	/**
	 * ��ָ�� URL ����POST����������
	 * 
	 * @param url
	 *            ��������� URL
	 * @param param
	 *            ����������������Ӧ���� name1=value1&name2=value2 ����ʽ��
	 * @return ������Զ����Դ����Ӧ���
	 */
	public String sendPost(String url, String param, String type) {
		PrintWriter out = null;
		BufferedReader in = null;
		String result = "";
		try {
			URL realUrl = new URL(url);
			// �򿪺�URL֮�������
			URLConnection conn = realUrl.openConnection();
			// ����ͨ�õ���������
			conn.setRequestProperty("accept", "*/*");
			conn.setRequestProperty("connection", "Keep-Alive");
			conn.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
			if (type.equals("json")) {
				conn.setRequestProperty("Content-Type", "application/json");
			}
			// ����POST�������������������
			conn.setDoOutput(true);
			conn.setDoInput(true);
			// ��ȡURLConnection�����Ӧ�������
			out = new PrintWriter(conn.getOutputStream());
			// �����������
			out.print(param);
			// flush������Ļ���
			out.flush();
			// ����BufferedReader����������ȡURL����Ӧ
			in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String line;
			while ((line = in.readLine()) != null) {
				result += line;

			}

		} catch (Exception e) {
			System.out.println("���� POST ��������쳣��" + e);
			e.printStackTrace();
		}
		// ʹ��finally�����ر��������������
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
		return result;
	}

	/**
	 * ����ţ�Ʒ��������ȡͼƬ��Ϣ
	 * 
	 * @param url
	 * @return
	 */
	public String sentHttpRequestToGetImageInfo(String url) {

		String result = "";
		BufferedReader in = null;
		try {
			while (true) {
				URL obj = new URL(url);
				HttpURLConnection con = (HttpURLConnection) obj.openConnection();
				con.setRequestMethod("GET");
				con.setRequestProperty("accept", "*/*");
				con.setDoOutput(true);
				con.setDoInput(true);
				int code = con.getResponseCode();
				if (!(code == 500)) {
					// ϵͳû�з����������ȡ������Ϣ��������
					in = new BufferedReader(new InputStreamReader(con.getInputStream()));
					String line;
					while ((line = in.readLine()) != null) {
						result += line;
					}
					break;
				}
			}
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * ����������ת��������
	 */
	public String numberToChineseNumber(String temp) {
		// ��λ����
		String[] units = new String[] { "ʮ", "��", "ǧ", "��", "ʮ", "��", "ǧ", "��" };

		// ���Ĵ�д��������
		String[] numeric = new String[] { "��", "Ҽ", "��", "��", "��", "��", "½", "��", "��", "��" };
		String res = "";
		// ����һ������������
		for (int k = -1; temp.length() > 0; k++) {
			// �������һλ
			int j = Integer.parseInt(temp.substring(temp.length() - 1, temp.length()));
			String rtemp = numeric[j];

			// ��ֵ����0�Ҳ��Ǹ�λ ��������λ��������λ ��ȥȡ��λ
			if (j != 0 && k != -1 || k % 8 == 3 || k % 8 == 7) {
				rtemp += units[k % 8];
			}

			// ƴ��֮ǰ��ǰ��
			res = rtemp + res;

			// ȥ�����һλ
			temp = temp.substring(0, temp.length() - 1);
		}

		// ȥ����������������..
		while (res.endsWith(numeric[0])) {
			res = res.substring(0, res.lastIndexOf(numeric[0]));
		}

		// �������滻����
		while (res.indexOf(numeric[0] + numeric[0]) != -1) {
			res = res.replaceAll(numeric[0] + numeric[0], numeric[0]);
		}

		// �� ��+ĳ����λ �����Ĵ��滻�� �õ�λ ȥ����λǰ�����
		for (int m = 1; m < units.length; m++) {
			res = res.replaceAll(numeric[0] + units[m], units[m]);
		}

		return res;
	}

	/**
	 * byte[]תString
	 */
	public String convertByteArrayToString(byte[] param) {
		StringBuffer sbuf = new StringBuffer();
		for (int i = 0; i < param.length; i++) {
			sbuf.append(param[i]);
		}
		return (sbuf.toString());
	}

	/**
	 * �ı����״̬��Ϊ��ӦӢ��
	 */
	public List<Record> changeLikeStatusToWord(List<Record> list) {
		for (Record record : list) {
			String likeStatus = record.get("likeStatus").toString();
			String status = "";
			// ��type�ĳɶ�Ӧstatus
			switch (likeStatus) {
			case "0":
				status = "like";
				break;
			case "2":
				status = "happy";
				break;
			case "3":
				status = "sad";
				break;
			case "4":
				status = "mad";
				break;
			case "5":
				status = "surprise";
				break;
			case "1":
				status = "unlike";
				break;
			}
			record.set("likeStatus", status);
		}
		return list;
	}

	/**
	 * �ռ���Ϣ��ռ���Ƭ��ƴ��
	 * 
	 * @param spaceList
	 * @param photoList
	 * @return
	 */
	public List<Record> combineSpaceInfoWithPhotoNum(List<Record> spaceList, List<Record> photoList, int userid) {

		for (Record groupRecord : spaceList) {

			// ���ؽ�������Ӽ��ܺ��groupid
			String groupid = groupRecord.get("groupid").toString();
			String encodeGroupid = "";
			try {
				encodeGroupid = DES.encryptDES("groupid=" + groupid + ",userid=" + userid, CommonParam.DESSecretKey);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			groupRecord.set("encodeGroupid", encodeGroupid);

			// ����ͼƬ��������
			boolean flag = false;
			for (Record photoRecord : photoList) {
				if ((groupRecord.get("groupid").toString()).equals((photoRecord.get("groupid").toString()))) {
					groupRecord.set("gpicnum", photoRecord.get("gpicnum"));
					flag = true;
					break;
				}
			}
			if (!flag) {
				groupRecord.set("gpicnum", 0);
				if(groupRecord.get("groupid").toString().equals(CommonParam.pGroupId+"")
						||groupRecord.get("groupid").toString().equals(CommonParam.pGroupId2+"")
						||groupRecord.get("groupid").toString().equals(CommonParam.pGroupId3+"")
						||groupRecord.get("groupid").toString().equals(CommonParam.pGroupId4+"")){
					groupRecord.set("gpicnum", 500000);
				}
			}
		}
		return spaceList;
	}
	
	/**
	 * �ռ���Ϣ��ռ���Ƭ��ƴ��
	 * 
	 * @param spaceList
	 * @param photoList
	 * @return
	 */
	public List<Record> combineSpaceInfoWithPhotoNumNew(List<Record> spaceList, List<Record> photoList, int userid) {

		for (Record groupRecord : spaceList) {

			// ���ؽ�������Ӽ��ܺ��groupid
			String groupid = groupRecord.get("groupid").toString();
			String encodeGroupid = "";
			try {
				encodeGroupid = DES.encryptDES("groupid=" + groupid + ",userid=" + userid, CommonParam.DESSecretKey);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			groupRecord.set("encodeGroupid", encodeGroupid);

			// ����ͼƬ��������
			boolean flag = false;
			for (Record photoRecord : photoList) {
				if ((groupRecord.get("groupid").toString()).equals((photoRecord.get("groupid").toString()))) {
					groupRecord.set("gpicnum", photoRecord.get("gpicnum"));
					flag = true;
					break;
				}
			}
			if (!flag) {
				groupRecord.set("gpicnum", 0);
			}
		}
		return spaceList;
	}

	/**
	 * �ռ���Ϣ��ռ䶯̬��ƴ��
	 * 
	 * @param spaceList
	 * @param eventList
	 * @return
	 */
	public List<Record> combineSpaceInfoWithEventNum(List<Record> spaceList, List<Record> eventList) {

		for (Record groupRecord : spaceList) {
			boolean flag = false;
			for (Record eventRecord : eventList) {
				if ((groupRecord.get("groupid").toString()).equals((eventRecord.get("groupid").toString()))) {
					groupRecord.set("geventnum", eventRecord.get("eventnum"));
					flag = true;
					break;
				}
			}
			if (!flag) {
				groupRecord.set("geventnum", 0);
			}
		}
		return spaceList;
	}
	
	/**
	 * �ռ���Ϣ��ռ䶯̬��ƴ��
	 * 
	 * @param spaceList
	 * @param eventList
	 * @return
	 */
	public List<Record> combineSpaceInfoWithEventNumNew(List<Record> spaceList, List<Record> eventList) {

		for (Record groupRecord : spaceList) {
			boolean flag = false;
			for (Record eventRecord : eventList) {
				if ((groupRecord.get("groupid").toString()).equals((eventRecord.get("groupid").toString()))) {
					groupRecord.set("geventnum", eventRecord.get("eventnum"));
					flag = true;
					break;
				}
			}
			if (!flag) {
				groupRecord.set("geventnum", 0);
			}
		}
		return spaceList;
	}

	/**
	 * ��ȡ��̬�����Ϣ
	 * 
	 * @param list
	 * @return
	 */

	public List<Record> getEventRedEnvelopInfo(List<Record> list) {
		RedEnvelop re = new RedEnvelop();
		Grab grab = new Grab();

		for (Record record : list) {
			if (record.get("eMain").toString().equals("5")) {
				String eid = record.get("eid").toString();

				// ��������Ϣ
				List<Record> redEnvelopInfo = re.getEventEnvelopBasicInfo(eid);
				if (redEnvelopInfo.size() != 0) {
					String redEnvelopID = redEnvelopInfo.get(0).get("redEnvelopID").toString();
					List<Record> receiveInfo = grab.getRedEnvelopGrabInfo(redEnvelopID);

					// ��������
					record.set("redEnvelopInfo", redEnvelopInfo.get(0)).set("receiveInfo", receiveInfo);
				}

			}
		}

		return list;
	}
	/**
	 * ��̬���ݷ�װ by lk ʱ��
	 * 
	 * @param eventList
	 * @param commentList
	 * @return
	 */
	public List<Record> eventDataEncapsulationShowMoments(List<Record> eventList, List<Record> commentList, String source) {

		Event event = new Event();
		// ������̬����Ƭ
		for (Record record : eventList) {
			record = event.CombineEventAndPictureShowMoments(record);
		}
		// ��װ��̬�ڵ��û�����
		//eventList = encapsulationEventList(eventList);

		// ��װ��̬����������û�����
		//commentList = encapsulationCommentList(commentList);
		commentList = new ArrayList<Record>();

		// ƴ���¼�������
		List<Record> list = combieEventAndComment(eventList, commentList);

		// ��װ�¼��͵���
		//list = combineEventWithLike(list, source);

		// ��ȡ��̬�����Ϣ
		//list = getEventRedEnvelopInfo(list);

		// ��Դ��Ȩ������ȡͼƬ����ͼ
		//list = AuthorizeResourceAndGetThumbnail(list);
		list = AuthorizeResourceAndGetThumbnailShowMoments(list);
		// ��������
		return list;
	}
	/**
	 * ��̬���ݷ�װ
	 * 
	 * @param eventList
	 * @param commentList
	 * @return
	 */
	public List<Record> eventDataEncapsulation(List<Record> eventList, List<Record> commentList, String source) {

		Event event = new Event();
		// ������̬����Ƭ
		for (Record record : eventList) {
			record = event.CombineEventAndPicture(record);
		}
		// ��װ��̬�ڵ��û�����
		eventList = encapsulationEventList(eventList);

		// ��װ��̬����������û�����
		commentList = encapsulationCommentList(commentList);

		// ƴ���¼�������
		List<Record> list = combieEventAndComment(eventList, commentList);

		// ��װ�¼��͵���
		list = combineEventWithLike(list, source);

		// ��ȡ��̬�����Ϣ
		list = getEventRedEnvelopInfo(list);

		// ��Դ��Ȩ������ȡͼƬ����ͼ
		list = AuthorizeResourceAndGetThumbnail(list);

		// ��������
		return list;
	}
	
	/**
	 * ��̬���ݷ�װ
	 * 
	 * @param eventList
	 * @param commentList
	 * @return
	 */
	public List<Record> eventDataEncapsulationNew(List<Record> eventList,String source,int userid,String ownUserid) {

		Event event = new Event();
		// ������̬����Ƭ
		for (Record record : eventList) {
			record = event.CombineEventAndPictureNew(record);
		}
		// ��װ��̬�ڵ��û�����New
		//eventList = encapsulationEventList(eventList);
		// ��װ�¼��͵���
		eventList = combineEventWithLikeNum(eventList, source,userid,ownUserid);
		
		
		// ��Դ��Ȩ������ȡͼƬ����ͼ
		eventList = AuthorizeResourceAndGetThumbnail(eventList);

		// ��������
		return eventList;
	}


	/**
	 * �ռ�������ݷ�װ
	 * 
	 * @param list
	 * @param userid
	 * @return
	 */
	public List<Record> spaceDataEncapsulation(List<Record> list, int userid) {

		// ��ȡ����Ƭ��
		List<Record> photoList = Group.GetSpacePhotoNum(list);

		// ������Ƭ�����뵽����Ϣ��
		list = combineSpaceInfoWithPhotoNum(list, photoList, userid);

		// ��ȡ��ᶯ̬��
		List<Record> eventList = Group.GetSpaceEventNum(list);

		// ���鶯̬�����뵽����Ϣ��
		list = combineSpaceInfoWithEventNum(list, eventList);

		// ��������ת������Ӧ������
		list = changeGroupTypeIntoWord(list);

		return list;
	}
	
	/**
	 * �ռ�������ݷ�װ
	 * 
	 * @param list
	 * @param userid
	 * @return
	 */
	public List<Record> spaceDataEncapsulationNew(List<Record> list, int userid) {

		// ��ȡ����Ƭ��
		List<Record> photoList = Group.GetSpacePhotoNumNew(list);

		// ������Ƭ�����뵽����Ϣ��
		list = combineSpaceInfoWithPhotoNumNew(list, photoList, userid);

		// ��ȡ��ᶯ̬��
		List<Record> eventList = Group.GetSpaceEventNumNew(list);

		// ���鶯̬�����뵽����Ϣ��
		list = combineSpaceInfoWithEventNumNew(list, eventList);

		// ��������ת������Ӧ������
		list = changeGroupTypeIntoWordNew(list);

		return list;
	}
	/**
	 * ͼƬ����
	 */
	public String[] PictureVerify(String[] picArray) {
		long start = System.currentTimeMillis();
		// ����һ���̳߳�
		ExecutorService exec = Executors.newCachedThreadPool();

		// ��������߳�������ÿ5��ͼ�ļ���Ϊһ���߳�
		int length = (int) Math.ceil(picArray.length / 5.0);
		// ִ�м����߳�
		for (int i = 0; i < length; i++) {
			exec.execute(new VerifyPicture(picArray, i * 5, i * 5 + 4));
		}
		// �ر��̳߳�
		exec.shutdown();

		// �ж��̳߳�ִ������ټ���ִ��
		try {
			// awaitTermination����false����ʱ�����ѭ��������true���̳߳��е��߳�ִ��������߳�����ѭ������ִ�У�ÿ��1��ѭ��һ��
			while (!exec.awaitTermination(1, TimeUnit.SECONDS))
				;
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		long end = System.currentTimeMillis();
		System.out.println("RunTime:" + (end - start));

		// ɸѡͼƬ
		int count = 0;
		for (int i = 0; i < picArray.length; i++) {
			if (!picArray[i].equals(""))
				count++;
		}
		// �洢���˺��ͼƬ����
		String[] resultArray = new String[count];
		int index = 0;
		for (int i = 0; i < picArray.length; i++) {
			if (!picArray[i].equals("")) {
				resultArray[index] = picArray[i];
				index++;
			}
		}

		// String[] tempArray = new String[picArray.length];
		// int index = 0;
		// int count = 0;
		//
		// for (int i = 0; i < picArray.length; i++) {
		// String address = operate.getDownloadToken(picArray[i] + "?pulp");
		// // �������󲢻�ȡ���ؽ��
		// String result = sentNetworkRequest(address);
		// if (!result.equals("")) {
		// JSONObject jo = JSONObject.parseObject(result);
		// int code = jo.getIntValue("code");
		// if (code == 0) {
		// JSONObject temp = JSONObject.parseObject(jo.get("pulp")
		// .toString());
		// int label = temp.getIntValue("label");
		// if (label == 2) {
		// // ����ɫ����Ը�ͼƬ,��ӵ�ͼƬ��
		// tempArray[index] = picArray[i];
		// index++;
		// count++;
		// }
		// }
		// }
		// }
		//
		// String[] resultArray = new String[count];
		// for (int i = 0; i < resultArray.length; i++) {
		// resultArray[i] = tempArray[i];
		// }

		return resultArray;
	}
	/**
	 * ͼƬ���� by ylm
	 */
	public String[] PictureVerifyNew(String[] picArray) {
		long start = System.currentTimeMillis();
		// ����һ���̳߳�
		ExecutorService exec = Executors.newCachedThreadPool();
		
		// ��������߳�������ÿ5��ͼ�ļ���Ϊһ���߳�
		int length = (int) Math.ceil(picArray.length / 5.0);
		// ִ�м����߳�
		for (int i = 0; i < length; i++) {
			exec.execute(new VerifyPicture(picArray, i * 5, i * 5 + 4));
		}
		// �ر��̳߳�
		exec.shutdown();
		
		// �ж��̳߳�ִ������ټ���ִ��
		try {
			// awaitTermination����false����ʱ�����ѭ��������true���̳߳��е��߳�ִ��������߳�����ѭ������ִ�У�ÿ��1��ѭ��һ��
			while (!exec.awaitTermination(1, TimeUnit.SECONDS))
				;
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		long end = System.currentTimeMillis();
		System.out.println("RunTime:" + (end - start));
		
		return picArray;
	}

	/**
	 * ����ͼƬ����
	 */
	public boolean SinglePictureVerify(String address) {
		address = operate.getDownloadToken(address + "?nrop");
		// �������󲢻�ȡ���ؽ��
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
					// ɫ��ͼƬ
					return true;
				} else {
					return false;
				}
			}
		}
		return true;
	}
	/**
	 * ͼƬ���� by ly YinianDataProcess
	 */
	public String[] PictureVerifyByLy(String[] picArray) {
		long start = System.currentTimeMillis();
		// ����һ���̳߳�
		ExecutorService exec = Executors.newCachedThreadPool();
		
		// ��������߳�������ÿ5��ͼ�ļ���Ϊһ���߳�
		int length = (int) Math.ceil(picArray.length / 5.0);
		// ִ�м����߳�
		for (int i = 0; i < length; i++) {
			exec.execute(new VerifyPictureNew(picArray, i * 5, i * 5 + 4));
		}
		// �ر��̳߳�
		exec.shutdown();
		
		// �ж��̳߳�ִ������ټ���ִ��
		try {
			// awaitTermination����false����ʱ�����ѭ��������true���̳߳��е��߳�ִ��������߳�����ѭ������ִ�У�ÿ��1��ѭ��һ��
			while (!exec.awaitTermination(1, TimeUnit.SECONDS))
				;
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		long end = System.currentTimeMillis();
		System.out.println("RunTime:" + (end - start));
		
		return picArray;
	}
	/**
	 * ��Ƶ����
	 */
	public boolean VideoVerify(String address) {
		// ���6�ţ�10���ӽ�ͼһ��
		String url = operate.getDownloadToken(address + "?tupu-video/nrop/f/6/s/10");
		//String result = sentNetworkRequest(url);
		String result = getNetworkRequest(url);		
		if (result.equals("")) {
			return false;
		}
		try {
			JSONObject jo = JSONObject.parseObject(result);
			int code = jo.getIntValue("code");
			if (code == 0) {
				int lable = jo.getIntValue("label");
				// 0--ɫ�� 1--�Ը� 2--����
				return lable == 2 ? false : true;
			} else {
				return true;
			}
		} catch (Exception e) {
			return false;
		}

	}
	/**
	 * ��̬���ݷ�װ by lk
	 * 
	 * @param eventList
	 * @param commentList
	 * @return
	 */
	public List<Record> eventDataEncapsulationByLk(List<Record> eventList, List<Record> commentList, String source) {

		Event event = new Event();
		// ������̬����Ƭ
		System.out.println("ʱ���� ������̬����Ƭ ��ʼ��"+System.currentTimeMillis());
//		for (Record record : eventList) {
//			record = event.CombineEventAndPicture(record);
//		}
		//by lk 
		StringBuffer eids=new StringBuffer();
		for(Record record:eventList){
			eids.append(record.get("eid").toString()).append(",");
		}
		List<Record> picList=new ArrayList<Record>();
		if(eids.length()>0){
			picList=event.CombineEventAndPictureByLk(eids.substring(0, eids.length()-1).toString());
		}
		Map<String,List<Record>> map=new HashMap<>();
		for(Record pic:picList){
			if(null!=map.get(pic.get("eid").toString())&&!map.get(pic.get("eid").toString()).isEmpty()){
				map.get(pic.get("eid").toString()).add(pic);
			}else{
				List<Record> list=new ArrayList<>();
				list.add(pic);
				map.put(pic.get("eid").toString(), list);
			}
		}
		for(Record record:eventList){
			if(null!=map.get(record.get("eid").toString())){
				record.set("picList", map.get(record.get("eid").toString()));
			}else{
				record.set("picList", new ArrayList<Record>());
			}
		}
		System.out.println("ʱ���� ������̬����Ƭ end��"+System.currentTimeMillis());
		System.out.println("ʱ���� ��װ��̬�ڵ��û����� ��ʼ��"+System.currentTimeMillis());
		// ��װ��̬�ڵ��û�����
		eventList = encapsulationEventList(eventList);
		System.out.println("ʱ���� ��װ��̬�ڵ��û����� end��"+System.currentTimeMillis());
		System.out.println("ʱ���� ��װ��̬����������û����� ��ʼ��"+System.currentTimeMillis());
		// ��װ��̬����������û�����
		commentList = encapsulationCommentList(commentList);
		System.out.println("ʱ���� ��װ��̬����������û����� end��"+System.currentTimeMillis());
		System.out.println("ʱ���� ƴ���¼������� ��ʼ��"+System.currentTimeMillis());
		// ƴ���¼�������
		List<Record> list = combieEventAndComment(eventList, commentList);
		System.out.println("ʱ���� ƴ���¼������� end��"+System.currentTimeMillis());
		System.out.println("ʱ���� ��װ�¼��͵��� ��ʼ��"+System.currentTimeMillis());
		// ��װ�¼��͵���
		list = combineEventWithLikeByLk(list, source);
		System.out.println("ʱ���� ��װ�¼��͵��� end��"+System.currentTimeMillis());
		System.out.println("ʱ���� ��ȡ��̬�����Ϣ ��ʼ��"+System.currentTimeMillis());
		// ��ȡ��̬�����Ϣ
		list = getEventRedEnvelopInfo(list);
		System.out.println("ʱ���� ��ȡ��̬�����Ϣ end��"+System.currentTimeMillis());
		System.out.println("ʱ���� ��Դ��Ȩ������ȡͼƬ����ͼ ��ʼ��"+System.currentTimeMillis());
		// ��Դ��Ȩ������ȡͼƬ����ͼ
		list = AuthorizeResourceAndGetThumbnail(list);
		System.out.println("ʱ���� ��Դ��Ȩ������ȡͼƬ����ͼ end��"+System.currentTimeMillis());
		// ��������
		return list;
	}
	//by lk ����10��������������������е����û�id
		public List<Record> combineEventWithLikeByLk(List<Record> eventList, String source) {
			for (Record record : eventList) {
				String eid = record.get("eid").toString();
				List<Record> likeList = Db.find(
						"select likeID,likeUserID,likeStatus,upic from `like`,users where userid=likeUserID and likeEventID="
								+ eid + " and likeStatus!=1 limit 10");
				// app��ת���ɵ��ʣ�С�����ת��
				if (source != null && !source.equals("app")) {
					likeList = changeLikeStatusToWord(likeList);
				}
				List<Record> likeUidList = Db.find(
						"select likeUserID from `like`,users where userid=likeUserID and likeEventID="
								+ eid + " and likeStatus!=1 ");
				record.set("like", likeList);
				record.set("likeCnt", likeUidList.size());
				record.set("likeUser", likeUidList);
			}
			return eventList;
		}	
}
