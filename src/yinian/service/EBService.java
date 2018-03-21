package yinian.service;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

import yinian.app.YinianDataProcess;
import yinian.common.CommonParam;
import yinian.draw.ComposePicture;
import yinian.model.Cart;
import yinian.model.Coupon;
import yinian.model.Item;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jfinal.aop.Before;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;
import com.jfinal.plugin.activerecord.tx.Tx;

public class EBService {

	private YinianDataProcess dataProcess = new YinianDataProcess();

	/**
	 * �ж��ֻ���΢�ź����Ƿ�� �Ѱ󶨷���0���Ѵ���������û����ظ��ֻ��������Ӧ��userid��δ�󶨷���-1����userid�Ѱ��������뷵��-2
	 * 
	 * @param userid
	 * @return
	 */
	public int judgePhoneAndWechatIsBind(String userid, String phone) {
		List<Record> list = Db.find("select userid from users where uphone='"
				+ phone + "' ");
		if (list.size() == 0) {
			// û�иú��룬˵��δ��,�жϸ��û�����Ϣ���Ƿ������������
			List<Record> list2 = Db
					.find("select uphone from users where userid=" + userid
							+ " ");
			if (list2.size() == 0) {
				return -1;
			} else {
				return -2;
			}
		} else {
			String temp = list.get(0).get("userid").toString();
			if (temp.equals(userid)) {
				// �Ѱ�
				return 0;
			} else {
				return Integer.parseInt(temp);
			}
		}
	}

	/**
	 * ���ֻ����뵽�û���Ϣ��
	 * 
	 * @param userid
	 * @param phone
	 * @return
	 */
	public boolean bindPhoneToUserid(String userid, String phone) {
		int count = Db.update("update users set uphone='" + phone
				+ "' where userid=" + userid + " ");
		return count == 1;
	}

	/**
	 * ���ɷ������Ż�ȯ
	 */
	public Record CreatShareRedPacketCoupon() {
		List<Record> list = Db
				.find("select ebGoodsID from ebgoods where ebGoodsStatus=0 ");
		int size = list.size();
		int temp = 1 + (int) (Math.random() * size);
		DecimalFormat dcmFmt = new DecimalFormat("0.00");
		Random rand = new Random();
		float f = 0;

		// ���ù���ʱ�䣬Ĭ��Ϊ7��
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, 7);
		Date date = cal.getTime();
		String expired = sdf.format(date);

		Record record = new Record();

		switch (temp) {
		case 1:
			f = (float) (1 + rand.nextFloat() * 2);
			record.set("couponName", "��������Ƭ").set("couponRange", "������������Ƭ");
			// ����Ƭ���۸�Ϊ5Ԫ
			break;
		case 2:
			f = (float) (3 + rand.nextFloat() * 5);
			record.set("couponName", "LOMO��").set("couponRange", "����LOMO��");
			// LOMO�����۸�Ϊ39.8Ԫ
			break;
		case 3:
			f = (float) (3 + rand.nextFloat() * 7);
			record.set("couponName", "��Ƭƴͼ").set("couponRange", "������Ƭƴͼ");
			// ��Ƭƴͼ���۸�Ϊ49.8Ԫ
			break;
		case 4:
			f = (float) (10 + rand.nextFloat() * 10);
			record.set("couponName", "ͼ����ֽ").set("couponRange", "����ͼ����ֽ");
			// ͼ����ֽ���۸�Ϊ458Ԫ
			break;
		}
		String price = dcmFmt.format(f);
		record.set("couponLimit", price).set("couponType", temp)
				.set("couponDate", expired);

		return record;
	}

	/**
	 * ���ɷ������Ż�ȯ
	 */
	@Before(Tx.class)
	public List<Coupon> CreatAgentRedPacketCoupon(List<Record> redPacketInfo,
			String userid, String receiveID) {
		List<Coupon> result = new ArrayList<Coupon>();
		String[] redPacketPriceLimit = redPacketInfo.get(0)
				.get("redPacketPriceLimit").toString().split(",");
		String[] redPacketTimeLimit = redPacketInfo.get(0)
				.get("redPacketTimeLimit").toString().split(",");

		// ���ù���ʱ�䣬Ĭ��Ϊ7��
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

		Coupon coupon;
		for (int i = 0; i < redPacketPriceLimit.length; i++) {
			if (!redPacketPriceLimit[i].equals("0")) {
				Calendar cal = Calendar.getInstance();
				cal.add(Calendar.DATE, Integer.parseInt(redPacketTimeLimit[i]));
				Date date = cal.getTime();
				String expired = sdf.format(date);
				coupon = new Coupon().set("couponUserID", userid)
						.set("couponReceiveID", receiveID)
						.set("couponLimit", redPacketPriceLimit[i])
						.set("couponType", i + 1).set("couponDate", expired);
				coupon = addCouponNameAndRange(coupon, i + 1);
				result.add(coupon);
				coupon.save();
			}
		}
		return result;
	}

	/**
	 * ����Ż�ȯ���ƺ��Ż�ȯʹ�÷�Χ
	 * 
	 * @param coupon
	 * @param goodsID
	 * @return
	 */
	public Coupon addCouponNameAndRange(Coupon coupon, int goodsID) {
		switch (goodsID) {
		case 1:
			coupon.set("couponName", "��������Ƭ").set("couponRange", "������������Ƭ");
			// ����Ƭ���۸�Ϊ5Ԫ
			break;
		case 2:
			coupon.set("couponName", "LOMO��").set("couponRange", "����LOMO��");
			// LOMO�����۸�Ϊ39.8Ԫ
			break;
		case 3:
			coupon.set("couponName", "��Ƭƴͼ").set("couponRange", "������Ƭƴͼ");
			// ��Ƭƴͼ���۸�Ϊ49.8Ԫ
			break;
		case 4:
			coupon.set("couponName", "ͼ����ֽ").set("couponRange", "����ͼ����ֽ");
			// ͼ����ֽ���۸�Ϊ458Ԫ
			break;

		}
		return coupon;

	}

	/**
	 * �ϳɷ��
	 * 
	 * @param itemID
	 * @param orderNumber
	 * @return
	 */
	public boolean ComposePostcardBottom(String itemID, String orderNumber) {
		// ��ȡ����
		Item item = new Item().findById(itemID);
		String picture = item.get("itemPic");
		String audio = item.get("itemAudio");
		String text = item.get("itemText");
		String from = item.get("itemFrom");
		String to = item.get("itemTo");
		// �����ļ���
		String filename = orderNumber
				+ String.valueOf(System.currentTimeMillis()) + ".jpg";
		// ��ȡ�ļ��洢·��
		String temp = "C:/Users/Zad/Desktop/temp/" + filename;
		String path = CommonParam.postcardBottomSaveRelativePath + filename;
		// �ϳ�ͼƬ
		ComposePicture.MakePostcardBottom(from, to, text, picture, audio,
				orderNumber, path);
		// �޸����ݿ�����
		item.set("itemResult", CommonParam.postcardBottomAbsolutePath
				+ filename);
		return item.update();
	}

	/**
	 * ����item�����ַ���
	 */
	public List<Record> parseItemInfoString(String itemInfo) {
		/**
		 * �������� ���� { "data": [ { "ebGoodsID ": 1, "itemInfo": [ { "num": 2,
		 * "itemID":1 }, { "num": 2, "itemID":2 } ] }, { "ebGoodsID ": "2",
		 * "itemInfo": [ { "num": 2, "itemID":3 }, { "num": 2, "itemID":4 } ] },
		 * ] }
		 */
		List<Record> result = new ArrayList<Record>();
		JSONObject jo = JSONObject.parseObject(itemInfo);
		JSONArray ja = jo.getJSONArray("data");

		for (int i = 0; i < ja.size(); i++) {
			JSONObject tempObject = ja.getJSONObject(i);
			JSONArray tempArray = tempObject.getJSONArray("itemInfo");

			for (int j = 0; j < tempArray.size(); j++) {
				int itemID = tempArray.getJSONObject(j).getIntValue("itemID");
				int num = tempArray.getJSONObject(j).getIntValue("num");
				Item item = new Item().findById(itemID);

				Record record = new Record()
						.set("itemID", itemID)
						.set("num", num)
						.set("ebGoodsID",
								Integer.parseInt(item.get("itemEBGoodsID")
										.toString()));
				result.add(record);
			}
		}
		return result;
	}

	/**
	 * ��ȡitem��Ϣ�е���Ʒ�ܼۡ�����
	 * 
	 * @param itemInfo
	 * @return
	 */
	public Record getTotalNumAndGoodsPriceFromItemList(List<Record> list) {
		Double totalPrice = 0.0;
		int totalNum = 0;
		// ��ȡ��ǰ������������Ʒ��ID����Ӧ�۸�
		List<Record> goodsPrice = Db
				.find("select ebGoodsID,ebGoodsPrice from ebgoods where ebGoodsStatus=0");
		// ��Record���洢���ݣ����ٲ�ѯʱ��
		Record goodsRecord = new Record();
		for (Record temp : goodsPrice) {
			goodsRecord.set(temp.get("ebGoodsID").toString(),
					temp.getDouble("ebGoodsPrice"));
		}
		for (Record temp : list) {
			totalNum += temp.getInt("num");
			Item item = new Item().findById(temp.getInt("itemID"));
			String ebGoodsID = item.get("itemEBGoodsID").toString();
			totalPrice = goodsRecord.getDouble(ebGoodsID);
		}
		Record record = new Record().set("totalPrice", totalPrice).set(
				"totalNum", totalNum);
		return record;
	}

	/**
	 * ��ȡ������
	 * 
	 * @return
	 */
	public String createOrderNumber() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		String date = sdf.format(new Date());
		Random random = new Random();
		String orderNumber = "YN" + date;
		// �ж϶������Ƿ��ظ�
		boolean flag = true;
		List<Record> list = Db.find("select ebOrderNumber from eborders ");
		while (flag) {
			int temp = random.nextInt(100000000);
			orderNumber += temp;
			for (Record record : list) {
				String number = record.getStr("ebOrderNumber");
				if (number.equals(orderNumber)) {
					flag = true;
					break;
				} else {
					flag = false;
				}
			}
		}
		return orderNumber;
	}

	/**
	 * ������item�붩��������������item�ѹ���������������һ����item�����¶���
	 * 
	 * @param ebOrderID
	 * @param itemList
	 * @return
	 */
	@Before(Tx.class)
	public List<Record> conbineItemListWithOrderID(String ebOrderID,
			List<Record> itemList) {
		for (Record record : itemList) {
			Item item = new Item().findById(record.get("itemID").toString());
			int num = Integer.parseInt(record.get("num").toString());
			// �ж��Ƿ���Ҫ�½�һ���µ�item
			if (item.get("itemEBOrderID") == null) {
				item.set("itemEBOrderID", ebOrderID).set("itemNum", num);
				if (!item.update()) {
					// ���ɹ�������ѭ��������itemList��գ��ϼ����Դ������ж�
					itemList = null;
					break;
				}
			} else {
				item.remove("itemID").remove("itemEBOrderID");
				item.set("itemEBOrderID", ebOrderID).set("itemNum", num);
				if (!item.save()) {
					// ���ɹ�������ѭ��������itemList��գ��ϼ����Դ������ж�
					itemList = null;
					break;
				}
				// ����itemList
				int itemID = Integer.parseInt(item.get("itemID").toString());
				record.set("itemID", itemID);
			}
		}
		return itemList;
	}

	/**
	 * �µ�������ݴ���
	 * 
	 * @param orderNumber
	 * @param itemList
	 * @return
	 */
	public boolean dataprocessAfterPlaceOrder(String orderNumber,
			List<Record> itemList) {
		for (Record record : itemList) {
			int ebGoodsID = Integer
					.parseInt(record.get("ebGoodsID").toString());
			String itemID = record.get("itemID").toString();
			Item item = new Item().findById(itemID);
			switch (ebGoodsID) {
			case 1:
				// ����Ƭ���ϳɷ��
				ComposePostcardBottom(itemID, orderNumber);
				break;
			case 2:
				// LOMO����������������Ԥ����ͼƬ
				String itemPic = item.get("itemPic").toString();
				dataProcess.sentNetworkRequest(
						CommonParam.LomoCardPictureDownload, itemPic);
				break;
			default:
				break;
			}
		}
		return true;
	}

	/**
	 * �޸Ĺ��ﳵ��Ϣ
	 */
	@Before(Tx.class)
	public boolean modifyCartInfo(String userid, String modifyInfo) {
		boolean flag = true;
		JSONArray array = JSONArray.parseArray(modifyInfo);
		for (int i = 0; i < array.size(); i++) {
			JSONObject object = array.getJSONObject(i);
			int number = object.getIntValue("number");
			int cartID = object.getIntValue("cartID");
			Cart cart = new Cart().findById(cartID);
			String cartUserID = cart.get("cartUserID").toString();
			if (userid.equals(cartUserID)) {
				cart.set("cartItemNum", number);
				flag = cart.update();
				if (!flag) {
					break;
				}
			} else {
				flag = false;
				break;
			}
		}
		return flag;
	}
}
