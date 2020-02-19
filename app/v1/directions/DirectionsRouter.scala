package v1.directions

import javax.inject.Inject
import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter
import play.api.routing.sird._
import v1.directions.DirectionsController

/**
 * Routes and URLs to the DirectionsController controller.
 */
class DirectionsRouter @Inject()(controller: DirectionsController) extends SimpleRouter {
  val prefix = "/v1/directions"

  override def routes: Routes = {

    case GET(p"/" ? q_?"origin=$origin" & q_?"destination=$destination") =>
      controller.getDirections(origin.get, destination.get)
  }
}
