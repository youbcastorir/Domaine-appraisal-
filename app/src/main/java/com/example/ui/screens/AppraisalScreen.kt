package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.SimilarSaleJson
import com.example.data.database.SavedAppraisal
import com.example.ui.components.ConfidenceRing
import com.example.ui.components.InfoAttributeWidget
import com.example.ui.components.MetricProgressBar
import com.example.ui.components.PricingTierCard
import com.example.ui.components.PredictionValueChart
import com.example.ui.components.SectionHeader
import com.example.ui.viewmodel.AppraisalState
import com.example.ui.viewmodel.DomainViewModel
import java.util.Locale

@Composable
fun AppraisalScreen(
    viewModel: DomainViewModel,
    modifier: Modifier = Modifier
) {
    val singleInput by viewModel.domainInput.collectAsState()
    val appraisalState by viewModel.appraisalState.collectAsState()
    val selectedAppraisal by viewModel.selectedAppraisal.collectAsState()

    val bulkInput by viewModel.bulkInput.collectAsState()
    val bulkResults by viewModel.bulkResults.collectAsState()
    val bulkLoading by viewModel.bulkLoading.collectAsState()
    val bulkProgress by viewModel.bulkProgress.collectAsState()

    var activeTab by remember { mutableStateOf(0) }
    val tabs = listOf("Single Appraisal", "Bulk System")

    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 8.dp)
    ) {
        // Tab segment row
        TabRow(
            selectedTabIndex = activeTab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            tabs.forEachIndexed { index, text ->
                Tab(
                    selected = activeTab == index,
                    onClick = { activeTab = index },
                    text = { Text(text, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (activeTab == 0) {
            // Single appraisal layout
            if (selectedAppraisal != null) {
                // If there's an appraisal successfully rendered or selected, show detailed overview!
                SingleAppraisalResultLayout(
                    appraisal = selectedAppraisal!!,
                    viewModel = viewModel,
                    onBack = { viewModel.selectAppraisal(null) }
                )
            } else {
                SingleAppraisalInputLayout(
                    input = singleInput,
                    appraisalState = appraisalState,
                    onInputChange = { viewModel.onDomainInputChange(it) },
                    onAppraise = { viewModel.appraiseDomain() },
                    viewModel = viewModel
                )
            }
        } else {
            // Bulk system appraisal layout
            BulkAppraisalLayout(
                bulkInput = bulkInput,
                bulkResults = bulkResults,
                bulkLoading = bulkLoading,
                bulkProgress = bulkProgress,
                onInputChange = { viewModel.onBulkInputChange(it) },
                onStartBulk = { viewModel.startBulkAppraisal() },
                viewModel = viewModel,
                context = context
            )
        }
    }
}

@Composable
fun SingleAppraisalInputLayout(
    input: String,
    appraisalState: AppraisalState,
    onInputChange: (String) -> Unit,
    onAppraise: () -> Unit,
    viewModel: DomainViewModel
) {
    val assessmentsList by viewModel.appraisals.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Instant Domain Appraisal",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Query domain pricing heuristics via deep machine neural processing. Accurate and transparent details.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = input,
                        onValueChange = onInputChange,
                        placeholder = { Text("e.g. startup.com, medical.ai, techcorp.net") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    if (appraisalState is AppraisalState.Loading) {
                        Button(
                            onClick = {},
                            enabled = false,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Analyzing Market Data...", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = onAppraise,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Appraise Securely", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (appraisalState is AppraisalState.Error) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = appraisalState.message,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Appraisal Logs History (${assessmentsList.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (assessmentsList.isNotEmpty()) {
                    Text(
                        text = "Clear All",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.clickable { viewModel.clearAllAppraisals() }
                    )
                }
            }
        }

        if (assessmentsList.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Historical logs are empty",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(assessmentsList) { appraisal ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                        .clickable { viewModel.selectAppraisal(appraisal) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = appraisal.domainName,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (appraisal.isPremium) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFFFD700).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text("Premium", color = Color(0xFFCCAC00), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Score: ${appraisal.confidenceScore}% • Len: ${appraisal.length}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = viewModel.formatCurrency(appraisal.estRetailValue),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        IconButton(
                            onClick = { viewModel.deleteAppraisalHistory(appraisal.id) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Help,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SingleAppraisalResultLayout(
    appraisal: SavedAppraisal,
    viewModel: DomainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val rates by viewModel.selectedCurrency.collectAsState()

    var showRegistrarDialog by remember { mutableStateOf(false) }
    var registrarInput by remember { mutableStateOf("") }
    var buyPriceInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Back Navigation Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("< Return", color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Bold)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Currency Selection Dropdown simulated via icons
                    Icon(
                        imageVector = Icons.Default.CurrencyExchange,
                        contentDescription = "Currency",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))

                    val currencies = listOf("USD", "EUR", "MAD")
                    currencies.forEach { cur ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (rates == cur) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { viewModel.setCurrency(cur) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = cur,
                                color = if (rates == cur) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }

        // Domain appraised header summary
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = appraisal.domainName,
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (appraisal.isPremium) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFFFFD700).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 3.dp)
                                    ) {
                                        Text("PREMIUM", color = Color(0xFFCCAC00), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Text(
                                text = "TLD: .${appraisal.domainName.substringAfterLast(".").uppercase(Locale.ROOT)} • Length: ${appraisal.length} chars",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        // Share / Export icon buttons
                        Row {
                            IconButton(onClick = {
                                val shareText = """
                                    DomValuate AI Domain Appraisal Report
                                    Domain: ${appraisal.domainName}
                                    Premium Asset: ${if (appraisal.isPremium) "Yes" else "No"}
                                    Estimated Values:
                                    - Retail: ${viewModel.formatCurrency(appraisal.estRetailValue)}
                                    - Wholesale (Broker): ${viewModel.formatCurrency(appraisal.estWholesaleValue)}
                                    - Liquidation (Auction): ${viewModel.formatCurrency(appraisal.estInvestorValue)}
                                    Confidence Rating: ${appraisal.confidenceScore}%
                                    AI Summary: ${appraisal.explanationText}
                                    Generated via DomValuate Client.
                                """.trimIndent()

                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "Appraisal Report: ${appraisal.domainName}")
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share Appraisal"))
                            }) {
                                Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    Divider(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ConfidenceRing(percentage = appraisal.confidenceScore, size = 95.dp)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Broker Summary context", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = appraisal.explanationText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }

        // Valuation Pricing Tiers
        item {
            SectionHeader(title = "Estimated Liquid Valuations", icon = Icons.Default.LocalOffer)
        }

        item {
            PricingTierCard(
                title = "End-User Retail Price",
                price = viewModel.formatCurrency(appraisal.estRetailValue),
                description = "Maximum projected secondary market listing price for end-user acquisition.",
                color = Color(0xFF10B981), // Green
                icon = Icons.Default.CheckCircle
            )
        }

        item {
            PricingTierCard(
                title = "Investor Wholesale Value",
                price = viewModel.formatCurrency(appraisal.estWholesaleValue),
                description = "Reasonable value expecting quick liquidation to other investors or resellers.",
                color = Color(0xFFF59E0B), // Amber
                icon = Icons.Default.Info
            )
        }

        item {
            PricingTierCard(
                title = "Rapid Liquidation Price",
                price = viewModel.formatCurrency(appraisal.estInvestorValue),
                description = "Bottom target bid value for rapid auction clearance during immediate distress sales.",
                color = Color(0xFFEF4444), // Red
                icon = Icons.Default.Warning
            )
        }

        // Action: Add to portfolio
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF10B981).copy(alpha = 0.08f))
                    .border(1.dp, Color(0xFF10B981).copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .clickable { showRegistrarDialog = !showRegistrarDialog }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FolderSpecial, contentDescription = null, tint = Color(0xFF10B981))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Add to Domain Portfolio Tracker", color = Color(0xFF10B981), fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        // Expanded Add registrar metadata panel animated
        item {
            AnimatedVisibility(
                visible = showRegistrarDialog,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Add Portfolio Metadata Details", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = registrarInput,
                            onValueChange = { registrarInput = it },
                            label = { Text("Registrar (e.g. GoDaddy, Namecheap)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = buyPriceInput,
                            onValueChange = { buyPriceInput = it },
                            label = { Text("Purchase Price (${viewModel.getCurrencySymbol()})") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                val buyVal = buyPriceInput.toDoubleOrNull()
                                viewModel.addToPortfolioFromAppraisal(
                                    appraisal = appraisal,
                                    registrar = registrarInput,
                                    purchasePrice = buyVal
                                )
                                Toast.makeText(context, "${appraisal.domainName} added to portfolio!", Toast.LENGTH_SHORT).show()
                                showRegistrarDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Confirm & Add to Tracker", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Domain Attributes Breakdown
        item {
            SectionHeader(title = "Domain Asset Attributes", icon = Icons.Default.Public)
        }

        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    InfoAttributeWidget(
                        label = "Extension popularity",
                        value = appraisal.extensionPopularity,
                        valueColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    InfoAttributeWidget(
                        label = "Commercial intent",
                        value = appraisal.commercialIntent,
                        valueColor = if (appraisal.commercialIntent == "High") Color(0xFF10B981) else Color(0xFFF59E0B),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    InfoAttributeWidget(
                        label = "Brandability potential",
                        value = appraisal.brandability,
                        valueColor = if (appraisal.brandability == "Excellent") Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    InfoAttributeWidget(
                        label = "Pronunciation factor",
                        value = appraisal.pronunciability,
                        valueColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // SEO metrics details
        item {
            SectionHeader(title = "SEO & organic rankings metrics", icon = Icons.Default.AutoAwesome)
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    MetricProgressBar(
                        label = "Estimated backlinks coverage",
                        value = appraisal.seoBacklinks.toString(),
                        percentage = (appraisal.seoBacklinks.toFloat() / 1500f).coerceAtMost(1f),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    MetricProgressBar(
                        label = "Mock domain authority score",
                        value = "${appraisal.seoAuthority}/100",
                        percentage = appraisal.seoAuthority.toFloat() / 100f,
                        color = Color(0xFF10B981)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    MetricProgressBar(
                        label = "Ad monthly search volume",
                        value = "${appraisal.searchVolume} query/mo",
                        percentage = (appraisal.searchVolume.toFloat() / 10000f).coerceAtMost(1f),
                        color = Color(0xFFF59E0B)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Simulated Google CPC value:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text(viewModel.formatCurrency(appraisal.cpcValue), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // Historical comparable sales
        item {
            SectionHeader(title = "Actual Comparable Sold Domains", icon = Icons.Default.ReceiptLong)
        }

        val sales = viewModel.parseSimilarSales(appraisal.similarSalesJson)
        if (sales.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No comparable sales found specifically linked to this pattern.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        } else {
            items(sales) { sale ->
                SimilarSaleItem(sale = sale, viewModel = viewModel)
            }
        }

        // 5-Year Scarcity Projection Chart
        item {
            PredictionValueChart(viewModel = viewModel, baseValue = appraisal.estRetailValue)
        }

        // Strategic Market trends advice
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Strategic Holdings Advisor Advice", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = appraisal.investmentRecommendation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun SimilarSaleItem(sale: SimilarSaleJson, viewModel: DomainViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(sale.domainName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Text("Year ${sale.year} • Platform ${sale.platform}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(viewModel.formatCurrency(sale.price), fontWeight = FontWeight.ExtraBold, color = Color(0xFF10B981), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun BulkAppraisalLayout(
    bulkInput: String,
    bulkResults: List<SavedAppraisal>,
    bulkLoading: Boolean,
    bulkProgress: Float,
    onInputChange: (String) -> Unit,
    onStartBulk: () -> Unit,
    viewModel: DomainViewModel,
    context: Context
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Bulk Appraisal Engine", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Appraise up to 10 domains at once. Separate with newlines or commas.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = bulkInput,
                        onValueChange = onInputChange,
                        placeholder = { Text("e.g. \nwebtech.net\nfinancebase.ai\ncryptotoken.com") },
                        minLines = 4,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (bulkLoading) {
                        Column {
                            LinearProgressIndicator(
                                progress = bulkProgress,
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Evaluating bulk queue: ${(bulkProgress * 100).toInt()}% completed",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Button(
                            onClick = onStartBulk,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Process Bulk Domains", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (bulkResults.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Processed Reports (${bulkResults.size})", fontWeight = FontWeight.Bold)

                    Button(
                        onClick = {
                            val csvHeader = "DomainName,IsPremium,Length,Extension,RetailValue,WholesaleValue,InvestorValue,ConfidenceScore\n"
                            val csvLines = bulkResults.joinToString("\n") {
                                "${it.domainName},${it.isPremium},${it.length},.${it.domainName.substringAfterLast(".")},${it.estRetailValue},${it.estWholesaleValue},${it.estInvestorValue},${it.confidenceScore}"
                            }
                            val csvContent = csvHeader + csvLines

                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/csv"
                                putExtra(Intent.EXTRA_SUBJECT, "DomValuate Bulk Appraisal Export")
                                putExtra(Intent.EXTRA_TEXT, csvContent)
                            }
                            context.startActivity(Intent.createChooser(intent, "Export Saved Appraisals CSV"))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export CSV", fontSize = 11.sp, color = MaterialTheme.colorScheme.onTertiaryContainer, fontWeight = FontWeight.Bold)
                    }
                }
            }

            items(bulkResults) { appraisal ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                        .clickable { viewModel.selectAppraisal(appraisal) }
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(appraisal.domainName, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            if (appraisal.isPremium) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFFFD700).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text("Premium", color = Color(0xFFCCAC00), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Text("Confidence Score: ${appraisal.confidenceScore}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        text = viewModel.formatCurrency(appraisal.estRetailValue),
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF10B981)
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
