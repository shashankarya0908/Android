package com.fable.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [BookCacheEntity::class, SearchHistoryEntity::class, LibraryCacheEntity::class, BookReviewEntity::class],
    version = 3,
    exportSchema = true,
)
abstract class FableDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `user_books` (`userId` TEXT NOT NULL, `bookId` TEXT NOT NULL, `bookJson` TEXT NOT NULL, `status` TEXT NOT NULL, `currentPage` INTEGER NOT NULL, `totalPages` INTEGER, `startedAt` INTEGER, `finishedAt` INTEGER, `addedAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `personalNotes` TEXT, `completionHistoryJson` TEXT NOT NULL, `syncPending` INTEGER NOT NULL, `deleted` INTEGER NOT NULL, PRIMARY KEY(`userId`, `bookId`))""",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_user_books_userId` ON `user_books` (`userId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_user_books_status` ON `user_books` (`status`)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `book_reviews` (`userId` TEXT NOT NULL, `bookId` TEXT NOT NULL, `reviewerUsername` TEXT NOT NULL, `bookTitle` TEXT NOT NULL, `bookThumbnailUrl` TEXT, `rating` INTEGER NOT NULL, `reviewText` TEXT, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `pendingSync` INTEGER NOT NULL, `deleted` INTEGER NOT NULL, PRIMARY KEY(`userId`, `bookId`))""",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_book_reviews_bookId` ON `book_reviews` (`bookId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_book_reviews_userId` ON `book_reviews` (`userId`)")
            }
        }
    }
}
