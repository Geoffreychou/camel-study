package xin.zero2one.camel.demo;

import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.language.JsonPathExpression;

import static org.apache.camel.model.rest.RestParamType.body;

/**
 * Created by zhoujundong on 2018/9/4.
 */
public class ChoiceCamel extends RouteBuilder {
    public static void main(String[] args) throws Exception {
        // 这是camel上下文对象，整个路由的驱动全靠它了。
        ModelCamelContext camelContext = new DefaultCamelContext();
        // 启动route
        camelContext.start();
        // 将我们编排的一个完整消息路由过程，加入到上下文中
        camelContext.addRoutes(new ChoiceCamel());

        // 通用没有具体业务意义的代码，只是为了保证主线程不退出
        synchronized (ChoiceCamel.class) {
            ChoiceCamel.class.wait();
        }
    }

    @Override
    public void configure() throws Exception {
        // 这是一个JsonPath表达式，用于从http携带的json信息中，提取orgId属性的值
        JsonPathExpression jsonPathExpression = new JsonPathExpression("$.data.orgId");
        jsonPathExpression.setResultType(String.class);

        // 通用使用http协议接受消息
        from("jetty:http://0.0.0.0:8282/choiceCamel")
                // 首先送入HttpProcessor，
                // 负责将exchange in Message Body之中的stream转成字符串
                // 当然，不转的话，下面主要的choice操作也可以运行
                // HttpProcessor中的实现和上文代码片段中的一致，这里就不再重复贴出
                .process(new HttpProcessor())
                // 将orgId属性的值存储 exchange in Message的header中，以便后续进行判断
                .setHeader("orgId", jsonPathExpression)
                .choice()
                // 当orgId == yuanbao，执行OtherProcessor
                // 当orgId == yinwenjie，执行OtherProcessor2
                // 其它情况执行OtherProcessor3
                .when(header("orgId").isEqualTo("yuanbao"))
                .process(new OtherProcessor())
                .when(header("orgId").isEqualTo("yinwenjie"))
                .process(new OtherProcessor2())
                .otherwise()
                .process(new OtherProcessor3())
                // 结束
                .endChoice();
    }

    /**
     * 这个处理器用来完成输入的json格式的转换
     * 和上一篇文章出现的HttpProcessor 内容基本一致。就不再贴出了
     * @author yinwenjie
     */
    public class HttpProcessor implements Processor {
        @Override
        public void process(Exchange exchange) throws Exception {

        }
    }

    /**
     * 另一个处理器OtherProcessor
     * @author yinwenjie
     */
    public class OtherProcessor implements Processor {

        @Override
        public void process(Exchange exchange) throws Exception {
            Message message = exchange.getIn();
            String body = message.getBody().toString();

            // 存入到exchange的out区域
            if(exchange.getPattern() == ExchangePattern.InOut) {
                Message outMessage = exchange.getOut();
                outMessage.setBody(body + " || 被OtherProcessor处理");
            }
        }
    }

    /**
     * 很简单的处理器OtherProcessor2
     * 和OtherProcessor基本相同，就不再重复贴出
     * @author yinwenjie
     */
    public class OtherProcessor2 implements Processor {
        @Override
        public void process(Exchange exchange) throws Exception {
            Message outMessage = exchange.getOut();
            Message message = exchange.getIn();
            String body = message.getBody().toString();
            outMessage.setBody(body + " || 被OtherProcessor2处理");
        }
    }

    /**
     * 很简单的处理器OtherProcessor3
     * 和OtherProcessor基本相同，就不再重复贴出
     * @author yinwenjie
     */
    public class OtherProcessor3 implements Processor {
        @Override
        public void process(Exchange exchange) throws Exception {
            exchange.getIn();
            Message outMessage = exchange.getOut();
            Message message = exchange.getIn();
            String body = message.getBody().toString();
            outMessage.setBody(body + " || 被OtherProcessor3处理");
        }

    }
}
