
package com.primal.runs.data.network
class NetworkError(val errorCode: Int, override val message: String?) : Throwable(message)
