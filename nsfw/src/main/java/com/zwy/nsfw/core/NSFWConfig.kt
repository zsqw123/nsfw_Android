package com.zwy.nsfw.core

import android.content.res.AssetManager
import java.io.File

/**
 * 配置扫描参数
 * [assetMannager]资源管理器，用于读取lib下的tfLite文件
 * [userGPU]是否开启GPU加速 默认关闭，部分手机开启后会有奔溃
 */
data class NSFWConfig(
        val assetMannager: AssetManager? = null,
        val file: File? = null,
        val userGPU: Boolean = false
)