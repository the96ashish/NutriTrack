package com.example.myapplication.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.myapplication.ActivityLevel
import com.example.myapplication.DateUtil
import com.example.myapplication.FitnessGoal
import com.example.myapplication.Gender
import com.example.myapplication.LocalStore
import com.example.myapplication.MainActivity
import com.example.myapplication.NutritionEngine
import com.example.myapplication.R
import com.example.myapplication.UserProfile
import kotlin.math.roundToInt

const val ACTION_ADD_WATER = "com.example.myapplication.ACTION_ADD_WATER"

// ============================================================================
// WIDGET UPDATE ENGINE
// ============================================================================

fun updateAllWidgets(context: Context) {
    val store = LocalStore(context)
    val todayKey = DateUtil.todayKey()
    val dailyLog = store.loadDailyLog(todayKey)
    val profile = store.loadProfile() ?: UserProfile(
        name = "Athlete",
        age = 28,
        gender = Gender.MALE,
        heightCm = 172.0,
        weightKg = 70.0,
        activityLevel = ActivityLevel.MODERATE,
        goal = FitnessGoal.MAINTAIN
    )
    val targets = NutritionEngine.calculateTargets(profile)

    val consumedCals = dailyLog.entries.sumOf { it.calories }.roundToInt()
    val consumedProtein = NutritionEngine.round1(dailyLog.entries.sumOf { it.protein })
    val consumedCarbs = NutritionEngine.round1(dailyLog.entries.sumOf { it.carbs })
    val consumedFat = NutritionEngine.round1(dailyLog.entries.sumOf { it.fat })
    val waterMl = dailyLog.waterIntakeMl
    val targetCals = targets.calories
    val targetWater = targets.waterMl

    val manager = AppWidgetManager.getInstance(context)

    // Open App Intent
    val appIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendingAppIntent = PendingIntent.getActivity(
        context,
        0,
        appIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // Add Water Intent
    val waterIntent = Intent(context, HydrationTrackerWidget::class.java).apply {
        action = ACTION_ADD_WATER
    }
    val pendingWaterIntent = PendingIntent.getBroadcast(
        context,
        0,
        waterIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // 1. Calorie Summary Widget (2x2)
    val calSummaryViews = RemoteViews(context.packageName, R.layout.widget_calorie_summary).apply {
        setTextViewText(R.id.widget_cal_consumed, "$consumedCals kcal")
        setTextViewText(R.id.widget_cal_target, "Goal: $targetCals kcal")
        val diff = targetCals - consumedCals
        if (diff >= 0) {
            setTextViewText(R.id.widget_cal_status, "$diff kcal left • On Track")
        } else {
            setTextViewText(R.id.widget_cal_status, "+${-diff} kcal over goal")
        }
        setOnClickPendingIntent(R.id.widget_container, pendingAppIntent)
    }
    val calComponent = ComponentName(context, CalorieSummaryWidget::class.java)
    manager.updateAppWidget(calComponent, calSummaryViews)

    // 2. Macro Dashboard Widget (4x2)
    val macroViews = RemoteViews(context.packageName, R.layout.widget_macro_dashboard).apply {
        setTextViewText(R.id.widget_macro_cal, "$consumedCals kcal")
        setTextViewText(R.id.widget_macro_pro, "${consumedProtein}g")
        setTextViewText(R.id.widget_macro_carb, "${consumedCarbs}g")
        setTextViewText(R.id.widget_macro_fat, "${consumedFat}g")
        setTextViewText(R.id.widget_water_text, "💧 Water: $waterMl / $targetWater ml")
        setOnClickPendingIntent(R.id.widget_container, pendingAppIntent)
        setOnClickPendingIntent(R.id.widget_btn_log, pendingAppIntent)
    }
    val macroComponent = ComponentName(context, MacroDashboardWidget::class.java)
    manager.updateAppWidget(macroComponent, macroViews)

    // 3. Hydration Tracker Widget (2x1)
    val waterViews = RemoteViews(context.packageName, R.layout.widget_hydration_tracker).apply {
        setTextViewText(R.id.widget_water_val, "$waterMl / $targetWater ml")
        setOnClickPendingIntent(R.id.widget_container, pendingAppIntent)
        setOnClickPendingIntent(R.id.widget_btn_add_water, pendingWaterIntent)
    }
    val waterComponent = ComponentName(context, HydrationTrackerWidget::class.java)
    manager.updateAppWidget(waterComponent, waterViews)

    // 4. Quick Meal Bar Widget (4x1)
    val mealBarViews = RemoteViews(context.packageName, R.layout.widget_quick_meal_bar).apply {
        setOnClickPendingIntent(R.id.widget_container, pendingAppIntent)
        setOnClickPendingIntent(R.id.btn_log_breakfast, pendingAppIntent)
        setOnClickPendingIntent(R.id.btn_log_lunch, pendingAppIntent)
        setOnClickPendingIntent(R.id.btn_log_snack, pendingAppIntent)
        setOnClickPendingIntent(R.id.btn_log_dinner, pendingAppIntent)
    }
    val mealComponent = ComponentName(context, QuickMealBarWidget::class.java)
    manager.updateAppWidget(mealComponent, mealBarViews)

    // 5. Combo Bar Widget (3x1)
    val comboViews = RemoteViews(context.packageName, R.layout.widget_combo_bar).apply {
        setTextViewText(R.id.widget_combo_cal, "$consumedCals / $targetCals kcal")
        setTextViewText(R.id.widget_combo_water, "$waterMl / $targetWater ml")
        setOnClickPendingIntent(R.id.widget_container, pendingAppIntent)
        setOnClickPendingIntent(R.id.widget_btn_combo_water, pendingWaterIntent)
    }
    val comboComponent = ComponentName(context, ComboBarWidget::class.java)
    manager.updateAppWidget(comboComponent, comboViews)
}

// ============================================================================
// APP WIDGET PROVIDER CLASSES
// ============================================================================

class CalorieSummaryWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateAllWidgets(context)
    }
}

class MacroDashboardWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateAllWidgets(context)
    }
}

class HydrationTrackerWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateAllWidgets(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_ADD_WATER) {
            val store = LocalStore(context)
            val todayKey = DateUtil.todayKey()
            val dailyLog = store.loadDailyLog(todayKey)
            val updatedLog = dailyLog.copy(waterIntakeMl = dailyLog.waterIntakeMl + 250)
            store.saveDailyLog(updatedLog)
            updateAllWidgets(context)
        }
    }
}

class QuickMealBarWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateAllWidgets(context)
    }
}

class ComboBarWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateAllWidgets(context)
    }
}
