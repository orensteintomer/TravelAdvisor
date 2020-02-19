package travel_advisor.datatypes.google_maps

case class Leg(distance: Distance,
               duration: Duration,
               start_address: String,
               end_address: String,
               steps: Array[Step])
