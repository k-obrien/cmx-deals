package cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.int
import kotlinx.coroutines.runBlocking

class CmxDealsCommand : CliktCommand() {

  private val wishlistID: String by option(help = "ID of a public Amazon wishlist").required()
  private val minimumDiscount: Int by option(help = "Minimum discount to report").int().default(50)

  override fun run() {
    runBlocking {
      runCmxDeals(wishlistID, minimumDiscount)
    }
  }
}