package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.SpendCategory
import com.example.data.model.TransactionType

class Converters {
    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType =
        runCatching { TransactionType.valueOf(value) }.getOrDefault(TransactionType.EXPENSE)

    @TypeConverter
    fun fromSpendCategory(value: SpendCategory): String = value.name

    @TypeConverter
    fun toSpendCategory(value: String): SpendCategory =
        runCatching { SpendCategory.valueOf(value) }.getOrDefault(SpendCategory.OTHER)
}
