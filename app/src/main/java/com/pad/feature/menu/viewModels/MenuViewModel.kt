package com.pad.feature.menu.viewModels

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.pad.feature.menu.data.ApiService
import com.pad.feature.menu.models.Category
import com.pad.feature.menu.models.Product
import com.pad.util.UtilMethods
import com.skydoves.powermenu.PowerMenuItem
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class MenuViewModel(app: Application): AndroidViewModel(app) {

    private var categories: MutableList<PowerMenuItem> = mutableListOf()
    var categoriesList: MutableList<Category> = mutableListOf()

    private var productList: MutableList<Product> = mutableListOf()

    private val categoriesMutableLiveData = MutableLiveData<MutableList<PowerMenuItem>>()
    val categoriesLiveData: LiveData<MutableList<PowerMenuItem>>
        get() = categoriesMutableLiveData

    @SuppressLint("CheckResult")
    fun getArMenuCategories() {
        if (UtilMethods.isConnectedToInternet(this.getApplication())) {
            //UtilMethods.showLoading(this.getApplication())
            val observable = ApiService.apiCall().getMenu()
            observable.subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ userResponse ->
                            //UtilMethods.hideLoading()
                            println(userResponse.toString())
                            this.categoriesList = userResponse.categoriesList.toMutableList()
                            this.categoriesList.forEach { categories ->
                                this.categories.add(PowerMenuItem(categories.name, categories.icon))
                            }
                            this.categories.let {
                                this.categoriesMutableLiveData.value = this.categories
                            }
                            /** userResponse is response data class*/
                        }, { error ->
                            //UtilMethods.hideLoading()
                            UtilMethods.showLongToast(this.getApplication(), error.message.toString())
                        }
                    )
        } else {
            UtilMethods.showLongToast(this.getApplication(), "No Internet Connection!")
        }
    }

    fun getProductsFromCategory(position: Int): MutableList<Product> {
        this.productList.clear()
        this.categoriesList[position].products.forEach { product ->
            this.productList.add(Product(product.textNames, product.imagesPath, product.modelsName))
        }
        return this.productList
    }
}