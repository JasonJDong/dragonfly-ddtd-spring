package org.dragonfly.ddtd.spring.test;

import org.dragonfly.ddtd.utils.RuntimeUtil;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

/**
 * 作用描述 性能测试
 *
 * @author jian.dong1
 * @version 1.0
 * @date 2022/2/16
 **/
public class TestPerformance {

    @Test
    public void performanceTest() throws InterruptedException {

        for (int i = 0; i < 20; i++) {

            System.out.println(
                    RuntimeUtil.cpuLoadString()
            );
            System.out.println(
                    RuntimeUtil.memoryLoadString()
            );
            TimeUnit.SECONDS.sleep(1L);
        }
    }
}
