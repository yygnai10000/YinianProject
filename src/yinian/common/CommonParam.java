package yinian.common;

public interface CommonParam {
    /*
     * 1000w��Ƭ��������
     */
    int pGroupId = 5268248;//����ID
    String pBeginData = "2018-02-12 00:00:00";//���ʼʱ��
    String pEndData = "2018-02-22 23:59:59";//��ر�ʱ��
    boolean pOpenBanner = true;//�ʱ�������banner�Ƿ���
    String pAdvId = "38";//groupadvertisements.id
    String pAdvListId = "11";//advertisementsmessage.advertisementsid
    boolean pOpenJoinGroup = false;//���û��������
    //boolean pGroupAddToTop=true;//�����ö�
    /*
     * 1000w��Ƭ��������end
     */
    int pGroupId2 = 5348798;//����ID
    String pBeginData2 = "2018-02-17 00:00:00";//���ʼʱ��
    String pEndData2 = "2018-02-22 23:59:59";//��ر�ʱ��
    /*
     * 1000w��Ƭ��3��
     */
    int pGroupId3 = 5386332;//����ID
    String pBeginData3 = "2018-02-23 00:00:00";//���ʼʱ��
    String pEndData3 = "2018-02-26 23:59:59";//��ر�ʱ��
    /*
     * 1000w��Ƭ��4��
     */
    int pGroupId4 = 5416927;//����ID
    String pBeginData4 = "2018-02-27 00:00:00";//���ʼʱ��
    String pEndData4 = "2018-03-02 23:59:59";//��ر�ʱ��
    /*
     * 1000w��Ƭ�ڶ���end
     */
    int pGroupId5 = 5460577;//ɹ�����裬��ǧԪ�ֽ�
    int pGroupId6 = 5518246;//�ϴ���Ƭ׬Ǯ�����6ǧԪ
    String pBeginData6 = "2018-03-07 00:00:00";//���ʼʱ��
    String pEndData6 = "2018-03-10 23:59:59";//��ر�ʱ��
    int pGroupId7 = 5577346;//ɹ���Σ�����������������
    /*
     * ��ţ����ز���
     */
    // ��ţ�ƹ�Կ
    String accessKey = "2caeYdL9QSwjhpJc2v05LLgaOrk_Mc_HterAtD28";
    // ��ţ����Կ
    String secretKey = "XQYY6AE3rhhp-ep9-xwOOUc2noRyAvXu8uLjkTMT";
    // ��ţ�ƹ����ռ���
    String openBucket = "yiniantest";
    // ��ţ��˽�пռ���
    String privateBucket = "yinian";
    // ��ţ�ƹ����ռ��ַ
    String qiniuOpenAddress = "http://7xlmtr.com1.z0.glb.clouddn.com/";
    // ��ţ��˽�пռ��ַ
    String qiniuPrivateAddress = "https://photo.zhuiyinanian.com/";//20171110 �޸�   http://7xpend.com1.z0.glb.clouddn.com/
    // by lk ���������ר����ţ�ƹ����ռ��ַ
    String personalQiniuPrivateAddress = "http://7xpend.com1.z0.glb.clouddn.com/";
    // ͼƬ����ͼ����
    String pictureThumbnailPara = "?imageView2/2/w/300";

    /**
     * ֧������ز���
     */
    // ֧������Կ
    String AlipayOpenKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAzM4psH+FX5T3o/fV8QttXjeNs03LR17lphyWlrmfWIBQ2qMiEY4/mw2MSK+Ul2QN2txz6qS2o8/p2FYxbIoLP/VbK2KT3r8Mu+UsVP8XltKZV/QEkSQwHq4cJA0WbAInhaJ39wGjfohuubfVvUkFzEDKIm3RiTRF8lexv6LLhM7vVZglvj/Xo9V6TG7983OY+8WAL/KXB0Iwn6brux5SvDOYSJb5IK6XDVLzceDV4WAq2RD4Dblg7L/jYSebHJO3C6ODEspnDoYlekUndqg7gMzq5mGQ3vx/m/Uq9eY6TCawBArOfC+z3MAA0FfLXQkHWXBaMhmIud00YVrqN+RVnwIDAQAB";
    // ֧��������
    String AlipayGateWay = "https://openapi.alipay.com/gateway.do";
    // Ӧ�ù�Կ
    String AliAppOpenKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAku4+PUHmAT2fuLygLeYud38cvmjVE+VclJnaTdABbjFlKMV/PQ3fqCM+UcVkn6qljkRupNlmSkD83GiJgHXgJ0JhKhT3Q3TibtHyrUZe7J9sLJQz5mePVVs1X+8VTpO8HsexsLesWjZ8tFgkcvfBvFOVJJqxBniOIRbOIb17o8MMUtJ5wEjgPl/4MlFzrDFozrTS8tE96eeWAL3uHoXYYIT4IHLi2GSjojmZo7yczMVQf0c71wOZgsfzrzBSuSBAjKb1XCx50aCdWEidieeINuIEvhCUjSlhpEOyWkcAu3LnyGg8+uFQ3tumpFNPTFyzJvYIoEk1EnoRLuHoIztlXwIDAQAB";
    // Ӧ��˽Կ
    String AliAppSecretKey = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCS7j49QeYBPZ+4vKAt5i53fxy+aNUT5VyUmdpN0AFuMWUoxX89Dd+oIz5RxWSfqqWORG6k2WZKQPzcaImAdeAnQmEqFPdDdOJu0fKtRl7sn2wslDPmZ49VWzVf7xVOk7wex7Gwt6xaNny0WCRy98G8U5UkmrEGeI4hFs4hvXujwwxS0nnASOA+X/gyUXOsMWjOtNLy0T3p55YAve4ehdhghPggcuLYZKOiOZmjvJzMxVB/RzvXA5mCx/OvMFK5IECMpvVcLHnRoJ1YSJ2J54g24gS+EJSNKWGkQ7JaRwC7cufIaDz64VDe26akU09MXLMm9gigSTUSehEu4egjO2VfAgMBAAECggEATU8j2nF5BwAYsUg2c9AgdOmiAyH/s2dEbkABmF2w0v0Uf/5is4pAOyTIHphhGI3ebFvfZ8enxbwqEALFDp13ItEPdOfxLkIq/ipjqP5U5eYAMfiM0ld9GGlUTTTsHhsnqX5jJVuPSSxQM0gJbaClBGVqHQdzy7bU+d4Y+bHMg15J5/1qR42IzTSdIL+QrbDit4zAipaFbklnZd/T9InO0xUiNHh6W88ZsweFspECLPLG7B3zjL0Wu6/o/5wMoyl59ym9F+gMxwgMaAnXC7IUqWexFuvOqMlMAARxMzlNadY/5UQxVZ0s7qYr3cfNSP8RL/WAlmAlQMiqU10mulprYQKBgQDPdBaAbXDhIi6eSqozmKoDNyS/zlf0xQ5N85Ks5A2Mhf+H85/8je5pAfnJCoCCeoQ8QYuu+ypwCNJ6Pte6BUSfT9GjdDSkksn7sFo9rZdIwd+mXFa7izYBFCJX+pCQlms9/GTU9ACYwbtZSUNf17qRvNS8zxDqYZbZJOSWJD0h8QKBgQC1UGowkMDgLxODCc5GWuQgWHxGQPs0ZTr/b4yog9t+Dh40v9PpFmqi7nV8DdKYeCdg2YKgOQWCWaM925l4+JaG2whKdmkLCe3hSTbvfNzibm+6KNJ53YWOaxx3aVKrmh2SxXysDc7Yf5MK+oJDgOy9ZiF//mG6QjJ2GHhZapCsTwKBgFK1tPb1K0kHSNDhceRqXHY1uRgj25uCKUbY8sStk91P22ZHsqSNrdz95anK3xumXyxq8ZcNGolaaxhED9xhl09xEmL5xLdMsuz/DauDl0pxyYpZTFcvlJNvhJXRF2weHJm1FDp3IfnFW72oYQ1IRGEwiRzR7auUxwDCHcTio+DxAoGAemkeKEeyUCi4TaHTJ+C5X7UdwfihIJCvskDHgAX1rDcv4YEZOqatu+6uVuCmK9YDD/oJnM6ij4yhD2QxS2DZj5ZVFKC47oD3nz/k3O852Y2h1Nc2spwCTVK3V08+Ryi3ip3WO/reiYAn8COMyrJCIuTO/TDrHesPUnfW2Nd/cTUCgYEAsxONdK8wbT9SrXMSiDU/gto5Zdxsc1X7ex/MdQp9FiSWN+rj28aCl3c+3sDluFvVod7R4Fw1+bqPuL9Pzze0uMdKF77H+ZiCJGINI/7eOw6Ws8/4CCXeKyIfWIkWj2vOgqty6WKZj7oQzV6G3odIlw0mtvfo6u/aNOafYLHoCg0=";
    // appID
    String AliAppID = "2016081801764947";

    /**
     * ������ز���
     */
    // Э��
    String API_PROTOCAL = "https";
    // ������ַ
    String API_HOST = "a1.easemob.com";
    // ��ҵ��
    String API_ORG = "zhuiyinanian";
    // APP��
    String API_APP = "zhuiyinanian";
    // APP�û�ID
    String APP_CLIENT_ID = "YXA6Nc3jcHiTEeaKOF_ShGzbMA";
    // APP�û�����
    String APP_CLIENT_SECRET = "YXA6Iz4qAxRpeyy2l28BpdqeAbo6k9k";
    // APP�ӿ�Ŀ¼
    String APP_IMP_LIB = "httpclient";
    // �û������׺
    String APP_USER_PASSWORD_SUFFIX = "Yi:Nian?365!";
    // Client id
    String imClientID = "YXA6Nc3jcHiTEeaKOF_ShGzbMA";
    // Client secret
    String imClientSecret = "YXA6Iz4qAxRpeyy2l28BpdqeAbo6k9k";

    /*
     * ������ز���
     */
    // AppID��ʶ�����Ӧ��
    String AppID = "J39fL8KyRh5TgiLp71upQ7";
    // AppKey������֤��̨�ĺϷ���
    String AppKey = "e1AbJ5vuTh5ai3nMXSVsC2";
    // AppSecret
    String AppSecret = "MQiPmf4IHf9dCG1QGzUVO5";
    // MasterSecret
    String MasterSecret = "9vou3Mka0dAzlZz7cTYUq";
    // ����������ַ
    String GetuiHost = "http://sdk.open.api.igexin.com/apiex.htm";

    /**
     * С������ز���
     */
    // ����С����appID
    String appID = "wx48b3b26e45ad2e2e";
    // ����С����secretID
    //String secretID = "5d788dbfb6b289130487d30694226d4c";
    String secretID = "66e67cbc568429982766ce1780ff66e6";
    // ����С��������url
    String getWechatSessionKeyUrl = "https://api.weixin.qq.com/sns/jscode2session?appid=" + appID + "&secret="
            + secretID + "&grant_type=authorization_code&js_code=";

    // ����С����appID
    String testAppID = "wx14e7aa6429da2129";
    // ����С����secretID
    String testSecretID = "4cc91dd3c3366f3758f38b50f4c51f78";
    // ����С��������url
    String testGetWechatSessionKeyUrl = "https://api.weixin.qq.com/sns/jscode2session?appid=" + testAppID + "&secret="
            + testSecretID + "&grant_type=authorization_code&js_code=";

    // �����С����appID
    String jjAppID = "wx73ef5bcc7a858a68";
    // ����С����secretID
    String jjSecretID = "735e912da4c0e673fde72d3907e87243";
    // ����С��������url
    String jjGetWechatSessionKeyUrl = "https://api.weixin.qq.com/sns/jscode2session?appid=" + jjAppID + "&secret="
            + jjSecretID + "&grant_type=authorization_code&js_code=";

    // ��ͼС����appID
    String playImageAppID = "wx228859fda649194f";
    // ��ͼС����secretID
    String playImageSecretID = "db88623f1156d295cb86a61e3139f2cb";
    // ��ͼС��������url
    String playImageGetWechatSessionKeyUrl = "https://api.weixin.qq.com/sns/jscode2session?appid=" + playImageAppID
            + "&secret=" + playImageSecretID + "&grant_type=authorization_code&js_code=";

    // ����ģ��ID
    String pushTemplateID = "QsH6csb8yIQk6wQGENPiQB_3pE_g8WBBlPYz5qSh0xw";
    // �ٻ�����ģ��ID
    String callbackPushTemplateID = "hNsPJ284VXzgXw_pG_qTXUTVzB6d2ZSovVkJVmmi8Ig";
    // �ٻ�����ģ��ID by lk �������������Ϣ
    String callbackPushPicCntTemplateID = "QsH6csb8yIQk6wQGENPiQBSRn0wPGiXK69196_5fEpQ";
    //����ģ��ID
    String likePushTemplateID = "FV6jRnU2fRptd8cvKxBq6acFwlYbMStIkg4OOTPYxww";
    //����ģ��ID
    String commentPushTemplateID = "ztWhBJkavmEpxFocyFMF0_Wac-fagDHsHfD4wxyYG5A";
    /**
     * �����ڲ���ز���
     */
    // DES����Կ��
    String DESSecretKey = "YZadZjYx";
    // ��ȡ�����б���select�������
    String selectForComment = "SELECT cid,ceid,A.userid cuid ,A.unickname cunickname,A.upic cpic , B.userid ruid , B.unickname runickname, B.upic rpic ,ccontent,ctime";
    // ��ȡ��̬�б���select�������
    String selectForEvent = "select eid,egroupid,elevel,eMain,userid,unickname,upic,etext,eaudio,ecover,ememorytime,ecardstyle,eplace,etype,eStoragePlace,euploadtime,isRecommend,isTopInRecommendGroup,eRecommendGroupID,GROUP_CONCAT(poriginal SEPARATOR \",\") AS url ";
    // ϵͳ�û�ID
    String systemUserID = "10";
    // �����˺�ID
    String superUserID = "3";

    // �ٷ��ռ�typeֵ
    int officialSpaceTypeValue = 5;
    // ������ͷ��
    String bestieGroup = "guimicover.png";
    // ������ͷ��
    String coupleGroup = "qinglvcover.png";
    // ������ͷ��
    String friendGrop = "sidangcover.png";
    // ������ͷ��
    String familyGroup = "jiarencover.png";
    // ������ͷ��
    String otherGroup = "defaultCoverOfSpace.png";
    // �ٷ����ͷ��
    String officialGroup = "other.png";
    // ϵͳ������ͥ�ռ����
    String familySpaceCover = "huanleyijiaren.jpg";
    // ϵͳ�������¿ռ����
    String loverSpaceCover = "qinglvkongjian.jpg";
    // ϵͳ�������ѿռ����
    String friendSpaceCover = "pengyoujuhui.jpg";
    //by lk ����С���򣬻��ּ��˿ռ����
    String simH5familySpaceCover = "simH5huanleyijiaren.png";
    //by lk ����С�������¿ռ����
    String simH5loverSpaceCover = "simH5qinglvkongjian.png";
    //by lk ����С�������ѿռ����
    String simH5friendSpaceCover = "simH5pengyoujuhui.png";

    // �û�Ĭ��ͷ��
    String userDefaultHeadPic = "default.png";
    // �����û�ͷ��
    String anonymousUserPicture = "anonymousUserPicture.png";
    // �û�Ĭ�ϱ���
    String userDefaultBackground = "userBackground1.png";
    // �������Ĭ����ĵ�һ��ͼƬ
    String defaultFirstPicOfGroup = "userBackground1.png";
    // ����logo��ַ
    String yinianLogo = "http://7xlmtr.com1.z0.glb.clouddn.com/yinianlogo.png";
    // ʱ������Ĭ��ͼƬһ
    String timeCompassDefaultPicOne = "http://www.zhuiyinanian.com/yinian/picture/timeCompassDefaultPicOne.jpg";
    // ʱ������Ĭ��ͼƬ��
    String timeCompassDefaultPicTwo = "http://www.zhuiyinanian.com/yinian/picture/timeCompassDefaultPicTwo.png";
    // ʱ������Ĭ��ͼƬһ������ͼ
    String timeCompassDefaultPicOneThumbnail = "http://www.zhuiyinanian.com/yinian/picture/timeCompassDefaultPicOne.jpg";
    // ʱ������Ĭ��ͼƬ��������ͼ
    String timeCompassDefaultPicTwoThumbnail = "http://www.zhuiyinanian.com/yinian/picture/timeCompassDefaultPicTwo.png";
    // ���������û�����Ƭʱ����ʾ��Ƭ
    String promptPhotoInPeep = "http://7xlmtr.com1.z0.glb.clouddn.com/promptPhotoInPeep.png";
    // �ص�ռλͼ
    String placeBitmap = qiniuOpenAddress + "didianzhanweitu.png";
    // ����ռλͼ
    String audioBitmap = qiniuOpenAddress + "shengyinzhanweitu.png";
    // ����ռλͼ
    String textBitmap = qiniuOpenAddress + "wenzizhanweitu.png";

    // ���������ͽӿ�����
    String TodayMemoryPushCode = "Yi3Nian6Push5Code:By?Zad!";

    // У԰������Ƽ���
    String SchoolAlbumsName = "'�人��ѧ','���пƼ���ѧ','�人����ѧ','����ũҵ��ѧ','���������ѧ','���ϲƾ�������ѧ','����ʦ����ѧ','�й����ʴ�ѧ'";
    // ����������Ƽ���
    String OfficialAlbumsName = "'����С���','�������','Ů�����','��ʳ���','ʱ��Ӱ���ڳ��','�᲻��ɾ������Ƭ','��ҵ����','�ֹ�����','�Һͳ�����ճ�','��Ӱ�ӻ���','��Լ���˻� ','Kiss Me'";
    // ��ռ�ID
    String ActivitySpaceID = "226228, 341765, 341683, 337178, 224840, 224399, 223768, 228759, 291717, 226793, 241232, 237426, 187734, 225280, 231218, 110117, 14934, 145339, 168248, 162795, 170644, 182326, 177021, 236954, 177593, 182209, 182922, 196231, 277223, 341804";

    // ���̹����LOMO��ͼƬԤ���ؽӿ�
    String LomoCardPictureDownload = "http://wx.zhuiyinanian.com/shop/getOrderPic";
    // ����Ƭ��״�����·�������ڴ洢��������
    String postcardBottomSaveRelativePath = "/alidata/www/zhuiyinanian/yinian/picture/postcardBottom/";
    // ����Ƭ��״�ž���·�������ڷ���
    String postcardBottomAbsolutePath = "http://picture.zhuiyinanian.com/yinian/picture/postcardBottom/";
    // ����Ƭ�����ʷ�
    double postcardMailFee = 5.0;
    // С�����¼���ܽӿ�
    String smallAppLoginDecode = "http://192.168.0.151/getUnitidFromSmallApp";
    // С�����ά��洢�ļ��з�����·��
    String smallAppQRCodeSaveServerPath = "/alidata/www/zhuiyinanian/yinian/smallAppQRCode/";
    // �������ļ���ʱ�洢�ļ���
    String tempPictureServerSavePath = "/tempPicture/";
    // ͼƬ�洢�ļ��б���·��
    //String pictureSaveLocalPath = "/Users/liukai/Documents/list3/";
    //String pictureSaveLocalPath = "D:/leiyu/list1/";
    //String pictureSaveLocalPath = "/Users/liukai/Documents/list3/";
    //String pictureSaveLocalPath = "D:\\leiyu\\list1";
    String pictureSaveLocalPath = "/Users/liukai/Documents/list4/";
    //String pictureSaveLocalPath = "~/Desktop/";
    // �ز�ͼƬ·��
    String materialPath = "http://picture.zhuiyinanian.com/yinian/basic/";

    /**
     * Ǯ��ϵͳ����
     */
    String keyOfSendRedEnvelop = "�����";
    String keyOfTopUp = "��ֵ";
    String keyOfWithdraw = "����";
    String keyOfGrabRedEnvelop = "�պ��";
    String keyOfSystemReward = "ϵͳ����";
    String keyOfRefund = "�˿�";

    /*
     * ʵʱ���Ϳ���
     */
    boolean canPublish = true;
    /**
     * ΢�ſ���ƽ̨��ز���
     */
    // �����appID
    String wechatServeAppID = "wx19ce39e964897e9f";
    // �����secret
    String wechatServeSecretID = "d0b05a935ea19d512d4d8f3adcf220e9";
    // �����accessToken��ȡ����
    String getServeAccessToken = "https://api.weixin.qq.com/sns/oauth2/access_token?appid=" + wechatServeAppID
            + "&secret=" + wechatServeSecretID + "&grant_type=authorization_code&code=";

    // PC��appID
    String wechatPCAppID = "wx0f21d36c30ed780d";
    // PC��secret
    String wechatPCSecretID = "511811af4ea49f80c20ad6a210375ac1";
    // PC��accessToken��ȡ����
    String getPCAccessToken = "https://api.weixin.qq.com/sns/oauth2/access_token?appid=" + wechatPCAppID + "&secret="
            + wechatPCSecretID + "&grant_type=authorization_code&code=";
    // ��ȡ�û���Ϣurl
    String getWechatUserInfoUrl = "https://api.weixin.qq.com/sns/userinfo";

}
