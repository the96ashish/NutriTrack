package com.example.myapplication

import android.content.Context
import kotlinx.coroutines.delay
import android.os.Bundle
import androidx.core.content.edit
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

// ============================================================================
// 1. COLOR PALETTE
// ============================================================================

private val Slate50 = Color(0xFFF8FAFC)
private val Slate100 = Color(0xFFF1F5F9)
private val Slate200 = Color(0xFFE2E8F0)
private val Slate400 = Color(0xFF94A3B8)
private val Slate500 = Color(0xFF64748B)
private val Slate700 = Color(0xFF334155)
private val Slate900 = Color(0xFF0F172A)

private val Emerald50 = Color(0xFFECFDF5)
private val Emerald600 = Color(0xFF059669)
private val Emerald700 = Color(0xFF047857)

private val Blue50 = Color(0xFFEFF6FF)
private val Blue500 = Color(0xFF3B82F6)
private val Blue700 = Color(0xFF1D4ED8)

private val Amber50 = Color(0xFFFFFBEB)
private val Amber500 = Color(0xFFF59E0B)
private val Amber700 = Color(0xFFB45309)

private val Rose50 = Color(0xFFFFF1F2)
private val Rose500 = Color(0xFFF43F5E)
private val Rose700 = Color(0xFFBE123C)

private val Cyan50 = Color(0xFFECFEFF)
private val Cyan500 = Color(0xFF06B6D4)
private val Cyan700 = Color(0xFF0E7490)

// ============================================================================
// 2. DATA MODELS & ENUMS
// ============================================================================

enum class Gender(val label: String) {
    MALE("Male"),
    FEMALE("Female")
}

enum class ActivityLevel(val label: String, val desc: String, val multiplier: Double) {
    SEDENTARY("Sedentary", "Desk job, minimal exercise", 1.2),
    LIGHT("Lightly Active", "Light exercise 1–3 days/wk", 1.375),
    MODERATE("Moderately Active", "Workouts 3–5 days/wk", 1.55),
    VERY("Very Active", "Hard training 6–7 days/wk", 1.725),
    EXTRA("Extra Active", "Intense daily training", 1.9)
}

enum class FitnessGoal(val label: String, val badge: String, val proteinPerKg: Double) {
    LOSE("Lose Fat", "-500 kcal", 1.8),
    MAINTAIN("Maintain", "TDEE Balance", 1.6),
    BUILD("Build Muscle", "+300 kcal", 2.0)
}

enum class MealType(val label: String, val emoji: String) {
    BREAKFAST("Breakfast", "☀️"),
    LUNCH("Lunch", "🍛"),
    DINNER("Dinner", "🌙"),
    SNACKS("Snacks & Drinks", "☕")
}

data class UserProfile(
    val name: String,
    val age: Int,
    val gender: Gender,
    val heightCm: Double,
    val weightKg: Double,
    val activityLevel: ActivityLevel,
    val goal: FitnessGoal,
    val targetWeightKg: Double? = null
)

data class FoodItem(
    val id: String,
    val name: String,
    val category: String,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val unitType: String,
    val servingLabel: String,
    val servingGramWeight: Double,
    val isCustom: Boolean = false
)

data class LoggedEntry(
    val id: String,
    val foodId: String,
    val foodName: String,
    val mealType: MealType,
    val inputMode: String, // "serving" or "weight"
    val quantity: Double,
    val totalGrams: Double,
    val servingLabel: String,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double
)

data class DailyLog(
    val date: String,
    val entries: List<LoggedEntry>,
    val waterIntakeMl: Int
)

data class MacroTargets(
    val bmr: Int,
    val tdee: Int,
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int,
    val waterMl: Int
)

// ============================================================================
// 3. MIFFLIN-ST JEOR CALCULATION ENGINE
// ============================================================================

object NutritionEngine {
    fun calculateBMI(weightKg: Double, heightCm: Double): Pair<Double, String> {
        if (weightKg <= 0.0 || heightCm <= 0.0) return 0.0 to "Normal"
        val heightM = heightCm / 100.0
        val raw = weightKg / heightM.pow(2.0)
        val rounded = (raw * 10.0).roundToInt() / 10.0
        val category = when {
            rounded < 18.5 -> "Underweight"
            rounded <= 24.9 -> "Normal"
            rounded <= 29.9 -> "Overweight"
            else -> "Obese"
        }
        return rounded to category
    }

    fun calculateBMR(profile: UserProfile): Double {
        val base = (10.0 * profile.weightKg) + (6.25 * profile.heightCm) - (5.0 * profile.age)
        return if (profile.gender == Gender.MALE) base + 5.0 else base - 161.0
    }

    fun calculateTDEE(profile: UserProfile): Double {
        return calculateBMR(profile) * profile.activityLevel.multiplier
    }

    fun calculateTargets(profile: UserProfile): MacroTargets {
        val bmr = calculateBMR(profile)
        val tdee = calculateTDEE(profile)
        val calorieTarget = when (profile.goal) {
            FitnessGoal.LOSE -> max(1200.0, tdee - 500.0)
            FitnessGoal.MAINTAIN -> tdee
            FitnessGoal.BUILD -> tdee + 300.0
        }
        val proteinG = profile.weightKg * profile.goal.proteinPerKg
        val fatG = (calorieTarget * 0.25) / 9.0
        val carbsG = max(0.0, (calorieTarget - (proteinG * 4.0) - (fatG * 9.0)) / 4.0)
        val waterMl = (profile.weightKg * 35.0).roundToInt()

        return MacroTargets(
            bmr = bmr.roundToInt(),
            tdee = tdee.roundToInt(),
            calories = calorieTarget.roundToInt(),
            protein = proteinG.roundToInt(),
            carbs = carbsG.roundToInt(),
            fat = fatG.roundToInt(),
            waterMl = waterMl
        )
    }

    fun buildEntry(
        food: FoodItem,
        mealType: MealType,
        inputMode: String,
        quantity: Double,
        existingId: String? = null
    ): LoggedEntry {
        val safeQty = if (quantity > 0.0) quantity else 1.0
        val totalGrams = if (inputMode == "weight") safeQty else safeQty * food.servingGramWeight
        val factor = totalGrams / food.servingGramWeight
        val cleanQty = if (safeQty % 1.0 == 0.0) safeQty.toInt().toString() else safeQty.toString()
        val label = if (inputMode == "weight") {
            "$cleanQty${food.unitType}"
        } else {
            "$cleanQty × ${food.servingLabel}"
        }
        return LoggedEntry(
            id = existingId ?: UUID.randomUUID().toString(),
            foodId = food.id,
            foodName = food.name,
            mealType = mealType,
            inputMode = inputMode,
            quantity = safeQty,
            totalGrams = totalGrams,
            servingLabel = label,
            calories = food.calories * factor,
            protein = food.protein * factor,
            carbs = food.carbs * factor,
            fat = food.fat * factor
        )
    }

    fun round1(value: Double): Double = (value * 10.0).roundToInt() / 10.0
}

// ============================================================================
// 4. FOOD CATALOGUE (legacy values per serving; sourced additions below)
// ============================================================================

val DEFAULT_FOOD_DATABASE: List<FoodItem> = listOf(
    // --- GRAINS & BREADS ---
    FoodItem("roti_chapati", "Roti / Chapati", "Grains & Breads", 104.0, 3.1, 18.5, 2.0, "g", "1 medium roti", 35.0),
    FoodItem("butter_roti", "Butter Roti / Tandoori Roti", "Grains & Breads", 130.0, 3.5, 19.0, 4.5, "g", "1 roti", 40.0),
    FoodItem("rumali_roti", "Rumali Roti", "Grains & Breads", 145.0, 4.0, 28.0, 2.0, "g", "1 roti", 50.0),
    FoodItem("plain_paratha", "Plain Paratha", "Grains & Breads", 220.0, 4.2, 28.0, 10.0, "g", "1 paratha", 60.0),
    FoodItem("aloo_paratha", "Aloo Paratha", "Grains & Breads", 290.0, 5.5, 38.0, 13.0, "g", "1 paratha", 120.0),
    FoodItem("paneer_paratha", "Paneer Paratha", "Grains & Breads", 330.0, 11.0, 34.0, 16.0, "g", "1 paratha", 120.0),
    FoodItem("gobhi_paratha", "Gobhi Paratha", "Grains & Breads", 250.0, 5.0, 35.0, 10.0, "g", "1 paratha", 120.0),
    FoodItem("pyaz_paratha", "Pyaz Paratha", "Grains & Breads", 260.0, 5.2, 36.0, 11.0, "g", "1 paratha", 120.0),
    FoodItem("puri", "Puri", "Grains & Breads", 125.0, 2.0, 13.0, 7.5, "g", "1 medium puri", 30.0),
    FoodItem("bhatura", "Bhatura", "Grains & Breads", 240.0, 5.0, 32.0, 10.5, "g", "1 bhatura", 80.0),
    FoodItem("steamed_basmati_rice", "Steamed Basmati Rice", "Grains & Breads", 195.0, 3.8, 43.0, 0.5, "g", "1 katori cooked", 150.0),
    FoodItem("brown_rice", "Brown Rice", "Grains & Breads", 170.0, 3.8, 36.0, 1.4, "g", "1 katori cooked", 150.0),
    FoodItem("jeera_rice", "Jeera Rice", "Grains & Breads", 210.0, 3.8, 42.0, 3.5, "g", "1 katori", 150.0),
    FoodItem("veg_pulao", "Veg Pulao", "Grains & Breads", 260.0, 5.0, 46.0, 6.5, "g", "1 plate", 200.0),
    FoodItem("matar_pulao", "Matar Pulao", "Grains & Breads", 270.0, 6.0, 47.0, 6.5, "g", "1 plate", 200.0),
    FoodItem("plain_naan", "Plain Naan", "Grains & Breads", 260.0, 7.5, 45.0, 5.0, "g", "1 naan", 90.0),
    FoodItem("butter_naan", "Butter Naan", "Grains & Breads", 310.0, 7.8, 46.0, 10.5, "g", "1 naan", 100.0),
    FoodItem("garlic_naan", "Garlic Naan", "Grains & Breads", 320.0, 8.0, 46.0, 11.0, "g", "1 naan", 100.0),
    FoodItem("cheese_naan", "Cheese Naan", "Grains & Breads", 370.0, 11.0, 46.0, 16.0, "g", "1 naan", 110.0),
    FoodItem("missi_roti", "Missi Roti", "Grains & Breads", 150.0, 5.5, 22.0, 4.5, "g", "1 roti", 50.0),
    FoodItem("bajra_roti", "Bajra Roti", "Grains & Breads", 140.0, 3.8, 25.0, 2.5, "g", "1 roti", 50.0),
    FoodItem("jowar_roti", "Jowar Roti", "Grains & Breads", 135.0, 3.5, 26.0, 1.8, "g", "1 roti", 50.0),
    FoodItem("akki_roti", "Akki Roti", "Grains & Breads", 160.0, 3.0, 28.0, 4.0, "g", "1 roti", 60.0),
    FoodItem("whole_wheat_bread", "Whole Wheat Bread", "Grains & Breads", 130.0, 5.0, 24.0, 1.5, "g", "2 slices", 50.0),
    FoodItem("white_bread", "White Bread", "Grains & Breads", 135.0, 4.0, 25.0, 1.8, "g", "2 slices", 50.0),
    FoodItem("rolled_oats", "Rolled Oats (Cooked)", "Grains & Breads", 150.0, 5.0, 27.0, 2.5, "g", "1 bowl", 200.0),
    FoodItem("dalia_porridge", "Dalia Porridge", "Grains & Breads", 160.0, 5.5, 30.0, 2.0, "g", "1 bowl", 200.0),
    FoodItem("vermicelli_upma", "Vermicelli Upma / Sevaiyya", "Grains & Breads", 210.0, 4.5, 38.0, 5.0, "g", "1 bowl", 180.0),

    // --- PROTEINS & DALS ---
    FoodItem("yellow_dal_tadka", "Yellow Dal Tadka", "Dal & Curries", 150.0, 7.5, 20.0, 4.5, "g", "1 katori", 150.0),
    FoodItem("dal_fry", "Dal Fry", "Dal & Curries", 165.0, 7.5, 20.0, 6.0, "g", "1 katori", 150.0),
    FoodItem("dal_makhani", "Dal Makhani", "Dal & Curries", 230.0, 8.5, 22.0, 12.0, "g", "1 katori", 150.0),
    FoodItem("dal_panchmel", "Dal Panchmel", "Dal & Curries", 170.0, 8.0, 21.0, 6.0, "g", "1 katori", 150.0),
    FoodItem("rajma_masala", "Rajma Masala", "Dal & Curries", 210.0, 9.5, 30.0, 5.5, "g", "1 katori", 160.0),
    FoodItem("chole_masala", "Chole / Chana Masala", "Dal & Curries", 220.0, 10.0, 32.0, 6.0, "g", "1 katori", 160.0),
    FoodItem("moong_dal_curry", "Moong Dal Curry", "Dal & Curries", 135.0, 8.0, 19.0, 3.0, "g", "1 katori", 150.0),
    FoodItem("masoor_dal_curry", "Masoor Dal Curry", "Dal & Curries", 140.0, 8.5, 20.0, 3.0, "g", "1 katori", 150.0),
    FoodItem("chana_dal_curry", "Chana Dal Curry", "Dal & Curries", 160.0, 8.5, 22.0, 4.0, "g", "1 katori", 150.0),
    FoodItem("kadhi_pakora", "Kadhi Pakora", "Dal & Curries", 210.0, 6.0, 20.0, 11.0, "g", "1 katori", 180.0),
    FoodItem("sambhar", "Sambhar", "Dal & Curries", 130.0, 5.0, 20.0, 3.5, "g", "1 bowl", 200.0),
    FoodItem("rasam", "Rasam", "Dal & Curries", 60.0, 1.5, 9.0, 2.0, "g", "1 bowl", 180.0),
    FoodItem("soya_bean_curry", "Soya Bean Curry / Chunks", "Dal & Curries", 180.0, 20.0, 12.0, 5.0, "g", "1 katori cooked", 150.0),
    FoodItem("sprouted_moong", "Sprouted Moong Salad", "Dal & Curries", 80.0, 7.0, 12.0, 0.5, "g", "1 bowl", 100.0),
    FoodItem("sprouted_chana", "Sprouted Chana Salad", "Dal & Curries", 110.0, 7.5, 16.0, 1.5, "g", "1 bowl", 100.0),
    FoodItem("boiled_egg", "Boiled Egg", "Dal & Curries", 72.0, 6.3, 0.4, 4.8, "g", "1 large egg", 50.0),
    FoodItem("sunny_side_up", "Sunny Side Up / Half Fry", "Dal & Curries", 95.0, 6.3, 0.4, 7.5, "g", "1 egg", 60.0),
    FoodItem("egg_omelette", "Egg Omelette", "Dal & Curries", 185.0, 13.0, 2.5, 14.0, "g", "2 eggs", 120.0),
    FoodItem("masala_cheese_omelette", "Masala Cheese Omelette", "Dal & Curries", 260.0, 17.0, 3.5, 20.0, "g", "2 eggs", 140.0),
    FoodItem("egg_bhurji", "Egg Bhurji", "Dal & Curries", 195.0, 13.5, 3.0, 14.5, "g", "2 eggs", 120.0),
    FoodItem("egg_curry", "Egg Curry", "Dal & Curries", 250.0, 15.0, 8.0, 17.0, "g", "2 eggs in gravy", 200.0),
    FoodItem("grilled_chicken_breast", "Grilled Chicken Breast", "Dal & Curries", 198.0, 37.0, 0.0, 4.3, "g", "1 cooked fillet", 120.0),
    FoodItem("chicken_curry", "Chicken Curry (Home-style)", "Dal & Curries", 280.0, 24.0, 8.0, 16.0, "g", "1 bowl", 200.0),
    FoodItem("butter_chicken", "Butter Chicken", "Dal & Curries", 380.0, 22.0, 10.0, 28.0, "g", "1 bowl", 200.0),
    FoodItem("kadai_chicken", "Kadai Chicken", "Dal & Curries", 310.0, 25.0, 8.0, 20.0, "g", "1 bowl", 200.0),
    FoodItem("chicken_tikka", "Chicken Tikka", "Dal & Curries", 230.0, 28.0, 4.0, 11.0, "g", "6 pieces", 150.0),
    FoodItem("tandoori_chicken", "Tandoori Chicken", "Dal & Curries", 260.0, 30.0, 3.0, 14.0, "g", "1 leg piece", 150.0),
    FoodItem("chicken_korma", "Chicken Korma", "Dal & Curries", 360.0, 23.0, 11.0, 25.0, "g", "1 bowl", 200.0),
    FoodItem("chicken_biryani", "Chicken Biryani", "Dal & Curries", 480.0, 28.0, 55.0, 16.0, "g", "1 plate", 350.0),
    FoodItem("mutton_curry", "Mutton Curry", "Dal & Curries", 340.0, 22.0, 6.0, 25.0, "g", "1 bowl", 200.0),
    FoodItem("mutton_rogan_josh", "Mutton Rogan Josh", "Dal & Curries", 370.0, 23.0, 7.0, 28.0, "g", "1 bowl", 200.0),
    FoodItem("mutton_biryani", "Mutton Biryani", "Dal & Curries", 550.0, 26.0, 58.0, 22.0, "g", "1 plate", 350.0),
    FoodItem("fish_curry", "Fish Curry (Rohu / Katla)", "Dal & Curries", 210.0, 22.0, 5.0, 11.0, "g", "1 bowl", 200.0),
    FoodItem("fish_fry", "Fish Fry", "Dal & Curries", 240.0, 24.0, 6.0, 13.5, "g", "1 fillet", 120.0),
    FoodItem("prawn_curry", "Prawn Curry", "Dal & Curries", 220.0, 24.0, 6.0, 11.0, "g", "1 bowl", 180.0),
    FoodItem("paneer_tikka", "Paneer Tikka", "Dal & Curries", 270.0, 16.0, 7.0, 19.0, "g", "6 pieces", 150.0),
    FoodItem("palak_paneer", "Palak Paneer", "Dal & Curries", 240.0, 12.0, 8.0, 18.0, "g", "1 katori", 180.0),
    FoodItem("shahi_paneer", "Shahi Paneer", "Dal & Curries", 280.0, 11.0, 12.0, 21.0, "g", "1 katori", 180.0),
    FoodItem("paneer_butter_masala", "Paneer Butter Masala", "Dal & Curries", 310.0, 12.0, 11.0, 24.0, "g", "1 katori", 180.0),
    FoodItem("kadai_paneer", "Kadai Paneer", "Dal & Curries", 270.0, 13.0, 9.0, 20.0, "g", "1 katori", 180.0),
    FoodItem("matar_paneer", "Matar Paneer", "Dal & Curries", 230.0, 11.0, 14.0, 15.0, "g", "1 katori", 180.0),
    FoodItem("malai_kofta", "Malai Kofta", "Dal & Curries", 380.0, 9.0, 22.0, 28.0, "g", "1 bowl", 200.0),
    FoodItem("raw_paneer", "Raw Paneer Cubes", "Dairy", 265.0, 18.0, 3.0, 20.0, "g", "1 cup cubes", 100.0),
    FoodItem("tofu_stir_fry", "Tofu Stir Fry", "Dal & Curries", 140.0, 12.0, 5.0, 8.0, "g", "1 bowl", 150.0),
    FoodItem("whey_protein_scoop", "Whey Protein Scoop", "Dal & Curries", 120.0, 24.0, 2.0, 1.5, "g", "1 scoop", 33.0),

    // --- BREAKFAST & TIFFIN ---
    FoodItem("masala_dosa", "Masala Dosa", "Meals & Snacks", 320.0, 7.0, 48.0, 11.0, "g", "1 dosa with sambar", 200.0),
    FoodItem("plain_dosa", "Plain Dosa", "Meals & Snacks", 180.0, 4.5, 32.0, 4.0, "g", "1 dosa", 120.0),
    FoodItem("rava_dosa", "Rava Dosa", "Meals & Snacks", 210.0, 4.0, 34.0, 6.5, "g", "1 dosa", 130.0),
    FoodItem("set_dosa", "Set Dosa / Sponge Dosa", "Meals & Snacks", 240.0, 5.5, 42.0, 5.0, "g", "2 dosas", 160.0),
    FoodItem("idli_sambar", "Idli with Sambar", "Meals & Snacks", 180.0, 6.0, 35.0, 1.5, "g", "2 idlis + sambar", 180.0),
    FoodItem("medu_vada", "Medu Vada", "Meals & Snacks", 270.0, 6.5, 28.0, 15.0, "g", "2 vadas", 100.0),
    FoodItem("uttapam", "Uttapam (Onion / Tomato)", "Meals & Snacks", 240.0, 5.5, 40.0, 6.5, "g", "1 uttapam", 150.0),
    FoodItem("ven_pongal", "Ven Pongal", "Meals & Snacks", 290.0, 6.5, 42.0, 11.0, "g", "1 bowl", 200.0),
    FoodItem("poha", "Poha", "Meals & Snacks", 220.0, 4.0, 36.0, 6.5, "g", "1 plate", 160.0),
    FoodItem("upma", "Upma", "Meals & Snacks", 210.0, 4.5, 32.0, 7.0, "g", "1 bowl", 160.0),
    FoodItem("besan_cheela", "Besan Cheela", "Meals & Snacks", 220.0, 10.0, 28.0, 7.0, "g", "2 cheelas", 120.0),
    FoodItem("moong_dal_cheela", "Moong Dal Cheela", "Meals & Snacks", 210.0, 12.0, 26.0, 6.0, "g", "2 cheelas", 120.0),
    FoodItem("bread_omelette", "Bread Omelette", "Meals & Snacks", 250.0, 11.0, 26.0, 11.0, "g", "2 slices + 1 egg", 140.0),
    FoodItem("poori_bhaji", "Poori Bhaji", "Meals & Snacks", 450.0, 8.0, 55.0, 22.0, "g", "3 pooris + aloo bhaji", 250.0),
    FoodItem("puttu_kadala", "Puttu with Kadala Curry", "Meals & Snacks", 350.0, 9.0, 58.0, 9.0, "g", "1 serving", 220.0),
    FoodItem("appam_stew", "Appam with Veg Stew", "Meals & Snacks", 280.0, 5.0, 45.0, 9.0, "g", "2 appams + stew", 220.0),
    FoodItem("french_toast", "French Toast", "Meals & Snacks", 260.0, 8.0, 32.0, 11.0, "g", "2 slices", 120.0),
    FoodItem("veg_cheese_toast", "Veg Cheese Toast", "Meals & Snacks", 260.0, 8.0, 30.0, 12.0, "g", "1 sandwich", 150.0),
    FoodItem("corn_cheese_sandwich", "Corn Cheese Grilled Sandwich", "Meals & Snacks", 290.0, 9.0, 34.0, 13.5, "g", "1 sandwich", 160.0),

    // --- COOKED VEGETABLES & SABZI ---
    FoodItem("aloo_gobhi", "Aloo Gobhi Sabzi", "Meals & Snacks", 130.0, 3.0, 16.0, 6.0, "g", "1 katori", 150.0),
    FoodItem("aloo_matar", "Aloo Matar Sabzi", "Meals & Snacks", 140.0, 3.5, 20.0, 5.5, "g", "1 katori", 150.0),
    FoodItem("aloo_palak", "Aloo Palak Sabzi", "Meals & Snacks", 120.0, 3.2, 14.0, 5.8, "g", "1 katori", 150.0),
    FoodItem("aloo_methi", "Aloo Methi Sabzi", "Meals & Snacks", 135.0, 3.2, 15.0, 6.5, "g", "1 katori", 150.0),
    FoodItem("bhindi_masala", "Bhindi Masala", "Meals & Snacks", 120.0, 2.5, 11.0, 7.5, "g", "1 katori", 150.0),
    FoodItem("baingan_bharta", "Baingan Bharta", "Meals & Snacks", 110.0, 2.0, 12.0, 6.0, "g", "1 katori", 150.0),
    FoodItem("mixed_veg_sabzi", "Mixed Veg Sabzi", "Meals & Snacks", 125.0, 3.0, 14.0, 6.5, "g", "1 katori", 150.0),
    FoodItem("mushroom_masala", "Mushroom Masala", "Meals & Snacks", 130.0, 4.5, 9.0, 8.0, "g", "1 katori", 150.0),
    FoodItem("lauki_sabzi", "Lauki / Bottle Gourd Sabzi", "Meals & Snacks", 70.0, 1.5, 8.0, 3.5, "g", "1 katori", 150.0),
    FoodItem("torai_sabzi", "Torai / Ridge Gourd Sabzi", "Meals & Snacks", 65.0, 1.2, 7.0, 3.5, "g", "1 katori", 150.0),
    FoodItem("karela_fry", "Karela / Bitter Gourd Fry", "Meals & Snacks", 110.0, 2.0, 10.0, 7.0, "g", "1 katori", 120.0),
    FoodItem("dum_aloo", "Dum Aloo", "Meals & Snacks", 220.0, 3.5, 26.0, 11.0, "g", "1 katori", 180.0),
    FoodItem("sarson_ka_saag", "Sarson Ka Saag with Butter", "Meals & Snacks", 180.0, 4.5, 12.0, 13.0, "g", "1 katori", 180.0),
    FoodItem("methi_matar_malai", "Methi Matar Malai", "Meals & Snacks", 240.0, 5.0, 16.0, 17.0, "g", "1 katori", 180.0),
    FoodItem("arbi_fry", "Arbi Fry / Colocasia", "Meals & Snacks", 160.0, 2.0, 22.0, 7.0, "g", "1 katori", 120.0),
    FoodItem("cabbage_matar", "Cabbage Matar Sabzi", "Meals & Snacks", 95.0, 2.5, 12.0, 4.5, "g", "1 katori", 150.0),
    FoodItem("chana_palak", "Chana Palak", "Meals & Snacks", 170.0, 7.0, 22.0, 5.5, "g", "1 katori", 180.0),

    // --- STREET FOOD & CHAAT ---
    FoodItem("samosa", "Samosa", "Meals & Snacks", 250.0, 4.5, 30.0, 12.5, "g", "1 medium samosa", 90.0),
    FoodItem("kachori", "Kachori", "Meals & Snacks", 270.0, 4.0, 28.0, 16.0, "g", "1 kachori", 80.0),
    FoodItem("pav_bhaji", "Pav Bhaji", "Meals & Snacks", 400.0, 9.0, 52.0, 17.0, "g", "2 pav + bhaji", 250.0),
    FoodItem("vada_pav", "Vada Pav", "Meals & Snacks", 290.0, 6.0, 42.0, 11.0, "g", "1 vada pav", 120.0),
    FoodItem("chole_bhature", "Chole Bhature", "Meals & Snacks", 620.0, 16.0, 75.0, 28.0, "g", "2 bhature + chole", 350.0),
    FoodItem("pani_puri", "Pani Puri / Golgappa", "Meals & Snacks", 160.0, 2.5, 24.0, 6.0, "g", "6 pieces", 120.0),
    FoodItem("bhel_puri", "Bhel Puri", "Meals & Snacks", 220.0, 4.5, 38.0, 6.0, "g", "1 plate", 150.0),
    FoodItem("sev_puri", "Sev Puri", "Meals & Snacks", 260.0, 4.0, 34.0, 12.0, "g", "6 pieces", 140.0),
    FoodItem("dahi_puri", "Dahi Puri", "Meals & Snacks", 310.0, 6.0, 42.0, 13.0, "g", "6 pieces", 160.0),
    FoodItem("papdi_chaat", "Papdi Chaat", "Meals & Snacks", 320.0, 6.5, 42.0, 14.0, "g", "1 plate", 180.0),
    FoodItem("aloo_tikki_chaat", "Aloo Tikki Chaat", "Meals & Snacks", 280.0, 5.0, 38.0, 12.0, "g", "2 tikkis", 180.0),
    FoodItem("dahi_vada", "Dahi Vada / Bhalla", "Meals & Snacks", 260.0, 7.0, 32.0, 11.0, "g", "2 vadas", 180.0),
    FoodItem("raj_kachori", "Raj Kachori", "Meals & Snacks", 420.0, 8.0, 52.0, 20.0, "g", "1 large kachori", 200.0),
    FoodItem("samosa_chaat", "Samosa Chaat", "Meals & Snacks", 380.0, 8.0, 48.0, 17.0, "g", "1 samosa with chole", 220.0),
    FoodItem("peanut_chaat", "Peanut Chaat", "Meals & Snacks", 250.0, 10.0, 18.0, 15.0, "g", "1 bowl", 120.0),
    FoodItem("corn_chaat", "Corn Chaat", "Meals & Snacks", 160.0, 4.5, 30.0, 3.5, "g", "1 bowl", 120.0),
    FoodItem("veg_steamed_momos", "Veg Steamed Momos", "Meals & Snacks", 180.0, 5.0, 32.0, 3.5, "g", "6 pieces", 120.0),
    FoodItem("veg_fried_momos", "Veg Fried Momos", "Meals & Snacks", 260.0, 5.0, 32.0, 12.0, "g", "6 pieces", 130.0),
    FoodItem("chicken_steamed_momos", "Chicken Steamed Momos", "Meals & Snacks", 220.0, 11.0, 28.0, 6.5, "g", "6 pieces", 130.0),
    FoodItem("veg_chowmein", "Veg Chowmein / Noodles", "Meals & Snacks", 310.0, 6.0, 48.0, 11.0, "g", "1 plate", 200.0),
    FoodItem("veg_manchurian", "Veg Manchurian Gravy", "Meals & Snacks", 230.0, 4.0, 26.0, 12.0, "g", "1 bowl", 200.0),
    FoodItem("chilli_paneer", "Chilli Paneer", "Meals & Snacks", 320.0, 14.0, 18.0, 22.0, "g", "1 bowl", 180.0),
    FoodItem("chilli_chicken", "Chilli Chicken", "Meals & Snacks", 290.0, 22.0, 14.0, 16.0, "g", "1 bowl", 180.0),
    FoodItem("veg_spring_roll", "Veg Spring Roll", "Meals & Snacks", 240.0, 4.5, 30.0, 11.0, "g", "2 rolls", 120.0),
    FoodItem("egg_kathi_roll", "Egg Kathi Roll", "Meals & Snacks", 340.0, 13.0, 38.0, 15.0, "g", "1 roll", 180.0),
    FoodItem("paneer_kathi_roll", "Paneer Kathi Roll", "Meals & Snacks", 390.0, 15.0, 40.0, 19.0, "g", "1 roll", 200.0),
    FoodItem("chicken_kathi_roll", "Chicken Kathi Roll", "Meals & Snacks", 380.0, 22.0, 36.0, 16.0, "g", "1 roll", 200.0),
    FoodItem("sabudana_khichdi", "Sabudana Khichdi", "Meals & Snacks", 280.0, 3.0, 46.0, 9.0, "g", "1 bowl", 180.0),
    FoodItem("sabudana_vada", "Sabudana Vada", "Meals & Snacks", 310.0, 3.5, 42.0, 14.5, "g", "2 vadas", 120.0),
    FoodItem("maggi_noodles", "Maggi Instant Noodles", "Fast Food & Junk", 310.0, 7.0, 42.0, 12.5, "g", "1 pack prepared", 140.0),
    FoodItem("veg_burger", "Veg Burger", "Fast Food & Junk", 320.0, 8.0, 44.0, 12.0, "g", "1 burger", 160.0),
    FoodItem("chicken_burger", "Chicken Burger", "Fast Food & Junk", 360.0, 18.0, 40.0, 14.0, "g", "1 burger", 170.0),
    FoodItem("veg_pizza", "Veg Pizza Slice", "Fast Food & Junk", 220.0, 8.0, 28.0, 8.5, "g", "1 slice medium", 100.0),
    FoodItem("french_fries", "French Fries", "Fast Food & Junk", 340.0, 4.0, 44.0, 16.0, "g", "1 medium serve", 120.0),
    FoodItem("potato_chips", "Potato Chips", "Fast Food & Junk", 210.0, 2.5, 21.0, 13.0, "g", "1 small bag", 40.0),
    FoodItem("dhokla", "Dhokla", "Meals & Snacks", 120.0, 4.0, 18.0, 3.5, "g", "2 pieces", 80.0),
    FoodItem("khandvi", "Khandvi", "Meals & Snacks", 100.0, 3.5, 12.0, 4.0, "g", "4 pieces", 80.0),
    FoodItem("litti_chokha", "Litti Chokha", "Meals & Snacks", 340.0, 10.0, 52.0, 10.0, "g", "2 littis + chokha", 220.0),
    FoodItem("dal_khichdi", "Dal Khichdi with Ghee", "Meals & Snacks", 230.0, 7.5, 36.0, 6.0, "g", "1 bowl", 220.0),
    FoodItem("vegetable_biryani", "Vegetable Biryani", "Meals & Snacks", 340.0, 7.0, 54.0, 10.0, "g", "1 plate", 300.0),
    FoodItem("roasted_makhana", "Roasted Makhana", "Meals & Snacks", 110.0, 3.0, 20.0, 2.0, "g", "1 bowl", 30.0),

    // --- DAIRY & FATS ---
    FoodItem("toned_milk", "Toned Milk", "Dairy", 150.0, 7.8, 12.2, 7.5, "ml", "1 glass", 250.0),
    FoodItem("full_cream_milk", "Full Cream Milk", "Dairy", 210.0, 8.0, 12.0, 14.0, "ml", "1 glass", 250.0),
    FoodItem("skimmed_milk", "Skimmed Milk", "Dairy", 90.0, 8.5, 12.5, 0.5, "ml", "1 glass", 250.0),
    FoodItem("buffalo_milk", "Buffalo Milk", "Dairy", 240.0, 9.0, 12.5, 16.0, "ml", "1 glass", 250.0),
    FoodItem("plain_curd", "Plain Curd / Dahi", "Dairy", 90.0, 5.0, 7.0, 5.0, "g", "1 katori", 150.0),
    FoodItem("greek_yogurt", "Greek Yogurt", "Dairy", 130.0, 13.0, 6.0, 6.0, "g", "1 cup", 150.0),
    FoodItem("hung_curd", "Hung Curd", "Dairy", 120.0, 9.0, 5.0, 7.0, "g", "100g", 100.0),
    FoodItem("mishti_doi", "Mishti Doi", "Dairy", 180.0, 4.5, 28.0, 5.5, "g", "1 cup", 120.0),
    FoodItem("shrikhand", "Shrikhand", "Dairy", 260.0, 6.0, 36.0, 10.0, "g", "1 bowl", 100.0),
    FoodItem("sweet_lassi", "Sweet Lassi", "Dairy", 220.0, 6.0, 32.0, 7.5, "ml", "1 glass", 250.0),
    FoodItem("masala_chaas", "Masala Chaas / Buttermilk", "Beverages", 50.0, 2.5, 4.0, 2.2, "ml", "1 glass", 250.0),
    FoodItem("cheese_slice", "Amul Cheese Slice", "Dairy", 62.0, 4.0, 0.5, 5.0, "g", "1 slice", 20.0),
    FoodItem("cheese_cube", "Processed Cheese Cube", "Dairy", 80.0, 5.0, 0.6, 6.5, "g", "1 cube", 25.0),
    FoodItem("mozzarella_cheese", "Mozzarella Cheese", "Dairy", 85.0, 6.5, 1.0, 6.0, "g", "1 serving", 30.0),
    FoodItem("amul_butter", "Amul Butter", "Dairy", 100.0, 0.1, 0.0, 11.5, "g", "1 tbsp", 14.0),
    FoodItem("fresh_malai", "Fresh Malai / Cream", "Dairy", 50.0, 0.4, 0.6, 5.2, "g", "1 tbsp", 15.0),
    FoodItem("desi_ghee", "Desi Ghee", "Dairy", 45.0, 0.0, 0.0, 5.0, "g", "1 tsp", 5.0),
    FoodItem("peanut_butter", "Peanut Butter", "Fats & Nuts", 95.0, 4.0, 3.2, 8.0, "g", "1 tbsp", 16.0),
    FoodItem("almonds", "Almonds", "Fats & Nuts", 86.0, 3.2, 3.0, 7.5, "g", "12 nuts", 15.0),
    FoodItem("walnuts", "Walnuts", "Fats & Nuts", 98.0, 2.3, 2.0, 9.8, "g", "4 halves", 15.0),
    FoodItem("cashews", "Cashews", "Fats & Nuts", 83.0, 2.7, 4.5, 6.5, "g", "10 nuts", 15.0),
    FoodItem("pistachios", "Pistachios", "Fats & Nuts", 84.0, 3.0, 4.0, 6.7, "g", "20 nuts", 15.0),
    FoodItem("raisins", "Raisins / Kishmish", "Fats & Nuts", 45.0, 0.5, 11.5, 0.1, "g", "1 tbsp", 15.0),
    FoodItem("dried_dates", "Dried Dates / Khajoor", "Fats & Nuts", 55.0, 0.5, 14.0, 0.1, "g", "2 dates", 20.0),
    FoodItem("roasted_peanuts", "Roasted Peanuts", "Fats & Nuts", 170.0, 7.5, 5.0, 14.0, "g", "1 handful", 30.0),
    FoodItem("pumpkin_seeds", "Pumpkin Seeds", "Fats & Nuts", 85.0, 4.5, 2.0, 7.0, "g", "1 tbsp", 15.0),
    FoodItem("chia_seeds", "Chia Seeds", "Fats & Nuts", 60.0, 2.0, 5.0, 3.8, "g", "1 tbsp", 12.0),
    FoodItem("flax_seeds", "Flax Seeds", "Fats & Nuts", 65.0, 2.2, 3.5, 5.0, "g", "1 tbsp", 12.0),

    // --- FRUITS & VEGETABLES ---
    FoodItem("banana", "Banana", "Fruits & Veg", 105.0, 1.3, 27.0, 0.4, "g", "1 medium banana", 118.0),
    FoodItem("apple", "Apple", "Fruits & Veg", 95.0, 0.5, 25.0, 0.3, "g", "1 medium apple", 180.0),
    FoodItem("mango", "Mango", "Fruits & Veg", 99.0, 1.4, 24.7, 0.6, "g", "1 cup sliced", 165.0),
    FoodItem("orange", "Orange / Mosambi", "Fruits & Veg", 62.0, 1.2, 15.4, 0.2, "g", "1 medium orange", 140.0),
    FoodItem("papaya", "Papaya", "Fruits & Veg", 65.0, 0.8, 16.0, 0.4, "g", "1 bowl cubed", 150.0),
    FoodItem("guava", "Guava / Amrood", "Fruits & Veg", 68.0, 2.6, 14.0, 1.0, "g", "1 medium guava", 100.0),
    FoodItem("watermelon", "Watermelon", "Fruits & Veg", 60.0, 1.2, 15.0, 0.3, "g", "1 bowl cubed", 200.0),
    FoodItem("muskmelon", "Muskmelon / Kharbuja", "Fruits & Veg", 68.0, 1.6, 16.5, 0.4, "g", "1 bowl", 200.0),
    FoodItem("pomegranate", "Pomegranate", "Fruits & Veg", 83.0, 1.7, 18.7, 1.2, "g", "1 bowl seeds", 100.0),
    FoodItem("grapes", "Grapes", "Fruits & Veg", 104.0, 1.1, 27.0, 0.2, "g", "1 cup", 150.0),
    FoodItem("pineapple", "Pineapple", "Fruits & Veg", 75.0, 0.8, 19.5, 0.2, "g", "1 cup", 150.0),
    FoodItem("custard_apple", "Custard Apple / Sitaphal", "Fruits & Veg", 94.0, 2.1, 23.6, 0.3, "g", "1 medium", 100.0),
    FoodItem("strawberry", "Strawberry", "Fruits & Veg", 48.0, 1.0, 11.5, 0.4, "g", "1 cup", 150.0),
    FoodItem("kiwi", "Kiwi", "Fruits & Veg", 42.0, 0.8, 10.0, 0.4, "g", "1 medium", 75.0),
    FoodItem("chikoo", "Chikoo / Sapota", "Fruits & Veg", 66.0, 0.4, 16.0, 0.9, "g", "1 medium", 80.0),
    FoodItem("pear", "Pear / Nashpati", "Fruits & Veg", 85.0, 0.6, 23.0, 0.2, "g", "1 medium", 150.0),
    FoodItem("cucumber", "Cucumber", "Fruits & Veg", 24.0, 1.0, 5.5, 0.2, "g", "1 medium sliced", 150.0),
    FoodItem("tomato", "Tomato", "Fruits & Veg", 22.0, 1.1, 4.8, 0.2, "g", "1 medium tomato", 120.0),
    FoodItem("boiled_potato", "Boiled Potato", "Fruits & Veg", 130.0, 2.8, 30.0, 0.2, "g", "1 medium potato", 150.0),
    FoodItem("boiled_sweet_potato", "Boiled Sweet Potato", "Fruits & Veg", 112.0, 2.0, 26.0, 0.2, "g", "1 medium", 130.0),

    // --- SWEETS & DESSERTS ---
    FoodItem("gulab_jamun", "Gulab Jamun", "Sweets & Desserts", 150.0, 2.2, 24.0, 6.0, "g", "1 piece", 50.0),
    FoodItem("rasgulla", "Rasgulla", "Sweets & Desserts", 120.0, 2.5, 23.0, 1.5, "g", "1 piece", 50.0),
    FoodItem("jalebi", "Jalebi", "Sweets & Desserts", 160.0, 1.2, 32.0, 3.5, "g", "2 pieces", 50.0),
    FoodItem("kaju_katli", "Kaju Katli", "Sweets & Desserts", 135.0, 3.0, 15.0, 7.5, "g", "2 pieces", 30.0),
    FoodItem("rice_kheer", "Rice Kheer / Payasam", "Sweets & Desserts", 210.0, 5.0, 32.0, 7.0, "g", "1 bowl", 150.0),
    FoodItem("gajar_ka_halwa", "Gajar ka Halwa", "Sweets & Desserts", 260.0, 4.5, 34.0, 12.0, "g", "1 bowl", 120.0),
    FoodItem("moong_dal_halwa", "Moong Dal Halwa", "Sweets & Desserts", 310.0, 6.0, 38.0, 15.0, "g", "1 bowl", 120.0),
    FoodItem("besan_ladoo", "Besan Ladoo", "Sweets & Desserts", 180.0, 3.5, 22.0, 9.0, "g", "1 ladoo", 40.0),
    FoodItem("motichoor_ladoo", "Motichoor Ladoo", "Sweets & Desserts", 170.0, 2.0, 24.0, 7.5, "g", "1 ladoo", 40.0),
    FoodItem("rasmalai", "Rasmalai", "Sweets & Desserts", 180.0, 5.0, 22.0, 8.5, "g", "1 piece with rabri", 80.0),
    FoodItem("mysore_pak", "Mysore Pak", "Sweets & Desserts", 175.0, 1.5, 18.0, 11.0, "g", "1 piece", 35.0),
    FoodItem("ice_cream", "Ice Cream", "Sweets & Desserts", 200.0, 3.5, 24.0, 10.0, "g", "1 scoop", 100.0),
    FoodItem("dark_chocolate", "Dark Chocolate (70%)", "Sweets & Desserts", 120.0, 1.6, 9.0, 8.5, "g", "2 squares", 20.0),

    // --- BEVERAGES ---
    FoodItem("masala_chai", "Masala Chai (Milk & Sugar)", "Beverages", 75.0, 2.2, 11.0, 2.5, "ml", "1 teacup", 150.0),
    FoodItem("ginger_chai", "Ginger Tea / Adrak Chai (Milk & Sugar)", "Beverages", 75.0, 2.2, 11.0, 2.5, "ml", "1 teacup", 150.0),
    FoodItem("elaichi_chai", "Cardamom Tea / Elaichi Chai (Milk & Sugar)", "Beverages", 75.0, 2.2, 11.0, 2.5, "ml", "1 teacup", 150.0),
    FoodItem("lemon_tea", "Lemon Tea", "Beverages", 35.0, 0.2, 8.5, 0.0, "ml", "1 cup", 150.0),
    FoodItem("green_tea", "Green Tea (Unsweetened)", "Beverages", 2.0, 0.2, 0.3, 0.0, "ml", "1 cup", 150.0),
    FoodItem("filter_coffee", "Filter Coffee (with Milk)", "Beverages", 85.0, 2.5, 12.0, 3.0, "ml", "1 cup", 150.0),
    FoodItem("cold_coffee", "Cold Coffee with Ice Cream", "Beverages", 240.0, 5.5, 34.0, 9.0, "ml", "1 glass", 250.0),
    FoodItem("cappuccino", "Cappuccino / Latte", "Beverages", 120.0, 5.0, 11.0, 6.0, "ml", "1 cup", 200.0),
    FoodItem("black_coffee", "Black Coffee", "Beverages", 3.0, 0.3, 0.4, 0.0, "ml", "1 cup", 150.0),
    FoodItem("coconut_water", "Tender Coconut Water", "Beverages", 48.0, 1.8, 9.5, 0.5, "ml", "1 fresh coconut", 250.0),
    FoodItem("orange_juice", "Fresh Orange / Mosambi Juice", "Beverages", 110.0, 1.8, 26.0, 0.5, "ml", "1 glass", 250.0),
    FoodItem("sugarcane_juice", "Sugarcane Juice", "Beverages", 180.0, 0.5, 45.0, 0.2, "ml", "1 glass", 250.0),
    FoodItem("aam_panna", "Aam Panna", "Beverages", 120.0, 0.5, 30.0, 0.2, "ml", "1 glass", 250.0),
    FoodItem("jaljeera", "Jaljeera", "Beverages", 40.0, 0.5, 9.0, 0.2, "ml", "1 glass", 250.0),
    FoodItem("mango_milkshake", "Mango Milkshake", "Beverages", 230.0, 5.5, 38.0, 6.5, "ml", "1 glass", 250.0),
    FoodItem("chocolate_milkshake", "Chocolate Milkshake", "Beverages", 250.0, 6.5, 36.0, 8.5, "ml", "1 glass", 250.0),
    FoodItem("badam_milk", "Badam Milk", "Beverages", 190.0, 6.5, 22.0, 8.5, "ml", "1 glass", 200.0),
    FoodItem("cola_soda", "Cola / Soda", "Beverages", 130.0, 0.0, 33.0, 0.0, "ml", "1 can", 300.0),
    FoodItem("nimbu_pani", "Nimbu Pani / Lemonade", "Beverages", 70.0, 0.2, 18.0, 0.1, "ml", "1 glass", 250.0)
) + IndianFoodExpansion.foods
val ALL_CATEGORIES = listOf(
    "All",
    "Grains & Breads",
    "Dal & Curries",
    "Dairy",
    "Fruits & Veg",
    "Meals & Snacks",
    "Beverages",
    "Fats & Nuts",
    "Fast Food & Junk",
    "Sweets & Desserts"
)

// ============================================================================
// 5. PERSISTENT LOCAL STORAGE (SharedPreferences + Built-In JSON)
// ============================================================================

class LocalStore(val context: Context) {
    fun getRecentFoods(allFoods: List<FoodItem>): List<FoodItem> {
        val recentList = mutableListOf<FoodItem>()
        val seenIds = mutableSetOf<String>()
        val todayKey = DateUtil.todayKey()
        for (i in 0..7) {
            val dKey = DateUtil.shiftDay(todayKey, -i)
            val log = loadDailyLog(dKey)
            for ((_, foodId) in log.entries.reversed()) {
                if (seenIds.add(foodId)) {
                    val food = allFoods.find { it.id == foodId }
                    if (food != null) recentList.add(food)
                }
                if (recentList.size >= 8) break
            }
            if (recentList.size >= 8) break
        }
        return recentList
    }
    private val prefs = context.getSharedPreferences("nutritrack_store_v1", Context.MODE_PRIVATE)

    fun saveProfile(profile: UserProfile) {
        val obj = JSONObject().apply {
            put("name", profile.name)
            put("age", profile.age)
            put("gender", profile.gender.name)
            put("heightCm", profile.heightCm)
            put("weightKg", profile.weightKg)
            put("activityLevel", profile.activityLevel.name)
            put("goal", profile.goal.name)
            if (profile.targetWeightKg != null) {
                put("targetWeightKg", profile.targetWeightKg)
            }
        }
        prefs.edit { putString("user_profile", obj.toString()) }
        try { com.example.myapplication.widgets.updateAllWidgets(context) } catch (_: Exception) {}
    }

    fun loadProfile(): UserProfile? {
        val raw = prefs.getString("user_profile", null) ?: return null
        return try {
            val obj = JSONObject(raw)
            val targetWt = if (obj.has("targetWeightKg")) obj.optDouble("targetWeightKg") else null
            UserProfile(
                name = obj.optString("name", "User"),
                age = obj.optInt("age", 28),
                gender = Gender.valueOf(obj.optString("gender", Gender.MALE.name)),
                heightCm = obj.optDouble("heightCm", 172.0),
                weightKg = obj.optDouble("weightKg", 70.0),
                activityLevel = ActivityLevel.valueOf(obj.optString("activityLevel", ActivityLevel.MODERATE.name)),
                goal = FitnessGoal.valueOf(obj.optString("goal", FitnessGoal.MAINTAIN.name)),
                targetWeightKg = targetWt
            )
        } catch (_: Exception) {
            null
        }
    }

    fun saveCustomFoods(list: List<FoodItem>) {
        val arr = JSONArray()
        list.forEach { item ->
            arr.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("name", item.name)
                    put("category", item.category)
                    put("calories", item.calories)
                    put("protein", item.protein)
                    put("carbs", item.carbs)
                    put("fat", item.fat)
                    put("unitType", item.unitType)
                    put("servingLabel", item.servingLabel)
                    put("servingGramWeight", item.servingGramWeight)
                }
            )
        }
        prefs.edit { putString("custom_foods", arr.toString()) }
    }

    fun loadCustomFoods(): List<FoodItem> {
        val raw = prefs.getString("custom_foods", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val result = mutableListOf<FoodItem>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                result.add(
                    FoodItem(
                        id = o.getString("id"),
                        name = o.getString("name"),
                        category = o.getString("category"),
                        calories = o.getDouble("calories"),
                        protein = o.getDouble("protein"),
                        carbs = o.getDouble("carbs"),
                        fat = o.getDouble("fat"),
                        unitType = o.optString("unitType", "g"),
                        servingLabel = o.getString("servingLabel"),
                        servingGramWeight = o.getDouble("servingGramWeight"),
                        isCustom = true
                    )
                )
            }
            result
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveDailyLog(log: DailyLog) {
        val entriesArray = JSONArray()
        log.entries.forEach { e ->
            entriesArray.put(
                JSONObject().apply {
                    put("id", e.id)
                    put("foodId", e.foodId)
                    put("foodName", e.foodName)
                    put("mealType", e.mealType.name)
                    put("inputMode", e.inputMode)
                    put("quantity", e.quantity)
                    put("totalGrams", e.totalGrams)
                    put("servingLabel", e.servingLabel)
                    put("calories", e.calories)
                    put("protein", e.protein)
                    put("carbs", e.carbs)
                    put("fat", e.fat)
                }
            )
        }
        val root = JSONObject().apply {
            put("date", log.date)
            put("waterIntakeMl", log.waterIntakeMl)
            put("entries", entriesArray)
        }
        prefs.edit { putString("daily_log_${log.date}", root.toString()) }
        try { com.example.myapplication.widgets.updateAllWidgets(context) } catch (_: Exception) {}
    }

    fun loadDailyLog(dateKey: String): DailyLog {
        val raw = prefs.getString("daily_log_$dateKey", null)
            ?: return DailyLog(dateKey, emptyList(), 0)
        return try {
            val root = JSONObject(raw)
            val arr = root.getJSONArray("entries")
            val entries = mutableListOf<LoggedEntry>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                entries.add(
                    LoggedEntry(
                        id = o.getString("id"),
                        foodId = o.getString("foodId"),
                        foodName = o.getString("foodName"),
                        mealType = MealType.valueOf(o.getString("mealType")),
                        inputMode = o.getString("inputMode"),
                        quantity = o.getDouble("quantity"),
                        totalGrams = o.getDouble("totalGrams"),
                        servingLabel = o.getString("servingLabel"),
                        calories = o.getDouble("calories"),
                        protein = o.getDouble("protein"),
                        carbs = o.getDouble("carbs"),
                        fat = o.getDouble("fat")
                    )
                )
            }
            DailyLog(
                date = dateKey,
                entries = entries,
                waterIntakeMl = root.optInt("waterIntakeMl", 0)
            )
        } catch (_: Exception) {
            DailyLog(dateKey, emptyList(), 0)
        }
    }
}

// ============================================================================
// 6. DATE UTILITY
// ============================================================================

object DateUtil {
    fun shortDisplayLabel(key: String): String {
        return try {
            val d = keyFmt.parse(key) ?: return key
            shortFmt.format(d)
        } catch (_: Exception) {
            key
        }
    }
    private val keyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val fullFmt = SimpleDateFormat("EEE, MMM d, yyyy", Locale.US)
    private val shortFmt = SimpleDateFormat("MMM d", Locale.US)

    fun todayKey(): String = keyFmt.format(Date())

    fun shiftDay(dateKey: String, delta: Int): String {
        val cal = Calendar.getInstance()
        cal.time = keyFmt.parse(dateKey) ?: Date()
        cal.add(Calendar.DAY_OF_YEAR, delta)
        return keyFmt.format(cal.time)
    }

    fun displayLabel(dateKey: String): String {
        val parsed = keyFmt.parse(dateKey) ?: Date()
        return if (dateKey == todayKey()) {
            "Today, ${shortFmt.format(parsed)}"
        } else {
            fullFmt.format(parsed)
        }
    }
}

// ============================================================================
// 7. MAIN ACTIVITY ENTRY POINT
// ============================================================================

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Emerald600,
                    onPrimary = Color.White,
                    background = Slate50,
                    surface = Color.White,
                    onSurface = Slate900
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Slate50
                ) {
                    NutriTrackAppScreen()
                }
            }
        }
    }
}

// ============================================================================
// 7.5 APP SPLASH & BRAND LOADING SCREEN
// ============================================================================

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }
    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "SplashAlpha"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(1200.milliseconds)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Emerald600),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(24.dp)
                .graphicsLayer(alpha = alphaAnim)
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🥗",
                    fontSize = 48.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "NutriTrack",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Smart Macro & Nutrition Tracker",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Emerald50
            )

            Spacer(modifier = Modifier.height(36.dp))

            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 3.dp,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

// ============================================================================
// 8. ROOT APPLICATION SCREEN
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutriTrackAppScreen() {
    val context = LocalContext.current
    val store = remember { LocalStore(context) }

    var profile by remember { mutableStateOf(store.loadProfile()) }
    var customFoods by remember { mutableStateOf(store.loadCustomFoods()) }
    var selectedDate by remember { mutableStateOf(DateUtil.todayKey()) }
    var dailyLog by remember(selectedDate) { mutableStateOf(store.loadDailyLog(selectedDate)) }

    var isProfileModalOpen by remember { mutableStateOf(profile == null) }
    var isAddFoodModalOpen by remember { mutableStateOf(false) }
    var isDisclaimerOpen by remember { mutableStateOf(false) }
    var activeMealForModal by remember { mutableStateOf(MealType.BREAKFAST) }
    var editingEntry by remember { mutableStateOf<LoggedEntry?>(null) }
    var appTab by remember { mutableIntStateOf(0) }
    var isShowingSplash by remember { mutableStateOf(true) } // 0 = Today's Log, 1 = Analytics & Insights

    val allFoods = remember(customFoods) { DEFAULT_FOOD_DATABASE + customFoods }

    val activeProfile = profile ?: UserProfile(
        name = "Athlete",
        age = 28,
        gender = Gender.MALE,
        heightCm = 172.0,
        weightKg = 70.0,
        activityLevel = ActivityLevel.MODERATE,
        goal = FitnessGoal.MAINTAIN
    )

    if (isShowingSplash) {
        SplashScreen(onFinished = { isShowingSplash = false })
        return
    }

    val targets = remember(activeProfile) { NutritionEngine.calculateTargets(activeProfile) }
    val bmiInfo = remember(activeProfile) {
        NutritionEngine.calculateBMI(activeProfile.weightKg, activeProfile.heightCm)
    }

    val totalCalories = dailyLog.entries.sumOf { it.calories }.roundToInt()
    val totalProtein = NutritionEngine.round1(dailyLog.entries.sumOf { it.protein })
    val totalCarbs = NutritionEngine.round1(dailyLog.entries.sumOf { it.carbs })
    val totalFat = NutritionEngine.round1(dailyLog.entries.sumOf { it.fat })
    

    fun persistLog(updated: DailyLog) {
        dailyLog = updated
        store.saveDailyLog(updated)
    }

    Scaffold(
        containerColor = Slate50,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .statusBarsPadding()
            ) {
                // Brand & Profile Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Emerald600),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🥗",
                                fontSize = 20.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "NutriTrack",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Slate900
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Emerald50)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "PRO",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Emerald700
                                    )
                                }
                            }
                            Text(
                                text = "${activeProfile.name} • ${activeProfile.weightKg}kg • BMI ${bmiInfo.first} (${bmiInfo.second})",
                                fontSize = 11.sp,
                                color = Slate500,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                        tooltip = { PlainTooltip { Text("Profile") } },
                        state = rememberTooltipState()
                    ) {
                        IconButton(onClick = { isProfileModalOpen = true }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Default.Person, contentDescription = "Profile", tint = Emerald600)
                        }
                    }
                }

                HorizontalDivider(color = Slate200, thickness = 1.dp)

                // Sticky Date Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { selectedDate = DateUtil.shiftDay(selectedDate, -1) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Previous Day",
                            tint = Slate700
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Slate100)
                                .clickable { selectedDate = DateUtil.todayKey() }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = Emerald600,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = DateUtil.displayLabel(selectedDate),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                        }

                        if (selectedDate != DateUtil.todayKey()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(
                                onClick = { selectedDate = DateUtil.todayKey() },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Today",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Emerald600
                                )
                            }
                        }
                    }

                    IconButton(onClick = { selectedDate = DateUtil.shiftDay(selectedDate, 1) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next Day",
                            tint = Slate700
                        )
                    }
                }

                HorizontalDivider(color = Slate200, thickness = 1.dp)

                // Top View Switcher (Today's Log vs Analytics & Insights)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = appTab == 0,
                        onClick = { appTab = 0 },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.ListAlt, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("Today's Log", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = appTab == 1,
                        onClick = { appTab = 1 },
                        leadingIcon = { Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("Analytics & Insights", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        bottomBar = {
            Box(Modifier.fillMaxWidth().navigationBarsPadding(), contentAlignment = Alignment.Center) {
                TextButton(onClick = { isDisclaimerOpen = true }) {
                    Text("About nutrition & disclaimer", fontSize = 12.sp, color = Slate700)
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingEntry = null
                    activeMealForModal = MealType.BREAKFAST
                    isAddFoodModalOpen = true
                },
                containerColor = Emerald600,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Log Food")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Log Food", fontWeight = FontWeight.Bold)
            }
        }
    ) { innerPadding ->
        if (appTab == 0) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
            ) {
            item {
                DashboardOverviewSection(
                    consumedCalories = totalCalories,
                    targetCalories = targets.calories,
                    consumedProtein = totalProtein,
                    targetProtein = targets.protein,
                    consumedCarbs = totalCarbs,
                    targetCarbs = targets.carbs,
                    consumedFat = totalFat,
                    targetFat = targets.fat,
                    waterMl = dailyLog.waterIntakeMl,
                    targetWaterMl = targets.waterMl,
                    onAddWater = { delta ->
                        persistLog(
                            dailyLog.copy(waterIntakeMl = max(0, dailyLog.waterIntakeMl + delta))
                        )
                    },
                    onResetWater = {
                        persistLog(dailyLog.copy(waterIntakeMl = 0))
                    }
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Daily Meal Log",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Slate900
                    )
                    Text(
                        text = "${dailyLog.entries.size} entries logged",
                        fontSize = 12.sp,
                        color = Slate500
                    )
                }
            }

            items(MealType.entries) { meal ->
                val mealEntries = dailyLog.entries.filter { it.mealType == meal }
                MealCardSection(
                    mealType = meal,
                    entries = mealEntries,
                    onAddFood = {
                        editingEntry = null
                        activeMealForModal = meal
                        isAddFoodModalOpen = true
                    },
                    onEditEntry = { entry ->
                        editingEntry = entry
                        activeMealForModal = entry.mealType
                        isAddFoodModalOpen = true
                    },
                    onDeleteEntry = { entryId ->
                        persistLog(
                            dailyLog.copy(entries = dailyLog.entries.filterNot { it.id == entryId })
                        )
                    }
                )
            }
        }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AnalyticsSection(
                    store = store,
                    currentDateKey = selectedDate,
                    targets = targets
                )
            }
        }
    }

    if (isDisclaimerOpen) {
        NutritionDisclaimerDialog(onDismiss = { isDisclaimerOpen = false })
    }

    if (isProfileModalOpen) {
        ProfileOnboardingModal(
            initialProfile = profile,
            canDismiss = profile != null,
            onDismiss = { isProfileModalOpen = false },
            onSaveProfile = { newProfile ->
                profile = newProfile
                store.saveProfile(newProfile)
                isProfileModalOpen = false
            }
        )
    }

    if (isAddFoodModalOpen) {
        AddOrEditFoodModal(
            initialMealType = activeMealForModal,
            editingEntry = editingEntry,
            foods = allFoods,
            onDismiss = {
                isAddFoodModalOpen = false
                editingEntry = null
            },
            onSaveEntry = { savedEntry ->
                val updatedEntries = if (editingEntry != null) {
                    dailyLog.entries.map { if (it.id == savedEntry.id) savedEntry else it }
                } else {
                    dailyLog.entries + savedEntry
                }
                persistLog(dailyLog.copy(entries = updatedEntries))
                isAddFoodModalOpen = false
                editingEntry = null
            },
            onCreateCustomFoodAndLog = { newFood, meal ->
                val nextCustom = customFoods + newFood
                customFoods = nextCustom
                store.saveCustomFoods(nextCustom)
                val firstEntry = NutritionEngine.buildEntry(newFood, meal, "serving", 1.0)
                persistLog(dailyLog.copy(entries = dailyLog.entries + firstEntry))
                isAddFoodModalOpen = false
                editingEntry = null
            }
        )
    }
}

// ============================================================================
// 9. DASHBOARD OVERVIEW (CALORIE RING + 4 BARS + WATER CONTROLS)
// ============================================================================

@Composable
fun DashboardOverviewSection(
    consumedCalories: Int,
    targetCalories: Int,
    consumedProtein: Double,
    targetProtein: Int,
    consumedCarbs: Double,
    targetCarbs: Int,
    consumedFat: Double,
    targetFat: Int,
    waterMl: Int,
    targetWaterMl: Int,
    onAddWater: (Int) -> Unit,
    onResetWater: () -> Unit
) {
    val ratio = if (targetCalories > 0) {
        (consumedCalories.toFloat() / targetCalories.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val sweepAngle by animateFloatAsState(
        targetValue = ratio * 360f,
        animationSpec = tween(durationMillis = 550),
        label = "CalorieArc"
    )

    val isOver = consumedCalories > targetCalories
    val ringColor = if (!isOver) Emerald600 else Rose500
    val calDiff = targetCalories - consumedCalories
    val exactPct = if (targetCalories > 0) (consumedCalories * 100) / targetCalories else 0
    
    val suggestions = remember(consumedCalories, consumedProtein, consumedFat, consumedCarbs) {
        val list = mutableListOf<String>()
        if (consumedCalories > targetCalories) list.add("🔥 Calorie limit exceeded! Opt for a light walk.")
        else if (targetCalories - consumedCalories > 600) list.add("🍽️ Great! You have room for balanced meals.")
        else list.add("✅ Calorie intake is on track today.")

        if (targetProtein > 0 && consumedProtein < targetProtein * 0.7) list.add("💪 Protein is low! Add paneer, dal, or lean meat.")
        if (targetFat > 0 && consumedFat > targetFat) list.add("🥑 Fat intake is high! Avoid deep-fried items today.")
        list
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Suggestions Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Slate100),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Daily Insights", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Slate900)
                suggestions.forEach { suggestion ->
                    Text(suggestion, fontSize = 12.sp, color = Slate700, fontWeight = FontWeight.Medium)
                }
            }
        }

        // Calorie Progress Ring Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Daily Energy Budget",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Text(
                            text = "Target: $targetCalories kcal",
                            fontSize = 12.sp,
                            color = Slate500
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (!isOver) Emerald50 else Rose50)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$exactPct% fulfilled",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (!isOver) Emerald700 else Rose700
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(164.dp)
                ) {
                    Canvas(modifier = Modifier.size(148.dp)) {
                        drawArc(
                            color = Slate100,
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = ringColor,
                            startAngle = -90f,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "CONSUMED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate400
                        )
                        Text(
                            text = "$consumedCalories",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Slate900
                        )
                        Text(
                            text = "/ $targetCalories kcal",
                            fontSize = 11.sp,
                            color = Slate500
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (!isOver) Slate50 else Rose50)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (!isOver) "Remaining Today" else "Over Daily Target",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (!isOver) Slate700 else Rose700
                    )
                    Text(
                        text = if (!isOver) "$calDiff kcal left" else "+${-calDiff} kcal over",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (!isOver) Emerald700 else Rose700
                    )
                }
            }
        }

        // Macronutrients & Hydration Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Macronutrients & Hydration",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )

                NutrientProgressRow(
                    title = "Protein",
                    consumedText = "${consumedProtein}g",
                    targetText = "${targetProtein}g",
                    ratio = if (targetProtein > 0) (consumedProtein / targetProtein).toFloat() else 0f,
                    barColor = Blue500,
                    badgeBg = Blue50,
                    badgeFg = Blue700
                )

                NutrientProgressRow(
                    title = "Carbohydrates",
                    consumedText = "${consumedCarbs}g",
                    targetText = "${targetCarbs}g",
                    ratio = if (targetCarbs > 0) (consumedCarbs / targetCarbs).toFloat() else 0f,
                    barColor = Amber500,
                    badgeBg = Amber50,
                    badgeFg = Amber700
                )

                NutrientProgressRow(
                    title = "Dietary Fats",
                    consumedText = "${consumedFat}g",
                    targetText = "${targetFat}g",
                    ratio = if (targetFat > 0) (consumedFat / targetFat).toFloat() else 0f,
                    barColor = Rose500,
                    badgeBg = Rose50,
                    badgeFg = Rose700
                )

                // Hydration Subcard
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Cyan50)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    NutrientProgressRow(
                        title = "💧 Daily Hydration",
                        consumedText = "$waterMl ml",
                        targetText = "$targetWaterMl ml",
                        ratio = if (targetWaterMl > 0) waterMl.toFloat() / targetWaterMl.toFloat() else 0f,
                        barColor = Cyan500,
                        badgeBg = Color.White,
                        badgeFg = Cyan700
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onAddWater(250) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Cyan700
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Text("+250 ml", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { onAddWater(500) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Cyan700
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Text("+500 ml", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onResetWater,
                            enabled = waterMl > 0,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset Water",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 9.5 ANALYTICAL SUMMARY & HEALTH RECOMMENDATIONS
// ============================================================================

@Composable
fun AnalyticsSection(
    store: LocalStore,
    currentDateKey: String,
    targets: MacroTargets
) {
    var timeframe by remember { mutableIntStateOf(1) } // 0 = Daily, 1 = Weekly (7d), 2 = Monthly (30d)

    val daysCount = when (timeframe) {
        0 -> 1
        1 -> 7
        else -> 30
    }

    val pastLogs = remember(currentDateKey, daysCount) {
        (0 until daysCount).map { offset ->
            val dateKey = DateUtil.shiftDay(currentDateKey, -offset)
            val log = store.loadDailyLog(dateKey)
            dateKey to log
        }.reversed()
    }

    val totalCals = pastLogs.sumOf { (_, log) -> log.entries.sumOf { it.calories } }
    val avgCals = if (pastLogs.isNotEmpty()) totalCals / pastLogs.size else 0.0
    val avgProtein = if (pastLogs.isNotEmpty()) pastLogs.sumOf { (_, log) -> log.entries.sumOf { it.protein } } / pastLogs.size else 0.0
    val avgCarbs = if (pastLogs.isNotEmpty()) pastLogs.sumOf { (_, log) -> log.entries.sumOf { it.carbs } } / pastLogs.size else 0.0
    val avgFat = if (pastLogs.isNotEmpty()) pastLogs.sumOf { (_, log) -> log.entries.sumOf { it.fat } } / pastLogs.size else 0.0

    val activeDays = pastLogs.count { (_, log) -> log.entries.isNotEmpty() }
    val targetAdherencePct = if (pastLogs.isNotEmpty()) {
        val metTargetDays = pastLogs.count { (_, log) ->
            val cals = log.entries.sumOf { it.calories }
            cals > 0 && cals <= targets.calories + 100
        }
        (metTargetDays * 100) / pastLogs.size
    } else 0

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Daily", "Weekly (7 Days)", "Monthly (30 Days)").forEachIndexed { idx, label ->
                    Button(
                        onClick = { timeframe = idx },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (timeframe == idx) Emerald600 else Slate100,
                            contentColor = if (timeframe == idx) Color.White else Slate700
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = when(timeframe) {
                            0 -> "Today's Intake Summary"
                            1 -> "7-Day Performance & Trends"
                            else -> "30-Day Monthly Summary"
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Daily Avg Energy", fontSize = 11.sp, color = Slate500)
                            Text("${avgCals.roundToInt()} kcal", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Emerald700)
                            Text("Goal: ${targets.calories} kcal", fontSize = 10.sp, color = Slate400)
                        }
                        Column {
                            Text("Logged Days", fontSize = 11.sp, color = Slate500)
                            Text("$activeDays / $daysCount days", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Slate900)
                            Text("Adherence: $targetAdherencePct%", fontSize = 10.sp, color = Emerald600, fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(color = Slate100)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Avg Protein", fontSize = 11.sp, color = Slate500)
                            Text("${NutritionEngine.round1(avgProtein)}g / ${targets.protein}g", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Blue700)
                        }
                        Column {
                            Text("Avg Carbs", fontSize = 11.sp, color = Slate500)
                            Text("${NutritionEngine.round1(avgCarbs)}g / ${targets.carbs}g", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Amber700)
                        }
                        Column {
                            Text("Avg Fat", fontSize = 11.sp, color = Slate500)
                            Text("${NutritionEngine.round1(avgFat)}g / ${targets.fat}g", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Rose700)
                        }
                    }
                }
            }
        }

        if (timeframe > 0) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Daily Calorie Intake Bar Chart", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Slate900)

                        val maxChartCal = maxOf(targets.calories * 1.3, pastLogs.maxOfOrNull { (_, l) -> l.entries.sumOf { it.calories } } ?: 2000.0)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            pastLogs.forEach { (dKey, log) ->
                                val cals = log.entries.sumOf { it.calories }
                                val barHeightRatio = (cals / maxChartCal).coerceIn(0.05, 1.0).toFloat()
                                val isOverTarget = cals > targets.calories

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom,
                                    modifier = Modifier.height(130.dp)
                                ) {
                                    if (cals > 0) {
                                        Text("${cals.roundToInt()}", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Slate500)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .width(22.dp)
                                            .fillMaxHeight(barHeightRatio)
                                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                            .background(if (cals == 0.0) Slate200 else if (isOverTarget) Rose500 else Emerald600)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(DateUtil.shortDisplayLabel(dKey), fontSize = 9.sp, color = Slate500)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Recommended Daily Intake of Macronutrients Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "📋 Recommended Daily Intake of Macronutrients",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Emerald50)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Daily Energy Target", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Emerald700)
                            Text("${targets.calories} kcal / day", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Slate900)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Emerald600)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Target TDEE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Blue700))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Protein Target", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate700)
                            }
                            Text("${targets.protein}g / day", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Blue700)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Amber700))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Carbohydrates Target", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate700)
                            }
                            Text("${targets.carbs}g / day", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Amber700)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Rose700))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Dietary Fats Target", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate700)
                            }
                            Text("${targets.fat}g / day", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Rose700)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Cyan500))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Daily Hydration Target", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate700)
                            }
                            Text("${targets.waterMl} ml / day", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Cyan700)
                        }
                    }
                }
            }
        }

        // General Nutritional Advice & Best Practices Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Slate100),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "💡 General Nutritional Advice & Best Practices",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Slate900
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("🥗 Balanced Plate Proportions", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Emerald700)
                            Text("Aim to fill 50% of your plate with green vegetables & fresh salad, 25% with lean protein (paneer, dal, eggs, chicken), and 25% with complex whole grains.", fontSize = 11.sp, color = Slate700)
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("🥩 Even Protein Distribution", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Blue700)
                            Text("Spread your daily protein target across 3-4 meals to maximize muscle protein synthesis and maintain satiety throughout the day.", fontSize = 11.sp, color = Slate700)
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("🌾 Complex Carbohydrates First", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Amber700)
                            Text("Choose unrefined grains like oats, millets, brown rice, and whole wheat roti over maida or sugary drinks to sustain steady insulin levels.", fontSize = 11.sp, color = Slate700)
                        }


                    }
                }
            }
        }
    }
}

@Composable
fun NutrientProgressRow(
    title: String,
    consumedText: String,
    targetText: String,
    ratio: Float,
    barColor: Color,
    badgeBg: Color,
    badgeFg: Color
) {
    val pct = (ratio * 100f).roundToInt()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Slate700
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$consumedText / $targetText",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeBg)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "$pct%",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeFg
                    )
                }
            }
        }
        LinearProgressIndicator(
            progress = { ratio.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape),
            color = barColor,
            trackColor = Slate100
        )
    }
}

// ============================================================================
// 10. COLLAPSIBLE MEAL CARD SECTION
// ============================================================================

@Composable
fun MealCardSection(
    mealType: MealType,
    entries: List<LoggedEntry>,
    onAddFood: () -> Unit,
    onEditEntry: (LoggedEntry) -> Unit,
    onDeleteEntry: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(true) }

    val mealCalories = entries.sumOf { it.calories }.roundToInt()
    val mealP = NutritionEngine.round1(entries.sumOf { it.protein })
    val mealC = NutritionEngine.round1(entries.sumOf { it.carbs })
    val mealF = NutritionEngine.round1(entries.sumOf { it.fat })

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Emerald50),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = mealType.emoji, fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = mealType.label,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Slate100)
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${entries.size}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate700
                                )
                            }
                        }
                        Text(
                            text = "$mealCalories kcal • Protein: ${mealP}g • Carbohydrates: ${mealC}g • Fats: ${mealF}g",
                            fontSize = 11.sp,
                            color = Slate500
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = onAddFood,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Emerald50,
                            contentColor = Emerald700
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Add", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Expand or Collapse",
                            tint = Slate400
                        )
                    }
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(color = Slate100)
                    if (entries.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No items logged for ${mealType.label.lowercase()} yet.",
                                fontSize = 12.sp,
                                color = Slate400
                            )
                        }
                    } else {
                        entries.forEach { entry ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = entry.foodName,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate900
                                    )
                                    Text(
                                        text = "${entry.servingLabel} (${entry.totalGrams.roundToInt()}g)",
                                        fontSize = 11.sp,
                                        color = Slate500
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        MiniPill("${entry.calories.roundToInt()} kcal", Emerald50, Emerald700)
                                        MiniPill("P: ${NutritionEngine.round1(entry.protein)}g", Blue50, Blue700)
                                        MiniPill("C: ${NutritionEngine.round1(entry.carbs)}g", Amber50, Amber700)
                                        MiniPill("F: ${NutritionEngine.round1(entry.fat)}g", Rose50, Rose700)
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                                    IconButton(onClick = { onEditEntry(entry) }, modifier = Modifier.size(48.dp)) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit ${entry.foodName}",
                                            tint = Slate500,
                                            modifier = Modifier.size(24.dp).offset(x = 4.dp)
                                        )
                                    }
                                    IconButton(onClick = { onDeleteEntry(entry.id) }, modifier = Modifier.size(48.dp)) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete ${entry.foodName}",
                                            tint = Rose500,
                                            modifier = Modifier.size(24.dp).offset(x = (-4).dp)
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(color = Slate100)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MiniPill(text: String, bg: Color, fg: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text = text, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = fg)
    }
}

// ============================================================================
// 11. ONBOARDING & PROFILE MODAL
// ============================================================================

@Composable
fun ProfileOnboardingModal(
    initialProfile: UserProfile?,
    canDismiss: Boolean,
    onDismiss: () -> Unit,
    onSaveProfile: (UserProfile) -> Unit
) {
    var name by remember { mutableStateOf(initialProfile?.name ?: "") }
    var ageStr by remember { mutableStateOf((initialProfile?.age ?: 28).toString()) }
    var gender by remember { mutableStateOf(initialProfile?.gender ?: Gender.MALE) }
    var heightStr by remember { mutableStateOf((initialProfile?.heightCm ?: 172.0).toString()) }
    var weightStr by remember { mutableStateOf((initialProfile?.weightKg ?: 70.0).toString()) }
    var targetWeightStr by remember {
        mutableStateOf(initialProfile?.targetWeightKg?.toString() ?: "")
    }
    var activity by remember {
        mutableStateOf(initialProfile?.activityLevel ?: ActivityLevel.MODERATE)
    }
    var goal by remember { mutableStateOf(initialProfile?.goal ?: FitnessGoal.MAINTAIN) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val previewProfile = UserProfile(
        name = name.ifBlank { "User" },
        age = ageStr.toIntOrNull()?.coerceIn(13, 100) ?: 28,
        gender = gender,
        heightCm = heightStr.toDoubleOrNull()?.coerceIn(100.0, 250.0) ?: 172.0,
        weightKg = weightStr.toDoubleOrNull()?.coerceIn(30.0, 300.0) ?: 70.0,
        activityLevel = activity,
        goal = goal
    )
    val previewTargets = NutritionEngine.calculateTargets(previewProfile)
    val previewBmi = NutritionEngine.calculateBMI(previewProfile.weightKg, previewProfile.heightCm)

    Dialog(
        onDismissRequest = { if (canDismiss) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.90f)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Slate50)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (initialProfile == null) "Personalize Your Targets" else "Edit Profile & Goals",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Slate900
                        )
                        Text(
                            text = "Mifflin-St Jeor Clinical Calculation Engine",
                            fontSize = 11.sp,
                            color = Slate500
                        )
                    }
                    if (canDismiss) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (errorMsg != null) {
                        Text(
                            text = errorMsg!!,
                            color = Rose700,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Your Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Gender.entries.forEach { g ->
                            val selected = gender == g
                            OutlinedButton(
                                onClick = { gender = g },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (selected) Emerald50 else Color.White,
                                    contentColor = if (selected) Emerald700 else Slate700
                                )
                            ) {
                                Text(g.label, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = ageStr,
                            onValueChange = { ageStr = it },
                            label = { Text("Age (13-100)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = heightStr,
                            onValueChange = { heightStr = it },
                            label = { Text("Height (cm)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = weightStr,
                            onValueChange = { weightStr = it },
                            label = { Text("Weight (kg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = targetWeightStr,
                            onValueChange = { targetWeightStr = it },
                            label = { Text("Goal Wt (opt)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text("Activity Level", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Slate700)
                    ActivityLevel.entries.forEach { act ->
                        val selected = activity == act
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    width = 1.dp,
                                    color = if (selected) Emerald600 else Slate200,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .background(if (selected) Emerald50 else Color.White)
                                .clickable { activity = act }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(act.label, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Slate900)
                                Text(act.desc, fontSize = 11.sp, color = Slate500)
                            }
                            Text("${act.multiplier}×", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Emerald700)
                        }
                    }

                    Text("Fitness Goal", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Slate700)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FitnessGoal.entries.forEach { fg ->
                            val selected = goal == fg
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(
                                        width = 1.dp,
                                        color = if (selected) Emerald600 else Slate200,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .background(if (selected) Emerald50 else Color.White)
                                    .clickable { goal = fg }
                                    .padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(fg.label, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Slate900)
                                Text(fg.badge, fontSize = 10.sp, color = Emerald700, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    // Live Biometric & Target Preview Box
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Slate100)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Live Preview • BMI ${previewBmi.first} (${previewBmi.second})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Text(
                            text = "BMR: ${previewTargets.bmr} kcal  •  TDEE: ${previewTargets.tdee} kcal  •  Target: ${previewTargets.calories} kcal",
                            fontSize = 11.sp,
                            color = Slate700,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Protein: ${previewTargets.protein}g  •  Carbs: ${previewTargets.carbs}g  •  Fat: ${previewTargets.fat}g  •  Water: ${previewTargets.waterMl}ml",
                            fontSize = 11.sp,
                            color = Emerald700,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = {
                            val age = ageStr.toIntOrNull()
                            val h = heightStr.toDoubleOrNull()
                            val w = weightStr.toDoubleOrNull()
                            val gw = targetWeightStr.toDoubleOrNull()
                            if (name.trim().length < 2) {
                                errorMsg = "Please enter your name (at least 2 characters)."
                                return@Button
                            }
                            if (age == null || age !in 13..100 || h == null || h !in 100.0..250.0 || w == null || w !in 30.0..300.0) {
                                errorMsg = "Check bounds: Age (13–100), Height (100–250cm), Weight (30–300kg)."
                                return@Button
                            }
                            onSaveProfile(
                                UserProfile(
                                    name = name.trim(),
                                    age = age,
                                    gender = gender,
                                    heightCm = h,
                                    weightKg = w,
                                    activityLevel = activity,
                                    goal = goal,
                                    targetWeightKg = gw
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (initialProfile == null) "Start Tracking Macros" else "Save Profile Changes",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// 12. ADD / EDIT / CUSTOM FOOD MODAL
// ============================================================================

@Composable
fun AddOrEditFoodModal(
    initialMealType: MealType,
    editingEntry: LoggedEntry?,
    foods: List<FoodItem>,
    onDismiss: () -> Unit,
    onSaveEntry: (LoggedEntry) -> Unit,
    onCreateCustomFoodAndLog: (FoodItem, MealType) -> Unit
) {
    val context = LocalContext.current
    val store = remember(context) { LocalStore(context) }
    val recentFoods = remember(foods) { store.getRecentFoods(foods) }

    var selectedMeal by remember { mutableStateOf(editingEntry?.mealType ?: initialMealType) }
    var activeTab by remember { mutableIntStateOf(0) } // 0 = Search Database, 1 = Create Custom
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    var selectedFood by remember {
        mutableStateOf(
            if (editingEntry != null) foods.find { it.id == editingEntry.foodId } else null
        )
    }
    var inputMode by remember { mutableStateOf(editingEntry?.inputMode ?: "serving") }
    var quantityStr by remember {
        mutableStateOf(
            if (editingEntry != null) {
                if (editingEntry.quantity % 1.0 == 0.0) editingEntry.quantity.toInt().toString()
                else editingEntry.quantity.toString()
            } else "1"
        )
    }

    // Custom Food state
    var customName by remember { mutableStateOf("") }
    var customCategory by remember { mutableStateOf("Meals & Snacks") }
    var customCal by remember { mutableStateOf("") }
    var customPro by remember { mutableStateOf("") }
    var customCarb by remember { mutableStateOf("") }
    var customFat by remember { mutableStateOf("") }
    var customServingLabel by remember { mutableStateOf("1 bowl (150g)") }
    var customServingWeight by remember { mutableStateOf("150") }
    var customError by remember { mutableStateOf<String?>(null) }

    val filteredFoods = remember(foods, searchQuery, selectedCategory) {
        foods.filter { item ->
            val matchesCat = selectedCategory == "All" || item.category == selectedCategory
            val matchesSearch = FoodSearch.matches(item, searchQuery)
            matchesCat && matchesSearch
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Slate50)
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when {
                            editingEntry != null -> "Edit ${editingEntry.foodName}"
                            selectedFood != null -> "Configure ${selectedFood!!.name}"
                            else -> "Log Food or Drink (${foods.size} items)"
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Slate900,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Meal Selector Pills
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MealType.entries.forEach { meal ->
                        FilterChip(
                            selected = selectedMeal == meal,
                            onClick = { selectedMeal = meal },
                            label = { Text("${meal.emoji} ${meal.label}", fontSize = 12.sp) }
                        )
                    }
                }

                if (selectedFood != null) {
                    // Quantity Configurator
                    val food = selectedFood!!
                    val qty = quantityStr.toDoubleOrNull()?.coerceAtLeast(0.1) ?: 1.0
                    val previewEntry = NutritionEngine.buildEntry(
                        food = food,
                        mealType = selectedMeal,
                        inputMode = inputMode,
                        quantity = qty,
                        existingId = editingEntry?.id
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(food.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Slate900)
                                Text(IndianFoodExpansion.note(food), fontSize = 12.sp, color = Slate700)
                                val baseServCal = food.calories.roundToInt()
                                val baseServPro = NutritionEngine.round1(food.protein)
                                val baseServCarb = NutritionEngine.round1(food.carbs)
                                val baseServFat = NutritionEngine.round1(food.fat)
                                
                                Text(
                                    "Per Serving (${food.servingGramWeight.roundToInt()}${food.unitType}): $baseServCal kcal • Protein: ${baseServPro}g • Carbohydrates: ${baseServCarb}g • Fats: ${baseServFat}g",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                                Text(
                                    "Per 100${food.unitType}: ${(food.calories * 100.0 / food.servingGramWeight).roundToInt()} kcal • Protein: ${NutritionEngine.round1(food.protein * 100.0 / food.servingGramWeight)}g • Carbohydrates: ${NutritionEngine.round1(food.carbs * 100.0 / food.servingGramWeight)}g • Fats: ${NutritionEngine.round1(food.fat * 100.0 / food.servingGramWeight)}g",
                                    fontSize = 11.sp,
                                    color = Slate400
                                )
                            }
                            if (editingEntry == null) {
                                TextButton(onClick = { selectedFood = null }) {
                                    Text("Change")
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    inputMode = "serving"
                                    quantityStr = "1"
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (inputMode == "serving") Emerald50 else Color.White
                                )
                            ) {
                                Text("By Serving", fontWeight = FontWeight.Bold, color = Emerald700)
                            }
                            OutlinedButton(
                                onClick = {
                                    inputMode = "weight"
                                    quantityStr = food.servingGramWeight.roundToInt().toString()
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (inputMode == "weight") Emerald50 else Color.White
                                )
                            ) {
                                Text("By Weight (${food.unitType})", fontWeight = FontWeight.Bold, color = Emerald700)
                            }
                        }

                        OutlinedTextField(
                            value = quantityStr,
                            onValueChange = { quantityStr = it },
                            label = {
                                Text(
                                    if (inputMode == "serving") "Servings (${food.servingLabel})"
                                    else "Exact Weight (${food.unitType})"
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Quick Multipliers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(0.5, 1.0, 1.5, 2.0, 3.0, 4.0, 5.0).forEach { mult ->
                                OutlinedButton(
                                    onClick = {
                                        quantityStr = if (inputMode == "serving") {
                                            if (mult % 1.0 == 0.0) mult.toInt().toString() else mult.toString()
                                        } else {
                                            (food.servingGramWeight * mult).roundToInt().toString()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(vertical = 6.dp)
                                ) {
                                    Text("${mult}x", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Live Nutrition Preview Card
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Emerald50)
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Live Entry Preview (${previewEntry.totalGrams.roundToInt()}${food.unitType})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Emerald700
                            )
                            Text(
                                text = "${previewEntry.calories.roundToInt()} kcal  •  Protein: ${NutritionEngine.round1(previewEntry.protein)}g  •  Carbohydrates: ${NutritionEngine.round1(previewEntry.carbs)}g  •  Fats: ${NutritionEngine.round1(previewEntry.fat)}g",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Slate900
                            )
                        }

                        Button(
                            onClick = { onSaveEntry(previewEntry) },
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (editingEntry != null) "Save Changes" else "Log to ${selectedMeal.label}",
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                } else {
                    // Tab Switcher
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { activeTab = 0 },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (activeTab == 0) Emerald600 else Slate100,
                                contentColor = if (activeTab == 0) Color.White else Slate700
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Search Database", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { activeTab = 1 },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (activeTab == 1) Emerald600 else Slate100,
                                contentColor = if (activeTab == 1) Color.White else Slate700
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Create Custom Food", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (activeTab == 0) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search Roti, Paneer, Dal, Dosa, Chai...", fontSize = 12.sp) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            )

                            if (recentFoods.isNotEmpty() && searchQuery.isBlank() && selectedCategory == "All") {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("🕒 Recent & Frequently Consumed", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate700)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        recentFoods.forEach { recentFood ->
                                            Card(
                                                shape = RoundedCornerShape(10.dp),
                                                colors = CardDefaults.cardColors(containerColor = Emerald50),
                                                modifier = Modifier.clickable {
                                                    selectedFood = recentFood
                                                    inputMode = "serving"
                                                    quantityStr = "1"
                                                }
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                ) {
                                                    Text(recentFood.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate900)
                                                    Text("${recentFood.calories.roundToInt()} kcal • ${recentFood.servingLabel}", fontSize = 9.sp, color = Emerald700)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                ALL_CATEGORIES.forEach { cat ->
                                    FilterChip(
                                        selected = selectedCategory == cat,
                                        onClick = { selectedCategory = cat },
                                        label = { Text(cat, fontSize = 11.sp) }
                                    )
                                }
                            }

                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(filteredFoods) { food ->
                                    val defEntry = NutritionEngine.buildEntry(food, selectedMeal, "serving", 1.0)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .border(1.dp, Slate200, RoundedCornerShape(10.dp))
                                            .clickable {
                                                selectedFood = food
                                                inputMode = "serving"
                                                quantityStr = "1"
                                            }
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(food.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Slate900)
                                            Text("Estimated nutrition • ${food.servingLabel}", fontSize = 12.sp, color = Slate700)
                                            Text(
                                                "Per Serving (${food.servingGramWeight.roundToInt()}${food.unitType}): ${defEntry.calories.roundToInt()} kcal • Protein: ${NutritionEngine.round1(defEntry.protein)}g • Carbohydrates: ${NutritionEngine.round1(defEntry.carbs)}g • Fats: ${NutritionEngine.round1(defEntry.fat)}g",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Slate700
                                            )
                                            Text(
                                                "Per 100${food.unitType}: ${(food.calories * 100.0 / food.servingGramWeight).roundToInt()} kcal (Protein: ${NutritionEngine.round1(food.protein * 100.0 / food.servingGramWeight)}g • Carbohydrates: ${NutritionEngine.round1(food.carbs * 100.0 / food.servingGramWeight)}g • Fats: ${NutritionEngine.round1(food.fat * 100.0 / food.servingGramWeight)}g)",
                                                fontSize = 11.sp,
                                                color = Slate400
                                            )
                                        }
                                        Text(
                                            text = "Select",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Emerald600
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Create Custom Food Form
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (customError != null) {
                                Text(
                                    text = customError!!,
                                    color = Rose700,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            OutlinedTextField(
                                value = customName,
                                onValueChange = { customName = it },
                                label = { Text("Food Name (e.g., Homemade Sattu Drink)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = customServingLabel,
                                    onValueChange = { customServingLabel = it },
                                    label = { Text("Serving Label") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = customServingWeight,
                                    onValueChange = { customServingWeight = it },
                                    label = { Text("Grams (g)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Text("Nutrition per serving", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Slate700)

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = customCal,
                                    onValueChange = { customCal = it },
                                    label = { Text("Calories (kcal) per serving") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = customPro,
                                    onValueChange = { customPro = it },
                                    label = { Text("Protein (g)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = customCarb,
                                    onValueChange = { customCarb = it },
                                    label = { Text("Carbs (g)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = customFat,
                                    onValueChange = { customFat = it },
                                    label = { Text("Fat (g)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Button(
                                onClick = {
                                    if (customName.trim().length < 2) {
                                        customError = "Please enter a food name."
                                        return@Button
                                    }
                                    val cal = customCal.toDoubleOrNull()
                                    if (cal == null || cal < 0.0) {
                                        customError = "Please enter valid calories per serving."
                                        return@Button
                                    }
                                    val newCustomFood = FoodItem(
                                        id = "custom_${System.currentTimeMillis()}",
                                        name = customName.trim(),
                                        category = customCategory,
                                        calories = cal,
                                        protein = customPro.toDoubleOrNull() ?: 0.0,
                                        carbs = customCarb.toDoubleOrNull() ?: 0.0,
                                        fat = customFat.toDoubleOrNull() ?: 0.0,
                                        unitType = "g",
                                        servingLabel = customServingLabel.ifBlank { "1 serving" },
                                        servingGramWeight = customServingWeight.toDoubleOrNull() ?: 100.0,
                                        isCustom = true
                                    )
                                    onCreateCustomFoodAndLog(newCustomFood, selectedMeal)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Save & Log 1 Serving", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
