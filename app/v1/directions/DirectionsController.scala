package v1.directions

import javax.inject.Inject
import play.api.libs.json.Json
import play.api.mvc._
import travel_advisor.utils.JsonUtil

import scala.concurrent._

class DirectionsController @Inject()(val controllerComponents: ControllerComponents,
                                     val directionManager: DirectionManager)
                                    (implicit ec: ExecutionContext) extends BaseController {

  def getDirections(origin: String, destination: String): Action[AnyContent] = Action.async { implicit request =>
    directionManager.search3(origin, destination).map { directions =>
      println(directions)
      Ok(JsonUtil.toJson(directions))
    }
  }
}
