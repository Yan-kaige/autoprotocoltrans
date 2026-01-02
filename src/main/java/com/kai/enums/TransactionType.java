package com.kai.enums;

/**
 * 标准交易类型枚举
 * 预置的银行交易类型，可以在添加配置时选择或修改
 */
public enum TransactionType {
    /**
     * 今日余额查询
     */
    TODAY_BALANCE_QUERY("今日余额查询"),
    
    /**
     * 历史余额查询
     */
    HISTORY_BALANCE_QUERY("历史余额查询"),
    
    /**
     * 今日明细查询
     */
    TODAY_DETAIL_QUERY("今日明细查询"),
    
    /**
     * 历史明细查询
     */
    HISTORY_DETAIL_QUERY("历史明细查询"),
    
    /**
     * 单笔付款
     */
    SINGLE_PAYMENT("单笔付款"),
    
    /**
     * 单笔查证
     */
    SINGLE_VERIFICATION("单笔查证"),
    
    /**
     * 回单下载
     */
    RECEIPT_DOWNLOAD("回单下载");
    
    private final String displayName;
    
    TransactionType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * 根据显示名称获取枚举值
     */
    public static TransactionType fromDisplayName(String displayName) {
        for (TransactionType type : values()) {
            if (type.displayName.equals(displayName)) {
                return type;
            }
        }
        return null;
    }
    
    /**
     * 获取所有显示名称数组
     */
    public static String[] getAllDisplayNames() {
        TransactionType[] types = values();
        String[] names = new String[types.length];
        for (int i = 0; i < types.length; i++) {
            names[i] = types[i].displayName;
        }
        return names;
    }
}


