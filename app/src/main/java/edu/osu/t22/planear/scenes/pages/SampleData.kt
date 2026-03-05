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
    FlightEntry("AAL1023", "06:15 AM", "09:30 AM", "Boeing 737-800",   452, "02/28/2026"),
    FlightEntry("UAL455",  "07:00 AM", "10:12 AM", "Airbus A320",      430, "02/28/2026"),
    FlightEntry("DAL892",  "08:30 AM", "12:45 PM", "Boeing 767-300",   470, "02/28/2026"),
    FlightEntry("SWA317",  "09:00 AM", "11:20 AM", "Boeing 737 MAX",   440, "02/28/2026"),
    FlightEntry("JBU528",  "10:15 AM", "01:40 PM", "Airbus A321",      460, "02/28/2026"),
    FlightEntry("FDX901",  "05:00 AM", "08:15 AM", "Boeing 777F",      490, "02/27/2026"),
    FlightEntry("UPS234",  "04:30 AM", "07:50 AM", "Boeing 747-8F",    500, "02/27/2026"),
    FlightEntry("AAL2045", "11:00 AM", "02:30 PM", "Airbus A319",      420, "02/27/2026"),
    FlightEntry("UAL718",  "12:30 PM", "04:00 PM", "Boeing 787-9",     480, "02/27/2026"),
    FlightEntry("DAL310",  "01:15 PM", "03:45 PM", "Airbus A330",      475, "02/27/2026"),
    FlightEntry("NKS602",  "02:00 PM", "05:10 PM", "Airbus A320neo",   435, "02/26/2026"),
    FlightEntry("AAL789",  "03:30 PM", "06:50 PM", "Boeing 757-200",   455, "02/26/2026"),
    FlightEntry("SWA142",  "04:00 PM", "06:15 PM", "Boeing 737-700",   425, "02/26/2026"),
    FlightEntry("JBU915",  "05:30 PM", "08:45 PM", "Airbus A321neo",   465, "02/26/2026"),
    FlightEntry("UAL333",  "06:00 PM", "09:20 PM", "Boeing 737-900",   445, "02/26/2026"),
    FlightEntry("DAL567",  "07:15 PM", "10:30 PM", "Airbus A350",      485, "02/25/2026"),
    FlightEntry("FDX412",  "08:00 PM", "11:15 PM", "MD-11F",           470, "02/25/2026"),
    FlightEntry("AAL1500", "06:45 AM", "10:00 AM", "Boeing 777-200",   495, "02/25/2026"),
    FlightEntry("SWA800",  "07:30 AM", "09:45 AM", "Boeing 737-800",   440, "02/25/2026"),
    FlightEntry("NKS101",  "08:15 AM", "11:30 AM", "Airbus A321",      450, "02/25/2026"),
    FlightEntry("UAL922",  "09:45 AM", "01:00 PM", "Boeing 787-10",    480, "02/24/2026"),
    FlightEntry("DAL215",  "10:30 AM", "01:45 PM", "Airbus A220",      415, "02/24/2026"),
    FlightEntry("JBU347",  "11:15 AM", "02:30 PM", "Airbus A320",      430, "02/24/2026"),
    FlightEntry("AAL650",  "12:00 PM", "03:15 PM", "Boeing 737 MAX",   445, "02/24/2026"),
    FlightEntry("SWA999",  "01:30 PM", "03:50 PM", "Boeing 737-800",   435, "02/24/2026"),
    FlightEntry("FDX780",  "02:15 PM", "05:30 PM", "Boeing 767-300F",  470, "02/23/2026"),
    FlightEntry("UAL111",  "03:00 PM", "06:20 PM", "Boeing 757-300",   460, "02/23/2026"),
    FlightEntry("DAL444",  "04:45 PM", "08:00 PM", "Airbus A330neo",   478, "02/23/2026"),
    FlightEntry("NKS275",  "05:30 PM", "08:45 PM", "Airbus A320neo",   432, "02/23/2026"),
    FlightEntry("AAL900",  "06:15 PM", "09:30 PM", "Boeing 777-300",   498, "02/23/2026")
)
