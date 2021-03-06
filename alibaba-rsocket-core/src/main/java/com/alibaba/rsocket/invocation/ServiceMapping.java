package com.alibaba.rsocket.invocation;

import java.lang.annotation.*;

/**
 * Service Mapping for interface method
 *
 * @author leijuan
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ServiceMapping {
    /**
     * service mapping value, service or handler name
     *
     * @return mapping value
     */
    String value() default "";

    String group();

    String version();

    String encoding();

    String decoding();

    String endpoint();
}
