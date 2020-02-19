package v1.directions

import com.sun.management.VMOption.Origin
import javax.inject.{Inject, Singleton}
import play.api.libs.json.JsValue
import play.api.libs.ws._
import travel_advisor.datatypes._
import travel_advisor.datatypes.google_maps.{Leg, Location, Step}
import travel_advisor.datatypes.weather_api.WeatherResponse
import travel_advisor.utils.JsonUtil

import scala.concurrent.{ExecutionContext, Future}

trait DirectionManager {
  def search(origin: String, destination: String): Future[DirectionsResponse]
  def search2(origin: String, destination: String): Future[DirectionsResponse]
  def search3(origin: String, destination: String): Future[DirectionsResponse]
}

@Singleton
class DirectionManagerImpl @Inject() (ws: WSClient)(implicit ec: ExecutionContext)
  extends DirectionManager {
  var _directions_cache: Map[DirectionRequest, DirectionsResponse] = Map.empty // TODO: change to val, how?
  val _maps_api_url: String = "https://maps.googleapis.com/maps/api/directions/json" // TODO: move to application.conf
  val _weather_api_url: String = "http://api.openweathermap.org/data/2.5/weather" // TODO: move to application.conf


  def search(origin: String, destination: String): Future[DirectionsResponse] = {
      val curr_direction_req = DirectionRequest(origin, destination)

      if (_directions_cache.contains(curr_direction_req)) {
        Future{_directions_cache(curr_direction_req)}
      } else {

        // Make request to external maps api TODO: move to application conf
        val maps_api_req =
          ws.url(_maps_api_url)
          .addQueryStringParameters("key" -> "AIzaSyCfuAAdJCxPkq5tUHUa8zj-FXegzSpFp0Q")
          .addQueryStringParameters("origin" -> origin)
          .addQueryStringParameters("destination" -> destination).get()

        // Deserializes google maps object
        val google_maps_res: Future[Leg] = maps_api_req.map(response => {
          val route = (response.json \ "routes")(0)
          val leg: JsValue = (route \ "legs")(0)
          JsonUtil.fromJson[Leg](leg.toString())
        })

        // When get google maps response builds our service response
        val res = google_maps_res.map(leg => {

          // Creates travel advisor (ie -> my service) response
          val origin = leg.start_address
          val destination = leg.end_address
          val totalDurationInMinutes = leg.duration.value / 60
          val totalDistanceInMeters = leg.distance.value
          val steps: Array[Future[DirectionStep]] = leg.steps.map(step => {
            val duration = step.duration.text
            val end_location = step.end_location
            val html_instructions = step.html_instructions
            val weather_api_req = ws.url(_weather_api_url)
              .addQueryStringParameters("lat" -> step.end_location.lat.toString)
              .addQueryStringParameters("lon" -> step.end_location.lng.toString)
              .addQueryStringParameters("d" -> "524901")
              .addQueryStringParameters("APPID" -> "ab5ed931736d6d7564e7b04477169958").get()

            val weather_api_res: Future[WeatherResponse] = weather_api_req.map(response => {
              JsonUtil.fromJson[WeatherResponse](response.json.toString())
            })
            val final_weather: Future[DirectionWeatherResponse] = weather_api_res.map(weather => {
              val celsius_temp = weather.main.temp - 273.15
              DirectionWeatherResponse(celsius_temp, weather.weather(0).description)
            })

            final_weather.map(we => DirectionStep(duration, end_location, html_instructions, we))
          })

          // When all steps ready(after all weather api requests)
          val travel_advisor_response: Future[DirectionsResponse] = Future.sequence(steps.toList).map(all_steps => {
            val weather_in_range = all_steps
              .count(d => d.weather.celsiusTemp > 30 || d.weather.celsiusTemp < 0) == 0

            val travelAdvice =
              if (weather_in_range && totalDurationInMinutes <= 60 * 3) {
                "Yes"
              } else {
                "No"
              }

            DirectionsResponse(origin,
                               destination,
                               totalDurationInMinutes,
                               totalDistanceInMeters,
                               all_steps.toArray,
                               travelAdvice)
          })
          travel_advisor_response.map(r => r)
        })
        res.flatMap(r => {
          r.onComplete(finished_obj => {
            _directions_cache = _directions_cache + (curr_direction_req -> finished_obj.get)
          })
          r
        })
      }
    }

  def search2(origin: String, destination: String): Future[DirectionsResponse] = {
    val curr_direction_req = DirectionRequest(origin, destination)

    // Checks if in cache
    if (_directions_cache.contains(curr_direction_req)) {
      Future{_directions_cache(curr_direction_req)}
    } else {

      // Make request to external maps api TODO: move to application conf
      val maps_api_req =
        ws.url(_maps_api_url)
          .addQueryStringParameters("key" -> "AIzaSyCfuAAdJCxPkq5tUHUa8zj-FXegzSpFp0Q")
          .addQueryStringParameters("origin" -> origin)
          .addQueryStringParameters("destination" -> destination).get()

      // Deserializes google maps object
      val google_maps_res: Future[Leg] = maps_api_req.map(response => {
        val route = (response.json \ "routes")(0)
        val leg: JsValue = (route \ "legs")(0)
        JsonUtil.fromJson[Leg](leg.toString())
      })

      // builds our service response
      val res: Future[DirectionsResponse] = google_maps_res.flatMap(leg => {

        // Creates travel advisor (ie -> my service) response
        val origin = leg.start_address
        val destination = leg.end_address
        val totalDurationInMinutes = leg.duration.value / 60
        val totalDistanceInMeters = leg.distance.value
        val steps: Array[Future[DirectionStep]] = leg.steps.map(step => {
          val duration = step.duration.text
          val end_location = step.end_location
          val html_instructions = step.html_instructions
          val weather_api_req = ws.url(_weather_api_url)
            .addQueryStringParameters("lat" -> step.end_location.lat.toString)
            .addQueryStringParameters("lon" -> step.end_location.lng.toString)
            .addQueryStringParameters("d" -> "524901")
            .addQueryStringParameters("APPID" -> "ab5ed931736d6d7564e7b04477169958").get()

          val weather_api_res: Future[WeatherResponse] = weather_api_req.map(response => {
            JsonUtil.fromJson[WeatherResponse](response.json.toString())
          })
          val final_weather: Future[DirectionWeatherResponse] = weather_api_res.map(weather => {
            val celsius_temp = weather.main.temp - 273.15
            DirectionWeatherResponse(celsius_temp, weather.weather(0).description)
          })

          final_weather.map(we => DirectionStep(duration, end_location, html_instructions, we))
        })

        // When all steps ready(after all weather api requests)
        val travel_advisor_response: Future[DirectionsResponse] = Future.sequence(steps.toList).map(all_steps => {
          val weather_in_range = all_steps
            .count(d => d.weather.celsiusTemp > 30 || d.weather.celsiusTemp < 0) == 0

          val travelAdvice =
            if (weather_in_range && totalDurationInMinutes <= 60 * 3) {
              "Yes"
            } else {
              "No"
            }

          DirectionsResponse(origin,
            destination,
            totalDurationInMinutes,
            totalDistanceInMeters,
            all_steps.toArray,
            travelAdvice)
        })
        travel_advisor_response
      })

      // Adds to cache TODO: change to functional
      res.onComplete(finished_obj => {
        _directions_cache = _directions_cache + (curr_direction_req -> finished_obj.get)
      })

      res
    }
  }

  def search3(origin: String, destination: String): Future[DirectionsResponse] = {
    val curr_direction_req = DirectionRequest(origin, destination)

    // Checks if in cache
    if (_directions_cache.contains(curr_direction_req)) {
      Future{_directions_cache(curr_direction_req)}
    } else {

      // Make request to external maps api TODO: move to application conf
      val maps_api_request =
        ws.url(_maps_api_url)
          .addQueryStringParameters("key" -> "AIzaSyCfuAAdJCxPkq5tUHUa8zj-FXegzSpFp0Q")
          .addQueryStringParameters("origin" -> origin)
          .addQueryStringParameters("destination" -> destination).get()

      for {
        leg <- parse_to_google_leg_obj(maps_api_request)
        res <- create_respond_obj(leg)
      } yield {
        _directions_cache = _directions_cache + (curr_direction_req -> res)
        res
      }
    }
  }

  private def parse_to_google_leg_obj(maps_req: Future[WSRequest#Self#Self#Self#Response]): Future[Leg] = {
    maps_req.map(response => {
      val route = (response.json \ "routes")(0)
      val leg: JsValue = (route \ "legs")(0)
      JsonUtil.fromJson[Leg](leg.toString())
    })
  }

  private def create_respond_obj(google_leg: Leg): Future[DirectionsResponse] = {

    // Creates travel advisor (ie -> my service) response
    val origin = google_leg.start_address
    val destination = google_leg.end_address
    val totalDurationInMinutes = google_leg.duration.value / 60
    val totalDistanceInMeters = google_leg.distance.value

    val steps = google_leg.steps.map(step => {
      for {
        weather_req <- create_weather_api_request(step)
        weather_obj <- parse_to_weather_api_object(weather_req)
      } yield DirectionStep(step.duration.text, step.end_location, step.html_instructions, weather_obj)
    })

    create_final_object(Future.sequence(steps.toList), origin, destination, totalDurationInMinutes, totalDistanceInMeters)
  }

  private def create_weather_api_request(google_step: Step): Future[WSRequest#Self#Self#Self#Self#Response] = {
    ws.url(_weather_api_url)
      .addQueryStringParameters("lat" -> google_step.end_location.lat.toString)
      .addQueryStringParameters("lon" -> google_step.end_location.lng.toString)
      .addQueryStringParameters("d" -> "524901")
      .addQueryStringParameters("APPID" -> "ab5ed931736d6d7564e7b04477169958").get()
  }

  private def parse_to_weather_api_object(weather_res: WSRequest#Self#Self#Self#Self#Response):Future[DirectionWeatherResponse] = {
    val weather_api_res = JsonUtil.fromJson[WeatherResponse](weather_res.json.toString())
    val celsius_temp = weather_api_res.main.temp - 273.15
    Future{DirectionWeatherResponse(celsius_temp, weather_api_res.weather(0).description)}
  }

  private def create_final_object(all_steps: Future[List[DirectionStep]],
                                  origin: String,
                                  destination: String,
                                  totalDurationInMinutes: Long,
                                  totalDistanceInMeters: Long) = {
    all_steps.map(ready_steps => {
      val weather_in_range = ready_steps
        .count(d => d.weather.celsiusTemp > 30 || d.weather.celsiusTemp < 0) == 0

      val travelAdvice =
        if (weather_in_range && totalDurationInMinutes <= 60 * 3) {
          "Yes"
        } else {
          "No"
        }

      DirectionsResponse(origin,
        destination,
        totalDurationInMinutes,
        totalDistanceInMeters,
        ready_steps.toArray,
        travelAdvice)
    })
  }
}
