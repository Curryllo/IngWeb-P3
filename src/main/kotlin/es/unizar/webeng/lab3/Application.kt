package es.unizar.webeng.lab3

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching

@SpringBootApplication
@EnableCaching
class Application

fun main(vararg args: String) {
    runApplication<Application>(*args)
}
