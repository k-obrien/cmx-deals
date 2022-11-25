import cli.runCmxDeals
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
  runBlocking {
    runCmxDeals(args)
  }
}