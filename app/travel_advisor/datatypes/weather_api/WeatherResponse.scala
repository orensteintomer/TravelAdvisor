package travel_advisor.datatypes.weather_api

case class WeatherApiMainData(temp: Double)
case class WeatherDescription(description: String)
case class WeatherResponse(main: WeatherApiMainData, weather: Array[WeatherDescription])
