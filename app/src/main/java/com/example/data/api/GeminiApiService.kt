package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// --- appraisal models stored/received ---

@JsonClass(generateAdapter = true)
data class SimilarSaleJson(
    @Json(name = "domainName") val domainName: String,
    @Json(name = "price") val price: Double,
    @Json(name = "year") val year: Int,
    @Json(name = "platform") val platform: String
)

@JsonClass(generateAdapter = true)
data class AppraisalResultJson(
    @Json(name = "domainName") val domainName: String,
    @Json(name = "isPremium") val isPremium: Boolean,
    @Json(name = "length") val length: Int,
    @Json(name = "extensionPopularity") val extensionPopularity: String, // High, Medium, Low
    @Json(name = "brandability") val brandability: String, // Excellent, Good, Fair
    @Json(name = "commercialIntent") val commercialIntent: String, // High, Medium, Low
    @Json(name = "pronunciability") val pronunciability: String, // Easy, Moderate, Hard
    @Json(name = "estRetailValue") val estRetailValue: Double,
    @Json(name = "estWholesaleValue") val estWholesaleValue: Double,
    @Json(name = "estInvestorValue") val estInvestorValue: Double,
    @Json(name = "confidenceScore") val confidenceScore: Int, // 0 to 100
    @Json(name = "seoBacklinks") val seoBacklinks: Int,
    @Json(name = "seoAuthority") val seoAuthority: Int, // 0 to 100
    @Json(name = "organicTrafficPotential") val organicTrafficPotential: String = "Medium",
    @Json(name = "searchVolume") val searchVolume: Int = 0,
    @Json(name = "cpcValue") val cpcValue: Double = 0.0,
    @Json(name = "similarSales") val similarSales: List<SimilarSaleJson> = emptyList(),
    @Json(name = "marketTrends") val marketTrends: String = "",
    @Json(name = "investmentRecommendation") val investmentRecommendation: String = "",
    @Json(name = "explanationText") val explanationText: String = ""
)

// --- Gemini Request / Response models ---

@JsonClass(generateAdapter = true)
data class Part(
    @Json(name = "text") val text: String
)

@JsonClass(generateAdapter = true)
data class Content(
    @Json(name = "parts") val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    @Json(name = "responseMimeType") val responseMimeType: String? = null,
    @Json(name = "temperature") val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    @Json(name = "contents") val contents: List<Content>,
    @Json(name = "generationConfig") val generationConfig: GenerationConfig? = null,
    @Json(name = "systemInstruction") val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class ResponsePart(
    @Json(name = "text") val text: String? = null
)

@JsonClass(generateAdapter = true)
data class ResponseContent(
    @Json(name = "parts") val parts: List<ResponsePart>? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    @Json(name = "content") val content: ResponseContent? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    @Json(name = "candidates") val candidates: List<Candidate>? = null
)

// --- Retrofit Setup ---

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    // Publicly accessible Moshi instance to serialize / deserialize lists etc.
    val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    val service: GeminiApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
        retrofit.create(GeminiApiService::class.java)
    }
}
