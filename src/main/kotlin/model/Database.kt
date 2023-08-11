package model

import util.Color
import util.InputUtils
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import di.PackageProvider
import local.DatabaseGenerator
import util.getTypeSpecs
import util.printProperties
import util.toSnakeCase
import java.nio.file.Path

data class Database(
    val name: String = "app",
    val entities: List<Entity> = emptyList()
) {

    companion object {
        fun input(modelsSpecs: List<FileSpec> = emptyList()): Database {
            println("\n🗃 Configure database...")
            val name = InputUtils.promptString("Enter Database name (app.db by default)", false, default = "app")
            val entities = mutableListOf<Entity>()
            var addEntity = true
            if (modelsSpecs.isNotEmpty()) {
                do {
                    modelsSpecs.forEach { modelSpec ->
                        val selectedModels = askSelectModels(modelSpec)
                        val tempEntities = askRenameEntities(selectedModels)
                        entities.addAll(tempEntities)
                    }
                    addEntity = InputUtils.getBoolean("Add another entity?", default = false)
                    if (addEntity) {
                        entities.add(Entity.input())
                    }
                } while (addEntity)
            }
            return Database(name = name, entities = entities)
        }

        private fun askSelectModels(modelSpec: FileSpec): List<TypeSpec> {
            val models = modelSpec.getTypeSpecs()
            util.println(Color.YELLOW, "Select the model to convert to a db entity")
            println("0. None/Skip")
            models.forEachIndexed { index, model ->
                println("${index + 1}. ${model.name}(" + model.printProperties() + ")")
            }
            val options = InputUtils.promptString("Type options (separated by space)", true).split(" ")
                .mapNotNull { it.trim().toIntOrNull() }
                .filter { it >= 0 && it <= models.size + 1 }
            return if (options.contains(0)) emptyList() else options.map { models[it - 1] }
        }

        private fun askRenameEntities(
            models: List<TypeSpec>,
        ): List<Entity> {
            val tempEntities = models.map {
                Entity(
                    name = it.name.orEmpty(),
                    model = it,
                    primaryKey = "id"
                )
            }.toMutableList()
            tempEntities.forEachIndexed { index, entity ->
                val nameInput = InputUtils.promptString(
                    "Rename ${entity.name} (type singular text)? (Press Enter to skip)",
                    false,
                    default = entity.name
                )
                val propertyNames = entity.model!!.propertySpecs.joinToString { it.name }
                val primaryKey = InputUtils.promptString(
                    "Primary key? (\"id\" by default). Properties: $propertyNames",
                    false,
                    default = "id"
                )
                val autogeneratePrimaryKey: Boolean = if (!propertyNames.contains(primaryKey)) {
                    InputUtils.getBoolean(
                        "Add autogenerated primary key \"$primaryKey\"? (Yes by default)",
                        default = true
                    )
                } else {
                    false
                }
                val tableName = InputUtils.promptString(
                    "Rename db table name \"${nameInput.toLowerCase().toSnakeCase()}s\"? (Press Enter to skip)",
                    false,
                    default = nameInput.toLowerCase().toSnakeCase() + "s"
                )
                tempEntities[index] =
                    entity.copy(
                        name = nameInput,
                        plural = nameInput + "s",
                        primaryKey = primaryKey,
                        autogeneratePrimaryKey = autogeneratePrimaryKey,
                        tableName = tableName
                    )
            }
            return tempEntities.toList()
        }

    }

}

fun main(args: Array<String>) {
    println(
        DatabaseGenerator(
            packageProvider = PackageProvider("com.ltossou.app"),
            projectPath = Path.of("C:\\Users\\me\\MyProject")
        )
            .generate(
                Database(
                    name = "app",
                    entities = listOf(
                        Entity(name = "study", plural = "studies", primaryKey = "id")
                    )
                )
            )
    )
}