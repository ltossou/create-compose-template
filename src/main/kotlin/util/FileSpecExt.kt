package util

import com.squareup.kotlinpoet.*

fun FileSpec.Builder.println(): FileSpec.Builder {
    this.build().println()
    return this
}

fun FileSpec.println(): FileSpec {
    return also {
        it.writeTo(System.out)
    }
}

fun FileSpec.getTypeSpecs(): List<TypeSpec> {
    val mainClass: TypeSpec =
        members.find { it is TypeSpec && it.kind == TypeSpec.Kind.CLASS } as TypeSpec
    return mutableListOf(mainClass).apply { addAll(mainClass.typeSpecs.filter { it.kind == TypeSpec.Kind.CLASS }) }
        .toList()
}

fun TypeSpec.Builder.addPropertyParameters(
    constructorBuilder: FunSpec.Builder = FunSpec.constructorBuilder(),
    propertysSpec: List<PropertySpec>
): TypeSpec.Builder {
    return this.primaryConstructor(
        constructorBuilder
            .addParameters(propertysSpec.map { ParameterSpec.builder(it.name, it.type).build() })
            .build()
    ).addProperties(propertysSpec.map { it.toBuilder().initializer(it.name).build() })
}

fun TypeSpec.printProperties(): String = propertySpecs.joinToString { it.name }