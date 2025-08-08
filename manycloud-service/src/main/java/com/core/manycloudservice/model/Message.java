package com.core.manycloudservice.model;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@Data
//指定xml的根节点
@XmlRootElement(name = "xml")
//指定Xml映射的生效范围
@XmlAccessorType(XmlAccessType.FIELD)
public class Message {

    /**
     * 开发者微信号
     */
    //指定Xml映射节点名
    @XmlElement(name = "ToUserName")
    protected String toUserName;
    /**
     * 发送方账号（一个OpenID）
     */
    @XmlElement(name = "FromUserName")
    protected String fromUserName;
    /**
     * 消息类型，文本为text
     */
    @XmlElement(name = "MsgType")
    protected String msgType;
    /**
     * 消息id，64位整型
     */
    @XmlElement(name = "MsgId")
    protected String msgId;
    /**
     * 消息的数据ID（消息如果来自文章时才有）
     */
    @XmlElement(name = "MsgDataId")
    protected String msgDataId;
    /**
     * 多图文时第几篇文章，从1开始（消息如果来自文章时才有）
     */
    @XmlElement(name = "Idx")
    protected String idx;
    /**
     * 消息创建时间 （整型）
     */
    @XmlElement(name = "CreateTime")
    protected long createTime;
    /**
     * 文本消息内容
     */
    @XmlElement(name = "Content")
    private String content;

    /**
     * 通过素材管理中的接口上传多媒体文件，得到的id
     */
    @XmlElement(name = "MediaId")
    private String mediaId;

    /**
     * 事件标签
     */
    @XmlElement(name = "Event")
    private String event;

    /**
     * 事件Key
     */
    @XmlElement(name = "EventKey")
    private String eventKey;

    /** 图文消息标题 **/
    @XmlElement(name = "Title")
    private String title;

    /** 图文消息描述 **/
    @XmlElement(name = "Description")
    private String description;

    /** 图文消息跳转链接 **/
    @XmlElement(name = "Url")
    private String url;

    /** 图文消息个数 **/
    @XmlElement(name = "ArticleCount")
    private String articleCount;

    /** 图文消息信息 **/
    @XmlElement(name = "Articles")
    private String Articles;

    /** 图片链接 **/
    @XmlElement(name = "PicUrl")
    private String picUrl;
}
