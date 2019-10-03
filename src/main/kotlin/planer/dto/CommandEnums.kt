package planer.dto

enum class ErrorCode(val code: Int, val defaultMessage: String) {
    INTERNAL(1, "An internal error occurred"),
    BAD_REQUEST(2, "Incorrect data specified"),
    NOTE_NOT_FOUND(3, "Item was not found"),
    FORBIDDEN(4, "Access is denied");
}

enum class CmdName {
    GET_ALL, CREATE, UPDATE, TOGGLE, DELETE, DELETE_ALL, ERROR
}

fun Command.addError(err: ErrorCode) = this.apply {
    cmd = CmdName.ERROR.name
    errorCode = err.code
    errorDescription = err.defaultMessage
}
