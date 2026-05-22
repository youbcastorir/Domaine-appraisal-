package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PortfolioDao {
    @Query("SELECT * FROM portfolio_domains ORDER BY domainName ASC")
    fun getAllPortfolio(): Flow<List<PortfolioDomain>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDomain(domain: PortfolioDomain): Long

    @Query("DELETE FROM portfolio_domains WHERE id = :id")
    suspend fun deleteDomain(id: Long)

    @Query("SELECT SUM(appraisedValue) FROM portfolio_domains")
    fun getTotalValueFlow(): Flow<Double?>
}
