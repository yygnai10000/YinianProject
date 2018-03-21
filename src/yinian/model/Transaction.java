package yinian.model;

import java.math.BigDecimal;
import java.util.List;

import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Model;
import com.jfinal.plugin.activerecord.Record;

public class Transaction extends Model<Transaction> {
	public static final Transaction dao = new Transaction();

	/**
	 * ��ʼ�����׼�¼
	 * 
	 * @param userid
	 * @return
	 */
	public List<Record> initializeTransactionRecord(String userid) {
		List<Record> list = Db
				.find("select transactionID,transactionMoney,transactionType,transactionTime from transaction where transactionUserID="
						+ userid
						+ " and transactionStatus=0 and transactionStatus='���׳ɹ�' order by transactionID desc limit 10 ");
		return list;
	}

	/**
	 * ���ؽ��׼�¼
	 * 
	 * @param userid
	 * @return
	 */
	public List<Record> loadingTransactionRecord(String userid,
			String transactionID) {
		List<Record> list = Db
				.find("select transactionID,transactionMoney,transactionType,transactionTime from transaction where transactionUserID="
						+ userid
						+ " and transactionID<"
						+ transactionID
						+ " and transactionStatus=0 and transactionStatus='���׳ɹ�' order by transactionID desc limit 10 ");
		return list;
	}

	/**
	 * �������뽻�׼�¼
	 */
	public boolean insertIncomeRecord(String userid, BigDecimal money,
			String type, String data) {
		Transaction tran = new Transaction().set("transactionUserID", userid)
				.set("transactionMoney", money).set("transactionData", data);
		switch (type) {
		case "��ֵ":
			tran.set("transactionType", type);
			break;
		case "�պ��":
			tran.set("transactionType", type);
			break;
		case "�˿�":
			tran.set("transactionType", type);
			break;
		default:
			return false;
		}
		return tran.save();
	}

	/**
	 * ����֧�����׼�¼
	 */
	public boolean insertExpenseRecord(String userid, BigDecimal money,
			String type, String data) {
		// ǮתΪ����
		money = money.multiply(new BigDecimal("-1"));
		Transaction tran = new Transaction().set("transactionUserID", userid)
				.set("transactionMoney", money).set("transactionData", data);
		switch (type) {
		case "�����":
			tran.set("transactionType", type);
			break;
		case "����":
			tran.set("transactionType", type);
			break;
		case "ϵͳ����":
			tran.set("transactionType", type);
			break;
		default:
			return false;
		}

		return tran.save();
	}

	/**
	 * ���ݶ����Ÿ��¶���״̬
	 */
	public boolean UpdateTransactionStatusByOrderNumber(String userid,
			String orderNumber) {
		int count = Db
				.update(" update transaction set transactionStatus='���׳ɹ�' where transactionData='"
						+ orderNumber + "' ");
		return count == 1;

	}

	/**
	 * ���ݶ����Ų�ѯ���׼�¼ID
	 */
	public int GetTransactionIDByOrderNumber(String orderNumber) {
		List<Record> list = Db
				.find("select transactionID from transaction where transactionData='"
						+ orderNumber + "' ");
		if (list.size() == 0) {
			return 0;
		} else {
			return Integer
					.parseInt(list.get(0).get("transactionID").toString());
		}
	}
	/**
	  * ���뽻��ʧ�ܼ�¼
	  */
	 public boolean insertFaildRecord(String userid, BigDecimal money,
	   String type, String data) {
	  // ǮתΪ����
	  money = money.multiply(new BigDecimal("-1"));
	  Transaction tran = new Transaction().set("transactionUserID", userid)
	    .set("transactionMoney", money).set("transactionData", data).set("transactionStatus", "����ʧ��");
	  switch (type) {
	  case "�����":
	   tran.set("transactionType", type);
	   break;
	  case "����":
	   tran.set("transactionType", type);
	   break;
	  case "ϵͳ����":
	   tran.set("transactionType", type);
	   break;
	  default:
	   return false;
	  }

	  return tran.save();
	 }
}
