package edu.osu.t22.planear.models

import edu.osu.t22.planear.adsb.Aircraft

// Temporary mock data to test UI components
object MockFlightRepository {

    fun getFlights(): List<Aircraft> = flights

    private val flights = listOf(
        Aircraft("A5DEE3", "AAL1023", "B737", 452.0, 35000.0),
        Aircraft("A4C849", "UAL455",  "A320",    430.0, 33000.0),
        Aircraft("A335B8", "DAL892",  "B767", 470.0, 37000.0),
        Aircraft("A67B81", "SWA317",  "B38M", 440.0, 34000.0),
        Aircraft("AC08D4", "JBU528",  "A321",    460.0, 36000.0),
        Aircraft("A5DEE3", "FDX901",  "B777F",    490.0, 38000.0),
        Aircraft("A5DEE3", "UPS234",  "B747",  500.0, 39000.0),
        Aircraft("A5DEE3", "AAL2045", "A319",    420.0, 32000.0),
        Aircraft("A5DEE3", "UAL718",  "B787",   480.0, 40000.0),
        Aircraft("A5DEE3", "DAL310",  "A330",    475.0, 37000.0),
        Aircraft("A5DEE3", "NKS602",  "A320neo", 435.0, 34000.0),
        Aircraft("A5DEE3", "AAL789",  "B757", 455.0, 35000.0),
        Aircraft("A5DEE3", "SWA142",  "B737", 425.0, 33000.0),
        Aircraft("A5DEE3", "JBU915",  "A321neo", 465.0, 36000.0),
        Aircraft("A5DEE3", "UAL333",  "B737", 445.0, 35000.0),
        Aircraft("A5DEE3", "DAL567",  "A350",    485.0, 41000.0),
        Aircraft("A5DEE3", "FDX412",  "MD11F",         470.0, 36000.0),
        Aircraft("A5DEE3", "AAL1500", "B777", 495.0, 39000.0),
        Aircraft("A5DEE3", "SWA800",  "B737", 440.0, 34000.0),
        Aircraft("A5DEE3", "NKS101",  "A321",    450.0, 35000.0),
        Aircraft("A5DEE3", "UAL922",  "B787",  480.0, 40000.0),
        Aircraft("A5DEE3", "DAL215",  "A220",    415.0, 31000.0),
        Aircraft("A5DEE3", "JBU347",  "A320",    430.0, 33000.0),
        Aircraft("A5DEE3", "AAL650",  "B737M", 445.0, 34000.0),
        Aircraft("A5DEE3", "SWA999",  "B737", 435.0, 33000.0),
        Aircraft("A5DEE3", "FDX780",  "B767",470.0, 37000.0),
        Aircraft("A5DEE3", "UAL111",  "B757", 460.0, 36000.0),
        Aircraft("A5DEE3", "DAL444",  "A330neo", 478.0, 38000.0),
        Aircraft("A5DEE3", "NKS275",  "A320neo", 432.0, 34000.0),
        Aircraft("A5DEE3", "AAL900",  "B777", 498.0, 40000.0)
    )
}
