package org.dragonfly.ddtd.spring.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * <p>
 * 
 * </p>
 *
 * @author jian.dong1
 * @since 2022-02-08
 */
@Getter
@Setter
@TableName("ddtd_example_task")
public class DdtdExampleTaskDO extends BaseDO {

    /**
     * 任务id
     */
    private String taskId;

    /**
     * 任务数据
     */
    private String taskData;

    /**
     * 任务状态
     */
    private Integer state;
    /**
     * 实例信息
     */
    private String instInfo;
    /**
     * 任务开始时间
     */
    private Date startAt;
    /**
     * 耗时
     */
    private String elapsed;
}
