package com.core.manycloudcommon.caller.vo;

import java.math.BigDecimal;

/**
 * 平台支出财务统计VO
 */
public class StatisticsExpenditureVO {

    /** 总支出 **/
    private BigDecimal expenditureTotal;

    /** 平台返佣 **/
    private BigDecimal platformCommission;

    /** 平台提现 **/
    private BigDecimal platformWithdrawal;

    /** 线下返佣 **/
    private BigDecimal offlineCommission;

    /** 线下退款 **/
    private BigDecimal offlineWithdrawal;

    /** 采购支出 **/
    private BigDecimal procure;

    public BigDecimal getExpenditureTotal() {
        return expenditureTotal;
    }

    public void setExpenditureTotal(BigDecimal expenditureTotal) {
        this.expenditureTotal = expenditureTotal;
    }

    public BigDecimal getPlatformCommission() {
        return platformCommission;
    }

    public void setPlatformCommission(BigDecimal platformCommission) {
        this.platformCommission = platformCommission;
    }

    public BigDecimal getPlatformWithdrawal() {
        return platformWithdrawal;
    }

    public void setPlatformWithdrawal(BigDecimal platformWithdrawal) {
        this.platformWithdrawal = platformWithdrawal;
    }

    public BigDecimal getOfflineCommission() {
        return offlineCommission;
    }

    public void setOfflineCommission(BigDecimal offlineCommission) {
        this.offlineCommission = offlineCommission;
    }

    public BigDecimal getOfflineWithdrawal() {
        return offlineWithdrawal;
    }

    public void setOfflineWithdrawal(BigDecimal offlineWithdrawal) {
        this.offlineWithdrawal = offlineWithdrawal;
    }

    public BigDecimal getProcure() {
        return procure;
    }

    public void setProcure(BigDecimal procure) {
        this.procure = procure;
    }
}
