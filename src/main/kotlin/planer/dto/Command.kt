package planer.dto

import planer.domain.Note

data class Command(

    var cmd: String = "",
    var id: Int? = null,
    var note: Note? = null,
    var notes: List<Note>? = null,
    var unfinishedCount: Int? = null,
    var errorCode: Int? = null,
    var errorDescription: String? = null

)
