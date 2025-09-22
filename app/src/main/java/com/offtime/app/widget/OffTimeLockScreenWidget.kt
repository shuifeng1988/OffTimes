package com.offtime.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.widget.RemoteViews
import com.offtime.app.R
import com.offtime.app.data.database.OffTimeDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import com.offtime.app.OffTimeApplication
import com.offtime.app.utils.DateLocalizer
import com.offtime.app.utils.LocaleUtils
import android.content.BroadcastReceiver
import android.content.ComponentName

/**
 * 锁屏小部件 - 显示默认分类的饼图和日使用时间折线图
 */
class OffTimeLockScreenWidget : AppWidgetProvider() {

    /**
     * 获取应用内语言设置的本地化Context
     */
    private fun getLocalizedContext(context: Context): Context {
        return try {
            val localeUtils = LocaleUtils(context)
            localeUtils.applyLanguageToContext(context)
        } catch (e: Exception) {
            android.util.Log.w("LockScreenWidget", "获取本地化Context失败，使用默认Context: ${e.message}")
            context
        }
    }

    companion object {
        const val ACTION_UPDATE_WIDGET = "com.offtime.app.widget.ACTION_UPDATE_WIDGET"
        /**
         * 刷新所有Widget实例（当语言设置变更时调用）
         */
        fun refreshAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val widgetComponent = android.content.ComponentName(context, OffTimeLockScreenWidget::class.java)
            val widgetIds = appWidgetManager.getAppWidgetIds(widgetComponent)
            
            if (widgetIds.isNotEmpty()) {
                val widget = OffTimeLockScreenWidget()
                widget.onUpdate(context, appWidgetManager, widgetIds)
                android.util.Log.d("LockScreenWidget", "已刷新 ${widgetIds.size} 个Widget实例")
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // 为每个小部件实例更新内容
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }



    override fun onEnabled(context: Context) {
        // 第一个小部件实例被创建时调用
        super.onEnabled(context)
    }

    override fun onDisabled(context: Context) {
        // 最后一个小部件实例被删除时调用
        super.onDisabled(context)
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 获取数据库实例
                val database = OffTimeDatabase.getDatabase(context)

                // 获取配置的显示天数
                val settings = database.appSettingsDao().getSettings()
                val displayDays = settings?.widgetDisplayDays ?: 30
                val defaultCategoryId = settings?.defaultCategoryId ?: 1
                
                // 获取所有分类
                val allCategories = database.appCategoryDao().getAllCategoriesList()
                
                // 根据默认分类ID获取分类，如果找不到则用第一个
                val currentCategory = allCategories.find { it.id == defaultCategoryId } 
                    ?: allCategories.firstOrNull()
                
                if (currentCategory != null) {
                    // 获取今日数据
                    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    val realUsage = database.dailyUsageDao().getTotalUsageByCategoryAndType(currentCategory.id, today, 0) ?: 0
                    val virtualUsage = database.dailyUsageDao().getTotalUsageByCategoryAndType(currentCategory.id, today, 1) ?: 0
                    val totalUsageMinutes = (realUsage + virtualUsage) / 60
                    
                    // 获取目标时间
                    val goal = database.goalRewardPunishmentUserDao().getUserGoalByCatId(currentCategory.id)
                    val goalMinutes = (goal?.dailyGoalMin ?: 120)
                    
                    // 获取最近指定天数的使用数据
                    val usageHistory = getRecentUsageData(database, currentCategory.id, displayDays)
                    
                    // 获取应用安装时间用于判断样式
                    val appInstallTime = getAppInstallTime(context)
                    
                    // 创建小部件视图
                    val views = createWidgetViews(
                        context,
                        currentCategory,
                        allCategories,
                        totalUsageMinutes,
                        goalMinutes,
                        usageHistory,
                        appInstallTime,
                        appWidgetId
                    )
                    
                    // 更新小部件
                    withContext(Dispatchers.Main) {
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                } else {
                    // 没有分类数据时显示默认视图
                    val views = createDefaultViews(context)
                    withContext(Dispatchers.Main) {
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("LockScreenWidget", "更新小部件失败", e)
                // 出错时显示默认视图
                val views = createDefaultViews(context)
                withContext(Dispatchers.Main) {
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }
    }

    private suspend fun getRecentUsageData(
        database: OffTimeDatabase,
        categoryId: Int,
        days: Int
    ): List<Int> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        val usageData = mutableListOf<Int>()
        
        for (i in days - 1 downTo 0) {
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            val date = dateFormat.format(calendar.time)
            
            val realUsage = database.dailyUsageDao().getTotalUsageByCategoryAndType(categoryId, date, 0) ?: 0
            val virtualUsage = database.dailyUsageDao().getTotalUsageByCategoryAndType(categoryId, date, 1) ?: 0
            val totalMinutes = (realUsage + virtualUsage) / 60
            
            usageData.add(totalMinutes)
        }
        
        return usageData
    }

    private fun getAppInstallTime(context: Context): Long {
        return try {
            val packageManager = context.packageManager
            val packageInfo = packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.firstInstallTime
        } catch (e: Exception) {
            android.util.Log.w("LockScreenWidget", "获取应用安装时间失败", e)
            // 返回一个很早的时间作为默认值，这样所有日期都会被视为安装后
            0L
        }
    }

    private fun createWidgetViews(
        context: Context,
        currentCategory: com.offtime.app.data.entity.AppCategoryEntity,
        allCategories: List<com.offtime.app.data.entity.AppCategoryEntity>,
        totalUsageMinutes: Int,
        goalMinutes: Int,
        usageHistory: List<Int>,
        appInstallTime: Long,
        appWidgetId: Int
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_lock_screen)
        
        // 获取本地化的Context
        val localizedContext = getLocalizedContext(context)
        
        // 设置分类图标和名称
        val categoryIcon = when (currentCategory.name) {
            "娱乐" -> "🎮"
            "学习" -> "📚"
            "健身" -> "💪"
            "总使用" -> "📱"
            "工作" -> "💼"
            "其他" -> "📱"
            else -> "📁"
        }
        views.setTextViewText(R.id.widget_category_icon, categoryIcon)
        views.setTextViewText(R.id.widget_category_name, DateLocalizer.getCategoryName(localizedContext, currentCategory.name))
        
        // 设置使用时间信息
        val minutesUnit = DateLocalizer.getTimeUnitShort(localizedContext, "minutes")
        val usageText = "${totalUsageMinutes}${minutesUnit} / ${goalMinutes}${minutesUnit}"
        views.setTextViewText(R.id.widget_usage_text, usageText)
        
        // 计算完成百分比
        val completionRate = if (goalMinutes > 0) {
            (totalUsageMinutes.toFloat() / goalMinutes * 100).coerceAtMost(100f)
        } else 0f
        views.setTextViewText(R.id.widget_completion_text, "${completionRate.toInt()}%")
        
        // 生成折线图 - 使用动态尺寸和安装时间信息
        val lineChartBitmap = createLineChartBitmap(localizedContext, usageHistory, appInstallTime)
        views.setImageViewBitmap(R.id.widget_line_chart, lineChartBitmap)
        
        // 设置分类切换按钮点击事件
        setupCategorySwitchButtons(context, views, currentCategory, allCategories, appWidgetId)
        
        // 设置点击事件 - 打开主应用
        val intent = Intent(context, com.offtime.app.MainActivity::class.java)
        val pendingIntent = android.app.PendingIntent.getActivity(
            context, 0, intent, 
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)
        
        return views
    }

    private fun setupCategorySwitchButtons(
        context: Context,
        views: RemoteViews,
        currentCategory: com.offtime.app.data.entity.AppCategoryEntity,
        allCategories: List<com.offtime.app.data.entity.AppCategoryEntity>,
        appWidgetId: Int
    ) {
        if (allCategories.size <= 1) {
            // 如果只有一个或没有分类，隐藏切换按钮
            views.setViewVisibility(R.id.widget_prev_category_btn, android.view.View.GONE)
            views.setViewVisibility(R.id.widget_next_category_btn, android.view.View.GONE)
            return
        }

        views.setViewVisibility(R.id.widget_prev_category_btn, android.view.View.VISIBLE)
        views.setViewVisibility(R.id.widget_next_category_btn, android.view.View.VISIBLE)

        val currentIndex = allCategories.indexOfFirst { it.id == currentCategory.id }
        
        // 设置左箭头点击事件（上一个分类）
        val prevIndex = if (currentIndex > 0) currentIndex - 1 else allCategories.size - 1
        val prevCategoryId = allCategories[prevIndex].id
        val prevIntent = Intent(context, OffTimeLockScreenWidget::class.java).apply {
            action = "SWITCH_CATEGORY"
            putExtra("categoryId", prevCategoryId)
            putExtra("appWidgetId", appWidgetId)
        }
        val prevPendingIntent = android.app.PendingIntent.getBroadcast(
            context, 
            appWidgetId * 1000 + prevCategoryId, // 确保唯一性
            prevIntent, 
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_prev_category_btn, prevPendingIntent)
        
        // 设置右箭头点击事件（下一个分类）
        val nextIndex = if (currentIndex < allCategories.size - 1) currentIndex + 1 else 0
        val nextCategoryId = allCategories[nextIndex].id
        val nextIntent = Intent(context, OffTimeLockScreenWidget::class.java).apply {
            action = "SWITCH_CATEGORY"
            putExtra("categoryId", nextCategoryId)
            putExtra("appWidgetId", appWidgetId)
        }
        val nextPendingIntent = android.app.PendingIntent.getBroadcast(
            context, 
            appWidgetId * 1000 + nextCategoryId, // 确保唯一性
            nextIntent, 
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_next_category_btn, nextPendingIntent)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        if (intent.action == "SWITCH_CATEGORY") {
            val categoryId = intent.getIntExtra("categoryId", -1)
            val appWidgetId = intent.getIntExtra("appWidgetId", -1)
            
            if (categoryId != -1 && appWidgetId != -1) {
                // 在协程中更新默认分类设置并刷新widget
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val database = OffTimeDatabase.getDatabase(context)
                        val settings = database.appSettingsDao().getSettings()
                        
                        if (settings != null) {
                            val updatedSettings = settings.copy(
                                defaultCategoryId = categoryId,
                                updatedAt = System.currentTimeMillis()
                            )
                            database.appSettingsDao().insertOrUpdateSettings(updatedSettings)
                            
                            // 刷新widget
                            withContext(Dispatchers.Main) {
                                val appWidgetManager = AppWidgetManager.getInstance(context)
                                updateAppWidget(context, appWidgetManager, appWidgetId)
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("LockScreenWidget", "分类切换失败", e)
                    }
                }
            }
        }
    }

    /**
     * 广播接收器，用于接收外部更新请求
     */
    class WidgetUpdateReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_UPDATE_WIDGET) {
                android.util.Log.d("LockScreenWidget", "接收到Widget更新广播")
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val widgetComponent = ComponentName(context, OffTimeLockScreenWidget::class.java)
                val widgetIds = appWidgetManager.getAppWidgetIds(widgetComponent)
                
                if (widgetIds.isNotEmpty()) {
                    OffTimeLockScreenWidget().onUpdate(context, appWidgetManager, widgetIds)
                }
            }
        }
    }

    private fun createDefaultViews(context: Context): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_lock_screen)
        
        // 获取本地化的Context
        val localizedContext = getLocalizedContext(context)
        
        // 设置默认图标和文本
        views.setTextViewText(R.id.widget_category_icon, "📱")
        views.setTextViewText(R.id.widget_category_name, "OffTimes")
        views.setTextViewText(R.id.widget_usage_text, localizedContext.getString(R.string.no_data))
        views.setTextViewText(R.id.widget_completion_text, "0%")
        
        // 隐藏切换按钮
        views.setViewVisibility(R.id.widget_prev_category_btn, android.view.View.GONE)
        views.setViewVisibility(R.id.widget_next_category_btn, android.view.View.GONE)
        
        // 设置默认折线图
        val defaultBitmap = createDefaultChartBitmap(localizedContext)
        views.setImageViewBitmap(R.id.widget_line_chart, defaultBitmap)
        
        return views
    }

    private fun createLineChartBitmap(
        context: Context,
        usageHistory: List<Int>, 
        appInstallTime: Long
    ): Bitmap {
        // 根据屏幕密度和尺寸动态调整折线图尺寸
        val appContext = OffTimeApplication.instance
        val displayMetrics = appContext.resources.displayMetrics
        val density = displayMetrics.density
        val screenWidthPixels = displayMetrics.widthPixels
        val screenHeightPixels = displayMetrics.heightPixels
        
        // 计算屏幕对角线尺寸（英寸）
        val diagonalPixels = kotlin.math.sqrt(
            (screenWidthPixels * screenWidthPixels + screenHeightPixels * screenHeightPixels).toDouble()
        )
        val diagonalInches = diagonalPixels / displayMetrics.densityDpi
        
        // 判断是否为Pixel 4（5.7英寸，1080×2280，444 PPI，设备像素比2.75）
        val isPixel4 = kotlin.math.abs(diagonalInches - 5.7) < 0.1 && 
                      kotlin.math.abs(density - 2.75f) < 0.1f &&
                      screenWidthPixels == 1080 && screenHeightPixels == 2280
        
        // 判断是否为其他5.7英寸屏幕（Pixel 5类型）
        val isSmallScreen = diagonalInches <= 6.0 && density >= 2.5f && !isPixel4
        
        // 判断是否为大屏设备（暂时禁用，让Pixel 9 Pro XL使用标准配置）
        val isLargeScreen = false  // diagonalInches >= 6.5 && density >= 2.5f
        
        // 添加调试日志
        android.util.Log.d("WidgetDebug", "屏幕信息: ${screenWidthPixels}x${screenHeightPixels}px, " +
            "对角线: ${diagonalInches}英寸, 密度: ${density}, " +
            "isPixel4: $isPixel4, isSmallScreen: $isSmallScreen, isLargeScreen: $isLargeScreen")
        
        // 根据屏幕大小调整尺寸
        val (width, height, fontSizeMultiplier) = when {
            isPixel4 -> {
                // Pixel 4专门适配：增加一倍高度，宽度与widget匹配，字体也相应增大
                Triple(1120, 560, 1.4f)
            }
            isLargeScreen -> {
                // 大屏设备（如Pixel 9 Pro XL）：高度300px，保持宽高比2.29:1
                Triple(687, 300, 1.1f)
            }
            isSmallScreen -> {
                // 其他5.7英寸屏幕：减小尺寸
                Triple(560, 240, 0.85f)
            }
            else -> {
                // 标准屏幕（如Pixel 8）：保持原尺寸
                Triple(640, 280, 1.0f)
            }
        }
        
        // 添加尺寸调试日志
        android.util.Log.d("WidgetDebug", "选择的图表尺寸: ${width}x${height}px, 字体倍数: ${fontSizeMultiplier}")
        
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // 绘制白色背景
        canvas.drawColor(Color.WHITE)
        
        val paint = Paint().apply {
            isAntiAlias = true
            strokeWidth = when {
                isPixel4 -> 3.5f
                isLargeScreen -> 2.5f
                isSmallScreen -> 1.8f
                else -> 2f
            }
        }
        
        val padding = when {
            isPixel4 -> 16f
            isLargeScreen -> 12f
            isSmallScreen -> 8f
            else -> 10f
        }
        val bottomPadding = when {
            isPixel4 -> 80f
            isLargeScreen -> 36f  // 适应新的300px高度
            isSmallScreen -> 35f
            else -> 40f
        }
        val leftPadding = when {
            isPixel4 -> 140f
            isLargeScreen -> 86f  // 按比例调整：100f * (687/800) ≈ 86f
            isSmallScreen -> 70f
            else -> 80f
        }
        val chartWidth = width - leftPadding - padding
        val chartHeight = height - padding - bottomPadding
        val chartLeft = leftPadding
        val chartTop = padding
        val chartRight = width - padding
        val chartBottom = padding + chartHeight
        
        if (usageHistory.isNotEmpty()) {
            // 根据实际数值范围确定纵坐标高度，移除目标值的影响
            val actualMaxValue = usageHistory.maxOrNull() ?: 0
            val maxValue = maxOf(actualMaxValue, 30) // 至少显示30分钟的范围
            
            // 绘制网格线 
            paint.color = Color.parseColor("#E0E0E0")
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = when {
                isPixel4 -> 1.6f
                isLargeScreen -> 1.2f
                isSmallScreen -> 0.8f
                else -> 1f
            }
            
            // 水平网格线 - 减少数量避免过密
            for (i in 0..4) {
                val y = chartTop + (chartHeight * i / 4f)
                canvas.drawLine(chartLeft, y, chartRight, y, paint)
            }
            
            // 垂直网格线 - 只绘制关键位置
            if (usageHistory.size > 1) {
                val step = maxOf(1, usageHistory.size / 6) // 最多6条垂直线
                for (i in 0 until usageHistory.size step step) {
                    val x = chartLeft + (chartWidth * i / (usageHistory.size - 1))
                    canvas.drawLine(x, chartTop, x, chartBottom, paint)
                }
            }
            
            // 绘制使用时间折线，根据安装前后使用不同样式
            val points = mutableListOf<android.graphics.PointF>()
            val lineCalendar = Calendar.getInstance()
            
            // 计算所有点的坐标和对应日期
            for (i in usageHistory.indices) {
                val x = if (usageHistory.size == 1) {
                    chartLeft + chartWidth / 2
                } else {
                    chartLeft + (chartWidth * i / (usageHistory.size - 1))
                }
                val y = chartBottom - (usageHistory[i].toFloat() / maxValue * chartHeight)
                points.add(android.graphics.PointF(x, y))
            }
            
            // 分段绘制折线，根据安装前后使用不同样式
            for (i in 0 until points.size - 1) {
                // 计算当前点对应的日期
                lineCalendar.time = Date()
                lineCalendar.add(Calendar.DAY_OF_YEAR, -(usageHistory.size - 1 - i))
                val currentDate = lineCalendar.timeInMillis
                
                // 判断是否在应用安装之前
                val isBeforeInstall = currentDate < appInstallTime
                
                if (isBeforeInstall) {
                    // 安装前：灰色虚线
                    paint.color = Color.parseColor("#AAAAAA")
                    paint.pathEffect = android.graphics.DashPathEffect(floatArrayOf(
                        when {
                            isPixel4 -> 16f
                            isLargeScreen -> 12f
                            isSmallScreen -> 8f
                            else -> 10f
                        }, 
                        when {
                            isPixel4 -> 8f
                            isLargeScreen -> 6f
                            isSmallScreen -> 4f
                            else -> 5f
                        }
                    ), 0f)
                } else {
                    // 安装后：蓝色实线
                    paint.color = Color.parseColor("#2196F3")
                    paint.pathEffect = null
                }
                
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = when {
                    isPixel4 -> 5f
                    isLargeScreen -> 4f
                    isSmallScreen -> 2.5f
                    else -> 3f
                }
                
                canvas.drawLine(
                    points[i].x, points[i].y,
                    points[i + 1].x, points[i + 1].y,
                    paint
                )
            }
            
            // 绘制数据点
            for (i in points.indices) {
                // 计算当前点对应的日期
                lineCalendar.time = Date()
                lineCalendar.add(Calendar.DAY_OF_YEAR, -(usageHistory.size - 1 - i))
                val currentDate = lineCalendar.timeInMillis
                
                // 判断是否在应用安装之前
                val isBeforeInstall = currentDate < appInstallTime
                
                paint.style = Paint.Style.FILL
                paint.pathEffect = null
                
                if (isBeforeInstall) {
                    // 安装前：灰色实点
                    paint.color = Color.parseColor("#AAAAAA")
                } else {
                    // 安装后：蓝色实点
                    paint.color = Color.parseColor("#2196F3")
                }
                
                val pointRadius = when {
                    isPixel4 -> 6.5f
                    isLargeScreen -> 5f
                    isSmallScreen -> 3.5f
                    else -> 4f
                }
                canvas.drawCircle(points[i].x, points[i].y, pointRadius, paint)
            }
            
            // 绘制坐标轴
            paint.color = Color.parseColor("#333333")
            paint.strokeWidth = when {
                isPixel4 -> 2.5f
                isLargeScreen -> 2f
                isSmallScreen -> 1.3f
                else -> 1.5f
            }
            paint.style = Paint.Style.STROKE
            paint.pathEffect = null
            
            // Y轴
            canvas.drawLine(chartLeft, chartTop, chartLeft, chartBottom, paint)
            // X轴
            canvas.drawLine(chartLeft, chartBottom, chartRight, chartBottom, paint)
            
            // 添加Y轴标签 - 使用小时表示
            val yLabelPaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#666666")
                textSize = (24f * fontSizeMultiplier) // 根据屏幕调整字体大小
                textAlign = Paint.Align.RIGHT
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                isFakeBoldText = false
            }
            
            // 绘制Y轴刻度线和标签的Paint
            val yTickPaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#333333")
                strokeWidth = when {
                    isPixel4 -> 6f
                    isLargeScreen -> 5f
                    isSmallScreen -> 3.5f
                    else -> 4f
                }
                style = Paint.Style.STROKE
            }
            
            for (i in 0..4) {
                val valueMinutes = (maxValue * (4 - i) / 4f).toInt()
                val valueHours = valueMinutes / 60.0
                val y = chartTop + (chartHeight * i / 4f)
                val text = String.format("%.1fh", valueHours)
                
                // 绘制向右的加粗刻度线
                val tickLength = when {
                    isPixel4 -> 20f
                    isLargeScreen -> 16f
                    isSmallScreen -> 10f
                    else -> 12f
                }
                canvas.drawLine(chartLeft - tickLength, y, chartLeft, y, yTickPaint)
                
                // 精确计算文字位置，避免扭曲
                val textY = y + (yLabelPaint.descent() - yLabelPaint.ascent()) / 2 - yLabelPaint.descent()
                val textOffset = when {
                    isPixel4 -> 26f
                    isLargeScreen -> 20f
                    isSmallScreen -> 14f
                    else -> 16f
                }
                canvas.drawText(text, chartLeft - textOffset, textY, yLabelPaint)
            }
            
            // 添加X轴标签（日期）- 只显示4个关键节点
            val xLabelPaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#666666")
                textSize = (24f * fontSizeMultiplier) // 与Y轴标签字体大小一致
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                isFakeBoldText = false
                // textAlign 将在绘制时动态设置
            }
            
            // 定义4个关键节点的索引：最早一天、中间两天、今天
            val keyIndices = if (usageHistory.size >= 4) {
                listOf(
                    0,  // 最早一天
                    usageHistory.size / 3,  // 第一个中间点
                    usageHistory.size * 2 / 3,  // 第二个中间点
                    usageHistory.size - 1  // 今天
                )
            } else {
                // 如果数据不足4个点，显示所有可用的点
                usageHistory.indices.toList()
            }
            
            // 生成日期标签 - 使用与首页相同的格式化逻辑
            val labelDateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val labelCalendar = java.util.Calendar.getInstance()
            
            // 绘制刻度线和标签的Paint
            val tickPaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#333333")
                strokeWidth = when {
                    isPixel4 -> 6f
                    isLargeScreen -> 5f
                    isSmallScreen -> 3.5f
                    else -> 4f
                }
                style = Paint.Style.STROKE
            }
            
            for (i in keyIndices) {
                val x = if (usageHistory.size == 1) {
                    chartLeft + chartWidth / 2
                } else {
                    chartLeft + (chartWidth * i / (usageHistory.size - 1))
                }
                
                // 绘制向下的加粗刻度线
                val tickLength = when {
                    isPixel4 -> 20f
                    isLargeScreen -> 16f
                    isSmallScreen -> 10f
                    else -> 12f
                }
                canvas.drawLine(x, chartBottom, x, chartBottom + tickLength, tickPaint)
                
                // 计算对应的日期
                labelCalendar.time = java.util.Date()
                labelCalendar.add(java.util.Calendar.DAY_OF_YEAR, -(usageHistory.size - 1 - i))
                val fullDateString = labelDateFormat.format(labelCalendar.time)
                
                // 格式化日期标签：最后一天显示"今天"，其他显示MM-dd格式
                val dateLabel = com.offtime.app.utils.DateLocalizer.formatWidgetDateLabel(
                    context, 
                    fullDateString, 
                    i == usageHistory.size - 1
                )
                
                // 根据位置调整标签对齐方式和位置，避免截断
                val labelOffset = when {
                    isPixel4 -> 14f
                    isLargeScreen -> 10f
                    isSmallScreen -> 7f
                    else -> 8f
                }
                val adjustedX = when {
                    i == 0 -> {
                        // 最左边的标签：左对齐，向右偏移
                        xLabelPaint.textAlign = Paint.Align.LEFT
                        x + labelOffset
                    }
                    i == usageHistory.size - 1 -> {
                        // 最右边的标签（"今天"）：右对齐，向左偏移
                        xLabelPaint.textAlign = Paint.Align.RIGHT
                        x - labelOffset
                    }
                    else -> {
                        // 中间的标签：居中对齐
                        xLabelPaint.textAlign = Paint.Align.CENTER
                        x
                    }
                }
                
                // 精确计算X轴标签位置
                val textOffsetY = when {
                    isPixel4 -> 35f
                    isLargeScreen -> 28f
                    isSmallScreen -> 18f
                    else -> 21f
                }
                val textY = chartBottom + textOffsetY + (xLabelPaint.descent() - xLabelPaint.ascent()) / 2
                canvas.drawText(dateLabel, adjustedX, textY, xLabelPaint)
            }
            
        } else {
            // 没有数据时显示提示
            val textPaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#999999")
                textSize = (13f * fontSizeMultiplier)
                textAlign = Paint.Align.CENTER
                typeface = android.graphics.Typeface.DEFAULT
            }
            val noDataText = context.getString(com.offtime.app.R.string.no_data_available)
            canvas.drawText(noDataText, width / 2f, height / 2f, textPaint)
        }
        
        return bitmap
    }

    private fun createDefaultChartBitmap(context: Context): Bitmap {
        // 根据屏幕尺寸调整默认图表尺寸
        val appContext = OffTimeApplication.instance
        val displayMetrics = appContext.resources.displayMetrics
        val density = displayMetrics.density
        val screenWidthPixels = displayMetrics.widthPixels
        val screenHeightPixels = displayMetrics.heightPixels
        
        // 计算屏幕对角线尺寸
        val diagonalPixels = kotlin.math.sqrt(
            (screenWidthPixels * screenWidthPixels + screenHeightPixels * screenHeightPixels).toDouble()
        )
        val diagonalInches = diagonalPixels / displayMetrics.densityDpi
        
        // 判断是否为Pixel 4
        val isPixel4 = kotlin.math.abs(diagonalInches - 5.7) < 0.1 && 
                      kotlin.math.abs(density - 2.75f) < 0.1f &&
                      screenWidthPixels == 1080 && screenHeightPixels == 2280
        
        // 判断是否为其他5.7英寸屏幕
        val isSmallScreen = diagonalInches <= 6.0 && density >= 2.5f && !isPixel4
        
        // 判断是否为大屏设备（暂时禁用，让Pixel 9 Pro XL使用标准配置）
        val isLargeScreen = false  // diagonalInches >= 6.5 && density >= 2.5f
        
        val (width, height) = when {
            isPixel4 -> {
                // Pixel 4：大尺寸
                Pair(1120, 560)
            }
            isLargeScreen -> {
                // 大屏设备：高度300px，保持宽高比2.29:1
                Pair(687, 300)
            }
            isSmallScreen -> {
                // 其他小屏幕：小尺寸
                Pair(560, 240)
            }
            else -> {
                // 标准屏幕：标准尺寸
                Pair(640, 280)
            }
        }
        
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // 绘制白色背景
        canvas.drawColor(Color.WHITE)
        
        val paint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#E0E0E0")
            style = Paint.Style.STROKE
            strokeWidth = when {
                isPixel4 -> 3.5f
                isLargeScreen -> 2.5f
                isSmallScreen -> 1.8f
                else -> 2f
            }
        }
        
        // 绘制简单的坐标轴
        val leftPadding = when {
            isPixel4 -> 70f
            isLargeScreen -> 43f  // 按比例调整：50f * (687/800) ≈ 43f
            isSmallScreen -> 35f
            else -> 40f
        }
        val bottomPadding = when {
            isPixel4 -> 60f
            isLargeScreen -> 30f  // 适应新的300px高度
            isSmallScreen -> 30f
            else -> 35f
        }
        val padding = when {
            isPixel4 -> 35f
            isLargeScreen -> 25f
            isSmallScreen -> 18f
            else -> 20f
        }
        val chartLeft = leftPadding
        val chartTop = padding
        val chartRight = width - padding
        val chartBottom = height - bottomPadding
        
        // Y轴
        canvas.drawLine(chartLeft, chartTop, chartLeft, chartBottom, paint)
        // X轴
        canvas.drawLine(chartLeft, chartBottom, chartRight, chartBottom, paint)
        
        // 显示"暂无数据"/"No Data"
        val textPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#999999")
            textSize = when {
                isPixel4 -> 24f
                isLargeScreen -> 18f
                isSmallScreen -> 12f
                else -> 14f
            }
            textAlign = Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT
        }
        canvas.drawText(context.getString(R.string.no_data), width / 2f, height / 2f, textPaint)
        
        return bitmap
    }
} 