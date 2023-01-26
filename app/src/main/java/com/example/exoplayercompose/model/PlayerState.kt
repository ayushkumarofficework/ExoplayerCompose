package com.example.exoplayercompose.model

data class PlayerState(val videoUrl : String? = null,
                       val adUrl : String? = "https://pubads.g.doubleclick.net/gampad/ads?iu=/21775744923/external/vmap_ad_samples&sz=640x480&cust_params=sample_ar%3Dpremidpostpod&ciu_szs=300x250&gdfp_req=1&ad_rule=1&output=vmap&unviewed_position_start=1&env=vp&impl=s&cmsid=496&vid=short_onecue&correlator=",
                       val playbackState : Int = 0,
                       val playWhenReady : Boolean = false ,
                       val playerCurrentPosition : Long = 0,
                        val isControllerVisible : Boolean = false)