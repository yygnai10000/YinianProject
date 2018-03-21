package yinian.common;

public interface CommonParam {
    /*
     * 1000w照片活动相关配置
     */
    int pGroupId = 5268248;//活动相册ID
    String pBeginData = "2018-02-12 00:00:00";//活动开始时间
    String pEndData = "2018-02-22 23:59:59";//活动关闭时间
    boolean pOpenBanner = true;//活动时所有相册banner是否开启
    String pAdvId = "38";//groupadvertisements.id
    String pAdvListId = "11";//advertisementsmessage.advertisementsid
    boolean pOpenJoinGroup = false;//新用户加入相册
    //boolean pGroupAddToTop=true;//活动相册置顶
    /*
     * 1000w照片活动相关配置end
     */
    int pGroupId2 = 5348798;//活动相册ID
    String pBeginData2 = "2018-02-17 00:00:00";//活动开始时间
    String pEndData2 = "2018-02-22 23:59:59";//活动关闭时间
    /*
     * 1000w照片第3波
     */
    int pGroupId3 = 5386332;//活动相册ID
    String pBeginData3 = "2018-02-23 00:00:00";//活动开始时间
    String pEndData3 = "2018-02-26 23:59:59";//活动关闭时间
    /*
     * 1000w照片第4波
     */
    int pGroupId4 = 5416927;//活动相册ID
    String pBeginData4 = "2018-02-27 00:00:00";//活动开始时间
    String pEndData4 = "2018-03-02 23:59:59";//活动关闭时间
    /*
     * 1000w照片第二波end
     */
    int pGroupId5 = 5460577;//晒出爱宠，领千元现金
    int pGroupId6 = 5518246;//上传照片赚钱，最高6千元
    String pBeginData6 = "2018-03-07 00:00:00";//活动开始时间
    String pEndData6 = "2018-03-10 23:59:59";//活动关闭时间
    int pGroupId7 = 5577346;//晒出游，秀出你的最美旅行照
    /*
     * 七牛云相关参数
     */
    // 七牛云公钥
    String accessKey = "2caeYdL9QSwjhpJc2v05LLgaOrk_Mc_HterAtD28";
    // 七牛云密钥
    String secretKey = "XQYY6AE3rhhp-ep9-xwOOUc2noRyAvXu8uLjkTMT";
    // 七牛云公开空间名
    String openBucket = "yiniantest";
    // 七牛云私有空间名
    String privateBucket = "yinian";
    // 七牛云公开空间地址
    String qiniuOpenAddress = "http://7xlmtr.com1.z0.glb.clouddn.com/";
    // 七牛云私有空间地址
    String qiniuPrivateAddress = "https://photo.zhuiyinanian.com/";//20171110 修改   http://7xpend.com1.z0.glb.clouddn.com/
    // by lk 个人云相册专用七牛云公开空间地址
    String personalQiniuPrivateAddress = "http://7xpend.com1.z0.glb.clouddn.com/";
    // 图片缩略图参数
    String pictureThumbnailPara = "?imageView2/2/w/300";

    /**
     * 支付宝相关参数
     */
    // 支付宝公钥
    String AlipayOpenKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAzM4psH+FX5T3o/fV8QttXjeNs03LR17lphyWlrmfWIBQ2qMiEY4/mw2MSK+Ul2QN2txz6qS2o8/p2FYxbIoLP/VbK2KT3r8Mu+UsVP8XltKZV/QEkSQwHq4cJA0WbAInhaJ39wGjfohuubfVvUkFzEDKIm3RiTRF8lexv6LLhM7vVZglvj/Xo9V6TG7983OY+8WAL/KXB0Iwn6brux5SvDOYSJb5IK6XDVLzceDV4WAq2RD4Dblg7L/jYSebHJO3C6ODEspnDoYlekUndqg7gMzq5mGQ3vx/m/Uq9eY6TCawBArOfC+z3MAA0FfLXQkHWXBaMhmIud00YVrqN+RVnwIDAQAB";
    // 支付宝网关
    String AlipayGateWay = "https://openapi.alipay.com/gateway.do";
    // 应用公钥
    String AliAppOpenKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAku4+PUHmAT2fuLygLeYud38cvmjVE+VclJnaTdABbjFlKMV/PQ3fqCM+UcVkn6qljkRupNlmSkD83GiJgHXgJ0JhKhT3Q3TibtHyrUZe7J9sLJQz5mePVVs1X+8VTpO8HsexsLesWjZ8tFgkcvfBvFOVJJqxBniOIRbOIb17o8MMUtJ5wEjgPl/4MlFzrDFozrTS8tE96eeWAL3uHoXYYIT4IHLi2GSjojmZo7yczMVQf0c71wOZgsfzrzBSuSBAjKb1XCx50aCdWEidieeINuIEvhCUjSlhpEOyWkcAu3LnyGg8+uFQ3tumpFNPTFyzJvYIoEk1EnoRLuHoIztlXwIDAQAB";
    // 应用私钥
    String AliAppSecretKey = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCS7j49QeYBPZ+4vKAt5i53fxy+aNUT5VyUmdpN0AFuMWUoxX89Dd+oIz5RxWSfqqWORG6k2WZKQPzcaImAdeAnQmEqFPdDdOJu0fKtRl7sn2wslDPmZ49VWzVf7xVOk7wex7Gwt6xaNny0WCRy98G8U5UkmrEGeI4hFs4hvXujwwxS0nnASOA+X/gyUXOsMWjOtNLy0T3p55YAve4ehdhghPggcuLYZKOiOZmjvJzMxVB/RzvXA5mCx/OvMFK5IECMpvVcLHnRoJ1YSJ2J54g24gS+EJSNKWGkQ7JaRwC7cufIaDz64VDe26akU09MXLMm9gigSTUSehEu4egjO2VfAgMBAAECggEATU8j2nF5BwAYsUg2c9AgdOmiAyH/s2dEbkABmF2w0v0Uf/5is4pAOyTIHphhGI3ebFvfZ8enxbwqEALFDp13ItEPdOfxLkIq/ipjqP5U5eYAMfiM0ld9GGlUTTTsHhsnqX5jJVuPSSxQM0gJbaClBGVqHQdzy7bU+d4Y+bHMg15J5/1qR42IzTSdIL+QrbDit4zAipaFbklnZd/T9InO0xUiNHh6W88ZsweFspECLPLG7B3zjL0Wu6/o/5wMoyl59ym9F+gMxwgMaAnXC7IUqWexFuvOqMlMAARxMzlNadY/5UQxVZ0s7qYr3cfNSP8RL/WAlmAlQMiqU10mulprYQKBgQDPdBaAbXDhIi6eSqozmKoDNyS/zlf0xQ5N85Ks5A2Mhf+H85/8je5pAfnJCoCCeoQ8QYuu+ypwCNJ6Pte6BUSfT9GjdDSkksn7sFo9rZdIwd+mXFa7izYBFCJX+pCQlms9/GTU9ACYwbtZSUNf17qRvNS8zxDqYZbZJOSWJD0h8QKBgQC1UGowkMDgLxODCc5GWuQgWHxGQPs0ZTr/b4yog9t+Dh40v9PpFmqi7nV8DdKYeCdg2YKgOQWCWaM925l4+JaG2whKdmkLCe3hSTbvfNzibm+6KNJ53YWOaxx3aVKrmh2SxXysDc7Yf5MK+oJDgOy9ZiF//mG6QjJ2GHhZapCsTwKBgFK1tPb1K0kHSNDhceRqXHY1uRgj25uCKUbY8sStk91P22ZHsqSNrdz95anK3xumXyxq8ZcNGolaaxhED9xhl09xEmL5xLdMsuz/DauDl0pxyYpZTFcvlJNvhJXRF2weHJm1FDp3IfnFW72oYQ1IRGEwiRzR7auUxwDCHcTio+DxAoGAemkeKEeyUCi4TaHTJ+C5X7UdwfihIJCvskDHgAX1rDcv4YEZOqatu+6uVuCmK9YDD/oJnM6ij4yhD2QxS2DZj5ZVFKC47oD3nz/k3O852Y2h1Nc2spwCTVK3V08+Ryi3ip3WO/reiYAn8COMyrJCIuTO/TDrHesPUnfW2Nd/cTUCgYEAsxONdK8wbT9SrXMSiDU/gto5Zdxsc1X7ex/MdQp9FiSWN+rj28aCl3c+3sDluFvVod7R4Fw1+bqPuL9Pzze0uMdKF77H+ZiCJGINI/7eOw6Ws8/4CCXeKyIfWIkWj2vOgqty6WKZj7oQzV6G3odIlw0mtvfo6u/aNOafYLHoCg0=";
    // appID
    String AliAppID = "2016081801764947";

    /**
     * 环信相关参数
     */
    // 协议
    String API_PROTOCAL = "https";
    // 主机地址
    String API_HOST = "a1.easemob.com";
    // 企业名
    String API_ORG = "zhuiyinanian";
    // APP名
    String API_APP = "zhuiyinanian";
    // APP用户ID
    String APP_CLIENT_ID = "YXA6Nc3jcHiTEeaKOF_ShGzbMA";
    // APP用户密码
    String APP_CLIENT_SECRET = "YXA6Iz4qAxRpeyy2l28BpdqeAbo6k9k";
    // APP接口目录
    String APP_IMP_LIB = "httpclient";
    // 用户密码后缀
    String APP_USER_PASSWORD_SUFFIX = "Yi:Nian?365!";
    // Client id
    String imClientID = "YXA6Nc3jcHiTEeaKOF_ShGzbMA";
    // Client secret
    String imClientSecret = "YXA6Iz4qAxRpeyy2l28BpdqeAbo6k9k";

    /*
     * 个推相关参数
     */
    // AppID标识忆年该应用
    String AppID = "J39fL8KyRh5TgiLp71upQ7";
    // AppKey用于验证后台的合法性
    String AppKey = "e1AbJ5vuTh5ai3nMXSVsC2";
    // AppSecret
    String AppSecret = "MQiPmf4IHf9dCG1QGzUVO5";
    // MasterSecret
    String MasterSecret = "9vou3Mka0dAzlZz7cTYUq";
    // 个推主机地址
    String GetuiHost = "http://sdk.open.api.igexin.com/apiex.htm";

    /**
     * 小程序相关参数
     */
    // 忆年小程序appID
    String appID = "wx48b3b26e45ad2e2e";
    // 忆年小程序secretID
    //String secretID = "5d788dbfb6b289130487d30694226d4c";
    String secretID = "66e67cbc568429982766ce1780ff66e6";
    // 忆年小程序请求url
    String getWechatSessionKeyUrl = "https://api.weixin.qq.com/sns/jscode2session?appid=" + appID + "&secret="
            + secretID + "&grant_type=authorization_code&js_code=";

    // 测试小程序appID
    String testAppID = "wx14e7aa6429da2129";
    // 测试小程序secretID
    String testSecretID = "4cc91dd3c3366f3758f38b50f4c51f78";
    // 测试小程序请求url
    String testGetWechatSessionKeyUrl = "https://api.weixin.qq.com/sns/jscode2session?appid=" + testAppID + "&secret="
            + testSecretID + "&grant_type=authorization_code&js_code=";

    // 精简版小程序appID
    String jjAppID = "wx73ef5bcc7a858a68";
    // 测试小程序secretID
    String jjSecretID = "735e912da4c0e673fde72d3907e87243";
    // 测试小程序请求url
    String jjGetWechatSessionKeyUrl = "https://api.weixin.qq.com/sns/jscode2session?appid=" + jjAppID + "&secret="
            + jjSecretID + "&grant_type=authorization_code&js_code=";

    // 玩图小程序appID
    String playImageAppID = "wx228859fda649194f";
    // 玩图小程序secretID
    String playImageSecretID = "db88623f1156d295cb86a61e3139f2cb";
    // 玩图小程序请求url
    String playImageGetWechatSessionKeyUrl = "https://api.weixin.qq.com/sns/jscode2session?appid=" + playImageAppID
            + "&secret=" + playImageSecretID + "&grant_type=authorization_code&js_code=";

    // 推送模板ID
    String pushTemplateID = "QsH6csb8yIQk6wQGENPiQB_3pE_g8WBBlPYz5qSh0xw";
    // 召回推送模板ID
    String callbackPushTemplateID = "hNsPJ284VXzgXw_pG_qTXUTVzB6d2ZSovVkJVmmi8Ig";
    // 召回推送模板ID by lk 隔日推送相册信息
    String callbackPushPicCntTemplateID = "QsH6csb8yIQk6wQGENPiQBSRn0wPGiXK69196_5fEpQ";
    //点赞模板ID
    String likePushTemplateID = "FV6jRnU2fRptd8cvKxBq6acFwlYbMStIkg4OOTPYxww";
    //评论模板ID
    String commentPushTemplateID = "ztWhBJkavmEpxFocyFMF0_Wac-fagDHsHfD4wxyYG5A";
    /**
     * 忆年内部相关参数
     */
    // DES加密钥匙
    String DESSecretKey = "YZadZjYx";
    // 获取评论列表中select部分语句
    String selectForComment = "SELECT cid,ceid,A.userid cuid ,A.unickname cunickname,A.upic cpic , B.userid ruid , B.unickname runickname, B.upic rpic ,ccontent,ctime";
    // 获取动态列表中select部分语句
    String selectForEvent = "select eid,egroupid,elevel,eMain,userid,unickname,upic,etext,eaudio,ecover,ememorytime,ecardstyle,eplace,etype,eStoragePlace,euploadtime,isRecommend,isTopInRecommendGroup,eRecommendGroupID,GROUP_CONCAT(poriginal SEPARATOR \",\") AS url ";
    // 系统用户ID
    String systemUserID = "10";
    // 超级账号ID
    String superUserID = "3";

    // 官方空间type值
    int officialSpaceTypeValue = 5;
    // 闺蜜组头像
    String bestieGroup = "guimicover.png";
    // 情侣组头像
    String coupleGroup = "qinglvcover.png";
    // 死党组头像
    String friendGrop = "sidangcover.png";
    // 家人组头像
    String familyGroup = "jiarencover.png";
    // 其他组头像
    String otherGroup = "defaultCoverOfSpace.png";
    // 官方相册头像
    String officialGroup = "other.png";
    // 系统引导家庭空间封面
    String familySpaceCover = "huanleyijiaren.jpg";
    // 系统引导情侣空间封面
    String loverSpaceCover = "qinglvkongjian.jpg";
    // 系统引导好友空间封面
    String friendSpaceCover = "pengyoujuhui.jpg";
    //by lk 精简小程序，欢乐家人空间封面
    String simH5familySpaceCover = "simH5huanleyijiaren.png";
    //by lk 精简小程序，情侣空间封面
    String simH5loverSpaceCover = "simH5qinglvkongjian.png";
    //by lk 精简小程序，朋友空间封面
    String simH5friendSpaceCover = "simH5pengyoujuhui.png";

    // 用户默认头像
    String userDefaultHeadPic = "default.png";
    // 匿名用户头像
    String anonymousUserPicture = "anonymousUserPicture.png";
    // 用户默认背景
    String userDefaultBackground = "userBackground1.png";
    // 邀请界面默认组的第一张图片
    String defaultFirstPicOfGroup = "userBackground1.png";
    // 忆年logo网址
    String yinianLogo = "http://7xlmtr.com1.z0.glb.clouddn.com/yinianlogo.png";
    // 时光罗盘默认图片一
    String timeCompassDefaultPicOne = "http://www.zhuiyinanian.com/yinian/picture/timeCompassDefaultPicOne.jpg";
    // 时光罗盘默认图片二
    String timeCompassDefaultPicTwo = "http://www.zhuiyinanian.com/yinian/picture/timeCompassDefaultPicTwo.png";
    // 时光罗盘默认图片一的缩略图
    String timeCompassDefaultPicOneThumbnail = "http://www.zhuiyinanian.com/yinian/picture/timeCompassDefaultPicOne.jpg";
    // 时光罗盘默认图片二的缩略图
    String timeCompassDefaultPicTwoThumbnail = "http://www.zhuiyinanian.com/yinian/picture/timeCompassDefaultPicTwo.png";
    // 互看功能用户无照片时的提示照片
    String promptPhotoInPeep = "http://7xlmtr.com1.z0.glb.clouddn.com/promptPhotoInPeep.png";
    // 地点占位图
    String placeBitmap = qiniuOpenAddress + "didianzhanweitu.png";
    // 声音占位图
    String audioBitmap = qiniuOpenAddress + "shengyinzhanweitu.png";
    // 文字占位图
    String textBitmap = qiniuOpenAddress + "wenzizhanweitu.png";

    // 今日忆推送接口密文
    String TodayMemoryPushCode = "Yi3Nian6Push5Code:By?Zad!";

    // 校园相册名称集合
    String SchoolAlbumsName = "'武汉大学','华中科技大学','武汉理工大学','华中农业大学','中南民族大学','中南财经政法大学','华中师范大学','中国地质大学'";
    // 公开相册名称集合
    String OfficialAlbumsName = "'忆年小伙伴','男神相册','女神相册','美食相册','时光影集众筹馆','舍不得删掉的照片','创业达人','手工物语','我和宠物的日常','电影杂货铺','里约奥运会 ','Kiss Me'";
    // 活动空间ID
    String ActivitySpaceID = "226228, 341765, 341683, 337178, 224840, 224399, 223768, 228759, 291717, 226793, 241232, 237426, 187734, 225280, 231218, 110117, 14934, 145339, 168248, 162795, 170644, 182326, 177021, 236954, 177593, 182209, 182922, 196231, 277223, 341804";

    // 电商管理端LOMO卡图片预下载接口
    String LomoCardPictureDownload = "http://wx.zhuiyinanian.com/shop/getOrderPic";
    // 明信片封底存放相对路径，用于存储到服务器
    String postcardBottomSaveRelativePath = "/alidata/www/zhuiyinanian/yinian/picture/postcardBottom/";
    // 明信片封底存放绝对路径，用于访问
    String postcardBottomAbsolutePath = "http://picture.zhuiyinanian.com/yinian/picture/postcardBottom/";
    // 明信片单独邮费
    double postcardMailFee = 5.0;
    // 小程序登录解密接口
    String smallAppLoginDecode = "http://192.168.0.151/getUnitidFromSmallApp";
    // 小程序二维码存储文件夹服务器路径
    String smallAppQRCodeSaveServerPath = "/alidata/www/zhuiyinanian/yinian/smallAppQRCode/";
    // 服务器文件临时存储文件夹
    String tempPictureServerSavePath = "/tempPicture/";
    // 图片存储文件夹本地路径
    //String pictureSaveLocalPath = "/Users/liukai/Documents/list3/";
    //String pictureSaveLocalPath = "D:/leiyu/list1/";
    //String pictureSaveLocalPath = "/Users/liukai/Documents/list3/";
    //String pictureSaveLocalPath = "D:\\leiyu\\list1";
    String pictureSaveLocalPath = "/Users/liukai/Documents/list4/";
    //String pictureSaveLocalPath = "~/Desktop/";
    // 素材图片路径
    String materialPath = "http://picture.zhuiyinanian.com/yinian/basic/";

    /**
     * 钱包系统参数
     */
    String keyOfSendRedEnvelop = "发红包";
    String keyOfTopUp = "充值";
    String keyOfWithdraw = "提现";
    String keyOfGrabRedEnvelop = "收红包";
    String keyOfSystemReward = "系统奖励";
    String keyOfRefund = "退款";

    /*
     * 实时推送开关
     */
    boolean canPublish = true;
    /**
     * 微信开放平台相关参数
     */
    // 服务号appID
    String wechatServeAppID = "wx19ce39e964897e9f";
    // 服务号secret
    String wechatServeSecretID = "d0b05a935ea19d512d4d8f3adcf220e9";
    // 服务号accessToken获取链接
    String getServeAccessToken = "https://api.weixin.qq.com/sns/oauth2/access_token?appid=" + wechatServeAppID
            + "&secret=" + wechatServeSecretID + "&grant_type=authorization_code&code=";

    // PC端appID
    String wechatPCAppID = "wx0f21d36c30ed780d";
    // PC端secret
    String wechatPCSecretID = "511811af4ea49f80c20ad6a210375ac1";
    // PC端accessToken获取链接
    String getPCAccessToken = "https://api.weixin.qq.com/sns/oauth2/access_token?appid=" + wechatPCAppID + "&secret="
            + wechatPCSecretID + "&grant_type=authorization_code&code=";
    // 获取用户信息url
    String getWechatUserInfoUrl = "https://api.weixin.qq.com/sns/userinfo";

}
