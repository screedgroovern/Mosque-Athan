import com.groovernapp.mosqueathan.server.WebPageJs
import java.io.File

fun main() {
    File("test_full_output.js").writeText(WebPageJs.getJs())
}
