package dilipsuthar.saathi.model

import java.util.*

data class ItemRideHistory constructor(
    val bike_id: String?,
    val ride_date_time: String?,
    val time_stamp: Date?,
    val travel_amount: String?,
    val travel_distance: String?,
    val travel_time: String?
)