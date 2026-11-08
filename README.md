# EMC Crafting (NeoForge 1.21.1)
Зависимости: ProjectE, JEI.
Использование: открой верстак -> в JEI нажми "+" у рецепта.
Ингредиенты берутся из инвентаря, а чего нет - создаётся из ИЗУЧЕННЫХ в столе трансмутации предметов за EMC.
Сборка: поставь fileId ProjectE в build.gradle, затем `./gradlew build` (нужен gradle wrapper: `gradle wrapper`).
Если какие-то методы API ProjectE не совпали с твоей версией - поправь импорты в EmcGridFiller.java.
