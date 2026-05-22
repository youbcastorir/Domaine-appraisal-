package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.api.AppraisalResultJson
import com.example.data.api.RetrofitClient
import com.example.data.api.SimilarSaleJson
import com.example.data.database.PortfolioDomain
import com.example.data.database.SavedAppraisal
import com.example.data.repository.DomainRepository
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

enum class Screen {
    DASHBOARD, APPRAISE, PORTFOLIO, CHAT
}

sealed interface AppraisalState {
    object Idle : AppraisalState
    object Loading : AppraisalState
    data class Success(val appraisal: SavedAppraisal) : AppraisalState
    data class Error(val message: String) : AppraisalState
}

data class ChatMessage(
    val sender: String, // "user" or "bot"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

class DomainViewModel(private val repository: DomainRepository) : ViewModel() {

    // Streams from database
    val appraisals: StateFlow<List<SavedAppraisal>> = repository.allAppraisals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val portfolio: StateFlow<List<PortfolioDomain>> = repository.allPortfolio
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalPortfolioValue: StateFlow<Double?> = repository.totalPortfolioValue
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Screen State
    private val _currentScreen = MutableStateFlow(Screen.DASHBOARD)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Multi-currency Selection: "USD", "EUR", "MAD", "GBP", "CAD"
    private val _selectedCurrency = MutableStateFlow("USD")
    val selectedCurrency: StateFlow<String> = _selectedCurrency.asStateFlow()

    val currencyRates = mapOf(
        "USD" to 1.0,
        "EUR" to 0.92,
        "MAD" to 10.0,
        "GBP" to 0.79,
        "CAD" to 1.36
    )

    val currencySymbols = mapOf(
        "USD" to "${'$'}",
        "EUR" to "€",
        "MAD" to "MAD",
        "GBP" to "£",
        "CAD" to "C${'$'}"
    )

    // Appraisal states
    private val _domainInput = MutableStateFlow("")
    val domainInput: StateFlow<String> = _domainInput.asStateFlow()

    private val _appraisalState = MutableStateFlow<AppraisalState>(AppraisalState.Idle)
    val appraisalState: StateFlow<AppraisalState> = _appraisalState.asStateFlow()

    // Selected saved appraisal for detailed viewing in dialog/sheet
    private val _selectedAppraisal = MutableStateFlow<SavedAppraisal?>(null)
    val selectedAppraisal: StateFlow<SavedAppraisal?> = _selectedAppraisal.asStateFlow()

    // Chatbot state
    private val _chatbotMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                sender = "bot",
                text = "Welcome to DomValuate Coaching! 🚀\nI'm your AI Domain Investing Specialist. Ask me anything about name valuations, outbound flip pitches, auction marketplaces (GoDaddy, Sedo, DropCatch, NameBio), or domain portfolio optimization!"
            )
        )
    )
    val chatbotMessages: StateFlow<List<ChatMessage>> = _chatbotMessages.asStateFlow()

    private val _chatInput = MutableStateFlow("")
    val chatInput: StateFlow<String> = _chatInput.asStateFlow()

    private val _chatbotLoading = MutableStateFlow(false)
    val chatbotLoading: StateFlow<Boolean> = _chatbotLoading.asStateFlow()

    // Bulk Appraiser states
    private val _bulkInput = MutableStateFlow("")
    val bulkInput: StateFlow<String> = _bulkInput.asStateFlow()

    private val _bulkResults = MutableStateFlow<List<SavedAppraisal>>(emptyList())
    val bulkResults: StateFlow<List<SavedAppraisal>> = _bulkResults.asStateFlow()

    private val _bulkLoading = MutableStateFlow(false)
    val bulkLoading: StateFlow<Boolean> = _bulkLoading.asStateFlow()

    private val _bulkProgress = MutableStateFlow(0f)
    val bulkProgress: StateFlow<Float> = _bulkProgress.asStateFlow()

    // UI Trigger actions
    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun setCurrency(currency: String) {
        if (currencyRates.containsKey(currency)) {
            _selectedCurrency.value = currency
        }
    }

    fun onDomainInputChange(input: String) {
        _domainInput.value = input
    }

    fun selectAppraisal(appraisal: SavedAppraisal?) {
        _selectedAppraisal.value = appraisal
    }

    fun appraiseDomain() {
        val domain = _domainInput.value.trim()
        if (domain.isEmpty()) return

        viewModelScope.launch {
            _appraisalState.value = AppraisalState.Loading
            try {
                // Ensure dot-extension exists
                if (!domain.contains(".") || domain.startsWith(".") || domain.endsWith(".")) {
                    throw Exception("Please enter a valid domain name with extension (e.g. startup.com, code.ai)")
                }
                val result = repository.appraiseDomain(domain)
                _appraisalState.value = AppraisalState.Success(result)
                _selectedAppraisal.value = result
            } catch (e: Exception) {
                _appraisalState.value = AppraisalState.Error(e.localizedMessage ?: "Appraisal engine error")
            }
        }
    }

    // Reset appraisal state to back out of detail view
    fun resetAppraisalState() {
        _appraisalState.value = AppraisalState.Idle
        _domainInput.value = ""
    }

    // Delete single evaluation historical entry
    fun deleteAppraisalHistory(id: Long) {
        viewModelScope.launch {
            repository.deleteAppraisal(id)
            if (_selectedAppraisal.value?.id == id) {
                _selectedAppraisal.value = null
            }
        }
    }

    // Portfolio Management
    fun addToPortfolioFromAppraisal(appraisal: SavedAppraisal, registrar: String, purchasePrice: Double? = null) {
        viewModelScope.launch {
            val portfolioDomain = PortfolioDomain(
                domainName = appraisal.domainName,
                purchasePrice = purchasePrice,
                purchaseDate = System.currentTimeMillis(),
                appraisedValue = appraisal.estRetailValue,
                registrar = registrar.ifBlank { "Unassigned Registrar" },
                expiryDate = System.currentTimeMillis() + (365L * 24 * 60 * 60 * 1000), // Default 1 year from now
                notes = "Saved from AI Assessment on DomValuate.",
                currency = "USD"
            )
            repository.addToPortfolio(portfolioDomain)
        }
    }

    fun addManualToPortfolio(domainName: String, value: Double, purchasePrice: Double?, registrar: String, notes: String) {
        val cleaned = domainName.trim().lowercase()
        if (cleaned.isEmpty()) return
        viewModelScope.launch {
            val portfolioDomain = PortfolioDomain(
                domainName = cleaned,
                purchasePrice = purchasePrice,
                purchaseDate = System.currentTimeMillis(),
                appraisedValue = value,
                registrar = registrar.ifBlank { "Manual" },
                expiryDate = System.currentTimeMillis() + (365L * 24 * 60 * 1000),
                notes = notes,
                currency = "USD"
            )
            repository.addToPortfolio(portfolioDomain)
        }
    }

    fun deleteFromPortfolio(id: Long) {
        viewModelScope.launch {
            repository.deleteFromPortfolio(id)
        }
    }

    // Chatbot Interaction
    fun onChatInputChange(input: String) {
        _chatInput.value = input
    }

    fun sendMessage() {
        val text = _chatInput.value.trim()
        if (text.isEmpty() || _chatbotLoading.value) return

        val userMessage = ChatMessage(sender = "user", text = text)
        _chatbotMessages.value = _chatbotMessages.value + userMessage
        _chatInput.value = ""
        _chatbotLoading.value = true

        viewModelScope.launch {
            // Compile context history
            val conversationText = _chatbotMessages.value.takeLast(6).joinToString("\n") {
                "${it.sender}: ${it.text}"
            }
            val replyText = repository.getChatbotResponse(conversationText, text)
            _chatbotMessages.value = _chatbotMessages.value + ChatMessage(sender = "bot", text = replyText)
            _chatbotLoading.value = false
        }
    }

    fun clearChat() {
        _chatbotMessages.value = listOf(
            ChatMessage(
                sender = "bot",
                text = "Coaching terminal re-initialized. 🚀 Ready for next strategy!"
            )
        )
    }

    // Bulk appraisal
    fun onBulkInputChange(input: String) {
        _bulkInput.value = input
    }

    fun startBulkAppraisal() {
        val rawInput = _bulkInput.value.trim()
        if (rawInput.isEmpty() || _bulkLoading.value) return

        val domains = rawInput.split(Regex("[,\\r\\n]+"))
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() && it.contains(".") }

        if (domains.isEmpty()) return

        _bulkLoading.value = true
        _bulkResults.value = emptyList()
        _bulkProgress.value = 0f

        viewModelScope.launch {
            val total = domains.size
            val resultsList = mutableListOf<SavedAppraisal>()
            domains.forEachIndexed { index, domain ->
                _bulkProgress.value = (index + 1).toFloat() / total
                try {
                    val appraisal = repository.appraiseDomain(domain)
                    resultsList.add(appraisal)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            _bulkResults.value = resultsList
            _bulkLoading.value = false
        }
    }

    // Helpers to convert standard double price value dynamically based on currency setting
    fun formatCurrency(usdValue: Double): String {
        val rate = currencyRates[_selectedCurrency.value] ?: 1.0
        val symbol = currencySymbols[_selectedCurrency.value] ?: "$"
        val converted = usdValue * rate
        return when {
            converted >= 1_000_000 -> String.format(Locale.US, "%s%.2fM", symbol, converted / 1_000_000)
            converted >= 1000 -> String.format(Locale.US, "%s%,.0f", symbol, converted)
            else -> String.format(Locale.US, "%s%.2f", symbol, converted)
        }
    }

    fun convertValue(usdValue: Double): Double {
        val rate = currencyRates[_selectedCurrency.value] ?: 1.0
        return usdValue * rate
    }

    fun getCurrencySymbol(): String {
        return currencySymbols[_selectedCurrency.value] ?: "$"
    }

    // Helper to extract list from JSON stored in Room DB
    fun parseSimilarSales(salesJson: String): List<SimilarSaleJson> {
        return try {
            val adapter: JsonAdapter<List<SimilarSaleJson>> = RetrofitClient.moshi.adapter(
                Types.newParameterizedType(List::class.java, SimilarSaleJson::class.java)
            )
            adapter.fromJson(salesJson) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun clearAllAppraisals() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }
}

class DomainViewModelFactory(private val repository: DomainRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DomainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DomainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
