package eu.kanade.tachiyomi.util

import kotlinx.coroutines.suspendCancellableCoroutine
import rx.Observable
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun <T> Observable<T>.awaitSingle(): T = suspendCancellableCoroutine { cont ->
    val sub = take(1).subscribe(
        { value -> if (cont.isActive) cont.resume(value) },
        { error -> if (cont.isActive) cont.resumeWithException(error) },
        {
            if (cont.isActive) cont.resumeWithException(IllegalStateException("Observable completed without a value"))
        },
    )
    cont.invokeOnCancellation { sub.unsubscribe() }
}
