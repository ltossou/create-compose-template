package com.fractalwrench.json2kotlin

import com.google.gson.JsonElement
import com.squareup.kotlinpoet.*
import java.util.*

/**
 * Generates TypeSpec objects from a TypedJsonElement stack, which is processed in BFS reverse level order.
 */
internal class TypeSpecGenerator(
    private val delegate: SourceBuildDelegate,
    groupingStrategy: GroupingStrategy
) {

    private val jsonElementMap = HashMap<JsonElement, TypeSpec>()
    private val typeReducer = TypeReducer(JsonTypeDetector())
    private val jsonFieldGrouper = JsonFieldGrouper(groupingStrategy)

    /**
     * Processes JSON nodes in a reverse level order traversal,
     * by building class types for each level of the tree.
     *
     * The class types are returned as a Stack, sorted by level then alphabetically.
     */
    fun generateTypeSpecs(bfsStack: Stack<TypedJsonElement>): Stack<TypeSpec.Builder> {
        val typeSpecs = Stack<TypeSpec.Builder>()
        var level = -1
        val levelQueue = LinkedList<TypedJsonElement>()

        while (bfsStack.isNotEmpty()) {
            val pop = bfsStack.pop()

            if (level != -1 && pop.level != level) {
                processTreeLevel(levelQueue, typeSpecs)
            }
            levelQueue.add(pop)
            level = pop.level
        }
        processTreeLevel(levelQueue, typeSpecs)
        return typeSpecs
    }

    /**
     * Processes all the elements which belong to a single level in the tree. Each element is converted to a type,
     * sorted, then pushed to the stack.
     */
    private fun processTreeLevel(levelQueue: LinkedList<TypedJsonElement>, stack: Stack<TypeSpec.Builder>) {
        val fieldValues = levelQueue.filter { it.isJsonObject }.toMutableList()

        jsonFieldGrouper.groupCommonJsonObjects(fieldValues)
            .flatMap { convertJsonObjectsToTypes(it) }
            .sortedByDescending { it.build().name }
            .forEach { stack += it }
        levelQueue.clear()
    }

    /**
     * Converts a List of JSON elements which share common fields into a Kotlin type. Properties
     * are sorted alphabetically.
     */
    private fun convertJsonObjectsToTypes(commonElements: List<TypedJsonElement>): List<TypeSpec.Builder> {
        val fields = HashSet<String>()

        commonElements.forEach {
            fields.addAll(it.asJsonObject.keySet()) //.map { key -> key.decapitalize() })
        }

        commonElements.sortedBy {
            it.kotlinIdentifier.toLowerCase()
        }

        val classType = buildClass(commonElements, fields.sortedBy {
            it.toKotlinIdentifier().toLowerCase()
        })

        return commonElements.filterNot { // reuse any types which already exist in the map
            val containsValue = jsonElementMap.containsValue(classType.build())
            jsonElementMap.put(it.jsonElement, classType.build())
            containsValue
        }.map { classType }
    }

    /**
     * Builds a Kotlin class from a JSON element.
     */
    private fun buildClass(commonElements: List<TypedJsonElement>, fields: Collection<String>): TypeSpec.Builder {
        val identifier = commonElements.last().kotlinIdentifier.toCamelCase()
        val classBuilder = TypeSpec.classBuilder(identifier.capitalize())
        val constructor = FunSpec.constructorBuilder()

        if (fields.isNotEmpty()) {
            val fieldTypeMap = typeReducer.findDistinctTypes(fields, commonElements, jsonElementMap)
            fields.forEach {
                buildProperty(it, fieldTypeMap, commonElements, classBuilder, constructor)
            }
            classBuilder.addModifiers(KModifier.DATA) // non-empty classes allow data modifier
            classBuilder.primaryConstructor(constructor.build())
        }

        delegate.prepareClass(classBuilder, commonElements.last())
        return classBuilder
    }

    /**
     * Builds a Kotlin property from a JSON element.
     */
    private fun buildProperty(
        fieldKey: String,
        fieldTypeMap: Map<String, TypeName>,
        commonElements: List<TypedJsonElement>,
        classBuilder: TypeSpec.Builder,
        constructor: FunSpec.Builder
    ) {

        val kotlinIdentifier =
            fieldKey.toKotlinIdentifier().toCamelCase().decapitalize()
        val typeName = fieldTypeMap[fieldKey]
        val initializer =
            PropertySpec.builder(kotlinIdentifier, typeName!!).initializer(kotlinIdentifier)
        delegate.prepareProperty(initializer, kotlinIdentifier, fieldKey, commonElements)
        classBuilder.addProperty(initializer.build())
        constructor.addParameter(kotlinIdentifier, typeName)
    }

}

private fun String.toCamelCase(): String = split("_")
    .mapIndexed { index, s -> if (index > 0) s.capitalize() else s }
    .joinToString(separator = "")
