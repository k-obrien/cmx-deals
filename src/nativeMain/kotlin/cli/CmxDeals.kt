package cli

import io.httpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.path
import io.ktor.utils.io.errors.IOException
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay

private val httpClient = httpClient()

private val PRODUCT_ID_REGEX = """ASIN:(.+)\|""".toRegex()
private val LAST_EVALUATED_KEY_REGEX = """"lastEvaluatedKey":"(.*?)"""".toRegex()

suspend fun runCmxDeals(wishlistID: String, minDiscount: Int) {
  val productIDs = getProductIDs(wishlistID)
  var processedProducts = 0
  val productCount = productIDs.getOrNull()?.size ?: 0
  productIDs
    .onSuccess {
      it
        .forEach { productID ->
          hangAbout()
          getProductDetails(productID)
            .onSuccess { productDetails ->
              if (productDetails.discount >= minDiscount) {
                println("$productDetails\n")
              }
            }
            .onFailure { throwable ->
              println("${throwable.message}")
              if (throwable is IOException) return@onSuccess
              println()
            }
          processedProducts = ++processedProducts
        }
    }
    .onFailure { println("${it.message}") }
  println("Processed $processedProducts of $productCount")
}

private suspend fun getProductIDs(wishlistID: String): Result<List<String>> = runCatching {
  buildList {
    var lastEvaluatedKey = ""
    do {
      hangAbout()
      val wishlist = getWishlist(wishlistID, lastEvaluatedKey).getOrThrow()
      PRODUCT_ID_REGEX
        .findAll(wishlist)
        .map { match -> match.groupValues[1] }
        .also { addAll(it) }
      lastEvaluatedKey = LAST_EVALUATED_KEY_REGEX.find(wishlist)?.groupValues?.get(1).orEmpty()
    } while (lastEvaluatedKey.isNotBlank())
  }
}

private const val WISHLIST_PATH = "registry/wishlist/"
private const val WISHLIST_QUERY_LAST_EVALUATED_KEY = "lek"

private suspend fun getWishlist(
  wishlistID: String,
  lastEvaluatedKey: String
): Result<String> = runCatching {
  httpClient
    .get {
      url {
        path(WISHLIST_PATH + wishlistID)
        parameter(WISHLIST_QUERY_LAST_EVALUATED_KEY, lastEvaluatedKey)
      }
    }
    .bodyAsText()
    .apply { validateForCaptcha() }
}

private val PRODUCT_TITLE_REGEX = """<span id="productTitle".*?>\s*(.+?)\s*</span>""".toRegex()
private val DIGITAL_LIST_PRICE_REGEX = """<td id="digital-list-price".*?>\s*\$(.+?)\s*</td>""".toRegex()
private val KINDLE_PRICE_REGEX = """<span id="kindle-price".*?>\s*\$(.+?)\s*</span>""".toRegex()
private val PAGE_COUNT_REGEX = """<span>(\d+ pages)</span>""".toRegex()

private suspend fun getProductDetails(productID: String): Result<ProductDetails> = getProductPage(productID)
  .mapCatching { (productPageUrl, productPage) ->
    val title = PRODUCT_TITLE_REGEX.find(productPage)?.groupValues?.get(1)
    requireNotNull(title) { "Product has no title: $productPageUrl" }

    val listPrice = DIGITAL_LIST_PRICE_REGEX.find(productPage)?.groupValues?.get(1)?.toDoubleOrNull()
    val kindlePrice = KINDLE_PRICE_REGEX.find(productPage)?.groupValues?.get(1)?.toDoubleOrNull()
    requireNotNull(listPrice ?: kindlePrice) { "Product has no price: $productPageUrl" }

    val pages = PAGE_COUNT_REGEX.find(productPage)?.groupValues?.get(1)
    ProductDetails(
      title = title,
      url = productPageUrl,
      listPrice = listPrice ?: kindlePrice!!,
      price = kindlePrice ?: listPrice!!,
      pages = pages
    )
  }

private data class ProductDetails(
  val title: String,
  val url: String,
  val listPrice: Double,
  val price: Double,
  val pages: String?
) {

  override fun toString(): String = "$title\n$$price (-$discount%) ${pages?.let { "—— $it" }.orEmpty()}\n$url"

  val discount: Int
    get() = floor(((listPrice - price) / listPrice) * 100).roundToInt()
}

private const val PRODUCT_PATH = "dp/"

private suspend fun getProductPage(productID: String): Result<Pair<String, String>> = runCatching {
  httpClient
    .get {
      url {
        path(PRODUCT_PATH + productID)
      }
    }
    .run { request.url.toString() to bodyAsText() }
    .apply { second.validateForCaptcha() }
}

private val CAPTCHA_PAGE_REGEX = """we just need to make sure you're not a robot""".toRegex()

private fun String.validateForCaptcha() {
  if (CAPTCHA_PAGE_REGEX.containsMatchIn(this)) {
    throw IOException("Encountered captcha challenge.")
  }
}

private suspend fun hangAbout() = delay((30..60).random().seconds)