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

    private String jsonString; // 返回的json字符串
    private JsonData jsonData = new JsonData(); // json操作类
    private YinianDataProcess dataProcess = new YinianDataProcess();// 数据处理类
    private QiniuOperate operate = new QiniuOperate();
    // enhance方法对目标进行AOP增强
    YinianService TxService = enhance(YinianService.class);
    YinianService ynService = new YinianService();
    private static final Logger log = Logger.getLogger(ActivityController.class);
    private ActivityService service = new ActivityService();
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    /******************************* 活动统一相关接口 Start *******************************/

    /**
     * 获取活动消息
     */
    public void GetActivityBanner() {
        List<Record> list = Db.find("select * from banner where bstatus=3 limit 1  ");
        jsonString = jsonData.getSuccessJson(list);
        renderText(jsonString);
    }

    /**
     * 获取拼图权限
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

    /******************************* 活动统一相关接口 End *******************************/

    /*******************************
     * <我的2016年回忆>活动接口 Start
     *******************************/

    /**
     * 显示回忆界面
     */
    public void ShowMyMemory() {

        // 获取参数
        String groupid = this.getPara("groupid");
        String userid = this.getPara("userid");// 点进查看的用户的userid

        // 获取统计数据字段
        Set<Integer> userSet = new HashSet<Integer>();
        int count = 0;

        // groupid解密获取明文
        groupid = dataProcess.decryptData(groupid, "groupid");
        // 判断相册是否存在
        List<Record> exist = Db.find("select * from groups where groupid=" + groupid + " and gstatus=0 ");
        if (exist.size() == 0) {
            jsonString = jsonData.getJson(1012, "相册已被删除");
        } else {
            // 获取图片数据
            List<Record> queryList = Db.find(
                    "select eid,userid,unickname,upic,etext,isAnonymous,GROUP_CONCAT(poriginal SEPARATOR \",\" ) as url from users,events,pictures where userid=euserid and eid=peid and egroupid="
                            + groupid + " and estatus=0 and pstatus=0 group by peid DESC  ");
            // 处理数据
            List<Record> dataList = new ArrayList<Record>();
            for (Record record : queryList) {
                // 添加朋友信息
                userSet.add(Integer.parseInt(record.get("userid").toString()));

                // 判断是否匿名，匿名则修改用户昵称和头像
                int isAnonymous = Integer.parseInt(record.get("isAnonymous").toString());
                if (isAnonymous == 1) {
                    record.set("unickname", "匿名").set("upic",
                            CommonParam.qiniuOpenAddress + CommonParam.anonymousUserPicture);
                }

                // 多张图片则拆分成多条数据
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

            // 返回数据集
            Record resultRecord = new Record();
            // 获取回忆总数
            count = dataList.size();
            // 获取好友总数
            int userCount = userSet.size();

            // 判断用户是否在相册内
            List<Record> in = Db
                    .find("select * from groupmembers where gmuserid=" + userid + " and gmgroupid=" + groupid + " ");
            if (in.size() == 0) {
                resultRecord.set("isInAlbum", "no");
            } else {
                resultRecord.set("isInAlbum", "yes");
            }

            // 判断用户是否已有回忆相册
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
            // 获取相册创建者昵称
            List<Record> creator = Db
                    .find("select unickname,upic from users,groups where userid=gcreator and groupid=" + groupid + " ");

            // 插入并返回数据
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
     * 上传回忆照片
     */
    @Before(Tx.class)
    public void UploadMemoryPhoto() {
        String userid = this.getPara("userid");
        String groupid = this.getPara("groupid");
        String url = this.getPara("url");
        String content = this.getPara("content");
        String isAnonymous = this.getPara("isAnonymous");

        // groupid解密获取明文
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
     * 创建回忆相册
     */
    public void CreateMemoryAlbum() {
        String userid = this.getPara("userid");

        // 判断用户是否已创建过回忆相册
        List<Record> judge = Db.find("select * from groups where gtype=9 and gstatus=0 and gcreator=" + userid + " ");
        if (judge.size() == 0) {
            User user = new User().findById(userid);
            String nickname = user.get("unickname").toString();
            String inviteCode = Group.CreateSpaceInviteCode();
            String groupCover = CommonParam.qiniuOpenAddress + CommonParam.otherGroup;

            jsonString = TxService.createAlbum(nickname + "的2016回忆相册", userid, groupCover, "9", inviteCode, "web");

        } else {
            jsonString = jsonData.getJson(2027, "用户已创建回忆相册");
        }
        renderText(jsonString);

    }

    /*******************************
     * <我的2016年回忆>活动接口 End
     *******************************/

    /*******************************
     * <照片拼图>活动接口 Start
     *******************************/

    /**
     * 制作拼图
     *
     * @throws Exception
     */
    public void MakePuzzle() throws Exception {
        String userid = this.getPara("userid");
        String picture = this.getPara("picture");
        String content = this.getPara("content");

        picture = picture.substring(0, 7).equals("http://") ? picture : CommonParam.qiniuOpenAddress + picture;
        content = (content == null || content.equals("")) ? "你的好友没有留下任何提示哦！" : content;

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
     * 修改拼图信息
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
            content = (content == null || content.equals("")) ? "你的好友没有留下任何提示哦！" : content;
            puzzle.set("puzzleContent", content);
            jsonString = dataProcess.updateFlagResult(puzzle.update());
        } else {
            jsonString = jsonData.getJson(6, "没有权限执行操作");
        }
        renderText(jsonString);
    }

    /**
     * 查看拼图页面
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

        // 获取页面信息
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

        // 判断用户是否玩过拼图
        List<Record> judge = Db.find("select * from `join` where joinUserID=" + userid + " and joinActivityID="
                + puzzleID + " and joinType=0 ");
        if (judge.size() == 0) {
            puzzleInfo.get(0).set("isJoin", "no");
        } else {
            puzzleInfo.get(0).set("isJoin", "yes");
        }

        // 合成拼图二维码
        if (url != null && !url.equals("")) {

        }

        jsonString = jsonData.getSuccessJson(puzzleInfo);
        renderText(jsonString);
    }

    /**
     * 完成拼图游戏
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

        // 判断用户是否玩过拼图
        List<Record> judge = Db.find("select * from `join` where joinUserID=" + userid + " and joinActivityID="
                + puzzleID + " and joinType=0 ");
        if (judge.size() == 0) {
            Join join = new Join().set("joinUserID", userid).set("joinScore", score).set("joinActivityID", puzzleID)
                    .set("joinType", 0);
            if (join.save()) {
                // 返回该拼图信息
                List<Record> joinerInfo = Db.find(
                        "select userid,unickname,upic,joinID,joinScore,joinTime from `join`,users where joinUserID=userid and joinActivityID="
                                + puzzleID + " and joinType=0 ORDER BY joinScore asc  ");
                jsonString = jsonData.getSuccessJson(joinerInfo);
            } else {
                jsonString = dataProcess.insertFlagResult(false);
            }
        } else {
            jsonString = jsonData.getJson(2028, "已参加过该游戏");
        }
        renderText(jsonString);

    }

    /**
     * 试玩拼图
     */
    public void TryPlayPuzzle() {
        String userid = this.getPara("userid");
        Double score = Double.parseDouble(this.getPara("score"));
        String puzzleID = this.getPara("puzzleID");

        Puzzle puzzle = new Puzzle().findById(puzzleID);
        int puzzleType = Integer.parseInt(puzzle.get("puzzleType").toString());
        if (puzzleType == 1) {
            // 判断用户是否玩过拼图
            List<Record> judge = Db.find("select * from `join` where joinUserID=" + userid + " and joinActivityID="
                    + puzzleID + " and joinType=0 ");
            if (judge.size() == 0) {
                Join join = new Join().set("joinUserID", userid).set("joinScore", score).set("joinActivityID", puzzleID)
                        .set("joinType", 0);
                if (join.save()) {
                    // 返回该拼图信息
                    List<Record> joinerInfo = Db.find(
                            "select userid,unickname,upic,joinID,joinScore,joinTime from `join`,users where joinUserID=userid and joinActivityID="
                                    + puzzleID + " and joinType=0 ORDER BY joinScore asc limit 15  ");
                    jsonString = jsonData.getSuccessJson(joinerInfo);
                } else {
                    jsonString = dataProcess.insertFlagResult(false);
                }
            } else {
                // 存储用户最佳成绩
                String joinID = judge.get(0).get("joinID").toString();
                double joinScore = Double.parseDouble(judge.get(0).get("joinScore").toString());
                if (score < joinScore) {
                    Join join = new Join().findById(joinID);
                    join.set("joinScore", score);
                    if (join.update()) {
                        // 返回该拼图信息
                        List<Record> joinerInfo = Db.find(
                                "select userid,unickname,upic,joinID,joinScore,joinTime from `join`,users where joinUserID=userid and joinActivityID="
                                        + puzzleID + " and joinType=0 ORDER BY joinScore asc limit 15  ");
                        jsonString = jsonData.getSuccessJson(joinerInfo);
                    } else {
                        jsonString = dataProcess.updateFlagResult(false);
                    }
                } else {
                    // 返回该拼图信息
                    List<Record> joinerInfo = Db.find(
                            "select userid,unickname,upic,joinID,joinScore,joinTime from `join`,users where joinUserID=userid and joinActivityID="
                                    + puzzleID + " and joinType=0 ORDER BY joinScore asc limit 15  ");
                    jsonString = jsonData.getSuccessJson(joinerInfo);
                }
            }
        } else {
            jsonString = jsonData.getJson(2, "请求参数错误");
        }
        renderText(jsonString);
    }

    /**
     * 获取游戏总人数
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

    /******************************* <照片拼图>活动接口 End *******************************/

    /*******************************
     * <阅后即焚>活动接口 Start
     *******************************/

    /**
     * 获取临时照片内容
     */
    public void GetTempPhotoContent() {
        String userid = this.getPara("userid");
        String tempID = this.getPara("tempID");

        List<Record> judge = Db
                .find(" select * from tempView where tempViewUserID=" + userid + " and tempViewTempID=" + tempID + " ");

        if (judge.size() == 0) {
            Temp temp = new Temp().findById(tempID);
            String tempPid = temp.get("tempPid").toString();
            // 获取图片信息
            List<Record> picList = Db
                    .find("select pid,poriginal,DATE(puploadtime) as puploadtime  from pictures where pid in(" + tempPid
                            + ") ORDER BY DATE(puploadtime) desc ");

            // 存储所有日期的图片
            List<Record> resultPicList = new ArrayList<Record>();
            // 存储单个日期的图片
            Record tempRecord = new Record();
            // 单个日期的图片集合
            Set<String> picSet = new HashSet<String>();

            // 图片授权，按日期分类
            for (int i = 0; i < picList.size(); i++) {
                String url = operate
                        .getDownloadToken((picList.get(i).get("poriginal").toString()) + "?imageView2/2/w/400");
                String time = picList.get(i).get("puploadtime").toString();

                if (tempRecord.get("date") == null) {
                    // 说明是新的日期，可直接插入
                    tempRecord.set("date", time);
                    picSet.add(url);
                } else {
                    if ((tempRecord.get("date").toString()).equals(time)) {
                        // 当前日期存在
                        picSet.add(url);
                    } else {
                        // 当前日期是新日期
                        tempRecord.set("picture", picSet.toArray());
                        resultPicList.add(tempRecord);
                        // 创建新的record和set
                        tempRecord = new Record().set("date", time);
                        picSet = new HashSet<String>();
                        picSet.add(url);
                    }
                }

                // 如果i是最后一个，则将record放入list中
                if ((i + 1) == picList.size()) {
                    tempRecord.set("picture", picSet.toArray());
                    resultPicList.add(tempRecord);
                }
            }
            Record resultRecord = new Record();

            // 获取允许查看时间
            int limitTime = Integer.parseInt(temp.get("tempLimit").toString());
            String tempURL = temp.get("tempURL");
            resultRecord.set("pic", resultPicList).set("limitTime", limitTime).set("picNum", picList.size())
                    .set("tempURL", tempURL);
            // 获取返回结果
            List<Record> result = new ArrayList<Record>();
            result.add(resultRecord);
            jsonString = jsonData.getSuccessJson(result);
        } else {
            jsonString = jsonData.getJson(2029, "已查看过阅后即焚");
        }
        renderText(jsonString);
    }

    /**
     * 完成临时照片的查看
     */
    public void FinishWatchTempPhoto() {
        String userid = this.getPara("userid");
        String tempID = this.getPara("tempID");
        TempView tv = new TempView().set("tempViewUserID", userid).set("tempViewTempID", tempID);
        jsonString = dataProcess.insertFlagResult(tv.save());
        renderText(jsonString);
    }

    /**
     * 制作临时照片
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
                // 处理图片
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
                jsonString = jsonData.getJson(1, "请求参数缺失");
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

    /******************************* <阅后即焚>活动接口 End *******************************/

    /******************************* <行业真相>活动接口 Start *****************************/

    /**
     * 获取所有行业
     */
    public void GetAllIndustry() {
        List<Record> result = IndustryPhoto.getAllIndustryType();
        jsonString = jsonData.getSuccessJson(result);
        renderText(jsonString);
    }

    /**
     * 获取行业真相
     */
    public void GetRealityOfTheIndustry() {
        // 获取参数
        String userid = this.getPara("userid");
        String industry = this.getPara("industry");
        String version = this.getPara("version");

        // 获取封底图片
        List<Record> photoList = Db
                .find("select address from industryPhoto where industry='" + industry + "' and status=0 ");
        int size = photoList.size();
        int num = new Random().nextInt(size);
        String address = photoList.get(num).get("address").toString();

        // 获取用户名
        User user = new User().findById(userid);
        String nickname = user.getStr("unickname");

        // 合成图片
        ComposePicture com = new ComposePicture();
        String afterURL = "";
        String beforeURL = "";
        // if (version != null && version.equals("2")) {
        // 新版本只有一张图片
        afterURL = com.ComposeIndustryPicture2ndVersion(address, nickname, userid);
        beforeURL = afterURL;
        // } else {
        // // 旧版本有两张图片
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

        // 将图片上传到七牛云
        operate.uploadFileToOpenSpace(CommonParam.tempPictureServerSavePath + afterURL, afterURL);

        // 删除本地文件
        File afterFile = new File(CommonParam.tempPictureServerSavePath + afterURL);
        afterFile.delete();

        // 返回结果
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

    /******************************* <行业真相>活动接口 End *******************************/

    /******************************* <活动相册>活动接口 Start *****************************/

    /**
     * 显示活动空间
     */
    public void ShowActivitySpace() {
        // 获取参数
        int userid = this.getParaToInt("userid");

        List<Record> result = service.getActivitySpace(userid);

        // 将用户加入到空间内
        GroupMember gm = new GroupMember();
        for (Record record : result) {
            int groupid = Integer.parseInt(record.get("groupid").toString());
            // 判断用户是否在空间内
            if (gm.judgeUserIsInTheAlbum(userid, groupid)) {
                // 不在空间内，将用户加入空间
                gm.AddGroupMember(userid, groupid);
            }
        }

        jsonString = jsonData.getSuccessJson(result);
        // 返回结果
        renderText(jsonString);
    }

    /**
     * 进入空间，无消息提醒
     */
    public void EnterSpaceWithoutNotify() {
        // 获取参数
        int userid = this.getParaToInt("userid");
        int groupid = this.getParaToInt("groupid");

        GroupMember gm = new GroupMember();
        // 判断用户是否在空间内
        if (gm.judgeUserIsInTheAlbum(userid, groupid)) {
            // 不在空间内，将用户加入空间
            jsonString = dataProcess.insertFlagResult(gm.AddGroupMember(userid, groupid));
        } else {
            jsonString = jsonData.getJson(1013, "用户已在组内");
        }
        renderText(jsonString);

    }

    /**
     * 搜索空间内用户动态
     */
    public void SearchSpaceUserEvent() {
        // 获取参数
        String groupid = this.getPara("groupid");
        String nickname = this.getPara("nickname");

        List<Record> result = service.getSearchEvents(groupid, nickname);
        jsonString = jsonData.getSuccessJson(result);
        renderText(jsonString);

    }

    /**
     * 显示排行榜，3分钟更新一次
     */
    public void ShowChart() {
        // 获取参数
        int userid = this.getParaToInt("userid");
        int groupid = this.getParaToInt("groupid");

        // 获取总排行
        List<Record> chart = CacheKit.get("ServiceCache", "chart");
        // 缓存为空，重新查询
        if (chart == null) {
            chart = service.getSpaceEventChart(groupid);
            if (chart != null) {
                CacheKit.put("ServiceCache", "chart", chart);
            }
        }

        // 获取个人排行
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
            // 没有进入排名
            List<Record> tempList = Db.find(
                    "SELECT userid,unickname,upic,count(*) AS num FROM users,`events`,`like` WHERE userid = euserid AND eid = likeEventID AND estatus = 0 AND likeStatus != 1 AND egroupid = "
                            + groupid + " and euserid = " + userid + " GROUP BY eid ORDER BY num DESC limit 1");
            if (tempList.size() == 0) {
                Record tempRecord = Db
                        .findFirst("SELECT userid,unickname,upic from users where userid=" + userid + " ");
                tempRecord.set("num", 0).set("rank", "未进入");
                myChart.add(tempRecord);
            } else {
                myChart.add(tempList.get(0).set("rank", "未进入"));
            }
        }

        // 返回结果
        Record resultRecord = new Record().set("totalChart", chart).set("personalChart", myChart);
        List<Record> result = new ArrayList<Record>();
        result.add(resultRecord);
        jsonString = jsonData.getSuccessJson(result);
        renderText(jsonString);

    }

    /**
     * 显示奖励广播
     */
    public void ShowRewardBroadcast() {
        String groupid = this.getPara("groupid");
        List<Record> list = Reward.GetRecentThirtyRewardInfo(groupid);
        jsonString = jsonData.getSuccessJson(list);
        renderText(jsonString);
    }

    /**
     * 满足奖励条件
     */
    public void AddRewardInfo() {
        int userid = this.getParaToInt("userid");
        int type = this.getParaToInt("type");
        String groupid = this.getPara("groupid");

        Reward reward = new Reward().set("RewardUserID", userid).set("RewardType", type);
        // 临时加上默认
        if (groupid == null) {
            reward.set("RewardGroupID", 1065266);
        } else {
            reward.set("RewardGroupID", groupid);
        }

        boolean flag = false;

        switch (type) {
            case 8:
                reward.set("RewardContent", "爱奇艺会员");
                flag = reward.save();
                break;
            case 28:
                reward.set("RewardContent", "李易峰写真周边明信片手机支架大礼包");
                flag = reward.save();
                break;
            case 48:
                reward.set("RewardContent", "心理罪门票一张");
                flag = reward.save();
                break;
            case 68:
                reward.set("RewardContent", "李易峰《1987了》 随笔集写真集书");
                flag = reward.save();
                break;
            default:
                break;
        }
        jsonString = dataProcess.insertFlagResult(flag);
        renderText(jsonString);
    }

    /******************************* <活动相册>活动接口 End *******************************/

    /*******************************
     * <小程序签到兑奖>活动接口 Start
     *******************************/

    /**
     * 每日签到
     *
     * @throws ParseException
     */
    public void DailySignIn() throws ParseException {
        String userid = this.getPara("userid");

        // 签到
        boolean signInFlag = service.signIn(userid, "0");

        // 增加用户空间100M=102400KB
        boolean increaseFlag = false;
        if (signInFlag) {
            User user = new User();
            increaseFlag = user.IncreaseUserTotalStorageSpace(userid, 102400.00);
        }
        if (signInFlag && increaseFlag) {
            jsonString = jsonData.getSuccessJson();
        } else {
            jsonString = jsonData.getJson(2030, "当天已签到");
        }

        renderText(jsonString);

    }


    /**
     * 每日签到
     */
    @Before(CrossDomain.class)
    public void DailySignIn2() throws ParseException {
        String userid = this.getPara("userid");

        // 签到
        boolean signInFlag = service.signIn2(userid, "0");

        // 增加用户空间100M=102400KB
        boolean increaseFlag = false;
        if (signInFlag) {
            User user = new User();
            increaseFlag = user.IncreaseUserTotalStorageSpace(userid, 102400.00);
        }
        if (signInFlag && increaseFlag) {
            jsonString = jsonData.getSuccessJson();
        } else {
            jsonString = jsonData.getJson(2030, "当天已签到");
        }

        renderText(jsonString);

    }

    /**
     * 显示福利页面
     *
     * @throws ParseException
     */
    public void ShowWelfarePage() throws ParseException {
        String userid = this.getPara("userid");

        Record resultRecord = new Record();

        // 用户空间信息
        User user = new User().findById(userid);
        Double useStorage = user.getDouble("uusespace");
        Double totalStorage = user.getDouble("utotalspace");

        resultRecord.set("useStorage", useStorage).set("totalStorage", totalStorage);

        // 签到信息（当天是否签到、签到天数）
        String today = sdf.format(new Date());
        Sign sign = new Sign();
        List<Record> signInfoList = sign.getUserSignInInfo(userid, "0");
        if (signInfoList.size() == 0) {
            // 从未签到过
            resultRecord.set("isTodaySign", false).set("signDay", 0).set("showSign", "1");
        } else {
            String signStartDate = signInfoList.get(0).get("signStartDate").toString();
            String signEndDate = signInfoList.get(0).get("signEndDate").toString();
            //by lk 是否显示签到福利
            String firstSign = signInfoList.get(0).get("signFirstTime").toString();
            long f = sdf.parse(firstSign).getTime();
            if (f > sdf.parse("2017-12-14 23:59:59").getTime()) {
                resultRecord.set("showSign", 1);
            } else {
                resultRecord.set("showSign", 0);
            }
            //by lk 是否显示签到福利  end
            // 签到天数
            long to = sdf.parse(signEndDate).getTime();
            long from = sdf.parse(signStartDate).getTime();
            int signDay = (int) ((to - from) / (1000 * 60 * 60 * 24)) + 1;
            resultRecord.set("signDay", signDay);
            //历史最大签到天数，用来判断是否可以领取
            int signCount = signInfoList.get(0).get("signCount");
            resultRecord.set("signCount", signCount);

            // 当天是否签到
            if (signEndDate.equals(today)) {
                resultRecord.set("isTodaySign", true);
            } else {
                resultRecord.set("isTodaySign", false);
            }

        }

        // 上传照片数
        int picNum = user.GetUserUploadPicNum(userid);
        resultRecord.set("picNum", picNum);

        // 邀请好友数
        List<Record> encourageList = Encourage.getEncourageInfo(userid);
        int inviteNum = 0;
        if (encourageList.size() != 0) {
            inviteNum = Integer.parseInt(encourageList.get(0).get("inviteNum").toString());
            resultRecord.set("inviteNum", inviteNum);
            // 是否分享过到朋友圈和微信群
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

        // 返回结果
        List<Record> resultList = new ArrayList<Record>();
        resultList.add(resultRecord);
        jsonString = jsonData.getSuccessJson(resultList);
        renderText(jsonString);

    }


    /**
     * 显示福利页面
     *
     * @throws ParseException
     */
    @Before(CrossDomain.class)
    public void ShowWelfarePage2() throws ParseException {
        String userid = this.getPara("userid");

        Record resultRecord = new Record();

        // 用户空间信息
        User user = new User().findById(userid);
        Double useStorage = user.getDouble("uusespace");
        Double totalStorage = user.getDouble("utotalspace");

        resultRecord.set("useStorage", useStorage).set("totalStorage", totalStorage);

        // 签到信息（当天是否签到、签到天数）
        String today = sdf.format(new Date());
        Sign sign = new Sign();
        List<Record> signInfoList = sign.getUserSignInInfo(userid, "0");
        if (signInfoList.size() == 0) {
            // 从未签到过
            resultRecord.set("isTodaySign", false).set("signDay", 0).set("showSign", "1");
        } else {
            String signStartDate = signInfoList.get(0).get("signStartDate").toString();
            String signEndDate = signInfoList.get(0).get("signEndDate").toString();
            //by lk 是否显示签到福利
            String firstSign = signInfoList.get(0).get("signFirstTime").toString();
            long f = sdf.parse(firstSign).getTime();
            if (f > sdf.parse("2017-12-14 23:59:59").getTime()) {
                resultRecord.set("showSign", 1);
            } else {
                resultRecord.set("showSign", 0);
            }
            //by lk 是否显示签到福利  end
            // 签到天数
            long to = sdf.parse(signEndDate).getTime();
            long from = sdf.parse(signStartDate).getTime();
            long todays = sdf.parse(today).getTime();
            int count = (int) ((todays - to) / (1000 * 60 * 60 * 24));
            System.out.println(count);
            //判断是否断签。如果断签显示signDay为0
            if (count >= 2) {
                int signDay = 0;
                resultRecord.set("signDay", signDay);
            } else {
                int signDay = (int) ((to - from) / (1000 * 60 * 60 * 24)) + 1;
                resultRecord.set("signDay", signDay);
            }

            //历史最大签到天数，用来判断是否可以领取
            int signCount = signInfoList.get(0).get("signCount");
            resultRecord.set("signCount", signCount);

            // 当天是否签到
            if (signEndDate.equals(today)) {
                resultRecord.set("isTodaySign", true);
            } else {
                resultRecord.set("isTodaySign", false);
            }

        }

        // 上传照片数
        int picNum = user.GetUserUploadPicNum(userid);
        resultRecord.set("picNum", picNum);

        // 邀请好友数
        List<Record> encourageList = Encourage.getEncourageInfo(userid);
        int inviteNum = 0;
        if (encourageList.size() != 0) {
            inviteNum = Integer.parseInt(encourageList.get(0).get("inviteNum").toString());
            resultRecord.set("inviteNum", inviteNum);
            // 是否分享过到朋友圈和微信群
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

        // 返回结果
        List<Record> resultList = new ArrayList<Record>();
        resultList.add(resultRecord);
        jsonString = jsonData.getSuccessJson(resultList);
        renderText(jsonString);

    }


    /**
     * 显示兑奖页面
     */
    @Before(CrossDomain.class)
    public void ShowPrizeReceivePage() {
        String userid = this.getPara("userid");
        String type = this.getPara("type");

        // 无数据则插入一条新数据
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
     * 成功邀请好友
     */
    public void SuccessInviteFriend() {
        String userid = this.getPara("userid");

        // 增加邀请人数
        boolean flag = service.recordSuccessInviteFriend(userid);

        // 增加用户总空间0.5G = 524288 KB
        boolean increaseFlag = false;
        if (flag) {
            User user = new User();
            increaseFlag = user.IncreaseUserTotalStorageSpace(userid, 524288.00);
        }
        jsonString = dataProcess.updateFlagResult(flag && increaseFlag);
        renderText(jsonString);
    }

    /**
     * 领取奖励
     */
    @Before(CrossDomain.class)
    public void ReceiveEncourageReward() {
        String userid = this.getPara("userid");
        String type = this.getPara("type");
        String level = this.getPara("level");

        // 拼接字段的key值
        String key = "";

        // 获取encourageID
        String encourageID = new Encourage().getEncourageIDbyUserID(userid);
        Encourage en = new Encourage().findById(encourageID);
        boolean receiveFlag = false;
        boolean updateFlag = false;

        switch (type) {

            case "shareToMoments":
                // 分享至朋友圈，一次性奖励
                // 增加0.5G = 524288 KB 空间
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
                // 分享至微信群，一次性奖励
                // 增加0.5G = 524288 KB 空间
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
                // 签到奖励领取
                key = "signLevel" + level;
                break;

            case "inviteFriend":
                // 邀请好友奖励领取
                key = "inviteLevel" + level;
                break;

            case "uploadPicture":
                key = "uploadLevel" + level;
                // 上传图片奖励领取
                break;

            default:
                break;
        }

        // 更新数据
        en.set(key, 1);
        updateFlag = en.update();

        // 返回结果
        jsonString = dataProcess.updateFlagResult(updateFlag);
        renderText(jsonString);
    }

    /**
     * 填写物流信息
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
     * 填写物流信息(新)
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
        //获取当前时间
        String current = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        Timestamp elReceiveTime = Timestamp.valueOf(current);

        // 拼接字段的key值
        String key = "";
        switch (type) {
            case "signIn":
                // 签到奖励领取
                key = "signLevel" + level;
                break;

            case "inviteFriend":
                // 邀请好友奖励领取
                key = "inviteLevel" + level;
                break;

            case "uploadPicture":
                key = "uploadLevel" + level;
                // 上传图片奖励领取
                break;

            default:
                break;
        }

        EncourageLogistics el = new EncourageLogistics();

        List<Record> elist = el.getUserEncourageInfo(userid, key);

        boolean elflag = false;
        //当查询结果为空时,新建一个奖品详情表
        if (elist.size() == 0) {
            el.set("elUserID", userid).set("elType", key).set("elTypeName", elTypeName).set("elReceiveTime", elReceiveTime)
                    .set("elName", name).set("elAddress", address).set("elPhone", phone).set("elStatus", 0);
            elflag = el.save();
        } else {
            String elID = elist.get(0).get("elID").toString();
            //将奖品的详情保存
            el = el.findById(elID);
            el.set("elTypeName", elTypeName).set("elReceiveTime", elReceiveTime).set("elStatus", 0)
                    .set("elName", name).set("elAddress", address).set("elPhone", phone);
            elflag = el.update();
        }

        //更新成功后返改变奖品的状态(此时是已处理的状态)
        if (elflag) {
            String encourageID = new Encourage().getEncourageIDbyUserID(userid);
            Encourage en = new Encourage().findById(encourageID);
            en.set(key, 1);
            boolean enflag = en.update();
            jsonString = dataProcess.insertFlagResult(enflag);
            renderText(jsonString);
        } else {
            //跟新失败返回失败消息
            jsonString = dataProcess.insertFlagResult(elflag);
            renderText(jsonString);
        }
    }

    /**
     * 获取二维码
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
     * <小程序签到兑奖>活动接口 End
     *******************************/

    /*******************************
     * <APP签到激励>活动接口 Start
     *******************************/

    /**
     * 显示扩容页面
     *
     * @throws ParseException
     */
    public void ShowExpandSpacePage() throws ParseException {
        String userid = this.getPara("userid");

        // 扩容信息
        List<Record> encourageList = Encourage.getAppEncourageInfo(userid);

        // 签到信息（当天是否签到、签到天数）
        String today = sdf.format(new Date());
        Sign sign = new Sign();
        List<Record> signInfoList = sign.getUserSignInInfo(userid, "1");
        if (signInfoList.size() == 0) {
            // 从未签到过
            encourageList.get(0).set("isTodaySign", false).set("signDay", 0);
        } else {
            String signStartDate = signInfoList.get(0).get("signStartDate").toString();
            String signEndDate = signInfoList.get(0).get("signEndDate").toString();

            int signDay = sign.GetSignDay(signStartDate, signEndDate);
            encourageList.get(0).set("signDay", signDay);

            // 当天是否签到
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
     * 签到
     *
     * @throws ParseException
     */
    @Before(Tx.class)
    public void AppSignIn() throws ParseException {
        String userid = this.getPara("userid");

        // 签到
        boolean signInFlag = service.signIn(userid, "1");

        if (signInFlag) {
            // 如果已连续七天签到，将flag设为可领取，将签到起始日设为明天，在今天看即为0
            Sign sign = new Sign();
            List<Record> signInfoList = sign.getUserSignInInfo(userid, "1");
            String signStartDate = signInfoList.get(0).get("signStartDate").toString();
            String signEndDate = signInfoList.get(0).get("signEndDate").toString();
            int signDay = sign.GetSignDay(signStartDate, signEndDate);
            if (signDay == 7) {
                // 签到起始日设置为明天
                String signID = signInfoList.get(0).get("signID").toString();
                sign = new Sign().findById(signID);
                // 获取日期
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DATE, 1);
                String tomorrow = sdf.format(cal.getTime());
                sign.set("signStartDate", tomorrow);

                // 设置为可领取奖励
                boolean encourageFlag = Encourage.SetOneFieldCanBeGet(userid, "appSign");

                jsonString = dataProcess.updateFlagResult(sign.update() && encourageFlag);
            } else {
                jsonString = jsonData.getSuccessJson();
            }

        } else {
            jsonString = jsonData.getJson(2030, "当天已签到");
        }

        renderText(jsonString);
    }

    /**
     * 领取奖励
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
     * APP成功邀请好友
     */
    public void AppSuccessInviteFriend() {
        String userid = this.getPara("userid");
        String beInvitedUserid = this.getPara("beInvitedUserid");

        // userid解密
        userid = dataProcess.decryptData(userid, "userid");

        // 判断用户是否已经被邀请
        boolean isInvite = InviteList.JudgeIsInvite(userid, beInvitedUserid);
        if (isInvite) {
            jsonString = jsonData.getJson(2031, "已邀请过该好友");
        } else {
            // 插入邀请信息
            InviteList.InsertInviteInfo(userid, beInvitedUserid);
            // 增加邀请数量
            List<Record> list = Encourage.getAppEncourageInfo(userid);
            int appInviteNum = Integer.parseInt(list.get(0).get("appInviteNum").toString()) + 1;
            String encourageID = list.get(0).get("encourageID").toString();
            Encourage en = new Encourage().findById(encourageID);
            en.set("appInviteNum", appInviteNum);

            // 判断是否满足可领取奖励的条件
            int appInvite = Integer.parseInt(list.get(0).get("appInvite").toString());
            if (appInvite == 0 && appInviteNum >= 3) {
                en.set("appInvite", 1);
            }
            jsonString = dataProcess.updateFlagResult(en.update());
        }

        renderText(jsonString);

    }

    /**
     * 满足领奖条件
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
     * <APP签到激励> 活动接口 End
     *******************************/

    /*******************************
     * <玩图小程序> 活动接口 Start
     *******************************/

    /**
     * 获取玩图小程序banner
     */
    public void GetPlaySmallAppBanner() {
        List<Record> list = Db.find("select bpic,btitle,bdata from banner where btype=6 and bstatus=0");
        jsonString = jsonData.getSuccessJson(list);
        renderText(jsonString);
    }

    /*******************************
     * <玩图小程序>活动接口 End
     *******************************/

    /*******************************
     * <城市空间>活动接口 Start
     *******************************/

    /**
     * 获取时刻banner
     */
    public void GetMomentsBanner() {
        List<Record> list = Db.find("select bpic,btitle,bdata from banner where btype=4 and bstatus=0");
        jsonString = jsonData.getSuccessJson(list);
        renderText(jsonString);
    }

    /**
     * 显示我的城市
     */
    public void ShowMyCitySpace() {
        String userid = this.getPara("userid");
        // 获取加入的城市空间列表
        List<Record> list = Db.find(
                "select groupid,gname,gpic from groups,groupmembers where groupid=gmgroupid and gtype=14 and gmuserid="
                        + userid + " and gstatus=0 and gmstatus=0 order by gmtime desc ");
        // 获取城市各城市排名
        List<Record> cityRank = service.getCityRank();
        // 插入排名
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
     * 显示城市相册列表
     */
    public void ShowCitySpaceList() {
        List<Record> list = service.getCityRank();
        jsonString = jsonData.getSuccessJson(list);
        renderText(jsonString);
    }

    /**
     * 搜索城市相册
     */
    public void SearchCitySpace() {
        String cityName = this.getPara("cityName");

        // 正则参数
        String regex = cityName + "+";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher;

        // 结果列表
        List<Record> resultList = new ArrayList<Record>();
        // 获取城市相册列表
        List<Record> cityList = service.getCityRank();
        // 正则匹配空间
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
     * 显示城市相册详情（包含加入空间判断、城市信息、我的贡献）
     */
    public void ShowMyPhotoContribution() {
        // 基本参数
        String userid = this.getPara("userid");
        String groupid = this.getPara("groupid");
        // 用户来源参数
        String port = this.getPara("port");
        String fromUserID = this.getPara("fromUserID");

        // 判断用户是否在空间内
        GroupMember gm = new GroupMember();
        boolean isInFlag = gm.judgeUserIsInTheAlbum(Integer.parseInt(userid), Integer.parseInt(groupid)); // true时用户不在空间内
        boolean flag = true;
        int count = 1;
        if (isInFlag) {
            // 不在相册，则插入用户数据
            gm = new GroupMember().set("gmgroupid", groupid).set("gmuserid", userid).set("gmPort", port)
                    .set("gmFromUserID", fromUserID);
            // 捕获插入异常，用户重复点击时会导致插入失败
            try {
                flag = gm.save();
                // 更新分组表中组成员数量字段
                count = Db.update("update groups set gnum = gnum+1 where groupid='" + groupid + "' ");
            } catch (ActiveRecordException e) {
                flag = true;
                count = 1;
            }
        }

        if (flag && (count == 1)) {
            // 获取城市空间信息
            Record cityInfo = service.getCitySpaceInfo(groupid);
            // 获取我的贡献
            Record myContribution = service.getMyContributionInCitySpace(userid, groupid);

            // 返回结果
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
     * 显示城市相册贡献榜
     */
    public void ShowCitySpaceContributionRank() {
        String groupid = this.getPara("groupid");
        List<Record> list = service.getCityContributionRank(groupid);
        jsonString = jsonData.getSuccessJson(list);
        renderText(jsonString);

    }

    /******************************* <城市空间>活动接口 End *******************************/

    /******************************* <打印>活动接口 Start *******************************/

    /**
     * 用户打印状态判断
     */
    public void PrintStatusJudge() {
        String userid = this.getPara("userid");
        List<Record> list = Db
                .find("select printID,printCode,printStatus,printPic from print where printUserID=" + userid + " ");
        jsonString = jsonData.getSuccessJson(list);
        renderText(jsonString);
    }

    /**
     * 下单
     */
    public void PlaceOrder() {
        // 获取参数
        String userid = this.getPara("userid");
        String picAddress = this.getPara("picAddress");
        // 第三方提供参数
        String openId = "1529885";
        String token = "8576205d357f8f87e3d13b3fd526b72";
        // MD5加密获取签名
        String sign = YinianUtils.EncodeByMd5With32Lowcase(picAddress + token);
        // 构造请求参数
        String param = "openId=" + openId + "&picUrl=" + picAddress + "&sign=" + sign;
        String url = "https://weixin.sinzk.com/external/order";
        // 发送请求并返回结果
        String result = dataProcess.sendPost(url, param, "text");
        JSONObject jo = JSONObject.parseObject(result);
        String code = jo.getString("code");
        if (code.equals("10000")) {
            String orderCode = jo.getString("ordercode");
            // 参入数据
            Print print = new Print().set("printUserID", userid).set("printPic", picAddress).set("printCode",
                    orderCode);
            if (print.save()) {
                List<Record> resultList = dataProcess.makeSingleParamToList("orderCode", orderCode);
                jsonString = jsonData.getSuccessJson(resultList);
            } else {
                jsonString = dataProcess.insertFlagResult(false);
            }
        } else {
            jsonString = jsonData.getJson(3000, "打印下单失败");
        }
        renderText(jsonString);
    }

    /**
     * 判断打印是否成功
     */
    public void JudgePrintIsSuccess() {
        String printCode = this.getPara("printCode");
        List<Record> list = Db.find("select printUserID,printStatus from print  where printCode=" + printCode + " ");
        jsonString = jsonData.getSuccessJson(list);
        renderText(jsonString);
    }

    /**
     * 打印成功,回调
     */
    public void PrintSuccessCallBack() {
        String printCode = this.getPara("printCode");
        int count = Db.update("update print set printStatus=1 where printCode=" + printCode + " ");
        if (count == 1) {
            jsonString = jsonData.getSuccessJson();
        } else {
            jsonString = jsonData.getJson(3001, "打印码不存在");
        }
        renderText(jsonString);
    }

    /******************************* <打印>活动接口 End *******************************/


    /********************************<后台管理接口> start********************************/
    /**
     * 查询商品信息详情列表
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
     * 今日新增奖品领取详情列表
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
     * 物流状态修改
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
                //修改物流状态
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

    /********************************<共享纸巾接口> start *****************************/
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
     * 获得所有的活动相册
     */
    public void GetActivitiGroups() {
        String type = this.getPara("type");
        String number = this.getPara("number");
        List<Record> activitiGroupList = new ArrayList<>();
        if (type.equals("initialize")) {
            //初始化数据从缓存中获取，5分钟缓存
            activitiGroupList = CacheKit.get("DataSystem", "activitiGroupList");
            if (activitiGroupList == null) {
                //缓存为空  从数据库查询
                activitiGroupList = new ActivityService().getAllActivitiGroups(type, number);
                CacheKit.put("DataSystem", "activitiGroupList", activitiGroupList);
            }
        } else {
            //分页加载
            activitiGroupList = new ActivityService().getAllActivitiGroups(type, number);
        }
        String jsonString = jsonData.getSuccessJson(activitiGroupList);
        renderText(jsonString);
    }

    /**
     * 获得我参与的活动相册
     */
    public void GetMyActivitiGroups() {
        String userid = this.getPara("userid");
        String type = this.getPara("type");
        String jointime = this.getPara("jointime");
        List<Record> myActivitiList = new ArrayList<>();
        if (type.equals("initialize")) {
            //初始化数据从系统缓存中获取,5分钟缓存
            myActivitiList = CacheKit.get("ServiceCache", userid + "_myActivitiList");
            if (myActivitiList == null) {
                //缓存中获取为空,从数据库查询
                myActivitiList = new ActivityService().getMyActivitiGroups(jointime, userid, type);
                CacheKit.put("ServiceCache", userid + "_myActivitiList", myActivitiList);
            }

        } else {
            //分页加载
            myActivitiList = new ActivityService().getMyActivitiGroups(jointime, userid, type);
        }
        String jsonString = jsonData.getSuccessJson(myActivitiList);
        renderText(jsonString);
    }

    /**
     * 获得首页Banner图
     */
    public void getBanner() {
        List<Record> list = new ActivityService().getActivitiBanner();
        String jsonString = jsonData.getSuccessJson(list);
        renderText(jsonString);
    }


}
