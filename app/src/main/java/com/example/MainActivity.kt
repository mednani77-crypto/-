package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.BottomNav
import com.example.ui.components.TopHeader
import com.example.ui.screens.*
import com.example.ui.theme.SlateBg
import com.example.ui.theme.SyscoTheme
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.SubView
import com.example.ui.viewmodel.SyscoViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: SyscoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SyscoTheme {
                // Mandatory Arabic RTL (Right-to-Left) Layout Direction
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    SyscoApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun SyscoApp(viewModel: SyscoViewModel) {
    val context = LocalContext.current

    val isSplashVisible by viewModel.isSplashVisible.collectAsStateWithLifecycle()
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val kpi by viewModel.kpiSummary.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val invoices by viewModel.invoices.collectAsStateWithLifecycle()
    val payments by viewModel.payments.collectAsStateWithLifecycle()
    val customerBalances by viewModel.customerBalances.collectAsStateWithLifecycle()

    val customerSubView by viewModel.customerSubView.collectAsStateWithLifecycle()
    val invoiceSubView by viewModel.invoiceSubView.collectAsStateWithLifecycle()
    val paymentSubView by viewModel.paymentSubView.collectAsStateWithLifecycle()
    val catalogSubView by viewModel.catalogSubView.collectAsStateWithLifecycle()
    val selectedProductIds by viewModel.selectedProductIds.collectAsStateWithLifecycle()

    val editingCustomer by viewModel.editingCustomer.collectAsStateWithLifecycle()
    val editingProduct by viewModel.editingProduct.collectAsStateWithLifecycle()

    val activeStatement by viewModel.activeStatement.collectAsStateWithLifecycle()
    val activeInvoicePreview by viewModel.activeInvoicePreview.collectAsStateWithLifecycle()

    // Handle toast messages
    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearUserMessage()
        }
    }

    // Android Back Button Navigation Handling
    BackHandler(enabled = !isSplashVisible) {
        when {
            activeStatement != null -> viewModel.closeCustomerStatement()
            activeInvoicePreview != null -> viewModel.closeInvoicePreview()
            currentTab == AppTab.CUSTOMERS && customerSubView == SubView.FORM -> viewModel.setCustomerSubView(SubView.LIST)
            currentTab == AppTab.INVOICES && invoiceSubView == SubView.FORM -> viewModel.setInvoiceSubView(SubView.LIST)
            currentTab == AppTab.PAYMENTS && paymentSubView == SubView.FORM -> viewModel.setPaymentSubView(SubView.LIST)
            currentTab == AppTab.CATALOG && catalogSubView == SubView.FORM -> viewModel.setCatalogSubView(SubView.LIST)
            currentTab != AppTab.DASHBOARD -> viewModel.setTab(AppTab.DASHBOARD)
        }
    }

    if (isSplashVisible) {
        SplashScreen(onDismiss = { viewModel.dismissSplash() })
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopHeader(
                    settings = settings,
                    onSettingsClick = { viewModel.setTab(AppTab.SETTINGS) },
                    onAiChatClick = { viewModel.setTab(AppTab.AI_CHAT) }
                )
            },
            bottomBar = {
                BottomNav(
                    currentTab = currentTab,
                    onTabSelected = { viewModel.setTab(it) }
                )
            },
            containerColor = SlateBg
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "tabTransition"
                ) { targetTab ->
                    when (targetTab) {
                        AppTab.DASHBOARD -> {
                            DashboardScreen(
                                viewModel = viewModel,
                                kpi = kpi,
                                invoices = invoices,
                                payments = payments,
                                onNavigate = { viewModel.setTab(it) }
                            )
                        }

                        AppTab.CUSTOMERS -> {
                            CustomersScreen(
                                viewModel = viewModel,
                                balances = customerBalances,
                                activeSubView = customerSubView,
                                editingCustomer = editingCustomer
                            )
                        }

                        AppTab.INVOICES -> {
                            InvoicesScreen(
                                viewModel = viewModel,
                                invoices = invoices,
                                customers = customers,
                                products = products,
                                activeSubView = invoiceSubView
                            )
                        }

                        AppTab.PAYMENTS -> {
                            PaymentsScreen(
                                viewModel = viewModel,
                                payments = payments,
                                customers = customers,
                                activeSubView = paymentSubView
                            )
                        }

                        AppTab.CATALOG -> {
                            CatalogScreen(
                                viewModel = viewModel,
                                products = products,
                                settings = settings,
                                selectedIds = selectedProductIds,
                                activeSubView = catalogSubView,
                                editingProduct = editingProduct
                            )
                        }

                        AppTab.SETTINGS -> {
                            SettingsScreen(
                                viewModel = viewModel,
                                settings = settings
                            )
                        }

                        AppTab.AI_CHAT -> {
                            GeminiChatScreen()
                        }
                    }
                }
            }

            // Customer Statement Modal Preview
            activeStatement?.let { (balance, entries) ->
                CustomerStatementDialog(
                    balance = balance,
                    entries = entries,
                    settings = settings,
                    onDismiss = { viewModel.closeCustomerStatement() },
                    onSharePdf = {
                        viewModel.shareCustomerStatementPdf(context, balance, entries)
                    }
                )
            }

            // Invoice Modal Preview
            activeInvoicePreview?.let { (invoice, items) ->
                InvoicePreviewDialog(
                    invoice = invoice,
                    items = items,
                    settings = settings,
                    onDismiss = { viewModel.closeInvoicePreview() },
                    onSharePdf = {
                        viewModel.shareInvoicePdf(context, invoice, items)
                    }
                )
            }
        }
    }
}
