package xin.zero2one.camel.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Created by zhoujundong on 2018/9/5.
 */
public class SpringXML {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringXML.class);

    public static void main(String[] args) throws Exception {
    /*
     * 这里是测试代码
     * 作为架构师，您应该知道在应用程序中如何进行Spring的加载、如果在Web程序中进行加载、如何在OSGI中间件中进行加载
     *
     * Camel会以SpringCamelContext类作为Camel上下文对象
     * */
        ApplicationContext ap = new ClassPathXmlApplicationContext("META-INF/application-config.xml");
        LOGGER.info("初始化....." + ap);

        // 没有具体的业务含义，只是保证主线程不退出
        synchronized (SpringXML.class) {
            SpringXML.class.wait();
        }
    }

}
