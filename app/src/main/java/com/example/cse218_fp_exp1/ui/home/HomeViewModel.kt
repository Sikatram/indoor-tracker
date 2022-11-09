package com.example.cse218_fp_exp1.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "MicroSleep Home Page"
    }
    val text: LiveData<String> = _text
}