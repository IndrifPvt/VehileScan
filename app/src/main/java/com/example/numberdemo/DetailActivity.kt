package com.example.numberdemo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_detail.*

class DetailActivity : AppCompatActivity() {
    private var dob = ArrayList<String>()
    private var id = ""
    private var employer = ArrayList<String>()
    private var nam = ArrayList<String>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)
        nam.clear()
        var intent = intent
        var args = intent.getBundleExtra("BUNDLE")
        var userComingBy = args.getString("userComingFrom")
        nam = args.getStringArrayList("detail")
        tv_number_plate_detail.setText(nam.get(0) +" | " + nam.get(1))

    }
}
