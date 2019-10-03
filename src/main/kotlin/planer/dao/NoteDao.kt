package planer.dao


import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import planer.domain.Note
import planer.domain.Notes
import planer.dto.CmdName.*
import planer.dto.Command
import planer.dto.ErrorCode
import planer.dto.addError
import java.sql.ResultSet


interface DaoInterface {
    fun init()
    fun destroy()
    fun create(note: Note): Command
    fun get(id: Int): Command
    fun getAll(): Command
    fun update(note: Note): Command
    fun toggle(note: Note): Command
    fun delete(id: Int): Command
    fun deleteAll(): Command
    fun countByDone(isDone: Boolean): Int
    fun getOrderedAll(): Command
}

class NoteDao : DaoInterface {

    override fun init() = transaction {
        addLogger(StdOutSqlLogger)
        SchemaUtils.createMissingTablesAndColumns(Notes)
    }

    override fun destroy() = transaction { SchemaUtils.drop(Notes) }

    override fun create(note: Note) = transaction {
        note.also {
            it.id = Notes.insertAndGetId(note.toNotes()).value
        }.toCommand(CREATE.name).apply {
            unfinishedCount = countByDone(false)
        }
    }

    override fun get(id: Int) = transaction {
        rowToNote(Notes.select { Notes.id.eq(id) }.single()).toCommand("getPropMap")
    }

    override fun getAll() = transaction {
        Command(cmd = GET_ALL.name, notes = Notes.selectAll().map { rowToNote(it) })
    }

    override fun update(note: Note) = transaction {
        if (isExist(note.id!!)) {
            Notes.update({ Notes.id eq note.id }) {
                it[title] = note.title
                it[done] = note.done
                it[dateTime] = DateTime.now()
            }
            return@transaction Command(cmd = UPDATE.name, note = note, unfinishedCount = countByDone(false))
        }
        getOrderedAll().addError(ErrorCode.NOTE_NOT_FOUND)
    }

    override fun toggle(note: Note) = transaction {
        val noteToUpdate = note.apply { done = !done; dateTime = DateTime.now() }
        if (isExist(note.id!!)) {
            Notes.update({ Notes.id eq note.id }) {
                it[done] = noteToUpdate.done
                it[dateTime] = noteToUpdate.dateTime
            }
            return@transaction Command(cmd = TOGGLE.name, note = noteToUpdate, unfinishedCount = countByDone(false))
        }
        getOrderedAll().addError(ErrorCode.NOTE_NOT_FOUND)
    }

    override fun deleteAll() = transaction { Command(DELETE_ALL.name).apply { Notes.deleteAll() } }

    override fun delete(id: Int) = transaction {
        if (isExist(id))
            return@transaction Command(cmd = DELETE.name, id = id).apply { Notes.deleteWhere { Notes.id eq id } }
        getOrderedAll().addError(ErrorCode.NOTE_NOT_FOUND)
    }

    override fun countByDone(isDone: Boolean) = transaction {
        Notes.select { Notes.done.eq(isDone) }.count()
    }

    override fun getOrderedAll() = transaction {
        val notes = """select * from((select * from notes where not done order by date_time) 
            union all (select * from notes where done order by date_time desc)) sel""".execAndMap { rs ->
            Note(
                id = rs.getInt("id"),
                title = rs.getString("title"),
                done = rs.getBoolean("done"),
                dateTime = DateTime(rs.getTimestamp("date_time"))
            )
        }
        Command(cmd = GET_ALL.name, notes = notes)
    }

    private fun <T : Any> String.execAndMap(transform: (ResultSet) -> T): List<T> {
        val result = arrayListOf<T>()
        TransactionManager.current().exec(this) { rs ->
            while (rs.next()) {
                result += transform(rs)
            }
        }
        return result
    }

    private fun isExist(id: Int) = transaction {
        Notes.select { Notes.id.eq(id) }.count() != 0
    }

    private fun Note.toNotes(): Notes.(UpdateBuilder<*>) -> Unit = {
        it[title] = this@toNotes.title
        it[done] = this@toNotes.done
        it[dateTime] = this@toNotes.dateTime
    }

    private fun rowToNote(row: ResultRow) =
        Note(row[Notes.id].value, row[Notes.title], row[Notes.done], row[Notes.dateTime])

    private fun Note.toCommand(cmd: String) = Command(cmd = cmd, note = this)

}
