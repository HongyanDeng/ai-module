package com.example.llm.entity;

import lombok.Data;
import lombok.Getter;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;


@Data
@Entity
@Table(name = "case_arc_info")
@EntityListeners(AuditingEntityListener.class)
public class CaseArcInfo implements Serializable {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "uuid", columnDefinition = "VARCHAR(255)", nullable = false)
    @Getter
    private String uuid;

    private static final long serialVersionUID = 776037426813011737L;

    /**
     * 归档机构编号
     */
    private String arcInstNo;
    /**
     * 归档机构名称
     */
    private String arcInstName;
    /**
     * 档案号
     */
    private String fileNo;
    /**
     * 经办机构编号
     */
    private String optinsNo;
    /**
     * 经办时间
     */
    private String optTime;
    /**
     * 经办人姓名
     */
    private String opterName;
    /**
     * 开放利用标识
     */
    private String openUtilSign;
    /**
     * 文件总数
     */
    private String fileTotai;
    /**
     * 文件页数
     */
    private String fileTotaiP;
    /**
     * 业务名称
     */
    private String bizName;
    /**
     * 业务受理时间
     */
    private String bizAcTime;
    /**
     * 业务办结时间
     */
    private String bizFnsTime;
    /**
     * 单位编号
     */
    private String empNo;
    /**
     * 单位名称
     */
    private String empName;
    /**
     * 证件号码
     */
    private String certno;
    /**
     * 人员姓名
     */
    private String psnName;
    /**
     * 业务编号（活动标识符）
     */
    private String bizNo;
    /**
     * 业务办结接收表ID
     */
    private String bizFnsRecId;
    /**
     * 案件号
     */
    private Integer caseNo;
    /**
     * 卷宗归档信息表ID
     */
    private String recArcInfoId;
    /**
     * 归档状态
     */
    private String archStas;
    /**
     * 页码范围
     */
    private String pageRange;
    /**
     * 备注
     */
    private String memo;
    /**
     * 人员编号
     */
    private String psnNo;
    /**
     * 归档时间
     */
    private Date fileArcTime;
    /**
     * 档案状态
     */
    private String fileStas;
    /**
     * 存储类型
     */
    private String storeType;
    /**
     * 归档类型(1、单位  2、个人)
     */
    private String archType;
    /**
     * 归档人
     */
    private String archr;
    /**
     * 检测结果 0-未检测 1-合格 2-不合格
     */
    private String checkResult;
    /**
     * 检测失败原因
     */
    private String checkFailReason;

}
