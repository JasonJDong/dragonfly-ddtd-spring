package org.dragonfly.ddtd.spring.domain;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.util.Date;

/**
 * 作用描述 数据库默认对象基类
 *
 * @author jian.dong1
 * @version 1.0
 * @date 2021/11/4
 **/
@Data
public abstract class BaseDO {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 租户标识
     */
    private String tenant;
    /**
     * 创建人id
     */
    private String createId;
    /**
     * 创建时间
     */
    @TableField(insertStrategy = FieldStrategy.NEVER)
    private Date created;
    /**
     * 逻辑删除
     */
    private Integer yn;
    /**
     * 修改人id
     */
    private String updateId;
    /**
     * 更新时间
     */
    @TableField(insertStrategy = FieldStrategy.NEVER)
    private Date modified;
}
