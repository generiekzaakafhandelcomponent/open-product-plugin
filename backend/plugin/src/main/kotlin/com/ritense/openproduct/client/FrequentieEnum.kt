package com.ritense.openproduct.client

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

enum class FrequentieEnum(
    @get:JsonValue val value: String,
) {
    EENMALIG("eenmalig"),
    JAARLIJKS("jaarlijks"),
    MAANDELIJKS("maandelijks"),
    ;

    companion object {
        /**
         * Open Producten returns an empty string when a product has no frequency; treat that as no value.
         */
        @JvmStatic
        @JsonCreator
        fun fromValue(value: String?): FrequentieEnum? {
            if (value.isNullOrBlank()) return null
            return entries.firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Onbekende frequentie: '$value'")
        }
    }
}
