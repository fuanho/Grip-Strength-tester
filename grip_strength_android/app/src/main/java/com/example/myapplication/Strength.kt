package com.example.myapplication

import java.util.*
import kotlin.collections.ArrayList

data class Strength(
    val date: Date = Date(),
    val username: String = "",
    val strength_data: ArrayList<Double> = ArrayList()
)
