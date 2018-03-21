package yinian.controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.bouncycastle.util.encoders.Base64;

import com.alibaba.fastjson.JSONArray;
import com.jfinal.aop.Before;
import com.jfinal.core.Controller;
import com.jfinal.plugin.activerecord.ActiveRecordException;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;
import com.jfinal.plugin.ehcache.CacheKit;

import net.sf.json.JSONObject;
import yinian.app.YinianDataProcess;
import yinian.common.CommonParam;
import yinian.interceptor.CrossDomain;
import yinian.model.FormID;
import yinian.model.Group;
import yinian.model.GroupCanPublish;
import yinian.model.GroupMember;
import yinian.model.PartnerRecord;
import yinian.model.Points;
import yinian.model.User;
import yinian.service.ActivityService;
import yinian.service.NewH5Service;
import yinian.service.PointsService;
import yinian.service.YinianService;
import yinian.utils.AES;
import yinian.utils.DES;
import yinian.utils.HttpUtils;
import yinian.utils.JsonData;
import yinian.utils.MD5;

public class NewH5Controller extends Controller{
	private String jsonString; // ���ص�json�ַ���
	private JsonData jsonData = new JsonData(); // json������
	private YinianDataProcess dataProcess = new YinianDataProcess();// ���ݴ�����
	/**
	 * �������
	 */
	@Before(CrossDomain.class)
	public void CreateDefaultAlbum() {
		// ��ȡ����
		String userid = this.getPara("userid")==null?"":this.getPara("userid");
		String groupName = this.getPara("groupName");
		String groupType = this.getPara("groupType");
		String url = this.getPara("url");
		String source = this.getPara("source");
		String formID = this.getPara("formID")==null?"":this.getPara("formID"); // С�������ͱ�ID
		String isDefault=this.getPara("isDefault")==null?"0":this.getPara("isDefault");//0Ϊ��ͨ��ᣬ1ΪȺ���
		String groupNewType=this.getPara("groupNewType")==null||this.getPara("groupNewType").equals("")||this.getPara("groupNewType").equals("0")?"13":this.getPara("groupNewType");//0ΪĬ��
		//1.���� 2�ۻ� 3���� 4���� 5���� 6���� 7���� 8��Ȥ 9У԰ 10��˾ 11���� 12� 13���� 14����
		//String openGId=this.getPara("openGId");//0ΪĬ��
		if(!userid.equals("")&&!formID.equals("")){
			FormID.insert(userid, formID);
		}
		if (url == null || url.equals("")) {
			// ����������ȡ��Ӧ��Ⱥͷ��ͼƬ
			switch (groupNewType) {
			case "1":// ����
				List<Record> coverList = CacheKit.get("EternalCache", "spaceCover1");
				// ����Ϊ�գ����²�ѯ
				if (coverList == null) {
					coverList = Group.GetNewSpaceDefaultCoverList(11);
					CacheKit.put("EternalCache", "spaceCover1", coverList);
				}
				int size = coverList.size();
				url = coverList.get(new Random().nextInt(size)).getStr("acurl");
				break;
			case "2":// �ۻ�
				coverList = CacheKit.get("EternalCache", "spaceCover2");
				// ����Ϊ�գ����²�ѯ
				if (coverList == null) {
					coverList = Group.GetNewSpaceDefaultCoverList(12);
					CacheKit.put("EternalCache", "spaceCover2", coverList);
				}
				size = coverList.size();
				url = coverList.get(new Random().nextInt(size)).getStr("acurl");
				break;
			case "3":// ����
				coverList = CacheKit.get("EternalCache", "spaceCover3");
				// ����Ϊ�գ����²�ѯ
				if (coverList == null) {
					coverList = Group.GetNewSpaceDefaultCoverList(13);
					CacheKit.put("EternalCache", "spaceCover3", coverList);
				}
				size = coverList.size();
				url = coverList.get(new Random().nextInt(size)).getStr("acurl");
				break;
			case "4":// ����  ֻ��һ��				
				url = CommonParam.qiniuOpenAddress+"20180104-4-1.jpg";
				break;
			case "5":// ����
				coverList = CacheKit.get("EternalCache", "spaceCover5");
				// ����Ϊ�գ����²�ѯ
				if (coverList == null) {
					coverList = Group.GetNewSpaceDefaultCoverList(15);
					CacheKit.put("EternalCache", "spaceCover5", coverList);
				}
				size = coverList.size();
				url = coverList.get(new Random().nextInt(size)).getStr("acurl");
				break;
			case "6":// ����
				coverList = CacheKit.get("EternalCache", "spaceCover6");
				// ����Ϊ�գ����²�ѯ
				if (coverList == null) {
					coverList = Group.GetNewSpaceDefaultCoverList(16);
					CacheKit.put("EternalCache", "spaceCover6", coverList);
				}
				size = coverList.size();
				url = coverList.get(new Random().nextInt(size)).getStr("acurl");
				break;
			case "7":// ����
				coverList = CacheKit.get("EternalCache", "spaceCover7");
				// ����Ϊ�գ����²�ѯ
				if (coverList == null) {
					coverList = Group.GetNewSpaceDefaultCoverList(17);
					CacheKit.put("EternalCache", "spaceCover7", coverList);
				}
				size = coverList.size();
				url = coverList.get(new Random().nextInt(size)).getStr("acurl");
				break;
			case "8":// ��Ȥ
				coverList = CacheKit.get("EternalCache", "spaceCover8");
				// ����Ϊ�գ����²�ѯ
				if (coverList == null) {
					coverList = Group.GetNewSpaceDefaultCoverList(18);
					CacheKit.put("EternalCache", "spaceCover8", coverList);
				}
				size = coverList.size();
				url = coverList.get(new Random().nextInt(size)).getStr("acurl");
				break;
			case "9":// У԰
				coverList = CacheKit.get("EternalCache", "spaceCover9");
				// ����Ϊ�գ����²�ѯ
				if (coverList == null) {
					coverList = Group.GetNewSpaceDefaultCoverList(19);
					CacheKit.put("EternalCache", "spaceCover9", coverList);
				}
				size = coverList.size();
				url = coverList.get(new Random().nextInt(size)).getStr("acurl");
				break;
			case "10":// ��˾
				coverList = CacheKit.get("EternalCache", "spaceCover10");
				// ����Ϊ�գ����²�ѯ
				if (coverList == null) {
					coverList = Group.GetNewSpaceDefaultCoverList(20);
					CacheKit.put("EternalCache", "spaceCover10", coverList);
				}
				size = coverList.size();
				url = coverList.get(new Random().nextInt(size)).getStr("acurl");
				break;
			case "11":// ����
				coverList = CacheKit.get("EternalCache", "spaceCover11");
				// ����Ϊ�գ����²�ѯ
				if (coverList == null) {
					coverList = Group.GetNewSpaceDefaultCoverList(21);
					CacheKit.put("EternalCache", "spaceCover11", coverList);
				}
				size = coverList.size();
				url = coverList.get(new Random().nextInt(size)).getStr("acurl");
				break;
			case "12":// � ֻ��һ��
				url = CommonParam.qiniuOpenAddress+"20180104-12-1.jpg";
				break;
			case "13":// ����
				coverList = CacheKit.get("EternalCache", "spaceCover13");
				// ����Ϊ�գ����²�ѯ
				if (coverList == null) {
					coverList = Group.GetNewSpaceDefaultCoverList(23);
					CacheKit.put("EternalCache", "spaceCover13", coverList);
				}
				size = coverList.size();
				url = coverList.get(new Random().nextInt(size)).getStr("acurl");
				break;
			case "14":// ����
				coverList = CacheKit.get("EternalCache", "spaceCover14");
				// ����Ϊ�գ����²�ѯ
				if (coverList == null) {
					coverList = Group.GetNewSpaceDefaultCoverList(24);
					CacheKit.put("EternalCache", "spaceCover14", coverList);
				}
				size = coverList.size();
				url = coverList.get(new Random().nextInt(size)).getStr("acurl");
				break;
			default:
				coverList = CacheKit.get("EternalCache", "spaceCover13");
				// ����Ϊ�գ����²�ѯ
				if (coverList == null) {
					coverList = Group.GetNewSpaceDefaultCoverList(23);
					CacheKit.put("EternalCache", "spaceCover13", coverList);
				}
				size = coverList.size();
				url = coverList.get(new Random().nextInt(size)).getStr("acurl");
				break;
			}
		} else {
			String temp = url.substring(0, 7);
			if (!(temp.equals("http://"))) {
				url = CommonParam.qiniuOpenAddress + url;
			}

		}
		String inviteCode = Group.CreateSpaceInviteCode();
		NewH5Service service=new NewH5Service();
		jsonString = service.createAlbum(groupName, userid, url, groupType, inviteCode, source,isDefault,groupNewType,null);

		// ���ؽ��
		renderText(jsonString);
	}
	/*
	 * ֽ΢������get����
	 */
	public void postToZW(String zj,String userid,String uwechatid){
		//Long ts=System.currentTimeMillis();
		String url="https://java.zk2013.com/kedie2017/AdvApi/WxSmallProg/FreeUpCoin.do";
		String params="scene="+zj+"&thirdopenid="+uwechatid;
		String sign=MD5.getMD5(params+"&3ecb837979debb4b2b92a0a250ed8f8a").toUpperCase();
		//System.out.println("params=="+params);
		//System.out.println("url=="+params+"&Sign="+sign);
				//&Sign=9B9959C1C28DA87112A520B5588B6986";
		HttpUtils u=new HttpUtils();		
		savePartnerRecord(userid, zj, u.sendGet(url,params+"&Sign="+sign), "ֽ΢");
		//{"status":-1,"status_str":"�ѷ��Ž���","api_data":[]}
	}
	/*
	 * �ڴ����������get����
	 */
	public void postToKD(String zj,String userid){
		Long ts=System.currentTimeMillis();
		String url="http://www.pocketuniversity.cn/signin/Sxcard/qrNotive?qrcode="+zj;		
		
		HttpUtils u=new HttpUtils();
		u.sendGet(url,null);
		//savePartnerRecord(userid, zj, u.sendGet(url,null), "�ڴ�");
		//{"status":-1,"status_str":"�ѷ��Ž���","api_data":[]}
	}
	/*
	 * ����ֽ��post-��һ�κ�����ɣ������к����������ȱ���
	 */
	public void postToZJ(String zj,String userid){
		Long ts=System.currentTimeMillis();
		String url="http://www.zhenhuaonline.cn/api/in/thirdparty/op_app/v1/handle?app_id=a_qLCogTZJjJbMU6&timestamp="+ts+"&sign=";		
		String param1 = getMD5("api=User.Tissue.Order.TicketOrdertimestamp="+ts+"tissue_ticket="+zj+"bjSjRNJmXvBE16faKxo3caLcFjGSZ5bV");
		url+=param1;
		param1="api=User.Tissue.Order.TicketOrder&timestamp="+ts+"&tissue_ticket="+zj;
		HttpUtils u=new HttpUtils();
		savePartnerRecord(userid, zj, u.sendPost(url, param1), "zho");
		//{"status":-1,"status_str":"�ѷ��Ž���","api_data":[]}
	}
	/*���������¼*/
	public void savePartnerRecord(String userid,String postValue,String getValue,String partner){
		PartnerRecord pr=new PartnerRecord();
		pr.set("userid", userid).set("postValue", postValue).set("getValue", getValue).set("partner", partner);
		pr.save();
	}
	 /** 
     * ����md5 
     *  
     * @param message 
     * @return 
     */  
    public static String getMD5(String message) {  
    String md5str = "";  
    try {  
        // 1 ����һ���ṩ��ϢժҪ�㷨�Ķ��󣬳�ʼ��Ϊmd5�㷨����  
        MessageDigest md = MessageDigest.getInstance("MD5");  
  
        // 2 ����Ϣ���byte����  
        byte[] input = message.getBytes();  
  
        // 3 ��������ֽ�����,�������128λ��  
        byte[] buff = md.digest(input);  
  
        // 4 ������ÿһ�ֽڣ�һ���ֽ�ռ��λ������16��������md5�ַ���  
        md5str = bytesToHex(buff);
        md5str = md5str.toLowerCase();
        System.out.println(md5str);
  
    } catch (Exception e) {  
        e.printStackTrace();  
    }  
    return md5str;  
    }  
    /** 
     * ������תʮ������ 
     *  
     * @param bytes 
     * @return 
     */  
    public static String bytesToHex(byte[] bytes) {  
    StringBuffer md5str = new StringBuffer();  
    // ������ÿһ�ֽڻ���16��������md5�ַ���  
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
	/**
	 * ��ʾС���������Ϣ
	 */
    //��ʾС���������Ϣ--����
    @Before(CrossDomain.class)
	public void ShowSmallAppAlbumInformationWithEncryption() {
		String uid=this.getPara("userid");
		String gid=this.getPara("groupid");
		String zj=this.getPara("zj");
		String inviteuserid=this.getPara("inviteuserid");
		if(uid==null||gid==null||gid.equals("undefined")||gid.equals("NaN")){
			jsonString=jsonData.getJson(2, "��������",new ArrayList<Record>());
			renderText(jsonString);
			return;
		}
		//��ʼ����
		boolean canDES=false;
		try{			
			uid=URLDecoder.decode(uid);
			uid=DES.decryptDES(uid, CommonParam.DESSecretKey);
			gid=URLDecoder.decode(gid);
			gid=DES.decryptDES(gid, CommonParam.DESSecretKey);
			canDES=true;
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			if(!canDES){
				jsonString=jsonData.getJson(2, "��������",new ArrayList<Record>());
				renderText(jsonString);
				return;
			}
			//System.out.println(a1);
		}
		//����end
		User inviteuser=new User();
		if(null!=inviteuserid&&!inviteuserid.equals("0")&&!inviteuserid.equals("undefined")&&!inviteuserid.equals("NaN")&&!inviteuserid.equals("")){
			inviteuser=new User().findById(inviteuserid);
		}
		//������
		User u=new User().findById(uid);
		int inBlackList=1;
		if(null!=u&&null!=u.get("ustate")&&u.get("ustate").toString().equals("1")){
			inBlackList=0;
		}
		//������
		// ��������
		int userid = Integer.parseInt(uid);
		int groupid = Integer.parseInt(gid);
		// �û���Դ����
		String port = this.getPara("port");
		String fromUserID = this.getPara("fromUserID");
		
		Group group = new Group().findById(groupid);
		int eventQRCodeCanPublish=new GroupCanPublish().getGroupPublishByType(groupid+"", "1");
		//dialogShow=1 ��ʾ��������
		int dialogShow=new GroupCanPublish().getGroupPublishByType(groupid+"", "2");
		//showAdvertisements=1 ��ʾ���λ
		//int advertisementsShow=new GroupCanPublish().getGroupPublishByType(groupid+"", "3");
		/*
		 * 1000W��Ƭ�
		 */
		int advertisementsShow=0;
		if(CommonParam.pOpenBanner){
			
			advertisementsShow=1;
			if(groupid==CommonParam.pGroupId7){
				YinianService service1 = new YinianService();
				service1.SetGroupIsTop(userid+"", CommonParam.pGroupId7+"", "yes");
//				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//				String time = sdf.format(new Date());
//				Db.update("update groupmembers set isTop=1 and topTime='"+time+"' where gmuserid="+userid);
				advertisementsShow=0;
			}
		}else{
			advertisementsShow=new GroupCanPublish().getGroupPublishByType(groupid+"", "3");
		}
		/*
		 * 1000W��Ƭ� end
		 */
		int status = Integer.parseInt(group.get("gstatus").toString());
		int gtype = Integer.parseInt(group.get("gtype").toString());
		int gAuthority = Integer.parseInt(group.get("gAuthority").toString());
		Record record = new Record();
		String upic = u.get("upic").toString();//�û�ͷ��
		record.set("upic", upic);
		//�ڴ�����
		if(null!=zj&&!zj.toString().equals("")&&zj.length()<4&&groupid==5193566){
			postToKD(zj,userid+"");
		}
		//ֽ΢����
		if(null!=zj&&!zj.toString().equals("")&&groupid==5275402){
			postToZW(zj,userid+"",u.get("uwechatid"));
		}
		//zho
		if(null!=zj&&!zj.toString().equals("")&&groupid==5298806){
			postToZJ(zj,userid+"");
		}
		//�ѱ�
		if(null!=zj&&!zj.toString().equals("")&&groupid==5451186){
			postToYB(zj,u.get("uwechatid").toString());
		}
		// �ж�����Ƿ�ɾ��
		if (status == 0) {
			// �ж��Ƿ��������
			GroupMember gm = new GroupMember();
			boolean isInFlag = gm.judgeUserIsInTheAlbum(userid, groupid); // trueʱ�û����ڿռ���

			boolean flag = true;
			boolean getNewGnum=false;
			int count = 1;
			record.set("joinStatus", 1);
			if (isInFlag) {	
				
				// ������ᣬ������û�����
				gm = new GroupMember().set("gmgroupid", groupid).set("gmuserid", userid).set("gmPort", port)
						.set("gmFromUserID", fromUserID);

				// ��������쳣���û��ظ����ʱ�ᵼ�²���ʧ��
				try {
					flag = gm.save();
					/*
					 * ����ֽ��(5166970==�ڴ�У԰��
					 */
//					if(null!=zj&&!zj.toString().equals("")&&groupid==5192130){
//						postToZJ(zj,userid+"");
//					}
					
					// ���·���������Ա�����ֶ�
					count = Db.update("update groups set gnum = gnum+1 where groupid='" + groupid + "' ");
					//������� begin
					//��ȡ�����б�	
					//List<Record> allActivitiGroupList =new ArrayList<Record>();
					String actGroupids = CacheKit.get("DataSystem", "allActivitiGroupIds");
					if(actGroupids == null) {
						//����Ϊ��  �����ݿ��ѯ						
						List<Record> actGroupList = Db.find("select activitiGroupid from activitigroups");
						if(null!=actGroupList&&!actGroupList.isEmpty()){
							StringBuffer ids=new StringBuffer();
							for(int i=0;i<actGroupList.size();i++){
								ids.append(actGroupList.get(i).get("activitiGroupid").toString()).append(",");
							}
							if(ids.length()>0){
								actGroupids=ids.substring(0, ids.length()-1);
								CacheKit.put("DataSystem", "ArrayList",actGroupids);			
							}		
						}						
					}
					if(Arrays.asList(actGroupids.split(",")).contains(groupid+"")){
						List uActList=Db.find("select joinUserid from activitimembers where joinGroupid="+groupid+" and joinUserid="+userid);
						if(uActList==null||uActList.isEmpty()){
							Db.update("insert into activitimembers (joinUserid,joinGroupid) values ("+userid+","+groupid+")");
						}
					}
					//������� end
					count = Db.update("update groups set gnum = gnum+1 where groupid='" + groupid + "' ");
					record.set("joinStatus", 0);
					getNewGnum=true;
				} catch (ActiveRecordException e) {
					flag = true;
					count = 1;

				}

			}

			if (flag && (count == 1)) {
				PointsService pointsService = new PointsService();
				int points = pointsService.getUseablePoints(userid+"");
				// ��ȡ�������� ,gAuthority��������0������ 1ֻ�д����� 2-����
				record.set("gname", group.get("gname").toString()).set("gcreator", group.get("gcreator").toString())
						.set("gtype", gtype).set("gnum", group.get("gnum").toString())
						.set("gpic", group.get("gpic").toString()).set("gAuthority", gAuthority)
						.set("gOrigin", group.get("gOrigin").toString())
						.set("eventQRCodeCanPublish", eventQRCodeCanPublish)
						.set("dialogShow", dialogShow).set("inBlackList", inBlackList).set("advertisementsShow", advertisementsShow)
						.set("isDefault", group.get("isDefault")).set("groupNewType", group.get("groupNewType"))
						.set("openGId", group.get("openGId"))
						.set("points", points)//points ����;
						.set("inviteusername", inviteuser.get("unickname"));//����������
				if(getNewGnum){
					record.set("gnum", group.getLong("gnum")+1);
				}
				//����� begin
				record.set("arTitle", "");
				record.set("arValue", new String[0]);
				if(null!=group.get("gOrigin").toString()&&!group.get("gOrigin").toString().equals("")&&group.get("gOrigin").toString().equals("1")){
										//String re="1���Ҳ�;2���ҿ�;3������;3����xx;4����00";
					List<Record> ruleList=Db.find("select arTitle,arValue from activitygrouprule where arGroupid="+group.get("groupid").toString());
					if(null!=ruleList&&!ruleList.isEmpty()){
						record.set("arTitle", ruleList.get(0).get("arTitle"));
						record.set("arValue", ruleList.get(0).get("arValue").toString().split(";"));
					}
					
				}
				//����� end
				// ��ȡ���ͽ���״̬
				if (isInFlag) {
					// ���ڿռ��ڵ��û�ֱ�ӷ���0
					record.set("isPush", "0");
				} else {
					// �ڿռ��ڵ��û�ȥ��ѯ
					List<Record> push = Db.find("select gmIsPush from groupmembers where gmgroupid=" + groupid
							+ " and gmuserid=" + userid + "");
					record.set("isPush", push.get(0).get("gmIsPush").toString());
				}

				// ��ȡ��Ա�б�����
				List<Record> groupMember = CacheKit.get("ConcurrencyCache", groupid + "Member");
				if (groupMember == null) {
					groupMember = Db.find(
							"select userid,unickname,upic,gmtime from users,groups,groupmembers where groupid=gmgroupid and users.userid=groupmembers.gmuserid and gmgroupid='"
									+ groupid + "' and gmstatus=0 order by gmid desc limit 3 ");
					CacheKit.put("ConcurrencyCache", groupid + "Member", groupMember);
				}

				// ��ȡ��Ƭ��������
				List<Record> photo = CacheKit.get("ConcurrencyCache", groupid + "Photo");
				if (photo == null) {
					photo = Db.find(
							"select count(*) as gpicNum from groups,events,pictures where peid=eid and groupid=egroupid and groupid="
									+ groupid + " and estatus in(0,3) and pstatus=0 ");
					CacheKit.put("ConcurrencyCache", groupid + "Photo", photo);
				}

				// ��ȡ����Ȩ���б����棬���ռ䷢��Ȩ��Ϊ������ʱ�Ų�ѯ������
				if (gAuthority == 2) {
					List<Record> uploadAuthority = CacheKit.get("ConcurrencyCache", groupid + "Authority");
					if (uploadAuthority == null) {
						uploadAuthority = Db.find("select gmuserid as userid from groupmembers where gmgroupid="
								+ groupid + " and gmauthority=1 ");
						CacheKit.put("ConcurrencyCache", groupid + "Authority", uploadAuthority);
					}
					record.set("authorityList", uploadAuthority);
				}
				record.set("picNum", photo.get(0).get("gpicNum").toString()).set("memberList", groupMember);
				
				List<Record> result = new ArrayList<Record>();
				result.add(record);
				jsonString = jsonData.getSuccessJson(result);

			} else {
				jsonString = dataProcess.insertFlagResult(false);
			}

		} else if (status == 1) {
			jsonString = jsonData.getJson(1012, "����ѱ�ɾ��");
		} else {
			jsonString = jsonData.getJson(1037, "����ѱ���");
		}
		renderText(jsonString);
	}
	@Before(CrossDomain.class)
	public void ShowSmallAppAlbumInformation() {
		String uid=this.getPara("userid");
		String gid=this.getPara("groupid");
		String zj=this.getPara("zj");
		String inviteuserid=this.getPara("inviteuserid");
		if(uid==null||gid==null||gid.equals("undefined")||gid.equals("NaN")){
			jsonString=jsonData.getJson(2, "��������",new ArrayList<Record>());
			renderText(jsonString);
			return;
		}
		User inviteuser=new User();
		if(null!=inviteuserid&&!inviteuserid.equals("0")&&!inviteuserid.equals("undefined")&&!inviteuserid.equals("NaN")&&!inviteuserid.equals("")){
			inviteuser=new User().findById(inviteuserid);
		}
		//������
		User u=new User().findById(uid);
		int inBlackList=1;
		if(null!=u&&null!=u.get("ustate")&&u.get("ustate").toString().equals("1")){
			inBlackList=0;
		}
		//������
		// ��������
		int userid = this.getParaToInt("userid");
		int groupid = this.getParaToInt("groupid");
		// �û���Դ����
		String port = this.getPara("port");
		String fromUserID = this.getPara("fromUserID");
		
		Group group = new Group().findById(groupid);
		int eventQRCodeCanPublish=new GroupCanPublish().getGroupPublishByType(groupid+"", "1");
		//dialogShow=1 ��ʾ��������
		int dialogShow=new GroupCanPublish().getGroupPublishByType(groupid+"", "2");
		//showAdvertisements=1 ��ʾ���λ
		//int advertisementsShow=new GroupCanPublish().getGroupPublishByType(groupid+"", "3");
		/*
		 * 1000W��Ƭ�
		 */
		int advertisementsShow=0;
		if(CommonParam.pOpenBanner){
			
			advertisementsShow=1;
			if(groupid==CommonParam.pGroupId7){
				YinianService service1 = new YinianService();
				service1.SetGroupIsTop(userid+"", CommonParam.pGroupId7+"", "yes");
//				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//				String time = sdf.format(new Date());
//				Db.update("update groupmembers set isTop=1 and topTime='"+time+"' where gmuserid="+userid);
				advertisementsShow=0;
			}
		}else{
			advertisementsShow=new GroupCanPublish().getGroupPublishByType(groupid+"", "3");
		}
		/*
		 * 1000W��Ƭ� end
		 */
		int status = Integer.parseInt(group.get("gstatus").toString());
		int gtype = Integer.parseInt(group.get("gtype").toString());
		int gAuthority = Integer.parseInt(group.get("gAuthority").toString());
		Record record = new Record();
		String upic = u.get("upic").toString();//�û�ͷ��
		record.set("upic", upic);
		//�ڴ�����
		if(null!=zj&&!zj.toString().equals("")&&zj.length()<4&&groupid==5193566){
			postToKD(zj,userid+"");
		}
		//ֽ΢����
		if(null!=zj&&!zj.toString().equals("")&&groupid==5275402){
			postToZW(zj,userid+"",u.get("uwechatid"));
		}
		//zho
		if(null!=zj&&!zj.toString().equals("")&&groupid==5298806){
			postToZJ(zj,userid+"");
		}
		//�ѱ�
		if(null!=zj&&!zj.toString().equals("")&&groupid==5451186){
			postToYB(zj,u.get("uwechatid").toString());
		}
		// �ж�����Ƿ�ɾ��
		if (status == 0) {
			// �ж��Ƿ��������
			GroupMember gm = new GroupMember();
			boolean isInFlag = gm.judgeUserIsInTheAlbum(userid, groupid); // trueʱ�û����ڿռ���

			boolean flag = true;
			boolean getNewGnum=false;
			int count = 1;
			record.set("joinStatus", 1);
			if (isInFlag) {	
				
				// ������ᣬ������û�����
				gm = new GroupMember().set("gmgroupid", groupid).set("gmuserid", userid).set("gmPort", port)
						.set("gmFromUserID", fromUserID);

				// ��������쳣���û��ظ����ʱ�ᵼ�²���ʧ��
				try {
					flag = gm.save();
					/*
					 * ����ֽ��(5166970==�ڴ�У԰��
					 */
//					if(null!=zj&&!zj.toString().equals("")&&groupid==5192130){
//						postToZJ(zj,userid+"");
//					}
					//������� begin
					//��ȡ�����б�	
					//List<Record> allActivitiGroupList =new ArrayList<Record>();
					String actGroupids = CacheKit.get("DataSystem", "allActivitiGroupIds");
					if(actGroupids == null) {
						//����Ϊ��  �����ݿ��ѯ						
						List<Record> actGroupList = Db.find("select activitiGroupid from activitigroups");
						if(null!=actGroupList&&!actGroupList.isEmpty()){
							StringBuffer ids=new StringBuffer();
							for(int i=0;i<actGroupList.size();i++){
								ids.append(actGroupList.get(i).get("activitiGroupid").toString()).append(",");
							}
							if(ids.length()>0){
								actGroupids=ids.substring(0, ids.length()-1);
								CacheKit.put("DataSystem", "ArrayList",actGroupids);			
							}		
						}						
					}
					if(Arrays.asList(actGroupids.split(",")).contains(groupid+"")){
						List uActList=Db.find("select joinUserid from activitimembers where joinGroupid="+groupid+" and joinUserid="+userid);
						if(uActList==null||uActList.isEmpty()){
							Db.update("insert into activitimembers (joinUserid,joinGroupid) values ("+userid+","+groupid+")");
						}
					}
					//������� end
					// ���·���������Ա�����ֶ�
					count = Db.update("update groups set gnum = gnum+1 where groupid='" + groupid + "' ");
					record.set("joinStatus", 0);
					getNewGnum=true;
				} catch (ActiveRecordException e) {
					flag = true;
					count = 1;

				}

			}

			if (flag && (count == 1)) {
				PointsService pointsService = new PointsService();
				int points = pointsService.getUseablePoints(userid+"");
				// ��ȡ�������� ,gAuthority��������0������ 1ֻ�д����� 2-����
				record.set("gname", group.get("gname").toString()).set("gcreator", group.get("gcreator").toString())
						.set("gtype", gtype).set("gnum", group.get("gnum").toString())
						.set("gpic", group.get("gpic").toString()).set("gAuthority", gAuthority)
						.set("gOrigin", group.get("gOrigin").toString())
						.set("eventQRCodeCanPublish", eventQRCodeCanPublish)
						.set("dialogShow", dialogShow).set("inBlackList", inBlackList).set("advertisementsShow", advertisementsShow)
						.set("isDefault", group.get("isDefault")).set("groupNewType", group.get("groupNewType"))
						.set("openGId", group.get("openGId"))
						.set("points", points)//points ����;
						.set("inviteusername", inviteuser.get("unickname"));//����������
				if(getNewGnum){
					record.set("gnum", group.getLong("gnum")+1);
				}
				//����� begin
				record.set("arTitle", "");
				record.set("arValue", new String[0]);
				if(null!=group.get("gOrigin").toString()&&!group.get("gOrigin").toString().equals("")&&group.get("gOrigin").toString().equals("1")){
										//String re="1���Ҳ�;2���ҿ�;3������;3����xx;4����00";
					List<Record> ruleList=Db.find("select arTitle,arValue from activitygrouprule where arGroupid="+group.get("groupid").toString());
					if(null!=ruleList&&!ruleList.isEmpty()){
						record.set("arTitle", ruleList.get(0).get("arTitle"));
						record.set("arValue", ruleList.get(0).get("arValue").toString().split(";"));
					}
					
				}
				//����� end
				// ��ȡ���ͽ���״̬
				if (isInFlag) {
					// ���ڿռ��ڵ��û�ֱ�ӷ���0
					record.set("isPush", "0");
				} else {
					// �ڿռ��ڵ��û�ȥ��ѯ
					List<Record> push = Db.find("select gmIsPush from groupmembers where gmgroupid=" + groupid
							+ " and gmuserid=" + userid + "");
					record.set("isPush", push.get(0).get("gmIsPush").toString());
				}

				// ��ȡ��Ա�б�����
				List<Record> groupMember = CacheKit.get("ConcurrencyCache", groupid + "Member");
				if (groupMember == null) {
					groupMember = Db.find(
							"select userid,unickname,upic,gmtime from users,groups,groupmembers where groupid=gmgroupid and users.userid=groupmembers.gmuserid and gmgroupid='"
									+ groupid + "' and gmstatus=0 order by gmid desc limit 3 ");
					CacheKit.put("ConcurrencyCache", groupid + "Member", groupMember);
				}

				// ��ȡ��Ƭ��������
				List<Record> photo = CacheKit.get("ConcurrencyCache", groupid + "Photo");
				if (photo == null) {
					photo = Db.find(
							"select count(*) as gpicNum from groups,events,pictures where peid=eid and groupid=egroupid and groupid="
									+ groupid + " and estatus in(0,3) and pstatus=0 ");
					CacheKit.put("ConcurrencyCache", groupid + "Photo", photo);
				}

				// ��ȡ����Ȩ���б����棬���ռ䷢��Ȩ��Ϊ������ʱ�Ų�ѯ������
				if (gAuthority == 2) {
					List<Record> uploadAuthority = CacheKit.get("ConcurrencyCache", groupid + "Authority");
					if (uploadAuthority == null) {
						uploadAuthority = Db.find("select gmuserid as userid from groupmembers where gmgroupid="
								+ groupid + " and gmauthority=1 ");
						CacheKit.put("ConcurrencyCache", groupid + "Authority", uploadAuthority);
					}
					record.set("authorityList", uploadAuthority);
				}
				record.set("picNum", photo.get(0).get("gpicNum").toString()).set("memberList", groupMember);
				
				List<Record> result = new ArrayList<Record>();
				result.add(record);
				jsonString = jsonData.getSuccessJson(result);

			} else {
				jsonString = dataProcess.insertFlagResult(false);
			}

		} else if (status == 1) {
			jsonString = jsonData.getJson(1012, "����ѱ�ɾ��");
		} else {
			jsonString = jsonData.getJson(1037, "����ѱ���");
		}
		renderText(jsonString);
	}
	public void postToYB(String zj,String openid){
		Long ts=System.currentTimeMillis();
		String url="https://api.uboxsale.com/Miniapp/SendCouponYi/umid/"+zj+"/thirdid/"+openid;		
//		String param1 = getMD5("api=User.Tissue.Order.TicketOrdertimestamp="+ts+"tissue_ticket="+zj+"bjSjRNJmXvBE16faKxo3caLcFjGSZ5bV");
//		url+=param1;
//		param1="api=User.Tissue.Order.TicketOrder&timestamp="+ts+"&tissue_ticket="+zj;
		HttpUtils u=new HttpUtils();
		
		savePartnerRecord(0+"", zj+"   "+openid, u.sendPost(url, ""), "Miniapp");
		//{"status":-1,"status_str":"�ѷ��Ž���","api_data":[]}
	}
	/**
	 * Ⱥ���-��ʾ�����Ϣ
	 */
	@Before(CrossDomain.class)
	public void ShowSmallAppFlockAlbumInformation() {
		String uid=this.getPara("userid");
		String gid=this.getPara("groupid");
		if(uid==null||gid==null||gid.equals("undefined")||gid.equals("NaN")){
			jsonString=jsonData.getJson(2, "��������",new ArrayList<Record>());
			renderText(jsonString);
			return;
		}
		//������
//		User u=new User().findById(uid);
//		int inBlackList=1;
//		if(null!=u&&null!=u.get("ustate")&&u.get("ustate").toString().equals("1")){
//			inBlackList=0;
//		}
		//������
		// ��������
		int userid = this.getParaToInt("userid");
		int groupid = this.getParaToInt("groupid");
		// �û���Դ����
		String port = this.getPara("port");
		String fromUserID = this.getPara("fromUserID");
		
		Group group = new Group().findById(groupid);
		User user=new User().findById(userid);
//		int eventQRCodeCanPublish=new GroupCanPublish().getGroupPublishByType(groupid+"", "1");
//		//dialogShow=1 ��ʾ��������
//		int dialogShow=new GroupCanPublish().getGroupPublishByType(groupid+"", "2");
//		//showAdvertisements=1 ��ʾ���λ
//		int advertisementsShow=new GroupCanPublish().getGroupPublishByType(groupid+"", "3");
		int status = Integer.parseInt(group.get("gstatus").toString());
		int gtype = Integer.parseInt(group.get("gtype").toString());
		int gAuthority = Integer.parseInt(group.get("gAuthority").toString());
		// �ж�����Ƿ�ɾ��
		if (status == 0) {
			// �ж��Ƿ��������
			GroupMember gm = new GroupMember();
			boolean isInFlag = gm.judgeUserIsInTheAlbum(userid, groupid); // trueʱ�û����ڿռ���

			boolean flag = true;
			boolean getNewGnum=false;
			int count = 1;
			Record record = new Record().set("joinStatus", 1);
			if (isInFlag) {
				// ������ᣬ������û�����
				gm = new GroupMember().set("gmgroupid", groupid).set("gmuserid", userid).set("gmPort", port)
						.set("gmFromUserID", fromUserID);

				// ��������쳣���û��ظ����ʱ�ᵼ�²���ʧ��
				try {
					flag = gm.save();
					// ���·���������Ա�����ֶ�
					//count = Db.update("update groups set gnum = gnum+1 where groupid='" + groupid + "' ");
					record.set("joinStatus", 0);
					getNewGnum=true;
				} catch (ActiveRecordException e) {
					flag = true;
					count = 1;

				}

			}

			if (flag && (count == 1)) {
				// ��ȡ�������� ,gAuthority��������0������ 1ֻ�д����� 2-����
				PointsService pointsService = new PointsService();
				int points = pointsService.getUseablePoints(userid+"");
				//int points = pointsService.getUseablePoints(userid);
				record.set("gname", group.get("gname").toString()).set("gcreator", group.get("gcreator").toString())
						.set("gtype", gtype).set("gnum", group.get("gnum").toString())
						.set("gpic", group.get("gpic").toString()).set("gAuthority", gAuthority)
						.set("gOrigin", group.get("gOrigin").toString())
					//	.set("eventQRCodeCanPublish", eventQRCodeCanPublish)
					//	.set("dialogShow", dialogShow).set("inBlackList", inBlackList).set("advertisementsShow", advertisementsShow)
						.set("isDefault", group.get("isDefault")).set("groupNewType", group.get("groupNewType"))
						.set("openGId", group.get("openGId"))
						.set("points", points);//points ����
				if(getNewGnum){
					record.set("gnum", group.getLong("gnum")+1);
				}
				if(null!=user){
					record.set("unickname", user.get("unickname").toString());
					record.set("upic", user.get("upic").toString());
				}
				// ��ȡ���ͽ���״̬
				if (isInFlag) {
					// ���ڿռ��ڵ��û�ֱ�ӷ���0
					record.set("isPush", "0");
				} else {
					// �ڿռ��ڵ��û�ȥ��ѯ
					List<Record> push = Db.find("select gmIsPush from groupmembers where gmgroupid=" + groupid
							+ " and gmuserid=" + userid + "");
					record.set("isPush", push.get(0).get("gmIsPush").toString());
				}

				// ��ȡ��Ա�б�����
//				List<Record> groupMember = CacheKit.get("ConcurrencyCache", groupid + "Member");
//				if (groupMember == null) {
//					groupMember = Db.find(
//							"select userid,unickname,upic,gmtime from users,groups,groupmembers where groupid=gmgroupid and users.userid=groupmembers.gmuserid and gmgroupid='"
//									+ groupid + "' and gmstatus=0 order by gmid desc limit 3 ");
//					CacheKit.put("ConcurrencyCache", groupid + "Member", groupMember);
//				}
//
//				// ��ȡ��Ƭ��������
				List<Record> photo = CacheKit.get("ConcurrencyCache", groupid + "Photo");
				if (photo == null) {
					photo = Db.find(
							"select count(*) as gpicNum from groups,events,pictures where peid=eid and groupid=egroupid and groupid="
									+ groupid + " and estatus in(0,3) and pstatus=0 ");
					CacheKit.put("ConcurrencyCache", groupid + "Photo", photo);
				}

				// ��ȡ����Ȩ���б����棬���ռ䷢��Ȩ��Ϊ������ʱ�Ų�ѯ������
//				if (gAuthority == 2) {
//					List<Record> uploadAuthority = CacheKit.get("ConcurrencyCache", groupid + "Authority");
//					if (uploadAuthority == null) {
//						uploadAuthority = Db.find("select gmuserid as userid from groupmembers where gmgroupid="
//								+ groupid + " and gmauthority=1 ");
//						CacheKit.put("ConcurrencyCache", groupid + "Authority", uploadAuthority);
//					}
//					record.set("authorityList", uploadAuthority);
//				}
				record.set("picNum", photo.get(0).get("gpicNum").toString());

				List<Record> result = new ArrayList<Record>();
				result.add(record);
				jsonString = jsonData.getSuccessJson(result);

			} else {
				jsonString = dataProcess.insertFlagResult(false);
			}

		} else if (status == 1) {
			jsonString = jsonData.getJson(1012, "����ѱ�ɾ��");
		} else {
			jsonString = jsonData.getJson(1037, "����ѱ���");
		}
		renderText(jsonString);
	}
	/*
	 * Ⱥ��� -��ȡ���openId
	 */
	public void getFlockAlbumOpenId_new(){
		 YinianDataProcess dataProcess = new YinianDataProcess();
		 NewH5Service service=new NewH5Service();
		 String code=this.getPara("code")==null?"":this.getPara("code");
		// System.out.println("code="+code);
		 String encryptedData=this.getPara("encryptedData")==null?"":this.getPara("encryptedData");
		// System.out.println("encryptedData="+encryptedData);
		 String iv=this.getPara("iv")==null?"":this.getPara("iv");
		 String userid = this.getPara("userid")==null||this.getPara("userid").toString().equals("")?"816596":this.getPara("userid");
		// System.out.println("iv="+iv);
		 /*���ܴ��ݵ��ֶ�*/
		 String groupType="4";
		 String groupNewType=this.getPara("groupNewType")==null||this.getPara("groupNewType").toString().equals("")||this.getPara("groupNewType").toString().equals("0")?"13":this.getPara("groupNewType");
		 String eid=this.getPara("eid")==null||this.getPara("eid").toString().equals("0")?"":this.getPara("eid");
		 AES aes = new AES();
		 List<Record> list = new ArrayList<Record>();
		 Record r=new Record();
		 String openGId="";
		 /*��ȡ����*/
		 String url= getUrlByNewType(groupNewType);
		 if(!code.equals("")&&!encryptedData.equals("")&&!iv.equals("")){
			 String result= dataProcess.sentNetworkRequest(CommonParam.getWechatSessionKeyUrl + code);
			 com.alibaba.fastjson.JSONObject jo = com.alibaba.fastjson.JSONObject.parseObject(result);
			 //System.out.println("jo="+jo);
			 byte[] resultByte;
			 String session_key = jo.getString("session_key");
				try {					
					resultByte = 
							//Pkcs7Encoder.decryptOfDiyIV(Base64.decode(encryptedData), Base64.decode(session_key), Base64.decode(iv));
							aes.decrypt_lk(Base64.decode(encryptedData), Base64.decode(session_key), Base64.decode(iv));
					// System.out.println("resultByte="+resultByte);
					if (null != resultByte && resultByte.length > 0) {
						String u = new String(resultByte, "UTF-8");
						com.alibaba.fastjson.JSONObject jou = com.alibaba.fastjson.JSONObject.parseObject(u);					
						//r.set("openGId", jou.get("openGId"));	
						openGId=jou.get("openGId").toString();
					}
					if(null==openGId||openGId.equals("")){
						jsonString = jsonData.getJson(2, "��������openGIdΪ��", new ArrayList<Record>());
						renderText(jsonString);
						return;
					}
					if(service.canCreateFlockAlbum(openGId)){
						String inviteCode = Group.CreateSpaceInviteCode();
						jsonString = service.createFlockAlbum("Ⱥ���", userid, url, groupType, inviteCode, "С����","1",groupNewType,openGId,eid);
						
					}else{
						//��ȡ���id			
						jsonString = jsonData.getSuccessJson(service.getFlockAlbumByOpenGId(openGId));
					}
					
					//list.add(r);
					
					// System.out.println("ivb="+ivb);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		 }else{
			 jsonString = jsonData.getJson(2, "��������", new ArrayList<Record>());			
		 }
		 renderText(jsonString);
	 }
	/*
	 * Ⱥ���-�����ȵ�ͼƬ�������
	 */
	/*
	 * Ⱥ��� -��ȡ���openId
	 */
	public void getFlockAlbumOpenId_withHotPic(){
		 YinianDataProcess dataProcess = new YinianDataProcess();
		 NewH5Service service=new NewH5Service();
		 String code=this.getPara("code")==null?"":this.getPara("code");
		// System.out.println("code="+code);
		 String encryptedData=this.getPara("encryptedData")==null?"":this.getPara("encryptedData");
		// System.out.println("encryptedData="+encryptedData);
		 String iv=this.getPara("iv")==null?"":this.getPara("iv");
		 String userid = this.getPara("userid")==null||this.getPara("userid").toString().equals("")?"816596":this.getPara("userid");
		// System.out.println("iv="+iv);
		 /*���ܴ��ݵ��ֶ�*/
		 String groupType="4";
		 String groupNewType=this.getPara("groupNewType")==null||this.getPara("groupNewType").toString().equals("")||this.getPara("groupNewType").toString().equals("0")?"13":this.getPara("groupNewType");
		 String hid=this.getPara("hid")==null||this.getPara("hid").toString().equals("0")?"":this.getPara("hid");		
		 AES aes = new AES();
		 List<Record> list = new ArrayList<Record>();
		 Record r=new Record();
		 String openGId="";
		 /*��ȡ����*/
		 String url= getUrlByNewType(groupNewType);
		 if(!code.equals("")&&!encryptedData.equals("")&&!iv.equals("")){
			 String result= dataProcess.sentNetworkRequest(CommonParam.getWechatSessionKeyUrl + code);
			 com.alibaba.fastjson.JSONObject jo = com.alibaba.fastjson.JSONObject.parseObject(result);
			 //System.out.println("jo="+jo);
			 byte[] resultByte;
			 String session_key = jo.getString("session_key");
				try {					
					resultByte = 
							//Pkcs7Encoder.decryptOfDiyIV(Base64.decode(encryptedData), Base64.decode(session_key), Base64.decode(iv));
							aes.decrypt_lk(Base64.decode(encryptedData), Base64.decode(session_key), Base64.decode(iv));
					// System.out.println("resultByte="+resultByte);
					if (null != resultByte && resultByte.length > 0) {
						String u = new String(resultByte, "UTF-8");
						com.alibaba.fastjson.JSONObject jou = com.alibaba.fastjson.JSONObject.parseObject(u);					
						//r.set("openGId", jou.get("openGId"));	
						openGId=jou.get("openGId").toString();
					}
					if(null==openGId||openGId.equals("")){
						jsonString = jsonData.getJson(2, "��������openGIdΪ��", new ArrayList<Record>());
						renderText(jsonString);
						return;
					}
					if(service.canCreateFlockAlbum(openGId)){
						String inviteCode = Group.CreateSpaceInviteCode();
						jsonString = service.createFlockAlbumWithHotPic("Ⱥ���", userid, url, groupType, inviteCode, "С����","1",groupNewType,openGId,hid);
						
					}else{
						//��ȡ���id	
						List<Record> groupidList=service.getFlockAlbumByOpenGId(openGId);
						if(null!=groupidList&&!groupidList.isEmpty()){
							String groupid=groupidList.get(0).getLong("groupid").toString();
							service.createHotPicEvent(groupid, userid, hid);
						}
						jsonString = jsonData.getSuccessJson(groupidList);
					}
					
					//list.add(r);
					
					// System.out.println("ivb="+ivb);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		 }else{
			 jsonString = jsonData.getJson(2, "��������", new ArrayList<Record>());			
		 }
		 renderText(jsonString);
	 }
	public void getFlockAlbumOpenId(){
		 YinianDataProcess dataProcess = new YinianDataProcess();
		 String code=this.getPara("code");
		 System.out.println("code="+code);
		 String encryptedData=this.getPara("encryptedData");
		 System.out.println("encryptedData="+encryptedData);
		 String iv=this.getPara("iv");
		 System.out.println("iv="+iv);
		 AES aes = new AES();
		 List<Record> list = new ArrayList<Record>();
		 Record r=new Record();
		 if(null!=encryptedData&&null!=iv){
			 String result= dataProcess.sentNetworkRequest(CommonParam.getWechatSessionKeyUrl + code);
			 com.alibaba.fastjson.JSONObject jo = com.alibaba.fastjson.JSONObject.parseObject(result);
			 System.out.println("jo="+jo);
			 byte[] resultByte;
			 String session_key = jo.getString("session_key");
				try {					
					resultByte = 
							//Pkcs7Encoder.decryptOfDiyIV(Base64.decode(encryptedData), Base64.decode(session_key), Base64.decode(iv));
							aes.decrypt_lk(Base64.decode(encryptedData), Base64.decode(session_key), Base64.decode(iv));
					 System.out.println("resultByte="+resultByte);
					if (null != resultByte && resultByte.length > 0) {
						String u = new String(resultByte, "UTF-8");
						com.alibaba.fastjson.JSONObject jou = com.alibaba.fastjson.JSONObject.parseObject(u);					
						r.set("openGId", jou.get("openGId"));						
					}else{
						r.set("openGId", "");	
					}
					list.add(r);
					
					// System.out.println("ivb="+ivb);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}finally{
					jsonString = jsonData.getJson(0, "success", list);					
			}
		 }else{
			 jsonString = jsonData.getJson(2, "��������", new ArrayList<Record>());			
		 }
		 renderText(jsonString);
	 }
	/*
	 * Ⱥ��� -�������
	 */
	@Before(CrossDomain.class)
	public void CreateFlockAlbum() {
		NewH5Service service=new NewH5Service();
		// ��ȡ����
		String userid = this.getPara("userid")==null?"":this.getPara("userid");
		String groupName = this.getPara("groupName");
		String groupType = this.getPara("groupType");
		String url = this.getPara("url");
		String source = this.getPara("source");
		//String formID = this.getPara("formID")==null?"":this.getPara("formID"); // С�������ͱ�ID
		String isDefault=this.getPara("isDefault")==null?"1":this.getPara("isDefault");//0Ϊ��ͨ��ᣬ1ΪȺ���
		String groupNewType=this.getPara("groupNewType")==null||this.getPara("groupNewType").equals("")||this.getPara("groupNewType").equals("0")?"13":this.getPara("groupNewType");//0ΪĬ��		
		String openGId=this.getPara("openGId");
		if(null==openGId||openGId.equals("")){
			jsonString = jsonData.getJson(2, "��������", new ArrayList<Record>());
			renderText(jsonString);
		}
		if(service.canCreateFlockAlbum(openGId)){
//		if(!userid.equals("")&&!formID.equals("")){
//			FormID.insert(userid, formID);
//		}
		//if (url == null || url.equals("")) {
			// ����������ȡ��Ӧ��Ⱥͷ��ͼƬ
			//����url
		url= getUrlByNewType(groupNewType);
//		}else {
//			String temp = url.substring(0, 7);
//			if (!(temp.equals("http://"))) {
//				url = CommonParam.qiniuOpenAddress + url;
//			}
//
//		}
		String inviteCode = Group.CreateSpaceInviteCode();
		
		jsonString = service.createAlbum("Ⱥ���", userid, url, groupType, inviteCode, source,isDefault,groupNewType,openGId);
		}else{
			jsonString = jsonData.getJson(2, "������Ѵ���", new ArrayList<Record>());
		}
		// ���ؽ��
		renderText(jsonString);
	}
	/*
	 * Ⱥ���-����openGId�ж�����Ƿ���ڣ����������򴴽���ᣬ�������id
	 */
	public void getGroupidByOpenGId(){
		NewH5Service service=new NewH5Service();
		String openGId=this.getPara("openGId");
		String userid = this.getPara("userid")==null||this.getPara("userid").toString().equals("")?"816596":this.getPara("userid");
		if(null==openGId||openGId.equals("")){
			jsonString = jsonData.getJson(2, "��������", new ArrayList<Record>());
			renderText(jsonString);
			return;
		}
		if(service.canCreateFlockAlbum(openGId)){
			String inviteCode = Group.CreateSpaceInviteCode();
			jsonString = service.createFlockAlbum("Ⱥ���", userid, "", "4", inviteCode, "Ⱥ���","1","19",openGId,"");
		}else{
			//��ȡ���id			
			jsonString = jsonData.getSuccessJson(service.getFlockAlbumByOpenGId(openGId));
		}
		renderText(jsonString);
	}
	public String getUrlByNewType(String groupNewType){
		String url=CommonParam.qiniuOpenAddress+"20180104-13-1.jpg";
		switch (groupNewType) {
		case "1":// ����
			List<Record> coverList = CacheKit.get("EternalCache", "spaceCover1");
			// ����Ϊ�գ����²�ѯ
			if (coverList == null) {
				coverList = Group.GetNewSpaceDefaultCoverList(11);
				CacheKit.put("EternalCache", "spaceCover1", coverList);
			}
			int size = coverList.size();
			url = coverList.get(new Random().nextInt(size)).getStr("acurl");
			break;
		case "2":// �ۻ�
			coverList = CacheKit.get("EternalCache", "spaceCover2");
			// ����Ϊ�գ����²�ѯ
			if (coverList == null) {
				coverList = Group.GetNewSpaceDefaultCoverList(12);
				CacheKit.put("EternalCache", "spaceCover2", coverList);
			}
			size = coverList.size();
			url = coverList.get(new Random().nextInt(size)).getStr("acurl");
			break;
		case "3":// ����
			coverList = CacheKit.get("EternalCache", "spaceCover3");
			// ����Ϊ�գ����²�ѯ
			if (coverList == null) {
				coverList = Group.GetNewSpaceDefaultCoverList(13);
				CacheKit.put("EternalCache", "spaceCover3", coverList);
			}
			size = coverList.size();
			url = coverList.get(new Random().nextInt(size)).getStr("acurl");
			break;
		case "4":// ����  ֻ��һ��				
			url = CommonParam.qiniuOpenAddress+"20180104-4-1.jpg";
			break;
		case "5":// ����
			coverList = CacheKit.get("EternalCache", "spaceCover5");
			// ����Ϊ�գ����²�ѯ
			if (coverList == null) {
				coverList = Group.GetNewSpaceDefaultCoverList(15);
				CacheKit.put("EternalCache", "spaceCover5", coverList);
			}
			size = coverList.size();
			url = coverList.get(new Random().nextInt(size)).getStr("acurl");
			break;
		case "6":// ����
			coverList = CacheKit.get("EternalCache", "spaceCover6");
			// ����Ϊ�գ����²�ѯ
			if (coverList == null) {
				coverList = Group.GetNewSpaceDefaultCoverList(16);
				CacheKit.put("EternalCache", "spaceCover6", coverList);
			}
			size = coverList.size();
			url = coverList.get(new Random().nextInt(size)).getStr("acurl");
			break;
		case "7":// ����
			coverList = CacheKit.get("EternalCache", "spaceCover7");
			// ����Ϊ�գ����²�ѯ
			if (coverList == null) {
				coverList = Group.GetNewSpaceDefaultCoverList(17);
				CacheKit.put("EternalCache", "spaceCover7", coverList);
			}
			size = coverList.size();
			url = coverList.get(new Random().nextInt(size)).getStr("acurl");
			break;
		case "8":// ��Ȥ
			coverList = CacheKit.get("EternalCache", "spaceCover8");
			// ����Ϊ�գ����²�ѯ
			if (coverList == null) {
				coverList = Group.GetNewSpaceDefaultCoverList(18);
				CacheKit.put("EternalCache", "spaceCover8", coverList);
			}
			size = coverList.size();
			url = coverList.get(new Random().nextInt(size)).getStr("acurl");
			break;
		case "9":// У԰
			coverList = CacheKit.get("EternalCache", "spaceCover9");
			// ����Ϊ�գ����²�ѯ
			if (coverList == null) {
				coverList = Group.GetNewSpaceDefaultCoverList(19);
				CacheKit.put("EternalCache", "spaceCover9", coverList);
			}
			size = coverList.size();
			url = coverList.get(new Random().nextInt(size)).getStr("acurl");
			break;
		case "10":// ��˾
			coverList = CacheKit.get("EternalCache", "spaceCover10");
			// ����Ϊ�գ����²�ѯ
			if (coverList == null) {
				coverList = Group.GetNewSpaceDefaultCoverList(20);
				CacheKit.put("EternalCache", "spaceCover10", coverList);
			}
			size = coverList.size();
			url = coverList.get(new Random().nextInt(size)).getStr("acurl");
			break;
		case "11":// ����
			coverList = CacheKit.get("EternalCache", "spaceCover11");
			// ����Ϊ�գ����²�ѯ
			if (coverList == null) {
				coverList = Group.GetNewSpaceDefaultCoverList(21);
				CacheKit.put("EternalCache", "spaceCover11", coverList);
			}
			size = coverList.size();
			url = coverList.get(new Random().nextInt(size)).getStr("acurl");
			break;
		case "12":// � ֻ��һ��
			url = CommonParam.qiniuOpenAddress+"20180104-12-1.jpg";
			break;
		case "13":// ����
			coverList = CacheKit.get("EternalCache", "spaceCover13");
			// ����Ϊ�գ����²�ѯ
			if (coverList == null) {
				coverList = Group.GetNewSpaceDefaultCoverList(23);
				CacheKit.put("EternalCache", "spaceCover13", coverList);
			}
			size = coverList.size();
			url = coverList.get(new Random().nextInt(size)).getStr("acurl");
			break;
		default:
			coverList = CacheKit.get("EternalCache", "spaceCover13");
			// ����Ϊ�գ����²�ѯ
			if (coverList == null) {
				coverList = Group.GetNewSpaceDefaultCoverList(23);
				CacheKit.put("EternalCache", "spaceCover13", coverList);
			}
			size = coverList.size();
			url = coverList.get(new Random().nextInt(size)).getStr("acurl");
			break;
		}
		return url;
	}
	/*
	 * ����-�ȵ�ͼƬ�б�
	 */
	 public void getHotPic(){
		 NewH5Service service=new NewH5Service();
		 String uid=this.getPara("uid");
		 jsonString = jsonData.getJson(2, "��������", new ArrayList<Record>());
			
		 if(null!=uid&&!uid.equals("")){
			 List<Record> list=service.getUserHotPicList(uid);
			 jsonString = jsonData.getSuccessJson(list);
		 }
		 renderText(jsonString);
		 return;
	 }
	 /*
	  * ����-�û����鿴�����һ���ȵ�ͼƬ
	  */
	 public void setUserHotPic(){
		 jsonString = jsonData.getJson(2, "��������", new ArrayList<Record>());
		 String uid=this.getPara("uid");
		 String hid=this.getPara("hid");
		 NewH5Service service=new NewH5Service();
		if(service.setUserHotPic(uid, hid)){
			 jsonString = jsonData.getSuccessJson(new ArrayList<Record>());
		}
		 renderText(jsonString);
		 return;
	 }
	 /*
	  * ��ʾ-�û���һ�η�����̬�ж�
	  */
	 public void userFirstPublish(){
		 jsonString = jsonData.getJson(2, "��������", new ArrayList<Record>());
		 String uid=this.getPara("uid"); 
		 if(null!=uid&&!uid.equals("")){
			 NewH5Service service=new NewH5Service();
			 Record r=new Record();
			 r.set("isFirstPublish", service.userFirstPublish(uid));
			 List<Record> list=new ArrayList<Record>();
			 list.add(r);
			 jsonString = jsonData.getSuccessJson(list);
		 }
		 renderText(jsonString);
		 return;
	 }
	 
	 /**
	  * ���-����
	 * @throws IOException 
	  */
	 public void sendValue() throws IOException {
		 
		 BufferedReader reader = null; 
			try {
				String strUrl = "http://picture.zhuiyinanian.com/yinian/sendValue.txt";
				URL url = new URL(strUrl); 
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				InputStreamReader input = new InputStreamReader(conn.getInputStream());
				reader = new BufferedReader(input);
				//reader = new BufferedReader(new InputStreamReader(new FileInputStream("D:\\leiyu\\sendValue.txt")));
				StringBuffer buffer = new StringBuffer();
				String line = reader.readLine();
				while (line!=null) {
					buffer.append(line);
					line = reader.readLine();
				}
				reader.close(); 
				String content = buffer.toString();
				
				List<Record> resultList = new ArrayList<Record>();
				Record r = new Record();
				if (content != null && content.length() > 0) {
					//�����ݲ�Ϊ��ʱ�ر�
					r.set("sendValue", 1);//0 ����  1�ر�
				}else {
					//������Ϊ��ʱ����
					r.set("sendValue", 0);//0 ����  1�ر�
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
	 /*
	  * by lk ������ ����ҳ(��ҳ��ȡ��)
	  */
	 public void getAllDefaultAlbumCover(){
		 //String acid=null==this.getPara("acid")||this.getPara("acid").equals("")?"0":this.getPara("acid");
		 NewH5Service service=new NewH5Service();		
		 List<Record> coverList = CacheKit.get("EternalCache", "AllDefaultAlbumCover");
		 if(null==coverList){
			 //System.out.println("���ǻ���");
				coverList=service.getAllDefaultAlbumCover(null);
				CacheKit.put("EternalCache", "AllDefaultAlbumCover", coverList);			
		 }
		 jsonString = jsonData.getSuccessJson(coverList);
		 renderText(jsonString);
	 }
	 /*
	  * by lk �����������ʱ��ʾ��Կkey
	  */
	 public void getUserKey(){
		 String userid=this.getPara("userid");
		 List<Record> list=new ArrayList<Record>();
		 Record r=new Record();
		 r.set("userkey", "");
		 if(null!=userid&&!userid.equals("")){
			 NewH5Service service=new NewH5Service();
			 r.set("userkey", URLEncoder.encode(service.getKey(userid)));
		 }
		 list.add(r);
		 jsonString = jsonData.getSuccessJson(list);
		 renderText(jsonString); 
	 }
	 /*
	  * by lk ͨ����Կkey��ʾid
	  */
	 public void getUseridByKey(){
		 String userkey=this.getPara("userkey");
		 List<Record> list=new ArrayList<Record>();
		 Record r=new Record();
		 r.set("userid", "");
		 if(null!=userkey&&!userkey.equals("")){
			 NewH5Service service=new NewH5Service();
			 r.set("userid", service.getUseridByKey(URLDecoder.decode(userkey)));
		 }
		 list.add(r);
		 jsonString = jsonData.getSuccessJson(list);
		 renderText(jsonString); 
	 }
}
