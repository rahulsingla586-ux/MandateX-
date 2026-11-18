package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.model.CancellationRequestEntity
import com.example.data.model.CategoryBudgetEntity
import com.example.data.model.CreditCardEntity
import com.example.data.model.LoanEmiEntity
import com.example.data.model.TransactionEntity

@Database(
    entities = [
        TransactionEntity::class,
        CategoryBudgetEntity::class,
        CancellationRequestEntity::class,
        LoanEmiEntity::class,
        CreditCardEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MandateDatabase : RoomDatabase() {
    abstract fun mandateDao(): MandateDao

    companion object {
        @Volatile
        private var INSTANCE: MandateDatabase? = null

        fun getInstance(context: Context): MandateDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MandateDatabase::class.java,
                    "mandatex_db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
