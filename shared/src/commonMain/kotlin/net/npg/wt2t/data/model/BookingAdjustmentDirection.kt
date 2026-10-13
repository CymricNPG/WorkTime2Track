package net.npg.wt2t.data.model

/** Identifies the direction for moving a booking boundary. */
enum class BookingAdjustmentDirection(
    internal val multiplier: Int,
) {
    EARLIER(-1),
    LATER(1),
}
