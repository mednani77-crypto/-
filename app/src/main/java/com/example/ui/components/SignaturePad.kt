package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Paint
import android.util.Base64
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import java.io.ByteArrayOutputStream

@Composable
fun SignaturePad(
    savedSignatureBase64: String,
    onSaveSignature: (String) -> Unit
) {
    val paths = remember { mutableStateListOf<List<Offset>>() }
    var currentPath by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var canvasWidth by remember { mutableIntStateOf(0) }
    var canvasHeight by remember { mutableIntStateOf(0) }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Draw,
                        contentDescription = null,
                        tint = NavyBrand
                    )
                    Text(
                        text = "التوقيع الإلكتروني لمندوب المبيعات",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                }

                if (savedSignatureBase64.isNotBlank()) {
                    Surface(
                        color = EmeraldCollectionLight,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "✓ تم الحفظ",
                            color = EmeraldCollectionDark,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Text(
                text = "وقّع بإصبعك داخل المستطيل أدناه ليُطبع التوقيع تلقائياً في أسفل الفواتير وكشوف الحسابات PDF:",
                fontSize = 12.sp,
                color = TextSecondary
            )

            // Canvas drawing surface
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF8FAFC))
                    .border(1.5.dp, BorderMedium, RoundedCornerShape(12.dp))
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                currentPath = listOf(offset)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                currentPath = currentPath + change.position
                            },
                            onDragEnd = {
                                if (currentPath.isNotEmpty()) {
                                    paths.add(currentPath)
                                    currentPath = emptyList()
                                }
                            }
                        )
                    }
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    canvasWidth = size.width.toInt()
                    canvasHeight = size.height.toInt()

                    // Guide baseline
                    drawLine(
                        color = Color(0xFFE2E8F0),
                        start = Offset(20f, size.height * 0.75f),
                        end = Offset(size.width - 20f, size.height * 0.75f),
                        strokeWidth = 2f
                    )

                    // Draw all completed paths
                    val stroke = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    for (pathPoints in paths) {
                        if (pathPoints.size > 1) {
                            val p = Path()
                            p.moveTo(pathPoints.first().x, pathPoints.first().y)
                            for (i in 1 until pathPoints.size) {
                                p.lineTo(pathPoints[i].x, pathPoints[i].y)
                            }
                            drawPath(p, color = Color(0xFF0F172A), style = stroke)
                        }
                    }

                    // Draw active path
                    if (currentPath.size > 1) {
                        val p = Path()
                        p.moveTo(currentPath.first().x, currentPath.first().y)
                        for (i in 1 until currentPath.size) {
                            p.lineTo(currentPath[i].x, currentPath[i].y)
                        }
                        drawPath(p, color = Color(0xFF0F172A), style = stroke)
                    }
                }

                if (paths.isEmpty() && currentPath.isEmpty()) {
                    Text(
                        text = "✍️ اضغط واسحب للتوقيع هنا...",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            // Buttons: Clear & Save
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        paths.clear()
                        currentPath = emptyList()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RoseReceivable)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("مسح", fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        if (paths.isNotEmpty() && canvasWidth > 0 && canvasHeight > 0) {
                            val bitmap = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
                            val canvas = android.graphics.Canvas(bitmap)
                            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = android.graphics.Color.parseColor("#0F172A")
                                strokeWidth = 6f
                                style = Paint.Style.STROKE
                                strokeCap = Paint.Cap.ROUND
                                strokeJoin = Paint.Join.ROUND
                            }

                            for (pts in paths) {
                                if (pts.size > 1) {
                                    val androidPath = android.graphics.Path()
                                    androidPath.moveTo(pts.first().x, pts.first().y)
                                    for (i in 1 until pts.size) {
                                        androidPath.lineTo(pts[i].x, pts[i].y)
                                    }
                                    canvas.drawPath(androidPath, paint)
                                }
                            }

                            val stream = ByteArrayOutputStream()
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                            val base64 = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
                            onSaveSignature(base64)
                        }
                    },
                    modifier = Modifier.weight(2f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NavyBrand)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("اعتماد وحفظ التوقيع", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
