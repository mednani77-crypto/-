package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CompanySettings
import com.example.ui.components.SignaturePad
import com.example.ui.theme.*
import com.example.ui.viewmodel.SyscoViewModel

@Composable
fun SettingsScreen(
    viewModel: SyscoViewModel,
    settings: CompanySettings
) {
    val context = LocalContext.current

    var companyName by remember(settings) { mutableStateOf(settings.companyName) }
    var salesRepName by remember(settings) { mutableStateOf(settings.salesRepName) }
    var address by remember(settings) { mutableStateOf(settings.address) }
    var phone by remember(settings) { mutableStateOf(settings.phone) }
    var taxId by remember(settings) { mutableStateOf(settings.taxId) }
    var exchangeRateString by remember(settings) { mutableStateOf(settings.exchangeRate.toString()) }
    var webhookUrl by remember(settings) { mutableStateOf(settings.webhookUrl) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "إعدادات الشركة وبيانات الترويسة الرسمية",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = NavyBrand
            )
            Text(
                text = "تظهر هذه المعلومات في ترويسة الفواتير، كشوفات الحسابات، وكتالوج الـ PDF",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }

        // Company Details Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = companyName,
                        onValueChange = { companyName = it },
                        label = { Text("اسم الشركة") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = salesRepName,
                        onValueChange = { salesRepName = it },
                        label = { Text("اسم مندوب المبيعات") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("العنوان في جيبوتي") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("أرقام هواتف المبيعات") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = taxId,
                        onValueChange = { taxId = it },
                        label = { Text("الرقم الضريبي والسجل التجاري (NIF / RC)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = exchangeRateString,
                        onValueChange = { exchangeRateString = it },
                        label = { Text("سعر الصرف (1 USD = كم DJF)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = webhookUrl,
                        onValueChange = { webhookUrl = it },
                        label = { Text("رابط Google Sheets Webhook (اختياري)") },
                        placeholder = { Text("https://script.google.com/macros/s/...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Button(
                        onClick = {
                            val rate = exchangeRateString.toDoubleOrNull() ?: 177.72
                            val updated = settings.copy(
                                companyName = companyName.trim(),
                                salesRepName = salesRepName.trim(),
                                address = address.trim(),
                                phone = phone.trim(),
                                taxId = taxId.trim(),
                                exchangeRate = rate,
                                webhookUrl = webhookUrl.trim()
                            )
                            viewModel.saveSettings(updated)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NavyBrand)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حفظ التعديلات وسعر الصرف", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Digital Touch Signature Canvas
        item {
            SignaturePad(
                savedSignatureBase64 = settings.signatureBase64,
                onSaveSignature = { base64 ->
                    viewModel.saveSignature(base64)
                }
            )
        }

        // Data Backup & Export Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "النسخ الاحتياطي والمزامنة",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimary
                    )

                    // Export CSV Excel
                    OutlinedButton(
                        onClick = { viewModel.exportCsvLedger(context) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.TableChart, contentDescription = null, tint = EmeraldCollection)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تصدير تقرير الأرصدة Excel / CSV (يدعم العربية)", color = TextPrimary, fontSize = 13.sp)
                    }

                    // Export JSON Backup
                    OutlinedButton(
                        onClick = { viewModel.exportBackup(context) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, tint = NavyBrand)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("مشاركة نسخة احتياطية كاملة (JSON Backup)", color = TextPrimary, fontSize = 13.sp)
                    }
                }
            }
        }

        // About / Footer
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "SYSCO LTD — Djibouti v1.0",
                    color = TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "نظام متكامل للمبيعات، كشوف الحسابات والمخزون",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }
    }
}
