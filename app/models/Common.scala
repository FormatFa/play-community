package models

import java.time.{Instant}
import cn.playscala.mongo.annotations.Entity
import play.api.libs.json.{JsArray, JsObject, Json}
import utils.DateTimeUtil



@Entity("common-category")
case class Category(_id: String, name: String, path: String, parentPath: String, index: Int, disabled: Boolean)

// 已整理文档目录
@Entity("doc-catalog")
case class DocCatalog(
  _id: String,
  nodes: JsArray,
  isDefault: Boolean,
  createTime: Instant,
  updateTime: Instant
)


@Entity("common-event")
case class Event(
  _id: String,
  actor: Author,
  action: String,
  resId: String,
  resType: String,
  resTitle: String,
  createTime: Instant
)

@Entity("common-tweet")
case class Tweet(_id: String, author: Author, title: String, content: String, images: List[String], createTime: Instant, voteStat: VoteStat, replyCount: Int, replies: List[Reply]){
  def toJson: JsObject = {
    Json.obj("_id" -> _id, "author" -> author, "content" -> content, "images" -> images, "replyCount" -> replies.size, "voteCount" -> voteStat.count, "time" -> DateTimeUtil.toPrettyString(createTime))
  }
}

@Entity("common-corporation")
case class Corporation(_id: String, title: String, city: String, grade: String, url: String, logo: String, description: String, author: Author, isChinese: Boolean, voteStat: VoteStat, viewStat: ViewStat, active: Boolean, createTime: Instant, updateTime: Instant)

/**
  * 消息提醒
  * @param source system/article/qa
  * @param action view/vote/reply/comment
  */
@Entity("common-message")
case class Message(_id: String, uid: String, source: String, sourceId: String, sourceTitle: String, actor: Author, action: String, content: String, createTime: Instant, read: Boolean)

case class Author(_id: String, login: String, name: String, headImg: String)
case class Reply(_id: String, content: String, editorType: String, author: Author, at: List[String], createTime: Instant, updateTime: Instant, voteStat: VoteStat, comments: List[Comment])
case class Comment(_id: String, content: String, editorType: String, commentator: Author, at: List[String], createTime: Instant, updateTime: Instant)



case class ReplyStat(replyCount: Int, replies: List[Reply], bestReply: Option[Reply], lastReply: Option[Reply])
case class ViewStat(count: Int, bitmap: String)
case class VoteStat(count: Int, bitmap: String)
case class CollectStat(count: Int, bitmap: String)


object Role {
  val USER = "user"
  val ADMIN = "admin"
}