package travel_advisor.datatypes

case class DirectionsResponse(origin: String,
                              destination: String,
                              totalDurationInMinutes: Long,
                              totalDistanceInMeters: Long,
                              steps: Array[DirectionStep],
                              travelAdvice: String)
