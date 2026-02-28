package edu.osu.t22.planear.models
// Temporary mock data to test UI components
object MockFlightRepository {

    fun getFlights(): List<FlightData> = flights

    private val flights = listOf(
        FlightData("AAL1023", "06:15 AM", "09:30 AM", "Boeing 737-800", 452, 35000, "02/28/2026"),
        FlightData("UAL455",  "07:00 AM", "10:12 AM", "Airbus A320",    430, 33000, "02/28/2026"),
        FlightData("DAL892",  "08:30 AM", "12:45 PM", "Boeing 767-300", 470, 37000, "02/28/2026"),
        FlightData("SWA317",  "09:00 AM", "11:20 AM", "Boeing 737 MAX", 440, 34000, "02/28/2026"),
        FlightData("JBU528",  "10:15 AM", "01:40 PM", "Airbus A321",    460, 36000, "02/28/2026"),
        FlightData("FDX901",  "05:00 AM", "08:15 AM", "Boeing 777F",    490, 38000, "02/27/2026"),
        FlightData("UPS234",  "04:30 AM", "07:50 AM", "Boeing 747-8F",  500, 39000, "02/27/2026"),
        FlightData("AAL2045", "11:00 AM", "02:30 PM", "Airbus A319",    420, 32000, "02/27/2026"),
        FlightData("UAL718",  "12:30 PM", "04:00 PM", "Boeing 787-9",   480, 40000, "02/27/2026"),
        FlightData("DAL310",  "01:15 PM", "03:45 PM", "Airbus A330",    475, 37000, "02/27/2026"),
        FlightData("NKS602",  "02:00 PM", "05:10 PM", "Airbus A320neo", 435, 34000, "02/26/2026"),
        FlightData("AAL789",  "03:30 PM", "06:50 PM", "Boeing 757-200", 455, 35000, "02/26/2026"),
        FlightData("SWA142",  "04:00 PM", "06:15 PM", "Boeing 737-700", 425, 33000, "02/26/2026"),
        FlightData("JBU915",  "05:30 PM", "08:45 PM", "Airbus A321neo", 465, 36000, "02/26/2026"),
        FlightData("UAL333",  "06:00 PM", "09:20 PM", "Boeing 737-900", 445, 35000, "02/26/2026"),
        FlightData("DAL567",  "07:15 PM", "10:30 PM", "Airbus A350",    485, 41000, "02/25/2026"),
        FlightData("FDX412",  "08:00 PM", "11:15 PM", "MD-11F",         470, 36000, "02/25/2026"),
        FlightData("AAL1500", "06:45 AM", "10:00 AM", "Boeing 777-200", 495, 39000, "02/25/2026"),
        FlightData("SWA800",  "07:30 AM", "09:45 AM", "Boeing 737-800", 440, 34000, "02/25/2026"),
        FlightData("NKS101",  "08:15 AM", "11:30 AM", "Airbus A321",    450, 35000, "02/25/2026"),
        FlightData("UAL922",  "09:45 AM", "01:00 PM", "Boeing 787-10",  480, 40000, "02/24/2026"),
        FlightData("DAL215",  "10:30 AM", "01:45 PM", "Airbus A220",    415, 31000, "02/24/2026"),
        FlightData("JBU347",  "11:15 AM", "02:30 PM", "Airbus A320",    430, 33000, "02/24/2026"),
        FlightData("AAL650",  "12:00 PM", "03:15 PM", "Boeing 737 MAX", 445, 34000, "02/24/2026"),
        FlightData("SWA999",  "01:30 PM", "03:50 PM", "Boeing 737-800", 435, 33000, "02/24/2026"),
        FlightData("FDX780",  "02:15 PM", "05:30 PM", "Boeing 767-300F",470, 37000, "02/23/2026"),
        FlightData("UAL111",  "03:00 PM", "06:20 PM", "Boeing 757-300", 460, 36000, "02/23/2026"),
        FlightData("DAL444",  "04:45 PM", "08:00 PM", "Airbus A330neo", 478, 38000, "02/23/2026"),
        FlightData("NKS275",  "05:30 PM", "08:45 PM", "Airbus A320neo", 432, 34000, "02/23/2026"),
        FlightData("AAL900",  "06:15 PM", "09:30 PM", "Boeing 777-300", 498, 40000, "02/23/2026")
    )
}
