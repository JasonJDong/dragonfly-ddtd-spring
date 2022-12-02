package org.dragonfly.ddtd.spring.generator;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.DataSourceConfig;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.TemplateType;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import org.dragonfly.ddtd.spring.domain.BaseDO;
import com.google.common.base.Splitter;
import com.google.common.collect.Maps;

import java.util.Map;

/**
 * 从表生成代码
 *
 * @author xbzheng
 */
public class MybatisXEngineCodeGenerator {
    public static void main(String[] args) {

        // 数据源配置
        DataSourceConfig.Builder dataSourceBuilder = new DataSourceConfig.Builder(
                "jdbc:mysql://127.0.0.1:3306/example_db?useUnicode=true&useSSL=false&characterEncoding=utf8",
                "platform",
                "7tafjU=ROHrzj31");

        FastAutoGenerator generator = FastAutoGenerator.create(dataSourceBuilder);
        String projectPath = System.getProperty("user.dir");
        generator.globalConfig(builder -> {

            // 全局配置
            builder.outputDir(projectPath + "/src/test/java")
                    .author("jian.dong1")
                    .fileOverride()
                    .disableOpenDir();
        }).packageConfig(builder -> {

            // 包配置
            Map<OutputFile, String> pathMap = Maps.newHashMap();
            pathMap.put(OutputFile.entity, projectPath + "/src/test/java/org/dragonfly/ddtd/domain");
            pathMap.put(OutputFile.mapper, projectPath + "/src/test/java/org/dragonfly/ddtd/dao/mapper");
            pathMap.put(OutputFile.service, projectPath + "/src/test/java/org/dragonfly/ddtd/service/");
            pathMap.put(OutputFile.serviceImpl, projectPath + "/src/test/java/org/dragonfly/ddtd/service/impl");
            pathMap.put(OutputFile.mapperXml, projectPath + "/src/test/resources/mapper");

            builder.moduleName("")
                    .parent("org.dragonfly.ddtd")
                    .entity("domain")
                    .service("service")
                    .serviceImpl("service.impl")
                    .mapper("dao.mapper")
                    .xml("/src/test/resources/mapper")
                    .pathInfo(pathMap);
        }).templateConfig(builder ->
                builder.disable(TemplateType.CONTROLLER)
        ).templateEngine(new FreemarkerTemplateEngine())
                .strategyConfig((scanner, builder) ->
                        builder.addInclude(Splitter.on(StringPool.COMMA).splitToList(scanner.apply("请输入表名，多个表逗号分隔：")))
                                .serviceBuilder()
                                .superServiceClass(IService.class)
                                .superServiceImplClass(ServiceImpl.class)
                                .mapperBuilder()
                                .superClass(BaseMapper.class)
                                .entityBuilder()
                                .formatFileName("%sDO")
                                .enableLombok()
                                .disableSerialVersionUID()
                                .superClass(BaseDO.class)
                                .addSuperEntityColumns("mart","created","modified","create_id","customize","yn","update_id")
                                .enableColumnConstant()
                                .idType(IdType.AUTO)
                ).execute();

    }
}

