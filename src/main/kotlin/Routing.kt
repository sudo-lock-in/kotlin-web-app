package com.example

import com.google.zxing.BarcodeFormat
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.qrcode.QRCodeWriter
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap


val studentDb: ConcurrentHashMap<String, Student> = ConcurrentHashMap(
    mapOf(
        "S1001" to Student("S1001", "Alice Johnson", "Computer Science", 2),
        "S1002" to Student("S1002", "Bob Chen", "Mathematics", 1),
        "S1003" to Student("S1003", "Priya Singh", null, 3),
    )
)

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondResource("static/index.html")
        }

        get("/api/student/{id}") {
            val id = call.parameters["id"]
            val student = id?.let(studentDb::get)

            if (student == null) {
                call.respond(HttpStatusCode.NotFound, "Student not found")
                return@get
            }

            call.respond(student)
        }

        get("/generate-id") {
            val sid = call.request.queryParameters["sid"]
            if (sid.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Missing required query parameter: sid")
                return@get
            }

            val student = studentDb[sid]
            if (student == null) {
                call.respond(HttpStatusCode.NotFound, "Student not found")
                return@get
            }
            val studentWithDefault = student.copy(major = student.major ?: "Undecided")
            val payload = Json.encodeToString(studentWithDefault)
            val pngBytes = generateQrCode(payload)
            call.respondBytes(
                bytes = pngBytes,
                contentType = ContentType.Image.PNG,
                status = HttpStatusCode.OK,
            )

        }
    }
}

fun generateQrCode(content: String, size: Int = 300): ByteArray {
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
    val qrImage = MatrixToImageWriter.toBufferedImage(matrix)

    val outputStream = ByteArrayOutputStream()
    ImageIO.write(qrImage, "PNG", outputStream)
    return outputStream.toByteArray()
}


