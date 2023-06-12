package com.happymeerkat.avocado.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(
    tableName = "list",
)
data class ListItem(
    @PrimaryKey
    val id: Int? = null,
    val title: String,
    val description: String? = null,
    val category: String? = null,
    val dateMade: Long? = null,
    val dateDue: Long? = null,
)