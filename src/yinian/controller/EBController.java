package yinian.controller;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import sun.misc.BASE64Decoder;
import yinian.app.YinianDataProcess;
import yinian.common.CommonParam;
import yinian.interceptor.CrossDomain;
import yinian.model.Address;
import yinian.model.Contact;
import yinian.model.Coupon;
import yinian.model.EBOrder;
import yinian.model.Item;
import yinian.model.Receive;
import yinian.model.Redpacket;
import yinian.service.EBService;
import yinian.utils.DES;
import yinian.utils.JsonData;
import yinian.utils.QiniuOperate;
import yinian.model.Cart;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jfinal.aop.Before;
import com.jfinal.core.Controller;
import com.jfinal.kit.JsonKit;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;
import com.jfinal.plugin.activerecord.tx.Tx;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.UploadManager;

@Before(CrossDomain.class)
public class EBController extends Controller {

	private String jsonString; // ���ص�json�ַ���
	private JsonData jsonData = new JsonData(); // json������
	private YinianDataProcess dataProcess = new YinianDataProcess();// ���ݴ�����
	private EBService service = new EBService(); // ����ҵ����
	private EBService TxService = enhance(EBService.class);

	public void test() {
		String isMail = this.getPara("isMail");
		if (isMail != null && isMail.equals("yes")) {
			System.out.println("haha");
		}
	}

	/**
	 * ��ʾ��Ʒ�б�
	 */
	public void ShowGoodsList() {
		List<Record> list = Db
				.find("select ebGoodsID,ebGoodsName,ebGoodsPrice,ebGoodsOrigin,ebGoodsUnit,ebGoodsIntroduce,ebGoodsPic from ebgoods where ebGoodsStatus=0 ");
		jsonString = jsonData.getJson(0, "success", list);
		renderText(jsonString);
	}

	/**
	 * ��ϵ�ͷ�
	 */
	public void ContactCustomerService() {
		String userid = this.getPara("userid");
		String content = this.getPara("content");
		String name = this.getPara("name");
		String wechatID = this.getPara("wechatID");
		String email = this.getPara("email");
		String phonenumber = this.getPara("phonenumber");
		Contact contact = new Contact().set("contactUserID", userid)
				.set("contactContent", content).set("contactName", name)
				.set("contactWechatID", wechatID).set("contactEmail", email)
				.set("contactPhoneNumber", phonenumber);
		jsonString = dataProcess.insertFlagResult(contact.save());
		renderText(jsonString);
	}

	/**
	 * ��ʾ�ջ���ַ�б�
	 */
	public void ShowAllAddress() {
		String userid = this.getPara("userid");
		List<Record> list = Db
				.find("select addressID,addressReceiver,addressPhoneNumber,addressProvince,addressCity,addressArea,addressDetail,isDefault from address where addressUserID="
						+ userid + " and addressStatus=0 ");
		jsonString = jsonData.getSuccessJson(list);
		renderText(jsonString);
	}

	/**
	 * ��ʾ�û�Ĭ���ջ���ַ
	 */
	public void ShowUserFirstAddress() {
		String userid = this.getPara("userid");
		List<Record> list = Db
				.find("select addressID,addressReceiver,addressPhoneNumber,addressProvince,addressCity,addressArea,addressDetail from address where addressUserID="
						+ userid + " and addressStatus=0 and isDefault=1");
		jsonString = jsonData.getSuccessJson(list);
		renderText(jsonString);
	}

	/**
	 * ��ȡ�ջ���ַ����
	 */
	public void GetAddressInfo() {
		String addressID = this.getPara("addressID");
		List<Record> list = Db
				.find("select addressID,addressReceiver,addressPhoneNumber,addressProvince,addressCity,addressArea,addressDetail,isDefault from address where addressID="
						+ addressID + " ");
		jsonString = jsonData.getSuccessJson(list);
		renderText(jsonString);
	}

	/**
	 * ����Ĭ���ջ���ַ
	 */
	@Before(Tx.class)
	public void ChangeDefaultAddress() {
		String userid = this.getPara("userid");
		String addressID = this.getPara("addressID");
		Db.update("update address set isDefault=0 where addressUserID="
				+ userid + " ");
		int count = Db.update("update address set isDefault=1 where addressID="
				+ addressID + " ");
		jsonString = dataProcess.updateFlagResult(count == 1);
		renderText(jsonString);
	}

	/**
	 * �����ջ���ַ
	 */
	@Before(Tx.class)
	public void ManageAddress() {
		String userid = this.getPara("userid");
		String type = this.getPara("type");
		String addressID = this.getPara("addressID");
		String receiver = this.getPara("receiver");
		String phone = this.getPara("phone");
		String province = this.getPara("province");
		String city = this.getPara("city");
		String area = this.getPara("area");
		String detail = this.getPara("detail");

		Address address = new Address();
		switch (type) {
		case "add":
			address.set("addressUserID", userid)
					.set("addressReceiver", receiver)
					.set("addressPhoneNumber", phone)
					.set("addressProvince", province).set("addressCity", city)
					.set("addressArea", area).set("addressDetail", detail);
			// �ж��û��Ƿ����ջ���ַ��û�еĻ�������ַΪĬ�ϵ�ַ
			List<Record> judgeList = Db
					.find("select * from address where addressUserID=" + userid
							+ " and addressStatus=0 ");
			if (judgeList.size() == 0) {
				address.set("isDefault", 1);
			}
			if (address.save()) {
				List<Record> list = dataProcess.makeSingleParamToList(
						"addressID", address.get("addressID").toString());
				jsonString = jsonData.getSuccessJson(list);
			} else {
				jsonString = jsonData.getJson(-50, "��������ʧ��");
			}
			break;
		case "modify":
			Address updateAddress = new Address().findById(addressID);
			updateAddress.set("addressReceiver", receiver)
					.set("addressPhoneNumber", phone)
					.set("addressProvince", province).set("addressCity", city)
					.set("addressArea", area).set("addressDetail", detail);
			jsonString = dataProcess.updateFlagResult(updateAddress.update());
			break;
		case "delete":
			Address deleteAddress = new Address().findById(addressID);
			deleteAddress.set("addressStatus", 1);
			// �ж�������ɾ���ĵ�ַ�Ƿ�ΪĬ�ϵ�ַ
			String isDefault = deleteAddress.get("isDefault").toString();
			if (isDefault.equals("1")) {
				// �ж��û��Ƿ����ջ���ַ���еĻ�����һ������ΪĬ�ϵ�ַ
				List<Record> deleteJudgeList = Db
						.find("select * from address where addressUserID="
								+ userid + "  and addressStatus=0 ");
				if (deleteJudgeList.size() != 0) {
					String id = deleteJudgeList.get(0).get("addressID")
							.toString();
					Address address1 = new Address().findById(id);
					address1.set("isDefault", 1);
					address1.update();
				}
			}

			jsonString = dataProcess.updateFlagResult(deleteAddress.update());
			break;
		}
		renderText(jsonString);
	}

	/**
	 * ��ʾ�����б�
	 */
	public void ShowOrderList() {
		String userid = this.getPara("userid");
		List<Record> orderList = Db
				.find("select ebOrderID,ebOrderNumber,ebOrderTotalNum,ebGoodsOrigin,ebOrderTotalPrice,ebOrderStatus,ebOrderLogisticsCompany,ebOrderLogisticsNumber,ebOrderPlaceTime,"
						+ "addressReceiver,addressPhoneNumber,addressProvince,addressCity,addressArea,addressDetail,ebGoodsID "
						+ "from address,eborders,ebgoods,items "
						+ "where ebOrderID=itemEBOrderID and itemEBGoodsID=ebGoodsID and ebOrderAddressID=addressID and ebOrderUserID="
						+ userid + " order by ebOrderID desc ");

		for (Record record : orderList) {
			String ebOrderID = record.get("ebOrderID").toString();
			List<Record> itemList = Db
					.find("select itemID,itemNum,ebGoodsID,ebGoodsName,ebGoodsEnName,ebGoodsPrice,ebGoodsUnit,ebGoodsPic "
							+ "from ebgoods,items where ebGoodsID=itemEBGoodsID and itemEBOrderID="
							+ ebOrderID + " ");
			record.set("item", itemList);
		}
		orderList = dataProcess.changeOrderStatusIntoWord(orderList);
		jsonString = jsonData.getSuccessJson(orderList);
		renderText(jsonString);
	}

	/**
	 * ��ӹ�����Ʒ
	 */
	@Before(Tx.class)
	public void AddItem() {
		String userid = this.getPara("userid");
		String goodsID = this.getPara("goodsID");
		String picture = this.getPara("picture");
		String audio = this.getPara("audio");
		String audioTime = this.getPara("audioTime");
		String from = this.getPara("from");
		String to = this.getPara("to");
		String content = this.getPara("content");
		String cover = this.getPara("cover");
		String result = this.getPara("result");

		// ͼƬ����
		if (!(picture.equals("")) && picture != null) {
			picture = dataProcess.EBPicStringProcess(picture);
		}
		Item item = new Item();
		switch (goodsID) {
		case "1":
			// ��������Ƭ
			// ��������
			audio = dataProcess.singleOpenResourcePrefix(audio);
			result = CommonParam.qiniuOpenAddress + result;
			item.set("itemEBGoodsID", goodsID).set("itemNum", 1)
					.set("itemPic", picture).set("itemAudio", audio)
					.set("itemText", content).set("itemFrom", from)
					.set("itemTo", to).set("itemCover", cover)
					.set("itemAudioTime", audioTime).set("itemResult", result);
			break;
		case "2":
			// lomo��
			item.set("itemEBGoodsID", goodsID).set("itemNum", 1)
					.set("itemPic", picture).set("itemAudio", audio)
					.set("itemAudioTime", audioTime);
			break;
		case "3":
			// ͼ����ֽ
			item.set("itemEBGoodsID", goodsID).set("itemNum", 1)
					.set("itemPic", picture).set("itemAudio", audio)
					.set("itemAudioTime", audioTime);
			break;
		case "4":
			// ��Ƭƴͼ
			item.set("itemEBGoodsID", goodsID).set("itemNum", 1)
					.set("itemPic", picture).set("itemAudio", audio)
					.set("itemAudioTime", audioTime);
			break;

		}
		if (item.save()) {
			String itemID = item.get("itemID").toString();
			List<Record> list = Db
					.find("select ebGoodsName,ebGoodsPrice,ebGoodsUnit,ebGoodsPic from ebgoods where ebGoodsID="
							+ goodsID + " ");
			// �����Żݽ��
			List<Record> coupon = Db
					.find("select couponID,couponLimit from coupon where couponLimit in (select max(couponLimit) from coupon where couponType="
							+ goodsID
							+ " and couponStatus=0 and couponUserID="
							+ userid
							+ ") and couponType="
							+ goodsID
							+ " and couponStatus=0 and couponUserID="
							+ userid
							+ " limit 1 ");
			if (coupon.size() == 0) {
				list.get(0).set("itemID", itemID).set("couponPrice", 0);
			} else {
				list.get(0)
						.set("itemID", itemID)
						.set("couponPrice",
								coupon.get(0).get("couponLimit").toString())
						.set("postage", 0);
				// �����Ż�ȯ��Ϣ
				item.set("itemCouponID", coupon.get(0).get("couponID")
						.toString());
				item.update();
			}
			
			// �����ʷ�
			if (goodsID.equals("1")) {
				list.get(0).set("postage", CommonParam.postcardMailFee);
			}

			jsonString = jsonData.getSuccessJson(list);
		} else {
			jsonString = jsonData.getJson(-50, "��������ʧ��");
		}
		renderText(jsonString);
	}

	/**
	 * �µ�
	 */
	@Before(Tx.class)
	public void PlaceOrder() {
		// ��ȡ����
		String userid = this.getPara("userid");
		String addressID = this.getPara("addressID");
		String itemID = this.getPara("itemID");
		String num = this.getPara("num");
		String goodsID = this.getPara("goodsID");// �Ǳ�Ҫ����
		String isMail = this.getPara("isMail"); // �Ƿ��� ����ֵ:yes,no
		// ��ȡ����
		Item item = new Item().findById(itemID);
		// ���㶩���ܼ�
		String goodsPrice = Db
				.findFirst(
						"select ebGoodsPrice from ebgoods where ebGoodsID="
								+ item.get("itemEBGoodsID").toString() + " ")
				.get("ebGoodsPrice").toString();
		double totalPrice = ((Double.parseDouble(goodsPrice)) * (Double
				.parseDouble(num)));
		if (isMail != null && isMail.equals("yes")) {
			// �ܼ��м����˷�
			totalPrice += CommonParam.postcardMailFee;
		}

		// ��ȡ�Ż�ȯ�۸�
		String couponID = item.get("itemCouponID").toString();
		Double couponLimit = 0.00;
		if (!couponID.equals("0")) {
			Coupon coupon = new Coupon().findById(couponID);
			couponLimit = Double.parseDouble(coupon.get("couponLimit")
					.toString());
		}
		// �ܷ���кϳɷ�׵��жϱ�־���ڵ�һ���µ���ֵ�ĳ�true������кϳ�
		boolean canComposeFlag = false;
		String globalOrderNumber = null;
		// �ж��Ƿ��Ѿ��µ������µ��Ļ���������������ֻ���ĵ�ַ�������������û�����
		if (item.get("itemEBOrderID") == null) {
			EBOrder order = new EBOrder();

			// ��ȡ������
			String orderNumber = service.createOrderNumber();
			// ���涩��
			order.set("ebOrderNumber", orderNumber)
					.set("ebOrderUserID", userid)
					.set("ebOrderAddressID", addressID)
					.set("ebOrderTotalNum", num)
					.set("ebOrderTotalPrice", totalPrice);
			if (isMail != null && isMail.equals("yes")) {
				// �����м����ʷѺ���Ҫ�ʼ�
				order.set("ebOrderPostFee", CommonParam.postcardMailFee).set(
						"ebOrderIsMail", 1);
			}
			if (order.save()) {
				String orderID = order.get("ebOrderID").toString();

				// *******�ж���Ʒ�����ǲ��ǵ���5�������޸�item�ڵ��ֶΣ��˵ط�����ʱ��*******//
				if (goodsID != null && !(goodsID.equals(""))
						&& goodsID.equals("5")) {
					item.set("itemEBGoodsID", 5);
				}
				// ************************End*****************************//

				// ������Ʒ��Ŀ
				item.set("itemEBOrderID", orderID).set("itemNum", num);
				if (item.update()) {
					Record record = new Record().set("ebOrderID", orderID)
							.set("ebOrderTotalPrice", totalPrice)
							.set("ebOrderNumber", orderNumber)
							.set("couponLimit", couponLimit);

					// *************�����LOMO����������������Ԥ������ͼƬ***********//
					if (goodsID != null && !(goodsID.equals(""))
							&& goodsID.equals("2")) {
						String itemPic = item.get("itemPic").toString();
						dataProcess.sentNetworkRequest(
								CommonParam.LomoCardPictureDownload, itemPic);
					}
					// **********************End*****************************//

					List<Record> result = new ArrayList<Record>();
					result.add(record);
					jsonString = jsonData.getSuccessJson(result);

					// �ϳɱ�־��Ϊtrue
					canComposeFlag = true;
					globalOrderNumber = orderNumber;
				} else {
					jsonString = jsonData.getJson(-51, "��������ʧ��");
				}
			} else {
				jsonString = jsonData.getJson(-50, "��������ʧ��");
			}
		} else {

			String orderID = item.get("itemEBOrderID").toString();
			EBOrder order = new EBOrder().findById(orderID);
			String orderNumber = order.get("ebOrderNumber").toString();
			order.set("ebOrderAddressID", addressID)
					.set("ebOrderTotalNum", num)
					.set("ebOrderTotalPrice", totalPrice);
			if (order.update()) {
				Record record = new Record().set("ebOrderID", orderID)
						.set("ebOrderTotalPrice", totalPrice)
						.set("ebOrderNumber", orderNumber)
						.set("couponLimit", couponLimit);
				List<Record> temp = new ArrayList<Record>();
				temp.add(record);
				jsonString = jsonData.getSuccessJson(temp);
			} else {
				jsonString = jsonData.getJson(-51, "��������ʧ��");
			}
		}
		renderText(jsonString);

		// ���ݷ��غ��������������Ƭ�����з�׵ĺϳ�
		String itemEBGoodsID = item.get("itemEBGoodsID").toString();
		if (itemEBGoodsID.equals("1") && canComposeFlag) {
			service.ComposePostcardBottom(itemID, globalOrderNumber);
		}
	}

	/**
	 * ���ﳵ����
	 */
	public void BuyGoodsByCart() {
		// ��ȡ����
		String userid = this.getPara("userid");
		String addressID = this.getPara("addressID");
		String itemInfo = this.getPara("itemInfo");
		String isMail = this.getPara("isMail");// �Ƿ��� ����ֵ:yes,no,����Ƭ���е�ֵ

		// ����item����ȡ��Ӧ��List<Record>����
		List<Record> itemList = service.parseItemInfoString(itemInfo);

		// ���㶩���ܼ�,��Ʒ����
		Record totalRecord = service
				.getTotalNumAndGoodsPriceFromItemList(itemList);
		double totalPrice = totalRecord.getDouble("totalPrice");
		int totalNum = totalRecord.getInt("totalNum");

		// �����Ƿ������˷�
		if (isMail != null && isMail.equals("yes")) {
			totalPrice += CommonParam.postcardMailFee;
		}

		// ��ȡ������
		String orderNumber = service.createOrderNumber();
		// ��¼��������
		EBOrder order = new EBOrder();
		order.set("ebOrderNumber", orderNumber).set("ebOrderUserID", userid)
				.set("ebOrderAddressID", addressID)
				.set("ebOrderTotalNum", totalNum)
				.set("ebOrderTotalPrice", totalPrice);
		if (isMail != null && isMail.equals("yes")) {
			// �����м����ʷѺ���Ҫ�ʼ�
			order.set("ebOrderPostFee", CommonParam.postcardMailFee).set(
					"ebOrderIsMail", 1);
		}

		// ���ɶ���
		if (order.save()) {
			// ��ȡ����ID
			String orderID = order.get("ebOrderID").toString();

			// ����itemList��item���ݲ����ظ��º�Ľ��
			itemList = TxService.conbineItemListWithOrderID(orderID, itemList);

			if (itemList == null) {
				// Ϊnull˵����������ִ��ʧ��
				jsonString = jsonData.getJson(-51, "��������ʧ��");
			} else {
				Record record = new Record().set("ebOrderID", orderID)
						.set("ebOrderTotalPrice", totalPrice)
						.set("ebOrderNumber", orderNumber)
						.set("couponLimit", null);
				List<Record> result = new ArrayList<Record>();
				result.add(record);
				jsonString = jsonData.getSuccessJson(result);
			}
		} else {
			jsonString = jsonData.getJson(-50, "��������ʧ��");
		}
		renderText(jsonString);
		
		// ���ݷ��ص�ǰ�˺󣬺�̨����ִ�����ݴ���
		service.dataprocessAfterPlaceOrder(orderNumber, itemList);
		
		
	}

	/**
	 * ֧���ɹ�
	 */
	public void PaySuccess() {
		// ��ȡ����
		String userid = this.getPara("userid");
		String ebOrderID = this.getPara("ebOrderID");
		String payWay = this.getPara("payWay");
		// ����֧���ɹ�ʱ��
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String date = sdf.format(new Date());
		int count = Db
				.update("update eborders set ebOrderStatus=1,ebOrderPayTime='"
						+ date + "',ebOrderPayWay='" + payWay
						+ "' where ebOrderID=" + ebOrderID + " ");

		// ���Ż�ȯ��Ϣ�ĳ���ʹ��
		List<Record> list = Db
				.find("select itemCouponID from items where itemEBOrderID="
						+ ebOrderID + " ");
		for (Record record : list) {
			String couponID = record.get("itemCouponID").toString();
			if (!(couponID.equals("0"))) {
				Db.update("update coupon set couponStatus=1 where couponID="
						+ couponID + " ");
			}
		}

		jsonString = dataProcess.updateFlagResult(count == 1);
		renderText(jsonString);
	}

	/**
	 * ��ȡ��Ʒ��Ŀ����Ʒ����Ϣ
	 */
	public void GetSingleItemInfo() {
		String itemID = this.getPara("itemID");
		List<Record> list = Db
				.find("select ebGoodsID,ebGoodsName,ebGoodsPrice,ebGoodsUnit,ebGoodsOrigin,ebGoodsPic from ebgoods,items where ebGoodsID=itemEBGoodsID and itemID="
						+ itemID + "  ");
		jsonString = jsonData.getSuccessJson(list);
		renderText(jsonString);
	}

	/**
	 * ��ȡ��Ʒ����
	 */
	public void GetGoodsIntroduce() {
		String goodsID = this.getPara("goodsID");
		Record record = Db
				.findFirst("select ebGoodsName,ebIntroducePic,ebIntroduceText,ebIntroduceLongPic from ebgoods where ebGoodsID="
						+ goodsID + " ");
		String[] pic = record.get("ebIntroducePic").toString().split(",");
		String[] text = record.get("ebIntroduceText").toString().split("&nbsp");
		List<Record> list = new ArrayList<Record>();
		for (int i = 0; i < pic.length; i++) {
			Record temp = new Record();
			temp.set("ebIntroducePic", CommonParam.qiniuOpenAddress + pic[i])
					.set("ebIntroduceText", text[i]);
			list.add(temp);
		}
		Record result = new Record()
				.set("info", list)
				.set("name", record.get("ebGoodsName").toString())
				.set("ebIntroduceLongPic",
						record.get("ebIntroduceLongPic").toString());
		List<Record> resultList = new ArrayList<Record>();
		resultList.add(result);
		jsonString = jsonData.getSuccessJson(resultList);
		renderText(jsonString);
	}

	/**
	 * ��ʾ������ڶ�����Ϣ
	 */
	public void ShowOrderInfo() {
		String goodsID = this.getPara("goodsID");
		List<Record> list = Db
				.find("select unickname,ebOrderNumber,addressReceiver,addressPhoneNumber,addressProvince,addressCity,"
						+ "addressArea,addressDetail,itemResult,itemCover,itemNum,itemPic,itemAudio,itemText,itemFrom,itemTo,ebOrderStatus "
						+ "from users,eborders,ebgoods,items,address "
						+ "where userid=ebOrderUserID and ebOrderID=itemEBOrderID and itemEBGoodsID=ebGoodsID "
						+ "and ebOrderAddressID=addressID and ebGoodsID="
						+ goodsID + " and ebOrderStatus in(1) ");
		list = dataProcess.changeOrderStatusIntoWord(list);
		for (Record record : list) {
			String addressProvince = record.get("addressProvince").toString();
			String addressCity = record.get("addressCity").toString();
			String addressArea = record.get("addressArea").toString();
			String addressDetail = record.get("addressDetail").toString();
			String address = addressProvince + addressCity + addressArea
					+ addressDetail;
			record.remove("addressProvince");
			record.remove("addressCity");
			record.remove("addressArea");
			record.remove("addressDetail");
			record.set("address", address);
		}
		jsonString = jsonData.getSuccessJson(list);
		renderText(jsonString);
	}

	/**
	 * ��ʾ�����ڵ�������Ƭ
	 */
	public void ShowAllPhotosInOrder() {
		String orderNumber = this.getPara("orderNumber");
		List<Record> list = Db
				.find("select itemPic from items,eborders where ebOrderID=itemEBOrderID and ebOrderNumber='"
						+ orderNumber + "' ");
		String[] array = list.get(0).get("itemPic").toString().split(",");
		Record record = new Record().set("picture", array);
		List<Record> result = new ArrayList<Record>();
		result.add(record);
		jsonString = jsonData.getSuccessJson(result);
		renderText(jsonString);
	}

	/**
	 * ��ʾ�û�����
	 */
	public void ShowUserFeedback() {
		List<Record> list = Db.find("select * from contact");
		for (Record record : list) {
			String status = record.get("contactStatus").toString();
			if (status.equals("0")) {
				record.set("contactStatus", "δ����");
			} else {
				record.set("contactStatus", "�Ѵ���");
			}
		}
		jsonString = jsonData.getSuccessJson(list);
		renderText(jsonString);
	}

	/**
	 * �޸Ķ���״̬
	 */
	public void ModifyOrderStatus() {
		String orderNum = this.getPara("orderNum");
		String status = this.getPara("status");
		int num = dataProcess.changeOrderStatusIntoNumber(status);
		int count = Db.update("update eborders set ebOrderStatus=" + num
				+ " where ebOrderNumber='" + orderNum + "' ");
		jsonString = dataProcess.updateFlagResult(count == 1);
		renderText(jsonString);
	}

	/**
	 * �޸��û�����״̬
	 */
	public void ModifyContactStatus() {
		String type = this.getPara("type");
		String contactID = this.getPara("contactID");
		Contact contact = new Contact().findById(contactID);
		switch (type) {
		case "δ����":
			contact.set("contactStatus", 0);
			break;
		case "�Ѵ���":
			contact.set("contactStatus", 1);
			break;
		}
		jsonString = dataProcess.updateFlagResult(contact.update());
		renderText(jsonString);
	}

	/**
	 * ��ȡ���״̬
	 */
	public void GetRedPacketStatus() {
		String redPacketID = this.getPara("redPacketID");
		String userid = this.getPara("userid");
		try {
			redPacketID = DES.decryptDES(redPacketID, CommonParam.DESSecretKey);
			List<Record> redPacketList = Db
					.find("select redPacketStatus from redpacket where redPacketID="
							+ redPacketID + " ");
			if (redPacketList.size() == 0) {
				jsonString = jsonData.getJson(2020, "���������");
			} else {
				Record record = new Record();
				List<Record> receiveList = Db
						.find("select receiveStatus from receive where receiveRPid="
								+ redPacketID
								+ " and receiveUserID="
								+ userid
								+ " ");
				if (receiveList.size() == 0) {
					record.set("status", "δ��ȡ");
				} else {
					String receiveStatus = receiveList.get(0)
							.get("receiveStatus").toString();
					switch (receiveStatus) {
					case "0":
						record.set("status", "����ȡ");
						break;
					case "1":
						record.set("status", "�ѳ�ʱ");
						break;
					case "2":
						record.set("status", "δ��ȡ");
						break;
					}
				}
				List<Record> list = new ArrayList<Record>();
				list.add(record);
				jsonString = jsonData.getSuccessJson(list);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			jsonString = jsonData.getJson(1014, "����ʧ��");
		}

		renderText(jsonString);
	}

	/**
	 * ��ȡ���
	 */
	public void ReceiveRedPacket() {
		String redPacketID = this.getPara("redPacketID");
		String userid = this.getPara("userid");
		String isOverTime = this.getPara("isOverTime");
		String phonenumber = this.getPara("phonenumber");
		try {
			redPacketID = DES.decryptDES(redPacketID, CommonParam.DESSecretKey);
			Receive receive = new Receive();
			if (isOverTime.equals("0")) {
				// δ��ʱ
				// �ж��Ƿ��Ѿ���ȡ��
				List<Record> receiveList = Db
						.find("select receiveStatus from receive where receiveRPid="
								+ redPacketID
								+ " and receiveUserID="
								+ userid
								+ " ");
				if (receiveList.size() == 0) {
					receive.set("receiveRPid", redPacketID)
							.set("receiveUserID", userid)
							.set("receiveStatus", 0);

					if (receive.save()) {
						String receiveID = receive.get("receiveID").toString();
						// �����Ż�ȯ
						Record couponRecord = service
								.CreatShareRedPacketCoupon();
						// �����Ż�ȯ����
						couponRecord.set("couponReceiveID", receiveID).set(
								"couponUserID", userid);
						Coupon coupon = new Coupon().put(couponRecord);
						coupon.save();
						// �ж��ֻ���΢��
						int judge = service.judgePhoneAndWechatIsBind(userid,
								phonenumber);
						if (judge != 0 && judge != -2) {
							// �ų��Ѱ󶨵����
							if (judge == -1) {
								// δ�󶨣����ֻ���΢��
								service.bindPhoneToUserid(userid, phonenumber);
							} else {
								// ������һ���˻�
								receive.set("receiveUserID", judge);
								receive.remove("receiveID");
								if (receive.save()) {
									// �����˻�Ҳ�����Ż�ȯ����
									receiveID = receive.get("receiveID")
											.toString();
									couponRecord.set("couponReceiveID",
											receiveID).set("couponUserID",
											judge);
									coupon = new Coupon().put(couponRecord);
									coupon.save();
								}
							}
						}
						// ƴ�ӷ�����Ϣ
						String name = couponRecord.get("couponName").toString();
						String price = couponRecord.get("couponLimit")
								.toString();
						// �����Ż�ȯ��Ϣ
						Record record = new Record().set("result", name + price
								+ "Ԫ�Ż�ȯ");
						List<Record> list = new ArrayList<Record>();
						list.add(record);
						jsonString = jsonData.getSuccessJson(list);
					} else {
						jsonString = jsonData.getJson(-50, "���ݲ��뵽���ݿ�ʧ��");
					}
				} else {
					jsonString = jsonData.getJson(2024, "������ú��");
				}
			} else {
				// ��ʱ
				receive.set("receiveRPid", redPacketID)
						.set("receiveUserID", userid).set("receiveStatus", 1);
				jsonString = dataProcess.insertFlagResult(receive.save());
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			jsonString = jsonData.getJson(1014, "����ʧ��");
		}

		renderText(jsonString);
	}

	/**
	 * ��ʾ�Ż�ȯ�б�
	 */
	public void ShowCouponList() {
		String userid = this.getPara("userid");
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		String today = sdf.format(new Date());
		List<Record> list = Db
				.find("select couponID,couponName,couponLimit,couponRange,couponType,couponDate,couponTime,couponStatus from coupon where couponUserID="
						+ userid + " and couponDate>='"+today+"' and couponStatus=0 ");
		list = dataProcess.changeCouponStatusIntoWord(list);
		jsonString = jsonData.getSuccessJson(list);
		renderText(jsonString);
	}

	/**
	 * ͨ���Ż����ȡ�Ż�ȯ
	 */
	@Before(Tx.class)
	public void GetCouponByCouponCode() {
		String userid = this.getPara("userid");
		String couponCode = this.getPara("couponCode");
		List<Record> redPacketInfo = Db
				.find("select redPacketID,redPacketType,redPacketPriceLimit,redPacketTimeLimit,redPacketStatus from redpacket where redPacketCode='"
						+ couponCode + "' ");
		if (couponCode.equals("����ϵͳĬ�Ϸ�����") || redPacketInfo.size() == 0) {
			jsonString = jsonData.getJson(2021, "�Ż��벻����");
		} else {
			String redPacketID = redPacketInfo.get(0).get("redPacketID")
					.toString();
			String redPacketStatus = redPacketInfo.get(0)
					.get("redPacketStatus").toString();
			if (redPacketStatus.equals("0")) {
				// �Ż�������ʹ���У��ж��û��Ƿ���ȡ
				List<Record> judge = Db
						.find("select receiveStatus from receive where receiveRPid="
								+ redPacketID
								+ " and receiveUserID="
								+ userid
								+ " ");
				if (judge.size() == 0) {
					// ˵��δ��ȡ��ִ����ȡ�ķ���
					Receive receive = new Receive().set("receiveRPid",
							redPacketID).set("receiveUserID", userid);
					if (receive.save()) {
						String receiveID = receive.get("receiveID").toString();
						// �����Ż�ȯ���ݣ����뵽���ݿ��в�����
						TxService.CreatAgentRedPacketCoupon(redPacketInfo,
								userid, receiveID);

						//��һ���Ժ����״̬����Ϊ����ȡ
						String redPacketType = redPacketInfo.get(0).get("redPacketType").toString();
						if(redPacketType.equals("3")){
							Redpacket redPacket = new Redpacket().findById(redPacketID);
							redPacket.set("redPacketStatus", 1);
							redPacket.update();
						}
						
						List<Record> list = Db
								.find("select couponID,couponName,couponLimit,couponRange,couponType,couponDate,couponTime,couponStatus from coupon where couponReceiveID="
										+ receiveID + " ");
						jsonString = jsonData.getSuccessJson(list);

					}
				} else {
					String receiveStatus = judge.get(0).get("receiveStatus")
							.toString();
					switch (receiveStatus) {
					case "0":
						jsonString = jsonData.getJson(2024, "������ú��");
						break;
					case "1":
						jsonString = jsonData.getJson(2025, "��ȡ���������ʱ");
						break;
					default:
						jsonString = jsonData.getJson(-54, "δ֪�쳣");
						break;
					}
				}

			} else {
				switch (redPacketStatus) {
				case "1":
					jsonString = jsonData.getJson(2022, "�Ż�ȯ������");
					break;
				case "2":
					jsonString = jsonData.getJson(2023, "�Ż����ѹ���");
					break;
				default:
					jsonString = jsonData.getJson(-54, "δ֪�쳣");
					break;
				}
			}

		}
		renderText(jsonString);

	}

	/**
	 * ���ɷ�����
	 */
	public void CreateShareRedPacket() {
		String ebOrderID = this.getPara("ebOrderID");
		String userid = this.getPara("userid");
		Redpacket red = new Redpacket().set("redPacketOwner", userid)
				.set("redPacketType", 0).set("redPacketOrderID", ebOrderID);
		if (red.save()) {
			String redPacketID = red.get("redPacketID").toString();
			try {
				redPacketID = DES.encryptDES(redPacketID,
						CommonParam.DESSecretKey);
				jsonString = jsonData.getSuccessJson(dataProcess
						.makeSingleParamToList("redPacketID", redPacketID));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				jsonString = jsonData.getJson(1029, "����ʧ��");
			}
		} else {
			jsonString = jsonData.getJson(-50, "��������ʧ��");
		}
		renderText(jsonString);
	}

	/**
	 * Web�˽�ȡͼƬ��ͼƬ�ϴ�����ţ�󷵻ص�ַ
	 * 
	 * @throws FileNotFoundException
	 */
	public void UploadCutPictureToQiniuAndReturnURL()
			throws FileNotFoundException {
		// ��ȡ����
		String data = this.getPara("data");
		System.out.println(data);
		// ��ȡͼƬbase64�ַ���
		JSONObject jo = JSONObject.parseObject(data);
		String imgString = jo.getString("imgstring");
		// �����ļ���
		Random random = new Random();
		String filename = String.valueOf(random.nextInt(100000))
				+ String.valueOf(System.currentTimeMillis())
				+ String.valueOf(random.nextInt(100000)) + ".jpg";
		// base64����,���洢ͼƬ
		BASE64Decoder decoder = new BASE64Decoder();
		try {
			// Base64����
			byte[] bytes = decoder.decodeBuffer(imgString);
			for (int i = 0; i < bytes.length; ++i) {
				if (bytes[i] < 0) {// �����쳣����
					bytes[i] += 256;
				}
			}
			// ����jpegͼƬ
			OutputStream out = new FileOutputStream(filename);
			out.write(bytes);
			out.flush();
			out.close();

		} catch (Exception e) {
			System.out.println(e);
		}
		// �ϴ���ţ��
		// �����ϴ�����
		UploadManager uploadManager = new UploadManager();
		QiniuOperate qiniu = new QiniuOperate();
		// ����put�����ϴ�
		try {
			Response res = uploadManager.put(filename, filename,
					qiniu.getUploadToken());
			// ��ӡ���ص���Ϣ
			System.out.println(res.bodyString());
		} catch (QiniuException e) {
			Response r = e.response;
			// ����ʧ��ʱ��ӡ���쳣����Ϣ
			System.out.println(r.toString());
			try {
				// ��Ӧ���ı���Ϣ
				System.out.println(r.bodyString());
			} catch (QiniuException e1) {
				// ignore
			}
		}
		// ɾ���ļ�
		File file = new File(filename);
		file.delete();
		// ���ؽ��
		filename = CommonParam.qiniuOpenAddress + filename;
		System.out.println(filename);
		List<Record> list = dataProcess.makeSingleParamToList("filePath",
				filename);
		jsonString = jsonData.getSuccessJson(list);
		renderText(jsonString);

	}

	/**
	 * �鿴���ﳵ
	 */
	public void ShowCart() {
		String userid = this.getPara("userid");
		List<Record> list = Db
				.find("select cartID,ebGoodsName,ebGoodsEnName,ebGoodsPrice,ebGoodsPic,ebGoodsUnit,cartItemNum,cartItemID,ebGoodsID from cart,items,ebgoods where cartItemID=itemID and itemEBGoodsID=ebGoodsID and cartUserID="
						+ userid + " and cartStatus=0 and ebGoodsStatus=0  ");

		Set<Integer> set = new HashSet<Integer>();
		ArrayList<List<Record>> array = new ArrayList<List<Record>>();
		// �����е����⣬��Ӧ��ֱ�Ӹ���20��Ӧ�����ɶ�̬�ģ���ȡ��ƷID�����ֵ����
		List<Record> no = new ArrayList<Record>();
		for (int i = 0; i < 20; i++) {
			array.add(no);
		}

		for (Record record : list) {
			boolean flag = false;
			List<Record> temp = new ArrayList<Record>();
			int goodsID = Integer.parseInt(record.get("ebGoodsID").toString());

			flag = set.contains(goodsID);
			if (flag) {
				array.get(goodsID).add(record);
			} else {
				temp.add(record);
				array.set(goodsID, temp);
			}
			set.add(goodsID);
		}
		// ƴ�ӽ��
		Iterator<Integer> it = set.iterator();
		List<Record> result = new ArrayList<Record>();
		while (it.hasNext()) {
			int id = it.next();
			List<Record> temp = array.get(id);
			String goodsName = temp.get(0).get("ebGoodsName").toString();
			Record record = new Record().set("goodsInfo", temp).set(
					"goodsName", goodsName);
			result.add(record);
		}

		jsonString = jsonData.getSuccessJson(result);
		renderText(jsonString);
	}

	/**
	 * �鿴��Ʒ����
	 */
	public void GetItemDetail() {
		String itemID = this.getPara("itemID");
		List<Record> list = Db
				.find("select itemEBGoodsID,itemPic,itemCover,itemAudio,itemAudioTime,itemText,itemFrom,itemTo from items where itemID="
						+ itemID + "  ");
		String[] array = list.get(0).get("itemPic").toString().split(",");
		list.get(0).set("itemPic", array);
		jsonString = jsonData.getSuccessJson(list);
		renderText(jsonString);
	}

	/**
	 * �����ﳵ
	 */
	public void ManageCart() {
		String userid = this.getPara("userid");
		String itemID = this.getPara("itemID");
		String cartID = this.getPara("cartID");
		String modifyInfo = this.getPara("modifyInfo");
		String type = this.getPara("type");
		Cart cart = null;
		switch (type) {
		case "add":
			List<Record> list = Db.find("select * from cart where cartUserID="
					+ userid + " and cartItemID=" + itemID
					+ " and cartStatus=0 ");
			if (list.size() == 0) {
				cart = new Cart().set("cartUserID", userid)
						.set("cartItemID", itemID).set("cartItemNum", 1);
				jsonString = dataProcess.insertFlagResult(cart.save());
			} else {
				String tempCartID = list.get(0).get("cartID").toString();
				cart = new Cart().findById(tempCartID);
				int cartGoodsNum = Integer.parseInt(cart.get("cartItemNum")
						.toString());
				cart.set("cartItemNum", cartGoodsNum + 1);
				jsonString = dataProcess.updateFlagResult(cart.update());
			}
			break;
		case "modify":
			// ����modifyInfo����
			boolean flag = TxService.modifyCartInfo(userid, modifyInfo);
			jsonString = dataProcess.updateFlagResult(flag);
			break;
		case "remove":
			cart = new Cart().findById(cartID);
			cart.set("cartStatus", 1);
			jsonString = dataProcess.deleteFlagResult(cart.update());
			break;
		}
		renderText(jsonString);
	}

	/**
	 * ��ȡ����ʱʹ����Ϣ
	 */
	@Before(Tx.class)
	public void GetInfoBeforePay() {
		// ��ȡ����
		String userid = this.getPara("userid");
		String itemInfo = this.getPara("itemInfo");
		// ���������б�
		List<Record> result = new ArrayList<Record>();
		Record resultRecord = new Record();

		// ��ȡĬ���ջ���ַ
		List<Record> addressList = Db
				.find("select addressID,addressReceiver,addressPhoneNumber,addressProvince,addressCity,addressArea,addressDetail from address where addressUserID="
						+ userid + " and addressStatus=0 and isDefault=1 ");
		resultRecord.set("address", addressList);

		// ��ȡ�ʷ���Ϣ
		List<Record> postFeeList = dataProcess.makeSingleParamToList(
				"postCardFee", CommonParam.postcardMailFee);
		resultRecord.set("postFee", postFeeList);

		// ��ȡ�Ż�ȯ��Ϣ
		// ����item����ȡ��Ӧ��List<Record>����
		JSONObject jo = JSONObject.parseObject(itemInfo);
		JSONArray ja = jo.getJSONArray("data");

		// ����Ʒ�����л�ȡ�Ż�ȯ��Ϣ
		List<Record> couponList = new ArrayList<Record>();
		for (int i = 0; i < ja.size(); i++) {
			double totalCouponPrice = 0.00;
			JSONObject tempObject = ja.getJSONObject(i);
			int goodsID = tempObject.getIntValue("ebGoodsID");
			JSONArray tempArray = tempObject.getJSONArray("itemInfo");

			// �����Ż�ȯ���
			int size = tempArray.size();
			List<Record> coupon = Db
					.find("select couponID,couponLimit from coupon where couponType="
							+ goodsID
							+ " and couponStatus=0 and couponUserID="
							+ userid
							+ " order by couponLimit desc limit "
							+ size + " ");
			// �����Ż�ȯ�ܶ��������Ӧ��item
			for (int j = 0; j < coupon.size(); j++) {
				int couponID = Integer.parseInt(coupon.get(j).get("couponID")
						.toString());
				double couponLimit = Double.parseDouble(coupon.get(j)
						.get("couponLimit").toString());
				// ����۸�
				totalCouponPrice += couponLimit;
				// item�����в����Ż�ȯ����
				JSONObject itemObject = tempArray.getJSONObject(j);
				int itemID = itemObject.getIntValue("itemID");
				Item item = new Item().findById(itemID);
				item.set("itemCouponID", couponID);
				item.update();
			}
			Record record = new Record().set("totalCouponPrice",
					totalCouponPrice).set("goodsID", goodsID);
			couponList.add(record);
		}
		resultRecord.set("coupon", couponList);
		result.add(resultRecord);
		jsonString = jsonData.getSuccessJson(result);
		renderText(jsonString);

	}

	// /**
	// * �µ�
	// */
	// @Before(Tx.class)
	// public void PlaceOrder() {
	// // ��ȡ����
	// String userid = this.getPara("userid");
	// String addressID = this.getPara("addressID");
	// String itemID = this.getPara("itemID");
	// String num = this.getPara("num");
	// String goodsID = this.getPara("goodsID");
	// // ��ȡ����
	// Item item = new Item().findById(itemID);
	// // ���㶩���ܼ�
	// String goodsPrice = Db
	// .findFirst(
	// "select ebGoodsPrice from ebgoods where ebGoodsID="
	// + item.get("itemEBGoodsID").toString() + " ")
	// .get("ebGoodsPrice").toString();
	// double totalPrice = ((Double.parseDouble(goodsPrice)) * (Double
	// .parseDouble(num)));
	// // ��ȡ�Ż�ȯ�۸�
	// String couponID = item.get("itemCouponID").toString();
	// Double couponLimit = 0.00;
	// if (!couponID.equals("0")) {
	// Coupon coupon = new Coupon().findById(couponID);
	// couponLimit = Double.parseDouble(coupon.get("couponLimit")
	// .toString());
	// }
	// // �ܷ���кϳɷ�׵��жϱ�־���ڵ�һ���µ���ֵ�ĳ�true������кϳ�
	// boolean canComposeFlag = false;
	// String globalOrderNumber = null;
	// // �ж��Ƿ��Ѿ��µ������µ��Ļ���������������ֻ���ĵ�ַ�������������û�����
	// if (item.get("itemEBOrderID") == null) {
	// EBOrder order = new EBOrder();
	//
	// // ��ȡ������
	// SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
	// String date = sdf.format(new Date());
	// Random random = new Random();
	// String orderNumber = "YN" + date;
	// // �ж϶������Ƿ��ظ�
	// boolean flag = true;
	// List<Record> list = Db.find("select ebOrderNumber from eborders ");
	// while (flag) {
	// int temp = random.nextInt(100000000);
	// orderNumber += temp;
	// for (Record record : list) {
	// String number = record.getStr("ebOrderNumber");
	// if (number.equals(orderNumber)) {
	// flag = true;
	// break;
	// } else {
	// flag = false;
	// }
	// }
	// }
	// // ���涩��
	// order.set("ebOrderNumber", orderNumber)
	// .set("ebOrderUserID", userid)
	// .set("ebOrderAddressID", addressID)
	// .set("ebOrderTotalNum", num)
	// .set("ebOrderTotalPrice", totalPrice);
	// if (order.save()) {
	// String orderID = order.get("ebOrderID").toString();
	//
	// // *******�ж���Ʒ�����ǲ��ǵ���5�������޸�item�ڵ��ֶΣ��˵ط�����ʱ��*******//
	// if (goodsID != null && !(goodsID.equals(""))
	// && goodsID.equals("5")) {
	// item.set("itemEBGoodsID", 5);
	// }
	// // ************************End*****************************//
	//
	// // ������Ʒ��Ŀ
	// item.set("itemEBOrderID", orderID).set("itemNum", num);
	// if (item.update()) {
	// Record record = new Record().set("ebOrderID", orderID)
	// .set("ebOrderTotalPrice", totalPrice)
	// .set("ebOrderNumber", orderNumber)
	// .set("couponLimit", couponLimit);
	//
	// // *************�����LOMO����������������Ԥ������ͼƬ***********//
	// if (goodsID != null && !(goodsID.equals(""))
	// && goodsID.equals("2")) {
	// String itemPic = item.get("itemPic").toString();
	// dataProcess.sentNetworkRequest(
	// CommonParam.LomoCardPictureDownload, itemPic);
	// }
	// // **********************End*****************************//
	//
	// List<Record> result = new ArrayList<Record>();
	// result.add(record);
	// jsonString = jsonData.getSuccessJson(result);
	//
	// // �ϳɱ�־��Ϊtrue
	// canComposeFlag = true;
	// globalOrderNumber = orderNumber;
	// } else {
	// jsonString = jsonData.getJson(-51, "��������ʧ��");
	// }
	// } else {
	// jsonString = jsonData.getJson(-50, "��������ʧ��");
	// }
	// } else {
	//
	// String orderID = item.get("itemEBOrderID").toString();
	// EBOrder order = new EBOrder().findById(orderID);
	// String orderNumber = order.get("ebOrderNumber").toString();
	// order.set("ebOrderAddressID", addressID)
	// .set("ebOrderTotalNum", num)
	// .set("ebOrderTotalPrice", totalPrice);
	// if (order.update()) {
	// Record record = new Record().set("ebOrderID", orderID)
	// .set("ebOrderTotalPrice", totalPrice)
	// .set("ebOrderNumber", orderNumber)
	// .set("couponLimit", couponLimit);
	// List<Record> temp = new ArrayList<Record>();
	// temp.add(record);
	// jsonString = jsonData.getSuccessJson(temp);
	// } else {
	// jsonString = jsonData.getJson(-51, "��������ʧ��");
	// }
	// }
	// renderText(jsonString);
	//
	// // ���ݷ��غ��������������Ƭ�����з�׵ĺϳ�
	// String itemEBGoodsID = item.get("itemEBGoodsID").toString();
	// if (itemEBGoodsID.equals("1") && canComposeFlag) {
	// service.ComposePostcardBottom(itemID, globalOrderNumber);
	// }
	// }

}
