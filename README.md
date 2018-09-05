# Apache-Camel学习

> 根据：架构设计：系统间通信---Apache Camel快速入门整理，作者：[说好不能打脸](https://blog.csdn.net/yinwenjie)

## 1. Camel介绍

> Camel empowers you to define routing and mediation rules in a variety of domain-specific languages, including a Java-based Fluent API, Spring or Blueprint XML Configuration files, and a Scala DSL. This means you get smart completion of routing rules in your IDE, whether in a Java, Scala or XML editor.
>
> Apache Camel uses URIs to work directly with any kind of Transport or messaging model such as HTTP, ActiveMQ, JMS, JBI, SCA, MINA or CXF, as well as pluggable Components and Data Format options. Apache Camel is a small library with minimal dependencies for easy embedding in any Java application. Apache Camel lets you work with the same API regardless which kind of Transport is used - so learn the API once and you can interact with all the Components provided out-of-box.
>
> Apache Camel provides support for Bean Binding and seamless integration with popular frameworks such as CDI, Spring, Blueprint and Guice. Camel also has extensive support for unit testing your routes.

domain-specific languages指代的是DSL（领域特定语言），首先Apache Camel支持DSL，这个问题已经在上一篇文章中说明过了。Apache Camel支持使用JAVA语言和Scala语言进行DSL规则描述，也支持使用XML文件进行的规则描述。这里提一下，JBOSS提供了一套工具“Tools for Apache Camel”可以图形化Apache Camel的规则编排过程。

Apache Camel在编排模式中依托URI描述规则，实现了传输协议和消息格式的转换：HTTP, ActiveMQ, JMS, JBI, SCA, MINA or CXF等等。Camel还可以嵌入到任何java应用程序中：看到了吧，Apache Camel不是ESB中间件服务，它需要依赖于相应的二次开发才能被当成ESB服务的核心部分进行使用。



## 2. Camel要素



### 2.1 EndPoint 控制端点

Apache Camel中关于Endpoint最直白的解释就是，Camel作为系统集成的基础服务组件，在已经编排好的路由规则中，和其它系统进行通信的设定点。这个“其它系统”，可以是存在于本地或者远程的文件系统，可以是进行业务处理的订单系统，可以是消息队列服务，可以是提供了访问地址、访问ip、访问路径的任何服务。Apache Camel利用自身提供的广泛的通信协议支持，使这里的“通信”动作可以采用大多数已知的协议，例如各种RPC协议、JMS协议、FTP协议、HTTP协议。

Camel中的Endpoint控制端点使用URI的方式描述对目标系统的通信。例如以下URI描述了对外部MQ服务的通信，消息格式是Stomp：

```java
// 以下代码表示从名为test的MQ队列中接收消息，消息格式为stomp
// 用户名为username，监听本地端口61613
from("stomp:queue:test?tcp://localhost:61613&login=username")

// 以下代码表示将消息发送到名为test的MQ队列中，消息格式为stomp
to("stomp:queue:test?tcp://localhost:61613&login=username");
```

以上的示例中，请注意“from”部分的说明。它并不是等待某个Http请求匹配描述的URI发送到路由路径上，而是主动向http URI描述的路径发送请求。如果想要达到前者的效果，请使用Jetty/Servlet开头的相关通信方式：<http://camel.apache.org/servlet.html> 和 <http://camel.apache.org/jetty.html>。而通过Apache Camel官网中 <http://camel.apache.org/uris.html> 路径可以查看大部分Camel通过URI格式所支持的Endpoint。



### 2.2 特殊的Endpoint Direct

Endpoint Direct用于在两个编排好的路由间实现Exchange消息的连接，上一个路由中由最后一个元素处理完的Exchange对象，将被发送至由Direct连接的下一个路由起始位置（<http://camel.apache.org/direct.html>）。注意，两个被连接的路由一定要是可用的，并且存在于同一个Camel服务中。以下的例子说明了Endpoint Direct的简单使用方式。

```java
package com.yinwenjie.test.cameltest.helloworld;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.ModelCamelContext;

/**
 * 测试两个路由的连接
 * @author yinwenjie
 */
public class DirectCamel {

    public static void main(String[] args) throws Exception {
        // 这是camel上下文对象，整个路由的驱动全靠它了。
        ModelCamelContext camelContext = new DefaultCamelContext();
        // 启动route
        camelContext.start();
        // 首先将两个完整有效的路由注册到Camel服务中
        camelContext.addRoutes((new DirectCamel()).new DirectRouteA());
        camelContext.addRoutes((new DirectCamel()).new DirectRouteB());

        // 通用没有具体业务意义的代码，只是为了保证主线程不退出
        synchronized (DirectCamel.class) {
            DirectCamel.class.wait();
        }
    }

    /**
     * DirectRouteA 其中使用direct 连接到 DirectRouteB
     * @author yinwenjie
     */
    public class DirectRouteA extends RouteBuilder {

        /* (non-Javadoc)
         * @see org.apache.camel.builder.RouteBuilder#configure()
         */
        @Override
        public void configure() throws Exception {
            from("jetty:http://0.0.0.0:8282/directCamel")
            // 连接路由：DirectRouteB
            .to("direct:directRouteB")
            .to("log:DirectRouteA?showExchangeId=true");
        }
    }

    /**
     * @author yinwenjie
     */
    public class DirectRouteB extends RouteBuilder {
        /* (non-Javadoc)
         * @see org.apache.camel.builder.RouteBuilder#configure()
         */
        @Override
        public void configure() throws Exception {
            from("direct:directRouteB")
            .to("log:DirectRouteB?showExchangeId=true");
        }
    }
}
```

以上代码片段中，我们编排了两个可用的路由（尽管两个路由都很简单，但确实是两个独立的路由）命名为DirectRouteA和DirectRouteB。其中DirectRouteA实例在最后一个Endpoint控制端点（direct:directRouteB）中使用Endpoint Direct将Exchange消息发送到DirectRouteB实例的开始位置。以下是控制台输出的内容：

> [2016-06-26 09:54:38] INFO  qtp231573738-21 Exchange[Id: ID-yinwenjie-240-54473-1466906074572-0-1, ExchangePattern: InOut, BodyType: org.apache.camel.converter.stream.InputStreamCache, Body: [Body is instance of org.apache.camel.StreamCache]] (MarkerIgnoringBase.java:96)
> [2016-06-26 09:54:38] INFO  qtp231573738-21 Exchange[Id: ID-yinwenjie-240-54473-1466906074572-0-1, ExchangePattern: InOut, BodyType: org.apache.camel.converter.stream.InputStreamCache, Body: [Body is instance of org.apache.camel.StreamCache]] (MarkerIgnoringBase.java:96)

从以上执行效果我们可以看到，被连接的两个路由使用的Exchange对象是同一个，也就是说在DirectRouteB路由中如果Exchange对象中的内容发生了变化就会在随后继续执行的DirectRouteA路由中产生影响。Endpoint Direct元素在我们实际使用Camel进行路由编排时，应用频度非常高。**因为它可以把多个已编排好的路由按照业务要求连接起来，形成一个新的路由，保持原有路由的良好重用**。



### 2.3 Exchange和Message消息格式

消息在我们已经编排好的业务路径上进行传递，通过我们自定义的消息转换方式或者Apache Camel提供的消息转换方式进行消息格式转换。那么为了完成这些消息传递、消息转换过程**Camel中的消息必须使用统一的消息描述格式，并且保证路径上的控制端点都能存取消息**

Camel提供的Exchange要素帮助开发人员在控制端点到处理器、处理器到处理器的路由过程中完成消息的统一描述。一个Exchange元素的结构如下图所示：

![Exchage](C:\Users\jundo\Desktop\typora\20160618145837322.jpg)

#### 2.3.1 Exchange中的基本属性

1. ExchangeID

   一个Exchange贯穿着整个编排的路由规则，ExchangeID就是它的唯一编号信息，同一个路由规则的不同实例（对路由规则分别独立的两次执行），ExchangeID不相同。

2. fromEndpoint

   表示exchange实例初始来源的Endpoint控制端点（类的实例），一般来说就是开发人员设置路由时由“from”关键字所表达的Endpoint。

3. properties

   Exchange对象贯穿整个路由执行过程中的控制端点、处理器甚至还有表达式、路由条件判断。为了让这些元素能够共享一些开发人员自定义的参数配置信息，Exchange以K-V结构提供了这样的参数配置信息存储方式。在`org.apache.camel.impl.DefaultExchange`类中，对应properties的实现代码如下所示：

   ```java
   ......
   public Map<String, Object> getProperties() {
       if (properties == null) {
           properties = new ConcurrentHashMap<String, Object>();
       }
       return properties;
   }
   ......
   ```

4. Pattern

   Exchange中的pattern属性非常重要，它的全称是：ExchangePattern（交换器工作模式）。其实现是一个枚举类型：`org.apache.camel.ExchangePattern`。可以使用的值包括：`InOnly`, `RobustInOnly`, `InOut`, `InOptionalOut`, `OutOnly`, `RobustOutOnly`, `OutIn`, `OutOptionalIn`。从Camel官方已公布的文档来看，这个属性描述了Exchange中消息的传播方式。

   例如Event Message类型的消息，其ExchangePattern默认设置为InOnly。Request/Reply Message类型的消息，其ExchangePattern设置为InOut。(http://camel.apache.org/exchange-pattern.html)

5. Exception

   如果在处理器Processor的处理过程中，开发人员需要抛出异常并终止整个消息路由的执行过程，可以通过设置Exchange中的exception属性来实现。


#### 2.3.2 Exchange中的Message

Exchange中还有两个重要属性inMessage和outMessage。这两个属性分别代表Exchange在某个处理元素（处理器、表达式等）上的输入消息和输出消息。

当控制端点和处理器、处理器和处理器间的Message在Exchange中传递时，**Exchange会自动将上一个元素的输出值作为作为这个元素的输入值**进行使用。但是如果在上一个处理器中，开发人员没有在Exchange中设置任何out message内容（即Excahnge中out属性为null），那么上一个处理器中的in message内容将作为这个处理器的in message内容。

这里需要注意一个问题，在DefaultExchange类中关于getOut()方法的实现，有这样的代码片段：

```java
......
public Message getOut() {
    // lazy create
    if (out == null) {
        out = (in != null && in instanceof MessageSupport)
            ? ((MessageSupport)in).newInstance() : new DefaultMessage();
        configureMessage(out);
    }
    return out;
}
......
```

所以，在处理器中对out message属性的赋值，并不需要开发人员明确的“new”一个Message对象。只需要调用getOut()方法，就可以完成out message属性赋值。以下路由代码片段在fromEndpoint后，连续进入两个Processor处理器，且Exchange的ExchangePattern为InOut。我们来观察从第一个处理处理完后，到第二个处理收到消息时Exchange对象中的各个属性产生的变化：

```java
......
from("jetty:http://0.0.0.0:8282/doHelloWorld")
.process(new HttpProcessor())
.process(new OtherProcessor())
......
```

- 第一个HttpProcessor执行末尾时，Exchange中的属性

  ![](C:\Users\jundo\Desktop\typora\20160619084320881.jpg)

  上图显示了当前内存区域中，Exchange对象的id为452，fromEndpoint属性是一个JettyHttpEndpoint的实例，对象id为479。注意两个重要的inMessage和outMessage，它们分别是HttpMessage的实例（对象id467）和DefaultMessage的实例（对象id476），这里说明一下无论是HttpMessage还是DefaultMessage，它们都是org.apache.camel.Message接口的实现。

  outMessage中的body部分存储了一个字符串信息，我们随后验证一下信息在下一个OtherProcessor处理器中的记录方式。

- 第二个OtherProcessor开始执行时，Exchange中的属性

![](C:\Users\jundo\Desktop\typora\20160619085113190.jpg)

可以看到HttpProcessor处理器中outMessage的Message对象作为了这个OtherProcessor处理器的inMessage属性，对象的id编号都是476，说明他们使用的内存区域都是相同的，是同一个对象。Excahnge对象的其它信息也从HttpProcessor处理器原封不动的传递到了OtherProcessor处理器。

每一个Message（无论是inMessage还是outMessage）对象主要包括四个属性：MessageID、Header、Body和Attachment。

- MessageID

  在系统开发阶段，提供给开发人员使用的标示消息对象唯一性的属性，这个属性可以没有值。

- Header

  消息结构中的“头部”信息，在这个属性中的信息采用K-V的方式进行存储，并可以随着Message对象的传递将信息带到下一个参与路由的元素中。

  主要注意的是在`org.apache.camel.impl.DefaultMessage`中对headers属性的实现是一个名叫`org.apache.camel.util.CaseInsensitiveMap`的类。看这个类的名字就知道：headers属性的特点是忽略大小写。也就是说：

  ```java
  ......
  outMessage.setHeader("testHeader", "headerValue");
  outMessage.setHeader("TESTHEADER", "headerValue");
  outMessage.setHeader("testheader", "HEADERVALUE");
  ......
  ```

  以上代码片段设置后，Message中的Headers属性中只有一个K-V键值对信息，且以最后一次设置的testheader为准。

- Body

  Message的业务消息内容存放在这里

- Attachment

  Message中使用attachment属性存储各种文件内容信息，以便这些文件内容在Camel路由的各个元素间进行流转。attachment同样使用K-V键值对形式进行文件内容的存储。但不同的是，这里的V是一个javax.activation.DataHandler类型的对象。


### 2.4 Processor 处理器

Camel中另一个重要的元素是Processor处理器，它用于接收从控制端点、路由选择条件又或者另一个处理器的Exchange中传来的消息信息，并进行处理。Camel核心包和各个Plugin组件都提供了很多Processor的实现，开发人员也可以通过实现org.apache.camel.Processor接口自定义处理器（后者是通常做法）。

既然是做编码，那么我们自然可以在自定义的Processor处理器中做很多事情。这些事情可能包括处理业务逻辑、建立数据库连接去做业务数据存储、建立和某个第三方业务系统的RPC连接，**但是我们一般不会那样做——那是Endpoint的工作**。Processor处理器中**最主要的工作是进行业务数据格式的转换和中间数据的临时存储**。这样做是因为Processor处理器是Camel编排的路由中，主要进行Exchange输入输出消息交换的地方。

不过开发人员当然可以在Processor处理器中连接数据库。例如开发人员需要根据上一个Endpoint中携带的“订单编号前缀”信息，在Processor处理器中连接到一个独立的数据库中（或者缓存服务中）查找其对应的路由信息，以便**动态决定**下一个路由路径。由于Camel支持和JAVA语言的Spring框架无缝集成，所以要在Processor处理器中操作数据库只需要进行非常简单的配置。

以下代码片段是自定义的Processor处理器实现，其中的process(Exchange exchange)方法是必须进行实现的：

```java
// 一个自定义处理器的实现
// 就是我们上文看到过的处理器实现了
public class OtherProcessor implements Processor {
    ......
    @Override
    public void process(Exchange exchange) throws Exception {
        Message message = exchange.getIn();
        String body = message.getBody().toString();
        //===============
        // 您可以在这里进行数据格式转换
        // 并且将结果存储到out message中
        //===============

        // 存入到exchange的out区域
        if(exchange.getPattern() == ExchangePattern.InOut) {
            Message outMessage = exchange.getOut();
            outMessage.setBody(body + " || other out");
        }
    }
    ......
}
```

注意，处理器Processor是和控制端点平级的概念。要看一个URI对应的实现是否是一个控制端点，最根本的就是看这个实现类是否实现了org.apache.camel.Endpoint接口；而要看一个路由中的元素是否是Processor处理器，最根本的就是看这个类是否实现了org.apache.camel.Processor接口。



### 2.5 Routing路由条件

在控制端点和处理器之间、处理器和处理器之间，Camel允许开发人员进行路由条件设置。例如开发人员可以拥有当Exchange In Message的内容为A的情况下将消息送入下一个处理器A，当Exchange In Message的内容为B时将消息送入下一个处理器B的处理能力。又例如，无论编排的路由中上一个元素的处理消息如何，都将携带消息的Exchange对象**复制** 多份，分别送入下一处理器X、Y、Z。开发人员甚至还可以通过路由规则完成Exchange到多个Endpoint的负载传输。

Camel中支持的路由规则非常丰富，包括：Message Filter、Based Router、Dynamic Router、Splitter、Aggregator、Resequencer等等。在Camel的官方文档中使用了非常形象化的图形来表示这些路由功能（<http://camel.apache.org/enterprise-integration-patterns.html>）：

Message Routing

| ![img](http://www.eaipatterns.com/img/ContentBasedRouterIcon.gif) | [Content Based Router](http://camel.apache.org/manual/content-based-router.html) | How do we handle a situation where the implementation of a single logical function (e.g., inventory check) is spread across multiple physical systems? |
| ------------------------------------------------------------ | ------------------------------------------------------------ | ------------------------------------------------------------ |
| ![img](http://www.eaipatterns.com/img/MessageFilterIcon.gif) | [Message Filter](http://camel.apache.org/manual/message-filter.html) | How can a component avoid receiving uninteresting messages?  |
| ![img](http://www.eaipatterns.com/img/DynamicRouterIcon.gif) | [Dynamic Router](http://camel.apache.org/manual/dynamic-router.html) | How can you avoid the dependency of the router on all possible destinations while maintaining its efficiency? |
| ![img](http://www.eaipatterns.com/img/RecipientListIcon.gif) | [Recipient List](http://camel.apache.org/manual/recipient-list.html) | How do we route a message to a list of (static or dynamically) specified recipients? |
| ![img](http://www.eaipatterns.com/img/SplitterIcon.gif)      | [Splitter](http://camel.apache.org/manual/splitter.html)     | How can we process a message if it contains multiple elements, each of which may have to be processed in a different way? |
| ![img](http://www.eaipatterns.com/img/AggregatorIcon.gif)    | [Aggregator](http://camel.apache.org/manual/aggregator2.html) | How do we combine the results of individual, but related messages so that they can be processed as a whole? |
| ![img](http://www.eaipatterns.com/img/ResequencerIcon.gif)   | [Resequencer](http://camel.apache.org/manual/resequencer.html) | How can we get a stream of related but out-of-sequence messages back into the correct order? |
| ![img](http://www.eaipatterns.com/img/DistributionAggregateIcon.gif) | [Composed Message Processor](http://camel.apache.org/manual/composed-message-processor.html) | How can you maintain the overall message flow when processing a message consisting of multiple elements, each of which may require different processing? |
| ![img](http://cwiki.apache.org/confluence/download/attachments/49204/clear.png) | [Scatter-Gather](http://camel.apache.org/manual/scatter-gather.html) | How do you maintain the overall message flow when a message needs to be sent to multiple recipients, each of which may send a reply? |
| ![img](http://www.eaipatterns.com/img/RoutingTableIcon.gif)  | [Routing Slip](http://camel.apache.org/manual/routing-slip.html) | How do we route a message consecutively through a series of processing steps when the sequence of steps is not known at design-time and may vary for each message? |
| ![img](http://cwiki.apache.org/confluence/download/attachments/49204/clear.png) | [Throttler](http://camel.apache.org/manual/throttler.html)   | How can I throttle messages to ensure that a specific endpoint does not get overloaded, or we don't exceed an agreed SLA with some external service? |
| ![img](http://cwiki.apache.org/confluence/download/attachments/49204/clear.png) | [Sampling](http://camel.apache.org/manual/sampling.html)     | How can I sample one message out of many in a given period to avoid downstream route does not get overloaded? |
| ![img](http://cwiki.apache.org/confluence/download/attachments/49204/clear.png) | [Delayer](http://camel.apache.org/manual/delayer.html)       | How can I delay the sending of a message?                    |
| ![img](http://cwiki.apache.org/confluence/download/attachments/49204/clear.png) | [Load Balancer](http://camel.apache.org/manual/load-balancer.html) | How can I balance load across a number of endpoints?         |
|                                                              | [Hystrix](http://camel.apache.org/manual/hystrix-eip.html)   | To use Hystrix Circuit Breaker when calling an external service. |
|                                                              | [Service Call](http://camel.apache.org/manual/servicecall-eip.html) | To call a remote service in a distributed system where the service is looked up from a service registry of some sorts. |
| ![img](http://cwiki.apache.org/confluence/download/attachments/49204/clear.png) | [Multicast](http://camel.apache.org/manual/multicast.html)   | How can I route a message to a number of endpoints at the same time? |
| ![img](http://cwiki.apache.org/confluence/download/attachments/49204/clear.png) | [Loop](http://camel.apache.org/manual/loop.html)             | How can I repeat processing a message in a loop?             |

#### 2.5.1 Content Based Router 基于内容的路由

它并不是一种单一的路由方式，而是多种基于条件和判断表达式的路由方式。其中可能包括choice语句/方法、when语句/方法、otherwise语句/方法。请看以下示例：

```java
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

```

以上代码片段中，开发人员首先使用JsonPath表达式，从Http中携带的json信息中寻找到orgId这个属性的值，然后将这个值存储在Exchange的header区域（这样做只是为了后续方便判断，您也可以将值存储在Exchange的properties区域，还可以直接使用JsonPath表达式进行判断） 。接下来，通过判断存储在header区域的值，让消息路由进入不同的Processor处理器。由于我们设置的from-jetty-endpoint中默认的Exchange Pattern值为InOut，所以在各个Processor处理器中完成处理后 Out Message的Body内容会以Http响应结果的形式返回到from-jetty-endPoint中。最后我们将在测试页面上看到Processor处理器中的消息值。

Camel中支持绝大多数被开发人员承认和使用的表达式：正则式、XPath、JsonPath等。如果各位读者对JsonPath的语法还不熟悉的话，可以参考Google提供的说明文档（<https://code.google.com/p/json-path/>）。为了测试以上代码片段的工作效果，我们使用Postman工具向指定的地址发送一段json信息，并观察整个路由的执行效果。如下图所示：

![1536077096402](C:\Users\jundo\AppData\Local\Temp\1536077096402.png)

关于路由判断，Camel中提供了丰富的条件判断手段。除了我们在本小节中使用的isEqualTo方式还包括：isGreaterThan、isGreaterThanOrEqualTo、isLessThan、isLessThanOrEqualTo、isNotEqualTo、in（多个值）、contains、regex等等，它们的共同点是这些方法都返回某个实现了org.apache.camel.Predicate接口的类。

#### 2.5.2 Recipient List 接收者列表

在Camel中可能被选择的消息路由路径称为接收者，Camel提供了多种方式向路由中可能成为下一处理元素的多个接收者发送消息：静态接收者列表（Static Recipient List）、动态接收者列表（Dynamic Recipient List）和 循环动态路由（Dynamic Router）。

##### 2.5.2.1 使用multicast处理Static Recipient List

使用multicast方式时，Camel将会把上一处理元素输出的Exchange复制多份发送给这个列表中的所有接收者，并且按顺序逐一执行（可设置为并行处理）这些接收者。这些接收者可能是通过Direct连接的另一个路由，也可能是Processor或者某个单一的Endpoint。需要注意的是，Excahnge是在Endpoint控制端点和Processor处理器间或者两个Processor处理器间唯一能够有效携带Message的元素，所以将一条消息复制多份并且让其执行不相互受到影响，那么必然就会对Exchange对象进行复制（是复制，是复制，虽然主要属性内容相同，但是这些Exchange使用的内存区域都是不一样的，ExchangeId也不一样）。

以下是multicast使用的简单示例代码：

```java
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

```

log:

> 2018 九月 05 10:40:02 INFO [pool-1-thread-2] org.apache.camel.util.CamelLogger.log(159) | Exchange[Id: ID-DESKTOP-URD5FD2-1536115198078-0-2, ExchangePattern: InOut, BodyType: org.apache.camel.converter.stream.InputStreamCache, Body: [Body is instance of org.apache.camel.StreamCache]]
> 2018 九月 05 10:40:02 INFO [pool-1-thread-1] org.apache.camel.util.CamelLogger.log(159) | Exchange[Id: ID-DESKTOP-URD5FD2-1536115198078-0-3, ExchangePattern: InOut, BodyType: org.apache.camel.converter.stream.InputStreamCache, Body: [Body is instance of org.apache.camel.StreamCache]]
> 2018 九月 05 10:40:02 INFO [qtp552936351-21] xin.zero2one.camel.demo.MulticastCamel$OtherProcessor.process(73) | OtherProcessor中的exchangeExchange[ID-DESKTOP-URD5FD2-1536115198078-0-1]

通过执行结果可以看到，在multicast中的两个接收者（两个路由分支的设定）分别在我们设置的线程池中运行，线程ID分别是【pool-1-thread-2】和【pool-1-thread-1】。**在multicast中的所有路由分支都运行完成后**，OtherProcessor处理器的实例在【qtp552936351-21】线程中继续运行（jetty:http-endpint对于本次请求的处理原本就在这个线程上运行）。

请各位读者特别注意以上三句日志所输出的ExchangeId，它们是完全不同的三个Exchange实例！其中在multicast的两个路由分支中承载Message的Excahnge对象，它们的Exchange-ID号分别为【 ID-DESKTOP-URD5FD2-1536115198078-0-2】和【ID-DESKTOP-URD5FD2-1536115198078-0-3】，来源则是multicast对原始Exchange对象的复制，原始Exchagne对象的Exchange-ID为【ID-DESKTOP-URD5FD2-1536115198078-0-1】。

##### 2.5.2.2 处理Dynamic Recipient List

在编排路由，很多情况下开发人员不能确定有哪些接收者会成为下一个处理元素：因为它们需要由Exchange中所携带的消息内容来动态决定下一个处理元素。这种情况下，开发人员就需要用到recipient方法对下一路由目标进行动态判断。以下代码示例中，我们将三个已经编排好的路由注册到Camel服务中，并通过打印在控制台上的结果观察其执行：

```java
package xin.zero2one.camel.demo;

import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.ModelCamelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by zhoujundong on 2018/9/5.
 */
public class DynamicCamel {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicCamel.class);

    public static void main(String[] args) throws Exception {
        // 这是camel上下文对象，整个路由的驱动全靠它了。
        ModelCamelContext camelContext = new DefaultCamelContext();
        // 启动route
        camelContext.start();
        // 将我们编排的一个完整消息路由过程，加入到上下文中
        camelContext.addRoutes((new DynamicCamel()).new DirectRouteA());
        camelContext.addRoutes((new DynamicCamel()).new DirectRouteB());
        camelContext.addRoutes((new DynamicCamel()).new DirectRouteC());

        // 通用没有具体业务意义的代码，只是为了保证主线程不退出
        synchronized (DynamicCamel.class) {
            DynamicCamel.class.wait();
        }
    }

    public class DirectRouteA extends RouteBuilder {

        /* (non-Javadoc)
         * @see org.apache.camel.builder.RouteBuilder#configure()
         */
        @Override
        public void configure() throws Exception {
            from("jetty:http://0.0.0.0:8282/dynamicCamel")
                    .setExchangePattern(ExchangePattern.InOnly)
                    .recipientList().jsonpath("$.data.routeName").delimiter(",")
                    .end()
                    .process(new MulticastCamel.OtherProcessor());
        }
    }


    /**
     * @author yinwenjie
     */
    public class DirectRouteB extends RouteBuilder {
        /* (non-Javadoc)
         * @see org.apache.camel.builder.RouteBuilder#configure()
         */
        @Override
        public void configure() throws Exception {
            // 第二个路由和第三个路由的代码都相似
            // 唯一不同的是类型
            from("direct:directRouteB")
                    .to("log:DirectRouteB?showExchangeId=true");
        }
    }

    /**
     * @author yinwenjie
     */
    public class DirectRouteC extends RouteBuilder {
        /* (non-Javadoc)
         * @see org.apache.camel.builder.RouteBuilder#configure()
         */
        @Override
        public void configure() throws Exception {
            // 第二个路由和第三个路由的代码都相似
            // 唯一不同的是类型
            from("direct:directRouteC")
                    .to("log:DirectRouteC?showExchangeId=true");
        }
    }

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

```

requestBody

> {"data":{"routeName":"direct:directRouteB,direct:directRouteC"},"token":"d9c33c8f-ae59-4edf-b37f-290ff208de2e","desc":""}

执行日志log

> 2018 九月 05 11:18:29 INFO [qtp259564670-21] org.apache.camel.util.CamelLogger.log(159) | Exchange[Id: ID-DESKTOP-URD5FD2-1536117469498-0-2, ExchangePattern: InOnly, BodyType: org.apache.camel.converter.stream.InputStreamCache, Body: [Body is instance of org.apache.camel.StreamCache]]
> 2018 九月 05 11:18:29 INFO [qtp259564670-21] org.apache.camel.util.CamelLogger.log(159) | Exchange[Id: ID-DESKTOP-URD5FD2-1536117469498-0-3, ExchangePattern: InOnly, BodyType: org.apache.camel.converter.stream.InputStreamCache, Body: [Body is instance of org.apache.camel.StreamCache]]
> 2018 九月 05 11:18:29 INFO [qtp259564670-21] xin.zero2one.camel.demo.MulticastCamel$OtherProcessor.process(73) | OtherProcessor中的exchangeExchange[ID-DESKTOP-URD5FD2-1536117469498-0-1]

recipientList方法将以 .data.routeName 中指定的路由信息动态决定一下个或者多个消息接收者，以上JSON片段中我们指定了两个“direct:directRouteB,direct:directRouteC”。那么recipientList会使用delimiter方法中设置的“,”作为分隔符来分别确定这两个接收者。

静态路由和动态路由在执行效果上有很多相似之处。例如在两种路径选择方式中，路由分支上的接收者中使用的Exchange对象的来源都是对上一执行元素所输出的Exchange对象的复制，这些Exchange对象除了其中携带的业务内容相同外，ExchangeID是不一样，也就是说每个路由分支的Exchange对象都不相同。所以各路由分支的消息都不受彼此影响。另外动态路由和静态路由都支持对路由分支的顺序执行和并发执行，都可以为并发执行设置独立的线程池。

##### 2.5.3.5 循环动态路由 Dynamic Router

动态循环路由的特点是开发人员可以通过条件表达式等方式，动态决定下一个路由位置。在下一路由位置处理完成后Exchange将被重新返回到路由判断点，并由动态循环路由再次做出新路径的判断。如此循环执行**直到动态循环路由不能再找到任何一条新的路由路径为止**。下图来源于官网（<http://camel.apache.org/dynamic-router.html>），展示了动态循环路由的工作效果：

![这里写图片描述](C:\Users\jundo\Desktop\typora\20160702214414419.jpg)

这里可以看出动态循环路由（dynamicRouter）和之前介绍的动态路由（recipientList）在工作方式上的差异。dynamicRouter一次选择只能确定一条路由路径，而recipientList只进行一次判断并确定多条路由分支路径；dynamicRouter确定的下一路由在执行完成后，Exchange对象还会被返回到dynamicRouter中以便开始第二次循环判断，而recipientList会为各个分支路由复制一个独立的Exchange对象，并且各个分支路由执行完成后Exchange对象也不会返回到recipientList；下面我们还是通过源代码片段，向各位读者展示dynamicRouter的使用方式。在代码中，我们编排了三个路由DirectRouteA主要负责通过Http协议接收处理请求，并执行dynamicRouter。DirectRouteB和DirectRouteC两个路由是可能被dynamicRouter选择的分支路径。

在DirectRouteA中我们使用“通过一个method方法返回信息”的方式确定dynamicRouter“动态循环路由”的下一个Endpoint。当然在实际使用中，开发人员还可以有很多方式向dynamicRouter“动态循环路由”返回指定的下一Endpoint。例如使用JsonPath指定JSON格式数据中的某个属性值，或者使用XPath指定XML数据中的某个属性值，又或者使用header方法指定Exchange中Header部分的某个属性。但是无论如何请开发人员确定一件事情：**向dynamicRouter指定下一个Endpoint的方式中是会返回null进行循环终止的**，否则整个dynamicRouter会无限的执行下去。

以上doDirect方法中，我们将一个计数器存储在了Exchange对象的properties区域，以便在同一个Exchange对象执行doDirect方法时进行计数操作。当同一个Exchange对象第一次执行动态循环路由判断时，选择directRouteB最为一下路由路径；当Exchange对象第二次执行动态循环路由判断时，选择DirectRouteC作为下一路由路径；当Exchange对象第三次执行时，选择一个Log4j-Endpoint作为下一个路由路径；当Exchange对象第四次执行时，作为路由路径判断的方法doDirect返回null，以便终止dynamicRouter的执行。

不能在DirectRouteA类中定义一个全局变量作为循环路由的计数器，因为由Jetty-HttpConsumer生成的线程池中，线程数量和线程对象是固定的，并且Camel也不是为每一个Exchange对象的运行创建新的DirectRouteA对象实例。

```java

```

输出日志：

> 2018 九月 05 11:39:03 INFO [qtp2122049087-22] xin.zero2one.camel.demo.DynamicRouterCamel$DirectRouteA.doDirect(71) | 这是Dynamic Router循环第：【1】次执行！执行线程：qtp2122049087-22
> 2018 九月 05 11:39:03 INFO [qtp2122049087-22] org.apache.camel.util.CamelLogger.log(159) | Exchange[Id: ID-DESKTOP-URD5FD2-1536118660798-0-7, ExchangePattern: InOut, BodyType: org.apache.camel.converter.stream.InputStreamCache]
> 2018 九月 05 11:39:03 INFO [qtp2122049087-22] xin.zero2one.camel.demo.DynamicRouterCamel$DirectRouteA.doDirect(71) | 这是Dynamic Router循环第：【2】次执行！执行线程：qtp2122049087-22
> 2018 九月 05 11:39:03 INFO [qtp2122049087-22] org.apache.camel.util.CamelLogger.log(159) | Exchange[Id: ID-DESKTOP-URD5FD2-1536118660798-0-7, ExchangePattern: InOut, BodyType: org.apache.camel.converter.stream.InputStreamCache]
> 2018 九月 05 11:39:03 INFO [qtp2122049087-22] xin.zero2one.camel.demo.DynamicRouterCamel$DirectRouteA.doDirect(71) | 这是Dynamic Router循环第：【3】次执行！执行线程：qtp2122049087-22
> 2018 九月 05 11:39:03 INFO [qtp2122049087-22] org.apache.camel.util.CamelLogger.log(159) | Exchange[Id: ID-DESKTOP-URD5FD2-1536118660798-0-7, ExchangePattern: InOut, BodyType: org.apache.camel.converter.stream.InputStreamCache]
> 2018 九月 05 11:39:03 INFO [qtp2122049087-22] xin.zero2one.camel.demo.DynamicRouterCamel$DirectRouteA.doDirect(71) | 这是Dynamic Router循环第：【4】次执行！执行线程：qtp2122049087-22
> 2018 九月 05 11:39:03 INFO [qtp2122049087-22] xin.zero2one.camel.demo.MulticastCamel$OtherProcessor.process(73) | OtherProcessor中的exchangeExchange[ID-DESKTOP-URD5FD2-1536118660798-0-7]

从以上执行效果看，无论dynamicRouter执行的是第几次循环判断，Exchange都是同一个（ID号为【ID-DESKTOP-URD5FD2-1536118660798-0-7】）。



### 2.6 Service与生命周期

在Apache Camel中有一个比Endpoint、Component、CamelContext等元素更基础的概念元素：Service。Camel官方文档中对Service的解释是：

> Camel uses a simple lifecycle interface called Service which has a single start() and stop() method.
> Various classes implement Service such as CamelContext along with a number of Component and Endpoint classes.
> When you use Camel you typically have to start the CamelContext which will start all the various components and endpoints and activate the routing rules until the context is stopped again.

包括`Endpoint`、`Component`、`CamelContext`等元素在内的大多数工作在Camel中的元素，都是一个一个的Service。例如，我们虽然定义了一个`JettyHttpComponent`（就是在代码中使用DSL定义的”jetty:<http://0.0.0.0:8282/directCamel>“头部所表示的Component），但是我们想要在Camel应用程序运行阶段使用这个Component，就需要利用start方法将这个Component启动起来。

实际上通过阅读`org.apache.camel.component.jetty.JettyHttpComponent`的源代码，读者可以发现JettyHttpComponent的启动过程起始大多数情况下什么都不会做，**只是在`org.apache.camel.support.ServiceSupport`中更改了`JettyHttpComponent`对象的一些状态属性**。倒是`HttpConsumer`这个Service，在启动的过程中启动了`JettyHttpComponent`对象的连接监听，并建立了若干个名为【qtp-*】的处理线程

Service有且只有两个接口方法定义：start()和stop()，这两个方法的含义显而易见，启动服务和终止服务。另外继承自Service的另外两个子级接口`SuspendableService`、`ShutdownableService`分别还定义了另外几个方法：suspend()、resume()和shutdown()方法，分别用来暂停服务、恢复服务和彻底停止服务（彻底停止服务意味着在Camel应用程序运行的有生之年不能再次启动了）。

Camel应用程序中的每一个Service都是独立运行的，各个Service的关联衔接通过CamelContext上下文对象完成。每一个Service通过调用start()方法被激活并参与到Camel应用程序的工作中，直到它的stop()方法被调用。也就是说，**每个Service都有独立的生命周期**。（<http://camel.apache.org/lifecycle.html>）

那么问题来了，既然每个Service都有独立的生命周期，我们启动Camel应用程序时就要启动包括Route、Endpoint、Component、Producer、Consumer、LifecycleStrategy等概念元素在内的无数多个Service实现，那么作为开发人员不可能编写代码一个一个的Service来进行启动（大多数开发人员不了解Camel的内部结构，也根本不知道要启动哪些Service）。那么作为Camel应用程序肯定需要提供一个办法，在应用程序启动时分析应用程序所涉及到的所有的Service，并统一管理这些Service启动和停止的动作。这就是CamelContext所设计的另一个功能。

## 3. CamelContext上下文

CamelContext从英文字面上理解，是Camel服务上下文的意思。CamelContext在Apache Camel中的重要性，就像ApplicationContext之于Spring、ServletContext之于Servlet…… 但是包括Camel官方文档在内的，所有读者能够在互联网上找到的资料对于CamelContext的介绍都只有聊聊数笔。

> The context component allows you to create new Camel Components from a CamelContext with a number of routes which is then treated as a black box, allowing you to refer to the local endpoints within the component from other CamelContexts.
>
> First you need to create a CamelContext, add some routes in it, start it and then register the CamelContext into the Registry (JNDI, Spring, Guice or OSGi etc).

以上是Camel官方文档（<http://camel.apache.org/context.html>）对于CamelContext作用的一些说明，大致的意思是说CamelContext横跨了Camel服务的整个生命周期，并且为Camel服务的工作环境提供支撑。

### 3.1 DefaultCamelContext

`DefaultCamelContext`主要的全局变量

```java
private EndpointRegistry<EndpointKey> endpoints;
private ClassLoader applicationContextClassLoader;
// 已使用的组件名称（即Endpoint URI头所代表的组件名称）和组件对象的对应关系
private final Map<String, Component> components = new ConcurrentHashMap<>();
// 针对原始路由编排所分析出的路由对象，路由对象是作为CamelContext从路由中的一个元素传递到下一个元素的依据
//  路由对象中还包含了，将路由定义中各元素连接起来的其它Service。例如DefaultChannel
private final Set<Route> routes = new LinkedHashSet<>();
// 由DSL或者XML描述的原始路由编排。每一个RouteDefinition元素中都包含了参与这个路由的所有Service定义。
private final List<RouteDefinition> routeDefinitions = new ArrayList<>();
// 生命周期策略，实际上是一组监听
private List<LifecycleStrategy> lifecycleStrategies = new CopyOnWriteArrayList<>();
// 这是一个计数器，记录当前每一个不同的Routeid中正在运行的的Exchange数量
private InflightRepository inflightRepository = new DefaultInflightRepository();
// 服务停止策略
private ShutdownStrategy shutdownStrategy = new DefaultShutdownStrategy(this);
```

Apache Camel中还有一个名叫`org.apache.camel.CamelContextAware`的接口，只要实现该接口的就必须实现这个接口定义的两个方法：setCamelContext和getCamelContext。而实际上在Camel中的大多数元素都实现了这个接口，所以我们在阅读代码时可以发现**`DefaultCamelContext`在一边启动各个Service的时候，顺便将自己所为参数赋给了正在启动的Service，最终实现了各个Service之间的共享上下文信息的效果**：

```java
public interface CamelContextAware {

    /**
     * Injects the {@link CamelContext}
     *
     * @param camelContext the Camel context
     */
    void setCamelContext(CamelContext camelContext);
    
    /**
     * Get the {@link CamelContext}
     *
     * @return camelContext the Camel context
     */
    CamelContext getCamelContext();
    
}
```

```java
// 这是DefaultCamelContext的doAddService方法中
// 对实现了CamelContextAware接口的Service
// 进行CamelContext设置的代码
private void doAddService(Object object, boolean closeOnShutdown) throws Exception {
    ......
    if (object instanceof CamelContextAware) {
        CamelContextAware aware = (CamelContextAware) object;
        aware.setCamelContext(this);
    }
    ......
}
```

首先说明**`DefaultCamelContext` 也是一个Service**，所以它必须实现Service接口的start()方法和stop()方法。而`DefaultCamelContext`对于start()方法的实现就是“启动其它已知的Service”。

更具体的来说，`DefaultCamelContext`将所有需要启动的Service按照它们的作用类型进行区分，例如负责策略管理的Service、负责Components组件描述的Service、负责注册管理的Service等等，然后再按照顺序启动这些Service。以下代码片段提取自`DefaultCamelContext`的`doStartCamel()`私有方法，并加入了笔者的中文注释（原有作者的注释依然保留），**这个私有方法由`DefaultCamelContext`中的start()方法间接调用**，用于完成上述各Service启动操作。

```java
// 为了调用该私有方法，之前的方法执行栈分别为：
// start()
// super.start()
// doStart()
......
private void doStartCamel() throws Exception {
    
    ......
        
    // 首先启动的是ManagementStrategy策略管理器，它的默认实现是DefaultManagementStrategy。
    // 还记得我们在分析DUBBO时提到的Java spi机制吧，Camel-Core也使用了这个机制，并进行了二次封装。详见org.apache.camel.spi代码包。
    // 启动ManagementStrategy，可以帮助Camel实现第三方组件包（例如Camel-JMS）的动态加载
    // start management strategy before lifecycles are started
    ManagementStrategy managementStrategy = getManagementStrategy();
    // inject CamelContext if aware
    if (managementStrategy instanceof CamelContextAware) {
        ((CamelContextAware) managementStrategy).setCamelContext(this);
    }
    ServiceHelper.startService(managementStrategy);

    ......
    // 然后启动的是 生命周期管理策略 
    // 这个lifecycleStrategies变量是一个LifecycleStrategy泛型的List集合。
    // 实际上LifecycleStrategy是指是一组监听，详见代码片段后续的描述
    ServiceHelper.startServices(lifecycleStrategies);

    ......
    // 接着做一系列的Service启动动作
    // 首先是Endpoint注册管理服务，要进行重点介绍的是org.apache.camel.util.LRUSoftCache
    // 它使用了java.lang.ref.SoftReference进行实现，这是Java提供的
    endpoints = new EndpointRegistry(this, endpoints);
        addService(endpoints);

    ......
    // 启动线程池管理策略和一些列其它服务
    // 基本上这些Service已经在上文中提到过
    doAddService(executorServiceManager, false);
    addService(producerServicePool);
    addService(inflightRepository);
    addService(shutdownStrategy);
    addService(packageScanClassResolver);
    addService(restRegistry);

    ......
    // start components
    startServices(components.values());
    // 启动路由定义，路由定义RouteDefinition本身并不是Service，但是其中包含了参与路由的各种元素，例如Endpoint。
    // start the route definitions before the routes is started
    startRouteDefinitions(routeDefinitions);

    ......
}
```

#### 3.1.1 LifecycleStrategy

`LifecycleStrategy`接口按照字面的理解是一个关于Camel中元素生命周期的规则管理器，但实际上`LifecycleStrategy`接口的定义更确切的应该被描述成一个监听器

![1536123371224](C:\Users\jundo\AppData\Local\Temp\1536123371224.png)

当Camel引用程序中发生诸如Route加载、Route移除、Service加载、Serivce移除、Context启动或者Context移除等事件时，`DefaultCamelContext`中已经被添加到集合“lifecycleStrategies”（`java.util.List<LifecycleStrategy>`）的`LifecycleStrategy`对象将会做相应的事件触发。

读者还应该注意到“lifecycleStrategies”集合是一个CopyOnWriteArrayList，我们随后对这个List的实现进行讲解。以下代码展示了在DefaultCamelContext添加Service时，DefaultCamelContext内部是如何触发“lifecycleStrategies”集合中已添加的监听的：

```java
 private void doAddService(Object object, boolean stopOnShutdown, boolean forceStart) throws Exception {

        // inject CamelContext
        if (object instanceof CamelContextAware) {
            CamelContextAware aware = (CamelContextAware) object;
            aware.setCamelContext(this);
        }

        if (object instanceof Service) {
            Service service = (Service) object;

            for (LifecycleStrategy strategy : lifecycleStrategies) {
                if (service instanceof Endpoint) {
                    // use specialized endpoint add
                    strategy.onEndpointAdd((Endpoint) service);
                } else {
                    strategy.onServiceAdd(this, service, null);
                }
            }

            if (!forceStart) {
                // now start the service (and defer starting if CamelContext is starting up itself)
                deferStartService(object, stopOnShutdown);
            } else {
                // only add to services to close if its a singleton
                // otherwise we could for example end up with a lot of prototype scope endpoints
                boolean singleton = true; // assume singleton by default
                if (object instanceof IsSingleton) {
                    singleton = ((IsSingleton) service).isSingleton();
                }
                // do not add endpoints as they have their own list
                if (singleton && !(service instanceof Endpoint)) {
                    // only add to list of services to stop if its not already there
                    if (stopOnShutdown && !hasService(service)) {
                        servicesToStop.add(service);
                    }
                }
                ServiceHelper.startService(service);
            }
        }
    }
```

#### 3.1.2 CopyOnWriteArrayList 与监听者模式

正如上一小节讲到的，已在`DefaultCamelContext`中注册的`LifecycleStrategy`对象存放于一个名叫“lifecycleStrategies”的集合中，后者是`CopyOnWriteArrayList`容器的实现，这是一个从JDK 1.5+ 版本开始提供的容器结构。

各位读者可以设想一下这样的操作：某个线程在对容器进行写操作的同时，还有另外的线程对容器进行读取操作。如果上述操作过程是在**没有“线程安全”特性的容器**中进行的，那么可能出现的情况就是：开发人员原本想读取容器中 i 位置的元素X，可这个元素已经被其它线程删除了，开发人员最后读取的 i 位置的元素变成了Y。但是在具有“写线程安全”特性的容器中进行这样的操作就不会有问题：因为写操作在另一个副本容器中进行，原容器中的数据大小、数据位置都不会受到影响。

如果上述操作过程是在**有“线程安全”特性的容器**中进行的，那么以上脏读的情况是可以避免的。但是又会出现另外一个问题：由于容器的各种读写操作都会加上锁（无论是悲观锁还是乐观锁），所以容器的读写性能又会收到影响。如果采用的是乐观锁，那么对性能的影响可能还不会太大，但是如果采用的是悲观锁，那么对性能的影响就有点具体了。

`CopyOnWriteArrayList`为我们提供了另一种线程安全的容器操作方式。`CopyOnWriteArrayList`的工作效果类似于`java.util.ArrayList`，但是它通过`ReentrantLock`实现了容器中写操作的线程安全性。`CopyOnWriteArrayList`最大的特点是：**当进行容器中元素的修改操作时，它会首先将容器中的原有元素克隆到一个副本容器中，然后对副本容器中的元素进行修改操作**。待这些操作完成后，再将副本中的元素集合重新会写到原有的容器中完成整个修改操作。这种工作机制称为Copy-On-Write（COW）。这样做的最主要目的是分离容器的读写操作。`CopyOnWriteArrayList`会对所有的写操作加锁，但是不会对任何容器的读操作加锁（因为写操作在一个副本中进行）。

另外CopyOnWriteArrayList还重新实现了一个新的迭代器：COWIterator。它是做什么的呢？举例说明：在ArrayList中我们如果在进行迭代时同时进行容器的写操作，那么就可能会因为下标超界等原因出现程序异常：

```java
List<?> list = new ArrayList<?>();
// 省略了添加元素部分的代码
......

// ArrayList不支持这样的操作方式，会报错
for(Object item : list){
    list.remove(item);
}
```

但如果使用CopyOnWriteArrayList中重写的COWIterator迭代器，就不会出现的情况（开发人员还可以使用JDK 1.5+ 提供的另一个线程安全COW容器：CopyOnWriteArraySet）：

```java
List<?> list = new CopyOnWriteArrayList<?>();
// 省略了添加元素部分的代码
......

// COWIterator迭代器支持一边迭代一边进行容器的写操作
for(Object item : list){
    list.remove(item);
}
```

那么`CopyOnWriteArrayList`和监听器模式有什么关系呢？在书本上我们学到的监听器容器基本上都不是线程安全的，这基本上是出于两方面的考虑。首先对于设计模式的初学者来说最重要的理解模式所代表的设计思想，而非实现细节；另外，在这些示例中，设计模式的实现和操作一般为单一线程，不会出现多其它线程同时操作容器的情况。以下是我们常看到的监听者模式（代码片段）：

```java
/**
 * 为事件监听携带的业务对象
 * @author yinwenjie
 */
public class BusinessEventObject extends EventObject {
    public BusinessEventObject(Object source) {
        super(source);
    }
}

/**
 * 监听器，其中只有一个事件方法
 * @author yinwenjie
 */
public interface BusinessEventListener extends EventListener {
    public void onBusinessStart(BusinessEventObject eventObject);
}

/**
 * 业务级别的代码
 * @author yinwenjie
 */
public class BusinessOperation {

    /**
     * 已注册的监听器放在这里
     */
    private List<BusinessEventListener> listeners = new ArrayList<BusinessEventListener>();

    public void registeListener(BusinessEventListener eventListener) {
        this.listeners.add(eventListener);
    }

    ......  

    public void doOp() {
        //业务代码在这里运行后，接着促发监听
        for (BusinessEventListener businessEventListener : listeners) {
            businessEventListener.onBusinessStart(new BusinessEventObject(this));
        }
    }
    ......
}
```

以上代码无需做太多说明。请注意，由于我们使用`ArrayList`这样的非线程安全容器作为已注册监听的存储容器，所以开发人员在使用这个容器触发监听事件时需要格外小心：确保同一时间只会有一个线程对容器进行写操作、确保在一个迭代器内没有容器的写操作、还要确保每个监听器的具体实现不会把当前线程锁死（次要）——但作为开发人员真的能随时保证这些事情吗？

#### 3.1.3 SoftReference

我们都知道JVM的内存是有上限的，JVM的垃圾回收线程进行工作时会将当前**没有任何引用可达性**的对象区域进行回收，以便保证JVM的内存空间能够被循环利用。当JVM的可用内存达到上限，且垃圾回收线程又无法找到任何可以回收的对象时，应用程序就会报错。JVM中某个线程的堆栈状态可能如下图所示：

![](C:\Users\jundo\Desktop\typora\20160710131735305.jpg)

上图中线程Thread1在执行时，在栈内存中创建了一个变量X。变量X指向堆内存为A类实例化对象分配的内存空间（后文称之为A对象）。注意，A对象中还对同样存在于堆内存区域中的B类、C类的实例化对象（后文称为B对象、C对象）有引用关系。那么如果JVM垃圾回收策略要对A对象、B对象、C对象三个内存区域进行回收，**除非针对这些区域的引用可达性全部消失，否则以上所说到的对内存区域都不会被回收**。这样的对象间引用方式被称为强引用（Strong Reference）：JVM宁愿抛出OutOfMemoryError也不会在还存在引用可及性的情况下回收内存区域。

> 引用可达性，是JVM垃圾回收策略中确认哪些内存区域可以进行回收的判断算法。大致的定义是：从某个根引用开始进行引用图结构的深度遍历扫描，当遍历完成时那些没有被扫描到的一个（或者多个）内存区域就是失去引用可达性的区域。

JAVA JDK1.2+开始还提供一种称为“软引用”（Soft Reference）的对象间引用方式。在这种方式下，对象间的引用关系通过一个命名为java.lang.ref.SoftReference的工作类进行间接托管，目的是**当JVM内存空间不足，垃圾回收策略被主动触发时** 进行以下回收策略操作：扫面当前堆内存中只建立了“软引用”的内存区域，**无论这些“软引用”是否依然存在引用可达性，都强制对这些建立了“软引用”的对象进行回收，以便腾出内存空间**。下面我们对上图中的对象间引用关系进行如下图所示的调整：

![](C:\Users\jundo\Desktop\typora\20160710132358804.jpg)

上如所示的引用关系和图A中的引用关系类似，只是我们在A对B、C的引用关系上都增加了一个SoftReference对象进行间接关联。代码片段如下所示：

```java
package com.test;

import java.lang.ref.SoftReference;

public class A {
    /**
     * 软引用 B
     */
    private SoftReference<B> paramB;

    /**
     * 软引用 C
     */
    private SoftReference<C> paramC;

    /**
     * 构造函数中，建立和B、C的软引用
     * @param paramB
     * @param paramC
     */
    public A(B paramB , C paramC) {
        this.paramB = new SoftReference<B>(paramB);
        this.paramC = new SoftReference<C>(paramC);
    }

    /**
     * @return the paramB
     */
    public B getParamB() {
        return paramB.get();
    }

    /**
     * @return the paramC
     */
    public C getParamC() {
        return paramC.get();
    }
}
```

当出现“软引用”对象被垃圾回收线程回收时，例如B对象被回收时，A对象中的getB()方法将会返回null。那么原来进行B对象间接引用动作的SoftReference对象该怎么处理呢？要知道如果B对象被回收了，那么承载这个“软引用”的SoftReference对象就没有什么用处了。还好JDK中帮我们准备了名叫ReferenceQueue的队列，当SoftReference对象所承载的“软引用”对象被回收后，这个Reference对象将被送入ReferenceQueue中（当然你也可以不指定，如果不指定的话SoftReference对象会以“强引用”的回收策略被回收，不过SoftReference对象所占用的内存空间不大），开发人员可以随时扫描ReferenceQueue，并对其中的Reference对象进行清除。

注意，**一个对象同一时间并不一定只被另一个对象引用**，而是可能被若干个对象同时引用。只要对这个对象的引用中有一个没有使用“软引用”特性，那么垃圾回收策略对它的回收就不会采用“软引用”的回收策略进行。如下图所示：

![](C:\Users\jundo\Desktop\typora\20160710133529206.jpg)

上图中，有两个对象元素同时对B对象进行了引用（注意是同一个B对象，而不是对B类分别new了两次）。其中A对象对B对象的依赖通过“软引用”（SoftReference）间接完成，D对象对B对象的引用却是通过传统的“硬引用”完成的。当垃圾回收策略开始工作时它会发现这样的情况，并且即使在内存空间不够的情况下，也不会对B对象进行回收，直到针对B对象的所有引用可达性消失。

> 在JAVA中还有弱引用、虚引用两个概念（Camel中的LRUWeakCache就是基于弱引用实现的）。但是由于他们至少和我们重点说明的DefaultCamelContext没有太多关系，所以这里笔者就不再发散性的讲下去了。对这块还不太了解的读者可以自行参考JDK官方文档。

#### 3.1.4 LRU算法简介

LRU的全称是Least Recently Used（最近最少使用），它是一种选择算法，有的文章中也把LRU算法称为“缓存淘汰算法”。在计算机技术实践中它被广泛用于缓存功能的开发，例如处理内存分页与虚拟内存的置换问题，或者又像Camel那样用于计算选择Endpoint对象将从缓存结构中被移除。下图的结构说明了LRU算法的大致工作过程：

![](C:\Users\jundo\Desktop\typora\20160711095600597.jpg)

上图中，我们可以看到几个关键点：

- 整个队列有一个阀值用于限制能够存放于队列容器中的最大元素个数，这个阀值我们暂且称为maxCacheSize。
- 当队列中的元素还没有达到这个maxCacheSize时，进入队列的元素将被放置在队列的最前面，队列会保持这种处理策略直到队列中的元素达到maxCacheSize为止。
- 当队列中的某个元素被选择时（一般来说，队列允许开发人员在选择元素时传入一个Key，队列会依据这个Key进行元素选择），被命中的元素又会重新排列到队列的最前面。这样一来，队列最尾部的元素就是近期使用最少的一个元素。
- 一旦当队列中的元素达到maxCacheSize后（不可能超过），新进入队列中的元素将会把队列最尾部的元素挤出队列，而它自己会排列到队列的最顶部。

## 4. 使用XML形式编排路由

除了上文中我们一直使用的DSL进行路由编排的操作方式以外，Apache Camel也支持使用XML文件描述进行路由编排。通过XML文件开发人员还可以将Camel和Spring结合起来使用——两者本来就可以进行无缝集成。下面我们对这种方式的使用大致进行一下介绍。首先我们创建一个XML文件，和Spring结合使用的：

```xml
<?xml version="1.0" encoding="UTF-8"?>

<beans xmlns="http://www.springframework.org/schema/beans" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:context="http://www.springframework.org/schema/context"
    xmlns:camel="http://camel.apache.org/schema/spring" 
    xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd
        http://www.springframework.org/schema/context http://www.springframework.org/schema/context/spring-context.xsd
        http://camel.apache.org/schema/spring http://camel.apache.org/schema/spring/camel-spring-2.14.1.xsd ">

    <camel:camelContext xmlns="http://camel.apache.org/schema/spring">
        <camel:endpoint id="jetty_from" uri="jetty:http://0.0.0.0:8282/directCamel"/>
        <camel:endpoint id="log_to" uri="log:helloworld2?showExchangeId=true"/>

        <camel:route>
            <camel:from ref="jetty_from"/>
            <camel:to ref="log_to"/>
        </camel:route>
    </camel:camelContext>

    ......
</beans>
```

以上xml文件中我们定义了一个Camel路由过程。请注意xml文件中所使用的schema xsd路径，不同的Apache Camel版本所使用的xsd路径是不一样的，这在Camel的官方文档中有详细说明：<http://camel.apache.org/xml-reference.html>。在示例代码中笔者使用的Camel版本是V2.14.1。

XML文件描述中，笔者定义了两个endpoint：id为“jetty_from”的Endpoint将作为route的入口，接着传来的Http协议信息将到达id为“log_to”endpoint中。后者是一个Log4j的操作，最终Exchange中的In Message Body信息将打印在控制台上。接下来我们启动测试程序：

代码见GitHub

输出日志：

> 2018 九月 05 14:58:40 INFO [qtp800493254-20] org.apache.camel.util.CamelLogger.log(159) | Exchange[Id: ID-DESKTOP-URD5FD2-1536130630533-0-1, ExchangePattern: InOut, BodyType: org.apache.camel.converter.stream.InputStreamCache, Body: [Body is instance of org.apache.camel.StreamCache]]
> 2018 九月 05 14:58:40 INFO [qtp800493254-20] xin.zero2one.camel.demo.service.impl.SpringXmlServiceImpl.sayHello(18) | input msg is ok
> 2018 九月 05 14:58:40 INFO [qtp800493254-20] xin.zero2one.camel.demo.processor.DefineProcessor.process(25) | process success