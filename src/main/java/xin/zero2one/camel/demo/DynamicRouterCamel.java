package xin.zero2one.camel.demo;

import org.apache.camel.Properties;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.ModelCamelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by zhoujundong on 2018/9/5.
 */
public class DynamicRouterCamel {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicRouterCamel.class);

    public static void main(String[] args) throws Exception {
        // 这是camel上下文对象，整个路由的驱动全靠它了。
        ModelCamelContext camelContext = new DefaultCamelContext();
        // 启动route
        camelContext.start();
        // 将我们编排的一个完整消息路由过程，加入到上下文中
        DynamicRouterCamel dynamicRouterCamel = new DynamicRouterCamel();
        camelContext.addRoutes(dynamicRouterCamel.new DirectRouteA());
        camelContext.addRoutes(dynamicRouterCamel.new DirectRouteB());
        camelContext.addRoutes(dynamicRouterCamel.new DirectRouteC());

        // 通用没有具体业务意义的代码，只是为了保证主线程不退出
        synchronized (DynamicRouterCamel.class) {
            DynamicRouterCamel.class.wait();
        }
    }


    /**
     * 第一个路由，主要用于定义整个路由的起点
     * 通过Http协议接收处理请求
     * @author yinwenjie
     */
    public class DirectRouteA extends RouteBuilder {

        /* (non-Javadoc)
         * @see org.apache.camel.builder.RouteBuilder#configure()
         */
        @Override
        public void configure() throws Exception {
            from("jetty:http://0.0.0.0:8282/dynamicRouterCamel")
                    // 使用dynamicRouter，进行“动态路由”循环，
                    // 直到指定的下一个元素为null为止
                    .dynamicRouter().method(this, "doDirect")
                    .process(new MulticastCamel.OtherProcessor());
        }

        /**
         * 该方法用于根据“动态循环”的次数，确定下一个执行的Endpoint
         * @param properties 通过注解能够获得的Exchange中properties属性，可以进行操作，并反映在整个路由过程中
         * @return
         */
        public String doDirect(@Properties Map<String, Object> properties) {
            // 在Exchange的properties属性中，取出Dynamic Router的循环次数
            AtomicInteger time = (AtomicInteger)properties.get("time");
            if(time == null) {
                time = new AtomicInteger(0);
                properties.put("time", time);
            } else {
                time = (AtomicInteger)time;
            }
            LOGGER.info("这是Dynamic Router循环第：【" + time.incrementAndGet() + "】次执行！执行线程：" + Thread.currentThread().getName());

            // 第一次选择DirectRouteB
            if(time.get() == 1) {
                return "direct:directRouteB";
            }
            // 第二次选择DirectRouteC
            else if(time.get() == 2) {
                return "direct:directRouteC";
            }
            // 第三次选择一个Log4j-Endpoint执行
            else if(time.get() == 3) {
                return "log:DirectRouteA?showExchangeId=true&showProperties=ture&showBody=false";
            }

            // 其它情况返回null，终止 dynamicRouter的执行
            return null;
        }
    }

    /**
     * 这是另一条路由分支
     * @author yinwenjie
     */
    public class DirectRouteB extends RouteBuilder {
        @Override
        public void configure() throws Exception {
            from("direct:directRouteB")
                    .to("log:DirectRouteB?showExchangeId=true&showProperties=ture&showBody=false");
        }
    }

    /**
     * 这是另一条路由分支
     * @author yinwenjie
     */
    public class DirectRouteC extends RouteBuilder {
        @Override
        public void configure() throws Exception {
            from("direct:directRouteC")
                    .to("log:DirectRouteC?showExchangeId=true&showProperties=ture&showBody=false");
        }
    }

}
