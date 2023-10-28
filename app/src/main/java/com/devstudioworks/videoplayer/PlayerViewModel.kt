package com.devstudioworks.videoplayer

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 * @Author: Kavin
 * @Date: 28/10/23
 */
class PlayerViewModel : ViewModel() {

    var selectedVideoUri: MutableLiveData<Uri?> = MutableLiveData(null)
}