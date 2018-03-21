package yinian.controller;

import com.alipay.api.AlipayApiException;
import com.alipay.api.domain.AlipayFundTransToaccountTransferModel;
import com.jfinal.aop.Before;
import com.jfinal.core.Controller;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.DbKit;
import com.jfinal.plugin.activerecord.Record;
import com.jfinal.plugin.activerecord.tx.Tx;
import yinian.app.YinianDataProcess;
import yinian.common.CommonParam;
import yinian.model.Grab;
import yinian.model.RedEnvelop;
import yinian.model.Transaction;
import yinian.model.User;
import yinian.service.UserService;
import yinian.utils.AliPay;
import yinian.utils.GrabRedEnvelop;
import yinian.utils.JsonData;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserController extends Controller {

    private String jsonString; // ���ص�json�ַ���
    private JsonData jsonData = new JsonData(); // json������
    private YinianDataProcess dataProcess = new YinianDataProcess();// ���ݴ�����
    private UserService service = new UserService(); // ҵ������
    private UserService txService = enhance(UserService.class);

    /******************************* <Ǯ��ϵͳ>��ؽӿ� Start *******************************/

    /**
     * �µ�
     */
    public void PlaceOrder() {
        String userid = this.getPara("userid");
        String money = this.getPara("money");

        // ���ɶ�����
        String orderNumber = "yinian" + System.currentTimeMillis() + userid;

        Transaction tran = new Transaction().set("transactionUserID", userid)
                .set("transactionMoney", new BigDecimal(money))
                .set("transactionType", CommonParam.keyOfTopUp)
                .set("transactionData", orderNumber)
                .set("transactionStatus", "������");
        if (tran.save()) {
            String transactionID = tran.get("transactionID").toString();
            Record result = new Record().set("transactionID", transactionID)
                    .set("orderNumber", orderNumber);
            List<Record> list = new ArrayList<Record>();
            list.add(result);
            jsonString = jsonData.getSuccessJson(list);
        }
        renderText(jsonString);
    }

    /**
     * ��ֵ
     */
    @Before(Tx.class)
    public void TopUp() {
        String userid = this.getPara("userid");
        String orderNumber = this.getPara("orderNumber");

        // ��beecloud������֤���

        Transaction tran = new Transaction();
        boolean updateFlag = tran.UpdateTransactionStatusByOrderNumber(userid,
                orderNumber);
        int transactionID = tran.GetTransactionIDByOrderNumber(orderNumber);
        tran = new Transaction().findById(transactionID);

        BigDecimal money = new BigDecimal(tran.get("transactionMoney")
                .toString());

        boolean addFlag = new User().UpdateUserBalance(userid, money, "add");

        User user = new User().findById(userid);
        String balance = user.get("uBalance").toString();
        List<Record> result = dataProcess.makeSingleParamToList("balance",
                balance);

        if (updateFlag && addFlag) {
            jsonString = jsonData.getSuccessJson(result);
        } else {
            jsonString = dataProcess.updateFlagResult(false);
        }

        renderText(jsonString);

    }

    /**
     * ����
     *
     * @throws SQLException
     */
    @Before(Tx.class)
    public void Withdraw() throws SQLException {
        String userid = this.getPara("userid");
        String money = this.getPara("money");
        String account = this.getPara("account");
        double uBalance = txService.getUserBalance(userid);
        if (Double.parseDouble(money) <= uBalance) {
            // ���ɶ���
            String orderNumber = "yinian" + userid + System.currentTimeMillis();

            // ҵ�����ݸ���
            boolean serviceFlag = txService.ExpenseMoney(userid, new BigDecimal(
                    money), CommonParam.keyOfWithdraw, orderNumber);

            if (serviceFlag) {
                // ���ֵ�����֧����
                AlipayFundTransToaccountTransferModel model = new AlipayFundTransToaccountTransferModel();
                model.setOutBizNo(orderNumber);// ���ɶ�����
                model.setPayeeType("ALIPAY_LOGONID");// �̶�ֵ
                model.setPayeeAccount(account);// ת���տ��˻�
                model.setAmount(money);// ת�˽��
                model.setPayerShowName("����APP");
                model.setPayerRealName("�人��׷����������Ƽ����޹�˾");// �˻���ʵ����
                model.setRemark("����APPǮ������");

                try {
                    boolean transferFlag = AliPay.AliPayWithdraw(model);
                    if (transferFlag) {
                        jsonString = jsonData.getSuccessJson();
                    } else {
                        DbKit.getConfig().getConnection().rollback();
//	     Db.update("update transaction set transactionStatus='����ʧ��' where transactionData='"
//	       + orderNumber + "'   ");
                        //���뽻��ʧ�ܼ�¼
                        boolean insertFlag = new Transaction().insertFaildRecord(userid, new BigDecimal(
                                money), CommonParam.keyOfWithdraw, orderNumber);

                        jsonString = jsonData.getJson(1046, "����ʧ��");
                    }
                } catch (AlipayApiException e) {
                    // TODO Auto-generated catch block
                    DbKit.getConfig().getConnection().rollback();
//	    Db.update("update transaction set transactionStatus='����ʧ��' where transactionData='"
//	      + orderNumber + "'   ");
                    //���뽻��ʧ�ܼ�¼
                    boolean insertFlag = new Transaction().insertFaildRecord(userid, new BigDecimal(
                            money), CommonParam.keyOfWithdraw, orderNumber);
                    jsonString = jsonData.getJson(1046, "����ʧ��");
                }

            } else {
                jsonString = dataProcess.updateFlagResult(false);
            }
        } else {
            jsonString = jsonData.getJson(1046, "����ʧ��");
        }

        renderText(jsonString);
    }

    /**
     * ����
     */
    public void Withdraw_old() {
        String userid = this.getPara("userid");
        String money = this.getPara("money");
        String account = this.getPara("account");

        // ���ɶ���
        String orderNumber = "yinian" + userid + System.currentTimeMillis();

        // ҵ�����ݸ���
        boolean serviceFlag = txService.ExpenseMoney(userid, new BigDecimal(
                money), CommonParam.keyOfWithdraw, orderNumber);

        if (serviceFlag) {
            // ���ֵ�����֧����
            AlipayFundTransToaccountTransferModel model = new AlipayFundTransToaccountTransferModel();
            model.setOutBizNo(orderNumber);// ���ɶ�����
            model.setPayeeType("ALIPAY_LOGONID");// �̶�ֵ
            model.setPayeeAccount(account);// ת���տ��˻�
            model.setAmount(money);// ת�˽��
            model.setPayerShowName("����APP");
            model.setPayerRealName("�人��׷����������Ƽ����޹�˾");// �˻���ʵ����
            model.setRemark("����APPǮ������");

            try {
                boolean transferFlag = AliPay.AliPayWithdraw(model);
                if (transferFlag) {
                    jsonString = jsonData.getSuccessJson();
                } else {
                    Db.update("update transaction set transactionStatus='����ʧ��' where transactionData='"
                            + orderNumber + "'   ");
                    jsonString = jsonData.getJson(1046, "����ʧ��");
                }
            } catch (AlipayApiException e) {
                // TODO Auto-generated catch block
                Db.update("update transaction set transactionStatus='����ʧ��' where transactionData='"
                        + orderNumber + "'   ");
                jsonString = jsonData.getJson(1046, "����ʧ��");
            }

        } else {
            jsonString = dataProcess.updateFlagResult(false);
        }

        renderText(jsonString);
    }

    /**
     * ��ʾ���׼�¼
     */
    public void ShowTransactionRecord() {
        String userid = this.getPara("userid");
        String transactionID = this.getPara("transactionID");
        String type = this.getPara("type");

        List<Record> result = service.getUserTransactionRecords(userid,
                transactionID, type);
        jsonString = jsonData.getSuccessJson(result);
        renderText(jsonString);

    }

    /******************************* <Ǯ��ϵͳ>��ؽӿ� End *******************************/

    /******************************* <�����>��ؽӿ� Start *******************************/

    /**
     * ��ʾ�����Ϣ
     */
    public void ShowRedEnvelopInfo() {
        String userid = this.getPara("userid");
        String redEnvelopID = this.getPara("redEnvelopID");

        Record resultRecord = new Record();
        // �ж��û��Ƿ��������
        Grab grabJudge = new Grab();
        if (grabJudge.JudgeUserIsGrabRedEnvelop(userid, redEnvelopID)) {
            // ��ȡ��ϸ��Ϣ
            List<Record> grabList = grabJudge
                    .getRedEnvelopGrabInfo(redEnvelopID);
            resultRecord.set("isGrabRedEnvelop", true)
                    .set("grabList", grabList);
        } else {
            // �жϺ���Ƿ����
            RedEnvelop re = new RedEnvelop().findById(redEnvelopID);
            int redEnvelopStatus = Integer.parseInt(re.get("redEnvelopStatus")
                    .toString());
            if (redEnvelopStatus == 2) {
                resultRecord.set("isGrabRedEnvelop", false).set("isExpired",
                        true);
            } else {
                resultRecord.set("isGrabRedEnvelop", false).set("isExpired",
                        false);
            }

        }
        List<Record> result = new ArrayList<Record>();
        result.add(resultRecord);
        jsonString = jsonData.getSuccessJson(result);
        renderText(jsonString);

    }

    /**
     * �����
     */
    @Before(Tx.class)
    public void GrabRedEnvelop() {

        String userid = this.getPara("userid");
        String redEnvelopID = this.getPara("redEnvelopID");

        // �ж��û��Ƿ������ú��
        Grab judge = new Grab();
        if (judge.JudgeUserIsGrabRedEnvelop(userid, redEnvelopID)) {
            jsonString = jsonData.getJson(1041, "�������ú��");

        } else {
            // �жϺ���Ƿ���������Ѿ�����
            RedEnvelop re = new RedEnvelop().findById(redEnvelopID);
            int remainNum = Integer.parseInt(re.get("redEnvelopRemainNum")
                    .toString());
            int redEnvelopStatus = Integer.parseInt(re.get("redEnvelopStatus")
                    .toString());

            if (redEnvelopStatus == 2) {
                jsonString = jsonData.getJson(1043, "����ѹ���");

            } else if (remainNum == 0) {
                jsonString = jsonData.getJson(1040, "����ѱ�����");

            } else {
                BigDecimal remainMoney = new BigDecimal(re.get(
                        "redEnvelopRemainMoney").toString());
                // ��ȡ��������
                BigDecimal grabMoney = GrabRedEnvelop.getRandomMoney(
                        remainMoney, remainNum);

                // �����������Ϣ
                Grab grab = new Grab().set("grabRedEnvelopID", redEnvelopID)
                        .set("grabUserID", userid).set("grabMoney", grabMoney);
                boolean grabFlag = grab.save();

                // ���º��ʣ����Ϣ
                re.set("redEnvelopRemainMoney", remainMoney.subtract(grabMoney))
                        .set("redEnvelopRemainNum", remainNum - 1);
                boolean updateRedEnvelopInfoFlag = re.update();

                // �����û�Ǯ�����
                boolean updateBalanceFlag = txService.incomeMoney(userid,
                        grabMoney, CommonParam.keyOfGrabRedEnvelop,
                        redEnvelopID);

                // �����������Ϣ
                if (grabFlag && updateRedEnvelopInfoFlag && updateBalanceFlag) {
                    List<Record> grabList = judge
                            .getRedEnvelopGrabInfo(redEnvelopID);
                    Record resultRecord = new Record().set("grabMoney",
                            grabMoney).set("grabList", grabList);
                    List<Record> result = new ArrayList<Record>();
                    result.add(resultRecord);
                    jsonString = jsonData.getSuccessJson(result);
                } else {
                    jsonString = dataProcess.insertFlagResult(false);
                }

            }
        }

        renderText(jsonString);

    }

    /******************************* <�����>��ؽӿ� End *******************************/
    /**
     * �û��Ƿ񱻼��������
     */
    public void inBlackList() {
        List<Record> list = new ArrayList<Record>();
        jsonString = jsonData.getJson(2, "��������", list);
        String userid = this.getPara("userid");
        if (null != userid && !userid.equals("")) {
            User u = new User().findById(userid);
            int inBlackList = 1;
            if (null != u && null != u.get("ustate") && u.get("ustate").toString().equals("1")) {
                inBlackList = 0;
            }
            Record r = new Record();
            r.set("inBlackList", inBlackList);
            list.add(r);
            jsonString = jsonData.getSuccessJson(list);
        }
        renderText(jsonString);
    }
}
