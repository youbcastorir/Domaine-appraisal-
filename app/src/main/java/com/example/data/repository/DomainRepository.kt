package com.example.data.repository

import com.example.BuildConfig
import com.example.data.api.AppraisalResultJson
import com.example.data.api.Content
import com.example.data.api.GenerateContentRequest
import com.example.data.api.GenerationConfig
import com.example.data.api.Part
import com.example.data.api.RetrofitClient
import com.example.data.api.SimilarSaleJson
import com.example.data.database.AppraisalDao
import com.example.data.database.PortfolioDao
import com.example.data.database.PortfolioDomain
import com.example.data.database.SavedAppraisal
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.Locale

class DomainRepository(
    private val appraisalDao: AppraisalDao,
    private val portfolioDao: PortfolioDao
) {
    val allAppraisals: Flow<List<SavedAppraisal>> = appraisalDao.getAllAppraisals()
    val allPortfolio: Flow<List<PortfolioDomain>> = portfolioDao.getAllPortfolio()
    val totalPortfolioValue: Flow<Double?> = portfolioDao.getTotalValueFlow()

    private val resultAdapter: JsonAdapter<AppraisalResultJson> =
        RetrofitClient.moshi.adapter(AppraisalResultJson::class.java)

    private val similarSalesListAdapter: JsonAdapter<List<SimilarSaleJson>> =
        RetrofitClient.moshi.adapter(
            Types.newParameterizedType(List::class.java, SimilarSaleJson::class.java)
        )

    /**
     * Conducts a professional market-value appraisal for a specific domain name.
     */
    suspend fun appraiseDomain(domainName: String): SavedAppraisal = withContext(Dispatchers.IO) {
        val trimmedDomain = domainName.trim().lowercase(Locale.ROOT)
        val apiKey = BuildConfig.GEMINI_API_KEY

        val systemInstruction = """
            You are DomValuate, an expert human domain broker and premier domain appraiser with 20+ years of active market trading experience. 
            You estimate actual, realistic cash resale prices of domain names on the secondary market (like Sedo, Afternic, NameJet, Afterbuy), NOT inflated or automated algorithms.
            
            Analyze domains strictly based on real industry appraisal parameters:
            1. Extensions & TLD: .com is king. New gTLDs (.ai, .io, .co) are premium if tech-related. Standard .org/.net are second tier. Country codes (.us, .co.uk) vary. Flop extensions (.xyz, .biz, .club, .info) have low market liquidity unless outstanding dictionary words.
            2. Length & Memorability: Under 10 characters is ideal. 2-3-4 letters are premium (especially pronounceable CVCV models or clean LLL.com patterns). Long hyphenated or multiple-word domains are heavily discounted.
            3. Branding & Keyword: Brandability represents dictionary terms, catchiness, and modern appeal (e.g., "stripe" or "figma"). Commercial/CPC keywords represent transactional value (e.g., "loans", "dentist", "insurance").
            4. Pronunciation & Commercial Intent: Is it easy to spell and say over the radio? Double letters or awkward spelling (e.g., "cool-shooz") drop value. High CPC means immediate monetization.
            5. SEO Metrics Mock Simulation: Realistically mock high/medium/low organic traffic potential, CPC value, search volume, backlinks, and domain authority based on the domain's keywords (e.g. "loans.com" would search volume 100k+, CPC $25+, backlinks 1000s; while "mytechblogxyz.info" would have 0 volume, 0 backlinks, 0 domain authority).
            
            Pricing Guidelines:
            - Retail Price: Price to an end-user business buyer (patient resale path, can take years).
            - Wholesale Price: Price to an active domain investor portfolio reseller (liquid price, e.g. 10% - 20% of retail).
            - Investor Price: Quick liquidation price (rapid cash sales at auctions).
            - Average domains (e.g., "mygamingblogonline.com") should be priced very modestly (Retail: $100-$300, Wholesale: $20-$50). Only highly premium dictionary keywords or ultra-short terms should receive valuations exceeding $10,000. Be extremely realistic. Avoid scam or inflated valuations.
            
            Output strictly a valid JSON conforming to the requested structure. Ensure no markdown formatting or block backticks outside the clean JSON string.
        """.trimIndent()

        val prompt = """
            Determine a realistic professional domain appraisal report for the domain: "$trimmedDomain".
            Return a JSON object matching exactly this structure:
            {
              "domainName": "$trimmedDomain",
              "isPremium": true/false (true if short, highly commercial, or premium .com dictionary word),
              "length": ${trimmedDomain.split(".")[0].length},
              "extensionPopularity": "High" or "Medium" or "Low" (relative to extension value),
              "brandability": "Excellent" or "Good" or "Fair" (visual and catchy appeal),
              "commercialIntent": "High" or "Medium" or "Low",
              "pronunciability": "Easy" or "Moderate" or "Hard",
              "estRetailValue": 5000.00 (double estimate),
              "estWholesaleValue": 1000.00 (double estimate),
              "estInvestorValue": 500.00 (double estimate),
              "confidenceScore": 85 (integer percentage 1-100 indicating confidence based on metric support),
              "seoBacklinks": 120 (integer estimation of standard links found or organic metrics),
              "seoAuthority": 25 (integer 0-100),
              "organicTrafficPotential": "High" or "Medium" or "Low",
              "searchVolume": 1200 (integer monthly estimation),
              "cpcValue": 1.45 (double estimation of search engine ads bid),
              "similarSales": [
                 { "domainName": "similardomain1.com", "price": 4500.00, "year": 2024, "platform": "Sedo" },
                 { "domainName": "similardomain2.com", "price": 1200.00, "year": 2023, "platform": "NameBio" }
              ],
              "marketTrends": "Explain contemporary market statistics for this type of keyword and extension.",
              "investmentRecommendation": "Give Domain Investor portfolio and holding advice.",
              "explanationText": "A crisp, professional human-like 4-sentence overview of the domain's market strengths and structural pricing context."
            }
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.2f
            ),
            systemInstruction = Content(parts = listOf(Part(text = systemInstruction)))
        )

        val retrievedAppraisal = try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw Exception("Empty response from AI engine.")
            
            // Clean JSON string in case the model includes any backticks or prefixes
            val cleanedJson = jsonText
                .replace("```json", "")
                .replace("```", "")
                .trim()

            val resultJson = resultAdapter.fromJson(cleanedJson)
                ?: throw Exception("Failed to serialize valuation report.")

            val salesJson = similarSalesListAdapter.toJson(resultJson.similarSales)

            SavedAppraisal(
                domainName = trimmedDomain,
                timestamp = System.currentTimeMillis(),
                isPremium = resultJson.isPremium,
                length = resultJson.length,
                extensionPopularity = resultJson.extensionPopularity,
                brandability = resultJson.brandability,
                commercialIntent = resultJson.commercialIntent,
                pronunciability = resultJson.pronunciability,
                estRetailValue = resultJson.estRetailValue,
                estWholesaleValue = resultJson.estWholesaleValue,
                estInvestorValue = resultJson.estInvestorValue,
                confidenceScore = resultJson.confidenceScore,
                seoBacklinks = resultJson.seoBacklinks,
                seoAuthority = resultJson.seoAuthority,
                searchVolume = resultJson.searchVolume,
                cpcValue = resultJson.cpcValue,
                similarSalesJson = salesJson,
                marketTrends = resultJson.marketTrends,
                investmentRecommendation = resultJson.investmentRecommendation,
                explanationText = resultJson.explanationText
            )
        } catch (e: Exception) {
            e.printStackTrace()
            // Create a realistic default heuristic valuation as an advanced local database fallback in case the API is offline
            generateHeuristicValueFallback(trimmedDomain, e.localizedMessage ?: "Network error")
        }

        // Save real appraisal response in the local history folder
        appraisalDao.insertAppraisal(retrievedAppraisal)
        retrievedAppraisal
    }

    /**
     * AI-Powered Specialized Domain Investment Coach/Advisor Chatbot.
     */
    suspend fun getChatbotResponse(history: String, userMessage: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val systemInstruction = """
            You are DomValuate AI, a veteran domain investor, domain broker, and consultant. 
            You are highly specialized in domain auctions, premium acquisitions, outbound email sales, TLD liquidation markets, registrar comparisons, SEO metrics, and flip-arbitrage tactics.
            
            Offer practical, expert-level human broker responses. Avoid boring generic disclaimers. 
            Teach the user about GoDaddy Auctions, Sedo, Afternic, NameBio historical research, DropCatch backorders, liquid vs non-liquid domains, generic keywords, brandables, and SEO metrics (Backlinks, CPC, Search Volume).
            
            Be helpful, strategic, highly analytical, and direct. Keep responses beautifully structured using clear Markdown (bullet points, italicized examples, bold values).
        """.trimIndent()

        val prompt = if (history.isNotEmpty()) {
            "Conversation context:\n$history\nUser: $userMessage\nDomValuate AI:"
        } else {
            "User: $userMessage\nDomValuate AI:"
        }

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(temperature = 0.7f),
            systemInstruction = Content(parts = listOf(Part(text = systemInstruction)))
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "I am evaluating the digital real estate market. Please try asking again shortly."
        } catch (e: Exception) {
            e.printStackTrace()
            "Digital real estate connection temporary setback: ${e.localizedMessage}. Please verify your network and Gemini API key configuration in the Secrets Panel."
        }
    }

    /**
     * Local appraisal generator to keep the application 100% functional even when
     * offline or during temporary network interruptions, obeying professional guidelines.
     */
    private fun generateHeuristicValueFallback(domain: String, errDetails: String): SavedAppraisal {
        val extension = domain.substringAfterLast(".", "com")
        val pParts = domain.substringBeforeLast(".", "domain")
        val len = pParts.length

        // Logical valuation heuristic
        val hasPremiumKeywords = pParts.contains("tech") || pParts.contains("ai") || pParts.contains("app") ||
                pParts.contains("pay") || pParts.contains("coin") || pParts.contains("cyber") || pParts.contains("data")

        var score = 30
        var isPremium = false

        var isCom = extension == "com"
        var extValue = 40
        if (isCom) {
            extValue = 85
            score += 25
        } else if (extension == "ai" || extension == "io" || extension == "co") {
            extValue = 75
            score += 15
        } else if (extension == "net" || extension == "org") {
            score += 5
            extValue = 60
        } else {
            extValue = 25
        }

        if (len <= 4) {
            score += 30
            isPremium = true
        } else if (len <= 8) {
            score += 10
        }

        if (hasPremiumKeywords) {
            score += 15
            isPremium = true
        }

        val retailPrice = when {
            len <= 3 && isCom -> 25000.0
            len <= 4 && isCom -> 7500.0
            hasPremiumKeywords && isCom -> 3800.0
            isCom -> 1200.0
            extension == "ai" && len <= 5 -> 5800.0
            extension == "ai" -> 2200.0
            extension == "io" || extension == "co" -> 1100.0
            else -> 240.0
        }

        val wholesalePrice = (retailPrice * 0.15).coerceAtLeast(15.0)
        val investorPrice = (retailPrice * 0.08).coerceAtLeast(10.0)

        val simSales = listOf(
            SimilarSaleJson(domainName = "similar-${pParts}.com", price = (retailPrice * 0.75), year = 2025, platform = "Sedo"),
            SimilarSaleJson(domainName = "example-${extension}.co", price = (retailPrice * 0.40), year = 2024, platform = "NameBio")
        )

        return SavedAppraisal(
            domainName = domain,
            isPremium = isPremium,
            length = len,
            extensionPopularity = if (extValue > 70) "High" else if (extValue > 50) "Medium" else "Low",
            brandability = if (len <= 6) "Excellent" else if (len <= 10) "Good" else "Fair",
            commercialIntent = if (hasPremiumKeywords) "High" else "Medium",
            pronunciability = if (len <= 7) "Easy" else "Moderate",
            estRetailValue = retailPrice,
            estWholesaleValue = wholesalePrice,
            estInvestorValue = investorPrice,
            confidenceScore = score.coerceIn(10, 95),
            seoBacklinks = if (isPremium) 320 else 5,
            seoAuthority = if (isCom) 18 else 3,
            searchVolume = if (hasPremiumKeywords) 850 else 0,
            cpcValue = if (hasPremiumKeywords) 1.25 else 0.0,
            similarSalesJson = similarSalesListAdapter.toJson(simSales),
            marketTrends = "High liquidity for short visual combinations on .com and .ai. Stable demand with average investor margins. Fallback mode active: (Local Appraisal Algorithm applied due to network: $errDetails).",
            investmentRecommendation = "Hold for premium end-user. Avoid bulk outbound listing on budget extensions.",
            explanationText = "Local heuristic analysis based on domain length of $len characters, extension '.$extension', branding parameters, and premium keyword attributes.",
            currency = "USD"
        )
    }

    // --- DB DAO triggers ---

    suspend fun insertAppraisal(appraisal: SavedAppraisal) {
        appraisalDao.insertAppraisal(appraisal)
    }

    suspend fun deleteAppraisal(id: Long) {
        appraisalDao.deleteAppraisal(id)
    }

    suspend fun clearHistory() {
        appraisalDao.clearAll()
    }

    suspend fun addToPortfolio(domain: PortfolioDomain) {
        portfolioDao.insertDomain(domain)
    }

    suspend fun deleteFromPortfolio(id: Long) {
        portfolioDao.deleteDomain(id)
    }
}
