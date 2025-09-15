package com.offtime.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "AppCategory_Defaults")
data class AppCategoryDefaultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val displayOrder: Int,
    val isDefault: Boolean = false,
    val isLocked: Boolean = false, // 总使用分类被锁定，不能删除
    val targetType: String, // "LESS_THAN" 或 "MORE_THAN"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) 