
package com.daftar.app

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalLayoutDirection
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class Order(
    val id: Long,
    val customer: String,
    val item: String,
    val price: Double,
    val status: Int,
    val note: String,
    val date: Long
)

private val navy = Color(0xFF172554)
private val teal = Color(0xFF0D9488)
private val pageBg = Color(0xFFF4F7FB)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { DaftarApp() }
    }
}

private fun loadOrders(context: Context): List<Order> {
    return try {
        val raw = context.getSharedPreferences("daftar", 0)
            .getString("orders", "[]") ?: "[]"
        val array = JSONArray(raw)
        (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            Order(
                o.getLong("id"),
                o.getString("customer"),
                o.getString("item"),
                o.getDouble("price"),
                o.getInt("status"),
                o.optString("note", ""),
                o.getLong("date")
            )
        }
    } catch (_: Exception) {
        emptyList()
    }
}

private fun saveOrders(context: Context, orders: List<Order>) {
    val array = JSONArray()
    orders.forEach { order ->
        array.put(JSONObject().apply {
            put("id", order.id)
            put("customer", order.customer)
            put("item", order.item)
            put("price", order.price)
            put("status", order.status)
            put("note", order.note)
            put("date", order.date)
        })
    }
    context.getSharedPreferences("daftar", 0)
        .edit().putString("orders", array.toString()).apply()
}

private val translations = mapOf(
    "ar" to listOf(
        "دفتر", "إدارة طلباتك ببساطة", "إجمالي الطلبات",
        "قيد التنفيذ", "مكتملة", "بحث عن طلب أو زبون",
        "طلباتي", "إضافة طلب", "لا توجد طلبات بعد",
        "أضف أول طلب لتبدأ تنظيم تجارتك", "زبون",
        "اسم الزبون", "المنتج أو الطلب", "السعر",
        "ملاحظات (اختياري)", "حفظ الطلب", "إلغاء",
        "جديد", "قيد التنفيذ", "مكتمل", "فاتورة PDF",
        "تم حفظ الفاتورة في التنزيلات", "خطأ في إنشاء الفاتورة",
        "تاريخ", "اللغة", "الكل", "العملة: أدخل السعر بعملتك",
        "يرجى إدخال اسم الزبون والمنتج وسعر صحيح",
        "إجمالي قيمة الطلبات", "مشاركة الفاتورة"
    ),
    "en" to listOf(
        "Daftar", "Manage orders simply", "Total orders",
        "In progress", "Completed", "Search orders or customers",
        "My orders", "Add order", "No orders yet",
        "Add your first order to organize your business", "Customer",
        "Customer name", "Product or order", "Price",
        "Notes (optional)", "Save order", "Cancel",
        "New", "In progress", "Completed", "PDF invoice",
        "Invoice saved to Downloads", "Could not create invoice",
        "Date", "Language", "All", "Enter prices in your currency",
        "Enter customer, product and a valid price",
        "Total order value", "Share invoice"
    ),
    "fr" to listOf(
        "Daftar", "Gérez vos commandes simplement", "Commandes",
        "En cours", "Terminées", "Rechercher une commande",
        "Mes commandes", "Ajouter", "Aucune commande",
        "Ajoutez votre première commande", "Client",
        "Nom du client", "Produit ou commande", "Prix",
        "Notes (facultatif)", "Enregistrer", "Annuler",
        "Nouveau", "En cours", "Terminé", "Facture PDF",
        "Facture enregistrée dans Téléchargements", "Erreur de facture",
        "Date", "Langue", "Tout", "Saisissez le prix dans votre devise",
        "Saisissez le client, le produit et un prix valide",
        "Valeur totale", "Partager la facture"
    ),
    "es" to listOf(
        "Daftar", "Gestiona tus pedidos fácilmente", "Pedidos totales",
        "En curso", "Completados", "Buscar pedidos o clientes",
        "Mis pedidos", "Añadir pedido", "No hay pedidos",
        "Añade tu primer pedido", "Cliente",
        "Nombre del cliente", "Producto o pedido", "Precio",
        "Notas (opcional)", "Guardar pedido", "Cancelar",
        "Nuevo", "En curso", "Completado", "Factura PDF",
        "Factura guardada en Descargas", "Error al crear factura",
        "Fecha", "Idioma", "Todos", "Introduce el precio en tu moneda",
        "Introduce cliente, producto y un precio válido",
        "Valor total", "Compartir factura"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DaftarApp() {
    val context = LocalContext.current
    var orders by remember { mutableStateOf(loadOrders(context)) }
    var language by remember { mutableStateOf("ar") }
    var search by remember { mutableStateOf("") }
    var filter by remember { mutableIntStateOf(-1) }
    var showAdd by remember { mutableStateOf(false) }
    var showLanguages by remember { mutableStateOf(false) }
    val t = translations[language] ?: translations.getValue("ar")
    val rtl = language == "ar"

    LaunchedEffect(orders) { saveOrders(context, orders) }

    CompositionLocalProvider(
        LocalLayoutDirection provides
            if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    ) {
        MaterialTheme(
            colorScheme = lightColorScheme(
                primary = navy,
                secondary = teal,
                background = pageBg,
                surface = Color.White
            )
        ) {
            Scaffold(
                containerColor = pageBg,
                floatingActionButton = {
                    ExtendedFloatingActionButton(
                        onClick = { showAdd = true },
                        containerColor = teal,
                        contentColor = Color.White,
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text("+  ${t[7]}", fontWeight = FontWeight.Bold)
                    }
                }
            ) { padding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(
                        start = 18.dp, end = 18.dp,
                        top = 14.dp, bottom = 100.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    t[0],
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = navy
                                )
                                Text(t[1], color = Color.Gray)
                            }
                            Box {
                                OutlinedButton(
                                    onClick = { showLanguages = true },
                                    shape = RoundedCornerShape(12.dp)
                                ) { Text("🌐 ${language.uppercase()}") }
                                DropdownMenu(
                                    expanded = showLanguages,
                                    onDismissRequest = { showLanguages = false }
                                ) {
                                    listOf(
                                        "ar" to "العربية",
                                        "en" to "English",
                                        "fr" to "Français",
                                        "es" to "Español"
                                    ).forEach { (code, name) ->
                                        DropdownMenuItem(
                                            text = { Text(name) },
                                            onClick = {
                                                language = code
                                                showLanguages = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = navy
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(22.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    t[2],
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Text(
                                    orders.size.toString(),
                                    color = Color.White,
                                    style = MaterialTheme.typography.displayMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                HorizontalDivider(
                                    color = Color.White.copy(alpha = 0.2f)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(t[3], color = Color.White.copy(alpha = 0.8f))
                                        Text(
                                            orders.count { it.status == 1 }.toString(),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Column {
                                        Text(t[4], color = Color.White.copy(alpha = 0.8f))
                                        Text(
                                            orders.count { it.status == 2 }.toString(),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Text(
                                    "${t[27]}: %.2f".format(
                                        Locale.US, orders.sumOf { it.price }
                                    ),
                                    color = Color(0xFF99F6E4),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = search,
                            onValueChange = { search = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text(t[5]) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp)
                        )
                    }

                    item {
                        Text(
                            t[6],
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = navy
                        )
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(-1, 0, 1, 2).forEach { status ->
                                FilterChip(
                                    selected = filter == status,
                                    onClick = { filter = status },
                                    label = {
                                        Text(
                                            when (status) {
                                                -1 -> t[25]
                                                0 -> t[17]
                                                1 -> t[18]
                                                else -> t[19]
                                            }
                                        )
                                    }
                                )
                            }
                        }
                    }

                    val filtered = orders.filter {
                        (it.customer.contains(search, true) ||
                            it.item.contains(search, true)) &&
                            (filter == -1 || it.status == filter)
                    }

                    if (filtered.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(26.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("📋", style = MaterialTheme.typography.displaySmall)
                                    Spacer(Modifier.height(10.dp))
                                    Text(t[8], fontWeight = FontWeight.Bold)
                                    Text(t[9], color = Color.Gray)
                                }
                            }
                        }
                    } else {
                        items(filtered, key = { it.id }) { order ->
                            OrderCard(
                                order = order,
                                t = t,
                                onStatus = {
                                    orders = orders.map {
                                        if (it.id == order.id)
                                            it.copy(status = (it.status + 1) % 3)
                                        else it
                                    }
                                },
                                onInvoice = {
                                    createInvoice(context, order, t)
                                },
                                onDelete = {
                                    orders = orders.filterNot { it.id == order.id }
                                }
                            )
                        }
                    }
                }
            }

            if (showAdd) {
                AddOrderDialog(
                    t = t,
                    onDismiss = { showAdd = false },
                    onSave = { customer, item, price, note ->
                        orders = orders + Order(
                            System.currentTimeMillis(),
                            customer.trim(),
                            item.trim(),
                            price,
                            0,
                            note.trim(),
                            System.currentTimeMillis()
                        )
                        showAdd = false
                    }
                )
            }
        }
    }
}

@Composable
private fun OrderCard(
    order: Order,
    t: List<String>,
    onStatus: () -> Unit,
    onInvoice: () -> Unit,
    onDelete: () -> Unit
) {
    val statusColor = when (order.status) {
        0 -> Color(0xFF64748B)
        1 -> Color(0xFFD97706)
        else -> teal
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        order.customer,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = navy
                    )
                    Text(order.item, color = Color.DarkGray)
                }
                Text(
                    "%.2f".format(Locale.US, order.price),
                    color = teal,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Text(
                "${t[23]}: " + SimpleDateFormat(
                    "yyyy-MM-dd", Locale.getDefault()
                ).format(Date(order.date)),
                color = Color.Gray,
                style = MaterialTheme.typography.bodySmall
            )

            if (order.note.isNotBlank()) {
                Text(order.note, color = Color.DarkGray)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onStatus) {
                    Text(
                        "● " + t[17 + order.status],
                        color = statusColor
                    )
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onInvoice) {
                    Text("PDF", color = navy)
                }
                TextButton(onClick = onDelete) {
                    Text("✕", color = Color(0xFFDC2626))
                }
            }
        }
    }
}

@Composable
private fun AddOrderDialog(
    t: List<String>,
    onDismiss: () -> Unit,
    onSave: (String, String, Double, String) -> Unit
) {
    var customer by remember { mutableStateOf("") }
    var item by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(t[7], fontWeight = FontWeight.Bold, color = navy)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = customer,
                    onValueChange = { customer = it },
                    label = { Text(t[11]) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = item,
                    onValueChange = { item = it },
                    label = { Text(t[12]) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text(t[13]) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    singleLine = true
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(t[14]) }
                )
                if (error) {
                    Text(t[26], color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val amount = price.replace(',', '.').toDoubleOrNull()
                if (customer.isBlank() || item.isBlank() ||
                    amount == null || amount < 0.0
                ) {
                    error = true
                } else {
                    onSave(customer, item, amount, note)
                }
            }) { Text(t[15]) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(t[16]) }
        }
    )
}

private fun createInvoice(
    context: Context,
    order: Order,
    t: List<String>
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        Toast.makeText(context, t[22], Toast.LENGTH_LONG).show()
        return
    }

    val pdf = PdfDocument()
    val page = pdf.startPage(
        PdfDocument.PageInfo.Builder(595, 842, 1).create()
    )
    val canvas = page.canvas
    val paint = Paint().apply {
        color = android.graphics.Color.rgb(23, 37, 84)
        textSize = 26f
        isFakeBoldText = true
    }

    canvas.drawText("DAFTAR | ${t[20]}", 40f, 65f, paint)
    paint.textSize = 16f
    paint.isFakeBoldText = false
    paint.color = android.graphics.Color.DKGRAY
    canvas.drawText("${t[11]}: ${order.customer}", 40f, 130f, paint)
    canvas.drawText("${t[12]}: ${order.item}", 40f, 170f, paint)
    canvas.drawText(
        "${t[13]}: ${"%.2f".format(Locale.US, order.price)}",
        40f, 210f, paint
    )
    canvas.drawText(
        "${t[23]}: " + SimpleDateFormat(
            "yyyy-MM-dd", Locale.getDefault()
        ).format(Date(order.date)),
        40f, 250f, paint
    )
    if (order.note.isNotBlank()) {
        canvas.drawText("${t[14]}: ${order.note.take(55)}", 40f, 290f, paint)
    }
    paint.color = android.graphics.Color.rgb(13, 148, 136)
    paint.textSize = 13f
    canvas.drawText("Created with Daftar", 40f, 800f, paint)
    pdf.finishPage(page)

    val filename = "Daftar_${order.id}.pdf"
    var uri: android.net.Uri? = null

    try {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/Daftar")
        }
        uri = context.contentResolver.insert(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI, values
        )
        if (uri == null) throw IllegalStateException("No file URI")

        context.contentResolver.openOutputStream(uri!!).use { output ->
            if (output == null) throw IllegalStateException("No output stream")
            pdf.writeTo(output)
        }

        Toast.makeText(context, t[21], Toast.LENGTH_LONG).show()

        val share = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(share, t[29]))
    } catch (_: Exception) {
        uri?.let {
            try { context.contentResolver.delete(it, null, null) }
            catch (_: Exception) {}
        }
        Toast.makeText(context, t[22], Toast.LENGTH_LONG).show()
    } finally {
        pdf.close()
    }
}
