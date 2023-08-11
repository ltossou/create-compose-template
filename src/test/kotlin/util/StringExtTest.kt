package util

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

class StringExtTest {

    @Test
    fun toSnakeCase() {
        assertEquals("this_is_a_snake", "thisIsAsSnake".toSnakeCase())
        assertEquals("this_is_a_snake", "ThisIsAsSnake".toSnakeCase())
    }

    @Test
    fun toCamelCase() {
        assertEquals("thisIsAsSnake", "this_is_a_snake".toCamelCase())
    }
}