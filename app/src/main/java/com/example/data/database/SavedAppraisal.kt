package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_appraisals")
data class SavedAppraisal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val domainName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isPremium: Boolean = false,
    val length: Int = 0,
    val extensionPopularity: String = "Medium",
    val brandability: String = "Fair",
    val commercialIntent: String = "Medium",
    val pronunciability: String = "Easy",
    val estRetailValue: Double = 0.0,
    val estWholesaleValue: Double = 0.0,
    val estInvestorValue: Double = 0.0,
    val confidenceScore: Int = 50,
    val seoBacklinks: Int = 0,
    val seoAuthority: Int = 0,
    val searchVolume: Int = 0,
    val cpcValue: Double = 0.0,
    val similarSalesJson: String = "[]", // Keep similar sales as JSON serialized string
    val marketTrends: String = "",
    val investmentRecommendation: String = "",
    val explanationText: String = "",
    val currency: String = "USD"
)
