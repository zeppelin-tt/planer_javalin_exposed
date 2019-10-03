package planer.handler

import com.fasterxml.jackson.module.kotlin.readValue
import io.javalin.Javalin
import io.javalin.Javalin.log
import io.javalin.websocket.WsCloseContext
import io.javalin.websocket.WsConnectContext
import io.javalin.websocket.WsContext
import io.javalin.websocket.WsMessageContext
import org.jetbrains.exposed.exceptions.ExposedSQLException
import planer.dao.NoteDao
import planer.dto.CmdName
import planer.dto.Command
import planer.dto.ErrorCode
import planer.dto.addError
import planer.dto.views.NoteView
import planer.util.*
import java.util.concurrent.ConcurrentHashMap

class CommandSocketHandler {

    private val noteRepository = NoteDao()
    private val connectionUserMap = ConcurrentHashMap<WsContext, Int>()
    private var nextUserNumber = 1

    fun addHandler(connect: Javalin) =
        connect.ws("/endpoint") { ws ->
            ws.onConnect { onConnectEvent(it) }
            ws.onClose { onCloseEvent(it) }
            ws.onMessage { onMessageEvent(it) }
            ws.onError {
                log.error("Error detected [sessionId = ${it.sessionId}], [session = ${it.session}]: ${it.error().toString()}")
            }
        } ?: throw Exception("Internal error").fillInStackTrace()

    private fun onConnectEvent(ctx: WsConnectContext) {
        nextUserNumber++.also {
            log.info("user $it was connected")
            connectionUserMap[ctx] = it
        }
        withNoteView(noteRepository.getOrderedAll()).also {
            ctx.send(it)
            log.info("sent all notes: $it")
        }
    }

    private fun onCloseEvent(ctx: WsCloseContext) {
        log.info("user ${connectionUserMap[ctx]} is gone")
        connectionUserMap.remove(ctx)
    }

    private fun onMessageEvent(ctx: WsMessageContext) {
        log.info("user ${connectionUserMap[ctx]} send command")
        log.info("income command: ${ctx.message()}")
        try {
            runAndCallback(globalObjectMapper.readValue(ctx.message())).apply {
                val commandStr = withNoteView(this)
                if (CmdName.valueOf(this.cmd) == CmdName.ERROR) {
                    ctx.send(commandStr)
                } else {
                    connectionUserMap.keys
                        .filter { it.session.isOpen }
                        .forEach { it.send(commandStr) }
                }
                log.info("outcome command: $commandStr")
            }
        } catch (ex: ExposedSQLException) {
            ctx.send(withNoteView(Command().addError(ErrorCode.INTERNAL)))
        }
    }

    private fun runAndCallback(command: Command) = when (CmdName.valueOf(command.cmd)) {
        CmdName.GET_ALL -> noteRepository.getOrderedAll()
        CmdName.CREATE ->
            if (isValidToCreate(command)) noteRepository.create(command.note!!) else sendBadRequestError()
        CmdName.UPDATE ->
            if (isValidToUpdate(command)) noteRepository.update(command.note!!) else sendBadRequestError()
        CmdName.TOGGLE ->
            if (isValidToToggle(command)) noteRepository.toggle(command.note!!) else sendBadRequestError()
        CmdName.DELETE ->
            if (isValidToDelete(command)) noteRepository.delete(command.id!!) else sendBadRequestError()
        CmdName.DELETE_ALL -> noteRepository.deleteAll()
        else -> {
            log.warn("Command ${command.cmd} not exist")
            sendBadRequestError()
        }
    }

    private fun sendBadRequestError() = Command().addError(ErrorCode.BAD_REQUEST)

    private fun withNoteView(obj: Any) = globalObjectMapper
        .writerWithView(NoteView.ToFront::class.java)
        .writeValueAsString(obj)

}
