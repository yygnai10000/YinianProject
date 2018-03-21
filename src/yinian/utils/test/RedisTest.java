package yinian.utils.test;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import redis.clients.jedis.Jedis;
import yinian.utils.RedisUtils;

public class RedisTest {
	public static void main(String[] args) {
//        //连接本地的 Redis 服务
//        Jedis jedis = new Jedis("localhost");
//        System.out.println("连接成功");
//        //查看服务是否运行
//        System.out.println("服务正在运行: "+jedis.ping());
//       // jedis.set("runoobkey", "www.runoob.com1");
//        System.out.println(jedis.get("runoobkey"));
		Jedis jedis = RedisUtils.getRedis();
		if(null!=jedis) {
			//从缓存中读取当前eid的点赞数
			String likeCnt = jedis.get("PublishList_5348798");
			if(null!=likeCnt&&!"".equals(likeCnt)) {
				//点赞成功后缓存点赞数加1
				JSONArray ja=JSONArray.fromObject(likeCnt);
				for(int i=0;i<ja.size();i++){
					JSONObject jo=ja.getJSONObject(i);
					//JSONObject jv=jo.getJSONObject("columns");
					System.out.println(emojiRecovery(jo.toString()));
				}
			}else {
				//当前eid未缓存点赞数，从数据库count点赞数，并同步到缓存
			
				//	jedis.set("likeCnt_12233", "555");
				//	System.out.println("no redis:"+jedis.get("likeCnt_12233"));
				}
			}
			//释放redis
			RedisUtils.returnResource(jedis);
    }
	public static String emojiRecovery(String str) {
        String patternString = "\\[\\[EMOJI:(.*?)\\]\\]";
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(str);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            try {
                matcher.appendReplacement(sb, URLDecoder.decode(matcher.group(1), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        matcher.appendTail(sb);

        return sb.toString();
    }
}
