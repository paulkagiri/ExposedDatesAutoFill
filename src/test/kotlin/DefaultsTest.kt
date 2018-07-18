package com.list

import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.*

object Animals : IntIdTable("Animals") {
    val name = varchar("name", length = 60)
    val createdAt = long("timestamp").clientDefault { System.currentTimeMillis() }
}

class Animal(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Animal>(Animals)

    var name by Animals.name
    val createdAt by Animals.createdAt
}


class DefaultsTest {

    companion object {

        @BeforeClass
        @JvmStatic
        fun setup() {
            // init db
            val dataSource = HikariDataSource()
            dataSource.jdbcUrl = "jdbc:postgresql://localhost:5432/testdb?ssl=false"
            dataSource.username = "test"
            dataSource.password = "testpassword"
            Database.connect(dataSource)
        }
    }

    @Before
    fun prepareTest() {
        transact {
            SchemaUtils.create(Animals)
        }
    }

    @After
    fun cleanupTest() {
        transact {
            SchemaUtils.drop(Animals)
        }
    }

    @Test
    fun testThatDefaultValuesSavedAreOk() {
        val timeB4Transaction = System.currentTimeMillis()

        val id = transaction {
            val animal = Animal.new {
                name = "Tom"
            }
            return@transaction animal.id
        }

        val animal = transact {
            Animal[id]
        }

        println("timeB4Transaction: $timeB4Transaction, createdAt: ${animal.createdAt}")
        Assert.assertTrue(animal.createdAt > timeB4Transaction)
    }
}