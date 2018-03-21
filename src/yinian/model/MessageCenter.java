package yinian.model;

import com.jfinal.plugin.activerecord.Model;
import redis.clients.jedis.Jedis;
import yinian.utils.RedisUtils;

/**
 * 消息中心对象
 *
 * @author yu_chen
 * @date 2018/3/16 16:06
 */
public class MessageCenter extends Model<MessageCenter> {

    /**
     * 缓存实例对象
     */
    private Jedis jedis = RedisUtils.getRedis();

    /**
     * 静态实例对象
     */
    public static final MessageCenter dao = new MessageCenter();

    /**
     * 组装key的方法
     *
     * @param userId
     * @param msgType
     * @return
     */
    private String getKeyStr(String userId, String msgType) {
        return userId + "-" + msgType;
    }

    /**
     * 通过用户ID查询消息记录
     * 消息通知类型  dz---点赞通知  pl---评论通知 jf---积分动态  tz--通知
     *
     * @param userId 用户ID
     * @return 0, 1
     */
    public int selectSingleTypeStatusByUserId(String userId, String msgType) {
        //从缓存中取得消息的状态
        if (jedis == null) {
            return 0;
        }
        String msgStatusStr = jedis.get(getKeyStr(userId, msgType));
        //如果是已读状态
        return msgStatusStr == null ? 0 : 1;
    }

    /**
     * 查询总的消息状态通过用户ID
     *
     * @param userId
     * @return
     */
    public int selectAllTypeStatusByUserId(String userId) {
        return this.selectSingleTypeStatusByUserId(userId, "dz")
                + this.selectSingleTypeStatusByUserId(userId, "pl")
                + this.selectSingleTypeStatusByUserId(userId, "jf")
                + this.selectSingleTypeStatusByUserId(userId, "tz");
    }

    /**
     * 有新消息
     * 设置一下状态
     *
     * @param userId  用户ID
     * @param msgType 消息类型  dz---点赞通知  pl---评论通知 jf---积分动态  tz--通知
     */
    public void newMsg(String userId, String msgType) {
        //如果已经存在这个Key就不需要重复设置
        jedis.set(getKeyStr(userId, msgType), "1", "NX");
    }

    /**
     * 读取消息
     * 如果已读就将这个key移除
     *
     * @param userId
     * @param msgType
     */
    public void readMsg(String userId, String msgType) {
        jedis.move(getKeyStr(userId, msgType), 0);
    }


}
