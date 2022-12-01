package com.positronen.events.presentation.base

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import java.lang.reflect.ParameterizedType
import javax.inject.Inject

abstract class BaseActivity<VM : ViewModel> : AppCompatActivity() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val _viewModel by lazy {
        ViewModelProvider(
            this,
            viewModelFactory
        )[getViewModelClass()]
    }

    open val viewModel: VM
        get() = _viewModel

    @Suppress("UNCHECKED_CAST")
    private fun getViewModelClass(): Class<VM> {
        val type = (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments.toList().last()
        return type as Class<VM>
    }

    protected val baseCoroutineScope: LifecycleCoroutineScope
        get() = lifecycleScope
}