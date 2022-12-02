package org.dragonfly.ddtd.spring.domain.enums;

import lombok.Getter;

/**
 * 作用描述 任务处理状态
 *
 * @author jian.dong1
 * @version 1.0
 * @date 2022/2/8
 **/
@Getter
public enum TaskProcessState {

    SUCCESS(1, "成功"),
    FAIL(-1, "失败"),
    TERMINATE(-9, "中断"),
    ;
    /**
     * 值
     */
    private final int value;
    /**
     * 名称
     */
    private final String name;

    TaskProcessState(int value, String name) {
        this.value = value;
        this.name = name;
    }
}
