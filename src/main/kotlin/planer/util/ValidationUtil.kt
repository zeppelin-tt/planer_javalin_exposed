package planer.util

import io.javalin.Javalin.log
import planer.dto.Command

class Validation {

    private var valid = true

    fun checkNull(param: Any?, paramName: String): Validation {
        if (param == null) {
            log.warn("parameter $paramName is null")
            this.valid = false
        }
        return this
    }

    fun checkEmptyString(param: String?, paramName: String): Validation {
        if (param != null && param.isEmpty()) {
            log.warn("string parameter $paramName is empty")
            this.valid = false
        }
        return this
    }

    fun isValid() = this.valid

}

fun isValidToCreate(command: Command): Boolean {
    return if (Validation().checkNull(command.note, "note").isValid())
        Validation()
            .checkNull(command.note!!.title, "title")
            .checkEmptyString(command.note!!.title, "title")
            .isValid()
    else false
}

fun isValidToUpdate(command: Command): Boolean {
    return if (Validation().checkNull(command.note, "note").isValid())
        Validation()
            .checkNull(command.note!!.title, "Note.title")
            .checkEmptyString(command.note!!.title, "Note.title")
            .checkNull(command.note!!.id, "Note.id")
            .isValid()
    else false
}

fun isValidToToggle(command: Command): Boolean {
    return if (Validation().checkNull(command.note, "note").isValid())
        Validation()
            .checkNull(command.note!!.title, "Note.title")
            .checkEmptyString(command.note!!.title, "Note.title")
            .checkNull(command.note!!.id, "Note.id")
            .checkNull(command.note!!.done, "Note.done")
            .isValid()
    else false
}

fun isValidToDelete(command: Command) = Validation().checkNull(command.id, "id").isValid()
