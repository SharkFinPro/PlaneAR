package edu.osu.t22.planear.scenes.pages

// Shared flight data (mirrors g_flightData / g_flightFavorites in AppScenes.cpp)
data class FlightEntry(
    val callsign: String,
    val takeoffTime: String,
    val landingTime: String,
    val planeType: String,
    val airspeed: Int,
    val date: String
)

val flightData: List<FlightEntry> = listOf(
    FlightEntry("AAL1023", "06:15 AM", "09:30 AM", "Boeing 737-800",  452, "02/28/2026"),
    FlightEntry("UAL455",  "07:00 AM", "10:12 AM", "Airbus A320",     430, "02/28/2026"),
    FlightEntry("DAL892",  "08:30 AM", "12:45 PM", "Boeing 767-300",  470, "02/28/2026"),
    FlightEntry("SWA317",  "09:00 AM", "11:20 AM", "Boeing 737 MAX",  440, "02/28/2026"),
    FlightEntry("JBU528",  "10:15 AM", "01:40 PM", "Airbus A321",     460, "02/28/2026"),
    FlightEntry("FDX901",  "05:00 AM", "08:15 AM", "Boeing 777F",     490, "02/27/2026"),
    FlightEntry("UPS234",  "04:30 AM", "07:50 AM", "Boeing 747-8F",   500, "02/27/2026"),
    FlightEntry("AAL2045", "11:00 AM", "02:30 PM", "Airbus A319",     420, "02/27/2026"),
    FlightEntry("UAL718",  "12:30 PM", "04:00 PM", "Boeing 787-9",    480, "02/27/2026"),
    FlightEntry("DAL310",  "01:15 PM", "03:45 PM", "Airbus A330",     475, "02/27/2026")
)

val flightFavorites: MutableList<Boolean> = MutableList(flightData.size) { false }