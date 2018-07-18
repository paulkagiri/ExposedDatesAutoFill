package app

import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.sql.ReferenceOption
import org.joda.time.DateTime
import org.joda.time.DateTimeZone

fun currentUtc(): DateTime = DateTime.now(DateTimeZone.UTC)

open class BaseEntity(id: EntityID<Int>) : IntEntity(id) {

    open var createdAt: DateTime? = null
    open var updatedAt: DateTime? = null

    init {
        EntityHook.subscribe { action ->
            if (action.changeType == EntityChangeType.Created) {
                try {
                    val time = currentUtc()
                    println("Setting createdAt for $idValue to $time")
                    createdAt = time
                } catch (e: Exception) {
                    //nothing much to do here
                }
            }
        }

        EntityHook.subscribe { action ->
            if (action.changeType == EntityChangeType.Updated) {
                try {
                    val time = currentUtc()
                    println("Setting updatedAt for $idValue to $time")
                    updatedAt = time
                } catch (e: Exception) {
                    //nothing much to do here
                }
            }
        }
    }
}

val BaseEntity.idValue: Int
    get() = this.id.value


object Users : IntIdTable("users") {
    val name = varchar("name", length = 60)
    val role = reference("roleId", Roles, onDelete = ReferenceOption.NO_ACTION).nullable()
    val createdAt = datetime("createdAt").nullable()
    val updatedAt = datetime("updatedAt").nullable()
}

class User(id: EntityID<Int>) : BaseEntity(id) {
    companion object : IntEntityClass<User>(Users)

    var name by Users.name
    var role by Role optionalReferencedOn Users.role
    override var createdAt by Users.createdAt
    override var updatedAt by Users.updatedAt
}

object Roles : IntIdTable("roles") {
    val name = varchar("name", length = 60)
    val createdAt = datetime("createdAt").nullable()
    val updatedAt = datetime("updatedAt").nullable()
}

class Role(id: EntityID<Int>) : BaseEntity(id) {
    companion object : IntEntityClass<Role>(Roles)

    var name by Roles.name
    override var createdAt by Roles.createdAt
    override var updatedAt by Roles.updatedAt
}