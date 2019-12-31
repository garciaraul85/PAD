package com.pad.feature.menu.models

import com.google.gson.annotations.SerializedName

data class MenuResponse(
        @SerializedName("category")
        var categoriesList: List<Category>
)