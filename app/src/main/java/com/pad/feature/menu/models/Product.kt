package com.pad.feature.menu.models

import com.google.ar.core.Anchor
import com.google.gson.annotations.SerializedName

data class Product(@SerializedName("name")var textNames: String = "", @SerializedName("icon")var imagesPath: String = "", @SerializedName("url")var modelsName: String = "") {
    lateinit var anchor: Anchor
}