package com.pad.feature.menu.data

import com.pad.feature.network.ApiWorker
import com.pad.feature.network.Constants
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory

object ApiService {
    private val TAG = "--ApiService"

    // get request builder
    fun apiCall() = Retrofit.Builder()
            .baseUrl(Constants.API_BASE_PATH)
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(ApiWorker.gsonConverter)
            .client(ApiWorker.client)
            .build()
            .create(MenuRequestInterface::class.java)!!
}