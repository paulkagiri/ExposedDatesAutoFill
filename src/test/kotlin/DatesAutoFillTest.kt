package com.list

import app.*
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import org.junit.*
import org.junit.Assert.assertTrue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Random


const val userName = "Test"

class DatesAutoFillTest {

    companion object {

        @BeforeClass
        @JvmStatic
        fun setup() {
            // init db
            val dataSource = HikariDataSource()
            dataSource.jdbcUrl = "jdbc:postgresql://localhost:5432/exposed_auto_fill_dates?ssl=false"
            dataSource.username = "test"
            dataSource.password = "test"
            Database.connect(dataSource)
        }
    }

    @Before
    fun prepareTest() {
        // create the user table
        transact {
            SchemaUtils.create(Users)
            SchemaUtils.create(Roles)
        }
    }

    @After
    fun cleanupTest() {
        //drop the user table
        transact {
            SchemaUtils.drop(Users)
            SchemaUtils.drop(Roles)
        }
    }

    @Test
    fun testThatInsertsShouldAutoFillCreatedAtAndNotUpdatedAt() {
        val dateBeforeTransaction = currentUtc()

        val userId = transact {
            // create a new user
            val user = User.new {
                name = userName
            }
            return@transact user.id
        }

        // query the created user
        val user = transact {
            User[userId]
        }

        assertTrue(user.createdAt != null && user.createdAt!! > dateBeforeTransaction)
        assertTrue(user.updatedAt == null)
    }

    @Test
    fun testThatUpdatesShouldAutoFillUpdatedAtAndNotCreatedAt() {
        transact {
            // create a new user
            User.new {
                name = userName
            }
        }

        // relax
        Thread.sleep(2000)

        val dateBeforeTransaction = currentUtc()

        val (userId, createdAtBefore) = transact {
            val userRow = Users.select { Users.name eq userName }.first()
            val userId = userRow[Users.id]
            val createdAt = userRow[Users.createdAt]

            assertTrue(createdAt != null)
            assertTrue(userRow[Users.updatedAt] == null)

            // do an update
            val user = User[userId]
            user.name = "Real"

            return@transact Pair(userId, createdAt)
        }

        // query the created user
        val user = transact {
            User[userId]
        }

        assertTrue(user.createdAt == createdAtBefore)
        assertTrue(user.updatedAt != null && user.updatedAt!! > dateBeforeTransaction)
    }

    @Test
    fun testThatMultipleInsertsDoNotAffectPreviousRecordsDates() {
        //insert multiple users in different transacts
        val usersCreatedTimes = mutableListOf<Pair<Int, DateTime?>>()
        val random = Random()
        for (i in 1..5) {
            val user = transact {
                val nameOfUser = "$userName-$i"

                println("\nInserting user $nameOfUser at ${dateFormat.format(Date())}")

                val user = User.new {
                    name = nameOfUser
                }

                Thread.sleep((1000 + random.nextInt(1000)).toLong())

                return@transact user
            }

            usersCreatedTimes.add(Pair(user.idValue, user.createdAt))
        }

        //check that the dates are as originally recorded
        usersCreatedTimes.forEach {
            // get the user from the database
            val user = transact {
                User[it.first]
            }

            println("\ncreatedAt first entered as ${it.second} and now is ${user.createdAt}")
            assertTrue(it.second == user.createdAt)
        }
    }

    @Test
    fun testThatInsertingOrUpdatingUsersDoesNotAffectRoles() {
        // create a role
        val originalRole = transact {
            Role.new {
                name = "Admin"
            }
        }

        val roleId = originalRole.id
        val roleCreatedAt = originalRole.createdAt

        //create some users
        val random = Random()
        for (i in 1..5) {
            transact {
                val user = User.new {
                    name = "$userName-$i"
                }

                Thread.sleep((1000 + random.nextInt(1000)).toLong())

                return@transact user
            }
        }

        // query the role created earlier
        val role = transact {
            Role[roleId]
        }

        assertTrue(role.createdAt == roleCreatedAt)
        assertTrue(role.updatedAt == null)
    }
}

var dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")

fun <T> transact(statement: Transaction.() -> T): T {
    return transaction {
        logger.addLogger(StdOutSqlLogger)
        statement()
    }
}
