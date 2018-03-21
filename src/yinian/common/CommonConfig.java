package yinian.common;

import com.jfinal.config.*;
import com.jfinal.plugin.activerecord.ActiveRecordPlugin;
import com.jfinal.plugin.c3p0.C3p0Plugin;
import com.jfinal.plugin.ehcache.EhCachePlugin;
import com.jfinal.render.ViewType;
import yinian.app.YinianBackground;
import yinian.controller.*;
import yinian.model.*;

public class CommonConfig extends JFinalConfig {

    /**
     * ���ó���
     */
    @Override
    public void configConstant(Constants me) {
        // TODO Auto-generated method stub

        // ���������ļ�������ʹ��getProperty(....)��ȡ�����ļ��е�ֵ
        this.loadPropertyFile("config.properties");
        // �Ƿ�򿪿���ģʽ������ģʽ�»��ÿ�������ڿ���̨������棬�������ļ����õ�����ֵ��Ĭ��Ϊ������
        // me.setDevMode(this.getPropertyToBoolean("devMode", false));
        // ���˿��������д򿪿�����ģʽ
        me.setDevMode(true);
        // �ַ���
        me.setEncoding("utf-8");
        // ����url�����ָ���
        me.setUrlParaSeparator("-");
        // Ĭ����ͼ���ͣ�����Ϊjsp������freemaker��velocity
        me.setViewType(ViewType.JSP);
    }

    /**
     * ����·��
     */
    @Override
    public void configRoute(Routes me) {
        // TODO Auto-generated method stub
        me.add("/yinian", YinianController.class);// �����������
        me.add("/h5", H5Controller.class);// web�˿�����
        me.add("/back", YinianBackground.class);// ��̨�ӿڿ�����
        me.add("/eb", EBController.class);// ���̿�����
        me.add("/data", DataController.class);// ���ݿ�����
        me.add("/im", IMController.class);// ��ʱͨѶ������
        me.add("/divine", DivineController.class);// ÿ��һǩ������
        me.add("/other", OtherController.class);// ��������������
        me.add("/activity", ActivityController.class);// ����ܿ�����
        me.add("/sliver", SliverController.class);// ������Ƭ���ܿ�����
        me.add("/space", SpaceController.class);// �ռ������
        me.add("/event", EventController.class);// ��̬������
        me.add("/user", UserController.class);// �û�������
        me.add("/ad", AdController.class);// ��������
        me.add("/personal", PersonalController.class);//by lk  ��������������
        me.add("/simH5", SimplificationH5Controller.class);//by lk �����С���������ӿ�
        me.add("/test", TestController.class);//by lk ��ʱʹ��controller
        me.add("/db", NewDbFieldController.class);//by lk �޸ı�ṹʱ��ʷ���ݴ���
        me.add("/portal", PortalController.class);//by lk �������·�������
        me.add("/bug", BugController.class);//by lk ��Ӫ���������
        me.add("/pc", PcController.class);//by lk pc��ר�ýӿ�
        me.add("/adv", AdvertisementController.class);//by lk pc��ר�ýӿ�
        me.add("/newh5", NewH5Controller.class);//by lk ��С����İ�ӿ�
        me.add("/app", AppController.class);//by ly appר�ýӿ�
        me.add("/points", PointsShopController.class);//�����̳�ר�ýӿ�
        me.add("/msgCenter", MessageCenterController.class);//��Ϣ���Ľӿ�
    }

    /**
     * ���ò��
     */
    @Override
    public void configPlugin(Plugins me) {
        // TODO Auto-generated method stub
        //����spring���

        // ����C3p0���ݿ����ӳز����Ҫ����c3p0.jar��mysql-connector.java
        C3p0Plugin c3p0Plugin = new C3p0Plugin(getProperty("jdbcUrl"), getProperty("user"),
                getProperty("password").trim());
        c3p0Plugin.setMaxPoolSize(50);
        c3p0Plugin.setInitialPoolSize(10);
        c3p0Plugin.setMaxIdleTime(30);
        me.add(c3p0Plugin);

        // ����encache������
        me.add(new EhCachePlugin());

        // ����ActiveRecord���
        ActiveRecordPlugin arp = new ActiveRecordPlugin(c3p0Plugin);
        me.add(arp);

        // ���ø������ݿ��
        arp.addMapping("users", "userid", User.class); // �û���ӳ��
        arp.addMapping("groups", "groupid", Group.class); // �����ӳ��
        arp.addMapping("groupmembers", "gmid", GroupMember.class); // ���Ա��ӳ��
        arp.addMapping("events", "eid", Event.class); // ʱ���ӳ��
        arp.addMapping("comments", "cid", Comment.class); // ���۱�ӳ��
        arp.addMapping("pictures", "pid", Picture.class); // ͼƬ��ӳ��
        arp.addMapping("messages", "mid", Message.class); // ��Ϣ��ӳ��
        arp.addMapping("notifications", "nid", Notification.class); // ֪ͨ��ӳ��
        arp.addMapping("informs", "iid", Inform.class); // ֪ͨ��ӳ��
        arp.addMapping("invitegroup", "igid", InviteGroup.class); // ��������ӳ��
        arp.addMapping("waits", "wid", Wait.class); // �ȴ���ӳ��
        arp.addMapping("likes", "lid", Likes.class); // ���ޱ�ӳ��
        arp.addMapping("feedbacks", "fid", Feedback.class); // ������ӳ��
        arp.addMapping("music", "musicID", Music.class); // ���ֱ�ӳ��
        arp.addMapping("templet", "templetID", Templet.class); // ģ���ӳ��
        arp.addMapping("musicalbum", "maID", MusicAlbum.class); // ��������ӳ��
        arp.addMapping("mapicture", "mapID", MAPicture.class); // �������ͼƬ��ӳ��
        arp.addMapping("orders", "orderID", Order.class); // �������ͼƬ��ӳ��
        arp.addMapping("todaymemory", "TMid", TodayMemory.class); // �������ӳ��
        arp.addMapping("gifts", "giftID", Gift.class); // �����ӳ��
        arp.addMapping("push", "pushID", Push.class); // ���ͱ�ӳ��
        arp.addMapping("historytag", "historyTagID", HistoryTag.class); // ��ʷ��ǩ��ӳ��
        arp.addMapping("tags", "tagID", Tag.class); // ��ǩ��ӳ��
        arp.addMapping("goods", "goodsID", Goods.class); // ��Ʒ��ӳ��
        arp.addMapping("historycover", "historyCoverID", HistoryCover.class); // ��ʷ����ӳ��
        arp.addMapping("lovertimemachine", "ltmID", LoverTimeMachine.class); // ����ʱ�����ӳ��
        arp.addMapping("eborders", "ebOrderID", EBOrder.class); // ���̶�����ӳ��
        arp.addMapping("ebgoods", "ebGoodsID", EBGoods.class); // ������Ʒ��ӳ��
        arp.addMapping("address", "addressID", Address.class); // �ջ���ַ��ӳ��
        arp.addMapping("contact", "contactID", Contact.class); // ��ϵ�ͷ���ӳ��
        arp.addMapping("items", "itemID", Item.class); // ��Ʒ��Ŀ��ӳ��
        arp.addMapping("backupevent", "backupEventID", BackupEvent.class); // ���ݶ�̬��ӳ��
        arp.addMapping("backupphoto", "backupPhotoID", BackupPhoto.class); // ������Ƭ��ӳ��
        arp.addMapping("view", "vid", View.class); // ��̬�鿴��ʷ��
        arp.addMapping("chat", "chatID", Chat.class); // �����¼��
        arp.addMapping("mark", "markID", Mark.class); // ʱ��ӡ�Ǳ�
        arp.addMapping("note", "noteID", Note.class); // ��ע����
        arp.addMapping("redpacket", "redPacketID", Redpacket.class); // �����
        arp.addMapping("receive", "receiveID", Receive.class); // �����ȡ��
        arp.addMapping("coupon", "couponID", Coupon.class); // �Ż�ȯ��
        arp.addMapping("fate", "fateID", Fate.class); // ���Ʊ�
        arp.addMapping("fateopen", "fateOpenID", FateOpen.class); // ���ƹ�����
        arp.addMapping("fatehistory", "fateHistoryID", FateHistory.class); // ������ʷ��
        arp.addMapping("charm", "charmID", Charm.class); // �����
        arp.addMapping("givecharm", "gcID", GiveCharm.class); // ������ͱ�
        arp.addMapping("cart", "cartID", Cart.class); // ���ﳵ��
        arp.addMapping("puzzle", "puzzleID", Puzzle.class); // ƴͼ��
        arp.addMapping("join", "joinID", Join.class); // ������
        arp.addMapping("like", "likeID", Like.class); // ������ޱ�
        arp.addMapping("temp", "tempID", Temp.class); // �ĺ󼴷ٱ�
        arp.addMapping("tempView", "tempViewID", TempView.class); // �ĺ󼴷ٲ鿴��
        arp.addMapping("authorizePhoto", "authorizeID", AuthorizePhoto.class); // ������Ȩ��
        arp.addMapping("peep", "peepID", Peep.class); // ���������
        arp.addMapping("industry", "industryID", Industry.class); // ��ҵ�����
        arp.addMapping("industryPhoto", "id", IndustryPhoto.class); // ��ҵ����ͼƬ��
        arp.addMapping("textLibrary", "textID", TextLibrary.class); // ���ֿ��
        arp.addMapping("reward", "RewardID", Reward.class); // ������
        arp.addMapping("grab", "grabID", Grab.class); // �������
        arp.addMapping("redEnvelop", "redEnvelopID", RedEnvelop.class); // �����
        arp.addMapping("transaction", "transactionID", Transaction.class); // ���׼�¼��

        // ���ر�
        arp.addMapping("sign", "signID", Sign.class); // ǩ����
        arp.addMapping("encourage", "encourageID", Encourage.class); // ������Ϣ��
        arp.addMapping("encourageLogistics", "elID", EncourageLogistics.class); // ����������
        arp.addMapping("inviteList", "id", InviteList.class); // �����б��
        arp.addMapping("points", "poid", Points.class);//���ֱ�
        arp.addMapping("pointsReceive", "prid", PointsReceive.class);//���ּ�¼��
        arp.addMapping("pointsgift", "pgid", PointsGift.class);//���ֽ�Ʒ��
        arp.addMapping("pointstype", "ptypeid", PointsType.class);//�������ͱ�
        arp.addMapping("pointsbonus", "pbid", PointsBonus.class);//�������ѱ�

        // ������ر�
        arp.addMapping("data", "id", Data.class); // ���ݱ�ӳ��
        arp.addMapping("botton", "id", Botton.class); // ������ӳ��
        arp.addMapping("interface", "id", Interface.class); // �����ӳ��
        arp.addMapping("invite", "id", Invite.class); // �����¼���ӳ��
        arp.addMapping("share", "id", Share.class); // �����¼���ӳ��
        arp.addMapping("ynDaily", "id", YiNianDaily.class); // �ձ�ӳ��
        arp.addMapping("ynWeekly", "id", YinianWeekly.class); // �ܱ�ӳ��
        arp.addMapping("ynMonthly", "id", YinianMonthly.class); // �±�ӳ��
        arp.addMapping("albumUVandPV", "id", AlbumUVandPV.class); // �ռ���UV��PV��ӳ��

        // ������ر�
        arp.addMapping("userPhoto", "id", UserPhoto.class); // �û���Ƭ��ӳ��
        //arp.addMapping("ynLog", "id", YiNianLog.class); // ����log��־��ӳ��
        arp.addMapping("smallAppLog", "id", SmallAppLog.class); // С��������ӳ��
        arp.addMapping("ad", "adID", Ad.class); // ����
        arp.addMapping("history", "historyID", History.class);// ��ʷ���ʱ�
        arp.addMapping("formid", "id", FormID.class);// С�������ͱ�ID��

        // ���������ݱ�
        arp.addMapping("print", "printID", Print.class);// ��������ӡ��
        // by lk ���������
        arp.addMapping("personalPhotoAlbum", "aid", PersonalPhotoAlbum.class);//�û����ͼƬ��
        arp.addMapping("personalAlbum", "peid", PersonalAlbum.class);//by ylm ����������
        // by lk  �û��Ƿ��ö�
        arp.addMapping("groupsistop", "id", GroupsIsTop.class);
        // by lk  �ռ��ܷ񷢲���̬��ά��
        arp.addMapping("groupCanPublish", "id", GroupCanPublish.class);
        // by lk  �����С���������Ϣ��
        arp.addMapping("simH5Share", "id", SimH5Share.class);
        // by lk  ��������ʾ��Ϣ��
        arp.addMapping("activityHint", "id", ActivityHint.class);
        // by lk  ��������ʾ��Ϣ��¼��
        arp.addMapping("activityHintMsg", "id", ActivityHintMsg.class);
        // by lk  �������¼�¼��
        arp.addMapping("portal_articles", "id", Article.class);
        //by lk ��̨������Ա��
        arp.addMapping("pcadmin", "id", PcAdmin.class);
        //by lk ��������Ա��
        arp.addMapping("userblacklist", "id", UserBlacklist.class);
        //by lk �ȵ�ͼƬ�б�
        arp.addMapping("hotpic", "hid", HotPic.class);
        //by lk �û��Ѳ鿴�ȵ�ͼ��
        arp.addMapping("userhotpic", "uhid", UserHotPic.class);
        //by lk ��������ӿ������¼��
        arp.addMapping("partnerrecord", "prid", PartnerRecord.class);
        //by lk ����-��������ñ�
        arp.addMapping("activitygrouprule", "id", ActivityGroupRule.class);
        //by ����ǩ�����ֱ�
        arp.addMapping("signold", "signID", Signold.class);
    }

    /**
     * ����ȫ��������
     */
    @Override
    public void configInterceptor(Interceptors me) {
        // TODO Auto-generated method stub

        // ��ӿ��Ʋ�ȫ��������
        //me.addGlobalActionInterceptor(new ExceptionIntoLogInterceptor());
        // ���ҵ���ȫ��������
        //me.addGlobalServiceInterceptor(new ExceptionIntoLogInterceptor());
    }

    /**
     * ���ô�����
     */
    @Override
    public void configHandler(Handlers me) {
        // TODO Auto-generated method stub

    }

    /**
     * ��ϵͳ������ɺ�ص� ϵͳ�����󴴽������߳�
     */
    @Override
    public void afterJFinalStart() {
        System.out.println("jfinal start");
	/*	int money=0;
		BufferedReader reader = null; 
		try {
			String strUrl = "http://picture.zhuiyinanian.com/yinian/money.txt";
			URL url = new URL(strUrl); 
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			InputStreamReader input = new InputStreamReader(conn.getInputStream());
			reader = new BufferedReader(input);
			//reader = new BufferedReader(new InputStreamReader(new FileInputStream("D:\\leiyu\\adsenseValue.txt")));
			StringBuffer buffer = new StringBuffer();
			String line = reader.readLine();
			while (line!=null) {
				buffer.append(line);
				line = reader.readLine();
			}
			reader.close(); 
			String content = buffer.toString();			
			if (content != null && content.length() > 0) {
				money=Integer.parseInt(content);
			}			
		} catch (Exception e) {
			e.printStackTrace();			
		} finally {
			try{
				reader.close();	
			}catch (Exception e) {
				e.printStackTrace();			
			}
			CanUserdMoney.getInstance().setMoney(money);
			System.out.println("�ܽ�"+CanUserdMoney.getInstance().getMoney());
		}
		*/
    }

    /**
     * ����ϵͳ�ر�ǰ�ص� ϵͳ�ر�ǰд�ػ���
     */
    @Override
    public void beforeJFinalStop() {
        System.out.println("jfinal end");
    }

}
