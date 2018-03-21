package junittest;

import com.jfinal.kit.PropKit;
import com.jfinal.plugin.activerecord.ActiveRecordPlugin;
import com.jfinal.plugin.druid.DruidPlugin;
import yinian.model.*;

/**
 *  测试工具类
 *
 * @author 杨岳
 * @date 2018/3/2015:16
 */
public class BaseTestJunit {
    /**
     * 设置静态对象，方便直接调用
     */
    public static final BaseTestJunit baseTestJunit=new BaseTestJunit();
    /**
     *  开启测试用于启动 ActiveRecordPlugin
     *  在所有测试方法的第一行调用开启
     *  默认调用测试数据库
     */
    public  void testStartAll(){
        /**
         * 加载配置文件
         */
        PropKit.use("config.properties");
        /**
         * 默认读取config.properties中数据源
         */
        String url = PropKit.get("jdbcUrl");
        String user = PropKit.get("user");
        String password = PropKit.get("password");

        DruidPlugin dp = new DruidPlugin(url,user,password);
        loadMapper(dp);
    }

    /**
     * 开启测试，并自定义测试的数据库
     *
     * @param url   数据库的rul路径
     * @param userName  数据库的账号
     * @param password  数据密码
     */
    public void  testStartAll(String url,String userName, String password){
        DruidPlugin dp = new DruidPlugin(url, userName, password);
        loadMapper(dp);
    }

    /**
     * 加载mapping
     */
    public static void loadMapper(DruidPlugin dp){
        ActiveRecordPlugin arp = new ActiveRecordPlugin(dp);
        /**
         * 加载mapping配置
         */
        arp.addMapping("users", "userid", User.class); // 用户表映射
        arp.addMapping("groups", "groupid", Group.class); // 分组表映射
        arp.addMapping("groupmembers", "gmid", GroupMember.class); // 组成员表映射
        arp.addMapping("events", "eid", Event.class); // 时间表映射
        arp.addMapping("comments", "cid", Comment.class); // 评论表映射
        arp.addMapping("pictures", "pid", Picture.class); // 图片表映射
        arp.addMapping("messages", "mid", Message.class); // 消息表映射
        arp.addMapping("notifications", "nid", Notification.class); // 通知表映射
        arp.addMapping("informs", "iid", Inform.class); // 通知表映射
        arp.addMapping("invitegroup", "igid", InviteGroup.class); // 邀请进组表映射
        arp.addMapping("waits", "wid", Wait.class); // 等待表映射
        arp.addMapping("likes", "lid", Likes.class); // 点赞表映射
        arp.addMapping("feedbacks", "fid", Feedback.class); // 反馈表映射
        arp.addMapping("music", "musicID", Music.class); // 音乐表映射
        arp.addMapping("templet", "templetID", Templet.class); // 模板表映射
        arp.addMapping("musicalbum", "maID", MusicAlbum.class); // 音乐相册表映射
        arp.addMapping("mapicture", "mapID", MAPicture.class); // 音乐相册图片表映射
        arp.addMapping("orders", "orderID", Order.class); // 音乐相册图片表映射
        arp.addMapping("todaymemory", "TMid", TodayMemory.class); // 今日忆表映射
        arp.addMapping("gifts", "giftID", Gift.class); // 礼物表映射
        arp.addMapping("push", "pushID", Push.class); // 推送表映射
        arp.addMapping("historytag", "historyTagID", HistoryTag.class); // 历史标签表映射
        arp.addMapping("tags", "tagID", Tag.class); // 标签表映射
        arp.addMapping("goods", "goodsID", Goods.class); // 商品表映射
        arp.addMapping("historycover", "historyCoverID", HistoryCover.class); // 历史封面映射
        arp.addMapping("lovertimemachine", "ltmID", LoverTimeMachine.class); // 情侣时光机表映射
        arp.addMapping("eborders", "ebOrderID", EBOrder.class); // 电商订单表映射
        arp.addMapping("ebgoods", "ebGoodsID", EBGoods.class); // 电商商品表映射
        arp.addMapping("address", "addressID", Address.class); // 收货地址表映射
        arp.addMapping("contact", "contactID", Contact.class); // 联系客服表映射
        arp.addMapping("items", "itemID", Item.class); // 商品条目表映射
        arp.addMapping("backupevent", "backupEventID", BackupEvent.class); // 备份动态表映射
        arp.addMapping("backupphoto", "backupPhotoID", BackupPhoto.class); // 备份照片表映射
        arp.addMapping("view", "vid", View.class); // 动态查看历史表
        arp.addMapping("chat", "chatID", Chat.class); // 聊天记录表
        arp.addMapping("mark", "markID", Mark.class); // 时光印记表
        arp.addMapping("note", "noteID", Note.class); // 备注名表
        arp.addMapping("redpacket", "redPacketID", Redpacket.class); // 红包表
        arp.addMapping("receive", "receiveID", Receive.class); // 红包领取表
        arp.addMapping("coupon", "couponID", Coupon.class); // 优惠券表
        arp.addMapping("fate", "fateID", Fate.class); // 运势表
        arp.addMapping("fateopen", "fateOpenID", FateOpen.class); // 运势公开表
        arp.addMapping("fatehistory", "fateHistoryID", FateHistory.class); // 运势历史表
        arp.addMapping("charm", "charmID", Charm.class); // 灵符表
        arp.addMapping("givecharm", "gcID", GiveCharm.class); // 灵符赠送表
        arp.addMapping("cart", "cartID", Cart.class); // 购物车表
        arp.addMapping("puzzle", "puzzleID", Puzzle.class); // 拼图表
        arp.addMapping("join", "joinID", Join.class); // 活动参与表
        arp.addMapping("like", "likeID", Like.class); // 表情点赞表
        arp.addMapping("temp", "tempID", Temp.class); // 阅后即焚表
        arp.addMapping("tempView", "tempViewID", TempView.class); // 阅后即焚查看表
        arp.addMapping("authorizePhoto", "authorizeID", AuthorizePhoto.class); // 互看授权表
        arp.addMapping("peep", "peepID", Peep.class); // 互看详情表
        arp.addMapping("industry", "industryID", Industry.class); // 行业真相表
        arp.addMapping("industryPhoto", "id", IndustryPhoto.class); // 行业真相图片表
        arp.addMapping("textLibrary", "textID", TextLibrary.class); // 文字库表
        arp.addMapping("reward", "RewardID", Reward.class); // 奖励表
        arp.addMapping("grab", "grabID", Grab.class); // 抢红包表
        arp.addMapping("redEnvelop", "redEnvelopID", RedEnvelop.class); // 红包表
        arp.addMapping("transaction", "transactionID", Transaction.class); // 交易记录表

        // 活动相关表
        arp.addMapping("sign", "signID", Sign.class); // 签到表
        arp.addMapping("encourage", "encourageID", Encourage.class); // 激励信息表
        arp.addMapping("encourageLogistics", "elID", EncourageLogistics.class); // 激励物流表
        arp.addMapping("inviteList", "id", InviteList.class); // 邀请列表表
        arp.addMapping("points", "poid", Points.class);//积分表
        arp.addMapping("pointsReceive", "prid", PointsReceive.class);//积分记录表
        arp.addMapping("pointsgift", "pgid", PointsGift.class);//积分奖品表
        arp.addMapping("pointstype", "ptypeid", PointsType.class);//积分类型表
        arp.addMapping("pointsbonus", "pbid", PointsBonus.class);//积分消费表

        // 数据相关表
        arp.addMapping("data", "id", Data.class); // 数据表映射
        arp.addMapping("botton", "id", Botton.class); // 按键表映射
        arp.addMapping("interface", "id", Interface.class); // 界面表映射
        arp.addMapping("invite", "id", Invite.class); // 邀请事件表映射
        arp.addMapping("share", "id", Share.class); // 分享事件表映射
        arp.addMapping("ynDaily", "id", YiNianDaily.class); // 日表映射
        arp.addMapping("ynWeekly", "id", YinianWeekly.class); // 周表映射
        arp.addMapping("ynMonthly", "id", YinianMonthly.class); // 月表映射
        arp.addMapping("albumUVandPV", "id", AlbumUVandPV.class); // 空间日UV和PV表映射

        // 其他相关表
        arp.addMapping("userPhoto", "id", UserPhoto.class); // 用户照片表映射
        //arp.addMapping("ynLog", "id", YiNianLog.class); // 忆年log日志表映射
        arp.addMapping("smallAppLog", "id", SmallAppLog.class); // 小程序错误表映射
        arp.addMapping("ad", "adID", Ad.class); // 广告表
        arp.addMapping("history", "historyID", History.class);// 历史访问表
        arp.addMapping("formid", "id", FormID.class);// 小程序推送表单ID表

        // 第三方数据表
        arp.addMapping("print", "printID", Print.class);// 第三方打印表
        // by lk 个人云相册
        arp.addMapping("personalPhotoAlbum", "aid", PersonalPhotoAlbum.class);//用户相册图片表
        arp.addMapping("personalAlbum", "peid", PersonalAlbum.class);//by ylm 个人云相册表
        // by lk  用户是否置顶
        arp.addMapping("groupsistop", "id", GroupsIsTop.class);
        // by lk  空间能否发布动态二维码
        arp.addMapping("groupCanPublish", "id", GroupCanPublish.class);
        // by lk  精简版小程序分享信息表
        arp.addMapping("simH5Share", "id", SimH5Share.class);
        // by lk  活动或跟新提示信息表
        arp.addMapping("activityHint", "id", ActivityHint.class);
        // by lk  活动或跟新提示信息记录表
        arp.addMapping("activityHintMsg", "id", ActivityHintMsg.class);
        // by lk  官网文章记录表
        arp.addMapping("portal_articles", "id", Article.class);
        //by lk 后台管理人员表
        arp.addMapping("pcadmin", "id", PcAdmin.class);
        //by lk 黑名单人员表
        arp.addMapping("userblacklist", "id", UserBlacklist.class);
        //by lk 热点图片列表
        arp.addMapping("hotpic", "hid", HotPic.class);
        //by lk 用户已查看热点图表
        arp.addMapping("userhotpic", "uhid", UserHotPic.class);
        //by 雷雨签到积分表
        arp.addMapping("signold", "signID", Signold.class);

        dp.start();
        arp.start();
    }
}
