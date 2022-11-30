package com.example.cse218_fp_exp1.ui.pin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PinViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "Pin"
    }
    val text: LiveData<String> = _text
}