package yinian.controller;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jfinal.aop.Before;
import com.jfinal.core.ActionKey;
import com.jfinal.core.Controller;
import com.jfinal.plugin.activerecord.ActiveRecordException;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;
import com.jfinal.plugin.activerecord.tx.Tx;
import com.jfinal.plugin.ehcache.CacheKit;

import net.sf.json.JSON;
import redis.clients.jedis.Jedis;
import yinian.app.YinianDAO;
import yinian.app.YinianDataProcess;
import yinian.common.CommonParam;
import yinian.interceptor.CrossDomain;
import yinian.model.Event;
import yinian.model.FormID;
import yinian.model.Group;
import yinian.model.GroupCanPublish;
import yinian.model.GroupMember;
import yinian.model.Like;
import yinian.model.User;
import yinian.service.EventService;
import yinian.service.H5Service;
import yinian.service.SpaceService;
import yinian.service.YinianService;
import yinian.thread.PictureVerifyThread;
import yinian.utils.JsonData;
import yinian.utils.QiniuOperate;
import yinian.utils.RedisUtils;
import yinian.utils.UrlUtils;

public class AppController extends Controller {

    // enhance������Ŀ�����AOP��ǿ
    private YinianService TxYinianService = enhance(YinianService.class);
    EventService TxEventService = enhance(EventService.class);
    private String jsonString; // ���ص�json�ַ���
    private JsonData jsonData = new JsonData(); // json������
    private QiniuOperate operate = new QiniuOperate(); // ��ţ�ƴ�����
    private YinianDAO dao = new YinianDAO();
    private YinianDataProcess dataProcess = new YinianDataProcess();// ���ݴ�����
    private YinianService yinianService = new YinianService();// ҵ������
    private EventService eventService = new EventService();// ҵ������
    private H5Service h5Service = new H5Service();
    private static String activityMenuUrl = "http://picture.zhuiyinanian.com/yinian/activityMenu.json";

    /**
     * ��ʾ��� ���ö�
     */
    @Before(CrossDomain.class)
    public void ShowGroupWithTop() {
        // ��ȡ����
        int userid = Integer.parseInt(this.getPara("userid"));
        jsonString = yinianService.showGroupWithTop(userid);
        // ���ؽ��
        renderText(jsonString);
    }


    /**
     * ��ʾ������Ϣ
     */
    public void ShowInfo() {
        // ��ȡ����
        String userid = this.getPara("userid");
        jsonString = yinianService.getPersonalInfo(userid);
        // ���ؽ��
        renderText(jsonString);
    }

    /**
     * ��ʾ�û�ͷ��Ϣ
     */
    public void ShowUserHead() {
        // ��ȡ����
        String userid = this.getPara("userid");
        jsonString = yinianService.getUserHead(userid);
        // ���ؽ��
        renderText(jsonString);
    }

    /**
     * ���������ᣬ��ҳ�˽ӿ�
     */
    @Before(Tx.class)
    public void EnterAlbum() {
        String userid = this.getPara("userid");
        String groupid = this.getPara("groupid");
        // �ж��û��Ƿ�������
        List<Record> list = Db
                .find("select * from groupmembers where gmgroupid=" + groupid + " and gmuserid=" + userid + " ");
        // ��ȡ������֮ǰ�������г�ԱID������֪ͨ
        List<Record> groupmemberList = yinianService.getGroupMemberID(groupid);
        // ��ȡ�������
        List<Record> groupTypeList = Db.find("select gtype from groups where groupid=" + groupid + " ");
        String gtype = groupTypeList.get(0).get("gtype").toString();
        if (list.size() == 0) {
            // ͨ���ж�������������Ҫ�����ĸ�֪ͨ����
            boolean insertFlag = false;
            boolean NotificationFlag = false;
            if (gtype.equals("5")) {
                TxYinianService.enterOfficialAlbum(groupid, userid);// ���鲢��������Ϣ�����û���ӵ��������б���
                // �������ݵ�likes����
                insertFlag = TxYinianService.newUserJoinInsertLikes(userid, groupid);
                NotificationFlag = TxYinianService.enterOfficialAlbumNotification(groupid, userid);
            } else {
                TxYinianService.enterGroup(groupid, userid);
                NotificationFlag = TxYinianService.insertEnterNotification(groupid, userid, groupmemberList);
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
     * ��ʾ�����ٷ����������Ϣ
     */
    public void ShowSingleOffcialAlbumInfo() {
        String groupid = this.getPara("groupid");
        String userid = this.getPara("userid");
        List<Record> list = yinianService.getSingleOfficialAlbumInfo(userid, groupid);
        jsonString = jsonData.getJson(0, "success", list);
        // ���ؽ��
        renderText(jsonString);
    }

    /**
     * �������
     */
    @Before(CrossDomain.class)
    public void CreateAlbum() {
        // ��ȡ����
        String userid = this.getPara("userid") == null ? "" : this.getPara("userid");
        String groupName = this.getPara("groupName");
        String groupType = this.getPara("groupType");
        String url = this.getPara("url");
        String source = this.getPara("source");
        String formID = this.getPara("formID") == null ? "" : this.getPara("formID"); // С�������ͱ�ID

        if (!userid.equals("") && !formID.equals("")) {
            FormID.insert(userid, formID);
        }
        if (url == null || url.equals("")) {
            // ����������ȡ��Ӧ��Ⱥͷ��ͼƬ
            switch (groupType) {
                case "0":// ������
                    url = CommonParam.qiniuOpenAddress + CommonParam.familyGroup;
                    break;
                case "1":// ������
                    url = CommonParam.qiniuOpenAddress + CommonParam.bestieGroup;
                    break;
                case "2":// ������
                    url = CommonParam.qiniuOpenAddress + CommonParam.friendGrop;
                    break;
                case "3":// ������
                    url = CommonParam.qiniuOpenAddress + CommonParam.coupleGroup;
                    break;
                case "4":// ����
                    // �ӻ����л�ȡ�������
                    List<Record> coverList = CacheKit.get("EternalCache", "spaceCover");
                    // ����Ϊ�գ����²�ѯ
                    if (coverList == null) {
                        coverList = Group.GetSpaceDefaultCoverList();
                    }
                    int size = coverList.size();
                    url = coverList.get(new Random().nextInt(size - 1)).getStr("acurl");
                    break;
                default:
                    url = CommonParam.qiniuOpenAddress + CommonParam.otherGroup;
                    break;
            }
        } else {
            String temp = url.substring(0, 7);
            if (!(temp.equals("http://"))) {
                url = CommonParam.qiniuOpenAddress + url;
            }

        }
        String inviteCode = Group.CreateSpaceInviteCode();

        jsonString = TxYinianService.createAlbum(groupName, userid, url, groupType, inviteCode, source);

        // ���ؽ��
        renderText(jsonString);
    }

    /**
     * ����ö�
     */
    //by lk  ��������ö�
    @Before(CrossDomain.class)
    public void SetGroupIsTop() {
        String userid = this.getPara("userid");
        String groupid = this.getPara("groupid");
        String isTop = this.getPara("isTop") == null ? "yes" : this.getPara("isTop");
        if (null != userid && null != groupid) {
            jsonString = yinianService.SetGroupIsTop(userid, groupid, isTop);

        } else {
            jsonString = jsonData.getJson(2, "��������");
        }
        renderText(jsonString);
    }

    /**
     * �����ռ�
     */
    public void SearchSpace() {
        String spaceName = this.getPara("spaceName");
        List<Record> result = yinianService.searchSpace(spaceName);
        jsonString = jsonData.getSuccessJson(result);
        renderText(jsonString);
    }

    /**
     * �޸ĸ��˵�������
     */
    public void ModifySingleInfo() {
        // ��ȡ����
        String userid = this.getPara("userid");
        String data = this.getPara("data");
        String type = this.getPara("type");
        boolean flag = yinianService.modifyPersonalSingleInfo(userid, data, type);
        jsonString = dataProcess.updateFlagResult(flag);
        // ���ؽ��
        renderText(jsonString);
    }

    /**
     * �޸�����
     */
    @Before(CrossDomain.class)
    public void ModifyGroupName() {
        // ��ȡ����
        String groupid = this.getPara("groupid");
        String groupName = this.getPara("groupName");
        // ��ȡ�ϴ�����������ͣ����ǹٷ���ᣨ5�����򲻲����޸�
        List<Record> list = dao.query("gtype", "groups", "groupid=" + groupid + " ");
        String type = list.get(0).get("gtype").toString();
        if (type.equals("5")) {
            jsonString = jsonData.getJson(0, "success");
        } else {
            jsonString = yinianService.modifyGroupName(groupid, groupName);
        }
        // ���ؽ��
        renderText(jsonString);
    }

    /**
     * ɾ����
     */
    public void DeleteGroup() {
        // ��ȡ����
        String userid = this.getPara("userid");
        String groupid = this.getPara("groupid");
        String source = this.getPara("source");
        if (userid == null || userid.equals("")) {
            jsonString = TxYinianService.deleteGroup(groupid, source);
            // jsonString = jsonData.getJson(1, "�������ȱʧ");
        } else {
            Group group = new Group().findById(groupid);
            String gcreator = group.get("gcreator").toString();
            if (gcreator.equals(userid)) {
                jsonString = TxYinianService.deleteGroup(groupid, source);
            } else {
                jsonString = jsonData.getJson(6, "û��Ȩ��ִ�в���");
            }
        }
        // ���ؽ��
        renderText(jsonString);
    }

    /**
     * �޸���ͷ��
     */
    @Before(CrossDomain.class)
    public void ModifyGroupPic() {
        String url = this.getPara("url");
        String groupID = this.getPara("groupID");
        // �ж�URL�Ƿ��Ѿ�ƴ�Ӻ�
        String temp = url.substring(0, 7);
        if (!(temp.equals("http://"))) {
            url = CommonParam.qiniuOpenAddress + url;
        }
        //���� by lk
        String address = operate.getDownloadToken(url + "?nrop");
        // �������󲢻�ȡ���ؽ��
        String result = new YinianDataProcess().sentNetworkRequest(address);
        if (!result.equals("")) {
            JSONObject jo = JSONObject.parseObject(result);
            int code = jo.getIntValue("code");
            if (code == 0) {
                JSONArray ja = jo.getJSONArray("fileList");
                JSONObject temp1 = ja.getJSONObject(0);

                // JSONObject temp = JSONObject.parseObject(jo.get("result").toString());
                int label = temp1.getIntValue("label");
                if (label == 0) {
                    url = null;
                }
            }
        }
        //end lk
        // ��ȡ�ϴ�����������ͣ����ǹٷ���ᣨ5�����򲻲����޸�
        List<Record> list = dao.query("gtype", "groups", "groupid=" + groupID + " ");
        String type = list.get(0).get("gtype").toString();
        if (type.equals("5")) {
            jsonString = jsonData.getJson(0, "success");
        } else {
            if (url == null) {
                jsonString = dataProcess.updateFlagResult(false);
            } else {
                boolean flag = yinianService.modifyGroupPic(url, groupID);
                jsonString = dataProcess.updateFlagResult(flag);
            }
        }
        // ���ؽ��
        renderText(jsonString);
    }

    /**
     * �˳����
     */
    public void LeaveAlbum() {
        String userid = this.getPara("userid");
        String groupid = this.getPara("groupid");
        if (userid == null || userid.equals("") || userid.equals("undefined") || userid.equals("NaN") || groupid == null || groupid.equals("") || groupid.equals("undefined") || groupid.equals("NaN")) {
            jsonString = jsonData.getJson(2, "��������", new ArrayList<Record>());
            renderText(jsonString);
            return;
        }
        String source = this.getPara("source");
        // ��ȡ�����ʣ����˳�����˽����ᣬ��ʹ��ԭ���������˳����ǹٷ���ᣬ��ʹ���˳��ٷ���᷽��
        List<Record> list = dao.query("gtype", "groups", "groupid=" + groupid + "");
        String gtype = list.get(0).get("gtype").toString();
        boolean quitFlag = false;
        if (gtype.equals("5")) {
            // �˳��ٷ���ᣬ������Ա����֪ͨ������
            quitFlag = TxYinianService.quitOfficialAlbum(userid, groupid);
        } else {
            // �˳���ᣬɾ�����Ա���ݣ����ó�Ա���͵Ķ�̬���������أ��ɹ�����trueʧ�ܷ���false
            quitFlag = TxYinianService.quitAlbum(userid, groupid, source);
        }
        jsonString = dataProcess.deleteFlagResult(quitFlag);
        // ���ؽ��
        renderText(jsonString);
    }


    /**
     * ��ʾС���������Ϣ
     */
    @Before(CrossDomain.class)
    public void ShowSmallAppAlbumInformation() {
        String uid = this.getPara("userid");
        String gid = this.getPara("groupid");
        if (uid == null || gid == null || gid.equals("undefined") || gid.equals("NaN")) {
            jsonString = jsonData.getJson(2, "��������", new ArrayList<Record>());
            renderText(jsonString);
            return;
        }
        //������
        User u = new User().findById(uid);
        int inBlackList = 1;
        if (null != u && null != u.get("ustate") && u.get("ustate").toString().equals("1")) {
            inBlackList = 0;
        }
        //������
        // ��������
        int userid = this.getParaToInt("userid");
        int groupid = this.getParaToInt("groupid");
        // �û���Դ����
        String port = this.getPara("port");
        String fromUserID = this.getPara("fromUserID");

        Group group = new Group().findById(groupid);
        int eventQRCodeCanPublish = new GroupCanPublish().getGroupPublishByType(groupid + "", "1");
        //dialogShow=1 ��ʾ��������
        int dialogShow = new GroupCanPublish().getGroupPublishByType(groupid + "", "2");
        //showAdvertisements=1 ��ʾ���λ
        int advertisementsShow = new GroupCanPublish().getGroupPublishByType(groupid + "", "3");
        int status = Integer.parseInt(group.get("gstatus").toString());
        int gtype = Integer.parseInt(group.get("gtype").toString());
        int gAuthority = Integer.parseInt(group.get("gAuthority").toString());
        // �ж�����Ƿ�ɾ��
        if (status == 0) {
            // �ж��Ƿ��������
            GroupMember gm = new GroupMember();
            boolean isInFlag = gm.judgeUserIsInTheAlbum(userid, groupid); // trueʱ�û����ڿռ���

            boolean flag = true;
            boolean getNewGnum = false;
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
                    count = Db.update("update groups set gnum = gnum+1 where groupid='" + groupid + "' ");
                    record.set("joinStatus", 0);
                    getNewGnum = true;
                } catch (ActiveRecordException e) {
                    flag = true;
                    count = 1;

                }

            }

            if (flag && (count == 1)) {
                // ��ȡ�������� ,gAuthority��������0������ 1ֻ�д����� 2-����
                record.set("gname", group.get("gname").toString()).set("gcreator", group.get("gcreator").toString())
                        .set("gtype", gtype).set("gnum", group.get("gnum").toString())
                        .set("gpic", group.get("gpic").toString()).set("gAuthority", gAuthority)
                        .set("gOrigin", group.get("gOrigin").toString())
                        .set("eventQRCodeCanPublish", eventQRCodeCanPublish)
                        .set("dialogShow", dialogShow).set("inBlackList", inBlackList).set("advertisementsShow", advertisementsShow);
                if (getNewGnum) {
                    record.set("gnum", group.getLong("gnum") + 1);
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
                List<Record> groupMember = CacheKit.get("ConcurrencyCache", groupid + "Member");
                if (groupMember == null) {
                    groupMember = Db.find(
                            "select userid,unickname,upic,gmtime from users,groups,groupmembers where groupid=gmgroupid and users.userid=groupmembers.gmuserid and gmgroupid='"
                                    + groupid + "' and gmstatus=0 order by gmid desc limit 10 ");
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


    /**
     * ��ʾʱ���� by lk test
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
                result = eventService.getSpaceTimeAxisContent(userid, groupid, type, eid, source);
                CacheKit.put("ConcurrencyCache", groupid + "InitializeEvent", result);
            }
        } else {
            result = eventService.getSpaceTimeAxisContent(userid, groupid, type, eid, source);
        }

        jsonString = jsonData.getSuccessJson(result);
        renderText(jsonString);

    }

    /**
     * �����û���ʾ��Ƭǽ�Ͷ���Ƶǽ
     */
    public void ShowPhotoAndVideoWallByUser() {
        String groupid = this.getPara("groupid");
        String type = this.getPara("type");
        String uploadTime = this.getPara("uploadTime");

        List<Record> list = new ArrayList<Record>();
        switch (type) {
            case "initialize":
                list = Db.find(
                        "select userid,unickname,upic,count(*) as num,GROUP_CONCAT(pOriginal) as url,MAX(euploadtime) as uploadtime from users,`events`,pictures where userid=euserid and eid=peid and egroupid="
                                + groupid
                                + " and estatus=0 and pstatus=0 and eMain in (0,4) GROUP BY userid ORDER BY MAX(euploadtime) desc limit 10");
                break;
            case "loading":
                list = Db.find(
                        "select userid,unickname,upic,count(*) as num,GROUP_CONCAT(pOriginal) as url,MAX(euploadtime) as uploadtime from users,`events`,pictures where userid=euserid and eid=peid and egroupid="
                                + groupid
                                + " and estatus=0 and pstatus=0 and eMain in (0,4) GROUP BY userid HAVING MAX( euploadtime ) < '"
                                + uploadTime + "'  ORDER BY MAX(euploadtime) desc limit 10");
                break;
        }
        // ��Դ��Ȩ
        QiniuOperate qiniu = new QiniuOperate();
        for (Record record : list) {
            String[] urlArray = record.getStr("url").split(",");
            List<Record> picList = new ArrayList<>();
            int end = ((urlArray.length >= 9) ? 9 : urlArray.length);
            for (int i = 0; i < end; i++) {
                String url = urlArray[i];
                String thumbnail = url + "?imageView2/1/w/200";
                String midThumbnail = url + "?imageView2/1/w/500";
                url = qiniu.getDownloadToken(url);
                midThumbnail = qiniu.getDownloadToken(midThumbnail);
                thumbnail = qiniu.getDownloadToken(thumbnail);
                Record picRecord = new Record().set("url", url).set("thumbnail", thumbnail).set("midThumbnail",
                        midThumbnail);
                picList.add(picRecord);
            }
            record.remove("url");
            record.set("picList", picList);
        }
        jsonString = jsonData.getSuccessJson(list);
        renderText(jsonString);

    }

    /**
     * ��Ƭǽ���ݳ�ʼ�������ղ鿴)
     */
    public void ShowPhotoAndVideoWall() {
        String groupid = this.getPara("groupid");
        Group g = new Group().findById(groupid);
        if (null != g && null != g.get("gtype") && g.get("gtype").toString().equals("11")) {
            jsonString = jsonData.getSuccessJson(new ArrayList<Record>());
            renderText(jsonString);
            return;
        }
        String type = this.getPara("type");
        String date = this.getPara("date");
        List<Record> result = CacheKit.get("ServiceCache", groupid + "_" + type + "_" + date + "ShowPhotoAndVideoWall");
        if (result == null) {
            result = yinianService.getPhotoAndVideoWall(groupid, type, date);
            CacheKit.put("ServiceCache", groupid + "_" + type + "_" + date + "ShowPhotoAndVideoWall", result);
        }
        //List<Record> result = service.getPhotoAndVideoWall(groupid, type, date);
        jsonString = jsonData.getSuccessJson(result);
        renderText(jsonString);
    }

    /**
     * ��ʾ��Ա�б�
     */
    @Before(CrossDomain.class)
    public void ShowGroupMember() {
        // ��ȡ����
        String userid = this.getPara("userid");
        String groupid = this.getPara("groupid");
        if (userid == null || userid.equals("") || userid.equals("undefined") || userid.equals("NaN") || groupid == null || groupid.equals("") || groupid.equals("undefined") || groupid.equals("NaN")) {
            jsonString = jsonData.getJson(2, "��������", new ArrayList<Record>());
            renderText(jsonString);
            return;
        }
        String source = this.getPara("source");
        // by lk
        if (userid != null && groupid != null && !groupid.equals("undefined") && !groupid.equals("NaN")) {
            if (source != null && !source.equals("")) {
                groupid = dataProcess.decryptData(groupid, "groupid");
            }
            jsonString = yinianService.getMemberList(userid, groupid);
        } else {
            jsonString = jsonData.getJson(2, "��������");
        }
        // ���ؽ��
        renderText(jsonString);
    }

    /**
     * by lk ��ȡ��˵�
     */
    public void GetActivityMenu() {
        String returnValue = jsonData.getJson(2, "�����������");
        int groupid = Integer.parseInt(
                null != this.getPara("groupid") && !this.getPara("groupid").equals("") ? this.getPara("groupid") : "0");
        if (groupid == 0) {
            renderText(returnValue);
            return;
        }
        Group group = new Group().findById(groupid);

        // ��ȡ��Ƭ��������
        List<Record> photo = CacheKit.get("ConcurrencyCache", groupid + "Photo");
        if (photo == null) {
            photo = Db.find(
                    "select count(*) as gpicNum from groups,events,pictures where peid=eid and groupid=egroupid and groupid="
                            + groupid + " and estatus in(0,3) and pstatus=0 ");
            CacheKit.put("ConcurrencyCache", groupid + "Photo", photo);
        }
        String content = UrlUtils.loadJson(activityMenuUrl);
        // List<Record> likelist = new Event().GetListByGroup(groupid, 1);// ��ȡ���޵�һ��
        // List<Record> publishlist = new Event().GetUsePublishPhotoCont(groupid, 0,
        // false, 1);// ��ȡ��Ƭ���˵�һ��

        // String returnValue=jsonData.getJson(2, "�����������");
        Record r = new Record();
        if (content != null && content.length() > 0) {

            // returnValue=jsonData.getJson(0, "success", JSONArray.fromObject(content));
            r.set("menu", net.sf.json.JSONArray.fromObject(content));
        }
        List<Record> newReturnList = new ArrayList<Record>();

        // r.set("like", likelist);
        // r.set("publish", publishlist);
        r.set("memberCnt", group.get("gnum").toString());
        r.set("photoCnt", photo.get(0).get("gpicNum").toString());
        r.set("gpic", group.get("gpic").toString());
        r.set("gname", group.get("gname").toString());
        newReturnList.add(r);
        returnValue = jsonData.getSuccessJson(newReturnList);
        renderText(returnValue);
    }

    /**
     * ��������
     */

    public void GetGroupLikeList() {
        if (this.getPara("groupid") == null || this.getPara("groupid").equals("") || this.getPara("uid") == null || this.getPara("uid").equals("")) {
            jsonString = jsonData.getJson(2, "��������", new ArrayList<Record>());
            renderText(jsonString);
            return;
        }
        String gid = this.getPara("groupid");
        String userid = this.getPara("uid");
        if (userid.equals("") || gid.equals("") || gid == null || gid.equals("undefined") || gid.equals("NaN")) {
            jsonString = jsonData.getJson(2, "��������", new ArrayList<Record>());
            renderText(jsonString);
            return;
        }
        int groupid = Integer.parseInt(this.getPara("groupid"));
        int uid = Integer.parseInt(this.getPara("uid"));
        int searchLimit = Integer
                .parseInt(this.getPara("searchLimit") != null && !this.getPara("searchLimit").equals("")
                        ? this.getPara("searchLimit")
                        : "50");
        List<Record> list = CacheKit.get("ConcurrencyCache", groupid + "GroupLikeList");
        if (list == null) {
            list = new SpaceService().GetListByElikeAndGroup(groupid, uid, searchLimit);
            CacheKit.put("ConcurrencyCache", groupid + "GroupLikeList", list);
        }
        //List<Record> list = new SpaceService().GetListByElikeAndGroup(groupid, uid, searchLimit);
        // List<Record> list=new Event().GetListByElikeAndGroup(groupid,eid);
        jsonString = jsonData.getJson(0, "success", list);
        renderText(jsonString);
    }

    /**
     * ��Ƭ����
     */
    public void GetPublishList() {
        if (this.getPara("groupid") == null || this.getPara("groupid").equals("") || this.getPara("uid") == null || this.getPara("uid").equals("")) {
            jsonString = jsonData.getJson(2, "��������", new ArrayList<Record>());
            renderText(jsonString);
            return;
        }
        int groupid = Integer.parseInt(this.getPara("groupid"));
        int uid = Integer.parseInt(this.getPara("uid"));
        int searchLimit = Integer
                .parseInt(this.getPara("searchLimit") != null && !this.getPara("searchLimit").equals("")
                        ? this.getPara("searchLimit")
                        : "100");
        String returnValue = jsonData.getJson(0, "success",
                new SpaceService().GetPublishList(uid, groupid, searchLimit));
        renderText(returnValue);
        // ����ͼ QiniuOperate a = new QiniuOperate();
        // String url = a.getDownloadToken(url+"?imageView2/2/w/200");
    }

    /**
     * �޸Ķ�̬�ȼ�,�ö�����
     */
    //@ActionKey("/yinian/ModifyEventLevel")
    public void ModifyEventLevel() {
        String userid = this.getPara("userid");
        int groupid = Integer.parseInt(this.getPara("groupid"));
        String eid = this.getPara("eid");
        String type = this.getPara("type");

        // Ȩ���ж�
        Group group = new Group().findById(groupid);
        String gcreator = group.get("gcreator").toString();
        if (userid.equals(gcreator)) {
            Event event = new Event().findById(eid);

            // �ж�����ԭ�ռ仹���Ƽ���ռ��ö�
            int egroupid = Integer.parseInt(event.get("egroupid").toString());

            String key = (egroupid == groupid ? "elevel" : "isTopInRecommendGroup");

            if (type.equals("stick")) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String time = sdf.format(new Date());
                event.set(key, 1).set("eTopTime", time);
            }
            if (type.equals("cancel")) {
                event.set(key, 0);
            }
            jsonString = dataProcess.updateFlagResult(event.update());
        } else {
            jsonString = jsonData.getJson(6, "û��Ȩ��ִ�в���");
        }
        renderText(jsonString);

    }

    /**
     * �ϴ���̬
     */
    @Before(CrossDomain.class)
    public void UploadEvent() {
        // ˭�����ĸ��ռ���
        String userid = this.getPara("userid") == null ? "" : this.getPara("userid");
        String groupid = this.getPara("groupid") == null ? "" : this.getPara("groupid");
        // ͼƬ
        String picAddress = this.getPara("picAddress") == null ? "" : this.getPara("picAddress");
        // ����
        String content = this.getPara("content") == null ? "" : this.getPara("content");
        // ����
        String audio = this.getPara("audio") == null ? "" : this.getPara("audio");
        // �ص�
        String place = this.getPara("place") == null ? "" : this.getPara("place");
        String placePic = this.getPara("placePic") == null ? "" : this.getPara("placePic");// λ�����ɵ�ͼƬ��ַ
        String placeLongitude = this.getPara("placeLongitude") == null ? "" : this.getPara("placeLongitude");// ����
        String placeLatitude = this.getPara("placeLatitude") == null ? "" : this.getPara("placeLatitude");// γ��
        // ��˭
        String peopleName = this.getPara("peopleName") == null ? "" : this.getPara("peopleName");
        // ��̬���ĸ�Ҫ��Ϊ��
        String main = this.getPara("main") == null ? "" : this.getPara("main"); // 0--��Ƭ 1--���� 2--���� 3--�ص�
        // ����Ԫ��
        String storage = this.getPara("storage") == null ? "" : this.getPara("storage");// �洢�ռ�
        String source = this.getPara("source") == null ? "" : this.getPara("source");// �жϽӿ���Դ
        String isPush = this.getPara("isPush") == null ? "" : this.getPara("isPush"); // �����ж� app:yes/no С����:true/false
        String formID = this.getPara("formID") == null ? "" : this.getPara("formID"); // С�������ͱ�ID


        // ����formID
        //FormID.insertFormID(userid, formID);
        if (!userid.equals("") && !formID.equals("")) {
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
            picArray = dataProcess.PictureVerify(picArray);
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
            boolean flag1 = false;
            // ����ռ��ϴ�
            for (int i = 0; i < IDs.length; i++) {
                // ͬ�����,0--��ͬ�� 1--ͬ�� ,��һ���ռ�Ϊԭ��������Ϊͬ��
                int isSynchronize = (i == 0 ? 0 : 1);
                eid = TxEventService.upload(userid, IDs[i], picArray, content, audio, place, placePic, placeLongitude,
                        placeLatitude, peopleName, main, storagePlace, firstPic, isPush, source, isSynchronize, formID);
                if (eid != 0) {
                    // ˵���ϴ��ɹ�
                    List<Record> result = eventService.getSingleEvent(eid, source);// ��ȡ��̬����Ϣ
                    flag1 = true;
                    jsonString = jsonData.getSuccessJson(result);
                } else {
                    flag1 = false;
                    jsonString = jsonData.getJson(-50, "���ݲ��뵽���ݿ�ʧ��");
                    break;
                }
            }
        }
        // ���ؽ��
        renderText(jsonString);
    }

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
        if (!cover.equals(""))
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
                int eid = TxEventService.uploadShortVedio(userid, IDs[i], address, content, storagePlace, place, cover, time,
                        isSynchronize, source);
                eventID = eid;
                if (eid == 0) {
                    flag = false;
                    break;
                }
            }
            if (flag) {
                // ˵���ϴ��ɹ�
                List<Record> result = eventService.getSingleEvent(eventID, source);// ��ȡ��̬����Ϣ
                jsonString = jsonData.getSuccessJson(result);
            } else {
                jsonString = jsonData.getJson(-50, "���ݲ��뵽���ݿ�ʧ��");
            }
        }

        // ���ؽ��
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
            List<Record> result = eventService.getMyEvents2ndVersion(userid, eid, source);
            jsonString = jsonData.getSuccessJson(result);
        }

        renderText(jsonString);
    }

    /**
     * ��ʾʱ�� by lk �򻯰汾
     */
    public void ShowMoments_sim() {
        System.out.println("ShowMoments��ʼ��" + System.currentTimeMillis());
        String userid = this.getPara("userid");
        String type = this.getPara("type");
        String eid = this.getPara("eid");
        if (userid == null || userid.equals("") || userid.equals("undefined") || userid.equals("NaN") || eid == null || eid.equals("") || eid.equals("undefined") || eid.equals("NaN")) {
            jsonString = jsonData.getJson(2, "��������", new ArrayList<Record>());
            renderText(jsonString);
            return;
        }
        List<Record> result = eventService.GetMoments_sim(userid, type, eid);
        jsonString = jsonData.getSuccessJson(result);
        renderText(jsonString);
        System.out.println("ShowMoments������" + System.currentTimeMillis());
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

        if (minID == null || null == this.getPara("groupid") || this.getPara("groupid").equals("")) {
            // ˢ����û�������ˣ�ֱ�ӷ���
            jsonString = jsonData.getSuccessJson();
        } else {
            int groupid = this.getParaToInt("groupid");
            int eid = Integer.parseInt(minID);
            List<Record> result = eventService.getSpaceMemberEvents(groupid, userid, eid, source);
            jsonString = jsonData.getSuccessJson(result);
        }
        renderText(jsonString);
    }

    /**
     * ��ȡ�ռ��Ա��Ƭ��
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
     * ��ȡ�û�����
     */
    @Before(CrossDomain.class)
    public void GetUserData() {
        String userid = this.getPara("userid");
        String source = this.getPara("source");

        // userid����
        if (source != null && source.equals("h5")) {
            userid = dataProcess.decryptData(userid, "userid");
        }
        //������
        User u = new User().findById(userid);
        int inBlackList = 1;
        if (null != u && null != u.get("ustate") && u.get("ustate").toString().equals("1")) {
            inBlackList = 0;
        }
        //������
        Record record = new Record();
        if (userid != null) {
            Record albumRecord = Db.findFirst(
                    "select count(*) as number from groups,groupmembers where groupid=gmgroupid and gmuserid=" + userid
                            + " and gmstatus=0 and gstatus=0 and gtype not in (5,12) and groupid not in ("
                            + CommonParam.ActivitySpaceID + ")");
            Record eventRecord = Db
                    .findFirst("select count(*) as number from events where euserid=" + userid + " and estatus=0 ");
            Record photoRecord = Db.findFirst("select count(*) as number from events,pictures where euserid=" + userid
                    + " and eid=peid and estatus=0 and pstatus=0 ");
            record.set("album", albumRecord.get("number").toString()).set("event", eventRecord.get("number").toString())
                    .set("photo", photoRecord.get("number").toString()).set("inBlackList", inBlackList);
            // ��ȡ�û��洢�ռ���Ϣ
            Record storage = Db
                    .findFirst("select uusespace,utotalspace,upic,unickname,ubackground,userid from users where userid="
                            + userid + " ");
            java.text.DecimalFormat df = new java.text.DecimalFormat("########");
            System.out.println("----" + df.format(storage.getDouble("utotalspace")));
            System.out.println("111----" + df.format(storage.getDouble("uusespace")));
            record.setColumns(storage);
        } else {
            record.set("album", 0).set("event", 0).set("photo", 0).set("uusespace", 0).set("utotalspace", 0).set("inBlackList", inBlackList);
        }
        List<Record> list = new ArrayList<Record>();
        list.add(record);
        jsonString = jsonData.getJson(0, "success", list);
        renderText(jsonString);
    }

    /**
     * ������������ȡ������ lk �޸ķ��ض���
     */
    public void AttachOrRemoveExpressionByLkNew() {
        String userid = this.getPara("userid") == null ? "" : this.getPara("userid");
        String eid = this.getPara("eid");
        String type = this.getPara("type");
        String source = this.getPara("source");
        String formID = this.getPara("formID") == null ? "" : this.getPara("formID"); // С�������ͱ�ID
        // ����formID
        //FormID.insertFormID(userid, formID);
        if (!userid.equals("") && !formID.equals("")) {
            FormID.insert(userid, formID);
        }
        int status = 0;
        if (source != null && source.equals("app")) {
            // appֱ�Ӵ�ֵ
            status = Integer.parseInt(type);
        } else {
            // С����Ӣ��,��type�ĳɶ�Ӧstatus
            switch (type) {
                case "like":
                    status = 0;
                    break;
                case "happy":
                    status = 2;
                    break;
                case "sad":
                    status = 3;
                    break;
                case "mad":
                    status = 4;
                    break;
                case "surprise":
                    status = 5;
                    break;
                case "unlike":
                    status = 1;
                    break;
            }
        }
        // �ж��û��Ƿ�����ز���
        List<Record> judge = Db
                .find("select * from `like` where likeEventID=" + eid + " and likeUserID=" + userid + " ");

        List<Record> result = new ArrayList<Record>();
//				String sql = "select likeID,likeUserID,likeStatus,upic from `like`,users where userid=likeUserID and likeEventID="
//						+ eid + " and likeStatus!=1";
        String sql = "select unickname,likeID,likeUserID,likeStatus,upic from `like`,users where userid=likeUserID and likeEventID="
                + eid + " and likeStatus!=1 order by likeID desc limit 0,10";
        String likeCntSql = "select count(*) cnt from `like` where likeEventID="
                + eid + " and likeStatus!=1 ";
        String iLikeSql = "select count(*) cnt from `like`,users where userid=likeUserID and likeEventID="
                + eid + " and userid='" + userid + "' and likeStatus!=1 ";
        Like like;
        if (judge.size() == 0) {
            // û�в�����
            like = new Like().set("likeEventID", eid).set("likeUserID", userid).set("likeStatus", status);
            if (like.save()) {
                result = Db.find(sql);
                result = dataProcess.changeLikeStatusToWord(result);
                List<Record> returnList = new ArrayList<Record>();
                Record r = new Record();
                r.set("likeCnt", 0);
                //��������ȡredis���棬��û�л������ȡ���ݿ⣬ͬʱ���»���
                Jedis jedis = RedisUtils.getRedis();
                if (null != jedis) {
                    //�ӻ����ж�ȡ��ǰeid�ĵ�����
                    String likeCnt = jedis.get("likeCnt_" + eid);
                    if (null != likeCnt && !"".equals(likeCnt)) {
                        //���޳ɹ��󻺴��������1
                        int likeCntInt = Integer.valueOf(likeCnt) + 1;
                        String jr = jedis.set("likeCnt_" + eid, String.valueOf(likeCntInt));
                        if (null != jr && "OK".equals(jr)) {
                            r.set("likeCnt", likeCntInt);
                        }
                    } else {
                        //��ǰeidδ����������������ݿ�count����������ͬ��������
                        List<Record> cntList = Db.find(likeCntSql);
                        if (!cntList.isEmpty()) {
                            r.set("likeCnt", cntList.get(0).get("cnt"));
                            jedis.set("likeCnt_" + eid, cntList.get(0).get("cnt").toString());
                        }
                    }
                    //�ͷ�redis
                    RedisUtils.returnResource(jedis);
                } else {
                    List<Record> cntList = Db.find(likeCntSql);
                    if (!cntList.isEmpty()) {
                        r.set("likeCnt", cntList.get(0).get("cnt"));
                    }
                }
                r.set("likeUser", 0);
                List<Record> iLikeCntList = Db.find(iLikeSql);
                if (!iLikeCntList.isEmpty()) {
                    r.set("likeUser", iLikeCntList.get(0).get("cnt"));
                }
                r.set("like", result);

                returnList.add(r);
                jsonString = jsonData.getSuccessJson(returnList);
                //jsonString = jsonData.getSuccessJson(result);
            } else {
                jsonString = dataProcess.insertFlagResult(false);
            }

        } else {
            // �в�����
            int likeID = Integer.parseInt(judge.get(0).get("likeID").toString());
            like = new Like().findById(likeID);
            like.set("likeStatus", status);
            if (like.update()) {
                result = Db.find(sql);
                result = dataProcess.changeLikeStatusToWord(result);
                List<Record> returnList = new ArrayList<Record>();
                //Record r=new Record();
                Record r = new Record();
                r.set("likeCnt", 0);
                //��������ȡredis���棬��û�л������ȡ���ݿ⣬ͬʱ���»���
                Jedis jedis = RedisUtils.getRedis();
                if (null != jedis) {
                    //�ӻ����ж�ȡ��ǰeid�ĵ�����
                    String likeCnt = jedis.get("likeCnt_" + eid);
                    if (null != likeCnt && !"".equals(likeCnt)) {
                        if (status == 1) {
                            //ȡ������ʱ�������������1
                            int likeCntInt = Integer.valueOf(likeCnt) > 0 ? Integer.valueOf(likeCnt) - 1 : 0;
                            String jr = jedis.set("likeCnt_" + eid, String.valueOf(likeCntInt));
                            if (null != jr && "OK".equals(jr)) {
                                r.set("likeCnt", likeCntInt);
                            }
                        } else {
                            //��ȡ�����ޣ��������������
                            r.set("likeCnt", Integer.valueOf(likeCnt));
                        }
                    } else {
                        //��ǰeidδ����������������ݿ�count����������ͬ��������
                        List<Record> cntList = Db.find(likeCntSql);
                        if (!cntList.isEmpty()) {
                            r.set("likeCnt", cntList.get(0).get("cnt"));
                            jedis.set("likeCnt_" + eid, cntList.get(0).get("cnt").toString());
                        }
                    }
                    //�ͷ�redis
                    RedisUtils.returnResource(jedis);
                } else {
                    List<Record> cntList = Db.find(likeCntSql);
                    if (!cntList.isEmpty()) {
                        r.set("likeCnt", cntList.get(0).get("cnt"));
                    }
                }
                r.set("like", result);
                r.set("likeUser", 0);
                List<Record> iLikeCntList = Db.find(iLikeSql);
                if (!iLikeCntList.isEmpty()) {
                    r.set("likeUser", iLikeCntList.get(0).get("cnt"));
                }
                returnList.add(r);
                jsonString = jsonData.getSuccessJson(returnList);
            } else {
                jsonString = dataProcess.updateFlagResult(false);
            }

        }

        renderText(jsonString);
        // �ж��û��Ƿ�����ز���
    }

    /**
     * �����ռ��Ա by lk �޸ķ�����������
     */
    public void SearchSpaceMembersReturnFormat() {
        String groupid = this.getPara("groupid");
        String name = this.getPara("name");
        String sql = "select userid,unickname,upic,gmtime,gname from users,groups,groupmembers where groupid=gmgroupid and users.userid=groupmembers.gmuserid and gmgroupid='"
                + groupid + "' and  unickname like '%" + name + "%' ";
        List<Record> list = Db.find(sql);
        List<Record> returnList = new ArrayList<Record>();
        for (Record r : list) {
            Record record = new Record();
            record.set("gmtime", list.get(0).get("gmtime"));
            record.set("gname", list.get(0).get("gname").toString());
            record.set("user", r);
            record.set("selected", false);
            returnList.add(record);
        }
        jsonString = jsonData.getSuccessJson(returnList);
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
                if (null == list || list.isEmpty()) {
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


    /**
     * ��ȡ����������Ƭ,��Ƭǽ
     */
    public void GetAllPhotosInOneGroup() {
        // ��ȡ����
        String groupid = this.getPara("groupid");
        String type = this.getPara("type");
        int id = Integer.parseInt(this.getPara("id"));
        List<Record> list = yinianService.getGroupPhotos(groupid, type, id);
        jsonString = jsonData.getJson(0, "success", list);
        // ���ؽ��
        renderText(jsonString);
    }

    /**
     * ���÷���Ȩ��
     */
    @Before({Tx.class, CrossDomain.class})
    public void SetUploadAuthority() {
        String groupid = this.getPara("groupid");
        String userid = this.getPara("userid");
        String authorityType = this.getPara("authorityType");// ����ֵ:all,onlyCreator,part
        String type = this.getPara("type");

        Group group = new Group().findById(groupid);
        switch (authorityType) {
            case "all":
                group.set("gAuthority", 0);
                jsonString = dataProcess.updateFlagResult(group.update());
                break;
            case "onlyCreator":
                group.set("gAuthority", 1);
                jsonString = dataProcess.updateFlagResult(group.update());
                break;
            case "part":
                group.set("gAuthority", 2);
                boolean groupFlag = group.update();
                int size = userid.split(",").length;
                int count = 0;// �������ݿ����
                if (type.equals("add")) {
                    // ���Ȩ�ޣ�1��ʾ��Ȩ��
                    count = Db.update("update groupmembers set gmauthority=1 where gmuserid in (" + userid
                            + ") and gmgroupid=" + groupid + "  ");
                } else {
                    // ȡ��Ȩ�ޣ�0��ʾ��Ȩ�ޣ�Ĭ��Ϊ0
                    count = Db.update("update groupmembers set gmauthority=0 where gmuserid in (" + userid
                            + ") and gmgroupid=" + groupid + "  ");
                }
                boolean groupmemberFlag = (count == size);
                jsonString = dataProcess.updateFlagResult(groupFlag && groupmemberFlag);
                break;
        }
        renderText(jsonString);
    }

    /**
     * �����ռ��Ա
     */
    public void SearchSpaceMembers() {
        String groupid = this.getPara("groupid");
        String name = this.getPara("name");

        List<Record> list = Db
                .find("select userid,unickname,upic from users,groupmembers where gmuserid=userid and gmgroupid="
                        + groupid + " and unickname like '%" + name + "%' ");

        jsonString = jsonData.getSuccessJson(list);
        renderText(jsonString);

    }

    /**
     * �鿴�ռ��ԱȨ���б�
     */
    public void GetSpaceMemberAuthorityList() {
        String groupid = this.getPara("groupid");
        String type = this.getPara("type");// ����ֵ������100����С��100��ʱ���ز�ͬ������

        String sql = "";
        switch (type) {
            case "bigger":
                // ֻ��ʾ��Ȩ����Ϣ
                sql = "select userid,unickname,upic,gmauthority from users,groupmembers where userid=gmuserid and gmgroupid="
                        + groupid + " and gmauthority=1 ";
                break;
            case "smaller":
                // ��ʾ�ռ��Ա������Ȩ��Ϣ
                sql = "select userid,unickname,upic,gmauthority from users,groupmembers where userid=gmuserid and gmgroupid="
                        + groupid + " ";
                break;
        }
        List<Record> list = Db.find(sql);
        jsonString = jsonData.getSuccessJson(list);
        renderText(jsonString);
    }

    /**
     * ����ͼƬIDɾ��ͼƬ�����û�ɾ����һ����̬�����е�ͼƬ��ɾ����̬
     */
    public void deletePic() {
        jsonString = jsonData.getJson(2, "�����������");
        String userid = this.getPara("userid") == null ? "" : this.getPara("userid");
        String pid = this.getPara("pid") == null ? "" : this.getPara("pid");
        String source = this.getPara("source");
        if (!pid.equals("")) {
            List<Record> list = h5Service.deletePic(userid, pid);
            jsonString = jsonData.getSuccessJson(list);
        }
        System.out.println("jsonString:" + jsonString);
        renderText(jsonString);
    }

    /**
     * ɾ������
     */
    public void DeleteComment() {
        String commentID = this.getPara("commentID");
        boolean flag = yinianService.deleteComment(commentID);
        jsonString = dataProcess.updateFlagResult(flag);
        // ���ؽ��
        renderText(jsonString);
    }

    /**
     * ��������1 1.1�汾 ���������ֶ� cid
     */
    public void SendComment1() {
        String commentUserId = this.getPara("commentUserId") == null ? "" : this.getPara("commentUserId");// ������ID
        String commentedUserId = this.getPara("commentedUserId");// ��������ID
        String eventId = this.getPara("eventId");// �¼�ID
        String content = this.getPara("content");// ��������
        String place = this.getPara("place");
        String formID = this.getPara("formID") == null ? "" : this.getPara("formID"); // С�������ͱ�ID

        if (!commentUserId.equals("") && !formID.equals("")) {
            FormID.insert(commentUserId, formID);
        }
        String cid = TxYinianService.sendComment1(commentUserId, commentedUserId, eventId, content, place);
        if (cid.equals("")) {
            jsonString = jsonData.getJson(-50, "��������ʧ��");
        } else {
            List<Record> list = dataProcess.makeSingleParamToList("cid", cid);
            jsonString = jsonData.getJson(0, "success", list);
        }
        // ���ؽ��
        renderText(jsonString);
    }

    /**
     * ��ȡͬ���ռ��б����ų����ϴ�Ȩ�޿ռ�
     */
    @Before(CrossDomain.class)
    public void GetSynchronizeSpaceList() {
        String userid = this.getPara("userid");
        if (userid != null && !userid.equals("")) {
            List<Record> list = Db.find(
                    "select isDefault,openGId,groupid,gname,gpic from groups,groupmembers where groupid=gmgroupid and gstatus=0 and gmstatus=0 and gmuserid="
                            + userid + " and gtype!=5 and (gcreator=" + userid + " or (gcreator!=" + userid
                            + " and (gAuthority=0 or (gAuthority=2 and gmauthority=1) )))");
            jsonString = jsonData.getSuccessJson(list);
        } else {
            jsonString = jsonData.getJson(2, "�����������");
        }
        renderText(jsonString);
    }

    /**
     * �߳����
     */
    public void KickMembers() {
        String owner = this.getPara("owner");
        String userid = this.getPara("userid");
        String groupid = this.getPara("groupid");

        Group group = new Group().findById(groupid);
        boolean quitFlag = false;
        if (owner.equals(group.get("gcreator").toString())) {
            String[] IDs = userid.split(",");
            for (int i = 0; i < IDs.length; i++) {
                // ����Ա�߳���ᣬɾ�����Ա���ݣ����ó�Ա���͵Ķ�̬���������أ��ɹ�����trueʧ�ܷ���false
                if (owner.equals(IDs[i])) {
                    // �ж��߳������ǲ����Լ�
                    jsonString = jsonData.getJson(1035, "�����߳��Լ�");
                } else {
                    quitFlag = TxYinianService.kickOutAlbum(IDs[i], groupid);
                    if (!quitFlag) {
                        break;
                    }
                }

            }
            jsonString = dataProcess.deleteFlagResult(quitFlag);
        } else {
            jsonString = jsonData.getJson(1034, "��Ȩ������");
        }

        // ���ؽ��
        renderText(jsonString);

    }


}
