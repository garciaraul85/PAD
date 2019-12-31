package com.pad.feature.menu.models

import com.google.gson.annotations.SerializedName

data class Category(@SerializedName("name")val name: String, @SerializedName("icon")val icon: String, @SerializedName("products")val products: ArrayList<Product>)