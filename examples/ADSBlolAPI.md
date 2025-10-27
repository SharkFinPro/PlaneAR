# adsb.lol API reference

As adsb.lol does not provide much documentation here are some examples to base calls from:

Example API result:
```json
{
    "msg": string, // Status from API (Ex: "No error")
    "now": number, // Timestamp of when file was generated
    "ctime": number, // Cache timestamp (when data was cached)
    "ptime": number, // Server processing time
    "total": number, // Amount of aircraft returned
    "ac": [ // list of aircraft data
        {
            "hex": string, // ICAO 24-bit address
            "type": string, // source/type of the position/message
            "flight": string, // callsign
            "r": string, // regristration
            "t": string, // ICAO aircraft type designator
            "alt_baro": number, // barometric altitude (feet) (MSL or reported?)
            "alt_geom": number, // geometric altitude (feet)
            "gs": number, // ground speed (knots)
            "track": number, // track over ground, direction of motion
            "baro_rate": number, // rate of change of barometric altitude (feet per minute)
            "squawk": string, // squawk code
            "emergency": string, // emergency status
            "category": string, // emitter category
            "nav_qnh": number, // altimeter setting (hPa / mbar)
            "nav_altitude_mcp": number, // selected altitude for autopilot (feet)
            "nav_heading": number, // selected heading for autopilot
            "lat": number, // latitude (decimal degrees)
            "lon": number, // longitude (decimal degrees)
            "nic": number, // Navigation Integrity Category
            "rc": number, // Radius of Containment (meters)
            "seen_pos": number, // seconds ago the position was last updated
            "version": number, // ADS-B version number (2)
            "nic_baro": number, // Navigation Integrity Category for barometric altitude
            "nac_p": number, // Navigation Accuracy Category for Position
            "nac_v": number, // Navigation Accuracy Category for Velocity
            "sil": number, // Source Integrity Level
            "sil_type": string, // how the SIL is derived/updated
            "gva": number, // Geometric Vertical Accuracy
            "sda": number, // System Design Assurance
            "alert": number, // Flight status alert bit
            "spi": number, // Special Position Identification bit
            "mlat": array, // multilateration derived data/positions for the aircraft
            "tisb": array, // ground broadcast of non-ADS-B targets
            "messages": number, // total number of Mode-S/ADS-B messages received from this aircraft
            "seen": number, // seconds ago any message (not necessarily a position) was last received from this aircraft
            "rssi": number, // signal strength (dBFS)
        }
    ]
}
```

Get all aircraft near me:

https://api.adsb.lol/v2/point/{lat}/{lon}/{range}

Example usage: "All aicraft 250nm around 51.89508, 2.79437"

https://api.adsb.lol/v2/point/51.89508/2.79437/250

Result: (for single test) 116 aircraft near 51.89508, 2.79437

Limits/Units:
- Range <= 250nm
- Lat lon in decimal degrees
- -90 < lat < 90
- -180 < lon < 180

Get aircraft from callsign:

https://api.adsb.lol/v2/callsign/{callsign}

Example usage: "Where is Alaska Airlines 590?"

https://api.adsb.lol/v2/callsign/ASA590

Result: Single aircraft with "flight" as "ASA590"