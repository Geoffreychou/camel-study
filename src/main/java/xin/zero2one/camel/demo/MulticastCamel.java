package xin.zero2one.camel.demo;

import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.MulticastDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by zhoujundong on 2018/9/5.
 */
public class MulticastCamel extends RouteBuilder{

    private static final Logger LOGGER = LoggerFactory.getLogger(MulticastCamel.class);

    public static void main(String[] args) throws Exception {
        // 这是camel上下文对象，整个路由的驱动全靠它了。
        ModelCamelContext camelContext = new DefaultCamelContext();
        // 启动route
        camelContext.start();
        // 将我们编排的一个完整消息路由过程，加入到上下文中
        camelContext.addRoutes(new MulticastCamel());

        // 通用没有具体业务意义的代码，只是为了保证主线程不退出
        synchronized (MulticastCamel.class) {
            MulticastCamel.class.wait();
        }
    }

    @Override
    public void configure() throws Exception {
        // 这个线程池用来进行multicast中各个路由线路的并发执行
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        MulticastDefinition multicastDefinition = from("jetty:http://0.0.0.0:8282/multicastCamel").multicast();

        // multicast 中的消息路由可以顺序执行也可以并发执行
        // 这里我们演示并发执行
        multicastDefinition.setParallelProcessing(true);
        // 为并发执行设置一个独立的线程池
        multicastDefinition.setExecutorService(executorService);

        // 注意，multicast中各路由路径的Excahnge都是基于上一路由元素的excahnge复制而来
        // 无论前者Excahnge中的Pattern如何设置，其处理结果都不会反映在最初的Excahnge对象中
        multicastDefinition.to(
                "log:helloworld1?showExchangeId=true"
                ,"log:helloworld2?showExchangeId=true")
                // 一定要使用end，否则OtherProcessor会被做为multicast中的一个分支路由
                .end()
                // 所以您在OtherProcessor中看到的Excahnge中的Body、Header等属性内容
                // 不会有“复制的Exchange”设置的任何值的痕迹
                .process(new OtherProcessor());
    }

    /**
     * 另一个处理器
     * @author yinwenjie
     */
    public static class OtherProcessor implements Processor {
        /* (non-Javadoc)
         * @see org.apache.camel.Processor#process(org.apache.camel.Exchange)
         */
        @Override
        public void process(Exchange exchange) throws Exception {
            Message message = exchange.getIn();
            LOGGER.info("OtherProcessor中的exchange" + exchange);
            String body = message.getBody().toString();

            // 存入到exchange的out区域
            if(exchange.getPattern() == ExchangePattern.InOut) {
                Message outMessage = exchange.getOut();
                outMessage.setBody(body + " || 被OtherProcessor处理");
            }
        }
    }

}
