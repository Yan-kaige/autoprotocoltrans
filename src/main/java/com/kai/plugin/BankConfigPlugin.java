package com.kai.plugin;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

/**
 * 银行配置插件基类
 * 其他项目可以继承此类或参考此实现来加载银行配置
 */
public class BankConfigPlugin extends Plugin {
    
    public BankConfigPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }
    
    @Override
    public void start() {
        // 插件启动时的逻辑
        // 可以在这里加载 bank-config.json 并初始化配置
        System.out.println("Bank Config Plugin started: " + getWrapper().getPluginId());
    }
    
    @Override
    public void stop() {
        // 插件停止时的逻辑
        System.out.println("Bank Config Plugin stopped: " + getWrapper().getPluginId());
    }
}

