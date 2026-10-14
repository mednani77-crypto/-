package com.example.data.model

import org.json.JSONArray
import org.json.JSONObject

object JsonHelper {
    fun serializeInvoiceItems(items: List<InvoiceItem>): String {
        val array = JSONArray()
        for (item in items) {
            val obj = JSONObject()
            obj.put("productId", item.productId)
            obj.put("productName", item.productName)
            obj.put("quantity", item.quantity)
            obj.put("unitPrice", item.unitPrice)
            obj.put("total", item.total)
            array.put(obj)
        }
        return array.toString()
    }

    fun deserializeInvoiceItems(json: String?): List<InvoiceItem> {
        if (json.isNullOrBlank()) return emptyList()
        val list = mutableListOf<InvoiceItem>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    InvoiceItem(
                        productId = obj.optLong("productId", 0L),
                        productName = obj.optString("productName", ""),
                        quantity = obj.optInt("quantity", 1),
                        unitPrice = obj.optDouble("unitPrice", 0.0),
                        total = obj.optDouble("total", 0.0)
                    )
                )
            }
        } catch (_: Exception) {
        }
        return list
    }
}
