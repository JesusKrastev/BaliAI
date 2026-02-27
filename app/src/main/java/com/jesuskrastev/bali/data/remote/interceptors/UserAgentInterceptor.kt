package com.jesuskrastev.bali.data.remote.interceptors

import javax.inject.Inject
import okhttp3.Interceptor
import okhttp3.Response

class UserAgentInterceptor @Inject constructor() : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("User-Agent", "BaliDrivingApp/1.0 (contact: jesus@bali.com)")
            .build()

        return chain.proceed(request)
    }
}
