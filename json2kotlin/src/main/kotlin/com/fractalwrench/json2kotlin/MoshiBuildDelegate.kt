package com.fractalwrench.json2kotlin

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * A build delegate which alters the generated source code to include GSON annotations
 */
class MoshiBuildDelegate : SourceBuildDelegate {

    private val regex = "%".toRegex()

    override fun prepareProperty(
        propertyBuilder: PropertySpec.Builder,
        kotlinIdentifier: String,
        jsonKey: String,
        commonElements: List<TypedJsonElement>
    ) {
        if (kotlinIdentifier != jsonKey) {
            val serializedNameBuilder = AnnotationSpec.builder(Json::class)
            serializedNameBuilder.addMember("name = \"${jsonKey.replace(regex, "%%")}\"", "")
            propertyBuilder.addAnnotation(serializedNameBuilder.build())
        }
    }

    override fun prepareClass(
        classBuilder: TypeSpec.Builder,
        jsonElement: TypedJsonElement
    ) {
        classBuilder.addAnnotation(
            AnnotationSpec.builder(JsonClass::class)
                .addMember("generateAdapter = %L", true)
                .build()
        )
    }

}