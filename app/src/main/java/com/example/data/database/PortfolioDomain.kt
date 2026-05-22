package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "portfolio_domains")
data class PortfolioDomain(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val domainName: String,
    val purchasePrice: Double? = null,
    val purchaseDate: Long? = null,
    val appraisedValue: Double = 0.0,
    val registrar: String = "Unknown",
    val expiryDate: Long? = null,
    val notes: String = "",
    val currency: String = "USD"
)
