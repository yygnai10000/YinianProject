package yinian.controller;

import java.util.List;

import com.jfinal.aop.Before;
import com.jfinal.core.Controller;
import com.jfinal.plugin.activerecord.Record;

import yinian.app.YinianDataProcess;
import yinian.common.CommonParam;
import yinian.interceptor.CrossDomain;
import yinian.service.PersonalService;
import yinian.utils.JsonData;

public class PersonalController extends Controller {
	private String jsonString; // ���ص�json�ַ���
	private JsonData jsonData = new JsonData(); // json������
	/*
	 * �û�����������ϴ�ͼƬ
	 */
	@Before(CrossDomain.class)
	public void SetUserPublishList() throws Exception {
		jsonString = jsonData.getJson(2, "�����������");
		String userid = this.getPara("userid")==null?"":this.getPara("userid");
		String picAddress=this.getPara("picAddress")==null?"":this.getPara("picAddress");
		String md5=this.getPara("md5")==null?"":this.getPara("md5");
		String createTime=this.getPara("createTime")==null?"":this.getPara("createTime");
		if(!userid.equals("")&&!picAddress.equals("")&&!md5.equals("")&&!createTime.equals("")){
			PersonalService service=new PersonalService();
			List<Record> list=service.insertPhoto(userid, picAddress, md5, createTime, createTime, 0);
			if(!list.isEmpty()){
				boolean hasRepeat = false;//�Ƿ����ظ��ϴ���ͼƬ
				for(Record r:list) {
					if(r.getBoolean("isRepeat")) {
						hasRepeat = true;
					}
					r.remove("isRepeat");
				}
				if(hasRepeat) {
					jsonString = jsonData.getJson(0, "successWihRepeat", list);		
				}else {
					jsonString = jsonData.getSuccessJson(list);		
				}
			}
		}
		renderText(jsonString);
		//String mode=this.getPara("mode");
	}
	/*
	 * ��ȡ�û����������ͼƬ
	 */
	@Before(CrossDomain.class)
	public void GetUserPublushList()throws Exception{
		jsonString = jsonData.getJson(2, "�����������");
		String userid = this.getPara("userid")==null?"":this.getPara("userid");
		String mode = this.getPara("mode")==null?"day":this.getPara("mode");
		PersonalService service=new PersonalService();
		if(!userid.equals("")){
			jsonString = jsonData.getSuccessJson(service.findListByUserIdAndMode(userid, mode));
		}
		renderText(jsonString);
	}
	/*
	 * pc�˻�ȡ�û����������ͼƬ
	 */
	@Before(CrossDomain.class)
	public void PcGetUserPublushList()throws Exception{
		jsonString = jsonData.getJson(2, "�����������");
		String userid = this.getPara("userid")==null?"":this.getPara("userid");
		String mode = this.getPara("mode")==null?"day":this.getPara("mode");
		String endDate = this.getPara("endDate")==null?"":this.getPara("endDate");
		PersonalService service=new PersonalService();
		if(!userid.equals("")){
			jsonString = jsonData.getSuccessJson(service.pcFindListByUserIdAndMode(userid, mode,endDate));
		}
		renderText(jsonString);
	}
	/*
	 * pc�˻�ȡ�û����������ͼƬ,���ϴ�ʱ��
	 */
	@Before(CrossDomain.class)
	public void PcGetUserPublushListByUploadTime()throws Exception{
		jsonString = jsonData.getJson(2, "�����������");
		String userid = this.getPara("userid")==null?"":this.getPara("userid");
		String mode = this.getPara("mode")==null?"day":this.getPara("mode");
		String endDate = this.getPara("endDate")==null?"":this.getPara("endDate");
		PersonalService service=new PersonalService();
		if(!userid.equals("")){
			jsonString = jsonData.getSuccessJson(service.pcFindListByUserIdAndModeAndUploadtime(userid, mode,endDate));
		}
		renderText(jsonString);
	}
	/**
	 * ɾ���û��ϴ���ͼƬ(������)
	 * @throws Exception
	 */
	@Before(CrossDomain.class)
	public void DeleteUserPublushList()throws Exception{
		jsonString = jsonData.getJson(2, "�����������");
		String userid = this.getPara("userid")==null?"":this.getPara("userid");
		String aid = this.getPara("aid")==null?"":this.getPara("aid");
		PersonalService service=new PersonalService();
		if(!userid.equals("")&&!aid.equals("")){
			if(service.isCanDel(userid, aid)){
				if(service.deletePhoto(aid,userid)){
					jsonString = jsonData.getSuccessJson();
				}
			}
		}
		renderText(jsonString);
	}
	/*
	 * �����û�id��ͼƬid��ȡͼƬԭͼ��ַ��ѹ����ַ�����ţ�
	 */
	@Before(CrossDomain.class)
	public void GetUserPhotoAddress()throws Exception{
		jsonString = jsonData.getJson(2, "�����������");
		String userid = this.getPara("userid")==null?"":this.getPara("userid");
		String aid = this.getPara("aid")==null?"":this.getPara("aid");
		PersonalService service=new PersonalService();
		if(!userid.equals("")&&!aid.equals("")){
			jsonString = jsonData.getSuccessJson(service.getUserPhotoById(userid, aid));
		}
		renderText(jsonString);
	}
	/*
	 * �����û�id��ͼƬid��ͼƬ�ñ�ע��Ϣ
	 */
	@Before(CrossDomain.class)
	public void SetUserPhotoRemark() throws Exception{
		jsonString = jsonData.getJson(2, "�����������");
		String userid = this.getPara("userid")==null?"":this.getPara("userid");
		String aid = this.getPara("aid")==null?"":this.getPara("aid");
		String remark = this.getPara("remark")==null?"":this.getPara("remark");
		PersonalService service = new PersonalService();
		if(!userid.equals("")&&!aid.equals("")){
			jsonString = jsonData.getSuccessJson(service.setPhotoRemark(userid, aid, remark));
		}
		renderText(jsonString);
	}
	/*  ��������ʹ��
	 * �����û�id��ͼƬid��ȡͼƬԭͼ��ַ�����ţ�
	 */
	@Before(CrossDomain.class)
	public void GetUserPhotoDownLoadAddress()throws Exception{
		jsonString = jsonData.getJson(2, "�����������");
		String userid = this.getPara("userid")==null?"":this.getPara("userid");
		String aid = this.getPara("aid")==null?"":this.getPara("aid");
		PersonalService service=new PersonalService();
		if(!userid.equals("")&&!aid.equals("")){
			jsonString = jsonData.getSuccessJson(service.getUserPhotoAddressById(userid, aid));
		}
		renderText(jsonString);
	}
	/**
	 * �ϴ�����Ƶ
	 */
	@Before(CrossDomain.class)
	public void UploadShortVideo() {
		YinianDataProcess dataProcess = new YinianDataProcess();// ���ݴ�����
		String userid = this.getPara("userid");
		String address = this.getPara("address");
		String md5 = this.getPara("md5")==null?"":this.getPara("md5");
		String createTime = this.getPara("createTime")==null?"":this.getPara("createTime");
		String cover = this.getPara("cover");
		String storage = this.getPara("storage");
		//String time = this.getPara("time");
		//String groupid = this.getPara("groupid");
		//String content = this.getPara("content");
		//String storage = this.getPara("storage");
		//String place = this.getPara("place");
		//String source = this.getPara("source");

		// �жϴ洢�ռ��Ƿ��д�
		//double storagePlace = ((storage == null || storage.equals("")) ? 0.00 : Double.parseDouble(storage));
		cover = (cover == null ? "" : cover);
		//time = (time == null ? "0" : time);

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
			if(!userid.equals("")&&!address.equals("")&&!createTime.equals("")){
				PersonalService service=new PersonalService();
				List<Record> list=service.insertShortVedio(userid, address, md5, createTime, cover, storage, 4);
				if(!list.isEmpty()){
					jsonString = jsonData.getSuccessJson(list);		
				}
			}
			// ֧��ͬʱ�ϴ�������ռ�
		/*	String[] IDs = groupid.split(",");
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
			}*/
		}

		// ���ؽ��
		renderText(jsonString);
	}
//	/**
//	 * �������������
//	 */
//	@Before(CrossDomain.class)
//	public void createPersonalAlbum() throws Exception{
//		String userid = this.getPara("userid")==null?"":this.getPara("userid");
//		String pename = this.getPara("pename")==null?"":this.getPara("pename");
//		String pepicAddress = this.getPara("pepicAddress")==null?"":this.getPara("pepicAddress");
//		
//		if(!userid.equals("")&&!pename.equals("")&&!pepicAddress.equals("")) {
//			PersonalService service = new PersonalService();
//			jsonString = jsonData.getSuccessJson(service.insertPersonalAlbum(userid, pename, pepicAddress));
//		}
//		
//		renderText(jsonString);
//	}
	/**
	 * �����û�id��ȡ�����Ϣ
	 */
	@Before(CrossDomain.class)
	public void getPersonalAlbum() throws Exception{
		String userid = this.getPara("userid")==null?"":this.getPara("userid");
		
		if(!userid.equals("")) {
			PersonalService service = new PersonalService();
			jsonString = jsonData.getSuccessJson(service.getPersonalAlbumInfo(userid));
		}
		
		renderText(jsonString);
	}
	/**
	 * �����û� id�޸������
	 */
	@Before(CrossDomain.class)
	public void updatePersonalAlbumName() throws Exception{
		String userid = this.getPara("userid")==null?"":this.getPara("userid");
		String pename = this.getPara("pename")==null?"":this.getPara("pename");
		
		if(!userid.equals("")) {
			PersonalService service = new PersonalService();
			jsonString = jsonData.getSuccessJson(service.setPersonalAlbumName(userid, pename));
		}
		
		renderText(jsonString);
	}
	/**
	 * �����û� id�޸�������
	 */
	@Before(CrossDomain.class)
	public void updatePersonalAlbumPic() throws Exception{
		String userid = this.getPara("userid")==null?"":this.getPara("userid");
		String pepic = this.getPara("pepic")==null?"":this.getPara("pepic");
		
		if(!userid.equals("")) {
			PersonalService service = new PersonalService();
			jsonString = jsonData.getSuccessJson(service.setPersonalAlbumPic(userid, pepic));
		}
		
		renderText(jsonString);
	}
}
