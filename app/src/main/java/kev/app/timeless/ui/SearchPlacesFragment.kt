package kev.app.timeless.ui

import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer

class SearchPlacesFragment : AppCompatActivity() {
    lateinit var locationObserver : Observer<Result<Location>>
    lateinit var location: MutableLiveData<Result<Location>>

    override fun onResume() {
        super.onResume()
    }
}