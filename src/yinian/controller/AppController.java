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

    // enhance方法对目标进行AOP增强
    private YinianService TxYinianService = enhance(YinianService.class);
    EventService TxEventService = enhance(EventService.class);
    private String jsonString; // 返回的json字符串
    private JsonData jsonData = new JsonData(); // json操作类
    private QiniuOperate operate = new QiniuOperate(); // 七牛云处理类
    private YinianDAO dao = new YinianDAO();
    private YinianDataProcess dataProcess = new YinianDataProcess();// 数据处理类
    private YinianService yinianService = new YinianService();// 业务层对象
    private EventService eventService = new EventService();// 业务层对象
    private H5Service h5Service = new H5Service();
    private static String activityMenuUrl = "http://picture.zhuiyinanian.com/yinian/activityMenu.json";

    /**
     * 显示相册 带置顶
     */
    @Before(CrossDomain.class)
    public void ShowGroupWithTop() {
        // 获取参数
        int userid = Integer.parseInt(this.getPara("userid"));
        jsonString = yinianService.showGroupWithTop(userid);
        // 返回结果
        renderText(jsonString);
    }


    /**
     * 显示个人信息
     */
    public void ShowInfo() {
        // 获取参数
        String userid = this.getPara("userid");
        jsonString = yinianService.getPersonalInfo(userid);
        // 返回结果
        renderText(jsonString);
    }

    /**
     * 显示用户头信息
     */
    public void ShowUserHead() {
        // 获取参数
        String userid = this.getPara("userid");
        jsonString = yinianService.getUserHead(userid);
        // 返回结果
        renderText(jsonString);
    }

    /**
     * 申请进入相册，网页端接口
     */
    @Before(Tx.class)
    public void EnterAlbum() {
        String userid = this.getPara("userid");
        String groupid = this.getPara("groupid");
        // 判断用户是否在组内
        List<Record> list = Db
                .find("select * from groupmembers where gmgroupid=" + groupid + " and gmuserid=" + userid + " ");
        // 获取进入组之前改组所有成员ID，留作通知
        List<Record> groupmemberList = yinianService.getGroupMemberID(groupid);
        // 获取相册类型
        List<Record> groupTypeList = Db.find("select gtype from groups where groupid=" + groupid + " ");
        String gtype = groupTypeList.get(0).get("gtype").toString();
        if (list.size() == 0) {
            // 通过判断组类型来决定要调用哪个通知方法
            boolean insertFlag = false;
            boolean NotificationFlag = false;
            if (gtype.equals("5")) {
                TxYinianService.enterOfficialAlbum(groupid, userid);// 进组并返回组信息用于用户添加到他的组列表中
                // 插入数据到likes表中
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
                jsonString = jsonData.getJson(-51, "更新数据失败");
            }
        } else {
            jsonString = jsonData.getJson(1010, "用户已在组内");
        }
        renderText(jsonString);
    }

    /**
     * 显示单个官方相册的相册信息
     */
    public void ShowSingleOffcialAlbumInfo() {
        String groupid = this.getPara("groupid");
        String userid = this.getPara("userid");
        List<Record> list = yinianService.getSingleOfficialAlbumInfo(userid, groupid);
        jsonString = jsonData.getJson(0, "success", list);
        // 返回结果
        renderText(jsonString);
    }

    /**
     * 创建相册
     */
    @Before(CrossDomain.class)
    public void CreateAlbum() {
        // 获取参数
        String userid = this.getPara("userid") == null ? "" : this.getPara("userid");
        String groupName = this.getPara("groupName");
        String groupType = this.getPara("groupType");
        String url = this.getPara("url");
        String source = this.getPara("source");
        String formID = this.getPara("formID") == null ? "" : this.getPara("formID"); // 小程序推送表单ID

        if (!userid.equals("") && !formID.equals("")) {
            FormID.insert(userid, formID);
        }
        if (url == null || url.equals("")) {
            // 根据组类别获取相应的群头像图片
            switch (groupType) {
                case "0":// 家人组
                    url = CommonParam.qiniuOpenAddress + CommonParam.familyGroup;
                    break;
                case "1":// 闺蜜组
                    url = CommonParam.qiniuOpenAddress + CommonParam.bestieGroup;
                    break;
                case "2":// 死党组
                    url = CommonParam.qiniuOpenAddress + CommonParam.friendGrop;
                    break;
                case "3":// 情侣组
                    url = CommonParam.qiniuOpenAddress + CommonParam.coupleGroup;
                    break;
                case "4":// 其他
                    // 从缓存中获取随机封面
                    List<Record> coverList = CacheKit.get("EternalCache", "spaceCover");
                    // 缓存为空，重新查询
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

        // 返回结果
        renderText(jsonString);
    }

    /**
     * 相册置顶
     */
    //by lk  设置相册置顶
    @Before(CrossDomain.class)
    public void SetGroupIsTop() {
        String userid = this.getPara("userid");
        String groupid = this.getPara("groupid");
        String isTop = this.getPara("isTop") == null ? "yes" : this.getPara("isTop");
        if (null != userid && null != groupid) {
            jsonString = yinianService.SetGroupIsTop(userid, groupid, isTop);

        } else {
            jsonString = jsonData.getJson(2, "参数错误");
        }
        renderText(jsonString);
    }

    /**
     * 搜索空间
     */
    public void SearchSpace() {
        String spaceName = this.getPara("spaceName");
        List<Record> result = yinianService.searchSpace(spaceName);
        jsonString = jsonData.getSuccessJson(result);
        renderText(jsonString);
    }

    /**
     * 修改个人单项资料
     */
    public void ModifySingleInfo() {
        // 获取参数
        String userid = this.getPara("userid");
        String data = this.getPara("data");
        String type = this.getPara("type");
        boolean flag = yinianService.modifyPersonalSingleInfo(userid, data, type);
        jsonString = dataProcess.updateFlagResult(flag);
        // 返回结果
        renderText(jsonString);
    }

    /**
     * 修改组名
     */
    @Before(CrossDomain.class)
    public void ModifyGroupName() {
        // 获取参数
        String groupid = this.getPara("groupid");
        String groupName = this.getPara("groupName");
        // 获取上传所在组的类型，若是官方相册（5），则不不能修改
        List<Record> list = dao.query("gtype", "groups", "groupid=" + groupid + " ");
        String type = list.get(0).get("gtype").toString();
        if (type.equals("5")) {
            jsonString = jsonData.getJson(0, "success");
        } else {
            jsonString = yinianService.modifyGroupName(groupid, groupName);
        }
        // 返回结果
        renderText(jsonString);
    }

    /**
     * 删除组
     */
    public void DeleteGroup() {
        // 获取参数
        String userid = this.getPara("userid");
        String groupid = this.getPara("groupid");
        String source = this.getPara("source");
        if (userid == null || userid.equals("")) {
            jsonString = TxYinianService.deleteGroup(groupid, source);
            // jsonString = jsonData.getJson(1, "请求参数缺失");
        } else {
            Group group = new Group().findById(groupid);
            String gcreator = group.get("gcreator").toString();
            if (gcreator.equals(userid)) {
                jsonString = TxYinianService.deleteGroup(groupid, source);
            } else {
                jsonString = jsonData.getJson(6, "没有权限执行操作");
            }
        }
        // 返回结果
        renderText(jsonString);
    }

    /**
     * 修改组头像
     */
    @Before(CrossDomain.class)
    public void ModifyGroupPic() {
        String url = this.getPara("url");
        String groupID = this.getPara("groupID");
        // 判断URL是否已经拼接好
        String temp = url.substring(0, 7);
        if (!(temp.equals("http://"))) {
            url = CommonParam.qiniuOpenAddress + url;
        }
        //鉴黄 by lk
        String address = operate.getDownloadToken(url + "?nrop");
        // 发送请求并获取返回结果
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
        // 获取上传所在组的类型，若是官方相册（5），则不不能修改
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
        // 返回结果
        renderText(jsonString);
    }

    /**
     * 退出相册
     */
    public void LeaveAlbum() {
        String userid = this.getPara("userid");
        String groupid = this.getPara("groupid");
        if (userid == null || userid.equals("") || userid.equals("undefined") || userid.equals("NaN") || groupid == null || groupid.equals("") || groupid.equals("undefined") || groupid.equals("NaN")) {
            jsonString = jsonData.getJson(2, "参数错误", new ArrayList<Record>());
            renderText(jsonString);
            return;
        }
        String source = this.getPara("source");
        // 获取组性质，若退出的是私密相册，则使用原方法；若退出的是官方相册，则使用退出官方相册方法
        List<Record> list = dao.query("gtype", "groups", "groupid=" + groupid + "");
        String gtype = list.get(0).get("gtype").toString();
        boolean quitFlag = false;
        if (gtype.equals("5")) {
            // 退出官方相册，给管理员发送通知与推送
            quitFlag = TxYinianService.quitOfficialAlbum(userid, groupid);
        } else {
            // 退出相册，删除组成员数据，将该成员发送的动态等数据隐藏，成功返回true失败返回false
            quitFlag = TxYinianService.quitAlbum(userid, groupid, source);
        }
        jsonString = dataProcess.deleteFlagResult(quitFlag);
        // 返回结果
        renderText(jsonString);
    }


    /**
     * 显示小程序相册信息
     */
    @Before(CrossDomain.class)
    public void ShowSmallAppAlbumInformation() {
        String uid = this.getPara("userid");
        String gid = this.getPara("groupid");
        if (uid == null || gid == null || gid.equals("undefined") || gid.equals("NaN")) {
            jsonString = jsonData.getJson(2, "参数错误", new ArrayList<Record>());
            renderText(jsonString);
            return;
        }
        //黑名单
        User u = new User().findById(uid);
        int inBlackList = 1;
        if (null != u && null != u.get("ustate") && u.get("ustate").toString().equals("1")) {
            inBlackList = 0;
        }
        //黑名单
        // 基本参数
        int userid = this.getParaToInt("userid");
        int groupid = this.getParaToInt("groupid");
        // 用户来源参数
        String port = this.getPara("port");
        String fromUserID = this.getPara("fromUserID");

        Group group = new Group().findById(groupid);
        int eventQRCodeCanPublish = new GroupCanPublish().getGroupPublishByType(groupid + "", "1");
        //dialogShow=1 显示活动相册助手
        int dialogShow = new GroupCanPublish().getGroupPublishByType(groupid + "", "2");
        //showAdvertisements=1 显示广告位
        int advertisementsShow = new GroupCanPublish().getGroupPublishByType(groupid + "", "3");
        int status = Integer.parseInt(group.get("gstatus").toString());
        int gtype = Integer.parseInt(group.get("gtype").toString());
        int gAuthority = Integer.parseInt(group.get("gAuthority").toString());
        // 判断相册是否删除
        if (status == 0) {
            // 判断是否在相册中
            GroupMember gm = new GroupMember();
            boolean isInFlag = gm.judgeUserIsInTheAlbum(userid, groupid); // true时用户不在空间内

            boolean flag = true;
            boolean getNewGnum = false;
            int count = 1;
            Record record = new Record().set("joinStatus", 1);
            if (isInFlag) {
                // 不在相册，则插入用户数据
                gm = new GroupMember().set("gmgroupid", groupid).set("gmuserid", userid).set("gmPort", port)
                        .set("gmFromUserID", fromUserID);

                // 捕获插入异常，用户重复点击时会导致插入失败
                try {
                    flag = gm.save();
                    // 更新分组表中组成员数量字段
                    count = Db.update("update groups set gnum = gnum+1 where groupid='" + groupid + "' ");
                    record.set("joinStatus", 0);
                    getNewGnum = true;
                } catch (ActiveRecordException e) {
                    flag = true;
                    count = 1;

                }

            }

            if (flag && (count == 1)) {
                // 获取返回数据 ,gAuthority————0所有人 1只有创建者 2-部分
                record.set("gname", group.get("gname").toString()).set("gcreator", group.get("gcreator").toString())
                        .set("gtype", gtype).set("gnum", group.get("gnum").toString())
                        .set("gpic", group.get("gpic").toString()).set("gAuthority", gAuthority)
                        .set("gOrigin", group.get("gOrigin").toString())
                        .set("eventQRCodeCanPublish", eventQRCodeCanPublish)
                        .set("dialogShow", dialogShow).set("inBlackList", inBlackList).set("advertisementsShow", advertisementsShow);
                if (getNewGnum) {
                    record.set("gnum", group.getLong("gnum") + 1);
                }
                // 获取推送接收状态
                if (isInFlag) {
                    // 不在空间内的用户直接返回0
                    record.set("isPush", "0");
                } else {
                    // 在空间内的用户去查询
                    List<Record> push = Db.find("select gmIsPush from groupmembers where gmgroupid=" + groupid
                            + " and gmuserid=" + userid + "");
                    record.set("isPush", push.get(0).get("gmIsPush").toString());
                }

                // 获取成员列表，缓存
                List<Record> groupMember = CacheKit.get("ConcurrencyCache", groupid + "Member");
                if (groupMember == null) {
                    groupMember = Db.find(
                            "select userid,unickname,upic,gmtime from users,groups,groupmembers where groupid=gmgroupid and users.userid=groupmembers.gmuserid and gmgroupid='"
                                    + groupid + "' and gmstatus=0 order by gmid desc limit 10 ");
                    CacheKit.put("ConcurrencyCache", groupid + "Member", groupMember);
                }

                // 获取照片数，缓存
                List<Record> photo = CacheKit.get("ConcurrencyCache", groupid + "Photo");
                if (photo == null) {
                    photo = Db.find(
                            "select count(*) as gpicNum from groups,events,pictures where peid=eid and groupid=egroupid and groupid="
                                    + groupid + " and estatus in(0,3) and pstatus=0 ");
                    CacheKit.put("ConcurrencyCache", groupid + "Photo", photo);
                }

                // 获取发布权限列表，缓存，当空间发布权限为部分人时才查询并返回
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
            jsonString = jsonData.getJson(1012, "相册已被删除");
        } else {
            jsonString = jsonData.getJson(1037, "相册已被封");
        }
        renderText(jsonString);
    }


    /**
     * 显示时间轴 by lk test
     */
    @Before(CrossDomain.class)
    public void ShowTimeAxis() {
        String userid = this.getPara("userid");
        String groupid = this.getPara("groupid");
        String type = this.getPara("type");
        int eid = this.getParaToInt("eid");
        String source = this.getPara("source");

        // // 设置该空间对该用户无新动态
        // GroupMember gm = new GroupMember();
        // gm.UpdateSingleGroupMenberNoNewDynamic(groupid,
        // String.valueOf(userid));

        // 初始化时间轴缓存
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
     * 根据用户显示照片墙和短视频墙
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
        // 资源授权
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
     * 照片墙数据初始化（按日查看)
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
     * 显示成员列表
     */
    @Before(CrossDomain.class)
    public void ShowGroupMember() {
        // 获取参数
        String userid = this.getPara("userid");
        String groupid = this.getPara("groupid");
        if (userid == null || userid.equals("") || userid.equals("undefined") || userid.equals("NaN") || groupid == null || groupid.equals("") || groupid.equals("undefined") || groupid.equals("NaN")) {
            jsonString = jsonData.getJson(2, "参数错误", new ArrayList<Record>());
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
            jsonString = jsonData.getJson(2, "参数错误");
        }
        // 返回结果
        renderText(jsonString);
    }

    /**
     * by lk 读取活动菜单
     */
    public void GetActivityMenu() {
        String returnValue = jsonData.getJson(2, "请求参数错误");
        int groupid = Integer.parseInt(
                null != this.getPara("groupid") && !this.getPara("groupid").equals("") ? this.getPara("groupid") : "0");
        if (groupid == 0) {
            renderText(returnValue);
            return;
        }
        Group group = new Group().findById(groupid);

        // 获取照片数，缓存
        List<Record> photo = CacheKit.get("ConcurrencyCache", groupid + "Photo");
        if (photo == null) {
            photo = Db.find(
                    "select count(*) as gpicNum from groups,events,pictures where peid=eid and groupid=egroupid and groupid="
                            + groupid + " and estatus in(0,3) and pstatus=0 ");
            CacheKit.put("ConcurrencyCache", groupid + "Photo", photo);
        }
        String content = UrlUtils.loadJson(activityMenuUrl);
        // List<Record> likelist = new Event().GetListByGroup(groupid, 1);// 获取点赞第一名
        // List<Record> publishlist = new Event().GetUsePublishPhotoCont(groupid, 0,
        // false, 1);// 获取照片达人第一名

        // String returnValue=jsonData.getJson(2, "请求参数错误");
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
     * 点赞排行
     */

    public void GetGroupLikeList() {
        if (this.getPara("groupid") == null || this.getPara("groupid").equals("") || this.getPara("uid") == null || this.getPara("uid").equals("")) {
            jsonString = jsonData.getJson(2, "参数错误", new ArrayList<Record>());
            renderText(jsonString);
            return;
        }
        String gid = this.getPara("groupid");
        String userid = this.getPara("uid");
        if (userid.equals("") || gid.equals("") || gid == null || gid.equals("undefined") || gid.equals("NaN")) {
            jsonString = jsonData.getJson(2, "参数错误", new ArrayList<Record>());
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
     * 照片达人
     */
    public void GetPublishList() {
        if (this.getPara("groupid") == null || this.getPara("groupid").equals("") || this.getPara("uid") == null || this.getPara("uid").equals("")) {
            jsonString = jsonData.getJson(2, "参数错误", new ArrayList<Record>());
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
        // 缩略图 QiniuOperate a = new QiniuOperate();
        // String url = a.getDownloadToken(url+"?imageView2/2/w/200");
    }

    /**
     * 修改动态等级,置顶功能
     */
    //@ActionKey("/yinian/ModifyEventLevel")
    public void ModifyEventLevel() {
        String userid = this.getPara("userid");
        int groupid = Integer.parseInt(this.getPara("groupid"));
        String eid = this.getPara("eid");
        String type = this.getPara("type");

        // 权限判定
        Group group = new Group().findById(groupid);
        String gcreator = group.get("gcreator").toString();
        if (userid.equals(gcreator)) {
            Event event = new Event().findById(eid);

            // 判断是在原空间还是推荐款空间置顶
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
            jsonString = jsonData.getJson(6, "没有权限执行操作");
        }
        renderText(jsonString);

    }

    /**
     * 上传动态
     */
    @Before(CrossDomain.class)
    public void UploadEvent() {
        // 谁发到哪个空间内
        String userid = this.getPara("userid") == null ? "" : this.getPara("userid");
        String groupid = this.getPara("groupid") == null ? "" : this.getPara("groupid");
        // 图片
        String picAddress = this.getPara("picAddress") == null ? "" : this.getPara("picAddress");
        // 文字
        String content = this.getPara("content") == null ? "" : this.getPara("content");
        // 语音
        String audio = this.getPara("audio") == null ? "" : this.getPara("audio");
        // 地点
        String place = this.getPara("place") == null ? "" : this.getPara("place");
        String placePic = this.getPara("placePic") == null ? "" : this.getPara("placePic");// 位置生成的图片地址
        String placeLongitude = this.getPara("placeLongitude") == null ? "" : this.getPara("placeLongitude");// 经度
        String placeLatitude = this.getPara("placeLatitude") == null ? "" : this.getPara("placeLatitude");// 纬度
        // 和谁
        String peopleName = this.getPara("peopleName") == null ? "" : this.getPara("peopleName");
        // 动态以哪个要素为主
        String main = this.getPara("main") == null ? "" : this.getPara("main"); // 0--照片 1--文字 2--语音 3--地点
        // 其他元素
        String storage = this.getPara("storage") == null ? "" : this.getPara("storage");// 存储空间
        String source = this.getPara("source") == null ? "" : this.getPara("source");// 判断接口来源
        String isPush = this.getPara("isPush") == null ? "" : this.getPara("isPush"); // 推送判断 app:yes/no 小程序:true/false
        String formID = this.getPara("formID") == null ? "" : this.getPara("formID"); // 小程序推送表单ID


        // 插入formID
        //FormID.insertFormID(userid, formID);
        if (!userid.equals("") && !formID.equals("")) {
            FormID.insert(userid, formID);
        }
        // 判断存储空间是否有传
        double storagePlace;
        if (storage == null || storage.equals("")) {
            storagePlace = 0.00;
        } else {
            storagePlace = Double.parseDouble(storage);
        }

        // 接口来源为web，需要解密
        if (source != null && source.equals("web")) {
            groupid = dataProcess.decryptData(groupid, "groupid");
        }

        // 地址字段数据处理
        String firstPic = null;
        String[] picArray = new String[0];
        if (picAddress != null && !picAddress.equals("")) {
            picArray = dataProcess.getPicAddress(picAddress, "private");
            // 图片鉴黄
            picArray = dataProcess.PictureVerify(picArray);
            // 获取动态第一张图片地址,可能没有上传图片
            firstPic = (picArray.length == 0 ? null : picArray[0]);
        }

        // 图片都被过滤掉，不插入数据
        int eid = 0;
        if (main.equals("0") && picArray.length == 0) {
            List<Record> errorList = new ArrayList<Record>();
            Record r = new Record();
            r.set("picList", new ArrayList<String>());
            errorList.add(r);
            jsonString = jsonData.getSuccessJson(errorList);
        } else {
            // 支持同时上传到多个空间
            String[] IDs = groupid.split(",");
            boolean flag1 = false;
            // 逐个空间上传
            for (int i = 0; i < IDs.length; i++) {
                // 同步标记,0--非同步 1--同步 ,第一个空间为原创，其他为同步
                int isSynchronize = (i == 0 ? 0 : 1);
                eid = TxEventService.upload(userid, IDs[i], picArray, content, audio, place, placePic, placeLongitude,
                        placeLatitude, peopleName, main, storagePlace, firstPic, isPush, source, isSynchronize, formID);
                if (eid != 0) {
                    // 说明上传成功
                    List<Record> result = eventService.getSingleEvent(eid, source);// 获取动态的信息
                    flag1 = true;
                    jsonString = jsonData.getSuccessJson(result);
                } else {
                    flag1 = false;
                    jsonString = jsonData.getJson(-50, "数据插入到数据库失败");
                    break;
                }
            }
        }
        // 返回结果
        renderText(jsonString);
    }

    /**
     * 上传短视频
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

        // 判断存储空间是否有传
        double storagePlace = ((storage == null || storage.equals("")) ? 0.00 : Double.parseDouble(storage));
        cover = (cover == null ? "" : cover);
        time = (time == null ? "0" : time);

        // 资源地址加前缀
        address = CommonParam.qiniuPrivateAddress + address;
        // 视频鉴黄,视频封面图片鉴黄true为色情视频
        boolean videoJudge = dataProcess.VideoVerify(address);
        boolean coverJudge = false;
        if (!cover.equals(""))
            coverJudge = dataProcess.SinglePictureVerify(cover);

        if (videoJudge || coverJudge) {
            jsonString = jsonData.getJson(1039, "资源违规");
        } else {
            // 支持同时上传到多个空间
            String[] IDs = groupid.split(",");
            boolean flag = true;
            int eventID = 0;
            // 逐个空间上传
            for (int i = 0; i < IDs.length; i++) {
                // 同步标记,0--非同步 1--同步 ,第一个空间为原创，其他为同步
                int isSynchronize = (i == 0 ? 0 : 1);
                // 上传短视频
                int eid = TxEventService.uploadShortVedio(userid, IDs[i], address, content, storagePlace, place, cover, time,
                        isSynchronize, source);
                eventID = eid;
                if (eid == 0) {
                    flag = false;
                    break;
                }
            }
            if (flag) {
                // 说明上传成功
                List<Record> result = eventService.getSingleEvent(eventID, source);// 获取动态的信息
                jsonString = jsonData.getSuccessJson(result);
            } else {
                jsonString = jsonData.getJson(-50, "数据插入到数据库失败");
            }
        }

        // 返回结果
        renderText(jsonString);
    }

    /**
     * 显示我的，第二版
     */
    public void ShowMe2ndVersion() {
        String userid = this.getPara("userid");
        String minID = this.getPara("minID");

        if (minID == null) {
            // 刷到底没有数据了，直接返回
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
     * 显示时刻 by lk 简化版本
     */
    public void ShowMoments_sim() {
        System.out.println("ShowMoments开始：" + System.currentTimeMillis());
        String userid = this.getPara("userid");
        String type = this.getPara("type");
        String eid = this.getPara("eid");
        if (userid == null || userid.equals("") || userid.equals("undefined") || userid.equals("NaN") || eid == null || eid.equals("") || eid.equals("undefined") || eid.equals("NaN")) {
            jsonString = jsonData.getJson(2, "参数错误", new ArrayList<Record>());
            renderText(jsonString);
            return;
        }
        List<Record> result = eventService.GetMoments_sim(userid, type, eid);
        jsonString = jsonData.getSuccessJson(result);
        renderText(jsonString);
        System.out.println("ShowMoments结束：" + System.currentTimeMillis());
    }

    /**
     * 显示空间成员的动态
     */
    public void ShowSpaceMemberEvents() {
        // 获取参数
        int userid = this.getParaToInt("userid");
        //int groupid = this.getParaToInt("groupid");
        String minID = this.getPara("minID");
        String source = this.getPara("source");

        if (minID == null || null == this.getPara("groupid") || this.getPara("groupid").equals("")) {
            // 刷到底没有数据了，直接返回
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
     * 获取空间成员照片数
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
     * 获取用户数据
     */
    @Before(CrossDomain.class)
    public void GetUserData() {
        String userid = this.getPara("userid");
        String source = this.getPara("source");

        // userid解密
        if (source != null && source.equals("h5")) {
            userid = dataProcess.decryptData(userid, "userid");
        }
        //黑名单
        User u = new User().findById(userid);
        int inBlackList = 1;
        if (null != u && null != u.get("ustate") && u.get("ustate").toString().equals("1")) {
            inBlackList = 0;
        }
        //黑名单
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
            // 获取用户存储空间信息
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
     * 共享相册点赞与取消点赞 lk 修改返回对象
     */
    public void AttachOrRemoveExpressionByLkNew() {
        String userid = this.getPara("userid") == null ? "" : this.getPara("userid");
        String eid = this.getPara("eid");
        String type = this.getPara("type");
        String source = this.getPara("source");
        String formID = this.getPara("formID") == null ? "" : this.getPara("formID"); // 小程序推送表单ID
        // 插入formID
        //FormID.insertFormID(userid, formID);
        if (!userid.equals("") && !formID.equals("")) {
            FormID.insert(userid, formID);
        }
        int status = 0;
        if (source != null && source.equals("app")) {
            // app直接传值
            status = Integer.parseInt(type);
        } else {
            // 小程序传英文,将type改成对应status
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
        // 判断用户是否有相关操作
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
            // 没有操作过
            like = new Like().set("likeEventID", eid).set("likeUserID", userid).set("likeStatus", status);
            if (like.save()) {
                result = Db.find(sql);
                result = dataProcess.changeLikeStatusToWord(result);
                List<Record> returnList = new ArrayList<Record>();
                Record r = new Record();
                r.set("likeCnt", 0);
                //点赞数读取redis缓存，若没有缓存则读取数据库，同时更新缓存
                Jedis jedis = RedisUtils.getRedis();
                if (null != jedis) {
                    //从缓存中读取当前eid的点赞数
                    String likeCnt = jedis.get("likeCnt_" + eid);
                    if (null != likeCnt && !"".equals(likeCnt)) {
                        //点赞成功后缓存点赞数加1
                        int likeCntInt = Integer.valueOf(likeCnt) + 1;
                        String jr = jedis.set("likeCnt_" + eid, String.valueOf(likeCntInt));
                        if (null != jr && "OK".equals(jr)) {
                            r.set("likeCnt", likeCntInt);
                        }
                    } else {
                        //当前eid未缓存点赞数，从数据库count点赞数，并同步到缓存
                        List<Record> cntList = Db.find(likeCntSql);
                        if (!cntList.isEmpty()) {
                            r.set("likeCnt", cntList.get(0).get("cnt"));
                            jedis.set("likeCnt_" + eid, cntList.get(0).get("cnt").toString());
                        }
                    }
                    //释放redis
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
            // 有操作过
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
                //点赞数读取redis缓存，若没有缓存则读取数据库，同时更新缓存
                Jedis jedis = RedisUtils.getRedis();
                if (null != jedis) {
                    //从缓存中读取当前eid的点赞数
                    String likeCnt = jedis.get("likeCnt_" + eid);
                    if (null != likeCnt && !"".equals(likeCnt)) {
                        if (status == 1) {
                            //取消点赞时，缓存点赞数减1
                            int likeCntInt = Integer.valueOf(likeCnt) > 0 ? Integer.valueOf(likeCnt) - 1 : 0;
                            String jr = jedis.set("likeCnt_" + eid, String.valueOf(likeCntInt));
                            if (null != jr && "OK".equals(jr)) {
                                r.set("likeCnt", likeCntInt);
                            }
                        } else {
                            //非取消点赞，缓存点赞数不变
                            r.set("likeCnt", Integer.valueOf(likeCnt));
                        }
                    } else {
                        //当前eid未缓存点赞数，从数据库count点赞数，并同步到缓存
                        List<Record> cntList = Db.find(likeCntSql);
                        if (!cntList.isEmpty()) {
                            r.set("likeCnt", cntList.get(0).get("cnt"));
                            jedis.set("likeCnt_" + eid, cntList.get(0).get("cnt").toString());
                        }
                    }
                    //释放redis
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
        // 判断用户是否有相关操作
    }

    /**
     * 搜索空间成员 by lk 修改返回数据内容
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
     * 显示动态点赞列表
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
     * 获取组内所有照片,照片墙
     */
    public void GetAllPhotosInOneGroup() {
        // 获取参数
        String groupid = this.getPara("groupid");
        String type = this.getPara("type");
        int id = Integer.parseInt(this.getPara("id"));
        List<Record> list = yinianService.getGroupPhotos(groupid, type, id);
        jsonString = jsonData.getJson(0, "success", list);
        // 返回结果
        renderText(jsonString);
    }

    /**
     * 设置发布权限
     */
    @Before({Tx.class, CrossDomain.class})
    public void SetUploadAuthority() {
        String groupid = this.getPara("groupid");
        String userid = this.getPara("userid");
        String authorityType = this.getPara("authorityType");// 三个值:all,onlyCreator,part
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
                int count = 0;// 更新数据库计数
                if (type.equals("add")) {
                    // 添加权限，1表示有权限
                    count = Db.update("update groupmembers set gmauthority=1 where gmuserid in (" + userid
                            + ") and gmgroupid=" + groupid + "  ");
                } else {
                    // 取消权限，0表示无权限，默认为0
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
     * 搜索空间成员
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
     * 查看空间成员权限列表
     */
    public void GetSpaceMemberAuthorityList() {
        String groupid = this.getPara("groupid");
        String type = this.getPara("type");// 两个值，大于100人与小于100人时返回不同的数据

        String sql = "";
        switch (type) {
            case "bigger":
                // 只显示授权者信息
                sql = "select userid,unickname,upic,gmauthority from users,groupmembers where userid=gmuserid and gmgroupid="
                        + groupid + " and gmauthority=1 ";
                break;
            case "smaller":
                // 显示空间成员所有授权信息
                sql = "select userid,unickname,upic,gmauthority from users,groupmembers where userid=gmuserid and gmgroupid="
                        + groupid + " ";
                break;
        }
        List<Record> list = Db.find(sql);
        jsonString = jsonData.getSuccessJson(list);
        renderText(jsonString);
    }

    /**
     * 根据图片ID删除图片，若用户删除了一个动态内所有的图片则删除动态
     */
    public void deletePic() {
        jsonString = jsonData.getJson(2, "请求参数错误");
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
     * 删除评论
     */
    public void DeleteComment() {
        String commentID = this.getPara("commentID");
        boolean flag = yinianService.deleteComment(commentID);
        jsonString = dataProcess.updateFlagResult(flag);
        // 返回结果
        renderText(jsonString);
    }

    /**
     * 发表评论1 1.1版本 新增返回字段 cid
     */
    public void SendComment1() {
        String commentUserId = this.getPara("commentUserId") == null ? "" : this.getPara("commentUserId");// 评论人ID
        String commentedUserId = this.getPara("commentedUserId");// 被评论人ID
        String eventId = this.getPara("eventId");// 事件ID
        String content = this.getPara("content");// 评论内容
        String place = this.getPara("place");
        String formID = this.getPara("formID") == null ? "" : this.getPara("formID"); // 小程序推送表单ID

        if (!commentUserId.equals("") && !formID.equals("")) {
            FormID.insert(commentUserId, formID);
        }
        String cid = TxYinianService.sendComment1(commentUserId, commentedUserId, eventId, content, place);
        if (cid.equals("")) {
            jsonString = jsonData.getJson(-50, "插入数据失败");
        } else {
            List<Record> list = dataProcess.makeSingleParamToList("cid", cid);
            jsonString = jsonData.getJson(0, "success", list);
        }
        // 返回结果
        renderText(jsonString);
    }

    /**
     * 获取同步空间列表——排除无上传权限空间
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
            jsonString = jsonData.getJson(2, "请求参数错误");
        }
        renderText(jsonString);
    }

    /**
     * 踢出相册
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
                // 将成员踢出相册，删除组成员数据，将该成员发送的动态等数据隐藏，成功返回true失败返回false
                if (owner.equals(IDs[i])) {
                    // 判断踢出的人是不是自己
                    jsonString = jsonData.getJson(1035, "不能踢出自己");
                } else {
                    quitFlag = TxYinianService.kickOutAlbum(IDs[i], groupid);
                    if (!quitFlag) {
                        break;
                    }
                }

            }
            jsonString = dataProcess.deleteFlagResult(quitFlag);
        } else {
            jsonString = jsonData.getJson(1034, "无权限踢人");
        }

        // 返回结果
        renderText(jsonString);

    }


}
