package api

import model.Api
import JsonConverter
import di.PackageProvider
import common.ResourceFileReader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MapperGeneratorTest {

    lateinit var generator: MapperGenerator
    private val fileReader = ResourceFileReader()

    @BeforeEach
    fun setUp() {
        generator = MapperGenerator(PackageProvider("com.ltossou"))
    }

    @Test
    fun generate() {
        assertEquals(
            """
            package com.ltossou.data.local.base

            abstract class Mapper<From, Entity> {
                abstract fun mapToEntity(from: From): Entity

                fun mapToEntity(froms: List<From>): List<Entity> {
                    return froms.map { mapToEntity(it) }
                }

                abstract fun mapFromEntity(to: Entity): From
            }
        """.trimIndent(), generator.generateBaseMapper().build()
        )
    }

    @Test
    fun generateFromModel() {
        val json = fileReader.readContents("json/wmata.stations.json")
        val api = Api(
            name = "metro",
            baseUrl = "api.wmata.com",
            endpoints = listOf(
                Api.Endpoint(method = Api.Endpoint.Method.GET, url = "/stations", json = json)
            ),
            authentication = Api.AuthType.Header(key = "appId", "abcde-key")
        )
        val modelResult = JsonConverter().generate(
            input = api.endpoints[0].json,
            rootClassName = "${api.endpoints.first().name.capitalize()}Response",
            packageName = "com.ltossou"
        )
        println(
            generator.generate(
                api = api,
                responseModels = mapOf(api.endpoints.first().name to modelResult),
                entities = emptyList()
            )
        )
    }
}