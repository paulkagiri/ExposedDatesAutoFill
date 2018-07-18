package app

import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.sql.ReferenceOption
import org.joda.time.DateTime
import org.joda.time.DateTimeZone

fun currentUtc(): DateTime = DateTime.now(DateTimeZone.UTC)

fun DateTime.toUtc(): DateTime = this.toDateTime(DateTimeZone.UTC)

abstract class BaseIntIdTable(name: String) : IntIdTable(name) {
    val createdAt = datetime("createdAt").nullable()
    val updatedAt = datetime("updatedAt").nullable()
}

abstract class BaseIntEntity(id: EntityID<Int>, table: BaseIntIdTable) : IntEntity(id) {
    var createdAt by table.createdAt
    var updatedAt by table.updatedAt
}

abstract class BaseIntEntityClass<E : BaseIntEntity>(table: BaseIntIdTable) : IntEntityClass<E>(table) {

    init {
        EntityHook.subscribe { action ->
            if (action.changeType == EntityChangeType.Created) {
                try {
                    action.toEntity(this)?.createdAt = currentUtc()
                } catch (e: Exception) {
                    //nothing much to do here
                }
            }
        }

        EntityHook.subscribe { action ->
            if (action.changeType == EntityChangeType.Updated) {
                try {
                    action.toEntity(this)?.updatedAt = currentUtc()
                } catch (e: Exception) {
                    //nothing much to do here
                }
            }
        }
    }
}

val BaseIntEntity.idValue: Int
    get() = this.id.value

object Users : BaseIntIdTable("users") {
    val name = varchar("name", length = 60)
    val role = reference("roleId", Roles, onDelete = ReferenceOption.NO_ACTION).nullable()
}

class User(id: EntityID<Int>) : BaseIntEntity(id, Users) {
    companion object : BaseIntEntityClass<User>(Users)

    var name by Users.name
    var role by Role optionalReferencedOn Users.role
}

object Roles : BaseIntIdTable("roles") {
    val name = varchar("name", length = 60)
}

class Role(id: EntityID<Int>) : BaseIntEntity(id, Roles) {
    companion object : BaseIntEntityClass<Role>(Roles)

    var name by Roles.name
}