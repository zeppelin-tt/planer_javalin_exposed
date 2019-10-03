package planer.domain

import com.fasterxml.jackson.annotation.JsonView
import org.jetbrains.exposed.dao.IntIdTable
import org.joda.time.DateTime
import planer.dto.views.NoteView

object Notes : IntIdTable("notes") {
    val title = varchar("title", 255)
    val done = bool("done").default(true)
    val dateTime = datetime("date_time")
}

@JsonView
data class Note(
    @JsonView(NoteView.ToFront::class)
    var id: Int? = null,
    @JsonView(NoteView.ToFront::class)
    var title: String = "",
    @JsonView(NoteView.ToFront::class)
    var done: Boolean = false,
    var dateTime: DateTime = DateTime.now()
)