package xin.zero2one.camel.demo.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import xin.zero2one.camel.demo.service.ISpringXmlService;

/**
 * Created by zhoujundong on 2018/9/5.
 */
@Component("defineProcessor")
public class DefineProcessor implements Processor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefineProcessor.class);

    @Autowired
    private ISpringXmlService springXmlService;

    @Override
    public void process(Exchange exchange) throws Exception {
        springXmlService.sayHello("ok");
        LOGGER.info("process success");
    }
}
