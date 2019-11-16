package controllers

import cn.playscala.mongo.Mongo
import infrastructure.repository.mongo.{MongoBoardRepository, MongoCategoryRepository, MongoResourceRepository, MongoUserRepository}
import javax.inject._
import models._
import play.api.data.Form
import play.api.data.Forms.{tuple, _}
import play.api.libs.json.{JsArray, Json, Writes}
import play.api.libs.json.Json.obj
import play.api.mvc._
import services.{CategoryService, CommonService, EventService}
import utils.{AppUtil, BoardUtil, RequestHelper}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BoardController @Inject()(cc: ControllerComponents, categoryService: CategoryService, boardRepo: MongoBoardRepository, resourceRepo: MongoResourceRepository, userRepo: MongoUserRepository, categoryRepo: MongoCategoryRepository)(implicit ec: ExecutionContext, parser: BodyParsers.Default) extends AbstractController(cc) {

  // 分页大小
  val PAGE_SIZE = 15

  /**
    * 按分页查看资源
    */
  def index(path: String, resType: String, status: String, page: Int) = Action.async { implicit request: Request[AnyContent] =>
    val cPage = AppUtil.parsePage(page)
    var q = Json.obj("categoryPath" -> Json.obj("$regex" -> s"^${path}"))

    resType match {
      case t if t.trim != "" => q ++= Json.obj("resType" -> t.trim)
      case _ =>
    }

    status match {
      case "0" =>
      case "1" =>
        q ++= Json.obj("closed" -> false)
      case "2" =>
        q ++= Json.obj("closed" -> true)
      case "3" =>
        q ++= Json.obj("recommended" -> true)
      case _ =>
    }

    boardRepo.findByPath(path) flatMap {
      case Some(board) =>
        for {
          resources <- resourceRepo.findList(q, Json.obj("createTime" -> -1), (cPage-1) * PAGE_SIZE, PAGE_SIZE)
          topViewResources <- resourceRepo.findTopViewList(resType, 10)
          topReplyResources <- resourceRepo.findTopReplyList(resType, 10)
          categoryList <- categoryRepo.findAllChildren(path)
          total <- resourceRepo.count(q)
        } yield {
          if (total > 0 && cPage > math.ceil(1.0*total/PAGE_SIZE).toInt) {
            Redirect(s"/${resType}s")
          } else {
            Ok(views.html.board.index(board, path, resType, status, resources, topViewResources, topReplyResources, categoryList, cPage, total.toInt))
          }
        }
      case None => Future.successful(Ok(views.html.message("系统提示", "该版块不存在！")))
    }
  }

  def viewAllBoards() = Action.async { implicit request: Request[AnyContent] =>
    boardRepo.findAll().map { list =>
      Ok(views.html.board.viewAllBoards(list))
    }
  }

  def recordTraffic(boardPath: String) = checkLogin.async { implicit request: Request[AnyContent] =>
    val uid = RequestHelper.getUid
    boardRepo.recordTraffic(boardPath, uid).map {
      case true => Ok(Json.obj("code" -> 0))
      case true => Ok(Json.obj("code" -> 1, "message" -> "error"))
    }
  }

  def categoryTree(defaultSelected: String, callback: String) = Action.async { implicit request: Request[AnyContent] =>
    for{
      categories <- categoryRepo.findAllList()
    } yield {
      implicit val categoryWrites = new Writes[Category] {
        def writes(c: Category) = Json.obj(
          "id"  -> c.path,
          "data"  -> Json.obj("path" -> c.path, "parentPath" -> c.parentPath, "disabled" -> c.disabled),
          "text"  -> c.name,
          "type"  -> (if(c.disabled){"gray"} else {"default"}),
          "state" -> Json.obj(
            "opened" -> (if(c._id == "root"){ "opened" } else { "" })
          ),
          "children" -> categories.filter(_.parentPath == c.path).map(n => writes(n))
        )
      }

      val data = JsArray(categories.map(c => Json.toJson(c)(categoryWrites)))
      Ok(views.html.board.categoryTree(defaultSelected, data, callback))
    }
  }

  def categoryTreeJson() = Action.async { implicit request: Request[AnyContent] =>
    for{
      categories <- categoryRepo.findAllList()
    } yield {
      implicit val categoryWrites = new Writes[Category] {
        def writes(c: Category) = Json.obj(
          "id"  -> c._id,
          "data"  -> Json.obj("path" -> c.path, "parentPath" -> c.parentPath, "disabled" -> c.disabled),
          "text"  -> c.name,
          "type"  -> (if(c.disabled){"gray"} else {"default"}),
          "state" -> Json.obj(
            "opened" -> (if(c._id == "root"){ "opened" } else { "" })
          ),
          "children" -> categories.filter(_.parentPath == c.path).map(n => writes(n))
        )
      }

      Ok(JsArray(categories.map(c => Json.toJson(c)(categoryWrites))))
        .as("application/json; charset=utf-8")
    }
  }

  /**
   *  根据分类路径查询分类名称
   */
  def getPathName(path: String) = Action.async { implicit request =>
    boardRepo.findBoardCategoryList(path) map { categories =>
      println(path + ": " + categories)
      val idPathToNamePathMap = categoryService.getIdPathToNamePathMap(categories)
      Ok(idPathToNamePathMap.get(path).getOrElse("-")).as("text/plain; charset=UTF-8")
    }

  }

}
