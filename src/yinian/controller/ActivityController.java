package yinian.controller;

import java.io.File;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import yinian.app.YinianDataProcess;
import yinian.common.CommonParam;
import yinian.draw.ComposePicture;
import yinian.interceptor.CrossDomain;
import yinian.model.Encourage;
import yinian.model.EncourageLogistics;
import yinian.model.Event;
import yinian.model.Group;
import yinian.model.GroupMember;
import yinian.model.Industry;
import yinian.model.IndustryPhoto;
import yinian.model.InviteList;
import yinian.model.Join;
import yinian.model.Picture;
import yinian.model.Print;
import yinian.model.Puzzle;
import yinian.model.Reward;
import yinian.model.Sign;
import yinian.model.SmallAppLog;
import yinian.model.Temp;
import yinian.model.TempView;
import yinian.model.User;
import yinian.service.ActivityService;
import yinian.service.YinianService;
import yinian.utils.DES;
import yinian.utils.JsonData;
import yinian.utils.QiniuOperate;
import yinian.utils.SmallAppQRCode;
import yinian.utils.YinianUtils;
import yinian.utils.test.PagerTest;

import com.alibaba.fastjson.JSONObject;
import com.jfinal.aop.Before;
import com.jfinal.core.Controller;
import com.jfinal.log.Logger;
import com.jfinal.plugin.activerecord.ActiveRecordException;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;
import com.jfinal.plugin.activerecord.tx.Tx;
import com.jfinal.plugin.ehcache.CacheKit;

@Before(CrossDomain.class)
public class ActivityController extends Controller {

    private String jsonString; // ���ص�json�ַ���
    private JsonData jsonData = new JsonData(); // json������
    private YinianDataProcess dataProcess = new YinianDataProcess();// ���ݴ�����
    private QiniuOperate operate = new QiniuOperate();
    // enhance������Ŀ�����AOP��ǿ
    YinianService TxService = enhance(YinianService.class);
    YinianService ynService = new YinianService();
    private static final Logger log = Logger.getLogger(ActivityController.class);
    private ActivityService service = new ActivityService();
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    /******************************* �ͳһ��ؽӿ� Start *******************************/

    /**
     * ��ȡ���Ϣ
     */
    public void GetActivityBanner() {
        List<Record> list = Db.find("select * from banner where bstatus=3 limit 1  ");
        jsonString = jsonData.getSuccessJson(list);
        renderText(jsonString);
    }

    /**
     * ��ȡƴͼȨ��
     */
    public void GetMyPhoto() {
        String id = this.getPara("id");
        List<Record> list = new ArrayList<Record>();
        if (id != null && !(id.equals(""))) {
            list = Db.find("select status from ynTemp where id=" + id + "");
        } else {
            list = Db.find("select status from ynTemp where id=1");
        }

        jsonString = jsonData.getSuccessJson(list);
        renderText(jsonString);
    }

    /******************************* �ͳһ��ؽӿ� End *******************************/

    /*******************************
     * <�ҵ�2016�����>��ӿ� Start
     *******************************/

    /**
     * ��ʾ�������
     */
    public void ShowMyMemory() {

        // ��ȡ����
        String groupid = this.getPara("groupid");
        String userid = this.getPara("userid");// ����鿴���û���userid

        // ��ȡͳ�������ֶ�
        Set<Integer> userSet = new HashSet<Integer>();
        int count = 0;

        // groupid���ܻ�ȡ����
        groupid = dataProcess.decryptData(groupid, "groupid");
        // �ж�����Ƿ����
        List<Record> exist = Db.find("select * from groups where groupid=" + groupid + " and gstatus=0 ");
        if (exist.size() == 0) {
            jsonString = jsonData.getJson(1012, "����ѱ�ɾ��");
        } else {
            // ��ȡͼƬ����
            List<Record> queryList = Db.find(
                    "select eid,userid,unickname,upic,etext,isAnonymous,GROUP_CONCAT(poriginal SEPARATOR \",\" ) as url from users,events,pictures where userid=euserid and eid=peid and egroupid="
                            + groupid + " and estatus=0 and pstatus=0 group by peid DESC  ");
            // ��������
            List<Record> dataList = new ArrayList<Record>();
            for (Record record : queryList) {
                // ���������Ϣ
                userSet.add(Integer.parseInt(record.get("userid").toString()));

                // �ж��Ƿ��������������޸��û��ǳƺ�ͷ��
                int isAnonymous = Integer.parseInt(record.get("isAnonymous").toString());
                if (isAnonymous == 1) {
                    record.set("unickname", "����").set("upic",
                            CommonParam.qiniuOpenAddress + CommonParam.anonymousUserPicture);
                }

                // ����ͼƬ���ֳɶ�������
                String url = record.get("url").toString();
                String[] array = url.split(",");
                for (int i = 0; i < array.length; i++) {
                    Record temp = new Record();
                    temp.setColumns(record);
                    temp.set("url", operate.getDownloadToken(array[i]));
                    temp.set("thumbnail", operate.getDownloadToken(array[i] + "?imageView2/1/w/400/h/480"));
                    dataList.add(temp);
                }

            }

            // �������ݼ�
            Record resultRecord = new Record();
            // ��ȡ��������
            count = dataList.size();
            // ��ȡ��������
            int userCount = userSet.size();

            // �ж��û��Ƿ��������
            List<Record> in = Db
                    .find("select * from groupmembers where gmuserid=" + userid + " and gmgroupid=" + groupid + " ");
            if (in.size() == 0) {
                resultRecord.set("isInAlbum", "no");
            } else {
                resultRecord.set("isInAlbum", "yes");
            }

            // �ж��û��Ƿ����л������
            List<Record> judge = Db
                    .find("select groupid from groups where gtype=9 and gstatus=0 and gcreator=" + userid + " ");
            if (judge.size() == 0) {
                resultRecord.set("isCreate", "no");
            } else {
                resultRecord.set("isCreate", "yes");
                String code = "groupid=" + judge.get(0).get("groupid").toString() + ",userid=" + userid;
                try {
                    code = DES.encryptDES(code, CommonParam.DESSecretKey);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                resultRecord.set("code", code);
            }
            // ��ȡ��ᴴ�����ǳ�
            List<Record> creator = Db
                    .find("select unickname,upic from users,groups where userid=gcreator and groupid=" + groupid + " ");

            // ���벢��������
            resultRecord.set("memoryCount", count).set("friendCount", userCount)
                    .set("creator", creator.get(0).get("unickname").toString()).set("data", dataList)
                    .set("headPicture", creator.get(0).get("upic").toString());
            List<Record> result = new ArrayList<Record>();
            result.add(resultRecord);
            jsonString = jsonData.getSuccessJson(result);
        }

        renderText(jsonString);

    }

    /**
     * �ϴ�������Ƭ
     */
    @Before(Tx.class)
    public void UploadMemoryPhoto() {
        String userid = this.getPara("userid");
        String groupid = this.getPara("groupid");
        String url = this.getPara("url");
        String content = this.getPara("content");
        String isAnonymous = this.getPara("isAnonymous");

        // groupid���ܻ�ȡ����
        groupid = dataProcess.decryptData(groupid, "groupid");

        Event event = new Event().set("egroupid", groupid).set("euserid", userid).set("etext", content)
                .set("efirstpic", CommonParam.qiniuPrivateAddress + url).set("etype", 0);
        if (isAnonymous.equals("yes")) {
            event.set("isAnonymous", 1);
        }
        if (event.save()) {
            String eid = event.get("eid").toString();
            Picture picture = new Picture().set("poriginal", CommonParam.qiniuPrivateAddress + url).set("peid", eid)
                    .set("pGroupid", groupid).set("puserid", userid);
            jsonString = dataProcess.insertFlagResult(picture.save());
        } else {
            jsonString = dataProcess.insertFlagResult(false);
        }
        renderText(jsonString);

    }

    /**
     * �����������
     */
    public void CreateMemoryAlbum() {
        String userid = this.getPara("userid");

        // �ж��û��Ƿ��Ѵ������������
        List<Record> judge = Db.find("select * from groups where gtype=9 and gstatus=0 and gcreator=" + userid + " ");
        if (judge.size() == 0) {
            User user = new User().findById(userid);
            String nickname = user.get("unickname").toString();
            String inviteCode = Group.CreateSpaceInviteCode();
            String groupCover = CommonParam.qiniuOpenAddress + CommonParam.otherGroup;

            jsonString = TxService.createAlbum(nickname + "��2016�������", userid, groupCover, "9", inviteCode, "web");

        } else {
            jsonString = jsonData.getJson(2027, "�û��Ѵ����������");
        }
        renderText(jsonString);

    }

    /*******************************
     * <�ҵ�2016�����>��ӿ� End
     *******************************/

    /*******************************
     * <��Ƭƴͼ>��ӿ� Start
     *******************************/

    /**
     * ����ƴͼ
     *
     * @throws Exception
     */
    public void MakePuzzle() throws Exception {
        String userid = this.getPara("userid");
        String picture = this.getPara("picture");
        String content = this.getPara("content");

        picture = picture.substring(0, 7).equals("http://") ? picture : CommonParam.qiniuOpenAddress + picture;
        content = (content == null || content.equals("")) ? "��ĺ���û�������κ���ʾŶ��" : content;

        Puzzle puzzle = new Puzzle().set("puzzleUserID", userid).set("puzzlePicture", picture).set("puzzleContent",
                content);
        if (puzzle.save()) {
            String puzzleID = puzzle.get("puzzleID").toString();
            String encodePuzzleID = DES.encryptDES(puzzleID, CommonParam.DESSecretKey);

            Record record = new Record().set("puzzleID", puzzleID).set("encodePuzzleID", encodePuzzleID);
            List<Record> list = new ArrayList<Record>();
            list.add(record);
            jsonString = jsonData.getSuccessJson(list);
        } else {
            jsonString = dataProcess.insertFlagResult(false);
        }

        renderText(jsonString);
    }

    /**
     * �޸�ƴͼ��Ϣ
     */
    public void ModifyPuzzleInfo() {
        String userid = this.getPara("userid");
        String content = this.getPara("content");
        String puzzleID = this.getPara("puzzleID");
        String type = this.getPara("type");

        if (type != null && type.equals("encode")) {
            try {
                puzzleID = DES.decryptDES(puzzleID, CommonParam.DESSecretKey);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        Puzzle puzzle = new Puzzle().findById(puzzleID);
        String puzzleUserID = puzzle.get("puzzleUserID").toString();
        if (puzzleUserID.equals(userid)) {
            content = (content == null || content.equals("")) ? "��ĺ���û�������κ���ʾŶ��" : content;
            puzzle.set("puzzleContent", content);
            jsonString = dataProcess.updateFlagResult(puzzle.update());
        } else {
            jsonString = jsonData.getJson(6, "û��Ȩ��ִ�в���");
        }
        renderText(jsonString);
    }

    /**
     * �鿴ƴͼҳ��
     */
    public void ShowPuzzle() {
        String userid = this.getPara("userid");
        String puzzleID = this.getPara("puzzleID");
        String url = this.getPara("url");
        String type = this.getPara("type");

        if (type != null && type.equals("encode")) {
            try {
                puzzleID = DES.decryptDES(puzzleID, CommonParam.DESSecretKey);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        // ��ȡҳ����Ϣ
        List<Record> puzzleInfo = Db.find(
                "select userid,unickname,upic,puzzlePicture,puzzleContent,puzzleQRCode from puzzle,users where puzzleUserID=userid and puzzleID="
                        + puzzleID + " and puzzleStatus=0 ");
        List<Record> joinerInfo;
        if (puzzleID.equals("1")) {
            joinerInfo = Db.find(
                    "select userid,unickname,upic,joinID,joinScore,joinTime from `join`,users where joinUserID=userid and joinActivityID="
                            + puzzleID + " and joinType=0 ORDER BY joinScore asc limit 15  ");
        } else {
            joinerInfo = Db.find(
                    "select userid,unickname,upic,joinID,joinScore,joinTime from `join`,users where joinUserID=userid and joinActivityID="
                            + puzzleID + " and joinType=0 ORDER BY joinScore asc  ");
        }

        puzzleInfo.get(0).set("joiner", joinerInfo);

        // �ж��û��Ƿ����ƴͼ
        List<Record> judge = Db.find("select * from `join` where joinUserID=" + userid + " and joinActivityID="
                + puzzleID + " and joinType=0 ");
        if (judge.size() == 0) {
            puzzleInfo.get(0).set("isJoin", "no");
        } else {
            puzzleInfo.get(0).set("isJoin", "yes");
        }

        // �ϳ�ƴͼ��ά��
        if (url != null && !url.equals("")) {

        }

        jsonString = jsonData.getSuccessJson(puzzleInfo);
        renderText(jsonString);
    }

    /**
     * ���ƴͼ��Ϸ
     */
    public void FinishPuzzling() {
        String userid = this.getPara("userid");
        String score = this.getPara("score");
        String puzzleID = this.getPara("puzzleID");
        String type = this.getPara("type");

        if (type != null && type.equals("encode")) {
            try {
                puzzleID = DES.decryptDES(puzzleID, CommonParam.DESSecretKey);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        // �ж��û��Ƿ����ƴͼ
        List<Record> judge = Db.find("select * from `join` where joinUserID=" + userid + " and joinActivityID="
                + puzzleID + " and joinType=0 ");
        if (judge.size() == 0) {
            Join join = new Join().set("joinUserID", userid).set("joinScore", score).set("joinActivityID", puzzleID)
                    .set("joinType", 0);
            if (join.save()) {
                // ���ظ�ƴͼ��Ϣ
                List<Record> joinerInfo = Db.find(
                        "select userid,unickname,upic,joinID,joinScore,joinTime from `join`,users where joinUserID=userid and joinActivityID="
                                + puzzleID + " and joinType=0 ORDER BY joinScore asc  ");
                jsonString = jsonData.getSuccessJson(joinerInfo);
            } else {
                jsonString = dataProcess.insertFlagResult(false);
            }
        } else {
            jsonString = jsonData.getJson(2028, "�Ѳμӹ�����Ϸ");
        }
        renderText(jsonString);

    }

    /**
     * ����ƴͼ
     */
    public void TryPlayPuzzle() {
        String userid = this.getPara("userid");
        Double score = Double.parseDouble(this.getPara("score"));
        String puzzleID = this.getPara("puzzleID");

        Puzzle puzzle = new Puzzle().findById(puzzleID);
        int puzzleType = Integer.parseInt(puzzle.get("puzzleType").toString());
        if (puzzleType == 1) {
            // �ж��û��Ƿ����ƴͼ
            List<Record> judge = Db.find("select * from `join` where joinUserID=" + userid + " and joinActivityID="
                    + puzzleID + " and joinType=0 ");
            if (judge.size() == 0) {
                Join join = new Join().set("joinUserID", userid).set("joinScore", score).set("joinActivityID", puzzleID)
                        .set("joinType", 0);
                if (join.save()) {
                    // ���ظ�ƴͼ��Ϣ
                    List<Record> joinerInfo = Db.find(
                            "select userid,unickname,upic,joinID,joinScore,joinTime from `join`,users where joinUserID=userid and joinActivityID="
                                    + puzzleID + " and joinType=0 ORDER BY joinScore asc limit 15  ");
                    jsonString = jsonData.getSuccessJson(joinerInfo);
                } else {
                    jsonString = dataProcess.insertFlagResult(false);
                }
            } else {
                // �洢�û���ѳɼ�
                String joinID = judge.get(0).get("joinID").toString();
                double joinScore = Double.parseDouble(judge.get(0).get("joinScore").toString());
                if (score < joinScore) {
                    Join join = new Join().findById(joinID);
                    join.set("joinScore", score);
                    if (join.update()) {
                        // ���ظ�ƴͼ��Ϣ
                        List<Record> joinerInfo = Db.find(
                                "select userid,unickname,upic,joinID,joinScore,joinTime from `join`,users where joinUserID=userid and joinActivityID="
                                        + puzzleID + " and joinType=0 ORDER BY joinScore asc limit 15  ");
                        jsonString = jsonData.getSuccessJson(joinerInfo);
                    } else {
                        jsonString = dataProcess.updateFlagResult(false);
                    }
                } else {
                    // ���ظ�ƴͼ��Ϣ
                    List<Record> joinerInfo = Db.find(
                            "select userid,unickname,upic,joinID,joinScore,joinTime from `join`,users where joinUserID=userid and joinActivityID="
                                    + puzzleID + " and joinType=0 ORDER BY joinScore asc limit 15  ");
                    jsonString = jsonData.getSuccessJson(joinerInfo);
                }
            }
        } else {
            jsonString = jsonData.getJson(2, "�����������");
        }
        renderText(jsonString);
    }

    /**
     * ��ȡ��Ϸ������
     */
    public void GetPuzzleGameTotalNum() {
        List<Record> list = Db.find("select btitle from banner where bid=22  ");
        int num = Integer.parseInt(list.get(0).get("btitle").toString());
        Random random = new Random();
        num = num + random.nextInt(30);
        Db.update("update banner set btitle=" + num + " where bid=22 ");
        jsonString = jsonData.getSuccessJson(dataProcess.makeSingleParamToList("number", num));
        renderText(jsonString);
    }

    /******************************* <��Ƭƴͼ>��ӿ� End *******************************/

    /*******************************
     * <�ĺ󼴷�>��ӿ� Start
     *******************************/

    /**
     * ��ȡ��ʱ��Ƭ����
     */
    public void GetTempPhotoContent() {
        String userid = this.getPara("userid");
        String tempID = this.getPara("tempID");

        List<Record> judge = Db
                .find(" select * from tempView where tempViewUserID=" + userid + " and tempViewTempID=" + tempID + " ");

        if (judge.size() == 0) {
            Temp temp = new Temp().findById(tempID);
            String tempPid = temp.get("tempPid").toString();
            // ��ȡͼƬ��Ϣ
            List<Record> picList = Db
                    .find("select pid,poriginal,DATE(puploadtime) as puploadtime  from pictures where pid in(" + tempPid
                            + ") ORDER BY DATE(puploadtime) desc ");

            // �洢�������ڵ�ͼƬ
            List<Record> resultPicList = new ArrayList<Record>();
            // �洢�������ڵ�ͼƬ
            Record tempRecord = new Record();
            // �������ڵ�ͼƬ����
            Set<String> picSet = new HashSet<String>();

            // ͼƬ��Ȩ�������ڷ���
            for (int i = 0; i < picList.size(); i++) {
                String url = operate
                        .getDownloadToken((picList.get(i).get("poriginal").toString()) + "?imageView2/2/w/400");
                String time = picList.get(i).get("puploadtime").toString();

                if (tempRecord.get("date") == null) {
                    // ˵�����µ����ڣ���ֱ�Ӳ���
                    tempRecord.set("date", time);
                    picSet.add(url);
                } else {
                    if ((tempRecord.get("date").toString()).equals(time)) {
                        // ��ǰ���ڴ���
                        picSet.add(url);
                    } else {
                        // ��ǰ������������
                        tempRecord.set("picture", picSet.toArray());
                        resultPicList.add(tempRecord);
                        // �����µ�record��set
                        tempRecord = new Record().set("date", time);
                        picSet = new HashSet<String>();
                        picSet.add(url);
                    }
                }

                // ���i�����һ������record����list��
                if ((i + 1) == picList.size()) {
                    tempRecord.set("picture", picSet.toArray());
                    resultPicList.add(tempRecord);
                }
            }
            Record resultRecord = new Record();

            // ��ȡ����鿴ʱ��
            int limitTime = Integer.parseInt(temp.get("tempLimit").toString());
            String tempURL = temp.get("tempURL");
            resultRecord.set("pic", resultPicList).set("limitTime", limitTime).set("picNum", picList.size())
                    .set("tempURL", tempURL);
            // ��ȡ���ؽ��
            List<Record> result = new ArrayList<Record>();
            result.add(resultRecord);
            jsonString = jsonData.getSuccessJson(result);
        } else {
            jsonString = jsonData.getJson(2029, "�Ѳ鿴���ĺ󼴷�");
        }
        renderText(jsonString);
    }

    /**
     * �����ʱ��Ƭ�Ĳ鿴
     */
    public void FinishWatchTempPhoto() {
        String userid = this.getPara("userid");
        String tempID = this.getPara("tempID");
        TempView tv = new TempView().set("tempViewUserID", userid).set("tempViewTempID", tempID);
        jsonString = dataProcess.insertFlagResult(tv.save());
        renderText(jsonString);
    }

    /**
     * ������ʱ��Ƭ
     */
    @Before(CrossDomain.class)
    public void MakeTempPhoto() {
        String userid = this.getPara("userid");
        String timeLimit = this.getPara("timeLimit");
        String type = this.getPara("type");
        String data = this.getPara("data");

        Temp temp = new Temp();

        switch (type) {
            case "photoWall":
                temp.set("tempUserID", userid).set("tempPid", data).set("tempLimit", timeLimit);
                break;
            case "timeAxis":
                List<Record> list = Db.find("select GROUP_CONCAT(pid) as pid from pictures where peid=" + data + "  ");
                String pid = list.get(0).get("pid").toString();
                temp.set("tempUserID", userid).set("tempPid", pid).set("tempLimit", timeLimit);
                break;
            case "phone":
                // ����ͼƬ
                String[] address = data.split(",");
                String pids = "";
                for (int i = 0; i < address.length; i++) {
                    Picture picture = new Picture().set("peid", 3).set("poriginal",
                            CommonParam.qiniuOpenAddress + address[i]);
                    if (picture.save()) {
                        pids += picture.get("pid").toString() + ",";
                    }
                }
                pids = pids.substring(0, pids.length() - 1);
                temp.set("tempUserID", userid).set("tempPid", pids).set("tempLimit", timeLimit);
                break;
            default:
                jsonString = jsonData.getJson(1, "�������ȱʧ");
                break;
        }
        if (temp.save()) {
            int tempID = Integer.parseInt(temp.get("tempID").toString());
            List<Record> result = dataProcess.makeSingleParamToList("tempID", tempID);
            jsonString = jsonData.getSuccessJson(result);
        } else {
            jsonString = dataProcess.insertFlagResult(false);
        }

        renderText(jsonString);
    }

    /******************************* <�ĺ󼴷�>��ӿ� End *******************************/

    /******************************* <��ҵ����>��ӿ� Start *****************************/

    /**
     * ��ȡ������ҵ
     */
    public void GetAllIndustry() {
        List<Record> result = IndustryPhoto.getAllIndustryType();
        jsonString = jsonData.getSuccessJson(result);
        renderText(jsonString);
    }

    /**
     * ��ȡ��ҵ����
     */
    public void GetRealityOfTheIndustry() {
        // ��ȡ����
        String userid = this.getPara("userid");
        String industry = this.getPara("industry");
        String version = this.getPara("version");

        // ��ȡ���ͼƬ
        List<Record> photoList = Db
                .find("select address from industryPhoto where industry='" + industry + "' and status=0 ");
        int size = photoList.size();
        int num = new Random().nextInt(size);
        String address = photoList.get(num).get("address").toString();

        // ��ȡ�û���
        User user = new User().findById(userid);
        String nickname = user.getStr("unickname");

        // �ϳ�ͼƬ
        ComposePicture com = new ComposePicture();
        String afterURL = "";
        String beforeURL = "";
        // if (version != null && version.equals("2")) {
        // �°汾ֻ��һ��ͼƬ
        afterURL = com.ComposeIndustryPicture2ndVersion(address, nickname, userid);
        beforeURL = afterURL;
        // } else {
        // // �ɰ汾������ͼƬ
        // afterURL = com.ComposeIndustryPicture(industry, address, nickname,
        // userid);
        //
        // beforeURL = afterURL.substring(0, afterURL.length() - 9)
        // + "before.jpg";
        // operate.uploadFileToOpenSpace(CommonParam.tempPictureServerSavePath
        // + beforeURL, beforeURL);
        // File beforeFile = new File(CommonParam.tempPictureServerSavePath
        // + beforeURL);
        // beforeFile.delete();
        // }

        // ��ͼƬ�ϴ�����ţ��
        operate.uploadFileToOpenSpace(CommonParam.tempPictureServerSavePath + afterURL, afterURL);

        // ɾ�������ļ�
        File afterFile = new File(CommonParam.tempPictureServerSavePath + afterURL);
        afterFile.delete();

        // ���ؽ��
        Record record = new Record().set("beforeURL", CommonParam.qiniuOpenAddress + beforeURL).set("afterURL",
                CommonParam.qiniuOpenAddress + afterURL);
        List<Record> list = new ArrayList<Record>();
        list.add(record);
        Industry in = new Industry().set("industryUserID", userid).set("industryContent", industry)
                .set("industryPicture", CommonParam.qiniuOpenAddress + afterURL);
        if (in.save()) {
            jsonString = jsonData.getSuccessJson(list);
        } else {
            jsonString = dataProcess.insertFlagResult(false);
        }
        renderText(jsonString);

    }

    /******************************* <��ҵ����>��ӿ� End *******************************/

    /******************************* <����>��ӿ� Start *****************************/

    /**
     * ��ʾ��ռ�
     */
    public void ShowActivitySpace() {
        // ��ȡ����
        int userid = this.getParaToInt("userid");

        List<Record> result = service.getActivitySpace(userid);

        // ���û����뵽�ռ���
        GroupMember gm = new GroupMember();
        for (Record record : result) {
            int groupid = Integer.parseInt(record.get("groupid").toString());
            // �ж��û��Ƿ��ڿռ���
            if (gm.judgeUserIsInTheAlbum(userid, groupid)) {
                // ���ڿռ��ڣ����û�����ռ�
                gm.AddGroupMember(userid, groupid);
            }
        }

        jsonString = jsonData.getSuccessJson(result);
        // ���ؽ��
        renderText(jsonString);
    }

    /**
     * ����ռ䣬����Ϣ����
     */
    public void EnterSpaceWithoutNotify() {
        // ��ȡ����
        int userid = this.getParaToInt("userid");
        int groupid = this.getParaToInt("groupid");

        GroupMember gm = new GroupMember();
        // �ж��û��Ƿ��ڿռ���
        if (gm.judgeUserIsInTheAlbum(userid, groupid)) {
            // ���ڿռ��ڣ����û�����ռ�
            jsonString = dataProcess.insertFlagResult(gm.AddGroupMember(userid, groupid));
        } else {
            jsonString = jsonData.getJson(1013, "�û���������");
        }
        renderText(jsonString);

    }

    /**
     * �����ռ����û���̬
     */
    public void SearchSpaceUserEvent() {
        // ��ȡ����
        String groupid = this.getPara("groupid");
        String nickname = this.getPara("nickname");

        List<Record> result = service.getSearchEvents(groupid, nickname);
        jsonString = jsonData.getSuccessJson(result);
        renderText(jsonString);

    }

    /**
     * ��ʾ���а�3���Ӹ���һ��
     */
    public void ShowChart() {
        // ��ȡ����
        int userid = this.getParaToInt("userid");
        int groupid = this.getParaToInt("groupid");

        // ��ȡ������
        List<Record> chart = CacheKit.get("ServiceCache", "chart");
        // ����Ϊ�գ����²�ѯ
        if (chart == null) {
            chart = service.getSpaceEventChart(groupid);
            if (chart != null) {
                CacheKit.put("ServiceCache", "chart", chart);
            }
        }

        // ��ȡ��������
        List<Record> myChart = new ArrayList<Record>();
        boolean searchFlag = false;
        for (Record record : chart) {
            int tempUserid = Integer.parseInt(record.get("userid").toString());
            if (userid == tempUserid) {
                myChart.add(record);
                searchFlag = true;
                break;
            }
        }
        if (!searchFlag) {
            // û�н�������
            List<Record> tempList = Db.find(
                    "SELECT userid,unickname,upic,count(*) AS num FROM users,`events`,`like` WHERE userid = euserid AND eid = likeEventID AND estatus = 0 AND likeStatus != 1 AND egroupid = "
                            + groupid + " and euserid = " + userid + " GROUP BY eid ORDER BY num DESC limit 1");
            if (tempList.size() == 0) {
                Record tempRecord = Db
                        .findFirst("SELECT userid,unickname,upic from users where userid=" + userid + " ");
                tempRecord.set("num", 0).set("rank", "δ����");
                myChart.add(tempRecord);
            } else {
                myChart.add(tempList.get(0).set("rank", "δ����"));
            }
        }

        // ���ؽ��
        Record resultRecord = new Record().set("totalChart", chart).set("personalChart", myChart);
        List<Record> result = new ArrayList<Record>();
        result.add(resultRecord);
        jsonString = jsonData.getSuccessJson(result);
        renderText(jsonString);

    }

    /**
     * ��ʾ�����㲥
     */
    public void ShowRewardBroadcast() {
        String groupid = this.getPara("groupid");
        List<Record> list = Reward.GetRecentThirtyRewardInfo(groupid);
        jsonString = jsonData.getSuccessJson(list);
        renderText(jsonString);
    }

    /**
     * ���㽱������
     */
    public void AddRewardInfo() {
        int userid = this.getParaToInt("userid");
        int type = this.getParaToInt("type");
        String groupid = this.getPara("groupid");

        Reward reward = new Reward().set("RewardUserID", userid).set("RewardType", type);
        // ��ʱ����Ĭ��
        if (groupid == null) {
            reward.set("RewardGroupID", 1065266);
        } else {
            reward.set("RewardGroupID", groupid);
        }

        boolean flag = false;

        switch (type) {
            case 8:
                reward.set("RewardContent", "�����ջ�Ա");
                flag = reward.save();
                break;
            case 28:
                reward.set("RewardContent", "���׷�д���ܱ�����Ƭ�ֻ�֧�ܴ����");
                flag = reward.save();
                break;
            case 48:
                reward.set("RewardContent", "��������Ʊһ��");
                flag = reward.save();
                break;
            case 68:
                reward.set("RewardContent", "���׷塶1987�ˡ� ��ʼ�д�漯��");
                flag = reward.save();
                break;
            default:
                break;
        }
        jsonString = dataProcess.insertFlagResult(flag);
        renderText(jsonString);
    }

    /******************************* <����>��ӿ� End *******************************/

    /*******************************
     * <С����ǩ���ҽ�>��ӿ� Start
     *******************************/

    /**
     * ÿ��ǩ��
     *
     * @throws ParseException
     */
    public void DailySignIn() throws ParseException {
        String userid = this.getPara("userid");

        // ǩ��
        boolean signInFlag = service.signIn(userid, "0");

        // �����û��ռ�100M=102400KB
        boolean increaseFlag = false;
        if (signInFlag) {
            User user = new User();
            increaseFlag = user.IncreaseUserTotalStorageSpace(userid, 102400.00);
        }
        if (signInFlag && increaseFlag) {
            jsonString = jsonData.getSuccessJson();
        } else {
            jsonString = jsonData.getJson(2030, "������ǩ��");
        }

        renderText(jsonString);

    }


    /**
     * ÿ��ǩ��
     */
    @Before(CrossDomain.class)
    public void DailySignIn2() throws ParseException {
        String userid = this.getPara("userid");

        // ǩ��
        boolean signInFlag = service.signIn2(userid, "0");

        // �����û��ռ�100M=102400KB
        boolean increaseFlag = false;
        if (signInFlag) {
            User user = new User();
            increaseFlag = user.IncreaseUserTotalStorageSpace(userid, 102400.00);
        }
        if (signInFlag && increaseFlag) {
            jsonString = jsonData.getSuccessJson();
        } else {
            jsonString = jsonData.getJson(2030, "������ǩ��");
        }

        renderText(jsonString);

    }

    /**
     * ��ʾ����ҳ��
     *
     * @throws ParseException
     */
    public void ShowWelfarePage() throws ParseException {
        String userid = this.getPara("userid");

        Record resultRecord = new Record();

        // �û��ռ���Ϣ
        User user = new User().findById(userid);
        Double useStorage = user.getDouble("uusespace");
        Double totalStorage = user.getDouble("utotalspace");

        resultRecord.set("useStorage", useStorage).set("totalStorage", totalStorage);

        // ǩ����Ϣ�������Ƿ�ǩ����ǩ��������
        String today = sdf.format(new Date());
        Sign sign = new Sign();
        List<Record> signInfoList = sign.getUserSignInInfo(userid, "0");
        if (signInfoList.size() == 0) {
            // ��δǩ����
            resultRecord.set("isTodaySign", false).set("signDay", 0).set("showSign", "1");
        } else {
            String signStartDate = signInfoList.get(0).get("signStartDate").toString();
            String signEndDate = signInfoList.get(0).get("signEndDate").toString();
            //by lk �Ƿ���ʾǩ������
            String firstSign = signInfoList.get(0).get("signFirstTime").toString();
            long f = sdf.parse(firstSign).getTime();
            if (f > sdf.parse("2017-12-14 23:59:59").getTime()) {
                resultRecord.set("showSign", 1);
            } else {
                resultRecord.set("showSign", 0);
            }
            //by lk �Ƿ���ʾǩ������  end
            // ǩ������
            long to = sdf.parse(signEndDate).getTime();
            long from = sdf.parse(signStartDate).getTime();
            int signDay = (int) ((to - from) / (1000 * 60 * 60 * 24)) + 1;
            resultRecord.set("signDay", signDay);
            //��ʷ���ǩ�������������ж��Ƿ������ȡ
            int signCount = signInfoList.get(0).get("signCount");
            resultRecord.set("signCount", signCount);

            // �����Ƿ�ǩ��
            if (signEndDate.equals(today)) {
                resultRecord.set("isTodaySign", true);
            } else {
                resultRecord.set("isTodaySign", false);
            }

        }

        // �ϴ���Ƭ��
        int picNum = user.GetUserUploadPicNum(userid);
        resultRecord.set("picNum", picNum);

        // ���������
        List<Record> encourageList = Encourage.getEncourageInfo(userid);
        int inviteNum = 0;
        if (encourageList.size() != 0) {
            inviteNum = Integer.parseInt(encourageList.get(0).get("inviteNum").toString());
            resultRecord.set("inviteNum", inviteNum);
            // �Ƿ�����������Ȧ��΢��Ⱥ
            resultRecord.set("isShareMoments", encourageList.get(0).get("shareMoments").toString())
                    .set("isShareWechatGroup", encourageList.get(0).get("shareWechatGroup").toString());

        } else {
            Encourage encourage = new Encourage().set("encourageUserID", userid);
            try {
                encourage.save();
            } catch (ActiveRecordException e) {

            } finally {
                resultRecord.set("inviteNum", inviteNum).set("isShareMoments", 0).set("isShareWechatGroup", 0);
            }

        }

        // ���ؽ��
        List<Record> resultList = new ArrayList<Record>();
        resultList.add(resultRecord);
        jsonString = jsonData.getSuccessJson(resultList);
        renderText(jsonString);

    }


    /**
     * ��ʾ����ҳ��
     *
     * @throws ParseException
     */
    @Before(CrossDomain.class)
    public void ShowWelfarePage2() throws ParseException {
        String userid = this.getPara("userid");

        Record resultRecord = new Record();

        // �û��ռ���Ϣ
        User user = new User().findById(userid);
        Double useStorage = user.getDouble("uusespace");
        Double totalStorage = user.getDouble("utotalspace");

        resultRecord.set("useStorage", useStorage).set("totalStorage", totalStorage);

        // ǩ����Ϣ�������Ƿ�ǩ����ǩ��������
        String today = sdf.format(new Date());
        Sign sign = new Sign();
        List<Record> signInfoList = sign.getUserSignInInfo(userid, "0");
        if (signInfoList.size() == 0) {
            // ��δǩ����
            resultRecord.set("isTodaySign", false).set("signDay", 0).set("showSign", "1");
        } else {
            String signStartDate = signInfoList.get(0).get("signStartDate").toString();
            String signEndDate = signInfoList.get(0).get("signEndDate").toString();
            //by lk �Ƿ���ʾǩ������
            String firstSign = signInfoList.get(0).get("signFirstTime").toString();
            long f = sdf.parse(firstSign).getTime();
            if (f > sdf.parse("2017-12-14 23:59:59").getTime()) {
                resultRecord.set("showSign", 1);
            } else {
                resultRecord.set("showSign", 0);
            }
            //by lk �Ƿ���ʾǩ������  end
            // ǩ������
            long to = sdf.parse(signEndDate).getTime();
            long from = sdf.parse(signStartDate).getTime();
            long todays = sdf.parse(today).getTime();
            int count = (int) ((todays - to) / (1000 * 60 * 60 * 24));
            System.out.println(count);
            //�ж��Ƿ��ǩ�������ǩ��ʾsignDayΪ0
            if (count >= 2) {
                int signDay = 0;
                resultRecord.set("signDay", signDay);
            } else {
                int signDay = (int) ((to - from) / (1000 * 60 * 60 * 24)) + 1;
                resultRecord.set("signDay", signDay);
            }

            //��ʷ���ǩ�������������ж��Ƿ������ȡ
            int signCount = signInfoList.get(0).get("signCount");
            resultRecord.set("signCount", signCount);

            // �����Ƿ�ǩ��
            if (signEndDate.equals(today)) {
                resultRecord.set("isTodaySign", true);
            } else {
                resultRecord.set("isTodaySign", false);
            }

        }

        // �ϴ���Ƭ��
        int picNum = user.GetUserUploadPicNum(userid);
        resultRecord.set("picNum", picNum);

        // ���������
        List<Record> encourageList = Encourage.getEncourageInfo(userid);
        int inviteNum = 0;
        if (encourageList.size() != 0) {
            inviteNum = Integer.parseInt(encourageList.get(0).get("inviteNum").toString());
            resultRecord.set("inviteNum", inviteNum);
            // �Ƿ�����������Ȧ��΢��Ⱥ
            resultRecord.set("isShareMoments", encourageList.get(0).get("shareMoments").toString())
                    .set("isShareWechatGroup", encourageList.get(0).get("shareWechatGroup").toString());

        } else {
            Encourage encourage = new Encourage().set("encourageUserID", userid);
            try {
                encourage.save();
            } catch (ActiveRecordException e) {

            } finally {
                resultRecord.set("inviteNum", inviteNum).set("isShareMoments", 0).set("isShareWechatGroup", 0);
            }

        }

        // ���ؽ��
        List<Record> resultList = new ArrayList<Record>();
        resultList.add(resultRecord);
        jsonString = jsonData.getSuccessJson(resultList);
        renderText(jsonString);

    }


    /**
     * ��ʾ�ҽ�ҳ��
     */
    @Before(CrossDomain.class)
    public void ShowPrizeReceivePage() {
        String userid = this.getPara("userid");
        String type = this.getPara("type");

        // �����������һ��������
        List<Record> encourageInfo = Encourage.getEncourageInfo(userid);
        if (encourageInfo.size() == 0) {
            Encourage en = new Encourage().set("encourageUserID", userid);
            en.save();
        }

        String sql = "select ";
        switch (type) {
            case "signIn":
                sql += "signLevelOne,signLevelTwo,signLevelThree,signLevelFour,signLevelFive ";
                break;
            case "inviteFriend":
                sql += "inviteLevelOne,inviteLevelTwo,inviteLevelThree,inviteLevelFour ";
                break;
            case "uploadPicture":
                sql += "uploadLevelOne,uploadLevelTwo,uploadLevelThree,uploadLevelFour ";
                break;
            default:
                break;
        }
        sql += " from encourage where encourageUserID=" + userid + " ";

        List<Record> result = Db.find(sql);
        jsonString = jsonData.getSuccessJson(result);
        renderText(jsonString);
    }

    /**
     * �ɹ��������
     */
    public void SuccessInviteFriend() {
        String userid = this.getPara("userid");

        // ������������
        boolean flag = service.recordSuccessInviteFriend(userid);

        // �����û��ܿռ�0.5G = 524288 KB
        boolean increaseFlag = false;
        if (flag) {
            User user = new User();
            increaseFlag = user.IncreaseUserTotalStorageSpace(userid, 524288.00);
        }
        jsonString = dataProcess.updateFlagResult(flag && increaseFlag);
        renderText(jsonString);
    }

    /**
     * ��ȡ����
     */
    @Before(CrossDomain.class)
    public void ReceiveEncourageReward() {
        String userid = this.getPara("userid");
        String type = this.getPara("type");
        String level = this.getPara("level");

        // ƴ���ֶε�keyֵ
        String key = "";

        // ��ȡencourageID
        String encourageID = new Encourage().getEncourageIDbyUserID(userid);
        Encourage en = new Encourage().findById(encourageID);
        boolean receiveFlag = false;
        boolean updateFlag = false;

        switch (type) {

            case "shareToMoments":
                // ����������Ȧ��һ���Խ���
                // ����0.5G = 524288 KB �ռ�
                String shareMoments = en.get("shareMoments").toString();
                if (shareMoments.equals("1")) {
                    receiveFlag = true;
                } else {
                    receiveFlag = new User().IncreaseUserTotalStorageSpace(userid, 524288.00);
                }

                if (receiveFlag)
                    key = "shareMoments";
                break;

            case "shareToWechatGroup":
                // ������΢��Ⱥ��һ���Խ���
                // ����0.5G = 524288 KB �ռ�
                String shareWechatGroup = en.get("shareWechatGroup").toString();
                if (shareWechatGroup.equals("1")) {
                    receiveFlag = true;
                } else {
                    receiveFlag = new User().IncreaseUserTotalStorageSpace(userid, 524288.00);
                }

                if (receiveFlag)
                    key = "shareWechatGroup";
                break;

            case "signIn":
                // ǩ��������ȡ
                key = "signLevel" + level;
                break;

            case "inviteFriend":
                // ������ѽ�����ȡ
                key = "inviteLevel" + level;
                break;

            case "uploadPicture":
                key = "uploadLevel" + level;
                // �ϴ�ͼƬ������ȡ
                break;

            default:
                break;
        }

        // ��������
        en.set(key, 1);
        updateFlag = en.update();

        // ���ؽ��
        jsonString = dataProcess.updateFlagResult(updateFlag);
        renderText(jsonString);
    }

    /**
     * ��д������Ϣ
     */
    public void WriteLogisticsInfo() {
        String userid = this.getPara("userid");
        String type = this.getPara("type");
        String level = this.getPara("level");
        String name = this.getPara("name");
        String address = this.getPara("addresss");
        String phone = this.getPara("phone");

        EncourageLogistics el = new EncourageLogistics().set("elUserID", userid).set("elType", type + "Level" + level)
                .set("elName", name).set("elAddress", address).set("elPhone", phone);
        jsonString = dataProcess.insertFlagResult(el.save());
        renderText(jsonString);
    }


    /**
     * ��д������Ϣ(��)
     */
    @Before(CrossDomain.class)
    public void WriteLogisticsInfo2() {
        String userid = this.getPara("userid");
        String type = this.getPara("type");
        String level = this.getPara("level");
        String name = this.getPara("name");
        String address = this.getPara("address");
        String phone = this.getPara("phone");
        String elTypeName = this.getPara("elTypeName");
        //��ȡ��ǰʱ��
        String current = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        Timestamp elReceiveTime = Timestamp.valueOf(current);

        // ƴ���ֶε�keyֵ
        String key = "";
        switch (type) {
            case "signIn":
                // ǩ��������ȡ
                key = "signLevel" + level;
                break;

            case "inviteFriend":
                // ������ѽ�����ȡ
                key = "inviteLevel" + level;
                break;

            case "uploadPicture":
                key = "uploadLevel" + level;
                // �ϴ�ͼƬ������ȡ
                break;

            default:
                break;
        }

        EncourageLogistics el = new EncourageLogistics();

        List<Record> elist = el.getUserEncourageInfo(userid, key);

        boolean elflag = false;
        //����ѯ���Ϊ��ʱ,�½�һ����Ʒ�����
        if (elist.size() == 0) {
            el.set("elUserID", userid).set("elType", key).set("elTypeName", elTypeName).set("elReceiveTime", elReceiveTime)
                    .set("elName", name).set("elAddress", address).set("elPhone", phone).set("elStatus", 0);
            elflag = el.save();
        } else {
            String elID = elist.get(0).get("elID").toString();
            //����Ʒ�����鱣��
            el = el.findById(elID);
            el.set("elTypeName", elTypeName).set("elReceiveTime", elReceiveTime).set("elStatus", 0)
                    .set("elName", name).set("elAddress", address).set("elPhone", phone);
            elflag = el.update();
        }

        //���³ɹ��󷵸ı佱Ʒ��״̬(��ʱ���Ѵ����״̬)
        if (elflag) {
            String encourageID = new Encourage().getEncourageIDbyUserID(userid);
            Encourage en = new Encourage().findById(encourageID);
            en.set(key, 1);
            boolean enflag = en.update();
            jsonString = dataProcess.insertFlagResult(enflag);
            renderText(jsonString);
        } else {
            //����ʧ�ܷ���ʧ����Ϣ
            jsonString = dataProcess.insertFlagResult(elflag);
            renderText(jsonString);
        }
    }

    /**
     * ��ȡ��ά��
     */
    public void GetQRCode() {
        String userid = this.getPara("userid");
        String shCode = this.getPara("shCode");

        String value = userid + "-" + shCode;
        SmallAppQRCode saq = new SmallAppQRCode();
        String url = saq.GetSmallAppQRCodeURL("encourage", value);

        jsonString = jsonData.getSuccessJson(dataProcess.makeSingleParamToList("QRCodeURL", url));
        renderText(jsonString);

    }

    /*******************************
     * <С����ǩ���ҽ�>��ӿ� End
     *******************************/

    /*******************************
     * <APPǩ������>��ӿ� Start
     *******************************/

    /**
     * ��ʾ����ҳ��
     *
     * @throws ParseException
     */
    public void ShowExpandSpacePage() throws ParseException {
        String userid = this.getPara("userid");

        // ������Ϣ
        List<Record> encourageList = Encourage.getAppEncourageInfo(userid);

        // ǩ����Ϣ�������Ƿ�ǩ����ǩ��������
        String today = sdf.format(new Date());
        Sign sign = new Sign();
        List<Record> signInfoList = sign.getUserSignInInfo(userid, "1");
        if (signInfoList.size() == 0) {
            // ��δǩ����
            encourageList.get(0).set("isTodaySign", false).set("signDay", 0);
        } else {
            String signStartDate = signInfoList.get(0).get("signStartDate").toString();
            String signEndDate = signInfoList.get(0).get("signEndDate").toString();

            int signDay = sign.GetSignDay(signStartDate, signEndDate);
            encourageList.get(0).set("signDay", signDay);

            // �����Ƿ�ǩ��
            if (signEndDate.equals(today)) {
                encourageList.get(0).set("isTodaySign", true);
            } else {
                encourageList.get(0).set("isTodaySign", false);
            }

        }
        jsonString = jsonData.getSuccessJson(encourageList);
        renderText(jsonString);

    }

    /**
     * ǩ��
     *
     * @throws ParseException
     */
    @Before(Tx.class)
    public void AppSignIn() throws ParseException {
        String userid = this.getPara("userid");

        // ǩ��
        boolean signInFlag = service.signIn(userid, "1");

        if (signInFlag) {
            // �������������ǩ������flag��Ϊ����ȡ����ǩ����ʼ����Ϊ���죬�ڽ��쿴��Ϊ0
            Sign sign = new Sign();
            List<Record> signInfoList = sign.getUserSignInInfo(userid, "1");
            String signStartDate = signInfoList.get(0).get("signStartDate").toString();
            String signEndDate = signInfoList.get(0).get("signEndDate").toString();
            int signDay = sign.GetSignDay(signStartDate, signEndDate);
            if (signDay == 7) {
                // ǩ����ʼ������Ϊ����
                String signID = signInfoList.get(0).get("signID").toString();
                sign = new Sign().findById(signID);
                // ��ȡ����
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DATE, 1);
                String tomorrow = sdf.format(cal.getTime());
                sign.set("signStartDate", tomorrow);

                // ����Ϊ����ȡ����
                boolean encourageFlag = Encourage.SetOneFieldCanBeGet(userid, "appSign");

                jsonString = dataProcess.updateFlagResult(sign.update() && encourageFlag);
            } else {
                jsonString = jsonData.getSuccessJson();
            }

        } else {
            jsonString = jsonData.getJson(2030, "������ǩ��");
        }

        renderText(jsonString);
    }

    /**
     * ��ȡ����
     */
    @Before(Tx.class)
    public void GetReward() {
        String userid = this.getPara("userid");
        String type = this.getPara("type");

        boolean encourageFlag = false;
        boolean spaceFlag = false;
        switch (type) {
            case "invite":
                encourageFlag = Encourage.SetOneFieldAlreadyBeGet(userid, "appInvite");
                spaceFlag = new User().IncreaseUserTotalStorageSpace(userid, 3145728.00);
                break;
            case "sign":
                encourageFlag = Encourage.SetOneFieldCanNotBeGet(userid, "appSign");
                spaceFlag = new User().IncreaseUserTotalStorageSpace(userid, 3145728.00);
                break;
            case "wifi":
                encourageFlag = Encourage.SetOneFieldAlreadyBeGet(userid, "appWiFiOpen");
                spaceFlag = new User().IncreaseUserTotalStorageSpace(userid, 3145728.00);
                break;
            case "share":
                encourageFlag = Encourage.SetOneFieldAlreadyBeGet(userid, "appShareMoments");
                spaceFlag = new User().IncreaseUserTotalStorageSpace(userid, 1572864.00);
                break;
            case "login":
                encourageFlag = Encourage.SetOneFieldAlreadyBeGet(userid, "appLoginWeb");
                spaceFlag = new User().IncreaseUserTotalStorageSpace(userid, 1048576.00);
                break;
        }
        jsonString = dataProcess.updateFlagResult(encourageFlag && spaceFlag);
        renderText(jsonString);
    }

    /**
     * APP�ɹ��������
     */
    public void AppSuccessInviteFriend() {
        String userid = this.getPara("userid");
        String beInvitedUserid = this.getPara("beInvitedUserid");

        // userid����
        userid = dataProcess.decryptData(userid, "userid");

        // �ж��û��Ƿ��Ѿ�������
        boolean isInvite = InviteList.JudgeIsInvite(userid, beInvitedUserid);
        if (isInvite) {
            jsonString = jsonData.getJson(2031, "��������ú���");
        } else {
            // ����������Ϣ
            InviteList.InsertInviteInfo(userid, beInvitedUserid);
            // ������������
            List<Record> list = Encourage.getAppEncourageInfo(userid);
            int appInviteNum = Integer.parseInt(list.get(0).get("appInviteNum").toString()) + 1;
            String encourageID = list.get(0).get("encourageID").toString();
            Encourage en = new Encourage().findById(encourageID);
            en.set("appInviteNum", appInviteNum);

            // �ж��Ƿ��������ȡ����������
            int appInvite = Integer.parseInt(list.get(0).get("appInvite").toString());
            if (appInvite == 0 && appInviteNum >= 3) {
                en.set("appInvite", 1);
            }
            jsonString = dataProcess.updateFlagResult(en.update());
        }

        renderText(jsonString);

    }

    /**
     * �����콱����
     */
    public void AchieveGetSpaceCondition() {
        String userid = this.getPara("userid");
        String type = this.getPara("type");

        List<Record> list = Encourage.getAppEncourageInfo(userid);
        boolean updateFlag = false;

        switch (type) {
            case "wifi":
                int appWiFiOpen = Integer.parseInt(list.get(0).get("appWiFiOpen").toString());
                if (appWiFiOpen == 2) {
                    updateFlag = true;
                } else {
                    updateFlag = Encourage.SetOneFieldCanBeGet(userid, "appWiFiOpen");
                }

                break;
            case "share":
                int appLoginWeb = Integer.parseInt(list.get(0).get("appShareMoments").toString());
                if (appLoginWeb == 2) {
                    updateFlag = true;
                } else {
                    updateFlag = Encourage.SetOneFieldCanBeGet(userid, "appShareMoments");
                }
                break;
        }
        jsonString = dataProcess.updateFlagResult(updateFlag);
        renderText(jsonString);
    }

    /*******************************
     * <APPǩ������> ��ӿ� End
     *******************************/

    /*******************************
     * <��ͼС����> ��ӿ� Start
     *******************************/

    /**
     * ��ȡ��ͼС����banner
     */
    public void GetPlaySmallAppBanner() {
        List<Record> list = Db.find("select bpic,btitle,bdata from banner where btype=6 and bstatus=0");
        jsonString = jsonData.getSuccessJson(list);
        renderText(jsonString);
    }

    /*******************************
     * <��ͼС����>��ӿ� End
     *******************************/

    /*******************************
     * <���пռ�>��ӿ� Start
     *******************************/

    /**
     * ��ȡʱ��banner
     */
    public void GetMomentsBanner() {
        List<Record> list = Db.find("select bpic,btitle,bdata from banner where btype=4 and bstatus=0");
        jsonString = jsonData.getSuccessJson(list);
        renderText(jsonString);
    }

    /**
     * ��ʾ�ҵĳ���
     */
    public void ShowMyCitySpace() {
        String userid = this.getPara("userid");
        // ��ȡ����ĳ��пռ��б�
        List<Record> list = Db.find(
                "select groupid,gname,gpic from groups,groupmembers where groupid=gmgroupid and gtype=14 and gmuserid="
                        + userid + " and gstatus=0 and gmstatus=0 order by gmtime desc ");
        // ��ȡ���и���������
        List<Record> cityRank = service.getCityRank();
        // ��������
        for (Record record : list) {
            String groupid = record.get("groupid").toString();
            for (Record tempRecord : cityRank) {
                String tempGroupid = tempRecord.get("groupid").toString();
                if (groupid.equals(tempGroupid)) {
                    record.set("rank", tempRecord.get("rank"));
                    break;
                }
            }
        }
        jsonString = jsonData.getSuccessJson(list);
        renderText(jsonString);

    }

    /**
     * ��ʾ��������б�
     */
    public void ShowCitySpaceList() {
        List<Record> list = service.getCityRank();
        jsonString = jsonData.getSuccessJson(list);
        renderText(jsonString);
    }

    /**
     * �����������
     */
    public void SearchCitySpace() {
        String cityName = this.getPara("cityName");

        // �������
        String regex = cityName + "+";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher;

        // ����б�
        List<Record> resultList = new ArrayList<Record>();
        // ��ȡ��������б�
        List<Record> cityList = service.getCityRank();
        // ����ƥ��ռ�
        for (Record record : cityList) {
            String gname = record.getStr("gname");
            matcher = pattern.matcher(gname);
            if (matcher.find())
                resultList.add(record);
        }
        jsonString = jsonData.getSuccessJson(resultList);
        renderText(jsonString);
    }

    /**
     * ��ʾ����������飨��������ռ��жϡ�������Ϣ���ҵĹ��ף�
     */
    public void ShowMyPhotoContribution() {
        // ��������
        String userid = this.getPara("userid");
        String groupid = this.getPara("groupid");
        // �û���Դ����
        String port = this.getPara("port");
        String fromUserID = this.getPara("fromUserID");

        // �ж��û��Ƿ��ڿռ���
        GroupMember gm = new GroupMember();
        boolean isInFlag = gm.judgeUserIsInTheAlbum(Integer.parseInt(userid), Integer.parseInt(groupid)); // trueʱ�û����ڿռ���
        boolean flag = true;
        int count = 1;
        if (isInFlag) {
            // ������ᣬ������û�����
            gm = new GroupMember().set("gmgroupid", groupid).set("gmuserid", userid).set("gmPort", port)
                    .set("gmFromUserID", fromUserID);
            // ��������쳣���û��ظ����ʱ�ᵼ�²���ʧ��
            try {
                flag = gm.save();
                // ���·���������Ա�����ֶ�
                count = Db.update("update groups set gnum = gnum+1 where groupid='" + groupid + "' ");
            } catch (ActiveRecordException e) {
                flag = true;
                count = 1;
            }
        }

        if (flag && (count == 1)) {
            // ��ȡ���пռ���Ϣ
            Record cityInfo = service.getCitySpaceInfo(groupid);
            // ��ȡ�ҵĹ���
            Record myContribution = service.getMyContributionInCitySpace(userid, groupid);

            // ���ؽ��
            Record resultRecord = new Record().set("cityInfo", cityInfo).set("myContribution", myContribution);
            List<Record> resultList = new ArrayList<Record>();
            resultList.add(resultRecord);
            jsonString = jsonData.getSuccessJson(resultList);

        } else {
            jsonString = dataProcess.insertFlagResult(false);
        }
        renderText(jsonString);

    }

    /**
     * ��ʾ������ṱ�װ�
     */
    public void ShowCitySpaceContributionRank() {
        String groupid = this.getPara("groupid");
        List<Record> list = service.getCityContributionRank(groupid);
        jsonString = jsonData.getSuccessJson(list);
        renderText(jsonString);

    }

    /******************************* <���пռ�>��ӿ� End *******************************/

    /******************************* <��ӡ>��ӿ� Start *******************************/

    /**
     * �û���ӡ״̬�ж�
     */
    public void PrintStatusJudge() {
        String userid = this.getPara("userid");
        List<Record> list = Db
                .find("select printID,printCode,printStatus,printPic from print where printUserID=" + userid + " ");
        jsonString = jsonData.getSuccessJson(list);
        renderText(jsonString);
    }

    /**
     * �µ�
     */
    public void PlaceOrder() {
        // ��ȡ����
        String userid = this.getPara("userid");
        String picAddress = this.getPara("picAddress");
        // �������ṩ����
        String openId = "1529885";
        String token = "8576205d357f8f87e3d13b3fd526b72";
        // MD5���ܻ�ȡǩ��
        String sign = YinianUtils.EncodeByMd5With32Lowcase(picAddress + token);
        // �����������
        String param = "openId=" + openId + "&picUrl=" + picAddress + "&sign=" + sign;
        String url = "https://weixin.sinzk.com/external/order";
        // �������󲢷��ؽ��
        String result = dataProcess.sendPost(url, param, "text");
        JSONObject jo = JSONObject.parseObject(result);
        String code = jo.getString("code");
        if (code.equals("10000")) {
            String orderCode = jo.getString("ordercode");
            // ��������
            Print print = new Print().set("printUserID", userid).set("printPic", picAddress).set("printCode",
                    orderCode);
            if (print.save()) {
                List<Record> resultList = dataProcess.makeSingleParamToList("orderCode", orderCode);
                jsonString = jsonData.getSuccessJson(resultList);
            } else {
                jsonString = dataProcess.insertFlagResult(false);
            }
        } else {
            jsonString = jsonData.getJson(3000, "��ӡ�µ�ʧ��");
        }
        renderText(jsonString);
    }

    /**
     * �жϴ�ӡ�Ƿ�ɹ�
     */
    public void JudgePrintIsSuccess() {
        String printCode = this.getPara("printCode");
        List<Record> list = Db.find("select printUserID,printStatus from print  where printCode=" + printCode + " ");
        jsonString = jsonData.getSuccessJson(list);
        renderText(jsonString);
    }

    /**
     * ��ӡ�ɹ�,�ص�
     */
    public void PrintSuccessCallBack() {
        String printCode = this.getPara("printCode");
        int count = Db.update("update print set printStatus=1 where printCode=" + printCode + " ");
        if (count == 1) {
            jsonString = jsonData.getSuccessJson();
        } else {
            jsonString = jsonData.getJson(3001, "��ӡ�벻����");
        }
        renderText(jsonString);
    }

    /******************************* <��ӡ>��ӿ� End *******************************/


    /********************************<��̨����ӿ�> start********************************/
    /**
     * ��ѯ��Ʒ��Ϣ�����б�
     * String elPhone,String startTime,String endTime,String pageCurrent
     */
    public void queryEnList() {
        Integer pageCurrent = Integer.parseInt(this.getPara("pageCurrent"));
        String elPhone = this.getPara("elPhone");
        String startTime = this.getPara("startTime");
        String endTime = this.getPara("endTime");
        Integer pagesize = 25;
        Integer pagenum = (pageCurrent - 1) * pagesize;
        String sql = "select u.userid,u.unickname,el.elTypeName,el.elType,el.elName,el.elAddress,el.elPhoe,el.elReceiveTime,el.elStatus"
                + "from users u , encouragelogistics el where el.elUserID=u.userid and el.elTypeName is not null ";
        if (elPhone != null && !"".equals(elPhone)) {
            sql = sql + " and elPhone like " + "'%" + elPhone + "%'";
        }
        if (startTime != null && !"".equals(startTime) && endTime != null && !"".equals(endTime)) {
            sql = sql + " and elReceiveTime between " + startTime + "and" + endTime;
        }
        sql = sql + " order by el.elReceiveTime asc limit pagenum,pagesize";
        List<Record> enlist = Db.find(sql);
        jsonString = jsonData.getSuccessJson(enlist);
        renderText(jsonString);
    }

    /**
     * ����������Ʒ��ȡ�����б�
     */
    public void addTodayEncourages() {
        Integer pageCurrent = Integer.parseInt(this.getPara("pageCurrent"));
        Integer pagesize = 25;
        Integer pagenum = (pageCurrent - 1) * pagesize;
        Calendar cal = Calendar.getInstance();
        String today = sdf.format(cal.getTime());
        String sql = "select u.userid,u.unickname,el.elTypeName,el.elType,el.elName,el.elAddress,el.elPhoe,el.elReceiveTime,el.elStatus"
                + "from users u , encouragelogistics el where el.elUserID=u.userid and el.elTypeName is not null and el.elReceiveTime like " + "'%" + today + "%'"
                + " order by el.elReceiveTime asc limit pagenum,pagesize";
        List<Record> enlist = Db.find(sql);
        jsonString = jsonData.getSuccessJson(enlist);
        renderText(jsonString);
    }

    /**
     * ����״̬�޸�
     * status elType  userid
     */
    public void updateEncourageStatus() {
        String status = this.getPara("status");
        String elType = this.getPara("elType");
        String userid = this.getPara("userid");

        EncourageLogistics el = new EncourageLogistics();
        List<Record> elist = el.getUserEncourageInfo(userid, elType);
        String elID = elist.get(0).get("elID").toString();
        EncourageLogistics elog = el.findById(elID);

        String encourageID = new Encourage().getEncourageIDbyUserID(userid);
        Encourage en = new Encourage().findById(encourageID);
        switch (status) {
            case "1":
                //�޸�����״̬
                el.set("elStatus", 1);
                if (el.update()) {
                    en.set("elType", 2);
                } else {
                    jsonString = dataProcess.insertFlagResult(el.update());
                    renderText(jsonString);
                }
                break;
            case "2":
                el.set("elStatus", 2);
                if (el.update()) {
                    en.set("elType", 3);
                } else {
                    jsonString = dataProcess.insertFlagResult(el.update());
                    renderText(jsonString);
                }
            default:
                break;
        }
        jsonString = dataProcess.insertFlagResult(en.update());
        renderText(jsonString);
    }

    /********************************<����ֽ��ӿ�> start *****************************/
    public void sendUser() {
        PagerTest pager = new PagerTest();
        String username = this.getPara("username");
        Long ts = System.currentTimeMillis();
        String url = "http://www.zhenhuaonline.cn/api/in/thirdparty/op_app/v1/handle?"
                + "app_id=a_MOkygXH5luDUjV&timestamp=" + ts + "&sign=";
        //String params=getMD5("timestamp="+ts+"api=User.GetVmOpenidapp_username=lktest001create_user=1");
        String params = pager.getMD5("api=User.GetVmOpenidapp_username=create_user=1" + "timestamp=" + ts + "zOH7c2QJb63hyejLsFWGJ3mJ58P5UsXZ");
        url += params;
        params = "api=User.GetVmOpenid&app_username=leiyutest&create_user=1";
        String jsonStr = pager.sendPost(url, params);
        JSONObject jo = JSONObject.parseObject(jsonStr);
        String json = jo.get("api_data").toString();
        JSONObject js2 = JSONObject.parseObject(json);
        String vm_openid = js2.getString("vm_openid").toString();

    }

    /**
     * ������еĻ���
     */
    public void GetActivitiGroups() {
        String type = this.getPara("type");
        String number = this.getPara("number");
        List<Record> activitiGroupList = new ArrayList<>();
        if (type.equals("initialize")) {
            //��ʼ�����ݴӻ����л�ȡ��5���ӻ���
            activitiGroupList = CacheKit.get("DataSystem", "activitiGroupList");
            if (activitiGroupList == null) {
                //����Ϊ��  �����ݿ��ѯ
                activitiGroupList = new ActivityService().getAllActivitiGroups(type, number);
                CacheKit.put("DataSystem", "activitiGroupList", activitiGroupList);
            }
        } else {
            //��ҳ����
            activitiGroupList = new ActivityService().getAllActivitiGroups(type, number);
        }
        String jsonString = jsonData.getSuccessJson(activitiGroupList);
        renderText(jsonString);
    }

    /**
     * ����Ҳ���Ļ���
     */
    public void GetMyActivitiGroups() {
        String userid = this.getPara("userid");
        String type = this.getPara("type");
        String jointime = this.getPara("jointime");
        List<Record> myActivitiList = new ArrayList<>();
        if (type.equals("initialize")) {
            //��ʼ�����ݴ�ϵͳ�����л�ȡ,5���ӻ���
            myActivitiList = CacheKit.get("ServiceCache", userid + "_myActivitiList");
            if (myActivitiList == null) {
                //�����л�ȡΪ��,�����ݿ��ѯ
                myActivitiList = new ActivityService().getMyActivitiGroups(jointime, userid, type);
                CacheKit.put("ServiceCache", userid + "_myActivitiList", myActivitiList);
            }

        } else {
            //��ҳ����
            myActivitiList = new ActivityService().getMyActivitiGroups(jointime, userid, type);
        }
        String jsonString = jsonData.getSuccessJson(myActivitiList);
        renderText(jsonString);
    }

    /**
     * �����ҳBannerͼ
     */
    public void getBanner() {
        List<Record> list = new ActivityService().getActivitiBanner();
        String jsonString = jsonData.getSuccessJson(list);
        renderText(jsonString);
    }


}
