package jp.winas.android.foundation.extension

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun <T> withMainContext(block: suspend CoroutineScope.() -> T): T = withContext(Dispatchers.Main, block)
suspend fun <T> withDefaultContext(block: suspend CoroutineScope.() -> T): T = withContext(Dispatchers.Default, block)