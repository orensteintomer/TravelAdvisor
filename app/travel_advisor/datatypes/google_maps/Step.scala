package travel_advisor.datatypes.google_maps

case class Step(distance: Distance,
                duration: Duration,
                end_location: Location,
                html_instructions: String)
