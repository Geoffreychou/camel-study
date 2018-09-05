package xin.zero2one.camel.demo.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import xin.zero2one.camel.demo.service.ISpringXmlService;

/**
 * Created by zhoujundong on 2018/9/5.
 */
@Component("SpringXmlServiceImpl")
public class SpringXmlServiceImpl implements ISpringXmlService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringXmlServiceImpl.class);

    @Override
    public void sayHello(String msg) {
        LOGGER.info("input msg is {}", msg);
    }
}
