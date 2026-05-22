package com.example.ui.screens

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.PortfolioDomain
import com.example.ui.components.SectionHeader
import com.example.ui.viewmodel.DomainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PortfolioScreen(
    viewModel: DomainViewModel,
    modifier: Modifier = Modifier
) {
    val portfolio by viewModel.portfolio.collectAsState()
    val totalValue by viewModel.totalPortfolioValue.collectAsState()

    var showAddForm by remember { mutableStateOf(false) }

    // Manual input states
    var domainInput by remember { mutableStateOf("") }
    var priceInput by remember { mutableStateOf("") }
    var valueInput by remember { mutableStateOf("") }
    var registrarInput by remember { mutableStateOf("") }
    var notesInput by remember { mutableStateOf("") }

    val context = LocalContext.current

    // Aggregate statistics math
    val investedSum = portfolio.mapNotNull { it.purchasePrice }.sum()
    val appraisedSum = totalValue ?: 0.0
    val netROI = appraisedSum - investedSum

    val roiPercent = if (investedSum > 0) {
        (netROI / investedSum) * 100.0
    } else {
        0.0
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Portfolio statistics dashboard header card
        item {
            Spacer(modifier = Modifier.height(12.dp))
            PortfolioStatsHeaderCard(
                portfolioSize = portfolio.size,
                appraisedSumLabel = viewModel.formatCurrency(appraisedSum),
                investedSumLabel = viewModel.formatCurrency(investedSum),
                netROILabel = viewModel.formatCurrency(netROI),
                roiPercent = roiPercent,
                viewModel = viewModel,
                portfolio = portfolio
            )
        }

        // Action controls (Add Manual Domain & Export Portfolio Ledger List)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { showAddForm = !showAddForm },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = if (showAddForm) Icons.Default.Delete else Icons.Default.Add,
                        contentDescription = "Toggle Panel"
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (showAddForm) "Close Panel" else "Add Asset Manual", fontWeight = FontWeight.Bold)
                }

                if (portfolio.isNotEmpty()) {
                    IconButton(onClick = {
                        val csvHeader = "DomainName,PurchasePrice,AppraisedValue,Registrar,Notes\n"
                        val csvLines = portfolio.joinToString("\n") {
                            "${it.domainName},${it.purchasePrice ?: 0.0},${it.appraisedValue},${it.registrar},${it.notes}"
                        }
                        val csvContent = csvHeader + csvLines

                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/csv"
                            putExtra(Intent.EXTRA_SUBJECT, "DomValuate Portfolio Ledger Export")
                            putExtra(Intent.EXTRA_TEXT, csvContent)
                        }
                        context.startActivity(Intent.createChooser(intent, "Export Saved Appraisals CSV"))
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Export CSV", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // Expandable Manual Form Panel
        item {
            AnimatedVisibility(
                visible = showAddForm,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Add Custom Portfolio Asset",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = domainInput,
                            onValueChange = { domainInput = it },
                            label = { Text("Domain Name (e.g. startup.com)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = priceInput,
                                onValueChange = { priceInput = it },
                                label = { Text("Buy Price (${viewModel.getCurrencySymbol()})") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 4.dp)
                            )
                            OutlinedTextField(
                                value = valueInput,
                                onValueChange = { valueInput = it },
                                label = { Text("Valuation (${viewModel.getCurrencySymbol()})") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = registrarInput,
                            onValueChange = { registrarInput = it },
                            label = { Text("Registrar (e.g. GoDaddy)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = notesInput,
                            onValueChange = { notesInput = it },
                            label = { Text("Aesthetic tags / Notes details") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                if (domainInput.isBlank()) {
                                    Toast.makeText(context, "Domain name is required", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val bVal = priceInput.toDoubleOrNull()
                                val valVal = valueInput.toDoubleOrNull() ?: 0.0

                                viewModel.addManualToPortfolio(
                                    domainName = domainInput,
                                    value = valVal,
                                    purchasePrice = bVal,
                                    registrar = registrarInput,
                                    notes = notesInput
                                )

                                Toast.makeText(context, "$domainInput ledger saved!", Toast.LENGTH_SHORT).show()

                                // Reset form inputs
                                domainInput = ""
                                priceInput = ""
                                valueInput = ""
                                registrarInput = ""
                                notesInput = ""
                                showAddForm = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Confirm & Record Asset", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Domain Ledger listings
        item {
            Text(
                text = "Tracked Secondary Assets (${portfolio.size})",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (portfolio.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderSpecial,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(50.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Portfolio tracker is empty",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Generate appraisals and click 'Add to Portfolio' or input items manually above to see estimated net values.",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier.padding(top = 4.dp, start = 16.dp, end = 16.dp)
                        )
                    }
                }
            }
        } else {
            items(portfolio) { domainItem ->
                PortfolioAssetRowItem(
                    domainItem = domainItem,
                    viewModel = viewModel,
                    onDelete = { viewModel.deleteFromPortfolio(domainItem.id) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun PortfolioStatsHeaderCard(
    portfolioSize: Int,
    appraisedSumLabel: String,
    investedSumLabel: String,
    netROILabel: String,
    roiPercent: Double,
    viewModel: DomainViewModel,
    portfolio: List<PortfolioDomain>
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(primaryColor, secondaryColor.copy(alpha = 0.85f))
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PORTFOLIO LEDGER PERFORMANCE",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.15f), CircleShape)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$portfolioSize Domains",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "Aggregated Retail Value",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Text(
                            text = appraisedSumLabel,
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                            color = Color.White
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        val prefix = if (roiPercent >= 0) "ROI: +" else "ROI: "
                        Text(
                            text = "$prefix${String.format(Locale.US, "%.1f", roiPercent)}%",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (roiPercent >= 0) Color(0xFFACFFD2) else Color(0xFFFFC6C6)
                        )
                        Text(
                            text = if (roiPercent >= 0) "Net Gain $netROILabel" else "Net Loss $netROILabel",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Divider(color = Color.White.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Capital Invested", fontSize = 10.sp, color = Color.White.copy(alpha = 0.75f))
                        Text(investedSumLabel, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        val liquidW = appraisedSumLabel // Wait, let's show simple wholesale liquidation targets
                        Text("Wholesale Liquidation", fontSize = 10.sp, color = Color.White.copy(alpha = 0.75f))
                        Text(viewModel.formatCurrency(portfolio.sumOf { it.appraisedValue } * 0.15), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun PortfolioAssetRowItem(
    domainItem: PortfolioDomain,
    viewModel: DomainViewModel,
    onDelete: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.US) }
    val buyVal = domainItem.purchasePrice ?: 0.0
    val appraised = domainItem.appraisedValue

    val prof = appraised - buyVal
    val isProfit = prof >= 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = domainItem.domainName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = domainItem.registrar,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (domainItem.notes.isNotEmpty()) {
                        Text(
                            text = domainItem.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                            maxLines = 1
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
    ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row {
                    Column(modifier = Modifier.padding(end = 20.dp)) {
                        Text("Buy Price", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = if (domainItem.purchasePrice != null) viewModel.formatCurrency(buyVal) else "N/A",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Column {
                        Text("AI Appraised", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = viewModel.formatCurrency(appraised),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (domainItem.purchasePrice != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Net ROI", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = (if (isProfit) "+" else "") + viewModel.formatCurrency(prof),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isProfit) Color(0xFF10B981) else Color(0xFFEF4444)
                        )
                    }
                }
            }
        }
    }
}
