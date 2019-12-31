package com.pad.feature.menu.data

import com.pad.feature.menu.models.MenuResponse
import io.reactivex.Observable
import retrofit2.http.GET
import retrofit2.http.Headers

interface MenuRequestInterface {
    @Headers("Content-Type: application/json")
    @GET("sampleData/categories_menu.json")
    fun getMenu() : Observable<MenuResponse>
}