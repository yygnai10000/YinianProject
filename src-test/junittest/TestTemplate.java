package junittest;
import com.jfinal.plugin.activerecord.Record;
import org.junit.Test;
import yinian.model.Comment;

import java.util.List;

/**
 *  测试类使用模板
 * @author 杨岳
 * @date 2018/3/2015:35
 */
public class TestTemplate {
    /**
     *  测试默认config中的是数据库配置
     */
    @Test
    public void testmodel(){
        //获取测试工具对象，并执行开启测试，加载ActiveRecordPlugin插件
        BaseTestJunit.baseTestJunit.testStartAll();
        //下面就是测试代码
        List<Record> records = Comment.dao.GetSingleEventComments(1);
        System.out.println(records);
    }

    /**
     * 测试自定义数据库配置
     */
    @Test
    public void  testCommentsHaveParameter(){
        //获取测试工具对象，并执行开启测试，加载ActiveRecordPlugin插件，但是需要在testStartAll方法中传入参数 第一个url 第二个数据账号第三个数据密码
        BaseTestJunit.baseTestJunit.testStartAll("jdbc:mysql://47.97.24.114:3306/yinian?characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&noAccessToProcedureBodies=true","querier","yn@!032018");
        //下面是测试代码
        List<Record> records = Comment.dao.GetSingleEventComments(1);
        System.out.println(records);
    }
}
