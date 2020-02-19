package travel_advisor.datatypes

import travel_advisor.datatypes.google_maps.Location
import travel_advisor.datatypes.weather_api.WeatherResponse

case class DirectionStep(duration: String,
                         end_location: Location,
                         html_instructions: String,
                         weather: DirectionWeatherResponse)
