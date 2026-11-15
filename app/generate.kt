import kotlin.random.Random
import java.io.File

fun main() {
    val items = mutableListOf<String>()

    val baseGrains = listOf(
        listOf("Roti", "Grains & Breads", 100, 3, 20, 1, 3, 10, "g", "1 roti", 35.0),
        listOf("Rice", "Grains & Breads", 130, 3, 28, 0, 1, 1, "g", "1 katori", 150.0),
        listOf("Naan", "Grains & Breads", 250, 8, 40, 5, 2, 300, "g", "1 naan", 80.0),
        listOf("Paratha", "Grains & Breads", 200, 5, 30, 8, 3, 200, "g", "1 paratha", 60.0),
        listOf("Bread", "Grains & Breads", 150, 5, 30, 2, 2, 250, "g", "2 slices", 50.0),
        listOf("Oats", "Grains & Breads", 140, 5, 25, 3, 4, 10, "g", "1 bowl", 150.0),
        listOf("Quinoa", "Grains & Breads", 120, 4, 21, 2, 3, 7, "g", "1 bowl", 150.0)
    )
    val modifiersGrains = listOf("Plain", "Butter", "Garlic", "Cheese", "Whole Wheat", "Multigrain", "Tandoori", "Stuffed", "Missi", "Gluten-Free")

    val baseProteins = listOf(
        listOf("Dal", "Proteins", 120, 6, 15, 3, 4, 300, "g", "1 katori", 150.0),
        listOf("Paneer", "Proteins", 250, 15, 3, 20, 0, 50, "g", "1 bowl", 100.0),
        listOf("Chicken", "Proteins", 150, 25, 0, 5, 0, 70, "g", "1 breast", 100.0),
        listOf("Egg", "Proteins", 70, 6, 0, 5, 0, 70, "g", "1 egg", 50.0),
        listOf("Fish", "Proteins", 120, 20, 0, 4, 0, 50, "g", "1 fillet", 100.0),
        listOf("Mutton", "Proteins", 250, 20, 0, 18, 0, 80, "g", "1 bowl", 150.0),
        listOf("Tofu", "Proteins", 80, 8, 2, 4, 1, 10, "g", "1 bowl", 100.0),
        listOf("Soy", "Proteins", 150, 15, 10, 5, 5, 20, "g", "1 bowl", 100.0)
    )
    val modifiersProteins = listOf("Boiled", "Grilled", "Fried", "Roasted", "Curry", "Tikka", "Masala", "Butter", "Spicy", "Smoked", "Kebab", "Tandoori")

    val baseVeg = listOf(
        listOf("Potato", "Fruits & Veg", 100, 2, 22, 0, 2, 10, "g", "1 bowl", 100.0),
        listOf("Spinach", "Fruits & Veg", 20, 3, 3, 0, 2, 70, "g", "1 bowl", 100.0),
        listOf("Cabbage", "Fruits & Veg", 25, 1, 6, 0, 3, 20, "g", "1 bowl", 100.0),
        listOf("Cauliflower", "Fruits & Veg", 25, 2, 5, 0, 2, 30, "g", "1 bowl", 100.0),
        listOf("Okra", "Fruits & Veg", 30, 2, 7, 0, 3, 10, "g", "1 bowl", 100.0),
        listOf("Eggplant", "Fruits & Veg", 25, 1, 6, 0, 3, 5, "g", "1 bowl", 100.0),
        listOf("Mixed Veg", "Fruits & Veg", 50, 2, 10, 0, 3, 30, "g", "1 bowl", 100.0)
    )
    val modifiersVeg = listOf("Boiled", "Steamed", "Fried", "Roasted", "Stir-fried", "Curry", "Masala", "Dry", "Spicy", "With Peas")

    val baseSnacks = listOf(
        listOf("Samosa", "Meals & Snacks", 250, 4, 30, 12, 2, 400, "g", "1 piece", 80.0),
        listOf("Pizza", "Fast Food & Junk", 300, 12, 35, 12, 2, 600, "g", "1 slice", 120.0),
        listOf("Burger", "Fast Food & Junk", 400, 15, 45, 18, 3, 800, "g", "1 burger", 150.0),
        listOf("Fries", "Fast Food & Junk", 350, 4, 45, 16, 4, 300, "g", "1 serve", 100.0),
        listOf("Chips", "Fast Food & Junk", 500, 5, 50, 30, 3, 600, "g", "1 packet", 100.0),
        listOf("Noodles", "Fast Food & Junk", 200, 5, 30, 7, 2, 500, "g", "1 bowl", 150.0),
        listOf("Momos", "Fast Food & Junk", 150, 6, 25, 3, 2, 300, "g", "6 pieces", 100.0),
        listOf("Poha", "Meals & Snacks", 200, 4, 40, 2, 2, 10, "g", "1 bowl", 150.0),
        listOf("Idli", "Meals & Snacks", 80, 2, 15, 0, 1, 100, "g", "1 piece", 40.0),
        listOf("Dosa", "Meals & Snacks", 150, 4, 25, 4, 1, 200, "g", "1 piece", 80.0)
    )
    val modifiersSnacks = listOf("Classic", "Cheese", "Spicy", "Sweet", "Diet", "Baked", "Extra Large", "Mini", "Special", "Premium")

    val baseSweets = listOf(
        listOf("Gulab Jamun", "Sweets & Desserts", 150, 2, 22, 6, 0, 20, "g", "1 piece", 50.0),
        listOf("Rasgulla", "Sweets & Desserts", 100, 2, 20, 1, 0, 10, "g", "1 piece", 50.0),
        listOf("Jalebi", "Sweets & Desserts", 150, 1, 30, 5, 0, 10, "g", "1 piece", 50.0),
        listOf("Barfi", "Sweets & Desserts", 120, 3, 15, 6, 0, 15, "g", "1 piece", 30.0),
        listOf("Ice Cream", "Sweets & Desserts", 200, 4, 25, 10, 0, 50, "g", "1 scoop", 100.0),
        listOf("Chocolate", "Sweets & Desserts", 250, 3, 30, 15, 2, 40, "g", "1 bar", 50.0)
    )
    val modifiersSweets = listOf("Classic", "Sugar-Free", "Low Fat", "Extra Sweet", "Premium", "Nutty", "Saffron", "Pistachio", "Rose", "Mango")

    val baseDrinks = listOf(
        listOf("Tea", "Beverages", 50, 1, 10, 1, 0, 10, "ml", "1 cup", 150.0),
        listOf("Coffee", "Beverages", 60, 2, 12, 1, 0, 10, "ml", "1 cup", 150.0),
        listOf("Juice", "Beverages", 100, 1, 25, 0, 1, 5, "ml", "1 glass", 250.0),
        listOf("Soda", "Beverages", 120, 0, 30, 0, 0, 20, "ml", "1 glass", 250.0),
        listOf("Milkshake", "Beverages", 200, 6, 30, 6, 1, 100, "ml", "1 glass", 250.0),
        listOf("Lassi", "Beverages", 150, 5, 20, 5, 0, 80, "ml", "1 glass", 250.0)
    )
    val modifiersDrinks = listOf("Regular", "Large", "Small", "Diet", "Sugar-Free", "Iced", "Hot", "Flavored", "Special", "Extra Strong")

    var id_c = 1
    val out = StringBuilder("val DEFAULT_FOOD_DATABASE: List<FoodItem> = listOf(\n")

    fun addItems(bases: List<List<Any>>, modifiers: List<String>) {
        for (b in bases) {
            val name = b[0] as String
            val cat = b[1] as String
            val cal = (b[2] as Number).toDouble()
            val pro = (b[3] as Number).toDouble()
            val carb = (b[4] as Number).toDouble()
            val fat = (b[5] as Number).toDouble()
            val fib = (b[6] as Number).toDouble()
            val sod = (b[7] as Number).toDouble()
            val unit = b[8] as String
            val lbl = b[9] as String
            val wt = (b[10] as Number).toDouble()

            out.append(String.format("    FoodItem(\"f%d\", \"%s\", \"%s\", %.1f, %.1f, %.1f, %.1f, %.1f, %.1f, \"%s\", \"%s\", %.1f),\n", id_c, name, cat, cal, pro, carb, fat, fib, sod, unit, lbl, wt))
            id_c++

            for (mod in modifiers) {
                if (mod == "Plain" || mod == "Regular" || mod == "Classic") continue
                val v_name = "$mod $name"

                var mulCal = 1.0
                var mulPro = 1.0
                var mulCarb = 1.0
                var mulFat = 1.0
                var mulFib = 1.0
                var mulSod = 1.0

                when (mod) {
                    "Butter", "Cheese" -> { mulCal = 1.4; mulFat = 1.8; mulPro = 1.1 }
                    "Diet", "Sugar-Free" -> { mulCal = 0.7; mulCarb = 0.5; mulFat = 0.8; mulSod = 0.1 }
                    "Extra Large", "Large" -> { mulCal = 1.5; mulPro = 1.5; mulCarb = 1.5; mulFat = 1.5; mulFib = 1.5; mulSod = 1.5 }
                    "Small", "Mini" -> { mulCal = 0.6; mulPro = 0.6; mulCarb = 0.6; mulFat = 0.6; mulFib = 0.6; mulSod = 0.6 }
                    "Fried" -> { mulCal = 1.5; mulFat = 2.0 }
                    "Boiled", "Steamed" -> { mulCal = 0.9; mulFat = 0.5 }
                    "Spicy", "Masala" -> { mulSod = 1.5; mulCal = 1.1 }
                    "Sweet", "Extra Sweet" -> { mulCal = 1.3; mulCarb = 1.5; mulSod = 1.5 }
                    "Whole Wheat", "Multigrain" -> { mulFib = 2.0; mulPro = 1.2 }
                    "Low Fat" -> { mulFat = 0.3; mulCal = 0.8 }
                    else -> { mulCal = 1.1; mulPro = 1.1; mulCarb = 1.1; mulFat = 1.1 }
                }

                // add a slight random jitter of +/- 5% to avoid exact multiples looking artificial, but keep it consistent
                val jitter = { 0.95 + Random.nextDouble() * 0.1 }

                val v_cal = cal * mulCal * jitter()
                val v_pro = pro * mulPro * jitter()
                val v_carb = carb * mulCarb * jitter()
                val v_fat = fat * mulFat * jitter()
                val v_fib = fib * mulFib * jitter()
                val v_sod = sod * mulSod * jitter()

                out.append(String.format("    FoodItem(\"f%d\", \"%s\", \"%s\", %.1f, %.1f, %.1f, %.1f, %.1f, %.1f, \"%s\", \"%s\", %.1f),\n", id_c, v_name, cat, v_cal, v_pro, v_carb, v_fat, v_fib, v_sod, unit, lbl, wt))
                id_c++
            }
        }
    }

    addItems(baseGrains, modifiersGrains)
    addItems(baseProteins, modifiersProteins)
    addItems(baseVeg, modifiersVeg)
    addItems(baseSnacks, modifiersSnacks)
    addItems(baseSweets, modifiersSweets)
    addItems(baseDrinks, modifiersDrinks)

    out.append(")\n")
    File("app/generated_list.txt").writeText(out.toString())
    println("Generated $id_c items.")
}
