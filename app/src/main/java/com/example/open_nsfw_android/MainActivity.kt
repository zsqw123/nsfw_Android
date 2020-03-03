package com.example.open_nsfw_android

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.zwy.nsfw.api.NSFWHelper
import com.zwy.nsfw.core.NSFWConfig
import com.zwy.nsfw.kotlin.getNsfwScore
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.concurrent.thread


class MainActivity : AppCompatActivity(), View.OnClickListener {
    private var nsfwHelper: NSFWHelper? = null
    private var mainAdapter: MainAdapter? = null
    private var index = 0
    private var listData: ArrayList<MyNsfwBean> = ArrayList()
    private var progressDialog: ProgressDialog? = null
    val imgList = mutableListOf<Bitmap>()
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initNsfwHelper()
        initAdapter()
        initClickListener()
        tv_version.text = "当前版本：${this.packageManager.getPackageInfo(packageName, 0).versionName}"
        if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
        ) { //表示未授权时
            //进行授权
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.bt_sc_assets -> {
                reScAssetsImgs()
            }
            R.id.bt_sc_from_other -> {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                intent.type = "image/*"
                startActivityForResult(intent, REQUEST_CODE)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    val uri = data?.data ?: return
                    Glide.with(this).asBitmap().load(uri).into(object : CustomTarget<Bitmap>() {
                        override fun onLoadCleared(placeholder: Drawable?) {}
                        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                            imgList += resource
                            if (!imgList.isNullOrEmpty()) reScFromImgs(imgList)
                        }
                    })
                }
                Activity.RESULT_CANCELED -> {
                    Toast.makeText(this, "未选择图片", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun initClickListener() {
        bt_sc_assets.setOnClickListener(this)
        bt_sc_from_other.setOnClickListener(this)
    }

    private fun initAdapter() {
        mainAdapter = MainAdapter(null)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = mainAdapter
    }

    private fun initNsfwHelper() {
        nsfwHelper = NSFWHelper.init(NSFWConfig(assets))
    }

    private fun reScFromImgs(list: MutableList<Bitmap>) {
        progressDialog = ProgressDialog.show(this, "提示", "请稍后")
        index = 0
        mainAdapter?.setNewData(null)
        listData = ArrayList()
        Thread(Runnable {
            for (i in list) {
                val nsfwScore = i.getNsfwScore(assets)
//                listData.add(MyNsfwBean(0.0f, 0.0f, lm.path, bitmap))
                listData.add(MyNsfwBean(nsfwScore.sfw, nsfwScore.nsfw, i))
//                val nsfwBean = nsfwHelper?.scanBitmap(bitmap)!!
//                listData[index].sfw = nsfwBean.sfw
//                listData[index].nsfw = nsfwBean.nsfw
//                rv.scrollToPosition(index)
                index++
            }
            runOnUiThread {
                mainAdapter?.setNewData(listData)
                mainAdapter?.notifyDataSetChanged()
                progressDialog?.dismiss()
            }
        }).start()
    }

    private fun reScAssetsImgs() {
        progressDialog = ProgressDialog.show(this, "提示", "请稍后")
        index = 0
        mainAdapter?.setNewData(null)
        listData = ArrayList()
        thread(true) {
            for (a in resources.assets.list("img")!!) {
                val path = "img/${a}"
                val b = BitmapFactory.decodeStream(resources.assets.open(path))
                listData.add(MyNsfwBean(0f, 0f, b))
                val nsfwBean = nsfwHelper?.scanBitmap(b)!!
                listData[index].sfw = nsfwBean.sfw
                listData[index].nsfw = nsfwBean.nsfw

                index++
            }
            runOnUiThread {
                mainAdapter?.setNewData(listData)
                mainAdapter?.notifyDataSetChanged()
                progressDialog?.dismiss()
//                rv.scrollToPosition(index)
            }
        }

    }

    override fun onBackPressed() {
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        nsfwHelper?.destroyFactory()
    }

    companion object {
        private const val REQUEST_CODE = 100
    }
}

data class MyNsfwBean(var sfw: Float, var nsfw: Float, val bitmap: Bitmap)